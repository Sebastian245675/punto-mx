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
import com.openbravo.data.loader.QBFCompareEnum;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.data.loader.Session;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import com.openbravo.data.loader.StaticSentence;
import com.openbravo.data.loader.SerializerReadClass;
import com.openbravo.data.loader.SerializerWriteString;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.*;
import com.openbravo.pos.printer.TicketParser;
import com.openbravo.pos.printer.TicketPrinterException;
import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptException;
import com.openbravo.pos.scripting.ScriptFactory;
import com.openbravo.pos.util.StringUtils;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.openbravo.data.loader.BaseSentence;
import com.openbravo.data.loader.QBFBuilder;
import com.openbravo.data.loader.SerializerReadBasic;
import com.openbravo.pos.reports.JRDataSourceBasic;
import com.openbravo.pos.reports.JRViewer400;
import com.openbravo.pos.reports.ReportFields;
import com.openbravo.pos.reports.ReportFieldsArray;
import com.openbravo.pos.reports.JPanelReport;
import java.sql.PreparedStatement;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.DefaultListModel;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
        
        // Actualizar el template en la base de datos desde el archivo XML
        actualizarTemplateEnBD();
        
        loadData();
    }
    
    /**
     * Actualiza el template Printer.CloseCash en la base de datos desde el archivo XML
     */
    private void actualizarTemplateEnBD() {
        try {
            // Leer el archivo XML desde el classpath
            InputStream is = getClass().getResourceAsStream("/com/openbravo/pos/templates/Printer.CloseCash.xml");
            if (is == null) {
                LOGGER.warning("No se pudo encontrar el archivo Printer.CloseCash.xml en el classpath");
                return;
            }
            
            // Leer todo el contenido del archivo
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] templateContent = baos.toByteArray();
            is.close();
            baos.close();
            
            // Actualizar el template en la base de datos
            // Tipo 0 = texto/XML
            m_dlSystem.setResource("Printer.CloseCash", 0, templateContent);
            LOGGER.info("Template Printer.CloseCash actualizado en la base de datos");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error leyendo el archivo Printer.CloseCash.xml: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error actualizando template en BD: " + e.getMessage(), e);
        }
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
            
            // Actualizar totales de entradas y salidas
            updateInflowsOutflowsTotals();
            
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
            LOGGER.info("updateCashMovements: m_PaymentsToClose o modelos son null");
            return;
        }
        
        try {
            m_inflowsListModel.clear();
            m_outflowsListModel.clear();
            
            // Obtener entradas y salidas de efectivo desde receipts y payments
            s = m_App.getSession();
            con = s.getConnection();
            String activeCashIndex = m_App.getActiveCashIndex();
            Date dateStart = m_PaymentsToClose.getDateStart();
            
            LOGGER.info("updateCashMovements: Buscando movimientos para MONEY=" + activeCashIndex + 
                       ", fecha inicio=" + dateStart);
            
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
            
            // Buscar entradas de dinero desde payments con PAYMENT = 'cashin'
            String sqlInflows = "SELECT receipts.DATENEW, payments.TOTAL, payments.NOTES " +
                                "FROM receipts " +
                                "INNER JOIN payments ON receipts.ID = payments.RECEIPT " +
                                "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'cashin' " +
                                "AND receipts.DATENEW >= ? " +
                                "ORDER BY receipts.DATENEW DESC";
            
            java.sql.PreparedStatement pstmtInflows = con.prepareStatement(sqlInflows);
            pstmtInflows.setString(1, activeCashIndex);
            pstmtInflows.setTimestamp(2, new java.sql.Timestamp(dateStart.getTime()));
            
            LOGGER.info("updateCashMovements: Ejecutando consulta de entradas: " + sqlInflows);
            
            rs = pstmtInflows.executeQuery();
            int countInflows = 0;
            while (rs.next()) {
                countInflows++;
                Date dateNew = rs.getTimestamp("DATENEW");
                double total = rs.getDouble("TOTAL");
                String notes = rs.getString("NOTES");
                String timeStr = timeFormat.format(dateNew);
                String item = String.format("%s $%.2f%s", timeStr, total, 
                                          (notes != null && !notes.isEmpty()) ? " - " + notes : "");
                m_inflowsListModel.addElement(item);
                LOGGER.info("updateCashMovements: Entrada encontrada: " + item);
            }
            rs.close();
            pstmtInflows.close();
            
            LOGGER.info("updateCashMovements: Total entradas encontradas: " + countInflows);
            
            // Buscar salidas de dinero desde payments con PAYMENT = 'cashout'
            String sqlOutflows = "SELECT receipts.DATENEW, payments.TOTAL, payments.NOTES " +
                                "FROM receipts " +
                                "INNER JOIN payments ON receipts.ID = payments.RECEIPT " +
                                "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'cashout' " +
                                "AND receipts.DATENEW >= ? " +
                                "ORDER BY receipts.DATENEW DESC";
            
            java.sql.PreparedStatement pstmtOutflows = con.prepareStatement(sqlOutflows);
            pstmtOutflows.setString(1, activeCashIndex);
            pstmtOutflows.setTimestamp(2, new java.sql.Timestamp(dateStart.getTime()));
            
            rs = pstmtOutflows.executeQuery();
            int countOutflows = 0;
            while (rs.next()) {
                countOutflows++;
                Date dateNew = rs.getTimestamp("DATENEW");
                double total = Math.abs(rs.getDouble("TOTAL")); // Valor absoluto para mostrar positivo
                String notes = rs.getString("NOTES");
                String timeStr = timeFormat.format(dateNew);
                String item = String.format("%s $%.2f%s", timeStr, total, 
                                          (notes != null && !notes.isEmpty()) ? " - " + notes : "");
                m_outflowsListModel.addElement(item);
                LOGGER.info("updateCashMovements: Salida encontrada: " + item);
            }
            rs.close();
            pstmtOutflows.close();
            
            LOGGER.info("updateCashMovements: Total salidas encontradas: " + countOutflows);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error actualizando movimientos de efectivo: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    /**
     * Actualiza los totales de entradas y salidas en los labels
     */
    private void updateInflowsOutflowsTotals() {
        if (m_jInflowsLabel == null || m_jOutflowsLabel == null) {
            return;
        }
        
        try {
            // Calcular total de entradas desde la base de datos
            s = m_App.getSession();
            con = s.getConnection();
            String activeCashIndex = m_App.getActiveCashIndex();
            Date dateStart = m_PaymentsToClose.getDateStart();
            
            // Sumar todas las entradas (cashin)
            String sqlInflowsTotal = "SELECT COALESCE(SUM(payments.TOTAL), 0) " +
                                    "FROM receipts " +
                                    "INNER JOIN payments ON receipts.ID = payments.RECEIPT " +
                                    "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'cashin' " +
                                    "AND receipts.DATENEW >= ?";
            
            java.sql.PreparedStatement pstmtInflows = con.prepareStatement(sqlInflowsTotal);
            pstmtInflows.setString(1, activeCashIndex);
            pstmtInflows.setTimestamp(2, new java.sql.Timestamp(dateStart.getTime()));
            
            rs = pstmtInflows.executeQuery();
            double inflowsTotal = 0.0;
            if (rs.next()) {
                inflowsTotal = rs.getDouble(1);
            }
            rs.close();
            pstmtInflows.close();
            
            // Actualizar label de entradas
            m_jInflowsLabel.setText("+" + Formats.CURRENCY.formatValue(inflowsTotal));
            LOGGER.info("updateInflowsOutflowsTotals: Total entradas = " + inflowsTotal);
            
            // Sumar todas las salidas (cashout) - usar valor absoluto
            String sqlOutflowsTotal = "SELECT COALESCE(SUM(ABS(payments.TOTAL)), 0) " +
                                     "FROM receipts " +
                                     "INNER JOIN payments ON receipts.ID = payments.RECEIPT " +
                                     "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'cashout' " +
                                     "AND receipts.DATENEW >= ?";
            
            java.sql.PreparedStatement pstmtOutflows = con.prepareStatement(sqlOutflowsTotal);
            pstmtOutflows.setString(1, activeCashIndex);
            pstmtOutflows.setTimestamp(2, new java.sql.Timestamp(dateStart.getTime()));
            
            rs = pstmtOutflows.executeQuery();
            double outflowsTotal = 0.0;
            if (rs.next()) {
                outflowsTotal = rs.getDouble(1);
            }
            rs.close();
            pstmtOutflows.close();
            
            // Actualizar label de salidas
            m_jOutflowsLabel.setText("-" + Formats.CURRENCY.formatValue(outflowsTotal));
            LOGGER.info("updateInflowsOutflowsTotals: Total salidas = " + outflowsTotal);
            
            // Actualizar el total de efectivo incluyendo entradas y salidas
            if (m_jCashTotalLabel != null) {
                double initialAmount = m_jInitialAmountLabel != null ? 
                    Formats.CURRENCY.parseValue(m_jInitialAmountLabel.getText()) : 0.0;
                double cashSales = m_PaymentsToClose.getCashTotal() != null ? m_PaymentsToClose.getCashTotal() : 0.0;
                double total = initialAmount + cashSales + inflowsTotal - outflowsTotal;
                m_jCashTotalLabel.setText(Formats.CURRENCY.formatValue(total));
                LOGGER.info("updateInflowsOutflowsTotals: Total actualizado = " + total + 
                           " (inicial=" + initialAmount + ", ventas=" + cashSales + 
                           ", entradas=" + inflowsTotal + ", salidas=" + outflowsTotal + ")");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error actualizando totales de entradas y salidas: " + e.getMessage(), e);
            e.printStackTrace();
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
        printPayments(report, false);
    }
    
    private void printPayments(String report, boolean isDayClose) {
        
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
                
                // Si es cierre del día, obtener todos los turnos del día con sus productos
                Date closeDate = m_PaymentsToClose.getDateEnd();
                // Variables para almacenar datos de productos (fuera del bloque para poder usarlas después)
                String productosXML = "";
                java.util.List<ConsolidatedProduct> consolidatedProductList = new java.util.ArrayList<>();
                java.util.List<PaymentsModel.ProductSalesLine> currentShiftProducts = new java.util.ArrayList<>();
                
                if (isDayClose && closeDate != null) {
                    LOGGER.info("Es cierre del día, obteniendo todos los turnos...");
                    java.util.List<ShiftData> allShifts = getAllShiftsForDay(closeDate);
                    LOGGER.info("Turnos obtenidos: " + (allShifts != null ? allShifts.size() : 0));
                    
                    // Verificar productos en cada turno antes de pasar al template
                    if (allShifts != null) {
                        for (ShiftData shift : allShifts) {
                            LOGGER.info("Verificando turno #" + shift.sequence + 
                                       " - ProductLines size: " + shift.getProductLines().size());
                            if (!shift.getProductLines().isEmpty()) {
                                LOGGER.info("  Primer producto: " + shift.getProductLines().get(0).printProductName());
                            }
                        }
                    }
                    
                    script.put("allShifts", allShifts);
                    script.put("isDayClose", Boolean.TRUE);
                    LOGGER.info("Datos pasados al template - isDayClose: true, allShifts size: " + (allShifts != null ? allShifts.size() : 0));
                    
                    // Calcular totales del día y consolidar productos
                    double totalDaySales = 0.0;
                    double totalDayPayments = 0.0;
                    java.util.Map<String, ConsolidatedProduct> consolidatedProducts = new java.util.HashMap<>();
                    
                    if (allShifts != null) {
                        for (ShiftData shift : allShifts) {
                            totalDaySales += shift.getTotalSales();
                            totalDayPayments += shift.getTotalPayments();
                            
                            // Consolidar productos de este turno
                            LOGGER.info("Procesando productos del turno #" + shift.getSequence() + " - Total productos: " + shift.getProductLines().size());
                            for (PaymentsModel.ProductSalesLine product : shift.getProductLines()) {
                                // Usar el nombre sin codificar para la agrupación
                                String productName = product.getProductName();
                                if (productName == null || productName.trim().isEmpty()) {
                                    LOGGER.warning("Producto con nombre vacío encontrado, saltando...");
                                    continue;
                                }
                                ConsolidatedProduct consolidated = consolidatedProducts.get(productName);
                                if (consolidated == null) {
                                    consolidated = new ConsolidatedProduct(productName);
                                    consolidatedProducts.put(productName, consolidated);
                                    LOGGER.info("Nuevo producto agregado a consolidación: " + productName);
                                }
                                double units = product.getProductUnits() != null ? product.getProductUnits() : 0.0;
                                double price = product.getProductPrice() != null ? product.getProductPrice() : 0.0;
                                double taxRate = product.getTaxRate() != null ? product.getTaxRate() : 0.0;
                                consolidated.addUnits(units);
                                consolidated.addTotal(price * (1.0 + taxRate) * units);
                                LOGGER.info("  Producto: " + productName + " - Unidades: " + units + " - Total: " + (price * (1.0 + taxRate) * units));
                            }
                        }
                    }
                    
                    // Convertir el mapa a lista ordenada
                    consolidatedProductList = new java.util.ArrayList<>(consolidatedProducts.values());
                    java.util.Collections.sort(consolidatedProductList, (a, b) -> a.getName().compareTo(b.getName()));
                    
                    // SOLUCIÓN RADICAL: Generar las líneas XML directamente en Java
                    StringBuilder productosText = new StringBuilder();
                    if (!consolidatedProductList.isEmpty()) {
                        for (ConsolidatedProduct product : consolidatedProductList) {
                            String name = StringUtils.encodeXML(product.getName());
                            // Limitar nombre a 25 caracteres
                            if (name.length() > 25) {
                                name = name.substring(0, 22) + "...";
                            }
                            String units = Formats.DOUBLE.formatValue(product.getTotalUnits());
                            String total = Formats.CURRENCY.formatValue(product.getTotalValue());
                            
                            // Generar línea XML directamente
                            productosText.append("<line>\n");
                            productosText.append("    <text align=\"left\" length=\"25\">").append(name).append("</text>\n");
                            productosText.append("    <text align=\"right\" length=\"8\">").append(units).append("</text>\n");
                            productosText.append("    <text align=\"right\" length=\"9\">").append(total).append("</text>\n");
                            productosText.append("</line>\n");
                        }
                    } else {
                        productosText.append("<line>\n");
                        productosText.append("    <text align=\"left\" length=\"42\">Sin productos vendidos</text>\n");
                        productosText.append("</line>\n");
                    }
                    
                    // Generar texto de productos por turno para el reporte del día
                    StringBuilder productosPorTurnoText = new StringBuilder();
                    if (allShifts != null && !allShifts.isEmpty()) {
                        for (ShiftData shift : allShifts) {
                            productosPorTurnoText.append("<line>\n");
                            productosPorTurnoText.append("    <text align=\"center\" length=\"42\">--- TURNO #").append(shift.getSequence()).append(" ---</text>\n");
                            productosPorTurnoText.append("</line>\n");
                            
                            if (!shift.getProductLines().isEmpty()) {
                                for (PaymentsModel.ProductSalesLine product : shift.getProductLines()) {
                                    String name = StringUtils.encodeXML(product.printProductName());
                                    if (name.length() > 25) {
                                        name = name.substring(0, 22) + "...";
                                    }
                                    String units = product.printProductUnits();
                                    String total = product.printProductSubValue();
                                    
                                    productosPorTurnoText.append("<line>\n");
                                    productosPorTurnoText.append("    <text align=\"left\" length=\"25\">").append(name).append("</text>\n");
                                    productosPorTurnoText.append("    <text align=\"right\" length=\"8\">").append(units).append("</text>\n");
                                    productosPorTurnoText.append("    <text align=\"right\" length=\"9\">").append(total).append("</text>\n");
                                    productosPorTurnoText.append("</line>\n");
                                }
                            } else {
                                productosPorTurnoText.append("<line>\n");
                                productosPorTurnoText.append("    <text align=\"left\" length=\"42\">Sin productos en este turno</text>\n");
                                productosPorTurnoText.append("</line>\n");
                            }
                            
                            productosPorTurnoText.append("<line>\n");
                            productosPorTurnoText.append("    <text align=\"right\" length=\"42\">Total Turno: ").append(Formats.CURRENCY.formatValue(shift.getTotalSales())).append("</text>\n");
                            productosPorTurnoText.append("</line>\n");
                            productosPorTurnoText.append("<line>\n");
                            productosPorTurnoText.append("</line>\n");
                        }
                    }
                    
                    script.put("totalDaySales", Formats.CURRENCY.formatValue(totalDaySales));
                    script.put("totalDayPayments", Formats.CURRENCY.formatValue(totalDayPayments));
                    script.put("consolidatedProducts", consolidatedProductList);
                    productosXML = productosText.toString(); // Productos consolidados del día
                    script.put("productosText", productosXML); // Variable de texto simple para productos consolidados
                    script.put("productosPorTurnoText", productosPorTurnoText.toString()); // Productos por turno
                    LOGGER.info("Totales del día - Ventas: " + totalDaySales + ", Pagos: " + totalDayPayments);
                    LOGGER.info("Productos consolidados: " + consolidatedProductList.size());
                    LOGGER.info("Turnos procesados: " + (allShifts != null ? allShifts.size() : 0));
                    LOGGER.info("Texto de productos consolidados generado:\n" + productosXML);
                    LOGGER.info("Texto de productos por turno generado:\n" + productosPorTurnoText.toString());
                    if (!consolidatedProductList.isEmpty()) {
                        LOGGER.info("Primer producto consolidado: " + consolidatedProductList.get(0).getName() + 
                                   " - Cantidad: " + consolidatedProductList.get(0).getTotalUnits() + 
                                   " - Total: " + consolidatedProductList.get(0).getTotalValue());
                    } else {
                        LOGGER.warning("Lista de productos consolidados está vacía!");
                    }
                } else {
                    // Cierre de turno: mostrar solo productos del turno actual
                    script.put("isDayClose", Boolean.FALSE);
                    script.put("allShifts", null);
                    script.put("consolidatedProducts", new java.util.ArrayList<>());
                    
                    // Obtener productos del turno actual desde PaymentsModel
                    currentShiftProducts = 
                        m_PaymentsToClose != null ? m_PaymentsToClose.getProductSalesLines() : new java.util.ArrayList<>();
                    
                    // Generar XML de productos del turno actual
                    StringBuilder productosTextTurno = new StringBuilder();
                    if (currentShiftProducts != null && !currentShiftProducts.isEmpty()) {
                        LOGGER.info("Cierre de turno: " + currentShiftProducts.size() + " productos del turno actual");
                        for (PaymentsModel.ProductSalesLine product : currentShiftProducts) {
                            String name = StringUtils.encodeXML(product.printProductName());
                            // Limitar nombre a 25 caracteres
                            if (name.length() > 25) {
                                name = name.substring(0, 22) + "...";
                            }
                            String units = product.printProductUnits();
                            String total = product.printProductSubValue();
                            
                            // Generar línea XML directamente
                            productosTextTurno.append("<line>\n");
                            productosTextTurno.append("    <text align=\"left\" length=\"25\">").append(name).append("</text>\n");
                            productosTextTurno.append("    <text align=\"right\" length=\"8\">").append(units).append("</text>\n");
                            productosTextTurno.append("    <text align=\"right\" length=\"9\">").append(total).append("</text>\n");
                            productosTextTurno.append("</line>\n");
                        }
                    } else {
                        productosTextTurno.append("<line>\n");
                        productosTextTurno.append("    <text align=\"left\" length=\"42\">Sin productos vendidos en este turno</text>\n");
                        productosTextTurno.append("</line>\n");
                        LOGGER.info("Cierre de turno: Sin productos vendidos en el turno actual");
                    }
                    
                    productosXML = productosTextTurno.toString();
                    script.put("productosText", productosXML);
                    script.put("currentShiftProducts", currentShiftProducts);
                    LOGGER.info("No es cierre del día. isDayClose: " + isDayClose + ", closeDate: " + closeDate);
                    LOGGER.info("Productos del turno actual pasados al template: " + (currentShiftProducts != null ? currentShiftProducts.size() : 0));
                }
                
                String ticketOutput = script.eval(sresource).toString();
                
                // Reemplazar productos consolidados en cierre de día
                if (isDayClose && closeDate != null && !consolidatedProductList.isEmpty()) {
                    String placeholder = "<!-- PRODUCTOS_PLACEHOLDER -->";
                    int placeholderIndex = ticketOutput.indexOf(placeholder);
                    if (placeholderIndex >= 0) {
                        StringBuilder productosXMLConsolidados = new StringBuilder();
                        for (ConsolidatedProduct product : consolidatedProductList) {
                            String name = product.printName();
                            if (name.length() > 25) {
                                name = name.substring(0, 22) + "...";
                            }
                            productosXMLConsolidados.append("        <line>\n");
                            productosXMLConsolidados.append("            <text align =\"left\" length=\"25\">").append(StringUtils.encodeXML(name)).append("</text>\n");
                            productosXMLConsolidados.append("            <text align =\"right\" length=\"8\">").append(product.printUnits()).append("</text>\n");
                            productosXMLConsolidados.append("            <text align =\"right\" length=\"9\">").append(product.printTotal()).append("</text>\n");
                            productosXMLConsolidados.append("        </line>\n");
                        }
                        
                        // Buscar y reemplazar el placeholder y la línea completa "Sin productos vendidos"
                        int startIndex = placeholderIndex;
                        // Buscar el inicio de la línea <line> que sigue al placeholder
                        int searchStart = startIndex + placeholder.length();
                        int lineStart = -1;
                        // Buscar <line> con posibles espacios/saltos de línea antes
                        for (int i = searchStart; i < Math.min(searchStart + 100, ticketOutput.length()); i++) {
                            String remaining = ticketOutput.substring(i);
                            if (remaining.startsWith("<line>") || remaining.trim().startsWith("<line>")) {
                                lineStart = i;
                                break;
                            }
                        }
                        
                        // Buscar el final de esa línea completa (hasta </line>)
                        int lineEnd = -1;
                        if (lineStart >= 0) {
                            lineEnd = ticketOutput.indexOf("</line>", lineStart);
                            if (lineEnd >= 0) {
                                lineEnd += 7; // Incluir </line>
                                // Incluir el salto de línea siguiente si existe
                                while (lineEnd < ticketOutput.length() && 
                                       (ticketOutput.charAt(lineEnd) == '\n' || 
                                        ticketOutput.charAt(lineEnd) == '\r' || 
                                        Character.isWhitespace(ticketOutput.charAt(lineEnd)))) {
                                    lineEnd++;
                                }
                            }
                        }
                        
                        if (lineStart > 0 && lineEnd > lineStart) {
                            // Eliminar desde el placeholder hasta el final de la línea completa
                            // Esto incluye: <!-- PRODUCTOS_PLACEHOLDER --> + espacios/saltos de línea + <line>...texto...</line>
                            // Buscar el inicio real: desde el placeholder, encontrar el inicio de la línea que contiene el placeholder
                            int lineStartIndex = startIndex;
                            // Retroceder hasta encontrar el inicio de la línea (carácter anterior a \n o inicio del string)
                            while (lineStartIndex > 0 && ticketOutput.charAt(lineStartIndex - 1) != '\n' && ticketOutput.charAt(lineStartIndex - 1) != '\r') {
                                lineStartIndex--;
                            }
                            
                            String before = ticketOutput.substring(0, lineStartIndex);
                            String after = ticketOutput.substring(lineEnd);
                            // El XML generado ya tiene la indentación correcta (8 espacios)
                            String replacement = productosXMLConsolidados.toString();
                            ticketOutput = before + replacement + after;
                            LOGGER.info("Productos consolidados insertados en template: " + consolidatedProductList.size());
                            LOGGER.info("Reemplazo exitoso - Longitud antes: " + before.length() + ", después: " + ticketOutput.length());
                            // Log del fragmento que se está reemplazando
                            String fragmentoReemplazado = ticketOutput.substring(startIndex, Math.min(startIndex + 200, lineEnd));
                            LOGGER.info("Fragmento reemplazado (primeros 200 chars): " + fragmentoReemplazado.replace("\n", "\\n").replace("\r", "\\r"));
                            // Verificar que el producto esté en el ticket después del reemplazo
                            int productoIndex = ticketOutput.indexOf("Tortillas");
                            LOGGER.info("Verificación: Producto 'Tortillas' encontrado en ticket después del reemplazo: " + (productoIndex >= 0) + " (posición: " + productoIndex + ")");
                        } else {
                            LOGGER.warning("No se pudo encontrar la línea a reemplazar después del placeholder PRODUCTOS_PLACEHOLDER. lineStart: " + lineStart + ", lineEnd: " + lineEnd);
                            LOGGER.warning("Fragmento después del placeholder (primeros 200 chars): " + ticketOutput.substring(Math.max(0, searchStart), Math.min(ticketOutput.length(), searchStart + 200)));
                        }
                    } else {
                        LOGGER.warning("No se encontró el marcador PRODUCTOS_PLACEHOLDER en el template");
                    }
                }
                
                // Reemplazar productos del turno en cierre de turno
                if (!isDayClose && currentShiftProducts != null && !currentShiftProducts.isEmpty()) {
                    String placeholderTurno = "<!-- PRODUCTOS_TURNO_PLACEHOLDER -->";
                    int placeholderIndex = ticketOutput.indexOf(placeholderTurno);
                    if (placeholderIndex >= 0) {
                        StringBuilder productosTurnoXML = new StringBuilder();
                        for (PaymentsModel.ProductSalesLine product : currentShiftProducts) {
                            String name = product.printProductName();
                            // El nombre ya viene codificado de printProductName()
                            if (name.length() > 25) {
                                name = name.substring(0, 22) + "...";
                            }
                            productosTurnoXML.append("        <line>\n");
                            productosTurnoXML.append("            <text align =\"left\" length=\"25\">").append(StringUtils.encodeXML(name)).append("</text>\n");
                            productosTurnoXML.append("            <text align =\"right\" length=\"8\">").append(product.printProductUnits()).append("</text>\n");
                            productosTurnoXML.append("            <text align =\"right\" length=\"9\">").append(product.printProductSubValue()).append("</text>\n");
                            productosTurnoXML.append("        </line>\n");
                        }
                        
                        // Buscar y reemplazar el placeholder y la línea completa "Sin productos vendidos en este turno"
                        int startIndex = placeholderIndex;
                        // Buscar el inicio de la línea <line> que sigue al placeholder
                        int searchStart = startIndex + placeholderTurno.length();
                        int lineStart = -1;
                        // Buscar <line> con posibles espacios/saltos de línea antes
                        for (int i = searchStart; i < Math.min(searchStart + 100, ticketOutput.length()); i++) {
                            String remaining = ticketOutput.substring(i);
                            if (remaining.startsWith("<line>") || remaining.trim().startsWith("<line>")) {
                                lineStart = i;
                                break;
                            }
                        }
                        
                        // Buscar el final de esa línea completa (hasta </line>)
                        int lineEnd = -1;
                        if (lineStart >= 0) {
                            lineEnd = ticketOutput.indexOf("</line>", lineStart);
                            if (lineEnd >= 0) {
                                lineEnd += 7; // Incluir </line>
                                // Incluir el salto de línea siguiente si existe
                                while (lineEnd < ticketOutput.length() && 
                                       (ticketOutput.charAt(lineEnd) == '\n' || 
                                        ticketOutput.charAt(lineEnd) == '\r' || 
                                        Character.isWhitespace(ticketOutput.charAt(lineEnd)))) {
                                    lineEnd++;
                                }
                            }
                        }
                        
                        if (lineStart > 0 && lineEnd > lineStart) {
                            // Reemplazar desde el placeholder (incluyéndolo) hasta el final de la línea "Sin productos vendidos en este turno"
                            String before = ticketOutput.substring(0, startIndex);
                            String after = ticketOutput.substring(lineEnd);
                            // El XML generado ya tiene la indentación correcta (8 espacios)
                            String replacement = productosTurnoXML.toString();
                            ticketOutput = before + replacement + after;
                            LOGGER.info("Productos del turno insertados en template: " + currentShiftProducts.size());
                            LOGGER.info("Reemplazo exitoso - Longitud antes: " + before.length() + ", después: " + ticketOutput.length());
                            // Verificar que el primer producto esté en el ticket después del reemplazo
                            if (!currentShiftProducts.isEmpty()) {
                                String primerProducto = currentShiftProducts.get(0).printProductName();
                                int productoIndex = ticketOutput.indexOf(primerProducto);
                                LOGGER.info("Verificación: Primer producto encontrado en ticket después del reemplazo: " + (productoIndex >= 0) + " (posición: " + productoIndex + ")");
                            }
                        } else {
                            LOGGER.warning("No se pudo encontrar la línea a reemplazar después del placeholder PRODUCTOS_TURNO_PLACEHOLDER. lineStart: " + lineStart + ", lineEnd: " + lineEnd);
                            LOGGER.warning("Fragmento después del placeholder (primeros 200 chars): " + ticketOutput.substring(Math.max(0, searchStart), Math.min(ticketOutput.length(), searchStart + 200)));
                        }
                    } else {
                        LOGGER.warning("No se encontró el marcador PRODUCTOS_TURNO_PLACEHOLDER en el template");
                    }
                }
                
                LOGGER.info("Ticket generado - Longitud: " + ticketOutput.length() + " caracteres");
                LOGGER.info("Ticket contiene 'REPORTE DEL DÍA': " + ticketOutput.contains("REPORTE DEL DÍA"));
                LOGGER.info("Ticket contiene 'Productos Vendidos': " + ticketOutput.contains("Productos Vendidos"));
                LOGGER.info("Ticket contiene 'PRODUCTOS VENDIDOS DEL DÍA': " + ticketOutput.contains("PRODUCTOS VENDIDOS DEL DÍA"));
                // Verificar que el XML generado contiene las etiquetas correctas
                int productosCount = 0;
                int productosIndex = 0;
                while ((productosIndex = ticketOutput.indexOf("<text align=\"left\" length=\"25\">", productosIndex)) != -1) {
                    productosCount++;
                    productosIndex += 1;
                }
                LOGGER.info("Número de líneas de productos encontradas en XML: " + productosCount);
                // Mostrar un fragmento del XML alrededor del placeholder para verificar
                int placeholderPos = ticketOutput.indexOf("<!-- PRODUCTOS_PLACEHOLDER -->");
                if (placeholderPos >= 0) {
                    int start = Math.max(0, placeholderPos - 100);
                    int end = Math.min(ticketOutput.length(), placeholderPos + 500);
                    LOGGER.info("Fragmento XML alrededor del placeholder (si aún existe): " + ticketOutput.substring(start, end));
                } else {
                    LOGGER.info("Placeholder PRODUCTOS_PLACEHOLDER no encontrado (fue reemplazado correctamente)");
                }
                // Log para verificar si la lista consolidada está presente en el contexto
                Object consolidatedProductsObj = script.get("consolidatedProducts");
                if (consolidatedProductsObj != null) {
                    LOGGER.info("consolidatedProducts en script: " + consolidatedProductsObj.getClass().getName());
                    if (consolidatedProductsObj instanceof java.util.List) {
                        java.util.List<?> list = (java.util.List<?>) consolidatedProductsObj;
                        LOGGER.info("Tamaño de consolidatedProducts en script: " + list.size());
                        if (!list.isEmpty()) {
                            LOGGER.info("Primer elemento: " + list.get(0).getClass().getName());
                        }
                    }
                } else {
                    LOGGER.warning("consolidatedProducts es NULL en el script!");
                }
                LOGGER.info("Ticket contiene 'PRODUCTOS VENDIDOS DEL DÍA': " + ticketOutput.contains("PRODUCTOS VENDIDOS DEL DÍA"));
                // Verificar si el ticket contiene el nombre del producto consolidado
                if (consolidatedProductsObj != null && consolidatedProductsObj instanceof java.util.List) {
                    java.util.List<?> list = (java.util.List<?>) consolidatedProductsObj;
                    if (!list.isEmpty() && list.get(0) instanceof ConsolidatedProduct) {
                        ConsolidatedProduct firstProduct = (ConsolidatedProduct) list.get(0);
                        String productName = firstProduct.getName();
                        LOGGER.info("Buscando producto '" + productName + "' en ticket: " + ticketOutput.contains(productName));
                    }
                }
                
                // Log del fragmento XML alrededor de donde deberían estar los productos
                int productosSectionStart = ticketOutput.indexOf("PRODUCTOS VENDIDOS DEL DÍA");
                if (productosSectionStart >= 0) {
                    int start = Math.max(0, productosSectionStart - 200);
                    int end = Math.min(ticketOutput.length(), productosSectionStart + 800);
                    String fragmento = ticketOutput.substring(start, end);
                    LOGGER.info("Fragmento XML alrededor de 'PRODUCTOS VENDIDOS DEL DÍA' (primeros 800 chars después):");
                    LOGGER.info(fragmento);
                }
                
                // Verificar que el XML sea válido buscando etiquetas bien formadas
                int lineCount = 0;
                int textCount = 0;
                int index = 0;
                while ((index = ticketOutput.indexOf("<line>", index)) != -1) {
                    lineCount++;
                    index += 6;
                }
                index = 0;
                while ((index = ticketOutput.indexOf("<text", index)) != -1) {
                    textCount++;
                    index += 5;
                }
                LOGGER.info("Conteo de etiquetas XML - <line>: " + lineCount + ", <text>: " + textCount);
                
                m_TTP.printTicket(ticketOutput);
// JG 16 May 2012 use multicatch
            } catch (ScriptException | TicketPrinterException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, 
                        AppLocal.getIntString("message.cannotprintticket"), e);
                msg.show(this);
            }
        }
    }
    
    /**
     * Clase auxiliar para almacenar datos de un turno
     */
    public static class ShiftData {
        public String money;
        public String host;
        public int sequence;
        public Date dateStart;
        public Date dateEnd;
        private double totalSales;
        private double totalPayments;
        public java.util.List<PaymentsModel.PaymentsLine> paymentLines;
        public java.util.List<PaymentsModel.ProductSalesLine> productLines;
        
        public ShiftData(String money, String host, int sequence, Date dateStart, Date dateEnd) {
            this.money = money;
            this.host = host;
            this.sequence = sequence;
            this.dateStart = dateStart;
            this.dateEnd = dateEnd;
            this.paymentLines = new java.util.ArrayList<>();
            this.productLines = new java.util.ArrayList<>();
        }
        
        public String getMoney() { return money; }
        public String getHost() { return host; }
        public int getSequence() { return sequence; }
        public Date getDateStart() { return dateStart; }
        public Date getDateEnd() { return dateEnd; }
        public double getTotalSales() { return totalSales; }
        public void setTotalSales(double total) { this.totalSales = total; }
        public double getTotalPayments() { return totalPayments; }
        public void setTotalPayments(double total) { this.totalPayments = total; }
        public java.util.List<PaymentsModel.PaymentsLine> getPaymentLines() { return paymentLines; }
        public java.util.List<PaymentsModel.ProductSalesLine> getProductLines() { return productLines; }
        
        public String printDateStart() {
            return Formats.TIMESTAMP.formatValue(dateStart);
        }
        
        public String printDateEnd() {
            return Formats.TIMESTAMP.formatValue(dateEnd);
        }
        
        public String printTotalSales() {
            return Formats.CURRENCY.formatValue(totalSales);
        }
        
        public String printTotalPayments() {
            return Formats.CURRENCY.formatValue(totalPayments);
        }
    }
    
    /**
     * Clase auxiliar para productos consolidados del día
     */
    public static class ConsolidatedProduct {
        private String name;
        private double totalUnits;
        private double totalValue;
        
        public ConsolidatedProduct(String name) {
            this.name = name;
            this.totalUnits = 0.0;
            this.totalValue = 0.0;
        }
        
        public void addUnits(double units) {
            this.totalUnits += units;
        }
        
        public void addTotal(double value) {
            this.totalValue += value;
        }
        
        public String getName() {
            return name;
        }
        
        public String printName() {
            return StringUtils.encodeXML(name);
        }
        
        public String printUnits() {
            return Formats.DOUBLE.formatValue(totalUnits);
        }
        
        public String printTotal() {
            return Formats.CURRENCY.formatValue(totalValue);
        }
        
        public double getTotalUnits() {
            return totalUnits;
        }
        
        public double getTotalValue() {
            return totalValue;
        }
    }
    
    /**
     * Obtiene todos los turnos cerrados del día con sus datos y productos vendidos
     */
    private java.util.List<ShiftData> getAllShiftsForDay(Date dayDate) {
        java.util.List<ShiftData> shifts = new java.util.ArrayList<>();
        
        try {
            Session session = m_App.getSession();
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
            String dayStr = dateFormat.format(dayDate);
            
            // Calcular inicio y fin del día para comparación
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(dayDate);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            java.sql.Timestamp dayStart = new java.sql.Timestamp(cal.getTimeInMillis());
            
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
            java.sql.Timestamp dayEnd = new java.sql.Timestamp(cal.getTimeInMillis());
            
            // Obtener todos los turnos cerrados del día (compatible con HSQLDB)
            String sql = "SELECT MONEY, HOST, HOSTSEQUENCE, DATESTART, DATEEND " +
                        "FROM closedcash " +
                        "WHERE DATEEND IS NOT NULL " +
                        "AND DATESTART >= ? AND DATESTART < ? " +
                        "ORDER BY HOSTSEQUENCE ASC";
            
            java.sql.PreparedStatement pstmt = session.getConnection().prepareStatement(sql);
            pstmt.setTimestamp(1, dayStart);
            pstmt.setTimestamp(2, dayEnd);
            java.sql.ResultSet rs = pstmt.executeQuery();
            
            LOGGER.info("Buscando turnos del día: " + dayStr + " (desde " + dayStart + " hasta " + dayEnd + ")");
            
            int shiftCount = 0;
            while (rs.next()) {
                shiftCount++;
                String money = rs.getString("MONEY");
                String host = rs.getString("HOST");
                int sequence = rs.getInt("HOSTSEQUENCE");
                Date dateStart = rs.getTimestamp("DATESTART");
                Date dateEnd = rs.getTimestamp("DATEEND");
                
                LOGGER.info("Procesando turno #" + sequence + " (MONEY: " + money + ")");
                
                ShiftData shift = new ShiftData(money, host, sequence, dateStart, dateEnd);
                
                // Obtener pagos del turno
                java.util.List<PaymentsModel.PaymentsLine> payments = new StaticSentence(session,
                    "SELECT payments.PAYMENT, SUM(payments.TOTAL), payments.NOTES, COUNT(payments.PAYMENT) " +
                    "FROM payments, receipts " +
                    "WHERE payments.RECEIPT = receipts.ID AND receipts.MONEY = ? " +
                    "GROUP BY payments.PAYMENT, payments.NOTES",
                    SerializerWriteString.INSTANCE,
                    new SerializerReadClass(PaymentsModel.PaymentsLine.class))
                    .list(money);
                
                if (payments != null && !payments.isEmpty()) {
                    shift.getPaymentLines().addAll(payments);
                    // Calcular total de pagos
                    double totalPayments = 0.0;
                    for (PaymentsModel.PaymentsLine pl : payments) {
                        totalPayments += pl.getValue();
                    }
                    shift.setTotalPayments(totalPayments);
                    LOGGER.info("Turno #" + sequence + ": " + payments.size() + " tipos de pago, Total: " + totalPayments);
                } else {
                    LOGGER.info("Turno #" + sequence + ": Sin pagos");
                }
                
                // Obtener productos vendidos del turno
                // Usar la misma consulta que PaymentsModel para consistencia
                try {
                    java.util.List<PaymentsModel.ProductSalesLine> products = new StaticSentence(session,
                        "SELECT products.NAME, SUM(ticketlines.UNITS), ticketlines.PRICE, taxes.RATE " +
                        "FROM ticketlines, tickets, receipts, products, taxes " +
                        "WHERE ticketlines.PRODUCT = products.ID " +
                        "AND ticketlines.TICKET = tickets.ID " +
                        "AND tickets.ID = receipts.ID " +
                        "AND ticketlines.TAXID = taxes.ID " +
                        "AND receipts.MONEY = ? " +
                        "GROUP BY products.NAME, ticketlines.PRICE, taxes.RATE",
                        SerializerWriteString.INSTANCE,
                        new SerializerReadClass(PaymentsModel.ProductSalesLine.class))
                        .list(money);
                    
                    LOGGER.info("Consulta productos ejecutada para turno #" + sequence + " (MONEY: " + money + ")");
                    
                    if (products != null) {
                        LOGGER.info("Productos obtenidos: " + products.size());
                        if (!products.isEmpty()) {
                            shift.getProductLines().addAll(products);
                            // Calcular total de ventas
                            double totalSales = 0.0;
                            int productIndex = 0;
                            for (PaymentsModel.ProductSalesLine psl : products) {
                                double priceWithTax = psl.getProductPrice() * (1.0 + psl.getTaxRate());
                                totalSales += priceWithTax * psl.getProductUnits();
                                LOGGER.info("  Producto " + (++productIndex) + ": " + psl.printProductName() + 
                                           " - Cantidad: " + psl.printProductUnits() + 
                                           " - Total: " + psl.printProductSubValue());
                            }
                            shift.setTotalSales(totalSales);
                            LOGGER.info("Turno #" + sequence + ": " + products.size() + " productos vendidos, Total ventas: " + totalSales);
                            LOGGER.info("Tamaño de productLines en shift después de agregar: " + shift.getProductLines().size());
                        } else {
                            LOGGER.warning("Turno #" + sequence + ": Lista de productos está vacía");
                        }
                    } else {
                        LOGGER.warning("Turno #" + sequence + ": products es null");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error obteniendo productos para turno #" + sequence + ": " + e.getMessage(), e);
                }
                
                shifts.add(shift);
            }
            
            LOGGER.info("Total de turnos encontrados: " + shiftCount);
            
            rs.close();
            pstmt.close();
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error obteniendo turnos del día: " + e.getMessage(), e);
        }
        
        return shifts;
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
    private JLabel m_jInflowsLabel; // Label para total de entradas
    private JLabel m_jOutflowsLabel; // Label para total de salidas
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
        
        // Título con diseño más moderno
        JLabel titleLabel = new JLabel("CORTE DE CAJA");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(new Color(30, 30, 30));
        leftPanel.add(titleLabel);
        leftPanel.add(Box.createVerticalStrut(10));
        
        // Botones de acción
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        // Botón Hacer corte de cajero con diseño mejorado
        JButton btnCashier = new JButton("Hacer corte de cajero");
        btnCashier.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCashier.setPreferredSize(new Dimension(200, 45));
        styleButton(btnCashier, new Color(33, 150, 243));
        btnCashier.addActionListener(e -> {
            LOGGER.info("Botón 'Hacer corte de cajero' presionado desde diseño moderno");
            // Llamar directamente al método en lugar de doClick() para asegurar que se ejecute
            m_jCloseCashActionPerformed(new java.awt.event.ActionEvent(btnCashier, java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
        });
        buttonPanel.add(btnCashier);
        
        // Botón Hacer corte del día con diseño mejorado
        JButton btnDay = new JButton("Hacer corte del día");
        btnDay.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDay.setPreferredSize(new Dimension(200, 45));
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
     * Crea una tarjeta de métrica con diseño moderno mejorado
     */
    private JPanel createMetricCard(String title, String value, Color color, String icon) {
        // Panel principal con sombra simulada
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Sombra suave
                g2.setColor(new Color(0, 0, 0, 10));
                g2.fillRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 12, 12);
                
                // Fondo con gradiente sutil
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(color.getRed(), color.getGreen(), color.getBlue(), 5),
                    0, getHeight(), new Color(color.getRed(), color.getGreen(), color.getBlue(), 15)
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 12, 12);
                
                g2.dispose();
            }
        };
        
        // Fondo con color suave basado en el color principal
        Color bgColor = new Color(
            Math.min(255, color.getRed() + 245),
            Math.min(255, color.getGreen() + 245),
            Math.min(255, color.getBlue() + 245)
        );
        card.setBackground(bgColor);
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30), 1),
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));
        
        // Panel principal con icono y título
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        
        // Icono y título con mejor espaciado
        JLabel titleLabel = new JLabel(icon + "  " + title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        titleLabel.setForeground(new Color(75, 85, 99));
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Valor (almacenar referencia para actualizar) - NÚMERO AÚN MÁS GRANDE
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 42)); // Aumentado de 36 a 42
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
        // Entradas - guardar referencia para actualizar
        m_jInflowsLabel = addCashLine(panel, "Entradas", "+$0.00", true, true);
        // Salidas - guardar referencia para actualizar
        m_jOutflowsLabel = addCashLine(panel, "Salidas", "-$0.00", false, false);
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
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Botón personalizado con gradiente y sombra
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.repaint();
            }
        });
        
        // Renderizado personalizado del botón
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                JButton btn = (JButton) c;
                boolean isHover = btn.getModel().isRollover();
                
                // Sombra
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(2, 2, btn.getWidth() - 4, btn.getHeight() - 4, 8, 8);
                
                // Gradiente
                Color startColor = isHover ? color.brighter() : color;
                Color endColor = isHover ? color : color.darker();
                GradientPaint gradient = new GradientPaint(
                    0, 0, startColor,
                    0, btn.getHeight(), endColor
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, btn.getWidth() - 2, btn.getHeight() - 2, 8, 8);
                
                // Borde
                g2.setColor(new Color(255, 255, 255, 30));
                g2.drawRoundRect(0, 0, btn.getWidth() - 2, btn.getHeight() - 2, 8, 8);
                
                g2.dispose();
                super.paint(g, c);
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
        
        // Detectar si viene del botón "Hacer corte del día"
        boolean isDayClose = false;
        if (evt.getSource() instanceof javax.swing.JButton) {
            javax.swing.JButton sourceButton = (javax.swing.JButton) evt.getSource();
            String buttonText = sourceButton.getText();
            if (buttonText != null && buttonText.contains("corte del día")) {
                isDayClose = true;
                LOGGER.info("Detectado: Cierre desde botón 'Hacer corte del día'");
            }
        }
        
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
                printPayments("Printer.CloseCash", isDayClose);

                // Si es "corte del día", siempre mostrar el reporte del día completo
                if (isDayClose) {
                    LOGGER.info("Mostrando reporte del día completo (corte del día)");
                    // Mostrar mensaje de éxito y luego el reporte
                    JOptionPane.showMessageDialog(this, 
                            AppLocal.getIntString("message.closecashok"), 
                            AppLocal.getIntString("message.title"), 
                            JOptionPane.INFORMATION_MESSAGE);
                    
                    // Mostrar reporte del día automáticamente
                    SwingUtilities.invokeLater(() -> {
                        showDayReport(dNow);
                    });
                } else {
                    // Mostrar mensaje normal si es cierre de turno normal
                    JOptionPane.showMessageDialog(this, 
                            AppLocal.getIntString("message.closecashok"), 
                            AppLocal.getIntString("message.title"), 
                            JOptionPane.INFORMATION_MESSAGE);
                }
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
    
    /**
     * Verifica si este es el último turno del día
     */
    private boolean isLastShiftOfDay(Date closeDate, String excludeCashIndex) {
        try {
            Connection conn = m_App.getSession().getConnection();
            
            // Crear rango de fechas para el día (inicio y fin del día)
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(closeDate);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            java.sql.Timestamp startOfDay = new java.sql.Timestamp(cal.getTimeInMillis());
            
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
            java.sql.Timestamp nextDayStart = new java.sql.Timestamp(cal.getTimeInMillis());
            
            // Contar turnos abiertos del día (que no tengan DATEEND)
            // Excluir el turno que estamos cerrando (excludeCashIndex)
            // Usar comparación de TIMESTAMP en lugar de DATE() para compatibilidad con HSQLDB
            String sql = "SELECT COUNT(*) FROM closedcash " +
                        "WHERE DATESTART >= ? AND DATESTART < ? AND DATEEND IS NULL AND HOST = ? AND MONEY != ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setTimestamp(1, startOfDay);
            pstmt.setTimestamp(2, nextDayStart);
            pstmt.setString(3, m_App.getProperties().getHost());
            pstmt.setString(4, excludeCashIndex);
            
            ResultSet rs = pstmt.executeQuery();
            int openShifts = 0;
            if (rs.next()) {
                openShifts = rs.getInt(1);
            }
            rs.close();
            pstmt.close();
            
            LOGGER.info("Verificación último turno del día: turnos abiertos (excluyendo el actual) = " + openShifts);
            
            // Si no hay turnos abiertos (además del que estamos cerrando), este era el último
            return openShifts == 0;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error verificando si es último turno del día", e);
            return false; // En caso de error, no mostrar reporte automáticamente
        }
    }
    
    /**
     * Muestra el reporte del día con todos los turnos cerrados
     */
    private void showDayReport(Date closeDate) {
        try {
            // Crear diálogo para mostrar el reporte
            JDialog reportDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                "Reporte de Caja Cerrada - Día Completo", true);
            reportDialog.setSize(1000, 700);
            reportDialog.setLocationRelativeTo(this);
            
            // Crear panel con el visor de reportes
            JPanel panel = new JPanel(new BorderLayout());
            JRViewer400 reportViewer = new JRViewer400(null);
            panel.add(reportViewer, BorderLayout.CENTER);
            
            // Panel de botones: Guardar PDF y Cerrar
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnSavePDF = new JButton("Guardar PDF");
            JButton btnClose = new JButton("Cerrar");
            
            // Variable para almacenar el JasperPrint generado
            final JasperPrint[] jpRef = new JasperPrint[1];
            
            btnSavePDF.addActionListener(e -> {
                if (jpRef[0] != null) {
                    try {
                        // Crear diálogo para elegir ubicación del archivo
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setDialogTitle("Guardar reporte como PDF");
                        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos PDF (*.pdf)", "pdf"));
                        
                        // Sugerir nombre de archivo con fecha
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                        String defaultFileName = "Reporte_Caja_Cerrada_" + sdf.format(closeDate) + ".pdf";
                        fileChooser.setSelectedFile(new java.io.File(defaultFileName));
                        
                        int userSelection = fileChooser.showSaveDialog(reportDialog);
                        if (userSelection == JFileChooser.APPROVE_OPTION) {
                            java.io.File fileToSave = fileChooser.getSelectedFile();
                            String filePath = fileToSave.getAbsolutePath();
                            
                            // Asegurar extensión .pdf
                            if (!filePath.toLowerCase().endsWith(".pdf")) {
                                filePath += ".pdf";
                            }
                            
                            // Exportar a PDF
                            JasperExportManager.exportReportToPdfFile(jpRef[0], filePath);
                            
                            JOptionPane.showMessageDialog(reportDialog,
                                "Reporte guardado exitosamente en:\n" + filePath,
                                "Éxito",
                                JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (JRException ex) {
                        LOGGER.log(Level.SEVERE, "Error guardando PDF", ex);
                        JOptionPane.showMessageDialog(reportDialog,
                            "Error al guardar el PDF: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(reportDialog,
                        "No hay reporte para guardar",
                        "Advertencia",
                        JOptionPane.WARNING_MESSAGE);
                }
            });
            
            btnClose.addActionListener(e -> reportDialog.dispose());
            
            buttonPanel.add(btnSavePDF);
            buttonPanel.add(btnClose);
            panel.add(buttonPanel, BorderLayout.SOUTH);
            
            reportDialog.add(panel);
            
            // Cargar y mostrar el reporte
            m_App.waitCursorBegin();
            
            try {
                // Compilar el reporte
                JasperReport jasperReport = JPanelReport.createJasperReport("/com/openbravo/reports/sales_closedpos");
                
                if (jasperReport != null) {
                    // Crear rango de fechas para el día (inicio y fin del día)
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(closeDate);
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    cal.set(java.util.Calendar.MINUTE, 0);
                    cal.set(java.util.Calendar.SECOND, 0);
                    cal.set(java.util.Calendar.MILLISECOND, 0);
                    java.sql.Timestamp startOfDay = new java.sql.Timestamp(cal.getTimeInMillis());
                    
                    cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
                    java.sql.Timestamp endOfDay = new java.sql.Timestamp(cal.getTimeInMillis());
                    
                    // Crear la consulta SQL con filtro directo de fechas (más simple y confiable)
                    // Incluimos initial_amount para mostrar los montos iniciales
                    String sentence = "SELECT " + 
                        "closedcash.HOST, " +
                        "closedcash.HOSTSEQUENCE, " +
                        "closedcash.MONEY, " +
                        "closedcash.DATESTART, " +
                        "closedcash.DATEEND, " +
                        "COALESCE(closedcash.initial_amount, 0.0) AS INITIAL_AMOUNT, " +
                        "COALESCE(payments.PAYMENT, 'Sin ventas') AS PAYMENT, " +
                        "COALESCE(SUM(payments.TOTAL), 0.0) AS TOTAL " +
                        "FROM closedcash " +
                        "LEFT JOIN receipts ON closedcash.MONEY = receipts.MONEY " +
                        "LEFT JOIN payments ON payments.RECEIPT = receipts.ID " +
                        "WHERE closedcash.DATEEND IS NOT NULL AND closedcash.DATEEND >= ? AND closedcash.DATEEND < ? " +
                        "GROUP BY closedcash.HOST, closedcash.HOSTSEQUENCE, closedcash.MONEY, closedcash.DATESTART, closedcash.DATEEND, closedcash.initial_amount, COALESCE(payments.PAYMENT, 'Sin ventas') " +
                        "ORDER BY closedcash.HOST, closedcash.HOSTSEQUENCE, closedcash.DATEEND DESC";
                    
                    // Crear sentence con parámetros directos de fecha
                    BaseSentence<Object[]> reportSentence = new StaticSentence<Object[], Object[]>(
                        m_App.getSession(),
                        sentence,
                        new SerializerWriteBasic(new Datas[] {Datas.TIMESTAMP, Datas.TIMESTAMP}),
                        new SerializerReadBasic(new Datas[] {
                            Datas.STRING, Datas.INT, Datas.STRING, 
                            Datas.TIMESTAMP, Datas.TIMESTAMP, Datas.DOUBLE, Datas.STRING, Datas.DOUBLE
                        })
                    );
                    
                    // Crear campos del reporte (incluyendo INITIAL_AMOUNT)
                    ReportFields reportFields = new ReportFieldsArray(
                        new String[] {"HOST", "HOSTSEQUENCE", "MONEY", "DATESTART", "DATEEND", "INITIAL_AMOUNT", "PAYMENT", "TOTAL"}
                    );
                    
                    // Crear parámetros de fecha en el formato que espera el reporte para ARG
                    // El formato es: [COMP_GREATEROREQUALS, startDate, COMP_LESS, endDate]
                    Object[] dateParams = new Object[] {
                        QBFCompareEnum.COMP_GREATEROREQUALS,
                        startOfDay,
                        QBFCompareEnum.COMP_LESS,
                        endOfDay
                    };
                    
                    // El parámetro ARG debe ser un array que contiene el array de fechas como primer elemento
                    // Esto es lo que espera el reporte: ((Object[])$P{ARG})[0])[1] y [3]
                    Object[] arg = new Object[] { dateParams };
                    
                    // Crear fuente de datos con los parámetros de fecha directos
                    JRDataSourceBasic dataSource = new JRDataSourceBasic(
                        reportSentence, 
                        reportFields, 
                        new Object[] {startOfDay, endOfDay}
                    );
                    
                    // Parámetros del reporte
                    Map<String, Object> reportParams = new HashMap<>();
                    reportParams.put("ARG", arg);
                    
                    // Cargar bundle en español si existe, sino usar el por defecto
                    ResourceBundle bundle = null;
                    try {
                        // Intentar cargar el bundle en español
                        bundle = ResourceBundle.getBundle(
                            "com.openbravo.reports.sales_closedpos_messages_es", 
                            Locale.getDefault()
                        );
                    } catch (MissingResourceException e) {
                        // Si no existe el bundle en español, usar el por defecto
                        try {
                            bundle = ResourceBundle.getBundle(
                                "com.openbravo.reports.sales_closedpos_messages",
                                Locale.getDefault()
                            );
                        } catch (MissingResourceException ex) {
                            LOGGER.log(Level.WARNING, "No se pudo cargar el bundle de recursos", ex);
                        }
                    }
                    if (bundle != null) {
                        reportParams.put("REPORT_RESOURCE_BUNDLE", bundle);
                    }
                    
                    // Generar el reporte
                    JasperPrint jp = JasperFillManager.fillReport(jasperReport, reportParams, dataSource);
                    jpRef[0] = jp; // Guardar referencia para el botón de PDF
                    reportViewer.loadJasperPrint(jp);
                    
                    // Mostrar el diálogo
                    reportDialog.setVisible(true);
                }
            } catch (JRException | BasicException e) {
                LOGGER.log(Level.SEVERE, "Error generando reporte", e);
                JOptionPane.showMessageDialog(reportDialog,
                    "Error al generar el reporte: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                reportDialog.dispose();
            } finally {
                m_App.waitCursorEnd();
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error mostrando reporte del día", e);
            JOptionPane.showMessageDialog(this,
                "Error al mostrar el reporte: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
}
