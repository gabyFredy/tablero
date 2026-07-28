package com.example.maquinawebmongo.controller;

import com.example.maquinawebmongo.model.Notificacion;
import com.example.maquinawebmongo.model.Usuario;
import com.example.maquinawebmongo.service.NotificacionService;
import com.example.maquinawebmongo.service.UsuarioService;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final UsuarioService usuarioService;

    NotificacionController(NotificacionService notificacionService, UsuarioService usuarioService) {
        this.notificacionService = notificacionService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<List<Notificacion>> getNoLeidas(Authentication auth) {
        String username = auth.getName();
        System.out.println("🔍 Usuario autenticado: " + username);
        
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorUsername(username);
        if (usuarioOpt.isEmpty()) {
            System.out.println("❌ Usuario no encontrado: " + username);
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        Usuario usuario = usuarioOpt.get();
        String usuarioId = usuario.getId();
        System.out.println("🔍 Usuario ID: " + usuarioId);
        
        List<Notificacion> notificaciones = notificacionService.getNoLeidas(usuarioId);
        System.out.println("📩 Notificaciones encontradas: " + notificaciones.size());
        
        return ResponseEntity.ok(notificaciones);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount(Authentication auth) {
        String username = auth.getName();
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorUsername(username);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("count", 0L));
        }
        String usuarioId = usuarioOpt.get().getId();
        long count = notificacionService.contarNoLeidas(usuarioId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/marcar/{id}")
    public ResponseEntity<Map<String, Boolean>> marcarLeida(@PathVariable String id) {
        notificacionService.marcarComoLeida(id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/marcar-todas")
    public ResponseEntity<Map<String, Boolean>> marcarTodas(Authentication auth) {
        String username = auth.getName();
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorUsername(username);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false));
        }
        String usuarioId = usuarioOpt.get().getId();
        notificacionService.marcarTodasComoLeidas(usuarioId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    
}