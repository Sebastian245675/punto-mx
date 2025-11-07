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
import com.openbravo.data.user.DirtyManager;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.loader.StaticSentence;
import com.openbravo.data.loader.SerializerReadBasic;
import com.openbravo.data.loader.Datas;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringReader;
import java.util.*;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Vista mejorada para gestión de roles con checkboxes organizados por categorías
 * Similar al sistema de Eleventa
 * 
 * @author adrianromero
 * @author Sebastian (mejoras UI)
 */
public final class RolesView extends javax.swing.JPanel implements EditorRecord {
    
    private String m_oId;
    private final DirtyManager dirtyManager;
    private final DataLogicAdmin dlAdmin;
    
    // Mapa para almacenar los checkboxes por className
    private final Map<String, JCheckBox> permissionCheckboxes = new HashMap<>();
    
    // Componentes UI
    private JComboBox<String> m_jName;
    private JLabel m_jUserName;  // Campo para mostrar el nombre del usuario
    private JPanel permissionsPanel;
    private JScrollPane scrollPane;
    private JButton btnSelectAll;
    private JButton btnDeselectAll;

    public RolesView(DirtyManager dirty, DataLogicAdmin dlAdmin) {
        this.dirtyManager = dirty;
        this.dlAdmin = dlAdmin;
        
        try {
            // IMPORTANTE: Primero inicializar el panel de permisos vacío
            permissionsPanel = new JPanel();
            permissionsPanel.setLayout(new BoxLayout(permissionsPanel, BoxLayout.Y_AXIS));
            permissionsPanel.setBackground(Color.WHITE);
            
            // Luego crear la UI principal (que usa permissionsPanel)
            initComponents();
            
            // Finalmente, poblar el panel de permisos con los checkboxes
            createPermissionsUI();
            
            writeValueEOF();
            
            // Cargar roles existentes en el ComboBox (al final, después de todo lo demás)
            SwingUtilities.invokeLater(() -> loadExistingRoles());
            
        } catch (Exception ex) {
            System.err.println("Error al inicializar RolesView: " + ex.getMessage());
            ex.printStackTrace();
            
            // Intentar inicialización mínima
            if (permissionsPanel == null) {
                permissionsPanel = new JPanel();
            }
            if (m_jName == null) {
                m_jName = new JComboBox<>();
                m_jName.addItem("MANAGER");
                m_jName.addItem("Employee");
            }
        }
    }
    
    /**
     * Carga los roles existentes en la base de datos en el ComboBox
     * Incluye los roles predeterminados MANAGER y Employee
     */
    private void loadExistingRoles() {
        try {
            // Verificar que el ComboBox esté disponible
            if (m_jName == null) {
                System.err.println("m_jName es null, no se pueden cargar roles");
                return;
            }
            
            // Limpiar el ComboBox
            m_jName.removeAllItems();
            
            // Agregar roles predeterminados
            m_jName.addItem("MANAGER");
            m_jName.addItem("Employee");
            
            // Verificar que tenemos acceso a la sesión
            if (dlAdmin == null) {
                System.err.println("DataLogicAdmin no disponible, usando solo roles predeterminados");
                return;
            }
            
            if (dlAdmin.getSession() == null) {
                System.err.println("Session no disponible, usando solo roles predeterminados");
                return;
            }
            
            // Cargar roles desde la base de datos
            try {
                SentenceList rolesQuery = new StaticSentence(
                    dlAdmin.getSession(),
                    "SELECT name FROM roles WHERE name NOT IN ('MANAGER', 'Employee') ORDER BY name",
                    null,
                    new SerializerReadBasic(new Datas[] {Datas.STRING})
                );
                
                List rolesList = rolesQuery.list();
                if (rolesList != null && !rolesList.isEmpty()) {
                    for (Object row : rolesList) {
                        Object[] roleData = (Object[]) row;
                        String roleName = (String) roleData[0];
                        if (roleName != null && !roleName.trim().isEmpty()) {
                            m_jName.addItem(roleName);
                        }
                    }
                }
            } catch (BasicException ex) {
                System.err.println("Error al ejecutar query de roles: " + ex.getMessage());
                // No propagar el error, continuar con roles predeterminados
            }
            
        } catch (Exception ex) {
            System.err.println("Error inesperado al cargar roles: " + ex.getMessage());
            ex.printStackTrace();
            // Asegurarse de que al menos tenga los roles básicos
            try {
                if (m_jName != null && m_jName.getItemCount() == 0) {
                    m_jName.addItem("MANAGER");
                    m_jName.addItem("Employee");
                }
            } catch (Exception e2) {
                System.err.println("Error crítico al establecer roles predeterminados: " + e2.getMessage());
            }
        }
    }
    
    /**
     * Crea la interfaz de usuario con checkboxes organizados por categorías
     */
    private void createPermissionsUI() {
        System.out.println("=== createPermissionsUI() INICIADO ===");
        
        // Limpiar el panel primero
        permissionsPanel.removeAll();
        System.out.println("Panel limpiado");
        
        // Obtener todas las categorías y permisos
        Map<String, List<PermissionInfo>> allPermissions = PermissionsCatalog.getAllPermissions();
        System.out.println("Categorías obtenidas: " + allPermissions.size());
        
        int totalCheckboxes = 0;
        
        // Crear un panel por cada categoría
        for (Map.Entry<String, List<PermissionInfo>> entry : allPermissions.entrySet()) {
            String category = entry.getKey();
            List<PermissionInfo> permissions = entry.getValue();
            System.out.println("Creando categoría: " + category + " con " + permissions.size() + " permisos");
            
            // Panel para la categoría
            JPanel categoryPanel = new JPanel();
            categoryPanel.setLayout(new BoxLayout(categoryPanel, BoxLayout.Y_AXIS));
            categoryPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                category,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14),
                new Color(0, 102, 204)
            ));
            categoryPanel.setBackground(Color.WHITE);
            categoryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // Checkbox "Seleccionar todos" de la categoría
            JCheckBox selectAllCategory = new JCheckBox("✓ Seleccionar todo en " + category);
            selectAllCategory.setFont(new Font("Arial", Font.BOLD, 12));
            selectAllCategory.setBackground(new Color(240, 248, 255));
            selectAllCategory.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // Panel interno para los checkboxes individuales
            JPanel checkboxesPanel = new JPanel();
            checkboxesPanel.setLayout(new GridLayout(0, 2, 10, 5));  // 2 columnas
            checkboxesPanel.setBackground(Color.WHITE);
            checkboxesPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 10, 10));
            checkboxesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            List<JCheckBox> categoryCheckboxes = new ArrayList<>();
            
            // Crear checkbox para cada permiso
            for (PermissionInfo permission : permissions) {
                JCheckBox checkbox = new JCheckBox(permission.getDisplayName());
                checkbox.setFont(new Font("Arial", Font.PLAIN, 12));
                checkbox.setBackground(Color.WHITE);
                checkbox.addActionListener(e -> {
                    dirtyManager.setDirty(true);
                    updateSelectAllCategoryState(selectAllCategory, categoryCheckboxes);
                });
                
                permissionCheckboxes.put(permission.getClassName(), checkbox);
                categoryCheckboxes.add(checkbox);
                checkboxesPanel.add(checkbox);
                totalCheckboxes++;
            }
            
            // Acción del checkbox "Seleccionar todos" de la categoría
            selectAllCategory.addActionListener(e -> {
                boolean selected = selectAllCategory.isSelected();
                for (JCheckBox cb : categoryCheckboxes) {
                    cb.setSelected(selected);
                }
                dirtyManager.setDirty(true);
            });
            
            categoryPanel.add(selectAllCategory);
            categoryPanel.add(Box.createVerticalStrut(5));
            categoryPanel.add(checkboxesPanel);
            categoryPanel.add(Box.createVerticalStrut(10));
            
            permissionsPanel.add(categoryPanel);
            permissionsPanel.add(Box.createVerticalStrut(10));
        }
        
        System.out.println("Total de checkboxes creados: " + totalCheckboxes);
        System.out.println("permissionCheckboxes.size(): " + permissionCheckboxes.size());
        
        // IMPORTANTE: Forzar actualización visual
        permissionsPanel.revalidate();
        permissionsPanel.repaint();
        System.out.println("Panel revalidado y repintado");
        
        // También actualizar el scrollPane si está disponible
        if (scrollPane != null) {
            scrollPane.revalidate();
            scrollPane.repaint();
            System.out.println("ScrollPane revalidado y repintado");
        } else {
            System.out.println("ADVERTENCIA: scrollPane es null");
        }
        
        System.out.println("=== createPermissionsUI() COMPLETADO ===");
    }
    
    /**
     * Actualiza el estado del checkbox "Seleccionar todo" de una categoría
     */
    private void updateSelectAllCategoryState(JCheckBox selectAllCategory, List<JCheckBox> categoryCheckboxes) {
        boolean allSelected = categoryCheckboxes.stream().allMatch(JCheckBox::isSelected);
        boolean noneSelected = categoryCheckboxes.stream().noneMatch(JCheckBox::isSelected);
        
        selectAllCategory.setSelected(allSelected);
        // El estado indeterminado no es estándar en JCheckBox, simplemente lo marcamos o no
        if (!allSelected && !noneSelected) {
            selectAllCategory.setSelected(false);
        }
    }
    
    /**
     * Marca todos los checkboxes
     */
    private void selectAllPermissions() {
        for (JCheckBox checkbox : permissionCheckboxes.values()) {
            checkbox.setSelected(true);
        }
        dirtyManager.setDirty(true);
    }
    
    /**
     * Desmarca todos los checkboxes
     */
    private void deselectAllPermissions() {
        for (JCheckBox checkbox : permissionCheckboxes.values()) {
            checkbox.setSelected(false);
        }
        dirtyManager.setDirty(true);
    }
    
    /**
     * Lee los permisos desde el XML y marca los checkboxes correspondientes
     */
    private void loadPermissionsFromXML(String xmlContent) {
        System.out.println("=== loadPermissionsFromXML() INICIADO ===");
        System.out.println("Total de checkboxes disponibles: " + permissionCheckboxes.size());
        
        // Primero, desmarcar todos
        int uncheckedCount = 0;
        for (JCheckBox checkbox : permissionCheckboxes.values()) {
            checkbox.setSelected(false);
            uncheckedCount++;
        }
        System.out.println("Desmarcados: " + uncheckedCount + " checkboxes");
        
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            System.out.println("XML vacío o null, terminando");
            return;
        }
        
        System.out.println("Parseando XML...");
        
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            
            final int[] foundCount = {0};
            final int[] appliedCount = {0};
            
            DefaultHandler handler = new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    if ("class".equals(qName)) {
                        String className = attributes.getValue("name");
                        foundCount[0]++;
                        
                        if (className != null && permissionCheckboxes.containsKey(className)) {
                            permissionCheckboxes.get(className).setSelected(true);
                            appliedCount[0]++;
                            System.out.println("  ✓ Permiso aplicado: " + className);
                        } else {
                            System.out.println("  ✗ Permiso NO encontrado en checkboxes: " + className);
                        }
                    }
                }
            };
            
            parser.parse(new InputSource(new StringReader(xmlContent)), handler);
            
            System.out.println("Permisos encontrados en XML: " + foundCount[0]);
            System.out.println("Permisos aplicados a checkboxes: " + appliedCount[0]);
            
        } catch (Exception ex) {
            System.err.println("ERROR al parsear XML de permisos: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        System.out.println("=== loadPermissionsFromXML() COMPLETADO ===");
    }
    
    /**
     * Genera el XML de permisos basado en los checkboxes seleccionados
     */
    private String generatePermissionsXML() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<permissions>\n");
        
        // Recorrer todas las categorías en orden
        Map<String, List<PermissionInfo>> allPermissions = PermissionsCatalog.getAllPermissions();
        
        for (Map.Entry<String, List<PermissionInfo>> entry : allPermissions.entrySet()) {
            List<PermissionInfo> permissions = entry.getValue();
            boolean hasAnySelected = false;
            StringBuilder categoryXML = new StringBuilder();
            
            for (PermissionInfo permission : permissions) {
                JCheckBox checkbox = permissionCheckboxes.get(permission.getClassName());
                if (checkbox != null && checkbox.isSelected()) {
                    if (!hasAnySelected) {
                        categoryXML.append("\n    <!-- ").append(entry.getKey()).append(" -->\n");
                        hasAnySelected = true;
                    }
                    categoryXML.append("    <class name=\"")
                              .append(permission.getClassName())
                              .append("\"/>\n");
                }
            }
            
            if (hasAnySelected) {
                xml.append(categoryXML);
            }
        }
        
        xml.append("</permissions>\n");
        return xml.toString();
    }
    
    @Override
    public void writeValueEOF() {
        m_oId = null;
        m_jName.setSelectedItem(null);
        deselectAllPermissions();
        m_jName.setEnabled(false);
        setPermissionsEnabled(false);
    }
    
    @Override
    public void writeValueInsert() {
        m_oId = UUID.randomUUID().toString();
        m_jName.setSelectedItem(null);
        deselectAllPermissions();
        m_jName.setEnabled(true);
        setPermissionsEnabled(true);
    }
    
    @Override
    public void writeValueDelete(Object value) {
        Object[] role = (Object[]) value;
        m_oId = (String)role[0];
        m_jName.setSelectedItem(Formats.STRING.formatValue((String)role[1]));
        
        String permissionsXML = Formats.BYTEA.formatValue((byte[])role[2]);
        loadPermissionsFromXML(permissionsXML);
        
        m_jName.setEnabled(false);
        setPermissionsEnabled(false);
    }

    @Override
    public void writeValueEdit(Object value) {
        System.out.println("=== RolesView.writeValueEdit() INICIADO ===");
        
        try {
            if (value == null) {
                System.err.println("ERROR: value es null");
                return;
            }
            
            System.out.println("Tipo de value: " + value.getClass().getName());
            
            Object[] role = (Object[]) value;
            System.out.println("Array length: " + role.length);
            System.out.println("role[0] (ID): " + role[0]);
            System.out.println("role[1] (Name): " + role[1]);
            System.out.println("role[2] (Permissions): " + (role[2] != null ? ((byte[])role[2]).length + " bytes" : "null"));
            
            m_oId = (String)role[0];
            String roleName = Formats.STRING.formatValue((String)role[1]);
            System.out.println("Rol formateado: " + roleName);
            
            System.out.println("Estableciendo nombre en ComboBox...");
            m_jName.setSelectedItem(roleName);
            System.out.println("Nombre establecido en ComboBox");
            
            String permissionsXML = Formats.BYTEA.formatValue((byte[])role[2]);
            System.out.println("XML de permisos: " + (permissionsXML != null ? permissionsXML.length() + " caracteres" : "null"));
            
            if (permissionsXML != null && permissionsXML.length() > 0) {
                System.out.println("Primeros 200 caracteres del XML:");
                System.out.println(permissionsXML.substring(0, Math.min(200, permissionsXML.length())));
            }
            
            System.out.println("Llamando a loadPermissionsFromXML()...");
            loadPermissionsFromXML(permissionsXML);
            System.out.println("loadPermissionsFromXML() completado");
            
            // Verificar si es un rol predeterminado (no editable)
            boolean isDefaultRole = isDefaultRole(roleName);
            System.out.println("Es rol predeterminado: " + isDefaultRole);
            
            m_jName.setEnabled(!isDefaultRole);
            setPermissionsEnabled(!isDefaultRole);
            
            // Mostrar mensaje informativo para roles predeterminados
            if (isDefaultRole) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        "<html><b>" + roleName + "</b> es un rol predeterminado del sistema.<br>" +
                        "Sus permisos están definidos automáticamente y no pueden modificarse.<br><br>" +
                        "<b>Características de " + roleName + ":</b><br>" +
                        getRoleDescription(roleName) +
                        "</html>",
                        "Rol Predeterminado",
                        JOptionPane.INFORMATION_MESSAGE);
                });
            }
            
            System.out.println("=== RolesView.writeValueEdit() COMPLETADO ===");
            
        } catch (Exception ex) {
            System.err.println("ERROR en writeValueEdit: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Verifica si un rol es predeterminado (no editable)
     * Solo ADMIN es fijo con todos los permisos
     */
    private boolean isDefaultRole(String roleName) {
        return "ADMIN".equals(roleName);
    }
    
    /**
     * Obtiene la descripción de un rol predeterminado
     */
    private String getRoleDescription(String roleName) {
        if ("ADMIN".equals(roleName)) {
            return "• Acceso total a todas las funciones<br>• Puede configurar el sistema<br>• Puede gestionar usuarios y roles<br><br><b>Este rol NO se puede modificar</b>";
        }
        return "";
    }

    @Override
    public Object createValue() throws BasicException {
        Object[] role = new Object[3];
        role[0] = m_oId == null ? UUID.randomUUID().toString() : m_oId;
        role[1] = m_jName.getSelectedItem() != null ? m_jName.getSelectedItem().toString() : "";
        
        String permissionsXML = generatePermissionsXML();
        role[2] = Formats.BYTEA.parseValue(permissionsXML);
        
        // Logs de depuración
        System.out.println("=== GUARDANDO ROL ===");
        System.out.println("ID: " + role[0]);
        System.out.println("Nombre: " + role[1]);
        System.out.println("XML generado (primeros 500 chars): " + permissionsXML.substring(0, Math.min(500, permissionsXML.length())));
        System.out.println("Cantidad de permisos seleccionados: " + countSelectedPermissions());
        
        // Sebastian - Mostrar mensaje informativo sobre sincronización con Firebase
        SwingUtilities.invokeLater(() -> {
            showFirebaseSyncMessage();
        });
        
        return role;
    }
    
    /**
     * Cuenta cuántos permisos están seleccionados
     */
    private int countSelectedPermissions() {
        int count = 0;
        for (JCheckBox checkbox : permissionCheckboxes.values()) {
            if (checkbox.isSelected()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Muestra mensaje informativo sobre la necesidad de sincronizar con Firebase
     */
    private void showFirebaseSyncMessage() {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBackground(Color.WHITE);
        
        // Icono de información
        JLabel iconLabel = new JLabel("ℹ");
        iconLabel.setFont(new Font("Arial", Font.BOLD, 48));
        iconLabel.setForeground(new Color(33, 150, 243));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Título
        JLabel titleLabel = new JLabel("<html><b>¡Cambios Guardados Exitosamente!</b></html>");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(76, 175, 80));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Mensaje principal
        JLabel messageLabel = new JLabel("<html><div style='text-align: center; padding: 10px;'>" +
                "Los permisos del rol han sido guardados en la base de datos local.<br><br>" +
                "<b>IMPORTANTE:</b> Para que estos cambios se apliquen en todos los sistemas,<br>" +
                "debe ir a <b>Configuración → Firebase Subir</b> y sincronizar los datos.<br><br>" +
                "Solo después de la sincronización los usuarios con este rol tendrán<br>" +
                "los permisos actualizados en todas las terminales.</div></html>");
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Panel de instrucciones con fondo destacado
        JPanel instructionsPanel = new JPanel();
        instructionsPanel.setLayout(new BoxLayout(instructionsPanel, BoxLayout.Y_AXIS));
        instructionsPanel.setBackground(new Color(255, 248, 225));
        instructionsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 193, 7), 2),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        
        JLabel stepTitle = new JLabel("<html><b>📋 Pasos siguientes:</b></html>");
        stepTitle.setFont(new Font("Arial", Font.BOLD, 14));
        stepTitle.setForeground(new Color(230, 81, 0));
        stepTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel step1 = new JLabel("<html>1. Vaya al menú <b>Sistema → Configuración</b></html>");
        step1.setFont(new Font("Arial", Font.PLAIN, 12));
        step1.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel step2 = new JLabel("<html>2. Busque la opción <b>Firebase Subir</b></html>");
        step2.setFont(new Font("Arial", Font.PLAIN, 12));
        step2.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel step3 = new JLabel("<html>3. Haga clic en <b>Subir</b> para sincronizar</html>");
        step3.setFont(new Font("Arial", Font.PLAIN, 12));
        step3.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        instructionsPanel.add(stepTitle);
        instructionsPanel.add(Box.createVerticalStrut(10));
        instructionsPanel.add(step1);
        instructionsPanel.add(Box.createVerticalStrut(5));
        instructionsPanel.add(step2);
        instructionsPanel.add(Box.createVerticalStrut(5));
        instructionsPanel.add(step3);
        
        // Agregar todos los componentes al panel principal
        messagePanel.add(Box.createVerticalStrut(10));
        messagePanel.add(iconLabel);
        messagePanel.add(Box.createVerticalStrut(15));
        messagePanel.add(titleLabel);
        messagePanel.add(Box.createVerticalStrut(15));
        messagePanel.add(messageLabel);
        messagePanel.add(Box.createVerticalStrut(20));
        messagePanel.add(instructionsPanel);
        messagePanel.add(Box.createVerticalStrut(10));
        
        // Mostrar el diálogo
        JOptionPane.showMessageDialog(
            this,
            messagePanel,
            "Sincronización Pendiente con Firebase",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void refresh() {
    }
    
    /**
     * Establece el nombre del usuario que está siendo editado
     * @param userName Nombre del usuario o null para ocultar
     */
    public void setUserName(String userName) {
        if (userName != null && !userName.trim().isEmpty()) {
            m_jUserName.setText(" - Usuario: " + userName);
            m_jUserName.setVisible(true);
        } else {
            m_jUserName.setText("");
            m_jUserName.setVisible(false);
        }
    }
    
    /**
     * Habilita o deshabilita todos los checkboxes de permisos
     */
    private void setPermissionsEnabled(boolean enabled) {
        for (JCheckBox checkbox : permissionCheckboxes.values()) {
            checkbox.setEnabled(enabled);
        }
        btnSelectAll.setEnabled(enabled);
        btnDeselectAll.setEnabled(enabled);
    }
    
    /**
     * Inicializa los componentes de la interfaz
     */
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel superior: Nombre del rol y usuario
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBackground(Color.WHITE);
        
        JLabel lblName = new JLabel("Nombre del Rol:");
        lblName.setFont(new Font("Arial", Font.BOLD, 14));
        
        m_jName = new JComboBox<>();
        m_jName.setFont(new Font("Arial", Font.PLAIN, 14));
        m_jName.setEditable(true); // Permitir escribir nuevos roles
        m_jName.setPreferredSize(new Dimension(200, 30));
        
        // Campo para mostrar el nombre del usuario
        m_jUserName = new JLabel("");
        m_jUserName.setFont(new Font("Arial", Font.BOLD, 14));
        m_jUserName.setForeground(new Color(33, 150, 243));
        m_jUserName.setVisible(false); // Oculto por defecto
        
        // Agregar listener para detectar cambios (con protección de errores)
        try {
            m_jName.addActionListener(e -> {
                if (dirtyManager != null) {
                    dirtyManager.setDirty(true);
                }
            });
            
            // También agregar listener al editor del ComboBox para detectar cambios de texto
            Component editorComponent = m_jName.getEditor().getEditorComponent();
            if (editorComponent instanceof JTextField && dirtyManager != null) {
                ((JTextField) editorComponent).getDocument().addDocumentListener(dirtyManager);
            }
        } catch (Exception ex) {
            System.err.println("Error al agregar listeners al ComboBox: " + ex.getMessage());
        }
        
        topPanel.add(lblName);
        topPanel.add(m_jName);
        topPanel.add(m_jUserName); // Añadir el campo de nombre de usuario
        
        // Panel de botones
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonsPanel.setBackground(Color.WHITE);
        
        btnSelectAll = new JButton("✓ Seleccionar Todos");
        btnSelectAll.setFont(new Font("Arial", Font.BOLD, 12));
        btnSelectAll.setBackground(new Color(76, 175, 80));
        btnSelectAll.setForeground(Color.WHITE);
        btnSelectAll.setFocusPainted(false);
        btnSelectAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSelectAll.addActionListener(e -> selectAllPermissions());
        
        btnDeselectAll = new JButton("✗ Deseleccionar Todos");
        btnDeselectAll.setFont(new Font("Arial", Font.BOLD, 12));
        btnDeselectAll.setBackground(new Color(244, 67, 54));
        btnDeselectAll.setForeground(Color.WHITE);
        btnDeselectAll.setFocusPainted(false);
        btnDeselectAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDeselectAll.addActionListener(e -> deselectAllPermissions());
        
        buttonsPanel.add(btnSelectAll);
        buttonsPanel.add(btnDeselectAll);
        
        topPanel.add(buttonsPanel);
        
        // ScrollPane para los permisos
        scrollPane = new JScrollPane(permissionsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        // Panel informativo
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(new Color(255, 252, 230));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 193, 7), 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JLabel infoIcon = new JLabel("ℹ");
        infoIcon.setFont(new Font("Arial", Font.BOLD, 24));
        infoIcon.setForeground(new Color(255, 152, 0));
        
        JLabel infoText = new JLabel("<html><b>Configuración de Permisos por Rol</b><br>" +
                "Seleccione los permisos que desea asignar a este rol. " +
                "Los usuarios con este rol tendrán acceso únicamente a las funciones marcadas.</html>");
        infoText.setFont(new Font("Arial", Font.PLAIN, 12));
        
        JPanel infoContentPanel = new JPanel(new BorderLayout(10, 0));
        infoContentPanel.setBackground(new Color(255, 252, 230));
        infoContentPanel.add(infoIcon, BorderLayout.WEST);
        infoContentPanel.add(infoText, BorderLayout.CENTER);
        
        infoPanel.add(infoContentPanel);
        
        // Agregar componentes al panel principal
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.SOUTH);
    }
}
