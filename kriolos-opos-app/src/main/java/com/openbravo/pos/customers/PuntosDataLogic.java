package com.openbravo.pos.customers;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.PreparedSentence;
import com.openbravo.data.loader.SentenceExec;
import com.openbravo.data.loader.SentenceFind;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.loader.SerializerWriteString;
import com.openbravo.data.loader.SerializerWrite;
import com.openbravo.data.loader.SerializerReadInteger;
import com.openbravo.data.loader.SerializerRead;
import com.openbravo.data.loader.DataRead;
import com.openbravo.data.loader.DataWrite;
import com.openbravo.data.loader.Session;
import com.openbravo.data.loader.StaticSentence;
import com.openbravo.pos.forms.DataLogicSales; // Sebastian - Importar DataLogicSales
import java.util.List;

public class PuntosDataLogic {
    
    protected Session s;
    protected DataLogicSales dlSales; // Sebastian - Referencia a DataLogicSales
    protected SentenceList m_sentconfig;
    protected SentenceFind m_sentconfigfind;
    protected SentenceExec m_sentconfigsave;
    protected SentenceExec m_sentconfigupdate;
    protected SentenceExec m_sentconfigdelete;
    
    protected SentenceList m_sentpuntos;
    protected SentenceFind m_sentpuntosfind;
    protected SentenceExec m_sentpuntossave;
    protected SentenceExec m_sentpuntosupdate;
    protected SentenceExec m_sentpuntosdelete;
    
    // Constructor original
    public PuntosDataLogic(Session s) {
        this.s = s;
        this.dlSales = null;
        initSentences();
    }
    
    // Constructor que toma DataLogicSales
    public PuntosDataLogic(DataLogicSales dlSales) {
        this.dlSales = dlSales;
        // Accedemos a la sesión usando reflexión para acceder al campo protegido 's'
        try {
            java.lang.reflect.Field sessionField = dlSales.getClass().getDeclaredField("s");
            sessionField.setAccessible(true);
            this.s = (Session) sessionField.get(dlSales);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo acceder a la sesión de DataLogicSales", e);
        }
        initSentences();
    }
    
    private void initSentences() {
        
        // Sentencias para configuración de puntos
        m_sentconfig = new StaticSentence(s,
            "SELECT ID, MONTO_POR_PUNTO, PUNTOS_OTORGADOS, SISTEMA_ACTIVO, MONEDA, LIMITE_DIARIO_PUNTOS, FECHA_CREACION, FECHA_ACTUALIZACION " +
            "FROM PUNTOS_CONFIGURACION ORDER BY FECHA_ACTUALIZACION DESC",
            null,
            PuntosConfiguracion.getSerializerRead());
            
        m_sentconfigfind = new PreparedSentence(s,
            "SELECT ID, MONTO_POR_PUNTO, PUNTOS_OTORGADOS, SISTEMA_ACTIVO, MONEDA, LIMITE_DIARIO_PUNTOS, FECHA_CREACION, FECHA_ACTUALIZACION " +
            "FROM PUNTOS_CONFIGURACION WHERE ID = ?",
            SerializerWriteString.INSTANCE,
            PuntosConfiguracion.getSerializerRead());
            
        m_sentconfigsave = new StaticSentence(s,
            "INSERT INTO PUNTOS_CONFIGURACION (ID, MONTO_POR_PUNTO, PUNTOS_OTORGADOS, SISTEMA_ACTIVO, MONEDA, LIMITE_DIARIO_PUNTOS, FECHA_CREACION, FECHA_ACTUALIZACION) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            PuntosConfiguracion.getSerializerWrite());
            
        m_sentconfigupdate = new StaticSentence(s,
            "UPDATE PUNTOS_CONFIGURACION SET MONTO_POR_PUNTO = ?, PUNTOS_OTORGADOS = ?, SISTEMA_ACTIVO = ?, MONEDA = ?, LIMITE_DIARIO_PUNTOS = ?, FECHA_ACTUALIZACION = ? " +
            "WHERE ID = ?",
            new SerializerWrite() {
                public void writeValues(DataWrite dp, Object obj) throws BasicException {
                    PuntosConfiguracion config = (PuntosConfiguracion) obj;
                    dp.setDouble(1, config.getMontoPorPunto());
                    dp.setInt(2, config.getPuntosOtorgados());
                    dp.setBoolean(3, config.isSistemaActivo());
                    dp.setString(4, config.getMoneda());
                    dp.setInt(5, config.getLimiteDiarioPuntos()); // Sebastian - Nuevo campo
                    dp.setTimestamp(6, config.getFechaActualizacion());
                    dp.setString(7, config.getId());
                }
            });
            
        m_sentconfigdelete = new StaticSentence(s,
            "DELETE FROM PUNTOS_CONFIGURACION WHERE ID = ?",
            SerializerWriteString.INSTANCE);
        
        // Sentencias para puntos de clientes
        m_sentpuntos = new StaticSentence(s,
            "SELECT ID, CLIENTE_ID, PUNTOS_ACTUALES, PUNTOS_TOTALES, ULTIMA_TRANSACCION, FECHA_ULTIMA_TRANSACCION, FECHA_CREACION " +
            "FROM CLIENTE_PUNTOS ORDER BY FECHA_ULTIMA_TRANSACCION DESC",
            null,
            ClientePuntos.getSerializerRead());
            
        m_sentpuntosfind = new PreparedSentence(s,
            "SELECT ID, CLIENTE_ID, PUNTOS_ACTUALES, PUNTOS_TOTALES, ULTIMA_TRANSACCION, FECHA_ULTIMA_TRANSACCION, FECHA_CREACION " +
            "FROM CLIENTE_PUNTOS WHERE CLIENTE_ID = ?",
            SerializerWriteString.INSTANCE,
            ClientePuntos.getSerializerRead());
            
        m_sentpuntossave = new StaticSentence(s,
            "INSERT INTO CLIENTE_PUNTOS (ID, CLIENTE_ID, PUNTOS_ACTUALES, PUNTOS_TOTALES, ULTIMA_TRANSACCION, FECHA_ULTIMA_TRANSACCION, FECHA_CREACION) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            ClientePuntos.getSerializerWrite());
            
        m_sentpuntosupdate = new StaticSentence(s,
            "UPDATE CLIENTE_PUNTOS SET PUNTOS_ACTUALES = ?, PUNTOS_TOTALES = ?, ULTIMA_TRANSACCION = ?, FECHA_ULTIMA_TRANSACCION = ? " +
            "WHERE CLIENTE_ID = ?",
            new SerializerWrite() {
                public void writeValues(DataWrite dp, Object obj) throws BasicException {
                    ClientePuntos puntos = (ClientePuntos) obj;
                    dp.setInt(1, puntos.getPuntosActuales());
                    dp.setInt(2, puntos.getPuntosTotales());
                    dp.setString(3, puntos.getUltimaTransaccion());
                    dp.setTimestamp(4, puntos.getFechaUltimaTransaccion());
                    dp.setString(5, puntos.getClienteId());
                }
            });
            
        m_sentpuntosdelete = new StaticSentence(s,
            "DELETE FROM CLIENTE_PUNTOS WHERE CLIENTE_ID = ?",
            SerializerWriteString.INSTANCE);
    }
    
    // Métodos para configuración
    public List getConfiguraciones() throws BasicException {
        return m_sentconfig.list();
    }
    
    public PuntosConfiguracion getConfiguracion(String id) throws BasicException {
        return (PuntosConfiguracion) m_sentconfigfind.find(id);
    }
    
    public PuntosConfiguracion getConfiguracionActiva() throws BasicException {
        try {
            List configs = m_sentconfig.list();
            if (configs != null && !configs.isEmpty()) {
                for (Object obj : configs) {
                    PuntosConfiguracion config = (PuntosConfiguracion) obj;
                    if (config.isSistemaActivo()) {
                        return config;
                    }
                }
                // Si no hay configuración activa, retorna la primera
                return (PuntosConfiguracion) configs.get(0);
            }
        } catch (BasicException e) {
            // Si la tabla no existe o hay error, crear tabla y configuración
            System.err.println("⚠️ Error accediendo configuración, recreando tablas: " + e.getMessage());
            initTables();
        }
        
        // Si no hay configuración, crea una por defecto
        PuntosConfiguracion configDefault = new PuntosConfiguracion();
        configDefault.setMontoPorPunto(400.0); // $400 MX
        configDefault.setPuntosOtorgados(10);   // 10 puntos
        configDefault.setMoneda("MX");
        configDefault.setSistemaActivo(true);
        
        try {
            saveConfiguracion(configDefault);
        } catch (BasicException e) {
            System.err.println("⚠️ Error guardando configuración por defecto: " + e.getMessage());
        }
        
        return configDefault;
    }
    
    public void saveConfiguracion(PuntosConfiguracion config) throws BasicException {
        m_sentconfigsave.exec(config);
    }
    
    public void updateConfiguracion(PuntosConfiguracion config) throws BasicException {
        m_sentconfigupdate.exec(config);
    }
    
    public void deleteConfiguracion(String id) throws BasicException {
        m_sentconfigdelete.exec(id);
    }
    
    // Métodos para puntos de clientes
    public List getClientesPuntos() throws BasicException {
        return m_sentpuntos.list();
    }
    
    public ClientePuntos getClientePuntos(String clienteId) throws BasicException {
        return (ClientePuntos) m_sentpuntosfind.find(clienteId);
    }
    
    public ClientePuntos getOrCreateClientePuntos(String clienteId) throws BasicException {
        ClientePuntos puntos = getClientePuntos(clienteId);
        if (puntos == null) {
            puntos = new ClientePuntos(clienteId);
            saveClientePuntos(puntos);
        }
        return puntos;
    }
    
    public void saveClientePuntos(ClientePuntos puntos) throws BasicException {
        m_sentpuntossave.exec(puntos);
    }
    
    public void updateClientePuntos(ClientePuntos puntos) throws BasicException {
        m_sentpuntosupdate.exec(puntos);
    }
    
    public void deleteClientePuntos(String clienteId) throws BasicException {
        m_sentpuntosdelete.exec(clienteId);
    }
    
    /**
     * Agrega puntos a un cliente por una compra usando acumulable diario
     * Sebastian - Modificado para acumular montos del día y otorgar puntos cuando se alcance el umbral
     */
    public void agregarPuntosPorCompra(String clienteId, double montoCompra, String descripcion) throws BasicException {
        System.out.println("🚀 agregarPuntosPorCompra INICIADO - Cliente: " + clienteId + ", Monto: $" + montoCompra);
        
        PuntosConfiguracion config = getConfiguracionActiva();
        if (config == null || !config.isSistemaActivo()) {
            System.out.println("⚠️ Sistema de puntos desactivado o sin configuración");
            return;
        }
        
        System.out.println("✅ Configuración activa - Monto por punto: $" + config.getMontoPorPunto() + 
                         ", Puntos otorgados: " + config.getPuntosOtorgados());
        
        // Obtener o inicializar el acumulable del día
        double acumulableActual = obtenerAcumulableDiario(clienteId);
        
        System.out.println("🔍 DEBUG INICIAL - Cliente: " + clienteId + 
                         ", Acumulable actual: $" + acumulableActual + 
                         ", Monto compra: $" + montoCompra);
        
        // Sumar el monto de la compra actual al acumulable
        double nuevoAcumulable = acumulableActual + montoCompra;
        
        System.out.println("💰 ACUMULABLE: Cliente " + clienteId + 
                         " - Anterior: $" + acumulableActual + 
                         " + Compra: $" + montoCompra + 
                         " = Nuevo: $" + nuevoAcumulable);
        
        // Calcular cuántos puntos se deben otorgar con el nuevo acumulable
        int puntosAOtorgar = config.calcularPuntos(nuevoAcumulable);
        
        // Calcular cuántos puntos ya se habían otorgado con el acumulable anterior
        int puntosYaOtorgados = config.calcularPuntos(acumulableActual);
        
        // Los puntos nuevos a otorgar son la diferencia
        int puntosNuevos = puntosAOtorgar - puntosYaOtorgados;
        
        System.out.println("🎯 PUNTOS: Acumulable anterior otorgaba " + puntosYaOtorgados + 
                         " puntos, nuevo acumulable otorga " + puntosAOtorgar + 
                         " puntos → Puntos nuevos: " + puntosNuevos);
        
        // SIEMPRE actualizar el acumulable, incluso si no hay puntos nuevos
        // Calcular el acumulable restante después de otorgar puntos
        double montoPorPunto = config.getMontoPorPunto();
        int tramosCompletos = (int) Math.floor(nuevoAcumulable / montoPorPunto);
        double montoUsado = tramosCompletos * montoPorPunto;
        double nuevoAcumulableRestante = nuevoAcumulable - montoUsado;
        
        System.out.println("📊 CÁLCULO ACUMULABLE RESTANTE: " + 
                         "Acumulable total: $" + nuevoAcumulable + 
                         " - Tramos completos: " + tramosCompletos + 
                         " × $" + montoPorPunto + 
                         " = Monto usado: $" + montoUsado + 
                         " → Nuevo acumulable restante: $" + nuevoAcumulableRestante);
        
        if (puntosNuevos > 0) {
            // Verificar límite diario antes de otorgar puntos
            int puntosGanadosHoy = getPuntosGanadosHoy(clienteId);
            int limiteDiario = config.getLimiteDiarioPuntos();
            
            System.out.println("🔍 VERIFICANDO LÍMITE: Cliente " + clienteId + 
                             " - Puntos hoy: " + puntosGanadosHoy + 
                             " - Límite diario: " + limiteDiario + 
                             " - Puntos nuevos a otorgar: " + puntosNuevos);
            
            if (puntosGanadosHoy >= limiteDiario) {
                System.out.println("🚫 LÍMITE DIARIO ALCANZADO: Cliente " + clienteId + 
                                 " ya ganó " + puntosGanadosHoy + " puntos hoy (límite: " + limiteDiario + ")");
                // Aún así actualizamos el acumulable aunque no otorguemos puntos
                actualizarAcumulableDiario(clienteId, nuevoAcumulableRestante);
                return;
            }
            
            // Si otorgar los puntos excedería el límite, ajustar la cantidad
            int puntosDisponibles = limiteDiario - puntosGanadosHoy;
            if (puntosNuevos > puntosDisponibles) {
                puntosNuevos = puntosDisponibles;
                System.out.println("⚠️ PUNTOS AJUSTADOS: Otorgando " + puntosNuevos + 
                                 " puntos para no exceder límite diario");
            }
            
            if (puntosNuevos > 0) {
                // Actualizar puntos totales del cliente
                ClientePuntos puntos = getOrCreateClientePuntos(clienteId);
                puntos.agregarPuntos(puntosNuevos, descripcion);
                updateClientePuntos(puntos);
                
                // Registrar en historial para límites diarios
                registrarHistorialPuntos(clienteId, puntosNuevos, descripcion, montoCompra);
                
                System.out.println("✅ PUNTOS OTORGADOS: " + puntosNuevos + 
                                 " puntos (total hoy: " + (puntosGanadosHoy + puntosNuevos) + 
                                 "/" + limiteDiario + ")");
            }
        }
        
        // Actualizar el acumulable diario (SIEMPRE, incluso si no se otorgaron puntos)
        try {
            actualizarAcumulableDiario(clienteId, nuevoAcumulableRestante);
            System.out.println("✅ Acumulable actualizado exitosamente: $" + nuevoAcumulableRestante);
        } catch (Exception e) {
            System.err.println("❌ ERROR actualizando acumulable: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Agrega puntos a un cliente por un producto específico
     */
    public void agregarPuntosPorProducto(String clienteId, String producto, int puntosEspecificos) throws BasicException {
        ClientePuntos puntos = getOrCreateClientePuntos(clienteId);
        puntos.agregarPuntos(puntosEspecificos, "Producto: " + producto);
        updateClientePuntos(puntos);
    }
    
    /**
     * Sebastian - Calcula cuántos puntos ha ganado un cliente en el día actual
     */
    public int getPuntosGanadosHoy(String clienteId) throws BasicException {
        try {
            // Consulta directa a la tabla de historial de puntos para el día actual
            String query = "SELECT COALESCE(SUM(PUNTOS_OTORGADOS), 0) " +
                          "FROM PUNTOS_HISTORIAL " +
                          "WHERE CLIENTE_ID = ? " +
                          "AND CAST(FECHA_TRANSACCION AS DATE) = CURRENT_DATE";
            
            PreparedSentence sentencia = new PreparedSentence(s, query, SerializerWriteString.INSTANCE, SerializerReadInteger.INSTANCE);
            Integer resultado = (Integer) sentencia.find(clienteId);
            
            int puntosHoy = resultado != null ? resultado : 0;
            System.out.println("📊 Cliente " + clienteId + " ha ganado " + puntosHoy + " puntos hoy");
            
            return puntosHoy;
            
        } catch (Exception e) {
            System.err.println("⚠️ Error calculando puntos del día para cliente " + clienteId + ": " + e.getMessage());
            // Fallback: retornar 0 para permitir continuar
            return 0;
        }
    }
    
    /**
     * Inicializa las tablas de puntos si no existen
     */
    public void initTables() throws BasicException {
        // Sebastian - Primero verificamos si las tablas ya existen para evitar logs SEVERE
        if (tablasYaExisten()) {
            System.out.println("ℹ️ Sistema de puntos: Tablas ya existen");
            verificarMigraciones(); // Verificar si necesitamos agregar columnas nuevas
            return;
        }
        
        try {
            // Crear tabla de configuración si no existe
            String createConfigTable = 
                "CREATE TABLE PUNTOS_CONFIGURACION (" +
                "ID VARCHAR(36) NOT NULL PRIMARY KEY, " +
                "MONTO_POR_PUNTO DECIMAL(10,2) NOT NULL, " +
                "PUNTOS_OTORGADOS INTEGER NOT NULL, " +
                "SISTEMA_ACTIVO BOOLEAN NOT NULL, " +
                "MONEDA VARCHAR(10) NOT NULL, " +
                "LIMITE_DIARIO_PUNTOS INTEGER DEFAULT 500, " + // Sebastian - Nuevo campo
                "FECHA_CREACION TIMESTAMP NOT NULL, " +
                "FECHA_ACTUALIZACION TIMESTAMP NOT NULL)";
            
            try {
                new StaticSentence(s, createConfigTable).exec();
                System.out.println("✅ Tabla PUNTOS_CONFIGURACION creada exitosamente");
            } catch (BasicException e) {
                if (e.getMessage().contains("already exists") || e.getMessage().contains("ya existe") || 
                    e.getMessage().contains("nombre del objeto ya existe")) {
                    System.out.println("ℹ️ Tabla PUNTOS_CONFIGURACION ya existe");
                } else {
                    System.err.println("⚠️ Error creando tabla PUNTOS_CONFIGURACION: " + e.getMessage());
                }
            }
            
            // Crear tabla de puntos de clientes si no existe (sintaxis HSQLDB sin DEFAULT)
            String createPuntosTable = 
                "CREATE TABLE CLIENTE_PUNTOS (" +
                "ID VARCHAR(36) NOT NULL PRIMARY KEY, " +
                "CLIENTE_ID VARCHAR(36) NOT NULL UNIQUE, " +
                "PUNTOS_ACTUALES INTEGER NOT NULL, " +
                "PUNTOS_TOTALES INTEGER NOT NULL, " +
                "ULTIMA_TRANSACCION VARCHAR(255), " +
                "FECHA_ULTIMA_TRANSACCION TIMESTAMP, " +
                "FECHA_CREACION TIMESTAMP NOT NULL)";
            
            try {
                new StaticSentence(s, createPuntosTable).exec();
                System.out.println("✅ Tabla CLIENTE_PUNTOS creada exitosamente");
            } catch (BasicException e) {
                if (e.getMessage().contains("already exists") || e.getMessage().contains("ya existe") || 
                    e.getMessage().contains("nombre del objeto ya existe")) {
                    System.out.println("ℹ️ Tabla CLIENTE_PUNTOS ya existe");
                } else {
                    System.err.println("⚠️ Error creando tabla CLIENTE_PUNTOS: " + e.getMessage());
                }
            }
            
            // Sebastian - Crear tabla de historial de puntos para límites diarios
            String createHistorialTable = 
                "CREATE TABLE PUNTOS_HISTORIAL (" +
                "ID VARCHAR(36) NOT NULL PRIMARY KEY, " +
                "CLIENTE_ID VARCHAR(36) NOT NULL, " +
                "PUNTOS_OTORGADOS INTEGER NOT NULL, " +
                "DESCRIPCION VARCHAR(255), " +
                "MONTO_COMPRA DECIMAL(10,2), " +
                "FECHA_TRANSACCION TIMESTAMP NOT NULL)";
            
            try {
                new StaticSentence(s, createHistorialTable).exec();
                System.out.println("✅ Tabla PUNTOS_HISTORIAL creada exitosamente");
            } catch (BasicException e) {
                if (e.getMessage().contains("already exists") || e.getMessage().contains("ya existe") || 
                    e.getMessage().contains("nombre del objeto ya existe")) {
                    System.out.println("ℹ️ Tabla PUNTOS_HISTORIAL ya existe");
                } else {
                    System.err.println("⚠️ Error creando tabla PUNTOS_HISTORIAL: " + e.getMessage());
                }
            }
            
            // Sebastian - Crear tabla de acumulable diario para puntos acumulables
            String createAcumulableTable = 
                "CREATE TABLE PUNTOS_ACUMULABLE_DIARIO (" +
                "CLIENTE_ID VARCHAR(36) NOT NULL PRIMARY KEY, " +
                "MONTO_ACUMULADO DECIMAL(10,2) NOT NULL, " +
                "FECHA_ACTUALIZACION TIMESTAMP NOT NULL)";
            
            try {
                new StaticSentence(s, createAcumulableTable).exec();
                System.out.println("✅ Tabla PUNTOS_ACUMULABLE_DIARIO creada exitosamente");
            } catch (BasicException e) {
                if (e.getMessage().contains("already exists") || e.getMessage().contains("ya existe") || 
                    e.getMessage().contains("nombre del objeto ya existe")) {
                    System.out.println("ℹ️ Tabla PUNTOS_ACUMULABLE_DIARIO ya existe");
                } else {
                    System.err.println("⚠️ Error creando tabla PUNTOS_ACUMULABLE_DIARIO: " + e.getMessage());
                }
            }
            
            // Verificar si existe configuración por defecto, si no, crearla
            try {
                PuntosConfiguracion configExistente = getConfiguracionActiva();
                if (configExistente == null) {
                    PuntosConfiguracion configDefault = new PuntosConfiguracion();
                    configDefault.setMontoPorPunto(400.0); // $400 MX
                    configDefault.setPuntosOtorgados(10);   // 10 puntos
                    configDefault.setMoneda("MX");
                    configDefault.setSistemaActivo(true);
                    saveConfiguracion(configDefault);
                    System.out.println("✅ Configuración por defecto creada: $400.00 MX = 10 puntos");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Error inicializando configuración por defecto: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error general inicializando tablas de puntos: " + e.getMessage());
            throw new BasicException("Error inicializando sistema de puntos: " + e.getMessage());
        }
    }
    
    /**
     * Verifica y asegura que el sistema de puntos esté completamente operativo
     */
    public void verificarSistemaPuntos() {
        System.out.println("🔧 Verificando sistema de puntos...");
        
        try {
            // Verificar y crear tablas si es necesario
            initTables();
            
            // Verificar configuración
            PuntosConfiguracion config = getConfiguracionActiva();
            System.out.println("✅ Sistema de puntos operativo:");
            System.out.println("   - Monto por punto: $" + config.getMontoPorPunto() + " " + config.getMoneda());
            System.out.println("   - Puntos otorgados: " + config.getPuntosOtorgados());
            System.out.println("   - Sistema activo: " + (config.isSistemaActivo() ? "SÍ" : "NO"));
            
        } catch (Exception e) {
            System.err.println("⚠️ Error verificando sistema de puntos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtiene los puntos actuales de un cliente específico
     */
    public int obtenerPuntos(String clienteId) throws BasicException {
        ClientePuntos puntos = getClientePuntos(clienteId);
        return puntos != null ? puntos.getPuntosActuales() : 0;
    }
    
    /**
     * Actualiza los puntos de un cliente específico
     */
    public void actualizarPuntos(String clienteId, int nuevosPuntos) throws BasicException {
        ClientePuntos puntos = getOrCreateClientePuntos(clienteId);
        int diferencia = nuevosPuntos - puntos.getPuntosActuales();
        
        // Actualizar puntos actuales
        puntos.setPuntosActuales(nuevosPuntos);
        
        // Si se agregaron puntos, sumar a totales
        if (diferencia > 0) {
            puntos.setPuntosTotales(puntos.getPuntosTotales() + diferencia);
            puntos.setUltimaTransaccion("Ajuste manual: +" + diferencia + " puntos");
        } else if (diferencia < 0) {
            puntos.setUltimaTransaccion("Ajuste manual: " + diferencia + " puntos");
        }
        
        // Actualizar fecha de última transacción
        puntos.setFechaUltimaTransaccion(new java.sql.Timestamp(System.currentTimeMillis()));
        
        updateClientePuntos(puntos);
    }
    
    /**
     * Agrega puntos a un cliente específico (incremento)
     */
    public void agregarPuntos(String clienteId, int puntosAAgregar) throws BasicException {
        if (puntosAAgregar <= 0) {
            throw new BasicException("La cantidad de puntos a agregar debe ser positiva");
        }
        
        ClientePuntos puntos = getOrCreateClientePuntos(clienteId);
        puntos.agregarPuntos(puntosAAgregar, "Ajuste manual: +" + puntosAAgregar + " puntos");
        updateClientePuntos(puntos);
    }
    
    /**
     * Quita puntos a un cliente específico (decremento)
     */
    public void quitarPuntos(String clienteId, int puntosAQuitar) throws BasicException {
        if (puntosAQuitar <= 0) {
            throw new BasicException("La cantidad de puntos a quitar debe ser positiva");
        }
        
        ClientePuntos puntos = getOrCreateClientePuntos(clienteId);
        int puntosActuales = puntos.getPuntosActuales();
        
        if (puntosAQuitar > puntosActuales) {
            throw new BasicException("No se pueden quitar " + puntosAQuitar + " puntos. El cliente solo tiene " + puntosActuales + " puntos disponibles.");
        }
        
        puntos.setPuntosActuales(puntosActuales - puntosAQuitar);
        puntos.setUltimaTransaccion("Ajuste manual: -" + puntosAQuitar + " puntos");
        puntos.setFechaUltimaTransaccion(new java.sql.Timestamp(System.currentTimeMillis()));
        
        updateClientePuntos(puntos);
    }
    
    /**
     * Sebastian - Descuenta puntos cuando se cancela una venta
     * Busca en el historial los puntos otorgados para el ticket y los descuenta
     * @param ticketId ID del ticket cancelado
     * @param clienteId ID del cliente
     * @param montoAcumulableTicket Monto acumulable del ticket (para actualizar acumulable diario incluso si no hay puntos)
     */
    public void descontarPuntosPorCancelacion(String ticketId, String clienteId, double montoAcumulableTicket) throws BasicException {
        System.out.println("🔄 descontarPuntosPorCancelacion INICIADO - Ticket: " + ticketId + ", Cliente: " + clienteId);
        
        try {
            // Buscar en el historial las transacciones relacionadas con este ticket
            // La descripción contiene "Venta automática #" + ticketId
            String descripcionBusqueda = "Venta automática #" + ticketId;
            
            String query = "SELECT ID, CLIENTE_ID, PUNTOS_OTORGADOS, DESCRIPCION, MONTO_COMPRA " +
                          "FROM PUNTOS_HISTORIAL " +
                          "WHERE CLIENTE_ID = ? AND DESCRIPCION LIKE ?";
            
            PreparedSentence sentencia = new PreparedSentence(s, query,
                new SerializerWrite<Object[]>() {
                    public void writeValues(DataWrite dp, Object[] obj) throws BasicException {
                        dp.setString(1, (String) obj[0]); // CLIENTE_ID
                        dp.setString(2, (String) obj[1]); // DESCRIPCION LIKE
                    }
                },
                new SerializerRead() {
                    @Override
                    public Object readValues(DataRead dr) throws BasicException {
                        return new Object[] {
                            dr.getString(1),  // ID
                            dr.getString(2),  // CLIENTE_ID
                            dr.getInt(3),    // PUNTOS_OTORGADOS
                            dr.getString(4),  // DESCRIPCION
                            dr.getDouble(5)   // MONTO_COMPRA
                        };
                    }
                });
            
            String pattern = "%" + descripcionBusqueda + "%";
            List<Object[]> resultados = sentencia.list(new Object[]{clienteId, pattern});
            
            if (resultados == null || resultados.isEmpty()) {
                System.out.println("ℹ️ No se encontraron puntos otorgados para el ticket #" + ticketId);
                return;
            }
            
            int totalPuntosADescontar = 0;
            double totalMontoADescontar = 0.0;
            
            // Sumar todos los puntos y montos de las transacciones encontradas
            for (Object[] resultado : resultados) {
                int puntosOtorgados = (Integer) resultado[2];
                double montoCompra = (Double) resultado[4];
                totalPuntosADescontar += puntosOtorgados;
                totalMontoADescontar += montoCompra;
                
                System.out.println("📋 Transacción encontrada: " + puntosOtorgados + " puntos, $" + montoCompra);
            }
            
            System.out.println("💰 TOTAL A DESCONTAR: " + totalPuntosADescontar + " puntos, $" + totalMontoADescontar);
            
            if (totalPuntosADescontar > 0) {
                // Descontar puntos del cliente
                ClientePuntos puntos = getOrCreateClientePuntos(clienteId);
                int puntosActuales = puntos.getPuntosActuales();
                
                // Si el cliente no tiene suficientes puntos, solo descontar los que tiene
                int puntosADescontar = Math.min(totalPuntosADescontar, puntosActuales);
                
                if (puntosADescontar > 0) {
                    puntos.setPuntosActuales(puntosActuales - puntosADescontar);
                    puntos.setUltimaTransaccion("Cancelación venta #" + ticketId + ": -" + puntosADescontar + " puntos");
                    puntos.setFechaUltimaTransaccion(new java.sql.Timestamp(System.currentTimeMillis()));
                    updateClientePuntos(puntos);
                    
                    System.out.println("✅ Puntos descontados: " + puntosADescontar + " (tenía " + puntosActuales + ", ahora tiene " + puntos.getPuntosActuales() + ")");
                } else {
                    System.out.println("⚠️ Cliente no tiene puntos para descontar (tiene " + puntosActuales + ", se intentaron descontar " + totalPuntosADescontar + ")");
                }
            }
            
            // Actualizar el acumulable diario (restar el monto de la venta cancelada)
            // Usar el monto del historial si está disponible, sino usar el monto del ticket
            double montoADescontarAcumulable = totalMontoADescontar > 0 ? totalMontoADescontar : montoAcumulableTicket;
            
            if (montoADescontarAcumulable > 0) {
                double acumulableActual = obtenerAcumulableDiario(clienteId);
                double nuevoAcumulable = Math.max(0.0, acumulableActual - montoADescontarAcumulable);
                
                String fuente = totalMontoADescontar > 0 ? "historial" : "ticket (sin puntos en historial)";
                System.out.println("🔄 Actualizando acumulable (" + fuente + "): $" + acumulableActual + " - $" + montoADescontarAcumulable + " = $" + nuevoAcumulable);
                actualizarAcumulableDiario(clienteId, nuevoAcumulable);
            }
            
            // Eliminar las transacciones del historial relacionadas con este ticket
            String deleteQuery = "DELETE FROM PUNTOS_HISTORIAL WHERE CLIENTE_ID = ? AND DESCRIPCION LIKE ?";
            PreparedSentence deleteSentencia = new PreparedSentence(s, deleteQuery,
                new SerializerWrite<Object[]>() {
                    public void writeValues(DataWrite dp, Object[] obj) throws BasicException {
                        dp.setString(1, (String) obj[0]); // CLIENTE_ID
                        dp.setString(2, (String) obj[1]); // DESCRIPCION LIKE
                    }
                });
            
            deleteSentencia.exec(new Object[]{clienteId, pattern});
            System.out.println("🗑️ Transacciones del historial eliminadas para ticket #" + ticketId);
            
        } catch (Exception e) {
            System.err.println("❌ ERROR en descontarPuntosPorCancelacion: " + e.getMessage());
            e.printStackTrace();
            // No lanzar excepción para no interrumpir la cancelación del ticket
        }
    }
    
    /**
     * Sebastian - Registra una transacción de puntos en el historial para límites diarios
     */
    private void registrarHistorialPuntos(String clienteId, int puntosOtorgados, String descripcion, double montoCompra) throws BasicException {
        try {
            String insertHistorial = "INSERT INTO PUNTOS_HISTORIAL (ID, CLIENTE_ID, PUNTOS_OTORGADOS, DESCRIPCION, MONTO_COMPRA, FECHA_TRANSACCION) " +
                                   "VALUES (?, ?, ?, ?, ?, ?)";
            
            String id = java.util.UUID.randomUUID().toString();
            java.sql.Timestamp ahora = new java.sql.Timestamp(System.currentTimeMillis());
            
            PreparedSentence sentencia = new PreparedSentence(s, insertHistorial, 
                new SerializerWrite<Object[]>() {
                    public void writeValues(DataWrite dp, Object[] obj) throws BasicException {
                        dp.setString(1, (String) obj[0]); // ID
                        dp.setString(2, (String) obj[1]); // CLIENTE_ID
                        dp.setInt(3, (Integer) obj[2]);   // PUNTOS_OTORGADOS
                        dp.setString(4, (String) obj[3]); // DESCRIPCION
                        dp.setDouble(5, (Double) obj[4]); // MONTO_COMPRA
                        dp.setTimestamp(6, (java.sql.Timestamp) obj[5]); // FECHA_TRANSACCION
                    }
                });
            
            Object[] params = {id, clienteId, puntosOtorgados, descripcion, montoCompra, ahora};
            sentencia.exec(params);
            
            System.out.println("📝 HISTORIAL REGISTRADO: " + puntosOtorgados + " puntos para cliente " + clienteId);
            
        } catch (Exception e) {
            System.err.println("⚠️ Error registrando historial de puntos: " + e.getMessage());
            // No lanzar excepción para no interrumpir la venta
        }
    }

    /**
     * Fuerza la recreación completa de las tablas de puntos
     */
    public void forzarCreacionTablas() throws BasicException {
        System.out.println("🔧 Forzando recreación de tablas de puntos...");
        
        try {
            // Eliminar tablas si existen (en orden correcto debido a dependencias)
            try {
                new StaticSentence(s, "DROP TABLE CLIENTE_PUNTOS").exec();
                System.out.println("ℹ️ Tabla CLIENTE_PUNTOS eliminada");
            } catch (Exception e) {
                System.out.println("ℹ️ Tabla CLIENTE_PUNTOS no existía o no se pudo eliminar");
            }
            
            try {
                new StaticSentence(s, "DROP TABLE PUNTOS_HISTORIAL").exec();
                System.out.println("ℹ️ Tabla PUNTOS_HISTORIAL eliminada");
            } catch (Exception e) {
                System.out.println("ℹ️ Tabla PUNTOS_HISTORIAL no existía o no se pudo eliminar");
            }
            
            try {
                new StaticSentence(s, "DROP TABLE PUNTOS_CONFIGURACION").exec();
                System.out.println("ℹ️ Tabla PUNTOS_CONFIGURACION eliminada");
            } catch (Exception e) {
                System.out.println("ℹ️ Tabla PUNTOS_CONFIGURACION no existía o no se pudo eliminar");
            }
        } catch (Exception e) {
            System.out.println("ℹ️ Algunas tablas no se pudieron eliminar: " + e.getMessage());
        }
        
        // Crear tablas nuevamente
        initTables();
        System.out.println("✅ Tablas recreadas exitosamente");
    }
    
    /**
     * Sebastian - Verifica si las tablas del sistema de puntos ya existen
     */
    private boolean tablasYaExisten() {
        try {
            // Intentamos hacer una consulta simple a cada tabla
            new StaticSentence(s, "SELECT COUNT(*) FROM PUNTOS_CONFIGURACION").exec();
            new StaticSentence(s, "SELECT COUNT(*) FROM CLIENTE_PUNTOS").exec();
            new StaticSentence(s, "SELECT COUNT(*) FROM PUNTOS_HISTORIAL").exec();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Sebastian - Obtiene el monto acumulado del día para un cliente
     * Si es un nuevo día, resetea el acumulable a 0
     */
    public double obtenerAcumulableDiario(String clienteId) throws BasicException {
        try {
            // Verificar si existe registro para el cliente y si es del día actual
            String query = "SELECT MONTO_ACUMULADO, FECHA_ACTUALIZACION " +
                          "FROM PUNTOS_ACUMULABLE_DIARIO " +
                          "WHERE CLIENTE_ID = ?";
            
            PreparedSentence sentencia = new PreparedSentence(s, query, 
                SerializerWriteString.INSTANCE,
                new SerializerRead() {
                    @Override
                    public Object readValues(DataRead dr) throws BasicException {
                        // Leer la fecha como Object porque HSQLDB puede devolver Date o Timestamp
                        Object fechaObj = dr.getObject(2);
                        return new Object[] {
                            dr.getDouble(1),  // MONTO_ACUMULADO
                            fechaObj          // FECHA_ACTUALIZACION (Date, Timestamp, o null)
                        };
                    }
                });
            
            Object[] resultado = (Object[]) sentencia.find(clienteId);
            
            if (resultado != null) {
                double montoAcumulado = (Double) resultado[0];
                Object fechaObj = resultado[1];
                
                // HSQLDB puede devolver Date o Timestamp, convertir a Timestamp
                java.sql.Timestamp fechaActualizacion = null;
                if (fechaObj != null) {
                    if (fechaObj instanceof java.sql.Timestamp) {
                        fechaActualizacion = (java.sql.Timestamp) fechaObj;
                    } else if (fechaObj instanceof java.util.Date) {
                        fechaActualizacion = new java.sql.Timestamp(((java.util.Date) fechaObj).getTime());
                    } else if (fechaObj instanceof java.sql.Date) {
                        fechaActualizacion = new java.sql.Timestamp(((java.sql.Date) fechaObj).getTime());
                    }
                }
                
                // Comparar solo la fecha (sin hora)
                java.util.Calendar calActualizacion = java.util.Calendar.getInstance();
                if (fechaActualizacion != null) {
                    calActualizacion.setTimeInMillis(fechaActualizacion.getTime());
                }
                java.util.Calendar calHoy = java.util.Calendar.getInstance();
                calHoy.setTimeInMillis(System.currentTimeMillis());
                
                boolean mismoDia = fechaActualizacion != null &&
                    calActualizacion.get(java.util.Calendar.YEAR) == calHoy.get(java.util.Calendar.YEAR) &&
                    calActualizacion.get(java.util.Calendar.DAY_OF_YEAR) == calHoy.get(java.util.Calendar.DAY_OF_YEAR);
                
                // Si la fecha es diferente a hoy, resetear el acumulable
                if (!mismoDia) {
                    System.out.println("🔄 Nuevo día detectado para cliente " + clienteId + 
                                     ", reseteando acumulable");
                    actualizarAcumulableDiario(clienteId, 0.0);
                    return 0.0;
                }
                
                System.out.println("✅ Acumulable recuperado: $" + montoAcumulado + 
                                 " (fecha: " + fechaActualizacion + ")");
                return montoAcumulado;
            } else {
                // No existe registro, crear uno con acumulable 0
                actualizarAcumulableDiario(clienteId, 0.0);
                return 0.0;
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Error obteniendo acumulable diario para cliente " + clienteId + ": " + e.getMessage());
            // Si hay error (tabla no existe), retornar 0 y la tabla se creará en la próxima actualización
            return 0.0;
        }
    }
    
    /**
     * Sebastian - Actualiza el monto acumulado del día para un cliente
     */
    public void actualizarAcumulableDiario(String clienteId, double nuevoMonto) throws BasicException {
        try {
            // Verificar si existe registro
            String checkQuery = "SELECT COUNT(*) FROM PUNTOS_ACUMULABLE_DIARIO WHERE CLIENTE_ID = ?";
            PreparedSentence checkSentencia = new PreparedSentence(s, checkQuery, 
                SerializerWriteString.INSTANCE, SerializerReadInteger.INSTANCE);
            
            Integer existe = (Integer) checkSentencia.find(clienteId);
            boolean existeRegistro = existe != null && existe > 0;
            
            java.sql.Timestamp ahora = new java.sql.Timestamp(System.currentTimeMillis());
            
            if (existeRegistro) {
                // Actualizar registro existente
                String updateQuery = "UPDATE PUNTOS_ACUMULABLE_DIARIO " +
                                   "SET MONTO_ACUMULADO = ?, FECHA_ACTUALIZACION = ? " +
                                   "WHERE CLIENTE_ID = ?";
                
                System.out.println("🔄 ACTUALIZANDO acumulable existente - Cliente: " + clienteId + 
                                 ", Nuevo monto: $" + nuevoMonto);
                
                PreparedSentence updateSentencia = new PreparedSentence(s, updateQuery,
                    new SerializerWrite<Object[]>() {
                        @Override
                        public void writeValues(DataWrite dp, Object[] obj) throws BasicException {
                            dp.setDouble(1, (Double) obj[0]);
                            dp.setTimestamp(2, (java.sql.Timestamp) obj[1]);
                            dp.setString(3, (String) obj[2]);
                        }
                    });
                
                Object[] params = {nuevoMonto, ahora, clienteId};
                updateSentencia.exec(params);
                System.out.println("✅ Acumulable actualizado en BD: $" + nuevoMonto);
            } else {
                // Insertar nuevo registro
                String insertQuery = "INSERT INTO PUNTOS_ACUMULABLE_DIARIO " +
                                   "(CLIENTE_ID, MONTO_ACUMULADO, FECHA_ACTUALIZACION) " +
                                   "VALUES (?, ?, ?)";
                
                System.out.println("➕ INSERTANDO nuevo acumulable - Cliente: " + clienteId + 
                                 ", Monto inicial: $" + nuevoMonto);
                
                PreparedSentence insertSentencia = new PreparedSentence(s, insertQuery,
                    new SerializerWrite<Object[]>() {
                        @Override
                        public void writeValues(DataWrite dp, Object[] obj) throws BasicException {
                            dp.setString(1, (String) obj[0]);
                            dp.setDouble(2, (Double) obj[1]);
                            dp.setTimestamp(3, (java.sql.Timestamp) obj[2]);
                        }
                    });
                
                Object[] params = {clienteId, nuevoMonto, ahora};
                insertSentencia.exec(params);
                System.out.println("✅ Acumulable insertado en BD: $" + nuevoMonto);
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Error actualizando acumulable diario para cliente " + clienteId + ": " + e.getMessage());
            // Si la tabla no existe, intentar crearla
            if (e.getMessage() != null && e.getMessage().contains("objeto no encontrado")) {
                try {
                    initTables();
                    // Reintentar después de crear la tabla
                    actualizarAcumulableDiario(clienteId, nuevoMonto);
                } catch (Exception ex) {
                    System.err.println("❌ Error creando tabla de acumulable: " + ex.getMessage());
                }
            }
        }
    }
    
    /**
     * Sebastian - Verifica y aplica migraciones necesarias (nuevas columnas)
     */
    private void verificarMigraciones() {
        try {







            // Verificar si la columna LIMITE_DIARIO_PUNTOS existe antes de agregarla
            String checkColumn = "SELECT LIMITE_DIARIO_PUNTOS FROM PUNTOS_CONFIGURACION WHERE 1=0";
            try {
                new StaticSentence(s, checkColumn).exec();
                // Si no hay error, la columna ya existe
            } catch (BasicException checkEx) {
                // La columna no existe, agregarla
                String addLimitColumn = "ALTER TABLE PUNTOS_CONFIGURACION ADD COLUMN LIMITE_DIARIO_PUNTOS INTEGER DEFAULT 500";
                new StaticSentence(s, addLimitColumn).exec();
                System.out.println("✅ Migración: Columna LIMITE_DIARIO_PUNTOS agregada");
            }
            
            // Sebastian - Verificar si la tabla PUNTOS_ACUMULABLE_DIARIO existe
            try {
                new StaticSentence(s, "SELECT COUNT(*) FROM PUNTOS_ACUMULABLE_DIARIO").exec();
                // Si no hay error, la tabla ya existe
            } catch (BasicException checkEx) {
                // La tabla no existe, crearla
                String createAcumulableTable = 
                    "CREATE TABLE PUNTOS_ACUMULABLE_DIARIO (" +
                    "CLIENTE_ID VARCHAR(36) NOT NULL PRIMARY KEY, " +
                    "MONTO_ACUMULADO DECIMAL(10,2) NOT NULL, " +
                    "FECHA_ACTUALIZACION TIMESTAMP NOT NULL)";
                new StaticSentence(s, createAcumulableTable).exec();
                System.out.println("✅ Migración: Tabla PUNTOS_ACUMULABLE_DIARIO creada");
            }
        } catch (BasicException e) {
            // Silencioso si hay error al agregar (probablemente ya existe)
        }
    }
}