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

import com.openbravo.data.loader.Session;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.admin.DataLogicAdmin;
import com.openbravo.pos.forms.DataLogicSales;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
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
    
    public FirebaseSyncManagerREST(Session session) {
        this.session = session;
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
    }
    
    /**
     * Realiza una sincronización completa de todos los datos
     */
    public CompletableFuture<SyncResult> performFullSync() {
        return CompletableFuture.supplyAsync(() -> {
            SyncResult result = new SyncResult();
            
            try {
                LOGGER.info("=== INICIANDO SINCRONIZACIÓN COMPLETA CON FIREBASE ===");
                result.startTime = LocalDateTime.now();
                
                // 1. Sincronizar usuarios
                LOGGER.info("1. Sincronizando usuarios...");
                boolean usuariosOk = syncUsuarios().join();
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
                
                result.endTime = LocalDateTime.now();
                result.success = result.errorCount == 0;
                
                String mensaje = String.format(
                    "=== SINCRONIZACIÓN COMPLETADA ===\n" +
                    "Tiempo: %s\n" +
                    "Exitosas: %d | Errores: %d\n" +
                    "Usuarios: %s | Clientes: %s | Categorías: %s | Productos: %s | Ventas: %s",
                    java.time.Duration.between(result.startTime, result.endTime).toString(),
                    result.successCount, result.errorCount,
                    usuariosOk ? "✓" : "✗",
                    clientesOk ? "✓" : "✗", 
                    categoriasOk ? "✓" : "✗",
                    productosOk ? "✓" : "✗",
                    ventasOk ? "✓" : "✗"
                );
                
                LOGGER.info(mensaje);
                
                return result;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error en sincronización completa", e);
                result.endTime = LocalDateTime.now();
                result.success = false;
                result.errorMessage = e.getMessage();
                return result;
            }
        });
    }
    
    /**
     * Sincroniza usuarios desde la tabla people
     */
    private CompletableFuture<Boolean> syncUsuarios() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> usuarios = new ArrayList<>();
                
                String sql = "SELECT ID, NAME, CARD, ROLE, VISIBLE, IMAGE " +
                           "FROM people WHERE VISIBLE = true";
                
                PreparedStatement stmt = session.getConnection().prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    Map<String, Object> usuario = new HashMap<>();
                    usuario.put("id", rs.getString("ID"));
                    usuario.put("nombre", rs.getString("NAME"));
                    usuario.put("tarjeta", rs.getString("CARD"));
                    usuario.put("rol", rs.getString("ROLE"));
                    usuario.put("visible", rs.getBoolean("VISIBLE"));
                    usuario.put("tieneImagen", rs.getBytes("IMAGE") != null);
                    usuario.put("fechaExtraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    usuario.put("tabla", "people");
                    
                    usuarios.add(usuario);
                }
                
                rs.close();
                stmt.close();
                
                LOGGER.info("Extraídos " + usuarios.size() + " usuarios de la base de datos local");
                
                return firebaseService.syncUsuarios(usuarios).join();
                
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error extrayendo usuarios", e);
                return false;
            }
        });
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
                    cliente.put("codigoBusqueda", rs.getString("SEARCHKEY"));
                    cliente.put("numeroIdentificacion", rs.getString("TAXID"));
                    cliente.put("nombre", rs.getString("NAME"));
                    cliente.put("tarjeta", rs.getString("CARD"));
                    cliente.put("categoriaImpuesto", rs.getString("TAXCATEGORY"));
                    cliente.put("primerNombre", rs.getString("FIRSTNAME"));
                    cliente.put("apellido", rs.getString("LASTNAME"));
                    cliente.put("email", rs.getString("EMAIL"));
                    cliente.put("telefono", rs.getString("PHONE"));
                    cliente.put("telefono2", rs.getString("PHONE2"));
                    cliente.put("direccion", rs.getString("ADDRESS"));
                    cliente.put("direccion2", rs.getString("ADDRESS2"));
                    cliente.put("codigoPostal", rs.getString("POSTAL"));
                    cliente.put("ciudad", rs.getString("CITY"));
                    cliente.put("region", rs.getString("REGION"));
                    cliente.put("pais", rs.getString("COUNTRY"));
                    
                    // Sin fecha de registro - campo CURDATE no existe en la tabla
                    cliente.put("fechaRegistro", null);
                    
                    cliente.put("deudaActual", rs.getDouble("CURDEBT"));
                    cliente.put("deudaMaxima", rs.getDouble("MAXDEBT"));
                    cliente.put("visible", rs.getBoolean("VISIBLE"));
                    cliente.put("fechaExtraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    cliente.put("tabla", "customers");
                    
                    clientes.add(cliente);
                }
                
                rs.close();
                stmt.close();
                
                LOGGER.info("Extraídos " + clientes.size() + " clientes de la base de datos local");
                
                return firebaseService.syncClientes(clientes).join();
                
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
                    categoria.put("categoriaPadre", rs.getString("PARENTID"));
                    categoria.put("tieneImagen", rs.getBytes("IMAGE") != null);
                    categoria.put("fechaExtraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    categoria.put("tabla", "categories");
                    
                    categorias.add(categoria);
                }
                
                rs.close();
                stmt.close();
                
                LOGGER.info("Extraídas " + categorias.size() + " categorías de la base de datos local");
                
                return firebaseService.syncCategorias(categorias).join();
                
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
                    producto.put("tipoCodigoBarras", rs.getString("CODETYPE"));
                    producto.put("nombre", rs.getString("NAME"));
                    producto.put("precioCompra", rs.getDouble("PRICEBUY"));
                    producto.put("precioVenta", rs.getDouble("PRICESELL"));
                    producto.put("categoriaId", rs.getString("CATEGORY"));
                    producto.put("categoriaNombre", rs.getString("CATEGORY_NAME"));
                    producto.put("categoriaImpuesto", rs.getString("TAXCAT"));
                    producto.put("categoriaImpuestoNombre", rs.getString("TAXCAT_NAME"));
                    producto.put("atributos", rs.getString("ATTRIBUTESET_ID"));
                    producto.put("tieneImagen", rs.getBytes("IMAGE") != null);
                    producto.put("esCompuesto", rs.getBoolean("ISCOM"));
                    producto.put("imprimirEnCocina", rs.getBoolean("PRINTKB"));
                    producto.put("estadoEnvio", rs.getBoolean("SENDSTATUS"));
                    producto.put("fechaExtraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    producto.put("tabla", "products");
                    
                    productos.add(producto);
                }
                
                rs.close();
                stmt.close();
                
                LOGGER.info("Extraídos " + productos.size() + " productos de la base de datos local");
                
                return firebaseService.syncProductos(productos).join();
                
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
                    venta.put("fechaVenta", rs.getTimestamp("DATENEW"));
                    venta.put("vendedorId", rs.getString("PERSON"));
                    venta.put("vendedorNombre", rs.getString("PERSON_NAME"));
                    venta.put("total", rs.getDouble("TOTAL"));
                    venta.put("fechaExtraccion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    venta.put("tabla", "receipts");
                    
                    // Obtener líneas del ticket
                    List<Map<String, Object>> lineas = getLineasTicket(rs.getString("ID"));
                    venta.put("lineas", lineas);
                    venta.put("numeroLineas", lineas.size());
                    
                    ventas.add(venta);
                }
                
                rs.close();
                stmt.close();
                
                LOGGER.info("Extraídas " + ventas.size() + " ventas de la base de datos local");
                
                return firebaseService.syncVentas(ventas).join();
                
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
                linea.put("numeroLinea", rs.getInt("LINE"));
                linea.put("productoId", rs.getString("PRODUCT"));
                linea.put("productoNombre", rs.getString("PRODUCT_NAME"));
                linea.put("productoReferencia", rs.getString("PRODUCT_REF"));
                linea.put("atributos", rs.getString("ATTRIBUTESETINSTANCE_ID"));
                linea.put("cantidad", rs.getDouble("UNITS"));
                linea.put("precio", rs.getDouble("PRICE"));
                linea.put("impuestoId", rs.getString("TAXID"));
                
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
    }
}