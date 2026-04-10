package ar.edu.uade.logistica.gui.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Boton plano con esquinas redondeadas, hover y press animados. Tres variantes de estilo.
 */
public class ModernButton extends JButton {

    public enum Variant { PRIMARY, SECONDARY, GHOST, DANGER }

    private final Variant variant;
    private boolean hover;
    private boolean pressed;

    public ModernButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFont(Theme.BODY_BOLD);
        setForeground(textColor());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(11, 18, 11, 18));
        setHorizontalAlignment(CENTER);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; pressed = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
        });
    }

    private Color backgroundColor() {
        return switch (variant) {
            case PRIMARY -> pressed ? Theme.PRIMARY_PRESSED : hover ? Theme.PRIMARY_HOVER : Theme.PRIMARY;
            case SECONDARY -> pressed ? Theme.SURFACE_HOVER : hover ? Theme.SURFACE_ALT : Theme.SURFACE;
            case GHOST -> pressed ? new Color(15, 23, 42, 24)
                    : hover ? new Color(15, 23, 42, 14)
                    : new Color(0, 0, 0, 0);
            case DANGER -> pressed ? new Color(0xB91C1C)
                    : hover ? new Color(0xDC2626)
                    : Theme.DANGER;
        };
    }

    private Color textColor() {
        return switch (variant) {
            case PRIMARY, DANGER -> Color.WHITE;
            case SECONDARY -> Theme.TEXT;
            case GHOST -> Theme.TEXT_MUTED;
        };
    }

    private Color borderColor() {
        return switch (variant) {
            case SECONDARY -> Theme.BORDER;
            default -> null;
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int radius = Theme.RADIUS_BUTTON;

        g2.setColor(backgroundColor());
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, radius, radius));

        Color b = borderColor();
        if (b != null) {
            g2.setColor(b);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, radius, radius));
        }
        g2.dispose();

        setForeground(textColor());
        super.paintComponent(g);
    }
}
