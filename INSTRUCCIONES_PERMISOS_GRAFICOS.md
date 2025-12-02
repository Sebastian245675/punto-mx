# Solución: Agregar Permiso de Gráficos

El error `NO PERMISSION` ocurre porque el permiso no está en la base de datos. Aquí están las soluciones:

## ✅ Opción 1: Script SQL Directo (MÁS RÁPIDO)

### Para Windows con HSQLDB:

1. **Abre una terminal en la carpeta del proyecto**

2. **Conecta a la base de datos usando HSQLDB DatabaseManager:**

```bash
java -cp "kriolos-opos-app/target/lib/hsqldb-*.jar" org.hsqldb.util.DatabaseManager
```

3. **En la ventana que se abre:**
   - Type: `HSQL Database Engine Standalone`
   - URL: `jdbc:hsqldb:file:C:/Users/Nadie/sebastian-pos-database/kriolos`
   - User: `SA`
   - Password: (dejar vacío)
   - Click "OK"

4. **Ejecuta este comando SQL:**

```sql
UPDATE roles 
SET permissions = STRINGTOBLOB(
    BLOBTOSTRING(permissions) || ';com.openbravo.pos.reports.JPanelGraphics'
)
WHERE id = '1'
AND BLOBTOSTRING(permissions) NOT LIKE '%JPanelGraphics%';

COMMIT;
```

5. **Verifica que funcionó:**

```sql
SELECT id, name, 
       CASE 
           WHEN BLOBTOSTRING(permissions) LIKE '%JPanelGraphics%' THEN 'PERMISO OK'
           ELSE 'NO ENCONTRADO'
       END as estado
FROM roles 
WHERE id = '1';
```

6. **Cierra sesión y vuelve a entrar en la aplicación**

---

## ✅ Opción 2: Borrar y Recrear Base de Datos (LIMPIA)

Si prefieres empezar de cero con los permisos correctos:

1. **Cierra la aplicación completamente**

2. **Borra la carpeta de la base de datos:**

```bash
# En PowerShell
Remove-Item -Recurse -Force "C:\Users\Nadie\sebastian-pos-database"
```

O manualmente elimina la carpeta: `C:\Users\Nadie\sebastian-pos-database`

3. **Ejecuta la aplicación de nuevo:**

```bash
./build-and-run.bat
```

La base de datos se recreará con todos los permisos actualizados, incluyendo el de gráficos.

---

## ✅ Opción 3: Agregar Permiso a Todos los Roles

Si quieres que TODOS los usuarios tengan acceso a gráficos:

```sql
UPDATE roles 
SET permissions = STRINGTOBLOB(
    BLOBTOSTRING(permissions) || ';com.openbravo.pos.reports.JPanelGraphics'
)
WHERE BLOBTOSTRING(permissions) NOT LIKE '%JPanelGraphics%';

COMMIT;
```

---

## 📝 Notas Importantes:

1. **El permiso se llama:** `com.openbravo.pos.reports.JPanelGraphics`

2. **El problema ocurre** porque `DefaultRolesInitializer` falla al actualizar los permisos BLOB.

3. **Después de aplicar cualquier solución**, cierra sesión y vuelve a entrar para que los permisos se recarguen.

---

## 🔍 Para Verificar que Funcionó:

1. Inicia sesión como `admin`
2. Haz clic en el botón "Reportes" en la barra superior
3. Deberías ver el panel de gráficos con:
   - Gráfico circular de Ganancia por Departamento
   - Gráfico de barras de Ventas por forma de pago
   - Tabla de Ventas por Departamento
   - Tabs con diferentes periodos

---

## ❓ Si Aún No Funciona:

Ejecuta este comando SQL para ver qué permisos tiene el rol ADMIN:

```sql
SELECT id, name, BLOBTOSTRING(permissions) as permisos
FROM roles 
WHERE id = '1';
```

Y comparte el resultado para ayudarte mejor.

