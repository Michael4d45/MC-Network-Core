package io.github.michael4d45;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class IPv4Router {

  private static final int UDP_PORT_START = 12345;

  private static DatagramSocket socket;
  private static Thread receiveThread;
  private static volatile boolean running = false;
  private static int udpPort;
  private static InetAddress localAddress;

  public static void init() {
    ServerLifecycleEvents.SERVER_STARTED.register(
        mcServer -> {
          int port = UDP_PORT_START;
          while (port <= 65535) {
            try {
              socket = new DatagramSocket(port);
              udpPort = port;
              localAddress = socket.getLocalAddress();
              NetworkCore.LOGGER.info("IPv4Router listening on UDP port {}", udpPort);
              running = true;
              receiveThread = new Thread(IPv4Router::receiveLoop, "IPv4Router-Receive");
              receiveThread.setDaemon(true);
              receiveThread.start();
              break;
            } catch (SocketException e) {
              port++;
            }
          }
          if (socket == null) {
            NetworkCore.LOGGER.error(
                "Failed to find a free UDP port starting from {}", UDP_PORT_START);
          }
        });

    ServerLifecycleEvents.SERVER_STOPPING.register(
        mcServer -> {
          running = false;
          if (socket != null) {
            socket.close();
          }
          if (receiveThread != null) {
            receiveThread.interrupt();
          }
        });
  }

  public static void sendFrame(Frame frame) {
    if (socket == null) {
      NetworkCore.LOGGER.warn("IPv4Router not initialized, cannot send frame");
      return;
    }
    switch (frame) {
      case IPv4Frame ipv4Frame -> {
        if (!sendUdp(ipv4Frame.getDstIp(), ipv4Frame.getDstUdpPort(), ipv4Frame.buildSymbols())) {
          // Send HOST_UNREACHABLE (Data Control code 0xA) back
          DataControlFrame errorControl =
              new DataControlFrame(0xA, encodeIp(ipv4Frame.getDstIp())); // HOST_UNREACHABLE
          IPv4Frame errorFrame =
              new IPv4Frame(
                  ipv4Frame.getSrcIp(),
                  ipv4Frame.getSrcUdpPort(),
                  getLocalIpBytes(),
                  udpPort,
                  errorControl);
          sendUdp(errorFrame.getDstIp(), errorFrame.getDstUdpPort(), errorFrame.buildSymbols());
        }
      }
      default ->
          NetworkCore.LOGGER.warn(
              "Unsupported frame type for IPv4Router: {}", frame.getClass().getSimpleName());
    }
  }

  private static void receiveLoop() {
    byte[] buffer = new byte[1024]; // Max payload size
    while (running) {
      try {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());

        int[] nibbles = bytesToNibbles(data);
        Frame frame = parseFrameFromNibbles(nibbles);
        if (frame == null) {
          // Send NETWORK_ERROR (Data Control code 0xB) back to sender
          DataControlFrame errorControl = new DataControlFrame(0xB, new int[0]); // NETWORK_ERROR
          IPv4Frame errorFrame =
              new IPv4Frame(
                  packet.getAddress().getAddress(),
                  packet.getPort(),
                  getLocalIpBytes(),
                  udpPort,
                  errorControl);
          sendUdp(errorFrame.getDstIp(), errorFrame.getDstUdpPort(), errorFrame.buildSymbols());
          continue;
        }
        NetworkCore.LOGGER.info("Received UDP packet, parsed frame {}", frame);
        Frame finalFrame = frame;
        var srv = DataRouter.server;
        if (srv != null) {
          srv.execute(
              () -> {
                switch (finalFrame) {
                  case IPv4Frame ipv4Frame -> DataRouter.deliverIPv4Frame(ipv4Frame);
                  default -> {}
                }
              });
        } else {
          NetworkCore.LOGGER.debug("Discarding frame received during shutdown: {}", finalFrame);
        }
      } catch (IOException e) {
        if (running) {
          NetworkCore.LOGGER.error("Error receiving UDP packet", e);
        }
      }
    }
  }

  /**
   * Converts a nibble array to bytes for UDP transmission.
   *
   * <p>Packs two nibbles per byte (high nibble in upper 4 bits, low nibble in lower 4 bits). For
   * odd-length nibble arrays, the last byte's low nibble is padded with 0.
   *
   * <p>Example: [0xF, 0x3, 0xA] → bytes [0xF3, 0xA0]
   *
   * <p>This padding is symmetric with bytesToNibbles and preserves EOF semantics: Frame.buildSymbols()
   * always produces arrays ending with EOF=0. If the frame has an odd number of nibbles (excluding
   * SOF/EOF), the padding creates an even-length byte array where the trailing 0 nibble matches EOF,
   * ensuring correct parsing on the receiving end.
   *
   * @param nibbles array of 4-bit values (0-15)
   * @return byte array with length ⌈nibbles.length / 2⌉
   */
  private static byte[] nibblesToBytes(int[] nibbles) {
    int byteLen = (nibbles.length + 1) / 2; // Round up
    byte[] bytes = new byte[byteLen];
    for (int i = 0; i < nibbles.length; i += 2) {
      int high = nibbles[i] & 0xF;
      int low = (i + 1 < nibbles.length) ? nibbles[i + 1] & 0xF : 0;
      bytes[i / 2] = (byte) ((high << 4) | low);
    }
    return bytes;
  }

  /**
   * Unpacks bytes received from UDP into a nibble array.
   *
   * <p>Each byte is split into two nibbles (upper 4 bits → first nibble, lower 4 bits → second
   * nibble). This is the inverse of nibblesToBytes.
   *
   * <p>The returned array always has even length (bytes.length * 2). Any padding added by
   * nibblesToBytes for odd-length frames is preserved, ensuring the trailing EOF nibble (0) is
   * correctly recognized by the frame parser.
   *
   * @param bytes UDP packet data
   * @return nibble array with length = bytes.length * 2
   */
  private static int[] bytesToNibbles(byte[] bytes) {
    int[] nibbles = new int[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      nibbles[i * 2] = (bytes[i] >> 4) & 0xF;
      nibbles[i * 2 + 1] = bytes[i] & 0xF;
    }
    return nibbles;
  }

  private static boolean sendUdp(byte[] dstIp, int dstUdpPort, int[] symbols) {
    try {
      byte[] data = nibblesToBytes(symbols);
      InetAddress address = InetAddress.getByAddress(dstIp);
      DatagramPacket packet = new DatagramPacket(data, data.length, address, dstUdpPort);
      socket.send(packet);
      NetworkCore.LOGGER.info(
          "Sent UDP packet to {}:{} with {} bytes", address, dstUdpPort, data.length);
      return true;
    } catch (IOException e) {
      NetworkCore.LOGGER.error("Failed to send UDP packet", e);
      return false;
    }
  }

  // Parses frame from UDP nibbles. Only IPv4 frames are supported over UDP.
  private static Frame parseFrameFromNibbles(int[] nibbles) {
    if (nibbles.length < 6) {
      NetworkCore.LOGGER.warn("UDP payload too short for frame");
      return null;
    }
    if (nibbles[0] != 15) {
      NetworkCore.LOGGER.warn("UDP payload missing SOF nibble");
      return null;
    }
    if (nibbles[nibbles.length - 1] != 0) {
      NetworkCore.LOGGER.warn("UDP payload missing EOF nibble");
      return null;
    }
    int type = nibbles[1] & 0xF;
    int code = nibbles[2] & 0xF;
    int len = (nibbles[3] << 4) | (nibbles[4] & 0xF);
    if (len != nibbles.length - 6) {
      NetworkCore.LOGGER.warn(
          "UDP payload length mismatch (expected {} got {})", len, nibbles.length - 6);
      return null;
    }
    int[] args = new int[len];
    System.arraycopy(nibbles, 5, args, 0, len);
    try {
      return switch (type) {
        case 3 -> IPv4Frame.from(code, args);
        default -> {
          NetworkCore.LOGGER.warn("Unsupported frame type {} received over UDP", type);
          yield null;
        }
      };
    } catch (IllegalArgumentException ex) {
      NetworkCore.LOGGER.warn("Failed to decode IPv4 frame: {}", ex.getMessage());
      return null;
    }
  }

  public static String getUdpAddress() {
    if (localAddress == null || udpPort == 0) {
      return "Not initialized";
    }
    return "localhost:" + udpPort;
  }

  public static int getUdpPort() {
    return udpPort;
  }

  public static byte[] getLocalIp() {
    if (localAddress != null) {
      return localAddress.getAddress();
    }
    try {
      return InetAddress.getByName("127.0.0.1").getAddress();
    } catch (UnknownHostException e) {
      return new byte[] {127, 0, 0, 1};
    }
  }

  private static int[] encodeIp(byte[] address) {
    byte[] addr = (address == null || address.length != 4) ? new byte[4] : address;
    int[] result = new int[8];
    for (int i = 0; i < 4; i++) {
      int b = addr[i] & 0xFF;
      result[i * 2] = (b >> 4) & 0xF;
      result[i * 2 + 1] = b & 0xF;
    }
    return result;
  }

  private static byte[] getLocalIpBytes() {
    if (localAddress != null) {
      return localAddress.getAddress();
    }
    if (socket != null) {
      return socket.getLocalAddress().getAddress();
    }
    return new byte[4];
  }
}
