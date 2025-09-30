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

import org.jetbrains.annotations.Nullable;

/** Minimal stub block entity. */
public class NetworkCoreBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
  private static final String PORT_KEY = "Port";
  private static final String SYMBOL_PERIOD_KEY = "SymbolPeriodTicks";

  /** Cached assigned network port (0 = unassigned). */
  private int port;

  /** Symbol period in ticks (1-8, default 2). */
  private int symbolPeriodTicks = 2;

  public NetworkCoreBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.NETWORK_CORE, pos, state);
  }

  public int getPort() {
    return port;
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
        int assigned = PortManager.getInstance().registerExisting(serverWorld, pos, port);
        if (assigned != port) {
          port = assigned; // adjust to resolved port
          markDirty();
        }
      } else {
        int assigned = PortManager.getInstance().requestPort(serverWorld, pos, 0);
        port = assigned;
        markDirty();
      }
      NetworkCoreBackend.getInstance().register(serverWorld, pos);
    }
  }

  @Override
  public void markRemoved() {
    if (this.world instanceof ServerWorld serverWorld) {
      PortManager.getInstance().release(serverWorld, pos);
      NetworkCoreBackend.getInstance().unregister(serverWorld, pos);
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
}
