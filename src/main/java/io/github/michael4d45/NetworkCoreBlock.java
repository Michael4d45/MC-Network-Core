package io.github.michael4d45;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

/** Network Core block. */
public class NetworkCoreBlock extends BlockWithEntity {

  public static final EnumProperty<Direction> FACING = Properties.FACING;
  public static final IntProperty RECEIVE_POWERED = IntProperty.of("receive_powered", 0, 15);
  public static final IntProperty TRANSMIT_POWERED = IntProperty.of("transmit_powered", 0, 15);
  public static final BooleanProperty RECEIVE_ACTIVE = BooleanProperty.of("receive_active");
  public static final BooleanProperty TRANSMIT_ACTIVE = BooleanProperty.of("transmit_active");
  public static final MapCodec<NetworkCoreBlock> CODEC = createCodec(NetworkCoreBlock::new);

  public NetworkCoreBlock(Settings settings) {
    super(settings);
    this.setDefaultState(
        this.stateManager
            .getDefaultState()
            .with(FACING, Direction.NORTH)
            .with(RECEIVE_POWERED, 0)
            .with(TRANSMIT_POWERED, 0)
            .with(RECEIVE_ACTIVE, false)
            .with(TRANSMIT_ACTIVE, false));
  }

  public static int getTransmitPower(BlockState state) {
    return state.get(TRANSMIT_POWERED);
  }

  @Override
  protected MapCodec<? extends BlockWithEntity> getCodec() {
    return CODEC;
  }

  @Nullable
  @Override
  public BlockState getPlacementState(ItemPlacementContext ctx) {
    Direction direction = ctx.getPlayerLookDirection();
    int transmitPowered = 0;
    return this.getDefaultState()
        .with(FACING, direction)
        .with(RECEIVE_POWERED, 0)
        .with(TRANSMIT_POWERED, transmitPowered)
        .with(RECEIVE_ACTIVE, false)
        .with(TRANSMIT_ACTIVE, transmitPowered > 0);
  }

  /**
   * Updates the level + active properties if they differ. Returns true if the block state was
   * actually changed (so callers can decide whether to notify neighbours). We purposefully do NOT
   * spam manual neighbour updates here; callers should only notify when a change occurred to avoid
   * redstone update loops (triggering the chain restricted neighbour update spam).
   */
  private static boolean updateLevelAndActiveIfNeeded(
      World world,
      BlockPos pos,
      BlockState state,
      IntProperty levelProp,
      BooleanProperty activeProp,
      int newLevel) {
    if (world.isClient) {
      return false;
    }
    boolean newActive = newLevel > 0;
    if (state.get(levelProp) == newLevel && state.get(activeProp) == newActive) {
      return false; // no change
    }
    // Only change state – rely on flags to propagate to clients; neighbour notification will be
    // handled explicitly by caller when needed.
    world.setBlockState(
        pos, state.with(levelProp, newLevel).with(activeProp, newActive), Block.NOTIFY_LISTENERS);
    return true;
  }

  @Override
  public void onPlaced(
      World world,
      BlockPos pos,
      BlockState state,
      @Nullable LivingEntity placer,
      ItemStack itemStack) {
    if (!world.isClient) {
      updateTransmitPowering(world, pos, state);
      BlockEntity be = world.getBlockEntity(pos);
      if (be instanceof NetworkCoreEntity core) {
        core.setPort(0);
      }
    }
    super.onPlaced(world, pos, state, placer, itemStack);
  }

  @Override
  public void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
    if (!world.isClient) {
      DataRouter.release(world, pos);
    }
    super.onStateReplaced(state, world, pos, moved);
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(FACING, RECEIVE_POWERED, TRANSMIT_POWERED, RECEIVE_ACTIVE, TRANSMIT_ACTIVE);
  }

  @Override
  protected void neighborUpdate(
      BlockState state,
      World world,
      BlockPos pos,
      Block sourceBlock,
      @Nullable WireOrientation wireOrientation,
      boolean notify) {
    updateTransmitPowering(world, pos, state);
    super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
  }

  @Override
  public int getWeakRedstonePower(
      BlockState state, BlockView world, BlockPos pos, Direction direction) {
    Direction emitFace = state.get(FACING).getOpposite();

    if (direction == emitFace) {
      return state.get(RECEIVE_POWERED);
    }
    return 0;
  }

  @Override
  public int getStrongRedstonePower(
      BlockState state, BlockView world, BlockPos pos, Direction direction) {
    return getWeakRedstonePower(state, world, pos, direction);
  }

  /** Set receive power level. */
  public static void setReceivePowering(World world, BlockPos pos, BlockState state, int powering) {
    boolean changed =
        updateLevelAndActiveIfNeeded(world, pos, state, RECEIVE_POWERED, RECEIVE_ACTIVE, powering);
    if (changed) {
      world.updateNeighbors(pos, state.getBlock());
    }
  }

  public static void updateTransmitPowering(World world, BlockPos pos, BlockState state) {
    Direction facing = state.get(FACING);
    Direction powerReadDirection = facing.getOpposite();
    int powering =
        world.getEmittedRedstonePower(pos.offset(powerReadDirection), powerReadDirection);
    updateLevelAndActiveIfNeeded(world, pos, state, TRANSMIT_POWERED, TRANSMIT_ACTIVE, powering);
  }

  @Nullable
  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new NetworkCoreEntity(pos, state);
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      World world, BlockState state, BlockEntityType<T> type) {
    return validateTicker(type, ModBlockEntities.NETWORK_CORE, NetworkCoreEntity::tick);
  }

  @Override
  public boolean emitsRedstonePower(BlockState state) {
    return true;
  }
}
