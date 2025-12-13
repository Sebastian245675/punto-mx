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
import java.io.FileWriter;
import java.io.IOException;
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
import com.openbravo.pos.sales.JDialogUnits;

import com.openbravo.pos.ticket.TicketLineInfo;
import com.openbravo.pos.util.InactivityListener;
import com.openbravo.pos.reports.JRPrinterAWT300;
import com.openbravo.pos.util.ReportUtils;
import com.openbravo.beans.JCalendarDialog;
import com.openbravo.data.loader.QBFCompareEnum;
import com.openbravo.data.user.ListProviderCreator;
import com.openbravo.data.user.EditorCreator;
import com.openbravo.pos.ticket.FindTicketsInfo;
import java.text.SimpleDateFormat;

import java.awt.*;

import static java.awt.Window.getWindows;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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

    // Sebastian - Labels de información estilo Eleventa
    private javax.swing.JLabel lblTotalValue;
    private javax.swing.JLabel lblPagoConValue;
    private javax.swing.JLabel lblCambioValue;
    protected TicketsEditor m_panelticket;
    protected TicketInfo m_oTicket;
    protected String m_oTicketExt;

    private int m_iNumberStatus;
    private int m_iNumberStatusInput;
    private int m_iNumberStatusPor;
    private StringBuffer m_sBarcode;
    // Sebastian - Flag para evitar que el diálogo de granel se abra dos veces
    private volatile boolean m_bGranelDialogOpen = false;

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
        // Configurar fondo blanco para la tabla de ventas
        m_ticketlines.setBackground(java.awt.Color.WHITE);
        // Sebastian - Eliminar cualquier espacio alrededor de la tabla
        m_ticketlines.setBorder(null);
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
        setupAdditionalShortcuts();
    }

    /**
     * Sebastian - Configura los atajos de teclado para el módulo de ventas
     * F2: Corte de caja
     * F3: Historial de pestañas
     * F4: Nueva pestaña de venta
     * F5: Cliente
     * F6: Eliminar línea
     * F7: Buscar producto
     * F8: Editar línea
     * F9: Atributos
     * F10: Dividir ticket
     * F11: Reimprimir último ticket
     * F12: Cobrar/Pagar
     */
    private void setupKeyboardShortcuts() {
        javax.swing.InputMap inputMap = this.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap actionMap = this.getActionMap();

        // F2: Corte de caja
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0), "corteCaja");
        actionMap.put("corteCaja", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                try {
                    if (m_App.getAppUserView().getUser().hasPermission("com.openbravo.pos.panels.JPanelCloseMoney")) {
                        LOGGER.log(System.Logger.Level.DEBUG, "F2 → Corte de Caja: Abriendo panel de cierre");
                        m_App.getAppUserView().showTask("com.openbravo.pos.panels.JPanelCloseMoney");
                    } else {
                        java.awt.Toolkit.getDefaultToolkit().beep();
                        LOGGER.log(System.Logger.Level.WARNING, "F2 → Sin permiso para corte de caja");
                    }
                } catch (Exception ex) {
                    LOGGER.log(System.Logger.Level.ERROR, "F2 → Error abriendo corte de caja: " + ex.getMessage(), ex);
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

        // F5: Asignar cliente
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0), "asignarCliente");
        actionMap.put("asignarCliente", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                LOGGER.log(System.Logger.Level.DEBUG, "F5 → Asignar Cliente: Abriendo modal de asignar cliente");
                mostrarModalIdCliente();
            }
        });

        // F6: Eliminar línea
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F6, 0), "eliminarLinea");
        actionMap.put("eliminarLinea", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (m_jDelete != null && m_jDelete.isEnabled() && m_oTicket != null && m_oTicket.getLinesCount() > 0) {
                    LOGGER.log(System.Logger.Level.DEBUG, "F6 → Eliminar Línea");
                    m_jDelete.doClick();
                } else {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                }
            }
        });

        // F7: Entradas
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F7, 0), "entradas");
        actionMap.put("entradas", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                LOGGER.log(System.Logger.Level.DEBUG, "F7 → Entradas: Abriendo diálogo de entradas");
                showEntradasDialog();
            }
        });

        // F8: Salidas
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F8, 0), "salidas");
        actionMap.put("salidas", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                LOGGER.log(System.Logger.Level.DEBUG, "F8 → Salidas: Abriendo diálogo de salidas");
                showSalidasDialog();
            }
        });

        // F9: Atributos
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F9, 0), "atributos");
        actionMap.put("atributos", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (jEditAttributes != null && jEditAttributes.isEnabled() && m_oTicket != null
                        && m_oTicket.getLinesCount() > 0) {
                    LOGGER.log(System.Logger.Level.DEBUG, "F9 → Atributos");
                    jEditAttributes.doClick();
                } else {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                }
            }
        });

        // F10: Dividir ticket
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F10, 0), "dividirTicket");
        actionMap.put("dividirTicket", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (btnSplit != null && btnSplit.isEnabled()) {
                    LOGGER.log(System.Logger.Level.DEBUG, "F10 → Dividir Ticket");
                    btnSplit.doClick();
                } else {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                }
            }
        });

        // F11: Mayoreo
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F11, 0), "mayoreo");
        actionMap.put("mayoreo", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                LOGGER.log(System.Logger.Level.DEBUG, "F11 → Mayoreo: Aplicando descuento de mayoreo");
                aplicarDescuentoMayoreo();
            }
        });

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

        // Agregar atajos para botones de m_jbtnconfig (descuento, imprimir, etc.)
        setupConfigButtonsShortcuts(inputMap, actionMap);

        LOGGER.log(System.Logger.Level.INFO,
                "✅ Atajos de teclado configurados: F2=Corte, F3=Historial, F4=Nueva, F5=Asignar Cliente, F6=Eliminar, F7=Entradas, F8=Salidas, F9=Atributos, F10=Dividir, F11=Mayoreo, F12=Cobrar");

        // Sebastian - Inicializar barra de pestañas después de que todos los
        // componentes estén listos
        javax.swing.SwingUtilities.invokeLater(() -> {
            initializeTabsBar();
            updateButtonTextsWithShortcuts();
        });
    }

    // Ctrl+Shift+U: Editar Unidades
    private void setupAdditionalShortcuts(javax.swing.InputMap inputMap, javax.swing.ActionMap actionMap) {
        inputMap.put(
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U,
                        java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK),
                "editarUnidades");
        actionMap.put("editarUnidades", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                editLineUnits();
            }
        });
    }

    // Ctrl+Shift+U: Editar Unidades
    private void setupAdditionalShortcuts() {
        javax.swing.InputMap inputMap = this.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap actionMap = this.getActionMap();

        inputMap.put(
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U,
                        java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK),
                "editarUnidades");
        actionMap.put("editarUnidades", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                editLineUnits();
            }
        });
    }

    /**
     * Configura atajos para los botones de configuración (descuento, imprimir,
     * etc.)
     */
    private void setupConfigButtonsShortcuts(javax.swing.InputMap inputMap, javax.swing.ActionMap actionMap) {
        if (m_jbtnconfig == null)
            return;

        // Buscar botones en m_jbtnconfig y asignarles atajos
        java.awt.Component[] components = m_jbtnconfig.getComponents();
        int keyCode = java.awt.event.KeyEvent.VK_1; // Empezar con números

        for (java.awt.Component comp : components) {
            if (comp instanceof javax.swing.JButton) {
                javax.swing.JButton btn = (javax.swing.JButton) comp;
                String btnKey = btn.getName();

                if (btnKey != null && !btnKey.isEmpty()) {
                    // Asignar atajos según el tipo de botón
                    if ("button.totaldiscount".equals(btnKey)) {
                        // Ctrl+D para descuento
                        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D,
                                java.awt.event.InputEvent.CTRL_DOWN_MASK), "descuento");
                        actionMap.put("descuento", new javax.swing.AbstractAction() {
                            @Override
                            public void actionPerformed(java.awt.event.ActionEvent e) {
                                if (btn.isEnabled()) {
                                    btn.doClick();
                                }
                            }
                        });
                        updateButtonTextWithShortcut(btn, "Ctrl+D");
                    } else if ("button.print".equals(btnKey)) {
                        // Ctrl+P para imprimir
                        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P,
                                java.awt.event.InputEvent.CTRL_DOWN_MASK), "imprimir");
                        actionMap.put("imprimir", new javax.swing.AbstractAction() {
                            @Override
                            public void actionPerformed(java.awt.event.ActionEvent e) {
                                if (btn.isEnabled()) {
                                    btn.doClick();
                                }
                            }
                        });
                        updateButtonTextWithShortcut(btn, "Ctrl+P");
                    } else if ("button.opendrawer".equals(btnKey)) {
                        // Ctrl+O para abrir cajón
                        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O,
                                java.awt.event.InputEvent.CTRL_DOWN_MASK), "abrirCajon");
                        actionMap.put("abrirCajon", new javax.swing.AbstractAction() {
                            @Override
                            public void actionPerformed(java.awt.event.ActionEvent e) {
                                if (btn.isEnabled()) {
                                    btn.doClick();
                                }
                            }
                        });
                        updateButtonTextWithShortcut(btn, "Ctrl+O");
                    }
                }
            }
        }
    }

    /**
     * Actualiza el texto de los botones para mostrar el atajo
     */
    private void updateButtonTextsWithShortcuts() {
        // Actualizar botón de cliente (F5 ahora es para asignar cliente, no este botón)
        if (jBtnCustomer != null) {
            String originalText = AppLocal.getIntString("button.customer");
            if (originalText == null || originalText.isEmpty()) {
                originalText = "Cliente";
            }
            jBtnCustomer.setText(originalText);
            // Remover F5 del tooltip si estaba
            String tooltip = jBtnCustomer.getToolTipText();
            if (tooltip != null && tooltip.contains("(F5)")) {
                jBtnCustomer.setToolTipText(tooltip.replace(" (F5)", ""));
            }
        }

        // Actualizar botón de cobrar
        if (m_jPayNow != null) {
            String originalText = AppLocal.getIntString("button.pay");
            if (originalText == null || originalText.isEmpty()) {
                originalText = "Cobrar";
            }
            m_jPayNow.setText("F12 - " + originalText);
        }

        // Actualizar botones de líneas
        if (m_jDelete != null) {
            String tooltip = m_jDelete.getToolTipText();
            if (tooltip != null && !tooltip.contains("F6")) {
                m_jDelete.setToolTipText(tooltip + " (F6)");
            }
        }

        // F7 ahora es para Entradas y Salidas, no para buscar producto
        if (m_jList != null) {
            String tooltip = m_jList.getToolTipText();
            if (tooltip != null && tooltip.contains("F7")) {
                // Remover F7 del tooltip si estaba
                m_jList.setToolTipText(tooltip.replace(" (F7)", ""));
            }
        }

        // F8 ahora es para Salidas, no para editar línea
        if (m_jEditLine != null) {
            String tooltip = m_jEditLine.getToolTipText();
            if (tooltip != null && tooltip.contains("F8")) {
                // Remover F8 del tooltip si estaba
                m_jEditLine.setToolTipText(tooltip.replace(" (F8)", ""));
            }
        }

        if (jEditAttributes != null) {
            String tooltip = jEditAttributes.getToolTipText();
            if (tooltip != null && !tooltip.contains("F9")) {
                jEditAttributes.setToolTipText(tooltip + " (F9)");
            }
        }

        if (btnSplit != null) {
            String tooltip = btnSplit.getToolTipText();
            if (tooltip != null && !tooltip.contains("F10")) {
                btnSplit.setToolTipText(tooltip + " (F10)");
            }
        }

        // F11 ahora es para Mayoreo, no para reimprimir
        if (btnReprint1 != null) {
            String tooltip = btnReprint1.getToolTipText();
            if (tooltip != null && tooltip.contains("F11")) {
                // Remover F11 del tooltip si estaba
                btnReprint1.setToolTipText(tooltip.replace(" (F11)", ""));
            }
        }
    }

    /**
     * Actualiza el texto de un botón para mostrar el atajo
     */
    private void updateButtonTextWithShortcut(javax.swing.JButton btn, String shortcut) {
        if (btn != null) {
            String currentText = btn.getText();
            if (currentText != null && !currentText.contains(shortcut)) {
                btn.setText(currentText + " (" + shortcut + ")");
            }
            String tooltip = btn.getToolTipText();
            if (tooltip != null && !tooltip.contains(shortcut)) {
                btn.setToolTipText(tooltip + " (" + shortcut + ")");
            }
        }
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

        // Botón 1 - Cliente (sin atajo específico, se usa F5 para asignar cliente)
        btnClienteCustom = new javax.swing.JButton("Cliente");
        btnClienteCustom.setPreferredSize(new java.awt.Dimension(120, 40));
        btnClienteCustom.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 11));
        btnClienteCustom.setBackground(new java.awt.Color(70, 130, 180));
        btnClienteCustom.setForeground(java.awt.Color.WHITE);
        btnClienteCustom.setFocusPainted(false);
        try {
            btnClienteCustom
                    .setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/customer.png")));
        } catch (Exception e) {
            // Ignorar si no se encuentra la imagen
        }
        btnClienteCustom.addActionListener(e -> {
            if (jBtnCustomer != null && jBtnCustomer.isEnabled()) {
                jBtnCustomer.doClick();
            }
        });

        // Botón 2 - Historial (F3)
        javax.swing.JButton btnHistorial = new javax.swing.JButton("F3 - Historial");
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
            if (m_ticketsbag != null) {
                m_ticketsbag.activate();
            }
        });

        // Botón 3 - Entradas y Salidas (F7/F8)
        btnEntradasSalidasCustom = new javax.swing.JButton(
                "<html><center>F7 Entradas<br/>F8 Salidas</center></html>");
        btnEntradasSalidasCustom.setPreferredSize(new java.awt.Dimension(120, 40));
        btnEntradasSalidasCustom.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 9));
        btnEntradasSalidasCustom.setBackground(new java.awt.Color(255, 140, 0)); // Color naranja
        btnEntradasSalidasCustom.setForeground(java.awt.Color.WHITE);
        btnEntradasSalidasCustom.setFocusPainted(false);
        try {
            // Intentar usar el icono de pagos si existe
            btnEntradasSalidasCustom
                    .setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/payments.png")));
        } catch (Exception e) {
            // Si no existe, usar un icono alternativo o sin icono
            try {
                btnEntradasSalidasCustom.setIcon(
                        new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/calculator.png")));
            } catch (Exception ex) {
                // Sin icono si no se encuentra ninguno
            }
        }
        btnEntradasSalidasCustom.addActionListener(e -> {
            // Abrir diálogo de Entradas y Salidas (sin tipo fijo para que el usuario elija)
            showEntradasSalidasDialog();
        });

        panelBotones.add(btnClienteCustom);
        panelBotones.add(btnHistorial);
        panelBotones.add(btnEntradasSalidasCustom);
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

        // Actualizar los templates Printer.Ticket y Printer.Ticket2 en la base de datos desde los archivos XML
        actualizarTemplateTicketEnBD();
        actualizarTemplateTicket2EnBD();

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

        // Sebastian - Actualizar indicador de ticket al activar
        updateTicketIndicator();

        // Aplicar fuentes grandes nuevamente por si acaso
        SwingUtilities.invokeLater(() -> {
            aplicarFuentesGrandesVentas();
        });
    }

    /**
     * Actualiza el template Printer.Ticket en la base de datos desde el archivo XML
     */
    private void actualizarTemplateTicketEnBD() {
        try {
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log", true);
                fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_ticket_update_start\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"JPanelTicket.java:843\",\"message\":\"Starting Printer.Ticket template update\",\"data\":{},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                fw.close();
                System.out.println("DEBUG: Starting Printer.Ticket template update");
            } catch (Exception ex) {
                System.out.println("DEBUG: Error logging template update start: " + ex.getMessage());
            }
            // #endregion
            
            // Leer el archivo XML desde el classpath
            java.io.InputStream is = getClass().getResourceAsStream("/com/openbravo/pos/templates/Printer.Ticket.xml");
            if (is == null) {
                LOGGER.log(System.Logger.Level.WARNING, "No se pudo encontrar el archivo Printer.Ticket.xml en el classpath");
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log", true);
                    fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_ticket_not_found\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"JPanelTicket.java:847\",\"message\":\"Printer.Ticket.xml not found in classpath\",\"data\":{},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                    fw.close();
                } catch (Exception ex) {}
                // #endregion
                return;
            }
            
            // Leer todo el contenido del archivo
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] templateContent = baos.toByteArray();
            is.close();
            baos.close();
            
            // #region agent log
            try {
                String templateStr = new String(templateContent, "UTF-8");
                boolean hasValor = templateStr.contains("Valor");
                boolean hasImporte = templateStr.contains("length=\"10\">Importe");
                boolean hasCode7 = templateStr.contains("length=\"7\">Código");
                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log", true);
                fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_ticket_content_check\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"JPanelTicket.java:863\",\"message\":\"Printer.Ticket template content check\",\"data\":{\"length\":" + templateContent.length + ",\"hasValor\":" + hasValor + ",\"hasImporte\":" + hasImporte + ",\"hasCode7\":" + hasCode7 + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                fw.close();
                System.out.println("DEBUG: Printer.Ticket template - hasValor=" + hasValor + ", hasImporte=" + hasImporte + ", hasCode7=" + hasCode7);
            } catch (Exception ex) {
                System.out.println("DEBUG: Error logging template content check: " + ex.getMessage());
            }
            // #endregion
            
            // Actualizar el template en la base de datos
            // Tipo 0 = texto/XML
            dlSystem.setResource("Printer.Ticket", 0, templateContent);
            LOGGER.log(System.Logger.Level.INFO, "Template Printer.Ticket actualizado en la base de datos");
            
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log", true);
                fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_ticket_updated\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"JPanelTicket.java:865\",\"message\":\"Printer.Ticket template updated in DB\",\"data\":{\"length\":" + templateContent.length + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                fw.close();
                System.out.println("DEBUG: Printer.Ticket template updated in DB, length=" + templateContent.length);
            } catch (Exception ex) {
                System.out.println("DEBUG: Error logging template update: " + ex.getMessage());
            }
            // #endregion
            
        } catch (java.io.IOException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error desconocido";
            LOGGER.log(System.Logger.Level.ERROR, "Error leyendo el archivo Printer.Ticket.xml: " + errorMsg);
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log", true);
                fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_ticket_io_error\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"JPanelTicket.java:868\",\"message\":\"IO Error updating Printer.Ticket\",\"data\":{\"error\":\"" + errorMsg.replace("\"", "\\\"") + "\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                fw.close();
            } catch (Exception ex) {}
            // #endregion
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error desconocido";
            LOGGER.log(System.Logger.Level.ERROR, "Error actualizando template Printer.Ticket en BD: " + errorMsg);
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log", true);
                fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_ticket_exception\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"JPanelTicket.java:872\",\"message\":\"Exception updating Printer.Ticket\",\"data\":{\"error\":\"" + errorMsg.replace("\"", "\\\"") + "\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                fw.close();
            } catch (Exception ex) {}
            // #endregion
        }
    }

    /**
     * Actualiza el template Printer.Ticket2 en la base de datos desde el archivo XML
     */
    private void actualizarTemplateTicket2EnBD() {
        try {
            // Leer el archivo XML desde el classpath
            java.io.InputStream is = getClass().getResourceAsStream("/com/openbravo/pos/templates/Printer.Ticket2.xml");
            if (is == null) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "No se pudo encontrar el archivo Printer.Ticket2.xml en el classpath");
                return;
            }

            // Leer todo el contenido del archivo
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
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
            dlSystem.setResource("Printer.Ticket2", 0, templateContent);
            LOGGER.log(System.Logger.Level.INFO, "Template Printer.Ticket2 actualizado en la base de datos");

            // #region agent log
            try {
                String templateStr = new String(templateContent, "UTF-8");
                int ticketIndex = templateStr.indexOf("<ticket>");
                int displayIndex = templateStr.indexOf("<display>");
                boolean ticketFirst = ticketIndex >= 0 && (displayIndex < 0 || ticketIndex < displayIndex);
                java.io.FileWriter fw = new java.io.FileWriter(
                        "c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log", true);
                fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_template_updated\",\"timestamp\":"
                        + System.currentTimeMillis()
                        + ",\"location\":\"JPanelTicket.java:864\",\"message\":\"Template Printer.Ticket2 updated in DB\",\"data\":{\"length\":"
                        + templateContent.length + ",\"hasTicket\":" + (ticketIndex >= 0) + ",\"hasDisplay\":"
                        + (displayIndex >= 0) + ",\"ticketFirst\":" + ticketFirst
                        + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"F\"}\n");
                fw.close();
                System.out.println("DEBUG: Template Printer.Ticket2 updated in DB, ticketFirst=" + ticketFirst);
            } catch (Exception ex) {
                System.out.println("DEBUG: Error logging template update: " + ex.getMessage());
            }
            // #endregion

        } catch (java.io.IOException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error desconocido";
            LOGGER.log(System.Logger.Level.ERROR, "Error leyendo el archivo Printer.Ticket2.xml: " + errorMsg);
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error desconocido";
            LOGGER.log(System.Logger.Level.ERROR, "Error actualizando template Printer.Ticket2 en BD: " + errorMsg);
        }
    }

    /**
     * Aplica fuentes optimizadas a los campos numéricos del panel de ventas
     * Se llama en activate() para sobrescribir las fuentes del Look and Feel Metal
     */
    private void aplicarFuentesGrandesVentas() {
        if (m_jKeyFactory != null) {
            // Configuración optimizada para códigos de barras largos
            m_jKeyFactory.setFont(new Font("Arial", Font.BOLD, 22)); // Fuente Arial Bold más grande y gruesa para
                                                                     // números más anchos
            m_jKeyFactory.setForeground(Color.BLACK);
            m_jKeyFactory.setBackground(Color.WHITE);
            m_jKeyFactory.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                    javax.swing.BorderFactory.createEmptyBorder(3, 6, 3, 6)));
            m_jKeyFactory.setMargin(new java.awt.Insets(2, 4, 2, 4));
            m_jKeyFactory.setAutoscrolls(true);
        }
        if (m_jPrice != null) {
            m_jPrice.setFont(new Font("Segoe UI", Font.BOLD, 24));
        }
        if (m_jTotalEuros != null) {
            m_jTotalEuros.setFont(new Font("Arial", Font.PLAIN, 52)); // Total estilo Eleventa - tamaño grande pero
                                                                      // delgado (PLAIN, tamaño 52)
            m_jTotalEuros.setForeground(new Color(0, 100, 200)); // Azul como en Eleventa (más claro que el anterior)
        }
        // if (m_jSubtotalEuros != null) {
        // m_jSubtotalEuros.setFont(new Font("Segoe UI", Font.PLAIN, 32)); // Ya no se
        // muestra
        // }
        // if (m_jTaxesEuros != null) {
        // m_jTaxesEuros.setFont(new Font("Segoe UI", Font.PLAIN, 32)); // Ya no se
        // muestra
        // }
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

        // Sebastian - Actualizar el índice del ticket activo si existe en la lista
        if (m_oTicket != null) {
            for (int i = 0; i < ventasActivas.size(); i++) {
                if (ventasActivas.get(i) == m_oTicket) {
                    ventaActualIndex = i;
                    break;
                }
            }
            updateTabsBar(); // Actualizar pestañas para resaltar la activa
        }

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
            // m_jSubtotalEuros.setText(null); // Ya no se muestra
            // m_jTaxesEuros.setText(null); // Ya no se muestra
            m_jTotalEuros.setText(null);
            jCheckStock.setText(null);

            // Sebastian - Limpiar campos de cliente cuando no hay ticket
            m_jCustomerId.setText("");
            m_jCustomerName.setText("");
            m_jCustomerPoints.setText("");
            m_jCustomerPoints.setVisible(false);
            if (m_jProductosVenta != null) {
                m_jProductosVenta.setText("0 productos en la venta actual.");
            }

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
            // m_jSubtotalEuros.setText(null); // Ya no se muestra
            // m_jTaxesEuros.setText(null); // Ya no se muestra
            m_jTotalEuros.setText("$0.00"); // Mostrar $0.00 en lugar de null
            if (lblTotalValue != null)
                lblTotalValue.setText("$0.00");
            if (lblPagoConValue != null)
                lblPagoConValue.setText("$0.00");
            if (lblCambioValue != null)
                lblCambioValue.setText("$0.00");
            if (m_jProductosVenta != null) {
                m_jProductosVenta.setText("0 productos en la venta actual.");
            }
        } else {
            // m_jSubtotalEuros.setText(m_oTicket.printSubTotal()); // Ya no se muestra
            // m_jTaxesEuros.setText(m_oTicket.printTax()); // Ya no se muestra
            m_jTotalEuros.setText(m_oTicket.printTotal());

            // Actualizar labels de información estilo Eleventa
            if (lblTotalValue != null) {
                lblTotalValue.setText(m_oTicket.printTotal());
            }

            if (m_jProductosVenta != null) {
                int productosCount = m_oTicket.getLinesCount();
                m_jProductosVenta.setText(
                        productosCount + " producto" + (productosCount != 1 ? "s" : "") + " en la venta actual.");
            }
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
                LOGGER.log(System.Logger.Level.INFO, "Adding product marked as SERVICE to ticket: " + oProduct.getID()
                        + " - " + oProduct.getName() + ". This product will not decrement stock on save.");
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
            boolean foundMatchingLine = false;

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
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter(
                            "c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log",
                            true);
                    fw.write(
                            "{\"location\":\"JPanelTicket.java:1232\",\"message\":\"addTicketLine called\",\"data\":{\"productId\":"
                                    + (oLine.getProductID() != null ? "\"" + oLine.getProductID() + "\"" : "null")
                                    + ",\"attSetInstId\":"
                                    + (oLine.getProductAttSetInstId() != null
                                            ? "\"" + oLine.getProductAttSetInstId() + "\""
                                            : "null")
                                    + ",\"price\":" + oLine.getPrice() + ",\"multiply\":" + oLine.getMultiply()
                                    + ",\"linesCount\":" + m_oTicket.getLinesCount() + "},\"timestamp\":"
                                    + System.currentTimeMillis()
                                    + ",\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                    fw.close();
                } catch (Exception e) {
                }
                // #endregion

                // Buscar si ya existe una línea con el mismo producto, atributos y precio
                for (int i = 0; i < m_oTicket.getLinesCount(); i++) {
                    TicketLineInfo existingLine = m_oTicket.getLine(i);
                    // Solo consolidar si no es producto compuesto
                    if (!existingLine.isProductCom()) {
                        // Comparar productid (puede ser null, usar Objects.equals para seguridad)
                        boolean sameProduct = java.util.Objects.equals(existingLine.getProductID(),
                                oLine.getProductID());
                        // Comparar attsetinstid (atributos de instancia, puede ser null)
                        boolean sameAttribs = java.util.Objects.equals(existingLine.getProductAttSetInstId(),
                                oLine.getProductAttSetInstId());
                        // Comparar precio (usar tolerancia para números de punto flotante)
                        boolean samePrice = Math.abs(existingLine.getPrice() - oLine.getPrice()) < 0.01;

                        if (sameProduct && sameAttribs && samePrice) {
                            // #region agent log
                            try {
                                java.io.FileWriter fw = new java.io.FileWriter(
                                        "c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log",
                                        true);
                                fw.write(
                                        "{\"location\":\"JPanelTicket.java:1248\",\"message\":\"Matching line found, consolidating\",\"data\":{\"lineIndex\":"
                                                + i + ",\"oldMultiply\":" + existingLine.getMultiply()
                                                + ",\"newMultiply\":"
                                                + (existingLine.getMultiply() + oLine.getMultiply())
                                                + "},\"timestamp\":" + System.currentTimeMillis()
                                                + ",\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                                fw.close();
                            } catch (Exception e) {
                            }
                            // #endregion

                            // Consolidar: incrementar la cantidad manteniendo el precio
                            existingLine.setMultiply(existingLine.getMultiply() + oLine.getMultiply());
                            // Actualizar la línea en el ticket y en la UI
                            // paintTicketLine ya actualiza todo: setLine, setTicketLine, countArticles,
                            // visorTicketLine, printPartialTotals, etc.
                            paintTicketLine(i, existingLine);
                            foundMatchingLine = true;
                            // Ejecutar el evento de cambio pero no las otras funciones (ya las ejecutó
                            // paintTicketLine)
                            executeEvent(m_oTicket, m_oTicketExt, TicketConstants.EV_TICKET_CHANGE);
                            return;
                        }
                    }
                }

                // Si no se encontró una línea coincidente, agregar como nueva línea
                if (!foundMatchingLine) {
                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter(
                                "c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log",
                                true);
                        fw.write(
                                "{\"location\":\"JPanelTicket.java:1275\",\"message\":\"No matching line found, adding new line\",\"data\":{},\"timestamp\":"
                                        + System.currentTimeMillis()
                                        + ",\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                        fw.close();
                    } catch (Exception e) {
                    }
                    // #endregion

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
            }

            // Si se consolidó, ya retornamos antes, así que llegamos aquí solo si NO se
            // consolidó
            // Actualizar visor y funciones solo si no se consolidó (paintTicketLine ya lo
            // hizo si se consolidó)
            if (!foundMatchingLine || oLine.isProductCom()) {
                visorTicketLine(oLine);
                printPartialTotals();
                stateToZero();
                countArticles();
            }

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
     * Sebastian - Método para agregar un producto "Varios" con nombre y precio
     * personalizado
     */
    private void agregarProductoVarios() {
        // Verificar que haya un ticket activo
        if (m_oTicket == null) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noticket"));
            msg.show(this);
            return;
        }

        // Crear diálogo para ingresar nombre y precio
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        gbc.anchor = java.awt.GridBagConstraints.WEST;

        // Campo para nombre del producto
        javax.swing.JLabel lblNombre = new javax.swing.JLabel("Nombre del producto:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(lblNombre, gbc);

        javax.swing.JTextField txtNombre = new javax.swing.JTextField(20);
        txtNombre.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(txtNombre, gbc);

        // Campo para precio
        javax.swing.JLabel lblPrecio = new javax.swing.JLabel("Precio:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        panel.add(lblPrecio, gbc);

        javax.swing.JTextField txtPrecio = new javax.swing.JTextField(15);
        txtPrecio.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 14));
        txtPrecio.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        gbc.gridx = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(txtPrecio, gbc);

        // Mostrar diálogo
        int result = javax.swing.JOptionPane.showConfirmDialog(
                this,
                panel,
                "Agregar Producto Varios",
                javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.PLAIN_MESSAGE);

        if (result == javax.swing.JOptionPane.OK_OPTION) {
            try {
                String nombre = txtNombre.getText().trim();
                if (nombre.isEmpty()) {
                    MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, "Debe ingresar un nombre para el producto");
                    msg.show(this);
                    return;
                }

                String precioStr = txtPrecio.getText().trim();
                if (precioStr.isEmpty()) {
                    precioStr = "0.00";
                }

                // Parsear el precio
                double precio = Formats.CURRENCY.parseValue(precioStr);
                if (precio < 0) {
                    MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, "El precio no puede ser negativo");
                    msg.show(this);
                    return;
                }

                // Crear producto genérico
                ProductInfoExt oProduct = new ProductInfoExt();
                oProduct.setReference("0000");
                oProduct.setCode("0000");
                oProduct.setName(nombre);

                // Obtener categoría de impuestos por defecto
                String taxCategoryID = ((TaxCategoryInfo) taxcategoriesmodel.getSelectedItem()).getID();
                oProduct.setTaxCategoryID(taxCategoryID);

                // Ajustar precio según si incluye impuestos o no
                double precioAjustado = includeTaxes(taxCategoryID, precio);
                oProduct.setPriceSell(precioAjustado);

                // Marcar como servicio para que no afecte el inventario
                oProduct.setService(true);

                // Agregar al ticket
                addTicketLine(oProduct, 1.0, precioAjustado);

                // Limpiar campo de búsqueda y devolver foco
                m_jKeyFactory.setText(null);
                java.awt.EventQueue.invokeLater(() -> {
                    m_jKeyFactory.requestFocus();
                });

            } catch (Exception ex) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                        "Error al agregar producto: " + ex.getMessage());
                msg.show(this);
                LOGGER.log(System.Logger.Level.WARNING, "Error agregando producto varios", ex);
            }
        }
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
            // Primero intentar buscar por código de barras
            ProductInfoExt oProduct = dlSales.getProductInfoByCode(sCode);

            // Si no se encuentra por código de barras, intentar por referencia
            if (oProduct == null) {
                try {
                    oProduct = dlSales.getProductInfoByReference(sCode);
                } catch (BasicException exRef) {
                    // Si falla la búsqueda por referencia, continuar con el flujo normal
                    LOGGER.log(System.Logger.Level.DEBUG, "No se encontró producto por referencia: " + sCode, exRef);
                }
            }

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
            // Sebastian - Protección contra doble apertura del diálogo
            if (m_bGranelDialogOpen) {
                System.out.println("DEBUG: Diálogo de granel ya está abierto, ignorando segunda llamada");
                return;
            }

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

            // Marcar que el diálogo está abierto ANTES de mostrarlo
            m_bGranelDialogOpen = true;

            try {
                Double peso = JGranelDialog.mostrarDialogo(
                        parentWindow,
                        prod.getName() != null ? prod.getName() : "Producto Granel",
                        prod.getPriceSell());

                System.out.println("DEBUG: Peso retornado: " + peso);

                if (peso != null && peso > 0) {
                    incProduct(prod, peso);
                }
            } finally {
                // Siempre liberar el flag, incluso si hay una excepción
                m_bGranelDialogOpen = false;
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
                        // Sebastian - Eliminar el ticket cerrado de la lista de pestañas
                        TicketInfo ticketCerrado = m_oTicket;
                        setActiveTicket(null, null);
                        refreshTicket();
                        m_ticketsbag.deleteTicket();

                        if (isAutoLogout()) {
                            if (isRestaurantMode() && isAutoLogoutRestaurant()) {
                                deactivate();
                            } else {
                                ((JRootApp) m_App).closeAppView();
                            }
                        }

                        // Eliminar el ticket de la lista si existe
                        if (ventasActivas.contains(ticketCerrado)) {
                            ventasActivas.remove(ticketCerrado);
                            // Ajustar el índice si es necesario
                            if (ventaActualIndex >= ventasActivas.size() && !ventasActivas.isEmpty()) {
                                ventaActualIndex = ventasActivas.size() - 1;
                            }
                        }

                        // Si quedan tickets, activar uno; si no, crear uno nuevo
                        if (!ventasActivas.isEmpty() && ventaActualIndex >= 0
                                && ventaActualIndex < ventasActivas.size()) {
                            setActiveTicket(ventasActivas.get(ventaActualIndex), null);
                        } else {
                            createNewTicket();
                        }

                        updateTabsBar(); // Actualizar pestañas después de eliminar
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
        // Sebastian - Usar el sistema de pestañas para crear nuevo ticket
        abrirNuevaVenta();
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

                            // Imprimir solo el ticket original cuando se selecciona imprimir
                            // La copia (Printer.Ticket2) solo se imprime cuando se solicita explícitamente
                            // desde "Ventas del día y Devoluciones"
                            // #region agent log
                            try {
                                java.io.FileWriter fw = new java.io.FileWriter(
                                        "c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log",
                                        true);
                                fw.write("{\"id\":\"log_" + System.currentTimeMillis()
                                        + "_close_ticket\",\"timestamp\":" + System.currentTimeMillis()
                                        + ",\"location\":\"JPanelTicket.java:2628\",\"message\":\"closeTicket printing logic\",\"data\":{\"ticketId\":"
                                        + ticket.getTicketId() + ",\"printSelected\":" + paymentdialog.isPrintSelected()
                                        + ",\"warrantyPrint\":" + warrantyPrint + ",\"willPrintOriginal\":"
                                        + (paymentdialog.isPrintSelected() || warrantyPrint)
                                        + ",\"willPrintCopy\":false},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"G\"}\n");
                                fw.close();
                                System.out.println("DEBUG: closeTicket - ticketId=" + ticket.getTicketId()
                                        + ", willPrintOriginal=" + (paymentdialog.isPrintSelected() || warrantyPrint)
                                        + ", willPrintCopy=false");
                            } catch (Exception ex) {
                                System.out.println("DEBUG: Error logging closeTicket: " + ex.getMessage());
                            }
                            // #endregion
                            if (paymentdialog.isPrintSelected() || warrantyPrint) {
                                try {
                                    // Actualizar el template Printer.Ticket antes de imprimir
                                    actualizarTemplateTicketEnBD();
                                    printTicket("Printer.Ticket", ticket, ticketext);
                                    Notify(AppLocal.getIntString("notify.printing"));
                                } catch (Exception ex) {
                                    LOGGER.log(System.Logger.Level.ERROR, "Exception on printTicket: Printer.Ticket",
                                            ex);
                                }
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
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter(
                    "c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log", true);
            fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_entry\",\"timestamp\":"
                    + System.currentTimeMillis()
                    + ",\"location\":\"JPanelTicket.java:2649\",\"message\":\"printTicket method entry\",\"data\":{\"resource\":\""
                    + sresourcename + "\",\"ticketId\":" + (ticket != null ? ticket.getTicketId() : "null")
                    + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\"}\n");
            fw.close();
        } catch (IOException ex) {
        }
        // #endregion

        String processTemaplated = "";
        LOGGER.log(System.Logger.Level.INFO, "Reading resource id: " + sresourcename);
        String sresource = dlSystem.getResourceAsXML(sresourcename);

        // #region agent log
        try {
            int ticketIndex = sresource != null ? sresource.indexOf("<ticket>") : -1;
            int displayIndex = sresource != null ? sresource.indexOf("<display>") : -1;
            boolean ticketFirst = ticketIndex >= 0 && (displayIndex < 0 || ticketIndex < displayIndex);
            boolean hasValor = sresource != null && sresource.contains(">Valor</text>");
            boolean hasImporte = sresource != null && sresource.contains("length=\"10\">Importe</text>");
            boolean hasCode7 = sresource != null && sresource.contains("length=\"7\">Código</text>");
            boolean hasCode8 = sresource != null && sresource.contains("length=\"8\">Código</text>");
            boolean hasArticulo12 = sresource != null && sresource.contains("length=\"12\">Artículo</text>");
            boolean hasArticulo15 = sresource != null && sresource.contains("length=\"15\">Artículo</text>");
            java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log", true);
            fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_resource\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"JPanelTicket.java:2815\",\"message\":\"Resource loaded from DB\",\"data\":{\"resource\":\"" + sresourcename + "\",\"isNull\":" + (sresource == null) + ",\"length\":" + (sresource != null ? sresource.length() : 0) + ",\"hasTicketTag\":" + (ticketIndex >= 0) + ",\"hasDisplayTag\":" + (displayIndex >= 0) + ",\"ticketIndex\":" + ticketIndex + ",\"displayIndex\":" + displayIndex + ",\"ticketFirst\":" + ticketFirst + ",\"hasValor\":" + hasValor + ",\"hasImporte\":" + hasImporte + ",\"hasCode7\":" + hasCode7 + ",\"hasCode8\":" + hasCode8 + ",\"hasArticulo12\":" + hasArticulo12 + ",\"hasArticulo15\":" + hasArticulo15 + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\"}\n");
            fw.close();
            System.out.println("DEBUG: Resource " + sresourcename + " loaded from DB - hasValor=" + hasValor + ", hasImporte=" + hasImporte + ", hasCode7=" + hasCode7 + ", hasCode8=" + hasCode8 + ", hasArticulo12=" + hasArticulo12 + ", hasArticulo15=" + hasArticulo15);
        } catch (IOException ex) {
            System.out.println("DEBUG: Error logging resource load: " + ex.getMessage());
        }
        // #endregion

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
                
                // Sebastian - Inicializar variables de puntos siempre (para evitar errores en Velocity)
                script.put("customerPoints", null);
                script.put("customerPointsAfter", null);
                
                // Sebastian - Agregar puntos del cliente al template si hay cliente
                if (ticket.getCustomer() != null && puntosDataLogic != null) {
                    try {
                        int puntosCliente = puntosDataLogic.obtenerPuntos(ticket.getCustomer().getId());
                        script.put("customerPoints", puntosCliente);
                        
                        // Calcular puntos después de la venta
                        double totalAcumulable = 0.0;
                        for (TicketLineInfo line : ticket.getLines()) {
                            if (line.isProductAccumulatesPoints()) {
                                totalAcumulable += line.getValue();
                            }
                        }
                        
                        PuntosConfiguracion config = puntosDataLogic.getConfiguracionActiva();
                        int puntosNuevos = 0;
                        if (config != null && config.isSistemaActivo() && totalAcumulable > 0) {
                            puntosNuevos = config.calcularPuntos(totalAcumulable);
                            
                            // Verificar límite diario
                            try {
                                int puntosGanadosHoy = puntosDataLogic.getPuntosGanadosHoy(ticket.getCustomer().getId());
                                int limiteDiario = config.getLimiteDiarioPuntos();
                                
                                // Ajustar puntos si exceden el límite
                                if (puntosGanadosHoy >= limiteDiario) {
                                    puntosNuevos = 0;
                                } else if (puntosGanadosHoy + puntosNuevos > limiteDiario) {
                                    puntosNuevos = limiteDiario - puntosGanadosHoy;
                                }
                            } catch (Exception e) {
                                // Si hay error al obtener puntos ganados hoy, continuar con el cálculo normal
                            }
                        }
                        
                        int puntosDespues = puntosCliente + puntosNuevos;
                        script.put("customerPointsAfter", puntosDespues);
                        
                        // #region agent log
                        try {
                            java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log", true);
                            fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_points_calc\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"JPanelTicket.java:2905\",\"message\":\"Customer points calculated\",\"data\":{\"customerId\":\"" + ticket.getCustomer().getId() + "\",\"puntosActuales\":" + puntosCliente + ",\"puntosNuevos\":" + puntosNuevos + ",\"puntosDespues\":" + puntosDespues + ",\"totalAcumulable\":" + totalAcumulable + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"I\"}\n");
                            fw.close();
                            System.out.println("DEBUG: Puntos calculados - Actuales: " + puntosCliente + ", Nuevos: " + puntosNuevos + ", Después: " + puntosDespues);
                        } catch (Exception ex2) {
                            System.out.println("DEBUG: Error logging points calculation: " + ex2.getMessage());
                        }
                        // #endregion
                    } catch (Exception ex) {
                        // Si no se pueden obtener los puntos, no agregar la variable
                        LOGGER.log(System.Logger.Level.WARNING, "Error obteniendo puntos del cliente para template: " + ex.getMessage());
                        // #region agent log
                        try {
                            java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log", true);
                            fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_points_error\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"JPanelTicket.java:2908\",\"message\":\"Error calculating customer points\",\"data\":{\"error\":\"" + ex.getMessage().replace("\"", "\\\"") + "\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"I\"}\n");
                            fw.close();
                        } catch (Exception ex2) {}
                        // #endregion
                    }
                } else {
                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log", true);
                        fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_no_customer_points\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"JPanelTicket.java:2871\",\"message\":\"No customer or puntosDataLogic for points calculation\",\"data\":{\"hasCustomer\":" + (ticket.getCustomer() != null) + ",\"hasPuntosDataLogic\":" + (puntosDataLogic != null) + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"I\"}\n");
                        fw.close();
                    } catch (Exception ex2) {}
                    // #endregion
                }

                // TODO - MUST present to the progress o printing processing
                refreshTicket();

                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log", true);
                    fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_before_velocity_eval\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"JPanelTicket.java:2777\",\"message\":\"Before Velocity eval\",\"data\":{\"resource\":\"" + sresourcename + "\",\"hasCustomer\":" + (ticket.getCustomer() != null) + ",\"customerId\":" + (ticket.getCustomer() != null ? "\"" + ticket.getCustomer().getId() + "\"" : "null") + ",\"hasPuntosDataLogic\":" + (puntosDataLogic != null) + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"H\"}\n");
                    fw.close();
                } catch (IOException ex) {}
                // #endregion
                
                try {
                    processTemaplated = script.eval(sresource).toString();
                } catch (ScriptException ex) {
                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log", true);
                        fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_velocity_error\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"JPanelTicket.java:2782\",\"message\":\"Velocity evaluation error\",\"data\":{\"resource\":\"" + sresourcename + "\",\"error\":\"" + ex.getMessage().replace("\"", "\\\"").replace("\n", "\\n") + "\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"H\"}\n");
                        fw.close();
                        System.out.println("DEBUG: Velocity error: " + ex.getMessage());
                    } catch (IOException ex2) {}
                    // #endregion
                    throw ex;
                }
                
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter(
                            "c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log",
                            true);
                    String xmlPreview = processTemaplated.length() > 500 ? processTemaplated.substring(0, 500) + "..."
                            : processTemaplated;
                    int ticketIndex = processTemaplated.indexOf("<ticket>");
                    int displayIndex = processTemaplated.indexOf("<display>");
                    boolean ticketTagFirst = ticketIndex >= 0 && (displayIndex < 0 || ticketIndex < displayIndex);
                    fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_processed\",\"timestamp\":"
                            + System.currentTimeMillis()
                            + ",\"location\":\"JPanelTicket.java:2688\",\"message\":\"Template processed by Velocity\",\"data\":{\"resource\":\""
                            + sresourcename + "\",\"xmlLength\":" + processTemaplated.length() + ",\"hasTicketTag\":"
                            + processTemaplated.contains("<ticket>") + ",\"hasDisplayTag\":"
                            + processTemaplated.contains("<display>") + ",\"ticketTagFirst\":" + ticketTagFirst
                            + ",\"xmlPreview\":\""
                            + xmlPreview.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
                            + "\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\"}\n");
                    fw.close();
                } catch (IOException ex) {
                }
                // #endregion

                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter(
                            "c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log",
                            true);
                    fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_before_ttp\",\"timestamp\":"
                            + System.currentTimeMillis()
                            + ",\"location\":\"JPanelTicket.java:2689\",\"message\":\"Before m_TTP.printTicket call\",\"data\":{\"resource\":\""
                            + sresourcename
                            + "\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"C\"}\n");
                    fw.close();
                } catch (IOException ex) {
                }
                // #endregion

                m_TTP.printTicket(processTemaplated, ticket);

                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter(
                            "c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log",
                            true);
                    fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_after_ttp\",\"timestamp\":"
                            + System.currentTimeMillis()
                            + ",\"location\":\"JPanelTicket.java:2689\",\"message\":\"After m_TTP.printTicket call\",\"data\":{\"resource\":\""
                            + sresourcename
                            + "\",\"status\":\"completed\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"C\"}\n");
                    fw.close();
                } catch (IOException ex) {
                }
                // #endregion
            } catch (ScriptException | TicketPrinterException ex) {
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter(
                            "c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log",
                            true);
                    fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_exception\",\"timestamp\":"
                            + System.currentTimeMillis()
                            + ",\"location\":\"JPanelTicket.java:2690\",\"message\":\"Exception in printTicket processing\",\"data\":{\"resource\":\""
                            + sresourcename + "\",\"error\":\"" + ex.getMessage().replace("\"", "\\\"")
                            + "\",\"class\":\"" + ex.getClass().getName()
                            + "\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"D\"}\n");
                    fw.close();
                } catch (IOException ex2) {
                }
                // #endregion
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

    /**
     * Sebastian - Método auxiliar para actualizar el label de puntos en
     * JPrincipalApp
     */
    private void updatePrincipalAppCustomerPoints(String text, boolean visible) {
        try {
            // Intentar acceder a través de m_App si es JRootApp
            if (m_App instanceof com.openbravo.pos.forms.JRootApp) {
                try {
                    java.lang.reflect.Field field = com.openbravo.pos.forms.JRootApp.class
                            .getDeclaredField("m_principalapp");
                    field.setAccessible(true);
                    com.openbravo.pos.forms.JPrincipalApp principalApp = (com.openbravo.pos.forms.JPrincipalApp) field
                            .get(m_App);
                    if (principalApp != null) {
                        principalApp.updateCustomerPointsDisplay(text, visible);
                        return;
                    }
                } catch (Exception e) {
                    // Si falla la reflexión, buscar en la jerarquía
                }
            }

            // Buscar JPrincipalApp en la jerarquía de componentes
            java.awt.Container parent = this.getParent();
            while (parent != null) {
                if (parent instanceof com.openbravo.pos.forms.JPrincipalApp) {
                    ((com.openbravo.pos.forms.JPrincipalApp) parent).updateCustomerPointsDisplay(text, visible);
                    return;
                }
                parent = parent.getParent();
            }
        } catch (Exception e) {
            System.err.println("Error al actualizar puntos en JPrincipalApp: " + e.getMessage());
        }
    }

    /**
     * Sebastian - Actualiza la vista de puntos del cliente después de operaciones
     * (cancelar/devolver)
     * 
     * @param clienteId ID del cliente cuyos puntos se actualizaron
     */
    private void actualizarVistaPuntosCliente(String clienteId) {
        try {
            if (puntosDataLogic == null || clienteId == null) {
                return;
            }

            // Obtener puntos actuales del cliente
            int puntosActuales = puntosDataLogic.obtenerPuntos(clienteId);

            // Si hay un ticket activo con este cliente, actualizar la vista
            if (m_oTicket != null && m_oTicket.getCustomer() != null &&
                    clienteId.equals(m_oTicket.getCustomer().getId())) {
                updateCustomerPointsDisplay();
            }

            // Actualizar también en JPrincipalApp si está disponible
            try {
                if (m_oTicket != null && m_oTicket.getCustomer() != null &&
                        clienteId.equals(m_oTicket.getCustomer().getId())) {
                    String nombreCliente = m_oTicket.getCustomer().getName();
                    String textoCompleto = String.format("%s %d", nombreCliente, puntosActuales);
                    updatePrincipalAppCustomerPoints(textoCompleto, true);
                }
            } catch (Exception e) {
                // Silencioso si no se puede actualizar
            }

            System.out.println(
                    "✅ Vista de puntos actualizada para cliente " + clienteId + " - Puntos: " + puntosActuales);

        } catch (Exception e) {
            System.err.println("⚠️ Error actualizando vista de puntos: " + e.getMessage());
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

                // Actualizar label en JPrincipalApp (barra superior)
                updatePrincipalAppCustomerPoints(textoCompleto, true);

                System.out.println("✅ Label actualizado y visible");

            } catch (Exception ex) {
                System.err.println("❌ Error updating customer points display: " + ex.getMessage());
                ex.printStackTrace();
                LOGGER.log(System.Logger.Level.WARNING, "Error updating customer points display: ", ex);
                updatePrincipalAppCustomerPoints(m_oTicket.getCustomer().getName(), true);
            }
        } else {
            System.out.println("🚫 No hay cliente asignado al ticket");
            updatePrincipalAppCustomerPoints("", false);
            if (m_jProductosVenta != null) {
                m_jProductosVenta.setText("0 productos en la venta actual.");
            }
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
        m_jProductosVenta = new javax.swing.JLabel(); // Sebastian - Label para productos de la venta actual
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

        m_jPanelContainer.setLayout(new java.awt.BorderLayout(0, 0)); // Sin gaps para eliminar espacios
        m_jPanelContainer.setBackground(new java.awt.Color(220, 220, 220)); // Fondo gris para el contenedor
        m_jPanelContainer.setOpaque(true);
        m_jPanelContainer.setBorder(null); // Sin bordes que creen espacio

        m_jPanelMainToolbar.setLayout(new java.awt.BorderLayout());
        m_jPanelMainToolbar.setBackground(new java.awt.Color(220, 220, 220)); // Fondo gris para el toolbar
        m_jPanelMainToolbar.setOpaque(true);

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
        jBtnCustomer.setToolTipText(bundle.getString("tooltip.salescustomer") + " (F5)"); // NOI18N
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

        // Sebastian - Eliminar padding izquierdo para que el contenido esté
        // completamente a la izquierda
        // Sebastian - Eliminar padding inferior para que el contenido llegue al límite
        m_jPanelTicket.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 5)); // Sin padding inferior
        m_jPanelTicket.setLayout(new java.awt.BorderLayout(0, 0)); // Sin gaps
        m_jPanelTicket.setBackground(new java.awt.Color(220, 220, 220)); // Fondo gris que continúa desde arriba
        m_jPanelTicket.setOpaque(true);

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
        m_jDelete.setToolTipText(bundle.getString("tooltip.saleremoveline") + " (F6)"); // NOI18N
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
        m_jList.setToolTipText(bundle.getString("tooltip.saleproductfind") + " (F7)"); // NOI18N
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
        m_jEditLine.setToolTipText(bundle.getString("tooltip.saleeditline") + " (F8)"); // NOI18N
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
        jEditAttributes.setToolTipText(bundle.getString("tooltip.saleattributes") + " (F9)"); // NOI18N
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

        // Sebastian - Hacer invisible solo algunos botones, pero mantener visible el
        // botón de eliminar
        // m_jDelete.setVisible(false); // Mantener visible para poder eliminar líneas
        // individuales
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

        // Sebastian - Limpiar el panel (ya no se usan estos botones, ahora se usan
        // pestañas)
        jPanel2.removeAll();

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

        // Sebastian - Ocultar completamente la barra lateral para que la tabla ocupe
        // todo el ancho
        m_jPanelLinesToolbar.setVisible(false);
        m_jPanelLinesToolbar.setPreferredSize(new java.awt.Dimension(0, 0));

        // No agregar la barra lateral al panel de ticket
        // m_jPanelTicket.add(m_jPanelLinesToolbar, java.awt.BorderLayout.LINE_START);

        m_jPanelLines.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        // Sebastian - Expandir el panel de líneas para ocupar TODO el ancho disponible
        // (sin barra lateral)
        // Remover el tamaño preferido limitado para que ocupe todo el espacio
        m_jPanelLines.setPreferredSize(null);
        m_jPanelLines.setLayout(new java.awt.BorderLayout(0, 0)); // Sin gaps para bajar la tabla
        m_jPanelLines.setBorder(null); // Sin bordes que creen espacio

        m_jPanelLinesSum.setLayout(new java.awt.BorderLayout(0, 0)); // Sin gaps
        m_jPanelLinesSum.setBorder(null); // Sin bordes que creen espacio
        m_jPanelLinesSum.setPreferredSize(null); // Sin tamaño preferido que cree espacio
        m_jPanelLinesSum.setMaximumSize(null); // Sin tamaño máximo que limite
        // Sebastian - Eliminar el filler para que no haya espacio en blanco a la
        // izquierda
        // m_jPanelLinesSum.add(filler2, java.awt.BorderLayout.LINE_START);

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
        // Sebastian - Eliminar customerPanel completamente para bajar más la tabla
        // m_jPanelLinesSum.add(customerPanel, java.awt.BorderLayout.NORTH);

        m_jTicketId.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        m_jTicketId.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jTicketId.setText("ID");
        m_jTicketId.setToolTipText("");
        m_jTicketId.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        m_jTicketId.setOpaque(true);
        // Sebastian - Ocultar m_jTicketId para liberar espacio a la izquierda
        m_jTicketId.setPreferredSize(new java.awt.Dimension(0, 0));
        m_jTicketId.setVisible(false);
        // m_jPanelLinesSum.add(m_jTicketId, java.awt.BorderLayout.CENTER);

        // Sebastian - Configuración del label de puntos del cliente
        m_jCustomerPoints.setFont(new java.awt.Font("Arial", 1, 14)); // Tamaño ajustado
        m_jCustomerPoints.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        m_jCustomerPoints.setText("");
        m_jCustomerPoints.setToolTipText("Puntos del cliente");
        m_jCustomerPoints.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        m_jCustomerPoints.setOpaque(false); // Sin fondo
        m_jCustomerPoints.setPreferredSize(new java.awt.Dimension(300, 28)); // Tamaño ajustado para estar al lado de
                                                                             // los botones
        m_jCustomerPoints.setRequestFocusEnabled(false);
        m_jCustomerPoints.setVerticalTextPosition(javax.swing.SwingConstants.CENTER);
        m_jCustomerPoints.setForeground(new java.awt.Color(0, 0, 0)); // Texto negro
        m_jCustomerPoints.setBackground(null); // Sin fondo
        m_jCustomerPoints.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10)); // Solo padding, sin
                                                                                                // borde
        // No añadir aquí, se añadirá al customerPointsPanel más adelante

        // Panel para área inferior completa estilo Eleventa
        // Sebastian - Reducir padding al mínimo para acercarlo a la barra inferior
        m_jPanelTotals.setPreferredSize(new java.awt.Dimension(Integer.MAX_VALUE, 120)); // Altura aumentada, ancho
                                                                                         // completo
        // Sebastian - Sin padding para acercarlo lo más posible a la barra inferior
        m_jPanelTotals.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        m_jPanelTotals.setBackground(java.awt.Color.WHITE); // Fondo blanco como Eleventa
        m_jPanelTotals.setOpaque(true);
        // Sebastian - Usar BorderLayout para que leftPanel esté pegado al borde
        // izquierdo
        m_jPanelTotals.setLayout(new java.awt.BorderLayout(0, 0)); // Sin gaps

        // === COLUMNA IZQUIERDA: Información y botones ===
        javax.swing.JPanel leftPanel = new javax.swing.JPanel();
        leftPanel.setLayout(new java.awt.BorderLayout(0, 0)); // BorderLayout para alinear abajo
        leftPanel.setOpaque(false);
        // Sebastian - Sin padding ni bordes para que esté completamente a la izquierda
        leftPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Panel contenedor para el contenido izquierdo (infoPanel + botones)
        javax.swing.JPanel leftContentPanel = new javax.swing.JPanel();
        leftContentPanel.setLayout(new javax.swing.BoxLayout(leftContentPanel, javax.swing.BoxLayout.Y_AXIS));
        leftContentPanel.setOpaque(false);

        // Sebastian - Remover "productos de la venta actual" de aquí, se moverá arriba
        // del panel de botones

        // Panel con Total, Pago Con, Cambio
        javax.swing.JPanel infoPanel = new javax.swing.JPanel(new java.awt.GridLayout(3, 2, 5, 2));
        infoPanel.setOpaque(false);
        infoPanel.setMaximumSize(new java.awt.Dimension(300, 60));
        infoPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        javax.swing.JLabel lblTotal = new javax.swing.JLabel("Total:");
        lblTotal.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
        infoPanel.add(lblTotal);
        lblTotalValue = new javax.swing.JLabel("$0.00");
        lblTotalValue.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
        infoPanel.add(lblTotalValue);

        javax.swing.JLabel lblPagoCon = new javax.swing.JLabel("Pago Con:");
        lblPagoCon.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
        infoPanel.add(lblPagoCon);
        lblPagoConValue = new javax.swing.JLabel("$0.00");
        lblPagoConValue.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
        infoPanel.add(lblPagoConValue);

        javax.swing.JLabel lblCambio = new javax.swing.JLabel("Cambio:");
        lblCambio.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
        infoPanel.add(lblCambio);
        lblCambioValue = new javax.swing.JLabel("$0.00");
        lblCambioValue.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
        infoPanel.add(lblCambioValue);

        leftContentPanel.add(infoPanel);
        leftContentPanel.add(javax.swing.Box.createVerticalStrut(5)); // Reducir espacio

        // Panel de botones pequeños (F5 Cambiar, Eliminar)
        javax.swing.JPanel smallButtonsPanel = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));
        smallButtonsPanel.setOpaque(false);
        smallButtonsPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        javax.swing.JButton btnCambiar = new javax.swing.JButton("Cambiar");
        btnCambiar.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 10));
        btnCambiar.setPreferredSize(new java.awt.Dimension(100, 28));
        btnCambiar.setFocusPainted(false);
        btnCambiar.setBackground(java.awt.Color.WHITE);
        btnCambiar.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(200, 200, 200), 1));
        smallButtonsPanel.add(btnCambiar);

        javax.swing.JButton btnAsignarCliente = new javax.swing.JButton("F5 - Asignar Cliente");
        btnAsignarCliente.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 10));
        btnAsignarCliente.setPreferredSize(new java.awt.Dimension(140, 28));
        btnAsignarCliente.setFocusPainted(false);
        btnAsignarCliente.setBackground(java.awt.Color.WHITE);
        btnAsignarCliente.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(200, 200, 200), 1));
        btnAsignarCliente.addActionListener(e -> mostrarModalIdCliente());
        smallButtonsPanel.add(btnAsignarCliente);

        leftContentPanel.add(smallButtonsPanel);

        // Agregar contenido al leftPanel en SOUTH para alinearlo abajo
        leftPanel.add(leftContentPanel, java.awt.BorderLayout.SOUTH);

        // === COLUMNA DERECHA: Botón Cobrar + Total + Ventas del día y Devoluciones ===
        javax.swing.JPanel rightPanel = new javax.swing.JPanel();
        rightPanel.setLayout(new java.awt.BorderLayout(0, 0)); // Sin espacio vertical, todo pegado abajo
        rightPanel.setOpaque(false);
        rightPanel.setBorder(null); // Sin bordes que creen espacio

        // === Panel superior: Botón Cobrar y Total (horizontal) ===
        javax.swing.JPanel topRightPanel = new javax.swing.JPanel();
        topRightPanel.setLayout(new java.awt.BorderLayout(0, 0));
        topRightPanel.setOpaque(false);
        topRightPanel.setBorder(null); // Sin bordes que creen espacio

        // Panel para el total y el botón "Ventas del día y Devoluciones" (vertical,
        // alineado a la derecha)
        javax.swing.JPanel totalAndButtonPanel = new javax.swing.JPanel();
        totalAndButtonPanel.setLayout(new java.awt.BorderLayout(0, 2)); // 2px de espacio vertical mínimo entre total y
                                                                        // botón
        totalAndButtonPanel.setOpaque(false);

        // Total exactamente como Eleventa - número grande en azul, estilo delgado pero
        // legible
        // Basado en la imagen: fuente más grande, estilo regular/delgado, color azul
        java.awt.Font totalFont = new java.awt.Font("Arial", java.awt.Font.PLAIN, 52); // Tamaño como Eleventa (grande
                                                                                       // pero no bold)
        m_jTotalEuros.setFont(totalFont);
        m_jTotalEuros.setForeground(new java.awt.Color(0, 100, 200)); // Azul más claro como en Eleventa (no tan oscuro)
        m_jTotalEuros.setHorizontalAlignment(javax.swing.SwingConstants.LEFT); // Alineación a la IZQUIERDA para que
                                                                               // siempre empiece desde el mismo punto
        m_jTotalEuros.setText("$0.00");
        m_jTotalEuros.setOpaque(false); // Sin fondo
        m_jTotalEuros.setRequestFocusEnabled(false);
        // Padding izquierdo reducido porque el gap del panel moverá ambos componentes
        // juntos
        m_jTotalEuros.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 0)); // Padding mínimo solo para el
                                                                                          // texto dentro del label
        // Ancho suficiente para números grandes - cuando crezca se expandirá hacia la
        // DERECHA
        m_jTotalEuros.setPreferredSize(new java.awt.Dimension(300, 60)); // Ancho más generoso para números grandes
        m_jTotalEuros.setMinimumSize(new java.awt.Dimension(150, 60)); // Mínimo para números pequeños
        m_jTotalEuros.setMaximumSize(new java.awt.Dimension(320, 60)); // Máximo con espacio para crecer

        // Ocultar el label "Total:" porque Eleventa no lo tiene
        m_jLblTotalEuros.setVisible(false);

        // Botón Cobrar compacto, justo al lado del total
        m_jPayNow = new javax.swing.JButton();
        m_jPayNow.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        m_jPayNow.setText("F12 - Cobrar");
        m_jPayNow.setFocusPainted(false);
        m_jPayNow.setBackground(new java.awt.Color(92, 184, 92)); // Verde
        m_jPayNow.setForeground(java.awt.Color.WHITE);
        m_jPayNow.setPreferredSize(new java.awt.Dimension(160, 40)); // Más ancho
        m_jPayNow.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new java.awt.Color(76, 174, 76), 1),
                javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 4) // Padding derecho reducido para acercarlo al
                                                                         // total
        ));
        m_jPayNow.setOpaque(true);

        // Acción: reutiliza el flujo de cierre/pago de ticket
        m_jPayNow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (m_oTicket != null && m_oTicket.getLinesCount() > 0) {
                    if (closeTicket(m_oTicket, m_oTicketExt)) {
                        // Sebastian - Eliminar el ticket cerrado de la lista de pestañas
                        TicketInfo ticketCerrado = m_oTicket;
                        setActiveTicket(null, null);
                        refreshTicket();
                        m_ticketsbag.deleteTicket();

                        // Eliminar el ticket de la lista si existe
                        if (ventasActivas.contains(ticketCerrado)) {
                            ventasActivas.remove(ticketCerrado);
                            // Ajustar el índice si es necesario
                            if (ventaActualIndex >= ventasActivas.size() && !ventasActivas.isEmpty()) {
                                ventaActualIndex = ventasActivas.size() - 1;
                            }
                        }

                        // Si quedan tickets, activar uno; si no, crear uno nuevo
                        if (!ventasActivas.isEmpty() && ventaActualIndex >= 0
                                && ventaActualIndex < ventasActivas.size()) {
                            setActiveTicket(ventasActivas.get(ventaActualIndex), null);
                        } else {
                            createNewTicket();
                        }

                        updateTabsBar(); // Actualizar pestañas después de eliminar
                    }
                    refreshTicket();
                }
            }
        });

        // Panel para el total con el botón cobrar justo al lado (alineado a la derecha)
        // - estilo Eleventa
        // Usar BoxLayout horizontal para tener mejor control del posicionamiento
        javax.swing.JPanel totalPanel = new javax.swing.JPanel();
        totalPanel.setLayout(new javax.swing.BoxLayout(totalPanel, javax.swing.BoxLayout.X_AXIS));
        totalPanel.setOpaque(false);
        // Agregar espacio flexible a la izquierda para empujar el contenido a la
        // derecha
        totalPanel.add(javax.swing.Box.createHorizontalStrut(200)); // 200px de espacio a la izquierda para mover ambos
                                                                    // a la derecha
        totalPanel.add(m_jPayNow); // Botón cobrar primero (quedará a la izquierda del total)
        totalPanel.add(javax.swing.Box.createHorizontalStrut(5)); // Gap pequeño (5px) entre botón y total
        totalPanel.add(m_jTotalEuros); // Total después - posición fija, crece hacia la derecha
        totalPanel.add(javax.swing.Box.createHorizontalGlue()); // Espacio flexible a la derecha
        totalPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 20)); // Padding derecho para espacio
                                                                                        // cuando crezca la cifra

        // Agregar total y botón cobrar al panel (arriba)
        totalAndButtonPanel.add(totalPanel, java.awt.BorderLayout.NORTH);

        // === Botón "Ventas del día y Devoluciones" directamente debajo del total ===
        javax.swing.JButton btnVentasDelDia = new javax.swing.JButton();
        btnVentasDelDia.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 9)); // Fuente más pequeña
        btnVentasDelDia.setText("Ventas / Devoluciones");
        btnVentasDelDia.setFocusPainted(false);
        btnVentasDelDia.setPreferredSize(new java.awt.Dimension(140, 22)); // Tamaño más pequeño
        btnVentasDelDia.setMinimumSize(new java.awt.Dimension(140, 22));
        btnVentasDelDia.setMaximumSize(new java.awt.Dimension(140, 22));
        btnVentasDelDia.setBackground(java.awt.Color.WHITE);
        btnVentasDelDia.setForeground(new java.awt.Color(80, 80, 80));
        btnVentasDelDia.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200), 1));
        btnVentasDelDia.setOpaque(true);
        btnVentasDelDia.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        // Implementar la funcionalidad del botón
        btnVentasDelDia.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mostrarVentasDelDiaYDevoluciones();
            }
        });

        // Panel contenedor para el botón, alineado debajo del inicio del total
        javax.swing.JPanel btnVentasPanel = new javax.swing.JPanel();
        btnVentasPanel.setLayout(new javax.swing.BoxLayout(btnVentasPanel, javax.swing.BoxLayout.X_AXIS));
        btnVentasPanel.setOpaque(false);
        // Alinearlo con el INICIO del total: 200px (strut inicial) + 160px (ancho botón
        // cobrar) + 5px (gap) = 365px
        btnVentasPanel.add(javax.swing.Box.createHorizontalStrut(365)); // 200 + 160 + 5 = 365px para alinearlo con el
                                                                        // inicio del total
        btnVentasPanel.add(btnVentasDelDia);
        btnVentasPanel.add(javax.swing.Box.createHorizontalGlue()); // Espacio flexible a la derecha
        btnVentasPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 20)); // Padding derecho

        // Agregar botón al panel (debajo del total)
        totalAndButtonPanel.add(btnVentasPanel, java.awt.BorderLayout.SOUTH);

        // Agregar panel de total y botón cobrar al panel superior
        topRightPanel.add(totalAndButtonPanel, java.awt.BorderLayout.EAST);

        // Agregar panel superior al rightPanel en SOUTH para que esté pegado abajo
        rightPanel.add(topRightPanel, java.awt.BorderLayout.SOUTH);

        // Agregar paneles al m_jPanelTotals usando BorderLayout
        // leftPanel completamente a la izquierda
        m_jPanelTotals.add(leftPanel, java.awt.BorderLayout.WEST);

        // rightPanel (total grande en su posición original + botón cobrar al lado) a la
        // derecha
        m_jPanelTotals.add(rightPanel, java.awt.BorderLayout.EAST);

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

        // Sebastian - Eliminar "productos de la venta actual" y bajar la tabla lo más
        // posible
        // Panel contenedor solo para botones - sin espacios innecesarios
        javax.swing.JPanel bottomContainer = new javax.swing.JPanel();
        bottomContainer.setLayout(new java.awt.BorderLayout(0, 0)); // Sin gaps
        bottomContainer.setOpaque(false);
        bottomContainer.setBorder(null); // Sin bordes que creen espacio
        bottomContainer.setPreferredSize(null); // Sin tamaño preferido que cree espacio
        bottomContainer.setMaximumSize(null); // Sin tamaño máximo que limite
        bottomContainer.add(m_jPanelTotals, java.awt.BorderLayout.CENTER);

        // Sebastian - Agregar directamente el panel de botones sin espacios adicionales
        m_jPanelLinesSum.add(bottomContainer, java.awt.BorderLayout.SOUTH);

        // Sebastian - Agregar m_jPanelLinesSum directamente sin espacios
        m_jPanelLines.add(m_jPanelLinesSum, java.awt.BorderLayout.SOUTH);

        // Sebastian - Crear barra de pestañas sobre la tabla de ventas
        javax.swing.JPanel tabsPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 2));
        tabsPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(200, 200, 200)));
        tabsPanel.setBackground(new java.awt.Color(220, 220, 220)); // Gris suave para continuar el fondo
        tabsPanel.setPreferredSize(new java.awt.Dimension(0, 35));
        tabsPanel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 35)); // Limitar altura máxima
        tabsPanel.setMinimumSize(new java.awt.Dimension(0, 35)); // Limitar altura mínima
        tabsPanel.setName("tabsPanel"); // Para poder encontrarlo después

        // Panel contenedor para la barra de pestañas y la tabla
        javax.swing.JPanel linesWithTabsPanel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 0)); // Sin gaps
        linesWithTabsPanel.setBackground(new java.awt.Color(220, 220, 220)); // Fondo gris que continúa desde arriba
        linesWithTabsPanel.setOpaque(true);
        linesWithTabsPanel.setBorder(null); // Sin bordes que creen espacio
        linesWithTabsPanel.add(tabsPanel, java.awt.BorderLayout.NORTH);
        // Asegurar que el panel de líneas (tabla) tenga fondo blanco
        m_jPanelLines.setBackground(java.awt.Color.WHITE);
        m_jPanelLines.setOpaque(true);
        linesWithTabsPanel.add(m_jPanelLines, java.awt.BorderLayout.CENTER);

        m_jPanelTicket.add(linesWithTabsPanel, java.awt.BorderLayout.CENTER);

        // Guardar referencia al panel de pestañas para poder actualizarlo
        m_jTabsPanel = tabsPanel;

        // La barra de pestañas se inicializa al final del constructor después de que
        // m_App esté listo

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

        jPanelScanner.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 2, 5)); // Sin padding superior para
                                                                                          // subir el contenido
        jPanelScanner.setMaximumSize(new java.awt.Dimension(800, 55)); // Ajustar altura del panel para fuente más
                                                                       // grande
        jPanelScanner.setPreferredSize(new java.awt.Dimension(800, 55));

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
        m_jEnter.setContentAreaFilled(false); // Quitar el fondo azul del botón
        m_jEnter.setBorderPainted(false); // Quitar el borde
        m_jEnter.setOpaque(false); // Hacer transparente
        m_jEnter.setPreferredSize(new java.awt.Dimension(35, 40)); // Tamaño ajustado para coincidir con la altura del
                                                                   // campo
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
        m_jKeyFactory.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 22)); // Fuente Arial Bold más grande y
                                                                                   // gruesa para números más anchos
        m_jKeyFactory.setForeground(new java.awt.Color(33, 33, 33)); // Texto oscuro moderno
        m_jKeyFactory.setBackground(java.awt.Color.WHITE);
        m_jKeyFactory.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jKeyFactory.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 10, 8, 10)); // Solo padding, sin borde
        m_jKeyFactory.setOpaque(true);
        m_jKeyFactory.setPreferredSize(new java.awt.Dimension(500, 44));
        m_jKeyFactory.setMinimumSize(new java.awt.Dimension(350, 44));
        m_jKeyFactory.setMaximumSize(new java.awt.Dimension(500, 44)); // Limitar el ancho máximo
        m_jKeyFactory.setAutoscrolls(true);
        m_jKeyFactory.setCaretColor(new java.awt.Color(52, 152, 219));
        m_jKeyFactory.setRequestFocusEnabled(true);
        m_jKeyFactory.setVerifyInputWhenFocusTarget(false);
        m_jKeyFactory.setScrollOffset(0);
        m_jKeyFactory.setMargin(new java.awt.Insets(4, 6, 4, 6));
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

        // Crear un panel contenedor para el campo de búsqueda con icono integrado
        javax.swing.JPanel searchFieldContainer = new javax.swing.JPanel();
        searchFieldContainer.setLayout(new java.awt.BorderLayout());
        searchFieldContainer.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(52, 152, 219), 2), // Azul moderno
                javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        searchFieldContainer.setBackground(java.awt.Color.WHITE);
        searchFieldContainer.setOpaque(true);
        searchFieldContainer.setPreferredSize(new java.awt.Dimension(500, 44)); // Tamaño intermedio
        searchFieldContainer.setMinimumSize(new java.awt.Dimension(350, 44));
        searchFieldContainer.setMaximumSize(new java.awt.Dimension(500, 44)); // Limitar el ancho máximo

        // Panel para el icono con padding
        javax.swing.JPanel iconContainer = new javax.swing.JPanel();
        iconContainer.setLayout(new java.awt.BorderLayout());
        iconContainer.setOpaque(false);
        iconContainer.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 5)); // Padding mejorado
        iconContainer.add(m_jEnter, java.awt.BorderLayout.CENTER);

        // Agregar el icono a la izquierda
        searchFieldContainer.add(iconContainer, java.awt.BorderLayout.WEST);

        // Agregar el campo de texto ocupando el resto del espacio
        searchFieldContainer.add(m_jKeyFactory, java.awt.BorderLayout.CENTER);

        // Sebastian - Layout comentado porque ahora usamos el campo de manera diferente
        // El campo de búsqueda y el botón se agregan directamente al
        // scannerContainerPanel
        m_jContEntries.add(m_jPanEntries, java.awt.BorderLayout.LINE_START);

        // Sebastian - Comentar la adición del panel de entradas para liberar espacio
        // m_jPanelTicket.add(m_jContEntries, java.awt.BorderLayout.LINE_END);

        // Sebastian - Panel indicador de ticket con diseño elegante tipo eleventa
        // (gradiente con desvanecido)
        // La barra debe empezar desde el borde izquierdo y extenderse más allá de la
        // mitad con desvanecido suave
        javax.swing.JPanel lblTicketIndicator = new javax.swing.JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2d = (java.awt.Graphics2D) g.create();

                // Obtener el ancho del panel y calcular punto de desvanecido (más allá de la
                // mitad, aprox 60%)
                int width = getWidth();

                // Crear gradiente con desvanecido suave tipo eleventa - colores más claros
                // Empieza desde el borde izquierdo, va más allá de la mitad y se desvanece
                // suavemente
                java.awt.Color colorInicio = new java.awt.Color(100, 160, 220); // Azul claro más suave
                java.awt.Color colorMedio = new java.awt.Color(135, 190, 235); // Azul cielo claro
                java.awt.Color colorFin = new java.awt.Color(255, 255, 255, 0); // Transparente

                java.awt.LinearGradientPaint gradient = new java.awt.LinearGradientPaint(
                        0, 0, width, 0,
                        new float[] { 0.0f, 0.6f, 1.0f },
                        new java.awt.Color[] { colorInicio, colorMedio, colorFin });

                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, width, getHeight()); // Bordes cuadrados elegantes como eleventa

                g2d.dispose();
            }
        };
        lblTicketIndicator.setLayout(new java.awt.BorderLayout());
        lblTicketIndicator.setOpaque(false);
        // Hacer la barra más gruesa (más alta) como en Eleventa
        lblTicketIndicator.setPreferredSize(new java.awt.Dimension(0, 38)); // Más alto que antes
        lblTicketIndicator.setMinimumSize(new java.awt.Dimension(0, 38));

        // Label con el texto sobre el panel con gradiente
        javax.swing.JLabel lblTicketText = new javax.swing.JLabel("VENTA - Ticket 1");
        lblTicketText.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14)); // Fuente un poco más grande
        lblTicketText.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblTicketText.setForeground(java.awt.Color.WHITE);
        lblTicketText.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 15, 10, 15)); // Más padding vertical
                                                                                              // para la barra más
                                                                                              // gruesa
        lblTicketText.setOpaque(false);

        lblTicketIndicator.add(lblTicketText, java.awt.BorderLayout.CENTER);

        // Panel para el indicador de ticket que se extiende desde el borde izquierdo
        // (sin padding)
        javax.swing.JPanel ticketIndicatorPanel = new javax.swing.JPanel(new java.awt.BorderLayout());
        ticketIndicatorPanel.setOpaque(false);
        ticketIndicatorPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0)); // Sin padding inferior
                                                                                                 // para que quede justo
                                                                                                 // encima
        ticketIndicatorPanel.add(lblTicketIndicator, java.awt.BorderLayout.CENTER);

        // Guardar referencia al label de texto para actualizar dinámicamente
        this.m_jTicketIndicator = lblTicketText;

        // Crear panel para la barra de búsqueda en la parte superior - OCUPA TODO EL
        // ANCHO
        javax.swing.JPanel searchPanel = new javax.swing.JPanel(new java.awt.BorderLayout());
        searchPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 4, 10)); // Sin padding superior para
                                                                                          // que quede justo debajo de
                                                                                          // la barra azul
        searchPanel.setBackground(new java.awt.Color(245, 245, 245)); // Fondo gris claro moderno
        searchPanel.setOpaque(true);

        // Sebastian - Crear panel contenedor para la sección del escáner - ANCHO
        // COMPLETO
        javax.swing.JPanel scannerContainerPanel = new javax.swing.JPanel();
        scannerContainerPanel.setLayout(new java.awt.BorderLayout());
        scannerContainerPanel.setBackground(java.awt.Color.WHITE); // Fondo blanco para la sección
        scannerContainerPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200), 1), // Borde gris delgado
                                                                                                  // y elegante
                javax.swing.BorderFactory.createEmptyBorder(10, 20, 8, 20) // Padding superior reducido para compactar
        ));

        // Panel horizontal para el campo de código y botón ENTER (sin label)
        javax.swing.JPanel scannerInputPanel = new javax.swing.JPanel(new java.awt.BorderLayout(10, 0));
        scannerInputPanel.setOpaque(false);

        // Botón ENTER - Agregar Producto
        javax.swing.JButton btnAgregarProducto = new javax.swing.JButton();
        btnAgregarProducto.setText("ENTER - Agregar Producto");
        btnAgregarProducto.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        btnAgregarProducto.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/ok.png")));
        btnAgregarProducto.setForeground(java.awt.Color.WHITE);
        btnAgregarProducto.setBackground(new java.awt.Color(46, 204, 113)); // Verde atractivo
        btnAgregarProducto.setFocusPainted(false);
        btnAgregarProducto.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(39, 174, 96), 1),
                javax.swing.BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        btnAgregarProducto.setPreferredSize(new java.awt.Dimension(220, 44));
        btnAgregarProducto.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnAgregarProducto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jEnterActionPerformed(evt);
            }
        });

        // Label "Código del Producto" antes de la barra de búsqueda - Tipografía
        // moderna y decorativa
        javax.swing.JLabel lblCodigoProducto = new javax.swing.JLabel("Código del Producto");
        lblCodigoProducto.setFont(new java.awt.Font("Segoe UI", java.awt.Font.ITALIC, 13)); // Fuente moderna en cursiva
        lblCodigoProducto.setForeground(new java.awt.Color(100, 100, 120)); // Color gris elegante y moderno
        lblCodigoProducto.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 8)); // Espacio antes de la
                                                                                              // barra

        // Panel wrapper para el label, la barra de búsqueda y el botón ENTER juntos
        javax.swing.JPanel searchWrapper = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        searchWrapper.setOpaque(false);
        searchWrapper.add(lblCodigoProducto);
        searchWrapper.add(searchFieldContainer);
        searchWrapper.add(btnAgregarProducto); // Botón ENTER justo al lado de la barra

        scannerInputPanel.add(searchWrapper, java.awt.BorderLayout.WEST);

        // jPanelScanner debe tener fondo blanco también para estar dentro de la sección
        jPanelScanner.setBackground(java.awt.Color.WHITE);
        jPanelScanner.setOpaque(true);

        scannerContainerPanel.add(scannerInputPanel, java.awt.BorderLayout.CENTER);

        // Agregar directamente sin wrapper para ocupar todo el ancho
        searchPanel.add(scannerContainerPanel, java.awt.BorderLayout.CENTER);

        // Sebastian - Crear barra de botones de acción debajo del campo de búsqueda
        javax.swing.JPanel actionButtonsPanel = new javax.swing.JPanel();
        actionButtonsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 6, 0));
        actionButtonsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 20, 12, 20)); // Padding superior
                                                                                                  // reducido para
                                                                                                  // acercar a la barra
                                                                                                  // de búsqueda
        actionButtonsPanel.setBackground(new java.awt.Color(245, 245, 245)); // Mismo fondo que searchPanel

        // Estilo común para todos los botones
        java.awt.Color btnBg = java.awt.Color.WHITE;
        java.awt.Color btnFg = new java.awt.Color(60, 60, 60);
        java.awt.Color btnBorder = new java.awt.Color(220, 220, 220);
        java.awt.Font btnFont = new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11);
        int btnHeight = 36;

        // Botón Varios
        javax.swing.JButton btnVarios = new javax.swing.JButton("INS Varios");
        btnVarios.setPreferredSize(new java.awt.Dimension(108, btnHeight));
        btnVarios.setFont(btnFont);
        btnVarios.setFocusPainted(false);
        btnVarios.setBackground(btnBg);
        btnVarios.setForeground(btnFg);
        btnVarios.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(btnBorder, 1),
                javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        btnVarios.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnVarios.addActionListener(e -> {
            agregarProductoVarios();
        });
        actionButtonsPanel.add(btnVarios);

        // Botón Artículo Común
        javax.swing.JButton btnArticuloComun = new javax.swing.JButton("CTRL+P Art. Común");
        btnArticuloComun.setPreferredSize(new java.awt.Dimension(145, btnHeight));
        btnArticuloComun.setFont(btnFont);
        btnArticuloComun.setFocusPainted(false);
        btnArticuloComun.setBackground(btnBg);
        btnArticuloComun.setForeground(btnFg);
        btnArticuloComun.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(btnBorder, 1),
                javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        btnArticuloComun.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnArticuloComun.addActionListener(e -> {
            javax.swing.JOptionPane.showMessageDialog(this, "Función Artículo Común", "Artículo Común",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
        });
        actionButtonsPanel.add(btnArticuloComun);

        // Botón Mayoreo
        javax.swing.JButton btnMayoreo = new javax.swing.JButton("F11 Mayoreo");
        btnMayoreo.setPreferredSize(new java.awt.Dimension(110, btnHeight));
        btnMayoreo.setFont(btnFont);
        btnMayoreo.setFocusPainted(false);
        btnMayoreo.setBackground(btnBg);
        btnMayoreo.setForeground(btnFg);
        btnMayoreo.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(btnBorder, 1),
                javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        btnMayoreo.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnMayoreo.addActionListener(e -> {
            aplicarDescuentoMayoreo();
        });
        actionButtonsPanel.add(btnMayoreo);

        // Botón Entradas
        javax.swing.JButton btnEntradas = new javax.swing.JButton("F7 Entradas");
        btnEntradas.setPreferredSize(new java.awt.Dimension(105, btnHeight));
        btnEntradas.setFont(btnFont);
        btnEntradas.setFocusPainted(false);
        btnEntradas.setBackground(btnBg);
        btnEntradas.setForeground(btnFg);
        btnEntradas.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(btnBorder, 1),
                javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        btnEntradas.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnEntradas.addActionListener(e -> {
            showEntradasDialog();
        });
        actionButtonsPanel.add(btnEntradas);

        // Botón Salidas
        javax.swing.JButton btnSalidas = new javax.swing.JButton("F8 Salidas");
        btnSalidas.setPreferredSize(new java.awt.Dimension(100, btnHeight));
        btnSalidas.setFont(btnFont);
        btnSalidas.setFocusPainted(false);
        btnSalidas.setBackground(btnBg);
        btnSalidas.setForeground(btnFg);
        btnSalidas.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(btnBorder, 1),
                javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        btnSalidas.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnSalidas.addActionListener(e -> {
            showSalidasDialog();
        });
        actionButtonsPanel.add(btnSalidas);

        // Botón F6 - Eliminar Línea (eliminar producto del ticket)
        javax.swing.JButton btnEliminarLinea = new javax.swing.JButton("F6 Eliminar");
        btnEliminarLinea.setPreferredSize(new java.awt.Dimension(115, btnHeight));
        btnEliminarLinea.setFont(btnFont);
        btnEliminarLinea.setFocusPainted(false);
        btnEliminarLinea.setBackground(new java.awt.Color(220, 53, 69)); // Color rojo para eliminar
        btnEliminarLinea.setForeground(java.awt.Color.WHITE);
        btnEliminarLinea.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 35, 51), 1),
                javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        btnEliminarLinea.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnEliminarLinea.setToolTipText("Eliminar producto seleccionado del ticket (F6 o doble clic)");
        btnEliminarLinea.addActionListener(e -> {
            int i = m_ticketlines.getSelectedIndex();
            if (i < 0) {
                java.awt.Toolkit.getDefaultToolkit().beep();
            } else if (m_oTicket != null && i >= 0 && i < m_oTicket.getLinesCount()) {
                removeTicketLine(i);
            }
        });
        actionButtonsPanel.add(btnEliminarLinea);

        // Botón F4 - Nueva Venta (reemplaza DEL Borrar Art.)
        javax.swing.JButton btnF4Nueva = new javax.swing.JButton("F4 Nueva");
        btnF4Nueva.setPreferredSize(new java.awt.Dimension(100, btnHeight));
        btnF4Nueva.setFont(btnFont);
        btnF4Nueva.setFocusPainted(false);
        btnF4Nueva.setBackground(btnBg);
        btnF4Nueva.setForeground(btnFg);
        btnF4Nueva.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(btnBorder, 1),
                javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        btnF4Nueva.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnF4Nueva.addActionListener(e -> {
            createNewTicket();
        });
        actionButtonsPanel.add(btnF4Nueva);

        // Botones de la barra lateral movidos aquí
        // (Botón ID Cliente movido a la parte inferior)

        // Sebastian - El label de puntos del cliente ahora está en JPrincipalApp (barra
        // superior)
        // Ya no se usa m_jCustomerPoints en este panel

        // Crear un panel contenedor para el toolbar y la búsqueda
        javax.swing.JPanel topPanel = new javax.swing.JPanel(new java.awt.BorderLayout());
        topPanel.setBackground(new java.awt.Color(220, 220, 220)); // Fondo gris que continúa desde arriba
        topPanel.setOpaque(true);
        // Sebastian - Ocultar todo el toolbar principal para interfaz ultramoderna
        m_jPanelMainToolbar.setVisible(false);
        topPanel.add(m_jPanelMainToolbar, java.awt.BorderLayout.NORTH);

        // Agregar la barra de búsqueda y los botones de acción en un panel vertical
        javax.swing.JPanel searchAndActionsPanel = new javax.swing.JPanel(new java.awt.BorderLayout());
        searchAndActionsPanel.setBackground(new java.awt.Color(220, 220, 220)); // Fondo gris suave que continúa desde
                                                                                // arriba
        searchAndActionsPanel.setOpaque(true);
        searchAndActionsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0)); // Sin padding para
                                                                                                  // reducir espacio

        // Agregar la barra VENTA - Ticket primero, desde el borde izquierdo
        searchAndActionsPanel.add(ticketIndicatorPanel, java.awt.BorderLayout.NORTH);

        // Panel para searchPanel y actionButtonsPanel
        javax.swing.JPanel searchAndButtonsContainer = new javax.swing.JPanel(new java.awt.BorderLayout());
        searchAndButtonsContainer.setOpaque(false);
        searchAndButtonsContainer.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0)); // Sin padding
        searchAndButtonsContainer.add(searchPanel, java.awt.BorderLayout.NORTH);
        searchAndButtonsContainer.add(actionButtonsPanel, java.awt.BorderLayout.SOUTH);

        searchAndActionsPanel.add(searchAndButtonsContainer, java.awt.BorderLayout.CENTER);

        topPanel.add(searchAndActionsPanel, java.awt.BorderLayout.SOUTH);

        // Sebastian - Remover productosPanel de arriba, se quedará justo arriba del
        // panel de botones

        // Sebastian - Crear un panel contenedor completo que incluya los puntos arriba
        // de todo
        javax.swing.JPanel completeTopPanel = new javax.swing.JPanel(new java.awt.BorderLayout());
        completeTopPanel.setBackground(new java.awt.Color(220, 220, 220)); // Fondo gris que continúa desde la barra
                                                                           // superior
        completeTopPanel.setOpaque(true);
        // Sebastian - Sin espacio superior aquí, el espacio está en JPrincipalApp para
        // bajar la barra de botones
        completeTopPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0)); // Sin padding

        // Panel central con búsqueda y botones (los puntos están dentro de
        // actionButtonsPanel)
        completeTopPanel.add(topPanel, java.awt.BorderLayout.CENTER); // Panel central con búsqueda y botones

        m_jPanelContainer.add(completeTopPanel, java.awt.BorderLayout.NORTH);
        m_jPanelContainer.add(m_jPanelTicket, java.awt.BorderLayout.CENTER);

        m_jPanelCatalog.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        m_jPanelCatalog.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jPanelCatalog.setLayout(new java.awt.BorderLayout());
        // Sebastian - Ocultar m_jPanelCatalog o hacerlo invisible para eliminar espacio
        // inferior
        m_jPanelCatalog.setVisible(false);
        m_jPanelCatalog.setPreferredSize(new java.awt.Dimension(0, 0));
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
        // Sebastian - Procesar el texto del campo de búsqueda cuando se presiona el
        // botón
        String searchText = m_jKeyFactory.getText();
        if (searchText != null && !searchText.trim().isEmpty()) {
            // Limpiamos m_sBarcode y agregamos el texto completo
            m_sBarcode = new StringBuffer(searchText.trim());
            stateTransition('\n'); // Procesar como Enter para buscar y agregar producto
        } else {
            // Si no hay texto, solo hacer la transición de estado normal
            stateTransition('\n');
        }
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
    // Sebastian - Botones personalizados para atajos
    private javax.swing.JButton btnClienteCustom;
    private javax.swing.JButton btnEntradasSalidasCustom;
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
    private javax.swing.JLabel m_jProductosVenta; // Sebastian - Label para productos de la venta actual
    private javax.swing.JCheckBox m_jaddtax;
    private javax.swing.JButton m_jbtnScale;
    private javax.swing.JButton m_jPayNow; // Botón Pagar añadido

    // Sebastian - Campos para gestión de clientes
    private javax.swing.JTextField m_jCustomerId;
    private javax.swing.JLabel m_jCustomerName;
    private javax.swing.JLabel m_jLblCustomerId;
    private javax.swing.JLabel m_jCustomerPoints; // Label para mostrar puntos del cliente

    // Sebastian - Panel de pestañas para tickets múltiples
    private javax.swing.JPanel m_jTabsPanel;

    // Sebastian - Label indicador de ticket (arriba del escáner)
    private javax.swing.JLabel m_jTicketIndicator;
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
     * Maneja tanto ventas normales (otorga puntos) como devoluciones (descuenta
     * puntos)
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

            // Sebastian - Detectar si es una devolución (REFUND)
            boolean esDevolucion = ticket.getTicketType() == TicketInfo.RECEIPT_REFUND;

            if (esDevolucion) {
                // Para devoluciones, usar el valor absoluto y descontar puntos
                double montoAbsoluto = Math.abs(totalAcumulable);

                if (montoAbsoluto > 0 && ticket.getTicketStatus() > 0) {
                    // ticket.getTicketStatus() contiene el ID del ticket original en devoluciones
                    String ticketIdOriginal = String.valueOf(ticket.getTicketStatus());
                    String clienteId = cliente.getId();

                    System.out.println("🔄 DEVOLUCIÓN DETECTADA - Ticket original: #" + ticketIdOriginal +
                            ", Cliente: " + clienteId + ", Monto: $" + montoAbsoluto);

                    try {
                        // Descontar puntos del ticket original
                        PuntosDataLogic.ResultadoDescuento resultado = puntosDataLogic
                                .descontarPuntosPorCancelacion(ticketIdOriginal, clienteId, montoAbsoluto);
                        System.out.println("✅ Puntos descontados por devolución exitosamente");

                        // Actualizar vista de puntos del cliente
                        actualizarVistaPuntosCliente(clienteId);

                        // Mostrar mensaje de confirmación de devolución
                        String mensajeDevolucion;
                        if (resultado.seDescontaronPuntos()) {
                            mensajeDevolucion = String.format(
                                    "<html><center><h3>✅ Devolución Procesada</h3>" +
                                            "<p><b>Ticket Original:</b> #%s</p>" +
                                            "<p><b>Cliente:</b> %s</p>" +
                                            "<p><b>Puntos Descontados:</b> %d</p>" +
                                            "<p><b>Puntos Anteriores:</b> %d → <b>Puntos Actuales:</b> %d</p>" +
                                            "<p><b>Monto:</b> $%.2f</p></center></html>",
                                    ticketIdOriginal,
                                    cliente.getName() != null ? cliente.getName() : clienteId,
                                    resultado.getPuntosDescontados(),
                                    resultado.getPuntosAnteriores(),
                                    resultado.getPuntosActuales(),
                                    montoAbsoluto);
                        } else {
                            mensajeDevolucion = String.format(
                                    "<html><center><h3>✅ Devolución Procesada</h3>" +
                                            "<p><b>Ticket Original:</b> #%s</p>" +
                                            "<p><b>Cliente:</b> %s</p>" +
                                            "<p>No se encontraron puntos para descontar</p>" +
                                            "<p><b>Monto:</b> $%.2f</p></center></html>",
                                    ticketIdOriginal,
                                    cliente.getName() != null ? cliente.getName() : clienteId,
                                    montoAbsoluto);
                        }

                        javax.swing.JOptionPane.showMessageDialog(
                                this,
                                mensajeDevolucion,
                                "Devolución Completada",
                                javax.swing.JOptionPane.INFORMATION_MESSAGE);

                    } catch (Exception ex) {
                        System.err.println("❌ ERROR descontando puntos por devolución: " + ex.getMessage());
                        ex.printStackTrace();
                        LOGGER.log(System.Logger.Level.ERROR, "Error descontando puntos por devolución: ", ex);
                    }
                }
                return; // Salir temprano para devoluciones
            }

            // Para ventas normales, continuar con la lógica de otorgar puntos
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

            // Crear descripción de la transacción
            String descripcion = String.format("Venta automática #%d - Total acumulable: $%.2f %s",
                    ticket.getTicketId(),
                    totalAcumulable,
                    config.getMoneda());

            // IMPORTANTE: SIEMPRE llamar a agregarPuntosPorCompra, incluso si
            // puntosAOtorgar <= 0
            // porque este método maneja el acumulable diario y puede otorgar puntos cuando
            // el acumulable total del día alcanza el umbral
            String clienteId = cliente.getId();
            System.out.println(
                    "🔄 LLAMANDO agregarPuntosPorCompra - Cliente: " + clienteId + ", Monto: $" + totalAcumulable);
            try {
                puntosDataLogic.agregarPuntosPorCompra(clienteId, totalAcumulable, descripcion);
                System.out.println("✅ agregarPuntosPorCompra completado exitosamente");

                // Actualizar vista de puntos del cliente
                actualizarVistaPuntosCliente(clienteId);

            } catch (Exception ex) {
                System.err.println("❌ ERROR en agregarPuntosPorCompra: " + ex.getMessage());
                ex.printStackTrace();
                LOGGER.log(System.Logger.Level.ERROR, "Error otorgando puntos: ", ex);
            }

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
     * Sebastian - Inicializa la barra de pestañas con un ticket inicial
     */
    private void initializeTabsBar() {
        // Si no hay tickets, crear uno inicial
        if (ventasActivas.isEmpty()) {
            TicketInfo ticketInicial = new TicketInfo();
            ventasActivas.add(ticketInicial);
            ventaActualIndex = 0;
            setActiveTicket(ticketInicial, null);
        }
        updateTabsBar();
    }

    /**
     * Sebastian - Actualiza la barra de pestañas con los tickets activos
     */
    private void updateTabsBar() {
        if (m_jTabsPanel == null)
            return;

        m_jTabsPanel.removeAll();

        // Agregar pestaña para cada ticket activo
        for (int i = 0; i < ventasActivas.size(); i++) {
            final int index = i;
            TicketInfo ticket = ventasActivas.get(i);
            boolean esActivo = (i == ventaActualIndex);

            javax.swing.JButton tabButton = new javax.swing.JButton("Ticket " + (i + 1));
            tabButton.setFont(new java.awt.Font("Arial", esActivo ? java.awt.Font.BOLD : java.awt.Font.PLAIN, 11));
            tabButton.setFocusPainted(false);
            tabButton.setBorderPainted(false);
            tabButton.setContentAreaFilled(true);
            tabButton.setPreferredSize(new java.awt.Dimension(100, 30));
            tabButton.setMaximumSize(new java.awt.Dimension(100, 30));
            tabButton.setMinimumSize(new java.awt.Dimension(80, 30));

            // Estilo diferente para la pestaña activa
            if (esActivo) {
                tabButton.setBackground(new java.awt.Color(255, 255, 255));
                tabButton.setForeground(new java.awt.Color(0, 102, 204));
                tabButton.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createMatteBorder(2, 1, 0, 1, new java.awt.Color(0, 102, 204)),
                        javax.swing.BorderFactory.createEmptyBorder(5, 10, 5, 10)));
            } else {
                tabButton.setBackground(new java.awt.Color(240, 240, 240));
                tabButton.setForeground(new java.awt.Color(100, 100, 100));
                tabButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 10, 5, 10));
            }

            tabButton.addActionListener(e -> {
                if (index != ventaActualIndex) {
                    // Guardar el ticket actual antes de cambiar
                    if (m_oTicket != null && m_oTicket.getLinesCount() > 0) {
                        boolean yaExiste = false;
                        for (int j = 0; j < ventasActivas.size(); j++) {
                            if (ventasActivas.get(j) == m_oTicket) {
                                yaExiste = true;
                                ventaActualIndex = j;
                                break;
                            }
                        }
                        if (!yaExiste && ventaActualIndex >= 0 && ventaActualIndex < ventasActivas.size()) {
                            ventasActivas.set(ventaActualIndex, m_oTicket);
                        }
                    }

                    ventaActualIndex = index;
                    setActiveTicket(ventasActivas.get(index), null);
                    updateTabsBar(); // Actualizar para resaltar la pestaña activa
                }
            });

            m_jTabsPanel.add(tabButton);
        }

        // Botón + para agregar nueva pestaña
        javax.swing.JButton addTabButton = new javax.swing.JButton("+");
        addTabButton.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        addTabButton.setFocusPainted(false);
        addTabButton.setBorderPainted(true);
        addTabButton.setContentAreaFilled(true);
        addTabButton.setPreferredSize(new java.awt.Dimension(35, 30));
        addTabButton.setBackground(java.awt.Color.WHITE);
        addTabButton.setForeground(java.awt.Color.BLACK);
        addTabButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 10, 5, 10));
        addTabButton.addActionListener(e -> {
            abrirNuevaVenta();
            updateTabsBar();
        });

        m_jTabsPanel.add(addTabButton);
        m_jTabsPanel.revalidate();
        m_jTabsPanel.repaint();

        // Sebastian - Actualizar el indicador de ticket (VENTA - Ticket X)
        updateTicketIndicator();
    }

    /**
     * Sebastian - Método para actualizar el indicador de ticket
     */
    private void updateTicketIndicator() {
        if (m_jTicketIndicator != null) {
            int ticketNumber = ventaActualIndex + 1;
            // Si no hay tickets activos o el índice es inválido, usar 1
            if (ventasActivas.isEmpty() || ventaActualIndex < 0 || ventaActualIndex >= ventasActivas.size()) {
                ticketNumber = 1;
            }
            String ticketText = "VENTA - Ticket " + ticketNumber;
            m_jTicketIndicator.setText(ticketText);
            m_jTicketIndicator.revalidate();
            m_jTicketIndicator.repaint();
        }
    }

    /**
     * Sebastian - Método para abrir nueva venta (nueva pestaña)
     */
    private void abrirNuevaVenta() {
        try {
            // Guardar la venta actual en la lista (siempre, incluso si está vacía)
            if (m_oTicket != null) {
                // Verificar si ya existe en la lista
                boolean yaExiste = false;
                for (int i = 0; i < ventasActivas.size(); i++) {
                    if (ventasActivas.get(i) == m_oTicket) {
                        yaExiste = true;
                        ventaActualIndex = i;
                        break;
                    }
                }
                // Si no existe, agregarlo en la posición actual o al final
                if (!yaExiste) {
                    if (ventaActualIndex >= 0 && ventaActualIndex < ventasActivas.size()) {
                        ventasActivas.set(ventaActualIndex, m_oTicket);
                    } else {
                        ventasActivas.add(m_oTicket);
                        ventaActualIndex = ventasActivas.size() - 1;
                    }
                }
            }

            // Crear un nuevo ticket vacío
            TicketInfo nuevoTicket = new TicketInfo();
            ventasActivas.add(nuevoTicket);
            ventaActualIndex = ventasActivas.size() - 1;

            // Establecer el nuevo ticket como activo
            setActiveTicket(nuevoTicket, null);

            // Actualizar la barra de pestañas
            updateTabsBar();

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
     * Sebastian - Método para mostrar modal de ID cliente con tabla de clientes y
     * buscador
     */
    private void mostrarModalIdCliente() {
        try {
            // Crear diálogo
            javax.swing.JDialog dialog = new javax.swing.JDialog(
                    (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this),
                    "Seleccionar Cliente",
                    true);
            dialog.setSize(700, 500);
            dialog.setLocationRelativeTo(this);

            // Panel principal
            javax.swing.JPanel mainPanel = new javax.swing.JPanel(new java.awt.BorderLayout(10, 10));
            mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Panel de búsqueda
            javax.swing.JPanel searchPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
            javax.swing.JLabel lblSearch = new javax.swing.JLabel("Buscar por Nombre o ID:");
            javax.swing.JTextField txtSearch = new javax.swing.JTextField(20);
            txtSearch.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            searchPanel.add(lblSearch);
            searchPanel.add(txtSearch);

            // Obtener lista de clientes
            java.util.List<CustomerInfo> allCustomers = dlCustomers.getCustomerList().list();

            // Modelo de tabla
            javax.swing.table.DefaultTableModel tableModel = new javax.swing.table.DefaultTableModel(
                    new Object[] { "ID", "Nombre", "SearchKey" }, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            // Llenar tabla con clientes
            for (CustomerInfo customer : allCustomers) {
                tableModel.addRow(new Object[] {
                        customer.getId(),
                        customer.getName() != null ? customer.getName() : "",
                        customer.getSearchkey() != null ? customer.getSearchkey() : ""
                });
            }

            // Tabla de clientes
            javax.swing.JTable table = new javax.swing.JTable(tableModel);
            table.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            table.setRowHeight(25);
            table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            table.getTableHeader().setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            table.getColumnModel().getColumn(0).setPreferredWidth(200);
            table.getColumnModel().getColumn(1).setPreferredWidth(300);
            table.getColumnModel().getColumn(2).setPreferredWidth(150);

            // Scroll pane para la tabla
            javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(table);
            scrollPane.setPreferredSize(new java.awt.Dimension(680, 350));

            // Filtro de búsqueda - busca por nombre (columna 1) y SearchKey (columna 2)
            txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyReleased(java.awt.event.KeyEvent e) {
                    String searchText = txtSearch.getText().trim();
                    javax.swing.table.TableRowSorter<javax.swing.table.TableModel> currentSorter = (javax.swing.table.TableRowSorter<javax.swing.table.TableModel>) table
                            .getRowSorter();

                    if (searchText.isEmpty()) {
                        currentSorter.setRowFilter(null);
                    } else {
                        // Crear un filtro que busque en las columnas 1 (Nombre) y 2 (SearchKey)
                        java.util.List<javax.swing.RowFilter<javax.swing.table.TableModel, Integer>> filters = new java.util.ArrayList<>();

                        // Filtrar por nombre (columna 1)
                        filters.add(javax.swing.RowFilter
                                .regexFilter("(?i)" + java.util.regex.Pattern.quote(searchText), 1));

                        // Filtrar por SearchKey (columna 2)
                        filters.add(javax.swing.RowFilter
                                .regexFilter("(?i)" + java.util.regex.Pattern.quote(searchText), 2));

                        // Combinar filtros con OR (cualquiera de los dos)
                        javax.swing.RowFilter<javax.swing.table.TableModel, Integer> combinedFilter = javax.swing.RowFilter
                                .orFilter(filters);

                        currentSorter.setRowFilter(combinedFilter);
                    }
                }
            });

            // Inicializar sorter
            javax.swing.table.TableRowSorter<javax.swing.table.TableModel> sorter = new javax.swing.table.TableRowSorter<>(
                    tableModel);
            table.setRowSorter(sorter);

            // Panel de botones
            javax.swing.JPanel buttonPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
            javax.swing.JButton btnSelect = new javax.swing.JButton("Seleccionar");
            javax.swing.JButton btnCancel = new javax.swing.JButton("Cancelar");
            btnSelect.setPreferredSize(new java.awt.Dimension(100, 30));
            btnCancel.setPreferredSize(new java.awt.Dimension(100, 30));
            buttonPanel.add(btnSelect);
            buttonPanel.add(btnCancel);

            // Acción de selección desde tabla (doble clic)
            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int selectedRow = table.getSelectedRow();
                        if (selectedRow >= 0) {
                            int modelRow = table.convertRowIndexToModel(selectedRow);
                            String searchkey = (String) tableModel.getValueAt(modelRow, 2);
                            if (searchkey != null && !searchkey.trim().isEmpty()) {
                                asignarClienteDesdeDialogo(searchkey, dialog);
                            }
                        }
                    }
                }
            });

            // Acción del botón Seleccionar
            btnSelect.addActionListener(e -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    int modelRow = table.convertRowIndexToModel(selectedRow);
                    String searchkey = (String) tableModel.getValueAt(modelRow, 2);
                    if (searchkey != null && !searchkey.trim().isEmpty()) {
                        asignarClienteDesdeDialogo(searchkey, dialog);
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(dialog,
                                "Por favor seleccione un cliente de la tabla.",
                                "Selección Requerida",
                                javax.swing.JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    javax.swing.JOptionPane.showMessageDialog(dialog,
                            "Por favor seleccione un cliente de la tabla.",
                            "Selección Requerida",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                }
            });

            // Acción del botón Cancelar
            btnCancel.addActionListener(e -> dialog.dispose());

            // Acción de Enter en el campo de búsqueda (buscar y seleccionar si hay un solo
            // resultado)
            txtSearch.addActionListener(e -> {
                String searchText = txtSearch.getText().trim();
                if (!searchText.isEmpty() && table.getRowCount() == 1) {
                    // Si hay un solo resultado después del filtro, seleccionarlo
                    table.setRowSelectionInterval(0, 0);
                    int modelRow = table.convertRowIndexToModel(0);
                    String searchkey = (String) tableModel.getValueAt(modelRow, 2);
                    if (searchkey != null && !searchkey.trim().isEmpty()) {
                        asignarClienteDesdeDialogo(searchkey, dialog);
                    }
                } else if (!searchText.isEmpty()) {
                    // Si hay múltiples resultados, solo seleccionar el primero
                    if (table.getRowCount() > 0) {
                        table.setRowSelectionInterval(0, 0);
                    }
                }
            });

            // Agregar componentes al panel principal
            mainPanel.add(searchPanel, java.awt.BorderLayout.NORTH);
            mainPanel.add(scrollPane, java.awt.BorderLayout.CENTER);
            mainPanel.add(buttonPanel, java.awt.BorderLayout.SOUTH);

            dialog.add(mainPanel);
            dialog.setVisible(true);

        } catch (Exception e) {
            System.err.println("Error al mostrar diálogo de clientes: " + e.getMessage());
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al cargar clientes: " + e.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Método auxiliar para asignar cliente desde el diálogo
     */
    private void asignarClienteDesdeDialogo(String searchkey, javax.swing.JDialog dialog) {
        try {
            // Cerrar el diálogo
            dialog.dispose();

            // Usar el método existente para procesar el ID del cliente
            m_jCustomerId.setText(searchkey.trim());

            // Buscar el cliente usando la lógica existente
            String customerId = searchkey.trim();
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
                // Cliente encontrado - ejecutar la lógica completa
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
        } catch (Exception e) {
            System.err.println("Error al asignar cliente: " + e.getMessage());
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al asignar cliente: " + e.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Sebastian - Muestra el diálogo de Ventas del día y Devoluciones
     */
    private void mostrarVentasDelDiaYDevoluciones() {
        try {
            // Crear diálogo
            javax.swing.JDialog dialog = new javax.swing.JDialog(
                    (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this),
                    "Ventas del día y Devoluciones",
                    true);
            dialog.setSize(1200, 700);
            dialog.setLocationRelativeTo(this);

            // Panel principal con BorderLayout
            javax.swing.JPanel mainPanel = new javax.swing.JPanel(new java.awt.BorderLayout(10, 10));
            mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // === PANEL IZQUIERDO: Lista de tickets ===
            javax.swing.JPanel leftPanel = new javax.swing.JPanel(new java.awt.BorderLayout(5, 5));
            leftPanel.setPreferredSize(new java.awt.Dimension(500, 0));

            // Título "VENTAS DEL DIA"
            javax.swing.JLabel lblTitulo = new javax.swing.JLabel("VENTAS DEL DIA");
            lblTitulo.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));
            lblTitulo.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 10, 0));
            leftPanel.add(lblTitulo, java.awt.BorderLayout.NORTH);

            // Panel de búsqueda
            javax.swing.JPanel searchPanel = new javax.swing.JPanel(new java.awt.BorderLayout(5, 5));
            javax.swing.JLabel lblSearch = new javax.swing.JLabel("Puedes buscar por folio o nombre del ticket:");
            lblSearch.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            javax.swing.JTextField txtSearch = new javax.swing.JTextField();
            txtSearch.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            searchPanel.add(lblSearch, java.awt.BorderLayout.NORTH);
            searchPanel.add(txtSearch, java.awt.BorderLayout.CENTER);
            leftPanel.add(searchPanel, java.awt.BorderLayout.NORTH);

            // Tabla de tickets
            javax.swing.table.DefaultTableModel ticketsTableModel = new javax.swing.table.DefaultTableModel(
                    new Object[] { "Folio", "Arts", "Hora", "Total" }, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            javax.swing.JTable ticketsTable = new javax.swing.JTable(ticketsTableModel);
            ticketsTable.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            ticketsTable.setRowHeight(25);
            ticketsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            ticketsTable.getTableHeader().setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            javax.swing.JScrollPane ticketsScroll = new javax.swing.JScrollPane(ticketsTable);
            leftPanel.add(ticketsScroll, java.awt.BorderLayout.CENTER);

            // Panel de filtros
            javax.swing.JPanel filtersPanel = new javax.swing.JPanel();
            filtersPanel.setLayout(new java.awt.GridBagLayout());
            java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
            gbc.insets = new java.awt.Insets(5, 5, 5, 5);
            gbc.anchor = java.awt.GridBagConstraints.WEST;

            // Filtro de fecha
            gbc.gridx = 0;
            gbc.gridy = 0;
            javax.swing.JLabel lblFecha = new javax.swing.JLabel("Del día:");
            lblFecha.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            filtersPanel.add(lblFecha, gbc);

            gbc.gridx = 1;
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy",
                    java.util.Locale.forLanguageTag("es-MX"));
            javax.swing.JLabel lblFechaValor = new javax.swing.JLabel(dateFormat.format(new java.util.Date()));
            lblFechaValor.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            filtersPanel.add(lblFechaValor, gbc);

            gbc.gridx = 2;
            javax.swing.JButton btnHoy = new javax.swing.JButton("Hoy");
            btnHoy.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
            btnHoy.setPreferredSize(new java.awt.Dimension(60, 25));
            filtersPanel.add(btnHoy, gbc);

            // Filtro de cajero
            gbc.gridx = 0;
            gbc.gridy = 1;
            javax.swing.JLabel lblCajero = new javax.swing.JLabel("Cajero:");
            lblCajero.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            filtersPanel.add(lblCajero, gbc);

            gbc.gridx = 1;
            gbc.gridwidth = 2;
            javax.swing.JLabel lblCajeroValor = new javax.swing.JLabel(m_App.getAppUserView().getUser().getName());
            lblCajeroValor.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            filtersPanel.add(lblCajeroValor, gbc);

            // Checkbox Ventas a Credito
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 3;
            javax.swing.JCheckBox chkVentasCredito = new javax.swing.JCheckBox("Ventas a Credito");
            chkVentasCredito.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            filtersPanel.add(chkVentasCredito, gbc);

            leftPanel.add(filtersPanel, java.awt.BorderLayout.SOUTH);

            // === PANEL DERECHO: Detalles del ticket ===
            javax.swing.JPanel rightPanel = new javax.swing.JPanel(new java.awt.BorderLayout(5, 5));
            rightPanel.setPreferredSize(new java.awt.Dimension(600, 0));
            rightPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Ticket 3(1)"));

            // Panel de información del ticket
            javax.swing.JPanel ticketInfoPanel = new javax.swing.JPanel();
            ticketInfoPanel.setLayout(new java.awt.GridBagLayout());
            java.awt.GridBagConstraints gbcInfo = new java.awt.GridBagConstraints();
            gbcInfo.insets = new java.awt.Insets(5, 5, 5, 5);
            gbcInfo.anchor = java.awt.GridBagConstraints.WEST;

            javax.swing.JLabel lblFolio = new javax.swing.JLabel("Folio:");
            javax.swing.JLabel lblFolioValor = new javax.swing.JLabel("-");
            javax.swing.JLabel lblCajeroDet = new javax.swing.JLabel("Cajero:");
            javax.swing.JLabel lblCajeroDetValor = new javax.swing.JLabel("-");
            javax.swing.JLabel lblCliente = new javax.swing.JLabel("Cliente:");
            javax.swing.JLabel lblClienteValor = new javax.swing.JLabel("-");
            javax.swing.JLabel lblFechaDet = new javax.swing.JLabel("-");

            gbcInfo.gridx = 0;
            gbcInfo.gridy = 0;
            ticketInfoPanel.add(lblFolio, gbcInfo);
            gbcInfo.gridx = 1;
            ticketInfoPanel.add(lblFolioValor, gbcInfo);
            gbcInfo.gridx = 0;
            gbcInfo.gridy = 1;
            ticketInfoPanel.add(lblCajeroDet, gbcInfo);
            gbcInfo.gridx = 1;
            ticketInfoPanel.add(lblCajeroDetValor, gbcInfo);
            gbcInfo.gridx = 0;
            gbcInfo.gridy = 2;
            ticketInfoPanel.add(lblCliente, gbcInfo);
            gbcInfo.gridx = 1;
            ticketInfoPanel.add(lblClienteValor, gbcInfo);
            gbcInfo.gridx = 0;
            gbcInfo.gridy = 3;
            gbcInfo.gridwidth = 2;
            ticketInfoPanel.add(lblFechaDet, gbcInfo);

            rightPanel.add(ticketInfoPanel, java.awt.BorderLayout.NORTH);

            // Tabla de items del ticket
            javax.swing.table.DefaultTableModel itemsTableModel = new javax.swing.table.DefaultTableModel(
                    new Object[] { "Cant.", "Descripción", "Importe" }, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            javax.swing.JTable itemsTable = new javax.swing.JTable(itemsTableModel);
            itemsTable.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            itemsTable.setRowHeight(25);
            itemsTable.getTableHeader().setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            javax.swing.JScrollPane itemsScroll = new javax.swing.JScrollPane(itemsTable);
            rightPanel.add(itemsScroll, java.awt.BorderLayout.CENTER);

            // Panel de totales y botones
            javax.swing.JPanel totalsPanel = new javax.swing.JPanel(new java.awt.BorderLayout(5, 5));

            javax.swing.JPanel totalsInfoPanel = new javax.swing.JPanel();
            totalsInfoPanel.setLayout(new java.awt.GridBagLayout());
            java.awt.GridBagConstraints gbcTotals = new java.awt.GridBagConstraints();
            gbcTotals.insets = new java.awt.Insets(5, 5, 5, 5);
            gbcTotals.anchor = java.awt.GridBagConstraints.WEST;

            javax.swing.JLabel lblTotal = new javax.swing.JLabel("Total:");
            lblTotal.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            javax.swing.JLabel lblTotalValor = new javax.swing.JLabel("$0.00");
            lblTotalValor.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            javax.swing.JLabel lblPagoCon = new javax.swing.JLabel("Pago Con:");
            javax.swing.JLabel lblPagoConValor = new javax.swing.JLabel("$0.00");

            gbcTotals.gridx = 0;
            gbcTotals.gridy = 0;
            totalsInfoPanel.add(lblTotal, gbcTotals);
            gbcTotals.gridx = 1;
            totalsInfoPanel.add(lblTotalValor, gbcTotals);
            gbcTotals.gridx = 0;
            gbcTotals.gridy = 1;
            totalsInfoPanel.add(lblPagoCon, gbcTotals);
            gbcTotals.gridx = 1;
            totalsInfoPanel.add(lblPagoConValor, gbcTotals);

            totalsPanel.add(totalsInfoPanel, java.awt.BorderLayout.NORTH);

            // Botones de acción
            javax.swing.JPanel buttonsPanel = new javax.swing.JPanel(
                    new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 5));
            javax.swing.JButton btnDevolver = new javax.swing.JButton("Devolver Artículo seleccionado");
            btnDevolver.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
            btnDevolver.setEnabled(false);
            javax.swing.JButton btnCancelar = new javax.swing.JButton("Cancelar Venta");
            btnCancelar.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
            btnCancelar.setEnabled(false);
            javax.swing.JButton btnFacturar = new javax.swing.JButton("Facturar...");
            btnFacturar.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
            btnFacturar.setEnabled(false);
            javax.swing.JButton btnImprimir = new javax.swing.JButton("Imprimir copia");
            btnImprimir.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
            btnImprimir.setEnabled(false);

            buttonsPanel.add(btnDevolver);
            buttonsPanel.add(btnCancelar);
            buttonsPanel.add(btnFacturar);
            buttonsPanel.add(btnImprimir);

            totalsPanel.add(buttonsPanel, java.awt.BorderLayout.SOUTH);
            rightPanel.add(totalsPanel, java.awt.BorderLayout.SOUTH);

            // Agregar paneles al panel principal
            mainPanel.add(leftPanel, java.awt.BorderLayout.WEST);
            mainPanel.add(rightPanel, java.awt.BorderLayout.CENTER);

            dialog.add(mainPanel);

            // === FUNCIONALIDAD ===
            // Variable para el checkbox (debe ser final para usar en la clase anónima)
            final javax.swing.JCheckBox finalChkVentasCredito = chkVentasCredito;

            // Cargar tickets del día
            ListProviderCreator<FindTicketsInfo> lpr = new ListProviderCreator<FindTicketsInfo>(
                    dlSales.getTicketsList(), new EditorCreator() {
                        @Override
                        public Object createValue() throws BasicException {
                            Object[] afilter = new Object[14];

                            // Filtrar por fecha del día actual
                            Calendar today = Calendar.getInstance();
                            today.set(Calendar.HOUR_OF_DAY, 0);
                            today.set(Calendar.MINUTE, 0);
                            today.set(Calendar.SECOND, 0);
                            today.set(Calendar.MILLISECOND, 0);
                            Date startDate = today.getTime();

                            Calendar tomorrow = Calendar.getInstance();
                            tomorrow.set(Calendar.HOUR_OF_DAY, 0);
                            tomorrow.set(Calendar.MINUTE, 0);
                            tomorrow.set(Calendar.SECOND, 0);
                            tomorrow.set(Calendar.MILLISECOND, 0);
                            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
                            Date endDate = tomorrow.getTime();

                            afilter[0] = QBFCompareEnum.COMP_NONE; // TicketID
                            afilter[1] = null;
                            afilter[2] = QBFCompareEnum.COMP_DISTINCT; // TicketType (excluir devoluciones si checkbox
                                                                       // no está marcado)
                            afilter[3] = finalChkVentasCredito.isSelected() ? null : 2;
                            afilter[4] = QBFCompareEnum.COMP_NONE; // Money
                            afilter[5] = null;
                            afilter[6] = QBFCompareEnum.COMP_GREATEROREQUALS; // StartDate
                            afilter[7] = startDate;
                            afilter[8] = QBFCompareEnum.COMP_LESS; // EndDate
                            afilter[9] = endDate;
                            afilter[10] = QBFCompareEnum.COMP_NONE; // User
                            afilter[11] = null;
                            afilter[12] = QBFCompareEnum.COMP_NONE; // Customer
                            afilter[13] = null;

                            return afilter;
                        }
                    });

            // Función para cargar tickets
            java.util.function.Consumer<Void> cargarTickets = (v) -> {
                try {
                    java.util.List<FindTicketsInfo> tickets = lpr.loadData();
                    ticketsTableModel.setRowCount(0);

                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a",
                            java.util.Locale.forLanguageTag("es-MX"));

                    for (FindTicketsInfo ticket : tickets) {
                        // Contar artículos del ticket
                        int articlesCount = 0;
                        try {
                            TicketInfo ticketInfo = dlSales.loadTicket(ticket.getTicketType(), ticket.getTicketId());
                            if (ticketInfo != null) {
                                articlesCount = ticketInfo.getLinesCount();
                            }
                        } catch (Exception e) {
                            // Si no se puede cargar, usar 0
                        }

                        ticketsTableModel.addRow(new Object[] {
                                ticket.getTicketId(),
                                articlesCount,
                                timeFormat.format(ticket.getDate()),
                                Formats.CURRENCY.formatValue(ticket.getTotal())
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };

            // Cargar tickets inicialmente
            cargarTickets.accept(null);

            // Variable para almacenar el ticket seleccionado
            final java.util.concurrent.atomic.AtomicReference<TicketInfo> selectedTicketRef = new java.util.concurrent.atomic.AtomicReference<>();

            // Listener para selección de ticket
            ticketsTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = ticketsTable.getSelectedRow();
                    if (selectedRow >= 0) {
                        int folio = (Integer) ticketsTableModel.getValueAt(selectedRow, 0);
                        // Buscar el ticket (puede ser tipo 0 o 1)
                        TicketInfo ticketInfo = null;
                        try {
                            ticketInfo = dlSales.loadTicket(0, folio);
                        } catch (Exception ex) {
                            try {
                                ticketInfo = dlSales.loadTicket(1, folio);
                            } catch (Exception ex2) {
                                // No encontrado
                            }
                        }

                        if (ticketInfo != null) {
                            // Guardar referencia al ticket seleccionado
                            selectedTicketRef.set(ticketInfo);

                            // Actualizar información del ticket
                            lblFolioValor.setText(String.valueOf(ticketInfo.getTicketId()));
                            lblCajeroDetValor
                                    .setText(ticketInfo.getUser() != null ? ticketInfo.getUser().getName() : "-");
                            lblClienteValor
                                    .setText(ticketInfo.getCustomer() != null ? ticketInfo.getCustomer().getName()
                                            : "Al contado");
                            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd 'de' MMMM yyyy h:mm a",
                                    java.util.Locale.forLanguageTag("es-MX"));
                            lblFechaDet.setText(dateTimeFormat.format(ticketInfo.getDate()));

                            // Actualizar tabla de items
                            itemsTableModel.setRowCount(0);
                            for (int i = 0; i < ticketInfo.getLinesCount(); i++) {
                                TicketLineInfo line = ticketInfo.getLine(i);
                                itemsTableModel.addRow(new Object[] {
                                        Formats.DOUBLE.formatValue(line.getMultiply()),
                                        line.getProductName(),
                                        Formats.CURRENCY.formatValue(line.getSubValue())
                                });
                            }

                            // Actualizar totales
                            lblTotalValor.setText(Formats.CURRENCY.formatValue(ticketInfo.getTotal()));
                            lblPagoConValor.setText(Formats.CURRENCY.formatValue(ticketInfo.getTotalPaid()));

                            // Habilitar botones (pero btnDevolver solo si hay un item seleccionado)
                            btnDevolver.setEnabled(false);
                            btnCancelar.setEnabled(true);
                            btnFacturar.setEnabled(true);
                            btnImprimir.setEnabled(true);
                        } else {
                            selectedTicketRef.set(null);
                            btnDevolver.setEnabled(false);
                        }
                    }
                }
            });

            // Listener para selección de artículo en la tabla de items
            itemsTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = itemsTable.getSelectedRow();
                    btnDevolver.setEnabled(selectedRow >= 0 && selectedTicketRef.get() != null);
                }
            });

            // Listener para búsqueda
            txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyReleased(java.awt.event.KeyEvent e) {
                    String searchText = txtSearch.getText().toLowerCase();
                    if (searchText.isEmpty()) {
                        ticketsTable.setRowSorter(null);
                    } else {
                        javax.swing.table.TableRowSorter<javax.swing.table.TableModel> sorter = new javax.swing.table.TableRowSorter<>(
                                ticketsTableModel);
                        sorter.setRowFilter(javax.swing.RowFilter
                                .regexFilter("(?i)" + java.util.regex.Pattern.quote(searchText), 0));
                        ticketsTable.setRowSorter(sorter);
                    }
                }
            });

            // Listener para checkbox
            chkVentasCredito.addActionListener(e -> cargarTickets.accept(null));

            // Listener para botón Hoy
            btnHoy.addActionListener(e -> {
                lblFechaValor.setText(dateFormat.format(new java.util.Date()));
                cargarTickets.accept(null);
            });

            // Listener para botón Devolver Artículo seleccionado
            btnDevolver.addActionListener(e -> {
                try {
                    TicketInfo originalTicket = selectedTicketRef.get();
                    int selectedItemRow = itemsTable.getSelectedRow();

                    if (originalTicket == null) {
                        javax.swing.JOptionPane.showMessageDialog(dialog,
                                "Por favor seleccione un ticket",
                                "Error",
                                javax.swing.JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    if (selectedItemRow < 0) {
                        javax.swing.JOptionPane.showMessageDialog(dialog,
                                "Por favor seleccione un artículo para devolver",
                                "Error",
                                javax.swing.JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // Obtener la línea del ticket original
                    TicketLineInfo originalLine = originalTicket.getLine(selectedItemRow);

                    // Calcular monto acumulable del artículo a devolver
                    double montoAcumulable = 0.0;
                    if (originalLine.isProductAccumulatesPoints()) {
                        montoAcumulable = Math.abs(originalLine.getValue());
                    }

                    // Crear un nuevo ticket de devolución
                    TicketInfo refundTicket = new TicketInfo();
                    refundTicket.setTicketType(TicketInfo.RECEIPT_REFUND);
                    refundTicket.setTicketStatus(originalTicket.getTicketId());
                    refundTicket.setCustomer(originalTicket.getCustomer());
                    refundTicket.setUser(m_App.getAppUserView().getUser().getUserInfo());
                    refundTicket.setActiveCash(m_App.getActiveCashIndex());
                    refundTicket.setDate(new java.util.Date());
                    refundTicket.setOldTicket(true);

                    // Crear una nueva línea con cantidad negativa para la devolución
                    TicketLineInfo refundLine = new TicketLineInfo(originalLine);
                    refundLine.setMultiply(-originalLine.getMultiply());

                    // Agregar la línea al ticket de devolución
                    refundTicket.addLine(refundLine);

                    // Cerrar el diálogo
                    dialog.dispose();

                    // Activar el ticket de devolución en el panel principal
                    setActiveTicket(refundTicket, null);

                    // Nota: El mensaje de confirmación se mostrará cuando se guarde el ticket de
                    // devolución
                    // en procesarPuntosAutomaticos()

                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(dialog,
                            "Error al procesar la devolución: " + ex.getMessage(),
                            "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            });

            // Listener para botón Cancelar Venta
            btnCancelar.addActionListener(e -> {
                try {
                    TicketInfo ticketACancelar = selectedTicketRef.get();

                    if (ticketACancelar == null) {
                        javax.swing.JOptionPane.showMessageDialog(dialog,
                                "Por favor seleccione un ticket para cancelar",
                                "Error",
                                javax.swing.JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // Confirmar cancelación
                    int confirmacion = javax.swing.JOptionPane.showOptionDialog(
                            dialog,
                            String.format(
                                    "<html><center><h3>¿Cancelar esta venta?</h3>" +
                                            "<p><b>Ticket:</b> #%d</p>" +
                                            "<p><b>Cliente:</b> %s</p>" +
                                            "<p><b>Total:</b> %s</p>" +
                                            "<p>Esta acción no se puede deshacer.</p></center></html>",
                                    ticketACancelar.getTicketId(),
                                    ticketACancelar.getCustomer() != null ? ticketACancelar.getCustomer().getName()
                                            : "Al contado",
                                    Formats.CURRENCY.formatValue(ticketACancelar.getTotal())),
                            "Confirmar Cancelación",
                            javax.swing.JOptionPane.YES_NO_OPTION,
                            javax.swing.JOptionPane.WARNING_MESSAGE,
                            null,
                            new Object[] { "Sí", "No" },
                            "No");

                    if (confirmacion != javax.swing.JOptionPane.YES_OPTION) {
                        return;
                    }

                    // Calcular monto acumulable antes de eliminar
                    String clienteId = ticketACancelar.getCustomer() != null ? ticketACancelar.getCustomer().getId()
                            : null;
                    String nombreCliente = ticketACancelar.getCustomer() != null
                            && ticketACancelar.getCustomer().getName() != null
                                    ? ticketACancelar.getCustomer().getName()
                                    : clienteId;
                    int ticketId = ticketACancelar.getTicketId();

                    double totalAcumulable = 0.0;
                    for (int i = 0; i < ticketACancelar.getLinesCount(); i++) {
                        TicketLineInfo line = ticketACancelar.getLine(i);
                        if (line.isProductAccumulatesPoints()) {
                            totalAcumulable += line.getValue();
                        }
                    }

                    // Descontar puntos antes de eliminar el ticket
                    PuntosDataLogic.ResultadoDescuento resultadoCancelacion = null;
                    if (clienteId != null && ticketId > 0 && puntosDataLogic != null) {
                        try {
                            resultadoCancelacion = puntosDataLogic.descontarPuntosPorCancelacion(
                                    String.valueOf(ticketId),
                                    clienteId,
                                    totalAcumulable);
                        } catch (Exception ex) {
                            LOGGER.log(System.Logger.Level.WARNING, "Error descontando puntos: " + ex.getMessage());
                        }
                    }

                    // Eliminar el ticket
                    try {
                        dlSales.deleteTicket(ticketACancelar, m_App.getInventoryLocation());

                        // Actualizar vista de puntos
                        if (clienteId != null) {
                            actualizarVistaPuntosCliente(clienteId);
                        }

                        // Mostrar mensaje de confirmación
                        if (resultadoCancelacion != null && clienteId != null) {
                            String mensajeCancelacion;
                            if (resultadoCancelacion.seDescontaronPuntos()) {
                                mensajeCancelacion = String.format(
                                        "<html><center><h3>✅ Venta Cancelada</h3>" +
                                                "<p><b>Ticket:</b> #%d</p>" +
                                                "<p><b>Cliente:</b> %s</p>" +
                                                "<p><b>Puntos Descontados:</b> %d</p>" +
                                                "<p><b>Puntos Anteriores:</b> %d → <b>Puntos Actuales:</b> %d</p></center></html>",
                                        ticketId,
                                        nombreCliente,
                                        resultadoCancelacion.getPuntosDescontados(),
                                        resultadoCancelacion.getPuntosAnteriores(),
                                        resultadoCancelacion.getPuntosActuales());
                            } else {
                                mensajeCancelacion = String.format(
                                        "<html><center><h3>✅ Venta Cancelada</h3>" +
                                                "<p><b>Ticket:</b> #%d</p>" +
                                                "<p><b>Cliente:</b> %s</p>" +
                                                "<p>No se encontraron puntos para descontar</p></center></html>",
                                        ticketId,
                                        nombreCliente);
                            }

                            javax.swing.JOptionPane.showMessageDialog(
                                    dialog,
                                    mensajeCancelacion,
                                    "Cancelación Completada",
                                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            // Mensaje simple si no hay cliente o puntos
                            javax.swing.JOptionPane.showMessageDialog(
                                    dialog,
                                    String.format(
                                            "<html><center><h3>✅ Venta Cancelada</h3>" +
                                                    "<p><b>Ticket:</b> #%d</p></center></html>",
                                            ticketId),
                                    "Cancelación Completada",
                                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
                        }

                        // Recargar la lista de tickets
                        cargarTickets.accept(null);
                        selectedTicketRef.set(null);

                        // Limpiar la vista de detalles
                        lblFolioValor.setText("-");
                        lblCajeroDetValor.setText("-");
                        lblClienteValor.setText("-");
                        lblFechaDet.setText("-");
                        itemsTableModel.setRowCount(0);
                        lblTotalValor.setText(Formats.CURRENCY.formatValue(0.0));
                        lblPagoConValor.setText(Formats.CURRENCY.formatValue(0.0));
                        btnCancelar.setEnabled(false);
                        btnDevolver.setEnabled(false);
                        btnFacturar.setEnabled(false);
                        btnImprimir.setEnabled(false);

                    } catch (Exception ex) {
                        javax.swing.JOptionPane.showMessageDialog(dialog,
                                "Error al cancelar el ticket: " + ex.getMessage(),
                                "Error",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }

                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(dialog,
                            "Error al procesar la cancelación: " + ex.getMessage(),
                            "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            });

            // Listener para botón Imprimir copia
            btnImprimir.addActionListener(e -> {
                // #region agent log
                System.out.println("DEBUG: Button Imprimir copia clicked at " + System.currentTimeMillis());
                try {
                    String logPath = "c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log";
                    java.io.FileWriter fw = new java.io.FileWriter(logPath, true);
                    fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_btn\",\"timestamp\":"
                            + System.currentTimeMillis()
                            + ",\"location\":\"JPanelTicket.java:6582\",\"message\":\"Button Imprimir copia clicked\",\"data\":{\"action\":\"click\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                    fw.close();
                    System.out.println("DEBUG: Log written to " + logPath);
                } catch (Exception ex) {
                    System.out.println("DEBUG: Error writing log: " + ex.getMessage());
                    ex.printStackTrace();
                }
                // #endregion
                try {
                    TicketInfo ticketAImprimir = selectedTicketRef.get();

                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter(
                                "c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log",
                                true);
                        fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_ticket\",\"timestamp\":"
                                + System.currentTimeMillis()
                                + ",\"location\":\"JPanelTicket.java:6490\",\"message\":\"Ticket selected for printing\",\"data\":{\"ticketId\":"
                                + (ticketAImprimir != null ? ticketAImprimir.getTicketId() : "null")
                                + ",\"ticketName\":\"" + (ticketAImprimir != null ? ticketAImprimir.getName() : "null")
                                + "\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                        fw.close();
                    } catch (IOException ex) {
                    }
                    // #endregion

                    if (ticketAImprimir == null) {
                        javax.swing.JOptionPane.showMessageDialog(dialog,
                                "Por favor seleccione un ticket para imprimir",
                                "Error",
                                javax.swing.JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // Imprimir la copia del ticket usando Printer.Ticket2
                    // Asegurar que el template esté actualizado antes de imprimir
                    actualizarTemplateTicket2EnBD();

                    try {
                        // #region agent log
                        try {
                            java.io.FileWriter fw = new java.io.FileWriter(
                                    "c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log",
                                    true);
                            fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_before\",\"timestamp\":"
                                    + System.currentTimeMillis()
                                    + ",\"location\":\"JPanelTicket.java:6610\",\"message\":\"Before printTicket call\",\"data\":{\"resource\":\"Printer.Ticket2\",\"ticketId\":"
                                    + ticketAImprimir.getTicketId()
                                    + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                            fw.close();
                        } catch (IOException ex) {
                        }
                        // #endregion
                        printTicket("Printer.Ticket2", ticketAImprimir, null);
                        // #region agent log
                        try {
                            java.io.FileWriter fw = new java.io.FileWriter(
                                    "c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log",
                                    true);
                            fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_after\",\"timestamp\":"
                                    + System.currentTimeMillis()
                                    + ",\"location\":\"JPanelTicket.java:6502\",\"message\":\"After printTicket call\",\"data\":{\"resource\":\"Printer.Ticket2\",\"status\":\"success\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                            fw.close();
                        } catch (IOException ex) {
                        }
                        // #endregion
                        Notify(AppLocal.getIntString("notify.printing"));
                        javax.swing.JOptionPane.showMessageDialog(dialog,
                                "Copia del ticket impresa correctamente",
                                "Impresión",
                                javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        // #region agent log
                        try {
                            java.io.FileWriter fw = new java.io.FileWriter(
                                    "c:\\Users\\Usuario\\Documents\\proyecto inicio cursor\\punto-mx\\.cursor\\debug.log",
                                    true);
                            String errorMsg = ex.getMessage() != null
                                    ? ex.getMessage().replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
                                    : "null";
                            fw.write("{\"id\":\"log_" + System.currentTimeMillis() + "_error\",\"timestamp\":"
                                    + System.currentTimeMillis()
                                    + ",\"location\":\"JPanelTicket.java:6632\",\"message\":\"Exception in printTicket\",\"data\":{\"error\":\""
                                    + errorMsg + "\",\"class\":\"" + ex.getClass().getName()
                                    + "\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                            fw.close();
                        } catch (IOException ex2) {
                        }
                        // #endregion
                        LOGGER.log(System.Logger.Level.ERROR, "Exception on printTicket: Printer.Ticket2", ex);
                        javax.swing.JOptionPane.showMessageDialog(dialog,
                                "Error al imprimir la copia: " + ex.getMessage(),
                                "Error",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(dialog,
                            "Error al procesar la impresión: " + ex.getMessage(),
                            "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            });

            dialog.setVisible(true);

        } catch (Exception e) {
            System.err.println("Error al mostrar ventas del día: " + e.getMessage());
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al cargar ventas del día: " + e.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Muestra un diálogo para registrar entradas y salidas de efectivo
     */
    private void showEntradasSalidasDialog() {
        showEntradasSalidasDialog(null);
    }

    /**
     * Muestra un diálogo para registrar entradas o salidas de efectivo
     * 
     * @param tipoFijo Si es "Entrada" o "Salida", se usa ese tipo y no se muestra
     *                 el selector
     */
    private void showEntradasSalidasDialog(String tipoFijo) {
        String titulo = tipoFijo != null ? tipoFijo + "s" : "Entradas y Salidas";
        javax.swing.JDialog dialog = new javax.swing.JDialog(
                (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this), titulo, true);
        dialog.setSize(350, tipoFijo != null ? 220 : 250);
        dialog.setLocationRelativeTo(this);

        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        gbc.anchor = java.awt.GridBagConstraints.WEST;

        int rowIndex = 0;

        // Tipo (Entrada/Salida) - Solo se muestra si tipoFijo es null
        javax.swing.JComboBox<String> cmbTipo = null;
        if (tipoFijo == null) {
            gbc.gridx = 0;
            gbc.gridy = rowIndex;
            panel.add(new javax.swing.JLabel("Tipo:"), gbc);
            gbc.gridx = 1;
            gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            cmbTipo = new javax.swing.JComboBox<>(new String[] { "Entrada", "Salida" });
            cmbTipo.setPreferredSize(new java.awt.Dimension(200, 25));
            panel.add(cmbTipo, gbc);
            rowIndex++;
        }

        // Monto
        gbc.gridx = 0;
        gbc.gridy = rowIndex;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new javax.swing.JLabel("Monto:"), gbc);
        gbc.gridx = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        javax.swing.JTextField txtMonto = new javax.swing.JTextField();
        txtMonto.setPreferredSize(new java.awt.Dimension(200, 25));
        panel.add(txtMonto, gbc);
        rowIndex++;

        // Hacer que el campo de monto tenga el foco inicial
        txtMonto.requestFocusInWindow();

        // Notas
        gbc.gridx = 0;
        gbc.gridy = rowIndex;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new javax.swing.JLabel("Notas:"), gbc);
        gbc.gridx = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        javax.swing.JTextField txtNotas = new javax.swing.JTextField();
        txtNotas.setPreferredSize(new java.awt.Dimension(200, 25));
        panel.add(txtNotas, gbc);
        rowIndex++;

        // Botones
        javax.swing.JPanel btnPanel = new javax.swing.JPanel(new java.awt.FlowLayout());
        javax.swing.JButton btnAceptar = new javax.swing.JButton("Aceptar");
        javax.swing.JButton btnCancelar = new javax.swing.JButton("Cancelar");
        btnPanel.add(btnAceptar);
        btnPanel.add(btnCancelar);

        gbc.gridx = 0;
        gbc.gridy = rowIndex;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.anchor = java.awt.GridBagConstraints.CENTER;
        panel.add(btnPanel, gbc);

        dialog.add(panel);

        btnCancelar.addActionListener(e -> dialog.dispose());

        // Guardar referencia final para uso en lambda
        final javax.swing.JComboBox<String> cmbTipoFinal = cmbTipo;
        final String tipoFijoFinal = tipoFijo;

        // Método auxiliar para guardar la entrada/salida
        java.awt.event.ActionListener guardarEntradaSalida = e -> {
            try {
                String tipo;
                if (tipoFijoFinal != null) {
                    tipo = tipoFijoFinal;
                } else {
                    tipo = cmbTipoFinal != null ? (String) cmbTipoFinal.getSelectedItem() : "Entrada";
                }
                String montoStr = txtMonto.getText().trim();
                String notas = txtNotas.getText().trim();

                if (montoStr.isEmpty()) {
                    javax.swing.JOptionPane.showMessageDialog(dialog,
                            "Por favor ingrese un monto",
                            "Error",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                    txtMonto.requestFocusInWindow();
                    return;
                }

                // Aceptar tanto coma como punto como separador decimal (formato mexicano)
                // Reemplazar coma por punto para el parseo
                montoStr = montoStr.replace(',', '.');
                // Remover espacios y caracteres no numéricos excepto punto y signo negativo
                montoStr = montoStr.replaceAll("[^0-9.\\-]", "");

                double monto;
                try {
                    monto = Double.parseDouble(montoStr);
                } catch (NumberFormatException nfe) {
                    javax.swing.JOptionPane.showMessageDialog(dialog,
                            "El monto debe ser un número válido (ejemplo: 67,00 o 67.00)",
                            "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                    txtMonto.requestFocusInWindow();
                    return;
                }
                if (monto <= 0) {
                    javax.swing.JOptionPane.showMessageDialog(dialog,
                            "El monto debe ser mayor a cero",
                            "Error",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                    txtMonto.requestFocusInWindow();
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

                dialog.dispose();
            } catch (NumberFormatException ex) {
                javax.swing.JOptionPane.showMessageDialog(dialog,
                        "El monto debe ser un número válido (ejemplo: 67,00 o 67.00)",
                        "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                txtMonto.requestFocusInWindow();
            } catch (com.openbravo.basic.BasicException ex) {
                LOGGER.log(System.Logger.Level.ERROR, "Error al guardar entrada/salida: " + ex.getMessage(), ex);
                javax.swing.JOptionPane.showMessageDialog(dialog,
                        "Error al guardar: " + ex.getMessage(),
                        "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                txtMonto.requestFocusInWindow();
            }
        };

        // Agregar ActionListener al campo de monto para detectar Enter
        txtMonto.addActionListener(guardarEntradaSalida);

        // Agregar ActionListener al botón Aceptar
        btnAceptar.addActionListener(guardarEntradaSalida);

        dialog.setVisible(true);
    }

    /**
     * Muestra un diálogo para registrar solo entradas de efectivo
     */
    private void showEntradasDialog() {
        showEntradasSalidasDialog("Entrada");
    }

    /**
     * Muestra un diálogo para registrar solo salidas de efectivo
     */
    private void showSalidasDialog() {
        showEntradasSalidasDialog("Salida");
    }

    /**
     * Aplica un descuento de mayoreo a las líneas del ticket
     * Muestra un diálogo para ingresar el porcentaje de descuento y lo aplica a
     * todas las líneas
     */
    private void aplicarDescuentoMayoreo() {
        // Verificar que haya un ticket activo
        if (m_oTicket == null || m_oTicket.getLinesCount() == 0) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "No hay productos en el ticket para aplicar descuento de mayoreo",
                    "Mayoreo",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Crear diálogo personalizado para pedir el porcentaje de descuento
        javax.swing.JDialog dialog = new javax.swing.JDialog(
                (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this),
                "Descuento de Mayoreo",
                true);
        dialog.setSize(350, 180);
        dialog.setLocationRelativeTo(this);

        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(10, 10, 10, 10);
        gbc.anchor = java.awt.GridBagConstraints.WEST;

        // Etiqueta
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(new javax.swing.JLabel("Ingrese el porcentaje de descuento a aplicar:"), gbc);

        // Campo de texto para el porcentaje
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new javax.swing.JLabel("Porcentaje (%):"), gbc);

        gbc.gridx = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        javax.swing.JTextField txtPorcentaje = new javax.swing.JTextField();
        txtPorcentaje.setPreferredSize(new java.awt.Dimension(200, 30));
        txtPorcentaje.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        panel.add(txtPorcentaje, gbc);

        // Botones
        javax.swing.JPanel btnPanel = new javax.swing.JPanel(new java.awt.FlowLayout());
        javax.swing.JButton btnAceptar = new javax.swing.JButton("Aceptar");
        javax.swing.JButton btnCancelar = new javax.swing.JButton("Cancelar");
        btnPanel.add(btnAceptar);
        btnPanel.add(btnCancelar);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.anchor = java.awt.GridBagConstraints.CENTER;
        panel.add(btnPanel, gbc);

        dialog.add(panel);

        // Hacer que el campo de texto tenga foco y seleccione todo al mostrar
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                txtPorcentaje.requestFocus();
                txtPorcentaje.selectAll();
            }
        });

        // Variable para almacenar el resultado
        final java.util.concurrent.atomic.AtomicReference<Double> resultado = new java.util.concurrent.atomic.AtomicReference<>(
                null);

        btnCancelar.addActionListener(e -> dialog.dispose());

        btnAceptar.addActionListener(e -> {
            try {
                String texto = txtPorcentaje.getText().trim();
                if (texto.isEmpty()) {
                    javax.swing.JOptionPane.showMessageDialog(dialog,
                            "Por favor ingrese un porcentaje",
                            "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double porcentaje = Double.parseDouble(texto);

                // Validar que el descuento esté entre 0 y 100
                if (porcentaje < 0 || porcentaje > 100) {
                    javax.swing.JOptionPane.showMessageDialog(dialog,
                            "El porcentaje debe estar entre 0 y 100",
                            "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                    return;
                }

                resultado.set(porcentaje);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                javax.swing.JOptionPane.showMessageDialog(dialog,
                        "Por favor ingrese un número válido",
                        "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });

        // Permitir Enter en el campo de texto
        txtPorcentaje.addActionListener(e -> btnAceptar.doClick());

        dialog.setVisible(true);

        // Obtener el resultado
        Double porcentajeDescuento = resultado.get();

        // Si el usuario canceló o no ingresó un valor válido
        if (porcentajeDescuento == null || porcentajeDescuento <= 0) {
            return;
        }

        // Aplicar descuento a todas las líneas del ticket
        int lineasModificadas = 0;
        double factorDescuento = 1.0 - (porcentajeDescuento / 100.0);

        for (int i = 0; i < m_oTicket.getLinesCount(); i++) {
            TicketLineInfo linea = m_oTicket.getLine(i);

            // Solo aplicar descuento a líneas de productos (no a descuentos o ajustes)
            if (linea.getProductID() != null && !linea.getProductID().equals("0000") && linea.getPrice() > 0) {
                double precioOriginal = linea.getPrice();
                double nuevoPrecio = precioOriginal * factorDescuento;

                // Crear nueva línea copiando la original
                TicketLineInfo nuevaLinea = new TicketLineInfo(linea);

                // Actualizar el precio
                nuevaLinea.setPrice(nuevoPrecio);

                // Actualizar el nombre del producto para indicar el descuento
                String nombreOriginal = linea.getProductName();
                if (nombreOriginal != null && !nombreOriginal.isEmpty()) {
                    // Verificar si ya tiene un descuento aplicado para no duplicar
                    if (!nombreOriginal.contains("[Mayoreo")) {
                        String nombreConDescuento = nombreOriginal + " [Mayoreo -" +
                                Formats.PERCENT.formatValue(porcentajeDescuento / 100.0) + "]";
                        nuevaLinea.getProperties().setProperty("product.name", nombreConDescuento);
                    }
                }

                // Actualizar la línea en el ticket
                m_oTicket.setLine(i, nuevaLinea);
                m_ticketlines.setTicketLine(i, nuevaLinea);
                lineasModificadas++;
            }
        }

        // Actualizar la vista del ticket
        refreshTicket();
        printPartialTotals();

        // Mostrar mensaje de confirmación
        if (lineasModificadas > 0) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Se aplicó un descuento del " + Formats.PERCENT.formatValue(porcentajeDescuento / 100.0) +
                            " a " + lineasModificadas + " línea(s) del ticket",
                    "Descuento de Mayoreo Aplicado",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
        } else {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "No se pudo aplicar el descuento. Verifique que haya productos válidos en el ticket.",
                    "Advertencia",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
        }
    }

    private void editLineUnits() {
        int index = m_ticketlines.getSelectedIndex();
        if (index >= 0) {
            TicketLineInfo line = m_oTicket.getLine(index);
            JDialogUnits dialog = JDialogUnits.getDialog(this, line);
            dialog.setVisible(true);

            if (dialog.isOK()) {
                m_oTicket.getLine(index).setMultiply(dialog.getUnits());
                // Refresh the line in UI
                m_ticketlines.setTicketLine(index, m_oTicket.getLine(index));
                m_ticketlines.setSelectedIndex(index);

                // Recalculate totals
                printPartialTotals();
                countArticles();
                try {
                    updateCustomerPointsDisplay();
                } catch (Exception e) {
                }
            }
        } else {
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }
}
