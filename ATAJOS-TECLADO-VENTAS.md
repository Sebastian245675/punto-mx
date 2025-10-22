# Atajos de Teclado - Módulo de Ventas

## 📝 Descripción
Se han implementado atajos de teclado para mejorar la eficiencia en el módulo de ventas de SebastianPOS.

## ⌨️ Atajos Disponibles

| Tecla | Acción | Descripción |
|-------|--------|-------------|
| **F12** | 💰 **Pagar / Cobrar** | Abre el diálogo de pago para procesar el ticket actual |
| **F2** | 🆔 **Panel ID Cliente** | Muestra el panel lateral para ingresar ID del cliente directamente |
| **F3** | 📋 **Historial de Pestañas** | Muestra la lista de tickets abiertos y guardados |
| **F4** | ➕ **Nueva Pestaña** | Crea un nuevo ticket de venta vacío |

## 🎯 Características Detalladas

### F12 - Pagar (Cobrar)
- **Función**: Procesa el pago del ticket actual
- **Condiciones**: 
  - Solo funciona cuando hay productos en el ticket
  - El botón de pagar debe estar habilitado
- **Comportamiento**:
  - Abre el diálogo de pago automáticamente
  - Si no hay productos, emite un pitido de advertencia
  - Equivalente a hacer clic en el botón verde "Pagar"
- **Implementación**: `m_jPayNow.doClick()`

### F2 - Panel ID Cliente (NUEVO)
- **Función**: Muestra el panel lateral para ingresar ID del cliente
- **Componentes del panel**:
  - Etiqueta "ID Cliente"
  - Campo de texto para ingresar el ID directamente
  - Etiqueta que muestra el nombre del cliente encontrado
- **Comportamiento**:
  - Hace visible el panel lateral que normalmente está oculto
  - Enfoca automáticamente el campo de texto
  - Selecciona el texto existente (si hay)
  - Al presionar Enter, busca el cliente por ID automáticamente
  - Actualiza el nombre del cliente y puntos de fidelidad
- **Implementación**: 
  - `m_jCustomerPanel.setVisible(true)`
  - `m_jCustomerId.requestFocus()`
  - `m_jCustomerId.selectAll()`
- **Nota**: Este atajo NO abre el diálogo de crear/buscar cliente (para eso está el botón)

### F3 - Historial de Pestañas
- **Función**: Gestiona múltiples tickets abiertos
- **Comportamiento**:
  - Muestra la bolsa de tickets (JTicketsBag)
  - Permite cambiar entre múltiples tickets abiertos
  - Guarda automáticamente el ticket actual antes de mostrar la lista
  - Útil para gestionar múltiples ventas simultáneas
- **Implementación**: `m_ticketsbag.activate()`

### F4 - Nueva Pestaña
- **Función**: Crear un nuevo ticket rápidamente
- **Comportamiento**:
  - Crea un ticket de venta completamente nuevo
  - Ideal para atender rápidamente a un nuevo cliente
  - Mantiene los tickets anteriores disponibles en el historial
  - Limpia el panel de productos
  - Reinicia el estado de la UI
- **Implementación**: `createNewTicket()`

## 🔧 Implementación Técnica

### Ubicación del Código
- **Archivo**: `kriolos-opos-app/src/main/java/com/openbravo/pos/sales/JPanelTicket.java`
- **Método principal**: `setupKeyboardShortcuts()`
- **Inicialización**: Se llama en el constructor después de `initComponents()`
- **Líneas**: 449-530 (aproximadamente)

### Tecnología Utilizada
```java
// Usa InputMap y ActionMap de Swing para atajos globales
InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
ActionMap actionMap = this.getActionMap();

// Ejemplo: F2 para mostrar panel de ID cliente
inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0), "mostrarPanelCliente");
actionMap.put("mostrarPanelCliente", new javax.swing.AbstractAction() {
    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        if (m_jCustomerPanel != null && m_jCustomerId != null) {
            m_jCustomerPanel.setVisible(true);
            m_jCustomerPanel.revalidate();
            javax.swing.SwingUtilities.invokeLater(() -> {
                m_jCustomerId.requestFocusInWindow();
                m_jCustomerId.selectAll();
            });
        }
    }
});
```

### Variables de Instancia Agregadas
```java
private javax.swing.JPanel m_jCustomerPanel; // Panel lateral para ID de cliente
```

### Logging y Depuración
Todos los atajos incluyen logging detallado para depuración:
- `System.Logger.Level.DEBUG` para eventos normales
- `System.Logger.Level.WARNING` para errores o condiciones null
- Los logs incluyen: inicio de acción, ruta del método, acciones ejecutadas, estado final

## 📦 Cambios en el Código

### Modificaciones realizadas:
1. **Nuevo panel de cliente como variable de instancia** (línea ~3787)
   - Cambió de variable local a `private javax.swing.JPanel m_jCustomerPanel`
   - Permite acceso desde el método de atajos de teclado

2. **Inicialización del panel** (línea 2838)
   - Cambió de `javax.swing.JPanel customerPanel = new...` 
   - A `m_jCustomerPanel = new...`

3. **Referencias al panel actualizadas** (líneas 2838-2868)
   - Todas las referencias `customerPanel` cambiadas a `m_jCustomerPanel`
   - Mantiene el panel oculto por defecto con `setVisible(false)`

4. **Método setupKeyboardShortcuts()** (líneas 449-530)
   - Implementa los 4 atajos de teclado (F12, F2, F3, F4)
   - F2 ahora muestra el panel lateral en lugar de abrir diálogo
   - Incluye validaciones y logging detallado

## 🧪 Pruebas

### Cómo probar F2:
1. Abrir el módulo de ventas
2. Presionar F2
3. **Resultado esperado**: 
   - Aparece el panel lateral superior con "ID Cliente"
   - El campo de texto está enfocado y listo para escribir
   - Cualquier texto existente está seleccionado
4. Escribir un ID de cliente y presionar Enter
5. **Resultado esperado**:
   - Se busca el cliente por ID
   - Se muestra el nombre del cliente en el panel
   - Se actualizan los puntos de fidelidad (si aplica)

### Cómo probar otros atajos:
- **F12**: Agregar productos al ticket, presionar F12 → se abre ventana de pago
- **F3**: Abrir varios tickets, presionar F3 → se muestra lista de tickets
- **F4**: Presionar F4 → se crea nuevo ticket vacío

## 📅 Historial de Cambios

### Versión 1.0.0 (Fecha actual)
- ✅ F12: Implementado - Abrir ventana de pago
- ✅ F2: Implementado - Mostrar panel lateral de ID cliente (3ra iteración)
- ✅ F3: Implementado - Mostrar historial de tickets
- ✅ F4: Implementado - Crear nueva pestaña de venta

### Iteraciones de F2:
1. **Iteración 1**: Intentó enfocar campo de texto directamente → No funcionó (campo oculto)
2. **Iteración 2**: Abría diálogo de crear/buscar cliente → No era lo solicitado
3. **Iteración 3**: Muestra panel lateral de ID cliente → ✅ Correcto

## 🔗 Referencias

- Archivo principal: `JPanelTicket.java`
- Líneas de atajos: 449-530
- Variables de instancia: 3780-3800
- Panel de cliente: 2838-2868
- Compilación: `mvn clean install -DskipTests`
- Ejecución: `java -jar target/kriolos-pos.jar`
