//    KriolOS POS
//    Copyright (c) 2019-2023 KriolOS

package com.openbravo.pos.branches;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.DataRead;
import com.openbravo.data.loader.PreparedSentence;
import com.openbravo.data.loader.SerializerRead;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.JPanelView;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.table.DefaultTableModel;

/**
 * Panel de Administración de Sucursales
 * 
 * @author Sebastian
 */
public class JPanelBranchesManagement extends JPanel implements JPanelView, BeanFactoryApp {

    private static final long serialVersionUID = 1L;
    
    private AppView app;
    private Session session;
    
    // UI Components
    private JTabbedPane tabbedPane;
    private JTable tblLocations, tblTransactions, tblCustomers, tblProducts;
    private DefaultTableModel modelLocations, modelTransactions, modelCustomers, modelProducts;
    private JComboBox<LocationItem> cmbLocation;
    private JLabel lblInfo;
    private String selectedLocationId = null;
    
    public JPanelBranchesManagement() {
        System.out.println("*** JPanelBranchesManagement - Constructor iniciado ***");
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel superior
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);
        
        // Tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("📍 Ubicaciones", createLocationsPanel());
        tabbedPane.addTab("💰 Ventas", createTransactionsPanel());
        tabbedPane.addTab("👥 Clientes", createCustomersPanel());
        tabbedPane.addTab("📦 Productos", createProductsPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        System.out.println("*** JPanelBranchesManagement - Constructor completado ***");
    }
    
    @Override
    public void init(AppView app) throws BeanFactoryException {
        System.out.println("*** JPanelBranchesManagement - init() ***");
        this.app = app;
        this.session = app.getSession();
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
        System.out.println("*** JPanelBranchesManagement - activate() ***");
        loadAllData();
    }

    @Override
    public boolean deactivate() {
        return true;
    }
    
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Filtrar por Sucursal"));
        
        JLabel lblSucursal = new JLabel("Sucursal:");
        lblSucursal.setFont(new Font("Arial", Font.BOLD, 12));
        
        cmbLocation = new JComboBox<>();
        cmbLocation.setPreferredSize(new Dimension(300, 30));
        cmbLocation.addItem(new LocationItem("", "--- TODAS LAS SUCURSALES ---"));
        
        JButton btnBuscar = new JButton("🔍 Buscar");
        btnBuscar.setFont(new Font("Arial", Font.BOLD, 12));
        btnBuscar.addActionListener(e -> {
            LocationItem selected = (LocationItem) cmbLocation.getSelectedItem();
            if (selected != null) {
                selectedLocationId = selected.getId().isEmpty() ? null : selected.getId();
                try {
                    loadAllData();
                } catch (BasicException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        JButton btnActualizar = new JButton("🔄 Actualizar");
        btnActualizar.setFont(new Font("Arial", Font.BOLD, 12));
        btnActualizar.addActionListener(e -> {
            selectedLocationId = null;
            cmbLocation.setSelectedIndex(0);
            try {
                loadAllData();
            } catch (BasicException ex) {
                ex.printStackTrace();
            }
        });
        
        lblInfo = new JLabel("Total registros: 0");
        lblInfo.setFont(new Font("Arial", Font.BOLD, 12));
        
        panel.add(lblSucursal);
        panel.add(cmbLocation);
        panel.add(btnBuscar);
        panel.add(btnActualizar);
        panel.add(lblInfo);
        
        return panel;
    }
    
    private JPanel createLocationsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"ID", "Nombre", "Dirección"};
        modelLocations = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblLocations = new JTable(modelLocations);
        panel.add(new JScrollPane(tblLocations), BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"Ticket", "Fecha", "Vendedor", "Cliente", "Total", "Estado", "Ubicación"};
        modelTransactions = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblTransactions = new JTable(modelTransactions);
        panel.add(new JScrollPane(tblTransactions), BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createCustomersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"ID", "Nombre", "Teléfono", "Email"};
        modelCustomers = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblCustomers = new JTable(modelCustomers);
        panel.add(new JScrollPane(tblCustomers), BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"Código", "Producto", "Ubicación", "Stock"};
        modelProducts = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblProducts = new JTable(modelProducts);
        panel.add(new JScrollPane(tblProducts), BorderLayout.CENTER);
        return panel;
    }
    
    private void loadAllData() throws BasicException {
        System.out.println("*** Cargando datos... Sucursal: " + selectedLocationId);
        loadLocations();
        loadTransactions();
        loadCustomers();
        loadProducts();
        updateInfo();
    }
    
    private void loadLocations() throws BasicException {
        modelLocations.setRowCount(0);
        cmbLocation.removeAllItems();
        cmbLocation.addItem(new LocationItem("", "--- TODAS LAS SUCURSALES ---"));
        
        try {
            PreparedSentence sent = new PreparedSentence(session,
                "SELECT ID, NAME, ADDRESS FROM LOCATIONS ORDER BY NAME",
                null,
                new SerializerRead() {
                    @Override
                    public Object readValues(DataRead dr) throws BasicException {
                        Object[] row = new Object[3];
                        row[0] = dr.getString(1);
                        row[1] = dr.getString(2);
                        row[2] = dr.getString(3);
                        
                        // Agregar al combo
                        String id = dr.getString(1);
                        String name = dr.getString(2);
                        cmbLocation.addItem(new LocationItem(id, id + " - " + name));
                        
                        return row;
                    }
                });
            
            List locations = sent.list();
            for (Object obj : locations) {
                modelLocations.addRow((Object[]) obj);
            }
            System.out.println("Ubicaciones cargadas: " + locations.size());
        } catch (Exception e) {
            System.err.println("Error cargando ubicaciones: " + e.getMessage());
        }
    }
    
    private void loadTransactions() throws BasicException {
        modelTransactions.setRowCount(0);
        
        try {
            String sql = "SELECT t.TICKETID, t.DATENEW, COALESCE(p.NAME, 'N/A'), " +
                        "COALESCE(c.NAME, 'General'), t.TOTAL, t.STATUS, COALESCE(l.NAME, 'Principal') " +
                        "FROM TICKETS t " +
                        "LEFT JOIN PEOPLE p ON t.PERSON = p.ID " +
                        "LEFT JOIN CUSTOMERS c ON t.CUSTOMER = c.ID " +
                        "LEFT JOIN LOCATIONS l ON t.LOCATION = l.ID ";
            
            if (selectedLocationId != null && !selectedLocationId.isEmpty()) {
                sql += "WHERE l.ID = '" + selectedLocationId + "' ";
            }
            
            sql += "ORDER BY t.DATENEW DESC LIMIT 500";
            
            PreparedSentence sent = new PreparedSentence(session, sql, null,
                new SerializerRead() {
                    @Override
                    public Object readValues(DataRead dr) throws BasicException {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                        Object[] row = new Object[7];
                        row[0] = dr.getString(1);
                        Date date = dr.getTimestamp(2);
                        row[1] = date != null ? sdf.format(date) : "";
                        row[2] = dr.getString(3);
                        row[3] = dr.getString(4);
                        row[4] = String.format("$%.2f", dr.getDouble(5));
                        int status = dr.getInt(6);
                        row[5] = status == 0 ? "✅ OK" : (status == 1 ? "⏳ Pendiente" : "❌ Anulado");
                        row[6] = dr.getString(7);
                        return row;
                    }
                });
            
            List transactions = sent.list();
            for (Object obj : transactions) {
                modelTransactions.addRow((Object[]) obj);
            }
            System.out.println("Transacciones cargadas: " + transactions.size());
        } catch (Exception e) {
            System.err.println("Error cargando transacciones: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadCustomers() throws BasicException {
        modelCustomers.setRowCount(0);
        
        try {
            PreparedSentence sent = new PreparedSentence(session,
                "SELECT ID, NAME, PHONE, EMAIL FROM CUSTOMERS ORDER BY NAME LIMIT 500",
                null,
                new SerializerRead() {
                    @Override
                    public Object readValues(DataRead dr) throws BasicException {
                        Object[] row = new Object[4];
                        row[0] = dr.getString(1);
                        row[1] = dr.getString(2);
                        row[2] = dr.getString(3) != null ? dr.getString(3) : "";
                        row[3] = dr.getString(4) != null ? dr.getString(4) : "";
                        return row;
                    }
                });
            
            List customers = sent.list();
            for (Object obj : customers) {
                modelCustomers.addRow((Object[]) obj);
            }
            System.out.println("Clientes cargados: " + customers.size());
        } catch (Exception e) {
            System.err.println("Error cargando clientes: " + e.getMessage());
        }
    }
    
    private void loadProducts() throws BasicException {
        modelProducts.setRowCount(0);
        
        try {
            String sql = "SELECT p.CODE, p.NAME, COALESCE(l.NAME, 'Principal'), " +
                        "COALESCE(s.UNITS, 0) " +
                        "FROM PRODUCTS p " +
                        "LEFT JOIN STOCKCURRENT s ON p.ID = s.PRODUCT " +
                        "LEFT JOIN LOCATIONS l ON s.LOCATION = l.ID ";
            
            if (selectedLocationId != null && !selectedLocationId.isEmpty()) {
                sql += "WHERE l.ID = '" + selectedLocationId + "' ";
            }
            
            sql += "ORDER BY p.NAME LIMIT 500";
            
            PreparedSentence sent = new PreparedSentence(session, sql, null,
                new SerializerRead() {
                    @Override
                    public Object readValues(DataRead dr) throws BasicException {
                        Object[] row = new Object[4];
                        row[0] = dr.getString(1);
                        row[1] = dr.getString(2);
                        row[2] = dr.getString(3);
                        row[3] = String.format("%.2f", dr.getDouble(4));
                        return row;
                    }
                });
            
            List products = sent.list();
            for (Object obj : products) {
                modelProducts.addRow((Object[]) obj);
            }
            System.out.println("Productos cargados: " + products.size());
        } catch (Exception e) {
            System.err.println("Error cargando productos: " + e.getMessage());
        }
    }
    
    private void updateInfo() {
        lblInfo.setText(String.format("Ubicaciones: %d | Ventas: %d | Clientes: %d | Productos: %d",
            modelLocations.getRowCount(), modelTransactions.getRowCount(),
            modelCustomers.getRowCount(), modelProducts.getRowCount()));
    }
    
    /**
     * Clase auxiliar para items del ComboBox
     */
    private static class LocationItem {
        private final String id;
        private final String name;
        
        public LocationItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public String getId() {
            return id;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
}
