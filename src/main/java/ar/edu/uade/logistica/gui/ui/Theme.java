package ar.edu.uade.logistica.gui.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Design tokens: colores, tipografia y espaciado. Centralizado para mantener consistencia
 * visual en toda la GUI.
 */
public final class Theme {

    public static final Color BG = new Color(0xF6F7FB);
    public static final Color SURFACE = Color.WHITE;
    public static final Color SURFACE_ALT = new Color(0xF1F5F9);
    public static final Color SURFACE_HOVER = new Color(0xE2E8F0);

    public static final Color BORDER = new Color(0xE4E7EC);
    public static final Color BORDER_STRONG = new Color(0xCBD5E1);

    public static final Color PRIMARY = new Color(0x4F46E5);
    public static final Color PRIMARY_HOVER = new Color(0x4338CA);
    public static final Color PRIMARY_PRESSED = new Color(0x3730A3);
    public static final Color PRIMARY_SOFT = new Color(0xEEF2FF);

    public static final Color SUCCESS = new Color(0x10B981);
    public static final Color SUCCESS_SOFT = new Color(0xECFDF5);
    public static final Color WARNING = new Color(0xF59E0B);
    public static final Color WARNING_SOFT = new Color(0xFFFBEB);
    public static final Color DANGER = new Color(0xEF4444);
    public static final Color DANGER_SOFT = new Color(0xFEF2F2);

    public static final Color TEXT = new Color(0x0F172A);
    public static final Color TEXT_MUTED = new Color(0x64748B);
    public static final Color TEXT_SUBTLE = new Color(0x94A3B8);

    public static final Color SHADOW = new Color(15, 23, 42, 18);

    public static final Font DISPLAY = font(28, Font.BOLD);
    public static final Font H1 = font(22, Font.BOLD);
    public static final Font H2 = font(16, Font.BOLD);
    public static final Font H3 = font(13, Font.BOLD);
    public static final Font BODY = font(13, Font.PLAIN);
    public static final Font BODY_BOLD = font(13, Font.BOLD);
    public static final Font SMALL = font(11, Font.PLAIN);
    public static final Font SMALL_BOLD = font(11, Font.BOLD);
    public static final Font METRIC = font(34, Font.BOLD);
    public static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    public static final int RADIUS_CARD = 14;
    public static final int RADIUS_BUTTON = 10;
    public static final int RADIUS_INPUT = 9;
    public static final int SHADOW_SIZE = 10;

    private Theme() {
    }

    private static Font font(int size, int style) {
        String[] candidates = {
                "Inter", "SF Pro Text", "Segoe UI", "Ubuntu", "Roboto",
                "Helvetica Neue", "DejaVu Sans", "Arial"
        };
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String name : candidates) {
            if (available.contains(name)) {
                return new Font(name, style, size);
            }
        }
        return new Font(Font.SANS_SERIF, style, size);
    }
}
