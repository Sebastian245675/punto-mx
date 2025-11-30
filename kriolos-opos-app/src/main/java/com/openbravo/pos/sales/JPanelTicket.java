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
package com.openbravo.pos.sales;

import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.basic.BasicException;
import com.openbravo.beans.JPasswordDialog;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.gui.ListKeyed;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.pos.customers.CustomerInfo;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.customers.CustomerInfoGlobal;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.customers.JCustomerFinder;
import com.openbravo.pos.customers.JDialogNewCustomer;
// Sebastian - Importaciones del sistema de puntos
import com.openbravo.pos.customers.PuntosDataLogic;
import com.openbravo.pos.customers.PuntosConfiguracion;
import com.openbravo.pos.forms.*;
import com.openbravo.pos.inventory.ProductStock;
import com.openbravo.format.Formats;
import java.text.MessageFormat;
import com.openbravo.pos.inventory.ProductsBundleInfo;
import com.openbravo.pos.inventory.TaxCategoryInfo;
import com.openbravo.pos.panels.JProductFinder;
import com.openbravo.pos.payment.JPaymentSelect;
import com.openbravo.pos.payment.JPaymentSelectReceipt;
import com.openbravo.pos.payment.JPaymentSelectRefund;
import com.openbravo.pos.printer.TicketParser;
import com.openbravo.pos.printer.TicketPrinterException;
import com.openbravo.pos.printer.screen.DeviceDisplayAdvance;
import com.openbravo.pos.reports.JPanelReport;
import com.openbravo.pos.sales.restaurant.RestaurantDBUtils;
import com.openbravo.pos.scale.ScaleException;
import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptException;
import com.openbravo.pos.scripting.ScriptFactory;
import com.openbravo.pos.ticket.ProductInfoExt;
import com.openbravo.pos.ticket.TaxInfo;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import com.openbravo.pos.util.InactivityListener;
import com.openbravo.pos.reports.JRPrinterAWT300;
import com.openbravo.pos.util.ReportUtils;

import java.awt.*;

import static java.awt.Window.getWindows;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.print.PrintService;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapArrayDataSource;

/**
 *
 * @author JG uniCenta
 */
public abstract class JPanelTicket extends JPanel implements JPanelView, TicketsEditor {

    protected final static System.Logger LOGGER = System.getLogger(JPanelTicket.class.getName());

    private final static int NUMBERZERO = 0;
    private final static int NUMBERVALID = 1;

    private final static int NUMBER_INPUTZERO = 0;
    private final static int NUMBER_INPUTZERODEC = 1;
    private final static int NUMBER_INPUTINT = 2;
    private final static int NUMBER_INPUTDEC = 3;
    private final static int NUMBER_PORZERO = 4;
    private final static int NUMBER_PORZERODEC = 5;
    private final static int NUMBER_PORINT = 6;
    private final static int NUMBER_PORDEC = 7;
    private final static long serialVersionUID = 1L;

    protected JTicketLines m_ticketlines;
    protected JPanelButtons m_jbtnconfig;
    protected AppView m_App;
    protected DataLogicSystem dlSystem;
    protected DataLogicSales dlSales;
    protected DataLogicCustomers dlCustomers;
    // Sebastian - Sistema de puntos
    protected PuntosDataLogic puntosDataLogic;
    protected TicketsEditor m_panelticket;
    protected TicketInfo m_oTicket;
    protected String m_oTicketExt;

    private int m_iNumberStatus;
    private int m_iNumberStatusInput;
    private int m_iNumberStatusPor;
    private StringBuffer m_sBarcode;

    private JTicketsBag m_ticketsbag;
    private TicketParser m_TTP;
    private SentenceList senttax;
    private ListKeyed taxcollection;

    private SentenceList senttaxcategories;
    // private ListKeyed taxcategoriescollection;
    private ComboBoxValModel taxcategoriesmodel;
    private TaxesLogic taxeslogic;
    private JPaymentSelect paymentdialogreceipt;
    private JPaymentSelect paymentdialogrefund;
    private InactivityListener listener;
    private DataLogicReceipts dlReceipts = null;
    private Boolean priceWith00;
    private RestaurantDBUtils restDB;
    private AppProperties m_config;
    // private Integer count = 0;
    // private Integer oCount = 0;

    /**
     * Creates new form JTicketView
     */
    public JPanelTicket(AppView app) {

        initComponents();

        LOGGER.log(System.Logger.Level.DEBUG, "JPanelTicket.init");
        m_config = app.getProperties();

        m_App = app;
        restDB = new RestaurantDBUtils(m_App);

        dlSystem = (DataLogicSystem) m_App.getBean("com.openbravo.pos.forms.DataLogicSystem");
        dlSales = (DataLogicSales) m_App.getBean("com.openbravo.pos.forms.DataLogicSales");
        dlCustomers = (DataLogicCustomers) m_App.getBean("com.openbravo.pos.customers.DataLogicCustomers");
        dlReceipts = (DataLogicReceipts) app.getBean("com.openbravo.pos.sales.DataLogicReceipts");

        // Sebastian - Inicializar sistema de puntos
        try {
            System.out.println("🔧 Inicializando sistema de puntos...");
            puntosDataLogic = new PuntosDataLogic(m_App.getSession());
            puntosDataLogic.verificarSistemaPuntos(); // Esto incluye initTables() y verificación completa
            System.out.println("✅ Sistema de puntos inicializado correctamente");
        } catch (Exception e) {
            System.err.println("❌ Error inicializando sistema de puntos: " + e.getMessage());
            e.printStackTrace();
            LOGGER.log(System.Logger.Level.ERROR, "Error inicializando sistema de puntos: " + e.getMessage());
        }

        // Configuration>Peripheral options
        m_jbtnScale.setVisible(m_App.getDeviceScale().existsScale());
        // Sebastian - Mostrar panel de scripts con solo dos botones personalizados
        m_jPanelScripts.setVisible(true);

        jTBtnShow.setSelected(false);

        // Scanner ahora está en la parte superior, no en m_jPanEntries
        // if (Boolean.valueOf(getAppProperty("till.amountattop"))) {
        // m_jPanEntries.remove(jPanelScanner);
        // m_jPanEntries.remove(m_jNumberKeys);
        // m_jPanEntries.add(jPanelScanner);
        // m_jPanEntries.add(m_jNumberKeys);
        // }

        priceWith00 = ("true".equals(getAppProperty("till.pricewith00")));

        if (priceWith00) {
            m_jNumberKeys.dotIs00(true);
        }

        LOGGER.log(System.Logger.Level.DEBUG, "JPanelTicket.init: criar: Ticket.Line");
        m_ticketlines = new JTicketLines(dlSystem.getResourceAsXML(TicketConstants.RES_TICKET_LINES));
        // Configurar callback para eliminar líneas con Delete al pasar el mouse
        m_ticketlines.setDeleteLineCallback((int rowIndex) -> {
            if (m_oTicket != null && rowIndex >= 0 && rowIndex < m_oTicket.getLinesCount()) {
                removeTicketLine(rowIndex);
            }
        });
        m_jPanelLines.add(m_ticketlines, java.awt.BorderLayout.CENTER);
        m_TTP = new TicketParser(m_App.getDeviceTicket(), dlSystem);

        senttax = dlSales.getTaxList();
        senttaxcategories = dlSales.getTaxCategoriesList();
        taxcategoriesmodel = new ComboBoxValModel();

        stateToZero();

        m_oTicket = null;
        m_oTicketExt = null;
        jCheckStock.setText(AppLocal.getIntString("message.title.checkstock"));

        initExtButtons();

        initComponentFromChild();

        initDeviceDisplay();

        // Apply modern look and feel styles to the ticket panel (non-fatal)
        try {
            com.openbravo.pos.util.ModernLookAndFeel.aplicarEstiloModerno();
            com.openbravo.pos.util.ModernLookAndFeel.estilizarComponentes(this);
        } catch (Throwable t) {
            LOGGER.log(System.Logger.Level.WARNING, "No se pudo aplicar estilo moderno: " + t.getMessage());
        }

        // Sebastian - Configurar atajos de teclado
        setupKeyboardShortcuts();
    }

    /**
     * Sebastian - Configura los atajos de teclado para el módulo de ventas
     * F12: Cobrar/Pagar
     * F2: Ingresar cliente (simula clic en botón)
     * F3: Historial de pestañas
     * F4: Nueva pestaña de venta
     */
    private void setupKeyboardShortcuts() {
        javax.swing.InputMap inputMap = this.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap actionMap = this.getActionMap();

        // F12: Cobrar/Pagar
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F12, 0), "cobrar");
        actionMap.put("cobrar", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (m_oTicket != null && m_oTicket.getLinesCount() > 0) {
                    if (m_jPayNow != null && m_jPayNow.isEnabled()) {
                        LOGGER.log(System.Logger.Level.DEBUG, "F12 → Cobrar: Simulando clic en botón Pagar");
                        m_jPayNow.doClick();
                    }
                } else {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                }
            }
        });

        // F2: Ingresar cliente (simula clic en jBtnCustomer)
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0), "ingresarCliente");
        actionMap.put("ingresarCliente", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (jBtnCustomer != null && jBtnCustomer.isEnabled()) {
                    LOGGER.log(System.Logger.Level.DEBUG, "F2 → Ingresar Cliente: Simulando clic en botón de cliente");
                    jBtnCustomer.doClick();
                } else {
                    LOGGER.log(System.Logger.Level.WARNING, "F2 → Botón de cliente no disponible");
                }
            }
        });

        // F3: Historial de pestañas
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0), "historialPestañas");
        actionMap.put("historialPestañas", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (m_ticketsbag != null) {
                    LOGGER.log(System.Logger.Level.DEBUG, "F3 → Historial: Activando lista de tickets");
                    m_ticketsbag.activate();
                }
            }
        });

        // F4: Nueva pestaña de venta
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0), "nuevaPestaña");
        actionMap.put("nuevaPestaña", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                LOGGER.log(System.Logger.Level.DEBUG, "F4 → Nueva Pestaña: Creando nuevo ticket");
                createNewTicket();
            }
        });

        LOGGER.log(System.Logger.Level.INFO,
                "✅ Atajos de teclado configurados: F12=Cobrar, F2=Cliente, F3=Historial, F4=Nueva");
    }

    private void initExtButtons() {
        // Script event buttons - mantener funcionalidad original pero oculta
        String resourceName = TicketConstants.RES_TICKET_BUTTONS;

        String sConfigRes = getResourceAsXML(resourceName);

        if (sConfigRes == null || sConfigRes.isBlank()) {
            LOGGER.log(System.Logger.Level.WARNING, "No found XML resource: " + resourceName);
            sConfigRes = "";
        }

        ScriptArg sa1 = new ScriptArg("ticket", m_oTicket);
        ScriptArg sa2 = new ScriptArg("user", m_App.getAppUserView().getUser());
        ScriptArg sa3 = new ScriptArg("sales", this);

        m_jbtnconfig = new JPanelButtons(m_App, new JPanelButtons.JPanelButtonListener() {
            @Override
            public void eval(String resource) {

                LOGGER.log(System.Logger.Level.INFO, "Rrocessing code (resource id): " + resource);
                evalScriptAndRefresh(resource, sa1, sa2, sa3);
            }

            @Override
            public void print(String resource) {

                LOGGER.log(System.Logger.Level.INFO, "Rrocessing template (resource id): " + resource);
                printTicket(resource);
            }
        }, sConfigRes);

        // Sebastian - Ocultar el panel original de botones
        m_jbtnconfig.setVisible(false);

        m_jPanelBagExt.add(m_jbtnconfig);

        // Sebastian - Agregar los 3 botones personalizados: Cliente, Historial y
        // Entradas/Salidas
        javax.swing.JPanel panelBotones = new javax.swing.JPanel();
        panelBotones.setLayout(new java.awt.GridLayout(3, 1, 5, 5));
        panelBotones.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Botón 1 - Cliente
        javax.swing.JButton btnCliente = new javax.swing.JButton("Cliente");
        btnCliente.setPreferredSize(new java.awt.Dimension(120, 40));
        btnCliente.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 11));
        btnCliente.setBackground(new java.awt.Color(70, 130, 180));
        btnCliente.setForeground(java.awt.Color.WHITE);
        btnCliente.setFocusPainted(false);
        try {
            btnCliente.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/customer.png")));
        } catch (Exception e) {
            // Ignorar si no se encuentra la imagen
        }
        btnCliente.addActionListener(e -> {
            javax.swing.JOptionPane.showMessageDialog(this, "Función de Cliente", "Cliente",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
        });

        // Botón 2 - Historial
        javax.swing.JButton btnHistorial = new javax.swing.JButton("Historial");
        btnHistorial.setPreferredSize(new java.awt.Dimension(120, 40));
        btnHistorial.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 11));
        btnHistorial.setBackground(new java.awt.Color(34, 139, 34));
        btnHistorial.setForeground(java.awt.Color.WHITE);
        btnHistorial.setFocusPainted(false);
        try {
            btnHistorial.setIcon(
                    new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/ticket_print.png")));
        } catch (Exception e) {
            // Ignorar si no se encuentra la imagen
        }
        btnHistorial.addActionListener(e -> {
            javax.swing.JOptionPane.showMessageDialog(this, "Función de Historial", "Historial",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
        });

        // Botón 3 - Entradas y Salidas
        javax.swing.JButton btnEntradasSalidas = new javax.swing.JButton(
                "<html><center>Entradas<br/>y Salidas</center></html>");
        btnEntradasSalidas.setPreferredSize(new java.awt.Dimension(120, 40));
        btnEntradasSalidas.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 10));
        btnEntradasSalidas.setBackground(new java.awt.Color(255, 140, 0)); // Color naranja
        btnEntradasSalidas.setForeground(java.awt.Color.WHITE);
        btnEntradasSalidas.setFocusPainted(false);
        try {
            // Intentar usar el icono de pagos si existe
            btnEntradasSalidas
                    .setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/payments.png")));
        } catch (Exception e) {
            // Si no existe, usar un icono alternativo o sin icono
            try {
                btnEntradasSalidas.setIcon(
                        new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/calculator.png")));
            } catch (Exception ex) {
                // Sin icono si no se encuentra ninguno
            }
        }
        btnEntradasSalidas.addActionListener(e -> {
            try {
                // Abrir el panel de Entradas y Salidas
                m_App.getAppUserView().showTask("com.openbravo.pos.panels.JPanelPayments");
            } catch (Exception ex) {
                LOGGER.log(System.Logger.Level.ERROR, "Error al abrir panel de Entradas y Salidas: " + ex.getMessage(),
                        ex);
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Error al abrir Entradas y Salidas: " + ex.getMessage(),
                        "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });

        panelBotones.add(btnCliente);
        panelBotones.add(btnHistorial);
        panelBotones.add(btnEntradasSalidas);
        m_jPanelBagExt.add(panelBotones);

        // Sebastian - Hacer visible el panel para mostrar los 2 botones nuevos
        m_jPanelBagExt.setVisible(true);
    }

    private void initComponentFromChild() {

        // Set Configuration>General>Tickets toolbar simple : standard : restaurant
        // option
        m_ticketsbag = getJTicketsBag();
        // Sebastian - Mantener el componente de bolsas de tickets visible para poder
        // cambiar entre ventas
        m_ticketsbag.getBagComponent().setVisible(true);
        m_jPanelBag.add(m_ticketsbag.getBagComponent(), BorderLayout.LINE_START);
        add(m_ticketsbag.getNullComponent(), "null");

        m_jPanelCatalog.add(getSouthComponent(), BorderLayout.CENTER);
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    private String getTicketsbag() {
        return getAppProperty("machine.ticketsbag");
    }

    private boolean isRestaurantMode() {
        return "restaurant".equals(getTicketsbag());
    }

    private boolean isAutoLogoutRestaurant() {
        return "true".equals(getAppProperty("till.autoLogoffrestaurant"));
    }

    private boolean isAutoLogout() {
        return "true".equals(getAppProperty("till.autoLogoff"));
    }

    private void closeAllDialogs() {
        Window[] windows = getWindows();

        for (Window window : windows) {
            if (window instanceof JDialog) {
                window.dispose();
            }
        }
    }

    private void saveCurrentTicket() {

        if (m_oTicket != null) {
            String currentTicket = m_oTicket.getId();
            try {
                dlReceipts.updateSharedTicket(currentTicket, m_oTicket, m_oTicket.getPickupId());
            } catch (BasicException ex) {
                LOGGER.log(System.Logger.Level.ERROR, "Exception on save current ticket: " + currentTicket, ex);
            }
        }
    }

    /**
     *
     * @throws BasicException
     */
    @Override
    public void activate() throws BasicException {

        LOGGER.log(System.Logger.Level.INFO, "JPanelTicket.activate");

        // Aplicar fuentes grandes para campos numéricos (después de que Metal los
        // sobrescriba)
        aplicarFuentesGrandesVentas();

        Action logoutAction = new LogoutAction();
        if (isAutoLogout()) {
            try {
                int delay = Integer.parseInt(getAppProperty("till.autotimer"));
                delay *= 1000;
                // Should be more that 1s (1000 milisecond)
                if (delay > 1000) {
                    listener = new InactivityListener(logoutAction, delay);
                    listener.start();
                }
            } catch (NumberFormatException ex) {
                LOGGER.log(System.Logger.Level.WARNING, "Exception on set auto logout timer: ", ex);
            }
        }

        paymentdialogreceipt = JPaymentSelectReceipt.getDialog(this);
        paymentdialogreceipt.init(m_App);
        paymentdialogrefund = JPaymentSelectRefund.getDialog(this);
        paymentdialogrefund.init(m_App);

        m_jaddtax.setSelected("true".equals(m_jbtnconfig.getProperty("taxesincluded")));

        List<TaxInfo> taxlist = senttax.list();
        taxcollection = new ListKeyed<>(taxlist);
        List<TaxCategoryInfo> taxcategorieslist = senttaxcategories.list();

        String taxesid = m_jbtnconfig.getProperty("taxcategoryid");
        taxcategoriesmodel = new ComboBoxValModel(taxcategorieslist);
        taxcategoriesmodel.setSelectedKey(taxesid);
        taxeslogic = new TaxesLogic(taxlist);

        m_jTax.setModel(taxcategoriesmodel);
        if (taxesid == null) {
            if (m_jTax.getItemCount() > 0) {
                m_jTax.setSelectedIndex(0);
            }
        } else {
            taxcategoriesmodel.setSelectedKey(taxesid);
        }

        m_jaddtax.setSelected((Boolean.parseBoolean(getAppProperty("till.taxincluded"))));
        if (m_App.getAppUserView().getUser().hasPermission("sales.ChangeTaxOptions")) {
            m_jTax.setVisible(true);
            m_jaddtax.setVisible(true);
        } else {
            m_jTax.setVisible(false);
            m_jaddtax.setVisible(false);
        }

        m_jDelete.setEnabled(m_App.hasPermission("sales.EditLines"));
        m_jNumberKeys.setMinusEnabled(m_App.hasPermission("sales.EditLines"));
        // Sebastian - Deshabilitar permanentemente el botón '=' porque usamos el botón
        // 'Pagar' dedicado
        m_jNumberKeys.setEqualsEnabled(false);
        m_jbtnconfig.setPermissions(m_App.getAppUserView().getUser());

        m_ticketsbag.setEnabled(false);
        m_ticketsbag.activate();

        CustomerInfoGlobal customerInfoGlobal = CustomerInfoGlobal.getInstance();

        if (customerInfoGlobal.getCustomerInfoExt() != null && m_oTicket != null) {
            m_oTicket.setCustomer(customerInfoGlobal.getCustomerInfoExt());
        }

        refreshTicket();

        // Aplicar fuentes grandes nuevamente por si acaso
        SwingUtilities.invokeLater(() -> {
            aplicarFuentesGrandesVentas();
        });
    }

    /**
     * Aplica fuentes grandes a los campos numéricos del panel de ventas
     * Se llama en activate() para sobrescribir las fuentes del Look and Feel Metal
     */
    private void aplicarFuentesGrandesVentas() {
        if (m_jKeyFactory != null) {
            m_jKeyFactory.setFont(new Font("Segoe UI", Font.BOLD, 28));
            m_jKeyFactory.setForeground(Color.BLACK); // Números negros
        }
        if (m_jPrice != null) {
            m_jPrice.setFont(new Font("Segoe UI", Font.BOLD, 24));
        }
        if (m_jTotalEuros != null) {
            m_jTotalEuros.setFont(new Font("Segoe UI", Font.BOLD, 56)); // Total más grande
        }
        if (m_jSubtotalEuros != null) {
            m_jSubtotalEuros.setFont(new Font("Segoe UI", Font.PLAIN, 32)); // Subtotal más grande
        }
        if (m_jTaxesEuros != null) {
            m_jTaxesEuros.setFont(new Font("Segoe UI", Font.PLAIN, 32)); // Impuestos más grande
        }
    }

    @Override
    public boolean deactivate() {
        LOGGER.log(System.Logger.Level.DEBUG, "JPanelTicket.deactivate");
        if (listener != null) {
            listener.stop();
        }

        saveCurrentTicket();

        return m_ticketsbag.deactivate();
    }

    protected abstract JTicketsBag getJTicketsBag();

    protected abstract Component getSouthComponent();

    protected abstract void resetSouthComponent();

    /**
     *
     * @param ticketInfo
     * @param oTicketExt
     */
    @Override
    public void setActiveTicket(TicketInfo ticketInfo, String oTicketExt) {
        m_oTicket = ticketInfo;
        m_oTicketExt = oTicketExt;

        LOGGER.log(System.Logger.Level.DEBUG, "JPanelTicket setActiveTicket: " + oTicketExt);

        if (m_oTicket != null) {
            m_oTicket.setUser(m_App.getAppUserView().getUser().getUserInfo());
            m_oTicket.setActiveCash(m_App.getActiveCashIndex());
            m_oTicket.setDate(new Date());

            if (isRestaurantMode()) {
                if (isAutoLogoutRestaurant()) {
                    if (listener != null) {
                        listener.restart();
                    }
                }

                j_btnRemotePrt.setVisible(m_App.hasPermission("sales.PrintRemote"));
                j_btnRemotePrt.setEnabled(m_App.hasPermission("sales.PrintRemote"));

                if (!m_oTicket.getOldTicket()) {
                    restDB.setTicketIdInTable(m_oTicket.getId(), m_oTicketExt);
                }

                if (Boolean.parseBoolean(getAppProperty("table.showcustomerdetails"))) {
                    String custname = restDB.getCustomerNameInTable(m_oTicketExt);
                    if (m_oTicket.getCustomer() != null && (custname == null || custname.isBlank())) {
                        restDB.setCustomerNameInTable(m_oTicket.getCustomer().getName(), m_oTicketExt);
                    }
                }

                if (Boolean.parseBoolean(getAppProperty("table.showwaiterdetails"))) {
                    String waiter = restDB.getWaiterNameInTable(m_oTicketExt);
                    if (waiter == null || waiter.isBlank()) {
                        restDB.setWaiterNameInTable(m_App.getAppUserView().getUser().getName(), m_oTicketExt);
                    }
                }

                if (restDB.getTableMovedFlag(m_oTicket.getId())) {
                    restDB.moveCustomer(m_oTicketExt, m_oTicket.getId());
                }
            }

            executeEvent(m_oTicket, m_oTicketExt, TicketConstants.EV_TICKET_SHOW);
        }

        refreshTicket();

        // Establecer foco automáticamente en el campo de búsqueda después de cambiar de
        // ticket
        setSearchFieldFocus();
    }

    /**
     *
     * @return
     */
    @Override
    public TicketInfo getActiveTicket() {
        return m_oTicket;
    }

    private void refreshTicket() {

        CardLayout cl = (CardLayout) (getLayout());

        if (m_oTicket == null) {
            m_jTicketId.setText(null);
            m_ticketlines.clearTicketLines();
            m_jSubtotalEuros.setText(null);
            m_jTaxesEuros.setText(null);
            m_jTotalEuros.setText(null);
            jCheckStock.setText(null);

            // Sebastian - Limpiar campos de cliente cuando no hay ticket
            m_jCustomerId.setText("");
            m_jCustomerName.setText("");
            m_jCustomerPoints.setText("");
            m_jCustomerPoints.setVisible(false);

            checkStock();
            stateToZero();
            repaint();

            cl.show(this, "null");

            if ((m_oTicket != null) && (m_oTicket.getLinesCount() == 0)) {
                resetSouthComponent();
            }

        } else {
            if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
                m_jEditLine.setVisible(false);
                m_jList.setVisible(false);
            }

            m_oTicket.getLines().forEach((line) -> {
                line.setTaxInfo(taxeslogic.getTaxInfo(line
                        .getProductTaxCategoryID(), m_oTicket.getCustomer()));
            });

            m_jTicketId.setText(m_oTicket.getName(m_oTicketExt));
            m_ticketlines.clearTicketLines();

            for (int i = 0; i < m_oTicket.getLinesCount(); i++) {
                m_ticketlines.addTicketLine(m_oTicket.getLine(i));
            }

            if (m_oTicket.getLinesCount() == 0) {
                resetSouthComponent();
            }

            // Sebastian - Actualizar campos de cliente cuando hay ticket
            updateCustomerFields();

            // Sebastian - Actualizar puntos cuando cambia el total del ticket
            updateCustomerPointsDisplay();

            countArticles();
            printPartialTotals();
            stateToZero();
            repaint();

            cl.show(this, "ticket");
            if (m_oTicket.getLinesCount() == 0) {
                resetSouthComponent();
            }

            m_jKeyFactory.setText(null);
            java.awt.EventQueue.invokeLater(() -> {
                m_jKeyFactory.requestFocus();
            });
        }
    }

    private void countArticles() {

        if (m_oTicket != null) {
            if (m_App.hasPermission("sales.Total") && m_oTicket.getArticlesCount() > 1) {
                btnSplit.setEnabled(true);
            } else {
                btnSplit.setEnabled(false);
            }
        }
    }

    private boolean changeCount() {

        Boolean pinOK = false;

        if (m_oTicket != null) {

            if (getAppProperty("override.check").equals("true")) {
                String pin = getAppProperty("override.pin");
                String iValue = JPasswordDialog.showEditor(this, AppLocal.getIntString("title.override.enterpin"));

                if (iValue != null && iValue.equals(pin)) {
                    pinOK = true;
                } else {
                    pinOK = false;
                    JOptionPane.showMessageDialog(this, AppLocal.getIntString("message.override.badpin"));
                }
            }
        }
        return pinOK;
    }

    private boolean isOverrideCheckEnabled() {
        return getAppProperty("override.check").equals("true");
    }

    private void printPartialTotals() {

        if (m_oTicket == null || m_oTicket.getLinesCount() == 0) {
            m_jSubtotalEuros.setText(null);
            m_jTaxesEuros.setText(null);
            m_jTotalEuros.setText(null);
        } else {
            m_jSubtotalEuros.setText(m_oTicket.printSubTotal());
            m_jTaxesEuros.setText(m_oTicket.printTax());
            m_jTotalEuros.setText(m_oTicket.printTotal());
        }
        repaint();
    }

    private void paintTicketLine(int index, TicketLineInfo oLine) {
        if (m_oTicket != null) {
            m_oTicket.setLine(index, oLine);
            m_ticketlines.setTicketLine(index, oLine);
            m_ticketlines.setSelectedIndex(index);
            // oCount = count; // pass line old multiplier value

            countArticles();
            visorTicketLine(oLine);
            printPartialTotals();

            // Sebastian - Actualizar puntos cuando cambia una línea del ticket
            updateCustomerPointsDisplay();

            stateToZero();
        }
    }

    private void addTicketLine(ProductInfoExt oProduct, double dMul, double dPrice) {

        LOGGER.log(System.Logger.Level.INFO, "Product onoProduct.isVprice: ", oProduct.isVprice());
        if (oProduct.isVprice()) {
            TaxInfo tax = taxeslogic.getTaxInfo(oProduct.getTaxCategoryID(), m_oTicket.getCustomer());

            if (m_jaddtax.isSelected()) {
                dPrice /= (1 + tax.getRate());
            }

            // Check stock for variable price product as well
            try {
                if (!oProduct.isService() && dMul > 0.0) {
                    ProductStock checkProduct = dlSales.getProductStockState(oProduct.getID(),
                            m_App.getInventoryLocation());
                    if (checkProduct == null) {
                        MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                                AppLocal.getIntString("message.location.current"));
                        msg.show(this);
                        return;
                    } else {
                        double unitsAvailable = checkProduct.getUnits();
                        if (unitsAvailable < dMul || unitsAvailable <= 0) {
                            // Show actionable dialog to manage stock
                            showInsufficientStockDialog(oProduct);
                            return;
                        }
                    }
                }
            } catch (BasicException ex) {
                LOGGER.log(System.Logger.Level.WARNING, "Could not check product stock", ex);
                // Allow adding if stock check fails - backend also checks it on save
            }

            addTicketLine(new TicketLineInfo(oProduct, dMul, dPrice, tax,
                    (java.util.Properties) (oProduct.getProperties().clone())));

        } else {
            if (oProduct.isService()) {
                LOGGER.log(System.Logger.Level.INFO, "Adding product marked as SERVICE to ticket: " + oProduct.getID() + " - " + oProduct.getName() + ". This product will not decrement stock on save.");
            }
            CustomerInfoExt customer = m_oTicket.getCustomer();

            // get the line product tax
            TaxInfo tax = taxeslogic.getTaxInfo(oProduct.getTaxCategoryID(), customer);

            Properties props = new Properties();
            if (oProduct.getProperties() != null && !oProduct.getProperties().isEmpty()) {
                props = (java.util.Properties) oProduct.getProperties().clone();
            }

            // Check stock before adding to the ticket (avoid negative stock at UI level)
            try {
                if (!oProduct.isService() && dMul > 0.0) {
                    ProductStock checkProduct = dlSales.getProductStockState(oProduct.getID(),
                            m_App.getInventoryLocation());
                    if (checkProduct == null) {
                        // No stock assigned to this location
                        MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                                AppLocal.getIntString("message.location.current"));
                        msg.show(this);
                        return;
                    } else {
                        double unitsAvailable = checkProduct.getUnits();
                        if (unitsAvailable < dMul || unitsAvailable <= 0) {
                            // Show actionable dialog to manage stock
                            showInsufficientStockDialog(oProduct);
                            return;
                        }
                    }
                }
            } catch (BasicException ex) {
                LOGGER.log(System.Logger.Level.WARNING, "Could not check product stock", ex);
                // Allow adding if stock check fails - backend also checks it on save
            }

            addTicketLine(new TicketLineInfo(oProduct, dMul, dPrice, tax, props));
            refreshTicket();

            j_btnRemotePrt.setEnabled(true);
        }

    }

    /**
     * Add a Ticket Line
     *
     * @param oLine Ticket line
     */
    protected void addTicketLine(TicketLineInfo oLine) {
        if (m_oTicket != null) {
            if (oLine.isProductCom()) {
                int i = m_ticketlines.getSelectedIndex();

                if (i >= 0 && !m_oTicket.getLine(i).isProductCom()) {
                    i++;
                }

                while (i >= 0 && i < m_oTicket.getLinesCount() && m_oTicket.getLine(i).isProductCom()) {
                    i++;
                }

                if (i >= 0) {
                    m_oTicket.insertLine(i, oLine);
                    m_ticketlines.insertTicketLine(i, oLine);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            } else {
                m_oTicket.addLine(oLine);
                m_ticketlines.addTicketLine(oLine);

                try {
                    int i = m_ticketlines.getSelectedIndex();
                    TicketLineInfo line = m_oTicket.getLine(i);

                    if (line.isProductVerpatrib()) {

                        JProductAttEdit2 attedit = JProductAttEdit2.getAttributesEditor(this, m_App.getSession());
                        attedit.editAttributes(line.getProductAttSetId(), line.getProductAttSetInstId());
                        attedit.setVisible(true);

                        if (attedit.isOK()) {
                            line.setProductAttSetInstId(attedit.getAttributeSetInst());
                            line.setProductAttSetInstDesc(attedit.getAttributeSetInstDescription());
                        }

                        paintTicketLine(i, line);
                    }

                } catch (Exception ex) {
                    LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
                    MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                            AppLocal.getIntString("message.cannotfindattributes"), ex);
                    msg.show(this);
                }
            }

            visorTicketLine(oLine);
            printPartialTotals();
            stateToZero();
            countArticles();

            executeEvent(m_oTicket, m_oTicketExt, TicketConstants.EV_TICKET_CHANGE);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * Show an improved dialog for insufficient stock and propose opening Stock
     * Management
     * 
     * @param oProduct
     */
    private void showInsufficientStockDialog(ProductInfoExt oProduct) {
        String msgTemplate = AppLocal.getIntString("message.stockinsufficient_action");
        String title = AppLocal.getIntString("message.stockinsufficient_title");
        String message = MessageFormat.format(msgTemplate, oProduct.getName());
        Object[] options = new Object[] { AppLocal.getIntString("message.manage.stock"),
                AppLocal.getIntString("button.cancel") };
        int res = JOptionPane.showOptionDialog(this, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        if (res == 0) { // Manage stock
            // Open Stock Management view and attempt to select the product in the stock
            // view
            try {
                // First, show the StockManagement view
                m_App.getAppUserView().showTask("com.openbravo.pos.inventory.StockManagement");

                // Try to obtain the same bean instance and call the helper to select the
                // product
                Object bean = m_App.getBean("com.openbravo.pos.inventory.StockManagement");
                if (bean instanceof com.openbravo.pos.inventory.StockManagement) {
                    com.openbravo.pos.inventory.StockManagement sm = (com.openbravo.pos.inventory.StockManagement) bean;
                    sm.selectProduct(oProduct.getID());
                }
            } catch (Exception ex) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                        AppLocal.getIntString("message.cannotopenstockmanager"));
                msg.show(this);
            }
        }
    }

    private TicketLineInfo getSelectedTicketLineInfo() {
        int i = m_ticketlines.getSelectedIndex();
        return getTicketLineInfo(i);
    }

    private TicketLineInfo getTicketLineInfo(int index) {
        return m_oTicket.getLine(index);
    }

    private void removeTicketLine(int i) {
        String ticketID = Integer.toString(m_oTicket.getTicketId());
        if (m_oTicket.getTicketId() == 0) {
            ticketID = "Void";
        }

        dlSystem.execLineRemoved(
                new Object[] {
                        m_App.getAppUserView().getUser().getName(),
                        ticketID,
                        m_oTicket.getLine(i).getProductID(),
                        m_oTicket.getLine(i).getProductName(),
                        m_oTicket.getLine(i).getMultiply()
                });

        if (m_oTicket.getLine(i).isProductCom()) {
            m_oTicket.removeLine(i);
            m_ticketlines.removeTicketLine(i);
        } else {
            // Verificar permisos pero eliminar directamente sin preguntar
            if (i < 1) {
                if (m_App.hasPermission("sales.DeleteLines")) {
                    m_oTicket.removeLine(i);
                    m_ticketlines.removeTicketLine(i);
                } else {
                    JOptionPane.showMessageDialog(this,
                            AppLocal.getIntString("message.deletelineno"),
                            AppLocal.getIntString("label.deleteline"), JOptionPane.WARNING_MESSAGE);
                }
            } else {
                m_oTicket.removeLine(i);
                m_ticketlines.removeTicketLine(i);

                while (i < m_oTicket.getLinesCount() && m_oTicket.getLine(i).isProductCom()) {
                    m_oTicket.removeLine(i);
                    m_ticketlines.removeTicketLine(i);
                }
            }
        }

        visorTicketLine(null);
        printPartialTotals();
        stateToZero();
        countArticles();

    }

    private ProductInfoExt getInputProduct() {
        ProductInfoExt oProduct = new ProductInfoExt();
        // Always add Default Prod ID + Add Name to Misc.
        // THOSE ATTRIBUTE ARE IMPORTANT FOR Table foreign key rela
        oProduct.setReference("0000");
        oProduct.setCode("0000");
        oProduct.setName("***");
        oProduct.setTaxCategoryID(((TaxCategoryInfo) taxcategoriesmodel.getSelectedItem()).getID());
        oProduct.setPriceSell(includeTaxes(oProduct.getTaxCategoryID(), getInputValue()));

        return oProduct;
    }

    private double includeTaxes(String tcid, double dValue) {
        if (m_jaddtax.isSelected()) {
            TaxInfo tax = taxeslogic.getTaxInfo(tcid, m_oTicket.getCustomer());
            double dTaxRate = tax == null ? 0.0 : tax.getRate();
            return dValue / (1.0 + dTaxRate);
        } else {
            return dValue;
        }
    }

    private double excludeTaxes(String tcid, double dValue) {
        TaxInfo tax = taxeslogic.getTaxInfo(tcid, m_oTicket.getCustomer());
        double dTaxRate = tax == null ? 0.0 : tax.getRate();
        return dValue / (1.0 + dTaxRate);
    }

    /**
     * Scanner Input Value Get Price from a input field MUST be Public is used
     * by Script (
     *
     * @return
     */
    public double getInputValue() {
        try {
            return Double.parseDouble(m_jPrice.getText());
        } catch (NumberFormatException ex) {
            LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
            return 0.0;
        }
    }

    /**
     * Scanner Por Value
     *
     * @return
     */
    public double getPorValue() {
        try {
            return Double.parseDouble(m_jPor.getText().substring(1));
        } catch (NumberFormatException | StringIndexOutOfBoundsException ex) {
            LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
            return 1.0;
        }
    }

    /**
     * Get selected ticket line
     *
     * @return line index
     */
    public int getSelectedIndex() {
        return m_ticketlines.getSelectedIndex();
    }

    private void stateToZero() {
        m_jPor.setText("");
        m_jPrice.setText("");
        m_jKeyFactory.setText(""); // Limpiar también el campo de búsqueda
        m_sBarcode = new StringBuffer();

        m_iNumberStatus = NUMBER_INPUTZERO;
        m_iNumberStatusInput = NUMBERZERO;
        m_iNumberStatusPor = NUMBERZERO;
        repaint();
    }

    private void incProductByCode(String sCode) {

        try {
            ProductInfoExt oProduct = dlSales.getProductInfoByCode(sCode);

            if (oProduct == null) {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(null,
                        sCode + " - " + AppLocal.getIntString("message.noproduct"),
                        "Verificación", JOptionPane.WARNING_MESSAGE);
                stateToZero();
            } else {
                incProduct(oProduct);
            }
        } catch (BasicException ex) {
            LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
            stateToZero();
            new MessageInf(ex).show(this);
        }
    }

    private void incProductByCodePrice(String sCode, double dPriceSell) {

        try {
            ProductInfoExt oProduct = dlSales.getProductInfoByCode(sCode);
            if (oProduct == null) {
                Toolkit.getDefaultToolkit().beep();
                new MessageInf(MessageInf.SGN_WARNING, AppLocal
                        .getIntString("message.noproduct")).show(this);
                stateToZero();
            } else {
                if (m_jaddtax.isSelected()) {
                    TaxInfo tax = taxeslogic.getTaxInfo(oProduct.getTaxCategoryID(), m_oTicket.getCustomer());
                    addTicketLine(oProduct, 1.0, dPriceSell / (1.0 + tax.getRate()));
                } else {
                    addTicketLine(oProduct, 1.0, dPriceSell);
                }
            }
        } catch (BasicException ex) {
            LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
            stateToZero();
            new MessageInf(ex).show(this);
        }
    }

    private void incProduct(ProductInfoExt prod) {

        if (prod.isScale()) {
            // Usar el diálogo estilo Eleventa para productos de granel
            System.out.println("DEBUG: Producto es granel, mostrando diálogo...");
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            if (parentWindow == null) {
                parentWindow = (Window) SwingUtilities.getRoot(this);
            }

            System.out.println(
                    "DEBUG: Ventana padre: " + (parentWindow != null ? parentWindow.getClass().getName() : "null"));
            System.out.println("DEBUG: Nombre producto: " + prod.getName());
            System.out.println("DEBUG: Precio: " + prod.getPriceSell());

            Double peso = JGranelDialog.mostrarDialogo(
                    parentWindow,
                    prod.getName() != null ? prod.getName() : "Producto Granel",
                    prod.getPriceSell());

            System.out.println("DEBUG: Peso retornado: " + peso);

            if (peso != null && peso > 0) {
                incProduct(prod, peso);
            }
        } else {
            if (!prod.isVprice()) {
                incProduct(prod, 1.0);
            } else {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(null,
                        AppLocal.getIntString("message.novprice"));
            }
        }
    }

    private void incProduct(ProductInfoExt prod, double dPor) {

        if (prod.isVprice()) {
            addTicketLine(prod, getPorValue(), getInputValue());
        } else {
            addTicketLine(prod, dPor, prod.getPriceSell());
        }
    }

    /**
     *
     * @param prod
     */
    protected void buttonTransition(ProductInfoExt prod) {

        if (m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERZERO) {
            incProduct(prod);
        } else if (m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO) {
            incProduct(prod, getInputValue());
        } else if (prod.isVprice()) {
            addTicketLine(prod, getPorValue(), getInputValue());
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    @SuppressWarnings("empty-statement")
    private void stateTransition(char cTrans) {

        if ((cTrans == '\n') || (cTrans == '?')) {

            if (m_sBarcode.length() > 0) {

                String sCode = m_sBarcode.toString();
                String sCodetype = "EAN"; // Declare EAN. It's default

                if ("true".equals(getAppProperty("machine.barcodetype"))) {
                    sCodetype = "UPC";
                } else {
                    sCodetype = "EAN"; // Ensure not null
                }

                if (sCode.startsWith("C") || sCode.startsWith("c")) {
                    try {
                        String card = sCode;
                        CustomerInfoExt newcustomer = dlSales.findCustomerExt(card);

                        if (newcustomer == null) {
                            Toolkit.getDefaultToolkit().beep();
                            new MessageInf(MessageInf.SGN_WARNING, AppLocal
                                    .getIntString("message.nocustomer")).show(this);
                        } else {
                            m_oTicket.setCustomer(newcustomer);
                            m_jTicketId.setText(m_oTicket.getName(m_oTicketExt));
                            updateCustomerPointsDisplay(); // Sebastian - Actualizar display de puntos
                        }
                    } catch (BasicException ex) {
                        LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
                        Toolkit.getDefaultToolkit().beep();
                        new MessageInf(MessageInf.SGN_WARNING, AppLocal
                                .getIntString("message.nocustomer"), ex).show(this);
                    }
                    stateToZero();

                } else if (sCode.startsWith(";")) {
                    stateToZero();

                    // START OF BARCODE PARSING
                    /*
                     * This block is deliberately verbose and is base for future scanner handling
                     * Some scanners inject a CR+LF... some don't...
                     * stateTransition() must allow for this as these add characters to .length()
                     * First 3 digits are GS1 CountryCode OR Retailer internal use
                     * 
                     * Prefix ManCodeProdCode CheckCode
                     * PPP MMMMMCCCCC K
                     * 012 3456789012 K
                     * Barcode CCCCC must be unique
                     * Notes:
                     * ManufacturerCode and ProductCode must be exactly 10 digits
                     * If code begins with 0 then is actually a UPC-A with prepended 0
                     * 
                     * KriolOS POS Retailer instore uses these RULES
                     * Prefixes 020 to 029 are set aside for Retailer internal use
                     * This means that CCCC becomes price/weight values
                     * Prefixes 978 and 979 are set aside for ISBN - Future use
                     * 
                     * Prefix ManCode ProdCode CheckCode
                     * PPP MMMMM CCCCC K Format
                     * 012 34567 89012 K Human
                     * 
                     */
                } else if ("EAN".equals(sCodetype)
                        && ((sCode.startsWith("2")) || (sCode.startsWith("02"))) // check code prefix
                        && ((sCode.length() == 13) || (sCode.length() == 12))) { // check code length variances

                    try {
                        ProductInfoExt oProduct // get product(s) with PMMMMM
                                = dlSales.getProductInfoByShortCode(sCode);

                        if (oProduct == null) { // nothing returned so display message to user
                            Toolkit.getDefaultToolkit().beep();
                            JOptionPane.showMessageDialog(null,
                                    sCode + " - "
                                            + AppLocal.getIntString("message.noproduct"),
                                    "Verificación", JOptionPane.WARNING_MESSAGE);
                            stateToZero(); // clear the user input

                        } else if ("EAN-13".equals(oProduct.getCodetype())) { // have a valid barcode
                            oProduct.setProperty("product.barcode", sCode); // set the screen's barcode from input
                            double dPriceSell = oProduct.getPriceSell(); // default price for product
                            double weight = 0; // used if barcode includes weight of product
                            double dUnits = 0; // used for pro-rata unit
                            String sVariableTypePrefix = sCode.substring(0, 2); // get first two PPP digits
                            String sVariableNum; // CCCCC variable value of barcode

                            if (sCode.length() == 13) { // full barcode from scanner
                                sVariableNum = sCode.substring(8, 12); // get the 5 CCCCC digits
                            } else { // barcode can be any length
                                sVariableNum = sCode.substring(7, 11); // get the 5 CCCCC digits
                            } // scanner has dropped 1st digit so shift get to left

                            // PRICE - SET value decimals
                            switch (sVariableTypePrefix) { // Use CCCCC value of 01049 as example
                                case "02": // first 2 PPP digits determine decimal position
                                    dUnits = (Double.parseDouble(sVariableNum) // position decimal in CCC.CC
                                            / 100) / oProduct.getPriceSell(); // 2 decimal = 010.49
                                    break;
                                case "20":
                                    dUnits = (Double.parseDouble(sVariableNum) // position decimal in CCC.CC
                                            / 100) / oProduct.getPriceSell(); // 2 decimal = 010.49
                                    break;
                                case "21":
                                    dUnits = (Double.parseDouble(sVariableNum) // position decimal in CC.CCC
                                            / 10) / oProduct.getPriceSell(); // 2 decimal = 0104.9
                                    break;
                                case "22":
                                    dUnits = Double.parseDouble(sVariableNum) // position decimal in CCCC.C
                                            / oProduct.getPriceSell(); // Price = 01049.
                                    break;

                                // WEIGHT - SET value decimals
                                case "23": // Use CCCCC 01049kg as example
                                    weight = Double.parseDouble(sVariableNum)
                                            / 1000; // Weight = 01.049
                                    dUnits = weight; // set Units for price calculation
                                    break;
                                case "24":
                                    weight = Double.parseDouble(sVariableNum)
                                            / 100; // Weight = 010.49
                                    dUnits = weight; // set Units for price calculation
                                    break;
                                case "25":
                                    weight = Double.parseDouble(sVariableNum)
                                            / 10; // Weight = 0104.9
                                    dUnits = weight; // set Units for price calculation
                                    break;
                                default:
                                    break;
                            }

                            TaxInfo tax = taxeslogic // get the TaxRate for the product
                                    .getTaxInfo(oProduct.getTaxCategoryID(),
                                            m_oTicket.getCustomer()); // calculate if ticket has a Customer

                            switch (sVariableTypePrefix) {
                                // PRICE - Assign var's
                                case "02": // now we need to calculate some values
                                    dPriceSell = oProduct.getPriceSellTax(tax)
                                            / (1.0 + tax.getRate()); // selling price with tax
                                    dUnits = (Double.parseDouble(sVariableNum)
                                            / 100) / oProduct.getPriceSellTax(tax); // Units as proportion of selling
                                                                                    // price
                                    oProduct.setProperty("product.price",
                                            Double.toString(oProduct.getPriceSell())); // push to screen
                                    break;
                                case "20": // as above
                                    dPriceSell = oProduct.getPriceSellTax(tax)
                                            / (1.0 + tax.getRate());
                                    dUnits = (Double.parseDouble(sVariableNum)
                                            / 100) / oProduct.getPriceSellTax(tax);
                                    oProduct.setProperty("product.price",
                                            Double.toString(oProduct.getPriceSellTax(tax)));
                                    break;
                                case "21":
                                    dPriceSell = oProduct.getPriceSellTax(tax)
                                            / (1.0 + tax.getRate());
                                    dUnits = (Double.parseDouble(sVariableNum)
                                            / 10) / oProduct.getPriceSellTax(tax);
                                    oProduct.setProperty("product.price",
                                            Double.toString(oProduct.getPriceSell()));
                                    break;
                                case "22":
                                    dPriceSell = oProduct.getPriceSellTax(tax)
                                            / (1.0 + tax.getRate());
                                    dUnits = (Double.parseDouble(sVariableNum)
                                            / 1) / oProduct.getPriceSellTax(tax);
                                    oProduct.setProperty("product.price",
                                            Double.toString(oProduct.getPriceSell()));
                                    break;

                                // WEIGHT - Assign variable to Unit
                                case "23":
                                    weight = Double.parseDouble(sVariableNum)
                                            / 1000; // 3 decimals = 01.049 kg
                                    dUnits = weight; // which represents 1gramme Units
                                    oProduct.setProperty("product.weight",
                                            Double.toString(weight));
                                    oProduct.setProperty("product.price",
                                            Double.toString(dPriceSell));
                                    break;
                                case "24":
                                    weight = Double.parseDouble(sVariableNum)
                                            / 100; // 2 decimals = 010.49 kg
                                    dUnits = weight; // which represents 10gramme Units
                                    oProduct.setProperty("product.weight",
                                            Double.toString(weight));
                                    oProduct.setProperty("product.price",
                                            Double.toString(dPriceSell));
                                    break;
                                case "25":
                                    weight = Double.parseDouble(sVariableNum)
                                            / 10; // 1 decimal = 0104.9 kg
                                    dUnits = weight; // which represents 100gramme Units
                                    oProduct.setProperty("product.weight",
                                            Double.toString(weight));
                                    oProduct.setProperty("product.price",
                                            Double.toString(dPriceSell));
                                    break;

                                /*
                                 * Some countries use different barcode prefix 26-29 or 250 etc.
                                 * Use this section to add more case statements but these are not mandatory
                                 * If you have your own internal or other barcode schema then...
                                 * Example:
                                 * case "28":
                                 * {
                                 * // price has tax. Remove it from sPriceSell
                                 * TaxInfo tax = taxeslogic.getTaxInfo(oProduct.getTaxCategoryID(),
                                 * m_oTicket.getCustomer());
                                 * dPriceSell /= (1.0 + tax.getRate());
                                 * oProduct.setProperty("product.price", Double.toString(dPriceSell));
                                 * weight = -1.0;
                                 * break;
                                 */
                                default:
                                    break;
                            }

                            if (m_jaddtax.isSelected()) {
                                dPriceSell = oProduct.getPriceSellTax(tax);
                            }

                            addTicketLine(oProduct, dUnits, dPriceSell);
                        }
                    } catch (BasicException ex) {
                        LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
                        stateToZero();
                        new MessageInf(ex).show(this);
                    }

                    // UPC-A
                    /*
                     * Note: if begins 02 then its a standard
                     * // UPC-A max value limitation is 4 digit price
                     * // UPC-A Extended uses State digit to give 5 digit price
                     * // KriolOS POS does not support UPC-A Extended at this time
                     * // Identifier Prod State Cost CheckCode
                     * // I PPPPP S CCCC K
                     * // 1 23456 7 8901 2
                     * 
                     * 0 = Standard UPC number (must have a zero to do zero-suppressed numbers)
                     * 1 = Reserved
                     * 2 = Random-weight items (fruits, vegetables, meats, etc.)
                     * 3 = Pharmaceuticals
                     * 4 = In-store marketing for retailers (Other stores will not understand)
                     * 5 = Coupons
                     * 6 = Standard UPC number
                     * 7 = Standard UPC number
                     * 8 = Reserved
                     * 9 = Reserved
                     */
                } else if ("UPC".equals(sCodetype)
                        && (sCode.startsWith("2"))
                        && (sCode.length() == 12)) {

                    try {
                        ProductInfoExt oProduct = dlSales.getProductInfoByUShortCode(sCode); // Return only UPC product

                        if (oProduct == null) {
                            Toolkit.getDefaultToolkit().beep();
                            JOptionPane.showMessageDialog(null,
                                    sCode + " - "
                                            + AppLocal.getIntString("message.noproduct"),
                                    "Verificación", JOptionPane.WARNING_MESSAGE);
                            stateToZero();
                        } else if ("Upc-A".equals(oProduct.getCodetype())) {
                            oProduct.setProperty("product.barcode", sCode);
                            double dPriceSell = oProduct.getPriceSell(); // default price for product
                            double weight = 0; // used if barcode includes weight of product
                            double dUnits = 0; // used for pro-rata unit
                            String sVariableNum = sCode.substring(7, 11); // grab the value from the code only using 4
                                                                          // digit price

                            TaxInfo tax = taxeslogic // get the TaxRate for the product
                                    .getTaxInfo(oProduct.getTaxCategoryID(),
                                            m_oTicket.getCustomer());

                            if (oProduct.getPriceSell() != 0.0) { // we have a weight barcode
                                weight = Double.parseDouble(sVariableNum) / 100; // 2 decimals (e.g. 10.49 kg)
                                dUnits = weight; // Units is now transformed to weight

                                oProduct.setProperty("product.weight" // catch-all for weight
                                        ,
                                        Double.toString(weight));
                                oProduct.setProperty("product.price" // get the prod sellprice
                                        ,
                                        Double.toString(oProduct.getPriceSell()));
                                dPriceSell = oProduct.getPriceSellTax(tax); // calculate the tax on sellprice
                                dUnits = (Double.parseDouble(sVariableNum) // calculate Units in sellprice with Tax
                                        / 100)
                                        / oProduct.getPriceSellTax(tax);

                            } else { // no sellprice so we have a price barcode
                                dPriceSell = (Double.parseDouble(sVariableNum) / 100);
                                dUnits = 1; // no sellprice to calculate so must be 1 Unit
                            }

                            if (m_jaddtax.isSelected()) {
                                addTicketLine(oProduct, dUnits, dPriceSell);
                            } else {
                                addTicketLine(oProduct, dUnits, dPriceSell / (1.0 + tax.getRate()));
                            }
                        }
                    } catch (BasicException ex) {
                        LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
                        stateToZero();
                        new MessageInf(ex).show(this);
                    }

                } else {
                    incProductByCode(sCode); // returned is standard so go get it
                }
                // END OF BARCODE

            } else {
                Toolkit.getDefaultToolkit().beep();
            }

        } else {

            m_sBarcode.append(cTrans);

            if (cTrans == '\u007f') {
                stateToZero();

            } else if ((cTrans == '0') && (m_iNumberStatus == NUMBER_INPUTZERO)) {
                m_jPrice.setText(Character.toString('0'));

            } else if ((cTrans == '1' || cTrans == '2' || cTrans == '3'
                    || cTrans == '4' || cTrans == '5' || cTrans == '6'
                    || cTrans == '7' || cTrans == '8' || cTrans == '9')
                    && (m_iNumberStatus == NUMBER_INPUTZERO)) {

                if (!priceWith00) {
                    m_jPrice.setText(m_jPrice.getText() + cTrans);
                } else {
                    m_jPrice.setText(setTempjPrice(m_jPrice.getText() + cTrans));
                }

                m_iNumberStatus = NUMBER_INPUTINT;
                m_iNumberStatusInput = NUMBERVALID;

            } else if ((cTrans == '0' || cTrans == '1' || cTrans == '2'
                    || cTrans == '3' || cTrans == '4' || cTrans == '5'
                    || cTrans == '6' || cTrans == '7' || cTrans == '8'
                    || cTrans == '9')
                    && (m_iNumberStatus == NUMBER_INPUTINT)) {

                if (!priceWith00) {
                    m_jPrice.setText(m_jPrice.getText() + cTrans);
                } else {
                    m_jPrice.setText(setTempjPrice(m_jPrice.getText() + cTrans));
                }

            } else if (cTrans == '.'
                    && m_iNumberStatus == NUMBER_INPUTZERO && !priceWith00) {
                m_jPrice.setText("0.");
                m_iNumberStatus = NUMBER_INPUTZERODEC;
            } else if (cTrans == '.'
                    && m_iNumberStatus == NUMBER_INPUTZERO) {
                m_jPrice.setText("");
                m_iNumberStatus = NUMBER_INPUTZERO;
            } else if (cTrans == '.'
                    && m_iNumberStatus == NUMBER_INPUTINT && !priceWith00) {
                m_jPrice.setText(m_jPrice.getText() + ".");
                m_iNumberStatus = NUMBER_INPUTDEC;
            } else if (cTrans == '.'
                    && m_iNumberStatus == NUMBER_INPUTINT) {

                if (!priceWith00) {
                    m_jPrice.setText(m_jPrice.getText() + "00");
                } else {
                    m_jPrice.setText(setTempjPrice(m_jPrice.getText() + "00"));
                }

                m_iNumberStatus = NUMBER_INPUTINT;

            } else if ((cTrans == '0')
                    && (m_iNumberStatus == NUMBER_INPUTZERODEC
                            || m_iNumberStatus == NUMBER_INPUTDEC)) {

                if (!priceWith00) {
                    m_jPrice.setText(m_jPrice.getText() + cTrans);
                } else {
                    m_jPrice.setText(setTempjPrice(m_jPrice.getText() + cTrans));
                }

            } else if ((cTrans == '1' || cTrans == '2' || cTrans == '3'
                    || cTrans == '4' || cTrans == '5' || cTrans == '6'
                    || cTrans == '7' || cTrans == '8' || cTrans == '9')
                    && (m_iNumberStatus == NUMBER_INPUTZERODEC
                            || m_iNumberStatus == NUMBER_INPUTDEC)) {

                m_jPrice.setText(m_jPrice.getText() + cTrans);
                m_iNumberStatus = NUMBER_INPUTDEC;
                m_iNumberStatusInput = NUMBERVALID;

            } else if (cTrans == '*'
                    && (m_iNumberStatus == NUMBER_INPUTINT
                            || m_iNumberStatus == NUMBER_INPUTDEC)) {
                m_jPor.setText("x");
                m_iNumberStatus = NUMBER_PORZERO;
            } else if (cTrans == '*'
                    && (m_iNumberStatus == NUMBER_INPUTZERO
                            || m_iNumberStatus == NUMBER_INPUTZERODEC)) {
                m_jPrice.setText("0");
                m_jPor.setText("x");
                m_iNumberStatus = NUMBER_PORZERO;

            } else if ((cTrans == '0')
                    && (m_iNumberStatus == NUMBER_PORZERO)) {
                m_jPor.setText("x0");
            } else if ((cTrans == '1' || cTrans == '2' || cTrans == '3'
                    || cTrans == '4' || cTrans == '5' || cTrans == '6'
                    || cTrans == '7' || cTrans == '8' || cTrans == '9')
                    && (m_iNumberStatus == NUMBER_PORZERO)) {

                m_jPor.setText("x" + Character.toString(cTrans));
                m_iNumberStatus = NUMBER_PORINT;
                m_iNumberStatusPor = NUMBERVALID;
            } else if ((cTrans == '0' || cTrans == '1' || cTrans == '2'
                    || cTrans == '3' || cTrans == '4' || cTrans == '5'
                    || cTrans == '6' || cTrans == '7' || cTrans == '8'
                    || cTrans == '9') && (m_iNumberStatus == NUMBER_PORINT)) {

                m_jPor.setText(m_jPor.getText() + cTrans);

            } else if (cTrans == '.'
                    && m_iNumberStatus == NUMBER_PORZERO && !priceWith00) {
                m_jPor.setText("x0.");
                m_iNumberStatus = NUMBER_PORZERODEC;
            } else if (cTrans == '.'
                    && m_iNumberStatus == NUMBER_PORZERO) {
                m_jPor.setText("x");
                m_iNumberStatus = NUMBERVALID;
            } else if (cTrans == '.'
                    && m_iNumberStatus == NUMBER_PORINT && !priceWith00) {
                m_jPor.setText(m_jPor.getText() + ".");
                m_iNumberStatus = NUMBER_PORDEC;
            } else if (cTrans == '.'
                    && m_iNumberStatus == NUMBER_PORINT) {
                m_jPor.setText(m_jPor.getText() + "00");
                m_iNumberStatus = NUMBERVALID;

            } else if ((cTrans == '0')
                    && (m_iNumberStatus == NUMBER_PORZERODEC
                            || m_iNumberStatus == NUMBER_PORDEC)) {
                m_jPor.setText(m_jPor.getText() + cTrans);
            } else if ((cTrans == '1' || cTrans == '2' || cTrans == '3'
                    || cTrans == '4' || cTrans == '5' || cTrans == '6'
                    || cTrans == '7' || cTrans == '8' || cTrans == '9')
                    && (m_iNumberStatus == NUMBER_PORZERODEC || m_iNumberStatus == NUMBER_PORDEC)) {

                m_jPor.setText(m_jPor.getText() + cTrans);
                m_iNumberStatus = NUMBER_PORDEC;
                m_iNumberStatusPor = NUMBERVALID;

            } else if (cTrans == '\u00a7'
                    && m_iNumberStatusInput == NUMBERVALID
                    && m_iNumberStatusPor == NUMBERZERO) {

                if (m_App.getDeviceScale().existsScale()
                        && m_App.hasPermission("sales.EditLines")) {
                    try {
                        Double value = m_App.getDeviceScale().readWeight();
                        if (value != null) {
                            ProductInfoExt product = getInputProduct();
                            addTicketLine(product, value, product.getPriceSell());
                        }
                    } catch (ScaleException ex) {
                        LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
                        Toolkit.getDefaultToolkit().beep();
                        new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noweight"), ex)
                                .show(this);
                        stateToZero();
                    }
                } else {

                    Toolkit.getDefaultToolkit().beep();
                }
            } else if (cTrans == '\u00a7'
                    && m_iNumberStatusInput == NUMBERZERO
                    && m_iNumberStatusPor == NUMBERZERO) {

                int i = m_ticketlines.getSelectedIndex();
                if (i < 0) {
                    Toolkit.getDefaultToolkit().beep();
                } else if (m_App.getDeviceScale().existsScale()) {
                    try {
                        Double value = m_App.getDeviceScale().readWeight();
                        if (value != null) {
                            TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));
                            newline.setMultiply(value);
                            newline.setPrice(Math.abs(newline.getPrice()));
                            paintTicketLine(i, newline);
                        }
                    } catch (ScaleException ex) {
                        LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
                        Toolkit.getDefaultToolkit().beep();
                        new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noweight"), ex)
                                .show(this);
                        stateToZero();
                    }
                } else {

                    Toolkit.getDefaultToolkit().beep();
                }

            } else if (cTrans == '+'
                    && m_iNumberStatusInput == NUMBERZERO
                    && m_iNumberStatusPor == NUMBERZERO) {
                int i = m_ticketlines.getSelectedIndex();

                if (i < 0) {
                    Toolkit.getDefaultToolkit().beep();
                } else {
                    TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));
                    // If it's a refund + button means one unit less
                    if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
                        if (isOverrideCheckEnabled()) {
                            // oCount = count - 1; //increment existing line

                            if (changeCount()) {
                                newline.setMultiply(newline.getMultiply() - 1.0);
                                newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                                paintTicketLine(i, newline);
                            }
                        } else {
                            newline.setMultiply(newline.getMultiply() - 1.0);
                            newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                            paintTicketLine(i, newline);
                        }
                    } else {
                        if (isOverrideCheckEnabled()) {
                            // oCount = count + 1; //increment existing line
                            if (changeCount()) {
                                newline.setMultiply(newline.getMultiply() + 1.0);
                                newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                                paintTicketLine(i, newline);
                            }
                        } else {
                            newline.setMultiply(newline.getMultiply() + 1.0);
                            newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                            paintTicketLine(i, newline);
                        }
                    }
                }
            } else if (cTrans == '-'
                    && m_iNumberStatusInput == NUMBERZERO
                    && m_iNumberStatusPor == NUMBERZERO
                    && m_App.hasPermission("sales.EditLines")) {

                int i = m_ticketlines.getSelectedIndex();
                if (i < 0) {
                    Toolkit.getDefaultToolkit().beep();
                } else {
                    TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));

                    if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
                        if (isOverrideCheckEnabled()) {
                            // oCount = count - 1; //increment existing line
                            if (changeCount()) {
                                newline.setMultiply(newline.getMultiply() - 1.0);
                                newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                                paintTicketLine(i, newline);
                            }
                        } else {
                            newline.setMultiply(newline.getMultiply() - 1.0);
                            newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                            paintTicketLine(i, newline);
                        }

                        if (newline.getMultiply() >= 0) {
                            removeTicketLine(i);
                        } else {
                            paintTicketLine(i, newline);
                        }
                    } else {
                        if (isOverrideCheckEnabled()) {
                            // oCount = count - 1; //increment existing line

                            if (changeCount()) {
                                newline.setMultiply(newline.getMultiply() - 1.0);
                                newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                                paintTicketLine(i, newline);
                            }
                        } else {
                            newline.setMultiply(newline.getMultiply() - 1.0);
                            newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                            paintTicketLine(i, newline);
                        }

                        if (newline.getMultiply() <= 0.0) {
                            removeTicketLine(i);
                        } else {
                            paintTicketLine(i, newline);
                        }
                    }
                }

            } else if (cTrans == '+'
                    && m_iNumberStatusInput == NUMBERZERO
                    && m_iNumberStatusPor == NUMBERVALID) {
                int i = m_ticketlines.getSelectedIndex();

                if (i < 0) {
                    Toolkit.getDefaultToolkit().beep();
                } else {
                    double dPor = getPorValue();
                    TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));

                    if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
                        if (isOverrideCheckEnabled()) {
                            // oCount = count - 1; //increment existing line
                            if (changeCount()) {
                                newline.setMultiply(-dPor);
                                newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                                newline.setPrice(Math.abs(newline.getPrice()));
                                paintTicketLine(i, newline);
                            }
                        } else {
                            newline.setMultiply(-dPor);
                            newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                            newline.setPrice(Math.abs(newline.getPrice()));
                            paintTicketLine(i, newline);
                        }
                    } else {
                        if (isOverrideCheckEnabled()) {
                            // oCount = count + 1; //increment existing line

                            if (changeCount()) {
                                newline.setMultiply(dPor);
                                newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                                newline.setPrice(Math.abs(newline.getPrice()));
                                paintTicketLine(i, newline);
                            }
                        } else {
                            newline.setMultiply(dPor);
                            newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                            newline.setPrice(Math.abs(newline.getPrice()));
                            paintTicketLine(i, newline);
                        }
                    }
                }
            } else if (cTrans == '-'
                    && m_iNumberStatusInput == NUMBERZERO
                    && m_iNumberStatusPor == NUMBERVALID
                    && m_App.hasPermission("sales.EditLines")) {
                int i = m_ticketlines.getSelectedIndex();

                if (i < 0) {
                    Toolkit.getDefaultToolkit().beep();
                } else {
                    double dPor = getPorValue();
                    TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));

                    if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
                        if (isOverrideCheckEnabled()) {
                            // oCount = count - 1; //increment existing line

                            if (changeCount()) {
                                newline.setMultiply(-dPor);
                                newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                                newline.setPrice(Math.abs(newline.getPrice()));
                                paintTicketLine(i, newline);
                            }
                        } else {
                            newline.setMultiply(-dPor);
                            newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                            newline.setPrice(Math.abs(newline.getPrice()));
                            paintTicketLine(i, newline);
                        }
                    } else {
                        if (isOverrideCheckEnabled()) {
                            // oCount = count - 1; //increment existing line

                            if (changeCount()) {
                                newline.setMultiply(dPor);
                                newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                                newline.setPrice(Math.abs(newline.getPrice()));
                                paintTicketLine(i, newline);
                            }
                        } else {
                            newline.setMultiply(dPor);
                            newline.setProperty(TicketConstants.PROP_TICKET_UPDATED, "true");
                            newline.setPrice(Math.abs(newline.getPrice()));
                            paintTicketLine(i, newline);
                        }
                    }
                }
            } else if (cTrans == '+'
                    && m_iNumberStatusInput == NUMBERVALID
                    && m_iNumberStatusPor == NUMBERZERO
                    && m_App.hasPermission("sales.EditLines")) {
                ProductInfoExt product = getInputProduct();
                addTicketLine(product, 1.0, product.getPriceSell());
                m_jEditLine.doClick();

            } else if (cTrans == '-'
                    && m_iNumberStatusInput == NUMBERVALID
                    && m_iNumberStatusPor == NUMBERZERO
                    && m_App.hasPermission("sales.EditLines")) {
                ProductInfoExt product = getInputProduct();
                addTicketLine(product, 1.0, -product.getPriceSell());
                m_jEditLine.doClick();

            } else if (cTrans == '+'
                    && m_iNumberStatusInput == NUMBERVALID
                    && m_iNumberStatusPor == NUMBERVALID
                    && m_App.hasPermission("sales.EditLines")) {
                ProductInfoExt product = getInputProduct();
                addTicketLine(product, getPorValue(), product.getPriceSell());

            } else if (cTrans == '-'
                    && m_iNumberStatusInput == NUMBERVALID
                    && m_iNumberStatusPor == NUMBERVALID
                    && m_App.hasPermission("sales.EditLines")) {
                ProductInfoExt product = getInputProduct();
                addTicketLine(product, getPorValue(), -product.getPriceSell());

            } else if (cTrans == ' ' || cTrans == '=') {
                if (m_oTicket != null && m_oTicket.getLinesCount() > 0) {
                    if (closeTicket(m_oTicket, m_oTicketExt)) {
                        setActiveTicket(null, null);
                        refreshTicket();
                        // Delete will create a empty ticket
                        m_ticketsbag.deleteTicket();

                        if (isAutoLogout()) {
                            if (isRestaurantMode() && isAutoLogoutRestaurant()) {
                                deactivate();
                            } else {
                                ((JRootApp) m_App).closeAppView();
                            }
                        }

                        createNewTicket();
                    }
                    refreshTicket();
                } else {
                    Toolkit.getDefaultToolkit().beep();
                    LOGGER.log(System.Logger.Level.DEBUG, "Canno close Ticket, because m_oTicket is " + m_oTicket
                            + ", and LinesCount is " + (m_oTicket != null ? m_oTicket.getLinesCount() : 0));
                }
            }
        }
    }

    /**
     * Método para establecer el foco en el campo de búsqueda de productos
     */
    protected void setSearchFieldFocus() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (m_jKeyFactory != null) {
                m_jKeyFactory.requestFocusInWindow();
            }
        });
    }

    private void createNewTicket() {
        // Create New Ticket
        TicketInfo ticket = new TicketInfo();
        setActiveTicket(ticket, null);

        // Establecer foco en el campo de búsqueda después de crear nuevo ticket
        setSearchFieldFocus();
    }

    private boolean closeTicket(TicketInfo ticket, String ticketext) {
        if (listener != null) {
            listener.stop();
        }
        boolean resultok = false;

        if (m_App.hasPermission("sales.Total")) {

            try {

                LOGGER.log(System.Logger.Level.INFO,
                        "TicketInfo type (0:Receipt; 1:Refund) is " + ticket.getTicketType());
                JPaymentSelect paymentdialog = null;
                if (ticket.getTicketType() == TicketInfo.RECEIPT_NORMAL) {
                    paymentdialog = JPaymentSelectReceipt.getDialog(this);
                } else if (ticket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
                    paymentdialog = JPaymentSelectRefund.getDialog(this);
                }

                if (paymentdialog != null) {
                    paymentdialog.init(m_App);
                } else {
                    // SHOULD THROW EXCEPTION HERE
                }

                taxeslogic.calculateTaxes(ticket);
                if (ticket.getTotal() >= 0.0) {
                    ticket.resetPayments();
                }

                if (paymentdialog != null && executeEvent(ticket, ticketext, TicketConstants.EV_TICKET_TOTAL) == null) {
                    if (listener != null) {
                        listener.stop();
                    }

                    printTicket("Printer.TicketTotal", ticket, ticketext);

                    paymentdialog.setPrintSelected("true".equals(m_jbtnconfig.getProperty("printselected", "true")));

                    paymentdialog.setTransactionID(ticket.getTransactionID());

                    if (paymentdialog.showDialog(ticket.getTotal(), ticket.getCustomer())) {

                        ticket.setPayments(paymentdialog.getSelectedPayments());

                        String LOG = "Ticket payment Ticket total: " + ticket.getTotal()
                                + ";Dialog total: " + paymentdialog.getTotal()
                                + " ;Dialog paid: " + paymentdialog.getPaidTotal()
                                + " ;Payments Selected: " + paymentdialog.getSelectedPayments().size();

                        LOGGER.log(System.Logger.Level.INFO, LOG);

                        ticket.setUser(m_App.getAppUserView().getUser().getUserInfo());
                        ticket.setActiveCash(m_App.getActiveCashIndex());
                        ticket.setDate(new Date());

                        Object scriptResult = executeEvent(ticket, ticketext, TicketConstants.EV_TICKET_SAVE);

                        if (scriptResult == null) {
                            try {
                                dlSales.saveTicket(ticket, m_App.getInventoryLocation());

                                // Sebastian - Otorgar puntos automáticamente después de guardar el ticket
                                procesarPuntosAutomaticos(ticket);

                                /*
                                 * // Check low stock for products after the ticket is saved and notify the user
                                 * List<String> lowStockProducts = new ArrayList<>();
                                 * try {
                                 * String location = m_App.getInventoryLocation();
                                 * for (TicketLineInfo l : ticket.getLines()) {
                                 * if (l.getProductID() != null && !l.isProductService()) {
                                 * double current = dlSales.findProductStock(location, l.getProductID(),
                                 * l.getProductAttSetInstId());
                                 * // double min = dlSales.findProductMinimumStock(location, l.getProductID());
                                 * // if (current <= min) {
                                 * // lowStockProducts.add(l.getProductName() + " (" +
                                 * Formats.DOUBLE.formatValue(current) + ")");
                                 * // }
                                 * // bundle components too
                                 * List<ProductsBundleInfo> bundle =
                                 * dlSales.getProductsBundle(l.getProductID());
                                 * if (bundle.size() > 0) {
                                 * for (ProductsBundleInfo comp : bundle) {
                                 * double currentComp = dlSales.findProductStock(location,
                                 * comp.getProductBundleId(), null);
                                 * // double minComp = dlSales.findProductMinimumStock(location,
                                 * comp.getProductBundleId());
                                 * // if (currentComp <= minComp) {
                                 * // ProductInfoExt bundleProduct =
                                 * dlSales.getProductInfo(comp.getProductBundleId());
                                 * // lowStockProducts.add(bundleProduct.getName() + " (" +
                                 * Formats.DOUBLE.formatValue(currentComp) + ")");
                                 * // }
                                 * }
                                 * }
                                 * }
                                 * }
                                 * } catch (BasicException ex) {
                                 * LOGGER.log(System.Logger.Level.WARNING,
                                 * "Error while checking low stock after saving ticket", ex);
                                 * }
                                 * if (!lowStockProducts.isEmpty()) {
                                 * java.text.MessageFormat mf = new
                                 * java.text.MessageFormat(AppLocal.getIntString("message.stocklowlist"));
                                 * String prodNames = String.join(", ", lowStockProducts);
                                 * String message = mf.format(new Object[]{prodNames});
                                 * MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, message);
                                 * msg.show(this);
                                 * }
                                 */

                            } catch (BasicException ex) {
                                LOGGER.log(System.Logger.Level.ERROR, "Exception on save ticket ", ex);
                                // If exception contains the type we threw in DataLogicSales, show the improved
                                // dialog
                                String message = ex.getMessage();
                                if (message != null && message.contains("Insufficient stock for product")) {
                                    // Try to extract ID from message: pattern (id=PRODUCT_ID)
                                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\(id=(.*?)\\)");
                                    java.util.regex.Matcher m = p.matcher(message);
                                    if (m.find()) {
                                        String pid = m.group(1);
                                        try {
                                            ProductInfoExt prod = dlSales.getProductInfo(pid);
                                            showInsufficientStockDialog(prod);
                                            return false;
                                        } catch (Exception e) {
                                            // fallback to default message
                                        }
                                    }
                                }
                                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE,
                                        AppLocal.getIntString("message.nosaveticket"), ex);
                                msg.show(this);
                            }

                            String eventName = TicketConstants.EV_TICKET_CLOSE;
                            try {
                                executeEvent(ticket, ticketext, eventName,
                                        new ScriptArg("print", paymentdialog.isPrintSelected()),
                                        new ScriptArg("ticket", ticket));
                            } catch (Exception ex) {
                                LOGGER.log(System.Logger.Level.ERROR, "Exception on executeEvent: " + eventName, ex);
                            }

                            Boolean warrantyPrint = warrantyCheck(ticket);

                            String scriptName = paymentdialog.isPrintSelected() || warrantyPrint
                                    ? "Printer.Ticket"
                                    : "Printer.Ticket2";
                            try {

                                printTicket(scriptName, ticket, ticketext);
                                Notify(AppLocal.getIntString("notify.printing"));
                            } catch (Exception ex) {
                                LOGGER.log(System.Logger.Level.ERROR, "Exception on printTicket: " + scriptName, ex);
                            }

                            resultok = true;

                            if ("restaurant".equals(m_App.getProperties()
                                    .getProperty("machine.ticketsbag")) && !ticket.getOldTicket()) {
                                restDB.clearCustomerNameInTable(ticketext);
                                restDB.clearWaiterNameInTable(ticketext);
                                restDB.clearTicketIdInTable(ticketext);
                            }
                        }
                    }
                }
            } catch (TaxesException ex) {
                LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                        AppLocal.getIntString("message.cannotcalculatetaxes"));
                msg.show(this);
                resultok = false;
            }

            m_oTicket.resetTaxes();
            m_oTicket.resetPayments();
            jCheckStock.setText("");

        }

        return resultok;
    }

    private boolean warrantyCheck(TicketInfo ticket) {

        Boolean warrantyPrint = false;
        int lines = 0;
        while (lines < ticket.getLinesCount()) {
            if (!warrantyPrint) {
                warrantyPrint = ticket.getLine(lines).isProductWarranty();
                return true;
            }
            lines++;
        }
        return false;
    }

    /**
     *
     * @param pTicket
     * @return
     */
    public String getPickupString(TicketInfo pTicket) {
        if (pTicket == null) {
            return ("0");
        }
        String tmpPickupId = Integer.toString(pTicket.getPickupId());
        String pickupSize = (getAppProperty("till.pickupsize"));
        if (pickupSize != null && (Integer.parseInt(pickupSize) >= tmpPickupId.length())) {
            while (tmpPickupId.length() < (Integer.parseInt(pickupSize))) {
                tmpPickupId = "0" + tmpPickupId;
            }
        }
        return (tmpPickupId);
    }

    private void printTicket(String sresourcename, TicketInfo ticket, String ticketext) {

        String processTemaplated = "";
        LOGGER.log(System.Logger.Level.INFO, "Reading resource id: " + sresourcename);
        String sresource = dlSystem.getResourceAsXML(sresourcename);
        if (sresource == null) {
            LOGGER.log(System.Logger.Level.WARNING, "NOTFOUND content for resource id: " + sresourcename);
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotprintticket"));
            msg.show(JPanelTicket.this);
        } else {
            if (ticket.getPickupId() == 0) {
                try {
                    ticket.setPickupId(dlSales.getNextPickupIndex());
                } catch (BasicException ex) {
                    LOGGER.log(System.Logger.Level.WARNING, "Exception on get pickup id: ", ex);
                    ticket.setPickupId(0);
                }
            }

            try {
                ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);

                if (Boolean.parseBoolean(getAppProperty("receipt.newlayout"))) {
                    script.put("taxes", ticket.getTaxLines());
                } else {
                    script.put("taxes", taxcollection);
                }

                Boolean warrantyPrint = warrantyCheck(ticket);

                script.put("taxeslogic", taxeslogic);
                script.put("ticket", ticket);
                script.put("place", ticketext);
                script.put("warranty", warrantyPrint);
                script.put("pickupid", getPickupString(ticket));

                // TODO - MUST present to the progress o printing processing
                refreshTicket();

                processTemaplated = script.eval(sresource).toString();
                m_TTP.printTicket(processTemaplated, ticket);
            } catch (ScriptException | TicketPrinterException ex) {
                LOGGER.log(System.Logger.Level.WARNING, "Exception on processing/Print resource id: " + sresourcename,
                        ex);
                LOGGER.log(System.Logger.Level.DEBUG, "Exeception PROCESSED TEMPLATE: \n\r+++++++++++++\n\r "
                        + processTemaplated + "\n\r+++++++++++++\n\r");
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                        AppLocal.getIntString("message.cannotprintticket"), ex);
                msg.show(JPanelTicket.this);
            }
        }
    }

    public void printTicket(String resource) {
        LOGGER.log(System.Logger.Level.DEBUG, "JPanelTicket printTicket: " + resource);
        if (resource == null) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotexecute"));
            msg.show(this);
        } else {
            printTicket(resource, m_oTicket, m_oTicketExt);
        }

        Notify(AppLocal.getIntString("notify.printed"));
        j_btnRemotePrt.setEnabled(false);
    }

    public void customerAdd(String resource) {
        Notify(AppLocal.getIntString("notify.customeradd"));
    }

    public void customerRemove(String resource) {
        Notify(AppLocal.getIntString("notify.customerremove"));
    }

    public void customerChange(String resource) {
        Notify(AppLocal.getIntString("notify.customerchange"));
    }

    public void Notify(String msg) {

    }

    private void printReport(String resourcefile, TicketInfo ticket, String ticketext) {

        try {

            JasperReport jr = JPanelReport.createJasperReport(resourcefile);

            Map reportparams = new HashMap();

            try {
                reportparams.put("REPORT_RESOURCE_BUNDLE", ResourceBundle.getBundle(resourcefile + ".properties"));
            } catch (MissingResourceException ex) {
                LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
            }
            reportparams.put("TAXESLOGIC", taxeslogic);

            Map reportfields = new HashMap();
            reportfields.put("TICKET", ticket);
            reportfields.put("PLACE", ticketext);

            JasperPrint jp = JasperFillManager.fillReport(jr, reportparams,
                    new JRMapArrayDataSource(new Object[] { reportfields }));

            PrintService service = ReportUtils.getPrintService(getAppProperty("machine.printername"));

            JRPrinterAWT300.printPages(jp, 0, jp.getPages().size() - 1, service);

        } catch (JRException ex) {
            LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                    AppLocal.getIntString("message.cannotloadreport") + "<br>" + resourcefile, ex);
            msg.show(this);
        }
    }

    private void initDeviceDisplay() {
        var deviceDisplay = m_App.getDeviceTicket().getDeviceDisplay();
        if (deviceDisplay != null && deviceDisplay instanceof DeviceDisplayAdvance) {
            DeviceDisplayAdvance advDisplay = (DeviceDisplayAdvance) deviceDisplay;

            // TODO EVALUATE PERFORMANCE TO CREATE THIS EVERY TIME
            JTicketLines m_ticketlines2 = new JTicketLines(
                    this.dlSystem.getResourceAsXML(TicketConstants.RES_TICKET_LINES));
            m_ticketlines2.setTicketTableFont(new Font("Segoe UI", Font.PLAIN, 22)); // Fuente moderna y números grandes
                                                                                     // en tabla

            this.m_ticketlines.addListSelectionListener((ListSelectionEvent e) -> {
                EventQueue.invokeLater(() -> {
                    DeviceDisplayAdvance advDisplay1 = (DeviceDisplayAdvance) JPanelTicket.this.m_App.getDeviceTicket()
                            .getDeviceDisplay();
                    int ticketLineIndex = JPanelTicket.this.m_ticketlines.getSelectedIndex();
                    // FEATURE 1
                    if (advDisplay1.hasFeature(1) && !e.getValueIsAdjusting()) {
                        if (ticketLineIndex >= 0) {
                            try {
                                String sProductId = JPanelTicket.this.m_oTicket.getLine(ticketLineIndex).getProductID();
                                if (sProductId != null) {
                                    ProductInfoExt prod = JPanelTicket.this.dlSales.getProductInfo(sProductId);
                                    if (prod == null) {
                                        prod = dlSales.getProductInfoByCode(sProductId);
                                    }
                                    if (prod != null) {
                                        advDisplay1.setProductImage(prod.getImage());
                                    }
                                }
                            } catch (BasicException ex) {
                                LOGGER.log(System.Logger.Level.WARNING, "", ex);
                            }
                        }
                    }

                    // FEATURE 2
                    if (advDisplay.hasFeature(2)) {

                        m_ticketlines2.clearTicketLines();
                        for (int j = 0; JPanelTicket.this.m_oTicket != null
                                && j < JPanelTicket.this.m_oTicket.getLinesCount(); j++) {
                            m_ticketlines2.insertTicketLine(j, JPanelTicket.this.m_oTicket.getLine(j));
                        }
                        m_ticketlines2.setSelectedIndex(ticketLineIndex);

                        advDisplay.setTicketLines(m_ticketlines2);
                    }
                });
            });
        }

    }

    private void visorTicketLine(TicketLineInfo oLine) {
        if (oLine == null) {
            m_App.getDeviceTicket().getDeviceDisplay().clearVisor();
        } else {
            try {
                ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
                script.put("ticketline", oLine);
                m_TTP.printTicket(script.eval(dlSystem.getResourceAsXML("Printer.TicketLine")).toString());

            } catch (ScriptException | TicketPrinterException ex) {
                LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                        AppLocal.getIntString("message.cannotprintline"), ex);
                msg.show(JPanelTicket.this);
            }
        }
    }

    private Object evalScript(ScriptObject scr, String resource, ScriptArg... args) {

        // resource here is guaranteed to be not null
        try {
            scr.setSelectedIndex(m_ticketlines.getSelectedIndex());
            return scr.evalScript(dlSystem.getResourceAsXML(resource), args);
        } catch (ScriptException ex) {
            LOGGER.log(System.Logger.Level.WARNING, "Exception on executing script with resource id: " + resource, ex);
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotexecute"), ex);
            msg.show(this);
            return msg;
        }
    }

    /**
     *
     * @param resource
     * @param args
     */
    public void evalScriptAndRefresh(String resource, ScriptArg... args) {

        if (resource == null) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotexecute"));
            msg.show(this);
        } else {
            ScriptObject scr = new ScriptObject(m_oTicket, m_oTicketExt);
            scr.setSelectedIndex(m_ticketlines.getSelectedIndex());
            evalScript(scr, resource, args);
            refreshTicket();

            setSelectedIndex(scr.getSelectedIndex());
        }
    }

    private Object executeEvent(TicketInfo ticket, String ticketext, String eventkey, ScriptArg... args) {

        String resource = m_jbtnconfig.getEvent(eventkey);
        if (resource == null) {
            return null;
        } else {
            ScriptObject scr = new ScriptObject(ticket, ticketext);
            return evalScript(scr, resource, args);
        }
    }

    /**
     *
     * @param sresourcename
     * @return
     */
    public String getResourceAsXML(String sresourcename) {
        return dlSystem.getResourceAsXML(sresourcename);
    }

    /**
     *
     * @param sresourcename
     * @return
     */
    public BufferedImage getResourceAsImage(String sresourcename) {
        return dlSystem.getResourceAsImage(sresourcename);
    }

    private void setSelectedIndex(int i) {

        if (i >= 0 && i < m_oTicket.getLinesCount()) {
            m_ticketlines.setSelectedIndex(i);
        } else if (m_oTicket.getLinesCount() > 0) {
            m_ticketlines.setSelectedIndex(m_oTicket.getLinesCount() - 1);
        }
    }

    private String setTempjPrice(String jPrice) {
        jPrice = jPrice.replace(".", "");
        // remove all leading zeros from the string
        long tempL = Long.parseLong(jPrice);
        jPrice = Long.toString(tempL);

        while (jPrice.length() < 3) {
            jPrice = "0" + jPrice;
        }
        return (jPrice.length() <= 2) ? jPrice : (new StringBuffer(jPrice).insert(jPrice.length() - 2, ".").toString());
    }

    public void checkStock() {

        int i = m_ticketlines.getSelectedIndex();
        if (i >= 0) {
            if (listener != null) {
                listener.stop();
            }
            try {
                TicketLineInfo line = m_oTicket.getLine(i);
                String pId = line.getProductID();
                String location = m_App.getInventoryLocation();
                ProductStock checkProduct;
                checkProduct = dlSales.getProductStockState(pId, location);

                if (checkProduct != null) {

                    if (checkProduct.getUnits() <= 0) {
                        jCheckStock.setForeground(Color.magenta);
                    } else {
                        jCheckStock.setForeground(Color.darkGray);
                    }

                    String content;

                    if (!location.equals(checkProduct.getLocation())) {
                        content = AppLocal.getIntString("message.location.current");
                        JFrame frame = new JFrame();
                        JOptionPane.showMessageDialog(frame,
                                content,
                                "Info",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        double dUnits = checkProduct.getUnits();
                        int iUnits;
                        iUnits = (int) dUnits;

                        jCheckStock.setText(Integer.toString(iUnits));
                    }

                } else {
                    jCheckStock.setText(null);
                }
            } catch (BasicException ex) {
                LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
            } finally {

                if (listener != null) {
                    listener.restart();
                }
            }
        } else {
            Toolkit.getDefaultToolkit().beep();
        }

    }

    public void checkCustomer() {
        if (m_oTicket.getCustomer().isVIP() == true) {

            String content;
            String vip;
            String discount;

            if (m_oTicket.getCustomer().isVIP() == true) {
                vip = AppLocal.getIntString("message.vipyes");
            } else {
                vip = AppLocal.getIntString("message.vipno");
            }
            if (m_oTicket.getCustomer().getDiscount() > 0) {
                discount = AppLocal.getIntString("message.discyes") + m_oTicket.getCustomer().getDiscount() + "%";
            } else {
                discount = AppLocal.getIntString("message.discno");
            }

            content = "<html>"
                    + "<b>" + AppLocal.getIntString("label.vip") + " : " + "</b>" + vip + "<br>"
                    + "<b>" + AppLocal.getIntString("label.discount") + " : " + "</b>" + discount + "<br>" + "</html>";

            JFrame frame = new JFrame();
            JOptionPane.showMessageDialog(frame,
                    content,
                    "Customer Discount Info",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // Sebastian - Método para actualizar información de puntos del cliente
    private void updateCustomerPointsDisplay() {
        System.out.println("🔍 updateCustomerPointsDisplay() called");

        if (m_oTicket.getCustomer() != null) {
            System.out.println("📋 Cliente detectado: " + m_oTicket.getCustomer().getName());

            try {
                // Obtener puntos actuales del cliente
                int puntosActuales = puntosDataLogic.obtenerPuntos(m_oTicket.getCustomer().getId());

                System.out.println("💯 Puntos actuales del cliente: " + puntosActuales);

                // Calcular total solo de productos que acumulan puntos
                double totalAcumulable = 0.0;
                for (TicketLineInfo line : m_oTicket.getLines()) {
                    if (line.isProductAccumulatesPoints()) {
                        totalAcumulable += line.getValue();
                    }
                }

                System.out.println("💰 Total ticket: $" + m_oTicket.getTotal());
                System.out.println("✅ Total acumulable (solo productos marcados): $" + totalAcumulable);

                PuntosConfiguracion config = puntosDataLogic.getConfiguracionActiva();
                int puntosNuevos = 0;

                if (config != null && config.isSistemaActivo()) {
                    // Sebastian - Calcular puntos solo sobre el monto acumulable
                    puntosNuevos = config.calcularPuntos(totalAcumulable);

                    // Debug adicional
                    System.out.println("🔧 DEBUG - Total acumulable: $" + totalAcumulable);
                    System.out.println("🔧 DEBUG - Monto por punto: $" + config.getMontoPorPunto());
                    System.out.println("🔧 DEBUG - Puntos otorgados: " + config.getPuntosOtorgados());

                    // Calcular tramos para mostrar la lógica
                    int tramosCompletos = (int) Math.floor(totalAcumulable / config.getMontoPorPunto());
                    System.out.println("🔧 DEBUG - Tramos completos: " + tramosCompletos + " (cada tramo = $"
                            + config.getMontoPorPunto() + ")");
                    System.out.println("🔧 DEBUG - Cálculo por tramos: " + tramosCompletos + " × "
                            + config.getPuntosOtorgados() + " = " + puntosNuevos + " puntos");

                    // Sebastian - Verificar límite diario
                    try {
                        int puntosGanadosHoy = puntosDataLogic.getPuntosGanadosHoy(m_oTicket.getCustomer().getId());
                        int limiteDiario = config.getLimiteDiarioPuntos();

                        System.out.println("📊 DEBUG - Puntos ganados hoy: " + puntosGanadosHoy + "/" + limiteDiario);

                        // Ajustar puntos si exceden el límite
                        if (puntosGanadosHoy >= limiteDiario) {
                            puntosNuevos = 0;
                            System.out.println("🚫 LÍMITE DIARIO ALCANZADO - No se otorgarán más puntos");
                        } else if (puntosGanadosHoy + puntosNuevos > limiteDiario) {
                            puntosNuevos = limiteDiario - puntosGanadosHoy;
                            System.out.println("⚠️ PUNTOS AJUSTADOS por límite diario: " + puntosNuevos);
                        }
                    } catch (Exception ex) {
                        System.err.println("⚠️ Error verificando límite diario: " + ex.getMessage());
                    }
                }

                int puntosFuturos = puntosActuales + puntosNuevos;

                System.out.println("🔮 Puntos nuevos a ganar: " + puntosNuevos);
                System.out.println("🎯 Puntos futuros: " + puntosFuturos);

                // Mostrar nombre del cliente y puntos en formato: "Juan Sebastian 360 → 450"
                String nombreCliente = m_oTicket.getCustomer().getName();
                String textoCompleto = String.format("%s %d → %d",
                        nombreCliente, puntosActuales, puntosFuturos);

                System.out.println("📝 Texto a mostrar: '" + textoCompleto + "'");

                m_jCustomerPoints.setText(textoCompleto);
                m_jCustomerPoints.setVisible(true);

                System.out.println("✅ Label actualizado y visible");

            } catch (Exception ex) {
                System.err.println("❌ Error updating customer points display: " + ex.getMessage());
                ex.printStackTrace();
                LOGGER.log(System.Logger.Level.WARNING, "Error updating customer points display: ", ex);
                m_jCustomerPoints.setText(m_oTicket.getCustomer().getName());
                m_jCustomerPoints.setVisible(true);
            }
        } else {
            System.out.println("🚫 No hay cliente asignado al ticket");
            m_jCustomerPoints.setText("");
            m_jCustomerPoints.setVisible(false);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the FormEditor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        m_jPanelContainer = new javax.swing.JPanel();
        m_jPanelMainToolbar = new javax.swing.JPanel();
        m_jPanelBag = new javax.swing.JPanel();
        jTBtnShow = new javax.swing.JToggleButton();
        m_jbtnScale = new javax.swing.JButton();
        m_jButtons = new javax.swing.JPanel();
        btnSplit = new javax.swing.JButton();
        btnReprint1 = new javax.swing.JButton();
        j_btnRemotePrt = new javax.swing.JButton();
        jBtnCustomer = new javax.swing.JButton();
        m_jPanelScripts = new javax.swing.JPanel();
        m_jPanelBagExt = new javax.swing.JPanel();
        m_jPanelBagExtDefaultEmpty = new javax.swing.JPanel();
        m_jPanelTicket = new javax.swing.JPanel();
        m_jPanelLinesToolbar = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        m_jDelete = new javax.swing.JButton();
        m_jList = new javax.swing.JButton();
        m_jEditLine = new javax.swing.JButton();
        jEditAttributes = new javax.swing.JButton();
        jCheckStock = new javax.swing.JButton();
        m_jPanelLines = new javax.swing.JPanel();
        m_jPanelLinesSum = new javax.swing.JPanel();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 0),
                new java.awt.Dimension(5, 32767));
        m_jTicketId = new javax.swing.JLabel();
        m_jCustomerPoints = new javax.swing.JLabel(); // Sebastian - Label para puntos del cliente
        m_jPanelTotals = new javax.swing.JPanel();
        m_jLblSubTotalEuros = new javax.swing.JLabel();
        m_jLblTaxEuros = new javax.swing.JLabel();
        m_jLblTotalEuros = new javax.swing.JLabel();
        m_jSubtotalEuros = new javax.swing.JLabel();
        m_jTaxesEuros = new javax.swing.JLabel();
        m_jTotalEuros = new javax.swing.JLabel();
        m_jContEntries = new javax.swing.JPanel();
        m_jPanEntries = new javax.swing.JPanel();
        m_jNumberKeys = new com.openbravo.beans.JNumberKeys();
        jPanelScanner = new javax.swing.JPanel();
        m_jPrice = new javax.swing.JLabel();
        m_jEnter = new javax.swing.JButton();
        m_jPor = new javax.swing.JLabel();
        m_jKeyFactory = new javax.swing.JTextField();
        m_jaddtax = new javax.swing.JCheckBox();
        m_jTax = new javax.swing.JComboBox();

        // Sebastian - Inicialización campos de cliente
        m_jLblCustomerId = new javax.swing.JLabel();
        m_jCustomerId = new javax.swing.JTextField();
        m_jCustomerName = new javax.swing.JLabel();

        m_jPanelCatalog = new javax.swing.JPanel();

        setBackground(new java.awt.Color(255, 204, 153));
        setOpaque(false);
        setLayout(new java.awt.CardLayout());

        m_jPanelContainer.setLayout(new java.awt.BorderLayout());

        m_jPanelMainToolbar.setLayout(new java.awt.BorderLayout());

        m_jPanelBag.setAutoscrolls(true);
        m_jPanelBag.setMaximumSize(new java.awt.Dimension(300, 100)); // Sebastian - Permitir que el panel sea visible
        m_jPanelBag.setPreferredSize(new java.awt.Dimension(200, 60)); // Sebastian - Hacer visible el panel de tickets

        jTBtnShow.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jTBtnShow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/resources.png"))); // NOI18N
        jTBtnShow.setPreferredSize(new java.awt.Dimension(80, 45));
        jTBtnShow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTBtnShowActionPerformed(evt);
            }
        });
        // Sebastian - Ocultar botones de toolbar para interfaz más limpia
        jTBtnShow.setVisible(false);
        m_jPanelBag.add(jTBtnShow);

        m_jbtnScale.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jbtnScale.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/scale.png"))); // NOI18N
        m_jbtnScale.setText(AppLocal.getIntString("button.scale")); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("pos_messages"); // NOI18N
        m_jbtnScale.setToolTipText(bundle.getString("tooltip.scale")); // NOI18N
        m_jbtnScale.setFocusPainted(false);
        m_jbtnScale.setFocusable(false);
        m_jbtnScale.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jbtnScale.setMaximumSize(new java.awt.Dimension(85, 44));
        m_jbtnScale.setMinimumSize(new java.awt.Dimension(85, 44));
        m_jbtnScale.setPreferredSize(new java.awt.Dimension(85, 45));
        m_jbtnScale.setRequestFocusEnabled(false);
        m_jbtnScale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jbtnScaleActionPerformed(evt);
            }
        });
        m_jbtnScale.setVisible(false);
        m_jPanelBag.add(m_jbtnScale);

        m_jButtons.setPreferredSize(new java.awt.Dimension(350, 55));

        btnSplit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/sale_split_sml.png"))); // NOI18N
        btnSplit.setToolTipText(bundle.getString("tooltip.salesplit")); // NOI18N
        btnSplit.setEnabled(false);
        btnSplit.setFocusPainted(false);
        btnSplit.setFocusable(false);
        btnSplit.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnSplit.setMaximumSize(new java.awt.Dimension(50, 40));
        btnSplit.setMinimumSize(new java.awt.Dimension(50, 40));
        btnSplit.setPreferredSize(new java.awt.Dimension(80, 45));
        btnSplit.setRequestFocusEnabled(false);
        btnSplit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSplitActionPerformed(evt);
            }
        });

        btnReprint1.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        btnReprint1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/reprint24.png"))); // NOI18N
        btnReprint1.setToolTipText(bundle.getString("tooltip.reprintLastTicket")); // NOI18N
        btnReprint1.setFocusPainted(false);
        btnReprint1.setFocusable(false);
        btnReprint1.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnReprint1.setMaximumSize(new java.awt.Dimension(50, 40));
        btnReprint1.setMinimumSize(new java.awt.Dimension(50, 40));
        btnReprint1.setPreferredSize(new java.awt.Dimension(80, 45));
        btnReprint1.setRequestFocusEnabled(false);
        btnReprint1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReprint1ActionPerformed(evt);
            }
        });

        j_btnRemotePrt.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        j_btnRemotePrt
                .setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/remote_print.png"))); // NOI18N
        j_btnRemotePrt.setText(bundle.getString("button.sendorder")); // NOI18N
        j_btnRemotePrt.setToolTipText(bundle.getString("tooltip.printtoremote")); // NOI18N
        j_btnRemotePrt.setMargin(new java.awt.Insets(0, 4, 0, 4));
        j_btnRemotePrt.setMaximumSize(new java.awt.Dimension(50, 40));
        j_btnRemotePrt.setMinimumSize(new java.awt.Dimension(50, 40));
        j_btnRemotePrt.setPreferredSize(new java.awt.Dimension(80, 45));
        j_btnRemotePrt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                j_btnRemotePrtActionPerformed(evt);
            }
        });

        jBtnCustomer.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jBtnCustomer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/customer.png"))); // NOI18N
        jBtnCustomer.setToolTipText(bundle.getString("tooltip.salescustomer") + " (F2)"); // NOI18N
        jBtnCustomer.setPreferredSize(new java.awt.Dimension(80, 45));
        jBtnCustomer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnCustomerActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout m_jButtonsLayout = new javax.swing.GroupLayout(m_jButtons);
        m_jButtons.setLayout(m_jButtonsLayout);
        m_jButtonsLayout.setHorizontalGroup(
                m_jButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(m_jButtonsLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jBtnCustomer, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnSplit, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(j_btnRemotePrt, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnReprint1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        m_jButtonsLayout.setVerticalGroup(
                m_jButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(m_jButtonsLayout.createSequentialGroup()
                                .addGap(5, 5, 5)
                                .addGroup(m_jButtonsLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(j_btnRemotePrt, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnSplit, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnReprint1, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jBtnCustomer, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        // Sebastian - Ocultar panel de botones para interfaz más limpia
        m_jButtons.setVisible(false);
        m_jPanelBag.add(m_jButtons);

        m_jPanelMainToolbar.add(m_jPanelBag, java.awt.BorderLayout.PAGE_START);

        m_jPanelScripts.setPreferredSize(new java.awt.Dimension(200, 60));
        m_jPanelScripts.setLayout(new java.awt.BorderLayout());

        m_jPanelBagExt.setPreferredSize(new java.awt.Dimension(20, 60));

        m_jPanelBagExtDefaultEmpty.setMinimumSize(new java.awt.Dimension(235, 50));
        m_jPanelBagExtDefaultEmpty.setPreferredSize(new java.awt.Dimension(10, 55));
        m_jPanelBagExt.add(m_jPanelBagExtDefaultEmpty);

        m_jPanelScripts.add(m_jPanelBagExt, java.awt.BorderLayout.PAGE_START);

        m_jPanelMainToolbar.add(m_jPanelScripts, java.awt.BorderLayout.CENTER);
        m_jPanelScripts.getAccessibleContext().setAccessibleDescription("");

        m_jPanelTicket.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        m_jPanelTicket.setLayout(new java.awt.BorderLayout());

        m_jPanelLinesToolbar.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jPanelLinesToolbar.setPreferredSize(new java.awt.Dimension(65, 270));
        m_jPanelLinesToolbar.setLayout(new java.awt.BorderLayout());

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 5));
        jPanel2.setPreferredSize(new java.awt.Dimension(80, 250));
        jPanel2.setLayout(new java.awt.GridLayout(3, 1, 5, 15)); // Sebastian - 3 filas para 3 botones con más espaciado

        // Sebastian - Reemplazar los 5 botones originales con solo 2 botones
        // personalizados
        // Los botones originales se mantienen para compatibilidad pero se hacen
        // invisibles

        // Hacer invisibles los botones originales

        m_jDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/editdelete.png"))); // NOI18N
        m_jDelete.setToolTipText(bundle.getString("tooltip.saleremoveline")); // NOI18N
        m_jDelete.setFocusPainted(false);
        m_jDelete.setFocusable(false);
        m_jDelete.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jDelete.setMaximumSize(new java.awt.Dimension(42, 36));
        m_jDelete.setMinimumSize(new java.awt.Dimension(42, 36));
        m_jDelete.setPreferredSize(new java.awt.Dimension(50, 45));
        m_jDelete.setRequestFocusEnabled(false);
        m_jDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jDeleteActionPerformed(evt);
            }
        });
        jPanel2.add(m_jDelete);

        m_jList.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search32.png"))); // NOI18N
        m_jList.setToolTipText(bundle.getString("tooltip.saleproductfind")); // NOI18N
        m_jList.setFocusPainted(false);
        m_jList.setFocusable(false);
        m_jList.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jList.setMaximumSize(new java.awt.Dimension(42, 36));
        m_jList.setMinimumSize(new java.awt.Dimension(42, 36));
        m_jList.setPreferredSize(new java.awt.Dimension(50, 45));
        m_jList.setRequestFocusEnabled(false);
        m_jList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jListActionPerformed(evt);
            }
        });
        jPanel2.add(m_jList);

        m_jEditLine
                .setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/sale_editline.png"))); // NOI18N
        m_jEditLine.setToolTipText(bundle.getString("tooltip.saleeditline")); // NOI18N
        m_jEditLine.setFocusPainted(false);
        m_jEditLine.setFocusable(false);
        m_jEditLine.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jEditLine.setMaximumSize(new java.awt.Dimension(42, 36));
        m_jEditLine.setMinimumSize(new java.awt.Dimension(42, 36));
        m_jEditLine.setPreferredSize(new java.awt.Dimension(50, 45));
        m_jEditLine.setRequestFocusEnabled(false);
        m_jEditLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jEditLineActionPerformed(evt);
            }
        });
        jPanel2.add(m_jEditLine);

        jEditAttributes
                .setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/attributes.png"))); // NOI18N
        jEditAttributes.setToolTipText(bundle.getString("tooltip.saleattributes")); // NOI18N
        jEditAttributes.setFocusPainted(false);
        jEditAttributes.setFocusable(false);
        jEditAttributes.setMargin(new java.awt.Insets(8, 14, 8, 14));
        jEditAttributes.setMaximumSize(new java.awt.Dimension(42, 36));
        jEditAttributes.setMinimumSize(new java.awt.Dimension(42, 36));
        jEditAttributes.setPreferredSize(new java.awt.Dimension(50, 45));
        jEditAttributes.setRequestFocusEnabled(false);
        jEditAttributes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jEditAttributesActionPerformed(evt);
            }
        });
        jPanel2.add(jEditAttributes);

        jCheckStock.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        jCheckStock.setForeground(new java.awt.Color(76, 197, 237));
        jCheckStock.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/info.png"))); // NOI18N
        jCheckStock.setToolTipText(bundle.getString("tooltip.salecheckstock")); // NOI18N
        jCheckStock.setFocusPainted(false);
        jCheckStock.setFocusable(false);
        jCheckStock.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jCheckStock.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        jCheckStock.setMargin(new java.awt.Insets(8, 4, 8, 4));
        jCheckStock.setMaximumSize(new java.awt.Dimension(42, 36));
        jCheckStock.setMinimumSize(new java.awt.Dimension(42, 36));
        jCheckStock.setPreferredSize(new java.awt.Dimension(80, 45));
        jCheckStock.setRequestFocusEnabled(false);
        jCheckStock.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        jCheckStock.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jCheckStockMouseClicked(evt);
            }
        });
        jCheckStock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckStockActionPerformed(evt);
            }
        });

        // Sebastian - Hacer invisibles los 5 botones originales
        m_jDelete.setVisible(false);
        m_jList.setVisible(false);
        m_jEditLine.setVisible(false);
        jEditAttributes.setVisible(false);
        jCheckStock.setVisible(false);

        // Agregar los botones originales (ocultos) para mantener compatibilidad
        jPanel2.add(m_jDelete);
        jPanel2.add(m_jList);
        jPanel2.add(m_jEditLine);
        jPanel2.add(jEditAttributes);
        jPanel2.add(jCheckStock);

        // Sebastian - Limpiar el panel y agregar solo mis 2 botones personalizados
        jPanel2.removeAll();

        // Botón 1 - Nueva Venta (Nueva Pestaña)
        javax.swing.JButton btnNuevaVenta = new javax.swing.JButton();
        btnNuevaVenta.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/editnew.png")));
        btnNuevaVenta.setToolTipText("Nueva Venta (Nueva Pestaña)");
        btnNuevaVenta.setFocusPainted(false);
        btnNuevaVenta.setFocusable(false);
        btnNuevaVenta.setMargin(new java.awt.Insets(8, 8, 8, 8));
        btnNuevaVenta.setPreferredSize(new java.awt.Dimension(60, 45));
        btnNuevaVenta.setRequestFocusEnabled(false);
        btnNuevaVenta.setOpaque(false);
        btnNuevaVenta.setContentAreaFilled(false);
        btnNuevaVenta.setBorderPainted(false);
        btnNuevaVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                abrirNuevaVenta();
            }
        });
        jPanel2.add(btnNuevaVenta);

        // Botón 2 - Cambiar entre Ventas
        javax.swing.JButton btnCambiarVenta = new javax.swing.JButton();
        btnCambiarVenta
                .setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/bookmark.png")));
        btnCambiarVenta.setToolTipText("Ver Ventas Pendientes");
        btnCambiarVenta.setFocusPainted(false);
        btnCambiarVenta.setFocusable(false);
        btnCambiarVenta.setMargin(new java.awt.Insets(8, 8, 8, 8));
        btnCambiarVenta.setPreferredSize(new java.awt.Dimension(60, 45));
        btnCambiarVenta.setRequestFocusEnabled(false);
        btnCambiarVenta.setOpaque(false);
        btnCambiarVenta.setContentAreaFilled(false);
        btnCambiarVenta.setBorderPainted(false);
        btnCambiarVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mostrarVentasPendientes();
            }
        });
        jPanel2.add(btnCambiarVenta);

        // Botón 3 - ID Cliente
        javax.swing.JButton btnIdCliente = new javax.swing.JButton();
        btnIdCliente.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/customer.png")));
        btnIdCliente.setToolTipText("Ingresar ID Cliente");
        btnIdCliente.setFocusPainted(false);
        btnIdCliente.setFocusable(false);
        btnIdCliente.setMargin(new java.awt.Insets(8, 8, 8, 8));
        btnIdCliente.setPreferredSize(new java.awt.Dimension(60, 45));
        btnIdCliente.setRequestFocusEnabled(false);
        btnIdCliente.setOpaque(false);
        btnIdCliente.setContentAreaFilled(false);
        btnIdCliente.setBorderPainted(false);
        btnIdCliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mostrarModalIdCliente();
            }
        });
        jPanel2.add(btnIdCliente);

        m_jPanelLinesToolbar.add(jPanel2, java.awt.BorderLayout.NORTH);

        m_jPanelTicket.add(m_jPanelLinesToolbar, java.awt.BorderLayout.LINE_START);

        m_jPanelLines.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        // Sebastian - Expandir el panel de líneas para ocupar más espacio horizontal
        m_jPanelLines.setPreferredSize(new java.awt.Dimension(750, 240));
        m_jPanelLines.setLayout(new java.awt.BorderLayout());

        m_jPanelLinesSum.setLayout(new java.awt.BorderLayout());
        m_jPanelLinesSum.add(filler2, java.awt.BorderLayout.LINE_START);

        // Sebastian - Configuración del panel de cliente
        javax.swing.JPanel customerPanel = new javax.swing.JPanel();
        customerPanel.setLayout(new java.awt.BorderLayout());
        customerPanel.setPreferredSize(new java.awt.Dimension(300, 30));

        javax.swing.JPanel customerInputPanel = new javax.swing.JPanel();
        customerInputPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 2));

        m_jLblCustomerId.setFont(new java.awt.Font("Arial", 1, 11)); // NOI18N
        m_jLblCustomerId.setText(AppLocal.getIntString("label.customerid")); // NOI18N
        customerInputPanel.add(m_jLblCustomerId);

        m_jCustomerId.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        m_jCustomerId.setPreferredSize(new java.awt.Dimension(100, 20));
        m_jCustomerId.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                m_jCustomerIdKeyReleased(evt);
            }
        });
        customerInputPanel.add(m_jCustomerId);

        customerPanel.add(customerInputPanel, java.awt.BorderLayout.WEST);

        m_jCustomerName.setFont(new java.awt.Font("Arial", 1, 11)); // NOI18N
        m_jCustomerName.setForeground(new java.awt.Color(0, 100, 0));
        m_jCustomerName.setText("");
        customerPanel.add(m_jCustomerName, java.awt.BorderLayout.CENTER);

        // Ocultar el panel de ID de cliente
        customerPanel.setVisible(false);

        m_jPanelLinesSum.add(customerPanel, java.awt.BorderLayout.NORTH);

        m_jTicketId.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        m_jTicketId.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jTicketId.setText("ID");
        m_jTicketId.setToolTipText("");
        m_jTicketId.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        m_jTicketId.setOpaque(true);
        m_jTicketId.setPreferredSize(new java.awt.Dimension(300, 40));
        m_jTicketId.setRequestFocusEnabled(false);
        m_jTicketId.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        m_jPanelLinesSum.add(m_jTicketId, java.awt.BorderLayout.CENTER);

        // Sebastian - Configuración del label de puntos del cliente
        m_jCustomerPoints.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N - Más grande para la posición superior
        m_jCustomerPoints.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        m_jCustomerPoints.setText("");
        m_jCustomerPoints.setToolTipText("Puntos del cliente");
        m_jCustomerPoints.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        m_jCustomerPoints.setOpaque(true);
        m_jCustomerPoints.setPreferredSize(new java.awt.Dimension(320, 35)); // Más ancho y altura ajustada
        m_jCustomerPoints.setRequestFocusEnabled(false);
        m_jCustomerPoints.setVerticalTextPosition(javax.swing.SwingConstants.CENTER);
        m_jCustomerPoints.setForeground(new java.awt.Color(0, 80, 0)); // Verde más oscuro
        m_jCustomerPoints.setBackground(new java.awt.Color(245, 255, 245)); // Fondo verde muy claro
        m_jCustomerPoints.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 150, 0), 2), // Borde verde
                javax.swing.BorderFactory.createEmptyBorder(5, 10, 5, 10) // Padding interno
        ));
        // No añadir aquí, se añadirá al customerPointsPanel más adelante

        m_jPanelTotals.setPreferredSize(new java.awt.Dimension(620, 120)); // Más ancho y alto para números MUY grandes
        m_jPanelTotals.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 50, 0, 0)); // Margen izquierdo para
                                                                                            // empujar a la derecha
        m_jPanelTotals.setLayout(new java.awt.GridLayout(2, 3, 4, 0));

        m_jLblSubTotalEuros.setFont(new java.awt.Font("Segoe UI", 1, 16)); // Fuente moderna y más grande
        m_jLblSubTotalEuros.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        m_jLblSubTotalEuros.setLabelFor(m_jSubtotalEuros);
        m_jLblSubTotalEuros.setText(AppLocal.getIntString("label.subtotalcash")); // NOI18N
        m_jPanelTotals.add(m_jLblSubTotalEuros);

        m_jLblTaxEuros.setFont(new java.awt.Font("Segoe UI", 1, 16)); // Fuente moderna y más grande
        m_jLblTaxEuros.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        m_jLblTaxEuros.setLabelFor(m_jSubtotalEuros);
        m_jLblTaxEuros.setText(AppLocal.getIntString("label.taxcash")); // NOI18N
        m_jPanelTotals.add(m_jLblTaxEuros);

        m_jLblTotalEuros.setFont(new java.awt.Font("Segoe UI", 1, 18)); // Fuente moderna y más grande para el total
        m_jLblTotalEuros.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        m_jLblTotalEuros.setLabelFor(m_jTotalEuros);
        m_jLblTotalEuros.setText(AppLocal.getIntString("label.totalcash")); // NOI18N
        m_jPanelTotals.add(m_jLblTotalEuros);

        m_jSubtotalEuros.setBackground(m_jEditLine.getBackground());
        m_jSubtotalEuros.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 32)); // Fuente moderna y números
                                                                                          // más grandes
        m_jSubtotalEuros.setForeground(m_jEditLine.getForeground());
        m_jSubtotalEuros.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        m_jSubtotalEuros.setLabelFor(m_jSubtotalEuros);
        m_jSubtotalEuros.setToolTipText(bundle.getString("tooltip.salesubtotal")); // NOI18N
        m_jSubtotalEuros.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 2, true));
        m_jSubtotalEuros.setMaximumSize(new java.awt.Dimension(180, 45));
        m_jSubtotalEuros.setMinimumSize(new java.awt.Dimension(120, 45));
        m_jSubtotalEuros.setPreferredSize(new java.awt.Dimension(130, 45));
        m_jSubtotalEuros.setRequestFocusEnabled(false);
        m_jPanelTotals.add(m_jSubtotalEuros);

        m_jTaxesEuros.setBackground(m_jEditLine.getBackground());
        m_jTaxesEuros.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 32)); // Fuente moderna y números más
                                                                                       // grandes
        m_jTaxesEuros.setForeground(m_jEditLine.getForeground());
        m_jTaxesEuros.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        m_jTaxesEuros.setLabelFor(m_jTaxesEuros);
        m_jTaxesEuros.setToolTipText(bundle.getString("tooltip.saletax")); // NOI18N
        m_jTaxesEuros.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 2, true));
        m_jTaxesEuros.setMaximumSize(new java.awt.Dimension(180, 45));
        m_jTaxesEuros.setMinimumSize(new java.awt.Dimension(120, 45));
        m_jTaxesEuros.setPreferredSize(new java.awt.Dimension(130, 45));
        m_jTaxesEuros.setRequestFocusEnabled(false);
        m_jPanelTotals.add(m_jTaxesEuros);

        m_jTotalEuros.setBackground(m_jEditLine.getBackground());
        m_jTotalEuros.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 56)); // Fuente moderna, bold y números
                                                                                      // MUY grandes
        m_jTotalEuros.setForeground(m_jEditLine.getForeground());
        m_jTotalEuros.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        m_jTotalEuros.setLabelFor(m_jTotalEuros);
        m_jTotalEuros.setToolTipText(bundle.getString("tooltip.saletotal")); // NOI18N
        m_jTotalEuros.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 3, true));
        m_jTotalEuros.setMaximumSize(new java.awt.Dimension(300, 70));
        m_jTotalEuros.setMinimumSize(new java.awt.Dimension(220, 70));
        m_jTotalEuros.setPreferredSize(new java.awt.Dimension(260, 70));
        m_jTotalEuros.setRequestFocusEnabled(false);
        m_jPanelTotals.add(m_jTotalEuros);

        // Sebastian - Panel para botón Pagar ANTES del subtotal
        javax.swing.JPanel payBeforePanel = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 6));
        payBeforePanel.setOpaque(false);

        // Botón Pagar (verde, destacado) - colocado antes del subtotal
        m_jPayNow = new javax.swing.JButton();
        m_jPayNow.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        m_jPayNow.setText(
                "<html><center>" + AppLocal.getIntString("button.pay") + "<br><small>(F5)</small></center></html>"); // "Pagar
                                                                                                                     // (F5)"
        m_jPayNow.setFocusPainted(false);
        m_jPayNow.setPreferredSize(new java.awt.Dimension(140, 40));
        m_jPayNow.setBackground(new java.awt.Color(46, 139, 87)); // SeaGreen
        m_jPayNow.setForeground(java.awt.Color.WHITE);
        m_jPayNow.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(34, 120, 60), 1, true));
        m_jPayNow.setOpaque(true);

        // Acción: reutiliza el flujo de cierre/pago de ticket
        m_jPayNow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (m_oTicket != null && m_oTicket.getLinesCount() > 0) {
                    if (closeTicket(m_oTicket, m_oTicketExt)) {
                        setActiveTicket(null, null);
                        refreshTicket();
                        m_ticketsbag.deleteTicket();
                        createNewTicket();
                    }
                    refreshTicket();
                }
            }
        });
        payBeforePanel.add(m_jPayNow);

        // Crear un panel wrapper que contenga los totales y el botón Pagar abajo a la
        // derecha
        javax.swing.JPanel totalsWithPay = new javax.swing.JPanel(new java.awt.BorderLayout());
        totalsWithPay.setOpaque(false);
        totalsWithPay.add(payBeforePanel, java.awt.BorderLayout.NORTH); // Botón arriba
        totalsWithPay.add(m_jPanelTotals, java.awt.BorderLayout.CENTER);

        // Sebastian - Botón pequeño de Entradas y Salidas en la parte inferior
        javax.swing.JPanel btnEntradasSalidasPanel = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 5));
        btnEntradasSalidasPanel.setOpaque(false);
        javax.swing.JButton btnEntradasSalidas = new javax.swing.JButton("E/S");
        btnEntradasSalidas.setPreferredSize(new java.awt.Dimension(60, 25));
        btnEntradasSalidas.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 10));
        btnEntradasSalidas.setBackground(new java.awt.Color(255, 140, 0)); // Color naranja
        btnEntradasSalidas.setForeground(java.awt.Color.WHITE);
        btnEntradasSalidas.setFocusPainted(false);
        btnEntradasSalidas.setBorderPainted(true);
        btnEntradasSalidas.setToolTipText("Entradas y Salidas");
        btnEntradasSalidas.addActionListener(e -> {
            showEntradasSalidasDialog();
        });
        btnEntradasSalidasPanel.add(btnEntradasSalidas);
        totalsWithPay.add(btnEntradasSalidasPanel, java.awt.BorderLayout.SOUTH);

        // Sebastian - Panel original del botón comentado porque ya está arriba
        /*
         * // Panel para contener el botón y alinearlo a la derecha
         * javax.swing.JPanel payPanel = new javax.swing.JPanel(new
         * java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 6));
         * payPanel.setOpaque(false);
         * 
         * // Botón Pagar (verde, destacado)
         * m_jPayNow = new javax.swing.JButton();
         * m_jPayNow.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
         * m_jPayNow.setText(AppLocal.getIntString("button.pay")); // "Pagar" desde
         * pos_messages.properties
         * m_jPayNow.setFocusPainted(false);
         * m_jPayNow.setPreferredSize(new java.awt.Dimension(140, 40));
         * m_jPayNow.setBackground(new java.awt.Color(46, 139, 87)); // SeaGreen
         * m_jPayNow.setForeground(java.awt.Color.WHITE);
         * m_jPayNow.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(34,
         * 120, 60), 1, true));
         * m_jPayNow.setOpaque(true);
         */

        // Sebastian - Comentado porque ya está definido arriba
        /*
         * // Acción: reutiliza el flujo de cierre/pago de ticket
         * m_jPayNow.addActionListener(new java.awt.event.ActionListener() {
         * public void actionPerformed(java.awt.event.ActionEvent evt) {
         * if (m_oTicket == null || m_oTicket.getLinesCount() == 0) {
         * Toolkit.getDefaultToolkit().beep();
         * return;
         * }
         * 
         * if (closeTicket(m_oTicket, m_oTicketExt)) {
         * setActiveTicket(null, null);
         * refreshTicket();
         * // Delete will create an empty ticket
         * m_ticketsbag.deleteTicket();
         * 
         * if (isAutoLogout()) {
         * if (isRestaurantMode() && isAutoLogoutRestaurant()) {
         * deactivate();
         * } else {
         * ((JRootApp) m_App).closeAppView();
         * }
         * }
         * 
         * createNewTicket();
         * }
         * }
         * });
         * 
         * payPanel.add(m_jPayNow);
         * totalsWithPay.add(payPanel, java.awt.BorderLayout.SOUTH);
         */

        m_jPanelLinesSum.add(totalsWithPay, java.awt.BorderLayout.LINE_END);

        m_jPanelLines.add(m_jPanelLinesSum, java.awt.BorderLayout.SOUTH);

        m_jPanelTicket.add(m_jPanelLines, java.awt.BorderLayout.CENTER);

        m_jContEntries.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jContEntries.setMinimumSize(new java.awt.Dimension(300, 350));
        m_jContEntries.setLayout(new java.awt.BorderLayout());

        m_jPanEntries.setPreferredSize(new java.awt.Dimension(300, 350));
        m_jPanEntries.setLayout(new javax.swing.BoxLayout(m_jPanEntries, javax.swing.BoxLayout.Y_AXIS));

        m_jNumberKeys.setMaximumSize(new java.awt.Dimension(300, 300));
        m_jNumberKeys.setMinimumSize(new java.awt.Dimension(250, 250));
        m_jNumberKeys.setPreferredSize(new java.awt.Dimension(250, 250));
        m_jNumberKeys.addJNumberEventListener(new com.openbravo.beans.JNumberEventListener() {
            public void keyPerformed(com.openbravo.beans.JNumberEvent evt) {
                m_jNumberKeysKeyPerformed(evt);
            }
        });
        m_jNumberKeys.hideNumberButtons(); // Ocultar solo los números, mantener operadores

        // Sebastian - Ocultar botones CE, *, + y - del teclado numérico (manteniendo
        // funcionalidad de teclado)
        m_jNumberKeys.setCEVisible(false);
        m_jNumberKeys.setMultiplyVisible(false);
        m_jNumberKeys.setPlusVisible(false); // Ocultar botón + visual
        m_jNumberKeys.setMinusVisible(false); // Ocultar botón - visual
        // m_jNumberKeys.setEqualsVisible(false); // Sebastian - Comentado temporalmente

        // Sebastian - Ocultar completamente el panel del teclado numérico para expandir
        // el área de ventas
        m_jContEntries.setVisible(false);

        // NO agregamos el teclado numérico ya que ocultamos todo el container
        // m_jPanEntries.add(m_jNumberKeys);

        // Sebastian - TODO: Investigar de dónde viene el botón '=' azul

        jPanelScanner.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanelScanner.setMaximumSize(new java.awt.Dimension(800, 70)); // Más alto para el campo grande
        jPanelScanner.setPreferredSize(new java.awt.Dimension(800, 70));

        m_jPrice.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24)); // Fuente moderna y números grandes
        m_jPrice.setForeground(new java.awt.Color(76, 197, 237));
        m_jPrice.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jPrice.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(76, 197, 237), 2),
                javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        m_jPrice.setOpaque(true);
        m_jPrice.setPreferredSize(new java.awt.Dimension(500, 40));
        m_jPrice.setRequestFocusEnabled(false);

        m_jEnter.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/barcode.png"))); // NOI18N
        m_jEnter.setToolTipText(bundle.getString("tooltip.salebarcode")); // NOI18N
        m_jEnter.setFocusPainted(false);
        m_jEnter.setFocusable(false);
        m_jEnter.setPreferredSize(new java.awt.Dimension(80, 45));
        m_jEnter.setRequestFocusEnabled(false);
        m_jEnter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jEnterActionPerformed(evt);
            }
        });

        m_jPor.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jPor.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        m_jPor.setText("AS");
        m_jPor.setRequestFocusEnabled(false);

        m_jKeyFactory.setEditable(true);
        m_jKeyFactory.setFont(new java.awt.Font("Segoe UI", 1, 28)); // Fuente moderna y números grandes
        m_jKeyFactory.setForeground(java.awt.Color.BLACK); // Números negros
        m_jKeyFactory.setBackground(java.awt.Color.WHITE);
        m_jKeyFactory.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jKeyFactory.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(76, 197, 237), 2),
                javax.swing.BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        m_jKeyFactory.setPreferredSize(new java.awt.Dimension(500, 50)); // Campo más alto para números grandes
        m_jKeyFactory.setAutoscrolls(false);
        m_jKeyFactory.setCaretColor(new java.awt.Color(76, 197, 237));
        m_jKeyFactory.setRequestFocusEnabled(true);
        m_jKeyFactory.setVerifyInputWhenFocusTarget(false);
        m_jKeyFactory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jKeyFactoryActionPerformed(evt);
            }
        });
        m_jKeyFactory.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                m_jKeyFactoryKeyTyped(evt);
            }
        });

        m_jaddtax.setToolTipText(bundle.getString("tooltip.switchtax")); // NOI18N
        m_jaddtax.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jaddtaxActionPerformed(evt);
            }
        });

        m_jTax.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jTax.setToolTipText(bundle.getString("tooltip.salestaxswitch")); // NOI18N
        m_jTax.setFocusable(false);

        javax.swing.GroupLayout jPanelScannerLayout = new javax.swing.GroupLayout(jPanelScanner);
        jPanelScanner.setLayout(jPanelScannerLayout);
        jPanelScannerLayout.setHorizontalGroup(
                jPanelScannerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanelScannerLayout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE) // Espacio
                                                                                                        // flexible para
                                                                                                        // centrar
                                .addComponent(m_jEnter, javax.swing.GroupLayout.PREFERRED_SIZE, 64,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(m_jKeyFactory, javax.swing.GroupLayout.PREFERRED_SIZE, 500,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(m_jTax, javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(m_jaddtax)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)) // Espacio
                                                                                                         // flexible
                                                                                                         // para centrar
        );
        jPanelScannerLayout.setVerticalGroup(
                jPanelScannerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanelScannerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                .addComponent(m_jEnter, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(m_jKeyFactory, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(m_jTax, javax.swing.GroupLayout.PREFERRED_SIZE, 30,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(m_jaddtax)));

        m_jContEntries.add(m_jPanEntries, java.awt.BorderLayout.LINE_START);

        // Sebastian - Comentar la adición del panel de entradas para liberar espacio
        // m_jPanelTicket.add(m_jContEntries, java.awt.BorderLayout.LINE_END);

        // Crear panel para la barra de búsqueda en la parte superior
        javax.swing.JPanel searchPanel = new javax.swing.JPanel(new java.awt.BorderLayout());
        searchPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        searchPanel.add(jPanelScanner, java.awt.BorderLayout.CENTER);

        // Sebastian - Crear panel superior exclusivo para puntos del cliente
        javax.swing.JPanel customerPointsPanel = new javax.swing.JPanel(new java.awt.BorderLayout());
        customerPointsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        customerPointsPanel.add(m_jCustomerPoints, java.awt.BorderLayout.EAST);

        // Crear un panel contenedor para el toolbar y la búsqueda
        javax.swing.JPanel topPanel = new javax.swing.JPanel(new java.awt.BorderLayout());
        // Sebastian - Ocultar todo el toolbar principal para interfaz ultramoderna
        m_jPanelMainToolbar.setVisible(false);
        topPanel.add(m_jPanelMainToolbar, java.awt.BorderLayout.NORTH);
        topPanel.add(searchPanel, java.awt.BorderLayout.SOUTH);

        // Sebastian - Crear un panel contenedor completo que incluya los puntos arriba
        // de todo
        javax.swing.JPanel completeTopPanel = new javax.swing.JPanel(new java.awt.BorderLayout());
        completeTopPanel.add(customerPointsPanel, java.awt.BorderLayout.NORTH);
        completeTopPanel.add(topPanel, java.awt.BorderLayout.CENTER);

        m_jPanelContainer.add(completeTopPanel, java.awt.BorderLayout.NORTH);
        m_jPanelContainer.add(m_jPanelTicket, java.awt.BorderLayout.CENTER);

        m_jPanelCatalog.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        m_jPanelCatalog.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jPanelCatalog.setLayout(new java.awt.BorderLayout());
        m_jPanelContainer.add(m_jPanelCatalog, java.awt.BorderLayout.SOUTH);

        add(m_jPanelContainer, "ticket");
    }// </editor-fold>//GEN-END:initComponents

    private void m_jbtnScaleActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jbtnScaleActionPerformed

        stateTransition('\u00a7');

    }// GEN-LAST:event_m_jbtnScaleActionPerformed

    private void m_jEditLineActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jEditLineActionPerformed

        int i = m_ticketlines.getSelectedIndex();

        if (i < 0) {
            Toolkit.getDefaultToolkit().beep(); // no line selected
        } else {
            try {
                TicketLineInfo newline = JProductLineEdit.showMessage(this, m_App, m_oTicket.getLine(i));
                if (newline != null) {
                    paintTicketLine(i, newline);
                }

            } catch (BasicException e) {
                new MessageInf(e).show(this);
            }
        }

    }// GEN-LAST:event_m_jEditLineActionPerformed

    private void m_jNumberKeysKeyPerformed(com.openbravo.beans.JNumberEvent evt) {// GEN-FIRST:event_m_jNumberKeysKeyPerformed

        stateTransition(evt.getKey());

        j_btnRemotePrt.setEnabled(true);
        j_btnRemotePrt.revalidate();

    }// GEN-LAST:event_m_jNumberKeysKeyPerformed

    private void m_jDeleteActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jDeleteActionPerformed

        int i = m_ticketlines.getSelectedIndex();

        if (i < 0) {
            Toolkit.getDefaultToolkit().beep();
        } else {
            removeTicketLine(i);
            jCheckStock.setText("");
        }
    }// GEN-LAST:event_m_jDeleteActionPerformed

    private void m_jListActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jListActionPerformed

        ProductInfoExt prod = JProductFinder.showMessage(JPanelTicket.this, dlSales);
        if (prod != null && m_oTicket != null) {
            buttonTransition(prod);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }

    }// GEN-LAST:event_m_jListActionPerformed

    private void jEditAttributesActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jEditAttributesActionPerformed
        if (listener != null) {
            listener.stop();
        }
        int i = m_ticketlines.getSelectedIndex();
        // no line selected (-1)
        if (i < 0) {
            Toolkit.getDefaultToolkit().beep();
        } else {
            try {
                TicketLineInfo line = m_oTicket.getLine(i);
                JProductAttEdit2 attedit = JProductAttEdit2.getAttributesEditor(this, m_App.getSession());
                if (line.getProductAttSetId() != null) {
                    attedit.editAttributes(line.getProductAttSetId(), line.getProductAttSetInstId());
                    attedit.setVisible(true);
                    if (attedit.isOK()) {
                        line.setProductAttSetInstId(attedit.getAttributeSetInst());
                        line.setProductAttSetInstDesc(attedit.getAttributeSetInstDescription());
                        paintTicketLine(i, line);
                    }
                } else {
                    JOptionPane.showMessageDialog(this,
                            AppLocal.getIntString("message.cannotfindattributes"),
                            AppLocal.getIntString("message.title"),
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (BasicException ex) {
                LOGGER.log(System.Logger.Level.WARNING, "Exception while Open Product Atribute Editor: ", ex);
            }
        }

        if (listener != null) {
            listener.restart();
        }
    }// GEN-LAST:event_jEditAttributesActionPerformed

    private void j_btnRemotePrtActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_j_btnRemotePrtActionPerformed

        String scriptId = "script.SendOrder";
        try {
            String rScript = (dlSystem.getResourceAsText(scriptId));
            ScriptEngine scriptEngine = ScriptFactory.getScriptEngine(ScriptFactory.BEANSHELL);
            scriptEngine.put("ticket", m_oTicket);
            scriptEngine.put("place", m_oTicketExt);
            scriptEngine.put("user", m_App.getAppUserView().getUser());
            scriptEngine.put("sales", this);
            scriptEngine.put("pickupid", m_oTicket.getPickupId());

            // TODO PB_NOTE MUST BE IMPROVE HERE
            Boolean warrantyPrint = warrantyCheck(m_oTicket);
            scriptEngine.put("ticket", m_oTicket);
            scriptEngine.put("place", m_oTicketExt);
            scriptEngine.put("taxes", taxcollection);
            scriptEngine.put("taxeslogic", taxeslogic);
            scriptEngine.put("user", m_App.getAppUserView().getUser());
            scriptEngine.put("sales", this);
            scriptEngine.put("taxesinc", m_jaddtax.isSelected());
            scriptEngine.put("warranty", warrantyPrint);
            scriptEngine.put("pickupid", getPickupString(m_oTicket));

            scriptEngine.eval(rScript);

        } catch (ScriptException ex) {
            LOGGER.log(System.Logger.Level.WARNING, "Exception on executing script: " + scriptId, ex);
        }

        remoteOrderDisplay();

    }// GEN-LAST:event_j_btnRemotePrtActionPerformed

    private void btnReprint1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnReprint1ActionPerformed

        // TODO GET LAST FROM DB (USER ID)
        /*
         * if (m_config.getProperty("lastticket.number") != null) {
         * try {
         * TicketInfo ticketInfo = dlSales.loadTicket(
         * Integer.parseInt((m_config.getProperty("lastticket.type"))),
         * Integer.parseInt((m_config.getProperty("lastticket.number"))));
         * if (ticketInfo == null) {
         * JFrame frame = new JFrame();
         * JOptionPane.showMessageDialog(frame,
         * AppLocal.getIntString("message.notexiststicket"),
         * AppLocal.getIntString("message.notexiststickettitle"),
         * JOptionPane.WARNING_MESSAGE);
         * } else {
         * try {
         * taxeslogic.calculateTaxes(ticketInfo);
         * //TicketTaxInfo[] taxlist = m_ticket.getTaxLines();
         * } catch (TaxesException ex) {
         * LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
         * }
         * printTicket("Printer.ReprintTicket", ticketInfo, null);
         * Notify("'Printed'");
         * }
         * } catch (BasicException ex) {
         * LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
         * MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
         * AppLocal.getIntString("message.cannotloadticket"), ex);
         * msg.show(this);
         * }
         * }
         */
    }// GEN-LAST:event_btnReprint1ActionPerformed

    private void btnSplitActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnSplitActionPerformed

        if (m_oTicket.getLinesCount() > 0) {
            ReceiptSplit splitdialog = ReceiptSplit.getDialog(this,
                    dlSystem.getResourceAsXML(TicketConstants.RES_TICKET_LINES), dlSales, dlCustomers, taxeslogic);

            TicketInfo ticket1 = m_oTicket.copyTicket();
            TicketInfo ticket2 = new TicketInfo();
            ticket2.setCustomer(m_oTicket.getCustomer());

            if (splitdialog.showDialog(ticket1, ticket2, m_oTicketExt)) {
                if (closeTicket(ticket2, m_oTicketExt)) { // already checked that number of lines > 0
                    setActiveTicket(ticket1, m_oTicketExt);// set result ticket
                }
            }
        }

    }// GEN-LAST:event_btnSplitActionPerformed

    private void jCheckStockActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jCheckStockActionPerformed

        checkStock();
    }// GEN-LAST:event_jCheckStockActionPerformed

    private void jCheckStockMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jCheckStockMouseClicked
        if (evt.getClickCount() == 2) {
            if (listener != null) {
                listener.stop();
            }

            int i = m_ticketlines.getSelectedIndex();
            if (i < 0) {
                Toolkit.getDefaultToolkit().beep();
            } else {
                try {
                    TicketLineInfo line = m_oTicket.getLine(i);
                    String pId = line.getProductID();
                    String location = m_App.getInventoryLocation();
                    ProductStock checkProduct = dlSales.getProductStockState(pId, location);

                    Double pMin;
                    Double pMax;
                    Double pUnits;
                    Date pMemoDate;
                    String content;

                    if (!location.equals(checkProduct.getLocation())) {
                        content = AppLocal.getIntString("message.location.current");
                    } else {
                        if (checkProduct.getMinimum() != null) {
                            pMin = checkProduct.getMinimum();
                        } else {
                            pMin = 0.;
                        }
                        if (checkProduct.getMaximum() != null) {
                            pMax = checkProduct.getMaximum();
                        } else {
                            pMax = 0.;
                        }
                        if (checkProduct.getUnits() != null) {
                            pUnits = checkProduct.getUnits();
                        } else {
                            pUnits = 0.;
                        }
                        if (checkProduct.getMemoDate() != null) {
                            pMemoDate = checkProduct.getMemoDate();
                        } else {
                            pMemoDate = null;
                        }

                        content = "<html>"
                                + "<b>" + AppLocal.getIntString("label.currentstock")
                                + " : " + "</b>" + pUnits + "<br>"
                                + "<b>" + AppLocal.getIntString("label.maximum")
                                + " : " + "</b>" + pMax + "<br>"
                                + "<b>" + AppLocal.getIntString("label.minimum")
                                + " : " + "</b>" + pMin + "<br>"
                                + "<b>" + AppLocal.getIntString("label.proddate")
                                + " : " + "</b>" + pMemoDate + "<br>";
                    }

                    JFrame frame = new JFrame();
                    JOptionPane.showMessageDialog(frame,
                            content,
                            "Info",
                            JOptionPane.INFORMATION_MESSAGE);

                } catch (BasicException ex) {
                    LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
                }
            }

            if (listener != null) {
                listener.restart();
            }
        }
    }// GEN-LAST:event_jCheckStockMouseClicked

    private void jTBtnShowActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jTBtnShowActionPerformed
        if (jTBtnShow.isSelected()) {
            m_jPanelScripts.setVisible(true);
            m_jPanelBagExt.setVisible(true);
        } else {
            m_jPanelScripts.setVisible(false);
            m_jPanelBagExt.setVisible(false);
        }
        refreshTicket();
        m_jKeyFactory.requestFocus();
    }// GEN-LAST:event_jTBtnShowActionPerformed

    private void jBtnCustomerActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jBtnCustomerActionPerformed
        if (listener != null) {
            listener.stop();
        }
        Object[] options = {
                AppLocal.getIntString("cboption.create"),
                AppLocal.getIntString("cboption.find"),
                AppLocal.getIntString("label.cancel") };

        int n = JOptionPane.showOptionDialog(this,
                AppLocal.getIntString("message.customeradd"),
                AppLocal.getIntString("label.customer"),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[2]);

        if (n == 0) {
            JDialogNewCustomer dialog = JDialogNewCustomer.getDialog(this, m_App);
            dialog.setVisible(true);

            CustomerInfoExt m_customerInfo = dialog.getSelectedCustomer();
            if (m_customerInfo != null) {
                try {
                    m_oTicket.setCustomer(m_customerInfo);
                } catch (Exception ex) {
                    LOGGER.log(System.Logger.Level.WARNING, "Exception on Select Customer: ", ex);
                }
            }
        }

        if (n == 1) {
            JCustomerFinder finder = JCustomerFinder.getCustomerFinder(this, dlCustomers);

            if (m_oTicket.getCustomerId() == null) {
                finder.setAppView(m_App);
                finder.search(m_oTicket.getCustomer());
                finder.executeSearch();
                finder.setVisible(true);

                CustomerInfo customerInfo = finder.getSelectedCustomer();
                if (customerInfo != null) {

                    try {
                        CustomerInfoExt customerExt = dlSales.loadCustomerExt(customerInfo.getId());
                        m_oTicket.setCustomer(customerExt);
                        if (isRestaurantMode()) {
                            restDB.setCustomerNameInTableByTicketId(customerExt.getName(), m_oTicket.getId());
                        }

                        checkCustomer();
                        updateCustomerPointsDisplay(); // Sebastian - Actualizar display de puntos

                        m_jTicketId.setText(m_oTicket.getName(m_oTicketExt));

                    } catch (BasicException ex) {
                        LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
                        MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                                AppLocal.getIntString("message.cannotfindcustomer"), ex);
                        msg.show(this);
                    }
                } else {
                    m_oTicket.setCustomer(null);
                    updateCustomerPointsDisplay(); // Sebastian - Limpiar display de puntos
                    if (isRestaurantMode()) {
                        restDB.setCustomerNameInTableByTicketId(null, m_oTicket.getId());
                    }
                    Notify("notify.customerremove");
                }

            } else {
                if (JOptionPane.showConfirmDialog(this,
                        AppLocal.getIntString("message.customerchange"),
                        AppLocal.getIntString("title.editor"),
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

                    finder.setAppView(m_App);
                    finder.search(m_oTicket.getCustomer());
                    finder.executeSearch();
                    finder.setVisible(true);

                    if (finder.getSelectedCustomer() != null) {
                        try {
                            m_oTicket.setCustomer(dlSales.loadCustomerExt(finder.getSelectedCustomer().getId()));
                            if (isRestaurantMode()) {
                                restDB.setCustomerNameInTableByTicketId(
                                        dlSales.loadCustomerExt(finder.getSelectedCustomer().getId()).toString(),
                                        m_oTicket.getId());
                            }

                            checkCustomer();
                            updateCustomerPointsDisplay(); // Sebastian - Actualizar display de puntos

                            m_jTicketId.setText(m_oTicket.getName());

                        } catch (BasicException ex) {
                            LOGGER.log(System.Logger.Level.WARNING, "Exception on: ", ex);
                            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                                    AppLocal.getIntString("message.cannotfindcustomer"), ex);
                            msg.show(this);
                        }
                    } else {
                        restDB.setCustomerNameInTableByTicketId(null, m_oTicket.getId());
                        m_oTicket.setCustomer(null);
                    }
                }
            }
        }

        refreshTicket();

    }// GEN-LAST:event_jBtnCustomerActionPerformed

    private void m_jEnterActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jEnterActionPerformed

        stateTransition('\n');
    }// GEN-LAST:event_m_jEnterActionPerformed

    private void m_jKeyFactoryKeyTyped(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_m_jKeyFactoryKeyTyped

        // Manejar operadores + y - para incrementar/decrementar cantidad
        if (evt.getKeyChar() == '+' || evt.getKeyChar() == '-') {
            evt.consume(); // Evitar que se escriba en el campo de texto
            stateTransition(evt.getKeyChar()); // Procesar como operador
            return;
        }

        // Permitir que el campo de texto maneje normalmente la entrada
        // Solo llamamos a stateTransition para Enter
        if (evt.getKeyChar() == '\n') {
            // Al presionar Enter, usamos el texto del campo como código de búsqueda
            String searchText = m_jKeyFactory.getText();
            if (searchText != null && !searchText.trim().isEmpty()) {
                // Limpiamos m_sBarcode y agregamos el texto completo
                m_sBarcode = new StringBuffer(searchText.trim());
                stateTransition('\n'); // Procesar como Enter para buscar
            }
        }
    }// GEN-LAST:event_m_jKeyFactoryKeyTyped

    private void m_jKeyFactoryActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jKeyFactoryActionPerformed
        // Manejar la búsqueda cuando se presiona Enter
        String searchText = m_jKeyFactory.getText();
        if (searchText != null && !searchText.trim().isEmpty()) {
            // Limpiamos m_sBarcode y agregamos el texto completo
            m_sBarcode = new StringBuffer(searchText.trim());
            stateTransition('\n'); // Procesar como Enter para buscar
        }
    }// GEN-LAST:event_m_jKeyFactoryActionPerformed

    private void m_jaddtaxActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jaddtaxActionPerformed
        m_jKeyFactory.requestFocus();
    }// GEN-LAST:event_m_jaddtaxActionPerformed

    // Sebastian - Método para buscar cliente por ID
    private void m_jCustomerIdKeyReleased(java.awt.event.KeyEvent evt) {
        searchCustomerById();
    }

    /**
     * Sebastian - Busca un cliente por ID/SearchKey y actualiza el label con el
     * nombre
     */
    private void searchCustomerById() {
        String customerId = m_jCustomerId.getText().trim();

        if (customerId.isEmpty()) {
            m_jCustomerName.setText("");
            // Limpiar cliente del ticket si se borra el ID
            if (m_oTicket != null) {
                m_oTicket.setCustomer(null);
                // Sebastian - Actualizar display de puntos cuando se remueve cliente
                updateCustomerPointsDisplay();
            }
            return;
        }

        try {
            // Buscar por searchkey usando DataLogicCustomers
            CustomerInfo customer = null;

            // Buscar en todos los clientes por searchkey
            java.util.List<CustomerInfo> allCustomers = dlCustomers.getCustomerList().list();
            for (CustomerInfo c : allCustomers) {
                if (customerId.equals(c.getSearchkey())) {
                    customer = c;
                    break;
                }
            }

            if (customer != null) {
                String customerName = customer.getName() != null ? customer.getName() : "Sin nombre";
                m_jCustomerName.setText("Cliente: " + customerName);
                m_jCustomerName.setForeground(new java.awt.Color(0, 100, 0));

                // Sebastian - IMPORTANTE: Asociar el cliente al ticket actual
                if (m_oTicket != null) {
                    try {
                        // Cargar CustomerInfoExt usando el ID del cliente encontrado
                        com.openbravo.pos.customers.CustomerInfoExt customerExt = dlSales
                                .loadCustomerExt(customer.getId());
                        m_oTicket.setCustomer(customerExt);
                        LOGGER.log(System.Logger.Level.INFO,
                                "Cliente asociado al ticket: " + customerName + " (ID: " + customer.getId() + ")");

                        // Sebastian - Actualizar display de puntos cuando se asigna cliente
                        updateCustomerPointsDisplay();
                    } catch (Exception ex) {
                        LOGGER.log(System.Logger.Level.WARNING,
                                "Error al cargar CustomerInfoExt para el cliente: " + customer.getId(), ex);
                        m_oTicket.setCustomer(null);
                    }
                }
            } else {
                m_jCustomerName.setText("Cliente no encontrado");
                m_jCustomerName.setForeground(new java.awt.Color(200, 0, 0));
                // Limpiar cliente del ticket si no se encuentra
                if (m_oTicket != null) {
                    m_oTicket.setCustomer(null);
                    // Sebastian - Actualizar display de puntos cuando no se encuentra cliente
                    updateCustomerPointsDisplay();
                }
            }
        } catch (Exception e) {
            m_jCustomerName.setText("Error al buscar cliente");
            m_jCustomerName.setForeground(new java.awt.Color(200, 0, 0));
            LOGGER.log(System.Logger.Level.WARNING, "Error searching customer by searchkey: " + customerId, e);
            // Limpiar cliente del ticket en caso de error
            if (m_oTicket != null) {
                m_oTicket.setCustomer(null);
                // Sebastian - Actualizar display de puntos en caso de error
                updateCustomerPointsDisplay();
            }
        }
    }

    // Sebastian - Método para actualizar los campos de cliente en la UI
    private void updateCustomerFields() {
        if (m_oTicket != null && m_oTicket.getCustomer() != null) {
            CustomerInfo customer = m_oTicket.getCustomer();
            String searchkey = customer.getSearchkey() != null ? customer.getSearchkey() : "";
            String customerName = customer.getName() != null ? customer.getName() : "Sin nombre";

            m_jCustomerId.setText(searchkey);
            m_jCustomerName.setText("Cliente: " + customerName);
            m_jCustomerName.setForeground(new java.awt.Color(0, 100, 0));

            // Sebastian - Actualizar información de puntos
            updateCustomerPointsDisplay();

            LOGGER.log(System.Logger.Level.INFO,
                    "Mostrando cliente del ticket: " + customerName + " (ID: " + customer.getId() + ")");
        } else {
            m_jCustomerId.setText("");
            m_jCustomerName.setText("");
            // Sebastian - Limpiar puntos cuando no hay cliente
            updateCustomerPointsDisplay();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnReprint1;
    private javax.swing.JButton btnSplit;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JButton jBtnCustomer;
    private javax.swing.JButton jCheckStock;
    private javax.swing.JButton jEditAttributes;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanelScanner;
    private javax.swing.JToggleButton jTBtnShow;
    private javax.swing.JButton j_btnRemotePrt;
    private javax.swing.JPanel m_jButtons;
    private javax.swing.JPanel m_jContEntries;
    private javax.swing.JButton m_jDelete;
    private javax.swing.JButton m_jEditLine;
    private javax.swing.JButton m_jEnter;
    private javax.swing.JTextField m_jKeyFactory;
    private javax.swing.JLabel m_jLblSubTotalEuros;
    private javax.swing.JLabel m_jLblTaxEuros;
    private javax.swing.JLabel m_jLblTotalEuros;
    private javax.swing.JButton m_jList;
    private com.openbravo.beans.JNumberKeys m_jNumberKeys;
    private javax.swing.JPanel m_jPanEntries;
    private javax.swing.JPanel m_jPanelBag;
    private javax.swing.JPanel m_jPanelBagExt;
    private javax.swing.JPanel m_jPanelBagExtDefaultEmpty;
    private javax.swing.JPanel m_jPanelCatalog;
    private javax.swing.JPanel m_jPanelContainer;
    private javax.swing.JPanel m_jPanelLines;
    private javax.swing.JPanel m_jPanelLinesSum;
    private javax.swing.JPanel m_jPanelLinesToolbar;
    private javax.swing.JPanel m_jPanelMainToolbar;
    private javax.swing.JPanel m_jPanelScripts;
    private javax.swing.JPanel m_jPanelTicket;
    private javax.swing.JPanel m_jPanelTotals;
    private javax.swing.JLabel m_jPor;
    private javax.swing.JLabel m_jPrice;
    private javax.swing.JLabel m_jSubtotalEuros;
    private javax.swing.JComboBox m_jTax;
    private javax.swing.JLabel m_jTaxesEuros;
    private javax.swing.JLabel m_jTicketId;
    private javax.swing.JLabel m_jTotalEuros;
    private javax.swing.JCheckBox m_jaddtax;
    private javax.swing.JButton m_jbtnScale;
    private javax.swing.JButton m_jPayNow; // Botón Pagar añadido

    // Sebastian - Campos para gestión de clientes
    private javax.swing.JTextField m_jCustomerId;
    private javax.swing.JLabel m_jCustomerName;
    private javax.swing.JLabel m_jLblCustomerId;
    private javax.swing.JLabel m_jCustomerPoints; // Label para mostrar puntos del cliente
    // End of variables declaration//GEN-END:variables

    /**
     * Internal Class utils methods, MUST never open to publics
     */

    /* Application Property */
    private String getAppProperty(String propertyName) {
        return m_App.getProperties().getProperty(propertyName);
    }

    /* Remote Orders Display - Utils methods */
    public void remoteOrderDisplay() {
        getRemoteOrderDisplay().remoteOrderDisplay(1, true);
    }

    /* Remote Orders Display - Utils methods */
    public void remoteOrderDisplay(String orderId) {
        remoteOrderDisplay(orderId, 1, true);
    }

    /* Remote Orders Display - Utils methods */
    public void remoteOrderDisplay(int display) {
        getRemoteOrderDisplay().remoteOrderDisplay(display, false);
    }

    /* Remote Orders Display - Utils methods */
    public String remoteOrderId() {
        return getRemoteOrderDisplay().remoteOrderId();
    }

    /* Remote Orders Display - Utils methods */
    public void remoteOrderDisplay(String orderId, int display, boolean primary) {
        getRemoteOrderDisplay().remoteOrderDisplay(orderId, display, primary);
    }

    /* Remote Orders Display - Utils methods */
    private RemoteOrderDisplay getRemoteOrderDisplay() {
        return new RemoteOrderDisplay(m_App, m_oTicket, m_oTicketExt, getPickupString(m_oTicket));
    }

    private class LogoutAction extends AbstractAction {

        public LogoutAction() {
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            closeAllDialogs();
            if (isRestaurantMode()) {
                deactivate();
                if (isAutoLogoutRestaurant()) {
                    ((JRootApp) m_App).closeAppView();
                } else {
                    setActiveTicket(null, null);
                }
            } else {
                deactivate();
                ((JRootApp) m_App).closeAppView();
            }
        }
    }

    /**
     * Script Argument
     */
    public static class ScriptArg {

        private final String key;
        private final Object value;

        /**
         *
         * @param key
         * @param value
         */
        public ScriptArg(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        /**
         *
         * @return
         */
        public String getKey() {
            return key;
        }

        /**
         *
         * @return
         */
        public Object getValue() {
            return value;
        }
    }

    /**
     * Script Object
     */
    public class ScriptObject {

        private final TicketInfo ticket;
        private final String ticketext;

        private int selectedindex;

        private ScriptObject(TicketInfo ticket, String ticketext) {
            this.ticket = ticket;
            this.ticketext = ticketext;
        }

        /**
         *
         * @return
         */
        public double getInputValue() {
            if (m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO) {
                return JPanelTicket.this.getInputValue();
            } else {
                return 0.0;
            }
        }

        /**
         *
         * @return
         */
        public int getSelectedIndex() {
            return selectedindex;
        }

        /**
         *
         * @param i
         */
        public void setSelectedIndex(int i) {
            selectedindex = i;
        }

        /**
         *
         * @param resourcefile
         */
        public void printReport(String resourcefile) {
            JPanelTicket.this.printReport(resourcefile, ticket, ticketext);
        }

        /**
         *
         * @param sresourcename
         */
        public void printTicket(String sresourcename) {
            JPanelTicket.this.printTicket(sresourcename, ticket, ticketext);
            j_btnRemotePrt.setEnabled(false);
        }

        public Object evalScript(String code, ScriptArg... args) throws ScriptException {

            ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.BEANSHELL);

            for (ScriptArg arg : args) {
                script.put(arg.getKey(), arg.getValue());
            }

            return script.eval(code);
        }
    }

    /**
     * Sebastian - Procesa automáticamente los puntos después de una venta exitosa
     */
    private void procesarPuntosAutomaticos(TicketInfo ticket) {
        try {
            // Verificar que hay un cliente asignado al ticket
            CustomerInfo cliente = ticket.getCustomer();
            if (cliente == null || cliente.getId() == null) {
                LOGGER.log(System.Logger.Level.DEBUG, "No hay cliente asignado al ticket, no se otorgan puntos");
                return;
            }

            // Verificar que el sistema de puntos está activo
            if (puntosDataLogic == null) {
                LOGGER.log(System.Logger.Level.WARNING, "Sistema de puntos no inicializado");
                return;
            }

            // Obtener configuración activa del sistema de puntos
            PuntosConfiguracion config = puntosDataLogic.getConfiguracionActiva();
            if (config == null || !config.isSistemaActivo()) {
                LOGGER.log(System.Logger.Level.DEBUG, "Sistema de puntos desactivado");
                return;
            }

            // Calcular total solo de productos que acumulan puntos
            double totalAcumulable = 0.0;
            for (TicketLineInfo line : ticket.getLines()) {
                if (line.isProductAccumulatesPoints()) {
                    totalAcumulable += line.getValue();
                }
            }

            if (totalAcumulable <= 0) {
                LOGGER.log(System.Logger.Level.DEBUG, "Total acumulable <= 0, no se otorgan puntos");
                return;
            }

            // Calcular puntos según la configuración sobre el monto acumulable
            int puntosAOtorgar = config.calcularPuntos(totalAcumulable);

            // Sebastian - Debug adicional para comparar
            System.out.println("🛒 PROCESAMIENTO REAL - Total ticket: $" + ticket.getTotal());
            System.out.println("🛒 PROCESAMIENTO REAL - Total acumulable: $" + totalAcumulable);
            System.out.println("🛒 PROCESAMIENTO REAL - Monto por punto: $" + config.getMontoPorPunto());
            System.out.println("🛒 PROCESAMIENTO REAL - Puntos otorgados: " + config.getPuntosOtorgados());

            // Mostrar lógica de tramos
            int tramosCompletos = (int) Math.floor(totalAcumulable / config.getMontoPorPunto());
            System.out.println("🛒 PROCESAMIENTO REAL - Tramos completos: " + tramosCompletos);
            System.out.println("🛒 PROCESAMIENTO REAL - Cálculo: " + tramosCompletos + " × "
                    + config.getPuntosOtorgados() + " = " + puntosAOtorgar + " puntos");

            if (puntosAOtorgar <= 0) {
                LOGGER.log(System.Logger.Level.DEBUG,
                        String.format("No se otorgan puntos: total acumulable=%.2f, configuración=%.2f %s = %d puntos",
                                totalAcumulable, config.getMontoPorPunto(), config.getMoneda(),
                                config.getPuntosOtorgados()));
                return;
            }

            // Crear descripción de la transacción
            String descripcion = String.format("Venta automática #%d - Total acumulable: $%.2f %s",
                    ticket.getTicketId(),
                    totalAcumulable,
                    config.getMoneda());

            // Otorgar puntos al cliente
            String clienteId = cliente.getId();
            puntosDataLogic.agregarPuntosPorCompra(clienteId, totalAcumulable, descripcion);

            // Log exitoso
            LOGGER.log(System.Logger.Level.INFO,
                    String.format(
                            "✅ PUNTOS OTORGADOS AUTOMÁTICAMENTE: Cliente=%s, Total acumulable=$%.2f, Puntos=%d, Desc='%s'",
                            clienteId, totalAcumulable, puntosAOtorgar, descripcion));

            // Mostrar notificación opcional al usuario (comentada por defecto)
            /*
             * JOptionPane.showMessageDialog(this,
             * String.format("🎉 ¡Puntos otorgados!\nCliente: %s\nPuntos: %d",
             * cliente.getName() != null ? cliente.getName() : clienteId,
             * puntosAOtorgar),
             * "Sistema de Puntos",
             * JOptionPane.INFORMATION_MESSAGE);
             */

        } catch (BasicException ex) {
            // Si es un error de tabla no encontrada, intentar crear las tablas
            if (ex.getMessage() != null && ex.getMessage().contains("objeto no encontrado")) {
                LOGGER.log(System.Logger.Level.WARNING, "Tablas de puntos no encontradas, intentando crearlas...");
                try {
                    puntosDataLogic.verificarSistemaPuntos(); // Recrear tablas
                    LOGGER.log(System.Logger.Level.INFO, "Tablas de puntos creadas, reintentando operación...");

                    // Reintentar la operación una vez
                    PuntosConfiguracion config = puntosDataLogic.getConfiguracionActiva();
                    if (config != null && config.isSistemaActivo()) {
                        CustomerInfo cliente = ticket.getCustomer();

                        // Calcular total acumulable
                        double totalAcumulable = 0.0;
                        for (TicketLineInfo line : ticket.getLines()) {
                            if (line.isProductAccumulatesPoints()) {
                                totalAcumulable += line.getValue();
                            }
                        }

                        int puntosAOtorgar = config.calcularPuntos(totalAcumulable);

                        if (puntosAOtorgar > 0) {
                            String descripcion = String.format("Venta automática #%d - Total: $%.2f %s",
                                    ticket.getTicketId(),
                                    totalAcumulable,
                                    config.getMoneda());

                            puntosDataLogic.agregarPuntosPorCompra(cliente.getId(), totalAcumulable, descripcion);
                            LOGGER.log(System.Logger.Level.INFO,
                                    String.format(
                                            "✅ PUNTOS OTORGADOS (después de crear tablas): Cliente=%s, Total acumulable=$%.2f, Puntos=%d",
                                            cliente.getId(), totalAcumulable, puntosAOtorgar));
                        }
                    }
                } catch (Exception retryEx) {
                    LOGGER.log(System.Logger.Level.ERROR,
                            "Error al reintentar después de crear tablas: " + retryEx.getMessage(), retryEx);
                }
            } else {
                LOGGER.log(System.Logger.Level.ERROR, "Error procesando puntos automáticos: " + ex.getMessage(), ex);
            }
        } catch (Exception ex) {
            LOGGER.log(System.Logger.Level.ERROR, "Error inesperado procesando puntos automáticos: " + ex.getMessage(),
                    ex);
        }
    }

    /**
     * JPnaleTicket constant defined in a single place
     */
    private static class TicketConstants {

        /**
         * Ticket Events(event key :string): Ticket event 'show'
         */
        public static final String EV_TICKET_SHOW = "ticket.show";

        /**
         * Ticket Events (eventKey :string): Ticket event 'change' Event:
         * 'ticket.change' (Ticket changed)
         */
        public static final String EV_TICKET_CHANGE = "ticket.change";

        /**
         * Ticket Events (eventKey :string): Ticket event 'close' Event:
         * 'ticket.close' (Ticket closed)
         */
        public static final String EV_TICKET_CLOSE = "ticket.close";

        /**
         * Ticket Events (eventKey :string): Ticket event 'save' Event:
         * 'ticket.save' (Ticket saved)
         */
        public static final String EV_TICKET_SAVE = "ticket.save";

        /**
         * Ticket Events (eventKey :string): Ticket event 'total' Event:
         * 'ticket.total' (Ticket total)
         */
        public static final String EV_TICKET_TOTAL = "ticket.total";

        /**
         * Ticket Property (property :boolean['true'|'false'): Ticket property
         * 'updated' Property: 'ticket.updated' (TicketLine was updated)
         */
        public static final String PROP_TICKET_UPDATED = "ticket.updated";

        /**
         * Ticket Resource (resource: XML): Ticket resource Resource:
         * 'Ticket.Buttons' (Define which buttons to show in Top Menu)
         */
        public static final String RES_TICKET_BUTTONS = "Ticket.Buttons";

        /**
         * Ticket Resource (resource: XML): Ticket resource: TicketLine Panel
         * configuration Resource: 'Ticket.Line' (Define which TicketLine
         * attribute to show in TicketLinePanel)
         */
        public static final String RES_TICKET_LINES = "Ticket.Line";

    }

    /**
     * Sebastian - Lista simple para almacenar ventas múltiples
     */
    private static java.util.List<TicketInfo> ventasActivas = new java.util.ArrayList<>();
    private static int ventaActualIndex = 0;

    /**
     * Sebastian - Método para abrir nueva venta (nueva pestaña)
     */
    private void abrirNuevaVenta() {
        try {
            // Guardar la venta actual si tiene contenido
            if (m_oTicket != null && m_oTicket.getLinesCount() > 0) {
                // Agregar a la lista si no está ya
                boolean yaExiste = false;
                for (int i = 0; i < ventasActivas.size(); i++) {
                    if (ventasActivas.get(i) == m_oTicket) {
                        yaExiste = true;
                        ventaActualIndex = i;
                        break;
                    }
                }
                if (!yaExiste) {
                    ventasActivas.add(m_oTicket);
                    ventaActualIndex = ventasActivas.size() - 1;
                }
            }

            // Crear un nuevo ticket vacío
            TicketInfo nuevoTicket = new TicketInfo();
            ventasActivas.add(nuevoTicket);
            ventaActualIndex = ventasActivas.size() - 1;

            // Establecer el nuevo ticket como activo
            setActiveTicket(nuevoTicket, null);

            // Establecer foco en el campo de búsqueda después de crear nueva venta
            setSearchFieldFocus();

            // Sebastian - Sin mensaje para mayor velocidad

        } catch (Exception e) {
            System.err.println("Error al crear nueva venta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sebastian - Método para cambiar entre ventas existentes
     */
    private void cambiarEntreVentas() {
        try {
            // Mostrar/ocultar la lista de tickets para cambiar entre ventas
            if (m_ticketsbag != null) {
                javax.swing.JComponent bagComponent = m_ticketsbag.getBagComponent();
                boolean wasVisible = bagComponent.isVisible();

                // Cambiar visibilidad
                bagComponent.setVisible(!wasVisible);
                bagComponent.revalidate();
                bagComponent.repaint();

                if (!wasVisible) {
                    // Si se está mostrando, dar instrucciones
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        javax.swing.JLabel mensaje = new javax.swing.JLabel("<html><div style='text-align: center;'>" +
                                "<b>� Lista de Ventas Activas</b><br/>" +
                                "✓ Haz clic en cualquier venta para cambiar a ella<br/>" +
                                "✓ Haz clic de nuevo en el botón naranja para ocultar<br/>" +
                                "✓ Cada venta mantiene sus productos y estado" +
                                "</div></html>");
                        mensaje.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));

                        javax.swing.JOptionPane optionPane = new javax.swing.JOptionPane(mensaje,
                                javax.swing.JOptionPane.INFORMATION_MESSAGE);
                        javax.swing.JDialog dialog = optionPane.createDialog(this, "Cambiar entre Ventas");

                        // Auto-cerrar el diálogo después de 3 segundos
                        javax.swing.Timer timer = new javax.swing.Timer(3000, e -> dialog.dispose());
                        timer.setRepeats(false);
                        timer.start();

                        dialog.setVisible(true);
                    });
                } else {
                    // Si se está ocultando, confirmar
                    System.out.println("Lista de ventas ocultada");
                }
            }

        } catch (Exception e) {
            System.err.println("Error al cambiar entre ventas: " + e.getMessage());
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al cambiar entre ventas: " + e.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Sebastian - Método mejorado para mostrar ventas pendientes
     */
    private void mostrarVentasPendientes() {
        try {
            if (ventasActivas.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "No hay ventas pendientes.\nUsa el botón verde para crear una nueva venta.",
                        "Sin Ventas Pendientes", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Crear lista de opciones
            String[] opciones = new String[ventasActivas.size()];
            for (int i = 0; i < ventasActivas.size(); i++) {
                TicketInfo ticket = ventasActivas.get(i);
                String estado = (i == ventaActualIndex) ? " ← ACTUAL" : "";
                opciones[i] = "Venta #" + (i + 1) + " (" + ticket.getLinesCount() + " productos)" + estado;
            }

            // Mostrar diálogo de selección
            String seleccion = (String) javax.swing.JOptionPane.showInputDialog(
                    this,
                    "Selecciona la venta a la que quieres cambiar:",
                    "Cambiar entre Ventas",
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    opciones,
                    opciones[ventaActualIndex]);

            if (seleccion != null) {
                // Encontrar el índice seleccionado
                for (int i = 0; i < opciones.length; i++) {
                    if (opciones[i].equals(seleccion)) {
                        if (i != ventaActualIndex) {
                            ventaActualIndex = i;
                            setActiveTicket(ventasActivas.get(i), null);
                            javax.swing.JOptionPane.showMessageDialog(this,
                                    "✅ Cambiado a Venta #" + (i + 1),
                                    "Venta Cambiada", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                        }
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error al mostrar ventas pendientes: " + e.getMessage());
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error: " + e.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Sebastian - Método para mostrar modal de ID cliente
     */
    private void mostrarModalIdCliente() {
        try {
            String idCliente = javax.swing.JOptionPane.showInputDialog(
                    this,
                    "Ingresa el ID del cliente:",
                    "ID Cliente",
                    javax.swing.JOptionPane.QUESTION_MESSAGE);

            if (idCliente != null && !idCliente.trim().isEmpty()) {
                // Usar el método existente para procesar el ID del cliente
                m_jCustomerId.setText(idCliente.trim());

                // Buscar el cliente usando la lógica existente
                String customerId = idCliente.trim();
                CustomerInfo customer = null;

                // Buscar en todos los clientes por searchkey
                java.util.List<CustomerInfo> allCustomers = dlCustomers.getCustomerList().list();
                for (CustomerInfo c : allCustomers) {
                    if (customerId.equals(c.getSearchkey())) {
                        customer = c;
                        break;
                    }
                }

                if (customer != null) {
                    // Cliente encontrado - ejecutar la lógica completa sin mostrar mensaje
                    searchCustomerById();
                } else {
                    // Cliente no encontrado
                    searchCustomerById(); // Esto actualizará el label con "Cliente no encontrado"

                    javax.swing.JOptionPane.showMessageDialog(this,
                            "❌ Cliente no encontrado\n\nEl ID '" + customerId
                                    + "' no existe en la base de datos.\nVerifica el ID e inténtalo nuevamente.",
                            "Cliente No Encontrado",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                }
            }

        } catch (Exception e) {
            System.err.println("Error al configurar ID cliente: " + e.getMessage());
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al buscar cliente: " + e.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Muestra un diálogo para registrar entradas y salidas de efectivo
     */
    private void showEntradasSalidasDialog() {
        javax.swing.JDialog dialog = new javax.swing.JDialog(
                (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this), "Entradas y Salidas", true);
        dialog.setSize(350, 250);
        dialog.setLocationRelativeTo(this);

        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        gbc.anchor = java.awt.GridBagConstraints.WEST;

        // Tipo (Entrada/Salida)
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new javax.swing.JLabel("Tipo:"), gbc);
        gbc.gridx = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        javax.swing.JComboBox<String> cmbTipo = new javax.swing.JComboBox<>(new String[] { "Entrada", "Salida" });
        cmbTipo.setPreferredSize(new java.awt.Dimension(200, 25));
        panel.add(cmbTipo, gbc);

        // Monto
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new javax.swing.JLabel("Monto:"), gbc);
        gbc.gridx = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        javax.swing.JTextField txtMonto = new javax.swing.JTextField();
        txtMonto.setPreferredSize(new java.awt.Dimension(200, 25));
        panel.add(txtMonto, gbc);

        // Notas
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new javax.swing.JLabel("Notas:"), gbc);
        gbc.gridx = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        javax.swing.JTextField txtNotas = new javax.swing.JTextField();
        txtNotas.setPreferredSize(new java.awt.Dimension(200, 25));
        panel.add(txtNotas, gbc);

        // Botones
        javax.swing.JPanel btnPanel = new javax.swing.JPanel(new java.awt.FlowLayout());
        javax.swing.JButton btnAceptar = new javax.swing.JButton("Aceptar");
        javax.swing.JButton btnCancelar = new javax.swing.JButton("Cancelar");
        btnPanel.add(btnAceptar);
        btnPanel.add(btnCancelar);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.anchor = java.awt.GridBagConstraints.CENTER;
        panel.add(btnPanel, gbc);

        dialog.add(panel);

        btnCancelar.addActionListener(e -> dialog.dispose());
        btnAceptar.addActionListener(e -> {
            try {
                String tipo = (String) cmbTipo.getSelectedItem();
                String montoStr = txtMonto.getText().trim();
                String notas = txtNotas.getText().trim();

                if (montoStr.isEmpty()) {
                    javax.swing.JOptionPane.showMessageDialog(dialog,
                            "Por favor ingrese un monto",
                            "Error",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                    return;
                }

                double monto = Double.parseDouble(montoStr);
                if (monto <= 0) {
                    javax.swing.JOptionPane.showMessageDialog(dialog,
                            "El monto debe ser mayor a cero",
                            "Error",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Crear el registro de pago
                // Usar "cashin" y "cashout" como tipos de pago, igual que PaymentsEditor
                String reason = "Entrada".equals(tipo) ? "cashin" : "cashout";
                double total = "Entrada".equals(tipo) ? monto : -monto;

                Object[] payment = new Object[7];
                payment[0] = java.util.UUID.randomUUID().toString(); // ID del receipt
                payment[1] = m_App.getActiveCashIndex(); // MONEY (caja activa)
                payment[2] = new java.util.Date(); // DATENEW
                payment[3] = java.util.UUID.randomUUID().toString(); // ID del payment
                payment[4] = reason; // PAYMENT: "cashin" o "cashout"
                payment[5] = total; // TOTAL (positivo para entrada, negativo para salida)
                payment[6] = notas.isEmpty() ? "" : notas; // NOTES

                // Guardar en la base de datos
                dlSales.getPaymentMovementInsert().exec(payment);

                // Log para depuración
                LOGGER.log(System.Logger.Level.INFO,
                        "Entrada/Salida guardada: Tipo=" + reason + ", Monto=" + total +
                                ", MONEY=" + payment[1] + ", ReceiptID=" + payment[0]);

                javax.swing.JOptionPane.showMessageDialog(dialog,
                        tipo + " de $" + String.format("%.2f", monto) + " registrada correctamente.\n" +
                                "Nota: Cierra y reabre el panel de cierre para ver los cambios.",
                        "Éxito",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);

                dialog.dispose();
            } catch (NumberFormatException ex) {
                javax.swing.JOptionPane.showMessageDialog(dialog,
                        "El monto debe ser un número válido",
                        "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            } catch (com.openbravo.basic.BasicException ex) {
                LOGGER.log(System.Logger.Level.ERROR, "Error al guardar entrada/salida: " + ex.getMessage(), ex);
                javax.swing.JOptionPane.showMessageDialog(dialog,
                        "Error al guardar: " + ex.getMessage(),
                        "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

}
