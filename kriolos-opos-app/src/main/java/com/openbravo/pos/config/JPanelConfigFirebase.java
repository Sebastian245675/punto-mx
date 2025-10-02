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

/**
 * Panel de configuración para Firebase - Almacenamiento en la nube
 * @author Sebastian
 */
public class JPanelConfigFirebase extends javax.swing.JPanel implements PanelConfig {
    
    private final DirtyManager dirty = new DirtyManager();
    
    /** Creates new form JPanelConfigFirebase */
    public JPanelConfigFirebase() {
        initComponents();
        
        // Establecer valores por defecto para el proyecto demos-d2610
        jtxtProjectId.setText("demos-d2610");
        jtxtApiKey.setText("AIzaSyBhZsfrKRqHZbfJj8cBc2S8lHzfCf4XQWU");
        jtxtAuthDomain.setText("demos-d2610.firebaseapp.com");
        jtxtStorageBucket.setText("demos-d2610.firebasestorage.app");
        jtxtMessagingSenderId.setText("123456789012");
        jtxtAppId.setText("1:123456789012:web:abcdef1234567890123456");
        jchkFirebaseEnabled.setSelected(true);
        jchkSyncCustomers.setSelected(true);
        jchkSyncProducts.setSelected(true);
        jchkSyncSales.setSelected(true);
        
        // Agregar listeners para detectar cambios
        jtxtProjectId.getDocument().addDocumentListener(dirty);
        jtxtApiKey.getDocument().addDocumentListener(dirty);
        jtxtAuthDomain.getDocument().addDocumentListener(dirty);
        jtxtStorageBucket.getDocument().addDocumentListener(dirty);
        jtxtMessagingSenderId.getDocument().addDocumentListener(dirty);
        jtxtAppId.getDocument().addDocumentListener(dirty);
        jchkFirebaseEnabled.addActionListener(dirty);
        jchkSyncCustomers.addActionListener(dirty);
        jchkSyncProducts.addActionListener(dirty);
        jchkSyncSales.addActionListener(dirty);
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
        jSeparator2 = new javax.swing.JSeparator();
        jLabel8 = new javax.swing.JLabel();
        jchkFirebaseEnabled = new javax.swing.JCheckBox();
        jLabel9 = new javax.swing.JLabel();
        jchkSyncCustomers = new javax.swing.JCheckBox();
        jchkSyncProducts = new javax.swing.JCheckBox();
        jchkSyncSales = new javax.swing.JCheckBox();
        jButtonTest = new javax.swing.JButton();
        jButtonUpload = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();

        setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        jLabel1.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        jLabel1.setText("Configuración Firebase");

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

        jLabel10.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        jLabel10.setText("<html>Configure aquí los parámetros de Firebase para sincronizar datos con la nube.<br/>Obtenga la configuración desde la consola de Firebase de su proyecto.</html>");

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
                            .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jtxtProjectId)
                            .addComponent(jtxtApiKey)
                            .addComponent(jtxtAuthDomain)
                            .addComponent(jtxtStorageBucket)
                            .addComponent(jtxtMessagingSenderId)
                            .addComponent(jtxtAppId, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)))
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
                        .addComponent(jButtonUpload)))
                .addContainerGap(30, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel1)
                .addGap(10, 10, 10)
                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jtxtProjectId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jtxtApiKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jtxtAuthDomain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jtxtStorageBucket, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jtxtMessagingSenderId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jtxtAppId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(jLabel8)
                .addGap(5, 5, 5)
                .addComponent(jchkFirebaseEnabled)
                .addGap(15, 15, 15)
                .addComponent(jLabel9)
                .addGap(5, 5, 5)
                .addComponent(jchkSyncCustomers)
                .addGap(5, 5, 5)
                .addComponent(jchkSyncProducts)
                .addGap(5, 5, 5)
                .addComponent(jchkSyncSales)
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonTest)
                    .addComponent(jButtonUpload))
                .addContainerGap(20, Short.MAX_VALUE))
        );
    }// </editor-fold>                        

    private void jButtonTestActionPerformed(java.awt.event.ActionEvent evt) {                                            
        // Guardar temporalmente la configuración para la prueba
        AppConfig tempConfig = new AppConfig(null);
        tempConfig.setProperty("firebase.projectid", jtxtProjectId.getText().trim());
        tempConfig.setProperty("firebase.apikey", jtxtApiKey.getText().trim());
        tempConfig.setProperty("firebase.authdomain", jtxtAuthDomain.getText().trim());
        tempConfig.setProperty("firebase.storagebucket", jtxtStorageBucket.getText().trim());
        tempConfig.setProperty("firebase.messagingsenderid", jtxtMessagingSenderId.getText().trim());
        tempConfig.setProperty("firebase.appid", jtxtAppId.getText().trim());
        tempConfig.setProperty("firebase.enabled", String.valueOf(jchkFirebaseEnabled.isSelected()));
        
        // Validar que los campos obligatorios estén llenos
        if (jtxtProjectId.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Por favor ingrese el Project ID de Firebase.",
                "Error de Configuración", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Deshabilitar el botón durante la prueba
        jButtonTest.setEnabled(false);
        jButtonTest.setText("Probando...");
        
        // Ejecutar prueba en background
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    com.openbravo.pos.firebase.FirebaseServiceREST service = 
                        com.openbravo.pos.firebase.FirebaseServiceREST.getInstance();
                    
                    // Inicializar el servicio con la configuración temporal
                    if (!service.initialize(tempConfig)) {
                        return false;
                    }
                    
                    // Probar conexión
                    return service.testConnection().get();
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
                            "La configuración de Firebase es correcta.",
                            "Prueba de Conexión Firebase", 
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                            "Error de conexión.\n" +
                            "Verifique la configuración y la conexión a internet.\n" +
                            "Asegúrese de que el Project ID sea correcto.",
                            "Prueba de Conexión Firebase", 
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

    private void jButtonUploadActionPerformed(java.awt.event.ActionEvent evt) {                                              
        // Verificar que Firebase esté habilitado
        if (!jchkFirebaseEnabled.isSelected()) {
            JOptionPane.showMessageDialog(this, 
                "Debe habilitar Firebase antes de subir datos.",
                "Firebase no habilitado", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Confirmar la operación
        int choice = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de que desea subir todos los datos a Firebase?\n" +
            "Esta operación puede tomar varios minutos dependiendo de la cantidad de datos.",
            "Confirmar Subida de Datos",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
            
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Guardar la configuración actual antes de subir
        // No necesitamos guardar aquí, la configuración ya está cargada
        
        // Ejecutar la subida en background
        SwingWorker<Boolean, String> uploadWorker = new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // Deshabilitar el botón durante la subida
                SwingUtilities.invokeLater(() -> {
                    jButtonUpload.setEnabled(false);
                    jButtonUpload.setText("Subiendo...");
                });
                
                try {
                    // Obtener la configuración de la base de datos desde el archivo de configuración
                    com.openbravo.pos.forms.AppConfig config = new com.openbravo.pos.forms.AppConfig(null);
                    config.load();
                    
                    // Crear una sesión directamente usando la configuración
                    com.openbravo.data.loader.Session session = null;
                    try {
                        session = com.openbravo.pos.forms.AppViewConnection.createSession(null, config);
                    } catch (Exception e) {
                        System.err.println("Error creando sesión de BD: " + e.getMessage());
                        return false;
                    }
                    
                    if (session == null) {
                        System.err.println("No se pudo crear la sesión de base de datos");
                        return false;
                    }
                    
                    // Crear una instancia del sync manager con la sesión obtenida
                    com.openbravo.pos.firebase.FirebaseSyncManagerREST syncManager = 
                        new com.openbravo.pos.firebase.FirebaseSyncManagerREST(session);
                    
                    // Ejecutar la sincronización completa
                    var syncFuture = syncManager.performFullSync();
                    var result = syncFuture.get(5, java.util.concurrent.TimeUnit.MINUTES); // Timeout de 5 minutos
                    return result.success;
                    
                } catch (Exception e) {
                    System.err.println("Error durante la subida: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                // Rehabilitar el botón
                jButtonUpload.setEnabled(true);
                jButtonUpload.setText("Subir Datos");
                
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                            "¡Datos subidos exitosamente!\n" +
                            "Todos los datos se han sincronizado con Firebase.",
                            "Subida Completada", 
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                            "Error durante la subida.\n" +
                            "Algunos datos pueden no haberse sincronizado correctamente.\n" +
                            "Revise los logs para más detalles.",
                            "Error en la Subida", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(JPanelConfigFirebase.this, 
                        "Error inesperado durante la subida:\n" + e.getMessage(),
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        uploadWorker.execute();
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
        // Guardar configuración de Firebase
        config.setProperty("firebase.projectid", jtxtProjectId.getText());
        config.setProperty("firebase.apikey", jtxtApiKey.getText());
        config.setProperty("firebase.authdomain", jtxtAuthDomain.getText());
        config.setProperty("firebase.storagebucket", jtxtStorageBucket.getText());
        config.setProperty("firebase.messagingsenderid", jtxtMessagingSenderId.getText());
        config.setProperty("firebase.appid", jtxtAppId.getText());
        
        config.setProperty("firebase.enabled", Boolean.toString(jchkFirebaseEnabled.isSelected()));
        config.setProperty("firebase.sync.customers", Boolean.toString(jchkSyncCustomers.isSelected()));
        config.setProperty("firebase.sync.products", Boolean.toString(jchkSyncProducts.isSelected()));
        config.setProperty("firebase.sync.sales", Boolean.toString(jchkSyncSales.isSelected()));
        
        dirty.setDirty(false);
    }

    // Variables declaration - do not modify                     
    private javax.swing.JButton jButtonTest;
    private javax.swing.JButton jButtonUpload;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
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
    // End of variables declaration                   
}