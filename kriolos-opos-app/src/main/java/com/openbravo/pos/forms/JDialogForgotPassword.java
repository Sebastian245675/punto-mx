/*
 * Diálogo de Recuperación de Contraseña
 * Permite recuperar la contraseña mediante pregunta de seguridad
 */
package com.openbravo.pos.forms;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.pos.util.Hashcypher;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Diálogo para recuperar contraseña olvidada usando pregunta de seguridad
 * @author Sebastian
 */
public class JDialogForgotPassword extends JDialog {
    
    private static final Logger LOGGER = Logger.getLogger(JDialogForgotPassword.class.getName());
    
    // Colores modernos
    private static final Color PRIMARY_BLUE = new Color(59, 130, 246);
    private static final Color PRIMARY_BLUE_HOVER = new Color(37, 99, 235);
    private static final Color BACKGROUND_LIGHT = new Color(249, 250, 251);
    private static final Color CARD_WHITE = new Color(255, 255, 255);
    private static final Color TEXT_DARK = new Color(31, 41, 55);
    private static final Color TEXT_GRAY = new Color(107, 114, 128);
    private static final Color BORDER_LIGHT = new Color(229, 231, 235);
    private static final Color BORDER_FOCUS = new Color(59, 130, 246);
    private static final Color ERROR_RED = new Color(239, 68, 68);
    
    private final DataLogicSystem m_dlSystem;
    private JTextField txtUsername;
    private JLabel lblSecurityQuestion;
    private JTextField txtSecurityAnswer;
    private JPasswordField txtNewPassword;
    private JPasswordField txtConfirmPassword;
    private JButton btnVerify;
    private JButton btnChangePassword;
    private JButton btnCancel;
    private JButton btnSavePassword;
    private JLabel lblNewPassword;
    private JLabel lblConfirmPassword;
    
    private AppUser currentUser;
    private boolean questionVerified = false;
    
    public JDialogForgotPassword(java.awt.Frame parent, DataLogicSystem dlSystem) {
        super(parent, "Recuperar Contraseña", true);
        this.m_dlSystem = dlSystem;
        initComponents();
        setupDialog();
    }
    
    private void initComponents() {
        setLayout(new GridBagLayout());
        setBackground(BACKGROUND_LIGHT);
        setResizable(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Panel principal con fondo
        javax.swing.JPanel mainPanel = new javax.swing.JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2d = (java.awt.Graphics2D) g.create();
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(CARD_WHITE);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2d.setColor(BORDER_LIGHT);
                g2d.setStroke(new java.awt.BasicStroke(1.0f));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2d.dispose();
            }
        };
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        GridBagConstraints panelGbc = new GridBagConstraints();
        panelGbc.insets = new Insets(0, 0, 0, 0);
        panelGbc.fill = GridBagConstraints.BOTH;
        panelGbc.weightx = 1.0;
        panelGbc.weighty = 1.0;
        
        // Título
        JLabel titleLabel = new JLabel("🔑 Recuperar Contraseña");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panelGbc.gridx = 0;
        panelGbc.gridy = 0;
        panelGbc.weightx = 1.0;
        panelGbc.insets = new Insets(0, 0, 25, 0);
        mainPanel.add(titleLabel, panelGbc);
        
        // Campo de usuario
        JLabel lblUsername = new JLabel("👤 Usuario:");
        lblUsername.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblUsername.setForeground(TEXT_DARK);
        panelGbc.gridx = 0;
        panelGbc.gridy = 1;
        panelGbc.anchor = GridBagConstraints.WEST;
        panelGbc.insets = new Insets(0, 0, 8, 0);
        mainPanel.add(lblUsername, panelGbc);
        
        txtUsername = new JTextField(25);
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtUsername.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT, 2),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        txtUsername.setBackground(new Color(249, 250, 251));
        txtUsername.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !questionVerified) {
                    verifyUser();
                }
            }
        });
        panelGbc.gridx = 0;
        panelGbc.gridy = 2;
        panelGbc.fill = GridBagConstraints.HORIZONTAL;
        panelGbc.weightx = 1.0;
        panelGbc.insets = new Insets(0, 0, 20, 0);
        mainPanel.add(txtUsername, panelGbc);
        
        // Botón verificar usuario
        btnVerify = new JButton("Verificar Usuario");
        btnVerify.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnVerify.setForeground(Color.WHITE);
        btnVerify.setBackground(PRIMARY_BLUE);
        btnVerify.setBorderPainted(false);
        btnVerify.setFocusPainted(false);
        btnVerify.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnVerify.setPreferredSize(new Dimension(0, 40));
        btnVerify.addActionListener(e -> verifyUser());
        panelGbc.gridx = 0;
        panelGbc.gridy = 3;
        panelGbc.fill = GridBagConstraints.HORIZONTAL;
        panelGbc.weightx = 1.0;
        panelGbc.insets = new Insets(0, 0, 25, 0);
        mainPanel.add(btnVerify, panelGbc);
        
        // Pregunta de seguridad (inicialmente oculta)
        lblSecurityQuestion = new JLabel("Pregunta de Seguridad:");
        lblSecurityQuestion.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSecurityQuestion.setForeground(TEXT_DARK);
        lblSecurityQuestion.setVisible(false);
        panelGbc.gridx = 0;
        panelGbc.gridy = 4;
        panelGbc.anchor = GridBagConstraints.WEST;
        panelGbc.insets = new Insets(0, 0, 8, 0);
        mainPanel.add(lblSecurityQuestion, panelGbc);
        
        txtSecurityAnswer = new JTextField(25);
        txtSecurityAnswer.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtSecurityAnswer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT, 2),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        txtSecurityAnswer.setBackground(new Color(249, 250, 251));
        txtSecurityAnswer.setVisible(false);
        txtSecurityAnswer.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && questionVerified) {
                    verifyAnswer();
                }
            }
        });
        panelGbc.gridx = 0;
        panelGbc.gridy = 5;
        panelGbc.fill = GridBagConstraints.HORIZONTAL;
        panelGbc.weightx = 1.0;
        panelGbc.insets = new Insets(0, 0, 20, 0);
        mainPanel.add(txtSecurityAnswer, panelGbc);
        
        // Botón verificar respuesta
        btnChangePassword = new JButton("Verificar Respuesta");
        btnChangePassword.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnChangePassword.setForeground(Color.WHITE);
        btnChangePassword.setBackground(PRIMARY_BLUE);
        btnChangePassword.setBorderPainted(false);
        btnChangePassword.setFocusPainted(false);
        btnChangePassword.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnChangePassword.setPreferredSize(new Dimension(0, 40));
        btnChangePassword.setVisible(false);
        btnChangePassword.addActionListener(e -> verifyAnswer());
        panelGbc.gridx = 0;
        panelGbc.gridy = 6;
        panelGbc.fill = GridBagConstraints.HORIZONTAL;
        panelGbc.weightx = 1.0;
        panelGbc.insets = new Insets(0, 0, 25, 0);
        mainPanel.add(btnChangePassword, panelGbc);
        
        // Campos de nueva contraseña (inicialmente ocultos)
        lblNewPassword = new JLabel("🔒 Nueva Contraseña:");
        lblNewPassword.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblNewPassword.setForeground(TEXT_DARK);
        lblNewPassword.setVisible(false);
        panelGbc.gridx = 0;
        panelGbc.gridy = 7;
        panelGbc.anchor = GridBagConstraints.WEST;
        panelGbc.insets = new Insets(0, 0, 8, 0);
        mainPanel.add(lblNewPassword, panelGbc);
        
        txtNewPassword = new JPasswordField(25);
        txtNewPassword.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtNewPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT, 2),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        txtNewPassword.setBackground(new Color(249, 250, 251));
        txtNewPassword.setVisible(false);
        panelGbc.gridx = 0;
        panelGbc.gridy = 8;
        panelGbc.fill = GridBagConstraints.HORIZONTAL;
        panelGbc.weightx = 1.0;
        panelGbc.insets = new Insets(0, 0, 15, 0);
        mainPanel.add(txtNewPassword, panelGbc);
        
        lblConfirmPassword = new JLabel("🔒 Confirmar Contraseña:");
        lblConfirmPassword.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblConfirmPassword.setForeground(TEXT_DARK);
        lblConfirmPassword.setVisible(false);
        panelGbc.gridx = 0;
        panelGbc.gridy = 9;
        panelGbc.anchor = GridBagConstraints.WEST;
        panelGbc.insets = new Insets(0, 0, 8, 0);
        mainPanel.add(lblConfirmPassword, panelGbc);
        
        txtConfirmPassword = new JPasswordField(25);
        txtConfirmPassword.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtConfirmPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT, 2),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        txtConfirmPassword.setBackground(new Color(249, 250, 251));
        txtConfirmPassword.setVisible(false);
        txtConfirmPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    changePassword();
                }
            }
        });
        panelGbc.gridx = 0;
        panelGbc.gridy = 10;
        panelGbc.fill = GridBagConstraints.HORIZONTAL;
        panelGbc.weightx = 1.0;
        panelGbc.insets = new Insets(0, 0, 20, 0);
        mainPanel.add(txtConfirmPassword, panelGbc);
        
        // Botones finales
        javax.swing.JPanel buttonPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        btnCancel = new JButton("Cancelar");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnCancel.setPreferredSize(new Dimension(100, 35));
        btnCancel.addActionListener(e -> dispose());
        buttonPanel.add(btnCancel);
        
        btnSavePassword = new JButton("Guardar Nueva Contraseña");
        btnSavePassword.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSavePassword.setForeground(Color.WHITE);
        btnSavePassword.setBackground(PRIMARY_BLUE);
        btnSavePassword.setBorderPainted(false);
        btnSavePassword.setFocusPainted(false);
        btnSavePassword.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnSavePassword.setPreferredSize(new Dimension(200, 35));
        btnSavePassword.setVisible(false);
        btnSavePassword.addActionListener(e -> changePassword());
        buttonPanel.add(btnSavePassword);
        
        panelGbc.gridx = 0;
        panelGbc.gridy = 11;
        panelGbc.fill = GridBagConstraints.HORIZONTAL;
        panelGbc.anchor = GridBagConstraints.EAST;
        panelGbc.weightx = 1.0;
        panelGbc.insets = new Insets(10, 0, 0, 0);
        mainPanel.add(buttonPanel, panelGbc);
        
        // Agregar panel principal al diálogo
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        add(mainPanel, gbc);
    }
    
    private void setupDialog() {
        setSize(500, 600);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    private void verifyUser() {
        String username = txtUsername.getText().trim();
        
        if (username.isEmpty()) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, "Por favor ingrese un nombre de usuario");
            msg.show(this);
            txtUsername.requestFocus();
            return;
        }
        
        try {
            currentUser = m_dlSystem.findPeopleByName(username);
            
            if (currentUser == null) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, "Usuario no encontrado");
                msg.show(this);
                txtUsername.requestFocus();
                txtUsername.selectAll();
                return;
            }
            
            // Obtener pregunta de seguridad desde la base de datos
            String securityQuestion = getSecurityQuestion(currentUser.getId());
            
            if (securityQuestion == null || securityQuestion.trim().isEmpty()) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, 
                    "Este usuario no tiene configurada una pregunta de seguridad.\n" +
                    "Por favor contacte al administrador.");
                msg.show(this);
                return;
            }
            
            // Mostrar pregunta de seguridad
            lblSecurityQuestion.setText("❓ " + securityQuestion);
            lblSecurityQuestion.setVisible(true);
            txtSecurityAnswer.setVisible(true);
            btnChangePassword.setVisible(true);
            txtUsername.setEnabled(false);
            btnVerify.setEnabled(false);
            
            txtSecurityAnswer.requestFocus();
            
        } catch (BasicException ex) {
            LOGGER.log(Level.WARNING, "Error al buscar usuario: ", ex);
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                    "Error al buscar usuario: " + ex.getMessage());
            msg.show(this);
        }
    }
    
    private void verifyAnswer() {
        String answer = txtSecurityAnswer.getText().trim();
        
        if (answer.isEmpty()) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, "Por favor ingrese la respuesta");
            msg.show(this);
            txtSecurityAnswer.requestFocus();
            return;
        }
        
        try {
            String correctAnswer = getSecurityAnswer(currentUser.getId());
            
            if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, 
                    "No se encontró respuesta de seguridad para este usuario.");
                msg.show(this);
                return;
            }
            
            // Comparar respuestas (case-insensitive)
            if (answer.equalsIgnoreCase(correctAnswer)) {
                questionVerified = true;
                
                // Mostrar campos de nueva contraseña
                showPasswordFields();
                
                MessageInf msg = new MessageInf(MessageInf.SGN_SUCCESS, 
                    "Respuesta correcta. Ahora puede cambiar su contraseña.");
                msg.show(this);
                
            } else {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, 
                    "Respuesta incorrecta. Por favor intente nuevamente.");
                msg.show(this);
                txtSecurityAnswer.requestFocus();
                txtSecurityAnswer.selectAll();
            }
            
        } catch (BasicException ex) {
            LOGGER.log(Level.WARNING, "Error al verificar respuesta: ", ex);
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                    "Error al verificar respuesta: " + ex.getMessage());
            msg.show(this);
        }
    }
    
    private void showPasswordFields() {
        // Ocultar campos de pregunta
        txtSecurityAnswer.setVisible(false);
        btnChangePassword.setVisible(false);
        
        // Mostrar campos de nueva contraseña
        lblNewPassword.setVisible(true);
        txtNewPassword.setVisible(true);
        lblConfirmPassword.setVisible(true);
        txtConfirmPassword.setVisible(true);
        btnSavePassword.setVisible(true);
        
        txtNewPassword.requestFocus();
        pack();
    }
    
    private void changePassword() {
        String newPassword = new String(txtNewPassword.getPassword());
        String confirmPassword = new String(txtConfirmPassword.getPassword());
        
        if (newPassword.isEmpty()) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, "Por favor ingrese una nueva contraseña");
            msg.show(this);
            txtNewPassword.requestFocus();
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, 
                "Las contraseñas no coinciden. Por favor verifique.");
            msg.show(this);
            txtConfirmPassword.requestFocus();
            txtConfirmPassword.selectAll();
            return;
        }
        
        if (newPassword.length() < 4) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, 
                "La contraseña debe tener al menos 4 caracteres.");
            msg.show(this);
            txtNewPassword.requestFocus();
            txtNewPassword.selectAll();
            return;
        }
        
        try {
            // Hashear la nueva contraseña
            String hashedPassword = Hashcypher.hashString(newPassword);
            
            // Actualizar contraseña en la base de datos
            m_dlSystem.execChangePassword(new Object[]{hashedPassword, currentUser.getId()});
            
            MessageInf msg = new MessageInf(MessageInf.SGN_SUCCESS, 
                "Contraseña actualizada exitosamente. Ahora puede iniciar sesión.");
            msg.show(this);
            
            dispose();
            
        } catch (BasicException ex) {
            LOGGER.log(Level.SEVERE, "Error al cambiar contraseña: ", ex);
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                    "Error al cambiar contraseña: " + ex.getMessage());
            msg.show(this);
        }
    }
    
    private String getSecurityQuestion(String userId) throws BasicException {
        return m_dlSystem.getSecurityQuestion(userId);
    }
    
    private String getSecurityAnswer(String userId) throws BasicException {
        return m_dlSystem.getSecurityAnswer(userId);
    }
}

