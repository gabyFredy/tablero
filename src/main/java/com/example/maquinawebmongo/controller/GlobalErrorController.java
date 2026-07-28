package com.example.maquinawebmongo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


 @ControllerAdvice
    public class GlobalErrorController {

        @ExceptionHandler(Exception.class)
        public String manejarErrores(Exception ex, RedirectAttributes ra) {
            ra.addFlashAttribute("error", "❌ Acceso denegado o recurso no disponible");
            return "redirect:/inicio";
        }

        @GetMapping("/error")
        public String paginaError(HttpServletRequest request, RedirectAttributes ra) {
            Integer status = (Integer) request.getAttribute("javax.servlet.error.status_code");
            if (status == 403 || status == 999) {
                ra.addFlashAttribute("error", "⛔ No tienes permisos para ver esta sección");
            } else {
                ra.addFlashAttribute("error", "❌ Ocurrió un error al cargar la página");
            }
            return "redirect:/inicio";
        }
    }

