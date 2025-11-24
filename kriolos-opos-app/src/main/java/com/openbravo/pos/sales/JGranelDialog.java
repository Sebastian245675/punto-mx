/*
 * JGranelDialog.java
 * 
 * Diálogo para entrada de peso en productos de granel
 * Estilo Eleventa: diseño simple con números grandes, campos lado a lado
 */
package com.openbravo.pos.sales;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.text.ParseException;
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
        
        System.out.println("DEBUG GRANEL - Constructor:");
        System.out.println("  Producto: " + nombreProducto);
        System.out.println("  Precio Unitario recibido: " + precioUnitario);
        System.out.println("  Precio Unitario formateado: " + PRECIO_FORMAT.format(precioUnitario));
        
        initComponents();
        setupEvents();
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        
        // Tamaño del diálogo ajustado para casillas panorámicas
        setSize(1300, 500);
        
        // Fondo blanco limpio
        getContentPane().setBackground(new Color(250, 250, 250));
        getContentPane().setLayout(new BorderLayout(0, 0));
        
        // Panel principal con fondo suave
        JPanel panelPrincipal = new JPanel(new BorderLayout(0, 0));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panelPrincipal.setBackground(new Color(250, 250, 250));
        panelPrincipal.setOpaque(true);
        
        // --- Nombre del producto arriba - más visible y elegante ---
        lblNombreProducto = new JLabel(nombreProducto);
        lblNombreProducto.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblNombreProducto.setHorizontalAlignment(SwingConstants.CENTER);
        lblNombreProducto.setBorder(BorderFactory.createEmptyBorder(10, 10, 15, 10));
        lblNombreProducto.setForeground(new Color(50, 50, 50));
        panelPrincipal.add(lblNombreProducto, BorderLayout.NORTH);
        
        // --- Panel central con campos lado a lado - SOLO LAS DOS CASILLAS ---
        JPanel panelCampos = new JPanel(new GridLayout(1, 2, 30, 0));
        panelCampos.setBackground(new Color(250, 250, 250));
        panelCampos.setOpaque(true);
        panelCampos.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        
        // Campo VALOR (Importe Actual) - IZQUIERDA
        CampoConPanel resultadoValor = crearCampoSimple("Importe Actual:", PRECIO_FORMAT.format(precioUnitario), false);
        txtValor = resultadoValor.campo;
        
        // Campo CANTIDAD - DERECHA (iniciar en 1.000 kilo)
        CampoConPanel resultadoCantidad = crearCampoSimple("Cantidad del Producto:", "1.000", true);
        txtCantidad = resultadoCantidad.campo;
        
        panelCampos.add(resultadoValor.panel);
        panelCampos.add(resultadoCantidad.panel);
        
        panelPrincipal.add(panelCampos, BorderLayout.CENTER);
        
        // --- Precio Unidad - más visible y elegante ---
        lblPrecioUnidad = new JLabel("Precio Unidad = " + PRECIO_FORMAT.format(precioUnitario));
        lblPrecioUnidad.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblPrecioUnidad.setHorizontalAlignment(SwingConstants.CENTER);
        lblPrecioUnidad.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        lblPrecioUnidad.setForeground(new Color(100, 100, 100));
        
        // --- Panel de acciones (botones) - SIN borde decorativo ---
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 8));
        panelAcciones.setBackground(Color.WHITE);
        panelAcciones.setOpaque(true);
        panelAcciones.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        btnAceptar = new JButton("✓ Aceptar");
        btnAceptar.setPreferredSize(new Dimension(160, 50));
        btnAceptar.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnAceptar.setFocusPainted(false);
        btnAceptar.setBackground(new Color(76, 175, 80));
        btnAceptar.setForeground(Color.WHITE);
        btnAceptar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAceptar.setBorder(BorderFactory.createRaisedBevelBorder());
        
        btnCancelar = new JButton("✗ Cancelar");
        btnCancelar.setPreferredSize(new Dimension(160, 50));
        btnCancelar.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnCancelar.setFocusPainted(false);
        btnCancelar.setBackground(new Color(244, 67, 54));
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelar.setBorder(BorderFactory.createRaisedBevelBorder());
        
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
        
        // Aplicar fuentes grandes inmediatamente después de inicializar
        aplicarFuentesGrandes();
    }
    
    /**
     * Método para aplicar fuentes grandes a los campos
     * Se llama después de inicializar y puede ser llamado nuevamente si se sobrescriben
     */
    private void aplicarFuentesGrandes() {
        if (txtCantidad != null) {
            Font fuenteCantidad = new Font("Segoe UI", Font.BOLD, 140);
            txtCantidad.setFont(fuenteCantidad);
            System.out.println("DEBUG: Aplicando fuente cantidad: " + fuenteCantidad.getSize() + " puntos");
        }
        if (txtValor != null) {
            Font fuenteValor = new Font("Segoe UI", Font.BOLD, 100);
            txtValor.setFont(fuenteValor);
            System.out.println("DEBUG: Aplicando fuente valor: " + fuenteValor.getSize() + " puntos");
        }
    }
    
    private CampoConPanel crearCampoSimple(String etiqueta, String valorInicial, boolean esCantidad) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBackground(new Color(250, 250, 250));
        panel.setOpaque(true);
        
        // Etiqueta más pequeña para dar más espacio a los números
        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setForeground(new Color(100, 100, 100));
        panel.add(lbl, BorderLayout.NORTH);
        
        // Campo de texto con números grandes
        JTextField campo = new JTextField(valorInicial);
        // Números más proporcionados - cantidad más grande que valor
        int tamanoFuente = esCantidad ? 140 : 100; // Números más pequeños para casillas panorámicas
        campo.setFont(new Font("Segoe UI", Font.BOLD, tamanoFuente));
        campo.setHorizontalAlignment(JTextField.CENTER);
        
        // Borde moderno con padding adecuado para números grandes
        campo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100), 3, true),
            BorderFactory.createEmptyBorder(20, 25, 20, 25) // Padding adecuado para números grandes
        ));
        
        // Fondo y color mejorados
        campo.setBackground(new Color(255, 255, 255));
        campo.setForeground(new Color(30, 30, 30));
        campo.setOpaque(true);
        
        // Casillas panorámicas: menos altas, más anchas
        int alturaCampo = esCantidad ? 160 : 130; // Altura reducida
        int anchoCampo = 550; // Muy anchas (estiradas horizontalmente)
        campo.setPreferredSize(new Dimension(anchoCampo, alturaCampo));
        campo.setMinimumSize(new Dimension(anchoCampo, alturaCampo));
        campo.setMaximumSize(new Dimension(anchoCampo, alturaCampo));
        
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
                    // Parsear correctamente usando el formato
                    Number num = PRECIO_FORMAT.parse(txtValor.getText());
                    double valor = num.doubleValue();
                    txtValor.setText(String.format("%.2f", valor));
                    txtValor.selectAll();
                } catch (Exception ex) {
                    txtValor.selectAll();
                }
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                // Al perder foco, formatear como precio
                try {
                    // Intentar parsear con punto decimal
                    String texto = txtValor.getText().replace("$", "").trim();
                    texto = texto.replace(",", "."); // Convertir coma a punto
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
                cantidad += 0.250; // Incrementar de 0.25 en 0.25 kilos
                if (cantidad < 0) cantidad = 0;
                actualizando = true; // Evitar actualización recursiva
                txtCantidad.setText(CANTIDAD_FORMAT.format(cantidad));
                actualizando = false;
                calcularDesdeCantidad();
            } catch (NumberFormatException ex) {
                actualizando = true;
                txtCantidad.setText("0.250");
                actualizando = false;
                calcularDesdeCantidad();
            }
        } else if (campoActivo == txtValor) {
            try {
                // Parsear correctamente el valor actual
                String texto = txtValor.getText();
                double valor = 0.0;
                try {
                    // Si tiene formato de precio, parsearlo
                    Number num = PRECIO_FORMAT.parse(texto);
                    valor = num.doubleValue();
                } catch (Exception e) {
                    // Si no tiene $, intentar parseo directo (punto decimal)
                    texto = texto.replace("$", "").replace(",", ".").trim();
                    valor = Double.parseDouble(texto);
                }
                
                valor += 50.0; // Incrementar de 50 en 50 pesos
                if (valor < 0) valor = 0;
                actualizando = true; // Evitar actualización recursiva
                // Mostrar sin formato mientras está activo
                txtValor.setText(String.format("%.2f", valor));
                actualizando = false;
                calcularDesdeValor();
            } catch (Exception ex) {
                actualizando = true;
                txtValor.setText("50.00");
                actualizando = false;
                calcularDesdeValor();
            }
        }
    }
    
    private void decrementarCampoActivo() {
        if (campoActivo == txtCantidad) {
            try {
                double cantidad = Double.parseDouble(txtCantidad.getText().trim());
                cantidad -= 0.250; // Decrementar de 0.25 en 0.25 kilos
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
                // Parsear correctamente el valor actual
                String texto = txtValor.getText();
                double valor = 0.0;
                try {
                    // Si tiene formato de precio, parsearlo
                    Number num = PRECIO_FORMAT.parse(texto);
                    valor = num.doubleValue();
                } catch (Exception e) {
                    // Si no tiene $, intentar parseo directo (punto decimal)
                    texto = texto.replace("$", "").replace(",", ".").trim();
                    valor = Double.parseDouble(texto);
                }
                
                valor -= 50.0; // Decrementar de 50 en 50 pesos
                if (valor < 0) valor = 0;
                actualizando = true; // Evitar actualización recursiva
                // Mostrar sin formato mientras está activo
                txtValor.setText(String.format("%.2f", valor));
                actualizando = false;
                calcularDesdeValor();
            } catch (Exception ex) {
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
                
                System.out.println("DEBUG CALCULAR DESDE CANTIDAD:");
                System.out.println("  Cantidad: " + cantidad);
                System.out.println("  Precio Unitario: " + precioUnitario);
                System.out.println("  Valor calculado: " + valor);
                
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
            
            String textoValor = txtValor.getText().replace("$", "").trim();
            if (textoValor.isEmpty() || textoValor.equals(".")) {
                if (!txtCantidad.hasFocus()) {
                    txtCantidad.setText("0.000");
                }
                btnAceptar.setEnabled(false);
            } else {
                // Parsear correctamente usando el NumberFormat para manejar separadores
                double valor = 0.0;
                try {
                    // Intentar parsear con el formato de precio (respeta coma decimal)
                    Number num = PRECIO_FORMAT.parse("$" + textoValor);
                    valor = num.doubleValue();
                } catch (ParseException e) {
                    // Si falla, intentar con punto decimal
                    try {
                        textoValor = textoValor.replace(",", ".");
                        valor = Double.parseDouble(textoValor);
                    } catch (NumberFormatException ex) {
                        valor = 0.0;
                    }
                }
                
                double cantidad = (precioUnitario > 0) ? (valor / precioUnitario) : 0.0;
                
                System.out.println("DEBUG CALCULAR DESDE VALOR:");
                System.out.println("  Texto original: " + txtValor.getText());
                System.out.println("  Texto parseado: " + textoValor);
                System.out.println("  Valor: " + valor);
                System.out.println("  Precio Unitario: " + precioUnitario);
                System.out.println("  Cantidad calculada: " + cantidad);
                
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
        // NO aplicar estilo moderno para evitar que sobrescriba las fuentes
        
        JGranelDialog dialog = new JGranelDialog(parent, nombreProducto, precioUnitario);
        
        // Verificar fuentes antes de mostrar
        System.out.println("DEBUG: Antes de mostrar - Fuente cantidad: " + 
            (dialog.txtCantidad != null ? dialog.txtCantidad.getFont().getSize() : "null") + " puntos");
        System.out.println("DEBUG: Antes de mostrar - Fuente valor: " + 
            (dialog.txtValor != null ? dialog.txtValor.getFont().getSize() : "null") + " puntos");
        
        // Asegurar que el diálogo sea visible
        dialog.setAlwaysOnTop(true);
        dialog.toFront();
        dialog.requestFocus();
        
        System.out.println("DEBUG: Mostrando diálogo granel...");
        dialog.setVisible(true);
        
        // Verificar fuentes después de mostrar
        System.out.println("DEBUG: Después de mostrar - Fuente cantidad: " + 
            (dialog.txtCantidad != null ? dialog.txtCantidad.getFont().getSize() : "null") + " puntos");
        System.out.println("DEBUG: Después de mostrar - Fuente valor: " + 
            (dialog.txtValor != null ? dialog.txtValor.getFont().getSize() : "null") + " puntos");
        
        System.out.println("DEBUG: Diálogo visible: " + dialog.isVisible());
        
        if (dialog.fueAceptado()) {
            return dialog.getCantidad();
        } else {
            return null;
        }
    }
}
