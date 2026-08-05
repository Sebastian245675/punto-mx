package com.openbravo.pos.admin;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.firebase.FirebaseServiceREST;
import com.openbravo.beans.JCalendarDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Birthday Management Panel - Ported from React EmployeeManager.tsx to Java
 * Swing
 *
 * @author Antigravity
 */
public class BirthdaysPanel extends JPanel implements JPanelView, BeanFactoryApp {

    private static final Logger LOGGER = Logger.getLogger(BirthdaysPanel.class.getName());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    // Colors
    private static final Color COLOR_PRIMARY = new Color(0, 150, 136); // Teal
    private static final Color COLOR_TODAY_BG = new Color(255, 243, 224); // Soft orange/gold
    private static final Color COLOR_TODAY_BORDER = new Color(255, 152, 0); // Orange
    private static final Color COLOR_UPCOMING_BG = new Color(224, 242, 241); // Soft teal
    private static final Color COLOR_UPCOMING_BORDER = new Color(77, 182, 172);
    private static final Color COLOR_MUTED_TEXT = new Color(117, 117, 117);
    private static final Color COLOR_DARK_TEXT = new Color(33, 33, 33);

    private AppView m_App;
    private Connection m_Connection;

    // UI Components
    private DefaultListModel<String> m_UpcomingListModel;
    private JList<String> m_UpcomingList;

    private JTextField m_txtSearch;
    private JComboBox<String> m_cbMonthFilter;
    private JTable m_Table;
    private DefaultTableModel m_TableModel;

    // Directory data holder
    private final List<EmployeeData> m_EmployeesList = new ArrayList<>();

    public BirthdaysPanel() {
        initComponents();
    }

    @Override
    public void init(AppView app) throws BeanFactoryException {
        m_App = app;
        try {
            m_Connection = app.getSession().getConnection();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not acquire database connection", e);
            throw new BeanFactoryException(e);
        }
    }

    @Override
    public Object getBean() {
        return this;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public String getTitle() {
        return "Gestión de Cumpleaños";
    }

    @Override
    public void activate() throws BasicException {
        loadData();
    }

    @Override
    public boolean deactivate() {
        return true;
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 15));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(Color.WHITE);

        // --- TITLE HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("DIRECTORIO Y CUMPLEANOS DE COLABORADORES");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(COLOR_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton btnRefresh = new JButton("Actualizar");
        btnRefresh.setFont(new Font("Arial", Font.BOLD, 12));
        btnRefresh.setBackground(new Color(240, 240, 240));
        btnRefresh.addActionListener(e -> loadData(true));
        headerPanel.add(btnRefresh, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // --- UPCOMING BIRTHDAYS PANEL (SINGLE GREEN BOX) ---
        JPanel upcomingPanel = new JPanel(new BorderLayout(5, 5));
        upcomingPanel.setBackground(COLOR_UPCOMING_BG);
        upcomingPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_UPCOMING_BORDER, 2),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        JLabel lblUpcomingTitle = new JLabel("Próximos Cumpleaños y Celebraciones 📅");
        lblUpcomingTitle.setFont(new Font("Arial", Font.BOLD, 14));
        lblUpcomingTitle.setForeground(COLOR_PRIMARY);
        upcomingPanel.add(lblUpcomingTitle, BorderLayout.NORTH);

        m_UpcomingListModel = new DefaultListModel<>();
        m_UpcomingList = new JList<>(m_UpcomingListModel);
        m_UpcomingList.setBackground(COLOR_UPCOMING_BG);
        m_UpcomingList.setFont(new Font("Arial", Font.PLAIN, 12));
        m_UpcomingList.setForeground(COLOR_DARK_TEXT);
        m_UpcomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane spUpcoming = new JScrollPane(m_UpcomingList);
        spUpcoming.setBorder(null);
        spUpcoming.setBackground(COLOR_UPCOMING_BG);
        upcomingPanel.add(spUpcoming, BorderLayout.CENTER);

        // --- DIRECTORY AND FILTERING (CENTER/BOTTOM) ---
        JPanel mainContentPanel = new JPanel(new BorderLayout(0, 10));
        mainContentPanel.setBackground(Color.WHITE);

        // Search and Action Bar
        JPanel searchBarPanel = new JPanel(new GridBagLayout());
        searchBarPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Search Field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        searchBarPanel.add(new JLabel("Buscar:"), gbc);

        m_txtSearch = new JTextField(20);
        m_txtSearch.setFont(new Font("Arial", Font.PLAIN, 12));
        m_txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyFilters();
            }
        });
        gbc.gridx = 1;
        gbc.weightx = 0;
        searchBarPanel.add(m_txtSearch, gbc);

        // Month Filter
        gbc.gridx = 2;
        gbc.weightx = 0;
        searchBarPanel.add(new JLabel("Mes Cumpleaños:"), gbc);

        String[] months = { "Todos", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre" };
        m_cbMonthFilter = new JComboBox<>(months);
        m_cbMonthFilter.setBackground(Color.WHITE);
        m_cbMonthFilter.setFont(new Font("Arial", Font.PLAIN, 12));
        m_cbMonthFilter.addActionListener(e -> applyFilters());
        gbc.gridx = 3;
        gbc.weightx = 0;
        searchBarPanel.add(m_cbMonthFilter, gbc);

        // Add Employee Button
        JButton btnAdd = new JButton("Nuevo Colaborador");
        btnAdd.setFont(new Font("Arial", Font.BOLD, 12));
        btnAdd.setBackground(COLOR_PRIMARY);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.addActionListener(e -> showAddEditDialog(null));
        gbc.gridx = 4;
        gbc.weightx = 0;
        searchBarPanel.add(btnAdd, gbc);

        // Import Button
        JButton btnImport = new JButton("Importar");
        btnImport.setFont(new Font("Arial", Font.BOLD, 12));
        btnImport.setBackground(new Color(240, 240, 240));
        btnImport.addActionListener(e -> importCSV());
        gbc.gridx = 5;
        gbc.weightx = 0;
        searchBarPanel.add(btnImport, gbc);

        // Export Button
        JButton btnExport = new JButton("Exportar");
        btnExport.setFont(new Font("Arial", Font.BOLD, 12));
        btnExport.setBackground(new Color(240, 240, 240));
        btnExport.addActionListener(e -> exportCSV());
        gbc.gridx = 6;
        gbc.weightx = 0;
        searchBarPanel.add(btnExport, gbc);

        // Filler panel to absorb remaining space and keep the search controls compact
        // to the left
        gbc.gridx = 7;
        gbc.weightx = 1.0;
        searchBarPanel.add(new JLabel(), gbc);

        mainContentPanel.add(searchBarPanel, BorderLayout.NORTH);

        // Table
        String[] columns = { "ID", "Nombre", "Teléfono", "Email", "Dirección", "Colonia", "Cumpleaños", "Película",
                "Libro", "Ingreso", "Cargo", "Notas" };
        m_TableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        m_Table = new JTable(m_TableModel);
        m_Table.setRowHeight(28);
        m_Table.setFont(new Font("Arial", Font.PLAIN, 12));
        m_Table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_Table.setBackground(Color.WHITE);
        m_Table.setShowGrid(true);
        m_Table.setGridColor(new Color(230, 230, 230));

        // Ocultar la columna ID para que no se muestre al usuario pero siga en el
        // modelo
        m_Table.removeColumn(m_Table.getColumn("ID"));

        // Al hacer doble clic sobre una celda, mostrar la información completa avanzada
        m_Table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    EmployeeData selected = getSelectedEmployee();
                    if (selected != null) {
                        showDetailsDialog(selected);
                    }
                }
            }
        });

        JTableHeader tableHeader = m_Table.getTableHeader();
        tableHeader.setFont(new Font("Arial", Font.BOLD, 12));
        tableHeader.setBackground(new Color(240, 240, 240));
        tableHeader.setForeground(COLOR_DARK_TEXT);

        // Center align table headers
        ((DefaultTableCellRenderer) tableHeader.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        JScrollPane spTable = new JScrollPane(m_Table);
        spTable.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        mainContentPanel.add(spTable, BorderLayout.CENTER);

        // Actions Panel (Bottom)
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setBackground(Color.WHITE);

        // Botón para ver ficha/detalle completo
        JButton btnView = new JButton("Ver Detalle");
        btnView.setFont(new Font("Arial", Font.BOLD, 12));
        btnView.setBackground(new Color(225, 240, 255));
        btnView.addActionListener(e -> {
            EmployeeData selected = getSelectedEmployee();
            if (selected != null) {
                showDetailsDialog(selected);
            }
        });
        actionsPanel.add(btnView);

        JButton btnEdit = new JButton("Modificar");
        btnEdit.setFont(new Font("Arial", Font.BOLD, 12));
        btnEdit.setBackground(new Color(240, 240, 240));
        btnEdit.addActionListener(e -> {
            EmployeeData selected = getSelectedEmployee();
            if (selected != null) {
                showAddEditDialog(selected);
            }
        });
        actionsPanel.add(btnEdit);

        JButton btnDelete = new JButton("Eliminar");
        btnDelete.setFont(new Font("Arial", Font.BOLD, 12));
        btnDelete.setBackground(new Color(255, 235, 235));
        btnDelete.setForeground(new Color(200, 0, 0));
        btnDelete.addActionListener(e -> {
            EmployeeData selected = getSelectedEmployee();
            if (selected != null) {
                deleteEmployee(selected);
            }
        });
        actionsPanel.add(btnDelete);

        mainContentPanel.add(actionsPanel, BorderLayout.SOUTH);

        // Assemble UI: Search and Table (mainContentPanel) in the center, and single
        // birthdays panel at the top
        JPanel centerContainer = new JPanel(new BorderLayout(0, 10));
        centerContainer.setBackground(Color.WHITE);

        int baseFontSize = getFont() != null ? getFont().getSize() : 12;
        int upcomingHeight = Math.max(90, Math.min(130, (int) (baseFontSize * 8.5))); // Limitar alto entre 90px y 130px
        int upcomingWidth = Math.max(600, Math.min(800, (int) (baseFontSize * 55))); // Limitar ancho entre 600px y
                                                                                     // 800px

        upcomingPanel.setPreferredSize(new Dimension(upcomingWidth, upcomingHeight));
        upcomingPanel.setMinimumSize(new Dimension(upcomingWidth, upcomingHeight));
        upcomingPanel.setMaximumSize(new Dimension(upcomingWidth, upcomingHeight));

        JPanel upcomingWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        upcomingWrapper.setBackground(Color.WHITE);
        upcomingWrapper.add(upcomingPanel);

        centerContainer.add(upcomingWrapper, BorderLayout.NORTH);
        centerContainer.add(mainContentPanel, BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);
    }

    private EmployeeData getSelectedEmployee() {
        int row = m_Table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un colaborador de la lista primero.",
                    "Colaborador no seleccionado", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        // Map table view index to list items
        String id = (String) m_TableModel.getValueAt(row, 0);
        for (EmployeeData emp : m_EmployeesList) {
            if (emp.id.equals(id)) {
                return emp;
            }
        }
        return null;
    }

    // --- DATABASE LOAD AND MAPPING ---

    private void ensureLocalTableExists() {
        if (m_Connection == null) {
            try {
                m_Connection = m_App.getSession().getConnection();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Could not acquire database connection for schema check", ex);
                return;
            }
        }

        try (java.sql.Statement stmt = m_Connection.createStatement()) {
            // Verificar si la tabla colaboradores ya existe
            try {
                stmt.executeQuery("SELECT count(*) FROM colaboradores");
                LOGGER.info("La tabla colaboradores ya existe.");
            } catch (SQLException e) {
                // Si falla es porque no existe la tabla, entonces la creamos
                LOGGER.info("Creando la tabla colaboradores...");
                stmt.execute("CREATE TABLE colaboradores (" +
                        "ID VARCHAR(255) PRIMARY KEY," +
                        "NAME VARCHAR(255) NOT NULL," +
                        "PHONE VARCHAR(255)," +
                        "EMAIL VARCHAR(255)," +
                        "ADDRESS VARCHAR(255)," +
                        "COLONIA VARCHAR(255)," +
                        "BIRTHDAY TIMESTAMP," +
                        "FAVORITE_MOVIE VARCHAR(255)," +
                        "FAVORITE_BOOK VARCHAR(255)," +
                        "HIRE_DATE TIMESTAMP," +
                        "JOB_TITLE VARCHAR(255)," +
                        "NOTES VARCHAR(1000)" +
                        ")");
                LOGGER.info("Tabla colaboradores creada exitosamente.");
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al verificar o crear la tabla colaboradores", ex);
        }
    }

    private void loadData() {
        loadData(false);
    }

    private void loadData(boolean isManual) {
        if (isManual) {
            syncFromFirebase(true);
        } else {
            loadLocalDataOnly();
            syncFromFirebase(false);
        }
    }

    private void loadLocalDataOnly() {
        m_EmployeesList.clear();
        m_UpcomingListModel.clear();
        m_TableModel.setRowCount(0);

        if (m_Connection == null) {
            try {
                m_Connection = m_App.getSession().getConnection();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Could not acquire database connection", ex);
                return;
            }
        }

        ensureLocalTableExists();

        try {
            String query = "SELECT ID, NAME, PHONE, EMAIL, ADDRESS, COLONIA, BIRTHDAY, FAVORITE_MOVIE, FAVORITE_BOOK, HIRE_DATE, JOB_TITLE, NOTES FROM colaboradores ORDER BY NAME";
            try (PreparedStatement pstmt = m_Connection.prepareStatement(query)) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        EmployeeData emp = new EmployeeData();
                        emp.id = rs.getString("ID");
                        emp.name = rs.getString("NAME");
                        emp.phone = rs.getString("PHONE") != null ? rs.getString("PHONE") : "";
                        emp.email = rs.getString("EMAIL") != null ? rs.getString("EMAIL") : "";
                        emp.address = rs.getString("ADDRESS") != null ? rs.getString("ADDRESS") : "";
                        emp.colonia = rs.getString("COLONIA") != null ? rs.getString("COLONIA") : "";
                        emp.birthday = rs.getTimestamp("BIRTHDAY");
                        emp.favoriteMovie = rs.getString("FAVORITE_MOVIE") != null ? rs.getString("FAVORITE_MOVIE")
                                : "";
                        emp.favoriteBook = rs.getString("FAVORITE_BOOK") != null ? rs.getString("FAVORITE_BOOK") : "";
                        emp.hireDate = rs.getTimestamp("HIRE_DATE");
                        emp.jobTitle = rs.getString("JOB_TITLE") != null ? rs.getString("JOB_TITLE") : "";
                        emp.notes = rs.getString("NOTES") != null ? rs.getString("NOTES") : "";

                        m_EmployeesList.add(emp);
                    }
                }
            }

            processBirthdays();
            applyFilters();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error reading database", e);
            JOptionPane.showMessageDialog(this, "Error leyendo la base de datos local: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void processBirthdays() {
        Calendar today = Calendar.getInstance();
        int todayMonth = today.get(Calendar.MONTH); // 0-11
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        m_UpcomingListModel.clear();

        List<UpcomingInfo> list = new ArrayList<>();

        for (EmployeeData emp : m_EmployeesList) {
            if (emp.birthday == null)
                continue;

            Calendar bday = Calendar.getInstance();
            bday.setTimeInMillis(emp.birthday.getTime());
            int bdayMonth = bday.get(Calendar.MONTH);
            int bdayDay = bday.get(Calendar.DAY_OF_MONTH);

            // Is today?
            if (todayMonth == bdayMonth && todayDay == bdayDay) {
                UpcomingInfo info = new UpcomingInfo();
                info.emp = emp;
                info.daysLeft = 0; // 0 significa hoy
                info.monthName = getMonthName(bdayMonth);
                info.day = bdayDay;
                list.add(info);
            } else {
                // Check if in the next 15 days
                Calendar bdayThisYear = Calendar.getInstance();
                bdayThisYear.set(Calendar.MONTH, bdayMonth);
                bdayThisYear.set(Calendar.DAY_OF_MONTH, bdayDay);
                bdayThisYear.set(Calendar.HOUR_OF_DAY, 0);
                bdayThisYear.set(Calendar.MINUTE, 0);
                bdayThisYear.set(Calendar.SECOND, 0);
                bdayThisYear.set(Calendar.MILLISECOND, 0);

                Calendar todayTrunc = Calendar.getInstance();
                todayTrunc.set(Calendar.HOUR_OF_DAY, 0);
                todayTrunc.set(Calendar.MINUTE, 0);
                todayTrunc.set(Calendar.SECOND, 0);
                todayTrunc.set(Calendar.MILLISECOND, 0);

                if (bdayThisYear.before(todayTrunc)) {
                    bdayThisYear.add(Calendar.YEAR, 1);
                }

                long diffMs = bdayThisYear.getTimeInMillis() - todayTrunc.getTimeInMillis();
                long diffDays = diffMs / (24 * 60 * 60 * 1000);

                if (diffDays > 0 && diffDays <= 15) {
                    UpcomingInfo info = new UpcomingInfo();
                    info.emp = emp;
                    info.daysLeft = (int) diffDays;
                    info.monthName = getMonthName(bdayMonth);
                    info.day = bdayDay;
                    list.add(info);
                }
            }
        }

        // Sort birthdays by days left (today first, then 1, 2...)
        Collections.sort(list, (o1, o2) -> Integer.compare(o1.daysLeft, o2.daysLeft));

        for (UpcomingInfo ui : list) {
            String role = ui.emp.jobTitle.isEmpty() ? "Colaborador" : ui.emp.jobTitle;
            if (ui.daysLeft == 0) {
                m_UpcomingListModel
                        .addElement("🎉 [CUMPLE HOY] " + ui.emp.name + " (" + role + ") 🎉 ¡Felicítalo hoy! 🎂");
            } else {
                m_UpcomingListModel.addElement(ui.emp.name + " (" + role + ") - " + ui.day + " de " + ui.monthName
                        + " (en " + ui.daysLeft + " días)");
            }
        }

        if (m_UpcomingListModel.isEmpty()) {
            m_UpcomingListModel.addElement("No hay cumpleaños registrados para hoy ni en los siguientes 15 días.");
        }
    }

    private boolean isBirthdayToday(Timestamp birthday) {
        if (birthday == null)
            return false;
        Calendar today = Calendar.getInstance();
        Calendar bday = Calendar.getInstance();
        bday.setTimeInMillis(birthday.getTime());
        return today.get(Calendar.MONTH) == bday.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) == bday.get(Calendar.DAY_OF_MONTH);
    }

    private void applyFilters() {
        m_TableModel.setRowCount(0);
        String search = m_txtSearch.getText().trim().toLowerCase();
        int monthIndex = m_cbMonthFilter.getSelectedIndex(); // 0 is Todos, 1 is Enero (which matches
                                                             // Calendar.JANUARY=0)

        for (EmployeeData emp : m_EmployeesList) {
            // Search text filter
            boolean matchesSearch = search.isEmpty() ||
                    emp.name.toLowerCase().contains(search) ||
                    emp.phone.toLowerCase().contains(search) ||
                    emp.email.toLowerCase().contains(search) ||
                    emp.address.toLowerCase().contains(search) ||
                    emp.colonia.toLowerCase().contains(search) ||
                    emp.jobTitle.toLowerCase().contains(search) ||
                    emp.notes.toLowerCase().contains(search);

            // Month filter
            boolean matchesMonth = true;
            if (monthIndex > 0) {
                if (emp.birthday == null) {
                    matchesMonth = false;
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(emp.birthday.getTime());
                    int bdayMonth = cal.get(Calendar.MONTH) + 1; // 1-12
                    matchesMonth = (bdayMonth == monthIndex);
                }
            }

            if (matchesSearch && matchesMonth) {
                String bdayText = "";
                if (emp.birthday != null) {
                    if (isBirthdayToday(emp.birthday)) {
                        bdayText = "🎉 CUMPLE HOY (" + DATE_FORMAT.format(emp.birthday) + ") 🎂";
                    } else {
                        bdayText = DATE_FORMAT.format(emp.birthday);
                    }
                }

                m_TableModel.addRow(new Object[] {
                        emp.id,
                        emp.name,
                        emp.phone,
                        emp.email,
                        emp.address,
                        emp.colonia,
                        bdayText,
                        emp.favoriteMovie,
                        emp.favoriteBook,
                        emp.hireDate != null ? DATE_FORMAT.format(emp.hireDate) : "",
                        emp.jobTitle,
                        emp.notes
                });
            }
        }
    }

    // --- SYNC TO SUPABASE ---

    private void syncToFirebaseAsync(EmployeeData emp, boolean delete) {
        new Thread(() -> {
            try {
                FirebaseServiceREST client = FirebaseServiceREST.getInstance();
                if (!client.isInitialized()) {
                    client.initialize(m_App.getProperties());
                }
                if (!client.isInitialized()) {
                    LOGGER.warning("Could not initialize Firebase connection for background sync");
                    return;
                }

                if (delete) {
                    client.deleteDocument("empleados", emp.id);
                } else {
                    Map<String, Object> record = new LinkedHashMap<>();
                    record.put("id", emp.id);
                    record.put("nombre", emp.name);
                    record.put("tarjeta", emp.id); // Default card to employee id/UUID
                    record.put("visible", true);
                    record.put("rol", "3"); // default employee role
                    record.put("telefono", emp.phone);
                    record.put("email", emp.email);
                    record.put("direccion", emp.address);
                    record.put("colonia", emp.colonia);
                    record.put("cumpleanos", emp.birthday != null ? new Timestamp(emp.birthday.getTime()) : null);
                    record.put("pelicula", emp.favoriteMovie);
                    record.put("libro", emp.favoriteBook);
                    record.put("fechaInscripcion", emp.hireDate != null ? new Timestamp(emp.hireDate.getTime()) : null);
                    record.put("empleo", emp.jobTitle);
                    record.put("notas", emp.notes);

                    client.syncEmployees(Collections.singletonList(record)).join();
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Firebase synchronization failed", ex);
            }
        }).start();
    }

    private void syncFromFirebase() {
        // Run once on load to merge remote updates
        new Thread(() -> {
            try {
                FirebaseServiceREST client = FirebaseServiceREST.getInstance();
                if (!client.isInitialized()) {
                    client.initialize(m_App.getProperties());
                }
                if (!client.isInitialized())
                    return;

                List<Map<String, Object>> remoteUsers = client.downloadEmployees().join();
                if (remoteUsers == null || remoteUsers.isEmpty())
                    return;

                Connection conn = m_App.getSession().getConnection();

                // Cargar nombres locales existentes en la tabla colaboradores para evitar duplicados
                Map<String, String> existingNameToIdMap = new HashMap<>();
                try (java.sql.Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT ID, NAME FROM colaboradores")) {
                    while (rs.next()) {
                        String localId = rs.getString("ID");
                        String localName = rs.getString("NAME");
                        if (localName != null) {
                            existingNameToIdMap.put(localName.toLowerCase().trim(), localId);
                        }
                    }
                }

                String updateSql = "UPDATE colaboradores SET NAME=?, PHONE=?, EMAIL=?, ADDRESS=?, COLONIA=?, BIRTHDAY=?, FAVORITE_MOVIE=?, FAVORITE_BOOK=?, HIRE_DATE=?, JOB_TITLE=?, NOTES=? WHERE ID=?";
                String insertSql = "INSERT INTO colaboradores (ID, NAME, PHONE, EMAIL, ADDRESS, COLONIA, BIRTHDAY, FAVORITE_MOVIE, FAVORITE_BOOK, HIRE_DATE, JOB_TITLE, NOTES) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                        PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                        PreparedStatement delPeople = conn.prepareStatement("DELETE FROM people WHERE ID = ?")) {

                    for (Map<String, Object> u : remoteUsers) {
                        String id = asString(u.get("id"));
                        if (id == null)
                            continue;

                        String name = asString(u.get("nombre"));
                        if (name == null)
                            name = asString(u.get("name"));
                        if (name == null)
                            name = "Colaborador";

                        // Asegurar unicidad del nombre
                        String originalName = name;
                        String lowercaseName = name.toLowerCase().trim();
                        if (existingNameToIdMap.containsKey(lowercaseName)) {
                            String associatedId = existingNameToIdMap.get(lowercaseName);
                            if (!associatedId.equals(id)) {
                                int suffix = 1;
                                String candidateName = originalName + " (" + suffix + ")";
                                while (existingNameToIdMap.containsKey(candidateName.toLowerCase().trim())) {
                                    suffix++;
                                    candidateName = originalName + " (" + suffix + ")";
                                }
                                name = candidateName;
                                lowercaseName = name.toLowerCase().trim();
                            }
                        }
                        existingNameToIdMap.put(lowercaseName, id);

                        String phone = asString(u.get("telefono"));
                        String email = asString(u.get("email"));
                        String address = asString(u.get("direccion"));
                        String colonia = asString(u.get("colonia"));
                        Timestamp birthday = asTimestamp(u.get("cumpleanos"));
                        String movie = asString(u.get("pelicula"));
                        String book = asString(u.get("libro"));
                        Timestamp hireDate = asTimestamp(u.get("fechaInscripcion"));
                        if (hireDate == null)
                            hireDate = asTimestamp(u.get("fecha_inscripcion"));
                        String jobTitle = asString(u.get("empleo"));
                        String notes = asString(u.get("notas"));

                        // Limpiar de 'people' para que no salga en la pantalla de Usuarios, evitando borrar usuarios del sistema POS
                        if (!isSystemOrActivePosUser(id, conn)) {
                            delPeople.setString(1, id);
                            delPeople.executeUpdate();
                        }

                        // Try updating local db in colaboradores
                        updateStmt.setString(1, name);
                        updateStmt.setString(2, phone);
                        updateStmt.setString(3, email);
                        updateStmt.setString(4, address);
                        updateStmt.setString(5, colonia);
                        updateStmt.setTimestamp(6, birthday);
                        updateStmt.setString(7, movie);
                        updateStmt.setString(8, book);
                        updateStmt.setTimestamp(9, hireDate);
                        updateStmt.setString(10, jobTitle);
                        updateStmt.setString(11, notes);
                        updateStmt.setString(12, id);

                        int affected = updateStmt.executeUpdate();
                        if (affected == 0) {
                            // Insert
                            insertStmt.setString(1, id);
                            insertStmt.setString(2, name);
                            insertStmt.setString(3, phone);
                            insertStmt.setString(4, email);
                            insertStmt.setString(5, address);
                            insertStmt.setString(6, colonia);
                            insertStmt.setTimestamp(7, birthday);
                            insertStmt.setString(8, movie);
                            insertStmt.setString(9, book);
                            insertStmt.setTimestamp(10, hireDate);
                            insertStmt.setString(11, jobTitle);
                            insertStmt.setString(12, notes);

                            insertStmt.executeUpdate();
                        }
                    }
                }

                // Reload data locally in UI thread once synced
                SwingUtilities.invokeLater(this::reloadLocalOnly);

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error syncing from Firebase", ex);
            }
        }).start();
    }

    private void syncFromFirebase(boolean isManual) {
        if (isManual) {
            // Show modal loading dialog
            Window owner = SwingUtilities.getWindowAncestor(this);
            JDialog progressDialog = new JDialog(owner, "Sincronización", Dialog.ModalityType.APPLICATION_MODAL);
            progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            JPanel panel = new JPanel(new BorderLayout(15, 15));
            panel.setBorder(new EmptyBorder(25, 25, 25, 25));

            JLabel label = new JLabel("Conectando y descargando colaboradores desde el servidor...");
            label.setFont(new Font("Arial", Font.BOLD, 12));
            panel.add(label, BorderLayout.NORTH);

            JProgressBar bar = new JProgressBar();
            bar.setIndeterminate(true);
            panel.add(bar, BorderLayout.CENTER);

            progressDialog.add(panel);
            progressDialog.pack();
            progressDialog.setLocationRelativeTo(this);

            // Run sync in a thread
            new Thread(() -> {
                String errorMessage = null;
                try {
                    FirebaseServiceREST client = FirebaseServiceREST.getInstance();
                    if (!client.isInitialized()) {
                        boolean initSuccess = client.initialize(m_App.getProperties());
                        if (!initSuccess) {
                            errorMessage = "No se pudo inicializar el servicio de Firebase. Verifique sus credenciales y configuración.";
                        }
                    }

                    if (errorMessage == null) {
                        // Test connection first
                        boolean isConnected = client.testConnection().join();
                        if (!isConnected) {
                            errorMessage = "No se pudo establecer conexión con el servidor. Verifique su conexión a Internet.";
                        }
                    }

                    if (errorMessage == null) {
                        List<Map<String, Object>> remoteUsers = client.downloadEmployees().join();
                        if (remoteUsers == null) {
                            errorMessage = "La descarga de colaboradores falló o devolvió un resultado nulo.";
                        } else {
                            Connection conn = m_App.getSession().getConnection();

                            // Cargar nombres locales existentes en la tabla colaboradores para evitar duplicados
                            Map<String, String> existingNameToIdMap = new HashMap<>();
                            try (java.sql.Statement stmt = conn.createStatement();
                                    ResultSet rs = stmt.executeQuery("SELECT ID, NAME FROM colaboradores")) {
                                while (rs.next()) {
                                    String localId = rs.getString("ID");
                                    String localName = rs.getString("NAME");
                                    if (localName != null) {
                                        existingNameToIdMap.put(localName.toLowerCase().trim(), localId);
                                    }
                                }
                            }

                            String updateSql = "UPDATE colaboradores SET NAME=?, PHONE=?, EMAIL=?, ADDRESS=?, COLONIA=?, BIRTHDAY=?, FAVORITE_MOVIE=?, FAVORITE_BOOK=?, HIRE_DATE=?, JOB_TITLE=?, NOTES=? WHERE ID=?";
                            String insertSql = "INSERT INTO colaboradores (ID, NAME, PHONE, EMAIL, ADDRESS, COLONIA, BIRTHDAY, FAVORITE_MOVIE, FAVORITE_BOOK, HIRE_DATE, JOB_TITLE, NOTES) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                            int updatedCount = 0;
                            int insertedCount = 0;

                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                                    PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                                    PreparedStatement delPeople = conn.prepareStatement("DELETE FROM people WHERE ID = ?")) {

                                for (Map<String, Object> u : remoteUsers) {
                                    String id = asString(u.get("id"));
                                    if (id == null)
                                        continue;

                                    String name = asString(u.get("nombre"));
                                    if (name == null)
                                        name = asString(u.get("name"));
                                    if (name == null)
                                        name = "Colaborador";

                                    // Asegurar unicidad del nombre
                                    String originalName = name;
                                    String lowercaseName = name.toLowerCase().trim();
                                    if (existingNameToIdMap.containsKey(lowercaseName)) {
                                        String associatedId = existingNameToIdMap.get(lowercaseName);
                                        if (!associatedId.equals(id)) {
                                            int suffix = 1;
                                            String candidateName = originalName + " (" + suffix + ")";
                                            while (existingNameToIdMap
                                                    .containsKey(candidateName.toLowerCase().trim())) {
                                                suffix++;
                                                candidateName = originalName + " (" + suffix + ")";
                                            }
                                            name = candidateName;
                                            lowercaseName = name.toLowerCase().trim();
                                        }
                                    }
                                    existingNameToIdMap.put(lowercaseName, id);

                                    String phone = asString(u.get("telefono"));
                                    String email = asString(u.get("email"));
                                    String address = asString(u.get("direccion"));
                                    String colonia = asString(u.get("colonia"));
                                    Timestamp birthday = asTimestamp(u.get("cumpleanos"));
                                    String movie = asString(u.get("pelicula"));
                                    String book = asString(u.get("libro"));
                                    Timestamp hireDate = asTimestamp(u.get("fechaInscripcion"));
                                    if (hireDate == null)
                                        hireDate = asTimestamp(u.get("fecha_inscripcion"));
                                    String jobTitle = asString(u.get("empleo"));
                                    String notes = asString(u.get("notes"));

                                    // Limpiar de 'people' para que no salga en la pantalla de Usuarios, evitando borrar usuarios del sistema POS
                                    if (!isSystemOrActivePosUser(id, conn)) {
                                        delPeople.setString(1, id);
                                        delPeople.executeUpdate();
                                    }

                                    // Try updating local db in colaboradores
                                    updateStmt.setString(1, name);
                                    updateStmt.setString(2, phone);
                                    updateStmt.setString(3, email);
                                    updateStmt.setString(4, address);
                                    updateStmt.setString(5, colonia);
                                    updateStmt.setTimestamp(6, birthday);
                                    updateStmt.setString(7, movie);
                                    updateStmt.setString(8, book);
                                    updateStmt.setTimestamp(9, hireDate);
                                    updateStmt.setString(10, jobTitle);
                                    updateStmt.setString(11, notes);
                                    updateStmt.setString(12, id);

                                    int affected = updateStmt.executeUpdate();
                                    if (affected == 0) {
                                        // Insert
                                        insertStmt.setString(1, id);
                                        insertStmt.setString(2, name);
                                        insertStmt.setString(3, phone);
                                        insertStmt.setString(4, email);
                                        insertStmt.setString(5, address);
                                        insertStmt.setString(6, colonia);
                                        insertStmt.setTimestamp(7, birthday);
                                        insertStmt.setString(8, movie);
                                        insertStmt.setString(9, book);
                                        insertStmt.setTimestamp(10, hireDate);
                                        insertStmt.setString(11, jobTitle);
                                        insertStmt.setString(12, notes);

                                        insertStmt.executeUpdate();
                                        insertedCount++;
                                    } else {
                                        updatedCount++;
                                    }
                                }
                            }

                            final int finalUpdated = updatedCount;
                            final int finalInserted = insertedCount;
                            final int totalDownloaded = remoteUsers.size();

                            // Close dialog and show success message
                            SwingUtilities.invokeLater(() -> {
                                progressDialog.dispose();
                                reloadLocalOnly();
                                JOptionPane.showMessageDialog(BirthdaysPanel.this,
                                        "Sincronización exitosa.\n" +
                                                "- Colaboradores descargados: " + totalDownloaded + "\n" +
                                                "- Nuevos registrados localmente: " + finalInserted + "\n" +
                                                "- Actualizados localmente: " + finalUpdated,
                                        "Sincronización completada",
                                        JOptionPane.INFORMATION_MESSAGE);
                            });
                            return; // success exit
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error en la sincronización manual", ex);
                    errorMessage = ex.getMessage();
                }

                // If we get here, an error occurred
                final String finalError = errorMessage != null ? errorMessage
                        : "Ocurrió un error desconocido durante la sincronización.";
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    reloadLocalOnly();
                    JOptionPane.showMessageDialog(BirthdaysPanel.this,
                            "Error al sincronizar con el servidor:\n" + finalError,
                            "Error de sincronización",
                            JOptionPane.ERROR_MESSAGE);
                });
            }).start();

            // Show modal dialog (this blocks the EDT until dispose() is called)
            progressDialog.setVisible(true);

        } else {
            // Keep background non-blocking sync for automatic startup load
            syncFromFirebase();
        }
    }

    private void reloadLocalOnly() {
        m_EmployeesList.clear();
        m_UpcomingListModel.clear();
        m_TableModel.setRowCount(0);

        try {
            String query = "SELECT ID, NAME, PHONE, EMAIL, ADDRESS, COLONIA, BIRTHDAY, FAVORITE_MOVIE, FAVORITE_BOOK, HIRE_DATE, JOB_TITLE, NOTES FROM colaboradores ORDER BY NAME";
            try (PreparedStatement pstmt = m_Connection.prepareStatement(query)) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        EmployeeData emp = new EmployeeData();
                        emp.id = rs.getString("ID");
                        emp.name = rs.getString("NAME");
                        emp.phone = rs.getString("PHONE") != null ? rs.getString("PHONE") : "";
                        emp.email = rs.getString("EMAIL") != null ? rs.getString("EMAIL") : "";
                        emp.address = rs.getString("ADDRESS") != null ? rs.getString("ADDRESS") : "";
                        emp.colonia = rs.getString("COLONIA") != null ? rs.getString("COLONIA") : "";
                        emp.birthday = rs.getTimestamp("BIRTHDAY");
                        emp.favoriteMovie = rs.getString("FAVORITE_MOVIE") != null ? rs.getString("FAVORITE_MOVIE")
                                : "";
                        emp.favoriteBook = rs.getString("FAVORITE_BOOK") != null ? rs.getString("FAVORITE_BOOK") : "";
                        emp.hireDate = rs.getTimestamp("HIRE_DATE");
                        emp.jobTitle = rs.getString("JOB_TITLE") != null ? rs.getString("JOB_TITLE") : "";
                        emp.notes = rs.getString("NOTES") != null ? rs.getString("NOTES") : "";

                        m_EmployeesList.add(emp);
                    }
                }
            }
            processBirthdays();
            applyFilters();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error loading local data", ex);
        }
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Timestamp asTimestamp(Object o) {
        if (o == null)
            return null;
        try {
            if (o instanceof Number) {
                return new Timestamp(((Number) o).longValue());
            }
            String s = String.valueOf(o);
            if (s.contains("T")) {
                try {
                    return Timestamp.from(java.time.Instant.parse(s));
                } catch (Exception ex) {
                    SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    return new Timestamp(iso.parse(s).getTime());
                }
            } else {
                SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");
                return new Timestamp(ymd.parse(s).getTime());
            }
        } catch (Exception e) {
            return null;
        }
    }

    // --- ADD / EDIT / DELETE ACTIONS ---

    private void showAddEditDialog(EmployeeData employee) {
        final boolean isEdit = (employee != null);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEdit ? "Editar Colaborador" : "Nuevo Colaborador", true);
        dialog.setLayout(new BorderLayout());

        int width = 980;
        int height = 620;
        try {
            GraphicsConfiguration gc = dialog.getGraphicsConfiguration();
            if (gc != null) {
                Rectangle bounds = gc.getBounds();
                Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
                int maxValWidth = bounds.width - insets.left - insets.right - 20;
                int maxValHeight = bounds.height - insets.top - insets.bottom - 40;
                width = Math.min(width, maxValWidth);
                height = Math.min(height, maxValHeight);
            }
        } catch (Exception ex) {
            try {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                width = Math.min(width, screenSize.width - 40);
                height = Math.min(height, screenSize.height - 80);
            } catch (Exception e) {
                // Ignorar
            }
        }
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtName = new JTextField(15);
        JTextField txtPhone = new JTextField(15);
        JTextField txtEmail = new JTextField(15);
        JTextField txtAddress = new JTextField(15);
        JTextField txtColonia = new JTextField(15);
        JTextField txtBirthday = new JTextField(12); // yyyy-mm-dd
        JTextField txtMovie = new JTextField(15);
        JTextField txtBook = new JTextField(15);
        JTextField txtHireDate = new JTextField(12); // yyyy-mm-dd
        JTextField txtJobTitle = new JTextField(15);
        JTextArea txtNotes = new JTextArea(3, 15);
        txtNotes.setLineWrap(true);
        txtNotes.setWrapStyleWord(true);
        JScrollPane spNotes = new JScrollPane(txtNotes);

        // Date picker buttons
        JButton btnBirthdayPicker = new JButton("📅");
        btnBirthdayPicker.setFont(new Font("Arial", Font.PLAIN, 12));
        btnBirthdayPicker.setBackground(new Color(240, 240, 240));
        btnBirthdayPicker.addActionListener(e -> {
            Date current = null;
            try {
                if (!txtBirthday.getText().trim().isEmpty()) {
                    current = DATE_FORMAT.parse(txtBirthday.getText().trim());
                }
            } catch (Exception ex) {
                // Ignore parse errors
            }
            Date selected = JCalendarDialog.showCalendar(dialog, current);
            if (selected != null) {
                txtBirthday.setText(DATE_FORMAT.format(selected));
            }
        });

        JButton btnHireDatePicker = new JButton("📅");
        btnHireDatePicker.setFont(new Font("Arial", Font.PLAIN, 12));
        btnHireDatePicker.setBackground(new Color(240, 240, 240));
        btnHireDatePicker.addActionListener(e -> {
            Date current = null;
            try {
                if (!txtHireDate.getText().trim().isEmpty()) {
                    current = DATE_FORMAT.parse(txtHireDate.getText().trim());
                }
            } catch (Exception ex) {
                // Ignore parse errors
            }
            Date selected = JCalendarDialog.showCalendar(dialog, current);
            if (selected != null) {
                txtHireDate.setText(DATE_FORMAT.format(selected));
            }
        });

        if (isEdit) {
            txtName.setText(employee.name);
            txtPhone.setText(employee.phone);
            txtEmail.setText(employee.email);
            txtAddress.setText(employee.address);
            txtColonia.setText(employee.colonia);
            txtBirthday.setText(employee.birthday != null ? DATE_FORMAT.format(employee.birthday) : "");
            txtMovie.setText(employee.favoriteMovie);
            txtBook.setText(employee.favoriteBook);
            txtHireDate.setText(employee.hireDate != null ? DATE_FORMAT.format(employee.hireDate) : "");
            txtJobTitle.setText(employee.jobTitle);
            txtNotes.setText(employee.notes);
        } else {
            // Default hire date to today
            txtHireDate.setText(DATE_FORMAT.format(new Date()));
        }

        // Fila 0: Nombre (Izq) | Nacimiento (Der)
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel lblName = new JLabel("Nombre *:");
        lblName.setFont(new Font("Arial", Font.PLAIN, 12));
        formPanel.add(lblName, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        formPanel.add(txtName, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel lblBday = new JLabel("Nacimiento (AAAA-MM-DD) *:");
        lblBday.setFont(new Font("Arial", Font.PLAIN, 12));
        formPanel.add(lblBday, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        JPanel pBirthday = new JPanel(new BorderLayout(5, 0));
        pBirthday.setBackground(Color.WHITE);
        pBirthday.add(txtBirthday, BorderLayout.CENTER);
        pBirthday.add(btnBirthdayPicker, BorderLayout.EAST);
        formPanel.add(pBirthday, gbc);

        // Fila 1: Teléfono (Izq) | Ingreso (Der)
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel lblPhone = new JLabel("Teléfono:");
        lblPhone.setFont(new Font("Arial", Font.PLAIN, 12));
        formPanel.add(lblPhone, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        formPanel.add(txtPhone, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel lblHire = new JLabel("Ingreso (AAAA-MM-DD):");
        lblHire.setFont(new Font("Arial", Font.PLAIN, 12));
        formPanel.add(lblHire, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        JPanel pHireDate = new JPanel(new BorderLayout(5, 0));
        pHireDate.setBackground(Color.WHITE);
        pHireDate.add(txtHireDate, BorderLayout.CENTER);
        pHireDate.add(btnHireDatePicker, BorderLayout.EAST);
        formPanel.add(pHireDate, gbc);

        // Fila 2: Email (Izq) | Puesto/Cargo (Der)
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel lblEmail = new JLabel("Email:");
        lblEmail.setFont(new Font("Arial", Font.PLAIN, 12));
        formPanel.add(lblEmail, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        formPanel.add(txtEmail, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel lblJob = new JLabel("Puesto/Cargo:");
        lblJob.setFont(new Font("Arial", Font.PLAIN, 12));
        formPanel.add(lblJob, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        formPanel.add(txtJobTitle, gbc);

        // Fila 3: Dirección (Izq) | Película (Der)
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel lblAddr = new JLabel("Dirección:");
        lblAddr.setFont(new Font("Arial", Font.PLAIN, 12));
        formPanel.add(lblAddr, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        formPanel.add(txtAddress, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel lblMovie = new JLabel("Película Favorita:");
        lblMovie.setFont(new Font("Arial", Font.PLAIN, 12));
        formPanel.add(lblMovie, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        formPanel.add(txtMovie, gbc);

        // Fila 4: Colonia (Izq) | Libro (Der)
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel lblCol = new JLabel("Colonia:");
        lblCol.setFont(new Font("Arial", Font.PLAIN, 12));
        formPanel.add(lblCol, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        formPanel.add(txtColonia, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel lblBook = new JLabel("Libro Favorito:");
        lblBook.setFont(new Font("Arial", Font.PLAIN, 12));
        formPanel.add(lblBook, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        formPanel.add(txtBook, gbc);

        // Fila 5: Notas (Completo abajo)
        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        JLabel lblNotes = new JLabel("Notas:");
        lblNotes.setFont(new Font("Arial", Font.PLAIN, 12));
        formPanel.add(lblNotes, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 3;
        formPanel.add(spNotes, gbc);

        // Wrap the form panel in a JScrollPane to handle smaller screens and prevent
        // cutting off components
        JScrollPane scrollForm = new JScrollPane(formPanel);
        scrollForm.setBorder(null);
        scrollForm.getVerticalScrollBar().setUnitIncrement(16); // Smooth scrolling
        dialog.add(scrollForm, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(new Color(245, 245, 245));
        JButton btnCancel = new JButton("Cancelar");
        btnCancel.setFont(new Font("Arial", Font.BOLD, 12));
        btnCancel.setBackground(new Color(240, 240, 240));
        btnCancel.addActionListener(e -> dialog.dispose());
        buttonPanel.add(btnCancel);

        JButton btnSave = new JButton("Guardar");
        btnSave.setFont(new Font("Arial", Font.BOLD, 12));
        btnSave.setBackground(COLOR_PRIMARY);
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            String birthdayStr = txtBirthday.getText().trim();
            String hireDateStr = txtHireDate.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "El campo Nombre es obligatorio.", "Validación",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            Timestamp birthdayVal = null;
            if (!birthdayStr.isEmpty()) {
                try {
                    birthdayVal = new Timestamp(DATE_FORMAT.parse(birthdayStr).getTime());
                } catch (ParseException ex) {
                    JOptionPane.showMessageDialog(dialog,
                            "Fecha de nacimiento inválida. Use el formato AAAA-MM-DD (ej: 1995-08-15).", "Validación",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } else {
                JOptionPane.showMessageDialog(dialog,
                        "La Fecha de Nacimiento es obligatoria para la gestión de cumpleaños.", "Validación",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            Timestamp hireDateVal = null;
            if (!hireDateStr.isEmpty()) {
                try {
                    hireDateVal = new Timestamp(DATE_FORMAT.parse(hireDateStr).getTime());
                } catch (ParseException ex) {
                    JOptionPane.showMessageDialog(dialog, "Fecha de ingreso inválida. Use el formato AAAA-MM-DD.",
                            "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            // Save employee
            try {
                if (isEdit) {
                    employee.name = name;
                    employee.phone = txtPhone.getText().trim();
                    employee.email = txtEmail.getText().trim();
                    employee.address = txtAddress.getText().trim();
                    employee.colonia = txtColonia.getText().trim();
                    employee.birthday = birthdayVal;
                    employee.favoriteMovie = txtMovie.getText().trim();
                    employee.favoriteBook = txtBook.getText().trim();
                    employee.hireDate = hireDateVal;
                    employee.jobTitle = txtJobTitle.getText().trim();
                    employee.notes = txtNotes.getText().trim();

                    updateEmployeeInDb(employee);
                    syncToFirebaseAsync(employee, false);
                } else {
                    EmployeeData newEmp = new EmployeeData();
                    newEmp.id = UUID.randomUUID().toString();
                    newEmp.name = name;
                    newEmp.phone = txtPhone.getText().trim();
                    newEmp.email = txtEmail.getText().trim();
                    newEmp.address = txtAddress.getText().trim();
                    newEmp.colonia = txtColonia.getText().trim();
                    newEmp.birthday = birthdayVal;
                    newEmp.favoriteMovie = txtMovie.getText().trim();
                    newEmp.favoriteBook = txtBook.getText().trim();
                    newEmp.hireDate = hireDateVal;
                    newEmp.jobTitle = txtJobTitle.getText().trim();
                    newEmp.notes = txtNotes.getText().trim();

                    insertEmployeeToDb(newEmp);
                    syncToFirebaseAsync(newEmp, false);
                }
                loadData();
                dialog.dispose();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error saving to database", ex);
                JOptionPane.showMessageDialog(dialog, "Error al guardar en la base de datos: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(btnSave);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void addFormRow(JPanel panel, String label, JTextField field, GridBagConstraints gbc, int y) {
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(lbl, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private void addDateFormRow(JPanel panel, String label, JTextField field, JButton btnPicker, GridBagConstraints gbc,
            int y) {
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(lbl, gbc);

        JPanel pickerPanel = new JPanel(new BorderLayout(5, 0));
        pickerPanel.setBackground(Color.WHITE);
        pickerPanel.add(field, BorderLayout.CENTER);
        pickerPanel.add(btnPicker, BorderLayout.EAST);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(pickerPanel, gbc);
    }

    private void insertEmployeeToDb(EmployeeData emp) throws SQLException {
        String query = "INSERT INTO colaboradores (ID, NAME, PHONE, EMAIL, ADDRESS, COLONIA, BIRTHDAY, FAVORITE_MOVIE, FAVORITE_BOOK, HIRE_DATE, JOB_TITLE, NOTES) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = m_Connection.prepareStatement(query)) {
            pstmt.setString(1, emp.id);
            pstmt.setString(2, emp.name);
            pstmt.setString(3, emp.phone);
            pstmt.setString(4, emp.email);
            pstmt.setString(5, emp.address);
            pstmt.setString(6, emp.colonia);
            pstmt.setTimestamp(7, emp.birthday);
            pstmt.setString(8, emp.favoriteMovie);
            pstmt.setString(9, emp.favoriteBook);
            pstmt.setTimestamp(10, emp.hireDate);
            pstmt.setString(11, emp.jobTitle);
            pstmt.setString(12, emp.notes);

            pstmt.executeUpdate();
        }
        if (!isSystemOrActivePosUser(emp.id, m_Connection)) {
            try (PreparedStatement delPstmt = m_Connection.prepareStatement("DELETE FROM people WHERE ID = ?")) {
                delPstmt.setString(1, emp.id);
                delPstmt.executeUpdate();
            }
        }
    }

    private void updateEmployeeInDb(EmployeeData emp) throws SQLException {
        String query = "UPDATE colaboradores SET NAME=?, PHONE=?, EMAIL=?, ADDRESS=?, COLONIA=?, BIRTHDAY=?, FAVORITE_MOVIE=?, FAVORITE_BOOK=?, HIRE_DATE=?, JOB_TITLE=?, NOTES=? WHERE ID=?";
        try (PreparedStatement pstmt = m_Connection.prepareStatement(query)) {
            pstmt.setString(1, emp.name);
            pstmt.setString(2, emp.phone);
            pstmt.setString(3, emp.email);
            pstmt.setString(4, emp.address);
            pstmt.setString(5, emp.colonia);
            pstmt.setTimestamp(6, emp.birthday);
            pstmt.setString(7, emp.favoriteMovie);
            pstmt.setString(8, emp.favoriteBook);
            pstmt.setTimestamp(9, emp.hireDate);
            pstmt.setString(10, emp.jobTitle);
            pstmt.setString(11, emp.notes);
            pstmt.setString(12, emp.id);

            pstmt.executeUpdate();
        }
        if (!isSystemOrActivePosUser(emp.id, m_Connection)) {
            try (PreparedStatement delPstmt = m_Connection.prepareStatement("DELETE FROM people WHERE ID = ?")) {
                delPstmt.setString(1, emp.id);
                delPstmt.executeUpdate();
            }
        }
    }

    private void deleteEmployee(EmployeeData emp) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de que desea eliminar a " + emp.name
                        + "? Esta acción borrará al colaborador localmente y del servidor Firebase.",
                "Confirmar Eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String query = "DELETE FROM colaboradores WHERE ID = ?";
                try (PreparedStatement pstmt = m_Connection.prepareStatement(query)) {
                    pstmt.setString(1, emp.id);
                    pstmt.executeUpdate();
                }
                if (!isSystemOrActivePosUser(emp.id, m_Connection)) {
                    try (PreparedStatement delPstmt = m_Connection.prepareStatement("DELETE FROM people WHERE ID = ?")) {
                        delPstmt.setString(1, emp.id);
                        delPstmt.executeUpdate();
                    }
                }
                syncToFirebaseAsync(emp, true);
                loadData();
                JOptionPane.showMessageDialog(this, "Colaborador eliminado exitosamente.");
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error deleting employee", ex);
                JOptionPane.showMessageDialog(this, "Error al eliminar en la base de datos: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- WHATSAPP & EMAIL REDIRECTS ---

    private void openWhatsApp(EmployeeData emp) {
        if (emp.phone == null || emp.phone.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El colaborador no tiene un número de teléfono registrado.",
                    "Número vacío", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Clean phone number (leave digits only)
        String cleanPhone = emp.phone.replaceAll("[^0-9]", "");
        if (cleanPhone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El número de teléfono registrado no es válido.", "Número no válido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Add default country code if missing (Mexico default prefix is 52)
        if (cleanPhone.length() == 10) {
            cleanPhone = "52" + cleanPhone;
        }

        String template = "¡Feliz cumpleaños " + emp.name + "! Esperamos que tengas un día maravilloso.";
        try {
            String url = "https://web.whatsapp.com/send?phone=" + cleanPhone + "&text="
                    + URLEncoder.encode(template, "UTF-8");
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                JOptionPane.showMessageDialog(this,
                        "La navegación no está soportada en su sistema operativo. Link:\n" + url, "WhatsApp Link",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Could not launch browser", ex);
            JOptionPane.showMessageDialog(this, "Error al abrir el navegador: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openEmail(EmployeeData emp) {
        if (emp.email == null || emp.email.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El colaborador no tiene un correo electrónico registrado.",
                    "Email vacío", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String subject = "¡Feliz cumpleaños!";
        String body = "Hola " + emp.name
                + ",\n\nTe deseamos un feliz cumpleaños de parte de todo el equipo de Punto MX. ¡Que pases un día excelente!\n\nSaludos cordiales.";
        try {
            String mailto = "mailto:" + emp.email + "?subject="
                    + URLEncoder.encode(subject, "UTF-8").replace("+", "%20") +
                    "&body=" + URLEncoder.encode(body, "UTF-8").replace("+", "%20");
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(mailto));
            } else {
                JOptionPane.showMessageDialog(this, "La navegación no está soportada. Mailto Link:\n" + mailto,
                        "Mailto Link", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Could not launch email client", ex);
            JOptionPane.showMessageDialog(this, "Error al abrir el cliente de correo: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- HELPER CLASSES AND METHODS ---

    private String getMonthName(int month) {
        String[] months = { "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre" };
        if (month >= 0 && month < 12) {
            return months[month];
        }
        return "";
    }

    private void showDetailsDialog(EmployeeData emp) {
        if (emp == null)
            return;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Ficha del Colaborador", true);
        dialog.setLayout(new BorderLayout());

        int width = 550;
        int height = 600;
        try {
            GraphicsConfiguration gc = dialog.getGraphicsConfiguration();
            if (gc != null) {
                Rectangle bounds = gc.getBounds();
                Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
                int maxValWidth = bounds.width - insets.left - insets.right - 20;
                int maxValHeight = bounds.height - insets.top - insets.bottom - 40;
                width = Math.min(width, maxValWidth);
                height = Math.min(height, maxValHeight);
            }
        } catch (Exception ex) {
            try {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                width = Math.min(width, screenSize.width - 40);
                height = Math.min(height, screenSize.height - 80);
            } catch (Exception e) {
                // Ignorar
            }
        }
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(this);

        // Header Panel (styled)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_PRIMARY);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel nameLabel = new JLabel(emp.name);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 18));
        nameLabel.setForeground(Color.WHITE);
        headerPanel.add(nameLabel, BorderLayout.NORTH);

        JLabel roleLabel = new JLabel(emp.jobTitle.isEmpty() ? "Colaborador" : emp.jobTitle);
        roleLabel.setFont(new Font("Arial", Font.ITALIC, 13));
        roleLabel.setForeground(new Color(230, 250, 245));
        headerPanel.add(roleLabel, BorderLayout.SOUTH);

        dialog.add(headerPanel, BorderLayout.NORTH);

        // Details Panel (scrollable)
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int rowIdx = 0;
        addDetailRow(contentPanel, "Teléfono:", emp.phone, gbc, rowIdx++);
        addDetailRow(contentPanel, "Email:", emp.email, gbc, rowIdx++);
        addDetailRow(contentPanel, "Colonia:", emp.colonia, gbc, rowIdx++);
        addDetailRow(contentPanel, "Cumpleaños:",
                emp.birthday != null ? DATE_FORMAT.format(emp.birthday) : "No registrado", gbc, rowIdx++);
        addDetailRow(contentPanel, "Fecha Ingreso:",
                emp.hireDate != null ? DATE_FORMAT.format(emp.hireDate) : "No registrado", gbc, rowIdx++);
        addDetailRow(contentPanel, "Película Favorita:", emp.favoriteMovie, gbc, rowIdx++);
        addDetailRow(contentPanel, "Libro Favorito:", emp.favoriteBook, gbc, rowIdx++);

        // Multi-line fields (Address and Notes)
        addDetailArea(contentPanel, "Dirección:", emp.address, gbc, rowIdx++, 60);
        addDetailArea(contentPanel, "Notas / Comentarios:", emp.notes, gbc, rowIdx++, 100);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        dialog.add(scrollPane, BorderLayout.CENTER);

        // Close Button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        buttonPanel.setBackground(new Color(245, 245, 245));
        JButton btnClose = new JButton("Cerrar");
        btnClose.setFont(new Font("Arial", Font.BOLD, 12));
        btnClose.setBackground(COLOR_PRIMARY);
        btnClose.setForeground(Color.WHITE);
        btnClose.addActionListener(e -> dialog.dispose());
        buttonPanel.add(btnClose);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void addDetailRow(JPanel panel, String label, String value, GridBagConstraints gbc, int y) {
        gbc.gridy = y;

        gbc.gridx = 0;
        gbc.weightx = 0.3;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        lbl.setForeground(COLOR_MUTED_TEXT);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        JLabel val = new JLabel(value == null || value.trim().isEmpty() ? "—" : value);
        val.setFont(new Font("Arial", Font.PLAIN, 13));
        val.setForeground(COLOR_DARK_TEXT);
        panel.add(val, gbc);
    }

    private void addDetailArea(JPanel panel, String label, String value, GridBagConstraints gbc, int y, int height) {
        gbc.gridy = y;

        gbc.gridx = 0;
        gbc.weightx = 0.3;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        lbl.setForeground(COLOR_MUTED_TEXT);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        JTextArea area = new JTextArea(value == null || value.trim().isEmpty() ? "—" : value);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Arial", Font.PLAIN, 12));
        area.setForeground(COLOR_DARK_TEXT);
        area.setBackground(new Color(250, 250, 250));
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(300, height));
        scroll.setBorder(null);
        panel.add(scroll, gbc);
    }

    private void exportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar Colaboradores a CSV");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));

        int userSelection = chooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToSave = chooser.getSelectedFile();
        String path = fileToSave.getAbsolutePath();
        if (!path.toLowerCase().endsWith(".csv")) {
            fileToSave = new File(path + ".csv");
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(fileToSave), "UTF-8"))) {
            // Write BOM for Excel UTF-8 compatibility
            writer.write('\ufeff');

            // Header
            writer.write(
                    "ID,Nombre,Teléfono,Email,Dirección,Colonia,Cumpleaños,Película Favorita,Libro Favorito,Ingreso,Cargo,Notas");
            writer.newLine();

            for (EmployeeData emp : m_EmployeesList) {
                writer.write(escapeCSVValue(emp.id) + ",");
                writer.write(escapeCSVValue(emp.name) + ",");
                writer.write(escapeCSVValue(emp.phone) + ",");
                writer.write(escapeCSVValue(emp.email) + ",");
                writer.write(escapeCSVValue(emp.address) + ",");
                writer.write(escapeCSVValue(emp.colonia) + ",");
                writer.write(escapeCSVValue(emp.birthday != null ? DATE_FORMAT.format(emp.birthday) : "") + ",");
                writer.write(escapeCSVValue(emp.favoriteMovie) + ",");
                writer.write(escapeCSVValue(emp.favoriteBook) + ",");
                writer.write(escapeCSVValue(emp.hireDate != null ? DATE_FORMAT.format(emp.hireDate) : "") + ",");
                writer.write(escapeCSVValue(emp.jobTitle) + ",");
                writer.write(escapeCSVValue(emp.notes));
                writer.newLine();
            }

            JOptionPane.showMessageDialog(this, "Datos exportados correctamente en:\n" + fileToSave.getAbsolutePath(),
                    "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error exportando a CSV", ex);
            JOptionPane.showMessageDialog(this, "Error al exportar los datos: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeCSVValue(String val) {
        if (val == null)
            return "";
        val = val.replace("\"", "\"\"");
        if (val.contains(",") || val.contains("\n") || val.contains("\"")) {
            return "\"" + val + "\"";
        }
        return val;
    }

    private void importCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Importar Colaboradores desde CSV");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));

        int userSelection = chooser.showOpenDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToLoad = chooser.getSelectedFile();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(fileToLoad), "UTF-8"))) {
            String line = reader.readLine();
            if (line == null) {
                JOptionPane.showMessageDialog(this, "El archivo seleccionado está vacío.", "Error de Importación",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Si tiene BOM de UTF-8, lo quitamos
            if (line.startsWith("\ufeff")) {
                line = line.substring(1);
            }

            Connection conn = m_App.getSession().getConnection();

            // Map names/IDs for local matching
            Map<String, String> existingNameToIdMap = new HashMap<>();
            try (java.sql.Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT ID, NAME FROM colaboradores")) {
                while (rs.next()) {
                    String localId = rs.getString("ID");
                    String localName = rs.getString("NAME");
                    if (localName != null) {
                        existingNameToIdMap.put(localName.toLowerCase().trim(), localId);
                    }
                }
            }

            String updateSql = "UPDATE colaboradores SET NAME=?, PHONE=?, EMAIL=?, ADDRESS=?, COLONIA=?, BIRTHDAY=?, FAVORITE_MOVIE=?, FAVORITE_BOOK=?, HIRE_DATE=?, JOB_TITLE=?, NOTES=? WHERE ID=?";
            String insertSql = "INSERT INTO colaboradores (ID, NAME, PHONE, EMAIL, ADDRESS, COLONIA, BIRTHDAY, FAVORITE_MOVIE, FAVORITE_BOOK, HIRE_DATE, JOB_TITLE, NOTES) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            int importedCount = 0;
            int updatedCount = 0;

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                    PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                    PreparedStatement delPeople = conn.prepareStatement("DELETE FROM people WHERE ID = ?")) {

                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty())
                        continue;

                    List<String> values = parseCSVLine(line);
                    if (values.size() < 2)
                        continue; // Al menos necesita un nombre

                    String id = values.size() > 0 ? values.get(0) : "";
                    String name = values.size() > 1 ? values.get(1) : "";
                    if (name.isEmpty())
                        continue;

                    String phone = values.size() > 2 ? values.get(2) : "";
                    String email = values.size() > 3 ? values.get(3) : "";
                    String address = values.size() > 4 ? values.get(4) : "";
                    String colonia = values.size() > 5 ? values.get(5) : "";
                    Timestamp birthday = values.size() > 6 ? parseCSVDate(values.get(6)) : null;
                    String movie = values.size() > 7 ? values.get(7) : "";
                    String book = values.size() > 8 ? values.get(8) : "";
                    Timestamp hireDate = values.size() > 9 ? parseCSVDate(values.get(9)) : null;
                    String jobTitle = values.size() > 10 ? values.get(10) : "";
                    String notes = values.size() > 11 ? values.get(11) : "";

                    boolean isUpdate = false;
                    String finalId = id;

                    // Match logic
                    if (finalId != null && !finalId.trim().isEmpty()) {
                        // Check if ID exists
                        try (PreparedStatement checkStmt = conn.prepareStatement("SELECT ID FROM colaboradores WHERE ID=?")) {
                            checkStmt.setString(1, finalId);
                            try (ResultSet rs = checkStmt.executeQuery()) {
                                if (rs.next()) {
                                    isUpdate = true;
                                }
                            }
                        }
                    } else {
                        // Try matching by name
                        String matchedId = existingNameToIdMap.get(name.toLowerCase().trim());
                        if (matchedId != null) {
                            finalId = matchedId;
                            isUpdate = true;
                        } else {
                            finalId = UUID.randomUUID().toString();
                        }
                    }

                    // Save locally
                    if (isUpdate) {
                        updateStmt.setString(1, name);
                        updateStmt.setString(2, phone);
                        updateStmt.setString(3, email);
                        updateStmt.setString(4, address);
                        updateStmt.setString(5, colonia);
                        updateStmt.setTimestamp(6, birthday);
                        updateStmt.setString(7, movie);
                        updateStmt.setString(8, book);
                        updateStmt.setTimestamp(9, hireDate);
                        updateStmt.setString(10, jobTitle);
                        updateStmt.setString(11, notes);
                        updateStmt.setString(12, finalId);
                        updateStmt.executeUpdate();
                        updatedCount++;
                    } else {
                        insertStmt.setString(1, finalId);
                        insertStmt.setString(2, name);
                        insertStmt.setString(3, phone);
                        insertStmt.setString(4, email);
                        insertStmt.setString(5, address);
                        insertStmt.setString(6, colonia);
                        insertStmt.setTimestamp(7, birthday);
                        insertStmt.setString(8, movie);
                        insertStmt.setString(9, book);
                        insertStmt.setTimestamp(10, hireDate);
                        insertStmt.setString(11, jobTitle);
                        insertStmt.setString(12, notes);
                        insertStmt.executeUpdate();
                        importedCount++;
                        existingNameToIdMap.put(name.toLowerCase().trim(), finalId);
                    }

                    // Clean from people, avoiding deleting standard and active POS system users
                    if (!isSystemOrActivePosUser(finalId, conn)) {
                        delPeople.setString(1, finalId);
                        delPeople.executeUpdate();
                    }

                    // Sync to Firebase
                    EmployeeData tempEmp = new EmployeeData();
                    tempEmp.id = finalId;
                    tempEmp.name = name;
                    tempEmp.phone = phone;
                    tempEmp.email = email;
                    tempEmp.address = address;
                    tempEmp.colonia = colonia;
                    tempEmp.birthday = birthday;
                    tempEmp.favoriteMovie = movie;
                    tempEmp.favoriteBook = book;
                    tempEmp.hireDate = hireDate;
                    tempEmp.jobTitle = jobTitle;
                    tempEmp.notes = notes;
                    syncToFirebaseAsync(tempEmp, false);
                }
            }

            reloadLocalOnly();
            JOptionPane.showMessageDialog(this, "Importación completada.\n" +
                    "- Nuevos colaboradores importados: " + importedCount + "\n" +
                    "- Colaboradores existentes actualizados: " + updatedCount + "\n\n" +
                    "Los datos importados están siendo sincronizados con el servidor en segundo plano.",
                    "Importación Exitosa", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error importando desde CSV", ex);
            JOptionPane.showMessageDialog(this, "Error al importar el archivo CSV: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<String> parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString().trim());
        return result;
    }

    private boolean isSystemOrActivePosUser(String id, Connection conn) {
        if (id == null) {
            return true;
        }
        String cleanId = id.trim();
        // Evitar eliminar IDs del sistema estándar de Openbravo POS
        if ("0".equals(cleanId) || "1".equals(cleanId) || "2".equals(cleanId) || "3".equals(cleanId) || "4".equals(cleanId)) {
            return true;
        }
        // Buscar en la base de datos si el usuario tiene privilegios elevados o nombres reservados
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT NAME, ROLE FROM people WHERE ID = ?")) {
            pstmt.setString(1, cleanId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("NAME");
                    String role = rs.getString("ROLE");
                    if (name != null) {
                        String lowerName = name.toLowerCase().trim();
                        if (lowerName.equals("admin") || lowerName.equals("administrator") || lowerName.equals("manager") || lowerName.equals("empl")) {
                            return true;
                        }
                    }
                    if ("0".equals(role)) { // Rol de administrador
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al validar si el usuario es del sistema", e);
        }
        return false;
    }

    private Timestamp parseCSVDate(String val) {
        if (val == null || val.trim().isEmpty())
            return null;
        try {
            return new Timestamp(DATE_FORMAT.parse(val.trim()).getTime());
        } catch (ParseException e) {
            return null;
        }
    }

    private static class EmployeeData {
        String id;
        String name;
        String phone;
        String email;
        String address;
        String colonia;
        Timestamp birthday;
        String favoriteMovie;
        String favoriteBook;
        Timestamp hireDate;
        String jobTitle;
        String notes;
    }

    private static class UpcomingInfo {
        EmployeeData emp;
        int daysLeft;
        String monthName;
        int day;
    }
}
