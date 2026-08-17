package com.example.maquinawebmongo.service;

import com.example.maquinawebmongo.model.Usuario;
import com.example.maquinawebmongo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import jakarta.annotation.PostConstruct;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    @Value("${app.url}")
    private String appUrl;
    
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final ZoneId ZONA_MEXICO = ZoneId.of("America/Mexico_City");

    UsuarioService(UsuarioRepository usuarioRepository, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        
        // ✅ Normalizar rol: asegurar prefijo ROLE_
        String rol = usuario.getRol();
        if (rol == null || rol.isEmpty()) {
            rol = "ROLE_USUARIO";
        } else if (!rol.startsWith("ROLE_")) {
            rol = "ROLE_" + rol;
        }
        
        List<org.springframework.security.core.GrantedAuthority> authorities = 
            java.util.Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(rol)
            );
        
        return new org.springframework.security.core.userdetails.User(
            usuario.getUsername(),
            usuario.getPassword(),
            authorities
        );
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }
    
    // ==================== REGISTRO CON VERIFICACIÓN ====================
    
    public Usuario registrarUsuario(Usuario usuario, String passwordSinEncriptar) {
        System.out.println("=== REGISTRANDO USUARIO ===");
        System.out.println("Username: " + usuario.getUsername());
        System.out.println("Email: " + usuario.getEmail());
        
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        String token = UUID.randomUUID().toString();
        usuario.setTokenVerificacion(token);
        usuario.setEmailVerificado(false);
        usuario.setEnabled(true);
        // ✅ Usar zona horaria de México
        usuario.setFechaEnvioVerificacion(LocalDateTime.now(ZONA_MEXICO));
        usuario.setFechaRegistro(LocalDateTime.now(ZONA_MEXICO));

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        System.out.println("Usuario guardado con ID: " + usuarioGuardado.getId());
        System.out.println("Token generado: " + token);
        
        enviarCorreoVerificacion(usuarioGuardado, passwordSinEncriptar);
        return usuarioGuardado;
    }
    
    public boolean enviarCorreoVerificacion(Usuario usuario, String passwordSinEncriptar) {
        try {
            System.out.println("=== ENVIANDO CORREO DE VERIFICACIÓN ===");
            System.out.println("Destino: " + usuario.getEmail());
            System.out.println("Token: " + usuario.getTokenVerificacion());
            
            String urlVerificacion = appUrl + "/verificar-correo?token=" + usuario.getTokenVerificacion();
            String asunto = "🔐 Verifica tu correo electrónico - OmniView";
            String mensaje = "Hola " + usuario.getNombreCompleto() + ",\n\n" +
                             "Has sido registrado en OmniView por el administrador.\n\n" +
                             "Para poder iniciar sesión, por favor verifica tu correo haciendo clic en el siguiente enlace:\n\n" +
                             urlVerificacion + "\n\n" +
                             "Tus credenciales de acceso son:\n" +
                             "👤 Usuario: " + usuario.getUsername() + "\n" +
                             "🔑 Contraseña: " + passwordSinEncriptar + "\n\n" +
                             "⚠️ Por seguridad, te recomendamos cambiar tu contraseña después de iniciar sesión.\n\n" +
                             "Si no solicitaste este registro, ignora este mensaje.\n\n" +
                             "Saludos,\n" +
                             "Equipo OmniView";
            
            System.out.println("Mensaje: " + mensaje);
            boolean enviado = emailService.enviarCorreo(usuario.getEmail(), asunto, mensaje);
            if (enviado) {
                System.out.println("✅ Correo enviado exitosamente");
                return true;
            }
            System.err.println("❌ No se pudo enviar el correo de verificación");
            return false;
        } catch (Exception e) {
            System.err.println("❌ ERROR en enviarCorreoVerificacion: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean verificarCorreo(String token) {
        System.out.println("🔍 VERIFICANDO TOKEN: " + token);
        Optional<Usuario> usuarioOpt = usuarioRepository.findByTokenVerificacion(token);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            System.out.println("✅ Usuario encontrado: " + usuario.getUsername());
            
            if (usuario.getFechaEnvioVerificacion() != null) {
                long horas = java.time.Duration.between(
                    usuario.getFechaEnvioVerificacion(), 
                    LocalDateTime.now(ZONA_MEXICO)
                ).toHours();
                System.out.println("⏰ Horas desde envío: " + horas);
                if (horas > 24) {
                    System.out.println("❌ Token expirado");
                    return false;
                }
            }
            
            usuario.setEmailVerificado(true);
            usuario.setTokenVerificacion(null);
            usuarioRepository.save(usuario);
            System.out.println("✅ Correo verificado exitosamente para: " + usuario.getUsername());
            return true;
        }
        System.out.println("❌ Token no encontrado: " + token);
        return false;
    }
    
    public boolean estaVerificado(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        return usuarioOpt.map(Usuario::isEmailVerificado).orElse(false);
    }
    
    public boolean existeUsuario(String username) {
        return usuarioRepository.existsByUsername(username);
    }
    
    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }
    
    @PostConstruct
    public void crearAdminSiNoExiste() {
        // ✅ ACTUALIZAR TODOS LOS USUARIOS CON ROL SIN PREFIJO A ROLE_
        List<Usuario> todosLosUsuarios = usuarioRepository.findAll();
        int actualizados = 0;
        
        for (Usuario u : todosLosUsuarios) {
            String rol = u.getRol();
            if (rol != null) {
                if ("ADMIN".equals(rol) || "admin".equalsIgnoreCase(rol)) {
                    u.setRol("ROLE_ADMIN");
                    usuarioRepository.save(u);
                    actualizados++;
                    System.out.println("✅ Usuario '" + u.getUsername() + "' actualizado a ROLE_ADMIN");
                } else if ("USUARIO".equals(rol) || "usuario".equalsIgnoreCase(rol)) {
                    u.setRol("ROLE_USUARIO");
                    usuarioRepository.save(u);
                    actualizados++;
                    System.out.println("✅ Usuario '" + u.getUsername() + "' actualizado a ROLE_USUARIO");
                }
            }
        }
        
        if (actualizados > 0) {
            System.out.println("✅ Se actualizaron " + actualizados + " usuarios al formato ROLE_");
        }
        
        // Crear admin si no existe
        Usuario adminExistente = usuarioRepository.findByUsername("admin").orElse(null);
        if (adminExistente == null) {
            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setNombreCompleto("Administrador del Sistema");
            admin.setEmail("gabrielglucuena@gmail.com");
            admin.setRol("ROLE_ADMIN");
            admin.setEmailVerificado(true);
            admin.setEnabled(true);
            admin.setSeccionesAcceso(null);
            admin.setFechaRegistro(LocalDateTime.now(ZONA_MEXICO));
            usuarioRepository.save(admin);
            System.out.println("✅ Usuario ADMIN creado: admin / 123456");
        }
        
        // ✅ Crear usuario de prueba con ROLE_USUARIO
        if (usuarioRepository.countByRol("ROLE_USUARIO") == 0) {
            Usuario user = new Usuario();
            user.setUsername("usuario");
            user.setPassword(passwordEncoder.encode("usuario123"));
            user.setNombreCompleto("Usuario Normal");
            user.setEmail("usuario@omniview.com");
            user.setRol("ROLE_USUARIO");
            user.setEmailVerificado(true);
            user.setEnabled(true);
            user.setFechaRegistro(LocalDateTime.now(ZONA_MEXICO));
            usuarioRepository.save(user);
            System.out.println("✅ Usuario USUARIO creado: usuario / usuario123");
        }
    }
    
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(String id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    public long contarUsuarios() {
        return usuarioRepository.count();
    }

    public long contarPorRol(String rol) {
        return usuarioRepository.countByRol(rol);
    }

    public Usuario actualizarUsuario(Usuario usuario) {
        if (usuario.getPassword() != null && !usuario.getPassword().startsWith("$2a$")) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    public void eliminarUsuario(String id) {
        usuarioRepository.deleteById(id);
    }

    // ==================== RECUPERACIÓN DE CONTRASEÑA ====================

    public boolean enviarRecuperacionPassword(String email) {
        System.out.println("🔍 Buscando usuario con email: " + email);
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        
        if (usuarioOpt.isPresent() && usuarioOpt.get().isEmailVerificado()) {
            Usuario usuario = usuarioOpt.get();
            System.out.println("✅ Usuario encontrado: " + usuario.getUsername());
            
            String token = UUID.randomUUID().toString();
            usuario.setTokenVerificacion(token);
            usuario.setFechaEnvioVerificacion(LocalDateTime.now(ZONA_MEXICO));
            usuarioRepository.save(usuario);
            
            String url = appUrl + "/reset-password?token=" + token;
            String asunto = "🔑 Recuperación de contraseña - OmniView";
            String mensaje = "Hola " + usuario.getNombreCompleto() + ",\n\n" +
                             "Hemos recibido una solicitud para restablecer tu contraseña.\n\n" +
                             "Haz clic en el siguiente enlace para crear una nueva contraseña:\n" +
                             url + "\n\n" +
                             "Si no solicitaste esto, ignora este mensaje.\n\n" +
                             "El enlace expirará en 24 horas.\n\n" +
                             "Saludos,\n" +
                             "Equipo OmniView";
            boolean enviado = emailService.enviarCorreo(usuario.getEmail(), asunto, mensaje);
            System.out.println(enviado ? "✅ Correo de recuperación enviado a: " + usuario.getEmail() : "❌ No se pudo enviar el correo de recuperación");
            return enviado;
        }
        
        System.out.println("❌ Usuario no encontrado o no verificado: " + email);
        return false;
    }

    public boolean resetPassword(String token, String nuevaPassword) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByTokenVerificacion(token);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (usuario.getFechaEnvioVerificacion() != null) {
                long horas = java.time.Duration.between(
                    usuario.getFechaEnvioVerificacion(), 
                    LocalDateTime.now(ZONA_MEXICO)
                ).toHours();
                if (horas > 24) return false;
            }
            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
            usuario.setTokenVerificacion(null);
            usuarioRepository.save(usuario);
            return true;
        }
        return false;
    }

    public boolean reenviarCorreoVerificacion(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isPresent() && !usuarioOpt.get().isEmailVerificado()) {
            Usuario usuario = usuarioOpt.get();
            System.out.println("📧 Reenviando correo de verificación a: " + email);
            
            String token = UUID.randomUUID().toString();
            usuario.setTokenVerificacion(token);
            usuario.setFechaEnvioVerificacion(LocalDateTime.now(ZONA_MEXICO));
            usuarioRepository.save(usuario);
            
            return enviarCorreoReenvioVerificacion(usuario);
        } else {
            System.out.println("❌ Usuario no encontrado o ya verificado: " + email);
            return false;
        }
    }

    public boolean enviarCorreoReenvioVerificacion(Usuario usuario) {
        try {
            System.out.println("=== REENVIANDO CORREO DE VERIFICACIÓN ===");
            System.out.println("Destino: " + usuario.getEmail());
            System.out.println("Token: " + usuario.getTokenVerificacion());
            
            String urlVerificacion = appUrl + "/verificar-correo?token=" + usuario.getTokenVerificacion();
            String asunto = "🔐 Verifica tu correo electrónico - OmniView";
            String mensaje = "Hola " + usuario.getNombreCompleto() + ",\n\n" +
                             "Has solicitado reenviar el correo de verificación.\n\n" +
                             "Para poder iniciar sesión, por favor verifica tu correo haciendo clic en el siguiente enlace:\n\n" +
                             urlVerificacion + "\n\n" +
                             "⚠️ Si no solicitaste este reenvío, ignora este mensaje.\n\n" +
                             "Saludos,\n" +
                             "Equipo OmniView";
            
            boolean enviado = emailService.enviarCorreo(usuario.getEmail(), asunto, mensaje);
            if (enviado) {
                System.out.println("✅ Correo de reenvío enviado exitosamente a: " + usuario.getEmail());
                return true;
            }
            System.err.println("❌ No se pudo enviar el correo de reenvío");
            return false;
        } catch (Exception e) {
            System.err.println("❌ ERROR en reenviarCorreoVerificacion: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==================== VALIDACIÓN DE CORREO ====================
    private static final List<String> DOMINIOS_VALIDOS = Arrays.asList(
        "gmail.com", "hotmail.com", "outlook.com", "yahoo.com", 
        "utez.edu.mx", "utez.mx", "email.com", "outlook.es"
    );

    public boolean esCorreoValido(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        email = email.trim().toLowerCase();
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!email.matches(emailRegex)) {
            return false;
        }
        String dominio = email.substring(email.indexOf("@") + 1).toLowerCase();
        // ✅ ELIMINADA la restricción que prohibía dominios propios
        if (!dominio.contains(".")) {
            return false;
        }
        return true;
    }
}