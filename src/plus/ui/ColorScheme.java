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
        // 色值由用户环境 Proxy 实际渲染逐条取色校准（十六进制），与 Proxy history 高亮完全一致
        COLORS.put("red", new Color(0xFF6464));
        COLORS.put("orange", new Color(0xFFC864));
        COLORS.put("magenta", new Color(0xFF64FF));
        COLORS.put("pink", new Color(0xFFC8C8));
        COLORS.put("yellow", new Color(0xFFFF64));
        COLORS.put("cyan", new Color(0x64FFFF));
        COLORS.put("blue", new Color(0x6464FF));
        COLORS.put("green", new Color(0x64FF64));
        COLORS.put("gray", new Color(0xB4B4B4));
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
