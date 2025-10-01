//Sebastian
//Servicio para gestión de clientes

package com.openbravo.pos.sebastian;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de clientes con persistencia en memoria
 * @author Sebastian
 */
public class CustomerService {
    
    private static CustomerService instance;
    private final Map<String, Customer> customers;
    
    private CustomerService() {
        this.customers = new ConcurrentHashMap<>();
        // Datos de ejemplo
        initializeExampleData();
    }
    
    public static CustomerService getInstance() {
        if (instance == null) {
            instance = new CustomerService();
        }
        return instance;
    }
    
    private void initializeExampleData() {
        // Cliente Sebastian con puntos altos
        Customer sebastian = new Customer("Sebastian", "Desarrollador", "sebastian@pos.com", "123-456-7890");
        sebastian.setAddress("Calle Principal 123");
        sebastian.addPoints(12500); // PLATINUM
        sebastian.setNotes("Creador del sistema Sebastian POS");
        customers.put(sebastian.getId(), sebastian);
        
        // Cliente ejemplo con puntos medios
        Customer maria = new Customer("María", "González", "maria@email.com", "987-654-3210");
        maria.setAddress("Avenida Central 456");
        maria.addPoints(3500); // SILVER
        maria.setNotes("Cliente frecuente");
        customers.put(maria.getId(), maria);
        
        // Cliente nuevo sin puntos
        Customer juan = new Customer("Juan", "Pérez", "juan@email.com", "555-123-4567");
        juan.setAddress("Plaza Mayor 789");
        juan.setNotes("Cliente nuevo");
        customers.put(juan.getId(), juan);
    }
    
    // CRUD Operations
    public String saveCustomer(Customer customer) {
        if (customer.getId() == null || customer.getId().isEmpty()) {
            customer.setId(UUID.randomUUID().toString());
        }
        customers.put(customer.getId(), customer);
        return customer.getId();
    }
    
    public Customer getCustomerById(String id) {
        return customers.get(id);
    }
    
    public List<Customer> getAllCustomers() {
        return new ArrayList<>(customers.values());
    }
    
    public List<Customer> getActiveCustomers() {
        return customers.values().stream()
                .filter(Customer::isActive)
                .collect(Collectors.toList());
    }
    
    public boolean deleteCustomer(String id) {
        return customers.remove(id) != null;
    }
    
    public void deactivateCustomer(String id) {
        Customer customer = customers.get(id);
        if (customer != null) {
            customer.setActive(false);
        }
    }
    
    // Búsquedas
    public List<Customer> searchCustomers(String query) {
        String lowerQuery = query.toLowerCase();
        return customers.values().stream()
                .filter(c -> c.getFullName().toLowerCase().contains(lowerQuery) ||
                           (c.getEmail() != null && c.getEmail().toLowerCase().contains(lowerQuery)) ||
                           (c.getPhone() != null && c.getPhone().contains(query)))
                .collect(Collectors.toList());
    }
    
    public List<Customer> getCustomersByMembershipLevel(String level) {
        return customers.values().stream()
                .filter(c -> level.equals(c.getMembershipLevel()))
                .collect(Collectors.toList());
    }
    
    // Estadísticas
    public Map<String, Integer> getMembershipStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("BRONZE", 0);
        stats.put("SILVER", 0);
        stats.put("GOLD", 0);
        stats.put("PLATINUM", 0);
        
        customers.values().forEach(c -> {
            String level = c.getMembershipLevel();
            stats.put(level, stats.get(level) + 1);
        });
        
        return stats;
    }
    
    public int getTotalCustomers() {
        return customers.size();
    }
    
    public int getActiveCustomersCount() {
        return (int) customers.values().stream().filter(Customer::isActive).count();
    }
    
    public int getTotalPoints() {
        return customers.values().stream().mapToInt(Customer::getPoints).sum();
    }
    
    // Gestión de puntos
    public void addPointsToCustomer(String customerId, int points, String reason) {
        Customer customer = customers.get(customerId);
        if (customer != null) {
            customer.addPoints(points);
            if (customer.getNotes() == null) {
                customer.setNotes("");
            }
            customer.setNotes(customer.getNotes() + "\n+ " + points + " pts: " + reason);
        }
    }
    
    public boolean updateCustomerLastPurchase(String customerId) {
        Customer customer = customers.get(customerId);
        if (customer != null) {
            customer.setLastPurchase(new Date());
            return true;
        }
        return false;
    }
    
    // Validaciones
    public boolean isEmailUnique(String email, String excludeId) {
        if (email == null || email.trim().isEmpty()) {
            return true; // Email vacío es válido
        }
        
        return customers.values().stream()
                .filter(c -> !c.getId().equals(excludeId))
                .noneMatch(c -> email.equalsIgnoreCase(c.getEmail()));
    }
    
    public boolean isPhoneUnique(String phone, String excludeId) {
        if (phone == null || phone.trim().isEmpty()) {
            return true; // Teléfono vacío es válido
        }
        
        return customers.values().stream()
                .filter(c -> !c.getId().equals(excludeId))
                .noneMatch(c -> phone.equals(c.getPhone()));
    }
    
    // Utility methods
    public void clearAllCustomers() {
        customers.clear();
    }
    
    public Customer[] getCustomersArray() {
        return customers.values().toArray(new Customer[0]);
    }
    
    // Métodos adicionales requeridos por JCustomerPanel
    public String addCustomer(Customer customer) {
        return saveCustomer(customer);
    }
    
    public void updateCustomer(Customer customer) {
        if (customer.getId() != null && customers.containsKey(customer.getId())) {
            customers.put(customer.getId(), customer);
        }
    }
    
    public Customer getCustomer(String id) {
        return getCustomerById(id);
    }
}