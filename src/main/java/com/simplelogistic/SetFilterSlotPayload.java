package com.simplelogistic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record SetFilterSlotPayload(
        BlockPos pos,
        Direction side,
        int operationIndex,
        int slotIndex,
        ItemStack filterStack
) implements CustomPacketPayload {

    public static final Type<SetFilterSlotPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SimpleLogistic.MODID, "set_filter_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFilterSlotPayload> STREAM_CODEC = CustomPacketPayload.codec(
            SetFilterSlotPayload::write,
            SetFilterSlotPayload::new
    );

    public SetFilterSlotPayload(RegistryFriendlyByteBuf buf) {
        this(
                buf.readBlockPos(),
                buf.readEnum(Direction.class),
                buf.readInt(),
                buf.readInt(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        );
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeEnum(side);
        buf.writeInt(operationIndex);
        buf.writeInt(slotIndex);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, filterStack);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetFilterSlotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().level() instanceof ServerLevel level)) return;

            BlockEntity be = level.getBlockEntity(payload.pos());
            if (be instanceof PipeBlockEntity pipe) {
                List<PipeBlockEntity.PipeOperation> ops = pipe.getOperations(payload.side());
                if (payload.operationIndex() >= 0 && payload.operationIndex() < ops.size()) {
                    PipeBlockEntity.PipeOperation op = ops.get(payload.operationIndex());
                    if (payload.slotIndex() >= 0 && payload.slotIndex() < op.filter.getSlots()) {
                        ItemStack ghostStack = payload.filterStack().copy();
                        if (!ghostStack.isEmpty()) {
                            ghostStack.setCount(1); // Filter immer als 1x Ghost Item
                        }
                        op.filter.setStackInSlot(payload.slotIndex(), ghostStack);
                        pipe.setChanged();
                    }
                }
            }
        });
    }
}
