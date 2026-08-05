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
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.awt.event.HierarchyEvent;
import java.util.WeakHashMap;

/**
 * Clase para aplicar estilos modernos a la aplicación
 */
public class ModernLookAndFeel {

    private static Font globalFont = new Font("Segoe UI", Font.PLAIN, 24);

    private static final WeakHashMap<Component, Boolean> adjustedComponents = new WeakHashMap<>();
    private static boolean ajustadorFuentesInicializado = false;

    private static synchronized void inicializarAjustadorFuentes() {
        if (ajustadorFuentesInicializado) {
            return;
        }
        ajustadorFuentesInicializado = true;

        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
                @Override
                public void eventDispatched(AWTEvent event) {
                    if (event instanceof HierarchyEvent) {
                        HierarchyEvent he = (HierarchyEvent) event;
                        if ((he.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                            Component comp = he.getComponent();
                            if (comp != null && comp.isShowing()) {
                                ajustarFuentesRecursivo(comp);
                            }
                        }
                    } else if (event instanceof ContainerEvent) {
                        ContainerEvent ce = (ContainerEvent) event;
                        if (ce.getID() == ContainerEvent.COMPONENT_ADDED) {
                            Component child = ce.getChild();
                            if (child != null) {
                                ajustarFuentesRecursivo(child);
                            }
                        }
                    }
                }
            }, AWTEvent.HIERARCHY_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK);
        } catch (Throwable t) {
            System.err.println("Error al registrar AWTEventListener para fuentes: " + t.getMessage());
        }
    }

    public static void registrarComponenteAjustado(Component comp) {
        // No marcamos JComponent como "fontAdjusted" aquí para permitir que el ajustador de fuentes
        // global aplique las escalas y overrides correctos después de la estilzación básica.
        if (comp == null) return;
        if (!(comp instanceof JComponent)) {
            synchronized (adjustedComponents) {
                adjustedComponents.put(comp, Boolean.TRUE);
            }
        }
    }

    public static void ajustarFuentesRecursivo(Component comp) {
        if (comp == null) return;
        ajustarFuenteComponente(comp);
        if (comp instanceof Container) {
            Container container = (Container) comp;
            Component[] children = container.getComponents();
            if (children != null) {
                for (Component child : children) {
                    ajustarFuentesRecursivo(child);
                }
            }
        }
    }

    public static void ajustarFuenteComponente(Component comp) {
        if (comp == null) return;
        
        if (comp instanceof JComponent) {
            JComponent jc = (JComponent) comp;
            if (Boolean.TRUE.equals(jc.getClientProperty("fontAdjusted"))) {
                return;
            }
            jc.putClientProperty("fontAdjusted", Boolean.TRUE);
            
            if (jc.getClientProperty("fontListenerRegistered") == null) {
                jc.putClientProperty("fontListenerRegistered", Boolean.TRUE);
                jc.addPropertyChangeListener("font", evt -> {
                    if (!Boolean.TRUE.equals(jc.getClientProperty("adjustingFont"))) {
                        jc.putClientProperty("fontAdjusted", Boolean.FALSE);
                        ajustarFuenteComponente(jc);
                    }
                });
            }
            
            jc.putClientProperty("adjustingFont", Boolean.TRUE);
            try {
                // Verificar overrides específicos basados en client properties
                if (Boolean.TRUE.equals(jc.getClientProperty("isBottomInfoLabel"))) {
                    Font newFont = new Font("Segoe UI", Font.BOLD, 26);
                    jc.setFont(newFont);
                    return;
                }
                if (Boolean.TRUE.equals(jc.getClientProperty("isActionToolbarButton"))) {
                    Font newFont = new Font("Segoe UI", Font.BOLD, 24);
                    jc.setFont(newFont);
                    return;
                }
                if (Boolean.TRUE.equals(jc.getClientProperty("isMenuButton"))) {
                    Font newFont = new Font("Segoe UI", Font.BOLD, 28);
                    jc.setFont(newFont);
                    return;
                }
                if (Boolean.TRUE.equals(jc.getClientProperty("isPaymentButton"))) {
                    Font newFont = new Font("Segoe UI", Font.BOLD, 30);
                    jc.setFont(newFont);
                    return;
                }
                if (Boolean.TRUE.equals(jc.getClientProperty("isReprintButton"))) {
                    Font newFont = new Font("Segoe UI", Font.BOLD, 24);
                    jc.setFont(newFont);
                    return;
                }
                if (Boolean.TRUE.equals(jc.getClientProperty("isSmallActionButton"))) {
                    int targetSize = 22;
                    if (jc instanceof JButton) {
                        String text = ((JButton)jc).getText();
                        if ("Cambiar".equals(text)) {
                            targetSize = 24;
                        } else if ("F5 - Asignar Cliente".equals(text)) {
                            targetSize = 22;
                        } else if (text != null && text.contains("Ventas") && text.contains("Devoluciones")) {
                            targetSize = 22;
                        }
                    }
                    Font newFont = new Font("Segoe UI", Font.BOLD, targetSize);
                    jc.setFont(newFont);
                    return;
                }

                Font font = jc.getFont();
                if (font != null) {
                    float currentSize = font.getSize2D();
                    float newSize = calcularNuevaEscala(currentSize);
                    Font newFont = font.deriveFont(newSize);
                    jc.setFont(newFont);
                }
            } catch (Throwable t) {
                // Ignorar
            } finally {
                jc.putClientProperty("adjustingFont", Boolean.FALSE);
            }
        } else {
            synchronized (adjustedComponents) {
                if (adjustedComponents.containsKey(comp)) {
                    return;
                }
                adjustedComponents.put(comp, Boolean.TRUE);
            }
            
            try {
                Font font = comp.getFont();
                if (font != null) {
                    float currentSize = font.getSize2D();
                    float newSize = calcularNuevaEscala(currentSize);
                    Font newFont = font.deriveFont(newSize);
                    comp.setFont(newFont);
                }
            } catch (Throwable t) {
                // Ignorar
            }
        }
    }

    private static float calcularNuevaEscala(float currentSize) {
        // Escala agresiva para usuarios con dificultades de visión
        if (currentSize <= 12f) {
            return currentSize + 8f; // 11 -> 19, 12 -> 20
        } else if (currentSize <= 14f) {
            return currentSize + 7f; // 14 -> 21
        } else if (currentSize <= 18f) {
            return currentSize + 6f; // 18 -> 24
        } else if (currentSize < 24f) {
            return currentSize + 5f; // 22 -> 27
        } else {
            return currentSize + 4f; // 24+ -> 28+
        }
    }

    /**
     * Aplica un Look and Feel moderno a la aplicación
     */
    public static void aplicarEstiloModerno() {
        try {
            // Preferir FlatLaf (si está disponible) porque ofrece un aspecto moderno y
            // consistente
            try {
                FlatLightLaf.setup();
                aplicarPropiedadesModernas();
                aplicarFuenteGlobal(new Font("Segoe UI", Font.PLAIN, 24));
                inicializarAjustadorFuentes();
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
                    aplicarFuenteGlobal(new Font("Segoe UI", Font.PLAIN, 22));
                    inicializarAjustadorFuentes();
                    return; // Éxito, salir
                }
            }

            // Si no se encuentra Nimbus, aplicar propiedades básicas al LAF actual
            aplicarPropiedadesModernas();
            aplicarFuenteGlobal(new Font("Segoe UI", Font.PLAIN, 22));
            inicializarAjustadorFuentes();

        } catch (Exception e) {
            System.err.println("Error aplicando Look and Feel moderno: " + e.getMessage());
        }
    }

    /**
     * Aplica un tema con color primario y fuente base personalizados en tiempo de
     * ejecución.
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
        UIManager.put("control", new Color(250, 250, 250)); // Fondo general
        UIManager.put("nimbusBase", new Color(51, 98, 140)); // Azul principal
        UIManager.put("nimbusBlueGrey", new Color(169, 184, 196)); // Gris azulado
        UIManager.put("nimbusFocus", new Color(63, 81, 181)); // Color de foco (Material Indigo)
        UIManager.put("nimbusSelectedText", Color.WHITE); // Texto seleccionado
        UIManager.put("nimbusSelectionBackground", new Color(63, 81, 181)); // Fondo de selección

        // Botones más modernos
        UIManager.put("Button.background", new Color(33, 150, 243)); // Azul Material
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
        UIManager.put("TitledBorder.font", new Font("Arial", Font.BOLD, 21));
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
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("Table.showGrid", Boolean.FALSE);
        UIManager.put("Table.selectionBackground", new Color(63, 81, 181));
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("TableHeader.background", new Color(245, 245, 245));
        UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 22));

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
    public static void aplicarFuenteGlobal(Font font) {
        if (font == null)
            return;
        globalFont = font;
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

        // Force some common keys derived dynamically from the new size
        float size = font.getSize2D();
        UIManager.put("defaultFont", font);
        UIManager.put("Button.font", font.deriveFont(Font.BOLD, size));
        UIManager.put("Label.font", font.deriveFont(size - 1f > 12f ? size - 1f : 12f));
        UIManager.put("TextField.font", font.deriveFont(size));
        UIManager.put("TextArea.font", font.deriveFont(size));
        UIManager.put("Table.font", font.deriveFont(size - 1f > 12f ? size - 1f : 12f));
        UIManager.put("TableHeader.font", font.deriveFont(Font.BOLD, size));

        // Improve option panes and dialogs
        UIManager.put("OptionPane.messageFont", font.deriveFont(size));
        UIManager.put("OptionPane.buttonFont", font.deriveFont(Font.BOLD, size - 1f > 12f ? size - 1f : 12f));
    }

    /**
     * Aplica estilo moderno a un JDialog específico
     */
    public static void aplicarEstiloModernoADialogo(JDialog dialog) {
        if (dialog == null)
            return;

        // Fondo moderno
        dialog.getContentPane().setBackground(new Color(250, 250, 250));

        // Aplicar estilos a todos los componentes del diálogo
        aplicarEstilosRecursivamente(dialog.getContentPane());

        // Sombra moderna (si es posible en el sistema)
        try {
            dialog.getRootPane().setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        } catch (Exception e) {
            // Ignorar si no se puede aplicar
        }
    }

    /**
     * Aplica estilos modernos recursivamente a todos los componentes
     */
    private static void aplicarEstilosRecursivamente(Container container) {
        float size = globalFont.getSize2D();
        // Calculate relative sizes - agresivos para máxima legibilidad
        int sizeButton = Math.max(Math.round(size * 0.85f), 20);
        int sizeToggle = Math.max(Math.round(size * 0.85f), 20);
        int sizeControl = Math.max(Math.round(size * 0.85f), 20);
        int sizeField = Math.round(size * 0.9f) + 4;
        int sizeLabel = Math.round(size * 0.9f) + 4;
        int sizeText = Math.round(size * 0.9f) + 4;

        String family = globalFont.getFamily();
        Font fontButton = new Font(family, Font.BOLD, sizeButton);
        Font fontField = new Font(family, Font.PLAIN, sizeField);
        Font fontLabel = new Font(family, Font.PLAIN, sizeLabel);
        Font fontLabelBold = new Font(family, Font.BOLD, sizeLabel);
        Font fontTextPlain = new Font(family, Font.PLAIN, sizeText);
        Font fontControlPlain = new Font(family, Font.PLAIN, sizeControl);
        Font fontControlBold = new Font(family, Font.BOLD, sizeControl);
        Font fontToggle = new Font(family, Font.BOLD, sizeToggle);

        if (container == null || container.getComponents() == null)
            return;

        for (Component comp : container.getComponents()) {
            if (comp == null)
                continue;
            try {
                if (comp instanceof JButton) {
                    aplicarEstiloBotonModerno((JButton) comp, fontButton);
                    registrarComponenteAjustado(comp);
                } else if (comp instanceof JTextField) {
                    aplicarEstiloCampoModerno((JTextField) comp, fontField);
                    registrarComponenteAjustado(comp);
                } else if (comp instanceof JLabel) {
                    aplicarEstiloEtiquetaModerna((JLabel) comp, fontLabel);
                    registrarComponenteAjustado(comp);
                } else if (comp instanceof JPanel) {
                    aplicarEstiloPanelModerno((JPanel) comp, fontLabelBold);
                    registrarComponenteAjustado(comp);
                } else if (comp instanceof JToggleButton) {
                    aplicarEstiloToggleModerno((JToggleButton) comp, fontToggle);
                    registrarComponenteAjustado(comp);
                } else if (comp instanceof JComboBox) {
                    comp.setFont(fontControlPlain);
                    registrarComponenteAjustado(comp);
                } else if (comp instanceof JCheckBox) {
                    comp.setFont(fontControlPlain);
                    registrarComponenteAjustado(comp);
                } else if (comp instanceof JRadioButton) {
                    comp.setFont(fontControlPlain);
                    registrarComponenteAjustado(comp);
                } else if (comp instanceof JTabbedPane) {
                    comp.setFont(fontControlBold);
                    registrarComponenteAjustado(comp);
                } else if (comp instanceof JList) {
                    comp.setFont(fontTextPlain);
                    registrarComponenteAjustado(comp);
                } else if (comp instanceof JTable) {
                    comp.setFont(fontTextPlain);
                    registrarComponenteAjustado(comp);
                } else if (comp instanceof JTextArea) {
                    comp.setFont(fontTextPlain);
                    registrarComponenteAjustado(comp);
                }
            } catch (Throwable t) {
                System.err.println("[LookAndFeelDebug] Error styling individual component: " + t.getMessage());
            }

            // Aplicar recursivamente a contenedores
            if (comp instanceof Container) {
                try {
                    aplicarEstilosRecursivamente((Container) comp);
                } catch (Throwable t) {
                    System.err.println("[LookAndFeelDebug] Error recursing into container: " + t.getMessage());
                }
            }
        }
    }

    /**
     * Public helper to apply modern styles recursively to any container.
     * Useful for modules that want to restyle existing UI components at runtime.
     */
    public static void estilizarComponentes(Container container) {
        if (container == null)
            return;
        aplicarEstilosRecursivamente(container);
    }

    /**
     * Aplica estilo moderno a un botón
     */
    private static void aplicarEstiloBotonModerno(JButton button, Font font) {
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBackground(new Color(33, 150, 243)); // Material Blue
        button.setForeground(Color.WHITE);
        button.setFont(font);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Bordes redondeados (efecto visual) - Reducidos para mantener botones
        // compactos
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(30, 136, 220), 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));

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
    private static void aplicarEstiloCampoModerno(JTextField field, Font font) {
        field.setFont(font);
        // Padding reducido para mantener los campos compactos
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 224, 224), 2),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        field.setBackground(Color.WHITE);

        // Efecto focus
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(63, 81, 181), 2),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(224, 224, 224), 2),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            }
        });
    }

    /**
     * Aplica estilo moderno a una etiqueta
     */
    private static void aplicarEstiloEtiquetaModerna(JLabel label, Font font) {
        label.setFont(font);
        label.setForeground(new Color(66, 66, 66));
    }

    /**
     * Aplica estilo moderno a un panel
     */
    private static void aplicarEstiloPanelModerno(JPanel panel, Font font) {
        panel.setBackground(new Color(250, 250, 250));

        // Si tiene borde de título, modernizarlo
        if (panel.getBorder() instanceof javax.swing.border.TitledBorder) {
            javax.swing.border.TitledBorder titleBorder = (javax.swing.border.TitledBorder) panel.getBorder();
            titleBorder.setTitleFont(font);
            titleBorder.setTitleColor(new Color(66, 66, 66));
        }
    }

    /**
     * Aplica estilo moderno a un toggle button
     */
    private static void aplicarEstiloToggleModerno(JToggleButton toggle, Font font) {
        toggle.setFocusPainted(false);
        toggle.setBorderPainted(false);
        toggle.setFont(font);
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