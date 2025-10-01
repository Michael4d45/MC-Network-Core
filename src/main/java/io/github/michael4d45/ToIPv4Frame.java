package io.github.michael4d45;

/** Frame carrying data to a remote IPv4 host. */
public class ToIPv4Frame extends Frame {

  public final int srcWorld;
  public final int srcPort;
  public final int dstPort;
  private final byte[] dstIp; // 4 bytes
  private final int[] payload;

  public ToIPv4Frame(int srcWorld, int srcPort, byte[] dstIp, int dstPort, int[] payload) {
    this.destinationPort = 0;
    this.destinationWorld = 0;
    this.srcWorld = srcWorld;
    this.srcPort = srcPort;
    this.dstIp = (dstIp == null) ? new byte[0] : dstIp.clone();
    this.dstPort = dstPort;
    this.payload = (payload == null) ? new int[0] : payload.clone();
  }

  public byte[] getDstIp() {
    return dstIp.clone();
  }

  @Override
  public int[] getPayload() {
    return payload.clone();
  }

  @Override
  public int[] buildSymbols() {
    int payloadLen = payload.length;
    int[] symbols = new int[23 + payloadLen]; // SOF + 21 header + payload + EOF
    symbols[0] = 15; // SOF
    symbols[1] = 3; // TYPE
    symbols[2] = (srcWorld >> 4) & 0xF; // SRC_WORLD_HI
    symbols[3] = srcWorld & 0xF; // SRC_WORLD_LO
    symbols[4] = (srcPort >> 12) & 0xF; // SRC_PORT_HI_HI
    symbols[5] = (srcPort >> 8) & 0xF; // SRC_PORT_HI_LO
    symbols[6] = (srcPort >> 4) & 0xF; // SRC_PORT_LO_HI
    symbols[7] = srcPort & 0xF; // SRC_PORT_LO_LO
    // DST_IP: 8 nibbles
    for (int i = 0; i < 4; i++) {
      symbols[8 + 2 * i] = (dstIp[i] >> 4) & 0xF;
      symbols[9 + 2 * i] = dstIp[i] & 0xF;
    }
    symbols[16] = (dstPort >> 12) & 0xF; // DST_PORT_HI_HI
    symbols[17] = (dstPort >> 8) & 0xF; // DST_PORT_HI_LO
    symbols[18] = (dstPort >> 4) & 0xF; // DST_PORT_LO_HI
    symbols[19] = dstPort & 0xF; // DST_PORT_LO_LO
    symbols[20] = (payloadLen >> 4) & 0xF; // LEN_HI
    symbols[21] = payloadLen & 0xF; // LEN_LO
    System.arraycopy(payload, 0, symbols, 22, payloadLen);
    symbols[22 + payloadLen] = 0; // EOF
    return symbols;
  }

  @Override
  public String toString() {
    return String.format(
        "ToIPv4Frame{srcWorld=%d, srcPort=%d, dstIp=%s, dstPort=%d, payload=%s}",
        srcWorld,
        srcPort,
        java.util.Arrays.toString(dstIp),
        dstPort,
        java.util.Arrays.toString(payload));
  }
}
