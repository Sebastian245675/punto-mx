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
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.resources.ImageResources;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.plaf.metal.MetalTabbedPaneUI;
import javax.swing.text.View;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author adrianromero
 */
public abstract class JPaymentSelect extends javax.swing.JDialog implements JPaymentNotifier {

    private static final long serialVersionUID = 1L;

    private PaymentInfoList m_aPaymentInfo;

    private boolean accepted;

    private AppView app;
    private double m_dTotal;
    private CustomerInfoExt customerext;
    private DataLogicSystem dlSystem;
    private DataLogicCustomers dlCustomers;
    private DataLogicSales dlSales;

    private final Map<String, JPaymentInterface> payments = new HashMap<>();
    private String m_sTransactionID;
    private static PaymentInfo returnPayment = null;

    public static PaymentInfo getReturnPayment() {
        return returnPayment;
    }

    public static void setReturnPayment(PaymentInfo returnPayment) {
        JPaymentSelect.returnPayment = returnPayment;
    }

    protected JPaymentSelect(java.awt.Frame parent, boolean modal, ComponentOrientation o) {
        super(parent, modal);
        initComponents();
        this.applyComponentOrientation(o);
        getRootPane().setDefaultButton(m_jButtonOK);

    }

    protected JPaymentSelect(java.awt.Dialog parent, boolean modal, ComponentOrientation o) {
        super(parent, modal);
        initComponents();
        this.applyComponentOrientation(o);
    }

    public void init(AppView app) {
        this.app = app;
        dlSystem = (DataLogicSystem) app.getBean("com.openbravo.pos.forms.DataLogicSystem");
        dlCustomers = (DataLogicCustomers) app.getBean("com.openbravo.pos.customers.DataLogicCustomers");
        dlSales = (DataLogicSales) app.getBean("com.openbravo.pos.forms.DataLogicSales");

        m_jButtonPrint.setVisible(true);
        setPrintSelected(!Boolean.parseBoolean(app.getProperties().getProperty("till.receiptprintoff")));
        setPrintSelectedLabel();
    }

    private void setPrintSelectedLabel() {
        if (m_jButtonPrint.isSelected()) {
            jlblPrinterStatus.setText(AppLocal.getIntString("jpaymentselect.printer.on", "Printer on"));
        } else {
            jlblPrinterStatus.setText(AppLocal.getIntString("jpaymentselect.printer.off", "Printer off"));
        }
    }

    public void setPrintSelected(boolean value) {
        m_jButtonPrint.setSelected(value);
    }

    public boolean isPrintSelected() {
        return m_jButtonPrint.isSelected();
    }

    /**
     * List of PaymentInfo
     * 
     * @return
     */
    public List<PaymentInfo> getSelectedPayments() {
        return m_aPaymentInfo.getPayments();
    }

    /**
     * Get PaymentInfoList
     * 
     * @return
     */
    public PaymentInfoList getPaymentInfoList() {
        return m_aPaymentInfo;
    }

    /**
     * Get total
     *
     * @return
     */
    public double getTotal() {
        return m_aPaymentInfo.getTotal();
    }

    /**
     * Get total Paid
     *
     * @return
     */
    public double getPaidTotal() {
        return m_aPaymentInfo.getPaidTotal();
    }

    public boolean showDialog(double total, CustomerInfoExt customerext, double deposit) {
        m_aPaymentInfo = new PaymentInfoList();
        accepted = false;
        total = total - deposit;
        m_dTotal = total;

        this.customerext = customerext;
        setPrintSelected(!Boolean.parseBoolean(app.getProperties().getProperty("till.receiptprintoff")));
        setPrintSelectedLabel();
        m_jTotalEuros.setText(Formats.CURRENCY.formatValue(m_dTotal));

        addTabs();

        // remove all tabs
        m_jTabPayment.removeAll();

        return accepted;
    }

    public boolean showDialog(double total, CustomerInfoExt customerext) {

        m_aPaymentInfo = new PaymentInfoList();
        accepted = false;

        m_dTotal = total;

        this.customerext = customerext;

        setPrintSelected(!Boolean.parseBoolean(app.getProperties().getProperty("till.receiptprintoff")));
        setPrintSelectedLabel();
        m_jTotalEuros.setText(Formats.CURRENCY.formatValue(m_dTotal));

        /**
         * m_jPayTotal.setText(Formats.CURRENCY.formatValue(m_dTotal));
         * N. Deppe 08/11/2018
         * Fix issue where dialog keeps moving lower and lower on the screen
         * Get the size of the screen, and center the dialog in the window
         */
        Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension thisDim = this.getSize();
        int x = (screenDim.width - thisDim.width) / 2;
        int y = (screenDim.height - thisDim.height) / 2;
        this.setLocation(x, y);

        addTabs();

        if (m_jTabPayment.getTabCount() == 0) {
            // No payment panels available
            m_aPaymentInfo.add(getDefaultPayment(total));
            accepted = true;
        } else {
            getRootPane().setDefaultButton(m_jButtonOK);
            printState();
            setVisible(true);
        }
        m_jTabPayment.removeAll();

        return accepted;
    }

    protected abstract void addTabs();

    protected abstract void setStatusPanel(boolean isPositive, boolean isComplete);

    protected abstract PaymentInfo getDefaultPayment(double total);

    protected void setOKEnabled(boolean value) {
        m_jButtonOK.setEnabled(value);
    }

    protected void setAddEnabled(boolean value) {
        m_jButtonAdd.setEnabled(value);
    }

    protected void addTabPayment(JPaymentCreator jpay) {
        if (app.hasPermission(jpay.getKey())) {

            JPaymentInterface jpayinterface = payments.get(jpay.getKey());
            if (jpayinterface == null) {
                jpayinterface = jpay.createJPayment();
                payments.put(jpay.getKey(), jpayinterface);
            }

            jpayinterface.getComponent().applyComponentOrientation(getComponentOrientation());

            String title = AppLocal.getIntString(jpay.getLabelKey());

            m_jTabPayment.addTab(
                    fixedStringRithPad(AppLocal.getIntString(jpay.getLabelKey())),
                    ImageResources.getIcon(jpay.getIconKey()),
                    jpayinterface.getComponent(),
                    title);
        }
    }

    private String fixedStringRithPad(final String text) {
        return fixedStringRithPad(text, 10);
    }

    private String fixedStringRithPad(String text, int length) {

        if (text.length() > length) {
            text = text.substring(0, length - 1);
        }
        return StringUtils.rightPad(text, length, "");
    }

    public interface JPaymentCreator {

        public JPaymentInterface createJPayment();

        public String getKey();

        public String getLabelKey();

        public String getIconKey();
    }

    public class JPaymentCashCreator implements JPaymentCreator {

        @Override
        public JPaymentInterface createJPayment() {
            return new JPaymentCashPos(JPaymentSelect.this, dlSystem);
        }

        @Override
        public String getKey() {
            return "payment.cash";
        }

        @Override
        public String getLabelKey() {
            return "tab.cash";
        }

        @Override
        public String getIconKey() {
            return "/com/openbravo/images/cash.png";
        }
    }

    public class JPaymentChequeCreator implements JPaymentCreator {

        @Override
        public JPaymentInterface createJPayment() {
            return new JPaymentCheque(JPaymentSelect.this);
        }

        @Override
        public String getKey() {
            return "payment.cheque";
        }

        @Override
        public String getLabelKey() {
            return "tab.cheque";
        }

        @Override
        public String getIconKey() {
            return "/com/openbravo/images/cheque.png";
        }
    }

    public class JPaymentVoucherCreator implements JPaymentCreator {

        @Override
        public JPaymentInterface createJPayment() {
            return new JPaymentVoucher(app, JPaymentSelect.this, "voucherin");
        }

        @Override
        public String getKey() {
            return "payment.voucher";
        }

        @Override
        public String getLabelKey() {
            return "tab.voucher";
        }

        @Override
        public String getIconKey() {
            return "/com/openbravo/images/voucher.png";
        }
    }

    public class JPaymentMagcardCreator implements JPaymentCreator {

        @Override
        public JPaymentInterface createJPayment() {
            return new JPaymentMagcard(app, JPaymentSelect.this);
        }

        @Override
        public String getKey() {
            return "payment.magcard";
        }

        @Override
        public String getLabelKey() {
            return "tab.magcard";
        }

        @Override
        public String getIconKey() {
            return "/com/openbravo/images/ccard.png";
        }
    }

    public class JPaymentFreeCreator implements JPaymentCreator {

        @Override
        public JPaymentInterface createJPayment() {
            return new JPaymentFree(JPaymentSelect.this);
        }

        @Override
        public String getKey() {
            return "payment.free";
        }

        @Override
        public String getLabelKey() {
            return "tab.free";
        }

        @Override
        public String getIconKey() {
            return "/com/openbravo/images/wallet.png";
        }
    }

    public class JPaymentDebtCreator implements JPaymentCreator {

        @Override
        public JPaymentInterface createJPayment() {
            return new JPaymentDebt(JPaymentSelect.this);
        }

        @Override
        public String getKey() {
            return "payment.debt";
        }

        @Override
        public String getLabelKey() {
            return "tab.debt";
        }

        @Override
        public String getIconKey() {
            return "/com/openbravo/images/customer.png";
        }
    }

    public class JPaymentCashRefundCreator implements JPaymentCreator {

        @Override
        public JPaymentInterface createJPayment() {
            return new JPaymentRefund(JPaymentSelect.this, "cashrefund");
        }

        @Override
        public String getKey() {
            return "refund.cash";
        }

        @Override
        public String getLabelKey() {
            return "tab.cashrefund";
        }

        @Override
        public String getIconKey() {
            return "/com/openbravo/images/cash.png";
        }
    }

    public class JPaymentChequeRefundCreator implements JPaymentCreator {

        @Override
        public JPaymentInterface createJPayment() {
            return new JPaymentRefund(JPaymentSelect.this, "chequerefund");
        }

        @Override
        public String getKey() {
            return "refund.cheque";
        }

        @Override
        public String getLabelKey() {
            return "tab.chequerefund";
        }

        @Override
        public String getIconKey() {
            return "/com/openbravo/images/cheque.png";
        }
    }

    public class JPaymentVoucherRefundCreator implements JPaymentCreator {

        @Override
        public JPaymentInterface createJPayment() {
            return new JPaymentRefund(JPaymentSelect.this, "voucherout");
        }

        @Override
        public String getKey() {
            return "refund.voucher";
        }

        @Override
        public String getLabelKey() {
            return "tab.voucher";
        }

        @Override
        public String getIconKey() {
            return "/com/openbravo/images/voucher.png";
        }
    }

    public class JPaymentMagcardRefundCreator implements JPaymentCreator {

        @Override
        public JPaymentInterface createJPayment() {
            return new JPaymentMagcard(app, JPaymentSelect.this);
        }

        @Override
        public String getKey() {
            return "refund.magcard";
        }

        @Override
        public String getLabelKey() {
            return "tab.magcard";
        }

        @Override
        public String getIconKey() {
            return "/com/openbravo/images/ccard.png";
        }
    }

    public class JPaymentBankCreator implements JPaymentCreator {

        @Override
        public JPaymentInterface createJPayment() {
            return new JPaymentBank(JPaymentSelect.this);
        }

        @Override
        public String getKey() {
            return "payment.bank";
        }

        @Override
        public String getLabelKey() {
            return "tab.bank";
        }

        @Override
        public String getIconKey() {
            return "/com/openbravo/images/bank.png";
        }
    }

    public class JPaymentSlipCreator implements JPaymentCreator {

        @Override
        public JPaymentInterface createJPayment() {
            return new JPaymentSlip(JPaymentSelect.this);
        }

        @Override
        public String getKey() {
            return "payment.slip";
        }

        @Override
        public String getLabelKey() {
            return "tab.slip";
        }

        @Override
        public String getIconKey() {
            return "/com/openbravo/images/slip.png";
        }
    }

    private void printState() {

        m_jRemaininglEuros.setText(Formats.CURRENCY.formatValue(
                m_dTotal - m_aPaymentInfo.getTotal()));
        m_jButtonRemove.setEnabled(!m_aPaymentInfo.isEmpty());
        m_jTabPayment.setSelectedIndex(0);
        ((JPaymentInterface) m_jTabPayment.getSelectedComponent())
                .activate(customerext,
                        m_dTotal - m_aPaymentInfo.getTotal(),
                        m_sTransactionID);
    }

    protected static Window getWindow(Component parent) {
        if (parent == null) {
            return new JFrame();
        } else if (parent instanceof Frame || parent instanceof Dialog) {
            return (Window) parent;
        } else {
            return getWindow(parent.getParent());
        }
    }

    @Override
    public void setStatus(boolean isPositive, boolean isComplete) {
        setStatusPanel(isPositive, isComplete);
    }

    @Override
    public void updateRemaining(double tendered) {
        // Calcular el restante basado en el monto introducido
        double remaining = m_dTotal - tendered;

        // Actualizar el campo de restante en tiempo real
        m_jRemaininglEuros.setText(Formats.CURRENCY.formatValue(remaining));

        // Sebastian - Colores estilo Eleventa: negro siempre, sin verde ni colores
        // extraños
        // Solo rojo si falta dinero, negro en cualquier otro caso (como Eleventa)
        if (remaining > 0.01) {
            // Falta dinero - color rojo
            m_jRemaininglEuros.setForeground(new java.awt.Color(220, 38, 38)); // Rojo
        } else {
            // Pago exacto, cambio o sobrepago - siempre negro (sin verde)
            m_jRemaininglEuros.setForeground(java.awt.Color.BLACK);
        }
    }

    public void setTransactionID(String tID) {
        this.m_sTransactionID = tID;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel4 = new javax.swing.JPanel();
        m_jLblTotalEuros1 = new javax.swing.JLabel();
        m_jTotalEuros = new javax.swing.JLabel();
        m_jLblRemainingEuros = new javax.swing.JLabel();
        m_jRemaininglEuros = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        m_jButtonRemove = new javax.swing.JButton();
        m_jButtonAdd = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        m_jTabPayment = new javax.swing.JTabbedPane();
        jPanel5 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        m_jButtonCancel = new javax.swing.JButton();
        m_jButtonOK = new javax.swing.JButton();
        m_jButtonPrint = new javax.swing.JToggleButton();
        jlblPrinterStatus = new javax.swing.JLabel();
        m_jButtonCobrarImprimir = new javax.swing.JButton();
        m_jButtonCobrarSinImprimir = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Venta de Productos: Cobrar"); // Sebastian - Título estilo Eleventa
        setMaximumSize(new java.awt.Dimension(1200, 800));
        setMinimumSize(new java.awt.Dimension(900, 650));
        setPreferredSize(new java.awt.Dimension(1100, 750));
        setResizable(true); // Sebastian - Permitir redimensionar para mejor adaptación

        jPanel4.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N - Tamaño aumentado

        // Sebastian - Etiqueta para el título del total
        m_jLblTotalEuros1.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24)); // NOI18N
        m_jLblTotalEuros1.setText("Cantidad a pagar:"); // Sebastian - Título solicitado
        m_jLblTotalEuros1.setVisible(true);
        m_jLblTotalEuros1.setPreferredSize(new java.awt.Dimension(220, 35));

        // Sebastian - Total en azul estilo Eleventa - MUY GRANDE
        m_jTotalEuros.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 72)); // NOI18N - Tamaño muy grande
        m_jTotalEuros.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jTotalEuros.setForeground(new java.awt.Color(52, 152, 219)); // Azul estilo Eleventa
        m_jTotalEuros.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 20, 15, 20));
        m_jTotalEuros.setOpaque(false);
        m_jTotalEuros.setPreferredSize(new java.awt.Dimension(350, 90));
        m_jTotalEuros.setRequestFocusEnabled(false);

        // Sebastian - "Restante" oculto por solicitud del usuario
        m_jLblRemainingEuros.setVisible(false);
        m_jLblRemainingEuros.setPreferredSize(new java.awt.Dimension(0, 0));

        m_jRemaininglEuros.setVisible(false);
        m_jRemaininglEuros.setPreferredSize(new java.awt.Dimension(0, 0));

        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 0));

        m_jButtonRemove.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20)); // Tamaño aumentado
        m_jButtonRemove
                .setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/btnminus.png"))); // NOI18N
        m_jButtonRemove.setToolTipText(AppLocal.getIntString("jpaymentselect.payment.delpartial")); // NOI18N
        m_jButtonRemove.setPreferredSize(new java.awt.Dimension(100, 60));
        m_jButtonRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jButtonRemoveActionPerformed(evt);
            }
        });

        m_jButtonAdd.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20)); // Tamaño aumentado
        m_jButtonAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/btnplus.png"))); // NOI18N
        m_jButtonAdd.setToolTipText(AppLocal.getIntString("jpaymentselect.payment.addpartial")); // NOI18N
        m_jButtonAdd.setPreferredSize(new java.awt.Dimension(100, 60));
        m_jButtonAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jButtonAddActionPerformed(evt);
            }
        });

        // Sebastian - Layout simplificado: solo "Cantidad a pagar" y botones de acción
        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 25, 20, 25));
        jPanel4Layout.setHorizontalGroup(
                jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(m_jLblTotalEuros1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(m_jTotalEuros, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(20, 20, 20)
                                .addComponent(m_jButtonAdd, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(m_jButtonRemove, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)));
        jPanel4Layout.setVerticalGroup(
                jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                        .addComponent(m_jLblTotalEuros1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(m_jTotalEuros, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(m_jButtonAdd, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(m_jButtonRemove, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        getContentPane().add(jPanel4, java.awt.BorderLayout.NORTH);

        jPanel3.setName(""); // NOI18N
        jPanel3.setLayout(new java.awt.BorderLayout());

        m_jTabPayment.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        m_jTabPayment.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        m_jTabPayment.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24)); // NOI18N - Tamaño muy aumentado
        m_jTabPayment.setRequestFocusEnabled(false);
        m_jTabPayment.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                m_jTabPaymentStateChanged(evt);
            }
        });
        m_jTabPayment.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                m_jTabPaymentKeyPressed(evt);
            }
        });
        jPanel3.add(m_jTabPayment, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel3, java.awt.BorderLayout.CENTER);

        // Sebastian - Panel RIGHT: botones estilo Eleventa con altura controlada
        // Wrapper de altura fija para que los botones no se estiren ni encojan
        javax.swing.JPanel jPanelBtnsWrapper = new javax.swing.JPanel();
        jPanelBtnsWrapper.setLayout(new java.awt.GridLayout(3, 1, 0, 6));
        jPanelBtnsWrapper.setBackground(new java.awt.Color(248, 248, 248));
        jPanelBtnsWrapper.setPreferredSize(new java.awt.Dimension(204, 150));
        jPanelBtnsWrapper.setMaximumSize(new java.awt.Dimension(204, 150));
        jPanelBtnsWrapper.setMinimumSize(new java.awt.Dimension(150, 150));

        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 8));
        jPanel5.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 0,
                new java.awt.Color(200, 200, 200)));
        jPanel5.setBackground(new java.awt.Color(248, 248, 248));
        jPanel5.setPreferredSize(new java.awt.Dimension(220, 0));

        // Boton F1 - Cobrar e Imprimir
        m_jButtonCobrarImprimir.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 13));
        m_jButtonCobrarImprimir
                .setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/printer24.png")));
        m_jButtonCobrarImprimir.setText(" F1 - Cobrar e Imprimir");
        m_jButtonCobrarImprimir.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jButtonCobrarImprimir.setFocusPainted(false);
        m_jButtonCobrarImprimir.setFocusable(false);
        m_jButtonCobrarImprimir.setBackground(java.awt.Color.WHITE);
        m_jButtonCobrarImprimir.setForeground(java.awt.Color.BLACK);
        m_jButtonCobrarImprimir.setOpaque(true);
        m_jButtonCobrarImprimir.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180), 1),
                javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        m_jButtonCobrarImprimir.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        m_jButtonCobrarImprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jButtonPrint.setSelected(true);
                m_jButtonOKActionPerformed(evt);
            }
        });
        jPanelBtnsWrapper.add(m_jButtonCobrarImprimir);

        // Boton F2 - Cobrar sin imprimir
        m_jButtonCobrarSinImprimir.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 13));
        m_jButtonCobrarSinImprimir
                .setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/cash.png")));
        m_jButtonCobrarSinImprimir.setText(" F2 - Cobrar sin imprimir");
        m_jButtonCobrarSinImprimir.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jButtonCobrarSinImprimir.setFocusPainted(false);
        m_jButtonCobrarSinImprimir.setFocusable(false);
        m_jButtonCobrarSinImprimir.setBackground(java.awt.Color.WHITE);
        m_jButtonCobrarSinImprimir.setForeground(java.awt.Color.BLACK);
        m_jButtonCobrarSinImprimir.setOpaque(true);
        m_jButtonCobrarSinImprimir.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180), 1),
                javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        m_jButtonCobrarSinImprimir.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        m_jButtonCobrarSinImprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jButtonPrint.setSelected(false);
                m_jButtonOKActionPerformed(evt);
            }
        });
        jPanelBtnsWrapper.add(m_jButtonCobrarSinImprimir);

        // Boton ESC - Cancelar
        m_jButtonCancel.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 13));
        m_jButtonCancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/cancel.png")));
        m_jButtonCancel.setText(" ESC - Cancelar");
        m_jButtonCancel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jButtonCancel.setToolTipText("Cancelar (ESC)");
        m_jButtonCancel.setFocusPainted(false);
        m_jButtonCancel.setFocusable(false);
        m_jButtonCancel.setBackground(java.awt.Color.WHITE);
        m_jButtonCancel.setForeground(java.awt.Color.BLACK);
        m_jButtonCancel.setOpaque(true);
        m_jButtonCancel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180), 1),
                javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        m_jButtonCancel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        m_jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jButtonCancelActionPerformed(evt);
            }
        });
        jPanelBtnsWrapper.add(m_jButtonCancel);

        jPanel5.add(jPanelBtnsWrapper);

        // Mantener m_jButtonOK oculto (se usa internamente)
        m_jButtonOK.setVisible(false);
        m_jButtonPrint.setVisible(false);
        jlblPrinterStatus.setVisible(false);

        getContentPane().add(jPanel5, java.awt.BorderLayout.EAST);

        // Registrar teclas F1, F2, ESC a nivel de ventana
        // Desactivar F1->Help que Java asigna por defecto para evitar conflicto
        // F1 = Cobrar e Imprimir (como en el programa anterior)
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0), "cobrarImprimir");
        getRootPane().getActionMap().put("cobrarImprimir", new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (m_jButtonCobrarImprimir.isEnabled())
                    m_jButtonCobrarImprimir.doClick();
            }
        });
        // F2 = Cobrar sin imprimir (como en el programa anterior)
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0), "cobrarSinImprimir");
        getRootPane().getActionMap().put("cobrarSinImprimir", new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (m_jButtonCobrarSinImprimir.isEnabled())
                    m_jButtonCobrarSinImprimir.doClick();
            }
        });
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "cancelar");
        getRootPane().getActionMap().put("cancelar", new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                m_jButtonCancel.doClick();
            }
        });
        // Anular la acción de ayuda de F1 heredada por defecto en Java/Swing
        javax.swing.Action noHelp = new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                /* nada */ }
        };
        getRootPane().getActionMap().put("postHelp", noHelp);

        setSize(new java.awt.Dimension(1100, 750));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void m_jButtonRemoveActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jButtonRemoveActionPerformed

        m_aPaymentInfo.removeLast();
        printState();

    }// GEN-LAST:event_m_jButtonRemoveActionPerformed

    private void m_jButtonAddActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jButtonAddActionPerformed

        PaymentInfo returnPayment = ((JPaymentInterface) m_jTabPayment.getSelectedComponent())
                .executePayment();
        if (returnPayment != null) {
            m_aPaymentInfo.add(returnPayment);
            printState();

            // Sebastian - Mostrar mensaje si el pago es parcial
            double remaining = m_dTotal - m_aPaymentInfo.getTotal();
            if (remaining > 0.01) { // Tolerancia para errores de redondeo
                String message = String.format(
                        "<html><div style='text-align: center; font-size: 14px;'>" +
                                "<b>Pago Parcial Registrado</b><br><br>" +
                                "Falta por pagar: <b style='color: #DC2626; font-size: 18px;'>%s</b>" +
                                "</div></html>",
                        Formats.CURRENCY.formatValue(remaining));

                JOptionPane.showMessageDialog(
                        this,
                        message,
                        "Pago Parcial",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }

    }// GEN-LAST:event_m_jButtonAddActionPerformed

    private void m_jTabPaymentStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_m_jTabPaymentStateChanged

        if (m_jTabPayment.getSelectedComponent() != null) {
            ((JPaymentInterface) m_jTabPayment.getSelectedComponent())
                    .activate(customerext,
                            m_dTotal - m_aPaymentInfo.getTotal(),
                            m_sTransactionID);
            m_jRemaininglEuros.setText(
                    Formats.CURRENCY.formatValue(
                            m_dTotal - m_aPaymentInfo.getTotal()));
        }

    }// GEN-LAST:event_m_jTabPaymentStateChanged

    private void m_jButtonOKActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jButtonOKActionPerformed

        SwingWorker<Object, Object> worker = new SwingWorker<>() {
            @Override
            protected Object doInBackground() throws Exception {
                setReturnPayment(
                        ((JPaymentInterface) m_jTabPayment.getSelectedComponent())
                                .executePayment());
                return null;
            }

            @Override
            public void done() {
                m_jButtonOK.setEnabled(true);
                m_jButtonCancel.setEnabled(true);
                if (returnPayment != null) {
                    m_aPaymentInfo.add(returnPayment);

                    // Sebastian - Verificar si el pago está completo antes de cerrar
                    double remaining = m_dTotal - m_aPaymentInfo.getTotal();
                    if (remaining > 0.01) { // Pago incompleto
                        String message = String.format(
                                "<html><div style='text-align: center; font-size: 14px;'>" +
                                        "<b>⚠️ Pago Incompleto</b><br><br>" +
                                        "Aún falta por pagar: <b style='color: #DC2626; font-size: 18px;'>%s</b><br><br>"
                                        +
                                        "Por favor, agregue otro método de pago." +
                                        "</div></html>",
                                Formats.CURRENCY.formatValue(remaining));

                        JOptionPane.showMessageDialog(
                                JPaymentSelect.this,
                                message,
                                "Pago Incompleto",
                                JOptionPane.WARNING_MESSAGE);

                        printState(); // Actualizar la vista
                    } else {
                        // Pago completo o con sobrepago
                        accepted = true;
                        dispose();
                    }
                }
            }
        };

        worker.execute();
    }// GEN-LAST:event_m_jButtonOKActionPerformed

    private void m_jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jButtonCancelActionPerformed

        dispose();

    }// GEN-LAST:event_m_jButtonCancelActionPerformed

    private void m_jButtonPrintActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jButtonPrintActionPerformed
        if (!m_jButtonPrint.isSelected()) {
            jlblPrinterStatus.setText("Printer OFF");
        } else {
            jlblPrinterStatus.setText("Printer ON");
        }
    }// GEN-LAST:event_m_jButtonPrintActionPerformed

    private void m_jTabPaymentKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_m_jTabPaymentKeyPressed
        // Backup handler: F1=Cobrar e Imprimir, F2=Cobrar sin imprimir
        if (evt.getKeyCode() == KeyEvent.VK_F1) {
            evt.consume();
            if (m_jButtonCobrarImprimir.isEnabled())
                m_jButtonCobrarImprimir.doClick();
        } else if (evt.getKeyCode() == KeyEvent.VK_F2) {
            evt.consume();
            if (m_jButtonCobrarSinImprimir.isEnabled())
                m_jButtonCobrarSinImprimir.doClick();
        } else if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
            evt.consume();
            m_jButtonCancel.doClick();
        }
    }// GEN-LAST:event_m_jTabPaymentKeyPressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JLabel jlblPrinterStatus;
    private javax.swing.JButton m_jButtonAdd;
    private javax.swing.JButton m_jButtonCancel;
    private javax.swing.JButton m_jButtonOK;
    private javax.swing.JToggleButton m_jButtonPrint;
    private javax.swing.JButton m_jButtonRemove;
    private javax.swing.JLabel m_jLblRemainingEuros;
    private javax.swing.JLabel m_jLblTotalEuros1;
    private javax.swing.JLabel m_jRemaininglEuros;
    private javax.swing.JTabbedPane m_jTabPayment;
    private javax.swing.JLabel m_jTotalEuros;
    private javax.swing.JButton m_jButtonCobrarImprimir;
    private javax.swing.JButton m_jButtonCobrarSinImprimir;
    // End of variables declaration//GEN-END:variables
}

// tabbedPane.setUI(new MyTabbedPaneUI());
class MyTabbedPaneUI extends MetalTabbedPaneUI {

    private final JTabbedPane tabPane;

    public MyTabbedPaneUI(JTabbedPane tabbedPane) {
        this.tabPane = tabbedPane;
    }

    @Override
    protected void layoutLabel(int tabPlacement,
            FontMetrics metrics, int tabIndex,
            String title, Icon icon,
            Rectangle tabRect, Rectangle iconRect,
            Rectangle textRect, boolean isSelected) {

        textRect.x = textRect.y = iconRect.x = iconRect.y = 0;
        View v = getTextViewForTab(tabIndex);
        if (v != null) {
            tabPane.putClientProperty("html", v);
        }
        SwingUtilities.layoutCompoundLabel((JComponent) tabPane,
                metrics, title, icon,
                SwingUtilities.CENTER,
                SwingUtilities.LEFT, // CENTER, &lt;----
                SwingUtilities.CENTER,
                SwingUtilities.TRAILING,
                tabRect,
                iconRect,
                textRect,
                textIconGap);
        tabPane.putClientProperty("html", null);
        textRect.translate(tabInsets.left, 0); // &lt;----
        // textRect.width -= tabInsets.left+tabInsets.right;

        int xNudge = getTabLabelShiftX(tabPlacement, tabIndex, isSelected);
        int yNudge = getTabLabelShiftY(tabPlacement, tabIndex, isSelected);
        iconRect.x += xNudge;
        iconRect.y += yNudge;
        textRect.x += xNudge;
        textRect.y += yNudge;
    }
}
