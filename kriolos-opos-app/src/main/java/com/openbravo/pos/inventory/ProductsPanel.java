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

package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.user.EditorListener;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.data.user.ListProviderCreator;
import com.openbravo.data.user.DefaultSaveProvider;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.panels.JPanelTable2;
import com.openbravo.pos.ticket.ProductFilter;
import com.openbravo.pos.supabase.SupabaseServiceManager;
import com.openbravo.pos.supabase.SupabaseServiceREST;

import java.awt.Component;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 * @author JG uniCenta
 *
 */
public class ProductsPanel extends JPanelTable2 implements EditorListener {

    private static final Logger LOGGER = Logger.getLogger(ProductsPanel.class.getName());
    
    private ProductsEditor jeditor;
    private ProductFilter jproductfilter = null;        
    
    private DataLogicSales m_dlSales = null;
    
    public ProductsPanel() {
    }
    
    /**
     *
     */
    @Override
    protected void init() {   
        m_dlSales = (DataLogicSales) app.getBean("com.openbravo.pos.forms.DataLogicSales");
        
        jproductfilter = new ProductFilter();     
        jproductfilter.init(app);

        row = m_dlSales.getProductsRow();

        //lpr =  new ListProviderCreator(m_dlSales.getProductCatQBF(), jproductfilter);
        lpr =  new ListProviderCreator(m_dlSales.getProductCatQBF());

        // Usar SaveProvider normal (sin validación de administrador ni envío automático a Supabase)
        spr = new DefaultSaveProvider(
            m_dlSales.getProductCatUpdate(),
            m_dlSales.getProductCatInsert(),
            m_dlSales.getProductCatDelete());
        
        jeditor = new ProductsEditor(app, dirty);       
    }
    
    /**
     *
     * @return value
     */
    @Override
    public EditorRecord getEditor() {
        return jeditor;
    }
    
    /**
     *
     * 
    @Override
    public Component getFilter() {
        return jproductfilter.getComponent();
    }*/

    /**
     *
     * @return btnScanPal
     */
    @Override
    public Component getToolbarExtras() {
        
        // Panel para contener solo el botón ScanPal (los otros botones van en un menú)
        javax.swing.JPanel panel = new javax.swing.JPanel();
        panel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));
        
        // Botón ScanPal
        JButton btnScanPal = new JButton();
        btnScanPal.setText("ScanPal");
        btnScanPal.setVisible(app.getDeviceScanner() != null);
        btnScanPal.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnScanPalActionPerformed(evt);
            }
        });
        if (app.getDeviceScanner() != null) {
            panel.add(btnScanPal);
        }
        
        // Botón con menú desplegable para Supabase
        JButton btnSupabase = new JButton("Supabase ▼");
        btnSupabase.setToolTipText("Opciones de sincronización con Supabase");
        
        // Crear menú popup
        javax.swing.JPopupMenu popupMenu = new javax.swing.JPopupMenu();
        
        // Item para subir productos
        javax.swing.JMenuItem itemUpload = new javax.swing.JMenuItem("Subir a Supabase");
        itemUpload.setToolTipText("Subir productos a la base de datos en Supabase (requiere código de administrador)");
        itemUpload.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUploadToSupabaseActionPerformed(evt);
            }
        });
        popupMenu.add(itemUpload);
        
        // Item para actualizar productos
        javax.swing.JMenuItem itemUpdate = new javax.swing.JMenuItem("Actualizar desde Supabase");
        itemUpdate.setToolTipText("Descargar productos desde Supabase a la base de datos local");
        itemUpdate.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateFromSupabaseActionPerformed(evt);
            }
        });
        popupMenu.add(itemUpdate);
        
        // Agregar listener al botón para mostrar el menú
        btnSupabase.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popupMenu.show(btnSupabase, 0, btnSupabase.getHeight());
            }
        });
        
        // Agregar el botón al panel
        panel.add(btnSupabase);
        
        return panel;
    }
    
    /**
     * Maneja el evento de subir productos a Supabase
     */
    private void btnUploadToSupabaseActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            // Verificar conexión a internet primero
            SupabaseServiceManager manager = SupabaseServiceManager.getInstance();
            if (!manager.hasInternetConnection()) {
                JOptionPane.showMessageDialog(
                    this,
                    "❌ No hay conexión a internet.\n\n" +
                    "Por favor, verifique su conexión e intente nuevamente.",
                    "Sin Conexión",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            // Pedir código de administrador
            JTextField adminCodeField = new JTextField(20);
            JLabel label = new JLabel("Código de Administrador:");
            label.setLabelFor(adminCodeField);
            
            JPanel panel = new JPanel(new java.awt.GridBagLayout());
            java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = java.awt.GridBagConstraints.WEST;
            gbc.insets = new java.awt.Insets(5, 5, 5, 5);
            panel.add(label, gbc);
            
            gbc.gridx = 1;
            gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            panel.add(adminCodeField, gbc);
            
            int option = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Validación de Administrador - Subir Productos a Supabase",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (option != JOptionPane.OK_OPTION) {
                return; // Usuario canceló
            }
            
            String adminCode = adminCodeField.getText().trim();
            if (adminCode.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Debe ingresar un código de administrador.",
                    "Código Requerido",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            // Validar código de administrador
            boolean isValid = validateAdminCode(adminCode);
            if (!isValid) {
                JOptionPane.showMessageDialog(
                    this,
                    "❌ El código ingresado no es válido o no pertenece a un administrador.\n\n" +
                    "Solo los usuarios con permisos de administrador pueden subir productos a Supabase.",
                    "Código No Válido",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            // Obtener todos los productos que faltan en Supabase y subirlos
            List<String> productosSubidos = uploadMissingProductsToSupabase();
            
            // Mostrar resultado
            if (productosSubidos.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "ℹ️ No hay productos nuevos por subir.\n\n" +
                    "Todos los productos ya están en Supabase.",
                    "Sin Productos Nuevos",
                    JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                StringBuilder message = new StringBuilder();
                message.append("✅ Productos subidos exitosamente a Supabase.\n\n");
                message.append("Productos subidos (").append(productosSubidos.size()).append("):\n");
                for (String nombre : productosSubidos) {
                    message.append("  • ").append(nombre).append("\n");
                }
                
                JOptionPane.showMessageDialog(
                    this,
                    message.toString(),
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
            
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error subiendo productos a Supabase: " + ex.getMessage(), ex);
            JOptionPane.showMessageDialog(
                this,
                "❌ Error al subir los productos a Supabase.\n\n" +
                "Por favor, verifique su conexión e intente nuevamente.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    /**
     * Obtiene todos los productos que faltan en Supabase y los sube
     * @return Lista de nombres de productos subidos exitosamente
     */
    private List<String> uploadMissingProductsToSupabase() {
        List<String> productosSubidos = new ArrayList<>();
        
        try {
            // Inicializar Supabase
            SupabaseServiceManager manager = SupabaseServiceManager.getInstance();
            AppConfig tempConfig = new AppConfig(null);
            tempConfig.load();
            manager.initialize(tempConfig);
            SupabaseServiceREST supabase = manager.getService();
            
            // Obtener todos los productos de Supabase (solo IDs)
            Set<String> productosEnSupabase = new HashSet<>();
            try {
                List<Map<String, Object>> productosSupabase = supabase.fetchData("productos?select=id");
                for (Map<String, Object> producto : productosSupabase) {
                    Object id = producto.get("id");
                    if (id != null) {
                        productosEnSupabase.add(id.toString());
                    }
                }
                LOGGER.info("Encontrados " + productosEnSupabase.size() + " productos en Supabase");
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error obteniendo productos de Supabase: " + ex.getMessage(), ex);
            }
            
            // Obtener todos los productos locales
            com.openbravo.data.loader.Session session = m_dlSales.getSession();
            String sql = "SELECT p.ID, p.REFERENCE, p.CODE, p.CODETYPE, p.NAME, " +
                       "p.PRICEBUY, p.PRICESELL, p.CATEGORY, p.TAXCAT, " +
                       "p.ATTRIBUTESET_ID, p.IMAGE, p.ISCOM, " +
                       "p.PRINTKB, p.SENDSTATUS " +
                       "FROM products p " +
                       "ORDER BY p.NAME";
            
            PreparedStatement stmt = session.getConnection().prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            List<Map<String, Object>> productosParaSubir = new ArrayList<>();
            
            while (rs.next()) {
                String productId = rs.getString("ID");
                
                // Solo agregar si no existe en Supabase
                if (!productosEnSupabase.contains(productId)) {
                    Map<String, Object> productMap = new LinkedHashMap<>();
                    productMap.put("id", productId);
                    productMap.put("referencia", rs.getString("REFERENCE"));
                    productMap.put("codigo", rs.getString("CODE"));
                    productMap.put("tipocodigobarras", rs.getString("CODETYPE"));
                    productMap.put("nombre", rs.getString("NAME"));
                    
                    try {
                        productMap.put("preciocompra", rs.getDouble("PRICEBUY"));
                    } catch (Exception e) {
                        productMap.put("preciocompra", 0.0);
                    }
                    
                    try {
                        productMap.put("precioventa", rs.getDouble("PRICESELL"));
                    } catch (Exception e) {
                        productMap.put("precioventa", 0.0);
                    }
                    
                    productMap.put("categoriaid", rs.getString("CATEGORY"));
                    productMap.put("categoriaimpuesto", rs.getString("TAXCAT"));
                    productMap.put("atributos", rs.getString("ATTRIBUTESET_ID"));
                    productMap.put("tieneimagen", rs.getBytes("IMAGE") != null);
                    productMap.put("escompuesto", rs.getBoolean("ISCOM"));
                    productMap.put("imprimirencocina", rs.getBoolean("PRINTKB"));
                    productMap.put("estadoenvio", rs.getBoolean("SENDSTATUS"));
                    productMap.put("fechaextraccion", LocalDateTime.now()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    productMap.put("tabla", "products");
                    
                    productosParaSubir.add(productMap);
                    productosSubidos.add(rs.getString("NAME")); // Guardar nombre para el listado
                }
            }
            
            rs.close();
            stmt.close();
            
            // Subir productos que faltan
            if (!productosParaSubir.isEmpty()) {
                LOGGER.info("Subiendo " + productosParaSubir.size() + " productos a Supabase");
                boolean success = supabase.syncData("productos", productosParaSubir);
                
                if (!success) {
                    LOGGER.warning("Error al subir algunos productos a Supabase");
                    productosSubidos.clear(); // Limpiar si hubo error
                }
            } else {
                LOGGER.info("No hay productos nuevos para subir");
            }
            
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error obteniendo productos locales: " + ex.getMessage(), ex);
            productosSubidos.clear();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error subiendo productos a Supabase: " + ex.getMessage(), ex);
            productosSubidos.clear();
        }
        
        return productosSubidos;
    }
    
    private void btnScanPalActionPerformed(java.awt.event.ActionEvent evt) {                                           
  
        JDlgUploadProducts.showMessage(this, app.getDeviceScanner(), bd);
    }
    
    /**
     * Maneja el evento de actualizar productos desde Supabase
     */
    private void btnUpdateFromSupabaseActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            // Verificar conexión a internet primero
            SupabaseServiceManager manager = SupabaseServiceManager.getInstance();
            if (!manager.hasInternetConnection()) {
                JOptionPane.showMessageDialog(
                    this,
                    "❌ No hay conexión a internet.\n\n" +
                    "Por favor, verifique su conexión e intente nuevamente.",
                    "Sin Conexión",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            // Mostrar mensaje de progreso
            JOptionPane.showMessageDialog(
                this,
                "Descargando productos desde Supabase...\n\n" +
                "Por favor, espere.",
                "Actualizando",
                JOptionPane.INFORMATION_MESSAGE
            );
            
            // Descargar productos
            DownloadResult result = downloadProductsFromSupabase();
            
            // Mostrar resultado
            if (result.success) {
                StringBuilder message = new StringBuilder();
                message.append("✅ Productos actualizados exitosamente.\n\n");
                message.append("Productos insertados: ").append(result.insertados).append("\n");
                message.append("Productos actualizados: ").append(result.actualizados).append("\n");
                if (result.errores > 0) {
                    message.append("Errores: ").append(result.errores).append("\n");
                }
                
                JOptionPane.showMessageDialog(
                    this,
                    message.toString(),
                    "Actualización Completada",
                    JOptionPane.INFORMATION_MESSAGE
                );
                
                // Refrescar la lista de productos
                try {
                    bd.refreshData();
                } catch (BasicException ex) {
                    LOGGER.log(Level.WARNING, "Error refrescando datos después de actualizar: " + ex.getMessage(), ex);
                }
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "❌ Error al descargar productos desde Supabase.\n\n" +
                    "Por favor, verifique su conexión e intente nuevamente.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
            
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error actualizando productos desde Supabase: " + ex.getMessage(), ex);
            JOptionPane.showMessageDialog(
                this,
                "❌ Error al actualizar productos desde Supabase.\n\n" +
                "Por favor, verifique su conexión e intente nuevamente.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    /**
     * Clase para almacenar el resultado de la descarga
     */
    private static class DownloadResult {
        boolean success;
        int insertados;
        int actualizados;
        int errores;
    }
    
    /**
     * Descarga productos desde Supabase e inserta/actualiza en la base local
     * Verifica que no se generen duplicados (verifica por ID antes de insertar)
     */
    private DownloadResult downloadProductsFromSupabase() {
        DownloadResult result = new DownloadResult();
        result.success = false;
        result.insertados = 0;
        result.actualizados = 0;
        result.errores = 0;
        
        PreparedStatement checkStmt = null;
        PreparedStatement insertStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;
        
        try {
            // Inicializar Supabase
            SupabaseServiceManager manager = SupabaseServiceManager.getInstance();
            AppConfig tempConfig = new AppConfig(null);
            tempConfig.load();
            manager.initialize(tempConfig);
            SupabaseServiceREST supabase = manager.getService();
            
            // Obtener productos de Supabase
            List<Map<String, Object>> productos = supabase.fetchData("productos");
            LOGGER.info("Descargados " + productos.size() + " productos desde Supabase");
            
            // Obtener sesión de base de datos
            com.openbravo.data.loader.Session session = m_dlSales.getSession();
            
            // Preparar statements
            String checkSql = "SELECT ID FROM products WHERE ID = ?";
            String insertSql = "INSERT INTO products (ID, REFERENCE, CODE, CODETYPE, NAME, PRICEBUY, PRICESELL, CATEGORY, TAXCAT, ATTRIBUTESET_ID, ISCOM, PRINTKB, SENDSTATUS) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String updateSql = "UPDATE products SET REFERENCE = ?, CODE = ?, CODETYPE = ?, NAME = ?, PRICEBUY = ?, PRICESELL = ?, CATEGORY = ?, TAXCAT = ?, ATTRIBUTESET_ID = ?, ISCOM = ?, PRINTKB = ?, SENDSTATUS = ? WHERE ID = ?";
            
            checkStmt = session.getConnection().prepareStatement(checkSql);
            insertStmt = session.getConnection().prepareStatement(insertSql);
            updateStmt = session.getConnection().prepareStatement(updateSql);
            
            for (Map<String, Object> producto : productos) {
                try {
                    String id = producto.get("id") != null ? producto.get("id").toString() : null;
                    String referencia = producto.get("referencia") != null ? producto.get("referencia").toString() : null;
                    String codigo = producto.get("codigo") != null ? producto.get("codigo").toString() : null;
                    String tipocodigobarras = producto.get("tipocodigobarras") != null ? producto.get("tipocodigobarras").toString() : null;
                    String nombre = producto.get("nombre") != null ? producto.get("nombre").toString() : null;
                    
                    Double preciocompra = null;
                    if (producto.get("preciocompra") != null) {
                        try {
                            preciocompra = producto.get("preciocompra") instanceof Number ? 
                                ((Number) producto.get("preciocompra")).doubleValue() : 
                                Double.parseDouble(producto.get("preciocompra").toString());
                        } catch (Exception e) {
                            preciocompra = 0.0;
                        }
                    }
                    
                    Double precioventa = null;
                    if (producto.get("precioventa") != null) {
                        try {
                            precioventa = producto.get("precioventa") instanceof Number ? 
                                ((Number) producto.get("precioventa")).doubleValue() : 
                                Double.parseDouble(producto.get("precioventa").toString());
                        } catch (Exception e) {
                            precioventa = 0.0;
                        }
                    }
                    
                    String categoriaid = producto.get("categoriaid") != null ? producto.get("categoriaid").toString() : null;
                    String categoriaimpuesto = producto.get("categoriaimpuesto") != null ? producto.get("categoriaimpuesto").toString() : null;
                    String atributos = producto.get("atributos") != null ? producto.get("atributos").toString() : null;
                    
                    Boolean escompuesto = null;
                    if (producto.get("escompuesto") != null) {
                        escompuesto = producto.get("escompuesto") instanceof Boolean ? 
                            (Boolean) producto.get("escompuesto") : 
                            Boolean.parseBoolean(producto.get("escompuesto").toString());
                    }
                    
                    Boolean imprimirencocina = null;
                    if (producto.get("imprimirencocina") != null) {
                        imprimirencocina = producto.get("imprimirencocina") instanceof Boolean ? 
                            (Boolean) producto.get("imprimirencocina") : 
                            Boolean.parseBoolean(producto.get("imprimirencocina").toString());
                    }
                    
                    Boolean estadoenvio = null;
                    if (producto.get("estadoenvio") != null) {
                        estadoenvio = producto.get("estadoenvio") instanceof Boolean ? 
                            (Boolean) producto.get("estadoenvio") : 
                            Boolean.parseBoolean(producto.get("estadoenvio").toString());
                    }
                    
                    // Validar que tenga ID
                    if (id == null || id.trim().isEmpty()) {
                        LOGGER.warning("Producto sin ID, saltando: " + producto);
                        result.errores++;
                        continue;
                    }
                    
                    // Verificar si existe (esto previene duplicados)
                    checkStmt.setString(1, id);
                    rs = checkStmt.executeQuery();
                    boolean existe = rs.next();
                    if (rs != null) {
                        rs.close();
                        rs = null;
                    }
                    
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
                        updateStmt.setString(13, id);
                        updateStmt.executeUpdate();
                        result.actualizados++;
                        LOGGER.fine("Producto actualizado: " + id + " - " + nombre);
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
                        insertStmt.executeUpdate();
                        result.insertados++;
                        LOGGER.fine("Producto insertado: " + id + " - " + nombre);
                    }
                } catch (Exception e) {
                    result.errores++;
                    LOGGER.log(Level.WARNING, "Error procesando producto: " + producto, e);
                }
            }
            
            LOGGER.info("Productos procesados: " + result.insertados + " insertados, " + 
                       result.actualizados + " actualizados, " + result.errores + " errores");
            result.success = true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error descargando productos desde Supabase", e);
            result.success = false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (checkStmt != null) checkStmt.close();
                if (insertStmt != null) insertStmt.close();
                if (updateStmt != null) updateStmt.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error cerrando statements", e);
            }
        }
        
        return result;
    }

    /**
     *
     * @return value
     */
    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.Products");
    }

    /**
     *
     * @throws BasicException
     */
    @Override
    public void activate() throws BasicException {
        // Primero activar el editor para inicializar taxeslogic antes de que super.activate() lo necesite
        if (jeditor != null) {
            jeditor.activate(); 
        }
        if (jproductfilter != null) {
            jproductfilter.activate();
        }
        
        // Luego llamar a super.activate() que necesita taxeslogic inicializado
        super.activate();
    }

    /**
     *
     * @param value
     */
    @Override
    public void updateValue(Object value) {
        // Guardar valores de stock después de actualizar el producto
        try {
            jeditor.saveStockValues();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al guardar valores de stock", e);
        }
    }
    
    /**
     * Valida el código de administrador contra Supabase
     * Verifica que el código exista y pertenezca a un usuario con rol = 1
     * @return true si el código es válido y pertenece a un administrador
     */
    public boolean validateAdminCode(String adminCode) {
        if (adminCode == null || adminCode.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Usar SupabaseServiceManager con conexión interna
            SupabaseServiceManager manager = SupabaseServiceManager.getInstance();
            AppConfig tempConfig = new AppConfig(null);
            tempConfig.load();
            manager.initialize(tempConfig);
            SupabaseServiceREST supabase = manager.getService();
            List<Map<String, Object>> usuarios = supabase.fetchData("usuarios");
            
            // Buscar el código (tarjeta) en la lista de usuarios
            for (Map<String, Object> u : usuarios) {
                Object card = u.get("tarjeta");
                if (card == null) card = u.get("card");
                if (card != null && adminCode.trim().equals(card.toString().trim())) {
                    // Verificar que el usuario tenga rol = 1 (admin)
                    Object rol = u.get("rol");
                    if (rol != null) {
                        String rolStr = rol.toString().trim();
                        // rol = 1 son admins
                        if ("1".equals(rolStr)) {
                            return true; // Código válido y es admin
                        }
                    }
                    return false; // Código existe pero no es admin
                }
            }
            return false; // Código no encontrado
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error validando código de administrador: " + ex.getMessage(), ex);
            return false;
        }
    }
    
    /**
     * Envía un producto a Supabase después de guardarlo localmente
     * Usa el mismo formato que syncProductos() para mantener consistencia
     */
    public void sendProductToSupabase(Object[] productData) {
        try {
            // Usar SupabaseServiceManager con conexión interna
            SupabaseServiceManager manager = SupabaseServiceManager.getInstance();
            AppConfig tempConfig = new AppConfig(null);
            tempConfig.load();
            manager.initialize(tempConfig);
            SupabaseServiceREST supabase = manager.getService();
            
            // Convertir el array de datos del producto a un Map para Supabase
            // Usar el mismo formato que syncProductos() - campos en minúsculas y español
            Map<String, Object> productMap = new LinkedHashMap<>();
            
            // Mapear los campos del producto según el orden en createValue()
            // myprod[0] = ID
            // myprod[1] = REFERENCE
            // myprod[2] = CODE
            // myprod[3] = CODETYPE
            // myprod[4] = NAME
            // myprod[5] = PRICEBUY
            // myprod[6] = PRICESELL
            // myprod[7] = CATEGORY
            // myprod[8] = TAXCAT
            // myprod[9] = ATTRIBUTESET_ID
            // myprod[12] = IMAGE
            // myprod[13] = ISCOM
            // myprod[16] = PRINTKB
            // myprod[17] = SENDSTATUS
            
            if (productData.length > 0 && productData[0] != null) {
                productMap.put("id", productData[0].toString());
            }
            if (productData.length > 1 && productData[1] != null) {
                productMap.put("referencia", productData[1].toString());
            }
            if (productData.length > 2 && productData[2] != null) {
                productMap.put("codigo", productData[2].toString());
            }
            if (productData.length > 3 && productData[3] != null) {
                productMap.put("tipocodigobarras", productData[3].toString());
            }
            if (productData.length > 4 && productData[4] != null) {
                productMap.put("nombre", productData[4].toString());
            }
            if (productData.length > 5 && productData[5] != null) {
                try {
                    productMap.put("preciocompra", productData[5] instanceof Number ? 
                        ((Number) productData[5]).doubleValue() : Double.parseDouble(productData[5].toString()));
                } catch (Exception e) {
                    productMap.put("preciocompra", 0.0);
                }
            }
            if (productData.length > 6 && productData[6] != null) {
                try {
                    productMap.put("precioventa", productData[6] instanceof Number ? 
                        ((Number) productData[6]).doubleValue() : Double.parseDouble(productData[6].toString()));
                } catch (Exception e) {
                    productMap.put("precioventa", 0.0);
                }
            }
            if (productData.length > 7 && productData[7] != null) {
                productMap.put("categoriaid", productData[7].toString());
            }
            if (productData.length > 8 && productData[8] != null) {
                productMap.put("categoriaimpuesto", productData[8].toString());
            }
            if (productData.length > 9 && productData[9] != null) {
                productMap.put("atributos", productData[9].toString());
            }
            // IMAGE - verificar si tiene imagen
            if (productData.length > 12 && productData[12] != null) {
                productMap.put("tieneimagen", true);
            } else {
                productMap.put("tieneimagen", false);
            }
            if (productData.length > 13 && productData[13] != null) {
                productMap.put("escompuesto", productData[13] instanceof Boolean ? 
                    (Boolean) productData[13] : Boolean.parseBoolean(productData[13].toString()));
            }
            if (productData.length > 16 && productData[16] != null) {
                productMap.put("imprimirencocina", productData[16] instanceof Boolean ? 
                    (Boolean) productData[16] : Boolean.parseBoolean(productData[16].toString()));
            }
            if (productData.length > 17 && productData[17] != null) {
                productMap.put("estadoenvio", productData[17] instanceof Boolean ? 
                    (Boolean) productData[17] : Boolean.parseBoolean(productData[17].toString()));
            }
            
            // Campos adicionales para fecha y tabla
            productMap.put("fechaextraccion", java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            productMap.put("tabla", "products");
            
            // Enviar a Supabase usando syncData (hace UPSERT) - tabla "productos"
            boolean success = supabase.syncData("productos", java.util.Collections.singletonList(productMap));
            
            if (success) {
                LOGGER.info("Producto enviado exitosamente a Supabase (tabla productos): " + productMap.get("nombre"));
            } else {
                LOGGER.warning("Error al enviar producto a Supabase (tabla productos): " + productMap.get("nombre"));
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error enviando producto a Supabase: " + ex.getMessage(), ex);
            ex.printStackTrace();
        }
    }
}