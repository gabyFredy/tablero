package com.example.maquinawebmongo.controller;

import com.example.maquinawebmongo.model.Usuario;
import com.example.maquinawebmongo.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // ==================== VER PERFIL ====================
    @GetMapping
    public String verPerfil(Model model, Authentication auth) {
        if (auth == null) {
            return "redirect:/login";
        }

        String username = auth.getName();
        Usuario usuario = usuarioService.buscarPorUsername(username).orElse(null);

        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("username", usuario.getUsername());
        model.addAttribute("nombreCompleto", usuario.getNombreCompleto());
        model.addAttribute("email", usuario.getEmail());
        model.addAttribute("roles", auth.getAuthorities());
        model.addAttribute("fechaRegistro", usuario.getFechaRegistro());

        return "perfil";
    }

    // ==================== FORMULARIO CAMBIAR CONTRASEÑA ====================
    @GetMapping("/cambiar-contraseña")
    public String cambiarPasswordForm(Model model, Authentication auth) {
        if (auth == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", auth.getName());
        return "cambiar-contraseña";
    }

    // ==================== PROCESAR CAMBIO DE CONTRASEÑA ====================
    @PostMapping("/cambiar-contraseña")
    public String cambiarPassword(@RequestParam String passwordActual,
                                  @RequestParam String passwordNueva,
                                  @RequestParam String confirmarPassword,
                                  RedirectAttributes ra,
                                  Authentication auth) {
        try {
            // Validar autenticación
            if (auth == null) {
                ra.addFlashAttribute("error", "❌ Debes iniciar sesión para cambiar tu contraseña");
                return "redirect:/login";
            }

            String username = auth.getName();
            Usuario usuario = usuarioService.buscarPorUsername(username).orElse(null);

            if (usuario == null) {
                ra.addFlashAttribute("error", "❌ Usuario no encontrado");
                return "redirect:/perfil";
            }

            // 1. Validar que la contraseña actual sea correcta
            if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
                ra.addFlashAttribute("error", "❌ La contraseña actual es incorrecta");
                return "redirect:/perfil/cambiar-contraseña";
            }

            // 2. Validar que la nueva contraseña tenga mínimo 6 caracteres
            if (passwordNueva.length() < 6) {
                ra.addFlashAttribute("error", "❌ La nueva contraseña debe tener al menos 6 caracteres");
                return "redirect:/perfil/cambiar-contraseña";
            }

            // 3. Validar que las contraseñas coincidan
            if (!passwordNueva.equals(confirmarPassword)) {
                ra.addFlashAttribute("error", "❌ Las contraseñas no coinciden");
                return "redirect:/perfil/cambiar-contraseña";
            }

            // 4. Validar que la nueva contraseña no sea igual a la actual
            if (passwordEncoder.matches(passwordNueva, usuario.getPassword())) {
                ra.addFlashAttribute("error", "❌ La nueva contraseña debe ser diferente a la actual");
                return "redirect:/perfil/cambiar-contraseña";
            }

            // 5. Guardar la nueva contraseña
            usuario.setPassword(passwordEncoder.encode(passwordNueva));
            usuarioService.actualizarUsuario(usuario);

            ra.addFlashAttribute("mensaje", "✅ Contraseña actualizada exitosamente");
            return "redirect:/perfil";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error al cambiar la contraseña: " + e.getMessage());
            return "redirect:/perfil/cambiar-contraseña";
        }
    }
}