package io.github.michael4d45;

/** Stub for DataFrame. */
public class DataFrame extends Frame implements RoutedFrame {

  public final int dstWorld;
  public final int dstPort;
  public final int srcWorld;
  public final int srcPort;
  private final int[] payload;

  public DataFrame(int dstWorld, int dstPort, int srcWorld, int srcPort, int[] payload) {
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
  public String toString() {
    return String.format(
        "DataFrame{dstWorld=%d, dstPort=%d, srcWorld=%d, srcPort=%d, payload=%s}",
        dstWorld, dstPort, srcWorld, srcPort, java.util.Arrays.toString(payload));
  }

  @Override
  public int getDstWorld() {
    return dstWorld;
  }

  @Override
  public int getDstPort() {
    return dstPort;
  }

  @Override
  public int[] buildSymbols() {
    int payloadLen = payload.length;
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
    System.arraycopy(payload, 0, symbols, 16, payloadLen);
    symbols[16 + payloadLen] = 0; // EOF
    return symbols;
  }

  public static DataFrame fromSymbols(int[] symbols) {
    if (symbols == null || symbols.length < 18) {
      throw new IllegalArgumentException("Invalid symbols array");
    }
    if (symbols[0] != 15) {
      throw new IllegalArgumentException("Invalid SOF");
    }
    if (symbols[1] != 0) {
      throw new IllegalArgumentException("Invalid TYPE");
    }
    int dstWorld = (symbols[2] << 4) | symbols[3];
    int dstPort = (symbols[4] << 12) | (symbols[5] << 8) | (symbols[6] << 4) | symbols[7];
    int srcWorld = (symbols[8] << 4) | symbols[9];
    int srcPort = (symbols[10] << 12) | (symbols[11] << 8) | (symbols[12] << 4) | symbols[13];
    int len = (symbols[14] << 4) | symbols[15];
    if (len < 0 || len != symbols.length - 17) {
      throw new IllegalArgumentException("Invalid length");
    }
    if (symbols[symbols.length - 1] != 0) {
      throw new IllegalArgumentException("Invalid EOF");
    }
    int[] payload = new int[len];
    System.arraycopy(symbols, 16, payload, 0, len);
    return new DataFrame(dstWorld, dstPort, srcWorld, srcPort, payload);
  }
}
