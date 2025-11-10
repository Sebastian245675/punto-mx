-- Script para agregar columnas faltante_cierre y sobrante_cierre a la tabla closedcash
-- Ejecutar este script en la base de datos local si las columnas no existen

-- Para HSQLDB (base de datos local por defecto)
ALTER TABLE closedcash ADD COLUMN faltante_cierre NUMERIC(10,2) DEFAULT 0.00;
ALTER TABLE closedcash ADD COLUMN sobrante_cierre NUMERIC(10,2) DEFAULT 0.00;

-- Para MySQL (si se usa MySQL en lugar de HSQLDB)
-- ALTER TABLE closedcash ADD COLUMN faltante_cierre DECIMAL(10,2) DEFAULT 0.00;
-- ALTER TABLE closedcash ADD COLUMN sobrante_cierre DECIMAL(10,2) DEFAULT 0.00;

-- Para PostgreSQL (si se usa PostgreSQL)
-- ALTER TABLE closedcash ADD COLUMN faltante_cierre NUMERIC(10,2) DEFAULT 0.00;
-- ALTER TABLE closedcash ADD COLUMN sobrante_cierre NUMERIC(10,2) DEFAULT 0.00;

-- Nota: Si las columnas ya existen, estos comandos fallarán.
-- Para verificar si las columnas existen, ejecutar:
-- SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'closedcash' AND COLUMN_NAME IN ('faltante_cierre', 'sobrante_cierre');

