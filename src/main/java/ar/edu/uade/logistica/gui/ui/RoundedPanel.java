package ar.edu.uade.logistica.gui.ui;

import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Panel con fondo redondeado, borde opcional y sombra suave. Pintado manualmente para no
 * depender de la look-and-feel del sistema. Los componentes hijos se pintan encima normal.
 */
public class RoundedPanel extends JPanel {

    private final int radius;
    private final int shadow;
    private Color fill = Theme.SURFACE;
    private Color border = Theme.BORDER;
    private boolean drawBorder = true;

    public RoundedPanel() {
        this(Theme.RADIUS_CARD, Theme.SHADOW_SIZE);
    }

    public RoundedPanel(int radius, int shadow) {
        this.radius = radius;
        this.shadow = shadow;
        setOpaque(false);
    }

    public RoundedPanel(LayoutManager layout) {
        this();
        setLayout(layout);
    }

    public RoundedPanel(LayoutManager layout, int radius, int shadow) {
        this(radius, shadow);
        setLayout(layout);
    }

    public void setFillColor(Color c) {
        this.fill = c;
        repaint();
    }

    public void setBorderColor(Color c) {
        this.border = c;
        repaint();
    }

    public void setDrawBorder(boolean draw) {
        this.drawBorder = draw;
        repaint();
    }

    public int getShadowInset() {
        return shadow;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (shadow > 0) {
            for (int i = 0; i < shadow; i++) {
                float alpha = 0.045f * (shadow - i) / shadow;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(Color.BLACK);
                int pad = i;
                int offY = (shadow - i);
                g2.fill(new RoundRectangle2D.Float(
                        pad, pad + offY,
                        w - pad * 2 - 1, h - pad * 2 - 1 - offY,
                        radius + i, radius + i));
            }
            g2.setComposite(AlphaComposite.SrcOver);
        }

        int inset = shadow;
        int cw = w - inset * 2;
        int ch = h - inset * 2;

        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Float(inset, inset, cw - 1, ch - 1, radius, radius));

        if (drawBorder && border != null) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(inset + 0.5f, inset + 0.5f, cw - 2, ch - 2, radius, radius));
        }

        g2.dispose();
    }
}
