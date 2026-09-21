package com.gugas749.abysscore.client.ui.screens;

import com.gugas749.abysscore.client.ui.AbyssUI;
import com.gugas749.abysscore.features.regions.ACBlockProtectionListener;
import com.gugas749.abysscore.network.region.OpenRegionScreenPacket;
import com.gugas749.abysscore.network.region.SubmitRegionUpdatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class RegionManagerScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int LIST_W = 150;
    private static final int PAD    = AbyssUI.PAD;
    private static final int ROW_H  = AbyssUI.ROW_H;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<OpenRegionScreenPacket.RegionEntry> regions;
    private int selectedIndex = -1;

    private final List<String> ALL_TAGS = List.of(
        ACBlockProtectionListener.NO_BUILD_TAG,
        ACBlockProtectionListener.NO_INTERACT_TAG,
        ACBlockProtectionListener.NO_FLY_TAG,
        ACBlockProtectionListener.NO_FRIENDLYFIRE_TAG,
        ACBlockProtectionListener.NO_HUNGER_TAG,
        ACBlockProtectionListener.NO_TP_TAG,
        ACBlockProtectionListener.NO_MOBSPAWNING_HOSTILE_TAG,
        ACBlockProtectionListener.NO_MOBSPAWNING_PACIFIC_TAG,
        ACBlockProtectionListener.NO_ENTRY_TAG
    );
    private final Map<String, Boolean> tagState = new LinkedHashMap<>();
    private final List<Integer> renderedTagYs   = new ArrayList<>();

    private int listScroll = 0;
    private EditBox filterTagBox;

    // ── Bounds ────────────────────────────────────────────────────────────────
    private int lx, ly, lw, lh; // list panel
    private int dx, dy, dw, dh; // detail panel

    private final Screen previousScreen;

    // Set before sending RequestRegionScreenPacket so the S2C response can restore it
    public static Screen pendingPreviousScreen = null;

    public RegionManagerScreen(List<OpenRegionScreenPacket.RegionEntry> regions,
            Screen previousScreen) {
        super(Component.translatable("screen.abysscore.region.title"));
        this.regions = new ArrayList<>(regions);
        this.previousScreen = previousScreen;
    }

    public RegionManagerScreen(List<OpenRegionScreenPacket.RegionEntry> regions) {
        this(regions, pendingPreviousScreen);
        pendingPreviousScreen = null;
    }

    @Override
    protected void init() {
        int mx = PAD * 2, my = PAD * 2;
        int mw = width  - PAD * 4;
        int mh = height - PAD * 4;

        lx = mx; ly = my; lw = LIST_W; lh = mh;
        dx = mx + LIST_W + PAD; dy = my;
        dw = mw - LIST_W - PAD; dh = mh;

        ALL_TAGS.forEach(tag -> tagState.put(tag, false));

        filterTagBox = new EditBox(font, 0, 0, 100, 16, Component.literal("filter tag"));
        filterTagBox.setMaxLength(64);
        filterTagBox.setHint(Component.literal("Scoreboard tag (empty = block all)"));
        filterTagBox.active = false;
        filterTagBox.visible = false;
        addRenderableWidget(filterTagBox);
    }

    // ── Select ────────────────────────────────────────────────────────────────

    private void selectRegion(int index) {
        selectedIndex = index;
        ALL_TAGS.forEach(tag -> tagState.put(tag, false));
        if (index < 0 || index >= regions.size()) {
            if (filterTagBox != null) { filterTagBox.setValue(""); filterTagBox.active = false; }
            return;
        }
        var r = regions.get(index);
        ALL_TAGS.forEach(tag -> tagState.put(tag, r.tags().contains(tag)));
        if (filterTagBox != null) {
            filterTagBox.setValue(r.entryFilterTag() != null ? r.entryFilterTag() : "");
            filterTagBox.active = true;
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onSave() {
        if (selectedIndex < 0 || selectedIndex >= regions.size()) return;
        String name = regions.get(selectedIndex).name();
        Set<String> active = new LinkedHashSet<>();
        ALL_TAGS.forEach(tag -> { if (tagState.getOrDefault(tag, false)) active.add(tag); });
        String filter = filterTagBox != null ? filterTagBox.getValue().trim() : "";
        PacketDistributor.sendToServer(new SubmitRegionUpdatePacket(name, false, active, filter));
        var old = regions.get(selectedIndex);
        regions.set(selectedIndex, new OpenRegionScreenPacket.RegionEntry(
            old.name(), old.dimension(),
            old.minX(), old.minY(), old.minZ(),
            old.maxX(), old.maxY(), old.maxZ(),
            new LinkedHashSet<>(active), filter
        ));
    }

    private void onDelete() {
        if (selectedIndex < 0 || selectedIndex >= regions.size()) return;
        PacketDistributor.sendToServer(new SubmitRegionUpdatePacket(
            regions.get(selectedIndex).name(), true, Set.of(), ""));
        regions.remove(selectedIndex);
        selectRegion(-1);
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // List clicks
        int rowY = ly + AbyssUI.HEADER_H + 2;
        if (AbyssUI.isHovered(mx, my, lx, rowY, lw, lh - AbyssUI.HEADER_H - 2)) {
            int clicked = (int)(my - rowY) / ROW_H + listScroll;
            if (clicked >= 0 && clicked < regions.size()) { selectRegion(clicked); return true; }
        }

        // Checkbox clicks
        if (selectedIndex >= 0) {
            int tx = dx + PAD;
            for (int i = 0; i < ALL_TAGS.size() && i < renderedTagYs.size(); i++) {
                int ry = renderedTagYs.get(i);
                if (AbyssUI.isHovered(mx, my, tx, ry + 3, 10, 10)) {
                    String tag = ALL_TAGS.get(i);
                    tagState.put(tag, !tagState.getOrDefault(tag, false));
                    return true;
                }
            }
        }

        // Save / Delete buttons at bottom of detail panel
        if (selectedIndex >= 0) {
            int by = dy + dh - 22;
            // Save
            if (AbyssUI.isHovered(mx, my, dx + dw - PAD - 80, by, 80, 18)) { onSave(); return true; }
            // Delete
            if (AbyssUI.isHovered(mx, my, dx + PAD, by, 80, 18)) { onDelete(); return true; }
        }
        // Close — returns to previous screen (main menu)
        int by = dy + dh - 22;
        if (AbyssUI.isHovered(mx, my, dx + dw / 2 - 40, by, 80, 18)) {
            net.minecraft.client.Minecraft.getInstance().setScreen(previousScreen); return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx2, double dy2) {
        if (AbyssUI.isHovered(mx, my, lx, ly, lw, lh)) {
            int maxScroll = Math.max(0, regions.size() - (lh - AbyssUI.HEADER_H) / ROW_H);
            listScroll = Math.max(0, Math.min(listScroll - (int) dy2, maxScroll));
            return true;
        }
        return super.mouseScrolled(mx, my, dx2, dy2);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        AbyssUI.drawBackground(g, width, height);
        renderList(g, mx, my);
        renderDetail(g, mx, my);
        super.render(g, mx, my, delta);
    }

    private void renderList(GuiGraphics g, int mx, int my) {
        AbyssUI.drawSidebar(g, lx, ly, lw, lh);
        AbyssUI.drawHeader(g, font, lx, ly, lw, "\u25A6  Regions");

        if (regions.isEmpty()) {
            g.drawCenteredString(font, "\u00a77No regions", lx + lw / 2, ly + lh / 2, AbyssUI.TEXT_MUTED);
            return;
        }

        int rowY = ly + AbyssUI.HEADER_H + 2;
        AbyssUI.scissor(g, lx, rowY, lw, lh - AbyssUI.HEADER_H - 2);

        for (int i = listScroll; i < regions.size(); i++) {
            var r = regions.get(i);
            int ry = rowY + (i - listScroll) * ROW_H;
            if (ry + ROW_H > ly + lh) break;

            boolean sel = (i == selectedIndex);
            boolean hov = AbyssUI.isHovered(mx, my, lx, ry, lw, ROW_H);

            if (sel) {
                g.fill(lx, ry, lx + lw, ry + ROW_H, AbyssUI.ROW_SELECTED);
                AbyssUI.drawBorder(g, lx, ry, lw, ROW_H, AbyssUI.BORDER_ACTIVE);
                g.fill(lx, ry, lx + 2, ry + ROW_H, AbyssUI.BORDER_ACTIVE);
            } else if (hov) {
                g.fill(lx, ry, lx + lw, ry + ROW_H, AbyssUI.ROW_HOVER);
            }

            int tagCount = r.tags().size();
            String badge = tagCount > 0 ? " \u00a70[" + tagCount + "]" : "";
            int nameColor = sel ? AbyssUI.TEXT_ACCENT : AbyssUI.TEXT;
            g.drawString(font, "\u00a7f" + r.name() + badge, lx + PAD, ry + 6, nameColor, false);
        }

        g.disableScissor();
    }

    private void renderDetail(GuiGraphics g, int mx, int my) {
        AbyssUI.drawPanel(g, dx, dy, dw, dh);
        AbyssUI.drawHeader(g, font, dx, dy, dw, "\u25A6  Region Details");

        if (selectedIndex < 0 || selectedIndex >= regions.size()) {
            g.drawCenteredString(font,
                "\u00a77\u2190  Select a region",
                dx + dw / 2, dy + dh / 2, AbyssUI.TEXT_MUTED);

            // Close button
            AbyssUI.drawButton(g, font, dx + dw / 2 - 40, dy + dh - 22, 80, 18,
                "Close", AbyssUI.isHovered(mx, my, dx + dw / 2 - 40, dy + dh - 22, 80, 18), false);
            return;
        }

        var r = regions.get(selectedIndex);
        int tx = dx + PAD;
        int ty = dy + AbyssUI.HEADER_H + PAD;

        // Region name
        g.drawString(font, "\u00a7b" + r.name(), tx, ty, AbyssUI.TEXT_ACCENT, false); ty += 14;
        g.fill(dx + PAD, ty, dx + dw - PAD, ty + 1, AbyssUI.BORDER); ty += 6;

        // Info
        g.drawString(font, "\u00a77Dim  \u00a7f" + shortenDim(r.dimension()), tx, ty, AbyssUI.TEXT, false); ty += 12;
        g.drawString(font, String.format("\u00a77From \u00a7f%d %d %d", r.minX(), r.minY(), r.minZ()), tx, ty, AbyssUI.TEXT, false); ty += 12;
        g.drawString(font, String.format("\u00a77To   \u00a7f%d %d %d", r.maxX(), r.maxY(), r.maxZ()), tx, ty, AbyssUI.TEXT, false); ty += 14;

        g.fill(dx + PAD, ty, dx + dw - PAD, ty + 1, AbyssUI.BORDER); ty += 6;
        g.drawString(font, "\u00a77Restrictions", tx, ty, AbyssUI.TEXT_MUTED, false); ty += 12;

        // Tag checkboxes
        renderedTagYs.clear();
        for (String tag : ALL_TAGS) {
            renderedTagYs.add(ty);
            boolean checked = tagState.getOrDefault(tag, false);

            // Checkbox
            int cbg = checked ? 0xFF0D2A0D : AbyssUI.PANEL_LIGHT;
            int cbr = checked ? AbyssUI.STATE_ON_BDR : AbyssUI.BORDER;
            g.fill(tx, ty + 3, tx + 10, ty + 13, cbg);
            AbyssUI.drawBorder(g, tx, ty + 3, 10, 10, cbr);
            if (checked) {
                // Checkmark — two fills forming a tick shape
                g.fill(tx + 2, ty + 7, tx + 4, ty + 11, 0xFF44CC44);
                g.fill(tx + 3, ty + 5, tx + 9,  ty + 8,  0xFF44CC44);
            }

            boolean hov = AbyssUI.isHovered(mx, my, tx, ty + 3, 10, 10);
            int labelColor = checked ? AbyssUI.TEXT : (hov ? AbyssUI.TEXT : AbyssUI.TEXT_MUTED);
            g.drawString(font, formatTagName(tag), tx + 14, ty + 4, labelColor, false);
            ty += 18;
        }

        // No-entry filter tag field
        boolean noEntry = tagState.getOrDefault(ACBlockProtectionListener.NO_ENTRY_TAG, false);
        if (noEntry && filterTagBox != null) {
            ty += 4;
            g.drawString(font, "\u00a77Entry filter tag:", tx + 14, ty, AbyssUI.TEXT_MUTED, false);
            ty += 12;
            // Draw field background
            g.fill(tx + 14, ty, tx + 14 + dw - PAD * 2 - 14 - 4, ty + 16, AbyssUI.PANEL_LIGHT);
            AbyssUI.drawBorder(g, tx + 14, ty, dw - PAD * 2 - 14 - 4, 16, AbyssUI.BORDER);
            filterTagBox.setX(tx + 16);
            filterTagBox.setY(ty + 1);
            filterTagBox.setWidth(dw - PAD * 2 - 14 - 8);
            filterTagBox.visible = true;
            filterTagBox.active = true;
        } else if (filterTagBox != null) {
            filterTagBox.visible = false;
            filterTagBox.active = false;
        }

        // Bottom buttons
        int by = dy + dh - 22;
        AbyssUI.drawButton(g, font, dx + PAD, by, 80, 18,
            "\u00a7cDelete", AbyssUI.isHovered(mx, my, dx + PAD, by, 80, 18), true);
        AbyssUI.drawButton(g, font, dx + dw / 2 - 40, by, 80, 18,
            "Close", AbyssUI.isHovered(mx, my, dx + dw / 2 - 40, by, 80, 18), false);
        AbyssUI.drawButton(g, font, dx + dw - PAD - 80, by, 80, 18,
            "\u00a7aSave", AbyssUI.isHovered(mx, my, dx + dw - PAD - 80, by, 80, 18), false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String shortenDim(String dim) {
        int colon = dim.indexOf(':');
        return colon >= 0 ? dim.substring(colon + 1) : dim;
    }

    private String formatTagName(String tag) {
        return switch (tag) {
            case "no_build"               -> "No Build";
            case "no_interact"            -> "No Interact";
            case "no_fly"                 -> "No Fly";
            case "no_friendlyfire"        -> "No Friendly Fire";
            case "no_hunger"              -> "No Hunger";
            case "no_tp"                  -> "No Teleport";
            case "no_mobspawning_hostile" -> "No Hostile Spawning";
            case "no_mobspawning_pacific" -> "No Passive Spawning";
            case "no_entry"              -> "No Entry";
            default -> tag;
        };
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float delta) {}
}
