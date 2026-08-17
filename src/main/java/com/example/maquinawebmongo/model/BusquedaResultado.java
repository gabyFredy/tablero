package com.example.maquinawebmongo.model;

public class BusquedaResultado {
    private String id;
    private String titulo;
    private String seccion;
    private String programa;
    private Double porcentaje;
    private String colorSeleccionado;
    private String url;
    private String seccionNombre;
    
    // Constructores
    public BusquedaResultado() {}
    
    public BusquedaResultado(String id, String titulo, String seccion, String programa, 
                            Double porcentaje, String colorSeleccionado, String url) {
        this.id = id;
        this.titulo = titulo;
        this.seccion = seccion;
        this.programa = programa;
        this.porcentaje = porcentaje;
        this.colorSeleccionado = colorSeleccionado;
        this.url = url;
        this.seccionNombre = obtenerNombreSeccion(seccion);
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    
    public String getSeccion() { return seccion; }
    public void setSeccion(String seccion) { this.seccion = seccion; }
    
    public String getPrograma() { return programa; }
    public void setPrograma(String programa) { this.programa = programa; }
    
    public Double getPorcentaje() { return porcentaje; }
    public void setPorcentaje(Double porcentaje) { this.porcentaje = porcentaje; }
    
    public String getColorSeleccionado() { return colorSeleccionado; }
    public void setColorSeleccionado(String colorSeleccionado) { this.colorSeleccionado = colorSeleccionado; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getSeccionNombre() { return seccionNombre; }
    public void setSeccionNombre(String seccionNombre) { this.seccionNombre = seccionNombre; }
    
    // Método auxiliar para obtener el nombre de la sección
    private String obtenerNombreSeccion(String seccionCompleta) {
        if (seccionCompleta == null) return "Sin sección";
        String seccion = seccionCompleta.contains("_") ? 
                         seccionCompleta.split("_")[0] : seccionCompleta;
        return switch (seccion) {
            case "presidencia" -> "Presidencia Municipal";
            case "sindicatura" -> "Sindicatura";
            case "regidores" -> "Regidores";
            case "ayuntamiento" -> "Ayuntamiento";
            case "administracion" -> "Administración";
            case "tesoreria" -> "Tesorería";
            case "contraloria" -> "Contraloría";
            case "seprac" -> "SEPRAC";
            case "obras" -> "Obras Públicas";
            case "sustentable" -> "Desarrollo Sustentable";
            case "turismo" -> "Turismo";
            case "humano" -> "Recursos Humanos";
            case "consejeria" -> "Consejería";
            case "dif" -> "DIF";
            case "sapac" -> "SAPAC";
            default -> seccion.substring(0, 1).toUpperCase() + seccion.substring(1);
        };
    }
}