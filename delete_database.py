#!/usr/bin/env python3
import mysql.connector

try:
    # Conectar a MySQL
    conn = mysql.connector.connect(
        host="localhost",
        user="kriolopos",
        password="kriolopos"
    )
    
    cursor = conn.cursor()
    
    print("✓ Conectado a MySQL")
    print("\n--- Eliminando base de datos kriolopos ---")
    
    # Eliminar la base de datos
    cursor.execute("DROP DATABASE IF EXISTS kriolopos")
    print("✓ Base de datos eliminada")
    
    # Recrear vacía
    cursor.execute("CREATE DATABASE kriolopos")
    print("✓ Base de datos recreada vacía")
    
    cursor.execute("USE kriolopos")
    
    conn.commit()
    cursor.close()
    conn.close()
    
    print("\n✓ Operación completada exitosamente")
    print("La próxima vez que inicie la aplicación, recreará la BD con los nuevos cambios.")
    
except mysql.connector.Error as err:
    print(f"✗ Error de MySQL: {err}")
except Exception as e:
    print(f"✗ Error: {e}")
