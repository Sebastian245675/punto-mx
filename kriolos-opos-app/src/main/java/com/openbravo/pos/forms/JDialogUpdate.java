package com.openbravo.pos.forms;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;

/**
 * Diálogo de actualización que permite al usuario actualizar la aplicación
 * @author Sebastian
 */
public class JDialogUpdate extends javax.swing.JDialog {
    
    private static final long serialVersionUID = 1L;
    
    private UpdateChecker.UpdateInfo updateInfo;
    private boolean updateAccepted = false;
    private boolean updateCompleted = false;
    
    /**
     * Crea un nuevo diálogo de actualización
     */
    public JDialogUpdate(java.awt.Frame parent, UpdateChecker.UpdateInfo updateInfo) {
        super(parent, true);
        this.updateInfo = updateInfo;
        initComponents();
        setupDialog();
    }
    
    private void setupDialog() {
        setTitle("Actualización Disponible");
        lblMessage.setText("<html><div style='text-align: center; padding: 10px;'>" +
            "<h3>¡Nueva versión disponible!</h3>" +
            "<p>Versión actual: <b>" + AppLocal.APP_VERSION + "</b></p>" +
            "<p>Nueva versión: <b>" + updateInfo.getVersion() + "</b></p>" +
            "<p style='margin-top: 15px;'>¿Deseas actualizar ahora?</p>" +
            "<p style='color: #666; font-size: 11px;'>Tus datos se conservarán intactos.</p>" +
            "</div></html>");
        
        setLocationRelativeTo(getParent());
    }
    
    /**
     * Muestra el diálogo y retorna true si el usuario aceptó actualizar
     */
    public boolean showUpdateDialog() {
        setVisible(true);
        return updateAccepted;
    }
    
    /**
     * Verifica si la actualización se completó exitosamente
     */
    public boolean isUpdateCompleted() {
        return updateCompleted;
    }
    
    private void initComponents() {
        lblMessage = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        lblProgress = new javax.swing.JLabel();
        btnUpdate = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        
        lblMessage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblMessage.setPreferredSize(new java.awt.Dimension(400, 120));
        
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);
        
        lblProgress.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblProgress.setVisible(false);
        
        btnUpdate.setText("Actualizar Ahora");
        btnUpdate.setPreferredSize(new java.awt.Dimension(120, 35));
        btnUpdate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startUpdate();
            }
        });
        
        btnCancel.setText("Más Tarde");
        btnCancel.setPreferredSize(new java.awt.Dimension(120, 35));
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAccepted = false;
                dispose();
            }
        });
        
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblMessage, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                    .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblProgress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblProgress, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        
        pack();
    }
    
    private void startUpdate() {
        updateAccepted = true;
        btnUpdate.setEnabled(false);
        btnCancel.setEnabled(false);
        progressBar.setVisible(true);
        lblProgress.setVisible(true);
        
        // Ejecutar actualización en hilo separado
        new Thread(() -> {
            boolean success = UpdateManager.applyUpdate(updateInfo.getVersion(), 
                new UpdateManager.ProgressCallback() {
                    @Override
                    public void onProgress(int percentage, String message) {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(percentage);
                            lblProgress.setText(message);
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        SwingUtilities.invokeLater(() -> {
                            javax.swing.JOptionPane.showMessageDialog(
                                JDialogUpdate.this,
                                "Error durante la actualización:\n" + error + 
                                "\n\nSe restaurará la versión anterior.",
                                "Error de Actualización",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                            UpdateManager.restoreBackup();
                            dispose();
                        });
                    }
                });
            
            if (success) {
                SwingUtilities.invokeLater(() -> {
                    updateCompleted = true;
                    javax.swing.JOptionPane.showMessageDialog(
                        JDialogUpdate.this,
                        "¡Actualización completada exitosamente!\n\n" +
                        "La aplicación se reiniciará ahora.",
                        "Actualización Exitosa",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    
                    // Cerrar aplicación para que se reinicie con nueva versión
                    System.exit(0);
                });
            }
        }).start();
    }
    
    // Variables de componentes
    private javax.swing.JLabel lblMessage;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel lblProgress;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JButton btnCancel;
}

