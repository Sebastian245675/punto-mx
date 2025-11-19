        // Eventos de teclado
        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Flechas arriba/abajo: incrementar/decrementar peso en 1.0 kg
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
                            txtPeso.setText(String.format("%.3f", valorActual));
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
