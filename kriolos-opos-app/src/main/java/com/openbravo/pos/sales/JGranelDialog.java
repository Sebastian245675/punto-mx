/*
 * JGranelDialog.java
 * 
 * Diálogo para entrada de peso en productos de granel
 * Estilo Eleventa: muestra peso y precio calculado en tiempo real
 */
package com.openbravo.pos.sales;

import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.util.ModernLookAndFeel;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Diálogo para entrada de peso en productos de granel
 * Calcula automáticamente el precio basado en el peso ingresado
 */
public class JGranelDialog extends JDialog {
    
    private static final DecimalFormat PESO_FORMAT = new DecimalFormat("0.000");
    private static final DecimalFormat PRECIO_FORMAT = new DecimalFormat("$#,##0.00");
    
    private JTextField txtPeso;
    private JTextField txtPrecio;
    private JLabel lblPrecioCalculado;
    private JLabel lblPesoCalculado;
    private JButton btnAceptar;
    private JButton btnCancelar;
    private JToggleButton btnModoCalculo;
    
    private double precioUnitario; // Precio por kilo
    private double pesoFinal = 0.0;
    private boolean aceptado = false;
    private boolean modoPesoAPrecio = true; // true = peso->precio, false = precio->peso
    
    /** 
     * Constructor
     * @param parent Ventana padre
     * @param precioUnitario Precio porkilo del producto
     */
    public JGranelDialog(Window parent, double precioUnitario) {
        super(parent, "Ingreso de Peso - Producto Granel", ModalityType.APPLICATION_MODAL);
        this.precioUnitario = precioUnitario;
        initComponents();
        setupEvents();
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        
        // Aplicar estilo moderno al diálogo
        getContentPane().setBackground(new Color(250, 250, 250));
        
        // Panel principal con diseño moderno
        JPanel panelPrincipal = new JPanel(new BorderLayout(15, 15));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panelPrincipal.setBackground(new Color(250, 250, 250));
        
        // Panel superior con información del producto - Estilo Material
        JPanel panelInfo = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        panelInfo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(224, 224, 224), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panelInfo.setBackground(Color.WHITE);
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        JLabel lblTituloPrecio = new JLabel("Precio por Kilo:");
        lblTituloPrecio.setFont(new Font("Arial", Font.BOLD, 14));
        lblTituloPrecio.setForeground(new Color(66, 66, 66));
        panelInfo.add(lblTituloPrecio, gbc);
        
        gbc.gridx = 1;
        JLabel lblPrecioUnitario = new JLabel(PRECIO_FORMAT.format(precioUnitario));
        lblPrecioUnitario.setFont(new Font("Arial", Font.BOLD, 16));
        lblPrecioUnitario.setForeground(new Color(76, 175, 80)); // Material Green
        panelInfo.add(lblPrecioUnitario, gbc);
        
        // Botón para cambiar modo de cálculo - Estilo moderno
        gbc.gridx = 2;
        gbc.insets = new Insets(5, 25, 5, 5);
        btnModoCalculo = new JToggleButton("Calcular por $");
        btnModoCalculo.setFont(new Font("Arial", Font.BOLD, 12));
        btnModoCalculo.setFocusPainted(false);
        btnModoCalculo.setBorderPainted(false);
        btnModoCalculo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnModoCalculo.setToolTipText("<html><center>Presiona F2 para cambiar modo:<br>• Peso → Precio<br>• Precio → Peso</center></html>");
        panelInfo.add(btnModoCalculo, gbc);
        
        // Panel central con entrada de datos - Diseño moderno
        JPanel panelEntrada = new JPanel(new GridBagLayout());
        panelEntrada.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(224, 224, 224), 1),
                "Ingreso de Datos",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 14),
                new Color(66, 66, 66)
            ),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panelEntrada.setBackground(Color.WHITE);
        
        gbc = new GridBagConstraints();
        
        // Campo de peso con estilo moderno
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 5, 10);
        JLabel lblPeso = new JLabel("Peso (Kg):");
        lblPeso.setFont(new Font("Arial", Font.BOLD, 13));
        lblPeso.setForeground(new Color(66, 66, 66));
        panelEntrada.add(lblPeso, gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtPeso = new JTextField("1.000", 12);
        txtPeso.setFont(new Font("Consolas", Font.BOLD, 18));
        txtPeso.setHorizontalAlignment(JTextField.RIGHT);
        txtPeso.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(224, 224, 224), 2),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        ((AbstractDocument) txtPeso.getDocument()).setDocumentFilter(new DecimalDocumentFilter());
        panelEntrada.add(txtPeso, gbc);
        
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        JLabel lblUnidadPeso = new JLabel(" Kg");
        lblUnidadPeso.setFont(new Font("Arial", Font.BOLD, 13));
        lblUnidadPeso.setForeground(new Color(120, 120, 120));
        panelEntrada.add(lblUnidadPeso, gbc);
        
        // Campo de precio con estilo moderno
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 10, 10);
        JLabel lblPrecio = new JLabel("Precio ($):");
        lblPrecio.setFont(new Font("Arial", Font.BOLD, 13));
        lblPrecio.setForeground(new Color(66, 66, 66));
        panelEntrada.add(lblPrecio, gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtPrecio = new JTextField("", 12);
        txtPrecio.setFont(new Font("Consolas", Font.BOLD, 18));
        txtPrecio.setHorizontalAlignment(JTextField.RIGHT);
        txtPrecio.setEnabled(false);
        txtPrecio.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(224, 224, 224), 2),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        ((AbstractDocument) txtPrecio.getDocument()).setDocumentFilter(new DecimalDocumentFilter());
        panelEntrada.add(txtPrecio, gbc);
        
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        JLabel lblUnidadPrecio = new JLabel(" $");
        lblUnidadPrecio.setFont(new Font("Arial", Font.BOLD, 13));
        lblUnidadPrecio.setForeground(new Color(120, 120, 120));
        panelEntrada.add(lblUnidadPrecio, gbc);
        
        // Panel con resultados calculados - Estilo elegante
        JPanel panelResultados = new JPanel(new GridBagLayout());
        panelResultados.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(224, 224, 224), 1),
                "Resultados",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 14),
                new Color(66, 66, 66)
            ),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panelResultados.setBackground(Color.WHITE);
        
        gbc = new GridBagConstraints();
        
        // Precio calculado con diseño moderno
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 10);
        JLabel lblTotalPagar = new JLabel("Total a Pagar:");
        lblTotalPagar.setFont(new Font("Arial", Font.BOLD, 13));
        lblTotalPagar.setForeground(new Color(66, 66, 66));
        panelResultados.add(lblTotalPagar, gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        lblPrecioCalculado = new JLabel(PRECIO_FORMAT.format(precioUnitario));
        lblPrecioCalculado.setFont(new Font("Arial", Font.BOLD, 20));
        lblPrecioCalculado.setForeground(new Color(33, 150, 243)); // Material Blue
        lblPrecioCalculado.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(33, 150, 243), 2),
            BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));
        lblPrecioCalculado.setOpaque(true);
        lblPrecioCalculado.setBackground(new Color(227, 242, 253)); // Azul claro
        lblPrecioCalculado.setHorizontalAlignment(SwingConstants.CENTER);
        panelResultados.add(lblPrecioCalculado, gbc);
        
        // Peso calculado con diseño moderno
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 10);
        JLabel lblCantidad = new JLabel("Cantidad (Kg):");
        lblCantidad.setFont(new Font("Arial", Font.BOLD, 13));
        lblCantidad.setForeground(new Color(66, 66, 66));
        panelResultados.add(lblCantidad, gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        lblPesoCalculado = new JLabel(PESO_FORMAT.format(1.0) + " Kg");
        lblPesoCalculado.setFont(new Font("Arial", Font.BOLD, 20));
        lblPesoCalculado.setForeground(new Color(255, 87, 34)); // Material Deep Orange
        lblPesoCalculado.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 87, 34), 2),
            BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));
        lblPesoCalculado.setOpaque(true);
        lblPesoCalculado.setBackground(new Color(255, 243, 224)); // Naranja claro
        lblPesoCalculado.setHorizontalAlignment(SwingConstants.CENTER);
        panelResultados.add(lblPesoCalculado, gbc);
        
        // Panel de botones con estilo moderno
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        panelBotones.setBackground(new Color(250, 250, 250));
        
        btnCancelar = new JButton("Cancelar");
        btnCancelar.setPreferredSize(new Dimension(120, 40));
        btnCancelar.setFont(new Font("Arial", Font.BOLD, 13));
        btnCancelar.setFocusPainted(false);
        btnCancelar.setBorderPainted(false);
        btnCancelar.setBackground(new Color(244, 67, 54)); // Material Red
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnAceptar = new JButton("Aceptar");
        btnAceptar.setPreferredSize(new Dimension(120, 40));
        btnAceptar.setFont(new Font("Arial", Font.BOLD, 13));
        btnAceptar.setFocusPainted(false);
        btnAceptar.setBorderPainted(false);
        btnAceptar.setBackground(new Color(76, 175, 80)); // Material Green
        btnAceptar.setForeground(Color.WHITE);
        btnAceptar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        panelBotones.add(btnCancelar);
        panelBotones.add(btnAceptar);
        
        // Ensamblar paneles
        JPanel panelCentral = new JPanel(new GridLayout(2, 1, 0, 15));
        panelCentral.setBackground(new Color(250, 250, 250));
        panelCentral.add(panelEntrada);
        panelCentral.add(panelResultados);
        
        panelPrincipal.add(panelInfo, BorderLayout.NORTH);
        panelPrincipal.add(panelCentral, BorderLayout.CENTER);
        panelPrincipal.add(panelBotones, BorderLayout.SOUTH);
        
        setContentPane(panelPrincipal);
        pack();
        
        // Configurar modo inicial
        configurarModo();
        
        // Calcular valores iniciales
        calcularValores();
        
        // Aplicar efectos hover a los botones
        aplicarEfectosHover();
    }
    
    private void setupEvents() {
        // Evento para cambiar modo de cálculo
        btnModoCalculo.addActionListener(e -> {
            modoPesoAPrecio = !modoPesoAPrecio;
            configurarModo();
            calcularValores();
        });
        
        // Evento para calcular en tiempo real - campo peso
        txtPeso.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (modoPesoAPrecio) {
                    SwingUtilities.invokeLater(() -> calcularValores());
                }
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (modoPesoAPrecio) {
                    SwingUtilities.invokeLater(() -> calcularValores());
                }
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                if (modoPesoAPrecio) {
                    SwingUtilities.invokeLater(() -> calcularValores());
                }
            }
        });
        
        // Evento para calcular en tiempo real - campo precio
        txtPrecio.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (!modoPesoAPrecio) {
                    SwingUtilities.invokeLater(() -> calcularValores());
                }
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (!modoPesoAPrecio) {
                    SwingUtilities.invokeLater(() -> calcularValores());
                }
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                if (!modoPesoAPrecio) {
                    SwingUtilities.invokeLater(() -> calcularValores());
                }
            }
        });
        
        // Eventos de teclado
        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    aceptar();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelar();
                } else if (e.getKeyCode() == KeyEvent.VK_F2) {
                    btnModoCalculo.doClick();
                }
            }
        };
        
        txtPeso.addKeyListener(keyListener);
        txtPrecio.addKeyListener(keyListener);
        
        // Eventos de botones
        btnAceptar.addActionListener(e -> aceptar());
        btnCancelar.addActionListener(e -> cancelar());
        
        // Seleccionar todo el texto al ganar foco
        FocusAdapter focusAdapter = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (e.getSource() instanceof JTextField) {
                    ((JTextField) e.getSource()).selectAll();
                }
            }
        };
        
        txtPeso.addFocusListener(focusAdapter);
        txtPrecio.addFocusListener(focusAdapter);
        
        // Configurar foco inicial
        SwingUtilities.invokeLater(this::establecerFocoInicial);
    }
    
    private void configurarModo() {
        Color colorActivo = new Color(76, 175, 80);    // Material Green
        Color colorInactivo = new Color(158, 158, 158); // Material Grey
        
        if (modoPesoAPrecio) {
            // Modo: Peso → Precio
            btnModoCalculo.setText("Calcular por $");
            btnModoCalculo.setSelected(false);
            btnModoCalculo.setBackground(colorInactivo);
            btnModoCalculo.setForeground(Color.WHITE);
            
            txtPeso.setEnabled(true);
            txtPrecio.setEnabled(false);
            txtPeso.setBackground(Color.WHITE);
            txtPrecio.setBackground(new Color(245, 245, 245));
            
            // Borde activo para peso
            txtPeso.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(63, 81, 181), 2),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));
            
            setTitle("🏷️ Peso → Precio - Producto Granel");
        } else {
            // Modo: Precio → Peso
            btnModoCalculo.setText("Calcular por Kg");
            btnModoCalculo.setSelected(true);
            btnModoCalculo.setBackground(colorActivo);
            btnModoCalculo.setForeground(Color.WHITE);
            
            txtPeso.setEnabled(false);
            txtPrecio.setEnabled(true);
            txtPeso.setBackground(new Color(245, 245, 245));
            txtPrecio.setBackground(Color.WHITE);
            
            // Borde activo para precio
            txtPrecio.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(63, 81, 181), 2),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));
            
            setTitle("💰 Precio → Peso - Producto Granel");
        }
    }
    
    /**
     * Aplica efectos hover modernos a los botones
     */
    private void aplicarEfectosHover() {
        // Efecto hover para botón Aceptar
        btnAceptar.addMouseListener(new MouseAdapter() {
            Color colorOriginal = btnAceptar.getBackground();
            
            @Override
            public void mouseEntered(MouseEvent e) {
                btnAceptar.setBackground(colorOriginal.brighter());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                btnAceptar.setBackground(colorOriginal);
            }
        });
        
        // Efecto hover para botón Cancelar
        btnCancelar.addMouseListener(new MouseAdapter() {
            Color colorOriginal = btnCancelar.getBackground();
            
            @Override
            public void mouseEntered(MouseEvent e) {
                btnCancelar.setBackground(colorOriginal.brighter());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                btnCancelar.setBackground(colorOriginal);
            }
        });
        
        // Efecto hover para botón de modo
        btnModoCalculo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!btnModoCalculo.isSelected()) {
                    btnModoCalculo.setBackground(new Color(189, 189, 189));
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                configurarModo(); // Restaurar colores originales
            }
        });
        
        // Efectos focus modernos para campos de texto
        aplicarEfectosFocusModernos();
    }
    
    /**
     * Aplica efectos de focus modernos a los campos de texto
     */
    private void aplicarEfectosFocusModernos() {
        // Efecto focus para campo peso
        txtPeso.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtPeso.isEnabled()) {
                    txtPeso.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(33, 150, 243), 3),
                        BorderFactory.createEmptyBorder(9, 15, 9, 15)
                    ));
                    txtPeso.selectAll();
                }
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                if (txtPeso.isEnabled()) {
                    txtPeso.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(224, 224, 224), 2),
                        BorderFactory.createEmptyBorder(10, 15, 10, 15)
                    ));
                }
            }
        });
        
        // Efecto focus para campo precio
        txtPrecio.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtPrecio.isEnabled()) {
                    txtPrecio.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(33, 150, 243), 3),
                        BorderFactory.createEmptyBorder(9, 15, 9, 15)
                    ));
                    txtPrecio.selectAll();
                }
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                if (txtPrecio.isEnabled()) {
                    txtPrecio.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(224, 224, 224), 2),
                        BorderFactory.createEmptyBorder(10, 15, 10, 15)
                    ));
                }
            }
        });
    }
    
    private void establecerFocoInicial() {
        if (modoPesoAPrecio) {
            txtPeso.requestFocus();
        } else {
            txtPrecio.requestFocus();
        }
    }
    
    private void calcularValores() {
        try {
            if (modoPesoAPrecio) {
                // Calcular precio basado en peso
                String textoPeso = txtPeso.getText().trim();
                if (textoPeso.isEmpty()) {
                    lblPrecioCalculado.setText(PRECIO_FORMAT.format(0.0));
                    lblPesoCalculado.setText(PESO_FORMAT.format(0.0) + " Kg");
                    btnAceptar.setEnabled(false);
                    return;
                }
                
                double peso = Double.parseDouble(textoPeso);
                double precioTotal = peso * precioUnitario;
                
                lblPrecioCalculado.setText(PRECIO_FORMAT.format(precioTotal));
                lblPesoCalculado.setText(PESO_FORMAT.format(peso) + " Kg");
                
                // Actualizar campo precio (solo visual, deshabilitado)
                txtPrecio.setText(String.format("%.2f", precioTotal));
                
                btnAceptar.setEnabled(peso > 0);
                
            } else {
                // Calcular peso basado en precio
                String textoPrecio = txtPrecio.getText().trim();
                if (textoPrecio.isEmpty()) {
                    lblPrecioCalculado.setText(PRECIO_FORMAT.format(0.0));
                    lblPesoCalculado.setText(PESO_FORMAT.format(0.0) + " Kg");
                    btnAceptar.setEnabled(false);
                    return;
                }
                
                double precioDisponible = Double.parseDouble(textoPrecio);
                double pesoCalculado = precioDisponible / precioUnitario;
                
                lblPrecioCalculado.setText(PRECIO_FORMAT.format(precioDisponible));
                lblPesoCalculado.setText(PESO_FORMAT.format(pesoCalculado) + " Kg");
                
                // Actualizar campo peso (solo visual, deshabilitado)
                txtPeso.setText(PESO_FORMAT.format(pesoCalculado));
                
                btnAceptar.setEnabled(precioDisponible > 0);
            }
            
        } catch (NumberFormatException ex) {
            lblPrecioCalculado.setText(PRECIO_FORMAT.format(0.0));
            lblPesoCalculado.setText(PESO_FORMAT.format(0.0) + " Kg");
            btnAceptar.setEnabled(false);
        }
    }
    
    private void aceptar() {
        try {
            double peso;
            
            if (modoPesoAPrecio) {
                // Modo peso → precio: obtener peso del campo de texto
                peso = Double.parseDouble(txtPeso.getText().trim());
            } else {
                // Modo precio → peso: calcular peso basado en precio ingresado
                double precioDisponible = Double.parseDouble(txtPrecio.getText().trim());
                peso = precioDisponible / precioUnitario;
            }
            
            if (peso <= 0) {
                JOptionPane.showMessageDialog(this, 
                    "El peso debe ser mayor a 0", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            pesoFinal = peso;
            aceptado = true;
            dispose();
            
        } catch (NumberFormatException ex) {
            String mensaje = modoPesoAPrecio ? 
                "Formato de peso inválido" : 
                "Formato de precio inválido";
            
            JOptionPane.showMessageDialog(this, 
                mensaje, 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            
            establecerFocoInicial();
        }
    }
    
    private void cancelar() {
        aceptado = false;
        dispose();
    }
    
    /**
     * @return true si el usuario aceptó, false si canceló
     */
    public boolean fueAceptado() {
        return aceptado;
    }
    
    /**
     * @return el peso ingresado por el usuario
     */
    public double getPeso() {
        return pesoFinal;
    }
    
    /**
     * Filtro para permitir solo entrada de números decimales
     */
    private static class DecimalDocumentFilter extends DocumentFilter {
        
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            
            if (isValidInput(fb.getDocument().getText(0, fb.getDocument().getLength()), 
                           string, offset)) {
                super.insertString(fb, offset, string, attr);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }
        
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            
            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
            String beforeOffset = currentText.substring(0, offset);
            String afterOffset = currentText.substring(offset + length);
            String newText = beforeOffset + text + afterOffset;
            
            if (isValidDecimal(newText)) {
                super.replace(fb, offset, length, text, attrs);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }
        
        private boolean isValidInput(String currentText, String newInput, int offset) {
            String beforeOffset = currentText.substring(0, offset);
            String afterOffset = currentText.substring(offset);
            String resultText = beforeOffset + newInput + afterOffset;
            
            return isValidDecimal(resultText);
        }
        
        private boolean isValidDecimal(String text) {
            if (text.isEmpty()) return true;
            
            // Permitir solo un punto decimal
            int dotCount = 0;
            for (char c : text.toCharArray()) {
                if (c == '.') {
                    dotCount++;
                    if (dotCount > 1) return false;
                } else if (!Character.isDigit(c)) {
                    return false;
                }
            }
            
            // Verificar que sea un número válido
            try {
                Double.parseDouble(text);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
    
    /**
     * Método estático para mostrar el diálogo con estilo moderno
     * @param parent Ventana padre
     * @param precioUnitario Precio por kilo
     * @return el peso ingresado, o null si se canceló
     */
    public static Double mostrarDialogo(Window parent, double precioUnitario) {
        // Aplicar Look and Feel moderno antes de crear el diálogo
        ModernLookAndFeel.aplicarEstiloModerno();
        
        JGranelDialog dialog = new JGranelDialog(parent, precioUnitario);
        
        // Aplicar estilo moderno específico al diálogo
        ModernLookAndFeel.aplicarEstiloModernoADialogo(dialog);
        
        dialog.setVisible(true);
        
        if (dialog.fueAceptado()) {
            return dialog.getPeso();
        } else {
            return null;
        }
    }
}