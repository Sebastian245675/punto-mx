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

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.JMessageDialog;
import com.openbravo.data.gui.MessageInf;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.IOException;
import java.rmi.RemoteException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

import com.openbravo.pos.config.JPanelConfiguration;
import com.openbravo.pos.instance.AppMessage;
import com.openbravo.pos.util.OSValidator;

/**
 * @author adrianromero
 */
public class JRootFrame extends javax.swing.JFrame implements AppMessage {

    private static final Logger LOGGER = Logger.getLogger(JRootFrame.class.getName());
    private static final long serialVersionUID = 1L;

    private final JSplashScreen splashScreen = new JSplashScreen();
    private final JRootApp m_rootapp;
    private final AppProperties m_props;

    public JRootFrame(AppProperties props) {
        initComponents();
        m_props = props;
        m_rootapp = new JRootApp(m_props);
    }

    public void initFrame() {

        setTitle(AppLocal.APP_NAME);

        // FORZAR ICONO PERSONALIZADO CON MÁXIMA AGRESIVIDAD
        try {
            // Intentar cargar desde múltiples ubicaciones
            String[] iconPaths = {
                    "/com/openbravo/images/connecting_pos_icon.png",
                    "/com/openbravo/images/app_logo_48x48.png",
                    "/com/openbravo/pos/templates/app_logo_48x48.png",
                    "/images/app_logo_48x48.png",
                    "/app_logo_48x48.png"
            };

            java.awt.Image iconImage = null;
            for (String path : iconPaths) {
                try {
                    iconImage = ImageIO.read(JRootFrame.class.getResourceAsStream(path));
                    if (iconImage != null)
                        break;
                } catch (Exception e) {
                    // Intentar siguiente ruta
                }
            }

            if (iconImage != null) {
                // Configurar múltiples tamaños de iconos
                java.util.List<java.awt.Image> iconList = new java.util.ArrayList<>();
                iconList.add(iconImage.getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH));
                iconList.add(iconImage.getScaledInstance(24, 24, java.awt.Image.SCALE_SMOOTH));
                iconList.add(iconImage.getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH));
                iconList.add(iconImage.getScaledInstance(48, 48, java.awt.Image.SCALE_SMOOTH));
                iconList.add(iconImage.getScaledInstance(64, 64, java.awt.Image.SCALE_SMOOTH));
                iconList.add(iconImage.getScaledInstance(128, 128, java.awt.Image.SCALE_SMOOTH));

                this.setIconImages(iconList);
                this.setIconImage(iconImage);

                // También configurar para la barra de tareas agresivamente
                try {
                    if (java.awt.Taskbar.isTaskbarSupported()) {
                        java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
                        if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                            taskbar.setIconImage(iconImage);
                        }
                    }
                } catch (Exception e) {
                    // Ignorar si la plataforma no soporta taskbar
                }

                // Forzar refresco del frame
                SwingUtilities.invokeLater(() -> {
                    this.repaint();
                    this.revalidate();
                });
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception load icon", e);
        }

        // Determinar modo de pantalla y aplicar ANTES de que el frame sea displayable
        // (antes de diálogos)
        // para evitar IllegalComponentStateException
        String screenmode = m_props.getProperty("machine.screenmode");
        if ("fullscreen".equals(screenmode)) {
            try {
                if (!this.isDisplayable()) {
                    this.setUndecorated(true);
                    this.setResizable(false);
                }
            } catch (java.awt.IllegalComponentStateException | java.lang.SecurityException e) {
                java.util.logging.Logger.getLogger(JRootFrame.class.getName()).log(java.util.logging.Level.WARNING,
                        "Error al establecer modo sin bordes: " + e.getMessage());
            }
        }

        // JRootFrame permanece OCULTO (setVisible(false) por defecto)
        // No añadimos nada al contentPane todavía para evitar "fondos blancos"

        // LOAD APP PANEL
        try {
            m_rootapp.initApp();

            // Sebastian - Mostrar diálogo de login estilo eleventa
            // Pasamos 'null' como padre para que el diálogo sea independiente de la ventana
            // principal
            // y no provoque que esta se muestre o parpadee de fondo.
            JLogonDialog logonDialog = new JLogonDialog(null, m_rootapp.getDataLogicSystem(), m_rootapp.getSession());
            logonDialog.setVisible(true);

            AppUser loggedUser = logonDialog.getLoggedUser();
            if (loggedUser == null) {
                // Usuario canceló el login (cerró la ventana o presionó Salir)
                System.exit(0);
                return;
            }

            // Login exitoso, poner m_rootapp en el contentPane
            this.getContentPane().add(m_rootapp, java.awt.BorderLayout.CENTER);

            // AHORA configurar y mostrar la ventana principal según el modo elegido
            if (null == screenmode) {
                this.modeWindow();
            } else {
                switch (screenmode) {
                    case "fullscreen":
                        this.modeKiosk();
                        break;
                    case "windowmaximised":
                        this.modeWindowMaximized();
                        break;
                    default:
                        this.modeWindow();
                        break;
                }
            }

            // Revalidar el diseño (la ventana ya es visible)
            this.validate();
            this.repaint();

            // Abrir la vista de la aplicación (esto pedirá monto inicial si es necesario)
            m_rootapp.openAppView(loggedUser);

            sendInitEnvent();
            return; // Éxito

        } catch (BasicException ex) {
            // LOAD CONFIG PANEL

            int opionRes = JMessageDialog.showConfirmDialog(this,
                    new MessageInf(MessageInf.SGN_DANGER,
                            "<html>Application fail to start<br>Do you want to open the configuration panel?", ex));

            /*
             * opionRes = JOptionPane.showConfirmDialog(this,
             * "<html>Application fail to start<br>Do you want to open the configuration panel?"
             * ,
             * "Application Error", JOptionPane.YES_NO_OPTION,
             * JOptionPane.INFORMATION_MESSAGE);
             * 
             */
            if (opionRes == JOptionPane.YES_OPTION) {
                // JFrmConfig jFrmConfig = new JFrmConfig(m_props);
                // jFrmConfig.setVisible(true);

                JPanelConfiguration config = new JPanelConfiguration(m_props);
                config.setCloseListener((JPanelConfiguration.CloseEvent e) -> {
                    // This will be call when user press save or close button on config panel
                    dispose();
                    System.exit(0);
                });

                getContentPane().remove(splashScreen);
                getContentPane().add(config, BorderLayout.CENTER);
            } else {
                dispose();
                System.exit(0);
            }
        }

    }

    private void modeWindowMaximized() {
        this.setMinimumSize(new java.awt.Dimension(1024, 700)); // Tamaño mínimo para evitar vistas cortadas
        if (this.getExtendedState() != MAXIMIZED_BOTH) {
            this.setExtendedState(MAXIMIZED_BOTH);
        }
        this.setLocationRelativeTo(null); // center
        if (!this.isVisible()) {
            this.setVisible(true);
        }
    }

    private void modeWindow() {
        this.setMinimumSize(new java.awt.Dimension(1024, 700)); // Tamaño mínimo para evitar vistas cortadas
        if (!this.isDisplayable()) {
            this.pack();
        }
        // Si pack() resultó en algo más pequeño que el mínimo, ajustar
        java.awt.Dimension size = this.getSize();
        if (size.width < 1280 || size.height < 750) {
            this.setSize(Math.max(size.width, 1280), Math.max(size.height, 750));
        }
        this.setLocationRelativeTo(null); // center
        if (!this.isVisible()) {
            this.setVisible(true);
        }
    }

    private void modeKiosk() {
        if (!this.isDisplayable()) {
            try {
                this.setUndecorated(true);
                this.setResizable(false);
            } catch (java.awt.IllegalComponentStateException e) {
                java.util.logging.Logger.getLogger(JRootFrame.class.getName())
                        .warning("Could not set kiosk mode: frame is already displayable.");
            }
        }

        // LINUX/UNIX
        if (new OSValidator().isUnix()) {
            GraphicsDevice device = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice();

            if (device.isFullScreenSupported()) {
                setResizable(true);

                addFocusListener(new FocusListener() {
                    @Override
                    public void focusGained(FocusEvent arg0) {
                        setAlwaysOnTop(true);
                    }

                    @Override
                    public void focusLost(FocusEvent arg0) {
                        setAlwaysOnTop(false);
                    }
                });
                device.setFullScreenWindow(this);
            } else {
                setVisible(true);
            }
        } else {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setBounds(0, 0, d.width, d.height);
            setVisible(true);
        }
    }

    private void sendInitEnvent() {
        /**
         * String scriptId = "application.started"; try { ScriptEngine
         * scriptEngine =
         * ScriptFactory.getScriptEngine(ScriptFactory.BEANSHELL);
         *
         * String script = ; scriptEngine.put("device", m_props.getHost());
         * scriptEngine.eval(script); } catch (BeanFactoryException |
         * ScriptException e) { LOGGER.log(Level.WARNING, "Exception on
         * executing scriptId: " + scriptId, e); }
         */
    }

    /**
     * @throws RemoteException
     */
    @Override
    public void restoreWindow() throws RemoteException {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (getExtendedState() == JFrame.ICONIFIED) {
                    setExtendedState(JFrame.NORMAL);
                }
                requestFocus();
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }

            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_formWindowClosing

        m_rootapp.tryToClose();

    }// GEN-LAST:event_formWindowClosing

    private void formWindowClosed(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_formWindowClosed

        System.exit(0);

    }// GEN-LAST:event_formWindowClosed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
