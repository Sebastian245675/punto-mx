package com.openbravo.pos.forms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestor de actualizaciones - Descarga y aplica actualizaciones sin borrar datos
 * @author Sebastian
 */
public class UpdateManager {
    
    private static final Logger LOGGER = Logger.getLogger(UpdateManager.class.getName());
    
    // URL base donde están los archivos JAR actualizados
    private static final String UPDATE_BASE_URL = "https://github.com/Sebastian245675/punto-mx/releases/download/";
    
    /**
     * Descarga y aplica una actualización
     * @param version Versión a descargar
     * @param progressCallback Callback para reportar progreso
     * @return true si la actualización fue exitosa
     */
    public static boolean applyUpdate(String version, ProgressCallback progressCallback) {
        try {
            progressCallback.onProgress(0, "Iniciando actualización...");
            
            // Obtener ruta del JAR actual
            String currentJarPath = getCurrentJarPath();
            if (currentJarPath == null) {
                progressCallback.onError("No se pudo determinar la ubicación del JAR actual");
                return false;
            }
            
            File currentJar = new File(currentJarPath);
            File backupJar = new File(currentJarPath + ".backup");
            File newJar = new File(currentJarPath + ".new");
            
            // 1. Crear respaldo del JAR actual
            progressCallback.onProgress(10, "Creando respaldo...");
            Files.copy(currentJar.toPath(), backupJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // 2. Descargar nuevo JAR
            progressCallback.onProgress(20, "Descargando nueva versión...");
            String downloadUrl = UPDATE_BASE_URL + "v" + version + "/kriolos-pos.jar";
            if (!downloadFile(downloadUrl, newJar, progressCallback)) {
                // Si falla, intentar desde URL alternativa
                downloadUrl = "https://raw.githubusercontent.com/Sebastian245675/punto-mx/main/kriolos-opos-app/target/kriolos-pos.jar";
                if (!downloadFile(downloadUrl, newJar, progressCallback)) {
                    progressCallback.onError("No se pudo descargar la actualización");
                    return false;
                }
            }
            
            // 3. Verificar que el nuevo JAR es válido (tiene tamaño razonable)
            if (newJar.length() < 1000) {
                progressCallback.onError("El archivo descargado parece estar corrupto");
                newJar.delete();
                return false;
            }
            
            // 4. Reemplazar JAR actual con el nuevo
            progressCallback.onProgress(90, "Aplicando actualización...");
            
            // En Windows, necesitamos renombrar en lugar de reemplazar directamente
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                // Cerrar la aplicación actual primero (se hará desde el diálogo)
                File tempOld = new File(currentJarPath + ".old");
                if (currentJar.exists()) {
                    currentJar.renameTo(tempOld);
                }
                newJar.renameTo(currentJar);
            } else {
                Files.move(newJar.toPath(), currentJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            
            progressCallback.onProgress(100, "¡Actualización completada!");
            LOGGER.info("Actualización aplicada exitosamente a versión " + version);
            
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error aplicando actualización", e);
            progressCallback.onError("Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Descarga un archivo desde una URL
     */
    private static boolean downloadFile(String urlString, File destination, ProgressCallback callback) {
        try {
            URL url = new java.net.URI(urlString).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                LOGGER.warning("No se pudo descargar: " + responseCode);
                return false;
            }
            
            long fileSize = conn.getContentLengthLong();
            try (InputStream inputStream = conn.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(destination)) {
                
                byte[] buffer = new byte[4096];
                long totalBytesRead = 0;
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    if (fileSize > 0) {
                        int progress = 20 + (int) ((totalBytesRead * 70) / fileSize);
                        callback.onProgress(progress, "Descargando... " + 
                            (totalBytesRead / 1024 / 1024) + " MB / " + 
                            (fileSize / 1024 / 1024) + " MB");
                    }
                }
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error descargando archivo: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene la ruta del JAR actual
     */
    private static String getCurrentJarPath() {
        try {
            // Obtener la ruta del JAR desde la clase
            String path = UpdateManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            
            // Decodificar URL encoding
            path = java.net.URLDecoder.decode(path, "UTF-8");
            
            // En Windows, remover el "/" inicial si existe
            if (path.startsWith("/") && path.length() > 2 && path.charAt(2) == ':') {
                path = path.substring(1);
            }
            
            return path;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo obtener ruta del JAR: " + e.getMessage());
            // Intentar método alternativo
            return new File("kriolos-opos-app/target/kriolos-pos.jar").getAbsolutePath();
        }
    }
    
    /**
     * Restaura el respaldo en caso de error
     */
    public static boolean restoreBackup() {
        try {
            String currentJarPath = getCurrentJarPath();
            File currentJar = new File(currentJarPath);
            File backupJar = new File(currentJarPath + ".backup");
            
            if (backupJar.exists()) {
                Files.copy(backupJar.toPath(), currentJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Respaldo restaurado exitosamente");
                return true;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error restaurando respaldo", e);
        }
        return false;
    }
    
    /**
     * Interfaz para reportar progreso de la actualización
     */
    public interface ProgressCallback {
        void onProgress(int percentage, String message);
        void onError(String error);
    }
}

