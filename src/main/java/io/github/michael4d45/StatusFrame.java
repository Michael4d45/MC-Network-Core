package io.github.michael4d45;

/** Stub for StatusFrame. */
public class StatusFrame extends Frame {
  private final int world;
  private final int port;
  private final int rxDepth;
  private final int txDepth;
  private final int errorFlags;

  public StatusFrame(int world, int port, int rxDepth, int txDepth, int errorFlags) {
    this.destinationPort = port;
    this.destinationWorld = world;
    this.world = world;
    this.port = port;
    this.rxDepth = rxDepth;
    this.txDepth = txDepth;
    this.errorFlags = errorFlags;
  }

  @Override
  public int[] getPayload() {
    return new int[] {
      0xA, // signature
      (world >> 4) & 0xF, // World high
      world & 0xF, // World low
      (port >> 4) & 0xF, // Port high
      port & 0xF, // Port low
      Math.min(rxDepth, 13), // RX queue depth clipped
      Math.min(txDepth, 13), // TX queue depth clipped
      errorFlags & 0xF // Error flags bitmap
    };
  }

  @Override
  public String toString() {
    return String.format(
        "StatusFrame{world=%d, port=%d, rxDepth=%d, txDepth=%d, errorFlags=%d}",
        world, port, rxDepth, txDepth, errorFlags);
  }

  @Override
  public int[] buildSymbols() {
    int[] payload = getPayload();
    int[] symbols = new int[13];
    symbols[0] = 15; // SOF
    symbols[1] = 2; // TYPE
    symbols[2] = 0; // LEN_HI
    symbols[3] = 8; // LEN_LO
    System.arraycopy(payload, 0, symbols, 4, 8);
    symbols[12] = 0; // EOF
    return symbols;
  }
}
