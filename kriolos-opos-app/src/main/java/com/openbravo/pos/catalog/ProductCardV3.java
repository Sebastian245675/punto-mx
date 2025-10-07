package com.openbravo.pos.catalog;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * Tarjeta de producto moderna y elegante con efectos visuales mejorados
 * @author Sebastian - Versión V3 Ultra Moderna
 */
public class ProductCardV3 extends JPanel {
    
    private static final long serialVersionUID = 1L;
    
    // Colores modernos y profesionales (Material Design 3.0)
    private static final Color BACKGROUND_COLOR = new Color(255, 255, 255);
    private static final Color HOVER_COLOR = new Color(248, 250, 252);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color HOVER_BORDER_COLOR = new Color(59, 130, 246);
    private static final Color TEXT_COLOR = new Color(31, 41, 55);
    private static final Color PRICE_COLOR = new Color(5, 150, 105);
    private static final Color ACCENT_COLOR = new Color(249, 115, 22);
    
    // Dimensiones optimizadas para mejor visual
    private static final int CARD_WIDTH = 180;
    private static final int CARD_HEIGHT = 220;
    private static final int IMAGE_SIZE = 100;
    private static final int BORDER_RADIUS = 18;
    
    private final CatalogItem catalogItem;
    private boolean isHovered = false;
    private boolean isPressed = false;
    
    public ProductCardV3(CatalogItem item, ActionListener actionListener) {
        this.catalogItem = item;
        
        initializeCard();
        setupInteractions();
        setupClickAction(actionListener);
    }
    
    private void initializeCard() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        setMinimumSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        setMaximumSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        
        setBackground(BACKGROUND_COLOR);
        setBorder(createDefaultBorder());
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Crear contenido de la tarjeta
        JPanel mainPanel = createMainPanel();
        add(mainPanel, BorderLayout.CENTER);
        
        // Tooltip mejorado con HTML styling
        if (catalogItem.getTextTip() != null && !catalogItem.getTextTip().isEmpty()) {
            setToolTipText(createStyledTooltip(catalogItem.getTextTip()));
        }
    }
    
    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(18, 18, 18, 18));
        
        // Área de imagen con marco elegante
        JPanel imageContainer = createImageContainer();
        imageContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(imageContainer);
        
        // Espaciado
        mainPanel.add(Box.createVerticalStrut(14));
        
        // Nombre del producto con tipografía mejorada
        JLabel nameLabel = createNameLabel();
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(nameLabel);
        
        // Precio con diseño destacado (si existe)
        if (catalogItem.getPrice() != null && !catalogItem.getPrice().isEmpty()) {
            mainPanel.add(Box.createVerticalStrut(8));
            JPanel pricePanel = createPricePanel();
            pricePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            mainPanel.add(pricePanel);
        }
        
        return mainPanel;
    }
    
    private JPanel createImageContainer() {
        JPanel imageContainer = new JPanel();
        imageContainer.setLayout(new BorderLayout());
        imageContainer.setOpaque(false);
        imageContainer.setPreferredSize(new Dimension(IMAGE_SIZE + 20, IMAGE_SIZE + 20));
        
        // Marco circular para la imagen
        JPanel imageFrame = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Sombra del marco
                g2d.setColor(new Color(0, 0, 0, 15));
                g2d.fillOval(3, 3, IMAGE_SIZE + 14, IMAGE_SIZE + 14);
                
                // Marco de la imagen
                g2d.setColor(new Color(241, 245, 249));
                g2d.fillOval(0, 0, IMAGE_SIZE + 20, IMAGE_SIZE + 20);
                
                g2d.dispose();
            }
        };
        imageFrame.setLayout(new BorderLayout());
        imageFrame.setOpaque(false);
        imageFrame.setPreferredSize(new Dimension(IMAGE_SIZE + 20, IMAGE_SIZE + 20));
        
        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        
        if (catalogItem.getImage() != null) {
            // Imagen redimensionada con alta calidad
            Image scaledImage = catalogItem.getImage().getScaledInstance(
                IMAGE_SIZE, IMAGE_SIZE, Image.SCALE_SMOOTH);
            ImageIcon imageIcon = new ImageIcon(scaledImage);
            imageLabel.setIcon(imageIcon);
        } else {
            // Ícono por defecto elegante
            imageLabel.setText("📦");
            imageLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
            imageLabel.setForeground(new Color(156, 163, 175));
        }
        
        imageFrame.add(imageLabel, BorderLayout.CENTER);
        imageContainer.add(imageFrame, BorderLayout.CENTER);
        
        return imageContainer;
    }
    
    private JLabel createNameLabel() {
        String displayText = catalogItem.getText();
        if (displayText.length() > 28) {
            displayText = displayText.substring(0, 25) + "...";
        }
        
        JLabel nameLabel = new JLabel();
        nameLabel.setText("<html><div style='text-align: center; line-height: 1.4; font-weight: 600; color: #1F2937;'>" + 
                         displayText + "</div></html>");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setForeground(TEXT_COLOR);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        return nameLabel;
    }
    
    private JPanel createPricePanel() {
        JPanel pricePanel = new JPanel();
        pricePanel.setLayout(new BorderLayout());
        pricePanel.setOpaque(true);
        pricePanel.setBackground(new Color(236, 253, 245));
        pricePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(167, 243, 208), 1),
            new EmptyBorder(6, 12, 6, 12)
        ));
        
        JLabel priceLabel = new JLabel(catalogItem.getPrice());
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        priceLabel.setForeground(PRICE_COLOR);
        priceLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        pricePanel.add(priceLabel, BorderLayout.CENTER);
        pricePanel.setMaximumSize(pricePanel.getPreferredSize());
        
        return pricePanel;
    }
    
    private String createStyledTooltip(String text) {
        return "<html><div style='padding: 10px; font-family: Segoe UI; font-size: 12px; " +
               "background-color: #F9FAFB; border: 1px solid #E5E7EB; border-radius: 8px;'>" + 
               text + "</div></html>";
    }
    
    private Border createDefaultBorder() {
        return BorderFactory.createLineBorder(BORDER_COLOR, 1);
    }
    
    private Border createHoverBorder() {
        return BorderFactory.createLineBorder(HOVER_BORDER_COLOR, 2);
    }
    
    private void setupInteractions() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                updateCardAppearance();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                isPressed = false;
                updateCardAppearance();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                updateCardAppearance();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                updateCardAppearance();
            }
        });
    }
    
    private void updateCardAppearance() {
        if (isPressed) {
            setBackground(new Color(239, 246, 255));
            setBorder(createHoverBorder());
        } else if (isHovered) {
            setBackground(HOVER_COLOR);
            setBorder(createHoverBorder());
        } else {
            setBackground(BACKGROUND_COLOR);
            setBorder(createDefaultBorder());
        }
        repaint();
    }
    
    private void setupClickAction(ActionListener actionListener) {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (actionListener != null) {
                    // Animación suave de click
                    Timer animationTimer = new Timer(200, evt -> {
                        updateCardAppearance();
                        ((Timer)evt.getSource()).stop();
                    });
                    animationTimer.start();
                    
                    actionListener.actionPerformed(null);
                }
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Sombra moderna y elegante
        if (isHovered || isPressed) {
            g2d.setColor(new Color(0, 0, 0, 25));
            g2d.fillRoundRect(4, 4, getWidth() - 4, getHeight() - 4, BORDER_RADIUS, BORDER_RADIUS);
        } else {
            g2d.setColor(new Color(0, 0, 0, 10));
            g2d.fillRoundRect(2, 2, getWidth() - 2, getHeight() - 2, BORDER_RADIUS, BORDER_RADIUS);
        }
        
        // Fondo con bordes redondeados
        g2d.setColor(getBackground());
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), BORDER_RADIUS, BORDER_RADIUS);
        
        // Efecto de brillo en hover
        if (isHovered) {
            GradientPaint gradient = new GradientPaint(
                0, 0, new Color(255, 255, 255, 40),
                0, getHeight() / 2, new Color(59, 130, 246, 20)
            );
            g2d.setPaint(gradient);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), BORDER_RADIUS, BORDER_RADIUS);
        }
        
        // Borde destacado en la parte superior cuando está seleccionado
        if (isPressed) {
            g2d.setColor(ACCENT_COLOR);
            g2d.fillRoundRect(0, 0, getWidth(), 4, BORDER_RADIUS, BORDER_RADIUS);
        }
        
        g2d.dispose();
    }
}