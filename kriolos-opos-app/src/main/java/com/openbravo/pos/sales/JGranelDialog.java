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
    private JButton btnAceptar;
    private JButton btnCancelar;
    private JToggleButton btnModoCalculo;
    private JLabel lblPrecioCalculado;
    private JLabel lblPesoCalculado;
    private JLabel lblModoValor;
    
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
        
        // Set size to 70% of screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screenSize.width * 0.7);
        int height = (int) (screenSize.height * 0.7);
        setSize(width, height);
        setResizable(false);

        getContentPane().setBackground(new Color(250, 250, 250));

        // --- Main Panel ---
        JPanel panelPrincipal = new JPanel(new BorderLayout(15, 15));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panelPrincipal.setBackground(new Color(250, 250, 250));

        // --- Top Info Panel (Red and Blue boxes) ---
        JPanel panelInfoSuperior = new JPanel(new GridLayout(1, 2, 15, 15));
        panelInfoSuperior.setBackground(new Color(250, 250, 250));

        JPanel panelPrecioUnitario = createInfoBox("Precio por Kilo", PRECIO_FORMAT.format(precioUnitario), new Color(211, 47, 47), Color.WHITE);
        
        // Manual creation of the "Mode" panel to get a reference to the value label
        JPanel panelModo = new JPanel(new BorderLayout());
        panelModo.setBackground(new Color(25, 118, 210)); // Blue
        panelModo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel lblModoTitle = new JLabel("Modo de Cálculo");
        lblModoTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblModoTitle.setForeground(Color.WHITE);
        lblModoTitle.setHorizontalAlignment(SwingConstants.CENTER);
        
        lblModoValor = new JLabel("Peso → Precio");
        lblModoValor.setFont(new Font("Arial", Font.BOLD, 48));
        lblModoValor.setForeground(Color.WHITE);
        lblModoValor.setHorizontalAlignment(SwingConstants.CENTER);
        
        panelModo.add(lblModoTitle, BorderLayout.NORTH);
        panelModo.add(lblModoValor, BorderLayout.CENTER);

        panelInfoSuperior.add(panelPrecioUnitario);
        panelInfoSuperior.add(panelModo);

        // --- Center Panel for Inputs and Calculated Values ---
        JPanel panelCentral = new JPanel(new BorderLayout(10, 10));
        panelCentral.setBackground(new Color(250, 250, 250));

        // --- Input fields panel (Kilo y Precio) ---
        JPanel panelEntrada = new JPanel(new GridBagLayout());
        panelEntrada.setBackground(new Color(250, 250, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        
        txtPeso = new JTextField("1.000");
        JPanel panelPeso = createInputPanel("Peso (Kg)", txtPeso);

        txtPrecio = new JTextField("");
        JPanel panelPrecio = createInputPanel("Precio ($)", txtPrecio);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Peso Panel
        gbc.gridx = 0;
        gbc.weightx = 0.4; // 40%
        panelEntrada.add(panelPeso, gbc);

        // Spacer
        gbc.gridx = 1;
        gbc.weightx = 0.2; // 20%
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        panelEntrada.add(spacer, gbc);

        // Precio Panel
        gbc.gridx = 2;
        gbc.weightx = 0.4; // 40%
        panelEntrada.add(panelPrecio, gbc);

        // --- Calculated info boxes panel (now semi-static) ---
        JPanel panelCalculadoInferior = new JPanel(new GridLayout(1, 2, 15, 0));
        panelCalculadoInferior.setBackground(new Color(250, 250, 250));
        panelCalculadoInferior.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        lblPesoCalculado = new JLabel("0.000 Kg", SwingConstants.CENTER);
        styleInfoLabel(lblPesoCalculado);
        panelCalculadoInferior.add(lblPesoCalculado);

        lblPrecioCalculado = new JLabel(PRECIO_FORMAT.format(0.0), SwingConstants.CENTER);
        styleInfoLabel(lblPrecioCalculado);
        panelCalculadoInferior.add(lblPrecioCalculado);

        panelCentral.add(panelEntrada, BorderLayout.CENTER);
        panelCentral.add(panelCalculadoInferior, BorderLayout.SOUTH);

        // --- Bottom Button Panel --- 
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        panelBotones.setBackground(new Color(250, 250, 250));
        
        btnModoCalculo = new JToggleButton("Modo (F2)");
        btnModoCalculo.setFont(new Font("Arial", Font.BOLD, 18));
        btnModoCalculo.setFocusPainted(false);
        btnModoCalculo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnModoCalculo.setToolTipText("<html><center>Presiona F2 para cambiar modo:<br>• Peso &rarr; Precio<br>• Precio &rarr; Peso</center></html>");
        
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
        
        panelBotones.add(btnModoCalculo);
        panelBotones.add(btnCancelar);
        panelBotones.add(btnAceptar);

        // --- Assemble everything ---
        panelPrincipal.add(panelInfoSuperior, BorderLayout.NORTH);
        panelPrincipal.add(panelCentral, BorderLayout.CENTER);
        panelPrincipal.add(panelBotones, BorderLayout.SOUTH);

        setContentPane(panelPrincipal);
        
        configurarModo();
        calcularValores();
        aplicarEfectosHover();
    }
    
    private JPanel createInputPanel(String title, JTextField textField) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5))
        );

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 24)); // Even larger font for input titles
        lblTitle.setForeground(new Color(66, 66, 66));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        panel.add(lblTitle, BorderLayout.NORTH);

        textField.setFont(new Font("Consolas", Font.BOLD, 72)); // Keep 72, already very large
        textField.setHorizontalAlignment(JTextField.CENTER); 
        textField.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        ((AbstractDocument) textField.getDocument()).setDocumentFilter(new DecimalDocumentFilter());
        panel.add(textField, BorderLayout.CENTER);

        return panel;
    }

    private void styleInfoLabel(JLabel label) {
        label.setFont(new Font("Arial", Font.BOLD, 60)); // Significantly larger for calculated values
        label.setOpaque(true);
        label.setBackground(new Color(238, 238, 238));
        label.setForeground(new Color(66, 66, 66));
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(224, 224, 224)),
            BorderFactory.createEmptyBorder(15, 10, 15, 10))
        );
    }

    private JPanel createInfoBox(String title, String value, Color bgColor, Color fgColor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        lblTitle.setForeground(fgColor);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Arial", Font.BOLD, 48));
        lblValue.setForeground(fgColor);
        lblValue.setHorizontalAlignment(SwingConstants.CENTER);
        
        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(lblValue, BorderLayout.CENTER);
        
        return panel;
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
                // Flechas arriba/abajo: incrementar/decrementar peso en 1.0 kg
                // PRIMERO: procesar flechas ANTES de otros tipos de teclas
                if ((e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN)) {
                    if (e.getSource() == txtPeso && txtPeso.isEnabled() && modoPesoAPrecio) {
                        try {
                            double valorActual = Double.parseDouble(txtPeso.getText().replace(",", ".").trim());
                            if (e.getKeyCode() == KeyEvent.VK_UP) {
                                valorActual += 1.0;
                            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                                valorActual -= 1.0;
                                if (valorActual < 0.0) valorActual = 0.0;
                            }
                            txtPeso.setText(String.format(java.util.Locale.US, "%.3f", valorActual));
                            // Usar invokeLater para asegurar que calcularValores() se ejecute DESPUES de setText()
                            SwingUtilities.invokeLater(() -> calcularValores());
                            e.consume();
                        } catch (NumberFormatException ex) {
                            if (e.getKeyCode() == KeyEvent.VK_UP) {
                                txtPeso.setText("1.000");
                            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                                txtPeso.setText("0.000");
                            }
                            SwingUtilities.invokeLater(() -> calcularValores());
                            e.consume();
                        }
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
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
            lblModoValor.setText("Peso → Precio");
            btnModoCalculo.setSelected(false);
            btnModoCalculo.setBackground(colorInactivo);
            btnModoCalculo.setForeground(Color.WHITE);
            
            txtPeso.setEnabled(true);
            txtPrecio.setEnabled(false);
            txtPeso.setBackground(Color.WHITE);
            txtPrecio.setBackground(new Color(245, 245, 245));
            
            setTitle("🏷️ Peso → Precio - Producto Granel");
        } else {
            // Modo: Precio → Peso
            lblModoValor.setText("Precio → Peso");
            btnModoCalculo.setSelected(true);
            btnModoCalculo.setBackground(colorActivo);
            btnModoCalculo.setForeground(Color.WHITE);
            
            txtPeso.setEnabled(false);
            txtPrecio.setEnabled(true);
            txtPeso.setBackground(new Color(245, 245, 245));
            txtPrecio.setBackground(Color.WHITE);
            
            setTitle("💰 Precio → Peso - Producto Granel");
        }
        establecerFocoInicial();
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
                } else {
                    btnModoCalculo.setBackground(btnModoCalculo.getBackground().brighter());
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
        FocusAdapter focusAdapter = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                JTextField source = (JTextField) e.getSource();
                if (source.isEnabled()) {
                    JPanel parent = (JPanel) source.getParent();
                    parent.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(33, 150, 243), 2),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4))
                    );
                    source.selectAll();
                }
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                JTextField source = (JTextField) e.getSource();
                JPanel parent = (JPanel) source.getParent();
                parent.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5))
                );
            }
        };
        
        txtPeso.addFocusListener(focusAdapter);
        txtPrecio.addFocusListener(focusAdapter);
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
                if (textoPeso.isEmpty() || textoPeso.equals(".")) {
                    btnAceptar.setEnabled(false);
                    return;
                }
                
                double peso = Double.parseDouble(textoPeso);
                double precioTotal = peso * precioUnitario;
                
                // Actualizar campo precio (solo visual, deshabilitado)
                txtPrecio.setText(String.format(java.util.Locale.US, "%.2f", precioTotal));
                
                btnAceptar.setEnabled(peso > 0);
                
            } else {
                // Calcular peso basado en precio
                String textoPrecio = txtPrecio.getText().trim();
                if (textoPrecio.isEmpty() || textoPrecio.equals(".")) {
                    btnAceptar.setEnabled(false);
                    return;
                }
                
                double precioDisponible = Double.parseDouble(textoPrecio);
                double pesoCalculado = (precioUnitario > 0) ? (precioDisponible / precioUnitario) : 0.0;
                
                // Actualizar campo peso (solo visual, deshabilitado)
                txtPeso.setText(String.format(java.util.Locale.US, "%.3f", pesoCalculado));
                
                btnAceptar.setEnabled(precioDisponible > 0);
            }
            
        } catch (NumberFormatException ex) {
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