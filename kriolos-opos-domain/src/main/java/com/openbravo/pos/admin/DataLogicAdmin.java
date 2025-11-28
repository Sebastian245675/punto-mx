//    KriolOS POS
//    Copyright (c)
//    Modificado para integración directa con Supabase REST API

package com.openbravo.pos.admin;

import com.openbravo.data.loader.*;
import com.openbravo.data.user.DefaultSaveProvider;
import com.openbravo.data.user.SaveProvider;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.BeanFactoryDataSingle;
import com.openbravo.basic.BasicException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;
import java.text.SimpleDateFormat;

public class DataLogicAdmin extends BeanFactoryDataSingle {

    private static final Logger LOGGER = Logger.getLogger(DataLogicAdmin.class.getName());

    // 🔹 URL base de tu Supabase REST (tabla "usuarios") - Valores por defecto para compatibilidad
    private static final String DEFAULT_SUPABASE_URL = "https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1";
    private static final String DEFAULT_SUPABASE_API_KEY = "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n";
    
    /**
     * Obtiene el servicio Supabase usando el singleton o valores por defecto
     */
    private Object getSupabaseService() {
        try {
            // Intentar usar SupabaseServiceManager primero
            Class<?> managerClass = Class.forName("com.openbravo.pos.supabase.SupabaseServiceManager");
            Object manager = managerClass.getMethod("getInstance").invoke(null);
            if (managerClass.getMethod("isInitialized").invoke(manager).equals(Boolean.TRUE)) {
                return managerClass.getMethod("getService").invoke(manager);
            }
        } catch (Exception e) {
            // Si falla, usar valores por defecto
        }
        
        // Fallback: crear instancia directa con valores por defecto
        try {
            Class<?> cls = Class.forName("com.openbravo.pos.supabase.SupabaseServiceREST");
            return cls.getConstructor(String.class, String.class).newInstance(DEFAULT_SUPABASE_URL, DEFAULT_SUPABASE_API_KEY);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo crear SupabaseServiceREST: " + e.getMessage(), e);
        }
    }

    private Session s;
    private TableDefinition<PeopleInfo> m_tpeople;
    private TableDefinition<RoleInfo> m_troles;

    public DataLogicAdmin() {}

    @Override
    public void init(Session s) {
        this.s = s;

        m_tpeople = new TableDefinition(s,
                "people",
                new String[]{"ID", "NAME", "APPPASSWORD", "ROLE", "VISIBLE", "CARD", "IMAGE", "BRANCH_NAME", "BRANCH_ADDRESS", "SECURITY_QUESTION", "SECURITY_ANSWER"},
                new String[]{"ID", AppLocal.getIntString("label.peoplename"), AppLocal.getIntString("label.Password"),
                        AppLocal.getIntString("label.role"), AppLocal.getIntString("label.peoplevisible"),
                        AppLocal.getIntString("label.card"), AppLocal.getIntString("label.peopleimage"),
                        "Sucursal (Nombre)", "Sucursal (Dirección)", "Pregunta de Seguridad", "Respuesta de Seguridad"},
                new Datas[]{Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.BOOLEAN,
                        Datas.STRING, Datas.IMAGE, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING},
                new Formats[]{Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING,
                        Formats.BOOLEAN, Formats.STRING, Formats.NULL, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING},
                new int[]{0}
        );

        m_troles = new TableDefinition(s,
                "roles",
                new String[]{"ID", "NAME", "PERMISSIONS"},
                new String[]{"ID", AppLocal.getIntString("label.name"), "PERMISSIONS"},
                new Datas[]{Datas.STRING, Datas.STRING, Datas.BYTES},
                new Formats[]{Formats.STRING, Formats.STRING, Formats.NULL},
                new int[]{0}
        );
    }

    // Descarga la tabla remota "usuarios" y la fusiona en la local "people"
    private void refreshLocalPeopleFromSupabase() throws BasicException {
        List<Map<String, Object>> users;
        try {
            Object svc = getSupabaseService();
            Object listObj = svc.getClass().getMethod("fetchData", String.class).invoke(svc, "usuarios");
            users = (List<Map<String, Object>>) listObj; // raw cast is fine at runtime
        } catch (Exception e) {
            throw new BasicException("No se pudo invocar SupabaseServiceREST.fetchData: " + e.getMessage());
        }
        if (users == null) return;

        // Preparar sentencias para upsert manual: UPDATE luego INSERT si no afectó filas
        SentenceExec update = new PreparedSentence(
                s,
                "UPDATE people SET NAME=?, APPPASSWORD=?, ROLE=?, VISIBLE=?, CARD=?, IMAGE=?, BRANCH_NAME=?, BRANCH_ADDRESS=?, SECURITY_QUESTION=?, SECURITY_ANSWER=? WHERE ID=?",
                new SerializerWriteBasic(new Datas[]{
                        Datas.STRING, // NAME
                        Datas.STRING, // APPPASSWORD
                        Datas.STRING, // ROLE
                        Datas.BOOLEAN,// VISIBLE
                        Datas.STRING, // CARD
                        Datas.IMAGE,  // IMAGE
                        Datas.STRING, // BRANCH_NAME
                        Datas.STRING, // BRANCH_ADDRESS
                        Datas.STRING, // SECURITY_QUESTION
                        Datas.STRING, // SECURITY_ANSWER
                        Datas.STRING  // ID (WHERE)
                }));

        SentenceExec insert = new PreparedSentence(
                s,
                "INSERT INTO people (ID, NAME, APPPASSWORD, ROLE, VISIBLE, CARD, IMAGE, BRANCH_NAME, BRANCH_ADDRESS, SECURITY_QUESTION, SECURITY_ANSWER) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                new SerializerWriteBasic(new Datas[]{
                        Datas.STRING, // ID
                        Datas.STRING, // NAME
                        Datas.STRING, // APPPASSWORD
                        Datas.STRING, // ROLE
                        Datas.BOOLEAN,// VISIBLE
                        Datas.STRING, // CARD
                        Datas.IMAGE,  // IMAGE
                        Datas.STRING, // BRANCH_NAME
                        Datas.STRING, // BRANCH_ADDRESS
                        Datas.STRING, // SECURITY_QUESTION
                        Datas.STRING  // SECURITY_ANSWER
                }));

        for (Map<String, Object> u : users) {
            // Guardar localmente con ID = tarjeta
            String id = asString(u.get("tarjeta"));
            String name = asString(u.get("nombre"));
            String role = asString(u.get("rol"));
            Boolean visible = asBoolean(u.get("visible"));
            String card = asString(u.get("tarjeta"));
            // No tenemos binario de imagen en Supabase, solo bandera; guardamos null localmente
            Object image = null;
            String branchName = asString(u.get("sucursal_nombre"));
            String branchAddress = asString(u.get("sucursal_direccion"));

            // Password de app: mantener vacío si no viene de remoto
            String appPassword = "";
            String securityQuestion = asString(u.get("security_question"));
            String securityAnswer = asString(u.get("security_answer"));

            int affected = update.exec(new Object[]{
                    name, appPassword, role, visible, card, image, branchName, branchAddress, securityQuestion, securityAnswer, id
            });
            if (affected == 0) {
                insert.exec(new Object[]{
                        id, name, appPassword, role, visible, card, image, branchName, branchAddress, securityQuestion, securityAnswer
                });
            }
        }
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Boolean asBoolean(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean) return (Boolean) o;
        String s = String.valueOf(o);
        return "1".equals(s) || "true".equalsIgnoreCase(s) || "t".equalsIgnoreCase(s);
    }

    public final TableDefinition<PeopleInfo> getTablePeople() {
        return m_tpeople;
    }

    public final TableDefinition<RoleInfo> getTableRoles() {
        return m_troles;
    }
    
    public final Session getSession() {
        return s;
    }

    // --------------------------------------------------------------------
    // 🔹 CRUD directo en Supabase
    // --------------------------------------------------------------------

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String serializeValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean || value instanceof Number) {
            return String.valueOf(value);
        }
        if (value instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            return "\"" + sdf.format((Date) value) + "\"";
        }
        return "\"" + escapeJson(String.valueOf(value)) + "\"";
    }

    private String mapToJson(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : data.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escapeJson(e.getKey())).append('"').append(':')
              .append(serializeValue(e.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }

    private boolean sendToSupabase(String method, String table, Map<String, Object> data, String id) {
        HttpURLConnection conn = null;
        try {
            // Obtener URL y API Key del servicio Supabase
            String baseUrl = DEFAULT_SUPABASE_URL;
            String apiKey = DEFAULT_SUPABASE_API_KEY;
            
            try {
                Class<?> managerClass = Class.forName("com.openbravo.pos.supabase.SupabaseServiceManager");
                Object manager = managerClass.getMethod("getInstance").invoke(null);
                if (managerClass.getMethod("isInitialized").invoke(manager).equals(Boolean.TRUE)) {
                    baseUrl = (String) managerClass.getMethod("getBaseUrl").invoke(manager);
                    // Obtener API key del servicio
                    Object service = managerClass.getMethod("getService").invoke(manager);
                    java.lang.reflect.Field field = service.getClass().getDeclaredField("apiKey");
                    field.setAccessible(true);
                    apiKey = (String) field.get(service);
                }
            } catch (Exception e) {
                // Usar valores por defecto si falla
            }
            
            String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            String targetUrl = base + table;
            if (method.equals("PATCH") || method.equals("DELETE")) {
                targetUrl += "?id=eq." + id;
            }

            URL url = new URL(targetUrl);
            conn = (HttpURLConnection) url.openConnection();
            String actualMethod = method;
            if ("PATCH".equals(method)) {
                // HttpURLConnection may not support PATCH; override via POST
                actualMethod = "POST";
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            }
            conn.setRequestMethod(actualMethod);
            conn.setRequestProperty("apikey", apiKey);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=minimal");
            boolean willSendBody = data != null && !data.isEmpty();
            conn.setDoOutput(willSendBody);

            if (willSendBody) {
                String json = mapToJson(data);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes());
                    os.flush();
                }
            }

            int response = conn.getResponseCode();
            boolean success = response >= 200 && response < 300;
            if (!success) {
                LOGGER.warning("❌ Supabase error (" + response + "): " + table + " -> " + method);
            } else {
                LOGGER.info("✅ Supabase " + method + " OK (" + table + ")");
                // Sincronizar tabla local tras éxito
                if ("usuarios".equals(table)) {
                    try {
                        refreshLocalPeopleFromSupabase();
                    } catch (Exception ex) {
                        LOGGER.warning("Sincronización local falló tras operación remota: " + ex.getMessage());
                    }
                }
            }
            return success;

        } catch (Exception e) {
            LOGGER.severe("⚠️ Error comunicando con Supabase: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // --------------------------------------------------------------------
    // 🔹 Métodos SentenceExec adaptados
    // --------------------------------------------------------------------

    private SentenceExec peopleSentenceExecInsert() {
        return new SentenceExec() {
            public int exec() throws BasicException { return exec(null); }
            public int exec(Object params) throws BasicException {
                Object[] v = (Object[]) params;
                
                // PRIMERO: Guardar en la base de datos local
                SentenceExec localInsert = new PreparedSentence(s,
                    "INSERT INTO people (ID, NAME, APPPASSWORD, ROLE, VISIBLE, CARD, IMAGE, BRANCH_NAME, BRANCH_ADDRESS, SECURITY_QUESTION, SECURITY_ANSWER) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    new SerializerWriteBasic(new Datas[]{
                        Datas.STRING, // ID
                        Datas.STRING, // NAME
                        Datas.STRING, // APPPASSWORD
                        Datas.STRING, // ROLE
                        Datas.BOOLEAN,// VISIBLE
                        Datas.STRING, // CARD
                        Datas.IMAGE,  // IMAGE
                        Datas.STRING, // BRANCH_NAME
                        Datas.STRING, // BRANCH_ADDRESS
                        Datas.STRING, // SECURITY_QUESTION
                        Datas.STRING  // SECURITY_ANSWER
                    }));
                
                int result = localInsert.exec(params);
                
                // SEGUNDO: Sincronizar con Supabase (en segundo plano, no fallar si hay error)
                try {
                    Map<String, Object> data = new LinkedHashMap<>();
                    Object cardValForId = v[5];
                    String idFromCard = cardValForId == null ? String.valueOf(v[0]) : String.valueOf(cardValForId);
                    data.put("id", idFromCard);
                    data.put("nombre", v[1]); // NAME
                    data.put("tarjeta", v[5]); // CARD
                    data.put("rol", v[3]); // ROLE
                    data.put("visible", v[4]); // VISIBLE
                    data.put("tieneimagen", v[6] != null); // IMAGE -> boolean
                    data.put("fechaextraccion", new Date()); // timestamp actual
                    data.put("tabla", "people");
                    data.put("sucursal_nombre", v[7]); // BRANCH_NAME
                    data.put("sucursal_direccion", v[8]); // BRANCH_ADDRESS
                    sendToSupabase("POST", "usuarios", data, null);
                } catch (Exception ex) {
                    System.err.println("Error al sincronizar con Supabase (INSERT people): " + ex.getMessage());
                    // No fallar la operación local
                }
                
                return result;
            }
        };
    }

    private SentenceExec peopleSentenceExecUpdate() {
        return new SentenceExec() {
            public int exec() throws BasicException { return exec(null); }
            public int exec(Object params) throws BasicException {
                Object[] v = (Object[]) params;
                
                // PRIMERO: Actualizar en la base de datos local
                SentenceExec localUpdate = new PreparedSentence(s,
                    "UPDATE people SET NAME=?, APPPASSWORD=?, ROLE=?, VISIBLE=?, CARD=?, IMAGE=?, BRANCH_NAME=?, BRANCH_ADDRESS=?, SECURITY_QUESTION=?, SECURITY_ANSWER=? WHERE ID=?",
                    new SerializerWriteBasic(new Datas[]{
                        Datas.STRING, // NAME
                        Datas.STRING, // APPPASSWORD
                        Datas.STRING, // ROLE
                        Datas.BOOLEAN,// VISIBLE
                        Datas.STRING, // CARD
                        Datas.IMAGE,  // IMAGE
                        Datas.STRING, // BRANCH_NAME
                        Datas.STRING, // BRANCH_ADDRESS
                        Datas.STRING, // SECURITY_QUESTION
                        Datas.STRING, // SECURITY_ANSWER
                        Datas.STRING  // ID (WHERE)
                    }));
                
                int result = localUpdate.exec(params);
                
                // SEGUNDO: Sincronizar con Supabase (en segundo plano, no fallar si hay error)
                try {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("nombre", v[1]); // NAME
                    data.put("tarjeta", v[5]); // CARD
                    data.put("rol", v[3]); // ROLE
                    data.put("visible", v[4]); // VISIBLE
                    data.put("tieneimagen", v[6] != null); // IMAGE -> boolean
                    data.put("fechaextraccion", new Date()); // actualizar extracción
                    data.put("tabla", "people");
                    data.put("sucursal_nombre", v[7]);
                    data.put("sucursal_direccion", v[8]);
                    String idOld = String.valueOf(v[0]);
                    String idNew = v[5] != null ? String.valueOf(v[5]) : idOld;
                    data.put("id", idNew);
                    sendToSupabase("PATCH", "usuarios", data, idOld);
                } catch (Exception ex) {
                    System.err.println("Error al sincronizar con Supabase (UPDATE people): " + ex.getMessage());
                    // No fallar la operación local
                }
                
                return result;
            }
        };
    }

    private SentenceExec peopleSentenceExecDelete() {
        return new SentenceExec() {
            public int exec() throws BasicException { return exec(null); }
            public int exec(Object params) throws BasicException {
                Object[] v = (Object[]) params;
                String id = String.valueOf(v[0]);
                
                // PRIMERO: Eliminar de la base de datos local
                SentenceExec localDelete = new PreparedSentence(s,
                    "DELETE FROM people WHERE ID=?",
                    new SerializerWriteBasic(new Datas[]{Datas.STRING}));
                
                int result = localDelete.exec(params);
                
                // SEGUNDO: Sincronizar con Supabase (en segundo plano, no fallar si hay error)
                try {
                    sendToSupabase("DELETE", "usuarios", null, id);
                } catch (Exception ex) {
                    System.err.println("Error al sincronizar con Supabase (DELETE people): " + ex.getMessage());
                    // No fallar la operación local
                }
                
                return result;
            }
        };
    }

    // --------------------------------------------------------------------
    // 🔹 Proveedor de guardado para el sistema
    // --------------------------------------------------------------------

    public SaveProvider<Object[]> getPeopleSaveProvider() {
        return new DefaultSaveProvider(
                peopleSentenceExecUpdate(),
                peopleSentenceExecInsert(),
                peopleSentenceExecDelete());
    }

    // --------------------------------------------------------------------
    // 🔹 Listas (las puedes mantener locales o también hacer fetch remoto)
    // --------------------------------------------------------------------

    public final SentenceList<PeopleInfo> getPeopleList() {
        return new StaticSentence(s,
                "SELECT ID, NAME FROM people ORDER BY NAME",
                null,
                new SerializerReadClass(PeopleInfo.class));
    }

    public final SentenceList<RoleInfo> getRolesList() {
        return new StaticSentence(s,
                "SELECT ID, NAME FROM roles ORDER BY NAME",
                null,
                new SerializerReadClass(RoleInfo.class));
    }

    public final SentenceList<ResourceInfo> getResourceList() {
        return new StaticSentence(s,
                "SELECT ID, NAME, RESTYPE, CONTENT FROM resources ORDER BY NAME",
                null,
                new SerializerReadClass(ResourceInfo.class));
    }
}
