/*
 * ModernLookAndFeel.java
 * 
 * Utilidad para aplicar estilos modernos a la aplicación
 */
package com.openbravo.pos.util;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import com.formdev.flatlaf.FlatLightLaf;

/**
 * Clase para aplicar estilos modernos a la aplicación
 */
public class ModernLookAndFeel {
    
    /**
     * Aplica un Look and Feel moderno a la aplicación
     */
    public static void aplicarEstiloModerno() {
        try {
            // Preferir FlatLaf (si está disponible) porque ofrece un aspecto moderno y consistente
            try {
                FlatLightLaf.setup();
                aplicarPropiedadesModernas();
                aplicarFuenteGlobal(new Font("Segoe UI", Font.PLAIN, 13));
                return;
            } catch (Throwable t) {
                // FlatLaf no disponible o fallo al arrancar: intentar Nimbus como fallback
            }

            // Buscar y aplicar Nimbus si FlatLaf no está disponible
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());

                    // Personalizaciones adicionales para Nimbus
                    personalizarNimbus();

                    // Aplicar propiedades globales modernas
                    aplicarPropiedadesModernas();
                    aplicarFuenteGlobal(new Font("Segoe UI", Font.PLAIN, 13));
                    return; // Éxito, salir
                }
            }

            // Si no se encuentra Nimbus, aplicar propiedades básicas al LAF actual
            aplicarPropiedadesModernas();
            aplicarFuenteGlobal(new Font("Segoe UI", Font.PLAIN, 13));
            
        } catch (Exception e) {
            System.err.println("Error aplicando Look and Feel moderno: " + e.getMessage());
        }
    }

    /**
     * Aplica un tema con color primario y fuente base personalizados en tiempo de ejecución.
     */
    public static void aplicarTemaPersonalizado(Color primary, Font baseFont) {
        if (primary != null) {
            UIManager.put("nimbusBase", primary);
            UIManager.put("Button.background", primary);
            UIManager.put("nimbusSelectionBackground", primary);
        }
        if (baseFont != null) {
            aplicarFuenteGlobal(baseFont);
        }
        aplicarPropiedadesModernas();
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
     * Configura los botones de JOptionPane en español
     */
    private static void configurarBotonesEspanol() {
        // Configurar botones de JOptionPane en español
        UIManager.put("OptionPane.yesButtonText", "Sí");
        UIManager.put("OptionPane.noButtonText", "No");
        UIManager.put("OptionPane.cancelButtonText", "Cancelar");
        UIManager.put("OptionPane.okButtonText", "Aceptar");
    }
    
    /**
     * Aplica propiedades modernas globales
     */
    private static void aplicarPropiedadesModernas() {
        // Configurar botones en español
        configurarBotonesEspanol();
        
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

        // Bordes redondeados y apariencia general para FlatLaf/Nimbus
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ProgressBar.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(8, 8, 8, 8));

        // Mejoras adicionales para tablas, menús y barras de herramientas
        UIManager.put("Table.rowHeight", 28);
        UIManager.put("Table.showGrid", Boolean.FALSE);
        UIManager.put("Table.selectionBackground", new Color(63, 81, 181));
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("TableHeader.background", new Color(245, 245, 245));
        UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 12));

        UIManager.put("ToolBar.background", new Color(250, 250, 250));
        UIManager.put("Menu.background", new Color(250, 250, 250));
        UIManager.put("Menu.selectionBackground", new Color(230, 230, 230));

        // Mejorar aspecto de popups y tooltips
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(new Color(200, 200, 200)));
        UIManager.put("ScrollPane.viewportBorder", BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    /**
     * Aplica una fuente por defecto a todos los componentes Swing.
     */
    private static void aplicarFuenteGlobal(Font font) {
        if (font == null) return;
        UIDefaults defaults = UIManager.getDefaults();
        for (Object key : defaults.keySet()) {
            if (key != null && key.toString().toLowerCase().contains("font")) {
                try {
                    UIManager.put(key, font);
                } catch (Exception e) {
                    // ignorar claves que no aceptan Font
                }
            }
        }

        // Force some common keys as well
        UIManager.put("defaultFont", font);
        UIManager.put("Button.font", font.deriveFont(Font.BOLD, 13f));
        UIManager.put("Label.font", font.deriveFont(12f));
        UIManager.put("TextField.font", font.deriveFont(13f));
        UIManager.put("TextArea.font", font.deriveFont(13f));
        UIManager.put("Table.font", font.deriveFont(12f));
        UIManager.put("TableHeader.font", font.deriveFont(Font.BOLD, 12f));

        // Improve option panes and dialogs
        UIManager.put("OptionPane.messageFont", font.deriveFont(13f));
        UIManager.put("OptionPane.buttonFont", font.deriveFont(Font.BOLD, 12f));
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
     * Public helper to apply modern styles recursively to any container.
     * Useful for modules that want to restyle existing UI components at runtime.
     */
    public static void estilizarComponentes(Container container) {
        if (container == null) return;
        aplicarEstilosRecursivamente(container);
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