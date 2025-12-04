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
    
    // URL donde se publica la versión más reciente
    // Puede ser desde GitHub raw o desde tu propio servidor
    private static final String VERSION_CHECK_URL = "https://raw.githubusercontent.com/Sebastian245675/punto-mx/main/VERSION.txt";
    
    // URL alternativa para verificar versiones (puedes agregar más)
    private static final String[] VERSION_CHECK_URLS = {
        VERSION_CHECK_URL,
        "https://raw.githubusercontent.com/Sebastian245675/punto-mx/main/kriolos-opos-app/target/classes/META-INF/build.properties"
    };
    
    /**
     * Verifica si hay una nueva versión disponible
     * @return UpdateInfo con información de la actualización, o null si no hay actualización
     */
    public static UpdateInfo checkForUpdates() {
        try {
            String latestVersion = fetchLatestVersion();
            if (latestVersion != null && !latestVersion.trim().isEmpty()) {
                // Obtener la versión actual de la aplicación
                String currentVersion = AppLocal.APP_VERSION;
                
                LOGGER.info("Comparando versiones - Actual: " + currentVersion + ", Disponible: " + latestVersion);
                
                // Comparar versiones (formato: 1.0.0-Sebastian o 1.0.0-SNAPSHOT)
                if (isNewerVersion(latestVersion.trim(), currentVersion)) {
                    LOGGER.info("Nueva versión disponible: " + latestVersion + " (actual: " + currentVersion + ")");
                    return new UpdateInfo(latestVersion, true, "Nueva versión disponible: " + latestVersion);
                } else {
                    LOGGER.info("Ya tienes la versión más reciente: " + currentVersion);
                }
            } else {
                LOGGER.warning("No se pudo obtener la versión más reciente desde el servidor");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error verificando actualizaciones: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Obtiene la versión más reciente desde el servidor
     * Intenta múltiples fuentes para mayor confiabilidad
     */
    private static String fetchLatestVersion() {
        // Intentar desde múltiples URLs
        for (String urlString : VERSION_CHECK_URLS) {
            try {
                URL url = new java.net.URI(urlString).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000); // 10 segundos timeout
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "CONNECTING-POS-UpdateChecker/1.0");
                
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            // Si es un archivo de propiedades, buscar la línea de versión
                            if (line.startsWith("version=") || line.startsWith("VERSION=")) {
                                String version = line.substring(line.indexOf("=") + 1).trim();
                                if (!version.isEmpty()) {
                                    LOGGER.info("Versión obtenida desde " + urlString + ": " + version);
                                    return version;
                                }
                            } else if (!line.isEmpty() && !line.startsWith("#") && !line.startsWith("//")) {
                                // Asumir que es la versión directamente
                                LOGGER.info("Versión obtenida desde " + urlString + ": " + line);
                                return line;
                            }
                        }
                    }
                } else {
                    LOGGER.log(Level.FINE, "Respuesta HTTP " + responseCode + " desde " + urlString);
                }
            } catch (java.net.SocketTimeoutException e) {
                LOGGER.log(Level.INFO, "Timeout al conectar con " + urlString + ", intentando siguiente URL...");
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "No se pudo obtener versión desde " + urlString + ": " + e.getMessage());
            }
        }
        
        // Intentar desde archivo local como último respaldo
        try {
            java.io.File versionFile = new java.io.File("VERSION.txt");
            if (!versionFile.exists()) {
                versionFile = new java.io.File("version.txt");
            }
            if (versionFile.exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new java.io.FileReader(versionFile))) {
                    String version = reader.readLine();
                    if (version != null && !version.trim().isEmpty()) {
                        LOGGER.info("Versión obtenida desde archivo local: " + version.trim());
                        return version.trim();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo leer archivo de versión local: " + e.getMessage());
        }
        
        LOGGER.warning("No se pudo obtener la versión desde ninguna fuente");
        return null;
    }
    
    /**
     * Compara dos versiones para determinar si la primera es más nueva
     * Formato esperado: "1.0.0-Sebastian", "1.0.1", "1.0.0-SNAPSHOT", etc.
     * Soporta versiones semánticas: MAJOR.MINOR.PATCH[-SUFFIX]
     */
    private static boolean isNewerVersion(String latest, String current) {
        try {
            // Normalizar versiones: remover espacios y convertir a minúsculas para comparación
            latest = latest.trim();
            current = current.trim();
            
            // Si son exactamente iguales, no hay actualización
            if (latest.equals(current)) {
                return false;
            }
            
            // Extraer números de versión (antes del guión si existe)
            String latestNum = latest.split("-")[0].trim();
            String currentNum = current.split("-")[0].trim();
            
            // Extraer sufijos (después del guión)
            String latestSuffix = latest.contains("-") ? latest.substring(latest.indexOf("-") + 1).trim() : "";
            String currentSuffix = current.contains("-") ? current.substring(current.indexOf("-") + 1).trim() : "";
            
            String[] latestParts = latestNum.split("\\.");
            String[] currentParts = currentNum.split("\\.");
            
            int maxLength = Math.max(latestParts.length, currentParts.length);
            
            // Comparar partes numéricas
            for (int i = 0; i < maxLength; i++) {
                int latestPart = 0;
                int currentPart = 0;
                
                try {
                    latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i].trim()) : 0;
                } catch (NumberFormatException e) {
                    LOGGER.warning("Parte de versión no numérica en 'latest': " + latestParts[i]);
                    latestPart = 0;
                }
                
                try {
                    currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i].trim()) : 0;
                } catch (NumberFormatException e) {
                    LOGGER.warning("Parte de versión no numérica en 'current': " + currentParts[i]);
                    currentPart = 0;
                }
                
                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            
            // Si las partes numéricas son iguales, comparar sufijos
            // SNAPSHOT < cualquier otra cosa
            // Sin sufijo > con sufijo (excepto SNAPSHOT)
            if (latestSuffix.isEmpty() && !currentSuffix.isEmpty() && !currentSuffix.equalsIgnoreCase("SNAPSHOT")) {
                return true; // latest sin sufijo es más nueva que current con sufijo
            }
            if (!latestSuffix.isEmpty() && latestSuffix.equalsIgnoreCase("SNAPSHOT")) {
                return false; // latest es SNAPSHOT, no es más nueva
            }
            if (currentSuffix.equalsIgnoreCase("SNAPSHOT") && !latestSuffix.equalsIgnoreCase("SNAPSHOT")) {
                return true; // latest no es SNAPSHOT y current sí
            }
            
            // Si ambos tienen sufijos diferentes y no son SNAPSHOT, comparar alfabéticamente
            if (!latestSuffix.isEmpty() && !currentSuffix.isEmpty() && 
                !latestSuffix.equalsIgnoreCase("SNAPSHOT") && !currentSuffix.equalsIgnoreCase("SNAPSHOT")) {
                return latestSuffix.compareToIgnoreCase(currentSuffix) > 0;
            }
            
            // Si llegamos aquí, las versiones numéricas son iguales y los sufijos no indican diferencia clara
            // Por seguridad, asumimos que latest es más nueva si son diferentes
            return !latest.equals(current);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error comparando versiones '" + latest + "' vs '" + current + "': " + e.getMessage(), e);
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

