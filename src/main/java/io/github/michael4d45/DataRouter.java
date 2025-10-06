package io.github.michael4d45;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Manages unique port assignments per world. */
public final class DataRouter {

  private static final int MIN_PORT = 0;
  private static final int MAX_PORT = 65535;

  public static MinecraftServer server;

  // Global port allocation state
  private static NetworkCorePortState allocation = new NetworkCorePortState();

  private DataRouter() {
    // Utility class: prevent instantiation
  }

  public static void init() {
    ServerLifecycleEvents.SERVER_STARTED.register(
        mcServer -> {
          DataRouter.server = mcServer;
          loadState();
        });
    ServerLifecycleEvents.SERVER_STOPPING.register(
        mcServer -> {
          saveState();
          // Clear static state to avoid leakage across integrated server sessions
          allocation = new NetworkCorePortState();
          DataRouter.server = null;
        });
  }

  public static void sendLocalDataFrame(NetworkCoreEntity source, DataFrame frame) {
    if (source == null || frame == null) {
      return;
    }
    World world = source.getWorld();
    if (!(world instanceof ServerWorld)) {
      return;
    }
    NetworkCoreEntity destination = getBlockEntityByPort(frame.getDstPort());
    if (destination != null && !destination.isRemoved()) {
      if (!destination.sendFrame(frame)) {
        // Queue full, send BLOCK_BUSY back to source
        emitBlockBusy(source, frame.getSrcPort());
      }
      return;
    }
    NetworkCore.LOGGER.warn("No NetworkCore listening on port {}", frame.getDstPort());
    emitPortUnreachable(source, frame.getDstPort());
  }

  public static void deliverIPv4Frame(IPv4Frame frame) {
    if (frame == null) {
      return;
    }
    if (frame.hasEncapsulatedFrame()) {
      Frame encapsulated = frame.getEncapsulatedFrame();
      switch (encapsulated) {
        case DataFrame dataFrame -> {
          NetworkCoreEntity destination = getBlockEntityByPort(dataFrame.getDstPort());
          if (destination != null && !destination.isRemoved()) {
            if (!destination.sendFrame(dataFrame)) {
              // Queue full, send TARGET_BUSY back
              emitIpv4TargetBusy(frame, dataFrame.getDstPort());
            }
          } else {
            NetworkCore.LOGGER.warn(
                "No NetworkCore listening on port {} for IPv4 delivery", dataFrame.getDstPort());
            emitIpv4PortUnreachable(frame, dataFrame.getDstPort());
          }
        }
        case DataControlFrame controlFrame -> {
          int targetPort = frame.getDstUdpPort();
          NetworkCoreEntity destination = getBlockEntityByPort(targetPort);
          if (destination != null && !destination.isRemoved()) {
            if (!destination.sendFrame(controlFrame)) {
              // Queue full, send TARGET_BUSY back
              emitIpv4TargetBusy(frame, targetPort);
            }
          } else {
            NetworkCore.LOGGER.warn(
                "No NetworkCore listening on port {} for IPv4 control frame (code={})",
                targetPort,
                controlFrame.getCode());
            emitIpv4PortUnreachable(frame, targetPort);
          }
        }
        case StatusFrame statusFrame -> // Status frame received over IPv4 - log it
            NetworkCore.LOGGER.info("Received status over IPv4: {}", statusFrame);
        default ->
            NetworkCore.LOGGER.warn(
                "IPv4 frame encapsulates unsupported frame type: {}", encapsulated);
      }
    }
  }

  public static int registerExisting(BlockPos pos, int desiredPort) {
    return allocation.claim(pos, desiredPort);
  }

  public static int requestPort(BlockPos pos, int desiredPort) {
    return allocation.reassign(pos, desiredPort);
  }

  public static NetworkCoreEntity getBlockEntityByPort(int port) {
    if (port < MIN_PORT || port > MAX_PORT) {
      return null;
    }
    BlockPos pos = allocation.getPosByPort(port);
    if (pos == null) {
      return null;
    }
    for (ServerWorld world : server.getWorlds()) {
      if (world.getBlockEntity(pos) instanceof NetworkCoreEntity nbe) {
        if (nbe.getPort() == port) {
          return nbe;
        }
      }
    }
    // Block entity missing or port changed; clean up allocation
    allocation.release(pos);
    return null;
  }

  public static void release(BlockPos pos) {
    allocation.release(pos);
  }

  public static Map<BlockPos, Integer> getAllocatedPorts(ServerWorld world) {
    Map<BlockPos, Integer> result = new HashMap<>();
    for (var entry : allocation.byPos.entrySet()) {
      BlockPos pos = entry.getKey();
      int port = entry.getValue();
      if (world.getBlockEntity(pos) instanceof NetworkCoreEntity) {
        result.put(pos, port);
      }
    }
    return result;
  }

  public static void saveState() {
    int count = allocation.byPos.size();
    if (count > 0) {
      NetworkCore.LOGGER.info("Saving {} network core port allocation(s)", count);
    }
    Path path = getStateFile();
    try {
      Files.createDirectories(path.getParent());
      NbtIo.writeCompressed(allocation.writeNbt(), path);
    } catch (IOException e) {
      NetworkCore.LOGGER.error("Failed to save network core ports", e);
    }
  }

  public static void loadState() {
    Path path = getStateFile();
    Path legacyPath = getLegacyStateFile();
    boolean migratedFromLegacy = false;
    if (!Files.exists(path) && Files.exists(legacyPath)) {
      path = legacyPath;
      migratedFromLegacy = true;
    }
    if (Files.exists(path)) {
      try {
        NbtCompound nbt = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
        allocation = NetworkCorePortState.fromNbt(nbt);
        int count = allocation.byPos.size();
        if (count > 0) {
          NetworkCore.LOGGER.info("Loaded {} network core port allocation(s)", count);
        }
        if (migratedFromLegacy) {
          saveState();
          try {
            Files.deleteIfExists(legacyPath);
          } catch (IOException e) {
            // Ignore deletion failure
          }
        }
      } catch (IOException e) {
        NetworkCore.LOGGER.error("Failed to load network core ports", e);
      }
    }
  }

  private static Path getStateFile() {
    return server.getSavePath(WorldSavePath.ROOT).resolve("data").resolve("network_core_ports.nbt");
  }

  private static Path getLegacyStateFile() {
    // Legacy path used in earlier versions (no longer used, kept for migration)
    return server.getSavePath(WorldSavePath.ROOT).resolve("network_core_ports.dat");
  }

  private static void emitPortUnreachable(NetworkCoreEntity source, int port) {
    if (source == null) {
      return;
    }
    int safePort = Math.max(0, Math.min(0xFFFF, port));
    int[] args = {
      (safePort >> 12) & 0xF, (safePort >> 8) & 0xF, (safePort >> 4) & 0xF, safePort & 0xF
    };
    source.sendFrame(new DataControlFrame(0x1, args));
  }

  private static void emitBlockBusy(NetworkCoreEntity source, int port) {
    if (source == null) {
      return;
    }
    int safePort = Math.max(0, Math.min(0xFFFF, port));
    int[] args = {
      (safePort >> 12) & 0xF, (safePort >> 8) & 0xF, (safePort >> 4) & 0xF, safePort & 0xF
    };
    source.sendFrame(new DataControlFrame(0x3, args));
  }

  private static void emitIpv4PortUnreachable(IPv4Frame frame, int port) {
    if (port < 0) {
      return;
    }
    int safePort = Math.max(0, Math.min(0xFFFF, port));
    IPv4Router.sendControlFrameTo(
        frame.getSrcIp(),
        frame.getSrcUdpPort(),
        frame.getDstIp(),
        frame.getDstUdpPort(),
        0x2,
        encodePort(safePort));
  }

  private static void emitIpv4TargetBusy(IPv4Frame frame, int port) {
    if (port < 0) {
      return;
    }
    int safePort = Math.max(0, Math.min(0xFFFF, port));
    IPv4Router.sendControlFrameTo(
        frame.getSrcIp(),
        frame.getSrcUdpPort(),
        frame.getDstIp(),
        frame.getDstUdpPort(),
        0x7,
        encodePort(safePort));
  }

  private static int[] encodePort(int port) {
    return new int[] {(port >> 12) & 0xF, (port >> 8) & 0xF, (port >> 4) & 0xF, port & 0xF};
  }
}
