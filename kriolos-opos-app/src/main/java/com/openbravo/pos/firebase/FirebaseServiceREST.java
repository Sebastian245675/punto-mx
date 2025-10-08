//    KriolOS POS
//    Copyright (c) 2019-2023 KriolOS
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>

package com.openbravo.pos.firebase;

import com.openbravo.pos.forms.AppConfig;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CompletableFuture;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Servicio para sincronización con Firebase usando REST API
 * Compatible con aplicaciones web Firebase
 */
public class FirebaseServiceREST {
    
    private static final Logger LOGGER = Logger.getLogger(FirebaseServiceREST.class.getName());
    private static FirebaseServiceREST instance;
    
    private AppConfig config;
    private HttpClient httpClient;
    private Gson gson;
    private String projectId;
    private String apiKey;
    private String baseUrl;
    private boolean initialized = false;
    
    private FirebaseServiceREST() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create();
    }
    
    public static synchronized FirebaseServiceREST getInstance() {
        if (instance == null) {
            instance = new FirebaseServiceREST();
        }
        return instance;
    }
    
    /**
     * Inicializa el servicio Firebase con la configuración
     */
    public boolean initialize(AppConfig config) {
        this.config = config;
        
        if (initialized) {
            LOGGER.info("Firebase REST Service ya está inicializado");
            return true;
        }
        
        try {
            projectId = config.getProperty("firebase.projectid");
            apiKey = config.getProperty("firebase.apikey");
            
            if (projectId == null || projectId.trim().isEmpty()) {
                LOGGER.warning("Firebase Project ID no configurado");
                return false;
            }
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                LOGGER.warning("Firebase API Key no configurada");
                return false;
            }
            
            // URL base para Firestore REST API
            baseUrl = "https://firestore.googleapis.com/v1/projects/" + projectId + "/databases/(default)/documents";
            
            initialized = true;
            LOGGER.info("Firebase REST Service inicializado para proyecto: " + projectId);
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar Firebase REST Service", e);
            initialized = false;
            return false;
        }
    }
    
    /**
     * Prueba la conexión con Firebase
     */
    public CompletableFuture<Boolean> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!hasInternetConnection()) {
                    LOGGER.warning("No hay conexión a internet");
                    return false;
                }
                
                if (!initialize(AppConfig.getInstance())) {
                    return false;
                }
                
                // Probar una consulta simple a Firestore
                String testUrl = baseUrl + "/test";
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(testUrl))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                
                LOGGER.info("Test de conexión Firebase - Status: " + response.statusCode());
                return response.statusCode() == 200 || response.statusCode() == 404; // 404 es OK para colección vacía
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al probar conexión con Firebase", e);
                return false;
            }
        });
    }
    
    /**
     * Sincroniza usuarios a Firestore
     */
    public CompletableFuture<Boolean> syncUsuarios(List<Map<String, Object>> usuarios) {
        return syncCollection("usuarios", usuarios);
    }
    
    /**
     * Sincroniza ventas a Firestore
     */
    public CompletableFuture<Boolean> syncVentas(List<Map<String, Object>> ventas) {
        return syncCollection("ventas", ventas);
    }
    
    /**
     * Sincroniza productos a Firestore
     */
    public CompletableFuture<Boolean> syncProductos(List<Map<String, Object>> productos) {
        return syncCollection("productos", productos);
    }
    
    /**
     * Sincroniza clientes a Firestore
     */
    public CompletableFuture<Boolean> syncClientes(List<Map<String, Object>> clientes) {
        return syncCollection("clientes", clientes);
    }
    
    /**
     * Sincroniza categorías a Firestore
     */
    public CompletableFuture<Boolean> syncCategorias(List<Map<String, Object>> categorias) {
        return syncCollection("categorias", categorias);
    }
    
    /**
     * Sebastian - Sincroniza puntos de clientes a Firestore
     */
    public CompletableFuture<Boolean> syncPuntosClientes(List<Map<String, Object>> puntos) {
        return syncCollection("puntos_clientes", puntos);
    }
    
    /**
     * Sebastian - Sincroniza cierres de caja a Firestore
     */
    public CompletableFuture<Boolean> syncCierresCaja(List<Map<String, Object>> cierres) {
        return syncCollection("cierres_caja", cierres);
    }
    
    /**
     * Sebastian - Sincroniza formas de pago a Firestore
     */
    public CompletableFuture<Boolean> syncFormasPago(List<Map<String, Object>> pagos) {
        return syncCollection("formas_pago", pagos);
    }
    
    /**
     * Sebastian - Sincroniza impuestos a Firestore
     */
    public CompletableFuture<Boolean> syncImpuestos(List<Map<String, Object>> impuestos) {
        return syncCollection("impuestos", impuestos);
    }
    
    /**
     * Sebastian - Sincroniza configuraciones a Firestore
     */
    public CompletableFuture<Boolean> syncConfiguraciones(List<Map<String, Object>> configuraciones) {
        return syncCollection("configuraciones", configuraciones);
    }
    
    /**
     * Sebastian - Sincroniza inventario a Firestore
     */
    public CompletableFuture<Boolean> syncInventario(List<Map<String, Object>> inventario) {
        return syncCollection("inventario", inventario);
    }
    
    /**
     * Método genérico para sincronizar una colección
     */
    private CompletableFuture<Boolean> syncCollection(String collectionName, List<Map<String, Object>> documents) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!initialized) {
                    LOGGER.warning("Firebase no está inicializado");
                    return false;
                }
                
                if (!hasInternetConnection()) {
                    LOGGER.warning("No hay conexión a internet para sincronizar " + collectionName);
                    return false;
                }
                
                LOGGER.info("Iniciando sincronización de " + collectionName + " - " + documents.size() + " documentos");
                
                int sincronizados = 0;
                int errores = 0;
                
                for (Map<String, Object> document : documents) {
                    try {
                        // Agregar metadatos de sincronización
                        Map<String, Object> docWithMetadata = new HashMap<>(document);
                        docWithMetadata.put("fechaSincronizacion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        docWithMetadata.put("origen", "kriolos-pos");
                        docWithMetadata.put("version", "1.0");
                        
                        // Generar ID único para el documento
                        String documentId = generateDocumentId(document);
                        
                        if (uploadDocument(collectionName, documentId, docWithMetadata)) {
                            sincronizados++;
                        } else {
                            errores++;
                        }
                        
                        // Pausa pequeña para no sobrecargar la API
                        Thread.sleep(100);
                        
                    } catch (Exception e) {
                        errores++;
                        LOGGER.log(Level.WARNING, "Error sincronizando documento de " + collectionName, e);
                    }
                }
                
                LOGGER.info("Sincronización de " + collectionName + " completada - " + 
                           sincronizados + " exitosos, " + errores + " errores");
                
                return errores == 0;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error en sincronización de " + collectionName, e);
                return false;
            }
        });
    }
    
    /**
     * Sube un documento a Firestore
     */
    private boolean uploadDocument(String collection, String documentId, Map<String, Object> data) {
        try {
            String url = baseUrl + "/" + collection + "/" + documentId;
            
            // Convertir datos al formato de Firestore
            Map<String, Object> firestoreDoc = convertToFirestoreFormat(data);
            
            String jsonBody = gson.toJson(Map.of("fields", firestoreDoc));
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                LOGGER.fine("Documento subido: " + collection + "/" + documentId);
                return true;
            } else {
                LOGGER.warning("Error subiendo documento " + collection + "/" + documentId + 
                              " - Status: " + response.statusCode() + 
                              " - Response: " + response.body());
                return false;
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error subiendo documento " + collection + "/" + documentId, e);
            return false;
        }
    }
    
    /**
     * Convierte datos Java al formato de Firestore
     */
    private Map<String, Object> convertToFirestoreFormat(Map<String, Object> data) {
        Map<String, Object> firestoreData = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            try {
                if (value == null) {
                    // Para Firestore REST API, usar null value específico
                    firestoreData.put(key, Map.of("nullValue", (Object) null));
                } else if (value instanceof String) {
                    String stringValue = (String) value;
                    // Asegurar que string no esté vacío para evitar problemas
                    firestoreData.put(key, Map.of("stringValue", stringValue.isEmpty() ? " " : stringValue));
                } else if (value instanceof Integer) {
                    firestoreData.put(key, Map.of("integerValue", value.toString()));
                } else if (value instanceof Long) {
                    firestoreData.put(key, Map.of("integerValue", value.toString()));
                } else if (value instanceof Double || value instanceof Float) {
                    firestoreData.put(key, Map.of("doubleValue", value.toString()));
                } else if (value instanceof Boolean) {
                    firestoreData.put(key, Map.of("booleanValue", value));
                } else if (value instanceof java.sql.Timestamp) {
                    // Convertir Timestamp a formato RFC 3339 requerido por Firestore
                    java.sql.Timestamp timestamp = (java.sql.Timestamp) value;
                    Instant instant = timestamp.toInstant();
                    String rfc3339 = instant.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
                    firestoreData.put(key, Map.of("timestampValue", rfc3339));
                } else if (value instanceof java.util.Date) {
                    // Convertir Date a formato RFC 3339 requerido por Firestore
                    java.util.Date date = (java.util.Date) value;
                    Instant instant = date.toInstant();
                    String rfc3339 = instant.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
                    firestoreData.put(key, Map.of("timestampValue", rfc3339));
            } else if (value instanceof List) {
                // Convertir List a array de Firestore
                List<?> list = (List<?>) value;
                List<Map<String, Object>> arrayValues = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map) {
                        // Si el item es un Map, convertirlo recursivamente
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        arrayValues.add(Map.of("mapValue", Map.of("fields", convertToFirestoreFormat(itemMap))));
                    } else {
                        // Si es un valor simple, convertirlo directamente
                        Map<String, Object> convertedItem = convertToFirestoreFormat(Map.of("value", item));
                        Object convertedValue = convertedItem.get("value");
                        if (convertedValue instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> valueMap = (Map<String, Object>) convertedValue;
                            arrayValues.add(valueMap);
                        } else {
                            // Si no es un Map, crear uno simple con stringValue
                            arrayValues.add(Map.of("stringValue", item.toString()));
                        }
                    }
                }
                firestoreData.put(key, Map.of("arrayValue", Map.of("values", arrayValues)));
            } else if (value instanceof byte[]) {
                // Convertir byte array a string base64 o simplemente indicar que existe
                byte[] byteArray = (byte[]) value;
                String stringValue = byteArray.length > 0 ? "BLOB_DATA_" + byteArray.length + "_bytes" : "EMPTY_BLOB";
                firestoreData.put(key, Map.of("stringValue", stringValue));
            } else {
                // Para otros tipos, convertir a string de forma segura
                String stringValue = value != null ? value.toString() : " ";
                // Asegurar que no esté vacío
                if (stringValue.trim().isEmpty()) {
                    stringValue = "EMPTY_VALUE";
                }
                firestoreData.put(key, Map.of("stringValue", stringValue));
            }
            } catch (Exception e) {
                // En caso de error, convertir a string seguro
                LOGGER.warning("Error convirtiendo campo '" + key + "' de tipo " + 
                             (value != null ? value.getClass().getName() : "null") + 
                             " - usando string por defecto. Error: " + e.getMessage());
                firestoreData.put(key, Map.of("stringValue", "ERROR_CONVERTING_" + 
                                (value != null ? value.getClass().getSimpleName() : "NULL")));
            }
        }
        
        return firestoreData;
    }
    
    /**
     * Genera un ID único para el documento
     */
    private String generateDocumentId(Map<String, Object> document) {
        // Usar ID existente si está disponible, sino generar uno nuevo
        Object id = document.get("id");
        if (id != null) {
            return "kriolos_" + id.toString();
        }
        
        // Generar ID basado en timestamp y hash
        long timestamp = System.currentTimeMillis();
        int hash = document.hashCode();
        return "kriolos_" + timestamp + "_" + Math.abs(hash);
    }
    
    /**
     * Verifica si hay conexión a internet
     */
    private boolean hasInternetConnection() {
        try {
            InetAddress address = InetAddress.getByName("8.8.8.8");
            return address.isReachable(5000);
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Verifica si el servicio está inicializado
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Obtiene el Project ID configurado
     */
    public String getProjectId() {
        return projectId;
    }
    
    // ===== MÉTODOS DE DESCARGA DESDE FIREBASE =====
    
    /**
     * Descarga usuarios desde Firebase
     */
    public CompletableFuture<List<Map<String, Object>>> downloadUsuarios() {
        return downloadCollection("usuarios");
    }
    
    /**
     * Descarga clientes desde Firebase
     */
    public CompletableFuture<List<Map<String, Object>>> downloadClientes() {
        return downloadCollection("clientes");
    }
    
    /**
     * Descarga categorías desde Firebase
     */
    public CompletableFuture<List<Map<String, Object>>> downloadCategorias() {
        return downloadCollection("categorias");
    }
    
    /**
     * Descarga productos desde Firebase
     */
    public CompletableFuture<List<Map<String, Object>>> downloadProductos() {
        return downloadCollection("productos");
    }
    
    /**
     * Descarga ventas desde Firebase
     */
    public CompletableFuture<List<Map<String, Object>>> downloadVentas() {
        return downloadCollection("ventas");
    }
    
    /**
     * Descarga puntos de clientes desde Firebase
     */
    public CompletableFuture<List<Map<String, Object>>> downloadPuntosClientes() {
        return downloadCollection("puntos_clientes");
    }
    
    /**
     * Descarga cierres de caja desde Firebase
     */
    public CompletableFuture<List<Map<String, Object>>> downloadCierresCaja() {
        return downloadCollection("cierres_caja");
    }
    
    /**
     * Descarga formas de pago desde Firebase
     */
    public CompletableFuture<List<Map<String, Object>>> downloadFormasPago() {
        return downloadCollection("formas_pago");
    }
    
    /**
     * Descarga impuestos desde Firebase
     */
    public CompletableFuture<List<Map<String, Object>>> downloadImpuestos() {
        return downloadCollection("impuestos");
    }
    
    /**
     * Descarga configuraciones desde Firebase
     */
    public CompletableFuture<List<Map<String, Object>>> downloadConfiguraciones() {
        return downloadCollection("configuraciones");
    }
    
    /**
     * Descarga inventario desde Firebase
     */
    public CompletableFuture<List<Map<String, Object>>> downloadInventario() {
        return downloadCollection("inventario");
    }
    
    /**
     * Método genérico para descargar una colección desde Firebase
     */
    private CompletableFuture<List<Map<String, Object>>> downloadCollection(String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) {
                LOGGER.warning("Firebase no está inicializado");
                return new ArrayList<>();
            }
            
            List<Map<String, Object>> documents = new ArrayList<>();
            
            try {
                String url = baseUrl + "/" + collectionName;
                LOGGER.info("Descargando colección: " + collectionName + " desde " + url);
                
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode == 200) {
                    // Leer la respuesta
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // Parsear la respuesta JSON
                    JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
                    
                    if (jsonResponse.has("documents")) {
                        JsonArray documentsArray = jsonResponse.getAsJsonArray("documents");
                        
                        for (JsonElement element : documentsArray) {
                            JsonObject document = element.getAsJsonObject();
                            Map<String, Object> docData = parseFirebaseDocument(document);
                            if (docData != null) {
                                documents.add(docData);
                            }
                        }
                    }
                    
                    LOGGER.info("Descargados " + documents.size() + " documentos de " + collectionName);
                } else {
                    LOGGER.warning("Error descargando " + collectionName + ": " + responseCode);
                }
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando " + collectionName, e);
            }
            
            return documents;
        });
    }
    
    /**
     * Parsea un documento de Firebase y convierte sus campos al formato local
     */
    private Map<String, Object> parseFirebaseDocument(JsonObject document) {
        try {
            Map<String, Object> data = new HashMap<>();
            
            // Obtener el ID del documento
            String documentName = document.get("name").getAsString();
            String documentId = documentName.substring(documentName.lastIndexOf("/") + 1);
            data.put("id", documentId);
            
            // Parsear los campos del documento
            if (document.has("fields")) {
                JsonObject fields = document.getAsJsonObject("fields");
                
                for (Map.Entry<String, JsonElement> entry : fields.entrySet()) {
                    String fieldName = entry.getKey();
                    JsonElement fieldValue = entry.getValue();
                    
                    Object value = parseFirebaseFieldValue(fieldValue);
                    data.put(fieldName, value);
                }
            }
            
            return data;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parseando documento Firebase", e);
            return null;
        }
    }
    
    /**
     * Parsea el valor de un campo de Firebase según su tipo
     */
    private Object parseFirebaseFieldValue(JsonElement fieldElement) {
        if (fieldElement == null || fieldElement.isJsonNull()) {
            return null;
        }
        
        JsonObject field = fieldElement.getAsJsonObject();
        
        // Determinar el tipo de campo y extraer el valor
        if (field.has("stringValue")) {
            return field.get("stringValue").getAsString();
        } else if (field.has("doubleValue")) {
            return field.get("doubleValue").getAsDouble();
        } else if (field.has("integerValue")) {
            return field.get("integerValue").getAsLong();
        } else if (field.has("booleanValue")) {
            return field.get("booleanValue").getAsBoolean();
        } else if (field.has("timestampValue")) {
            return field.get("timestampValue").getAsString();
        } else if (field.has("nullValue")) {
            return null;
        } else {
            // Para otros tipos, retornar como string
            return field.toString();
        }
    }
}