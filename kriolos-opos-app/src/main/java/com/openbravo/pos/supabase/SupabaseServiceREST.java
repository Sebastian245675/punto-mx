package com.openbravo.pos.supabase;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Level;
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

            // Considerar exitoso si es 201 (Created), 200 (OK) o 409 (Conflict - duplicado)
            // 409 significa que el registro ya existe, lo cual es aceptable
            // También aceptar 204 (No Content) como éxito
            success = responseCode == 201 || responseCode == 200 || responseCode == 204 || responseCode == 409;
            
            if (!success) {
                errorMessage = responseBody != null ? responseBody : "Error desconocido (código: " + responseCode + ")";
                // Verificar si el error es porque la tabla no existe
                if (errorMessage.contains("PGRST205") && errorMessage.contains("Could not find the table")) {
                    LOGGER.severe("ERROR: La tabla '" + table + "' no existe en Supabase. Debe crearla primero.");
                }
            }

        } catch (java.net.UnknownHostException | java.net.ConnectException e) {
            // Errores de conexión - no intentar más
            errorMessage = "Error de conexión: " + e.getMessage();
            success = false;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            success = false;
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
     * Registra el resultado de una operación de sincronización en el archivo JSON local
     */
    public synchronized void logSyncResult(String table, int recordsCount, int responseCode, boolean success, String error) {
        // Asegurar que la ruta del log esté inicializada
        if (LOG_FILE_PATH == null) {
            initializeLogFilePath();
        }
        
        File logFile = new File(LOG_FILE_PATH);

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

            // Escribir todos los logs - usando el File de forma directa con Jackson para evitar el cierre prematuro del stream
            mapper.writerWithDefaultPrettyPrinter().writeValue(logFile, logs);
            
            LOGGER.info("Log escrito exitosamente en " + LOG_FILE_PATH + " para tabla " + table + " (total de logs: " + logs.size() + ")");

        } catch (IOException writeEx) {
            LOGGER.severe("Error al escribir en el archivo de logs " + LOG_FILE_PATH + ": " + writeEx.getMessage());
            writeEx.printStackTrace();
            
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
                        // Verificar si el error es porque la tabla no existe
                        String errorMsg = sb.toString();
                        if (errorMsg != null && errorMsg.contains("PGRST205") && errorMsg.contains("Could not find the table")) {
                            LOGGER.severe("ERROR: La tabla '" + table + "' no existe en Supabase. Debe crearla primero.");
                        } else {
                            LOGGER.severe("Error al obtener datos de Supabase (" + table + "): " + errorMsg);
                        }
                        logSyncResult(table, 0, responseCode, false, errorMsg);
                        return null; // Retornar null para indicar error
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error excepcional en fetchData(" + table + "): " + e.getMessage(), e);
            logSyncResult(table, 0, 500, false, e.getMessage());
            return null; // Retornar null en caso de excepción
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
