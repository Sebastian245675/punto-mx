/*
 * Copyright (C) 2022 KriolOS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.openbravo.pos.printer.screen;

import com.openbravo.pos.printer.ticket.BasicTicket;
import java.awt.*;
import java.util.Map;

class JTicket extends javax.swing.JPanel {

    private static final int H_GAP = 8;
    private static final int V_GAP = 8;
    private final int columns;
    private final int paperWidth;
    private final int screenFontSize;
    private final int linewidth;

    private final BasicTicket basict;
    private final Map desktophints;
    private boolean cancelled = false;

    public JTicket(BasicTicket t, int columns, int paperWidth, int screenFontSize) {
        this.columns = columns;
        this.paperWidth = paperWidth;
        this.screenFontSize = screenFontSize;
        this.linewidth = (int) Math.round(paperWidth * 1.33); // Points to pixels on screen
        basict = t;
        desktophints = (Map) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        initComponents();
    }

    public JTicket(BasicTicket t) {
        this(t, 46, 262, 12);
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        paintBorder(g);

        Graphics2D g2d = (Graphics2D) g;

        if (desktophints != null) {
            g2d.addRenderingHints(desktophints);
        }

        Insets i = getInsets();
        g2d.setPaint(new GradientPaint(
                getWidth() - i.left - i.right - 100,
                getHeight() - i.top - i.bottom - 100,
                getBackground(),
                getWidth() - i.left - i.right,
                getHeight() - i.top - i.bottom,
                new Color(0xf0f0f0), true));
        g2d.fillRect(i.left, i.top,
                getWidth() - i.left - i.right,
                getHeight() - i.top - i.bottom);

        g.setColor(getForeground());

        // Render ticket content with proper horizontal scaling like real thermal printers do
        Graphics2D g2dScaled = (Graphics2D) g2d.create();
        try {
            double physicalWidth = paperWidth * 1.33;
            // IMPORTANTE: Usamos un tamaño de fuente base fijo de 12 para que al cambiar el tamaño de fuente en la
            // configuración, la escala horizontal no anule el cambio y la fuente realmente se dibuje más grande/ancha.
            double expectedTextWidth = columns * screenFontSize * 0.6;
            double scaleX = physicalWidth / expectedTextWidth;
            if (scaleX < 0.5) scaleX = 0.5;
            if (scaleX > 3.0) scaleX = 3.0;

            g2dScaled.translate(i.left + H_GAP, i.top + V_GAP);
            g2dScaled.scale(scaleX, 1.0);
            int logicalWidth = (int) (physicalWidth / scaleX);
            basict.draw(g2dScaled, 0, 0, logicalWidth);
        } finally {
            g2dScaled.dispose();
        }

        // Dibujar sello de CANCELADO si aplica
        if (cancelled) {
            drawCancelledStamp(g2d);
        }
    }

    /**
     * Dibuja un sello de "CANCELADO" en diagonal sobre el ticket,
     * al estilo de un sello de goma rojo.
     */
    private void drawCancelledStamp(Graphics2D g2d) {
        Graphics2D stamp = (Graphics2D) g2d.create();
        try {
            stamp.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            stamp.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            // Rotar -35 grados centrado en el componente
            stamp.translate(cx, cy);
            stamp.rotate(Math.toRadians(-30));

            String text = "CANCELADO";
            java.awt.Font stampFont = new java.awt.Font("Arial", java.awt.Font.BOLD, 42);
            stamp.setFont(stampFont);
            java.awt.FontMetrics fm = stamp.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getAscent();

            // Rectángulo del sello
            int padding = 8;
            int rectX = -textWidth / 2 - padding;
            int rectY = -textHeight - padding;
            int rectW = textWidth + padding * 2;
            int rectH = textHeight + padding * 2 + fm.getDescent();

            // Relleno semi-transparente del rectángulo
            stamp.setColor(new Color(220, 0, 0, 40));
            stamp.fillRoundRect(rectX, rectY, rectW, rectH, 12, 12);

            // Borde del sello
            stamp.setColor(new Color(200, 0, 0, 180));
            stamp.setStroke(new java.awt.BasicStroke(3.5f));
            stamp.drawRoundRect(rectX, rectY, rectW, rectH, 12, 12);

            // Doble borde (efecto sello de goma)
            stamp.setStroke(new java.awt.BasicStroke(1.5f));
            stamp.drawRoundRect(rectX + 4, rectY + 4, rectW - 8, rectH - 8, 8, 8);

            // Texto
            stamp.setColor(new Color(200, 0, 0, 200));
            stamp.drawString(text, -textWidth / 2, 0);

        } finally {
            stamp.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Insets ins = getInsets();
        return new Dimension(
                linewidth + (2 * H_GAP) + ins.left + ins.right,
                basict.getHeight() + (2 * V_GAP) + ins.top + ins.bottom);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setBackground(new java.awt.Color(255, 255, 255));
        setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

}
