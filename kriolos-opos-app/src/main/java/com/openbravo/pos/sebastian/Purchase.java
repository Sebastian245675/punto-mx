//Sebastian
//Entidad Purchase para historial de compras

package com.openbravo.pos.sebastian;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * Entidad Purchase para representar compras individuales
 * @author Sebastian
 */
public class Purchase implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Campos principales
    private String id;
    private String customerId;
    private Date purchaseDate;
    private double totalAmount;
    private int pointsEarned;
    
    // Detalles de la compra
    private String paymentMethod; // EFECTIVO, TARJETA, TRANSFERENCIA
    private String status; // COMPLETADA, PENDIENTE, CANCELADA
    private String receiptNumber;
    private String notes;
    
    // Items de la compra
    private List<PurchaseItem> items;
    
    // Información del vendedor
    private String salesPerson;
    private String location;
    
    // Constructores
    public Purchase() {
        this.id = UUID.randomUUID().toString();
        this.purchaseDate = new Date();
        this.items = new ArrayList<>();
        this.status = "COMPLETADA";
        this.paymentMethod = "EFECTIVO";
    }
    
    public Purchase(String customerId, double totalAmount) {
        this();
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.pointsEarned = (int) totalAmount; // 1 punto por cada $1
    }
    
    // Métodos de negocio
    public void addItem(String productName, int quantity, double price) {
        PurchaseItem item = new PurchaseItem(productName, quantity, price);
        this.items.add(item);
        recalculateTotal();
    }
    
    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
            recalculateTotal();
        }
    }
    
    private void recalculateTotal() {
        this.totalAmount = items.stream()
                .mapToDouble(item -> item.getQuantity() * item.getPrice())
                .sum();
        this.pointsEarned = (int) this.totalAmount;
    }
    
    public String getFormattedDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(purchaseDate);
    }
    
    public String getFormattedAmount() {
        return String.format("$%.2f", totalAmount);
    }
    
    public String getDisplayInfo() {
        return getFormattedDate() + " - " + getFormattedAmount() + 
               " (" + pointsEarned + " pts) - " + paymentMethod;
    }
    
    // Getters y Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public Date getPurchaseDate() {
        return purchaseDate;
    }
    
    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    // Método alias para compatibilidad con JCustomerPanel
    public double getTotal() {
        return getTotalAmount();
    }
    
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
        this.pointsEarned = (int) totalAmount;
    }
    
    public int getPointsEarned() {
        return pointsEarned;
    }
    
    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getReceiptNumber() {
        return receiptNumber;
    }
    
    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public List<PurchaseItem> getItems() {
        return items;
    }
    
    public void setItems(List<PurchaseItem> items) {
        this.items = items;
        recalculateTotal();
    }
    
    public String getSalesPerson() {
        return salesPerson;
    }
    
    public void setSalesPerson(String salesPerson) {
        this.salesPerson = salesPerson;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    @Override
    public String toString() {
        return getDisplayInfo();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Purchase purchase = (Purchase) obj;
        return id != null ? id.equals(purchase.id) : purchase.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    /**
     * Clase interna para representar items individuales de la compra
     */
    public static class PurchaseItem implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String productName;
        private int quantity;
        private double price;
        private String category;
        
        public PurchaseItem() {}
        
        public PurchaseItem(String productName, int quantity, double price) {
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
        }
        
        public double getSubtotal() {
            return quantity * price;
        }
        
        public String getFormattedSubtotal() {
            return String.format("$%.2f", getSubtotal());
        }
        
        public String getDisplayInfo() {
            return productName + " x" + quantity + " @ $" + 
                   String.format("%.2f", price) + " = " + getFormattedSubtotal();
        }
        
        // Getters y Setters
        public String getProductName() {
            return productName;
        }
        
        public void setProductName(String productName) {
            this.productName = productName;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
        
        public double getPrice() {
            return price;
        }
        
        public void setPrice(double price) {
            this.price = price;
        }
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
        
        @Override
        public String toString() {
            return getDisplayInfo();
        }
    }
}