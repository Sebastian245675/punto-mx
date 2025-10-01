//Sebastian
//Servicio para gestionar historial de compras

package com.openbravo.pos.sebastian;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar el historial de compras de clientes
 * @author Sebastian
 */
public class PurchaseService {
    
    private static PurchaseService instance;
    private final Map<String, Purchase> purchases;
    
    private PurchaseService() {
        this.purchases = new ConcurrentHashMap<>();
        initializeExampleData();
    }
    
    public static PurchaseService getInstance() {
        if (instance == null) {
            synchronized (PurchaseService.class) {
                if (instance == null) {
                    instance = new PurchaseService();
                }
            }
        }
        return instance;
    }
    
    // Operaciones CRUD
    public void addPurchase(Purchase purchase) {
        if (purchase != null && purchase.getId() != null) {
            purchases.put(purchase.getId(), purchase);
            
            // Actualizar estadísticas del cliente
            updateCustomerStats(purchase);
        }
    }
    
    public Purchase getPurchase(String id) {
        return purchases.get(id);
    }
    
    public void updatePurchase(Purchase purchase) {
        if (purchase != null && purchase.getId() != null) {
            purchases.put(purchase.getId(), purchase);
        }
    }
    
    public void deletePurchase(String id) {
        purchases.remove(id);
    }
    
    public List<Purchase> getAllPurchases() {
        return new ArrayList<>(purchases.values());
    }
    
    // Búsquedas específicas
    public List<Purchase> getPurchasesByCustomer(String customerId) {
        return purchases.values().stream()
                .filter(purchase -> customerId.equals(purchase.getCustomerId()))
                .sorted((p1, p2) -> p2.getPurchaseDate().compareTo(p1.getPurchaseDate()))
                .collect(Collectors.toList());
    }
    
    public List<Purchase> getPurchasesByDateRange(Date startDate, Date endDate) {
        return purchases.values().stream()
                .filter(purchase -> {
                    Date purchaseDate = purchase.getPurchaseDate();
                    return purchaseDate.compareTo(startDate) >= 0 && 
                           purchaseDate.compareTo(endDate) <= 0;
                })
                .sorted((p1, p2) -> p2.getPurchaseDate().compareTo(p1.getPurchaseDate()))
                .collect(Collectors.toList());
    }
    
    public List<Purchase> getPurchasesByAmount(double minAmount, double maxAmount) {
        return purchases.values().stream()
                .filter(purchase -> purchase.getTotalAmount() >= minAmount && 
                                   purchase.getTotalAmount() <= maxAmount)
                .sorted((p1, p2) -> Double.compare(p2.getTotalAmount(), p1.getTotalAmount()))
                .collect(Collectors.toList());
    }
    
    public List<Purchase> getPurchasesByPaymentMethod(String paymentMethod) {
        return purchases.values().stream()
                .filter(purchase -> paymentMethod.equals(purchase.getPaymentMethod()))
                .sorted((p1, p2) -> p2.getPurchaseDate().compareTo(p1.getPurchaseDate()))
                .collect(Collectors.toList());
    }
    
    // Estadísticas de compras
    public double getTotalSalesByCustomer(String customerId) {
        return getPurchasesByCustomer(customerId).stream()
                .mapToDouble(Purchase::getTotalAmount)
                .sum();
    }
    
    public int getTotalPurchasesByCustomer(String customerId) {
        return getPurchasesByCustomer(customerId).size();
    }
    
    public double getAveragePurchaseByCustomer(String customerId) {
        List<Purchase> customerPurchases = getPurchasesByCustomer(customerId);
        if (customerPurchases.isEmpty()) return 0.0;
        
        double total = customerPurchases.stream()
                .mapToDouble(Purchase::getTotalAmount)
                .sum();
        return total / customerPurchases.size();
    }
    
    public Purchase getLastPurchaseByCustomer(String customerId) {
        return getPurchasesByCustomer(customerId).stream()
                .findFirst()
                .orElse(null);
    }
    
    public Map<String, Integer> getPurchaseStatsByPaymentMethod() {
        return purchases.values().stream()
                .collect(Collectors.groupingBy(
                    Purchase::getPaymentMethod,
                    Collectors.collectingAndThen(
                        Collectors.counting(),
                        Math::toIntExact
                    )
                ));
    }
    
    public Map<String, Double> getSalesStatsByPaymentMethod() {
        return purchases.values().stream()
                .collect(Collectors.groupingBy(
                    Purchase::getPaymentMethod,
                    Collectors.summingDouble(Purchase::getTotalAmount)
                ));
    }
    
    // Métodos de utilidad
    public Purchase createPurchase(String customerId, double totalAmount, 
                                 String paymentMethod, String receiptNumber) {
        Purchase purchase = new Purchase(customerId, totalAmount);
        purchase.setPaymentMethod(paymentMethod);
        purchase.setReceiptNumber(receiptNumber);
        
        addPurchase(purchase);
        return purchase;
    }
    
    public Purchase createPurchaseWithItems(String customerId, String paymentMethod, 
                                          String receiptNumber, List<Purchase.PurchaseItem> items) {
        Purchase purchase = new Purchase();
        purchase.setCustomerId(customerId);
        purchase.setPaymentMethod(paymentMethod);
        purchase.setReceiptNumber(receiptNumber);
        purchase.setItems(items);
        
        addPurchase(purchase);
        return purchase;
    }
    
    private void updateCustomerStats(Purchase purchase) {
        CustomerService customerService = CustomerService.getInstance();
        Customer customer = customerService.getCustomer(purchase.getCustomerId());
        
        if (customer != null) {
            customer.addPurchase(purchase.getTotalAmount());
            customerService.updateCustomer(customer);
        }
    }
    
    // Reportes
    public String getCustomerPurchaseReport(String customerId) {
        List<Purchase> purchases = getPurchasesByCustomer(customerId);
        if (purchases.isEmpty()) {
            return "No hay compras registradas para este cliente.";
        }
        
        double total = getTotalSalesByCustomer(customerId);
        double average = getAveragePurchaseByCustomer(customerId);
        Purchase lastPurchase = getLastPurchaseByCustomer(customerId);
        
        StringBuilder report = new StringBuilder();
        report.append("=== REPORTE DE COMPRAS ===\n");
        report.append("Total de compras: ").append(purchases.size()).append("\n");
        report.append("Monto total: $").append(String.format("%.2f", total)).append("\n");
        report.append("Promedio por compra: $").append(String.format("%.2f", average)).append("\n");
        report.append("Última compra: ").append(lastPurchase.getFormattedDate()).append("\n");
        report.append("Monto última compra: ").append(lastPurchase.getFormattedAmount()).append("\n");
        
        return report.toString();
    }
    
    public String getGeneralStatsReport() {
        int totalPurchases = purchases.size();
        double totalSales = purchases.values().stream()
                .mapToDouble(Purchase::getTotalAmount)
                .sum();
        double averageSale = totalPurchases > 0 ? totalSales / totalPurchases : 0.0;
        
        Map<String, Integer> paymentStats = getPurchaseStatsByPaymentMethod();
        
        StringBuilder report = new StringBuilder();
        report.append("=== ESTADÍSTICAS GENERALES ===\n");
        report.append("Total de compras: ").append(totalPurchases).append("\n");
        report.append("Ventas totales: $").append(String.format("%.2f", totalSales)).append("\n");
        report.append("Promedio por venta: $").append(String.format("%.2f", averageSale)).append("\n");
        report.append("\n--- Métodos de pago ---\n");
        paymentStats.forEach((method, count) -> 
            report.append(method).append(": ").append(count).append(" compras\n"));
        
        return report.toString();
    }
    
    // Datos de ejemplo
    private void initializeExampleData() {
        // Obtener algunos clientes de ejemplo
        CustomerService customerService = CustomerService.getInstance();
        List<Customer> customers = customerService.getAllCustomers();
        
        if (!customers.isEmpty()) {
            Random random = new Random();
            String[] paymentMethods = {"EFECTIVO", "TARJETA", "TRANSFERENCIA"};
            String[] products = {"Café Expreso", "Sándwich Club", "Ensalada César", 
                               "Jugo Natural", "Pastel de Chocolate", "Pizza Margarita"};
            
            // Crear 20 compras de ejemplo
            for (int i = 0; i < 20; i++) {
                Customer customer = customers.get(random.nextInt(customers.size()));
                Purchase purchase = new Purchase();
                purchase.setCustomerId(customer.getId());
                purchase.setPaymentMethod(paymentMethods[random.nextInt(paymentMethods.length)]);
                purchase.setReceiptNumber("R" + String.format("%06d", i + 1));
                purchase.setSalesPerson("Vendedor " + (random.nextInt(3) + 1));
                purchase.setLocation("Sucursal Centro");
                
                // Fecha aleatoria en los últimos 30 días
                Date baseDate = new Date();
                long randomTime = baseDate.getTime() - (long)(random.nextDouble() * 30 * 24 * 60 * 60 * 1000);
                purchase.setPurchaseDate(new Date(randomTime));
                
                // Agregar productos aleatorios
                int numItems = random.nextInt(3) + 1;
                for (int j = 0; j < numItems; j++) {
                    String product = products[random.nextInt(products.length)];
                    int quantity = random.nextInt(3) + 1;
                    double price = 5.0 + (random.nextDouble() * 20.0);
                    purchase.addItem(product, quantity, price);
                }
                
                addPurchase(purchase);
            }
        }
    }
    
    // Búsqueda avanzada
    public List<Purchase> searchPurchases(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllPurchases();
        }
        
        String term = searchTerm.toLowerCase().trim();
        return purchases.values().stream()
                .filter(purchase -> {
                    // Buscar en número de recibo
                    if (purchase.getReceiptNumber() != null && 
                        purchase.getReceiptNumber().toLowerCase().contains(term)) {
                        return true;
                    }
                    
                    // Buscar en método de pago
                    if (purchase.getPaymentMethod() != null && 
                        purchase.getPaymentMethod().toLowerCase().contains(term)) {
                        return true;
                    }
                    
                    // Buscar en vendedor
                    if (purchase.getSalesPerson() != null && 
                        purchase.getSalesPerson().toLowerCase().contains(term)) {
                        return true;
                    }
                    
                    // Buscar en productos
                    return purchase.getItems().stream()
                            .anyMatch(item -> item.getProductName() != null && 
                                     item.getProductName().toLowerCase().contains(term));
                })
                .sorted((p1, p2) -> p2.getPurchaseDate().compareTo(p1.getPurchaseDate()))
                .collect(Collectors.toList());
    }
}