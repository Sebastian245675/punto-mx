# Diagnóstico: Por qué el stock no se descuenta

## Posibles Causas

### 1. Productos marcados como servicio
Si un producto tiene `isservice = true` en la tabla `products`, NO se descuenta stock.

**Verificación SQL:**
```sql
SELECT id, name, isservice 
FROM products 
WHERE isservice = true;
```

**Solución:** Si el producto NO debería ser servicio, actualizar:
```sql
UPDATE products SET isservice = false WHERE id = 'ID_DEL_PRODUCTO';
```

### 2. Ubicación (location) no coincide
El stock se guarda por ubicación. Si la ubicación usada al guardar la venta no coincide con la que está en `stockcurrent`, el UPDATE no encuentra el registro.

**Verificación SQL:**
```sql
-- Ver qué ubicaciones hay en stockcurrent
SELECT DISTINCT location FROM stockcurrent;

-- Ver qué ubicación se está usando (revisar logs o configuración)
-- La ubicación debe coincidir exactamente
```

**Solución:** Asegurarse de que la ubicación configurada en la aplicación coincida con la de `stockcurrent`.

### 3. ATTRIBUTESETINSTANCE_ID no coincide
Si el producto tiene atributos (talla, color, etc.) y el `ATTRIBUTESETINSTANCE_ID` no coincide, el UPDATE no encuentra el registro.

**Verificación SQL:**
```sql
-- Ver registros de stock con atributos
SELECT location, product, attributesetinstance_id, units 
FROM stockcurrent 
WHERE product = 'ID_DEL_PRODUCTO';
```

### 4. El registro no existe en stockcurrent
Si no existe un registro en `stockcurrent` para el producto en esa ubicación, se intenta INSERTAR en lugar de ACTUALIZAR, pero podría fallar.

**Verificación SQL:**
```sql
-- Verificar si existe stock para un producto
SELECT * FROM stockcurrent 
WHERE location = 'UBICACION' 
AND product = 'ID_DEL_PRODUCTO';
```

## Cómo revisar los logs

Al guardar una venta, busca en los logs estos mensajes:

1. **Si se intenta descontar:**
   ```
   === REGISTRANDO MOVIMIENTO DE STOCK ===
   Product ID: [ID]
   Location: [UBICACION]
   Delta Units (negativo para venta): [CANTIDAD]
   Stock actual ANTES: [STOCK_ANTES]
   Stock actual DESPUÉS: [STOCK_DESPUES]
   ```

2. **Si el producto es servicio:**
   ```
   Skipping stock update: product is marked as SERVICE. Product ID: [ID]
   ```

3. **Si no se encuentra el registro:**
   ```
   UPDATE ejecutado - Filas afectadas: 0
   No se actualizó ninguna fila - Ejecutando INSERT
   ```

## Consultas útiles para diagnóstico

```sql
-- Ver productos que son servicios
SELECT id, name, isservice FROM products WHERE isservice = true;

-- Ver stock actual de un producto
SELECT sc.location, sc.product, p.name, sc.units, sc.attributesetinstance_id
FROM stockcurrent sc
JOIN products p ON sc.product = p.id
WHERE sc.product = 'ID_DEL_PRODUCTO';

-- Ver movimientos recientes de stock (stockdiary)
SELECT sd.datenew, sd.reason, sd.location, p.name, sd.units, sd.price
FROM stockdiary sd
JOIN products p ON sd.product = p.id
WHERE sd.product = 'ID_DEL_PRODUCTO'
ORDER BY sd.datenew DESC
LIMIT 10;

-- Ver ventas recientes de un producto
SELECT r.datenew, tl.product, p.name, tl.units, tl.price
FROM ticketlines tl
JOIN receipts r ON tl.ticket = r.id
JOIN products p ON tl.product = p.id
WHERE tl.product = 'ID_DEL_PRODUCTO'
ORDER BY r.datenew DESC
LIMIT 10;
```

## Pasos para resolver

1. **Revisar logs** al guardar una venta para ver qué mensajes aparecen
2. **Verificar en BD** si los productos están marcados como servicio
3. **Verificar ubicación** - debe coincidir entre la configuración y stockcurrent
4. **Verificar stockcurrent** - debe existir un registro para el producto en esa ubicación
5. **Revisar ATTRIBUTESETINSTANCE_ID** si el producto tiene atributos

