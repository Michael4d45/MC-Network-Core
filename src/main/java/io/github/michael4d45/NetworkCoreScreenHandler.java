package io.github.michael4d45;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class NetworkCoreScreenHandler extends ScreenHandler {
    private final PropertyDelegate propertyDelegate;
    private final NetworkCoreBlockEntity blockEntity;

    public NetworkCoreScreenHandler(int syncId, PlayerInventory playerInventory, NetworkCoreBlockEntity blockEntity) {
        super(NetworkCore.NETWORK_CORE_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;
        this.propertyDelegate = new ArrayPropertyDelegate(2);
        propertyDelegate.set(0, blockEntity.getPort());
        propertyDelegate.set(1, blockEntity.getSymbolPeriod());

        // add player inventory slots
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    public NetworkCoreScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(NetworkCore.NETWORK_CORE_SCREEN_HANDLER, syncId);
        this.blockEntity = null;
        this.propertyDelegate = new ArrayPropertyDelegate(2);

        // add player inventory slots
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    public PropertyDelegate getPropertyDelegate() {
        return propertyDelegate;
    }

    public NetworkCoreBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    public void read(PacketByteBuf buf) {
        propertyDelegate.set(0, buf.readInt());
        propertyDelegate.set(1, buf.readInt());
    }
}
