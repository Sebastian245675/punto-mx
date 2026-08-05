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
package com.openbravo.pos.customers;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.*;
import com.openbravo.data.user.DefaultSaveProvider;
import com.openbravo.data.user.SaveProvider;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.BeanFactoryDataSingle;
import com.openbravo.pos.voucher.VoucherInfo;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author JG uniCenta
 * @author adrianromero
 */
public class DataLogicCustomers extends BeanFactoryDataSingle {

    protected Session s;
    private static final Logger LOGGER = Logger.getLogger(DataLogicCustomers.class.getName());
    private static final String INTERNAL_SUPABASE_URL = "https://wotsbsjxabwtovxpfgly.supabase.co/rest/v1";
    private static final String INTERNAL_SUPABASE_API_KEY = "sb_publishable_ztjnUyfwQ7rAFW3T-g4ocA_8vqYXbRQ";

    private Object getSupabaseService() {
        try {
            Class<?> cls = Class.forName("com.openbravo.pos.supabase.CustomerServiceSupabase");
            return cls.getConstructor(String.class, String.class).newInstance(INTERNAL_SUPABASE_URL, INTERNAL_SUPABASE_API_KEY);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Sebastian - Error cargando CustomerServiceSupabase por reflexion: " + e.getMessage(), e);
            return null;
        }
    }

    private static final Datas[] RESERVATION_DATA = new Datas[]{
        Datas.STRING, Datas.TIMESTAMP, Datas.TIMESTAMP, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.INT, Datas.BOOLEAN, Datas.STRING
    };

    private static final Datas[] CUSTOMER_DATA_OBJ = new Datas[]{
        Datas.OBJECT, Datas.STRING, //TAXID
        Datas.OBJECT, Datas.STRING, //SEARCHKEY
        Datas.OBJECT, Datas.STRING, //NAME
        Datas.OBJECT, Datas.STRING, //POSTAL
        Datas.OBJECT, Datas.STRING, //PHONE
        Datas.OBJECT, Datas.STRING //EMAIL
    };

    @Override
    public void init(Session s) {
        this.s = s;
        try {
            ensurePuntosColumnExists();
        } catch (BasicException e) {
            LOGGER.log(Level.SEVERE, "Error asegurando columna PUNTOS en customers: " + e.getMessage());
        }
    }

    private void ensurePuntosColumnExists() throws BasicException {
        try {
            new StaticSentence(s, "ALTER TABLE CUSTOMERS ADD COLUMN PUNTOS INTEGER DEFAULT 0").exec();
            LOGGER.log(Level.INFO, "Columna PUNTOS agregada exitosamente a la tabla CUSTOMERS.");
        } catch (BasicException e) {
            if (e.getMessage() != null && (e.getMessage().contains("already exists") || e.getMessage().contains("ya existe") || e.getMessage().contains("duplicate column"))) {
            } else {
                LOGGER.log(Level.WARNING, "No se pudo agregar la columna PUNTOS (podría ya existir) o error: " + e.getMessage());
            }
        }
    }

    public SentenceList<CustomerInfo> getCustomerList() {
        return new StaticSentence(s,
                new QBFBuilder("SELECT "
                        + "ID, TAXID, SEARCHKEY, NAME, "
                        + "POSTAL, EMAIL, PHONE, IMAGE, PUNTOS "
                        + "FROM customers "
                        + "WHERE VISIBLE = " + s.DB.TRUE() + " AND ?(QBF_FILTER) ORDER BY NAME",
                        new String[]{"TAXID", "SEARCHKEY", "NAME", "POSTAL", "PHONE", "EMAIL"}),
                new SerializerWriteBasic(CUSTOMER_DATA_OBJ),
                new CustomerInfoRead());
    }

    public final CustomerInfo getCustomerInfo(String id) throws BasicException {
        return (CustomerInfo) new PreparedSentence(s,
                "SELECT "
                + "ID, TAXID, SEARCHKEY, NAME, "
                + "POSTAL, EMAIL, PHONE, IMAGE, PUNTOS "
                + "FROM customers WHERE VISIBLE = " + s.DB.TRUE() + " "
                + "AND ID = ?",
                SerializerWriteString.INSTANCE,
                new CustomerInfoRead()).find(id);
    }

    public int updateCustomerExt(final CustomerInfoExt customer) throws BasicException {
        return new PreparedSentence(s,
                "UPDATE customers SET NOTES = ? WHERE ID = ?",
                SerializerWriteParams.INSTANCE
        ).exec(new DataParams() {
            @Override
            public void writeValues() throws BasicException {
                setString(1, customer.getNotes());
                setString(2, customer.getId());
            }
        });
    }

    public final SentenceList getReservationsList() {
        return new PreparedSentence(s,
                "SELECT R.ID, R.CREATED, R.DATENEW, C.CUSTOMER, customers.TAXID, customers.SEARCHKEY, COALESCE(customers.NAME, R.TITLE), R.CHAIRS, R.ISDONE, R.DESCRIPTION FROM reservations R LEFT OUTER JOIN reservation_customers C ON R.ID = C.ID LEFT OUTER JOIN customers ON C.CUSTOMER = customers.ID WHERE R.DATENEW >= ? AND R.DATENEW < ?",
                new SerializerWriteBasic(new Datas[]{Datas.TIMESTAMP, Datas.TIMESTAMP}),
                new SerializerReadBasic(RESERVATION_DATA));
    }

    public final SentenceExec getReservationsUpdate() {
        return new SentenceExecTransaction(s) {
            @Override
            public int execInTransaction(Object[] params) throws BasicException {
                new PreparedSentence(s, "DELETE FROM reservation_customers WHERE ID = ?", new SerializerWriteBasicExt(RESERVATION_DATA, new int[]{0})).exec(params);
                if (params[3] != null) {
                    new PreparedSentence(s, "INSERT INTO reservation_customers (ID, CUSTOMER) VALUES (?, ?)", new SerializerWriteBasicExt(RESERVATION_DATA, new int[]{0, 3})).exec(params);
                }
                return new PreparedSentence(s, "UPDATE reservations SET ID = ?, CREATED = ?, DATENEW = ?, TITLE = ?, CHAIRS = ?, ISDONE = ?, DESCRIPTION = ? WHERE ID = ?", new SerializerWriteBasicExt(RESERVATION_DATA, new int[]{0, 1, 2, 6, 7, 8, 9, 0})).exec(params);
            }
        };
    }

    public final SentenceExec getReservationsDelete() {
        return new SentenceExecTransaction(s) {
            @Override
            public int execInTransaction(Object[] params) throws BasicException {
                new PreparedSentence(s, "DELETE FROM reservation_customers WHERE ID = ?", new SerializerWriteBasicExt(RESERVATION_DATA, new int[]{0})).exec(params);
                return new PreparedSentence(s, "DELETE FROM reservations WHERE ID = ?", new SerializerWriteBasicExt(RESERVATION_DATA, new int[]{0})).exec(params);
            }
        };
    }

    public final SentenceExec getReservationsInsert() {
        return new SentenceExecTransaction(s) {
            @Override
            public int execInTransaction(Object[] params) throws BasicException {
                int i = new PreparedSentence(s, "INSERT INTO reservations (ID, CREATED, DATENEW, TITLE, CHAIRS, ISDONE, DESCRIPTION) VALUES (?, ?, ?, ?, ?, ?, ?)", new SerializerWriteBasicExt(RESERVATION_DATA, new int[]{0, 1, 2, 6, 7, 8, 9})).exec(params);
                if (params[3] != null) {
                    new PreparedSentence(s, "INSERT INTO reservation_customers (ID, CUSTOMER) VALUES (?, ?)", new SerializerWriteBasicExt(RESERVATION_DATA, new int[]{0, 3})).exec(params);
                }
                return i;
            }
        };
    }

    public final TableDefinition getTableCustomers() {
        return new TableDefinition(s, "customers",
                new String[]{"ID", "SEARCHKEY", "TAXID", "NAME", "TAXCATEGORY", "CARD", "MAXDEBT", "ADDRESS", "ADDRESS2", "POSTAL", "CITY", "REGION", "COUNTRY", "FIRSTNAME", "LASTNAME", "EMAIL", "PHONE", "PHONE2", "FAX", "NOTES", "VISIBLE", "CURDATE", "CURDEBT", "IMAGE", "ISVIP", "DISCOUNT", "MEMODATE", "PUNTOS"},
                new String[]{"ID", AppLocal.getIntString("label.searchkey"), AppLocal.getIntString("label.taxid"), AppLocal.getIntString("label.name"), "TAXCATEGORY", "CARD", AppLocal.getIntString("label.maxdebt"), AppLocal.getIntString("label.address"), AppLocal.getIntString("label.address2"), AppLocal.getIntString("label.postal"), AppLocal.getIntString("label.city"), AppLocal.getIntString("label.region"), AppLocal.getIntString("label.country"), AppLocal.getIntString("label.firstname"), AppLocal.getIntString("label.lastname"), AppLocal.getIntString("label.email"), AppLocal.getIntString("label.phone"), AppLocal.getIntString("label.phone2"), AppLocal.getIntString("label.fax"), AppLocal.getIntString("label.notes"), "VISIBLE", AppLocal.getIntString("label.curdate"), AppLocal.getIntString("label.curdebt"), "IMAGE", "ISVIP", "DISCOUNT", "MEMODATE", "PUNTOS"},
                new Datas[]{Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.DOUBLE, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.BOOLEAN, Datas.TIMESTAMP, Datas.DOUBLE, Datas.IMAGE, Datas.BOOLEAN, Datas.DOUBLE, Datas.TIMESTAMP, Datas.INT},
                new Formats[]{Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.CURRENCY, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.BOOLEAN, Formats.TIMESTAMP, Formats.CURRENCY, Formats.NULL, Formats.BOOLEAN, Formats.DOUBLE, Formats.TIMESTAMP, Formats.INT},
                new int[]{0});
    }

    public final VoucherInfo getVoucherInfo(String id) throws BasicException {
        return (VoucherInfo) new PreparedSentence(s, "SELECT vouchers.ID, VOUCHER_NUMBER, CUSTOMER, customers.NAME, AMOUNT, STATUS FROM vouchers JOIN customers ON customers.id = vouchers.CUSTOMER WHERE STATUS='A' AND vouchers.ID=?", SerializerWriteString.INSTANCE, VoucherInfo.getSerializerRead()).find(id);
    }

    public final VoucherInfo getVoucherInfoAll(String id) throws BasicException {
        return (VoucherInfo) new PreparedSentence(s,
                "SELECT vouchers.ID, VOUCHER_NUMBER, CUSTOMER, "
                + "customers.NAME, AMOUNT, STATUS "
                + "FROM vouchers "
                + "JOIN customers ON customers.id = vouchers.CUSTOMER  "
                + "WHERE vouchers.ID=?",
                SerializerWriteString.INSTANCE,
                VoucherInfo.getSerializerRead()).find(id);
    }

    public final PreparedSentence getVoucherNumber() {
        return new PreparedSentence(s,
                "SELECT SUBSTRING(MAX(VOUCHER_NUMBER),10,3) AS LAST_NUMBER FROM vouchers "
                + "WHERE SUBSTRING(VOUCHER_NUMBER,1,8) = ?",
                SerializerWriteString.INSTANCE, (SerializerRead<String>) (DataRead dr) -> dr.getString(1));
    }

    protected static class CustomerInfoRead implements SerializerRead<CustomerInfo> {
        @Override
        public CustomerInfo readValues(DataRead dr) throws BasicException {
            CustomerInfo c = new CustomerInfo(dr.getString(1));
            c.setTaxid(dr.getString(2));
            c.setSearchkey(dr.getString(3));
            c.setName(dr.getString(4));
            c.setPostal(dr.getString(5));
            c.setPhone(dr.getString(7)); // PHONE is 7th in query
            c.setEmail(dr.getString(6)); // EMAIL is 6th in query
            c.setImage(ImageUtils.readImage(dr.getBytes(8)));
            if (c instanceof CustomerInfoExt) {
                ((CustomerInfoExt) c).setPuntos(dr.getInt(9));
            }
            return c;
        }
    }

    private Datas[] customerData = new Datas[]{Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.DOUBLE, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.BOOLEAN, Datas.TIMESTAMP, Datas.DOUBLE, Datas.IMAGE, Datas.BOOLEAN, Datas.DOUBLE, Datas.TIMESTAMP, Datas.INT};

    private SentenceExec customerSentenceExecUpdate() {
        return new PreparedSentenceExec(this.s, "update customers set ID = ?, SEARCHKEY = ?, TAXID = ?, NAME = ?, TAXCATEGORY = ?, CARD = ?, MAXDEBT = ?, ADDRESS = ?, ADDRESS2 = ?, POSTAL = ?, CITY = ?, REGION = ?, COUNTRY = ?, FIRSTNAME = ?, LASTNAME = ?, EMAIL = ?, PHONE = ?, PHONE2 = ?, FAX = ?, NOTES = ?, VISIBLE = ?, CURDATE = ?, CURDEBT = ?, IMAGE = ?, ISVIP = ?, DISCOUNT = ?, MEMODATE = ?, PUNTOS = ? where ID = ?", customerData, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 0});
    }

    private SentenceExec customerSentenceExecDelete() {
        return new PreparedSentenceExec(this.s, "DELETE FROM customers WHERE ID = ?", new Datas[]{Datas.STRING}, new int[]{0});
    }

    private SentenceExec customerSentenceExecInsert() {
        return new PreparedSentenceExec(this.s, "insert into customers (ID, SEARCHKEY, TAXID, NAME, TAXCATEGORY, CARD, MAXDEBT, ADDRESS, ADDRESS2, POSTAL, CITY, REGION, COUNTRY, FIRSTNAME, LASTNAME, EMAIL, PHONE, PHONE2, FAX, NOTES, VISIBLE, CURDATE, CURDEBT, IMAGE, ISVIP, DISCOUNT, MEMODATE, PUNTOS) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", customerData, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27});
    }

    public SaveProvider<Object[]> getCustomerSaveProvider() {
        return new DefaultSaveProvider(customerSentenceExecUpdate(), customerSentenceExecInsert(), customerSentenceExecDelete()) {
            @Override
            public int insertData(Object[] value) throws BasicException {
                int i = super.insertData(value);
                sendToSupabase(value);
                return i;
            }
            @Override
            public int updateData(Object[] value) throws BasicException {
                int i = super.updateData(value);
                sendToSupabase(value);
                return i;
            }
        };
    }

    private void sendToSupabase(Object[] customer) {
        new Thread(() -> {
            try {
                Object svc = getSupabaseService();
                if (svc != null) {
                    svc.getClass().getMethod("upsertCustomer", String.class, String.class, String.class, String.class, String.class, String.class, int.class).invoke(svc, (String) customer[0], (String) customer[1], (String) customer[3], (String) customer[15], (String) customer[16], (String) customer[7], customer[27] == null ? 0 : (Integer) customer[27]);
                }
            } catch (Exception e) {
                System.err.println("❌ Sebastian - Error sincronizando a Supabase desde POS local: " + e.getMessage());
                LOGGER.log(Level.SEVERE, "Sebastian - Error sincronizando a Supabase desde POS local: ", e);
            }
        }).start();
    }

    public void refreshLocalCustomersFromSupabase() throws BasicException {
        try {
            Object svc = getSupabaseService();
            if (svc == null) {
                LOGGER.warning("Sebastian - No se pudo obtener el servicio de Supabase (svc es null)");
                return;
            }
            List remoteCustomers = (List) svc.getClass().getMethod("fetchAllCustomers").invoke(svc);
            LOGGER.info("Sebastian - Sincronizando clientes desde Supabase. Total remotos: " + (remoteCustomers != null ? remoteCustomers.size() : 0));
            if (remoteCustomers != null) {
                for (Object obj : remoteCustomers) {
                    Map remote = (Map) obj;
                    String id = (String) remote.get("id");
                    String nombre = (String) remote.get("nombre");
                    String email = (String) remote.get("email");
                    String phone = (String) remote.get("telefono");
                    String codigo = (String) remote.get("codigo");
                    String direccion = (String) remote.get("direccion");
                    int puntos = (remote.get("puntos") != null) ? ((Number) remote.get("puntos")).intValue() : 0;
                    LOGGER.info("Sebastian - Sincronizando: " + nombre + " [" + id + "], Puntos: " + puntos);

                    // Ignorar clientes temporales de la app si no tienen nombre
                    if (nombre == null || nombre.isEmpty()) {
                        LOGGER.info("Sebastian - Saltando cliente sin nombre de Supabase: ID=" + id);
                        continue;
                    }
                    
                    // Si el código (searchkey) es nulo o parece un UUID, intentar usar algo más amigable
                    if (codigo == null || codigo.trim().isEmpty() || codigo.length() > 30) {
                        // Si ya tiene un searchkey local corto, no lo sobrescribimos con el UUID
                        // Pero por ahora, si viene de la app sin código, generamos uno temporal
                        if (codigo == null || codigo.trim().isEmpty()) {
                            codigo = "APP-" + (System.currentTimeMillis() % 1000000);
                            LOGGER.info("Sebastian - Generando código amigable para cliente de la app: " + codigo);
                        }
                    }

                    // Intentar actualizar
                    int updated = new PreparedSentence(s, "UPDATE CUSTOMERS SET NAME = ?, EMAIL = ?, PHONE = ?, PUNTOS = ?, SEARCHKEY = ?, TAXID = ?, ADDRESS = ? WHERE ID = ?", new SerializerWriteBasic(new Datas[]{Datas.STRING, Datas.STRING, Datas.STRING, Datas.INT, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING})).exec(new Object[]{nombre, email, phone, puntos, codigo != null ? codigo : id, codigo != null ? codigo : id, direccion, id});

                    if (updated == 0) {
                        // Insertar nuevo si no existe
                        new PreparedSentence(s, "INSERT INTO CUSTOMERS (ID, SEARCHKEY, TAXID, NAME, EMAIL, PHONE, ADDRESS, VISIBLE, PUNTOS) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", new SerializerWriteBasic(new Datas[]{Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.BOOLEAN, Datas.INT})).exec(new Object[]{id, codigo != null ? codigo : id, codigo != null ? codigo : id, nombre, email, phone, direccion, true, puntos});
                    }

                    // Sincronizar tabla CLIENTE_PUNTOS
                    int cpUpdated = new PreparedSentence(s, "UPDATE CLIENTE_PUNTOS SET PUNTOS_ACTUALES = ?, PUNTOS_TOTALES = CASE WHEN PUNTOS_TOTALES < ? THEN ? ELSE PUNTOS_TOTALES END, FECHA_ULTIMA_TRANSACCION = ? WHERE CLIENTE_ID = ?", new SerializerWriteBasic(new Datas[]{Datas.INT, Datas.INT, Datas.INT, Datas.TIMESTAMP, Datas.STRING})).exec(new Object[]{puntos, puntos, puntos, new java.sql.Timestamp(System.currentTimeMillis()), id});
                    if (cpUpdated == 0) {
                        new PreparedSentence(s, "INSERT INTO CLIENTE_PUNTOS (ID, CLIENTE_ID, PUNTOS_ACTUALES, PUNTOS_TOTALES, FECHA_CREACION) VALUES (?, ?, ?, ?, ?)", new SerializerWriteBasic(new Datas[]{Datas.STRING, Datas.STRING, Datas.INT, Datas.INT, Datas.TIMESTAMP})).exec(new Object[]{UUID.randomUUID().toString(), id, puntos, puntos, new java.sql.Timestamp(System.currentTimeMillis())});
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al refrescar clientes desde Supabase: " + e.getMessage());
        }
    }
}
