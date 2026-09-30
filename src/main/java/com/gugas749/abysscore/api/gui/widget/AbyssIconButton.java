package com.gugas749.abysscore.api.gui.widget;

import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A small button that is just an icon (e.g. the panel's close "X").
 * The label is never drawn — it's used as tooltip and for narration.
 */
public class AbyssIconButton extends AbstractButton {

    private final AbyssTheme.Sprite icon, iconHover;
    private final Runnable onPress;

    public AbyssIconButton(int x, int y, AbyssTheme.Sprite icon, AbyssTheme.Sprite iconHover,
                           Component label, Runnable onPress) {
        super(x, y, icon.width(), icon.height(), label);
        this.icon = icon;
        this.iconHover = iconHover;
        this.onPress = onPress;
        setTooltip(Tooltip.create(label));
    }

    @Override
    public void onPress() {
        onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        AbyssDraw.sprite(g, isHoveredOrFocused() ? iconHover : icon, getX(), getY());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
