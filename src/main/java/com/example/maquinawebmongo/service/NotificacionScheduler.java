package com.example.maquinawebmongo.service;

import com.example.maquinawebmongo.model.Main;
import com.example.maquinawebmongo.model.Notificacion;
import com.example.maquinawebmongo.model.Usuario;
import com.example.maquinawebmongo.repository.MainRepository;
import com.example.maquinawebmongo.repository.NotificacionRepository;
import com.example.maquinawebmongo.repository.UsuarioRepository;
import com.example.maquinawebmongo.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;  // ✅ AGREGADO
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;  // ✅ CAMBIADO
import java.time.ZoneId;        // ✅ AGREGADO
import java.util.List;
import java.util.Optional;

@Component
public class NotificacionScheduler {

    @Autowired
    private MainRepository mainRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EmailService emailService;

    @Value("${app.url}")  // ✅ USAMOS LA VARIABLE CONFIGURADA
    private String appUrl;

    @Scheduled(fixedDelay = 30000)
    public void verificarIndicadoresDisponibles() {
        System.out.println("🔄 Verificando indicadores disponibles para edición...");
        
        List<Main> todos = mainRepository.findAll();
        LocalDateTime ahora = LocalDateTime.now();  // ✅ CAMBIADO
        int notificacionesEnviadas = 0;

        for (Main item : todos) {
            boolean puedeEditar = calcularPuedeEditar(item, ahora);
            
            if (puedeEditar && !item.isNotificacionEnviada()) {
                List<Usuario> usuarios = usuarioService.listarTodos();
                    
                    for (Usuario usuario : usuarios) {
                        if (usuario.isEmailVerificado() && usuario.getEmail() != null && !usuario.getEmail().contains("omniview.com")) {
                            String url = "/" + item.getSeccion().replace("_", "/") + "/editar/" + item.getId();

                            Notificacion notificacion = new Notificacion(
                                usuario.getId(),
                                "📝 Indicador disponible para edición",
                                "El indicador '" + item.getTitulo() + " " + item.getPrograma() + "' ya puede ser modificado.",
                                "SUCCESS",
                                url,
                                item.getId()
                            );
                            notificacion.setTipoNotificacion("EDICION_DISPONIBLE");
                            notificacionRepository.save(notificacion);
                            
                            String asunto = "🔔 OmniView - Indicador disponible para edición";
                            String mensaje = "Hola " + usuario.getNombreCompleto() + ",\n\n" +
                                             "El indicador '" + item.getTitulo() + "' ya puede ser modificado.\n" +
                                             "Del programa '" + item.getPrograma() + "'.\n" +   
                                             "Accede al siguiente enlace para editarlo:\n" +
                                             appUrl + url + "\n\n" +  // ✅ USAMOS VARIABLE
                                             "Saludos,\n" +
                                             "Equipo OmniView";
                            boolean enviado = emailService.enviarCorreo(usuario.getEmail(), asunto, mensaje);
                            if (enviado){
                                System.out.println("✅ Correo enviado a: " + usuario.getEmail());
                            } else {
                                System.err.println("❌ No se pudo enviar el correo a: " + usuario.getEmail());
                            }
                        }
                    }

                    item.setNotificacionEnviada(true);
                    item.setNotificado(true);
                    mainRepository.save(item);

                    notificacionesEnviadas++;
                    System.out.println("📧 Notificación enviada para: " + item.getTitulo());
                
            }
        }
        
        if (notificacionesEnviadas > 0) {
            System.out.println("✅ Se enviaron " + notificacionesEnviadas + " notificaciones.");
        }
    }

    public void reiniciarNotificacion(String registroId) {
        Optional<Main> mainOpt = mainRepository.findById(registroId);
        if (mainOpt.isPresent()) {
            Main main = mainOpt.get();
            main.setNotificado(false);
            mainRepository.save(main);
            System.out.println("✅ Notificación reiniciada para: " + main.getTitulo());
        }
    }

    private boolean calcularPuedeEditar(Main item, LocalDateTime ahora) {  // ✅ CAMBIADO
        if (item.getFechaCreacion() == null || item.getTiempo() == null) {
            return false;
        }

        LocalDateTime fechaBase = item.getFechaUltimaEdicion() != null 
            ? item.getFechaUltimaEdicion() 
            : item.getFechaCreacion();

        long diffEnMillis = java.time.Duration.between(fechaBase, ahora).toMillis();
        long diffEnDias = diffEnMillis / (1000 * 60 * 60 * 24);
        
        switch (item.getTiempo()) {
            case "Minuto": return diffEnMillis >= 60 * 1000;
            case "Hora": return diffEnMillis >= 60 * 60 * 1000;
            case "Diario": return diffEnDias >= 1;
            case "Semanal": return diffEnDias >= 7;
            case "Quincenal": return diffEnDias >= 15;
            case "Mensual": return diffEnDias >= 30;
            case "Bimestral": return diffEnDias >= 60;
            case "Trimestral": return diffEnDias >= 90;
            case "Cuatrimestral": return diffEnDias >= 120;
            case "Semestral": return diffEnDias >= 180;
            case "Anual": return diffEnDias >= 365;
            default: return false;
        }
    }
}