package com.gugas749.abysscore.client.ui.screens;

import com.gugas749.abysscore.client.ui.AbyssUI;
import com.gugas749.abysscore.network.dimen.SubmitDimenCreatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Random;

public class DimenCreateScreen extends Screen {

    private static final int W = 300;
    private static final int H = 230;
    private static final int PAD = AbyssUI.PAD;

    private int px, py;

    private EditBox nameBox;
    private EditBox displayNameBox;
    private EditBox seedBox;
    private CycleButton<String> styleButton;
    private Button randomSeedButton;

    public DimenCreateScreen() {
        super(Component.translatable("screen.abysscore.dimen.create.title"));
    }

    @Override
    protected void init() {
        px = (width  - W) / 2;
        py = (height - H) / 2;

        int x  = px + PAD + 4;
        int fw = W - (PAD + 4) * 2;
        int hw = (fw - 4) / 2;
        int y  = py + AbyssUI.HEADER_H + PAD * 2;

        nameBox = abyssField(x, y, fw, "e.g. my_world"); y += 26;
        displayNameBox = abyssField(x, y, fw, "e.g. My World"); y += 26;

        styleButton = CycleButton.<String>builder(s ->
            Component.literal(s.charAt(0) + s.substring(1).toLowerCase()))
            .withValues("NORMAL", "SUPERFLAT", "VOID")
            .create(x, y, fw, 16,
                Component.translatable("screen.abysscore.dimen.create.style"));
        addRenderableWidget(styleButton);
        y += 26;

        seedBox = abyssField(x, y, hw, "0 = random");
        seedBox.setMaxLength(20);
        seedBox.setValue("0");

        randomSeedButton = Button.builder(
            Component.literal("\u21BA Random"),
            btn -> seedBox.setValue(String.valueOf(new Random().nextLong()))
        ).bounds(x + hw + 4, y, hw, 16).build();
        addRenderableWidget(randomSeedButton);

        // Bottom buttons
        int by = py + H - PAD - 18;
        addRenderableWidget(Button.builder(
            Component.translatable("screen.abysscore.dimen.create.create"),
            btn -> onCreate()
        ).bounds(x, by, 100, 18).build());

        addRenderableWidget(Button.builder(
            Component.literal("Cancel"),
            btn -> onClose()
        ).bounds(px + W - PAD - 4 - 80, by, 80, 18).build());
    }

    @Override
    public void tick() {
        boolean isNormal = "NORMAL".equals(styleButton.getValue());
        seedBox.setVisible(isNormal);
        randomSeedButton.visible = isNormal;
    }

    private EditBox abyssField(int x, int y, int w, String hint) {
        EditBox box = new EditBox(font, x, y, w, 16, Component.literal(hint));
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
            rawName, displayNameBox.getValue().trim(), styleButton.getValue(), seed));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        AbyssUI.drawBackground(g, width, height);
        AbyssUI.drawPanel(g, px, py, W, H);
        AbyssUI.drawHeader(g, font, px, py, W, "\u2318  Create Dimension");

        int x = px + PAD + 4;
        int y = py + AbyssUI.HEADER_H + PAD * 2;

        // Field labels + backgrounds
        drawFieldLabel(g, x, y, "Internal Name"); drawFieldBg(g, x, y, W - (PAD+4)*2); y += 26;
        drawFieldLabel(g, x, y, "Display Name");  drawFieldBg(g, x, y, W - (PAD+4)*2); y += 26;
        drawFieldLabel(g, x, y, "Style"); y += 26;

        if ("NORMAL".equals(styleButton.getValue())) {
            drawFieldLabel(g, x, y, "Seed");
            int hw = (W - (PAD+4)*2 - 4) / 2;
            drawFieldBg(g, x, y, hw);
        }

        super.render(g, mx, my, delta);
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
