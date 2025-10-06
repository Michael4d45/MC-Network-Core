package io.github.michael4d45;

import java.util.Arrays;

/**
 * Data frame transporting payload between ports within the same world. CODE must be 0x0 for all
 * standard Data frames in protocol v1. Non-zero CODE values are reserved for future
 * application-layer extensions.
 */
public class DataFrame extends Frame {

  public static final int DEFAULT_CODE = 0x0;

  private final int code;
  private final int dstPort;
  private final int srcPort;
  private final int[] payload;

  public DataFrame(int dstPort, int srcPort, int[] payload) {
    this(DEFAULT_CODE, dstPort, srcPort, payload);
  }

  public DataFrame(int code, int dstPort, int srcPort, int[] payload) {
    this.code = code & 0xF;
    if (code != 0x0) {
      NetworkCore.LOGGER.warn(
          "Data frame created with non-zero CODE=0x{} (reserved for future use)",
          Integer.toHexString(code & 0xF).toUpperCase());
    }
    this.dstPort = clampPort(dstPort);
    this.srcPort = clampPort(srcPort);
    this.payload = (payload == null) ? new int[0] : Arrays.copyOf(payload, payload.length);
  }

  public int getDstPort() {
    return dstPort;
  }

  public int getSrcPort() {
    return srcPort;
  }

  public int[] getPayload() {
    return Arrays.copyOf(payload, payload.length);
  }

  @Override
  public int getType() {
    return 0;
  }

  @Override
  public int getCode() {
    return code;
  }

  @Override
  protected int[] getPayloadArgs() {
    int[] args = new int[8 + payload.length];
    encodePort(args, 0, dstPort);
    encodePort(args, 4, srcPort);
    System.arraycopy(payload, 0, args, 8, payload.length);
    return args;
  }

  @Override
  public String toString() {
    return String.format(
        "DataFrame{code=%d, dstPort=%d, srcPort=%d, payload=%s}",
        code, dstPort, srcPort, Arrays.toString(payload));
  }

  public static DataFrame from(int code, int[] args) {
    if (args.length < 8) {
      throw new IllegalArgumentException("Data frame payload must include dst/src port");
    }
    if (code != 0x0) {
      NetworkCore.LOGGER.warn(
          "Data frame parsed with non-zero CODE=0x{} (reserved for future use)",
          Integer.toHexString(code & 0xF).toUpperCase());
    }
    int dstPort = decodePort(args, 0);
    int srcPort = decodePort(args, 4);
    int[] payload = Arrays.copyOfRange(args, 8, args.length);
    return new DataFrame(code, dstPort, srcPort, payload);
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

  private static int clampPort(int port) {
    if (port < 0 || port > 0xFFFF) {
      throw new IllegalArgumentException("Port out of range: " + port);
    }
    return port;
  }
}
