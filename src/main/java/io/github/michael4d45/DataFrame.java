package io.github.michael4d45;

/** Stub for DataFrame. */
public class DataFrame extends Frame {

  public final int dstWorld;
  public final int dstPort;
  public final int srcWorld;
  public final int srcPort;
  private final int[] payload;

  public DataFrame(int dstWorld, int dstPort, int srcWorld, int srcPort, int[] payload) {
    this.destinationPort = dstPort;
    this.destinationWorld = dstWorld;
    this.dstWorld = dstWorld;
    this.dstPort = dstPort;
    this.srcWorld = srcWorld;
    this.srcPort = srcPort;
    // defensive copy to avoid exposing internal representation
    this.payload =
        (payload == null) ? new int[0] : java.util.Arrays.copyOf(payload, payload.length);
  }

  public DataFrame(int dstPort, int srcPort, int[] payload) {
    this(0, dstPort, 0, srcPort, payload);
  }

  @Override
  public int[] getPayload() {
    return java.util.Arrays.copyOf(payload, payload.length);
  }

  @Override
  public String toString() {
    return String.format(
        "DataFrame{dstWorld=%d, dstPort=%d, srcWorld=%d, srcPort=%d, payload=%s}",
        dstWorld, dstPort, srcWorld, srcPort, java.util.Arrays.toString(payload));
  }

  @Override
  public int[] buildSymbols() {
    int[] pl = getPayload();
    int payloadLen = pl.length;
    int[] symbols = new int[17 + payloadLen];
    symbols[0] = 15; // SOF
    symbols[1] = 0; // TYPE
    symbols[2] = (dstWorld >> 4) & 0xF; // DST_WORLD_HI
    symbols[3] = dstWorld & 0xF; // DST_WORLD_LO
    symbols[4] = (dstPort >> 12) & 0xF; // DST_PORT_HI_HI
    symbols[5] = (dstPort >> 8) & 0xF; // DST_PORT_HI_LO
    symbols[6] = (dstPort >> 4) & 0xF; // DST_PORT_LO_HI
    symbols[7] = dstPort & 0xF; // DST_PORT_LO_LO
    symbols[8] = (srcWorld >> 4) & 0xF; // SRC_WORLD_HI
    symbols[9] = srcWorld & 0xF; // SRC_WORLD_LO
    symbols[10] = (srcPort >> 12) & 0xF; // SRC_PORT_HI_HI
    symbols[11] = (srcPort >> 8) & 0xF; // SRC_PORT_HI_LO
    symbols[12] = (srcPort >> 4) & 0xF; // SRC_PORT_LO_HI
    symbols[13] = srcPort & 0xF; // SRC_PORT_LO_LO
    symbols[14] = (payloadLen >> 4) & 0xF; // LEN_HI
    symbols[15] = payloadLen & 0xF; // LEN_LO
    System.arraycopy(pl, 0, symbols, 16, payloadLen);
    symbols[16 + payloadLen] = 0; // EOF
    return symbols;
  }
}
