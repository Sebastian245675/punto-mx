/*
 * Sebastian POS - Pantalla de Login Modernizada
 */
package com.openbravo.pos.forms;

import com.openbravo.basic.BasicException;
import com.openbravo.beans.JFlowPanel;
import com.openbravo.beans.JPasswordDialog;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.Session;
import com.openbravo.data.loader.StaticSentence;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.data.loader.SerializerReadBasic;
import com.openbravo.data.loader.Datas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Pantalla de Login Modernizada por Sebastian
 * 
 * @author Sebastian
 */
public class JAuthPanel extends javax.swing.JPanel {

    private static final Logger LOGGER = Logger.getLogger(JAuthPanel.class.getName());
    private static final long serialVersionUID = 1L;

    // Colores modernos - Estilo azul vibrante
    private static final Color PRIMARY_BLUE = new Color(59, 130, 246); // Azul vibrante principal
    private static final Color PRIMARY_BLUE_DARK = new Color(37, 99, 235); // Azul oscuro
    private static final Color PRIMARY_BLUE_LIGHT = new Color(96, 165, 250); // Azul claro
    private static final Color BACKGROUND_DARK = new Color(55, 65, 81); // Fondo oscuro gris
    private static final Color BACKGROUND_DARKER = new Color(31, 41, 55); // Fondo más oscuro
    private static final Color CARD_BLUE = new Color(59, 130, 246); // Panel azul principal
    private static final Color CARD_BLUE_LIGHT = new Color(96, 165, 250); // Panel azul claro (gradiente)
    private static final Color TEXT_WHITE = new Color(255, 255, 255); // Texto blanco
    private static final Color TEXT_GRAY_LIGHT = new Color(229, 231, 235); // Texto gris claro
    private static final Color INPUT_BG = new Color(255, 255, 255); // Fondo de inputs (blanco)
    private static final Color BORDER_LIGHT = new Color(229, 231, 235); // Borde claro
    private static final Color BUTTON_YELLOW = new Color(255, 193, 7); // Botón amarillo brillante
    private static final Color BUTTON_YELLOW_HOVER = new Color(255, 213, 79); // Botón amarillo hover
    private static final Color BUTTON_YELLOW_TEXT = new Color(0, 0, 0); // Texto negro en botón amarillo
    private static final Color LINK_YELLOW = new Color(255, 213, 79); // Link amarillo
    private static final Color ERROR_RED = new Color(239, 68, 68); // Rojo para errores

    private StringBuilder inputtext;

    private final DataLogicSystem m_dlSystem;
    private final Session m_session;
    private final AuthListener authListener;

    // Campos de login
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private javax.swing.JComboBox<String> usernameCombo; // Selector avanzado de usuarios
    private java.util.List<String> recentUsers; // Lista de usuarios recientes

    public JAuthPanel(DataLogicSystem dlSystem, Session session, AuthListener authcListener) {

        authListener = authcListener;
        m_dlSystem = dlSystem;
        m_session = session;

        initComponents();
        initPanel();
    }

    private void initPanel() {
        // Aplicar diseño moderno al panel principal con fondo oscuro
        setBackground(BACKGROUND_DARKER);

        // Ocultar la lista de usuarios y mostrar el formulario de login
        usersLisScrollPane.setVisible(false);

        // Cargar historial de usuarios
        loadRecentUsers();

        // Crear y configurar el formulario de login
        setupLoginForm();

        inputtext = new StringBuilder();
        m_txtKeys.setText(null);

        // Focus con delay para mejor UX
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (txtUsername != null) {
                    txtUsername.requestFocus();
                }
            }
        });
    }

    /**
     * Carga el historial de usuarios desde las preferencias
     */
    private void loadRecentUsers() {
        recentUsers = new java.util.ArrayList<>();
        try {
            com.openbravo.pos.forms.AppConfig config = com.openbravo.pos.forms.AppConfig.getInstance();
            config.load();
            String usersStr = config.getProperty("login.recent.users");
            if (usersStr != null && !usersStr.isEmpty()) {
                String[] users = usersStr.split(",");
                for (String user : users) {
                    if (user != null && !user.trim().isEmpty()) {
                        recentUsers.add(user.trim());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar usuarios recientes", e);
        }
    }

    /**
     * Guarda un usuario en el historial
     */
    private void saveUserToHistory(String username) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }

        try {
            // Remover si ya existe
            recentUsers.remove(username.trim());
            // Agregar al inicio
            recentUsers.add(0, username.trim());

            // Limitar a 10 usuarios
            if (recentUsers.size() > 10) {
                recentUsers = recentUsers.subList(0, 10);
            }

            // Guardar en preferencias
            try {
                com.openbravo.pos.forms.AppConfig config = com.openbravo.pos.forms.AppConfig.getInstance();
                config.load();
                String usersStr = String.join(",", recentUsers);
                config.setProperty("login.recent.users", usersStr);
                config.save();
            } catch (java.io.IOException e) {
                LOGGER.log(Level.WARNING, "Error al guardar configuración", e);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al guardar usuario en historial", e);
        }
    }

    private void setupLoginForm() {
        // Crear panel principal con fondo gris corporativo simple
        javax.swing.JPanel mainContainer = new javax.swing.JPanel();
        mainContainer.setLayout(new GridBagLayout());
        mainContainer.setBackground(new Color(240, 240, 240)); // Fondo gris claro corporativo
        mainContainer.setOpaque(true);

        // Crear tarjeta de login simple - solo fondo blanco
        javax.swing.JPanel loginCard = new javax.swing.JPanel();
        loginCard.setLayout(new GridBagLayout());
        loginCard.setBackground(Color.WHITE);
        loginCard.setOpaque(true);
        loginCard.setBorder(BorderFactory.createEmptyBorder(50, 60, 50, 60));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 0, 20, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Título "login" - Corporativo
        JLabel titleLabel = new JLabel("login");
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        titleLabel.setForeground(new Color(50, 50, 50));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 30, 0);
        loginCard.add(titleLabel, gbc);

        // Etiqueta "username" - Corporativo
        JLabel lblUsername = new JLabel("username");
        lblUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblUsername.setForeground(new Color(80, 80, 80));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        loginCard.add(lblUsername, gbc);

        // Campo de usuario con autocompletado avanzado (opcional)
        if (recentUsers != null && !recentUsers.isEmpty()) {
            // Si hay usuarios recientes, usar ComboBox con autocompletado
            usernameCombo = new javax.swing.JComboBox<>();
            usernameCombo.setEditable(true);
            usernameCombo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            usernameCombo.setBackground(Color.WHITE);
            usernameCombo.setForeground(new Color(17, 24, 39));
            usernameCombo.setOpaque(true);

            // Agregar usuarios recientes
            for (String user : recentUsers) {
                usernameCombo.addItem(user);
            }

            // Configurar el editor para que funcione como campo de texto
            javax.swing.JTextField editor = (javax.swing.JTextField) usernameCombo.getEditor().getEditorComponent();
            editor.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                    BorderFactory.createEmptyBorder(12, 15, 12, 15)));
            editor.setBackground(Color.WHITE);
            editor.setForeground(new Color(17, 24, 39));
            editor.setOpaque(true);

            // Efecto de foco simple
            editor.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    editor.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(59, 130, 246), 2),
                            BorderFactory.createEmptyBorder(11, 14, 11, 14)));
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    editor.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                            BorderFactory.createEmptyBorder(12, 15, 12, 15)));
                }
            });

            editor.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        txtPassword.requestFocus();
                    }
                }
            });

            txtUsername = editor; // Usar el editor como txtUsername
        } else {
            // Si no hay usuarios recientes, usar campo de texto normal
            txtUsername = new JTextField(30);
            txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            txtUsername.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                    BorderFactory.createEmptyBorder(12, 15, 12, 15)));
            txtUsername.setBackground(Color.WHITE);
            txtUsername.setForeground(new Color(17, 24, 39));
            txtUsername.setOpaque(true);

            txtUsername.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    txtUsername.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(59, 130, 246), 2),
                            BorderFactory.createEmptyBorder(11, 14, 11, 14)));
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    txtUsername.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                            BorderFactory.createEmptyBorder(12, 15, 12, 15)));
                }
            });

            txtUsername.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        txtPassword.requestFocus();
                    }
                }
            });
        }

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 20, 0);
        if (usernameCombo != null) {
            loginCard.add(usernameCombo, gbc);
        } else {
            loginCard.add(txtUsername, gbc);
        }

        // Etiqueta "password" - Corporativo
        JLabel lblPassword = new JLabel("password");
        lblPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblPassword.setForeground(new Color(80, 80, 80));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 8, 0);
        loginCard.add(lblPassword, gbc);

        // Campo de contraseña
        txtPassword = new JPasswordField(30);
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        txtPassword.setBackground(Color.WHITE);
        txtPassword.setForeground(new Color(17, 24, 39));
        txtPassword.setOpaque(true);

        txtPassword.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                txtPassword.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(59, 130, 246), 2),
                        BorderFactory.createEmptyBorder(11, 14, 11, 14)));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                txtPassword.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                        BorderFactory.createEmptyBorder(12, 15, 12, 15)));
            }
        });

        txtPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 20, 0);
        loginCard.add(txtPassword, gbc);

        // Botón "sign in" corporativo - Simple y visible
        btnLogin = new JButton("sign in");
        btnLogin.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setBackground(new Color(59, 130, 246)); // Azul corporativo
        btnLogin.setBorderPainted(false);
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnLogin.setPreferredSize(new Dimension(0, 45));
        btnLogin.setMinimumSize(new Dimension(0, 45));
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btnLogin.setOpaque(true);

        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });

        // Efecto hover simple
        btnLogin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnLogin.setBackground(new Color(37, 99, 235)); // Azul más oscuro al hover
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnLogin.setBackground(new Color(59, 130, 246)); // Azul normal
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 20, 0);
        loginCard.add(btnLogin, gbc);

        // Link "Olvidé mi contraseña" - Corporativo y visible
        JButton btnForgotPassword = new JButton("¿Olvidaste tu contraseña?");
        btnForgotPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnForgotPassword.setForeground(new Color(59, 130, 246)); // Azul corporativo
        btnForgotPassword.setContentAreaFilled(false);
        btnForgotPassword.setBorderPainted(false);
        btnForgotPassword.setFocusPainted(false);
        btnForgotPassword.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnForgotPassword.setOpaque(false);
        btnForgotPassword.setPreferredSize(new Dimension(0, 30));
        btnForgotPassword.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showForgotPasswordDialog();
            }
        });

        // Efecto hover simple
        btnForgotPassword.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnForgotPassword.setForeground(new Color(37, 99, 235));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnForgotPassword.setForeground(new Color(59, 130, 246));
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(15, 0, 0, 0);
        loginCard.add(btnForgotPassword, gbc);

        // Agregar la tarjeta al contenedor principal
        GridBagConstraints cardGbc = new GridBagConstraints();
        cardGbc.gridx = 0;
        cardGbc.gridy = 0;
        cardGbc.anchor = GridBagConstraints.CENTER;
        cardGbc.insets = new Insets(10, 10, 10, 10);
        mainContainer.add(loginCard, cardGbc);

        // Agregar el panel principal al scroll pane
        usersLisScrollPane.setViewportView(mainContainer);
        usersLisScrollPane.setVisible(true);
        usersLisScrollPane.setOpaque(false);
        usersLisScrollPane.getViewport().setOpaque(false);
    }

    private void showForgotPasswordDialog() {
        java.awt.Frame parentFrame = (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this);
        JDialogForgotPassword dialog = new JDialogForgotPassword(parentFrame, m_dlSystem);
        dialog.setVisible(true);
    }

    private void performLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty()) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                    "Por favor ingrese un usuario");
            msg.show(this);
            txtUsername.requestFocus();
            return;
        }

        try {
            // Buscar usuario (case-insensitive)
            AppUser user = m_dlSystem.findPeopleByName(username);

            // Si no lo encuentra, intentar búsqueda insensible a mayúsculas/minúsculas
            // usando SQL directo
            if (user == null && m_session != null) {
                try {
                    Object[] found = (Object[]) new StaticSentence(m_session,
                            "SELECT NAME FROM PEOPLE WHERE UPPER(NAME) = UPPER(?)",
                            new SerializerWriteBasic(new Datas[] { Datas.STRING }),
                            new SerializerReadBasic(new Datas[] { Datas.STRING }))
                            .find(username);

                    if (found != null && found[0] != null) {
                        String realName = (String) found[0];
                        LOGGER.log(Level.INFO, "Usuario encontrado con diferente casing: " + realName);
                        user = m_dlSystem.findPeopleByName(realName);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error en búsqueda case-insensitive", e);
                }
            }

            if (user == null) {
                // Intentar también con minúsculas y mayúsculas por si acaso
                LOGGER.log(Level.INFO, "Usuario no encontrado: " + username);
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                        "Usuario no encontrado.");
                msg.show(this);
                txtUsername.requestFocus();
                txtUsername.selectAll();
                return;
            }

            LOGGER.log(Level.INFO, "Usuario encontrado: " + user.getName());

            // Verificar si el usuario necesita contraseña
            if (user.authenticate()) {
                // Usuario sin contraseña, permitir acceso directo
                LOGGER.log(Level.INFO, "Usuario sin contraseña - Login exitoso: " + username);
                saveUserToHistory(user.getName()); // Guardar en historial
                authListener.onSucess(user);
            } else {
                // Usuario con contraseña, validar
                if (password.isEmpty()) {
                    MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                            "Por favor ingrese la contraseña");
                    msg.show(this);
                    txtPassword.requestFocus();
                    return;
                }

                if (user.authenticate(password)) {
                    LOGGER.log(Level.INFO, "Login exitoso: " + username);
                    saveUserToHistory(user.getName()); // Guardar en historial
                    authListener.onSucess(user);
                } else {
                    LOGGER.log(Level.INFO, "Contraseña incorrecta para: " + username);
                    MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                            AppLocal.getIntString("message.BadPassword"));
                    msg.show(this);
                    txtPassword.requestFocus();
                    txtPassword.selectAll();
                }
            }
        } catch (BasicException ex) {
            LOGGER.log(Level.WARNING, "Error al buscar usuario: ", ex);
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                    "Error al buscar usuario: " + ex.getMessage());
            msg.show(this);
        }
    }

    // Métodos showListPeople y processKey eliminados - ya no se usan con el nuevo
    // sistema de login

    public void showPanel() {

    }

    class AppUserAction extends AbstractAction {

        private static final long serialVersionUID = 1L;
        private final AppUser m_actionuser;

        public AppUserAction(AppUser user) {
            m_actionuser = user;
            putValue(Action.SMALL_ICON, m_actionuser.getIcon());
            putValue(Action.NAME, m_actionuser.getName());
            putValue(Action.SELECTED_KEY, "USER_ID_" + m_actionuser.getName());
        }

        public AppUser getUser() {
            return m_actionuser;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {

            try {
                if (m_actionuser.authenticate()) {
                    LOGGER.log(Level.INFO, "IS Logged");
                    authListener.onSucess(m_actionuser);
                } else {
                    String sPassword = JPasswordDialog.showEditor(JAuthPanel.this,
                            AppLocal.getIntString("label.Password"),
                            m_actionuser.getName(),
                            m_actionuser.getIcon());
                    if (sPassword != null) {

                        if (m_actionuser.authenticate(sPassword)) {
                            LOGGER.log(Level.INFO, "Login Success");
                            authListener.onSucess(m_actionuser);
                        } else {
                            LOGGER.log(Level.INFO, "Login failed");
                            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                                    AppLocal.getIntString("message.BadPassword"));
                            msg.show(JAuthPanel.this);
                        }
                    }
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Exception on LOGIN: ", ex);
            }
        }
    }

    public interface AuthListener {
        public void onSucess(AppUser user);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        m_vendorImageLabel = new javax.swing.JLabel();
        mainScrollPanel = new javax.swing.JScrollPane();
        jCopyRightPanel1 = new com.openbravo.pos.forms.JCopyRightPanel();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 10),
                new java.awt.Dimension(32767, 0));
        leftPanel = new javax.swing.JPanel();
        leftHeaderPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        usersLisScrollPane = new javax.swing.JScrollPane();
        leftFooterPanel = new javax.swing.JPanel();
        m_txtKeys = new javax.swing.JTextField();

        setLayout(new java.awt.BorderLayout());
        setBackground(BACKGROUND_DARKER);

        // 🎨 PANEL PRINCIPAL CON DISEÑO MODERNO
        mainPanel.setLayout(new java.awt.BorderLayout());
        mainPanel.setBackground(BACKGROUND_DARKER);

        // 🖼️ LOGO MODERNIZADO
        m_vendorImageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        m_vendorImageLabel
                .setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/app_logo_48x48.png"))); // NOI18N
        m_vendorImageLabel.setText("🚀 Sebastian POS");
        m_vendorImageLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        m_vendorImageLabel.setForeground(TEXT_WHITE);
        m_vendorImageLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        m_vendorImageLabel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        m_vendorImageLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        m_vendorImageLabel.setName("m_vendorImageLabel");
        mainPanel.add(m_vendorImageLabel, java.awt.BorderLayout.NORTH);

        // 🎯 SCROLL PANEL MODERNIZADO
        mainScrollPanel.setViewportView(jCopyRightPanel1);
        mainScrollPanel.setBorder(BorderFactory.createEmptyBorder());
        mainScrollPanel.setBackground(BACKGROUND_DARKER);

        mainPanel.add(mainScrollPanel, java.awt.BorderLayout.CENTER);
        mainPanel.add(filler2, java.awt.BorderLayout.SOUTH);

        // 👥 PANEL DE LOGIN CENTRADO (MEDIA PANTALLA)
        // Obtener dimensiones de la pantalla
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int panelWidth = (int) (screenSize.width * 0.5); // 50% del ancho de la pantalla
        int panelHeight = (int) (screenSize.height * 0.7); // 70% de la altura de la pantalla

        leftPanel.setPreferredSize(new java.awt.Dimension(panelWidth, panelHeight));
        leftPanel.setMinimumSize(new java.awt.Dimension(500, 600));
        leftPanel.setMaximumSize(new java.awt.Dimension(panelWidth, panelHeight));
        leftPanel.setLayout(new java.awt.BorderLayout());
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Crear un panel contenedor para centrar el panel de login
        javax.swing.JPanel centerContainer = new javax.swing.JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2d = (java.awt.Graphics2D) g.create();
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                        java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                java.awt.GradientPaint gp = new java.awt.GradientPaint(
                        0, 0, BACKGROUND_DARKER,
                        getWidth(), getHeight(), BACKGROUND_DARK);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        centerContainer.setLayout(new java.awt.GridBagLayout());
        centerContainer.setOpaque(false);
        centerContainer.add(leftPanel, new java.awt.GridBagConstraints());

        // Agregar el panel de login centrado en el CENTER
        add(centerContainer, java.awt.BorderLayout.CENTER);

        // 🏷️ HEADER MODERNIZADO
        leftHeaderPanel.setMinimumSize(new java.awt.Dimension(400, 80));
        leftHeaderPanel.setPreferredSize(new java.awt.Dimension(400, 80));
        leftHeaderPanel.setLayout(new java.awt.BorderLayout());
        leftHeaderPanel.setBackground(Color.WHITE);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("🔐 Iniciar Sesión");
        jLabel1.setFont(new Font("Segoe UI", Font.BOLD, 24));
        jLabel1.setForeground(TEXT_WHITE);
        jLabel1.setBorder(BorderFactory.createEmptyBorder(20, 10, 30, 10));
        jLabel1.setName("m_LoginLabel");
        leftHeaderPanel.add(jLabel1, java.awt.BorderLayout.CENTER);

        leftPanel.add(leftHeaderPanel, java.awt.BorderLayout.NORTH);

        // 📜 PANEL DE LOGIN (formulario de usuario y contraseña) - DISEÑO MODERNO
        usersLisScrollPane.setBorder(BorderFactory.createEmptyBorder());
        usersLisScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        usersLisScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        usersLisScrollPane.setFont(new java.awt.Font("Segoe UI", 0, 14));
        usersLisScrollPane.setMinimumSize(new java.awt.Dimension(450, 500));
        usersLisScrollPane.setPreferredSize(new java.awt.Dimension(500, 550));
        usersLisScrollPane.setBackground(BACKGROUND_DARKER);
        usersLisScrollPane.setOpaque(false);
        leftPanel.add(usersLisScrollPane, java.awt.BorderLayout.CENTER);

        // 🔤 CAMPO DE TEXTO MODERNIZADO (oculto)
        leftFooterPanel.setBackground(Color.WHITE);
        m_txtKeys.setPreferredSize(new java.awt.Dimension(0, 0));
        m_txtKeys.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                m_txtKeysKeyTyped(evt);
            }
        });

        javax.swing.GroupLayout leftFooterPanelLayout = new javax.swing.GroupLayout(leftFooterPanel);
        leftFooterPanel.setLayout(leftFooterPanelLayout);
        leftFooterPanelLayout.setHorizontalGroup(
                leftFooterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(leftFooterPanelLayout.createSequentialGroup()
                                .addComponent(m_txtKeys, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(350, Short.MAX_VALUE)));
        leftFooterPanelLayout.setVerticalGroup(
                leftFooterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(leftFooterPanelLayout.createSequentialGroup()
                                .addComponent(m_txtKeys, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(60, 60, 60)));

        leftPanel.add(leftFooterPanel, java.awt.BorderLayout.SOUTH);

        // El panel ya está agregado al centerContainer, no necesita agregarse aquí
    }// </editor-fold>//GEN-END:initComponents

    private void m_txtKeysKeyTyped(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_m_txtKeysKeyTyped
        // Método ya no se usa con el nuevo sistema de login tradicional
        // Se mantiene para compatibilidad con el código generado
    }// GEN-LAST:event_m_txtKeysKeyTyped

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler filler2;
    private com.openbravo.pos.forms.JCopyRightPanel jCopyRightPanel1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel leftFooterPanel;
    private javax.swing.JPanel leftHeaderPanel;
    private javax.swing.JPanel leftPanel;
    private javax.swing.JTextField m_txtKeys;
    private javax.swing.JLabel m_vendorImageLabel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JScrollPane mainScrollPanel;
    private javax.swing.JScrollPane usersLisScrollPane;
    // End of variables declaration//GEN-END:variables
}
