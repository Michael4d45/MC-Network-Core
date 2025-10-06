package io.github.michael4d45;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Persistent state for network core port allocations. Ports are world-agnostic (apply to entire
 * Minecraft instance, not per-world). Each allocation stores both BlockPos and dimension ID to
 * disambiguate identical coordinates across dimensions (Overworld/Nether/End).
 */
public class NetworkCorePortState {
  // Allocation entry combining position and dimension
  public static class PortAllocation {
    final BlockPos pos;
    final RegistryKey<World> dimension;

    PortAllocation(BlockPos pos, RegistryKey<World> dimension) {
      this.pos = pos.toImmutable();
      this.dimension = dimension;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof PortAllocation other)) return false;
      return pos.equals(other.pos) && dimension.equals(other.dimension);
    }

    @Override
    public int hashCode() {
      return 31 * pos.hashCode() + dimension.hashCode();
    }
  }

  final PortAllocation[] byPort = new PortAllocation[65536]; // index 0..65535
  final Map<PortAllocation, Integer> byPos = new HashMap<>();

  public NetworkCorePortState() {}

  public static NetworkCorePortState fromNbt(NbtCompound nbt) {
    NetworkCorePortState state = new NetworkCorePortState();
    NbtList list = nbt.getList("Ports").orElse(new NbtList());
    for (int i = 0; i < list.size(); i++) {
      NbtCompound c = list.getCompound(i).orElse(null);
      if (c != null) {
        int port = c.getInt("Port").orElse(-1);
        if (port < 0) {
          port = c.getByte("Port").map(b -> b & 0xFF).orElse(-1);
        }
        if (port >= 0 && port <= 65535) {
          BlockPos pos =
              new BlockPos(
                  c.getInt("X").orElse(0), c.getInt("Y").orElse(0), c.getInt("Z").orElse(0));
          // Migration: if no Dimension field, assume Overworld
          String dimStr = c.getString("Dimension").orElse("minecraft:overworld");
          RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(dimStr));
          PortAllocation alloc = new PortAllocation(pos, dimension);
          state.byPort[port] = alloc;
          state.byPos.put(alloc, port);
        }
      }
    }
    return state;
  }

  public NbtCompound writeNbt() {
    NbtList list = new NbtList();
    for (int i = 0; i < byPort.length; i++) {
      PortAllocation alloc = byPort[i];
      if (alloc != null) {
        NbtCompound c = new NbtCompound();
        c.putInt("Port", i);
        c.putInt("X", alloc.pos.getX());
        c.putInt("Y", alloc.pos.getY());
        c.putInt("Z", alloc.pos.getZ());
        c.putString("Dimension", alloc.dimension.getValue().toString());
        list.add(c);
      }
    }
    NbtCompound nbt = new NbtCompound();
    nbt.put("Ports", list);
    return nbt;
  }

  public synchronized int claim(BlockPos pos, RegistryKey<World> dimension, int desiredPort) {
    PortAllocation key = new PortAllocation(pos, dimension);
    Integer existing = byPos.get(key);
    if (existing != null) {
      return existing;
    }
    int candidate = clamp(desiredPort);
    if (candidate >= 0) {
      PortAllocation owner = byPort[candidate];
      if (owner == null || owner.equals(key)) {
        byPort[candidate] = key;
        byPos.put(key, candidate);
        return candidate;
      }
    }
    int fallback = findAvailablePort();
    int assigned = fallback != -1 ? fallback : -1;
    if (assigned >= 0) {
      byPort[assigned] = key;
      byPos.put(key, assigned);
    } else {
      byPos.put(key, -1);
    }
    return assigned;
  }

  public synchronized int reassign(BlockPos pos, RegistryKey<World> dimension, int desiredPort) {
    PortAllocation key = new PortAllocation(pos, dimension);
    Integer currentObj = byPos.get(key);
    if (currentObj == null) {
      return claim(pos, dimension, desiredPort);
    }
    int current = currentObj;
    int candidate = clamp(desiredPort);
    if (candidate < 0) {
      if (current >= 0) {
        byPort[current] = null;
      }
      byPos.put(key, -1);
      return -1;
    }
    PortAllocation owner = byPort[candidate];
    int newPort = candidate;
    if (owner != null && !owner.equals(key)) {
      int fallback = findAvailablePort();
      if (fallback == -1) {
        return current;
      }
      newPort = fallback;
      NetworkCore.LOGGER.info(
          "Port {} already in use. Reassigning block at {} in {} to port {}.",
          candidate,
          key.pos,
          key.dimension.getValue(),
          newPort);
    }
    if (newPort == current) {
      return current;
    }
    if (current >= 0) {
      byPort[current] = null;
    }
    byPort[newPort] = key;
    byPos.put(key, newPort);
    return newPort;
  }

  public synchronized void release(BlockPos pos, RegistryKey<World> dimension) {
    PortAllocation key = new PortAllocation(pos, dimension);
    Integer current = byPos.remove(key);
    if (current != null && current >= 0) {
      PortAllocation owner = byPort[current];
      if (owner != null && owner.equals(key)) {
        byPort[current] = null;
      }
    }
  }

  public synchronized PortAllocation getAllocationByPort(int port) {
    if (port < 0 || port > 65535) {
      return null;
    }
    return byPort[port];
  }

  private int findAvailablePort() {
    for (int port = 0; port <= 65535; port++) {
      if (byPort[port] == null) {
        return port;
      }
    }
    return -1;
  }

  private static int clamp(int port) {
    if (port < 0) {
      return 0;
    }
    if (port > 65535) {
      return 65535;
    }
    return port;
  }
}
