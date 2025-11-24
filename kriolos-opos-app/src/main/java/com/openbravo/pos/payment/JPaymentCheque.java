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
import com.openbravo.format.Formats;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.util.RoundUtils;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.BorderFactory;

/**
 *
 * @author JG uniCenta
 */
public class JPaymentCheque extends javax.swing.JPanel implements JPaymentInterface {
    
    private JPaymentNotifier m_notifier;

    private double m_dPaid;
    private double m_dTotal;
    
    /** Creates new form JPaymentCash
     * @param notifier */
    public JPaymentCheque(JPaymentNotifier notifier) {
        
        m_notifier = notifier;
        
        initComponents();  
        
        m_jTendered.addPropertyChangeListener("Edition", new RecalculateState());
        m_jTendered.addEditorKeys(m_jKeys);
        
        // Sebastian - Agregar listener para teclas de borrado (DEL, Backspace)
        m_jTendered.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                
                if (keyCode == KeyEvent.VK_BACK_SPACE || keyCode == KeyEvent.VK_DELETE) {
                    Double currentValue = m_jTendered.getValue();
                    
                    if (currentValue != null && currentValue > 0) {
                        String valueStr = String.valueOf(currentValue);
                        valueStr = valueStr.replace(".", "");
                        
                        if (valueStr.length() > 1) {
                            valueStr = valueStr.substring(0, valueStr.length() - 1);
                            
                            try {
                                double newValue = Double.parseDouble(valueStr) / 100.0;
                                m_jTendered.setDoubleValue(newValue);
                            } catch (NumberFormatException ex) {
                                m_jTendered.reset();
                            }
                        } else {
                            m_jTendered.reset();
                        }
                        
                        printState();
                        e.consume();
                    }
                }
            }
        });
        
        // Sebastian - Ajustes simples de UI (misma metodología que Cash)
        adjustUIComponents();
    }
    
    /**
     *
     * @param customerext
     * @param dTotal
     * @param transID
     */
    @Override
    public void activate(CustomerInfoExt customerext, double dTotal, String transID) {
        
        m_dTotal = dTotal;
        m_jTendered.reset();
        m_jTendered.activate();
        
        // Sebastian - Asegurar que el campo tenga el foco para recibir eventos de teclado
        m_jTendered.requestFocusInWindow();
        
        printState();
        
    }

    /**
     *
     * @return
     */
    @Override
    public PaymentInfo executePayment() {
        return new PaymentInfoTicket(m_dPaid, "cheque");      
    }

    /**
     *
     * @return
     */
    @Override
    public Component getComponent() {
        return this;
    }

    private void printState() {
        
        Double value = m_jTendered.getValue();
        if (value == null) {
            m_dPaid = m_dTotal;
        } else {
            m_dPaid = value;
        } 

        m_jMoneyEuros.setText(Formats.CURRENCY.formatValue(m_dPaid));
        
        int iCompare = RoundUtils.compare(m_dPaid, m_dTotal);
        
        // if iCompare > 0 then the payment is not valid
        m_notifier.setStatus(m_dPaid > 0.0 && iCompare <= 0, iCompare == 0);
        
        // Actualizar el campo "Restante" en el diálogo principal en tiempo real
        m_notifier.updateRemaining(m_dPaid);
    }
    
    private class RecalculateState implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            printState();
        }
    }     

    // Sebastian - Método para ajustar UI (misma metodología que Cash)
    private void adjustUIComponents() {
        // 1. Hacer el keypad súper pequeño: 20x20
        if (m_jKeys != null) {
            m_jKeys.setPreferredSize(new Dimension(20, 20));
            m_jKeys.setMinimumSize(new Dimension(20, 20));
            m_jKeys.setMaximumSize(new Dimension(20, 20));
        }
        
        // 2. Hacer el input crítico m_jTendered INVISIBLE pero funcional
        if (m_jTendered != null) {
            m_jTendered.setVisible(false);
            m_jTendered.setPreferredSize(new Dimension(1, 1));
            m_jTendered.setMinimumSize(new Dimension(1, 1));
            m_jTendered.setMaximumSize(new Dimension(1, 1));
            m_jTendered.setFocusable(true);
            m_jTendered.requestFocusInWindow();
        }
        
        // Ocultar el panel que contiene m_jTendered
        if (jPanel3 != null) {
            jPanel3.setVisible(false);
            jPanel3.setPreferredSize(new Dimension(0, 0));
        }
        
        // 3. Hacer m_jMoneyEuros más grande y visible
        if (m_jMoneyEuros != null) {
            m_jMoneyEuros.setPreferredSize(new Dimension(400, 60));
            m_jMoneyEuros.setMinimumSize(new Dimension(400, 60));
            m_jMoneyEuros.setMaximumSize(new Dimension(400, 60));
            
            Font currentFont = m_jMoneyEuros.getFont();
            m_jMoneyEuros.setFont(new Font(currentFont.getName(), Font.BOLD, 28));
            
            m_jMoneyEuros.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(34, 197, 94), 3),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));
            
            m_jMoneyEuros.setBounds(120, 10, 400, 60);
        }
        
        // Ajustar etiqueta
        if (jLabel8 != null) {
            jLabel8.setBounds(10, 10, 100, 60);
        }
        
        // Ajustar panel contenedor
        if (jPanel4 != null) {
            jPanel4.setPreferredSize(new Dimension(550, 80));
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        m_jKeys = new com.openbravo.editor.JEditorKeys();
        jPanel3 = new javax.swing.JPanel();
        m_jTendered = new com.openbravo.editor.JEditorCurrencyPositive();
        jPanel4 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        m_jMoneyEuros = new javax.swing.JLabel();

        setLayout(new java.awt.BorderLayout());

        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));
        jPanel1.add(m_jKeys);

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel3.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jPanel3.setLayout(new java.awt.BorderLayout());

        m_jTendered.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        jPanel3.add(m_jTendered, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel3);

        jPanel2.add(jPanel1, java.awt.BorderLayout.NORTH);

        add(jPanel2, java.awt.BorderLayout.EAST);

        jPanel4.setLayout(null);

        jLabel8.setFont(jLabel8.getFont());
        jLabel8.setText(AppLocal.getIntString("label.InputCash")); // NOI18N
        jLabel8.setPreferredSize(new java.awt.Dimension(100, 30));
        jPanel4.add(jLabel8);
        jLabel8.setBounds(10, 4, 100, 30);

        m_jMoneyEuros.setBackground(new java.awt.Color(204, 255, 51));
        m_jMoneyEuros.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        m_jMoneyEuros.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        m_jMoneyEuros.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jMoneyEuros.setOpaque(true);
        m_jMoneyEuros.setPreferredSize(new java.awt.Dimension(180, 30));
        jPanel4.add(m_jMoneyEuros);
        m_jMoneyEuros.setBounds(120, 4, 180, 30);

        add(jPanel4, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private com.openbravo.editor.JEditorKeys m_jKeys;
    private javax.swing.JLabel m_jMoneyEuros;
    private com.openbravo.editor.JEditorCurrencyPositive m_jTendered;
    // End of variables declaration//GEN-END:variables
    
}
