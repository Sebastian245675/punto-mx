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
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package com.openbravo.pos.forms;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.*;
import com.openbravo.format.Formats;
import com.openbravo.pos.admin.ResourceInfo;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppUser;
import com.openbravo.pos.forms.BeanFactoryDataSingle;
import com.openbravo.pos.util.ThumbNailBuilder;
import com.openbravo.pos.voucher.VoucherInfo;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 *
 * @author JG uniCenta
 */
public class DataLogicSystem extends BeanFactoryDataSingle {

    private final static Logger LOGGER = Logger.getLogger(ImageUtils.class.getName());

    private Session session;
    private String m_dbVersion;
    private TableDefinition<ResourceInfo> m_tresources;

    public DataLogicSystem() {
    }

    @Override
    public void init(Session session) {
        this.session = session;
        this.m_dbVersion = this.session.DB.getName();

        m_tresources = new TableDefinition(session,
                "resources",
                new String[] {
                        "ID", "NAME", "RESTYPE", "CONTENT" },
                new String[] {
                        "ID",
                        AppLocal.getIntString("label.name"),
                        AppLocal.getIntString("label.type"),
                        "CONTENT" },
                new Datas[] {
                        Datas.STRING, Datas.STRING, Datas.INT, Datas.BYTES },
                new Formats[] {
                        Formats.STRING, Formats.STRING, Formats.INT, Formats.NULL },
                new int[] { 0 });

        //// <editor-fold defaultstate="collapsed" desc="START OF PRODUCT">
        // </editor-fold>
        //// <editor-fold defaultstate="collapsed" desc="START OF CUSTOMER">
        //// </editor-fold>

        //// <editor-fold defaultstate="collapsed" desc="START OF CASH">
        //// </editor-fold>
        //// <editor-fold defaultstate="collapsed" desc="START OF LOCATION AND PLACES">
        //// </editor-fold>

    }

    public final TableDefinition<ResourceInfo> getTableResources() {
        return m_tresources;
    }

    public String getDBVersion() {
        return m_dbVersion;
    }

    public final String findVersion() throws BasicException {
        final SentenceFind<String> m_version = new PreparedSentence<String, String>(this.session,
                "SELECT VERSION FROM applications WHERE ID = ?",
                SerializerWriteString.INSTANCE, SerializerReadString.INSTANCE);

        return m_version.find(AppLocal.APP_ID);
    }

    public final String getUser() throws BasicException {
        return ("");
    }

    public final void execDummy() throws BasicException {

        SentenceExec m_dummy = new StaticSentence(this.session, "SELECT * FROM people WHERE 1 = 0");
        m_dummy.exec();
    }

    //// <editor-fold defaultstate="collapsed" desc="START OF PEOPLE">

    /**
     *
     * @return @throws BasicException
     */
    public final List<AppUser> listPeopleVisible() throws BasicException {
        final SentenceList m_peoplevisible = new StaticSentence(this.session,
                "SELECT ID, NAME, APPPASSWORD, CARD, ROLE "
                        + "FROM people "
                        + "WHERE VISIBLE = " + this.session.DB.TRUE() + " ORDER BY NAME",
                new AppuserReader());

        return m_peoplevisible.list();
    }

    /**
     *
     * @param role
     * @return
     * @throws BasicException
     */
    public final List<String> getPermissions(String role) throws BasicException {
        final SentenceList<String> m_permissionlist = new StaticSentence(this.session,
                "SELECT PERMISSIONS FROM permissions WHERE ID = ?",
                SerializerWriteString.INSTANCE,
                new SerializerReadBasic(new Datas[] { Datas.STRING }));

        return m_permissionlist.list(role);
    }

    /**
     *
     * @param card
     * @return
     * @throws BasicException
     */
    public final AppUser findPeopleByCard(String card) throws BasicException {

        final SentenceFind<AppUser> m_peoplebycard = new PreparedSentence<String, AppUser>(this.session,
                "SELECT ID, NAME, APPPASSWORD, CARD, ROLE, IMAGE "
                        + "FROM people "
                        + "WHERE CARD = ? AND VISIBLE = " + this.session.DB.TRUE(),
                SerializerWriteString.INSTANCE,
                new AppuserReader());
        return m_peoplebycard.find(card);
    }

    /**
     * Find user by username (name) - case insensitive
     * 
     * @param username
     * @return
     * @throws BasicException
     */
    public final AppUser findPeopleByName(String username) throws BasicException {
        final SentenceFind<AppUser> m_peoplebyname = new PreparedSentence<String, AppUser>(this.session,
                "SELECT ID, NAME, APPPASSWORD, CARD, ROLE, IMAGE "
                        + "FROM people "
                        + "WHERE UPPER(NAME) = UPPER(?) AND VISIBLE = " + this.session.DB.TRUE(),
                SerializerWriteString.INSTANCE,
                new AppuserReader());
        return m_peoplebyname.find(username);
    }

    /**
     * Authenticates a password against any user with the ADMIN role
     *
     * @param password The password to check
     * @return true if matches any admin
     * @throws BasicException
     */
    public final boolean authenticateAdmin(String password) throws BasicException {
        // En este sistema, el rol '1' es ADMIN según DefaultRolesInitializer
        final SentenceList<AppUser> m_admins = new StaticSentence(this.session,
                "SELECT ID, NAME, APPPASSWORD, CARD, ROLE, IMAGE "
                        + "FROM people "
                        + "WHERE ROLE = '1' AND VISIBLE = " + this.session.DB.TRUE(),
                new AppuserReader());

        List<AppUser> admins = m_admins.list();
        for (AppUser admin : admins) {
            if (admin.authenticate(password)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find role permissions by role name
     * 
     * @param sRole
     * @return
     */
    public final String findRolePermissions(String sRole) {
        String content = new String();
        try {
            // 1. Buscar por ID del rol (los roles custom se guardan por ID en people.ROLE)
            final SentenceFind m_rolepermissionsById = new PreparedSentence(this.session,
                    "SELECT permissions FROM roles WHERE ID = ?",
                    SerializerWriteString.INSTANCE,
                    SerializerReadBytes.INSTANCE);

            byte[] permissionsBytes = (byte[]) m_rolepermissionsById.find(sRole);
            if (permissionsBytes != null && permissionsBytes.length > 0) {
                content = Formats.BYTEA.formatValue(permissionsBytes);
            } else {
                // 2. Fallback: buscar por nombre exacto
                final SentenceFind m_rolepermissionsByName = new PreparedSentence(this.session,
                        "SELECT permissions FROM roles WHERE name = ?",
                        SerializerWriteString.INSTANCE,
                        SerializerReadBytes.INSTANCE);

                byte[] permByName = (byte[]) m_rolepermissionsByName.find(sRole);
                if (permByName != null && permByName.length > 0) {
                    content = Formats.BYTEA.formatValue(permByName);
                } else {
                    // 3. Fallback: buscar por nombre case-insensitive
                    final SentenceFind m_rolepermissions_ci = new PreparedSentence(this.session,
                            "SELECT permissions FROM roles WHERE UPPER(name) = UPPER(?)",
                            SerializerWriteString.INSTANCE,
                            SerializerReadBytes.INSTANCE);

                    byte[] permByNameCI = (byte[]) m_rolepermissions_ci.find(sRole);
                    if (permByNameCI != null && permByNameCI.length > 0) {
                        content = Formats.BYTEA.formatValue(permByNameCI);
                    } else {
                        System.err.println("No se encontraron permisos para el rol: " + sRole);
                    }
                }
            }
        } catch (BasicException e) {
            LOGGER.log(Level.SEVERE, "Exception on format permissions for role: " + sRole, e);
        }
        return content;
    }

    /**
     *
     * @param userdata
     * @throws BasicException
     */
    public final void execChangePassword(Object[] userdata) throws BasicException {

        final SentenceExec m_changepassword = new StaticSentence(this.session,
                "UPDATE people SET APPPASSWORD = ? WHERE ID = ?",
                new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING }));

        m_changepassword.exec(userdata);
    }

    /**
     * Obtiene la pregunta de seguridad de un usuario
     * 
     * @param userId ID del usuario
     * @return Pregunta de seguridad o null si no existe
     * @throws BasicException
     */
    public final String getSecurityQuestion(String userId) throws BasicException {
        final SentenceFind m_securityquestion = new PreparedSentence(this.session,
                "SELECT SECURITY_QUESTION FROM people WHERE ID = ?",
                SerializerWriteString.INSTANCE,
                SerializerReadString.INSTANCE);

        return (String) m_securityquestion.find(userId);
    }

    /**
     * Obtiene la respuesta de seguridad de un usuario
     * 
     * @param userId ID del usuario
     * @return Respuesta de seguridad o null si no existe
     * @throws BasicException
     */
    public final String getSecurityAnswer(String userId) throws BasicException {
        final SentenceFind m_securityanswer = new PreparedSentence(this.session,
                "SELECT SECURITY_ANSWER FROM people WHERE ID = ?",
                SerializerWriteString.INSTANCE,
                SerializerReadString.INSTANCE);

        return (String) m_securityanswer.find(userId);
    }

    /**
     *
     * @param permissions
     * @throws BasicException
     */
    public final void execUpdatePermissions(Object[] permissions) throws BasicException {
        final SentenceExec m_updatepermissions = new StaticSentence(this.session,
                "INSERT INTO permissions (ID, PERMISSIONS) "
                        + "VALUES (?, ?)",
                new SerializerWriteBasic(new Datas[] {
                        Datas.STRING,
                        Datas.STRING }));
        m_updatepermissions.exec(permissions);
    }

    //// </editor-fold>

    //// <editor-fold defaultstate="collapsed" desc="START OF RESOURCE">
    private byte[] getResource(String name) {

        SentenceFind m_resourcebytes = new PreparedSentence(this.session,
                "SELECT CONTENT FROM resources WHERE NAME = ?",
                SerializerWriteString.INSTANCE,
                SerializerReadBytes.INSTANCE);

        byte[] resource = null;

        try {
            resource = (byte[]) m_resourcebytes.find(name);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception while get resource name: " + name, e);
        }

        if (resource == null || resource.length == 0) {
            LOGGER.log(Level.WARNING, "Resource NOT found in DB name: {0}, intentando cargar desde classpath...", name);
            try (InputStream is = getClass().getResourceAsStream("/com/openbravo/pos/templates/" + name + (name.endsWith(".xml") ? "" : ".xml"))) {
                if (is != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    resource = baos.toByteArray();
                }
            } catch (Exception exClasspath) {
                LOGGER.log(Level.WARNING, "Tampoco se pudo cargar desde classpath: " + name);
            }
        }

        return resource;
    }

    /**
     *
     * @param name
     * @param type
     * @param data
     */
    public final void setResource(String name, int type, byte[] data) {
        if (data == null) {
            data = new byte[0];
        }
        try {
            java.sql.Connection conn = this.session.getConnection();
            try (java.sql.PreparedStatement updateStmt = conn.prepareStatement("UPDATE resources SET RESTYPE = ?, CONTENT = ? WHERE NAME = ?");
                 java.sql.PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO resources(ID, NAME, RESTYPE, CONTENT) VALUES (?, ?, ?, ?)")) {

                updateStmt.setInt(1, type);
                updateStmt.setBytes(2, data);
                updateStmt.setString(3, name);
                int updated = updateStmt.executeUpdate();

                if (updated == 0) {
                    insertStmt.setString(1, UUID.randomUUID().toString());
                    insertStmt.setString(2, name);
                    insertStmt.setInt(3, type);
                    insertStmt.setBytes(4, data);
                    insertStmt.executeUpdate();
                    LOGGER.log(Level.INFO, "Recurso insertado en BD: {0}", name);
                } else {
                    LOGGER.log(Level.INFO, "Recurso actualizado en BD: {0}", name);
                }

                if (!conn.getAutoCommit()) {
                    conn.commit();
                }
            }
        } catch (java.sql.SQLException ex) {
            LOGGER.log(Level.SEVERE, "Exception al guardar recurso: " + name, ex);
        }
    }

    /**
     *
     * @param sName
     * @param data
     */
    public final void setResourceAsBinary(String sName, byte[] data) {
        setResource(sName, 2, data);
    }

    /**
     *
     * @param sName
     * @return
     */
    public final byte[] getResourceAsBinary(String sName) {
        return getResource(sName);
    }

    /**
     *
     * @param sName
     * @return
     */
    public final String getResourceAsText(String sName) {
        return Formats.BYTEA.formatValue(getResource(sName));
    }

    /**
     *
     * @param sName
     * @return
     */
    public final String getResourceAsXML(String sName) {
        return Formats.BYTEA.formatValue(getResource(sName));
    }

    /**
     *
     * @param sName
     * @return
     */
    public final BufferedImage getResourceAsImage(String sName) {

        LOGGER.log(Level.INFO, "Get image resource id: " + sName);
        BufferedImage img = null;
        try {
            InputStream strem = new ByteArrayInputStream(getResource(sName));
            img = ImageIO.read(strem);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception on get resource: " + sName, e);
        }
        return img;
    }

    /**
     *
     * @param sName
     * @param p
     */
    public final void setResourceAsProperties(String sName, Properties p) {
        if (p == null) {
            setResource(sName, 0, null); // texto
        } else {
            try {
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                p.storeToXML(o, AppLocal.APP_NAME, "UTF8");
                setResource(sName, 0, o.toByteArray()); // El texto de las propiedades
            } catch (IOException e) { // no deberia pasar nunca
                LOGGER.log(Level.SEVERE, "Exception on set resource: " + sName, e);
            }
        }
    }

    /**
     *
     * @param sName
     * @return
     */
    public final Properties getResourceAsProperties(String sName) {

        Properties p = new Properties();
        try {
            byte[] xml = getResource(sName);
            if (xml != null) {
                p.loadFromXML(new ByteArrayInputStream(xml));
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception on get resource as Properties, name: " + sName, e);
        }
        return p;
    }

    //// </editor-fold>
    //// <editor-fold defaultstate="collapsed" desc="START OF CASH">
    /**
     *
     * @param host
     * @return
     * @throws BasicException
     */
    public final int getSequenceCash(String host) throws BasicException {
        final SentenceFind m_sequencecash = new PreparedSentence(this.session,
                "SELECT MAX(HOSTSEQUENCE) FROM closedcash WHERE HOST = ?",
                SerializerWriteString.INSTANCE,
                SerializerReadInteger.INSTANCE);

        Integer i = (Integer) m_sequencecash.find(host);
        return (i == null) ? 1 : i;
    }

    /**
     *
     * @param sActiveCashIndex
     * @return
     * @throws BasicException
     */
    public final Object[] findActiveCash(String sActiveCashIndex) throws BasicException {
        LOGGER.info("🔍 Sebastian - Buscando caja activa: " + sActiveCashIndex);

        // Sebastian - Intentar primero con fondo inicial
        try {
            final SentenceFind m_activecash = new PreparedSentence(this.session,
                    "SELECT HOST, HOSTSEQUENCE, DATESTART, DATEEND, NOSALES, initial_amount "
                            + "FROM closedcash WHERE MONEY = ?",
                    SerializerWriteString.INSTANCE,
                    new SerializerReadBasic(new Datas[] {
                            Datas.STRING,
                            Datas.INT,
                            Datas.TIMESTAMP,
                            Datas.TIMESTAMP,
                            Datas.INT,
                            Datas.DOUBLE }));

            Object[] result = (Object[]) m_activecash.find(sActiveCashIndex);

            if (result != null) {
                LOGGER.info("📋 Sebastian - Resultado BD completo:");
                for (int i = 0; i < result.length; i++) {
                    LOGGER.info("   [" + i + "] = " + result[i] + " ("
                            + (result[i] != null ? result[i].getClass().getSimpleName() : "null") + ")");
                }

                // Verificación especial del campo initial_amount
                if (result.length > 5 && result[5] != null) {
                    LOGGER.info("💰 Sebastian - initial_amount desde BD: $" + result[5]);
                } else {
                    LOGGER.warning("⚠️ Sebastian - initial_amount es null o no existe en posición [5]");
                }
            } else {
                LOGGER.warning("❌ Sebastian - No se encontró caja con MONEY = " + sActiveCashIndex);
            }

            return result;
        } catch (BasicException e) {
            LOGGER.warning("⚠️ Sebastian - Error con consulta extendida, usando fallback: " + e.getMessage());
            // Sebastian - Fallback para compatibilidad con versión anterior
            final SentenceFind m_activecash = new PreparedSentence(this.session,
                    "SELECT HOST, HOSTSEQUENCE, DATESTART, DATEEND, NOSALES "
                            + "FROM closedcash WHERE MONEY = ?",
                    SerializerWriteString.INSTANCE,
                    new SerializerReadBasic(new Datas[] {
                            Datas.STRING,
                            Datas.INT,
                            Datas.TIMESTAMP,
                            Datas.TIMESTAMP,
                            Datas.INT }));

            Object[] result = (Object[]) m_activecash.find(sActiveCashIndex);
            LOGGER.info("📋 Sebastian - Resultado BD fallback (sin initial_amount): longitud = "
                    + (result != null ? result.length : "null"));
            return result;
        }
    }

    /**
     *
     * @param sClosedCashIndex
     * @return
     * @throws BasicException
     */
    public final Object[] findClosedCash(String sClosedCashIndex) throws BasicException {

        final SentenceFind m_closedcash = new StaticSentence(this.session,
                "SELECT HOST, HOSTSEQUENCE, DATESTART, DATEEND, NOSALES "
                        + "FROM closedcash WHERE HOSTSEQUENCE = ?",
                SerializerWriteString.INSTANCE,
                new SerializerReadBasic(new Datas[] {
                        Datas.STRING,
                        Datas.INT,
                        Datas.TIMESTAMP,
                        Datas.TIMESTAMP,
                        Datas.INT }));

        return (Object[]) m_closedcash.find(sClosedCashIndex);
    }

    /**
     *
     * @param cash
     * @throws BasicException
     */
    public final void execInsertCash(Object[] cash) throws BasicException {
        // Sebastian - Compatibilidad con versión anterior (sin fondo inicial)
        if (cash.length == 5) {
            // Versión anterior sin fondo inicial
            final SentenceExec m_insertcash = new StaticSentence(this.session,
                    "INSERT INTO closedcash(MONEY, HOST, HOSTSEQUENCE, DATESTART, DATEEND) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    new SerializerWriteBasic(new Datas[] {
                            Datas.STRING,
                            Datas.STRING,
                            Datas.INT,
                            Datas.TIMESTAMP,
                            Datas.TIMESTAMP }));
            m_insertcash.exec(cash);
        } else if (cash.length == 6) {
            // Nueva versión con fondo inicial
            execInsertCashWithInitialAmount(cash);
        } else {
            throw new BasicException("Invalid cash array length: " + cash.length);
        }
    }

    /**
     * Sebastian - Insertar caja con fondo inicial
     * 
     * @param cash Array con [MONEY, HOST, HOSTSEQUENCE, DATESTART, DATEEND,
     *             INITIAL_AMOUNT]
     * @throws BasicException
     */
    public final void execInsertCashWithInitialAmount(Object[] cash) throws BasicException {
        final SentenceExec m_insertcash = new StaticSentence(this.session,
                "INSERT INTO closedcash(MONEY, HOST, HOSTSEQUENCE, DATESTART, DATEEND, initial_amount) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                new SerializerWriteBasic(new Datas[] {
                        Datas.STRING,
                        Datas.STRING,
                        Datas.INT,
                        Datas.TIMESTAMP,
                        Datas.TIMESTAMP,
                        Datas.DOUBLE }));

        m_insertcash.exec(cash);
    }

    /**
     * Sebastian - Actualizar el monto inicial de una caja específica
     * 
     * @param cashIndex     Índice de la caja
     * @param initialAmount Nuevo monto inicial
     * @throws BasicException
     */
    public final void execUpdateCashInitialAmount(String cashIndex, double initialAmount) throws BasicException {
        try {
            LOGGER.info("� Sebastian - FORZANDO ACTUALIZACIÓN DE MONTO INICIAL...");
            LOGGER.info("� Sebastian - Caja: " + cashIndex + ", Monto: $" + initialAmount);

            // PASO 1: Desactivar auto-commit para control total
            this.session.getConnection().setAutoCommit(false);
            LOGGER.info("🔧 Sebastian - Auto-commit DESACTIVADO");

            // PASO 2: Ejecutar UPDATE con parámetros explícitos
            final SentenceExec m_updatecash = new StaticSentence(this.session,
                    "UPDATE closedcash SET initial_amount = ? WHERE MONEY = ?",
                    new SerializerWriteBasic(new Datas[] {
                            Datas.DOUBLE,
                            Datas.STRING }));

            m_updatecash.exec(new Object[] { initialAmount, cashIndex });
            LOGGER.info("✅ Sebastian - UPDATE closedcash ejecutado correctamente");

            // Verificar inmediatamente qué se guardó
            PreparedSentence<String, Object[]> verifyStmt = new PreparedSentence<>(this.session,
                    "SELECT initial_amount FROM closedcash WHERE MONEY = ?",
                    SerializerWriteString.INSTANCE,
                    new SerializerReadBasic(new Datas[] { Datas.DOUBLE }));

            Object[] result = verifyStmt.find(cashIndex);
            if (result != null && result.length > 0) {
                LOGGER.info("🔍 Sebastian - Verificación inmediata PRE-COMMIT: BD tiene $" + result[0]);
            } else {
                LOGGER.warning("❌ Sebastian - No se pudo verificar el valor guardado PRE-COMMIT");
            }

            // PASO 3: COMMIT inmediato y forzado
            this.session.getConnection().commit();
            LOGGER.info("✅ Sebastian - COMMIT forzado ejecutado");

            // PASO 4: Eliminamos CHECKPOINT (era solo para HSQLDB)
            LOGGER.info("💾 Sebastian - Datos persistidos por COMMIT");

            // PASO 5: VERIFICACIÓN FINAL (sin SHUTDOWN que desconecta la BD)
            Object[] finalResult = verifyStmt.find(cashIndex);
            if (finalResult != null && finalResult.length > 0) {
                double finalAmount = finalResult[0] != null ? ((Number) finalResult[0]).doubleValue() : 0.0;
                LOGGER.info("� Sebastian - Verificación FINAL: BD persiste $" + finalAmount);
            }
            LOGGER.info("💾 Sebastian - Datos persistidos sin desconectar BD");

            // PASO 6: Reactivar auto-commit
            this.session.getConnection().setAutoCommit(true);
            LOGGER.info("🔄 Sebastian - Auto-commit reactivado para operaciones futuras");

            LOGGER.info("🎯 Sebastian - MONTO INICIAL GUARDADO PERMANENTEMENTE CON CHECKPOINT: $" + initialAmount);

        } catch (Exception ex) {
            LOGGER.severe("❌ Sebastian - ERROR CRÍTICO en actualización: " + ex.getMessage());
            ex.printStackTrace();

            try {
                this.session.getConnection().rollback();
                this.session.getConnection().setAutoCommit(true);
                LOGGER.info("🔄 Sebastian - Rollback ejecutado, auto-commit restaurado");
            } catch (Exception rollbackEx) {
                LOGGER.severe("❌ Sebastian - Error en rollback: " + rollbackEx.getMessage());
            }

            throw new BasicException("Error crítico actualizando monto inicial", ex);
        }
    }

    /**
     *
     * @param drawer
     * @throws BasicException
     */
    public final void execDrawerOpened(Object[] drawer) throws BasicException {
        final SentenceExec m_draweropened = new StaticSentence(this.session,
                "INSERT INTO draweropened ( NAME, TICKETID) "
                        + "VALUES (?, ?)",
                new SerializerWriteBasic(new Datas[] {
                        Datas.STRING,
                        Datas.STRING }));
        m_draweropened.exec(drawer);
    }
    //// </editor-fold>

    //// <editor-fold defaultstate="collapsed" desc="START OF LINEREMOVED">

    /**
     *
     * @param line
     */
    public final void execLineRemoved(Object[] line) {

        final SentenceExec m_lineremoved = new StaticSentence(this.session,
                "INSERT INTO lineremoved (NAME, TICKETID, PRODUCTID, PRODUCTNAME, UNITS) "
                        + "VALUES (?, ?, ?, ?, ?)",
                new SerializerWriteBasic(new Datas[] {
                        Datas.STRING, Datas.STRING,
                        Datas.STRING, Datas.STRING,
                        Datas.DOUBLE
                }));

        try {
            m_lineremoved.exec(line);
        } catch (BasicException e) {
            LOGGER.log(Level.SEVERE, "Exception on execute line removed: ", e);
        }
    }

    /**
     *
     * @param ticket
     */
    public final void execTicketRemoved(Object[] ticket) {
        final SentenceExec m_ticketremoved = new StaticSentence(this.session,
                "INSERT INTO lineremoved (NAME, TICKETID, PRODUCTNAME, UNITS) "
                        + "VALUES (?, ?, ?, ?)",
                new SerializerWriteBasic(new Datas[] {
                        Datas.STRING, Datas.STRING,
                        Datas.STRING, Datas.DOUBLE
                }));
        try {
            m_ticketremoved.exec(ticket);
        } catch (BasicException e) {
            LOGGER.log(Level.SEVERE, "Exception on execute ticket removed: ", e);
        }
    }
    //// </editor-fold>

    /**
     *
     * @param iLocation
     * @return
     * @throws BasicException
     */
    public final String findLocationName(String iLocation) throws BasicException {
        final SentenceFind m_locationfind = new StaticSentence(this.session,
                "SELECT NAME FROM locations WHERE ID = ?",
                SerializerWriteString.INSTANCE,
                SerializerReadString.INSTANCE);
        return (String) m_locationfind.find(iLocation);
    }

    //// <editor-fold defaultstate="collapsed" desc="START OF CVIMPORT">

    /**
     *
     * @param csv
     * @throws BasicException
     */
    public final void execCSVStockUpdate(Object[] csv) throws BasicException {
        // Push Product Quantity Update into CSVImport table
        final SentenceExec m_insertStockUpdateCSVEntry = new StaticSentence(this.session,
                "INSERT INTO csvimport ( "
                        + "ID, ROWNUMBER, CSVERROR, REFERENCE, CODE, PRICEBUY ) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                new SerializerWriteBasic(new Datas[] {
                        Datas.STRING,
                        Datas.STRING,
                        Datas.STRING,
                        Datas.STRING,
                        Datas.STRING,
                        Datas.DOUBLE
                }));
        m_insertStockUpdateCSVEntry.exec(csv);

    }

    /**
     *
     * @param csv
     * @throws BasicException
     */
    public final void execAddCSVEntry(Object[] csv) throws BasicException {
        // Push Products into CSVImport table
        final SentenceExec m_insertCSVEntry = new StaticSentence(this.session,
                "INSERT INTO csvimport ( "
                        + "ID, ROWNUMBER, CSVERROR, REFERENCE, "
                        + "CODE, NAME, PRICEBUY, PRICESELL, "
                        + "PREVIOUSBUY, PREVIOUSSELL, CATEGORY, TAX) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new SerializerWriteBasic(new Datas[] {
                        Datas.STRING,
                        Datas.STRING,
                        Datas.STRING,
                        Datas.STRING,
                        Datas.STRING,
                        Datas.STRING,
                        Datas.DOUBLE,
                        Datas.DOUBLE,
                        Datas.DOUBLE,
                        Datas.DOUBLE,
                        Datas.STRING,
                        Datas.STRING
                }));
        m_insertCSVEntry.exec(csv);

    }

    /**
     *
     * @param csv
     * @throws BasicException
     */
    public final void execCustomerAddCSVEntry(Object[] csv) throws BasicException {
        // Push Customers into CSVImport table
        final SentenceExec m_insertCustomerCSVEntry = new StaticSentence(this.session,
                "INSERT INTO csvimport ( "
                        + "ID, ROWNUMBER, CSVERROR, SEARCHKEY, NAME) "
                        + "VALUES (?, ?, ?, ?, ?)",
                new SerializerWriteBasic(new Datas[] {
                        Datas.STRING,
                        Datas.STRING,
                        Datas.STRING,
                        Datas.STRING,
                        Datas.STRING
                }));
        m_insertCustomerCSVEntry.exec(csv);

    }
    //// </editor-fold>

    // This is used by CSVimport to detect what type of product insert we are
    // looking at, or what error occured
    /**
     *
     * @param myProduct Object[]. The array is [0]
     * @return
     * @throws BasicException
     */
    public final String getProductRecordType(Object[] myProduct) throws BasicException {

        final SentenceFind m_getProductAllFields;
        final SentenceFind m_getProductRefAndCode;
        final SentenceFind m_getProductRefAndName;
        final SentenceFind m_getProductCodeAndName;
        final SentenceFind m_getProductByReference;
        final SentenceFind m_getProductByCode;
        final SentenceFind m_getProductByName;

        m_getProductAllFields = new PreparedSentence(this.session,
                "SELECT ID FROM products WHERE REFERENCE=? AND CODE=? AND NAME=? ",
                new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING, Datas.STRING }),
                new ProductIdRead());

        m_getProductRefAndCode = new PreparedSentence(this.session,
                "SELECT ID FROM products WHERE REFERENCE=? AND CODE=?",
                new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING }),
                new ProductIdRead());

        m_getProductRefAndName = new PreparedSentence(this.session,
                "SELECT ID FROM products WHERE REFERENCE=? AND NAME=? ",
                new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING }),
                new ProductIdRead());

        m_getProductCodeAndName = new PreparedSentence(this.session,
                "SELECT ID FROM products WHERE CODE=? AND NAME=? ",
                new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING }),
                new ProductIdRead());

        m_getProductByReference = new PreparedSentence(this.session,
                "SELECT ID FROM products WHERE REFERENCE=? ",
                SerializerWriteString.INSTANCE,
                new ProductIdRead());

        m_getProductByCode = new PreparedSentence(this.session,
                "SELECT ID FROM products WHERE CODE=? ",
                SerializerWriteString.INSTANCE,
                new ProductIdRead());

        m_getProductByName = new PreparedSentence(this.session,
                "SELECT ID FROM products WHERE NAME=? ",
                SerializerWriteString.INSTANCE,
                new ProductIdRead());

        // check if the product exist with all the details, if so return product ID
        if (m_getProductAllFields.find(myProduct) != null) {
            return m_getProductAllFields.find(myProduct).toString();
        }
        // check if the product exists with matching reference and code, but a different
        // name
        if (m_getProductRefAndCode.find(myProduct[0], myProduct[1]) != null) {
            return "Name change";
        }

        if (m_getProductRefAndName.find(myProduct[0], myProduct[2]) != null) {
            return "Barcode change";
        }

        if (m_getProductCodeAndName.find(myProduct[1], myProduct[2]) != null) {
            return "Reference change";
        }

        if (m_getProductByReference.find(myProduct[0]) != null) {
            return "Duplicate Reference found.";
        }

        if (m_getProductByCode.find(myProduct[1]) != null) {
            return "Duplicate Barcode found.";
        }

        if (m_getProductByName.find(myProduct[2]) != null) {
            return "Duplicate Description found.";
        }

        return "new";
    }

    /**
     *
     * @param myCustomer
     * @return
     * @throws BasicException
     */
    public final String getCustomerRecordType(Object[] myCustomer) throws BasicException {

        final SerializerRead customerIdRead = (DataRead dr) -> (dr.getString(1));

        final SentenceFind m_getCustomerAllFields;
        final SentenceFind m_getCustomerSearchKeyAndName;
        final SentenceFind m_getCustomerBySearchKey;
        final SentenceFind m_getCustomerByName;

        // duplicate this for now as will extend in future release
        m_getCustomerAllFields = new PreparedSentence(this.session,
                "SELECT ID FROM customers WHERE SEARCHKEY=? AND NAME=? ",
                new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING }),
                customerIdRead);

        m_getCustomerSearchKeyAndName = new PreparedSentence(this.session,
                "SELECT ID FROM customers WHERE SEARCHKEY=? AND NAME=? ",
                new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING }),
                customerIdRead);

        m_getCustomerBySearchKey = new PreparedSentence(this.session,
                "SELECT ID FROM customers WHERE SEARCHKEY=? ",
                SerializerWriteString.INSTANCE,
                customerIdRead);

        m_getCustomerByName = new PreparedSentence(this.session,
                "SELECT ID FROM customers WHERE NAME=? ",
                SerializerWriteString.INSTANCE,
                customerIdRead);

        if (m_getCustomerAllFields.find(myCustomer) != null) {
            return m_getCustomerAllFields.find(myCustomer).toString();
        }

        if (m_getCustomerSearchKeyAndName.find(myCustomer[0], myCustomer[1]) != null) {
            return "reference error";
        }

        if (m_getCustomerBySearchKey.find(myCustomer[0]) != null) {
            return "Duplicate Search Key found.";
        }

        if (m_getCustomerByName.find(myCustomer[1]) != null) {
            return "Duplicate Name found.";
        }

        return "new";
    }

    public final void updatePlaces(int x, int y, String id) throws BasicException {
        final SentenceExec m_updatePlaces = new StaticSentence(this.session,
                "UPDATE PLACES SET X = ?, Y = ? WHERE ID = ?",
                new SerializerWriteBasic(new Datas[] { Datas.INT, Datas.INT, Datas.STRING }));
        m_updatePlaces.exec(new Object[] { x, y, id });
    }

    public final List<VoucherInfo> getVouchersActiveList() throws BasicException {
        final SentenceList<VoucherInfo> m_voucherlist = new StaticSentence(this.session,
                "SELECT id, voucher_number, customer, amount, status FROM vouchers WHERE status LIKE 'A'",
                SerializerWriteString.INSTANCE,
                VoucherInfo.getSerializerRead());

        return m_voucherlist.list();
    }

    //// <editor-fold defaultstate="collapsed" desc="START OF ORDER">

    /**
     * 
     * @param orderId
     * @param qty
     * @param details
     * @param attributes
     * @param notes
     * @param ticketId
     * @param ordertime
     * @param displayId
     * @param auxiliary
     * @param completetime
     * @throws BasicException
     */
    public final void addOrder(String orderId, Integer qty,
            String details, String attributes, String notes, String ticketId,
            String ordertime, Integer displayId, String auxiliary, String completetime) throws BasicException {

        if (ordertime == null) {
            ordertime = Long.toString(new Date().getTime());
        }

        final SentenceExec m_addOrder = new StaticSentence(this.session,
                "INSERT INTO orders (ORDERID, QTY, DETAILS, ATTRIBUTES, "
                        + "NOTES, TICKETID, ORDERTIME, DISPLAYID, AUXILIARY, "
                        + "COMPLETETIME) "
                        + "VALUES (?, ?, ?, ?, ?, "
                        + "?, ?, ?, ?, ? ) ",
                new SerializerWriteBasic(new Datas[] {
                        Datas.STRING, // OrderId
                        Datas.INT, // Qty
                        Datas.STRING, // Details
                        Datas.STRING, // Attributes
                        Datas.STRING, // Notes
                        Datas.STRING, // TicketId
                        Datas.TIMESTAMP, // OrderTime
                        Datas.INT, // DisplayId
                        Datas.INT, // Auxiliary
                        Datas.TIMESTAMP // CompleteTime
                }));
        m_addOrder.exec(new Object[] { orderId, qty, details, attributes, notes, ticketId,
                ordertime, displayId, auxiliary, completetime });
    }

    /**
     * 
     * @param orderId
     * @param qty
     * @param details
     * @param attributes
     * @param notes
     * @param ticketId
     * @param ordertime
     * @param displayId
     * @param auxiliary
     * @param completetime
     * @throws BasicException
     */
    public final void updateOrder(String orderId, Integer qty,
            String details, String attributes, String notes, String ticketId,
            String ordertime, Integer displayId, String auxiliary, String completetime) throws BasicException {

        final SentenceExec m_updateOrder = new StaticSentence(this.session,
                "UPDATE orders SET "
                        + "ORDERID = ?, "
                        + "QTY = ?, "
                        + "DETAILS = ?, "
                        + "ATTRIBUTES = ?, "
                        + "NOTES = ?, "
                        + "TICKETID = ?, "
                        + "ORDERTIME = ?, "
                        + "DISPLAYID = ?, "
                        + "AUXILIARY = ?, "
                        + "COMPLETETIME = ? "
                        + "WHERE ORDERID = ? ",
                new SerializerWriteBasic(new Datas[] {
                        Datas.STRING, // OrderId
                        Datas.INT, // Qty
                        Datas.STRING, // Details
                        Datas.STRING, // Attributes
                        Datas.STRING, // Notes
                        Datas.STRING, // TicketId
                        Datas.STRING, // OrderTime
                        Datas.INT, // DisplayId
                        Datas.INT, // Auxiliary
                        Datas.STRING // CompleteTime
                }));
        m_updateOrder.exec(new Object[] { orderId, qty, details, attributes, notes, ticketId,
                ordertime, displayId, auxiliary, completetime });
    }

    /**
     * Delete Order
     * 
     * @param orderId
     * @throws BasicException
     */
    public void deleteOrder(String orderId) throws BasicException {
        final SentenceExec m_deleteOrder = new StaticSentence(this.session,
                "DELETE FROM orders WHERE ORDERID = ?",
                SerializerWriteString.INSTANCE);
        m_deleteOrder.exec(orderId);
    }
    //// </editor-fold>

    private final static class AppuserReader implements SerializerRead<AppUser> {

        final ThumbNailBuilder defaultUserTN = new ThumbNailBuilder(32, 32, "com/openbravo/images/user.png");

        @Override
        public AppUser readValues(DataRead dr) throws BasicException {

            return new AppUser(
                    dr.getString(1),
                    dr.getString(2),
                    dr.getString(3),
                    dr.getString(4),
                    dr.getString(5),
                    // new ImageIcon(tnb.getThumbNail(ImageUtils.readImage(dr.getBytes(6)))));
                    new ImageIcon(defaultUserTN.getThumbNail()));
        }
    }

    private final static class ProductIdRead implements SerializerRead<String> {

        @Override
        public String readValues(DataRead dr) throws BasicException {
            return dr.getString(1);
        }
    };
}
