package com.simplelogistic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PipeWrenchItem extends Item {

    public PipeWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        Direction face = context.getClickedFace();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide() && player != null) {
            ServerLevel serverLevel = (ServerLevel) level;

            if (player.isShiftKeyDown()) {
                // 1. Shift + Rechtsklick: Instant-Harvest & Direkt ins Inventar
                ItemStack dropStack = new ItemStack(state.getBlock().asItem());
                level.removeBlock(pos, false); // Trigger PipeBlock.affectNeighborsAfterRemoval → unregistriert Pipe automatisch
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1.0f, 1.0f);

                if (!player.addItem(dropStack)) {
                    Block.popResource(level, pos, dropStack);
                }
                return InteractionResult.SUCCESS;
            } else {
                // 2. Normaler Rechtsklick: Seite durchschalten (NONE -> INPUT -> OUTPUT -> DISABLED)
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof PipeBlockEntity pipe) {
                    PipeBlockEntity.ConnectionMode newMode = pipe.cycleSideMode(face);
                    NetworkManager.rebuildAllNetworks(serverLevel);

                    // Sound & Action-Bar Feedback
                    level.playSound(null, pos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 0.8f, 1.4f);

                    ChatFormatting color = switch (newMode) {
                        case INPUT -> ChatFormatting.BLUE;
                        case OUTPUT -> ChatFormatting.GOLD;
                        case DISABLED -> ChatFormatting.RED;
                        case NONE -> ChatFormatting.GRAY;
                    };

                    player.sendOverlayMessage(
                        Component.literal(face.name() + ": ")
                            .append(Component.literal(newMode.name()).withStyle(color, ChatFormatting.BOLD))
                    );
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.SUCCESS;
    }
}
