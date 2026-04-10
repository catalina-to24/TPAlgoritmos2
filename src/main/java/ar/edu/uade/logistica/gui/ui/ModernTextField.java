package ar.edu.uade.logistica.gui.ui;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Text field redondeado con placeholder y focus ring. Usa pintado manual del fondo y
 * deja que el text field base renderice texto/caret normal encima.
 */
public class ModernTextField extends JTextField {

    private String placeholder = "";

    public ModernTextField() {
        this("");
    }

    public ModernTextField(String placeholder) {
        super();
        this.placeholder = placeholder;
        setOpaque(false);
        setFont(Theme.BODY);
        setForeground(Theme.TEXT);
        setCaretColor(Theme.PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        setColumns(14);

        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e) { repaint(); }
        });
        getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { repaint(); }
            @Override public void removeUpdate(DocumentEvent e) { repaint(); }
            @Override public void changedUpdate(DocumentEvent e) { repaint(); }
        });
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int radius = Theme.RADIUS_INPUT;

        if (hasFocus()) {
            g2.setColor(new Color(Theme.PRIMARY.getRed(), Theme.PRIMARY.getGreen(), Theme.PRIMARY.getBlue(), 36));
            g2.fill(new RoundRectangle2D.Float(-2, -2, w + 3, h + 3, radius + 4, radius + 4));
        }

        g2.setColor(Theme.SURFACE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, radius, radius));

        g2.setColor(hasFocus() ? Theme.PRIMARY : Theme.BORDER);
        g2.setStroke(new BasicStroke(hasFocus() ? 1.6f : 1f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 2, radius, radius));

        g2.dispose();

        super.paintComponent(g);

        if (getText().isEmpty() && !placeholder.isEmpty() && !hasFocus()) {
            Graphics2D g3 = (Graphics2D) g.create();
            g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g3.setFont(getFont());
            g3.setColor(Theme.TEXT_SUBTLE);
            FontMetrics fm = g3.getFontMetrics();
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g3.drawString(placeholder, 14, y);
            g3.dispose();
        }
    }
}
