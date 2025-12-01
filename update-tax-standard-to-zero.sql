-- Script para actualizar el impuesto estándar a 0%
-- Ejecutar este script en la base de datos existente

-- Para PostgreSQL
UPDATE taxes SET rate = 0 WHERE id = '001' AND name = 'Tax Standard';

-- Para MySQL (usar solo uno según tu base de datos)
-- UPDATE taxes SET rate = 0 WHERE id = '001' AND name = 'Tax Standard';

