-- Script para agregar el campo accumulates_points a la tabla products
-- Sebastian - Sistema de puntos selectivo por producto
-- Fecha: 2025-10-24

-- Agregar la columna si no existe
SET @dbname = DATABASE();
SET @tablename = 'products';
SET @columnname = 'accumulates_points';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_NAME = @tablename)
      AND (TABLE_SCHEMA = @dbname)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " bit(1) NOT NULL DEFAULT b'1'")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Por defecto, todos los productos existentes acumulan puntos
UPDATE products 
SET accumulates_points = b'1' 
WHERE accumulates_points IS NULL OR accumulates_points = b'0';
