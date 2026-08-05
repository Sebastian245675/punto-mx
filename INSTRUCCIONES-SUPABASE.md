# Instrucciones para crear las tablas en Supabase

## Pasos para ejecutar el script SQL

1. **Accede al panel de Supabase:**
   - Ve a https://supabase.com/dashboard
   - Inicia sesión con tu cuenta
   - Selecciona tu proyecto: `knysrejfucsooraqbqeq`

2. **Abre el SQL Editor:**
   - En el menú lateral, haz clic en **"SQL Editor"** (o "Editor SQL")
   - Haz clic en **"New query"** (Nueva consulta)

3. **Copia y pega el script:**
   - Abre el archivo `crear-tablas-supabase.sql`
   - Copia todo el contenido (Ctrl+A, Ctrl+C)
   - Pégalo en el editor SQL de Supabase (Ctrl+V)

4. **Ejecuta el script:**
   - Haz clic en el botón **"Run"** (Ejecutar) o presiona `Ctrl+Enter`
   - Espera a que termine la ejecución (puede tardar unos segundos)

5. **Verifica que las tablas se crearon:**
   - Ve a **"Table Editor"** en el menú lateral
   - Deberías ver las siguientes tablas:
     - `productos`
     - `usuarios`
     - `clientes`
     - `categorias`
     - `impuestos`
     - `ventas`
     - `cierres`
     - `puntos_historial`
     - `formas_de_pago`
     - `config`
     - `inventario`

## Notas importantes

- ✅ El script usa `CREATE TABLE IF NOT EXISTS`, por lo que puedes ejecutarlo varias veces sin problemas
- ✅ Todas las tablas tienen índices para mejorar el rendimiento
- ✅ Las tablas incluyen campos `created_at` y `updated_at` que se actualizan automáticamente
- ✅ La tabla `ventas` usa `JSONB` para almacenar las líneas de venta (más flexible)

## Si hay errores

Si encuentras algún error al ejecutar el script:

1. **Error de permisos:** Asegúrate de estar usando la cuenta correcta con permisos de administrador
2. **Error de sintaxis:** Verifica que copiaste todo el script completo
3. **Tabla ya existe:** Si una tabla ya existe y quieres recrearla, primero elimínala con:
   ```sql
   DROP TABLE IF EXISTS nombre_tabla CASCADE;
   ```

## Después de crear las tablas

Una vez que las tablas estén creadas, podrás:

- ✅ Subir productos desde el sistema POS
- ✅ Sincronizar usuarios, clientes, ventas, etc.
- ✅ Usar todas las funciones de sincronización con Supabase

## Verificar la conexión

Para verificar que todo funciona:

1. Ve a la sección de **Productos** en el sistema POS
2. Haz clic en **"Subir a Supabase"**
3. Si las tablas están creadas correctamente, los productos se subirán sin errores
