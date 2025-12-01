-- Script para agregar el permiso Menu.Branches a la base de datos
-- Ejecutar en MySQL con: mysql -u kriolopos -p kriolopos kriolopos < add_menu_branches_permission.sql

USE kriolopos;

-- Ver permisos existentes (opcional)
SELECT ID FROM permissions LIMIT 5;

-- Agregar Menu.Branches si no existe
INSERT IGNORE INTO permissions (ID, PERMISSIONS) 
VALUES ('Menu.Branches', CONVERT(X'3C706572 6D697373 696F6E73 3E3C2F70 65726D69 73736961 6E733E', CHAR));

-- Verificar que se agregó correctamente
SELECT ID FROM permissions WHERE ID = 'Menu.Branches';
