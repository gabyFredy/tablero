package com.example.maquinawebmongo.controller;

import com.example.maquinawebmongo.model.Usuario;
import com.example.maquinawebmongo.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioAdminController {

    private final UsuarioService usuarioService;

    UsuarioAdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // ==================== LISTAR SECCIONES DISPONIBLES ====================
    private Map<String, String> getSeccionesDisponibles() {
        Map<String, String> secciones = new LinkedHashMap<>();
        secciones.put("presidencia", "Presidencia Municipal");
        secciones.put("sindicatura", "Sindicatura");
        secciones.put("regidores", "Regidores");
        secciones.put("ayuntamiento", "Ayuntamiento");
        secciones.put("administracion", "Administración");
        secciones.put("tesoreria", "Tesorería");
        secciones.put("contraloria", "Contraloría");
        secciones.put("seprac", "SEPRAC");
        secciones.put("obras", "Obras Públicas");
        secciones.put("sustentable", "Desarrollo Sustentable");
        secciones.put("turismo", "Turismo");
        secciones.put("humano", "Recursos Humanos");
        secciones.put("consejeria", "Consejería");
        secciones.put("dif", "DIF");
        secciones.put("sapac", "SAPAC");
        return secciones;
    }

    private boolean esAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return false;
    
    return auth.getAuthorities().stream()
        .anyMatch(g -> {
            String authority = g.getAuthority();
            return "ROLE_ADMIN".equals(authority) || 
                   "ADMIN".equals(authority);
        });
    }

    // ==================== LISTAR USUARIOS ====================
    @GetMapping
    public String listarUsuarios(Model model, Authentication auth) {
        // Verificar si el usuario es admin
        if (!esAdmin()) {
            return "redirect:/inicio";
        }
        String username = auth.getName();
        System.out.println("🔍 Accediendo a /admin/usuarios con usuario: " + username);

        Optional<Usuario> usuarioOptional = usuarioService.buscarPorUsername(username);
        if (usuarioOptional.isPresent()) {
            Usuario usuario = usuarioOptional.get();
            System.out.println("🔍 Rol en UsuarioAdminController: " + usuario.getRol());
            
            // ✅ NORMALIZAR ROL ANTES DE COMPARAR
            String rolUsuario = usuario.getRol();
            if (rolUsuario != null && !rolUsuario.startsWith("ROLE_")) {
                rolUsuario = "ROLE_" + rolUsuario;
            }
            if (!"ROLE_ADMIN".equals(rolUsuario)) {
                System.out.println("❌ No es ADMIN, redirigiendo...");
                return "redirect:/inicio";
            }
            System.out.println("✅ Es ADMIN, mostrando lista de usuarios");
        }
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("totalUsuarios", usuarioService.contarUsuarios());
        model.addAttribute("totalAdmins", usuarioService.contarPorRol("ROLE_ADMIN"));
        model.addAttribute("totalUsuariosNormales", usuarioService.contarPorRol("ROLE_USUARIO"));
        return "admin/usuarios";
    }

    // Formulario para nuevo usuario
    @GetMapping("/nuevo")
    public String nuevoUsuarioForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("titulo", "Nuevo Usuario");
        model.addAttribute("accion", "Crear");
        model.addAttribute("seccionesDisponibles", getSeccionesDisponibles());
        return "admin/usuario-form";
    }

    // Guardar usuario
    // ==================== GUARDAR USUARIO ====================
    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario,
                                @RequestParam String confirmarPassword,
                                @RequestParam(required = false) List<String> seccionesAcceso,
                                RedirectAttributes ra) {
        try {
            // ✅ NORMALIZAR ROL: Si es "ADMIN", convertirlo a "ROLE_ADMIN"
            if (usuario.getRol() != null && usuario.getRol().equals("ADMIN")) {
                usuario.setRol("ROLE_ADMIN");
            }
            
            // ✅ Recortar espacios del correo
            if (usuario.getEmail() != null) {
                usuario.setEmail(usuario.getEmail().trim().toLowerCase());
            }

            // 1. Validar correo
            if (!usuarioService.esCorreoValido(usuario.getEmail())) {
                ra.addFlashAttribute("error", "❌ El correo electrónico no es válido. Usa un correo real (Gmail, Hotmail, UTEZ, etc.)");
                return "redirect:/admin/usuarios/nuevo";
            }

            // 2. Validar usuario existente
            if (usuarioService.existeUsuario(usuario.getUsername())) {
                ra.addFlashAttribute("error", "❌ El nombre de usuario ya existe");
                return "redirect:/admin/usuarios/nuevo";
            }

            // 3. Validar email existente
            if (usuarioService.existeEmail(usuario.getEmail())) {
                ra.addFlashAttribute("error", "❌ El correo electrónico ya está registrado");
                return "redirect:/admin/usuarios/nuevo";
            }

            // 4. Validar contraseñas
            if (!usuario.getPassword().equals(confirmarPassword)) {
                ra.addFlashAttribute("error", "❌ Las contraseñas no coinciden");
                return "redirect:/admin/usuarios/nuevo";
            }

            // 5. Validar secciones (solo si es ADMIN)
            if ("ROLE_ADMIN".equals(usuario.getRol())) {
                usuario.setSeccionesAcceso(null);
            } else {
                if (seccionesAcceso == null || seccionesAcceso.isEmpty()) {
                    ra.addFlashAttribute("error", "❌ El usuario debe tener al menos una sección asignada");
                    return "redirect:/admin/usuarios/nuevo";
                }
                usuario.setSeccionesAcceso(seccionesAcceso);
            }

            // 6. Guardar usuario
            String passwordSinEncriptar = usuario.getPassword();
            Usuario usuarioCreado = usuarioService.registrarUsuario(usuario, passwordSinEncriptar);

            ra.addFlashAttribute("mensaje", "✅ Usuario creado exitosamente. Se ha enviado un correo de verificación a " + usuarioCreado.getEmail());
            return "redirect:/admin/usuarios";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error al crear usuario: " + e.getMessage());
            return "redirect:/admin/usuarios/nuevo";
        }
    }

    // Formulario para editar usuario
    @GetMapping("/editar/{id}")
    public String editarUsuarioForm(@PathVariable String id, Model model, RedirectAttributes ra) {
        try {
            Usuario usuario = usuarioService.buscarPorId(id);
            if (usuario == null) {
                ra.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/admin/usuarios";
            }
            model.addAttribute("usuario", usuario);
            model.addAttribute("titulo", "Editar Usuario");
            model.addAttribute("accion", "Editar");
            model.addAttribute("seccionesDisponibles", getSeccionesDisponibles());
            return "admin/usuario-form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al cargar usuario");
            return "redirect:/admin/usuarios";
        }
    }

    // ==================== ACTUALIZAR USUARIO ====================
    @PostMapping("/actualizar/{id}")
    public String actualizarUsuario(@PathVariable String id,
                                    @ModelAttribute Usuario usuarioActualizado,
                                    @RequestParam(required = false) String nuevaPassword,
                                    @RequestParam(required = false) String confirmarPassword,
                                    @RequestParam(required = false) List<String> seccionesAcceso,
                                    RedirectAttributes ra,
                                    HttpSession session) {
        try {
            Usuario usuario = usuarioService.buscarPorId(id);
            if (usuario == null) {
                ra.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/admin/usuarios";
            }

            // ✅ NORMALIZAR ROL: Si es "ADMIN", convertirlo a "ROLE_ADMIN"
            if (usuarioActualizado.getRol() != null && usuarioActualizado.getRol().equals("ADMIN")) {
                usuarioActualizado.setRol("ROLE_ADMIN");
            }

            // ✅ Recortar espacios del correo
            if (usuarioActualizado.getEmail() != null) {
                usuarioActualizado.setEmail(usuarioActualizado.getEmail().trim().toLowerCase());
            }

            // Actualizar campos
            usuario.setNombreCompleto(usuarioActualizado.getNombreCompleto());
            usuario.setEmail(usuarioActualizado.getEmail());
            usuario.setRol(usuarioActualizado.getRol());
            usuario.setEnabled(usuarioActualizado.isEnabled());

            if ("ROLE_ADMIN".equals(usuario.getRol())) {
                usuario.setSeccionesAcceso(null);
            } else {
                if (seccionesAcceso == null || seccionesAcceso.isEmpty()) {
                    ra.addFlashAttribute("error", "El usuario debe tener al menos una sección asignada");
                    return "redirect:/admin/usuarios/editar/" + id;
                }
                usuario.setSeccionesAcceso(seccionesAcceso);
            }

            // Actualizar contraseña solo si se llenó
            if (nuevaPassword != null && !nuevaPassword.isBlank()) {
                if (!nuevaPassword.equals(confirmarPassword)) {
                    ra.addFlashAttribute("error", "Las contraseñas no coinciden");
                    return "redirect:/admin/usuarios/editar/" + id;
                }
                usuario.setPassword(nuevaPassword);
            }

            usuarioService.actualizarUsuario(usuario);

            // Cerrar sesión si el usuario cambió sus propios datos
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName().equals(usuario.getUsername())) {
                session.invalidate();
                ra.addFlashAttribute("mensaje", "✅ Usuario actualizado. Por favor, vuelve a iniciar sesión.");
                return "redirect:/login";
            }

            ra.addFlashAttribute("mensaje", "✅ Usuario actualizado exitosamente");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error al actualizar: " + e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    // Eliminar usuario
    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable String id, RedirectAttributes ra) {
        try {
            Usuario usuario = usuarioService.buscarPorId(id);
            if (usuario == null) {
                ra.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/admin/usuarios";
            }

            // No permitir eliminar al último admin
            if ("ROLE_ADMIN".equals(usuario.getRol()) && usuarioService.contarPorRol("ROLE_ADMIN") <= 1) {
                ra.addFlashAttribute("error", "No se puede eliminar al único administrador del sistema");
                return "redirect:/admin/usuarios";
            }

            usuarioService.eliminarUsuario(id);
            ra.addFlashAttribute("mensaje", "✅ Usuario eliminado exitosamente");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error al eliminar: " + e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    // ==================== ACTIVAR/DESACTIVAR USUARIO ====================
    @PostMapping("/toggle/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleUsuario(@PathVariable String id) {
        try {
            Usuario usuario = usuarioService.buscarPorId(id);
            if (usuario == null) {
                return ResponseEntity.notFound().build();
            }

            // No permitir desactivar al último administrador
            if ("ROLE_ADMIN".equals(usuario.getRol()) && usuarioService.contarPorRol("ROLE_ADMIN") <= 1) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "No se puede desactivar al único administrador del sistema");
                return ResponseEntity.badRequest().body(response);
            }

            // Cambiar estado
            usuario.setEnabled(!usuario.isEnabled());
            usuarioService.actualizarUsuario(usuario);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("enabled", usuario.isEnabled());
            response.put("mensaje", usuario.isEnabled() ? "Usuario activado" : "Usuario desactivado");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}