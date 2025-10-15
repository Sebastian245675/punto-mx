package com.openbravo.pos.util;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * Sistema de logging centralizado que captura TODOS los logs a archivo
 * para facilitar depuración en equipos de usuarios finales
 */
public class ApplicationLogger {
    
    private static final String LOG_FILE_NAME = "SebastianPOS-log.txt";
    private static Path logFilePath;
    private static FileHandler fileHandler;
    private static boolean initialized = false;
    
    /**
     * Inicializa el sistema de logging
     * Crea archivo de log en el mismo directorio del ejecutable
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            // El log siempre se guarda en el directorio actual (donde está el .exe)
            logFilePath = Paths.get(System.getProperty("user.dir"), LOG_FILE_NAME);
            
            // Crear FileHandler con formato personalizado
            fileHandler = new FileHandler(logFilePath.toString(), true); // append=true
            fileHandler.setFormatter(new SimpleFormatter() {
                private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                
                @Override
                public synchronized String format(LogRecord record) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(dateFormat.format(new Date(record.getMillis())));
                    sb.append(" [").append(record.getLevel()).append("] ");
                    sb.append(record.getLoggerName()).append(" - ");
                    sb.append(formatMessage(record));
                    sb.append("\n");
                    
                    if (record.getThrown() != null) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        record.getThrown().printStackTrace(pw);
                        sb.append(sw.toString());
                    }
                    
                    return sb.toString();
                }
            });
            
            // Agregar handler al logger raíz para capturar TODOS los logs
            Logger rootLogger = Logger.getLogger("");
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Level.ALL);
            
            // También redirigir System.out y System.err al archivo
            PrintStream logStream = new PrintStream(new FileOutputStream(logFilePath.toFile(), true), true);
            PrintStream dualOut = new DualPrintStream(System.out, logStream);
            PrintStream dualErr = new DualPrintStream(System.err, logStream);
            System.setOut(dualOut);
            System.setErr(dualErr);
            
            initialized = true;
            
            // Log de inicio
            log("========================================");
            log("SebastianPOS - Sesión iniciada");
            log("Fecha: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            log("Java Version: " + System.getProperty("java.version"));
            log("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            log("User: " + System.getProperty("user.name"));
            log("Working Directory: " + System.getProperty("user.dir"));
            log("Log File: " + logFilePath.toAbsolutePath());
            log("========================================\n");
            
        } catch (Exception e) {
            System.err.println("ERROR: No se pudo inicializar el sistema de logging");
            e.printStackTrace();
        }
    }
    
    /**
     * Escribe un mensaje directo al log
     */
    public static void log(String message) {
        if (!initialized) {
            initialize();
        }
        
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String logLine = timestamp + " [INFO] " + message + "\n";
            Files.write(logFilePath, logLine.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("ERROR escribiendo al log: " + e.getMessage());
        }
    }
    
    /**
     * Registra una excepción completa
     */
    public static void logException(String context, Throwable t) {
        if (!initialized) {
            initialize();
        }
        
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            sb.append(" [ERROR] ").append(context).append("\n");
            sb.append("Exception: ").append(t.getClass().getName()).append("\n");
            sb.append("Message: ").append(t.getMessage()).append("\n");
            sb.append("Stack Trace:\n");
            
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            sb.append(sw.toString());
            sb.append("\n");
            
            Files.write(logFilePath, sb.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("ERROR escribiendo excepción al log: " + e.getMessage());
        }
    }
    
    /**
     * Cierra el sistema de logging
     */
    public static void shutdown() {
        if (fileHandler != null) {
            log("\n========================================");
            log("SebastianPOS - Sesión finalizada");
            log("Fecha: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            log("========================================");
            
            fileHandler.close();
        }
    }
    
    /**
     * PrintStream dual que escribe a dos destinos simultáneamente
     */
    private static class DualPrintStream extends PrintStream {
        private final PrintStream second;
        
        public DualPrintStream(PrintStream main, PrintStream second) {
            super(main);
            this.second = second;
        }
        
        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            second.write(buf, off, len);
        }
        
        @Override
        public void flush() {
            super.flush();
            second.flush();
        }
    }
}
