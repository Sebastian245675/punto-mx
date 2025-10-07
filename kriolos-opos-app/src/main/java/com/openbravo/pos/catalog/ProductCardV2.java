package com.openbravo.pos.catalog;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Tarjeta de producto modernizada con diseño material y efectos visuales
 * @author Sebastian - Versión mejorada
 */
public class ProductCardV2 extends JPanel {

    private static final int PRODUCT_IMAGE_SIZE = 90;
    private static final int CARD_WIDTH = 140;
    private static final int CARD_HEIGHT = 160;

    private final Border normalBorder;
    private final Border hoverBorder;
    private final Border pressedBorder;
    private boolean isHovered = false;

    public ProductCardV2(CatalogItem item, ActionListener listener) {

        // Colores modernos para los bordes
        Color baseBorderColor = new Color(230, 230, 230);
        Color hoverBorderColor = new Color(66, 165, 245); // Azul Material
        Color pressedBorderColor = new Color(33, 150, 243); // Azul más oscuro

        Border baseBorder = BorderFactory.createLineBorder(baseBorderColor, 1);
        Border shadowBorder = BorderFactory.createLineBorder(new Color(0, 0, 0, 20), 1);
        Border emptyBorder = BorderFactory.createEmptyBorder(8, 8, 8, 8);

        normalBorder = BorderFactory.createCompoundBorder(baseBorder, emptyBorder);
        hoverBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(hoverBorderColor, 2), 
            BorderFactory.createEmptyBorder(7, 7, 7, 7)
        );
        pressedBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(pressedBorderColor, 2), 
            BorderFactory.createEmptyBorder(7, 7, 7, 7)
        );

        this.setLayout(new BorderLayout(0, 5));
        this.setBorder(normalBorder);
        this.setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        this.setBackground(Color.WHITE);
        this.putClientProperty("item", item);

        // Efectos de hover y click mejorados
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JPanel sourcePanel = (JPanel) e.getSource();
                listener.actionPerformed(new ActionEvent(sourcePanel, ActionEvent.ACTION_PERFORMED, "product_click"));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                JPanel clickedPanel = (JPanel) e.getSource();
                clickedPanel.setBorder(pressedBorder);
                clickedPanel.setBackground(new Color(245, 245, 245));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                JPanel releasedPanel = (JPanel) e.getSource();
                if (isHovered) {
                    releasedPanel.setBorder(hoverBorder);
                    releasedPanel.setBackground(new Color(250, 250, 250));
                } else {
                    releasedPanel.setBorder(normalBorder);
                    releasedPanel.setBackground(Color.WHITE);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                JPanel hoveredPanel = (JPanel) e.getSource();
                hoveredPanel.setBorder(hoverBorder);
                hoveredPanel.setBackground(new Color(250, 250, 250));
                hoveredPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                JPanel exitedPanel = (JPanel) e.getSource();
                exitedPanel.setBorder(normalBorder);
                exitedPanel.setBackground(Color.WHITE);
                exitedPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        };
        this.addMouseListener(mouseAdapter);

        //Set Card Image con placeholder mejorado
        JPanel imagePanel = new JPanel();
        imagePanel.setOpaque(false);
        imagePanel.setPreferredSize(new Dimension(PRODUCT_IMAGE_SIZE, PRODUCT_IMAGE_SIZE));
        
        if (item.getImage() != null) {
            ImageIcon icon = new ImageIcon(item.getImage().getScaledInstance(
                PRODUCT_IMAGE_SIZE - 10, PRODUCT_IMAGE_SIZE - 10, Image.SCALE_SMOOTH));
            JLabel imageLabel = new JLabel(icon);
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imagePanel.add(imageLabel);
        } else {
            // Placeholder moderno para productos sin imagen
            JLabel placeholderLabel = new JLabel(createModernPlaceholder(item.getText()));
            placeholderLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imagePanel.add(placeholderLabel);
        }

        //Set Card Background Color
        if (item.getColorHex() != null) {
            try {
                Color categoryColor = Color.decode(item.getColorHex());
                this.setBackground(new Color(
                    categoryColor.getRed(), 
                    categoryColor.getGreen(), 
                    categoryColor.getBlue(), 
                    30 // Transparencia para efecto sutil
                ));
            } catch (Exception e) {
                // Color por defecto si hay error
                this.setBackground(Color.WHITE);
            }
        }

        this.setToolTipText(item.getTextTip());
        this.add(imagePanel, BorderLayout.CENTER);

        // Panel de texto modernizado
        JPanel textPanel = new JPanel(new BorderLayout(0, 2));
        textPanel.setOpaque(false);
        textPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Nombre del producto con fuente mejorada
        JLabel nameLabel = new JLabel("<html><center>" + 
            (item.getText().length() > 15 ? 
                item.getText().substring(0, 15) + "..." : 
                item.getText()) + "</center></html>");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        nameLabel.setForeground(new Color(33, 33, 33));
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        textPanel.add(nameLabel, BorderLayout.NORTH);

        // Precio con estilo mejorado
        String priceStr = item.getPrice();
        if (priceStr != null && !priceStr.trim().isEmpty()) {
            JLabel priceLabel = new JLabel(priceStr);
            priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            priceLabel.setForeground(new Color(76, 175, 80)); // Verde material
            priceLabel.setHorizontalAlignment(SwingConstants.CENTER);
            textPanel.add(priceLabel, BorderLayout.SOUTH);
        }

        this.add(textPanel, BorderLayout.SOUTH);
    }

    /**
     * Crea un placeholder moderno para productos sin imagen
     */
    private String createModernPlaceholder(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "📦";
        }
        
        // Usa la primera letra del producto como icono
        String firstLetter = text.substring(0, 1).toUpperCase();
        return "<html><div style='text-align:center; font-size:24px; color:#999;'>" +
               "<div style='background:#f5f5f5; border-radius:50%; padding:10px;'>" +
               firstLetter + "</div></div></html>";
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Efecto de sombra sutil
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (isHovered) {
            // Sombra más pronunciada al hacer hover
            g2d.setColor(new Color(0, 0, 0, 30));
            g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 8, 8);
        } else {
            // Sombra sutil normal
            g2d.setColor(new Color(0, 0, 0, 10));
            g2d.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 6, 6);
        }
        
        g2d.dispose();
    }

    private String createSmallText(String text) {
        return text.substring(0, Math.min(3, text.length()));
    }

    private Image createDefaultImage(int width, int height, String text) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fondo moderno con gradiente
        g2d.setColor(new Color(245, 245, 245));
        g2d.fillRoundRect(0, 0, width, height, 8, 8);
        
        // Texto centrado
        g2d.setColor(new Color(158, 158, 158));
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 18));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        int y = (fm.getAscent() + (height - (fm.getAscent() + fm.getDescent())) / 2);
        g2d.drawString(text, x, y);
        g2d.dispose();
        return img;
    }
}
