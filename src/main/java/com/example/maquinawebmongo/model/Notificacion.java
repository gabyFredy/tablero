package com.example.maquinawebmongo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "notificaciones")
public class Notificacion {
    
    @Id
    private String id;
    private String usuarioId;
    private String titulo;
    private String mensaje;
    private String tipo;  // INFO, WARNING, SUCCESS
    private String urlRelacionada;
    private boolean leida;
    private LocalDateTime fechaCreacion;
    private String registroId;  // ID del indicador relacionado
    private String tipoNotificacion;
    
    public Notificacion() {}
    
    public Notificacion(String usuarioId, String titulo, String mensaje, String tipo, String urlRelacionada, String registroId) {
        this.usuarioId = usuarioId;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.urlRelacionada = urlRelacionada;
        this.registroId = registroId;
        this.leida = false;
        this.fechaCreacion = LocalDateTime.now();
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getUrlRelacionada() { return urlRelacionada; }
    public void setUrlRelacionada(String urlRelacionada) { this.urlRelacionada = urlRelacionada; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getRegistroId() { return registroId; }
    public void setRegistroId(String registroId) { this.registroId = registroId; }
    public String getTipoNotificacion() { return tipoNotificacion; }
    public void setTipoNotificacion(String tipoNotificacion) { this.tipoNotificacion = tipoNotificacion; }
}