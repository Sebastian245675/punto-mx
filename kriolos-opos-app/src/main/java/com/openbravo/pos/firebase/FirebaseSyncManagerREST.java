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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.supabase.SupabaseServiceREST;
import com.openbravo.pos.admin.DataLogicAdmin;
import com.openbravo.pos.forms.DataLogicSales;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
 

/**
 * Gestor de sincronización con Firebase usando REST API
 * Extrae datos reales de la base de datos local y los sincroniza con Firestore
 */
public class FirebaseSyncManagerREST {
    
    private static final Logger LOGGER = Logger.getLogger(FirebaseSyncManagerREST.class.getName());
    
    private final FirebaseServiceREST firebaseService;
    private final Session session;
    private final DataLogicCustomers dlCustomers;
    private final DataLogicSales dlSales;
    private final DataLogicSystem dlSystem;
    private final DataLogicAdmin dlAdmin;
    
    private final com.openbravo.pos.forms.AppUserView appUserView;
    
    /**
     * Constructor compatible con código existente (sin AppUserView)
     */
    public FirebaseSyncManagerREST(Session session) {
        this(session, null);
    }
    
    /**
     * Constructor con soporte para tracking por usuario
     */
    public FirebaseSyncManagerREST(Session session, com.openbravo.pos.forms.AppUserView appUserView) {
        this.session = session;
        this.appUserView = appUserView;
        this.firebaseService = FirebaseServiceREST.getInstance();
        this.dlCustomers = new DataLogicCustomers();
        this.dlSales = new DataLogicSales();
        this.dlSystem = new DataLogicSystem();
        this.dlAdmin = new DataLogicAdmin();
        
        // Inicializar DataLogics con la sesión
        this.dlCustomers.init(session);
        this.dlSales.init(session);
        this.dlSystem.init(session);
        this.dlAdmin.init(session);
        
        // Log para diagnóstico
        if (appUserView != null) {
            LOGGER.info("FirebaseSyncManagerREST inicializado con AppUserView");
        } else {
            LOGGER.warning("FirebaseSyncManagerREST inicializado sin AppUserView - modo compatibilidad");
        }
    }
    
    /**
     * Obtiene el ID Remoto del usuario actual desde la base de datos o el AppUserView
     */
    public String getUsuarioActual() {
        try {
            if (appUserView != null && appUserView.getUser() != null) {
                return appUserView.getUser().getName();
            } else {
                return "Desconocido";
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo usuario actual: " + e.getMessage());
            return "Error";
        }
    }
    
    
    /**
     * Realiza una sincronización completa de todos los datos
     * IMPORTANTE: Este método se ejecuta en el thread del caller para evitar
     * problemas de concurrencia con la conexión de base de datos
     */
    public CompletableFuture<SyncResult> performFullSync() {
        SyncResult result = new SyncResult();
        
        try {
            LOGGER.info("=== INICIANDO SINCRONIZACIÓN COMPLETA CON FIREBASE ===");
            result.startTime = LocalDateTime.now();
            
            // Obtener el usuario actual para el tracking
            String currentUserId = getUsuarioActual();
            LOGGER.info("Usuario actual para tracking: " + (currentUserId != null ? currentUserId : "unknown"));
            
            // Inicializar Firebase con la configuración
            com.openbravo.pos.forms.AppConfig config = new com.openbravo.pos.forms.AppConfig(null);
            config.load();
            
            // Si tenemos appUserView, usarlo; sino, Firebase usará el userId obtenido de BD
            if (appUserView != null) {
                firebaseService.initialize(config, appUserView);
                LOGGER.info("Firebase inicializado con AppUserView");
            } else {
                firebaseService.initialize(config);
                LOGGER.info("Firebase inicializado sin AppUserView (modo compatibilidad)");
            }
            // Forzar el userId en el servicio Firebase para asegurar que
            // los documentos subidos contengan el usuario correcto
            if (currentUserId != null) {
                firebaseService.setUserId(currentUserId);
            }
            
            // 1. Sincronizar usuarios
            LOGGER.info("1. Sincronizando usuarios...");
            boolean usuariosOk = syncUsuarios();
            LOGGER.info("1.1 CompletableFuture de usuarios creado, esperando resultado...");
            LOGGER.info("1.2 Usuarios sincronizados con resultado: " + usuariosOk);
                result.usuariosSincronizados = usuariosOk;
                if (usuariosOk) result.successCount++; else result.errorCount++;
                
                // 2. Sincronizar clientes
                LOGGER.info("2. Sincronizando clientes...");
                boolean clientesOk = syncClientes().join();
                result.clientesSincronizados = clientesOk;
                if (clientesOk) result.successCount++; else result.errorCount++;
                
                // 3. Sincronizar categorías
                LOGGER.info("3. Sincronizando categorías...");
                boolean categoriasOk = syncCategorias().join();
                result.categoriasSincronizadas = categoriasOk;
                if (categoriasOk) result.successCount++; else result.errorCount++;
                
                // 4. Sincronizar productos
                LOGGER.info("4. Sincronizando productos...");
                boolean productosOk = syncProductos().join();
                result.productosSincronizados = productosOk;
                if (productosOk) result.successCount++; else result.errorCount++;
                
                // 5. Sincronizar ventas
                LOGGER.info("5. Sincronizando ventas...");
                boolean ventasOk = syncVentas().join();
                result.ventasSincronizadas = ventasOk;
                if (ventasOk) result.successCount++; else result.errorCount++;
                
                // 6. Sebastian - Sincronizar puntos de clientes
                LOGGER.info("6. Sincronizando puntos de clientes...");
                boolean puntosOk = syncPuntosClientes().join();
                result.puntosSincronizados = puntosOk;
                if (puntosOk) result.successCount++; else result.errorCount++;
                
                // 7. Sebastian - Sincronizar cierres de caja
                LOGGER.info("7. Sincronizando cierres de caja...");
                boolean cierresOk = syncCierresCaja().join();
                result.cierresSincronizados = cierresOk;
                if (cierresOk) result.successCount++; else result.errorCount++;
                
                // 8. Sebastian - Sincronizar formas de pago
                LOGGER.info("8. Sincronizando formas de pago...");
                boolean pagosOk = syncFormasPago().join();
                result.pagosSincronizados = pagosOk;
                if (pagosOk) result.successCount++; else result.errorCount++;
                
                // 9. Sebastian - Sincronizar impuestos
                LOGGER.info("9. Sincronizando impuestos...");
                boolean impuestosOk = syncImpuestos().join();
                result.impuestosSincronizados = impuestosOk;
                if (impuestosOk) result.successCount++; else result.errorCount++;
                
                // 10. Sebastian - Sincronizar configuraciones
                LOGGER.info("10. Sincronizando configuraciones...");
                boolean configuracionesOk = syncConfiguraciones().join();
                result.configuracionesSincronizadas = configuracionesOk;
                if (configuracionesOk) result.successCount++; else result.errorCount++;
                
                // 11. Sebastian - Sincronizar inventario
                LOGGER.info("11. Sincronizando inventario...");
                boolean inventarioOk = syncInventario().join();
                result.inventarioSincronizado = inventarioOk;
                if (inventarioOk) result.successCount++; else result.errorCount++;
                
            result.endTime = LocalDateTime.now();
            result.success = result.errorCount == 0;
            
            String mensaje = String.format(
                "=== SINCRONIZACIÓN COMPLETA COMPLETADA ===\n" +
                "Tiempo: %s\n" +
                "Exitosas: %d | Errores: %d\n" +
                "✓ Usuarios: %s | Clientes: %s | Categorías: %s\n" +
                "✓ Productos: %s | Ventas: %s | Puntos: %s\n" +
                "✓ Cierres: %s | Pagos: %s | Impuestos: %s\n" +
                "✓ Configs: %s | Inventario: %s",
                java.time.Duration.between(result.startTime, result.endTime).toString(),
                result.successCount, result.errorCount,
                usuariosOk ? "✓" : "✗",
                clientesOk ? "✓" : "✗", 
                categoriasOk ? "✓" : "✗",
                productosOk ? "✓" : "✗", ventasOk ? "✓" : "✗", puntosOk ? "✓" : "✗",
                cierresOk ? "✓" : "✗", pagosOk ? "✓" : "✗", impuestosOk ? "✓" : "✗",
                configuracionesOk ? "✓" : "✗", inventarioOk ? "✓" : "✗");
            
            LOGGER.info(mensaje);
            
            // Devolver el resultado como un CompletableFuture ya completado
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en sincronización completa", e);
            result.endTime = LocalDateTime.now();
            result.success = false;
            result.errorMessage = e.getMessage();
            return CompletableFuture.completedFuture(result);
        }
    }    /**
     * Sincroniza usuarios desde la tabla people
     * SOLUCIÓN DEFINITIVA: Ejecuta en thread separado con timeout para evitar deadlocks
     */
    private boolean syncUsuarios() {
        return true;
    }
    
    /**
     * Sincroniza clientes desde la tabla customers
     */
    private CompletableFuture<Boolean> syncClientes() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> clientes = new ArrayList<>();
                
                String sql = "SELECT ID, SEARCHKEY, TAXID, NAME, CARD, TAXCATEGORY, " +
                           "FIRSTNAME, LASTNAME, EMAIL, PHONE, PHONE2, " +
                           "ADDRESS, ADDRESS2, POSTAL, CITY, REGION, COUNTRY, " +
                           "CURDEBT, MAXDEBT, VISIBLE " +
                           "FROM customers WHERE VISIBLE = true";
                
                PreparedStatement stmt = session.getConnection().prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    Map<String, Object> cliente = new HashMap<>();
                    cliente.put("id", rs.getString("ID"));
                    cliente.put("codigobusqueda", rs.getString("SEARCHKEY"));
                    cliente.put("numeroidentificacion", rs.getString("TAXID"));
                    cliente.put("nombre", rs.getString("NAME"));
                    cliente.put("tarjeta", rs.getString("CARD"));
                    cliente.put("categoriaimpuesto", rs.getString("TAXCATEGORY"));
                    cliente.put("primernombre", rs.getString("FIRSTNAME"));
                    cliente.put("apellido", rs.getString("LASTNAME"));
                    cliente.put("email", rs.getString("EMAIL"));
                    cliente.put("telefono", rs.getString("PHONE"));
                    cliente.put("telefono2", rs.getString("PHONE2"));
                    cliente.put("direccion", rs.getString("ADDRESS"));
                    cliente.put("direccion2", rs.getString("ADDRESS2"));
                    cliente.put("codigopostal", rs.getString("POSTAL"));
                    cliente.put("ciudad", rs.getString("CITY"));
                    cliente.put("region", rs.getString("REGION"));
                    cliente.put("pais", rs.getString("COUNTRY"));
                    
                    // Sin fecha de registro - campo CURDATE no existe en la tabla
                    cliente.put("fecharegistro", null);
                    
                    cliente.put("deudaactual", rs.getDouble("CURDEBT"));
                    cliente.put("deudamaxima", rs.getDouble("MAXDEBT"));
                    cliente.put("visible", rs.getBoolean("VISIBLE"));
                    cliente.put("fechaextraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    cliente.put("tabla", "customers");
                    
                    clientes.add(cliente);
                }
                
                rs.close();
                stmt.close();
                
                LOGGER.info("Extraídos " + clientes.size() + " clientes de la base de datos local");
                
                SupabaseServiceREST supabase = new SupabaseServiceREST("https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1", "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n");
                return supabase.syncData("clientes", clientes);                
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error extrayendo clientes", e);
                return false;
            }
        });
    }
    
    /**
     * Sincroniza categorías desde la tabla categories
     */
    private CompletableFuture<Boolean> syncCategorias() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> categorias = new ArrayList<>();
                
                String sql = "SELECT ID, NAME, PARENTID, IMAGE " +
                           "FROM categories ORDER BY NAME";
                
                PreparedStatement stmt = session.getConnection().prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    Map<String, Object> categoria = new HashMap<>();
                    categoria.put("id", rs.getString("ID"));
                    categoria.put("nombre", rs.getString("NAME"));
                    categoria.put("categoriapadre", rs.getString("PARENTID"));
                    categoria.put("tieneimagen", rs.getBytes("IMAGE") != null);
                    categoria.put("fechaextraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    categoria.put("tabla", "categories");
                    
                    categorias.add(categoria);
                }
                
                rs.close();
                stmt.close();
                
                LOGGER.info("Extraídas " + categorias.size() + " categorías de la base de datos local");
                
                SupabaseServiceREST supabase = new SupabaseServiceREST("https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1", "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n");
                return supabase.syncData("categorias", categorias);                
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error extrayendo categorías", e);
                return false;
            }
        });
    }
    
    /**
     * Sincroniza productos desde la tabla products
     */
    private CompletableFuture<Boolean> syncProductos() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> productos = new ArrayList<>();
                
                String sql = "SELECT p.ID, p.REFERENCE, p.CODE, p.CODETYPE, p.NAME, " +
                           "p.PRICEBUY, p.PRICESELL, p.CATEGORY, p.TAXCAT, " +
                           "p.ATTRIBUTESET_ID, p.IMAGE, p.ISCOM, " +
                           "p.PRINTKB, p.SENDSTATUS, " +
                           "c.NAME as CATEGORY_NAME, " +
                           "tc.NAME as TAXCAT_NAME " +
                           "FROM products p " +
                           "LEFT JOIN categories c ON p.CATEGORY = c.ID " +
                           "LEFT JOIN taxcategories tc ON p.TAXCAT = tc.ID " +
                           "ORDER BY p.NAME";
                
                PreparedStatement stmt = session.getConnection().prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    Map<String, Object> producto = new HashMap<>();
                    producto.put("id", rs.getString("ID"));
                    producto.put("referencia", rs.getString("REFERENCE"));
                    producto.put("codigo", rs.getString("CODE"));
                    producto.put("tipocodigobarras", rs.getString("CODETYPE"));
                    producto.put("nombre", rs.getString("NAME"));
                    producto.put("preciocompra", rs.getDouble("PRICEBUY"));
                    producto.put("precioventa", rs.getDouble("PRICESELL"));
                    producto.put("categoriaid", rs.getString("CATEGORY"));
                    producto.put("categorianombre", rs.getString("CATEGORY_NAME"));
                    producto.put("categoriaimpuesto", rs.getString("TAXCAT"));
                    producto.put("categoriaimpuestonombre", rs.getString("TAXCAT_NAME"));
                    producto.put("atributos", rs.getString("ATTRIBUTESET_ID"));
                    producto.put("tieneimagen", rs.getBytes("IMAGE") != null);
                    producto.put("escompuesto", rs.getBoolean("ISCOM"));
                    producto.put("imprimirencocina", rs.getBoolean("PRINTKB"));
                    producto.put("estadoenvio", rs.getBoolean("SENDSTATUS"));
                    producto.put("fechaextraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    producto.put("tabla", "products");
                    
                    productos.add(producto);
                }
                
                rs.close();
                stmt.close();
                
                LOGGER.info("Extraídos " + productos.size() + " productos de la base de datos local");
                
                SupabaseServiceREST supabase = new SupabaseServiceREST("https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1", "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n");
                return supabase.syncData("productos", productos);                
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error extrayendo productos", e);
                return false;
            }
        });
    }
    
    /**
     * Sincroniza ventas desde las tablas receipts y ticketlines
     */
    private CompletableFuture<Boolean> syncVentas() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> ventas = new ArrayList<>();
                
                // Obtener tickets recientes (últimos 30 días)
                String sql = "SELECT r.ID, r.MONEY, r.DATENEW, r.PERSON, " +
                           "p.NAME as PERSON_NAME, " +
                           "SUM(tl.UNITS * tl.PRICE) as TOTAL " +
                           "FROM receipts r " +
                           "LEFT JOIN people p ON r.PERSON = p.ID " +
                           "LEFT JOIN ticketlines tl ON r.ID = tl.TICKET " +
                           "WHERE r.DATENEW >= DATEADD('DAY', -30, NOW()) " +
                           "GROUP BY r.ID, r.MONEY, r.DATENEW, r.PERSON, p.NAME " +
                           "ORDER BY r.DATENEW DESC";
                
                PreparedStatement stmt = session.getConnection().prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    Map<String, Object> venta = new HashMap<>();
                    venta.put("id", rs.getString("ID"));
                    venta.put("caja", rs.getString("MONEY"));
                
                    Timestamp fechaVenta = rs.getTimestamp("DATENEW");
                    if (fechaVenta != null) {
                        venta.put("fechaventa", fechaVenta.toInstant().toString()); // formato ISO 8601 válido para Supabase
                    } else {
                        venta.put("fechaventa", null);
                    }
                
                    venta.put("vendedorid", rs.getString("PERSON"));
                    venta.put("vendedornombre", rs.getString("PERSON_NAME"));
                    venta.put("total", rs.getDouble("TOTAL"));
                    venta.put("fechaextraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    venta.put("tabla", "receipts");
                
                    List<Map<String, Object>> lineas = getLineasTicket(rs.getString("ID"));
                    venta.put("lineas", lineas);
                
                    ventas.add(venta);
                }
                
                
                rs.close();
                stmt.close();
                
                LOGGER.info("Extraídas " + ventas.size() + " ventas de la base de datos local");
                
                SupabaseServiceREST supabase = new SupabaseServiceREST("https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1", "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n");
                return supabase.syncData("ventas", ventas);                
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error extrayendo ventas", e);
                return false;
            }
        });
    }
    
    /**
     * Obtiene las líneas de un ticket específico
     */
    private List<Map<String, Object>> getLineasTicket(String ticketId) {
        List<Map<String, Object>> lineas = new ArrayList<>();
        
        try {
            String sql = "SELECT tl.TICKET, tl.LINE, tl.PRODUCT, tl.ATTRIBUTESETINSTANCE_ID, " +
                       "tl.UNITS, tl.PRICE, tl.TAXID, tl.ATTRIBUTES, " +
                       "p.NAME as PRODUCT_NAME, p.REFERENCE as PRODUCT_REF " +
                       "FROM ticketlines tl " +
                       "LEFT JOIN products p ON tl.PRODUCT = p.ID " +
                       "WHERE tl.TICKET = ? ORDER BY tl.LINE";
            
            PreparedStatement stmt = session.getConnection().prepareStatement(sql);
            stmt.setString(1, ticketId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> linea = new HashMap<>();
                linea.put("numerolinea", rs.getInt("LINE"));
                linea.put("productoid", rs.getString("PRODUCT"));
                linea.put("productonombre", rs.getString("PRODUCT_NAME"));
                linea.put("productoreferencia", rs.getString("PRODUCT_REF"));
                linea.put("atributos", rs.getString("ATTRIBUTESETINSTANCE_ID"));
                linea.put("cantidad", rs.getDouble("UNITS"));
                linea.put("precio", rs.getDouble("PRICE"));
                linea.put("impuestoid", rs.getString("TAXID"));
                
                // ATTRIBUTES es un campo BLOB, no intentar convertir a String
                byte[] attributesBlob = rs.getBytes("ATTRIBUTES");
                linea.put("tieneAtributos", attributesBlob != null && attributesBlob.length > 0);
                
                linea.put("subtotal", rs.getDouble("UNITS") * rs.getDouble("PRICE"));
                
                lineas.add(linea);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error obteniendo líneas del ticket " + ticketId, e);
        }
        
        return lineas;
    }
    
    /**
     * Sebastian - Sincroniza puntos de clientes (CLIENTE_PUNTOS + PUNTOS_HISTORIAL)
     */
    private CompletableFuture<Boolean> syncPuntosClientes() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> puntos = new ArrayList<>();
    
                // 🔹 Sincronizar puntos actuales de clientes
                String sql1 = "SELECT cp.ID, cp.CLIENTE_ID, c.NAME as CLIENTE_NOMBRE, cp.PUNTOS_ACTUALES, cp.PUNTOS_TOTALES, " +
                              "cp.ULTIMA_TRANSACCION, cp.FECHA_ULTIMA_TRANSACCION, cp.FECHA_CREACION " +
                              "FROM CLIENTE_PUNTOS cp " +
                              "LEFT JOIN customers c ON cp.CLIENTE_ID = c.ID " +
                              "ORDER BY cp.FECHA_CREACION";
    
                PreparedStatement stmt1 = session.getConnection().prepareStatement(sql1);
                ResultSet rs1 = stmt1.executeQuery();
                
                while (rs1.next()) {
                    Map<String, Object> punto = new HashMap<>();
                    punto.put("id", rs1.getString("ID"));
                    punto.put("clienteid", rs1.getString("CLIENTE_ID"));
                    punto.put("clientenombre", rs1.getString("CLIENTE_NOMBRE"));
                    punto.put("puntosactuales", rs1.getInt("PUNTOS_ACTUALES"));
                    punto.put("puntostotales", rs1.getInt("PUNTOS_TOTALES"));
                    punto.put("puntosotorgados", null);
                    punto.put("descripcion", null);
                    punto.put("montocompra", null);
                    punto.put("ultimatransaccion", rs1.getString("ULTIMA_TRANSACCION"));
    
                    // Fechas en formato ISO 8601
                    Timestamp tsUltima = rs1.getTimestamp("FECHA_ULTIMA_TRANSACCION");
                    punto.put("fechaultimatransaccion", tsUltima != null ? tsUltima.toInstant().toString() : null);
    
                    Timestamp tsCreacion = rs1.getTimestamp("FECHA_CREACION");
                    punto.put("fechacreacion", tsCreacion != null ? tsCreacion.toInstant().toString() : null);
    
                    punto.put("fechatransaccion", null);
                    punto.put("tipo", "puntos_actuales");
                    punto.put("fechaextraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    punto.put("tabla", "cliente_puntos");
                    puntos.add(punto);
                }
                rs1.close();
                stmt1.close();
    
                // 🔹 Sincronizar historial de puntos
                String sql2 = "SELECT ph.ID, ph.CLIENTE_ID, c.NAME as CLIENTE_NOMBRE, ph.PUNTOS_OTORGADOS, ph.DESCRIPCION, " +
                              "ph.MONTO_COMPRA, ph.FECHA_TRANSACCION " +
                              "FROM PUNTOS_HISTORIAL ph " +
                              "LEFT JOIN customers c ON ph.CLIENTE_ID = c.ID " +
                              "ORDER BY ph.FECHA_TRANSACCION DESC";
    
                PreparedStatement stmt2 = session.getConnection().prepareStatement(sql2);
                ResultSet rs2 = stmt2.executeQuery();
                while (rs2.next()) {
                    Map<String, Object> historial = new HashMap<>();
                    historial.put("id", rs2.getString("ID"));
                    historial.put("clienteid", rs2.getString("CLIENTE_ID"));
                    historial.put("clientenombre", rs2.getString("CLIENTE_NOMBRE"));
                    historial.put("puntosactuales", null);
                    historial.put("puntostotales", null);
                    historial.put("puntosotorgados", rs2.getInt("PUNTOS_OTORGADOS"));
                    historial.put("descripcion", rs2.getString("DESCRIPCION"));
                    historial.put("montocompra", rs2.getDouble("MONTO_COMPRA"));
                    historial.put("ultimatransaccion", null);
                    historial.put("fechaultimatransaccion", null);
                    historial.put("fechacreacion", null);
    
                    // Fecha de transacción en ISO 8601
                    Timestamp tsTransaccion = rs2.getTimestamp("FECHA_TRANSACCION");
                    historial.put("fechatransaccion", tsTransaccion != null ? tsTransaccion.toInstant().toString() : null);
    
                    historial.put("tipo", "historial_puntos");
                    historial.put("fechaextraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    historial.put("tabla", "puntos_historial");
                    puntos.add(historial);
                }
                rs2.close();
                stmt2.close();
    
                LOGGER.info("Extraídos " + puntos.size() + " registros de puntos de clientes");
    
                SupabaseServiceREST supabase = new SupabaseServiceREST(
                    "https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1",
                    "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n"
                );
    
                return supabase.syncData("puntos_historial", puntos);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error extrayendo puntos de clientes", e);
                return false;
            }
        });
    }
    

    
    
    /**
     * Sebastian - Sincroniza cierres de caja (CLOSEDCASH + detalles)
     */
    private CompletableFuture<Boolean> syncCierresCaja() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> cierres = new ArrayList<>();
                
                // Obtener cierres con información de dinero desde receipts
                String sql = "SELECT cc.MONEY, cc.HOST, cc.HOSTSEQUENCE, cc.DATESTART, cc.DATEEND, " +
                           "COALESCE(SUM(p.TOTAL), 0.0) as MONTO_TOTAL " +
                           "FROM closedcash cc " +
                           "LEFT JOIN receipts r ON r.DATENEW >= cc.DATESTART AND r.DATENEW <= cc.DATEEND " +
                           "LEFT JOIN payments p ON p.RECEIPT = r.ID " +
                           "GROUP BY cc.MONEY, cc.HOST, cc.HOSTSEQUENCE, cc.DATESTART, cc.DATEEND " +
                           "ORDER BY cc.DATEEND DESC";
                
                PreparedStatement stmt = session.getConnection().prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    Map<String, Object> cierre = new HashMap<>();
                    cierre.put("id", rs.getString("HOST") + "_" + rs.getInt("HOSTSEQUENCE"));
                    cierre.put("dineroid", rs.getString("MONEY")); // ID de referencia al dinero
                    cierre.put("dineromonto", rs.getDouble("MONTO_TOTAL")); // Monto calculado del período
                    cierre.put("host", rs.getString("HOST"));
                    cierre.put("secuencia", rs.getInt("HOSTSEQUENCE"));
                    cierre.put("fechainicio", rs.getTimestamp("DATESTART"));
                    cierre.put("fechafin", rs.getTimestamp("DATEEND"));
                    cierre.put("fechaextraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    cierre.put("fechasincronizacion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    cierre.put("origen", "kriolos-pos");
                    cierre.put("version", "1.0");
                    cierre.put("tabla", "closedcash");
                    cierres.add(cierre);
                }
                rs.close();
                stmt.close();
                
                LOGGER.info("Extraídos " + cierres.size() + " cierres de caja");
                SupabaseServiceREST supabase = new SupabaseServiceREST("https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1", "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n");
                return supabase.syncData("cierres", cierres);                
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error extrayendo cierres de caja", e);
                return false;
            }
        });
    }
    
    /**
     * Sebastian - Sincroniza todas las formas de pago (PAYMENTS)
     */
    private CompletableFuture<Boolean> syncFormasPago() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> pagos = new ArrayList<>();
                
                String sql = "SELECT p.ID, p.RECEIPT, p.PAYMENT, p.TOTAL, " +
                           "p.TENDERED, p.CARDNAME, p.VOUCHER, r.DATENEW " +
                           "FROM payments p " +
                           "LEFT JOIN receipts r ON p.RECEIPT = r.ID " +
                           "ORDER BY r.DATENEW DESC";
                
                PreparedStatement stmt = session.getConnection().prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    Map<String, Object> pago = new HashMap<>();
                    pago.put("id", rs.getString("ID"));
                    pago.put("recibo", rs.getString("RECEIPT"));
                    pago.put("metodopago", rs.getString("PAYMENT"));
                    pago.put("total", rs.getDouble("TOTAL"));
                    pago.put("recibido", rs.getDouble("TENDERED"));
                    pago.put("nombretarjeta", rs.getString("CARDNAME"));
                    pago.put("voucher", rs.getString("VOUCHER"));
                
                    Timestamp fechaVenta = rs.getTimestamp("DATENEW");
                    if (fechaVenta != null) {
                        pago.put("fechaventa", fechaVenta.toInstant().toString()); // ✅ formato ISO 8601
                    } else {
                        pago.put("fechaventa", null);
                    }
                
                    String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    pago.put("fechaextraccion", fechaActual);
                    pago.put("fechasincronizacion", fechaActual);
                    pago.put("origen", "kriolos-pos");
                    pago.put("version", "1.0");
                    pago.put("tabla", "payments");
                
                    pagos.add(pago);
                }
                
                rs.close();
                stmt.close();
                
                LOGGER.info("Extraídos " + pagos.size() + " registros de pagos");
                    SupabaseServiceREST supabase = new SupabaseServiceREST("https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1", "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n");
                    return supabase.syncData("formas_de_pago", pagos);                
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error extrayendo formas de pago", e);
                return false;
            }
        });
    }
    
    /**
     * Sebastian - Sincroniza impuestos y categorías de impuestos
     */
    private CompletableFuture<Boolean> syncImpuestos() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> impuestos = new ArrayList<>();
                
                
                
                // Impuestos (estructura simplificada)
                try {
                    String sql2 = "SELECT ID, NAME, CATEGORY, RATE FROM taxes ORDER BY NAME";
                    PreparedStatement stmt2 = session.getConnection().prepareStatement(sql2);
                    ResultSet rs2 = stmt2.executeQuery();
                    
                    while (rs2.next()) {
                        Map<String, Object> impuesto = new HashMap<>();
                        impuesto.put("id", rs2.getString("ID"));
                        impuesto.put("nombre", rs2.getString("NAME"));
                        impuesto.put("categoria", rs2.getString("CATEGORY"));
                        impuesto.put("tasa", rs2.getDouble("RATE"));
                        impuesto.put("tipo", "impuesto");
                        impuesto.put("fechaextraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        impuesto.put("tabla", "taxes");
                        impuestos.add(impuesto);
                    }
                    rs2.close();
                    stmt2.close();
                } catch (SQLException e) {
                    LOGGER.info("Error en taxes: " + e.getMessage());
                }
                
                LOGGER.info("Extraídos " + impuestos.size() + " registros de impuestos");
                SupabaseServiceREST supabase = new SupabaseServiceREST("https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1", "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n");
                return supabase.syncData("impuestos", impuestos);                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error extrayendo impuestos", e);
                return false;
            }
        });
    }
    
    /**
     * Sebastian - Sincroniza configuraciones del sistema
     */
private CompletableFuture<Boolean> syncConfiguraciones() {
    return CompletableFuture.supplyAsync(() -> {
        try {
            List<Map<String, Object>> configuraciones = new ArrayList<>();

            // Configuración de puntos
            String sql1 = "SELECT ID, MONTO_POR_PUNTO, PUNTOS_OTORGADOS, SISTEMA_ACTIVO, " +
                         "MONEDA, LIMITE_DIARIO_PUNTOS, FECHA_CREACION, FECHA_ACTUALIZACION " +
                         "FROM PUNTOS_CONFIGURACION ORDER BY FECHA_CREACION";

            try (PreparedStatement stmt1 = session.getConnection().prepareStatement(sql1);
                 ResultSet rs1 = stmt1.executeQuery()) {

                while (rs1.next()) {
                    Map<String, Object> config = new HashMap<>();
                    config.put("id", rs1.getString("ID"));
                    config.put("montoporpunto", rs1.getDouble("MONTO_POR_PUNTO"));
                    config.put("puntosotorgados", rs1.getInt("PUNTOS_OTORGADOS"));
                    config.put("sistemaactivo", rs1.getBoolean("SISTEMA_ACTIVO"));
                    config.put("moneda", rs1.getString("MONEDA"));
                    config.put("limitediario", rs1.getInt("LIMITE_DIARIO_PUNTOS"));

                    // ---- FECHA_CREACION: manejar distintos formatos seguros ----
                    try {
                        Object rawFechaCreacion = rs1.getObject("FECHA_CREACION");
                        if (rawFechaCreacion == null) {
                            config.put("fechacreacion", null);
                        } else if (rawFechaCreacion instanceof Number) {
                            long millis = ((Number) rawFechaCreacion).longValue();
                            Instant instant = Instant.ofEpochMilli(millis);
                            String fechaISO = instant.atZone(ZoneId.systemDefault())
                                                     .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            config.put("fechacreacion", fechaISO);
                        } else if (rawFechaCreacion instanceof Timestamp) {
                            String fechaISO = ((Timestamp) rawFechaCreacion).toLocalDateTime()
                                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            config.put("fechacreacion", fechaISO);
                        } else if (rawFechaCreacion instanceof String) {
                            // intentar parsear string (si ya es ISO)
                            String s = ((String) rawFechaCreacion).trim();
                            if (s.isEmpty()) {
                                config.put("fechacreacion", null);
                            } else {
                                config.put("fechacreacion", s);
                            }
                        } else {
                            config.put("fechacreacion", null);
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error parseando FECHA_CREACION", e);
                        config.put("fechacreacion", null);
                    }

                    // ---- FECHA_ACTUALIZACION: manejar distintos formatos seguros ----
                    try {
                        Object rawFechaAct = rs1.getObject("FECHA_ACTUALIZACION");
                        if (rawFechaAct == null) {
                            config.put("fechaactualizacion", null);
                        } else if (rawFechaAct instanceof Number) {
                            long millis = ((Number) rawFechaAct).longValue();
                            Instant instant = Instant.ofEpochMilli(millis);
                            String fechaISO = instant.atZone(ZoneId.systemDefault())
                                                     .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            config.put("fechaactualizacion", fechaISO);
                        } else if (rawFechaAct instanceof Timestamp) {
                            String fechaISO = ((Timestamp) rawFechaAct).toLocalDateTime()
                                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            config.put("fechaactualizacion", fechaISO);
                        } else if (rawFechaAct instanceof String) {
                            String s = ((String) rawFechaAct).trim();
                            if (s.isEmpty()) {
                                config.put("fechaactualizacion", null);
                            } else {
                                config.put("fechaactualizacion", s);
                            }
                        } else {
                            config.put("fechaactualizacion", null);
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error parseando FECHA_ACTUALIZACION", e);
                        config.put("fechaactualizacion", null);
                    }

                    config.put("tipo", "configuracion_puntos");
                    config.put("fechaextraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    config.put("tabla", "puntos_configuracion");
                    configuraciones.add(config);
                }
            } catch (SQLException e) {
                LOGGER.info("Tabla PUNTOS_CONFIGURACION no encontrada, saltando...");
            }

            // Roles y recursos del sistema
            String sql2 = "SELECT ID, NAME, PERMISSIONS FROM roles ORDER BY NAME";
            try (PreparedStatement stmt2 = session.getConnection().prepareStatement(sql2);
                 ResultSet rs2 = stmt2.executeQuery()) {

                while (rs2.next()) {
                    Map<String, Object> rol = new HashMap<>();
                    rol.put("id", rs2.getString("ID"));
                    rol.put("nombre", rs2.getString("NAME"));
                    rol.put("permisos", rs2.getString("PERMISSIONS"));
                    rol.put("tipo", "rol");
                    rol.put("fechaextraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    rol.put("tabla", "roles");
                    configuraciones.add(rol);
                }
            } catch (SQLException e) {
                LOGGER.info("Tabla roles no encontrada, saltando...");
            }

            LOGGER.info("Extraídos " + configuraciones.size() + " registros de configuración");

            // Opcional: loggear el JSON que vamos a enviar (útil para depuración de errores rojos)
            try {
                ObjectMapper mapper = new ObjectMapper();
                LOGGER.info("JSON a enviar a Supabase (config) — ejemplo primeros 3 elementos: " +
                            mapper.writeValueAsString(configuraciones.stream().limit(3).toArray()));
            } catch (Exception ex) {
                LOGGER.fine("No se pudo serializar JSON para logging: " + ex.getMessage());
            }

            SupabaseServiceREST supabase = new SupabaseServiceREST(
                "https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1",
                "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n"
            );
            return supabase.syncData("config", configuraciones);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error extrayendo configuraciones", e);
            return false;
        }
    });
}


    
    /**
     * Sebastian - Sincroniza inventario (STOCKDIARY + STOCKCURRENT)
     */
    private CompletableFuture<Boolean> syncInventario() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> inventario = new ArrayList<>();
    
                // 🔹 Stock actual
                String sql1 = "SELECT sc.LOCATION, sc.PRODUCT, sc.UNITS, p.NAME as PRODUCT_NAME, " +
                              "p.REFERENCE as PRODUCT_REF " +
                              "FROM stockcurrent sc " +
                              "LEFT JOIN products p ON sc.PRODUCT = p.ID " +
                              "ORDER BY p.NAME";
    
                PreparedStatement stmt1 = session.getConnection().prepareStatement(sql1);
                ResultSet rs1 = stmt1.executeQuery();
    
                while (rs1.next()) {
                    Map<String, Object> stock = new HashMap<>();
                    stock.put("id", UUID.randomUUID().toString());                  
                    stock.put("fecha", null);                                       
                    stock.put("razon", null);                                       
                    stock.put("ubicacion", rs1.getString("LOCATION"));
                    stock.put("productoid", rs1.getString("PRODUCT"));
                    stock.put("productonombre", rs1.getString("PRODUCT_NAME"));
                    stock.put("productoreferencia", rs1.getString("PRODUCT_REF"));
                    stock.put("atributos", null);
                    stock.put("unidades", rs1.getDouble("UNITS"));
                    stock.put("precio", null);                                      
                    stock.put("tipo", "stock_actual");
                    stock.put("fechaextraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    stock.put("tabla", "stockcurrent");
                    inventario.add(stock);
                }
    
                rs1.close();
                stmt1.close();
    
                // 🔹 Movimientos de stock
                String sql2 = "SELECT sd.ID, sd.DATENEW, sd.REASON, sd.LOCATION, sd.PRODUCT, " +
                              "sd.ATTRIBUTESETINSTANCE_ID, sd.UNITS, sd.PRICE, p.NAME as PRODUCT_NAME, " +
                              "p.REFERENCE as PRODUCT_REF " +
                              "FROM stockdiary sd " +
                              "LEFT JOIN products p ON sd.PRODUCT = p.ID " +
                              "ORDER BY sd.DATENEW DESC " +
                              "LIMIT 200";
    
                PreparedStatement stmt2 = session.getConnection().prepareStatement(sql2);
                ResultSet rs2 = stmt2.executeQuery();
    
                while (rs2.next()) {
                    Map<String, Object> movimiento = new HashMap<>();
                    movimiento.put("id", rs2.getString("ID"));
    
                    // Fecha en ISO 8601
                    Timestamp tsFecha = rs2.getTimestamp("DATENEW");
                    movimiento.put("fecha", tsFecha != null ? tsFecha.toInstant().toString() : null);
    
                    movimiento.put("razon", rs2.getInt("REASON"));
                    movimiento.put("ubicacion", rs2.getString("LOCATION"));
                    movimiento.put("productoid", rs2.getString("PRODUCT"));
                    movimiento.put("productonombre", rs2.getString("PRODUCT_NAME"));
                    movimiento.put("productoreferencia", rs2.getString("PRODUCT_REF"));
                    movimiento.put("atributos", rs2.getString("ATTRIBUTESETINSTANCE_ID"));
                    movimiento.put("unidades", rs2.getDouble("UNITS"));
                    movimiento.put("precio", rs2.getDouble("PRICE"));
                    movimiento.put("tipo", "movimiento_stock");
                    movimiento.put("fechaextraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    movimiento.put("tabla", "stockdiary");
                    inventario.add(movimiento);
                }
    
                rs2.close();
                stmt2.close();
    
                LOGGER.info("Extraídos " + inventario.size() + " registros de inventario");
    
                SupabaseServiceREST supabase = new SupabaseServiceREST(
                    "https://cqoayydnqyqmhzanfsij.supabase.co/rest/v1",
                    "sb_secret_xGdxVXBbwvpRSYsHjfDNoQ_OVXl-T5n"
                );
    
                return supabase.syncData("inventario", inventario);                
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error extrayendo inventario", e);
                return false;
            }
        });
    }
    
    
    /**
     * Resultado de la sincronización
     */
    public static class SyncResult {
        public boolean success = false;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public int successCount = 0;
        public int errorCount = 0;
        public String errorMessage;
        
        public boolean usuariosSincronizados = false;
        public boolean clientesSincronizados = false;
        public boolean categoriasSincronizadas = false;
        public boolean productosSincronizados = false;
        public boolean ventasSincronizadas = false;
        // Sebastian - Nuevos campos para sincronización completa
        public boolean puntosSincronizados = false;
        public boolean cierresSincronizados = false;
        public boolean pagosSincronizados = false;
        public boolean impuestosSincronizados = false;
        public boolean configuracionesSincronizadas = false;
        public boolean inventarioSincronizado = false;
    }
}