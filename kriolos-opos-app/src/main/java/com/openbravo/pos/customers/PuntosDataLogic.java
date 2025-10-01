/**
 * Sebastian - Data Access Object para el sistema de puntos
 */
package com.openbravo.pos.customers;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.PreparedSentence;
import com.openbravo.data.loader.SentenceExec;
import com.openbravo.data.loader.SentenceFind;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.loader.SerializerWriteString;
import com.openbravo.data.loader.SerializerWrite;
import com.openbravo.data.loader.DataWrite;
import com.openbravo.data.loader.Session;
import com.openbravo.data.loader.StaticSentence;
import java.util.List;

public class PuntosDataLogic {
    
    protected Session s;
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
    
    public PuntosDataLogic(Session s) {
        this.s = s;
        
        // Sentencias para configuración de puntos
        m_sentconfig = new StaticSentence(s,
            "SELECT ID, MONTO_POR_PUNTO, PUNTOS_OTORGADOS, SISTEMA_ACTIVO, MONEDA, FECHA_CREACION, FECHA_ACTUALIZACION " +
            "FROM PUNTOS_CONFIGURACION ORDER BY FECHA_ACTUALIZACION DESC",
            null,
            PuntosConfiguracion.getSerializerRead());
            
        m_sentconfigfind = new PreparedSentence(s,
            "SELECT ID, MONTO_POR_PUNTO, PUNTOS_OTORGADOS, SISTEMA_ACTIVO, MONEDA, FECHA_CREACION, FECHA_ACTUALIZACION " +
            "FROM PUNTOS_CONFIGURACION WHERE ID = ?",
            SerializerWriteString.INSTANCE,
            PuntosConfiguracion.getSerializerRead());
            
        m_sentconfigsave = new StaticSentence(s,
            "INSERT INTO PUNTOS_CONFIGURACION (ID, MONTO_POR_PUNTO, PUNTOS_OTORGADOS, SISTEMA_ACTIVO, MONEDA, FECHA_CREACION, FECHA_ACTUALIZACION) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            PuntosConfiguracion.getSerializerWrite());
            
        m_sentconfigupdate = new StaticSentence(s,
            "UPDATE PUNTOS_CONFIGURACION SET MONTO_POR_PUNTO = ?, PUNTOS_OTORGADOS = ?, SISTEMA_ACTIVO = ?, MONEDA = ?, FECHA_ACTUALIZACION = ? " +
            "WHERE ID = ?",
            new SerializerWrite() {
                public void writeValues(DataWrite dp, Object obj) throws BasicException {
                    PuntosConfiguracion config = (PuntosConfiguracion) obj;
                    dp.setDouble(1, config.getMontoPorPunto());
                    dp.setInt(2, config.getPuntosOtorgados());
                    dp.setBoolean(3, config.isSistemaActivo());
                    dp.setString(4, config.getMoneda());
                    dp.setTimestamp(5, config.getFechaActualizacion());
                    dp.setString(6, config.getId());
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
                ClientePuntos puntos = getOrCreateClientePuntos(clienteId);
                puntos.agregarPuntos(puntosAOtorgar, descripcion);
                updateClientePuntos(puntos);
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
     * Inicializa las tablas de puntos si no existen
     */
    public void initTables() throws BasicException {
        try {
            // Crear tabla de configuración si no existe
            String createConfigTable = 
                "CREATE TABLE PUNTOS_CONFIGURACION (" +
                "ID VARCHAR(36) NOT NULL PRIMARY KEY, " +
                "MONTO_POR_PUNTO DECIMAL(10,2) NOT NULL, " +
                "PUNTOS_OTORGADOS INTEGER NOT NULL, " +
                "SISTEMA_ACTIVO BOOLEAN NOT NULL, " +
                "MONEDA VARCHAR(10) NOT NULL, " +
                "FECHA_CREACION TIMESTAMP NOT NULL, " +
                "FECHA_ACTUALIZACION TIMESTAMP NOT NULL)";
            
            try {
                new StaticSentence(s, createConfigTable).exec();
                System.out.println("✅ Tabla PUNTOS_CONFIGURACION creada exitosamente");
            } catch (BasicException e) {
                if (e.getMessage().contains("already exists") || e.getMessage().contains("ya existe")) {
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
                if (e.getMessage().contains("already exists") || e.getMessage().contains("ya existe")) {
                    System.out.println("ℹ️ Tabla CLIENTE_PUNTOS ya existe");
                } else {
                    System.err.println("⚠️ Error creando tabla CLIENTE_PUNTOS: " + e.getMessage());
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
}