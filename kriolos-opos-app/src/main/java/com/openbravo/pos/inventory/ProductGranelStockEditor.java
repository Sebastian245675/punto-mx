/*
 * ProductGranelStockEditor.java
 * 
 * Editor especializado para manejo de stock decimal en productos a granel
 * Permite entrada y visualización de stock con decimales (ej: 100.5 kg)
 * Diseñado para complementar el modal JGranelDialog existente
 */
package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.PreparedSentence;
import com.openbravo.data.loader.SerializerWriteBasicExt;
import com.openbravo.data.user.DirtyManager;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Editor especializado para productos a granel que permite
 * manejo de stock con decimales de forma intuitiva
 */
public class ProductGranelStockEditor extends JPanel implements EditorRecord {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000");
    
    private AppView app;
    private Object id;
    private Object prodid;
    private String prodref;
    private String prodname;
    private Object location;
    private boolean isGranelProduct = false;
    
    // Componentes UI
    private JLabel m_jTitle;
    private JTextField m_jStockActual;
    private JTextField m_jStockMinimo;
    private JTextField m_jStockMaximo;
    private JTextField m_jAjusteStock;
    private JLabel m_jUnidadMedida;
    private JButton m_jBtnAplicarAjuste;
    private JCheckBox m_jEsGranel;
    private JLabel m_jInfoStock;
    
    /**
     * Constructor
     */
    public ProductGranelStockEditor(AppView app, DirtyManager dirty) {
        this.app = app;
        initComponents();
        setupEvents(dirty);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(250, 250, 250));
        
        // Panel título
        JPanel panelTitulo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelTitulo.setBackground(new Color(250, 250, 250));
        m_jTitle = new JLabel("Stock de Producto");
        m_jTitle.setFont(new Font("Arial", Font.BOLD, 16));
        m_jTitle.setForeground(new Color(66, 66, 66));
        panelTitulo.add(m_jTitle);
        
        // Panel principal con información de stock
        JPanel panelPrincipal = new JPanel(new GridBagLayout());
        panelPrincipal.setBackground(Color.WHITE);
        panelPrincipal.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(224, 224, 224), 1),
                "Gestión de Stock para Productos a Granel",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 14),
                new Color(66, 66, 66)
            ),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Checkbox para marcar como granel
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        m_jEsGranel = new JCheckBox("Es producto a granel (permite decimales)");
        m_jEsGranel.setFont(new Font("Arial", Font.BOLD, 13));
        m_jEsGranel.setForeground(new Color(76, 175, 80));
        m_jEsGranel.setBackground(Color.WHITE);
        panelPrincipal.add(m_jEsGranel, gbc);
        
        // Información de stock actual
        gbc.gridwidth = 1;
        gbc.gridy++;
        JLabel lblStockActual = new JLabel("Stock Actual:");
        lblStockActual.setFont(new Font("Arial", Font.BOLD, 13));
        panelPrincipal.add(lblStockActual, gbc);
        
        gbc.gridx = 1;
        m_jStockActual = new JTextField(12);
        m_jStockActual.setFont(new Font("Consolas", Font.BOLD, 14));
        m_jStockActual.setHorizontalAlignment(JTextField.RIGHT);
        m_jStockActual.setEditable(false);
        m_jStockActual.setBackground(new Color(245, 245, 245));
        panelPrincipal.add(m_jStockActual, gbc);
        
        gbc.gridx = 2;
        m_jUnidadMedida = new JLabel("unidades");
        m_jUnidadMedida.setFont(new Font("Arial", Font.PLAIN, 12));
        m_jUnidadMedida.setForeground(new Color(120, 120, 120));
        panelPrincipal.add(m_jUnidadMedida, gbc);
        
        // Stock mínimo
        gbc.gridx = 0; gbc.gridy++;
        JLabel lblStockMin = new JLabel("Stock Mínimo:");
        lblStockMin.setFont(new Font("Arial", Font.PLAIN, 13));
        panelPrincipal.add(lblStockMin, gbc);
        
        gbc.gridx = 1;
        m_jStockMinimo = new JTextField(12);
        m_jStockMinimo.setFont(new Font("Consolas", Font.PLAIN, 14));
        m_jStockMinimo.setHorizontalAlignment(JTextField.RIGHT);
        setupDecimalFilter(m_jStockMinimo);
        panelPrincipal.add(m_jStockMinimo, gbc);
        
        // Stock máximo
        gbc.gridx = 0; gbc.gridy++;
        JLabel lblStockMax = new JLabel("Stock Máximo:");
        lblStockMax.setFont(new Font("Arial", Font.PLAIN, 13));
        panelPrincipal.add(lblStockMax, gbc);
        
        gbc.gridx = 1;
        m_jStockMaximo = new JTextField(12);
        m_jStockMaximo.setFont(new Font("Consolas", Font.PLAIN, 14));
        m_jStockMaximo.setHorizontalAlignment(JTextField.RIGHT);
        setupDecimalFilter(m_jStockMaximo);
        panelPrincipal.add(m_jStockMaximo, gbc);
        
        // Separador visual
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        JSeparator sep = new JSeparator();
        sep.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        panelPrincipal.add(sep, gbc);
        
        // Ajuste de stock
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        gbc.gridy++;
        JLabel lblAjuste = new JLabel("Ajuste de Stock:");
        lblAjuste.setFont(new Font("Arial", Font.BOLD, 13));
        lblAjuste.setForeground(new Color(255, 87, 34));
        panelPrincipal.add(lblAjuste, gbc);
        
        gbc.gridx = 1;
        m_jAjusteStock = new JTextField(12);
        m_jAjusteStock.setFont(new Font("Consolas", Font.BOLD, 14));
        m_jAjusteStock.setHorizontalAlignment(JTextField.RIGHT);
        m_jAjusteStock.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 87, 34), 2),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        setupDecimalFilter(m_jAjusteStock);
        panelPrincipal.add(m_jAjusteStock, gbc);
        
        gbc.gridx = 2;
        m_jBtnAplicarAjuste = new JButton("Aplicar");
        m_jBtnAplicarAjuste.setFont(new Font("Arial", Font.BOLD, 12));
        m_jBtnAplicarAjuste.setBackground(new Color(76, 175, 80));
        m_jBtnAplicarAjuste.setForeground(Color.WHITE);
        m_jBtnAplicarAjuste.setFocusPainted(false);
        m_jBtnAplicarAjuste.setBorderPainted(false);
        m_jBtnAplicarAjuste.setCursor(new Cursor(Cursor.HAND_CURSOR));
        panelPrincipal.add(m_jBtnAplicarAjuste, gbc);
        
        // Panel información
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        m_jInfoStock = new JLabel("<html><div style='padding:10px; color:#666;'>" +
                "💡 <b>Tip:</b> Para productos a granel puedes usar decimales como 100.5 kg. " +
                "El sistema calculará automáticamente los precios en el modal de ventas.</div></html>");
        m_jInfoStock.setFont(new Font("Arial", Font.PLAIN, 11));
        m_jInfoStock.setOpaque(true);
        m_jInfoStock.setBackground(new Color(255, 248, 225));
        m_jInfoStock.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 193, 7), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        panelPrincipal.add(m_jInfoStock, gbc);
        
        add(panelTitulo, BorderLayout.NORTH);
        add(panelPrincipal, BorderLayout.CENTER);
    }
    
    private void setupDecimalFilter(JTextField field) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DecimalDocumentFilter());
        
        // Efecto visual al obtener foco
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.selectAll();
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(63, 81, 181), 2),
                    BorderFactory.createEmptyBorder(3, 8, 3, 8)
                ));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(224, 224, 224), 1),
                    BorderFactory.createEmptyBorder(3, 8, 3, 8)
                ));
            }
        });
    }
    
    private void setupEvents(DirtyManager dirty) {
        // Eventos de cambio para marcar como dirty
        if (dirty != null) {
            m_jStockMinimo.getDocument().addDocumentListener(dirty);
            m_jStockMaximo.getDocument().addDocumentListener(dirty);
            m_jEsGranel.addActionListener(e -> dirty.setDirty(true));
        }
        
        // Evento para cambio de modo granel
        m_jEsGranel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGranelMode();
            }
        });
        
        // Evento para aplicar ajuste de stock
        m_jBtnAplicarAjuste.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                aplicarAjusteStock();
            }
        });
    }
    
    private void updateGranelMode() {
        isGranelProduct = m_jEsGranel.isSelected();
        
        if (isGranelProduct) {
            m_jUnidadMedida.setText("kg");
            m_jInfoStock.setVisible(true);
            
            // Cambiar colores para modo granel
            m_jStockActual.setBackground(new Color(227, 242, 253));
            m_jStockMinimo.setBackground(new Color(255, 243, 224));
            m_jStockMaximo.setBackground(new Color(255, 243, 224));
        } else {
            m_jUnidadMedida.setText("unidades");
            m_jInfoStock.setVisible(false);
            
            // Colores normales
            m_jStockActual.setBackground(new Color(245, 245, 245));
            m_jStockMinimo.setBackground(Color.WHITE);
            m_jStockMaximo.setBackground(Color.WHITE);
        }
        
        revalidate();
        repaint();
    }
    
    private void aplicarAjusteStock() {
        try {
            String textoAjuste = m_jAjusteStock.getText().trim();
            if (textoAjuste.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Ingrese la cantidad a ajustar",
                    "Ajuste de Stock",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            double ajuste = Double.parseDouble(textoAjuste);
            double stockActual = getStockActual();
            double nuevoStock = stockActual + ajuste;
            
            if (nuevoStock < 0) {
                int opcion = JOptionPane.showConfirmDialog(this,
                    String.format("El ajuste resultará en stock negativo (%.3f).\n¿Desea continuar?", nuevoStock),
                    "Stock Negativo",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                
                if (opcion != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            // Aplicar el ajuste en base de datos
            aplicarAjusteEnBD(ajuste);
            
            // Actualizar display
            m_jStockActual.setText(formatearStock(nuevoStock));
            m_jAjusteStock.setText("");
            
            // Mensaje de confirmación
            String mensaje = ajuste >= 0 
                ? String.format("✅ Stock incrementado en %.3f %s", ajuste, m_jUnidadMedida.getText())
                : String.format("⚠️ Stock reducido en %.3f %s", Math.abs(ajuste), m_jUnidadMedida.getText());
                
            JOptionPane.showMessageDialog(this, mensaje, "Ajuste Aplicado", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Formato de número inválido",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error al aplicar ajuste: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void aplicarAjusteEnBD(double ajuste) throws BasicException {
        if (prodid == null || location == null) {
            throw new BasicException("Producto o ubicación no definidos");
        }
        
        // Actualizar stock en stockcurrent
        PreparedSentence sentence = new PreparedSentence(app.getSession(),
            "UPDATE stockcurrent SET UNITS = (UNITS + ?) WHERE PRODUCT = ? AND LOCATION = ?",
            new SerializerWriteBasicExt(new Datas[]{
                Datas.DOUBLE,
                Datas.STRING,
                Datas.STRING
            }, new int[]{0, 1, 2}));
        
        Object[] params = new Object[]{ajuste, prodid, location};
        sentence.exec(params);
        
        // TODO: Aquí podrías agregar registro en stockdiary para auditoría
        // registrarMovimientoStock(ajuste, "Ajuste manual desde editor");
    }
    
    private double getStockActual() {
        try {
            String texto = m_jStockActual.getText().trim();
            return texto.isEmpty() ? 0.0 : Double.parseDouble(texto.replace(",", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private String formatearStock(double stock) {
        return isGranelProduct ? DECIMAL_FORMAT.format(stock) : String.valueOf((int)stock);
    }

    // Implementación de EditorRecord
    @Override
    public void writeValueEOF() {
        m_jTitle.setText("Fin de registros");
        id = null;
        prodid = null;
        prodref = null;
        prodname = null;
        location = null;
        m_jStockActual.setText(null);
        m_jStockMinimo.setText(null);
        m_jStockMaximo.setText(null);
        m_jAjusteStock.setText(null);
        m_jEsGranel.setSelected(false);
        updateGranelMode();
    }

    @Override
    public void writeValueInsert() {
        m_jTitle.setText("Nuevo registro de stock");
        id = null;
        prodid = null;
        prodref = null;
        prodname = null;
        location = null;
        m_jStockActual.setText("0.000");
        m_jStockMinimo.setText("0.000");
        m_jStockMaximo.setText("100.000");
        m_jAjusteStock.setText("");
        m_jEsGranel.setSelected(false);
        updateGranelMode();
    }

    @Override
    public void writeValueEdit(Object value) {
        Object[] data = (Object[]) value;
        id = data[0];
        prodid = data[1];
        prodref = (String) data[2];
        prodname = (String) data[3];
        location = data[4];
        
        m_jTitle.setText(String.format("%s - %s", prodref, prodname));
        
        // Stock actual (data[7] basado en ProductsWarehousePanel)
        double stockActual = data[7] != null ? ((Double) data[7]).doubleValue() : 0.0;
        m_jStockActual.setText(formatearStock(stockActual));
        
        // Stock mínimo y máximo (data[5] y data[6])
        double stockMin = data[5] != null ? ((Double) data[5]).doubleValue() : 0.0;
        double stockMax = data[6] != null ? ((Double) data[6]).doubleValue() : 100.0;
        
        m_jStockMinimo.setText(DECIMAL_FORMAT.format(stockMin));
        m_jStockMaximo.setText(DECIMAL_FORMAT.format(stockMax));
        
        // Detectar si es producto a granel basado en si tiene decimales en stock
        isGranelProduct = (stockActual % 1 != 0) || (stockMin % 1 != 0) || (stockMax % 1 != 0);
        m_jEsGranel.setSelected(isGranelProduct);
        updateGranelMode();
        
        m_jAjusteStock.setText("");
    }

    @Override
    public void writeValueDelete(Object value) {
        writeValueEdit(value);
    }

    @Override
    public Object createValue() throws BasicException {
        return new Object[]{
            id,
            prodid,
            prodref,
            prodname,
            location,
            Formats.DOUBLE.parseValue(m_jStockMinimo.getText()),
            Formats.DOUBLE.parseValue(m_jStockMaximo.getText()),
            Formats.DOUBLE.parseValue(m_jStockActual.getText())
        };
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void refresh() {
        // Recargar datos si es necesario
    }
    
    /**
     * Filtro para permitir solo entrada de números decimales
     */
    private static class DecimalDocumentFilter extends DocumentFilter {
        private static final String DECIMAL_PATTERN = "^\\d*\\.?\\d{0,3}$";
        
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            
            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
            String newText = currentText.substring(0, offset) + string + currentText.substring(offset);
            
            if (newText.matches(DECIMAL_PATTERN) || newText.isEmpty()) {
                super.insertString(fb, offset, string, attr);
            }
        }
        
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            
            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
            String newText = currentText.substring(0, offset) + text + currentText.substring(offset + length);
            
            if (newText.matches(DECIMAL_PATTERN) || newText.isEmpty()) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }
}