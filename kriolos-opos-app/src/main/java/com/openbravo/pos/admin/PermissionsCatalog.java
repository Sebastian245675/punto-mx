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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo de todos los permisos disponibles en el sistema
 * 
 * @author Sebastian
 */
public class PermissionsCatalog {
    
    private static final Map<String, List<PermissionInfo>> PERMISSIONS = new LinkedHashMap<>();
    
    static {
        // VENTAS
        List<PermissionInfo> sales = new ArrayList<>();
        sales.add(new PermissionInfo("com.openbravo.pos.sales.JPanelTicketSales", "Pantalla de Ventas", "Ventas"));
        sales.add(new PermissionInfo("com.openbravo.pos.sales.JPanelTicketEdits", "Editar Tickets", "Ventas"));
        sales.add(new PermissionInfo("sales.Override", "Anular Operaciones", "Ventas"));
        sales.add(new PermissionInfo("sales.ViewSharedTicket", "Ver Tickets Compartidos", "Ventas"));
        sales.add(new PermissionInfo("sales.DeleteLines", "Eliminar Líneas", "Ventas"));
        sales.add(new PermissionInfo("sales.EditLines", "Editar Líneas", "Ventas"));
        sales.add(new PermissionInfo("sales.EditTicket", "Editar Ticket", "Ventas"));
        sales.add(new PermissionInfo("sales.RefundTicket", "Reembolsos", "Ventas"));
        sales.add(new PermissionInfo("sales.PrintTicket", "Imprimir Ticket", "Ventas"));
        sales.add(new PermissionInfo("sales.Total", "Cerrar Venta", "Ventas"));
        sales.add(new PermissionInfo("sales.ChangeTaxOptions", "Cambiar Opciones de Impuestos", "Ventas"));
        sales.add(new PermissionInfo("sales.PrintRemote", "Impresión Remota", "Ventas"));
        sales.add(new PermissionInfo("sales.ShowList", "Mostrar Lista", "Ventas"));
        sales.add(new PermissionInfo("sales.DeleteTicket", "Eliminar Ticket", "Ventas"));
        sales.add(new PermissionInfo("sales.Layout", "Diseño de Pantalla", "Ventas"));
        PERMISSIONS.put("Ventas", sales);
        
        // PAGOS
        List<PermissionInfo> payments = new ArrayList<>();
        payments.add(new PermissionInfo("payment.cash", "Efectivo", "Métodos de Pago"));
        payments.add(new PermissionInfo("payment.cheque", "Cheque", "Métodos de Pago"));
        payments.add(new PermissionInfo("payment.voucher", "Vale/Cupón", "Métodos de Pago"));
        payments.add(new PermissionInfo("payment.magcard", "Tarjeta", "Métodos de Pago"));
        payments.add(new PermissionInfo("payment.slip", "Comprobante", "Métodos de Pago"));
        payments.add(new PermissionInfo("payment.free", "Gratis", "Métodos de Pago"));
        payments.add(new PermissionInfo("payment.debt", "Crédito/Deuda", "Métodos de Pago"));
        payments.add(new PermissionInfo("payment.bank", "Transferencia Bancaria", "Métodos de Pago"));
        PERMISSIONS.put("Métodos de Pago", payments);
        
        // REEMBOLSOS
        List<PermissionInfo> refunds = new ArrayList<>();
        refunds.add(new PermissionInfo("refund.cash", "Reembolso en Efectivo", "Reembolsos"));
        refunds.add(new PermissionInfo("refund.cheque", "Reembolso con Cheque", "Reembolsos"));
        refunds.add(new PermissionInfo("refund.voucher", "Reembolso con Vale", "Reembolsos"));
        refunds.add(new PermissionInfo("refund.magcard", "Reembolso a Tarjeta", "Reembolsos"));
        PERMISSIONS.put("Reembolsos", refunds);
        
        // BOTONES ESPECIALES
        List<PermissionInfo> buttons = new ArrayList<>();
        buttons.add(new PermissionInfo("button.totaldiscount", "Descuento Total", "Botones Especiales"));
        buttons.add(new PermissionInfo("button.linediscount", "Descuento por Línea", "Botones Especiales"));
        buttons.add(new PermissionInfo("button.print", "Imprimir", "Botones Especiales"));
        buttons.add(new PermissionInfo("button.opendrawer", "Abrir Cajón", "Botones Especiales"));
        buttons.add(new PermissionInfo("button.sendorder", "Enviar Orden", "Botones Especiales"));
        buttons.add(new PermissionInfo("button.refundit", "Reembolsar", "Botones Especiales"));
        buttons.add(new PermissionInfo("button.scharge", "Cargo por Servicio", "Botones Especiales"));
        buttons.add(new PermissionInfo("button.keyboard", "Teclado", "Botones Especiales"));
        buttons.add(new PermissionInfo("button.posapps", "Aplicaciones POS", "Botones Especiales"));
        PERMISSIONS.put("Botones Especiales", buttons);
        
        // CAJA
        List<PermissionInfo> cashManagement = new ArrayList<>();
        cashManagement.add(new PermissionInfo("com.openbravo.pos.panels.JPanelPayments", "Gestión de Pagos", "Caja"));
        cashManagement.add(new PermissionInfo("com.openbravo.pos.panels.JPanelCloseMoney", "Cierre de Caja", "Caja"));
        cashManagement.add(new PermissionInfo("com.openbravo.pos.panels.JPanelCloseMoneyReprint", "Reimprimir Cierre", "Caja"));
        PERMISSIONS.put("Caja", cashManagement);
        
        // CLIENTES
        List<PermissionInfo> customers = new ArrayList<>();
        customers.add(new PermissionInfo("com.openbravo.pos.forms.MenuCustomers", "Menú Clientes", "Clientes"));
        customers.add(new PermissionInfo("com.openbravo.pos.customers.CustomersPanel", "Gestión de Clientes", "Clientes"));
        customers.add(new PermissionInfo("com.openbravo.pos.customers.CustomersPayment", "Pagos de Clientes", "Clientes"));
        customers.add(new PermissionInfo("/com/openbravo/reports/customers.bs", "Reporte: Clientes", "Clientes"));
        customers.add(new PermissionInfo("/com/openbravo/reports/customers_sales.bs", "Reporte: Ventas por Cliente", "Clientes"));
        customers.add(new PermissionInfo("/com/openbravo/reports/customers_debtors.bs", "Reporte: Clientes Deudores", "Clientes"));
        customers.add(new PermissionInfo("/com/openbravo/reports/customers_diary.bs", "Reporte: Diario de Clientes", "Clientes"));
        customers.add(new PermissionInfo("/com/openbravo/reports/customers_cards.bs", "Reporte: Tarjetas de Clientes", "Clientes"));
        customers.add(new PermissionInfo("/com/openbravo/reports/customers_list.bs", "Reporte: Lista de Clientes", "Clientes"));
        customers.add(new PermissionInfo("/com/openbravo/reports/customers_export.bs", "Reporte: Exportar Clientes", "Clientes"));
        customers.add(new PermissionInfo("/com/openbravo/reports/customers_vouchers.bs", "Reporte: Vales de Clientes", "Clientes"));
        PERMISSIONS.put("Clientes", customers);
        
        // PROVEEDORES
        List<PermissionInfo> suppliers = new ArrayList<>();
        suppliers.add(new PermissionInfo("com.openbravo.pos.forms.MenuSuppliers", "Menú Proveedores", "Proveedores"));
        suppliers.add(new PermissionInfo("com.openbravo.pos.suppliers.SuppliersPanel", "Gestión de Proveedores", "Proveedores"));
        suppliers.add(new PermissionInfo("/com/openbravo/reports/suppliers.bs", "Reporte: Proveedores", "Proveedores"));
        suppliers.add(new PermissionInfo("/com/openbravo/reports/suppliers_b.bs", "Reporte: Proveedores B", "Proveedores"));
        suppliers.add(new PermissionInfo("/com/openbravo/reports/suppliers_creditors.bs", "Reporte: Proveedores Acreedores", "Proveedores"));
        suppliers.add(new PermissionInfo("/com/openbravo/reports/suppliers_diary.bs", "Reporte: Diario de Proveedores", "Proveedores"));
        suppliers.add(new PermissionInfo("/com/openbravo/reports/suppliers_list.bs", "Reporte: Lista de Proveedores", "Proveedores"));
        suppliers.add(new PermissionInfo("/com/openbravo/reports/suppliers_sales.bs", "Reporte: Compras a Proveedores", "Proveedores"));
        suppliers.add(new PermissionInfo("/com/openbravo/reports/suppliers_export.bs", "Reporte: Exportar Proveedores", "Proveedores"));
        suppliers.add(new PermissionInfo("/com/openbravo/reports/suppliers_products.bs", "Reporte: Productos por Proveedor", "Proveedores"));
        PERMISSIONS.put("Proveedores", suppliers);
        
        // INVENTARIO
        List<PermissionInfo> inventory = new ArrayList<>();
        inventory.add(new PermissionInfo("com.openbravo.pos.forms.MenuStockManagement", "Menú Inventario", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.ProductsPanel", "Gestión de Productos", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.CategoriesPanel", "Gestión de Categorías", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.AttributesPanel", "Atributos", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.AttributeValuesPanel", "Valores de Atributos", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.AttributeSetsPanel", "Conjuntos de Atributos", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.AttributeUsePanel", "Uso de Atributos", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.AuxiliarPanel", "Panel Auxiliar", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.BundlePanel", "Paquetes/Combos", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.ProductsWarehousePanel", "Productos por Almacén", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.StockDiaryPanel", "Diario de Stock", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.StockManagement", "Gestión de Stock", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.TaxCategoriesPanel", "Categorías de Impuestos", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.TaxCustCategoriesPanel", "Categorías de Impuestos por Cliente", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.TaxPanel", "Gestión de Impuestos", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.UomPanel", "Unidades de Medida", "Inventario"));
        inventory.add(new PermissionInfo("com.openbravo.pos.inventory.LocationsPanel", "Ubicaciones/Almacenes", "Inventario"));
        PERMISSIONS.put("Inventario", inventory);
        
        // REPORTES DE INVENTARIO
        List<PermissionInfo> inventoryReports = new ArrayList<>();
        inventoryReports.add(new PermissionInfo("/com/openbravo/reports/inventory.bs", "Reporte: Inventario", "Reportes de Inventario"));
        inventoryReports.add(new PermissionInfo("/com/openbravo/reports/inventoryb.bs", "Reporte: Inventario B", "Reportes de Inventario"));
        inventoryReports.add(new PermissionInfo("/com/openbravo/reports/inventory_diary.bs", "Reporte: Diario de Inventario", "Reportes de Inventario"));
        inventoryReports.add(new PermissionInfo("/com/openbravo/reports/inventorybroken.bs", "Reporte: Productos Dañados", "Reportes de Inventario"));
        inventoryReports.add(new PermissionInfo("/com/openbravo/reports/inventorydiff.bs", "Reporte: Diferencias de Inventario", "Reportes de Inventario"));
        inventoryReports.add(new PermissionInfo("/com/openbravo/reports/inventorydiffdetail.bs", "Reporte: Detalle de Diferencias", "Reportes de Inventario"));
        inventoryReports.add(new PermissionInfo("/com/openbravo/reports/inventorylistdetail.bs", "Reporte: Lista Detallada", "Reportes de Inventario"));
        inventoryReports.add(new PermissionInfo("/com/openbravo/reports/products.bs", "Reporte: Productos", "Reportes de Inventario"));
        inventoryReports.add(new PermissionInfo("/com/openbravo/reports/productscatalog.bs", "Reporte: Catálogo de Productos", "Reportes de Inventario"));
        inventoryReports.add(new PermissionInfo("/com/openbravo/reports/productlabels.bs", "Reporte: Etiquetas de Productos", "Reportes de Inventario"));
        inventoryReports.add(new PermissionInfo("/com/openbravo/reports/salecatalog.bs", "Reporte: Catálogo de Venta", "Reportes de Inventario"));
        inventoryReports.add(new PermissionInfo("/com/openbravo/reports/barcode_sheet.bs", "Reporte: Hoja de Códigos de Barras", "Reportes de Inventario"));
        inventoryReports.add(new PermissionInfo("/com/openbravo/reports/barcode_shelfedgelabels.bs", "Reporte: Etiquetas de Anaquel", "Reportes de Inventario"));
        PERMISSIONS.put("Reportes de Inventario", inventoryReports);
        
        // REPORTES DE VENTAS
        List<PermissionInfo> salesReports = new ArrayList<>();
        salesReports.add(new PermissionInfo("com.openbravo.pos.forms.MenuSalesManagement", "Menú Reportes de Ventas", "Reportes de Ventas"));
        salesReports.add(new PermissionInfo("/com/openbravo/reports/sales_cashflow.bs", "Reporte: Flujo de Efectivo", "Reportes de Ventas"));
        salesReports.add(new PermissionInfo("/com/openbravo/reports/sales_cashregisterlog.bs", "Reporte: Log de Caja", "Reportes de Ventas"));
        salesReports.add(new PermissionInfo("/com/openbravo/reports/sales_extendedcashregisterlog.bs", "Reporte: Log Extendido de Caja", "Reportes de Ventas"));
        salesReports.add(new PermissionInfo("/com/openbravo/reports/sales_categorysales.bs", "Reporte: Ventas por Categoría", "Reportes de Ventas"));
        salesReports.add(new PermissionInfo("/com/openbravo/reports/sales_categorysales_1.bs", "Reporte: Ventas por Categoría v2", "Reportes de Ventas"));
        salesReports.add(new PermissionInfo("/com/openbravo/reports/sales_closedpos.bs", "Reporte: POS Cerrados", "Reportes de Ventas"));
        salesReports.add(new PermissionInfo("/com/openbravo/reports/sales_closedpos_1.bs", "Reporte: POS Cerrados v2", "Reportes de Ventas"));
        salesReports.add(new PermissionInfo("/com/openbravo/reports/sales_closedproducts.bs", "Reporte: Productos Vendidos", "Reportes de Ventas"));
        salesReports.add(new PermissionInfo("/com/openbravo/reports/sales_closedproducts_1.bs", "Reporte: Productos Vendidos v2", "Reportes de Ventas"));
        salesReports.add(new PermissionInfo("/com/openbravo/reports/sales_extproducts.bs", "Reporte: Productos Extendido", "Reportes de Ventas"));
        salesReports.add(new PermissionInfo("/com/openbravo/reports/sales_paymentreport.bs", "Reporte: Pagos", "Reportes de Ventas"));
        salesReports.add(new PermissionInfo("/com/openbravo/reports/sales_productsalesprofit.bs", "Reporte: Ganancias por Producto", "Reportes de Ventas"));
        salesReports.add(new PermissionInfo("/com/openbravo/reports/sales_saletaxes.bs", "Reporte: Impuestos de Venta", "Reportes de Ventas"));
        salesReports.add(new PermissionInfo("/com/openbravo/reports/sales_taxcatsales.bs", "Reporte: Ventas por Categoría Fiscal", "Reportes de Ventas"));
        salesReports.add(new PermissionInfo("/com/openbravo/reports/sales_taxes.bs", "Reporte: Impuestos", "Reportes de Ventas"));
        PERMISSIONS.put("Reportes de Ventas", salesReports);
        
        // GRÁFICOS
        List<PermissionInfo> charts = new ArrayList<>();
        charts.add(new PermissionInfo("/com/openbravo/reports/sales_chart_chartsales.bs", "Gráfico: Ventas", "Gráficos"));
        charts.add(new PermissionInfo("/com/openbravo/reports/sales_chart_piesalescat.bs", "Gráfico: Pastel Ventas por Categoría", "Gráficos"));
        charts.add(new PermissionInfo("/com/openbravo/reports/sales_chart_productsales.bs", "Gráfico: Ventas de Productos", "Gráficos"));
        charts.add(new PermissionInfo("/com/openbravo/reports/sales_chart_timeseriesproduct.bs", "Gráfico: Series de Tiempo", "Gráficos"));
        charts.add(new PermissionInfo("/com/openbravo/reports/sales_chart_top10sales.bs", "Gráfico: Top 10 Ventas", "Gráficos"));
        PERMISSIONS.put("Gráficos", charts);
        
        // MANTENIMIENTO
        List<PermissionInfo> maintenance = new ArrayList<>();
        maintenance.add(new PermissionInfo("com.openbravo.pos.forms.MenuMaintenance", "Menú Mantenimiento", "Mantenimiento"));
        maintenance.add(new PermissionInfo("com.openbravo.pos.admin.PeoplePanel", "Gestión de Usuarios", "Mantenimiento"));
        maintenance.add(new PermissionInfo("com.openbravo.pos.admin.RolesPanel", "Gestión de Roles", "Mantenimiento"));
        maintenance.add(new PermissionInfo("com.openbravo.pos.admin.ResourcesPanel", "Gestión de Recursos", "Mantenimiento"));
        maintenance.add(new PermissionInfo("com.openbravo.pos.sales.restaurant.JPanelFloors", "Gestión de Pisos (Restaurant)", "Mantenimiento"));
        maintenance.add(new PermissionInfo("com.openbravo.pos.sales.restaurant.JPanelPlaces", "Gestión de Mesas (Restaurant)", "Mantenimiento"));
        maintenance.add(new PermissionInfo("com.openbravo.pos.voucher.VoucherPanel", "Gestión de Vales", "Mantenimiento"));
        maintenance.add(new PermissionInfo("com.openbravo.pos.branches.JPanelBranchesManagement", "Administrar Sucursales", "Mantenimiento"));
        maintenance.add(new PermissionInfo("com.openbravo.pos.panels.JPanelPrinter", "Configuración de Impresoras", "Mantenimiento"));
        maintenance.add(new PermissionInfo("com.openbravo.pos.config.JPanelConfiguration", "Configuración del Sistema", "Mantenimiento"));
        maintenance.add(new PermissionInfo("Menu.ChangePassword", "Cambiar Contraseña", "Mantenimiento"));
        PERMISSIONS.put("Mantenimiento", maintenance);
        
        // REPORTES DE USUARIOS
        List<PermissionInfo> userReports = new ArrayList<>();
        userReports.add(new PermissionInfo("/com/openbravo/reports/users.bs", "Reporte: Usuarios", "Reportes de Usuarios"));
        userReports.add(new PermissionInfo("/com/openbravo/reports/usersales.bs", "Reporte: Ventas por Usuario", "Reportes de Usuarios"));
        userReports.add(new PermissionInfo("/com/openbravo/reports/usernosales.bs", "Reporte: Usuarios sin Ventas", "Reportes de Usuarios"));
        userReports.add(new PermissionInfo("/com/openbravo/reports/uservoids.bs", "Reporte: Anulaciones por Usuario", "Reportes de Usuarios"));
        PERMISSIONS.put("Reportes de Usuarios", userReports);
        
        // IMPORTACIÓN/EXPORTACIÓN
        List<PermissionInfo> imports = new ArrayList<>();
        imports.add(new PermissionInfo("com.openbravo.pos.imports.JPanelCSV", "Panel de CSV", "Importación/Exportación"));
        imports.add(new PermissionInfo("com.openbravo.pos.imports.JPanelCSVImport", "Importar CSV", "Importación/Exportación"));
        imports.add(new PermissionInfo("com.openbravo.pos.imports.StockQtyImport", "Importar Cantidades de Stock", "Importación/Exportación"));
        imports.add(new PermissionInfo("com.openbravo.pos.imports.CustomerCSVImport", "Importar Clientes CSV", "Importación/Exportación"));
        imports.add(new PermissionInfo("com.openbravo.pos.imports.JPanelCSVCleardb", "Limpiar Base de Datos", "Importación/Exportación"));
        imports.add(new PermissionInfo("com.unicenta.pos.transfer.Transfer", "Transferencia de Datos", "Importación/Exportación"));
        PERMISSIONS.put("Importación/Exportación", imports);
        
        // HERRAMIENTAS
        List<PermissionInfo> tools = new ArrayList<>();
        tools.add(new PermissionInfo("/com/openbravo/reports/tools_newproducts.bs", "Herramienta: Nuevos Productos", "Herramientas"));
        tools.add(new PermissionInfo("/com/openbravo/reports/tools_updatedprices.bs", "Herramienta: Precios Actualizados", "Herramientas"));
        tools.add(new PermissionInfo("/com/openbravo/reports/tools_inventoryqtyupdate.bs", "Herramienta: Actualizar Cantidades", "Herramientas"));
        tools.add(new PermissionInfo("/com/openbravo/reports/tools_badprice.bs", "Herramienta: Precios Incorrectos", "Herramientas"));
        tools.add(new PermissionInfo("/com/openbravo/reports/tools_invalidcategory.bs", "Herramienta: Categorías Inválidas", "Herramientas"));
        tools.add(new PermissionInfo("/com/openbravo/reports/tools_missingdata.bs", "Herramienta: Datos Faltantes", "Herramientas"));
        tools.add(new PermissionInfo("/com/openbravo/reports/tools_invaliddata.bs", "Herramienta: Datos Inválidos", "Herramientas"));
        PERMISSIONS.put("Herramientas", tools);
        
        // EMPLEADOS (EPM)
        List<PermissionInfo> employees = new ArrayList<>();
        employees.add(new PermissionInfo("com.openbravo.pos.forms.MenuEmployees", "Menú Empleados", "Gestión de Empleados"));
        employees.add(new PermissionInfo("com.openbravo.pos.epm.BreaksPanel", "Gestión de Descansos", "Gestión de Empleados"));
        employees.add(new PermissionInfo("com.openbravo.pos.epm.LeavesPanel", "Gestión de Ausencias", "Gestión de Empleados"));
        employees.add(new PermissionInfo("com.openbravo.pos.epm.JPanelEmployeePresence", "Control de Presencia", "Gestión de Empleados"));
        employees.add(new PermissionInfo("/com/openbravo/reports/epm_dailypresence.bs", "Reporte: Presencia Diaria", "Gestión de Empleados"));
        employees.add(new PermissionInfo("/com/openbravo/reports/epm_dailyschedule.bs", "Reporte: Horario Diario", "Gestión de Empleados"));
        employees.add(new PermissionInfo("/com/openbravo/reports/epm_performance.bs", "Reporte: Rendimiento", "Gestión de Empleados"));
        PERMISSIONS.put("Gestión de Empleados", employees);
    }
    
    public static Map<String, List<PermissionInfo>> getAllPermissions() {
        return PERMISSIONS;
    }
    
    public static List<String> getCategories() {
        return new ArrayList<>(PERMISSIONS.keySet());
    }
    
    public static List<PermissionInfo> getPermissionsByCategory(String category) {
        return PERMISSIONS.getOrDefault(category, new ArrayList<>());
    }
}
