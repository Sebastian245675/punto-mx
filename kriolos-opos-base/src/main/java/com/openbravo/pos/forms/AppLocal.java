//    Sebastian

package com.openbravo.pos.forms;

import com.openbravo.beans.LocaleResources;

/**
 * @author Sebastian
 */
public class AppLocal {
    

    public static final String APP_NAME = "CONNECTING POS";
    public static final String APP_ID = "sebastian-pos";
    
    /**
     * Obtiene la versión de la aplicación.
     * Intenta usar la versión generada automáticamente, si no está disponible usa la versión por defecto.
     */
    public static String getAppVersion() {
        return APP_VERSION;
    }
    
    /**
     * Versión de la aplicación.
     * Se inicializa estáticamente intentando obtener la versión generada en tiempo de build.
     */
    public static final String APP_VERSION;
    
    static {
        String version = "1.0.0-SNAPSHOT"; // Versión por defecto
        
        try {
            // Intentar obtener la versión desde la clase generada en tiempo de build
            Class<?> versionClass = Class.forName("com.openbravo.pos.generated.Version");
            Object versionObj = versionClass.getMethod("getFullVersion").invoke(null);
            String generatedVersion = versionObj != null ? versionObj.toString() : null;
            
            // Si la versión generada es válida y no es el placeholder, usarla
            if (generatedVersion != null && !generatedVersion.contains("${") && 
                !generatedVersion.equals("0.0.0-BUILD") && !generatedVersion.isEmpty()) {
                version = generatedVersion;
            }
        } catch (Exception e) {
            // Si no se puede cargar la clase generada, usar versión por defecto
            // Esto es normal durante desarrollo o si la clase no está disponible
        }
        
        APP_VERSION = version;
    }
    
    public static String APP_SHORT_DESCRIPTION = "Sistema creado por Sebastian!";

    private static final LocaleResources m_resources;
    
    static {
        m_resources = new LocaleResources();
        m_resources.addBundleName("pos_messages");
        m_resources.addBundleName("erp_messages");
    }
    
    /** Creates a new instance of AppLocal */
    private AppLocal() {
    }
    
    /**
     *
     * @param sKey local values
     * @return string values
     */
    public static String getIntString(String sKey) {
        return m_resources.getString(sKey);
    }
    
    /**
     *
     * @param sKey local values
     * @param sValues string values
     * @return string values
     */
    public static String getIntString(String sKey, Object ... sValues) {
        return m_resources.getString(sKey, sValues);
    }
    
    public static String getLockFileName(){
        return APP_ID+".lock";
    }
    
    public static String getLogFileName(){
        return APP_ID+".log";
    }
}
