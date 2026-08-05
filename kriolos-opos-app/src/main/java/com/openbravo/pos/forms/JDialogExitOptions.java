package com.openbravo.pos.forms;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import com.openbravo.pos.forms.AppLocal;

/**
 * Diálogo personalizado para opciones de salida
 * Diseño inspirado en eleventa solicitado por el usuario
 */
public class JDialogExitOptions extends JDialog {

    private boolean closeShiftRequested = false;
    private boolean exitOnlyRequested = false;
    private final JRootApp m_appview;

    public JDialogExitOptions(Frame parent, JRootApp appview) {
        super(parent, true);
        this.m_appview = appview;
        initComponents();
    }

    private void initComponents() {
        setUndecorated(true);
        setTitle("SALIR DE PUNTO MX");
        setPreferredSize(new Dimension(500, 380));
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Panel principal con borde sutil
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 1));
        mainPanel.setBackground(Color.WHITE);

        // Cabecera (Gris oscuro con título centrado)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(60, 63, 65));
        headerPanel.setPreferredSize(new Dimension(500, 45));

        JLabel lblTitle = new JLabel("SALIR DE PUNTO MX");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(lblTitle, BorderLayout.CENTER);

        // Botón cerrar (X roja en la esquina derecha)
        JButton btnCloseX = new JButton("X");
        btnCloseX.setFocusPainted(false);
        btnCloseX.setBorderPainted(false);
        btnCloseX.setContentAreaFilled(false);
        btnCloseX.setOpaque(true);
        btnCloseX.setBackground(new Color(220, 53, 69));
        btnCloseX.setForeground(Color.WHITE);
        btnCloseX.setFont(new Font("Arial", Font.BOLD, 14));
        btnCloseX.setPreferredSize(new Dimension(35, 30));
        btnCloseX.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCloseX.addActionListener(e -> dispose());

        JPanel xPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        xPanel.setOpaque(false);
        xPanel.add(btnCloseX);
        headerPanel.add(xPanel, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Cuerpo del diálogo
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBackground(Color.WHITE);
        bodyPanel.setBorder(new EmptyBorder(20, 40, 20, 40));

        JLabel lblPrompt = new JLabel("Por favor elige la opción que deseas...");
        lblPrompt.setFont(new Font("Arial", Font.PLAIN, 18));
        lblPrompt.setAlignmentX(Component.CENTER_ALIGNMENT);
        bodyPanel.add(lblPrompt);
        bodyPanel.add(Box.createVerticalStrut(25));

        // Botón 1: Cerrar Turno
        JButton btnCerrarTurno = createOptionButton("🔒 Cerrar Turno ...", new Color(245, 245, 245));
        btnCerrarTurno.addActionListener(e -> {
            closeShiftRequested = true;
            dispose();
        });
        bodyPanel.add(btnCerrarTurno);
        bodyPanel.add(Box.createVerticalStrut(15));

        // Botón 2: Dejar turno abierto y Salir
        JButton btnSalirAbierto = createOptionButton("🚪 Dejar turno abierto y Salir ...", new Color(245, 245, 245));
        btnSalirAbierto.addActionListener(e -> {
            exitOnlyRequested = true;
            dispose();
        });
        bodyPanel.add(btnSalirAbierto);
        bodyPanel.add(Box.createVerticalStrut(20));

        // Advertencia
        JPanel warningPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        warningPanel.setBackground(new Color(255, 251, 235)); // Amarillito claro
        warningPanel.setBorder(BorderFactory.createLineBorder(new Color(252, 211, 77), 1));
        warningPanel.setMaximumSize(new Dimension(420, 60));

        JLabel lblWarningIcon = new JLabel("⚠️");
        lblWarningIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        warningPanel.add(lblWarningIcon);

        JLabel lblWarningText = new JLabel(
                "<html><body style='width: 300px;'>Si dejas tu turno abierto solo tu o un Administrador podrán abrir el programa</body></html>");
        lblWarningText.setFont(new Font("Arial", Font.PLAIN, 13));
        lblWarningText.setForeground(new Color(146, 64, 14));
        warningPanel.add(lblWarningText);

        bodyPanel.add(warningPanel);
        bodyPanel.add(Box.createVerticalStrut(25));

        // Botón Cancelar
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Arial", Font.PLAIN, 16));
        btnCancelar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCancelar.setFocusPainted(false);
        btnCancelar.setBackground(Color.WHITE);
        btnCancelar.setPreferredSize(new Dimension(120, 35));
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelar.addActionListener(e -> dispose());
        bodyPanel.add(btnCancelar);

        mainPanel.add(bodyPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(getParent());
    }

    private JButton createOptionButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.PLAIN, 18));
        btn.setMaximumSize(new Dimension(420, 50));
        btn.setPreferredSize(new Dimension(420, 50));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Efecto hover
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(230, 230, 230));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
            }
        });

        return btn;
    }

    public boolean isCloseShiftRequested() {
        return closeShiftRequested;
    }

    public boolean isExitOnlyRequested() {
        return exitOnlyRequested;
    }
}
