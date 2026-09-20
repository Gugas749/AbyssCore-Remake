package com.gugas749.abysscore.client.ui.screens;

import com.gugas749.abysscore.client.ui.AbyssUI;
import com.gugas749.abysscore.network.bulk.SubmitBulkCommandPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BulkCommandScreen extends Screen {

    private static final int W            = 360;
    private static final int H            = 280;
    private static final int PAD          = AbyssUI.PAD;
    private static final int ROW_H        = 22;
    private static final int MAX_COMMANDS = 10;
    private static final int VISIBLE_ROWS = 6;

    private int px, py;

    private EditBox nameBox;
    private CycleButton<Integer> permButton;
    private final List<EditBox> commandBoxes = new ArrayList<>();
    private int scrollOffset = 0;

    // Saved state for rebuild
    private String savedName = "";
    private int savedPerm = 0;
    private final List<String> savedCmds = new ArrayList<>();

    public BulkCommandScreen() {
        super(Component.translatable("screen.abysscore.bulk.title"));
    }

    @Override
    protected void init() {
        px = (width  - W) / 2;
        py = (height - H) / 2;

        int x  = px + PAD + 4;
        int fw = W - (PAD + 4) * 2;
        int y  = py + AbyssUI.HEADER_H + PAD * 2;

        // Name field
        nameBox = abyssField(x, y, fw, "Bulk command name..."); y += 26;
        nameBox.setValue(savedName);

        // Permission cycle
        permButton = CycleButton.<Integer>builder(level ->
            level == 0
                ? Component.translatable("screen.abysscore.bulk.perm_everyone")
                : Component.translatable("screen.abysscore.bulk.perm_op"))
            .withValues(0, 2)
            .create(x, y, fw, 16,
                Component.translatable("screen.abysscore.bulk.perm_label"));
        addRenderableWidget(permButton);
        y += 26;

        // Commands list
        int listY = y;
        if (commandBoxes.isEmpty()) {
            commandBoxes.add(makeCommandBox(0));
            if (!savedCmds.isEmpty()) commandBoxes.get(0).setValue(savedCmds.get(0));
        }
        rebuildCommandWidgets(x, listY, fw);

        // Bottom buttons
        int by = py + H - PAD - 18;

        addRenderableWidget(Button.builder(
            Component.literal("+ Add"),
            btn -> onAddRow()
        ).bounds(x, by, 60, 18).build());

        addRenderableWidget(Button.builder(
            Component.translatable("screen.abysscore.bulk.save"),
            btn -> onSave()
        ).bounds(px + W / 2 - 50, by, 100, 18).build());

        addRenderableWidget(Button.builder(
            Component.literal("Cancel"),
            btn -> onClose()
        ).bounds(px + W - PAD - 4 - 80, by, 80, 18).build());
    }

    private void rebuildCommandWidgets(int x, int listY, int fw) {
        commandBoxes.forEach(this::removeWidget);
        int start = Math.min(scrollOffset, Math.max(0, commandBoxes.size() - VISIBLE_ROWS));
        int visible = Math.min(VISIBLE_ROWS, commandBoxes.size() - start);
        for (int i = 0; i < visible; i++) {
            int idx = start + i;
            if (idx >= commandBoxes.size()) break;
            EditBox box = commandBoxes.get(idx);
            box.setX(x);
            box.setY(listY + i * ROW_H + 2);
            box.setWidth(fw - 20);
            addRenderableWidget(box);

            final int ri = idx;
            addRenderableWidget(Button.builder(
                Component.literal("\u00d7"),
                btn -> onRemoveRow(ri)
            ).bounds(x + fw - 16, listY + i * ROW_H + 2, 16, 16).build());
        }
    }

    private EditBox abyssField(int x, int y, int w, String hint) {
        EditBox box = new EditBox(font, x, y, w, 16, Component.literal(hint));
        box.setMaxLength(256);
        box.setHint(Component.literal(hint));
        box.setBordered(false);
        addRenderableWidget(box);
        return box;
    }

    private EditBox makeCommandBox(int index) {
        EditBox box = new EditBox(font, 0, 0, 100, 16,
            Component.literal("Command " + (index + 1)));
        box.setMaxLength(256);
        box.setHint(Component.literal("/" + (index + 1)));
        box.setBordered(false);
        return box;
    }

    private void onAddRow() {
        if (commandBoxes.size() >= MAX_COMMANDS) return;
        commandBoxes.add(makeCommandBox(commandBoxes.size()));
        if (commandBoxes.size() > VISIBLE_ROWS) scrollOffset = commandBoxes.size() - VISIBLE_ROWS;
        rebuildWithState();
    }

    private void onRemoveRow(int idx) {
        if (commandBoxes.size() <= 1) return;
        commandBoxes.remove(idx);
        scrollOffset = Math.max(0, Math.min(scrollOffset, commandBoxes.size() - VISIBLE_ROWS));
        rebuildWithState();
    }

    private void rebuildWithState() {
        savedName = nameBox != null ? nameBox.getValue() : savedName;
        savedPerm = permButton != null ? permButton.getValue() : savedPerm;
        savedCmds.clear();
        commandBoxes.forEach(b -> savedCmds.add(b.getValue()));
        clearWidgets();
        commandBoxes.clear();
        init();
        // Restore command boxes
        for (int i = 0; i < savedCmds.size() && i < commandBoxes.size(); i++) {
            commandBoxes.get(i).setValue(savedCmds.get(i));
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int listY = py + AbyssUI.HEADER_H + PAD * 2 + 26 + 26;
        if (AbyssUI.isHovered(mx, my, px + PAD, listY, W - PAD * 2, VISIBLE_ROWS * ROW_H)) {
            scrollOffset -= (int) Math.signum(sy);
            scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, commandBoxes.size() - VISIBLE_ROWS)));
            rebuildWithState();
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private void onSave() {
        String name = nameBox != null ? nameBox.getValue().trim() : "";
        if (name.isEmpty()) return;
        List<String> cmds = commandBoxes.stream()
            .map(b -> b.getValue().trim())
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        if (cmds.isEmpty()) return;
        PacketDistributor.sendToServer(new SubmitBulkCommandPacket(
            name, permButton != null ? permButton.getValue() : 0, cmds));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        AbyssUI.drawBackground(g, width, height);
        AbyssUI.drawPanel(g, px, py, W, H);
        AbyssUI.drawHeader(g, font, px, py, W, "\u2630  Bulk Command");

        int x = px + PAD + 4;
        int fw = W - (PAD + 4) * 2;
        int y = py + AbyssUI.HEADER_H + PAD * 2;

        // Name field bg + label
        drawFieldLabel(g, x, y, "Name"); drawFieldBg(g, x, y, fw); y += 26;
        // Perm label
        drawFieldLabel(g, x, y, "Permission"); y += 26;
        // Commands label
        drawFieldLabel(g, x, y, "Commands");

        // Scroll indicator
        if (commandBoxes.size() > VISIBLE_ROWS) {
            int start = Math.min(scrollOffset, commandBoxes.size() - VISIBLE_ROWS);
            String si = (start+1) + "-" + Math.min(start+VISIBLE_ROWS, commandBoxes.size()) + "/" + commandBoxes.size();
            g.drawString(font, "\u00a70" + si, px + W - PAD - 4 - font.width(si), y, AbyssUI.TEXT_MUTED, false);
        }
        if (commandBoxes.size() >= MAX_COMMANDS) {
            g.drawString(font, "\u00a7cMax " + MAX_COMMANDS,
                x + 80, y, AbyssUI.TEXT_MUTED, false);
        }
        y += 10;

        // Command row backgrounds
        int listY = y;
        int start = Math.min(scrollOffset, Math.max(0, commandBoxes.size() - VISIBLE_ROWS));
        for (int i = 0; i < VISIBLE_ROWS && (start + i) < commandBoxes.size(); i++) {
            int ry = listY + i * ROW_H;
            g.fill(x - 2, ry, x + fw - 16 + 2, ry + 16, AbyssUI.PANEL_LIGHT);
            AbyssUI.drawBorder(g, x - 2, ry, fw - 16 + 4, 16, AbyssUI.BORDER);
            g.drawString(font, "\u00a70" + (start + i + 1) + ".",
                x - 2 + 2, ry + 4, AbyssUI.TEXT_MUTED, false);
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
