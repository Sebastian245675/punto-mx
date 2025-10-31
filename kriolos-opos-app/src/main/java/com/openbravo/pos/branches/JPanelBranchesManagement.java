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
    private JPanel jPanelCashClosures;
    private JTextField jTextFieldBranchName;
    private JTable jTableBranches;
    private DefaultTableModel modelBranches;
    private JTable jTableSales;
    private DefaultTableModel modelSales;
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
        loadCashClosures();
    }

    @Override
    public boolean deactivate() {
        return true;
    }

    private void refreshAllData() throws BasicException {
        searchBranches(); // No lanza BasicException, solo maneja Exception
        loadSales(); // No lanza BasicException, solo maneja Exception
        loadCashClosures();
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

        String[] salesColumns = {"ID Venta", "Fecha", "Total", "Sucursal/Caja"};
        modelSales = new DefaultTableModel(salesColumns, 0);
        jTableSales = new JTable(modelSales);

        String[] cashClosureColumns = {"ID Cierre", "Fecha Inicio", "Fecha Fin", "Efectivo", "Tarjeta", "Total", "Sucursal"};
        modelCashClosures = new DefaultTableModel(cashClosureColumns, 0);
        jTableCashClosures = new JTable(modelCashClosures);

        setLayout(new BorderLayout());

        // Panel superior con título y botón actualizar
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("");
        titleLabel.setFont(titleLabel.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JButton refreshButton = new JButton("Actualizar información");
        refreshButton.addActionListener(e -> {
            try {
                refreshAllData();
            } catch (BasicException ex) {
                ex.printStackTrace();
                // TODO: Show error message to user
            }
        });
        headerPanel.add(refreshButton, BorderLayout.WEST);
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

        // Panel Ver Ventas
        jPanelSales.setLayout(new BorderLayout());
        jPanelSales.add(new JScrollPane(jTableSales), BorderLayout.CENTER);
        jTabbedPane1.addTab("Ver Ventas", jPanelSales);

        // Panel Ver Cierres de Caja
        jPanelCashClosures.setLayout(new BorderLayout());
        jPanelCashClosures.add(new JScrollPane(jTableCashClosures), BorderLayout.CENTER);
        jTabbedPane1.addTab("Ver Cierres de Caja", jPanelCashClosures);

        add(jTabbedPane1, BorderLayout.CENTER);
    }

    private void searchBranches() {
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

                rows.add(new Object[] { id, fechaTs, total, caja });
            }
            rows.sort(Comparator.comparing((Object[] r) -> (Timestamp) r[1], Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            for (Object[] r : rows) modelSales.addRow(r);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: Handle exception gracefully
        }
    }

    private void loadCashClosures() throws BasicException {
        modelCashClosures.setRowCount(0); // Clear previous results

        try {
            SupabaseServiceREST supabase = new SupabaseServiceREST(
                "https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1",
                "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n"
            );

            // Mapear usuarios.id -> usuarios.sucursal_nombre
            java.util.Map<String, String> userIdToSucursal = new java.util.HashMap<>();
            for (Map<String, Object> usuario : supabase.fetchData("usuarios")) {
                Object userId = usuario.get("id");
                Object sucursalNombre = usuario.get("sucursal_nombre");
                if (userId != null && sucursalNombre != null) {
                    userIdToSucursal.put(userId.toString().trim(), sucursalNombre.toString());
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
                Object dineroid = cierre.get("dineroid");

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
                if (efectivo != null && tarjeta != null) {
                    total = efectivo + tarjeta;
                } else if (efectivo != null) {
                    total = efectivo;
                } else if (tarjeta != null) {
                    total = tarjeta;
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

                rows.add(new Object[]{
                    id,
                    fechaInicioTs,
                    fechaFinTs,
                    efectivo,
                    tarjeta,
                    total,
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
