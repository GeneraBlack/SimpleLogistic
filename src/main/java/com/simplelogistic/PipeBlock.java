package com.simplelogistic;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.Map;

public class PipeBlock extends Block implements EntityBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
            Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH,
            Direction.WEST, WEST, Direction.UP, UP, Direction.DOWN, DOWN
    );

    // Basis-Würfel in der Mitte (4x4x4 Pixel)
    private static final VoxelShape CENTER = Block.box(6.0D, 6.0D, 6.0D, 10.0D, 10.0D, 10.0D);

    private final PipeTier tier;

    public PipeBlock(BlockBehaviour.Properties properties, PipeTier tier) {
        super(properties.strength(2.0f).noOcclusion());
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false));
    }

    public PipeTier getTier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), canConnectTo(level, pos, dir));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos currentPos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (level instanceof Level realLevel) {
            return state.setValue(PROPERTY_BY_DIRECTION.get(direction), canConnectTo(realLevel, currentPos, direction));
        }
        return state;
    }

    private boolean canConnectTo(Level level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);

        // 1. Verbindet sich mit anderen Rohren (gleiches Tier oder Universal)
        if (neighborState.getBlock() instanceof PipeBlock otherPipe) {
            if (this.tier == PipeTier.UNIVERSAL || otherPipe.getTier() == PipeTier.UNIVERSAL || this.tier == otherPipe.getTier()) {
                return true;
            }
        }

        // 2. Verbindet sich mit Dimensions-Knoten
        if (neighborState.getBlock() instanceof DimensionalNodeBlock) {
            return true;
        }

        // 3. Verbindet sich mit Maschinen (Inventare, Tanks, Energie), basierend auf dem Tier
        if (tier == PipeTier.ITEM || tier == PipeTier.UNIVERSAL) {
            if (level.getCapability(Capabilities.Item.BLOCK, neighborPos, dir.getOpposite()) != null) return true;
        }
        if (tier == PipeTier.FLUID || tier == PipeTier.UNIVERSAL) {
            if (level.getCapability(Capabilities.Fluid.BLOCK, neighborPos, dir.getOpposite()) != null) return true;
        }
        if (tier == PipeTier.ENERGY || tier == PipeTier.UNIVERSAL) {
            if (level.getCapability(Capabilities.Energy.BLOCK, neighborPos, dir.getOpposite()) != null) return true;
        }

        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Hier würde normalerweise eine vorberechnete Shape aus einem Array zurückgegeben werden, 
        // abhängig von den 6 Boolean Properties. Für den Anfang nutzen wir den Center.
        VoxelShape shape = CENTER;
        if (state.getValue(UP)) shape = Shapes.or(shape, Block.box(6.0D, 10.0D, 6.0D, 10.0D, 16.0D, 10.0D));
        if (state.getValue(DOWN)) shape = Shapes.or(shape, Block.box(6.0D, 0.0D, 6.0D, 10.0D, 6.0D, 10.0D));
        if (state.getValue(NORTH)) shape = Shapes.or(shape, Block.box(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 6.0D));
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, Block.box(6.0D, 6.0D, 10.0D, 10.0D, 10.0D, 16.0D));
        if (state.getValue(EAST)) shape = Shapes.or(shape, Block.box(10.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D));
        if (state.getValue(WEST)) shape = Shapes.or(shape, Block.box(0.0D, 6.0D, 6.0D, 6.0D, 10.0D, 10.0D));
        return shape;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state, tier);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level instanceof ServerLevel serverLevel) {
            NetworkManager.registerPipe(serverLevel, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean isMoving) {
        NetworkManager.unregisterPipe(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof PipeBlockEntity pipe) {
                List<Direction> machineDirs = pipe.getMachineDirections();
                if (machineDirs.isEmpty()) {
                    return InteractionResult.PASS;
                }

                // Automatische Erkennung: Welche Maschine meint der Spieler?
                // Wenn die angeklickte Seite eine Maschine hat → diese.
                // Sonst → erste verfügbare Maschinen-Richtung.
                Direction configDir = pipe.resolveConfigDirection(hitResult.getDirection());

                player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, p) -> new PipeMenu(id, inv, pipe, configDir),
                    pipe.getDisplayName()
                ), buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeEnum(configDir);
                    buf.writeNbt(pipe.writeToNbt(player.registryAccess()));
                });
            }
        }
        return InteractionResult.SUCCESS;
    }
}
