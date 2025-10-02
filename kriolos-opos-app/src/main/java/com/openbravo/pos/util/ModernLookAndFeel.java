/*
 * ModernLookAndFeel.java
 * 
 * Utilidad para aplicar estilos modernos a la aplicación
 */
package com.openbravo.pos.util;

import javax.swing.*;
import java.awt.*;

/**
 * Clase para aplicar estilos modernos a la aplicación
 */
public class ModernLookAndFeel {
    
    /**
     * Aplica un Look and Feel moderno a la aplicación
     */
    public static void aplicarEstiloModerno() {
        try {
            // Buscar y aplicar Nimbus
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    
                    // Personalizaciones adicionales para Nimbus
                    personalizarNimbus();
                    
                    // Aplicar propiedades globales modernas
                    aplicarPropiedadesModernas();
                    
                    return; // Éxito, salir
                }
            }
            
            // Si no se encuentra Nimbus, aplicar propiedades básicas al LAF actual
            aplicarPropiedadesModernas();
            
        } catch (Exception e) {
            System.err.println("Error aplicando Look and Feel moderno: " + e.getMessage());
        }
    }
    
    /**
     * Personaliza el tema Nimbus con colores modernos
     */
    private static void personalizarNimbus() {
        // Colores modernos inspirados en Material Design
        UIManager.put("control", new Color(250, 250, 250));                    // Fondo general
        UIManager.put("nimbusBase", new Color(51, 98, 140));                  // Azul principal
        UIManager.put("nimbusBlueGrey", new Color(169, 184, 196));            // Gris azulado
        UIManager.put("nimbusFocus", new Color(63, 81, 181));                 // Color de foco (Material Indigo)
        UIManager.put("nimbusSelectedText", Color.WHITE);                      // Texto seleccionado
        UIManager.put("nimbusSelectionBackground", new Color(63, 81, 181));   // Fondo de selección
        
        // Botones más modernos
        UIManager.put("Button.background", new Color(33, 150, 243));          // Azul Material
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button[Default].backgroundPainter", new Color(33, 150, 243));
        
        // Campos de texto más limpios
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.border", BorderFactory.createLineBorder(new Color(224, 224, 224), 1));
        
        // Paneles más modernos
        UIManager.put("Panel.background", new Color(250, 250, 250));
        
        // Tablas más elegantes
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.alternateRowColor", new Color(248, 248, 248));
        UIManager.put("Table.gridColor", new Color(224, 224, 224));
    }
    
    /**
     * Aplica propiedades modernas globales
     */
    private static void aplicarPropiedadesModernas() {
        // Habilitar anti-aliasing para texto más suave
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        // Renderizado de texto más suave
        System.setProperty("swing.plaf.metal.controlFont", "Arial");
        System.setProperty("swing.plaf.metal.userFont", "Arial");
        
        // Mejores transiciones y animaciones
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        
        // Bordes más modernos para todos los componentes
        UIManager.put("TitledBorder.font", new Font("Arial", Font.BOLD, 12));
        UIManager.put("TitledBorder.titleColor", new Color(66, 66, 66));
        
        // Tooltips más elegantes
        UIManager.put("ToolTip.background", new Color(97, 97, 97));
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(new Color(128, 128, 128), 1));
        
        // Scrollbars más modernos
        UIManager.put("ScrollBar.background", new Color(240, 240, 240));
        UIManager.put("ScrollBar.thumb", new Color(180, 180, 180));
        UIManager.put("ScrollBar.track", new Color(245, 245, 245));
    }
    
    /**
     * Aplica estilo moderno a un JDialog específico
     */
    public static void aplicarEstiloModernoADialogo(JDialog dialog) {
        if (dialog == null) return;
        
        // Fondo moderno
        dialog.getContentPane().setBackground(new Color(250, 250, 250));
        
        // Aplicar estilos a todos los componentes del diálogo
        aplicarEstilosRecursivamente(dialog.getContentPane());
        
        // Sombra moderna (si es posible en el sistema)
        try {
            dialog.getRootPane().setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
        } catch (Exception e) {
            // Ignorar si no se puede aplicar
        }
    }
    
    /**
     * Aplica estilos modernos recursivamente a todos los componentes
     */
    private static void aplicarEstilosRecursivamente(Container container) {
        for (Component comp : container.getComponents()) {
            
            if (comp instanceof JButton) {
                aplicarEstiloBotonModerno((JButton) comp);
            }
            else if (comp instanceof JTextField) {
                aplicarEstiloCampoModerno((JTextField) comp);
            }
            else if (comp instanceof JLabel) {
                aplicarEstiloEtiquetaModerna((JLabel) comp);
            }
            else if (comp instanceof JPanel) {
                aplicarEstiloPanelModerno((JPanel) comp);
            }
            else if (comp instanceof JToggleButton) {
                aplicarEstiloToggleModerno((JToggleButton) comp);
            }
            
            // Aplicar recursivamente a contenedores
            if (comp instanceof Container) {
                aplicarEstilosRecursivamente((Container) comp);
            }
        }
    }
    
    /**
     * Aplica estilo moderno a un botón
     */
    private static void aplicarEstiloBotonModerno(JButton button) {
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBackground(new Color(33, 150, 243));  // Material Blue
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Bordes redondeados (efecto visual)
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(30, 136, 220), 1),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        
        // Efectos hover si es posible
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            Color colorOriginal = button.getBackground();
            
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(colorOriginal.brighter());
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(colorOriginal);
            }
        });
    }
    
    /**
     * Aplica estilo moderno a un campo de texto
     */
    private static void aplicarEstiloCampoModerno(JTextField field) {
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(224, 224, 224), 2),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setBackground(Color.WHITE);
        
        // Efecto focus
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(63, 81, 181), 2),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
            
            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(224, 224, 224), 2),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
    }
    
    /**
     * Aplica estilo moderno a una etiqueta
     */
    private static void aplicarEstiloEtiquetaModerna(JLabel label) {
        label.setFont(new Font("Arial", Font.PLAIN, 12));
        label.setForeground(new Color(66, 66, 66));
    }
    
    /**
     * Aplica estilo moderno a un panel
     */
    private static void aplicarEstiloPanelModerno(JPanel panel) {
        panel.setBackground(new Color(250, 250, 250));
        
        // Si tiene borde de título, modernizarlo
        if (panel.getBorder() instanceof javax.swing.border.TitledBorder) {
            javax.swing.border.TitledBorder titleBorder = 
                (javax.swing.border.TitledBorder) panel.getBorder();
            titleBorder.setTitleFont(new Font("Arial", Font.BOLD, 12));
            titleBorder.setTitleColor(new Color(66, 66, 66));
        }
    }
    
    /**
     * Aplica estilo moderno a un toggle button
     */
    private static void aplicarEstiloToggleModerno(JToggleButton toggle) {
        toggle.setFocusPainted(false);
        toggle.setBorderPainted(false);
        toggle.setFont(new Font("Arial", Font.BOLD, 11));
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        Color colorNormal = new Color(224, 224, 224);
        Color colorSeleccionado = new Color(76, 175, 80); // Material Green
        
        toggle.setBackground(toggle.isSelected() ? colorSeleccionado : colorNormal);
        toggle.setForeground(toggle.isSelected() ? Color.WHITE : new Color(66, 66, 66));
        
        // Cambiar colores al seleccionar/deseleccionar
        toggle.addActionListener(e -> {
            toggle.setBackground(toggle.isSelected() ? colorSeleccionado : colorNormal);
            toggle.setForeground(toggle.isSelected() ? Color.WHITE : new Color(66, 66, 66));
        });
    }
}