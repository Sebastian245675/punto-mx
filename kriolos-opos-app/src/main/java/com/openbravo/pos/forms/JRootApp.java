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
import java.io.*;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import javax.swing.SwingUtilities;
import java.io.IOException;
import com.openbravo.beans.JDoubleDialog;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import org.openide.util.Exceptions;

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
        //TODO load Windows Title 
        //m_jLblTitle.setText(m_dlSystem.getResourceAsText("Window.Title"));
        //m_jLblTitle.repaint();
    }

    public void initApp() throws BasicException{

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
        }catch(BasicException ex) {
            throw new BasicException("Database verification fail", ex);
        }

        logStartup();

        hostSavedProperties = m_dlSystem.getResourceAsProperties(getHostID());

        if (checkActiveCash()) {
            LOGGER.log(Level.WARNING, "Fail on verify ActiveCash");
            throw new BasicException("Fail on verify ActiveCash");
        }

        setInventoryLocation();

        initPeripheral();

        setTitlePanel();

        setStatusBarPanel();

        showLoginPanel();
    }

    private void setTitlePanel() {

        /*Timer show Date Hour:min:seg
        javax.swing.Timer clockTimer = new javax.swing.Timer(1000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                String m_clock = getLineTimer();
                String m_date = getLineDate();
                jLabel2.setText("  " + m_date + " " + m_clock);
            }
        });

        clockTimer.start();*/
        
        //Remove Panel Title
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
            if (valcash == null || !appFileProperties.getHost().equals(valcash[0])) {
                setActiveCash(UUID.randomUUID().toString(),
                        m_dlSystem.getSequenceCash(appFileProperties.getHost()) + 1, new Date(), null);
                m_dlSystem.execInsertCash(
                        new Object[]{getActiveCashIndex(), appFileProperties.getHost(),
                            getActiveCashSequence(),
                            getActiveCashDateStart(),
                            getActiveCashDateEnd()});
            } else {
                setActiveCash(sActiveCashIndex,
                        (Integer) valcash[1],
                        (Date) valcash[2],
                        (Date) valcash[3]);
            }
        } catch (BasicException e) {
            MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE,
                    AppLocal.getIntString("message.cannotclosecash"), e);
            msg.show(this);
            return true;
        }
        return false;
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

        hostSavedProperties.setProperty("activecash", activeCash.getCashIndex());
        m_dlSystem.setResourceAsProperties(getHostID(), hostSavedProperties);
    }

    // Sebastian - Implementación del método con fondo inicial
    @Override
    public void setActiveCash(String sIndex, int iSeq, Date dStart, Date dEnd, double initialAmount) {
        activeCash.setCashIndex(sIndex);
        activeCash.setCashSequence(iSeq);
        activeCash.setCashDateStart(dStart);
        activeCash.setCashDateEnd(dEnd);
        activeCash.setInitialAmount(initialAmount); // Sebastian - Establecer fondo inicial

        hostSavedProperties.setProperty("activecash", activeCash.getCashIndex());
        m_dlSystem.setResourceAsProperties(getHostID(), hostSavedProperties);
    }

    // Sebastian - Implementación del método para obtener fondo inicial
    @Override
    public double getActiveCashInitialAmount() {
        return activeCash.getInitialAmount();
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
            }
            catch (ScriptException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void showView(String view) {
        CardLayout cl = (CardLayout) (m_jPanelContainer.getLayout());
        cl.show(m_jPanelContainer, view);
    }

    private void openAppView(AppUser user) {

        LOGGER.log(Level.WARNING, "INFO :: showMainAppPanel");
        
        // 🔍 Sebastian - Debug: Verificar rol del usuario
        LOGGER.log(Level.INFO, "🔍 DEBUG - Usuario: " + user.getName() + ", Rol ID: '" + user.getRole() + "'");
        
        // 💰 Sebastian - Verificar si es empleado/manager usando IDs de roles de la BD
        // Roles en BD: '0'=Administrator, '1'=Manager, '2'=Employee, '3'=Guest
        if ("2".equals(user.getRole()) || "1".equals(user.getRole())) {
            String roleName = "2".equals(user.getRole()) ? "EMPLOYEE" : "MANAGER";
            LOGGER.log(Level.INFO, "💰 Solicitando dinero inicial para: " + user.getName() + " (Rol ID: " + user.getRole() + " = " + roleName + ")");
            
            // ⚠️ Este método es OBLIGATORIO y siempre retorna un valor válido
            Double dineroInicial = solicitarDineroInicialCaja(user);
            
            // Verificación adicional por seguridad
            if (dineroInicial == null) {
                LOGGER.log(Level.SEVERE, "🚨 ERROR CRÍTICO: dineroInicial es null después del método obligatorio");
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                    "� Error crítico en el sistema.\nNo se pudo registrar el dinero inicial.\nContacta al administrador.");
                msg.show(this);
                return; // No continuar sin dinero inicial
            }
            
            // Registrar el dinero inicial en el sistema
            registrarDineroInicialCaja(user, dineroInicial);
            
        } else {
            String roleName = "0".equals(user.getRole()) ? "ADMINISTRATOR" : "3".equals(user.getRole()) ? "GUEST" : "UNKNOWN";
            LOGGER.log(Level.INFO, "ℹ️ Usuario " + user.getName() + " con rol ID '" + user.getRole() + "' (" + roleName + ") no requiere dinero inicial");
        }
        
        if (closeAppView()) {

            m_principalapp = new JPrincipalApp(this, user);

            // 🔧 Sebastian - Limpiar jPanel3 antes de agregar para evitar duplicados
            jPanel3.removeAll();
            jPanel3.add(m_principalapp.getNotificator());
            // Reagregar el botón de cerrar que se eliminó con removeAll()
            jPanel3.add(m_jClose);
            jPanel3.revalidate();
            jPanel3.repaint();

            String viewID = "_" + m_principalapp.getUser().getId();
            m_jPanelContainer.add(m_principalapp, viewID);
            showView(viewID);

            m_principalapp.activate();
            
            // Inicializar Firebase Sync Manager después de activar la aplicación principal
            initializeFirebaseSync();
        }
    }
    
    /**
     * Inicializa el Firebase Sync Manager y ejecuta la sincronización automática
     * si hay conexión a internet y Firebase está configurado.
     */
    private void initializeFirebaseSync() {
        try {
            if (session != null) {
                // Crear el FirebaseSyncManagerREST con la sesión actual
                firebaseSyncManager = new FirebaseSyncManagerREST(session);
                
                // Ejecutar sincronización completa en segundo plano
                CompletableFuture.runAsync(() -> {
                    try {
                        LOGGER.info("Firebase sync iniciado automáticamente después del login");
                        firebaseSyncManager.performFullSync().join();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error al ejecutar sincronización completa con Firebase: " + e.getMessage(), e);
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al inicializar Firebase Sync Manager: " + e.getMessage(), e);
        }
    }
    
    //Release hardware,files,...
    private void releaseResources() {
        if(m_DeviceTicket != null){
            m_DeviceTicket.getDeviceDisplay().clearVisor();
        }
        
        // Limpiar el Firebase Sync Manager si existe
        if(firebaseSyncManager != null) {
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

        if (closeAppView()) {
            releaseResources();
            if(session != null){
                try {
                    session.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, "", ex);
                }
            }
            java.awt.Window parent = SwingUtilities.getWindowAncestor(this);
            if(parent != null){
                parent.dispose();
            }else {
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
            // 🔧 Sebastian - Limpiar completamente jPanel3 para evitar duplicados
            jPanel3.removeAll();
            jPanel3.revalidate();
            jPanel3.repaint();

            m_jPanelContainer.remove(m_principalapp);
            m_principalapp = null;

            //showLoginPanel();
            return true;
        }
    }

    private void showLoginPanel() {
        LOGGER.log(Level.WARNING, "INFO :: showLoginPanel");
        if (mAuthPanel == null) {
            mAuthPanel = new JAuthPanel(m_dlSystem, new JAuthPanel.AuthListener() {
                @Override
                public void onSucess(AppUser user) {
                    openAppView(user);
                }
            });
            m_jPanelContainer.add(mAuthPanel, "login");
        }
        showView("login");
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
        m_jHost.setText("<html>" + appFileProperties.getHost() + " ;<b>WareHouse<b>: " + sWareHouse + "<br>" + url + "</html>");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the FormEditor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
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
        setPreferredSize(new java.awt.Dimension(1024, 768));
        setLayout(new java.awt.BorderLayout());

        m_jPanelTitle.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")));
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

        statusBarPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")));
        statusBarPanel.setLayout(new javax.swing.BoxLayout(statusBarPanel, javax.swing.BoxLayout.LINE_AXIS));

        panelTask.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        m_jHost.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        m_jHost.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/display.png"))); // NOI18N
        m_jHost.setText("*Hostname");
        m_jHost.setMaximumSize(new java.awt.Dimension(200, 32));
        m_jHost.setMinimumSize(new java.awt.Dimension(200, 32));
        m_jHost.setPreferredSize(new java.awt.Dimension(200, 32));
        panelTask.add(m_jHost);

        statusBarPanel.add(panelTask);

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        m_jClose.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jClose.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/exit.png"))); // NOI18N
        m_jClose.setText(AppLocal.getIntString("button.exit")); // NOI18N
        m_jClose.setFocusPainted(false);
        m_jClose.setFocusable(false);
        m_jClose.setPreferredSize(new java.awt.Dimension(100, 50));
        m_jClose.setRequestFocusEnabled(false);
        m_jClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jCloseActionPerformed(evt);
            }
        });
        jPanel3.add(m_jClose);

        statusBarPanel.add(jPanel3);

        add(statusBarPanel, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void m_jCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jCloseActionPerformed
        tryToClose();
    }//GEN-LAST:event_m_jCloseActionPerformed


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

    // 💰 Sebastian - Métodos para manejo de dinero inicial en caja
    
    /**
     * Solicita al empleado/manager que ingrese el dinero inicial en caja
     * @param user Usuario que está iniciando sesión
     * @return Monto de dinero inicial (OBLIGATORIO - no puede ser null)
     */
    private Double solicitarDineroInicialCaja(AppUser user) {
        Double monto = null;
        boolean esObligatorio = true;
        
        while (monto == null && esObligatorio) {
            try {
                LOGGER.log(Level.INFO, "🚀 INICIANDO solicitarDineroInicialCaja para: " + user.getName());
                
                String titulo = "💰 Dinero Inicial - " + user.getName() + " (OBLIGATORIO)";
                String mensaje = "¡Hola " + user.getName() + "!\n\n" +
                               "🎯 CAMPO OBLIGATORIO: Debes ingresar el monto\n" +
                               "    de dinero que hay en la caja para comenzar\n" +
                               "    con las ventas del día.\n\n" +
                               "💡 Ejemplo: 100.00\n" +
                               "⚠️ No puedes continuar sin ingresar este dato.";
                
                LOGGER.log(Level.INFO, "📱 Mostrando JDoubleDialog...");
                monto = JDoubleDialog.showComponent(this, titulo, mensaje);
                LOGGER.log(Level.INFO, "📱 JDoubleDialog cerrado. Monto: " + monto);
                
                if (monto != null && monto >= 0) {
                    LOGGER.log(Level.INFO, 
                        String.format("💰 Dinero inicial ingresado: $%.2f - Usuario: %s", 
                        monto, user.getName()));
                    return monto; // Valor válido, salir del bucle
                } else if (monto != null && monto < 0) {
                    MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                        "❌ El monto no puede ser negativo.\nPor favor, ingresa un valor válido mayor o igual a 0.");
                    msg.show(this);
                    monto = null; // Resetear para continuar el bucle
                } else {
                    // Usuario canceló - mostrar advertencia y volver a preguntar
                    MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                        "⚠️ CAMPO OBLIGATORIO\n\n" +
                        "Debes ingresar el dinero inicial para continuar.\n" +
                        "Este campo es obligatorio para empleados y gerentes.\n\n" +
                        "Si no hay dinero en caja, ingresa 0 (cero).");
                    msg.show(this);
                    monto = null; // Continuar el bucle
                }
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al solicitar dinero inicial", e);
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                    "❌ Error al solicitar dinero inicial: " + e.getMessage() + 
                    "\n\nIntentando nuevamente...");
                msg.show(this);
                monto = null; // Continuar el bucle en caso de error
            }
        }
        
        // Esta línea nunca debería ejecutarse debido al bucle while
        return monto != null ? monto : 0.0;
    }
    
    /**
     * Registra el dinero inicial en el sistema de caja
     * @param user Usuario que registra el dinero
     * @param monto Monto de dinero inicial
     */
    private void registrarDineroInicialCaja(AppUser user, Double monto) {
        try {
            // Crear un registro en draweropened para marcar el dinero inicial
            // Usar ID del usuario para consistencia con la base de datos
            m_dlSystem.execDrawerOpened(new Object[] {
                user.getId(),  // ✅ Corregido: usar ID en lugar de nombre
                String.format("Dinero Inicial: $%.2f por %s", monto, user.getName())
            });
            
            LOGGER.log(Level.INFO, 
                String.format("✅ Dinero inicial registrado: $%.2f por %s (ID: %s)", 
                monto, user.getName(), user.getId()));
            
            // Mostrar confirmación al usuario
            MessageInf msg = new MessageInf(MessageInf.SGN_SUCCESS,
                String.format("✅ Dinero inicial registrado exitosamente\n\n" +
                             "💰 Monto: $%.2f\n" +
                             "👤 Usuario: %s\n" +
                             "🆔 ID: %s\n" +
                             "🕐 Hora: %s", 
                             monto, user.getName(), user.getId(),
                             new java.text.SimpleDateFormat("HH:mm:ss").format(new Date())));
            msg.show(this);
            
        } catch (BasicException e) {
            LOGGER.log(Level.WARNING, "Error al registrar dinero inicial", e);
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                "⚠️ Advertencia: No se pudo registrar el dinero inicial en el sistema.\n" +
                "El sistema funcionará normalmente, pero recomendamos\n" +
                "anotar el monto manualmente.\n\nError: " + e.getMessage());
            msg.show(this);
        }
    }
}
