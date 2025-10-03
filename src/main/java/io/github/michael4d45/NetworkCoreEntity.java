package io.github.michael4d45;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Minimal stub block entity. */
public class NetworkCoreEntity extends BlockEntity {

  private static final String PORT_KEY = "Port";

  /** Cached assigned network port (0 = unassigned). */
  private int port;

  /** Runtime state (not persisted). */
  private final CoreRuntime runtime = new CoreRuntime();

  public NetworkCoreEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.NETWORK_CORE, pos, state);
  }

  public int getPort() {
    return port;
  }

  public int getWorldId() {
    return DataRouter.getWorldId((ServerWorld) this.world);
  }

  public void setPort(int port) {
    if (!(this.world instanceof ServerWorld serverWorld)) {
      return;
    }
    int assigned = DataRouter.requestPort(serverWorld, pos, port);
    NetworkCore.LOGGER.debug("Assigned port {} to Network Core at {}", assigned, pos);
    if (this.port != assigned) {
      this.port = assigned;
      markDirty();
    }
  }

  public CoreRuntime getRuntime() {
    return runtime;
  }

  @Override
  protected void readData(ReadView view) {
    int loaded = view.getOptionalInt(PORT_KEY).orElse(-1);
    this.port = Math.max(-1, Math.min(65535, loaded));
  }

  @Override
  protected void writeData(WriteView view) {
    if (port >= 0) {
      view.putInt(PORT_KEY, port);
    }
  }

  // Called after world sets this block entity up (Fabric provides ticking/level load hooks).
  public void handleLoad() {
    if (this.world instanceof ServerWorld serverWorld) {
      if (port >= 0) {
        int assigned = DataRouter.registerExisting(serverWorld, pos, port);
        if (assigned != port) {
          port = assigned; // adjust to resolved port
          markDirty();
        }
      } else {
        int assigned = DataRouter.requestPort(serverWorld, pos, 0);
        port = assigned;
        markDirty();
      }
    }
  }

  @Override
  public void markRemoved() {
    if (this.world instanceof ServerWorld serverWorld) {
      DataRouter.release(serverWorld, pos);
    }
    super.markRemoved();
  }

  public static void tick(World world, BlockPos pos, BlockState state, NetworkCoreEntity be) {
    if (!(world instanceof ServerWorld serverWorld)) {
      return;
    }

    // Skip ticking when:
    // 1. Singleplayer pause menu is open (integrated server paused)
    // 2. Global tick freeze is active (/tick freeze)
    var server = serverWorld.getServer();
    // Integrated server pause (singleplayer ESC menu) - server stops most ticks, this is an extra
    // guard
    if (server.isPaused()) {
      return;
    }

    // Tick freeze (debug /tick freeze). Allow ticking when a step is being executed.
    var tickManager = server.getTickManager();
    if (tickManager.isFrozen() && !tickManager.shouldTick()) {
      return;
    }

    // Clock gating: only advance tickCounter when CLOCK_ACTIVE is true (level-triggered).
    boolean clockNow = state.getOrEmpty(NetworkCoreBlock.CLOCK_ACTIVE).orElse(false);
    if (clockNow) {
      int transmitPower = NetworkCoreBlock.getTransmitPower(state);
      be.runtime.processTxSymbol(be, transmitPower);
      be.runtime.processRxOutput();
      int receivePower = be.runtime.getLastOutputPower();
      NetworkCoreBlock.setReceivePowering(serverWorld, pos, state, receivePower);
    }
  }

  public void sendFrame(Frame frame) {
    runtime.sendFrame(frame);
  }
}
