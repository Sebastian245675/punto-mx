package com.openbravo.pos.forms;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.data.loader.StaticSentence;
import com.openbravo.format.Formats;
import com.openbravo.pos.panels.PaymentsModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

/**
 * Dialogo de cierre de turno - diseño fiel a Eleventa
 */
public class JDialogCloseShift extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(JDialogCloseShift.class.getName());

    // Colores
    private static final Color COL_HEADER_BG = new Color(52, 90, 70); // verde oscuro header
    private static final Color COL_HEADER_FG = Color.WHITE;
    private static final Color COL_ROW_BG = new Color(240, 243, 240); // gris verdoso claro filas alternas
    private static final Color COL_WHITE = Color.WHITE;
    private static final Color COL_LABEL = new Color(50, 50, 50);
    private static final Color COL_GREEN_TEXT = new Color(34, 120, 34);
    private static final Color COL_RED_TEXT = new Color(180, 30, 30);
    private static final Color COL_BLUE_TEXT = new Color(20, 80, 180);
    private static final Color COL_STATUS_GREEN_BG = new Color(221, 240, 221);
    private static final Color COL_STATUS_RED_BG = new Color(245, 220, 220);
    private static final Color COL_STATUS_BLUE_BG = new Color(220, 230, 245);
    private static final Color COL_BTN_GREEN = new Color(70, 140, 70);
    private static final Color COL_BTN_GRAY_BG = new Color(210, 210, 210);
    private static final Color COL_GRID_LINE = new Color(180, 190, 180);

    private boolean closed = false;
    private boolean shouldCloseShift = false;

    private final AppView m_App;
    private DataLogicSystem m_dlSystem;
    private PaymentsModel m_PaymentsToClose;

    // Componentes
    private JLabel lblEsperadoValor;
    private JTextField txtFisico;
    private JLabel lblDiferenciaValor;
    private JPanel panelStatus;
    private JLabel lblStatusIcon;
    private JLabel lblStatusText;

    private double m_totalEsperado = 0.0;

    // -------------------------------------------------------------------------
    public JDialogCloseShift(Frame parent, AppView app) {
        super(parent, true);
        this.m_App = app;

        try {
            m_dlSystem = (DataLogicSystem) m_App.getBean("com.openbravo.pos.forms.DataLogicSystem");
            m_PaymentsToClose = PaymentsModel.loadInstance(m_App);
        } catch (BasicException e) {
            LOGGER.log(Level.SEVERE, "Error cargando datos", e);
            JOptionPane.showMessageDialog(parent, "Error al cargar datos: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        buildUI();
        loadData();
        pack();
        setSize(Math.max(getWidth(), 650), getHeight());
        setMinimumSize(new Dimension(650, getHeight()));
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    // -------------------------------------------------------------------------
    private void buildUI() {
        setTitle("Cierre de turno");
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(COL_WHITE);

        // ── HEADER VERDE OSCURO ──────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COL_HEADER_BG);
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel lblHeader = new JLabel("CIERRE DE TURNO");
        lblHeader.setFont(new Font("Arial", Font.BOLD, 20));
        lblHeader.setForeground(COL_HEADER_FG);
        header.add(lblHeader, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        // ── CUERPO ───────────────────────────────────────────────────────────
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(COL_WHITE);
        body.setBorder(new EmptyBorder(10, 16, 0, 16));

        // Texto instructivo
        JLabel lblInstruction = new JLabel(
                "<html>Por favor cuenta el dinero en caja e ingrésalo para<br>proceder con el cierre de turno.</html>");
        lblInstruction.setFont(new Font("Arial", Font.PLAIN, 13));
        lblInstruction.setForeground(COL_LABEL);
        lblInstruction.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(lblInstruction);
        body.add(Box.createVerticalStrut(10));

        // ── TABLA DE VALORES ─────────────────────────────────────────────────
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setBackground(COL_WHITE);

        GridBagConstraints cl = new GridBagConstraints(); // label column
        cl.anchor = GridBagConstraints.WEST;
        cl.insets = new Insets(0, 0, 0, 0);
        cl.fill = GridBagConstraints.BOTH;
        cl.weighty = 1;

        GridBagConstraints cv = new GridBagConstraints(); // value column
        cv.anchor = GridBagConstraints.EAST;
        cv.insets = new Insets(0, 0, 0, 0);
        cv.fill = GridBagConstraints.BOTH;
        cv.weighty = 1;

        // ROW 1 – Efectivo esperado
        cl.gridx = 0;
        cl.gridy = 0;
        cl.weightx = 1;
        cv.gridx = 1;
        cv.gridy = 0;
        cv.weightx = 0;
        JPanel r1l = cellPanel(COL_ROW_BG, new JLabel("  Efectivo esperado en caja  "), false);
        lblEsperadoValor = new JLabel("$0.00  ");
        lblEsperadoValor.setFont(new Font("Arial", Font.BOLD, 15));
        lblEsperadoValor.setForeground(COL_LABEL);
        JPanel r1v = cellPanel(COL_ROW_BG, lblEsperadoValor, true);
        grid.add(r1l, cl);
        grid.add(r1v, cv);

        // ROW 2 – Input dinero físico
        cl.gridy = 1;
        cv.gridy = 1;
        JPanel r2l = cellPanel(COL_WHITE, new JLabel("  ¿Cuánto efectivo hay en Caja?  "), false);

        txtFisico = new JTextField(9);
        txtFisico.setFont(new Font("Arial", Font.BOLD, 15));
        txtFisico.setHorizontalAlignment(JTextField.RIGHT);
        txtFisico.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 130, 200), 2),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        txtFisico.setBackground(new Color(200, 220, 245));
        txtFisico.setForeground(COL_LABEL);
        txtFisico.addActionListener(e -> confirmarCierre());
        txtFisico.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                recalcular();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                recalcular();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                recalcular();
            }
        });

        JPanel inputWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 6));
        inputWrapper.setBackground(COL_WHITE);
        inputWrapper.add(txtFisico);
        JPanel r2v = new JPanel(new BorderLayout());
        r2v.setBackground(COL_WHITE);
        r2v.setBorder(new MatteBorder(0, 0, 1, 0, COL_GRID_LINE));
        r2v.add(inputWrapper, BorderLayout.CENTER);

        grid.add(r2l, cl);
        grid.add(r2v, cv);

        // ROW 3 – Diferencia
        cl.gridy = 2;
        cv.gridy = 2;
        JPanel r3l = cellPanel(COL_ROW_BG, new JLabel("  Diferencia  "), false);
        lblDiferenciaValor = new JLabel("$0.00  ");
        lblDiferenciaValor.setFont(new Font("Arial", Font.BOLD, 15));
        lblDiferenciaValor.setForeground(COL_GREEN_TEXT);
        JPanel r3v = cellPanel(COL_ROW_BG, lblDiferenciaValor, true);
        grid.add(r3l, cl);
        grid.add(r3v, cv);

        // Borde exterior del grid
        grid.setBorder(BorderFactory.createLineBorder(COL_GRID_LINE, 1));
        body.add(grid);

        // ── PANEL DE ESTADO ──────────────────────────────────────────────────
        panelStatus = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 8));
        panelStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelStatus.setBackground(COL_STATUS_GREEN_BG);

        lblStatusIcon = new JLabel("✔");
        lblStatusIcon.setFont(new Font("Arial", Font.BOLD, 14));
        lblStatusIcon.setForeground(COL_GREEN_TEXT);

        lblStatusText = new JLabel("¡Excelente! Todo en orden");
        lblStatusText.setFont(new Font("Arial", Font.BOLD, 14));
        lblStatusText.setForeground(COL_GREEN_TEXT);

        panelStatus.add(lblStatusIcon);
        panelStatus.add(lblStatusText);
        body.add(panelStatus);

        root.add(body, BorderLayout.CENTER);

        // ── BOTONES ──────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new GridLayout(1, 2, 0, 0));
        footer.setBorder(new MatteBorder(1, 0, 0, 0, COL_GRID_LINE));

        JButton btnCerrar = buildButton("🔒  Cerrar Turno", COL_BTN_GREEN, COL_HEADER_FG);
        btnCerrar.addActionListener(e -> confirmarCierre());

        JButton btnCancelar = buildButton("Cancelar", COL_BTN_GRAY_BG, COL_LABEL);
        btnCancelar.addActionListener(e -> dispose());

        footer.add(btnCerrar);
        footer.add(btnCancelar);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private JPanel cellPanel(Color bg, JLabel label, boolean alignRight) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bg);
        p.setBorder(new MatteBorder(0, 0, 1, 0, COL_GRID_LINE));
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        label.setForeground(COL_LABEL);
        if (alignRight) {
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            p.add(label, BorderLayout.EAST);
        } else {
            p.add(label, BorderLayout.WEST);
        }
        p.setPreferredSize(new Dimension(alignRight ? 220 : 380, 40));
        return p;
    }

    private JButton buildButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setPreferredSize(new Dimension(260, 52));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(bg.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    // ── datos ─────────────────────────────────────────────────────────────────
    private void loadData() {
        try {
            double fondoInicial = getInitialAmount();
            double efectivoRecibido = m_PaymentsToClose.getCashTotal() != null ? m_PaymentsToClose.getCashTotal() : 0.0;
            m_totalEsperado = fondoInicial + efectivoRecibido;

            String fmt = Formats.CURRENCY.formatValue(m_totalEsperado);
            lblEsperadoValor.setText(fmt + "  ");
            txtFisico.setText(fmt);
            txtFisico.selectAll();
            SwingUtilities.invokeLater(() -> txtFisico.requestFocusInWindow());
            recalcular();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error cargando datos", e);
        }
    }

    private double getInitialAmount() {
        try {
            String activeCashIndex = m_App.getActiveCashIndex();
            Connection conn = m_App.getSession().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT INITIAL_AMOUNT FROM CLOSEDCASH WHERE MONEY = ? AND DATEEND IS NULL");
            pstmt.setString(1, activeCashIndex);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double v = rs.getDouble("INITIAL_AMOUNT");
                if (!rs.wasNull()) {
                    rs.close();
                    pstmt.close();
                    return v;
                }
            }
            rs.close();
            pstmt.close();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error obteniendo fondo inicial", e);
        }
        if (m_PaymentsToClose != null && m_PaymentsToClose.getInitialAmount() != null)
            return m_PaymentsToClose.getInitialAmount();
        return m_App.getActiveCashInitialAmount();
    }

    private void recalcular() {
        try {
            String raw = txtFisico.getText().trim();
            if (raw.isEmpty()) {
                setStatus(0, true);
                return;
            }

            double fisico;
            try {
                fisico = Formats.CURRENCY.parseValue(raw);
            } catch (BasicException ex) {
                fisico = Double.parseDouble(raw.replace("$", "").replace(",", "").replace(" ", ""));
            }

            double diferencia = fisico - m_totalEsperado;
            lblDiferenciaValor.setText(Formats.CURRENCY.formatValue(diferencia) + "  ");
            setStatus(diferencia, false);
        } catch (Exception ex) {
            setStatus(0, true);
        }
    }

    private void setStatus(double diferencia, boolean empty) {
        if (empty || Math.abs(diferencia) < 0.01) {
            panelStatus.setBackground(COL_STATUS_GREEN_BG);
            lblStatusIcon.setText("✔");
            lblStatusIcon.setForeground(COL_GREEN_TEXT);
            lblStatusText.setText("¡Excelente! Todo en orden");
            lblStatusText.setForeground(COL_GREEN_TEXT);
            lblDiferenciaValor.setForeground(COL_GREEN_TEXT);
        } else if (diferencia > 0) {
            panelStatus.setBackground(COL_STATUS_BLUE_BG);
            lblStatusIcon.setText("ℹ");
            lblStatusIcon.setForeground(COL_BLUE_TEXT);
            lblStatusText.setText("Sobrante detectado");
            lblStatusText.setForeground(COL_BLUE_TEXT);
            lblDiferenciaValor.setForeground(COL_BLUE_TEXT);
        } else {
            panelStatus.setBackground(COL_STATUS_RED_BG);
            lblStatusIcon.setText("✘");
            lblStatusIcon.setForeground(COL_RED_TEXT);
            lblStatusText.setText("Faltante detectado");
            lblStatusText.setForeground(COL_RED_TEXT);
            lblDiferenciaValor.setForeground(COL_RED_TEXT);
        }
        panelStatus.revalidate();
        panelStatus.repaint();
    }

    // ── acción de cierre ──────────────────────────────────────────────────────
    private void confirmarCierre() {
        try {
            String raw = txtFisico.getText().trim();
            if (raw.isEmpty()) {
                txtFisico.requestFocusInWindow();
                return;
            }

            double fisico;
            try {
                fisico = Formats.CURRENCY.parseValue(raw);
            } catch (BasicException ex) {
                fisico = Double.parseDouble(raw.replace("$", "").replace(",", "").replace(" ", ""));
            }

            double diferencia = fisico - m_totalEsperado;

            // Avisar si hay diferencia > 1 peso
            if (Math.abs(diferencia) > 1.0) {
                String msg = diferencia < 0
                        ? "Hay un faltante de " + Formats.CURRENCY.formatValue(Math.abs(diferencia))
                                + ".\n¿Desea cerrar el turno de todos modos?"
                        : "Hay un sobrante de " + Formats.CURRENCY.formatValue(diferencia)
                                + ".\n¿Desea cerrar el turno de todos modos?";
                int r = JOptionPane.showConfirmDialog(this, msg, "Confirmar cierre",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (r != JOptionPane.YES_OPTION)
                    return;
            }

            // Ejecutar cierre
            String oldCashIndex = m_App.getActiveCashIndex();
            int oldSequence = m_App.getActiveCashSequence();
            Date oldDateStart = m_App.getActiveCashDateStart();
            Date dNow = new Date();

            double faltante = diferencia < 0 ? Math.abs(diferencia) : 0.0;
            double sobrante = diferencia > 0 ? diferencia : 0.0;

            // Asegurar que las columnas existan en la tabla closedcash antes de ejecutar el UPDATE
            try {
                String testSql = "SELECT faltante_cierre, sobrante_cierre FROM closedcash WHERE 1=0";
                new StaticSentence(m_App.getSession(), testSql).exec();
            } catch (BasicException e) {
                LOGGER.info("Columnas faltante_cierre y sobrante_cierre no existen en closedcash, intentando crearlas...");
                try {
                    String alterSql1 = "ALTER TABLE closedcash ADD COLUMN faltante_cierre NUMERIC(10,2) DEFAULT 0.00";
                    new StaticSentence(m_App.getSession(), alterSql1).exec();
                    LOGGER.info("Columna faltante_cierre creada exitosamente");
                } catch (BasicException ex1) {
                    LOGGER.warning("No se pudo crear faltante_cierre: " + ex1.getMessage());
                }
                try {
                    String alterSql2 = "ALTER TABLE closedcash ADD COLUMN sobrante_cierre NUMERIC(10,2) DEFAULT 0.00";
                    new StaticSentence(m_App.getSession(), alterSql2).exec();
                    LOGGER.info("Columna sobrante_cierre creada exitosamente");
                } catch (BasicException ex2) {
                    LOGGER.warning("No se pudo crear sobrante_cierre: " + ex2.getMessage());
                }
            }

            if (m_App.getActiveCashDateEnd() == null) {
                StaticSentence<Object[], Integer> stmt = new StaticSentence<>(
                        m_App.getSession(),
                        "UPDATE closedcash SET DATEEND = ?, NOSALES = ?, faltante_cierre = ?, sobrante_cierre = ? WHERE HOST = ? AND MONEY = ?",
                        new SerializerWriteBasic(new Datas[] {
                                Datas.TIMESTAMP, Datas.INT, Datas.DOUBLE, Datas.DOUBLE, Datas.STRING, Datas.STRING }));
                stmt.exec(new Object[] { dNow, m_PaymentsToClose.getSales(), faltante, sobrante,
                        m_App.getProperties().getHost(), oldCashIndex });
            }

            m_App.setActiveCash(oldCashIndex, oldSequence, oldDateStart, dNow, 0.0);
            m_dlSystem.execDrawerOpened(new Object[] { m_App.getAppUserView().getUser().getName(), "Close Cash" });

            if (m_PaymentsToClose != null) {
                m_PaymentsToClose.setDateEnd(dNow);
            }

            // Abrir cajón de dinero físicamente al cerrar turno usando la misma lógica del botón "Probar Cajón" de Configuración
            openCashDrawer(m_App);

            LOGGER.info("Turno cerrado sin impresión automática. El reporte queda disponible desde el botón Imprimir.");

            this.closed = true;
            this.shouldCloseShift = true;
            dispose();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cerrando turno", e);
            JOptionPane.showMessageDialog(this, "Error al cerrar el turno:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean shouldCloseShift() {
        return shouldCloseShift;
    }

    /**
     * Método para abrir el cajón monedero UNA SOLA VEZ al cerrar turno.
     */
    public static void openCashDrawer(AppView app) {
        if (app == null) {
            return;
        }

        try {
            if (app.getDeviceTicket() != null && app.getDeviceTicket().getDevicePrinter("1") != null) {
                app.getDeviceTicket().getDevicePrinter("1").openDrawer();
                LOGGER.info("Cajón monedero abierto exitosamente al cerrar turno.");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error al abrir el cajón monedero al cerrar turno: " + ex.getMessage(), ex);
        }
    }
}
