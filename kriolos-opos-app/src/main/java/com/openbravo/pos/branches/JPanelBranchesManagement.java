//    KriolOS POS
//    Copyright (c) 2019-2023 KriolOS

package com.openbravo.pos.branches;

import com.openbravo.pos.forms.AppView;
import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.DataRead;
import com.openbravo.data.loader.PreparedSentence;
import com.openbravo.data.loader.SerializerRead;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import com.openbravo.pos.forms.JPanelView;
import javax.swing.JComponent;
import com.openbravo.pos.supabase.SupabaseServiceREST;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;

public class JPanelBranchesManagement extends JPanel implements JPanelView, BeanFactoryApp {

    private AppView m_App;
    private Session m_session;
    private JTabbedPane jTabbedPane1;
    private JPanel jPanelSearchBranch;
    private JPanel jPanelSales;
    private JPanel jPanelProducts;
    private JPanel jPanelCashClosures;
    private JTextField jTextFieldBranchId;
    private JTable jTableBranches;
    private DefaultTableModel modelBranches;
    private JTable jTableSales;
    private DefaultTableModel modelSales;
    private JTable jTableProducts;
    private DefaultTableModel modelProducts;
    private JTable jTableCashClosures;
    private DefaultTableModel modelCashClosures;

    public JPanelBranchesManagement() {
        initComponents();
    }

    @Override
    public void init(AppView app) throws BeanFactoryException {
        m_App = app;
        m_session = app.getSession();
    }

    @Override
    public Object getBean() {
        return this;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public String getTitle() {
        return "Administrar Sucursales";
    }

    @Override
    public void activate() throws BasicException {
        searchBranches(); // Load all branches initially
        loadSales();
        loadProducts();
        loadCashClosures();
    }

    @Override
    public boolean deactivate() {
        return true;
    }
    
    private void initComponents() {
        jTabbedPane1 = new JTabbedPane();
        jPanelSearchBranch = new JPanel();
        jPanelSales = new JPanel();
        jPanelProducts = new JPanel();
        jPanelCashClosures = new JPanel();
        jTextFieldBranchId = new JTextField(20);
        String[] branchColumns = {"ID", "Nombre", "Dirección"};
        modelBranches = new DefaultTableModel(branchColumns, 0);
        jTableBranches = new JTable(modelBranches);

        String[] salesColumns = {"ID Venta", "Fecha", "Total", "Sucursal/Caja"};
        modelSales = new DefaultTableModel(salesColumns, 0);
        jTableSales = new JTable(modelSales);

        String[] productColumns = {"ID Producto", "Nombre", "Precio", "Sucursal", "Stock"};
        modelProducts = new DefaultTableModel(productColumns, 0);
        jTableProducts = new JTable(modelProducts);

        String[] cashClosureColumns = {"ID Cierre", "Fecha Inicio", "Fecha Fin", "Efectivo", "Tarjeta", "Total", "Sucursal"};
        modelCashClosures = new DefaultTableModel(cashClosureColumns, 0);
        jTableCashClosures = new JTable(modelCashClosures);

        setLayout(new BorderLayout());

        // Panel Buscar Sucursal
        JPanel searchPanel = new JPanel(new FlowLayout());
        searchPanel.add(new JLabel("Buscar por ID de Sucursal:"));
        searchPanel.add(jTextFieldBranchId);
        
        JButton searchButton = new JButton("Buscar");
        searchButton.addActionListener(e -> {
            searchBranches();
        });
        searchPanel.add(searchButton);

        jPanelSearchBranch.setLayout(new BorderLayout());
        jPanelSearchBranch.add(searchPanel, BorderLayout.NORTH);
        jPanelSearchBranch.add(new JScrollPane(jTableBranches), BorderLayout.CENTER);

        jTabbedPane1.addTab("Buscar Sucursal", jPanelSearchBranch);

        // Panel Ver Ventas
        jPanelSales.setLayout(new BorderLayout());
        jPanelSales.add(new JScrollPane(jTableSales), BorderLayout.CENTER);
        jTabbedPane1.addTab("Ver Ventas", jPanelSales);

        // Panel Ver Productos
        jPanelProducts.setLayout(new BorderLayout());
        jPanelProducts.add(new JScrollPane(jTableProducts), BorderLayout.CENTER);
        jTabbedPane1.addTab("Ver Productos", jPanelProducts);

        // Panel Ver Cierres de Caja
        jPanelCashClosures.setLayout(new BorderLayout());
        jPanelCashClosures.add(new JScrollPane(jTableCashClosures), BorderLayout.CENTER);
        jTabbedPane1.addTab("Ver Cierres de Caja", jPanelCashClosures);

        add(jTabbedPane1, BorderLayout.CENTER);
    }

    private void searchBranches() {
        String branchId = jTextFieldBranchId.getText().trim();
        modelBranches.setRowCount(0); // Clear previous results

        try {
            String sql = "SELECT ID, NAME, ADDRESS FROM LOCATIONS ";
            if (!branchId.isEmpty()) {
                sql += "WHERE ID = ?";
                for (Object obj : new PreparedSentence<String, Object[]>(m_session, sql, com.openbravo.data.loader.SerializerWriteString.INSTANCE, new SerializerRead<Object[]>() {
                    @Override
                    public Object[] readValues(DataRead dr) throws BasicException {
                        Object[] row = new Object[3];
                        row[0] = dr.getString(1);
                        row[1] = dr.getString(2);
                        row[2] = dr.getString(3);
                        return row;
                    }
                }).list(branchId)) {
                    modelBranches.addRow((Object[]) obj);
                }
            } else {
                new PreparedSentence<Object, Object[]>(m_session, sql, null, new SerializerRead<Object[]>() {
                    @Override
                    public Object[] readValues(DataRead dr) throws BasicException {
                        Object[] row = new Object[3];
                        row[0] = dr.getString(1);
                        row[1] = dr.getString(2);
                        row[2] = dr.getString(3);
                        return row;
                    }
                }).list().forEach(row -> modelBranches.addRow((Object[]) row));
            }

        } catch (BasicException ex) {
            ex.printStackTrace();
            // TODO: Handle exception gracefully, perhaps show an error message to the user
        }
    }

    private void loadSales() throws BasicException {
        modelSales.setRowCount(0); // Clear previous results

        try {
            SupabaseServiceREST supabase = new SupabaseServiceREST(
                "https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1",
                "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n"
            );

            List<Map<String, Object>> ventas = supabase.fetchData("ventas");
            List<Object[]> rows = new ArrayList<>();
            for (Map<String, Object> venta : ventas) {
                Object id = venta.get("id");
                Object fechaIso = venta.get("fechaventa");
                Object totalVal = venta.get("total");
                Object cajaVal = venta.get("caja");

                Timestamp fechaTs = null;
                if (fechaIso instanceof String) {
                    try {
                        fechaTs = Timestamp.from(Instant.parse((String) fechaIso));
                    } catch (Exception ignore) {
                        fechaTs = null;
                    }
                }

                Double total = null;
                if (totalVal instanceof Number) {
                    total = ((Number) totalVal).doubleValue();
                } else if (totalVal instanceof String) {
                    try { total = Double.valueOf((String) totalVal); } catch (Exception ignore) { total = null; }
                }

                String caja = cajaVal != null ? String.valueOf(cajaVal) : null;

                rows.add(new Object[] { id, fechaTs, total, caja });
            }
            rows.sort(Comparator.comparing((Object[] r) -> (Timestamp) r[1], Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            for (Object[] r : rows) modelSales.addRow(r);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: Handle exception gracefully
        }
    }

    private void loadProducts() throws BasicException {
        modelProducts.setRowCount(0); // Clear previous results

        try {
            String sql = "SELECT P.ID, P.NAME, P.PRICEBUY, L.NAME, S.UNITS FROM PRODUCTS P JOIN STOCKCURRENT S ON P.ID = S.PRODUCT JOIN LOCATIONS L ON S.LOCATION = L.ID ORDER BY P.NAME";
            new PreparedSentence<Object, Object[]>(m_session, sql, null, new SerializerRead<Object[]>() {
                @Override
                public Object[] readValues(DataRead dr) throws BasicException {
                    Object[] row = new Object[5];
                    row[0] = dr.getString(1);
                    row[1] = dr.getString(2);
                    row[2] = dr.getDouble(3);
                    row[3] = dr.getString(4);
                    row[4] = dr.getDouble(5);
                    return row;
                }
            }).list().forEach(row -> modelProducts.addRow((Object[]) row));
        } catch (BasicException ex) {
            ex.printStackTrace();
            // TODO: Handle exception gracefully
        }
    }

    private void loadCashClosures() throws BasicException {
        modelCashClosures.setRowCount(0); // Clear previous results

        try {
            String sql = "SELECT C.ID, C.DATESTART, C.DATEEND, C.MONCASH, C.MONCASHIN, C.TOTAL, L.NAME FROM CLOSEDCASH C JOIN LOCATIONS L ON C.HOST = L.ID ORDER BY C.DATEEND DESC";
            new PreparedSentence<Object, Object[]>(m_session, sql, null, new SerializerRead<Object[]>() {
                @Override
                public Object[] readValues(DataRead dr) throws BasicException {
                    Object[] row = new Object[7];
                    row[0] = dr.getString(1);
                    row[1] = dr.getTimestamp(2);
                    row[2] = dr.getTimestamp(3);
                    row[3] = dr.getDouble(4);
                    row[4] = dr.getDouble(5);
                    row[5] = dr.getDouble(6);
                    row[6] = dr.getString(7);
                    return row;
                }
            }).list().forEach(row -> modelCashClosures.addRow((Object[]) row));
        } catch (BasicException ex) {
            ex.printStackTrace();
            // TODO: Handle exception gracefully
        }
    }
}
