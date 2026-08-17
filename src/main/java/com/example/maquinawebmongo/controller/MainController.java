package com.example.maquinawebmongo.controller;

import com.example.maquinawebmongo.model.BusquedaResultado;
import com.example.maquinawebmongo.model.GraficaData;
import com.example.maquinawebmongo.model.Main;
import com.example.maquinawebmongo.model.Usuario;
import com.example.maquinawebmongo.repository.MainRepository;
import com.example.maquinawebmongo.service.NotificacionService;
import com.example.maquinawebmongo.repository.NotificacionRepository;
import com.example.maquinawebmongo.service.UsuarioService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Duration;
import java.util.Arrays;

@Controller
public class MainController {

    // ✅ Zona horaria fija para México
    private static final ZoneId ZONA_MEXICO = ZoneId.of("America/Mexico_City");

    private final MainRepository mainRepository;
    private final NotificacionService notificacionService;
    private final NotificacionRepository notificacionRepository;
    private final UsuarioService usuarioService;

    public MainController(MainRepository mainRepository, NotificacionService notificacionService, 
                          NotificacionRepository notificacionRepository, UsuarioService usuarioService) {
        this.mainRepository = mainRepository;
        this.notificacionService = notificacionService;
        this.notificacionRepository = notificacionRepository;
        this.usuarioService = usuarioService;
    }

    private boolean esAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return false;
    
    // ✅ Verificar autoridades (funciona para ROLE_ADMIN y ADMIN)
    return auth.getAuthorities().stream()
        .anyMatch(g -> {
            String authority = g.getAuthority();
            // ✅ Acepta ROLE_ADMIN, ADMIN, o cualquier cosa que termine en ADMIN
            return "ROLE_ADMIN".equals(authority) || 
                   "ADMIN".equals(authority) ||
                   authority.endsWith("ADMIN");
        });
    }

    private Double formatearPorcentaje(Double valor) {
        if (valor == null) return 0.0;
        return Math.round(valor * 100.0) / 100.0;
    }

    @GetMapping({"/", "/inicio"})
    public String inicio(Model model, Authentication auth) {
        if (auth != null) {
            model.addAttribute("username", auth.getName());
            model.addAttribute("esAdmin", esAdmin());
            model.addAttribute("roles", auth.getAuthorities());
            System.out.println("🔍 Usuario en inicio: " + auth.getName());
            System.out.println("🔍 ¿Es admin? " + esAdmin());
            System.out.println("🔍 Authorities: " + auth.getAuthorities());
        } else {
            model.addAttribute("username", "Invitado");
            model.addAttribute("esAdmin", false);
        }
        
        List<Main> todos = mainRepository.findAll();
        model.addAttribute("totalIndicadores", todos.size());
        
        Map<String, Integer> conteoPorSeccion = new HashMap<>();
        for (Main item : todos) {
            String seccion = item.getSeccion();
            if (seccion != null) {
                String[] partes = seccion.split("_");
                String seccionNombre = partes.length > 0 ? partes[0] : "otras";
                conteoPorSeccion.put(seccionNombre, conteoPorSeccion.getOrDefault(seccionNombre, 0) + 1);
            }
        }
        model.addAttribute("conteoPorSeccion", conteoPorSeccion);
        
        return "inicio";
    }

    
    @GetMapping("/{seccion}/dashboard")
    public String dashboard(@PathVariable String seccion, Model model, Authentication auth, RedirectAttributes ra) {
        if (!tieneAccesoASeccion(seccion, auth)) {
            ra.addFlashAttribute("error", "Acceso denegado - No tienes acceso a esta sección");
            return "redirect:/inicio";
        }
        model.addAttribute("seccion", seccion);
        model.addAttribute("seccionNombre", getNombreSeccion(seccion));
        return seccion + "/dashboard";
    }

    @GetMapping("/debug/ver-programas")
    @ResponseBody
    public String verProgramas() {
        if(!esAdmin()) return "NO AUTORIZADO";
        
        List<Main> todos = mainRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><h2>Registros en BD</h2><ul>");
        for (Main item : todos) {
            sb.append("<li>");
            sb.append("<strong>Título:</strong> ").append(item.getTitulo()).append("<br>");
            sb.append("<strong>Programa:</strong> '").append(item.getPrograma()).append("'<br>");
            sb.append("<strong>Color:</strong> '").append(item.getColorSeleccionado()).append("'<br>");
            sb.append("</li><hr>");
        }
        sb.append("</ul></body></html>");
        return sb.toString();
    }

    @GetMapping("/debug/actualizar-programas")
    @ResponseBody
    public String actualizarProgramas() {
        if(!esAdmin()) return "NO AUTORIZADO";

        List<Main> todos = mainRepository.findAll();
        int actualizados = 0;
        
        for (Main item : todos) {
            if (item.getPrograma() == null || item.getPrograma().isEmpty()) {
                String seccion = item.getSeccion();
                String tema = "";
                if (seccion != null && seccion.contains("_")) {
                    tema = seccion.split("_")[1];
                }
                String nombrePrograma = getNombreTema(tema);
                item.setPrograma(nombrePrograma);
                mainRepository.save(item);
                actualizados++;
            }
        }
        
        return "Se actualizaron " + actualizados + " registros con el nombre del programa";
    }

    @GetMapping("/{seccion}/{tema}")
    public String listarTema(@PathVariable String seccion, @PathVariable String tema, 
                             Model model, Authentication auth, RedirectAttributes ra) {
        if (!tieneAccesoASeccion(seccion, auth)) {
            ra.addFlashAttribute("error", "Acceso denegado - No tienes acceso a esta sección");
            return "redirect:/inicio";
        }
        
        String coleccion = seccion + "_" + tema;
        List<Main> mainList = mainRepository.findBySeccionOrderByOrdenAsc(coleccion);
        
        boolean necesitaActualizar = false;
        for (int i = 0; i < mainList.size(); i++) {
            Main item = mainList.get(i);
            if (item.getOrden() == null || item.getOrden() == 0) {
                item.setOrden(i + 1);
                necesitaActualizar = true;
            }
        }
        if (necesitaActualizar) {
            mainRepository.saveAll(mainList);
        }

        List<Map<String, Object>> mainListProcesada = new ArrayList<>();
        for (Main item : mainList) {
            Map<String, Object> datos = new HashMap<>();
            datos.put("id", item.getId());
            datos.put("titulo", item.getTitulo());
            datos.put("tiempo", item.getTiempo());
            datos.put("porcentaje", item.getPorcentaje() != null ? item.getPorcentaje() : 0.0);
            datos.put("programa", item.getPrograma());
            datos.put("orden", item.getOrden());
            datos.put("colorSeleccionado", item.getColorSeleccionado());

            boolean puedeEditar = calcularPuedeEditar(item);
            datos.put("puedeEditar", puedeEditar);

            long fechaCreacion = 0;
            if (item.getFechaCreacion() != null) {
                fechaCreacion = item.getFechaCreacion()
                        .atZone(ZONA_MEXICO)
                        .toInstant()
                        .toEpochMilli();
            }
            datos.put("fechaCreacion", fechaCreacion);

            long fechaUltimaEdicion = 0;
            if (item.getFechaUltimaEdicion() != null) {
                fechaUltimaEdicion = item.getFechaUltimaEdicion()
                        .atZone(ZONA_MEXICO)
                        .toInstant()
                        .toEpochMilli();
            }
            datos.put("fechaUltimaEdicion", fechaUltimaEdicion);

            mainListProcesada.add(datos);
        }

        model.addAttribute("mainList", mainListProcesada);
        model.addAttribute("seccionUrl", seccion);
        model.addAttribute("temaUrl", tema);
        model.addAttribute("tituloPagina", getNombreTema(tema));
        model.addAttribute("esAdmin", esAdmin());
        model.addAttribute("fechaActual", LocalDateTime.now(ZONA_MEXICO));
        return "main/lista";
    }

    private boolean calcularPuedeEditar(Main item) {
        if (item.getFechaCreacion() == null || item.getTiempo() == null) {
            return false;
        }
        LocalDateTime ahora = LocalDateTime.now(ZONA_MEXICO);
        if (!esMesPermitido(ahora)) {
            return false;
        }
        
        if (!esDiaLaboral(ahora)) {
            return false;
        }

        LocalDateTime fechaBase = item.getFechaUltimaEdicion() != null 
            ? item.getFechaUltimaEdicion() 
            : item.getFechaCreacion();

        long diffEnDias = ChronoUnit.DAYS.between(fechaBase, ahora);

        return switch (item.getTiempo()) {
            case "Mensual" -> diffEnDias >= 30;
            case "Bimestral" -> diffEnDias >= 60;
            case "Trimestral" -> diffEnDias >= 90;
            case "Cuatrimestral" -> diffEnDias >= 120;
            case "Semestral" -> diffEnDias >= 180;
            case "Anual" -> diffEnDias >= 365;
            default -> false;
        };
    }

    private boolean esMesPermitido(LocalDateTime fecha) {
        int mes = fecha.getMonthValue();
        return mes == 2 || mes == 5 || mes == 8 || mes == 11;
    }

    private boolean esDiaLaboral(LocalDateTime fecha) {
        int diaSemana = fecha.getDayOfWeek().getValue();
        return diaSemana >= 1 && diaSemana <= 5;
    }

    @GetMapping("/{seccion}/{tema}/ver/{id}")
    public String verTema(@PathVariable String seccion, @PathVariable String tema,
                          @PathVariable String id, Model model, RedirectAttributes ra, Authentication auth) {
        if (!tieneAccesoASeccion(seccion, auth)) {
            ra.addFlashAttribute("error", "Acceso denegado - No tienes acceso a esta sección");
            return "redirect:/inicio";
        }
        Optional<Main> mainOpt = mainRepository.findById(id);
        if (mainOpt.isPresent()) {
            Main main = mainOpt.get();
            String coleccionEsperada = seccion + "_" + tema;
            if (!coleccionEsperada.equals(main.getSeccion())) {
                ra.addFlashAttribute("error", "❌ Registro no pertenece a esta sección");
                return "redirect:/" + seccion + "/" + tema;
            }
            model.addAttribute("main", main);
            model.addAttribute("seccionUrl", seccion);
            model.addAttribute("temaUrl", tema);
            return "main/detalles";
        }
        ra.addFlashAttribute("error", "❌ Registro no encontrado");
        return "redirect:/" + seccion + "/" + tema;
    }

    @GetMapping("/{seccion}/{tema}/add")
    public String addTema(@PathVariable String seccion, @PathVariable String tema, 
                          Model model, RedirectAttributes ra, Authentication auth) {
        if (!tieneAccesoASeccion(seccion, auth)) {
            ra.addFlashAttribute("error", "Acceso denegado - No tienes acceso a esta sección");
            return "redirect:/inicio";
        }
        if(!esAdmin()) {
            ra.addFlashAttribute("error", "❌ Acceso denegado: Solo administradores");
            return "redirect:/" + seccion + "/" + tema;
        }

        try {
            Main main = new Main();
            main.setSeccion(seccion + "_" + tema);
            main.setColorSeleccionado(null);
            model.addAttribute("main", main);
            model.addAttribute("accion", "Crear");
            model.addAttribute("seccionUrl", seccion);
            model.addAttribute("temaUrl", tema);
            model.addAttribute("tituloPagina", getNombreTema(tema));
            return "main/formulario";
        } catch (Exception e) {
            model.addAttribute("error", "❌ Error al cargar el formulario: " + e.getMessage());
            return "main/formulario";
        }
    }

    @PostMapping("/{seccion}/{tema}/guardar")
    public String guardarTema(@PathVariable String seccion, @PathVariable String tema,
                              @ModelAttribute Main main, RedirectAttributes ra, Authentication auth) {
        if (!tieneAccesoASeccion(seccion, auth)) {
            return "redirect:/inicio";
        }
        if(!esAdmin()) {
            ra.addFlashAttribute("error", "❌ Acceso denegado");
            return "redirect:/" + seccion + "/" + tema;
        }

        try {
            String coleccion = seccion + "_" + tema;
            main.setSeccion(coleccion);
            main.setPrograma(getNombreTema(tema));

            if (main.getId() == null || main.getId().isEmpty()) {
                main.setId(java.util.UUID.randomUUID().toString());
            }

            if (main.getFechaCreacion() == null) {
                main.setFechaCreacion(LocalDateTime.now(ZONA_MEXICO));
            }

            if (main.getOrden() == null || main.getOrden() == 0) {
                List<Main> existentes = mainRepository.findBySeccionOrderByOrdenAsc(coleccion);
                int maxOrden = existentes.stream()
                    .mapToInt(m -> m.getOrden() != null ? m.getOrden() : 0)
                    .max()
                    .orElse(0);
                main.setOrden(maxOrden + 1);
            }

            if (main.getTitulo() == null || main.getTitulo().trim().isEmpty()) {
                ra.addFlashAttribute("error", "⚠️ El título es obligatorio");
                return "redirect:/" + seccion + "/" + tema + "/add";
            }
            
            main.setPorcentaje(formatearPorcentaje(main.getPorcentaje()));

            if (main.getColorSeleccionado() != null && !main.getColorSeleccionado().isEmpty()) {
                List<String> coloresValidos = Arrays.asList("verde", "amarillo", "rojo", "gris");
                if (!coloresValidos.contains(main.getColorSeleccionado())) {
                    main.setColorSeleccionado(null);
                }
            }

            mainRepository.save(main);
            ra.addFlashAttribute("mensaje", "✅ Guardado exitoso");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error al guardar: " + e.getMessage());
        }
        return "redirect:/" + seccion + "/" + tema;
    }

    @GetMapping("/{seccion}/{tema}/editar/{id}")
    public String editarTema(@PathVariable String seccion, @PathVariable String tema,
                             @PathVariable String id, Model model, RedirectAttributes ra, Authentication auth) {
        if (!tieneAccesoASeccion(seccion, auth)) {
            return "redirect:/inicio";
        }
        if(!esAdmin()) {
            ra.addFlashAttribute("error", "❌ Acceso denegado");
            return "redirect:/" + seccion + "/" + tema;
        }

        Optional<Main> mainOpt = mainRepository.findById(id);
        if (mainOpt.isPresent()) {
            Main main = mainOpt.get();
            String coleccionEsperada = seccion + "_" + tema;
            if (!coleccionEsperada.equals(main.getSeccion())) {
                ra.addFlashAttribute("error", "❌ Registro no pertenece a esta sección");
                return "redirect:/" + seccion + "/" + tema;
            }
            model.addAttribute("main", main);
            model.addAttribute("accion", "Editar");
            model.addAttribute("seccionUrl", seccion);
            model.addAttribute("temaUrl", tema);
            model.addAttribute("tituloPagina", getNombreTema(tema));
            return "main/formulario";
        }
        ra.addFlashAttribute("error", "❌ Registro no encontrado");
        return "redirect:/" + seccion + "/" + tema;
    }

    @PostMapping("/{seccion}/{tema}/actualizar/{id}")
    public String actualizarTema(@PathVariable String seccion, @PathVariable String tema,
                                 @PathVariable String id, @ModelAttribute Main mainActualizado, 
                                 RedirectAttributes ra, Authentication auth) {
        if (!tieneAccesoASeccion(seccion, auth)) {
            return "redirect:/inicio";
        }
        if(!esAdmin()) {
            ra.addFlashAttribute("error", "❌ Acceso denegado");
            return "redirect:/" + seccion + "/" + tema;
        }

        try {
            Optional<Main> mainOpt = mainRepository.findById(id);
            if (mainOpt.isPresent()) {
                Main main = mainOpt.get();
                String coleccionEsperada = seccion + "_" + tema;
                if (!coleccionEsperada.equals(main.getSeccion())) {
                    ra.addFlashAttribute("error", "❌ No tienes permiso para modificar este registro");
                    return "redirect:/" + seccion + "/" + tema;
                }

                if (mainActualizado.getTitulo() == null || mainActualizado.getTitulo().trim().isEmpty()) {
                    ra.addFlashAttribute("error", "⚠️ El título es obligatorio");
                    return "redirect:/" + seccion + "/" + tema + "/editar/" + id;
                }

                main.setTitulo(mainActualizado.getTitulo().trim());
                main.setPrograma(getNombreTema(tema));
                main.setTiempo(mainActualizado.getTiempo() != null ? mainActualizado.getTiempo().trim() : "");
                main.setPorcentaje(formatearPorcentaje(mainActualizado.getPorcentaje()));
                main.setFechaUltimaEdicion(LocalDateTime.now(ZONA_MEXICO));
                main.setNotificacionEnviada(false);
                
                if (mainActualizado.getColorSeleccionado() != null) {
                    List<String> coloresValidos = Arrays.asList("verde", "amarillo", "rojo", "gris", "");
                    if (coloresValidos.contains(mainActualizado.getColorSeleccionado())) {
                        String color = mainActualizado.getColorSeleccionado().isEmpty() ? 
                                      null : mainActualizado.getColorSeleccionado();
                        main.setColorSeleccionado(color);
                    }
                }
                
                if (mainActualizado.getOrden() != null) {
                    main.setOrden(mainActualizado.getOrden());
                }

                mainRepository.save(main);
                ra.addFlashAttribute("mensaje", "✅ Actualizado correctamente");
            } else {
                ra.addFlashAttribute("error", "❌ Registro no encontrado");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error al actualizar: " + e.getMessage());
        }
        return "redirect:/" + seccion + "/" + tema;
    }

        @PostMapping("/{seccion}/{tema}/eliminar/{id}")
    public String eliminarTema(@PathVariable String seccion, @PathVariable String tema,
                               @PathVariable String id, RedirectAttributes ra, Authentication auth) {
        if (!tieneAccesoASeccion(seccion, auth)) {
            return "redirect:/inicio";
        }
        if(!esAdmin()) {
            ra.addFlashAttribute("error", "❌ Acceso denegado: Solo administradores pueden eliminar registros");
            return "redirect:/" + seccion + "/" + tema;
        }

        try {
            Optional<Main> mainOpt = mainRepository.findById(id);
            if (mainOpt.isPresent()) {
                Main main = mainOpt.get();
                String coleccionEsperada = seccion + "_" + tema;
                if (!coleccionEsperada.equals(main.getSeccion())) {
                    ra.addFlashAttribute("error", "❌ El registro no pertenece a esta sección");
                    return "redirect:/" + seccion + "/" + tema;
                }

                mainRepository.deleteById(id);
                ra.addFlashAttribute("mensaje", "✅ Registro eliminado exitosamente");
            } else {
                ra.addFlashAttribute("error", "❌ Registro no encontrado");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error al eliminar: " + e.getMessage());
        }
        return "redirect:/" + seccion + "/" + tema;
    }

    // ==================== BÚSQUEDA EN JSON (PARA EL BUSCADOR GLOBAL) ====================
    @GetMapping("/api/buscar")
    @ResponseBody
    public Map<String, Object> buscarJSON(@RequestParam String q, Authentication auth) {
        Map<String, Object> respuesta = new HashMap<>();
        List<Map<String, Object>> resultados = new ArrayList<>();
        
        try {
            if (q == null || q.trim().isEmpty()) {
                respuesta.put("success", false);
                respuesta.put("mensaje", "Término de búsqueda vacío");
                return respuesta;
            }
            
            String termino = q.trim().toLowerCase();
            List<Main> todos = mainRepository.findAll();
            
            for (Main item : todos) {
                if (item.getSeccion() == null) continue;
                
                // Verificar acceso del usuario
                String[] partesSeccion = item.getSeccion().split("_");
                String seccion = partesSeccion.length > 0 ? partesSeccion[0] : "";
                
                if (!tieneAccesoASeccion(seccion, auth)) {
                    continue;
                }
                
                String titulo = item.getTitulo() != null ? item.getTitulo().toLowerCase() : "";
                String programa = item.getPrograma() != null ? item.getPrograma().toLowerCase() : "";
                String seccionLower = seccion.toLowerCase();
                
                // Buscar coincidencia
                if (titulo.contains(termino) || programa.contains(termino) || seccionLower.contains(termino)) {
                    Map<String, Object> dato = new HashMap<>();
                    dato.put("id", item.getId());
                    dato.put("titulo", item.getTitulo());
                    dato.put("programa", item.getPrograma());
                    dato.put("porcentaje", item.getPorcentaje());
                    dato.put("colorSeleccionado", item.getColorSeleccionado());
                    dato.put("seccionNombre", getNombreSeccion(seccion));
                    
                    // Construir URL
                    String temaUrl = partesSeccion.length > 1 ? partesSeccion[1] : "";
                    dato.put("url", "/" + seccion + "/" + temaUrl + "/ver/" + item.getId());
                    
                    resultados.add(dato);
                }
            }
            
            respuesta.put("success", true);
            respuesta.put("resultados", resultados);
            respuesta.put("total", resultados.size());
            respuesta.put("termino", termino);
            
        } catch (Exception e) {
            respuesta.put("success", false);
            respuesta.put("mensaje", "Error en la búsqueda: " + e.getMessage());
        }
        
        return respuesta;
    }

    // ==================== GRÁFICAS ====================
   
    @GetMapping("/{seccion}/dashboard/grafica")
    public String graficaSeccion(@PathVariable String seccion, Model model, Authentication auth) {
        // Verificar Acceso
        if (!tieneAccesoASeccion(seccion, auth)) {
            return "redirect:/error?mensaje=Acceso denegado";
        }
        model.addAttribute("seccion", seccion);
        model.addAttribute("seccionNombre", getNombreSeccion(seccion));
        return "seccion/grafica-seccion";
    }

    @GetMapping("/api/{seccion}/dashboard/grafica")
    @ResponseBody
    public List<GraficaData> getGraficaSeccion(@PathVariable String seccion, Authentication auth) {
        // Verificar Acceso
        if (!tieneAccesoASeccion(seccion, auth)) {
            return new java.util.ArrayList<>();
        }
        String prefijo = seccion + "_";
        List<Main> todosLosRegistros = mainRepository.findAll();
        java.util.Set<String> temasUnicos = new java.util.HashSet<>();
        
        for (Main item : todosLosRegistros) {
            String seccionCompleta = item.getSeccion();
            if (seccionCompleta != null && seccionCompleta.startsWith(prefijo)) {
                String tema = seccionCompleta.substring(prefijo.length());
                temasUnicos.add(tema);
            }
        }
        
        List<String> temasLista = new java.util.ArrayList<>(temasUnicos);
        java.util.Collections.sort(temasLista);
        List<GraficaData> datos = new java.util.ArrayList<>();
        
        for (String tema : temasLista) {
            String coleccion = seccion + "_" + tema;
            List<Main> items = mainRepository.findBySeccion(coleccion);
            double suma = 0;
            for (Main item : items) {
                suma += item.getPorcentaje() != null ? item.getPorcentaje() : 0;
            }
            double promedio = items.isEmpty() ? 0 : suma / items.size();
            datos.add(new GraficaData(getNombreTema(tema), promedio));
        }
        return datos;
    }

    @GetMapping("/{seccion}/{tema}/grafica")
    public String graficaPrograma(@PathVariable String seccion, @PathVariable String tema, Model model, Authentication auth) {
        // Verificar Acceso
        if (!tieneAccesoASeccion(seccion, auth)) {
            return "redirect:/error?mensaje=Acceso denegado";
        }
        model.addAttribute("seccionUrl", seccion);
        model.addAttribute("temaUrl", tema);
        model.addAttribute("tituloPagina", getNombreTema(tema));
        return "main/grafica-programa";
    }

    @GetMapping("/api/{seccion}/{tema}/grafica")
    @ResponseBody
    public List<GraficaData> getGraficaPrograma(@PathVariable String seccion, @PathVariable String tema, Authentication auth) {
        // Verificar Acceso
        if (!tieneAccesoASeccion(seccion, auth)) {
            return new java.util.ArrayList<>();
        }
        String coleccion = seccion + "_" + tema;
        List<Main> items = mainRepository.findBySeccion(coleccion);
        List<GraficaData> datos = new java.util.ArrayList<>();
        for (Main item : items) {
            datos.add(new GraficaData(item.getTitulo(), item.getPorcentaje() != null ? item.getPorcentaje() : 0));
        }
        return datos;
    }

    // ==================== MÉTODOS DE APOYO ====================
    private boolean tieneAccesoASeccion(String seccion, Authentication auth) {
        if (esAdmin()) return true;
        if (auth == null) return false;

        String username = auth.getName();
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorUsername(username);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            String rol = usuario.getRol();
            
            if ("ROLE_ADMIN".equals(rol) || "ADMIN".equals(rol)) {
                return true;
            }
            
            List<String> seccionesAsignadas = usuario.getSeccionesAcceso();
            if (seccionesAsignadas != null && seccionesAsignadas.contains(seccion)) {
                return true;
            }
        }
        return false;
    }

    private String getNombreSeccion(String clave) {
        return switch (clave) {
            case "presidencia" -> "Presidencia Municipal";
            case "sindicatura" -> "Sindicatura";
            case "regidores" -> "Regidores";
            case "ayuntamiento" -> "Ayuntamiento";
            case "administracion" -> "Administración";
            case "tesoreria" -> "Tesorería";
            case "contraloria" -> "Contraloría";
            case "seprac" -> "SEPRAC";
            case "obras" -> "Obras Públicas";
            case "sustentable" -> "Desarrollo Sustentable";
            case "turismo" -> "Turismo";
            case "humano" -> "Recursos Humanos";
            case "consejeria" -> "Consejería";
            case "dif" -> "DIF";
            case "sapac" -> "SAPAC";
            default -> clave.substring(0, 1).toUpperCase() + clave.substring(1);
        };
    }

    private String getNombreTema(String tema) {
        return switch (tema) {
            case "gobierno-solidario" -> "Gobierno Solidario";
            case "comunicacion-social" -> "Comunicación Social Responsable";
            case "seguridad-publica" -> "Consejo Municipal de Seguridad Pública";
            case "planeacion" -> "Planeación y Evaluación de la Administración Municipal";
            case "atencion-mujeres" -> "Modelo de Atención Integral a Mujeres";
            case "proteccion-infantil" -> "Protección y Bienestar de Niñas, Niños y Adolescentes";
            case "representacion-legal" -> "Representación Legal del Municipio de Cuernavaca";
            case "representacion-popular" -> "Representación Popular";
            case "coordinacion-municipal" -> "Coordinación Municipal";
            case "gestion-gobierno-archivo" -> "Gestión de Gobierno y Archivo Municipal";
            case "politica-municipal" -> "Política Municipal";
            case "austeridad-consciente" -> "Austeridad Consciente y Transparente de los Recursos y Servicios Públicos";
            case "austeridad-consciente-capitalizacion" -> "Austeridad Consciente en la Capitalización de Recursos Humanos";
            case "administracion-eficaz" -> "Administración Eficaz y Transparente de los Recursos Materiales";
            case "administracion-eficiente" -> "Administración Eficiente de los Recursos Tecnológicos";
            case "fortalecimiento-financiero" -> "Fortalecimiento Financiero para el Desarrollo Municipal";
            case "recaudacion-eficiente" -> "Recaudación Eficiente y Equitativa de los Ingresos";
            case "recaudacion-impuesto-predial" -> "Recaudación Eficiente del Impuesto Predial";
            case "administracion-eficiente-ayuntamiento" -> "Administración Eficiente del Recursos del Ayuntamiento de Cuernavaca";
            case "control-supervision-representacion-popular" -> "Control y Supervisión de la Representación Pupular";
            case "combate-corrupcion" -> "Combate a la Corrupción";
            case "seguridad-auxilio-cuidadano" -> "Seguridad y Auxilio Cuidadano";
            case "planeacion-desarrollo-urbano" -> "Planeación del Desarrollo Urbano";
            case "obras-publicas" -> "Obras Públicas con Claridad al Servicio de la Ciudadanía";
            case "sostenibilidad-cuernavaca" -> "Sostenibilidad en Cuernavaca";
            case "presentacion-servicios-publicos" -> "Presentación de Servicios Públicos";
            case "mejora-servicios-funerarios" -> "Mejora de Servicios Funerarios";
            case "economia-turismo" -> "Economía y Turismo";
            case "humanos" -> "Planeación, Seguimiento y Control del Desarrollo Humano y la Participation Social";
            case "desarrollo-humano-social" -> "Desarrollo Humano y Social";
            case "cultura-todos" -> "La Cultura es de Todos y para Todos";
            case "juventudes" -> "Cuernavaca para las Juventudes";
            case "deporte-accion" -> "Deporte en Accion";
            case "desarrollo-social-incluyente" -> "Desarrollo Social Incluyente y Calidad de Vida";
            case "accion-migrante" -> "Accion Migrante y Asuntos Religiosos";
            case "defensa-justicia-legalidad" -> "Defensa con justicia y Legalidad";
            case "fortalecer-fomentar-proteger" -> "Servicios para Fortalecer, Fomentar y Proteger el Sano Desarrollo Fisico, Mental y Emocional de las Familias del Municipio de Cuernavaca";
            case "servicios-basicos" -> "Servicios Basicos de Asistencia Social y de Apoyo para el Desarrollo Integral de las Familias y de los Grupos Vulnerables del Municipio de Cuernavaca";
            case "prodder" -> "PRODDER";
            case "jubilados-pensionados" -> "Jubilados y Pensionados";
            case "conagua" -> "CONAGUA";
            default -> {
                String textoFormateado = tema.replace("-", " ");
                StringBuilder resultado = new StringBuilder();
                for (String palabra : textoFormateado.split(" ")) {
                    if (!palabra.isEmpty()) {
                        resultado.append(palabra.substring(0, 1).toUpperCase())
                                 .append(palabra.substring(1))
                                 .append(" ");
                    }
                }
                yield resultado.toString().trim();
            }
        };
    }
}