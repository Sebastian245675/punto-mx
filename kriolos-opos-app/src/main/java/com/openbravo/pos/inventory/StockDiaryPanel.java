//    KriolOS POS
//    Copyright (c) 2019-2023 KriolOS
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.data.user.ListProvider;
import com.openbravo.data.user.DefaultSaveProvider;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSales;
//import com.openbravo.pos.suppliers.DataLogicSuppliers;
import com.openbravo.pos.panels.JPanelTable;
import com.openbravo.pos.suppliers.DataLogicSuppliers;
import javax.swing.*;
import java.awt.*;

/**
 *
 * @author adrianromero
 */
public class StockDiaryPanel extends JPanelTable {
    
    private StockDiaryEditor jeditor;    
    private DataLogicSales m_dlSales;
//    private DataLogicSuppliers m_dlSuppliers;
    
    /** Creates a new instance of JPanelDiaryEditor */
    public StockDiaryPanel() {
    }
    
    /**
     *
     */
    @Override
    protected void init() {
        m_dlSales = (DataLogicSales) app.getBean("com.openbravo.pos.forms.DataLogicSales");
//        DataLogicSuppliers m_dlSuppliers = (DataLogicSuppliers) app.getBean("com.openbravo.pos.suppliers.DataLogicSuppliers");        
        jeditor = new StockDiaryEditor(app, dirty); 
        
        // Aplicar estructura avanzada y diseño profesional
        aplicarEstructuraAvanzada();
    }
    
    /**
     * Aplicar estructura avanzada y diseño profesional
     */
    private void aplicarEstructuraAvanzada() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Optimizar layout principal
                optimizarLayoutPrincipal();
                
                // Reestructurar componentes
                reestructurarComponentesProfesional();
                
                // Mejorar jerarquía visual
                aplicarJerarquiaVisual();
                
                // Optimizar espaciado y agrupación
                optimizarEspaciadoYAgrupacion();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Optimizar el layout principal con estructura profesional
     */
    private void optimizarLayoutPrincipal() {
        // Aplicar espaciado profesional
        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Configurar fondo con mejor contraste
        this.setBackground(new Color(252, 253, 254));
        
        // Si tiene layout manager, optimizarlo
        if (this.getLayout() instanceof BorderLayout) {
            BorderLayout layout = (BorderLayout) this.getLayout();
            layout.setHgap(15);
            layout.setVgap(15);
        }
    }
    
    /**
     * Reestructurar componentes de manera profesional
     */
    private void reestructurarComponentesProfesional() {
        // Aplicar mejoras estructurales a todos los componentes
        reestructurarComponentesRecursivo(this);
    }
    
    /**
     * Reestructurar componentes recursivamente
     */
    private void reestructurarComponentesRecursivo(Container container) {
        Component[] components = container.getComponents();
        
        for (Component comp : components) {
            if (comp instanceof JButton) {
                aplicarEstructuraBoton((JButton) comp);
            } else if (comp instanceof JTable) {
                aplicarEstructuraTabla((JTable) comp);
            } else if (comp instanceof JTextField) {
                aplicarEstructuraCampo((JTextField) comp);
            } else if (comp instanceof JLabel) {
                aplicarEstructuraEtiqueta((JLabel) comp);
            } else if (comp instanceof JPanel) {
                aplicarEstructuraPanel((JPanel) comp);
            } else if (comp instanceof JScrollPane) {
                aplicarEstructuraScrollPane((JScrollPane) comp);
            } else if (comp instanceof JSplitPane) {
                aplicarEstructuraSplitPane((JSplitPane) comp);
            } else if (comp instanceof JToolBar) {
                aplicarEstructuraToolBar((JToolBar) comp);
            }
            
            // Continuar recursivamente
            if (comp instanceof Container) {
                reestructurarComponentesRecursivo((Container) comp);
            }
        }
    }
    
    /**
     * Aplicar estructura profesional a botones
     */
    private void aplicarEstructuraBoton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
            BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        button.setFocusPainted(false);
        button.setMargin(new Insets(8, 16, 8, 16));
        
        // Mejorar spacing
        Dimension size = button.getPreferredSize();
        button.setPreferredSize(new Dimension(size.width + 8, size.height + 4));
    }
    
    /**
     * Aplicar estructura profesional a tablas
     */
    private void aplicarEstructuraTabla(JTable table) {
        // Tipografía moderna
        table.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        // Espaciado óptimo
        table.setRowHeight(32);
        table.setIntercellSpacing(new Dimension(1, 1));
        
        // Grid profesional
        table.setGridColor(new Color(235, 235, 235));
        table.setShowGrid(true);
        
        // Colores de selección modernos
        table.setSelectionBackground(new Color(230, 240, 255));
        table.setSelectionForeground(new Color(60, 60, 60));
        
        // Header profesional
        if (table.getTableHeader() != null) {
            javax.swing.table.JTableHeader header = table.getTableHeader();
            header.setFont(new Font("Segoe UI", Font.BOLD, 11));
            header.setBackground(new Color(248, 249, 250));
            header.setForeground(new Color(70, 70, 70));
            header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
            ));
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        }
    }
    
    /**
     * Aplicar estructura profesional a campos de texto
     */
    private void aplicarEstructuraCampo(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        
        // Mejorar tamaño mínimo
        Dimension size = field.getPreferredSize();
        field.setPreferredSize(new Dimension(size.width, Math.max(size.height, 36)));
    }
    
    /**
     * Aplicar estructura profesional a etiquetas
     */
    private void aplicarEstructuraEtiqueta(JLabel label) {
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(new Color(80, 80, 80));
        
        // Detectar etiquetas importantes
        String text = label.getText();
        if (text != null && text.contains(":")) {
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));
            label.setForeground(new Color(60, 60, 60));
        }
    }
    
    /**
     * Aplicar estructura profesional a paneles
     */
    private void aplicarEstructuraPanel(JPanel panel) {
        // Espaciado interno profesional
        if (panel.getBorder() == null || panel.getBorder() instanceof javax.swing.border.EmptyBorder) {
            panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        }
        
        // Fondo sutil
        if (panel.getBackground() != null && panel.isOpaque()) {
            panel.setBackground(new Color(254, 254, 254));
        }
    }
    
    /**
     * Aplicar estructura profesional a scroll panes
     */
    private void aplicarEstructuraScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(3, 3, 3, 3)
        ));
        
        // Mejorar viewport si contiene tabla
        if (scrollPane.getViewport().getView() instanceof JTable) {
            JTable table = (JTable) scrollPane.getViewport().getView();
            aplicarEstructuraTabla(table);
        }
    }
    
    /**
     * Aplicar estructura profesional a split panes
     */
    private void aplicarEstructuraSplitPane(JSplitPane splitPane) {
        splitPane.setBorder(null);
        splitPane.setDividerSize(8);
        splitPane.setOpaque(false);
        
        // Mejorar divider
        splitPane.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(new Color(240, 240, 240));
                        g.fillRect(0, 0, getSize().width, getSize().height);
                    }
                };
            }
        });
    }
    
    /**
     * Aplicar estructura profesional a toolbars
     */
    private void aplicarEstructuraToolBar(JToolBar toolBar) {
        toolBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        toolBar.setFloatable(false);
        
        // Aplicar espaciado entre elementos
        Component[] components = toolBar.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JButton) {
                aplicarEstructuraBoton((JButton) components[i]);
            }
            
            // Agregar espaciado entre botones
            if (i < components.length - 1) {
                toolBar.add(Box.createHorizontalStrut(8));
            }
        }
    }
    
    /**
     * Aplicar jerarquía visual profesional
     */
    private void aplicarJerarquiaVisual() {
        // Definir jerarquía de fonts
        Font titleFont = new Font("Segoe UI", Font.BOLD, 14);
        Font subtitleFont = new Font("Segoe UI", Font.BOLD, 12);
        Font bodyFont = new Font("Segoe UI", Font.PLAIN, 11);
        Font captionFont = new Font("Segoe UI", Font.PLAIN, 10);
        
        // Aplicar jerarquía basada en contexto
        aplicarJerarquiaRecursiva(this, bodyFont);
    }
    
    /**
     * Aplicar jerarquía visual recursivamente
     */
    private void aplicarJerarquiaRecursiva(Container container, Font defaultFont) {
        Component[] components = container.getComponents();
        
        for (Component comp : components) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                String text = label.getText();
                
                // Determinar nivel jerárquico
                if (text != null) {
                    if (text.length() > 20 && !text.contains(":")) {
                        // Texto largo: caption
                        label.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    } else if (text.contains(":")) {
                        // Etiqueta de campo: subtitle
                        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    }
                }
            } else if (comp instanceof Container) {
                aplicarJerarquiaRecursiva((Container) comp, defaultFont);
            }
        }
    }
    
    /**
     * Optimizar espaciado y agrupación
     */
    private void optimizarEspaciadoYAgrupacion() {
        // Aplicar espaciado óptimo entre grupos de componentes
        optimizarEspaciadoRecursivo(this);
    }
    
    /**
     * Optimizar espaciado recursivamente
     */
    private void optimizarEspaciadoRecursivo(Container container) {
        // Aplicar espaciado profesional basado en el layout manager
        if (container.getLayout() instanceof BorderLayout) {
            BorderLayout layout = (BorderLayout) container.getLayout();
            layout.setHgap(12);
            layout.setVgap(12);
        } else if (container.getLayout() instanceof FlowLayout) {
            FlowLayout layout = (FlowLayout) container.getLayout();
            layout.setHgap(10);
            layout.setVgap(8);
        } else if (container.getLayout() instanceof GridLayout) {
            GridLayout layout = (GridLayout) container.getLayout();
            layout.setHgap(8);
            layout.setVgap(8);
        }
        
        // Continuar recursivamente
        Component[] components = container.getComponents();
        for (Component comp : components) {
            if (comp instanceof Container) {
                optimizarEspaciadoRecursivo((Container) comp);
            }
        }
    }
    
    /**
     *
     * @return
     */
    @Override
    public ListProvider getListProvider() {
        return null;
    }
    
    /**
     *
     * @return
     */
    @Override
    public DefaultSaveProvider getSaveProvider() {
        return  new DefaultSaveProvider(null
                , m_dlSales.getStockDiaryInsert()
                , m_dlSales.getStockDiaryDelete());      
    }
    
    /**
     *
     * @return
     */
    @Override
    public EditorRecord getEditor() {
        return jeditor;
    }
    
    /**
     *
     * @return
     */
    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.StockDiary");
    }

    /**
     *
     * @throws BasicException
     */
    @Override
    public void activate() throws BasicException {
        jeditor.activate(); // primero activo el editor 
        super.activate();   // segundo activo el padre        
    } 
}
