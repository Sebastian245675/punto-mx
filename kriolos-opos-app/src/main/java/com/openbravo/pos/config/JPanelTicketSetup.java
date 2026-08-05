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
package com.openbravo.pos.config;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Session;
import com.openbravo.data.user.DirtyManager;
import java.awt.Component;
import javax.swing.SpinnerNumberModel;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.sql.SQLException;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.util.AltEncrypter;
import javax.swing.JOptionPane;

/**
 *
 * @author JG uniCenta
 */
public class JPanelTicketSetup extends javax.swing.JPanel implements PanelConfig {

    private static final Logger LOGGER = Logger.getLogger(JPanelTicketSetup.class.getName());
    private static final long serialVersionUID = 1L;
    private final DirtyManager dirty = new DirtyManager();
    private String receipt = "1";
    private Integer x = 0;
    private String receiptSize;
    private String pickupSize;
    private AppConfig config;

    /**
     *
     * @param oApp
     */
    public JPanelTicketSetup() {

        initComponents();
        jComboFontName.setEditable(true);

        jReceiptSize.addChangeListener(dirty);
        jPickupSize.addChangeListener(dirty);
        jTextReceiptPrefix.getDocument().addDocumentListener(dirty);
        m_jReceiptPrintOff.addActionListener(dirty);

        jSpinnerWidth.addChangeListener(dirty);
        jSpinnerHeight.addChangeListener(dirty);
        jSpinnerX.addChangeListener(dirty);
        jSpinnerY.addChangeListener(dirty);
        jComboFontName.addActionListener(dirty);
        jSpinnerFontSize.addChangeListener(dirty);
        jSpinnerColumns.addChangeListener(dirty);
        jCheckBold.addActionListener(dirty);
        jCheckNormalTotals.addActionListener(dirty);

        jbtnReset.setVisible(true);
    }

    /**
     *
     * @return
     */
    @Override
    public boolean hasChanged() {
        return dirty.isDirty();
    }

    /**
     *
     * @return
     */
    @Override
    public Component getConfigComponent() {
        return this;
    }

    /**
     *
     * @param config
     */
    @Override
    public void loadProperties(AppConfig config) {

        this.config = config;
        int recSize;
        receiptSize = (config.getProperty("till.receiptsize"));
        try {
            recSize = Integer.parseInt(receiptSize);
        } catch (NumberFormatException ex) {
            recSize = 1;
        }

        jReceiptSize.setModel(new SpinnerNumberModel(recSize, 0, 20, 1));

        int picSize;
        pickupSize = (config.getProperty("till.pickupsize"));
        try {
            picSize = Integer.parseInt(pickupSize);
        } catch (NumberFormatException ex) {
            picSize = 1;
        }

        jPickupSize.setModel(new SpinnerNumberModel(picSize, 0, 20, 1));

        jTextReceiptPrefix.setText(config.getProperty("till.receiptprefix"));

        m_jReceiptPrintOff.setSelected(Boolean.parseBoolean(config.getProperty("till.receiptprintoff")));
        
        receiptPrefixExample();

        String widthProp = config.getProperty("paper.receipt.width");
        try {
            jSpinnerWidth.setValue(Integer.parseInt(widthProp != null ? widthProp : "262"));
        } catch (Exception ex) {
            jSpinnerWidth.setValue(262);
        }
        String heightProp = config.getProperty("paper.receipt.height");
        try {
            jSpinnerHeight.setValue(Integer.parseInt(heightProp != null ? heightProp : "546"));
        } catch (Exception ex) {
            jSpinnerHeight.setValue(546);
        }
        String xProp = config.getProperty("paper.receipt.x");
        try {
            jSpinnerX.setValue(Integer.parseInt(xProp != null ? xProp : "10"));
        } catch (Exception ex) {
            jSpinnerX.setValue(10);
        }
        String yProp = config.getProperty("paper.receipt.y");
        try {
            jSpinnerY.setValue(Integer.parseInt(yProp != null ? yProp : "10"));
        } catch (Exception ex) {
            jSpinnerY.setValue(10);
        }
        
        String fontNameProp = config.getProperty("paper.receipt.fontname");
        jComboFontName.setSelectedItem(fontNameProp != null ? fontNameProp : "Courier New");
        
        String fontSizeProp = config.getProperty("paper.receipt.fontsize");
        try {
            jSpinnerFontSize.setValue(Integer.parseInt(fontSizeProp != null ? fontSizeProp : "7"));
        } catch (Exception ex) {
            jSpinnerFontSize.setValue(7);
        }
        String columnsProp = config.getProperty("paper.receipt.columns");
        try {
            jSpinnerColumns.setValue(Integer.parseInt(columnsProp != null ? columnsProp : "42"));
        } catch (Exception ex) {
            jSpinnerColumns.setValue(42);
        }
        
        String fontBoldProp = config.getProperty("paper.receipt.fontbold");
        jCheckBold.setSelected(Boolean.parseBoolean(fontBoldProp != null ? fontBoldProp : "false"));
        String normalTotalsProp = config.getProperty("paper.receipt.normaltotals");
        jCheckNormalTotals.setSelected(Boolean.parseBoolean(normalTotalsProp != null ? normalTotalsProp : "false"));

        dirty.setDirty(false);

    }

    private void receiptPrefixExample() {
        receipt = "";
        x = 1;
        while (x < (Integer) jReceiptSize.getValue()) {
            receipt += "0";
            x++;
        }

        receipt += "1";
        jTicketExample.setText(jTextReceiptPrefix.getText() + receipt);
    }

    /*
     * JG Oct 2017 
     * This block to be used for internal SETS/RESETS and external ORDERS sync's  
     */
    public void loadUp() throws ClassNotFoundException, SQLException {

        /* Add external received order reset block here - 
        Get connex to secondary or external system's DB + [params]
        Pref' use is JSON/REST rather than PreparedStatement
         */
    }

    /**
     *
     * @param config
     */
    @Override
    public void saveProperties(AppConfig config) {

        config.setProperty("till.receiptprefix", jTextReceiptPrefix.getText());
        config.setProperty("till.receiptsize", jReceiptSize.getValue().toString());
        config.setProperty("till.pickupsize", jPickupSize.getValue().toString());
        config.setProperty("till.receiptprintoff", Boolean.toString(m_jReceiptPrintOff.isSelected()));

        config.setProperty("paper.receipt.width", jSpinnerWidth.getValue().toString());
        config.setProperty("paper.receipt.height", jSpinnerHeight.getValue().toString());
        config.setProperty("paper.receipt.x", jSpinnerX.getValue().toString());
        config.setProperty("paper.receipt.y", jSpinnerY.getValue().toString());
        config.setProperty("paper.receipt.fontname", jComboFontName.getSelectedItem().toString());
        config.setProperty("paper.receipt.fontsize", jSpinnerFontSize.getValue().toString());
        config.setProperty("paper.receipt.columns", jSpinnerColumns.getValue().toString());
        config.setProperty("paper.receipt.fontbold", Boolean.toString(jCheckBold.isSelected()));
        config.setProperty("paper.receipt.normaltotals", Boolean.toString(jCheckNormalTotals.isSelected()));

        dirty.setDirty(false);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTextField2 = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jReceiptSize = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        jTextReceiptPrefix = new javax.swing.JTextField();
        jTicketExample = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jPickupSize = new javax.swing.JSpinner();
        m_jReceiptPrintOff = new javax.swing.JCheckBox();
        jbtnReset = new javax.swing.JButton();
        jPanelTicketParams = new javax.swing.JPanel();
        jLabelWidth = new javax.swing.JLabel();
        jSpinnerWidth = new javax.swing.JSpinner();
        jLabelX = new javax.swing.JLabel();
        jSpinnerX = new javax.swing.JSpinner();
        jLabelHeight = new javax.swing.JLabel();
        jSpinnerHeight = new javax.swing.JSpinner();
        jLabelY = new javax.swing.JLabel();
        jSpinnerY = new javax.swing.JSpinner();
        jLabelFontName = new javax.swing.JLabel();
        jComboFontName = new javax.swing.JComboBox<>();
        jLabelFontSize = new javax.swing.JLabel();
        jSpinnerFontSize = new javax.swing.JSpinner();
        jLabelColumns = new javax.swing.JLabel();
        jSpinnerColumns = new javax.swing.JSpinner();
        jCheckBold = new javax.swing.JCheckBox();
        jCheckNormalTotals = new javax.swing.JCheckBox();

        jTextField2.setText("jTextField2");

        setBackground(new java.awt.Color(255, 255, 255));
        setOpaque(false);
        setPreferredSize(new java.awt.Dimension(700, 500));

        jLabel1.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("pos_messages"); // NOI18N
        jLabel1.setText(bundle.getString("label.ticketsetupnumber")); // NOI18N
        jLabel1.setPreferredSize(new java.awt.Dimension(190, 30));

        jReceiptSize.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jReceiptSize.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        jReceiptSize.setPreferredSize(new java.awt.Dimension(50, 30));
        jReceiptSize.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jReceiptSizeStateChanged(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel3.setText(bundle.getString("label.ticketsetupprefix")); // NOI18N

        jTextReceiptPrefix.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jTextReceiptPrefix.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextReceiptPrefix.setPreferredSize(new java.awt.Dimension(100, 30));
        jTextReceiptPrefix.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextReceiptPrefixKeyReleased(evt);
            }
        });

        jTicketExample.setEditable(false);
        jTicketExample.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jTicketExample.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTicketExample.setText("1");
        jTicketExample.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        jTicketExample.setEnabled(false);
        jTicketExample.setPreferredSize(new java.awt.Dimension(100, 30));

        jLabel2.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel2.setText(bundle.getString("label.pickupcodesize")); // NOI18N
        jLabel2.setToolTipText(bundle.getString("label.pickupcodesize")); // NOI18N
        jLabel2.setPreferredSize(new java.awt.Dimension(190, 30));

        jPickupSize.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jPickupSize.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        jPickupSize.setToolTipText("");
        jPickupSize.setPreferredSize(new java.awt.Dimension(50, 30));
        jPickupSize.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jPickupSizeStateChanged(evt);
            }
        });

        m_jReceiptPrintOff.setBackground(new java.awt.Color(255, 255, 255));
        m_jReceiptPrintOff.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jReceiptPrintOff.setText(bundle.getString("label.receiptprint")); // NOI18N
        m_jReceiptPrintOff.setPreferredSize(new java.awt.Dimension(180, 30));
        m_jReceiptPrintOff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jReceiptPrintOffActionPerformed(evt);
            }
        });

        jbtnReset.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jbtnReset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/reload.png"))); // NOI18N
        jbtnReset.setText(AppLocal.getIntString("label.resetpickup")); // NOI18N
        jbtnReset.setMaximumSize(new java.awt.Dimension(70, 33));
        jbtnReset.setMinimumSize(new java.awt.Dimension(70, 33));
        jbtnReset.setPreferredSize(new java.awt.Dimension(100, 45));
        jbtnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnResetActionPerformed(evt);
            }
        });

        jPanelTicketParams.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Configuración del Ancho y Fuente del Ticket", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 1, 14))); // NOI18N
        jPanelTicketParams.setOpaque(false);
        jPanelTicketParams.setLayout(new java.awt.GridLayout(4, 4, 10, 10));

        jLabelWidth.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabelWidth.setText("Ancho Papel (puntos):");
        jPanelTicketParams.add(jLabelWidth);

        jSpinnerWidth.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jSpinnerWidth.setModel(new javax.swing.SpinnerNumberModel(262, 100, 800, 1));
        jPanelTicketParams.add(jSpinnerWidth);

        jLabelX.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabelX.setText("Margen Izq. (X):");
        jPanelTicketParams.add(jLabelX);

        jSpinnerX.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jSpinnerX.setModel(new javax.swing.SpinnerNumberModel(10, 0, 100, 1));
        jPanelTicketParams.add(jSpinnerX);

        jLabelHeight.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabelHeight.setText("Alto Papel (puntos):");
        jPanelTicketParams.add(jLabelHeight);

        jSpinnerHeight.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jSpinnerHeight.setModel(new javax.swing.SpinnerNumberModel(546, 100, 2000, 1));
        jPanelTicketParams.add(jSpinnerHeight);

        jLabelY.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabelY.setText("Margen Sup. (Y):");
        jPanelTicketParams.add(jLabelY);

        jSpinnerY.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jSpinnerY.setModel(new javax.swing.SpinnerNumberModel(10, 0, 100, 1));
        jPanelTicketParams.add(jSpinnerY);

        jLabelFontName.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabelFontName.setText("Fuente de Impresión:");
        jPanelTicketParams.add(jLabelFontName);

        jComboFontName.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jComboFontName.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Courier New", "Monospaced", "Arial", "Lucida Console", "Segoe UI", "Tahoma" }));
        jPanelTicketParams.add(jComboFontName);

        jLabelFontSize.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabelFontSize.setText("Tamaño de Fuente:");
        jPanelTicketParams.add(jLabelFontSize);

        jSpinnerFontSize.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jSpinnerFontSize.setModel(new javax.swing.SpinnerNumberModel(7, 5, 24, 1));
        jPanelTicketParams.add(jSpinnerFontSize);

        jLabelColumns.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabelColumns.setText("Columnas de Texto:");
        jPanelTicketParams.add(jLabelColumns);

        jSpinnerColumns.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jSpinnerColumns.setModel(new javax.swing.SpinnerNumberModel(42, 20, 100, 1));
        jPanelTicketParams.add(jSpinnerColumns);

        jCheckBold.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jCheckBold.setText("Poner todo en Negritas");
        jCheckBold.setOpaque(false);
        jPanelTicketParams.add(jCheckBold);

        jCheckNormalTotals.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jCheckNormalTotals.setText("Fuente normal en totales");
        jCheckNormalTotals.setOpaque(false);
        jPanelTicketParams.add(jCheckNormalTotals);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(m_jReceiptPrintOff, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jTextReceiptPrefix, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE)
                            .addComponent(jReceiptSize, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPickupSize, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(6, 6, 6)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jbtnReset, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTicketExample, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(247, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelTicketParams, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jReceiptSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jTextReceiptPrefix, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTicketExample, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jPickupSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbtnReset, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(m_jReceiptPrintOff, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanelTicketParams, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(120, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jTextReceiptPrefixKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextReceiptPrefixKeyReleased

        jTicketExample.setText(jTextReceiptPrefix.getText() + receipt);
    }//GEN-LAST:event_jTextReceiptPrefixKeyReleased

    private void jReceiptSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jReceiptSizeStateChanged

        receiptPrefixExample();

    }//GEN-LAST:event_jReceiptSizeStateChanged

    private void jPickupSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jPickupSizeStateChanged

    }//GEN-LAST:event_jPickupSizeStateChanged

    private void m_jReceiptPrintOffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jReceiptPrintOffActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_m_jReceiptPrintOffActionPerformed

    private void jbtnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnResetActionPerformed

        int response = JOptionPane.showOptionDialog(this,
                AppLocal.getIntString("message.resetpickup"),
                "Reset",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, null, null);
        if (response == JOptionPane.YES_OPTION) {
            try {

                String db_user = (config.getProperty("db.user"));
                String db_url = (config.getProperty("db.URL") + config.getProperty("db.schema") + config.getProperty("db.options"));
                String db_password = (config.getProperty("db.password"));

                if (db_user != null && db_password != null && db_password.startsWith("crypt:")) {
                    AltEncrypter cypher = new AltEncrypter("cypherkey" + db_user);
                    db_password = cypher.decrypt(db_password.substring(6));
                }

                Session session = new Session(db_url, db_user, db_password);
                session.begin();
                session.DB.getSequenceSentence(session, "pickup_number").find();
                session.DB.resetSequenceSentence(session, "pickup_number");
                session.commit();

            } catch (BasicException | SQLException ex) {
                LOGGER.log(Level.WARNING, null, ex);
            }

        }

    }//GEN-LAST:event_jbtnResetActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JSpinner jPickupSize;
    private javax.swing.JSpinner jReceiptSize;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextReceiptPrefix;
    private javax.swing.JTextField jTicketExample;
    private javax.swing.JButton jbtnReset;
    private javax.swing.JCheckBox m_jReceiptPrintOff;
    
    private javax.swing.JPanel jPanelTicketParams;
    private javax.swing.JLabel jLabelWidth;
    private javax.swing.JSpinner jSpinnerWidth;
    private javax.swing.JLabel jLabelHeight;
    private javax.swing.JSpinner jSpinnerHeight;
    private javax.swing.JLabel jLabelX;
    private javax.swing.JSpinner jSpinnerX;
    private javax.swing.JLabel jLabelY;
    private javax.swing.JSpinner jSpinnerY;
    private javax.swing.JLabel jLabelFontName;
    private javax.swing.JComboBox<String> jComboFontName;
    private javax.swing.JLabel jLabelFontSize;
    private javax.swing.JSpinner jSpinnerFontSize;
    private javax.swing.JLabel jLabelColumns;
    private javax.swing.JSpinner jSpinnerColumns;
    private javax.swing.JCheckBox jCheckBold;
    private javax.swing.JCheckBox jCheckNormalTotals;
    // End of variables declaration//GEN-END:variables

}
