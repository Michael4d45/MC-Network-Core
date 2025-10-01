package io.github.michael4d45;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

/** Persistent state for network core port allocations per world. */
public class NetworkCorePortState {
  final BlockPos[] byPort = new BlockPos[65536]; // index 0..65535
  final Map<BlockPos, Integer> byPos = new HashMap<>();

  public NetworkCorePortState() {}

  public static NetworkCorePortState fromNbt(NbtCompound nbt) {
    NetworkCorePortState state = new NetworkCorePortState();
    NbtList list = nbt.getList("Ports").orElse(new NbtList());
    for (int i = 0; i < list.size(); i++) {
      NbtCompound c = list.getCompound(i).orElse(null);
      if (c != null) {
        int port = c.getByte("Port").orElse((byte) 0) & 0xFF;
        if (port >= 0 && port <= 65535) {
          BlockPos pos =
              new BlockPos(
                  c.getInt("X").orElse(0), c.getInt("Y").orElse(0), c.getInt("Z").orElse(0));
          state.byPort[port] = pos;
          state.byPos.put(pos, port);
        }
      }
    }
    return state;
  }

  public NbtCompound writeNbt() {
    NbtList list = new NbtList();
    for (int i = 0; i < byPort.length; i++) {
      BlockPos pos = byPort[i];
      if (pos != null) {
        NbtCompound c = new NbtCompound();
        c.putByte("Port", (byte) i);
        c.putInt("X", pos.getX());
        c.putInt("Y", pos.getY());
        c.putInt("Z", pos.getZ());
        list.add(c);
      }
    }
    NbtCompound nbt = new NbtCompound();
    nbt.put("Ports", list);
    return nbt;
  }

  public synchronized int claim(BlockPos pos, int desiredPort) {
    BlockPos key = pos.toImmutable();
    Integer existing = byPos.get(key);
    if (existing != null) {
      return existing;
    }
    int candidate = clamp(desiredPort);
    if (candidate >= 0) {
      BlockPos owner = byPort[candidate];
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

  public synchronized int reassign(BlockPos pos, int desiredPort) {
    BlockPos key = pos.toImmutable();
    Integer currentObj = byPos.get(key);
    if (currentObj == null) {
      return claim(pos, desiredPort);
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
    BlockPos owner = byPort[candidate];
    int newPort = candidate;
    if (owner != null && !owner.equals(key)) {
      int fallback = findAvailablePort();
      if (fallback == -1) {
        return current;
      }
      newPort = fallback;
      NetworkCore.LOGGER.info(
          "Port {} already in use. Reassigning block at {} to port {}.", candidate, key, newPort);
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

  public synchronized void release(BlockPos pos) {
    BlockPos key = pos.toImmutable();
    Integer current = byPos.remove(key);
    if (current != null && current >= 0) {
      BlockPos owner = byPort[current];
      if (owner != null && owner.equals(key)) {
        byPort[current] = null;
      }
    }
  }

  public synchronized BlockPos getPosByPort(int port) {
    if (port < 0 || port > 65535) {
      return null;
    }
    BlockPos pos = byPort[port];
    if (pos != null) {
      // Lazy cleanup: check if the block entity still exists and has this port
      // But since we can't access world here, we'll assume it's correct or clean on lookup failure
      return pos;
    }
    return null;
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
