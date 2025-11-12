-- Script para agregar el campo accumulates_points a la tabla products (HSQLDB)
-- Sebastian - Sistema de puntos selectivo por producto
-- Fecha: 2025-10-26

-- Para HSQLDB, la sintaxis es diferente a MySQL
-- Agregar la columna si no existe
ALTER TABLE products ADD COLUMN accumulates_points BOOLEAN DEFAULT TRUE NOT NULL;

-- Por defecto, todos los productos existentes acumulan puntos
UPDATE products 
SET accumulates_points = TRUE 
WHERE accumulates_points IS NULL;
