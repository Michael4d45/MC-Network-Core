package io.github.michael4d45;

import java.util.Arrays;

/**
 * IPv4 frame for communication between Minecraft and remote IPv4 hosts.
 *
 * <p>The CODE field is reserved for future application-layer use. In protocol v1, CODE should be
 * 0x0 for all standard IPv4 frames. Non-zero values (0x1–0xF) may be used by future extensions to
 * indicate priority, QoS flags, or other transport metadata. Current implementation accepts but
 * does not interpret non-zero CODE values.
 *
 * <p>IPv4 frames encapsulate Data (TYPE=0) or Data Control (TYPE=1) frames only. All diagnostics,
 * errors, and status reporting use Data Control frames, whether local or remote.
 */
public class IPv4Frame extends Frame {

  private static final int ADDRESS_ARGS = 24;

  private final int code;
  private final byte[] dstIp; // 4 bytes
  private final int dstUdpPort;
  private final byte[] srcIp; // 4 bytes
  private final int srcUdpPort;
  private final Frame encapsulatedFrame;

  public IPv4Frame(
      byte[] dstIp, int dstUdpPort, byte[] srcIp, int srcUdpPort, Frame encapsulatedFrame) {
    this(0, dstIp, dstUdpPort, srcIp, srcUdpPort, encapsulatedFrame);
  }

  public IPv4Frame(
      int code,
      byte[] dstIp,
      int dstUdpPort,
      byte[] srcIp,
      int srcUdpPort,
      Frame encapsulatedFrame) {
    this.code = code & 0xF;
    if (code != 0x0) {
      NetworkCore.LOGGER.warn(
          "IPv4 frame created with non-zero CODE=0x{} (reserved for future use)",
          Integer.toHexString(code & 0xF).toUpperCase());
    }
    this.dstIp = (dstIp == null) ? new byte[4] : dstIp.clone();
    this.dstUdpPort = clampPort(dstUdpPort);
    this.srcIp = (srcIp == null) ? new byte[4] : srcIp.clone();
    this.srcUdpPort = clampPort(srcUdpPort);
    this.encapsulatedFrame = encapsulatedFrame;
  }

  public byte[] getDstIp() {
    return dstIp.clone();
  }

  public int getDstUdpPort() {
    return dstUdpPort;
  }

  public byte[] getSrcIp() {
    return srcIp.clone();
  }

  public int getSrcUdpPort() {
    return srcUdpPort;
  }

  public Frame getEncapsulatedFrame() {
    return encapsulatedFrame;
  }

  public boolean hasEncapsulatedFrame() {
    return encapsulatedFrame != null;
  }

  @Override
  public int getType() {
    return 3;
  }

  @Override
  public int getCode() {
    return code;
  }

  @Override
  protected int[] getPayloadArgs() {
    int[] encapsulatedArgs = encapsulatedFrame.getPayloadArgs();
    int encapsulatedLen = encapsulatedArgs.length;
    int lenHi = (encapsulatedLen >> 4) & 0xF;
    int lenLo = encapsulatedLen & 0xF;
    int[] args = new int[ADDRESS_ARGS + 4 + encapsulatedArgs.length];
    encodeIp(args, 0, dstIp);
    encodePort(args, 8, dstUdpPort);
    encodeIp(args, 12, srcIp);
    encodePort(args, 20, srcUdpPort);
    args[24] = encapsulatedFrame.getType();
    args[25] = encapsulatedFrame.getCode();
    args[26] = lenHi;
    args[27] = lenLo;
    System.arraycopy(encapsulatedArgs, 0, args, 28, encapsulatedArgs.length);
    return args;
  }

  @Override
  public String toString() {
    return String.format(
        "IPv4Frame{code=%d, dstIp=%s, dstUdpPort=%d, srcIp=%s, srcUdpPort=%d, payload=%s}",
        code,
        Arrays.toString(dstIp),
        dstUdpPort,
        Arrays.toString(srcIp),
        srcUdpPort,
        encapsulatedFrame.toString());
  }

  public static IPv4Frame from(int code, int[] args) {
    if (args.length < ADDRESS_ARGS + 4) {
      throw new IllegalArgumentException("IPv4 frame payload too short");
    }
    byte[] dstIp = decodeIp(args, 0);
    int dstUdpPort = decodePort(args, 8);
    byte[] srcIp = decodeIp(args, 12);
    int srcUdpPort = decodePort(args, 20);
    int encapsulatedType = args[24];
    int encapsulatedCode = args[25];
    int lenHi = args[26];
    int lenLo = args[27];
    int encapsulatedLen = (lenHi << 4) | lenLo;
    if (args.length < ADDRESS_ARGS + 4 + encapsulatedLen) {
      throw new IllegalArgumentException("IPv4 frame encapsulated payload too short");
    }
    int[] encapsulatedArgs =
        Arrays.copyOfRange(args, ADDRESS_ARGS + 4, ADDRESS_ARGS + 4 + encapsulatedLen);
    Frame encapsulatedFrame =
        switch (encapsulatedType) {
          case 0 -> DataFrame.from(encapsulatedCode, encapsulatedArgs);
          case 1 -> new DataControlFrame(encapsulatedCode, encapsulatedArgs);
          case 3 ->
              throw new IllegalArgumentException(
                  "IPv4 frames cannot encapsulate other IPv4 frames (type="
                      + encapsulatedType
                      + ")");
          default ->
              throw new IllegalArgumentException(
                  "Unknown encapsulated frame type " + encapsulatedType);
        };
    return new IPv4Frame(code, dstIp, dstUdpPort, srcIp, srcUdpPort, encapsulatedFrame);
  }

  private static void encodeIp(int[] target, int offset, byte[] address) {
    byte[] addr = (address == null || address.length != 4) ? new byte[4] : address;
    for (int i = 0; i < 4; i++) {
      int b = addr[i] & 0xFF;
      target[offset + (i * 2)] = (b >> 4) & 0xF;
      target[offset + (i * 2) + 1] = b & 0xF;
    }
  }

  private static byte[] decodeIp(int[] source, int offset) {
    byte[] result = new byte[4];
    for (int i = 0; i < 4; i++) {
      result[i] = (byte) ((source[offset + (i * 2)] << 4) | source[offset + (i * 2) + 1]);
    }
    return result;
  }

  private static void encodePort(int[] target, int offset, int port) {
    target[offset] = (port >> 12) & 0xF;
    target[offset + 1] = (port >> 8) & 0xF;
    target[offset + 2] = (port >> 4) & 0xF;
    target[offset + 3] = port & 0xF;
  }

  private static int decodePort(int[] source, int offset) {
    return (source[offset] << 12)
        | (source[offset + 1] << 8)
        | (source[offset + 2] << 4)
        | source[offset + 3];
  }

  private static int clampPort(int value) {
    if (value < 0 || value > 0xFFFF) {
      throw new IllegalArgumentException("Port out of range: " + value);
    }
    return value;
  }
}
