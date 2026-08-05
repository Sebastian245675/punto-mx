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
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
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

        // lpr = new ListProviderCreator(m_dlSales.getProductCatQBF(), jproductfilter);
        lpr = new ListProviderCreator(m_dlSales.getProductCatQBF());

        // Usar SaveProvider con validación de administrador y envío automático a
        // Supabase
        spr = new ProductSaveProviderWithAdminValidation(new DefaultSaveProvider(
                m_dlSales.getProductCatUpdate(),
                m_dlSales.getProductCatInsert(),
                m_dlSales.getProductCatDelete()), this);

        jeditor = new ProductsEditor(app, dirty);
        
        // Aplicar tamaño de letra gigante recursivamente a todo el panel de Productos
        setLargeFont(this);
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
     * @Override
     *           public Component getFilter() {
     *           return jproductfilter.getComponent();
     *           }
     */

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

        // Item para subir productos (Deshabilitado)
        javax.swing.JMenuItem itemUpload = new javax.swing.JMenuItem("Subir a Supabase (Deshabilitado)");
        itemUpload.setEnabled(false);
        itemUpload.setToolTipText("La sincronización de productos ha sido deshabilitada");
        popupMenu.add(itemUpload);

        // Item para actualizar productos (Deshabilitado)
        javax.swing.JMenuItem itemUpdate = new javax.swing.JMenuItem("Actualizar desde Supabase (Deshabilitado)");
        itemUpdate.setEnabled(false);
        itemUpdate.setToolTipText("La sincronización de productos ha sido deshabilitada");
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

        // Botón Exportar Excel
        JButton btnExportExcel = new JButton("Exportar Excel");
        btnExportExcel.setToolTipText("Exportar todos los productos con toda su información a Excel");
        btnExportExcel.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportExcelActionPerformed(evt);
            }
        });
        panel.add(btnExportExcel);

        // Botón Importar Excel
        JButton btnImportExcel = new JButton("Importar Excel");
        btnImportExcel.setToolTipText("Importar productos con toda su información desde un archivo Excel/TSV");
        btnImportExcel.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImportExcelActionPerformed(evt);
            }
        });
        panel.add(btnImportExcel);

        return panel;
    }

    /**
     * Maneja el evento de subir productos a Supabase
     */
    private void btnUploadToSupabaseActionPerformed(java.awt.event.ActionEvent evt) {
        JOptionPane.showMessageDialog(this, "La sincronización de productos está deshabilitada.", "Sincronización Deshabilitada", JOptionPane.INFORMATION_MESSAGE);
        /*
        try {
            // Verificar conexión a internet primero
            SupabaseServiceManager manager = SupabaseServiceManager.getInstance();
            if (!manager.hasInternetConnection()) {
                JOptionPane.showMessageDialog(
                        this,
                        "❌ No hay conexión a internet.\n\n" +
                                "Por favor, verifique su conexión e intente nuevamente.",
                        "Sin Conexión",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Obtener todos los productos que faltan en Supabase y subirlos directamente
            List<String> productosSubidos = uploadMissingProductsToSupabase();

            // Mostrar resultado
            if (productosSubidos == null) {
                // Hubo un error durante la subida
                // Verificar si el error es porque la tabla no existe
                String errorMsg = "❌ Error al subir los productos a Supabase.\n\n";
                errorMsg += "⚠️ La tabla 'productos' no existe en Supabase.\n\n";
                errorMsg += "Para solucionar esto:\n";
                errorMsg += "1. Ve al panel de Supabase (https://supabase.com/dashboard)\n";
                errorMsg += "2. Crea la tabla 'productos' con los campos necesarios\n";
                errorMsg += "3. O verifica que la tabla tenga el nombre correcto\n\n";
                errorMsg += "Si el problema persiste, verifica:\n";
                errorMsg += "• La conexión a internet\n";
                errorMsg += "• La configuración de Supabase (URL y API Key)\n";

                JOptionPane.showMessageDialog(
                        this,
                        errorMsg,
                        "Tabla No Encontrada",
                        JOptionPane.ERROR_MESSAGE);
            } else if (productosSubidos.isEmpty()) {
                // No hay productos para subir
                JOptionPane.showMessageDialog(
                        this,
                        "ℹ️ No hay productos para subir.\n\n" +
                                "No hay productos en la base de datos local.",
                        "Sin Productos",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Éxito: productos sincronizados (insertados nuevos o actualizados existentes)
                StringBuilder message = new StringBuilder();
                message.append("✅ Productos sincronizados exitosamente con Supabase.\n\n");
                message.append("Se procesaron ").append(productosSubidos.size()).append(" productos.\n");
                message.append("(Nuevos productos se insertaron, productos existentes se actualizaron)\n\n");
                if (productosSubidos.size() <= 10) {
                    message.append("Productos procesados:\n");
                    for (String nombre : productosSubidos) {
                        message.append("  • ").append(nombre).append("\n");
                    }
                } else {
                    message.append("Primeros 10 productos:\n");
                    for (int i = 0; i < 10 && i < productosSubidos.size(); i++) {
                        message.append("  • ").append(productosSubidos.get(i)).append("\n");
                    }
                    message.append("  ... y ").append(productosSubidos.size() - 10).append(" más");
                }

                JOptionPane.showMessageDialog(
                        this,
                        message.toString(),
                        "Sincronización Exitosa",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error subiendo productos a Supabase: " + ex.getMessage(), ex);
            JOptionPane.showMessageDialog(
                    this,
                    "❌ Error inesperado al subir los productos a Supabase.\n\n" +
                            "Por favor, verifique su conexión e intente nuevamente.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        */
    }

    /**
     * Sube TODOS los productos locales a Supabase usando UPSERT (inserta nuevos,
     * actualiza existentes)
     * Esto permite actualizar productos modificados localmente
     * 
     * @return Lista de nombres de productos procesados exitosamente, o null si hubo
     *         error
     */
    private List<String> uploadMissingProductsToSupabase() {
        List<String> productosProcesados = new ArrayList<>();
        boolean huboErrorAlSubir = false;

        try {
            // Inicializar Supabase
            SupabaseServiceManager manager = SupabaseServiceManager.getInstance();
            AppConfig tempConfig = new AppConfig(null);
            tempConfig.load();
            manager.initialize(tempConfig);

            // Verificar conexión a internet antes de continuar
            if (!manager.hasInternetConnection()) {
                LOGGER.warning("No hay conexión a internet para subir productos");
                huboErrorAlSubir = true;
                return null;
            }

            SupabaseServiceREST supabase = manager.getService();

            // Obtener todos los productos locales (TODOS, no solo los que faltan)
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

                // Agregar TODOS los productos locales (syncData hará UPSERT: inserta nuevos,
                // actualiza existentes)
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
                productosProcesados.add(rs.getString("NAME")); // Guardar nombre para el listado
            }

            rs.close();
            stmt.close();

            // Subir TODOS los productos usando syncData (UPSERT: inserta nuevos, actualiza
            // existentes)
            if (!productosParaSubir.isEmpty()) {
                LOGGER.info("Subiendo " + productosParaSubir.size()
                        + " productos a Supabase (UPSERT: inserta nuevos, actualiza existentes)");
                // Usar syncData que hace UPSERT - inserta si no existe, actualiza si existe
                boolean success = supabase.syncData("productos", productosParaSubir);

                if (!success) {
                    huboErrorAlSubir = true;
                    productosProcesados.clear();
                } else {
                    LOGGER.info("Productos sincronizados exitosamente: " + productosParaSubir.size());
                }
            } else {
                LOGGER.info("No hay productos para subir");
            }

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error obteniendo productos locales: " + ex.getMessage(), ex);
            productosProcesados.clear();
            huboErrorAlSubir = true;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error subiendo productos a Supabase: " + ex.getMessage(), ex);
            productosProcesados.clear();
            huboErrorAlSubir = true;
        }

        // Si hubo error al subir, retornar null para indicar error
        if (huboErrorAlSubir) {
            return null;
        }

        return productosProcesados;
    }

    private void btnScanPalActionPerformed(java.awt.event.ActionEvent evt) {

        JDlgUploadProducts.showMessage(this, app.getDeviceScanner(), bd);
    }

    /**
     * Maneja el evento de actualizar productos desde Supabase
     */
    private void btnUpdateFromSupabaseActionPerformed(java.awt.event.ActionEvent evt) {
        JOptionPane.showMessageDialog(this, "La sincronización de productos está deshabilitada.", "Sincronización Deshabilitada", JOptionPane.INFORMATION_MESSAGE);
        /*
        try {
            // Verificar conexión a internet primero
            SupabaseServiceManager manager = SupabaseServiceManager.getInstance();
            if (!manager.hasInternetConnection()) {
                JOptionPane.showMessageDialog(
                        this,
                        "❌ No hay conexión a internet.\n\n" +
                                "Por favor, verifique su conexión e intente nuevamente.",
                        "Sin Conexión",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Mostrar mensaje de progreso
            JOptionPane.showMessageDialog(
                    this,
                    "Descargando productos desde Supabase...\n\n" +
                            "Por favor, espere.",
                    "Actualizando",
                    JOptionPane.INFORMATION_MESSAGE);

            // Descargar productos
            DownloadResult result = downloadProductsFromSupabase();

            // Mostrar resultado
            if (result.success) {
                StringBuilder message = new StringBuilder();
                message.append("✅ Productos sincronizados desde Supabase.\n\n");
                message.append("Productos insertados (nuevos): ").append(result.insertados).append("\n");
                message.append("Productos actualizados (existentes): ").append(result.actualizados).append("\n");
                if (result.errores > 0) {
                    message.append("Errores: ").append(result.errores).append("\n");
                }
                message.append("\n");
                message.append("ℹ️ Nota: Los productos locales que no están en Supabase se mantienen intactos.\n");
                message.append("Solo se insertan productos nuevos o se actualizan productos existentes.");

                JOptionPane.showMessageDialog(
                        this,
                        message.toString(),
                        "Sincronización Completada",
                        JOptionPane.INFORMATION_MESSAGE);

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
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error actualizando productos desde Supabase: " + ex.getMessage(), ex);
            JOptionPane.showMessageDialog(
                    this,
                    "❌ Error al actualizar productos desde Supabase.\n\n" +
                            "Por favor, verifique su conexión e intente nuevamente.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        */
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
     * IMPORTANTE: NO borra productos locales que no estén en Supabase
     * Solo inserta productos nuevos o actualiza productos existentes
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

            // IMPORTANTE: Solo insertamos/actualizamos productos de Supabase
            // NO borramos productos locales que no estén en Supabase
            // Esto preserva los productos locales que aún no se han subido

            // Obtener sesión de base de datos
            com.openbravo.data.loader.Session session = m_dlSales.getSession();

            // Preparar statements
            String checkSql = "SELECT ID FROM products WHERE ID = ?";
            String insertSql = "INSERT INTO products (ID, REFERENCE, CODE, CODETYPE, NAME, PRICEBUY, PRICESELL, CATEGORY, TAXCAT, ATTRIBUTESET_ID, ISCOM, PRINTKB, SENDSTATUS) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String updateSql = "UPDATE products SET REFERENCE = ?, CODE = ?, CODETYPE = ?, NAME = ?, PRICEBUY = ?, PRICESELL = ?, CATEGORY = ?, TAXCAT = ?, ATTRIBUTESET_ID = ?, ISCOM = ?, PRINTKB = ?, SENDSTATUS = ? WHERE ID = ?";

            checkStmt = session.getConnection().prepareStatement(checkSql);
            insertStmt = session.getConnection().prepareStatement(insertSql);
            updateStmt = session.getConnection().prepareStatement(updateSql);

            for (Map<String, Object> producto : productos) {
                try {
                    String id = producto.get("id") != null ? producto.get("id").toString() : null;
                    String referencia = producto.get("referencia") != null ? producto.get("referencia").toString()
                            : null;
                    String codigo = producto.get("codigo") != null ? producto.get("codigo").toString() : null;
                    String tipocodigobarras = producto.get("tipocodigobarras") != null
                            ? producto.get("tipocodigobarras").toString()
                            : null;
                    String nombre = producto.get("nombre") != null ? producto.get("nombre").toString() : null;

                    Double preciocompra = null;
                    if (producto.get("preciocompra") != null) {
                        try {
                            preciocompra = producto.get("preciocompra") instanceof Number
                                    ? ((Number) producto.get("preciocompra")).doubleValue()
                                    : Double.parseDouble(producto.get("preciocompra").toString());
                        } catch (Exception e) {
                            preciocompra = 0.0;
                        }
                    }

                    Double precioventa = null;
                    if (producto.get("precioventa") != null) {
                        try {
                            precioventa = producto.get("precioventa") instanceof Number
                                    ? ((Number) producto.get("precioventa")).doubleValue()
                                    : Double.parseDouble(producto.get("precioventa").toString());
                        } catch (Exception e) {
                            precioventa = 0.0;
                        }
                    }

                    String categoriaid = producto.get("categoriaid") != null ? producto.get("categoriaid").toString()
                            : null;
                    String categoriaimpuesto = producto.get("categoriaimpuesto") != null
                            ? producto.get("categoriaimpuesto").toString()
                            : null;
                    String atributos = producto.get("atributos") != null ? producto.get("atributos").toString() : null;

                    Boolean escompuesto = null;
                    if (producto.get("escompuesto") != null) {
                        escompuesto = producto.get("escompuesto") instanceof Boolean
                                ? (Boolean) producto.get("escompuesto")
                                : Boolean.parseBoolean(producto.get("escompuesto").toString());
                    }

                    Boolean imprimirencocina = null;
                    if (producto.get("imprimirencocina") != null) {
                        imprimirencocina = producto.get("imprimirencocina") instanceof Boolean
                                ? (Boolean) producto.get("imprimirencocina")
                                : Boolean.parseBoolean(producto.get("imprimirencocina").toString());
                    }

                    Boolean estadoenvio = null;
                    if (producto.get("estadoenvio") != null) {
                        estadoenvio = producto.get("estadoenvio") instanceof Boolean
                                ? (Boolean) producto.get("estadoenvio")
                                : Boolean.parseBoolean(producto.get("estadoenvio").toString());
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
                if (rs != null)
                    rs.close();
                if (checkStmt != null)
                    checkStmt.close();
                if (insertStmt != null)
                    insertStmt.close();
                if (updateStmt != null)
                    updateStmt.close();
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
        // Primero activar el editor para inicializar taxeslogic antes de que
        // super.activate() lo necesite
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
        // NO llamar saveStockValues() aquí.
        // Este método se invoca desde fireDataBrowse() DESPUÉS de que setValues()
        // ya reseteó m_jStockCurrent a "0". Llamar saveStockValues() aquí leería "0"
        // del campo y sobrescribiría el stock correcto que ya se guardó en createValue().
        // El stock se guarda correctamente en ProductsEditor.createValue() -> saveStockValues().
    }

    /**
     * Verifica si el usuario actual tiene permisos de administrador (rol = 1)
     * 
     * @return true si el usuario actual es administrador o mánager
     */
    public boolean isCurrentUserAdmin() {
        if (app == null || app.getAppUserView() == null || app.getAppUserView().getUser() == null) {
            return false;
        }
        String roleId = app.getAppUserView().getUser().getRole();
        // rol = 1 (ADMIN), rol = 0 (Visitas/especiales si existen)
        return "1".equals(roleId) || "0".equals(roleId);
    }

    /**
     * Valida el código de administrador contra Supabase
     * Verifica que el código exista y pertenezca a un usuario con rol = 1
     * 
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
                if (card == null)
                    card = u.get("card");
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
                    productMap.put("preciocompra",
                            productData[5] instanceof Number ? ((Number) productData[5]).doubleValue()
                                    : Double.parseDouble(productData[5].toString()));
                } catch (Exception e) {
                    productMap.put("preciocompra", 0.0);
                }
            }
            if (productData.length > 6 && productData[6] != null) {
                try {
                    productMap.put("precioventa",
                            productData[6] instanceof Number ? ((Number) productData[6]).doubleValue()
                                    : Double.parseDouble(productData[6].toString()));
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
                productMap.put("escompuesto", productData[13] instanceof Boolean ? (Boolean) productData[13]
                        : Boolean.parseBoolean(productData[13].toString()));
            }
            if (productData.length > 16 && productData[16] != null) {
                productMap.put("imprimirencocina", productData[16] instanceof Boolean ? (Boolean) productData[16]
                        : Boolean.parseBoolean(productData[16].toString()));
            }
            if (productData.length > 17 && productData[17] != null) {
                productMap.put("estadoenvio", productData[17] instanceof Boolean ? (Boolean) productData[17]
                        : Boolean.parseBoolean(productData[17].toString()));
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

    /**
     * Exporta todos los productos de la base de datos a un archivo Excel (.xls) usando tabulaciones.
     * Esta técnica permite abrir el archivo directamente en Excel sin cuadros de diálogo de importación
     * y con todos los caracteres en español (acentos, eñes) perfectamente codificados mediante UTF-8 BOM.
     */
    private void btnExportExcelActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setDialogTitle("Exportar Productos a Excel");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos de Excel (*.xls)", "xls"));
        
        // Sugerir nombre de archivo con la fecha de hoy
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String defaultName = "productos_" + java.time.LocalDateTime.now().format(dtf) + ".xls";
        fileChooser.setSelectedFile(new java.io.File(defaultName));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == javax.swing.JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();
            
            // Forzar extensión .xls
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".xls")) {
                fileToSave = new java.io.File(filePath + ".xls");
            }

            try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                    new java.io.OutputStreamWriter(new java.io.FileOutputStream(fileToSave), java.nio.charset.StandardCharsets.UTF_8))) {
                
                // Escribir la marca de orden de bytes (BOM) UTF-8 (\uFEFF) para compatibilidad total con Excel en español
                writer.write("\uFEFF");

                // Escribir cabeceras de columnas en español
                writer.write("Referencia\tCódigo de Barras\tNombre\tPrecio Costo\tPrecio Venta\tCategoría\tImpuesto\tUnidad\tProveedor\tStock Actual\tStock Mínimo\tEs Kit\tEs Granel\tEs Paquete\tImprimir Cocina\tAcumula Puntos\n");

                // Obtener conexión a base de datos local usando la sesión activa de Openbravo
                com.openbravo.data.loader.Session session = m_dlSales.getSession();
                
                // Query SQL robusta con joins para obtener TODA la información enriquecida de los productos
                String sql = "SELECT p.REFERENCE, p.CODE, p.NAME, p.PRICEBUY, p.PRICESELL, " +
                        "c.NAME AS CATEGORY_NAME, t.NAME AS TAX_NAME, u.NAME AS UOM_NAME, s.NAME AS SUPPLIER_NAME, " +
                        "COALESCE(st.UNITS, 0.0) AS STOCK_CURRENT, COALESCE(sl.STOCKSECURITY, 0.0) AS STOCK_MIN, " +
                        "p.ISCOM, p.ISSCALE, p.ISCONSTANT, p.PRINTKB, p.ACCUMULATES_POINTS " +
                        "FROM products p " +
                        "LEFT JOIN categories c ON p.CATEGORY = c.ID " +
                        "LEFT JOIN taxcategories t ON p.TAXCAT = t.ID " +
                        "LEFT JOIN uom u ON p.UOM = u.ID " +
                        "LEFT JOIN suppliers s ON p.SUPPLIER = s.ID " +
                        "LEFT JOIN (SELECT PRODUCT, SUM(UNITS) AS UNITS FROM stockcurrent GROUP BY PRODUCT) st ON p.ID = st.PRODUCT " +
                        "LEFT JOIN (SELECT PRODUCT, SUM(STOCKSECURITY) AS STOCKSECURITY FROM stocklevel GROUP BY PRODUCT) sl ON p.ID = sl.PRODUCT " +
                        "ORDER BY p.NAME";

                java.sql.PreparedStatement stmt = session.getConnection().prepareStatement(sql);
                java.sql.ResultSet rs = stmt.executeQuery();

                int count = 0;
                while (rs.next()) {
                    String reference = rs.getString("REFERENCE");
                    String code = rs.getString("CODE");
                    String name = rs.getString("NAME");
                    double priceBuy = rs.getDouble("PRICEBUY");
                    double priceSell = rs.getDouble("PRICESELL");
                    String categoryName = rs.getString("CATEGORY_NAME");
                    String taxName = rs.getString("TAX_NAME");
                    String uomName = rs.getString("UOM_NAME");
                    String supplierName = rs.getString("SUPPLIER_NAME");
                    double stockCurrent = rs.getDouble("STOCK_CURRENT");
                    double stockMin = rs.getDouble("STOCK_MIN");
                    boolean isCom = rs.getBoolean("ISCOM");
                    boolean isScale = rs.getBoolean("ISSCALE");
                    boolean isConstant = rs.getBoolean("ISCONSTANT");
                    boolean printKb = rs.getBoolean("PRINTKB");
                    boolean accumulatePoints = rs.getBoolean("ACCUMULATES_POINTS");

                    // Tratar nulos limpiamente para evitar textos "null" en Excel
                    reference = reference == null ? "" : reference.trim();
                    code = code == null ? "" : code.trim();
                    name = name == null ? "" : name.trim();
                    categoryName = categoryName == null ? "" : categoryName.trim();
                    taxName = taxName == null ? "" : taxName.trim();
                    uomName = uomName == null ? "" : uomName.trim();
                    supplierName = supplierName == null ? "" : supplierName.trim();

                    // Escribir registro delimitado por tabulador
                    writer.write(String.format("%s\t%s\t%s\t%.2f\t%.2f\t%s\t%s\t%s\t%s\t%.2f\t%.2f\t%s\t%s\t%s\t%s\t%s\n",
                            reference,
                            code,
                            name,
                            priceBuy,
                            priceSell,
                            categoryName,
                            taxName,
                            uomName,
                            supplierName,
                            stockCurrent,
                            stockMin,
                            isCom ? "SÍ" : "NO",
                            isScale ? "SÍ" : "NO",
                            isConstant ? "SÍ" : "NO",
                            printKb ? "SÍ" : "NO",
                            accumulatePoints ? "SÍ" : "NO"
                    ));
                    count++;
                }

                rs.close();
                stmt.close();

                JOptionPane.showMessageDialog(this,
                        "✅ Se han exportado " + count + " productos correctamente a Excel con toda su información.",
                        "Exportación Exitosa",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error exportando productos a Excel: " + e.getMessage(), e);
                JOptionPane.showMessageDialog(this,
                        "❌ Error al exportar los productos a Excel:\n" + e.getMessage(),
                        "Error de Exportación",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Parsea un string a double de manera segura y compatible con formatos regionales.
     */
    private double parseDoubleSafely(String val) {
        if (val == null || val.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(val.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Aplica el tipo de letra Segoe UI 24 de manera recursiva a todos los componentes
     * del panel de productos para una perfecta visualización y legibilidad.
     */
    private void setLargeFont(java.awt.Component comp) {
        if (comp == null) return;
        
        java.awt.Font currentFont = comp.getFont();
        if (currentFont == null || currentFont.getSize() < 24) {
            comp.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 24));
        }
        
        if (comp instanceof javax.swing.JTable) {
            javax.swing.JTable t = (javax.swing.JTable) comp;
            t.setRowHeight(32);
            t.getTableHeader().setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        }
        if (comp instanceof javax.swing.text.JTextComponent) {
            comp.setPreferredSize(new java.awt.Dimension(comp.getPreferredSize().width, 36));
        }
        if (comp instanceof java.awt.Container) {
            for (java.awt.Component child : ((java.awt.Container) comp).getComponents()) {
                setLargeFont(child);
            }
        }
    }

    /**
     * Importa productos y niveles de stock desde un archivo de Excel/TSV.
     * Lee cabeceras, identifica productos por código de barras o referencia,
     * actualiza si existen, crea nuevos si no, resuelve categorías/proveedores/impuestos
     * por nombre (creándolos dinámicamente si es necesario) y actualiza el inventario actual
     * y stock mínimo de seguridad en la base de datos de manera atómica y segura.
     */
    private void btnImportExcelActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setDialogTitle("Importar Productos desde Excel");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos de Excel (*.xls, *.tsv)", "xls", "tsv"));

        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == javax.swing.JFileChooser.APPROVE_OPTION) {
            java.io.File fileToLoad = fileChooser.getSelectedFile();

            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.io.FileInputStream(fileToLoad), java.nio.charset.StandardCharsets.UTF_8))) {

                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new Exception("El archivo seleccionado está vacío.");
                }

                // Detectar UTF-8 BOM y removerlo si está presente
                if (headerLine.startsWith("\uFEFF")) {
                    headerLine = headerLine.substring(1);
                }

                String[] headers = headerLine.split("\t");
                if (headers.length < 5) {
                    throw new Exception("Formato inválido. El archivo debe contener al menos las columnas principales separadas por tabuladores (Referencia, Código, Nombre, Precio Costo, Precio Venta).");
                }

                com.openbravo.data.loader.Session session = m_dlSales.getSession();
                java.sql.Connection conn = session.getConnection();
                
                // Desactivar temporalmente autocommit para hacer el proceso transaccional
                boolean originalAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);

                // Sentencias preparadas reutilizables para máxima performance y protección contra SQL Injection
                java.sql.PreparedStatement selectProductStmt = conn.prepareStatement(
                        "SELECT ID FROM products WHERE CODE = ? OR REFERENCE = ?");
                
                java.sql.PreparedStatement insertProductStmt = conn.prepareStatement(
                        "INSERT INTO products (ID, REFERENCE, CODE, NAME, PRICEBUY, PRICESELL, CATEGORY, TAXCAT, UOM, SUPPLIER, ISCOM, ISSCALE, ISCONSTANT, PRINTKB, ACCUMULATES_POINTS, CODETYPE) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'EAN13')");
                
                java.sql.PreparedStatement updateProductStmt = conn.prepareStatement(
                        "UPDATE products SET REFERENCE = ?, CODE = ?, NAME = ?, PRICEBUY = ?, PRICESELL = ?, CATEGORY = ?, TAXCAT = ?, UOM = ?, SUPPLIER = ?, ISCOM = ?, ISSCALE = ?, ISCONSTANT = ?, PRINTKB = ?, ACCUMULATES_POINTS = ? " +
                        "WHERE ID = ?");

                // Consultas para resolución de IDs por nombre
                java.sql.PreparedStatement selectCategoryStmt = conn.prepareStatement("SELECT ID FROM categories WHERE NAME = ?");
                java.sql.PreparedStatement insertCategoryStmt = conn.prepareStatement("INSERT INTO categories (ID, NAME) VALUES (?, ?)");
                java.sql.PreparedStatement selectFirstCategoryStmt = conn.prepareStatement("SELECT ID FROM categories ORDER BY NAME LIMIT 1");
                
                java.sql.PreparedStatement selectTaxStmt = conn.prepareStatement("SELECT ID FROM taxcategories WHERE NAME = ?");
                java.sql.PreparedStatement selectFirstTaxStmt = conn.prepareStatement("SELECT ID FROM taxcategories ORDER BY NAME LIMIT 1");
                
                java.sql.PreparedStatement selectUomStmt = conn.prepareStatement("SELECT ID FROM uom WHERE NAME = ?");
                java.sql.PreparedStatement selectFirstUomStmt = conn.prepareStatement("SELECT ID FROM uom ORDER BY NAME LIMIT 1");
                
                java.sql.PreparedStatement selectSupplierStmt = conn.prepareStatement("SELECT ID FROM suppliers WHERE NAME = ?");
                java.sql.PreparedStatement insertSupplierStmt = conn.prepareStatement("INSERT INTO suppliers (ID, NAME) VALUES (?, ?)");

                // Consultas para inventario
                String defaultLocation = app.getInventoryLocation();
                if (defaultLocation == null || defaultLocation.isEmpty()) {
                    defaultLocation = "0"; // Fallback a ubicación 0
                }
                
                java.sql.PreparedStatement updateStockStmt = conn.prepareStatement(
                        "UPDATE stockcurrent SET UNITS = ? WHERE LOCATION = ? AND PRODUCT = ? AND (ATTRIBUTESETINSTANCE_ID IS NULL OR ATTRIBUTESETINSTANCE_ID = '')");
                
                java.sql.PreparedStatement insertStockStmt = conn.prepareStatement(
                        "INSERT INTO stockcurrent (LOCATION, PRODUCT, ATTRIBUTESETINSTANCE_ID, UNITS) VALUES (?, ?, NULL, ?)");
                
                java.sql.PreparedStatement updateStockLevelStmt = conn.prepareStatement(
                        "UPDATE stocklevel SET STOCKSECURITY = ? WHERE LOCATION = ? AND PRODUCT = ?");
                
                java.sql.PreparedStatement insertStockLevelStmt = conn.prepareStatement(
                        "INSERT INTO stocklevel (ID, LOCATION, PRODUCT, STOCKSECURITY, STOCKMAXIMUM) VALUES (?, ?, ?, ?, 0.0)");

                // Variables de control por defecto
                String defaultCategoryId = "";
                java.sql.ResultSet catRS = selectFirstCategoryStmt.executeQuery();
                if (catRS.next()) {
                    defaultCategoryId = catRS.getString("ID");
                }
                catRS.close();
                
                if (defaultCategoryId.isEmpty()) {
                    // Si no hay categorías, crear una por defecto
                    defaultCategoryId = java.util.UUID.randomUUID().toString();
                    insertCategoryStmt.setString(1, defaultCategoryId);
                    insertCategoryStmt.setString(2, "General");
                    insertCategoryStmt.executeUpdate();
                }

                String defaultTaxId = "";
                java.sql.ResultSet taxRS = selectFirstTaxStmt.executeQuery();
                if (taxRS.next()) {
                    defaultTaxId = taxRS.getString("ID");
                }
                taxRS.close();

                String defaultUomId = null;
                java.sql.ResultSet uomRS = selectFirstUomStmt.executeQuery();
                if (uomRS.next()) {
                    defaultUomId = uomRS.getString("ID");
                }
                uomRS.close();

                String line;
                int createdCount = 0;
                int updatedCount = 0;
                int lineCount = 0;

                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    if (line.trim().isEmpty()) continue;

                    String[] fields = line.split("\t");
                    if (fields.length < 3) continue; // Saltear líneas vacías o incompletas

                    // Mapear campos con valores seguros
                    String reference = fields.length > 0 ? fields[0].trim() : "";
                    String barcode = fields.length > 1 ? fields[1].trim() : "";
                    String name = fields.length > 2 ? fields[2].trim() : "";
                    
                    if (name.isEmpty()) continue; // Nombre obligatorio

                    // Si referencia está vacía, usar el código de barras
                    if (reference.isEmpty() && !barcode.isEmpty()) {
                        reference = barcode;
                    }
                    if (barcode.isEmpty() && !reference.isEmpty()) {
                        barcode = reference;
                    }

                    double priceBuy = fields.length > 3 ? parseDoubleSafely(fields[3]) : 0.0;
                    double priceSell = fields.length > 4 ? parseDoubleSafely(fields[4]) : 0.0;
                    String categoryName = fields.length > 5 ? fields[5].trim() : "";
                    String taxName = fields.length > 6 ? fields[6].trim() : "";
                    String uomName = fields.length > 7 ? fields[7].trim() : "";
                    String supplierName = fields.length > 8 ? fields[8].trim() : "";
                    double stockCurrent = fields.length > 9 ? parseDoubleSafely(fields[9]) : 0.0;
                    double stockMin = fields.length > 10 ? parseDoubleSafely(fields[10]) : 0.0;
                    
                    boolean isCom = fields.length > 11 ? "SÍ".equalsIgnoreCase(fields[11].trim()) : false;
                    boolean isScale = fields.length > 12 ? "SÍ".equalsIgnoreCase(fields[12].trim()) : false;
                    boolean isConstant = fields.length > 13 ? "SÍ".equalsIgnoreCase(fields[13].trim()) : false;
                    boolean printKb = fields.length > 14 ? "SÍ".equalsIgnoreCase(fields[14].trim()) : false;
                    boolean accumulatePoints = fields.length > 15 ? "SÍ".equalsIgnoreCase(fields[15].trim()) : false;

                    // 1. Resolver Categoría
                    String categoryId = defaultCategoryId;
                    if (!categoryName.isEmpty()) {
                        selectCategoryStmt.setString(1, categoryName);
                        java.sql.ResultSet rsCat = selectCategoryStmt.executeQuery();
                        if (rsCat.next()) {
                            categoryId = rsCat.getString("ID");
                        } else {
                            // Crear nueva categoría si no existe
                            categoryId = java.util.UUID.randomUUID().toString();
                            insertCategoryStmt.setString(1, categoryId);
                            insertCategoryStmt.setString(2, categoryName);
                            insertCategoryStmt.executeUpdate();
                        }
                        rsCat.close();
                    }

                    // 2. Resolver Impuesto
                    String taxId = defaultTaxId;
                    if (!taxName.isEmpty()) {
                        selectTaxStmt.setString(1, taxName);
                        java.sql.ResultSet rsTax = selectTaxStmt.executeQuery();
                        if (rsTax.next()) {
                            taxId = rsTax.getString("ID");
                        }
                        rsTax.close();
                    }

                    // 3. Resolver UOM (Unidad de Medida)
                    String uomId = defaultUomId;
                    if (!uomName.isEmpty()) {
                        selectUomStmt.setString(1, uomName);
                        java.sql.ResultSet rsUom = selectUomStmt.executeQuery();
                        if (rsUom.next()) {
                            uomId = rsUom.getString("ID");
                        }
                        rsUom.close();
                    }

                    // 4. Resolver Proveedor
                    String supplierId = null;
                    if (!supplierName.isEmpty()) {
                        selectSupplierStmt.setString(1, supplierName);
                        java.sql.ResultSet rsSup = selectSupplierStmt.executeQuery();
                        if (rsSup.next()) {
                            supplierId = rsSup.getString("ID");
                        } else {
                            // Crear nuevo proveedor si no existe
                            supplierId = java.util.UUID.randomUUID().toString();
                            insertSupplierStmt.setString(1, supplierId);
                            insertSupplierStmt.setString(2, supplierName);
                            insertSupplierStmt.executeUpdate();
                        }
                        rsSup.close();
                    }

                    // 5. Verificar si el producto ya existe en la base de datos (por código o referencia)
                    String productId = null;
                    selectProductStmt.setString(1, barcode);
                    selectProductStmt.setString(2, reference);
                    java.sql.ResultSet rsProd = selectProductStmt.executeQuery();
                    if (rsProd.next()) {
                        productId = rsProd.getString("ID");
                    }
                    rsProd.close();

                    if (productId != null) {
                        // PRODUCTO EXISTENTE: Actualizar
                        updateProductStmt.setString(1, reference);
                        updateProductStmt.setString(2, barcode);
                        updateProductStmt.setString(3, name);
                        updateProductStmt.setDouble(4, priceBuy);
                        updateProductStmt.setDouble(5, priceSell);
                        updateProductStmt.setString(6, categoryId);
                        updateProductStmt.setString(7, taxId);
                        updateProductStmt.setString(8, uomId);
                        updateProductStmt.setString(9, supplierId);
                        updateProductStmt.setBoolean(10, isCom);
                        updateProductStmt.setBoolean(11, isScale);
                        updateProductStmt.setBoolean(12, isConstant);
                        updateProductStmt.setBoolean(13, printKb);
                        updateProductStmt.setBoolean(14, accumulatePoints);
                        updateProductStmt.setString(15, productId);
                        updateProductStmt.executeUpdate();
                        updatedCount++;
                    } else {
                        // PRODUCTO NUEVO: Crear e insertar
                        productId = java.util.UUID.randomUUID().toString();
                        insertProductStmt.setString(1, productId);
                        insertProductStmt.setString(2, reference);
                        insertProductStmt.setString(3, barcode);
                        insertProductStmt.setString(4, name);
                        insertProductStmt.setDouble(5, priceBuy);
                        insertProductStmt.setDouble(6, priceSell);
                        insertProductStmt.setString(7, categoryId);
                        insertProductStmt.setString(8, taxId);
                        insertProductStmt.setString(9, uomId);
                        insertProductStmt.setString(10, supplierId);
                        insertProductStmt.setBoolean(11, isCom);
                        insertProductStmt.setBoolean(12, isScale);
                        insertProductStmt.setBoolean(13, isConstant);
                        insertProductStmt.setBoolean(14, printKb);
                        insertProductStmt.setBoolean(15, accumulatePoints);
                        insertProductStmt.executeUpdate();
                        createdCount++;
                    }

                    // 6. Actualizar o insertar unidades en stockcurrent
                    updateStockStmt.setDouble(1, stockCurrent);
                    updateStockStmt.setString(2, defaultLocation);
                    updateStockStmt.setString(3, productId);
                    int stockRowsAffected = updateStockStmt.executeUpdate();
                    if (stockRowsAffected == 0) {
                        insertStockStmt.setString(1, defaultLocation);
                        insertStockStmt.setString(2, productId);
                        insertStockStmt.setDouble(3, stockCurrent);
                        insertStockStmt.executeUpdate();
                    }

                    // 7. Actualizar o insertar nivel mínimo de seguridad en stocklevel
                    updateStockLevelStmt.setDouble(1, stockMin);
                    updateStockLevelStmt.setString(2, defaultLocation);
                    updateStockLevelStmt.setString(3, productId);
                    int stockLevelRowsAffected = updateStockLevelStmt.executeUpdate();
                    if (stockLevelRowsAffected == 0) {
                        insertStockLevelStmt.setString(1, java.util.UUID.randomUUID().toString());
                        insertStockLevelStmt.setString(2, defaultLocation);
                        insertStockLevelStmt.setString(3, productId);
                        insertStockLevelStmt.setDouble(4, stockMin);
                        insertStockLevelStmt.executeUpdate();
                    }
                }

                // Confirmar transacción
                conn.commit();
                conn.setAutoCommit(originalAutoCommit);

                // Recargar la tabla/lista de productos del panel
                bd.actionLoad();

                JOptionPane.showMessageDialog(this,
                        "✅ Importación completada con éxito:\n\n" +
                        "• Productos creados nuevos: " + createdCount + "\n" +
                        "• Productos existentes actualizados: " + updatedCount + "\n" +
                        "• Ubicación de stock asignada: " + defaultLocation,
                        "Importación Exitosa",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al importar productos: " + e.getMessage(), e);
                JOptionPane.showMessageDialog(this,
                        "❌ Error de Importación:\n" + e.getMessage(),
                        "Error de Importación",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}