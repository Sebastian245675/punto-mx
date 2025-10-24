/*
 * Copyright (C) 2025 Sebastian
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.openbravo.pos.config;

import com.openbravo.pos.firebase.FirebaseServiceREST;
import com.openbravo.pos.forms.AppConfig;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Panel para ver todos los usuarios de la colección roles en Firebase
 * @author Sebastian 2025
 */
public class JPanelFirebaseUsers extends JDialog {
    
    private static final Logger LOGGER = Logger.getLogger(JPanelFirebaseUsers.class.getName());
    
    private JTable tablaUsuarios;
    private DefaultTableModel modeloTabla;
    private JLabel lblEstado;
    private JLabel lblTotal;
    private JButton btnActualizar;
    private JButton btnCerrar;
    private AppConfig config;
    
    /**
     * Constructor
     * @param parent Ventana padre
     * @param config Configuración de Firebase
     */
    public JPanelFirebaseUsers(Window parent, AppConfig config) {
        super(parent, "Usuarios Firebase - Colección Roles", ModalityType.APPLICATION_MODAL);
        this.config = config;
        initComponents();
        cargarUsuarios();
    }
    
    private void initComponents() {
        setSize(900, 600);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // Panel superior con título y estado
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBackground(new Color(52, 152, 219));
        panelSuperior.setPreferredSize(new Dimension(0, 70));
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel lblTitulo = new JLabel("👥 Usuarios Firebase - Colección: roles");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 20));
        lblTitulo.setForeground(Color.WHITE);
        
        lblEstado = new JLabel("⚪ Listo");
        lblEstado.setFont(new Font("Arial", Font.PLAIN, 14));
        lblEstado.setForeground(Color.WHITE);
        
        JPanel panelTituloEstado = new JPanel(new BorderLayout());
        panelTituloEstado.setOpaque(false);
        panelTituloEstado.add(lblTitulo, BorderLayout.WEST);
        panelTituloEstado.add(lblEstado, BorderLayout.EAST);
        
        panelSuperior.add(panelTituloEstado, BorderLayout.CENTER);
        
        add(panelSuperior, BorderLayout.NORTH);
        
        // Crear tabla con todas las columnas posibles
        String[] columnas = {
            "ID Doc", "Usuario", "Nombre", "Rol", "Email", 
            "Teléfono", "Activo", "Fecha Creación", "Última Modificación"
        };
        
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 6) { // Columna "Activo"
                    return Boolean.class;
                }
                return String.class;
            }
        };
        
        tablaUsuarios = new JTable(modeloTabla);
        tablaUsuarios.setFont(new Font("Arial", Font.PLAIN, 13));
        tablaUsuarios.setRowHeight(30);
        tablaUsuarios.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        tablaUsuarios.getTableHeader().setBackground(new Color(52, 152, 219));
        tablaUsuarios.getTableHeader().setForeground(Color.WHITE);
        tablaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Ajustar anchos de columnas
        tablaUsuarios.getColumnModel().getColumn(0).setPreferredWidth(100); // ID Doc
        tablaUsuarios.getColumnModel().getColumn(1).setPreferredWidth(100); // Usuario
        tablaUsuarios.getColumnModel().getColumn(2).setPreferredWidth(200); // Nombre
        tablaUsuarios.getColumnModel().getColumn(3).setPreferredWidth(100); // Rol
        tablaUsuarios.getColumnModel().getColumn(4).setPreferredWidth(180); // Email
        tablaUsuarios.getColumnModel().getColumn(5).setPreferredWidth(120); // Teléfono
        tablaUsuarios.getColumnModel().getColumn(6).setPreferredWidth(80);  // Activo
        tablaUsuarios.getColumnModel().getColumn(7).setPreferredWidth(150); // Fecha Creación
        tablaUsuarios.getColumnModel().getColumn(8).setPreferredWidth(150); // Última Mod
        
        // Renderizador para centrar algunas columnas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tablaUsuarios.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Usuario
        tablaUsuarios.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Rol
        tablaUsuarios.getColumnModel().getColumn(6).setCellRenderer(centerRenderer); // Activo
        
        JScrollPane scrollPane = new JScrollPane(tablaUsuarios);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);
        
        // Panel inferior con estadísticas y botones
        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Estadísticas
        JPanel panelEstadisticas = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        panelEstadisticas.setBackground(new Color(245, 245, 245));
        
        lblTotal = new JLabel("Total Usuarios: 0");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 14));
        panelEstadisticas.add(lblTotal);
        
        panelInferior.add(panelEstadisticas, BorderLayout.WEST);
        
        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        
        btnActualizar = new JButton("🔄 Actualizar");
        btnActualizar.setFont(new Font("Arial", Font.PLAIN, 14));
        btnActualizar.addActionListener(e -> cargarUsuarios());
        panelBotones.add(btnActualizar);
        
        btnCerrar = new JButton("Cerrar");
        btnCerrar.setFont(new Font("Arial", Font.PLAIN, 14));
        btnCerrar.addActionListener(e -> dispose());
        panelBotones.add(btnCerrar);
        
        panelInferior.add(panelBotones, BorderLayout.EAST);
        
        add(panelInferior, BorderLayout.SOUTH);
    }
    
    /**
     * Carga los usuarios desde Firebase
     */
    private void cargarUsuarios() {
        // Deshabilitar botón mientras carga
        btnActualizar.setEnabled(false);
        lblEstado.setText("🔄 Cargando...");
        lblEstado.setForeground(Color.YELLOW);
        
        // Limpiar tabla
        modeloTabla.setRowCount(0);
        
        // Ejecutar en un hilo separado para no bloquear la UI
        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            private boolean exito = false;
            
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                try {
                    LOGGER.info("Iniciando descarga de usuarios desde Firebase...");
                    
                    FirebaseServiceREST service = FirebaseServiceREST.getInstance();
                    
                    // Verificar que Firebase esté inicializado
                    if (!service.isInitialized()) {
                        LOGGER.warning("Firebase no está inicializado, intentando inicializar...");
                        service.initialize(config);
                    }
                    
                    // Descargar usuarios desde Firebase (colección roles)
                    List<Map<String, Object>> usuarios = service.downloadRoles().get();
                    exito = true;
                    
                    LOGGER.info("Usuarios descargados: " + (usuarios != null ? usuarios.size() : 0));
                    
                    return usuarios;
                    
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error cargando usuarios desde Firebase", e);
                    exito = false;
                    return null;
                }
            }
            
            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> usuarios = get();
                    
                    if (exito && usuarios != null && !usuarios.isEmpty()) {
                        mostrarUsuarios(usuarios);
                        lblEstado.setText("✅ Actualizado");
                        lblEstado.setForeground(new Color(46, 204, 113));
                    } else {
                        if (usuarios != null && usuarios.isEmpty()) {
                            lblEstado.setText("⚠️ Sin datos");
                            lblEstado.setForeground(Color.ORANGE);
                            JOptionPane.showMessageDialog(
                                JPanelFirebaseUsers.this,
                                "No se encontraron usuarios en la colección roles de Firebase.\n" +
                                "Verifique que la colección exista y tenga datos.",
                                "Sin datos",
                                JOptionPane.WARNING_MESSAGE
                            );
                        } else {
                            lblEstado.setText("❌ Error");
                            lblEstado.setForeground(Color.RED);
                            JOptionPane.showMessageDialog(
                                JPanelFirebaseUsers.this,
                                "Error al cargar usuarios desde Firebase.\n" +
                                "Verifique la conexión y configuración.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                            );
                        }
                        
                        // Actualizar estadísticas con 0
                        lblTotal.setText("Total Usuarios: 0");
                    }
                    
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error procesando usuarios", e);
                    lblEstado.setText("❌ Error");
                    lblEstado.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(
                        JPanelFirebaseUsers.this,
                        "Error inesperado: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
                
                btnActualizar.setEnabled(true);
            }
        };
        
        worker.execute();
    }
    
    /**
     * Muestra los usuarios en la tabla
     */
    private void mostrarUsuarios(List<Map<String, Object>> usuarios) {
        for (Map<String, Object> usuario : usuarios) {
            try {
                // Obtener datos del usuario
                String idDoc = obtenerValor(usuario, "id", "");
                String usuarioId = obtenerValor(usuario, "usuario", "");
                String nombre = obtenerValor(usuario, "nombre", "Sin nombre");
                String rol = obtenerValor(usuario, "rol", "Usuario");
                String email = obtenerValor(usuario, "email", "");
                String telefono = obtenerValor(usuario, "telefono", "");
                
                // Verificar si está activo
                Object activoObj = usuario.get("activo");
                Boolean activo = true; // Por defecto activo
                if (activoObj != null) {
                    if (activoObj instanceof Boolean) {
                        activo = (Boolean) activoObj;
                    } else if (activoObj instanceof String) {
                        activo = "true".equalsIgnoreCase((String) activoObj) || 
                                 "1".equals(activoObj) || 
                                 "si".equalsIgnoreCase((String) activoObj);
                    }
                }
                
                // Fechas
                String fechaCreacion = obtenerValor(usuario, "fechaCreacion", "N/A");
                String ultimaMod = obtenerValor(usuario, "ultimaModificacion", "N/A");
                
                // Formatear fechas si son muy largas
                fechaCreacion = formatearFecha(fechaCreacion);
                ultimaMod = formatearFecha(ultimaMod);
                
                // Agregar fila a la tabla
                Object[] fila = {
                    idDoc,
                    usuarioId,
                    nombre,
                    rol,
                    email,
                    telefono,
                    activo,
                    fechaCreacion,
                    ultimaMod
                };
                
                modeloTabla.addRow(fila);
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error procesando usuario", e);
            }
        }
        
        // Actualizar estadísticas
        lblTotal.setText("Total Usuarios: " + usuarios.size());
    }
    
    /**
     * Obtiene un valor del mapa con valor por defecto
     */
    private String obtenerValor(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }
    
    /**
     * Formatea una fecha si es muy larga (ISO 8601)
     */
    private String formatearFecha(String fecha) {
        if (fecha == null || fecha.equals("N/A")) {
            return fecha;
        }
        
        // Si es fecha ISO 8601, extraer solo fecha y hora
        if (fecha.length() > 19 && fecha.contains("T")) {
            return fecha.substring(0, 19).replace("T", " ");
        }
        
        return fecha;
    }
}
