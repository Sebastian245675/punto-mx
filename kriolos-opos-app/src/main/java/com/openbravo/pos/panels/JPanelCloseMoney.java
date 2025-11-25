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
import javax.swing.DefaultListModel;

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
    
    // Clase para almacenar el resultado de la validación de dinero físico
    private static class ValidacionDineroResult {
        boolean valido;
        double faltante;
        double sobrante;
        
        ValidacionDineroResult(boolean valido, double faltante, double sobrante) {
            this.valido = valido;
            this.faltante = faltante;
            this.sobrante = sobrante;
        }
    }
    
    // Variables para almacenar faltante y sobrante del cierre actual
    private double faltanteCierre = 0.0;
    private double sobranteCierre = 0.0;    
    private String SQL;
    private ResultSet rs;
    
    private AppUser m_User;
    
    
    /** Creates new form JPanelCloseMoney */
    public JPanelCloseMoney() {
        // Inicializar componentes básicos primero
        initBasicComponents();
        // Crear el diseño moderno
        createModernLayout();
    }
    
    /**
     * Inicializa solo los componentes básicos necesarios
     */
    private void initBasicComponents() {
        // Inicializar componentes que se usan en otros métodos
        m_jSequence = new JTextField();
        m_jMinDate = new JTextField();
        m_jMaxDate = new JTextField();
        m_jCash = new JTextField();
        m_jInitialAmount = new JTextField();
        m_jCount = new JTextField();
        m_jLinesRemoved = new JTextField();
        m_jNoCashSales = new JTextField();
        m_jSalesTotal = new JTextField();
        m_jSalesTaxes = new JTextField();
        m_jSales = new JTextField();
        m_jSalesSubtotal = new JTextField();
        m_jTicketTable = new JTable();
        m_jsalestable = new JTable();
        m_jScrollTableTicket = new JScrollPane();
        m_jScrollSales = new JScrollPane();
        m_jCloseCash = new JButton();
        m_jPrintCashPreview = new JButton();
        m_jPrintCash1 = new JButton();
        m_jReprintCash = new JButton();
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
        // Verificar y crear las columnas faltante_cierre y sobrante_cierre si no existen
        verificarColumnasFaltanteSobrante();
        
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
            
            // Obtener el monto inicial de la caja activa actual directamente desde la BD
            try {
                Double initialAmount = null;
                String activeCashIndex = m_App.getActiveCashIndex();
                s = m_App.getSession();
                con = s.getConnection();
                
                // Consultar directamente desde la BD para asegurar que obtenemos el valor correcto
                SQL = "SELECT INITIAL_AMOUNT FROM CLOSEDCASH WHERE MONEY = ? AND DATEEND IS NULL";
                
                java.sql.PreparedStatement pstmt = con.prepareStatement(SQL);
                pstmt.setString(1, activeCashIndex);
                
                ResultSet rsInitial = pstmt.executeQuery();
                if (rsInitial.next()) {
                    double dbInitialAmount = rsInitial.getDouble("INITIAL_AMOUNT");
                    if (!rsInitial.wasNull()) {
                        initialAmount = dbInitialAmount;
                        LOGGER.info("Fondo inicial obtenido desde BD en loadData: " + initialAmount);
                    } else {
                        LOGGER.warning("INITIAL_AMOUNT es NULL en la BD para MONEY: " + activeCashIndex);
                    }
                } else {
                    LOGGER.warning("No se encontró registro en CLOSEDCASH para MONEY: " + activeCashIndex);
                }
                
                if (rsInitial != null) rsInitial.close();
                if (pstmt != null) pstmt.close();
                
                // Si no se encontró en la BD, intentar desde PaymentsModel como respaldo
                if (initialAmount == null) {
                    try {
                        initialAmount = m_PaymentsToClose.getInitialAmount();
                        if (initialAmount != null && initialAmount > 0.0) {
                            LOGGER.info("Fondo inicial obtenido desde PaymentsModel en loadData: " + initialAmount);
                        } else {
                            // Intentar desde getActiveCashInitialAmount como último recurso
                            try {
                                initialAmount = m_App.getActiveCashInitialAmount();
                                LOGGER.info("Fondo inicial obtenido desde getActiveCashInitialAmount() en loadData: " + initialAmount);
                            } catch (Exception e) {
                                LOGGER.log(Level.WARNING, "Error obteniendo fondo inicial desde getActiveCashInitialAmount() en loadData: " + e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error obteniendo fondo inicial desde PaymentsModel en loadData: " + e.getMessage());
                    }
                }
                
                // Establecer el valor en el campo (usar 0.0 si aún es null)
                if (initialAmount == null) {
                    initialAmount = 0.0;
                    LOGGER.warning("Fondo inicial no encontrado en loadData, usando 0.0 por defecto");
                }
                // Redondear a 2 decimales para evitar mostrar ceros innecesarios
                double initialAmountRounded = Math.round(initialAmount * 100.0) / 100.0;
                m_jInitialAmount.setText(Formats.CURRENCY.formatValue(initialAmountRounded));
                LOGGER.info("Fondo inicial final establecido en campo m_jInitialAmount: " + initialAmountRounded);
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error obteniendo monto inicial en loadData: " + e.getMessage(), e);
                // Intentar obtener desde PaymentsModel como último recurso
                try {
                    Double fallbackAmount = m_PaymentsToClose.getInitialAmount();
                    if (fallbackAmount != null) {
                        // Redondear a 2 decimales
                        double fallbackRounded = Math.round(fallbackAmount * 100.0) / 100.0;
                        m_jInitialAmount.setText(Formats.CURRENCY.formatValue(fallbackRounded));
                        LOGGER.info("Fondo inicial establecido desde fallback en loadData: " + fallbackRounded);
                    } else {
                        m_jInitialAmount.setText(Formats.CURRENCY.formatValue(0.0));
                    }
                } catch (Exception ex) {
                    m_jInitialAmount.setText(Formats.CURRENCY.formatValue(0.0));
                }
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
        
        // Actualizar componentes del diseño moderno
        updateModernLayoutData();
    }
    
    /**
     * Actualiza los componentes del diseño moderno con los datos reales
     */
    private void updateModernLayoutData() {
        if (m_PaymentsToClose == null) {
            return;
        }
        
        try {
            // Actualizar información del turno
            if (m_jShiftInfoLabel != null) {
                String userName = m_PaymentsToClose.getUser();
                String dateStart = m_PaymentsToClose.printDateStart();
                String dateEnd = m_PaymentsToClose.printDateEnd();
                String timeRange = "";
                
                if (dateStart != null && !dateStart.isEmpty()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
                    try {
                        Date startDate = sdf.parse(dateStart);
                        String startTime = timeFormat.format(startDate);
                        if (dateEnd != null && !dateEnd.isEmpty()) {
                            Date endDate = sdf.parse(dateEnd);
                            String endTime = timeFormat.format(endDate);
                            timeRange = String.format("De %s a %s - (Turno Actual)", startTime, endTime);
                        } else {
                            timeRange = String.format("De %s - (Turno Actual)", startTime);
                        }
                    } catch (ParseException e) {
                        timeRange = dateStart;
                    }
                }
                
                String shiftInfo = String.format("Corte de %s iniciado el %s", userName, dateStart != null ? dateStart.split(" ")[0] : "");
                if (!timeRange.isEmpty()) {
                    shiftInfo += "\n" + timeRange;
                }
                m_jShiftInfoLabel.setText(shiftInfo);
            }
            
            // Actualizar tarjetas de métricas
            if (m_jSalesTotalLabel != null) {
                String salesTotal = m_PaymentsToClose.printSalesTotal();
                m_jSalesTotalLabel.setText(salesTotal != null ? salesTotal : "$0.00");
            }
            
            if (m_jProfitLabel != null) {
                // Calcular ganancia real (ventas totales - costo de compra)
                double profit = 0.0;
                try {
                    String activeCashIndex = m_App.getActiveCashIndex();
                    Session session = m_App.getSession();
                    Connection conn = session.getConnection();
                    
                    // Consulta para calcular ganancia real: SUM((precio_venta - precio_compra) * unidades)
                    String profitSql = "SELECT SUM((ticketlines.PRICE - COALESCE(products.PRICEBUY, 0)) * ticketlines.UNITS) " +
                                      "FROM ticketlines " +
                                      "INNER JOIN receipts ON ticketlines.TICKET = receipts.ID " +
                                      "LEFT JOIN products ON ticketlines.PRODUCT = products.ID " +
                                      "WHERE receipts.MONEY = ? AND ticketlines.PRODUCT IS NOT NULL";
                    
                    java.sql.PreparedStatement pstmt = conn.prepareStatement(profitSql);
                    pstmt.setString(1, activeCashIndex);
                    
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        double dbProfit = rs.getDouble(1);
                        if (!rs.wasNull()) {
                            profit = dbProfit;
                            LOGGER.info("Ganancia calculada desde BD: " + profit);
                        }
                    }
                    
                    if (rs != null) rs.close();
                    if (pstmt != null) pstmt.close();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error calculando ganancia desde BD, usando cálculo alternativo: " + e.getMessage());
                    // Fallback: intentar calcular desde salesBase si hay error
                    try {
                        String salesBaseStr = m_PaymentsToClose.printSalesBase();
                        if (salesBaseStr != null && !salesBaseStr.isEmpty()) {
                            double salesBase = Formats.CURRENCY.parseValue(salesBaseStr);
                            // Si no podemos obtener el costo, asumir 50% de ganancia como fallback
                            profit = salesBase * 0.5;
                            LOGGER.warning("Usando cálculo de ganancia aproximado (50%): " + profit);
                        }
                    } catch (BasicException ex) {
                        LOGGER.log(Level.WARNING, "Error en cálculo alternativo de ganancia: " + ex.getMessage());
                        profit = 0.0;
                    }
                }
                
                m_jProfitLabel.setText(Formats.CURRENCY.formatValue(profit));
            }
            
            // Actualizar sección Dinero en Caja
            // Obtener fondo inicial directamente desde la BD
            double initialAmount = 0.0;
            try {
                String activeCashIndex = m_App.getActiveCashIndex();
                Session session = m_App.getSession();
                Connection conn = session.getConnection();
                
                String sql = "SELECT INITIAL_AMOUNT FROM CLOSEDCASH WHERE MONEY = ? AND DATEEND IS NULL";
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, activeCashIndex);
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    double dbAmount = rs.getDouble("INITIAL_AMOUNT");
                    if (!rs.wasNull()) {
                        initialAmount = dbAmount;
                    }
                }
                
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo fondo inicial para diseño moderno: " + e.getMessage());
                // Fallback
                if (m_PaymentsToClose != null && m_PaymentsToClose.getInitialAmount() != null) {
                    initialAmount = m_PaymentsToClose.getInitialAmount();
                }
            }
            
            // Actualizar label de fondo de caja
            if (m_jInitialAmountLabel != null) {
                // Redondear a 2 decimales para evitar mostrar ceros innecesarios
                double initialAmountRounded = Math.round(initialAmount * 100.0) / 100.0;
                m_jInitialAmountLabel.setText(Formats.CURRENCY.formatValue(initialAmountRounded));
            }
            
            // Actualizar ventas en efectivo
            if (m_jCashSalesLabel != null && m_PaymentsToClose != null) {
                double cashSales = m_PaymentsToClose.getCashTotal() != null ? m_PaymentsToClose.getCashTotal() : 0.0;
                m_jCashSalesLabel.setText("+" + Formats.CURRENCY.formatValue(cashSales));
            }
            
            // Actualizar total
            if (m_jCashTotalLabel != null) {
                double cashTotal = m_PaymentsToClose.getCashTotal() != null ? m_PaymentsToClose.getCashTotal() : 0.0;
                double total = initialAmount + cashTotal;
                m_jCashTotalLabel.setText(Formats.CURRENCY.formatValue(total));
            }
            
            // Actualizar listas de entradas y salidas
            updateCashMovements();
            
            // Actualizar ventas por departamento
            updateDepartmentSales();
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error actualizando diseño moderno: " + e.getMessage(), e);
        }
    }
    
    /**
     * Actualiza las listas de movimientos de efectivo (entradas y salidas)
     */
    private void updateCashMovements() {
        if (m_PaymentsToClose == null || m_inflowsListModel == null || m_outflowsListModel == null) {
            return;
        }
        
        try {
            m_inflowsListModel.clear();
            m_outflowsListModel.clear();
            
            // Obtener entradas y salidas de efectivo desde draweropened
            s = m_App.getSession();
            con = s.getConnection();
            String sdbmanager = m_dlSystem.getDBVersion();
            
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            
            // Buscar entradas de dinero (TICKETID que no sea 'No Sale' y tenga información de entrada)
            // Por ahora, usar draweropened con TICKETID específico para entradas
            String sqlInflows = "SELECT OPENDATE, NAME, TICKETID FROM draweropened " +
                                "WHERE TICKETID LIKE 'Entrada%' AND OPENDATE > {fn TIMESTAMP('" + 
                                m_PaymentsToClose.getDateStartDerby() + "')} " +
                                "ORDER BY OPENDATE DESC";
            
            stmt = con.createStatement();
            rs = stmt.executeQuery(sqlInflows);
            while (rs.next()) {
                Date openDate = rs.getTimestamp("OPENDATE");
                String name = rs.getString("NAME");
                String ticketId = rs.getString("TICKETID");
                String timeStr = timeFormat.format(openDate);
                // Extraer monto si está en el TICKETID o NAME
                String item = String.format("%s %s", timeStr, name != null ? name : ticketId);
                m_inflowsListModel.addElement(item);
            }
            rs.close();
            
            // Buscar salidas de dinero
            String sqlOutflows = "SELECT OPENDATE, NAME, TICKETID FROM draweropened " +
                                "WHERE TICKETID LIKE 'Salida%' AND OPENDATE > {fn TIMESTAMP('" + 
                                m_PaymentsToClose.getDateStartDerby() + "')} " +
                                "ORDER BY OPENDATE DESC";
            
            rs = stmt.executeQuery(sqlOutflows);
            while (rs.next()) {
                Date openDate = rs.getTimestamp("OPENDATE");
                String name = rs.getString("NAME");
                String ticketId = rs.getString("TICKETID");
                String timeStr = timeFormat.format(openDate);
                String item = String.format("%s %s", timeStr, name != null ? name : ticketId);
                m_outflowsListModel.addElement(item);
            }
            rs.close();
            stmt.close();
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error actualizando movimientos de efectivo: " + e.getMessage(), e);
        }
    }
    
    /**
     * Actualiza la lista de ventas por departamento
     */
    private void updateDepartmentSales() {
        if (m_PaymentsToClose == null || m_deptSalesListModel == null) {
            return;
        }
        
        try {
            m_deptSalesListModel.clear();
            
            // Obtener ventas por categoría/departamento
            java.util.List categorySales = m_PaymentsToClose.getCategorySalesLines();
            if (categorySales != null) {
                for (Object obj : categorySales) {
                    PaymentsModel.CategorySalesLine category = (PaymentsModel.CategorySalesLine) obj;
                    String deptName = category.printCategoryName();
                    String totalStr = category.printCategorySum();
                    String item = String.format("%s: %s", deptName, totalStr);
                    m_deptSalesListModel.addElement(item);
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error actualizando ventas por departamento: " + e.getMessage(), e);
        }
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
    
    /**
     * Valida el dinero físico en caja antes de cerrar
     * Calcula el dinero esperado (fondo inicial + efectivo recibido) y lo compara con el dinero físico ingresado
     * @return true si el dinero es correcto o sobra, false si falta dinero
     */
    private ValidacionDineroResult validarDineroFisicoEnCaja() {
        try {
            LOGGER.info("=== Iniciando validación de dinero físico en caja ===");
            
            // Obtener fondo inicial directamente de la base de datos para la caja activa
            double fondoInicial = 0.0;
            try {
                String activeCashIndex = m_App.getActiveCashIndex();
                Session session = m_App.getSession();
                Connection conn = session.getConnection();
                
                String sql = "SELECT INITIAL_AMOUNT FROM CLOSEDCASH WHERE MONEY = ? AND DATEEND IS NULL";
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, activeCashIndex);
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    double dbAmount = rs.getDouble("INITIAL_AMOUNT");
                    if (!rs.wasNull()) {
                        fondoInicial = dbAmount;
                        LOGGER.info("Validación: Fondo inicial obtenido desde BD: " + fondoInicial);
                    } else {
                        LOGGER.warning("Validación: INITIAL_AMOUNT es NULL en BD");
                    }
                } else {
                    LOGGER.warning("Validación: No se encontró registro en CLOSEDCASH para MONEY: " + activeCashIndex);
                }
                
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Validación: Error obteniendo fondo inicial desde BD, usando fallback: " + e.getMessage());
                // Fallback a PaymentsModel
                if (m_PaymentsToClose != null && m_PaymentsToClose.getInitialAmount() != null) {
                    fondoInicial = m_PaymentsToClose.getInitialAmount();
                    LOGGER.info("Validación: Usando fondo inicial desde PaymentsModel: " + fondoInicial);
                }
            }
            
            double efectivoRecibido = m_PaymentsToClose.getCashTotal() != null ? m_PaymentsToClose.getCashTotal() : 0.0;
            double dineroEsperado = fondoInicial + efectivoRecibido;
            
            LOGGER.info(String.format("Validación dinero: Fondo inicial=%.2f, Efectivo recibido=%.2f, Esperado=%.2f",
                fondoInicial, efectivoRecibido, dineroEsperado));
            
            // Crear diálogo personalizado mejorado para ingresar dinero físico
            String inputDineroFisico = mostrarDialogoDineroFisico(fondoInicial, efectivoRecibido, dineroEsperado);
            
            // Si el usuario cancela, no cerrar la caja
            if (inputDineroFisico == null || inputDineroFisico.trim().isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "El cierre de caja fue cancelado.\nDebe ingresar el dinero físico para continuar.",
                    "Cierre Cancelado",
                    JOptionPane.WARNING_MESSAGE
                );
                return new ValidacionDineroResult(false, 0.0, 0.0);
            }
            
            // Validar que el input sea un número válido
            double dineroFisico;
            try {
                // Usar Formats.CURRENCY.parseValue() que sabe cómo parsear valores formateados como moneda
                dineroFisico = Formats.CURRENCY.parseValue(inputDineroFisico);
            } catch (BasicException e) {
                // Si falla el parseo con Formats, intentar limpiar y parsear manualmente
                try {
                    // Eliminar símbolos de moneda, espacios y reemplazar comas por puntos
                    String inputLimpio = inputDineroFisico.trim()
                        .replace("$", "")
                        .replace(",", "")
                        .replace(" ", "");
                    dineroFisico = Double.parseDouble(inputLimpio);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(
                        this,
                        "❌ Error: Debe ingresar un número válido.\n\n" +
                        "Ejemplo: 5000 o 5000.50\n" +
                        "El valor ingresado fue: " + inputDineroFisico,
                        "Error de Validación",
                        JOptionPane.ERROR_MESSAGE
                    );
                    LOGGER.log(Level.WARNING, "Error parseando dinero físico: " + inputDineroFisico, ex);
                    return new ValidacionDineroResult(false, 0.0, 0.0);
                }
            }
            
            // Comparar dinero físico con dinero esperado
            double diferencia = dineroFisico - dineroEsperado;
            double tolerancia = 0.01; // Tolerancia de 1 centavo para errores de redondeo
            
            // Inicializar faltante y sobrante
            double faltante = 0.0;
            double sobrante = 0.0;
            String mensajeCierre = "";
            
            if (diferencia < -tolerancia) {
                // Falta dinero - mostrar advertencia pero permitir continuar
                faltante = Math.abs(diferencia);
                int respuesta = mostrarDialogoFaltanteDinero(
                    fondoInicial, efectivoRecibido, dineroEsperado, dineroFisico, faltante
                );
                
                if (respuesta != JOptionPane.YES_OPTION) {
                    return new ValidacionDineroResult(false, faltante, 0.0);
                }
                
                mensajeCierre = String.format(
                    "Cierre de caja realizado.\n\n" +
                    "Se detectó un faltante de: %s",
                    Formats.CURRENCY.formatValue(faltante)
                );
                
                LOGGER.warning(String.format(
                    "Cierre de caja con dinero faltante: Esperado=%.2f, Físico=%.2f, Falta=%.2f",
                    dineroEsperado, dineroFisico, faltante
                ));
            } else if (diferencia > tolerancia) {
                // Sobra dinero (se permite pero se informa) - mostrar desglose completo
                sobrante = diferencia;
                int respuesta = mostrarDialogoSobranteDinero(
                    fondoInicial, efectivoRecibido, dineroEsperado, dineroFisico, sobrante
                );
                
                if (respuesta != JOptionPane.YES_OPTION) {
                    return new ValidacionDineroResult(false, 0.0, sobrante);
                }
                
                mensajeCierre = String.format(
                    "Cierre de caja realizado.\n\n" +
                    "Se detectó un sobrante de: %s",
                    Formats.CURRENCY.formatValue(sobrante)
                );
                
                LOGGER.info(String.format(
                    "Cierre de caja con dinero sobrante: Esperado=%.2f, Físico=%.2f, Sobra=%.2f",
                    dineroEsperado, dineroFisico, sobrante
                ));
            } else {
                // Dinero correcto (dentro de la tolerancia) - mensaje simple
                mensajeCierre = "Cierre de caja realizado correctamente.";
                LOGGER.info(String.format(
                    "Validación de dinero exitosa: Esperado=%.2f, Físico=%.2f",
                    dineroEsperado, dineroFisico
                ));
            }
            
            // Guardar faltante y sobrante en variables de instancia
            faltanteCierre = faltante;
            sobranteCierre = sobrante;
            
            // Mostrar mensaje de cierre realizado
            JOptionPane.showMessageDialog(
                this,
                mensajeCierre,
                "Cierre de Caja",
                JOptionPane.INFORMATION_MESSAGE
            );
            
            return new ValidacionDineroResult(true, faltante, sobrante);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error validando dinero físico en caja", e);
            JOptionPane.showMessageDialog(
                this,
                "Error al validar el dinero en caja.\n\n¿Desea continuar con el cierre de todos modos?",
                "Error de Validación",
                JOptionPane.ERROR_MESSAGE
            );
            // En caso de error, permitir continuar después de confirmar
            int respuesta = JOptionPane.showConfirmDialog(
                this,
                "Hubo un error al validar el dinero. ¿Desea continuar con el cierre de caja?",
                "Continuar con Cierre",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (respuesta == JOptionPane.YES_OPTION) {
                faltanteCierre = 0.0;
                sobranteCierre = 0.0;
                return new ValidacionDineroResult(true, 0.0, 0.0);
            } else {
                return new ValidacionDineroResult(false, 0.0, 0.0);
            }
        }
    }
    
    /**
     * Muestra un diálogo mejorado para ingresar el dinero físico en caja
     * @param fondoInicial El fondo inicial de la caja
     * @param efectivoRecibido El efectivo recibido por ventas
     * @param dineroEsperado El dinero total esperado en caja
     * @return El valor ingresado por el usuario, o null si canceló
     */
    private String mostrarDialogoDineroFisico(double fondoInicial, double efectivoRecibido, double dineroEsperado) {
        // Crear un panel personalizado con mejor diseño
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);
        
        // Título
        JLabel titulo = new JLabel("💰 Validar Dinero en Caja");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setForeground(new Color(30, 30, 30));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        panel.add(titulo);
        
        // Panel de información con fondo gris claro
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(new Color(249, 250, 251));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // Fondo inicial
        JPanel fondoPanel = crearLineaInfo("Fondo Inicial", fondoInicial, new Color(59, 130, 246));
        infoPanel.add(fondoPanel);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Efectivo recibido
        JPanel efectivoPanel = crearLineaInfo("Efectivo Recibido", efectivoRecibido, new Color(34, 197, 94));
        infoPanel.add(efectivoPanel);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Separador
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(229, 231, 235));
        infoPanel.add(separator);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Total esperado (destacado)
        JPanel totalPanel = crearLineaInfo("Total Esperado", dineroEsperado, new Color(30, 30, 30), true);
        infoPanel.add(totalPanel);
        
        panel.add(infoPanel);
        panel.add(Box.createVerticalStrut(20));
        
        // Campo de entrada
        JLabel labelInput = new JLabel("Ingrese la cantidad de dinero físico que tiene en caja:");
        labelInput.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        labelInput.setForeground(new Color(50, 50, 50));
        labelInput.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(labelInput);
        panel.add(Box.createVerticalStrut(10));
        
        JTextField campoDinero = new JTextField();
        campoDinero.setFont(new Font("Segoe UI", Font.BOLD, 16));
        campoDinero.setHorizontalAlignment(JTextField.RIGHT);
        campoDinero.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 2),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        campoDinero.setBackground(Color.WHITE);
        campoDinero.setPreferredSize(new Dimension(300, 45));
        campoDinero.setMaximumSize(new Dimension(300, 45));
        campoDinero.setText(Formats.CURRENCY.formatValue(dineroEsperado)); // Pre-llenar con el total esperado
        campoDinero.selectAll();
        campoDinero.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                campoDinero.selectAll();
            }
        });
        panel.add(campoDinero);
        
        // Mostrar el diálogo
        int resultado = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Validar Dinero en Caja",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (resultado == JOptionPane.OK_OPTION) {
            return campoDinero.getText();
        } else {
            return null;
        }
    }
    
    /**
     * Crea una línea de información para el diálogo
     */
    private JPanel crearLineaInfo(String label, double valor, Color color) {
        return crearLineaInfo(label, valor, color, false);
    }
    
    /**
     * Crea una línea de información para el diálogo
     */
    private JPanel crearLineaInfo(String label, double valor, Color color, boolean destacado) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(249, 250, 251));
        panel.setOpaque(false);
        
        JLabel labelComp = new JLabel(label + ":");
        labelComp.setFont(new Font("Segoe UI", destacado ? Font.BOLD : Font.PLAIN, destacado ? 15 : 14));
        labelComp.setForeground(new Color(75, 85, 99));
        panel.add(labelComp, BorderLayout.WEST);
        
        JLabel valorComp = new JLabel(Formats.CURRENCY.formatValue(valor));
        valorComp.setFont(new Font("Segoe UI", Font.BOLD, destacado ? 16 : 15));
        valorComp.setForeground(color);
        panel.add(valorComp, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * Muestra un diálogo mejorado para advertencia de dinero faltante
     */
    private int mostrarDialogoFaltanteDinero(double fondoInicial, double efectivoRecibido, 
                                               double dineroEsperado, double dineroFisico, double faltante) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);
        
        // Título con icono de advertencia
        JPanel tituloPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tituloPanel.setBackground(Color.WHITE);
        tituloPanel.setOpaque(false);
        
        JLabel icono = new JLabel("⚠️");
        icono.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        tituloPanel.add(icono);
        
        JLabel titulo = new JLabel(" ADVERTENCIA: FALTA DINERO");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setForeground(new Color(220, 38, 38)); // Rojo
        tituloPanel.add(titulo);
        
        panel.add(tituloPanel);
        panel.add(Box.createVerticalStrut(15));
        
        // Panel de información con fondo rojo claro
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(new Color(254, 242, 242));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 38, 38), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // Fondo inicial
        JPanel fondoPanel = crearLineaInfoAdvertencia("Fondo Inicial", fondoInicial, new Color(75, 85, 99));
        infoPanel.add(fondoPanel);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Efectivo recibido
        JPanel efectivoPanel = crearLineaInfoAdvertencia("Efectivo Recibido", efectivoRecibido, new Color(75, 85, 99));
        infoPanel.add(efectivoPanel);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Separador
        JSeparator separator1 = new JSeparator();
        separator1.setForeground(new Color(220, 38, 38));
        infoPanel.add(separator1);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Debe haber en caja
        JPanel debePanel = crearLineaInfoAdvertencia("DEBE HABER EN CAJA", dineroEsperado, new Color(30, 30, 30), true);
        infoPanel.add(debePanel);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Tiene en caja
        JPanel tienePanel = crearLineaInfoAdvertencia("TIENE EN CAJA", dineroFisico, new Color(75, 85, 99));
        infoPanel.add(tienePanel);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Separador
        JSeparator separator2 = new JSeparator();
        separator2.setForeground(new Color(220, 38, 38));
        infoPanel.add(separator2);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Falta en caja (destacado en rojo)
        JPanel faltaPanel = crearLineaInfoAdvertencia("FALTA EN CAJA", faltante, new Color(220, 38, 38), true);
        infoPanel.add(faltaPanel);
        
        panel.add(infoPanel);
        panel.add(Box.createVerticalStrut(20));
        
        // Mensaje de confirmación
        JLabel mensaje = new JLabel("<html><div style='text-align: center;'>" +
            "¿Desea continuar con el cierre de caja<br/>de todos modos?</div></html>");
        mensaje.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        mensaje.setForeground(new Color(50, 50, 50));
        mensaje.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(mensaje);
        
        return JOptionPane.showConfirmDialog(
            this,
            panel,
            "Advertencia: Falta Dinero",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
    }
    
    /**
     * Muestra un diálogo mejorado para advertencia de dinero sobrante
     */
    private int mostrarDialogoSobranteDinero(double fondoInicial, double efectivoRecibido, 
                                               double dineroEsperado, double dineroFisico, double sobrante) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);
        
        // Título con icono de advertencia
        JPanel tituloPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tituloPanel.setBackground(Color.WHITE);
        tituloPanel.setOpaque(false);
        
        JLabel icono = new JLabel("⚠️");
        icono.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        tituloPanel.add(icono);
        
        JLabel titulo = new JLabel(" ADVERTENCIA: SOBRA DINERO");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setForeground(new Color(234, 179, 8)); // Amarillo/Naranja
        tituloPanel.add(titulo);
        
        panel.add(tituloPanel);
        panel.add(Box.createVerticalStrut(15));
        
        // Panel de información con fondo amarillo claro
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(new Color(255, 251, 235));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(234, 179, 8), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // Fondo inicial
        JPanel fondoPanel = crearLineaInfoAdvertencia("Fondo Inicial", fondoInicial, new Color(75, 85, 99));
        infoPanel.add(fondoPanel);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Efectivo recibido
        JPanel efectivoPanel = crearLineaInfoAdvertencia("Efectivo Recibido", efectivoRecibido, new Color(75, 85, 99));
        infoPanel.add(efectivoPanel);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Separador
        JSeparator separator1 = new JSeparator();
        separator1.setForeground(new Color(234, 179, 8));
        infoPanel.add(separator1);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Debe haber en caja
        JPanel debePanel = crearLineaInfoAdvertencia("DEBE HABER EN CAJA", dineroEsperado, new Color(30, 30, 30), true);
        infoPanel.add(debePanel);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Tiene en caja
        JPanel tienePanel = crearLineaInfoAdvertencia("TIENE EN CAJA", dineroFisico, new Color(75, 85, 99));
        infoPanel.add(tienePanel);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Separador
        JSeparator separator2 = new JSeparator();
        separator2.setForeground(new Color(234, 179, 8));
        infoPanel.add(separator2);
        infoPanel.add(Box.createVerticalStrut(8));
        
        // Sobra en caja (destacado en amarillo/naranja)
        JPanel sobraPanel = crearLineaInfoAdvertencia("SOBRA EN CAJA", sobrante, new Color(234, 179, 8), true);
        infoPanel.add(sobraPanel);
        
        panel.add(infoPanel);
        panel.add(Box.createVerticalStrut(20));
        
        // Mensaje de confirmación
        JLabel mensaje = new JLabel("<html><div style='text-align: center;'>" +
            "¿Desea continuar con el cierre de caja?</div></html>");
        mensaje.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        mensaje.setForeground(new Color(50, 50, 50));
        mensaje.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(mensaje);
        
        return JOptionPane.showConfirmDialog(
            this,
            panel,
            "Advertencia: Sobra Dinero",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
    }
    
    /**
     * Crea una línea de información para los diálogos de advertencia
     */
    private JPanel crearLineaInfoAdvertencia(String label, double valor, Color color) {
        return crearLineaInfoAdvertencia(label, valor, color, false);
    }
    
    /**
     * Crea una línea de información para los diálogos de advertencia
     */
    private JPanel crearLineaInfoAdvertencia(String label, double valor, Color color, boolean destacado) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        JLabel labelComp = new JLabel(label + ":");
        labelComp.setFont(new Font("Segoe UI", destacado ? Font.BOLD : Font.PLAIN, destacado ? 15 : 14));
        labelComp.setForeground(new Color(75, 85, 99));
        panel.add(labelComp, BorderLayout.WEST);
        
        JLabel valorComp = new JLabel(Formats.CURRENCY.formatValue(valor));
        valorComp.setFont(new Font("Segoe UI", Font.BOLD, destacado ? 16 : 15));
        valorComp.setForeground(color);
        panel.add(valorComp, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * Verifica si las columnas faltante_cierre y sobrante_cierre existen en la tabla closedcash
     * Si no existen, intenta crearlas automáticamente
     * @return true si las columnas existen (o se crearon exitosamente), false si no existen y no se pudieron crear
     */
    private boolean verificarColumnasFaltanteSobrante() {
        try {
            // Intentar una consulta simple para verificar si las columnas existen
            String testSql = "SELECT faltante_cierre, sobrante_cierre FROM closedcash WHERE 1=0";
            try {
                new StaticSentence(m_App.getSession(), testSql).exec();
                // Si no hay error, las columnas existen
                return true;
            } catch (BasicException e) {
                // Las columnas no existen, intentar crearlas
                LOGGER.info("Columnas faltante_cierre y sobrante_cierre no existen, intentando crearlas...");
                try {
                    // Agregar columna faltante_cierre
                    String alterSql1 = "ALTER TABLE closedcash ADD COLUMN faltante_cierre NUMERIC(10,2) DEFAULT 0.00";
                    new StaticSentence(m_App.getSession(), alterSql1).exec();
                    LOGGER.info("Columna faltante_cierre agregada exitosamente");
                    
                    // Agregar columna sobrante_cierre
                    String alterSql2 = "ALTER TABLE closedcash ADD COLUMN sobrante_cierre NUMERIC(10,2) DEFAULT 0.00";
                    new StaticSentence(m_App.getSession(), alterSql2).exec();
                    LOGGER.info("Columna sobrante_cierre agregada exitosamente");
                    
                    return true;
                } catch (BasicException alterEx) {
                    // No se pudieron crear las columnas (puede ser que ya existan o haya un error de permisos)
                    LOGGER.warning("No se pudieron crear las columnas faltante_cierre y sobrante_cierre: " + alterEx.getMessage());
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error verificando columnas faltante_cierre y sobrante_cierre: " + e.getMessage());
            return false;
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
   
    // Variables para almacenar referencias a los componentes del diseño moderno
    private JLabel m_jSalesTotalLabel;
    private JLabel m_jProfitLabel;
    private JLabel m_jCashTotalLabel;
    private JLabel m_jShiftInfoLabel;
    private JLabel m_jInitialAmountLabel; // Label para fondo de caja en diseño moderno
    private JLabel m_jCashSalesLabel; // Label para ventas en efectivo
    private DefaultListModel<String> m_inflowsListModel;
    private DefaultListModel<String> m_outflowsListModel;
    private DefaultListModel<String> m_deptSalesListModel;
    
    /**
     * Crea el diseño moderno basado en la imagen de referencia
     */
    private void createModernLayout() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        
        // Panel principal con scroll
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Título y botones de acción
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        
        // Información del turno
        JPanel shiftInfoPanel = createShiftInfoPanel();
        mainPanel.add(shiftInfoPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        
        // Tarjetas de Ventas Totales y Ganancia
        JPanel metricsPanel = createMetricsCards();
        mainPanel.add(metricsPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        
        // Sección Dinero en Caja
        JPanel cashPanel = createCashSection();
        mainPanel.add(cashPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        
        // Sección Ventas por tipo
        JPanel salesTypePanel = createSalesTypeSection();
        mainPanel.add(salesTypePanel);
        mainPanel.add(Box.createVerticalStrut(20));
        
        // Sección Ingresos de contado
        JPanel cashIncomePanel = createCashIncomeSection();
        mainPanel.add(cashIncomePanel);
        mainPanel.add(Box.createVerticalStrut(20));
        
        // Panel inferior con listas
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel);
        
        // Scroll pane para el contenido
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Crea el panel de encabezado con título y botones
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Panel izquierdo con título y botones de acción
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(Color.WHITE);
        
        // Título
        JLabel titleLabel = new JLabel("CORTE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(50, 50, 50));
        leftPanel.add(titleLabel);
        leftPanel.add(Box.createVerticalStrut(10));
        
        // Botones de acción
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        // Botón Hacer corte de cajero
        JButton btnCashier = new JButton("Hacer corte de cajero");
        styleButton(btnCashier, new Color(33, 150, 243));
        btnCashier.addActionListener(e -> {
            LOGGER.info("Botón 'Hacer corte de cajero' presionado desde diseño moderno");
            // Llamar directamente al método en lugar de doClick() para asegurar que se ejecute
            m_jCloseCashActionPerformed(new java.awt.event.ActionEvent(btnCashier, java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
        });
        buttonPanel.add(btnCashier);
        
        // Botón Hacer corte del día - Implementar funcionalidad básica
        JButton btnDay = new JButton("Hacer corte del día");
        styleButton(btnDay, new Color(76, 175, 80));
        btnDay.addActionListener(e -> {
            LOGGER.info("Botón 'Hacer corte del día' presionado");
            // Por ahora, hacer lo mismo que el corte de cajero hasta que se implemente la funcionalidad completa
            int respuesta = JOptionPane.showConfirmDialog(
                this,
                "El corte del día cerrará todas las cajas activas del día.\n\n" +
                "¿Desea continuar con el corte del día?",
                "Corte del Día",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (respuesta == JOptionPane.YES_OPTION) {
                // Por ahora, hacer el mismo proceso que el corte de cajero
                m_jCloseCashActionPerformed(new java.awt.event.ActionEvent(btnDay, java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
            }
        });
        buttonPanel.add(btnDay);
        
        leftPanel.add(buttonPanel);
        panel.add(leftPanel, BorderLayout.WEST);
        
        return panel;
    }
    
    /**
     * Crea el panel con información del turno
     */
    private JPanel createShiftInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        m_jShiftInfoLabel = new JLabel();
        m_jShiftInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        m_jShiftInfoLabel.setForeground(new Color(107, 114, 128));
        panel.add(m_jShiftInfoLabel);
        
        return panel;
    }
    
    /**
     * Crea las tarjetas de métricas (Ventas Totales y Ganancia)
     */
    private JPanel createMetricsCards() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 15, 0));
        panel.setBackground(Color.WHITE);
        
        // Tarjeta Ventas Totales con icono
        JPanel salesCard = createMetricCard("Ventas Totales", "$0.00", new Color(33, 150, 243), "💰");
        panel.add(salesCard);
        
        // Tarjeta Ganancia con icono
        JPanel profitCard = createMetricCard("Ganancia", "$0.00", new Color(76, 175, 80), "📊");
        panel.add(profitCard);
        
        return panel;
    }
    
    /**
     * Crea una tarjeta de métrica con diseño mejorado
     */
    private JPanel createMetricCard(String title, String value, Color color, String icon) {
        JPanel card = new JPanel(new BorderLayout());
        // Fondo con color suave basado en el color principal
        Color bgColor = new Color(
            Math.min(255, color.getRed() + 240),
            Math.min(255, color.getGreen() + 240),
            Math.min(255, color.getBlue() + 240)
        );
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));
        
        // Panel principal con icono y título
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(bgColor);
        
        // Icono y título
        JLabel titleLabel = new JLabel(icon + " " + title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        titleLabel.setForeground(new Color(75, 85, 99));
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Valor (almacenar referencia para actualizar) - NÚMERO MÁS GRANDE
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 36)); // Aumentado de 24 a 36
        valueLabel.setForeground(color);
        contentPanel.add(valueLabel);
        
        if (title.contains("Ventas Totales")) {
            m_jSalesTotalLabel = valueLabel;
        } else if (title.contains("Ganancia")) {
            m_jProfitLabel = valueLabel;
        }
        
        card.add(contentPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    /**
     * Crea la sección de Dinero en Caja con mejor diseño
     */
    private JPanel createCashSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        // Borde con color azul
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(59, 130, 246), 2),
            "💰 Dinero en Caja",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 18),
            new Color(59, 130, 246)
        ));
        panel.setBorder(BorderFactory.createCompoundBorder(
            panel.getBorder(),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Fondo de caja - guardar referencia para actualizar
        m_jInitialAmountLabel = addCashLine(panel, "Fondo de caja", "$0.00", false, false);
        // Ventas en Efectivo - guardar referencia para actualizar
        m_jCashSalesLabel = addCashLine(panel, "Ventas en Efectivo", "+$0.00", true, true);
        // Abonos en efectivo
        addCashLine(panel, "Abonos en efectivo", "+$0.00", true, true);
        // Entradas
        addCashLine(panel, "Entradas", "+$0.00", true, true);
        // Salidas
        addCashLine(panel, "Salidas", "-$0.00", false, false);
        // Devoluciones en efectivo
        addCashLine(panel, "Devoluciones en efectivo", "-$0.00", false, false);
        
        // Separador
        panel.add(Box.createVerticalStrut(10));
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(229, 231, 235));
        panel.add(separator);
        panel.add(Box.createVerticalStrut(10));
        
        // Total - con número más grande y color destacado
        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setBackground(new Color(249, 250, 251));
        totalPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel totalLabel = new JLabel("Total");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        totalLabel.setForeground(new Color(30, 30, 30));
        totalPanel.add(totalLabel, BorderLayout.WEST);
        
        m_jCashTotalLabel = new JLabel("$0.00");
        m_jCashTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 28)); // Aumentado de 18 a 28
        m_jCashTotalLabel.setForeground(new Color(33, 150, 243)); // Azul destacado
        totalPanel.add(m_jCashTotalLabel, BorderLayout.EAST);
        panel.add(totalPanel);
        
        return panel;
    }
    
    /**
     * Agrega una línea a la sección de dinero en caja con números más grandes
     * @return El JLabel del valor para poder actualizarlo después
     */
    private JLabel addCashLine(JPanel panel, String label, String value, boolean positive, boolean isGreen) {
        JPanel linePanel = new JPanel(new BorderLayout());
        linePanel.setBackground(Color.WHITE);
        linePanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        labelComp.setForeground(new Color(50, 50, 50));
        linePanel.add(labelComp, BorderLayout.WEST);
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Segoe UI", Font.BOLD, 18)); // Aumentado de 14 a 18
        if (isGreen) {
            valueComp.setForeground(new Color(34, 197, 94)); // Verde para positivos
        } else if (positive) {
            valueComp.setForeground(new Color(34, 197, 94)); // Verde
        } else {
            valueComp.setForeground(new Color(239, 68, 68)); // Rojo para negativos
        }
        linePanel.add(valueComp, BorderLayout.EAST);
        
        panel.add(linePanel);
        return valueComp; // Retornar el label para poder actualizarlo
    }
    
    /**
     * Crea la sección de Ventas por tipo con mejor diseño
     */
    private JPanel createSalesTypeSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        // Borde con color verde
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(76, 175, 80), 2),
            "🛒 Ventas",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 18),
            new Color(76, 175, 80)
        ));
        panel.setBorder(BorderFactory.createCompoundBorder(
            panel.getBorder(),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        addCashLine(panel, "En Efectivo", "+$0.00", true, true);
        addCashLine(panel, "Con Tarjeta de Crédito", "+$0.00", true, true);
        addCashLine(panel, "A Crédito", "+$0.00", true, true);
        addCashLine(panel, "Con Vales de Despensa", "+$0.00", true, true);
        addCashLine(panel, "Devoluciones de Ventas", "-$0.00", false, false);
        
        // Separador y total
        panel.add(Box.createVerticalStrut(10));
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(229, 231, 235));
        panel.add(separator);
        panel.add(Box.createVerticalStrut(10));
        
        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setBackground(new Color(249, 250, 251));
        totalPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel totalLabel = new JLabel("Total");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        totalLabel.setForeground(new Color(30, 30, 30));
        totalPanel.add(totalLabel, BorderLayout.WEST);
        
        JLabel totalValue = new JLabel("$0.00");
        totalValue.setFont(new Font("Segoe UI", Font.BOLD, 24)); // Aumentado de 18 a 24
        totalValue.setForeground(new Color(76, 175, 80)); // Verde destacado
        totalPanel.add(totalValue, BorderLayout.EAST);
        panel.add(totalPanel);
        
        return panel;
    }
    
    /**
     * Crea la sección de Ingresos de contado con mejor diseño
     */
    private JPanel createCashIncomeSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        // Borde con color amarillo/naranja
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(234, 179, 8), 2),
            "💵 Ingresos de contado",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 18),
            new Color(234, 179, 8)
        ));
        panel.setBorder(BorderFactory.createCompoundBorder(
            panel.getBorder(),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        addCashLine(panel, "Ventas en Efectivo", "+$0.00", true, true);
        
        // Separador y total
        panel.add(Box.createVerticalStrut(10));
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(229, 231, 235));
        panel.add(separator);
        panel.add(Box.createVerticalStrut(10));
        
        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setBackground(new Color(249, 250, 251));
        totalPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel totalLabel = new JLabel("Total");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        totalLabel.setForeground(new Color(30, 30, 30));
        totalPanel.add(totalLabel, BorderLayout.WEST);
        
        JLabel totalValue = new JLabel("$0.00");
        totalValue.setFont(new Font("Segoe UI", Font.BOLD, 24)); // Aumentado de 18 a 24
        totalValue.setForeground(new Color(76, 175, 80)); // Verde destacado
        totalPanel.add(totalValue, BorderLayout.EAST);
        panel.add(totalPanel);
        
        return panel;
    }
    
    /**
     * Crea el panel inferior con listas
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 15, 0));
        panel.setBackground(Color.WHITE);
        
        // Lista de Entradas de efectivo
        m_inflowsListModel = new DefaultListModel<>();
        JPanel inflowsPanel = createListPanel("⬇️ Entradas de efectivo", m_inflowsListModel);
        panel.add(inflowsPanel);
        
        // Lista de Ventas por Departamento
        m_deptSalesListModel = new DefaultListModel<>();
        JPanel deptPanel = createListPanel("📦 Ventas por Departamento", m_deptSalesListModel);
        panel.add(deptPanel);
        
        // Lista de Salidas de Efectivo
        m_outflowsListModel = new DefaultListModel<>();
        JPanel outflowsPanel = createListPanel("⬆️ Salidas de Efectivo", m_outflowsListModel);
        panel.add(outflowsPanel);
        
        return panel;
    }
    
    /**
     * Crea un panel con lista con mejor diseño y color
     */
    private JPanel createListPanel(String title, DefaultListModel<String> listModel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Determinar color según el tipo de lista
        Color borderColor = new Color(107, 114, 128); // Gris por defecto
        if (title.contains("Entradas")) {
            borderColor = new Color(34, 197, 94); // Verde para entradas
        } else if (title.contains("Salidas")) {
            borderColor = new Color(239, 68, 68); // Rojo para salidas
        } else if (title.contains("Departamento")) {
            borderColor = new Color(147, 51, 234); // Púrpura para departamentos
        }
        
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(borderColor, 2),
            title,
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 16),
            borderColor
        ));
        panel.setBorder(BorderFactory.createCompoundBorder(
            panel.getBorder(),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JList<String> list = new JList<>(listModel);
        list.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        list.setBackground(Color.WHITE);
        list.setForeground(new Color(50, 50, 50));
        list.setSelectionBackground(new Color(239, 246, 255));
        list.setSelectionForeground(new Color(30, 64, 175));
        
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(200, 150));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Estiliza un botón
     */
    private void styleButton(JButton button, Color color) {
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(180, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });
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

        LOGGER.info("=== INICIO: Botón de cierre de caja presionado ===");
        
        int res = JOptionPane.showConfirmDialog(this, 
                AppLocal.getIntString("message.wannaclosecash"), 
                AppLocal.getIntString("message.title"), 
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        LOGGER.info("Respuesta del diálogo de confirmación: " + (res == JOptionPane.YES_OPTION ? "SÍ" : "NO"));
        
        if (res == JOptionPane.YES_OPTION) {
            
            // Validación de dinero físico en caja
            ValidacionDineroResult validacion = validarDineroFisicoEnCaja();
            if (!validacion.valido) {
                // Si la validación falla y el usuario no quiso continuar, no hacer el cierre
                LOGGER.info("Cierre de caja abortado debido a la validación de dinero físico.");
                return;
            }
            
            LOGGER.info("Validación de dinero físico completada. Faltante: " + validacion.faltante + ", Sobrante: " + validacion.sobrante);
            
            // Los valores de faltante y sobrante ya están guardados en faltanteCierre y sobranteCierre

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
            
            LOGGER.info("Iniciando proceso de cierre de caja. Faltante: " + faltanteCierre + ", Sobrante: " + sobranteCierre);

            try {

                if (m_App.getActiveCashDateEnd() == null) {
                    LOGGER.info("Caja activa encontrada, procediendo a cerrarla...");
                    // Verificar si las columnas faltante_cierre y sobrante_cierre existen
                    boolean columnasExisten = verificarColumnasFaltanteSobrante();
                    
                    if (columnasExisten) {
                        // Las columnas existen, usar UPDATE completo
                        try {
                            LOGGER.info("Actualizando closedcash con columnas faltante_cierre y sobrante_cierre...");
                            new StaticSentence(m_App.getSession()
                                , "UPDATE closedcash SET DATEEND = ?, NOSALES = ?, faltante_cierre = ?, sobrante_cierre = ? WHERE HOST = ? AND MONEY = ?"
                                , new SerializerWriteBasic(new Datas[] {
                                    Datas.TIMESTAMP, 
                                    Datas.INT,
                                    Datas.DOUBLE,
                                    Datas.DOUBLE,
                                    Datas.STRING, 
                                    Datas.STRING}))
                            .exec(new Object[] {dNow, result, faltanteCierre, sobranteCierre,
                                m_App.getProperties().getHost(), 
                                m_App.getActiveCashIndex()});
                            LOGGER.info(String.format("Cierre de caja guardado exitosamente con faltante=%.2f, sobrante=%.2f", 
                                faltanteCierre, sobranteCierre));
                        } catch (BasicException e1) {
                            // Si falla a pesar de verificar, usar fallback
                            LOGGER.warning("Error al guardar faltante_cierre y sobrante_cierre, usando fallback: " + e1.getMessage());
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
                    } else {
                        // Las columnas no existen, usar UPDATE sin ellas
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
                        LOGGER.info("Cierre de caja guardado (columnas faltante_cierre y sobrante_cierre no disponibles)");
                    }
                }
            } catch (BasicException e) {
                LOGGER.log(Level.SEVERE, "Error al actualizar closedcash con DATEEND y NOSALES.", e);
                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, 
                        AppLocal.getIntString("message.cannotclosecash"), e);
                msg.show(this);
                return; // No continuar si falla la actualización de closedcash
            }

            try {
                // Creamos una nueva caja con fondo inicial en 0.0
                m_App.setActiveCash(UUID.randomUUID().toString(), 
                        m_App.getActiveCashSequence() + 1, dNow, null, 0.0);

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
                LOGGER.log(Level.SEVERE, "Error al crear nueva caja o imprimir reporte.", e);
                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, 
                        AppLocal.getIntString("message.cannotclosecash"), e);
                msg.show(this);
                return; // No continuar si falla la creación de la nueva caja
            }

            try {
                loadData();
                // Actualizar el diseño moderno después de cargar datos
                updateModernLayoutData();
            } catch (BasicException e) {
                LOGGER.log(Level.WARNING, "Error al recargar datos después del cierre.", e);
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
