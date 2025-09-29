package io.github.michael4d45;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class NetworkCoreBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    private int port = 8080;
    private int symbolPeriod = 20;

    public NetworkCoreBlockEntity(BlockPos pos, BlockState state) {
        super(NetworkCore.NETWORK_CORE_BLOCK_ENTITY, pos, state);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getSymbolPeriod() {
        return symbolPeriod;
    }

    public void setSymbolPeriod(int symbolPeriod) {
        this.symbolPeriod = symbolPeriod;
    }

    @Override
    public Text getDisplayName() {
        return Text.of("Network Core");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new NetworkCoreScreenHandler(syncId, playerInventory, this);
    }

    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(port);
        buf.writeInt(symbolPeriod);
    }
}