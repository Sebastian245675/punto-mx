/*
 * ProductsGranelPanel.java
 * 
 * Panel especializado para gestión de productos a granel
 * Integra el nuevo ProductGranelStockEditor para manejo de stock decimal
 */
package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.JMessageDialog;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.*;
import com.openbravo.data.user.*;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.panels.JPanelTable2;
import com.openbravo.pos.sales.JGranelDialog;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.UUID;
import javax.swing.*;

/**
 * Panel especializado para gestión de productos a granel
 * con soporte completo para stock decimal
 */
public class ProductsGranelPanel extends JPanelTable2 {

    private JParamsLocation m_paramslocation;    
    private ProductGranelStockEditor jeditor;
    private DataLogicSales dlSales;
    private JButton btnTestGranel;
    
    public ProductsGranelPanel() {
    }

    @Override
    protected void init() {   
        dlSales = (DataLogicSales) app.getBean("com.openbravo.pos.forms.DataLogicSales");
        
        // Filtro por ubicación
        m_paramslocation = new JParamsLocation();
        m_paramslocation.init(app);
        m_paramslocation.addActionListener(new ReloadActionListener());

        // Definir estructura de datos
        row = new Row(
                new Field("ID", Datas.STRING, Formats.STRING),
                new Field("PRODUCT_ID", Datas.STRING, Formats.STRING),
                new Field(AppLocal.getIntString("label.prodref"), Datas.STRING, Formats.STRING, true, true, true),
                new Field(AppLocal.getIntString("label.prodname"), Datas.STRING, Formats.STRING, true, true, true),
                new Field("LOCATION", Datas.STRING, Formats.STRING),
                new Field("STOCKSECURITY", Datas.DOUBLE, Formats.DOUBLE),
                new Field("STOCKMAXIMUM", Datas.DOUBLE, Formats.DOUBLE),
                new Field("UNITS", Datas.DOUBLE, Formats.DOUBLE)
        );

        // Query para obtener productos a granel (con ISSCALE = true)
        lpr = new ListProviderCreator(new PreparedSentence<Object[], Object[]>(app.getSession(),
            "SELECT " +
                "stocklevel.ID, " +
                "stocklevel.PRODUCT, " +
                "products.REFERENCE, " +
                "products.NAME, " +
                "stocklevel.LOCATION, " +
                "stocklevel.STOCKSECURITY, " +
                "stocklevel.STOCKMAXIMUM, " +
                "COALESCE(stockcurrent.UNITS, 0) " +
            "FROM products " +
                "JOIN stocklevel ON stocklevel.PRODUCT = products.ID " +
                "LEFT JOIN stockcurrent ON (stockcurrent.PRODUCT = stocklevel.PRODUCT AND stockcurrent.LOCATION = stocklevel.LOCATION) " +
            "WHERE stocklevel.LOCATION = ? AND products.ISSCALE = true " +
            "ORDER BY products.NAME",
            new SerializerWriteBasicExt(new Datas[] {Datas.OBJECT, Datas.STRING}, new int[]{1, 1}),
            new GranelSerializerRead()
        ), m_paramslocation);
        
        // Operaciones de guardado
        SentenceExec updatesent = new SentenceExecTransaction(app.getSession()) {
            @Override
            public int execInTransaction(Object[] params) throws BasicException {
                Object[] values = params;
                if (values[0] == null) {
                    // INSERT
                    values[0] = UUID.randomUUID().toString();
                    return new PreparedSentence(app.getSession(),
                        "INSERT INTO stocklevel (ID, LOCATION, PRODUCT, STOCKSECURITY, STOCKMAXIMUM) VALUES (?, ?, ?, ?, ?)",
                        new SerializerWriteBasicExt(row.getDatas(), new int[] {0, 4, 1, 5, 6})).exec(params);
                } else {
                    // UPDATE
                    return new PreparedSentence(app.getSession(),
                        "UPDATE stocklevel SET STOCKSECURITY = ?, STOCKMAXIMUM = ? WHERE ID = ?",
                        new SerializerWriteBasicExt(row.getDatas(), new int[] {5, 6, 0})).exec(params);
                }
            }
        };     
        
        spr = new DefaultSaveProvider(updatesent, null, null);
        jeditor = new ProductGranelStockEditor(app, dirty);   
        
        // Agregar botón de test para el modal
        createTestButton();
    }
    
    private void createTestButton() {
        btnTestGranel = new JButton("🧪 Probar Modal Granel");
        btnTestGranel.setFont(new Font("Arial", Font.BOLD, 12));
        btnTestGranel.setBackground(new Color(63, 81, 181));
        btnTestGranel.setForeground(Color.WHITE);
        btnTestGranel.setFocusPainted(false);
        btnTestGranel.setBorderPainted(false);
        btnTestGranel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTestGranel.setToolTipText("Probar el modal de entrada de peso para productos a granel");
        
        btnTestGranel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                probarModalGranel();
            }
        });
    }
    
    private void probarModalGranel() {
        try {
            // Mostrar el modal con un precio de ejemplo
            Double peso = JGranelDialog.mostrarDialogo(
                SwingUtilities.getWindowAncestor(this),
                25.50 // Precio por kilo de ejemplo
            );
            
            if (peso != null) {
                String mensaje = String.format(
                    "✅ Modal funcionando correctamente\n\n" +
                    "Peso ingresado: %.3f kg\n" +
                    "Precio por kg: $25.50\n" +
                    "Total calculado: $%.2f",
                    peso, peso * 25.50
                );
                
                JOptionPane.showMessageDialog(this, mensaje, 
                    "Test Modal Granel", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Modal cancelado por el usuario", 
                    "Test Modal Granel", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JMessageDialog.showMessage(this, 
                new MessageInf(MessageInf.SGN_WARNING, "Error en modal granel", ex));
        }
    }

    @Override
    public Component getFilter() {
        // Panel que incluye el filtro de ubicación y el botón de test
        JPanel filterPanel = new JPanel(new BorderLayout(10, 0));
        filterPanel.add(m_paramslocation.getComponent(), BorderLayout.CENTER);
        filterPanel.add(btnTestGranel, BorderLayout.EAST);
        return filterPanel;
    }

    @Override
    public EditorRecord getEditor() {
        return jeditor;
    }

    @Override
    public void activate() throws BasicException {
        m_paramslocation.activate(); 
        super.activate();
    }

    @Override
    public String getTitle() {
        return "📦 Productos a Granel - Gestión de Stock Decimal";
    }      
    
    private class ReloadActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                ProductsGranelPanel.this.bd.actionLoad();
            } catch (BasicException w) {
                // Error silencioso para el reload
            }
        }
    }
    
    /**
     * Serializador especializado para productos a granel
     */
    private static class GranelSerializerRead implements SerializerRead<Object[]> {
        @Override
        public Object[] readValues(DataRead dr) throws BasicException {
            return new Object[] {
                dr.getString(1),  // ID
                dr.getString(2),  // PRODUCT_ID  
                dr.getString(3),  // REFERENCE
                dr.getString(4),  // NAME
                dr.getString(5),  // LOCATION
                dr.getDouble(6),  // STOCKSECURITY
                dr.getDouble(7),  // STOCKMAXIMUM
                dr.getDouble(8)   // UNITS (current stock)
            };
        }
    }
}