import re

# Leer el archivo
with open(r'c:\Users\Daniel\Desktop\pos\kriolos-opos-app\src\main\java\com\openbravo\pos\sales\JGranelDialog.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Código a buscar (la sección con else if para las flechas en keyPressed)
old_section = '''        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    aceptar();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelar();
                } else if (e.getKeyCode() == KeyEvent.VK_F2) {
                    btnModoCalculo.doClick();
                } else if (e.getSource() == txtPeso && txtPeso.isEnabled() && modoPesoAPrecio) {
                    // Solo si el campo está habilitado y en modo peso->precio
                    try {
                        double valorActual = Double.parseDouble(txtPeso.getText().replace(",", ".").trim());
                        if (e.getKeyCode() == KeyEvent.VK_UP) {
                            valorActual += 1.0;
                            txtPeso.setText(String.format("%.3f", valorActual));
                            calcularValores();
                            e.consume();
                        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                            valorActual -= 1.0;
                            if (valorActual < 0.0) valorActual = 0.0;
                            txtPeso.setText(String.format("%.3f", valorActual));
                            calcularValores();
                            e.consume();
                        }
                    } catch (NumberFormatException ex) {
                        // Si el campo está vacío o inválido, poner 1.000 o 0.000
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
        };'''

new_section = '''        KeyListener keyListener = new KeyAdapter() {
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
        };'''

# Reemplazar
if old_section in content:
    content = content.replace(old_section, new_section)
    with open(r'c:\Users\Daniel\Desktop\pos\kriolos-opos-app\src\main\java\com\openbravo\pos\sales\JGranelDialog.java', 'w', encoding='utf-8') as f:
        f.write(content)
    print("✓ Archivo actualizado correctamente")
else:
    print("✗ No se encontró la sección a reemplazar")
