# Instrucciones para Crear el Ejecutable (.exe) con Icono

## Requisitos Previos

1. **Launch4j** instalado:
   - Descarga desde: https://launch4j.sourceforge.net/
   - O instala desde: `winget install Launch4j` (si tienes winget)

2. **Java 21+** instalado (para compilar el proyecto)

3. **Maven** instalado (para compilar el proyecto)

## Pasos para Generar el EXE

### Opción 1: Script Batch (Windows)

```batch
crear-exe-con-icono.bat
```

### Opción 2: Script PowerShell (Recomendado)

```powershell
.\crear-exe-con-icono.ps1
```

## Crear un Icono

Si no tienes un archivo `icono.ico`, puedes:

1. **Usar el script incluido:**
   ```powershell
   .\crear-icono.ps1
   ```
   (Nota: Este script crea un PNG que necesitas convertir a ICO)

2. **Descargar un icono:**
   - Visita: https://www.flaticon.com/
   - Busca un icono relacionado con "POS" o "ventas"
   - Descarga en formato .ico o convierte PNG a ICO

3. **Crear manualmente:**
   - Usa herramientas como IcoFX, GIMP, o Photoshop
   - Tamaño recomendado: 256x256 píxeles
   - Guarda como `icono.ico` en la raíz del proyecto

## Verificación del Tamaño

El script verificará automáticamente que el EXE no exceda 130 MB. Si lo excede:

### Opciones para Reducir el Tamaño:

1. **Optimizar el JAR:**
   ```bash
   mvn clean package -DskipTests
   ```

2. **Eliminar dependencias innecesarias** del `pom.xml`

3. **Usar ProGuard** para ofuscar y reducir el tamaño:
   ```xml
   <plugin>
       <groupId>com.github.wvengen</groupId>
       <artifactId>proguard-maven-plugin</artifactId>
       ...
   </plugin>
   ```

4. **Excluir recursos innecesarios** del JAR

## Ubicación del EXE Generado

El EXE se generará en:
- `D:\Descargas\CONNECTING-POS.exe` (si existe)
- `D:\Downloads\CONNECTING-POS.exe` (alternativa)
- `dist\CONNECTING-POS.exe` (si las anteriores no existen)

## Solución de Problemas

### Error: "Launch4j no encontrado"
- Instala Launch4j desde: https://launch4j.sourceforge.net/
- O coloca `launch4j.exe` en la carpeta `launch4j\` del proyecto

### Error: "JAR no encontrado"
- Compila el proyecto primero: `mvn clean package -DskipTests`
- Verifica que el JAR esté en: `kriolos-opos-app\target\kriolos-pos.jar`

### El EXE excede 130 MB
- Revisa el tamaño del JAR (debe ser < 120 MB)
- Optimiza las dependencias en `pom.xml`
- Considera usar un JRE embebido más pequeño

### El icono no aparece
- Verifica que el archivo `icono.ico` exista
- Asegúrate de que el icono sea válido (puedes abrirlo con un visor de imágenes)
- El icono debe estar en formato .ico, no .png o .jpg

## Configuración Avanzada

Puedes modificar `launch4j-config.xml` para personalizar:
- Opciones de JVM
- Mensajes de error
- Información de versión
- Comportamiento del ejecutable

## Notas Importantes

- El EXE incluye el JAR completo, por lo que el tamaño será similar al JAR + overhead de Launch4j (~5-10 MB)
- No incluye el JRE, el usuario debe tener Java 21+ instalado
- Para incluir el JRE, necesitarías usar herramientas como jlink o crear un instalador
