package com.openbravo.pos.supabase;

import java.util.*;
import java.util.logging.Logger;

public class PeopleServiceSupabase {

    private static final Logger LOGGER = Logger.getLogger(PeopleServiceSupabase.class.getName());
    private final SupabaseServiceREST supabase;

    public PeopleServiceSupabase(String baseUrl, String apiKey) {
        this.supabase = new SupabaseServiceREST(baseUrl, apiKey);
    }

    /**
     * Inserta una nueva persona en la tabla people.
     */
    public boolean insertPerson(String id, String name, String appPassword, String role, boolean visible,
                                String card, String imageBase64, String branchName, String branchAddress) {

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("ID", id);
        record.put("NAME", name);
        record.put("APPPASSWORD", appPassword);
        record.put("ROLE", role);
        record.put("VISIBLE", visible);
        record.put("CARD", card);
        record.put("IMAGE", imageBase64);
        record.put("BRANCH_NAME", branchName);
        record.put("BRANCH_ADDRESS", branchAddress);

        return supabase.syncData("usuarios", Collections.singletonList(record));
    }

    /**
     * Actualiza una persona existente (usa upsert: resolution=merge-duplicates)
     */
    public boolean updatePerson(String id, String name, String appPassword, String role, boolean visible,
                                String card, String imageBase64, String branchName, String branchAddress) {

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("ID", id); // obligatorio para que Supabase pueda hacer merge
        record.put("NAME", name);
        record.put("APPPASSWORD", appPassword);
        record.put("ROLE", role);
        record.put("VISIBLE", visible);
        record.put("CARD", card);
        record.put("IMAGE", imageBase64);
        record.put("BRANCH_NAME", branchName);
        record.put("BRANCH_ADDRESS", branchAddress);

        return supabase.syncData("usuarios", Collections.singletonList(record));
    }

    /**
     * Elimina una persona por ID.
     */
    public boolean deletePerson(String id) {
        boolean success = false;
        try {
            String deleteUrl = supabaseBaseUrl() + "/usuarios?ID=eq." + id;
            success = sendDelete(deleteUrl);
        } catch (Exception e) {
            LOGGER.severe("Error eliminando persona con ID " + id + ": " + e.getMessage());
        }
        return success;
    }

    /**
     * Obtiene todas las personas de la tabla.
     */
    public List<Map<String, Object>> fetchPeople() {
        return supabase.fetchData("usuarios");
    }

    // --- Métodos internos de soporte ---

    private boolean sendDelete(String fullUrl) {
        try {
            java.net.URL url = new java.net.URL(fullUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("apikey", getApiKey());
            conn.setRequestProperty("Authorization", "Bearer " + getApiKey());
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(15000);

            int responseCode = conn.getResponseCode();
            conn.disconnect();
            return responseCode >= 200 && responseCode < 300;
        } catch (Exception e) {
            LOGGER.severe("Error en DELETE: " + e.getMessage());
            return false;
        }
    }

    private String supabaseBaseUrl() {
        try {
            java.lang.reflect.Field field = supabase.getClass().getDeclaredField("baseUrl");
            field.setAccessible(true);
            return (String) field.get(supabase);
        } catch (Exception e) {
            return "";
        }
    }

    private String getApiKey() {
        try {
            java.lang.reflect.Field field = supabase.getClass().getDeclaredField("apiKey");
            field.setAccessible(true);
            return (String) field.get(supabase);
        } catch (Exception e) {
            return "";
        }
    }
}
