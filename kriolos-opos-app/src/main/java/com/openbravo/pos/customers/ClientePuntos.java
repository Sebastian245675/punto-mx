/**
 * Sebastian - Clase para manejar los puntos de los clientes
 */
package com.openbravo.pos.customers;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.DataRead;
import com.openbravo.data.loader.DataWrite;
import com.openbravo.data.loader.SerializerRead;
import com.openbravo.data.loader.SerializerWrite;
import java.util.Date;
import java.util.UUID;

public class ClientePuntos {
    
    private String id;
    private String clienteId;
    private int puntosActuales;
    private int puntosTotales;
    private String ultimaTransaccion;
    private Date fechaUltimaTransaccion;
    private Date fechaCreacion;
    
    // Constructor vacío
    public ClientePuntos() {
        this.id = UUID.randomUUID().toString();
        this.puntosActuales = 0;
        this.puntosTotales = 0;
        this.fechaCreacion = new Date();
        this.fechaUltimaTransaccion = new Date();
    }
    
    // Constructor con cliente ID
    public ClientePuntos(String clienteId) {
        this();
        this.clienteId = clienteId;
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    
    public int getPuntosActuales() { return puntosActuales; }
    public void setPuntosActuales(int puntosActuales) { this.puntosActuales = puntosActuales; }
    
    public int getPuntosTotales() { return puntosTotales; }
    public void setPuntosTotales(int puntosTotales) { this.puntosTotales = puntosTotales; }
    
    public String getUltimaTransaccion() { return ultimaTransaccion; }
    public void setUltimaTransaccion(String ultimaTransaccion) { this.ultimaTransaccion = ultimaTransaccion; }
    
    public Date getFechaUltimaTransaccion() { return fechaUltimaTransaccion; }
    public void setFechaUltimaTransaccion(Date fechaUltimaTransaccion) { this.fechaUltimaTransaccion = fechaUltimaTransaccion; }
    
    public Date getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Date fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    
    /**
     * Agrega puntos al cliente
     */
    public void agregarPuntos(int puntos, String descripcion) {
        this.puntosActuales += puntos;
        this.puntosTotales += puntos;
        this.ultimaTransaccion = descripcion;
        this.fechaUltimaTransaccion = new Date();
    }
    
    /**
     * Usa puntos del cliente
     */
    public boolean usarPuntos(int puntos, String descripcion) {
        if (this.puntosActuales >= puntos) {
            this.puntosActuales -= puntos;
            this.ultimaTransaccion = descripcion;
            this.fechaUltimaTransaccion = new Date();
            return true;
        }
        return false;
    }
    
    /**
     * Serializer para leer desde la base de datos
     */
    public static SerializerRead getSerializerRead() {
        return new SerializerRead() {
            @Override
            public Object readValues(DataRead dr) throws BasicException {
                ClientePuntos puntos = new ClientePuntos();
                puntos.id = dr.getString(1);
                puntos.clienteId = dr.getString(2);
                puntos.puntosActuales = dr.getInt(3);
                puntos.puntosTotales = dr.getInt(4);
                puntos.ultimaTransaccion = dr.getString(5);
                puntos.fechaUltimaTransaccion = dr.getTimestamp(6);
                puntos.fechaCreacion = dr.getTimestamp(7);
                return puntos;
            }
        };
    }
    
    /**
     * Serializer para escribir a la base de datos
     */
    public static SerializerWrite getSerializerWrite() {
        return new SerializerWrite() {
            @Override
            public void writeValues(DataWrite dp, Object obj) throws BasicException {
                ClientePuntos puntos = (ClientePuntos) obj;
                dp.setString(1, puntos.getId());
                dp.setString(2, puntos.getClienteId());
                dp.setInt(3, puntos.getPuntosActuales());
                dp.setInt(4, puntos.getPuntosTotales());
                dp.setString(5, puntos.getUltimaTransaccion());
                dp.setTimestamp(6, puntos.getFechaUltimaTransaccion());
                dp.setTimestamp(7, puntos.getFechaCreacion());
            }
        };
    }
    
    @Override
    public String toString() {
        return String.format("ClientePuntos[cliente=%s, actuales=%d, totales=%d]", 
                           clienteId, puntosActuales, puntosTotales);
    }
}