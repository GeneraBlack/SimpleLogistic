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

public record ManageOperationPayload(
        BlockPos pos,
        Direction side,
        int action, // 0 = ADD, 1 = REMOVE
        int opIndex
) implements CustomPacketPayload {

    public static final Type<ManageOperationPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SimpleLogistic.MODID, "manage_operation"));

    public static final StreamCodec<FriendlyByteBuf, ManageOperationPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ManageOperationPayload::write,
            ManageOperationPayload::new
    );

    public ManageOperationPayload(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readEnum(Direction.class), buf.readInt(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeEnum(side);
        buf.writeInt(action);
        buf.writeInt(opIndex);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ManageOperationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().level() instanceof ServerLevel level)) return;

            BlockEntity be = level.getBlockEntity(payload.pos());
            if (be instanceof PipeBlockEntity pipe) {
                if (payload.action() == 0) {
                    // Max 3 Operationen pro Seite
                    if (pipe.getOperations(payload.side()).size() < 3) {
                        pipe.addOperation(payload.side());
                    }
                } else if (payload.action() == 1) {
                    pipe.removeOperation(payload.side(), payload.opIndex());
                }
                pipe.setChanged();
                NetworkManager.rebuildAllNetworks(level);
            }
        });
    }
}
