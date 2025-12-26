/**
 * Sebastian - Clase para manejar la configuración del sistema de puntos
 */
package com.openbravo.pos.customers;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.DataRead;
import com.openbravo.data.loader.DataWrite;
import com.openbravo.data.loader.SerializerRead;
import com.openbravo.data.loader.SerializerWrite;
import java.util.Date;
import java.util.UUID;

public class PuntosConfiguracion {
    
    // Configuración del sistema
    private String id;
    private double montoPorPunto;  // Cuánto dinero = 1 punto (ej: 700.000 MX = 50 puntos)
    private int puntosOtorgados;   // Cuántos puntos se otorgan por el monto
    private boolean sistemaActivo;
    private String moneda;
    private int limiteDiarioPuntos; // Sebastian - Límite máximo de puntos por día
    private Date fechaCreacion;
    private Date fechaActualizacion;
    
    // Constructor vacío
    public PuntosConfiguracion() {
        this.id = UUID.randomUUID().toString();
        this.montoPorPunto = 700000.0; // 700,000 MX por defecto
        this.puntosOtorgados = 50;     // 50 puntos por defecto
        this.sistemaActivo = true;
        this.moneda = "MX";
        this.limiteDiarioPuntos = 500; // Sebastian - 500 puntos por día por defecto
        this.fechaCreacion = new Date();
        this.fechaActualizacion = new Date();
    }
    
    // Constructor con parámetros
    public PuntosConfiguracion(double montoPorPunto, int puntosOtorgados, String moneda) {
        this();
        this.montoPorPunto = montoPorPunto;
        this.puntosOtorgados = puntosOtorgados;
        this.moneda = moneda;
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public double getMontoPorPunto() { return montoPorPunto; }
    public void setMontoPorPunto(double montoPorPunto) { 
        this.montoPorPunto = montoPorPunto; 
        this.fechaActualizacion = new Date();
    }
    
    public int getPuntosOtorgados() { return puntosOtorgados; }
    public void setPuntosOtorgados(int puntosOtorgados) { 
        this.puntosOtorgados = puntosOtorgados; 
        this.fechaActualizacion = new Date();
    }
    
    public boolean isSistemaActivo() { return sistemaActivo; }
    public void setSistemaActivo(boolean sistemaActivo) { 
        this.sistemaActivo = sistemaActivo; 
        this.fechaActualizacion = new Date();
    }
    
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    
    // Sebastian - Getter y setter para límite diario de puntos
    public int getLimiteDiarioPuntos() { return limiteDiarioPuntos; }
    public void setLimiteDiarioPuntos(int limiteDiarioPuntos) { 
        this.limiteDiarioPuntos = limiteDiarioPuntos; 
        this.fechaActualizacion = new Date();
    }
    
    public Date getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Date fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    
    public Date getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(Date fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
    
    /**
     * Calcula cuántos puntos corresponden a un monto dado
     * Solo otorga puntos completos cuando se alcanza cada umbral completo
     * No otorga fracciones de puntos
     * Ejemplo: Si montoPorPunto = $800 y puntosOtorgados = 10
     * $0-799 = 0 puntos, $800-1599 = 10 puntos, $1600-2399 = 20 puntos, etc.
     * 
     * IMPORTANTE: Solo se otorgan puntos cuando se alcanza cada umbral completo.
     * Si el monto es $1200 y el umbral es $800, solo se otorgan 10 puntos (1 umbral completo),
     * no 15 puntos proporcionales.
     */
    public int calcularPuntos(double monto) {
        if (!sistemaActivo || montoPorPunto <= 0) {
            return 0;
        }
        
        // Si el monto es menor al monto requerido, no otorga puntos
        if (monto < montoPorPunto) {
            return 0;
        }
        
        // Calcular cuántos tramos completos se han alcanzado
        // Ejemplo: monto = 1200, montoPorPunto = 800
        // tramosCompletos = floor(1200/800) = floor(1.5) = 1
        // puntos = 1 * 10 = 10 puntos (solo 1 umbral completo)
        int tramosCompletos = (int) Math.floor(monto / montoPorPunto);
        
        // Los puntos son el número de tramos completos multiplicado por los puntos otorgados
        return tramosCompletos * puntosOtorgados;
    }
    
    /**
     * Serializer para leer desde la base de datos
     */
    public static SerializerRead<PuntosConfiguracion> getSerializerRead() {
        return new SerializerRead<PuntosConfiguracion>() {
            @Override
            public PuntosConfiguracion readValues(DataRead dr) throws BasicException {
                PuntosConfiguracion config = new PuntosConfiguracion();
                config.id = dr.getString(1);
                config.montoPorPunto = dr.getDouble(2);
                config.puntosOtorgados = dr.getInt(3);
                config.sistemaActivo = dr.getBoolean(4);
                config.moneda = dr.getString(5);
                config.limiteDiarioPuntos = dr.getInt(6); // Sebastian - Nuevo campo
                config.fechaCreacion = dr.getTimestamp(7);
                config.fechaActualizacion = dr.getTimestamp(8);
                return config;
            }
        };
    }
    
    /**
     * Serializer para escribir a la base de datos
     */
    public static SerializerWrite<PuntosConfiguracion> getSerializerWrite() {
        return new SerializerWrite<PuntosConfiguracion>() {
            @Override
            public void writeValues(DataWrite dp, PuntosConfiguracion config) throws BasicException {
                dp.setString(1, config.getId());
                dp.setDouble(2, config.getMontoPorPunto());
                dp.setInt(3, config.getPuntosOtorgados());
                dp.setBoolean(4, config.isSistemaActivo());
                dp.setString(5, config.getMoneda());
                dp.setInt(6, config.getLimiteDiarioPuntos()); // Sebastian - Nuevo campo
                dp.setTimestamp(7, config.getFechaCreacion());
                dp.setTimestamp(8, config.getFechaActualizacion());
            }
        };
    }
    
    @Override
    public String toString() {
        return String.format("PuntosConfig[%s %.2f=%d puntos, activo=%s]", 
                           moneda, montoPorPunto, puntosOtorgados, sistemaActivo);
    }
}