package com.openbravo.pos.supabase;

import com.openbravo.pos.forms.AppProperties;
import com.openbravo.pos.forms.AppUserView;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio singleton para gestión de conexión con Supabase
 * Centraliza la inicialización y configuración de Supabase
 */
public class SupabaseServiceManager {
    
    private static final Logger LOGGER = Logger.getLogger(SupabaseServiceManager.class.getName());
    private static SupabaseServiceManager instance;
    
    private AppProperties config;
    private AppUserView appUserView;
    private String baseUrl;
    private String apiKey;
    private boolean initialized = false;
    private SupabaseServiceREST supabaseService;
    
    // Credenciales internas de Supabase - siempre conectado
    private static final String INTERNAL_SUPABASE_URL = "https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1";
    private static final String INTERNAL_SUPABASE_API_KEY = "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n";
    
    private SupabaseServiceManager() {
    }
    
    public static synchronized SupabaseServiceManager getInstance() {
        if (instance == null) {
            instance = new SupabaseServiceManager();
        }
        return instance;
    }
    
    /**
     * Inicializa el servicio Supabase con la configuración (sin AppUserView)
     * Mantiene compatibilidad con código existente
     */
    public boolean initialize(AppProperties config) {
        return initialize(config, null);
    }
    
    /**
     * Inicializa el servicio Supabase con la configuración y el usuario actual
     */
    public boolean initialize(AppProperties config, AppUserView appUserView) {
        this.config = config;
        this.appUserView = appUserView;
        
        if (initialized && supabaseService != null) {
            LOGGER.info("Supabase Service ya está inicializado");
            return true;
        }
        
        try {
            // Siempre usar credenciales internas - conexión automática
            baseUrl = INTERNAL_SUPABASE_URL;
            apiKey = INTERNAL_SUPABASE_API_KEY;
            
            // Crear instancia de SupabaseServiceREST con credenciales internas
            supabaseService = new SupabaseServiceREST(baseUrl, apiKey);
            
            initialized = true;
            LOGGER.info("Supabase Service inicializado automáticamente con credenciales internas. URL: " + baseUrl);
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar Supabase Service", e);
            initialized = false;
            return false;
        }
    }
    
    /**
     * Obtiene la instancia de SupabaseServiceREST
     * Debe estar inicializado previamente
     */
    public SupabaseServiceREST getService() {
        if (!initialized || supabaseService == null) {
            throw new IllegalStateException("Supabase Service no está inicializado. Llama a initialize() primero.");
        }
        return supabaseService;
    }
    
    /**
     * Verifica si el servicio está inicializado
     */
    public boolean isInitialized() {
        return initialized && supabaseService != null;
    }
    
    /**
     * Obtiene la URL base configurada
     */
    public String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * Verifica si hay conexión a internet
     */
    public boolean hasInternetConnection() {
        try {
            java.net.InetAddress address = java.net.InetAddress.getByName("8.8.8.8");
            return address.isReachable(5000);
        } catch (java.io.IOException e) {
            return false;
        }
    }
}

