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

import com.openbravo.pos.menu.JRootMenu;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.JMessageDialog;
import com.openbravo.data.gui.MessageInf;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 *
 * @author adrianromero
 */
public class JPrincipalApp extends JPanel implements AppUserView {

    private static final Logger LOGGER = Logger.getLogger(JPrincipalApp.class.getName());
    private static final long serialVersionUID = 1L;

    private final JRootApp m_appview;
    private final AppUser m_appuser;
    private final DataLogicSystem m_dlSystem;
    private final JLabel m_principalnotificator;

    private Icon menu_open;
    private Icon menu_close;

    private final JRootMenu rMenu;

    // Referencias a botones principales para atajos de teclado
    private javax.swing.JButton btnVentasRef;
    private javax.swing.JButton btnCierreRef;
    private javax.swing.JButton btnInventarioRef;
    private javax.swing.JButton btnReportesRef;
    
    // Referencia al panel de perfil en el panel superior
    private javax.swing.JPanel profilePanelRef;

    /**
     * Creates a JPanel
     *
     * @param appview
     * @param appuser
     */
    public JPrincipalApp(JRootApp appview, AppUser appuser) {

        m_appview = appview;
        m_appuser = appuser;

        m_dlSystem = (DataLogicSystem) m_appview.getBean("com.openbravo.pos.forms.DataLogicSystem");

        // IMPORTANTE: Inicializar roles predeterminados ANTES de cargar permisos del
        // usuario
        try {
            System.out.println("=== INICIALIZANDO ROLES AL INICIO DE LA APP ===");
            com.openbravo.pos.admin.DataLogicAdmin dlAdmin = (com.openbravo.pos.admin.DataLogicAdmin) m_appview
                    .getBean("com.openbravo.pos.admin.DataLogicAdmin");
            com.openbravo.pos.admin.DefaultRolesInitializer.initializeDefaultRoles(dlAdmin.getSession());
            System.out.println("=== ROLES INICIALIZADOS CORRECTAMENTE ===");
        } catch (Exception ex) {
            System.err.println("ERROR al inicializar roles: " + ex.getMessage());
            ex.printStackTrace();
        }

        AppUserPermissionsLoader aupLoader = new AppUserPermissionsLoader(m_dlSystem);

        // Convertir ID de rol a nombre de rol para compatibilidad
        String roleName = mapRoleIdToName(m_appuser.getRole());
        System.out.println(
                "Usuario: " + m_appuser.getName() + " | Rol ID: " + m_appuser.getRole() + " | Rol Nombre: " + roleName);

        Set<String> userPermissions = aupLoader.getPermissionsForRole(roleName);

        // Sebastian - TEMPORAL: Agregar permiso de gráficos manualmente hasta que se
        // arregle el BLOB
        if ("ADMIN".equals(roleName) || "1".equals(m_appuser.getRole())) {
            userPermissions.add("com.openbravo.pos.reports.JPanelGraphics");
            System.out.println("✓ Permiso de Gráficos agregado manualmente al admin");
        }

        m_appuser.fillPermissions(userPermissions);

        initComponents();
        applyComponentOrientation(m_appview.getComponentOrientation());

        m_principalnotificator = new JLabel();
        m_principalnotificator.applyComponentOrientation(getComponentOrientation());
        m_principalnotificator.setText(m_appuser.getName());
        // Sebastian - Sin icono en el perfil, solo texto
        m_principalnotificator.setIcon(null);
        
        // Sebastian - Configurar estilo del perfil para el panel superior
        m_principalnotificator.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 13));
        m_principalnotificator.setForeground(new java.awt.Color(50, 50, 70));
        m_principalnotificator.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        
        // Agregar el perfil al panel superior (se inicializa en initComponents)
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (profilePanelRef != null) {
                profilePanelRef.removeAll();
                profilePanelRef.add(m_principalnotificator);
                profilePanelRef.revalidate();
                profilePanelRef.repaint();
            }
        });

        // MENU SIDE
        colapseHPanel.add(Box.createVerticalStrut(50), 0);
        m_jPanelMenu.getVerticalScrollBar().setPreferredSize(new Dimension(35, 35));
        rMenu = new JRootMenu(this, this);
        rMenu.setRootMenu(m_jPanelMenu, m_dlSystem);
        setMenuIcon();
        assignMenuButtonIcon();

        // MAIN
        m_jPanelTitle.setVisible(false);
        m_jPanelTitle.setPreferredSize(new java.awt.Dimension(0, 0)); // Sin tamaño cuando está oculto
        m_jPanelTitle.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 0)); // Sin altura cuando está oculto
        addView(new JPanel(), "<NULL>");
        showView("<NULL>");
        
        // Configurar atajos de teclado globales después de inicializar todo
        setupGlobalKeyboardShortcuts();

    }

    private void setMenuIcon() {
        if (colapseButton.getComponentOrientation().isLeftToRight()) {
            menu_open = new ImageIcon(getClass().getResource(
                    "/com/openbravo/images/menu-right.png"));
            menu_close = new ImageIcon(getClass().getResource(
                    "/com/openbravo/images/menu-left.png"));
        } else {
            menu_open = new ImageIcon(getClass().getResource(
                    "/com/openbravo/images/menu-left.png"));
            menu_close = new ImageIcon(getClass().getResource(
                    "/com/openbravo/images/menu-right.png"));
        }
    }

    private void assignMenuButtonIcon() {
        colapseButton.setIcon(m_jPanelMenu.isVisible() ? menu_close : menu_open);
    }

    private void setMenuVisible(boolean value) {

        m_jPanelMenu.setVisible(value);
        assignMenuButtonIcon();
        revalidate();
    }

    public JComponent getNotificator() {
        return m_principalnotificator;
    }
    

    public void activate() {

        // Sebastian - Mantener el menú lateral siempre oculto para diseño tipo eleventa
        setMenuVisible(false);
        rMenu.getViewManager().resetActionfirst();
        
        // Sebastian - Refrescar el logo cuando se active el panel (por si cambió en configuración)
        try {
            // Buscar el logoPanel en el componente
            javax.swing.JPanel artisticPanel = findArtisticTopPanel(this);
            if (artisticPanel != null) {
                for (java.awt.Component comp : artisticPanel.getComponents()) {
                    if (comp instanceof javax.swing.JPanel) {
                        javax.swing.JPanel logoPanel = (javax.swing.JPanel) comp;
                        @SuppressWarnings("unchecked")
                        java.util.function.Consumer<String> updateLogo = (java.util.function.Consumer<String>) logoPanel.getClientProperty("updateLogo");
                        if (updateLogo != null) {
                            // Recargar la configuración y actualizar el logo
                            com.openbravo.pos.forms.AppConfig appConfig = com.openbravo.pos.forms.AppConfig.getInstance();
                            appConfig.load();
                            String logoPath = appConfig.getProperty("start.logo");
                            updateLogo.accept(logoPath);
                            logoPanel.revalidate();
                            logoPanel.repaint();
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al refrescar el logo", e);
        }
    }
    
    // Sebastian - Método helper para encontrar el artisticTopPanel
    private javax.swing.JPanel findArtisticTopPanel(java.awt.Container container) {
        for (java.awt.Component comp : container.getComponents()) {
            if (comp instanceof javax.swing.JPanel) {
                javax.swing.JPanel panel = (javax.swing.JPanel) comp;
                // Verificar si es el artisticTopPanel buscando el logoPanel dentro
                for (java.awt.Component child : panel.getComponents()) {
                    if (child instanceof javax.swing.JPanel) {
                        javax.swing.JPanel childPanel = (javax.swing.JPanel) child;
                        if (childPanel.getClientProperty("logoLabel") != null) {
                            return panel; // Este es el artisticTopPanel
                        }
                    }
                }
                // Buscar recursivamente
                javax.swing.JPanel found = findArtisticTopPanel(panel);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public boolean deactivate() {
        if (rMenu.getViewManager().deactivateLastView()) {
            showView("<NULL>");
            return true;
        } else {
            return false;
        }

    }

    @Override
    public void exitToLogin() {
        // Sebastian - Verificar si hay turno abierto - OBLIGATORIO cerrar el turno antes de salir
        if (m_appview.getActiveCashDateEnd() == null && m_appview.getActiveCashIndex() != null) {
            // Mostrar mensaje informativo
            int opcion = JOptionPane.showOptionDialog(
                    this,
                    "Tienes un turno abierto.\nDebes cerrar el turno antes de salir.",
                    "Cerrar Sesión",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    new Object[] { "Cerrar Turno", "Cancelar" },
                    "Cerrar Turno");

            if (opcion == JOptionPane.OK_OPTION) { // Cerrar Turno
                java.awt.Window parentWindow = SwingUtilities.getWindowAncestor(this);
                java.awt.Frame parentFrame = null;
                if (parentWindow instanceof java.awt.Frame) {
                    parentFrame = (java.awt.Frame) parentWindow;
                } else if (parentWindow instanceof java.awt.Dialog) {
                    parentFrame = (java.awt.Frame) ((java.awt.Dialog) parentWindow).getParent();
                }

                JDialogCloseShift dialog = new JDialogCloseShift(parentFrame, m_appview);
                dialog.setVisible(true);

                if (dialog.isClosed() && dialog.shouldCloseShift()) {
                    // El turno fue cerrado exitosamente, ahora permitir salir
                    m_appview.closeAppView();
                }
                // Si canceló el cierre del turno, no hacer nada (no permitir salir)
                return;
            }
            // Si canceló, no hacer nada (no permitir salir)
            return;
        }

        // No hay turno abierto, permitir salir normalmente
        m_appview.closeAppView();
    }

    private void addView(JComponent component, String sView) {
        m_jPanelContainer.add(component, sView);
    }

    private void showView(String sView) {
        CardLayout cl = (CardLayout) (m_jPanelContainer.getLayout());
        cl.show(m_jPanelContainer, sView);
    }

    @Override
    public AppUser getUser() {
        return m_appuser;
    }

    @Override
    public void showTask(String sTaskClass) {

        LOGGER.info("Show View for class: " + sTaskClass);
        try {
            m_appview.waitCursorBegin();

            if (m_appuser.hasPermission(sTaskClass)) {

                JPanelView viewPanel = rMenu.getViewManager().getCreatedViews().get(sTaskClass);
                if (viewPanel == null) {

                    viewPanel = rMenu.getViewManager().getPreparedViews().get(sTaskClass);

                    if (viewPanel == null) {

                        try {
                            viewPanel = (JPanelView) m_appview.getBean(sTaskClass);
                        } catch (BeanFactoryException e) {
                            LOGGER.log(Level.SEVERE, "Exception on get a JPanelView Bean for class: " + sTaskClass, e);
                            viewPanel = new JPanelNull(m_appview, e);
                        }
                    }

                    rMenu.getViewManager().getCreatedViews().put(sTaskClass, viewPanel);
                }

                if (!rMenu.getViewManager().checkIfLastView(viewPanel)) {

                    if (rMenu.getViewManager().getLastView() != null) {
                        LOGGER.info("Call 'deactivate' on class: "
                                + rMenu.getViewManager().getLastView().getClass().getName());
                        rMenu.getViewManager().getLastView().deactivate();
                    }

                    viewPanel.getComponent().applyComponentOrientation(getComponentOrientation());
                    addView(viewPanel.getComponent(), sTaskClass);

                    LOGGER.info("Call 'activate' on class: " + sTaskClass);
                    viewPanel.activate();

                    rMenu.getViewManager().setLastView(viewPanel);

                    // Sebastian - Mantener el menú lateral siempre oculto
                    setMenuVisible(false);

                    showView(sTaskClass);
                    String sTitle = viewPanel.getTitle();
                    if (sTitle != null && !sTitle.isBlank()) {
                        m_jPanelTitle.setVisible(true);
                        m_jPanelTitle.setPreferredSize(null); // Restaurar tamaño normal
                        m_jPanelTitle.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)); // Restaurar
                                                                                                                    // tamaño
                                                                                                                    // máximo
                        m_jTitle.setText(sTitle);
                    } else {
                        m_jPanelTitle.setVisible(false);
                        m_jPanelTitle.setPreferredSize(new java.awt.Dimension(0, 0)); // Sin tamaño cuando está oculto
                        m_jPanelTitle.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 0)); // Sin altura cuando
                                                                                                    // está oculto
                        m_jTitle.setText("");
                    }
                } else {
                    LOGGER.log(Level.INFO, "Already open: " + sTaskClass + ", Instance: " + viewPanel);
                }
            } else {

                LOGGER.log(Level.INFO, "NO PERMISSION on call class: : " + sTaskClass);
                JMessageDialog.showMessage(this,
                        new MessageInf(MessageInf.SGN_WARNING,
                                AppLocal.getIntString("message.notpermissions"), "<html>" + sTaskClass));
            }
            m_appview.waitCursorEnd();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception on show class: " + sTaskClass, e);
            JMessageDialog.showMessage(this,
                    new MessageInf(MessageInf.SGN_WARNING,
                            AppLocal.getIntString("message.notactive"), e));
        }
    }

    @Override
    public void executeTask(String sTaskClass) {

        m_appview.waitCursorBegin();

        if (m_appuser.hasPermission(sTaskClass)) {
            try {
                ProcessAction myProcess = (ProcessAction) m_appview.getBean(sTaskClass);

                try {
                    MessageInf m = myProcess.execute();
                    if (m != null) {
                        JMessageDialog.showMessage(JPrincipalApp.this, m);
                    }
                } catch (BasicException eb) {
                    JMessageDialog.showMessage(JPrincipalApp.this, new MessageInf(eb));
                }
            } catch (BeanFactoryException e) {
                JMessageDialog.showMessage(JPrincipalApp.this,
                        new MessageInf(MessageInf.SGN_WARNING,
                                AppLocal.getIntString("label.LoadError"), e));
            }
        } else {
            JMessageDialog.showMessage(JPrincipalApp.this,
                    new MessageInf(MessageInf.SGN_WARNING,
                            AppLocal.getIntString("message.notpermissions")));
        }
        m_appview.waitCursorEnd();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        m_jPanelLefSide = new javax.swing.JPanel();
        m_jPanelMenu = new javax.swing.JScrollPane();
        colapseHPanel = new javax.swing.JPanel();
        colapseButton = new javax.swing.JButton();
        m_jPanelRightSide = new javax.swing.JPanel();
        m_jPanelTitle = new javax.swing.JPanel();
        m_jTitle = new javax.swing.JLabel();
        m_jPanelContainer = new javax.swing.JPanel();

        setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        setLayout(new java.awt.BorderLayout());

        m_jPanelLefSide.setLayout(new java.awt.BorderLayout());

        m_jPanelMenu.setBackground(new java.awt.Color(102, 102, 102));
        m_jPanelMenu.setBorder(null);
        m_jPanelMenu.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jPanelMenu.setPreferredSize(new java.awt.Dimension(250, 2));
        m_jPanelLefSide.add(m_jPanelMenu, java.awt.BorderLayout.LINE_START);

        colapseHPanel.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        colapseHPanel.setPreferredSize(new java.awt.Dimension(45, 45));

        colapseButton.setToolTipText(AppLocal.getIntString("tooltip.menu")); // NOI18N
        colapseButton.setFocusPainted(false);
        colapseButton.setFocusable(false);
        colapseButton.setIconTextGap(0);
        colapseButton.setMargin(new java.awt.Insets(10, 2, 10, 2));
        colapseButton.setMaximumSize(new java.awt.Dimension(45, 32224661));
        colapseButton.setMinimumSize(new java.awt.Dimension(32, 32));
        colapseButton.setPreferredSize(new java.awt.Dimension(36, 45));
        colapseButton.setRequestFocusEnabled(false);
        colapseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colapseButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout colapseHPanelLayout = new javax.swing.GroupLayout(colapseHPanel);
        colapseHPanel.setLayout(colapseHPanelLayout);
        colapseHPanelLayout.setHorizontalGroup(
                colapseHPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, colapseHPanelLayout
                                .createSequentialGroup()
                                .addGap(0, 0, 0)
                                .addComponent(colapseButton, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap()));
        colapseHPanelLayout.setVerticalGroup(
                colapseHPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(colapseHPanelLayout.createSequentialGroup()
                                .addContainerGap(88, Short.MAX_VALUE)
                                .addComponent(colapseButton, javax.swing.GroupLayout.DEFAULT_SIZE, 33, Short.MAX_VALUE)
                                .addContainerGap(188, Short.MAX_VALUE)));

        m_jPanelLefSide.add(colapseHPanel, java.awt.BorderLayout.LINE_END);

        // Sebastian - Ocultar la barra lateral del menú para diseño tipo eleventa
        m_jPanelLefSide.setVisible(false);
        // add(m_jPanelLefSide, java.awt.BorderLayout.LINE_START);

        m_jPanelRightSide.setPreferredSize(new java.awt.Dimension(200, 40));
        m_jPanelRightSide.setLayout(new java.awt.BorderLayout());

        // Sebastian - Crear barra horizontal superior con TODOS los botones del menú
        // (estilo eleventa)
        // Panel contenedor principal con BorderLayout para mantener botón cerrar fijo
        javax.swing.JPanel topMenuBar = new javax.swing.JPanel(new java.awt.BorderLayout(5, 0));
        topMenuBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5)); // Reducir padding vertical (2px
                                                                                       // en lugar de 5px)
        topMenuBar.setBackground(new java.awt.Color(220, 220, 220)); // Gris suave
        topMenuBar.setMinimumSize(new java.awt.Dimension(0, 30)); // Altura mínima reducida (25px botón + padding)
        topMenuBar.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 30)); // Altura máxima fija para una sola
                                                                                  // línea

        // Panel izquierdo con todos los botones del menú
        javax.swing.JPanel leftMenuPanel = new javax.swing.JPanel();
        leftMenuPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 3, 2)); // Espaciado horizontal
                                                                                          // reducido para diseño
                                                                                          // compacto
        leftMenuPanel.setBackground(new java.awt.Color(220, 220, 220)); // Gris suave
        leftMenuPanel.setOpaque(false);

        // Panel derecho con puntos del cliente y botón cerrar
        javax.swing.JPanel rightPanel = new javax.swing.JPanel();
        rightPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 3, 2));
        rightPanel.setBackground(new java.awt.Color(220, 220, 220)); // Gris suave
        rightPanel.setOpaque(false);

        // ========== MENU.MAIN - Elementos principales ==========
        // Botón Ventas (Menu.Ticket) - Solo mostrar si tiene permiso
        if (m_appuser.hasPermission("com.openbravo.pos.sales.JPanelTicketSales")) {
            btnVentasRef = createMenuButton(
                    "/com/openbravo/images/sale.png",
                    "F1 " + AppLocal.getIntString("Menu.Ticket"),
                    "com.openbravo.pos.sales.JPanelTicketSales",
                    new java.awt.Color(52, 152, 219)); // Azul (igual que Cerrar Caja)
            leftMenuPanel.add(btnVentasRef);
        }

        // Botón Pagos de Clientes (Menu.CustomersPayment) - Solo mostrar si tiene permiso
        if (m_appuser.hasPermission("com.openbravo.pos.customers.CustomersPayment")) {
            javax.swing.JButton btnPagosClientes = createMenuButton(
                    "/com/openbravo/images/customerpay.png",
                    AppLocal.getIntString("Menu.CustomersPayment"),
                    "com.openbravo.pos.customers.CustomersPayment");
            leftMenuPanel.add(btnPagosClientes);
        }

        // Botón Cierre de Caja (Menu.CloseTPV) - Solo mostrar si tiene permiso
        if (m_appuser.hasPermission("com.openbravo.pos.panels.JPanelCloseMoney")) {
            btnCierreRef = createMenuButton(
                    "/com/openbravo/images/calculator.png",
                    "F2 " + AppLocal.getIntString("Menu.CloseTPV"),
                    "com.openbravo.pos.panels.JPanelCloseMoney",
                    new java.awt.Color(52, 152, 219)); // Azul atractivo
            leftMenuPanel.add(btnCierreRef);
        }

        // Botón Gestión de Sucursales eliminado de la barra superior - ahora está dentro de Mantenimiento

        // ========== MENU.BACKOFFICE - Submenús ==========
        // Botón Clientes (Menu.Customers - submenu) - Solo mostrar si tiene permiso
        if (m_appuser.hasPermission("com.openbravo.pos.forms.MenuCustomers")) {
            javax.swing.JButton btnClientes = createMenuButton(
                    "/com/openbravo/images/customer.png",
                    AppLocal.getIntString("Menu.Customers"),
                    "com.openbravo.pos.forms.MenuCustomers");
            leftMenuPanel.add(btnClientes);
        }

        // Botón Proveedores (Menu.Suppliers - submenu) - Solo mostrar si tiene permiso
        if (m_appuser.hasPermission("com.openbravo.pos.forms.MenuSuppliers")) {
            javax.swing.JButton btnProveedores = createMenuButton(
                    "/com/openbravo/images/stockmaint.png",
                    AppLocal.getIntString("Menu.Suppliers"),
                    "com.openbravo.pos.forms.MenuSuppliers");
            leftMenuPanel.add(btnProveedores);
        }

        // Botón Gestión de Inventario (Menu.StockManagement - submenu) - Solo mostrar si tiene permiso
        if (m_appuser.hasPermission("com.openbravo.pos.forms.MenuStockManagement")) {
            btnInventarioRef = createMenuButton(
                    "/com/openbravo/images/products.png",
                    "F3 " + AppLocal.getIntString("Menu.StockManagement"),
                    "com.openbravo.pos.forms.MenuStockManagement",
                    new java.awt.Color(52, 152, 219)); // Azul (igual que Cerrar Caja)
            leftMenuPanel.add(btnInventarioRef);
        }

        // Botón Gestión de Ventas (Menu.SalesManagement - submenu) - Solo mostrar si tiene permiso
        // Cambiar texto para distinguirlo del botón principal de Ventas
        if (m_appuser.hasPermission("com.openbravo.pos.forms.MenuSalesManagement")) {
            javax.swing.JButton btnVentasManagement = createMenuButton(
                    "/com/openbravo/images/sales.png",
                    "Gestión Ventas", // Texto más descriptivo para distinguirlo
                    "com.openbravo.pos.forms.MenuSalesManagement",
                    null); // Sin color especial
            leftMenuPanel.add(btnVentasManagement);
        }

        // Botón Mantenimiento (Menu.Maintenance - submenu) - Solo mostrar si tiene permiso
        if (m_appuser.hasPermission("com.openbravo.pos.forms.MenuMaintenance")) {
            javax.swing.JButton btnMantenimiento = createMenuButton(
                    "/com/openbravo/images/maintain.png",
                    AppLocal.getIntString("Menu.Maintenance"),
                    "com.openbravo.pos.forms.MenuMaintenance");
            leftMenuPanel.add(btnMantenimiento);
        }
        // Botón Herramientas eliminado de la barra superior - ahora está dentro de Mantenimiento
        // Botón Gestión de Presencia eliminado de la barra superior

        // ========== MENU.SYSTEM ==========
        // Botón Cambiar Contraseña movido a Configuración > General

        // Botón Configuración (Menu.Configuration) - Solo mostrar si tiene permiso
        if (m_appuser.hasPermission("com.openbravo.pos.config.JPanelConfiguration")) {
            javax.swing.JButton btnConfig = createMenuButton(
                    "/com/openbravo/images/configuration.png",
                    AppLocal.getIntString("Menu.Configuration"),
                    "com.openbravo.pos.config.JPanelConfiguration");
            leftMenuPanel.add(btnConfig);
        }

        // Botón Impresora (Menu.Printer) - Solo mostrar si tiene permiso
        if (m_appuser.hasPermission("com.openbravo.pos.panels.JPanelPrinter")) {
            javax.swing.JButton btnImpresora = createMenuButton(
                    "/com/openbravo/images/printer.png",
                    AppLocal.getIntString("Menu.Printer"),
                    "com.openbravo.pos.panels.JPanelPrinter");
            leftMenuPanel.add(btnImpresora);
        }

        // Botón Reportes (Menu.Reports) - Solo mostrar si tiene permiso
        if (m_appuser.hasPermission("com.openbravo.pos.reports.JPanelGraphics")) {
            btnReportesRef = createMenuButton(
                    null, // Sin icono
                    "F4 " + AppLocal.getIntString("Menu.Reports"),
                    "com.openbravo.pos.reports.JPanelGraphics",
                    new java.awt.Color(52, 152, 219)); // Azul (igual que Cerrar Caja)
            leftMenuPanel.add(btnReportesRef);
        }

        // Sebastian - Label de puntos del cliente (al lado del botón Salir)
        m_jCustomerPoints = new javax.swing.JLabel();
        m_jCustomerPoints.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
        m_jCustomerPoints.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        m_jCustomerPoints.setText("");
        m_jCustomerPoints.setToolTipText("Puntos del cliente");
        m_jCustomerPoints.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        m_jCustomerPoints.setOpaque(false); // Sin fondo
        m_jCustomerPoints.setPreferredSize(new java.awt.Dimension(300, 25));
        m_jCustomerPoints.setForeground(java.awt.Color.BLACK);
        m_jCustomerPoints.setVisible(false); // Inicialmente oculto
        rightPanel.add(m_jCustomerPoints);

        // Botón Salir (Menu.Exit) - Siempre fijo al final
        javax.swing.JButton btnSalir = new javax.swing.JButton();
        // Icono removido para diseño compacto
        btnSalir.setText(AppLocal.getIntString("Menu.Exit"));
        btnSalir.setPreferredSize(new java.awt.Dimension(70, 25));
        btnSalir.setMinimumSize(new java.awt.Dimension(60, 25));
        btnSalir.setMaximumSize(new java.awt.Dimension(90, 25));
        btnSalir.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 9));
        btnSalir.setBackground(java.awt.Color.WHITE);
        btnSalir.setForeground(java.awt.Color.BLACK);
        btnSalir.setOpaque(true);
        btnSalir.setFocusPainted(false);
        btnSalir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitToLogin();
            }
        });
        rightPanel.add(btnSalir);

        // Agregar paneles al topMenuBar principal
        topMenuBar.add(leftMenuPanel, java.awt.BorderLayout.WEST);
        topMenuBar.add(rightPanel, java.awt.BorderLayout.EAST);

        // Altura fija para una sola línea con botones compactos
        topMenuBar.setPreferredSize(new java.awt.Dimension(Integer.MAX_VALUE, 30)); // Altura fija para una línea

        m_jPanelTitle.setLayout(new java.awt.BorderLayout());
        m_jPanelTitle.setBorder(null); // Sin borde para eliminar espacio
        m_jPanelTitle.setPreferredSize(new java.awt.Dimension(0, 0)); // Sin tamaño cuando está oculto
        m_jPanelTitle.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 0)); // Sin altura cuando está oculto

        m_jTitle.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        m_jTitle.setForeground(new java.awt.Color(0, 168, 223));
        m_jTitle.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.darkGray),
                javax.swing.BorderFactory.createEmptyBorder(2, 10, 2, 10))); // Reducir padding vertical (2px en lugar
                                                                             // de 10px)
        m_jTitle.setMaximumSize(new java.awt.Dimension(100, 28)); // Reducir altura máxima aún más
        m_jTitle.setMinimumSize(new java.awt.Dimension(30, 24));
        m_jTitle.setPreferredSize(new java.awt.Dimension(100, 28)); // Reducir altura preferida (28px)
        m_jPanelTitle.add(m_jTitle, java.awt.BorderLayout.NORTH);

        // Sebastian - Panel artístico superior con fondo difuminado tipo Eleventa
        javax.swing.JPanel artisticTopPanel = new javax.swing.JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2d = (java.awt.Graphics2D) g.create();
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                
                int width = getWidth();
                int height = getHeight();
                
                // Gradiente base suave tipo Eleventa - mejorado con más tonos
                java.awt.GradientPaint baseGradient = new java.awt.GradientPaint(
                    0, 0, new java.awt.Color(250, 252, 255),
                    0, height, new java.awt.Color(238, 242, 247)
                );
                g2d.setPaint(baseGradient);
                g2d.fillRect(0, 0, width, height);
                
                // Efecto de luz difuminada superior izquierda (para logo) - más suave
                java.awt.RadialGradientPaint lightEffect1 = new java.awt.RadialGradientPaint(
                    150, 40, 250,
                    new float[]{0f, 0.5f, 0.8f, 1f},
                    new java.awt.Color[]{
                        new java.awt.Color(140, 195, 240, 50),
                        new java.awt.Color(140, 195, 240, 25),
                        new java.awt.Color(140, 195, 240, 10),
                        new java.awt.Color(140, 195, 240, 0)
                    }
                );
                g2d.setPaint(lightEffect1);
                g2d.fillOval(-50, -30, 500, 200);
                
                // Efecto de luz difuminada superior derecha - más suave
                java.awt.RadialGradientPaint lightEffect2 = new java.awt.RadialGradientPaint(
                    width - 150, 40, 220,
                    new float[]{0f, 0.6f, 0.9f, 1f},
                    new java.awt.Color[]{
                        new java.awt.Color(110, 170, 230, 35),
                        new java.awt.Color(110, 170, 230, 15),
                        new java.awt.Color(110, 170, 230, 5),
                        new java.awt.Color(110, 170, 230, 0)
                    }
                );
                g2d.setPaint(lightEffect2);
                g2d.fillOval(width - 440, -20, 440, 180);
                
                // Efecto adicional central para más profundidad
                java.awt.RadialGradientPaint lightEffect3 = new java.awt.RadialGradientPaint(
                    width / 2, height / 3, 300,
                    new float[]{0f, 0.7f, 1f},
                    new java.awt.Color[]{
                        new java.awt.Color(120, 180, 225, 20),
                        new java.awt.Color(120, 180, 225, 5),
                        new java.awt.Color(120, 180, 225, 0)
                    }
                );
                g2d.setPaint(lightEffect3);
                g2d.fillOval(width / 2 - 300, -50, 600, 200);
                
                // Línea sutil inferior con gradiente más suave
                java.awt.MultipleGradientPaint.CycleMethod cycleMethod = java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;
                java.awt.Color[] lineColors = {
                    new java.awt.Color(200, 210, 220, 80),
                    new java.awt.Color(220, 220, 220, 40),
                    new java.awt.Color(240, 240, 240, 0)
                };
                float[] lineFractions = {0.0f, 0.5f, 1.0f};
                java.awt.LinearGradientPaint lineGradient = new java.awt.LinearGradientPaint(
                    0, height - 1, width, height - 1,
                    lineFractions, lineColors, cycleMethod
                );
                g2d.setPaint(lineGradient);
                g2d.fillRect(0, height - 2, width, 2);
                
                // Sombra sutil superior para profundidad
                java.awt.GradientPaint shadowGradient = new java.awt.GradientPaint(
                    0, 0, new java.awt.Color(0, 0, 0, 5),
                    0, 10, new java.awt.Color(0, 0, 0, 0)
                );
                g2d.setPaint(shadowGradient);
                g2d.fillRect(0, 0, width, 10);
                
                g2d.dispose();
            }
        };
        artisticTopPanel.setLayout(new java.awt.BorderLayout());
        artisticTopPanel.setPreferredSize(new java.awt.Dimension(0, 80)); // Altura exacta de 80px
        artisticTopPanel.setOpaque(false);
        
        // Panel para el logo en la parte izquierda con medidas exactas
        javax.swing.JPanel logoPanel = new javax.swing.JPanel();
        logoPanel.setLayout(new java.awt.BorderLayout());
        logoPanel.setOpaque(false);
        logoPanel.setPreferredSize(new java.awt.Dimension(200, 80)); // Ancho exacto 200px, alto 80px
        logoPanel.setMaximumSize(new java.awt.Dimension(200, 80));
        logoPanel.setMinimumSize(new java.awt.Dimension(200, 80));
        logoPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Padding interno para centrar el logo
        
        // Sebastian - Cargar imagen del logo desde configuraciones (propiedad "start.logo")
        javax.swing.JLabel logoLabel = new javax.swing.JLabel();
        logoLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        logoLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        
        // Método para actualizar el logo
        java.util.function.Consumer<String> updateLogo = (logoPath) -> {
            try {
                logoLabel.setIcon(null); // Limpiar icono anterior
                logoLabel.setText(""); // Limpiar texto anterior
                
                if (logoPath != null && !logoPath.trim().isEmpty()) {
                    java.io.File logoFile = new java.io.File(logoPath);
                    LOGGER.log(Level.INFO, "Verificando archivo de logo: " + logoPath + " - Existe: " + logoFile.exists() + " - Es archivo: " + logoFile.isFile());
                    
                    if (logoFile.exists() && logoFile.isFile()) {
                        // Cargar la imagen del logo usando ImageIO para mejor manejo
                        try {
                            java.awt.Image originalImage = javax.imageio.ImageIO.read(logoFile);
                            
                            // Verificar que la imagen se cargó correctamente
                            if (originalImage != null) {
                                int originalWidth = originalImage.getWidth(null);
                                int originalHeight = originalImage.getHeight(null);
                                LOGGER.log(Level.INFO, "Imagen cargada: " + originalWidth + "x" + originalHeight);
                                
                                if (originalWidth > 0 && originalHeight > 0) {
                                    // Escalar la imagen para que quepa en el panel (máximo 180x60px manteniendo proporción)
                                    int maxWidth = 180;
                                    int maxHeight = 60;
                                    
                                    // Calcular dimensiones manteniendo proporción
                                    double widthRatio = (double) maxWidth / originalWidth;
                                    double heightRatio = (double) maxHeight / originalHeight;
                                    double ratio = Math.min(widthRatio, heightRatio);
                                    
                                    int scaledWidth = (int) (originalWidth * ratio);
                                    int scaledHeight = (int) (originalHeight * ratio);
                                    
                                    LOGGER.log(Level.INFO, "Escalando imagen a: " + scaledWidth + "x" + scaledHeight);
                                    
                                    // Escalar la imagen con mejor calidad usando Graphics2D
                                    java.awt.Image scaledImage = originalImage.getScaledInstance(
                                        scaledWidth, scaledHeight, java.awt.Image.SCALE_SMOOTH
                                    );
                                    
                                    // Usar BufferedImage para mejor renderizado
                                    java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(
                                        scaledWidth, scaledHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB
                                    );
                                    java.awt.Graphics2D g2d = bufferedImage.createGraphics();
                                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                                    g2d.drawImage(scaledImage, 0, 0, null);
                                    g2d.dispose();
                                    
                                    logoLabel.setIcon(new javax.swing.ImageIcon(bufferedImage));
                                    logoLabel.setText(""); // Sin texto, solo imagen
                                    LOGGER.log(Level.INFO, "✓ Logo cargado y mostrado exitosamente desde: " + logoPath);
                                    logoPanel.revalidate();
                                    logoPanel.repaint();
                                    return;
                                }
                            }
                        } catch (javax.imageio.IIOException e) {
                            LOGGER.log(Level.WARNING, "Error al leer imagen del logo (formato no soportado?): " + logoPath, e);
                        }
                    } else {
                        LOGGER.log(Level.WARNING, "El archivo de logo no existe o no es un archivo válido: " + logoPath);
                    }
                } else {
                    LOGGER.log(Level.INFO, "No hay ruta de logo configurada en 'start.logo'");
                }
                
                // Si no hay ruta válida o el archivo no existe, mostrar texto "LOGO"
                logoLabel.setIcon(null);
                logoLabel.setText("LOGO");
                logoLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24));
                logoLabel.setForeground(new java.awt.Color(100, 160, 220));
                logoPanel.revalidate();
                logoPanel.repaint();
            } catch (Exception e) {
                // En caso de error, mostrar texto "LOGO"
                logoLabel.setIcon(null);
                logoLabel.setText("LOGO");
                logoLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24));
                logoLabel.setForeground(new java.awt.Color(100, 160, 220));
                LOGGER.log(Level.WARNING, "Error al cargar el logo desde: " + logoPath, e);
                logoPanel.revalidate();
                logoPanel.repaint();
            }
        };
        
        // Cargar el logo inicialmente
        try {
            com.openbravo.pos.forms.AppConfig appConfig = com.openbravo.pos.forms.AppConfig.getInstance();
            appConfig.load();
            String logoPath = appConfig.getProperty("start.logo"); // Sebastian - Propiedad correcta desde Configuración > General > Logo
            LOGGER.log(Level.INFO, "Ruta del logo desde configuraciones: " + logoPath);
            if (logoPath != null && !logoPath.trim().isEmpty()) {
                LOGGER.log(Level.INFO, "Intentando cargar logo desde: " + logoPath);
            }
            updateLogo.accept(logoPath);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar configuración del logo", e);
            updateLogo.accept(null);
        }
        
        logoPanel.add(logoLabel, java.awt.BorderLayout.CENTER);
        
        // Sebastian - Guardar referencia al logoLabel para poder actualizarlo dinámicamente
        // (se puede usar más adelante para refrescar cuando cambie la configuración)
        logoPanel.putClientProperty("logoLabel", logoLabel);
        logoPanel.putClientProperty("updateLogo", updateLogo);
        
        artisticTopPanel.add(logoPanel, java.awt.BorderLayout.WEST);
        
        // Panel derecho con "Le atiende: [perfil]"
        javax.swing.JPanel rightTopPanel = new javax.swing.JPanel();
        rightTopPanel.setLayout(new java.awt.BorderLayout());
        rightTopPanel.setOpaque(false);
        rightTopPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 20, 10, 30)); // Padding para espaciar
        
        // Panel contenedor para el texto y el perfil (vertical)
        javax.swing.JPanel atendidoPanel = new javax.swing.JPanel();
        atendidoPanel.setLayout(new javax.swing.BoxLayout(atendidoPanel, javax.swing.BoxLayout.Y_AXIS));
        atendidoPanel.setOpaque(false);
        atendidoPanel.setAlignmentX(javax.swing.JComponent.RIGHT_ALIGNMENT);
        
        // Panel superior: "Le atiende: [perfil]"
        javax.swing.JPanel perfilPanel = new javax.swing.JPanel();
        perfilPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        perfilPanel.setOpaque(false);
        perfilPanel.setAlignmentX(javax.swing.JComponent.RIGHT_ALIGNMENT);
        
        // Label "Le atiende:"
        javax.swing.JLabel lblLeAtiende = new javax.swing.JLabel("Le atiende:");
        lblLeAtiende.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 13));
        lblLeAtiende.setForeground(new java.awt.Color(80, 80, 100));
        perfilPanel.add(lblLeAtiende);
        
        // Perfil del usuario - se agregará después de inicializar m_principalnotificator
        profilePanelRef = new javax.swing.JPanel();
        profilePanelRef.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));
        profilePanelRef.setOpaque(false);
        
        perfilPanel.add(profilePanelRef);
        atendidoPanel.add(perfilPanel);
        
        // Espacio pequeño entre perfil y botón cerrar
        atendidoPanel.add(javax.swing.Box.createVerticalStrut(5));
        
        // Panel inferior: botón cerrar
        javax.swing.JPanel closeButtonPanel = new javax.swing.JPanel();
        closeButtonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        closeButtonPanel.setOpaque(false);
        closeButtonPanel.setAlignmentX(javax.swing.JComponent.RIGHT_ALIGNMENT);
        
        // Botón cerrar programa
        javax.swing.JButton btnCerrar = new javax.swing.JButton();
        btnCerrar.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
        btnCerrar.setText(AppLocal.getIntString("button.exit"));
        btnCerrar.setFocusPainted(false);
        btnCerrar.setFocusable(false);
        btnCerrar.setPreferredSize(new java.awt.Dimension(80, 25));
        btnCerrar.setMinimumSize(new java.awt.Dimension(70, 25));
        btnCerrar.setMaximumSize(new java.awt.Dimension(90, 25));
        btnCerrar.setBackground(new java.awt.Color(220, 53, 69)); // Rojo para cerrar
        btnCerrar.setForeground(java.awt.Color.WHITE);
        btnCerrar.setOpaque(true);
        btnCerrar.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 12, 4, 12));
        btnCerrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_appview.tryToClose();
            }
        });
        closeButtonPanel.add(btnCerrar);
        
        atendidoPanel.add(closeButtonPanel);
        rightTopPanel.add(atendidoPanel, java.awt.BorderLayout.CENTER);
        
        artisticTopPanel.add(rightTopPanel, java.awt.BorderLayout.EAST);

        // Sebastian - Agregar barra de menú horizontal arriba del título (con múltiples
        // filas automáticas)
        // Usar BoxLayout vertical para permitir que el panel de botones se expanda
        // correctamente
        javax.swing.JPanel topContainer = new javax.swing.JPanel();
        topContainer.setLayout(new java.awt.BorderLayout());
        
        // Agregar panel artístico arriba
        topContainer.add(artisticTopPanel, java.awt.BorderLayout.NORTH);
        
        // Panel para la barra de menú y título
        javax.swing.JPanel menuContainer = new javax.swing.JPanel();
        menuContainer.setLayout(new javax.swing.BoxLayout(menuContainer, javax.swing.BoxLayout.Y_AXIS));
        menuContainer.setOpaque(false);
        topMenuBar.setAlignmentX(javax.swing.JComponent.LEFT_ALIGNMENT);
        menuContainer.add(topMenuBar);
        m_jPanelTitle.setAlignmentX(javax.swing.JComponent.LEFT_ALIGNMENT);
        menuContainer.add(m_jPanelTitle); // Sin espacio entre barra y título
        // Asegurar que el contenedor respete el tamaño preferido del panel de botones
        menuContainer.setAlignmentX(javax.swing.JComponent.LEFT_ALIGNMENT);
        // No forzar tamaño preferido para evitar espacio cuando m_jPanelTitle está
        // oculto
        // El tamaño se calculará automáticamente basado en los componentes visibles
        
        topContainer.add(menuContainer, java.awt.BorderLayout.CENTER);

        m_jPanelRightSide.add(topContainer, java.awt.BorderLayout.NORTH);

        m_jPanelContainer.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jPanelContainer.setLayout(new java.awt.CardLayout());
        m_jPanelRightSide.add(m_jPanelContainer, java.awt.BorderLayout.CENTER);

        add(m_jPanelRightSide, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void colapseButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_colapseButtonActionPerformed

        setMenuVisible(!m_jPanelMenu.isVisible());

    }// GEN-LAST:event_colapseButtonActionPerformed

    /**
     * Método helper para crear botones del menú de forma consistente
     */
    private javax.swing.JButton createMenuButton(String iconPath, String text, String taskClass) {
        return createMenuButton(iconPath, text, taskClass, null);
    }
    
    /**
     * Método helper para crear botones del menú con color personalizado
     */
    private javax.swing.JButton createMenuButton(String iconPath, String text, String taskClass, java.awt.Color backgroundColor) {
        javax.swing.JButton button = new javax.swing.JButton();
        // Iconos removidos para diseño compacto como eleventa
        button.setText(text);
        
        // Calcular ancho basado en la longitud del texto para que se lea bien
        int textLength = text.length();
        int buttonWidth = Math.max(60, Math.min(130, 30 + (textLength * 6))); // Mínimo 60, máximo 130, más proporcional al texto
        
        // Tamaño compacto y proporcional al texto
        button.setPreferredSize(new java.awt.Dimension(buttonWidth, 25));
        button.setMinimumSize(new java.awt.Dimension(60, 25));
        button.setMaximumSize(new java.awt.Dimension(130, 25));
        button.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 10)); // Tamaño de fuente ligeramente mayor
        
        // Color de fondo personalizado o blanco por defecto
        if (backgroundColor != null) {
            button.setBackground(backgroundColor);
            // Texto blanco si el fondo es oscuro, negro si es claro
            int brightness = (backgroundColor.getRed() + backgroundColor.getGreen() + backgroundColor.getBlue()) / 3;
            button.setForeground(brightness < 128 ? java.awt.Color.WHITE : java.awt.Color.BLACK);
        } else {
            button.setBackground(java.awt.Color.WHITE);
            button.setForeground(java.awt.Color.BLACK);
        }
        
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200), 1),
            javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5)));
        
        button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showTask(taskClass);
            }
        });
        return button;
    }

    /**
     * Configura los atajos de teclado globales para los botones principales
     * F1: Ventas
     * F2: Cerrar Caja
     * F3: Stock (Gestión de Inventario)
     * F4: Reportes
     */
    private void setupGlobalKeyboardShortcuts() {
        javax.swing.InputMap inputMap = this.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap actionMap = this.getActionMap();
        
        // F1: Ventas
        if (btnVentasRef != null) {
            inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0), "shortcutVentas");
            actionMap.put("shortcutVentas", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (btnVentasRef != null && btnVentasRef.isEnabled()) {
                        btnVentasRef.doClick();
                    }
                }
            });
        }
        
        // F2: Cerrar Caja
        if (btnCierreRef != null) {
            inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0), "shortcutCierre");
            actionMap.put("shortcutCierre", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (btnCierreRef != null && btnCierreRef.isEnabled()) {
                        btnCierreRef.doClick();
                    }
                }
            });
        }
        
        // F3: Stock (Gestión de Inventario)
        if (btnInventarioRef != null) {
            inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0), "shortcutInventario");
            actionMap.put("shortcutInventario", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (btnInventarioRef != null && btnInventarioRef.isEnabled()) {
                        btnInventarioRef.doClick();
                    }
                }
            });
        }
        
        // F4: Reportes
        if (btnReportesRef != null) {
            inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0), "shortcutReportes");
            actionMap.put("shortcutReportes", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (btnReportesRef != null && btnReportesRef.isEnabled()) {
                        btnReportesRef.doClick();
                    }
                }
            });
        }
        
        LOGGER.log(Level.INFO, "✅ Atajos de teclado globales configurados: F1=Ventas, F2=Cerrar Caja, F3=Stock, F4=Reportes");
    }

    /**
     * Convierte el ID del rol (0, 1, 2, 3...) al nombre del rol (ADMIN, MANAGER,
     * Employee)
     * para compatibilidad con el sistema de permisos
     */
    private String mapRoleIdToName(String roleId) {
        if (roleId == null || roleId.trim().isEmpty()) {
            return "Employee"; // Por defecto
        }

        switch (roleId.trim()) {
            case "0":
            case "1": // rol = 1 son admins según la tabla usuarios
                return "ADMIN";
            case "2":
                return "MANAGER";
            case "3":
                return "Employee";
            default:
                // Si ya es un nombre de rol, devolverlo tal cual
                if (roleId.equalsIgnoreCase("ADMIN") || roleId.equalsIgnoreCase("MANAGER")
                        || roleId.equalsIgnoreCase("Employee")) {
                    return roleId.substring(0, 1).toUpperCase() + roleId.substring(1).toLowerCase();
                }
                return roleId; // Rol personalizado
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton colapseButton;
    private javax.swing.JPanel colapseHPanel;
    private javax.swing.JPanel m_jPanelContainer;
    private javax.swing.JPanel m_jPanelLefSide;
    private javax.swing.JScrollPane m_jPanelMenu;
    private javax.swing.JPanel m_jPanelRightSide;
    private javax.swing.JPanel m_jPanelTitle;
    private javax.swing.JLabel m_jTitle;
    // Sebastian - Label de puntos del cliente en la barra superior
    private javax.swing.JLabel m_jCustomerPoints;
    // End of variables declaration//GEN-END:variables

    /**
     * Sebastian - Método público para actualizar el label de puntos del cliente
     */
    public void updateCustomerPointsDisplay(String text, boolean visible) {
        if (m_jCustomerPoints != null) {
            m_jCustomerPoints.setText(text);
            m_jCustomerPoints.setVisible(visible);
        }
    }

}
