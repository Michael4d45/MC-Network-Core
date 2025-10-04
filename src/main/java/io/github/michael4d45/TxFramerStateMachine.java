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
          newBuffer.add(15);
          return new Result(State.TYPE, newBuffer, null, false, 0);
        }
      }
      case TYPE -> {
        newBuffer.add(symbol);
        return new Result(State.HEADER, newBuffer, null, false, 0);
      }
      case HEADER -> {
        newBuffer.add(symbol);
        int type = newBuffer.get(1);
        switch (type) {
          case 0 -> {
            // data
            if (newBuffer.size() == 16) {
              int lenHi = newBuffer.get(14);
              int lenLo = newBuffer.get(15);
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
            if (newBuffer.size() == 5) {
              int lenHi = newBuffer.get(3);
              int lenLo = newBuffer.get(4);
              int expLen = (lenHi << 4) | lenLo;
              if (expLen >= 0) {
                return new Result(State.DATA, newBuffer, null, false, expLen);
              } else {
                NetworkCore.LOGGER.warn("Invalid length in control frame: " + expLen);
                return new Result(State.ERROR, newBuffer, null, true, 0);
              }
            }
          }
          case 3 -> {
            // IPv4 Frame
            if (newBuffer.size() == 40) {
              int lenHi = newBuffer.get(38);
              int lenLo = newBuffer.get(39);
              int expLen = (lenHi << 4) | lenLo;
              if (expLen >= 0) {
                return new Result(State.DATA, newBuffer, null, false, expLen);
              } else {
                NetworkCore.LOGGER.warn("Invalid length in IPv4 frame: " + expLen);
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
        int type2 = newBuffer.get(1);
        int totalExpected;
        switch (type2) {
          case 0 -> totalExpected = 16 + expectedLength + 1;
          case 1 -> totalExpected = 5 + expectedLength + 1; // control header length
          case 3 -> totalExpected = 40 + expectedLength + 1; // IPv4 header length (TYPE + 38 data)
          default -> {
            NetworkCore.LOGGER.warn("Unexpected frame type in DATA state: " + type2);
            return new Result(State.ERROR, newBuffer, null, true, 0);
          }
        }
        if (newBuffer.size() == totalExpected && symbol == 0) {
          switch (type2) {
            case 0 -> {
              try {
                committed = DataFrame.fromSymbols(newBuffer.stream().mapToInt(i -> i).toArray());
                return new Result(State.IDLE, newBuffer, committed, false, 0);
              } catch (IllegalArgumentException e) {
                NetworkCore.LOGGER.warn("Failed to parse data frame: " + e.getMessage());
                return new Result(State.ERROR, newBuffer, null, true, 0);
              }
            }
            case 1 -> {
              try {
                committed = ControlFrame.fromSymbols(newBuffer.stream().mapToInt(i -> i).toArray());
                return new Result(State.IDLE, newBuffer, committed, false, 0);
              } catch (IllegalArgumentException e) {
                NetworkCore.LOGGER.warn("Failed to parse control frame: " + e.getMessage());
                return new Result(State.ERROR, newBuffer, null, true, 0);
              }
            }
            case 3 -> {
              try {
                committed = IPv4Frame.fromSymbols(newBuffer.stream().mapToInt(i -> i).toArray());
                return new Result(State.IDLE, newBuffer, committed, false, 0);
              } catch (IllegalArgumentException e) {
                NetworkCore.LOGGER.warn("Failed to parse IPv4 frame: " + e.getMessage());
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
