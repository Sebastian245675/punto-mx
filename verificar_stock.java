// Script para verificar el stock y diagnosticar problemas
// Ejecutar desde la aplicación o como consulta SQL

// 1. Verificar productos marcados como servicio que NO deberían serlo
SELECT id, name, isservice 
FROM products 
WHERE isservice = true 
AND id IN (
    SELECT DISTINCT product 
    FROM stockcurrent 
    WHERE units > 0
);

// 2. Verificar stock actual vs stock esperado
SELECT 
    sc.location,
    sc.product,
    p.name as product_name,
    sc.units as stock_actual,
    sc.attributesetinstance_id,
    (SELECT COALESCE(SUM(CASE 
        WHEN sd.reason = 0 THEN -sd.units  -- OUT_SALE (venta)
        WHEN sd.reason = 1 THEN sd.units   -- IN_REFUND (reembolso)
        WHEN sd.reason = 2 THEN sd.units   -- IN (entrada)
        WHEN sd.reason = 3 THEN -sd.units  -- OUT (salida)
        ELSE 0 
    END), 0)
    FROM stockdiary sd
    WHERE sd.location = sc.location 
    AND sd.product = sc.product
    AND ((sd.attributesetinstance_id IS NULL AND sc.attributesetinstance_id IS NULL)
         OR sd.attributesetinstance_id = sc.attributesetinstance_id)
    ) as stock_calculado_desde_movimientos
FROM stockcurrent sc
INNER JOIN products p ON sc.product = p.id
WHERE sc.units != 0
ORDER BY sc.location, p.name;

// 3. Verificar ventas recientes sin descuento de stock
SELECT 
    r.datenew,
    r.id as receipt_id,
    tl.product,
    p.name as product_name,
    tl.units as unidades_vendidas,
    sc.units as stock_antes_venta,
    (SELECT units FROM stockcurrent 
     WHERE location = (SELECT id FROM locations LIMIT 1)
     AND product = tl.product
     LIMIT 1) as stock_actual
FROM ticketlines tl
INNER JOIN receipts r ON tl.ticket = r.id
INNER JOIN products p ON tl.product = p.id
LEFT JOIN stockcurrent sc ON sc.product = tl.product
WHERE r.datenew >= DATEADD('DAY', -7, NOW())
AND p.isservice = false
AND tl.units > 0
ORDER BY r.datenew DESC
LIMIT 20;

// 4. Verificar movimientos de stock recientes
SELECT 
    sd.datenew,
    sd.reason,
    CASE sd.reason
        WHEN 0 THEN 'OUT_SALE (Venta)'
        WHEN 1 THEN 'IN_REFUND (Reembolso)'
        WHEN 2 THEN 'IN (Entrada)'
        WHEN 3 THEN 'OUT (Salida)'
        ELSE 'Desconocido'
    END as tipo_movimiento,
    sd.location,
    p.name as product_name,
    sd.units,
    sd.price
FROM stockdiary sd
INNER JOIN products p ON sd.product = p.id
WHERE sd.datenew >= DATEADD('DAY', -7, NOW())
ORDER BY sd.datenew DESC
LIMIT 50;

