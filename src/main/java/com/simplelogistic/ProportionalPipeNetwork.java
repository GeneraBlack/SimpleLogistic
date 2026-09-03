package com.simplelogistic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.*;

public class ProportionalPipeNetwork {

    public record PipeNode(ServerLevel level, PipeBlockEntity pipe, Direction sideConnected, BlockPos machinePos) {}
    private record SourceTask(ServerLevel level, BlockPos pipePos, BlockPos machinePos, Direction dir, PipeBlockEntity.PipeOperation op) {}
    private record SinkTask(ServerLevel level, BlockPos pipePos, BlockPos machinePos, Direction dir, PipeBlockEntity.PipeOperation op) {}

    private final List<PipeNode> nodes;

    public ProportionalPipeNetwork(List<PipeNode> nodes) {
        this.nodes = nodes;
    }

    private static IItemHandler getItemHandler(ServerLevel level, BlockPos pos, Direction dir) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
    }

    private static IFluidHandler getFluidHandler(ServerLevel level, BlockPos pos, Direction dir) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, dir);
    }

    private static IEnergyStorage getEnergyStorage(ServerLevel level, BlockPos pos, Direction dir) {
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, dir);
    }

    public void tickNetwork() {
        List<SourceTask> itemSources = new ArrayList<>();
        List<SinkTask> itemSinks = new ArrayList<>();
        List<SourceTask> fluidSources = new ArrayList<>();
        List<SinkTask> fluidSinks = new ArrayList<>();
        List<SourceTask> energySources = new ArrayList<>();
        List<SinkTask> energySinks = new ArrayList<>();

        for (PipeNode node : nodes) {
            ServerLevel level = node.level();
            PipeBlockEntity pipe = node.pipe();
            BlockPos pipePos = pipe.getBlockPos();

            // Sicherheitsprüfung: Ist der Chunk geladen?
            if (!level.isLoaded(pipePos) || !level.isLoaded(node.machinePos())) continue;

            // Operationen für genau die Richtung dieser Maschine abrufen
            List<PipeBlockEntity.PipeOperation> ops = pipe.getOperations(node.sideConnected());

            for (PipeBlockEntity.PipeOperation op : ops) {
                if (!isRedstoneSatisfied(level, pipePos, op)) continue;

                Direction targetDir = op.simulatedTargetSide != null ? op.simulatedTargetSide : node.sideConnected().getOpposite();

                if (op.mode == PipeBlockEntity.ConnectionMode.INPUT) {
                    switch (op.type) {
                        case ITEM -> itemSources.add(new SourceTask(level, pipePos, node.machinePos(), targetDir, op));
                        case FLUID -> fluidSources.add(new SourceTask(level, pipePos, node.machinePos(), targetDir, op));
                        case ENERGY -> energySources.add(new SourceTask(level, pipePos, node.machinePos(), targetDir, op));
                    }
                } else if (op.mode == PipeBlockEntity.ConnectionMode.OUTPUT) {
                    switch (op.type) {
                        case ITEM -> itemSinks.add(new SinkTask(level, pipePos, node.machinePos(), targetDir, op));
                        case FLUID -> fluidSinks.add(new SinkTask(level, pipePos, node.machinePos(), targetDir, op));
                        case ENERGY -> energySinks.add(new SinkTask(level, pipePos, node.machinePos(), targetDir, op));
                    }
                }
            }
        }

        processItems(itemSources, itemSinks);
        processFluids(fluidSources, fluidSinks);
        processEnergy(energySources, energySinks);
    }

    private boolean isRedstoneSatisfied(ServerLevel level, BlockPos pos, PipeBlockEntity.PipeOperation op) {
        boolean hasSignal = level.hasNeighborSignal(pos);
        return switch (op.redstoneMode) {
            case ALWAYS_ACTIVE -> true;
            case HIGH -> hasSignal;
            case LOW -> !hasSignal;
            case PULSE -> hasSignal;
        };
    }

    private boolean matchesItemFilter(ItemStack stack, PipeBlockEntity.PipeOperation op) {
        if (stack.isEmpty()) return false;

        if (op.tagFilter != null && !op.tagFilter.isBlank()) {
            String filter = op.tagFilter.trim();
            if (filter.startsWith("#")) {
                String tagLoc = filter.substring(1);
                boolean matches = stack.getTags().anyMatch(t -> t.location().toString().equalsIgnoreCase(tagLoc));
                return op.isWhitelist ? matches : !matches;
            } else if (filter.startsWith("@")) {
                String modId = filter.substring(1);
                String itemMod = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
                boolean matches = itemMod.equalsIgnoreCase(modId);
                return op.isWhitelist ? matches : !matches;
            }
        }

        boolean hasGhost = false;
        boolean matchesGhost = false;
        for (int i = 0; i < op.filter.getSlots(); i++) {
            ItemStack ghost = op.filter.getStackInSlot(i);
            if (!ghost.isEmpty()) {
                hasGhost = true;
                if (op.matchNbt) {
                    if (ItemStack.isSameItemSameComponents(stack, ghost)) {
                        matchesGhost = true;
                        break;
                    }
                } else {
                    if (ItemStack.isSameItem(stack, ghost)) {
                        matchesGhost = true;
                        break;
                    }
                }
            }
        }

        if (hasGhost) {
            return op.isWhitelist ? matchesGhost : !matchesGhost;
        }

        return true;
    }

    private void processItems(List<SourceTask> sources, List<SinkTask> sinks) {
        if (sources.isEmpty() || sinks.isEmpty()) return;

        Map<Integer, List<SinkTask>> priorityGroups = new TreeMap<>(Collections.reverseOrder());
        for (SinkTask sink : sinks) {
            priorityGroups.computeIfAbsent(sink.op().priority, k -> new ArrayList<>()).add(sink);
        }

        for (SourceTask source : sources) {
            IItemHandler sourceHandler = getItemHandler(source.level(), source.machinePos(), source.dir());
            if (sourceHandler == null) continue;

            for (int slot = 0; slot < sourceHandler.getSlots(); slot++) {
                ItemStack availableStack = sourceHandler.extractItem(slot, 64, true);
                if (availableStack.isEmpty() || !matchesItemFilter(availableStack, source.op())) continue;

                for (List<SinkTask> priorityGroup : priorityGroups.values()) {
                    if (availableStack.isEmpty()) break;

                    // Phase 1: Simuliere was jeder Sink aufnehmen kann
                    Map<SinkTask, Integer> demands = new LinkedHashMap<>();
                    int totalDemand = 0;

                    for (SinkTask sink : priorityGroup) {
                        if (!matchesItemFilter(availableStack, sink.op())) continue;

                        IItemHandler sinkHandler = getItemHandler(sink.level(), sink.machinePos(), sink.dir());
                        if (sinkHandler != null) {
                            ItemStack simResult = ItemHandlerHelper.insertItem(sinkHandler, availableStack.copy(), true);
                            int accepted = availableStack.getCount() - simResult.getCount();
                            if (accepted > 0) {
                                demands.put(sink, accepted);
                                totalDemand += accepted;
                            }
                        }
                    }

                    if (totalDemand <= 0) continue;

                    // Phase 2: Berechne exakt wieviel jeder Sink bekommt (proportionale Verteilung)
                    int toExtract = Math.min(availableStack.getCount(), totalDemand);
                    Map<SinkTask, Integer> allocations = new LinkedHashMap<>();
                    int allocated = 0;

                    for (Map.Entry<SinkTask, Integer> entry : demands.entrySet()) {
                        double share = (double) entry.getValue() / totalDemand;
                        int amount = (int) Math.floor(share * toExtract);
                        if (amount > 0) {
                            allocations.put(entry.getKey(), amount);
                            allocated += amount;
                        }
                    }

                    // Rundungsreste dem ersten Sink zuweisen
                    int remainder = toExtract - allocated;
                    if (remainder > 0 && !allocations.isEmpty()) {
                        Map.Entry<SinkTask, Integer> first = allocations.entrySet().iterator().next();
                        allocations.put(first.getKey(), first.getValue() + remainder);
                    }

                    // Phase 3: Simuliere jeden Sink-Insert nochmal um die echte Menge zu bestimmen
                    int confirmedTotal = 0;
                    Map<SinkTask, Integer> confirmed = new LinkedHashMap<>();
                    for (Map.Entry<SinkTask, Integer> entry : allocations.entrySet()) {
                        IItemHandler sinkHandler = getItemHandler(entry.getKey().level(), entry.getKey().machinePos(), entry.getKey().dir());
                        if (sinkHandler != null) {
                            ItemStack simResult = ItemHandlerHelper.insertItem(sinkHandler, availableStack.copyWithCount(entry.getValue()), true);
                            int realAccept = entry.getValue() - simResult.getCount();
                            if (realAccept > 0) {
                                confirmed.put(entry.getKey(), realAccept);
                                confirmedTotal += realAccept;
                            }
                        }
                    }

                    if (confirmedTotal <= 0) continue;

                    // Phase 4: Extrahiere EXAKT die bestätigte Menge
                    ItemStack extracted = sourceHandler.extractItem(slot, confirmedTotal, false);
                    if (extracted.isEmpty()) continue;

                    // Phase 5: Verteile und tracke tatsächlich eingefügte Menge
                    int totalInserted = 0;
                    for (Map.Entry<SinkTask, Integer> entry : confirmed.entrySet()) {
                        IItemHandler sinkHandler = getItemHandler(entry.getKey().level(), entry.getKey().machinePos(), entry.getKey().dir());
                        if (sinkHandler != null) {
                            ItemStack leftover = ItemHandlerHelper.insertItem(sinkHandler, extracted.copyWithCount(entry.getValue()), false);
                            totalInserted += entry.getValue() - leftover.getCount();
                        }
                    }

                    // Phase 6: Sicherheitsnetz — falls nicht alles verteilt wurde, zurück in die Quelle
                    int notInserted = extracted.getCount() - totalInserted;
                    if (notInserted > 0) {
                        ItemStack returnStack = extracted.copyWithCount(notInserted);
                        ItemStack stillLeft = ItemHandlerHelper.insertItem(sourceHandler, returnStack, false);
                        // Falls auch die Quelle nichts annimmt: als Item droppen
                        if (!stillLeft.isEmpty()) {
                            net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                                source.level(), source.pipePos().getX() + 0.5, source.pipePos().getY() + 0.5, source.pipePos().getZ() + 0.5, stillLeft);
                            source.level().addFreshEntity(drop);
                        }
                    }

                    // Aktualisiere verfügbare Menge für nächste Prioritätsgruppe
                    availableStack = sourceHandler.extractItem(slot, 64, true);
                }
            }
        }
    }

    private void processFluids(List<SourceTask> sources, List<SinkTask> sinks) {
        if (sources.isEmpty() || sinks.isEmpty()) return;

        Map<Integer, List<SinkTask>> priorityGroups = new TreeMap<>(Collections.reverseOrder());
        for (SinkTask sink : sinks) {
            priorityGroups.computeIfAbsent(sink.op().priority, k -> new ArrayList<>()).add(sink);
        }

        for (SourceTask source : sources) {
            IFluidHandler sourceHandler = getFluidHandler(source.level(), source.machinePos(), source.dir());
            if (sourceHandler == null) continue;

            FluidStack available = sourceHandler.drain(1000, IFluidHandler.FluidAction.SIMULATE);
            if (available.isEmpty()) continue;

            int remainingSupply = available.getAmount();

            for (List<SinkTask> priorityGroup : priorityGroups.values()) {
                if (remainingSupply <= 0) break;

                // Phase 1: Simuliere Demand
                Map<SinkTask, Integer> demands = new LinkedHashMap<>();
                int totalDemand = 0;

                for (SinkTask sink : priorityGroup) {
                    IFluidHandler sinkHandler = getFluidHandler(sink.level(), sink.machinePos(), sink.dir());
                    if (sinkHandler != null) {
                        int accepted = sinkHandler.fill(available.copyWithAmount(remainingSupply), IFluidHandler.FluidAction.SIMULATE);
                        if (accepted > 0) {
                            demands.put(sink, accepted);
                            totalDemand += accepted;
                        }
                    }
                }

                if (totalDemand <= 0) continue;

                // Phase 2: Berechne Allokationen
                int toTransfer = Math.min(remainingSupply, totalDemand);
                Map<SinkTask, Integer> confirmed = new LinkedHashMap<>();
                int confirmedTotal = 0;

                for (Map.Entry<SinkTask, Integer> entry : demands.entrySet()) {
                    double share = (double) entry.getValue() / totalDemand;
                    int amount = (int) Math.floor(share * toTransfer);
                    if (amount > 0) {
                        // Simuliere nochmal für die tatsächliche Menge
                        IFluidHandler sinkHandler = getFluidHandler(entry.getKey().level(), entry.getKey().machinePos(), entry.getKey().dir());
                        if (sinkHandler != null) {
                            int realAccept = sinkHandler.fill(available.copyWithAmount(amount), IFluidHandler.FluidAction.SIMULATE);
                            if (realAccept > 0) {
                                confirmed.put(entry.getKey(), realAccept);
                                confirmedTotal += realAccept;
                            }
                        }
                    }
                }

                if (confirmedTotal <= 0) continue;

                // Phase 3: Extrahiere exakt die bestätigte Menge
                FluidStack extracted = sourceHandler.drain(confirmedTotal, IFluidHandler.FluidAction.EXECUTE);
                if (extracted.isEmpty()) continue;

                // Phase 4: Verteile und tracke tatsächlich eingefügte Menge
                int totalInserted = 0;
                for (Map.Entry<SinkTask, Integer> entry : confirmed.entrySet()) {
                    IFluidHandler sinkHandler = getFluidHandler(entry.getKey().level(), entry.getKey().machinePos(), entry.getKey().dir());
                    if (sinkHandler != null) {
                        int inserted = sinkHandler.fill(extracted.copyWithAmount(entry.getValue()), IFluidHandler.FluidAction.EXECUTE);
                        totalInserted += inserted;
                        remainingSupply -= inserted;
                    }
                }

                // Phase 5: Sicherheitsnetz — nicht eingefügte Flüssigkeit zurück in die Quelle
                int notInserted = extracted.getAmount() - totalInserted;
                if (notInserted > 0) {
                    sourceHandler.fill(extracted.copyWithAmount(notInserted), IFluidHandler.FluidAction.EXECUTE);
                }

                available = sourceHandler.drain(1000, IFluidHandler.FluidAction.SIMULATE);
                remainingSupply = available.getAmount();
            }
        }
    }

    private void processEnergy(List<SourceTask> sources, List<SinkTask> sinks) {
        if (sources.isEmpty() || sinks.isEmpty()) return;

        Map<Integer, List<SinkTask>> priorityGroups = new TreeMap<>(Collections.reverseOrder());
        for (SinkTask sink : sinks) {
            priorityGroups.computeIfAbsent(sink.op().priority, k -> new ArrayList<>()).add(sink);
        }

        for (SourceTask source : sources) {
            IEnergyStorage sourceHandler = getEnergyStorage(source.level(), source.machinePos(), source.dir());
            if (sourceHandler == null) continue;

            int remainingSupply = sourceHandler.extractEnergy(Integer.MAX_VALUE, true);
            if (remainingSupply <= 0) continue;

            for (List<SinkTask> priorityGroup : priorityGroups.values()) {
                if (remainingSupply <= 0) break;

                // Phase 1: Simuliere Demand
                Map<SinkTask, Integer> demands = new LinkedHashMap<>();
                int totalDemand = 0;

                for (SinkTask sink : priorityGroup) {
                    IEnergyStorage sinkHandler = getEnergyStorage(sink.level(), sink.machinePos(), sink.dir());
                    if (sinkHandler != null) {
                        int accepted = sinkHandler.receiveEnergy(remainingSupply, true);
                        if (accepted > 0) {
                            demands.put(sink, accepted);
                            totalDemand += accepted;
                        }
                    }
                }

                if (totalDemand <= 0) continue;

                // Phase 2: Berechne Allokationen
                int toTransfer = Math.min(remainingSupply, totalDemand);
                Map<SinkTask, Integer> confirmed = new LinkedHashMap<>();
                int confirmedTotal = 0;

                for (Map.Entry<SinkTask, Integer> entry : demands.entrySet()) {
                    double share = (double) entry.getValue() / totalDemand;
                    int amount = (int) Math.floor(share * toTransfer);
                    if (amount > 0) {
                        IEnergyStorage sinkHandler = getEnergyStorage(entry.getKey().level(), entry.getKey().machinePos(), entry.getKey().dir());
                        if (sinkHandler != null) {
                            int realAccept = sinkHandler.receiveEnergy(amount, true);
                            if (realAccept > 0) {
                                confirmed.put(entry.getKey(), realAccept);
                                confirmedTotal += realAccept;
                            }
                        }
                    }
                }

                if (confirmedTotal <= 0) continue;

                // Phase 3: Extrahiere exakt
                int extracted = sourceHandler.extractEnergy(confirmedTotal, false);
                if (extracted <= 0) continue;

                // Phase 4: Verteile und tracke tatsächlich empfangene Menge
                int totalReceived = 0;
                for (Map.Entry<SinkTask, Integer> entry : confirmed.entrySet()) {
                    IEnergyStorage sinkHandler = getEnergyStorage(entry.getKey().level(), entry.getKey().machinePos(), entry.getKey().dir());
                    if (sinkHandler != null) {
                        int received = sinkHandler.receiveEnergy(entry.getValue(), false);
                        totalReceived += received;
                    }
                }

                // Phase 5: Nicht empfangene Energie zurück in die Quelle
                int notReceived = extracted - totalReceived;
                if (notReceived > 0) {
                    sourceHandler.receiveEnergy(notReceived, false);
                }

                remainingSupply = sourceHandler.extractEnergy(Integer.MAX_VALUE, true);
            }
        }
    }
}

