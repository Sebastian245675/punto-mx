//    KriolOS POS
//    Copyright (c) 2019-2023 KriolOS

package com.openbravo.pos.branches;

import com.openbravo.pos.forms.AppView;
import com.openbravo.basic.BasicException;
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
import com.openbravo.pos.supabase.SupabaseServiceManager;
import com.openbravo.pos.forms.AppConfig;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

public class JPanelBranchesManagement extends JPanel implements JPanelView, BeanFactoryApp {

    private AppView m_App;
    private Session m_session;
    private JTabbedPane jTabbedPane1;
    private JPanel jPanelSearchBranch;
    private JPanel jPanelSales;
    private JPanel jPanelCashClosures;
    private JTextField jTextFieldBranchName;
    private JTable jTableBranches;
    private DefaultTableModel modelBranches;
    private JTable jTableSales;
    private DefaultTableModel modelSales;
    private JTable jTableCashClosures;
    private DefaultTableModel modelCashClosures;
    
    // Campos para validación de administrador
    private JTextField jTextFieldAdminCode;
    private JButton jButtonValidateAdmin;
    private JLabel jLabelAdminStatus;
    private boolean adminValidated = false;
    private String validatedAdminCode = null;
    private JPanel jPanelValidation;

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
        // Limpiar estado de validación cada vez que se activa el panel
        // NO cargar desde caché - siempre requerir validación manual
        adminValidated = false;
        validatedAdminCode = null;
        
        // Limpiar campo de código y estado del label
        if (jTextFieldAdminCode != null) {
            jTextFieldAdminCode.setText("");
        }
        if (jLabelAdminStatus != null) {
            jLabelAdminStatus.setText("⚠️ Debe validar el código de administrador para ver esta sección");
            jLabelAdminStatus.setForeground(java.awt.Color.ORANGE);
        }
        
        // Limpiar todas las tablas
        clearAllData();
        
        // NO cargar datos hasta que se valide el código manualmente
    }
    
    /**
     * Muestra mensaje indicando que se debe validar el código de administrador
     */
    private void showValidationMessage() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                this,
                "⚠️ ACCESO RESTRINGIDO\n\n" +
                "Para acceder a 'Administrar Sucursales', debe validar primero\n" +
                "el código de tarjeta de un administrador.\n\n" +
                "Ingrese el código en el campo superior y haga clic en 'Validar Administrador'.",
                "Validación Requerida",
                JOptionPane.WARNING_MESSAGE
            );
        });
    }
    
    /**
     * Carga los datos solo si el administrador está validado
     */
    private void loadDataIfValidated() {
        if (!adminValidated) {
            return;
        }
        
        try {
            // Llamar a la función RPC de Supabase para actualizar totales de cierre
            actualizarMontosCierres();
            
            searchBranches(); // Load all branches initially
            loadSales();
            loadCashClosures();
        } catch (Exception e) {
            System.err.println("Error cargando datos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Llama a la función RPC de Supabase para actualizar los montos de dinero y tarjeta en los cierres
     */
    private void actualizarMontosCierres() {
        // Verificar validación antes de actualizar
        if (!adminValidated) {
            return;
        }
        
        try {
            // Usar SupabaseServiceManager para obtener el servicio
            AppConfig config = new AppConfig(null);
            config.load();
            SupabaseServiceManager supabaseManager = SupabaseServiceManager.getInstance();
            supabaseManager.initialize(config);
            
            SupabaseServiceREST supabase = supabaseManager.getService();
            boolean success = supabase.callRPC("onactualizardinero_tarjeta_cierres", null);
            if (success) {
                System.out.println("Función onactualizardinero_tarjeta_cierres ejecutada exitosamente");
            } else {
                System.err.println("Error al ejecutar onactualizardinero_tarjeta_cierres");
            }
        } catch (Exception e) {
            System.err.println("Error al llamar onactualizardinero_tarjeta_cierres: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean deactivate() {
        // Limpiar estado de validación al salir del panel
        adminValidated = false;
        validatedAdminCode = null;
        
        // Limpiar campo de código
        if (jTextFieldAdminCode != null) {
            jTextFieldAdminCode.setText("");
        }
        
        // Limpiar todas las tablas
        clearAllData();
        
        return true;
    }

    private void refreshAllData() throws BasicException {
        // Verificar validación antes de refrescar
        if (!adminValidated) {
            showValidationMessage();
            return;
        }
        
        // Actualizar montos antes de refrescar los datos
        actualizarMontosCierres();
        searchBranches(); // No lanza BasicException, solo maneja Exception
        loadSales(); // No lanza BasicException, solo maneja Exception
        loadCashClosures();
    }
    
    /**
     * Valida el código de administrador contra Supabase
     * Verifica que el código exista y pertenezca a un usuario con rol = 1
     */
    private void validateAdminCode(String adminCode, boolean silent) {
        if (adminCode == null || adminCode.trim().isEmpty()) {
            if (!silent && jLabelAdminStatus != null) {
                jLabelAdminStatus.setText("⚠️ Ingrese un código de administrador");
                jLabelAdminStatus.setForeground(java.awt.Color.ORANGE);
            }
            return;
        }
        
        // Deshabilitar el botón durante la validación
        if (!silent && jButtonValidateAdmin != null && jLabelAdminStatus != null) {
            jButtonValidateAdmin.setEnabled(false);
            jButtonValidateAdmin.setText("Validando...");
            jLabelAdminStatus.setText("⏳ Verificando...");
            jLabelAdminStatus.setForeground(java.awt.Color.BLUE);
        }
        
        // Ejecutar validación en background
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String userName = null;
            private String userRole = null;
            
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    // Usar SupabaseServiceManager con conexión interna
                    SupabaseServiceManager manager = SupabaseServiceManager.getInstance();
                    AppConfig tempConfig = new AppConfig(null);
                    tempConfig.load();
                    manager.initialize(tempConfig);
                    SupabaseServiceREST supabase = manager.getService();
                    List<Map<String, Object>> usuarios = supabase.fetchData("usuarios");
                    
                    // Buscar el código (tarjeta) en la lista de usuarios
                    for (Map<String, Object> u : usuarios) {
                        Object card = u.get("tarjeta");
                        if (card == null) card = u.get("card");
                        if (card != null && adminCode.trim().equals(card.toString().trim())) {
                            // Verificar que el usuario tenga rol = 1 (admin)
                            Object rol = u.get("rol");
                            if (rol != null) {
                                String rolStr = rol.toString().trim();
                                // rol = 1 son admins
                                if ("1".equals(rolStr)) {
                                    // Obtener nombre del usuario
                                    Object nombre = u.get("nombre");
                                    if (nombre == null) nombre = u.get("name");
                                    userName = nombre != null ? nombre.toString() : null;
                                    userRole = rolStr;
                                    return true; // Código válido y es admin
                                }
                            }
                            // Si llegamos aquí, el código existe pero no es admin
                            userName = null;
                            userRole = rol != null ? rol.toString() : null;
                            return false; // Código existe pero no es admin
                        }
                    }
                    return false; // Código no encontrado
                } catch (Exception ex) {
                    System.err.println("Error validando código de administrador: " + ex.getMessage());
                    ex.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                // Rehabilitar el botón (en hilo de UI)
                if (!silent && jButtonValidateAdmin != null) {
                    SwingUtilities.invokeLater(() -> {
                        jButtonValidateAdmin.setEnabled(true);
                        jButtonValidateAdmin.setText("Validar Administrador");
                    });
                }
                
                try {
                    boolean isValidAdmin = get();
                    if (isValidAdmin) {
                        adminValidated = true;
                        validatedAdminCode = adminCode.trim();
                        
                        // NO guardar en configuración - el código debe validarse cada vez
                        // Esto asegura que cada sesión requiera validación manual
                        
                        // Actualizar label (tanto en modo silencioso como normal)
                        SwingUtilities.invokeLater(() -> {
                            if (jLabelAdminStatus != null) {
                                jLabelAdminStatus.setText("✓ Administrador validado: " + (userName != null ? userName : adminCode));
                                jLabelAdminStatus.setForeground(new java.awt.Color(0, 150, 0));
                            }
                            
                            // Mostrar mensaje solo si no es modo silencioso
                            if (!silent) {
                                JOptionPane.showMessageDialog(
                                    JPanelBranchesManagement.this,
                                    "✅ Código de administrador validado correctamente.\n\n" +
                                    "Administrador: " + (userName != null ? userName : adminCode) + "\n" +
                                    "Ahora puede ver la información de sucursales.",
                                    "Validación Exitosa",
                                    JOptionPane.INFORMATION_MESSAGE
                                );
                            }
                            
                            // Cargar los datos ahora que está validado
                            loadDataIfValidated();
                        });
                        
                    } else {
                        adminValidated = false;
                        validatedAdminCode = null;
                        
                        if (!silent) {
                            SwingUtilities.invokeLater(() -> {
                                if (userRole != null && !"1".equals(userRole)) {
                                    jLabelAdminStatus.setText("✗ El código no pertenece a un administrador");
                                    JOptionPane.showMessageDialog(
                                        JPanelBranchesManagement.this,
                                        "❌ El código ingresado no pertenece a un administrador.\n\n" +
                                        "Solo los usuarios con permisos de administrador pueden acceder a esta sección.",
                                        "Código No Válido",
                                        JOptionPane.WARNING_MESSAGE
                                    );
                                } else {
                                    jLabelAdminStatus.setText("✗ Código no encontrado");
                                    JOptionPane.showMessageDialog(
                                        JPanelBranchesManagement.this,
                                        "❌ El código ingresado no es válido.",
                                        "Código No Válido",
                                        JOptionPane.WARNING_MESSAGE
                                    );
                                }
                                jLabelAdminStatus.setForeground(java.awt.Color.RED);
                            });
                        }
                        
                        // Limpiar datos si no está validado (en hilo de UI)
                        SwingUtilities.invokeLater(() -> {
                            clearAllData();
                        });
                    }
                } catch (Exception e) {
                    adminValidated = false;
                    validatedAdminCode = null;
                    
                    if (!silent) {
                        SwingUtilities.invokeLater(() -> {
                            jLabelAdminStatus.setText("✗ Error al validar");
                            jLabelAdminStatus.setForeground(java.awt.Color.RED);
                            JOptionPane.showMessageDialog(
                                JPanelBranchesManagement.this,
                                "Error al validar el código de administrador.\n\nPor favor, verifique su conexión e intente nuevamente.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                            );
                        });
                    }
                    
                    // Limpiar datos (en hilo de UI)
                    SwingUtilities.invokeLater(() -> {
                        clearAllData();
                    });
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Limpia todos los datos de las tablas
     */
    private void clearAllData() {
        modelBranches.setRowCount(0);
        modelSales.setRowCount(0);
        modelCashClosures.setRowCount(0);
    }
    
    /**
     * Maneja el evento de validación de administrador
     */
    private void jButtonValidateAdminActionPerformed(java.awt.event.ActionEvent evt) {
        String adminCode = jTextFieldAdminCode.getText().trim();
        validateAdminCode(adminCode, false);
    }
    
    private void initComponents() {
        jTabbedPane1 = new JTabbedPane();
        jPanelSearchBranch = new JPanel();
        jPanelSales = new JPanel();
        jPanelCashClosures = new JPanel();
        jTextFieldBranchName = new JTextField(20);
        String[] branchColumns = {"ID", "Nombre", "Dirección"};
        modelBranches = new DefaultTableModel(branchColumns, 0);
        jTableBranches = new JTable(modelBranches);

        String[] salesColumns = {"ID Venta", "Fecha", "Total", "Sucursal/Caja", "Vendedor/Nombre"};
        modelSales = new DefaultTableModel(salesColumns, 0);
        jTableSales = new JTable(modelSales);

        String[] cashClosureColumns = {"Vendedor", "Fecha Inicio", "Fecha Fin", "Monto Inicial", "Efectivo", "Tarjeta", "Total", "Faltante", "Sobrante", "Sucursal"};
        modelCashClosures = new DefaultTableModel(cashClosureColumns, 0);
        jTableCashClosures = new JTable(modelCashClosures);

        setLayout(new BorderLayout());

        // Panel superior con validación de administrador
        JPanel headerPanel = new JPanel(new BorderLayout());
        
        // Panel de validación de administrador
        jPanelValidation = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        jPanelValidation.setBorder(javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createEtchedBorder(),
            "Validación de Administrador",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new java.awt.Font("Arial", java.awt.Font.BOLD, 12)
        ));
        
        jPanelValidation.add(new JLabel("Código de Administrador:"));
        jTextFieldAdminCode = new JTextField(20);
        jTextFieldAdminCode.setToolTipText("Ingrese el código de tarjeta de un administrador (rol = 1)");
        jPanelValidation.add(jTextFieldAdminCode);
        
        jButtonValidateAdmin = new JButton("Validar Administrador");
        jButtonValidateAdmin.addActionListener(this::jButtonValidateAdminActionPerformed);
        jPanelValidation.add(jButtonValidateAdmin);
        
        jLabelAdminStatus = new JLabel("⚠️ Debe validar el código de administrador para ver esta sección");
        jLabelAdminStatus.setForeground(java.awt.Color.ORANGE);
        jLabelAdminStatus.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
        jPanelValidation.add(jLabelAdminStatus);
        
        headerPanel.add(jPanelValidation, BorderLayout.NORTH);
        
        // Panel con botón de actualizar
        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton refreshButton = new JButton("Actualizar información");
        refreshButton.addActionListener(e -> {
            try {
                refreshAllData();
            } catch (BasicException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                    JPanelBranchesManagement.this,
                    "Error al actualizar información: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });
        refreshPanel.add(refreshButton);
        headerPanel.add(refreshPanel, BorderLayout.CENTER);
        
        add(headerPanel, BorderLayout.NORTH);

        // Panel Buscar Sucursal
        JPanel searchPanel = new JPanel(new FlowLayout());
        searchPanel.add(new JLabel("Buscar por Nombre de Sucursal:"));
        searchPanel.add(jTextFieldBranchName);
        
        JButton searchButton = new JButton("Buscar");
        searchButton.addActionListener(e -> {
            searchBranches();
        });
        searchPanel.add(searchButton);

        JButton showAllButton = new JButton("Mostrar todo");
        showAllButton.addActionListener(e -> {
            jTextFieldBranchName.setText("");
            searchBranches();
        });
        searchPanel.add(showAllButton);

        jPanelSearchBranch.setLayout(new BorderLayout());
        jPanelSearchBranch.add(searchPanel, BorderLayout.NORTH);
        jPanelSearchBranch.add(new JScrollPane(jTableBranches), BorderLayout.CENTER);

        jTabbedPane1.addTab("Buscar Sucursal", jPanelSearchBranch);
        jTabbedPane1.setToolTipTextAt(0, "Buscar y visualizar información de las sucursales");

        // Panel Ver Ventas
        jPanelSales.setLayout(new BorderLayout());
        jPanelSales.add(new JScrollPane(jTableSales), BorderLayout.CENTER);
        jTabbedPane1.addTab("Ver Ventas", jPanelSales);
        jTabbedPane1.setToolTipTextAt(1, "Consultar todas las ventas registradas por sucursal");

        // Panel Ver Cierres de Caja
        jPanelCashClosures.setLayout(new BorderLayout());
        jPanelCashClosures.add(new JScrollPane(jTableCashClosures), BorderLayout.CENTER);
        jTabbedPane1.addTab("Ver Cierres de Caja", jPanelCashClosures);
        jTabbedPane1.setToolTipTextAt(2, "Revisar los cierres de caja de todas las sucursales");

        // Agregar listener para cuando se cambie a la pestaña "Ver Cierres de Caja"
        jTabbedPane1.addChangeListener(e -> {
            // Verificar validación antes de cambiar de pestaña
            if (!adminValidated) {
                showValidationMessage();
                return;
            }
            
            int selectedIndex = jTabbedPane1.getSelectedIndex();
            String selectedTitle = jTabbedPane1.getTitleAt(selectedIndex);
            if ("Ver Cierres de Caja".equals(selectedTitle)) {
                // Actualizar montos cuando se cambia a esta pestaña
                actualizarMontosCierres();
                // Recargar los datos de cierres después de actualizar
                try {
                    loadCashClosures();
                } catch (BasicException ex) {
                    ex.printStackTrace();
                }
            }
        });

        add(jTabbedPane1, BorderLayout.CENTER);
    }

    private void searchBranches() {
        // Verificar validación antes de buscar
        if (!adminValidated) {
            return;
        }
        
        String branchName = jTextFieldBranchName.getText().trim().toLowerCase();
        modelBranches.setRowCount(0); // Clear previous results

        try {
            SupabaseServiceREST supabase = new SupabaseServiceREST(
                "https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1",
                "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n"
            );
            List<Map<String, Object>> usuarios = supabase.fetchData("usuarios");
            for (Map<String, Object> u : usuarios) {
                Object sid = u.get("sucursal_id");
                Object sname = u.get("sucursal_nombre");
                Object saddr = u.get("sucursal_direccion");
                if (sid == null || sname == null || saddr == null) continue; // solo completos
                String idStr = String.valueOf(sid);
                String nameStr = String.valueOf(sname);
                if (!branchName.isEmpty() && !nameStr.toLowerCase().contains(branchName)) continue;
                modelBranches.addRow(new Object[]{
                    idStr,
                    nameStr,
                    String.valueOf(saddr)
                });
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: Handle exception gracefully, perhaps show an error message to the user
        }
    }

    private void loadSales() throws BasicException {
        // Verificar validación antes de cargar
        if (!adminValidated) {
            modelSales.setRowCount(0);
            return;
        }
        
        modelSales.setRowCount(0); // Clear previous results

        try {
            SupabaseServiceREST supabase = new SupabaseServiceREST(
                "https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1",
                "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n"
            );

            // Mapear cierres.dineroid -> cierres.host para mostrar nombre de equipo
            java.util.Map<String, String> dineroidToHost = new java.util.HashMap<>();
            for (Map<String, Object> cierre : supabase.fetchData("cierres")) {
                Object dineroid = cierre.get("dineroid");
                Object host = cierre.get("host");
                if (dineroid != null && host != null) dineroidToHost.put(dineroid.toString(), host.toString());
            }

            // Mapear usuarios.id -> usuarios.sucursal_nombre (fuente de verdad para sucursal)
            java.util.Map<String, String> userIdToSucursal = new java.util.HashMap<>();
            for (Map<String, Object> usuario : supabase.fetchData("usuarios")) {
                Object userId = usuario.get("id");
                Object sucursalNombre = usuario.get("sucursal_nombre");
                if (userId != null && sucursalNombre != null) {
                    userIdToSucursal.put(userId.toString().trim(), sucursalNombre.toString());
                }
            }

            List<Map<String, Object>> ventas = supabase.fetchData("ventas");
            List<Object[]> rows = new ArrayList<>();
            for (Map<String, Object> venta : ventas) {
                Object id = venta.get("id");
                Object fechaIso = venta.get("fechaventa");
                Object totalVal = venta.get("total");
                Object cajaVal = venta.get("caja");
                Object vendedorId = venta.get("vendedorid");
                Object vendedorNombre = venta.get("vendedornombre");

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
                String host = null;
                // Obtener host del cierre si existe coincidencia
                if (caja != null && dineroidToHost.containsKey(caja)) {
                    host = dineroidToHost.get(caja);
                }
                
                // Obtener sucursal del vendedor usando usuarios.id
                String sucursal = null;
                if (vendedorId != null) {
                    String uid = vendedorId.toString().trim();
                    sucursal = userIdToSucursal.get(uid);
                }
                
                // Formatear como "sucursal - host" o solo host si no hay sucursal
                if (sucursal != null && host != null) {
                    caja = sucursal + " - " + host;
                } else if (host != null) {
                    caja = host;
                } else if (sucursal != null) {
                    caja = sucursal;
                }
                // Si no hay ni sucursal ni host, caja queda con el valor original

                // Formatear vendedor/nombre: "vendedorid - vendedornombre"
                String vendedorDisplay = "";
                if (vendedorId != null && vendedorNombre != null) {
                    vendedorDisplay = vendedorId.toString().trim() + " - " + vendedorNombre.toString();
                } else if (vendedorId != null) {
                    vendedorDisplay = vendedorId.toString().trim();
                } else if (vendedorNombre != null) {
                    vendedorDisplay = vendedorNombre.toString();
                }

                rows.add(new Object[] { id, fechaTs, total, caja, vendedorDisplay });
            }
            rows.sort(Comparator.comparing((Object[] r) -> (Timestamp) r[1], Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            for (Object[] r : rows) modelSales.addRow(r);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: Handle exception gracefully
        }
    }

    private void loadCashClosures() throws BasicException {
        // Verificar validación antes de cargar
        if (!adminValidated) {
            modelCashClosures.setRowCount(0);
            return;
        }
        
        modelCashClosures.setRowCount(0); // Clear previous results

        try {
            SupabaseServiceREST supabase = new SupabaseServiceREST(
                "https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1",
                "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n"
            );

            // Mapear usuarios.id -> usuarios.sucursal_nombre
            java.util.Map<String, String> userIdToSucursal = new java.util.HashMap<>();
            // Mapear usuarios.tarjeta (vendedorid) -> usuarios.nombre
            java.util.Map<String, String> tarjetaToNombre = new java.util.HashMap<>();
            for (Map<String, Object> usuario : supabase.fetchData("usuarios")) {
                Object userId = usuario.get("id");
                Object sucursalNombre = usuario.get("sucursal_nombre");
                if (userId != null && sucursalNombre != null) {
                    userIdToSucursal.put(userId.toString().trim(), sucursalNombre.toString());
                }
                // Mapear tarjeta (código de validación) -> nombre
                Object tarjeta = usuario.get("tarjeta");
                Object nombre = usuario.get("nombre");
                if (tarjeta != null && nombre != null) {
                    tarjetaToNombre.put(tarjeta.toString().trim(), nombre.toString());
                }
            }

            // Mapear ventas.caja -> ventas.vendedorid (tomar el primero encontrado por cada caja)
            java.util.Map<String, String> cajaToVendedorId = new java.util.HashMap<>();
            for (Map<String, Object> venta : supabase.fetchData("ventas")) {
                Object caja = venta.get("caja");
                Object vendedorId = venta.get("vendedorid");
                if (caja != null && vendedorId != null && !cajaToVendedorId.containsKey(caja.toString())) {
                    cajaToVendedorId.put(caja.toString().trim(), vendedorId.toString().trim());
                }
            }

            // Obtener todos los cierres
            List<Map<String, Object>> cierres = supabase.fetchData("cierres");
            List<Object[]> rows = new ArrayList<>();
            
            for (Map<String, Object> cierre : cierres) {
                Object id = cierre.get("id");
                Object fechaInicio = cierre.get("fechainicio");
                Object fechaFin = cierre.get("fechafin");
                Object efectivoVal = cierre.get("cierre_efectivo");
                Object tarjetaVal = cierre.get("cierre_card");
                Object initialAmountVal = cierre.get("initial_amount");
                Object dineroid = cierre.get("dineroid");
                Object faltanteCierreVal = cierre.get("faltante_cierre");
                Object sobranteCierreVal = cierre.get("sobrante_cierre");

                // Parsear fechas desde timestamp en milisegundos
                Timestamp fechaInicioTs = null;
                Timestamp fechaFinTs = null;
                
                // Procesar fecha inicio (milisegundos -> Timestamp)
                if (fechaInicio != null) {
                    try {
                        long millis;
                        if (fechaInicio instanceof Number) {
                            millis = ((Number) fechaInicio).longValue();
                        } else if (fechaInicio instanceof String) {
                            millis = Long.parseLong((String) fechaInicio);
                        } else {
                            millis = 0;
                        }
                        if (millis > 0) {
                            fechaInicioTs = new Timestamp(millis);
                        }
                    } catch (Exception ignore) {}
                }
                
                // Procesar fecha fin (milisegundos -> Timestamp) - REQUERIDA
                if (fechaFin != null) {
                    try {
                        long millis;
                        if (fechaFin instanceof Number) {
                            millis = ((Number) fechaFin).longValue();
                        } else if (fechaFin instanceof String) {
                            millis = Long.parseLong((String) fechaFin);
                        } else {
                            millis = 0;
                        }
                        if (millis > 0) {
                            fechaFinTs = new Timestamp(millis);
                        }
                    } catch (Exception ignore) {}
                }
                
                // Solo mostrar filas que tengan fechafin
                if (fechaFinTs == null) {
                    continue; // Saltar esta fila si no tiene fecha fin
                }

                // Parsear monto inicial
                Double montoInicial = null;
                if (initialAmountVal instanceof Number) {
                    montoInicial = ((Number) initialAmountVal).doubleValue();
                } else if (initialAmountVal instanceof String) {
                    try { montoInicial = Double.valueOf((String) initialAmountVal); } catch (Exception ignore) {}
                }

                // Parsear efectivo y tarjeta
                Double efectivo = null;
                if (efectivoVal instanceof Number) {
                    efectivo = ((Number) efectivoVal).doubleValue();
                } else if (efectivoVal instanceof String) {
                    try { efectivo = Double.valueOf((String) efectivoVal); } catch (Exception ignore) {}
                }

                Double tarjeta = null;
                if (tarjetaVal instanceof Number) {
                    tarjeta = ((Number) tarjetaVal).doubleValue();
                } else if (tarjetaVal instanceof String) {
                    try { tarjeta = Double.valueOf((String) tarjetaVal); } catch (Exception ignore) {}
                }

                // Calcular total
                Double total = null;
                if (efectivo != null && tarjeta != null && montoInicial != null) {
                    total = efectivo + tarjeta + montoInicial;
                } else if (efectivo != null) {
                    total = efectivo;
                } else if (tarjeta != null) {
                    total = tarjeta;
                }
                
                // Parsear faltante_cierre y sobrante_cierre
                Double faltanteCierre = null;
                if (faltanteCierreVal instanceof Number) {
                    faltanteCierre = ((Number) faltanteCierreVal).doubleValue();
                    if (faltanteCierre == 0.0) faltanteCierre = null; // Mostrar null si es 0
                } else if (faltanteCierreVal instanceof String) {
                    try {
                        faltanteCierre = Double.valueOf((String) faltanteCierreVal);
                        if (faltanteCierre == 0.0) faltanteCierre = null;
                    } catch (Exception ignore) {}
                }
                
                Double sobranteCierre = null;
                if (sobranteCierreVal instanceof Number) {
                    sobranteCierre = ((Number) sobranteCierreVal).doubleValue();
                    if (sobranteCierre == 0.0) sobranteCierre = null; // Mostrar null si es 0
                } else if (sobranteCierreVal instanceof String) {
                    try {
                        sobranteCierre = Double.valueOf((String) sobranteCierreVal);
                        if (sobranteCierre == 0.0) sobranteCierre = null;
                    } catch (Exception ignore) {}
                }

                // Obtener sucursal: cierres.dineroid -> ventas.caja -> ventas.vendedorid -> usuarios.id -> usuarios.sucursal_nombre
                String sucursal = null;
                if (dineroid != null) {
                    String cajaStr = dineroid.toString().trim();
                    String vendedorId = cajaToVendedorId.get(cajaStr);
                    if (vendedorId != null) {
                        sucursal = userIdToSucursal.get(vendedorId);
                    }
                }

                // Obtener nombre del vendedor: cierres.dineroid -> ventas.caja -> ventas.vendedorid -> usuarios.tarjeta -> usuarios.nombre
                String nombreVendedor = "";
                if (dineroid != null) {
                    String cajaStr = dineroid.toString().trim();
                    String vendedorId = cajaToVendedorId.get(cajaStr);
                    if (vendedorId != null) {
                        // vendedorId es el código de validación (tarjeta), buscar en el mapa tarjeta -> nombre
                        nombreVendedor = tarjetaToNombre.get(vendedorId);
                        if (nombreVendedor == null) {
                            nombreVendedor = ""; // Si no se encuentra, dejar vacío
                        }
                    }
                }

                rows.add(new Object[]{
                    nombreVendedor, // Reemplazar id por nombre del vendedor
                    fechaInicioTs,
                    fechaFinTs,
                    montoInicial,
                    efectivo,
                    tarjeta,
                    total,
                    faltanteCierre, // Faltante
                    sobranteCierre, // Sobrante
                    sucursal != null ? sucursal : ""
                });
            }

            // Ordenar por fecha fin descendente
            rows.sort(Comparator.comparing((Object[] r) -> (Timestamp) r[2], Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            
            for (Object[] r : rows) {
                modelCashClosures.addRow(r);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: Handle exception gracefully
        }
    }
}
