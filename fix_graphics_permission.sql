-- SOLUCIÓN RÁPIDA: Agregar permiso de Gráficos al rol ADMIN
-- Ejecutar este script en tu base de datos HSQLDB

-- Paso 1: Ver los permisos actuales del rol ADMIN
SELECT id, name, CAST(permissions AS VARCHAR(10000)) as permisos_texto 
FROM roles 
WHERE id = '1';

-- Paso 2: Agregar el permiso manualmente al rol ADMIN
-- Como permissions es BLOB, necesitamos leer, modificar y escribir

-- Opción A: Si permissions está vacío o es null
UPDATE roles 
SET permissions = STRINGTOBLOB('com.openbravo.pos.reports.JPanelGraphics')
WHERE id = '1' 
AND (permissions IS NULL OR LENGTH(permissions) = 0);

-- Opción B: Si ya hay permisos, concatenar
-- Primero verificamos qué permisos tiene:
SELECT id, name, 
       CASE 
           WHEN permissions IS NULL THEN 'NULL'
           WHEN LENGTH(permissions) = 0 THEN 'VACIO'
           ELSE CAST(permissions AS VARCHAR(10000))
       END as estado_permisos
FROM roles 
WHERE id = '1';

-- Si tiene permisos, agregar el nuevo (usando ; como separador):
UPDATE roles 
SET permissions = STRINGTOBLOB(
    BLOBTOSTRING(permissions) || ';com.openbravo.pos.reports.JPanelGraphics'
)
WHERE id = '1'
AND permissions IS NOT NULL 
AND LENGTH(permissions) > 0
AND BLOBTOSTRING(permissions) NOT LIKE '%com.openbravo.pos.reports.JPanelGraphics%';

-- Verificar que se aplicó:
SELECT id, name, 
       CASE 
           WHEN BLOBTOSTRING(permissions) LIKE '%JPanelGraphics%' THEN '✓ PERMISO AGREGADO'
           ELSE '✗ NO ENCONTRADO'
       END as verificacion
FROM roles 
WHERE id = '1';

COMMIT;

