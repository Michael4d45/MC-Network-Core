package io.github.michael4d45;

import java.util.List;

public class TxFramerStateMachine {

  public enum State {
    IDLE,
    TYPE,
    CODE,
    LEN_HI,
    LEN_LO,
    ARGS,
    EXPECT_EOF,
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
        newBuffer.add(symbol & 0xF);
        return new Result(State.CODE, newBuffer, null, false, 0);
      }
      case CODE -> {
        newBuffer.add(symbol & 0xF);
        return new Result(State.LEN_HI, newBuffer, null, false, 0);
      }
      case LEN_HI -> {
        newBuffer.add(symbol & 0xF);
        return new Result(State.LEN_LO, newBuffer, null, false, 0);
      }
      case LEN_LO -> {
        newBuffer.add(symbol & 0xF);
        int lenHi = newBuffer.get(3);
        int lenLo = newBuffer.get(4);
        int expLen = (lenHi << 4) | lenLo;
        if (expLen < 0 || expLen > 0xFF) {
          NetworkCore.LOGGER.warn("Invalid payload length: {}", expLen);
          return new Result(State.ERROR, newBuffer, null, true, 0);
        }
        // LEN now means total args for ALL frame types (no special Data frame handling)
        if (expLen == 0) {
          return new Result(State.EXPECT_EOF, newBuffer, null, false, 0);
        }
        return new Result(State.ARGS, newBuffer, null, false, expLen);
      }
      case ARGS -> {
        newBuffer.add(symbol & 0xF);
        int argsRead = newBuffer.size() - 5;
        if (argsRead == expectedLength) {
          return new Result(State.EXPECT_EOF, newBuffer, null, false, expectedLength);
        } else if (argsRead > expectedLength) {
          NetworkCore.LOGGER.warn("Received more argument nibbles than expected");
          return new Result(State.ERROR, newBuffer, null, true, 0);
        }
      }
      case EXPECT_EOF -> {
        if (symbol != 0) {
          NetworkCore.LOGGER.warn("Expected EOF nibble but received {}", symbol);
          return new Result(State.ERROR, newBuffer, null, true, 0);
        }
        newBuffer.add(0);
        try {
          committed = decodeFrame(newBuffer);
          return new Result(State.IDLE, new java.util.ArrayList<>(), committed, false, 0);
        } catch (IllegalArgumentException e) {
          NetworkCore.LOGGER.warn("Failed to parse frame: {}", e.getMessage());
          return new Result(State.ERROR, newBuffer, null, true, 0);
        }
      }
      case ERROR -> {
        if (symbol == 0) {
          return new Result(State.IDLE, new java.util.ArrayList<>(), null, false, 0);
        }
      }
    }
    return new Result(currentState, newBuffer, committed, errorInc, expectedLength);
  }

  private static Frame decodeFrame(List<Integer> buffer) {
    int[] symbols = buffer.stream().mapToInt(i -> i & 0xF).toArray();
    if (symbols.length < 6) {
      throw new IllegalArgumentException("Frame too short");
    }
    int type = symbols[1];
    int code = symbols[2];
    int len = (symbols[3] << 4) | symbols[4];
    if (len != symbols.length - 6) {
      throw new IllegalArgumentException(
          "Length mismatch: expected " + len + ", got " + (symbols.length - 6));
    }
    int[] args = new int[len];
    System.arraycopy(symbols, 5, args, 0, len);
    return switch (type) {
      case 0 -> DataFrame.from(code, args);
      case 1 -> DataControlFrame.from(code, args);
      case 2 -> StatusFrame.from(code, args);
      case 3 -> IPv4Frame.from(code, args);
      case 4 ->
          // In-game parsing: UDP ports are unknown at parse time and set to -1.
          // IPv4Router will extract actual ports from DatagramPacket metadata or resolve
          // them from routing context when sending over the network.
          IPv4ControlFrame.from(code, args, -1, -1);
      default -> throw new IllegalArgumentException("Unknown frame type: " + type);
    };
  }
}
