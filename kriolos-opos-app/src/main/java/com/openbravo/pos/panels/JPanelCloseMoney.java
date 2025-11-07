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

package com.openbravo.pos.panels;

import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.forms.AppUser;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.gui.TableRendererBasic;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.data.loader.Session;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import com.openbravo.data.loader.StaticSentence;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.*;
import com.openbravo.pos.printer.TicketParser;
import com.openbravo.pos.printer.TicketPrinterException;
import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptException;
import com.openbravo.pos.scripting.ScriptFactory;
import java.awt.Dimension;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author adrianromero
 */
public class JPanelCloseMoney extends JPanel implements JPanelView, BeanFactoryApp {
    private static final Logger LOGGER = Logger.getLogger(JPanelCloseMoney.class.getName());
    private AppView m_App;
    private DataLogicSystem m_dlSystem;
    
    private PaymentsModel m_PaymentsToClose = null;   
    
    private TicketParser m_TTP;
    private final DateFormat df= new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");   
    
    private Session s;
    private Connection con;  
    private Statement stmt;
    private Integer result;
    private Integer dresult;    
    private String SQL;
    private ResultSet rs;
    
    private AppUser m_User;
    
    
    /** Creates new form JPanelCloseMoney */
    public JPanelCloseMoney() {
        initComponents();
        
        // Aplicar diseño moderno
        setupModernUI();
             
    }
    
    /**
     *
     * @param app
     * @throws BeanFactoryException
     */
    @Override
    public void init(AppView app) throws BeanFactoryException {
        
        m_App = app;        
        m_dlSystem = (DataLogicSystem) m_App.getBean("com.openbravo.pos.forms.DataLogicSystem");
        m_TTP = new TicketParser(m_App.getDeviceTicket(), m_dlSystem);

        m_jTicketTable.setDefaultRenderer(Object.class, new TableRendererBasic(
            new Formats[] {new FormatsPayment(), Formats.CURRENCY, Formats.INT}));
        m_jTicketTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        m_jScrollTableTicket.getVerticalScrollBar().setPreferredSize(new Dimension(25,25));       
        m_jTicketTable.getTableHeader().setReorderingAllowed(false);         
        m_jTicketTable.setRowHeight(25);
        m_jTicketTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);         
        
        m_jsalestable.setDefaultRenderer(Object.class, new TableRendererBasic(
            new Formats[] {Formats.STRING, Formats.CURRENCY, Formats.CURRENCY, Formats.CURRENCY}));
        m_jsalestable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        m_jScrollSales.getVerticalScrollBar().setPreferredSize(new Dimension(25,25));       
        m_jsalestable.getTableHeader().setReorderingAllowed(false);         
        m_jsalestable.setRowHeight(25);
        m_jsalestable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
    }
    
    /**
     *
     * @return
     */
    @Override
    public Object getBean() {
        return this;
    }
    
    /**
     *
     * @return
     */
    @Override
    public JComponent getComponent() {
        return this;
    }
    /**
     * @return 
     */

    
    /**
     *
     * @return
     */
    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.CloseTPV");
    }

    /**
     *
     * @throws BasicException
     */
    @Override
    public void activate() throws BasicException {
        loadData();
    }

    /**
     *
     * @return
     */
    @Override
    public boolean deactivate() {

        return true;
    }  
    
    private void loadData() throws BasicException {
        
        // Reset
        m_jSequence.setText(null);
        m_jMinDate.setText(null);
        m_jMaxDate.setText(null);
        m_jPrintCashPreview.setEnabled(false);
        m_jCloseCash.setEnabled(false);
        
        
        m_jCount.setText(null);
        m_jCash.setText(null);
        m_jInitialAmount.setText(null);

        m_jSales.setText(null);
        m_jSalesSubtotal.setText(null);
        m_jSalesTaxes.setText(null);
        m_jSalesTotal.setText(null);
        
        m_jTicketTable.setModel(new DefaultTableModel());
        m_jsalestable.setModel(new DefaultTableModel());
            
        // LoadData
        m_PaymentsToClose = PaymentsModel.loadInstance(m_App);
        
        // Populate Data
        m_jSequence.setText(m_PaymentsToClose.printSequence());
        m_jMinDate.setText(m_PaymentsToClose.printDateStart());
        m_jMaxDate.setText(m_PaymentsToClose.printDateEnd());
        
        if (m_PaymentsToClose.getPayments() != 0 
                || m_PaymentsToClose.getSales() != 0) {

            m_jPrintCashPreview.setEnabled(true);
            m_jCloseCash.setEnabled(true);
       
            
            m_jCount.setText(m_PaymentsToClose.printPayments());
            m_jCash.setText(m_PaymentsToClose.printPaymentsTotal());
            
            // Obtener el monto inicial de la caja abierta basado en HOSTSEQUENCE
            try {
                int hostSequence = m_PaymentsToClose.getSequence(); // Obtener HOSTSEQUENCE
                String host = m_App.getProperties().getHost();
                
                s = m_App.getSession();
                con = s.getConnection();
                
                SQL = "SELECT INITIAL_AMOUNT FROM CLOSEDCASH WHERE HOST = ? AND HOSTSEQUENCE = ? ORDER BY DATEEND DESC LIMIT 1";
                
                // Para HSQLDB cambiar LIMIT por TOP
                String sdbmanager = m_dlSystem.getDBVersion();
                if ("HSQL Database Engine".equals(sdbmanager)) {
                    SQL = "SELECT TOP 1 INITIAL_AMOUNT FROM CLOSEDCASH WHERE HOST = ? AND HOSTSEQUENCE = ? ORDER BY DATEEND DESC";
                }
                
                java.sql.PreparedStatement pstmt = con.prepareStatement(SQL);
                pstmt.setString(1, host);
                pstmt.setInt(2, hostSequence);
                
                ResultSet rsInitial = pstmt.executeQuery();
                if (rsInitial.next()) {
                    double initialAmount = rsInitial.getDouble("INITIAL_AMOUNT");
                    if (!rsInitial.wasNull()) {
                        m_jInitialAmount.setText(Formats.CURRENCY.formatValue(initialAmount));
                    } else {
                        m_jInitialAmount.setText(Formats.CURRENCY.formatValue(0.0));
                    }
                } else {
                    m_jInitialAmount.setText(Formats.CURRENCY.formatValue(0.0));
                }
                
                rsInitial.close();
                pstmt.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo monto inicial: " + e.getMessage(), e);
                m_jInitialAmount.setText(Formats.CURRENCY.formatValue(0.0));
            }
            
            m_jSales.setText(m_PaymentsToClose.printSales());
            m_jSalesSubtotal.setText(m_PaymentsToClose.printSalesBase());
            m_jSalesTaxes.setText(m_PaymentsToClose.printSalesTaxes());
            m_jSalesTotal.setText(m_PaymentsToClose.printSalesTotal());
        }          

        m_jTicketTable.setModel(m_PaymentsToClose.getPaymentsModel());
                
        TableColumnModel jColumns = m_jTicketTable.getColumnModel();
        jColumns.getColumn(0).setPreferredWidth(100);
        jColumns.getColumn(0).setResizable(false);
        jColumns.getColumn(1).setPreferredWidth(100);
        jColumns.getColumn(1).setResizable(false);
        jColumns.getColumn(2).setPreferredWidth(100);
        jColumns.getColumn(2).setResizable(false);   

        
        m_jsalestable.setModel(m_PaymentsToClose.getSalesModel());
        
        jColumns = m_jsalestable.getColumnModel();
        jColumns.getColumn(0).setPreferredWidth(100);
        jColumns.getColumn(0).setResizable(false);
        jColumns.getColumn(1).setPreferredWidth(100);
        jColumns.getColumn(1).setResizable(false);
        jColumns.getColumn(2).setPreferredWidth(100);
        jColumns.getColumn(2).setResizable(false);        
                               
// read number of no cash drawer activations
       try{
            result=0;
            s=m_App.getSession();
            con=s.getConnection();  
            String sdbmanager = m_dlSystem.getDBVersion();           

            SQL = "SELECT * " +
                        "FROM draweropened " +
                        "WHERE TICKETID = 'No Sale' AND OPENDATE > {fn TIMESTAMP('" + m_PaymentsToClose.getDateStartDerby() + "')}";

            stmt = (Statement) con.createStatement();      
            rs = stmt.executeQuery(SQL);
            while (rs.next()){
                result ++;           
            }
                rs=null;

// Get Ticket DELETES & Line Voids            
            dresult=0;
            SQL = "SELECT * " +
                        "FROM lineremoved " +
                        "WHERE REMOVEDDATE > {fn TIMESTAMP('" + m_PaymentsToClose.getDateStartDerby() + "')}"; 

            stmt = (Statement) con.createStatement();      
            rs = stmt.executeQuery(SQL);
            while (rs.next()){
                dresult ++;           
            }
                rs=null;
                con=null;
                s=null;                
            }  
        catch (SQLException e){}         

        m_jLinesRemoved.setText(dresult.toString());
        m_jNoCashSales.setText(result.toString());              
    }   
    
    private void CloseCash() {

        int res = JOptionPane.showConfirmDialog(this, 
                AppLocal.getIntString("message.wannaclosecash"), 
                AppLocal.getIntString("message.title"), 
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        if (res == JOptionPane.YES_OPTION) {

            Date dNow = new Date();

            try {

                if (m_App.getActiveCashDateEnd() == null) {
                    new StaticSentence(m_App.getSession()
                        , "UPDATE closedcash SET DATEEND = ?, NOSALES = ? WHERE HOST = ? AND MONEY = ?"
                        , new SerializerWriteBasic(new Datas[] {
                            Datas.TIMESTAMP, 
                            Datas.INT, 
                            Datas.STRING, 
                            Datas.STRING}))
                    .exec(new Object[] {dNow, result, 
                        m_App.getProperties().getHost(), 
                        m_App.getActiveCashIndex()});
                }
            } catch (BasicException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, 
                        AppLocal.getIntString("message.cannotclosecash"), e);
                msg.show(this);
            }

            try {
                // Create NEW CloshCash Sequence
                m_App.setActiveCash(UUID.randomUUID().toString(), 
                        m_App.getActiveCashSequence() + 1, dNow, null);

                // Create CURRENT CloseCash Sequence
                m_dlSystem.execInsertCash(
                    new Object[] {m_App.getActiveCashIndex(), 
                        m_App.getProperties().getHost(), 
                        m_App.getActiveCashSequence(), 
                        m_App.getActiveCashDateStart(), 
                        m_App.getActiveCashDateEnd(),0.0});

                m_dlSystem.execDrawerOpened(
                    new Object[] {m_App.getAppUserView().getUser().getName(),"Close Cash"});

                // Set ENDDATE CloseCash Date
                m_PaymentsToClose.setDateEnd(dNow);

                // print report
                printPayments("Printer.CloseCash");

                // Close Cash Message
                JOptionPane.showMessageDialog(this, 
                        AppLocal.getIntString("message.closecashok"), 
                        AppLocal.getIntString("message.title"), 
                        JOptionPane.INFORMATION_MESSAGE);
                
                // Navegar automáticamente a la vista de configuración de Supabase para subir datos
                try {
                    // Obtener AppUserView desde JRootApp para poder usar showTask
                    if (m_App instanceof com.openbravo.pos.forms.JRootApp) {
                        com.openbravo.pos.forms.JRootApp rootApp = (com.openbravo.pos.forms.JRootApp) m_App;
                        com.openbravo.pos.forms.AppUserView appUserView = rootApp.getAppUserView();
                        if (appUserView != null) {
                            appUserView.showTask("com.openbravo.pos.config.JPanelConfiguration");
                            LOGGER.info("Navegando a configuración de Supabase después de cerrar caja");
                        } else {
                            LOGGER.warning("No se pudo obtener AppUserView para navegar a configuración");
                        }
                    } else {
                        LOGGER.warning("m_App no es instancia de JRootApp");
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "No se pudo navegar a configuración de Supabase", ex);
                }
            } catch (BasicException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, 
                        AppLocal.getIntString("message.cannotclosecash"), e);
                msg.show(this);
            }

            try {
                loadData();
            } catch (BasicException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, 
                        AppLocal.getIntString("label.noticketstoclose"), e);
                msg.show(this);
            }
        }        
    }
    
    private void printPayments(String report) {
        
        String sresource = m_dlSystem.getResourceAsXML(report);
        if (sresource == null) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, 
                    AppLocal.getIntString("message.cannotprintticket"));
            msg.show(this);
        } else {
            try {
                ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
                script.put("payments", m_PaymentsToClose);
                script.put("nosales",result.toString());                
                m_TTP.printTicket(script.eval(sresource).toString());
// JG 16 May 2012 use multicatch
            } catch (ScriptException | TicketPrinterException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, 
                        AppLocal.getIntString("message.cannotprintticket"), e);
                msg.show(this);
            }
        }
    }
    
    private class FormatsPayment extends Formats {
        @Override
        protected String formatValueInt(Object value) {
            return AppLocal.getIntString("transpayment." + (String) value);
        }   
        @Override
        protected Object parseValueInt(String value) throws ParseException {
            return value;
        }
        @Override
        public int getAlignment() {
            return javax.swing.SwingConstants.LEFT;
        }         
    }    
   
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        m_jSequence = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        m_jMinDate = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        m_jMaxDate = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        m_jCash = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        m_jInitialAmount = new javax.swing.JTextField();
        jLabelInitialAmount = new javax.swing.JLabel();
        m_jCount = new javax.swing.JTextField();
        m_jLinesRemoved = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        m_jScrollTableTicket = new javax.swing.JScrollPane();
        m_jTicketTable = new javax.swing.JTable();
        jLabel9 = new javax.swing.JLabel();
        m_jNoCashSales = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        m_jSalesTotal = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        m_jSalesTaxes = new javax.swing.JTextField();
        m_jScrollSales = new javax.swing.JScrollPane();
        m_jsalestable = new javax.swing.JTable();
        m_jSales = new javax.swing.JTextField();
        m_jSalesSubtotal = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        m_jCloseCash = new javax.swing.JButton();
        m_jPrintCashPreview = new javax.swing.JButton();
        m_jPrintCash1 = new javax.swing.JButton();
        m_jReprintCash = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        jPanel1.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel11.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel11.setText(AppLocal.getIntString("label.sequence")); // NOI18N
        jLabel11.setPreferredSize(new java.awt.Dimension(125, 30));
        jPanel1.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 11, -1, -1));

        m_jSequence.setEditable(false);
        m_jSequence.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jSequence.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        m_jSequence.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(m_jSequence, new org.netbeans.lib.awtextra.AbsoluteConstraints(145, 12, -1, -1));

        jLabel2.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel2.setText(AppLocal.getIntString("label.StartDate")); // NOI18N
        jLabel2.setPreferredSize(new java.awt.Dimension(125, 30));
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 50, -1, -1));

        m_jMinDate.setEditable(false);
        m_jMinDate.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jMinDate.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        m_jMinDate.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(m_jMinDate, new org.netbeans.lib.awtextra.AbsoluteConstraints(145, 50, -1, -1));

        jLabel3.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel3.setText(AppLocal.getIntString("label.EndDate")); // NOI18N
        jLabel3.setPreferredSize(new java.awt.Dimension(125, 30));
        jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 50, 150, -1));

        m_jMaxDate.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jMaxDate.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        m_jMaxDate.setEnabled(false);
        m_jMaxDate.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(m_jMaxDate, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 50, -1, -1));

        jLabel5.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel5.setText(AppLocal.getIntString("label.sales")); // NOI18N
        jLabel5.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 90, -1, -1));

        m_jCash.setEditable(false);
        m_jCash.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jCash.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        m_jCash.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(m_jCash, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 320, -1, -1));

        jLabel4.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel4.setText(AppLocal.getIntString("label.cash")); // NOI18N
        jLabel4.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 320, -1, -1));

        m_jInitialAmount.setEditable(false);
        m_jInitialAmount.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jInitialAmount.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        m_jInitialAmount.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(m_jInitialAmount, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 290, -1, -1));

        jLabelInitialAmount.setFont(new java.awt.Font("Arial", Font.BOLD, 14)); // NOI18N
        jLabelInitialAmount.setText("💰 Fondo Inicial"); // NOI18N
        jLabelInitialAmount.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(jLabelInitialAmount, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 290, -1, -1));

        m_jCount.setEditable(false);
        m_jCount.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jCount.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        m_jCount.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(m_jCount, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 10, -1, -1));

        m_jLinesRemoved.setEditable(false);
        m_jLinesRemoved.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jLinesRemoved.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        m_jLinesRemoved.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(m_jLinesRemoved, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 360, -1, -1));

        jLabel1.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel1.setText(AppLocal.getIntString("label.Tickets")); // NOI18N
        jLabel1.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 10, -1, -1));
        jPanel1.add(jSeparator1, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 350, 315, 10));

        m_jScrollTableTicket.setBorder(null);
        m_jScrollTableTicket.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        m_jScrollTableTicket.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jScrollTableTicket.setMinimumSize(new java.awt.Dimension(350, 140));
        m_jScrollTableTicket.setPreferredSize(new java.awt.Dimension(325, 150));

        m_jTicketTable.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jTicketTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        m_jTicketTable.setFocusable(false);
        m_jTicketTable.setIntercellSpacing(new java.awt.Dimension(0, 1));
        m_jTicketTable.setRequestFocusEnabled(false);
        m_jScrollTableTicket.setViewportView(m_jTicketTable);

        jPanel1.add(m_jScrollTableTicket, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 95, -1, -1));

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("pos_messages"); // NOI18N
        
        jLabel9.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel9.setText(bundle.getString("label.linevoids")); // NOI18N
        jLabel9.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 360, -1, -1));

        m_jNoCashSales.setEditable(false);
        m_jNoCashSales.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jNoCashSales.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        m_jNoCashSales.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(m_jNoCashSales, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 400, -1, -1));

        jLabel8.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel8.setText(bundle.getString("label.nocashsales")); // NOI18N
        jLabel8.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 400, -1, -1));

        m_jSalesTotal.setEditable(false);
        m_jSalesTotal.setFont(new java.awt.Font("Arial", Font.BOLD, 14)); // NOI18N
        m_jSalesTotal.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        m_jSalesTotal.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(m_jSalesTotal, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 250, -1, -1));

        jLabel7.setFont(new java.awt.Font("Arial", Font.BOLD, 14)); // NOI18N
        jLabel7.setText(AppLocal.getIntString("label.total")); // NOI18N
        jLabel7.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 250, -1, -1));

        jLabel12.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel12.setText(AppLocal.getIntString("label.taxes")); // NOI18N
        jLabel12.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 170, -1, -1));

        m_jSalesTaxes.setEditable(false);
        m_jSalesTaxes.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jSalesTaxes.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        m_jSalesTaxes.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(m_jSalesTaxes, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 170, -1, -1));

        m_jScrollSales.setBorder(null);
        m_jScrollSales.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        m_jScrollSales.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jScrollSales.setPreferredSize(new java.awt.Dimension(325, 150));

        m_jsalestable.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jsalestable.setFocusable(false);
        m_jsalestable.setIntercellSpacing(new java.awt.Dimension(0, 1));
        m_jsalestable.setRequestFocusEnabled(false);
        m_jScrollSales.setViewportView(m_jsalestable);

        jPanel1.add(m_jScrollSales, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 250, -1, -1));

        m_jSales.setEditable(false);
        m_jSales.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jSales.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        m_jSales.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(m_jSales, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 90, -1, -1));

        m_jSalesSubtotal.setEditable(false);
        m_jSalesSubtotal.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jSalesSubtotal.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        m_jSalesSubtotal.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(m_jSalesSubtotal, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 130, -1, -1));

        jLabel6.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel6.setText(AppLocal.getIntString("label.totalnet")); // NOI18N
        jLabel6.setPreferredSize(new java.awt.Dimension(150, 30));
        jPanel1.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 130, -1, -1));

        m_jCloseCash.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jCloseCash.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/calculator.png"))); // NOI18N
        m_jCloseCash.setText(AppLocal.getIntString("button.closecash")); // NOI18N
        m_jCloseCash.setToolTipText(bundle.getString("tooltip.btn.closecash")); // NOI18N
        m_jCloseCash.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jCloseCash.setIconTextGap(2);
        m_jCloseCash.setInheritsPopupMenu(true);
        m_jCloseCash.setMaximumSize(new java.awt.Dimension(85, 33));
        m_jCloseCash.setMinimumSize(new java.awt.Dimension(85, 33));
        m_jCloseCash.setPreferredSize(new java.awt.Dimension(150, 45));
        m_jCloseCash.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jCloseCashActionPerformed(evt);
            }
        });
        jPanel1.add(m_jCloseCash, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 450, -1, -1));

        m_jPrintCashPreview.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jPrintCashPreview.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/printer.png"))); // NOI18N
        m_jPrintCashPreview.setText(AppLocal.getIntString("button.partialcash")); // NOI18N
        m_jPrintCashPreview.setToolTipText(AppLocal.getIntString("tooltip.btn.partialcash")); // NOI18N
        m_jPrintCashPreview.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jPrintCashPreview.setIconTextGap(2);
        m_jPrintCashPreview.setMaximumSize(new java.awt.Dimension(85, 33));
        m_jPrintCashPreview.setMinimumSize(new java.awt.Dimension(85, 33));
        m_jPrintCashPreview.setPreferredSize(new java.awt.Dimension(150, 45));
        m_jPrintCashPreview.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jPrintCashPreviewActionPerformed(evt);
            }
        });
        jPanel1.add(m_jPrintCashPreview, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 450, -1, -1));

        m_jPrintCash1.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jPrintCash1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/printer.png"))); // NOI18N
        m_jPrintCash1.setText(AppLocal.getIntString("button.closecashpreview")); // NOI18N
        m_jPrintCash1.setToolTipText(bundle.getString("tooltip.btn.closecashpreview")); // NOI18N
        m_jPrintCash1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jPrintCash1.setIconTextGap(2);
        m_jPrintCash1.setMaximumSize(new java.awt.Dimension(85, 33));
        m_jPrintCash1.setMinimumSize(new java.awt.Dimension(85, 33));
        m_jPrintCash1.setPreferredSize(new java.awt.Dimension(150, 45));
        m_jPrintCash1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jPrintCash1ActionPerformed(evt);
            }
        });
        jPanel1.add(m_jPrintCash1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 450, -1, -1));

        m_jReprintCash.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jReprintCash.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/printer.png"))); // NOI18N
        m_jReprintCash.setText(AppLocal.getIntString("button.closecashreprint")); // NOI18N
        m_jReprintCash.setToolTipText(bundle.getString("tooltip.btn.closecashreprint")); // NOI18N
        m_jReprintCash.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jReprintCash.setIconTextGap(2);
        m_jReprintCash.setMaximumSize(new java.awt.Dimension(85, 33));
        m_jReprintCash.setMinimumSize(new java.awt.Dimension(85, 33));
        m_jReprintCash.setPreferredSize(new java.awt.Dimension(150, 45));
        m_jReprintCash.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jReprintCashActionPerformed(evt);
            }
        });
        jPanel1.add(m_jReprintCash, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 450, -1, -1));

        add(jPanel1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void m_jCloseCashActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jCloseCashActionPerformed

        int res = JOptionPane.showConfirmDialog(this, 
                AppLocal.getIntString("message.wannaclosecash"), 
                AppLocal.getIntString("message.title"), 
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        if (res == JOptionPane.YES_OPTION) {

            String scriptId = "cash.close";
            try {
                //Fire cash.closed event
                ScriptEngine scriptEngine = ScriptFactory.getScriptEngine(ScriptFactory.BEANSHELL);
                DataLogicSystem dlSystem = (DataLogicSystem) m_App.getBean("com.openbravo.pos.forms.DataLogicSystem");
                String script = dlSystem.getResourceAsXML(scriptId);
                scriptEngine.eval(script);
            }
            catch (BeanFactoryException | ScriptException e) {
                LOGGER.log(Level.WARNING, "Exception on executing script: "+scriptId, e);
            }

            Date dNow = new Date();

            try {

                if (m_App.getActiveCashDateEnd() == null) {
                    new StaticSentence(m_App.getSession()
                        , "UPDATE closedcash SET DATEEND = ?, NOSALES = ? WHERE HOST = ? AND MONEY = ?"
                        , new SerializerWriteBasic(new Datas[] {
                            Datas.TIMESTAMP, 
                            Datas.INT, 
                            Datas.STRING, 
                            Datas.STRING}))
                    .exec(new Object[] {dNow, result, 
                        m_App.getProperties().getHost(), 
                        m_App.getActiveCashIndex()});
                }
            } catch (Exception e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, 
                        AppLocal.getIntString("message.cannotclosecash"), e);
                msg.show(this);
            }

            try {
                // Creamos una nueva caja
                m_App.setActiveCash(UUID.randomUUID().toString(), 
                        m_App.getActiveCashSequence() + 1, dNow, null);

                // creamos la caja activa
                m_dlSystem.execInsertCash(
                    new Object[] {m_App.getActiveCashIndex(), 
                        m_App.getProperties().getHost(), 
                        m_App.getActiveCashSequence(), 
                        m_App.getActiveCashDateStart(), 
                        m_App.getActiveCashDateEnd(),0.0});

                m_dlSystem.execDrawerOpened(
                    new Object[] {m_App.getAppUserView().getUser().getName(),"Close Cash"});

                // ponemos la fecha de fin
                m_PaymentsToClose.setDateEnd(dNow);

                // print report
                printPayments("Printer.CloseCash");

                // Mostramos el mensaje
                JOptionPane.showMessageDialog(this, 
                        AppLocal.getIntString("message.closecashok"), 
                        AppLocal.getIntString("message.title"), 
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (BasicException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, 
                        AppLocal.getIntString("message.cannotclosecash"), e);
                msg.show(this);
            }

            try {
                loadData();
            } catch (BasicException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, 
                        AppLocal.getIntString("label.noticketstoclose"), e);
                msg.show(this);
            }

        }
    }//GEN-LAST:event_m_jCloseCashActionPerformed

    private void m_jPrintCashPreviewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jPrintCashPreviewActionPerformed

        printPayments("Printer.PartialCash");

    }//GEN-LAST:event_m_jPrintCashPreviewActionPerformed

    private void m_jPrintCash1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jPrintCash1ActionPerformed
        printPayments("Printer.CloseCash.Preview"); 
    }//GEN-LAST:event_m_jPrintCash1ActionPerformed

    private void m_jReprintCashActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jReprintCashActionPerformed
        m_App.getAppUserView().showTask("com.openbravo.pos.panels.JPanelCloseMoneyReprint");
    }//GEN-LAST:event_m_jReprintCashActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelInitialAmount;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextField m_jCash;
    private javax.swing.JButton m_jCloseCash;
    private javax.swing.JTextField m_jCount;
    private javax.swing.JTextField m_jInitialAmount;
    private javax.swing.JTextField m_jLinesRemoved;
    private javax.swing.JTextField m_jMaxDate;
    private javax.swing.JTextField m_jMinDate;
    private javax.swing.JTextField m_jNoCashSales;
    private javax.swing.JButton m_jPrintCash1;
    private javax.swing.JButton m_jPrintCashPreview;
    private javax.swing.JButton m_jReprintCash;
    private javax.swing.JTextField m_jSales;
    private javax.swing.JTextField m_jSalesSubtotal;
    private javax.swing.JTextField m_jSalesTaxes;
    private javax.swing.JTextField m_jSalesTotal;
    private javax.swing.JScrollPane m_jScrollSales;
    private javax.swing.JScrollPane m_jScrollTableTicket;
    private javax.swing.JTextField m_jSequence;
    private javax.swing.JTable m_jTicketTable;
    private javax.swing.JTable m_jsalestable;
    // End of variables declaration//GEN-END:variables
    
    /**
     * Configura el diseño moderno de la interfaz con características avanzadas
     */
    private void setupModernUI() {
        // Configurar tema principal
        configurarTemaModerno();
        
        // Crear layout con cards
        crearLayoutConCards();
        
        // Modernizar todos los componentes
        modernizarComponentesAvanzados();
        
        // Agregar animaciones y efectos
        agregarEfectosVisuales();
        
        // Configurar responsive design
        configurarResponsiveDesign();
    }
    
    /**
     * Configura el tema moderno avanzado
     */
    private void configurarTemaModerno() {
        // Fondo principal con gradiente sutil
        if (jPanel1 != null) {
            jPanel1.setBackground(new Color(248, 250, 252));
            jPanel1.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)
            ));
        }
        
        // Configurar fuente principal
        try {
            Font modernaFont = new Font("Inter", Font.PLAIN, 14);
            UIManager.put("Label.font", modernaFont);
            UIManager.put("Button.font", modernaFont);
            UIManager.put("TextField.font", modernaFont);
        } catch (Exception e) {
            // Fallback a Segoe UI
            Font fallbackFont = new Font("Segoe UI", Font.PLAIN, 14);
            UIManager.put("Label.font", fallbackFont);
        }
    }
    
    /**
     * Moderniza los botones con efectos premium y gradientes
     */
    private void modernizarBotones() {
        // Botón cerrar caja - Estilo premium rojo
        if (m_jCloseCash != null) {
            crearBotonPremium(m_jCloseCash, 
                new Color(239, 68, 68), new Color(220, 38, 38), 
                "💰 " + AppLocal.getIntString("label.closecash"), 
                AppLocal.getIntString("label.finishshift"));
        }
        
        // Botón reporte parcial - Estilo premium azul
        if (m_jPrintCashPreview != null) {
            crearBotonPremium(m_jPrintCashPreview, 
                new Color(59, 130, 246), new Color(37, 99, 235), 
                "📊 " + AppLocal.getIntString("label.partialreport"), 
                AppLocal.getIntString("label.currentsalesview"));
        }
        
        // Botón vista previa - Estilo premium gris
        if (m_jPrintCash1 != null) {
            crearBotonPremium(m_jPrintCash1, 
                new Color(107, 114, 128), new Color(75, 85, 99), 
                "👁️ " + AppLocal.getIntString("label.preview"), 
                AppLocal.getIntString("label.previewreport"));
        }
        
        // Botón reimprimir - Estilo premium verde
        if (m_jReprintCash != null) {
            crearBotonPremium(m_jReprintCash, 
                new Color(34, 197, 94), new Color(22, 163, 74), 
                "🖨️ " + AppLocal.getIntString("label.reprint"), 
                AppLocal.getIntString("label.reprintlast"));
        }
    }
    
    /**
     * Crea un botón premium con efectos avanzados y mejor visibilidad de texto
     */
    private void crearBotonPremium(JButton boton, Color colorPrincipal, Color colorHover, String texto, String tooltip) {
        boton.setText(texto);
        boton.setToolTipText(tooltip);
        
        // Fuente más grande y legible
        boton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        boton.setForeground(Color.WHITE);
        boton.setBackground(colorPrincipal);
        
        // Tamaño mínimo para asegurar visibilidad del texto
        boton.setPreferredSize(new Dimension(180, 50));
        boton.setMinimumSize(new Dimension(160, 45));
        
        // Borde con mejor padding
        boton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(colorPrincipal.darker(), 1),
            BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));
        
        boton.setFocusPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setOpaque(true);
        
        // Centrar el texto correctamente
        boton.setHorizontalAlignment(SwingConstants.CENTER);
        boton.setVerticalAlignment(SwingConstants.CENTER);
        
        // Efectos premium con transiciones mejoradas
        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                boton.setBackground(colorHover);
                boton.setFont(new Font("Segoe UI", Font.BOLD, 16)); // Texto más grande en hover
                boton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(colorHover.darker(), 2),
                    BorderFactory.createEmptyBorder(11, 19, 11, 19)
                ));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                boton.setBackground(colorPrincipal);
                boton.setFont(new Font("Segoe UI", Font.BOLD, 15)); // Volver al tamaño normal
                boton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(colorPrincipal.darker(), 1),
                    BorderFactory.createEmptyBorder(12, 20, 12, 20)
                ));
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                boton.setBackground(colorHover.darker());
                // Efecto de "presionado"
                boton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(colorHover.darker(), 3),
                    BorderFactory.createEmptyBorder(10, 18, 10, 18)
                ));
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (boton.contains(e.getPoint())) {
                    boton.setBackground(colorHover);
                } else {
                    boton.setBackground(colorPrincipal);
                }
                boton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(colorPrincipal.darker(), 1),
                    BorderFactory.createEmptyBorder(12, 20, 12, 20)
                ));
            }
        });
    }
    
    /**
     * Moderniza los campos de texto con mejor visualización de números
     */
    private void modernizarCamposTexto() {
        JTextField[] campos = {
            m_jSequence, m_jMinDate, m_jMaxDate, m_jCount,
            m_jSales, m_jSalesSubtotal, m_jSalesTaxes, m_jSalesTotal,
            m_jCash, m_jLinesRemoved
        };
        
        for (JTextField campo : campos) {
            if (campo != null) {
                // Aplicar estilo mejorado para números
                if (esUnCampoNumerico(campo)) {
                    estilizarCampoNumerico(campo);
                } else {
                    estilizarCampoTexto(campo);
                }
            }
        }
    }
    
    /**
     * Aplica estilo premium a un campo de texto con mejor visibilidad
     */
    private void estilizarCampoTexto(JTextField campo) {
        campo.setFont(new Font("Inter", Font.PLAIN, 14));
        
        // Tamaño mínimo para asegurar visibilidad completa
        campo.setPreferredSize(new Dimension(120, 42));
        campo.setMinimumSize(new Dimension(100, 42));
        
        campo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        campo.setBackground(Color.WHITE);
        campo.setForeground(new Color(17, 24, 39));
        campo.setCaretColor(new Color(59, 130, 246));
        
        // Efectos de enfoque premium
        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                campo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(59, 130, 246), 2),
                    BorderFactory.createEmptyBorder(11, 15, 11, 15)
                ));
                campo.setBackground(new Color(248, 250, 252));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                campo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                    BorderFactory.createEmptyBorder(12, 16, 12, 16)
                ));
                campo.setBackground(Color.WHITE);
            }
        });
        
        // Efecto hover sutil
        campo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!campo.hasFocus()) {
                    campo.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(156, 163, 175), 1),
                        BorderFactory.createEmptyBorder(12, 16, 12, 16)
                    ));
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (!campo.hasFocus()) {
                    campo.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                        BorderFactory.createEmptyBorder(12, 16, 12, 16)
                    ));
                }
            }
        });
    }
    
    /**
     * Moderniza las tablas
     */
    private void modernizarTablas() {
        if (m_jTicketTable != null) {
            estilizarTabla(m_jTicketTable);
        }
        if (m_jsalestable != null) {
            estilizarTabla(m_jsalestable);
        }
        
        // Modernizar scroll panes
        if (m_jScrollTableTicket != null) {
            estilizarScrollPane(m_jScrollTableTicket, "💳 " + AppLocal.getIntString("label.paymentdetail"));
        }
        if (m_jScrollSales != null) {
            estilizarScrollPane(m_jScrollSales, "📊 " + AppLocal.getIntString("label.salesummary"));
        }
    }
    
    /**
     * Aplica estilo moderno a una tabla
     */
    private void estilizarTabla(JTable tabla) {
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setRowHeight(32);
        tabla.setShowGrid(true);
        tabla.setGridColor(new Color(243, 244, 246));
        tabla.setSelectionBackground(new Color(239, 246, 255));
        tabla.setSelectionForeground(new Color(30, 64, 175));
        tabla.setBackground(Color.WHITE);
        tabla.setForeground(new Color(17, 24, 39));
        
        // Encabezado moderno
        if (tabla.getTableHeader() != null) {
            tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
            tabla.getTableHeader().setBackground(new Color(249, 250, 251));
            tabla.getTableHeader().setForeground(new Color(55, 65, 81));
            tabla.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(229, 231, 235)));
            tabla.getTableHeader().setPreferredSize(new Dimension(0, 36));
        }
    }
    
    /**
     * Aplica estilo moderno a un scroll pane
     */
    private void estilizarScrollPane(JScrollPane scrollPane, String titulo) {
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            titulo,
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14),
            new Color(55, 65, 81)
        ));
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);
    }
    
    /**
     * Configura el layout principal
     */
    private void configurarLayoutPrincipal() {
        // Agregar efectos sutiles y espaciado
        if (getParent() != null) {
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        }
    }
    
    /**
     * Crea layout avanzado con sistema de cards
     */
    private void crearLayoutConCards() {
        // Implementar cards para secciones principales
        // Esto se puede expandir según necesidades específicas
        SwingUtilities.invokeLater(() -> {
            aplicarSombrasComponentes();
        });
    }
    
    /**
     * Moderniza todos los componentes con características avanzadas
     */
    private void modernizarComponentesAvanzados() {
        // Modernizar campos de texto
        modernizarCamposTexto();
        
        // Modernizar tablas
        modernizarTablas();
        
        // Modernizar botones
        modernizarBotones();
        
        // Agregar labels con iconos
        agregarIconosYLabels();
    }
    
    /**
     * Agrega efectos visuales y animaciones sutiles
     */
    private void agregarEfectosVisuales() {
        // Efectos de hover para paneles
        if (jPanel1 != null) {
            jPanel1.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    jPanel1.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                        BorderFactory.createEmptyBorder(24, 24, 24, 24)
                    ));
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    jPanel1.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                        BorderFactory.createEmptyBorder(24, 24, 24, 24)
                    ));
                }
            });
        }
    }
    
    /**
     * Configura diseño responsive
     */
    private void configurarResponsiveDesign() {
        // Agregar listener para redimensionamiento
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ajustarComponentesSegunTamano();
            }
        });
    }
    
    /**
     * Aplica sombras a los componentes principales
     */
    private void aplicarSombrasComponentes() {
        // Sombras para scroll panes
        if (m_jScrollTableTicket != null) {
            aplicarSombraPanel(m_jScrollTableTicket);
        }
        if (m_jScrollSales != null) {
            aplicarSombraPanel(m_jScrollSales);
        }
    }
    
    /**
     * Aplica efecto de sombra a un panel
     */
    private void aplicarSombraPanel(JComponent componente) {
        componente.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 3, new Color(0, 0, 0, 10)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
            )
        ));
    }
    
    /**
     * Agrega iconos y mejora los labels
     */
    private void agregarIconosYLabels() {
        // Buscar y mejorar labels existentes
        mejorarLabelsExistentes(this);
    }
    
    /**
     * Mejora recursivamente los labels en el contenedor
     */
    private void mejorarLabelsExistentes(Container contenedor) {
        for (Component comp : contenedor.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                label.setFont(new Font("Inter", Font.BOLD, 13));
                label.setForeground(new Color(55, 65, 81));
            } else if (comp instanceof Container) {
                mejorarLabelsExistentes((Container) comp);
            }
        }
    }
    
    /**
     * Ajusta componentes según el tamaño de la ventana
     */
    private void ajustarComponentesSegunTamano() {
        int ancho = getWidth();
        int alto = getHeight();
        
        // Ajustar fuentes según tamaño
        if (ancho < 800) {
            // Pantalla pequeña - fuentes más pequeñas
            ajustarFuentesParaPantallaChica();
        } else {
            // Pantalla normal - fuentes normales
            ajustarFuentesParaPantallaNormal();
        }
    }
    
    /**
     * Ajusta fuentes para pantalla pequeña
     */
    private void ajustarFuentesParaPantallaChica() {
        Font fuentePequena = new Font("Inter", Font.PLAIN, 12);
        aplicarFuenteAComponentes(this, fuentePequena);
    }
    
    /**
     * Ajusta fuentes para pantalla normal
     */
    private void ajustarFuentesParaPantallaNormal() {
        Font fuenteNormal = new Font("Inter", Font.PLAIN, 14);
        aplicarFuenteAComponentes(this, fuenteNormal);
    }
    
    /**
     * Aplica fuente recursivamente a todos los componentes
     */
    private void aplicarFuenteAComponentes(Container contenedor, Font fuente) {
        for (Component comp : contenedor.getComponents()) {
            if (comp instanceof JLabel || comp instanceof JTextField || comp instanceof JButton) {
                comp.setFont(fuente);
            } else if (comp instanceof Container) {
                aplicarFuenteAComponentes((Container) comp, fuente);
            }
        }
    }
    
    /**
     * Personaliza las scrollbars para un look más moderno
     */
    private void personalizarScrollBar(JScrollPane scrollPane) {
        try {
            // Personalizar scrollbar vertical
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setBackground(new Color(248, 250, 252));
            vertical.setPreferredSize(new Dimension(12, 0));
            
            // Personalizar scrollbar horizontal
            JScrollBar horizontal = scrollPane.getHorizontalScrollBar();
            horizontal.setBackground(new Color(248, 250, 252));
            horizontal.setPreferredSize(new Dimension(0, 12));
            
        } catch (Exception e) {
            // Ignorar errores de personalización
        }
    }
    
    /**
     * Verifica si un campo es numérico basándose en su nombre
     */
    private boolean esUnCampoNumerico(JTextField campo) {
        String nombre = campo.getName();
        if (nombre == null) return false;
        
        // Determinar si es un campo numérico por el nombre del componente
        return campo == m_jCount || campo == m_jSales || campo == m_jSalesSubtotal || 
               campo == m_jSalesTaxes || campo == m_jSalesTotal || campo == m_jCash || 
               campo == m_jLinesRemoved || campo == m_jSequence;
    }
    
    /**
     * Aplica estilo específico para campos numéricos con mejor visualización
     */
    private void estilizarCampoNumerico(JTextField campo) {
        // Fuente monoespaciada para mejor alineación de números
        campo.setFont(new Font("JetBrains Mono", Font.PLAIN, 14));
        
        // Si no está disponible JetBrains Mono, usar Consolas o monospace
        if (!campo.getFont().getFamily().equals("JetBrains Mono")) {
            campo.setFont(new Font("Consolas", Font.PLAIN, 14));
            if (!campo.getFont().getFamily().equals("Consolas")) {
                campo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            }
        }
        
        // Tamaño más amplio para números largos
        campo.setPreferredSize(new Dimension(160, 44));
        campo.setMinimumSize(new Dimension(140, 44));
        
        // Alineación a la derecha para números
        campo.setHorizontalAlignment(JTextField.RIGHT);
        
        campo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
            BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));
        
        campo.setBackground(new Color(249, 250, 251));
        campo.setForeground(new Color(17, 24, 39));
        campo.setCaretColor(new Color(59, 130, 246));
        
        // Efectos de enfoque para campos numéricos
        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                campo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(34, 197, 94), 2),
                    BorderFactory.createEmptyBorder(11, 19, 11, 19)
                ));
                campo.setBackground(new Color(240, 253, 244));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                campo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                    BorderFactory.createEmptyBorder(12, 20, 12, 20)
                ));
                campo.setBackground(new Color(249, 250, 251));
                
                // Formatear el número al perder el foco
                formatearNumero(campo);
            }
        });
        
        // Efecto hover para campos numéricos
        campo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!campo.hasFocus()) {
                    campo.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(34, 197, 94), 1),
                        BorderFactory.createEmptyBorder(12, 20, 12, 20)
                    ));
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (!campo.hasFocus()) {
                    campo.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                        BorderFactory.createEmptyBorder(12, 20, 12, 20)
                    ));
                }
            }
        });
    }
    
    /**
     * Formatea un número en el campo de texto para mejor visualización
     */
    private void formatearNumero(JTextField campo) {
        try {
            String texto = campo.getText().trim();
            if (!texto.isEmpty() && !texto.equals("-")) {
                // Intentar formatear como número decimal
                double numero = Double.parseDouble(texto);
                
                // Formatear según el tipo de campo
                if (campo == m_jCount || campo == m_jLinesRemoved || campo == m_jSequence) {
                    // Campos enteros
                    campo.setText(String.format("%,d", (long)numero));
                } else {
                    // Campos decimales (dinero)
                    campo.setText(String.format("%,.2f", numero));
                }
            }
        } catch (NumberFormatException e) {
            // Si no es un número válido, dejar como está
        }
    }
    
}
