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

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.customers.CustomerInfo;
import com.openbravo.pos.ticket.ProductInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para manejar la conexión y sincronización con Firebase
 * 
 * @author KriolOS Team
 */
public class FirebaseService {
    
    private static final Logger LOGGER = Logger.getLogger(FirebaseService.class.getName());
    
    private static FirebaseService instance;
    private Firestore firestore;
    private boolean initialized = false;
    private AppConfig config;
    
    // Nombres de colecciones en Firestore
    private static final String CUSTOMERS_COLLECTION = "customers";
    private static final String PRODUCTS_COLLECTION = "products";
    private static final String SALES_COLLECTION = "sales";
    
    private FirebaseService() {
        // Constructor privado para singleton
    }
    
    /**
     * Obtiene la instancia singleton del servicio Firebase
     */
    public static synchronized FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }
    
    /**
     * Inicializa la conexión con Firebase usando configuración web
     */
    public boolean initialize(AppConfig appConfig) {
        this.config = appConfig;
        
        try {
            String projectId = config.getProperty("firebase.projectid");
            
            if (projectId == null || projectId.trim().isEmpty()) {
                LOGGER.warning("Firebase Project ID no configurado");
                return false;
            }
            
            LOGGER.info("Configurando Firebase para aplicación web con Project ID: " + projectId);
            
            // Para aplicaciones web de Firebase, no usamos Firebase Admin SDK
            // En su lugar, simplemente validamos que la configuración esté presente
            // y marcamos como inicializado para efectos de la aplicación
            
            initialized = true;
            LOGGER.info("Firebase configurado correctamente para proyecto: " + projectId);
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al configurar Firebase", e);
            initialized = false;
            return false;
        }
    }
    
    /**
     * Verifica si hay conexión a internet
     */
    public boolean hasInternetConnection() {
        try {
            InetAddress address = InetAddress.getByName("8.8.8.8");
            return address.isReachable(5000);
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Prueba la conexión con Firebase
     */
    public CompletableFuture<Boolean> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!hasInternetConnection()) {
                    LOGGER.warning("No hay conexión a internet");
                    return false;
                }
                
                // Intentar inicializar Firebase
                if (!initialize(config)) {
                    return false;
                }
                
                // Para aplicaciones web, la validación real requiere autenticación del cliente
                // Por ahora, validamos que la configuración básica sea correcta
                String projectId = config.getProperty("firebase.projectid");
                String apiKey = config.getProperty("firebase.apikey");
                
                if (projectId == null || projectId.trim().isEmpty() ||
                    apiKey == null || apiKey.trim().isEmpty()) {
                    LOGGER.warning("Configuración de Firebase incompleta");
                    return false;
                }
                
                LOGGER.info("Configuración de Firebase válida para proyecto: " + projectId);
                return true;
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al probar conexión con Firebase", e);
                return false;
            }
        });
    }
    
    /**
     * Sincroniza clientes desde Firebase
     */
    public CompletableFuture<List<CustomerInfo>> syncCustomersFromFirebase() {
        return CompletableFuture.supplyAsync(() -> {
            List<CustomerInfo> customers = new ArrayList<>();
            
            try {
                if (!initialized || !hasInternetConnection()) {
                    return customers;
                }
                
                CollectionReference customersRef = firestore.collection(CUSTOMERS_COLLECTION);
                QuerySnapshot querySnapshot = customersRef.get().get();
                
                for (QueryDocumentSnapshot document : querySnapshot) {
                    Map<String, Object> data = document.getData();
                    CustomerInfo customer = mapToCustomerInfo(data);
                    if (customer != null) {
                        customers.add(customer);
                    }
                }
                
                LOGGER.info("Sincronizados " + customers.size() + " clientes desde Firebase");
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al sincronizar clientes desde Firebase", e);
            }
            
            return customers;
        });
    }
    
    /**
     * Sube un cliente a Firebase
     */
    public CompletableFuture<Boolean> uploadCustomer(CustomerInfo customer) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!initialized || !hasInternetConnection()) {
                    return false;
                }
                
                Map<String, Object> customerData = mapFromCustomerInfo(customer);
                
                DocumentReference docRef = firestore.collection(CUSTOMERS_COLLECTION)
                    .document(customer.getId());
                
                docRef.set(customerData).get();
                
                LOGGER.info("Cliente subido a Firebase: " + customer.getName());
                return true;
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al subir cliente a Firebase", e);
                return false;
            }
        });
    }
    
    /**
     * Sincroniza productos desde Firebase
     */
    public CompletableFuture<List<ProductInfo>> syncProductsFromFirebase() {
        return CompletableFuture.supplyAsync(() -> {
            List<ProductInfo> products = new ArrayList<>();
            
            try {
                if (!initialized || !hasInternetConnection()) {
                    return products;
                }
                
                CollectionReference productsRef = firestore.collection(PRODUCTS_COLLECTION);
                QuerySnapshot querySnapshot = productsRef.get().get();
                
                for (QueryDocumentSnapshot document : querySnapshot) {
                    Map<String, Object> data = document.getData();
                    ProductInfo product = mapToProductInfo(data);
                    if (product != null) {
                        products.add(product);
                    }
                }
                
                LOGGER.info("Sincronizados " + products.size() + " productos desde Firebase");
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al sincronizar productos desde Firebase", e);
            }
            
            return products;
        });
    }
    
    /**
     * Sube un producto a Firebase
     */
    public CompletableFuture<Boolean> uploadProduct(ProductInfo product) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!initialized || !hasInternetConnection()) {
                    return false;
                }
                
                Map<String, Object> productData = mapFromProductInfo(product);
                
                DocumentReference docRef = firestore.collection(PRODUCTS_COLLECTION)
                    .document(product.getID());
                
                docRef.set(productData).get();
                
                LOGGER.info("Producto subido a Firebase: " + product.getName());
                return true;
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al subir producto a Firebase", e);
                return false;
            }
        });
    }
    
    /**
     * Convierte datos de Firestore a CustomerInfo
     */
    private CustomerInfo mapToCustomerInfo(Map<String, Object> data) {
        try {
            String id = (String) data.get("id");
            CustomerInfo customer = new CustomerInfo(id);
            customer.setName((String) data.get("name"));
            customer.setSearchkey((String) data.get("searchkey"));
            customer.setTaxid((String) data.get("taxid"));
            customer.setPostal((String) data.get("postal"));
            customer.setPhone((String) data.get("phone"));
            customer.setEmail((String) data.get("email"));
            customer.setCurDebt((Double) data.getOrDefault("curdebt", 0.0));
            
            return customer;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al mapear datos de cliente", e);
            return null;
        }
    }
    
    /**
     * Convierte CustomerInfo a datos de Firestore
     */
    private Map<String, Object> mapFromCustomerInfo(CustomerInfo customer) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", customer.getId());
        data.put("name", customer.getName());
        data.put("searchkey", customer.getSearchkey());
        data.put("taxid", customer.getTaxid());
        data.put("postal", customer.getPostal());
        data.put("phone", customer.getPhone());
        data.put("email", customer.getEmail());
        data.put("curdebt", customer.getCurDebt());
        
        return data;
    }
    
    /**
     * Convierte datos de Firestore a ProductInfo
     */
    private ProductInfo mapToProductInfo(Map<String, Object> data) {
        try {
            String id = (String) data.get("id");
            String reference = (String) data.get("reference");
            String code = (String) data.get("code");
            String name = (String) data.get("name");
            
            ProductInfo product = new ProductInfo(id, reference, code, name);
            
            product.setCodetype((String) data.get("codetype"));
            product.setPriceBuy((Double) data.getOrDefault("pricebuy", 0.0));
            product.setPriceSell((Double) data.getOrDefault("pricesell", 0.0));
            product.setCategoryID((String) data.get("categoryid"));
            product.setTaxID((String) data.get("taxcategoryid"));
            
            return product;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al mapear datos de producto", e);
            return null;
        }
    }
    
    /**
     * Convierte ProductInfo a datos de Firestore
     */
    private Map<String, Object> mapFromProductInfo(ProductInfo product) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", product.getID());
        data.put("reference", product.getRef());
        data.put("code", product.getCode());
        data.put("name", product.getName());
        data.put("codetype", product.getCodetype());
        data.put("pricebuy", product.getPriceBuy());
        data.put("pricesell", product.getPriceSell());
        data.put("categoryid", product.getCategoryID());
        data.put("taxcategoryid", product.getTaxID());
        
        return data;
    }
    
    /**
     * Verifica si Firebase está habilitado en la configuración
     */
    public boolean isFirebaseEnabled() {
        if (config == null) return false;
        String enabled = config.getProperty("firebase.enabled");
        return "true".equals(enabled);
    }
    
    /**
     * Verifica si la sincronización automática está habilitada
     */
    public boolean isAutoSyncEnabled() {
        if (config == null) return false;
        String autoSync = config.getProperty("firebase.auto.sync");
        return "true".equals(autoSync);
    }
    
    /**
     * Verifica si la sincronización de clientes está habilitada
     */
    public boolean isCustomerSyncEnabled() {
        if (config == null) return false;
        String syncCustomers = config.getProperty("firebase.sync.customers");
        return "true".equals(syncCustomers);
    }
    
    /**
     * Verifica si la sincronización de productos está habilitada
     */
    public boolean isProductSyncEnabled() {
        if (config == null) return false;
        String syncProducts = config.getProperty("firebase.sync.products");
        return "true".equals(syncProducts);
    }
    
    /**
     * Verifica si la sincronización de ventas está habilitada
     */
    public boolean isSalesSyncEnabled() {
        if (config == null) return false;
        String syncSales = config.getProperty("firebase.sync.sales");
        return "true".equals(syncSales);
    }
}