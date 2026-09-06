package com.simplelogistic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record UpdateOperationPayload(
        BlockPos pos,
        Direction side,
        int opIndex,
        int modeOrdinal,
        int targetSideOrdinal,
        int typeOrdinal,
        int redstoneModeOrdinal,
        int priority,
        boolean isWhitelist,
        boolean matchNbt,
        String tagFilter
) implements CustomPacketPayload {

    public static final Type<UpdateOperationPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SimpleLogistic.MODID, "update_operation"));

    public static final StreamCodec<FriendlyByteBuf, UpdateOperationPayload> STREAM_CODEC = CustomPacketPayload.codec(
            UpdateOperationPayload::write,
            UpdateOperationPayload::new
    );

    public UpdateOperationPayload(FriendlyByteBuf buf) {
        this(
                buf.readBlockPos(),
                buf.readEnum(Direction.class),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeEnum(side);
        buf.writeInt(opIndex);
        buf.writeInt(modeOrdinal);
        buf.writeInt(targetSideOrdinal);
        buf.writeInt(typeOrdinal);
        buf.writeInt(redstoneModeOrdinal);
        buf.writeInt(priority);
        buf.writeBoolean(isWhitelist);
        buf.writeBoolean(matchNbt);
        buf.writeUtf(tagFilter != null ? tagFilter : "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateOperationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().level() instanceof ServerLevel level)) return;

            BlockEntity be = level.getBlockEntity(payload.pos());
            if (be instanceof PipeBlockEntity pipe) {
                pipe.ensureDefaultOperation(payload.side());
                List<PipeBlockEntity.PipeOperation> ops = pipe.getOperations(payload.side());

                // Maximal 3 Operationen pro Seite erlaubt
                if (payload.opIndex() >= 0 && payload.opIndex() < 3) {
                    while (ops.size() <= payload.opIndex()) {
                        pipe.addOperation(payload.side());
                    }
                }

                if (payload.opIndex() >= 0 && payload.opIndex() < ops.size()) {
                    // Bounds-Check für Enum-Ordinals
                    if (payload.modeOrdinal() < 0 || payload.modeOrdinal() >= PipeBlockEntity.ConnectionMode.values().length) return;
                    if (payload.typeOrdinal() < 0 || payload.typeOrdinal() >= PipeBlockEntity.TransferType.values().length) return;
                    if (payload.redstoneModeOrdinal() < 0 || payload.redstoneModeOrdinal() >= PipeBlockEntity.RedstoneMode.values().length) return;
                    if (payload.targetSideOrdinal() != -1 && (payload.targetSideOrdinal() < 0 || payload.targetSideOrdinal() >= Direction.values().length)) return;

                    PipeBlockEntity.PipeOperation op = ops.get(payload.opIndex());
                    op.mode = PipeBlockEntity.ConnectionMode.values()[payload.modeOrdinal()];
                    op.simulatedTargetSide = payload.targetSideOrdinal() == -1 ? null : Direction.values()[payload.targetSideOrdinal()];
                    op.type = PipeBlockEntity.TransferType.values()[payload.typeOrdinal()];
                    op.redstoneMode = PipeBlockEntity.RedstoneMode.values()[payload.redstoneModeOrdinal()];
                    op.priority = payload.priority();
                    op.isWhitelist = payload.isWhitelist();
                    op.matchNbt = payload.matchNbt();
                    op.tagFilter = payload.tagFilter();

                    pipe.setChanged();
                    NetworkManager.markDirty();
                }
            }
        });
    }
}
