package ar.edu.uade.logistica.gui.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Segmentos de navegacion estilo pill (reemplazo de JTabbedPane). Boton activo en primario,
 * inactivos con hover suave. Contenido cambia via CardLayout.
 */
public class PillTabs extends JPanel {

    private final List<TabButton> buttons = new ArrayList<>();
    private final List<String> keys = new ArrayList<>();
    private final CardLayout cards = new CardLayout();
    private final JPanel cardHost = new JPanel(cards);
    private final JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private int activeIndex = -1;

    public PillTabs() {
        super(new BorderLayout(0, 16));
        setOpaque(false);
        tabBar.setOpaque(false);
        cardHost.setOpaque(false);
        add(tabBar, BorderLayout.NORTH);
        add(cardHost, BorderLayout.CENTER);
    }

    public void addTab(String title, JComponent page) {
        String key = title + "#" + keys.size();
        keys.add(key);
        cardHost.add(page, key);

        final int index = buttons.size();
        TabButton btn = new TabButton(title);
        btn.addActionListener(e -> setActive(index));
        buttons.add(btn);
        tabBar.add(btn);

        if (activeIndex == -1) {
            setActive(0);
        }
    }

    public void setActive(int index) {
        if (index < 0 || index >= buttons.size()) {
            return;
        }
        activeIndex = index;
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setActive(i == index);
        }
        cards.show(cardHost, keys.get(index));
    }

    private static class TabButton extends JButton {
        private boolean active;
        private boolean hover;

        TabButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(Theme.BODY_BOLD);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
            setForeground(Theme.TEXT_MUTED);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            setForeground(active ? Color.WHITE : Theme.TEXT_MUTED);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int radius = h;

            Color bg;
            if (active) {
                bg = Theme.PRIMARY;
            } else if (hover) {
                bg = Theme.SURFACE_ALT;
            } else {
                bg = new Color(0, 0, 0, 0);
            }
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, radius, radius));
            g2.dispose();

            setForeground(active ? Color.WHITE : hover ? Theme.TEXT : Theme.TEXT_MUTED);
            super.paintComponent(g);
        }
    }
}
