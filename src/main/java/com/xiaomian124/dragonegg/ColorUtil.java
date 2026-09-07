package com.xiaomian124.dragonegg;

import java.awt.Color;

public class ColorUtil {

    // ColorUtil.java留着备用吧...
    public static String gradient(String text, Color start, Color end) {
        if (text == null || text.isEmpty()) return "";
        int length = text.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (length - 1);
            Color color = lerpColor(start, end, ratio);
            sb.append(colorToMinecraft(color));
            sb.append(text.charAt(i));
        }
        return sb.toString();
    }

    private static Color lerpColor(Color a, Color b, float t) {
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int b2 = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(clamp(r), clamp(g), clamp(b2));
    }

    private static int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }

    private static String colorToMinecraft(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        return String.format("§x§%02x§%02x§%02x", r, g, b);
    }
}