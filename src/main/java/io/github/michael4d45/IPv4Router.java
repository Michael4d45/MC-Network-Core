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
          // Send HOST_UNREACHABLE back
          IPv4ControlFrame errorFrame =
              new IPv4ControlFrame(
                  0x1, // HOST_UNREACHABLE
                  ipv4Frame.getSrcIp(),
                  getLocalIpBytes(),
                  encodeIp(ipv4Frame.getDstIp()), // unreachable IP
                  ipv4Frame.getSrcUdpPort(),
                  udpPort);
          sendControlFrame(errorFrame);
        }
      }
      case IPv4ControlFrame ipv4ControlFrame -> sendControlFrame(ipv4ControlFrame);
      default ->
          NetworkCore.LOGGER.warn(
              "Unsupported frame type for IPv4Router: {}", frame.getClass().getSimpleName());
    }
  }

  public static void sendControlFrameTo(
      byte[] dstIp, int dstUdpPort, byte[] srcIp, int srcUdpPort, int code, int[] payloadArgs) {
    if (socket == null) {
      NetworkCore.LOGGER.warn("IPv4Router not initialized, cannot send control frame");
      return;
    }
    if (dstIp == null || dstIp.length != 4) {
      NetworkCore.LOGGER.warn("Invalid destination IP for IPv4 control frame");
      return;
    }
    byte[] resolvedSrc = (srcIp == null || srcIp.length != 4) ? getLocalIpBytes() : srcIp.clone();
    IPv4ControlFrame frame =
        new IPv4ControlFrame(code, dstIp, resolvedSrc, payloadArgs, dstUdpPort, srcUdpPort);
    sendControlFrame(frame);
  }

  private static void receiveLoop() {
    byte[] buffer = new byte[1024]; // Max payload size
    while (running) {
      try {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());

        int[] nibbles = bytesToNibbles(data);
        Frame frame = parseFrameFromNibbles(nibbles, packet.getPort(), udpPort);
        if (frame == null) {
          // Send PARAMETER_PROBLEM back to sender
          IPv4ControlFrame errorFrame =
              new IPv4ControlFrame(
                  0x5, // PARAMETER_PROBLEM
                  packet.getAddress().getAddress(),
                  getLocalIpBytes(),
                  new int[0], // no args
                  packet.getPort(),
                  udpPort);
          sendControlFrame(errorFrame);
          continue;
        }
        NetworkCore.LOGGER.info("Received UDP packet, parsed frame {}", frame);
        Frame finalFrame = frame;
        DatagramPacket finalPacket = packet;
        var srv = DataRouter.server;
        if (srv != null) {
          srv.execute(
              () -> {
                switch (finalFrame) {
                  case IPv4Frame ipv4Frame -> DataRouter.deliverIPv4Frame(ipv4Frame);
                  case IPv4ControlFrame ipv4ControlFrame ->
                      handleIPv4ControlFrame(
                          ipv4ControlFrame, finalPacket.getAddress(), finalPacket.getPort());
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

  private static boolean sendUdp(int[] symbols, InetAddress address, int port) {
    try {
      byte[] data = nibblesToBytes(symbols);
      DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
      socket.send(packet);
      NetworkCore.LOGGER.info("Sent UDP packet to {}:{} with {} bytes", address, port, data.length);
      return true;
    } catch (IOException e) {
      NetworkCore.LOGGER.error("Failed to send UDP packet", e);
      return false;
    }
  }

  private static String formatIpFromArgs(int[] args) {
    if (args.length < 8) return "invalid";
    return String.format(
        "%d.%d.%d.%d",
        (args[0] << 4) | args[1],
        (args[2] << 4) | args[3],
        (args[4] << 4) | args[5],
        (args[6] << 4) | args[7]);
  }

  private static int formatPortFromArgs(int[] args) {
    if (args.length < 4) return -1;
    return (args[0] << 12) | (args[1] << 8) | (args[2] << 4) | args[3];
  }

  private static void handleIPv4ControlFrame(
      IPv4ControlFrame frame, InetAddress remoteAddr, int remotePort) {
    switch (frame.getCode()) {
      case 0x0 -> // NETWORK_UNREACHABLE
          NetworkCore.LOGGER.warn(
              "Received NETWORK_UNREACHABLE from {}:{} targeting {}",
              remoteAddr,
              remotePort,
              formatIpFromArgs(frame.getArgs()));
      case 0x1 -> // HOST_UNREACHABLE
          NetworkCore.LOGGER.warn(
              "Received HOST_UNREACHABLE from {}:{} - {}",
              remoteAddr,
              remotePort,
              formatIpFromArgs(frame.getArgs()));
      case 0x2 -> // PORT_UNREACHABLE
          NetworkCore.LOGGER.warn(
              "Received PORT_UNREACHABLE from {}:{} - port {}",
              remoteAddr,
              remotePort,
              formatPortFromArgs(frame.getArgs()));
      case 0x3 -> { // ECHO_REQUEST
        IPv4ControlFrame reply =
            new IPv4ControlFrame(
                0x4, frame.getSrcIp(), getLocalIpBytes(), frame.getArgs(), remotePort, udpPort);
        sendControlFrame(reply);
      }
      case 0x4 -> // ECHO_REPLY
          NetworkCore.LOGGER.info(
              "Received ECHO_REPLY from {}:{} with payload {}",
              remoteAddr,
              remotePort,
              Arrays.toString(frame.getArgs()));
      case 0x5 -> // PARAMETER_PROBLEM
          NetworkCore.LOGGER.warn("Received PARAMETER_PROBLEM from {}:{}", remoteAddr, remotePort);
      case 0x6 -> { // MODEQ
        // Send back status information
        int errorFlags = (socket == null || !running) ? 1 : 0; // 1 if not initialized
        StatusFrame statusFrame = new StatusFrame(udpPort, 0, 0, errorFlags);
        IPv4Frame responseFrame =
            new IPv4Frame(frame.getSrcIp(), remotePort, getLocalIpBytes(), udpPort, statusFrame);
        sendUdp(
            responseFrame.getDstIp(), responseFrame.getDstUdpPort(), responseFrame.buildSymbols());
      }
      case 0x7 -> // TARGET_BUSY
          NetworkCore.LOGGER.warn(
              "Received TARGET_BUSY from {}:{} - port {}",
              remoteAddr,
              remotePort,
              formatPortFromArgs(frame.getArgs()));
      default ->
          NetworkCore.LOGGER.warn(
              "Received unknown IPv4 control code {} from {}:{}",
              frame.getCode(),
              remoteAddr,
              remotePort);
    }
  }

  private static void sendControlFrame(IPv4ControlFrame frame) {
    try {
      InetAddress address = InetAddress.getByAddress(frame.getDstIp());
      int port = frame.getDstUdpPort();
      if (!sendUdp(frame.buildSymbols(), address, port)) {
        NetworkCore.LOGGER.error("Failed to send IPv4 control frame to {}:{}", address, port);
      }
    } catch (UnknownHostException e) {
      NetworkCore.LOGGER.error("Invalid destination IP for IPv4 control frame", e);
    }
  }

  // Parses frame from UDP nibbles. Note: IPv4ControlFrame UDP port metadata (dstUdpPort,
  // srcUdpPort)
  // is extracted from the DatagramPacket and passed to the from() method for proper routing.
  private static Frame parseFrameFromNibbles(int[] nibbles, int remoteSrcPort, int localDstPort) {
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
        case 4 -> IPv4ControlFrame.from(code, args, localDstPort, remoteSrcPort);
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
