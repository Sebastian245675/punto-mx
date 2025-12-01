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
        
        // IMPORTANTE: Inicializar roles predeterminados ANTES de cargar permisos del usuario
        try {
            System.out.println("=== INICIALIZANDO ROLES AL INICIO DE LA APP ===");
            com.openbravo.pos.admin.DataLogicAdmin dlAdmin = 
                (com.openbravo.pos.admin.DataLogicAdmin) m_appview.getBean("com.openbravo.pos.admin.DataLogicAdmin");
            com.openbravo.pos.admin.DefaultRolesInitializer.initializeDefaultRoles(dlAdmin.getSession());
            System.out.println("=== ROLES INICIALIZADOS CORRECTAMENTE ===");
        } catch (Exception ex) {
            System.err.println("ERROR al inicializar roles: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        AppUserPermissionsLoader aupLoader = new AppUserPermissionsLoader(m_dlSystem);
        
        // Convertir ID de rol a nombre de rol para compatibilidad
        String roleName = mapRoleIdToName(m_appuser.getRole());
        System.out.println("Usuario: " + m_appuser.getName() + " | Rol ID: " + m_appuser.getRole() + " | Rol Nombre: " + roleName);
        
        Set<String> userPermissions = aupLoader.getPermissionsForRole(roleName);
        m_appuser.fillPermissions(userPermissions);

        initComponents();
        applyComponentOrientation(m_appview.getComponentOrientation());

        m_principalnotificator = new JLabel();
        m_principalnotificator.applyComponentOrientation(getComponentOrientation());
        m_principalnotificator.setText(m_appuser.getName());
        m_principalnotificator.setIcon(m_appuser.getIcon());

        //MENU SIDE
        colapseHPanel.add(Box.createVerticalStrut(50), 0);
        m_jPanelMenu.getVerticalScrollBar().setPreferredSize(new Dimension(35, 35));
        rMenu = new JRootMenu(this, this);
        rMenu.setRootMenu(m_jPanelMenu, m_dlSystem);
        setMenuIcon();
        assignMenuButtonIcon();

        //MAIN 
        m_jPanelTitle.setVisible(false);
        m_jPanelTitle.setPreferredSize(new java.awt.Dimension(0, 0)); // Sin tamaño cuando está oculto
        m_jPanelTitle.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 0)); // Sin altura cuando está oculto
        addView(new JPanel(), "<NULL>");
        showView("<NULL>");

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
                        LOGGER.info("Call 'deactivate' on class: " + rMenu.getViewManager().getLastView().getClass().getName());
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
                        m_jPanelTitle.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)); // Restaurar tamaño máximo
                        m_jTitle.setText(sTitle);
                    } else {
                        m_jPanelTitle.setVisible(false);
                        m_jPanelTitle.setPreferredSize(new java.awt.Dimension(0, 0)); // Sin tamaño cuando está oculto
                        m_jPanelTitle.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 0)); // Sin altura cuando está oculto
                        m_jTitle.setText("");
                    }
                } else {
                    LOGGER.log(Level.INFO, "Already open: " + sTaskClass + ", Instance: " + viewPanel);
                }
            } else {

                LOGGER.log(Level.INFO, "NO PERMISSION on call class: : " + sTaskClass);
                JMessageDialog.showMessage(this,
                        new MessageInf(MessageInf.SGN_WARNING,
                                AppLocal.getIntString("message.notpermissions"), "<html>"+sTaskClass));
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
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, colapseHPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(colapseButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        colapseHPanelLayout.setVerticalGroup(
            colapseHPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(colapseHPanelLayout.createSequentialGroup()
                .addContainerGap(88, Short.MAX_VALUE)
                .addComponent(colapseButton, javax.swing.GroupLayout.DEFAULT_SIZE, 33, Short.MAX_VALUE)
                .addContainerGap(188, Short.MAX_VALUE))
        );

        m_jPanelLefSide.add(colapseHPanel, java.awt.BorderLayout.LINE_END);

        // Sebastian - Ocultar la barra lateral del menú para diseño tipo eleventa
        m_jPanelLefSide.setVisible(false);
        // add(m_jPanelLefSide, java.awt.BorderLayout.LINE_START);

        m_jPanelRightSide.setPreferredSize(new java.awt.Dimension(200, 40));
        m_jPanelRightSide.setLayout(new java.awt.BorderLayout());
        
        // Sebastian - Crear barra horizontal superior con TODOS los botones del menú (estilo eleventa)
        javax.swing.JPanel topMenuBar = new javax.swing.JPanel();
        topMenuBar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 3, 2)); // Espaciado horizontal reducido para diseño compacto
        topMenuBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5)); // Reducir padding vertical (2px en lugar de 5px)
        topMenuBar.setBackground(new java.awt.Color(220, 220, 220)); // Gris suave
        // Diseño compacto para que quepan todos los botones en una línea
        topMenuBar.setAlignmentX(javax.swing.JComponent.LEFT_ALIGNMENT);
        topMenuBar.setMinimumSize(new java.awt.Dimension(0, 30)); // Altura mínima reducida (25px botón + padding)
        topMenuBar.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 30)); // Altura máxima fija para una sola línea
        
        // ========== MENU.MAIN - Elementos principales ==========
        // Botón Ventas (Menu.Ticket)
        javax.swing.JButton btnVentas = createMenuButton(
            "/com/openbravo/images/sale.png",
            AppLocal.getIntString("Menu.Ticket"),
            "com.openbravo.pos.sales.JPanelTicketSales"
        );
        topMenuBar.add(btnVentas);
        
        // Botón Pagos de Clientes (Menu.CustomersPayment)
        javax.swing.JButton btnPagosClientes = createMenuButton(
            "/com/openbravo/images/customerpay.png",
            AppLocal.getIntString("Menu.CustomersPayment"),
            "com.openbravo.pos.customers.CustomersPayment"
        );
        topMenuBar.add(btnPagosClientes);
        
        // Botón Cierre de Caja (Menu.CloseTPV)
        javax.swing.JButton btnCierre = createMenuButton(
            "/com/openbravo/images/calculator.png",
            AppLocal.getIntString("Menu.CloseTPV"),
            "com.openbravo.pos.panels.JPanelCloseMoney"
        );
        topMenuBar.add(btnCierre);
        
        // Botón Gestión de Sucursales (Menu.BranchesManagement)
        javax.swing.JButton btnSucursales = createMenuButton(
            "/com/openbravo/images/user.png",
            AppLocal.getIntString("Menu.BranchesManagement"),
            "com.openbravo.pos.branches.JPanelBranchesManagement"
        );
        topMenuBar.add(btnSucursales);
        
        // ========== MENU.BACKOFFICE - Submenús ==========
        // Botón Clientes (Menu.Customers - submenu)
        javax.swing.JButton btnClientes = createMenuButton(
            "/com/openbravo/images/customer.png",
            AppLocal.getIntString("Menu.Customers"),
            "com.openbravo.pos.forms.MenuCustomers"
        );
        topMenuBar.add(btnClientes);
        
        // Botón Proveedores (Menu.Suppliers - submenu)
        javax.swing.JButton btnProveedores = createMenuButton(
            "/com/openbravo/images/stockmaint.png",
            AppLocal.getIntString("Menu.Suppliers"),
            "com.openbravo.pos.forms.MenuSuppliers"
        );
        topMenuBar.add(btnProveedores);
        
        // Botón Gestión de Inventario (Menu.StockManagement - submenu)
        javax.swing.JButton btnInventario = createMenuButton(
            "/com/openbravo/images/products.png",
            AppLocal.getIntString("Menu.StockManagement"),
            "com.openbravo.pos.forms.MenuStockManagement"
        );
        topMenuBar.add(btnInventario);
        
        // Botón Gestión de Ventas (Menu.SalesManagement - submenu)
        javax.swing.JButton btnVentasManagement = createMenuButton(
            "/com/openbravo/images/sales.png",
            AppLocal.getIntString("Menu.SalesManagement"),
            "com.openbravo.pos.forms.MenuSalesManagement"
        );
        topMenuBar.add(btnVentasManagement);
        
        // Botón Mantenimiento (Menu.Maintenance - submenu)
        javax.swing.JButton btnMantenimiento = createMenuButton(
            "/com/openbravo/images/maintain.png",
            AppLocal.getIntString("Menu.Maintenance"),
            "com.openbravo.pos.forms.MenuMaintenance"
        );
        topMenuBar.add(btnMantenimiento);
        
        // Botón Gestión de Presencia (Menu.PresenceManagement - submenu)
        javax.swing.JButton btnPresencia = createMenuButton(
            "/com/openbravo/images/users.png",
            AppLocal.getIntString("Menu.PresenceManagement"),
            "com.openbravo.pos.forms.MenuEmployees"
        );
        topMenuBar.add(btnPresencia);
        
        // ========== MENU.UTILITIES ==========
        // Botón Herramientas (Menu.Tools - submenu)
        javax.swing.JButton btnHerramientas = createMenuButton(
            "/com/openbravo/images/utilities.png",
            AppLocal.getIntString("Menu.Tools"),
            "com.openbravo.pos.imports.JPanelCSV"
        );
        topMenuBar.add(btnHerramientas);
        
        // ========== MENU.SYSTEM ==========
        // Botón Cambiar Contraseña movido a Configuración > General
        
        // Botón Configuración (Menu.Configuration)
        javax.swing.JButton btnConfig = createMenuButton(
            "/com/openbravo/images/configuration.png",
            AppLocal.getIntString("Menu.Configuration"),
            "com.openbravo.pos.config.JPanelConfiguration"
        );
        topMenuBar.add(btnConfig);
        
        // Botón Impresora (Menu.Printer)
        javax.swing.JButton btnImpresora = createMenuButton(
            "/com/openbravo/images/printer.png",
            AppLocal.getIntString("Menu.Printer"),
            "com.openbravo.pos.panels.JPanelPrinter"
        );
        topMenuBar.add(btnImpresora);
        
        // Botón Salir (Menu.Exit)
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
        topMenuBar.add(btnSalir);
        
        // Altura fija para una sola línea con botones compactos
        topMenuBar.setPreferredSize(new java.awt.Dimension(Integer.MAX_VALUE, 30)); // Altura fija para una línea

        m_jPanelTitle.setLayout(new java.awt.BorderLayout());
        m_jPanelTitle.setBorder(null); // Sin borde para eliminar espacio
        m_jPanelTitle.setPreferredSize(new java.awt.Dimension(0, 0)); // Sin tamaño cuando está oculto
        m_jPanelTitle.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 0)); // Sin altura cuando está oculto

        m_jTitle.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        m_jTitle.setForeground(new java.awt.Color(0, 168, 223));
        m_jTitle.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.darkGray), javax.swing.BorderFactory.createEmptyBorder(2, 10, 2, 10))); // Reducir padding vertical (2px en lugar de 10px)
        m_jTitle.setMaximumSize(new java.awt.Dimension(100, 28)); // Reducir altura máxima aún más
        m_jTitle.setMinimumSize(new java.awt.Dimension(30, 24));
        m_jTitle.setPreferredSize(new java.awt.Dimension(100, 28)); // Reducir altura preferida (28px)
        m_jPanelTitle.add(m_jTitle, java.awt.BorderLayout.NORTH);
        
        // Sebastian - Agregar barra de menú horizontal arriba del título (con múltiples filas automáticas)
        // Usar BoxLayout vertical para permitir que el panel de botones se expanda correctamente
        javax.swing.JPanel topContainer = new javax.swing.JPanel();
        topContainer.setLayout(new javax.swing.BoxLayout(topContainer, javax.swing.BoxLayout.Y_AXIS));
        topContainer.setBorder(null); // Sin borde
        topMenuBar.setAlignmentX(javax.swing.JComponent.LEFT_ALIGNMENT);
        topContainer.add(topMenuBar);
        m_jPanelTitle.setAlignmentX(javax.swing.JComponent.LEFT_ALIGNMENT);
        topContainer.add(m_jPanelTitle); // Sin espacio entre barra y título
        // Asegurar que el contenedor respete el tamaño preferido del panel de botones
        topContainer.setAlignmentX(javax.swing.JComponent.LEFT_ALIGNMENT);
        // No forzar tamaño preferido para evitar espacio cuando m_jPanelTitle está oculto
        // El tamaño se calculará automáticamente basado en los componentes visibles

        m_jPanelRightSide.add(topContainer, java.awt.BorderLayout.NORTH);

        m_jPanelContainer.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jPanelContainer.setLayout(new java.awt.CardLayout());
        m_jPanelRightSide.add(m_jPanelContainer, java.awt.BorderLayout.CENTER);

        add(m_jPanelRightSide, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void colapseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colapseButtonActionPerformed

    setMenuVisible(!m_jPanelMenu.isVisible());

}//GEN-LAST:event_colapseButtonActionPerformed

    /**
     * Método helper para crear botones del menú de forma consistente
     */
    private javax.swing.JButton createMenuButton(String iconPath, String text, String taskClass) {
        javax.swing.JButton button = new javax.swing.JButton();
        // Iconos removidos para diseño compacto como eleventa
        button.setText(text);
        // Tamaño reducido para que quepan todos en una línea
        button.setPreferredSize(new java.awt.Dimension(70, 25));
        button.setMinimumSize(new java.awt.Dimension(60, 25));
        button.setMaximumSize(new java.awt.Dimension(90, 25));
        button.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 9));
        // Botones blancos con texto negro
        button.setBackground(java.awt.Color.WHITE);
        button.setForeground(java.awt.Color.BLACK);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showTask(taskClass);
            }
        });
        return button;
    }

    /**
     * Convierte el ID del rol (0, 1, 2, 3...) al nombre del rol (ADMIN, MANAGER, Employee)
     * para compatibilidad con el sistema de permisos
     */
    private String mapRoleIdToName(String roleId) {
        if (roleId == null || roleId.trim().isEmpty()) {
            return "Employee"; // Por defecto
        }
        
        switch (roleId.trim()) {
            case "0":
            case "1":  // rol = 1 son admins según la tabla usuarios
                return "ADMIN";
            case "2":
                return "MANAGER";
            case "3":
                return "Employee";
            default:
                // Si ya es un nombre de rol, devolverlo tal cual
                if (roleId.equalsIgnoreCase("ADMIN") || roleId.equalsIgnoreCase("MANAGER") || roleId.equalsIgnoreCase("Employee")) {
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
    // End of variables declaration//GEN-END:variables

}
