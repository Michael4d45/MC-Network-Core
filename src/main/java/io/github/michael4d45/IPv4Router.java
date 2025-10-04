package io.github.michael4d45;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
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

  public static void sendFrame(IPv4Frame frame) {
    if (socket == null) {
      NetworkCore.LOGGER.warn("IPv4Router not initialized, cannot send frame");
      return;
    }
    try {
      byte[] data = nibblesToBytes(frame.buildSymbols());
      InetAddress address = InetAddress.getByAddress(frame.getDstIp());
      DatagramPacket packet = new DatagramPacket(data, data.length, address, frame.dstUdpPort);
      socket.send(packet);
      NetworkCore.LOGGER.info(
          "Sent UDP packet to {}:{} with {} bytes", address, frame.dstUdpPort, data.length);
    } catch (IOException e) {
      NetworkCore.LOGGER.error("Failed to send UDP packet", e);
    }
  }

  private static void receiveLoop() {
    byte[] buffer = new byte[1024]; // Max payload size
    while (running) {
      try {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());

        IPv4Frame frame = IPv4Frame.fromSymbols(bytesToNibbles(data));
        NetworkCore.LOGGER.info("Received UDP packet, parsed frame {}", frame);
        DataRouter.server.execute(() -> DataRouter.sendFrame(frame));
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

  public static String getUdpAddress() {
    if (localAddress == null || udpPort == 0) {
      return "Not initialized";
    }
    return "localhost:" + udpPort;
  }
}
