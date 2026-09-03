package com.simplelogistic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import java.util.List;

public record ChangePipeModePayload(BlockPos pos, Direction side, int operationIndex, int newModeOrdinal, int targetSideOrdinal, int transferTypeOrdinal) implements CustomPacketPayload {
    
    public static final Type<ChangePipeModePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleLogistic.MODID, "change_pipe_mode"));

    public static final StreamCodec<FriendlyByteBuf, ChangePipeModePayload> STREAM_CODEC = CustomPacketPayload.codec(
        ChangePipeModePayload::write,
        ChangePipeModePayload::new
    );

    public ChangePipeModePayload(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readEnum(Direction.class), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeEnum(side);
        buf.writeInt(operationIndex);
        buf.writeInt(newModeOrdinal);
        buf.writeInt(targetSideOrdinal);
        buf.writeInt(transferTypeOrdinal);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChangePipeModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().level() instanceof ServerLevel level)) return;
            
            BlockEntity be = level.getBlockEntity(payload.pos());
            if (be instanceof PipeBlockEntity pipe) {
                List<PipeBlockEntity.PipeOperation> ops = pipe.getOperations(payload.side());
                
                if (payload.operationIndex() >= 0 && payload.operationIndex() < ops.size()) {
                    // Bounds-Check für Enum-Ordinals
                    if (payload.newModeOrdinal() < 0 || payload.newModeOrdinal() >= PipeBlockEntity.ConnectionMode.values().length) return;
                    if (payload.transferTypeOrdinal() < 0 || payload.transferTypeOrdinal() >= PipeBlockEntity.TransferType.values().length) return;
                    if (payload.targetSideOrdinal() != -1 && (payload.targetSideOrdinal() < 0 || payload.targetSideOrdinal() >= Direction.values().length)) return;

                    PipeBlockEntity.PipeOperation op = ops.get(payload.operationIndex());
                    
                    op.mode = PipeBlockEntity.ConnectionMode.values()[payload.newModeOrdinal()];
                    op.simulatedTargetSide = payload.targetSideOrdinal() == -1 ? null : Direction.values()[payload.targetSideOrdinal()];
                    op.type = PipeBlockEntity.TransferType.values()[payload.transferTypeOrdinal()];
                    
                    pipe.setChanged();
                    NetworkManager.rebuildAllNetworks(level);
                }
            }
        });
    }
}
