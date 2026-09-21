package com.gugas749.abysscore.client.ui.screens;

import com.gugas749.abysscore.client.ui.AbyssUI;
import com.gugas749.abysscore.network.dimen.SubmitDimenCreatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Random;

public class DimenCreateScreen extends Screen {

    private static final int W = 300;
    private static final int H = 260;
    private static final int PAD = AbyssUI.PAD;

    private int px, py;
    private final Screen previousScreen;

    private EditBox nameBox;
    private EditBox displayNameBox;
    private EditBox seedBox;
    // Style and Random Seed are drawn manually — no vanilla widgets
    private String currentStyle = "NORMAL";
    private static final String[] STYLES = {"NORMAL", "SUPERFLAT", "VOID"};

    public DimenCreateScreen(Screen previousScreen) {
        super(Component.translatable("screen.abysscore.dimen.create.title"));
        this.previousScreen = previousScreen;
    }

    // Backwards-compatible no-arg constructor
    public DimenCreateScreen() { this(null); }

    @Override
    protected void init() {
        px = (width  - W) / 2;
        py = (height - H) / 2;

        int x  = px + PAD + 4;
        int fw = W - (PAD + 4) * 2;
        int hw = (fw - 4) / 2;
        int y  = py + AbyssUI.HEADER_H + PAD * 2;

        nameBox = abyssField(x, y, fw, "e.g. my_world"); y += 34;
        displayNameBox = abyssField(x, y, fw, "e.g. My World"); y += 34;
        // Style row — drawn manually, y += 34
        y += 34;
        seedBox = abyssField(x, y, hw, "0 = random");
        seedBox.setMaxLength(20);
        seedBox.setValue("0");
        // Random button drawn manually
    }

    private void cycleStyle() {
        for (int i = 0; i < STYLES.length; i++) {
            if (STYLES[i].equals(currentStyle)) {
                currentStyle = STYLES[(i + 1) % STYLES.length];
                return;
            }
        }
    }

    private EditBox abyssField(int x, int y, int w, String hint) {
        EditBox box = new EditBox(font, x, y + 1, w, 16, Component.literal(hint));
        box.setMaxLength(64);
        box.setHint(Component.literal(hint));
        box.setBordered(false);
        addRenderableWidget(box);
        return box;
    }

    private void onCreate() {
        String rawName = nameBox.getValue().trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
        if (rawName.isEmpty()) return;
        long seed = 0;
        try { seed = Long.parseLong(seedBox.getValue().trim()); }
        catch (NumberFormatException ignored) {}
        PacketDistributor.sendToServer(new SubmitDimenCreatePacket(
            rawName, displayNameBox.getValue().trim(), currentStyle, seed));
        net.minecraft.client.Minecraft.getInstance().setScreen(previousScreen);
    }

    private void goBack() {
        net.minecraft.client.Minecraft.getInstance().setScreen(previousScreen);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        AbyssUI.drawBackground(g, width, height);
        AbyssUI.drawPanel(g, px, py, W, H);
        AbyssUI.drawHeader(g, font, px, py, W, "\u2318  Create Dimension");

        int x  = px + PAD + 4;
        int fw = W - (PAD + 4) * 2;
        int hw = (fw - 4) / 2;
        int y  = py + AbyssUI.HEADER_H + PAD * 2;

        // Internal Name
        drawFieldLabel(g, x, y, "Internal Name"); drawFieldBg(g, x, y, fw); y += 34;
        // Display Name
        drawFieldLabel(g, x, y, "Display Name"); drawFieldBg(g, x, y, fw); y += 34;

        // Style — Abyss-style cycle button
        drawFieldLabel(g, x, y, "Style");
        String styleLabel = currentStyle.charAt(0) + currentStyle.substring(1).toLowerCase();
        boolean styleHov = AbyssUI.isHovered(mx, my, x, y, fw, 16);
        AbyssUI.drawButton(g, font, x, y, fw, 16, styleLabel, styleHov, false);
        y += 34;

        // Seed row (only for NORMAL)
        if ("NORMAL".equals(currentStyle)) {
            drawFieldLabel(g, x, y, "Seed");
            drawFieldBg(g, x, y, hw);
            // Random button — Abyss style
            boolean randHov = AbyssUI.isHovered(mx, my, x + hw + 4, y, hw, 16);
            AbyssUI.drawButton(g, font, x + hw + 4, y, hw, 16, "\u21BA Random", randHov, false);
            seedBox.setVisible(true);
        } else {
            seedBox.setVisible(false);
        }

        // Bottom buttons
        int by = py + H - PAD - 20;
        int btnW = 100;
        AbyssUI.drawButton(g, font, x, by, btnW, 18,
            "\u00a7bCreate", AbyssUI.isHovered(mx, my, x, by, btnW, 18), false);
        AbyssUI.drawButton(g, font, px + W - PAD - 4 - 80, by, 80, 18,
            "\u00a7cCancel", AbyssUI.isHovered(mx, my, px + W - PAD - 4 - 80, by, 80, 18), true);

        super.render(g, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x  = px + PAD + 4;
        int fw = W - (PAD + 4) * 2;
        int hw = (fw - 4) / 2;
        int y  = py + AbyssUI.HEADER_H + PAD * 2 + 34 + 34; // style row y
        int by = py + H - PAD - 20;
        int btnW = 100;

        // Style cycle
        if (AbyssUI.isHovered(mx, my, x, y, fw, 16)) { cycleStyle(); return true; }
        // Random seed
        if ("NORMAL".equals(currentStyle) && AbyssUI.isHovered(mx, my, x + hw + 4, y + 34, hw, 16)) {
            seedBox.setValue(String.valueOf(new Random().nextLong())); return true;
        }
        // Create
        if (AbyssUI.isHovered(mx, my, x, by, btnW, 18)) { onCreate(); return true; }
        // Cancel
        if (AbyssUI.isHovered(mx, my, px + W - PAD - 4 - 80, by, 80, 18)) { goBack(); return true; }

        return super.mouseClicked(mx, my, button);
    }

    private void drawFieldLabel(GuiGraphics g, int x, int y, String label) {
        g.drawString(font, "\u00a77" + label, x, y - 9, AbyssUI.TEXT_MUTED, false);
    }

    private void drawFieldBg(GuiGraphics g, int x, int y, int w) {
        g.fill(x - 2, y - 1, x + w + 2, y + 17, AbyssUI.PANEL_LIGHT);
        AbyssUI.drawBorder(g, x - 2, y - 1, w + 4, 18, AbyssUI.BORDER);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float delta) {}
}
