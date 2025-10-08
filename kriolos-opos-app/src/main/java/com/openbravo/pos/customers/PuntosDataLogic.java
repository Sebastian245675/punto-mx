package com.openbravo.pos.customers;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.PreparedSentence;
import com.openbravo.data.loader.SentenceExec;
import com.openbravo.data.loader.SentenceFind;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.loader.SerializerWriteString;
import com.openbravo.data.loader.SerializerWrite;
import com.openbravo.data.loader.SerializerReadInteger;
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
     * Agrega puntos a un cliente por una compra
     */
    public void agregarPuntosPorCompra(String clienteId, double montoCompra, String descripcion) throws BasicException {
        PuntosConfiguracion config = getConfiguracionActiva();
        if (config != null && config.isSistemaActivo()) {
            int puntosAOtorgar = config.calcularPuntos(montoCompra);
            if (puntosAOtorgar > 0) {
                // Sebastian - Verificar límite diario antes de otorgar puntos
                int puntosGanadosHoy = getPuntosGanadosHoy(clienteId);
                int limiteDiario = config.getLimiteDiarioPuntos();
                
                System.out.println("🔍 VERIFICANDO LÍMITE: Cliente " + clienteId + 
                                 " - Puntos hoy: " + puntosGanadosHoy + 
                                 " - Límite diario: " + limiteDiario + 
                                 " - Puntos a otorgar: " + puntosAOtorgar);
                
                if (puntosGanadosHoy >= limiteDiario) {
                    System.out.println("🚫 LÍMITE DIARIO ALCANZADO: Cliente " + clienteId + 
                                     " ya ganó " + puntosGanadosHoy + " puntos hoy (límite: " + limiteDiario + ")");
                    return; // No otorgar más puntos
                }
                
                // Si otorgar los puntos excedería el límite, ajustar la cantidad
                int puntosDisponibles = limiteDiario - puntosGanadosHoy;
                if (puntosAOtorgar > puntosDisponibles) {
                    puntosAOtorgar = puntosDisponibles;
                    System.out.println("⚠️ PUNTOS AJUSTADOS: Otorgando " + puntosAOtorgar + 
                                     " puntos para no exceder límite diario");
                }
                
                if (puntosAOtorgar > 0) {
                    // Actualizar puntos totales del cliente
                    ClientePuntos puntos = getOrCreateClientePuntos(clienteId);
                    puntos.agregarPuntos(puntosAOtorgar, descripcion);
                    updateClientePuntos(puntos);
                    
                    // Sebastian - Registrar en historial para límites diarios
                    registrarHistorialPuntos(clienteId, puntosAOtorgar, descripcion, montoCompra);
                    
                    System.out.println("✅ PUNTOS OTORGADOS: " + puntosAOtorgar + 
                                     " puntos (total hoy: " + (puntosGanadosHoy + puntosAOtorgar) + 
                                     "/" + limiteDiario + ")");
                }
            }
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
     * Sebastian - Verifica y aplica migraciones necesarias (nuevas columnas)
     */
    private void verificarMigraciones() {
        try {
            // Intentar agregar columna LIMITE_DIARIO_PUNTOS si no existe
            String addLimitColumn = "ALTER TABLE PUNTOS_CONFIGURACION ADD COLUMN LIMITE_DIARIO_PUNTOS INTEGER DEFAULT 500";
            new StaticSentence(s, addLimitColumn).exec();
            System.out.println("✅ Migración: Columna LIMITE_DIARIO_PUNTOS agregada");
        } catch (BasicException e) {
            if (e.getMessage().contains("already exists") || e.getMessage().contains("ya existe") || 
                e.getMessage().contains("duplicate column")) {
                // La columna ya existe, todo bien
            } else {
                System.out.println("ℹ️ Migración: " + e.getMessage());
            }
        }
    }
}