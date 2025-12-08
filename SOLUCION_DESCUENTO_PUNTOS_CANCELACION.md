# Solución: Descuento de Puntos al Cancelar Ventas

## Problema Identificado

Al cancelar una venta, el sistema no descontaba los puntos que ya se habían asignado al cliente durante la venta. Esto causaba que los clientes mantuvieran puntos que no les correspondían, generando inconsistencias en el sistema de puntos.

## Solución Implementada

Se implementó un sistema completo que detecta y descuenta automáticamente los puntos otorgados cuando se cancela una venta, además de actualizar el acumulable diario del cliente.

## Cambios Realizados

### 1. Nuevo Método en `PuntosDataLogic.java`

Se agregó el método `descontarPuntosPorCancelacion()` que realiza las siguientes operaciones:

#### Ubicación
- **Archivo**: `kriolos-opos-app/src/main/java/com/openbravo/pos/customers/PuntosDataLogic.java`
- **Línea**: ~579

#### Funcionalidad

```java
public void descontarPuntosPorCancelacion(String ticketId, String clienteId, double montoAcumulableTicket)
```

El método:

1. **Busca en el historial** las transacciones de puntos relacionadas con el ticket cancelado
   - Utiliza el patrón de búsqueda: `"Venta automática #" + ticketId`
   - Consulta la tabla `PUNTOS_HISTORIAL` para encontrar todas las transacciones

2. **Calcula el total a descontar**
   - Suma todos los puntos otorgados encontrados en el historial
   - Suma todos los montos de las compras relacionadas

3. **Descuenta los puntos del cliente**
   - Verifica que el cliente tenga suficientes puntos
   - Si no tiene suficientes, descuenta solo los disponibles
   - Actualiza los puntos actuales del cliente
   - Registra la transacción con descripción: `"Cancelación venta #" + ticketId`

4. **Actualiza el acumulable diario**
   - Resta el monto de la venta cancelada del acumulable diario
   - Utiliza el monto del historial si está disponible, sino usa el monto del ticket
   - Asegura que el acumulable no sea negativo

5. **Elimina transacciones del historial**
   - Elimina todas las transacciones de `PUNTOS_HISTORIAL` relacionadas con el ticket cancelado
   - Mantiene la integridad de los datos

#### Características de Seguridad

- Manejo de errores robusto que no interrumpe la cancelación del ticket
- Validación de que el cliente tenga puntos suficientes antes de descontar
- Logs detallados para depuración y auditoría
- Protección contra valores negativos en el acumulable

### 2. Modificación en `DataLogicSales.java`

Se modificó el método `deleteTicket()` para integrar el descuento de puntos.

#### Ubicación
- **Archivo**: `kriolos-opos-domain/src/main/java/com/openbravo/pos/forms/DataLogicSales.java`
- **Línea**: ~2042

#### Cambios Realizados

```java
// Sebastian - Descontar puntos si el ticket tenía un cliente y se le otorgaron puntos
if (ticket.getCustomer() != null && ticket.getCustomer().getId() != null && ticket.getTicketId() > 0) {
    try {
        com.openbravo.pos.customers.PuntosDataLogic puntosDataLogic = new com.openbravo.pos.customers.PuntosDataLogic(s);
        String ticketIdStr = String.valueOf(ticket.getTicketId());
        String clienteId = ticket.getCustomer().getId();
        
        // Calcular el monto acumulable del ticket (solo productos que acumulan puntos)
        double totalAcumulable = 0.0;
        for (int i = 0; i < ticket.getLinesCount(); i++) {
            TicketLineInfo line = ticket.getLine(i);
            if (line.isProductAccumulatesPoints()) {
                totalAcumulable += line.getValue();
            }
        }
        
        puntosDataLogic.descontarPuntosPorCancelacion(ticketIdStr, clienteId, totalAcumulable);
        LOGGER.info("Puntos descontados por cancelación de ticket #" + ticketIdStr + " para cliente " + clienteId);
    } catch (Exception e) {
        // No interrumpir la cancelación del ticket si hay error con los puntos
        LOGGER.log(Level.WARNING, "Error descontando puntos al cancelar ticket: " + e.getMessage(), e);
    }
}
```

#### Funcionalidad

1. **Verifica condiciones necesarias**
   - Que el ticket tenga un cliente asignado
   - Que el cliente tenga un ID válido
   - Que el ticket tenga un ID válido

2. **Calcula el monto acumulable**
   - Itera sobre todas las líneas del ticket
   - Suma solo los productos que acumulan puntos (`isProductAccumulatesPoints()`)
   - Pasa este monto al método de descuento

3. **Llama al método de descuento**
   - Crea una instancia de `PuntosDataLogic` con la sesión actual
   - Ejecuta el descuento de puntos
   - Registra la operación en los logs

4. **Manejo de errores**
   - Captura cualquier excepción sin interrumpir la cancelación del ticket
   - Registra advertencias en el log para depuración

## Flujo de Ejecución

```
1. Usuario cancela una venta
   ↓
2. Se llama a deleteTicket() en DataLogicSales
   ↓
3. Se actualiza el inventario
   ↓
4. Se actualizan las deudas del cliente (si aplica)
   ↓
5. [NUEVO] Se calcula el monto acumulable del ticket
   ↓
6. [NUEVO] Se llama a descontarPuntosPorCancelacion()
   ↓
7. [NUEVO] Se buscan puntos en el historial
   ↓
8. [NUEVO] Se descuentan los puntos del cliente
   ↓
9. [NUEVO] Se actualiza el acumulable diario
   ↓
10. [NUEVO] Se eliminan transacciones del historial
   ↓
11. Se eliminan los registros del ticket
```

## Casos de Uso Cubiertos

### Caso 1: Venta con puntos otorgados
- **Escenario**: Cliente compra productos que acumulan puntos y recibe puntos
- **Al cancelar**: Se descuentan los puntos otorgados y se actualiza el acumulable

### Caso 2: Venta sin puntos otorgados (pero con productos acumulables)
- **Escenario**: Cliente compra productos que acumulan puntos pero no alcanza el umbral para puntos
- **Al cancelar**: Se actualiza el acumulable diario restando el monto, aunque no haya puntos que descontar

### Caso 3: Cliente sin puntos suficientes
- **Escenario**: Cliente ya usó los puntos otorgados o tiene menos puntos de los que se intentan descontar
- **Al cancelar**: Se descuentan solo los puntos disponibles, se registra en logs

### Caso 4: Ticket sin cliente
- **Escenario**: Ticket cancelado no tiene cliente asignado
- **Al cancelar**: No se ejecuta el descuento de puntos (comportamiento esperado)

## Logs y Depuración

El sistema genera logs detallados para facilitar la depuración:

```
🔄 descontarPuntosPorCancelacion INICIADO - Ticket: 123, Cliente: abc-123
📋 Transacción encontrada: 10 puntos, $400.0
💰 TOTAL A DESCONTAR: 10 puntos, $400.0
✅ Puntos descontados: 10 (tenía 50, ahora tiene 40)
🔄 Actualizando acumulable (historial): $800.0 - $400.0 = $400.0
🗑️ Transacciones del historial eliminadas para ticket #123
```

## Consideraciones Técnicas

### Base de Datos
- Utiliza la tabla `PUNTOS_HISTORIAL` para buscar transacciones
- Utiliza la tabla `CLIENTE_PUNTOS` para actualizar puntos del cliente
- Utiliza la tabla `PUNTOS_ACUMULABLE_DIARIO` para actualizar el acumulable

### Transacciones
- El descuento de puntos se ejecuta dentro de la misma transacción de cancelación del ticket
- Si hay error en el descuento, no se interrumpe la cancelación del ticket

### Rendimiento
- Búsqueda optimizada usando índices en `CLIENTE_ID` y `DESCRIPCION`
- Operaciones atómicas para mantener consistencia de datos

## Pruebas Recomendadas

1. **Cancelar venta con puntos otorgados**
   - Verificar que los puntos se descuenten correctamente
   - Verificar que el acumulable se actualice

2. **Cancelar venta sin puntos otorgados**
   - Verificar que el acumulable se actualice aunque no haya puntos

3. **Cancelar venta con cliente sin puntos suficientes**
   - Verificar que solo se descuenten los puntos disponibles
   - Verificar que se registre en logs

4. **Cancelar venta sin cliente**
   - Verificar que no se genere error
   - Verificar que la cancelación se complete normalmente

## Mantenimiento Futuro

### Posibles Mejoras
- Agregar notificación al usuario cuando se descuenten puntos
- Crear reporte de cancelaciones con descuento de puntos
- Agregar validación adicional para tickets muy antiguos
- Implementar reversión de puntos usados (si el cliente ya los canjeó)

### Archivos Modificados
- `kriolos-opos-app/src/main/java/com/openbravo/pos/customers/PuntosDataLogic.java`
- `kriolos-opos-domain/src/main/java/com/openbravo/pos/forms/DataLogicSales.java`

## Fecha de Implementación
- **Fecha**: Enero 2025
- **Desarrollador**: Sebastian
- **Versión**: Sistema de puntos v2.0

---

## Notas Adicionales

- El sistema mantiene compatibilidad con ventas anteriores que no tenían registro en el historial
- Los logs ayudan a identificar problemas sin interrumpir el flujo normal de trabajo
- La solución es robusta y maneja casos edge sin generar errores críticos
