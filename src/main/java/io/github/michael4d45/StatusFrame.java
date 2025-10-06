package io.github.michael4d45;

/** Status frame emitted in response to MODEQ requests. */
public class StatusFrame extends Frame {

  private final int code;
  private final int port;
  private final int rxDepth;
  private final int errorFlags;

  public StatusFrame(int port, int rxDepth, int errorFlags) {
    this(0, port, rxDepth, errorFlags);
  }

  public StatusFrame(int code, int port, int rxDepth, int errorFlags) {
    this.code = code & 0xF;
    this.port = port & 0xFFFF;
    this.rxDepth = rxDepth;
    this.errorFlags = errorFlags & 0xF;
  }

  public int[] getPayload() {
    return new int[] {
      0xA, // signature
      (port >> 12) & 0xF, // Port high high
      (port >> 8) & 0xF, // Port high low
      (port >> 4) & 0xF, // Port low high
      port & 0xF, // Port low low
      Math.min(rxDepth, 64), // RX queue depth clipped to capacity
      errorFlags & 0xF // Error flags bitmap (bit0=RX_OVERFLOW, bit1=TX_FRAMING_ERR,
      // bit2=PORT_ALLOC_FAILURE, bit3=IPV4_ROUTING_FAILURE)
    };
  }

  @Override
  public int getType() {
    return 2;
  }

  @Override
  public int getCode() {
    return code;
  }

  @Override
  protected int[] getPayloadArgs() {
    return getPayload();
  }

  @Override
  public String toString() {
    return String.format(
        "StatusFrame{code=%d, port=%d, rxDepth=%d, errorFlags=%d}",
        code, port, rxDepth, errorFlags);
  }

  public static StatusFrame from(int code, int[] args) {
    if (args.length != 7) {
      throw new IllegalArgumentException("Status frame payload must be 7 nibbles");
    }
    if ((args[0] & 0xF) != 0xA) {
      throw new IllegalArgumentException("Status frame missing signature nibble");
    }
    int port = (args[1] << 12) | (args[2] << 8) | (args[3] << 4) | args[4];
    int rxDepth = args[5] & 0xF;
    int errorFlags = args[6] & 0xF;
    return new StatusFrame(code, port, rxDepth, errorFlags);
  }
}
