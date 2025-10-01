//    Sebastian

package com.openbravo.pos.forms;

import com.openbravo.beans.LocaleResources;

/**
 * @author Sebastian
 */
public class AppLocal {
    

    public static final String APP_NAME = "Sebastian POS";
    public static final String APP_ID = "sebastian-pos";
    public static final String APP_VERSION = "1.0.0-Sebastian";
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
