//    Sebastian
package com.openbravo.pos.forms;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.JMessageDialog;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.Session;
import com.openbravo.format.Formats;
import com.openbravo.pos.printer.DeviceTicket;
import com.openbravo.pos.printer.TicketParser;
import com.openbravo.pos.printer.TicketPrinterException;
import com.openbravo.pos.scale.DeviceScale;
import com.openbravo.pos.scanpal2.DeviceScanner;
import com.openbravo.pos.scanpal2.DeviceScannerFactory;
import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptException;
import com.openbravo.pos.scripting.ScriptFactory;
import com.openbravo.pos.firebase.FirebaseSyncManagerREST;
import java.awt.CardLayout;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Frame;
import java.io.*;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import org.openide.util.Exceptions;
import com.openbravo.pos.firebase.FirebaseDownloadManagerREST;

/**
 *
 * @author adrianromero
 */
public class JRootApp extends JPanel implements AppView {

    private static final Logger LOGGER = Logger.getLogger(JRootApp.class.getName());
    private static final long serialVersionUID = 1L;

    private final AppProperties appFileProperties;
    private Session session;
    private DataLogicSystem m_dlSystem;

    private Properties hostSavedProperties = null;
    private CashDrawer activeCash = new CashDrawer();
    private CashDrawer closedCash = new CashDrawer();
    private String m_sInventoryLocation;

    private DeviceScale m_Scale;
    private DeviceScanner m_Scanner;
    private DeviceTicket m_DeviceTicket;
    private TicketParser m_TicketParser;

    private JPrincipalApp m_principalapp = null;
    private JAuthPanel mAuthPanel = null;
    private FirebaseSyncManagerREST firebaseSyncManager = null;

    // Sebastian - Indicar si la caja activa necesita configuración de fondo inicial
    private boolean needsInitialCashSetup = false;

    private String getLineTimer() {
        return Formats.HOURMIN.formatValue(new Date());
    }

    private String getLineDate() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.DEFAULT, getDefaultLocale());
        return df.format(new Date());
    }

    public JRootApp(AppProperties props) {
        initComponents();

        appFileProperties = props;
        // TODO load Windows Title
        // m_jLblTitle.setText(m_dlSystem.getResourceAsText("Window.Title"));
        // m_jLblTitle.repaint();
    }

    public void initApp() throws BasicException {

        applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));

        try {
            session = AppViewConnection.createSession(this, appFileProperties);

        } catch (BasicException e) {
            LOGGER.log(Level.WARNING, "Exception on DB createSession", e);
            throw new BasicException("Exception on DB createSession", e);
        }

        m_dlSystem = (DataLogicSystem) getBean("com.openbravo.pos.forms.DataLogicSystem");

        LOGGER.log(Level.INFO, "DB Migration execution Starting");
        try {
            com.openbravo.pos.data.DBMigrator.execDBMigration(session);
            LOGGER.log(Level.INFO, "Database verification or migration done sucessfully");
        } catch (BasicException ex) {
            throw new BasicException("Database verification fail", ex);
        }

        logStartup();

        hostSavedProperties = m_dlSystem.getResourceAsProperties(getHostID());

        if (checkActiveCash()) {
            LOGGER.log(Level.WARNING, "Fail on verify ActiveCash");
            throw new BasicException("Fail on verify ActiveCash");
        }

        // DESHABILITADO: Limpiar tabla usuarios al arrancar (excepto admin, empl y
        // manager)
        // Este código eliminaba los usuarios creados manualmente. Comentado para
        // permitir usuarios personalizados.
        /*
         * try {
         * LOGGER.log(Level.INFO,
         * "🗑️ Limpiando tabla usuarios (people), excepto admin, empl y manager...");
         * java.sql.Connection conn = session.getConnection();
         * boolean autoCommitOriginal = conn.getAutoCommit();
         * 
         * try {
         * // Asegurar que autocommit esté activo o hacer commit manual
         * conn.setAutoCommit(false);
         * 
         * // Eliminar todos excepto admin, empl y manager
         * java.sql.PreparedStatement stmt = conn.prepareStatement(
         * "DELETE FROM people WHERE NAME NOT IN ('admin', 'empl', 'manager')"
         * );
         * int deletedRows = stmt.executeUpdate();
         * stmt.close();
         * 
         * // Hacer commit explícito
         * conn.commit();
         * 
         * LOGGER.log(Level.INFO,
         * "✅ Tabla usuarios (people) limpiada exitosamente. Registros eliminados: " +
         * deletedRows);
         * 
         * // Verificar que realmente se eliminaron y mostrar los usuarios restantes
         * java.sql.PreparedStatement checkStmt = conn.prepareStatement(
         * "SELECT COUNT(*) as total FROM people"
         * );
         * java.sql.ResultSet rs = checkStmt.executeQuery();
         * if (rs.next()) {
         * int remainingRows = rs.getInt(1);
         * LOGGER.log(Level.INFO, "🔍 Verificación: Registros restantes en people: " +
         * remainingRows);
         * }
         * rs.close();
         * checkStmt.close();
         * 
         * // Mostrar qué usuarios quedaron
         * java.sql.PreparedStatement listStmt = conn.prepareStatement(
         * "SELECT NAME FROM people ORDER BY NAME"
         * );
         * java.sql.ResultSet listRs = listStmt.executeQuery();
         * StringBuilder userList = new StringBuilder("👥 Usuarios restantes: ");
         * while (listRs.next()) {
         * userList.append(listRs.getString("NAME")).append(", ");
         * }
         * LOGGER.log(Level.INFO, userList.toString());
         * listRs.close();
         * listStmt.close();
         * 
         * } finally {
         * // Restaurar autocommit original
         * conn.setAutoCommit(autoCommitOriginal);
         * }
         * } catch (Exception e) {
         * LOGGER.log(Level.SEVERE, "❌ ERROR al limpiar tabla usuarios: " +
         * e.getMessage(), e);
         * e.printStackTrace();
         * }
         */

        // Inicializar Supabase automáticamente con credenciales internas
        try {
            com.openbravo.pos.forms.AppConfig dlConfig = new com.openbravo.pos.forms.AppConfig(null);
            dlConfig.load();

            // Supabase siempre está conectado con credenciales internas
            com.openbravo.pos.firebase.FirebaseDownloadManagerREST downloader = new com.openbravo.pos.firebase.FirebaseDownloadManagerREST(
                    session, dlConfig);
            java.util.Map<String, Boolean> selections = new java.util.HashMap<>();
            // NO sincronizar usuarios al inicio para mantenerlos limpios
            selections.put("clientes", true);
            selections.put("categorias", true);
            selections.put("productos", false);
            selections.put("puntos_historial", true);
            selections.put("formas_de_pago", true);
            selections.put("impuestos", true);
            selections.put("config", true);
            selections.put("ticketlines", true);
            downloader.performSelectedDownload(selections).join();
            LOGGER.info("Supabase sincronización completada exitosamente");
        } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING,
                    "Error al inicializar Supabase. Continuando sin sincronización inicial: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "Error al inicializar Supabase. Continuando sin sincronización inicial: " + e.getMessage(), e);
        }

        setInventoryLocation();

        initPeripheral();

        setTitlePanel();

        setStatusBarPanel();

        // showLoginPanel(); // Sebastian - Ahora se usa JLogonDialog desde JRootFrame

        // Sebastian - Verificar actualizaciones en segundo plano (no bloquea el inicio)
        checkForUpdatesAsync();
    }

    /**
     * Sebastian - Verifica actualizaciones de forma asíncrona
     */
    private void checkForUpdatesAsync() {
        new Thread(() -> {
            try {
                // Esperar un poco para no interferir con el inicio
                Thread.sleep(3000);

                UpdateChecker.UpdateInfo updateInfo = UpdateChecker.checkForUpdates();
                if (updateInfo != null && updateInfo.isAvailable()) {
                    // Mostrar diálogo de actualización en el hilo de UI
                    SwingUtilities.invokeLater(() -> {
                        showUpdateDialog(updateInfo);
                    });
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error verificando actualizaciones: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Sebastian - Muestra el diálogo de actualización
     */
    private void showUpdateDialog(UpdateChecker.UpdateInfo updateInfo) {
        java.awt.Window parentWindow = SwingUtilities.getWindowAncestor(this);
        java.awt.Frame parentFrame = null;
        if (parentWindow instanceof java.awt.Frame) {
            parentFrame = (java.awt.Frame) parentWindow;
        } else if (parentWindow instanceof java.awt.Dialog) {
            parentFrame = (java.awt.Frame) ((java.awt.Dialog) parentWindow).getParent();
        }

        JDialogUpdate updateDialog = new JDialogUpdate(parentFrame, updateInfo);
        updateDialog.showUpdateDialog();
    }

    /**
     * Sebastian - Método público para verificar actualizaciones manualmente
     * Puede ser llamado desde el menú de configuración
     */
    public void checkForUpdatesManually() {
        waitCursorBegin();
        new Thread(() -> {
            try {
                UpdateChecker.UpdateInfo updateInfo = UpdateChecker.checkForUpdates();
                SwingUtilities.invokeLater(() -> {
                    waitCursorEnd();
                    if (updateInfo != null && updateInfo.isAvailable()) {
                        showUpdateDialog(updateInfo);
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(
                                SwingUtilities.getWindowAncestor(this),
                                "Ya tienes la versión más reciente.\nVersión actual: " + AppLocal.APP_VERSION,
                                "Sin Actualizaciones",
                                javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    waitCursorEnd();
                    javax.swing.JOptionPane.showMessageDialog(
                            SwingUtilities.getWindowAncestor(this),
                            "Error al verificar actualizaciones:\n" + e.getMessage(),
                            "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    /**
     * Sebastian - Método para mostrar opción de actualización cuando hay un
     * problema
     * Puede ser llamado desde catch blocks o cuando se detecta un error crítico
     */
    public void showUpdateOptionOnError(String errorMessage) {
        int option = javax.swing.JOptionPane.showOptionDialog(
                SwingUtilities.getWindowAncestor(this),
                "Se ha detectado un problema:\n\n" + errorMessage +
                        "\n\n¿Deseas verificar si hay una actualización disponible que pueda solucionarlo?",
                "Problema Detectado",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE,
                null,
                new Object[] { "Verificar Actualización", "Cancelar" },
                "Verificar Actualización");

        if (option == 0) {
            checkForUpdatesManually();
        }
    }

    private void setTitlePanel() {

        /*
         * Timer show Date Hour:min:seg
         * javax.swing.Timer clockTimer = new javax.swing.Timer(1000, new
         * ActionListener() {
         * 
         * @Override
         * public void actionPerformed(ActionEvent evt) {
         * String m_clock = getLineTimer();
         * String m_date = getLineDate();
         * jLabel2.setText("  " + m_date + " " + m_clock);
         * }
         * });
         * 
         * clockTimer.start();
         */

        // Remove Panel Title
        remove(m_jPanelTitle);

    }

    private String getHostID() {
        return appFileProperties.getHost() + "/properties";
    }

    private void setInventoryLocation() {
        m_sInventoryLocation = hostSavedProperties.getProperty("location");
        if (m_sInventoryLocation == null) {
            m_sInventoryLocation = "0";
            hostSavedProperties.setProperty("location", m_sInventoryLocation);
            m_dlSystem.setResourceAsProperties(getHostID(), hostSavedProperties);
        }
    }

    private void initPeripheral() {
        m_DeviceTicket = new DeviceTicket(this, appFileProperties);

        m_TicketParser = new TicketParser(getDeviceTicket(), m_dlSystem);
        printerStart();

        m_Scale = new DeviceScale(this, appFileProperties);

        m_Scanner = DeviceScannerFactory.createInstance(appFileProperties);
    }

    private boolean checkActiveCash() {
        try {
            String sActiveCashIndex = hostSavedProperties.getProperty("activecash");
            Object[] valcash = sActiveCashIndex == null
                    ? null
                    : m_dlSystem.findActiveCash(sActiveCashIndex);

            // Verificar si la caja existe, pertenece al host correcto y está ABIERTA (sin
            // DATEEND)
            boolean cashExists = valcash != null;
            boolean cashBelongsToHost = cashExists && valcash.length > 0
                    && appFileProperties.getHost().equals(valcash[0]);
            boolean cashIsOpen = cashExists && valcash.length > 3 && valcash[3] == null; // DATEEND es null = caja
                                                                                         // abierta

            if (!cashExists || !cashBelongsToHost || !cashIsOpen) {
                // La caja no existe, no pertenece a este host, o está cerrada - crear nueva
                if (cashExists && !cashIsOpen) {
                    LOGGER.log(Level.INFO, "🔒 Sebastian - La caja está cerrada (tiene DATEEND), creando nueva caja");
                } else if (!cashExists) {
                    LOGGER.log(Level.INFO, "🔍 Sebastian - No se encontró caja activa, creando nueva");
                } else if (!cashBelongsToHost) {
                    LOGGER.log(Level.INFO, "🔍 Sebastian - La caja no pertenece a este host, creando nueva");
                }

                // Sebastian - Solo inicializar caja sin monto inicial (se pedirá después del
                // login)
                setActiveCash(UUID.randomUUID().toString(),
                        m_dlSystem.getSequenceCash(appFileProperties.getHost()) + 1, new Date(), null, 0.0);
                m_dlSystem.execInsertCash(
                        new Object[] { getActiveCashIndex(), appFileProperties.getHost(),
                                getActiveCashSequence(),
                                getActiveCashDateStart(),
                                getActiveCashDateEnd(), 0.0 }); // Sebastian - Monto inicial temporal
                needsInitialCashSetup = true; // Sebastian - Marcar que necesita configuración de fondo
                return false; // Continuar normalmente
            } else {
                // La caja existe, pertenece al host y está ABIERTA - recuperarla
                // Sebastian - Recuperar fondo inicial guardado (si existe)
                LOGGER.log(Level.INFO, "🔍 Sebastian - Datos de caja recuperados - Array length: " + valcash.length);
                for (int i = 0; i < valcash.length; i++) {
                    LOGGER.log(Level.INFO, "🔍 Sebastian - valcash[" + i + "] = " + valcash[i] + " (tipo: "
                            + (valcash[i] != null ? valcash[i].getClass().getSimpleName() : "null") + ")");
                }

                // Sebastian - Manejar correctamente el fondo inicial, incluso si es null
                double savedInitialAmount = 0.0;
                if (valcash.length > 5 && valcash[5] != null) {
                    // Si el valor existe y no es null, convertirlo a double
                    if (valcash[5] instanceof Number) {
                        savedInitialAmount = ((Number) valcash[5]).doubleValue();
                    } else if (valcash[5] instanceof Double) {
                        savedInitialAmount = (Double) valcash[5];
                    } else {
                        try {
                            savedInitialAmount = Double.parseDouble(valcash[5].toString());
                        } catch (NumberFormatException e) {
                            LOGGER.log(Level.WARNING,
                                    "⚠️ Sebastian - No se pudo convertir initial_amount a double: " + valcash[5], e);
                            savedInitialAmount = 0.0;
                        }
                    }
                } else {
                    // Si no existe o es null, verificar directamente en la BD
                    LOGGER.log(Level.INFO,
                            "🔍 Sebastian - initial_amount no encontrado en array, consultando BD directamente...");
                    try {
                        Session s = getSession();
                        java.sql.Connection con = s.getConnection();
                        String sql = "SELECT initial_amount FROM closedcash WHERE MONEY = ? AND DATEEND IS NULL";
                        java.sql.PreparedStatement pstmt = con.prepareStatement(sql);
                        pstmt.setString(1, sActiveCashIndex);
                        java.sql.ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            double dbAmount = rs.getDouble("initial_amount");
                            if (!rs.wasNull()) {
                                savedInitialAmount = dbAmount;
                                LOGGER.log(Level.INFO,
                                        "✅ Sebastian - Fondo inicial recuperado desde BD: $" + savedInitialAmount);
                            } else {
                                LOGGER.log(Level.WARNING,
                                        "⚠️ Sebastian - initial_amount es NULL en BD para MONEY: " + sActiveCashIndex);
                                savedInitialAmount = 0.0;
                            }
                        } else {
                            LOGGER.log(Level.WARNING,
                                    "⚠️ Sebastian - No se encontró registro en BD para MONEY: " + sActiveCashIndex);
                            savedInitialAmount = 0.0;
                        }
                        rs.close();
                        pstmt.close();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING,
                                "⚠️ Sebastian - Error consultando initial_amount desde BD: " + e.getMessage(), e);
                        savedInitialAmount = 0.0;
                    }
                }

                setActiveCash(sActiveCashIndex,
                        (Integer) valcash[1],
                        (Date) valcash[2],
                        (Date) valcash[3], // DATEEND debería ser null si está abierta
                        savedInitialAmount);

                LOGGER.log(Level.INFO,
                        "💰 Sebastian POS - Caja activa recuperada con fondo inicial: $" + savedInitialAmount +
                                " (Caja " + (valcash[3] == null ? "ABIERTA" : "CERRADA") + ")");

                // Sebastian - Verificar si hay tickets para determinar si el turno está en uso
                boolean hasTickets = false;
                try {
                    java.sql.Connection con = session.getConnection();
                    java.sql.PreparedStatement checkStmt = con.prepareStatement(
                            "SELECT COUNT(*) as ticket_count FROM receipts WHERE MONEY = ?");
                    checkStmt.setString(1, sActiveCashIndex);
                    java.sql.ResultSet rs = checkStmt.executeQuery();
                    if (rs.next()) {
                        hasTickets = rs.getInt("ticket_count") > 0;
                    }
                    rs.close();
                    checkStmt.close();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error verificando tickets: " + e.getMessage(), e);
                }

                // Sebastian - Necesita configuración si:
                // 1. El fondo inicial es 0 o menor
                // 2. Y no hay tickets (significa que es un turno nuevo sin usar o de otro
                // usuario)
                // Si tiene tickets pero fondo inicial es 0, podría ser un problema pero no se
                // pedirá de nuevo
                needsInitialCashSetup = (savedInitialAmount <= 0.0 && !hasTickets);

                if (needsInitialCashSetup) {
                    LOGGER.log(Level.INFO,
                            "🔧 Sebastian - Caja necesita configuración de fondo inicial (valor actual: $"
                                    + savedInitialAmount +
                                    ", sin tickets - turno nuevo o de otro usuario)");
                } else if (savedInitialAmount <= 0.0 && hasTickets) {
                    LOGGER.log(Level.WARNING, "⚠️ Sebastian - Caja tiene fondo inicial 0 pero tiene tickets. " +
                            "Esto podría indicar un problema, pero no se pedirá fondo inicial de nuevo.");
                } else {
                    LOGGER.log(Level.INFO, "✅ Sebastian - Caja abierta con fondo inicial configurado: $"
                            + savedInitialAmount + ", NO se pedirá fondo inicial");
                }

                return false; // Continuar normalmente
            }
        } catch (BasicException e) {
            MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE,
                    AppLocal.getIntString("message.cannotclosecash"), e);
            msg.show(this);
            return true; // Error crítico
        }
    }

    private void logStartup() {
        // create the filename
        String sUserPath = AppConfig.getInstance().getAppDataDirectory();

        Instant machineTimestamp = Instant.now();
        String sContent = sUserPath + ","
                + machineTimestamp + ","
                + AppLocal.APP_ID + ","
                + AppLocal.APP_NAME + ","
                + AppLocal.APP_VERSION + "\n";

        try {
            Files.write(new File(sUserPath, AppLocal.getLogFileName()).toPath(), sContent.getBytes(),
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private String readDataBaseVersion() {
        try {
            return m_dlSystem.findVersion();
        } catch (BasicException ed) {
            return null;
        }
    }

    @Override
    public DeviceTicket getDeviceTicket() {
        return m_DeviceTicket;
    }

    @Override
    public DeviceScale getDeviceScale() {
        return m_Scale;
    }

    @Override
    public DeviceScanner getDeviceScanner() {
        return m_Scanner;
    }

    @Override
    public Session getSession() {
        return session;
    }

    public DataLogicSystem getDataLogicSystem() {
        return m_dlSystem;
    }

    @Override
    public String getInventoryLocation() {
        return m_sInventoryLocation;
    }

    @Override
    public String getActiveCashIndex() {
        return activeCash.getCashIndex();
    }

    @Override
    public int getActiveCashSequence() {
        return activeCash.getCashSequence();
    }

    @Override
    public Date getActiveCashDateStart() {
        return activeCash.getCashDateStart();
    }

    @Override
    public Date getActiveCashDateEnd() {
        return activeCash.getCashDateEnd();
    }

    @Override
    public void setActiveCash(String sIndex, int iSeq, Date dStart, Date dEnd) {
        activeCash.setCashIndex(sIndex);
        activeCash.setCashSequence(iSeq);
        activeCash.setCashDateStart(dStart);
        activeCash.setCashDateEnd(dEnd);
        activeCash.setInitialAmount(0.0); // Sebastian - Fondo inicial por defecto

        hostSavedProperties.setProperty("activecash", activeCash.getCashIndex());
        m_dlSystem.setResourceAsProperties(getHostID(), hostSavedProperties);
    }

    // Sebastian - Método para establecer caja activa con fondo inicial
    @Override
    public void setActiveCash(String sIndex, int iSeq, Date dStart, Date dEnd, double initialAmount) {
        activeCash.setCashIndex(sIndex);
        activeCash.setCashSequence(iSeq);
        activeCash.setCashDateStart(dStart);
        activeCash.setCashDateEnd(dEnd);
        activeCash.setInitialAmount(initialAmount);

        hostSavedProperties.setProperty("activecash", activeCash.getCashIndex());
        m_dlSystem.setResourceAsProperties(getHostID(), hostSavedProperties);
    }

    // Sebastian - Obtener fondo inicial de la caja activa
    @Override
    public double getActiveCashInitialAmount() {
        return activeCash.getInitialAmount();
    }

    // Sebastian - Actualizar solo el fondo inicial de la caja activa
    public void setActiveCashInitialAmount(double initialAmount) {
        activeCash.setInitialAmount(initialAmount);
    }

    @Override
    public String getClosedCashIndex() {
        return closedCash.getCashIndex();
    }

    @Override
    public int getClosedCashSequence() {
        return closedCash.getCashSequence();
    }

    @Override
    public Date getClosedCashDateStart() {
        return closedCash.getCashDateStart();
    }

    @Override
    public Date getClosedCashDateEnd() {
        return closedCash.getCashDateEnd();
    }

    @Override
    public void setClosedCash(String sIndex, int iSeq, Date dStart, Date dEnd) {
        closedCash.setCashIndex(sIndex);
        closedCash.setCashSequence(iSeq);
        closedCash.setCashDateStart(dStart);
        closedCash.setCashDateEnd(dEnd);
        hostSavedProperties.setProperty("closecash", closedCash.getCashIndex());
        m_dlSystem.setResourceAsProperties(getHostID(), hostSavedProperties);
    }

    @Override
    public AppProperties getProperties() {
        return appFileProperties;
    }

    @Override
    public Object getBean(String beanfactory) throws BeanFactoryException {
        return BeanContainer.geBean(beanfactory, this);
    }

    @Override
    public void waitCursorBegin() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    @Override
    public void waitCursorEnd() {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    @Override
    public AppUserView getAppUserView() {
        return m_principalapp;
    }

    @Override
    public boolean hasPermission(String permission) {
        return Optional.ofNullable(this.getAppUserView())
                .map(i -> i.getUser())
                .map(u -> u.hasPermission(permission))
                .orElse(false);
    }

    private void printerStart() {

        String sresource = m_dlSystem.getResourceAsXML("Printer.Start");
        if (sresource == null) {
            m_DeviceTicket.getDeviceDisplay().writeVisor(AppLocal.APP_NAME, AppLocal.APP_VERSION);
        } else {
            try {
                ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
                script.put("appname", AppLocal.APP_NAME);
                script.put("appShortDescription", AppLocal.APP_SHORT_DESCRIPTION);
                String xmlContent = script.eval(sresource).toString();
                m_TicketParser.printTicket(xmlContent);
            } catch (TicketPrinterException eTP) {
                m_DeviceTicket.getDeviceDisplay().writeVisor(AppLocal.APP_NAME, AppLocal.APP_VERSION);
            } catch (ScriptException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void showView(String view) {
        CardLayout cl = (CardLayout) (m_jPanelContainer.getLayout());
        cl.show(m_jPanelContainer, view);
    }

    public void openAppView(AppUser user) {

        LOGGER.log(Level.WARNING, "INFO :: showMainAppPanel");

        // Sebastian - Verificar si hay un turno abierto antes de permitir el login del
        // nuevo usuario
        // IMPORTANTE: Verificar ANTES de cerrar la vista para poder acceder a
        // m_principalapp
        boolean belongsToOtherUser = false;
        AppUser previousUser = null;

        if (m_principalapp != null) {
            previousUser = m_principalapp.getUser();
            if (previousUser != null && !previousUser.getName().equals(user.getName())) {
                belongsToOtherUser = true;
                LOGGER.log(Level.INFO, "🔒 Sebastian - Detectado cambio de usuario: " + previousUser.getName() +
                        " -> " + user.getName());
            }
        }

        // Sebastian - LÓGICA SIMPLE: Verificar si hay un turno y si tiene fondo inicial
        // configurado
        String activeCashIndex = getActiveCashIndex();
        Date activeCashDateEnd = getActiveCashDateEnd();
        double activeCashInitialAmount = getActiveCashInitialAmount();

        if (activeCashIndex != null) {
            if (activeCashDateEnd == null) {
                // Turno ABIERTO - verificar si tiene fondo inicial configurado
                LOGGER.log(Level.INFO,
                        "🔒 Sebastian - Hay un turno abierto. Fondo inicial: $" + activeCashInitialAmount);

                // Si NO tiene fondo inicial, simplemente pedir el fondo inicial (es un turno
                // recién creado)
                if (activeCashInitialAmount <= 0.0) {
                    LOGGER.log(Level.INFO, "✅ Sebastian - Turno abierto SIN fondo inicial. Se pedirá fondo inicial.");
                    // No hacer nada, el sistema pedirá fondo inicial más adelante
                    belongsToOtherUser = false;
                } else {
                    // Tiene fondo inicial - verificar si pertenece al mismo usuario
                    // Solo verificar si hay un usuario anterior diferente o si hay tickets de otro
                    // usuario
                    if (previousUser != null && !previousUser.getName().equals(user.getName())) {
                        // Cambio de usuario detectado - verificar si el turno tiene tickets
                        belongsToOtherUser = checkIfCashBelongsToOtherUser(user);
                        if (belongsToOtherUser) {
                            LOGGER.log(Level.INFO, "🔒 Sebastian - Turno abierto pertenece a otro usuario (" +
                                    previousUser.getName() + "). Forzando cierre.");
                        }
                    } else {
                        // Mismo usuario o primer login - permitir continuar
                        LOGGER.log(Level.INFO,
                                "✅ Sebastian - Turno abierto pertenece al mismo usuario o es primer login.");
                        belongsToOtherUser = false;
                    }
                }
            }

            if (belongsToOtherUser) {
                // Forzar el cierre del turno antes de permitir el login del nuevo usuario
                LOGGER.log(Level.INFO,
                        "🔒 Sebastian - Forzando cierre de turno del usuario anterior antes de permitir login del nuevo usuario");

                java.awt.Window parentWindow = SwingUtilities.getWindowAncestor(this);
                Frame parentFrame = null;
                if (parentWindow instanceof Frame) {
                    parentFrame = (Frame) parentWindow;
                } else if (parentWindow instanceof Dialog) {
                    parentFrame = (Frame) ((Dialog) parentWindow).getParent();
                }

                // Obtener el nombre del usuario del turno abierto
                String turnoUserName = getUserNameFromActiveCash();
                if (turnoUserName == null && previousUser != null) {
                    turnoUserName = previousUser.getName();
                }
                if (turnoUserName == null) {
                    turnoUserName = "otro usuario";
                }

                // Mostrar mensaje informativo con el nombre del usuario
                String mensajeUsuario = "Hay un turno abierto del usuario '" + turnoUserName + "'.\n" +
                        "Debes cerrar el turno antes de iniciar sesión con otro usuario.\n\n" +
                        "¿Deseas cerrar el turno ahora?";

                int opcion = JOptionPane.showOptionDialog(
                        parentFrame,
                        mensajeUsuario,
                        "Turno Abierto",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new Object[] { "Cerrar Turno", "Cancelar" },
                        "Cerrar Turno");

                if (opcion == JOptionPane.YES_OPTION) {
                    // Crear una vista temporal del usuario anterior solo para cerrar el turno
                    AppUser userToCloseShift = null;
                    try {
                        if (turnoUserName != null && !turnoUserName.equals("otro usuario")) {
                            userToCloseShift = m_dlSystem.findPeopleByName(turnoUserName);
                        }
                        if (userToCloseShift == null && previousUser != null) {
                            userToCloseShift = previousUser;
                        }

                        // Si tenemos un usuario, crear una vista temporal solo para cerrar el turno
                        if (userToCloseShift != null) {
                            // Guardar el estado actual de m_principalapp
                            JPrincipalApp previousPrincipalApp = m_principalapp;

                            try {
                                // Crear una vista temporal del usuario anterior solo para cerrar el turno
                                JPrincipalApp tempApp = new JPrincipalApp(this, userToCloseShift);

                                // Asignar temporalmente la vista temporal a m_principalapp para que
                                // getAppUserView() funcione
                                m_principalapp = tempApp;

                                // Mostrar diálogo completo de cierre de turno usando this (JRootApp) como
                                // AppView
                                JDialogCloseShift dialog = new JDialogCloseShift(parentFrame, this);
                                dialog.setVisible(true);

                                if (!dialog.isClosed() || !dialog.shouldCloseShift()) {
                                    // El usuario canceló el cierre del turno, no permitir el login
                                    LOGGER.log(Level.INFO,
                                            "🔒 Sebastian - El usuario canceló el cierre del turno. No se permite el login.");
                                    return;
                                }

                                LOGGER.log(Level.INFO,
                                        "✅ Sebastian - Turno cerrado exitosamente. Continuando con el login del nuevo usuario.");
                            } finally {
                                // Restaurar el estado original de m_principalapp
                                m_principalapp = previousPrincipalApp;
                            }
                        } else {
                            // No se pudo obtener el usuario, mostrar mensaje de error
                            JOptionPane.showMessageDialog(
                                    parentFrame,
                                    "No se pudo obtener la información del usuario del turno abierto.\n" +
                                            "Por favor, cierra el turno manualmente antes de iniciar sesión.",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            LOGGER.log(Level.WARNING,
                                    "🔒 Sebastian - No se pudo obtener el usuario del turno. No se permite el login.");
                            return;
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error cerrando turno: " + e.getMessage(), e);
                        JOptionPane.showMessageDialog(
                                parentFrame,
                                "Error al cerrar el turno: " + e.getMessage() + "\n" +
                                        "Por favor, cierra el turno manualmente antes de iniciar sesión.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else {
                    // El usuario canceló, no permitir el login
                    LOGGER.log(Level.INFO,
                            "🔒 Sebastian - El usuario canceló. No se permite el login sin cerrar el turno.");
                    return;
                }
            } else if (activeCashDateEnd != null) {
                // Turno CERRADO - SIEMPRE crear un nuevo turno y pedir fondo inicial
                // No importa si pertenece al mismo usuario o a otro, un turno cerrado requiere
                // un nuevo turno
                LOGGER.log(Level.INFO,
                        "🔒 Sebastian - Hay un turno cerrado. Creando nuevo turno para " + user.getName() + "...");

                // Verificar si el turno cerrado pertenece a otro usuario (solo para logging)
                String turnoUserName = getUserNameFromActiveCash();
                if (turnoUserName != null && !turnoUserName.equals(user.getName())) {
                    LOGGER.log(Level.INFO, "🔒 Sebastian - Turno cerrado pertenece a otro usuario (" + turnoUserName +
                            "). Creando nuevo turno para " + user.getName());
                } else {
                    LOGGER.log(Level.INFO, "✅ Sebastian - Turno cerrado. Creando nuevo turno para " + user.getName());
                }

                // Crear nuevo turno
                try {
                    setActiveCash(UUID.randomUUID().toString(),
                            m_dlSystem.getSequenceCash(appFileProperties.getHost()) + 1, new Date(), null, 0.0);
                    m_dlSystem.execInsertCash(
                            new Object[] { getActiveCashIndex(), appFileProperties.getHost(),
                                    getActiveCashSequence(),
                                    getActiveCashDateStart(),
                                    getActiveCashDateEnd(), 0.0 });
                    needsInitialCashSetup = true; // SIEMPRE pedir fondo inicial para un nuevo turno
                    LOGGER.log(Level.INFO, "✅ Sebastian - Nuevo turno creado. Se solicitará fondo inicial.");
                } catch (BasicException e) {
                    LOGGER.log(Level.SEVERE, "Error creando nuevo turno: " + e.getMessage(), e);
                }
            }
        }

        if (closeAppView()) {

            m_principalapp = new JPrincipalApp(this, user);

            // Sebastian - El perfil ahora está en el panel superior, no en jPanel3
            // jPanel3.add(m_principalapp.getNotificator());
            jPanel3.revalidate();

            String viewID = "_" + m_principalapp.getUser().getId();
            m_jPanelContainer.add(m_principalapp, viewID);
            showView(viewID);

            m_principalapp.activate();

            // Sebastian - Verificar si la caja activa pertenece a otro usuario
            boolean cashBelongsToOtherUser = checkIfCashBelongsToOtherUser(user);

            // Sebastian - Solo verificar monto inicial si realmente necesita configuración
            // O si la caja pertenece a otro usuario (necesita nuevo fondo inicial)
            if (needsInitialCashSetup || cashBelongsToOtherUser) {
                if (cashBelongsToOtherUser) {
                    LOGGER.log(Level.INFO,
                            "🔧 Sebastian - La caja activa pertenece a otro usuario, solicitando nuevo fondo inicial");
                    // Cerrar el turno del usuario anterior y crear uno nuevo para el usuario actual
                    createNewCashForUser(user);
                } else {
                    LOGGER.log(Level.INFO,
                            "🔧 Sebastian - Configurando fondo inicial para nueva caja o caja sin fondo configurado");
                }
                checkAndSetupInitialCash();
            } else {
                LOGGER.log(Level.INFO,
                        "✅ Sebastian - Caja activa ya tiene fondo inicial configurado, omitiendo solicitud");
            }

            // Inicializar Firebase Sync Manager después de activar la aplicación principal
            initializeFirebaseSync();
        }
    }

    /**
     * Inicializa el Supabase Sync Manager y ejecuta la sincronización automática
     * si hay conexión a internet y Supabase está configurado.
     */
    private void initializeFirebaseSync() {
        try {
            if (session != null && m_principalapp != null) {
                // Crear el FirebaseSyncManagerREST con la sesión actual y el AppUserView
                firebaseSyncManager = new FirebaseSyncManagerREST(session, m_principalapp);

                // Inicializar Supabase con el usuario actual - siempre conectado con
                // credenciales internas
                com.openbravo.pos.supabase.SupabaseServiceManager supabaseManager = com.openbravo.pos.supabase.SupabaseServiceManager
                        .getInstance();
                if (!supabaseManager.initialize(appFileProperties, m_principalapp)) {
                    LOGGER.warning("No se pudo inicializar Supabase Service. Omitiendo sincronización.");
                    return;
                }

                // Ejecutar sincronización completa en segundo plano
                CompletableFuture.runAsync(() -> {
                    try {
                        LOGGER.info("Supabase sync iniciado automáticamente después del login");
                        com.openbravo.pos.forms.AppConfig dlConfig = new com.openbravo.pos.forms.AppConfig(null);
                        dlConfig.load();

                        // Supabase siempre está conectado con credenciales internas
                        com.openbravo.pos.firebase.FirebaseDownloadManagerREST downloader = new com.openbravo.pos.firebase.FirebaseDownloadManagerREST(
                                session, dlConfig);
                        java.util.Map<String, Boolean> selections = new java.util.HashMap<>();
                        // NO sincronizar usuarios - mantenerlos limpios
                        selections.put("usuarios", true);
                        selections.put("clientes", true);
                        selections.put("categorias", true);
                        selections.put("productos", false);
                        selections.put("puntos_historial", true);
                        selections.put("formas_de_pago", true);
                        selections.put("impuestos", true);
                        selections.put("config", true);
                        selections.put("ticketlines", true);
                        downloader.performSelectedDownload(selections).join();
                        LOGGER.info("Supabase sincronización completada exitosamente después del login");
                    } catch (IllegalStateException e) {
                        LOGGER.log(Level.WARNING,
                                "Error al inicializar Supabase. Omitiendo sincronización: " + e.getMessage());
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING,
                                "Error al ejecutar sincronización completa con Supabase: " + e.getMessage(), e);
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al inicializar Supabase Sync Manager: " + e.getMessage(), e);
        }
    }

    /**
     * Sebastian - Configurar monto inicial (solo se llama cuando realmente se
     * necesita)
     */
    private void checkAndSetupInitialCash() {
        LOGGER.log(Level.INFO,
                "� Sebastian - Solicitando configuración de fondo inicial para caja: " + getActiveCashIndex());
        SwingUtilities.invokeLater(() -> {
            showInitialCashDialog();
        });
    }

    /**
     * Sebastian - Mostrar diálogo para configurar monto inicial
     */
    private void showInitialCashDialog() {
        try {
            LOGGER.log(Level.INFO, "🔔 Sebastian - Mostrando diálogo de monto inicial...");

            // Ejecutar en EDT si no estamos ya en él
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeAndWait(() -> showInitialCashDialog());
                return;
            }

            // Usar la aplicación principal como padre
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (parentFrame == null && getTopLevelAncestor() instanceof JFrame) {
                parentFrame = (JFrame) getTopLevelAncestor();
            }

            JDialogInitialCash dialog = new JDialogInitialCash(parentFrame);
            dialog.setInitialAmount(0.0);

            // Asegurar que el diálogo esté al frente
            dialog.setAlwaysOnTop(true);
            dialog.toFront();
            dialog.requestFocus();

            dialog.setVisible(true);

            if (dialog.isAccepted()) {
                double initialAmount = dialog.getInitialAmount();
                LOGGER.log(Level.INFO, "💰 Sebastian POS - Fondo inicial establecido: $" + initialAmount);
                updateActiveCashInitialAmount(initialAmount);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error mostrando diálogo de monto inicial", ex);
        }
    }

    /**
     * Sebastian - Actualizar monto inicial de la caja activa
     */
    private void updateActiveCashInitialAmount(double initialAmount) {
        try {
            String activeCashIndex = getActiveCashIndex();
            LOGGER.log(Level.INFO, "🔄 Sebastian - Actualizando monto inicial en BD. Caja: " + activeCashIndex
                    + ", Monto: $" + initialAmount);

            // Actualizar en la base de datos
            try {
                m_dlSystem.execUpdateCashInitialAmount(activeCashIndex, initialAmount);
                LOGGER.log(Level.INFO, "✅ Sebastian - Monto inicial actualizado en BD correctamente");
            } catch (Exception sqlEx) {
                LOGGER.log(Level.SEVERE,
                        "❌ Sebastian - ERROR CRÍTICO en execUpdateCashInitialAmount: " + sqlEx.getMessage(), sqlEx);
                throw sqlEx; // Re-lanzar para que se vea el error completo
            }

            // Verificar que el cambio se guardó consultando la BD inmediatamente
            Object[] verificationData = m_dlSystem.findActiveCash(activeCashIndex);
            if (verificationData != null && verificationData.length > 5) {
                double savedAmount = (verificationData[5] != null) ? ((Number) verificationData[5]).doubleValue() : 0.0;
                LOGGER.log(Level.INFO, "🔍 Sebastian - Verificación BD: Monto guardado = $" + savedAmount);

                if (Math.abs(savedAmount - initialAmount) < 0.01) {
                    LOGGER.log(Level.INFO, "✅ Sebastian - Verificación EXITOSA: BD actualizada correctamente");
                } else {
                    LOGGER.log(Level.WARNING, "❌ Sebastian - Verificación FALLÓ: BD muestra $" + savedAmount
                            + ", esperaba $" + initialAmount);
                }
            }

            // Actualizar en memoria
            setActiveCashInitialAmount(initialAmount);

            // Sebastian - Marcar que ya no necesita configuración de fondo inicial
            needsInitialCashSetup = false;

            LOGGER.log(Level.INFO, "✅ Sebastian - PROCESO COMPLETO: Monto inicial actualizado: $" + initialAmount);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "❌ Sebastian - Error actualizando monto inicial: " + ex.getMessage(), ex);
        }
    }

    /**
     * Sebastian - Obtener el nombre del usuario del turno abierto
     */
    private String getUserNameFromActiveCash() {
        try {
            String activeCashIndex = getActiveCashIndex();
            if (activeCashIndex == null) {
                return null;
            }

            // Buscar el primer ticket de la caja activa para obtener el usuario
            java.sql.Connection con = session.getConnection();
            java.sql.PreparedStatement userCheckStmt = con.prepareStatement(
                    "SELECT people.NAME FROM tickets " +
                            "INNER JOIN receipts ON tickets.ID = receipts.ID " +
                            "INNER JOIN people ON tickets.PERSON = people.ID " +
                            "WHERE receipts.MONEY = ? " +
                            "ORDER BY receipts.DATENEW ASC " +
                            "LIMIT 1");
            userCheckStmt.setString(1, activeCashIndex);
            java.sql.ResultSet userCheckRs = userCheckStmt.executeQuery();

            if (userCheckRs.next()) {
                String ticketUserName = userCheckRs.getString("NAME");
                userCheckRs.close();
                userCheckStmt.close();
                return ticketUserName;
            }
            userCheckRs.close();
            userCheckStmt.close();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error obteniendo nombre del usuario del turno: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Sebastian - Verificar si la caja activa pertenece a otro usuario
     */
    private boolean checkIfCashBelongsToOtherUser(AppUser currentUser) {
        try {
            String activeCashIndex = getActiveCashIndex();
            if (activeCashIndex == null) {
                return false;
            }

            // Buscar el primer ticket de la caja activa para verificar el usuario
            java.sql.Connection con = session.getConnection();
            java.sql.PreparedStatement userCheckStmt = con.prepareStatement(
                    "SELECT people.NAME FROM tickets " +
                            "INNER JOIN receipts ON tickets.ID = receipts.ID " +
                            "INNER JOIN people ON tickets.PERSON = people.ID " +
                            "WHERE receipts.MONEY = ? " +
                            "ORDER BY receipts.DATENEW ASC " +
                            "LIMIT 1");
            userCheckStmt.setString(1, activeCashIndex);
            java.sql.ResultSet userCheckRs = userCheckStmt.executeQuery();

            if (userCheckRs.next()) {
                String ticketUserName = userCheckRs.getString("NAME");
                userCheckRs.close();
                userCheckStmt.close();

                if (ticketUserName != null && !ticketUserName.equals(currentUser.getName())) {
                    LOGGER.log(Level.INFO, "🔍 Sebastian - Caja activa pertenece a otro usuario. " +
                            "Usuario del ticket: " + ticketUserName + ", Usuario actual: " + currentUser.getName());
                    return true;
                }
            } else {
                // Si no hay tickets, verificar la fecha de creación de la caja
                // Si la caja fue creada hoy, probablemente es del mismo usuario
                // Esto es válido cuando se selecciona "Mantener Turno" - la caja puede estar
                // abierta
                // sin tickets pero con fondo inicial ya configurado
                userCheckRs.close();
                userCheckStmt.close();

                // Verificar la fecha de inicio de la caja
                Date cashDateStart = getActiveCashDateStart();
                Date today = new Date();
                java.util.Calendar calCash = java.util.Calendar.getInstance();
                java.util.Calendar calToday = java.util.Calendar.getInstance();
                calCash.setTime(cashDateStart);
                calToday.setTime(today);

                boolean sameDay = calCash.get(java.util.Calendar.YEAR) == calToday.get(java.util.Calendar.YEAR) &&
                        calCash.get(java.util.Calendar.DAY_OF_YEAR) == calToday.get(java.util.Calendar.DAY_OF_YEAR);

                if (sameDay) {
                    // La caja fue creada hoy, probablemente es del mismo usuario
                    // No hay evidencia de que pertenezca a otro usuario (no hay tickets)
                    LOGGER.log(Level.INFO, "🔍 Sebastian - Caja activa no tiene tickets pero fue creada hoy, " +
                            "asumiendo que pertenece al usuario actual (caja mantenida abierta). No se pedirá fondo inicial de nuevo.");
                    return false; // No pertenece a otro usuario, puede usar esta caja
                } else {
                    // La caja fue creada en otro día, podría ser de otro usuario
                    // Pero solo si tiene fondo inicial significativo (mayor a 0)
                    double initialAmount = getActiveCashInitialAmount();
                    if (initialAmount > 0.0) {
                        LOGGER.log(Level.INFO, "🔍 Sebastian - Caja activa no tiene tickets, fue creada en otro día " +
                                "y tiene fondo inicial, podría pertenecer a otro usuario");
                        return true; // Probablemente pertenece a otro usuario
                    } else {
                        LOGGER.log(Level.INFO, "🔍 Sebastian - Caja activa no tiene tickets, fue creada en otro día " +
                                "pero no tiene fondo inicial, se puede usar para nuevo usuario");
                        return false; // No pertenece a otro usuario, puede usar esta caja
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error verificando si la caja pertenece a otro usuario: " + e.getMessage(), e);
            return false; // En caso de error, asumir que no pertenece a otro usuario
        }
        return false;
    }

    /**
     * Sebastian - Crear nueva caja para el usuario actual
     */
    private void createNewCashForUser(AppUser user) {
        try {
            String oldCashIndex = getActiveCashIndex();
            Date oldDateStart = getActiveCashDateStart();
            int oldSequence = getActiveCashSequence();

            // Cerrar la caja anterior si tiene tickets
            try {
                java.sql.Connection con = session.getConnection();
                java.sql.PreparedStatement checkTicketsStmt = con.prepareStatement(
                        "SELECT COUNT(*) FROM receipts WHERE MONEY = ?");
                checkTicketsStmt.setString(1, oldCashIndex);
                java.sql.ResultSet checkTicketsRs = checkTicketsStmt.executeQuery();

                if (checkTicketsRs.next() && checkTicketsRs.getInt(1) > 0) {
                    // Tiene tickets, cerrar el turno usando SQL directo
                    Date now = new Date();
                    java.sql.PreparedStatement updateStmt = con.prepareStatement(
                            "UPDATE closedcash SET DATEEND = ?, NOSALES = ? WHERE MONEY = ?");
                    updateStmt.setTimestamp(1, new java.sql.Timestamp(now.getTime()));
                    updateStmt.setInt(2, 0);
                    updateStmt.setString(3, oldCashIndex);
                    updateStmt.executeUpdate();
                    updateStmt.close();
                    LOGGER.log(Level.INFO, "🔒 Sebastian - Caja anterior cerrada: " + oldCashIndex);
                }
                checkTicketsRs.close();
                checkTicketsStmt.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error cerrando caja anterior: " + e.getMessage(), e);
            }

            // Crear nueva caja para el usuario actual
            String newCashIndex = UUID.randomUUID().toString();
            int newSequence = m_dlSystem.getSequenceCash(appFileProperties.getHost()) + 1;
            Date now = new Date();

            // Insertar nueva caja con fondo inicial en 0 (se pedirá después)
            m_dlSystem.execInsertCash(
                    new Object[] {
                            newCashIndex,
                            appFileProperties.getHost(),
                            newSequence,
                            now,
                            null,
                            0.0 // Fondo inicial en 0, se pedirá después
                    });

            // Establecer como caja activa
            setActiveCash(newCashIndex, newSequence, now, null, 0.0);
            needsInitialCashSetup = true; // Marcar que necesita configuración de fondo

            LOGGER.log(Level.INFO, "🆕 Sebastian - Nueva caja creada para usuario: " + user.getName() +
                    " (MONEY: " + newCashIndex + ", Sequence: " + newSequence + ")");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creando nueva caja para usuario: " + e.getMessage(), e);
        }
    }

    // Release hardware,files,...
    private void releaseResources() {
        if (m_DeviceTicket != null) {
            m_DeviceTicket.getDeviceDisplay().clearVisor();
        }

        // Limpiar el Firebase Sync Manager si existe
        if (firebaseSyncManager != null) {
            try {
                // Aquí se puede agregar cualquier limpieza necesaria del FirebaseSyncManager
                firebaseSyncManager = null;
                LOGGER.info("Firebase Sync Manager liberado correctamente");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al liberar Firebase Sync Manager: " + e.getMessage(), e);
            }
        }
    }

    public void tryToClose() {

        // Verificar si hay un turno abierto antes de cerrar
        if (m_principalapp != null && getActiveCashDateEnd() == null && getActiveCashIndex() != null) {
            // Sebastian - Hay un turno abierto, abrir directamente el diálogo de cierre
            java.awt.Window parentWindow = SwingUtilities.getWindowAncestor(this);
            Frame parentFrame = null;
            if (parentWindow instanceof Frame) {
                parentFrame = (Frame) parentWindow;
            } else if (parentWindow instanceof Dialog) {
                parentFrame = (Frame) ((Dialog) parentWindow).getParent();
            }

            JDialogCloseShift dialog = new JDialogCloseShift(parentFrame, this);
            dialog.setVisible(true);

            if (dialog.isClosed() && dialog.shouldCloseShift()) {
                // El turno fue cerrado exitosamente, ahora cerrar la aplicación
                if (closeAppView()) {
                    releaseResources();
                    if (session != null) {
                        try {
                            session.close();
                        } catch (SQLException ex) {
                            LOGGER.log(Level.WARNING, "", ex);
                        }
                    }
                    java.awt.Window parent = SwingUtilities.getWindowAncestor(this);
                    if (parent != null) {
                        parent.dispose();
                    } else {
                        this.setVisible(false);
                        this.setEnabled(false);
                    }
                }
            }
            // Si canceló el diálogo de cierre, no hacer nada (no cerrar la aplicación)
            return;
        }

        // No hay turno abierto, cerrar normalmente
        if (closeAppView()) {
            releaseResources();
            if (session != null) {
                try {
                    session.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, "", ex);
                }
            }
            java.awt.Window parent = SwingUtilities.getWindowAncestor(this);
            if (parent != null) {
                parent.dispose();
            } else {
                this.setVisible(false);
                this.setEnabled(false);
            }
        }
    }

    public boolean closeAppView() {

        if (m_principalapp == null) {
            return true;
        } else if (!m_principalapp.deactivate()) {
            return false;
        } else {
            // Sebastian - El perfil ahora está en el panel superior, no en jPanel3
            // jPanel3.remove(m_principalapp.getNotificator());
            jPanel3.revalidate();
            jPanel3.repaint();

            m_jPanelContainer.remove(m_principalapp);
            m_principalapp = null;

            // showLoginPanel();
            return true;
        }
    }

    private void showLoginPanel() {
        LOGGER.log(Level.INFO, "Mostrando el nuevo JLogonDialog...");

        java.awt.Window parentWindow = SwingUtilities.getWindowAncestor(this);

        // Sebastian - Ocultar la ventana principal para que el diálogo de login
        // aparezca igual que al inicio de la app (flotando, sin fondo blanco detrás)
        boolean wasVisible = false;
        if (parentWindow != null) {
            wasVisible = parentWindow.isVisible();
            parentWindow.setVisible(false);
        }

        // Mostrar el diálogo de login sin padre (null) para que flote igual que al
        // inicio
        JLogonDialog logonDialog = new JLogonDialog(null, m_dlSystem, session);
        logonDialog.setVisible(true);

        AppUser loggedUser = logonDialog.getLoggedUser();
        if (loggedUser != null) {
            // Login exitoso: volver a mostrar la ventana principal y abrir la vista
            if (parentWindow != null) {
                parentWindow.setVisible(true);
            }
            openAppView(loggedUser);
        } else {
            // Si el usuario cancela o cierra el diálogo de login, cerramos la aplicación
            LOGGER.log(Level.INFO, "Login cancelado o diálogo cerrado. Saliendo de la aplicación...");
            System.exit(0);
        }
    }

    // Método público para mostrar el panel de login desde otras clases
    public void showLoginPanelPublic() {
        showLoginPanel();
    }

    private void setStatusBarPanel() {
        String sWareHouse;

        try {
            sWareHouse = m_dlSystem.findLocationName(m_sInventoryLocation);
        } catch (BasicException e) {
            sWareHouse = "";
        }

        String url;
        try {
            url = session.getURL();
        } catch (SQLException e) {
            url = "";
        }
        String hostText = appFileProperties.getHost() + " ; WareHouse: " + sWareHouse + " | " + url;
        m_jHost.setText(hostText);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the FormEditor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        m_jPanelTitle = new javax.swing.JPanel();
        m_jLblTitle = new javax.swing.JLabel();
        poweredby = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        m_jPanelContainer = new javax.swing.JPanel();
        statusBarPanel = new javax.swing.JPanel();
        panelTask = new javax.swing.JPanel();
        m_jHost = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        m_jClose = new javax.swing.JButton();

        setEnabled(false);
        setPreferredSize(new java.awt.Dimension(1280, 800));
        setMinimumSize(new java.awt.Dimension(900, 600));
        setLayout(new java.awt.BorderLayout());

        m_jPanelTitle.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0,
                javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")));
        m_jPanelTitle.setPreferredSize(new java.awt.Dimension(449, 40));
        m_jPanelTitle.setLayout(new java.awt.BorderLayout());

        m_jLblTitle.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        m_jLblTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        m_jLblTitle.setText("Kriol Point of Sales (krPoS) - DESARROLLO EN TIEMPO REAL ✅");
        m_jPanelTitle.add(m_jLblTitle, java.awt.BorderLayout.CENTER);

        poweredby.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        poweredby.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 5));
        poweredby.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        poweredby.setMaximumSize(new java.awt.Dimension(180, 34));
        poweredby.setPreferredSize(new java.awt.Dimension(180, 34));
        m_jPanelTitle.add(poweredby, java.awt.BorderLayout.LINE_END);

        jLabel2.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(102, 102, 102));
        jLabel2.setPreferredSize(new java.awt.Dimension(180, 34));
        m_jPanelTitle.add(jLabel2, java.awt.BorderLayout.LINE_START);

        add(m_jPanelTitle, java.awt.BorderLayout.NORTH);

        m_jPanelContainer.setLayout(new java.awt.CardLayout());
        add(m_jPanelContainer, java.awt.BorderLayout.CENTER);

        // Sebastian - Ocultar la barra inferior completamente
        statusBarPanel.setBorder(null);
        statusBarPanel.setLayout(new javax.swing.BoxLayout(statusBarPanel, javax.swing.BoxLayout.LINE_AXIS));
        statusBarPanel.setPreferredSize(new java.awt.Dimension(0, 0)); // Sin altura
        statusBarPanel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 0));
        statusBarPanel.setVisible(false); // Ocultar completamente

        panelTask.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 2)); // Menos padding vertical (2px en
                                                                                      // lugar del default)

        // Sebastian - El host ahora está en el panel superior, no en la barra inferior
        m_jHost.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N - Fuente más pequeña
        m_jHost.setIcon(null); // Sin icono
        m_jHost.setText("*Hostname");
        // panelTask.add(m_jHost); // Ya no se agrega a la barra inferior

        statusBarPanel.add(panelTask);

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 5, 2)); // Menos padding vertical (2px en
                                                                                     // lugar del default)

        // Sebastian - El botón cerrar ahora está en el panel superior, no en la barra
        // inferior
        m_jClose.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N - Fuente más pequeña
        m_jClose.setIcon(null); // Sin icono
        m_jClose.setText(AppLocal.getIntString("button.exit")); // NOI18N
        m_jClose.setFocusPainted(false);
        m_jClose.setFocusable(false);
        m_jClose.setPreferredSize(new java.awt.Dimension(90, 30)); // Más pequeño y más delgado
        m_jClose.setMinimumSize(new java.awt.Dimension(80, 30));
        m_jClose.setMaximumSize(new java.awt.Dimension(100, 30));
        m_jClose.setRequestFocusEnabled(false);
        m_jClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jCloseActionPerformed(evt);
            }
        });
        // jPanel3.add(m_jClose); // Ya no se agrega a la barra inferior

        statusBarPanel.add(jPanel3);

        // Sebastian - La barra inferior está oculta, no se agrega al layout
        // add(statusBarPanel, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void m_jCloseActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jCloseActionPerformed
        tryToClose();
    }// GEN-LAST:event_m_jCloseActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JButton m_jClose;
    private javax.swing.JLabel m_jHost;
    private javax.swing.JLabel m_jLblTitle;
    private javax.swing.JPanel m_jPanelContainer;
    private javax.swing.JPanel m_jPanelTitle;
    private javax.swing.JPanel panelTask;
    private javax.swing.JLabel poweredby;
    private javax.swing.JPanel statusBarPanel;
    // End of variables declaration//GEN-END:variables
}
