# Crear Ejecutable .exe para KriolOS POS

Este documento explica cómo crear un ejecutable .exe optimizado para KriolOS POS que inicie en aproximadamente 5 segundos.

## Opciones Disponibles

### 1. Launcher Optimizado (Más Rápido - Recomendado)

**Archivo:** `kriolos-pos-launcher.bat`

Este es el método más rápido y simple. Solo ejecuta el launcher optimizado:

```batch
kriolos-pos-launcher.bat
```

**Características:**
- Inicio optimizado con opciones JVM para carga rápida
- Configuración de memoria optimizada
- Garbage Collector G1 para mejor rendimiento
- Sin verificación de bytecode para inicio más rápido

**Ventajas:**
- No requiere herramientas adicionales
- Funciona inmediatamente después de compilar
- Inicio muy rápido (~5 segundos)

**Desventajas:**
- Sigue siendo un archivo .bat (no .exe nativo)
- Muestra una ventana de consola brevemente

---

### 2. Crear .exe con jpackage (Recomendado para distribución)

**Archivo:** `create-exe.bat`

Requiere JDK 14 o superior con jpackage incluido.

```batch
create-exe.bat
```

**Características:**
- Crea un ejecutable .exe nativo
- Incluye todas las dependencias
- Puede crear un instalador completo
- Optimizado para inicio rápido

**Ventajas:**
- Ejecutable nativo real (.exe)
- Puede distribuirse sin requerir Java instalado (si se incluye JRE)
- Profesional y completo

**Desventajas:**
- Requiere JDK 14+
- Tarda más tiempo en crear el ejecutable
- Genera archivos más grandes

**Resultado:** `dist\KriolOS-POS\KriolOS-POS.exe`

---

### 3. Crear .exe con Launch4j

**Archivo:** `create-exe-launch4j.bat`

Requiere Launch4j instalado: https://launch4j.sourceforge.net/

```batch
create-exe-launch4j.bat
```

**Características:**
- Crea un wrapper .exe alrededor del JAR
- Configuración personalizable
- Optimizado para inicio rápido

**Ventajas:**
- Ejecutable .exe nativo
- Fácil de configurar
- Herramienta gratuita y popular

**Desventajas:**
- Requiere Launch4j instalado
- Requiere Java instalado en el sistema destino

**Resultado:** `dist\KriolOS-POS.exe`

---

### 4. Convertir .bat a .exe con herramientas externas

Puedes usar herramientas gratuitas para convertir `kriolos-pos-launcher.bat` en un .exe:

**Opciones:**
- **Bat To Exe Converter**: https://www.battoexeconverter.com/
- **IExpress** (incluido en Windows): Herramienta de Microsoft
- **PS2EXE** (PowerShell): Para scripts PowerShell

---

## Optimizaciones para Inicio Rápido

Todos los scripts incluyen las siguientes optimizaciones JVM:

```batch
-Xms256m                    # Memoria inicial pequeña
-Xmx2g                       # Memoria máxima suficiente
-XX:+UseG1GC                 # Garbage Collector G1 (mejor para inicio)
-XX:+UseStringDeduplication  # Optimización de strings
-Dsun.java2d.d3d=false       # Desactivar aceleración 3D
-Dsun.java2d.noddraw=true    # Desactivar DirectDraw
-Xverify:none                # Sin verificación de bytecode (más rápido)
-XX:TieredStopAtLevel=1      # Compilación JIT más rápida
-XX:+TieredCompilation       # Compilación por niveles
-Dfile.encoding=UTF-8        # Encoding UTF-8
```

---

## Requisitos Previos

1. **Compilar el proyecto:**
   ```batch
   mvn clean install -DskipTests
   ```

2. **Verificar que el JAR existe:**
   ```
   kriolos-opos-app\target\kriolos-pos.jar
   ```

3. **Java instalado:**
   - Mínimo: Java 21
   - Recomendado: JDK 21+ (para jpackage)

---

## Pasos Rápidos

### Opción Más Rápida (Launcher .bat):
```batch
kriolos-pos-launcher.bat
```

### Opción Profesional (.exe con jpackage):
```batch
create-exe.bat
```

### Opción con Launch4j:
1. Instalar Launch4j desde: https://launch4j.sourceforge.net/
2. Ejecutar: `create-exe-launch4j.bat`

---

## Solución de Problemas

### Error: "No se encuentra el archivo JAR"
**Solución:** Compila el proyecto primero:
```batch
mvn clean install -DskipTests
```

### Error: "jpackage no está disponible"
**Solución:** 
- Instala JDK 14 o superior
- O usa `create-exe-launch4j.bat` con Launch4j
- O usa `kriolos-pos-launcher.bat` (más simple)

### La aplicación tarda mucho en iniciar
**Solución:**
- Verifica que estás usando el launcher optimizado
- Asegúrate de tener suficiente RAM disponible
- Cierra otras aplicaciones Java que puedan estar ejecutándose

### El .exe no funciona en otra computadora
**Solución:**
- Si usaste Launch4j: Asegúrate de que Java esté instalado en la computadora destino
- Si usaste jpackage: Incluye el JRE en el paquete usando `--runtime-image`

---

## Notas Adicionales

- El launcher optimizado (`kriolos-pos-launcher.bat`) es la opción más rápida para desarrollo
- Para distribución, usa `jpackage` o `Launch4j` para crear un .exe profesional
- Todos los scripts están optimizados para iniciar en ~5 segundos
- Las opciones JVM pueden ajustarse según las necesidades del sistema

---

## Contacto y Soporte

Para más información, consulta la documentación del proyecto o el repositorio en GitHub.

