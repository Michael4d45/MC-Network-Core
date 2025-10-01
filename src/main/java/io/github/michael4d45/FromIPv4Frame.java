package io.github.michael4d45;

/** Frame carrying data from a remote IPv4 host. */
public class FromIPv4Frame extends Frame {

  public final int dstWorld;
  public final int dstPort;
  public final int srcPort;
  private final byte[] srcIp; // 4 bytes
  private final int[] payload;

  public FromIPv4Frame(int dstWorld, int dstPort, byte[] srcIp, int srcPort, int[] payload) {
    this.destinationPort = dstPort;
    this.destinationWorld = dstWorld;
    this.dstWorld = dstWorld;
    this.dstPort = dstPort;
    this.srcIp = (srcIp == null) ? new byte[0] : srcIp.clone();
    this.srcPort = srcPort;
    this.payload = (payload == null) ? new int[0] : payload.clone();
  }

  public byte[] getSrcIp() {
    return srcIp.clone();
  }

  @Override
  public int[] getPayload() {
    return payload.clone();
  }

  @Override
  public int[] buildSymbols() {
    int payloadLen = payload.length;
    int[] symbols = new int[19 + payloadLen]; // SOF + 17 header + payload + EOF
    symbols[0] = 15; // SOF
    symbols[1] = 4; // TYPE
    symbols[2] = (dstWorld >> 4) & 0xF; // DST_WORLD_HI
    symbols[3] = dstWorld & 0xF; // DST_WORLD_LO
    symbols[4] = (dstPort >> 4) & 0xF; // DST_PORT_HI
    symbols[5] = dstPort & 0xF; // DST_PORT_LO
    // SRC_IP: 8 nibbles
    for (int i = 0; i < 4; i++) {
      symbols[6 + 2 * i] = (srcIp[i] >> 4) & 0xF;
      symbols[7 + 2 * i] = srcIp[i] & 0xF;
    }
    symbols[14] = (srcPort >> 4) & 0xF; // SRC_PORT_HI
    symbols[15] = srcPort & 0xF; // SRC_PORT_LO
    symbols[16] = (payloadLen >> 4) & 0xF; // LEN_HI
    symbols[17] = payloadLen & 0xF; // LEN_LO
    System.arraycopy(payload, 0, symbols, 18, payloadLen);
    symbols[18 + payloadLen] = 0; // EOF
    return symbols;
  }

  @Override
  public String toString() {
    return String.format(
        "FromIPv4Frame{dstWorld=%d, dstPort=%d, srcIp=%s, srcPort=%d, payload=%s}",
        dstWorld,
        dstPort,
        java.util.Arrays.toString(srcIp),
        srcPort,
        java.util.Arrays.toString(payload));
  }
}
