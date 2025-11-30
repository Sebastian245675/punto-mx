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
import com.openbravo.pos.supabase.SupabaseServiceREST;
import com.openbravo.pos.supabase.SupabaseServiceManager;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.forms.AppConfig;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.math.BigDecimal;

/**
 * Gestor de descarga de datos desde Supabase hacia la base de datos local
 * @author Sebastian
 */
public class FirebaseDownloadManagerREST {
    
    private static final Logger LOGGER = Logger.getLogger(FirebaseDownloadManagerREST.class.getName());
    
    private final Session session;
    private final SupabaseServiceManager supabaseManager;
    
    public FirebaseDownloadManagerREST(Session session, AppConfig config) {
        this.session = session;
        this.supabaseManager = SupabaseServiceManager.getInstance();
        
        // Inicializar el servicio Supabase
        if (!supabaseManager.initialize(config)) {
            throw new IllegalStateException("No se pudo inicializar Supabase Service");
        }
    }

    private static java.sql.Timestamp toSqlTimestamp(Object value) {
        try {
            if (value == null) return null;
            if (value instanceof java.sql.Timestamp) {
                return (java.sql.Timestamp) value;
            }
            if (value instanceof java.util.Date) {
                return new java.sql.Timestamp(((java.util.Date) value).getTime());
            }
            if (value instanceof Number) {
                long millis = ((Number) value).longValue();
                return new java.sql.Timestamp(millis);
            }
            if (value instanceof CharSequence) {
                String s = value.toString().trim();
                if (s.isEmpty()) return null;
                // Try ISO-8601: 2025-10-29T14:41:07 or with zone suffix
                try {
                    java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(s);
                    return java.sql.Timestamp.from(odt.toInstant());
                } catch (Exception ignore) {}
                try {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s);
                    return java.sql.Timestamp.valueOf(ldt);
                } catch (Exception ignore) {}
                // Fallback: parse as millis in string
                try {
                    long millis = Long.parseLong(s);
                    return new java.sql.Timestamp(millis);
                } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            // ignore and return null
        }
        return null;
    }
    
    /**
     * Ejecuta la descarga completa según las selecciones especificadas
     */
    public CompletableFuture<DownloadResult> performSelectedDownload(Map<String, Boolean> selections) {
        return CompletableFuture.supplyAsync(() -> {
            Instant startTime = Instant.now();
            LOGGER.info("=== INICIANDO DESCARGA SELECTIVA DESDE FIREBASE ===");
            
            DownloadResult result = new DownloadResult();
            
            try {
                // Descargar cada categoría seleccionada
                if (selections.getOrDefault("usuarios", false)) {
                    LOGGER.info("1. Descargando usuarios...");
                    result.usuariosDescargados = downloadUsuarios().join();
                }
                
                if (selections.getOrDefault("clientes", false)) {
                    LOGGER.info("2. Descargando clientes...");
                    result.clientesDescargados = downloadClientes().join();
                }
                
                if (selections.getOrDefault("categorias", false)) {
                    LOGGER.info("3. Descargando categorías...");
                    result.categoriasDescargadas = downloadCategorias().join();
                }
                
                if (selections.getOrDefault("productos", false)) {
                    LOGGER.info("4. Descargando productos...");
                    result.productosDescargados = downloadProductos().join();
                }
                
                if (selections.getOrDefault("ventas", false)) {
                    LOGGER.info("5. Descargando ventas...");
                    result.ventasDescargadas = downloadVentas().join();
                }
                
                if (selections.getOrDefault("puntos", false)) {
                    LOGGER.info("6. Descargando puntos de clientes...");
                    result.puntosDescargados = downloadPuntosClientes().join();
                }
                
                if (selections.getOrDefault("cierres", false)) {
                    LOGGER.info("7. Descargando cierres de caja...");
                    result.cierresDescargados = downloadCierresCaja().join();
                }
                
                if (selections.getOrDefault("pagos", false)) {
                    LOGGER.info("8. Descargando formas de pago...");
                    result.pagosDescargados = downloadFormasPago().join();
                }
                
                if (selections.getOrDefault("impuestos", false)) {
                    LOGGER.info("9. Descargando impuestos...");
                    result.impuestosDescargados = downloadImpuestos().join();
                }
                
                if (selections.getOrDefault("configuraciones", false)) {
                    LOGGER.info("10. Descargando configuraciones...");
                    result.configuracionesDescargadas = downloadConfiguraciones().join();
                }
                
                if (selections.getOrDefault("inventario", false)) {
                    LOGGER.info("11. Descargando inventario...");
                    result.inventarioDescargado = downloadInventario().join();
                }
                
                // Calcular estadísticas finales
                Duration duration = Duration.between(startTime, Instant.now());
                int exitosas = 0;
                int errores = 0;
                
                if (result.usuariosDescargados) exitosas++; else if (selections.getOrDefault("usuarios", false)) errores++;
                if (result.clientesDescargados) exitosas++; else if (selections.getOrDefault("clientes", false)) errores++;
                if (result.categoriasDescargadas) exitosas++; else if (selections.getOrDefault("categorias", false)) errores++;
                if (result.productosDescargados) exitosas++; else if (selections.getOrDefault("productos", false)) errores++;
                if (result.ventasDescargadas) exitosas++; else if (selections.getOrDefault("ventas", false)) errores++;
                if (result.puntosDescargados) exitosas++; else if (selections.getOrDefault("puntos", false)) errores++;
                if (result.cierresDescargados) exitosas++; else if (selections.getOrDefault("cierres", false)) errores++;
                if (result.pagosDescargados) exitosas++; else if (selections.getOrDefault("pagos", false)) errores++;
                if (result.impuestosDescargados) exitosas++; else if (selections.getOrDefault("impuestos", false)) errores++;
                if (result.configuracionesDescargadas) exitosas++; else if (selections.getOrDefault("configuraciones", false)) errores++;
                if (result.inventarioDescargado) exitosas++; else if (selections.getOrDefault("inventario", false)) errores++;
                
                result.success = (errores == 0);
                
                LOGGER.info("=== DESCARGA SELECTIVA COMPLETADA ===");
                LOGGER.info("Tiempo: " + duration.toString());
                LOGGER.info("Exitosas: " + exitosas + " | Errores: " + errores);
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error durante la descarga selectiva", e);
                result.success = false;
            }
            
            return result;
        });
    }
    
    /**
     * Descarga usuarios desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadUsuarios() {
        return CompletableFuture.supplyAsync(() -> {
            PreparedStatement checkStmt = null;
            PreparedStatement insertStmt = null;
            PreparedStatement updateStmt = null;
            java.sql.ResultSet rs = null;
            
            try {
                SupabaseServiceREST supabase = supabaseManager.getService();
                
                List<Map<String, Object>> usuarios = supabase.fetchData("usuarios");
                LOGGER.info("Descargados " + usuarios.size() + " usuarios desde Supabase");
                
                int insertados = 0;
                int actualizados = 0;
                int errores = 0;
                
                // Preparar statements
                String checkSql = "SELECT ID FROM people WHERE ID = ?";
                String insertSql = "INSERT INTO people (ID, NAME, APPPASSWORD, CARD, ROLE, VISIBLE, IMAGE, BRANCH_NAME, BRANCH_ADDRESS) " +
                                  "VALUES (?, ?, NULL, ?, ?, ?, NULL, ?, ?)";
                String updateSql = "UPDATE people SET NAME = ?, CARD = ?, ROLE = ?, VISIBLE = ?, BRANCH_NAME = ?, BRANCH_ADDRESS = ? WHERE ID = ?";
                
                checkStmt = session.getConnection().prepareStatement(checkSql);
                insertStmt = session.getConnection().prepareStatement(insertSql);
                updateStmt = session.getConnection().prepareStatement(updateSql);
                
                for (Map<String, Object> usuario : usuarios) {
                    try {
                        String id = (String) usuario.get("id");
                        String nombre = (String) usuario.get("nombre");
                        String tarjeta = (String) usuario.get("tarjeta");
                        String rol = (String) usuario.get("rol");
                        Boolean visible = (Boolean) usuario.get("visible");
                        String sucursalNombre = (String) usuario.get("sucursal_nombre");
                        String sucursalDireccion = (String) usuario.get("sucursal_direccion");
                        
                        // Validar que tenga ID
                        if (id == null || id.trim().isEmpty()) {
                            LOGGER.warning("Usuario sin ID, saltando: " + usuario);
                            errores++;
                            continue;
                        }
                        
                        // Verificar si existe
                        checkStmt.setString(1, id);
                        rs = checkStmt.executeQuery();
                        boolean existe = rs.next();
                        rs.close();
                        
                        if (existe) {
                            // Actualizar usuario existente
                            updateStmt.setString(1, nombre != null ? nombre : "Usuario sin nombre");
                            updateStmt.setString(2, tarjeta); // Puede ser null
                            updateStmt.setString(3, rol != null ? rol : "3"); // Role por defecto: Guest
                            updateStmt.setBoolean(4, visible != null ? visible : true);
                            updateStmt.setString(5, sucursalNombre);
                            updateStmt.setString(6, sucursalDireccion);
                            updateStmt.setString(7, id);
                            updateStmt.executeUpdate();
                            actualizados++;
                            LOGGER.fine("Usuario actualizado: " + id + " - " + nombre + " (ID Remoto: " + tarjeta + ")");
                        } else {
                            // Insertar nuevo usuario
                            insertStmt.setString(1, id);
                            insertStmt.setString(2, nombre != null ? nombre : "Usuario sin nombre");
                            insertStmt.setString(3, tarjeta); // Puede ser null
                            insertStmt.setString(4, rol != null ? rol : "3"); // Role por defecto: Guest
                            insertStmt.setBoolean(5, visible != null ? visible : true);
                            insertStmt.setString(6, sucursalNombre);
                            insertStmt.setString(7, sucursalDireccion);
                            insertStmt.executeUpdate();
                            insertados++;
                            LOGGER.fine("Usuario insertado: " + id + " - " + nombre + " (ID Remoto: " + tarjeta + ")");
                        }
                        
                    } catch (Exception e) {
                        errores++;
                        LOGGER.log(Level.WARNING, "Error procesando usuario: " + usuario, e);
                    }
                }
                
                LOGGER.info("Usuarios procesados: " + insertados + " insertados, " + 
                           actualizados + " actualizados, " + errores + " errores");
                return errores == 0;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando usuarios", e);
                return false;
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (checkStmt != null) checkStmt.close();
                    if (insertStmt != null) insertStmt.close();
                    if (updateStmt != null) updateStmt.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error cerrando recursos", e);
                }
            }
        });
    }
    
    /**
     * Descarga clientes desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadClientes() {
        return CompletableFuture.supplyAsync(() -> {
            PreparedStatement checkStmt = null;
            PreparedStatement insertStmt = null;
            PreparedStatement updateStmt = null;
            java.sql.ResultSet rs = null;
    
            try {
                SupabaseServiceREST supabase = supabaseManager.getService();
                List<Map<String, Object>> clientes = supabase.fetchData("clientes");
                LOGGER.info("Descargados " + clientes.size() + " clientes desde Supabase");
    
                int insertados = 0;
                int actualizados = 0;
                int errores = 0;
    
                String checkSql = "SELECT ID FROM customers WHERE ID = ?";
                String insertSql = "INSERT INTO customers (ID, SEARCHKEY, TAXID, NAME, CARD, TAXCATEGORY, " +
                                   "NOTES, VISIBLE, CURDATE, CURDEBT, MAXDEBT) " +
                                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                String updateSql = "UPDATE customers SET SEARCHKEY = ?, TAXID = ?, NAME = ?, CARD = ?, " +
                                   "TAXCATEGORY = ?, NOTES = ?, VISIBLE = ?, CURDATE = ?, CURDEBT = ?, " +
                                   "MAXDEBT = ? WHERE ID = ?";
    
                checkStmt = session.getConnection().prepareStatement(checkSql);
                insertStmt = session.getConnection().prepareStatement(insertSql);
                updateStmt = session.getConnection().prepareStatement(updateSql);
    
                for (Map<String, Object> cliente : clientes) {
                    try {
                        String id = (String) cliente.get("id");
                        String searchkey = (String) cliente.get("searchkey");
                        String taxid = (String) cliente.get("taxid");
                        String name = (String) cliente.get("nombre");
                        String card = (String) cliente.get("tarjeta");
                        String taxcategory = (String) cliente.get("taxcategory");
                        String notes = (String) cliente.get("notes");
                        Boolean visible = (Boolean) cliente.get("visible");
                        String curdate = (String) cliente.get("curdate");
                        Object curdebtObj = cliente.get("curdebt");
                        Object maxdebtObj = cliente.get("maxdebt");
    
                        if (id == null || id.trim().isEmpty()) {
                            LOGGER.warning("Cliente sin ID, saltando: " + cliente);
                            errores++;
                            continue;
                        }
    
                        checkStmt.setString(1, id);
                        rs = checkStmt.executeQuery();
                        boolean existe = rs.next();
                        rs.close();
    
                        BigDecimal curdebt = BigDecimal.ZERO;
                        BigDecimal maxdebt = BigDecimal.ZERO;
    
                        if (curdebtObj instanceof Number)
                            curdebt = new BigDecimal(((Number) curdebtObj).doubleValue());
                        if (maxdebtObj instanceof Number)
                            maxdebt = new BigDecimal(((Number) maxdebtObj).doubleValue());
    
                        // ✅ Convertir fecha si viene en texto ISO (por ejemplo: 2025-10-29T16:20:39.236957+00:00)
                        Timestamp curDateTimestamp = null;
                        if (curdate != null && !curdate.isEmpty()) {
                            try {
                                curDateTimestamp = Timestamp.from(java.time.Instant.parse(curdate));
                            } catch (Exception e) {
                                LOGGER.fine("Fecha inválida para cliente " + id + ": " + curdate);
                            }
                        }
    
                        // ✅ Si taxcategory está vacío o no existe, usar null (para evitar violación de FK)
                        if (taxcategory != null && taxcategory.trim().isEmpty()) {
                            taxcategory = null;
                        }
    
                        if (existe) {
                            updateStmt.setString(1, searchkey != null ? searchkey : id);
                            updateStmt.setString(2, taxid);
                            updateStmt.setString(3, name != null ? name : "Cliente sin nombre");
                            updateStmt.setString(4, card);
    
                            if (taxcategory != null)
                                updateStmt.setString(5, taxcategory);
                            else
                                updateStmt.setNull(5, java.sql.Types.VARCHAR);
    
                            updateStmt.setString(6, notes);
                            updateStmt.setBoolean(7, visible != null ? visible : true);
                            updateStmt.setTimestamp(8, curDateTimestamp);
                            updateStmt.setBigDecimal(9, curdebt);
                            updateStmt.setBigDecimal(10, maxdebt);
                            updateStmt.setString(11, id);
                            updateStmt.executeUpdate();
                            actualizados++;
                            LOGGER.fine("Cliente actualizado: " + id + " - " + name);
                        } else {
                            insertStmt.setString(1, id);
                            insertStmt.setString(2, searchkey != null ? searchkey : id);
                            insertStmt.setString(3, taxid);
                            insertStmt.setString(4, name != null ? name : "Cliente sin nombre");
                            insertStmt.setString(5, card);
    
                            if (taxcategory != null)
                                insertStmt.setString(6, taxcategory);
                            else
                                insertStmt.setNull(6, java.sql.Types.VARCHAR);
    
                            insertStmt.setString(7, notes);
                            insertStmt.setBoolean(8, visible != null ? visible : true);
                            insertStmt.setTimestamp(9, curDateTimestamp);
                            insertStmt.setBigDecimal(10, curdebt);
                            insertStmt.setBigDecimal(11, maxdebt);
                            insertStmt.executeUpdate();
                            insertados++;
                            LOGGER.fine("Cliente insertado: " + id + " - " + name);
                        }
    
                    } catch (java.sql.SQLIntegrityConstraintViolationException fkEx) {
                        errores++;
                        LOGGER.warning("Violación de integridad al insertar cliente (FK no válida): " + cliente);
                    } catch (Exception e) {
                        errores++;
                        LOGGER.log(Level.WARNING, "Error procesando cliente: " + cliente, e);
                    }
                }
    
                LOGGER.info("Clientes procesados: " + insertados + " insertados, " +
                           actualizados + " actualizados, " + errores + " errores");
                return errores == 0;
    
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando clientes", e);
                return false;
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (checkStmt != null) checkStmt.close();
                    if (insertStmt != null) insertStmt.close();
                    if (updateStmt != null) updateStmt.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error cerrando recursos", e);
                }
            }
        });
    }
    
    
    /**
     * Descarga categorías desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadCategorias() {
        return CompletableFuture.supplyAsync(() -> {
            PreparedStatement checkStmt = null;
            PreparedStatement insertStmt = null;
            PreparedStatement updateStmt = null;
            java.sql.ResultSet rs = null;
            try {
                SupabaseServiceREST supabase = supabaseManager.getService();
                List<Map<String, Object>> categorias = supabase.fetchData("categorias");
                LOGGER.info("Descargadas " + categorias.size() + " categorías desde Supabase");
                
                int insertados = 0;
                int actualizados = 0;
                int errores = 0;
                
                // Preparar statements
                String checkSql = "SELECT ID FROM categories WHERE ID = ?";
                String insertSql = "INSERT INTO categories (ID, NAME, PARENTID, IMAGE) VALUES (?, ?, ?, NULL)";
                String updateSql = "UPDATE categories SET NAME = ?, PARENTID = ? WHERE ID = ?";
                
                checkStmt = session.getConnection().prepareStatement(checkSql);
                insertStmt = session.getConnection().prepareStatement(insertSql);
                updateStmt = session.getConnection().prepareStatement(updateSql);
                
                for (Map<String, Object> categoria : categorias) {
                    try {
                        String id = (String) categoria.get("id");
                        String nombre = (String) categoria.get("nombre");
                        String categoriapadre = (String) categoria.get("categoriapadre");
                        Boolean tieneimagen = (Boolean) categoria.get("tieneimagen");
                        
                        // Validar que tenga ID
                        if (id == null || id.trim().isEmpty()) {
                            LOGGER.warning("Categoría sin ID, saltando: " + categoria);
                            errores++;
                            continue;
                        }
                        
                        // Verificar si existe
                        checkStmt.setString(1, id);
                        rs = checkStmt.executeQuery();
                        boolean existe = rs.next();
                        rs.close();
                        
                        if (existe) {
                            // Actualizar categoría existente
                            updateStmt.setString(1, nombre != null ? nombre : "Categoría sin nombre");
                            updateStmt.setString(2, categoriapadre);
                            updateStmt.setString(3, id);
                            updateStmt.executeUpdate();
                            actualizados++;
                            LOGGER.fine("Categoría actualizada: " + id + " - " + nombre);
                        } else {
                            // Insertar nueva categoría
                            insertStmt.setString(1, id);
                            insertStmt.setString(2, nombre != null ? nombre : "Categoría sin nombre");
                            insertStmt.setString(3, categoriapadre);
                            insertStmt.executeUpdate();
                            insertados++;
                            LOGGER.fine("Categoría insertada: " + id + " - " + nombre);
                        }
                    } catch (Exception e) {
                        errores++;
                        LOGGER.log(Level.WARNING, "Error procesando categoría: " + categoria, e);
                    }
                }
                
                LOGGER.info("Categorías procesadas: " + insertados + " insertadas, " + 
                           actualizados + " actualizadas, " + errores + " errores");
                return true;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando categorías", e);
                return false;
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (checkStmt != null) checkStmt.close();
                    if (insertStmt != null) insertStmt.close();
                    if (updateStmt != null) updateStmt.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error cerrando recursos", e);
                }
            }
        });
    }

    /**
     * Descarga ventas desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadVentas() {
        return CompletableFuture.supplyAsync(() -> {
            PreparedStatement checkStmt = null;
            PreparedStatement insertStmt = null;
            PreparedStatement updateStmt = null;
            PreparedStatement checkMoneyStmt = null;
            PreparedStatement insertMoneyStmt = null;
            PreparedStatement seqStmt = null;
            java.sql.ResultSet rs = null;
    
            try {
                SupabaseServiceREST supabase = supabaseManager.getService();
    
                List<Map<String, Object>> ventas = supabase.fetchData("ventas");
                LOGGER.info("Descargadas " + ventas.size() + " ventas desde Firebase");
    
                int insertados = 0;
                int actualizados = 0;
                int errores = 0;
    
                String checkSql = "SELECT ID FROM receipts WHERE ID = ?";
                String insertSql = "INSERT INTO receipts (ID, MONEY, DATENEW, PERSON) VALUES (?, ?, ?, ?)";
                String updateSql = "UPDATE receipts SET MONEY = ?, DATENEW = ?, PERSON = ? WHERE ID = ?";
                String checkMoneySql = "SELECT MONEY FROM closedcash WHERE MONEY = ?";
                String seqSql = "SELECT MAX(HOSTSEQUENCE) FROM closedcash WHERE HOST = ?";
                String insertMoneySql = "INSERT INTO closedcash (MONEY, HOST, HOSTSEQUENCE, DATESTART, DATEEND) VALUES (?, ?, ?, ?, ?)";
    
                checkStmt = session.getConnection().prepareStatement(checkSql);
                insertStmt = session.getConnection().prepareStatement(insertSql);
                updateStmt = session.getConnection().prepareStatement(updateSql);
                checkMoneyStmt = session.getConnection().prepareStatement(checkMoneySql);
                seqStmt = session.getConnection().prepareStatement(seqSql);
                insertMoneyStmt = session.getConnection().prepareStatement(insertMoneySql);
    
                for (Map<String, Object> venta : ventas) {
                    try {
                        String id = (String) venta.get("id");
                        String caja = (String) venta.get("caja");
                        Object rawFechaVenta = venta.get("fechaventa");
                        java.sql.Timestamp fechaVentaTs = toSqlTimestamp(rawFechaVenta);
                        String vendedorid = (String) venta.get("vendedorid");
    
                        if (id == null || id.trim().isEmpty()) {
                            LOGGER.warning("Venta sin ID, saltando: " + venta);
                            errores++;
                            continue;
                        }
    
                        // Verificar si existe el MONEY (caja)
                        checkMoneyStmt.setString(1, caja);
                        java.sql.ResultSet rsMoney = checkMoneyStmt.executeQuery();
                        boolean moneyExiste = rsMoney.next();
                        rsMoney.close();
    
                        if (!moneyExiste) {
                            String host = "default-host";
    
                            // Buscar el siguiente HOSTSEQUENCE disponible
                            seqStmt.setString(1, host);
                            java.sql.ResultSet rsSeq = seqStmt.executeQuery();
                            int nextSeq = 1;
                            if (rsSeq.next()) {
                                nextSeq = rsSeq.getInt(1) + 1;
                            }
                            rsSeq.close();
    
                            try {
                                insertMoneyStmt.setString(1, caja);
                                insertMoneyStmt.setString(2, host);
                                insertMoneyStmt.setInt(3, nextSeq);
                                insertMoneyStmt.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
                                insertMoneyStmt.setNull(5, java.sql.Types.TIMESTAMP);
                                insertMoneyStmt.executeUpdate();
                                LOGGER.info("Se creó cierre de caja automáticamente: " + caja + " con HOSTSEQUENCE=" + nextSeq);
                            } catch (SQLException ex) {
                                LOGGER.warning("No se pudo insertar cierre de caja (puede existir conflicto): " + ex.getMessage());
                            }
                        }
    
                        // Verificar si existe la venta
                        checkStmt.setString(1, id);
                        rs = checkStmt.executeQuery();
                        boolean existe = rs.next();
                        rs.close();
    
                        if (existe) {
                            updateStmt.setString(1, caja);
                            updateStmt.setTimestamp(2, fechaVentaTs);
                            updateStmt.setString(3, vendedorid);
                            updateStmt.setString(4, id);
                            updateStmt.executeUpdate();
                            actualizados++;
                            LOGGER.fine("Venta actualizada: " + id + " - " + caja);
                        } else {
                            insertStmt.setString(1, id);
                            insertStmt.setString(2, caja);
                            insertStmt.setTimestamp(3, fechaVentaTs);
                            insertStmt.setString(4, vendedorid);
                            insertStmt.executeUpdate();
                            insertados++;
                            LOGGER.fine("Venta insertada: " + id + " - " + caja);
                        }
    
                    } catch (Exception e) {
                        errores++;
                        LOGGER.log(Level.WARNING, "Error procesando venta: " + venta, e);
                    }
                }
    
                LOGGER.info("Ventas procesadas: " + insertados + " insertadas, " +
                            actualizados + " actualizadas, " + errores + " errores");
                return errores == 0;
    
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando ventas", e);
                return false;
            } finally {
                try { if (rs != null) rs.close(); } catch (SQLException ignore) {}
                try { if (checkStmt != null) checkStmt.close(); } catch (SQLException ignore) {}
                try { if (insertStmt != null) insertStmt.close(); } catch (SQLException ignore) {}
                try { if (updateStmt != null) updateStmt.close(); } catch (SQLException ignore) {}
                try { if (checkMoneyStmt != null) checkMoneyStmt.close(); } catch (SQLException ignore) {}
                try { if (insertMoneyStmt != null) insertMoneyStmt.close(); } catch (SQLException ignore) {}
                try { if (seqStmt != null) seqStmt.close(); } catch (SQLException ignore) {}
            }
        });
    }
    
    
    
    /**
     * Descarga productos desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadProductos() {
        return CompletableFuture.supplyAsync(() -> {
            PreparedStatement checkStmt = null;
            PreparedStatement insertStmt = null;
            PreparedStatement updateStmt = null;
            java.sql.ResultSet rs = null;
            try {
                SupabaseServiceREST supabase = supabaseManager.getService();
                List<Map<String, Object>> productos = supabase.fetchData("productos");
                LOGGER.info("Descargados " + productos.size() + " productos desde Supabase");
                
                int insertados = 0;
                int actualizados = 0;
                int errores = 0;
                
                // Preparar statements
                String checkSql = "SELECT ID FROM products WHERE ID = ?";
                String insertSql = "INSERT INTO products (ID, REFERENCE, CODE, CODETYPE, NAME, PRICEBUY, PRICESELL, CATEGORY, TAXCAT, ATTRIBUTESET_ID, ISCOM, PRINTKB, SENDSTATUS, ISSERVICE) " +
                                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                String updateSql = "UPDATE products SET REFERENCE = ?, CODE = ?, CODETYPE = ?, NAME = ?, PRICEBUY = ?, PRICESELL = ?, CATEGORY = ?, TAXCAT = ?, ATTRIBUTESET_ID = ?, ISCOM = ?, PRINTKB = ?, SENDSTATUS = ?, ISSERVICE = ? WHERE ID = ?";
                
                checkStmt = session.getConnection().prepareStatement(checkSql);
                insertStmt = session.getConnection().prepareStatement(insertSql);
                updateStmt = session.getConnection().prepareStatement(updateSql);
                
                for (Map<String, Object> producto : productos) {
                    try {
                        String id = (String) producto.get("id");
                        String referencia = (String) producto.get("referencia");
                        String codigo = (String) producto.get("codigo");
                        String tipocodigobarras = (String) producto.get("tipocodigobarras");
                        String nombre = (String) producto.get("nombre");
                        Double preciocompra = (Double) producto.get("preciocompra");
                        Double precioventa = (Double) producto.get("precioventa");
                        String categoriaid = (String) producto.get("categoriaid");
                        String categorianombre = (String) producto.get("categorianombre");
                        String categoriaimpuesto = (String) producto.get("categoriaimpuesto");
                        String categoriaimpuestonombre = (String) producto.get("categoriaimpuestonombre");
                        String atributos = (String) producto.get("atributos");
                        Boolean tieneimagen = (Boolean) producto.get("tieneimagen");
                        Boolean escompuesto = (Boolean) producto.get("escompuesto");
                        Boolean imprimirencocina = (Boolean) producto.get("imprimirencocina");
                        Boolean estadoenvio = (Boolean) producto.get("estadoenvio");
                        // Obtener ISSERVICE desde Supabase (puede venir como esServicio, isservice, o es_servicio)
                        Boolean esServicio = (Boolean) producto.get("esServicio");
                        if (esServicio == null) {
                            esServicio = (Boolean) producto.get("isservice");
                        }
                        if (esServicio == null) {
                            esServicio = (Boolean) producto.get("es_servicio");
                        }
                        if (esServicio == null) {
                            esServicio = false; // Por defecto, no es servicio
                        }
                        String fechaextraccion = (String) producto.get("fechaextraccion");
                        String tabla = (String) producto.get("tabla");
                        
                        // Validar que tenga ID
                        if (id == null || id.trim().isEmpty()) {
                            LOGGER.warning("Producto sin ID, saltando: " + producto);
                            errores++;
                            continue;
                        }
                        
                        // Verificar si existe
                        checkStmt.setString(1, id);
                        rs = checkStmt.executeQuery();
                        boolean existe = rs.next();
                        rs.close();
                        
                        if (existe) {
                            // Actualizar producto existente
                            updateStmt.setString(1, referencia != null ? referencia : id);
                            updateStmt.setString(2, codigo);
                            updateStmt.setString(3, tipocodigobarras);
                            updateStmt.setString(4, nombre != null ? nombre : "Producto sin nombre");
                            updateStmt.setDouble(5, preciocompra != null ? preciocompra : 0.0);
                            updateStmt.setDouble(6, precioventa != null ? precioventa : 0.0);
                            updateStmt.setString(7, categoriaid);
                            updateStmt.setString(8, categoriaimpuesto);
                            updateStmt.setString(9, atributos);
                            updateStmt.setBoolean(10, escompuesto != null ? escompuesto : false);
                            updateStmt.setBoolean(11, imprimirencocina != null ? imprimirencocina : false);
                            updateStmt.setBoolean(12, estadoenvio != null ? estadoenvio : false);
                            updateStmt.setBoolean(13, esServicio);
                            updateStmt.setString(14, id);
                            updateStmt.executeUpdate();
                            actualizados++;
                            LOGGER.fine("Producto actualizado: " + id + " - " + nombre + " (ISSERVICE: " + esServicio + ")");
                        } else {
                            // Insertar nuevo producto
                            insertStmt.setString(1, id);
                            insertStmt.setString(2, referencia != null ? referencia : id);
                            insertStmt.setString(3, codigo);
                            insertStmt.setString(4, tipocodigobarras);
                            insertStmt.setString(5, nombre != null ? nombre : "Producto sin nombre");
                            insertStmt.setDouble(6, preciocompra != null ? preciocompra : 0.0);
                            insertStmt.setDouble(7, precioventa != null ? precioventa : 0.0);   

                            insertStmt.setString(8, categoriaid);
                            insertStmt.setString(9, categoriaimpuesto);
                            insertStmt.setString(10, atributos);
                            insertStmt.setBoolean(11, escompuesto != null ? escompuesto : false);
                            insertStmt.setBoolean(12, imprimirencocina != null ? imprimirencocina : false);
                            insertStmt.setBoolean(13, estadoenvio != null ? estadoenvio : false);
                            insertStmt.setBoolean(14, esServicio);
                            insertStmt.executeUpdate();
                            insertados++;
                            LOGGER.fine("Producto insertado: " + id + " - " + nombre + " (ISSERVICE: " + esServicio + ")");
                        }
                    }
                    catch (Exception e) {
                        errores++;
                        LOGGER.log(Level.WARNING, "Error procesando producto: " + producto, e);
                    }
                }
                
                LOGGER.info("Productos procesados: " + insertados + " insertados, " + 
                           actualizados + " actualizados, " + errores + " errores");
                return errores == 0;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando productos", e);
                return false;
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (checkStmt != null) checkStmt.close();
                    if (insertStmt != null) insertStmt.close();
                    if (updateStmt != null) updateStmt.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error cerrando recursos", e);
                }
            }
        });
    }
    
    /**
     * Descarga puntos de clientes desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadPuntosClientes() {
        return CompletableFuture.supplyAsync(() -> {
            try {

                SupabaseServiceREST supabase = supabaseManager.getService();
                List<Map<String, Object>> puntos = supabase.fetchData("puntos_historial");
                LOGGER.info("Descargados " + puntos.size() + " registros de puntos de clientes desde Supabase");
                
                
                return true;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando puntos de clientes", e);
                return false;
            }
        });
    }
    
    /**
     * Descarga cierres de caja desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadCierresCaja() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SupabaseServiceREST supabase = supabaseManager.getService();
                List<Map<String, Object>> cierres = supabase.fetchData("cierres");
                LOGGER.info("Descargados " + cierres.size() + " cierres de caja desde Supabase");
                

                return true;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando cierres de caja", e);
                return false;
            }
        });
    }
    
    /**
     * Descarga formas de pago desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadFormasPago() {
        return CompletableFuture.supplyAsync(() -> {
            PreparedStatement checkStmt = null;
            PreparedStatement insertStmt = null;
            PreparedStatement updateStmt = null;
            java.sql.ResultSet rs = null;
            try {
                SupabaseServiceREST supabase = supabaseManager.getService();
                List<Map<String, Object>> formas = supabase.fetchData("formas_de_pago");
                LOGGER.info("Descargados " + formas.size() + " formas de pago desde supabase");
                
                int insertados = 0;
                int actualizados = 0;
                int errores = 0;
                
                // Preparar statements
                String checkSql = "SELECT ID FROM payment_methods WHERE ID = ?";
                String insertSql = "INSERT INTO payment_methods (ID, NAME, DESCRIPTION) VALUES (?, ?, ?)";
                String updateSql = "UPDATE payment_methods SET NAME = ?, DESCRIPTION = ? WHERE ID = ?";
                
                checkStmt = session.getConnection().prepareStatement(checkSql);
                insertStmt = session.getConnection().prepareStatement(insertSql);
                updateStmt = session.getConnection().prepareStatement(updateSql);
                
                for (Map<String, Object> forma : formas) {
                    try {
                        String id = (String) forma.get("id");
                        String nombre = (String) forma.get("nombre");
                        String descripcion = (String) forma.get("descripcion");
                        
                        // Validar que tenga ID
                        if (id == null || id.trim().isEmpty()) {
                            LOGGER.warning("Forma de pago sin ID, saltando: " + forma);
                            errores++;
                            continue;
                        }
                        
                        // Verificar si existe
                        checkStmt.setString(1, id);
                        rs = checkStmt.executeQuery();
                        boolean existe = rs.next();
                        rs.close();
                        
                        if (existe) {
                            // Actualizar forma de pago existente
                            updateStmt.setString(1, nombre);
                            updateStmt.setString(2, descripcion);
                            updateStmt.setString(3, id);
                            updateStmt.executeUpdate();
                            actualizados++;
                            LOGGER.fine("Forma de pago actualizada: " + id + " - " + nombre);
                        } else {
                            // Insertar nueva forma de pago
                            insertStmt.setString(1, id);
                            insertStmt.setString(2, nombre);
                            insertStmt.setString(3, descripcion);
                            insertStmt.executeUpdate();
                            insertados++;
                            LOGGER.fine("Forma de pago insertada: " + id + " - " + nombre);
                        }
                    } catch (Exception e) {
                        errores++;
                        LOGGER.log(Level.WARNING, "Error procesando forma de pago: " + forma, e);
                    }
                }
                
                LOGGER.info("Formas de pago procesadas: " + insertados + " insertadas, " + 
                           actualizados + " actualizadas, " + errores + " errores");
                return errores == 0;    
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando formas de pago", e);
                return false;
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (checkStmt != null) checkStmt.close();
                    if (insertStmt != null) insertStmt.close();
                    if (updateStmt != null) updateStmt.close();

                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error cerrando recursos", e);
                }
            }
        });
    }
    


    /**
     * Descarga impuestos desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadImpuestos() {
        return CompletableFuture.supplyAsync(() -> {
            PreparedStatement checkStmt = null;
            PreparedStatement insertStmt = null;
            PreparedStatement updateStmt = null;
            java.sql.ResultSet rs = null;
            try {
                SupabaseServiceREST supabase = supabaseManager.getService();
                List<Map<String, Object>> impuestos = supabase.fetchData("impuestos");
                LOGGER.info("Descargados " + impuestos.size() + " impuestos desde Supabase");
                
                int insertados = 0;
                int actualizados = 0;
                int errores = 0;
                
                // Preparar statements
                String checkSql = "SELECT ID FROM taxes WHERE ID = ?";
                String insertSql = "INSERT INTO taxes (ID, NAME, CATEGORY, RATE) VALUES (?, ?, ?, ?)";
                String updateSql = "UPDATE taxes SET NAME = ?, CATEGORY = ?, RATE = ? WHERE ID = ?";
                
                checkStmt = session.getConnection().prepareStatement(checkSql);
                insertStmt = session.getConnection().prepareStatement(insertSql);
                updateStmt = session.getConnection().prepareStatement(updateSql);
                
                for (Map<String, Object> impuesto : impuestos) {
                    try {
                        String id = (String) impuesto.get("id");
                        String nombre = (String) impuesto.get("nombre");
                        String categoria = (String) impuesto.get("categoria");
                        Double tasa = (Double) impuesto.get("tasa");
                        
                        // Validar que tenga ID
                        if (id == null || id.trim().isEmpty()) {
                            LOGGER.warning("Impuesto sin ID, saltando: " + impuesto);
                            errores++;
                            continue;
                        }
                        
                        // Verificar si existe
                        checkStmt.setString(1, id);
                        rs = checkStmt.executeQuery();
                        boolean existe = rs.next();
                        rs.close();
                        
                        if (existe) {
                            // Actualizar impuesto existente
                            updateStmt.setString(1, nombre);
                            updateStmt.setString(2, categoria);
                            updateStmt.setDouble(3, tasa);
                            updateStmt.setString(4, id);
                            updateStmt.executeUpdate();
                            actualizados++;
                            LOGGER.fine("Impuesto actualizado: " + id + " - " + nombre);
                        } else {
                            // Insertar nuevo impuesto
                            insertStmt.setString(1, id);
                            insertStmt.setString(2, nombre);
                            insertStmt.setString(3, categoria);
                            insertStmt.setDouble(4, tasa);
                            insertStmt.executeUpdate();
                            insertados++;
                            LOGGER.fine("Impuesto insertado: " + id + " - " + nombre);
                        }
                    } catch (Exception e) {
                        errores++;
                        LOGGER.log(Level.WARNING, "Error procesando impuesto: " + impuesto, e);
                    }
                }
                return true;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando impuestos", e);
                return false;
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (checkStmt != null) checkStmt.close();
                    if (insertStmt != null) insertStmt.close();
                    if (updateStmt != null) updateStmt.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error cerrando recursos", e);
                }
            }
        });
    }
    
    /**
     * Descarga configuraciones desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadConfiguraciones() {
        return CompletableFuture.supplyAsync(() -> {
            PreparedStatement checkStmt = null;
            PreparedStatement insertStmt = null;
            PreparedStatement updateStmt = null;
            java.sql.ResultSet rs = null;
            try {

                SupabaseServiceREST supabase = supabaseManager.getService();
                List<Map<String, Object>> configuraciones = supabase.fetchData("config");
                LOGGER.info("Descargadas " + configuraciones.size() + " configuraciones desde Firebase");
                
                int insertados = 0;
                int actualizados = 0;
                int errores = 0;
                
                // Preparar statements
                String checkSql = "SELECT ID FROM configurations WHERE ID = ?";
                String insertSql = "INSERT INTO configurations (ID, NAME, VALUE) VALUES (?, ?, ?)";
                String updateSql = "UPDATE configurations SET NAME = ?, VALUE = ? WHERE ID = ?";
                
                checkStmt = session.getConnection().prepareStatement(checkSql);
                insertStmt = session.getConnection().prepareStatement(insertSql);
                updateStmt = session.getConnection().prepareStatement(updateSql);
                
                for (Map<String, Object> configuracion : configuraciones) {
                    try {
                        String id = (String) configuracion.get("id");
                        String nombre = (String) configuracion.get("nombre");
                        String valor = (String) configuracion.get("valor");
                        
                        // Validar que tenga ID
                        if (id == null || id.trim().isEmpty()) {
                            LOGGER.warning("Configuración sin ID, saltando: " + configuracion);
                            errores++;
                            continue;
                        }
                        
                        // Verificar si existe
                        checkStmt.setString(1, id);
                        rs = checkStmt.executeQuery();
                        boolean existe = rs.next();
                        rs.close();
                        
                        if (existe) {
                            // Actualizar configuración existente
                            updateStmt.setString(1, nombre);
                            updateStmt.setString(2, valor);
                            updateStmt.setString(3, id);
                            updateStmt.executeUpdate();
                            actualizados++;
                            LOGGER.fine("Configuración actualizada: " + id + " - " + nombre);
                        } else {
                            // Insertar nueva configuración
                            insertStmt.setString(1, id);
                            insertStmt.setString(2, nombre);
                            insertStmt.setString(3, valor);
                            insertStmt.executeUpdate();
                            insertados++;
                            LOGGER.fine("Configuración insertada: " + id + " - " + nombre);
                        }
                    } catch (Exception e) {
                        errores++;
                        LOGGER.log(Level.WARNING, "Error procesando configuración: " + configuracion, e);
                    }
                }
                
                LOGGER.info("Configuraciones procesadas: " + insertados + " insertadas, " + 
                           actualizados + " actualizadas, " + errores + " errores");
                return true;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando configuraciones", e);
                return false;
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (checkStmt != null) checkStmt.close();
                    if (insertStmt != null) insertStmt.close();
                    if (updateStmt != null) updateStmt.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error cerrando recursos", e);
                }
            }
        });
    }
    
    /**
     * Descarga inventario desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadInventario() {
        return CompletableFuture.supplyAsync(() -> {
            PreparedStatement checkProductStmt = null;
            PreparedStatement checkStmt = null;
            PreparedStatement insertStmt = null;
            PreparedStatement updateStmt = null;
            ResultSet rs = null;
    
            try {
                SupabaseServiceREST supabase = supabaseManager.getService();
    
                List<Map<String, Object>> inventario = supabase.fetchData("inventario");
                LOGGER.info("Descargado inventario con " + inventario.size() + " registros desde Supabase");
    
                int insertados = 0;
                int actualizados = 0;
                int errores = 0;
    
                // ✅ Compatible con HSQLDB
                String checkProductSql = "SELECT ID FROM products WHERE ID = ?";
                String checkSql = """
                    SELECT * FROM stockcurrent 
                    WHERE LOCATION = ? 
                    AND PRODUCT = ? 
                    AND ((ATTRIBUTESETINSTANCE_ID IS NULL AND ? IS NULL) OR ATTRIBUTESETINSTANCE_ID = ?)
                    """;

                String insertSql = "INSERT INTO stockcurrent (LOCATION, PRODUCT, ATTRIBUTESETINSTANCE_ID, UNITS) VALUES (?, ?, ?, ?)";
                String updateSql = """
                    UPDATE stockcurrent 
                    SET UNITS = ? 
                    WHERE LOCATION = ? 
                    AND PRODUCT = ? 
                    AND ((ATTRIBUTESETINSTANCE_ID IS NULL AND ? IS NULL) OR ATTRIBUTESETINSTANCE_ID = ?)
                    """;

                checkProductStmt = session.getConnection().prepareStatement(checkProductSql);
                checkStmt = session.getConnection().prepareStatement(checkSql);
                insertStmt = session.getConnection().prepareStatement(insertSql);
                updateStmt = session.getConnection().prepareStatement(updateSql);
                
                for (Map<String, Object> movimiento : inventario) {
                    try {
                        String location = (String) movimiento.get("ubicacion");
                        String product = (String) movimiento.get("productoid");
                        String attributeSetInstanceId = (String) movimiento.get("atributos");
                        Double units = (Double) movimiento.get("unidades");

                        if (location == null || product == null || units == null) {
                            LOGGER.warning("Movimiento de inventario incompleto, saltando: " + movimiento);
                            errores++;
                            continue;
                        }

                        // 🔍 VERIFICAR QUE EL PRODUCTO EXISTE EN LA TABLA PRODUCTS
                        checkProductStmt.setString(1, product);
                        ResultSet productRs = checkProductStmt.executeQuery();
                        boolean productoExiste = productRs.next();
                        productRs.close();

                        if (!productoExiste) {
                            LOGGER.warning("⚠️ Producto no encontrado en BD local: " + product + " (" + movimiento.get("productonombre") + "). Inventario saltado.");
                            errores++;
                            continue;
                        }

                        // 🔍 Verificar existencia del registro de inventario
                        checkStmt.setString(1, location);
                        checkStmt.setString(2, product);
                        checkStmt.setString(3, attributeSetInstanceId);
                        checkStmt.setString(4, attributeSetInstanceId);

                        rs = checkStmt.executeQuery();
                        boolean existe = rs.next();
                        Double stockActual = null;
                        if (existe) {
                            stockActual = rs.getDouble("UNITS");
                        }
                        rs.close();
    
                        if (existe) {
                            // ⚠️ NO actualizar si ya existe registro local - preservar cambios locales
                            // El stock local tiene prioridad sobre el remoto para evitar sobrescribir ventas
                            LOGGER.fine("Inventario local existe para " + product + " en " + location + 
                                      " (local: " + stockActual + ", remoto: " + units + ") - NO actualizado para preservar cambios locales");
                            // No hacer nada - preservar el stock local
                        } else {
                            // 🆕 Insertar solo si no existe
                            insertStmt.setString(1, location);
                            insertStmt.setString(2, product);
                            insertStmt.setString(3, attributeSetInstanceId);
                            insertStmt.setDouble(4, units);
    
                            insertStmt.executeUpdate();
                            insertados++;
                            LOGGER.fine("Inventario insertado: " + product + " en " + location + " con " + units + " unidades");
                        }
    
                    } catch (Exception e) {
                        errores++;
                        LOGGER.log(Level.WARNING, "Error procesando movimiento de inventario: " + movimiento, e);
                    }
                }
    
                LOGGER.info("Inventario sincronizado: " + insertados + " insertados, " + actualizados + " actualizados, " + errores + " errores.");
                return errores == 0;

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando inventario", e);
                return false;
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (checkProductStmt != null) checkProductStmt.close();
                    if (checkStmt != null) checkStmt.close();
                    if (insertStmt != null) insertStmt.close();
                    if (updateStmt != null) updateStmt.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error cerrando recursos", e);
                }
            }
        });
    }
    
    



































    /**
     * Resultado de la descarga
     */
    public static class DownloadResult {
        public boolean success = false;
        public boolean usuariosDescargados = false;
        public boolean clientesDescargados = false;
        public boolean categoriasDescargadas = false;
        public boolean productosDescargados = false;
        public boolean ventasDescargadas = false;
        public boolean puntosDescargados = false;
        public boolean cierresDescargados = false;
        public boolean pagosDescargados = false;
        public boolean impuestosDescargados = false;
        public boolean configuracionesDescargadas = false;
        public boolean inventarioDescargado = false;
    }
}