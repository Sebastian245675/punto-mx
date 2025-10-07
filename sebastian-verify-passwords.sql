-- ========================================
-- VERIFICAR CONTRASEÑAS DESPUÉS DE LA ACTUALIZACIÓN
-- ========================================

-- Ver el estado actual de todas las contraseñas
SELECT 
    id,
    name,
    role,
    CASE 
        WHEN apppassword IS NULL THEN 'NULL (PELIGROSO)'
        WHEN apppassword = '' THEN 'VACÍA (PELIGROSO)'
        WHEN apppassword LIKE 'sha1:%' THEN 'SHA-1 HASH (SEGURO)'
        WHEN apppassword LIKE 'plain:%' THEN 'TEXTO PLANO (INSEGURO)'
        ELSE 'FORMATO DESCONOCIDO'
    END as password_status,
    LEFT(apppassword, 10) as password_preview
FROM people
ORDER BY id;

-- Ver solo los usuarios con contraseñas inseguras
SELECT 
    id,
    name,
    'CONTRASEÑA INSEGURA DETECTADA' as warning
FROM people 
WHERE apppassword IS NULL 
   OR apppassword = ''
   OR apppassword LIKE 'plain:%';