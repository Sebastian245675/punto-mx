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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.math.BigDecimal;

/**
 * Gestor de descarga de datos desde Firebase hacia la base de datos local
 * @author Sebastian
 */
public class FirebaseDownloadManagerREST {
    
    private static final Logger LOGGER = Logger.getLogger(FirebaseDownloadManagerREST.class.getName());
    
    private final Session session;
    private final FirebaseServiceREST firebaseService;
    
    public FirebaseDownloadManagerREST(Session session, AppConfig config) {
        this.session = session;
        this.firebaseService = FirebaseServiceREST.getInstance();
        
        // Inicializar el servicio Firebase
        if (!firebaseService.initialize(config)) {
            throw new IllegalStateException("No se pudo inicializar Firebase Service");
        }
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
                List<Map<String, Object>> usuarios = firebaseService.downloadUsuarios().join();
                LOGGER.info("Descargados " + usuarios.size() + " usuarios desde Firebase");
                
                int insertados = 0;
                int actualizados = 0;
                int errores = 0;
                
                // Preparar statements
                String checkSql = "SELECT ID FROM people WHERE ID = ?";
                String insertSql = "INSERT INTO people (ID, NAME, APPPASSWORD, CARD, ROLE, VISIBLE, IMAGE) " +
                                  "VALUES (?, ?, NULL, ?, ?, ?, NULL)";
                String updateSql = "UPDATE people SET NAME = ?, CARD = ?, ROLE = ?, VISIBLE = ? WHERE ID = ?";
                
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
                            updateStmt.setString(5, id);
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
                List<Map<String, Object>> clientes = firebaseService.downloadClientes().join();
                LOGGER.info("Descargados " + clientes.size() + " clientes desde Firebase");
                
                int insertados = 0;
                int actualizados = 0;
                int errores = 0;
                
                // Preparar statements
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
                        
                        // Validar que tenga ID
                        if (id == null || id.trim().isEmpty()) {
                            LOGGER.warning("Cliente sin ID, saltando: " + cliente);
                            errores++;
                            continue;
                        }
                        
                        // Verificar si existe
                        checkStmt.setString(1, id);
                        rs = checkStmt.executeQuery();
                        boolean existe = rs.next();
                        rs.close();
                        
                        // Convertir deudas a BigDecimal
                        BigDecimal curdebt = BigDecimal.ZERO;
                        BigDecimal maxdebt = BigDecimal.ZERO;
                        
                        if (curdebtObj != null) {
                            if (curdebtObj instanceof Number) {
                                curdebt = new BigDecimal(((Number) curdebtObj).doubleValue());
                            }
                        }
                        if (maxdebtObj != null) {
                            if (maxdebtObj instanceof Number) {
                                maxdebt = new BigDecimal(((Number) maxdebtObj).doubleValue());
                            }
                        }
                        
                        if (existe) {
                            // Actualizar cliente existente
                            updateStmt.setString(1, searchkey != null ? searchkey : id);
                            updateStmt.setString(2, taxid);
                            updateStmt.setString(3, name != null ? name : "Cliente sin nombre");
                            updateStmt.setString(4, card);
                            updateStmt.setString(5, taxcategory != null ? taxcategory : "000");
                            updateStmt.setString(6, notes);
                            updateStmt.setBoolean(7, visible != null ? visible : true);
                            updateStmt.setString(8, curdate);
                            updateStmt.setBigDecimal(9, curdebt);
                            updateStmt.setBigDecimal(10, maxdebt);
                            updateStmt.setString(11, id);
                            updateStmt.executeUpdate();
                            actualizados++;
                            LOGGER.fine("Cliente actualizado: " + id + " - " + name);
                        } else {
                            // Insertar nuevo cliente
                            insertStmt.setString(1, id);
                            insertStmt.setString(2, searchkey != null ? searchkey : id);
                            insertStmt.setString(3, taxid);
                            insertStmt.setString(4, name != null ? name : "Cliente sin nombre");
                            insertStmt.setString(5, card);
                            insertStmt.setString(6, taxcategory != null ? taxcategory : "000");
                            insertStmt.setString(7, notes);
                            insertStmt.setBoolean(8, visible != null ? visible : true);
                            insertStmt.setString(9, curdate);
                            insertStmt.setBigDecimal(10, curdebt);
                            insertStmt.setBigDecimal(11, maxdebt);
                            insertStmt.executeUpdate();
                            insertados++;
                            LOGGER.fine("Cliente insertado: " + id + " - " + name);
                        }
                        
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
            try {
                List<Map<String, Object>> categorias = firebaseService.downloadCategorias().join();
                LOGGER.info("Descargadas " + categorias.size() + " categorías desde Firebase");
                
                // TODO: Implementar lógica de inserción/actualización
                return true;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando categorías", e);
                return false;
            }
        });
    }
    
    /**
     * Descarga productos desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadProductos() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> productos = firebaseService.downloadProductos().join();
                LOGGER.info("Descargados " + productos.size() + " productos desde Firebase");
                
                // TODO: Implementar lógica de inserción/actualización
                return true;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando productos", e);
                return false;
            }
        });
    }
    
    /**
     * Descarga ventas desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadVentas() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> ventas = firebaseService.downloadVentas().join();
                LOGGER.info("Descargadas " + ventas.size() + " ventas desde Firebase");
                
                // TODO: Implementar lógica de inserción/actualización
                return true;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando ventas", e);
                return false;
            }
        });
    }
    
    /**
     * Descarga puntos de clientes desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadPuntosClientes() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> puntos = firebaseService.downloadPuntosClientes().join();
                LOGGER.info("Descargados " + puntos.size() + " registros de puntos desde Firebase");
                
                // TODO: Implementar lógica de inserción/actualización
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
                List<Map<String, Object>> cierres = firebaseService.downloadCierresCaja().join();
                LOGGER.info("Descargados " + cierres.size() + " cierres de caja desde Firebase");
                
                // TODO: Implementar lógica de inserción/actualización
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
            try {
                List<Map<String, Object>> formas = firebaseService.downloadFormasPago().join();
                LOGGER.info("Descargadas " + formas.size() + " formas de pago desde Firebase");
                
                // TODO: Implementar lógica de inserción/actualización
                return true;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando formas de pago", e);
                return false;
            }
        });
    }
    
    /**
     * Descarga impuestos desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadImpuestos() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> impuestos = firebaseService.downloadImpuestos().join();
                LOGGER.info("Descargados " + impuestos.size() + " impuestos desde Firebase");
                
                // TODO: Implementar lógica de inserción/actualización
                return true;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando impuestos", e);
                return false;
            }
        });
    }
    
    /**
     * Descarga configuraciones desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadConfiguraciones() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> configuraciones = firebaseService.downloadConfiguraciones().join();
                LOGGER.info("Descargadas " + configuraciones.size() + " configuraciones desde Firebase");
                
                // TODO: Implementar lógica de inserción/actualización
                return true;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando configuraciones", e);
                return false;
            }
        });
    }
    
    /**
     * Descarga inventario desde Firebase e inserta/actualiza en la base local
     */
    private CompletableFuture<Boolean> downloadInventario() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> inventario = firebaseService.downloadInventario().join();
                LOGGER.info("Descargado inventario con " + inventario.size() + " registros desde Firebase");
                
                // TODO: Implementar lógica de inserción/actualización
                return true;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error descargando inventario", e);
                return false;
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