package com.example.maquinawebmongo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Document(collection = "usuarios")
public class Usuario implements UserDetails {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String username;
    
    @Indexed(unique = true)
    private String email;
    
    private String password;
    private String nombreCompleto;
    private String rol;  // "ADMIN" o "USUARIO"
    private boolean enabled = true;
    private LocalDateTime fechaRegistro;

    private boolean emailVerificado = false;
    private String tokenVerificacion;
    private LocalDateTime fechaEnvioVerificacion;

    private List<String> seccionesAcceso; // Lista de secciones a las que el usuario tiene acceso
    
    public Usuario() {}
    
    public Usuario(String username, String password, String nombreCompleto, String email, String rol) {
        this.username = username;
        this.password = password;
        this.nombreCompleto = nombreCompleto;
        this.email = email;
        this.rol = rol;
        this.fechaRegistro = LocalDateTime.now();
        this.emailVerificado = false;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }
    
    @Override
    public String getUsername() { return username; }
    
    @Override
    public String getPassword() { return password; }
    
    @Override
    public boolean isAccountNonExpired() { return true; }
    
    @Override
    public boolean isAccountNonLocked() { return true; }
    
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    
    @Override
    public boolean isEnabled() { return enabled && emailVerificado; }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public boolean isEmailVerificado() { return emailVerificado; }
    public void setEmailVerificado(boolean emailVerificado) { this.emailVerificado = emailVerificado; }
    public String getTokenVerificacion() { return tokenVerificacion; }
    public void setTokenVerificacion(String tokenVerificacion) { this.tokenVerificacion = tokenVerificacion; }
    public LocalDateTime getFechaEnvioVerificacion() { return fechaEnvioVerificacion; }
    public void setFechaEnvioVerificacion(LocalDateTime fechaEnvioVerificacion) { this.fechaEnvioVerificacion = fechaEnvioVerificacion; }
    public List<String> getSeccionesAcceso() { return seccionesAcceso; }
    public void setSeccionesAcceso(List<String> seccionesAcceso) { this.seccionesAcceso = seccionesAcceso; }

    @Override
    public String toString() {
        return "Usuario{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", nombreCompleto='" + nombreCompleto + '\'' +
                ", rol='" + rol + '\'' +
                ", enabled=" + enabled +
                ", fechaRegistro=" + fechaRegistro +
                ", emailVerificado=" + emailVerificado +
                ", tokenVerificacion='" + tokenVerificacion + '\'' +
                ", fechaEnvioVerificacion=" + fechaEnvioVerificacion +
                ", seccionesAcceso=" + seccionesAcceso +
                '}';
    }
}