package com.simplelogistic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DimensionalNodeBlockEntity extends BlockEntity {

    private String channel = "default";

    public DimensionalNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIMENSIONAL_NODE_BE.get(), pos, state);
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel != null ? channel.trim() : "default";
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            NetworkManager.rebuildAllNetworks(serverLevel);
        }
    }

    /**
     * Wird aufgerufen wenn das BlockEntity in die Welt geladen wird.
     * Forciert den Chunk, damit cross-dimensionale Netzwerke funktionieren.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            forceChunk(serverLevel, worldPosition);
        }
    }

    /**
     * Wird von DimensionalNodeBlock.onRemove aufgerufen.
     * Gibt den Chunk wieder frei, sofern kein anderer Node im selben Chunk ist.
     */
    public void releaseChunk() {
        if (level instanceof ServerLevel serverLevel) {
            unforceChunkIfLast(serverLevel, worldPosition);
        }
    }

    private static void forceChunk(ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = ChunkPos.containing(pos);
        level.setChunkForced(chunkPos.x(), chunkPos.z(), true);
    }

    private static void unforceChunkIfLast(ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = ChunkPos.containing(pos);

        // Prüfe ob noch andere Dimensional Nodes in diesem Chunk vorhanden sind
        var chunk = level.getChunk(chunkPos.x(), chunkPos.z());
        boolean hasOtherNodes = false;
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof DimensionalNodeBlockEntity && !be.getBlockPos().equals(pos)) {
                hasOtherNodes = true;
                break;
            }
        }

        if (!hasOtherNodes) {
            level.setChunkForced(chunkPos.x(), chunkPos.z(), false);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Channel", channel);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.channel = input.getStringOr("Channel", "default");
    }
}
