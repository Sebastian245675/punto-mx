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

package com.openbravo.pos.admin;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Session;
import com.openbravo.data.loader.PreparedSentence;
import com.openbravo.data.loader.StaticSentence;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.data.loader.SerializerReadBasic;
import com.openbravo.data.loader.Datas;
import com.openbravo.format.Formats;
import java.util.*;

/**
 * Inicializa los roles predeterminados con sus permisos específicos
 * 
 * @author Sebastian
 */
public class DefaultRolesInitializer {
    
    /**
     * Inicializa los tres roles predeterminados en la base de datos
     */
    public static void initializeDefaultRoles(Session session) {
        try {
            // ADMIN - Acceso total (FIJO, NO EDITABLE)
            initializeRole(session, "0", "ADMIN", getAdminPermissions());
            
            // MANAGER - Acceso parcial (EDITABLE - solo se crea si no existe)
            initializeRoleIfNotExists(session, "1", "MANAGER", getManagerPermissions());
            
            // Employee - Acceso controlado (EDITABLE - solo se crea si no existe)
            initializeRoleIfNotExists(session, "2", "Employee", getEmployeePermissions());
            
            System.out.println("Roles predeterminados inicializados correctamente");
            
        } catch (BasicException ex) {
            System.err.println("Error al inicializar roles predeterminados: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Inicializa un rol específico en la base de datos (siempre actualiza)
     */
    private static void initializeRole(Session session, String id, String name, Set<String> permissions) throws BasicException {
        // Verificar si el rol ya existe
        StaticSentence checkRole = new StaticSentence(session,
            "SELECT id FROM roles WHERE name = ?",
            new SerializerWriteBasic(new Datas[]{Datas.STRING}),
            new SerializerReadBasic(new Datas[]{Datas.STRING})
        );
        
        Object resultRow = checkRole.find(name);
        Object existingId = null;
        if (resultRow != null && resultRow instanceof Object[]) {
            Object[] row = (Object[]) resultRow;
            if (row.length > 0) {
                existingId = row[0];
            }
        }
        
        String permissionsXML = generatePermissionsXML(permissions);
        byte[] permissionsBytes = Formats.BYTEA.parseValue(permissionsXML);
        
        if (existingId == null) {
            // Insertar nuevo rol
            StaticSentence insertRole = new StaticSentence(session,
                "INSERT INTO roles (id, name, permissions) VALUES (?, ?, ?)",
                new SerializerWriteBasic(new Datas[]{Datas.STRING, Datas.STRING, Datas.BYTES})
            );
            insertRole.exec(new Object[]{id, name, permissionsBytes});
            System.out.println("Rol creado: " + name);
        } else {
            // Actualizar permisos del rol existente
            StaticSentence updateRole = new StaticSentence(session,
                "UPDATE roles SET permissions = ? WHERE name = ?",
                new SerializerWriteBasic(new Datas[]{Datas.BYTES, Datas.STRING})
            );
            updateRole.exec(new Object[]{permissionsBytes, name});
            System.out.println("Rol actualizado: " + name);
        }
    }
    
    /**
     * Inicializa un rol solo si NO existe (para roles editables)
     */
    private static void initializeRoleIfNotExists(Session session, String id, String name, Set<String> permissions) throws BasicException {
        // Verificar si el rol ya existe
        StaticSentence checkRole = new StaticSentence(session,
            "SELECT id FROM roles WHERE name = ?",
            new SerializerWriteBasic(new Datas[]{Datas.STRING}),
            new SerializerReadBasic(new Datas[]{Datas.STRING})
        );
        
        Object resultRow = checkRole.find(name);
        
        if (resultRow == null) {
            // Solo insertar si NO existe
            String permissionsXML = generatePermissionsXML(permissions);
            byte[] permissionsBytes = Formats.BYTEA.parseValue(permissionsXML);
            
            StaticSentence insertRole = new StaticSentence(session,
                "INSERT INTO roles (id, name, permissions) VALUES (?, ?, ?)",
                new SerializerWriteBasic(new Datas[]{Datas.STRING, Datas.STRING, Datas.BYTES})
            );
            insertRole.exec(new Object[]{id, name, permissionsBytes});
            System.out.println("Rol creado: " + name);
        } else {
            System.out.println("Rol ya existe (editable): " + name + " - No se modifica");
        }
    }
    
    /**
     * Genera el XML de permisos
     */
    private static String generatePermissionsXML(Set<String> permissions) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<permissions>\n");
        
        for (String permission : permissions) {
            xml.append("    <class name=\"").append(permission).append("\"/>\n");
        }
        
        xml.append("</permissions>\n");
        return xml.toString();
    }
    
    /**
     * Permisos para ADMIN - Acceso total
     */
    private static Set<String> getAdminPermissions() {
        Set<String> permissions = new LinkedHashSet<>();
        
        System.out.println("=== GENERANDO PERMISOS PARA ADMIN ===");
        
        // Todos los permisos de todas las categorías
        Map<String, List<PermissionInfo>> allPermissions = PermissionsCatalog.getAllPermissions();
        System.out.println("Categorías encontradas: " + allPermissions.size());
        
        for (Map.Entry<String, List<PermissionInfo>> entry : allPermissions.entrySet()) {
            String category = entry.getKey();
            List<PermissionInfo> categoryPermissions = entry.getValue();
            System.out.println("Categoría: " + category + " -> " + categoryPermissions.size() + " permisos");
            
            for (PermissionInfo permission : categoryPermissions) {
                permissions.add(permission.getClassName());
                System.out.println("  - " + permission.getDisplayName() + " (" + permission.getClassName() + ")");
            }
        }
        
        System.out.println("Total de permisos para ADMIN: " + permissions.size());
        
        return permissions;
    }
    
    /**
     * Permisos para MANAGER - Acceso parcial
     * Puede hacer todo excepto: modificar configuración crítica, cambiar precios base, eliminar registros históricos
     */
    private static Set<String> getManagerPermissions() {
        Set<String> permissions = new LinkedHashSet<>();
        
        // Ventas completas
        permissions.add("com.openbravo.pos.sales.JPanelTicketSales");
        permissions.add("sales.Total");
        permissions.add("sales.EditLines");
        permissions.add("sales.RemoveLines");
        permissions.add("Menu.Ticket");
        
        // Métodos de pago
        permissions.add("payment.cash");
        permissions.add("payment.cheque");
        permissions.add("payment.paper");
        permissions.add("payment.magcard");
        permissions.add("payment.free");
        
        // Reembolsos
        permissions.add("refund.cash");
        permissions.add("refund.cheque");
        permissions.add("refund.magcard");
        permissions.add("refund.paper");
        
        // Caja
        permissions.add("com.openbravo.pos.panels.JPanelCloseMoney");
        permissions.add("Menu.CloseTPV");
        
        // Clientes
        permissions.add("com.openbravo.pos.customers.CustomersPanel");
        permissions.add("Menu.Customers");
        
        // Inventario (sin modificar precios base)
        permissions.add("com.openbravo.pos.inventory.ProductsPanel");
        permissions.add("Menu.Products");
        permissions.add("com.openbravo.pos.inventory.CategoriesPanel");
        
        // Reportes básicos
        permissions.add("com.openbravo.reports.JReportClosedPos");
        permissions.add("com.openbravo.reports.JReportClosedProducts");
        permissions.add("Menu.Reports");
        
        return permissions;
    }
    
    /**
     * Permisos para Employee - Acceso controlado
     * Solo puede: hacer ventas, cobrar, ver productos
     */
    private static Set<String> getEmployeePermissions() {
        Set<String> permissions = new LinkedHashSet<>();
        
        // Ventas básicas (sin editar ni eliminar líneas)
        permissions.add("com.openbravo.pos.sales.JPanelTicketSales");
        permissions.add("sales.Total");
        permissions.add("Menu.Ticket");
        
        // Solo métodos de pago básicos
        permissions.add("payment.cash");
        permissions.add("payment.magcard");
        
        // Ver productos (sin modificar)
        permissions.add("Menu.Products");
        
        return permissions;
    }
}
