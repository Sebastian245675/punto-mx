/*
 * Sebastian POS - Pantalla de Login Modernizada
 */
package com.openbravo.pos.forms;

import com.openbravo.basic.BasicException;
import com.openbravo.beans.JFlowPanel;
import com.openbravo.beans.JPasswordDialog;
import com.openbravo.beans.JPasswordDialogModern;
import com.openbravo.data.gui.MessageInf;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * Pantalla de Login Modernizada por Sebastian
 * @author Sebastian
 */
public class JAuthPanel extends javax.swing.JPanel {

    private static final Logger LOGGER = Logger.getLogger(JAuthPanel.class.getName());
    private static final long serialVersionUID = 1L;
    
    // Colores modernos
    private static final Color MODERN_BLUE = new Color(64, 128, 255);
    private static final Color MODERN_LIGHT_BLUE = new Color(100, 150, 255);
    private static final Color MODERN_DARK = new Color(45, 45, 45);
    private static final Color MODERN_LIGHT = new Color(248, 249, 250);
    private static final Color MODERN_GRAY = new Color(108, 117, 125);

    private StringBuilder inputtext;
    
    private final DataLogicSystem m_dlSystem;
    private final AuthListener authListener;

    public JAuthPanel(DataLogicSystem dlSystem, AuthListener authcListener) {
        
        authListener = authcListener;
        m_dlSystem = dlSystem;
        
        initComponents();
        initPanel();
    }

    private void initPanel() {
        // 🎨 APLICAR DISEÑO ULTRA MODERNO AL PANEL PRINCIPAL
        setBackground(MODERN_LIGHT);
        
        // Título principal moderno
        jLabel1.setFont(new Font("Segoe UI", Font.BOLD, 18));
        jLabel1.setForeground(MODERN_DARK);
        jLabel1.setText("🔐 Seleccionar Usuario");
        jLabel1.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));
        
        // Panel lateral moderno
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(230, 230, 230)),
            BorderFactory.createEmptyBorder(0, 15, 0, 15)
        ));
        
        // Header panel moderno
        leftHeaderPanel.setBackground(MODERN_BLUE);
        jLabel1.setForeground(Color.WHITE);
        jLabel1.setOpaque(true);
        jLabel1.setBackground(MODERN_BLUE);
        
        // Estilo moderno para el scroll pane
        usersLisScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(12, 35));
        usersLisScrollPane.setBackground(Color.WHITE);
        usersLisScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        usersLisScrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = MODERN_BLUE;
                this.trackColor = new Color(240, 240, 240);
            }
        });
        
        // Panel principal con efectos visuales
        mainPanel.setBackground(MODERN_LIGHT);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Campo de texto oculto pero funcional
        m_txtKeys.setPreferredSize(new Dimension(0, 0));
        m_txtKeys.setVisible(false);
        
        showListPeople();

        inputtext = new StringBuilder();
        m_txtKeys.setText(null);
        
        // Focus con delay para mejor UX
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                m_txtKeys.requestFocus();
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 🎨 EFECTOS VISUALES MODERNOS PARA EL LOGIN
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Gradiente de fondo moderno
        GradientPaint backgroundGradient = new GradientPaint(
            0, 0, MODERN_LIGHT,
            getWidth(), getHeight(), new Color(240, 245, 251)
        );
        g2d.setPaint(backgroundGradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // Efecto de líneas decorativas sutiles
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
        g2d.setColor(MODERN_BLUE);
        g2d.setStroke(new BasicStroke(2));
        
        // Líneas diagonales decorativas
        for (int i = 0; i < getWidth() + getHeight(); i += 100) {
            g2d.drawLine(i, 0, i - getHeight(), getHeight());
        }
        
        g2d.dispose();
    }

    private void showListPeople() {
        try {
            usersLisScrollPane.getViewport().setView(null);

            JFlowPanel jPeople = new JFlowPanel();
            jPeople.applyComponentOrientation(getComponentOrientation());
            jPeople.setBackground(MODERN_LIGHT);

            java.util.List<AppUser> peoples = m_dlSystem.listPeopleVisible();
            
            LOGGER.log(Level.INFO, "✨ CONNECTING POS - Usuarios encontrados: " + peoples.size());

            for (AppUser user : peoples) {
                
                JButton btn = new JButton(new AppUserAction(user));
                
                // 🎨 DISEÑO ULTRA MODERNO PARA BOTONES DE USUARIO
                btn.applyComponentOrientation(getComponentOrientation());
                btn.setFocusPainted(false);
                btn.setFocusable(false);
                btn.setRequestFocusEnabled(false);
                
                // Tamaño moderno y elegante
                btn.setMaximumSize(new Dimension(160, 90));
                btn.setPreferredSize(new Dimension(160, 90));
                btn.setMinimumSize(new Dimension(160, 90));
                
                // Colores modernos con gradiente visual
                btn.setBackground(MODERN_BLUE);
                btn.setForeground(Color.WHITE);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(52, 116, 235), 2, true),
                    BorderFactory.createEmptyBorder(15, 20, 15, 20)
                ));
                
                // Fuente moderna y elegante
                btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
                
                // Alineación perfecta
                btn.setHorizontalAlignment(SwingConstants.CENTER);
                btn.setHorizontalTextPosition(AbstractButton.CENTER);
                btn.setVerticalTextPosition(AbstractButton.BOTTOM);
                
                // 🚀 EFECTOS HOVER PREMIUM
                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        btn.setBackground(new Color(52, 116, 235));
                        btn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(40, 104, 223), 3, true),
                            BorderFactory.createEmptyBorder(14, 19, 14, 19)
                        ));
                        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                        
                        // Efecto de "elevación"
                        btn.setPreferredSize(new Dimension(165, 95));
                        btn.revalidate();
                    }
                    
                    @Override
                    public void mouseExited(MouseEvent e) {
                        btn.setBackground(MODERN_BLUE);
                        btn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(52, 116, 235), 2, true),
                            BorderFactory.createEmptyBorder(15, 20, 15, 20)
                        ));
                        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                        
                        // Restaurar tamaño
                        btn.setPreferredSize(new Dimension(160, 90));
                        btn.revalidate();
                    }
                    
                    @Override
                    public void mousePressed(MouseEvent e) {
                        btn.setBackground(new Color(40, 104, 223));
                    }
                    
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        btn.setBackground(new Color(52, 116, 235));
                    }
                });
                
                // 👤 CREAR AVATAR MODERNO PARA EL USUARIO
                configurarAvatarModerno(btn, user);
                
                jPeople.add(btn);
            }

            usersLisScrollPane.getViewport().setView(jPeople);

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "❌ Error al cargar usuarios: ", ex);
        }
    }

    private void processKey(char c) {

        if ((c == '\n') || (c == '?')) {
            AppUser user = null;
            try {
                user = m_dlSystem.findPeopleByCard(inputtext.toString());
            } catch (BasicException ex) {
                LOGGER.log(Level.WARNING, "Exception on findPeopleByCard: ", ex);
            }

            if (user == null) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
                        AppLocal.getIntString("message.nocard"));
                msg.show(this);
            } else {
                authListener.onSucess(user);
            }

            inputtext = new StringBuilder();
        } else {
            inputtext.append(c);
        }
    }

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
                    String sPassword = JPasswordDialogModern.showEditor(JAuthPanel.this,
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
    
    /**
     * 👤 Configura un avatar moderno para el botón de usuario
     */
    private void configurarAvatarModerno(JButton btn, AppUser user) {
        String userName = user.getName();
        
        // Array de emojis modernos para avatares
        String[] avatars = {
            "👨‍💼", "👩‍💼", "👨‍🔧", "👩‍🔧", "👨‍💻", "👩‍💻", 
            "👨‍🍳", "👩‍🍳", "👨‍⚕️", "👩‍⚕️", "👨‍🏫", "👩‍🏫",
            "💼", "🧑‍💼", "👤", "🎯", "⭐", "🚀"
        };
        
        // Seleccionar avatar basado en el hash del nombre
        String selectedAvatar = avatars[Math.abs(userName.hashCode()) % avatars.length];
        
        // Crear texto con avatar y nombre
        String buttonText = "<html><div style='text-align: center; padding: 8px;'>" +
                           "<div style='font-size: 24px; margin-bottom: 5px;'>" + selectedAvatar + "</div>" +
                           "<div style='font-size: 12px; font-weight: bold; color: white;'>" + userName + "</div>" +
                           "</html>";
        
        btn.setText(buttonText);
        
        // Configuraciones adicionales de estilo
        btn.setVerticalTextPosition(SwingConstants.CENTER);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
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
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        m_vendorImageLabel = new javax.swing.JLabel();
        mainScrollPanel = new javax.swing.JScrollPane();
        jCopyRightPanel1 = new com.openbravo.pos.forms.JCopyRightPanel();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 10), new java.awt.Dimension(32767, 0));
        leftPanel = new javax.swing.JPanel();
        leftHeaderPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        usersLisScrollPane = new javax.swing.JScrollPane();
        leftFooterPanel = new javax.swing.JPanel();
        m_txtKeys = new javax.swing.JTextField();

        setLayout(new java.awt.BorderLayout());
        setBackground(MODERN_LIGHT);

        // 🎨 PANEL PRINCIPAL CON DISEÑO MODERNO
        mainPanel.setLayout(new java.awt.BorderLayout());
        mainPanel.setBackground(MODERN_LIGHT);

        // 🖼️ LOGO MODERNIZADO
        m_vendorImageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        m_vendorImageLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/app_logo_100x100.png"))); // NOI18N
        m_vendorImageLabel.setText("🚀 Sebastian POS"); 
        m_vendorImageLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        m_vendorImageLabel.setForeground(MODERN_DARK);
        m_vendorImageLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        m_vendorImageLabel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        m_vendorImageLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        m_vendorImageLabel.setName("m_vendorImageLabel");
        mainPanel.add(m_vendorImageLabel, java.awt.BorderLayout.NORTH);

        // 🎯 SCROLL PANEL MODERNIZADO
        mainScrollPanel.setViewportView(jCopyRightPanel1);
        mainScrollPanel.setBorder(BorderFactory.createEmptyBorder());
        mainScrollPanel.setBackground(MODERN_LIGHT);

        mainPanel.add(mainScrollPanel, java.awt.BorderLayout.CENTER);
        mainPanel.add(filler2, java.awt.BorderLayout.SOUTH);

        add(mainPanel, java.awt.BorderLayout.CENTER);

        // 👥 PANEL LATERAL DE USUARIOS MODERNIZADO
        leftPanel.setPreferredSize(new java.awt.Dimension(350, 450));
        leftPanel.setLayout(new java.awt.BorderLayout());
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, MODERN_BLUE),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // 🏷️ HEADER MODERNIZADO
        leftHeaderPanel.setMinimumSize(new java.awt.Dimension(350, 60));
        leftHeaderPanel.setPreferredSize(new java.awt.Dimension(350, 60));
        leftHeaderPanel.setLayout(new java.awt.BorderLayout());
        leftHeaderPanel.setBackground(Color.WHITE);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("👋 Selecciona tu Usuario");
        jLabel1.setFont(new Font("Segoe UI", Font.BOLD, 16));
        jLabel1.setForeground(MODERN_DARK);
        jLabel1.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));
        jLabel1.setName("m_LoginLabel");
        leftHeaderPanel.add(jLabel1, java.awt.BorderLayout.CENTER);

        leftPanel.add(leftHeaderPanel, java.awt.BorderLayout.NORTH);

        // 📜 LISTA DE USUARIOS MODERNIZADA
        usersLisScrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MODERN_GRAY, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        usersLisScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        usersLisScrollPane.setFont(new java.awt.Font("Segoe UI", 0, 14));
        usersLisScrollPane.setMinimumSize(new java.awt.Dimension(21, 60));
        usersLisScrollPane.setPreferredSize(new java.awt.Dimension(350, 60));
        usersLisScrollPane.setBackground(Color.WHITE);
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
                .addComponent(m_txtKeys, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(350, Short.MAX_VALUE))
        );
        leftFooterPanelLayout.setVerticalGroup(
            leftFooterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(leftFooterPanelLayout.createSequentialGroup()
                .addComponent(m_txtKeys, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(60, 60, 60))
        );

        leftPanel.add(leftFooterPanel, java.awt.BorderLayout.SOUTH);

        add(leftPanel, java.awt.BorderLayout.EAST);
    }// </editor-fold>//GEN-END:initComponents

    private void m_txtKeysKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_m_txtKeysKeyTyped

        m_txtKeys.setText("0");
        processKey(evt.getKeyChar());
    }//GEN-LAST:event_m_txtKeysKeyTyped


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
