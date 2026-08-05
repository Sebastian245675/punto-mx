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
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.pos.forms.DataLogicSales; // Sebastian - Importar DataLogicSales
import java.util.List;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PuntosDataLogic {

    private static final Logger LOGGER = Logger.getLogger(PuntosDataLogic.class.getName());
    private static final String DEFAULT_SUPABASE_URL = "https://wotsbsjxabwtovxpfgly.supabase.co/rest/v1";
    private static final String DEFAULT_SUPABASE_API_KEY = "sb_publishable_ztjnUyfwQ7rAFW3T-g4ocA_8vqYXbRQ";

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
                "SELECT ID, MONTO_POR_PUNTO, PUNTOS_OTORGADOS, SISTEMA_ACTIVO, MONEDA, LIMITE_DIARIO_PUNTOS, FECHA_CREACION, FECHA_ACTUALIZACION "
                        +
                        "FROM PUNTOS_CONFIGURACION ORDER BY FECHA_ACTUALIZACION DESC",
                null,
                PuntosConfiguracion.getSerializerRead());

        m_sentconfigfind = new PreparedSentence(s,
                "SELECT ID, MONTO_POR_PUNTO, PUNTOS_OTORGADOS, SISTEMA_ACTIVO, MONEDA, LIMITE_DIARIO_PUNTOS, FECHA_CREACION, FECHA_ACTUALIZACION "
                        +
                        "FROM PUNTOS_CONFIGURACION WHERE ID = ?",
                SerializerWriteString.INSTANCE,
                PuntosConfiguracion.getSerializerRead());

        m_sentconfigsave = new StaticSentence(s,
                "INSERT INTO PUNTOS_CONFIGURACION (ID, MONTO_POR_PUNTO, PUNTOS_OTORGADOS, SISTEMA_ACTIVO, MONEDA, LIMITE_DIARIO_PUNTOS, FECHA_CREACION, FECHA_ACTUALIZACION) "
                        +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                PuntosConfiguracion.getSerializerWrite());

        m_sentconfigupdate = new StaticSentence(s,
                "UPDATE PUNTOS_CONFIGURACION SET MONTO_POR_PUNTO = ?, PUNTOS_OTORGADOS = ?, SISTEMA_ACTIVO = ?, MONEDA = ?, LIMITE_DIARIO_PUNTOS = ?, FECHA_ACTUALIZACION = ? "
                        +
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
                "SELECT ID, CLIENTE_ID, PUNTOS_ACTUALES, PUNTOS_TOTALES, ULTIMA_TRANSACCION, FECHA_ULTIMA_TRANSACCION, FECHA_CREACION "
                        +
                        "FROM CLIENTE_PUNTOS ORDER BY FECHA_ULTIMA_TRANSACCION DESC",
                null,
                ClientePuntos.getSerializerRead());

        m_sentpuntosfind = new PreparedSentence(s,
                "SELECT ID, CLIENTE_ID, PUNTOS_ACTUALES, PUNTOS_TOTALES, ULTIMA_TRANSACCION, FECHA_ULTIMA_TRANSACCION, FECHA_CREACION "
                        +
                        "FROM CLIENTE_PUNTOS WHERE CLIENTE_ID = ?",
                SerializerWriteString.INSTANCE,
                ClientePuntos.getSerializerRead());

        m_sentpuntossave = new StaticSentence(s,
                "INSERT INTO CLIENTE_PUNTOS (ID, CLIENTE_ID, PUNTOS_ACTUALES, PUNTOS_TOTALES, ULTIMA_TRANSACCION, FECHA_ULTIMA_TRANSACCION, FECHA_CREACION) "
                        +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                ClientePuntos.getSerializerWrite());

        m_sentpuntosupdate = new StaticSentence(s,
                "UPDATE CLIENTE_PUNTOS SET PUNTOS_ACTUALES = ?, PUNTOS_TOTALES = ?, ULTIMA_TRANSACCION = ?, FECHA_ULTIMA_TRANSACCION = ? "
                        +
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
        configDefault.setPuntosOtorgados(10); // 10 puntos
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
        if (puntos.getPuntosActuales() != 0 || puntos.getPuntosTotales() != 0) {
            syncPuntosToSupabaseAndLocalCustomers(puntos.getClienteId(), puntos.getPuntosActuales());
        }
    }

    public void updateClientePuntos(ClientePuntos puntos) throws BasicException {
        m_sentpuntosupdate.exec(puntos);
        syncPuntosToSupabaseAndLocalCustomers(puntos.getClienteId(), puntos.getPuntosActuales());
    }

    private void syncPuntosToSupabaseAndLocalCustomers(String clienteId, int totalPuntos) {
        new Thread(() -> {
            try {
                // 1. Actualizar tabla local CUSTOMERS (columna PUNTOS)
                new PreparedSentence(s, "UPDATE CUSTOMERS SET PUNTOS = ? WHERE ID = ?",
                        new SerializerWriteBasic(new Datas[]{Datas.INT, Datas.STRING})).exec(new Object[]{totalPuntos, clienteId});
                
                // 2. Sincronizar con Supabase
                com.openbravo.pos.supabase.CustomerServiceSupabase svc = new com.openbravo.pos.supabase.CustomerServiceSupabase(DEFAULT_SUPABASE_URL, DEFAULT_SUPABASE_API_KEY);
                svc.updatePuntos(clienteId, totalPuntos);
                
                LOGGER.info("Puntos del cliente " + clienteId + " sincronizados: " + totalPuntos);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error sincronizando puntos con Supabase/Local: " + e.getMessage());
            }
        }).start();
    }

    public void deleteClientePuntos(String clienteId) throws BasicException {
        m_sentpuntosdelete.exec(clienteId);
    }

    /**
     * Agrega puntos a un cliente por una compra usando acumulable diario
     * Sebastian - Modificado para acumular montos del día y otorgar puntos cuando
     * se alcance el umbral
     * ACTUALIZADO: Corregida la lógica para que funcione correctamente en tiempo
     * real
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

        // Obtener o inicializar el acumulable del día (este ya es el restante de
        // compras anteriores)
        double acumulableActual = obtenerAcumulableDiario(clienteId);

        System.out.println("🔍 DEBUG INICIAL - Cliente: " + clienteId +
                ", Acumulable actual (restante): $" + acumulableActual +
                ", Monto compra: $" + montoCompra);

        // Sumar el monto de la compra actual al acumulable restante
        double nuevoAcumulableTotal = acumulableActual + montoCompra;

        System.out.println("💰 ACUMULABLE TOTAL: Cliente " + clienteId +
                " - Restante anterior: $" + acumulableActual +
                " + Compra nueva: $" + montoCompra +
                " = Total acumulado: $" + nuevoAcumulableTotal);

        // Calcular el acumulable restante y los puntos de forma más precisa
        double montoPorPunto = config.getMontoPorPunto();

        // Calcular cuántos tramos completos se pueden formar con el nuevo acumulable
        // total
        int tramosCompletosNuevo = (int) Math.floor(nuevoAcumulableTotal / montoPorPunto);
        int tramosCompletosAnterior = (int) Math.floor(acumulableActual / montoPorPunto);

        // Los puntos nuevos son la diferencia de tramos completos multiplicado por
        // puntos otorgados
        int tramosNuevos = tramosCompletosNuevo - tramosCompletosAnterior;
        int puntosNuevos = tramosNuevos * config.getPuntosOtorgados();

        // Calcular el acumulable restante después de otorgar puntos
        // Esto es lo que quedará para la próxima compra del día
        double montoUsadoParaPuntos = tramosCompletosNuevo * montoPorPunto;
        double nuevoAcumulableRestante = nuevoAcumulableTotal - montoUsadoParaPuntos;

        System.out.println("🎯 CÁLCULO DE PUNTOS:");
        System.out.println("   - Acumulable anterior (restante): $" + acumulableActual + " → " + tramosCompletosAnterior
                + " tramos completos");
        System.out.println("   - Acumulable nuevo (total): $" + nuevoAcumulableTotal + " → " + tramosCompletosNuevo
                + " tramos completos");
        System.out.println("   - Tramos nuevos: " + tramosNuevos + " × " + config.getPuntosOtorgados() + " puntos = "
                + puntosNuevos + " puntos nuevos");

        System.out.println("📊 CÁLCULO ACUMULABLE RESTANTE:");
        System.out.println("   - Total acumulado: $" + nuevoAcumulableTotal);
        System.out.println("   - Tramos completos: " + tramosCompletosNuevo + " × $" + montoPorPunto + " = $"
                + montoUsadoParaPuntos);
        System.out.println("   - Nuevo acumulable restante: $" + nuevoAcumulableRestante);

        // Verificar límite diario antes de otorgar puntos
        int puntosGanadosHoy = getPuntosGanadosHoy(clienteId);
        int limiteDiario = config.getLimiteDiarioPuntos();

        System.out.println("🔍 VERIFICANDO LÍMITE DIARIO:");
        System.out.println("   - Puntos ganados hoy: " + puntosGanadosHoy);
        System.out.println("   - Límite diario: " + limiteDiario);
        System.out.println("   - Puntos nuevos a otorgar: " + puntosNuevos);

        // Si hay puntos nuevos para otorgar
        if (puntosNuevos > 0) {
            if (puntosGanadosHoy >= limiteDiario) {
                System.out.println("🚫 LÍMITE DIARIO ALCANZADO: Cliente " + clienteId +
                        " ya ganó " + puntosGanadosHoy + " puntos hoy (límite: " + limiteDiario + ")");
                System.out.println("   - No se otorgan puntos, pero se actualiza el acumulable restante");
                // Aún así actualizamos el acumulable aunque no otorguemos puntos
                actualizarAcumulableDiario(clienteId, nuevoAcumulableRestante);
                return;
            }

            // Si otorgar los puntos excedería el límite, ajustar la cantidad
            int puntosDisponibles = limiteDiario - puntosGanadosHoy;
            if (puntosNuevos > puntosDisponibles) {
                System.out.println("⚠️ AJUSTANDO PUNTOS: Se otorgarían " + puntosNuevos +
                        " pero solo hay " + puntosDisponibles + " disponibles");
                puntosNuevos = puntosDisponibles;
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
        } else {
            System.out.println("ℹ️ No hay puntos nuevos para otorgar en esta compra");
        }

        // IMPORTANTE: SIEMPRE actualizar el acumulable diario restante, incluso si no
        // se otorgaron puntos
        // Este valor se guarda para que las próximas compras del día puedan seguir
        // acumulando desde donde quedó
        try {
            actualizarAcumulableDiario(clienteId, nuevoAcumulableRestante);
            System.out.println("✅ Acumulable diario actualizado: $" + nuevoAcumulableRestante +
                    " (este valor se usará en la próxima compra del día)");
        } catch (Exception e) {
            System.err.println("❌ ERROR actualizando acumulable: " + e.getMessage());
            e.printStackTrace();
            // No lanzar excepción para no interrumpir la venta, pero registrar el error
        }
    }

    /**
     * Agrega puntos a un cliente por un producto específico
     */
    public void agregarPuntosPorProducto(String clienteId, String producto, int puntosEspecificos)
            throws BasicException {
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

            PreparedSentence sentencia = new PreparedSentence(s, query, SerializerWriteString.INSTANCE,
                    SerializerReadInteger.INSTANCE);
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
     * Sebastian - Obtiene los puntos otorgados para un ticket específico desde el
     * historial
     * 
     * @param ticketId  ID del ticket
     * @param clienteId ID del cliente
     * @return Puntos otorgados para este ticket, o -1 si no se encontraron
     */
    public int getPuntosOtorgadosPorTicket(String ticketId, String clienteId) throws BasicException {
        try {
            String descripcionBusqueda = "Venta automática #" + ticketId;
            String query = "SELECT COALESCE(SUM(PUNTOS_OTORGADOS), 0) " +
                    "FROM PUNTOS_HISTORIAL " +
                    "WHERE CLIENTE_ID = ? AND DESCRIPCION LIKE ?";

            PreparedSentence sentencia = new PreparedSentence(s, query,
                    new SerializerWrite<Object[]>() {
                        public void writeValues(DataWrite dp, Object[] obj) throws BasicException {
                            dp.setString(1, (String) obj[0]); // CLIENTE_ID
                            dp.setString(2, (String) obj[1]); // DESCRIPCION LIKE
                        }
                    },
                    SerializerReadInteger.INSTANCE);

            String pattern = "%" + descripcionBusqueda + "%";
            Integer resultado = (Integer) sentencia.find(new Object[] { clienteId, pattern });

            int puntosTicket = resultado != null ? resultado : 0;
            if (puntosTicket > 0) {
                System.out.println("🎫 Puntos otorgados para ticket #" + ticketId + ": " + puntosTicket);
            }
            return puntosTicket;

        } catch (Exception e) {
            System.err.println("⚠️ Error obteniendo puntos del ticket #" + ticketId + ": " + e.getMessage());
            return -1; // Retornar -1 para indicar error
        }
    }

    /**
     * Inicializa las tablas de puntos si no existen
     */
    public void initTables() throws BasicException {
        // Sebastian - Primero verificamos si las tablas ya existen para evitar logs
        // SEVERE
        if (tablasYaExisten()) {
            System.out.println("ℹ️ Sistema de puntos: Tablas ya existen");
            verificarMigraciones(); // Verificar si necesitamos agregar columnas nuevas
            return;
        }

        try {
            // Crear tabla de configuración si no existe
            String createConfigTable = "CREATE TABLE PUNTOS_CONFIGURACION (" +
                    "ID VARCHAR(36) NOT NULL PRIMARY KEY, " +
                    "MONTO_POR_PUNTO DECIMAL(10,2) NOT NULL, " +
                    "PUNTOS_OTORGADOS INTEGER NOT NULL, " +
                    "SISTEMA_ACTIVO BOOLEAN NOT NULL, " +
                    "MONEDA VARCHAR(10) NOT NULL, " +
                    "LIMITE_DIARIO_PUNTOS INTEGER DEFAULT 500, " + // Sebastian - Nuevo campo
                    "FECHA_CREACION DATETIME NOT NULL, " +
                    "FECHA_ACTUALIZACION DATETIME NOT NULL)";

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
            String createPuntosTable = "CREATE TABLE CLIENTE_PUNTOS (" +
                    "ID VARCHAR(36) NOT NULL PRIMARY KEY, " +
                    "CLIENTE_ID VARCHAR(255) NOT NULL UNIQUE, " +
                    "PUNTOS_ACTUALES INTEGER NOT NULL, " +
                    "PUNTOS_TOTALES INTEGER NOT NULL, " +
                    "ULTIMA_TRANSACCION VARCHAR(255), " +
                    "FECHA_ULTIMA_TRANSACCION DATETIME, " +
                    "FECHA_CREACION DATETIME NOT NULL)";

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
            String createHistorialTable = "CREATE TABLE PUNTOS_HISTORIAL (" +
                    "ID VARCHAR(36) NOT NULL PRIMARY KEY, " +
                    "CLIENTE_ID VARCHAR(255) NOT NULL, " +
                    "PUNTOS_OTORGADOS INTEGER NOT NULL, " +
                    "DESCRIPCION VARCHAR(255), " +
                    "MONTO_COMPRA DECIMAL(10,2), " +
                    "FECHA_TRANSACCION DATETIME NOT NULL)";

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
            String createAcumulableTable = "CREATE TABLE PUNTOS_ACUMULABLE_DIARIO (" +
                    "CLIENTE_ID VARCHAR(255) NOT NULL PRIMARY KEY, " +
                    "MONTO_ACUMULADO DECIMAL(10,2) NOT NULL, " +
                    "FECHA_ACTUALIZACION DATETIME NOT NULL)";

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
                List configs = m_sentconfig.list();
                if (configs == null || configs.isEmpty()) {
                    PuntosConfiguracion configDefault = new PuntosConfiguracion();
                    configDefault.setMontoPorPunto(400.0); // $400 MX
                    configDefault.setPuntosOtorgados(10); // 10 puntos
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
        try {
            ClientePuntos puntos = getClientePuntos(clienteId);
            return puntos != null ? puntos.getPuntosActuales() : 0;
        } catch (Exception e) {
            return 0;
        }
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
            throw new BasicException("No se pueden quitar " + puntosAQuitar + " puntos. El cliente solo tiene "
                    + puntosActuales + " puntos disponibles.");
        }

        puntos.setPuntosActuales(puntosActuales - puntosAQuitar);
        puntos.setUltimaTransaccion("Ajuste manual: -" + puntosAQuitar + " puntos");
        puntos.setFechaUltimaTransaccion(new java.sql.Timestamp(System.currentTimeMillis()));

        updateClientePuntos(puntos);
    }

    /**
     * Sebastian - Clase para retornar información sobre el descuento de puntos
     */
    public static class ResultadoDescuento {
        private int puntosDescontados;
        private int puntosAnteriores;
        private int puntosActuales;
        private double montoDescontado;
        private boolean seDescontaronPuntos;

        public ResultadoDescuento(int puntosDescontados, int puntosAnteriores, int puntosActuales,
                double montoDescontado, boolean seDescontaronPuntos) {
            this.puntosDescontados = puntosDescontados;
            this.puntosAnteriores = puntosAnteriores;
            this.puntosActuales = puntosActuales;
            this.montoDescontado = montoDescontado;
            this.seDescontaronPuntos = seDescontaronPuntos;
        }

        public int getPuntosDescontados() {
            return puntosDescontados;
        }

        public int getPuntosAnteriores() {
            return puntosAnteriores;
        }

        public int getPuntosActuales() {
            return puntosActuales;
        }

        public double getMontoDescontado() {
            return montoDescontado;
        }

        public boolean seDescontaronPuntos() {
            return seDescontaronPuntos;
        }
    }

    /**
     * Sebastian - Descuenta puntos cuando se cancela una venta
     * Busca en el historial los puntos otorgados para el ticket y los descuenta
     * 
     * @param ticketId              ID del ticket cancelado
     * @param clienteId             ID del cliente
     * @param montoAcumulableTicket Monto acumulable del ticket (para actualizar
     *                              acumulable diario incluso si no hay puntos)
     * @return ResultadoDescuento con información sobre los puntos descontados
     */
    public ResultadoDescuento descontarPuntosPorCancelacion(String ticketId, String clienteId,
            double montoAcumulableTicket) throws BasicException {
        System.out
                .println("🔄 descontarPuntosPorCancelacion INICIADO - Ticket: " + ticketId + ", Cliente: " + clienteId);

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
                                    dr.getString(1), // ID
                                    dr.getString(2), // CLIENTE_ID
                                    dr.getInt(3), // PUNTOS_OTORGADOS
                                    dr.getString(4), // DESCRIPCION
                                    dr.getDouble(5) // MONTO_COMPRA
                            };
                        }
                    });

            String pattern = "%" + descripcionBusqueda + "%";
            List<Object[]> resultados = sentencia.list(new Object[] { clienteId, pattern });

            if (resultados == null || resultados.isEmpty()) {
                System.out.println("ℹ️ No se encontraron puntos otorgados para el ticket #" + ticketId);
                // Retornar resultado vacío pero actualizar acumulable si es necesario
                if (montoAcumulableTicket > 0) {
                    double acumulableActual = obtenerAcumulableDiario(clienteId);
                    // Permitir valores negativos para reflejar correctamente la cancelación
                    double nuevoAcumulable = acumulableActual - montoAcumulableTicket;

                    // Limitar solo si queda muy negativo (más de un tramo)
                    PuntosConfiguracion config = getConfiguracionActiva();
                    if (config != null) {
                        double montoPorPunto = config.getMontoPorPunto();
                        if (nuevoAcumulable < -montoPorPunto) {
                            nuevoAcumulable = 0.0;
                        }
                    } else {
                        nuevoAcumulable = Math.max(0.0, nuevoAcumulable);
                    }

                    System.out.println("🔄 Actualizando acumulable (sin puntos en historial): $" + acumulableActual
                            + " - $" + montoAcumulableTicket + " = $" + nuevoAcumulable);
                    actualizarAcumulableDiario(clienteId, nuevoAcumulable);
                }
                return new ResultadoDescuento(0, 0, obtenerPuntos(clienteId), montoAcumulableTicket, false);
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

            int puntosActuales = 0;
            int puntosADescontar = 0;

            if (totalPuntosADescontar > 0) {
                // Descontar puntos del cliente
                ClientePuntos puntos = getOrCreateClientePuntos(clienteId);
                puntosActuales = puntos.getPuntosActuales();

                // Si el cliente no tiene suficientes puntos, solo descontar los que tiene
                puntosADescontar = Math.min(totalPuntosADescontar, puntosActuales);

                if (puntosADescontar > 0) {
                    puntos.setPuntosActuales(puntosActuales - puntosADescontar);
                    puntos.setUltimaTransaccion(
                            "Cancelación venta #" + ticketId + ": -" + puntosADescontar + " puntos");
                    puntos.setFechaUltimaTransaccion(new java.sql.Timestamp(System.currentTimeMillis()));
                    updateClientePuntos(puntos);

                    System.out.println("✅ Puntos descontados: " + puntosADescontar + " (tenía " + puntosActuales
                            + ", ahora tiene " + puntos.getPuntosActuales() + ")");
                } else {
                    System.out.println("⚠️ Cliente no tiene puntos para descontar (tiene " + puntosActuales
                            + ", se intentaron descontar " + totalPuntosADescontar + ")");
                }
            } else {
                // Si no hay puntos para descontar, obtener puntos actuales para el resultado
                puntosActuales = getOrCreateClientePuntos(clienteId).getPuntosActuales();
            }

            // Actualizar el acumulable diario (restar el monto de la venta cancelada)
            // Sebastian - IMPORTANTE: El acumulable debe restablecerse correctamente
            // Cuando se otorgan puntos, el acumulable se reduce por el monto usado para
            // generar puntos
            // Al cancelar, debemos restar el monto completo de la venta cancelada del
            // acumulable actual
            double montoADescontarAcumulable = totalMontoADescontar > 0 ? totalMontoADescontar : montoAcumulableTicket;

            if (montoADescontarAcumulable > 0) {
                double acumulableActual = obtenerAcumulableDiario(clienteId);
                PuntosConfiguracion config = getConfiguracionActiva();

                // Restar el monto completo de la venta cancelada
                // Si el acumulable queda negativo, significa que parte del monto cancelado
                // ya se usó para generar puntos (y esos puntos ya se descontaron arriba)
                double nuevoAcumulable = acumulableActual - montoADescontarAcumulable;

                // Permitir valores negativos hasta un límite razonable (un tramo completo)
                // Esto permite que el acumulable refleje correctamente la cancelación
                // En la próxima compra, el acumulable se ajustará automáticamente
                if (config != null) {
                    double montoPorPunto = config.getMontoPorPunto();
                    // Solo limitar si queda muy negativo (más de un tramo), puede indicar un error
                    if (nuevoAcumulable < -montoPorPunto) {
                        System.out.println("⚠️ Acumulable muy negativo (" + nuevoAcumulable
                                + "), limitando a 0 para evitar inconsistencias");
                        nuevoAcumulable = 0.0;
                    }
                } else {
                    // Si no hay configuración, limitar a 0 como medida de seguridad
                    nuevoAcumulable = Math.max(0.0, nuevoAcumulable);
                }

                String fuente = totalMontoADescontar > 0 ? "historial" : "ticket (sin puntos en historial)";
                System.out.println("🔄 RESTABLECIENDO ACUMULABLE DIARIO (" + fuente + "):");
                System.out.println("   - Acumulable actual: $" + acumulableActual);
                System.out.println("   - Monto de venta cancelada: $" + montoADescontarAcumulable);
                System.out.println("   - Nuevo acumulable: $" + nuevoAcumulable);
                System.out.println(
                        "   " + (nuevoAcumulable < 0 ? "⚠️ Acumulable negativo (normal si la venta ya generó puntos)"
                                : "✅ Acumulable positivo"));

                actualizarAcumulableDiario(clienteId, nuevoAcumulable);
                System.out.println("✅ Acumulable diario restablecido correctamente: $" + nuevoAcumulable);
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

            deleteSentencia.exec(new Object[] { clienteId, pattern });
            System.out.println("🗑️ Transacciones del historial eliminadas para ticket #" + ticketId);

            // Retornar resultado del descuento
            int puntosFinales = getOrCreateClientePuntos(clienteId).getPuntosActuales();
            int puntosDescontadosFinal = 0;
            int puntosAnterioresFinal = puntosActuales;

            if (totalPuntosADescontar > 0 && puntosADescontar > 0) {
                puntosDescontadosFinal = puntosADescontar;
            }

            return new ResultadoDescuento(
                    puntosDescontadosFinal,
                    puntosAnterioresFinal,
                    puntosFinales,
                    montoADescontarAcumulable,
                    puntosDescontadosFinal > 0);

        } catch (Exception e) {
            System.err.println("❌ ERROR en descontarPuntosPorCancelacion: " + e.getMessage());
            e.printStackTrace();
            // No lanzar excepción para no interrumpir la cancelación del ticket
            // Retornar resultado vacío en caso de error
            try {
                return new ResultadoDescuento(0, obtenerPuntos(clienteId), obtenerPuntos(clienteId), 0, false);
            } catch (Exception ex) {
                return new ResultadoDescuento(0, 0, 0, 0, false);
            }
        }
    }

    /**
     * Sebastian - Registra una transacción de puntos en el historial para límites
     * diarios
     */
    private void registrarHistorialPuntos(String clienteId, int puntosOtorgados, String descripcion, double montoCompra)
            throws BasicException {
        try {
            String insertHistorial = "INSERT INTO PUNTOS_HISTORIAL (ID, CLIENTE_ID, PUNTOS_OTORGADOS, DESCRIPCION, MONTO_COMPRA, FECHA_TRANSACCION) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            String id = java.util.UUID.randomUUID().toString();
            java.sql.Timestamp ahora = new java.sql.Timestamp(System.currentTimeMillis());

            PreparedSentence sentencia = new PreparedSentence(s, insertHistorial,
                    new SerializerWrite<Object[]>() {
                        public void writeValues(DataWrite dp, Object[] obj) throws BasicException {
                            dp.setString(1, (String) obj[0]); // ID
                            dp.setString(2, (String) obj[1]); // CLIENTE_ID
                            dp.setInt(3, (Integer) obj[2]); // PUNTOS_OTORGADOS
                            dp.setString(4, (String) obj[3]); // DESCRIPCION
                            dp.setDouble(5, (Double) obj[4]); // MONTO_COMPRA
                            dp.setTimestamp(6, (java.sql.Timestamp) obj[5]); // FECHA_TRANSACCION
                        }
                    });

            Object[] params = { id, clienteId, puntosOtorgados, descripcion, montoCompra, ahora };
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
     * Sebastian - Clase para representar puntos redimidos
     */
    public static class PuntosRedimidos {
        private String clienteId;
        private String nombreCliente;
        private int puntosRedimidos;
        private java.util.Date fecha;

        public PuntosRedimidos(String clienteId, String nombreCliente, int puntosRedimidos, java.util.Date fecha) {
            this.clienteId = clienteId;
            this.nombreCliente = nombreCliente;
            this.puntosRedimidos = puntosRedimidos;
            this.fecha = fecha;
        }

        public String getClienteId() {
            return clienteId;
        }

        public String getNombreCliente() {
            return nombreCliente;
        }

        public int getPuntosRedimidos() {
            return puntosRedimidos;
        }

        public java.util.Date getFecha() {
            return fecha;
        }
    }

    /**
     * Sebastian - Registra puntos redimidos en el historial
     */
    public void registrarPuntosRedimidos(String clienteId, String nombreCliente, int puntosRedimidos)
            throws BasicException {
        try {
            // Verificar si existe la tabla, si no crearla
            try {
                new StaticSentence(s, "SELECT COUNT(*) FROM PUNTOS_REDIMIDOS").exec();
            } catch (Exception e) {
                // Crear tabla si no existe
                String createTable = "CREATE TABLE PUNTOS_REDIMIDOS (" +
                        "ID VARCHAR(36) NOT NULL PRIMARY KEY, " +
                        "CLIENTE_ID VARCHAR(255) NOT NULL, " +
                        "NOMBRE_CLIENTE VARCHAR(255) NOT NULL, " +
                        "PUNTOS_REDIMIDOS INTEGER NOT NULL, " +
                        "FECHA_REDENCION TIMESTAMP NOT NULL)";
                new StaticSentence(s, createTable).exec();
                System.out.println("✅ Tabla PUNTOS_REDIMIDOS creada exitosamente");
            }

            String insertQuery = "INSERT INTO PUNTOS_REDIMIDOS (ID, CLIENTE_ID, NOMBRE_CLIENTE, PUNTOS_REDIMIDOS, FECHA_REDENCION) "
                    +
                    "VALUES (?, ?, ?, ?, ?)";

            String id = java.util.UUID.randomUUID().toString();
            java.sql.Timestamp ahora = new java.sql.Timestamp(System.currentTimeMillis());

            PreparedSentence sentencia = new PreparedSentence(s, insertQuery,
                    new SerializerWrite<Object[]>() {
                        public void writeValues(DataWrite dp, Object[] obj) throws BasicException {
                            dp.setString(1, (String) obj[0]); // ID
                            dp.setString(2, (String) obj[1]); // CLIENTE_ID
                            dp.setString(3, (String) obj[2]); // NOMBRE_CLIENTE
                            dp.setInt(4, (Integer) obj[3]); // PUNTOS_REDIMIDOS
                            dp.setTimestamp(5, (java.sql.Timestamp) obj[4]); // FECHA_REDENCION
                        }
                    });

            Object[] params = { id, clienteId, nombreCliente, puntosRedimidos, ahora };
            sentencia.exec(params);

            System.out.println(
                    "✅ Puntos redimidos registrados: " + puntosRedimidos + " puntos para cliente " + nombreCliente);

        } catch (Exception e) {
            System.err.println("⚠️ Error registrando puntos redimidos: " + e.getMessage());
            throw new BasicException("Error registrando puntos redimidos: " + e.getMessage(), e);
        }
    }

    /**
     * Sebastian - Obtiene el historial de puntos redimidos de un cliente
     */
    public java.util.List<PuntosRedimidos> getHistorialPuntosRedimidos(String clienteId) throws BasicException {
        try {
            java.util.List<PuntosRedimidos> historial = new java.util.ArrayList<>();

            // Verificar si existe la tabla
            try {
                new StaticSentence(s, "SELECT COUNT(*) FROM PUNTOS_REDIMIDOS").exec();
            } catch (Exception e) {
                // Crear tabla si no existe
                try {
                    String createTable = "CREATE TABLE PUNTOS_REDIMIDOS (" +
                            "ID VARCHAR(36) NOT NULL PRIMARY KEY, " +
                            "CLIENTE_ID VARCHAR(255) NOT NULL, " +
                            "NOMBRE_CLIENTE VARCHAR(255) NOT NULL, " +
                            "PUNTOS_REDIMIDOS INTEGER NOT NULL, " +
                            "FECHA_REDENCION TIMESTAMP NOT NULL)";
                    new StaticSentence(s, createTable).exec();
                    LOGGER.info("✅ Tabla PUNTOS_REDIMIDOS creada exitosamente desde historial");
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error creando tabla PUNTOS_REDIMIDOS", ex);
                    return historial;
                }
            }

            String query = "SELECT CLIENTE_ID, NOMBRE_CLIENTE, PUNTOS_REDIMIDOS, FECHA_REDENCION " +
                    "FROM PUNTOS_REDIMIDOS " +
                    "WHERE CLIENTE_ID = ? " +
                    "ORDER BY FECHA_REDENCION DESC";

            PreparedSentence sentencia = new PreparedSentence(s, query,
                    SerializerWriteString.INSTANCE,
                    new SerializerRead<PuntosRedimidos>() {
                        @Override
                        public PuntosRedimidos readValues(DataRead dr) throws BasicException {
                            String clienteId = dr.getString(1);
                            String nombreCliente = dr.getString(2);
                            int puntos = dr.getInt(3);
                            // Leer la fecha - puede ser Timestamp, Date o null
                            Object fechaObj = dr.getObject(4);
                            java.util.Date fecha;
                            if (fechaObj != null) {
                                if (fechaObj instanceof java.sql.Timestamp) {
                                    java.sql.Timestamp ts = (java.sql.Timestamp) fechaObj;
                                    fecha = new java.util.Date(ts.getTime());
                                } else if (fechaObj instanceof java.util.Date) {
                                    fecha = (java.util.Date) fechaObj;
                                } else if (fechaObj instanceof java.sql.Date) {
                                    java.sql.Date sqlDate = (java.sql.Date) fechaObj;
                                    fecha = new java.util.Date(sqlDate.getTime());
                                } else {
                                    fecha = new java.util.Date();
                                }
                            } else {
                                fecha = new java.util.Date();
                            }
                            return new PuntosRedimidos(clienteId, nombreCliente, puntos, fecha);
                        }
                    });

            java.util.List resultados = sentencia.list(clienteId);
            if (resultados != null) {
                for (Object obj : resultados) {
                    historial.add((PuntosRedimidos) obj);
                }
            }

            return historial;

        } catch (Exception e) {
            System.err.println("⚠️ Error obteniendo historial de puntos redimidos: " + e.getMessage());
            throw new BasicException("Error obteniendo historial: " + e.getMessage(), e);
        }
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
                                    dr.getDouble(1), // MONTO_ACUMULADO
                                    fechaObj // FECHA_ACTUALIZACION (Date, Timestamp, o null)
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
                        calActualizacion.get(java.util.Calendar.DAY_OF_YEAR) == calHoy
                                .get(java.util.Calendar.DAY_OF_YEAR);

                // Si la fecha es diferente a hoy, resetear el acumulable
                if (!mismoDia) {
                    System.out.println("🔄 Nuevo día detectado para cliente " + clienteId +
                            ", reseteando acumulable (fecha anterior: " + fechaActualizacion + ")");
                    actualizarAcumulableDiario(clienteId, 0.0);
                    return 0.0;
                }

                System.out.println("✅ Acumulable recuperado del día: $" + montoAcumulado +
                        " (fecha última actualización: " + fechaActualizacion + ")");
                return montoAcumulado;
            } else {
                // No existe registro, crear uno con acumulable 0
                System.out.println("ℹ️ No existe registro de acumulable para cliente " + clienteId +
                        ", creando nuevo registro con acumulable = $0.00");
                actualizarAcumulableDiario(clienteId, 0.0);
                return 0.0;
            }

        } catch (Exception e) {
            System.err
                    .println("⚠️ Error obteniendo acumulable diario para cliente " + clienteId + ": " + e.getMessage());
            e.printStackTrace();
            // Si hay error (tabla no existe), retornar 0 y la tabla se creará en la próxima
            // actualización
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
                        ", Nuevo monto: $" + nuevoMonto + ", Fecha: " + ahora);

                PreparedSentence updateSentencia = new PreparedSentence(s, updateQuery,
                        new SerializerWrite<Object[]>() {
                            @Override
                            public void writeValues(DataWrite dp, Object[] obj) throws BasicException {
                                dp.setDouble(1, (Double) obj[0]);
                                dp.setTimestamp(2, (java.sql.Timestamp) obj[1]);
                                dp.setString(3, (String) obj[2]);
                            }
                        });

                Object[] params = { nuevoMonto, ahora, clienteId };
                updateSentencia.exec(params);
                System.out.println("✅ Acumulable actualizado en BD: $" + nuevoMonto + " (fecha: " + ahora + ")");
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

                Object[] params = { clienteId, nuevoMonto, ahora };
                insertSentencia.exec(params);
                System.out.println("✅ Acumulable insertado en BD: $" + nuevoMonto + " (fecha: " + ahora + ")");
            }

        } catch (Exception e) {
            System.err.println(
                    "⚠️ Error actualizando acumulable diario para cliente " + clienteId + ": " + e.getMessage());
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
                String createAcumulableTable = "CREATE TABLE PUNTOS_ACUMULABLE_DIARIO (" +
                        "CLIENTE_ID VARCHAR(255) NOT NULL PRIMARY KEY, " +
                        "MONTO_ACUMULADO DECIMAL(10,2) NOT NULL, " +
                        "FECHA_ACTUALIZACION TIMESTAMP NOT NULL)";
                new StaticSentence(s, createAcumulableTable).exec();
                System.out.println("✅ Migración: Tabla PUNTOS_ACUMULABLE_DIARIO creada");
            }

            // Migración: Alterar CLIENTE_ID en todas las tablas de puntos a VARCHAR(255) para soportar IDs remotos prefijados con cli_
            try {
                new StaticSentence(s, "ALTER TABLE CLIENTE_PUNTOS ALTER COLUMN CLIENTE_ID VARCHAR(255)").exec();
                System.out.println("✅ Migración: CLIENTE_PUNTOS.CLIENTE_ID alterado a VARCHAR(255)");
            } catch (BasicException ex) {
                // Silencioso si falla o ya está aplicado
            }
            try {
                new StaticSentence(s, "ALTER TABLE PUNTOS_HISTORIAL ALTER COLUMN CLIENTE_ID VARCHAR(255)").exec();
                System.out.println("✅ Migración: PUNTOS_HISTORIAL.CLIENTE_ID alterado a VARCHAR(255)");
            } catch (BasicException ex) {
                // Silencioso si falla o ya está aplicado
            }
            try {
                new StaticSentence(s, "ALTER TABLE PUNTOS_ACUMULABLE_DIARIO ALTER COLUMN CLIENTE_ID VARCHAR(255)").exec();
                System.out.println("✅ Migración: PUNTOS_ACUMULABLE_DIARIO.CLIENTE_ID alterado a VARCHAR(255)");
            } catch (BasicException ex) {
                // Silencioso si falla o ya está aplicado
            }
            try {
                // Verificar si existe la tabla PUNTOS_REDIMIDOS antes de intentar alterarla
                new StaticSentence(s, "SELECT COUNT(*) FROM PUNTOS_REDIMIDOS WHERE 1=0").exec();
                try {
                    new StaticSentence(s, "ALTER TABLE PUNTOS_REDIMIDOS ALTER COLUMN CLIENTE_ID VARCHAR(255)").exec();
                    System.out.println("✅ Migración: PUNTOS_REDIMIDOS.CLIENTE_ID alterado a VARCHAR(255)");
                } catch (BasicException ex) {
                    // Silencioso si falla o ya está aplicado
                }
            } catch (BasicException tableEx) {
                // Silencioso, no existe la tabla aún (se creará con VARCHAR(255) cuando sea necesario)
            }
        } catch (BasicException e) {
            // Silencioso si hay error al agregar
        }
    }
}