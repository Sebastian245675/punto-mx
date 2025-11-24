//    KriolOS POS
//    Copyright (c) 2019-2023 KriolOS
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package com.openbravo.pos.payment;

// import com.openbravo.data.gui.MessageInf; // No usado - botones predefinidos deshabilitados
import com.openbravo.format.Formats;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSystem;
// import com.openbravo.pos.scripting.ScriptEngine; // No usado - botones predefinidos deshabilitados
// import com.openbravo.pos.scripting.ScriptException; // No usado - botones predefinidos deshabilitados
// import com.openbravo.pos.scripting.ScriptFactory; // No usado - botones predefinidos deshabilitados
import com.openbravo.pos.util.RoundUtils;
import com.openbravo.pos.util.ThumbNailBuilder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingConstants;

/**
 *
 * @author adrianromero
 */
public class JPaymentCashPos extends javax.swing.JPanel implements JPaymentInterface {

    private static final long serialVersionUID = 1L;

    private final JPaymentNotifier m_notifier;
    private double m_dPaid;
    private double m_dTotal;
    private final Boolean priceWith00;

    /**
     * Creates new form JPaymentCash
     *
     * @param notifier
     * @param dlSystem
     */
    public JPaymentCashPos(JPaymentNotifier notifier, DataLogicSystem dlSystem) {

        m_notifier = notifier;

        initComponents();

        m_jTendered.addPropertyChangeListener("Edition", new RecalculateState());
        m_jTendered.addEditorKeys(m_jKeys);
        
        // Sebastian - Agregar listener para teclas de borrado (DEL, Backspace)
        m_jTendered.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                
                // Detectar teclas de borrado: Backspace o Delete
                if (keyCode == KeyEvent.VK_BACK_SPACE || keyCode == KeyEvent.VK_DELETE) {
                    Double currentValue = m_jTendered.getValue();
                    
                    if (currentValue != null && currentValue > 0) {
                        // Convertir a String para poder eliminar el último dígito
                        String valueStr = String.valueOf(currentValue);
                        
                        // Eliminar el punto decimal si existe
                        valueStr = valueStr.replace(".", "");
                        
                        if (valueStr.length() > 1) {
                            // Eliminar el último dígito
                            valueStr = valueStr.substring(0, valueStr.length() - 1);
                            
                            try {
                                // Convertir de vuelta a double dividiendo por 100 (para mantener decimales)
                                double newValue = Double.parseDouble(valueStr) / 100.0;
                                m_jTendered.setDoubleValue(newValue);
                            } catch (NumberFormatException ex) {
                                // Si hay error, resetear a 0
                                m_jTendered.reset();
                            }
                        } else {
                            // Si solo queda un dígito, resetear a 0
                            m_jTendered.reset();
                        }
                        
                        printState();
                        e.consume(); // Consumir el evento para que no se propague
                    }
                }
            }
        });

        AppConfig m_config = AppConfig.getInstance();
        m_config.load();
        priceWith00 = ("true".equals(m_config.getProperty("till.pricewith00")));
        if (priceWith00) {
            // use '00' instead of '.'
            m_jKeys.dotIs00(true);
        }
//        m_config=null;

        // Sebastian - Deshabilitado: Botones de montos predefinidos no necesarios
        /*
        String code = dlSystem.getResourceAsXML("payment.cash");
        if (code != null) {
            try {
                ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.BEANSHELL);
                script.put("payment", new ScriptPaymentCash(dlSystem));
                script.eval(code);
            } catch (ScriptException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.cannotexecute"), e);
                msg.show(this);
            }
        }
        */
        
        // Sebastian - Ajustes simples de UI
        adjustUIComponents();

        // Agregar listener para teclas de flecha
        addArrowKeyListener();
    }

    @Override
    public void activate(CustomerInfoExt customerext, double dTotal, String transID) {
        m_dTotal = dTotal;
        m_jTendered.reset();
        m_jTendered.activate();
        
        // Sebastian - Asegurar que el campo tenga el foco para recibir eventos de teclado
        m_jTendered.requestFocusInWindow();

        printState();
    }

    @Override
    public PaymentInfo executePayment() {
        if (m_dPaid - m_dTotal >= 0.0) {
            // pago completo
            return new PaymentInfoCash(m_dTotal, m_dPaid);
        } else {
            // pago parcial
            return new PaymentInfoCash(m_dPaid, m_dPaid);
        }
    }

    @Override
    public Component getComponent() {
        return this;
    }

    private void printState() {

        Double value = m_jTendered.getValue();
        if (value == null || value == 0.0) {
            m_dPaid = m_dTotal;
        } else {
            m_dPaid = value;
        }

        int iCompare = RoundUtils.compare(m_dPaid, m_dTotal);

        m_jMoneyEuros.setText(Formats.CURRENCY.formatValue(m_dPaid));
        m_jChangeEuros.setText(iCompare > 0
                ? Formats.CURRENCY.formatValue(m_dPaid - m_dTotal)
                : null);

        m_notifier.setStatus(m_dPaid > 0.0, iCompare >= 0);
        
        // Actualizar el campo "Restante" en el diálogo principal en tiempo real
        m_notifier.updateRemaining(m_dPaid);
    }

    private class RecalculateState implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            printState();
        }
    }

    public class ScriptPaymentCash {

        private final DataLogicSystem dlSystem;
        private final ThumbNailBuilder tnbbutton;
        private final AppConfig m_config;

        public ScriptPaymentCash(DataLogicSystem dlSystem) {
            
            this.m_config = AppConfig.getInstance();
            this.dlSystem = dlSystem;
            tnbbutton = new ThumbNailBuilder(64, 48, "com/openbravo/images/cash.png");
        }

        public void addButton(String image, double amount) {
            JButton btn = new JButton();
            try {
                if ((m_config.getProperty("payments.textoverlay")).equals("false")) {
                    btn.setIcon(new ImageIcon(tnbbutton.getThumbNail(dlSystem.getResourceAsImage(image))));
                } else {
                    btn.setIcon(new ImageIcon(tnbbutton.getThumbNail(dlSystem.getResourceAsImage(image), Formats.CURRENCY.formatValue(amount))));
                }
            } catch (Exception e) {
                btn.setIcon(new ImageIcon(tnbbutton.getThumbNail(dlSystem.getResourceAsImage(image), Formats.CURRENCY.formatValue(amount))));
            }

            btn.setFocusPainted(false);
            btn.setFocusable(false);
            btn.setRequestFocusEnabled(false);
            btn.setHorizontalTextPosition(SwingConstants.CENTER);
            btn.setVerticalTextPosition(SwingConstants.BOTTOM);
            btn.setMargin(new Insets(2, 2, 2, 2));
            btn.addActionListener(new AddAmount(amount));
            jPanel6.add(btn);
        }
    }

    private class AddAmount implements ActionListener {

        private final double amount;

        public AddAmount(double amount) {
            this.amount = amount;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Double tendered = m_jTendered.getValue();

            if (tendered == null) {
                m_jTendered.setDoubleValue(amount);
            } else {
                m_jTendered.setDoubleValue(tendered + amount);

            }

            printState();
        }
    }

    // Sebastian - Método simple para ajustar UI sin complicaciones
    private void adjustUIComponents() {
        // 1. Hacer el keypad súper pequeño y ponerlo en esquina: 20x20
        if (m_jKeys != null) {
            m_jKeys.setPreferredSize(new Dimension(20, 20));
            m_jKeys.setMinimumSize(new Dimension(20, 20));
            m_jKeys.setMaximumSize(new Dimension(20, 20));
        }
        
        // 2. Hacer el input crítico m_jTendered visible pero muy pequeño para que reciba foco
        if (m_jTendered != null) {
            m_jTendered.setVisible(true);
            m_jTendered.setPreferredSize(new Dimension(10, 10));
            m_jTendered.setMinimumSize(new Dimension(10, 10));
            m_jTendered.setMaximumSize(new Dimension(10, 10));

            // IMPORTANTE: Mantener el campo focusable para que pueda recibir eventos de teclado
            m_jTendered.setFocusable(true);
            m_jTendered.requestFocusInWindow(); // Solicitar el foco al iniciar
        }

        // Mantener visible el panel que contiene m_jTendered para que reciba foco
        if (jPanel3 != null) {
            jPanel3.setVisible(true);
            jPanel3.setPreferredSize(new Dimension(10, 10));
        }
        
        // 3. Hacer que ambos campos de visualización sean más grandes
        Dimension fieldSize = new Dimension(400, 60);
        
        if (m_jMoneyEuros != null) {
            m_jMoneyEuros.setPreferredSize(fieldSize);
            m_jMoneyEuros.setMinimumSize(fieldSize);
            m_jMoneyEuros.setMaximumSize(fieldSize);
            
            // Fuente más grande y negrita
            Font currentFont = m_jMoneyEuros.getFont();
            m_jMoneyEuros.setFont(new Font(currentFont.getName(), Font.BOLD, 28));
            
            // Borde verde más visible
            m_jMoneyEuros.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(34, 197, 94), 3),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));
        }
        
        if (m_jChangeEuros != null) {
            m_jChangeEuros.setPreferredSize(fieldSize);
            m_jChangeEuros.setMinimumSize(fieldSize);
            m_jChangeEuros.setMaximumSize(fieldSize);
            
            // Fuente más grande y negrita
            Font currentFont = m_jChangeEuros.getFont();
            m_jChangeEuros.setFont(new Font(currentFont.getName(), Font.BOLD, 28));
            
            // Borde amarillo más visible
            m_jChangeEuros.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(234, 179, 8), 3),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));
        }
        
        // Ajustar el panel contenedor y reposicionar los componentes
        if (jPanel4 != null) {
            // Hacer el panel más alto para acomodar los campos más grandes
            jPanel4.setPreferredSize(new Dimension(500, 140));
            
            // Reposicionar los componentes con las posiciones absolutas correctas
            if (m_jMoneyEuros != null) {
                m_jMoneyEuros.setBounds(120, 10, 400, 60);
            }
            
            if (m_jChangeEuros != null) {
                m_jChangeEuros.setBounds(120, 75, 400, 60);
            }
            
            // Reposicionar las etiquetas también
            if (jLabel8 != null) { // "InputCash"
                jLabel8.setBounds(10, 10, 100, 60);
            }
            
            if (jLabel6 != null) { // "ChangeCash"
                jLabel6.setBounds(10, 75, 100, 60);
            }
        }
    }

    private void addArrowKeyListener() {
        if (m_jTendered == null) {
            return;
        }

        KeyAdapter adapter = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();
                Double currentValue = m_jTendered.getValue();

                if (keyCode == KeyEvent.VK_UP) {
                    double newValue = (currentValue == null ? 0.0 : currentValue) + 1.0;
                    m_jTendered.setDoubleValue(newValue);
                    printState();
                    e.consume();
                } else if (keyCode == KeyEvent.VK_DOWN) {
                    double newValue = (currentValue == null ? 0.0 : currentValue) - 1.0;
                    if (newValue < 0.0) {
                        newValue = 0.0;
                    }
                    m_jTendered.setDoubleValue(newValue);
                    printState();
                    e.consume();
                }
            }
        };

        m_jTendered.addKeyListener(adapter);

        // Registrar también en componentes hijos
        for (Component child : m_jTendered.getComponents()) {
            try {
                child.addKeyListener(adapter);
            } catch (Exception ex) {
                // Ignorar excepciones silenciosamente
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel5 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        m_jChangeEuros = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        m_jMoneyEuros = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        m_jKeys = new com.openbravo.editor.JEditorKeys();
        jPanel3 = new javax.swing.JPanel();
        m_jTendered = new com.openbravo.editor.JEditorCurrencyPositive();

        setPreferredSize(new java.awt.Dimension(700, 400));
        setLayout(new java.awt.BorderLayout());

        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel4.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jPanel4.setPreferredSize(new java.awt.Dimension(450, 70));
        jPanel4.setLayout(null);

        m_jChangeEuros.setBackground(new java.awt.Color(255, 255, 255));
        m_jChangeEuros.setFont(m_jChangeEuros.getFont().deriveFont(m_jChangeEuros.getFont().getSize()+5f));
        m_jChangeEuros.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        m_jChangeEuros.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jChangeEuros.setOpaque(true);
        m_jChangeEuros.setPreferredSize(new java.awt.Dimension(180, 30));
        jPanel4.add(m_jChangeEuros);
        m_jChangeEuros.setBounds(120, 36, 180, 30);

        jLabel6.setFont(jLabel6.getFont());
        jLabel6.setText(AppLocal.getIntString("label.ChangeCash")); // NOI18N
        jLabel6.setPreferredSize(new java.awt.Dimension(100, 30));
        jPanel4.add(jLabel6);
        jLabel6.setBounds(10, 36, 100, 30);

        jLabel8.setFont(jLabel8.getFont());
        jLabel8.setText(AppLocal.getIntString("label.InputCash")); // NOI18N
        jLabel8.setPreferredSize(new java.awt.Dimension(100, 30));
        jPanel4.add(jLabel8);
        jLabel8.setBounds(10, 4, 100, 30);

        m_jMoneyEuros.setBackground(new java.awt.Color(204, 255, 51));
        m_jMoneyEuros.setFont(m_jMoneyEuros.getFont().deriveFont(m_jMoneyEuros.getFont().getSize()+5f));
        m_jMoneyEuros.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        m_jMoneyEuros.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jMoneyEuros.setOpaque(true);
        m_jMoneyEuros.setPreferredSize(new java.awt.Dimension(180, 30));
        jPanel4.add(m_jMoneyEuros);
        m_jMoneyEuros.setBounds(120, 4, 180, 30);

        jPanel5.add(jPanel4, java.awt.BorderLayout.NORTH);

        jPanel6.setPreferredSize(new java.awt.Dimension(450, 10));
        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        jPanel6.setVisible(false); // Sebastian - Ocultar botones de montos predefinidos
        jPanel5.add(jPanel6, java.awt.BorderLayout.CENTER);

        add(jPanel5, java.awt.BorderLayout.CENTER);

        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));
        jPanel1.add(m_jKeys);

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel3.setLayout(new java.awt.BorderLayout());

        m_jTendered.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 28));
        m_jTendered.setMaximumSize(new java.awt.Dimension(180, 45));
        m_jTendered.setMinimumSize(new java.awt.Dimension(180, 45));
        m_jTendered.setPreferredSize(new java.awt.Dimension(180, 45));
        jPanel3.add(m_jTendered, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel3);

        jPanel2.add(jPanel1, java.awt.BorderLayout.NORTH);

        add(jPanel2, java.awt.BorderLayout.LINE_END);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JLabel m_jChangeEuros;
    private com.openbravo.editor.JEditorKeys m_jKeys;
    private javax.swing.JLabel m_jMoneyEuros;
    private com.openbravo.editor.JEditorCurrencyPositive m_jTendered;
    // End of variables declaration//GEN-END:variables

}
