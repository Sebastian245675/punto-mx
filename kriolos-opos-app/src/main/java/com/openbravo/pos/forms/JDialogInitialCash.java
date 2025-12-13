/*
 * Sebastian POS - Diálogo para ingresar fondo inicial de caja
 * Creado para manejar el fondo inicial cuando se abre una nueva caja
 */
package com.openbravo.pos.forms;

import com.openbravo.format.Formats;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

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
        super(parent, "Dinero en caja", true);
        initComponents();
        setLocationRelativeTo(parent);
        setUndecorated(false);
    }
    
    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(600, 420);
        setLayout(new BorderLayout());
        setResizable(false);
        getContentPane().setBackground(Color.WHITE); // Fondo blanco para el contenido
        
        // Panel principal con fondo blanco
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setOpaque(true); // Asegurar que el panel sea opaco
        mainPanel.setBorder(new EmptyBorder(30, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Pregunta principal
        JLabel questionLabel = new JLabel("¿Efectivo Inicial en Caja ?");
        questionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        questionLabel.setForeground(new Color(50, 50, 50));
        questionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, 25, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        mainPanel.add(questionLabel, gbc);
        
        // Campo de texto para la cantidad con borde azul
        m_jInitialAmount = new JTextField(15);
        // Fuente aún más grande y gruesa
        Font boldFont = new Font("Segoe UI", Font.BOLD, 56);
        m_jInitialAmount.setFont(boldFont);
        m_jInitialAmount.setHorizontalAlignment(SwingConstants.CENTER);
        m_jInitialAmount.setBorder(createBlueBorder());
        m_jInitialAmount.setPreferredSize(new Dimension(450, 90));
        m_jInitialAmount.setMinimumSize(new Dimension(450, 90));
        m_jInitialAmount.setBackground(Color.WHITE);
        m_jInitialAmount.setOpaque(true);
        m_jInitialAmount.setForeground(Color.BLACK);
        // Establecer texto inicial después de configurar todas las propiedades
        String initialText = Formats.CURRENCY.formatValue(0.0);
        m_jInitialAmount.setText(initialText);
        m_jInitialAmount.setCaretPosition(initialText.length());
        m_jInitialAmount.selectAll();
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 30, 0);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(m_jInitialAmount, gbc);
        
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        
        // Panel de botones con fondo blanco
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setOpaque(true); // Asegurar que el panel sea opaco
        buttonPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        // Botón con icono y estilo moderno
        m_jOK = new JButton("Registrar dinero inicial en caja") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dibujar icono de billetes verdes a la izquierda
                int iconSize = 20;
                int iconX = 15;
                int iconY = (getHeight() - iconSize) / 2;
                
                // Simular billetes verdes apilados
                g2.setColor(new Color(76, 175, 80)); // Verde
                g2.fillRect(iconX, iconY, iconSize, iconSize * 2 / 3);
                g2.setColor(new Color(56, 142, 60)); // Verde oscuro
                g2.fillRect(iconX + 2, iconY + 2, iconSize - 4, iconSize * 2 / 3 - 4);
                g2.setColor(new Color(76, 175, 80));
                g2.fillRect(iconX + 3, iconY + 3, iconSize - 6, iconSize * 2 / 3 - 6);
                
                super.paintComponent(g);
                g2.dispose();
            }
        };
        m_jOK.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        m_jOK.setPreferredSize(new Dimension(280, 45));
        m_jOK.setBackground(new Color(33, 150, 243)); // Azul moderno
        m_jOK.setForeground(Color.WHITE);
        m_jOK.setFocusPainted(false);
        m_jOK.setBorderPainted(false);
        m_jOK.setOpaque(true); // Importante: hacer el botón opaco para que se vea el color
        m_jOK.setContentAreaFilled(true); // Asegurar que se llene el área
        m_jOK.setBorder(new EmptyBorder(0, 45, 0, 15)); // Espacio para el icono
        m_jOK.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        
        // Efecto hover
        m_jOK.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                m_jOK.setBackground(new Color(25, 118, 210));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                m_jOK.setBackground(new Color(33, 150, 243));
            }
        });
        
        m_jOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acceptAction();
            }
        });
        
        buttonPanel.add(m_jOK);
        
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        
        // Asegurar que el diálogo sea visible
        setVisible(false); // Primero ocultar para configurar todo
        validate();
        repaint();
        
        // Focus inicial después de mostrar
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                m_jInitialAmount.requestFocusInWindow();
                m_jInitialAmount.selectAll();
            }
        });
        
        // Enter en el campo de texto activa OK
        m_jInitialAmount.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acceptAction();
            }
        });
    }
    
    /**
     * Crea un borde azul para el campo de texto
     */
    private Border createBlueBorder() {
        Border blueBorder = BorderFactory.createLineBorder(new Color(33, 150, 243), 2);
        Border padding = new EmptyBorder(10, 15, 10, 15);
        return new CompoundBorder(blueBorder, padding);
    }
    
    private void acceptAction() {
        try {
            String text = m_jInitialAmount.getText().trim();
            if (text.isEmpty()) {
                text = "0.00";
            }
            
            // Sebastian - Intentar parsear primero como número simple (sin formato)
            // Esto evita problemas cuando el usuario ingresa solo números (ej: "4545")
            // y el formato de moneda usa punto como separador de miles
            try {
                // Remover todos los caracteres no numéricos excepto punto y coma
                String cleanText = text.replaceAll("[^0-9.,]", "");
                
                // Si el texto limpio contiene solo números (sin punto ni coma), parsearlo directamente
                if (cleanText.matches("^[0-9]+$")) {
                    // Es un número entero sin formato, parsearlo directamente
                    initialAmount = Double.parseDouble(cleanText);
                } else {
                    // Tiene punto o coma, intentar parsearlo como número decimal
                    // Reemplazar coma por punto para parseo estándar
                    String normalizedText = cleanText.replace(',', '.');
                    // Si hay múltiples puntos, el último es el decimal
                    int lastDotIndex = normalizedText.lastIndexOf('.');
                    if (lastDotIndex > 0) {
                        // Hay punto(s), el último es el decimal
                        String beforeLastDot = normalizedText.substring(0, lastDotIndex).replace(".", "");
                        String afterLastDot = normalizedText.substring(lastDotIndex + 1);
                        normalizedText = beforeLastDot + "." + afterLastDot;
                    }
                    initialAmount = Double.parseDouble(normalizedText);
                }
            } catch (NumberFormatException e) {
                // Si falla el parseo simple, intentar con el formato de moneda
                initialAmount = Formats.CURRENCY.parseValue(text);
            }
            
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