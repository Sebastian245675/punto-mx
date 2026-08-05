package com.openbravo.pos.supabase;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para interactuar con la tabla 'clientes' en Supabase.
 * Proporciona métodos para sincronizar puntos y datos básicos.
 */
public class CustomerServiceSupabase {

    private static final Logger LOGGER = Logger.getLogger(CustomerServiceSupabase.class.getName());
    private final String supabaseUrl;
    private final String apiKey;

    public CustomerServiceSupabase(String supabaseUrl, String apiKey) {
        this.supabaseUrl = supabaseUrl;
        this.apiKey = apiKey;
    }

    /**
     * Sincroniza un cliente completo (Upsert).
     */
    public boolean upsertCustomer(String id, String codigo, String nombre, String email, String phone, String direccion, int puntos) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("codigo", codigo != null ? codigo : id);
        data.put("nombre", nombre != null ? nombre : "Cliente sin nombre");
        // No enviamos email porque no existe en la tabla clientes de Supabase
        data.put("telefono", phone);
        data.put("direccion", direccion);
        data.put("puntos", puntos);
        try {
            data.put("creado_en", java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()));
            // Simular un array vacío estilo JSON si fuera necesario
            data.put("compras", new ArrayList<>());
        } catch (Exception ignore) {}

        return callSupabase("clientes", "POST", data, "id=eq." + id);
    }

    /**
     * Actualiza solo los puntos de un cliente.
     */
    public boolean updatePuntos(String id, int puntos) {
        Map<String, Object> data = new HashMap<>();
        data.put("puntos", puntos);
        return callSupabase("clientes", "PATCH", data, "id=eq." + id);
    }

    /**
     * Obtiene todos los clientes con sus campos básicos.
     */
    public List<Map<String, Object>> fetchAllCustomers() {
        return fetchData("clientes", "id,nombre,telefono,puntos,codigo,direccion");
    }

    private List<Map<String, Object>> fetchData(String table, String select) {
        HttpURLConnection conn = null;
        try {
            String urlStr = supabaseUrl + "/" + table + "?select=" + select;
            URL url = URI.create(urlStr).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", apiKey);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return parseJsonList(response.toString());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching data from Supabase: " + e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return new ArrayList<>();
    }

    private boolean callSupabase(String table, String method, Map<String, Object> data, String query) {
        try {
            String urlStr = supabaseUrl + "/" + table;
            if (query != null && !query.isEmpty()) {
                urlStr += "?" + query;
            }
            
            String json = mapToJson(data);
            
            // Usar el HttpClient nuevo de Java 11+ para soportar PATCH de forma nativa
            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .header("apikey", apiKey)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(java.time.Duration.ofSeconds(10)); // Timeout global de la petición
            
            if ("POST".equals(method)) {
                requestBuilder.header("Prefer", "resolution=merge-duplicates");
                requestBuilder.POST(java.net.http.HttpRequest.BodyPublishers.ofString(json));
            } else if ("PATCH".equals(method)) {
                requestBuilder.method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString(json));
            } else {
                requestBuilder.method(method, java.net.http.HttpRequest.BodyPublishers.ofString(json));
            }
            
            java.net.http.HttpRequest request = requestBuilder.build();
            
            // Instanciar cliente con tiempo máximo de conexión (5 segundos)
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .build();
                    
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            
            int responseCode = response.statusCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                LOGGER.info("Supabase response SUCCESS (" + responseCode + ") for table " + table);
                return true;
            } else {
                LOGGER.severe("Supabase response ERROR (" + responseCode + ") for table " + table + " con método " + method + ": " + response.body());
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calling Supabase: " + e.getMessage(), e);
            return false;
        }
    }

    // Métodos auxiliares simples para JSON (en un entorno real usaríamos una librería como Jackson o Gson)
    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
            } else {
                sb.append(value);
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private List<Map<String, Object>> parseJsonList(String json) {
        List<Map<String, Object>> list = new ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{[^\\{]*?\\}");
        java.util.regex.Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            list.add(parseJsonObject(matcher.group()));
        }
        return list;
    }

    private Map<String, Object> parseJsonObject(String json) {
        Map<String, Object> map = new HashMap<>();
        // Quitar llaves
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        // Regex para encontrar "clave": valor, respetando comas dentro de comillas
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*(\"(?:\\\\\"|[^\"])*\"|[^,]+)");
        java.util.regex.Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2).trim();

            if (value.startsWith("\"")) {
                // Quitar comillas y manejar comillas escapadas
                map.put(key, value.substring(1, value.length() - 1).replace("\\\"", "\""));
            } else if (value.equals("null")) {
                map.put(key, null);
            } else if (value.equals("true") || value.equals("false")) {
                map.put(key, Boolean.parseBoolean(value));
            } else {
                try {
                    if (value.contains(".")) {
                        map.put(key, Double.parseDouble(value));
                    } else {
                        map.put(key, Integer.parseInt(value));
                    }
                } catch (NumberFormatException e) {
                    map.put(key, value);
                }
            }
        }
        return map;
    }
}
