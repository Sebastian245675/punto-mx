-- ======================================================
-- Migration SQL: Add birthday and employee profile fields
-- Target Table: usuarios (Supabase)
-- Run this script in your Supabase SQL Editor
-- ======================================================

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS telefono TEXT;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email TEXT;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS direccion TEXT;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS colonia TEXT;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS cumpleanos TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS pelicula TEXT;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS libro TEXT;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fecha_inscripcion TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS empleo TEXT;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS notas TEXT;

-- Index for searching and filtering by birthday month
CREATE INDEX IF NOT EXISTS idx_usuarios_cumpleanos ON usuarios(cumpleanos);
