package com.gugas749.abysscore.api.overlay;

/**
 * WHAT an overlay looks like. Immutable — the "with..." methods return a copy.
 *
 *   AbyssOverlaySpec.tint(0xAA0000, 0.25f)                 // flat red tint, 25% opacity
 *   AbyssOverlaySpec.vignette(0x5A0080, 0.6f).fade(10, 20) // purple edges, fade in/out
 *
 * @param color              RGB, no alpha (0xRRGGBB)
 * @param maxAlpha           opacity at full strength, 0.0–1.0
 * @param style              TINT = whole screen, VIGNETTE = darkened edges only
 * @param fadeInTicks        ticks to go from 0 to maxAlpha (20 ticks = 1 second)
 * @param fadeOutTicks       ticks to go from maxAlpha back to 0 at the end
 * @param scaleWithAmplifier effect overlays only: level I = 25%, II = 50%, III = 75%, IV+ = 100%
 */
public record AbyssOverlaySpec(int color, float maxAlpha, Style style,
                               int fadeInTicks, int fadeOutTicks, boolean scaleWithAmplifier) {

    public enum Style { TINT, VIGNETTE }

    public AbyssOverlaySpec {
        color    = color & 0xFFFFFF;                       // drop any alpha the caller passed
        maxAlpha = Math.max(0f, Math.min(1f, maxAlpha));
        fadeInTicks  = Math.max(0, fadeInTicks);
        fadeOutTicks = Math.max(0, fadeOutTicks);
    }

    public static AbyssOverlaySpec tint(int rgb, float alpha) {
        return new AbyssOverlaySpec(rgb, alpha, Style.TINT, 5, 10, false);
    }

    public static AbyssOverlaySpec vignette(int rgb, float alpha) {
        return new AbyssOverlaySpec(rgb, alpha, Style.VIGNETTE, 5, 10, false);
    }

    public AbyssOverlaySpec fade(int inTicks, int outTicks) {
        return new AbyssOverlaySpec(color, maxAlpha, style, inTicks, outTicks, scaleWithAmplifier);
    }

    public AbyssOverlaySpec scaleWithAmplifier() {
        return new AbyssOverlaySpec(color, maxAlpha, style, fadeInTicks, fadeOutTicks, true);
    }
}
