package com.example.maquinawebmongo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
//import java.util.Date;

//import org.springframework.cglib.core.Local;
import org.springframework.data.annotation.CreatedDate;


@Document(collection = "main") // La colección base, tú filtras por el campo "seccion"
public class Main {

    @CreatedDate
    private LocalDateTime fechaCreacion;

    @Id
    private String id;
    private String seccion; // Aquí guardas: seccion_tema
    private String titulo;
    private String programa;
    private String tiempo;
    private Double porcentaje;
    private LocalDateTime fechaUltimaEdicion; // Nuevo campo para la fecha de última actualización
    private boolean notificado = false;
    private boolean notificacionEnviada = false; // Nuevo campo para indicar si la notificación ya fue enviada

    private transient boolean puedeEditar; // Campo temporal para la vista, no se guarda en MongoDB

    // Constructores
    public Main() {}

    // Getters y Setters OBLIGATORIOS para que funcione @ModelAttribute
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSeccion() { return seccion; }
    public void setSeccion(String seccion) { this.seccion = seccion; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getPrograma() { return programa; }
    public void setPrograma(String programa) { this.programa = programa; }

    public String getTiempo() { return tiempo; }
    public void setTiempo(String tiempo) { this.tiempo = tiempo; }

    public Double getPorcentaje() { return porcentaje; }
    public void setPorcentaje(Double porcentaje) { this.porcentaje = porcentaje; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public boolean isPuedeEditar() { return puedeEditar; }
    public void setPuedeEditar(boolean puedeEditar) { this.puedeEditar = puedeEditar; }

    public LocalDateTime getFechaUltimaEdicion() { return fechaUltimaEdicion; }
    public void setFechaUltimaEdicion(LocalDateTime fechaUltimaEdicion) { this.fechaUltimaEdicion = fechaUltimaEdicion; }  

    public boolean isNotificado() { return notificado; }
    public void setNotificado(boolean notificado) { this.notificado = notificado; }  

    public boolean isNotificacionEnviada() { return notificacionEnviada; }
    public void setNotificacionEnviada(boolean notificacionEnviada) { this.notificacionEnviada = notificacionEnviada; }

    // ToString
    @Override
    public String toString() {
        return "Main{" +
                "id='" + id + '\'' +
                ", seccion='" + seccion + '\'' +
                ", titulo='" + titulo + '\'' +
                ", programa='" + programa + '\'' +
                ", tiempo='" + tiempo + '\'' +
                ", porcentaje=" + porcentaje +
                ", fechaCreacion=" + fechaCreacion +
                ", puedeEditar=" + puedeEditar +
                ", fechaUltimaEdicion=" + fechaUltimaEdicion +
                ", notificado=" + notificado +
                ", notificacionEnviada=" + notificacionEnviada +
                '}';
    }
}