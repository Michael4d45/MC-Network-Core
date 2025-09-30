package io.github.michael4d45;

/** Stub for ControlFrame. */
public class ControlFrame extends Frame {

  public final int opcode;
  private final int[] args;

  public ControlFrame(int opcode, int[] args) {
    this.destinationPort = 0;
    this.destinationWorld = 0;
    this.opcode = opcode;
    // defensive copy to avoid exposing internal representation
    this.args = (args == null) ? new int[0] : java.util.Arrays.copyOf(args, args.length);
  }

  @Override
  public int[] getPayload() {
    return getArgs();
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
    int[] args = getArgs();
    int len = 1 + args.length; // OP + args
    int[] symbols = new int[5 + args.length];
    symbols[0] = 15; // SOF
    symbols[1] = 1; // TYPE
    symbols[2] = (len >> 4) & 0xF; // LEN_HI
    symbols[3] = len & 0xF; // LEN_LO
    symbols[4] = opcode; // OP
    System.arraycopy(args, 0, symbols, 5, args.length);
    symbols[5 + args.length] = 0; // EOF
    return symbols;
  }
}
