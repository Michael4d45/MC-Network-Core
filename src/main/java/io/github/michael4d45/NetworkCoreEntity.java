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

  /** Cached assigned network port (-1 = unassigned). */
  private int port;

  /** Runtime state (not persisted). */
  private final CoreRuntime runtime = new CoreRuntime();

  /** Flag to indicate port needs registration on first tick after load. */
  private boolean needsPortRegistration = false;

  public NetworkCoreEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.NETWORK_CORE, pos, state);
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    if (!(getWorld() instanceof ServerWorld serverWorld)) {
      NetworkCore.LOGGER.warn("Cannot set port on client side");
      return;
    }
    int assigned = DataRouter.requestPort(pos, serverWorld, port);
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
    // Mark that we need to register this port with the allocator on first tick
    // (when server/world are available)
    if (this.port >= 0) {
      this.needsPortRegistration = true;
    }
  }

  @Override
  protected void writeData(WriteView view) {
    if (port >= 0) {
      view.putInt(PORT_KEY, port);
    }
  }

  public static void tick(World world, BlockPos pos, BlockState state, NetworkCoreEntity be) {
    if (!(world instanceof ServerWorld serverWorld)) {
      return;
    }

    // Handle port registration after load, before any other processing
    if (be.needsPortRegistration) {
      be.needsPortRegistration = false;
      int reconciledPort = DataRouter.registerExisting(pos, serverWorld, be.port);
      if (reconciledPort != be.port) {
        NetworkCore.LOGGER.info(
            "Port conflict during load: requested {} but assigned {} at {}",
            be.port,
            reconciledPort,
            pos);
        be.port = reconciledPort;
        be.markDirty();
      }
    }

    // Skip ticking when:
    // 1. Odd ticks (redstone ticks are every 2 game ticks)
    // 2. Singleplayer pause menu is open (integrated server paused)
    // 3. Global tick freeze is active (/tick freeze)
    var server = serverWorld.getServer();
    var tickManager = server.getTickManager();
    if (world.getTime() % 2 != 0
        || server.isPaused()
        || (tickManager.isFrozen() && !tickManager.shouldTick())) {
      return;
    }

    // Clock gating: only advance tickCounter when CLOCK_ACTIVE is true (level-triggered).
    if (!state.getOrEmpty(NetworkCoreBlock.CLOCK_ACTIVE).orElse(false)) {
      return;
    }

    int transmitPower = state.get(NetworkCoreBlock.TRANSMIT_POWERED);
    be.runtime.processTxSymbol(be, transmitPower);
    be.runtime.processRxOutput();
    int receivePower = be.runtime.getLastOutputPower();
    NetworkCoreBlock.setReceivePowering(serverWorld, pos, state, receivePower);
  }

  public boolean sendFrame(Frame frame) {
    return runtime.sendFrame(frame);
  }

  public void processRemoteDataControlFrame(
      DataControlFrame frame, byte[] srcIp, int srcUdpPort, byte[] dstIp, int dstUdpPort) {
    runtime.processRemoteDataControlFrame(frame, srcIp, srcUdpPort, dstIp, dstUdpPort, getPort());
  }
}
