//Sebastian
//Panel moderno de gestión de clientes con pestañas

package com.openbravo.pos.sebastian;

import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.basic.BasicException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Panel moderno para gestión de clientes Sebastian POS con sistema de pestañas
 * @author Sebastian
 */
public class JCustomerPanel extends JPanel implements JPanelView, BeanFactoryApp {
    
    // Colores modernos Sebastian 2025
    private static final Color SEBASTIAN_PRIMARY = new Color(52, 73, 94);     // Azul oscuro elegante
    private static final Color SEBASTIAN_SECONDARY = new Color(46, 204, 113); // Verde moderno
    private static final Color SEBASTIAN_ACCENT = new Color(155, 89, 182);    // Púrpura elegante
    private static final Color SEBASTIAN_LIGHT = new Color(236, 240, 241);    // Gris muy claro
    private static final Color SEBASTIAN_DARK = new Color(44, 62, 80);        // Azul muy oscuro
    private static final Color SUCCESS_GREEN = new Color(39, 174, 96);        // Verde éxito
    private static final Color WARNING_ORANGE = new Color(230, 126, 34);      // Naranja advertencia
    private static final Color DANGER_RED = new Color(231, 76, 60);           // Rojo peligro
    private static final Color INFO_BLUE = new Color(52, 152, 219);           // Azul información
    
    private CustomerService customerService;
    private PurchaseService purchaseService;
    
    // Componentes principales
    private JTabbedPane tabbedPane;
    private JTable customersTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JLabel statsLabel;
    
    // Pestaña de clientes - Formulario
    private JTextField txtFirstName, txtLastName, txtEmail, txtPhone, txtAddress, txtPoints;
    private JTextArea txtNotes;
    private JComboBox<String> cmbMembershipLevel;
    private JCheckBox chkActive;
    private Customer currentCustomer;
    
    // Pestaña de puntos
    private JTable pointsTable;
    private DefaultTableModel pointsTableModel;
    private JLabel lblCurrentPoints, lblTotalEarned, lblTotalRedeemed;
    private JTextField txtPointsToAdd, txtPointsToRedeem;
    
    // Pestaña de historial
    private JTable purchaseHistoryTable;
    private DefaultTableModel purchaseTableModel;
    private JLabel lblTotalPurchases, lblTotalSpent, lblAveragePurchase;
    private JTextField txtPurchaseSearch;
    
    public JCustomerPanel() {
        customerService = CustomerService.getInstance();
        purchaseService = PurchaseService.getInstance();
        initComponents();
        loadCustomers();
        updateStats();
    }
    
    /**
     * Inicializa todos los componentes de la interfaz con pestañas
     */
    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(SEBASTIAN_LIGHT);
        
        // Header con título y estadísticas
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Panel principal con pestañas
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.setForeground(SEBASTIAN_DARK);
        
        // Crear pestañas
        tabbedPane.addTab("  👥 Clientes  ", createCustomersTab());
        tabbedPane.addTab("  🏆 Puntos  ", createPointsTab());
        tabbedPane.addTab("  📊 Historial  ", createHistoryTab());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Escuchar cambios de pestaña
        tabbedPane.addChangeListener(e -> onTabChanged());
    }
    
    /**
     * Crea el panel del header con título y estadísticas
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(SEBASTIAN_PRIMARY);
        headerPanel.setBorder(new EmptyBorder(20, 25, 20, 25));
        
        // Panel izquierdo con título
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Sebastian POS");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        
        JLabel subtitleLabel = new JLabel("Sistema Avanzado de Gestión de Clientes");
        subtitleLabel.setForeground(new Color(189, 195, 199));
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.CENTER);
        
        // Panel de estadísticas
        statsLabel = new JLabel();
        statsLabel.setForeground(Color.WHITE);
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(statsLabel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    /**
     * Crea la pestaña de gestión de clientes
     */
    private JPanel createCustomersTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Panel de búsqueda
        panel.add(createSearchPanel(), BorderLayout.NORTH);
        
        // Panel central dividido
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createCustomersTablePanel());
        splitPane.setRightComponent(createCustomerFormPanel());
        splitPane.setDividerLocation(500);
        splitPane.setResizeWeight(0.6);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Crea el panel de búsqueda
     */
    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        searchPanel.setBackground(Color.WHITE);
        
        JLabel searchLabel = new JLabel("🔍");
        searchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        searchLabel.setForeground(SEBASTIAN_PRIMARY);
        
        searchField = new JTextField(25);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SEBASTIAN_LIGHT, 2),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.setBackground(SEBASTIAN_LIGHT);
        
        JButton searchButton = createModernButton("Buscar", SEBASTIAN_SECONDARY);
        searchButton.addActionListener(e -> searchCustomers());
        
        JButton clearButton = createModernButton("Limpiar", INFO_BLUE);
        clearButton.addActionListener(e -> {
            searchField.setText("");
            loadCustomers();
        });
        
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(clearButton);
        
        return searchPanel;
    }
    
    /**
     * Crea el panel de la tabla de clientes
     */
    private JPanel createCustomersTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Lista de Clientes"));
        
        // Crear tabla
        String[] columnNames = {"ID", "Nombre", "Email", "Teléfono", "Puntos", "Nivel", "Activo"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 6) return Boolean.class; // Activo
                return String.class;
            }
        };
        
        customersTable = new JTable(tableModel);
        styleTable(customersTable);
        
        customersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = customersTable.getSelectedRow();
                if (selectedRow >= 0) {
                    String customerId = (String) customersTable.getValueAt(selectedRow, 0);
                    loadCustomerDetails(customerId);
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(customersTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(createModernButton("🆕 Nuevo", SEBASTIAN_SECONDARY, e -> newCustomer()));
        buttonPanel.add(createModernButton("💾 Guardar", SEBASTIAN_PRIMARY, e -> saveCustomer()));
        buttonPanel.add(createModernButton("🗑️ Eliminar", DANGER_RED, e -> deleteCustomer()));
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Crea el formulario de cliente
     */
    private JPanel createCustomerFormPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("📝 Información del Cliente"));
        
        // Formulario
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Inicializar campos
        txtFirstName = createModernTextField();
        txtLastName = createModernTextField();
        txtEmail = createModernTextField();
        txtPhone = createModernTextField();
        txtAddress = createModernTextField();
        txtPoints = createModernTextField();
        txtPoints.setEditable(false);
        
        txtNotes = new JTextArea(3, 20);
        txtNotes.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtNotes.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        cmbMembershipLevel = new JComboBox<>(new String[]{"BRONZE", "SILVER", "GOLD", "PLATINUM"});
        cmbMembershipLevel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        chkActive = new JCheckBox("Cliente Activo");
        chkActive.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chkActive.setSelected(true);
        
        // Agregar campos al formulario
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtFirstName, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Apellidos:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtLastName, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtEmail, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Teléfono:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtPhone, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Dirección:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtAddress, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Puntos:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtPoints, gbc);
        
        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(new JLabel("Nivel:"), gbc);
        gbc.gridx = 1;
        formPanel.add(cmbMembershipLevel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 7;
        gbc.gridwidth = 2;
        formPanel.add(chkActive, gbc);
        
        gbc.gridx = 0; gbc.gridy = 8;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Notas:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(new JScrollPane(txtNotes), gbc);
        
        panel.add(formPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Crea la pestaña de puntos
     */
    private JPanel createPointsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Panel de información de puntos
        JPanel infoPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        infoPanel.setBorder(BorderFactory.createTitledBorder("🏆 Información de Puntos"));
        
        lblCurrentPoints = new JLabel("Puntos Actuales: -");
        lblTotalEarned = new JLabel("Total Ganados: -");
        lblTotalRedeemed = new JLabel("Total Canjeados: -");
        
        lblCurrentPoints.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotalEarned.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotalRedeemed.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        infoPanel.add(lblCurrentPoints);
        infoPanel.add(lblTotalEarned);
        infoPanel.add(lblTotalRedeemed);
        
        // Panel de acciones de puntos
        JPanel actionsPanel = new JPanel(new FlowLayout());
        
        txtPointsToAdd = new JTextField(10);
        txtPointsToRedeem = new JTextField(10);
        
        JButton btnAddPoints = createModernButton("✅ Añadir Puntos", SUCCESS_GREEN);
        JButton btnRedeemPoints = createModernButton("🎁 Canjear Puntos", SEBASTIAN_ACCENT);
        
        btnAddPoints.addActionListener(e -> addPoints());
        btnRedeemPoints.addActionListener(e -> redeemPoints());
        
        actionsPanel.add(new JLabel("Añadir:"));
        actionsPanel.add(txtPointsToAdd);
        actionsPanel.add(btnAddPoints);
        actionsPanel.add(Box.createHorizontalStrut(20));
        actionsPanel.add(new JLabel("Canjear:"));
        actionsPanel.add(txtPointsToRedeem);
        actionsPanel.add(btnRedeemPoints);
        
        infoPanel.add(actionsPanel);
        
        panel.add(infoPanel, BorderLayout.NORTH);
        
        // Tabla de historial de puntos
        pointsTableModel = new DefaultTableModel(new String[]{"Fecha", "Tipo", "Puntos", "Descripción"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        pointsTable = new JTable(pointsTableModel);
        styleTable(pointsTable);
        
        JScrollPane scrollPane = new JScrollPane(pointsTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("📊 Historial de Puntos"));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Crea la pestaña de historial de compras
     */
    private JPanel createHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Panel de estadísticas de compras
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("📊 Estadísticas de Compras"));
        
        lblTotalPurchases = new JLabel("Total Compras: -");
        lblTotalSpent = new JLabel("Total Gastado: -");
        lblAveragePurchase = new JLabel("Promedio: -");
        
        lblTotalPurchases.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotalSpent.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblAveragePurchase.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        statsPanel.add(lblTotalPurchases);
        statsPanel.add(lblTotalSpent);
        statsPanel.add(lblAveragePurchase);
        
        panel.add(statsPanel, BorderLayout.NORTH);
        
        // Panel de búsqueda de compras
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtPurchaseSearch = new JTextField(20);
        JButton btnSearchPurchases = createModernButton("🔍 Buscar", SEBASTIAN_PRIMARY);
        btnSearchPurchases.addActionListener(e -> searchPurchases());
        
        searchPanel.add(new JLabel("Buscar compras:"));
        searchPanel.add(txtPurchaseSearch);
        searchPanel.add(btnSearchPurchases);
        
        // Tabla de historial de compras
        purchaseTableModel = new DefaultTableModel(new String[]{"Fecha", "Recibo", "Total", "Método Pago", "Items"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        purchaseHistoryTable = new JTable(purchaseTableModel);
        styleTable(purchaseHistoryTable);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(purchaseHistoryTable), BorderLayout.CENTER);
        
        panel.add(centerPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Maneja el cambio de pestañas
     */
    private void onTabChanged() {
        int selectedTab = tabbedPane.getSelectedIndex();
        if (currentCustomer != null) {
            switch (selectedTab) {
                case 1: // Pestaña de puntos
                    updatePointsTab();
                    break;
                case 2: // Pestaña de historial
                    updateHistoryTab();
                    break;
            }
        }
    }
    
    // Métodos de gestión de puntos e historial
    
    private void updatePointsTab() {
        if (currentCustomer == null) {
            lblCurrentPoints.setText("Puntos Actuales: -");
            lblTotalEarned.setText("Total Ganados: -");
            lblTotalRedeemed.setText("Total Canjeados: -");
            pointsTableModel.setRowCount(0);
            return;
        }
        
        lblCurrentPoints.setText("Puntos Actuales: " + currentCustomer.getPoints());
        lblTotalEarned.setText("Total Ganados: " + currentCustomer.getTotalPointsEarned());
        lblTotalRedeemed.setText("Total Canjeados: " + currentCustomer.getTotalPointsRedeemed());
        
        // Ejemplo de historial de puntos
        pointsTableModel.setRowCount(0);
        pointsTableModel.addRow(new Object[]{"Hoy", "Ganados", "+50", "Compra #12345"});
        pointsTableModel.addRow(new Object[]{"Ayer", "Canjeados", "-25", "Descuento aplicado"});
    }
    
    private void updateHistoryTab() {
        if (currentCustomer == null) {
            lblTotalPurchases.setText("Total Compras: -");
            lblTotalSpent.setText("Total Gastado: -");
            lblAveragePurchase.setText("Promedio: -");
            purchaseTableModel.setRowCount(0);
            return;
        }
        
        lblTotalPurchases.setText("Total Compras: " + currentCustomer.getTotalPurchases());
        lblTotalSpent.setText(String.format("Total Gastado: €%.2f", currentCustomer.getTotalSpent()));
        lblAveragePurchase.setText(String.format("Promedio: €%.2f", currentCustomer.getAveragePurchase()));
        
        loadPurchaseHistory();
    }
    
    private void loadPurchaseHistory() {
        purchaseTableModel.setRowCount(0);
        if (currentCustomer != null) {
            List<Purchase> purchases = purchaseService.getPurchasesByCustomer(currentCustomer.getId());
            for (Purchase purchase : purchases) {
                purchaseTableModel.addRow(new Object[]{
                    purchase.getFormattedDate(),
                    purchase.getReceiptNumber(),
                    String.format("€%.2f", purchase.getTotal()),
                    purchase.getPaymentMethod(),
                    purchase.getItems().size() + " items"
                });
            }
        }
    }
    
    private void addPoints() {
        if (currentCustomer == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente primero", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            int points = Integer.parseInt(txtPointsToAdd.getText().trim());
            if (points <= 0) {
                JOptionPane.showMessageDialog(this, "Introduzca un número válido de puntos", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            currentCustomer.setPoints(currentCustomer.getPoints() + points);
            currentCustomer.setTotalPointsEarned(currentCustomer.getTotalPointsEarned() + points);
            customerService.updateCustomer(currentCustomer);
            
            txtPointsToAdd.setText("");
            updatePointsTab();
            loadCustomers();
            
            JOptionPane.showMessageDialog(this, "Puntos añadidos correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Introduzca un número válido", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void redeemPoints() {
        if (currentCustomer == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente primero", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            int points = Integer.parseInt(txtPointsToRedeem.getText().trim());
            if (points <= 0) {
                JOptionPane.showMessageDialog(this, "Introduzca un número válido de puntos", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (currentCustomer.redeemPoints(points)) {
                customerService.updateCustomer(currentCustomer);
                txtPointsToRedeem.setText("");
                updatePointsTab();
                loadCustomers();
                
                JOptionPane.showMessageDialog(this, "Puntos canjeados correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Puntos insuficientes", "Error", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Introduzca un número válido", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void searchPurchases() {
        String searchText = txtPurchaseSearch.getText().trim();
        if (searchText.isEmpty()) {
            loadPurchaseHistory();
        } else {
            purchaseTableModel.setRowCount(0);
            if (currentCustomer != null) {
                List<Purchase> purchases = purchaseService.searchPurchases(searchText);
                for (Purchase purchase : purchases) {
                    if (purchase.getCustomerId().equals(currentCustomer.getId())) {
                        purchaseTableModel.addRow(new Object[]{
                            purchase.getFormattedDate(),
                            purchase.getReceiptNumber(),
                            String.format("€%.2f", purchase.getTotal()),
                            purchase.getPaymentMethod(),
                            purchase.getItems().size() + " items"
                        });
                    }
                }
            }
        }
    }
    
    // Métodos auxiliares de gestión de clientes
    
    private void loadCustomerDetails(String customerId) {
        currentCustomer = customerService.getCustomerById(customerId);
        if (currentCustomer != null) {
            txtFirstName.setText(currentCustomer.getFirstName() != null ? currentCustomer.getFirstName() : "");
            txtLastName.setText(currentCustomer.getLastName() != null ? currentCustomer.getLastName() : "");
            txtEmail.setText(currentCustomer.getEmail() != null ? currentCustomer.getEmail() : "");
            txtPhone.setText(currentCustomer.getPhone() != null ? currentCustomer.getPhone() : "");
            txtAddress.setText(currentCustomer.getAddress() != null ? currentCustomer.getAddress() : "");
            txtPoints.setText(String.valueOf(currentCustomer.getPoints()));
            cmbMembershipLevel.setSelectedItem(currentCustomer.getMembershipLevel());
            chkActive.setSelected(currentCustomer.isActive());
            txtNotes.setText(currentCustomer.getNotes() != null ? currentCustomer.getNotes() : "");
        }
    }
    
    private void newCustomer() {
        clearForm();
    }
    
    private void saveCustomer() {
        try {
            Customer customer = currentCustomer != null ? currentCustomer : new Customer();
            
            customer.setFirstName(txtFirstName.getText().trim());
            customer.setLastName(txtLastName.getText().trim());
            customer.setEmail(txtEmail.getText().trim());
            customer.setPhone(txtPhone.getText().trim());
            customer.setAddress(txtAddress.getText().trim());
            
            if (!txtPoints.getText().trim().isEmpty()) {
                customer.setPoints(Integer.parseInt(txtPoints.getText().trim()));
            }
            
            customer.setMembershipLevel((String) cmbMembershipLevel.getSelectedItem());
            customer.setActive(chkActive.isSelected());
            customer.setNotes(txtNotes.getText().trim());
            
            if (currentCustomer == null) {
                customerService.addCustomer(customer);
            } else {
                customerService.updateCustomer(customer);
            }
            
            loadCustomers();
            updateStats();
            clearForm();
            
            JOptionPane.showMessageDialog(this, "Cliente guardado exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Los puntos deben ser un número válido", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al guardar el cliente: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void deleteCustomer() {
        if (currentCustomer == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente para eliminar", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de que desea eliminar este cliente?",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION);
            
        if (result == JOptionPane.YES_OPTION) {
            customerService.deleteCustomer(currentCustomer.getId());
            loadCustomers();
            updateStats();
            clearForm();
            
            JOptionPane.showMessageDialog(this, "Cliente eliminado exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void clearForm() {
        currentCustomer = null;
        txtFirstName.setText("");
        txtLastName.setText("");
        txtEmail.setText("");
        txtPhone.setText("");
        txtAddress.setText("");
        txtPoints.setText("0");
        cmbMembershipLevel.setSelectedIndex(0);
        chkActive.setSelected(true);
        txtNotes.setText("");
        customersTable.clearSelection();
    }
    
    private void loadCustomers() {
        tableModel.setRowCount(0);
        List<Customer> customers = customerService.getAllCustomers();
        
        for (Customer customer : customers) {
            Object[] row = {
                customer.getId(),
                customer.getFullName(),
                customer.getEmail() != null ? customer.getEmail() : "",
                customer.getPhone() != null ? customer.getPhone() : "",
                customer.getPoints(),
                customer.getMembershipLevel(),
                customer.isActive()
            };
            tableModel.addRow(row);
        }
    }
    
    private void updateStats() {
        var stats = customerService.getMembershipStatistics();
        int total = customerService.getTotalCustomers();
        int active = customerService.getActiveCustomersCount();
        int totalPoints = customerService.getTotalPoints();
        
        String statsText = String.format(
            "Total: %d | Activos: %d | Puntos: %,d | Bronze: %d | Silver: %d | Gold: %d | Platinum: %d",
            total, active, totalPoints,
            stats.get("BRONZE"), stats.get("SILVER"), stats.get("GOLD"), stats.get("PLATINUM")
        );
        
        statsLabel.setText(statsText);
    }
    
    private void searchCustomers() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            loadCustomers();
            return;
        }
        
        tableModel.setRowCount(0);
        List<Customer> customers = customerService.searchCustomers(query);
        
        for (Customer customer : customers) {
            Object[] row = {
                customer.getId(),
                customer.getFullName(),
                customer.getEmail() != null ? customer.getEmail() : "",
                customer.getPhone() != null ? customer.getPhone() : "",
                customer.getPoints(),
                customer.getMembershipLevel(),
                customer.isActive()
            };
            tableModel.addRow(row);
        }
    }
    
    // Métodos de creación de componentes modernos
    
    private JButton createModernButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        
        return button;
    }
    
    private JButton createModernButton(String text, Color bgColor, ActionListener listener) {
        JButton button = createModernButton(text, bgColor);
        button.addActionListener(listener);
        return button;
    }
    
    private JTextField createModernTextField() {
        JTextField field = new JTextField(15);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return field;
    }
    
    private void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(35);
        table.setSelectionBackground(SEBASTIAN_PRIMARY);
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setBackground(SEBASTIAN_LIGHT);
        table.getTableHeader().setForeground(SEBASTIAN_DARK);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setGridColor(new Color(189, 195, 199));
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
    }
    
    // Implementación de interfaces requeridas
    
    @Override
    public void init(AppView app) throws BeanFactoryException {
        // Implementación requerida por BeanFactoryApp
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
        return "Sebastian - Gestión de Clientes";
    }
    
    @Override
    public void activate() throws BasicException {
        // Activar el panel
        loadCustomers();
        updateStats();
    }
    
    @Override
    public boolean deactivate() {
        return true;
    }
}