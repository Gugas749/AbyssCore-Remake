package com.gugas749.abysscore.client.ui;

import com.gugas749.abysscore.client.ui.screens.*;
import com.gugas749.abysscore.network.menu.packets.MenuActionPacket;
import com.gugas749.abysscore.network.menu.packets.OpenMainMenuPacket;
import com.gugas749.abysscore.network.menu.packets.SaveTitlePacket;
import com.gugas749.abysscore.network.menu.packets.RequestRegionScreenPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class AbyssCoreMenuScreen extends Screen {

    // ── Categories ────────────────────────────────────────────────────────────
    private static final String[] CATS = {
        "Players", "Titles", "Vanish",
        "Regions", "Dimensions", "Help Requests", "Bulk Commands"
    };

    // Icons (Minecraft font symbols used as visual anchors)
    private static final String[] CAT_ICONS = {
        "\u2605", // ★ Players
        "\u2736", // ✶ Titles
        "\u25CE", // ◎ Vanish
        "\u25A6", // ▦ Regions
        "\u2318", // ⌘ Dimensions
        "\u2709", // ✉ Help
        "\u2630"  // ☰ Bulk
    };

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int PAD      = AbyssUI.PAD;
    private static final int SIDE_W   = AbyssUI.SIDEBAR_W;
    private static final int ROW_H    = AbyssUI.ROW_H;
    private static final int BTN_SM   = AbyssUI.BTN_W_SM;

    // Panel computed at init
    private int px, py, pw, ph;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<OpenMainMenuPacket.PlayerState> players;
    private final List<OpenMainMenuPacket.TitleEntry>  titles;
    private final List<OpenMainMenuPacket.DimEntry>    dims;
    private final List<OpenMainMenuPacket.HelpEntry>   helpRequests;
    private final List<OpenMainMenuPacket.BulkEntry>   bulkCommands;
    private final Map<Integer, String>                 bindSlots;
    private boolean teamVisibility;

    // ── Navigation ────────────────────────────────────────────────────────────
    private int selectedCat = 0;

    // ── Scroll ────────────────────────────────────────────────────────────────
    private int playerScroll = 0, titleScroll = 0, dimScroll = 0;
    private int helpScroll   = 0, bulkScroll  = 0;

    // ── Title editor ──────────────────────────────────────────────────────────
    private int editingTitle = -1; // -1=none, -2=new
    private int runningTitle = -1;
    private EditBox titleNameBox, titleTextBox, titleSubBox;
    private EditBox titleFadeInBox, titleStayBox, titleFadeOutBox;
    private EditBox runTagBox, runTeamBox;

    public AbyssCoreMenuScreen(OpenMainMenuPacket pkt) {
        super(Component.literal("Abyss"));
        this.players       = new ArrayList<>(pkt.players());
        this.titles        = new ArrayList<>(pkt.titles());
        this.dims          = new ArrayList<>(pkt.dims());
        this.helpRequests  = new ArrayList<>(pkt.helpRequests());
        this.bulkCommands  = new ArrayList<>(pkt.bulkCommands());
        this.bindSlots     = new LinkedHashMap<>(pkt.bindSlots());
        this.teamVisibility = pkt.teamVisibility();
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        // Full screen minus small margin
        int mx = PAD * 2, my = PAD * 2;
        int mw = width  - PAD * 4;
        int mh = height - PAD * 4;

        // Right panel bounds
        px = mx + SIDE_W + PAD;
        py = my;
        pw = mw - SIDE_W - PAD;
        ph = mh;

        clearWidgets();

        // Title-editor widgets (only when editing)
        if (selectedCat == 1 && editingTitle != -1) {
            initTitleEditor();
        }
        if (selectedCat == 1 && runningTitle != -1) {
            initRunDialog();
        }
        if (selectedCat == 1 && editingTitle == -1 && runningTitle == -1) {
            // New title button positioned top-right of panel
            addAbyssButton(px + pw - PAD - AbyssUI.BTN_W_LG, py + PAD + AbyssUI.HEADER_H + PAD,
                AbyssUI.BTN_W_LG, AbyssUI.BTN_H, "+ New Title", false, () -> {
                    editingTitle = -2;
                    init();
                });
        }
    }

    private void addAbyssButton(int x, int y, int w, int h, String label, boolean danger, Runnable action) {
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
            Component.literal(label), b -> action.run()
        ).bounds(x, y, w, h).build());
    }

    private void initTitleEditor() {
        int x  = px + PAD;
        int y  = py + AbyssUI.HEADER_H + PAD * 2;
        int fw = pw - PAD * 2;
        int third = (fw - 8) / 3;

        titleNameBox = abyssEditBox(x, y, fw, "Name"); y += 22;
        titleTextBox = abyssEditBox(x, y, fw, "Title text  (&a green  &c red  &e yellow...)"); y += 22;
        titleSubBox  = abyssEditBox(x, y, fw, "Subtitle (optional)"); y += 22;

        titleFadeInBox  = abyssEditBox(x,              y, third, "Fade in (ticks)");
        titleStayBox    = abyssEditBox(x + third + 4,  y, third, "Stay (ticks)");
        titleFadeOutBox = abyssEditBox(x + third*2+8,  y, third, "Fade out (ticks)");
        y += 22;

        if (editingTitle >= 0 && editingTitle < titles.size()) {
            var t = titles.get(editingTitle);
            titleNameBox.setValue(t.name());
            titleTextBox.setValue(t.titleText());
            titleSubBox.setValue(t.subtitleText());
            titleFadeInBox.setValue(String.valueOf(t.fadeIn()));
            titleStayBox.setValue(String.valueOf(t.stay()));
            titleFadeOutBox.setValue(String.valueOf(t.fadeOut()));
        } else {
            titleFadeInBox.setValue("10");
            titleStayBox.setValue("70");
            titleFadeOutBox.setValue("20");
        }

        final int fy = y + PAD;
        addAbyssButton(x,          fy, 70, AbyssUI.BTN_H, "Save",   false, this::onSaveTitle);
        addAbyssButton(x + 74,     fy, 70, AbyssUI.BTN_H, "Cancel", false, () -> { editingTitle = -1; init(); });
    }

    private void initRunDialog() {
        int x  = px + PAD;
        int y  = py + AbyssUI.HEADER_H + PAD * 3 + 20;
        int fw = pw - PAD * 2;

        addAbyssButton(x, y, fw, AbyssUI.BTN_H, "Send to ALL players", false, () -> {
            sendTitle(MenuActionPacket.Action.SEND_TITLE_ALL, titles.get(runningTitle).id(), "");
            runningTitle = -1; init();
        }); y += 22;

        runTagBox = abyssEditBox(x, y, fw - BTN_SM - 4, "Scoreboard tag...");
        addAbyssButton(x + fw - BTN_SM, y, BTN_SM, AbyssUI.BTN_H, "By Tag", false, () -> {
            sendTitle(MenuActionPacket.Action.SEND_TITLE_TAG, titles.get(runningTitle).id(), runTagBox.getValue().trim());
            runningTitle = -1; init();
        }); y += 22;

        runTeamBox = abyssEditBox(x, y, fw - BTN_SM - 4, "Team name...");
        addAbyssButton(x + fw - BTN_SM, y, BTN_SM, AbyssUI.BTN_H, "By Team", false, () -> {
            sendTitle(MenuActionPacket.Action.SEND_TITLE_TEAM, titles.get(runningTitle).id(), runTeamBox.getValue().trim());
            runningTitle = -1; init();
        }); y += 22;

        addAbyssButton(x, y, 60, AbyssUI.BTN_H, "Cancel", false, () -> { runningTitle = -1; init(); });
    }

    private EditBox abyssEditBox(int x, int y, int w, String hint) {
        EditBox box = new EditBox(font, x, y, w, 16, Component.literal(hint));
        box.setHint(Component.literal(hint));
        box.setMaxLength(256);
        box.setBordered(false); // we draw our own border
        addRenderableWidget(box);
        return box;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        // Full screen abyss background
        AbyssUI.drawBackground(g, width, height);

        int ox = PAD * 2, oy = PAD * 2;
        int ow = width  - PAD * 4;
        int oh = height - PAD * 4;

        // Outer container border
        AbyssUI.drawBorder(g, ox, oy, ow, oh, AbyssUI.BORDER);

        renderSidebar(g, ox, oy, oh, mx, my);
        renderPanel(g, mx, my);

        super.render(g, mx, my, delta);
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private void renderSidebar(GuiGraphics g, int ox, int oy, int oh, int mx, int my) {
        AbyssUI.drawSidebar(g, ox, oy, SIDE_W, oh);

        // Abyss logo / title area
        int logoH = 32;
        g.fill(ox, oy, ox + SIDE_W, oy + logoH, 0xFF060E1E);
        g.fill(ox, oy + logoH - 1, ox + SIDE_W, oy + logoH, AbyssUI.BORDER_ACTIVE);
        g.drawCenteredString(font, "\u00a7bABYSS", ox + SIDE_W / 2, oy + 8, AbyssUI.TEXT_ACCENT);
        g.drawCenteredString(font, "\u00a77CORE", ox + SIDE_W / 2, oy + 18, AbyssUI.TEXT_MUTED);

        // Category rows
        int catY = oy + logoH + PAD / 2;
        for (int i = 0; i < CATS.length; i++) {
            boolean sel  = (i == selectedCat);
            boolean hov  = AbyssUI.isHovered(mx, my, ox + 1, catY, SIDE_W - 2, ROW_H);
            String label = CAT_ICONS[i] + "  " + CATS[i];
            AbyssUI.drawCategoryRow(g, font, ox + 1, catY, SIDE_W - 2, label, sel, hov);
            catY += ROW_H + 2;
        }

        // Version watermark at bottom
        g.drawCenteredString(font, "\u00a70v0.2.0",
            ox + SIDE_W / 2, oy + oh - 12, AbyssUI.TEXT_DISABLED);
    }

    // ── Panel routing ─────────────────────────────────────────────────────────

    private void renderPanel(GuiGraphics g, int mx, int my) {
        AbyssUI.drawPanel(g, px, py, pw, ph);

        // Header
        String catLabel = CAT_ICONS[selectedCat] + "  " + CATS[selectedCat];
        AbyssUI.drawHeader(g, font, px, py, pw, catLabel);

        int contentY = py + AbyssUI.HEADER_H + PAD;

        switch (selectedCat) {
            case 0 -> renderPlayers(g, contentY, mx, my);
            case 1 -> renderTitles(g, contentY, mx, my);
            case 2 -> renderVanish(g, contentY, mx, my);
            case 3 -> renderRegions(g, contentY);
            case 4 -> renderDims(g, contentY, mx, my);
            case 5 -> renderHelp(g, contentY, mx, my);
            case 6 -> renderBulk(g, contentY, mx, my);
        }
    }

    // ── Players panel ─────────────────────────────────────────────────────────

    private void renderPlayers(GuiGraphics g, int y, int mx, int my) {
        int x = px + PAD;

        // Column headers
        g.drawString(font, "\u00a77Name",   x,       y, AbyssUI.TEXT_MUTED, false);
        g.drawString(font, "\u00a77Staff",  x + 140, y, AbyssUI.TEXT_MUTED, false);
        g.drawString(font, "\u00a77Vanish", x + 186, y, AbyssUI.TEXT_MUTED, false);
        g.drawString(font, "\u00a77God",    x + 234, y, AbyssUI.TEXT_MUTED, false);
        g.drawString(font, "\u00a77Blind",  x + 272, y, AbyssUI.TEXT_MUTED, false);
        y += 12;

        // Divider
        g.fill(px + PAD, y, px + pw - PAD, y + 1, AbyssUI.BORDER);
        y += 4;

        if (players.isEmpty()) {
            drawEmpty(g, "No players online.");
            return;
        }

        AbyssUI.scissor(g, px + 1, y, pw - 2, ph - (y - py) - PAD);
        for (int i = playerScroll; i < players.size(); i++) {
            var p = players.get(i);
            int ry = y + (i - playerScroll) * ROW_H;
            if (ry + ROW_H > py + ph - PAD) break;
            boolean hov = AbyssUI.isHovered(mx, my, px + 1, ry, pw - 2, ROW_H);
            AbyssUI.drawRow(g, px + 1, ry, pw - 2, hov, i % 2 == 0);
            g.drawString(font, p.name(), x, ry + 6, AbyssUI.TEXT, false);
            AbyssUI.drawToggle(g, font, x + 182, ry + 2, 36, 16, p.vanished());
            AbyssUI.drawToggle(g, font, x + 228, ry + 2, 36, 16, p.godMode());
            AbyssUI.drawToggle(g, font, x + 266, ry + 2, 36, 16, p.blinded());
        }
        g.disableScissor();
    }

    // ── Titles panel ──────────────────────────────────────────────────────────

    private void renderTitles(GuiGraphics g, int y, int mx, int my) {
        int x = px + PAD;

        if (editingTitle != -1) {
            String sub = editingTitle == -2 ? "New Title" : "Edit — " + titles.get(editingTitle).name();
            g.drawString(font, "\u00a77" + sub, x, y, AbyssUI.TEXT_MUTED, false);
            return;
        }
        if (runningTitle != -1) {
            g.drawString(font, "\u00a77Send: \u00a7b" + titles.get(runningTitle).name(), x, y, AbyssUI.TEXT, false);
            return;
        }

        y += ROW_H + PAD; // space for "New Title" button

        if (titles.isEmpty()) { drawEmpty(g, "No titles saved. Click + New Title."); return; }

        AbyssUI.scissor(g, px + 1, y, pw - 2, ph - (y - py) - PAD);
        for (int i = titleScroll; i < titles.size(); i++) {
            var t = titles.get(i);
            int ry = y + (i - titleScroll) * ROW_H;
            if (ry + ROW_H > py + ph - PAD) break;
            boolean hov = AbyssUI.isHovered(mx, my, px + 1, ry, pw - 2, ROW_H);
            AbyssUI.drawRow(g, px + 1, ry, pw - 2, hov, i % 2 == 0);
            g.drawString(font, t.name(), x, ry + 6, AbyssUI.TEXT, false);
            g.drawString(font, "\u00a70" + shorten(t.titleText(), 30), x + 110, ry + 6, AbyssUI.TEXT_MUTED, false);
            int bx = px + pw - PAD - BTN_SM * 2 - 4;
            AbyssUI.drawButton(g, font, bx,          ry + 2, BTN_SM, 16, "\u00a7aRun",  hov, false);
            AbyssUI.drawButton(g, font, bx+BTN_SM+4, ry + 2, BTN_SM, 16, "\u00a79Edit", hov, false);
        }
        g.disableScissor();
    }

    // ── Vanish panel ──────────────────────────────────────────────────────────

    private void renderVanish(GuiGraphics g, int y, int mx, int my) {
        int x = px + PAD;

        // Team visibility toggle
        boolean tvHov = AbyssUI.isHovered(mx, my, x, y, pw - PAD * 2, ROW_H);
        g.drawString(font, "\u00a77Team Visibility", x, y + 6, AbyssUI.TEXT_MUTED, false);
        AbyssUI.drawToggle(g, font, px + pw - PAD - 50, y + 2, 50, 16, teamVisibility);
        y += ROW_H + PAD;

        g.fill(px + PAD, y, px + pw - PAD, y + 1, AbyssUI.BORDER);
        y += PAD;

        g.drawString(font, "\u00a77Vanished Players", x, y, AbyssUI.TEXT_MUTED, false);
        y += 14;

        var vanished = players.stream().filter(OpenMainMenuPacket.PlayerState::vanished).toList();
        if (vanished.isEmpty()) { drawEmpty(g, "No players are vanished."); return; }

        AbyssUI.scissor(g, px + 1, y, pw - 2, ph - (y - py) - PAD);
        for (int i = 0; i < vanished.size(); i++) {
            var p = vanished.get(i);
            int ry = y + i * ROW_H;
            if (ry + ROW_H > py + ph - PAD) break;
            boolean hov = AbyssUI.isHovered(mx, my, px + 1, ry, pw - 2, ROW_H);
            AbyssUI.drawRow(g, px + 1, ry, pw - 2, hov, i % 2 == 0);
            g.drawString(font, p.name(), x, ry + 6, AbyssUI.TEXT, false);
            int bx = px + pw - PAD - 46 * 3 - 8;
            AbyssUI.drawButton(g, font, bx,      ry+2, 44, 16, "\u00a7aShow",  hov, false);
            AbyssUI.drawButton(g, font, bx+48,   ry+2, 44, 16, "\u00a7cHide",  hov, true);
            AbyssUI.drawButton(g, font, bx+96,   ry+2, 44, 16, "\u00a77Clear", hov, false);
        }
        g.disableScissor();
    }

    // ── Regions panel ─────────────────────────────────────────────────────────

    private void renderRegions(GuiGraphics g, int y) {
        int x  = px + PAD;
        int bw = 160, bh = 20;
        int bx = px + pw / 2 - bw / 2;
        int by = py + ph / 2 - bh / 2;

        g.drawString(font, "\u00a77Opens the dedicated Region Manager screen.",
            x, y, AbyssUI.TEXT_MUTED, false);

        AbyssUI.drawActivePanel(g, bx, by, bw, bh);
        g.drawCenteredString(font, "\u00a7b\u25A6  Open Region Manager", bx + bw / 2, by + 6, AbyssUI.TEXT_ACCENT);
    }

    // ── Dimensions panel ──────────────────────────────────────────────────────

    private void renderDims(GuiGraphics g, int y, int mx, int my) {
        int x = px + PAD;

        // Create dim button
        int cbw = 100, cbh = AbyssUI.BTN_H;
        int cbx = px + pw - PAD - cbw;
        int cby = py + AbyssUI.HEADER_H + PAD;
        AbyssUI.drawButton(g, font, cbx, cby, cbw, cbh, "\u00a7b+ Create Dim", AbyssUI.isHovered(mx, my, cbx, cby, cbw, cbh), false);

        // Column header
        g.drawString(font, "\u00a77Dimension",     x,       y, AbyssUI.TEXT_MUTED, false);
        g.drawString(font, "\u00a77Status",  x + 220, y, AbyssUI.TEXT_MUTED, false);
        y += 12;
        g.fill(px + PAD, y, px + pw - PAD, y + 1, AbyssUI.BORDER);
        y += 4;

        if (dims.isEmpty()) { drawEmpty(g, "No dimensions registered."); return; }

        AbyssUI.scissor(g, px + 1, y, pw - 2, ph - (y - py) - PAD);
        for (int i = dimScroll; i < dims.size(); i++) {
            var d = dims.get(i);
            int ry = y + (i - dimScroll) * ROW_H;
            if (ry + ROW_H > py + ph - PAD) break;
            boolean hov = AbyssUI.isHovered(mx, my, px + 1, ry, pw - 2, ROW_H);
            AbyssUI.drawRow(g, px + 1, ry, pw - 2, hov, i % 2 == 0);
            g.drawString(font, d.name(), x, ry + 6, AbyssUI.TEXT, false);
            String stateColor = switch (d.state()) {
                case "LOADED"   -> "\u00a7a";
                case "UNLOADED" -> "\u00a7c";
                default         -> "\u00a7e";
            };
            g.drawString(font, stateColor + d.state().toLowerCase(), x + 220, ry + 6, AbyssUI.TEXT, false);
            AbyssUI.drawButton(g, font, px + pw - PAD - BTN_SM, ry + 2, BTN_SM, 16, "\u00a7bTP", hov, false);
        }
        g.disableScissor();
    }

    // ── Help panel ────────────────────────────────────────────────────────────

    private void renderHelp(GuiGraphics g, int y, int mx, int my) {
        int x = px + PAD;

        if (helpRequests.isEmpty()) { drawEmpty(g, "No active help requests."); return; }

        AbyssUI.scissor(g, px + 1, y, pw - 2, ph - (y - py) - PAD);
        for (int i = helpScroll; i < helpRequests.size(); i++) {
            var h = helpRequests.get(i);
            int ry = y + (i - helpScroll) * ROW_H;
            if (ry + ROW_H > py + ph - PAD) break;
            boolean hov = AbyssUI.isHovered(mx, my, px + 1, ry, pw - 2, ROW_H);
            AbyssUI.drawRow(g, px + 1, ry, pw - 2, hov, i % 2 == 0);
            g.drawString(font, "\u00a7b" + h.playerName() + "\u00a77: \u00a7f" + shorten(h.reason(), 44),
                x, ry + 6, AbyssUI.TEXT, false);
            AbyssUI.drawButton(g, font, px + pw - PAD - BTN_SM, ry + 2, BTN_SM, 16, "\u00a7a[TP]", hov, false);
        }
        g.disableScissor();
    }

    // ── Bulk panel ────────────────────────────────────────────────────────────

    private void renderBulk(GuiGraphics g, int y, int mx, int my) {
        int x = px + PAD;

        g.drawString(font, "\u00a77Name",        x,       y, AbyssUI.TEXT_MUTED, false);
        g.drawString(font, "\u00a77Bound Slot",  x + 150, y, AbyssUI.TEXT_MUTED, false);
        g.drawString(font, "\u00a77Quick Bind",  x + 220, y, AbyssUI.TEXT_MUTED, false);
        y += 12;
        g.fill(px + PAD, y, px + pw - PAD, y + 1, AbyssUI.BORDER);
        y += 4;

        if (bulkCommands.isEmpty()) { drawEmpty(g, "No bulk commands defined."); return; }

        AbyssUI.scissor(g, px + 1, y, pw - 2, ph - (y - py) - PAD);
        for (int i = bulkScroll; i < bulkCommands.size(); i++) {
            var b = bulkCommands.get(i);
            int ry = y + (i - bulkScroll) * ROW_H;
            if (ry + ROW_H > py + ph - PAD) break;
            boolean hov = AbyssUI.isHovered(mx, my, px + 1, ry, pw - 2, ROW_H);
            AbyssUI.drawRow(g, px + 1, ry, pw - 2, hov, i % 2 == 0);
            g.drawString(font, b.name(), x, ry + 6, AbyssUI.TEXT, false);

            String bound = bindSlots.entrySet().stream()
                .filter(e -> e.getValue().equals(b.name()))
                .map(e -> "\u00a7b" + e.getKey())
                .findFirst().orElse("\u00a70-");
            g.drawString(font, bound, x + 150, ry + 6, AbyssUI.TEXT, false);

            // Quick bind slots 1-9
            int slotX = x + 220;
            for (int slot = 1; slot <= 9; slot++) {
                boolean isBound = b.name().equals(bindSlots.get(slot));
                int bg  = isBound ? 0xFF0D2A3A : AbyssUI.BTN;
                int bdr = isBound ? AbyssUI.BORDER_ACTIVE : AbyssUI.BORDER;
                g.fill(slotX, ry + 2, slotX + 12, ry + 18, bg);
                AbyssUI.drawBorder(g, slotX, ry + 2, 12, 16, bdr);
                g.drawCenteredString(font, isBound ? "\u00a7b" + slot : "\u00a70" + slot,
                    slotX + 6, ry + 6, AbyssUI.TEXT);
                slotX += 14;
            }

            // Run button
            AbyssUI.drawButton(g, font, slotX + 4, ry + 2, BTN_SM, 16, "\u00a7aRun", hov, false);
        }
        g.disableScissor();
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int ox = PAD * 2, oy = PAD * 2, oh = height - PAD * 4;

        // Sidebar category click
        int catY = oy + 32 + AbyssUI.PAD / 2;
        for (int i = 0; i < CATS.length; i++) {
            if (AbyssUI.isHovered(mx, my, ox + 1, catY, SIDE_W - 2, ROW_H)) {
                if (selectedCat != i) { selectedCat = i; editingTitle = runningTitle = -1; init(); }
                return true;
            }
            catY += ROW_H + 2;
        }

        int contentY = py + AbyssUI.HEADER_H + AbyssUI.PAD;

        switch (selectedCat) {
            case 0 -> handlePlayerClick(mx, my, contentY);
            case 1 -> handleTitleClick(mx, my, contentY);
            case 2 -> handleVanishClick(mx, my, contentY);
            case 3 -> handleRegionClick(mx, my);
            case 4 -> handleDimClick(mx, my, contentY);
            case 5 -> handleHelpClick(mx, my, contentY);
            case 6 -> handleBulkClick(mx, my, contentY);
        }

        return super.mouseClicked(mx, my, btn);
    }

    private void handlePlayerClick(double mx, double my, int y) {
        int x = px + PAD;
        y += 16; // skip header row + divider
        for (int i = playerScroll; i < players.size(); i++) {
            int ry = y + (i - playerScroll) * ROW_H;
            if (!AbyssUI.isHovered(mx, my, px + 1, ry, pw - 2, ROW_H)) continue;
            var p = players.get(i);
            if (AbyssUI.isHovered(mx, my, x + 136, ry + 2, 36, 16)) { togglePlayer(i, 0); return; }
            if (AbyssUI.isHovered(mx, my, x + 182, ry + 2, 36, 16)) { togglePlayer(i, 1); return; }
            if (AbyssUI.isHovered(mx, my, x + 228, ry + 2, 36, 16)) { togglePlayer(i, 2); return; }
            if (AbyssUI.isHovered(mx, my, x + 266, ry + 2, 36, 16)) { togglePlayer(i, 3); return; }
        }
    }

    private void handleTitleClick(double mx, double my, int y) {
        if (editingTitle != -1 || runningTitle != -1) return;
        y += ROW_H + AbyssUI.PAD;
        for (int i = titleScroll; i < titles.size(); i++) {
            int ry = y + (i - titleScroll) * ROW_H;
            if (!AbyssUI.isHovered(mx, my, px + 1, ry, pw - 2, ROW_H)) continue;
            int bx = px + pw - AbyssUI.PAD - BTN_SM * 2 - 4;
            if (AbyssUI.isHovered(mx, my, bx,          ry+2, BTN_SM, 16)) { runningTitle = i; init(); return; }
            if (AbyssUI.isHovered(mx, my, bx+BTN_SM+4, ry+2, BTN_SM, 16)) { editingTitle = i; init(); return; }
        }
    }

    private void handleVanishClick(double mx, double my, int y) {
        // Team visibility toggle
        if (AbyssUI.isHovered(mx, my, px + pw - AbyssUI.PAD - 50, y + 2, 50, 16)) {
            PacketDistributor.sendToServer(MenuActionPacket.of(MenuActionPacket.Action.TOGGLE_TEAM_VISIBILITY));
            teamVisibility = !teamVisibility;
            return;
        }
        y += ROW_H + AbyssUI.PAD * 2 + 14;
        var vanished = players.stream().filter(OpenMainMenuPacket.PlayerState::vanished).toList();
        for (int i = 0; i < vanished.size(); i++) {
            int ry = y + i * ROW_H;
            if (!AbyssUI.isHovered(mx, my, px + 1, ry, pw - 2, ROW_H)) continue;
            var p = vanished.get(i);
            int bx = px + pw - AbyssUI.PAD - 46 * 3 - 8;
            if (AbyssUI.isHovered(mx, my, bx,    ry+2, 44, 16)) { PacketDistributor.sendToServer(new MenuActionPacket(MenuActionPacket.Action.VANISH_SHOW_TO,   p.uuid(), p.uuid().toString(), "", 0)); return; }
            if (AbyssUI.isHovered(mx, my, bx+48, ry+2, 44, 16)) { PacketDistributor.sendToServer(new MenuActionPacket(MenuActionPacket.Action.VANISH_HIDE_FROM, p.uuid(), p.uuid().toString(), "", 0)); return; }
            if (AbyssUI.isHovered(mx, my, bx+96, ry+2, 44, 16)) { PacketDistributor.sendToServer(new MenuActionPacket(MenuActionPacket.Action.VANISH_CLEAR,     null,     p.uuid().toString(), "", 0)); return; }
        }
    }

    private void handleRegionClick(double mx, double my) {
        int bw = 160, bh = 20;
        int bx = px + pw / 2 - bw / 2;
        int by = py + ph / 2 - bh / 2;
        if (AbyssUI.isHovered(mx, my, bx, by, bw, bh)) {
            PacketDistributor.sendToServer(new RequestRegionScreenPacket());
        }
    }

    private void handleDimClick(double mx, double my, int y) {
        int cbw = 100, cbh = AbyssUI.BTN_H;
        int cbx = px + pw - AbyssUI.PAD - cbw;
        int cby = py + AbyssUI.HEADER_H + AbyssUI.PAD;
        if (AbyssUI.isHovered(mx, my, cbx, cby, cbw, cbh)) {
            Minecraft.getInstance().setScreen(new DimenCreateScreen());
            return;
        }
        y += 16;
        for (int i = dimScroll; i < dims.size(); i++) {
            int ry = y + (i - dimScroll) * ROW_H;
            if (!AbyssUI.isHovered(mx, my, px + 1, ry, pw - 2, ROW_H)) continue;
            if (AbyssUI.isHovered(mx, my, px + pw - AbyssUI.PAD - BTN_SM, ry+2, BTN_SM, 16)) {
                PacketDistributor.sendToServer(MenuActionPacket.stringAction(MenuActionPacket.Action.DIMEN_TP, dims.get(i).name(), ""));
            }
        }
    }

    private void handleHelpClick(double mx, double my, int y) {
        for (int i = helpScroll; i < helpRequests.size(); i++) {
            int ry = y + (i - helpScroll) * ROW_H;
            if (!AbyssUI.isHovered(mx, my, px + 1, ry, pw - 2, ROW_H)) continue;
            if (AbyssUI.isHovered(mx, my, px + pw - AbyssUI.PAD - BTN_SM, ry+2, BTN_SM, 16)) {
                PacketDistributor.sendToServer(MenuActionPacket.playerAction(MenuActionPacket.Action.HELP_ACCEPT, helpRequests.get(i).playerUUID()));
                helpRequests.remove(i);
                return;
            }
        }
    }

    private void handleBulkClick(double mx, double my, int y) {
        int x = px + AbyssUI.PAD;
        y += 16;
        for (int i = bulkScroll; i < bulkCommands.size(); i++) {
            var b = bulkCommands.get(i);
            int ry = y + (i - bulkScroll) * ROW_H;
            if (!AbyssUI.isHovered(mx, my, px + 1, ry, pw - 2, ROW_H)) continue;
            // Slots
            int slotX = x + 220;
            for (int slot = 1; slot <= 9; slot++) {
                if (AbyssUI.isHovered(mx, my, slotX, ry + 2, 12, 16)) {
                    boolean isBound = b.name().equals(bindSlots.get(slot));
                    if (isBound) {
                        PacketDistributor.sendToServer(MenuActionPacket.intAction(MenuActionPacket.Action.BULK_UNBIND, "", slot));
                        bindSlots.remove(slot);
                    } else {
                        PacketDistributor.sendToServer(MenuActionPacket.intAction(MenuActionPacket.Action.BULK_BIND, b.name(), slot));
                        bindSlots.put(slot, b.name());
                    }
                    return;
                }
                slotX += 14;
            }
            // Run
            if (AbyssUI.isHovered(mx, my, slotX + 4, ry + 2, BTN_SM, 16)) {
                PacketDistributor.sendToServer(MenuActionPacket.stringAction(MenuActionPacket.Action.BULK_RUN, b.name(), ""));
                return;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int d = -(int) dy;
        switch (selectedCat) {
            case 0 -> playerScroll = clamp(playerScroll + d, players.size());
            case 1 -> titleScroll  = clamp(titleScroll  + d, titles.size());
            case 4 -> dimScroll    = clamp(dimScroll    + d, dims.size());
            case 5 -> helpScroll   = clamp(helpScroll   + d, helpRequests.size());
            case 6 -> bulkScroll   = clamp(bulkScroll   + d, bulkCommands.size());
        }
        return true;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void togglePlayer(int idx, int col) {
        var p = players.get(idx);
        MenuActionPacket.Action action = switch (col) {
            case 0 -> MenuActionPacket.Action.TOGGLE_STAFF;
            case 1 -> MenuActionPacket.Action.TOGGLE_VANISH;
            case 2 -> MenuActionPacket.Action.TOGGLE_GOD;
            default -> MenuActionPacket.Action.TOGGLE_BLIND;
        };
        PacketDistributor.sendToServer(MenuActionPacket.playerAction(action, p.uuid()));
        players.set(idx, new OpenMainMenuPacket.PlayerState(
            p.uuid(), p.name(),
            col == 1 ? !p.vanished()  : p.vanished(),
            col == 2 ? !p.godMode()   : p.godMode(),
            col == 3 ? !p.blinded()   : p.blinded()
        ));
    }

    private void onSaveTitle() {
        if (titleNameBox == null || titleTextBox == null) return;
        String name = titleNameBox.getValue().trim();
        String text = titleTextBox.getValue().trim();
        if (name.isEmpty() || text.isEmpty()) return;
        String id = (editingTitle >= 0 && editingTitle < titles.size()) ? titles.get(editingTitle).id() : "";
        PacketDistributor.sendToServer(new SaveTitlePacket(id, name, text,
            titleSubBox != null ? titleSubBox.getValue().trim() : "",
            parseInt(titleFadeInBox, 10), parseInt(titleStayBox, 70), parseInt(titleFadeOutBox, 20)));
        editingTitle = -1;
        onClose();
    }

    private void sendTitle(MenuActionPacket.Action action, String id, String arg) {
        PacketDistributor.sendToServer(MenuActionPacket.stringAction(action, id, arg));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void drawEmpty(GuiGraphics g, String msg) {
        g.drawCenteredString(font, "\u00a77" + msg, px + pw / 2, py + ph / 2, AbyssUI.TEXT_MUTED);
    }

    private int clamp(int val, int size) {
        return Math.max(0, Math.min(val, Math.max(0, size - 12)));
    }

    private int parseInt(EditBox box, int def) {
        if (box == null) return def;
        try { return Integer.parseInt(box.getValue().trim()); } catch (Exception e) { return def; }
    }

    private String shorten(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float delta) {}
}
