/*
 * JLogonDialog.java
 * Diálogo de inicio de sesión personalizado
 */
package com.openbravo.pos.forms;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.Session;
import com.openbravo.data.loader.StaticSentence;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.data.loader.SerializerReadBasic;
import com.openbravo.data.loader.Datas;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.openbravo.pos.util.Hashcypher;

/**
 * Diálogo de login con diseño premium
 * 
 * @author Sebastian
 */
public class JLogonDialog extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(JLogonDialog.class.getName());

    // Colores corporativos
    private static final Color BRAND_BLUE = new Color(29, 78, 216);
    private static final Color BRAND_BLUE_LIGHT = new Color(37, 99, 235);
    private static final Color TEXT_DARK = new Color(31, 41, 55);
    private static final Color TEXT_GRAY = new Color(75, 85, 99);
    private static final Color BORDER_COLOR = new Color(209, 213, 219);
    private static final Color BACKGROUND_WHITE = Color.WHITE;

    private final DataLogicSystem m_dlSystem;
    private final Session m_session;

    private JComboBox<String> cmbUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnExit;
    private AppUser loggedUser = null;

    public JLogonDialog(java.awt.Frame parent, DataLogicSystem dlSystem, Session session) {
        super(parent, "Comenzar Nuevo Turno", true);
        this.m_dlSystem = dlSystem;
        this.m_session = session;

        initComponents();
        setupDialog();
        loadRecentUsers();
    }

    private void initComponents() {
        // Usar BorderLayout para el diálogo
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_WHITE);

        // Panel de contenido con GridBagLayout
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(BACKGROUND_WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);

        // TITULO: "Comenzar nuevo turno"
        JLabel lblTitle = new JLabel("Comenzar nuevo turno", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 36));
        lblTitle.setForeground(BRAND_BLUE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 15, 0);
        contentPanel.add(lblTitle, gbc);

        // SUBTITULO: "Por favor ingresa tu usuario..."
        JLabel lblSubTitle = new JLabel("Por favor ingresa tu usuario y contraseña para continuar.",
                SwingConstants.CENTER);
        lblSubTitle.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblSubTitle.setForeground(TEXT_GRAY);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 40, 0);
        contentPanel.add(lblSubTitle, gbc);

        // ETIQUETA USUARIO
        JLabel lblUser = new JLabel("Usuario:");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblUser.setForeground(TEXT_DARK);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 0, 5, 0);
        contentPanel.add(lblUser, gbc);

        // COMBO USUARIO
        cmbUsername = new JComboBox<>();
        cmbUsername.setEditable(true);
        cmbUsername.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        cmbUsername.setPreferredSize(new Dimension(400, 45));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 20, 0);
        contentPanel.add(cmbUsername, gbc);

        // ETIQUETA CONTRASEÑA
        JLabel lblPass = new JLabel("Contraseña:");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblPass.setForeground(TEXT_DARK);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.insets = new Insets(5, 0, 5, 0);
        contentPanel.add(lblPass, gbc);

        // PANEL CONTRASEÑA + LINK
        JPanel passPanel = new JPanel(new BorderLayout(15, 0));
        passPanel.setBackground(BACKGROUND_WHITE);

        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        txtPassword.setPreferredSize(new Dimension(300, 50));
        txtPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        });
        passPanel.add(txtPassword, BorderLayout.CENTER);

        // LINK: Olvidé mi contraseña
        JLabel lblForgot = new JLabel("<html><u>Olvide mi contraseña</u></html>");
        lblForgot.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblForgot.setForeground(BRAND_BLUE);
        lblForgot.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblForgot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showForgotPassword();
            }
        });
        passPanel.add(lblForgot, BorderLayout.EAST);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 40, 0);
        contentPanel.add(passPanel, gbc);

        // PANEL DE BOTONES
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setBackground(BACKGROUND_WHITE);
        GridBagConstraints bGbc = new GridBagConstraints();
        bGbc.fill = GridBagConstraints.BOTH;
        bGbc.weightx = 0.5;
        bGbc.insets = new Insets(0, 0, 0, 0);

        // BOTÓN ACCEDER
        btnLogin = new JButton("Acceder");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnLogin.setBackground(new Color(243, 244, 246));
        btnLogin.setBorder(BorderFactory.createLineBorder(new Color(209, 213, 219)));
        btnLogin.setPreferredSize(new Dimension(150, 45));
        btnLogin.setFocusPainted(false);
        try {
            ImageIcon lockIcon = new ImageIcon(getClass().getResource("/com/openbravo/images/password.png"));
            if (lockIcon != null) {
                btnLogin.setIcon(new ImageIcon(lockIcon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH)));
            }
        } catch (Exception e) {
        }

        btnLogin.addActionListener(e -> performLogin());
        bGbc.gridx = 0;
        bGbc.insets = new Insets(0, 0, 0, 10);
        buttonPanel.add(btnLogin, bGbc);

        // BOTÓN SALIR
        btnExit = new JButton("Salir");
        btnExit.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        btnExit.setBackground(new Color(243, 244, 246));
        btnExit.setBorder(BorderFactory.createLineBorder(new Color(209, 213, 219)));
        btnExit.setPreferredSize(new Dimension(150, 45));
        btnExit.setFocusPainted(false);
        btnExit.addActionListener(e -> System.exit(0));
        bGbc.gridx = 1;
        bGbc.insets = new Insets(0, 0, 0, 0);
        buttonPanel.add(btnExit, bGbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        contentPanel.add(buttonPanel, gbc);

        add(contentPanel, BorderLayout.CENTER);
    }

    private void setupDialog() {
        pack();
        setResizable(false);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void loadRecentUsers() {
        try {
            com.openbravo.pos.forms.AppConfig config = com.openbravo.pos.forms.AppConfig.getInstance();
            config.load();
            String usersStr = config.getProperty("login.recent.users");
            if (usersStr != null && !usersStr.isEmpty()) {
                String[] users = usersStr.split(",");
                for (String user : users) {
                    if (user != null && !user.trim().isEmpty()) {
                        cmbUsername.addItem(user.trim());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar usuarios recientes", e);
        }
    }

    private void saveUserToHistory(String username) {
        if (username == null || username.trim().isEmpty())
            return;
        try {
            com.openbravo.pos.forms.AppConfig config = com.openbravo.pos.forms.AppConfig.getInstance();
            config.load();
            String currentUsers = config.getProperty("login.recent.users");
            java.util.List<String> userList = new java.util.ArrayList<>();
            if (currentUsers != null && !currentUsers.isEmpty()) {
                for (String u : currentUsers.split(",")) {
                    if (!u.trim().equalsIgnoreCase(username))
                        userList.add(u.trim());
                }
            }
            userList.add(0, username.trim());
            if (userList.size() > 5)
                userList = userList.subList(0, 5);
            config.setProperty("login.recent.users", String.join(",", userList));
            config.save();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al guardar historial", e);
        }
    }

    private void performLogin() {
        String username = cmbUsername.getEditor().getItem().toString().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty()) {
            new MessageInf(MessageInf.SGN_WARNING, "Por favor ingrese un usuario").show(this);
            cmbUsername.requestFocus();
            return;
        }

        try {
            // Reutilizar lógica de búsqueda de JAuthPanel
            AppUser user = m_dlSystem.findPeopleByName(username);

            // Búsqueda case-insensitive si falla la exacta
            if (user == null && m_session != null) {
                Object[] found = (Object[]) new StaticSentence(m_session,
                        "SELECT NAME FROM PEOPLE WHERE UPPER(NAME) = UPPER(?)",
                        new SerializerWriteBasic(new Datas[] { Datas.STRING }),
                        new SerializerReadBasic(new Datas[] { Datas.STRING }))
                        .find(username);
                if (found != null) {
                    user = m_dlSystem.findPeopleByName((String) found[0]);
                }
            }

            if (user == null) {
                new MessageInf(MessageInf.SGN_WARNING, "Usuario no encontrado.").show(this);
                return;
            }

            if (user.authenticate(password)) {
                loggedUser = user;
                saveUserToHistory(user.getName());
                dispose();
            } else {
                new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.BadPassword")).show(this);
                txtPassword.requestFocus();
                txtPassword.selectAll();
            }
        } catch (BasicException ex) {
            LOGGER.log(Level.SEVERE, "Error en login", ex);
            new MessageInf(MessageInf.SGN_DANGER, "Error al iniciar sesión: " + ex.getMessage()).show(this);
        }
    }

    private void showForgotPassword() {
        JDialogForgotPassword dialog = new JDialogForgotPassword(null, m_dlSystem);
        dialog.setVisible(true);
    }

    public AppUser getLoggedUser() {
        return loggedUser;
    }
}
