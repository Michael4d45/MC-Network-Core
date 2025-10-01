package io.github.michael4d45;

import java.util.Queue;

/** Rx emitter from Frame to nibble stream (pure-functional). */
public class RxEmitterStateMachine {
  public enum State {
    IDLE,
    OUTPUTTING
  }

  public static class Result {
    public final State newState;
    public final Frame currentFrame;
    // Expose copy of symbols to avoid EI_EXPOSE_REP warnings
    public final int[] symbols;
    public final Integer outputSymbol;
    public final int position;

    public Result(
        State newState, Frame currentFrame, int[] symbols, Integer outputSymbol, int position) {
      this.newState = newState;
      this.currentFrame = currentFrame;
      this.symbols = (symbols == null) ? null : java.util.Arrays.copyOf(symbols, symbols.length);
      this.outputSymbol = outputSymbol;
      this.position = position;
    }
  }

  public static Result process(
      State currentState,
      Frame currentFrame,
      int[] currentSymbols,
      int currentPosition,
      Queue<Frame> rxRing) {
    switch (currentState) {
      case IDLE -> {
        if (!rxRing.isEmpty()) {
          Frame frame = rxRing.poll();
          int[] symbols = frame.buildSymbols();
          return new Result(State.OUTPUTTING, frame, symbols, symbols[0], 1);
        } else {
          return new Result(State.IDLE, null, null, 0, 0);
        }
      }
      case OUTPUTTING -> {
        if (currentSymbols == null || currentPosition >= currentSymbols.length) {
          return new Result(State.IDLE, null, null, 0, 0);
        } else {
          int symbol = currentSymbols[currentPosition];
          return new Result(
              State.OUTPUTTING, currentFrame, currentSymbols, symbol, currentPosition + 1);
        }
      }
    }
    return new Result(currentState, currentFrame, currentSymbols, 0, currentPosition);
  }
}
