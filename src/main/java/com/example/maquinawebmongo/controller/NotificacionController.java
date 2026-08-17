package com.example.maquinawebmongo.controller;

import com.example.maquinawebmongo.model.Notificacion;
import com.example.maquinawebmongo.model.Usuario;
import com.example.maquinawebmongo.service.NotificacionService;
import com.example.maquinawebmongo.service.UsuarioService;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

// ✅ CAMBIADO: @RestController → @Controller (para manejar vistas y JSON)
@Controller
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final UsuarioService usuarioService;

    NotificacionController(NotificacionService notificacionService, UsuarioService usuarioService) {
        this.notificacionService = notificacionService;
        this.usuarioService = usuarioService;
    }

    // ==================== API REST (JSON) ====================
    
    @GetMapping("/no-leidas")
    @ResponseBody  // ✅ Agregado para devolver JSON
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
    @ResponseBody  // ✅ Agregado para devolver JSON
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
    @ResponseBody  // ✅ Agregado para devolver JSON
    public ResponseEntity<Map<String, Boolean>> marcarLeida(@PathVariable String id) {
        notificacionService.marcarComoLeida(id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/marcar-todas")
    @ResponseBody  // ✅ Agregado para devolver JSON
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

    // ==================== VISTA HTML ====================
    
    @GetMapping("/todas")
    public String verTodasNotificaciones(Authentication auth, Model model) {
        System.out.println("🔍 Entrando a /api/notificaciones/todas");
        
        try {
            String username = auth.getName();
            System.out.println("🔍 Usuario: " + username);
            
            Optional<Usuario> usuarioOpt = usuarioService.buscarPorUsername(username);
            
            if (usuarioOpt.isPresent()) {
                String usuarioId = usuarioOpt.get().getId();
                System.out.println("🔍 Usuario ID: " + usuarioId);
                
                // Obtener TODAS las notificaciones (leídas y no leídas)
                List<Notificacion> todas = notificacionService.getTodas(usuarioId);
                long noLeidas = notificacionService.contarNoLeidas(usuarioId);
                
                System.out.println("📩 Notificaciones encontradas: " + (todas != null ? todas.size() : 0));
                
                model.addAttribute("notificaciones", todas != null ? todas : new ArrayList<>());
                model.addAttribute("noLeidas", noLeidas);
                model.addAttribute("total", todas != null ? todas.size() : 0);
            } else {
                System.out.println("❌ Usuario no encontrado");
                model.addAttribute("notificaciones", new ArrayList<>());
                model.addAttribute("noLeidas", 0);
                model.addAttribute("total", 0);
            }
            
            return "notificaciones/todas";
            
        } catch (Exception e) {
            System.out.println("❌ ERROR en /api/notificaciones/todas: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("notificaciones", new ArrayList<>());
            model.addAttribute("noLeidas", 0);
            model.addAttribute("total", 0);
            return "notificaciones/todas";
        }
    }
}