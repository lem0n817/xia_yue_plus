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
        // 与 Burp 原生 highlight 色板一致（亮色主题），保证与 Proxy history 高亮观感相同
        COLORS.put("red", new Color(255, 102, 102));
        COLORS.put("orange", new Color(255, 166, 102));
        COLORS.put("magenta", new Color(255, 102, 255));
        COLORS.put("pink", new Color(255, 153, 255));
        COLORS.put("yellow", new Color(255, 255, 102));
        COLORS.put("cyan", new Color(102, 255, 255));
        COLORS.put("blue", new Color(102, 153, 255));
        COLORS.put("green", new Color(102, 255, 102));
        COLORS.put("gray", new Color(204, 204, 204));
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
