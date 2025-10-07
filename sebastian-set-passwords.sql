-- ========================================
-- SCRIPT: Establecer Contraseñas Seguras
-- Sistema: Sebastian POS Modernizado
-- Fecha: Octubre 7, 2025
-- ========================================

USE kriolosexpress;

-- Mostrar estado actual de usuarios
SELECT 'ESTADO ACTUAL:' as INFO;
SELECT ID, NAME, 
       CASE 
           WHEN APPPASSWORD IS NULL THEN 'SIN CONTRASEÑA' 
           ELSE 'CON CONTRASEÑA' 
       END as PASSWORD_STATUS, 
       ROLE, 
       CASE 
           WHEN VISIBLE = 1 THEN 'VISIBLE' 
           ELSE 'OCULTO' 
       END as VISIBILITY_STATUS
FROM people 
ORDER BY ID;

-- ========================================
-- ACTUALIZAR CONTRASEÑAS CON HASH SHA-1 (FORMATO CORRECTO)
-- ========================================

-- Administrator - Password: admin2024
-- Hash SHA-1: sha1:db7db5897571e433fd1ebc420d06eb91142aaffb
UPDATE people 
SET apppassword = 'sha1:db7db5897571e433fd1ebc420d06eb91142aaffb' 
WHERE id = '0' AND name = 'Administrator';

-- Manager - Password: manager2024  
-- Hash SHA-1: sha1:67c8efa46828cd323e1973d01c9b257c96191b91
UPDATE people 
SET apppassword = 'sha1:67c8efa46828cd323e1973d01c9b257c96191b91' 
WHERE id = '1' AND name = 'Manager';

-- Employee - Password: empleado2024
-- Hash SHA-1: sha1:a47ed520f0b588d47987446a2293445ac09bd025
UPDATE people 
SET apppassword = 'sha1:a47ed520f0b588d47987446a2293445ac09bd025' 
WHERE id = '2' AND name = 'Employee';

-- Guest - Password: guest2024
-- Hash SHA-1: sha1:09e89404b17a4f5dd136ca819233ddf9384ae730
UPDATE people 
SET apppassword = 'sha1:09e89404b17a4f5dd136ca819233ddf9384ae730' 
WHERE id = '3' AND name = 'Guest';

-- ========================================
-- VERIFICAR CAMBIOS
-- ========================================

SELECT 'ESTADO DESPUÉS DE ACTUALIZAR:' as INFO;
SELECT ID, NAME, 
       CASE 
           WHEN APPPASSWORD IS NULL THEN 'SIN CONTRASEÑA' 
           WHEN LENGTH(APPPASSWORD) = 64 THEN 'CONTRASEÑA SHA-256 ✓' 
           ELSE 'CONTRASEÑA CONFIGURADA' 
       END as PASSWORD_STATUS, 
       ROLE,
       CASE 
           WHEN VISIBLE = 1 THEN 'VISIBLE' 
           ELSE 'OCULTO' 
       END as VISIBILITY_STATUS
FROM people 
ORDER BY ID;

-- ========================================
-- INFORMACIÓN DE CREDENCIALES
-- ========================================

SELECT 'CREDENCIALES DE ACCESO:' as INFO;
SELECT 'Administrator' as USUARIO, 'admin2024' as PASSWORD, 'Acceso completo' as DESCRIPCION
UNION ALL
SELECT 'Manager' as USUARIO, 'manager2024' as PASSWORD, 'Gestión + Modal efectivo' as DESCRIPCION  
UNION ALL
SELECT 'Employee' as USUARIO, 'empleado2024' as PASSWORD, 'Ventas + Modal efectivo' as DESCRIPCION
UNION ALL
SELECT 'Guest' as USUARIO, 'guest2024' as PASSWORD, 'Solo lectura' as DESCRIPCION;

-- ========================================
-- COMMIT CAMBIOS
-- ========================================

COMMIT;

SELECT 'SCRIPT COMPLETADO EXITOSAMENTE ✓' as RESULTADO;
SELECT 'Reinicia la aplicación para aplicar los cambios' as INSTRUCCIONES;