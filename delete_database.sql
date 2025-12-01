-- Script para eliminar y recrear la base de datos kriolopos
-- Esto forzará a la aplicación a recrearla desde cero con los cambios en los XML

DROP DATABASE IF EXISTS kriolopos;
CREATE DATABASE kriolopos;
USE kriolopos;

-- La aplicación recreará todas las tablas automáticamente la próxima vez que inicie
