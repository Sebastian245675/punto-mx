/*
 * JGranelDialog.java
 * 
 * Diálogo para entrada de peso en productos de granel
 * Estilo Eleventa: diseño simple con números grandes, campos lado a lado
 */
package com.openbravo.pos.sales;

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
 * Estilo Eleventa: valor a la izquierda, cantidad a la derecha, números grandes
 */
public class JGranelDialog extends JDialog {
    
    private static final DecimalFormat CANTIDAD_FORMAT = new DecimalFormat("0.000");
    private static final DecimalFormat PRECIO_FORMAT = new DecimalFormat("$#,##0.00");
    
    private JLabel lblNombreProducto;
    private JTextField txtValor;  // Valor/Importe a la izquierda
    private JTextField txtCantidad; // Cantidad a la derecha
    private JLabel lblPrecioUnidad;
    private JButton btnAceptar;
    private JButton btnCancelar;
    
    private double precioUnitario; // Precio por kilo
    private String nombreProducto;
    private double cantidadFinal = 0.0;
    private boolean aceptado = false;
    private boolean actualizando = false; // Bandera para evitar actualizaciones recursivas
    private JTextField campoActivo; // Campo que tiene el foco actualmente
    
    /** 
     * Constructor
     * @param parent Ventana padre
     * @param nombreProducto Nombre del producto
     * @param precioUnitario Precio por kilo del producto
     */
    public JGranelDialog(Window parent, String nombreProducto, double precioUnitario) {
        super(parent, "¿Cantidad del Producto?", ModalityType.APPLICATION_MODAL);
        this.precioUnitario = precioUnitario;
        this.nombreProducto = nombreProducto;
        initComponents();
        setupEvents();
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        
        // Tamaño del diálogo - normal, pero con números grandes
        setSize(800, 350);
        
        // Fondo blanco simple - SIN decoraciones de ventana
        getContentPane().setBackground(Color.WHITE);
        getContentPane().setLayout(new BorderLayout(0, 0));
        
        // Panel principal - fondo blanco
        JPanel panelPrincipal = new JPanel(new BorderLayout(0, 0));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        panelPrincipal.setBackground(Color.WHITE);
        panelPrincipal.setOpaque(true);
        
        // --- Nombre del producto arriba (muy pequeño, discreto) ---
        lblNombreProducto = new JLabel(nombreProducto);
        lblNombreProducto.setFont(new Font("Arial", Font.PLAIN, 12));
        lblNombreProducto.setHorizontalAlignment(SwingConstants.CENTER);
        lblNombreProducto.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        lblNombreProducto.setForeground(new Color(100, 100, 100));
        panelPrincipal.add(lblNombreProducto, BorderLayout.NORTH);
        
        // --- Panel central con campos lado a lado - SOLO LAS DOS CASILLAS ---
        JPanel panelCampos = new JPanel(new GridLayout(1, 2, 15, 0));
        panelCampos.setBackground(Color.WHITE);
        panelCampos.setOpaque(true);
        panelCampos.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Campo VALOR (Importe Actual) - IZQUIERDA
        CampoConPanel resultadoValor = crearCampoSimple("Importe Actual:", PRECIO_FORMAT.format(precioUnitario), false);
        txtValor = resultadoValor.campo;
        
        // Campo CANTIDAD - DERECHA
        CampoConPanel resultadoCantidad = crearCampoSimple("Cantidad del Producto:", "1.000", true);
        txtCantidad = resultadoCantidad.campo;
        
        panelCampos.add(resultadoValor.panel);
        panelCampos.add(resultadoCantidad.panel);
        
        panelPrincipal.add(panelCampos, BorderLayout.CENTER);
        
        // --- Precio Unidad (muy pequeño, discreto) ---
        lblPrecioUnidad = new JLabel("Precio Unidad = " + PRECIO_FORMAT.format(precioUnitario));
        lblPrecioUnidad.setFont(new Font("Arial", Font.PLAIN, 10));
        lblPrecioUnidad.setHorizontalAlignment(SwingConstants.CENTER);
        lblPrecioUnidad.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        lblPrecioUnidad.setForeground(new Color(120, 120, 120));
        
        // --- Panel de acciones (botones) - SIN borde decorativo ---
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 8));
        panelAcciones.setBackground(Color.WHITE);
        panelAcciones.setOpaque(true);
        panelAcciones.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        btnAceptar = new JButton("✓ Aceptar");
        btnAceptar.setPreferredSize(new Dimension(130, 40));
        btnAceptar.setFont(new Font("Arial", Font.BOLD, 14));
        btnAceptar.setFocusPainted(false);
        btnAceptar.setBackground(new Color(76, 175, 80));
        btnAceptar.setForeground(Color.WHITE);
        btnAceptar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnCancelar = new JButton("✗ Cancelar");
        btnCancelar.setPreferredSize(new Dimension(130, 40));
        btnCancelar.setFont(new Font("Arial", Font.BOLD, 14));
        btnCancelar.setFocusPainted(false);
        btnCancelar.setBackground(new Color(244, 67, 54));
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        panelAcciones.add(btnAceptar);
        panelAcciones.add(btnCancelar);
        
        // Panel sur con precio unidad y acciones
        JPanel panelSur = new JPanel(new BorderLayout(0, 3));
        panelSur.setBackground(Color.WHITE);
        panelSur.setOpaque(true);
        panelSur.add(lblPrecioUnidad, BorderLayout.NORTH);
        panelSur.add(panelAcciones, BorderLayout.CENTER);
        
        panelPrincipal.add(panelSur, BorderLayout.SOUTH);
        
        setContentPane(panelPrincipal);
        
        // Calcular importe inicial
        calcularDesdeCantidad();
        campoActivo = txtCantidad; // Empezar con cantidad activa
    }
    
    private CampoConPanel crearCampoSimple(String etiqueta, String valorInicial, boolean esCantidad) {
        JPanel panel = new JPanel(new BorderLayout(0, 3));
        panel.setBackground(Color.WHITE);
        panel.setOpaque(true);
        
        // Etiqueta muy pequeña arriba (casi invisible)
        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(new Font("Arial", Font.PLAIN, 9));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setForeground(new Color(150, 150, 150));
        panel.add(lbl, BorderLayout.NORTH);
        
        // Campo de texto con NÚMEROS MUY GRANDES
        JTextField campo = new JTextField(valorInicial);
        // NÚMEROS EXTRA GRANDES para el campo de cantidad (como en la segunda foto)
        int tamanoFuente = esCantidad ? 180 : 120; // Campo cantidad más grande
        campo.setFont(new Font("Arial", Font.BOLD, tamanoFuente));
        campo.setHorizontalAlignment(JTextField.CENTER);
        campo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 80), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        campo.setBackground(Color.WHITE);
        campo.setForeground(Color.BLACK);
        campo.setOpaque(true);
        
        panel.add(campo, BorderLayout.CENTER);
        
        // Aplicar filtro de documento
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new DecimalDocumentFilter());
        
        return new CampoConPanel(panel, campo);
    }
    
    /**
     * Clase auxiliar para devolver el panel y el campo de texto juntos
     */
    private static class CampoConPanel {
        JPanel panel;
        JTextField campo;
        
        CampoConPanel(JPanel panel, JTextField campo) {
            this.panel = panel;
            this.campo = campo;
        }
    }
    
    private javax.swing.event.DocumentListener listenerCantidad;
    private javax.swing.event.DocumentListener listenerValor;
    
    private void setupEvents() {
        // Eventos de cambio en cantidad
        listenerCantidad = new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (!actualizando) {
                    SwingUtilities.invokeLater(() -> {
                        if (!actualizando) {
                            calcularDesdeCantidad();
                        }
                    });
                }
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (!actualizando) {
                    SwingUtilities.invokeLater(() -> {
                        if (!actualizando) {
                            calcularDesdeCantidad();
                        }
                    });
                }
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                if (!actualizando) {
                    SwingUtilities.invokeLater(() -> {
                        if (!actualizando) {
                            calcularDesdeCantidad();
                        }
                    });
                }
            }
        };
        txtCantidad.getDocument().addDocumentListener(listenerCantidad);
        
        // Eventos de cambio en valor
        listenerValor = new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (!actualizando) {
                    SwingUtilities.invokeLater(() -> {
                        if (!actualizando) {
                            calcularDesdeValor();
                        }
                    });
                }
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (!actualizando) {
                    SwingUtilities.invokeLater(() -> {
                        if (!actualizando) {
                            calcularDesdeValor();
                        }
                    });
                }
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                if (!actualizando) {
                    SwingUtilities.invokeLater(() -> {
                        if (!actualizando) {
                            calcularDesdeValor();
                        }
                    });
                }
            }
        };
        txtValor.getDocument().addDocumentListener(listenerValor);
        
        // Eventos de teclado compartidos
        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    aceptar();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelar();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    // Incrementar el campo activo
                    incrementarCampoActivo();
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    // Decrementar el campo activo
                    decrementarCampoActivo();
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    // Cambiar a campo valor (izquierda)
                    txtValor.requestFocus();
                    txtValor.selectAll();
                    campoActivo = txtValor;
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    // Cambiar a campo cantidad (derecha)
                    txtCantidad.requestFocus();
                    txtCantidad.selectAll();
                    campoActivo = txtCantidad;
                    e.consume();
                }
            }
        };
        
        txtCantidad.addKeyListener(keyListener);
        txtValor.addKeyListener(keyListener);
        
        // Detectar qué campo tiene el foco
        txtCantidad.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                campoActivo = txtCantidad;
                txtCantidad.selectAll();
            }
        });
        
        txtValor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                campoActivo = txtValor;
                // Al ganar foco, mostrar solo el número sin formato
                try {
                    String texto = txtValor.getText().replace("$", "").replace(",", "").trim();
                    double valor = Double.parseDouble(texto);
                    txtValor.setText(String.format("%.2f", valor));
                    txtValor.selectAll();
                } catch (NumberFormatException ex) {
                    txtValor.selectAll();
                }
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                // Al perder foco, formatear como precio
                try {
                    String texto = txtValor.getText().replace("$", "").replace(",", "").trim();
                    double valor = Double.parseDouble(texto);
                    txtValor.setText(PRECIO_FORMAT.format(valor));
                } catch (NumberFormatException ex) {
                    txtValor.setText(PRECIO_FORMAT.format(0.0));
                }
            }
        });
        
        // Eventos de botones
        btnAceptar.addActionListener(e -> aceptar());
        btnCancelar.addActionListener(e -> cancelar());
        
        // Foco inicial en cantidad
        SwingUtilities.invokeLater(() -> {
            txtCantidad.requestFocus();
            campoActivo = txtCantidad;
        });
    }
    
    private void incrementarCampoActivo() {
        if (campoActivo == txtCantidad) {
            try {
                double cantidad = Double.parseDouble(txtCantidad.getText().trim());
                cantidad += 0.100;
                if (cantidad < 0) cantidad = 0;
                actualizando = true; // Evitar actualización recursiva
                txtCantidad.setText(CANTIDAD_FORMAT.format(cantidad));
                actualizando = false;
                calcularDesdeCantidad();
            } catch (NumberFormatException ex) {
                actualizando = true;
                txtCantidad.setText("1.000");
                actualizando = false;
                calcularDesdeCantidad();
            }
        } else if (campoActivo == txtValor) {
            try {
                String texto = txtValor.getText().replace("$", "").replace(",", "").trim();
                double valor = Double.parseDouble(texto);
                valor += precioUnitario;
                if (valor < 0) valor = 0;
                actualizando = true; // Evitar actualización recursiva
                // Mostrar sin formato mientras está activo
                txtValor.setText(String.format("%.2f", valor));
                actualizando = false;
                calcularDesdeValor();
            } catch (NumberFormatException ex) {
                actualizando = true;
                txtValor.setText(String.format("%.2f", precioUnitario));
                actualizando = false;
                calcularDesdeValor();
            }
        }
    }
    
    private void decrementarCampoActivo() {
        if (campoActivo == txtCantidad) {
            try {
                double cantidad = Double.parseDouble(txtCantidad.getText().trim());
                cantidad -= 0.100;
                if (cantidad < 0) cantidad = 0;
                actualizando = true; // Evitar actualización recursiva
                txtCantidad.setText(CANTIDAD_FORMAT.format(cantidad));
                actualizando = false;
                calcularDesdeCantidad();
            } catch (NumberFormatException ex) {
                actualizando = true;
                txtCantidad.setText("0.000");
                actualizando = false;
                calcularDesdeCantidad();
            }
        } else if (campoActivo == txtValor) {
            try {
                String texto = txtValor.getText().replace("$", "").replace(",", "").trim();
                double valor = Double.parseDouble(texto);
                valor -= precioUnitario;
                if (valor < 0) valor = 0;
                actualizando = true; // Evitar actualización recursiva
                // Mostrar sin formato mientras está activo
                txtValor.setText(String.format("%.2f", valor));
                actualizando = false;
                calcularDesdeValor();
            } catch (NumberFormatException ex) {
                actualizando = true;
                txtValor.setText("0.00");
                actualizando = false;
                calcularDesdeValor();
            }
        }
    }
    
    private void calcularDesdeCantidad() {
        if (actualizando) return;
        actualizando = true;
        try {
            // Remover listener temporalmente para evitar bucle infinito
            txtValor.getDocument().removeDocumentListener(listenerValor);
            
            String textoCantidad = txtCantidad.getText().trim();
            if (textoCantidad.isEmpty() || textoCantidad.equals(".")) {
                if (!txtValor.hasFocus()) {
                    txtValor.setText(PRECIO_FORMAT.format(0.0));
                }
                btnAceptar.setEnabled(false);
            } else {
                double cantidad = Double.parseDouble(textoCantidad);
                double valor = cantidad * precioUnitario;
                
                if (!txtValor.hasFocus()) {
                    txtValor.setText(PRECIO_FORMAT.format(valor));
                }
                btnAceptar.setEnabled(cantidad > 0);
            }
            
        } catch (NumberFormatException ex) {
            if (!txtValor.hasFocus()) {
                txtValor.setText(PRECIO_FORMAT.format(0.0));
            }
            btnAceptar.setEnabled(false);
        } finally {
            // Volver a agregar el listener
            try {
                txtValor.getDocument().addDocumentListener(listenerValor);
            } catch (Exception e) {
                // Ignorar si ya está agregado
            }
            actualizando = false;
        }
    }
    
    private void calcularDesdeValor() {
        if (actualizando) return;
        actualizando = true;
        try {
            // Remover listener temporalmente para evitar bucle infinito
            txtCantidad.getDocument().removeDocumentListener(listenerCantidad);
            
            String textoValor = txtValor.getText().replace("$", "").replace(",", "").trim();
            if (textoValor.isEmpty() || textoValor.equals(".")) {
                if (!txtCantidad.hasFocus()) {
                    txtCantidad.setText("0.000");
                }
                btnAceptar.setEnabled(false);
            } else {
                double valor = Double.parseDouble(textoValor);
                double cantidad = (precioUnitario > 0) ? (valor / precioUnitario) : 0.0;
                
                if (!txtCantidad.hasFocus()) {
                    txtCantidad.setText(CANTIDAD_FORMAT.format(cantidad));
                }
                btnAceptar.setEnabled(valor > 0);
                
                // Formatear el valor solo si no está en edición
                if (!txtValor.hasFocus()) {
                    txtValor.setText(PRECIO_FORMAT.format(valor));
                }
            }
            
        } catch (NumberFormatException ex) {
            if (!txtCantidad.hasFocus()) {
                txtCantidad.setText("0.000");
            }
            btnAceptar.setEnabled(false);
        } finally {
            // Volver a agregar el listener
            try {
                txtCantidad.getDocument().addDocumentListener(listenerCantidad);
            } catch (Exception e) {
                // Ignorar si ya está agregado
            }
            actualizando = false;
        }
    }
    
    private void aceptar() {
        try {
            double cantidad = Double.parseDouble(txtCantidad.getText().trim());
            
            if (cantidad <= 0) {
                JOptionPane.showMessageDialog(this, 
                    "La cantidad debe ser mayor a 0", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            cantidadFinal = cantidad;
            aceptado = true;
            dispose();
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, 
                "Formato de cantidad inválido", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            txtCantidad.requestFocus();
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
     * @return la cantidad (peso) ingresado por el usuario
     */
    public double getCantidad() {
        return cantidadFinal;
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
            
            // Remover caracteres de formato de precio
            text = text.replace("$", "").replace(",", "").trim();
            
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
     * Método estático para mostrar el diálogo estilo Eleventa
     * @param parent Ventana padre
     * @param nombreProducto Nombre del producto
     * @param precioUnitario Precio por kilo
     * @return la cantidad (peso) ingresado, o null si se canceló
     */
    public static Double mostrarDialogo(Window parent, String nombreProducto, double precioUnitario) {
        // Aplicar Look and Feel moderno antes de crear el diálogo
        ModernLookAndFeel.aplicarEstiloModerno();
        
        JGranelDialog dialog = new JGranelDialog(parent, nombreProducto, precioUnitario);
        
        // Aplicar estilo moderno específico al diálogo
        ModernLookAndFeel.aplicarEstiloModernoADialogo(dialog);
        
        // Asegurar que el diálogo sea visible
        dialog.setAlwaysOnTop(true);
        dialog.toFront();
        dialog.requestFocus();
        
        System.out.println("DEBUG: Mostrando diálogo granel...");
        dialog.setVisible(true);
        System.out.println("DEBUG: Diálogo visible: " + dialog.isVisible());
        
        if (dialog.fueAceptado()) {
            return dialog.getCantidad();
        } else {
            return null;
        }
    }
}
