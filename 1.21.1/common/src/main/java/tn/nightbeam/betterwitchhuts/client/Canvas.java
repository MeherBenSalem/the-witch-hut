package tn.nightbeam.betterwitchhuts.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * The few drawing calls the Coven screen uses, named as in the 26.x builds, over 1.21.1's {@link GuiGraphics}. The
 * tooltip is kept and drawn after everything else, as 26.x's "tooltip for next frame" does.
 */
public final class Canvas {
    private final GuiGraphics g;
    private Font tooltipFont;
    private List<Component> tooltip;
    private int tooltipX;
    private int tooltipY;

    public Canvas(GuiGraphics g) {
        this.g = g;
    }

    public void fill(int x1, int y1, int x2, int y2, int colour) {
        g.fill(x1, y1, x2, y2, colour);
    }

    public void outline(int x, int y, int w, int h, int colour) {
        g.renderOutline(x, y, w, h, colour);
    }

    public void text(Font font, Component text, int x, int y, int colour, boolean shadow) {
        g.drawString(font, text, x, y, colour, shadow);
    }

    public void text(Font font, String text, int x, int y, int colour, boolean shadow) {
        g.drawString(font, text, x, y, colour, shadow);
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int colour, boolean shadow) {
        g.drawString(font, text, x, y, colour, shadow);
    }

    public void centeredText(Font font, Component text, int x, int y, int colour) {
        g.drawString(font, text, x - font.width(text) / 2, y, colour, false);
    }

    public void textWithWordWrap(Font font, FormattedText text, int x, int y, int width, int colour, boolean shadow) {
        int line = 0;
        for (FormattedCharSequence part : font.split(text, width)) {
            g.drawString(font, part, x, y + line * font.lineHeight, colour, shadow);
            line++;
        }
    }

    public void blit(ResourceLocation texture, int x, int y, float u, float v, int w, int h, int texW, int texH) {
        g.blit(texture, x, y, u, v, w, h, texW, texH);
    }

    public void item(ItemStack stack, int x, int y) {
        g.renderItem(stack, x, y);
    }

    public void setTooltipForNextFrame(Font font, List<Component> lines, int x, int y) {
        tooltipFont = font;
        tooltip = lines;
        tooltipX = x;
        tooltipY = y;
    }

    public void drawTooltip() {
        if (tooltip != null) {
            g.renderComponentTooltip(tooltipFont, tooltip, tooltipX, tooltipY);
        }
    }
}
