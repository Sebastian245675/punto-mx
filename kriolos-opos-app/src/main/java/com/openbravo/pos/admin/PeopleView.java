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
import javax.swing.border.TitledBorder;
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

        // Panel de permisos con tabs
        private JTabbedPane permissionsTabbedPane;
        private final Map<String, JCheckBox> permissionCheckboxes = new HashMap<>();

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
                setPreferredSize(new Dimension(800, 600));

                jTabbedPane1.setMinimumSize(new Dimension(750, 550));
                jTabbedPane1.setPreferredSize(new Dimension(780, 580));

                // General Panel Layout
                generalPanel.setLayout(new BorderLayout(0, 15));
                generalPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
                generalPanel.setBackground(Color.WHITE);

                // Header Title
                JLabel headerTitle = new JLabel("GESTIÓN DE USUARIO");
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
                permissionsTabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
                permissionsTabbedPane.setFont(new Font("Arial", Font.BOLD, 12));
                permissionsTabbedPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

                generalPanel.add(permissionsTabbedPane, BorderLayout.CENTER);

                jTabbedPane1.addTab(AppLocal.getIntString("label.general"), generalPanel);

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
                Map<String, List<PermissionInfo>> allPermissions = PermissionsCatalog.getAllPermissions();

                for (Map.Entry<String, List<PermissionInfo>> entry : allPermissions.entrySet()) {
                        String category = entry.getKey();
                        List<PermissionInfo> permissions = entry.getValue();

                        // Panel para la pestaña
                        JPanel tabContent = new JPanel(new BorderLayout());
                        tabContent.setBackground(Color.WHITE);
                        tabContent.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

                        // Opciones de acción masiva
                        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
                        actionsPanel.setBackground(Color.WHITE);

                        JButton selectAllBtn = new JButton("Seleccionar Todo");
                        selectAllBtn.setFont(new Font("Arial", Font.PLAIN, 11));
                        selectAllBtn.setBackground(new Color(230, 240, 250));
                        selectAllBtn.addActionListener(e -> {
                                for (PermissionInfo perm : permissions) {
                                        JCheckBox cb = permissionCheckboxes.get(perm.getClassName());
                                        if (cb != null)
                                                cb.setSelected(true);
                                }
                                m_Dirty.setDirty(true);
                        });

                        JButton deselectAllBtn = new JButton("Ninguno");
                        deselectAllBtn.setFont(new Font("Arial", Font.PLAIN, 11));
                        deselectAllBtn.setBackground(new Color(250, 230, 230));
                        deselectAllBtn.addActionListener(e -> {
                                for (PermissionInfo perm : permissions) {
                                        JCheckBox cb = permissionCheckboxes.get(perm.getClassName());
                                        if (cb != null)
                                                cb.setSelected(false);
                                }
                                m_Dirty.setDirty(true);
                        });

                        actionsPanel.add(selectAllBtn);
                        actionsPanel.add(deselectAllBtn);
                        tabContent.add(actionsPanel, BorderLayout.NORTH);

                        // Checkboxes en Grid
                        JPanel checksPanel = new JPanel(new GridLayout(0, 2, 10, 5)); // 2 columnas
                        checksPanel.setBackground(Color.WHITE);

                        for (PermissionInfo perm : permissions) {
                                JCheckBox checkBox = new JCheckBox(perm.getDisplayName());
                                checkBox.setFont(new Font("Arial", Font.PLAIN, 13));
                                checkBox.setBackground(Color.WHITE);
                                checkBox.setCursor(new Cursor(Cursor.HAND_CURSOR));
                                checkBox.addActionListener(e -> m_Dirty.setDirty(true));

                                permissionCheckboxes.put(perm.getClassName(), checkBox);
                                checksPanel.add(checkBox);
                        }

                        // Wrapper para alinear arriba
                        JPanel checksWrapper = new JPanel(new BorderLayout());
                        checksWrapper.setBackground(Color.WHITE);
                        checksWrapper.add(checksPanel, BorderLayout.NORTH);

                        JScrollPane scrollPane = new JScrollPane(checksWrapper);
                        scrollPane.setBorder(null);
                        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

                        tabContent.add(scrollPane, BorderLayout.CENTER);

                        permissionsTabbedPane.addTab(category, tabContent);
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

                for (JCheckBox cb : permissionCheckboxes.values()) {
                        cb.setSelected(false);
                }

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
                for (JCheckBox cb : permissionCheckboxes.values()) {
                        cb.setEnabled(enabled);
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
                if (xml == null || xml.isEmpty()) {
                        return;
                }

                for (JCheckBox cb : permissionCheckboxes.values()) {
                        cb.setSelected(false);
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
                                                        JCheckBox cb = permissionCheckboxes.get(className);
                                                        if (cb != null) {
                                                                cb.setSelected(true);
                                                        }
                                                }
                                        }
                                }
                        };

                        saxParser.parse(new InputSource(new StringReader(xml)), handler);
                } catch (Exception e) {
                        System.err.println("Error al parsear XML de permisos: " + e.getMessage());
                }
        }

        private String generatePermissionsXML() {
                StringBuilder xml = new StringBuilder();
                xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                xml.append("<permissions>\n");

                for (Map.Entry<String, JCheckBox> entry : permissionCheckboxes.entrySet()) {
                        if (entry.getValue().isSelected()) {
                                xml.append("  <class name=\"").append(entry.getKey()).append("\"/>\n");
                        }
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
