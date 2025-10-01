package io.github.michael4d45;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Manages unique port assignments per world. */
public final class Router {

  private static final int MIN_PORT = 1;
  private static final int MAX_PORT = 255;
  private static final Router INSTANCE = new Router();

  private static MinecraftServer server;
  private static final Map<Identifier, Integer> worldIds = new ConcurrentHashMap<>();
  private static int nextWorldId = 0;

  private final Map<RegistryKey<World>, NetworkCorePortState> allocations =
      new ConcurrentHashMap<>();

  private Router() {}

  public static void init() {
    ServerLifecycleEvents.SERVER_STARTED.register(
        server -> {
          Router.server = server;
          loadWorldIds();
          for (ServerWorld world : server.getWorlds()) {
            Identifier id = world.getRegistryKey().getValue();
            worldIds.computeIfAbsent(id, k -> nextWorldId++);
            NetworkCore.LOGGER.info("Assigned world ID {} to world {}", worldIds.get(id), id);
            Router.loadState(world);
          }
        });
    ServerLifecycleEvents.SERVER_STOPPING.register(
        server -> {
          saveWorldIds();
          for (ServerWorld world : server.getWorlds()) {
            NetworkCore.LOGGER.info(
                "Requesting port save for world {}", world.getRegistryKey().getValue());
            Router.saveState(world);
          }
        });
  }

  public static Router getInstance() {
    return INSTANCE;
  }

  private static ServerWorld getWorldById(int id) {
    for (var entry : worldIds.entrySet()) {
      if (entry.getValue() == id) {
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, entry.getKey());
        return server.getWorld(key);
      }
    }
    return null;
  }

  public static int getWorldId(ServerWorld world) {
    return worldIds.getOrDefault(world.getRegistryKey().getValue(), -1);
  }

  public void sendFrame(Frame frame) {
    ServerWorld world = getWorldById(frame.destinationWorld);
    if (world == null) {
      NetworkCore.LOGGER.warn("Cannot send frame, invalid world ID: " + frame.destinationWorld);
      return;
    }
    NetworkCoreEntity be = getBlockEntityByPort(world, frame.destinationPort);
    if (be != null) {
      be.sendFrame(frame);
    } else {
      NetworkCore.LOGGER.warn(
          "Cannot send frame, no block found at world ID {} port {}",
          frame.destinationWorld,
          frame.destinationPort);
      NetworkCore.LOGGER.debug("Available worlds and ports:");
      for (var entry : worldIds.entrySet()) {
        NetworkCore.LOGGER.debug(" - World ID {}: {}", entry.getValue(), entry.getKey());
        for (var beEntry : allocations.entrySet()) {
          if (beEntry.getKey().getValue().equals(entry.getKey())) {
            NetworkCorePortState state = beEntry.getValue();
            for (int port = 1; port <= 255; port++) {
              if (state.byPort[port] != null) {
                NetworkCore.LOGGER.debug("   - Port {}: {}", port, state.byPort[port]);
              }
            }
            break; // Assuming one state per world
          }
        }
      }
    }
  }

  public int registerExisting(ServerWorld world, BlockPos pos, int desiredPort) {
    return getAllocation(world).claim(pos, desiredPort);
  }

  public int requestPort(ServerWorld world, BlockPos pos, int desiredPort) {
    return getAllocation(world).reassign(pos, desiredPort);
  }

  public NetworkCoreEntity getBlockEntityByPort(ServerWorld world, int port) {
    if (port < MIN_PORT || port > MAX_PORT) {
      return null;
    }
    NetworkCorePortState state = getAllocation(world);
    BlockPos pos = state.getPosByPort(port);
    if (pos == null) {
      return null;
    }
    if (world.getBlockEntity(pos) instanceof NetworkCoreEntity nbe) {
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
    NetworkCorePortState state = Router.getInstance().allocations.get(world.getRegistryKey());
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
        Router.getInstance().allocations.put(world.getRegistryKey(), state);
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

  private static void loadWorldIds() {
    Path path =
        server.getSavePath(WorldSavePath.ROOT).resolve("data").resolve("network_core_worlds.nbt");
    if (Files.exists(path)) {
      try {
        NbtCompound nbt = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
        for (String key : nbt.getKeys()) {
          nbt.getInt(key).ifPresent(id -> worldIds.put(Identifier.of(key), id));
        }
        nextWorldId = worldIds.values().stream().mapToInt(i -> i).max().orElse(-1) + 1;
      } catch (IOException e) {
        NetworkCore.LOGGER.error("Failed to load world ids", e);
      }
    }
  }

  private static void saveWorldIds() {
    Path path =
        server.getSavePath(WorldSavePath.ROOT).resolve("data").resolve("network_core_worlds.nbt");
    try {
      Files.createDirectories(path.getParent());
      NbtCompound nbt = new NbtCompound();
      for (var entry : worldIds.entrySet()) {
        nbt.putInt(entry.getKey().toString(), entry.getValue());
      }
      NbtIo.writeCompressed(nbt, path);
    } catch (IOException e) {
      NetworkCore.LOGGER.error("Failed to save world ids", e);
    }
  }
}
