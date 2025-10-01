package io.github.michael4d45;

import java.util.List;

public class TxFramerStateMachine {

  public enum State {
    IDLE,
    TYPE,
    HEADER,
    DATA,
    ERROR
  }

  public static class Result {

    public final State state;
    public final List<Integer> buffer;
    public final Frame committedFrame;
    public final boolean errorIncremented;
    public final int expectedLength;

    public Result(
        State state,
        List<Integer> buffer,
        Frame committedFrame,
        boolean errorIncremented,
        int expectedLength) {
      this.state = state;
      this.buffer = buffer;
      this.committedFrame = committedFrame;
      this.errorIncremented = errorIncremented;
      this.expectedLength = expectedLength;
    }
  }

  public static Result process(
      State currentState, List<Integer> currentBuffer, int expectedLength, int symbol) {
    List<Integer> newBuffer = new java.util.ArrayList<>(currentBuffer);
    Frame committed = null;
    boolean errorInc = false;

    switch (currentState) {
      case IDLE -> {
        if (symbol == 15) { // SOF
          newBuffer.clear();
          return new Result(State.TYPE, newBuffer, null, false, 0);
        }
      }
      case TYPE -> {
        newBuffer.add(symbol);
        return new Result(State.HEADER, newBuffer, null, false, 0);
      }
      case HEADER -> {
        newBuffer.add(symbol);
        int type = newBuffer.get(0);
        switch (type) {
          case 0 -> {
            // data
            if (newBuffer.size() == 15) {
              int lenHi = newBuffer.get(13);
              int lenLo = newBuffer.get(14);
              int expLen = (lenHi << 4) | lenLo;
              if (expLen >= 0) {
                return new Result(State.DATA, newBuffer, null, false, expLen);
              } else {
                NetworkCore.LOGGER.warn("Invalid length in data frame: " + expLen);
                return new Result(State.ERROR, newBuffer, null, true, 0);
              }
            }
          }
          case 1 -> {
            // control
            if (newBuffer.size() == 4) {
              int lenHi = newBuffer.get(1);
              int lenLo = newBuffer.get(2);
              int expLen = (lenHi << 4) | lenLo;
              if (expLen >= 1) {
                return new Result(State.DATA, newBuffer, null, false, expLen);
              } else {
                NetworkCore.LOGGER.warn("Invalid length in control frame: " + expLen);
                return new Result(State.ERROR, newBuffer, null, true, 0);
              }
            }
          }
          case 3 -> {
            // To IPv4 Frame
            if (newBuffer.size() == 21) {
              int lenHi = newBuffer.get(19);
              int lenLo = newBuffer.get(20);
              int expLen = (lenHi << 4) | lenLo;
              if (expLen >= 0) {
                return new Result(State.DATA, newBuffer, null, false, expLen);
              } else {
                NetworkCore.LOGGER.warn("Invalid length in To IPv4 frame: " + expLen);
                return new Result(State.ERROR, newBuffer, null, true, 0);
              }
            }
          }
          case 4 -> {
            // From IPv4 Frame
            if (newBuffer.size() == 21) {
              int lenHi = newBuffer.get(19);
              int lenLo = newBuffer.get(20);
              int expLen = (lenHi << 4) | lenLo;
              if (expLen >= 0) {
                return new Result(State.DATA, newBuffer, null, false, expLen);
              } else {
                NetworkCore.LOGGER.warn("Invalid length in From IPv4 frame: " + expLen);
                return new Result(State.ERROR, newBuffer, null, true, 0);
              }
            }
          }
          default -> {
            NetworkCore.LOGGER.warn("Unknown frame type: " + type);
            return new Result(State.ERROR, newBuffer, null, true, 0);
          }
        }
      }
      case DATA -> {
        newBuffer.add(symbol);
        int type2 = newBuffer.get(0);
        int totalExpected;
        switch (type2) {
          case 0 -> totalExpected = 15 + expectedLength + 1;
          case 1 -> totalExpected = 4 + expectedLength + 1; // control header length
          case 3, 4 ->
              totalExpected = 21 + expectedLength + 1; // IPv4 header length (TYPE + 20 header)
          default -> {
            NetworkCore.LOGGER.warn("Unexpected frame type in DATA state: " + type2);
            return new Result(State.ERROR, newBuffer, null, true, 0);
          }
        }
        if (newBuffer.size() == totalExpected && symbol == 0) {
          switch (type2) {
            case 0 -> {
              int dstWorldHi = newBuffer.get(1);
              int dstWorldLo = newBuffer.get(2);
              int dstPortHiHi = newBuffer.get(3);
              int dstPortHiLo = newBuffer.get(4);
              int dstPortLoHi = newBuffer.get(5);
              int dstPortLoLo = newBuffer.get(6);
              int srcWorldHi = newBuffer.get(7);
              int srcWorldLo = newBuffer.get(8);
              int srcPortHiHi = newBuffer.get(9);
              int srcPortHiLo = newBuffer.get(10);
              int srcPortLoHi = newBuffer.get(11);
              int srcPortLoLo = newBuffer.get(12);
              int dataLenHi = newBuffer.get(13);
              int dataLenLo = newBuffer.get(14);
              int payloadLen = (dataLenHi << 4) | dataLenLo;
              if (payloadLen == expectedLength) {
                int[] payload = new int[payloadLen];
                for (int i = 0; i < payloadLen; i++) {
                  payload[i] = newBuffer.get(15 + i);
                }
                int dstWorld = (dstWorldHi << 4) | dstWorldLo;
                int dstPort =
                    (dstPortHiHi << 12) | (dstPortHiLo << 8) | (dstPortLoHi << 4) | dstPortLoLo;
                int srcWorld = (srcWorldHi << 4) | srcWorldLo;
                int srcPort =
                    (srcPortHiHi << 12) | (srcPortHiLo << 8) | (srcPortLoHi << 4) | srcPortLoLo;
                committed = new DataFrame(dstWorld, dstPort, srcWorld, srcPort, payload);
                return new Result(State.IDLE, newBuffer, committed, false, 0);
              } else {
                return new Result(State.ERROR, newBuffer, null, true, 0);
              }
            }
            case 1 -> {
              int opcode = newBuffer.get(3);
              int[] args = new int[expectedLength - 1];
              for (int i = 0; i < args.length; i++) {
                args[i] = newBuffer.get(4 + i);
              }
              ControlFrame controlFrame = new ControlFrame(opcode, args);
              return new Result(State.IDLE, newBuffer, controlFrame, false, 0);
            }
            case 3 -> {
              // To IPv4 Frame: srcWorld, srcPort, dstIp[4], dstPort, payload
              int srcWorldHi = newBuffer.get(1);
              int srcWorldLo = newBuffer.get(2);
              int srcPortHiHi = newBuffer.get(3);
              int srcPortHiLo = newBuffer.get(4);
              int srcPortLoHi = newBuffer.get(5);
              int srcPortLoLo = newBuffer.get(6);
              byte[] dstIp = new byte[4];
              for (int i = 0; i < 4; i++) {
                int hi = newBuffer.get(7 + 2 * i);
                int lo = newBuffer.get(8 + 2 * i);
                dstIp[i] = (byte) ((hi << 4) | lo);
              }
              int dstPortHiHi = newBuffer.get(15);
              int dstPortHiLo = newBuffer.get(16);
              int dstPortLoHi = newBuffer.get(17);
              int dstPortLoLo = newBuffer.get(18);
              int dstPort =
                  (dstPortHiHi << 12) | (dstPortHiLo << 8) | (dstPortLoHi << 4) | dstPortLoLo;
              int lenHi = newBuffer.get(19);
              int lenLo = newBuffer.get(20);
              int payloadLen = (lenHi << 4) | lenLo;
              if (payloadLen == expectedLength) {
                int[] payload = new int[payloadLen];
                for (int i = 0; i < payloadLen; i++) {
                  payload[i] = newBuffer.get(21 + i);
                }
                int srcWorld = (srcWorldHi << 4) | srcWorldLo;
                int srcPort =
                    (srcPortHiHi << 12) | (srcPortHiLo << 8) | (srcPortLoHi << 4) | srcPortLoLo;
                committed = new ToIPv4Frame(srcWorld, srcPort, dstIp, dstPort, payload);
                return new Result(State.IDLE, newBuffer, committed, false, 0);
              } else {
                return new Result(State.ERROR, newBuffer, null, true, 0);
              }
            }
            case 4 -> {
              // From IPv4 Frame: dstWorld, dstPort, srcIp[4], srcPort, payload
              int dstWorldHi = newBuffer.get(1);
              int dstWorldLo = newBuffer.get(2);
              int dstPortHiHi = newBuffer.get(3);
              int dstPortHiLo = newBuffer.get(4);
              int dstPortLoHi = newBuffer.get(5);
              int dstPortLoLo = newBuffer.get(6);
              byte[] srcIp = new byte[4];
              for (int i = 0; i < 4; i++) {
                int hi = newBuffer.get(7 + 2 * i);
                int lo = newBuffer.get(8 + 2 * i);
                srcIp[i] = (byte) ((hi << 4) | lo);
              }
              int srcPortHiHi = newBuffer.get(15);
              int srcPortHiLo = newBuffer.get(16);
              int srcPortLoHi = newBuffer.get(17);
              int srcPortLoLo = newBuffer.get(18);
              int srcPort =
                  (srcPortHiHi << 12) | (srcPortHiLo << 8) | (srcPortLoHi << 4) | srcPortLoLo;
              int lenHi = newBuffer.get(19);
              int lenLo = newBuffer.get(20);
              int payloadLen = (lenHi << 4) | lenLo;
              if (payloadLen == expectedLength) {
                int[] payload = new int[payloadLen];
                for (int i = 0; i < payloadLen; i++) {
                  payload[i] = newBuffer.get(21 + i);
                }
                int dstWorld = (dstWorldHi << 4) | dstWorldLo;
                int dstPort =
                    (dstPortHiHi << 12) | (dstPortHiLo << 8) | (dstPortLoHi << 4) | dstPortLoLo;
                committed = new FromIPv4Frame(dstWorld, dstPort, srcIp, srcPort, payload);
                return new Result(State.IDLE, newBuffer, committed, false, 0);
              } else {
                return new Result(State.ERROR, newBuffer, null, true, 0);
              }
            }
            default -> {}
          }
        } else if (newBuffer.size() > totalExpected) {
          NetworkCore.LOGGER.warn("Buffer size exceeded expected length");
          return new Result(State.ERROR, newBuffer, null, true, 0);
        }
      }
      case ERROR -> {
        if (symbol == 0) {
          return new Result(State.IDLE, newBuffer, null, false, 0);
        }
      }
    }
    return new Result(currentState, newBuffer, committed, errorInc, expectedLength);
  }
}
