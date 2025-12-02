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
            // Actualizar HTML si existe el viewer
            if (m_htmlViewer != null) {
                String htmlContent = generateHTMLContent();
                m_htmlViewer.setText(htmlContent);
            }
            
            // Mantener compatibilidad con componentes Swing antiguos si existen
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
                    // DEBUG: also show a popup to confirm results for users who run day close
                    try {
                        SwingUtilities.invokeLater(() -> {
                            StringBuilder msg = new StringBuilder();
                            msg.append("Cantidad de turnos encontrados: ").append(allShifts != null ? allShifts.size() : 0).append("\n");
                            int totalProducts = 0;
                            if (allShifts != null) {
                                for (ShiftData s : allShifts) totalProducts += s.getProductLines().size();
                            }
                            msg.append("Total productos por día encontrados: ").append(totalProducts).append("\n");
                            if (totalProducts == 0) {
                                msg.append("No se encontraron productos. Revisa el registro de errores o la base de datos.");
                            }
                            JOptionPane.showMessageDialog(this, msg.toString(), "Depuración: Corte del Día", JOptionPane.INFORMATION_MESSAGE);
                        });
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "No se pudo mostrar popup de debug: " + e.getMessage(), e);
                    }
                    
                    // Obtener el fondo inicial del primer turno del día
                    double initialAmount = 0.0;
                    if (allShifts != null && !allShifts.isEmpty()) {
                        try {
                            String firstShiftMoney = allShifts.get(0).getMoney();
                            Session session = m_App.getSession();
                            java.sql.Connection con = session.getConnection();
                            String sql = "SELECT INITIAL_AMOUNT FROM closedcash WHERE MONEY = ?";
                            java.sql.PreparedStatement pstmt = con.prepareStatement(sql);
                            pstmt.setString(1, firstShiftMoney);
                            java.sql.ResultSet rs = pstmt.executeQuery();
                            if (rs.next()) {
                                double dbInitial = rs.getDouble("INITIAL_AMOUNT");
                                if (!rs.wasNull()) {
                                    initialAmount = dbInitial;
                                    LOGGER.info("Fondo inicial obtenido del primer turno: " + initialAmount);
                                }
                            }
                            rs.close();
                            pstmt.close();
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error obteniendo fondo inicial: " + e.getMessage(), e);
                        }
                    }
                    
                    // Calcular totales del día y consolidar productos
                    double totalDaySales = 0.0;
                    double totalDayPayments = 0.0;
                    java.util.Map<String, ConsolidatedProduct> consolidatedProducts = new java.util.HashMap<>();

                    // Consolidar productos por DEPARTAMENTO (categoría) en lugar de por producto individual
                    // Primero, obtener las ventas agrupadas por categoría directamente desde la base de datos
                    java.util.Map<String, ConsolidatedProduct> categoryConsolidatedProducts = new java.util.HashMap<>();
                    
                    if (allShifts != null && !allShifts.isEmpty()) {
                        try {
                            Session session = m_App.getSession();
                            // Obtener todas las MONEY de los turnos del día
                            java.util.List<String> moneyList = new java.util.ArrayList<>();
                        for (ShiftData shift : allShifts) {
                                if (shift.getMoney() != null && !shift.getMoney().isEmpty()) {
                                    moneyList.add(shift.getMoney());
                                }
                            }
                            
                            if (!moneyList.isEmpty()) {
                                // Consulta SQL para obtener ventas agrupadas por categoría (departamento)
                                // Para cada MONEY, obtener las ventas por categoría
                                for (String money : moneyList) {
                                    java.util.List<CategorySalesData> categorySales = new StaticSentence(session,
                                        "SELECT COALESCE(categories.NAME, 'Sin Departamento') as CATEGORY_NAME, " +
                                        "SUM(ticketlines.UNITS) as TOTAL_UNITS, " +
                                        "SUM((ticketlines.PRICE + ticketlines.PRICE * taxes.RATE) * ticketlines.UNITS) as TOTAL_VALUE " +
                                        "FROM ticketlines " +
                                        "INNER JOIN tickets ON ticketlines.TICKET = tickets.ID " +
                                        "INNER JOIN receipts ON tickets.ID = receipts.ID " +
                                        "INNER JOIN products ON ticketlines.PRODUCT = products.ID " +
                                        "LEFT JOIN categories ON products.CATEGORY = categories.ID " +
                                        "INNER JOIN taxes ON ticketlines.TAXID = taxes.ID " +
                                        "WHERE receipts.MONEY = ? " +
                                        "GROUP BY COALESCE(categories.NAME, 'Sin Departamento')",
                                        SerializerWriteString.INSTANCE,
                                        new SerializerReadClass(CategorySalesData.class))
                                        .list(money);
                                    
                                    if (categorySales != null) {
                                        for (CategorySalesData catSale : categorySales) {
                                            String categoryName = catSale.getCategoryName();
                                            if (categoryName == null || categoryName.trim().isEmpty()) {
                                                categoryName = "Sin Departamento";
                                            }
                                            
                                            ConsolidatedProduct consolidated = categoryConsolidatedProducts.get(categoryName);
                                            if (consolidated == null) {
                                                consolidated = new ConsolidatedProduct(categoryName);
                                                categoryConsolidatedProducts.put(categoryName, consolidated);
                                            }
                                            
                                            double units = catSale.getTotalUnits() != null ? catSale.getTotalUnits() : 0.0;
                                            double totalValue = catSale.getTotalValue() != null ? catSale.getTotalValue() : 0.0;
                                            
                                            consolidated.addUnits(units);
                                            consolidated.addTotal(totalValue);
                                            LOGGER.info("Consolidated category from MONEY " + money + ": " + categoryName + " - Units: " + units + " - Value: " + totalValue);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error consolidando por categoría, usando método anterior: " + e.getMessage(), e);
                            // Fallback: consolidar por producto si falla la consulta por categoría
                            for (ShiftData shift : allShifts) {
                            for (PaymentsModel.ProductSalesLine product : shift.getProductLines()) {
                                String productName = product.getProductName();
                                if (productName == null || productName.trim().isEmpty()) continue;
                                
                                ConsolidatedProduct consolidated = consolidatedProducts.get(productName);
                                if (consolidated == null) {
                                    consolidated = new ConsolidatedProduct(productName);
                                    consolidatedProducts.put(productName, consolidated);
                                }
                                
                                double units = product.getProductUnits() != null ? product.getProductUnits() : 0.0;
                                double price = product.getProductPrice() != null ? product.getProductPrice() : 0.0;
                                double taxRate = product.getTaxRate() != null ? product.getTaxRate() : 0.0;
                                double totalValue = price * (1.0 + taxRate) * units;
                                
                                consolidated.addUnits(units);
                                consolidated.addTotal(totalValue);
                                }
                            }
                        }
                        
                        // Calcular totales del día
                        for (ShiftData shift : allShifts) {
                            totalDaySales += shift.getTotalSales();
                            totalDayPayments += shift.getTotalPayments();
                        }
                        
                        // Usar productos consolidados por categoría si están disponibles
                        if (!categoryConsolidatedProducts.isEmpty()) {
                            consolidatedProducts = categoryConsolidatedProducts;
                            LOGGER.info("Usando consolidación por DEPARTAMENTO: " + consolidatedProducts.size() + " departamentos");
                        } else {
                            LOGGER.info("Usando consolidación por PRODUCTO (fallback): " + consolidatedProducts.size() + " productos");
                        }
                    }
                    
                    LOGGER.info("Total productos consolidados desde turnos: " + consolidatedProducts.size());
                    
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
                        productosText.append("    <text align=\"left\" length=\"42\">Sin ventas por departamento</text>\n");
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
                    script.put("initialAmount", Formats.CURRENCY.formatValue(initialAmount));
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
                    // Prepare replacement string for consolidated products
                    String replacement = "";
                    try {
                        StringBuilder productosXMLConsolidados = new StringBuilder();
                        for (ConsolidatedProduct product : consolidatedProductList) {
                            String name = product.printName();
                            if (name.length() > 25) name = name.substring(0, 22) + "...";
                            productosXMLConsolidados.append("        <line>\n");
                            productosXMLConsolidados.append("            <text align =\"left\" length=\"25\">" + StringUtils.encodeXML(name) + "</text>\n");
                            productosXMLConsolidados.append("            <text align =\"right\" length=\"8\">" + product.printUnits() + "</text>\n");
                            productosXMLConsolidados.append("            <text align =\"right\" length=\"9\">" + product.printTotal() + "</text>\n");
                            productosXMLConsolidados.append("        </line>\n");
                        }
                        replacement = productosXMLConsolidados.toString();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error creando replacement para productos consolidados: " + e.getMessage(), e);
                    }
                    String placeholder = "<!-- PRODUCTOS_PLACEHOLDER -->";
                    int placeholderIndex = ticketOutput.indexOf(placeholder);
                    if (placeholderIndex >= 0) {
                        
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
                        
                        // If placeholder present, replace comment with the generated lines directly (replacement already prepared)
                        ticketOutput = ticketOutput.replace(placeholder, replacement);
                        // Also remove the default "Sin productos vendidos" block if present after replacement
                        String defaultNoProducts = "<line>\n" +
                                "            <text align =\"left\" length=\"42\">Sin productos vendidos</text>\n" +
                                "        </line>\n";
                        if (ticketOutput.contains(defaultNoProducts)) {
                            ticketOutput = ticketOutput.replace(defaultNoProducts, "");
                        }
                        LOGGER.info("Productos consolidados insertados en template: " + consolidatedProductList.size());
                        LOGGER.info("Reemplazo exitoso - Longitud resultante: " + ticketOutput.length());
                        // Log del fragmento que se está reemplazando
                        if (startIndex >= 0 && lineEnd > startIndex) {
                            String fragmentoReemplazado = ticketOutput.substring(startIndex, Math.min(startIndex + 200, lineEnd));
                            LOGGER.info("Fragmento reemplazado (primeros 200 chars): " + fragmentoReemplazado.replace("\n", "\\n").replace("\r", "\\r"));
                            // Verificar que alguno de los productos aparezca en el ticket después del reemplazo
                            boolean anyFound = false;
                            for (ConsolidatedProduct cp : consolidatedProductList) {
                                if (ticketOutput.indexOf(cp.getName()) >= 0) {
                                    anyFound = true; break;
                                }
                            }
                            LOGGER.info("Verificación: ¿Algún producto consolidado encontrado en ticket?: " + anyFound);
                            if (!anyFound) {
                                // As a last resort, append the products list at the end of the ticket (just before </ticket>)
                                try {
                                    StringBuilder appendSection = new StringBuilder();
                                    appendSection.append("\n<line size=\"1\">\n");
                                    appendSection.append("    <text align =\"center\" bold=\"true\" length=\"42\">VENTAS POR DEPARTAMENTO</text>\n");
                                    appendSection.append("</line>\n");
                                    appendSection.append("<line>\n");
                                    appendSection.append("   <text align =\"left\" bold=\"true\" length=\"25\">Departamento</text>\n");
                                    appendSection.append("   <text align =\"right\" bold=\"true\" length=\"8\">Cantidad</text>\n");
                                    appendSection.append("   <text align =\"right\" bold=\"true\" length=\"9\">Total</text>\n");
                                    appendSection.append("</line>\n");
                                    appendSection.append(replacement);
                                    int idx = ticketOutput.lastIndexOf("</ticket>");
                                    if (idx >= 0) {
                                        ticketOutput = ticketOutput.substring(0, idx) + appendSection.toString() + ticketOutput.substring(idx);
                                        LOGGER.info("Append fallback: se agregó la lista de productos al final del ticket.");
                                    } else {
                                        ticketOutput += appendSection.toString();
                                        LOGGER.info("Append fallback: se agregó la lista de productos al final del output (no se encontró </ticket>).");
                                    }
                                } catch (Exception e) {
                                    LOGGER.log(Level.WARNING, "Error appending consolidated products as fallback: " + e.getMessage(), e);
                                }
                            }
                        } else {
                            LOGGER.warning("No se pudo encontrar la línea a reemplazar después del placeholder PRODUCTOS_PLACEHOLDER. lineStart: " + lineStart + ", lineEnd: " + lineEnd);
                            LOGGER.warning("Fragmento después del placeholder (primeros 200 chars): " + ticketOutput.substring(Math.max(0, searchStart), Math.min(ticketOutput.length(), searchStart + 200)));
                            // Try fallback: directly replace the default no-products line
                            String defaultNoProductsFallback = "<line>\n" +
                                    "            <text align =\"left\" length=\"42\">Sin productos vendidos</text>\n" +
                                    "        </line>\n";
                            if (ticketOutput.contains(defaultNoProductsFallback)) {
                                ticketOutput = ticketOutput.replace(defaultNoProductsFallback, replacement);
                                LOGGER.info("Fallback: default no-products replaced with consolidated products");
                            } else if (ticketOutput.indexOf("Sin productos vendidos") >= 0) {
                                // find the line block containing the phrase and replace it
                                int pos = ticketOutput.indexOf("Sin productos vendidos");
                                int startLine = ticketOutput.lastIndexOf("<line", pos);
                                int endLine = ticketOutput.indexOf("</line>", pos);
                                if (startLine >= 0 && endLine >= 0) {
                                    endLine += 7; // include closing tag
                                    ticketOutput = ticketOutput.substring(0, startLine) + replacement + ticketOutput.substring(endLine);
                                    LOGGER.info("Fallback: replaced generic 'Sin productos vendidos' line with consolidated products");
                                } else {
                                    LOGGER.warning("Fallback: no se pudo localizar el bloque <line> que contiene 'Sin productos vendidos'");
                                }
                            } else {
                                LOGGER.warning("Fallback: no default no-products placeholder found to replace");
                            }
                        }
                    } else {
                        LOGGER.warning("No se encontró el marcador PRODUCTOS_PLACEHOLDER en el template");
                        // Fallback: replace default text line directly if exists
                        String defaultNoProducts = "<line>\n" +
                                "            <text align =\"left\" length=\"42\">Sin productos vendidos</text>\n" +
                                "        </line>\n";
                        if (ticketOutput.contains(defaultNoProducts)) {
                            ticketOutput = ticketOutput.replace(defaultNoProducts, replacement);
                            LOGGER.info("Fallback: Replaced default 'Sin productos vendidos' with products list");
                        } else {
                            LOGGER.warning("Fallback: default 'Sin productos vendidos' not found in template for replacement");
                        }
                    }
                }
                
                // Reemplazar productos del turno en cierre de turno
                if (!isDayClose && currentShiftProducts != null && !currentShiftProducts.isEmpty()) {
                    String placeholderTurno = "<!-- PRODUCTOS_TURNO_PLACEHOLDER -->";
                    int placeholderIndex = ticketOutput.indexOf(placeholderTurno);
                    // Prepare productosTurnoXML regardless of placeholder presence
                    StringBuilder productosTurnoXML = new StringBuilder();
                    for (PaymentsModel.ProductSalesLine product : currentShiftProducts) {
                        String name = product.printProductName();
                        if (name.length() > 25) {
                            name = name.substring(0, 22) + "...";
                        }
                        productosTurnoXML.append("        <line>\n");
                        productosTurnoXML.append("            <text align =\"left\" length=\"25\">" + StringUtils.encodeXML(name) + "</text>\n");
                        productosTurnoXML.append("            <text align =\"right\" length=\"8\">" + product.printProductUnits() + "</text>\n");
                        productosTurnoXML.append("            <text align =\"right\" length=\"9\">" + product.printProductSubValue() + "</text>\n");
                        productosTurnoXML.append("        </line>\n");
                    }
                    if (placeholderIndex >= 0) {
                        // productosTurnoXML ya está creado arriba, solo crear replacement
                        // Crear replacement string
                        String replacementTurno = productosTurnoXML.toString();
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
                        
                        if (lineStart >= 0 && lineEnd > lineStart) {
                            // Reemplazar desde el placeholder (incluyéndolo) hasta el final de la línea "Sin productos vendidos en este turno"
                            String before = ticketOutput.substring(0, startIndex);
                            String after = ticketOutput.substring(lineEnd);
                            // El XML generado ya tiene la indentación correcta (8 espacios)
                            ticketOutput = before + replacementTurno + after;
                            LOGGER.info("Productos del turno insertados en template: " + currentShiftProducts.size());
                            LOGGER.info("Reemplazo exitoso - Longitud resultante: " + ticketOutput.length());
                            // Verificar que el primer producto esté en el ticket después del reemplazo
                            if (!currentShiftProducts.isEmpty()) {
                                String primerProducto = currentShiftProducts.get(0).printProductName();
                                int productoIndex = ticketOutput.indexOf(primerProducto);
                                LOGGER.info("Verificación: Primer producto encontrado en ticket después del reemplazo: " + (productoIndex >= 0) + " (posición: " + productoIndex + ")");
                            }
                        } else {
                            LOGGER.warning("No se pudo encontrar la línea a reemplazar después del placeholder PRODUCTOS_TURNO_PLACEHOLDER. lineStart: " + lineStart + ", lineEnd: " + lineEnd);
                            LOGGER.warning("Fragmento después del placeholder (primeros 200 chars): " + ticketOutput.substring(Math.max(0, searchStart), Math.min(ticketOutput.length(), searchStart + 200)));
                            String defaultNoProductsTurno = "<line>\n" +
                                    "            <text align =\"left\" length=\"42\">Sin productos vendidos en este turno</text>\n" +
                                    "        </line>\n";
                            String replacementTurnoFallback = productosTurnoXML.toString();
                            if (ticketOutput.contains(defaultNoProductsTurno)) {
                                ticketOutput = ticketOutput.replace(defaultNoProductsTurno, replacementTurnoFallback);
                                LOGGER.info("Fallback: default no-products turno replaced with products list");
                            } else if (ticketOutput.indexOf("Sin productos vendidos en este turno") >= 0) {
                                int pos = ticketOutput.indexOf("Sin productos vendidos en este turno");
                                int startLine = ticketOutput.lastIndexOf("<line", pos);
                                int endLine = ticketOutput.indexOf("</line>", pos);
                                if (startLine >= 0 && endLine >= 0) {
                                    endLine += 7;
                                    ticketOutput = ticketOutput.substring(0, startLine) + replacementTurnoFallback + ticketOutput.substring(endLine);
                                    LOGGER.info("Fallback: replaced generic 'Sin productos vendidos en este turno' line with products list");
                                } else {
                                    LOGGER.warning("Fallback: no se pudo localizar el bloque <line> para 'Sin productos vendidos en este turno'");
                                }
                            } else {
                                LOGGER.warning("Fallback: default 'Sin productos vendidos en este turno' not found for replacement");
                            }
                        }
                    } else {
                        LOGGER.warning("No se encontró el marcador PRODUCTOS_TURNO_PLACEHOLDER en el template");
                        String defaultNoProductsTurno = "<line>\n" +
                                "            <text align =\"left\" length=\"42\">Sin productos vendidos en este turno</text>\n" +
                                "        </line>\n";
                        String replacementTurno2 = productosTurnoXML.toString();
                        if (ticketOutput.contains(defaultNoProductsTurno)) {
                            ticketOutput = ticketOutput.replace(defaultNoProductsTurno, replacementTurno2);
                            LOGGER.info("Fallback: Replaced default 'Sin productos vendidos en este turno' with products list");
                        } else {
                            LOGGER.warning("Fallback: default 'Sin productos vendidos en este turno' not found in template for replacement");
                        }
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
                
                // Save output to temp file for debugging
                try {
                    String tmpdir = System.getProperty("java.io.tmpdir");
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmssSSS");
                    String fname = "ticket_close_debug_" + sdf.format(new java.util.Date()) + ".xml";
                    java.io.File outFile = new java.io.File(tmpdir, fname);
                    try (java.io.FileWriter fw = new java.io.FileWriter(outFile)) {
                        fw.write(ticketOutput);
                    }
                    LOGGER.info("Ticket output debug saved to: " + outFile.getAbsolutePath());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error saving ticket output debug file: " + e.getMessage(), e);
                }

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
     * Genera el reporte del corte del día directamente desde Java, sin usar templates XML
     * Incluye: turno, usuario, productos vendidos, totales, fondo inicial, entradas, salidas
     */
    private void generateDayCloseReportDirect(Date dayDate) {
        try {
            LOGGER.info("Generando reporte del corte del día directamente desde Java...");
            
            // Obtener todos los turnos del día
            java.util.List<ShiftData> allShifts = getAllShiftsForDay(dayDate);
            if (allShifts == null || allShifts.isEmpty()) {
                LOGGER.warning("No se encontraron turnos para el día");
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, 
                        "No se encontraron turnos para el día seleccionado");
                msg.show(this);
                return;
            }
            
            // Calcular totales del día
            double totalDayInitialAmount = 0.0;
            double totalDaySales = 0.0;
            double totalDayPayments = 0.0;
            double totalDayCashIn = 0.0;
            double totalDayCashOut = 0.0;
            
            // Consolidar productos por DEPARTAMENTO para todo el día
            java.util.Map<String, ConsolidatedProduct> dayCategoryConsolidated = new java.util.HashMap<>();
            
            try {
                Session session = m_App.getSession();
                // Obtener todas las MONEY de los turnos del día
                java.util.List<String> moneyList = new java.util.ArrayList<>();
                for (ShiftData shift : allShifts) {
                    if (shift.getMoney() != null && !shift.getMoney().isEmpty()) {
                        moneyList.add(shift.getMoney());
                    }
                    totalDayInitialAmount += shift.getInitialAmount();
                    totalDaySales += shift.getTotalSales();
                    totalDayPayments += shift.getTotalPayments();
                    totalDayCashIn += shift.getCashIn();
                    totalDayCashOut += shift.getCashOut();
                }
                
                // Obtener ventas agrupadas por departamento para todos los turnos del día
                if (!moneyList.isEmpty()) {
                    for (String money : moneyList) {
                        java.util.List<CategorySalesData> categorySales = new StaticSentence(session,
                            "SELECT COALESCE(categories.NAME, 'Sin Departamento') as CATEGORY_NAME, " +
                            "SUM(ticketlines.UNITS) as TOTAL_UNITS, " +
                            "SUM((ticketlines.PRICE + ticketlines.PRICE * taxes.RATE) * ticketlines.UNITS) as TOTAL_VALUE " +
                            "FROM ticketlines " +
                            "INNER JOIN tickets ON ticketlines.TICKET = tickets.ID " +
                            "INNER JOIN receipts ON tickets.ID = receipts.ID " +
                            "INNER JOIN products ON ticketlines.PRODUCT = products.ID " +
                            "LEFT JOIN categories ON products.CATEGORY = categories.ID " +
                            "INNER JOIN taxes ON ticketlines.TAXID = taxes.ID " +
                            "WHERE receipts.MONEY = ? " +
                            "GROUP BY COALESCE(categories.NAME, 'Sin Departamento')",
                            SerializerWriteString.INSTANCE,
                            new SerializerReadClass(CategorySalesData.class))
                            .list(money);
                        
                        if (categorySales != null) {
                            for (CategorySalesData catSale : categorySales) {
                                String categoryName = catSale.getCategoryName();
                                if (categoryName == null || categoryName.trim().isEmpty()) {
                                    categoryName = "Sin Departamento";
                                }
                                
                                ConsolidatedProduct consolidated = dayCategoryConsolidated.get(categoryName);
                                if (consolidated == null) {
                                    consolidated = new ConsolidatedProduct(categoryName);
                                    dayCategoryConsolidated.put(categoryName, consolidated);
                                }
                                
                                double units = catSale.getTotalUnits() != null ? catSale.getTotalUnits() : 0.0;
                                double totalValue = catSale.getTotalValue() != null ? catSale.getTotalValue() : 0.0;
                                
                                consolidated.addUnits(units);
                                consolidated.addTotal(totalValue);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error consolidando por departamento en generateDayCloseReportDirect: " + e.getMessage(), e);
            }
            
            // Construir el documento XML directamente
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<output>\n");
            xml.append("  <ticket>\n");
            xml.append("    <image>Printer.Ticket.Logo</image>\n");
            xml.append("    <line></line>\n");
            xml.append("    <line></line>\n");
            xml.append("    <line size=\"1\">\n");
            xml.append("      <text align=\"center\" bold=\"true\" length=\"42\">CORTE DEL DÍA</text>\n");
            xml.append("    </line>\n");
            xml.append("    <line></line>\n");
            
            // Mostrar información de turnos
            for (ShiftData shift : allShifts) {
                xml.append("    <line size=\"1\">\n");
                xml.append("      <text align=\"center\" bold=\"true\" length=\"42\">TURNO ").append(shift.getSequence()).append(" - ").append(StringUtils.encodeXML(shift.getUserName())).append("</text>\n");
                xml.append("    </line>\n");
                xml.append("    <line>\n");
                xml.append("      <text align=\"left\" length=\"42\">Inicio: ").append(shift.printDateStart()).append("</text>\n");
                xml.append("    </line>\n");
                xml.append("    <line>\n");
                xml.append("      <text align=\"left\" length=\"42\">Fin: ").append(shift.printDateEnd()).append("</text>\n");
                xml.append("    </line>\n");
                xml.append("    <line><text>------------------------------------------</text></line>\n");
            }
            
            // Mostrar VENTAS POR DEPARTAMENTO del día completo (consolidadas de todos los turnos)
            xml.append("    <line size=\"1\">\n");
            xml.append("      <text align=\"center\" bold=\"true\" length=\"42\">VENTAS POR DEPARTAMENTO</text>\n");
            xml.append("    </line>\n");
            xml.append("    <line>\n");
            xml.append("      <text align=\"left\" bold=\"true\" length=\"25\">Departamento</text>\n");
            xml.append("      <text align=\"right\" bold=\"true\" length=\"8\">Cantidad</text>\n");
            xml.append("      <text align=\"right\" bold=\"true\" length=\"9\">Total</text>\n");
            xml.append("    </line>\n");
            xml.append("    <line><text>------------------------------------------</text></line>\n");
            
            // Lista de departamentos consolidados
            if (!dayCategoryConsolidated.isEmpty()) {
                // Ordenar por nombre de departamento
                java.util.List<ConsolidatedProduct> sortedCategories = new java.util.ArrayList<>(dayCategoryConsolidated.values());
                java.util.Collections.sort(sortedCategories, (a, b) -> a.getName().compareTo(b.getName()));
                
                for (ConsolidatedProduct category : sortedCategories) {
                    String categoryName = StringUtils.encodeXML(category.getName());
                    if (categoryName.length() > 25) {
                        categoryName = categoryName.substring(0, 22) + "...";
                    }
                    xml.append("    <line>\n");
                    xml.append("      <text align=\"left\" length=\"25\">").append(categoryName).append("</text>\n");
                    xml.append("      <text align=\"right\" length=\"8\">").append(category.printUnits()).append("</text>\n");
                    xml.append("      <text align=\"right\" length=\"9\">").append(category.printTotal()).append("</text>\n");
                    xml.append("    </line>\n");
                }
            } else {
                xml.append("    <line>\n");
                xml.append("      <text align=\"left\" length=\"42\">Sin ventas por departamento</text>\n");
                xml.append("    </line>\n");
            }
            
            xml.append("    <line><text>------------------------------------------</text></line>\n");
            
            // Mostrar totales por turno
            for (ShiftData shift : allShifts) {
                // Calcular total del turno (fondo inicial + ventas + entradas - salidas)
                double totalTurno = shift.getInitialAmount() + shift.getTotalSales() + shift.getCashIn() - shift.getCashOut();
                
                xml.append("    <line size=\"1\">\n");
                xml.append("      <text align=\"left\" bold=\"true\" length=\"42\">TURNO ").append(shift.getSequence()).append(" - Total Ventas:</text>\n");
                xml.append("    </line>\n");
                xml.append("    <line>\n");
                xml.append("      <text align=\"left\" length=\"30\" bold=\"true\">Total de Turno:</text>\n");
                xml.append("      <text align=\"right\" length=\"12\" bold=\"true\">").append(shift.printTotalSales()).append("</text>\n");
                xml.append("    </line>\n");
                xml.append("    <line>\n");
                xml.append("      <text align=\"left\" length=\"30\" bold=\"true\">Monto Inicial:</text>\n");
                xml.append("      <text align=\"right\" length=\"12\" bold=\"true\">").append(shift.printInitialAmount()).append("</text>\n");
                xml.append("    </line>\n");
                xml.append("    <line><text>------------------------------------------</text></line>\n");
            }
            
            // Resumen total del día
            double totalDia = totalDayInitialAmount + totalDaySales + totalDayCashIn - totalDayCashOut;
            
            xml.append("    <line><text>==========================================</text></line>\n");
            xml.append("    <line size=\"1\">\n");
            xml.append("      <text align=\"center\" bold=\"true\" length=\"42\">TOTAL DEL DÍA</text>\n");
            xml.append("    </line>\n");
            xml.append("    <line><text>==========================================</text></line>\n");
            xml.append("    <line>\n");
            xml.append("      <text align=\"left\" length=\"30\" bold=\"true\">Total del Día:</text>\n");
            xml.append("      <text align=\"right\" length=\"12\" bold=\"true\">").append(Formats.CURRENCY.formatValue(totalDia)).append("</text>\n");
            xml.append("    </line>\n");
            xml.append("    <line><text>==========================================</text></line>\n");
            xml.append("    <line></line>\n");
            xml.append("    <line></line>\n");
            
            xml.append("  </ticket>\n");
            xml.append("</output>\n");
            
            String ticketOutput = xml.toString();
            LOGGER.info("Reporte del corte del día generado - Longitud: " + ticketOutput.length() + " caracteres");
            LOGGER.info("Total turnos procesados: " + allShifts.size());
            LOGGER.info("Total del día - Ventas: " + totalDaySales + ", Pagos: " + totalDayPayments);
            
            // Imprimir el documento
            m_TTP.printTicket(ticketOutput);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generando reporte del corte del día: " + e.getMessage(), e);
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, 
                    "Error generando reporte del corte del día: " + e.getMessage(), e);
            msg.show(this);
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
        private String userName;
        private double totalSales;
        private double totalPayments;
        private double initialAmount;
        private double cashIn;
        private double cashOut;
        public java.util.List<PaymentsModel.PaymentsLine> paymentLines;
        public java.util.List<PaymentsModel.ProductSalesLine> productLines;
        
        public ShiftData(String money, String host, int sequence, Date dateStart, Date dateEnd, String userName) {
            this.money = money;
            this.host = host;
            this.sequence = sequence;
            this.dateStart = dateStart;
            this.dateEnd = dateEnd;
            this.userName = userName != null ? userName : "admin";
            this.initialAmount = 0.0;
            this.cashIn = 0.0;
            this.cashOut = 0.0;
            this.paymentLines = new java.util.ArrayList<>();
            this.productLines = new java.util.ArrayList<>();
        }
        
        public String getUserName() {
            return userName != null ? userName : "admin";
        }
        
        public String printUserName() {
            return StringUtils.encodeXML(getUserName());
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
        public double getInitialAmount() { return initialAmount; }
        public void setInitialAmount(double amount) { this.initialAmount = amount; }
        public double getCashIn() { return cashIn; }
        public void setCashIn(double amount) { this.cashIn = amount; }
        public double getCashOut() { return cashOut; }
        public void setCashOut(double amount) { this.cashOut = amount; }
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
        
        public String printInitialAmount() {
            return Formats.CURRENCY.formatValue(initialAmount);
        }
        
        public String printCashIn() {
            return Formats.CURRENCY.formatValue(cashIn);
        }
        
        public String printCashOut() {
            return Formats.CURRENCY.formatValue(cashOut);
        }
    }
    
    /**
     * Clase auxiliar para leer datos de ventas por categoría desde la base de datos
     */
    public static class CategorySalesData implements com.openbravo.data.loader.SerializableRead {
        private String categoryName;
        private Double totalUnits;
        private Double totalValue;
        
        @Override
        public void readValues(com.openbravo.data.loader.DataRead dr) throws com.openbravo.basic.BasicException {
            categoryName = dr.getString(1);
            totalUnits = dr.getDouble(2);
            totalValue = dr.getDouble(3);
        }
        
        public String getCategoryName() {
            return categoryName;
        }
        
        public Double getTotalUnits() {
            return totalUnits;
        }
        
        public Double getTotalValue() {
            return totalValue;
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
            
            // Obtener todos los turnos cerrados del día (sin usar PERSON que no existe en closedcash)
            String sql = "SELECT closedcash.MONEY, closedcash.HOST, closedcash.HOSTSEQUENCE, " +
                        "closedcash.DATESTART, closedcash.DATEEND, closedcash.INITIAL_AMOUNT " +
                        "FROM closedcash " +
                        "WHERE closedcash.DATEEND IS NOT NULL " +
                        "AND closedcash.DATESTART >= ? AND closedcash.DATESTART < ? " +
                        "ORDER BY closedcash.HOSTSEQUENCE ASC";
            
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
                double initialAmount = rs.getDouble("INITIAL_AMOUNT");
                if (rs.wasNull()) {
                    initialAmount = 0.0;
                }
                
                // Obtener el usuario del primer ticket del turno
                String userName = "admin";
                try {
                    java.sql.PreparedStatement userStmt = session.getConnection().prepareStatement(
                        "SELECT people.NAME FROM tickets " +
                        "INNER JOIN receipts ON tickets.ID = receipts.ID " +
                        "INNER JOIN people ON tickets.PERSON = people.ID " +
                        "WHERE receipts.MONEY = ? " +
                        "ORDER BY receipts.DATENEW ASC " +
                        "LIMIT 1"
                    );
                    userStmt.setString(1, money);
                    java.sql.ResultSet userRs = userStmt.executeQuery();
                    if (userRs.next()) {
                        userName = userRs.getString("NAME");
                        if (userName == null || userName.trim().isEmpty()) {
                            userName = "admin";
                        }
                    }
                    userRs.close();
                    userStmt.close();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error obteniendo usuario del turno: " + e.getMessage(), e);
                }
                
                LOGGER.info("Procesando turno #" + sequence + " (MONEY: " + money + ", Usuario: " + userName + ", Fondo inicial: " + initialAmount + ")");
                
                ShiftData shift = new ShiftData(money, host, sequence, dateStart, dateEnd, userName);
                shift.setInitialAmount(initialAmount);
                
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
                
                // Obtener entradas y salidas del turno
                try {
                    // Entradas (cashin)
                    java.sql.PreparedStatement cashInStmt = session.getConnection().prepareStatement(
                        "SELECT COALESCE(SUM(payments.TOTAL), 0) AS TOTAL " +
                        "FROM payments " +
                        "INNER JOIN receipts ON payments.RECEIPT = receipts.ID " +
                        "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'cashin'"
                    );
                    cashInStmt.setString(1, money);
                    java.sql.ResultSet cashInRs = cashInStmt.executeQuery();
                    if (cashInRs.next()) {
                        double cashIn = cashInRs.getDouble("TOTAL");
                        if (!cashInRs.wasNull()) {
                            shift.setCashIn(cashIn);
                        }
                    }
                    cashInRs.close();
                    cashInStmt.close();
                    
                    // Salidas (cashout)
                    java.sql.PreparedStatement cashOutStmt = session.getConnection().prepareStatement(
                        "SELECT COALESCE(SUM(payments.TOTAL), 0) AS TOTAL " +
                        "FROM payments " +
                        "INNER JOIN receipts ON payments.RECEIPT = receipts.ID " +
                        "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'cashout'"
                    );
                    cashOutStmt.setString(1, money);
                    java.sql.ResultSet cashOutRs = cashOutStmt.executeQuery();
                    if (cashOutRs.next()) {
                        double cashOut = cashOutRs.getDouble("TOTAL");
                        if (!cashOutRs.wasNull()) {
                            shift.setCashOut(cashOut);
                        }
                    }
                    cashOutRs.close();
                    cashOutStmt.close();
                    
                    LOGGER.info("Turno #" + sequence + ": Entradas=" + shift.getCashIn() + ", Salidas=" + shift.getCashOut());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error obteniendo entradas/salidas del turno: " + e.getMessage(), e);
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
     * Crea el diseño moderno basado en la imagen de referencia usando HTML/CSS
     */
    private void createModernLayout() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        
        // Panel principal con HTML
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        
        // Editor HTML para mostrar el contenido
        JEditorPane htmlViewer = new JEditorPane();
        htmlViewer.setContentType("text/html");
        htmlViewer.setEditable(false);
        htmlViewer.setBackground(Color.WHITE);
        
        // Generar HTML inicial
        String htmlContent = generateHTMLContent();
        htmlViewer.setText(htmlContent);
        
        // Guardar referencia para actualizar después
        m_htmlViewer = htmlViewer;
        
        // Scroll pane
        JScrollPane scrollPane = new JScrollPane(htmlViewer);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Panel de botones en la parte superior con título y fecha a la izquierda
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        
        // Panel izquierdo con título, fecha y botón Cerrar turno
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(Color.WHITE);
        
        // Panel para título y fecha
        JPanel titleDatePanel = new JPanel();
        titleDatePanel.setLayout(new BoxLayout(titleDatePanel, BoxLayout.Y_AXIS));
        titleDatePanel.setBackground(Color.WHITE);
        
        // Obtener nombre de usuario y rango de fechas
        String userName = m_PaymentsToClose != null && m_PaymentsToClose.getUser() != null ? m_PaymentsToClose.getUser() : "admin";
        String dateStart = m_PaymentsToClose != null ? m_PaymentsToClose.printDateStart() : "";
        String dateEnd = m_PaymentsToClose != null ? m_PaymentsToClose.printDateEnd() : "";
        
        // Formatear rango de fechas
        String timeRange = "";
        if (dateStart != null && !dateStart.isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                java.text.SimpleDateFormat displayFormat = new java.text.SimpleDateFormat("d/M/yyyy, h:mm:ss a");
                java.util.Date startDate = sdf.parse(dateStart);
                if (dateEnd != null && !dateEnd.isEmpty()) {
                    java.util.Date endDate = sdf.parse(dateEnd);
                    timeRange = displayFormat.format(startDate) + " a " + displayFormat.format(endDate);
                } else {
                    timeRange = displayFormat.format(startDate);
                }
            } catch (Exception e) {
                try {
                    java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    java.text.SimpleDateFormat displayFormat = new java.text.SimpleDateFormat("d/M/yyyy, h:mm:ss a");
                    java.util.Date startDate = sdf2.parse(dateStart);
                    if (dateEnd != null && !dateEnd.isEmpty()) {
                        java.util.Date endDate = sdf2.parse(dateEnd);
                        timeRange = displayFormat.format(startDate) + " a " + displayFormat.format(endDate);
                    } else {
                        timeRange = displayFormat.format(startDate);
                    }
                } catch (Exception e2) {
                    timeRange = dateStart;
                }
            }
        }
        
        // Título
        JLabel titleLabel = new JLabel("Corte de " + userName);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(33, 37, 41));
        titleDatePanel.add(titleLabel);
        
        // Fecha
        JLabel dateLabel = new JLabel(timeRange);
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dateLabel.setForeground(new Color(108, 117, 125));
        titleDatePanel.add(dateLabel);
        
        leftPanel.add(titleDatePanel, BorderLayout.WEST);
        
        // Botón Cerrar turno al lado del título
        JButton btnCloseShift = new JButton("🔒 Cerrar turno");
        btnCloseShift.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnCloseShift.setPreferredSize(new Dimension(130, 28));
        btnCloseShift.setForeground(Color.WHITE);
        btnCloseShift.setBackground(new Color(239, 68, 68));
        btnCloseShift.setFocusPainted(false);
        btnCloseShift.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 38, 38), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btnCloseShift.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCloseShift.addActionListener(e -> {
            m_jCloseCashActionPerformed(new java.awt.event.ActionEvent(btnCloseShift, java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
        });
        leftPanel.add(btnCloseShift, BorderLayout.EAST);
        
        buttonPanel.add(leftPanel, BorderLayout.WEST);
        
        // Panel derecho con botones pequeños y pulidos
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setBackground(Color.WHITE);
        
        // Botón Imprimir
        JButton btnPrint = new JButton("Imprimir");
        btnPrint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnPrint.setPreferredSize(new Dimension(90, 28));
        btnPrint.setBackground(new Color(108, 117, 125));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(73, 80, 87), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrint.addActionListener(e -> {
            try {
                htmlViewer.print();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error imprimiendo", ex);
            }
        });
        rightPanel.add(btnPrint);
        
        // Botones de corte - más pequeños y pulidos
        JButton btnCashier = new JButton("🧾 Corte de cajero");
        btnCashier.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnCashier.setPreferredSize(new Dimension(140, 28));
        btnCashier.setForeground(Color.WHITE);
        btnCashier.setBackground(new Color(59, 130, 246));
        btnCashier.setFocusPainted(false);
        btnCashier.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(37, 99, 235), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btnCashier.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCashier.addActionListener(e -> {
            m_jCloseCashActionPerformed(new java.awt.event.ActionEvent(btnCashier, java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
        });
        rightPanel.add(btnCashier);
        
        JButton btnDay = new JButton("📅 Corte del día");
        btnDay.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnDay.setPreferredSize(new Dimension(140, 28));
        btnDay.setForeground(Color.WHITE);
        btnDay.setBackground(new Color(168, 85, 247));
        btnDay.setFocusPainted(false);
        btnDay.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(147, 51, 234), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btnDay.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDay.addActionListener(e -> {
            int respuesta = JOptionPane.showConfirmDialog(
                this,
                "El corte del día cerrará todas las cajas activas del día.\n\n¿Desea continuar?",
                "Corte del Día",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (respuesta == JOptionPane.YES_OPTION) {
                m_jCloseCashActionPerformed(new java.awt.event.ActionEvent(btnDay, java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
            }
        });
        rightPanel.add(btnDay);
        
        buttonPanel.add(rightPanel, BorderLayout.EAST);
        
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JEditorPane m_htmlViewer;
    
    /**
     * Genera el contenido HTML completo con el diseño exacto de la imagen
     */
    private String generateHTMLContent() {
        if (m_PaymentsToClose == null) {
            return "<html><body><p>Cargando datos...</p></body></html>";
        }
        
        try {
            // Obtener todos los datos necesarios
            String userName = m_PaymentsToClose.getUser() != null ? m_PaymentsToClose.getUser() : "Usuario";
            String dateStart = m_PaymentsToClose.printDateStart();
            String dateEnd = m_PaymentsToClose.printDateEnd();
            
            // Formatear rango de fechas exactamente como Eleventa
            String timeRange = "";
            if (dateStart != null && !dateStart.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                    SimpleDateFormat timeFormatFull = new SimpleDateFormat("h:mm:ss a");
                    Date startDate = sdf.parse(dateStart);
                    String startTime = timeFormatFull.format(startDate).toLowerCase();
                    if (dateEnd != null && !dateEnd.isEmpty()) {
                        Date endDate = sdf.parse(dateEnd);
                        String endTime = timeFormatFull.format(endDate).toLowerCase();
                        timeRange = String.format("%s a las %s", startTime, endTime);
                    } else {
                        timeRange = startTime;
                    }
                } catch (ParseException e) {
                    // Intentar otro formato
                    try {
                        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        SimpleDateFormat timeFormatFull = new SimpleDateFormat("h:mm:ss a");
                        Date startDate = sdf2.parse(dateStart);
                        String startTime = timeFormatFull.format(startDate).toLowerCase();
                        if (dateEnd != null && !dateEnd.isEmpty()) {
                            Date endDate = sdf2.parse(dateEnd);
                            String endTime = timeFormatFull.format(endDate).toLowerCase();
                            timeRange = String.format("%s a las %s", startTime, endTime);
                        } else {
                            timeRange = startTime;
                        }
                    } catch (ParseException e2) {
                        timeRange = dateStart;
                    }
                }
            }
            
            // Obtener ventas totales
            double totalSales = 0.0;
            try {
                String salesTotalStr = m_PaymentsToClose.printSalesTotal();
                if (salesTotalStr != null && !salesTotalStr.isEmpty()) {
                    totalSales = Formats.CURRENCY.parseValue(salesTotalStr);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error parseando ventas totales", e);
            }
            
            // Calcular ganancia
            double profit = 0.0;
            try {
                String activeCashIndex = m_App.getActiveCashIndex();
                Session session = m_App.getSession();
                Connection conn = session.getConnection();
                
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
                    }
                }
                rs.close();
                pstmt.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error calculando ganancia", e);
            }
            
            // Obtener fondo inicial
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
                rs.close();
                pstmt.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo fondo inicial", e);
                if (m_PaymentsToClose.getInitialAmount() != null) {
                    initialAmount = m_PaymentsToClose.getInitialAmount();
                }
            }
            
            // Obtener ventas en efectivo
            double cashSales = m_PaymentsToClose.getCashTotal() != null ? m_PaymentsToClose.getCashTotal() : 0.0;
            
            // Obtener abonos en efectivo (pagos de créditos en efectivo)
            double creditPaymentsCash = 0.0;
            try {
                String activeCashIndex = m_App.getActiveCashIndex();
                Session session = m_App.getSession();
                Connection conn = session.getConnection();
                Date abonoDateStart = m_PaymentsToClose.getDateStart();
                
                String sql = "SELECT COALESCE(SUM(payments.TOTAL), 0) " +
                             "FROM receipts " +
                             "INNER JOIN payments ON receipts.ID = payments.RECEIPT " +
                             "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'debt' " +
                             "AND receipts.DATENEW >= ?";
                
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, activeCashIndex);
                pstmt.setTimestamp(2, new java.sql.Timestamp(abonoDateStart.getTime()));
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    creditPaymentsCash = rs.getDouble(1);
                }
                rs.close();
                pstmt.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo abonos", e);
            }
            
            // Obtener entradas y salidas
            double cashIn = 0.0;
            double cashOut = 0.0;
            java.util.List<String> inflowsList = new java.util.ArrayList<>();
            java.util.List<String> outflowsList = new java.util.ArrayList<>();
            
            try {
                String activeCashIndex = m_App.getActiveCashIndex();
                Session session = m_App.getSession();
                Connection conn = session.getConnection();
                Date shiftDateStart = m_PaymentsToClose.getDateStart();
                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
                
                // Entradas
                String sqlIn = "SELECT receipts.DATENEW, payments.TOTAL, payments.NOTES " +
                              "FROM receipts " +
                              "INNER JOIN payments ON receipts.ID = payments.RECEIPT " +
                              "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'cashin' " +
                              "AND receipts.DATENEW >= ? " +
                              "ORDER BY receipts.DATENEW DESC";
                
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sqlIn);
                pstmt.setString(1, activeCashIndex);
                pstmt.setTimestamp(2, new java.sql.Timestamp(shiftDateStart.getTime()));
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    Date dateNew = rs.getTimestamp("DATENEW");
                    double total = rs.getDouble("TOTAL");
                    String notes = rs.getString("NOTES");
                    cashIn += total;
                    String timeStr = timeFormat.format(dateNew).toLowerCase();
                    String item;
                    if (notes != null && !notes.isEmpty() && !notes.trim().isEmpty()) {
                        // Si hay notas, usar el formato con las notas
                        item = String.format("%s %s: %s", timeStr, escapeHtml(notes), Formats.CURRENCY.formatValue(total));
                    } else {
                        // Si no hay notas, usar "Entrada De Dinero"
                        item = String.format("%s Entrada De Dinero: %s", timeStr, Formats.CURRENCY.formatValue(total));
                    }
                    inflowsList.add(item);
                }
                rs.close();
                pstmt.close();
                
                // Salidas
                String sqlOut = "SELECT receipts.DATENEW, payments.TOTAL, payments.NOTES " +
                               "FROM receipts " +
                               "INNER JOIN payments ON receipts.ID = payments.RECEIPT " +
                               "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'cashout' " +
                               "AND receipts.DATENEW >= ? " +
                               "ORDER BY receipts.DATENEW DESC";
                
                pstmt = conn.prepareStatement(sqlOut);
                pstmt.setString(1, activeCashIndex);
                pstmt.setTimestamp(2, new java.sql.Timestamp(shiftDateStart.getTime()));
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    Date dateNew = rs.getTimestamp("DATENEW");
                    double total = Math.abs(rs.getDouble("TOTAL"));
                    String notes = rs.getString("NOTES");
                    cashOut += total;
                    String timeStr = timeFormat.format(dateNew).toLowerCase();
                    String noteText = notes != null && !notes.isEmpty() ? escapeHtml(notes) : "Efectivo";
                    String item = String.format("%s %s: %s", 
                        timeStr, noteText, Formats.CURRENCY.formatValue(total));
                    outflowsList.add(item);
                }
                rs.close();
                pstmt.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo entradas/salidas", e);
            }
            
            // Calcular total de dinero en caja
            double cashTotal = initialAmount + cashSales + creditPaymentsCash + cashIn - cashOut;
            
            // Obtener ventas por tipo de pago
            double cardSales = 0.0;
            double creditSales = 0.0;
            double voucherSales = 0.0;
            double returns = 0.0;
            
            if (m_PaymentsToClose.getPaymentLines() != null) {
                for (PaymentsModel.PaymentsLine pl : m_PaymentsToClose.getPaymentLines()) {
                    String paymentType = pl.getType();
                    double value = pl.getValue() != null ? pl.getValue() : 0.0;
                    if ("cash".equals(paymentType)) {
                        // Ya lo tenemos en cashSales
                    } else if ("card".equals(paymentType) || "magcard".equals(paymentType)) {
                        cardSales += value;
                    } else if ("debt".equals(paymentType)) {
                        creditSales += value;
                    } else if ("voucher".equals(paymentType)) {
                        voucherSales += value;
                    }
                }
            }
            
            // Obtener ventas por departamento
            java.util.List<String> deptSalesList = new java.util.ArrayList<>();
            try {
                java.util.List categorySales = m_PaymentsToClose.getCategorySalesLines();
                if (categorySales != null) {
                    for (Object obj : categorySales) {
                        PaymentsModel.CategorySalesLine category = (PaymentsModel.CategorySalesLine) obj;
                        String deptName = category.printCategoryName();
                        String totalStr = category.printCategorySum();
                        deptSalesList.add(String.format("%s: %s", deptName, totalStr));
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo ventas por departamento", e);
            }
            
            // Obtener pagos de créditos
            java.util.List<String> creditPaymentsList = new java.util.ArrayList<>();
            try {
                String activeCashIndex = m_App.getActiveCashIndex();
                Session session = m_App.getSession();
                Connection conn = session.getConnection();
                Date creditDateStart = m_PaymentsToClose.getDateStart();
                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
                
                String sql = "SELECT receipts.DATENEW, payments.TOTAL, customers.NAME as CUSTOMER_NAME " +
                             "FROM receipts " +
                             "INNER JOIN payments ON receipts.ID = payments.RECEIPT " +
                             "LEFT JOIN tickets ON receipts.ID = tickets.ID " +
                             "LEFT JOIN customers ON tickets.CUSTOMER = customers.ID " +
                             "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'debt' " +
                             "AND receipts.DATENEW >= ? " +
                             "ORDER BY receipts.DATENEW DESC";
                
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, activeCashIndex);
                pstmt.setTimestamp(2, new java.sql.Timestamp(creditDateStart.getTime()));
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    Date dateNew = rs.getTimestamp("DATENEW");
                    double total = rs.getDouble("TOTAL");
                    String customer = rs.getString("CUSTOMER_NAME");
                    String timeStr = timeFormat.format(dateNew).toLowerCase();
                    String item = String.format("%s De %s: %s", 
                        timeStr,
                        customer != null && !customer.isEmpty() ? escapeHtml(customer) : "Cliente",
                        Formats.CURRENCY.formatValue(total));
                    creditPaymentsList.add(item);
                }
                rs.close();
                pstmt.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo pagos de créditos", e);
            }
            
            // Generar HTML con diseño exacto de Eleventa
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head>");
            html.append("<meta charset='UTF-8'>");
            html.append("<style>");
            html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
            html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background: #f8f9fa; padding: 0; margin: 0; }");
            html.append(".container { max-width: 100%; margin: 0; background: white; padding: 0; }");
            html.append(".header { background: white; padding: 20px 24px; border-bottom: 1px solid #e9ecef; }");
            html.append(".header h1 { font-size: 20px; font-weight: 600; color: #212529; margin: 0 0 4px 0; line-height: 1.2; }");
            html.append(".header .subtitle { font-size: 13px; color: #6c757d; margin: 0; }");
            html.append(".metrics { width: 100%; background: white; border-bottom: 1px solid #e9ecef; border-collapse: collapse; }");
            html.append(".metrics td { vertical-align: top; }");
            html.append(".metrics td:first-child { padding-left: 24px; }");
            html.append(".metric-card { background: #f8f9fa; padding: 16px; border-radius: 4px; border: 1px solid #e9ecef; margin-right: 16px; }");
            html.append(".metric-card-right { background: white; border: 1px solid #dee2e6; }");
            html.append(".metric-label { font-size: 12px; color: #6c757d; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.5px; font-weight: 500; }");
            html.append(".metric-value { font-size: 24px; font-weight: 700; color: #212529; line-height: 1.2; }");
            html.append(".sections-container { width: 100%; background: white; border-bottom: 1px solid #e9ecef; border-collapse: collapse; }");
            html.append(".sections-container td { vertical-align: top; padding: 20px 24px; background: white; }");
            html.append(".sections-container td:first-child { padding-left: 24px; }");
            html.append(".section-left { border-right: 1px solid #e9ecef; }");
            html.append(".section-title { font-size: 14px; font-weight: 600; color: #212529; margin-bottom: 16px; text-transform: uppercase; letter-spacing: 0.5px; }");
            html.append(".section-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #f1f3f5; }");
            html.append(".section-row:last-of-type { border-bottom: 2px solid #dee2e6; margin-bottom: 0; }");
            html.append(".section-label { font-size: 14px; color: #495057; flex: 1; }");
            html.append(".section-value { font-size: 14px; font-weight: 500; color: #212529; text-align: right; }");
            html.append(".section-total { display: flex; justify-content: space-between; align-items: center; padding: 14px 0 0 0; margin-top: 8px; font-weight: 600; font-size: 15px; color: #212529; }");
            html.append(".positive { color: #28a745 !important; }");
            html.append(".negative { color: #dc3545 !important; }");
            html.append(".list-section { background: white; padding: 20px 24px; border-bottom: 1px solid #e9ecef; }");
            html.append(".list-title { font-size: 14px; font-weight: 600; color: #212529; margin-bottom: 12px; text-transform: uppercase; letter-spacing: 0.5px; }");
            html.append(".list-item { padding: 8px 0; font-size: 13px; color: #495057; line-height: 1.5; border-bottom: 1px solid #f1f3f5; }");
            html.append(".list-item:last-child { border-bottom: none; }");
            html.append(".empty-message { padding: 8px 0; font-size: 13px; color: #adb5bd; font-style: italic; }");
            html.append("</style>");
            html.append("</head><body>");
            html.append("<div class='container'>");
            
            // Métricas principales - usando tabla para lado a lado (header movido a la barra superior)
            html.append("<table class='metrics' cellpadding='0' cellspacing='0' border='0' width='100%'>");
            html.append("<tr>");
            
            // Ventas Totales (lado izquierdo)
            html.append("<td style='width: 50%; padding: 20px 24px 20px 24px;'>");
            html.append("<div class='metric-card'>");
            html.append("<div class='metric-label'>💰 Ventas Totales</div>");
            html.append("<div class='metric-value'>").append(Formats.CURRENCY.formatValue(totalSales)).append("</div>");
            html.append("</div>");
            html.append("</td>");
            
            // Ganancia (lado derecho)
            html.append("<td style='width: 50%; padding: 20px 24px 20px 16px;'>");
            html.append("<div class='metric-card metric-card-right'>");
            html.append("<div class='metric-label'>📊 Ganancia</div>");
            html.append("<div class='metric-value'>").append(Formats.CURRENCY.formatValue(profit)).append("</div>");
            html.append("</div>");
            html.append("</td>");
            
            html.append("</tr>");
            html.append("</table>");
            
            // Contenedor de dos columnas para Dinero en Caja (izquierda) y Ventas (derecha) usando tabla
            html.append("<table class='sections-container' cellpadding='0' cellspacing='0' border='0' width='100%'>");
            html.append("<tr>");
            
            // Dinero en Caja (lado izquierdo)
            html.append("<td class='section section-left' style='width: 50%; padding-left: 24px;'>");
            html.append("<div class='section-title'>📊 Dinero en Caja</div>");
            html.append("<div class='section-row'><span class='section-label'>Fondo de caja</span><span class='section-value'>").append(Formats.CURRENCY.formatValue(initialAmount)).append("</span></div>");
            html.append("<div class='section-row'><span class='section-label'>Ventas en Efectivo</span><span class='section-value positive'>+").append(Formats.CURRENCY.formatValue(cashSales)).append("</span></div>");
            html.append("<div class='section-row'><span class='section-label'>Abonos en efectivo</span><span class='section-value positive'>+").append(Formats.CURRENCY.formatValue(creditPaymentsCash)).append("</span></div>");
            html.append("<div class='section-row'><span class='section-label'>Entradas</span><span class='section-value positive'>+").append(Formats.CURRENCY.formatValue(cashIn)).append("</span></div>");
            html.append("<div class='section-row'><span class='section-label'>Salidas</span><span class='section-value negative'>-").append(Formats.CURRENCY.formatValue(cashOut)).append("</span></div>");
            html.append("<div class='section-row'><span class='section-label'>Devoluciones en efectivo</span><span class='section-value negative'>-").append(Formats.CURRENCY.formatValue(returns)).append("</span></div>");
            html.append("<div class='section-total'><span>Total</span><span>").append(Formats.CURRENCY.formatValue(cashTotal)).append("</span></div>");
            html.append("</td>");
            
            // Ventas (lado derecho)
            html.append("<td class='section' style='width: 50%; padding-left: 24px;'>");
            html.append("<div class='section-title'>🛒 Ventas</div>");
            html.append("<div class='section-row'><span class='section-label'>En Efectivo</span><span class='section-value positive'>+").append(Formats.CURRENCY.formatValue(cashSales)).append("</span></div>");
            html.append("<div class='section-row'><span class='section-label'>Con Tarjeta de Crédito</span><span class='section-value positive'>+").append(Formats.CURRENCY.formatValue(cardSales)).append("</span></div>");
            html.append("<div class='section-row'><span class='section-label'>A Crédito</span><span class='section-value positive'>+").append(Formats.CURRENCY.formatValue(creditSales)).append("</span></div>");
            html.append("<div class='section-row'><span class='section-label'>Con Vales de Despensa</span><span class='section-value positive'>+").append(Formats.CURRENCY.formatValue(voucherSales)).append("</span></div>");
            html.append("<div class='section-row'><span class='section-label'>Devoluciones de Ventas</span><span class='section-value negative'>-").append(Formats.CURRENCY.formatValue(returns)).append("</span></div>");
            html.append("<div class='section-total'><span>Total</span><span>").append(Formats.CURRENCY.formatValue(totalSales)).append("</span></div>");
            html.append("</td>");
            
            html.append("</tr>");
            html.append("</table>"); // Cierre de la tabla sections-container
            
            // Espaciador para separar las secciones (más espacio para no quedar pegados)
            html.append("<div style='height: 30px; background: white;'></div>");
            
            // Contenedor de dos columnas para Entradas de efectivo (izquierda) y Salidas de Efectivo (derecha)
            html.append("<table class='sections-container' cellpadding='0' cellspacing='0' border='0' width='100%' style='padding-left: 24px;'>");
            html.append("<tr>");
            
            // Entradas de efectivo (lado izquierdo, debajo de Dinero en Caja)
            html.append("<td class='section section-left' style='width: 50%; padding: 20px 24px;'>");
            html.append("<div class='list-section' style='padding: 0; margin: 0; border: none;'>");
            html.append("<div class='list-title'>⬇️ Entradas de efectivo</div>");
            if (inflowsList.isEmpty()) {
                html.append("<div class='empty-message'>- No hubo entradas -</div>");
            } else {
                for (String item : inflowsList) {
                    html.append("<div class='list-item'>").append(item).append("</div>");
                }
            }
            html.append("</div>");
            html.append("</td>");
            
            // Salidas de Efectivo (lado derecho, debajo de Ventas)
            html.append("<td class='section' style='width: 50%; padding: 20px 24px;'>");
            html.append("<div class='list-section' style='padding: 0; margin: 0; border: none;'>");
            html.append("<div class='list-title'>⬆️ Salidas de Efectivo</div>");
            if (outflowsList.isEmpty()) {
                html.append("<div class='empty-message'>- No hubo salidas -</div>");
            } else {
                for (String item : outflowsList) {
                    html.append("<div class='list-item'>").append(item).append("</div>");
                }
            }
            html.append("</div>");
            html.append("</td>");
            
            html.append("</tr>");
            html.append("</table>");
            
            // Espaciador para separar las secciones (más espacio para no quedar pegados)
            html.append("<div style='height: 30px; background: white;'></div>");
            
            // Contenedor de dos columnas para Ventas por Departamento (izquierda) y Pagos de Créditos (derecha)
            html.append("<table class='sections-container' cellpadding='0' cellspacing='0' border='0' width='100%' style='padding-left: 24px;'>");
            html.append("<tr>");
            
            // Ventas por Departamento (lado izquierdo, debajo de Entradas de efectivo)
            html.append("<td class='section section-left' style='width: 50%; padding: 20px 24px;'>");
            html.append("<div class='list-section' style='padding: 0; margin: 0; border: none;'>");
            html.append("<div class='list-title'>📦 Ventas por Departamento</div>");
            if (deptSalesList.isEmpty()) {
                html.append("<div class='empty-message'>- Sin ventas por departamento -</div>");
            } else {
                for (String item : deptSalesList) {
                    html.append("<div class='list-item'>").append(item).append("</div>");
                }
            }
            html.append("</div>");
            html.append("</td>");
            
            // Pagos de Créditos (lado derecho, debajo de Salidas de Efectivo)
            html.append("<td class='section' style='width: 50%; padding: 20px 24px;'>");
            html.append("<div class='list-section' style='padding: 0; margin: 0; border: none;'>");
            html.append("<div class='list-title'>👥 Pagos de Créditos</div>");
            if (creditPaymentsList.isEmpty()) {
                html.append("<div class='empty-message'>- No se recibieron pagos de créditos -</div>");
            } else {
                for (String item : creditPaymentsList) {
                    html.append("<div class='list-item'>").append(item).append("</div>");
                }
            }
            html.append("</div>");
            html.append("</td>");
            
            html.append("</tr>");
            html.append("</table>");
            
            // Obtener ganancias por departamento
            java.util.List<String> deptProfitList = new java.util.ArrayList<>();
            try {
                String activeCashIndex = m_App.getActiveCashIndex();
                Session session = m_App.getSession();
                Connection conn = session.getConnection();
                Date shiftDateStart = m_PaymentsToClose.getDateStart();
                
                String sql = "SELECT COALESCE(categories.NAME, 'Sin Departamento') as CATEGORY_NAME, " +
                             "SUM((ticketlines.PRICE - COALESCE(products.PRICEBUY, 0)) * ticketlines.UNITS) as TOTAL_PROFIT " +
                             "FROM ticketlines " +
                             "INNER JOIN tickets ON ticketlines.TICKET = tickets.ID " +
                             "INNER JOIN receipts ON tickets.ID = receipts.ID " +
                             "INNER JOIN products ON ticketlines.PRODUCT = products.ID " +
                             "LEFT JOIN categories ON products.CATEGORY = categories.ID " +
                             "WHERE receipts.MONEY = ? AND receipts.DATENEW >= ? " +
                             "GROUP BY COALESCE(categories.NAME, 'Sin Departamento') " +
                             "ORDER BY TOTAL_PROFIT DESC";
                
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, activeCashIndex);
                pstmt.setTimestamp(2, new java.sql.Timestamp(shiftDateStart.getTime()));
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String deptName = rs.getString("CATEGORY_NAME");
                    double deptProfit = rs.getDouble("TOTAL_PROFIT");
                    if (deptProfit > 0) {
                        deptProfitList.add(String.format("%s: %s", deptName, Formats.CURRENCY.formatValue(deptProfit)));
                    }
                }
                rs.close();
                pstmt.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo ganancias por departamento", e);
            }
            
            // Obtener clientes con más ventas
            java.util.List<String> topCustomersList = new java.util.ArrayList<>();
            try {
                String activeCashIndex = m_App.getActiveCashIndex();
                Session session = m_App.getSession();
                Connection conn = session.getConnection();
                Date shiftDateStart = m_PaymentsToClose.getDateStart();
                
                String sql = "SELECT customers.NAME, SUM(receipts.TOTAL) as TOTAL_SALES, COUNT(receipts.ID) as COUNT_TICKETS " +
                             "FROM receipts " +
                             "INNER JOIN tickets ON receipts.ID = tickets.ID " +
                             "LEFT JOIN customers ON tickets.CUSTOMER = customers.ID " +
                             "WHERE receipts.MONEY = ? AND receipts.DATENEW >= ? " +
                             "AND customers.ID IS NOT NULL " +
                             "GROUP BY customers.ID, customers.NAME " +
                             "ORDER BY TOTAL_SALES DESC " +
                             "LIMIT 10";
                
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, activeCashIndex);
                pstmt.setTimestamp(2, new java.sql.Timestamp(shiftDateStart.getTime()));
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String customerName = rs.getString("NAME");
                    double customerSales = rs.getDouble("TOTAL_SALES");
                    int countTickets = rs.getInt("COUNT_TICKETS");
                    topCustomersList.add(String.format("%s: %s (%d tickets)", 
                        customerName, Formats.CURRENCY.formatValue(customerSales), countTickets));
                }
                rs.close();
                pstmt.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo clientes con más ventas", e);
            }
            
            // Espaciador para separar las secciones
            html.append("<div style='height: 30px; background: white;'></div>");
            
            // Contenedor de dos columnas para Ganancias por Departamento (izquierda) y Clientes con más ventas (derecha)
            html.append("<table class='sections-container' cellpadding='0' cellspacing='0' border='0' width='100%' style='padding-left: 24px;'>");
            html.append("<tr>");
            
            // Ganancias por Departamento (lado izquierdo, debajo de Ventas por Departamento)
            html.append("<td class='section section-left' style='width: 50%; padding: 20px 24px;'>");
            html.append("<div class='list-section' style='padding: 0; margin: 0; border: none;'>");
            html.append("<div class='list-title'>💰 Ganancias por Departamento</div>");
            if (deptProfitList.isEmpty()) {
                html.append("<div class='empty-message'>- Sin ganancias por departamento -</div>");
            } else {
                for (String item : deptProfitList) {
                    html.append("<div class='list-item'>").append(item).append("</div>");
                }
            }
            html.append("</div>");
            html.append("</td>");
            
            // Clientes con más ventas (lado derecho, debajo de Pagos de Créditos)
            html.append("<td class='section' style='width: 50%; padding: 20px 24px;'>");
            html.append("<div class='list-section' style='padding: 0; margin: 0; border: none;'>");
            html.append("<div class='list-title'>👥 Clientes con más ventas</div>");
            if (topCustomersList.isEmpty()) {
                html.append("<div class='empty-message'>- No hay clientes con ventas -</div>");
            } else {
                for (String item : topCustomersList) {
                    html.append("<div class='list-item'>").append(item).append("</div>");
                }
            }
            html.append("</div>");
            html.append("</td>");
            
            html.append("</tr>");
            html.append("</table>");
            
            // Obtener devoluciones en efectivo
            java.util.List<String> cashReturnsList = new java.util.ArrayList<>();
            try {
                String activeCashIndex = m_App.getActiveCashIndex();
                Session session = m_App.getSession();
                Connection conn = session.getConnection();
                Date shiftDateStart = m_PaymentsToClose.getDateStart();
                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
                
                // Buscar devoluciones: pagos negativos en efectivo o tickets de tipo REFUND
                String sql = "SELECT receipts.DATENEW, ABS(payments.TOTAL) as TOTAL, tickets.TICKETTYPE, tickets.TICKETID " +
                             "FROM receipts " +
                             "INNER JOIN payments ON receipts.ID = payments.RECEIPT " +
                             "INNER JOIN tickets ON receipts.ID = tickets.ID " +
                             "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'cash' " +
                             "AND receipts.DATENEW >= ? " +
                             "AND (payments.TOTAL < 0 OR receipts.TOTAL < 0) " +
                             "ORDER BY receipts.DATENEW DESC";
                
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, activeCashIndex);
                pstmt.setTimestamp(2, new java.sql.Timestamp(shiftDateStart.getTime()));
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    Date dateNew = rs.getTimestamp("DATENEW");
                    double total = rs.getDouble("TOTAL");
                    String timeStr = timeFormat.format(dateNew).toLowerCase();
                    int ticketId = rs.getInt("TICKETID");
                    cashReturnsList.add(String.format("%s Devolución #%d: %s", 
                        timeStr, ticketId, Formats.CURRENCY.formatValue(total)));
                }
                rs.close();
                pstmt.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo devoluciones en efectivo", e);
            }
            
            // Espaciador para separar las secciones
            html.append("<div style='height: 30px; background: white;'></div>");
            
            // Sección de Devoluciones en efectivo (ocupando todo el ancho)
            html.append("<table class='sections-container' cellpadding='0' cellspacing='0' border='0' width='100%' style='padding-left: 24px;'>");
            html.append("<tr>");
            html.append("<td class='section' style='width: 100%; padding: 20px 24px;'>");
            html.append("<div class='list-section' style='padding: 0; margin: 0; border: none;'>");
            html.append("<div class='list-title'>↩️ Devoluciones en efectivo</div>");
            if (cashReturnsList.isEmpty()) {
                html.append("<div class='empty-message'>- No hubo devoluciones en efectivo -</div>");
            } else {
                for (String item : cashReturnsList) {
                    html.append("<div class='list-item'>").append(item).append("</div>");
                }
            }
            html.append("</div>");
            html.append("</td>");
            html.append("</tr>");
            html.append("</table>");
            
            html.append("</div>"); // container
            html.append("</body></html>");
            
            return html.toString();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generando HTML", e);
            return "<html><body><p>Error cargando datos: " + escapeHtml(e.getMessage()) + "</p></body></html>";
        }
    }
    
    /**
     * Escapa HTML para prevenir XSS
     */
    private String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    /**
     * Crea el panel de encabezado con título y botones
     */
    private JPanel createHeaderPanel() {
        // Panel con fondo verde/gris azulado (estilo Eleventa)
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(95, 135, 145)); // Verde/gris azulado como Eleventa
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        // Título "CORTE" en la parte superior izquierda
        JLabel titleLabel = new JLabel("CORTE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        panel.add(titleLabel, BorderLayout.WEST);
        
        // Botones de acción en la parte superior (estilo Eleventa)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setBackground(new Color(95, 135, 145));
        
        // Botón "Hacer corte de cajero" (estilo premium con gradiente azul)
        JButton btnCashier = new JButton("🧾 Hacer corte de cajero");
        btnCashier.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCashier.setPreferredSize(new Dimension(200, 45));
        btnCashier.setForeground(Color.WHITE);
        btnCashier.setFocusPainted(false);
        btnCashier.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCashier.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(37, 99, 235), 2),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        btnCashier.setContentAreaFilled(false);
        btnCashier.setOpaque(true);
        
        // Gradiente azul para corte de cajero
        btnCashier.setBackground(new Color(59, 130, 246)); // Azul vibrante
        
        // Efecto hover con gradiente más oscuro
        btnCashier.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnCashier.setBackground(new Color(37, 99, 235)); // Azul más oscuro
                btnCashier.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(29, 78, 216), 2),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnCashier.setBackground(new Color(59, 130, 246));
                btnCashier.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(37, 99, 235), 2),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                btnCashier.setBackground(new Color(29, 78, 216)); // Azul más oscuro al presionar
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                btnCashier.setBackground(new Color(37, 99, 235));
            }
        });
        btnCashier.addActionListener(e -> {
            LOGGER.info("Botón 'Hacer corte de cajero' presionado");
            m_jCloseCashActionPerformed(new java.awt.event.ActionEvent(btnCashier, java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
        });
        buttonPanel.add(btnCashier);
        
        // Botón "Hacer corte del día" (estilo premium con gradiente púrpura)
        JButton btnDay = new JButton("📅 Hacer corte del día");
        btnDay.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDay.setPreferredSize(new Dimension(200, 45));
        btnDay.setForeground(Color.WHITE);
        btnDay.setFocusPainted(false);
        btnDay.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDay.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(147, 51, 234), 2),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        btnDay.setContentAreaFilled(false);
        btnDay.setOpaque(true);
        
        // Gradiente púrpura para corte del día
        btnDay.setBackground(new Color(168, 85, 247)); // Púrpura vibrante
        
        // Efecto hover con gradiente más oscuro
        btnDay.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnDay.setBackground(new Color(147, 51, 234)); // Púrpura más oscuro
                btnDay.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(126, 34, 206), 2),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnDay.setBackground(new Color(168, 85, 247));
                btnDay.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(147, 51, 234), 2),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                btnDay.setBackground(new Color(126, 34, 206)); // Púrpura más oscuro al presionar
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                btnDay.setBackground(new Color(147, 51, 234));
            }
        });
        btnDay.addActionListener(e -> {
            LOGGER.info("Botón 'Hacer corte del día' presionado");
            int respuesta = JOptionPane.showConfirmDialog(
                this,
                "El corte del día cerrará todas las cajas activas del día.\n\n¿Desea continuar?",
                "Corte del Día",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (respuesta == JOptionPane.YES_OPTION) {
                m_jCloseCashActionPerformed(new java.awt.event.ActionEvent(btnDay, java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
            }
        });
        buttonPanel.add(btnDay);
        
        panel.add(buttonPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Crea el panel con información del turno
     */
    private JPanel createShiftInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(235, 235, 235));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        
        m_jShiftInfoLabel = new JLabel();
        m_jShiftInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        m_jShiftInfoLabel.setForeground(new Color(90, 90, 90));
        panel.add(m_jShiftInfoLabel, BorderLayout.WEST);
        
        return panel;
    }
    
    /**
     * Crea las tarjetas de métricas (Ventas Totales y Ganancia)
     */
    private JPanel createMetricsCards() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));
        panel.setBackground(new Color(235, 235, 235));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        
        // Tarjeta Ventas Totales - estilo Eleventa simple
        JPanel salesCard = createMetricCard("💰 Ventas Totales", "$0.00", new Color(59, 130, 246));
        panel.add(salesCard);
        
        // Tarjeta Ganancia - estilo Eleventa simple (fondo blanco destacado)
        JPanel profitCard = createMetricCard("📊 Ganancia", "$0.00", new Color(76, 175, 80));
        profitCard.setBackground(Color.WHITE); // Ganancia tiene fondo blanco en Eleventa
        panel.add(profitCard);
        
        return panel;
    }
    
    /**
     * Crea una tarjeta de métrica estilo Eleventa (simple y corporativo)
     */
    private JPanel createMetricCard(String title, String value, Color color) {
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
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        
        // Panel principal con icono y título
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        
        // Título simple (estilo Eleventa)
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLabel.setForeground(new Color(90, 90, 90));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Valor grande (estilo Eleventa)
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        contentPanel.add(valueLabel);
        
        if (title.contains("Ventas Totales")) {
            m_jSalesTotalLabel = valueLabel;
        } else if (title.contains("Ganancia")) {
            m_jProfitLabel = valueLabel;
        }
        
        card.add(contentPanel, BorderLayout.WEST);
        
        return card;
    }
    
    /**
     * Crea la sección de Dinero en Caja estilo Eleventa
     */
    private JPanel createCashSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 245, 245)); // Fondo gris claro
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 20, 0, 20),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // Título de sección (estilo Eleventa)
        JLabel sectionTitle = new JLabel("📊 Dinero en Caja");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sectionTitle.setForeground(new Color(60, 60, 60));
        panel.add(sectionTitle);
        panel.add(Box.createVerticalStrut(8));
        
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
        
        // Separador simple
        panel.add(Box.createVerticalStrut(5));
        JSeparator separatorCash = new JSeparator();
        separatorCash.setForeground(new Color(200, 200, 200));
        panel.add(separatorCash);
        panel.add(Box.createVerticalStrut(5));
        
        // Separador simple
        panel.add(Box.createVerticalStrut(5));
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(200, 200, 200));
        panel.add(separator);
        panel.add(Box.createVerticalStrut(5));
        
        // Total - estilo Eleventa
        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setBackground(new Color(245, 245, 245));
        totalPanel.setBorder(BorderFactory.createEmptyBorder(6, 15, 6, 15));
        JLabel totalLabel = new JLabel("Total");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        totalLabel.setForeground(new Color(50, 50, 50));
        totalPanel.add(totalLabel, BorderLayout.WEST);
        
        m_jCashTotalLabel = new JLabel("$0.00");
        m_jCashTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        m_jCashTotalLabel.setForeground(new Color(50, 50, 50));
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
        linePanel.setBackground(new Color(245, 245, 245));
        linePanel.setBorder(BorderFactory.createEmptyBorder(4, 15, 4, 15));
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelComp.setForeground(new Color(90, 90, 90));
        linePanel.add(labelComp, BorderLayout.WEST);
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        if (isGreen) {
            valueComp.setForeground(new Color(0, 150, 0)); // Verde Eleventa
        } else if (positive) {
            valueComp.setForeground(new Color(0, 150, 0)); // Verde
        } else {
            valueComp.setForeground(new Color(200, 0, 0)); // Rojo Eleventa
        }
        linePanel.add(valueComp, BorderLayout.EAST);
        
        panel.add(linePanel);
        return valueComp;
    }
    
    /**
     * Crea la sección de Ventas por tipo con mejor diseño
     */
    private JPanel createSalesTypeSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 20, 0, 20),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // Título de sección (estilo Eleventa)
        JLabel sectionTitle = new JLabel("🛒 Ventas");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sectionTitle.setForeground(new Color(60, 60, 60));
        panel.add(sectionTitle);
        panel.add(Box.createVerticalStrut(8));
        
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
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 20, 0, 20),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // Título de sección (estilo Eleventa - opcional, puede omitirse)
        JLabel sectionTitle = new JLabel("💵 Ingresos de contado");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sectionTitle.setForeground(new Color(60, 60, 60));
        panel.add(sectionTitle);
        panel.add(Box.createVerticalStrut(8));
        
        addCashLine(panel, "Ventas en Efectivo", "+$0.00", true, true);
        
        // Separador y total
        panel.add(Box.createVerticalStrut(6));
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(229, 231, 235));
        panel.add(separator);
        panel.add(Box.createVerticalStrut(6));
        
        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setBackground(new Color(249, 250, 251));
        totalPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel totalLabel = new JLabel("Total");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        totalLabel.setForeground(new Color(30, 30, 30));
        totalPanel.add(totalLabel, BorderLayout.WEST);
        
        JLabel totalValue = new JLabel("$0.00");
        totalValue.setFont(new Font("Segoe UI", Font.BOLD, 18));
        totalValue.setForeground(new Color(76, 175, 80)); // Verde destacado
        totalPanel.add(totalValue, BorderLayout.EAST);
        panel.add(totalPanel);
        
        return panel;
    }
    
    /**
     * Crea el panel inferior con listas
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 8, 0));
        panel.setBackground(new Color(235, 235, 235));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 20, 0, 20));
        
        // Lista de Entradas de efectivo
        m_inflowsListModel = new DefaultListModel<>();
        JPanel inflowsPanel = createListPanel("⬇️ Entradas de efectivo", m_inflowsListModel);
        panel.add(inflowsPanel);
        
        // Lista de Salidas de Efectivo
        m_outflowsListModel = new DefaultListModel<>();
        JPanel outflowsPanel = createListPanel("⬆️ Salidas de Efectivo", m_outflowsListModel);
        panel.add(outflowsPanel);
        
        // Lista de Ventas por Departamento
        m_deptSalesListModel = new DefaultListModel<>();
        JPanel deptPanel = createListPanel("📦 Ventas por Departamento", m_deptSalesListModel);
        panel.add(deptPanel);
        
        return panel;
    }
    
    /**
     * Crea un panel con lista con mejor diseño y color
     */
    private JPanel createListPanel(String title, DefaultListModel<String> listModel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        
        // Título simple (estilo Eleventa)
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(new Color(60, 60, 60));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Lista simple
        JList<String> list = new JList<>(listModel);
        list.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        list.setBackground(Color.WHITE);
        list.setForeground(new Color(70, 70, 70));
        list.setSelectionBackground(new Color(220, 220, 220));
        list.setSelectionForeground(new Color(50, 50, 50));
        list.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(250, 150));
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
                if (isDayClose) {
                    // Usar el nuevo servicio que genera el documento directamente desde Java
                    LOGGER.info("Generando reporte del corte del día con servicio nuevo (sin templates)");
                    generateDayCloseReportDirect(dNow);
                } else {
                    // Cierre de turno normal: usar el método tradicional
                    printPayments("Printer.CloseCash", false);
                }

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
                    
                    // Mostrar reporte del turno actual
                    SwingUtilities.invokeLater(() -> {
                        showShiftReport();
                    });
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
            LOGGER.info("Generando reporte visual del corte del día directamente desde Java...");
            
            // Obtener todos los turnos del día
            java.util.List<ShiftData> allShifts = getAllShiftsForDay(closeDate);
            if (allShifts == null || allShifts.isEmpty()) {
                LOGGER.warning("No se encontraron turnos para el día");
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, 
                    "No se encontraron turnos para el día seleccionado.");
                msg.show(this);
                return;
            }
            
            // Calcular totales del día
            double totalDayInitialAmount = 0.0;
            double totalDaySales = 0.0;
            double totalDayPayments = 0.0;
            double totalDayCashIn = 0.0;
            double totalDayCashOut = 0.0;
            
            // Generar HTML del reporte
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head>");
            html.append("<meta charset='UTF-8'>");
            html.append("<title>Reporte de Caja Cerrada - Día Completo</title>");
            html.append("<style>");
            html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
            html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%); min-height: 100vh; }");
            html.append(".container { max-width: 1200px; margin: 0 auto; background: white; border-radius: 15px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); padding: 30px; }");
            html.append("h1 { text-align: center; color: #2c3e50; font-size: 2.5em; margin-bottom: 10px; text-shadow: 2px 2px 4px rgba(0,0,0,0.1); }");
            html.append(".header-info { text-align: center; color: #7f8c8d; margin-bottom: 30px; font-size: 1.1em; }");
            html.append("h2 { color: #2c3e50; border-left: 5px solid #3498db; padding-left: 15px; margin: 25px 0 15px 0; font-size: 1.8em; }");
            html.append("h3 { color: #34495e; margin: 20px 0 10px 0; font-size: 1.4em; }");
            html.append("table { width: 100%; border-collapse: separate; border-spacing: 0; margin: 15px 0; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.08); }");
            html.append("th { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 15px 12px; text-align: left; font-weight: 600; text-transform: uppercase; font-size: 0.9em; letter-spacing: 0.5px; }");
            html.append("td { padding: 12px; border-bottom: 1px solid #ecf0f1; background-color: white; }");
            html.append("tr:last-child td { border-bottom: none; }");
            html.append("tr:nth-child(even) td { background-color: #f8f9fa; }");
            html.append("tr:hover td { background-color: #e8f4f8; transition: background-color 0.3s ease; }");
            html.append(".total { font-weight: bold; font-size: 1.2em; color: #2c3e50; background-color: #ecf0f1 !important; }");
            html.append(".shift-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; margin: 25px 0 20px 0; border-radius: 10px; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3); }");
            html.append(".shift-header h2 { color: white; border: none; padding: 0; margin: 0 0 10px 0; }");
            html.append(".shift-header p { margin: 5px 0; font-size: 1em; }");
            html.append(".summary { background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%); padding: 25px; margin: 25px 0; border-radius: 12px; border: 1px solid #e1e8ed; box-shadow: 0 4px 15px rgba(0,0,0,0.08); transition: transform 0.3s ease, box-shadow 0.3s ease; }");
            html.append(".summary:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.12); }");
            html.append(".summary h2 { border-left-color: #3498db; }");
            html.append("p { margin: 10px 0; line-height: 1.6; color: #555; }");
            html.append("em { color: #95a5a6; font-style: italic; }");
            html.append("@media print { body { background: white; } .container { box-shadow: none; } }");
            html.append("</style>");
            html.append("</head><body>");
            html.append("<div class='container'>");
            
            html.append("<h1>📊 REPORTE DE CAJA CERRADA</h1>");
            html.append("<div class='header-info'>");
            html.append("<p><strong>📅 Fecha:</strong> ").append(Formats.DATE.formatValue(closeDate)).append("</p>");
            html.append("<p><strong>🕐 Generado:</strong> ").append(Formats.TIMESTAMP.formatValue(new Date())).append("</p>");
            html.append("</div>");
            
            // Procesar cada turno
            for (ShiftData shift : allShifts) {
                totalDayInitialAmount += shift.getInitialAmount();
                totalDaySales += shift.getTotalSales();
                totalDayPayments += shift.getTotalPayments();
                totalDayCashIn += shift.getCashIn();
                totalDayCashOut += shift.getCashOut();
                
                // Calcular total del turno
                double totalTurno = shift.getInitialAmount() + shift.getTotalSales() + shift.getCashIn() - shift.getCashOut();
                
                // Encabezado del turno
                html.append("<div class='shift-header'>");
                html.append("<h2>TURNO ").append(shift.getSequence()).append(" - ").append(shift.getUserName() != null ? shift.getUserName() : "admin").append("</h2>");
                html.append("<p><strong>Inicio:</strong> ").append(Formats.TIMESTAMP.formatValue(shift.getDateStart())).append("</p>");
                html.append("<p><strong>Fin:</strong> ").append(Formats.TIMESTAMP.formatValue(shift.getDateEnd())).append("</p>");
                html.append("</div>");
                
                // Resumen del turno
                html.append("<div class='summary'>");
                html.append("<h3>📊 Resumen del Turno</h3>");
                html.append("<table>");
                html.append("<tr style='background-color: #f8f9fa;'><td style='padding: 12px;'><strong>Monto Inicial del Turno:</strong></td><td style='padding: 12px; font-size: 1.1em; color: #27ae60;'><strong>").append(Formats.CURRENCY.formatValue(shift.getInitialAmount())).append("</strong></td></tr>");
                html.append("<tr><td style='padding: 12px;'><strong>Total de Ventas:</strong></td><td style='padding: 12px; font-size: 1.1em; color: #2c3e50;'>").append(Formats.CURRENCY.formatValue(shift.getTotalSales())).append("</td></tr>");
                html.append("<tr style='background-color: #f8f9fa;'><td style='padding: 12px;'><strong>Entradas:</strong></td><td style='padding: 12px; font-size: 1.1em; color: #27ae60;'>").append(Formats.CURRENCY.formatValue(shift.getCashIn())).append("</td></tr>");
                html.append("<tr><td style='padding: 12px;'><strong>Salidas:</strong></td><td style='padding: 12px; font-size: 1.1em; color: #e74c3c;'>").append(Formats.CURRENCY.formatValue(shift.getCashOut())).append("</td></tr>");
                html.append("<tr style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-top: 2px solid #667eea;'><td style='padding: 15px; color: white; font-size: 1.2em;'><strong>TOTAL DEL TURNO:</strong></td><td style='padding: 15px; color: white; font-size: 1.3em; font-weight: bold;'>").append(Formats.CURRENCY.formatValue(totalTurno)).append("</td></tr>");
                html.append("</table>");
                html.append("</div>");
            }
            
            // VENTAS CONSOLIDADAS POR DEPARTAMENTO
            html.append("<div class='summary'>");
            html.append("<h2>🏢 VENTAS POR DEPARTAMENTO</h2>");
            
            // Consolidar ventas por departamento de todos los turnos
            java.util.Map<String, ConsolidatedProduct> categoryConsolidated = new java.util.HashMap<>();
            try {
                Session session = m_App.getSession();
                java.util.List<String> moneyList = new java.util.ArrayList<>();
                for (ShiftData shift : allShifts) {
                    if (shift.getMoney() != null && !shift.getMoney().isEmpty()) {
                        moneyList.add(shift.getMoney());
                    }
                }
                
                if (!moneyList.isEmpty()) {
                    for (String money : moneyList) {
                        java.util.List<CategorySalesData> categorySales = new StaticSentence(session,
                            "SELECT COALESCE(categories.NAME, 'Sin Departamento') as CATEGORY_NAME, " +
                            "SUM(ticketlines.UNITS) as TOTAL_UNITS, " +
                            "SUM((ticketlines.PRICE + ticketlines.PRICE * taxes.RATE) * ticketlines.UNITS) as TOTAL_VALUE " +
                            "FROM ticketlines " +
                            "INNER JOIN tickets ON ticketlines.TICKET = tickets.ID " +
                            "INNER JOIN receipts ON tickets.ID = receipts.ID " +
                            "INNER JOIN products ON ticketlines.PRODUCT = products.ID " +
                            "LEFT JOIN categories ON products.CATEGORY = categories.ID " +
                            "INNER JOIN taxes ON ticketlines.TAXID = taxes.ID " +
                            "WHERE receipts.MONEY = ? " +
                            "GROUP BY COALESCE(categories.NAME, 'Sin Departamento')",
                            SerializerWriteString.INSTANCE,
                            new SerializerReadClass(CategorySalesData.class))
                            .list(money);
                        
                        if (categorySales != null) {
                            for (CategorySalesData catSale : categorySales) {
                                String categoryName = catSale.getCategoryName();
                                if (categoryName == null || categoryName.trim().isEmpty()) {
                                    categoryName = "Sin Departamento";
                                }
                                
                                ConsolidatedProduct consolidated = categoryConsolidated.get(categoryName);
                                if (consolidated == null) {
                                    consolidated = new ConsolidatedProduct(categoryName);
                                    categoryConsolidated.put(categoryName, consolidated);
                                }
                                
                                double units = catSale.getTotalUnits() != null ? catSale.getTotalUnits() : 0.0;
                                double totalValue = catSale.getTotalValue() != null ? catSale.getTotalValue() : 0.0;
                                
                                consolidated.addUnits(units);
                                consolidated.addTotal(totalValue);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error consolidando por departamento en showDayReport: " + e.getMessage(), e);
            }
            
            if (!categoryConsolidated.isEmpty()) {
                // Ordenar por nombre de departamento
                java.util.List<ConsolidatedProduct> sortedCategories = new java.util.ArrayList<>(categoryConsolidated.values());
                java.util.Collections.sort(sortedCategories, (a, b) -> a.getName().compareTo(b.getName()));
                
                html.append("<table>");
                html.append("<tr><th>Departamento</th><th>Cantidad</th><th>Total</th></tr>");
                for (ConsolidatedProduct category : sortedCategories) {
                    html.append("<tr>");
                    html.append("<td>").append(escapeHtml(category.getName())).append("</td>");
                    html.append("<td>").append(Formats.DOUBLE.formatValue(category.getTotalUnits())).append("</td>");
                    html.append("<td>").append(Formats.CURRENCY.formatValue(category.getTotalValue())).append("</td>");
                    html.append("</tr>");
                }
                html.append("</table>");
            } else {
                html.append("<p>Sin ventas por departamento</p>");
            }
            html.append("</div>");
            
            // SALIDAS DE EFECTIVO (DETALLADAS)
            html.append("<div class='summary'>");
            html.append("<h2>💸 Salidas de Efectivo</h2>");
            try {
                Session session = m_App.getSession();
                java.util.List<String> moneyList = new java.util.ArrayList<>();
                for (ShiftData shift : allShifts) {
                    if (shift.getMoney() != null && !shift.getMoney().isEmpty()) {
                        moneyList.add(shift.getMoney());
                    }
                }
                
                java.util.List<java.util.Map<String, Object>> outflows = new java.util.ArrayList<>();
                if (!moneyList.isEmpty()) {
                    for (String money : moneyList) {
                        java.sql.PreparedStatement cashOutStmt = session.getConnection().prepareStatement(
                            "SELECT receipts.DATENEW, payments.TOTAL, payments.NOTES " +
                            "FROM receipts " +
                            "INNER JOIN payments ON receipts.ID = payments.RECEIPT " +
                            "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'cashout' " +
                            "ORDER BY receipts.DATENEW DESC"
                        );
                        cashOutStmt.setString(1, money);
                        java.sql.ResultSet cashOutRs = cashOutStmt.executeQuery();
                        while (cashOutRs.next()) {
                            java.util.Map<String, Object> outflow = new java.util.HashMap<>();
                            outflow.put("date", cashOutRs.getTimestamp("DATENEW"));
                            outflow.put("total", cashOutRs.getDouble("TOTAL"));
                            outflow.put("notes", cashOutRs.getString("NOTES"));
                            outflows.add(outflow);
                        }
                        cashOutRs.close();
                        cashOutStmt.close();
                    }
                }
                
                if (!outflows.isEmpty()) {
                    html.append("<table>");
                    html.append("<tr><th>Fecha/Hora</th><th>Monto</th><th>Notas</th></tr>");
                    for (java.util.Map<String, Object> outflow : outflows) {
                        html.append("<tr>");
                        html.append("<td>").append(Formats.TIMESTAMP.formatValue((Date)outflow.get("date"))).append("</td>");
                        Object totalObj = outflow.get("total");
                        html.append("<td>").append(Formats.CURRENCY.formatValue(totalObj != null ? (Double)totalObj : 0.0)).append("</td>");
                        html.append("<td>").append(escapeHtml((String)outflow.get("notes"))).append("</td>");
                        html.append("</tr>");
                    }
                    html.append("</table>");
                } else {
                    html.append("<p><em>- No hubo Salidas en Efectivo -</em></p>");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo salidas de efectivo: " + e.getMessage(), e);
                html.append("<p><em>- No hubo Salidas en Efectivo -</em></p>");
            }
            html.append("</div>");
            
            // PAGOS DE CRÉDITOS
            html.append("<div class='summary'>");
            html.append("<h2>💳 Pagos de Créditos</h2>");
            try {
                Session session = m_App.getSession();
                java.util.List<String> moneyList = new java.util.ArrayList<>();
                for (ShiftData shift : allShifts) {
                    if (shift.getMoney() != null && !shift.getMoney().isEmpty()) {
                        moneyList.add(shift.getMoney());
                    }
                }
                
                java.util.List<java.util.Map<String, Object>> creditPayments = new java.util.ArrayList<>();
                if (!moneyList.isEmpty()) {
                    for (String money : moneyList) {
                        java.sql.PreparedStatement creditStmt = session.getConnection().prepareStatement(
                            "SELECT receipts.DATENEW, payments.TOTAL, payments.NOTES, customers.NAME as CUSTOMER_NAME " +
                            "FROM receipts " +
                            "INNER JOIN payments ON receipts.ID = payments.RECEIPT " +
                            "LEFT JOIN tickets ON receipts.ID = tickets.ID " +
                            "LEFT JOIN customers ON tickets.CUSTOMER = customers.ID " +
                            "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'debt' " +
                            "ORDER BY receipts.DATENEW DESC"
                        );
                        creditStmt.setString(1, money);
                        java.sql.ResultSet creditRs = creditStmt.executeQuery();
                        while (creditRs.next()) {
                            java.util.Map<String, Object> payment = new java.util.HashMap<>();
                            payment.put("date", creditRs.getTimestamp("DATENEW"));
                            payment.put("total", creditRs.getDouble("TOTAL"));
                            payment.put("notes", creditRs.getString("NOTES"));
                            payment.put("customer", creditRs.getString("CUSTOMER_NAME"));
                            creditPayments.add(payment);
                        }
                        creditRs.close();
                        creditStmt.close();
                    }
                }
                
                if (!creditPayments.isEmpty()) {
                    html.append("<table>");
                    html.append("<tr><th>Fecha/Hora</th><th>Cliente</th><th>Monto</th><th>Notas</th></tr>");
                    for (java.util.Map<String, Object> payment : creditPayments) {
                        html.append("<tr>");
                        html.append("<td>").append(Formats.TIMESTAMP.formatValue((Date)payment.get("date"))).append("</td>");
                        html.append("<td>").append(escapeHtml((String)payment.get("customer"))).append("</td>");
                        Object totalObj = payment.get("total");
                        html.append("<td>").append(Formats.CURRENCY.formatValue(totalObj != null ? (Double)totalObj : 0.0)).append("</td>");
                        html.append("<td>").append(escapeHtml((String)payment.get("notes"))).append("</td>");
                        html.append("</tr>");
                    }
                    html.append("</table>");
                } else {
                    html.append("<p><em>- No se recibieron pagos de créditos -</em></p>");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo pagos de créditos: " + e.getMessage(), e);
                html.append("<p><em>- No se recibieron pagos de créditos -</em></p>");
            }
            html.append("</div>");
            
            // CLIENTES CON MÁS VENTAS
            html.append("<div class='summary'>");
            html.append("<h2>👥 Clientes con más ventas</h2>");
            try {
                Session session = m_App.getSession();
                java.util.List<String> moneyList = new java.util.ArrayList<>();
                for (ShiftData shift : allShifts) {
                    if (shift.getMoney() != null && !shift.getMoney().isEmpty()) {
                        moneyList.add(shift.getMoney());
                    }
                }
                
                java.util.List<java.util.Map<String, Object>> topCustomersSales = new java.util.ArrayList<>();
                if (!moneyList.isEmpty()) {
                    String moneyPlaceholders = java.util.Collections.nCopies(moneyList.size(), "?").stream().collect(java.util.stream.Collectors.joining(","));
                    java.sql.PreparedStatement customerStmt = session.getConnection().prepareStatement(
                        "SELECT customers.NAME, SUM(receipts.TOTAL) as TOTAL_SALES, COUNT(receipts.ID) as COUNT_TICKETS " +
                        "FROM receipts " +
                        "INNER JOIN tickets ON receipts.ID = tickets.ID " +
                        "LEFT JOIN customers ON tickets.CUSTOMER = customers.ID " +
                        "WHERE receipts.MONEY IN (" + moneyPlaceholders + ") " +
                        "AND customers.ID IS NOT NULL " +
                        "GROUP BY customers.ID, customers.NAME " +
                        "ORDER BY TOTAL_SALES DESC " +
                        "LIMIT 10"
                    );
                    for (int i = 0; i < moneyList.size(); i++) {
                        customerStmt.setString(i + 1, moneyList.get(i));
                    }
                    java.sql.ResultSet customerRs = customerStmt.executeQuery();
                    while (customerRs.next()) {
                        java.util.Map<String, Object> customer = new java.util.HashMap<>();
                        customer.put("name", customerRs.getString("NAME"));
                        customer.put("total", customerRs.getDouble("TOTAL_SALES"));
                        customer.put("count", customerRs.getInt("COUNT_TICKETS"));
                        topCustomersSales.add(customer);
                    }
                    customerRs.close();
                    customerStmt.close();
                }
                
                if (!topCustomersSales.isEmpty()) {
                    html.append("<table>");
                    html.append("<tr><th>Cliente</th><th>Total Ventas</th><th># Tickets</th></tr>");
                    for (java.util.Map<String, Object> customer : topCustomersSales) {
                        html.append("<tr>");
                        html.append("<td>").append(escapeHtml((String)customer.get("name"))).append("</td>");
                        Object totalObj = customer.get("total");
                        html.append("<td>").append(Formats.CURRENCY.formatValue(totalObj != null ? (Double)totalObj : 0.0)).append("</td>");
                        html.append("<td>").append(customer.get("count")).append("</td>");
                        html.append("</tr>");
                    }
                    html.append("</table>");
                } else {
                    html.append("<p><em>- No hubo ventas -</em></p>");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo clientes con más ventas: " + e.getMessage(), e);
                html.append("<p><em>- No hubo ventas -</em></p>");
            }
            html.append("</div>");
            
            // CLIENTES CON MÁS GANANCIA
            html.append("<div class='summary'>");
            html.append("<h2>💰 Clientes con más ganancia</h2>");
            try {
                Session session = m_App.getSession();
                java.util.List<String> moneyList = new java.util.ArrayList<>();
                for (ShiftData shift : allShifts) {
                    if (shift.getMoney() != null && !shift.getMoney().isEmpty()) {
                        moneyList.add(shift.getMoney());
                    }
                }
                
                java.util.List<java.util.Map<String, Object>> topCustomersProfit = new java.util.ArrayList<>();
                if (!moneyList.isEmpty()) {
                    String moneyPlaceholders = java.util.Collections.nCopies(moneyList.size(), "?").stream().collect(java.util.stream.Collectors.joining(","));
                    java.sql.PreparedStatement profitStmt = session.getConnection().prepareStatement(
                        "SELECT customers.NAME, " +
                        "SUM((ticketlines.PRICE - products.PRICEBUY) * ticketlines.UNITS) as TOTAL_PROFIT, " +
                        "SUM(receipts.TOTAL) as TOTAL_SALES " +
                        "FROM receipts " +
                        "INNER JOIN tickets ON receipts.ID = tickets.ID " +
                        "INNER JOIN ticketlines ON tickets.ID = ticketlines.TICKET " +
                        "INNER JOIN products ON ticketlines.PRODUCT = products.ID " +
                        "LEFT JOIN customers ON tickets.CUSTOMER = customers.ID " +
                        "WHERE receipts.MONEY IN (" + moneyPlaceholders + ") " +
                        "AND customers.ID IS NOT NULL " +
                        "GROUP BY customers.ID, customers.NAME " +
                        "ORDER BY TOTAL_PROFIT DESC " +
                        "LIMIT 10"
                    );
                    for (int i = 0; i < moneyList.size(); i++) {
                        profitStmt.setString(i + 1, moneyList.get(i));
                    }
                    java.sql.ResultSet profitRs = profitStmt.executeQuery();
                    while (profitRs.next()) {
                        java.util.Map<String, Object> customer = new java.util.HashMap<>();
                        customer.put("name", profitRs.getString("NAME"));
                        customer.put("profit", profitRs.getDouble("TOTAL_PROFIT"));
                        customer.put("sales", profitRs.getDouble("TOTAL_SALES"));
                        topCustomersProfit.add(customer);
                    }
                    profitRs.close();
                    profitStmt.close();
                }
                
                if (!topCustomersProfit.isEmpty()) {
                    html.append("<table>");
                    html.append("<tr><th>Cliente</th><th>Ganancia</th><th>Total Ventas</th></tr>");
                    for (java.util.Map<String, Object> customer : topCustomersProfit) {
                        html.append("<tr>");
                        html.append("<td>").append(escapeHtml((String)customer.get("name"))).append("</td>");
                        Object profitObj = customer.get("profit");
                        Object salesObj = customer.get("sales");
                        html.append("<td>").append(Formats.CURRENCY.formatValue(profitObj != null ? (Double)profitObj : 0.0)).append("</td>");
                        html.append("<td>").append(Formats.CURRENCY.formatValue(salesObj != null ? (Double)salesObj : 0.0)).append("</td>");
                        html.append("</tr>");
                    }
                    html.append("</table>");
                } else {
                    html.append("<p><em>- No hubo ventas -</em></p>");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo clientes con más ganancia: " + e.getMessage(), e);
                html.append("<p><em>- No hubo ventas -</em></p>");
            }
            html.append("</div>");
            
            // IMPUESTOS (ANÁLISIS DE IMPUESTOS)
            html.append("<div class='summary'>");
            html.append("<h2>📋 Análisis de Impuestos</h2>");
            try {
                Session session = m_App.getSession();
                java.util.List<String> moneyList = new java.util.ArrayList<>();
                for (ShiftData shift : allShifts) {
                    if (shift.getMoney() != null && !shift.getMoney().isEmpty()) {
                        moneyList.add(shift.getMoney());
                    }
                }
                
                java.util.List<java.util.Map<String, Object>> taxes = new java.util.ArrayList<>();
                if (!moneyList.isEmpty()) {
                    String moneyPlaceholders = java.util.Collections.nCopies(moneyList.size(), "?").stream().collect(java.util.stream.Collectors.joining(","));
                    java.sql.PreparedStatement taxStmt = session.getConnection().prepareStatement(
                        "SELECT taxcategories.NAME, SUM(taxlines.AMOUNT) as TOTAL_TAX, SUM(taxlines.BASE) as TOTAL_BASE " +
                        "FROM receipts " +
                        "INNER JOIN taxlines ON receipts.ID = taxlines.RECEIPT " +
                        "INNER JOIN taxes ON taxlines.TAXID = taxes.ID " +
                        "INNER JOIN taxcategories ON taxes.CATEGORY = taxcategories.ID " +
                        "WHERE receipts.MONEY IN (" + moneyPlaceholders + ") " +
                        "GROUP BY taxcategories.NAME " +
                        "ORDER BY TOTAL_TAX DESC"
                    );
                    for (int i = 0; i < moneyList.size(); i++) {
                        taxStmt.setString(i + 1, moneyList.get(i));
                    }
                    java.sql.ResultSet taxRs = taxStmt.executeQuery();
                    while (taxRs.next()) {
                        java.util.Map<String, Object> tax = new java.util.HashMap<>();
                        tax.put("name", taxRs.getString("NAME"));
                        tax.put("amount", taxRs.getDouble("TOTAL_TAX"));
                        tax.put("base", taxRs.getDouble("TOTAL_BASE"));
                        taxes.add(tax);
                    }
                    taxRs.close();
                    taxStmt.close();
                }
                
                if (!taxes.isEmpty()) {
                    html.append("<table>");
                    html.append("<tr><th>Categoría de Impuesto</th><th>Base</th><th>Monto</th></tr>");
                    double totalTaxAmount = 0.0;
                    double totalTaxBase = 0.0;
                    for (java.util.Map<String, Object> tax : taxes) {
                        html.append("<tr>");
                        html.append("<td>").append(escapeHtml((String)tax.get("name"))).append("</td>");
                        Object baseObj = tax.get("base");
                        Object amountObj = tax.get("amount");
                        html.append("<td>").append(Formats.CURRENCY.formatValue(baseObj != null ? (Double)baseObj : 0.0)).append("</td>");
                        html.append("<td>").append(Formats.CURRENCY.formatValue(amountObj != null ? (Double)amountObj : 0.0)).append("</td>");
                        html.append("</tr>");
                        totalTaxAmount += (Double)tax.get("amount");
                        totalTaxBase += (Double)tax.get("base");
                    }
                    html.append("<tr class='total'><td><strong>TOTAL</strong></td><td><strong>").append(Formats.CURRENCY.formatValue(totalTaxBase)).append("</strong></td><td><strong>").append(Formats.CURRENCY.formatValue(totalTaxAmount)).append("</strong></td></tr>");
                    html.append("</table>");
                } else {
                    html.append("<p><em>- No hay impuestos registrados -</em></p>");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo análisis de impuestos: " + e.getMessage(), e);
                html.append("<p><em>- No hay impuestos registrados -</em></p>");
            }
            html.append("</div>");
            
            // DEVOLUCIONES (PRODUCTOS ELIMINADOS/VOIDS)
            html.append("<div class='summary'>");
            html.append("<h2>↩️ Devoluciones</h2>");
            try {
                Session session = m_App.getSession();
                java.util.List<String> moneyList = new java.util.ArrayList<>();
                for (ShiftData shift : allShifts) {
                    if (shift.getMoney() != null && !shift.getMoney().isEmpty()) {
                        moneyList.add(shift.getMoney());
                    }
                }
                
                java.util.List<java.util.Map<String, Object>> returns = new java.util.ArrayList<>();
                if (!moneyList.isEmpty()) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(closeDate);
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    cal.set(java.util.Calendar.MINUTE, 0);
                    cal.set(java.util.Calendar.SECOND, 0);
                    cal.set(java.util.Calendar.MILLISECOND, 0);
                    java.sql.Timestamp dayStart = new java.sql.Timestamp(cal.getTimeInMillis());
                    
                    cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
                    java.sql.Timestamp dayEnd = new java.sql.Timestamp(cal.getTimeInMillis());
                    
                    java.sql.PreparedStatement returnStmt = session.getConnection().prepareStatement(
                        "SELECT lineremoved.REMOVEDDATE, lineremoved.NAME, lineremoved.UNITS, " +
                        "lineremoved.PRICE, people.NAME as USER_NAME, tickets.ID as TICKET_ID " +
                        "FROM lineremoved " +
                        "LEFT JOIN people ON lineremoved.NAME = people.NAME " +
                        "LEFT JOIN tickets ON lineremoved.NAME = tickets.ID " +
                        "WHERE lineremoved.REMOVEDDATE >= ? AND lineremoved.REMOVEDDATE < ? " +
                        "ORDER BY lineremoved.REMOVEDDATE DESC"
                    );
                    returnStmt.setTimestamp(1, dayStart);
                    returnStmt.setTimestamp(2, dayEnd);
                    java.sql.ResultSet returnRs = returnStmt.executeQuery();
                    while (returnRs.next()) {
                        java.util.Map<String, Object> ret = new java.util.HashMap<>();
                        ret.put("date", returnRs.getTimestamp("REMOVEDDATE"));
                        ret.put("product", returnRs.getString("NAME"));
                        ret.put("units", returnRs.getDouble("UNITS"));
                        ret.put("price", returnRs.getDouble("PRICE"));
                        ret.put("user", returnRs.getString("USER_NAME"));
                        ret.put("ticket", returnRs.getString("TICKET_ID"));
                        returns.add(ret);
                    }
                    returnRs.close();
                    returnStmt.close();
                }
                
                if (!returns.isEmpty()) {
                    html.append("<table>");
                    html.append("<tr><th>Fecha/Hora</th><th>Producto</th><th>Cantidad</th><th>Precio</th><th>Usuario</th><th>Ticket</th></tr>");
                    for (java.util.Map<String, Object> ret : returns) {
                        html.append("<tr>");
                        html.append("<td>").append(Formats.TIMESTAMP.formatValue((Date)ret.get("date"))).append("</td>");
                        html.append("<td>").append(escapeHtml((String)ret.get("product"))).append("</td>");
                        Object unitsObj = ret.get("units");
                        Object priceObj = ret.get("price");
                        html.append("<td>").append(Formats.DOUBLE.formatValue(unitsObj != null ? (Double)unitsObj : 0.0)).append("</td>");
                        html.append("<td>").append(Formats.CURRENCY.formatValue(priceObj != null ? (Double)priceObj : 0.0)).append("</td>");
                        html.append("<td>").append(escapeHtml((String)ret.get("user"))).append("</td>");
                        html.append("<td>").append(escapeHtml((String)ret.get("ticket"))).append("</td>");
                        html.append("</tr>");
                    }
                    html.append("</table>");
                } else {
                    html.append("<p><em>- No hubo devoluciones -</em></p>");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo devoluciones: " + e.getMessage(), e);
                html.append("<p><em>- No hubo devoluciones -</em></p>");
            }
            html.append("</div>");
            
            // Resumen total del día
            double totalDia = totalDayInitialAmount + totalDaySales + totalDayCashIn - totalDayCashOut;
            
            html.append("<div class='summary' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none;'>");
            html.append("<h2 style='color: white; border-left-color: white;'>🎯 TOTAL DEL DÍA</h2>");
            html.append("<table style='background: rgba(255,255,255,0.1);'>");
            html.append("<tr style='background: rgba(255,255,255,0.2);'><td style='color: white; padding: 15px;'><strong>Total Fondo Inicial:</strong></td><td style='color: white; padding: 15px; font-size: 1.1em;'>").append(Formats.CURRENCY.formatValue(totalDayInitialAmount)).append("</td></tr>");
            html.append("<tr><td style='color: white; padding: 15px;'><strong>Total Ventas:</strong></td><td style='color: white; padding: 15px; font-size: 1.1em;'>").append(Formats.CURRENCY.formatValue(totalDaySales)).append("</td></tr>");
            html.append("<tr style='background: rgba(255,255,255,0.2);'><td style='color: white; padding: 15px;'><strong>Total Entradas:</strong></td><td style='color: white; padding: 15px; font-size: 1.1em;'>").append(Formats.CURRENCY.formatValue(totalDayCashIn)).append("</td></tr>");
            html.append("<tr><td style='color: white; padding: 15px;'><strong>Total Salidas:</strong></td><td style='color: white; padding: 15px; font-size: 1.1em;'>").append(Formats.CURRENCY.formatValue(totalDayCashOut)).append("</td></tr>");
            html.append("<tr style='background: rgba(255,255,255,0.3); border-top: 2px solid white;'><td style='color: white; padding: 20px; font-size: 1.3em;'><strong>TOTAL DEL DÍA:</strong></td><td style='color: white; padding: 20px; font-size: 1.5em; font-weight: bold;'>").append(Formats.CURRENCY.formatValue(totalDia)).append("</td></tr>");
            html.append("</table>");
            html.append("</div>");
            
            html.append("</div>"); // Cerrar container
            html.append("</body></html>");
            
            // Crear diálogo para mostrar el reporte HTML
            JDialog reportDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                "Reporte de Caja Cerrada - Día Completo", true);
            reportDialog.setSize(1000, 700);
            reportDialog.setLocationRelativeTo(this);
            
            // Crear panel con el visor HTML
            JPanel panel = new JPanel(new BorderLayout());
            JEditorPane htmlViewer = new JEditorPane();
            htmlViewer.setContentType("text/html");
            htmlViewer.setEditable(false);
            htmlViewer.setText(html.toString());
            JScrollPane scrollPane = new JScrollPane(htmlViewer);
            panel.add(scrollPane, BorderLayout.CENTER);
            
            // Panel de botones: Guardar HTML, Imprimir y Cerrar
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnSaveHTML = new JButton("Guardar HTML");
            JButton btnPrint = new JButton("Imprimir");
            JButton btnClose = new JButton("Cerrar");
            
            final String htmlContent = html.toString(); // Para usar en el listener
            
            btnSaveHTML.addActionListener(e -> {
                try {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Guardar reporte como HTML");
                    fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos HTML (*.html)", "html"));
                    
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                    String defaultFileName = "Reporte_Caja_Cerrada_" + sdf.format(closeDate) + ".html";
                    fileChooser.setSelectedFile(new java.io.File(defaultFileName));
                    
                    int userSelection = fileChooser.showSaveDialog(reportDialog);
                    if (userSelection == JFileChooser.APPROVE_OPTION) {
                        java.io.File fileToSave = fileChooser.getSelectedFile();
                        String filePath = fileToSave.getAbsolutePath();
                        
                        if (!filePath.toLowerCase().endsWith(".html")) {
                            filePath += ".html";
                        }
                        
                        java.io.FileWriter writer = new java.io.FileWriter(filePath);
                        writer.write(htmlContent);
                        writer.close();
                        
                        JOptionPane.showMessageDialog(reportDialog,
                            "Reporte guardado exitosamente en:\n" + filePath,
                            "Éxito",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (java.io.IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error guardando HTML", ex);
                    JOptionPane.showMessageDialog(reportDialog,
                        "Error al guardar el HTML: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            });
            
            btnPrint.addActionListener(e -> {
                try {
                    htmlViewer.print();
                } catch (java.awt.print.PrinterException ex) {
                    LOGGER.log(Level.SEVERE, "Error imprimiendo", ex);
                    JOptionPane.showMessageDialog(reportDialog,
                        "Error al imprimir: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            });
            
            btnClose.addActionListener(e -> reportDialog.dispose());
            
            buttonPanel.add(btnSaveHTML);
            buttonPanel.add(btnPrint);
            buttonPanel.add(btnClose);
            panel.add(buttonPanel, BorderLayout.SOUTH);
            
            reportDialog.add(panel);
            reportDialog.setVisible(true);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error mostrando reporte del día", e);
            JOptionPane.showMessageDialog(this,
                "Error al mostrar el reporte: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Muestra el reporte del turno actual (cierre de cajero)
     */
    private void showShiftReport() {
        try {
            if (m_PaymentsToClose == null) {
                LOGGER.warning("No hay datos de turno para mostrar");
                return;
            }
            
            LOGGER.info("Generando reporte visual del turno actual...");
            
            Date shiftDate = m_PaymentsToClose.getDateEnd() != null ? m_PaymentsToClose.getDateEnd() : new Date();
            
            // Generar HTML del reporte
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head>");
            html.append("<meta charset='UTF-8'>");
            html.append("<title>Reporte de Caja Cerrada - Turno Actual</title>");
            html.append("<style>");
            html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
            html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%); min-height: 100vh; }");
            html.append(".container { max-width: 1200px; margin: 0 auto; background: white; border-radius: 15px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); padding: 30px; }");
            html.append("h1 { text-align: center; color: #2c3e50; font-size: 2.5em; margin-bottom: 10px; text-shadow: 2px 2px 4px rgba(0,0,0,0.1); }");
            html.append(".header-info { text-align: center; color: #7f8c8d; margin-bottom: 30px; font-size: 1.1em; }");
            html.append("h2 { color: #2c3e50; border-left: 5px solid #3498db; padding-left: 15px; margin: 25px 0 15px 0; font-size: 1.8em; }");
            html.append("h3 { color: #34495e; margin: 20px 0 10px 0; font-size: 1.4em; }");
            html.append("table { width: 100%; border-collapse: separate; border-spacing: 0; margin: 15px 0; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.08); }");
            html.append("th { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 15px 12px; text-align: left; font-weight: 600; text-transform: uppercase; font-size: 0.9em; letter-spacing: 0.5px; }");
            html.append("td { padding: 12px; border-bottom: 1px solid #ecf0f1; background-color: white; }");
            html.append("tr:last-child td { border-bottom: none; }");
            html.append("tr:nth-child(even) td { background-color: #f8f9fa; }");
            html.append("tr:hover td { background-color: #e8f4f8; transition: background-color 0.3s ease; }");
            html.append(".total { font-weight: bold; font-size: 1.2em; color: #2c3e50; background-color: #ecf0f1 !important; }");
            html.append(".shift-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; margin: 25px 0 20px 0; border-radius: 10px; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3); }");
            html.append(".shift-header h2 { color: white; border: none; padding: 0; margin: 0 0 10px 0; }");
            html.append(".shift-header p { margin: 5px 0; font-size: 1em; }");
            html.append(".summary { background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%); padding: 25px; margin: 25px 0; border-radius: 12px; border: 1px solid #e1e8ed; box-shadow: 0 4px 15px rgba(0,0,0,0.08); transition: transform 0.3s ease, box-shadow 0.3s ease; }");
            html.append(".summary:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.12); }");
            html.append(".summary h2 { border-left-color: #3498db; }");
            html.append("p { margin: 10px 0; line-height: 1.6; color: #555; }");
            html.append("em { color: #95a5a6; font-style: italic; }");
            html.append("@media print { body { background: white; } .container { box-shadow: none; } }");
            html.append("</style>");
            html.append("</head><body>");
            html.append("<div class='container'>");
            
            html.append("<h1>📊 REPORTE DE CAJA CERRADA</h1>");
            html.append("<div class='header-info'>");
            html.append("<p><strong>📅 Fecha:</strong> ").append(Formats.DATE.formatValue(shiftDate)).append("</p>");
            html.append("<p><strong>🕐 Generado:</strong> ").append(Formats.TIMESTAMP.formatValue(new Date())).append("</p>");
            html.append("</div>");
            
            // Encabezado del turno
            html.append("<div class='shift-header'>");
            html.append("<h2>TURNO ").append(m_PaymentsToClose.printSequence()).append(" - ").append(m_PaymentsToClose.printUser()).append("</h2>");
            html.append("<p><strong>Inicio:</strong> ").append(m_PaymentsToClose.printDateStart()).append("</p>");
            html.append("<p><strong>Fin:</strong> ").append(m_PaymentsToClose.printDateEnd()).append("</p>");
            html.append("</div>");
            
            // Resumen del turno
            double initialAmount = m_PaymentsToClose.getInitialAmount() != null ? m_PaymentsToClose.getInitialAmount() : 0.0;
            // Calcular total de ventas desde base + impuestos
            double salesBase = 0.0;
            double salesTaxes = 0.0;
            try {
                // Obtener valores desde la base de datos directamente
                Session session = m_App.getSession();
                String activeCashIndex = m_App.getActiveCashIndex();
                Date dateStart = m_PaymentsToClose.getDateStart();
                
                if (dateStart != null) {
                    java.sql.PreparedStatement salesStmt = session.getConnection().prepareStatement(
                        "SELECT COALESCE(SUM(receipts.TOTAL), 0) AS TOTAL " +
                        "FROM receipts " +
                        "WHERE receipts.MONEY = ? AND receipts.DATENEW >= ?"
                    );
                    salesStmt.setString(1, activeCashIndex);
                    salesStmt.setTimestamp(2, new java.sql.Timestamp(dateStart.getTime()));
                    java.sql.ResultSet salesRs = salesStmt.executeQuery();
                    if (salesRs.next()) {
                        salesBase = salesRs.getDouble("TOTAL");
                    }
                    salesRs.close();
                    salesStmt.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo total de ventas: " + e.getMessage(), e);
            }
            double totalSales = salesBase;
            double cashIn = 0.0;
            double cashOut = 0.0;
            
            // Obtener entradas y salidas del turno actual
            try {
                Session session = m_App.getSession();
                String activeCashIndex = m_App.getActiveCashIndex();
                Date dateStart = m_PaymentsToClose.getDateStart();
                
                if (dateStart != null) {
                    // Entradas (cashin)
                    java.sql.PreparedStatement cashInStmt = session.getConnection().prepareStatement(
                        "SELECT COALESCE(SUM(payments.TOTAL), 0) AS TOTAL " +
                        "FROM payments " +
                        "INNER JOIN receipts ON payments.RECEIPT = receipts.ID " +
                        "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'cashin' AND receipts.DATENEW >= ?"
                    );
                    cashInStmt.setString(1, activeCashIndex);
                    cashInStmt.setTimestamp(2, new java.sql.Timestamp(dateStart.getTime()));
                    java.sql.ResultSet cashInRs = cashInStmt.executeQuery();
                    if (cashInRs.next()) {
                        cashIn = cashInRs.getDouble("TOTAL");
                    }
                    cashInRs.close();
                    cashInStmt.close();
                    
                    // Salidas (cashout)
                    java.sql.PreparedStatement cashOutStmt = session.getConnection().prepareStatement(
                        "SELECT COALESCE(SUM(ABS(payments.TOTAL)), 0) AS TOTAL " +
                        "FROM payments " +
                        "INNER JOIN receipts ON payments.RECEIPT = receipts.ID " +
                        "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'cashout' AND receipts.DATENEW >= ?"
                    );
                    cashOutStmt.setString(1, activeCashIndex);
                    cashOutStmt.setTimestamp(2, new java.sql.Timestamp(dateStart.getTime()));
                    java.sql.ResultSet cashOutRs = cashOutStmt.executeQuery();
                    if (cashOutRs.next()) {
                        cashOut = cashOutRs.getDouble("TOTAL");
                    }
                    cashOutRs.close();
                    cashOutStmt.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo entradas/salidas del turno: " + e.getMessage(), e);
            }
            
            double totalTurno = initialAmount + totalSales + cashIn - cashOut;
            
            html.append("<div class='summary'>");
            html.append("<h3>📊 Resumen del Turno</h3>");
            html.append("<table>");
            html.append("<tr style='background-color: #f8f9fa;'><td style='padding: 12px;'><strong>Monto Inicial del Turno:</strong></td><td style='padding: 12px; font-size: 1.1em; color: #27ae60;'><strong>").append(Formats.CURRENCY.formatValue(initialAmount)).append("</strong></td></tr>");
            html.append("<tr><td style='padding: 12px;'><strong>Total de Ventas:</strong></td><td style='padding: 12px; font-size: 1.1em; color: #2c3e50;'>").append(Formats.CURRENCY.formatValue(totalSales)).append("</td></tr>");
            html.append("<tr style='background-color: #f8f9fa;'><td style='padding: 12px;'><strong>Entradas:</strong></td><td style='padding: 12px; font-size: 1.1em; color: #27ae60;'>").append(Formats.CURRENCY.formatValue(cashIn)).append("</td></tr>");
            html.append("<tr><td style='padding: 12px;'><strong>Salidas:</strong></td><td style='padding: 12px; font-size: 1.1em; color: #e74c3c;'>").append(Formats.CURRENCY.formatValue(cashOut)).append("</td></tr>");
            html.append("<tr style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-top: 2px solid #667eea;'><td style='padding: 15px; color: white; font-size: 1.2em;'><strong>TOTAL DEL TURNO:</strong></td><td style='padding: 15px; color: white; font-size: 1.3em; font-weight: bold;'>").append(Formats.CURRENCY.formatValue(totalTurno)).append("</td></tr>");
            html.append("</table>");
            html.append("</div>");
            
            // VENTAS POR DEPARTAMENTO DEL TURNO
            html.append("<div class='summary'>");
            html.append("<h2>🏢 VENTAS POR DEPARTAMENTO</h2>");
            
            try {
                Session session = m_App.getSession();
                String activeCashIndex = m_App.getActiveCashIndex();
                Date dateStart = m_PaymentsToClose.getDateStart();
                
                java.util.Map<String, ConsolidatedProduct> categoryConsolidated = new java.util.HashMap<>();
                
                if (dateStart != null) {
                    java.sql.PreparedStatement categoryStmt = session.getConnection().prepareStatement(
                        "SELECT COALESCE(categories.NAME, 'Sin Departamento') as CATEGORY_NAME, " +
                        "SUM(ticketlines.UNITS) as TOTAL_UNITS, " +
                        "SUM((ticketlines.PRICE + ticketlines.PRICE * taxes.RATE) * ticketlines.UNITS) as TOTAL_VALUE " +
                        "FROM ticketlines " +
                        "INNER JOIN tickets ON ticketlines.TICKET = tickets.ID " +
                        "INNER JOIN receipts ON tickets.ID = receipts.ID " +
                        "INNER JOIN products ON ticketlines.PRODUCT = products.ID " +
                        "LEFT JOIN categories ON products.CATEGORY = categories.ID " +
                        "INNER JOIN taxes ON ticketlines.TAXID = taxes.ID " +
                        "WHERE receipts.MONEY = ? AND receipts.DATENEW >= ? " +
                        "GROUP BY COALESCE(categories.NAME, 'Sin Departamento')"
                    );
                    categoryStmt.setString(1, activeCashIndex);
                    categoryStmt.setTimestamp(2, new java.sql.Timestamp(dateStart.getTime()));
                    java.sql.ResultSet categoryRs = categoryStmt.executeQuery();
                    
                    while (categoryRs.next()) {
                        String categoryName = categoryRs.getString("CATEGORY_NAME");
                        if (categoryName == null || categoryName.trim().isEmpty()) {
                            categoryName = "Sin Departamento";
                        }
                        
                        ConsolidatedProduct consolidated = categoryConsolidated.get(categoryName);
                        if (consolidated == null) {
                            consolidated = new ConsolidatedProduct(categoryName);
                            categoryConsolidated.put(categoryName, consolidated);
                        }
                        
                        double units = categoryRs.getDouble("TOTAL_UNITS");
                        double totalValue = categoryRs.getDouble("TOTAL_VALUE");
                        
                        consolidated.addUnits(units);
                        consolidated.addTotal(totalValue);
                    }
                    categoryRs.close();
                    categoryStmt.close();
                }
                
                if (!categoryConsolidated.isEmpty()) {
                    java.util.List<ConsolidatedProduct> sortedCategories = new java.util.ArrayList<>(categoryConsolidated.values());
                    java.util.Collections.sort(sortedCategories, (a, b) -> a.getName().compareTo(b.getName()));
                    
                    html.append("<table>");
                    html.append("<tr><th>Departamento</th><th>Cantidad</th><th>Total</th></tr>");
                    for (ConsolidatedProduct category : sortedCategories) {
                        html.append("<tr>");
                        html.append("<td>").append(escapeHtml(category.getName())).append("</td>");
                        html.append("<td>").append(Formats.DOUBLE.formatValue(category.getTotalUnits())).append("</td>");
                        html.append("<td>").append(Formats.CURRENCY.formatValue(category.getTotalValue())).append("</td>");
                        html.append("</tr>");
                    }
                    html.append("</table>");
                } else {
                    html.append("<p><em>- Sin ventas por departamento -</em></p>");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error consolidando por departamento en showShiftReport: " + e.getMessage(), e);
                html.append("<p><em>- Sin ventas por departamento -</em></p>");
            }
            html.append("</div>");
            
            // SALIDAS DE EFECTIVO
            html.append("<div class='summary'>");
            html.append("<h2>💸 Salidas de Efectivo</h2>");
            try {
                Session session = m_App.getSession();
                String activeCashIndex = m_App.getActiveCashIndex();
                Date dateStart = m_PaymentsToClose.getDateStart();
                
                java.util.List<java.util.Map<String, Object>> outflows = new java.util.ArrayList<>();
                if (dateStart != null) {
                    java.sql.PreparedStatement cashOutStmt = session.getConnection().prepareStatement(
                        "SELECT receipts.DATENEW, payments.TOTAL, payments.NOTES " +
                        "FROM receipts " +
                        "INNER JOIN payments ON receipts.ID = payments.RECEIPT " +
                        "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'cashout' AND receipts.DATENEW >= ? " +
                        "ORDER BY receipts.DATENEW DESC"
                    );
                    cashOutStmt.setString(1, activeCashIndex);
                    cashOutStmt.setTimestamp(2, new java.sql.Timestamp(dateStart.getTime()));
                    java.sql.ResultSet cashOutRs = cashOutStmt.executeQuery();
                    while (cashOutRs.next()) {
                        java.util.Map<String, Object> outflow = new java.util.HashMap<>();
                        outflow.put("date", cashOutRs.getTimestamp("DATENEW"));
                        outflow.put("total", cashOutRs.getDouble("TOTAL"));
                        outflow.put("notes", cashOutRs.getString("NOTES"));
                        outflows.add(outflow);
                    }
                    cashOutRs.close();
                    cashOutStmt.close();
                }
                
                if (!outflows.isEmpty()) {
                    html.append("<table>");
                    html.append("<tr><th>Fecha/Hora</th><th>Monto</th><th>Notas</th></tr>");
                    for (java.util.Map<String, Object> outflow : outflows) {
                        html.append("<tr>");
                        html.append("<td>").append(Formats.TIMESTAMP.formatValue((Date)outflow.get("date"))).append("</td>");
                        Object totalObj = outflow.get("total");
                        html.append("<td>").append(Formats.CURRENCY.formatValue(totalObj != null ? (Double)totalObj : 0.0)).append("</td>");
                        html.append("<td>").append(escapeHtml((String)outflow.get("notes"))).append("</td>");
                        html.append("</tr>");
                    }
                    html.append("</table>");
                } else {
                    html.append("<p><em>- No hubo Salidas en Efectivo -</em></p>");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo salidas de efectivo: " + e.getMessage(), e);
                html.append("<p><em>- No hubo Salidas en Efectivo -</em></p>");
            }
            html.append("</div>");
            
            // PAGOS DE CRÉDITOS
            html.append("<div class='summary'>");
            html.append("<h2>💳 Pagos de Créditos</h2>");
            try {
                Session session = m_App.getSession();
                String activeCashIndex = m_App.getActiveCashIndex();
                Date dateStart = m_PaymentsToClose.getDateStart();
                
                java.util.List<java.util.Map<String, Object>> creditPayments = new java.util.ArrayList<>();
                if (dateStart != null) {
                    java.sql.PreparedStatement creditStmt = session.getConnection().prepareStatement(
                        "SELECT receipts.DATENEW, payments.TOTAL, payments.NOTES, customers.NAME as CUSTOMER_NAME " +
                        "FROM receipts " +
                        "INNER JOIN payments ON receipts.ID = payments.RECEIPT " +
                        "LEFT JOIN tickets ON receipts.ID = tickets.ID " +
                        "LEFT JOIN customers ON tickets.CUSTOMER = customers.ID " +
                        "WHERE receipts.MONEY = ? AND payments.PAYMENT = 'debt' AND receipts.DATENEW >= ? " +
                        "ORDER BY receipts.DATENEW DESC"
                    );
                    creditStmt.setString(1, activeCashIndex);
                    creditStmt.setTimestamp(2, new java.sql.Timestamp(dateStart.getTime()));
                    java.sql.ResultSet creditRs = creditStmt.executeQuery();
                    while (creditRs.next()) {
                        java.util.Map<String, Object> payment = new java.util.HashMap<>();
                        payment.put("date", creditRs.getTimestamp("DATENEW"));
                        payment.put("total", creditRs.getDouble("TOTAL"));
                        payment.put("notes", creditRs.getString("NOTES"));
                        payment.put("customer", creditRs.getString("CUSTOMER_NAME"));
                        creditPayments.add(payment);
                    }
                    creditRs.close();
                    creditStmt.close();
                }
                
                if (!creditPayments.isEmpty()) {
                    html.append("<table>");
                    html.append("<tr><th>Fecha/Hora</th><th>Cliente</th><th>Monto</th><th>Notas</th></tr>");
                    for (java.util.Map<String, Object> payment : creditPayments) {
                        html.append("<tr>");
                        html.append("<td>").append(Formats.TIMESTAMP.formatValue((Date)payment.get("date"))).append("</td>");
                        html.append("<td>").append(escapeHtml((String)payment.get("customer"))).append("</td>");
                        Object totalObj = payment.get("total");
                        html.append("<td>").append(Formats.CURRENCY.formatValue(totalObj != null ? (Double)totalObj : 0.0)).append("</td>");
                        html.append("<td>").append(escapeHtml((String)payment.get("notes"))).append("</td>");
                        html.append("</tr>");
                    }
                    html.append("</table>");
                } else {
                    html.append("<p><em>- No se recibieron pagos de créditos -</em></p>");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo pagos de créditos: " + e.getMessage(), e);
                html.append("<p><em>- No se recibieron pagos de créditos -</em></p>");
            }
            html.append("</div>");
            
            // ANÁLISIS DE IMPUESTOS
            html.append("<div class='summary'>");
            html.append("<h2>📋 Análisis de Impuestos</h2>");
            try {
                if (m_PaymentsToClose.getSaleLines() != null && !m_PaymentsToClose.getSaleLines().isEmpty()) {
                    html.append("<table>");
                    html.append("<tr><th>Categoría de Impuesto</th><th>Base</th><th>Monto</th></tr>");
                    double totalTaxAmount = 0.0;
                    double totalTaxBase = 0.0;
                    for (PaymentsModel.SalesLine taxLine : m_PaymentsToClose.getSaleLines()) {
                        html.append("<tr>");
                        html.append("<td>").append(escapeHtml(taxLine.printTaxName())).append("</td>");
                        html.append("<td>").append(taxLine.printTaxNet()).append("</td>");
                        html.append("<td>").append(taxLine.printTaxes()).append("</td>");
                        html.append("</tr>");
                        // Intentar extraer valores numéricos si es posible
                        try {
                            String taxBaseStr = taxLine.printTaxNet().replaceAll("[^0-9.,-]", "").replace(",", "");
                            String taxAmountStr = taxLine.printTaxes().replaceAll("[^0-9.,-]", "").replace(",", "");
                            if (!taxBaseStr.isEmpty()) totalTaxBase += Double.parseDouble(taxBaseStr);
                            if (!taxAmountStr.isEmpty()) totalTaxAmount += Double.parseDouble(taxAmountStr);
                        } catch (Exception ex) {
                            // Ignorar si no se puede parsear
                        }
                    }
                    html.append("<tr class='total'><td><strong>TOTAL</strong></td><td><strong>").append(Formats.CURRENCY.formatValue(totalTaxBase)).append("</strong></td><td><strong>").append(Formats.CURRENCY.formatValue(totalTaxAmount)).append("</strong></td></tr>");
                    html.append("</table>");
                } else {
                    html.append("<p><em>- No hay impuestos registrados -</em></p>");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo análisis de impuestos: " + e.getMessage(), e);
                html.append("<p><em>- No hay impuestos registrados -</em></p>");
            }
            html.append("</div>");
            
            // DEVOLUCIONES
            html.append("<div class='summary'>");
            html.append("<h2>↩️ Devoluciones</h2>");
            try {
                if (m_PaymentsToClose.getRemovedProductLines() != null && !m_PaymentsToClose.getRemovedProductLines().isEmpty()) {
                    html.append("<table>");
                    html.append("<tr><th>Ticket</th><th>Usuario</th><th>Producto</th><th>Cantidad</th></tr>");
                    for (PaymentsModel.RemovedProductLines removed : m_PaymentsToClose.getRemovedProductLines()) {
                        html.append("<tr>");
                        html.append("<td>").append(escapeHtml(removed.printTicketId())).append("</td>");
                        html.append("<td>").append(escapeHtml(removed.printWorkerName())).append("</td>");
                        html.append("<td>").append(escapeHtml(removed.printProductName())).append("</td>");
                        html.append("<td>").append(removed.printTotalUnits()).append("</td>");
                        html.append("</tr>");
                    }
                    html.append("</table>");
                } else {
                    html.append("<p><em>- No hubo devoluciones -</em></p>");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obteniendo devoluciones: " + e.getMessage(), e);
                html.append("<p><em>- No hubo devoluciones -</em></p>");
            }
            html.append("</div>");
            
            html.append("</div>"); // Cerrar container
            html.append("</body></html>");
            
            // Crear diálogo para mostrar el reporte HTML
            JDialog reportDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                "Reporte de Caja Cerrada - Turno Actual", true);
            reportDialog.setSize(1000, 700);
            reportDialog.setLocationRelativeTo(this);
            
            // Crear panel con el visor HTML
            JPanel panel = new JPanel(new BorderLayout());
            JEditorPane htmlViewer = new JEditorPane();
            htmlViewer.setContentType("text/html");
            htmlViewer.setEditable(false);
            htmlViewer.setText(html.toString());
            JScrollPane scrollPane = new JScrollPane(htmlViewer);
            panel.add(scrollPane, BorderLayout.CENTER);
            
            // Panel de botones: Guardar HTML, Imprimir y Cerrar
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnSaveHTML = new JButton("Guardar HTML");
            JButton btnPrint = new JButton("Imprimir");
            JButton btnClose = new JButton("Cerrar");
            
            final String htmlContent = html.toString();
            
            btnSaveHTML.addActionListener(e -> {
                try {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Guardar reporte como HTML");
                    fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos HTML (*.html)", "html"));
                    
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd_HHmmss");
                    String defaultFileName = "Reporte_Turno_" + sdf.format(new Date()) + ".html";
                    fileChooser.setSelectedFile(new java.io.File(defaultFileName));
                    
                    int userSelection = fileChooser.showSaveDialog(reportDialog);
                    if (userSelection == JFileChooser.APPROVE_OPTION) {
                        java.io.File fileToSave = fileChooser.getSelectedFile();
                        String filePath = fileToSave.getAbsolutePath();
                        
                        if (!filePath.toLowerCase().endsWith(".html")) {
                            filePath += ".html";
                        }
                        
                        java.io.FileWriter writer = new java.io.FileWriter(filePath);
                        writer.write(htmlContent);
                        writer.close();
                        
                        JOptionPane.showMessageDialog(reportDialog,
                            "Reporte guardado exitosamente en:\n" + filePath,
                            "Éxito",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (java.io.IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error guardando HTML", ex);
                    JOptionPane.showMessageDialog(reportDialog,
                        "Error al guardar el HTML: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            });
            
            btnPrint.addActionListener(e -> {
                try {
                    htmlViewer.print();
                } catch (java.awt.print.PrinterException ex) {
                    LOGGER.log(Level.SEVERE, "Error imprimiendo", ex);
                    JOptionPane.showMessageDialog(reportDialog,
                        "Error al imprimir: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            });
            
            btnClose.addActionListener(e -> reportDialog.dispose());
            
            buttonPanel.add(btnSaveHTML);
            buttonPanel.add(btnPrint);
            buttonPanel.add(btnClose);
            panel.add(buttonPanel, BorderLayout.SOUTH);
            
            reportDialog.add(panel);
            reportDialog.setVisible(true);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generando reporte del turno: " + e.getMessage(), e);
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, 
                "Error generando reporte del turno: " + e.getMessage(), e);
            msg.show(this);
        }
    }
    
}
