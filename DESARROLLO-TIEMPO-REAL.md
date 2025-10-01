# 🚀 Desarrollo en Tiempo Real - Kriol POS

Este documento explica cómo configurar un entorno de desarrollo que detecta automáticamente cambios en archivos Java y recompila el proyecto.

## 📋 Requisitos Previos

- Java 17+
- Maven 3.6+
- PowerShell (incluido en Windows)

## 🔧 Opciones de Desarrollo

### ✅ Opción 1: Monitoreo Automático (Recomendado)

Ejecuta el script que monitorea cambios automáticamente:

```powershell
cd d:\unicenta-pos
powershell -ExecutionPolicy Bypass -File .\simple-watch.ps1
```

**¿Qué hace?**
- 👀 Monitorea todos los archivos `.java` en `kriolos-opos-app/src/main/java`
- 🔄 Detecta cambios automáticamente
- ⚡ Recompila solo lo necesario con `mvn compile`
- ✅ Muestra resultados en tiempo real

### 🔨 Opción 2: Compilación Rápida Manual

Si prefieres control manual, usa:

```batch
# Windows
.\quick-compile.bat

# O directamente con Maven
mvn compile -DskipTests -pl kriolos-opos-app
```

## 🎯 Workflow de Desarrollo

### 1. **Iniciar el Monitoreo**
```powershell
powershell -ExecutionPolicy Bypass -File .\simple-watch.ps1
```

### 2. **Modificar Código**
- Edita cualquier archivo `.java` en tu IDE favorito
- El sistema detectará el cambio automáticamente
- Verás mensajes como: `"Cambio detectado en: MiArchivo.java"`

### 3. **Ejecutar la Aplicación**
En otra terminal:
```bash
mvn spring-boot:run -pl kriolos-opos-app
```

### 4. **Reiniciar Aplicación** (cuando sea necesario)
- Detén la aplicación (Ctrl+C)
- Vuelve a ejecutar: `mvn spring-boot:run -pl kriolos-opos-app`

## 🔍 Credenciales por Defecto

- **Usuario:** `admin`
- **Contraseña:** `admin`

## 📁 Estructura del Proyecto

```
kriolos-opos-app/
├── src/main/java/           # ← Archivos monitoreados
├── pom.xml                  # ← Configuración Maven
└── target/                  # ← Archivos compilados
```

## 🛠️ Solución de Problemas

### Error de Compilación
Si ves errores durante la compilación:
1. Revisa la sintaxis del archivo modificado
2. Verifica imports y dependencias
3. Ejecuta `mvn clean compile` manualmente para más detalles

### Monitoreo No Detecta Cambios
1. Verifica que el archivo esté en `kriolos-opos-app/src/main/java/`
2. Asegúrate de que sea un archivo `.java`
3. Reinicia el script de monitoreo

### Aplicación No Refleja Cambios
- La aplicación requiere reinicio para reflejar cambios compilados
- Detén con Ctrl+C y vuelve a ejecutar `mvn spring-boot:run`

## 💡 Tips de Desarrollo

1. **Mantén el Monitoreo Activo**: Deja corriendo `simple-watch.ps1` durante todo el desarrollo
2. **Compilaciones Incrementales**: Maven solo recompila archivos modificados
3. **IDE Integration**: Funciona con cualquier IDE (VS Code, IntelliJ, Eclipse)
4. **Git Workflow**: Los archivos `.bat` y `.ps1` son herramientas locales (no commitear)

## 🏃‍♂️ Comando Rápido de Inicio

Para empezar desarrollo inmediatamente:

```powershell
# Terminal 1: Monitoreo
cd d:\unicenta-pos
powershell -ExecutionPolicy Bypass -File .\simple-watch.ps1

# Terminal 2: Aplicación  
cd d:\unicenta-pos
mvn spring-boot:run -pl kriolos-opos-app
```

¡Ahora ya tienes desarrollo en tiempo real configurado! 🎉