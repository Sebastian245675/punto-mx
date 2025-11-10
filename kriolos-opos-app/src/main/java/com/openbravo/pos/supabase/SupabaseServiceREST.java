package com.openbravo.pos.supabase;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SupabaseServiceREST {

    private static final Logger LOGGER = Logger.getLogger(SupabaseServiceREST.class.getName());
    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    // Timeouts to avoid hanging requests
    private static final int CONNECT_TIMEOUT_MS = 5000; // 5s
    private static final int READ_TIMEOUT_MS = 20000;   // 20s
    // Batch size to avoid oversized payloads
    private static final int BATCH_SIZE = 500;

    // Ruta del archivo de logs (en el directorio home del usuario, como AppConfig)
    private static String LOG_FILE_PATH = null;

    public SupabaseServiceREST(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        initializeLogFilePath();
        createLogFileIfNotExists();
    }
    
    /**
     * Inicializa la ruta del archivo de logs en el directorio home del usuario
     */
    private static void initializeLogFilePath() {
        if (LOG_FILE_PATH == null) {
            String userHome = System.getProperty("user.home");
            LOG_FILE_PATH = new File(userHome, "supabase_logs.json").getAbsolutePath();
            Logger.getLogger(SupabaseServiceREST.class.getName()).info("Archivo de logs de Supabase: " + LOG_FILE_PATH);
        }
    }

    /**
     * Envía una lista de registros a una tabla en Supabase
     */
    public boolean syncData(String table, List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) {
            logSyncResult(table, 0, 204, true, null);
            return true;
        }

        boolean overallSuccess = true;
        int totalSent = 0;
        int lastResponseCode = 0;
        String lastErrorMessage = null;

        long startNs = System.nanoTime();
        try {
            for (int i = 0; i < records.size(); i += BATCH_SIZE) {
                List<Map<String, Object>> batch = records.subList(i, Math.min(i + BATCH_SIZE, records.size()));
                SendResult sendResult = sendBatch(table, batch);
                totalSent += batch.size();
                lastResponseCode = sendResult.responseCode;
                if (!sendResult.success) {
                    overallSuccess = false;
                    lastErrorMessage = sendResult.errorMessage;
                    // No detenemos inmediatamente para intentar enviar el resto, pero marcamos fallo
                }
            }
        } catch (Exception e) {
            overallSuccess = false;
            lastErrorMessage = e.getMessage();
            LOGGER.severe("Error general sincronizando tabla " + table + ": " + e.getMessage());
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        LOGGER.info("syncData(" + table + ") envió " + totalSent + " registros en " + elapsedMs + "ms. Éxito=" + overallSuccess);

        // Registrar resultado en JSON (última respuesta)
        logSyncResult(table, totalSent, lastResponseCode, overallSuccess, lastErrorMessage);
        return overallSuccess;
    }
    
    /**
     * Inserta una lista de registros en una tabla de Supabase (solo INSERT, sin UPSERT)
     * Esto evita que se eliminen o sobrescriban registros existentes
     */
    public boolean insertData(String table, List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) {
            logSyncResult(table, 0, 204, true, null);
            return true;
        }

        boolean overallSuccess = true;
        int totalSent = 0;
        int lastResponseCode = 0;
        String lastErrorMessage = null;

        long startNs = System.nanoTime();
        try {
            for (int i = 0; i < records.size(); i += BATCH_SIZE) {
                List<Map<String, Object>> batch = records.subList(i, Math.min(i + BATCH_SIZE, records.size()));
                SendResult sendResult = insertBatch(table, batch);
                totalSent += batch.size();
                lastResponseCode = sendResult.responseCode;
                if (!sendResult.success) {
                    overallSuccess = false;
                    lastErrorMessage = sendResult.errorMessage;
                    // No detenemos inmediatamente para intentar enviar el resto, pero marcamos fallo
                }
            }
        } catch (Exception e) {
            overallSuccess = false;
            lastErrorMessage = e.getMessage();
            LOGGER.severe("Error general insertando en tabla " + table + ": " + e.getMessage());
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        LOGGER.info("insertData(" + table + ") envió " + totalSent + " registros en " + elapsedMs + "ms. Éxito=" + overallSuccess);

        // Registrar resultado en JSON (última respuesta)
        logSyncResult(table, totalSent, lastResponseCode, overallSuccess, lastErrorMessage);
        return overallSuccess;
    }

    private static class SendResult {
        final boolean success;
        final int responseCode;
        final String errorMessage;

        SendResult(boolean success, int responseCode, String errorMessage) {
            this.success = success;
            this.responseCode = responseCode;
            this.errorMessage = errorMessage;
        }
    }

    private SendResult sendBatch(String table, List<Map<String, Object>> batch) {
        int responseCode = 0;
        String errorMessage = null;
        boolean success = false;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/" + table);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST"); // O "PATCH" si usas UPSERT
            conn.setRequestProperty("apikey", apiKey);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates"); // UPSERT en Supabase
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setDoInput(true);

            String json = mapper.writeValueAsString(batch);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
                os.flush();
            }

            responseCode = conn.getResponseCode();
            success = responseCode >= 200 && responseCode < 300;

            // Leer y cerrar streams para liberar la conexión
            InputStream stream = success ? conn.getInputStream() : conn.getErrorStream();
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    if (!success) {
                        errorMessage = sb.toString();
                    }
                }
            }

        } catch (Exception e) {
            errorMessage = e.getMessage();
            LOGGER.severe("Error sincronizando batch en tabla " + table + ": " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return new SendResult(success, responseCode, errorMessage);
    }
    
    /**
     * Inserta un batch de registros sin UPSERT (solo INSERT)
     * Esto evita que se sobrescriban registros existentes
     */
    private SendResult insertBatch(String table, List<Map<String, Object>> batch) {
        int responseCode = 0;
        String errorMessage = null;
        boolean success = false;
        HttpURLConnection conn = null;
        String responseBody = null;
        try {
            URL url = new URL(baseUrl + "/" + table);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", apiKey);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=representation"); // Para obtener los datos insertados
            // NO usar "Prefer: resolution=merge-duplicates" para hacer solo INSERT
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setDoInput(true);

            String json = mapper.writeValueAsString(batch);
            LOGGER.info("Enviando batch a " + table + " (" + batch.size() + " registros): " + 
                       (json.length() > 500 ? json.substring(0, 500) + "..." : json));
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
                os.flush();
            }

            responseCode = conn.getResponseCode();
            
            // Leer respuesta (tanto éxito como error)
            InputStream stream = responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    responseBody = sb.toString();
                }
            }

            // Solo considerar exitoso si es 201 (Created) o 200 (OK)
            // 409 (Conflict) es un error y debe ser manejado
            success = responseCode == 201 || responseCode == 200;
            
            if (success) {
                LOGGER.info("Batch insertado exitosamente en " + table + ". Response code: " + responseCode + 
                           ", registros: " + batch.size());
                if (responseBody != null && !responseBody.isEmpty()) {
                    LOGGER.fine("Respuesta de Supabase: " + responseBody);
                }
            } else {
                errorMessage = responseBody != null ? responseBody : "Error desconocido";
                LOGGER.severe("Error insertando batch en " + table + ". Response code: " + responseCode + 
                             ", Error: " + errorMessage);
                // Si es 409, puede ser duplicado, pero aún así es un error
                if (responseCode == 409) {
                    LOGGER.warning("Conflicto (409) al insertar en " + table + 
                                  ". Puede ser porque el registro ya existe. Error: " + errorMessage);
                }
            }

        } catch (Exception e) {
            errorMessage = e.getMessage();
            LOGGER.severe("Error insertando batch en tabla " + table + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return new SendResult(success, responseCode, errorMessage);
    }

    /**
     * Crea el archivo de logs si no existe o está vacío
     */
    private void createLogFileIfNotExists() {
        try {
            File file = new File(LOG_FILE_PATH);
            
            // Crear el directorio padre si no existe
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
                LOGGER.info("Directorio de logs creado: " + parentDir.getAbsolutePath());
            }
            
            // Crear o inicializar el archivo si no existe o está vacío
            if (!file.exists() || file.length() == 0) {
                try (FileWriter fw = new FileWriter(file)) {
                    fw.write("[]"); // inicializa como lista JSON vacía
                    fw.flush();
                }
                LOGGER.info("Archivo de logs inicializado: " + LOG_FILE_PATH);
            } else {
                LOGGER.info("Archivo de logs ya existe: " + LOG_FILE_PATH + " (tamaño: " + file.length() + " bytes)");
            }
        } catch (IOException e) {
            LOGGER.severe("No se pudo crear el archivo de logs en " + LOG_FILE_PATH + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            LOGGER.severe("Error inesperado al inicializar archivo de logs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Escribe un registro JSON con el resultado de cada sincronización
     * IMPORTANTE: Este método es synchronized para evitar problemas de concurrencia
     */
    private synchronized void logSyncResult(String table, int recordsCount, int responseCode, boolean success, String error) {
        // Asegurar que la ruta del log esté inicializada
        if (LOG_FILE_PATH == null) {
            initializeLogFilePath();
        }
        
        File logFile = new File(LOG_FILE_PATH);
        FileWriter fw = null;
        
        try {
            List<ObjectNode> logs = new ArrayList<>();

            // Leer logs existentes
            if (logFile.exists() && logFile.length() > 0) {
                try (FileReader fr = new FileReader(logFile, java.nio.charset.StandardCharsets.UTF_8)) {
                    String content = "";
                    try (BufferedReader br = new BufferedReader(fr)) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        content = sb.toString().trim();
                    }
                    
                    if (!content.isEmpty() && !content.equals("[]") && content.startsWith("[")) {
                        try {
                            ObjectNode[] logArray = mapper.readValue(content, ObjectNode[].class);
                            logs = new ArrayList<>(Arrays.asList(logArray));
                            LOGGER.fine("Logs existentes cargados: " + logs.size() + " entradas");
                        } catch (Exception parseEx) {
                            LOGGER.warning("Error parseando logs existentes, iniciando lista vacía: " + parseEx.getMessage());
                            logs = new ArrayList<>();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warning("Error leyendo logs existentes, iniciando lista vacía: " + e.getMessage());
                    logs = new ArrayList<>();
                }
            } else {
                LOGGER.fine("Archivo de logs no existe o está vacío, iniciando lista vacía");
            }

            // Crear nuevo registro
            ObjectNode logEntry = mapper.createObjectNode();
            logEntry.put("timestamp", new Date().toString());
            logEntry.put("table", table);
            logEntry.put("records_sent", recordsCount);
            logEntry.put("response_code", responseCode);
            logEntry.put("success", success);
            logEntry.put("error", error != null ? error : "");

            // Agregar nuevo log
            logs.add(logEntry);
            LOGGER.info("Nuevo log agregado para tabla " + table + ": " + (success ? "éxito" : "error") + ", registros: " + recordsCount);
            
            // Limitar a los últimos 1000 logs para evitar que el archivo crezca demasiado
            if (logs.size() > 1000) {
                logs = logs.subList(logs.size() - 1000, logs.size());
                LOGGER.fine("Logs limitados a los últimos 1000 registros");
            }

            // Crear el directorio padre si no existe
            File parentDir = logFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
                LOGGER.info("Directorio de logs creado: " + parentDir.getAbsolutePath());
            }

            // Escribir todos los logs - usar FileWriter sin try-with-resources para evitar cierre prematuro
            fw = new FileWriter(logFile, java.nio.charset.StandardCharsets.UTF_8);
            mapper.writerWithDefaultPrettyPrinter().writeValue(fw, logs);
            fw.flush();
            fw.close();
            fw = null; // Marcar como cerrado para evitar doble cierre
            
            LOGGER.info("Log escrito exitosamente en " + LOG_FILE_PATH + " para tabla " + table + " (total de logs: " + logs.size() + ")");

        } catch (IOException writeEx) {
            LOGGER.severe("Error al escribir en el archivo de logs " + LOG_FILE_PATH + ": " + writeEx.getMessage());
            writeEx.printStackTrace();
            
            // Cerrar el FileWriter si está abierto
            if (fw != null) {
                try {
                    fw.close();
                } catch (Exception e) {
                    // Ignorar errores al cerrar
                }
            }
            
            // Intentar escribir un log de error simple como fallback
            try {
                String errorLog = String.format("[%s] ERROR escribiendo log JSON: tabla=%s, error=%s\n", 
                    new Date(), table, writeEx.getMessage());
                Files.write(logFile.toPath(), errorLog.getBytes(java.nio.charset.StandardCharsets.UTF_8), 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.APPEND);
            } catch (Exception fallbackEx) {
                LOGGER.severe("Error crítico: no se pudo escribir ni siquiera el log de error: " + fallbackEx.getMessage());
            }
        } catch (Exception e) {
            LOGGER.severe("Error inesperado al escribir logs en " + LOG_FILE_PATH + ": " + e.getMessage());
            e.printStackTrace();
            
            // Cerrar el FileWriter si está abierto
            if (fw != null) {
                try {
                    fw.close();
                } catch (Exception closeEx) {
                    // Ignorar errores al cerrar
                }
            }
            
            // Intentar escribir un log de error simple como fallback
            try {
                String errorLog = String.format("[%s] ERROR CRÍTICO escribiendo log: tabla=%s, error=%s\n", 
                    new Date(), table, e.getMessage());
                Files.write(logFile.toPath(), errorLog.getBytes(java.nio.charset.StandardCharsets.UTF_8), 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.APPEND);
            } catch (Exception fallbackEx) {
                LOGGER.severe("Error crítico: no se pudo escribir ni siquiera el log de error: " + fallbackEx.getMessage());
            }
        } finally {
            // Asegurar que el FileWriter se cierre incluso si hay un error
            if (fw != null) {
                try {
                    fw.close();
                } catch (Exception e) {
                    // Ignorar errores al cerrar
                }
            }
        }
    }
    /**
 * Obtiene todos los registros de una tabla en Supabase.
 * @param table Nombre de la tabla, puede incluir parámetros de consulta (ej: "cierres?select=id")
 */
    public List<Map<String, Object>> fetchData(String table) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        HttpURLConnection conn = null;

        try {
            // Si la tabla ya incluye parámetros de consulta (?), no agregar más
            String urlStr = baseUrl + "/" + table;
            if (!table.contains("?")) {
                urlStr += "?select=*";
            }
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", apiKey);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);

            int responseCode = conn.getResponseCode();
            boolean success = responseCode >= 200 && responseCode < 300;

            InputStream stream = success ? conn.getInputStream() : conn.getErrorStream();
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }

                    if (success) {
                        // Parseamos el JSON devuelto por Supabase
                        resultList = mapper.readValue(sb.toString(), List.class);
                        LOGGER.info("fetchData(" + table + ") obtuvo " + resultList.size() + " registros.");
                    } else {
                        LOGGER.severe("Error al obtener datos de Supabase (" + table + "): " + sb);
                        logSyncResult(table, 0, responseCode, false, sb.toString());
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.severe("Error al obtener datos de Supabase (" + table + "): " + e.getMessage());
            logSyncResult(table, 0, 500, false, e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }

        return resultList;
    }

    /**
     * Llama a una función RPC en Supabase
     * @param functionName Nombre de la función RPC
     * @param params Parámetros para la función (Map o null si no hay parámetros)
     * @return true si la llamada fue exitosa, false en caso contrario
     */
    public boolean callRPC(String functionName, Map<String, Object> params) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/rpc/" + functionName);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", apiKey);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // Si hay parámetros, enviarlos como JSON
            if (params != null && !params.isEmpty()) {
                String json = mapper.writeValueAsString(params);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes());
                    os.flush();
                }
            } else {
                // Enviar objeto vacío si no hay parámetros
                try (OutputStream os = conn.getOutputStream()) {
                    os.write("{}".getBytes());
                    os.flush();
                }
            }

            int responseCode = conn.getResponseCode();
            boolean success = responseCode >= 200 && responseCode < 300;

            // Leer respuesta
            InputStream stream = success ? conn.getInputStream() : conn.getErrorStream();
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    if (success) {
                        LOGGER.info("callRPC(" + functionName + ") ejecutada exitosamente. Respuesta: " + sb.toString());
                    } else {
                        LOGGER.severe("Error al llamar función RPC (" + functionName + "): " + sb.toString());
                    }
                }
            }

            return success;

        } catch (Exception e) {
            LOGGER.severe("Error al llamar función RPC (" + functionName + "): " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
