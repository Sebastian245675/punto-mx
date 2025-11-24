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

package com.openbravo.beans;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Diálogo simple de contraseña sin teclado en pantalla
 * @author Sebastian
 */
public class JPasswordDialogSimple extends JDialog {
    
    private static final long serialVersionUID = 1L;
    
    private JPasswordField passwordField;
    private String password = null;
    private boolean accepted = false;

    public JPasswordDialogSimple(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    public JPasswordDialogSimple(Dialog parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }
    
    private void initComponents() {
        setTitle("Contraseña");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        
        // Panel principal con fondo degradado sutil
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(35, 40, 35, 40));
        mainPanel.setBackground(new Color(248, 249, 250)); // Fondo gris muy claro
        
        // Panel del encabezado con icono y usuario
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        headerPanel.setOpaque(false);
        
        JLabel iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(64, 64));
        headerPanel.add(iconLabel);
        
        JLabel userLabel = new JLabel();
        userLabel.setFont(new Font("Arial", Font.BOLD, 22));
        userLabel.setForeground(new Color(30, 30, 30));
        headerPanel.add(userLabel);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Panel central con campo de contraseña - MEJORADO
        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        
        JLabel passwordLabel = new JLabel("Contraseña:");
        passwordLabel.setFont(new Font("Arial", Font.BOLD, 16));
        passwordLabel.setForeground(new Color(60, 60, 60));
        passwordLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        centerPanel.add(passwordLabel, BorderLayout.NORTH);
        
        // Campo de contraseña MUCHO MÁS GRANDE y mejorado visualmente
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Arial", Font.BOLD, 32)); // Fuente mucho más grande
        passwordField.setPreferredSize(new Dimension(450, 70)); // Campo mucho más grande
        passwordField.setMinimumSize(new Dimension(450, 70));
        passwordField.setMaximumSize(new Dimension(450, 70));
        passwordField.setEchoChar('●'); // Usar círculo en lugar de asterisco
        
        // Borde mejorado con efecto de sombra sutil
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1)
            ),
            BorderFactory.createEmptyBorder(12, 18, 12, 18) // Padding interno generoso
        ));
        
        // Fondo blanco para el campo
        passwordField.setBackground(Color.WHITE);
        passwordField.setForeground(new Color(30, 30, 30));
        
        // Efecto de foco mejorado
        passwordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                passwordField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(59, 130, 246), 2),
                        BorderFactory.createLineBorder(new Color(100, 180, 255), 1)
                    ),
                    BorderFactory.createEmptyBorder(12, 18, 12, 18)
                ));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                passwordField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                        BorderFactory.createLineBorder(new Color(220, 220, 220), 1)
                    ),
                    BorderFactory.createEmptyBorder(12, 18, 12, 18)
                ));
            }
        });
        
        // Listener para Enter
        passwordField.addActionListener(e -> acceptPassword());
        
        // Listener para Escape
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelPassword();
                }
            }
        });
        
        centerPanel.add(passwordField, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Panel de botones mejorado
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.setFont(new Font("Arial", Font.PLAIN, 15));
        cancelButton.setPreferredSize(new Dimension(120, 45));
        cancelButton.setMinimumSize(new Dimension(120, 45));
        cancelButton.setFocusPainted(false);
        cancelButton.setBackground(new Color(240, 240, 240));
        cancelButton.setForeground(new Color(80, 80, 80));
        cancelButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        cancelButton.addActionListener(e -> cancelPassword());
        
        JButton okButton = new JButton("Aceptar");
        okButton.setFont(new Font("Arial", Font.BOLD, 15));
        okButton.setPreferredSize(new Dimension(120, 45));
        okButton.setMinimumSize(new Dimension(120, 45));
        okButton.setBackground(new Color(59, 130, 246));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40, 100, 200), 1),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        okButton.addActionListener(e -> acceptPassword());
        
        // Efecto hover en botones
        okButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                okButton.setBackground(new Color(40, 120, 230));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                okButton.setBackground(new Color(59, 130, 246));
            }
        });
        
        cancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                cancelButton.setBackground(new Color(220, 220, 220));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                cancelButton.setBackground(new Color(240, 240, 240));
            }
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Configuración del diálogo - más grande para el campo grande
        setPreferredSize(new Dimension(550, 380));
        pack();
        setLocationRelativeTo(getParent());
        
        // Establecer botón por defecto
        getRootPane().setDefaultButton(okButton);
    }
    
    public void setMessage(String userName, Icon userIcon) {
        // Buscar y actualizar el label de usuario e icono en el nuevo diseño
        Component[] topLevelComponents = getContentPane().getComponents();
        if (topLevelComponents.length > 0 && topLevelComponents[0] instanceof JPanel) {
            JPanel mainPanel = (JPanel) topLevelComponents[0];
            Component[] mainComponents = mainPanel.getComponents();
            
            // Buscar el headerPanel (primer componente en BorderLayout.NORTH)
            for (Component comp : mainComponents) {
                if (comp instanceof JPanel) {
                    JPanel headerPanel = (JPanel) comp;
                    Component[] headerComponents = headerPanel.getComponents();
                    
                    if (headerComponents.length >= 2) {
                        if (headerComponents[0] instanceof JLabel && userIcon != null) {
                            ((JLabel) headerComponents[0]).setIcon(userIcon);
                        }
                        if (headerComponents[1] instanceof JLabel && userName != null) {
                            ((JLabel) headerComponents[1]).setText(userName);
                        }
                        break;
                    }
                }
            }
        }
    }
    
    private void acceptPassword() {
        char[] pwd = passwordField.getPassword();
        password = new String(pwd);
        // Limpiar el array de caracteres por seguridad
        java.util.Arrays.fill(pwd, ' ');
        accepted = true;
        dispose();
    }
    
    private void cancelPassword() {
        password = null;
        accepted = false;
        dispose();
    }
    
    public String getPassword() {
        return accepted ? password : null;
    }
    
    protected static Window getWindow(Component parent) {
        if (parent == null) {
            return new JFrame();
        } else if (parent instanceof Frame || parent instanceof Dialog) {
            return (Window) parent;
        } else {
            return getWindow(parent.getParent());
        }
    }
    
    public static String showEditor(Component parent, String title, String message, Icon icon) {
        Window window = getWindow(parent);
        
        JPasswordDialogSimple dialog;
        if (window instanceof Frame) {
            dialog = new JPasswordDialogSimple((Frame) window, true);
        } else {
            dialog = new JPasswordDialogSimple((Dialog) window, true);
        }
        
        if (title != null) {
            dialog.setTitle(title);
        }
        
        if (message != null || icon != null) {
            dialog.setMessage(message, icon);
        }
        
        // Enfocar el campo de contraseña
        SwingUtilities.invokeLater(() -> dialog.passwordField.requestFocusInWindow());
        
        dialog.setVisible(true);
        return dialog.getPassword();
    }
}

