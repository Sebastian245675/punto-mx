-- Sebastian POS - Agregar campo de fondo inicial a closedcash
-- Ejecutar este script para agregar la funcionalidad de fondo inicial

-- Agregar campo initial_amount a la tabla closedcash
ALTER TABLE closedcash 
ADD COLUMN initial_amount DECIMAL(10,2) DEFAULT 0.00 NOT NULL;

-- Comentario del campo
-- COMMENT ON COLUMN closedcash.initial_amount IS 'Fondo inicial de caja en pesos mexicanos';

-- Verificar que se agregó correctamente
-- SELECT * FROM closedcash LIMIT 1;