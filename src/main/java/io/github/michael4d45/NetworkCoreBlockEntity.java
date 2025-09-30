package io.github.michael4d45;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import org.jetbrains.annotations.Nullable;

/** Minimal stub block entity. */
public class NetworkCoreBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
  private static final String PORT_KEY = "Port";

  /** Cached assigned network port (0 = unassigned). */
  private int port;

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

  // Newer 1.21.6+ lifecycle methods
  public void readData(NbtCompound nbt) {
    if (nbt.contains(PORT_KEY)) {
      int loaded = nbt.getInt(PORT_KEY).orElse(0);
      if (loaded < 0) loaded = 0;
      if (loaded > 255) loaded = 255;
      this.port = loaded;
    }
  }

  protected void writeData(NbtCompound nbt) {
    if (port > 0) {
      nbt.putInt(PORT_KEY, port);
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
