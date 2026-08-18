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
//    along with this program.  If not, see <http://www.gnu.org/licenses/>
package com.openbravo.pos.forms;

import com.openbravo.pos.instance.InstanceManager;
import com.openbravo.pos.util.ModernLookAndFeel;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class StartPOS {

    private static final Logger LOGGER = Logger.getLogger(StartPOS.class.getName());
    private static final String APP_WINDOW_TITLE = "Tortillería La Conchita";

    public static void main(final String args[]) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                writeStartupDebug("Uncaught exception en hilo " + thread.getName(), throwable));

        writeStartupDebug("=== StartPOS.main iniciado ===");
        writeStartupDebug("args.length=" + args.length);
        for (int i = 0; i < args.length; i++) {
            writeStartupDebug("arg[" + i + "]=" + args[i]);
        }
        writeStartupDebug("java.version=" + System.getProperty("java.version"));
        writeStartupDebug("java.home=" + System.getProperty("java.home"));
        writeStartupDebug("java.vendor=" + System.getProperty("java.vendor"));
        writeStartupDebug("java.class.path=" + System.getProperty("java.class.path"));
        writeStartupDebug("user.home=" + System.getProperty("user.home"));
        writeStartupDebug("user.dir=" + System.getProperty("user.dir"));
        writeStartupDebug("os.name=" + System.getProperty("os.name"));
        writeStartupDebug("os.version=" + System.getProperty("os.version"));
        writeStartupDebug("os.arch=" + System.getProperty("os.arch"));

        if (System.getProperty("sun.java2d.uiScale") == null) {
            System.setProperty("sun.java2d.uiScale", "1.0");
        }
        writeStartupDebug("sun.java2d.uiScale=" + System.getProperty("sun.java2d.uiScale"));

        ModernLookAndFeel.aplicarEstiloModerno();
        writeStartupDebug("Estilo moderno aplicado");

        installEscapeToCloseDialogs();
        writeStartupDebug("Cierre global de diálogos con ESC instalado");

        com.openbravo.pos.printer.ticket.TicketPrintLogger.info(">>> APLICACION StartPOS INICIADA EXITOSAMENTE <<<");
        writeStartupDebug("TicketPrintLogger inicializado");

        File configFile = (args.length > 0 ? new File(args[0]) : null);
        AppConfig config = new AppConfig(configFile);
        config.load();
        writeStartupDebug("Config cargada desde: " + config.getConfigFile().getAbsolutePath());
        writeStartupDebug("Config existe=" + config.getConfigFile().exists());
        AppConfig.applySystemProperties(config);
        writeStartupDebug("System properties aplicadas");

        String fontSizeStr = config.getProperty("font.size");
        String fontName = config.getProperty("font.name");
        if (fontName == null || fontName.isBlank()) {
            fontName = "Segoe UI";
            config.setProperty("font.name", fontName);
        }
        int fontSize = 32;
        if (fontSizeStr == null || fontSizeStr.isBlank()) {
            config.setProperty("font.size", String.valueOf(fontSize));
            try {
                config.save();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al guardar el tamaño de fuente por defecto", e);
                writeStartupDebug("Error guardando font.size por defecto: " + e.getMessage(), e);
            }
        } else {
            try {
                fontSize = Integer.parseInt(fontSizeStr.trim());
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Error al parsear el tamaño de fuente: " + fontSizeStr, e);
                writeStartupDebug("Error parseando font.size=" + fontSizeStr, e);
            }
        }

        ModernLookAndFeel.aplicarFuenteGlobal(new java.awt.Font(fontName, java.awt.Font.PLAIN, fontSize));
        writeStartupDebug("Fuente global aplicada: " + fontName + " " + fontSize);

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                writeStartupDebug("Entrando a SwingUtilities.invokeLater");

                final JRootFrame rootframe;
                try {
                    rootframe = new JRootFrame(config);
                    writeStartupDebug("JRootFrame creado");
                } catch (Throwable t) {
                    writeStartupDebug("Error creando JRootFrame", t);
                    throw t;
                }
                if (1 != 1 && "true".equals(config.getProperty("machine.uniqueinstance"))) {

                    try {
                        InstanceManager.queryInstance().restoreWindow();
                    } catch (RemoteException | NotBoundException e) {
                        String msg = "Cannot start the application. Another instance is alreday running";
                        LOGGER.log(Level.WARNING, msg, e);
                        writeStartupDebug(msg, e);
                        JOptionPane.showMessageDialog(null,
                                msg,
                                APP_WINDOW_TITLE, JOptionPane.WARNING_MESSAGE);
                        System.exit(-1000);
                    }

                    try {
                        final InstanceManager instmanager = new InstanceManager(rootframe);
                        instmanager.registerInstance();

                    } catch (RemoteException | AlreadyBoundException e) {
                        String msg = "Cannot start the application. Cannot register a new instance";
                        LOGGER.log(Level.WARNING, msg, e);
                        writeStartupDebug(msg, e);
                        JOptionPane.showMessageDialog(null,
                                msg,
                                APP_WINDOW_TITLE, JOptionPane.WARNING_MESSAGE);
                        System.exit(-1001);
                    }
                }

                try {
                    writeStartupDebug("Llamando rootframe.initFrame()");
                    rootframe.initFrame();
                    writeStartupDebug("rootframe.initFrame() completado");
                } catch (Throwable t) {
                    writeStartupDebug("Error en rootframe.initFrame(): " + t.getMessage(), t);
                    throw t;
                }
            }
        });
    }

    private static void installEscapeToCloseDialogs() {
        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(event -> {
            if (event.isConsumed()
                    || event.getID() != java.awt.event.KeyEvent.KEY_PRESSED
                    || event.getKeyCode() != java.awt.event.KeyEvent.VK_ESCAPE) {
                return false;
            }

            java.awt.Window activeWindow = java.awt.KeyboardFocusManager
                    .getCurrentKeyboardFocusManager()
                    .getActiveWindow();
            if (!(activeWindow instanceof java.awt.Dialog) || !activeWindow.isShowing()) {
                return false;
            }

            activeWindow.dispatchEvent(new java.awt.event.WindowEvent(
                    activeWindow, java.awt.event.WindowEvent.WINDOW_CLOSING));
            event.consume();
            return true;
        });
    }

    private static void writeStartupDebug(String message) {
        writeStartupDebug(message, null);
    }

    private static void writeStartupDebug(String message, Throwable throwable) {
        try {
            String customPath = System.getProperty("ticket.debug.file");
            Path logPath = (customPath == null || customPath.isBlank())
                    ? Path.of(System.getProperty("user.home"), "Downloads", "ticket prinf.debug")
                    : Path.of(customPath);
            Path parent = logPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            StringBuilder line = new StringBuilder()
                    .append(LocalDateTime.now())
                    .append(" [StartPOS] ")
                    .append(message)
                    .append(System.lineSeparator());
            if (throwable != null) {
                line.append(throwable.toString()).append(System.lineSeparator());
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                pw.flush();
                line.append(sw).append(System.lineSeparator());
            }
            Files.writeString(logPath, line.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
