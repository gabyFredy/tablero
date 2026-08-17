package com.example.maquinawebmongo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.AccessDeniedException;

@ControllerAdvice
public class GlobalErrorController {

    // ✅ Manejar TODAS las excepciones pero con control
    @ExceptionHandler(Exception.class)
    public String manejarErrores(Exception ex, RedirectAttributes ra) {
        // ✅ Imprimir en consola para saber qué está pasando
        System.out.println("🔍 Excepción capturada: " + ex.getClass().getName());
        System.out.println("🔍 Mensaje: " + ex.getMessage());
        
        // ✅ Si es una excepción de seguridad, NO mostrar mensaje
        if (ex instanceof AccessDeniedException || 
            ex instanceof org.springframework.security.core.AuthenticationException) {
            return "redirect:/inicio";
        }
        
        // ✅ Si es una excepción de navegación, NO mostrar mensaje
        if (ex instanceof org.springframework.web.servlet.NoHandlerFoundException ||
            ex instanceof org.springframework.web.bind.MissingServletRequestParameterException ||
            ex instanceof org.springframework.web.servlet.resource.NoResourceFoundException) {
            return "redirect:/inicio";
        }
        
        // ✅ Solo mostrar mensaje si es un error REAL
        System.err.println("❌ ERROR: " + ex.getMessage());
        ex.printStackTrace();
        ra.addFlashAttribute("error", "❌ Ocurrió un error en el sistema");
        return "redirect:/inicio";
    }

    @GetMapping("/error")
    public String paginaError(HttpServletRequest request, RedirectAttributes ra) {
        Integer status = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        
        // ✅ No mostrar mensaje al recargar (solo errores reales)
        if (status == 403 || status == 404 || status == 500) {
            // No agregar mensaje para que no persista
            return "redirect:/inicio";
        }
        return "redirect:/inicio";
    }
}