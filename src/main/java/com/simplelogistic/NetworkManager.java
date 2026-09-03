package com.simplelogistic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = SimpleLogistic.MODID)
public class NetworkManager {

    // ResourceKey statt ServerLevel als Key → verhindert Memory-Leak bei dynamischen Dimensionen
    private static final Map<ResourceKey<Level>, Set<BlockPos>> TRACKED_PIPES = new ConcurrentHashMap<>();
    private static final List<ProportionalPipeNetwork> ACTIVE_NETWORKS = new ArrayList<>();
    private static boolean needsRebuild = false;
    private static boolean isServerStopping = false;

    public static void registerPipe(ServerLevel level, BlockPos pos) {
        if (isServerStopping) return;
        TRACKED_PIPES.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
        needsRebuild = true;
    }

    public static void unregisterPipe(ServerLevel level, BlockPos pos) {
        if (isServerStopping) return;
        Set<BlockPos> pipes = TRACKED_PIPES.get(level.dimension());
        if (pipes != null) {
            pipes.remove(pos.immutable());
            needsRebuild = true;
        }
    }

    public static void markDirty() {
        if (!isServerStopping) {
            needsRebuild = true;
        }
    }

    public static void rebuildAllNetworks(ServerLevel level) {
        markDirty();
    }

    /**
     * Scannt einen geladenen Chunk und registriert alle PipeBlockEntities.
     */
    private static void scanChunkForPipes(ServerLevel level, LevelChunk chunk) {
        Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();
        for (Map.Entry<BlockPos, BlockEntity> entry : blockEntities.entrySet()) {
            if (entry.getValue() instanceof PipeBlockEntity) {
                TRACKED_PIPES.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(entry.getKey().immutable());
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(net.neoforged.neoforge.event.level.ChunkEvent.Load event) {
        if (isServerStopping) return;
        if (event.getLevel() instanceof ServerLevel serverLevel && event.getChunk() instanceof LevelChunk chunk) {
            scanChunkForPipes(serverLevel, chunk);
            needsRebuild = true;
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(net.neoforged.neoforge.event.level.ChunkEvent.Unload event) {
        if (isServerStopping) return;
        if (event.getLevel() instanceof ServerLevel serverLevel && event.getChunk() instanceof LevelChunk chunk) {
            Set<BlockPos> pipes = TRACKED_PIPES.get(serverLevel.dimension());
            if (pipes != null) {
                Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();
                for (Map.Entry<BlockPos, BlockEntity> entry : blockEntities.entrySet()) {
                    if (entry.getValue() instanceof PipeBlockEntity) {
                        pipes.remove(entry.getKey().immutable());
                    }
                }
            }
            needsRebuild = true;
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        isServerStopping = true;
        needsRebuild = false;
        ACTIVE_NETWORKS.clear();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        isServerStopping = false;
        needsRebuild = false;
        TRACKED_PIPES.clear();
        ACTIVE_NETWORKS.clear();
    }

    /**
     * Prüft ob ein Nachbar-BlockEntity tatsächlich eine relevante Capability hat (Item/Fluid/Energy).
     */
    private static boolean hasMachineCapability(ServerLevel level, BlockPos pos, Direction fromDir) {
        Direction accessDir = fromDir.getOpposite();
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, accessDir) != null
            || level.getCapability(Capabilities.FluidHandler.BLOCK, pos, accessDir) != null
            || level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, accessDir) != null;
    }

    /**
     * Markiert das Netzwerk für einen Rebuild im nächsten Tick.
     * Der eigentliche Rebuild erfolgt in rebuildAllNetworksInternal() mit Server-Zugriff.
     */
    public static void rebuildAllNetworks() {
        markDirty();
    }

    /**
     * Baut alle Netzwerke neu auf. Wird vom Level-Tick mit Server-Zugriff aufgerufen.
     */
    private static void rebuildAllNetworksInternal(net.minecraft.server.MinecraftServer server) {
        if (isServerStopping) return;

        Map<String, List<ProportionalPipeNetwork.PipeNode>> channelNodes = new HashMap<>();
        List<ProportionalPipeNetwork> standaloneNetworks = new ArrayList<>();

        for (Map.Entry<ResourceKey<Level>, Set<BlockPos>> entry : TRACKED_PIPES.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) continue;

            Set<BlockPos> allPipes = entry.getValue();
            if (allPipes.isEmpty()) continue;

            Set<BlockPos> visited = new HashSet<>();

            for (BlockPos pipePos : new HashSet<>(allPipes)) {
                if (visited.contains(pipePos)) continue;
                if (!level.isLoaded(pipePos)) continue;

                List<ProportionalPipeNetwork.PipeNode> localNodes = new ArrayList<>();
                Set<String> connectedChannels = new HashSet<>();
                Queue<BlockPos> queue = new ArrayDeque<>();
                queue.add(pipePos);
                visited.add(pipePos);

                while (!queue.isEmpty()) {
                    BlockPos currentPos = queue.poll();
                    if (!level.isLoaded(currentPos)) continue;

                    BlockEntity be = level.getBlockEntity(currentPos);
                    if (!(be instanceof PipeBlockEntity currentPipe)) continue;

                    for (Direction dir : Direction.values()) {
                        BlockPos neighborPos = currentPos.relative(dir);
                        if (!level.isLoaded(neighborPos)) continue;

                        BlockEntity neighborBe = level.getBlockEntity(neighborPos);

                        if (neighborBe instanceof PipeBlockEntity) {
                            if (!visited.contains(neighborPos)) {
                                visited.add(neighborPos);
                                queue.add(neighborPos);
                                allPipes.add(neighborPos.immutable());
                            }
                        } else if (neighborBe instanceof DimensionalNodeBlockEntity dimensionalNode) {
                            connectedChannels.add(dimensionalNode.getChannel());
                        } else if (neighborBe != null && hasMachineCapability(level, neighborPos, dir)) {
                            // Nur echte Maschinen mit Capabilities als Knoten registrieren
                            localNodes.add(new ProportionalPipeNetwork.PipeNode(level, currentPipe, dir, neighborPos));
                        }
                    }
                }

                if (!localNodes.isEmpty()) {
                    if (!connectedChannels.isEmpty()) {
                        for (String channel : connectedChannels) {
                            channelNodes.computeIfAbsent(channel, k -> new ArrayList<>()).addAll(localNodes);
                        }
                    } else {
                        standaloneNetworks.add(new ProportionalPipeNetwork(localNodes));
                    }
                }
            }
        }

        ACTIVE_NETWORKS.clear();
        ACTIVE_NETWORKS.addAll(standaloneNetworks);
        for (List<ProportionalPipeNetwork.PipeNode> dimNodes : channelNodes.values()) {
            if (!dimNodes.isEmpty()) {
                ACTIVE_NETWORKS.add(new ProportionalPipeNetwork(dimNodes));
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (isServerStopping) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        // Führe den globalen Takt nur auf der Overworld aus
        if (serverLevel != serverLevel.getServer().overworld()) return;

        if (needsRebuild) {
            needsRebuild = false;
            rebuildAllNetworksInternal(serverLevel.getServer());
        }

        if (serverLevel.getGameTime() % 10 != 0) return;

        for (ProportionalPipeNetwork network : ACTIVE_NETWORKS) {
            network.tickNetwork();
        }
    }
}
