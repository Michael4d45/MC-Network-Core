package io.github.michael4d45;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

/** Minimal stub block entity. */
public class NetworkCoreEntity extends BlockEntity implements NamedScreenHandlerFactory {

  private static final String PORT_KEY = "Port";
  private static final String SYMBOL_PERIOD_KEY = "SymbolPeriodTicks";

  /** Cached assigned network port (0 = unassigned). */
  private int port;

  /** Symbol period in ticks (1-8, default 2). */
  private int symbolPeriodTicks = 2;

  /** Tick counter for symbol processing. */
  private int tickCounter = 0;

  /** Runtime state (not persisted). */
  private final CoreRuntime runtime = new CoreRuntime();

  /** Paused state for testing. */
  private boolean paused = false;

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

  public int getSymbolPeriodTicks() {
    return symbolPeriodTicks;
  }

  public void setSymbolPeriodTicks(int symbolPeriodTicks) {
    int clamped = Math.max(1, Math.min(8, symbolPeriodTicks));
    if (this.symbolPeriodTicks != clamped) {
      this.symbolPeriodTicks = clamped;
      markDirty();
    }
  }

  public boolean isPaused() {
    return paused;
  }

  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  public CoreRuntime getRuntime() {
    return runtime;
  }

  @Override
  protected void readData(ReadView view) {
    int loaded = view.getOptionalInt(PORT_KEY).orElse(-1);
    this.port = Math.max(-1, Math.min(65535, loaded));

    int loadedPeriod = view.getOptionalInt(SYMBOL_PERIOD_KEY).orElse(2);
    this.symbolPeriodTicks = Math.max(1, Math.min(8, loadedPeriod));
  }

  @Override
  protected void writeData(WriteView view) {
    if (port >= 0) {
      view.putInt(PORT_KEY, port);
    }
    if (symbolPeriodTicks != 2) { // Only save if not default
      view.putInt(SYMBOL_PERIOD_KEY, symbolPeriodTicks);
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

  @Override
  public Text getDisplayName() {
    return Text.literal("Network Core");
  }

  @Nullable
  @Override
  public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
    return null; // Stub: no UI yet
  }

  public static void tick(World world, BlockPos pos, BlockState state, NetworkCoreEntity be) {
    if (!(world instanceof ServerWorld serverWorld)) {
      return;
    }

    // Skip ticking when:
    // 1. Explicitly paused via testing flag (be.paused)
    // 2. Singleplayer pause menu is open (integrated server paused)
    // 3. Global tick freeze is active (/tick freeze)
    if (be.paused) {
      return;
    }

    var server = serverWorld.getServer();
    // Integrated server pause (singleplayer ESC menu) - server stops most ticks, this is an extra
    // guard
    if (server.isPaused()) {
      return;
    }

    // Tick freeze (debug /tick freeze). Allow ticking when a step is being executed.
    var tickManager = server.getTickManager();
    // If the server is frozen AND not currently consuming a step tick, skip.
    // (When stepping, the manager reports it should tick even while frozen.)
    try {
      // Use reflective/defensive call in case mappings differ; fall back to isFrozen only.
      boolean shouldTick = true;
      try {
        shouldTick = (boolean) tickManager.getClass().getMethod("shouldTick").invoke(tickManager);
      } catch (ReflectiveOperationException ignored) {
        // If method absent, assume we should tick (so stepping still works) and only block when
        // fully frozen.
      }
      if (tickManager.isFrozen() && !shouldTick) {
        return;
      }
    } catch (RuntimeException t) {
      // Fail open (continue ticking) to avoid breaking debug sessions if an unexpected runtime
      // issue occurs.
    }

    int period = be.getSymbolPeriodTicks();
    be.tickCounter++;
    if (be.tickCounter >= period) {
      int transmitPower = NetworkCoreBlock.getTransmitPower(state);
      be.runtime.processTxSymbol(be, transmitPower);
      be.runtime.processRxOutput();
      int receivePower = be.runtime.getLastOutputPower();
      NetworkCoreBlock.setReceivePowering(serverWorld, pos, state, receivePower);
      be.tickCounter = 0;
    }
  }

  public void sendFrame(Frame frame) {
    runtime.sendFrame(frame);
  }
}
