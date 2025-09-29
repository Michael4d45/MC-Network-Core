package io.github.michael4d45;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class NetworkCoreBlock extends Block implements BlockEntityProvider {
    public static final net.minecraft.state.property.Property<Direction> FACING = Properties.FACING;
    public static final BooleanProperty RECEIVE_ACTIVE = BooleanProperty.of("receive_active");
    public static final BooleanProperty TRANSMIT_ACTIVE = BooleanProperty.of("transmit_active");

    public NetworkCoreBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState()
            .with(FACING, Direction.NORTH)
            .with(RECEIVE_ACTIVE, false)
            .with(TRANSMIT_ACTIVE, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, RECEIVE_ACTIVE, TRANSMIT_ACTIVE);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof NetworkCoreBlockEntity) {
                player.openHandledScreen((NetworkCoreBlockEntity) blockEntity);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkCoreBlockEntity(pos, state);
    }
}