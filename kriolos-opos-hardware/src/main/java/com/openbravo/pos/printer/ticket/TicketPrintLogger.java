/*
 * Copyright (C) 2026 PuntoMX
 *
 * Archivo de logging dedicado para diagnosticar problemas de impresión de tickets.
 * Los logs se escriben tanto a la consola como a un archivo: ticket-print-debug.log
 */
package com.openbravo.pos.printer.ticket;

import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger dedicado para diagnosticar el proceso de impresión de tickets.
 * Registra cada paso: configuración de fuentes, tamaños de línea, escala,
 * normalTotals, etc.
 * 
 * Los logs se escriben a: ticket-print-debug.log (en el directorio de trabajo)
 * 
 * @author PuntoMX Debug
 */
public class TicketPrintLogger {

    private static final String LOG_FILE_LOCAL = "ticket-print-debug.log";
    private static final String LOG_FILE_DOWNLOADS = System.getProperty("user.home") + "\\Downloads\\ticket-print-debug.log";
    private static final String LOG_FILE_DESKTOP = System.getProperty("user.home") + "\\Desktop\\ticket-print-debug.log";
    private static final String LOG_FILE_OUTER = "c:\\Users\\USUARIO\\Downloads\\punto-mx3\\ticket-print-debug.log";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static int ticketCounter = 0;

    static {
        log("INFO", "=========================================================");
        log("INFO", "TicketPrintLogger INICIALIZADO CORRECTAMENTE");
        log("INFO", "=========================================================");
    }

    /**
     * Escribe un mensaje a los archivos de log (local, Descargas, Escritorio) y a System.out.
     */
    private static synchronized void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(FMT);
        String logLine = String.format("[%s] [%s] [TICKET-PRINT] %s", timestamp, level, message);

        // Escribir a consola
        System.out.println(logLine);

        // Escribir a archivo local
        writeLogToPath(LOG_FILE_LOCAL, logLine);

        // Escribir a carpeta de Descargas
        writeLogToPath(LOG_FILE_DOWNLOADS, logLine);

        // Escribir a archivo fuera de la carpeta
        writeLogToPath(LOG_FILE_OUTER, logLine);

        // Escribir a archivo en el Escritorio
        writeLogToPath(LOG_FILE_DESKTOP, logLine);
    }

    private static void writeLogToPath(String path, String line) {
        try {
            java.io.File file = new java.io.File(path);
            java.io.File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (PrintWriter pw = new PrintWriter(new FileWriter(file, true))) {
                pw.println(line);
                pw.flush();
            }
        } catch (Exception e) {
            System.err.println("[LOG-WRITE-ERROR] " + path + ": " + e.getMessage());
        }
    }

    public static void info(String message) {
        log("INFO", message);
    }

    public static void warn(String message) {
        log("WARN", message);
    }

    public static void error(String message) {
        log("ERROR", message);
    }

    // ========================================================================
    // Métodos específicos para cada punto del flujo de impresión
    // ========================================================================

    /**
     * Log cuando se crea un nuevo BasicTicketForPrinter (al iniciar un ticket).
     */
    public static void logTicketCreated(String fontName, int fontSize, boolean fontBold, boolean normalTotals) {
        ticketCounter++;
        info("=========================================================");
        info("NUEVO TICKET #" + ticketCounter + " CREADO");
        info("=========================================================");
        info("  Parámetros de construcción:");
        info("    fontName     = " + fontName);
        info("    fontSize     = " + fontSize);
        info("    fontBold     = " + fontBold);
        info("    normalTotals = " + normalTotals);
        if (normalTotals) {
            warn("  *** normalTotals=TRUE: Las líneas con size>0 (TOTAL, PAGO CON, etc.) ");
            warn("      serán FORZADAS a size=0 (tamaño normal). ¡Los totales NO se imprimirán grandes!");
            warn("      Para que los totales se impriman en tamaño grande, normalTotals debe ser FALSE.");
        } else {
            info("  normalTotals=FALSE: Las líneas con size>0 se imprimirán con escala ampliada. ¡CORRECTO!");
        }
    }

    /**
     * Log cuando se calcula la fuente base.
     */
    public static void logBaseFontCalculated(String fontName, int fontSize, int effectiveSize, Font baseFont) {
        info("  Cálculo de fuente base:");
        info("    fontSize original  = " + fontSize);
        info("    effectiveSize      = " + effectiveSize + " (Math.max(fontSize, 11))");
        info("    Fuente creada      = " + baseFont.getFontName());
        info("    Tamaño de fuente   = " + baseFont.getSize());
        info("    Estilo de fuente   = " + (baseFont.isBold() ? "BOLD" : "PLAIN"));
        AffineTransform t = baseFont.getTransform();
        if (t != null) {
            info("    Transform          = scaleX=" + t.getScaleX() + ", scaleY=" + t.getScaleY()
                    + ", shearX=" + t.getShearX() + ", shearY=" + t.getShearY()
                    + ", translateX=" + t.getTranslateX() + ", translateY=" + t.getTranslateY());
        }
    }

    /**
     * Log cuando se llama a beginLine() - el punto CLAVE donde normalTotals puede forzar size=0.
     */
    public static void logBeginLine(int originalSize, int effectiveSize, boolean normalTotals) {
        info("  --- beginLine() ---");
        info("    size solicitado (del XML)  = " + originalSize + " (" + describeFontSize(originalSize) + ")");
        info("    normalTotals               = " + normalTotals);

        if (normalTotals && originalSize > 0) {
            warn("    >>> PROBLEMA DETECTADO: normalTotals=TRUE y size=" + originalSize);
            warn("    >>> El size se FUERZA a 0 (NORMAL). ¡La línea NO se imprimirá grande!");
            warn("    >>> Solución: Cambiar normalTotals a FALSE en la configuración de la impresora");
        }

        info("    size efectivo (usado)      = " + effectiveSize + " (" + describeFontSize(effectiveSize) + ")");
    }

    /**
     * Log cuando se llama a printText() - cada texto que se agrega a la línea.
     */
    public static void logPrintText(int style, String text) {
        info("    printText: style=" + describeStyle(style) + ", text=\"" + text + "\"");
    }

    /**
     * Log cuando se llama a endLine().
     */
    public static void logEndLine() {
        info("  --- endLine() ---");
    }

    /**
     * Log cuando PrinterFontState calcula la fuente final para una línea.
     */
    public static void logFontStateCalculation(int fontSizeInt, double widthScale, double heightScale,
            Font baseFont, Font derivedFont, int style) {
        info("    PrinterFontState - Cálculo de fuente para línea:");
        info("      fontSize (int)   = " + fontSizeInt + " (" + describeFontSize(fontSizeInt) + ")");
        info("      widthScale       = " + widthScale);
        info("      heightScale      = " + heightScale);
        info("      baseFont size    = " + baseFont.getSize());
        info("      derivedFont size = " + derivedFont.getSize());
        info("      style aplicado   = " + describeStyle(style));
        AffineTransform t = derivedFont.getTransform();
        if (t != null) {
            info("      Transform final  = scaleX=" + String.format("%.3f", t.getScaleX())
                    + ", scaleY=" + String.format("%.3f", t.getScaleY()));
        }
    }

    /**
     * Log cuando PrintableBasicTicket se usa para enviar a la impresora.
     */
    public static void logPrintableTicket(int imageable_x, int imageable_y, int imageable_width,
            int imageable_height, int columns, int fontSize, double scaleX, int logicalWidth) {
        info("  PrintableBasicTicket - Configuración de impresión:");
        info("    imageable_x      = " + imageable_x);
        info("    imageable_y      = " + imageable_y);
        info("    imageable_width  = " + imageable_width);
        info("    imageable_height = " + imageable_height);
        info("    columns          = " + columns);
        info("    fontSize         = " + fontSize);
        info("    scaleX calculada = " + String.format("%.4f", scaleX));
        info("    logicalWidth     = " + logicalWidth);
    }

    /**
     * Log resumen al finalizar el ticket (endReceipt).
     */
    public static void logEndReceipt(String printerName) {
        info("=========================================================");
        info("TICKET #" + ticketCounter + " ENVIADO A IMPRESORA: " + printerName);
        info("=========================================================");
    }

    // ========================================================================
    // Utilidades de descripción
    // ========================================================================

    private static String describeFontSize(int size) {
        switch (size) {
            case 0: return "NORMAL (1x1)";
            case 1: return "DOUBLE_HEIGHT (1x2) - Letras altas";
            case 2: return "DOUBLE_WIDTH (2x1) - Letras anchas";
            case 3: return "DOUBLE_WIDTH_HEIGHT (2x2) - Letras grandes";
            default: return "DESCONOCIDO(" + size + ")";
        }
    }

    private static String describeStyle(int style) {
        StringBuilder sb = new StringBuilder();
        if (style == 0) {
            sb.append("PLAIN");
        } else {
            if ((style & 1) != 0) sb.append("BOLD ");
            if ((style & 2) != 0) sb.append("UNDERLINE ");
        }
        return sb.toString().trim() + " (" + style + ")";
    }
}
