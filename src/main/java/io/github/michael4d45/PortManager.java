package io.github.michael4d45;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Manages unique port assignments per world. */
public final class PortManager {
  private static final int MIN_PORT = 1;
  private static final int MAX_PORT = 255;
  private static final PortManager INSTANCE = new PortManager();

  private final Map<RegistryKey<World>, NetworkCorePortState> allocations =
      new ConcurrentHashMap<>();

  private PortManager() {}

  public static PortManager getInstance() {
    return INSTANCE;
  }

  public int registerExisting(ServerWorld world, BlockPos pos, int desiredPort) {
    return getAllocation(world).claim(pos, desiredPort);
  }

  public int requestPort(ServerWorld world, BlockPos pos, int desiredPort) {
    return getAllocation(world).reassign(pos, desiredPort);
  }

  public NetworkCoreBlockEntity getBlockEntityByPort(ServerWorld world, int port) {
    if (port < MIN_PORT || port > MAX_PORT) {
      return null;
    }
    NetworkCorePortState state = getAllocation(world);
    BlockPos pos = state.getPosByPort(port);
    if (pos == null) {
      return null;
    }
    if (world.getBlockEntity(pos) instanceof NetworkCoreBlockEntity nbe) {
      if (nbe.getPort() == port) {
        return nbe;
      }
    }
    // Block entity missing or port changed; clean up allocation
    state.release(pos);
    return null;
  }

  public void release(ServerWorld world, BlockPos pos) {
    getAllocation(world).release(pos);
  }

  private NetworkCorePortState getAllocation(ServerWorld world) {
    RegistryKey<World> key = world.getRegistryKey();
    return allocations.computeIfAbsent(key, k -> new NetworkCorePortState());
  }

  public static void saveState(ServerWorld world) {
    NetworkCorePortState state = PortManager.getInstance().allocations.get(world.getRegistryKey());
    if (state != null) {
      Path path = getStateFile(world);
      try {
        Files.createDirectories(path.getParent());
        NbtIo.writeCompressed(state.writeNbt(), path);
      } catch (IOException e) {
        NetworkCore.LOGGER.error(
            "Failed to save network core ports for world {}", world.getRegistryKey().getValue(), e);
      }
    }
  }

  public static void loadState(ServerWorld world) {
    Path path = getStateFile(world);
    Path legacyPath = getLegacyStateFile(world);
    boolean migratedFromLegacy = false;
    if (!Files.exists(path) && Files.exists(legacyPath)) {
      path = legacyPath;
      migratedFromLegacy = true;
    }
    if (Files.exists(path)) {
      try {
        NbtCompound nbt = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
        NetworkCorePortState state = NetworkCorePortState.fromNbt(nbt);
        PortManager.getInstance().allocations.put(world.getRegistryKey(), state);
        if (migratedFromLegacy) {
          saveState(world);
          try {
            Files.deleteIfExists(legacyPath);
          } catch (IOException e) {
            // Ignore deletion failure
          }
        }
      } catch (IOException e) {
        NetworkCore.LOGGER.error(
            "Failed to load network core ports for world {}", world.getRegistryKey().getValue(), e);
      }
    }
  }

  private static Path getStateFile(ServerWorld world) {
    Identifier id = world.getRegistryKey().getValue();
    return world
        .getServer()
        .getSavePath(WorldSavePath.ROOT)
        .resolve("data")
        .resolve("network_core_ports")
        .resolve(id.getNamespace())
        .resolve(id.getPath() + ".nbt");
  }

  private static Path getLegacyStateFile(ServerWorld world) {
    return world
        .getServer()
        .getSavePath(WorldSavePath.ROOT)
        .resolve("data")
        .resolve("network_core_ports.nbt");
  }
}
