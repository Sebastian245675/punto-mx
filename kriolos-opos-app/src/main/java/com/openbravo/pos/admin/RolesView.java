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
    
    // Mapa para almacenar los checkboxes por className
    private final Map<String, JCheckBox> permissionCheckboxes = new HashMap<>();
    
    // Componentes UI
    private JTextField m_jName;
    private JPanel permissionsPanel;
    private JScrollPane scrollPane;
    private JButton btnSelectAll;
    private JButton btnDeselectAll;

    public RolesView(DirtyManager dirty) {
        this.dirtyManager = dirty;
        
        // IMPORTANTE: Primero inicializar el panel de permisos vacío
        permissionsPanel = new JPanel();
        permissionsPanel.setLayout(new BoxLayout(permissionsPanel, BoxLayout.Y_AXIS));
        permissionsPanel.setBackground(Color.WHITE);
        
        // Luego crear la UI principal (que usa permissionsPanel)
        initComponents();
        
        // Finalmente, poblar el panel de permisos con los checkboxes
        createPermissionsUI();
        
        writeValueEOF();
    }
    
    /**
     * Crea la interfaz de usuario con checkboxes organizados por categorías
     */
    private void createPermissionsUI() {
        // Limpiar el panel primero
        permissionsPanel.removeAll();
        
        // Obtener todas las categorías y permisos
        Map<String, List<PermissionInfo>> allPermissions = PermissionsCatalog.getAllPermissions();
        
        // Crear un panel por cada categoría
        for (Map.Entry<String, List<PermissionInfo>> entry : allPermissions.entrySet()) {
            String category = entry.getKey();
            List<PermissionInfo> permissions = entry.getValue();
            
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
        
        // IMPORTANTE: Forzar actualización visual
        permissionsPanel.revalidate();
        permissionsPanel.repaint();
        
        // También actualizar el scrollPane si está disponible
        if (scrollPane != null) {
            scrollPane.revalidate();
            scrollPane.repaint();
        }
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
        // Primero, desmarcar todos
        for (JCheckBox checkbox : permissionCheckboxes.values()) {
            checkbox.setSelected(false);
        }
        
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            return;
        }
        
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            
            DefaultHandler handler = new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    if ("class".equals(qName)) {
                        String className = attributes.getValue("name");
                        if (className != null && permissionCheckboxes.containsKey(className)) {
                            permissionCheckboxes.get(className).setSelected(true);
                        }
                    }
                }
            };
            
            parser.parse(new InputSource(new StringReader(xmlContent)), handler);
        } catch (Exception ex) {
            System.err.println("Error al parsear XML de permisos: " + ex.getMessage());
        }
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
        m_jName.setText(null);
        deselectAllPermissions();
        m_jName.setEnabled(false);
        setPermissionsEnabled(false);
    }
    
    @Override
    public void writeValueInsert() {
        m_oId = UUID.randomUUID().toString();
        m_jName.setText(null);
        deselectAllPermissions();
        m_jName.setEnabled(true);
        setPermissionsEnabled(true);
    }
    
    @Override
    public void writeValueDelete(Object value) {
        Object[] role = (Object[]) value;
        m_oId = (String)role[0];
        m_jName.setText(Formats.STRING.formatValue((String)role[1]));
        
        String permissionsXML = Formats.BYTEA.formatValue((byte[])role[2]);
        loadPermissionsFromXML(permissionsXML);
        
        m_jName.setEnabled(false);
        setPermissionsEnabled(false);
    }

    @Override
    public void writeValueEdit(Object value) {
        Object[] role = (Object[]) value;
        m_oId = (String)role[0];
        m_jName.setText(Formats.STRING.formatValue((String)role[1]));
        
        String permissionsXML = Formats.BYTEA.formatValue((byte[])role[2]);
        loadPermissionsFromXML(permissionsXML);
        
        m_jName.setEnabled(true);
        setPermissionsEnabled(true);
    }

    @Override
    public Object createValue() throws BasicException {
        Object[] role = new Object[3];
        role[0] = m_oId == null ? UUID.randomUUID().toString() : m_oId;
        role[1] = m_jName.getText();
        
        String permissionsXML = generatePermissionsXML();
        role[2] = Formats.BYTEA.parseValue(permissionsXML);
        
        return role;
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void refresh() {
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
    /**
     * Inicializa los componentes de la interfaz
     */
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel superior: Nombre del rol
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBackground(Color.WHITE);
        
        JLabel lblName = new JLabel("Nombre del Rol:");
        lblName.setFont(new Font("Arial", Font.BOLD, 14));
        
        m_jName = new JTextField(30);
        m_jName.setFont(new Font("Arial", Font.PLAIN, 14));
        m_jName.getDocument().addDocumentListener(dirtyManager);
        
        topPanel.add(lblName);
        topPanel.add(m_jName);
        
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
