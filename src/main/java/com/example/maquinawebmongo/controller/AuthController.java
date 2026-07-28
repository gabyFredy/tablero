package com.example.maquinawebmongo.controller;

import com.example.maquinawebmongo.model.Usuario;
import com.example.maquinawebmongo.service.UsuarioService;
import com.example.maquinawebmongo.service.EmailService;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    private final EmailService emailService;

    AuthController(UsuarioService usuarioService, EmailService emailService) {
        this.usuarioService = usuarioService;
        this.emailService = emailService;
    }

    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "noVerificado", required = false) String noVerificado,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
        }
        if (logout != null) {
            model.addAttribute("mensaje", "Sesión cerrada correctamente");
        }
        if (noVerificado != null) {
            model.addAttribute("error", "❌ Tu correo no ha sido verificado. Revisa tu bandeja de entrada.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, RedirectAttributes ra) {
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorUsername(username);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (!usuario.isEmailVerificado()) {
                ra.addFlashAttribute("error", "❌ Tu correo no ha sido verificado. Revisa tu bandeja de entrada.");
                usuarioService.reenviarCorreoVerificacion(usuario.getEmail());
                return "redirect:/login?noVerificado=true";
            }
        }
        return "redirect:/inicio";
    }

    @GetMapping("/registro")
    @PreAuthorize("hasRole('ADMIN')")
    public String registroForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    // ✅ CORREGIDO: Guardar la contraseña antes de encriptarla
    @PostMapping("/registro")
    @PreAuthorize("hasRole('ADMIN')")
    public String registrarUsuario(Usuario usuario,
                                   @RequestParam String confirmarPassword,
                                   RedirectAttributes ra) {
        try {
            String username = usuario.getUsername() == null ? "" : usuario.getUsername().trim();
            String email = usuario.getEmail() == null ? "" : usuario.getEmail().trim();
            String password = usuario.getPassword() == null ? "" : usuario.getPassword();

            if (!usuarioService.esCorreoValido(usuario.getEmail())){
                ra.addFlashAttribute("error", "El correo no es valido. Usa un correo real (Gmail, Hotmail...)");
                return "redirect:/registro";
            }

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                ra.addFlashAttribute("error", "Todos los campos son obligatorios.");
                return "redirect:/registro";
            }

            if (password.length() < 6) {
                ra.addFlashAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
                return "redirect:/registro";
            }

            if (!password.equals(confirmarPassword)) {
                ra.addFlashAttribute("error", "Las contraseñas no coinciden.");
                return "redirect:/registro";
            }

            if (usuarioService.existeUsuario(username)) {
                ra.addFlashAttribute("error", "El nombre de usuario ya existe.");
                return "redirect:/registro";
            }

            if (usuarioService.existeEmail(email)) {
                ra.addFlashAttribute("error", "El correo ya está registrado.");
                return "redirect:/registro";
            }

            usuario.setUsername(username);
            usuario.setEmail(email);
            usuario.setRol("USUARIO");
            
            String passwordSinEncriptar = password;
            usuarioService.registrarUsuario(usuario, passwordSinEncriptar);

            ra.addFlashAttribute("mensaje", "✅ Usuario creado correctamente. El administrador puede seguir registrando usuarios desde el panel.");
            return "redirect:/admin/usuarios";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error al registrar: " + e.getMessage());
            return "redirect:/registro";
        }
    }

    @GetMapping("/verificar-correo")
    public String verificarCorreo(@RequestParam String token, RedirectAttributes ra) {
        boolean verificado = usuarioService.verificarCorreo(token);
        if (verificado) {
            ra.addFlashAttribute("mensaje", "✅ ¡Correo verificado exitosamente! Ahora puedes iniciar sesión.");
        } else {
            ra.addFlashAttribute("error", "❌ Token inválido o expirado. Contacta al administrador.");
        }
        return "redirect:/login";
    }

    @GetMapping("/recuperar-password")
    public String recuperarPasswordForm() {
        return "recuperar-password";
    }

    @PostMapping("/recuperar-password")
    public String recuperarPassword(@RequestParam String email, RedirectAttributes ra) {
        boolean enviado = usuarioService.enviarRecuperacionPassword(email);
        if (enviado) {
            ra.addFlashAttribute("mensaje", "✅ Se ha enviado un enlace de recuperación a tu correo.");
        } else {
            ra.addFlashAttribute("error", "❌ El correo no está registrado o no está verificado.");
        }
        return "redirect:/login";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String password,
                                @RequestParam String confirmarPassword,
                                RedirectAttributes ra) {
        if (!password.equals(confirmarPassword)) {
            ra.addFlashAttribute("error", "Las contraseñas no coinciden");
            return "redirect:/reset-password?token=" + token;
        }
        if (password.length() < 6) {
            ra.addFlashAttribute("error", "La contraseña debe tener al menos 6 caracteres");
            return "redirect:/reset-password?token=" + token;
        }
        boolean restablecido = usuarioService.resetPassword(token, password);
        if (restablecido) {
            ra.addFlashAttribute("mensaje", "✅ Contraseña actualizada correctamente.");
        } else {
            ra.addFlashAttribute("error", "❌ Token inválido o expirado.");
        }
        return "redirect:/login";
    }

    @GetMapping("/reenviar-verificacion")
    public String reenviarVerificacion(@RequestParam String email, RedirectAttributes ra) {
        boolean enviado = usuarioService.reenviarCorreoVerificacion(email);
        if (enviado) {
            ra.addFlashAttribute("mensaje", "✅ Se ha reenviado el correo de verificación. Revisa tu bandeja de entrada.");
        } else {
            ra.addFlashAttribute("error", "❌ No se pudo reenviar el correo. Revisa la configuración de SMTP o que el correo del usuario sea correcto.");
        }
        return "redirect:/login";
    }

    @GetMapping("/test-email")
    @ResponseBody
    public String testEmail(@RequestParam String email) {
        try {
            System.out.println("=== TEST EMAIL ===");
            String asunto = "🧪 Correo de prueba - OmniView";
            String mensaje = "Este es un correo de prueba para verificar que la configuración de correo funciona correctamente.";
            boolean enviado = emailService.enviarCorreo(email, asunto, mensaje);
            return enviado ? "✅ Correo enviado a: " + email : "❌ No se pudo enviar el correo a: " + email;
        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }
}