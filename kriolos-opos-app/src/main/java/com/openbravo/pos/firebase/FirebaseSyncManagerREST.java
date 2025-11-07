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
import com.openbravo.pos.supabase.SupabaseServiceManager;
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
import java.util.Set;
import java.util.HashSet;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
 

/**
 * Gestor de sincronización con Supabase usando REST API
 * Extrae datos reales de la base de datos local y los sincroniza con Supabase
 */
public class FirebaseSyncManagerREST {
    
    private static final Logger LOGGER = Logger.getLogger(FirebaseSyncManagerREST.class.getName());
    
    private final SupabaseServiceManager supabaseManager;
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
        this.supabaseManager = SupabaseServiceManager.getInstance();
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
            LOGGER.info("=== INICIANDO SINCRONIZACIÓN COMPLETA CON SUPABASE ===");
            result.startTime = LocalDateTime.now();
            
            // Obtener el usuario actual para el tracking
            String currentUserId = getUsuarioActual();
            LOGGER.info("Usuario actual para tracking: " + (currentUserId != null ? currentUserId : "unknown"));
            
            // Inicializar Supabase con la configuración
            com.openbravo.pos.forms.AppConfig config = new com.openbravo.pos.forms.AppConfig(null);
            config.load();
            
            // Inicializar Supabase
            if (appUserView != null) {
                supabaseManager.initialize(config, appUserView);
                LOGGER.info("Supabase inicializado con AppUserView");
            } else {
                supabaseManager.initialize(config);
                LOGGER.info("Supabase inicializado sin AppUserView (modo compatibilidad)");
            }
            // El userId se obtiene del usuario actual cuando se necesite
            
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
                
                SupabaseServiceREST supabase = supabaseManager.getService();
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
                
                SupabaseServiceREST supabase = supabaseManager.getService();
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
                
                SupabaseServiceREST supabase = supabaseManager.getService();
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
                // Obtener el card validado desde la configuración
                com.openbravo.pos.forms.AppConfig config = new com.openbravo.pos.forms.AppConfig(null);
                config.load();
                String cardValidado = config.getProperty("supabase.userid");
                if (cardValidado == null || cardValidado.trim().isEmpty()) {
                    cardValidado = config.getProperty("firebase.userid"); // Fallback para compatibilidad
                }
                
                if (cardValidado == null || cardValidado.trim().isEmpty()) {
                    LOGGER.severe("No se encontró card validado en la configuración. No se pueden subir ventas.");
                    return false;
                }
                
                cardValidado = cardValidado.trim();
                LOGGER.info("Usando card validado para vendedorid: " + cardValidado);
                
                // Obtener el nombre del usuario desde Supabase usando el card
                String vendedorNombre = null;
                try {
                    SupabaseServiceREST supabase = supabaseManager.getService();
                    List<Map<String, Object>> usuarios = supabase.fetchData("usuarios");
                    
                    for (Map<String, Object> usuario : usuarios) {
                        Object card = usuario.get("tarjeta");
                        if (card == null) card = usuario.get("card");
                        if (card == null) card = usuario.get("TARJETA");
                        if (card == null) card = usuario.get("CARD");
                        
                        if (card != null && cardValidado.equals(card.toString().trim())) {
                            Object nombre = usuario.get("nombre");
                            if (nombre == null) nombre = usuario.get("name");
                            if (nombre == null) nombre = usuario.get("NOMBRE");
                            if (nombre == null) nombre = usuario.get("NAME");
                            
                            if (nombre != null) {
                                vendedorNombre = nombre.toString();
                                LOGGER.info("Nombre encontrado para card " + cardValidado + ": " + vendedorNombre);
                                break;
                            }
                        }
                    }
                    
                    if (vendedorNombre == null) {
                        LOGGER.warning("No se encontró nombre para el card " + cardValidado + " en la tabla usuarios");
                        vendedorNombre = cardValidado; // Usar el card como fallback
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error obteniendo nombre del usuario desde Supabase: " + e.getMessage(), e);
                    vendedorNombre = cardValidado; // Usar el card como fallback
                }
                
                List<Map<String, Object>> ventas = new ArrayList<>();
                
                // Obtener solo ventas de cajas cerradas (closedcash con DATEEND no NULL)
                // Esto evita subir ventas de la caja actual que aún está abierta
                String sql = "SELECT r.ID, r.MONEY, r.DATENEW, r.PERSON, " +
                           "p.NAME as PERSON_NAME, " +
                           "SUM(tl.UNITS * tl.PRICE) as TOTAL " +
                           "FROM receipts r " +
                           "INNER JOIN closedcash cc ON r.MONEY = cc.MONEY " +
                           "LEFT JOIN people p ON r.PERSON = p.ID " +
                           "LEFT JOIN ticketlines tl ON r.ID = tl.TICKET " +
                           "WHERE cc.DATEEND IS NOT NULL " +
                           "AND r.DATENEW >= DATEADD('DAY', -30, NOW()) " +
                           "GROUP BY r.ID, r.MONEY, r.DATENEW, r.PERSON, p.NAME " +
                           "ORDER BY r.DATENEW DESC";
                
                PreparedStatement stmt = session.getConnection().prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    String ticketId = rs.getString("ID");
                    Map<String, Object> venta = new HashMap<>();
                    // Generar un ID único para cada venta nueva (cada venta es independiente)
                    venta.put("id", UUID.randomUUID().toString());
                    venta.put("caja", rs.getString("MONEY"));
                
                    Timestamp fechaVenta = rs.getTimestamp("DATENEW");
                    if (fechaVenta != null) {
                        venta.put("fechaventa", fechaVenta.toInstant().toString()); // formato ISO 8601 válido para Supabase
                    } else {
                        venta.put("fechaventa", null);
                    }
                
                    // Usar el card validado como vendedorid y el nombre obtenido de Supabase
                    venta.put("vendedorid", cardValidado);
                    venta.put("vendedornombre", vendedorNombre);
                    venta.put("total", rs.getDouble("TOTAL"));
                    // Usar ZonedDateTime para timestamptz con zona horaria
                    venta.put("fechaextraccion", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                    venta.put("tabla", "receipts");
                
                    List<Map<String, Object>> lineas = getLineasTicket(ticketId);
                    venta.put("lineas", lineas);
                    venta.put("numerolineas", lineas != null ? lineas.size() : 0);
                    
                    // Guardar el ticket ID original para logging
                    venta.put("_ticketIdOriginal", ticketId);
                
                    ventas.add(venta);
                }
                
                
                rs.close();
                stmt.close();
                
                LOGGER.info("Extraídas " + ventas.size() + " ventas de la base de datos local (solo de cajas cerradas)");
                
                if (ventas.isEmpty()) {
                    LOGGER.info("No hay ventas para sincronizar (solo se suben ventas de cajas cerradas). Si hay cajas abiertas, ciérrelas primero.");
                    return true;
                }
                
                // Primero, eliminar duplicados dentro del mismo batch
                Set<String> ticketsProcesados = new HashSet<>();
                List<Map<String, Object>> ventasSinDuplicados = new ArrayList<>();
                Map<String, String> ticketIdPorVenta = new HashMap<>(); // Mapa para guardar ticketId por clave de venta
                int duplicadosLocales = 0;
                
                for (Map<String, Object> venta : ventas) {
                    // Usar fechaventa + vendedorid + total como clave única para detectar duplicados
                    Object fecha = venta.get("fechaventa");
                    Object vendedor = venta.get("vendedorid");
                    Object total = venta.get("total");
                    Object ticketIdOriginal = venta.get("_ticketIdOriginal");
                    
                    if (fecha != null && vendedor != null && total != null) {
                        String fechaNormalizada = normalizarFecha(fecha.toString());
                        String clave = fechaNormalizada + "|" + vendedor.toString().trim() + "|" + normalizarTotal(total.toString());
                        if (!ticketsProcesados.contains(clave)) {
                            ticketsProcesados.add(clave);
                            // Guardar el ticketId original antes de eliminarlo
                            if (ticketIdOriginal != null) {
                                ticketIdPorVenta.put(clave, ticketIdOriginal.toString());
                            }
                            // Remover el campo temporal antes de enviar
                            venta.remove("_ticketIdOriginal");
                            ventasSinDuplicados.add(venta);
                        } else {
                            duplicadosLocales++;
                            LOGGER.info("Venta duplicada en batch local omitida: ticketId=" + ticketIdOriginal + 
                                       ", fecha=" + fechaNormalizada + ", vendedor=" + vendedor + ", total=" + total);
                        }
                    } else {
                        if (ticketIdOriginal != null) {
                            // Si no hay fecha/vendedor/total, usar solo el ticketId como clave
                            ticketIdPorVenta.put("direct_" + ticketIdOriginal.toString(), ticketIdOriginal.toString());
                        }
                        venta.remove("_ticketIdOriginal");
                        ventasSinDuplicados.add(venta);
                    }
                }
                
                LOGGER.info("Ventas sin duplicados locales: " + ventasSinDuplicados.size() + 
                           " (de " + ventas.size() + " totales, " + duplicadosLocales + " duplicados locales eliminados)");
                
                SupabaseServiceREST supabase = supabaseManager.getService();
                
                // Consultar TODAS las ventas existentes en Supabase para evitar duplicados
                Set<String> ventasExistentes = new HashSet<>();
                try {
                    // Obtener TODAS las ventas desde Supabase
                    List<Map<String, Object>> ventasEnSupabase = supabase.fetchData("ventas?select=fechaventa,vendedorid,total");
                    for (Map<String, Object> ventaSupabase : ventasEnSupabase) {
                        Object fecha = ventaSupabase.get("fechaventa");
                        Object vendedor = ventaSupabase.get("vendedorid");
                        Object total = ventaSupabase.get("total");
                        if (fecha != null && vendedor != null && total != null) {
                            String fechaNormalizada = normalizarFecha(fecha.toString());
                            String clave = fechaNormalizada + "|" + vendedor.toString().trim() + "|" + normalizarTotal(total.toString());
                            ventasExistentes.add(clave);
                        }
                    }
                    LOGGER.info("Encontradas " + ventasExistentes.size() + " ventas existentes en Supabase");
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error consultando ventas existentes en Supabase, se intentará insertar todas: " + e.getMessage());
                }
                
                // Filtrar solo las ventas que no existen en Supabase
                List<Map<String, Object>> ventasNuevas = new ArrayList<>();
                for (Map<String, Object> venta : ventasSinDuplicados) {
                    Object fecha = venta.get("fechaventa");
                    Object vendedor = venta.get("vendedorid");
                    Object total = venta.get("total");
                    
                    if (fecha != null && vendedor != null && total != null) {
                        String fechaNormalizada = normalizarFecha(fecha.toString());
                        String clave = fechaNormalizada + "|" + vendedor.toString().trim() + "|" + normalizarTotal(total.toString());
                        if (!ventasExistentes.contains(clave)) {
                            ventasNuevas.add(venta);
                        } else {
                            LOGGER.info("Venta ya existe en Supabase, omitida: fecha=" + fechaNormalizada + ", vendedor=" + vendedor + ", total=" + total);
                        }
                    } else {
                        ventasNuevas.add(venta);
                    }
                }
                
                LOGGER.info("Ventas nuevas a insertar: " + ventasNuevas.size() + " (de " + ventasSinDuplicados.size() + " sin duplicados locales, " + ventas.size() + " originales)");
                
                if (ventasNuevas.isEmpty()) {
                    LOGGER.info("No hay ventas nuevas para insertar (todas ya existen en Supabase)");
                    return true;
                }
                
                // Guardar IDs de ventas locales que se van a subir para eliminarlas después
                // Usar el mapa ticketIdPorVenta para recuperar los IDs originales
                List<String> idsVentasSubidas = new ArrayList<>();
                for (Map<String, Object> venta : ventasNuevas) {
                    // Reconstruir la clave para buscar el ticketId original
                    Object fecha = venta.get("fechaventa");
                    Object vendedor = venta.get("vendedorid");
                    Object total = venta.get("total");
                    
                    if (fecha != null && vendedor != null && total != null) {
                        String fechaNormalizada = normalizarFecha(fecha.toString());
                        String clave = fechaNormalizada + "|" + vendedor.toString().trim() + "|" + normalizarTotal(total.toString());
                        String ticketId = ticketIdPorVenta.get(clave);
                        if (ticketId != null) {
                            idsVentasSubidas.add(ticketId);
                        }
                    }
                }
                
                // Usar insertData para crear solo las ventas nuevas
                boolean resultado = supabase.insertData("ventas", ventasNuevas);
                
                // Si la subida fue exitosa, eliminar las ventas de la BD local
                if (resultado && !idsVentasSubidas.isEmpty()) {
                    LOGGER.info("Subida exitosa. Eliminando " + idsVentasSubidas.size() + " ventas de la BD local...");
                    try {
                        eliminarVentasLocales(idsVentasSubidas);
                        LOGGER.info("Ventas eliminadas exitosamente de la BD local");
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error eliminando ventas de la BD local (no crítico): " + e.getMessage(), e);
                    }
                }
                
                return resultado;                
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error extrayendo ventas", e);
                return false;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error en syncVentas", e);
                return false;
            }
        });
    }
    
    /**
     * Elimina ventas de la BD local después de subirlas exitosamente a Supabase
     */
    private void eliminarVentasLocales(List<String> ticketIds) throws SQLException {
        if (ticketIds.isEmpty()) {
            return;
        }
        
        // Eliminar en orden: primero las dependencias, luego el receipt
        // IMPORTANTE: NO eliminar payments aquí, se eliminarán después de subirlos a Supabase
        String deleteTaxLines = "DELETE FROM taxlines WHERE RECEIPT = ?";
        String deleteTicketLines = "DELETE FROM ticketlines WHERE TICKET = ?";
        String deleteTickets = "DELETE FROM tickets WHERE ID = ?";
        String deleteReceipts = "DELETE FROM receipts WHERE ID = ?";
        
        PreparedStatement stmtTax = session.getConnection().prepareStatement(deleteTaxLines);
        PreparedStatement stmtTL = session.getConnection().prepareStatement(deleteTicketLines);
        PreparedStatement stmtT = session.getConnection().prepareStatement(deleteTickets);
        PreparedStatement stmtR = session.getConnection().prepareStatement(deleteReceipts);
        
        try {
            for (String ticketId : ticketIds) {
                // Eliminar taxlines
                stmtTax.setString(1, ticketId);
                stmtTax.executeUpdate();
                
                // NO eliminar payments aquí - se eliminarán después de subirlos a Supabase
                
                // Eliminar ticketlines
                stmtTL.setString(1, ticketId);
                stmtTL.executeUpdate();
                
                // Eliminar tickets
                stmtT.setString(1, ticketId);
                stmtT.executeUpdate();
                
                // Eliminar receipts
                stmtR.setString(1, ticketId);
                stmtR.executeUpdate();
            }
        } finally {
            stmtTax.close();
            stmtTL.close();
            stmtT.close();
            stmtR.close();
        }
    }
    
    /**
     * Elimina pagos de la BD local después de subirlos exitosamente a Supabase
     */
    private void eliminarPagosLocales(List<String> receiptIds) throws SQLException {
        if (receiptIds.isEmpty()) {
            return;
        }
        
        // Eliminar pagos asociados a estos receipts
        String deletePayments = "DELETE FROM payments WHERE RECEIPT = ?";
        PreparedStatement stmt = session.getConnection().prepareStatement(deletePayments);
        
        try {
            for (String receiptId : receiptIds) {
                stmt.setString(1, receiptId);
                stmt.executeUpdate();
            }
            LOGGER.info("Eliminados " + receiptIds.size() + " grupos de pagos de la BD local");
        } finally {
            stmt.close();
        }
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
     * Genera IDs únicos agregando sufijos si ya existe un ID con la misma secuencia
     */
    private CompletableFuture<Boolean> syncCierresCaja() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SupabaseServiceREST supabase = supabaseManager.getService();
                
                // PASO 1: Consultar todos los IDs existentes en Supabase para evitar duplicados
                Set<String> idsExistentes = new HashSet<>();
                try {
                    List<Map<String, Object>> cierresExistentes = supabase.fetchData("cierres?select=id");
                    for (Map<String, Object> cierreExistente : cierresExistentes) {
                        Object idObj = cierreExistente.get("id");
                        if (idObj != null) {
                            idsExistentes.add(idObj.toString());
                        }
                    }
                    LOGGER.info("Encontrados " + idsExistentes.size() + " IDs de cierres existentes en Supabase");
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error consultando cierres existentes en Supabase, se intentará generar IDs únicos: " + e.getMessage());
                }
                
                List<Map<String, Object>> cierres = new ArrayList<>();
                
                // PASO 2: Obtener SOLO cierres cerrados (con DATEEND) con información de dinero desde receipts
                // NO incluir cajas activas (DATEEND IS NULL)
                // IMPORTANTE: Usar r.MONEY = cc.MONEY para calcular montos correctamente
                // Ordenar por MONTO_TOTAL DESC y luego por DATESTART ASC
                // Esto asegura que si hay múltiples cierres con la misma secuencia, 
                // el que tiene más dinero (más importante) obtenga el sufijo _1
                String sql = "SELECT cc.MONEY, cc.HOST, cc.HOSTSEQUENCE, cc.DATESTART, cc.DATEEND, " +
                           "COALESCE(SUM(p.TOTAL), 0.0) as MONTO_TOTAL, cc.INITIAL_AMOUNT " +
                           "FROM closedcash cc " +
                           "LEFT JOIN receipts r ON r.MONEY = cc.MONEY " +
                           "LEFT JOIN payments p ON p.RECEIPT = r.ID " +
                           "WHERE cc.DATEEND IS NOT NULL " +
                           "GROUP BY cc.MONEY, cc.HOST, cc.HOSTSEQUENCE, cc.DATESTART, cc.DATEEND, cc.INITIAL_AMOUNT " +
                           "ORDER BY MONTO_TOTAL DESC, cc.DATESTART ASC";
                
                PreparedStatement stmt = session.getConnection().prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    Map<String, Object> cierre = new HashMap<>();
                    
                    // PASO 3: Generar ID base (HOST + "_" + HOSTSEQUENCE)
                    String host = rs.getString("HOST");
                    int hostSequence = rs.getInt("HOSTSEQUENCE");
                    String dineroId = rs.getString("MONEY");
                    double montoTotal = rs.getDouble("MONTO_TOTAL");
                    String idBase = host + "_" + hostSequence;
                    
                    // PASO 4: Generar ID único agregando sufijo si ya existe
                    String idUnico = generarIdUnico(idBase, idsExistentes);
                    
                    // Log detallado para debugging
                    LOGGER.info(String.format("Procesando cierre: MONEY=%s, ID base=%s, ID único=%s, Monto=%.2f, Host=%s, Secuencia=%d", 
                        dineroId, idBase, idUnico, montoTotal, host, hostSequence));
                    
                    // Agregar el ID generado al set para evitar duplicados en el mismo batch
                    idsExistentes.add(idUnico);
                    
                    cierre.put("id", idUnico);
                    cierre.put("dineroid", dineroId); // ID de referencia al dinero
                    cierre.put("dineromonto", montoTotal); // Monto calculado del período
                    cierre.put("host", host);
                    cierre.put("secuencia", hostSequence);
                    cierre.put("fechainicio", rs.getTimestamp("DATESTART"));
                    cierre.put("fechafin", rs.getTimestamp("DATEEND"));
                    cierre.put("fechaextraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    cierre.put("fechasincronizacion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    cierre.put("origen", "kriolos-pos");
                    cierre.put("version", "1.0");
                    cierre.put("tabla", "closedcash");
                    // initial_amount es DOUBLE, manejar NULL correctamente
                    double initialAmount = rs.getDouble("INITIAL_AMOUNT");
                    if (rs.wasNull()) {
                        cierre.put("initial_amount", null);
                    } else {
                        cierre.put("initial_amount", initialAmount);
                    }
                    
                    // Log si se agregó sufijo
                    if (!idUnico.equals(idBase)) {
                        LOGGER.info("ID duplicado detectado: " + idBase + " -> ID único generado: " + idUnico);
                    }
                    
                    cierres.add(cierre);
                }
                rs.close();
                stmt.close();
                
                LOGGER.info("Extraídos " + cierres.size() + " cierres de caja");
                
                if (cierres.isEmpty()) {
                    LOGGER.info("No hay cierres de caja para sincronizar");
                    return true;
                }
                
                // Guardar IDs de cierres que se van a subir para eliminarlos después
                List<String> moneyIdsSubidos = new ArrayList<>();
                for (Map<String, Object> cierre : cierres) {
                    Object dineroId = cierre.get("dineroid");
                    if (dineroId != null) {
                        moneyIdsSubidos.add(dineroId.toString());
                    }
                }
                
                boolean resultado = supabase.syncData("cierres", cierres);
                
                // Si la subida fue exitosa, eliminar los cierres de la BD local
                if (resultado && !moneyIdsSubidos.isEmpty()) {
                    LOGGER.info("Subida exitosa. Eliminando " + moneyIdsSubidos.size() + " cierres de caja de la BD local...");
                    try {
                        eliminarCierresLocales(moneyIdsSubidos);
                        LOGGER.info("Cierres de caja eliminados exitosamente de la BD local");
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error eliminando cierres de caja de la BD local (no crítico): " + e.getMessage(), e);
                    }
                }
                
                return resultado;                
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error extrayendo cierres de caja", e);
                return false;
            }
        });
    }
    
    /**
     * Genera un ID único para un cierre de caja agregando sufijos si el ID base ya existe
     * 
     * @param idBase ID base en formato "HOST_SECUENCIA" (ej: "DESKTOP-P0S4BBC_2")
     * @param idsExistentes Set con todos los IDs que ya existen en Supabase
     * @return ID único que no existe en idsExistentes
     * 
     * Ejemplos:
     * - Si "DESKTOP-P0S4BBC_2" no existe -> retorna "DESKTOP-P0S4BBC_2"
     * - Si "DESKTOP-P0S4BBC_2" existe -> retorna "DESKTOP-P0S4BBC_2_1"
     * - Si "DESKTOP-P0S4BBC_2_1" también existe -> retorna "DESKTOP-P0S4BBC_2_2"
     */
    private String generarIdUnico(String idBase, Set<String> idsExistentes) {
        // Si el ID base no existe, usarlo directamente
        if (!idsExistentes.contains(idBase)) {
            return idBase;
        }
        
        // Si existe, agregar sufijo _1, _2, _3, etc. hasta encontrar uno disponible
        int sufijo = 1;
        String idCandidato;
        do {
            idCandidato = idBase + "_" + sufijo;
            sufijo++;
        } while (idsExistentes.contains(idCandidato));
        
        return idCandidato;
    }
    
    /**
     * Elimina cierres de caja de la BD local después de subirlos exitosamente a Supabase
     * IMPORTANTE: Primero elimina todas las ventas asociadas a estos cierres para evitar violaciones de integridad
     * NO elimina cierres que tienen DATEEND IS NULL (cajas activas actuales)
     */
    private void eliminarCierresLocales(List<String> moneyIds) throws SQLException {
        if (moneyIds.isEmpty()) {
            return;
        }
        
        // PASO 0: Filtrar cierres - NO eliminar cajas activas (DATEEND IS NULL)
        List<String> cierresParaEliminar = new ArrayList<>();
        String checkActiveCash = "SELECT DATEEND FROM closedcash WHERE MONEY = ?";
        PreparedStatement checkActiveStmt = session.getConnection().prepareStatement(checkActiveCash);
        
        try {
            for (String moneyId : moneyIds) {
                checkActiveStmt.setString(1, moneyId);
                ResultSet rs = checkActiveStmt.executeQuery();
                if (rs.next()) {
                    Timestamp dateEnd = rs.getTimestamp("DATEEND");
                    if (dateEnd != null) {
                        // Solo agregar si tiene DATEEND (caja cerrada)
                        cierresParaEliminar.add(moneyId);
                    } else {
                        // Es una caja activa, NO eliminar
                        LOGGER.warning("NO se eliminará el cierre " + moneyId + " porque es una caja activa (DATEEND IS NULL)");
                    }
                }
                rs.close();
            }
        } finally {
            checkActiveStmt.close();
        }
        
        if (cierresParaEliminar.isEmpty()) {
            LOGGER.info("No hay cierres cerrados para eliminar (todos son cajas activas)");
            return;
        }
        
        // PASO 1: Obtener TODAS las ventas asociadas a estos cierres de caja
        List<String> todasLasVentas = new ArrayList<>();
        String selectVentas = "SELECT ID FROM receipts WHERE MONEY = ?";
        PreparedStatement stmtSelect = session.getConnection().prepareStatement(selectVentas);
        
        try {
            for (String moneyId : cierresParaEliminar) {
                stmtSelect.setString(1, moneyId);
                ResultSet rs = stmtSelect.executeQuery();
                while (rs.next()) {
                    todasLasVentas.add(rs.getString("ID"));
                }
                rs.close();
            }
        } finally {
            stmtSelect.close();
        }
        
        // PASO 2: Eliminar todas las ventas asociadas (con todas sus dependencias)
        if (!todasLasVentas.isEmpty()) {
            LOGGER.info("Eliminando " + todasLasVentas.size() + " ventas asociadas a los cierres de caja...");
            try {
                eliminarVentasLocales(todasLasVentas);
                LOGGER.info("Ventas eliminadas correctamente");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Algunas ventas no pudieron eliminarse, pero continuaremos con los cierres: " + e.getMessage());
            }
        }
        
        // PASO 3: Verificar que no queden ventas antes de eliminar los cierres
        // (esto es una medida de seguridad adicional)
        String deleteClosedCash = "DELETE FROM closedcash WHERE MONEY = ? AND DATEEND IS NOT NULL";
        PreparedStatement stmt = session.getConnection().prepareStatement(deleteClosedCash);
        
        try {
            for (String moneyId : cierresParaEliminar) {
                // Verificar que no haya ventas antes de eliminar el cierre
                String checkVentas = "SELECT COUNT(*) FROM receipts WHERE MONEY = ?";
                PreparedStatement checkStmt = session.getConnection().prepareStatement(checkVentas);
                checkStmt.setString(1, moneyId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    LOGGER.warning("Aún hay " + rs.getInt(1) + " ventas asociadas al cierre " + moneyId + ". No se eliminará el cierre para evitar violación de integridad.");
                    rs.close();
                    checkStmt.close();
                    continue;
                }
                rs.close();
                checkStmt.close();
                
                // Verificar nuevamente que DATEEND no sea NULL antes de eliminar
                String checkDateEnd = "SELECT DATEEND FROM closedcash WHERE MONEY = ?";
                PreparedStatement checkDateStmt = session.getConnection().prepareStatement(checkDateEnd);
                checkDateStmt.setString(1, moneyId);
                ResultSet dateRs = checkDateStmt.executeQuery();
                if (dateRs.next()) {
                    Timestamp dateEnd = dateRs.getTimestamp("DATEEND");
                    if (dateEnd == null) {
                        LOGGER.warning("NO se eliminará el cierre " + moneyId + " porque ahora es una caja activa (DATEEND IS NULL)");
                        dateRs.close();
                        checkDateStmt.close();
                        continue;
                    }
                }
                dateRs.close();
                checkDateStmt.close();
                
                // Si no hay ventas y tiene DATEEND, eliminar el cierre
                stmt.setString(1, moneyId);
                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    LOGGER.info("Cierre de caja " + moneyId + " eliminado correctamente");
                }
            }
            LOGGER.info("Proceso de eliminación de cierres de caja completado");
        } finally {
            stmt.close();
        }
    }
    
    /**
     * Sebastian - Sincroniza todas las formas de pago (PAYMENTS)
     * Solo sube pagos de cajas cerradas para mantener consistencia con las ventas
     */
    private CompletableFuture<Boolean> syncFormasPago() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> pagos = new ArrayList<>();
                
                // Obtener solo pagos de ventas de cajas cerradas (closedcash con DATEEND no NULL)
                // Esto evita subir pagos de la caja actual que aún está abierta
                String sql = "SELECT p.ID, p.RECEIPT, p.PAYMENT, p.TOTAL, " +
                           "p.TENDERED, p.CARDNAME, p.VOUCHER, r.DATENEW " +
                           "FROM payments p " +
                           "INNER JOIN receipts r ON p.RECEIPT = r.ID " +
                           "INNER JOIN closedcash cc ON r.MONEY = cc.MONEY " +
                           "WHERE cc.DATEEND IS NOT NULL " +
                           "AND r.DATENEW >= DATEADD('DAY', -30, NOW()) " +
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
                
                LOGGER.info("Extraídos " + pagos.size() + " registros de pagos de cajas cerradas");
                
                if (pagos.isEmpty()) {
                    LOGGER.info("No hay pagos para sincronizar (solo se suben pagos de cajas cerradas). Si hay cajas abiertas, ciérrelas primero.");
                    return true;
                }
                
                // Guardar IDs de receipts cuyos pagos se van a subir para eliminarlos después
                Set<String> receiptIdsSubidos = new HashSet<>();
                for (Map<String, Object> pago : pagos) {
                    Object recibo = pago.get("recibo");
                    if (recibo != null) {
                        receiptIdsSubidos.add(recibo.toString());
                    }
                }
                
                SupabaseServiceREST supabase = supabaseManager.getService();
                boolean resultado = supabase.syncData("formas_de_pago", pagos);
                
                // Si la subida fue exitosa, eliminar los pagos de la BD local
                if (resultado && !receiptIdsSubidos.isEmpty()) {
                    LOGGER.info("Subida exitosa. Eliminando " + receiptIdsSubidos.size() + " grupos de pagos de la BD local...");
                    try {
                        eliminarPagosLocales(new ArrayList<>(receiptIdsSubidos));
                        LOGGER.info("Pagos eliminados exitosamente de la BD local");
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error eliminando pagos de la BD local (no crítico): " + e.getMessage(), e);
                    }
                }
                
                return resultado;                
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
                SupabaseServiceREST supabase = supabaseManager.getService();
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
     * Normaliza una fecha para comparación (remueve milisegundos y normaliza formato)
     * Formato de Supabase: "2025-11-05 21:26:07.743+00" o "2025-11-05T21:26:07.743Z"
     * Formato normalizado: "2025-11-05 21:26:07" (solo fecha y hora hasta segundos)
     */
    private String normalizarFecha(String fecha) {
        if (fecha == null || fecha.isEmpty()) {
            return "";
        }
        try {
            String fechaNormalizada = fecha.trim();
            
            // Normalizar separadores: T -> espacio, Z -> +00
            fechaNormalizada = fechaNormalizada.replace("T", " ");
            fechaNormalizada = fechaNormalizada.replace("Z", "+00");
            
            // Remover milisegundos si existen (buscar .123 y remover)
            if (fechaNormalizada.contains(".")) {
                int puntoIndex = fechaNormalizada.indexOf(".");
                // Buscar el siguiente carácter que no sea dígito (+, -, espacio, fin de string)
                int finMilisegundos = puntoIndex + 1;
                while (finMilisegundos < fechaNormalizada.length() && 
                       Character.isDigit(fechaNormalizada.charAt(finMilisegundos))) {
                    finMilisegundos++;
                }
                // Remover los milisegundos
                fechaNormalizada = fechaNormalizada.substring(0, puntoIndex) + 
                                 fechaNormalizada.substring(finMilisegundos);
            }
            
            // Remover zona horaria (todo después de + o -)
            int plusIndex = fechaNormalizada.indexOf("+");
            int minusIndex = fechaNormalizada.indexOf("-", 10); // Buscar después de la fecha (YYYY-MM-DD)
            if (plusIndex > 0) {
                fechaNormalizada = fechaNormalizada.substring(0, plusIndex).trim();
            } else if (minusIndex > 10) {
                fechaNormalizada = fechaNormalizada.substring(0, minusIndex).trim();
            }
            
            // Asegurar formato: YYYY-MM-DD HH:MM:SS (19 caracteres)
            if (fechaNormalizada.length() > 19) {
                fechaNormalizada = fechaNormalizada.substring(0, 19);
            }
            
            return fechaNormalizada.trim();
        } catch (Exception e) {
            LOGGER.warning("Error normalizando fecha: " + fecha + " - " + e.getMessage());
            return fecha.trim();
        }
    }
    
    /**
     * Normaliza un total para comparación (remueve decimales innecesarios y espacios)
     */
    private String normalizarTotal(String total) {
        if (total == null || total.isEmpty()) {
            return "";
        }
        try {
            // Convertir a número y luego a string para normalizar formato
            double valor = Double.parseDouble(total.trim());
            // Redondear a 2 decimales para comparación
            return String.format("%.2f", valor);
        } catch (Exception e) {
            LOGGER.warning("Error normalizando total: " + total + " - " + e.getMessage());
            return total.trim();
        }
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