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

package com.openbravo.pos.forms;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.BaseSentence;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.SerializerReadBasic;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.data.loader.StaticSentence;
import com.openbravo.format.Formats;
import com.openbravo.pos.panels.PaymentsModel;
import com.openbravo.pos.reports.JRDataSourceBasic;
import com.openbravo.pos.reports.JRViewer400;
import com.openbravo.pos.reports.ReportFields;
import com.openbravo.pos.reports.ReportFieldsArray;
import com.openbravo.pos.reports.JPanelReport;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

/**
 * Diálogo personalizado para cerrar turno con diseño mejorado
 */
public class JDialogCloseShift extends JDialog {
    
    private static final Logger LOGGER = Logger.getLogger(JDialogCloseShift.class.getName());
    
    private boolean closed = false;
    private boolean shouldCloseShift = false;
    
    private AppView m_App;
    private DataLogicSystem m_dlSystem;
    private PaymentsModel m_PaymentsToClose;
    private Frame parentFrame; // Guardar el frame padre para usarlo en el reporte
    
    private JTextField txtDineroFisico;
    private JLabel lblDiferencia;
    private JButton btnCerrarTurno;
    private JButton btnCancelar;
    private boolean isEmployee = false;
    
    public JDialogCloseShift(Frame parent, AppView app) {
        this(parent, app, false);
    }
    
    public JDialogCloseShift(Frame parent, AppView app, boolean isEmployee) {
        super(parent, true);
        this.parentFrame = parent; // Guardar el frame padre
        this.m_App = app;
        this.isEmployee = isEmployee;
        
        try {
            m_dlSystem = (DataLogicSystem) m_App.getBean("com.openbravo.pos.forms.DataLogicSystem");
            m_PaymentsToClose = PaymentsModel.loadInstance(m_App);
        } catch (BasicException e) {
            LOGGER.log(Level.SEVERE, "Error cargando datos", e);
            JOptionPane.showMessageDialog(parent, 
                "Error al cargar datos del turno: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }
        
        initComponents();
        loadData();
        
        // Para empleados: deshabilitar cancelar y hacer el diálogo no cancelable
        if (isEmployee) {
            if (btnCancelar != null) {
                btnCancelar.setEnabled(false);
                btnCancelar.setVisible(false);
            }
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        }
    }
    
    private void initComponents() {
        setTitle("Cerrar Turno");
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        setMinimumSize(new Dimension(400, 300));
        
        // Panel principal con diseño moderno
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(245, 245, 250));
        mainPanel.setOpaque(true);
        
        // Panel central con entrada de dinero físico (sin información del turno)
        JPanel moneyPanel = createMoneyPanel();
        mainPanel.add(moneyPanel, BorderLayout.CENTER);
        
        // Panel inferior con botones
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().setBackground(new Color(245, 245, 250));
        
        pack();
        setSize(450, 350);
        setLocationRelativeTo(getParent());
    }
    
    
    private JPanel createMoneyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setOpaque(true);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "💰 Ingrese el Dinero Físico en Caja",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 16),
                new Color(70, 130, 180)
            ),
            new EmptyBorder(30, 30, 30, 30)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.anchor = GridBagConstraints.CENTER;
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtDineroFisico = new JTextField(15);
        txtDineroFisico.setFont(new Font("Arial", Font.BOLD, 28));
        txtDineroFisico.setHorizontalAlignment(JTextField.CENTER);
        txtDineroFisico.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 130, 180), 3),
            new EmptyBorder(15, 15, 15, 15)
        ));
        txtDineroFisico.setPreferredSize(new Dimension(300, 60));
        txtDineroFisico.setOpaque(true);
        txtDineroFisico.setBackground(Color.WHITE);
        txtDineroFisico.addActionListener(e -> calculateDifference());
        // Agregar listener para calcular automáticamente mientras escribe
        txtDineroFisico.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                calculateDifference();
            }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                calculateDifference();
            }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                calculateDifference();
            }
        });
        panel.add(txtDineroFisico, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.insets = new Insets(20, 15, 15, 15);
        lblDiferencia = new JLabel(" ");
        lblDiferencia.setFont(new Font("Arial", Font.BOLD, 18));
        lblDiferencia.setHorizontalAlignment(JLabel.CENTER);
        lblDiferencia.setOpaque(false);
        panel.add(lblDiferencia, gbc);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBackground(new Color(245, 245, 250));
        panel.setOpaque(true);
        
        btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Arial", Font.PLAIN, 12));
        btnCancelar.setPreferredSize(new Dimension(120, 35));
        btnCancelar.setOpaque(true);
        btnCancelar.addActionListener(e -> {
            closed = false;
            shouldCloseShift = false;
            dispose();
        });
        
        btnCerrarTurno = new JButton("Cerrar Turno");
        btnCerrarTurno.setFont(new Font("Arial", Font.BOLD, 12));
        btnCerrarTurno.setPreferredSize(new Dimension(140, 35));
        btnCerrarTurno.setBackground(new Color(70, 130, 180));
        btnCerrarTurno.setForeground(Color.WHITE);
        btnCerrarTurno.setFocusPainted(false);
        btnCerrarTurno.setOpaque(true);
        btnCerrarTurno.addActionListener(e -> closeShift());
        
        panel.add(btnCancelar);
        panel.add(btnCerrarTurno);
        
        return panel;
    }
    
    private void loadData() {
        try {
            // Calcular total esperado para sugerir en el campo
            double fondoInicial = getInitialAmount();
            double efectivoRecibido = m_PaymentsToClose.getCashTotal() != null ? 
                m_PaymentsToClose.getCashTotal() : 0.0;
            double totalEsperado = fondoInicial + efectivoRecibido;
            
            // Establecer valor sugerido en el campo de dinero físico
            txtDineroFisico.setText(Formats.CURRENCY.formatValue(totalEsperado));
            txtDineroFisico.selectAll();
            txtDineroFisico.requestFocus();
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error cargando datos", e);
        }
    }
    
    private double getInitialAmount() {
        try {
            String activeCashIndex = m_App.getActiveCashIndex();
            Connection conn = m_App.getSession().getConnection();
            
            String sql = "SELECT INITIAL_AMOUNT FROM CLOSEDCASH WHERE MONEY = ? AND DATEEND IS NULL";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, activeCashIndex);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double amount = rs.getDouble("INITIAL_AMOUNT");
                if (!rs.wasNull()) {
                    rs.close();
                    pstmt.close();
                    return amount;
                }
            }
            rs.close();
            pstmt.close();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error obteniendo fondo inicial", e);
        }
        
        // Fallback
        if (m_PaymentsToClose != null && m_PaymentsToClose.getInitialAmount() != null) {
            return m_PaymentsToClose.getInitialAmount();
        }
        return m_App.getActiveCashInitialAmount();
    }
    
    private void calculateDifference() {
        try {
            String input = txtDineroFisico.getText().trim();
            if (input.isEmpty()) {
                lblDiferencia.setText("");
                return;
            }
            
            double dineroFisico;
            try {
                dineroFisico = Formats.CURRENCY.parseValue(input);
            } catch (BasicException e) {
                String inputLimpio = input.replace("$", "").replace(",", "").replace(" ", "");
                dineroFisico = Double.parseDouble(inputLimpio);
            }
            
            double fondoInicial = getInitialAmount();
            double efectivoRecibido = m_PaymentsToClose.getCashTotal() != null ? 
                m_PaymentsToClose.getCashTotal() : 0.0;
            double totalEsperado = fondoInicial + efectivoRecibido;
            double diferencia = dineroFisico - totalEsperado;
            
            if (Math.abs(diferencia) < 0.01) {
                lblDiferencia.setText("✓ Cuadra perfectamente");
                lblDiferencia.setForeground(new Color(34, 139, 34));
            } else if (diferencia > 0) {
                lblDiferencia.setText("Sobrante: " + Formats.CURRENCY.formatValue(diferencia));
                lblDiferencia.setForeground(new Color(255, 140, 0));
            } else {
                lblDiferencia.setText("Faltante: " + Formats.CURRENCY.formatValue(Math.abs(diferencia)));
                lblDiferencia.setForeground(new Color(220, 20, 60));
            }
        } catch (Exception e) {
            lblDiferencia.setText("Error en el cálculo");
            lblDiferencia.setForeground(Color.RED);
        }
    }
    
    private void closeShift() {
        try {
            String input = txtDineroFisico.getText().trim();
            if (input.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Debe ingresar el dinero físico contado.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE);
                txtDineroFisico.requestFocus();
                return;
            }
            
            double dineroFisico;
            try {
                dineroFisico = Formats.CURRENCY.parseValue(input);
            } catch (BasicException e) {
                String inputLimpio = input.replace("$", "").replace(",", "").replace(" ", "");
                dineroFisico = Double.parseDouble(inputLimpio);
            }
            
            double fondoInicial = getInitialAmount();
            double efectivoRecibido = m_PaymentsToClose.getCashTotal() != null ? 
                m_PaymentsToClose.getCashTotal() : 0.0;
            double totalEsperado = fondoInicial + efectivoRecibido;
            double diferencia = dineroFisico - totalEsperado;
            
            // Confirmar cierre
            String mensaje = "¿Está seguro de cerrar el turno?\n\n";
            mensaje += "Dinero esperado: " + Formats.CURRENCY.formatValue(totalEsperado) + "\n";
            mensaje += "Dinero físico: " + Formats.CURRENCY.formatValue(dineroFisico) + "\n";
            if (Math.abs(diferencia) > 0.01) {
                if (diferencia > 0) {
                    mensaje += "Sobrante: " + Formats.CURRENCY.formatValue(diferencia) + "\n";
                } else {
                    mensaje += "Faltante: " + Formats.CURRENCY.formatValue(Math.abs(diferencia)) + "\n";
                }
            }
            
            int confirm = JOptionPane.showConfirmDialog(this,
                mensaje,
                "Confirmar Cierre de Turno",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            
            // Guardar información del turno actual antes de cerrarlo
            String oldCashIndex = m_App.getActiveCashIndex();
            int oldSequence = m_App.getActiveCashSequence();
            Date oldDateStart = m_App.getActiveCashDateStart();
            
            // Cerrar el turno
            Date dNow = new Date();
            
            // Sebastian - Actualizar el turno existente en lugar de insertar uno nuevo
            if (m_App.getActiveCashDateEnd() == null) {
                com.openbravo.data.loader.StaticSentence<Object[], Integer> sentence = 
                    new com.openbravo.data.loader.StaticSentence<>(
                        m_App.getSession(),
                        "UPDATE closedcash SET DATEEND = ?, NOSALES = ? WHERE HOST = ? AND MONEY = ?",
                        new com.openbravo.data.loader.SerializerWriteBasic(
                            new com.openbravo.data.loader.Datas[] {
                                com.openbravo.data.loader.Datas.TIMESTAMP,
                                com.openbravo.data.loader.Datas.INT,
                                com.openbravo.data.loader.Datas.STRING,
                                com.openbravo.data.loader.Datas.STRING
                            }
                        )
                    );
                
                int noSales = m_PaymentsToClose.getSales();
                
                sentence.exec(new Object[] {
                    dNow,
                    noSales,
                    m_App.getProperties().getHost(),
                    oldCashIndex
                });
                LOGGER.log(Level.INFO, "✅ Sebastian - Turno cerrado correctamente (UPDATE): " + oldCashIndex);
            } else {
                // El turno ya estaba cerrado, solo actualizar NOSALES si es necesario
                LOGGER.log(Level.INFO, "⚠️ Sebastian - El turno ya estaba cerrado: " + oldCashIndex);
            }
            
            // Sebastian - Establecer el turno como cerrado en las propiedades (con DATEEND)
            // El próximo usuario detectará que el turno está cerrado y creará su propio turno
            m_App.setActiveCash(
                oldCashIndex,  // Mantener el índice del turno cerrado
                oldSequence,
                oldDateStart,
                dNow,  // DATEEND = ahora (turno cerrado)
                0.0
            );
            LOGGER.log(Level.INFO, "✅ Sebastian - Turno cerrado y guardado en propiedades (DATEEND=" + dNow + "). " +
                       "El próximo usuario detectará el turno cerrado y creará su propio turno.");
            
            // Registrar apertura de cajón
            m_dlSystem.execDrawerOpened(
                new Object[] {
                    m_App.getAppUserView().getUser().getName(),
                    "Close Cash"
                }
            );
            
            // Establecer fecha de fin
            m_PaymentsToClose.setDateEnd(dNow);
            
            // Verificar si hay más turnos abiertos del día
            boolean isLastShiftOfDay = isLastShiftOfDay(dNow);
            
            // Mostrar mensaje de éxito
            if (isLastShiftOfDay) {
                int option = JOptionPane.showConfirmDialog(this,
                    "✓ Turno cerrado exitosamente\n\n" +
                    "Este es el último turno del día.\n" +
                    "¿Desea ver el reporte completo del día con todos los turnos?",
                    "Cierre de Turno - Último del Día",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
                
                if (option == JOptionPane.YES_OPTION) {
                    // Mostrar reporte del día de forma síncrona (bloquea hasta que se cierre)
                    // Esto asegura que el reporte se muestre ANTES de cerrar el diálogo principal
                    showDayReport(dNow);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                    "✓ Turno cerrado exitosamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
            }
            
            // Solo marcar como cerrado y cerrar el diálogo DESPUÉS de que todos los diálogos se hayan cerrado
            closed = true;
            shouldCloseShift = true;
            dispose();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cerrando turno", e);
            JOptionPane.showMessageDialog(this,
                "Error al cerrar el turno: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public boolean isClosed() {
        return closed;
    }
    
    public boolean shouldCloseShift() {
        return shouldCloseShift;
    }
    
    /**
     * Verifica si este es el último turno del día
     */
    private boolean isLastShiftOfDay(Date closeDate) {
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
            
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
            cal.set(java.util.Calendar.MINUTE, 59);
            cal.set(java.util.Calendar.SECOND, 59);
            cal.set(java.util.Calendar.MILLISECOND, 999);
            java.sql.Timestamp endOfDay = new java.sql.Timestamp(cal.getTimeInMillis());
            
            // Contar turnos abiertos del día (que no tengan DATEEND)
            // Usar comparación de TIMESTAMP en lugar de DATE() para compatibilidad con HSQLDB
            String sql = "SELECT COUNT(*) FROM closedcash " +
                        "WHERE DATESTART >= ? AND DATESTART < ? AND DATEEND IS NULL AND HOST = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setTimestamp(1, startOfDay);
            pstmt.setTimestamp(2, new java.sql.Timestamp(endOfDay.getTime() + 1)); // +1ms para incluir el final del día
            pstmt.setString(3, m_App.getProperties().getHost());
            
            ResultSet rs = pstmt.executeQuery();
            int openShifts = 0;
            if (rs.next()) {
                openShifts = rs.getInt(1);
            }
            rs.close();
            pstmt.close();
            
            // Si no hay turnos abiertos, este era el último
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
            // Usar el frame padre guardado en el constructor
            // Si no está disponible, intentar obtenerlo del window ancestor
            Frame frameToUse = this.parentFrame;
            if (frameToUse == null) {
                Window parentWindow = SwingUtilities.getWindowAncestor(this);
                if (parentWindow instanceof Frame) {
                    frameToUse = (Frame) parentWindow;
                } else if (parentWindow instanceof Dialog) {
                    frameToUse = (Frame) ((Dialog) parentWindow).getParent();
                }
            }
            
            // Crear diálogo para mostrar el reporte (modal, bloquea hasta que se cierre)
            // IMPORTANTE: Este diálogo debe mostrarse ANTES de que el diálogo principal se cierre
            // El diálogo es modal, por lo que setVisible(true) bloqueará hasta que se cierre
            JDialog reportDialog = new JDialog(frameToUse, 
                "Reporte de Caja Cerrada - Día Completo", true);
            reportDialog.setSize(1000, 700);
            reportDialog.setLocationRelativeTo(this);
            
            // Crear panel con el visor de reportes
            JPanel panel = new JPanel(new BorderLayout());
            JRViewer400 reportViewer = new JRViewer400(null);
            panel.add(reportViewer, BorderLayout.CENTER);
            
            // Botón cerrar
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnClose = new JButton("Cerrar");
            btnClose.addActionListener(e -> reportDialog.dispose());
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
                    
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                    cal.set(java.util.Calendar.MINUTE, 59);
                    cal.set(java.util.Calendar.SECOND, 59);
                    cal.set(java.util.Calendar.MILLISECOND, 999);
                    java.sql.Timestamp endOfDay = new java.sql.Timestamp(cal.getTimeInMillis());
                    
                    // Crear la consulta SQL para el día usando comparación de TIMESTAMP
                    String sentence = "SELECT " + 
                        "closedcash.HOST, " +
                        "closedcash.HOSTSEQUENCE, " +
                        "closedcash.MONEY, " +
                        "closedcash.DATESTART, " +
                        "closedcash.DATEEND, " +
                        "COALESCE(closedcash.INITIAL_AMOUNT, 0.0) AS INITIAL_AMOUNT, " +
                        "COALESCE(payments.PAYMENT, 'Sin ventas') AS PAYMENT, " +
                        "COALESCE(SUM(payments.TOTAL), 0.0) AS TOTAL " +
                        "FROM closedcash " +
                        "LEFT JOIN receipts ON closedcash.MONEY = receipts.MONEY " +
                        "LEFT JOIN payments ON payments.RECEIPT = receipts.ID " +
                        "WHERE closedcash.DATEEND IS NOT NULL " +
                        "AND closedcash.DATEEND >= ? " +
                        "AND closedcash.DATEEND <= ? " +
                        "AND closedcash.HOST = ? " +
                        "GROUP BY closedcash.HOST, closedcash.HOSTSEQUENCE, closedcash.MONEY, closedcash.DATESTART, closedcash.DATEEND, closedcash.INITIAL_AMOUNT, COALESCE(payments.PAYMENT, 'Sin ventas') " +
                        "ORDER BY closedcash.HOST, closedcash.HOSTSEQUENCE, closedcash.DATEEND DESC";
                    
                    // Crear sentence con parámetros
                    BaseSentence<Object[]> reportSentence = new StaticSentence<>(
                        m_App.getSession(),
                        sentence,
                        new SerializerWriteBasic(new Datas[] {Datas.TIMESTAMP, Datas.TIMESTAMP, Datas.STRING}),
                        new SerializerReadBasic(new Datas[] {
                            Datas.STRING, Datas.INT, Datas.STRING, 
                            Datas.TIMESTAMP, Datas.TIMESTAMP, Datas.DOUBLE, Datas.STRING, Datas.DOUBLE
                        })
                    );
                    
                    // Crear campos del reporte
                    ReportFields reportFields = new ReportFieldsArray(
                        new String[] {"HOST", "HOSTSEQUENCE", "MONEY", "DATESTART", "DATEEND", "INITIAL_AMOUNT", "PAYMENT", "TOTAL"}
                    );
                    
                    // Crear fuente de datos
                    JRDataSourceBasic dataSource = new JRDataSourceBasic(
                        reportSentence, 
                        reportFields, 
                        new Object[] {startOfDay, endOfDay, m_App.getProperties().getHost()}
                    );
                    
                    // Parámetros del reporte
                    Map<String, Object> reportParams = new HashMap<>();
                    reportParams.put("ARG", new Object[] {startOfDay, endOfDay, m_App.getProperties().getHost()});
                    
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

