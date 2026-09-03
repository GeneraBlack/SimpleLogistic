package com.simplelogistic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class PipeBlockEntity extends BlockEntity implements MenuProvider {

    public enum ConnectionMode { NONE, INPUT, OUTPUT, DISABLED }
    public enum TransferType { ITEM, FLUID, ENERGY }
    public enum RedstoneMode { ALWAYS_ACTIVE, HIGH, LOW, PULSE }

    public static class PipeOperation {
        public ConnectionMode mode = ConnectionMode.NONE;
        public TransferType type = TransferType.ITEM;
        public Direction simulatedTargetSide = null;
        public boolean isWhitelist = true;
        public boolean matchNbt = false;
        public RedstoneMode redstoneMode = RedstoneMode.ALWAYS_ACTIVE;
        public int priority = 0;
        public String tagFilter = "";
        public final ItemStackHandler filter = new ItemStackHandler(9);
    }

    // Operationen pro Richtung (= pro angeschlossene Maschine)
    private final Map<Direction, List<PipeOperation>> operationsMap = new EnumMap<>(Direction.class);
    private final PipeTier tier;

    public PipeBlockEntity(BlockPos pos, BlockState state, PipeTier tier) {
        super(ModBlockEntities.PIPE_BE.get(), pos, state);
        this.tier = tier;
        for (Direction dir : Direction.values()) {
            operationsMap.put(dir, new ArrayList<>());
        }
    }

    public PipeTier getTier() {
        return tier;
    }

    /**
     * Findet die Richtungen, in denen eine Maschine (nicht Pipe, nicht DimensionalNode) angeschlossen ist.
     */
    public List<Direction> getMachineDirections() {
        List<Direction> result = new ArrayList<>();
        if (level == null) return result;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe != null && !(neighborBe instanceof PipeBlockEntity) && !(neighborBe instanceof DimensionalNodeBlockEntity)) {
                result.add(dir);
            }
        }
        return result;
    }

    /**
     * Ermittelt die beste Richtung für die GUI basierend auf der angeklickten Seite.
     * Wenn die angeklickte Seite eine Maschine hat → diese Richtung.
     * Sonst → erste Richtung mit Maschine.
     */
    public Direction resolveConfigDirection(Direction clickedSide) {
        List<Direction> machineDirs = getMachineDirections();
        if (machineDirs.isEmpty()) return clickedSide;
        if (machineDirs.contains(clickedSide)) return clickedSide;
        return machineDirs.get(0);
    }

    public List<PipeOperation> getOperations(Direction dir) {
        List<PipeOperation> ops = operationsMap.get(dir);
        if (ops == null) {
            ops = new ArrayList<>();
            operationsMap.put(dir, ops);
        }
        return ops;
    }

    /**
     * Für Abwärtskompatibilität mit Code der getOperations() ohne Richtung aufruft.
     * Gibt alle Operationen zusammengefasst zurück.
     */
    public List<PipeOperation> getOperations() {
        List<PipeOperation> all = new ArrayList<>();
        for (List<PipeOperation> ops : operationsMap.values()) {
            all.addAll(ops);
        }
        return all;
    }

    public void ensureDefaultOperation(Direction dir) {
        List<PipeOperation> ops = getOperations(dir);
        if (ops.isEmpty()) {
            PipeOperation op = new PipeOperation();
            if (tier == PipeTier.FLUID) op.type = TransferType.FLUID;
            if (tier == PipeTier.ENERGY) op.type = TransferType.ENERGY;
            ops.add(op);
        }
    }

    public void addOperation(Direction dir) {
        PipeOperation op = new PipeOperation();
        if (tier == PipeTier.FLUID) op.type = TransferType.FLUID;
        if (tier == PipeTier.ENERGY) op.type = TransferType.ENERGY;
        operationsMap.get(dir).add(op);
        setChanged();
    }

    public void removeOperation(Direction dir, int index) {
        List<PipeOperation> ops = operationsMap.get(dir);
        if (index >= 0 && index < ops.size()) {
            ops.remove(index);
            setChanged();
        }
    }

    public ConnectionMode cycleSideMode(Direction dir) {
        List<PipeOperation> ops = operationsMap.get(dir);
        if (ops.isEmpty()) {
            addOperation(dir);
        }
        PipeOperation op = ops.get(0);
        ConnectionMode nextMode = switch (op.mode) {
            case NONE -> ConnectionMode.INPUT;
            case INPUT -> ConnectionMode.OUTPUT;
            case OUTPUT -> ConnectionMode.DISABLED;
            case DISABLED -> ConnectionMode.NONE;
        };
        op.mode = nextMode;
        setChanged();
        return nextMode;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    public CompoundTag writeToNbt(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (Direction dir : Direction.values()) {
            List<PipeOperation> ops = operationsMap.get(dir);
            if (ops.isEmpty()) continue;

            ListTag opsListTag = new ListTag();
            for (PipeOperation op : ops) {
                CompoundTag opTag = new CompoundTag();
                opTag.putInt("Mode", op.mode.ordinal());
                opTag.putInt("TransferType", op.type.ordinal());
                opTag.putInt("TargetSide", op.simulatedTargetSide == null ? -1 : op.simulatedTargetSide.ordinal());
                opTag.putBoolean("IsWhitelist", op.isWhitelist);
                opTag.putBoolean("MatchNbt", op.matchNbt);
                opTag.putInt("RedstoneMode", op.redstoneMode.ordinal());
                opTag.putInt("Priority", op.priority);
                opTag.putString("TagFilter", op.tagFilter != null ? op.tagFilter : "");
                opTag.put("Filter", op.filter.serializeNBT(registries));
                opsListTag.add(opTag);
            }
            tag.put("Operations_" + dir.name(), opsListTag);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (Direction dir : Direction.values()) {
            List<PipeOperation> ops = operationsMap.get(dir);
            ops.clear();
            if (tag.contains("Operations_" + dir.name(), Tag.TAG_LIST)) {
                ListTag opsListTag = tag.getList("Operations_" + dir.name(), Tag.TAG_COMPOUND);
                for (int i = 0; i < opsListTag.size(); i++) {
                    CompoundTag opTag = opsListTag.getCompound(i);
                    PipeOperation op = new PipeOperation();
                    op.mode = ConnectionMode.values()[opTag.getInt("Mode")];
                    op.type = TransferType.values()[opTag.getInt("TransferType")];
                    int targetOrdinal = opTag.getInt("TargetSide");
                    op.simulatedTargetSide = targetOrdinal == -1 ? null : Direction.values()[targetOrdinal];
                    op.isWhitelist = opTag.getBoolean("IsWhitelist");
                    op.matchNbt = opTag.getBoolean("MatchNbt");
                    if (opTag.contains("RedstoneMode")) {
                        op.redstoneMode = RedstoneMode.values()[opTag.getInt("RedstoneMode")];
                    }
                    if (opTag.contains("Priority")) {
                        op.priority = opTag.getInt("Priority");
                    }
                    if (opTag.contains("TagFilter")) {
                        op.tagFilter = opTag.getString("TagFilter");
                    }
                    if (opTag.contains("Filter")) {
                        op.filter.deserializeNBT(registries, opTag.getCompound("Filter"));
                    }
                    ops.add(op);
                }
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simplelogistic." + tier.name().toLowerCase() + "_pipe");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null;
    }
}
