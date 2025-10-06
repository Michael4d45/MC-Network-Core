package io.github.michael4d45;

import java.util.Arrays;

/**
 * IPv4 control frame conveying ICMP-like diagnostics between instances.
 *
 * <p><b>UDP Port Metadata:</b> The dstUdpPort and srcUdpPort fields are required for proper routing
 * over UDP. When IPv4ControlFrames are parsed in-game (via the TX parser), these ports are
 * initially unknown and set to -1. The IPv4Router extracts the actual UDP port information from the
 * DatagramPacket and populates these fields during frame construction. For frames created in-game
 * that need to be sent over UDP, the router will resolve the appropriate UDP ports based on routing
 * tables or cached session state.
 */
public class IPv4ControlFrame extends Frame {

  private static final int ADDRESS_ARGS = 16;

  private final int code;
  private final byte[] dstIp;
  private final byte[] srcIp;
  private final int[] contextArgs;
  private final int dstUdpPort;
  private final int srcUdpPort;

  public IPv4ControlFrame(
      int code, byte[] dstIp, byte[] srcIp, int[] contextArgs, int dstUdpPort, int srcUdpPort) {
    this.code = code & 0xF;
    this.dstIp = (dstIp == null) ? new byte[4] : dstIp.clone();
    this.srcIp = (srcIp == null) ? new byte[4] : srcIp.clone();
    this.contextArgs =
        (contextArgs == null) ? new int[0] : Arrays.copyOf(contextArgs, contextArgs.length);
    this.dstUdpPort = dstUdpPort;
    this.srcUdpPort = srcUdpPort;
  }

  public byte[] getDstIp() {
    return dstIp.clone();
  }

  public byte[] getSrcIp() {
    return srcIp.clone();
  }

  public int getDstUdpPort() {
    return dstUdpPort;
  }

  public int getSrcUdpPort() {
    return srcUdpPort;
  }

  public int[] getArgs() {
    return Arrays.copyOf(contextArgs, contextArgs.length);
  }

  @Override
  public int getType() {
    return 4;
  }

  @Override
  public int getCode() {
    return code;
  }

  @Override
  protected int[] getPayloadArgs() {
    int[] args = new int[ADDRESS_ARGS + contextArgs.length];
    encodeIp(args, 0, dstIp);
    encodeIp(args, 8, srcIp);
    System.arraycopy(contextArgs, 0, args, ADDRESS_ARGS, contextArgs.length);
    return args;
  }

  @Override
  public String toString() {
    return String.format(
        "IPv4ControlFrame{code=%d, dstIp=%s, srcIp=%s, args=%s, dstUdpPort=%d, srcUdpPort=%d}",
        code,
        Arrays.toString(dstIp),
        Arrays.toString(srcIp),
        Arrays.toString(contextArgs),
        dstUdpPort,
        srcUdpPort);
  }

  public static IPv4ControlFrame from(int code, int[] args, int dstUdpPort, int srcUdpPort) {
    if (args.length < ADDRESS_ARGS) {
      throw new IllegalArgumentException("IPv4 control frame payload too short for addresses");
    }
    byte[] dstIp = decodeIp(args, 0);
    byte[] srcIp = decodeIp(args, 8);
    int[] context = Arrays.copyOfRange(args, ADDRESS_ARGS, args.length);
    return new IPv4ControlFrame(code, dstIp, srcIp, context, dstUdpPort, srcUdpPort);
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
}
