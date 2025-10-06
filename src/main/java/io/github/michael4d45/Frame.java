package io.github.michael4d45;

/** Base abstraction for all nibble-framed messages. */
public abstract class Frame {

  /** Returns the 4-bit frame type identifier. */
  public abstract int getType();

  /** Returns the 4-bit subtype / code field for this frame. */
  public abstract int getCode();

  /**
   * Returns the payload / argument nibble sequence (LEN long).
   *
   * <p>Callers must treat the returned array as immutable; subclasses are expected to return fresh
   * copies to avoid exposing internal representation.
   */
  protected abstract int[] getPayloadArgs();

  /** Builds the nibble stream representing this frame (SOF..EOF inclusive). */
  public int[] buildSymbols() {
    int[] args = getPayloadArgs();
    if (args.length > 0xFF) {
      throw new IllegalStateException("Payload length exceeds 255 nibbles");
    }
    int[] symbols = new int[args.length + 6];
    symbols[0] = 15; // SOF
    symbols[1] = getType() & 0xF;
    symbols[2] = getCode() & 0xF;
    symbols[3] = (args.length >> 4) & 0xF; // LEN_HI
    symbols[4] = args.length & 0xF; // LEN_LO
    System.arraycopy(args, 0, symbols, 5, args.length);
    symbols[symbols.length - 1] = 0; // EOF
    return symbols;
  }
}
