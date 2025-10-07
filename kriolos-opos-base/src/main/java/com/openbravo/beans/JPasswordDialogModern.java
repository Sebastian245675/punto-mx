/*
 * Copyright (C) 2023 Paulo Borges
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
package com.openbravo.beans;

import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.util.Hashcypher;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.CaretListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;

/**
 * Diálogo de contraseña moderno sin teclado numérico
 * @author Sebastian
 */
public class JPasswordDialogModern extends javax.swing.JDialog {
    
    private JTextField passwordField;
    private String actualPassword = "";
    private String inputPassword = null;
    private boolean okPressed = false;

    public JPasswordDialogModern(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    public JPasswordDialogModern(Dialog parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }
    
    private void initComponents() {
        setTitle("Login");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        
        // Panel principal con padding
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Panel del título/mensaje
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel();
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        // Panel del campo de contraseña
        JPanel passwordPanel = new JPanel(new BorderLayout());
        passwordPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel passwordLabel = new JLabel("Contraseña:");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordPanel.add(passwordLabel, BorderLayout.NORTH);
        
        passwordField = new JTextField();
        passwordField.setFont(new Font("Arial", Font.BOLD, 18)); // Fuente más grande y negritas
        passwordField.setPreferredSize(new Dimension(250, 40)); // Más alto
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(64, 128, 255), 2), // Borde azul más grueso
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        passwordField.setBackground(Color.WHITE);
        passwordField.setForeground(Color.BLACK);
        passwordField.setHorizontalAlignment(JTextField.CENTER); // Centrar el texto
        
        // Añadir listener para mostrar progreso visual
        charCountLabel = new JLabel("0 caracteres");
        charCountLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        charCountLabel.setForeground(Color.GRAY);
        charCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Manejar la entrada de caracteres y mostrar asteriscos
        passwordField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE) {
                    // Manejar backspace
                    if (actualPassword.length() > 0) {
                        actualPassword = actualPassword.substring(0, actualPassword.length() - 1);
                        updateDisplay();
                    }
                    e.consume(); // Prevenir el comportamiento por defecto
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    okButtonPressed();
                    e.consume();
                } else if (Character.isISOControl(e.getKeyChar())) {
                    // Ignorar caracteres de control excepto los ya manejados
                    e.consume();
                }
            }
            
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isISOControl(c)) {
                    actualPassword += c;
                    updateDisplay();
                }
                e.consume(); // Prevenir que se muestre el carácter real
            }
        });
        
        // Acción para Enter
        passwordField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                okButtonPressed();
            }
        });
        
        JPanel fieldPanel = new JPanel(new BorderLayout());
        fieldPanel.add(passwordField, BorderLayout.CENTER);
        fieldPanel.add(charCountLabel, BorderLayout.SOUTH);
        
        passwordPanel.add(fieldPanel, BorderLayout.CENTER);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.setFont(new Font("Arial", Font.PLAIN, 12));
        cancelButton.setPreferredSize(new Dimension(90, 35));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelButtonPressed();
            }
        });
        
        JButton okButton = new JButton("Aceptar");
        okButton.setFont(new Font("Arial", Font.PLAIN, 12));
        okButton.setPreferredSize(new Dimension(90, 35));
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                okButtonPressed();
            }
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(okButton);
        
        // Ensamblar el diálogo
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(passwordPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        getContentPane().add(mainPanel);
        
        // Configurar tamaño y posición
        pack();
        setLocationRelativeTo(getParent());
        
        // Establecer botón por defecto y foco
        getRootPane().setDefaultButton(okButton);
        
        // Guardar referencias para métodos públicos
        this.titleLabel = titleLabel;
        
        // Ahora sí podemos inicializar la visualización
        updateDisplay();
    }
    
    private JLabel titleLabel;
    
    public void setMessage(String message, Icon icon) {
        titleLabel.setText(message);
        titleLabel.setIcon(icon);
    }
    
    private JLabel charCountLabel; // Hacer referencia global
    
    private void updateDisplay() {
        // Mostrar asteriscos en el campo
        StringBuilder display = new StringBuilder();
        for (int i = 0; i < actualPassword.length(); i++) {
            display.append("●");
        }
        passwordField.setText(display.toString());
        
        // Actualizar contador si existe
        if (charCountLabel != null) {
            charCountLabel.setText(actualPassword.length() + " caracteres");
            if (actualPassword.length() > 0) {
                charCountLabel.setForeground(new Color(0, 128, 0)); // Verde cuando hay texto
            } else {
                charCountLabel.setForeground(Color.GRAY);
            }
        }
    }
    
    private void okButtonPressed() {
        inputPassword = actualPassword;
        okPressed = true;
        setVisible(false);
        dispose();
    }
    
    private void cancelButtonPressed() {
        inputPassword = null;
        okPressed = false;
        setVisible(false);
        dispose();
    }
    
    public String getPassword() {
        return inputPassword;
    }
    
    private static Window getWindow(Component parent) {
        if (parent == null) {
            return new JFrame();
        } else if (parent instanceof Frame || parent instanceof Dialog) {
            return (Window) parent;
        } else {
            return getWindow(parent.getParent());
        }
    }

    public static String showEditor(Component parent, String title) {
        return showEditor(parent, title, null, null);
    }

    public static String showEditor(Component parent, String title, String message) {
        return showEditor(parent, title, message, null);
    }

    public static String showEditor(Component parent, String title, String message, Icon icon) {
        
        Window window = getWindow(parent);      
        
        JPasswordDialogModern dialog;
        if (window instanceof Frame) { 
            dialog = new JPasswordDialogModern((Frame) window, true);
        } else {
            dialog = new JPasswordDialogModern((Dialog) window, true);
        }
        
        dialog.setTitle(title);
        if (message != null || icon != null) {
            dialog.setMessage(message, icon);
        }
        
        // Enfocar el campo de contraseña cuando se muestre
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dialog.passwordField.requestFocusInWindow();
            }
        });
        
        dialog.setVisible(true);
        return dialog.getPassword();
    }

    public static String changePassword(Component parent) {
        // Show the changePassword dialogs but do not check the old password
        
        String sPassword = JPasswordDialogModern.showEditor(parent,                 
                AppLocal.getIntString("label.Password"), 
                AppLocal.getIntString("label.passwordnew"),
                new ImageIcon(Hashcypher.class.getResource("/com/openbravo/images/password.png")));
        if (sPassword != null) {
            String sPassword2 = JPasswordDialogModern.showEditor(parent,                 
                    AppLocal.getIntString("label.Password"), 
                    AppLocal.getIntString("label.passwordrepeat"),
                    new ImageIcon(Hashcypher.class.getResource("/com/openbravo/images/password.png")));
            if (sPassword2 != null) {
                if (sPassword.equals(sPassword2)) {
                    return  Hashcypher.hashString(sPassword);
                } else {
                    JOptionPane.showMessageDialog(parent, AppLocal.getIntString("message.changepassworddistinct"), AppLocal.getIntString("message.title"), JOptionPane.WARNING_MESSAGE);
                }
            }
        }   
        
        return null;
    }

    /**
     *
     * @param parent
     * @param sOldPassword
     * @return
     */
    public static String changePassword(Component parent, String sOldPassword) {
        
        String sPassword = JPasswordDialogModern.showEditor(parent,                 
                AppLocal.getIntString("label.Password"), 
                AppLocal.getIntString("label.passwordold"),
                new ImageIcon(Hashcypher.class.getResource("/com/openbravo/images/password.png")));
        if (sPassword != null) {
            if (Hashcypher.authenticate(sPassword, sOldPassword)) {
                return changePassword(parent);               
            } else {
                JOptionPane.showMessageDialog(parent, AppLocal.getIntString("message.BadPassword"), AppLocal.getIntString("message.title"), JOptionPane.WARNING_MESSAGE);
           }
        }
        return null;
    }
}