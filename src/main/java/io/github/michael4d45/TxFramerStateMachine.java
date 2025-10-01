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

    public final State newState;
    public final List<Integer> buffer;
    public final Frame committedFrame;
    public final boolean errorIncremented;
    public final int expectedLength;

    public Result(
        State newState,
        List<Integer> buffer,
        Frame committedFrame,
        boolean errorIncremented,
        int expectedLength) {
      this.newState = newState;
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
            if (newBuffer.size() == 11) {
              int lenHi = newBuffer.get(9);
              int lenLo = newBuffer.get(10);
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
        if (type2 == 0) {
          totalExpected = 11 + expectedLength + 1;
        } else {
          totalExpected = 4 + expectedLength + 1;
        }
        if (newBuffer.size() == totalExpected && symbol == 0) {
          if (type2 == 0) {
            int dstWorldHi = newBuffer.get(1);
            int dstWorldLo = newBuffer.get(2);
            int dstPortHi = newBuffer.get(3);
            int dstPortLo = newBuffer.get(4);
            int srcWorldHi = newBuffer.get(5);
            int srcWorldLo = newBuffer.get(6);
            int srcPortHi = newBuffer.get(7);
            int srcPortLo = newBuffer.get(8);
            int dataLenHi = newBuffer.get(9);
            int dataLenLo = newBuffer.get(10);
            int payloadLen = (dataLenHi << 4) | dataLenLo;
            if (payloadLen == expectedLength) {
              int[] payload = new int[payloadLen];
              for (int i = 0; i < payloadLen; i++) {
                payload[i] = newBuffer.get(11 + i);
              }
              int dstWorld = (dstWorldHi << 4) | dstWorldLo;
              int dstPort = (dstPortHi << 4) | dstPortLo;
              int srcWorld = (srcWorldHi << 4) | srcWorldLo;
              int srcPort = (srcPortHi << 4) | srcPortLo;
              committed = new DataFrame(dstWorld, dstPort, srcWorld, srcPort, payload);
              return new Result(State.IDLE, newBuffer, committed, false, 0);
            } else {
              return new Result(State.ERROR, newBuffer, null, true, 0);
            }
          } else {
            int opcode = newBuffer.get(3);
            int[] args = new int[expectedLength - 1];
            for (int i = 0; i < args.length; i++) {
              args[i] = newBuffer.get(4 + i);
            }
            ControlFrame controlFrame = new ControlFrame(opcode, args);
            return new Result(State.IDLE, newBuffer, controlFrame, false, 0);
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
