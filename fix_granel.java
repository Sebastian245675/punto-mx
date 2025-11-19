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
            
            @Override
            public void keyReleased(KeyEvent e) {
                // Flechas arriba/abajo solo para txtPeso en modo peso->precio
                if (e.getSource() == txtPeso && txtPeso.isEnabled() && modoPesoAPrecio) {
                    if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
                        try {
                            double valorActual = Double.parseDouble(txtPeso.getText().replace(",", ".").trim());
                            if (e.getKeyCode() == KeyEvent.VK_UP) {
                                valorActual += 1.0;
                            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                                valorActual -= 1.0;
                                if (valorActual < 0.0) valorActual = 0.0;
                            }
                            txtPeso.setText(String.format("%.3f", valorActual));
                            calcularValores();
                            e.consume();
                        } catch (NumberFormatException ex) {
                            // Si el campo está vacío o inválido
                            if (e.getKeyCode() == KeyEvent.VK_UP) {
                                txtPeso.setText("1.000");
                            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                                txtPeso.setText("0.000");
                            }
                            calcularValores();
                            e.consume();
                        }
                    }
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
