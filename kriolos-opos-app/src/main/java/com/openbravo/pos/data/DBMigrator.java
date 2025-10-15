/*
 * Copyright (C) 2022 KriolOS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.openbravo.pos.data;


import com.openbravo.basic.BasicException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.DirectoryResourceAccessor;

/**
 * Database Migrator con soporte para Spring Boot JAR
 * Solución al problema de Liquibase con recursos en BOOT-INF/classes/ y JARs anidados
 * 
 * @author poolborges
 * @author Sebastian (Correcciones para .exe)
 */
public class DBMigrator {

    private final static Logger LOGGER = Logger.getLogger(DBMigrator.class.getName());
    private static Path logFile = null;
    
    static {
        try {
            Path logDir = Paths.get(System.getProperty("user.home"), "kriolopos");
            Files.createDirectories(logDir);
            logFile = logDir.resolve("liquibase-debug.log");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No se pudo crear archivo de log", e);
        }
    }
    
    /**
     * Sistema de logging para debug - escribe en archivo y consola
     */
    private static void debugLog(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String logMessage = "[" + timestamp + "] " + message;
        
        // Log en consola
        LOGGER.info(logMessage);
        
        // Log en archivo
        if (logFile != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile.toFile(), true))) {
                writer.write(logMessage);
                writer.newLine();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error escribiendo en log file", e);
            }
        }
    }
    
    public static void main(String[] args) throws SQLException {
        //execDBMigration();
    }

    public static void execDBMigration(com.openbravo.data.loader.Session dbSession) throws BasicException {
        boolean res = false;
        debugLog("=== Database Migration INICIO ===");
        
        try {
            Connection conn = dbSession.getConnection();
            JdbcConnection connliquibase = new JdbcConnection(conn);
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connliquibase);
            
            // Crear directorio temporal para recursos extraídos
            Path tempDir = Paths.get(System.getProperty("user.home"), "kriolopos", "liquibase-temp");
            Files.createDirectories(tempDir);
            debugLog("Directorio temporal creado: " + tempDir);
            
            // Extraer XMLs de Liquibase
            debugLog("=== Extrayendo XMLs de Liquibase ===");
            extractResource("/pos_liquidbase/db-changelog-master.xml", tempDir);
            extractResource("/pos_liquidbase/db-changelog-v4_5.xml", tempDir);
            extractResource("/pos_liquidbase/db-changelog-v4_5__LOAD.xml", tempDir);
            extractResource("/pos_liquidbase/db-changelog-v5_0.xml", tempDir);
            
            // Extraer templates y recursos referenciados
            debugLog("=== Extrayendo templates y recursos referenciados ===");
            extractResourcesRecursive("com/openbravo/pos/templates", tempDir);
            
            // Extraer imágenes desde JAR anidado
            debugLog("=== Extrayendo imágenes desde JAR anidado ===");
            extractResourcesFromNestedJar("BOOT-INF/lib/kriolos-opos-assets-image", "com/openbravo/images", tempDir);
            
            // Usar DirectoryResourceAccessor en lugar de ClassLoaderResourceAccessor
            debugLog("=== Creando Liquibase con DirectoryResourceAccessor ===");
            DirectoryResourceAccessor resourceAccessor = new DirectoryResourceAccessor(tempDir);
            Liquibase liquibase = new Liquibase("pos_liquidbase/db-changelog-master.xml", resourceAccessor, database);
            
            debugLog("Liquibase creado exitosamente");
            
            // Ejecutar migración
            debugLog("=== Ejecutando liquibase.update() ===");
            liquibase.update("pos-database-update");
            
            debugLog("=== ✓ Database Migration COMPLETADO EXITOSAMENTE ===");
            res = true;
            
        } catch (DatabaseException ex) {
            debugLog("❌ DB Migration DatabaseException: " + ex.getMessage());
            LOGGER.log(Level.SEVERE,"DB Migration Exception: " , ex);
            throw new BasicException("DB Migration Exception: ", ex);
        } catch (LiquibaseException | SQLException | IOException ex) {
            debugLog("❌ DB Migration Exception: " + ex.getMessage());
            LOGGER.log(Level.SEVERE, "DB Migration Exception: ", ex);
            throw new BasicException("DB Migration Exception: ", ex);
        }
    }
    
    /**
     * Extrae un recurso individual del JAR al sistema de archivos
     */
    private static void extractResource(String resourcePath, Path baseDir) throws IOException {
        debugLog("  Extrayendo: " + resourcePath);
        
        try (InputStream is = DBMigrator.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                debugLog("  ⚠ Recurso no encontrado: " + resourcePath);
                return;
            }
            
            String relativePath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            Path targetFile = baseDir.resolve(relativePath);
            Files.createDirectories(targetFile.getParent());
            Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
            
            debugLog("  ✓ Extraído: " + relativePath + " (" + Files.size(targetFile) + " bytes)");
        }
    }
    
    /**
     * Extrae recursivamente todos los archivos de un directorio desde el JAR principal
     * Maneja la estructura BOOT-INF/classes/ de Spring Boot
     */
    private static void extractResourcesRecursive(String resourcePath, Path baseDir) throws IOException {
        debugLog("  Extrayendo recursivamente: " + resourcePath);
        
        // Obtener la ubicación del JAR
        java.security.CodeSource codeSource = DBMigrator.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            debugLog("  ⚠ No se pudo obtener CodeSource");
            return;
        }
        
        java.net.URL location = codeSource.getLocation();
        String locationStr = location.toString();
        
        // Extraer el path del JAR
        String jarPath = locationStr;
        if (jarPath.startsWith("jar:file:")) {
            jarPath = jarPath.substring(9); // Remover "jar:file:"
            int bangIndex = jarPath.indexOf("!");
            if (bangIndex > 0) {
                jarPath = jarPath.substring(0, bangIndex);
            }
        } else if (jarPath.startsWith("file:")) {
            jarPath = jarPath.substring(5); // Remover "file:"
        }
        
        // En Windows, remover la barra inicial si existe
        if (jarPath.startsWith("/") && jarPath.length() > 2 && jarPath.charAt(2) == ':') {
            jarPath = jarPath.substring(1);
        }
        
        debugLog("  JAR path extraído: " + jarPath);
        
        java.io.File jarFile = new java.io.File(jarPath);
        if (!jarFile.exists() || !jarFile.getName().endsWith(".jar")) {
            debugLog("  ⚠ No es un JAR o no existe, intentando extracción directa");
            // Intentar extracción directa para desarrollo
            extractResourceDirectly(resourcePath, baseDir);
            return;
        }
        
        // Abrir el JAR y buscar archivos
        try (JarFile jar = new JarFile(jarFile)) {
            String bootInfPrefix = "BOOT-INF/classes/" + resourcePath;
            debugLog("  Buscando archivos con prefijo: " + bootInfPrefix);
            
            Enumeration<JarEntry> entries = jar.entries();
            int count = 0;
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                if (entry.getName().startsWith(bootInfPrefix) && !entry.isDirectory()) {
                    String relativePath = entry.getName().substring("BOOT-INF/classes/".length());
                    Path targetFile = baseDir.resolve(relativePath);
                    
                    // Crear directorio padre si no existe (sin fallar si ya existe)
                    Path parentDir = targetFile.getParent();
                    if (parentDir != null && !Files.exists(parentDir)) {
                        Files.createDirectories(parentDir);
                    }
                    
                    try (InputStream is = jar.getInputStream(entry)) {
                        Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                    count++;
                }
            }
            
            debugLog("  ✓ Extraídos " + count + " archivos de " + resourcePath);
        }
    }
    
    /**
     * Método auxiliar para extraer recursos en modo desarrollo (no JAR) o desde .exe
     */
    private static void extractResourceDirectly(String resourcePath, Path baseDir) throws IOException {
        debugLog("  Intentando extracción directa de: " + resourcePath);
        
        // Intentar obtener recursos desde el ClassLoader
        ClassLoader cl = DBMigrator.class.getClassLoader();
        
        // Si resourcePath empieza con /, quitarlo para el ClassLoader
        String cleanPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        
        // Intentar extraer todos los archivos del directorio
        try {
            java.net.URL resourceUrl = cl.getResource(cleanPath);
            if (resourceUrl != null) {
                debugLog("  URL del recurso: " + resourceUrl);
                
                // Si es un directorio, intentar extraer sus contenidos
                if (resourceUrl.getProtocol().equals("jar")) {
                    extractFromJarUrl(resourceUrl, cleanPath, baseDir);
                } else {
                    // Extracción directa para desarrollo
                    java.nio.file.Path sourcePath = java.nio.file.Paths.get(resourceUrl.toURI());
                    if (Files.isDirectory(sourcePath)) {
                        Files.walk(sourcePath).forEach(source -> {
                            try {
                                java.nio.file.Path dest = baseDir.resolve(sourcePath.relativize(source));
                                if (Files.isDirectory(source)) {
                                    if (!Files.exists(dest)) {
                                        Files.createDirectories(dest);
                                    }
                                } else {
                                    Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                                }
                            } catch (IOException e) {
                                // Continuar con el siguiente archivo
                            }
                        });
                    } else {
                        Path targetFile = baseDir.resolve(cleanPath);
                        Files.createDirectories(targetFile.getParent());
                        Files.copy(sourcePath, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                debugLog("  ✓ Extraído directamente: " + resourcePath);
            } else {
                debugLog("  ⚠ Recurso no encontrado: " + cleanPath);
            }
        } catch (Exception e) {
            debugLog("  ⚠ Error en extracción directa: " + e.getMessage());
            // No lanzar excepción, continuar con la migración
        }
    }
    
    /**
     * Extrae archivos desde una URL tipo jar:
     */
    private static void extractFromJarUrl(java.net.URL jarUrl, String resourcePath, Path baseDir) throws IOException {
        String jarPath = jarUrl.getPath();
        if (jarPath.contains("!")) {
            String[] parts = jarPath.split("!");
            String jarFile = parts[0];
            if (jarFile.startsWith("file:")) {
                jarFile = jarFile.substring(5);
            }
            
            try (JarFile jar = new JarFile(jarFile)) {
                String prefix = resourcePath.endsWith("/") ? resourcePath : resourcePath + "/";
                Enumeration<JarEntry> entries = jar.entries();
                
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    
                    // Buscar en BOOT-INF/classes/ o directamente
                    boolean matches = name.equals(resourcePath) || 
                                    name.startsWith(prefix) ||
                                    name.equals("BOOT-INF/classes/" + resourcePath) ||
                                    name.startsWith("BOOT-INF/classes/" + prefix);
                    
                    if (matches && !entry.isDirectory()) {
                        String relativePath = name;
                        if (relativePath.startsWith("BOOT-INF/classes/")) {
                            relativePath = relativePath.substring("BOOT-INF/classes/".length());
                        }
                        
                        Path targetFile = baseDir.resolve(relativePath);
                        Files.createDirectories(targetFile.getParent());
                        
                        try (InputStream is = jar.getInputStream(entry)) {
                            Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Extrae recursos desde un JAR anidado (JAR dentro del JAR de Spring Boot)
     * Esto resuelve el problema de las imágenes en BOOT-INF/lib/kriolos-opos-assets-image-*.jar
     */
    private static void extractResourcesFromNestedJar(String nestedJarPrefix, String resourcePrefix, Path baseDir) throws IOException {
        debugLog("  Extrayendo desde JAR anidado: " + nestedJarPrefix);
        
        // Obtener la ubicación del JAR principal
        java.security.CodeSource codeSource = DBMigrator.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            debugLog("  ⚠ No se pudo obtener CodeSource");
            return;
        }
        
        java.net.URL location = codeSource.getLocation();
        String locationStr = location.toString();
        
        // Extraer el path del JAR principal
        String jarPath = locationStr;
        if (jarPath.startsWith("jar:file:")) {
            jarPath = jarPath.substring(9);
            int bangIndex = jarPath.indexOf("!");
            if (bangIndex > 0) {
                jarPath = jarPath.substring(0, bangIndex);
            }
        } else if (jarPath.startsWith("file:")) {
            jarPath = jarPath.substring(5);
        }
        
        if (jarPath.startsWith("/") && jarPath.length() > 2 && jarPath.charAt(2) == ':') {
            jarPath = jarPath.substring(1);
        }
        
        java.io.File mainJarFile = new java.io.File(jarPath);
        if (!mainJarFile.exists() || !mainJarFile.getName().endsWith(".jar")) {
            debugLog("  ⚠ No es un JAR de Spring Boot, saltando extracción de JAR anidado");
            return;
        }
        
        try (JarFile mainJar = new JarFile(mainJarFile)) {
            // Buscar el JAR anidado
            JarEntry nestedJarEntry = null;
            Enumeration<JarEntry> entries = mainJar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith(nestedJarPrefix) && entry.getName().endsWith(".jar")) {
                    nestedJarEntry = entry;
                    break;
                }
            }
            
            if (nestedJarEntry == null) {
                debugLog("  ⚠ JAR anidado no encontrado con prefijo: " + nestedJarPrefix);
                return;
            }
            
            debugLog("  Encontrado JAR anidado: " + nestedJarEntry.getName());
            
            // Extraer el JAR anidado a un archivo temporal
            Path tempJarPath = Files.createTempFile("nested-", ".jar");
            try (InputStream is = mainJar.getInputStream(nestedJarEntry)) {
                Files.copy(is, tempJarPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Abrir el JAR anidado y extraer recursos
            try (JarFile nestedJar = new JarFile(tempJarPath.toFile())) {
                Enumeration<JarEntry> nestedEntries = nestedJar.entries();
                int count = 0;
                while (nestedEntries.hasMoreElements()) {
                    JarEntry entry = nestedEntries.nextElement();
                    
                    if (entry.getName().startsWith(resourcePrefix) && !entry.isDirectory()) {
                        Path targetFile = baseDir.resolve(entry.getName());
                        Files.createDirectories(targetFile.getParent());
                        
                        try (InputStream is = nestedJar.getInputStream(entry)) {
                            Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                        count++;
                    }
                }
                
                debugLog("  ✓ Extraídos " + count + " archivos desde JAR anidado");
            } finally {
                // Limpiar archivo temporal
                Files.deleteIfExists(tempJarPath);
            }
        }
    }
}
