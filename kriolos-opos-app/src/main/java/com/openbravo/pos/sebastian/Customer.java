//Sebastian
//Sistema de gestion de clientes moderno

package com.openbravo.pos.sebastian;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Entidad Customer con ID único y sistema de puntajes
 * @author Sebastian
 */
public class Customer implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Campos principales
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    
    // Sistema de puntajes avanzado
    private int points;
    private int totalPointsEarned; // Total de puntos ganados históricamente
    private int totalPointsRedeemed; // Total de puntos canjeados
    private String membershipLevel; // BRONZE, SILVER, GOLD, PLATINUM
    
    // Estadísticas de compras
    private double totalSpent; // Total gastado históricamente
    private int totalPurchases; // Número total de compras
    private double averagePurchase; // Promedio de compra
    private double lastPurchaseAmount; // Monto de la última compra
    
    // Fechas importantes
    private Date dateCreated;
    private Date lastPurchase;
    private Date dateOfBirth;
    
    // Estado
    private boolean active;
    private String notes;
    
    // Constructores
    public Customer() {
        this.id = UUID.randomUUID().toString();
        this.dateCreated = new Date();
        this.points = 0;
        this.membershipLevel = "BRONZE";
        this.active = true;
    }
    
    public Customer(String firstName, String lastName, String email, String phone) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }
    
    // Métodos de negocio avanzados
    public void addPoints(int newPoints) {
        this.points += newPoints;
        this.totalPointsEarned += newPoints;
        updateMembershipLevel();
    }
    
    public boolean redeemPoints(int pointsToRedeem) {
        if (this.points >= pointsToRedeem) {
            this.points -= pointsToRedeem;
            this.totalPointsRedeemed += pointsToRedeem;
            updateMembershipLevel();
            return true;
        }
        return false;
    }
    
    public void addPurchase(double amount) {
        this.totalSpent += amount;
        this.totalPurchases++;
        this.lastPurchaseAmount = amount;
        this.lastPurchase = new Date();
        
        // Calcular puntos ganados (1 punto por cada $1 gastado)
        int pointsEarned = (int) amount;
        addPoints(pointsEarned);
        
        calculateAveragePurchase();
    }
    
    private void calculateAveragePurchase() {
        if (totalPurchases > 0) {
            this.averagePurchase = totalSpent / totalPurchases;
        } else {
            this.averagePurchase = 0.0;
        }
    }
    
    private void updateMembershipLevel() {
        if (points >= 10000) {
            membershipLevel = "PLATINUM";
        } else if (points >= 5000) {
            membershipLevel = "GOLD";
        } else if (points >= 1000) {
            membershipLevel = "SILVER";
        } else {
            membershipLevel = "BRONZE";
        }
    }
    
    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
    
    public String getDisplayInfo() {
        return getFullName() + " - " + membershipLevel + " (" + points + " pts) - $" + 
               String.format("%.2f", totalSpent) + " gastados";
    }
    
    public String getPointsInfo() {
        return "Puntos Actuales: " + points + 
               " | Total Ganados: " + totalPointsEarned + 
               " | Total Canjeados: " + totalPointsRedeemed;
    }
    
    public String getPurchaseInfo() {
        return "Compras: " + totalPurchases + 
               " | Total Gastado: $" + String.format("%.2f", totalSpent) + 
               " | Promedio: $" + String.format("%.2f", averagePurchase);
    }
    
    // Getters y Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public int getPoints() {
        return points;
    }
    
    public void setPoints(int points) {
        this.points = points;
        updateMembershipLevel();
    }
    
    public int getTotalPointsEarned() {
        return totalPointsEarned;
    }
    
    public void setTotalPointsEarned(int totalPointsEarned) {
        this.totalPointsEarned = totalPointsEarned;
    }
    
    public int getTotalPointsRedeemed() {
        return totalPointsRedeemed;
    }
    
    public void setTotalPointsRedeemed(int totalPointsRedeemed) {
        this.totalPointsRedeemed = totalPointsRedeemed;
    }
    
    public double getTotalSpent() {
        return totalSpent;
    }
    
    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
        calculateAveragePurchase();
    }
    
    public int getTotalPurchases() {
        return totalPurchases;
    }
    
    public void setTotalPurchases(int totalPurchases) {
        this.totalPurchases = totalPurchases;
        calculateAveragePurchase();
    }
    
    public double getAveragePurchase() {
        return averagePurchase;
    }
    
    public double getLastPurchaseAmount() {
        return lastPurchaseAmount;
    }
    
    public void setLastPurchaseAmount(double lastPurchaseAmount) {
        this.lastPurchaseAmount = lastPurchaseAmount;
    }
    
    public String getMembershipLevel() {
        return membershipLevel;
    }
    
    public void setMembershipLevel(String membershipLevel) {
        this.membershipLevel = membershipLevel;
    }
    
    public Date getDateCreated() {
        return dateCreated;
    }
    
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }
    
    public Date getLastPurchase() {
        return lastPurchase;
    }
    
    public void setLastPurchase(Date lastPurchase) {
        this.lastPurchase = lastPurchase;
    }
    
    public Date getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    @Override
    public String toString() {
        return getDisplayInfo();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Customer customer = (Customer) obj;
        return id != null ? id.equals(customer.id) : customer.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}