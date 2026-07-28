package com.example.maquinawebmongo.service;

import com.example.maquinawebmongo.model.Notificacion;
import com.example.maquinawebmongo.model.Usuario;
import com.example.maquinawebmongo.repository.NotificacionRepository;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    
    private final EmailService emailService;
    
    private final UsuarioService usuarioService;

    NotificacionService(NotificacionRepository notificacionRepository, EmailService emailService, UsuarioService usuarioService) {
        this.notificacionRepository = notificacionRepository;
        this.emailService = emailService;
        this.usuarioService = usuarioService;
    }

    public Notificacion crearNotificacion(Notificacion notificacion) {
        return notificacionRepository.save(notificacion);
    }

    public void notificarEdicionDisponible(String registroId, String titulo, String url) {
        List<Usuario> usuarios = usuarioService.listarTodos();

        for (Usuario usuario : usuarios) {
            if (!usuario.isEmailVerificado()) {
                continue;
            }

            Notificacion notificacion = new Notificacion(
                usuario.getId(),
                "📝 Indicador disponible para edición",
                "El indicador '" + titulo + "' ya puede ser modificado. Haz clic para editarlo.",
                "SUCCESS",
                url,
                registroId
            );
            notificacionRepository.save(notificacion);

            if (usuario.getEmail() != null && !usuario.getEmail().isEmpty()) {
                String asunto = "🔔 OmniView - Indicador disponible para edición";
                String mensaje = "Hola " + usuario.getNombreCompleto() + ",\n\n" +
                                 "El indicador '" + titulo + "' ya puede ser modificado.\n" +
                                 "Accede al siguiente enlace para editarlo:\n" +
                                 url + "\n\n" +
                                 "Saludos,\n" +
                                 "Equipo OmniView";
                emailService.enviarCorreo(usuario.getEmail(), asunto, mensaje);
            }
        }
    }

    public List<Notificacion> getNoLeidas(String usuarioId) {
        return notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(usuarioId);
    }

    public List<Notificacion> getTodas(String usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    public long contarNoLeidas(String usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    public void marcarComoLeida(String notificacionId) {
        notificacionRepository.findById(notificacionId).ifPresent(n -> {
            n.setLeida(true);
            notificacionRepository.save(n);
        });
    }

    public void marcarTodasComoLeidas(String usuarioId) {
        List<Notificacion> noLeidas = notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(usuarioId);
        noLeidas.forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(noLeidas);
    }
}