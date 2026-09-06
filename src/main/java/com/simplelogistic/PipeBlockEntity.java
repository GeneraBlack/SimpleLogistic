package com.simplelogistic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registries) {
        loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
    }

    public CompoundTag writeToNbt(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (Direction dir : Direction.values()) {
            List<PipeOperation> ops = operationsMap.get(dir);
            if (ops.isEmpty()) continue;

            ValueOutput.ValueOutputList opsList = output.childrenList("Operations_" + dir.name());
            for (PipeOperation op : ops) {
                ValueOutput opOutput = opsList.addChild();
                opOutput.putInt("Mode", op.mode.ordinal());
                opOutput.putInt("TransferType", op.type.ordinal());
                opOutput.putInt("TargetSide", op.simulatedTargetSide == null ? -1 : op.simulatedTargetSide.ordinal());
                opOutput.putBoolean("IsWhitelist", op.isWhitelist);
                opOutput.putBoolean("MatchNbt", op.matchNbt);
                opOutput.putInt("RedstoneMode", op.redstoneMode.ordinal());
                opOutput.putInt("Priority", op.priority);
                opOutput.putString("TagFilter", op.tagFilter != null ? op.tagFilter : "");
                op.filter.serialize(opOutput.child("Filter"));
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (Direction dir : Direction.values()) {
            List<PipeOperation> ops = operationsMap.get(dir);
            ops.clear();
            for (ValueInput opInput : input.childrenListOrEmpty("Operations_" + dir.name())) {
                PipeOperation op = new PipeOperation();
                op.mode = ConnectionMode.values()[opInput.getIntOr("Mode", 0)];
                op.type = TransferType.values()[opInput.getIntOr("TransferType", 0)];
                int targetOrdinal = opInput.getIntOr("TargetSide", -1);
                op.simulatedTargetSide = targetOrdinal == -1 ? null : Direction.values()[targetOrdinal];
                op.isWhitelist = opInput.getBooleanOr("IsWhitelist", true);
                op.matchNbt = opInput.getBooleanOr("MatchNbt", false);
                op.redstoneMode = RedstoneMode.values()[opInput.getIntOr("RedstoneMode", 0)];
                op.priority = opInput.getIntOr("Priority", 0);
                op.tagFilter = opInput.getStringOr("TagFilter", "");
                opInput.child("Filter").ifPresent(filterInput -> op.filter.deserialize(filterInput));
                ops.add(op);
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
