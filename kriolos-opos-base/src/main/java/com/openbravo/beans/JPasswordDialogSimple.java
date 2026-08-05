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
import java.awt.geom.*;
import javax.swing.*;

/**
 * Diálogo simplificado de contraseña con toggle de visibilidad.
 * 
 * @author Sebastian
 */
public class JPasswordDialogSimple extends JDialog {

    private static final long serialVersionUID = 1L;

    private JPasswordField passwordField;
    private JLabel iconLabel;
    private JLabel userLabel;
    private JToggleButton eyeButton;
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
        setBackground(Color.WHITE);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. Icono
        iconLabel = new JLabel();
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(iconLabel, gbc);

        // 2. Título de usuario
        userLabel = new JLabel("Contraseña", SwingConstants.CENTER);
        userLabel.setFont(new Font("Arial", Font.BOLD, 18));
        userLabel.setForeground(new Color(50, 50, 50));
        gbc.gridy = 1;
        mainPanel.add(userLabel, gbc);

        // 3. Label "Escriba su contraseña"
        JLabel promptLabel = new JLabel("Contraseña:");
        promptLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridy = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        mainPanel.add(promptLabel, gbc);

        // 4. Input Wrapper (Password + Eye)
        JPanel inputWrapper = new JPanel(new BorderLayout());
        inputWrapper.setBackground(Color.WHITE);
        inputWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 5)));

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Arial", Font.PLAIN, 22));
        passwordField.setEchoChar('*');
        passwordField.setBorder(null);
        passwordField.setOpaque(true);
        passwordField.setPreferredSize(new Dimension(280, 40));

        eyeButton = new JToggleButton();
        eyeButton.setIcon(new EyeIcon(true));
        eyeButton.setSelectedIcon(new EyeIcon(false));
        eyeButton.setPreferredSize(new Dimension(35, 35));
        eyeButton.setBorderPainted(false);
        eyeButton.setContentAreaFilled(false);
        eyeButton.setFocusPainted(false);
        eyeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eyeButton.setToolTipText("Mostrar/Ocultar");

        eyeButton.addActionListener(e -> {
            passwordField.setEchoChar(eyeButton.isSelected() ? (char) 0 : '*');
            passwordField.requestFocusInWindow();
        });

        inputWrapper.add(passwordField, BorderLayout.CENTER);
        inputWrapper.add(eyeButton, BorderLayout.EAST);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 5, 15, 5);
        mainPanel.add(inputWrapper, gbc);

        // 5. Botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton cancelBtn = new JButton("Cancelar");
        cancelBtn.setPreferredSize(new Dimension(100, 35));
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> cancelPassword());

        JButton okBtn = new JButton("Aceptar");
        okBtn.setPreferredSize(new Dimension(100, 35));
        okBtn.setBackground(new Color(59, 130, 246));
        okBtn.setForeground(Color.WHITE);
        okBtn.setFocusPainted(false);
        okBtn.addActionListener(e -> acceptPassword());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);

        gbc.gridy = 4;
        gbc.insets = new Insets(10, 5, 0, 5);
        mainPanel.add(buttonPanel, gbc);

        // Listeners globales
        passwordField.addActionListener(e -> acceptPassword());
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelPassword();
                }
            }
        });

        getContentPane().add(mainPanel);
        pack();
        setSize(450, 360);
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(okBtn);
    }

    public void setMessage(String userName, Icon userIcon) {
        if (userLabel != null && userName != null) {
            userLabel.setText(userName);
        }
        if (iconLabel != null && userIcon != null) {
            iconLabel.setIcon(userIcon);
        }
    }

    private void acceptPassword() {
        password = new String(passwordField.getPassword());
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

    public static String showEditor(Component parent, String title, String message, Icon icon) {
        Window window = SwingUtilities.windowForComponent(parent);
        JPasswordDialogSimple dialog;

        if (window instanceof Frame) {
            dialog = new JPasswordDialogSimple((Frame) window, true);
        } else if (window instanceof Dialog) {
            dialog = new JPasswordDialogSimple((Dialog) window, true);
        } else {
            dialog = new JPasswordDialogSimple((Frame) null, true);
        }

        if (title != null) {
            dialog.setTitle(title);
        }
        if (message != null || icon != null) {
            dialog.setMessage(message, icon);
        }

        SwingUtilities.invokeLater(() -> dialog.passwordField.requestFocusInWindow());
        dialog.setVisible(true);
        return dialog.getPassword();
    }

    // --- ÍCONO DE OJO MEJORADO ---
    private static class EyeIcon implements Icon {
        private final boolean showOpen;

        public EyeIcon(boolean showOpen) {
            this.showOpen = showOpen;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Centrar el dibujo en los 24x24 del icono
            g2.translate(x + 2, y + 2);

            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(100, 100, 100));

            // Curva del ojo
            GeneralPath path = new GeneralPath();
            path.moveTo(0, 10);
            path.quadTo(10, -2, 20, 10); // Superior
            path.quadTo(10, 22, 0, 10); // Inferior
            path.closePath();
            g2.draw(path);

            // Pupila
            g2.fillOval(7, 7, 6, 6);

            // Si está tachado (ocultar)
            if (!showOpen) {
                g2.setColor(new Color(220, 50, 50));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawLine(2, 18, 18, 2);
            }

            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 24;
        }

        @Override
        public int getIconHeight() {
            return 24;
        }
    }
}
