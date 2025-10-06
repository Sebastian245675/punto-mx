/*
 * Sebastian POS - Diálogo para ingresar fondo inicial de caja
 * Creado para manejar el fondo inicial cuando se abre una nueva caja
 */
package com.openbravo.pos.forms;

import com.openbravo.format.Formats;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * Diálogo para ingresar el fondo inicial de caja
 * @author Sebastian
 */
public class JDialogInitialCash extends JDialog {
    
    private static final long serialVersionUID = 1L;
    
    private JTextField m_jInitialAmount;
    private JButton m_jOK;
    
    private double initialAmount = 0.0;
    private boolean accepted = false;
    
    public JDialogInitialCash(JFrame parent) {
        super(parent, "💰 Fondo Inicial de Caja", true);
        initComponents();
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(400, 250);
        setLayout(new BorderLayout());
        
        // Panel principal
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Título
        JLabel titleLabel = new JLabel("💰 Ingrese el Fondo Inicial de Caja");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(titleLabel, gbc);
        
        // Descripción
        JLabel descLabel = new JLabel("<html><center>Ingrese la cantidad de dinero que tiene<br>en la caja al iniciar el turno</center></html>");
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 10, 15, 10);
        mainPanel.add(descLabel, gbc);
        
        // Label de cantidad
        JLabel amountLabel = new JLabel("💵 Cantidad (MX$):");
        amountLabel.setFont(new Font("Arial", Font.BOLD, 12));
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.insets = new Insets(5, 10, 5, 5);
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(amountLabel, gbc);
        
        // Campo de texto para la cantidad
        m_jInitialAmount = new JTextField(15);
        m_jInitialAmount.setFont(new Font("Arial", Font.PLAIN, 14));
        m_jInitialAmount.setText("0.00");
        m_jInitialAmount.selectAll();
        gbc.gridx = 1;
        gbc.insets = new Insets(5, 5, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(m_jInitialAmount, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        m_jOK = new JButton("✅ Confirmar Fondo Inicial");
        m_jOK.setFont(new Font("Arial", Font.BOLD, 14));
        m_jOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acceptAction();
            }
        });
        
        buttonPanel.add(m_jOK);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Focus inicial
        m_jInitialAmount.requestFocusInWindow();
        
        // Enter en el campo de texto activa OK
        m_jInitialAmount.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acceptAction();
            }
        });
    }
    
    private void acceptAction() {
        try {
            String text = m_jInitialAmount.getText().trim();
            if (text.isEmpty()) {
                text = "0.00";
            }
            initialAmount = Formats.CURRENCY.parseValue(text);
            if (initialAmount < 0) {
                // No permitir montos negativos
                m_jInitialAmount.setText("0.00");
                m_jInitialAmount.selectAll();
                return;
            }
            accepted = true;
            dispose();
        } catch (Exception e) {
            // Error al parsear, mantener el valor en 0
            m_jInitialAmount.setText("0.00");
            m_jInitialAmount.selectAll();
        }
    }
    
    /**
     * Obtiene la cantidad ingresada por el usuario
     * @return la cantidad del fondo inicial, o 0.0 si se canceló
     */
    public double getInitialAmount() {
        return initialAmount;
    }
    
    /**
     * Verifica si el usuario confirmó el diálogo
     * @return true cuando se confirma el fondo inicial
     */
    public boolean isAccepted() {
        return accepted;
    }
    
    /**
     * Establece una cantidad predeterminada
     * @param amount la cantidad predeterminada
     */
    public void setInitialAmount(double amount) {
        this.initialAmount = amount;
        m_jInitialAmount.setText(Formats.CURRENCY.formatValue(amount));
        m_jInitialAmount.selectAll();
    }
}