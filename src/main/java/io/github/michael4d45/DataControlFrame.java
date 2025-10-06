package io.github.michael4d45;

import java.util.Arrays;

/** Control frame used for intra-world diagnostics and management. */
public class DataControlFrame extends Frame {

  private final int code;
  private final int[] args;

  public DataControlFrame(int code, int[] args) {
    this.code = code & 0xF;
    this.args = (args == null) ? new int[0] : Arrays.copyOf(args, args.length);
  }

  public int[] getArgs() {
    return Arrays.copyOf(args, args.length);
  }

  @Override
  public int getType() {
    return 1;
  }

  @Override
  public int getCode() {
    return code;
  }

  @Override
  protected int[] getPayloadArgs() {
    return Arrays.copyOf(args, args.length);
  }

  @Override
  public String toString() {
    return String.format("DataControlFrame{code=%d, args=%s}", code, Arrays.toString(args));
  }

  public static DataControlFrame from(int code, int[] args) {
    return new DataControlFrame(code, args);
  }
}
