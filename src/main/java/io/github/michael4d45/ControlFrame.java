package io.github.michael4d45;

/** Stub for ControlFrame. */
public class ControlFrame extends Frame {

  public final int opcode;
  private final int[] args;

  public ControlFrame(int opcode, int[] args) {
    this.opcode = opcode;
    // defensive copy to avoid exposing internal representation
    this.args = (args == null) ? new int[0] : java.util.Arrays.copyOf(args, args.length);
  }

  public int[] getArgs() {
    return java.util.Arrays.copyOf(args, args.length);
  }

  @Override
  public String toString() {
    return String.format(
        "ControlFrame{opcode=%d, args=%s}", opcode, java.util.Arrays.toString(args));
  }

  @Override
  public int[] buildSymbols() {
    int len = 1 + args.length; // OP + args
    int[] symbols = new int[5 + args.length];
    symbols[0] = 15; // SOF
    symbols[1] = 1; // TYPE
    symbols[2] = opcode; // OP
    symbols[3] = (len >> 4) & 0xF; // LEN_HI
    symbols[4] = len & 0xF; // LEN_LO
    System.arraycopy(args, 0, symbols, 5, args.length);
    symbols[5 + args.length] = 0; // EOF
    return symbols;
  }

  public static ControlFrame fromSymbols(int[] symbols) {
    if (symbols == null || symbols.length < 6) {
      throw new IllegalArgumentException("Invalid symbols for ControlFrame: too short");
    }
    if (symbols[0] != 15) {
      throw new IllegalArgumentException("Invalid symbols for ControlFrame: missing SOF");
    }
    if (symbols[1] != 1) {
      throw new IllegalArgumentException("Invalid symbols for ControlFrame: wrong TYPE");
    }
    int opcode = symbols[2];
    int len = (symbols[3] << 4) | symbols[4];
    if (len < 1 || len != symbols.length - 6) {
      throw new IllegalArgumentException("Invalid symbols for ControlFrame: length mismatch");
    }
    if (symbols[symbols.length - 1] != 0) {
      throw new IllegalArgumentException("Invalid symbols for ControlFrame: missing EOF");
    }
    int[] args = new int[len];
    System.arraycopy(symbols, 5, args, 0, len);
    return new ControlFrame(opcode, args);
  }
}
