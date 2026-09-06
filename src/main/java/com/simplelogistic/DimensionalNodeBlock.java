package com.simplelogistic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class DimensionalNodeBlock extends Block implements EntityBlock {

    public DimensionalNodeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DimensionalNodeBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level instanceof ServerLevel serverLevel) {
            NetworkManager.rebuildAllNetworks(serverLevel);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean isMoving) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DimensionalNodeBlockEntity node) {
            node.releaseChunk();
        }
        NetworkManager.rebuildAllNetworks(level);
        super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DimensionalNodeBlockEntity node) {
                ItemStack held = player.getMainHandItem();
                if (held.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
                    // Wenn der Spieler ein umbenanntes Item hält (z.B. Papier "Kanal_Nether"), setze diesen Kanal!
                    String newChannel = held.getHoverName().getString();
                    node.setChannel(newChannel);
                    player.sendOverlayMessage(
                        Component.literal("Dimensional Node Channel set to: ")
                            .append(Component.literal(newChannel).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                    );
                } else {
                    // Zeige aktuellen Kanal in der Action Bar
                    player.sendOverlayMessage(
                        Component.literal("Dimensional Node [Channel: ")
                            .append(Component.literal(node.getChannel()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                            .append(Component.literal("] (Rename an item in Anvil to change)"))
                    );
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
