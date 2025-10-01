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
    return Router.getWorldId((ServerWorld) this.world);
  }

  public void setPort(int port) {
    if (this.port != port) {
      this.port = port;
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
    int loaded = view.getOptionalInt(PORT_KEY).orElse(0);
    this.port = Math.max(0, Math.min(255, loaded));

    int loadedPeriod = view.getOptionalInt(SYMBOL_PERIOD_KEY).orElse(2);
    this.symbolPeriodTicks = Math.max(1, Math.min(8, loadedPeriod));
  }

  @Override
  protected void writeData(WriteView view) {
    if (port > 0) {
      view.putInt(PORT_KEY, port);
    }
    if (symbolPeriodTicks != 2) { // Only save if not default
      view.putInt(SYMBOL_PERIOD_KEY, symbolPeriodTicks);
    }
  }

  // Called after world sets this block entity up (Fabric provides ticking/level load hooks).
  public void handleLoad() {
    if (this.world instanceof ServerWorld serverWorld) {
      if (port > 0) {
        int assigned = Router.getInstance().registerExisting(serverWorld, pos, port);
        if (assigned != port) {
          port = assigned; // adjust to resolved port
          markDirty();
        }
      } else {
        int assigned = Router.getInstance().requestPort(serverWorld, pos, 0);
        port = assigned;
        markDirty();
      }
    }
  }

  @Override
  public void markRemoved() {
    if (this.world instanceof ServerWorld serverWorld) {
      Router.getInstance().release(serverWorld, pos);
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

    if (be.paused) {
      return; // Skip processing when paused for testing
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
