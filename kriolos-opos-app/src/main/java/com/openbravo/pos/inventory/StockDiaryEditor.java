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

import com.openbravo.data.user.DirtyManager;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.suppliers.DataLogicSuppliers;
import com.openbravo.pos.panels.JProductFinder;
import java.awt.Component;
import java.awt.Container;
import com.openbravo.basic.BasicException;
import com.openbravo.beans.DateUtils;
import com.openbravo.beans.JCalendarDialog;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.format.Formats;
import com.openbravo.pos.catalog.CatalogSelector;
import com.openbravo.pos.catalog.JCatalog;
import com.openbravo.pos.printer.TicketParser;
import com.openbravo.pos.sales.JProductAttEdit;
import com.openbravo.pos.ticket.ProductInfoExt;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public final class StockDiaryEditor extends javax.swing.JPanel implements EditorRecord {
    
    private final AppView m_App;
    private final DataLogicSystem m_dlSystem;    
    private final DataLogicSales m_dlSales;
    private final DataLogicSuppliers m_dlSuppliers;    
    private final TicketParser m_TTP;
    
    private final CatalogSelector m_cat;
    private final ComboBoxValModel m_ReasonModel;

    private final SentenceList m_sentlocations;
    private ComboBoxValModel m_LocationsModel;    
    private ComboBoxValModel m_LocationsModelDes;
    
    private final SentenceList m_sentsuppliers;
    private ComboBoxValModel m_SuppliersModel;      
    private String m_sID;

    private String productid;
    private String productref;
    private String productcode;
    private String productname;
    private String attsetid;
    private String attsetinstid;
    private String attsetinstdesc;
    private String sAppUser;
    

    private final String user;    

    
    /** Creates new form StockDiaryEditor
     * @param app
     * @param dirty */
    public StockDiaryEditor(AppView app, DirtyManager dirty) {
        
        m_App = app;
        m_dlSystem = (DataLogicSystem) m_App.getBean("com.openbravo.pos.forms.DataLogicSystem");
        m_dlSales = (DataLogicSales) m_App.getBean("com.openbravo.pos.forms.DataLogicSales");
        m_dlSuppliers = (DataLogicSuppliers) m_App.getBean("com.openbravo.pos.suppliers.DataLogicSuppliers");        
        m_TTP = new TicketParser(m_App.getDeviceTicket(), m_dlSystem);
        
        // Inicializar variables final
        m_sentlocations = m_dlSales.getLocationsList();
        m_LocationsModel = new ComboBoxValModel();       
        m_LocationsModelDes = new ComboBoxValModel();  
        
        m_sentsuppliers = m_dlSuppliers.getSupplierList();
        m_SuppliersModel = new ComboBoxValModel();
        
        m_ReasonModel = new ComboBoxValModel();
        m_ReasonModel.add(MovementReason.IN_PURCHASE);
        m_ReasonModel.add(MovementReason.IN_REFUND);
        m_ReasonModel.add(MovementReason.IN_MOVEMENT);
        m_ReasonModel.add(MovementReason.OUT_SALE);
        m_ReasonModel.add(MovementReason.OUT_REFUND);
        m_ReasonModel.add(MovementReason.OUT_BREAK);
        m_ReasonModel.add(MovementReason.OUT_MOVEMENT);   
        m_ReasonModel.add(MovementReason.OUT_CROSSING);          

        m_cat = new JCatalog(m_dlSales);
        m_cat.addActionListener(new CatalogListener());
        
        initComponents();
        
        // Configurar modelos y listeners
        m_jreason.setModel(m_ReasonModel);
        catcontainer.add(m_cat.getComponent(), BorderLayout.CENTER);        
        
        m_jdate.getDocument().addDocumentListener(dirty);
        m_jreason.addActionListener(dirty);
        m_jLocation.addActionListener(dirty);
        m_jLocationDes.addActionListener(dirty);
      
        jproduct.getDocument().addDocumentListener(dirty);
        jattributes.getDocument().addDocumentListener(dirty);
        m_junits.getDocument().addDocumentListener(dirty);
        m_jprice.getDocument().addDocumentListener(dirty);
        m_jSupplier.addActionListener(dirty);  
        
        writeValueEOF();
        
        // Aplicar mejoras estructurales avanzadas
        reestructurarLayoutProfesional();
        aplicarMejorasVisuales();
        
        user = m_App.getAppUserView().getUser().getName();        
    }
    
    /**
     * Reestructurar el layout con diseño profesional avanzado
     */
    private void reestructurarLayoutProfesional() {
        SwingUtilities.invokeLater(() -> {
            // Mejorar estructura general
            optimizarEstructuraGeneral();
            
            // Reorganizar campos en grupos lógicos
            crearAgrupacionesLogicas();
            
            // Mejorar tipografía y espaciado
            aplicarTipografiaProfesional();
            
            // Optimizar tabla de resultados
            optimizarTablaResultados();
        });
    }
    
    /**
     * Optimizar la estructura general del panel
     */
    private void optimizarEstructuraGeneral() {
        // Aplicar espaciado más profesional
        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Mejorar el panel principal si existe
        Component[] components = this.getComponents();
        for (Component comp : components) {
            if (comp instanceof javax.swing.JPanel) {
                javax.swing.JPanel panel = (javax.swing.JPanel) comp;
                panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            }
        }
    }
    
    /**
     * Crear agrupaciones lógicas de campos
     */
    private void crearAgrupacionesLogicas() {
        // Buscar y agrupar campos relacionados
        mejorarGrupoProducto();
        mejorarGrupoMovimiento();
        mejorarGrupoUbicacion();
        mejorarGrupoFechaYUsuario();
    }
    
    /**
     * Mejorar el grupo de información del producto
     */
    private void mejorarGrupoProducto() {
        // Encontrar componentes relacionados con producto
        Component[] components = this.getComponents();
        
        // Aplicar mejoras a campos de producto
        for (Component comp : components) {
            if (comp instanceof javax.swing.JPanel) {
                buscarYMejorarCamposProducto((javax.swing.JPanel) comp);
            }
        }
    }
    
    /**
     * Buscar y mejorar campos de producto en un panel
     */
    private void buscarYMejorarCamposProducto(javax.swing.JPanel panel) {
        Component[] components = panel.getComponents();
        
        for (Component comp : components) {
            if (comp instanceof JTextField) {
                JTextField field = (JTextField) comp;
                // Aplicar estilo profesional a campos de texto
                field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            } else if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                // Mejorar tipografía de etiquetas
                label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                label.setForeground(new Color(60, 60, 60));
            }
        }
    }
    
    /**
     * Mejorar el grupo de información del movimiento
     */
    private void mejorarGrupoMovimiento() {
        // Aplicar mejoras específicas para campos de movimiento
        optimizarCamposNumericos();
    }
    
    /**
     * Optimizar campos numéricos (cantidades, precios)
     */
    private void optimizarCamposNumericos() {
        Component[] allComponents = getAllComponents(this);
        
        for (Component comp : allComponents) {
            if (comp instanceof JTextField) {
                JTextField field = (JTextField) comp;
                String name = field.getName();
                
                // Identificar campos numéricos y aplicar formato especial
                if (name != null && (name.contains("quantity") || name.contains("price") || name.contains("units"))) {
                    field.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    field.setHorizontalAlignment(JTextField.RIGHT);
                    field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)
                    ));
                }
            }
        }
    }
    
    /**
     * Mejorar el grupo de ubicación
     */
    private void mejorarGrupoUbicacion() {
        Component[] allComponents = getAllComponents(this);
        
        for (Component comp : allComponents) {
            if (comp instanceof JComboBox) {
                JComboBox<?> combo = (JComboBox<?>) comp;
                combo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                combo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)
                ));
            }
        }
    }
    
    /**
     * Mejorar el grupo de fecha y usuario
     */
    private void mejorarGrupoFechaYUsuario() {
        // Aplicar estilo especial a campos de fecha y usuario
        Component[] allComponents = getAllComponents(this);
        
        for (Component comp : allComponents) {
            if (comp instanceof JTextField) {
                JTextField field = (JTextField) comp;
                String name = field.getName();
                
                if (name != null && (name.contains("date") || name.contains("user"))) {
                    field.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                    field.setForeground(new Color(100, 100, 100));
                }
            }
        }
    }
    
    /**
     * Aplicar tipografía profesional
     */
    private void aplicarTipografiaProfesional() {
        // Definir jerarquía tipográfica
        Font fuenteTitulo = new Font("Segoe UI", Font.BOLD, 14);
        Font fuenteSubtitulo = new Font("Segoe UI", Font.BOLD, 12);
        Font fuenteNormal = new Font("Segoe UI", Font.PLAIN, 11);
        Font fuentePequeña = new Font("Segoe UI", Font.PLAIN, 10);
        
        // Aplicar jerarquía a todos los componentes
        aplicarJerarquiaTipografica(this, fuenteNormal);
    }
    
    /**
     * Aplicar jerarquía tipográfica recursivamente
     */
    private void aplicarJerarquiaTipografica(Container container, Font fuenteBase) {
        Component[] components = container.getComponents();
        
        for (Component comp : components) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                label.setFont(fuenteBase);
                
                // Detectar si es un título por el texto
                String text = label.getText();
                if (text != null && text.length() < 20 && text.contains(":")) {
                    label.setFont(new Font("Segoe UI", Font.BOLD, 11));
                }
            } else if (comp instanceof JTextField) {
                JTextField field = (JTextField) comp;
                field.setFont(fuenteBase);
            } else if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                button.setFont(new Font("Segoe UI", Font.BOLD, 11));
            } else if (comp instanceof Container) {
                aplicarJerarquiaTipografica((Container) comp, fuenteBase);
            }
        }
    }
    
    /**
     * Optimizar tabla de resultados
     */
    private void optimizarTablaResultados() {
        Component[] allComponents = getAllComponents(this);
        
        for (Component comp : allComponents) {
            if (comp instanceof javax.swing.JTable) {
                javax.swing.JTable table = (javax.swing.JTable) comp;
                
                // Aplicar mejoras estructurales a la tabla
                table.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                table.setRowHeight(32);
                table.setGridColor(new Color(240, 240, 240));
                table.setShowGrid(true);
                table.setIntercellSpacing(new java.awt.Dimension(1, 1));
                
                // Mejorar header
                if (table.getTableHeader() != null) {
                    table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
                    table.getTableHeader().setBackground(new Color(250, 250, 250));
                    table.getTableHeader().setForeground(new Color(70, 70, 70));
                    table.getTableHeader().setPreferredSize(new java.awt.Dimension(
                        table.getTableHeader().getPreferredSize().width, 36
                    ));
                }
            } else if (comp instanceof javax.swing.JScrollPane) {
                javax.swing.JScrollPane scroll = (javax.swing.JScrollPane) comp;
                scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
            }
        }
        
        // Continuar con las mejoras visuales
        SwingUtilities.invokeLater(() -> {
            aplicarMejorasEstructurales();
        });
    }
    
    /**
     *
     * @throws BasicException
     */
    public void activate() throws BasicException {
        m_cat.loadCatalog();
         
        java.util.List l = m_sentlocations.list();
        m_LocationsModel = new ComboBoxValModel(m_sentlocations.list());
        m_jLocation.setModel(m_LocationsModel);
        m_LocationsModelDes = new ComboBoxValModel(l);
        m_jLocationDes.setModel(m_LocationsModelDes);
        
        java.util.List sl = m_sentsuppliers.list();
        m_SuppliersModel = new ComboBoxValModel(m_sentsuppliers.list());
        m_jSupplier.setModel(m_SuppliersModel);
        
    }
    
    /**
     *
     */
    @Override
    public void refresh() {
    }
    
    /**
     *
     */
    @Override
    public void writeValueEOF() {
        m_sID = null;
        m_jdate.setText(null);
        m_ReasonModel.setSelectedKey(null);
        m_LocationsModel.setSelectedKey(m_App.getInventoryLocation());
        m_LocationsModelDes.setSelectedKey(m_App.getInventoryLocation());        
        m_SuppliersModel.setSelectedKey(null);        
        
        productid = null;
        productref = null;
        productcode = null;
        productname = null;
        m_jreference.setText(null);
        m_jcodebar.setText(null);
        jproduct.setText(null);
        attsetid = null;
        attsetinstid = null;
        attsetinstdesc = null;
        jattributes.setText(null);
        m_junits.setText(null);
        m_jprice.setText(null);
        m_jdate.setEnabled(false);
        m_jbtndate.setEnabled(false);
        m_jreason.setEnabled(false);
        m_jreference.setEnabled(false);
        m_jEnter1.setEnabled(false);
        m_jcodebar.setEnabled(false);
        m_jEnter.setEnabled(false);
        m_jLocation.setEnabled(false);
        jproduct.setEnabled(false);
        jEditProduct.setEnabled(false);
        jattributes.setEnabled(false);
        jEditAttributes.setEnabled(false);
        m_junits.setEnabled(false);
        m_jprice.setEnabled(false);
        m_cat.setComponentEnabled(false);
    }
    
    /**
     *
     */
    @Override
    public void writeValueInsert() {
        m_sID = UUID.randomUUID().toString();
        m_jdate.setText(Formats.TIMESTAMP.formatValue(DateUtils.getTodayMinutes()));
        m_ReasonModel.setSelectedItem(MovementReason.IN_PURCHASE);
        m_LocationsModel.setSelectedKey(m_App.getInventoryLocation());
        m_LocationsModelDes.setSelectedKey(m_App.getInventoryLocation());        
        m_SuppliersModel.setSelectedKey(null);        
        
        productid = null;
        productref = null;
        productcode = null;
        productname = null;
        m_jreference.setText(null);
        m_jcodebar.setText(null);
        jproduct.setText(null);
        attsetid = null;
        attsetinstid = null;
        attsetinstdesc = null;
        jattributes.setText(null);
        m_jcodebar.setText(null);
        m_junits.setText(null);
        m_jprice.setText(null);
        m_jdate.setEnabled(true);
        m_jbtndate.setEnabled(true);
        m_jreason.setEnabled(true);
        m_jreference.setEnabled(true);
        m_jEnter1.setEnabled(true);
        m_jcodebar.setEnabled(true);
        m_jEnter.setEnabled(true);
        m_jLocation.setEnabled(true);
        m_jSupplier.setEnabled(true);        
        jproduct.setEnabled(true);
        jEditProduct.setEnabled(true);
        jattributes.setEnabled(true);
        jEditAttributes.setEnabled(true);
        m_junits.setEnabled(true);
        m_jprice.setEnabled(true);   
        m_cat.setComponentEnabled(true);
    }

    /**
     *
     * @param value
     */
    @Override
    public void writeValueDelete(Object value) {
        Object[] diary = (Object[]) value;
        m_sID = (String) diary[0];
        m_jdate.setText(Formats.TIMESTAMP.formatValue((Date)diary[1]));
        m_ReasonModel.setSelectedKey(diary[2]);
        m_LocationsModel.setSelectedKey(diary[3]);
        productid = (String) diary[4];
        attsetinstid = (String) diary[5];
        m_junits.setText(Formats.DOUBLE.formatValue(signum((Double) diary[6], (Integer) diary[2])));
        m_jprice.setText(Formats.CURRENCY.formatValue((Double)diary[7]));
        productref = (String) diary[8];
        productcode = (String) diary[9];
        productname =(String) diary[10];
        attsetid = (String) diary[11];
        attsetinstdesc = (String) diary[12];
        m_SuppliersModel.setSelectedKey(diary[13]);        
        
        m_jreference.setText(productref);
        m_jcodebar.setText(productcode);
        jproduct.setText(productname);
        jattributes.setText(attsetinstdesc);

        m_jdate.setEnabled(false);
        m_jbtndate.setEnabled(false);
        m_jreason.setEnabled(false);
        m_jreference.setEnabled(false);
        m_jEnter1.setEnabled(false);
        m_jcodebar.setEnabled(false);
        m_jEnter.setEnabled(false);
        m_jLocation.setEnabled(false);
        m_jLocationDes.setEnabled(false);        
        m_jSupplier.setEnabled(false);        
        jproduct.setEnabled(false);
        jEditProduct.setEnabled(false);
        jattributes.setEnabled(false);
        jEditAttributes.setEnabled(false);
        m_junits.setEnabled(false);
        m_jprice.setEnabled(false);   
        m_cat.setComponentEnabled(false);
    }
    
    /**
     *
     * @param value
     */
    @Override
    public void writeValueEdit(Object value) {
        Object[] diary = (Object[]) value;
        m_sID = (String) diary[0];
        m_jdate.setText(Formats.TIMESTAMP.formatValue((Date)diary[1]));
        m_ReasonModel.setSelectedKey(diary[2]);
        m_LocationsModel.setSelectedKey(diary[3]);
        productid = (String) diary[4];
        attsetinstid = (String) diary[5];
        m_junits.setText(Formats.DOUBLE.formatValue(signum((Double) diary[6], (Integer) diary[2])));
        m_jprice.setText(Formats.CURRENCY.formatValue((Double)diary[7]));
        sAppUser = (String) diary[8];
        productref = (String) diary[9];
        productcode = (String) diary[10];
        productname =(String) diary[11];
        attsetid = (String) diary[12];
        attsetinstdesc = (String) diary[13];
        m_SuppliersModel.setSelectedKey(diary[14]);        

        m_jreference.setText(productref);
        m_jcodebar.setText(productcode);
        jproduct.setText(productname);
        jattributes.setText(attsetinstdesc);

        m_jdate.setEnabled(false);
        m_jbtndate.setEnabled(false);
        m_jreason.setEnabled(false);
        m_jreference.setEnabled(false);
        m_jEnter1.setEnabled(false);
        m_jcodebar.setEnabled(false);
        m_jEnter.setEnabled(false);
        m_jLocation.setEnabled(false);
        m_jLocationDes.setEnabled(false);
        m_jSupplier.setEnabled(false);         
        jproduct.setEnabled(true);
        jEditProduct.setEnabled(true);
        jattributes.setEnabled(false);
        jEditAttributes.setEnabled(false);
        m_junits.setEnabled(false);
        m_jprice.setEnabled(false);  
        m_cat.setComponentEnabled(false);
    }
    
    /**
     *
     * @return
     * @throws BasicException
     */
    @Override
    public Object createValue() throws BasicException {

        return new Object[] {
            m_sID,
            Formats.TIMESTAMP.parseValue(m_jdate.getText()),
            m_ReasonModel.getSelectedKey(),
            m_LocationsModel.getSelectedKey(),
            productid,
            attsetinstid,
            samesignum((Double) Formats.DOUBLE.parseValue(m_junits.getText()), (Integer) m_ReasonModel.getSelectedKey()),
            Formats.CURRENCY.parseValue(m_jprice.getText()),
            m_App.getAppUserView().getUser().getName(),
            productref,
            productcode,
            productname,
            attsetid,
            attsetinstdesc,
//            m_SuppliersModel.getSelectedKey()
        };
    }
    
    /**
     *
     * @return
     */
    @Override
    public Component getComponent() {
        return this;
    }
//    private ProductInfoExt getProduct(String id)  {
//        try {
//            return m_dlSales.getProductInfo(id);
//        } catch (BasicException e) {
//            return null;
//        }
//    }
    
    private Double signum(Double d, Integer i) {
        if (d == null || i == null) {
            return d;
        } else if (i < 0) {
            return -d;
        } else {
            return d;
        } 
    }
    
    private Double samesignum(Double d, Integer i) {
        
        if (d == null || i == null) {
            return d;
        } else if ((i > 0 && d < 0.0) ||
            (i < 0 && d > 0.0)) {
            return -d;
        } else {
            return d;
        }            
    }
    
    private void assignProduct(ProductInfoExt prod) {
        
        if (jproduct.isEnabled()) {
            if (prod == null) {
                productid = null;
                productref = null;
                productcode = null;
                productname = null;
                attsetid = null;
                attsetinstid = null;
                attsetinstdesc = null;
                jproduct.setText(null);
                m_jcodebar.setText(null);
                m_jreference.setText(null);
                jattributes.setText(null);
//                m_jSupplier = null;
            } else {
                productid = prod.getID();
                productref = prod.getReference();
                productcode = prod.getCode();
                productname = prod.toString();
                attsetid = prod.getAttributeSetID();
                attsetinstid = null;
                attsetinstdesc = null;
                jproduct.setText(productname);
                m_jcodebar.setText(productcode);
                m_jreference.setText(productref);
                jattributes.setText(null);

                // calculo el precio sugerido para la entrada.
                MovementReason reason = (MovementReason)  m_ReasonModel.getSelectedItem();
                Double dPrice = reason.getPrice(prod.getPriceBuy(), prod.getPriceSell());
                m_jprice.setText(Formats.CURRENCY.formatValue(dPrice));
            }
        }
    }
    
    private void assignProductByCode() {
        try {
            ProductInfoExt oProduct = m_dlSales.getProductInfoByCode(m_jcodebar.getText());
            if (oProduct == null) {       
                assignProduct(null);
                Toolkit.getDefaultToolkit().beep();                   
            } else {
                assignProduct(oProduct);
            }
        } catch (BasicException eData) {        
            assignProduct(null);
            MessageInf msg = new MessageInf(eData);
            msg.show(this);            
        }        
    }
    
    private void assignProductByReference() {
        try {
            ProductInfoExt oProduct = m_dlSales.getProductInfoByReference(m_jreference.getText());
            if (oProduct == null) {       
                assignProduct(null);
                Toolkit.getDefaultToolkit().beep();                   
            } else {
                assignProduct(oProduct);
            }
        } catch (BasicException eData) {        
            assignProduct(null);
            MessageInf msg = new MessageInf(eData);
            msg.show(this);            
        }        
    }
    
    private class CatalogListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            assignProduct((ProductInfoExt) e.getSource());
        }  
    }  
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLblDate = new javax.swing.JLabel();
        m_jdate = new javax.swing.JTextField();
        m_jbtndate = new javax.swing.JButton();
        jLblReason = new javax.swing.JLabel();
        m_jreason = new javax.swing.JComboBox();
        jLblName = new javax.swing.JLabel();
        jproduct = new javax.swing.JTextField();
        jEditProduct = new javax.swing.JButton();
        jLblLocation = new javax.swing.JLabel();
        m_jLocation = new javax.swing.JComboBox();
        jLBCode = new javax.swing.JLabel();
        m_jcodebar = new javax.swing.JTextField();
        m_jEnter = new javax.swing.JButton();
        jLblRef = new javax.swing.JLabel();
        m_jreference = new javax.swing.JTextField();
        m_jEnter1 = new javax.swing.JButton();
        jLblAtt = new javax.swing.JLabel();
        jattributes = new javax.swing.JTextField();
        jEditAttributes = new javax.swing.JButton();
        jLblUnits = new javax.swing.JLabel();
        m_junits = new javax.swing.JTextField();
        jLblPrice = new javax.swing.JLabel();
        m_jprice = new javax.swing.JTextField();
        catcontainer = new javax.swing.JPanel();
        m_jLocationDes = new javax.swing.JComboBox();
        jLblMoveTo = new javax.swing.JLabel();
        m_jSupplier = new javax.swing.JComboBox();
        jLblLocation1 = new javax.swing.JLabel();

        setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        setMinimumSize(new java.awt.Dimension(550, 250));
        setPreferredSize(new java.awt.Dimension(1000, 550));
        setLayout(new java.awt.BorderLayout());

        jPanel1.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jPanel1.setMaximumSize(new java.awt.Dimension(2147483647, 2147483647));
        jPanel1.setMinimumSize(new java.awt.Dimension(780, 260));
        jPanel1.setPreferredSize(new java.awt.Dimension(400, 600));

        jLblDate.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblDate.setText(AppLocal.getIntString("label.stockdate")); // NOI18N
        jLblDate.setMaximumSize(new java.awt.Dimension(23, 20));
        jLblDate.setMinimumSize(new java.awt.Dimension(23, 20));
        jLblDate.setPreferredSize(new java.awt.Dimension(110, 30));

        m_jdate.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jdate.setMinimumSize(new java.awt.Dimension(40, 20));
        m_jdate.setPreferredSize(new java.awt.Dimension(170, 30));

        m_jbtndate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/date.png"))); // NOI18N
        m_jbtndate.setToolTipText("Open Calendar");
        m_jbtndate.setPreferredSize(new java.awt.Dimension(64, 45));
        m_jbtndate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jbtndateActionPerformed(evt);
            }
        });

        jLblReason.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblReason.setText(AppLocal.getIntString("label.stockreason")); // NOI18N
        jLblReason.setMaximumSize(new java.awt.Dimension(36, 20));
        jLblReason.setMinimumSize(new java.awt.Dimension(36, 20));
        jLblReason.setPreferredSize(new java.awt.Dimension(110, 30));

        m_jreason.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jreason.setPreferredSize(new java.awt.Dimension(170, 30));
        m_jreason.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jreasonActionPerformed(evt);
            }
        });

        jLblName.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblName.setText(AppLocal.getIntString("label.prodname")); // NOI18N
        jLblName.setMaximumSize(new java.awt.Dimension(40, 20));
        jLblName.setMinimumSize(new java.awt.Dimension(40, 20));
        jLblName.setPreferredSize(new java.awt.Dimension(110, 30));

        jproduct.setEditable(false);
        jproduct.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jproduct.setText("  ");
        jproduct.setPreferredSize(new java.awt.Dimension(170, 30));
        jproduct.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jproductActionPerformed(evt);
            }
        });

        jEditProduct.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search24.png"))); // NOI18N
        jEditProduct.setToolTipText("Search Product List");
        jEditProduct.setPreferredSize(new java.awt.Dimension(64, 45));
        jEditProduct.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jEditProductActionPerformed(evt);
            }
        });

        jLblLocation.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblLocation.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLblLocation.setText("Location");
        jLblLocation.setPreferredSize(new java.awt.Dimension(110, 30));

        m_jLocation.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jLocation.setPreferredSize(new java.awt.Dimension(170, 30));

        jLBCode.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLBCode.setText(AppLocal.getIntString("label.prodbarcode")); // NOI18N
        jLBCode.setMaximumSize(new java.awt.Dimension(40, 20));
        jLBCode.setMinimumSize(new java.awt.Dimension(40, 20));
        jLBCode.setPreferredSize(new java.awt.Dimension(110, 30));

        m_jcodebar.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jcodebar.setPreferredSize(new java.awt.Dimension(170, 30));
        m_jcodebar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jcodebarActionPerformed(evt);
            }
        });

        m_jEnter.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/barcode.png"))); // NOI18N
        m_jEnter.setToolTipText("Get Barcode");
        m_jEnter.setFocusPainted(false);
        m_jEnter.setFocusable(false);
        m_jEnter.setMaximumSize(new java.awt.Dimension(54, 33));
        m_jEnter.setMinimumSize(new java.awt.Dimension(54, 33));
        m_jEnter.setPreferredSize(new java.awt.Dimension(64, 45));
        m_jEnter.setRequestFocusEnabled(false);
        m_jEnter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jEnterActionPerformed(evt);
            }
        });

        jLblRef.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblRef.setText(AppLocal.getIntString("label.prodref")); // NOI18N
        jLblRef.setMaximumSize(new java.awt.Dimension(40, 20));
        jLblRef.setMinimumSize(new java.awt.Dimension(40, 20));
        jLblRef.setPreferredSize(new java.awt.Dimension(110, 30));

        m_jreference.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jreference.setPreferredSize(new java.awt.Dimension(170, 30));
        m_jreference.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jreferenceActionPerformed(evt);
            }
        });

        m_jEnter1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/products.png"))); // NOI18N
        m_jEnter1.setToolTipText("Enter Product ID");
        m_jEnter1.setFocusPainted(false);
        m_jEnter1.setFocusable(false);
        m_jEnter1.setMaximumSize(new java.awt.Dimension(64, 33));
        m_jEnter1.setMinimumSize(new java.awt.Dimension(64, 33));
        m_jEnter1.setPreferredSize(new java.awt.Dimension(64, 45));
        m_jEnter1.setRequestFocusEnabled(false);
        m_jEnter1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jEnter1ActionPerformed(evt);
            }
        });

        jLblAtt.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblAtt.setText(AppLocal.getIntString("label.attributes")); // NOI18N
        jLblAtt.setMaximumSize(new java.awt.Dimension(48, 20));
        jLblAtt.setMinimumSize(new java.awt.Dimension(48, 20));
        jLblAtt.setPreferredSize(new java.awt.Dimension(110, 30));

        jattributes.setEditable(false);
        jattributes.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jattributes.setPreferredSize(new java.awt.Dimension(170, 30));
        jattributes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jattributesActionPerformed(evt);
            }
        });

        jEditAttributes.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/attributes.png"))); // NOI18N
        jEditAttributes.setToolTipText("Product Attributes");
        jEditAttributes.setMaximumSize(new java.awt.Dimension(65, 33));
        jEditAttributes.setMinimumSize(new java.awt.Dimension(65, 33));
        jEditAttributes.setPreferredSize(new java.awt.Dimension(64, 45));
        jEditAttributes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jEditAttributesActionPerformed(evt);
            }
        });

        jLblUnits.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblUnits.setText(AppLocal.getIntString("label.units")); // NOI18N
        jLblUnits.setMaximumSize(new java.awt.Dimension(40, 20));
        jLblUnits.setMinimumSize(new java.awt.Dimension(40, 20));
        jLblUnits.setPreferredSize(new java.awt.Dimension(110, 30));

        m_junits.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_junits.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        m_junits.setPreferredSize(new java.awt.Dimension(100, 30));

        jLblPrice.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblPrice.setText(AppLocal.getIntString("label.price")); // NOI18N
        jLblPrice.setPreferredSize(new java.awt.Dimension(110, 30));

        m_jprice.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jprice.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        m_jprice.setPreferredSize(new java.awt.Dimension(170, 30));

        catcontainer.setBackground(new java.awt.Color(255, 255, 255));
        catcontainer.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        catcontainer.setMaximumSize(new java.awt.Dimension(2147483647, 2147483647));
        catcontainer.setMinimumSize(new java.awt.Dimension(0, 250));
        catcontainer.setPreferredSize(new java.awt.Dimension(600, 0));
        catcontainer.setLayout(new java.awt.BorderLayout());

        m_jLocationDes.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jLocationDes.setEnabled(false);
        m_jLocationDes.setPreferredSize(new java.awt.Dimension(170, 30));
        m_jLocationDes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jLocationDesActionPerformed(evt);
            }
        });

        jLblMoveTo.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblMoveTo.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("pos_messages"); // NOI18N
        jLblMoveTo.setText(bundle.getString("label.moveto")); // NOI18N
        jLblMoveTo.setEnabled(false);
        jLblMoveTo.setPreferredSize(new java.awt.Dimension(110, 30));

        m_jSupplier.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jSupplier.setPreferredSize(new java.awt.Dimension(170, 30));

        jLblLocation1.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblLocation1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLblLocation1.setText(AppLocal.getIntString("button.exit")); // NOI18N
        jLblLocation1.setPreferredSize(new java.awt.Dimension(110, 30));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLblDate, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(m_jdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(m_jbtndate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLblReason, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLblLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(m_jreason, 0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(m_jLocation, 0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jLblMoveTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLblLocation1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(m_jLocationDes, 0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(m_jSupplier, 0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLblAtt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jattributes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jEditAttributes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLBCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(m_jcodebar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(m_jEnter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLblUnits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(m_junits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLblPrice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(m_jprice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLblName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLblRef, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(m_jreference, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(114, 114, 114)
                                .addComponent(jproduct, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(m_jEnter1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jEditProduct, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(catcontainer, javax.swing.GroupLayout.DEFAULT_SIZE, 612, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(m_jdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLblDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(m_jbtndate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(m_jreason, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLblReason, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(m_jLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLblLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLblMoveTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(m_jLocationDes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(m_jSupplier, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLblLocation1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLblName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jproduct, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jEditProduct, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(m_jreference, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLblRef, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(m_jEnter1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jattributes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLblAtt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jEditAttributes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(m_jEnter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(m_jcodebar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLBCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLblUnits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(m_junits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLblPrice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(m_jprice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(0, 47, Short.MAX_VALUE))
                    .addComponent(catcontainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(48, Short.MAX_VALUE))
        );

        catcontainer.getAccessibleContext().setAccessibleParent(jPanel1);

        add(jPanel1, java.awt.BorderLayout.PAGE_START);
    }// </editor-fold>//GEN-END:initComponents

    private void m_jEnter1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jEnter1ActionPerformed

        assignProductByReference();
        
    }//GEN-LAST:event_m_jEnter1ActionPerformed

    private void m_jreferenceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jreferenceActionPerformed

        assignProductByReference();

    }//GEN-LAST:event_m_jreferenceActionPerformed

    private void m_jcodebarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jcodebarActionPerformed
       
        assignProductByCode();

    }//GEN-LAST:event_m_jcodebarActionPerformed

    private void m_jEnterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jEnterActionPerformed
            
        assignProductByCode();
   
    }//GEN-LAST:event_m_jEnterActionPerformed

    private void jEditAttributesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jEditAttributesActionPerformed

        if (productid == null) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.productnotselected"));
            msg.show(this);
        } else {
            try {
                JProductAttEdit attedit = JProductAttEdit.getAttributesEditor(this, m_App.getSession());
                attedit.editAttributes(attsetid, attsetinstid);
                attedit.setVisible(true);
               
                if (attedit.isOK()) {
                    attsetinstid = attedit.getAttributeSetInst();
                    attsetinstdesc = attedit.getAttributeSetInstDescription();
                    jattributes.setText(attsetinstdesc);
                }
            } catch (BasicException ex) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotfindattributes"), ex);
                msg.show(this);
            }
        }      
}//GEN-LAST:event_jEditAttributesActionPerformed

    private void m_jbtndateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jbtndateActionPerformed
        
        Date date;
        try {
            date = (Date) Formats.TIMESTAMP.parseValue(m_jdate.getText());
        } catch (BasicException e) {
            date = null;
        }        
        date = JCalendarDialog.showCalendarTime(this, date);
        if (date != null) {
            m_jdate.setText(Formats.TIMESTAMP.formatValue(date));
        }
        
    }//GEN-LAST:event_m_jbtndateActionPerformed

    private void jEditProductActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jEditProductActionPerformed
        
        assignProduct(JProductFinder.showMessage(this, m_dlSales));

}//GEN-LAST:event_jEditProductActionPerformed

    private void jattributesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jattributesActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jattributesActionPerformed

    private void m_jLocationDesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jLocationDesActionPerformed
        
    }//GEN-LAST:event_m_jLocationDesActionPerformed

    private void jproductActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jproductActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jproductActionPerformed

    private void m_jreasonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jreasonActionPerformed
/*
* JG May 2017 for FUTURE - moved out of StockManagement
* for time being just let's show a message        
        if (m_ReasonModel.getSelectedItem() == MovementReason.OUT_CROSSING) {
            m_jLocationDes.setEnabled(true);            
            jLblMoveTo.setEnabled(true);
        }else{
            m_jLocationDes.setEnabled(false);            
            jLblMoveTo.setEnabled(false);    
        }
*/
        if (m_ReasonModel.getSelectedItem() == MovementReason.OUT_CROSSING) {        
            JOptionPane.showMessageDialog(this, "Transfer option in development. Please us (In) + (Out) Movement options");
        }
    }//GEN-LAST:event_m_jreasonActionPerformed
    
    /**
     * Aplica mejoras visuales modernas al editor de stock
     */
    private void aplicarMejorasVisuales() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Configurar fuentes modernas
                Font modernFont = new Font("Segoe UI", Font.PLAIN, 12);
                Font labelFont = new Font("Segoe UI", Font.PLAIN, 11);
                Font buttonFont = new Font("Segoe UI", Font.PLAIN, 11);
                
                // Colores modernos
                Color primaryColor = new Color(0, 123, 255);
                Color successColor = new Color(40, 167, 69);
                Color backgroundColor = new Color(248, 249, 250);
                Color borderColor = new Color(222, 226, 230);
                Color textColor = new Color(73, 80, 87);
                
                // Configurar el panel principal
                this.setBackground(backgroundColor);
                
                // Mejorar etiquetas
                configurarLabel(jLblDate, labelFont, textColor);
                configurarLabel(jLblReason, labelFont, textColor);
                configurarLabel(jLblLocation, labelFont, textColor);
                configurarLabel(jLblLocation1, labelFont, textColor);
                configurarLabel(jLblMoveTo, labelFont, textColor);
                configurarLabel(jLBCode, labelFont, textColor);
                configurarLabel(jLblRef, labelFont, textColor);
                configurarLabel(jLblName, labelFont, textColor);
                configurarLabel(jLblAtt, labelFont, textColor);
                configurarLabel(jLblUnits, labelFont, textColor);
                configurarLabel(jLblPrice, labelFont, textColor);
                
                // Mejorar campos de texto
                configurarTextField(m_jdate, modernFont, borderColor);
                configurarTextField(m_jreference, modernFont, borderColor);
                configurarTextField(m_jcodebar, modernFont, borderColor);
                configurarTextField(jproduct, modernFont, borderColor);
                configurarTextField(jattributes, modernFont, borderColor);
                configurarTextField(m_junits, modernFont, borderColor);
                configurarTextField(m_jprice, modernFont, borderColor);
                
                // Mejorar botones
                configurarBoton(m_jbtndate, buttonFont, primaryColor);
                configurarBoton(jEditProduct, buttonFont, primaryColor);
                configurarBoton(jEditAttributes, buttonFont, primaryColor);
                configurarBoton(m_jEnter, buttonFont, successColor);
                configurarBoton(m_jEnter1, buttonFont, successColor);
                
                // Mejorar comboboxes
                configurarComboBox(m_jreason, modernFont, borderColor);
                configurarComboBox(m_jLocation, modernFont, borderColor);
                configurarComboBox(m_jLocationDes, modernFont, borderColor);
                configurarComboBox(m_jSupplier, modernFont, borderColor);
                
                // Mejorar paneles
                if (jPanel1 != null) {
                    jPanel1.setBackground(backgroundColor);
                }
                if (catcontainer != null) {
                    catcontainer.setBackground(backgroundColor);
                }
                
            } catch (Exception e) {
                // En caso de error, continuar sin aplicar mejoras
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Configura un label con estilo moderno
     */
    private void configurarLabel(JLabel label, Font font, Color color) {
        if (label != null) {
            label.setFont(font);
            label.setForeground(color);
        }
    }
    
    /**
     * Configura un campo de texto con estilo moderno
     */
    private void configurarTextField(JTextField field, Font font, Color borderColor) {
        if (field != null) {
            field.setFont(font);
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
            ));
        }
    }
    
    /**
     * Configura un botón con estilo moderno
     */
    private void configurarBoton(JButton button, Font font, Color backgroundColor) {
        if (button != null) {
            button.setFont(font);
            button.setBackground(backgroundColor);
            button.setForeground(Color.WHITE);
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(backgroundColor, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
            button.setFocusPainted(false);
            button.setOpaque(true);
        }
    }
    
    /**
     * Configura un combobox con estilo moderno
     */
    private void configurarComboBox(JComboBox comboBox, Font font, Color borderColor) {
        if (comboBox != null) {
            comboBox.setFont(font);
            comboBox.setBorder(BorderFactory.createLineBorder(borderColor, 1));
        }
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel catcontainer;
    private javax.swing.JButton jEditAttributes;
    private javax.swing.JButton jEditProduct;
    private javax.swing.JLabel jLBCode;
    private javax.swing.JLabel jLblAtt;
    private javax.swing.JLabel jLblDate;
    private javax.swing.JLabel jLblLocation;
    private javax.swing.JLabel jLblLocation1;
    private javax.swing.JLabel jLblMoveTo;
    private javax.swing.JLabel jLblName;
    private javax.swing.JLabel jLblPrice;
    private javax.swing.JLabel jLblReason;
    private javax.swing.JLabel jLblRef;
    private javax.swing.JLabel jLblUnits;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField jattributes;
    private javax.swing.JTextField jproduct;
    private javax.swing.JButton m_jEnter;
    private javax.swing.JButton m_jEnter1;
    private javax.swing.JComboBox m_jLocation;
    private javax.swing.JComboBox m_jLocationDes;
    private javax.swing.JComboBox m_jSupplier;
    private javax.swing.JButton m_jbtndate;
    private javax.swing.JTextField m_jcodebar;
    private javax.swing.JTextField m_jdate;
    private javax.swing.JTextField m_jprice;
    private javax.swing.JComboBox m_jreason;
    private javax.swing.JTextField m_jreference;
    private javax.swing.JTextField m_junits;
    // End of variables declaration//GEN-END:variables
    
    /**
     * Aplica mejoras estructurales avanzadas para crear un diseño profesional
     */
    private void aplicarMejorasEstructurales() {
        try {
            // Configurar fuente profesional
            Font fonteProfissional = new Font("Segoe UI", Font.PLAIN, 12);
            Font fonteLabel = new Font("Segoe UI", Font.BOLD, 11);
            Font fonteTitulo = new Font("Segoe UI", Font.BOLD, 14);
            
            // Colores profesionales
            Color corFondoPrimario = new Color(248, 249, 250);
            Color corBordePrimario = new Color(223, 225, 229);
            Color corTexto = new Color(33, 37, 41);
            Color corAcento = new Color(0, 123, 255);
            
            // Aplicar mejoras al panel principal
            Component[] todosComponentes = getAllComponents(this);
            
            for (Component comp : todosComponentes) {
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    panel.setBackground(corFondoPrimario);
                    
                    // Mejorar bordes de paneles
                    if (panel.getBorder() != null) {
                        panel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(corBordePrimario, 1),
                            BorderFactory.createEmptyBorder(12, 12, 12, 12)
                        ));
                    }
                    
                } else if (comp instanceof JLabel) {
                    JLabel label = (JLabel) comp;
                    label.setFont(fonteLabel);
                    label.setForeground(corTexto);
                    
                } else if (comp instanceof JTextField) {
                    JTextField field = (JTextField) comp;
                    field.setFont(fonteProfissional);
                    field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(corBordePrimario, 1),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8)
                    ));
                    field.setBackground(Color.WHITE);
                    
                } else if (comp instanceof JComboBox) {
                    JComboBox combo = (JComboBox) comp;
                    combo.setFont(fonteProfissional);
                    combo.setBorder(BorderFactory.createLineBorder(corBordePrimario, 1));
                    combo.setBackground(Color.WHITE);
                    
                } else if (comp instanceof JButton) {
                    JButton button = (JButton) comp;
                    button.setFont(fonteProfissional);
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(corAcento, 1),
                        BorderFactory.createEmptyBorder(6, 12, 6, 12)
                    ));
                    button.setBackground(Color.WHITE);
                    button.setForeground(corAcento);
                    button.setFocusPainted(false);
                    
                    // Efecto hover
                    button.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseEntered(java.awt.event.MouseEvent evt) {
                            button.setBackground(corAcento);
                            button.setForeground(Color.WHITE);
                        }
                        
                        @Override
                        public void mouseExited(java.awt.event.MouseEvent evt) {
                            button.setBackground(Color.WHITE);
                            button.setForeground(corAcento);
                        }
                    });
                }
            }
            
            // Configurar espaciado y layout profesional
            if (this.getLayout() instanceof BorderLayout) {
                this.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            }
            
        // Aplicar mejoras específicas a componentes principales
        aplicarMejorasComponentesEspecificos(fonteTitulo, corAcento);
        
        // Ocultar el catálogo/teclado de productos
        ocultarCatalogoProductos();        } catch (Exception e) {
            // Si hay error, continúa sin aplicar mejoras
            System.err.println("Error aplicando mejoras estructurales: " + e.getMessage());
        }
    }
    
    /**
     * Aplica mejoras específicas a componentes principales
     */
    private void aplicarMejorasComponentesEspecificos(Font fonteTitulo, Color corAcento) {
        // Mejorar campos específicos del editor de stock
        if (m_jdate != null) {
            m_jdate.setToolTipText("Fecha de la transacción");
        }
        
        if (m_jreason != null) {
            m_jreason.setToolTipText("Motivo del movimiento de stock");
        }
        
        if (m_jLocation != null) {
            m_jLocation.setToolTipText("Ubicación de origen");
        }
        
        if (m_jLocationDes != null) {
            m_jLocationDes.setToolTipText("Ubicación de destino");
        }
        
        if (jproduct != null) {
            jproduct.setToolTipText("Código del producto");
        }
        
        if (m_junits != null) {
            m_junits.setToolTipText("Cantidad de unidades");
        }
        
        if (m_jprice != null) {
            m_jprice.setToolTipText("Precio unitario");
        }
        
        if (m_jSupplier != null) {
            m_jSupplier.setToolTipText("Proveedor del producto");
        }
    }
    
    /**
     * Ocultar el catálogo/teclado de productos
     */
    private void ocultarCatalogoProductos() {
        try {
            if (catcontainer != null) {
                catcontainer.setVisible(false);
                catcontainer.setPreferredSize(new java.awt.Dimension(0, 0));
                catcontainer.setMaximumSize(new java.awt.Dimension(0, 0));
                catcontainer.setMinimumSize(new java.awt.Dimension(0, 0));
                
                // Revalidar el layout para aplicar los cambios
                if (this.getParent() != null) {
                    this.getParent().revalidate();
                    this.getParent().repaint();
                }
                this.revalidate();
                this.repaint();
            }
        } catch (Exception e) {
            // Si hay error, continúa sin ocultar
            System.err.println("Error ocultando catálogo: " + e.getMessage());
        }
    }
    
    /**
     * Obtener todos los componentes recursivamente
     */
    private Component[] getAllComponents(Container container) {
        java.util.List<Component> allComponents = new java.util.ArrayList<>();
        addComponentsRecursively(container, allComponents);
        return allComponents.toArray(new Component[0]);
    }
    
    /**
     * Agregar componentes recursivamente
     */
    private void addComponentsRecursively(Container container, java.util.List<Component> allComponents) {
        Component[] components = container.getComponents();
        
        for (Component comp : components) {
            allComponents.add(comp);
            
            if (comp instanceof Container) {
                addComponentsRecursively((Container) comp, allComponents);
            }
        }
    }
    
}
