package com.openbravo.pos.reports;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.format.Formats;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel de Gráficos - Estilo Eleventa
 * Muestra gráficos de ventas por departamento, forma de pago, y periodos
 */
public class JPanelGraphics extends JPanel implements JPanelView {
    
    private static final Logger LOGGER = Logger.getLogger(JPanelGraphics.class.getName());
    
    private AppView m_App;
    private Session session;
    
    // Componentes UI
    private JTabbedPane tabbedPane;
    private ChartPanel chartPanelDepartment;
    private ChartPanel chartPanelPayment;
    private JTable tableDepartment;
    private JLabel lblTotalSales;
    private JLabel lblTotalProfit;
    
    // Datos actuales
    private java.util.Date dateStart;
    private java.util.Date dateEnd;
    private String activeCashIndex;
    
    // Clases de datos
    public static class DepartmentData {
        public String name;
        public double sales;
        public double profit;
        public int index;
        
        public DepartmentData(String name, double sales, double profit, int index) {
            this.name = name;
            this.sales = sales;
            this.profit = profit;
            this.index = index;
        }
    }
    
    public static class PaymentData {
        public String type;
        public String displayName;
        public double amount;
        
        public PaymentData(String type, String displayName, double amount) {
            this.type = type;
            this.displayName = displayName;
            this.amount = amount;
        }
    }
    
    public JPanelGraphics() {
        initComponents();
    }
    
    public void init(AppView app) {
        m_App = app;
        session = m_App.getSession();
        activeCashIndex = m_App.getActiveCashIndex();
        
        // Cargar datos iniciales después de que los componentes estén listos
        SwingUtilities.invokeLater(() -> {
            loadCurrentMonthData();
        });
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);
        
        // Panel superior con título
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JLabel titleLabel = new JLabel("Resumen de Ventas");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(60, 60, 60));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Crear un solo panel de gráficos compartido que se actualiza
        JPanel mainChartsPanel = createMainChartsPanel();
        
        // TabbedPane para diferentes periodos
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        // Todas las pestañas muestran el mismo panel (se actualiza al cambiar de pestaña)
        for (int i = 0; i < 5; i++) {
            tabbedPane.addTab(getTabName(i), mainChartsPanel);
        }
        
        // Listener para cargar datos al cambiar de pestaña
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            loadDataForPeriod(selectedIndex);
        });
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createMainChartsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel superior con resumen y gráficos circulares/barras
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        topPanel.setBackground(Color.WHITE);
        
        // Gráfico circular: Ganancia por Departamento
        chartPanelDepartment = new ChartPanel(createEmptyPieChart());
        chartPanelDepartment.setPreferredSize(new Dimension(400, 300));
        chartPanelDepartment.setMinimumSize(new Dimension(300, 250));
        chartPanelDepartment.setBackground(Color.WHITE);
        chartPanelDepartment.setDomainZoomable(false);
        chartPanelDepartment.setRangeZoomable(false);
        JPanel deptPanel = createChartPanel("Ganancia por Departamento", chartPanelDepartment);
        topPanel.add(deptPanel);
        
        // Gráfico de barras: Ventas por forma de pago
        chartPanelPayment = new ChartPanel(createEmptyBarChart());
        chartPanelPayment.setPreferredSize(new Dimension(400, 300));
        chartPanelPayment.setMinimumSize(new Dimension(300, 250));
        chartPanelPayment.setBackground(Color.WHITE);
        chartPanelPayment.setDomainZoomable(false);
        chartPanelPayment.setRangeZoomable(false);
        JPanel paymentPanel = createChartPanel("Ventas por forma de pago", chartPanelPayment);
        topPanel.add(paymentPanel);
        
        mainPanel.add(topPanel, BorderLayout.CENTER);
        
        // Panel inferior con tabla de ventas por departamento
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        JLabel tableTitle = new JLabel("Ventas por Departamento");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        tableTitle.setForeground(new Color(70, 130, 180));
        bottomPanel.add(tableTitle, BorderLayout.NORTH);
        
        // Tabla de departamentos
        tableDepartment = createDepartmentTable();
        JScrollPane scrollPane = new JScrollPane(tableDepartment);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        bottomPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Panel de resumen con totales
        JPanel summaryPanel = createSummaryPanel();
        bottomPanel.add(summaryPanel, BorderLayout.SOUTH);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    private JPanel createChartPanel(String title, ChartPanel chartPanel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(70, 130, 180));
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private JFreeChart createEmptyPieChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        JFreeChart chart = ChartFactory.createPieChart(null, dataset, true, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        return chart;
    }
    
    private JFreeChart createEmptyBarChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        JFreeChart chart = ChartFactory.createBarChart(null, "Forma de pago", "Ventas", 
            dataset, PlotOrientation.HORIZONTAL, false, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        return chart;
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void updatePieChart(List<DepartmentData> departments) {
        if (chartPanelDepartment == null) {
            LOGGER.warning("chartPanelDepartment es null, no se puede actualizar el gráfico");
            return;
        }
        
        if (departments == null || departments.isEmpty()) {
            LOGGER.info("No hay datos de departamentos para mostrar");
            // Mostrar gráfico vacío con mensaje
            DefaultPieDataset dataset = new DefaultPieDataset();
            dataset.setValue("Sin datos", 1.0);
            JFreeChart chart = ChartFactory.createPieChart(null, dataset, true, true, false);
            chart.setBackgroundPaint(Color.WHITE);
            chartPanelDepartment.setChart(chart);
            return;
        }
        
        DefaultPieDataset dataset = new DefaultPieDataset();
        
        // Ordenar por ganancia descendente
        departments.sort((a, b) -> Double.compare(b.profit, a.profit));
        
        // Agregar los primeros 6 departamentos más grandes
        int count = 0;
        double othersProfit = 0.0;
        Color[] colors = {
            new Color(102, 204, 204),  // Turquesa claro
            new Color(51, 153, 204),   // Azul medio
            new Color(0, 102, 153),    // Azul oscuro
            new Color(102, 178, 204),  // Azul claro
            new Color(153, 204, 204),  // Turquesa muy claro
            new Color(102, 153, 153),  // Verde azulado
            new Color(102, 178, 178)   // Turquesa medio
        };
        
        for (DepartmentData dept : departments) {
            if (count < 6 && dept.profit > 0) {
                String label = dept.index > 0 ? dept.index + "." + dept.name : dept.name;
                dataset.setValue(label, dept.profit);
                count++;
            } else if (dept.profit > 0) {
                othersProfit += dept.profit;
            }
        }
        
        if (othersProfit > 0) {
            dataset.setValue("Otros...", othersProfit);
        }
        
        if (dataset.getItemCount() == 0) {
            LOGGER.info("No hay datos con ganancia positiva para mostrar");
            dataset.setValue("Sin ganancias", 1.0);
        }
        
        JFreeChart chart = ChartFactory.createPieChart(null, dataset, true, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setLabelBackgroundPaint(new Color(240, 240, 240));
        plot.setLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        // Aplicar colores
        int colorIndex = 0;
        for (Object key : dataset.getKeys()) {
            if (colorIndex < colors.length) {
                plot.setSectionPaint((Comparable) key, colors[colorIndex]);
            }
            colorIndex++;
        }
        
        chartPanelDepartment.setChart(chart);
        chartPanelDepartment.repaint();
        LOGGER.info("Gráfico circular actualizado con " + dataset.getItemCount() + " elementos");
    }
    
    private void updateBarChart(List<PaymentData> payments) {
        if (chartPanelPayment == null) {
            LOGGER.warning("chartPanelPayment es null, no se puede actualizar el gráfico");
            return;
        }
        
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        if (payments == null || payments.isEmpty()) {
            LOGGER.info("No hay datos de pagos para mostrar");
            // Mostrar gráfico vacío
            JFreeChart chart = ChartFactory.createBarChart(null, "Forma de pago", "Ventas", 
                dataset, PlotOrientation.HORIZONTAL, false, true, false);
            chart.setBackgroundPaint(Color.WHITE);
            chartPanelPayment.setChart(chart);
            return;
        }
        
        // Ordenar por monto descendente
        payments.sort((a, b) -> Double.compare(b.amount, a.amount));
        
        for (PaymentData payment : payments) {
            if (payment.amount > 0) {
                dataset.addValue(payment.amount, payment.displayName, payment.displayName);
            }
        }
        
        if (dataset.getRowCount() == 0) {
            LOGGER.info("No hay pagos con monto positivo para mostrar");
        }
        
        JFreeChart chart = ChartFactory.createBarChart(null, "Forma de pago", "Ventas", 
            dataset, PlotOrientation.HORIZONTAL, false, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(new Color(200, 200, 200));
        if (dataset.getRowCount() > 0) {
            plot.getRenderer().setSeriesPaint(0, new Color(70, 130, 180));
        }
        
        chartPanelPayment.setChart(chart);
        chartPanelPayment.repaint();
        LOGGER.info("Gráfico de barras actualizado con " + dataset.getRowCount() + " formas de pago");
    }
    
    private JTable createDepartmentTable() {
        String[] columns = {"Departamento", "Monto"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(30);
        table.setShowGrid(true);
        table.setGridColor(new Color(230, 230, 230));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(70, 130, 180));
        table.getTableHeader().setForeground(Color.WHITE);
        
        return table;
    }
    
    private void updateDepartmentTable(List<DepartmentData> departments) {
        DefaultTableModel model = (DefaultTableModel) tableDepartment.getModel();
        model.setRowCount(0);
        
        // Ordenar por ventas descendente
        departments.sort((a, b) -> Double.compare(b.sales, a.sales));
        
        for (DepartmentData dept : departments) {
            String label = dept.index > 0 ? dept.index + "." + dept.name : dept.name;
            String amount = Formats.CURRENCY.formatValue(dept.sales);
            model.addRow(new Object[]{label, amount});
        }
    }
    
    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        
        // Panel de Ventas
        JPanel salesPanel = new JPanel(new BorderLayout());
        salesPanel.setBackground(new Color(230, 240, 250));
        salesPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel salesLabel = new JLabel("Ventas");
        salesLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        salesLabel.setForeground(new Color(70, 130, 180));
        salesPanel.add(salesLabel, BorderLayout.NORTH);
        
        lblTotalSales = new JLabel("$0.00");
        lblTotalSales.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTotalSales.setForeground(new Color(50, 50, 50));
        lblTotalSales.setHorizontalAlignment(SwingConstants.CENTER);
        salesPanel.add(lblTotalSales, BorderLayout.CENTER);
        
        // Panel de Ganancia
        JPanel profitPanel = new JPanel(new BorderLayout());
        profitPanel.setBackground(new Color(230, 250, 240));
        profitPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(76, 175, 80), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel profitLabel = new JLabel("Ganancia");
        profitLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        profitLabel.setForeground(new Color(76, 175, 80));
        profitPanel.add(profitLabel, BorderLayout.NORTH);
        
        lblTotalProfit = new JLabel("$0.00");
        lblTotalProfit.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTotalProfit.setForeground(new Color(50, 50, 50));
        lblTotalProfit.setHorizontalAlignment(SwingConstants.CENTER);
        profitPanel.add(lblTotalProfit, BorderLayout.CENTER);
        
        panel.add(salesPanel);
        panel.add(profitPanel);
        
        return panel;
    }
    
    private String getTabName(int index) {
        switch (index) {
            case 0: return "Semana Actual";
            case 1: return "Mes Actual";
            case 2: return "Mes Anterior";
            case 3: return "Año actual";
            case 4: return "Periodo...";
            default: return "Periodo";
        }
    }
    
    private void loadCurrentMonthData() {
        loadDataForPeriod(1); // Mes Actual
    }
    
    private void loadDataForPeriod(int periodIndex) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Obtener fecha de inicio de la caja activa
                java.util.Date cashStartDate = getCashStartDate();
                
                Calendar cal = Calendar.getInstance();
                java.util.Date endDate = new java.util.Date();
                
                switch (periodIndex) {
                    case 0: // Semana Actual - desde inicio de caja o última semana
                        if (cashStartDate != null && cashStartDate.after(getWeekStart())) {
                            dateStart = cashStartDate;
                        } else {
                            dateStart = getWeekStart();
                        }
                        dateEnd = endDate;
                        break;
                    case 1: // Mes Actual - TODAS las ventas de la caja activa (sin filtro de fecha)
                        // Para ver TODAS las ventas, no usar filtro de fecha
                        dateStart = cashStartDate; // Solo desde que se abrió la caja
                        dateEnd = endDate;
                        break;
                    case 2: // Mes Anterior
                        cal.add(Calendar.MONTH, -1);
                        cal.set(Calendar.DAY_OF_MONTH, 1);
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        dateStart = cal.getTime();
                        cal.add(Calendar.MONTH, 1);
                        cal.add(Calendar.DAY_OF_MONTH, -1);
                        cal.set(Calendar.HOUR_OF_DAY, 23);
                        cal.set(Calendar.MINUTE, 59);
                        cal.set(Calendar.SECOND, 59);
                        dateEnd = cal.getTime();
                        break;
                    case 3: // Año actual - desde inicio de caja o inicio del año
                        if (cashStartDate != null && cashStartDate.after(getYearStart())) {
                            dateStart = cashStartDate;
                        } else {
                            dateStart = getYearStart();
                        }
                        dateEnd = endDate;
                        break;
                    case 4: // Periodo personalizado
                        showCustomPeriodDialog();
                        return;
                }
                
                LOGGER.info("Periodo seleccionado: " + periodIndex + ", Fecha inicio: " + dateStart + ", Fecha fin: " + dateEnd);
                loadDataFromDatabase();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error cargando datos del periodo", e);
                JOptionPane.showMessageDialog(this, 
                    "Error al cargar los datos: " + e.getMessage(),
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    private java.util.Date getCashStartDate() {
        try {
            if (session == null || activeCashIndex == null) {
                return null;
            }
            
            PreparedStatement stmt = session.getConnection().prepareStatement(
                "SELECT DATESTART FROM closedcash WHERE MONEY = ? AND DATEEND IS NULL");
            stmt.setString(1, activeCashIndex);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("DATESTART");
                rs.close();
                stmt.close();
                if (ts != null) {
                    LOGGER.info("Fecha de inicio de caja: " + ts);
                    return new java.util.Date(ts.getTime());
                }
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error obteniendo fecha de inicio de caja", e);
        }
        return null;
    }
    
    private java.util.Date getWeekStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }
    
    private java.util.Date getMonthStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }
    
    private java.util.Date getYearStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }
    
    private void showCustomPeriodDialog() {
        // TODO: Implementar selector de fechas
        LOGGER.info("Mostrando diálogo de periodo personalizado");
        loadDataForPeriod(1); // Por ahora carga mes actual
    }
    
    private void loadDataFromDatabase() throws BasicException {
        if (session == null || activeCashIndex == null) {
            LOGGER.warning("Session o activeCashIndex es null. Session: " + (session != null) + ", CashIndex: " + activeCashIndex);
            return;
        }
        
        LOGGER.info("Cargando datos desde base de datos. CashIndex: " + activeCashIndex);
        
        try {
            // 1. Obtener ventas por departamento con ganancias
            List<DepartmentData> departments = loadDepartmentData();
            LOGGER.info("Departamentos cargados: " + (departments != null ? departments.size() : 0));
            
            // 2. Obtener ventas por forma de pago
            List<PaymentData> payments = loadPaymentData();
            LOGGER.info("Formas de pago cargadas: " + (payments != null ? payments.size() : 0));
            
            // 3. Calcular totales
            final List<DepartmentData> finalDepartments = departments != null ? departments : new ArrayList<>();
            final List<PaymentData> finalPayments = payments != null ? payments : new ArrayList<>();
            
            double totalSalesCalc = 0.0;
            double totalProfitCalc = 0.0;
            for (DepartmentData dept : finalDepartments) {
                totalSalesCalc += dept.sales;
                totalProfitCalc += dept.profit;
            }
            final double totalSales = totalSalesCalc;
            final double totalProfit = totalProfitCalc;
            LOGGER.info("Totales calculados - Ventas: " + totalSales + ", Ganancia: " + totalProfit);
            
            // 4. Actualizar UI en el hilo de eventos
            SwingUtilities.invokeLater(() -> {
                updatePieChart(finalDepartments);
                updateBarChart(finalPayments);
                updateDepartmentTable(finalDepartments);
                if (lblTotalSales != null) {
                    lblTotalSales.setText(Formats.CURRENCY.formatValue(totalSales));
                }
                if (lblTotalProfit != null) {
                    lblTotalProfit.setText(Formats.CURRENCY.formatValue(totalProfit));
                }
                // Forzar repaint
                if (chartPanelDepartment != null) {
                    chartPanelDepartment.repaint();
                }
                if (chartPanelPayment != null) {
                    chartPanelPayment.repaint();
                }
                repaint();
            });
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error SQL cargando datos", e);
            throw new BasicException(e);
        }
    }
    
    private List<DepartmentData> loadDepartmentData() throws SQLException {
        List<DepartmentData> departments = new ArrayList<>();
        
        try {
            // Construir la consulta SQL dinámicamente
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT COALESCE(categories.NAME, 'Sin Departamento') as CATEGORY_NAME, ");
            sqlBuilder.append("COALESCE(categories.ID, '') as CATEGORY_ID, ");
            sqlBuilder.append("SUM((ticketlines.PRICE + ticketlines.PRICE * taxes.RATE) * ticketlines.UNITS) as TOTAL_SALES, ");
            sqlBuilder.append("SUM((ticketlines.PRICE - COALESCE(products.PRICEBUY, 0)) * ticketlines.UNITS) as TOTAL_PROFIT ");
            sqlBuilder.append("FROM ticketlines ");
            sqlBuilder.append("INNER JOIN tickets ON ticketlines.TICKET = tickets.ID ");
            sqlBuilder.append("INNER JOIN receipts ON tickets.ID = receipts.ID ");
            sqlBuilder.append("INNER JOIN products ON ticketlines.PRODUCT = products.ID ");
            sqlBuilder.append("LEFT JOIN categories ON products.CATEGORY = categories.ID ");
            sqlBuilder.append("INNER JOIN taxes ON ticketlines.TAXID = taxes.ID ");
            sqlBuilder.append("WHERE receipts.MONEY = ? ");
            
            if (dateStart != null) {
                sqlBuilder.append("AND receipts.DATENEW >= ? ");
            }
            if (dateEnd != null) {
                sqlBuilder.append("AND receipts.DATENEW <= ? ");
            }
            
            sqlBuilder.append("GROUP BY COALESCE(categories.NAME, 'Sin Departamento'), COALESCE(categories.ID, '') ");
            sqlBuilder.append("ORDER BY TOTAL_PROFIT DESC");
            
            String sql = sqlBuilder.toString();
            LOGGER.info("Ejecutando consulta de departamentos");
            LOGGER.info("Parámetros: activeCashIndex=" + activeCashIndex);
            if (dateStart != null) {
                LOGGER.info("  - Filtro fecha inicio: " + dateStart);
            } else {
                LOGGER.info("  - Sin filtro de fecha inicio (todas las ventas de la caja)");
            }
            if (dateEnd != null) {
                LOGGER.info("  - Filtro fecha fin: " + dateEnd);
            } else {
                LOGGER.info("  - Sin filtro de fecha fin (hasta ahora)");
            }
            
            PreparedStatement stmt = session.getConnection().prepareStatement(sql);
            int paramIndex = 1;
            stmt.setString(paramIndex++, activeCashIndex);
            if (dateStart != null) {
                Timestamp tsStart = new Timestamp(dateStart.getTime());
                stmt.setTimestamp(paramIndex++, tsStart);
                LOGGER.info("  - Parámetro fecha inicio establecido: " + tsStart);
            }
            if (dateEnd != null) {
                Timestamp tsEnd = new Timestamp(dateEnd.getTime());
                stmt.setTimestamp(paramIndex++, tsEnd);
                LOGGER.info("  - Parámetro fecha fin establecido: " + tsEnd);
            }
            
            ResultSet rs = stmt.executeQuery();
            int index = 1;
            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                String name = rs.getString("CATEGORY_NAME");
                double sales = rs.getDouble("TOTAL_SALES");
                double profit = rs.getDouble("TOTAL_PROFIT");
                
                LOGGER.info("Fila " + rowCount + ": " + name + " - Ventas: " + sales + ", Ganancia: " + profit);
                
                // Solo agregar si hay ventas
                if (sales > 0 || profit > 0) {
                    // Intentar obtener el índice de la categoría si existe
                    String catId = rs.getString("CATEGORY_ID");
                    int deptIndex = index;
                    try {
                        if (catId != null && !catId.isEmpty()) {
                            PreparedStatement catStmt = session.getConnection().prepareStatement(
                                "SELECT ORDERNUM FROM categories WHERE ID = ?");
                            catStmt.setString(1, catId);
                            ResultSet catRs = catStmt.executeQuery();
                            if (catRs.next()) {
                                int orderNum = catRs.getInt("ORDERNUM");
                                if (orderNum > 0) {
                                    deptIndex = orderNum;
                                }
                            }
                            catRs.close();
                            catStmt.close();
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.FINE, "No se pudo obtener ORDERNUM para categoría " + catId, e);
                    }
                    
                    departments.add(new DepartmentData(name, sales, profit, deptIndex));
                    index++;
                }
            }
            rs.close();
            stmt.close();
            
            LOGGER.info("Total filas procesadas: " + rowCount + ", Departamentos agregados: " + departments.size());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error SQL cargando departamentos: " + e.getMessage(), e);
            e.printStackTrace();
            throw e;
        }
        
        return departments;
    }
    
    private List<PaymentData> loadPaymentData() throws SQLException, BasicException {
        List<PaymentData> payments = new ArrayList<>();
        
        try {
            Map<String, String> paymentNames = new HashMap<>();
            paymentNames.put("cash", "Efectivo");
            paymentNames.put("card", "Tarjeta");
            paymentNames.put("magcard", "Tarjeta");
            paymentNames.put("debt", "Crédito");
            paymentNames.put("voucher", "Vales");
            paymentNames.put("transfer", "Transferencia");
            paymentNames.put("cheque", "Cheque");
            
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT payments.PAYMENT, SUM(payments.TOTAL) as TOTAL ");
            sqlBuilder.append("FROM payments ");
            sqlBuilder.append("INNER JOIN receipts ON payments.RECEIPT = receipts.ID ");
            sqlBuilder.append("WHERE receipts.MONEY = ? ");
            if (dateStart != null) {
                sqlBuilder.append("AND receipts.DATENEW >= ? ");
            }
            if (dateEnd != null) {
                sqlBuilder.append("AND receipts.DATENEW <= ? ");
            }
            sqlBuilder.append("GROUP BY payments.PAYMENT");
            
            String sql = sqlBuilder.toString();
            LOGGER.info("Ejecutando consulta de formas de pago: " + sql);
            
            PreparedStatement stmt = session.getConnection().prepareStatement(sql);
            int paramIndex = 1;
            stmt.setString(paramIndex++, activeCashIndex);
            if (dateStart != null) {
                stmt.setTimestamp(paramIndex++, new Timestamp(dateStart.getTime()));
            }
            if (dateEnd != null) {
                stmt.setTimestamp(paramIndex++, new Timestamp(dateEnd.getTime()));
            }
            
            ResultSet rs = stmt.executeQuery();
            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                String type = rs.getString("PAYMENT");
                double amount = Math.abs(rs.getDouble("TOTAL")); // Usar valor absoluto
                LOGGER.info("Forma de pago encontrada: " + type + " = " + amount);
                if (amount > 0) {
                    String displayName = paymentNames.getOrDefault(type, type);
                    payments.add(new PaymentData(type, displayName, amount));
                }
            }
            rs.close();
            stmt.close();
            
            LOGGER.info("Total formas de pago encontradas: " + rowCount + ", Agregadas: " + payments.size());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error SQL cargando formas de pago: " + e.getMessage(), e);
            e.printStackTrace();
            throw e;
        }
        
        return payments;
    }
    
    @Override
    public JComponent getComponent() {
        return this;
    }
    
    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.Graphics");
    }
    
    @Override
    public void activate() throws BasicException {
        // Asegurar que session y activeCashIndex estén disponibles
        if (session == null && m_App != null) {
            session = m_App.getSession();
        }
        if (activeCashIndex == null && m_App != null) {
            activeCashIndex = m_App.getActiveCashIndex();
        }
        
        LOGGER.info("Panel de gráficos activado. CashIndex: " + activeCashIndex);
        
        // Recargar datos del periodo actual
        int currentTab = tabbedPane != null ? tabbedPane.getSelectedIndex() : 1;
        loadDataForPeriod(currentTab);
    }
    
    @Override
    public boolean deactivate() {
        return true;
    }
}
