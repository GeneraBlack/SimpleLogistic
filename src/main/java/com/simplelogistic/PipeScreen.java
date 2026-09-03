package com.simplelogistic;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class PipeScreen extends AbstractContainerScreen<PipeMenu> {

    // Farben
    private static final int BG_COLOR = 0xCC101010;
    private static final int PANEL_BG = 0xFF2B2B2B;
    private static final int PANEL_HEADER = 0xFF1A1A3A;
    private static final int PANEL_BORDER = 0xFF555555;
    private static final int SELECTED_BG = 0xFF3A3A6A;
    private static final int HOVER_BG = 0xFF3A3A4A;

    // Spalten-Positionen (relativ zu leftPos)
    private static final int COL1_X = 5;     // Maschinen
    private static final int COL1_W = 78;
    private static final int COL2_X = 86;    // Aktionen
    private static final int COL2_W = 56;
    private static final int COL3_X = 145;   // Konfiguration
    private static final int COL3_W = 112;
    private static final int COL4_X = 260;   // Filter
    private static final int COL4_W = 75;
    private static final int HEADER_H = 16;
    private static final int CONTENT_Y = 32; // Start der Inhalte unter Header

    private int selectedOpIndex = 0;
    private EditBox tagFilterBox;
    private boolean needsRebuild = false;
    private List<Direction> machineDirs = List.of();

    public PipeScreen(PipeMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = PipeMenu.GUI_WIDTH;
        this.imageHeight = PipeMenu.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.machineDirs = this.menu.getPipe().getMachineDirections();
        this.menu.getPipe().ensureDefaultOperation(this.menu.getSide());
        rebuildScreen();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (needsRebuild) {
            needsRebuild = false;
            rebuildScreen();
        }
    }

    private void scheduleRebuild() {
        needsRebuild = true;
    }

    private void switchToDirection(Direction newDir) {
        this.menu.setSide(newDir);
        this.menu.getPipe().ensureDefaultOperation(newDir);
        this.selectedOpIndex = 0;
        scheduleRebuild();
    }

    /**
     * Gibt den angezeigten Namen eines Blocks an einer Position zurück.
     * Zeigt auch die Verbindungsseite relativ zur Maschine an.
     */
    private String getMachineName(Direction dir) {
        PipeBlockEntity pipe = this.menu.getPipe();
        if (pipe.getLevel() == null) return dir.name();
        BlockPos machinePos = pipe.getBlockPos().relative(dir);
        BlockState state = pipe.getLevel().getBlockState(machinePos);
        String name = state.getBlock().getName().getString();
        // Kürzen wenn nötig
        if (name.length() > 9) {
            name = name.substring(0, 8) + "…";
        }
        return name;
    }

    /**
     * Gibt die Verbindungsseite relativ zur Maschine zurück (z.B. "Front", "Links").
     * Die Pipe berührt die Maschine an dir.getOpposite() (Maschinen-Face).
     */
    private String getConnectionSide(Direction dir) {
        PipeBlockEntity pipe = this.menu.getPipe();
        if (pipe.getLevel() == null) return dir.name();
        BlockPos machinePos = pipe.getBlockPos().relative(dir);
        BlockState state = pipe.getLevel().getBlockState(machinePos);
        Direction machineFace = dir.getOpposite(); // Seite der Maschine die berührt wird
        Direction facing = getMachineFacing(state);
        return getRelativeSideName(machineFace, facing);
    }

    /**
     * Ermittelt die Blickrichtung eines Blocks aus seinem BlockState.
     */
    private Direction getMachineFacing(BlockState state) {
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        }
        return null; // Block hat keine Blickrichtung
    }

    /**
     * Konvertiert eine absolute Richtung in einen relativen Seitennamen
     * basierend auf der Blickrichtung der Maschine.
     */
    private String getRelativeSideName(Direction side, Direction machineFacing) {
        if (side == Direction.UP) return "Oben";
        if (side == Direction.DOWN) return "Unten";

        if (machineFacing == null || machineFacing.getAxis() == Direction.Axis.Y) {
            // Block ohne horizontale Blickrichtung → Fallback
            return switch (side) {
                case NORTH -> "Nord";
                case SOUTH -> "Süd";
                case EAST -> "Ost";
                case WEST -> "West";
                default -> side.name();
            };
        }

        if (side == machineFacing) return "Front";
        if (side == machineFacing.getOpposite()) return "Hinten";

        // Links/Rechts aus Maschinen-Perspektive
        Direction right = machineFacing.getClockWise();
        if (side == right) return "Rechts";
        if (side == right.getOpposite()) return "Links";

        return side.name();
    }

    /**
     * Gibt den relativen Seitennamen für die Ziel-Seite (Side Spoofing) zurück.
     * Bezogen auf die aktuell ausgewählte Maschine.
     */
    private String getTargetSideName(Direction targetSide) {
        if (targetSide == null) return "AUTO";
        PipeBlockEntity pipe = this.menu.getPipe();
        if (pipe.getLevel() == null) return targetSide.name();
        BlockPos machinePos = pipe.getBlockPos().relative(this.menu.getSide());
        BlockState state = pipe.getLevel().getBlockState(machinePos);
        Direction facing = getMachineFacing(state);
        return getRelativeSideName(targetSide, facing);
    }

    public void rebuildScreen() {
        this.clearWidgets();

        int x = this.leftPos;
        int y = this.topPos;

        PipeBlockEntity pipe = this.menu.getPipe();

        // ==========================================
        // SPALTE 1: MASCHINEN-LISTE
        // ==========================================
        int machY = y + CONTENT_Y;
        for (int i = 0; i < machineDirs.size(); i++) {
            Direction dir = machineDirs.get(i);
            String name = getMachineName(dir);
            String side = getConnectionSide(dir);
            boolean isSelected = dir == this.menu.getSide();

            ChatFormatting color = isSelected ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
            String prefix = isSelected ? "▶ " : "  ";

            this.addRenderableWidget(Button.builder(
                Component.literal(prefix + name).withStyle(color)
                    .append(Component.literal("\n(" + side + ")").withStyle(ChatFormatting.GRAY)),
                b -> switchToDirection(dir)
            ).bounds(x + COL1_X + 1, machY + i * 24, COL1_W - 2, 22).build());
        }

        // ==========================================
        // SPALTE 2: AKTIONEN-LISTE
        // ==========================================
        final List<PipeBlockEntity.PipeOperation> ops = pipe.getOperations(this.menu.getSide());
        if (selectedOpIndex >= ops.size()) {
            selectedOpIndex = Math.max(0, ops.size() - 1);
        }

        int actY = y + CONTENT_Y;
        for (int i = 0; i < Math.min(ops.size(), 4); i++) {
            int opIdx = i;
            PipeBlockEntity.PipeOperation op = ops.get(i);
            String label = (i + 1) + "." + shortMode(op.mode);
            ChatFormatting color = (i == selectedOpIndex) ? ChatFormatting.YELLOW : ChatFormatting.WHITE;

            this.addRenderableWidget(Button.builder(
                Component.literal(label).withStyle(color),
                b -> { selectedOpIndex = opIdx; scheduleRebuild(); }
            ).bounds(x + COL2_X + 1, actY + i * 22, COL2_W - 2, 20).build());
        }

        // + / - Buttons
        int btnY = actY + Math.min(ops.size(), 4) * 22 + 2;
        this.addRenderableWidget(Button.builder(
            Component.literal("+").withStyle(ChatFormatting.GREEN),
            b -> {
                PacketDistributor.sendToServer(new ManageOperationPayload(this.menu.getPipePos(), this.menu.getSide(), 0, 0));
                pipe.addOperation(this.menu.getSide());
                selectedOpIndex = pipe.getOperations(this.menu.getSide()).size() - 1;
                scheduleRebuild();
            }
        ).bounds(x + COL2_X + 1, btnY, 25, 16).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("-").withStyle(ChatFormatting.RED),
            b -> {
                if (pipe.getOperations(this.menu.getSide()).size() > 1) {
                    PacketDistributor.sendToServer(new ManageOperationPayload(this.menu.getPipePos(), this.menu.getSide(), 1, selectedOpIndex));
                    pipe.removeOperation(this.menu.getSide(), selectedOpIndex);
                    selectedOpIndex = Math.max(0, selectedOpIndex - 1);
                    scheduleRebuild();
                }
            }
        ).bounds(x + COL2_X + 28, btnY, 25, 16).build());

        // ==========================================
        // SPALTE 3: KONFIGURATION
        // ==========================================
        if (!ops.isEmpty() && selectedOpIndex < ops.size()) {
            PipeBlockEntity.PipeOperation currentOp = ops.get(selectedOpIndex);
            int cfgX = x + COL3_X + 1;
            int cfgY = y + CONTENT_Y;

            // Modus
            ChatFormatting modeColor = switch (currentOp.mode) {
                case INPUT -> ChatFormatting.BLUE;
                case OUTPUT -> ChatFormatting.GOLD;
                case DISABLED -> ChatFormatting.RED;
                case NONE -> ChatFormatting.GRAY;
            };
            this.addRenderableWidget(Button.builder(
                Component.literal("Modus: ").append(Component.literal(currentOp.mode.name()).withStyle(modeColor, ChatFormatting.BOLD)),
                b -> {
                    currentOp.mode = switch (currentOp.mode) {
                        case NONE -> PipeBlockEntity.ConnectionMode.INPUT;
                        case INPUT -> PipeBlockEntity.ConnectionMode.OUTPUT;
                        case OUTPUT -> PipeBlockEntity.ConnectionMode.DISABLED;
                        case DISABLED -> PipeBlockEntity.ConnectionMode.NONE;
                    };
                    sendUpdate(currentOp);
                    scheduleRebuild();
                }
            ).bounds(cfgX, cfgY, COL3_W - 2, 18).build());

            // Ziel-Seite (mit relativen Namen)
            String targetName = getTargetSideName(currentOp.simulatedTargetSide);
            this.addRenderableWidget(Button.builder(
                Component.literal("Ziel: " + targetName),
                b -> {
                    if (currentOp.simulatedTargetSide == null) {
                        currentOp.simulatedTargetSide = Direction.UP;
                    } else {
                        int next = currentOp.simulatedTargetSide.ordinal() + 1;
                        currentOp.simulatedTargetSide = next >= Direction.values().length ? null : Direction.values()[next];
                    }
                    sendUpdate(currentOp);
                    scheduleRebuild();
                }
            ).bounds(cfgX, cfgY + 20, COL3_W - 2, 18).build());

            // Redstone
            String rsName = switch (currentOp.redstoneMode) {
                case ALWAYS_ACTIVE -> "RS: IMMER";
                case HIGH -> "RS: HIGH";
                case LOW -> "RS: LOW";
                case PULSE -> "RS: PULS";
            };
            this.addRenderableWidget(Button.builder(Component.literal(rsName), b -> {
                int nextRs = (currentOp.redstoneMode.ordinal() + 1) % PipeBlockEntity.RedstoneMode.values().length;
                currentOp.redstoneMode = PipeBlockEntity.RedstoneMode.values()[nextRs];
                sendUpdate(currentOp);
                scheduleRebuild();
            }).bounds(cfgX, cfgY + 40, COL3_W - 2, 18).build());

            // Transfer-Typ (nur bei Universal)
            if (this.menu.getTier() == PipeTier.UNIVERSAL) {
                this.addRenderableWidget(Button.builder(Component.literal("Typ: " + currentOp.type.name()), b -> {
                    int nextType = (currentOp.type.ordinal() + 1) % PipeBlockEntity.TransferType.values().length;
                    currentOp.type = PipeBlockEntity.TransferType.values()[nextType];
                    sendUpdate(currentOp);
                    scheduleRebuild();
                }).bounds(cfgX, cfgY + 60, COL3_W - 2, 18).build());
            }

            // Priorität
            int prioY = cfgY + (this.menu.getTier() == PipeTier.UNIVERSAL ? 80 : 60);
            this.addRenderableWidget(Button.builder(Component.literal("-10"), b -> {
                currentOp.priority -= 10; sendUpdate(currentOp); scheduleRebuild();
            }).bounds(cfgX, prioY, 22, 16).build());

            this.addRenderableWidget(Button.builder(Component.literal("-1"), b -> {
                currentOp.priority -= 1; sendUpdate(currentOp); scheduleRebuild();
            }).bounds(cfgX + 24, prioY, 20, 16).build());

            this.addRenderableWidget(Button.builder(
                Component.literal("P:" + currentOp.priority).withStyle(ChatFormatting.AQUA),
                b -> { currentOp.priority = 0; sendUpdate(currentOp); scheduleRebuild(); }
            ).bounds(cfgX + 46, prioY, 36, 16).build());

            this.addRenderableWidget(Button.builder(Component.literal("+1"), b -> {
                currentOp.priority += 1; sendUpdate(currentOp); scheduleRebuild();
            }).bounds(cfgX + 84, prioY, 20, 16).build());

            this.addRenderableWidget(Button.builder(Component.literal("+10"), b -> {
                currentOp.priority += 10; sendUpdate(currentOp); scheduleRebuild();
            }).bounds(cfgX + 106, prioY, 24, 16 ).build());

            // ==========================================
            // SPALTE 4: FILTER
            // ==========================================
            int fltX = x + COL4_X + 1;
            int fltY = y + CONTENT_Y;

            // Whitelist / Blacklist
            String wlName = currentOp.isWhitelist ? "Whitelist" : "Blacklist";
            ChatFormatting wlColor = currentOp.isWhitelist ? ChatFormatting.GREEN : ChatFormatting.RED;
            this.addRenderableWidget(Button.builder(Component.literal(wlName).withStyle(wlColor), b -> {
                currentOp.isWhitelist = !currentOp.isWhitelist;
                sendUpdate(currentOp);
                scheduleRebuild();
            }).bounds(fltX, fltY, COL4_W - 2, 16).build());

            // NBT
            this.addRenderableWidget(Button.builder(
                Component.literal(currentOp.matchNbt ? "NBT: AN" : "NBT: AUS"),
                b -> {
                    currentOp.matchNbt = !currentOp.matchNbt;
                    sendUpdate(currentOp);
                    scheduleRebuild();
                }
            ).bounds(fltX, fltY + 18, COL4_W - 2, 16).build());

            // Tag Filter EditBox
            tagFilterBox = new EditBox(this.font, fltX, fltY + 36, COL4_W - 2, 14, Component.literal("Tag"));
            tagFilterBox.setMaxLength(64);
            String tagValue = currentOp.tagFilter != null ? currentOp.tagFilter : "";
            tagFilterBox.setValue(tagValue);
            tagFilterBox.setResponder(text -> {
                currentOp.tagFilter = text;
                sendUpdate(currentOp);
            });
            this.addRenderableWidget(tagFilterBox);

            // Ghost-Slots werden in render() gezeichnet (ab fltY + 54)
        }
    }

    private String shortMode(PipeBlockEntity.ConnectionMode mode) {
        return switch (mode) {
            case NONE -> "OFF";
            case INPUT -> "IN";
            case OUTPUT -> "OUT";
            case DISABLED -> "DIS";
        };
    }

    private void sendUpdate(PipeBlockEntity.PipeOperation op) {
        PacketDistributor.sendToServer(new UpdateOperationPayload(
                this.menu.getPipePos(),
                this.menu.getSide(),
                selectedOpIndex,
                op.mode.ordinal(),
                op.simulatedTargetSide != null ? op.simulatedTargetSide.ordinal() : -1,
                op.type.ordinal(),
                op.redstoneMode.ordinal(),
                op.priority,
                op.isWhitelist,
                op.matchNbt,
                op.tagFilter
        ));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Ghost-Slots Klick-Handling
        PipeBlockEntity pipe = this.menu.getPipe();
        List<PipeBlockEntity.PipeOperation> ops = pipe.getOperations(this.menu.getSide());
        if (!ops.isEmpty() && selectedOpIndex < ops.size()) {
            PipeBlockEntity.PipeOperation op = ops.get(selectedOpIndex);
            int fltX = this.leftPos + COL4_X + 1;
            int ghostY = this.topPos + CONTENT_Y + 54;

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int slotX = fltX + c * 18;
                    int slotY = ghostY + r * 18;
                    if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                        int slotIdx = r * 3 + c;
                        ItemStack carried = this.menu.getCarried();

                        ItemStack ghost = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
                        op.filter.setStackInSlot(slotIdx, ghost);
                        PacketDistributor.sendToServer(new SetFilterSlotPayload(
                                this.menu.getPipePos(), this.menu.getSide(), selectedOpIndex, slotIdx, ghost));
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        // Ghost-Slots rendern
        PipeBlockEntity pipe = this.menu.getPipe();
        List<PipeBlockEntity.PipeOperation> ops = pipe.getOperations(this.menu.getSide());
        if (!ops.isEmpty() && selectedOpIndex < ops.size()) {
            PipeBlockEntity.PipeOperation op = ops.get(selectedOpIndex);
            int fltX = this.leftPos + COL4_X + 1;
            int ghostY = this.topPos + CONTENT_Y + 54;

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int slotX = fltX + c * 18;
                    int slotY = ghostY + r * 18;

                    // Slot-Hintergrund
                    g.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF373737);
                    g.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);

                    int slotIdx = r * 3 + c;
                    ItemStack ghost = op.filter.getStackInSlot(slotIdx);
                    if (!ghost.isEmpty()) {
                        g.renderItem(ghost, slotX, slotY);
                        g.renderItemDecorations(this.font, ghost, slotX, slotY);

                        if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                            g.renderTooltip(this.font, ghost, mouseX, mouseY);
                        }
                    }
                }
            }
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        // Gesamter Hintergrund
        g.fill(x, y, x + w, y + h, BG_COLOR);

        // Äußerer Rahmen
        drawBorder(g, x, y, w, h, PANEL_BORDER);

        // Spalte 1: Maschinen
        drawPanel(g, x + COL1_X, y + 14, COL1_W, h - 14 - 100, "Maschinen");

        // Spalte 2: Aktionen
        drawPanel(g, x + COL2_X, y + 14, COL2_W, h - 14 - 100, "Aktionen");

        // Spalte 3: Konfiguration
        drawPanel(g, x + COL3_X, y + 14, COL3_W, h - 14 - 100, "Konfiguration");

        // Spalte 4: Filter
        drawPanel(g, x + COL4_X, y + 14, COL4_W, h - 14 - 100, "Filter");

        // Inventar-Bereich Hintergrund
        int invBgX = x + 85;
        int invBgY = y + 140;
        g.fill(invBgX - 4, invBgY - 4, invBgX + 166, invBgY + 80, PANEL_BG);
        drawBorder(g, invBgX - 4, invBgY - 4, 170, 84, PANEL_BORDER);
    }

    private void drawPanel(GuiGraphics g, int x, int y, int w, int h, String title) {
        // Panel Hintergrund
        g.fill(x, y, x + w, y + h, PANEL_BG);
        // Header
        g.fill(x, y, x + w, y + HEADER_H, PANEL_HEADER);
        // Rahmen
        drawBorder(g, x, y, w, h, PANEL_BORDER);
        // Titel
        g.drawString(this.font, title, x + 3, y + 4, 0xCCCCCC, false);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);         // Top
        g.fill(x, y + h - 1, x + w, y + h, color);  // Bottom
        g.fill(x, y, x + 1, y + h, color);           // Left
        g.fill(x + w - 1, y, x + w, y + h, color);   // Right
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Haupttitel oben
        g.drawString(this.font, "Simple Logistic — Pipe Konfiguration", 8, 4, 0xFFFFFF, false);
    }

    public int getSelectedOpIndex() {
        return selectedOpIndex;
    }

    public int getGuiLeft() {
        return this.leftPos;
    }

    public int getGuiTop() {
        return this.topPos;
    }
}
