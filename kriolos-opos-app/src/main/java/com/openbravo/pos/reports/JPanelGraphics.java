package com.openbravo.pos.reports;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
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
public class JPanelGraphics extends JPanel implements JPanelView, BeanFactoryApp {
    
    private static final Logger LOGGER = Logger.getLogger(JPanelGraphics.class.getName());
    
    private AppView m_App;
    private Session session;
    
    // Componentes UI
    private ChartPanel chartPanelSalesProfit; // Gráfico principal de Ventas y Ganancias
    private ChartPanel chartPanelDepartment;
    private ChartPanel chartPanelPayment; // Gráfico de barras: Ventas por forma de pago
    private JTable tableDepartment;
    private JTable tableDepartmentProfit; // Tabla de ganancias por departamento
    private JTable tableSalesByMonth;
    private JLabel lblTotalSales;
    
    // Labels para métricas de ventas
    private JLabel lblSalesTotal;
    private JLabel lblTotalProfit;
    private JLabel lblNumberOfSales;
    private JLabel lblAverageSale;
    private JLabel lblProfitMargin;
    
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
    
    @Override
    public void init(AppView app) throws BeanFactoryException {
        m_App = app;
        session = m_App.getSession();
        activeCashIndex = m_App.getActiveCashIndex();
        
        LOGGER.info("JPanelGraphics.init() llamado. CashIndex: " + activeCashIndex);
        
        // Los datos se cargarán cuando se active el panel
    }
    
    @Override
    public Object getBean() {
        return this;
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);
        
        // Panel superior con título y botones de periodo
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("Resumen de Ventas");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(60, 60, 60));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        // Panel de botones para seleccionar periodo (Día, Semana, Mes, Año)
        JPanel periodButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        periodButtonsPanel.setBackground(Color.WHITE);
        periodButtonsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Crear botones para cada periodo
        JButton[] periodButtons = new JButton[4];
        String[] periodNames = {"Día Actual", "Semana Actual", "Mes Actual", "Año Actual"};
        
        for (int i = 0; i < 4; i++) {
            final int periodIndex = i;
            periodButtons[i] = new JButton(periodNames[i]);
            periodButtons[i].setFont(new Font("Segoe UI", Font.BOLD, 12));
            periodButtons[i].setPreferredSize(new Dimension(120, 35));
            periodButtons[i].setBackground(new Color(70, 130, 180));
            periodButtons[i].setForeground(Color.WHITE);
            periodButtons[i].setFocusPainted(false);
            periodButtons[i].setBorderPainted(true);
            
            // Estilo del botón seleccionado
            if (i == 0) {
                periodButtons[i].setBackground(new Color(50, 100, 150));
            }
            
            periodButtons[i].addActionListener(e -> {
                // Resetear todos los botones
                for (JButton btn : periodButtons) {
                    btn.setBackground(new Color(70, 130, 180));
                }
                // Resaltar el botón seleccionado
                periodButtons[periodIndex].setBackground(new Color(50, 100, 150));
                
                // Cargar datos del periodo seleccionado
                loadDataForPeriod(periodIndex);
            });
            
            periodButtonsPanel.add(periodButtons[i]);
        }
        
        headerPanel.add(periodButtonsPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);
        
        // Crear el panel de gráficos principal con scroll
        JPanel mainChartsPanel = createMainChartsPanel();
        JScrollPane scrollPane = new JScrollPane(mainChartsPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private JPanel createMainChartsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel superior con gráfico de ventas/ganancias a la izquierda y otros gráficos
        JPanel topPanel = new JPanel(new BorderLayout(15, 0));
        topPanel.setBackground(Color.WHITE);
        
        // Panel superior: Solo gráfico principal de Ventas y Ganancias
        JPanel topHorizontalPanel = new JPanel(new BorderLayout());
        topHorizontalPanel.setBackground(Color.WHITE);
        
        // GRÁFICO PRINCIPAL: Ventas y Ganancias
        chartPanelSalesProfit = new ChartPanel(createEmptySalesProfitBarChart());
        chartPanelSalesProfit.setPreferredSize(new Dimension(550, 280));
        chartPanelSalesProfit.setMinimumSize(new Dimension(500, 260));
        chartPanelSalesProfit.setMaximumSize(new Dimension(600, 300));
        chartPanelSalesProfit.setBackground(Color.WHITE);
        chartPanelSalesProfit.setDomainZoomable(false);
        chartPanelSalesProfit.setRangeZoomable(false);
        chartPanelSalesProfit.setMouseWheelEnabled(false);
        JPanel salesProfitPanel = createChartPanel("", chartPanelSalesProfit); // Sin título
        salesProfitPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        topHorizontalPanel.add(salesProfitPanel, BorderLayout.CENTER);
        
        // Panel central con gráfico principal y métricas
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(topHorizontalPanel, BorderLayout.NORTH);
        
        // Panel de métricas debajo de los gráficos
        JPanel metricsPanel = createSalesMetricsPanel();
        centerPanel.add(metricsPanel, BorderLayout.CENTER);
        
        mainPanel.add(centerPanel, BorderLayout.NORTH);
        
        // Separador visual
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(200, 200, 200));
        separator.setPreferredSize(new Dimension(0, 2));
        mainPanel.add(separator, BorderLayout.CENTER);
        
        // Panel inferior con dos columnas: Ventas por Mes | Ventas por Departamento + Ganancias
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Tabla izquierda: Ventas por Mes (con gráfico de torta)
        JPanel monthTablePanel = new JPanel(new BorderLayout(10, 10));
        monthTablePanel.setBackground(Color.WHITE);
        
        JLabel monthTableTitle = new JLabel("Ventas por mes");
        monthTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        monthTableTitle.setForeground(new Color(70, 130, 180));
        monthTablePanel.add(monthTableTitle, BorderLayout.NORTH);
        
        // Panel que contiene tabla y gráfico lado a lado
        JPanel monthContentPanel = new JPanel(new BorderLayout(10, 0));
        monthContentPanel.setBackground(Color.WHITE);
        
        // Tabla a la izquierda
        tableSalesByMonth = createSalesByMonthTable();
        JScrollPane monthScrollPane = new JScrollPane(tableSalesByMonth);
        monthScrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        monthContentPanel.add(monthScrollPane, BorderLayout.CENTER);
        
        // Gráfico circular (Torta) pequeño a la derecha de la tabla - sin fondo, solo la rueda
        chartPanelDepartment = new ChartPanel(createEmptyPieChart());
        chartPanelDepartment.setPreferredSize(new Dimension(180, 180));
        chartPanelDepartment.setMinimumSize(new Dimension(160, 160));
        chartPanelDepartment.setMaximumSize(new Dimension(200, 200));
        chartPanelDepartment.setOpaque(false); // Sin fondo
        chartPanelDepartment.setBackground(new Color(0, 0, 0, 0)); // Fondo transparente
        chartPanelDepartment.setDomainZoomable(false);
        chartPanelDepartment.setRangeZoomable(false);
        chartPanelDepartment.setMouseWheelEnabled(false);
        chartPanelDepartment.setBorder(null); // Sin borde
        
        // Panel para el gráfico sin bordes ni fondo
        JPanel chartWrapper = new JPanel(new BorderLayout());
        chartWrapper.setOpaque(false);
        chartWrapper.setBorder(null);
        chartWrapper.add(chartPanelDepartment, BorderLayout.CENTER);
        monthContentPanel.add(chartWrapper, BorderLayout.EAST);
        
        monthTablePanel.add(monthContentPanel, BorderLayout.CENTER);
        
        // Gráfico de barras: Ventas por forma de pago (debajo de la tabla de meses)
        chartPanelPayment = new ChartPanel(createEmptyBarChart());
        chartPanelPayment.setPreferredSize(new Dimension(0, 200));
        chartPanelPayment.setMinimumSize(new Dimension(0, 180));
        chartPanelPayment.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        chartPanelPayment.setBackground(Color.WHITE);
        chartPanelPayment.setDomainZoomable(false);
        chartPanelPayment.setRangeZoomable(false);
        chartPanelPayment.setMouseWheelEnabled(false);
        JPanel paymentChartPanel = new JPanel(new BorderLayout());
        paymentChartPanel.setBackground(Color.WHITE);
        paymentChartPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        paymentChartPanel.add(chartPanelPayment, BorderLayout.CENTER);
        monthTablePanel.add(paymentChartPanel, BorderLayout.SOUTH);
        
        // Panel derecho: Ventas por Departamento + Ganancias por Departamento
        JPanel deptMainPanel = new JPanel(new BorderLayout(0, 10));
        deptMainPanel.setBackground(Color.WHITE);
        
        // Tabla superior: Ventas por Departamento
        JPanel deptTablePanel = new JPanel(new BorderLayout(10, 10));
        deptTablePanel.setBackground(Color.WHITE);
        
        JLabel deptTableTitle = new JLabel("Ventas por Departamento");
        deptTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        deptTableTitle.setForeground(new Color(70, 130, 180));
        deptTablePanel.add(deptTableTitle, BorderLayout.NORTH);
        
        tableDepartment = createDepartmentTable();
        JScrollPane deptScrollPane = new JScrollPane(tableDepartment);
        deptScrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        deptTablePanel.add(deptScrollPane, BorderLayout.CENTER);
        
        // Tabla inferior: Ganancias por Departamento
        JPanel deptProfitTablePanel = new JPanel(new BorderLayout(10, 10));
        deptProfitTablePanel.setBackground(Color.WHITE);
        
        JLabel deptProfitTableTitle = new JLabel("Ganancias por Departamento");
        deptProfitTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        deptProfitTableTitle.setForeground(new Color(70, 130, 180));
        deptProfitTablePanel.add(deptProfitTableTitle, BorderLayout.NORTH);
        
        tableDepartmentProfit = createDepartmentProfitTable();
        JScrollPane deptProfitScrollPane = new JScrollPane(tableDepartmentProfit);
        deptProfitScrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        deptProfitTablePanel.add(deptProfitScrollPane, BorderLayout.CENTER);
        
        deptMainPanel.add(deptTablePanel, BorderLayout.NORTH);
        deptMainPanel.add(deptProfitTablePanel, BorderLayout.CENTER);
        
        bottomPanel.add(monthTablePanel);
        bottomPanel.add(deptMainPanel);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    private JPanel createChartPanel(String title, ChartPanel chartPanel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(50, 100, 150));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private JFreeChart createEmptyPieChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        // No agregar ningún valor, dejar el dataset vacío
        JFreeChart chart = ChartFactory.createPieChart(null, dataset, false, false, false); // Sin leyenda, sin tooltips, sin URLs
        chart.setBackgroundPaint(new Color(0, 0, 0, 0)); // Fondo transparente
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(new Color(0, 0, 0, 0)); // Fondo transparente
        plot.setOutlineVisible(false); // Sin borde exterior
        plot.setLabelGenerator(null); // Sin etiquetas
        plot.setShadowPaint(new Color(0, 0, 0, 0)); // Sin sombra
        plot.setInteriorGap(0.05); // Pequeño espacio interior
        return chart;
    }
    
    private JFreeChart createEmptyBarChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        // Agregar un valor temporal para que el gráfico se vea
        dataset.addValue(1.0, "Esperando datos...", "Esperando datos...");
        JFreeChart chart = ChartFactory.createBarChart(null, "Forma de pago", "Ventas ($)", 
            dataset, PlotOrientation.HORIZONTAL, false, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.getDomainAxis().setLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
        plot.getRangeAxis().setLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
        return chart;
    }
    
    private JFreeChart createEmptySalesProfitBarChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        // Agregar valores temporales para que el gráfico se vea
        dataset.addValue(0.0, "Ventas", "Esperando datos...");
        dataset.addValue(0.0, "Ganancia", "Esperando datos...");
        JFreeChart chart = ChartFactory.createBarChart(null, "", "Ventas", 
            dataset, PlotOrientation.VERTICAL, true, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.getDomainAxis().setLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
        plot.getRangeAxis().setLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
        plot.getDomainAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
        plot.getRangeAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
        return chart;
    }
    
    private void updateSalesProfitBarChart(double totalSales, double totalProfit) {
        if (chartPanelSalesProfit == null) {
            LOGGER.warning("chartPanelSalesProfit es null, no se puede actualizar el gráfico");
            return;
        }
        
        try {
            // Cargar datos agrupados por mes
            List<MonthData> monthData = loadSalesProfitByMonth();
            
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            // Si hay datos por mes, mostrarlos; si no, mostrar totales
            if (monthData != null && !monthData.isEmpty()) {
                for (MonthData month : monthData) {
                    dataset.addValue(month.sales, "Ventas", month.monthName);
                    dataset.addValue(month.profit, "Ganancia", month.monthName);
                }
            } else {
                // Fallback: mostrar totales
                dataset.addValue(totalSales, "Ventas", "Total");
                dataset.addValue(totalProfit, "Ganancia", "Total");
            }
            
            JFreeChart chart = ChartFactory.createBarChart(null, "", "Ventas", 
                dataset, PlotOrientation.VERTICAL, true, true, false);
            chart.setBackgroundPaint(Color.WHITE);
            
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setOutlineVisible(false);
            plot.setRangeGridlinePaint(new Color(200, 200, 200));
            
            // Tamaño de fuente más pequeño para gráfico compacto
            plot.getDomainAxis().setLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
            plot.getRangeAxis().setLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
            plot.getDomainAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
            plot.getRangeAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
            
            // Colores similares a la imagen: Azul más oscuro para Ventas, Azul claro para Ganancia
            plot.getRenderer().setSeriesPaint(0, new Color(70, 130, 180));   // Azul para Ventas
            plot.getRenderer().setSeriesPaint(1, new Color(173, 216, 230));  // Azul claro para Ganancia
            
            chartPanelSalesProfit.setChart(chart);
            chartPanelSalesProfit.repaint();
            LOGGER.info("Gráfico de Ventas y Ganancias actualizado - Ventas: " + totalSales + ", Ganancias: " + totalProfit);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error actualizando gráfico de ventas y ganancias", e);
        }
    }
    
    // Clase para almacenar datos por mes
    private static class MonthData {
        public String monthName;
        public double sales;
        public double profit;
        
        public MonthData(String monthName, double sales, double profit) {
            this.monthName = monthName;
            this.sales = sales;
            this.profit = profit;
        }
    }
    
    private List<MonthData> loadSalesProfitByMonth() throws SQLException {
        List<MonthData> monthData = new ArrayList<>();
        
        if (session == null || activeCashIndex == null) {
            return monthData;
        }
        
        try {
            // Consulta SQL para agrupar por mes (compatible con HSQLDB)
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT ");
            sqlBuilder.append("MONTH(receipts.DATENEW) as MONTH_NUM, ");
            sqlBuilder.append("SUM((ticketlines.PRICE + ticketlines.PRICE * taxes.RATE) * ticketlines.UNITS) as TOTAL_SALES, ");
            sqlBuilder.append("SUM((ticketlines.PRICE - COALESCE(products.PRICEBUY, 0)) * ticketlines.UNITS) as TOTAL_PROFIT ");
            sqlBuilder.append("FROM ticketlines ");
            sqlBuilder.append("INNER JOIN tickets ON ticketlines.TICKET = tickets.ID ");
            sqlBuilder.append("INNER JOIN receipts ON tickets.ID = receipts.ID ");
            sqlBuilder.append("INNER JOIN products ON ticketlines.PRODUCT = products.ID ");
            sqlBuilder.append("INNER JOIN taxes ON ticketlines.TAXID = taxes.ID ");
            sqlBuilder.append("WHERE receipts.MONEY = ? ");
            
            if (dateStart != null) {
                sqlBuilder.append("AND receipts.DATENEW >= ? ");
            }
            if (dateEnd != null) {
                sqlBuilder.append("AND receipts.DATENEW <= ? ");
            }
            
            sqlBuilder.append("GROUP BY MONTH(receipts.DATENEW) ");
            sqlBuilder.append("ORDER BY MONTH(receipts.DATENEW)");
            
            PreparedStatement stmt = session.getConnection().prepareStatement(sqlBuilder.toString());
            int paramIndex = 1;
            stmt.setString(paramIndex++, activeCashIndex);
            if (dateStart != null) {
                stmt.setTimestamp(paramIndex++, new Timestamp(dateStart.getTime()));
            }
            if (dateEnd != null) {
                stmt.setTimestamp(paramIndex++, new Timestamp(dateEnd.getTime()));
            }
            
            ResultSet rs = stmt.executeQuery();
            String[] monthNames = {"", "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
            while (rs.next()) {
                int monthNum = rs.getInt("MONTH_NUM");
                double sales = rs.getDouble("TOTAL_SALES");
                double profit = rs.getDouble("TOTAL_PROFIT");
                
                if (sales > 0 || profit > 0) {
                    String monthName = (monthNum >= 1 && monthNum <= 12) ? monthNames[monthNum] : "N/A";
                    monthData.add(new MonthData(monthName, sales, profit));
                }
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error cargando datos por mes", e);
        }
        
        return monthData;
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void updatePieChart(List<DepartmentData> departments) {
        if (chartPanelDepartment == null) {
            LOGGER.warning("chartPanelDepartment es null, no se puede actualizar el gráfico");
            return;
        }
        
        if (departments == null || departments.isEmpty()) {
            LOGGER.info("No hay datos de departamentos para mostrar");
            // Ocultar el gráfico completamente cuando no hay datos
            chartPanelDepartment.setVisible(false);
            return;
        }
        
        DefaultPieDataset dataset = new DefaultPieDataset();
        
        // Ordenar por ganancia descendente
        departments.sort((a, b) -> Double.compare(b.profit, a.profit));
        
        // Agregar los primeros 6 departamentos más grandes
        int count = 0;
        double othersProfit = 0.0;
        Color[] colors = {
            new Color(70, 130, 180),   // Azul acero
            new Color(60, 179, 113),   // Verde mar
            new Color(255, 140, 0),    // Naranja oscuro
            new Color(220, 20, 60),    // Rojo carmesí (pero solo si hay múltiples segmentos)
            new Color(138, 43, 226),   // Violeta azul
            new Color(255, 215, 0),    // Oro
            new Color(102, 204, 204),  // Turquesa claro
            new Color(51, 153, 204)    // Azul medio
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
            // Ocultar el gráfico completamente cuando no hay datos
            chartPanelDepartment.setVisible(false);
            return;
        }
        
        // Mostrar el gráfico cuando hay datos
        chartPanelDepartment.setVisible(true);
        
        JFreeChart chart = ChartFactory.createPieChart(null, dataset, false, false, false); // Sin leyenda, sin tooltips, sin URLs
        chart.setBackgroundPaint(new Color(0, 0, 0, 0)); // Fondo transparente
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(new Color(0, 0, 0, 0)); // Fondo transparente
        plot.setOutlineVisible(false); // Sin borde exterior
        plot.setLabelGenerator(null); // Sin etiquetas
        plot.setShadowPaint(new Color(0, 0, 0, 0)); // Sin sombra
        plot.setInteriorGap(0.05); // Pequeño espacio interior para mejor visualización
        
        // Aplicar colores variados
        int colorIndex = 0;
        for (Object key : dataset.getKeys()) {
            if (colorIndex < colors.length) {
                plot.setSectionPaint((Comparable) key, colors[colorIndex]);
            } else {
                // Si hay más elementos que colores, generar colores alternativos
                float hue = (colorIndex * 0.1f) % 1.0f;
                Color newColor = Color.getHSBColor(hue, 0.6f, 0.9f);
                plot.setSectionPaint((Comparable) key, newColor);
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
        
        JFreeChart chart = ChartFactory.createBarChart(null, "Forma de pago", "Ventas ($)", 
            dataset, PlotOrientation.HORIZONTAL, false, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(new Color(200, 200, 200));
        
        // Mejorar la apariencia del gráfico
        plot.getDomainAxis().setLabelFont(new Font("Segoe UI", Font.BOLD, 13));
        plot.getRangeAxis().setLabelFont(new Font("Segoe UI", Font.BOLD, 13));
        plot.getDomainAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
        plot.getRangeAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        if (dataset.getRowCount() > 0) {
            // Colores para las barras
            Color[] barColors = {
                new Color(70, 130, 180),   // Azul acero
                new Color(60, 179, 113),   // Verde mar
                new Color(255, 140, 0),    // Naranja oscuro
                new Color(220, 20, 60),    // Rojo carmesí
                new Color(138, 43, 226),   // Violeta azul
                new Color(255, 215, 0)     // Oro
            };
            int colorIndex = 0;
            for (int i = 0; i < dataset.getRowCount(); i++) {
                if (colorIndex < barColors.length) {
                    plot.getRenderer().setSeriesPaint(i, barColors[colorIndex % barColors.length]);
                }
                colorIndex++;
            }
        }
        
        chartPanelPayment.setChart(chart);
        chartPanelPayment.repaint();
        LOGGER.info("Gráfico de barras actualizado con " + dataset.getRowCount() + " formas de pago");
    }
    
    private JTable createSalesByMonthTable() {
        String[] columns = {"Mes", "Monto"};
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
    
    private void updateSalesByMonthTable(List<MonthData> monthData) {
        DefaultTableModel model = (DefaultTableModel) tableSalesByMonth.getModel();
        model.setRowCount(0);
        
        if (monthData != null && !monthData.isEmpty()) {
            for (MonthData month : monthData) {
                String monthName = month.monthName;
                String amount = Formats.CURRENCY.formatValue(month.sales);
                model.addRow(new Object[]{monthName, amount});
            }
        }
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
    
    private JTable createDepartmentProfitTable() {
        String[] columns = {"Departamento", "Ganancia"};
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
    
    private void updateDepartmentProfitTable(List<DepartmentData> departments) {
        DefaultTableModel model = (DefaultTableModel) tableDepartmentProfit.getModel();
        model.setRowCount(0);
        
        if (departments != null && !departments.isEmpty()) {
            // Ordenar por ganancia descendente
            List<DepartmentData> sortedDepts = new ArrayList<>(departments);
            sortedDepts.sort((a, b) -> Double.compare(b.profit, a.profit));
            
            for (DepartmentData dept : sortedDepts) {
                if (dept.profit > 0) {
                    String profit = Formats.CURRENCY.formatValue(dept.profit);
                    model.addRow(new Object[]{dept.name, profit});
                }
            }
        }
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
    
    private JPanel createSalesMetricsPanel() {
        // Panel principal con dos filas
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Primera fila: Ventas Totales | Ganancia
        JPanel firstRow = new JPanel(new GridLayout(1, 2, 10, 0));
        firstRow.setBackground(Color.WHITE);
        
        // Métrica 1: Ventas Totales
        JPanel salesTotalPanel = createMetricCell("Ventas Totales");
        lblSalesTotal = new JLabel("$0.00");
        lblSalesTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSalesTotal.setForeground(new Color(50, 50, 50));
        salesTotalPanel.add(lblSalesTotal, BorderLayout.CENTER);
        firstRow.add(salesTotalPanel);
        
        // Métrica 2: Ganancia
        JPanel profitPanel = createMetricCell("Ganancia");
        lblTotalProfit = new JLabel("$0.00");
        lblTotalProfit.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotalProfit.setForeground(new Color(50, 50, 50));
        profitPanel.add(lblTotalProfit, BorderLayout.CENTER);
        firstRow.add(profitPanel);
        
        // Segunda fila: Número de Ventas | Venta Promedio | Margen de utilidad promedio
        JPanel secondRow = new JPanel(new GridLayout(1, 3, 10, 0));
        secondRow.setBackground(Color.WHITE);
        
        // Métrica 3: Número de Ventas
        JPanel numberSalesPanel = createMetricCell("Número de Ventas");
        lblNumberOfSales = new JLabel("0");
        lblNumberOfSales.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblNumberOfSales.setForeground(new Color(50, 50, 50));
        numberSalesPanel.add(lblNumberOfSales, BorderLayout.CENTER);
        secondRow.add(numberSalesPanel);
        
        // Métrica 4: Venta Promedio
        JPanel averageSalePanel = createMetricCell("Venta Promedio");
        lblAverageSale = new JLabel("$0.00");
        lblAverageSale.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblAverageSale.setForeground(new Color(50, 50, 50));
        averageSalePanel.add(lblAverageSale, BorderLayout.CENTER);
        secondRow.add(averageSalePanel);
        
        // Métrica 5: Margen de utilidad promedio
        JPanel profitMarginPanel = createMetricCell("Margen de utilidad promedio");
        lblProfitMargin = new JLabel("0.00%");
        lblProfitMargin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblProfitMargin.setForeground(new Color(50, 50, 50));
        profitMarginPanel.add(lblProfitMargin, BorderLayout.CENTER);
        secondRow.add(profitMarginPanel);
        
        mainPanel.add(firstRow, BorderLayout.NORTH);
        mainPanel.add(secondRow, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    private JPanel createMetricCell(String labelText) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(new Color(100, 100, 100));
        panel.add(label, BorderLayout.NORTH);
        
        return panel;
    }
    
    private String getTabName(int index) {
        switch (index) {
            case 0: return "📅 Día Actual";
            case 1: return "📅 Semana Actual";
            case 2: return "📅 Mes Actual";
            case 3: return "📅 Año Actual";
            case 4: return "📅 Periodo...";
            default: return "📅 Periodo";
        }
    }
    
    private void loadCurrentMonthData() {
        loadDataForPeriod(0); // Día Actual por defecto
    }
    
    private void loadDataForPeriod(int periodIndex) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Obtener fecha de inicio de la caja activa
                java.util.Date cashStartDate = getCashStartDate();
                
                Calendar cal = Calendar.getInstance();
                java.util.Date endDate = new java.util.Date();
                
                switch (periodIndex) {
                    case 0: // Día Actual - desde inicio del día de hoy o desde apertura de caja
                        Calendar todayCal = Calendar.getInstance();
                        todayCal.set(Calendar.HOUR_OF_DAY, 0);
                        todayCal.set(Calendar.MINUTE, 0);
                        todayCal.set(Calendar.SECOND, 0);
                        todayCal.set(Calendar.MILLISECOND, 0);
                        java.util.Date todayStart = todayCal.getTime();
                        
                        if (cashStartDate != null && cashStartDate.after(todayStart)) {
                            dateStart = cashStartDate; // Si la caja se abrió hoy, desde la apertura
                        } else {
                            dateStart = todayStart; // Desde inicio del día
                        }
                        dateEnd = endDate; // Hasta ahora
                        break;
                    case 1: // Semana Actual - desde inicio de caja o lunes de esta semana
                        java.util.Date weekStart = getWeekStart();
                        if (cashStartDate != null && cashStartDate.after(weekStart)) {
                            dateStart = cashStartDate;
                        } else {
                            dateStart = weekStart;
                        }
                        dateEnd = endDate;
                        break;
                    case 2: // Mes Actual - desde inicio de mes o desde apertura de caja
                        java.util.Date monthStart = getMonthStart();
                        if (cashStartDate != null && cashStartDate.after(monthStart)) {
                            dateStart = cashStartDate;
                        } else {
                            dateStart = monthStart;
                        }
                        dateEnd = endDate;
                        break;
                    case 3: // Año Actual - desde inicio de año o desde apertura de caja
                        java.util.Date yearStart = getYearStart();
                        if (cashStartDate != null && cashStartDate.after(yearStart)) {
                            dateStart = cashStartDate;
                        } else {
                            dateStart = yearStart;
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
    
    private int loadNumberOfSales() {
        int count = 0;
        try {
            if (session == null || activeCashIndex == null) {
                return 0;
            }
            
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT COUNT(DISTINCT receipts.ID) as TOTAL_COUNT ");
            sqlBuilder.append("FROM receipts ");
            sqlBuilder.append("INNER JOIN tickets ON receipts.ID = tickets.ID ");
            sqlBuilder.append("WHERE receipts.MONEY = ? ");
            
            if (dateStart != null) {
                sqlBuilder.append("AND receipts.DATENEW >= ? ");
            }
            if (dateEnd != null) {
                sqlBuilder.append("AND receipts.DATENEW <= ? ");
            }
            
            PreparedStatement stmt = session.getConnection().prepareStatement(sqlBuilder.toString());
            int paramIndex = 1;
            stmt.setString(paramIndex++, activeCashIndex);
            if (dateStart != null) {
                stmt.setTimestamp(paramIndex++, new Timestamp(dateStart.getTime()));
            }
            if (dateEnd != null) {
                stmt.setTimestamp(paramIndex++, new Timestamp(dateEnd.getTime()));
            }
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt("TOTAL_COUNT");
            }
            rs.close();
            stmt.close();
            
            LOGGER.info("Número de ventas encontradas: " + count);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error obteniendo número de ventas", e);
        }
        
        return count;
    }
    
    private void loadDataFromDatabase() throws BasicException {
        if (session == null || activeCashIndex == null) {
            LOGGER.warning("Session o activeCashIndex es null. Session: " + (session != null) + ", CashIndex: " + activeCashIndex);
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, 
                    "Error: No se pudo obtener la sesión o la caja activa. Por favor, verifica que haya una caja abierta.",
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            });
            return;
        }
        
        LOGGER.info("═══════════════════════════════════════════════");
        LOGGER.info("Cargando datos desde base de datos.");
        LOGGER.info("CashIndex: " + activeCashIndex);
        LOGGER.info("Fecha inicio: " + (dateStart != null ? dateStart : "NULL"));
        LOGGER.info("Fecha fin: " + (dateEnd != null ? dateEnd : "NULL"));
        
        try {
            // 0. Verificar si hay recepciones (receipts) en la base de datos
            verifyReceiptsExist();
            
            // 1. Obtener ventas por departamento con ganancias
            List<DepartmentData> departments = loadDepartmentData();
            LOGGER.info("Departamentos cargados: " + (departments != null ? departments.size() : 0));
            if (departments != null && !departments.isEmpty()) {
                for (DepartmentData dept : departments) {
                    LOGGER.info("  - " + dept.name + ": Ventas=" + dept.sales + ", Ganancia=" + dept.profit);
                }
            } else {
                LOGGER.warning("⚠️ No se encontraron departamentos con ventas en el periodo seleccionado");
            }
            
            // 2. Obtener ventas por forma de pago
            List<PaymentData> payments = loadPaymentData();
            LOGGER.info("Formas de pago cargadas: " + (payments != null ? payments.size() : 0));
            
            // 3. Obtener número de ventas y ventas por mes
            int numberOfSales = loadNumberOfSales();
            List<MonthData> monthData = loadSalesProfitByMonth();
            
            // 4. Calcular totales
            final List<DepartmentData> finalDepartments = departments != null ? departments : new ArrayList<>();
            final List<PaymentData> finalPayments = payments != null ? payments : new ArrayList<>();
            final List<MonthData> finalMonthData = monthData != null ? monthData : new ArrayList<>();
            
            double totalSalesCalc = 0.0;
            double totalProfitCalc = 0.0;
            for (DepartmentData dept : finalDepartments) {
                totalSalesCalc += dept.sales;
                totalProfitCalc += dept.profit;
            }
            final double totalSales = totalSalesCalc;
            final double totalProfit = totalProfitCalc;
            final int finalNumberOfSales = numberOfSales;
            final double averageSale = (numberOfSales > 0) ? (totalSales / numberOfSales) : 0.0;
            
            LOGGER.info("Totales calculados - Ventas: " + totalSales + ", Ganancia: " + totalProfit + ", Número de ventas: " + numberOfSales);
            LOGGER.info("═══════════════════════════════════════════════");
            
            // 5. Actualizar UI en el hilo de eventos
            SwingUtilities.invokeLater(() -> {
                // PRIMERO: Actualizar el gráfico principal de Ventas y Ganancias
                updateSalesProfitBarChart(totalSales, totalProfit);
                // Luego los otros gráficos
                updatePieChart(finalDepartments);
                updateBarChart(finalPayments);
                updateDepartmentTable(finalDepartments);
                updateDepartmentProfitTable(finalDepartments);
                updateSalesByMonthTable(finalMonthData);
                
                // Actualizar métricas de ventas
                if (lblSalesTotal != null) {
                    lblSalesTotal.setText(Formats.CURRENCY.formatValue(totalSales));
                }
                if (lblTotalProfit != null) {
                    lblTotalProfit.setText(Formats.CURRENCY.formatValue(totalProfit));
                }
                if (lblNumberOfSales != null) {
                    lblNumberOfSales.setText(String.valueOf(finalNumberOfSales));
                }
                if (lblAverageSale != null) {
                    lblAverageSale.setText(Formats.CURRENCY.formatValue(averageSale));
                }
                if (lblProfitMargin != null) {
                    double profitMargin = (totalSales > 0) ? ((totalProfit / totalSales) * 100.0) : 0.0;
                    lblProfitMargin.setText(String.format("%.2f%%", profitMargin));
                }
                
                // Forzar repaint
                if (chartPanelSalesProfit != null) {
                    chartPanelSalesProfit.repaint();
                }
                if (chartPanelDepartment != null) {
                    chartPanelDepartment.repaint();
                }
                repaint();
            });
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error SQL cargando datos", e);
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, 
                    "Error al cargar datos: " + e.getMessage(),
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            });
            throw new BasicException(e);
        }
    }
    
    private void verifyReceiptsExist() {
        try {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT COUNT(*) as TOTAL_RECEIPTS, ");
            sqlBuilder.append("MIN(receipts.DATENEW) as MIN_DATE, ");
            sqlBuilder.append("MAX(receipts.DATENEW) as MAX_DATE ");
            sqlBuilder.append("FROM receipts ");
            sqlBuilder.append("WHERE receipts.MONEY = ? ");
            
            if (dateStart != null) {
                sqlBuilder.append("AND receipts.DATENEW >= ? ");
            }
            if (dateEnd != null) {
                sqlBuilder.append("AND receipts.DATENEW <= ? ");
            }
            
            PreparedStatement stmt = session.getConnection().prepareStatement(sqlBuilder.toString());
            int paramIndex = 1;
            stmt.setString(paramIndex++, activeCashIndex);
            if (dateStart != null) {
                stmt.setTimestamp(paramIndex++, new Timestamp(dateStart.getTime()));
            }
            if (dateEnd != null) {
                stmt.setTimestamp(paramIndex++, new Timestamp(dateEnd.getTime()));
            }
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int totalReceipts = rs.getInt("TOTAL_RECEIPTS");
                Timestamp minDate = rs.getTimestamp("MIN_DATE");
                Timestamp maxDate = rs.getTimestamp("MAX_DATE");
                LOGGER.info("📊 Verificación: Total de recepciones encontradas: " + totalReceipts);
                if (minDate != null) {
                    LOGGER.info("  - Primera recepción: " + minDate);
                }
                if (maxDate != null) {
                    LOGGER.info("  - Última recepción: " + maxDate);
                }
                if (totalReceipts == 0) {
                    LOGGER.warning("⚠️ No se encontraron recepciones en la base de datos para el periodo seleccionado");
                }
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error verificando recepciones", e);
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
        // Asegurar que m_App esté inicializado
        if (m_App == null) {
            LOGGER.severe("m_App es null, no se puede activar el panel de gráficos");
            return;
        }
        
        // Asegurar que session y activeCashIndex estén disponibles
        if (session == null) {
            session = m_App.getSession();
        }
        
        // Intentar obtener activeCashIndex varias veces si es null
        if (activeCashIndex == null) {
            activeCashIndex = m_App.getActiveCashIndex();
        }
        
        // Si aún es null, intentar de nuevo con un pequeño delay
        if (activeCashIndex == null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    Thread.sleep(500); // Esperar 500ms
                    if (m_App != null) {
                        activeCashIndex = m_App.getActiveCashIndex();
                        LOGGER.info("Reintentando obtener activeCashIndex después de delay: " + activeCashIndex);
                        if (activeCashIndex != null) {
                            loadDataForPeriod(0); // Cargar datos del día actual
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error obteniendo activeCashIndex después de delay", e);
                }
            });
        }
        
        LOGGER.info("Panel de gráficos activado. CashIndex: " + activeCashIndex + ", Session: " + (session != null));
        
        if (activeCashIndex == null) {
            LOGGER.warning("⚠️ activeCashIndex es null - los datos no se cargarán. Verifica que haya una caja abierta.");
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, 
                    "Advertencia: No hay una caja activa abierta. Por favor, abre una caja primero para ver los gráficos.",
                    "Caja no activa", 
                    JOptionPane.WARNING_MESSAGE);
            });
            return;
        }
        
        // Cargar datos del día actual por defecto
        SwingUtilities.invokeLater(() -> {
            try {
                loadDataForPeriod(0); // Día Actual
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error recargando datos al activar panel", e);
            }
        });
    }
    
    @Override
    public boolean deactivate() {
        return true;
    }
}
