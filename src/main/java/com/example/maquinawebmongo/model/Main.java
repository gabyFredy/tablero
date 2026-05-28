package com.example.maquinawebmongo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "main") // La colección base, tú filtras por el campo "seccion"
public class Main {

    @Id
    private String id;
    private String seccion; // Aquí guardas: seccion_tema
    private String titulo;
    private String programa;
    private String tiempo;
    private Double porcentaje;

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
                '}';
    }
}