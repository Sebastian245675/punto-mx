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

package com.openbravo.pos.customers;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ListCellRendererBasic;
import com.openbravo.data.loader.ComparatorCreator;
import com.openbravo.data.loader.Vectorer;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.data.user.ListProvider;
import com.openbravo.data.user.ListProviderCreator;
import com.openbravo.data.user.SaveProvider;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.panels.JPanelTable;
import javax.swing.ListCellRenderer;
import java.awt.Component;
// Sebastian - Importaciones para el sistema de puntos
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.data.loader.Session;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JOptionPane;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Color;

/**
 *
 * @author adrianromero
 */
public class CustomersPanel extends JPanelTable {

    private static final long serialVersionUID = 1L;
    
    private DataLogicCustomers dlCustomers;
    private CustomersView jeditor;
    
    // Sebastian - Variables para el sistema de puntos
    private PuntosDataLogic puntosDataLogic;
    private DataLogicSales dlSales;
    
    public CustomersPanel() {}

    @Override
    protected void init() {
        this.jeditor = new CustomersView(app, dirty);
        this.dlCustomers  = (DataLogicCustomers) app.getBean("com.openbravo.pos.customers.DataLogicCustomers");
        
        // Sebastian - Inicializar sistema de puntos
        try {
            this.dlSales = (DataLogicSales) app.getBean("com.openbravo.pos.forms.DataLogicSales");
            Session session = app.getSession();
            this.puntosDataLogic = new PuntosDataLogic(session);
            
            // Inicializar tablas si no existen
            this.puntosDataLogic.initTables();
        } catch (BasicException e) {
            System.err.println("Error inicializando sistema de puntos: " + e.getMessage());
        }
        
        // Aplicar tamaño de letra gigante recursivamente a todo el panel de Clientes
        setLargeFont(this);
    }
    
    /**
     * Sebastian - Asegura que dlCustomers esté inicializado
     */
    private void ensureDlCustomersInitialized() {
        if (this.dlCustomers == null && app != null) {
            try {
                this.dlCustomers = (DataLogicCustomers) app.getBean("com.openbravo.pos.customers.DataLogicCustomers");
            } catch (Exception e) {
                System.err.println("Error inicializando dlCustomers: " + e.getMessage());
            }
        }
    }

    @Override
    public void activate() throws BasicException {     
        ensureDlCustomersInitialized(); // Asegurar inicialización antes de usar
        if (dlCustomers != null) {
            try {
                dlCustomers.refreshLocalCustomersFromSupabase();
            } catch (Exception e) {
                System.err.println("Error al sincronizar clientes desde Supabase: " + e.getMessage());
            }
        }
        super.activate();
        jeditor.activate();     
    }

    @Override
    public ListProvider getListProvider() {
        ensureDlCustomersInitialized(); // Asegurar inicialización antes de usar
        if (dlCustomers == null) {
            throw new IllegalStateException("DataLogicCustomers no está inicializado");
        }
        return new ListProviderCreator(dlCustomers.getTableCustomers());
    }

    @Override
    public SaveProvider getSaveProvider() {
        ensureDlCustomersInitialized(); // Asegurar inicialización antes de usar
        if (dlCustomers == null) {
            throw new IllegalStateException("DataLogicCustomers no está inicializado");
        }
        return dlCustomers.getCustomerSaveProvider();
        /*return new DefaultSaveProvider(dlCustomers.getTableCustomers(), new int[] {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,            
            15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26});  
       */
    }

    @Override
    public Vectorer getVectorer() {
        ensureDlCustomersInitialized(); // Asegurar inicialización antes de usar
        if (dlCustomers == null) {
            throw new IllegalStateException("DataLogicCustomers no está inicializado");
        }
        return dlCustomers.getTableCustomers().getVectorerBasic(new int[]{1, 2, 3, 4});
    }

    @Override
    public ComparatorCreator getComparatorCreator() {
        ensureDlCustomersInitialized(); // Asegurar inicialización antes de usar
        if (dlCustomers == null) {
            throw new IllegalStateException("DataLogicCustomers no está inicializado");
        }
        return dlCustomers.getTableCustomers().getComparatorCreator(new int[] {1, 2, 3, 4});
    }

    @Override
    public ListCellRenderer getListCellRenderer() {
        ensureDlCustomersInitialized(); // Asegurar inicialización antes de usar
        if (dlCustomers == null) {
            throw new IllegalStateException("DataLogicCustomers no está inicializado");
        }
        return new ListCellRendererBasic(dlCustomers.getTableCustomers().getRenderStringBasic(new int[]{3}));
    }
    
    // Sebastian - Agregar botón de puntos y exportación a Excel en la barra de herramientas
    @Override
    public Component getToolbarExtras() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        
        JButton btnPuntos = new JButton("Puntos");
        btnPuntos.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        btnPuntos.setBackground(Color.RED);
        btnPuntos.setForeground(Color.WHITE);
        btnPuntos.setFocusPainted(false);
        btnPuntos.setToolTipText("Gestionar sistema de puntos de clientes");
        btnPuntos.addActionListener(e -> abrirVentanaPuntos());
        panel.add(btnPuntos);

        JButton btnExportarClientes = new JButton("Exportar Excel");
        btnExportarClientes.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        btnExportarClientes.setBackground(new Color(40, 167, 69));
        btnExportarClientes.setForeground(Color.WHITE);
        btnExportarClientes.setFocusPainted(false);
        btnExportarClientes.setToolTipText("Exportar toda la información de los clientes a Excel");
        btnExportarClientes.addActionListener(e -> btnExportarClientesActionPerformed(e));
        panel.add(btnExportarClientes);
        
        return panel;
    }
    
    /**
     * Sebastian - Abre la ventana de configuración de puntos REAL - Sin ejemplos
     */
    private void abrirVentanaPuntos() {
        try {
            // Cargar configuración actual
            PuntosConfiguracion configActual = puntosDataLogic.getConfiguracionActiva();
            
            JDialog ventanaPuntos = new JDialog();
            ventanaPuntos.setTitle("Sistema de Puntos - Configuración Real");
            ventanaPuntos.setSize(650, 500);
            ventanaPuntos.setLocationRelativeTo(this);
            ventanaPuntos.setModal(true);
            
            JPanel panelPrincipal = new JPanel(new BorderLayout());
            panelPrincipal.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            // Panel de título
            JPanel panelTitulo = new JPanel();
            JLabel titulo = new JLabel("⚙️ CONFIGURACIÓN SISTEMA DE PUNTOS");
            titulo.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));
            titulo.setForeground(Color.RED);
            panelTitulo.add(titulo);
            
            // Panel principal de configuración
            JPanel panelConfiguracion = new JPanel(new GridLayout(8, 2, 15, 15));
            panelConfiguracion.setBorder(javax.swing.BorderFactory.createTitledBorder("Configuración del Sistema"));
            
            // Configuración principal
            panelConfiguracion.add(new JLabel("💰 Monto requerido (MX):"));
            JTextField txtMontoRequerido = new JTextField(String.valueOf(configActual.getMontoPorPunto()));
            txtMontoRequerido.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            txtMontoRequerido.setToolTipText("Ejemplo: 400.00 para otorgar puntos cada $400 MX");
            panelConfiguracion.add(txtMontoRequerido);
            
            panelConfiguracion.add(new JLabel("⭐ Puntos a otorgar:"));
            JTextField txtCantidadPuntos = new JTextField(String.valueOf(configActual.getPuntosOtorgados()));
            txtCantidadPuntos.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            txtCantidadPuntos.setToolTipText("Ejemplo: 10 puntos por cada $400 MX");
            panelConfiguracion.add(txtCantidadPuntos);
            
            panelConfiguracion.add(new JLabel("💱 Moneda:"));
            JTextField txtMoneda = new JTextField(configActual.getMoneda());
            txtMoneda.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            panelConfiguracion.add(txtMoneda);
            
            // Sebastian - Campo para límite diario de puntos
            panelConfiguracion.add(new JLabel("🚫 Límite diario de puntos:"));
            JTextField txtLimiteDiario = new JTextField(String.valueOf(configActual.getLimiteDiarioPuntos()));
            txtLimiteDiario.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            txtLimiteDiario.setToolTipText("Máximo de puntos que un cliente puede ganar por día");
            panelConfiguracion.add(txtLimiteDiario);
            
            panelConfiguracion.add(new JLabel("✅ Sistema activado:"));
            javax.swing.JCheckBox chkSistemaActivo = new javax.swing.JCheckBox("Activar sistema automático en ventas");
            chkSistemaActivo.setSelected(configActual.isSistemaActivo());
            chkSistemaActivo.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            panelConfiguracion.add(chkSistemaActivo);
            
            // Ejemplo en tiempo real
            panelConfiguracion.add(new JLabel("📊 Configuración actual:"));
            JLabel lblEjemplo = new JLabel(String.format("$%.2f %s = %d puntos", 
                configActual.getMontoPorPunto(), configActual.getMoneda(), configActual.getPuntosOtorgados()));
            lblEjemplo.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            lblEjemplo.setForeground(Color.BLUE);
            panelConfiguracion.add(lblEjemplo);
            
            // Botones de acción (negrita y tamaño 14 para mejor lectura sobre colores vivos)
            JButton btnActualizarEjemplo = new JButton("🔄 Actualizar Vista");
            btnActualizarEjemplo.setBackground(Color.ORANGE);
            btnActualizarEjemplo.setForeground(Color.BLACK);
            btnActualizarEjemplo.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            panelConfiguracion.add(btnActualizarEjemplo);
            
            JButton btnGuardarConfig = new JButton("💾 Guardar Configuración");
            btnGuardarConfig.setBackground(Color.GREEN);
            btnGuardarConfig.setForeground(Color.BLACK);
            btnGuardarConfig.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            panelConfiguracion.add(btnGuardarConfig);
            
            // Botón para recrear tablas (troubleshooting)
            JButton btnRecrearTablas = new JButton("🔧 Recrear Tablas");
            btnRecrearTablas.setBackground(Color.RED);
            btnRecrearTablas.setForeground(Color.WHITE);
            btnRecrearTablas.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            btnRecrearTablas.setToolTipText("Fuerza la recreación de las tablas de puntos (usar solo si hay problemas)");
            panelConfiguracion.add(btnRecrearTablas);
            
            // Panel de asignación manual de puntos
            JPanel panelAsignacion = new JPanel(new GridLayout(6, 2, 15, 15));
            panelAsignacion.setBorder(javax.swing.BorderFactory.createTitledBorder("Asignación Manual de Puntos"));
            
            panelAsignacion.add(new JLabel("🆔 ID del Cliente:"));
            JTextField txtClienteId = new JTextField();
            txtClienteId.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            txtClienteId.setToolTipText("ID único del cliente en el sistema");
            panelAsignacion.add(txtClienteId);
            
            panelAsignacion.add(new JLabel("📦 Producto/Descripción:"));
            JTextField txtDescripcion = new JTextField();
            txtDescripcion.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            txtDescripcion.setToolTipText("Nombre del producto o descripción de la asignación");
            panelAsignacion.add(txtDescripcion);
            
            panelAsignacion.add(new JLabel("⭐ Puntos a asignar:"));
            JTextField txtPuntosAsignar = new JTextField("0");
            txtPuntosAsignar.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            txtPuntosAsignar.setToolTipText("Cantidad específica de puntos para este producto");
            panelAsignacion.add(txtPuntosAsignar);
            
            panelAsignacion.add(new JLabel("🔍 Consultar Cliente:"));
            JButton btnConsultarCliente = new JButton("Ver Puntos Actuales");
            btnConsultarCliente.setBackground(Color.BLUE);
            btnConsultarCliente.setForeground(Color.WHITE);
            btnConsultarCliente.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 11));
            panelAsignacion.add(btnConsultarCliente);
            
            panelAsignacion.add(new JLabel("➕ Acción:"));
            JButton btnAsignarPuntos = new JButton("Asignar Puntos");
            btnAsignarPuntos.setBackground(Color.GREEN);
            btnAsignarPuntos.setForeground(Color.WHITE);
            btnAsignarPuntos.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            panelAsignacion.add(btnAsignarPuntos);
            
            // Panel informativo
            JPanel panelInfo = new JPanel(new BorderLayout());
            panelInfo.setBorder(javax.swing.BorderFactory.createTitledBorder("Información del Sistema"));
            
            JTextArea txtInfo = new JTextArea(4, 50);
            txtInfo.setEditable(false);
            txtInfo.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            txtInfo.setText(
                "FUNCIONAMIENTO:\n" +
                "• El sistema se conectará automáticamente con el módulo de ventas\n" +
                "• Los puntos se asignarán automáticamente según la configuración establecida\n" +
                "• También puedes asignar puntos manualmente por productos específicos\n" +
                "• Los datos se guardan en la base de datos del sistema"
            );
            txtInfo.setBackground(new java.awt.Color(245, 245, 245));
            
            javax.swing.JScrollPane scrollInfo = new javax.swing.JScrollPane(txtInfo);
            panelInfo.add(scrollInfo, BorderLayout.CENTER);
            
            // Panel de botones principales
            JPanel panelBotones = new JPanel(new FlowLayout());
            
            JButton btnCerrar = new JButton("❌ Cerrar");
            btnCerrar.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            
            panelBotones.add(btnCerrar);
            
            // EVENTOS DE BOTONES REALES
            
            // Actualizar vista en tiempo real
            btnActualizarEjemplo.addActionListener(e -> {
                try {
                    double monto = Double.parseDouble(txtMontoRequerido.getText());
                    int puntos = Integer.parseInt(txtCantidadPuntos.getText());
                    String moneda = txtMoneda.getText();
                    lblEjemplo.setText(String.format("$%.2f %s = %d puntos", monto, moneda, puntos));
                } catch (NumberFormatException ex) {
                    lblEjemplo.setText("❌ Error en valores numéricos");
                }
            });
            
            // Guardar configuración REAL
            btnGuardarConfig.addActionListener(e -> {
                try {
                    // Parsear monto aceptando coma o punto decimal
                    String montoTexto = txtMontoRequerido.getText().trim().replace(",", ".");
                    double monto = Double.parseDouble(montoTexto);
                    
                    int puntos = Integer.parseInt(txtCantidadPuntos.getText().trim());
                    String moneda = txtMoneda.getText().trim();
                    int limiteDiario = Integer.parseInt(txtLimiteDiario.getText().trim()); // Sebastian - Nuevo campo
                    boolean activo = chkSistemaActivo.isSelected();
                    
                    if (monto <= 0 || puntos <= 0 || limiteDiario <= 0 || moneda.isEmpty()) {
                        JOptionPane.showMessageDialog(ventanaPuntos,
                            "❌ Todos los campos deben tener valores válidos:\n• Monto > 0\n• Puntos > 0\n• Límite diario > 0\n• Moneda no vacía",
                            "Datos Inválidos",
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    configActual.setMontoPorPunto(monto);
                    configActual.setPuntosOtorgados(puntos);
                    configActual.setMoneda(moneda);
                    configActual.setLimiteDiarioPuntos(limiteDiario); // Sebastian - Nuevo campo
                    configActual.setSistemaActivo(activo);
                    
                    puntosDataLogic.updateConfiguracion(configActual);
                    
                    JOptionPane.showMessageDialog(ventanaPuntos,
                        String.format("✅ Configuración guardada exitosamente:\n\n" +
                                    "💰 Monto: $%.2f %s\n" +
                                    "⭐ Puntos: %d\n" +
                                    "� Límite diario: %d puntos\n" +
                                    "�🔄 Sistema: %s\n\n" +
                                    "Esta configuración se aplicará automáticamente en las ventas.", 
                                    monto, moneda, puntos, limiteDiario,
                                    activo ? "ACTIVO" : "INACTIVO"),
                        "Configuración Guardada",
                        JOptionPane.INFORMATION_MESSAGE);
                        
                    // Actualizar vista
                    lblEjemplo.setText(String.format("$%.2f %s = %d puntos", monto, moneda, puntos));
                        
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(ventanaPuntos,
                        "❌ Error en formato numérico:\n• Monto debe ser un número decimal\n• Puntos debe ser un número entero",
                        "Error de Formato",
                        JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ventanaPuntos,
                        "❌ Error guardando configuración: " + ex.getMessage(),
                        "Error del Sistema",
                        JOptionPane.ERROR_MESSAGE);
                }
            });
            
            // Recrear tablas de puntos (troubleshooting)
            btnRecrearTablas.addActionListener(e -> {
                int confirmacion = JOptionPane.showConfirmDialog(ventanaPuntos,
                    "⚠️ ADVERTENCIA ⚠️\n\n" +
                    "Esta acción recreará completamente las tablas de puntos.\n" +
                    "Se perderán TODOS los datos de puntos existentes.\n\n" +
                    "¿Estás seguro de continuar?",
                    "Confirmar Recreación de Tablas",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                    
                if (confirmacion == JOptionPane.YES_OPTION) {
                    try {
                        puntosDataLogic.forzarCreacionTablas();
                        JOptionPane.showMessageDialog(ventanaPuntos,
                            "✅ Tablas de puntos recreadas exitosamente.\n\n" +
                            "El sistema está listo para funcionar.",
                            "Tablas Recreadas",
                            JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ventanaPuntos,
                            "❌ Error recreando tablas: " + ex.getMessage(),
                            "Error del Sistema",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            
            // Consultar puntos de cliente REAL
            btnConsultarCliente.addActionListener(e -> {
                try {
                    String clienteId = txtClienteId.getText().trim();
                    if (clienteId.isEmpty()) {
                        JOptionPane.showMessageDialog(ventanaPuntos,
                            "❌ Ingresa el ID del cliente",
                            "Campo Requerido",
                            JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    
                    ClientePuntos puntos = puntosDataLogic.getClientePuntos(clienteId);
                    if (puntos != null) {
                        JOptionPane.showMessageDialog(ventanaPuntos,
                            String.format("📊 INFORMACIÓN DEL CLIENTE: %s\n\n" +
                                        "⭐ Puntos actuales: %d\n" +
                                        "🏆 Puntos totales acumulados: %d\n" +
                                        "📝 Última transacción: %s\n" +
                                        "📅 Fecha última transacción: %s", 
                                        clienteId, 
                                        puntos.getPuntosActuales(),
                                        puntos.getPuntosTotales(),
                                        puntos.getUltimaTransaccion() != null ? puntos.getUltimaTransaccion() : "Ninguna",
                                        puntos.getFechaUltimaTransaccion() != null ? puntos.getFechaUltimaTransaccion().toString() : "N/A"),
                            "Consulta de Puntos",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(ventanaPuntos,
                            String.format("ℹ️ El cliente '%s' no tiene puntos registrados.\n\nSe creará un registro automáticamente cuando realice compras.", clienteId),
                            "Cliente Sin Puntos",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                    
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ventanaPuntos,
                        "❌ Error consultando puntos: " + ex.getMessage(),
                        "Error del Sistema",
                        JOptionPane.ERROR_MESSAGE);
                }
            });
            
            // Asignar puntos manualmente REAL
            btnAsignarPuntos.addActionListener(e -> {
                try {
                    String clienteId = txtClienteId.getText().trim();
                    String descripcion = txtDescripcion.getText().trim();
                    int puntosAsignar = Integer.parseInt(txtPuntosAsignar.getText());
                    
                    if (clienteId.isEmpty() || descripcion.isEmpty() || puntosAsignar <= 0) {
                        JOptionPane.showMessageDialog(ventanaPuntos,
                            "❌ Completa todos los campos:\n• ID del cliente\n• Descripción del producto\n• Puntos > 0",
                            "Campos Incompletos",
                            JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    
                    // Confirmar asignación
                    int confirmacion = JOptionPane.showConfirmDialog(ventanaPuntos,
                        String.format("¿Confirmas asignar %d puntos al cliente '%s' por '%s'?", 
                                    puntosAsignar, clienteId, descripcion),
                        "Confirmar Asignación",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                    
                    if (confirmacion == JOptionPane.YES_OPTION) {
                        puntosDataLogic.agregarPuntosPorProducto(clienteId, descripcion, puntosAsignar);
                        
                        JOptionPane.showMessageDialog(ventanaPuntos,
                            String.format("✅ PUNTOS ASIGNADOS EXITOSAMENTE\n\n" +
                                        "👤 Cliente: %s\n" +
                                        "📦 Producto: %s\n" +
                                        "⭐ Puntos: %d\n\n" +
                                        "Los puntos han sido guardados en la base de datos.", 
                                        clienteId, descripcion, puntosAsignar),
                            "Asignación Exitosa",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        // Limpiar campos después de asignación exitosa
                        txtDescripcion.setText("");
                        txtPuntosAsignar.setText("0");
                    }
                    
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(ventanaPuntos,
                        "❌ Los puntos deben ser un número entero válido",
                        "Error de Formato",
                        JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ventanaPuntos,
                        "❌ Error asignando puntos: " + ex.getMessage(),
                        "Error del Sistema",
                        JOptionPane.ERROR_MESSAGE);
                }
            });
            
            btnCerrar.addActionListener(e -> ventanaPuntos.dispose());
            
            // Ensamblar la ventana
            JPanel panelContenido = new JPanel(new BorderLayout(10, 10));
            panelContenido.add(panelConfiguracion, BorderLayout.NORTH);
            panelContenido.add(panelAsignacion, BorderLayout.CENTER);
            panelContenido.add(panelInfo, BorderLayout.SOUTH);
            
            panelPrincipal.add(panelTitulo, BorderLayout.NORTH);
            panelPrincipal.add(panelContenido, BorderLayout.CENTER);
            panelPrincipal.add(panelBotones, BorderLayout.SOUTH);
            
            ventanaPuntos.add(panelPrincipal);
            ventanaPuntos.setVisible(true);
            
        } catch (BasicException e) {
            JOptionPane.showMessageDialog(this,
                "❌ Error inicializando sistema de puntos: " + e.getMessage(),
                "Error del Sistema",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public EditorRecord getEditor() {
        return jeditor;
    }

    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.CustomersManagement");
    }    

    /**
     * Exporta toda la base de datos de clientes e historial de puntos a un archivo Excel/TSV.
     */
    private void btnExportarClientesActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setDialogTitle("Exportar Clientes a Excel");
        fileChooser.setSelectedFile(new java.io.File("Clientes.xls"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos de Excel (*.xls)", "xls"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == javax.swing.JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();
            
            // Asegurar extensión .xls
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".xls")) {
                fileToSave = new java.io.File(filePath + ".xls");
            }

            try (java.io.PrintWriter pw = new java.io.PrintWriter(
                    new java.io.OutputStreamWriter(new java.io.FileOutputStream(fileToSave), java.nio.charset.StandardCharsets.UTF_8))) {

                // Escribir UTF-8 BOM para soporte correcto de acentos en Excel
                pw.write('\uFEFF');

                // Cabeceras de columnas
                pw.print("Clave Búsqueda\tRFC\tNombre Completo\tNombre\tApellido\tCorreo\tTeléfono\tTeléfono 2\tDirección\tCiudad\tCódigo Postal\tLímite Crédito\tDeuda Actual\tDescuento %\tPuntos\tNotas\n");

                Session session = app.getSession();
                java.sql.Connection conn = session.getConnection();
                java.sql.Statement stmt = conn.createStatement();
                java.sql.ResultSet rs = stmt.executeQuery(
                        "SELECT c.SEARCHKEY, c.TAXID, c.NAME, c.FIRSTNAME, c.LASTNAME, c.EMAIL, c.PHONE, c.PHONE2, " +
                        "c.ADDRESS, c.CITY, c.POSTAL, c.MAXDEBT, c.CURDEBT, c.DISCOUNT, c.NOTES, " +
                        "COALESCE(p.puntos_actuales, 0) AS PUNTOS " +
                        "FROM customers c " +
                        "LEFT JOIN cliente_puntos p ON c.ID = p.cliente_id " +
                        "ORDER BY c.NAME");

                int count = 0;
                while (rs.next()) {
                    String searchKey = rs.getString("SEARCHKEY");
                    String taxId = rs.getString("TAXID");
                    String name = rs.getString("NAME");
                    String firstName = rs.getString("FIRSTNAME");
                    String lastName = rs.getString("LASTNAME");
                    String email = rs.getString("EMAIL");
                    String phone = rs.getString("PHONE");
                    String phone2 = rs.getString("PHONE2");
                    String address = rs.getString("ADDRESS");
                    String city = rs.getString("CITY");
                    String postal = rs.getString("POSTAL");
                    double maxDebt = rs.getDouble("MAXDEBT");
                    double curDebt = rs.getDouble("CURDEBT");
                    double discount = rs.getDouble("DISCOUNT");
                    int puntos = rs.getInt("PUNTOS");
                    String notes = rs.getString("NOTES");

                    // Reemplazar saltos de línea y tabuladores en notas para no romper el formato TSV
                    if (notes != null) {
                        notes = notes.replace("\n", " ").replace("\r", " ").replace("\t", " ");
                    } else {
                        notes = "";
                    }

                    pw.print(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%.2f\t%.2f\t%.2f\t%d\t%s\n",
                            searchKey != null ? searchKey : "",
                            taxId != null ? taxId : "",
                            name != null ? name : "",
                            firstName != null ? firstName : "",
                            lastName != null ? lastName : "",
                            email != null ? email : "",
                            phone != null ? phone : "",
                            phone2 != null ? phone2 : "",
                            address != null ? address : "",
                            city != null ? city : "",
                            postal != null ? postal : "",
                            maxDebt,
                            curDebt,
                            discount,
                            puntos,
                            notes
                    ));
                    count++;
                }

                rs.close();
                stmt.close();

                JOptionPane.showMessageDialog(this,
                        "✅ Se han exportado " + count + " clientes correctamente a Excel con toda su información y puntos.",
                        "Exportación Exitosa",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                System.err.println("Error exportando clientes a Excel: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                        "❌ Error al exportar los clientes a Excel:\n" + e.getMessage(),
                        "Error de Exportación",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Aplica el tipo de letra Segoe UI 24 de manera recursiva a todos los componentes
     * del panel de clientes para una perfecta legibilidad.
     */
    private void setLargeFont(java.awt.Component comp) {
        if (comp == null) return;
        
        java.awt.Font currentFont = comp.getFont();
        if (currentFont == null || currentFont.getSize() < 24) {
            comp.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 24));
        }
        
        if (comp instanceof javax.swing.JTable) {
            javax.swing.JTable t = (javax.swing.JTable) comp;
            t.setRowHeight(32);
            t.getTableHeader().setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        }
        if (comp instanceof javax.swing.text.JTextComponent) {
            comp.setPreferredSize(new java.awt.Dimension(comp.getPreferredSize().width, 36));
        }
        if (comp instanceof java.awt.Container) {
            for (java.awt.Component child : ((java.awt.Container) comp).getComponents()) {
                setLargeFont(child);
            }
        }
    }
}
