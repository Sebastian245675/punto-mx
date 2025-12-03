package com.openbravo.pos.forms;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para verificar si hay actualizaciones disponibles
 * @author Sebastian
 */
public class UpdateChecker {
    
    private static final Logger LOGGER = Logger.getLogger(UpdateChecker.class.getName());
    
    // URL donde se publica la versión más reciente (puedes cambiarla por tu servidor)
    private static final String VERSION_CHECK_URL = "https://raw.githubusercontent.com/Sebastian245675/punto-mx/main/VERSION.txt";
    
    /**
     * Verifica si hay una nueva versión disponible
     * @return UpdateInfo con información de la actualización, o null si no hay actualización
     */
    public static UpdateInfo checkForUpdates() {
        try {
            String latestVersion = fetchLatestVersion();
            if (latestVersion != null && !latestVersion.trim().isEmpty()) {
                String currentVersion = AppLocal.APP_VERSION;
                
                // Comparar versiones (formato: 1.0.0-Sebastian)
                if (isNewerVersion(latestVersion.trim(), currentVersion)) {
                    LOGGER.info("Nueva versión disponible: " + latestVersion + " (actual: " + currentVersion + ")");
                    return new UpdateInfo(latestVersion, true, "Nueva versión disponible");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error verificando actualizaciones: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Obtiene la versión más reciente desde el servidor
     */
    private static String fetchLatestVersion() {
        // Intentar desde URL principal
        try {
            URL url = new java.net.URI(VERSION_CHECK_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000); // 5 segundos timeout
            conn.setReadTimeout(5000);
            
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    String version = reader.readLine();
                    if (version != null) {
                        return version.trim();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "No se pudo obtener versión desde URL principal, intentando respaldo...");
        }
        
        // Intentar desde archivo local como respaldo
        try {
            java.io.File versionFile = new java.io.File("version.txt");
            if (versionFile.exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new java.io.FileReader(versionFile))) {
                    return reader.readLine();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo leer archivo de versión local");
        }
        
        return null;
    }
    
    /**
     * Compara dos versiones para determinar si la primera es más nueva
     * Formato esperado: "1.0.0-Sebastian" o "1.0.1"
     */
    private static boolean isNewerVersion(String latest, String current) {
        try {
            // Extraer números de versión (antes del guión si existe)
            String latestNum = latest.split("-")[0];
            String currentNum = current.split("-")[0];
            
            String[] latestParts = latestNum.split("\\.");
            String[] currentParts = currentNum.split("\\.");
            
            int maxLength = Math.max(latestParts.length, currentParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                
                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            
            // Si son iguales, verificar si hay sufijo diferente
            return !latest.equals(current);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error comparando versiones: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Clase para almacenar información de actualización
     */
    public static class UpdateInfo {
        private final String version;
        private final boolean available;
        private final String message;
        
        public UpdateInfo(String version, boolean available, String message) {
            this.version = version;
            this.available = available;
            this.message = message;
        }
        
        public String getVersion() {
            return version;
        }
        
        public boolean isAvailable() {
            return available;
        }
        
        public String getMessage() {
            return message;
        }
    }
}

