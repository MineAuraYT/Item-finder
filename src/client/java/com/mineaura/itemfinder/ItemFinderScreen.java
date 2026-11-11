package com.mineaura.itemfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class ItemFinderScreen extends Screen {
    private EditBox searchBox;
    private String status = "Search for an item in nearby containers.";
    private List<ScanResult> results = List.of();
    private int radius = ContainerScanner.DEFAULT_RADIUS;
    private boolean scanning;

    public ItemFinderScreen() {
        super(Component.literal("Item Finder"));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(430, this.width - 24);
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(12, (this.height - 300) / 2);
        int contentWidth = panelWidth - 24;

        searchBox = new EditBox(this.font, left + 12, top + 48, contentWidth - 92, 22,
                Component.literal("Search item"));
        searchBox.setHint(Component.literal("e.g. netherite, diamond, apple"));
        this.addRenderableWidget(searchBox);

        this.addRenderableWidget(Button.builder(Component.literal("Scan"), button -> startScan())
                .bounds(left + panelWidth - 74, top + 48, 62, 22).build());
        this.addRenderableWidget(Button.builder(Component.literal("-"), button -> {
            radius = Math.max(10, radius - 10);
            rebuildWidgets();
        }).bounds(left + 12, top + 78, 24, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            radius = Math.min(50, radius + 10);
            rebuildWidgets();
        }).bounds(left + 42, top + 78, 24, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left + panelWidth - 72, top + 264, 60, 20).build());

        int rowY = top + 130;
        int rowHeight = 25;
        int maxRows = Math.max(0, Math.min(5, (top + 252 - rowY) / rowHeight));
        for (int i = 0; i < Math.min(results.size(), maxRows); i++) {
            ScanResult result = results.get(i);
            String label = result.containerName() + " • " + result.count() + "x  @  "
                    + result.position().getX() + ", " + result.position().getY() + ", " + result.position().getZ();
            this.addRenderableWidget(Button.builder(Component.literal(label), button -> highlight(result.position()))
                    .bounds(left + 12, rowY + i * rowHeight, contentWidth, 21).build());
        }
    }

    private void startScan() {
        if (scanning) return;
        String query = searchBox.getValue().trim();
        if (query.isEmpty()) {
            status = "Enter an item name first.";
            return;
        }

        Minecraft client = Minecraft.getInstance();
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null || client.player == null || client.level == null) {
            status = "This version scans single-player worlds only. Multiplayer needs server-side support.";
            results = List.of();
            rebuildWidgets();
            return;
        }

        BlockPos origin = client.player.blockPosition();
        var dimension = client.level.dimension();
        scanning = true;
        status = "Scanning loaded containers...";
        results = List.of();
        rebuildWidgets();

        ContainerScanner.scan(server, dimension, origin, query, radius, matches -> client.execute(() -> {
            scanning = false;
            results = matches;
            if (matches.isEmpty()) {
                status = "Can't detect the item. Go to a different area and try again.";
            } else {
                status = "Item found in " + matches.size() + " container(s). Select a result to highlight it.";
            }
            rebuildWidgets();
        }));
    }

    private void highlight(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        ContainerHighlightRenderer.show(pos, client.level.dimension());
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.9;
        double z = pos.getZ() + 0.5;
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2.0 * i) / 24.0;
            double px = x + Math.cos(angle) * 0.65;
            double pz = z + Math.sin(angle) * 0.65;
            client.level.addParticle(ParticleTypes.END_ROD, px, y + (i % 3) * 0.18, pz, 0.0, 0.025, 0.0);
        }
        status = "Glowing highlight active for 6 seconds at X: " + pos.getX()
                + "  Y: " + pos.getY() + "  Z: " + pos.getZ();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int panelWidth = Math.min(430, this.width - 24);
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(12, (this.height - 300) / 2);

        // Draw the panel before widgets so the panel never paints over buttons.
        graphics.fill(left, top, left + panelWidth, top + 292, 0xF0181924);
        graphics.fill(left, top, left + panelWidth, top + 32, 0xFF50308B);
        graphics.fill(left + 8, top + 38, left + panelWidth - 8, top + 73, 0xFF292B3A);
        graphics.text(this.font, "ITEM FINDER", left + 12, top + 11, 0xFFFFFFFF, true);
        graphics.text(this.font, "Search loaded chests, barrels & placed shulkers", left + 12, top + 35, 0xFFCECEDE, false);
        graphics.text(this.font, "Scan radius: " + radius + " blocks", left + 76, top + 84, 0xFFE7D8FF, true);
        graphics.text(this.font, status, left + 12, top + 108, 0xFFFFFFFF, false);
        graphics.text(this.font, "Results: " + results.size(), left + 12, top + 120, 0xFFBBA5F2, true);
        graphics.text(this.font, "Click a result to highlight its container with particles.", left + 12, top + 252,
                0xFFBFC0CC, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(null);
    }
}
