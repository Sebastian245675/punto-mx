package com.openbravo.pos.supabase;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
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

    // Ruta del archivo de logs
    private static final String LOG_FILE_PATH = "supabase_logs.json";

    public SupabaseServiceREST(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        createLogFileIfNotExists();
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
     * Crea el archivo de logs si no existe
     */
    private void createLogFileIfNotExists() {
        File file = new File(LOG_FILE_PATH);
        try {
            if (!file.exists()) {
                file.createNewFile();
                try (FileWriter fw = new FileWriter(file)) {
                    fw.write("[]"); // inicializa como lista JSON vacía
                }
            }
        } catch (IOException e) {
            LOGGER.severe("No se pudo crear el archivo de logs: " + e.getMessage());
        }
    }

    /**
     * Escribe un registro JSON con el resultado de cada sincronización
     */
    private synchronized void logSyncResult(String table, int recordsCount, int responseCode, boolean success, String error) {
        try {
            List<ObjectNode> logs;

            // Leer logs existentes
            try (FileReader fr = new FileReader(LOG_FILE_PATH)) {
                logs = Arrays.asList(mapper.readValue(fr, ObjectNode[].class));
            } catch (EOFException e) {
                logs = new ArrayList<>();
            } catch (Exception e) {
                logs = new ArrayList<>();
            }

            // Crear nuevo registro
            ObjectNode logEntry = mapper.createObjectNode();
            logEntry.put("timestamp", new Date().toString());
            logEntry.put("table", table);
            logEntry.put("records_sent", recordsCount);
            logEntry.put("response_code", responseCode);
            logEntry.put("success", success);
            logEntry.put("error", error != null ? error : "");

            // Agregar y escribir
            List<ObjectNode> newLogs = new ArrayList<>(logs);
            newLogs.add(logEntry);

            try (FileWriter fw = new FileWriter(LOG_FILE_PATH)) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(fw, newLogs);
            }

        } catch (IOException e) {
            LOGGER.severe("Error al escribir en el archivo de logs: " + e.getMessage());
        }
    }
    /**
 * Obtiene todos los registros de una tabla en Supabase.
 */
    public List<Map<String, Object>> fetchData(String table) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        HttpURLConnection conn = null;

        try {
            URL url = new URL(baseUrl + "/" + table + "?select=*");
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
}
