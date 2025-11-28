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

import com.openbravo.data.gui.ListCellRendererBasic;
import com.openbravo.data.loader.ComparatorCreator;
import com.openbravo.data.loader.TableDefinition;
import com.openbravo.data.loader.Vectorer;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.data.user.ListProvider;
import com.openbravo.data.user.ListProviderCreator;
import com.openbravo.data.user.DefaultSaveProvider;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.panels.JPanelTable;
import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.loader.StaticSentence;
import com.openbravo.data.loader.SerializerReadBasic;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.data.loader.Datas;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 *
 * @author adrianromero
 */
public class RolesPanel extends JPanelTable {

    private TableDefinition troles;
    private TableDefinition trolesmenu;
    private RolesView jeditor;
    private DataLogicAdmin dlAdmin;

    private JList<String> defaultRolesList;
    private JList<String> usersList;
    private DefaultListModel<String> defaultRolesModel;
    private DefaultListModel<String> usersModel;

    public RolesPanel() {
    }

    @Override
    protected void init() {
        dlAdmin = (DataLogicAdmin) app.getBean("com.openbravo.pos.admin.DataLogicAdmin");
        troles = dlAdmin.getTableRoles();
        jeditor = new RolesView(dirty, dlAdmin);

        // Inicializar roles predeterminados con sus permisos específicos
        try {
            DefaultRolesInitializer.initializeDefaultRoles(dlAdmin.getSession());
        } catch (Exception ex) {
            System.err.println("Error al inicializar roles predeterminados: " + ex.getMessage());
        }

        // Crear las listas personalizadas
        createCustomRolesLists();
    }

    /**
     * Crea dos listas: una para roles predeterminados y otra para usuarios
     */
    private void createCustomRolesLists() {
        // Modelos de datos
        defaultRolesModel = new DefaultListModel<>();
        usersModel = new DefaultListModel<>();

        // Agregar roles predeterminados
        defaultRolesModel.addElement("ADMIN");
        defaultRolesModel.addElement("MANAGER");
        defaultRolesModel.addElement("Employee");

        // Lista de roles predeterminados (altura fija para 3 elementos)
        defaultRolesList = new JList<>(defaultRolesModel);
        defaultRolesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        defaultRolesList.setFont(new Font("Arial", Font.PLAIN, 14));
        defaultRolesList.setFixedCellHeight(30);
        defaultRolesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && defaultRolesList.getSelectedValue() != null) {
                usersList.clearSelection();
                loadRoleData(defaultRolesList.getSelectedValue());
            }
        });

        // Lista de usuarios (altura variable)
        usersList = new JList<>(usersModel);
        usersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        usersList.setFont(new Font("Arial", Font.PLAIN, 14));
        usersList.setFixedCellHeight(30);
        usersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && usersList.getSelectedValue() != null) {
                defaultRolesList.clearSelection();
                String selectedUser = usersList.getSelectedValue();
                loadUserRoleData(selectedUser);
            }
        });

        // Cargar usuarios desde la base de datos de forma asíncrona
        SwingUtilities.invokeLater(() -> loadUsers());
    }

    /**
     * Carga los usuarios desde la base de datos
     * Método público para poder refrescar la lista desde otras partes del código
     */
    public void loadUsers() {
        try {
            System.out.println("Iniciando carga de usuarios...");

            if (dlAdmin == null) {
                System.err.println("dlAdmin es null");
                return;
            }

            if (dlAdmin.getSession() == null) {
                System.err.println("Session es null");
                return;
            }

            System.out.println("Ejecutando query de usuarios...");

            SentenceList usersQuery = new StaticSentence(
                    dlAdmin.getSession(),
                    "SELECT name FROM people ORDER BY name",
                    null,
                    new SerializerReadBasic(new Datas[] { Datas.STRING }));

            List usersList = usersQuery.list();
            System.out.println("Usuarios encontrados: " + (usersList != null ? usersList.size() : 0));

            if (usersList != null) {
                usersModel.clear();
                for (Object row : usersList) {
                    Object[] userData = (Object[]) row;
                    String userName = (String) userData[0];
                    if (userName != null && !userName.trim().isEmpty()) {
                        System.out.println("Agregando usuario: " + userName);
                        usersModel.addElement(userName);
                    }
                }
                System.out.println("Total de usuarios cargados: " + usersModel.getSize());
            }
        } catch (BasicException ex) {
            System.err.println("Error al cargar usuarios: " + ex.getMessage());
            ex.printStackTrace();
        } catch (Exception ex) {
            System.err.println("Error inesperado al cargar usuarios: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Carga el rol asignado a un usuario y muestra sus permisos (editable)
     */
    private void loadUserRoleData(String userName) {
        try {
            if (dlAdmin == null || dlAdmin.getSession() == null) {
                return;
            }

            System.out.println("Cargando rol del usuario: " + userName);

            // Obtener el rol del usuario
            SentenceList userRoleQuery = new StaticSentence(
                    dlAdmin.getSession(),
                    "SELECT role FROM people WHERE name = ?",
                    new SerializerWriteBasic(new Datas[] { Datas.STRING }),
                    new SerializerReadBasic(new Datas[] { Datas.STRING }));

            List result = userRoleQuery.list(new Object[] { userName });
            if (result != null && !result.isEmpty()) {
                Object[] userData = (Object[]) result.get(0);
                String roleId = (String) userData[0];
                System.out.println("Rol ID del usuario " + userName + ": " + roleId);

                if (roleId != null && !roleId.trim().isEmpty()) {
                    // Convertir ID a nombre de rol
                    String roleName = mapRoleIdToName(roleId);
                    System.out.println("Rol nombre: " + roleName);

                    // Establecer el nombre del usuario en el editor
                    if (jeditor instanceof RolesView) {
                        ((RolesView) jeditor).setUserName(userName);
                    }

                    // Cargar permisos del rol (ahora es editable si no es ADMIN)
                    loadRoleData(roleName);
                }
            }
        } catch (BasicException ex) {
            System.err.println("Error al cargar rol del usuario: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Convierte el ID del rol a nombre de rol
     */
    private String mapRoleIdToName(String roleId) {
        if (roleId == null || roleId.trim().isEmpty()) {
            return "Employee";
        }

        switch (roleId.trim()) {
            case "0":
                return "ADMIN";
            case "1":
                return "MANAGER";
            case "2":
            case "3":
                return "Employee";
            default:
                return roleId; // Ya es un nombre o rol personalizado
        }
    }

    /**
     * Carga los datos del rol seleccionado en el editor
     */
    private void loadRoleData(String roleName) {
        try {
            System.out.println("=== CARGANDO ROL: " + roleName + " ===");

            // Limpiar el nombre del usuario (solo se mostrará cuando se seleccione un
            // usuario)
            if (jeditor instanceof RolesView) {
                ((RolesView) jeditor).setUserName(null);
            }

            if (dlAdmin == null) {
                System.err.println("ERROR: dlAdmin es null");
                return;
            }

            if (dlAdmin.getSession() == null) {
                System.err.println("ERROR: Session es null");
                return;
            }

            System.out.println("Ejecutando query para rol: " + roleName);

            StaticSentence roleQuery = new StaticSentence(
                    dlAdmin.getSession(),
                    "SELECT id, name, permissions FROM roles WHERE name = ?",
                    new SerializerWriteBasic(new Datas[] { Datas.STRING }),
                    new SerializerReadBasic(new Datas[] { Datas.STRING, Datas.STRING, Datas.BYTES }));

            List result = roleQuery.list(new Object[] { roleName });
            System.out.println("Resultado de query: " + (result != null ? result.size() + " filas" : "null"));

            if (result != null && !result.isEmpty()) {
                Object[] roleData = (Object[]) result.get(0);
                System.out.println("ID: " + roleData[0]);
                System.out.println("Nombre: " + roleData[1]);
                System.out.println("Permissions bytes: "
                        + (roleData[2] != null ? ((byte[]) roleData[2]).length + " bytes" : "null"));

                System.out.println("Llamando a jeditor.writeValueEdit()...");
                jeditor.writeValueEdit(roleData);
                System.out.println("jeditor.writeValueEdit() completado");
            } else {
                System.err.println("ERROR: No se encontró el rol en la base de datos");
            }
        } catch (BasicException ex) {
            System.err.println("ERROR BasicException al cargar datos del rol: " + ex.getMessage());
            ex.printStackTrace();
        } catch (Exception ex) {
            System.err.println("ERROR inesperado al cargar datos del rol: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public Component getFilter() {
        // No usar filter, dejar que las listas se agreguen en LINE_START
        return null;
    }

    /**
     * Sobrescribimos para agregar nuestras listas personalizadas en lugar del
     * JListNavigator
     */
    @Override
    public JComponent getComponent() {
        // Verificar si ya se agregaron las listas
        if (defaultRolesList != null && defaultRolesList.getParent() == null) {
            // Buscar el contenedor y agregar nuestras listas
            SwingUtilities.invokeLater(() -> {
                addCustomListsToContainer();
            });
        }
        return super.getComponent();
    }

    /**
     * Agrega las listas personalizadas al contenedor principal
     */
    private void addCustomListsToContainer() {
        // Buscar el container donde normalmente se agrega el JListNavigator
        Container parent = this;
        Component[] components = parent.getComponents();

        // Buscar el panel "container" que tiene BorderLayout
        JPanel containerPanel = null;
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                if (panel.getLayout() instanceof BorderLayout) {
                    containerPanel = panel;
                    break;
                }
            }
        }

        if (containerPanel != null) {
            // Panel contenedor para las dos listas
            JPanel listsPanel = new JPanel();
            listsPanel.setLayout(new BoxLayout(listsPanel, BoxLayout.Y_AXIS));
            listsPanel.setPreferredSize(new Dimension(200, 0));

            // Panel para roles predeterminados (altura fija: 3 elementos * 30px + bordes)
            JPanel defaultPanel = new JPanel(new BorderLayout());
            defaultPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(),
                    "Roles del Sistema",
                    TitledBorder.LEFT,
                    TitledBorder.TOP,
                    new Font("Arial", Font.BOLD, 12)));
            defaultPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
            defaultPanel.setPreferredSize(new Dimension(200, 130));

            JScrollPane defaultScrollPane = new JScrollPane(defaultRolesList);
            defaultScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            defaultPanel.add(defaultScrollPane, BorderLayout.CENTER);

            // Panel para usuarios (ocupa el resto del espacio)
            JPanel usersPanel = new JPanel(new BorderLayout());

            // Crear TitledBorder personalizado con botón
            TitledBorder usersBorder = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(),
                    "Usuarios   ", // Espacios adicionales para el botón
                    TitledBorder.LEFT,
                    TitledBorder.TOP,
                    new Font("Arial", Font.BOLD, 12));
            usersPanel.setBorder(usersBorder);

            // Botón de refrescar (pequeño, superpuesto en la esquina)
            JButton btnRefresh = new JButton("🔄");
            btnRefresh.setFont(new Font("Arial", Font.PLAIN, 12));
            btnRefresh.setPreferredSize(new Dimension(30, 20));
            btnRefresh.setMargin(new Insets(0, 0, 0, 0));
            btnRefresh.setToolTipText("Refrescar lista de usuarios");
            btnRefresh.setFocusPainted(false);
            btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnRefresh.addActionListener(e -> {
                loadUsers();
            });

            // Panel contenedor con OverlayLayout para superponer el botón
            JLayeredPane layeredPane = new JLayeredPane();
            layeredPane.setLayout(null);

            JScrollPane usersScrollPane = new JScrollPane(usersList);
            usersScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

            // Configurar bounds para que el scroll ocupe todo el espacio
            usersScrollPane.setBounds(0, 0, 200, 300);
            btnRefresh.setBounds(165, 3, 30, 20); // Posición fija en la esquina superior derecha

            layeredPane.add(usersScrollPane, JLayeredPane.DEFAULT_LAYER);
            layeredPane.add(btnRefresh, JLayeredPane.PALETTE_LAYER);

            usersPanel.add(layeredPane, BorderLayout.CENTER);

            // Listener para ajustar el tamaño cuando cambie el panel
            usersPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
                public void componentResized(java.awt.event.ComponentEvent evt) {
                    Dimension size = usersPanel.getSize();
                    Insets insets = usersPanel.getInsets();
                    int width = size.width - insets.left - insets.right;
                    int height = size.height - insets.top - insets.bottom;

                    layeredPane.setPreferredSize(new Dimension(width, height));
                    usersScrollPane.setBounds(0, 0, width, height);
                    btnRefresh.setBounds(width - 35, 3, 30, 20);

                    layeredPane.revalidate();
                    layeredPane.repaint();
                }
            });

            // Agregar ambos paneles al contenedor
            listsPanel.add(defaultPanel);
            listsPanel.add(Box.createVerticalStrut(5)); // Pequeño espacio entre listas
            listsPanel.add(usersPanel);

            // Agregar al contenedor en la posición LINE_START (izquierda)
            containerPanel.add(listsPanel, BorderLayout.LINE_START);
            containerPanel.revalidate();
            containerPanel.repaint();
        }
    }

    @Override
    public ListProvider getListProvider() {
        return new ListProviderCreator(troles);
    }

    @Override
    public DefaultSaveProvider getSaveProvider() {
        return new DefaultSaveProvider(troles);
    }

    @Override
    public Vectorer getVectorer() {
        return troles.getVectorerBasic(new int[] { 1 });
    }

    @Override
    public ComparatorCreator getComparatorCreator() {
        return troles.getComparatorCreator(new int[] { 1 });
    }

    @Override
    public ListCellRenderer getListCellRenderer() {
        // Retornar null para desactivar el JListNavigator por defecto
        // Usamos nuestras listas personalizadas en getFilter()
        return null;
    }

    @Override
    public EditorRecord getEditor() {
        return jeditor;
    }

    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.Roles");
    }
}
