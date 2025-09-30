package io.github.michael4d45;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import org.jetbrains.annotations.Nullable;

/** Minimal stub block entity. */
public class NetworkCoreBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
  public NetworkCoreBlockEntity(BlockPos pos, BlockState state) {
    super(NetworkCore.NETWORK_CORE_BLOCK_ENTITY_TYPE, pos, state);
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
