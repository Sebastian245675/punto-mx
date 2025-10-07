package com.openbravo.pos.catalog;

import java.awt.*;
import javax.swing.*;

/**
 * Panel de flujo personalizado para productos con mejor distribución
 */
public class JFlowPanel extends JPanel {
    
    private static final long serialVersionUID = 1L;
    
    private static final int DEFAULT_HGAP = 20;
    private static final int DEFAULT_VGAP = 20;
    
    public JFlowPanel() {
        super();
        // Usar GridBagLayout para mejor control
        setLayout(new ModernFlowLayout(FlowLayout.LEFT, DEFAULT_HGAP, DEFAULT_VGAP));
        setOpaque(true);
        setBackground(new Color(248, 250, 252));
    }
    
    /**
     * Layout personalizado que distribuye mejor los elementos
     */
    private static class ModernFlowLayout extends FlowLayout {
        
        public ModernFlowLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }
        
        @Override
        public void layoutContainer(Container target) {
            synchronized (target.getTreeLock()) {
                Insets insets = target.getInsets();
                int maxWidth = target.getWidth() - (insets.left + insets.right + getHgap() * 2);
                int nmembers = target.getComponentCount();
                int x = insets.left + getHgap();
                int y = insets.top + getVgap();
                int rowh = 0;
                int start = 0;
                
                boolean ltr = target.getComponentOrientation().isLeftToRight();
                
                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = m.getPreferredSize();
                        m.setSize(d.width, d.height);
                        
                        if ((x == insets.left + getHgap()) || ((x + d.width) <= maxWidth)) {
                            if (x > insets.left + getHgap()) {
                                x += getHgap();
                            }
                            x += d.width;
                            rowh = Math.max(rowh, d.height);
                        } else {
                            rowh = moveComponents(target, insets.left + getHgap(), y, 
                                                maxWidth - x, rowh, start, i, ltr);
                            y += getVgap() + rowh;
                            rowh = d.height;
                            x = insets.left + getHgap() + d.width;
                            start = i;
                        }
                    }
                }
                moveComponents(target, insets.left + getHgap(), y, maxWidth - x, rowh, start, nmembers, ltr);
            }
        }
        
        private int moveComponents(Container target, int x, int y, int width, int height,
                                 int rowStart, int rowEnd, boolean ltr) {
            switch (getAlignment()) {
                case LEFT:
                    x += ltr ? 0 : width;
                    break;
                case CENTER:
                    x += width / 2;
                    break;
                case RIGHT:
                    x += ltr ? width : 0;
                    break;
                case LEADING:
                    break;
                case TRAILING:
                    x += width;
                    break;
            }
            
            for (int i = rowStart; i < rowEnd; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    if (ltr) {
                        m.setLocation(x, y + (height - m.getHeight()) / 2);
                    } else {
                        m.setLocation(target.getWidth() - x - m.getWidth(), 
                                    y + (height - m.getHeight()) / 2);
                    }
                    x += m.getWidth() + getHgap();
                }
            }
            return height;
        }
    }
}