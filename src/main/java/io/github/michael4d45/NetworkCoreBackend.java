package io.github.michael4d45;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Backend for tracking Network Core blocks and ticking protocol state. */
public final class NetworkCoreBackend {
  private static final NetworkCoreBackend INSTANCE = new NetworkCoreBackend();

  private NetworkCoreBackend() {}

  public static NetworkCoreBackend getInstance() {
    return INSTANCE;
  }

  public void register(World world, BlockPos pos) {
    if (world instanceof ServerWorld serverWorld) {
      register(serverWorld, pos);
    }
  }

  public void unregister(World world, BlockPos pos) {
    if (world instanceof ServerWorld serverWorld) {
      unregister(serverWorld, pos);
    }
  }

  private void register(ServerWorld world, BlockPos pos) {
    // TODO: implement real registration logic
  }

  private void unregister(ServerWorld world, BlockPos pos) {
    // TODO: implement real unregistration logic
  }
}
