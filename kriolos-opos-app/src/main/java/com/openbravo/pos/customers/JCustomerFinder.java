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

package com.openbravo.pos.customers;

import com.openbravo.pos.businesspartner.BusinessPartnerListCellRenderer;
import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.QBFCompareEnum;
import com.openbravo.data.user.EditorCreator;
import com.openbravo.data.user.ListProvider;
import com.openbravo.data.user.ListProviderCreator;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import java.awt.*;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 *
 * @author adrianromero
 */
public class JCustomerFinder extends javax.swing.JDialog implements EditorCreator {

    private CustomerInfo m_ReturnCustomer;
    private ListProvider lpr;
    private AppView appView;
    private DataLogicCustomers dlCustomers;

    public void searchKey() {
        jbtnExecute.setMnemonic(KeyEvent.VK_E); 
        executeSearch();
    }

    public void resetKey() {
        jbtnReset.setMnemonic(KeyEvent.VK_R);
        m_jtxtTaxID.reset();
        m_jtxtSearchKey.reset();
        m_jtxtName.reset();
        m_jtxtPostal.reset();
        m_jtxtPhone.reset();
        m_jtxtEmail.reset();
        m_jtxtTaxID.activate();

        cleanSearch();
    }

    public void setAppView(AppView appView) {
        this.appView = appView;
    }

    private JCustomerFinder(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
    }

    private JCustomerFinder(java.awt.Dialog parent, boolean modal) {
        super(parent, modal);
    }

    public static JCustomerFinder getCustomerFinder(Component parent, DataLogicCustomers dlCustomers) {
        Window window = getWindow(parent);

        JCustomerFinder myMsg;
        if (window instanceof Frame) {
            myMsg = new JCustomerFinder((Frame) window, true);
        } else {
            myMsg = new JCustomerFinder((Dialog) window, true);
        }
        myMsg.init(dlCustomers);
        myMsg.applyComponentOrientation(parent.getComponentOrientation());

        return myMsg;
    }

    public CustomerInfo getSelectedCustomer() {
        return m_ReturnCustomer;
    }

    private void init(DataLogicCustomers dlCustomers) {
        this.dlCustomers = dlCustomers;
        initComponents();

        setLargeFont(this);

        jScrollPane1.getVerticalScrollBar().setPreferredSize(new Dimension(35, 35));

        m_jtxtTaxID.addEditorKeys(m_jKeys);
        m_jtxtSearchKey.addEditorKeys(m_jKeys);
        m_jtxtName.addEditorKeys(m_jKeys);
        m_jtxtPostal.addEditorKeys(m_jKeys);
        m_jtxtPhone.addEditorKeys(m_jKeys);
        m_jtxtEmail.addEditorKeys(m_jKeys);

        m_jtxtTaxID.reset();
        m_jtxtSearchKey.reset();
        m_jtxtName.reset();
        m_jtxtPostal.reset();
        m_jtxtPhone.reset();
        m_jtxtEmail.reset();

        m_jtxtTaxID.activate();

        lpr = new ListProviderCreator(dlCustomers.getCustomerList(), this);

        jListCustomers.setCellRenderer(new BusinessPartnerListCellRenderer());

        getRootPane().setDefaultButton(jcmdOK);

        m_ReturnCustomer = null;
    }

    public void search(CustomerInfo customer) {
        if (customer == null || customer.getName() == null || customer.getName().equals("")) {
            m_jtxtTaxID.reset();
            m_jtxtSearchKey.reset();
            m_jtxtName.reset();
            m_jtxtPostal.reset();
            m_jtxtPhone.reset();
            m_jtxtEmail.reset();
            m_jtxtTaxID.activate();
            cleanSearch();
        } else {
            m_jtxtTaxID.setText(customer.getTaxid());
            m_jtxtSearchKey.setText(customer.getSearchkey());
            m_jtxtName.setText(customer.getName());
            m_jtxtPostal.setText(customer.getPostal());
            m_jtxtPhone.setText(customer.getPhone());
            m_jtxtEmail.setText(customer.getEmail());
            m_jtxtTaxID.activate();
            executeSearch();
        }
    }

    private void cleanSearch() {
        m_jtxtTaxID.setText("");
        m_jtxtSearchKey.setText("");
        m_jtxtName.setText("");
        m_jtxtPostal.setText("");
        m_jtxtPhone.setText("");
        m_jtxtEmail.setText("");
        jListCustomers.setModel(new MyListData(new ArrayList()));
    }

    public void executeSearch() {
        try {
            // Sincronizar puntos desde Supabase antes de buscar
            dlCustomers.refreshLocalCustomersFromSupabase();
            
            jListCustomers.setModel(new MyListData(lpr.loadData()));
            if (jListCustomers.getModel().getSize() > 0) {
                jListCustomers.setSelectedIndex(0);
            } else {
                if(!m_jtxtName.getText().equals("")) {
                    int n = JOptionPane.showConfirmDialog(
                        null,
                        AppLocal.getIntString("message.customernotfound"),
                        AppLocal.getIntString("title.editor"),
                        JOptionPane.YES_NO_OPTION);

                    if (n != 1) {
                        this.setVisible(false);
                        appView.getAppUserView().showTask("com.openbravo.pos.customers.CustomersPanel");
                        JOptionPane.showMessageDialog(null, 
                            "You must complete Account and Search Key Then Save to add to Ticket",
                            "Create Customer",JOptionPane.OK_OPTION);
                    }
                }
            }
        } catch (BasicException e) {
            // Log error or show message
        }
    }

    @Override
    public Object createValue() throws BasicException {
        Object[] afilter = new Object[12];
        if (m_jtxtTaxID.getText() == null || m_jtxtTaxID.getText().equals("")) {
            afilter[0] = QBFCompareEnum.COMP_NONE;
            afilter[1] = null;
        } else {
            afilter[0] = QBFCompareEnum.COMP_RE;
            afilter[1] = "%" + m_jtxtTaxID.getText() + "%";
        }
        if (m_jtxtSearchKey.getText() == null || m_jtxtSearchKey.getText().equals("")) {
            afilter[2] = QBFCompareEnum.COMP_NONE;
            afilter[3] = null;
        } else {
            afilter[2] = QBFCompareEnum.COMP_RE;
            afilter[3] = "%" + m_jtxtSearchKey.getText() + "%";
        }
        if (m_jtxtName.getText() == null || m_jtxtName.getText().equals("")) {
            afilter[4] = QBFCompareEnum.COMP_NONE;
            afilter[5] = null;
        } else {
            afilter[4] = QBFCompareEnum.COMP_RE;
            afilter[5] = "%" + m_jtxtName.getText() + "%";
        }
        if (m_jtxtPostal.getText() == null || m_jtxtPostal.getText().equals("")) {
            afilter[6] = QBFCompareEnum.COMP_NONE;
            afilter[7] = null;
        } else {
            afilter[6] = QBFCompareEnum.COMP_RE;
            afilter[7] = "%" + m_jtxtPostal.getText() + "%";
        }
        if (m_jtxtPhone.getText() == null || m_jtxtPhone.getText().equals("")) {
            afilter[8] = QBFCompareEnum.COMP_NONE;
            afilter[9] = null;
        } else {
            afilter[8] = QBFCompareEnum.COMP_RE;
            afilter[9] = "%" + m_jtxtPhone.getText() + "%";
        }
        if (m_jtxtEmail.getText() == null || m_jtxtEmail.getText().equals("")) {
            afilter[10] = QBFCompareEnum.COMP_NONE;
            afilter[11] = null;
        } else {
            afilter[10] = QBFCompareEnum.COMP_RE;
            afilter[11] = "%" + m_jtxtEmail.getText() + "%";
        }
        return afilter;
    }

    private static Window getWindow(Component parent) {
        if (parent == null) {
            return new JFrame();
        } else if (parent instanceof Frame || parent instanceof Dialog) {
            return (Window) parent;
        } else {
            return getWindow(parent.getParent());
        }
    }

    private static class MyListData extends javax.swing.AbstractListModel {
        private final java.util.List m_data;
        public MyListData(java.util.List data) {
            m_data = data;
        }
        @Override
        public Object getElementAt(int index) {
            return m_data.get(index);
        }
        @Override
        public int getSize() {
            return m_data.size();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        m_jKeys = new com.openbravo.editor.JEditorKeys();
        jPanel8 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jcmdCancel = new javax.swing.JButton();
        jcmdOK = new javax.swing.JButton();
        jImageViewerCustomer = new com.openbravo.data.gui.JImageViewer();
        jPanel3 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jLblTaxID = new javax.swing.JLabel();
        m_jtxtTaxID = new com.openbravo.editor.JEditorString();
        jLblSearchKey = new javax.swing.JLabel();
        m_jtxtSearchKey = new com.openbravo.editor.JEditorString();
        jLblPostal = new javax.swing.JLabel();
        m_jtxtPostal = new com.openbravo.editor.JEditorString();
        jLblName = new javax.swing.JLabel();
        m_jtxtName = new com.openbravo.editor.JEditorString();
        jLblPhone = new javax.swing.JLabel();
        jLblEmail = new javax.swing.JLabel();
        m_jtxtPhone = new com.openbravo.editor.JEditorString();
        m_jtxtEmail = new com.openbravo.editor.JEditorString();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jListCustomers = new javax.swing.JList();
        jPanel6 = new javax.swing.JPanel();
        jbtnReset = new javax.swing.JButton();
        jbtnExecute = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(AppLocal.getIntString("form.customertitle")); 
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        jPanel2.setLayout(new java.awt.BorderLayout());
        jPanel2.add(m_jKeys, java.awt.BorderLayout.NORTH);

        jPanel8.setLayout(new java.awt.BorderLayout());

        jcmdCancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/cancel.png")));
        jcmdCancel.setText(AppLocal.getIntString("button.cancel"));
        jcmdCancel.setPreferredSize(new java.awt.Dimension(110, 45));
        jcmdCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jcmdCancelActionPerformed(evt);
            }
        });
        jPanel1.add(jcmdCancel);

        jcmdOK.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/ok.png")));
        jcmdOK.setText(AppLocal.getIntString("button.ok"));
        jcmdOK.setEnabled(false);
        jcmdOK.setPreferredSize(new java.awt.Dimension(110, 45));
        jcmdOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jcmdOKActionPerformed(evt);
            }
        });
        jPanel1.add(jcmdOK);

        jPanel8.add(jPanel1, java.awt.BorderLayout.LINE_END);
        jPanel2.add(jPanel8, java.awt.BorderLayout.PAGE_END);
        jPanel2.add(jImageViewerCustomer, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel2, java.awt.BorderLayout.LINE_END);

        jPanel3.setPreferredSize(new java.awt.Dimension(450, 0));
        jPanel3.setLayout(new java.awt.BorderLayout());

        jPanel5.setLayout(new java.awt.BorderLayout());

        jLblTaxID.setText(AppLocal.getIntString("label.taxid"));
        jLblTaxID.setPreferredSize(new java.awt.Dimension(110, 30));
        m_jtxtTaxID.setPreferredSize(new java.awt.Dimension(200, 30));

        jLblSearchKey.setText(AppLocal.getIntString("label.searchkey"));
        jLblSearchKey.setPreferredSize(new java.awt.Dimension(110, 30));
        m_jtxtSearchKey.setPreferredSize(new java.awt.Dimension(250, 30));

        jLblPostal.setText("Postal");
        jLblPostal.setPreferredSize(new java.awt.Dimension(110, 30));
        m_jtxtPostal.setPreferredSize(new java.awt.Dimension(250, 30));

        jLblName.setText(AppLocal.getIntString("label.prodname"));
        jLblName.setPreferredSize(new java.awt.Dimension(110, 30));
        m_jtxtName.setPreferredSize(new java.awt.Dimension(250, 30));

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("pos_messages"); 
        jLblPhone.setText(bundle.getString("label.companytelephone"));
        jLblPhone.setPreferredSize(new java.awt.Dimension(110, 30));
        jLblEmail.setText(bundle.getString("label.companyemail"));
        jLblEmail.setPreferredSize(new java.awt.Dimension(110, 30));

        m_jtxtPhone.setPreferredSize(new java.awt.Dimension(250, 30));
        m_jtxtEmail.setPreferredSize(new java.awt.Dimension(250, 30));

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLblTaxID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(m_jtxtTaxID, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLblName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLblSearchKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLblPostal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(m_jtxtSearchKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(m_jtxtPostal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(m_jtxtName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLblPhone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLblEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(m_jtxtPhone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(m_jtxtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(m_jtxtTaxID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLblTaxID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(m_jtxtSearchKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLblSearchKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLblPostal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jtxtPostal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(m_jtxtName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLblName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLblPhone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jtxtPhone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLblEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jtxtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel5.add(jPanel7, java.awt.BorderLayout.CENTER);
        jPanel3.add(jPanel5, java.awt.BorderLayout.PAGE_START);

        jPanel4.setLayout(new java.awt.BorderLayout());
        jListCustomers.setFont(new java.awt.Font("Arial", 0, 14));
        jListCustomers.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jListCustomersValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jListCustomers);
        jPanel4.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jbtnReset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/reload.png")));
        jbtnReset.setText(bundle.getString("button.reset"));
        jbtnReset.setPreferredSize(new java.awt.Dimension(110, 45));
        jbtnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnResetActionPerformed(evt);
            }
        });
        jPanel6.add(jbtnReset);

        jbtnExecute.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/ok.png")));
        jbtnExecute.setText(AppLocal.getIntString("button.executefilter"));
        jbtnExecute.setPreferredSize(new java.awt.Dimension(110, 45));
        jbtnExecute.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnExecuteActionPerformed(evt);
            }
        });
        jPanel6.add(jbtnExecute);
        jPanel4.add(jPanel6, java.awt.BorderLayout.PAGE_START);
        jPanel3.add(jPanel4, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel3, java.awt.BorderLayout.CENTER);
        setSize(new java.awt.Dimension(758, 634));
        setLocationRelativeTo(null);
    }
    // </editor-fold>

    private void jcmdOKActionPerformed(java.awt.event.ActionEvent evt) {
        m_ReturnCustomer = (CustomerInfo) jListCustomers.getSelectedValue();
        dispose();
    }

    private void jcmdCancelActionPerformed(java.awt.event.ActionEvent evt) {
        dispose();
    }

    private void jbtnExecuteActionPerformed(java.awt.event.ActionEvent evt) {
        m_ReturnCustomer=null;
        executeSearch();
    }

    private void jListCustomersValueChanged(javax.swing.event.ListSelectionEvent evt) {
        m_ReturnCustomer = (CustomerInfo) jListCustomers.getSelectedValue();
        if (m_ReturnCustomer != null) {
            jImageViewerCustomer.setImage(m_ReturnCustomer.getImage());
        }         
        jcmdOK.setEnabled(jListCustomers.getSelectedValue() != null);
    }

    private void jbtnResetActionPerformed(java.awt.event.ActionEvent evt) {
        m_jtxtTaxID.reset();
        m_jtxtSearchKey.reset();
        m_jtxtName.reset();
        m_jtxtPostal.reset();
        m_jtxtPhone.reset();
        m_jtxtEmail.reset();
        m_jtxtTaxID.activate(); 
        cleanSearch();
    }

    // Variables declaration
    private com.openbravo.data.gui.JImageViewer jImageViewerCustomer;
    private javax.swing.JLabel jLblEmail;
    private javax.swing.JLabel jLblName;
    private javax.swing.JLabel jLblPhone;
    private javax.swing.JLabel jLblPostal;
    private javax.swing.JLabel jLblSearchKey;
    private javax.swing.JLabel jLblTaxID;
    private javax.swing.JList jListCustomers;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton jbtnExecute;
    private javax.swing.JButton jbtnReset;
    private javax.swing.JButton jcmdCancel;
    private javax.swing.JButton jcmdOK;
    private com.openbravo.editor.JEditorKeys m_jKeys;
    private com.openbravo.editor.JEditorString m_jtxtEmail;
    private com.openbravo.editor.JEditorString m_jtxtName;
    private com.openbravo.editor.JEditorString m_jtxtPhone;
    private com.openbravo.editor.JEditorString m_jtxtPostal;
    private com.openbravo.editor.JEditorString m_jtxtSearchKey;
    private com.openbravo.editor.JEditorString m_jtxtTaxID;

    /**
     * Aplica el tipo de letra Segoe UI 24 de manera recursiva a todos los componentes
     * del buscador de clientes para una perfecta legibilidad.
     */
    private void setLargeFont(java.awt.Component comp) {
        if (comp == null) return;
        
        java.awt.Font currentFont = comp.getFont();
        if (currentFont == null || currentFont.getSize() < 24) {
            comp.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 24));
        }
        
        if (comp instanceof javax.swing.JTable) {
            javax.swing.JTable t = (javax.swing.JTable) comp;
            t.setRowHeight(32);
            t.getTableHeader().setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        }
        if (comp instanceof javax.swing.text.JTextComponent) {
            comp.setPreferredSize(new java.awt.Dimension(comp.getPreferredSize().width, 36));
        }
        if (comp instanceof java.awt.Container) {
            for (java.awt.Component child : ((java.awt.Container) comp).getComponents()) {
                setLargeFont(child);
            }
        }
    }
}
