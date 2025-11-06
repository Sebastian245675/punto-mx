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

import com.openbravo.data.user.DirtyManager;
import com.openbravo.pos.forms.AppConfig;
import javax.swing.SwingUtilities;
import com.openbravo.pos.forms.AppLocal;
import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import java.util.HashMap;
import java.util.Map;

/**
 * Panel de configuración para Firebase - Almacenamiento en la nube
 * @author Sebastian
 */
public class JPanelConfigFirebase extends javax.swing.JPanel implements PanelConfig {
    
    private final DirtyManager dirty = new DirtyManager();
    private boolean userIdValidated = false;
    private String validatedUserId = null;
    private String hayCajasAbiertasDetalle = null;
    
    private com.openbravo.pos.forms.AppView m_App;

    /** Creates new form JPanelConfigFirebase */
    public JPanelConfigFirebase() {
        initComponents();
        
        // Ocultar campos de configuración de Firebase - solo mostrar ID Usuario
        jtxtProjectId.setVisible(false);
        jtxtApiKey.setVisible(false);
        jtxtAuthDomain.setVisible(false);
        jtxtStorageBucket.setVisible(false);
        jtxtMessagingSenderId.setVisible(false);
        jtxtAppId.setVisible(false);
        jLabel2.setVisible(false);
        jLabel3.setVisible(false);
        jLabel4.setVisible(false);
        jLabel5.setVisible(false);
        jLabel6.setVisible(false);
        jLabel7.setVisible(false);
        jLabel8.setVisible(false);
        jchkFirebaseEnabled.setVisible(false);
        jLabel9.setVisible(false);
        jchkSyncCustomers.setVisible(false);
        jchkSyncProducts.setVisible(false);
        jchkSyncSales.setVisible(false);
        
        // Configuración por defecto - Supabase siempre habilitado
        jchkFirebaseEnabled.setSelected(true);
        jchkSyncCustomers.setSelected(true);
        jchkSyncProducts.setSelected(true);
        jchkSyncSales.setSelected(true);
        
        // Solo escuchar cambios en el ID Usuario
        jtxtUserId.getDocument().addDocumentListener(dirty);
        
        // Listener para invalidar la validación cuando cambie el ID de usuario
        jtxtUserId.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                invalidateUserValidation();
            }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                invalidateUserValidation();
            }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                invalidateUserValidation();
            }
        });
        
        // Deshabilitar los botones de subida/descarga hasta que se valide el ID
        updateButtonsState();
    }

    public JPanelConfigFirebase(com.openbravo.pos.forms.AppView app) {
        this();
        this.m_App = app;
    }
    
    /**
     * Invalida la validación del usuario cuando se cambia el ID
     */
    private void invalidateUserValidation() {
        if (userIdValidated) {
            userIdValidated = false;
            validatedUserId = null;
            jLabelUserStatus.setText("⚠️ ID modificado - debe validar nuevamente");
            jLabelUserStatus.setForeground(java.awt.Color.ORANGE);
            updateButtonsState();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel2 = new javax.swing.JLabel();
        jtxtProjectId = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jtxtApiKey = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jtxtAuthDomain = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jtxtStorageBucket = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jtxtMessagingSenderId = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jtxtAppId = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jtxtUserId = new javax.swing.JTextField();
        jButtonValidateUser = new javax.swing.JButton();
        jLabelUserStatus = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel8 = new javax.swing.JLabel();
        jchkFirebaseEnabled = new javax.swing.JCheckBox();
        jLabel9 = new javax.swing.JLabel();
        jchkSyncCustomers = new javax.swing.JCheckBox();
        jchkSyncProducts = new javax.swing.JCheckBox();
        jchkSyncSales = new javax.swing.JCheckBox();
        jButtonTest = new javax.swing.JButton();
        jButtonUpload = new javax.swing.JButton();
        jButtonDownload = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();

        setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        jLabel1.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        jLabel1.setText("Configuración Supabase");

        jLabel2.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel2.setText("Project ID:");

        jtxtProjectId.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        jLabel3.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel3.setText("API Key:");

        jtxtApiKey.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        jLabel4.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel4.setText("Auth Domain:");

        jtxtAuthDomain.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        jLabel5.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel5.setText("Storage Bucket:");

        jtxtStorageBucket.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        jLabel6.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel6.setText("Messaging Sender ID:");

        jtxtMessagingSenderId.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        jLabel7.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel7.setText("App ID:");

        jtxtAppId.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        jLabel11.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel11.setText("ID Usuario:");

        jtxtUserId.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jtxtUserId.setToolTipText("Ingrese el número de usuario (ej: 6767)");

        jButtonValidateUser.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        jButtonValidateUser.setText("Validar");
        jButtonValidateUser.setToolTipText("Validar que el número de usuario existe en la tabla usuarios de Supabase");
        jButtonValidateUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonValidateUserActionPerformed(evt);
            }
        });

        jLabelUserStatus.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        jLabelUserStatus.setText(" ");

        jLabel8.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        jLabel8.setText("Estado del Servicio:");

        jchkFirebaseEnabled.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jchkFirebaseEnabled.setText("Habilitar Firebase");

        jLabel9.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        jLabel9.setText("Sincronización:");

        jchkSyncCustomers.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jchkSyncCustomers.setText("Sincronizar Clientes");

        jchkSyncProducts.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jchkSyncProducts.setText("Sincronizar Productos");

        jchkSyncSales.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jchkSyncSales.setText("Sincronizar Ventas");

        jButtonTest.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jButtonTest.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/run_script.png"))); // NOI18N
        jButtonTest.setText("Probar Conexión");
        jButtonTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonTestActionPerformed(evt);
            }
        });

        jButtonUpload.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jButtonUpload.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/filesave.png"))); // NOI18N
        jButtonUpload.setText("Subir Datos");
        jButtonUpload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUploadActionPerformed(evt);
            }
        });

        jButtonDownload.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jButtonDownload.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/fileopen.png"))); // NOI18N
        jButtonDownload.setText("Traer Datos");
        jButtonDownload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDownloadActionPerformed(evt);
            }
        });

        jButtonViewUsers = new javax.swing.JButton();
        jButtonViewUsers.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jButtonViewUsers.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/user.png"))); // NOI18N
        jButtonViewUsers.setText("Ver Usuarios");
        jButtonViewUsers.setToolTipText("Ver todos los usuarios de la tabla usuarios en Supabase");
        jButtonViewUsers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonViewUsersActionPerformed(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        jLabel10.setText("<html>Supabase está conectado automáticamente. Solo ingrese su ID Usuario para habilitar la sincronización de datos.</html>");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 450, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 450, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                            .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jtxtProjectId)
                            .addComponent(jtxtApiKey)
                            .addComponent(jtxtAuthDomain)
                            .addComponent(jtxtStorageBucket)
                            .addComponent(jtxtMessagingSenderId)
                            .addComponent(jtxtAppId, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jtxtUserId, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonValidateUser, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(168, 168, 168)
                        .addComponent(jLabelUserStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 450, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(jchkFirebaseEnabled)
                    .addComponent(jLabel9)
                    .addComponent(jchkSyncCustomers)
                    .addComponent(jchkSyncProducts)
                    .addComponent(jchkSyncSales)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButtonTest)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonUpload)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonDownload)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonViewUsers)))
                .addContainerGap(30, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jLabel1)
                .addGap(8, 8, 8)
                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 8, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jtxtProjectId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jtxtApiKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jtxtAuthDomain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jtxtStorageBucket, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jtxtMessagingSenderId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jtxtAppId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(jtxtUserId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonValidateUser))
                .addGap(3, 3, 3)
                .addComponent(jLabelUserStatus)
                .addGap(8, 8, 8)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 8, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(jLabel8)
                .addGap(3, 3, 3)
                .addComponent(jchkFirebaseEnabled)
                .addGap(10, 10, 10)
                .addComponent(jLabel9)
                .addGap(3, 3, 3)
                .addComponent(jchkSyncCustomers)
                .addGap(3, 3, 3)
                .addComponent(jchkSyncProducts)
                .addGap(3, 3, 3)
                .addComponent(jchkSyncSales)
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonTest)
                    .addComponent(jButtonUpload)
                    .addComponent(jButtonDownload)
                    .addComponent(jButtonViewUsers))
                .addContainerGap(15, Short.MAX_VALUE))
        );
    }// </editor-fold>                        

    private void jButtonValidateUserActionPerformed(java.awt.event.ActionEvent evt) {
        String userId = jtxtUserId.getText().trim();
        
        // Validar que el campo no esté vacío
        if (userId.isEmpty()) {
            jLabelUserStatus.setText("⚠️ Ingrese un ID de usuario");
            jLabelUserStatus.setForeground(java.awt.Color.ORANGE);
            return;
        }
        
        // Deshabilitar el botón durante la validación
        jButtonValidateUser.setEnabled(false);
        jButtonValidateUser.setText("Validando...");
        jLabelUserStatus.setText("⏳ Verificando...");
        jLabelUserStatus.setForeground(java.awt.Color.BLUE);
        
        // Ejecutar validación en background consultando Supabase (validación por CARD)
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String userName = null;
            
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    // Usar SupabaseServiceManager con conexión interna
                    com.openbravo.pos.supabase.SupabaseServiceManager manager = 
                        com.openbravo.pos.supabase.SupabaseServiceManager.getInstance();
                    AppConfig tempConfig = new AppConfig(null);
                    tempConfig.load();
                    manager.initialize(tempConfig);
                    com.openbravo.pos.supabase.SupabaseServiceREST supabase = manager.getService();
                    java.util.List<java.util.Map<String, Object>> usuarios = supabase.fetchData("usuarios");
                    
                    // Buscar el card en la lista de usuarios - solo verificar que exista
                    for (java.util.Map<String, Object> u : usuarios) {
                        Object card = u.get("tarjeta");
                        if (card == null) card = u.get("card");
                        if (card != null && userId.equals(card.toString())) {
                            // Si encontramos el card, obtener el nombre y retornar true
                            Object nombre = u.get("nombre");
                            if (nombre == null) nombre = u.get("name");
                            userName = nombre != null ? nombre.toString() : null;
                            return true; // Card existe, validación exitosa
                        }
                    }
                    return false; // Card no encontrado
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(JPanelConfigFirebase.class.getName())
                        .log(java.util.logging.Level.SEVERE, "Error validando CARD contra Supabase", ex);
                    return false;
                }
            }
            
            @Override
            protected void done() {
                // Rehabilitar el botón
                jButtonValidateUser.setEnabled(true);
                jButtonValidateUser.setText("Validar");
                
                try {
                    boolean exists = get();
                    if (exists) {
                        jLabelUserStatus.setText("✓ CARD válido (Supabase): " + (userName != null ? userName : userId));
                        jLabelUserStatus.setForeground(new java.awt.Color(0, 150, 0));
                        
                        // Marcar como validado y guardar el ID
                        userIdValidated = true;
                        validatedUserId = userId; // Usamos el CARD como código habilitante
                        
                        // Guardar automáticamente en la configuración para que esté disponible al subir ventas
                        try {
                            com.openbravo.pos.forms.AppConfig config = new com.openbravo.pos.forms.AppConfig(null);
                            config.load();
                            config.setProperty("supabase.userid", userId);
                            config.setProperty("firebase.userid", userId); // Compatibilidad
                            config.save();
                            java.util.logging.Logger.getLogger(JPanelConfigFirebase.class.getName())
                                .info("Card validado guardado automáticamente en configuración: " + userId);
                        } catch (Exception e) {
                            java.util.logging.Logger.getLogger(JPanelConfigFirebase.class.getName())
                                .log(java.util.logging.Level.WARNING, "Error guardando card validado en configuración", e);
                        }
                        
                        // Habilitar los botones de subida/descarga
                        updateButtonsState();
                        
                        JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                            "CARD validado en Supabase: " + (userName != null ? userName : userId) + "\n" +
                            "Ahora puede subir datos a Supabase.",
                            "Validación Exitosa", 
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        jLabelUserStatus.setText("✗ CARD no encontrado en Supabase");
                        jLabelUserStatus.setForeground(java.awt.Color.RED);
                        
                        // Marcar como no validado
                        userIdValidated = false;
                        validatedUserId = null;
                        updateButtonsState();
                        
                        JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                            "El CARD ingresado no existe en la tabla people o está inactivo.",
                            "CARD No Válido", 
                            JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception e) {
                    jLabelUserStatus.setText("✗ Error al validar");
                    jLabelUserStatus.setForeground(java.awt.Color.RED);
                    
                    // Marcar como no validado
                    userIdValidated = false;
                    validatedUserId = null;
                    updateButtonsState();
                    
                    JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                        "Error al validar el usuario:\n" + e.getMessage(),
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Actualiza el estado de los botones según si el ID está validado
     */
    private void updateButtonsState() {
        jButtonUpload.setEnabled(userIdValidated);
        jButtonDownload.setEnabled(userIdValidated);
        
        if (!userIdValidated) {
            jButtonUpload.setToolTipText("Debe validar el ID de usuario antes de subir datos");
            jButtonDownload.setToolTipText("Debe validar el ID de usuario antes de traer datos");
        } else {
            jButtonUpload.setToolTipText("Subir datos a Supabase con identificador: " + validatedUserId);
            jButtonDownload.setToolTipText("Traer datos desde Supabase con identificador: " + validatedUserId);
        }
    }

    private void jButtonViewUsersActionPerformed(java.awt.event.ActionEvent evt) {
        // Deshabilitar el botón durante la carga
        jButtonViewUsers.setEnabled(false);
        jButtonViewUsers.setText("Cargando...");
        
        // Descargar usuarios desde Supabase en background
        SwingWorker<java.util.List<java.util.Map<String, Object>>, Void> worker = new SwingWorker<java.util.List<java.util.Map<String, Object>>, Void>() {
            @Override
            protected java.util.List<java.util.Map<String, Object>> doInBackground() throws Exception {
                // Usar SupabaseServiceManager con conexión interna
                com.openbravo.pos.supabase.SupabaseServiceManager manager = 
                    com.openbravo.pos.supabase.SupabaseServiceManager.getInstance();
                AppConfig tempConfig = new AppConfig(null);
                tempConfig.load();
                manager.initialize(tempConfig);
                com.openbravo.pos.supabase.SupabaseServiceREST supabase = manager.getService();
                
                // Obtener usuarios desde Supabase
                return supabase.fetchData("usuarios");
            }
            
            @Override
            protected void done() {
                // Rehabilitar el botón
                jButtonViewUsers.setEnabled(true);
                jButtonViewUsers.setText("Ver Usuarios");
                
                try {
                    java.util.List<java.util.Map<String, Object>> roles = get();
                    
                    if (roles == null || roles.isEmpty()) {
                        JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                            "No se encontraron usuarios en la colección 'roles' de Firebase.",
                            "Sin Usuarios", 
                            JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    
                    // Abrir el diálogo con los roles
                    com.openbravo.pos.firebase.JPanelFirebaseUsersDialog dialog = 
                        new com.openbravo.pos.firebase.JPanelFirebaseUsersDialog(
                            javax.swing.SwingUtilities.getWindowAncestor(JPanelConfigFirebase.this),
                            roles
                        );
                    dialog.setVisible(true);
                    
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                        "Error al cargar usuarios desde Firebase:\n" + e.getMessage(),
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
    }

    private void jButtonTestActionPerformed(java.awt.event.ActionEvent evt) {                                            
        // Usar configuración temporal para la prueba
        AppConfig tempConfig = new AppConfig(null);
        tempConfig.load();
        
        // Deshabilitar el botón durante la prueba
        jButtonTest.setEnabled(false);
        jButtonTest.setText("Probando...");
        
        // Ejecutar prueba en background
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    // Usar SupabaseServiceManager con conexión interna
                    com.openbravo.pos.supabase.SupabaseServiceManager manager = 
                        com.openbravo.pos.supabase.SupabaseServiceManager.getInstance();
                    
                    // Inicializar el servicio con la configuración temporal
                    if (!manager.initialize(tempConfig)) {
                        return false;
                    }
                    
                    // Probar conexión intentando obtener datos
                    com.openbravo.pos.supabase.SupabaseServiceREST supabase = manager.getService();
                    java.util.List<java.util.Map<String, Object>> test = supabase.fetchData("usuarios");
                    return test != null; // Si puede obtener datos, la conexión funciona
                } catch (Exception e) {
                    return false;
                }
            }
            
            @Override
            protected void done() {
                // Rehabilitar el botón
                jButtonTest.setEnabled(true);
                jButtonTest.setText("Probar Conexión");
                
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                            "¡Conexión exitosa!\n" +
                            "Supabase está conectado correctamente.",
                            "Prueba de Conexión Supabase", 
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                            "Error de conexión.\n" +
                            "Verifique su conexión a internet e intente nuevamente.",
                            "Prueba de Conexión Supabase", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                        "Error inesperado durante la prueba de conexión:\n" + e.getMessage(),
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
    }                                           
    
    /**
     * Verifica cuántas ventas hay disponibles para subir (ventas de cajas cerradas)
     * @return número de ventas disponibles para subir
     */
    private int verificarVentasDisponibles() {
        try {
            // Obtener la configuración de la base de datos
            com.openbravo.pos.forms.AppConfig config = new com.openbravo.pos.forms.AppConfig(null);
            config.load();
            
            // Crear una sesión de base de datos
            com.openbravo.data.loader.Session session = null;
            try {
                session = com.openbravo.pos.forms.AppViewConnection.createSession(null, config);
            } catch (Exception e) {
                System.err.println("Error creando sesión de BD para verificar ventas: " + e.getMessage());
                return 0;
            }
            
            if (session == null) {
                return 0;
            }
            
            java.sql.PreparedStatement stmt = null;
            java.sql.ResultSet rs = null;
            try {
                // Contar ventas de cajas cerradas (solo estas se pueden subir)
                String sql = "SELECT COUNT(*) FROM receipts r " +
                           "INNER JOIN closedcash cc ON r.MONEY = cc.MONEY " +
                           "WHERE cc.DATEEND IS NOT NULL";
                
                stmt = session.getConnection().prepareStatement(sql);
                rs = stmt.executeQuery();
                
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count;
                }
                
                return 0;
            } catch (Exception e) {
                System.err.println("Error verificando ventas disponibles: " + e.getMessage());
                return 0;
            } finally {
                if (rs != null) { try { rs.close(); } catch (Exception e) {} }
                if (stmt != null) { try { stmt.close(); } catch (Exception e) {} }
                if (session != null) { try { session.close(); } catch (Exception e) {} }
            }
        } catch (Exception e) {
            System.err.println("Error en verificarVentasDisponibles: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Verifica si hay cajas abiertas (DATEEND IS NULL) en la base de datos
     * @return true si hay cajas abiertas, false en caso contrario
     */
    private boolean hayCajasAbiertas() {
        try {
            // Obtener la configuración de la base de datos
            com.openbravo.pos.forms.AppConfig config = new com.openbravo.pos.forms.AppConfig(null);
            config.load();
            
            // Crear una sesión de base de datos
            com.openbravo.data.loader.Session session = null;
            try {
                session = com.openbravo.pos.forms.AppViewConnection.createSession(null, config);
            } catch (Exception e) {
                System.err.println("Error creando sesión de BD para verificar cajas: " + e.getMessage());
                // Si no se puede crear la sesión, asumimos que no hay cajas abiertas para permitir la subida
                return false;
            }
            
            if (session == null) {
                return false;
            }
            
            java.sql.PreparedStatement stmt = null;
            java.sql.ResultSet rs = null;
            try {
                // Asegurar que obtenemos datos frescos (sin caché)
                java.sql.Connection conn = session.getConnection();
                
                // Forzar actualización de datos (si la BD lo soporta)
                try {
                    // Pequeña pausa para asegurar que los cambios se hayan guardado
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                
                // Consultar detalles de cajas abiertas y sus ventas para depuración
                String debugSql = "SELECT cc.MONEY, cc.DATEEND, COUNT(r.ID) as ventas_count " +
                                "FROM closedcash cc " +
                                "LEFT JOIN receipts r ON r.MONEY = cc.MONEY " +
                                "WHERE cc.DATEEND IS NULL " +
                                "GROUP BY cc.MONEY, cc.DATEEND";
                
                java.sql.PreparedStatement debugStmt = conn.prepareStatement(debugSql);
                java.sql.ResultSet debugRs = debugStmt.executeQuery();
                
                int totalVentasEnCajasAbiertas = 0;
                java.util.List<String> cajasInfo = new java.util.ArrayList<>();
                
                while (debugRs.next()) {
                    String moneyId = debugRs.getString("MONEY");
                    java.sql.Timestamp dateEnd = debugRs.getTimestamp("DATEEND");
                    int ventasCount = debugRs.getInt("ventas_count");
                    
                    if (ventasCount > 0) {
                        totalVentasEnCajasAbiertas += ventasCount;
                        cajasInfo.add("Caja: " + moneyId + " - Ventas: " + ventasCount);
                    }
                }
                debugRs.close();
                debugStmt.close();
                
                System.out.println("=== DEPURACIÓN: Verificación de cajas ===");
                System.out.println("Total de ventas en cajas abiertas: " + totalVentasEnCajasAbiertas);
                
                // Guardar detalles para mostrar en el mensaje
                String detalles = "";
                if (!cajasInfo.isEmpty()) {
                    System.out.println("Cajas abiertas con ventas:");
                    detalles = String.join("\n", cajasInfo);
                    for (String info : cajasInfo) {
                        System.out.println("  - " + info);
                    }
                } else {
                    System.out.println("No hay cajas abiertas con ventas");
                }
                
                // Guardar detalle de forma thread-safe usando publish
                if (totalVentasEnCajasAbiertas > 0 && !detalles.isEmpty()) {
                    // Guardar en variable de instancia para acceso desde otro hilo
                    synchronized (JPanelConfigFirebase.this) {
                        hayCajasAbiertasDetalle = detalles;
                    }
                }
                
                return totalVentasEnCajasAbiertas > 0;
                
            } catch (Exception e) {
                System.err.println("Error verificando cajas abiertas: " + e.getMessage());
                e.printStackTrace();
                // En caso de error, permitimos la subida para no bloquear al usuario
                return false;
            } finally {
                // Cerrar recursos en orden inverso
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (Exception e) {
                        // Ignorar errores al cerrar
                    }
                }
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (Exception e) {
                        // Ignorar errores al cerrar
                    }
                }
                if (session != null) {
                    try {
                        session.close();
                    } catch (Exception e) {
                        // Ignorar errores al cerrar
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error general verificando cajas abiertas: " + e.getMessage());
            e.printStackTrace();
            // En caso de error, permitimos la subida para no bloquear al usuario
            return false;
        }
    }

    private void jButtonUploadActionPerformed(java.awt.event.ActionEvent evt) {                                              
        // Verificar que el ID esté validado
        if (!userIdValidated || validatedUserId == null) {
            JOptionPane.showMessageDialog(this, 
                "Debe validar el ID de usuario antes de subir datos.\n" +
                "Ingrese un ID y haga clic en 'Validar'.",
                "ID No Validado", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Limpiar detalle anterior
        hayCajasAbiertasDetalle = null;
        
        // Verificar si hay cajas abiertas antes de permitir la subida (en background para no bloquear UI)
        SwingWorker<Boolean, String> checkWorker = new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // Limpiar detalle antes de verificar
                synchronized (JPanelConfigFirebase.this) {
                    hayCajasAbiertasDetalle = null;
                }
                
                boolean resultado = hayCajasAbiertas();
                
                return resultado;
            }
            
            @Override
            protected void done() {
                try {
                    boolean hayCajasAbiertas = get();
                    if (hayCajasAbiertas) {
                        String mensaje = "No se puede subir datos porque hay ventas en cajas abiertas.\n\n";
                        
                        // Agregar información de depuración si está disponible
                        if (hayCajasAbiertasDetalle != null && !hayCajasAbiertasDetalle.isEmpty()) {
                            mensaje += "Detalles:\n" + hayCajasAbiertasDetalle + "\n\n";
                        }
                        
                        mensaje += "Por favor, cierre la caja actual (que tiene ventas) antes de subir datos.\n" +
                                  "Las ventas de cajas abiertas no se subirán hasta que se cierre la caja.\n\n" +
                                  "NOTA: Si acaba de cerrar la caja, espere unos segundos y vuelva a intentar.\n" +
                                  "Revise la consola para ver los detalles de depuración.";
                        
                        JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                            mensaje,
                            "Caja con Ventas Abierta", 
                            JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    
                    // Limpiar detalle si no hay cajas abiertas
                    hayCajasAbiertasDetalle = null;
                    
                    // Continuar con la confirmación si no hay cajas abiertas
                    continuarConSubida();
                } catch (Exception e) {
                    System.err.println("Error verificando cajas abiertas: " + e.getMessage());
                    e.printStackTrace();
                    // En caso de error, permitir la subida
                    continuarConSubida();
                }
            }
        };
        
        checkWorker.execute();
    }
    
    /**
     * Continúa con el proceso de subida después de verificar que no hay cajas abiertas
     */
    private void continuarConSubida() {
        // Supabase siempre está habilitado - no necesitamos verificar
        
        // Verificar primero si hay ventas disponibles para subir
        int ventasDisponibles = verificarVentasDisponibles();
        
        if (ventasDisponibles == 0) {
            JOptionPane.showMessageDialog(this,
                "No hay ventas nuevas para subir.\n\n" +
                "Todas las ventas de cajas cerradas ya han sido subidas a Supabase.\n" +
                "Para subir nuevas ventas, primero debe cerrar la caja actual.",
                "Sin Ventas para Subir",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Confirmar la operación
        int choice = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de que desea subir todos los datos a Supabase?\n" +
            "Esta operación puede tomar varios minutos dependiendo de la cantidad de datos.\n\n" +
            "Ventas disponibles para subir: " + ventasDisponibles + "\n" +
            "Se utilizará el identificador '" + validatedUserId + "' para la sincronización.",
            "Confirmar Subida de Datos",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
            
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Crear diálogo de progreso
        JDialog progressDialog = new JDialog((java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this), "Subiendo Datos", true);
        progressDialog.setSize(400, 150);
        progressDialog.setLocationRelativeTo(this);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        JPanel progressPanel = new JPanel(new BorderLayout(10, 10));
        progressPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel statusLabel = new JLabel("Iniciando subida de datos...");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        progressPanel.add(statusLabel, BorderLayout.NORTH);
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(false);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        
        progressDialog.add(progressPanel);
        
        // Ejecutar la subida en background
        SwingWorker<Boolean, String> uploadWorker = new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // Mostrar diálogo de progreso
                SwingUtilities.invokeLater(() -> {
                    progressDialog.setVisible(true);
                });
                
                // Deshabilitar el botón durante la subida
                SwingUtilities.invokeLater(() -> {
                    jButtonUpload.setEnabled(false);
                    jButtonUpload.setText("Subiendo...");
                });
                
                try {
                    publish("Conectando a la base de datos...");
                    // Obtener la configuración de la base de datos desde el archivo de configuración
                    com.openbravo.pos.forms.AppConfig config = new com.openbravo.pos.forms.AppConfig(null);
                    config.load();
                    
                    // Crear una sesión directamente usando la configuración
                    com.openbravo.data.loader.Session session = null;
                    try {
                        session = com.openbravo.pos.forms.AppViewConnection.createSession(null, config);
                    } catch (Exception e) {
                        publish("ERROR: No se pudo conectar a la base de datos: " + e.getMessage());
                        return false;
                    }
                    
                    if (session == null) {
                        publish("ERROR: No se pudo crear la sesión de base de datos");
                        return false;
                    }
                    
                    publish("Iniciando sincronización con Supabase...");
                    
                    // Crear una instancia del sync manager con la sesión obtenida
                    com.openbravo.pos.firebase.FirebaseSyncManagerREST syncManager = 
                        new com.openbravo.pos.firebase.FirebaseSyncManagerREST(session);
                    
                    publish("Sincronizando datos... (esto puede tomar varios minutos)");
                    
                    // Ejecutar la sincronización completa
                    var syncFuture = syncManager.performFullSync();
                    var result = syncFuture.get(10, java.util.concurrent.TimeUnit.MINUTES); // Timeout de 10 minutos
                    
                    if (result.success) {
                        publish("¡Sincronización completada exitosamente!");
                    } else {
                        publish("Sincronización completada con errores. Revise los logs.");
                    }
                    
                    return result.success;
                    
                } catch (java.util.concurrent.TimeoutException e) {
                    publish("ERROR: La sincronización tardó demasiado tiempo (>10 minutos)");
                    return false;
                } catch (Exception e) {
                    publish("ERROR: " + e.getMessage());
                    return false;
                }
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String lastMessage = chunks.get(chunks.size() - 1);
                    statusLabel.setText(lastMessage);
                }
            }
            
            @Override
            protected void done() {
                // Cerrar diálogo de progreso
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                });
                
                // Rehabilitar el botón
                jButtonUpload.setEnabled(true);
                jButtonUpload.setText("Subir Datos");
                
                try {
                    boolean success = get();
                    
                    // Verificar si hay ventas después de la subida
                    final int ventasRestantes = JPanelConfigFirebase.this.verificarVentasDisponibles();
                    
                    if (success) {
                        String mensaje = "¡Datos subidos exitosamente!\n\n" +
                            "Todos los datos se han sincronizado con Supabase.\n" +
                            "Los datos están disponibles en la nube.";
                        
                        if (ventasRestantes == 0) {
                            mensaje += "\n\n✓ Todas las ventas disponibles han sido subidas.\n" +
                                      "No hay más ventas para subir en este momento.";
                        } else {
                            mensaje += "\n\n⚠ Aún hay " + ventasRestantes + " venta(s) disponible(s) para subir.\n" +
                                      "Esto puede deberse a que:\n" +
                                      "- Se crearon nuevas ventas durante la subida\n" +
                                      "- Algunas ventas no se pudieron subir (revise los logs)";
                        }
                        
                        JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                            mensaje,
                            "Subida Completada", 
                            ventasRestantes == 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                            "Error durante la subida de datos.\n\n" +
                            "Algunos datos pueden no haberse sincronizado correctamente.\n" +
                            "Revise los logs para más detalles.\n\n" +
                            (ventasRestantes > 0 ? 
                                "Ventas restantes: " + ventasRestantes : 
                                "No hay más ventas disponibles para subir."),
                            "Error en la Subida", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (java.util.concurrent.ExecutionException e) {
                    String errorDetail = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                        "Error durante la subida:\n\n" + errorDetail + "\n\nRevise los logs para más información.",
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                        "Error inesperado durante la subida:\n\n" + e.getMessage() + "\n\nRevise los logs para más información.",
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        uploadWorker.execute();
    }                                              

    private void jButtonDownloadActionPerformed(java.awt.event.ActionEvent evt) {                                               
        // Crear y mostrar el diálogo de selección de datos
        JDialog downloadDialog = createDownloadDialog();
        downloadDialog.setVisible(true);
    }                                               
    
    /**
     * Crea el diálogo de selección de datos para descargar desde Supabase
     */
    private JDialog createDownloadDialog() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Traer Datos desde Supabase");
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setModal(true);
        
        // Panel principal
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Título
        JLabel titleLabel = new JLabel("Seleccione los datos a traer:");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Panel de checkboxes
        JPanel checkPanel = new JPanel();
        checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
        
        // Crear checkboxes para cada categoría
        JCheckBox chkUsuarios = new JCheckBox("Usuarios");
        JCheckBox chkClientes = new JCheckBox("Clientes");
        JCheckBox chkCategorias = new JCheckBox("Categorías de Productos");
        JCheckBox chkProductos = new JCheckBox("Productos");
        JCheckBox chkVentas = new JCheckBox("Ventas");
        JCheckBox chkPuntos = new JCheckBox("Puntos de Clientes");
        JCheckBox chkCierres = new JCheckBox("Cierres de Caja");
        JCheckBox chkPagos = new JCheckBox("Formas de Pago");
        JCheckBox chkImpuestos = new JCheckBox("Impuestos");
        JCheckBox chkConfiguraciones = new JCheckBox("Configuraciones");
        JCheckBox chkInventario = new JCheckBox("Inventario");
        
        // Botón "Seleccionar Todo"
        JCheckBox chkSelectAll = new JCheckBox("Seleccionar Todo");
        chkSelectAll.setFont(new Font("Arial", Font.BOLD, 12));
        
        // Lista de checkboxes para facilitar el manejo
        JCheckBox[] checkboxes = {chkUsuarios, chkClientes, chkCategorias, chkProductos, 
                                 chkVentas, chkPuntos, chkCierres, chkPagos, 
                                 chkImpuestos, chkConfiguraciones, chkInventario};
        
        // Acción del "Seleccionar Todo"
        chkSelectAll.addActionListener(e -> {
            boolean selected = chkSelectAll.isSelected();
            for (JCheckBox cb : checkboxes) {
                cb.setSelected(selected);
            }
        });
        
        // Agregar checkboxes al panel
        checkPanel.add(chkSelectAll);
        checkPanel.add(Box.createVerticalStrut(10));
        
        for (JCheckBox cb : checkboxes) {
            cb.setFont(new Font("Arial", Font.PLAIN, 12));
            checkPanel.add(cb);
            checkPanel.add(Box.createVerticalStrut(5));
        }
        
        mainPanel.add(new JScrollPane(checkPanel), BorderLayout.CENTER);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        
        JButton btnTraer = new JButton("Traer Datos");
        btnTraer.setFont(new Font("Arial", Font.BOLD, 12));
        btnTraer.addActionListener(e -> {
            // Verificar que al menos una opción esté seleccionada
            boolean hasSelection = false;
            for (JCheckBox cb : checkboxes) {
                if (cb.isSelected()) {
                    hasSelection = true;
                    break;
                }
            }
            
            if (!hasSelection) {
                JOptionPane.showMessageDialog(dialog, 
                    "Por favor seleccione al menos una categoría de datos para traer.",
                    "Selección Requerida", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            dialog.dispose();
            
            // Crear mapa de selecciones
            Map<String, Boolean> selections = new HashMap<>();
            selections.put("usuarios", chkUsuarios.isSelected());
            selections.put("clientes", chkClientes.isSelected());
            selections.put("categorias", chkCategorias.isSelected());
            selections.put("productos", chkProductos.isSelected());
            selections.put("ventas", chkVentas.isSelected());
            selections.put("puntos", chkPuntos.isSelected());
            selections.put("cierres", chkCierres.isSelected());
            selections.put("pagos", chkPagos.isSelected());
            selections.put("impuestos", chkImpuestos.isSelected());
            selections.put("configuraciones", chkConfiguraciones.isSelected());
            selections.put("inventario", chkInventario.isSelected());
            
            // Ejecutar la descarga
            executeDownload(selections);
        });
        
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(btnTraer);
        buttonPanel.add(btnCancelar);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        return dialog;
    }
    
    /**
     * Ejecuta la descarga de datos desde Firebase según las selecciones
     */
    private void executeDownload(Map<String, Boolean> selections) {
        // Crear SwingWorker para manejar la descarga en background
        SwingWorker<Boolean, Void> downloadWorker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    // Deshabilitar el botón durante la operación
                    SwingUtilities.invokeLater(() -> {
                        jButtonDownload.setEnabled(false);
                        jButtonDownload.setText("Descargando...");
                    });
                    
                    // Implementar la descarga real usando FirebaseDownloadManagerREST
                    try {
                        AppConfig tempConfig = new AppConfig(null);
                        tempConfig.load();
                        
                        // Obtener la sesión de base de datos
                        com.openbravo.data.loader.Session session = null;
                        try {
                            session = com.openbravo.pos.forms.AppViewConnection.createSession(null, tempConfig);
                        } catch (Exception e) {
                            System.err.println("Error creando sesión de BD: " + e.getMessage());
                            return false;
                        }
                        
                        if (session == null) {
                            System.err.println("No se pudo crear la sesión de base de datos");
                            return false;
                        }
                        
                        com.openbravo.pos.firebase.FirebaseDownloadManagerREST downloadManager = 
                            new com.openbravo.pos.firebase.FirebaseDownloadManagerREST(session, tempConfig);
                        
                        com.openbravo.pos.firebase.FirebaseDownloadManagerREST.DownloadResult result = 
                            downloadManager.performSelectedDownload(selections).join();
                        
                        return result.success;
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                // Rehabilitar el botón
                jButtonDownload.setEnabled(true);
                jButtonDownload.setText("Traer Datos");
                
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                            "¡Datos descargados exitosamente!\n" +
                            "Los datos seleccionados se han traído desde Firebase.",
                            "Descarga Completada", 
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                            "Error durante la descarga.\n" +
                            "Algunos datos pueden no haberse descargado correctamente.\n" +
                            "Revise los logs para más detalles.",
                            "Error en la Descarga", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                        "Error inesperado durante la descarga:\n" + e.getMessage(),
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        downloadWorker.execute();
    }

    @Override
    public boolean hasChanged() {
        return dirty.isDirty();
    }

    @Override
    public Component getConfigComponent() {
        return this;
    }

    @Override
    public void loadProperties(AppConfig config) {
        // Cargar configuración de Firebase
        String projectId = config.getProperty("firebase.projectid");
        jtxtProjectId.setText(projectId != null ? projectId : "");
        
        String apiKey = config.getProperty("firebase.apikey");
        jtxtApiKey.setText(apiKey != null ? apiKey : "");
        
        String authDomain = config.getProperty("firebase.authdomain");
        jtxtAuthDomain.setText(authDomain != null ? authDomain : "");
        
        String storageBucket = config.getProperty("firebase.storagebucket");
        jtxtStorageBucket.setText(storageBucket != null ? storageBucket : "");
        
        String messagingSenderId = config.getProperty("firebase.messagingsenderid");
        jtxtMessagingSenderId.setText(messagingSenderId != null ? messagingSenderId : "");
        
        String appId = config.getProperty("firebase.appid");
        jtxtAppId.setText(appId != null ? appId : "");
        
        String userId = config.getProperty("firebase.userid");
        jtxtUserId.setText(userId != null ? userId : "");
        
        // Limpiar el estado de validación
        jLabelUserStatus.setText(" ");
        
        String enabled = config.getProperty("firebase.enabled");
        jchkFirebaseEnabled.setSelected("true".equals(enabled));
        
        String syncCustomers = config.getProperty("firebase.sync.customers");
        jchkSyncCustomers.setSelected("true".equals(syncCustomers));
        
        String syncProducts = config.getProperty("firebase.sync.products");
        jchkSyncProducts.setSelected("true".equals(syncProducts));
        
        String syncSales = config.getProperty("firebase.sync.sales");
        jchkSyncSales.setSelected("true".equals(syncSales));
        
        dirty.setDirty(false);
    }

    @Override
    public void saveProperties(AppConfig config) {
        // Guardar solo el ID Usuario - Supabase está conectado automáticamente
        config.setProperty("supabase.userid", jtxtUserId.getText().trim());
        // También guardar en firebase.userid para compatibilidad
        config.setProperty("firebase.userid", jtxtUserId.getText().trim());
        
        // Siempre habilitado - Supabase está conectado automáticamente
        config.setProperty("firebase.enabled", "true");
        config.setProperty("firebase.sync.customers", Boolean.toString(jchkSyncCustomers.isSelected()));
        config.setProperty("firebase.sync.products", Boolean.toString(jchkSyncProducts.isSelected()));
        config.setProperty("firebase.sync.sales", Boolean.toString(jchkSyncSales.isSelected()));
        
        dirty.setDirty(false);
    }

    // Variables declaration - do not modify                     
    private javax.swing.JButton jButtonTest;
    private javax.swing.JButton jButtonUpload;
    private javax.swing.JButton jButtonDownload;
    private javax.swing.JButton jButtonValidateUser;
    private javax.swing.JButton jButtonViewUsers;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelUserStatus;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JCheckBox jchkFirebaseEnabled;
    private javax.swing.JCheckBox jchkSyncCustomers;
    private javax.swing.JCheckBox jchkSyncProducts;
    private javax.swing.JCheckBox jchkSyncSales;
    private javax.swing.JTextField jtxtApiKey;
    private javax.swing.JTextField jtxtAppId;
    private javax.swing.JTextField jtxtAuthDomain;
    private javax.swing.JTextField jtxtMessagingSenderId;
    private javax.swing.JTextField jtxtProjectId;
    private javax.swing.JTextField jtxtStorageBucket;
    private javax.swing.JTextField jtxtUserId;
    // End of variables declaration                   
}