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
        com.openbravo.pos.forms.AppConfig dlConfig = new com.openbravo.pos.forms.AppConfig(null);
        dlConfig.load();
        com.openbravo.pos.firebase.FirebaseDownloadManagerREST downloader =
            new com.openbravo.pos.firebase.FirebaseDownloadManagerREST(session, dlConfig);
        java.util.Map<String, Boolean> selections = new java.util.HashMap<>();
        selections.put("cierres", true);
        selections.put("usuarios", true);
        selections.put("clientes", true);
        selections.put("categorias", true);
        selections.put("productos", true);
        selections.put("ventas", true);
        selections.put("puntos_historial", true);
        selections.put("formas_de_pago", true);
        selections.put("impuestos", true);
        selections.put("config", true);
        selections.put("inventario", true);
        selections.put("ticketlines", true);
        downloader.performSelectedDownload(selections).join();

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
                // Sebastian - Solo inicializar caja sin monto inicial (se pedirá después del login)
                setActiveCash(UUID.randomUUID().toString(),
                        m_dlSystem.getSequenceCash(appFileProperties.getHost()) + 1, new Date(), null, 0.0);
                m_dlSystem.execInsertCash(
                        new Object[]{getActiveCashIndex(), appFileProperties.getHost(),
                            getActiveCashSequence(),
                            getActiveCashDateStart(),
                            getActiveCashDateEnd(), 0.0}); // Sebastian - Monto inicial temporal
                needsInitialCashSetup = true; // Sebastian - Marcar que necesita configuración de fondo
                return false; // Continuar normalmente
            } else {
                // Sebastian - Recuperar fondo inicial guardado (si existe)
                LOGGER.log(Level.INFO, "🔍 Sebastian - Datos de caja recuperados - Array length: " + valcash.length);
                for (int i = 0; i < valcash.length; i++) {
                    LOGGER.log(Level.INFO, "🔍 Sebastian - valcash[" + i + "] = " + valcash[i] + " (tipo: " + (valcash[i] != null ? valcash[i].getClass().getSimpleName() : "null") + ")");
                }
                
                double savedInitialAmount = valcash.length > 5 ? (Double) valcash[5] : 0.0;
                setActiveCash(sActiveCashIndex,
                        (Integer) valcash[1],
                        (Date) valcash[2],
                        (Date) valcash[3],
                        savedInitialAmount);
                
                LOGGER.log(Level.INFO, "💰 Sebastian POS - Caja activa recuperada con fondo inicial: $" + savedInitialAmount);
                
                // Sebastian - Solo necesita configuración si el fondo inicial es 0 o null
                needsInitialCashSetup = (savedInitialAmount <= 0.0);
                
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
        if (closeAppView()) {

            m_principalapp = new JPrincipalApp(this, user);

            jPanel3.add(m_principalapp.getNotificator());
            jPanel3.revalidate();

            String viewID = "_" + m_principalapp.getUser().getId();
            m_jPanelContainer.add(m_principalapp, viewID);
            showView(viewID);

            m_principalapp.activate();
            
            // Sebastian - Solo verificar monto inicial si realmente necesita configuración
            if (needsInitialCashSetup) {
                LOGGER.log(Level.INFO, "🔧 Sebastian - Configurando fondo inicial para nueva caja o caja sin fondo configurado");
                checkAndSetupInitialCash();
            } else {
                LOGGER.log(Level.INFO, "✅ Sebastian - Caja activa ya tiene fondo inicial configurado, omitiendo solicitud");
            }
            
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
            if (session != null && m_principalapp != null) {
                // Crear el FirebaseSyncManagerREST con la sesión actual y el AppUserView
                firebaseSyncManager = new FirebaseSyncManagerREST(session, m_principalapp);
                
                // Inicializar Firebase con el usuario actual
                com.openbravo.pos.firebase.FirebaseServiceREST firebaseService = 
                    com.openbravo.pos.firebase.FirebaseServiceREST.getInstance();
                firebaseService.initialize(appFileProperties, m_principalapp);
                
                // Ejecutar sincronización completa en segundo plano
                CompletableFuture.runAsync(() -> {
                    try {
                        LOGGER.info("supabase sync iniciado automáticamente después del login");
                        com.openbravo.pos.forms.AppConfig dlConfig = new com.openbravo.pos.forms.AppConfig(null);
                        dlConfig.load();
                        com.openbravo.pos.firebase.FirebaseDownloadManagerREST downloader =
                            new com.openbravo.pos.firebase.FirebaseDownloadManagerREST(session, dlConfig);
                        java.util.Map<String, Boolean> selections = new java.util.HashMap<>();
                        selections.put("cierres", true);
                        selections.put("usuarios", true);
                        selections.put("clientes", true);
                        selections.put("categorias", true);
                        selections.put("productos", true);
                        selections.put("ventas", true);
                        selections.put("puntos_historial", true);
                        selections.put("formas_de_pago", true);
                        selections.put("impuestos", true);
                        selections.put("config", true);
                        selections.put("inventario", true);
                        selections.put("ticketlines", true);
                        downloader.performSelectedDownload(selections).join();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error al ejecutar sincronización completa con supabase: " + e.getMessage(), e);
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al inicializar Firebase Sync Manager: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sebastian - Configurar monto inicial (solo se llama cuando realmente se necesita)
     */
    private void checkAndSetupInitialCash() {
        LOGGER.log(Level.INFO, "� Sebastian - Solicitando configuración de fondo inicial para caja: " + getActiveCashIndex());
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
            dialog.setInitialAmount(4000.0);
            
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
            LOGGER.log(Level.INFO, "🔄 Sebastian - Actualizando monto inicial en BD. Caja: " + activeCashIndex + ", Monto: $" + initialAmount);
            
            // Actualizar en la base de datos
            try {
                m_dlSystem.execUpdateCashInitialAmount(activeCashIndex, initialAmount);
                LOGGER.log(Level.INFO, "✅ Sebastian - Monto inicial actualizado en BD correctamente");
            } catch (Exception sqlEx) {
                LOGGER.log(Level.SEVERE, "❌ Sebastian - ERROR CRÍTICO en execUpdateCashInitialAmount: " + sqlEx.getMessage(), sqlEx);
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
                    LOGGER.log(Level.WARNING, "❌ Sebastian - Verificación FALLÓ: BD muestra $" + savedAmount + ", esperaba $" + initialAmount);
                }
            }
            
            // Actualizar en memoria
            setActiveCashInitialAmount(initialAmount);
            LOGGER.log(Level.INFO, "✅ Sebastian - Monto inicial actualizado en memoria correctamente");
            
            // Sebastian - Marcar que ya no necesita configuración de fondo inicial
            needsInitialCashSetup = false;
            
            LOGGER.log(Level.INFO, "✅ Sebastian - PROCESO COMPLETO: Monto inicial actualizado: $" + initialAmount);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "❌ Sebastian - Error actualizando monto inicial: " + ex.getMessage(), ex);
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
            jPanel3.remove(m_principalapp.getNotificator());
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
}
