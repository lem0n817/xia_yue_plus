package plus.ui;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * HaE 色名 → Swing 颜色映射，以及行染色所需的 alpha 混合。
 * 色名与 Burp 原生 highlight 色一致，便于 setHighlightColor 透传。
 */
public final class ColorScheme {

    private static final Map<String, Color> COLORS = new HashMap<String, Color>();

    static {
        COLORS.put("red", new Color(225, 65, 65));
        COLORS.put("orange", new Color(240, 140, 20));
        COLORS.put("magenta", new Color(200, 40, 200));
        COLORS.put("pink", new Color(240, 100, 170));
        COLORS.put("yellow", new Color(216, 186, 0));
        COLORS.put("cyan", new Color(20, 170, 180));
        COLORS.put("blue", new Color(70, 120, 220));
        COLORS.put("green", new Color(70, 160, 70));
        COLORS.put("gray", new Color(130, 130, 130));
    }

    private ColorScheme() {
    }

    public static Color of(String name) {
        if (name == null) {
            return null;
        }
        return COLORS.get(name.toLowerCase());
    }

    /** 按规则色以 alpha 比例叠加到当前单元格背景上，明暗主题均适用 */
    public static Color blend(Color base, Color tint, float alpha) {
        if (base == null) {
            base = Color.WHITE;
        }
        if (tint == null) {
            return base;
        }
        float inv = 1f - alpha;
        int r = Math.round(base.getRed() * inv + tint.getRed() * alpha);
        int g = Math.round(base.getGreen() * inv + tint.getGreen() * alpha);
        int b = Math.round(base.getBlue() * inv + tint.getBlue() * alpha);
        return new Color(r, g, b);
    }
}
