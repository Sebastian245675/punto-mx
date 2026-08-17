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

package com.openbravo.pos.admin;

import com.openbravo.basic.BasicException;
import com.openbravo.beans.JPasswordDialog;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.user.*;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.util.StringUtils;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.StringReader;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Vista de edición de usuarios con diseño mejorado
 *
 * @author adrianromero
 * @author Sebastian (mejoras UI)
 */
public class PeopleView extends JPanel implements EditorRecord<Object> {

        private static final long serialVersionUID = 1L;

        private String m_oId;
        private String m_sPassword;
        private String m_currentRoleId; // Track what role this user currently has

        private final DirtyManager m_Dirty;
        private final DataLogicAdmin dlAdmin;

        private final SentenceList<RoleInfo> m_sentrole;
        private ComboBoxValModel<RoleInfo> m_RoleModel;

        private final ComboBoxValModel<String> m_ReasonModel;

        private static final String TAB_SALES = "Ventas";
        private static final String TAB_CUSTOMERS = "Clientes";
        private static final String TAB_PRODUCTS = "Productos";
        private static final String TAB_INVENTORY = "Inventario";
        private static final String TAB_OTHER = "Otros";
        private static final List<String> PERMISSION_TAB_ORDER = Arrays.asList(
                        TAB_SALES, TAB_CUSTOMERS, TAB_PRODUCTS, TAB_INVENTORY, TAB_OTHER);
        private static final Set<String> SALES_CATEGORIES = new HashSet<>(Arrays.asList(
                        "Ventas", "Métodos de Pago", "Reembolsos", "Botones Especiales"));
        private static final Set<String> PRODUCT_PERMISSIONS = new HashSet<>(Arrays.asList(
                        "com.openbravo.pos.inventory.ProductsPanel",
                        "com.openbravo.pos.inventory.CategoriesPanel",
                        "com.openbravo.pos.inventory.AttributesPanel",
                        "com.openbravo.pos.inventory.AttributeValuesPanel",
                        "com.openbravo.pos.inventory.AttributeSetsPanel",
                        "com.openbravo.pos.inventory.AttributeUsePanel",
                        "com.openbravo.pos.inventory.AuxiliarPanel",
                        "com.openbravo.pos.inventory.BundlePanel",
                        "com.openbravo.pos.inventory.TaxCategoriesPanel",
                        "com.openbravo.pos.inventory.TaxCustCategoriesPanel",
                        "com.openbravo.pos.inventory.TaxPanel",
                        "com.openbravo.pos.inventory.UomPanel",
                        "/com/openbravo/reports/products.bs",
                        "/com/openbravo/reports/productscatalog.bs",
                        "/com/openbravo/reports/productlabels.bs",
                        "/com/openbravo/reports/salecatalog.bs",
                        "/com/openbravo/reports/barcode_sheet.bs",
                        "/com/openbravo/reports/barcode_shelfedgelabels.bs",
                        "/com/openbravo/reports/tools_newproducts.bs",
                        "/com/openbravo/reports/tools_updatedprices.bs",
                        "/com/openbravo/reports/tools_badprice.bs",
                        "/com/openbravo/reports/tools_invalidcategory.bs",
                        "/com/openbravo/reports/tools_missingdata.bs",
                        "/com/openbravo/reports/tools_invaliddata.bs"));

        // Panel de permisos con tabs
        private JTabbedPane permissionsTabbedPane;
        private final Map<String, JCheckBox> bundleCheckboxes = new LinkedHashMap<>();
        private final Map<String, PermissionBundle> permissionBundles = new LinkedHashMap<>();
        private final Set<String> selectedPermissionNames = new LinkedHashSet<>();
        private final List<JButton> permissionActionButtons = new ArrayList<>();

        // Componentes UI principales
        private JTabbedPane jTabbedPane1;
        private JPanel generalPanel;
        private JPanel imagePanel;
        private JLabel jLabel1;
        private JTextField m_jName;
        private JCheckBox m_jVisible;
        private JLabel jLabel3;
        private JButton jButton1;
        private JLabel jLblCardID;
        private JTextField m_jcard;
        private JComboBox webCBSecurity;
        private JLabel jLabel6;
        private com.openbravo.data.gui.JImageEditor m_jImage;

        // Colores
        private static final Color PRIMARY_COLOR = new Color(41, 128, 185);
        private static final Color HEADER_BG = new Color(245, 245, 245);

        private static final class PermissionBundle {
                private final String key;
                private final String displayName;
                private final List<PermissionInfo> permissions;

                private PermissionBundle(String key, String displayName, List<PermissionInfo> permissions) {
                        this.key = key;
                        this.displayName = displayName;
                        this.permissions = new ArrayList<>(permissions);
                }
        }

        public PeopleView(DataLogicAdmin dlAdmin, DirtyManager dirty) {
                this.dlAdmin = dlAdmin;
                this.m_Dirty = dirty;

                m_sentrole = dlAdmin.getRolesList();
                m_RoleModel = new ComboBoxValModel<>();

                m_ReasonModel = new ComboBoxValModel<>();
                m_ReasonModel.add(AppLocal.getIntString("cboption.generate"));
                m_ReasonModel.add(AppLocal.getIntString("cboption.clear"));

                initComponents();
                createPermissionsTabs();

                cleanFields();
                disableFields();
        }

        private void initComponents() {
                jTabbedPane1 = new JTabbedPane();
                generalPanel = new JPanel();
                imagePanel = new JPanel();

                jLabel1 = new JLabel();
                m_jName = new JTextField();
                m_jVisible = new JCheckBox();
                jLabel3 = new JLabel();
                jButton1 = new JButton();
                jLblCardID = new JLabel();
                m_jcard = new JTextField();
                webCBSecurity = new JComboBox();
                jLabel6 = new JLabel();
                m_jImage = new com.openbravo.data.gui.JImageEditor();

                setFont(new Font("Arial", Font.PLAIN, 12));
                setPreferredSize(new Dimension(900, 640));

                jTabbedPane1.setMinimumSize(new Dimension(760, 560));
                jTabbedPane1.setPreferredSize(new Dimension(880, 620));

                // General Panel Layout
                generalPanel.setLayout(new BorderLayout(0, 15));
                generalPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
                generalPanel.setBackground(Color.WHITE);

                // Header Title
                JLabel headerTitle = new JLabel("USUARIOS Y PERMISOS");
                headerTitle.setFont(new Font("Arial", Font.BOLD, 22));
                headerTitle.setForeground(PRIMARY_COLOR);
                headerTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

                JPanel northContainer = new JPanel(new BorderLayout());
                northContainer.setBackground(Color.WHITE);
                northContainer.add(headerTitle, BorderLayout.NORTH);

                JPanel basicInfoPanel = createBasicInfoPanel();
                northContainer.add(basicInfoPanel, BorderLayout.CENTER);

                generalPanel.add(northContainer, BorderLayout.NORTH);

                // Permissions Tabs
                permissionsTabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
                permissionsTabbedPane.setFont(new Font("Arial", Font.BOLD, 15));
                permissionsTabbedPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

                generalPanel.add(permissionsTabbedPane, BorderLayout.CENTER);

                jTabbedPane1.addTab("Datos y permisos", generalPanel);

                // Image Panel
                m_jImage.setFont(new Font("Arial", Font.PLAIN, 12));
                m_jImage.setPreferredSize(new Dimension(300, 250));
                m_jImage.addPropertyChangeListener("image", m_Dirty);

                imagePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
                imagePanel.add(m_jImage);

                jTabbedPane1.addTab(AppLocal.getIntString("label.peopleimage"), imagePanel);

                setLayout(new BorderLayout());
                add(jTabbedPane1, BorderLayout.CENTER);
        }

        private JPanel createBasicInfoPanel() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setBackground(HEADER_BG);
                panel.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                                BorderFactory.createEmptyBorder(15, 20, 15, 20)));

                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(8, 10, 8, 10);
                gbc.anchor = GridBagConstraints.WEST;
                gbc.fill = GridBagConstraints.NONE; // FIX: No stretch

                // Row 1: Usuario & Clave
                jLabel1.setFont(new Font("Arial", Font.BOLD, 14));
                jLabel1.setForeground(new Color(80, 80, 80));
                jLabel1.setText(AppLocal.getIntString("label.peoplenamem")); // Usuario
                jLabel1.addMouseListener(new java.awt.event.MouseAdapter() {
                        public void mouseClicked(java.awt.event.MouseEvent evt) {
                                jLabel1MouseClicked(evt);
                        }
                });

                m_jName.setFont(new Font("Arial", Font.PLAIN, 14));
                m_jName.setPreferredSize(new Dimension(220, 30));
                m_jName.getDocument().addDocumentListener(m_Dirty);

                jLabel6.setFont(new Font("Arial", Font.BOLD, 14));
                jLabel6.setForeground(new Color(80, 80, 80));
                jLabel6.setText(AppLocal.getIntString("label.Password"));

                jButton1.setFont(new Font("Arial", Font.PLAIN, 12));
                jButton1.setIcon(new ImageIcon(getClass().getResource("/com/openbravo/images/password.png")));
                jButton1.setText(AppLocal.getIntString("button.peoplepassword"));
                jButton1.setPreferredSize(new Dimension(160, 30));
                jButton1.addActionListener(evt -> jButton1ActionPerformed(evt));

                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.weightx = 0;
                panel.add(jLabel1, gbc);

                gbc.gridx = 1;
                gbc.gridy = 0;
                gbc.weightx = 0; // FIX: Weight 0
                panel.add(m_jName, gbc);

                gbc.gridx = 2;
                gbc.gridy = 0;
                gbc.weightx = 0;
                gbc.insets = new Insets(8, 30, 8, 10);
                panel.add(jLabel6, gbc);

                gbc.gridx = 3;
                gbc.gridy = 0;
                gbc.weightx = 0; // FIX: Weight 0
                gbc.insets = new Insets(8, 10, 8, 10);
                panel.add(jButton1, gbc);

                // Filler for Row 1 to push content left
                GridBagConstraints gbcFiller = new GridBagConstraints();
                gbcFiller.gridx = 4;
                gbcFiller.gridy = 0;
                gbcFiller.weightx = 1.0;
                gbcFiller.fill = GridBagConstraints.HORIZONTAL;
                panel.add(new JPanel() {
                        {
                                setOpaque(false);
                        }
                }, gbcFiller);

                // Row 2: Visible & Tarjeta
                jLabel3.setFont(new Font("Arial", Font.BOLD, 14));
                jLabel3.setForeground(new Color(80, 80, 80));
                jLabel3.setText(AppLocal.getIntString("label.peoplevisible"));

                m_jVisible.setFont(new Font("Arial", Font.PLAIN, 12));
                m_jVisible.setBackground(HEADER_BG);
                m_jVisible.addActionListener(m_Dirty);

                jLblCardID.setFont(new Font("Arial", Font.BOLD, 14));
                jLblCardID.setForeground(new Color(80, 80, 80));
                jLblCardID.setText(AppLocal.getIntString("label.card"));

                m_jcard.setFont(new Font("Arial", Font.PLAIN, 14));
                m_jcard.setPreferredSize(new Dimension(150, 30));
                m_jcard.getDocument().addDocumentListener(m_Dirty);

                webCBSecurity.setFont(new Font("Arial", Font.PLAIN, 12));
                webCBSecurity.setModel(m_ReasonModel);
                webCBSecurity.setPreferredSize(new Dimension(100, 30));
                webCBSecurity.addActionListener(evt -> webCBSecurityActionPerformed(evt));

                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.weightx = 0;
                gbc.insets = new Insets(8, 10, 8, 10);
                panel.add(jLabel3, gbc);

                gbc.gridx = 1;
                gbc.gridy = 1;
                gbc.weightx = 0; // FIX: Weight 0
                panel.add(m_jVisible, gbc);

                gbc.gridx = 2;
                gbc.gridy = 1;
                gbc.weightx = 0;
                gbc.insets = new Insets(8, 30, 8, 10);
                panel.add(jLblCardID, gbc);

                gbc.gridx = 3;
                gbc.gridy = 1;
                gbc.weightx = 0; // FIX: Weight 0
                gbc.insets = new Insets(8, 10, 8, 10);
                JPanel cardPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                cardPanel.setBackground(HEADER_BG);
                cardPanel.add(m_jcard);
                cardPanel.add(webCBSecurity);
                panel.add(cardPanel, gbc);

                return panel;
        }

        private void createPermissionsTabs() {
                bundleCheckboxes.clear();
                permissionBundles.clear();
                permissionActionButtons.clear();
                permissionsTabbedPane.removeAll();

                Map<String, List<PermissionBundle>> groupedPermissions = createPermissionBundles();
                for (String tabName : PERMISSION_TAB_ORDER) {
                        permissionsTabbedPane.addTab(tabName, createPermissionTab(groupedPermissions.get(tabName)));
                }
        }

        private Map<String, List<PermissionBundle>> createPermissionBundles() {
                Map<String, Map<String, List<PermissionInfo>>> technicalGroups = new LinkedHashMap<>();
                for (String tabName : PERMISSION_TAB_ORDER) {
                        technicalGroups.put(tabName, new LinkedHashMap<>());
                }

                for (Map.Entry<String, List<PermissionInfo>> entry : PermissionsCatalog.getAllPermissions().entrySet()) {
                        for (PermissionInfo permission : entry.getValue()) {
                                String tabName = getPermissionTab(entry.getKey(), permission);
                                String sectionName = getPermissionSection(entry.getKey(), tabName, permission);
                                technicalGroups.get(tabName)
                                                .computeIfAbsent(sectionName, key -> new ArrayList<>())
                                                .add(permission);
                        }
                }

                Map<String, List<PermissionBundle>> bundlesByTab = new LinkedHashMap<>();
                for (String tabName : PERMISSION_TAB_ORDER) {
                        List<PermissionBundle> bundles = new ArrayList<>();
                        for (Map.Entry<String, List<PermissionInfo>> section : technicalGroups.get(tabName).entrySet()) {
                                String key = tabName + ":" + section.getKey();
                                PermissionBundle bundle = new PermissionBundle(
                                                key,
                                                getBundleDisplayName(tabName, section.getKey()),
                                                section.getValue());
                                bundles.add(bundle);
                                permissionBundles.put(key, bundle);
                        }
                        bundlesByTab.put(tabName, bundles);
                }
                return bundlesByTab;
        }

        private static String getPermissionTab(String category, PermissionInfo permission) {
                if (SALES_CATEGORIES.contains(category)) {
                        return TAB_SALES;
                }
                if ("Caja".equals(category)
                                && "com.openbravo.pos.panels.JPanelPayments".equals(permission.getClassName())) {
                        return TAB_SALES;
                }
                if (TAB_CUSTOMERS.equals(category)) {
                        return TAB_CUSTOMERS;
                }
                if ("Inventario".equals(category)
                                || "Reportes de Inventario".equals(category)
                                || "Herramientas".equals(category)) {
                        return PRODUCT_PERMISSIONS.contains(permission.getClassName())
                                        ? TAB_PRODUCTS
                                        : TAB_INVENTORY;
                }
                return TAB_OTHER;
        }

        private static String getPermissionSection(String category, String tabName, PermissionInfo permission) {
                String permissionName = permission.getClassName();
                if (TAB_SALES.equals(tabName)) {
                        if ("com.openbravo.pos.panels.JPanelPayments".equals(permissionName)) {
                                return "Registrar entradas y salidas de efectivo";
                        }
                        if ("Métodos de Pago".equals(category)) {
                                return "payment.debt".equals(permissionName)
                                                ? "Cobrar ventas a crédito"
                                                : "Cobrar en efectivo, tarjeta y otros medios";
                        }
                        if ("Reembolsos".equals(category)
                                        || "sales.RefundTicket".equals(permissionName)
                                        || "button.refundit".equals(permissionName)) {
                                return "Cancelar tickets y realizar devoluciones";
                        }
                        if ("button.totaldiscount".equals(permissionName)
                                        || "button.linediscount".equals(permissionName)) {
                                return "Aplicar descuentos a las ventas";
                        }
                        if ("com.openbravo.pos.sales.JPanelTicketEdits".equals(permissionName)
                                        || "sales.EditTicket".equals(permissionName)
                                        || "sales.PrintTicket".equals(permissionName)
                                        || "sales.ShowList".equals(permissionName)
                                        || "sales.ViewSharedTicket".equals(permissionName)) {
                                return "Revisar el historial y reimprimir ventas";
                        }
                        if ("sales.DeleteLines".equals(permissionName)
                                        || "sales.EditLines".equals(permissionName)
                                        || "sales.DeleteTicket".equals(permissionName)) {
                                return "Modificar o eliminar artículos de una venta";
                        }
                        if ("com.openbravo.pos.sales.JPanelTicketSales".equals(permissionName)
                                        || "sales.Total".equals(permissionName)) {
                                return "Realizar y cobrar ventas";
                        }
                        return "Usar herramientas avanzadas de venta";
                }

                if (TAB_CUSTOMERS.equals(tabName)) {
                        if ("com.openbravo.pos.customers.CustomersPayment".equals(permissionName)) {
                                return "Recibir pagos y abonos de clientes";
                        }
                        if (permissionName.startsWith("/com/openbravo/reports/")) {
                                return "Ver reportes y cuentas de clientes";
                        }
                        return "Crear, modificar y consultar clientes";
                }

                if (TAB_PRODUCTS.equals(tabName)) {
                        if ("Inventario".equals(category)) {
                                if ("com.openbravo.pos.inventory.ProductsPanel".equals(permissionName)) {
                                        return "Crear, modificar y eliminar productos";
                                }
                                return "Configurar categorías, atributos e impuestos";
                        }
                        if ("Reportes de Inventario".equals(category)) {
                                return "Consultar catálogos e imprimir etiquetas";
                        }
                        if ("Herramientas".equals(category)) {
                                return "Revisar precios y datos de productos";
                        }
                }
                if (TAB_INVENTORY.equals(tabName)) {
                        if ("Inventario".equals(category)) {
                                if ("com.openbravo.pos.inventory.StockManagement".equals(permissionName)) {
                                        return "Ajustar las existencias del inventario";
                                }
                                if ("com.openbravo.pos.inventory.LocationsPanel".equals(permissionName)) {
                                        return "Administrar almacenes y ubicaciones";
                                }
                                return "Consultar existencias y movimientos de inventario";
                        }
                        if ("Reportes de Inventario".equals(category)) {
                                return "Ver reportes de existencias y diferencias";
                        }
                        if ("Herramientas".equals(category)) {
                                return "Actualizar cantidades de inventario";
                        }
                }

                if ("Caja".equals(category)) {
                        return "com.openbravo.pos.panels.JPanelCloseMoneyReprint".equals(permissionName)
                                        ? "Reimprimir cortes de caja"
                                        : "Realizar cortes de turno y del día";
                }
                if ("Proveedores".equals(category)) {
                        return "Gestionar proveedores y consultar sus reportes";
                }
                if ("Reportes de Ventas".equals(category) || "Gráficos".equals(category)) {
                        return "Ver reportes de ventas y ganancias";
                }
                if ("Mantenimiento".equals(category)) {
                        if ("com.openbravo.pos.admin.PeoplePanel".equals(permissionName)
                                        || "com.openbravo.pos.admin.RolesPanel".equals(permissionName)
                                        || "com.openbravo.pos.admin.ResourcesPanel".equals(permissionName)) {
                                return "Administrar usuarios y permisos";
                        }
                        if ("com.openbravo.pos.config.JPanelConfiguration".equals(permissionName)
                                        || "com.openbravo.pos.panels.JPanelPrinter".equals(permissionName)
                                        || "Menu.ChangePassword".equals(permissionName)
                                        || "com.openbravo.pos.forms.MenuMaintenance".equals(permissionName)) {
                                return "Cambiar la configuración del programa";
                        }
                        if ("com.openbravo.pos.sales.restaurant.JPanelFloors".equals(permissionName)
                                        || "com.openbravo.pos.sales.restaurant.JPanelPlaces".equals(permissionName)) {
                                return "Administrar pisos y mesas del restaurante";
                        }
                        if ("com.openbravo.pos.voucher.VoucherPanel".equals(permissionName)) {
                                return "Administrar vales";
                        }
                        if ("com.openbravo.pos.branches.JPanelBranchesManagement".equals(permissionName)) {
                                return "Administrar sucursales";
                        }
                }
                if ("Reportes de Usuarios".equals(category)) {
                        return "Ver reportes de usuarios y sus ventas";
                }
                if ("Importación/Exportación".equals(category)) {
                        return "Importar y exportar información";
                }
                if ("Gestión de Empleados".equals(category)) {
                        return "Gestionar empleados, horarios y asistencia";
                }
                return category;
        }

        private static String getBundleDisplayName(String tabName, String sectionName) {
                return sectionName;
        }

        private JPanel createPermissionTab(List<PermissionBundle> bundles) {
                JPanel tabContent = new JPanel(new BorderLayout(0, 8));
                tabContent.setBackground(Color.WHITE);
                tabContent.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

                tabContent.add(createPermissionActions(bundles), BorderLayout.NORTH);

                JPanel checksPanel = new JPanel(new GridLayout(0, 1, 8, 10));
                checksPanel.setBackground(Color.WHITE);
                checksPanel.setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));
                for (PermissionBundle bundle : bundles) {
                        JCheckBox checkBox = new JCheckBox(bundle.displayName);
                        checkBox.setFont(new Font("Arial", Font.PLAIN, 16));
                        checkBox.setBackground(Color.WHITE);
                        checkBox.setCursor(new Cursor(Cursor.HAND_CURSOR));
                        checkBox.setToolTipText("Activa todos los permisos necesarios para: " + bundle.displayName);
                        checkBox.addActionListener(event -> {
                                applyBundleSelection(bundle, checkBox.isSelected());
                                m_Dirty.setDirty(true);
                        });
                        bundleCheckboxes.put(bundle.key, checkBox);
                        checksPanel.add(checkBox);
                }

                JPanel topAlignedChecks = new JPanel(new BorderLayout());
                topAlignedChecks.setBackground(Color.WHITE);
                topAlignedChecks.add(checksPanel, BorderLayout.NORTH);

                JScrollPane scrollPane = new JScrollPane(topAlignedChecks);
                scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
                scrollPane.getVerticalScrollBar().setUnitIncrement(18);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                tabContent.add(scrollPane, BorderLayout.CENTER);
                return tabContent;
        }

        private JPanel createPermissionActions(List<PermissionBundle> bundles) {
                JPanel actionsPanel = new JPanel(new BorderLayout());
                actionsPanel.setBackground(Color.WHITE);

                JLabel instruction = new JLabel("Cada casilla activa un grupo completo de permisos relacionados.");
                instruction.setFont(new Font("Arial", Font.PLAIN, 13));
                instruction.setForeground(new Color(80, 80, 80));
                actionsPanel.add(instruction, BorderLayout.WEST);

                JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                buttonsPanel.setBackground(Color.WHITE);
                JButton selectAllButton = createPermissionActionButton("Seleccionar todo", new Color(230, 240, 250));
                JButton clearButton = createPermissionActionButton("Ninguno", new Color(250, 230, 230));
                selectAllButton.addActionListener(event -> setBundleSelections(bundles, true));
                clearButton.addActionListener(event -> setBundleSelections(bundles, false));
                buttonsPanel.add(selectAllButton);
                buttonsPanel.add(clearButton);
                actionsPanel.add(buttonsPanel, BorderLayout.EAST);
                return actionsPanel;
        }

        private JButton createPermissionActionButton(String text, Color background) {
                JButton button = new JButton(text);
                button.setFont(new Font("Arial", Font.PLAIN, 12));
                button.setBackground(background);
                button.setFocusable(false);
                permissionActionButtons.add(button);
                return button;
        }

        private void setBundleSelections(List<PermissionBundle> bundles, boolean selected) {
                for (PermissionBundle bundle : bundles) {
                        JCheckBox checkBox = bundleCheckboxes.get(bundle.key);
                        if (checkBox != null) {
                                checkBox.setSelected(selected);
                        }
                        applyBundleSelection(bundle, selected);
                }
                m_Dirty.setDirty(true);
        }

        private void applyBundleSelection(PermissionBundle bundle, boolean selected) {
                for (PermissionInfo permission : bundle.permissions) {
                        if (selected) {
                                selectedPermissionNames.add(permission.getClassName());
                        } else {
                                selectedPermissionNames.remove(permission.getClassName());
                        }
                }
        }

        private void syncBundleCheckboxes() {
                for (PermissionBundle bundle : permissionBundles.values()) {
                        boolean hasAnyPermission = false;
                        for (PermissionInfo permission : bundle.permissions) {
                                if (selectedPermissionNames.contains(permission.getClassName())) {
                                        hasAnyPermission = true;
                                        break;
                                }
                        }
                        JCheckBox checkBox = bundleCheckboxes.get(bundle.key);
                        if (checkBox != null) {
                                checkBox.setSelected(hasAnyPermission);
                        }
                }
        }

        private void cleanFields() {
                m_oId = null;
                m_sPassword = null;
                m_currentRoleId = null;
                m_jName.setText(null);
                m_jVisible.setSelected(false);
                m_jcard.setText(null);
                m_jImage.setImage(null);

                clearPermissions();

                if (permissionsTabbedPane.getTabCount() > 0) {
                        permissionsTabbedPane.setSelectedIndex(0);
                }
        }

        private void disableFields() {
                m_jName.setEnabled(false);
                m_jVisible.setEnabled(false);
                m_jcard.setEnabled(false);
                m_jImage.setEnabled(false);
                jButton1.setEnabled(false);
                webCBSecurity.setEnabled(false);
                setPermissionsEnabled(false);
                permissionsTabbedPane.setEnabled(false);
        }

        private void enableFields() {
                m_jName.setEnabled(true);
                m_jVisible.setEnabled(true);
                m_jcard.setEnabled(true);
                m_jImage.setEnabled(true);
                jButton1.setEnabled(true);
                webCBSecurity.setEnabled(true);
                setPermissionsEnabled(true);
                permissionsTabbedPane.setEnabled(true);
        }

        private void setPermissionsEnabled(boolean enabled) {
                for (JCheckBox cb : bundleCheckboxes.values()) {
                        cb.setEnabled(enabled);
                }
                for (JButton button : permissionActionButtons) {
                        button.setEnabled(enabled);
                }
        }

        @Override
        public void writeValueEOF() {
                cleanFields();
                disableFields();
        }

        @Override
        public void writeValueInsert() {
                cleanFields();
                m_oId = UUID.randomUUID().toString();
                m_jVisible.setSelected(true);
                enableFields();
        }

        @Override
        public void writeValueDelete(Object value) {
                Object[] people = (Object[]) value;
                m_oId = (String) people[0];
                m_jName.setText(Formats.STRING.formatValue((String) people[1]));
                m_sPassword = Formats.STRING.formatValue((String) people[2]);
                m_jVisible.setSelected(((Boolean) people[4]));
                m_jcard.setText(Formats.STRING.formatValue((String) people[5]));
                m_jImage.setImage((BufferedImage) people[6]);

                m_currentRoleId = (String) people[3];
                loadPermissionsFromRole(m_currentRoleId);

                disableFields();
        }

        @Override
        public void writeValueEdit(Object value) {
                Object[] people = (Object[]) value;
                m_oId = (String) people[0];
                m_jName.setText(Formats.STRING.formatValue((String) people[1]));
                m_sPassword = Formats.STRING.formatValue((String) people[2]);
                m_jVisible.setSelected(((Boolean) people[4]));
                m_jcard.setText(Formats.STRING.formatValue((String) people[5]));
                m_jImage.setImage((BufferedImage) people[6]);

                if (m_jcard.getText() != null && m_jcard.getText().length() == 16) {
                        jLblCardID.setText(AppLocal.getIntString("label.ibutton"));
                } else {
                        jLblCardID.setText(AppLocal.getIntString("label.card"));
                }

                m_currentRoleId = (String) people[3];
                loadPermissionsFromRole(m_currentRoleId);

                enableFields();
        }

        private void loadPermissionsFromRole(String roleId) {
                clearPermissions();
                if (roleId == null) {
                        return;
                }

                try {
                        Object roleData = new com.openbravo.data.loader.StaticSentence(
                                        dlAdmin.getSession(),
                                        "SELECT PERMISSIONS FROM ROLES WHERE ID = ?",
                                        new com.openbravo.data.loader.SerializerWriteBasic(
                                                        new com.openbravo.data.loader.Datas[] {
                                                                        com.openbravo.data.loader.Datas.STRING }),
                                        new com.openbravo.data.loader.SerializerReadBasic(
                                                        new com.openbravo.data.loader.Datas[] {
                                                                        com.openbravo.data.loader.Datas.BYTES }))
                                        .find(roleId);

                        if (roleData != null) {
                                Object[] record = (Object[]) roleData;
                                if (record[0] != null) {
                                        String xml = Formats.BYTEA.formatValue((byte[]) record[0]);
                                        loadPermissionsFromXML(xml);
                                }
                        }
                } catch (BasicException e) {
                        System.err.println("Error al cargar permisos del rol: " + e.getMessage());
                }
        }

        private void loadPermissionsFromXML(String xml) {
                clearPermissions();
                if (xml == null || xml.isEmpty()) {
                        return;
                }

                try {
                        SAXParserFactory factory = SAXParserFactory.newInstance();
                        SAXParser saxParser = factory.newSAXParser();

                        DefaultHandler handler = new DefaultHandler() {
                                @Override
                                public void startElement(String uri, String localName, String qName,
                                                Attributes attributes) {
                                        if ("class".equals(qName)) {
                                                String className = attributes.getValue("name");
                                                if (className != null) {
                                                        selectedPermissionNames.add(className);
                                                }
                                        }
                                }
                        };

                        saxParser.parse(new InputSource(new StringReader(xml)), handler);
                        syncBundleCheckboxes();
                } catch (Exception e) {
                        System.err.println("Error al parsear XML de permisos: " + e.getMessage());
                }
        }

        private void clearPermissions() {
                selectedPermissionNames.clear();
                for (JCheckBox checkBox : bundleCheckboxes.values()) {
                        checkBox.setSelected(false);
                }
        }

        private String generatePermissionsXML() {
                StringBuilder xml = new StringBuilder();
                xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                xml.append("<permissions>\n");

                for (String permissionName : selectedPermissionNames) {
                        xml.append("  <class name=\"").append(permissionName).append("\"/>\n");
                }

                xml.append("</permissions>");
                return xml.toString();
        }

        @Override
        public Object createValue() throws BasicException {
                Object[] people = new Object[11];
                String cardText = m_jcard.getText();
                boolean hasCard = cardText != null && cardText.length() > 0;
                String computedId = hasCard ? cardText : (m_oId == null ? UUID.randomUUID().toString() : m_oId);

                // Guardar permisos en un rol personalizado para este usuario
                String roleId = savePermissionsToCustomRole(computedId, m_jName.getText());

                people[0] = computedId;
                people[1] = Formats.STRING.parseValue(m_jName.getText());
                people[2] = Formats.STRING.parseValue(m_sPassword);
                people[3] = roleId; // Rol personalizado con los permisos seleccionados
                people[4] = m_jVisible.isSelected();
                people[5] = Formats.STRING.parseValue(cardText);
                people[6] = m_jImage.getImage();
                people[7] = null;
                people[8] = null;
                people[9] = null;
                people[10] = null;

                return people;
        }

        /**
         * Guarda los permisos seleccionados en un rol personalizado para este usuario.
         * Si el usuario ya tiene un rol custom, lo actualiza. Si no, crea uno nuevo.
         * Si tiene un rol estándar (1=ADMIN, 2=MANAGER, 3=Employee), crea uno nuevo.
         */
        private String savePermissionsToCustomRole(String userId, String userName) throws BasicException {
                String permXml = generatePermissionsXML();
                byte[] permBytes = permXml.getBytes(java.nio.charset.StandardCharsets.UTF_8);

                // Determinar el roleId a usar
                String roleId = m_currentRoleId;
                boolean isStandardRole = roleId == null || "1".equals(roleId) || "2".equals(roleId)
                                || "3".equals(roleId);

                if (isStandardRole) {
                        // Crear un rol personalizado con ID único para este usuario
                        roleId = "custom_" + userId;
                }

                String roleName = "Custom_" + (userName != null ? userName : userId);

                // Verificar si el rol ya existe
                Object existing = new com.openbravo.data.loader.StaticSentence(
                                dlAdmin.getSession(),
                                "SELECT ID FROM roles WHERE ID = ?",
                                new com.openbravo.data.loader.SerializerWriteBasic(
                                                new com.openbravo.data.loader.Datas[] {
                                                                com.openbravo.data.loader.Datas.STRING }),
                                new com.openbravo.data.loader.SerializerReadBasic(
                                                new com.openbravo.data.loader.Datas[] {
                                                                com.openbravo.data.loader.Datas.STRING }))
                                .find(roleId);

                if (existing != null) {
                        // Actualizar el rol existente con los nuevos permisos
                        new com.openbravo.data.loader.PreparedSentence(
                                        dlAdmin.getSession(),
                                        "UPDATE roles SET NAME = ?, PERMISSIONS = ? WHERE ID = ?",
                                        new com.openbravo.data.loader.SerializerWriteBasic(
                                                        new com.openbravo.data.loader.Datas[] {
                                                                        com.openbravo.data.loader.Datas.STRING,
                                                                        com.openbravo.data.loader.Datas.BYTES,
                                                                        com.openbravo.data.loader.Datas.STRING }))
                                        .exec(new Object[] { roleName, permBytes, roleId });
                } else {
                        // Insertar nuevo rol
                        new com.openbravo.data.loader.PreparedSentence(
                                        dlAdmin.getSession(),
                                        "INSERT INTO roles (ID, NAME, PERMISSIONS) VALUES (?, ?, ?)",
                                        new com.openbravo.data.loader.SerializerWriteBasic(
                                                        new com.openbravo.data.loader.Datas[] {
                                                                        com.openbravo.data.loader.Datas.STRING,
                                                                        com.openbravo.data.loader.Datas.STRING,
                                                                        com.openbravo.data.loader.Datas.BYTES }))
                                        .exec(new Object[] { roleId, roleName, permBytes });
                }

                m_currentRoleId = roleId;
                return roleId;
        }

        @Override
        public Component getComponent() {
                return this;
        }

        public void activate() throws BasicException {
                m_RoleModel = new ComboBoxValModel<>(m_sentrole.list());
        }

        @Override
        public void refresh() {
        }

        private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
                String sNewPassword = JPasswordDialog.changePassword(this);
                if (sNewPassword != null) {
                        m_sPassword = sNewPassword;
                        m_Dirty.setDirty(true);
                }
        }

        private void webCBSecurityActionPerformed(java.awt.event.ActionEvent evt) {
                if (webCBSecurity.getSelectedIndex() == 0) {
                        if (JOptionPane.showConfirmDialog(this,
                                        AppLocal.getIntString("message.cardnew"),
                                        AppLocal.getIntString("title.editor"),
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                                m_jcard.setText("C" + StringUtils.getCardNumber());
                                m_Dirty.setDirty(true);
                        }
                }

                if (webCBSecurity.getSelectedIndex() == 1) {
                        if (JOptionPane.showConfirmDialog(this,
                                        AppLocal.getIntString("message.cardremove"),
                                        AppLocal.getIntString("title.editor"),
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                                m_jcard.setText(null);
                                m_Dirty.setDirty(true);
                        }
                }
        }

        private void jLabel1MouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                        String uuidString = m_oId.toString();
                        StringSelection stringSelection = new StringSelection(uuidString);
                        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clpbrd.setContents(stringSelection, null);

                        JOptionPane.showMessageDialog(null,
                                        AppLocal.getIntString("message.uuidcopy"));
                }
        }
}
