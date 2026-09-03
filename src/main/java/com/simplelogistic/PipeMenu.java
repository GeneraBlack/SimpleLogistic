package com.simplelogistic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PipeMenu extends AbstractContainerMenu {

    private final PipeBlockEntity pipe;
    private Direction side;

    // GUI-Dimensionen (breiter für 4-Spalten-Layout)
    public static final int GUI_WIDTH = 340;
    public static final int GUI_HEIGHT = 236;
    private static final int INV_X = 89; // zentriert in 340px: (340-162)/2

    public PipeMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
        this(id, playerInv, extraData.readBlockPos(), extraData.readEnum(Direction.class), extraData.readNbt());
    }

    private PipeMenu(int id, Inventory playerInv, BlockPos pos, Direction side, CompoundTag tag) {
        this(id, playerInv, getAndSyncPipe(playerInv, pos, tag), side);
    }

    public PipeMenu(int id, Inventory playerInv, PipeBlockEntity pipe, Direction side) {
        super(ModMenus.PIPE_MENU.get(), id);
        this.pipe = pipe;
        this.side = side;

        // Spieler-Hauptinventar (3 Reihen x 9 Slots)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, 148 + row * 18));
            }
        }

        // Spieler-Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, INV_X + col * 18, 206));
        }
    }

    private static PipeBlockEntity getAndSyncPipe(Inventory playerInv, BlockPos pos, CompoundTag tag) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof PipeBlockEntity pipe) {
            if (tag != null && playerInv.player.level().isClientSide) {
                pipe.readFromNbt(tag, playerInv.player.registryAccess());
            }
            return pipe;
        }
        throw new IllegalStateException("Incorrect block entity at " + pos);
    }

    public BlockPos getPipePos() {
        return pipe.getBlockPos();
    }

    public Direction getSide() {
        return side;
    }

    public void setSide(Direction side) {
        this.side = side;
    }

    public PipeTier getTier() {
        return pipe.getTier();
    }

    public PipeBlockEntity getPipe() {
        return pipe;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 27) {
                if (!this.moveItemStackTo(itemstack1, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 36) {
                if (!this.moveItemStackTo(itemstack1, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(pipe.getLevel(), pipe.getBlockPos()), player, ModBlocks.UNIVERSAL_PIPE.get())
            || stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(pipe.getLevel(), pipe.getBlockPos()), player, ModBlocks.ITEM_PIPE.get())
            || stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(pipe.getLevel(), pipe.getBlockPos()), player, ModBlocks.FLUID_PIPE.get())
            || stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(pipe.getLevel(), pipe.getBlockPos()), player, ModBlocks.ENERGY_PIPE.get());
    }
}
