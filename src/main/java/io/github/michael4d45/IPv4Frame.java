package io.github.michael4d45;

/** IPv4 frame for communication between Minecraft and remote IPv4 hosts. */
public class IPv4Frame extends Frame implements RoutedFrame {

  public final byte[] dstIp; // 4 bytes
  public final int dstUdpPort;
  public final int dstWorld;
  public final int dstPort;
  public final byte[] srcIp; // 4 bytes
  public final int srcUdpPort;
  public final int srcWorld;
  public final int srcPort;
  private final int[] payload;

  public IPv4Frame(
      byte[] dstIp,
      int dstUdpPort,
      int dstWorld,
      int dstPort,
      byte[] srcIp,
      int srcUdpPort,
      int srcWorld,
      int srcPort,
      int[] payload) {
    this.dstIp = (dstIp == null) ? new byte[0] : dstIp.clone();
    this.dstUdpPort = dstUdpPort;
    this.dstWorld = dstWorld;
    this.dstPort = dstPort;
    this.srcIp = (srcIp == null) ? new byte[0] : srcIp.clone();
    this.srcUdpPort = srcUdpPort;
    this.srcWorld = srcWorld;
    this.srcPort = srcPort;
    this.payload = (payload == null) ? new int[0] : payload.clone();
  }

  public byte[] getDstIp() {
    return dstIp.clone();
  }

  public byte[] getSrcIp() {
    return srcIp.clone();
  }

  @Override
  public int[] buildSymbols() {
    int payloadLen = payload.length;
    int[] symbols = new int[41 + payloadLen]; // SOF + TYPE + 38 header + payload + EOF
    symbols[0] = 15; // SOF
    symbols[1] = 3; // TYPE
    // DST_IP: 8 nibbles
    for (int i = 0; i < 4; i++) {
      symbols[2 + 2 * i] = (dstIp[i] >> 4) & 0xF;
      symbols[3 + 2 * i] = dstIp[i] & 0xF;
    }
    symbols[10] = (dstUdpPort >> 12) & 0xF; // DST_UDP_PORT_HI_HI
    symbols[11] = (dstUdpPort >> 8) & 0xF; // DST_UDP_PORT_HI_LO
    symbols[12] = (dstUdpPort >> 4) & 0xF; // DST_UDP_PORT_LO_HI
    symbols[13] = dstUdpPort & 0xF; // DST_UDP_PORT_LO_LO
    symbols[14] = (dstWorld >> 4) & 0xF; // DST_WORLD_HI
    symbols[15] = dstWorld & 0xF; // DST_WORLD_LO
    symbols[16] = (dstPort >> 12) & 0xF; // DST_PORT_HI_HI
    symbols[17] = (dstPort >> 8) & 0xF; // DST_PORT_HI_LO
    symbols[18] = (dstPort >> 4) & 0xF; // DST_PORT_LO_HI
    symbols[19] = dstPort & 0xF; // DST_PORT_LO_LO
    // SRC_IP: 8 nibbles
    for (int i = 0; i < 4; i++) {
      symbols[20 + 2 * i] = (srcIp[i] >> 4) & 0xF;
      symbols[21 + 2 * i] = srcIp[i] & 0xF;
    }
    symbols[28] = (srcUdpPort >> 12) & 0xF; // SRC_UDP_PORT_HI_HI
    symbols[29] = (srcUdpPort >> 8) & 0xF; // SRC_UDP_PORT_HI_LO
    symbols[30] = (srcUdpPort >> 4) & 0xF; // SRC_UDP_PORT_LO_HI
    symbols[31] = srcUdpPort & 0xF; // SRC_UDP_PORT_LO_LO
    symbols[32] = (srcWorld >> 4) & 0xF; // SRC_WORLD_HI
    symbols[33] = srcWorld & 0xF; // SRC_WORLD_LO
    symbols[34] = (srcPort >> 12) & 0xF; // SRC_PORT_HI_HI
    symbols[35] = (srcPort >> 8) & 0xF; // SRC_PORT_HI_LO
    symbols[36] = (srcPort >> 4) & 0xF; // SRC_PORT_LO_HI
    symbols[37] = srcPort & 0xF; // SRC_PORT_LO_LO
    symbols[38] = (payloadLen >> 4) & 0xF; // LEN_HI
    symbols[39] = payloadLen & 0xF; // LEN_LO
    System.arraycopy(payload, 0, symbols, 40, payloadLen);
    symbols[40 + payloadLen] = 0; // EOF
    return symbols;
  }

  @Override
  public String toString() {
    return String.format(
        "IPv4Frame{dstIp=%s, dstUdpPort=%d, dstWorld=%d, dstPort=%d, srcIp=%s, srcUdpPort=%d, srcWorld=%d, srcPort=%d, payload=%s}",
        java.util.Arrays.toString(dstIp),
        dstUdpPort,
        dstWorld,
        dstPort,
        java.util.Arrays.toString(srcIp),
        srcUdpPort,
        srcWorld,
        srcPort,
        java.util.Arrays.toString(payload));
  }

  @Override
  public int getDstWorld() {
    return dstWorld;
  }

  @Override
  public int getDstPort() {
    return dstPort;
  }

  public static IPv4Frame fromSymbols(int[] symbols) {
    if (symbols == null || symbols.length < 41) {
      throw new IllegalArgumentException("Invalid symbols array");
    }
    if (symbols[0] != 15) {
      throw new IllegalArgumentException("Invalid SOF");
    }
    if (symbols[1] != 3) {
      throw new IllegalArgumentException("Invalid TYPE");
    }
    // Parse header
    byte[] dstIp = new byte[4];
    for (int i = 0; i < 4; i++) {
      dstIp[i] = (byte) ((symbols[2 + 2 * i] << 4) | symbols[3 + 2 * i]);
    }
    int dstUdpPort = (symbols[10] << 12) | (symbols[11] << 8) | (symbols[12] << 4) | symbols[13];
    int dstWorld = (symbols[14] << 4) | symbols[15];
    int dstPort = (symbols[16] << 12) | (symbols[17] << 8) | (symbols[18] << 4) | symbols[19];
    byte[] srcIp = new byte[4];
    for (int i = 0; i < 4; i++) {
      srcIp[i] = (byte) ((symbols[20 + 2 * i] << 4) | symbols[21 + 2 * i]);
    }
    int srcUdpPort = (symbols[28] << 12) | (symbols[29] << 8) | (symbols[30] << 4) | symbols[31];
    int srcWorld = (symbols[32] << 4) | symbols[33];
    int srcPort = (symbols[34] << 12) | (symbols[35] << 8) | (symbols[36] << 4) | symbols[37];
    int payloadLen = (symbols[38] << 4) | symbols[39];
    if (payloadLen < 0 || payloadLen > symbols.length - 41) {
      throw new IllegalArgumentException("Invalid payload length");
    }
    int[] payload = new int[payloadLen];
    System.arraycopy(symbols, 40, payload, 0, payloadLen);
    if (symbols[40 + payloadLen] != 0) {
      throw new IllegalArgumentException("Invalid EOF");
    }
    return new IPv4Frame(
        dstIp, dstUdpPort, dstWorld, dstPort, srcIp, srcUdpPort, srcWorld, srcPort, payload);
  }
}
