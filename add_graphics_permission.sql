-- Script para agregar el permiso de Panel de Gráficos al rol de administrador
-- Ejecutar este script si tienes problemas de permisos con el panel de gráficos

-- Verificar si el permiso ya existe en el rol 'Administrator'
SELECT PERMISSIONS FROM roles WHERE NAME = 'Administrator';

-- Si el permiso 'com.openbravo.pos.reports.JPanelGraphics' NO está en la lista, ejecutar:
UPDATE roles 
SET PERMISSIONS = CONCAT(PERMISSIONS, ';com.openbravo.pos.reports.JPanelGraphics')
WHERE NAME = 'Administrator' 
AND PERMISSIONS NOT LIKE '%com.openbravo.pos.reports.JPanelGraphics%';

-- Verificar el resultado
SELECT PERMISSIONS FROM roles WHERE NAME = 'Administrator';

-- Si tienes rol 'ADMIN' en lugar de 'Administrator':
UPDATE roles 
SET PERMISSIONS = CONCAT(PERMISSIONS, ';com.openbravo.pos.reports.JPanelGraphics')
WHERE NAME = 'ADMIN' 
AND PERMISSIONS NOT LIKE '%com.openbravo.pos.reports.JPanelGraphics%';

-- Para agregar a TODOS los roles (si quieres dar acceso a todos):
UPDATE roles 
SET PERMISSIONS = CONCAT(PERMISSIONS, ';com.openbravo.pos.reports.JPanelGraphics')
WHERE PERMISSIONS NOT LIKE '%com.openbravo.pos.reports.JPanelGraphics%';

COMMIT;

