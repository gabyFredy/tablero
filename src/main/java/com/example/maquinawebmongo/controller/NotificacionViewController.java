package com.example.maquinawebmongo.controller;

import com.example.maquinawebmongo.model.Notificacion;
import com.example.maquinawebmongo.model.Usuario;
import com.example.maquinawebmongo.service.NotificacionService;
import com.example.maquinawebmongo.service.UsuarioService;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/notificaciones")
public class NotificacionViewController {

    private final NotificacionService notificacionService;

    private final UsuarioService usuarioService;

    NotificacionViewController(NotificacionService notificacionService, UsuarioService usuarioService) {
        this.notificacionService = notificacionService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/todas")
    public String verTodasNotificaciones(Authentication auth, Model model) {
        String username = auth.getName();
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorUsername(username);
        
        if (usuarioOpt.isPresent()) {
            String usuarioId = usuarioOpt.get().getId();
            
            List<Notificacion> todas = notificacionService.getTodas(usuarioId);
            long noLeidas = notificacionService.contarNoLeidas(usuarioId);
            
            model.addAttribute("notificaciones", todas);
            model.addAttribute("noLeidas", noLeidas);
            model.addAttribute("total", todas.size());
        }
        
        return "notificaciones/todas";
    }
}