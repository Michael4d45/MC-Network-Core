package io.github.michael4d45;

public class IPv4Router {
  public static void sendFrame(ToIPv4Frame frame) {
    NetworkCore.LOGGER.info("Sending IPv4 frame: {}", frame);
    // Send to IPv4 network (stub)
  }

  // /*
  //  * Handle an incoming frame from the IPv4 network.
  //  */
  // public static void handleIncomingFrame(data) {
  //   NetworkCore.LOGGER.info("Received IPv4 frame: {}", frame);
  //   // convert to FromIPv4Frame
  //   FromIPv4Frame frame = new FromIPv4Frame(...);
  //   DataRouter.sendFrame(frame);
  // }
}
