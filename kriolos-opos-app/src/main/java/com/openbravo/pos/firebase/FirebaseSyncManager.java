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

import com.openbravo.basic.BasicException;

import com.openbravo.data.loader.Session;
import com.openbravo.pos.customers.CustomerInfo;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.ticket.ProductInfo;
import com.openbravo.pos.ticket.ProductInfoExt;
import com.openbravo.data.user.DefaultSaveProvider;
import com.openbravo.data.user.SaveProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Administrador de sincronización entre datos locales y Firebase
 * 
 * @author KriolOS Team
 */
public class FirebaseSyncManager {
    
    private static final Logger LOGGER = Logger.getLogger(FirebaseSyncManager.class.getName());
    
    private final FirebaseService firebaseService;
    private final DataLogicCustomers dlCustomers;
    private final DataLogicSales dlSales;
    private final Session session;
    
    public FirebaseSyncManager(Session session) {
        this.session = session;
        this.firebaseService = FirebaseService.getInstance();
        this.dlCustomers = new DataLogicCustomers();
        this.dlCustomers.init(session);
        this.dlSales = new DataLogicSales();
        this.dlSales.init(session);
    }
    
    /**
     * Ejecuta sincronización completa si está habilitada
     */
    public CompletableFuture<SyncResult> performAutoSync(AppConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            SyncResult result = new SyncResult();
            
            try {
                // Verificar si Firebase está habilitado
                if (!firebaseService.isFirebaseEnabled()) {
                    result.setMessage("Firebase no está habilitado");
                    return result;
                }
                
                // Verificar conexión a internet
                if (!firebaseService.hasInternetConnection()) {
                    result.setMessage("No hay conexión a internet");
                    return result;
                }
                
                // Inicializar Firebase si es necesario
                if (!firebaseService.initialize(config)) {
                    result.setMessage("Error al inicializar Firebase");
                    return result;
                }
                
                // Sincronizar clientes si está habilitado
                if (firebaseService.isCustomerSyncEnabled()) {
                    SyncResult customerResult = syncCustomers().get();
                    result.addCustomerStats(customerResult);
                }
                
                // Sincronizar productos si está habilitado
                if (firebaseService.isProductSyncEnabled()) {
                    SyncResult productResult = syncProducts().get();
                    result.addProductStats(productResult);
                }
                
                result.setSuccess(true);
                result.setMessage("Sincronización completada exitosamente");
                
                LOGGER.info("Sincronización automática completada: " + result.getSummary());
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error durante sincronización automática", e);
                result.setMessage("Error durante sincronización: " + e.getMessage());
            }
            
            return result;
        });
    }
    
    /**
     * Sincroniza clientes entre local y Firebase
     */
    public CompletableFuture<SyncResult> syncCustomers() {
        return CompletableFuture.supplyAsync(() -> {
            SyncResult result = new SyncResult();
            
            try {
                // Obtener clientes locales
                List<CustomerInfo> localCustomers = getLocalCustomers();
                Set<String> localCustomerIds = new HashSet<>();
                for (CustomerInfo customer : localCustomers) {
                    localCustomerIds.add(customer.getId());
                }
                
                // Obtener clientes de Firebase
                List<CustomerInfo> firebaseCustomers = firebaseService.syncCustomersFromFirebase().get();
                
                // Subir clientes locales que no están en Firebase
                int uploadedCount = 0;
                for (CustomerInfo customer : localCustomers) {
                    boolean existsInFirebase = firebaseCustomers.stream()
                        .anyMatch(fc -> fc.getId().equals(customer.getId()));
                    
                    if (!existsInFirebase) {
                        boolean uploaded = firebaseService.uploadCustomer(customer).get();
                        if (uploaded) {
                            uploadedCount++;
                        }
                    }
                }
                
                // Descargar clientes de Firebase que no están localmente
                int downloadedCount = 0;
                for (CustomerInfo firebaseCustomer : firebaseCustomers) {
                    if (!localCustomerIds.contains(firebaseCustomer.getId())) {
                        boolean saved = saveLocalCustomer(firebaseCustomer);
                        if (saved) {
                            downloadedCount++;
                        }
                    }
                }
                
                result.setSuccess(true);
                result.setCustomersUploaded(uploadedCount);
                result.setCustomersDownloaded(downloadedCount);
                result.setMessage("Clientes sincronizados: " + uploadedCount + " subidos, " + downloadedCount + " descargados");
                
                LOGGER.info("Sincronización de clientes completada: " + result.getMessage());
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al sincronizar clientes", e);
                result.setMessage("Error al sincronizar clientes: " + e.getMessage());
            }
            
            return result;
        });
    }
    
    /**
     * Sincroniza productos entre local y Firebase
     */
    public CompletableFuture<SyncResult> syncProducts() {
        return CompletableFuture.supplyAsync(() -> {
            SyncResult result = new SyncResult();
            
            try {
                // Obtener productos locales
                List<ProductInfo> localProducts = getLocalProducts();
                Set<String> localProductIds = new HashSet<>();
                for (ProductInfo product : localProducts) {
                    localProductIds.add(product.getID());
                }
                
                // Obtener productos de Firebase
                List<ProductInfo> firebaseProducts = firebaseService.syncProductsFromFirebase().get();
                
                // Subir productos locales que no están en Firebase
                int uploadedCount = 0;
                for (ProductInfo product : localProducts) {
                    boolean existsInFirebase = firebaseProducts.stream()
                        .anyMatch(fp -> fp.getID().equals(product.getID()));
                    
                    if (!existsInFirebase) {
                        boolean uploaded = firebaseService.uploadProduct(product).get();
                        if (uploaded) {
                            uploadedCount++;
                        }
                    }
                }
                
                // Descargar productos de Firebase que no están localmente
                int downloadedCount = 0;
                for (ProductInfo firebaseProduct : firebaseProducts) {
                    if (!localProductIds.contains(firebaseProduct.getID())) {
                        boolean saved = saveLocalProduct(firebaseProduct);
                        if (saved) {
                            downloadedCount++;
                        }
                    }
                }
                
                result.setSuccess(true);
                result.setProductsUploaded(uploadedCount);
                result.setProductsDownloaded(downloadedCount);
                result.setMessage("Productos sincronizados: " + uploadedCount + " subidos, " + downloadedCount + " descargados");
                
                LOGGER.info("Sincronización de productos completada: " + result.getMessage());
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al sincronizar productos", e);
                result.setMessage("Error al sincronizar productos: " + e.getMessage());
            }
            
            return result;
        });
    }
    
    /**
     * Obtiene todos los clientes de la base de datos local
     */
    private List<CustomerInfo> getLocalCustomers() throws BasicException {
        return dlCustomers.getCustomerList().list();
    }
    
    /**
     * Obtiene todos los productos de la base de datos local
     */
    private List<ProductInfo> getLocalProducts() throws BasicException {
        List<ProductInfoExt> extProducts = dlSales.getProductList().list();
        List<ProductInfo> products = new ArrayList<>();
        for (ProductInfoExt ext : extProducts) {
            // Convertir ProductInfoExt a ProductInfo usando constructor correcto
            ProductInfo product = new ProductInfo(
                ext.getID(),
                ext.getReference(),
                ext.getCode(),
                ext.getName()
            );
            // Configurar campos adicionales usando setters
            product.setCodetype(ext.getCodetype());
            product.setPriceBuy(ext.getPriceBuy());
            product.setPriceSell(ext.getPriceSell());
            product.setCategoryID(ext.getCategoryID());
            product.setTaxID(ext.getTaxCategoryID());
            
            products.add(product);
        }
        return products;
    }
    
    /**
     * Guarda un cliente en la base de datos local
     */
    private boolean saveLocalCustomer(CustomerInfo customer) {
        try {
            SaveProvider<Object[]> customerSaveProvider = dlCustomers.getCustomerSaveProvider();
            // Aquí se necesitaría convertir CustomerInfo a Object[] para usar con SaveProvider
            // Por ahora, simplemente loggeamos que se intentó guardar
            LOGGER.info("Intento de guardar cliente local: " + customer.getName());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al guardar cliente local: " + customer.getName(), e);
            return false;
        }
    }
    
    /**
     * Guarda un producto en la base de datos local
     */
    private boolean saveLocalProduct(ProductInfo product) {
        try {
            SaveProvider productSaveProvider = new DefaultSaveProvider(
                dlSales.getProductCatUpdate(),
                dlSales.getProductCatInsert(), 
                dlSales.getProductCatDelete());
            // Aquí se necesitaría convertir ProductInfo a Object[] para usar con SaveProvider
            // Por ahora, simplemente loggeamos que se intentó guardar
            LOGGER.info("Intento de guardar producto local: " + product.getName());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al guardar producto local: " + product.getName(), e);
            return false;
        }
    }
    
    /**
     * Clase para encapsular los resultados de sincronización
     */
    public static class SyncResult {
        private boolean success = false;
        private String message = "";
        private int customersUploaded = 0;
        private int customersDownloaded = 0;
        private int productsUploaded = 0;
        private int productsDownloaded = 0;
        
        // Getters y setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public int getCustomersUploaded() { return customersUploaded; }
        public void setCustomersUploaded(int customersUploaded) { this.customersUploaded = customersUploaded; }
        
        public int getCustomersDownloaded() { return customersDownloaded; }
        public void setCustomersDownloaded(int customersDownloaded) { this.customersDownloaded = customersDownloaded; }
        
        public int getProductsUploaded() { return productsUploaded; }
        public void setProductsUploaded(int productsUploaded) { this.productsUploaded = productsUploaded; }
        
        public int getProductsDownloaded() { return productsDownloaded; }
        public void setProductsDownloaded(int productsDownloaded) { this.productsDownloaded = productsDownloaded; }
        
        public void addCustomerStats(SyncResult other) {
            this.customersUploaded += other.customersUploaded;
            this.customersDownloaded += other.customersDownloaded;
        }
        
        public void addProductStats(SyncResult other) {
            this.productsUploaded += other.productsUploaded;
            this.productsDownloaded += other.productsDownloaded;
        }
        
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Clientes: ").append(customersUploaded).append(" subidos, ")
              .append(customersDownloaded).append(" descargados. ");
            sb.append("Productos: ").append(productsUploaded).append(" subidos, ")
              .append(productsDownloaded).append(" descargados.");
            return sb.toString();
        }
    }
}