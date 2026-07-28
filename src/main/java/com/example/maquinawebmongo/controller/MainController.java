package com.example.maquinawebmongo.controller;

import com.example.maquinawebmongo.model.GraficaData;
import com.example.maquinawebmongo.model.Main;
import com.example.maquinawebmongo.model.Usuario;
import com.example.maquinawebmongo.repository.MainRepository;
import com.example.maquinawebmongo.service.NotificacionService;
import com.example.maquinawebmongo.repository.NotificacionRepository;
import com.example.maquinawebmongo.service.UsuarioService;

//import jakarta.servlet.http.HttpServletRequest;

//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

@Controller
public class MainController {

    private final MainRepository mainRepository;

    private final NotificacionService notificacionService;

    private final NotificacionRepository notificacionRepository;

    private final UsuarioService usuarioService;

    public MainController(MainRepository mainRepository, NotificacionService notificacionService, NotificacionRepository notificacionRepository, UsuarioService usuarioService) {
        this.mainRepository = mainRepository;
        this.notificacionService = notificacionService;
        this.notificacionRepository = notificacionRepository;
        this.usuarioService = usuarioService;
    }

    private boolean esAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
            .anyMatch(g -> g.getAuthority().endsWith("ADMIN"));
    }

    

    // ==================== PÁGINA PRINCIPAL ====================
    @GetMapping({"/", "/inicio"})
    public String inicio() {
        return "inicio";
    }

    @GetMapping("/perfil")
public String perfil(Model model, Authentication auth) {
    model.addAttribute("username", auth.getName());
    model.addAttribute("roles", auth.getAuthorities());

    String username = auth.getName();
    Usuario usuario = usuarioService.buscarPorUsername(username).orElse(null);
    
    if (usuario != null) {
        model.addAttribute("username", usuario.getUsername());
        model.addAttribute("nombreCompleto", usuario.getNombreCompleto());
        model.addAttribute("email", usuario.getEmail());
        model.addAttribute("roles", auth.getAuthorities());
        model.addAttribute("fechaRegistro", usuario.getFechaRegistro() != null ? 
            new java.text.SimpleDateFormat("dd/MM/yyyy").format(usuario.getFechaRegistro()) : "no disponible");
    }
    
    return "perfil";
}

    // ==================== DASHBOARD DE CADA SECCIÓN ====================
    @GetMapping("/{seccion}/dashboard")
    public String dashboard(@PathVariable String seccion, Model model, Authentication auth, RedirectAttributes ra) {
        // Verificar Acceso
        if (!tieneAccesoASeccion(seccion, auth)) {
            ra.addFlashAttribute("error", "Acceso denegado - No tienes acceso a esta sección");
            return "redirect:/inicio";
        }
        model.addAttribute("seccion", seccion);
        model.addAttribute("seccionNombre", getNombreSeccion(seccion));
        return seccion + "/dashboard";
    }

    // ==================== DIAGNÓSTICO (SOLO ADMIN) ====================
    @GetMapping("/debug/ver-programas")
    @ResponseBody
    public String verProgramas() {
        // 🔒 SOLO ADMIN
        if(!esAdmin()) return "NO AUTORIZADO";
        
        List<Main> todos = mainRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><h2>Registros en BD</h2><ul>");
        for (Main item : todos) {
            sb.append("<li>");
            sb.append("<strong>Título:</strong> ").append(item.getTitulo()).append("<br>");
            sb.append("<strong>Programa:</strong> '").append(item.getPrograma()).append("'<br>");
            sb.append("<strong>¿Es null?</strong> ").append(item.getPrograma() == null).append("<br>");
            sb.append("</li><hr>");
        }
        sb.append("</ul></body></html>");
        return sb.toString();
    }

    @GetMapping("/debug/actualizar-programas")
    @ResponseBody
    public String actualizarProgramas() {
        // 🔒 SOLO ADMIN
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

    // ==================== CRUD ====================

    // Listar elementos (Todos autenticados)
    @GetMapping("/{seccion}/{tema}")
    public String listarTema(@PathVariable String seccion, @PathVariable String tema, Model model, Authentication auth, RedirectAttributes ra) {
        // Verificar Acceso
        if (!tieneAccesoASeccion(seccion, auth)) {
            ra.addFlashAttribute("error", "Acceso denegado - No tienes acceso a esta sección");
            return "redirect:/inicio";
        }
        String coleccion = seccion + "_" + tema;
        List<Main> mainList = mainRepository.findBySeccion(coleccion);

        List<Map<String, Object>> mainListProcesada = new ArrayList<>();
        for (Main item : mainList) {
            Map<String, Object> datos = new HashMap<>();
            datos.put("id", item.getId());
            datos.put("titulo", item.getTitulo());
            datos.put("tiempo", item.getTiempo());
            datos.put("porcentaje", item.getPorcentaje());
            datos.put("programa", item.getPrograma());

            boolean puedeEditar = calcularPuedeEditar(item);
            datos.put("puedeEditar", puedeEditar);

            long fechaCreacion = 0;
            if (item.getFechaCreacion() != null) {
                fechaCreacion = item.getFechaCreacion()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
            }
            datos.put("fechaCreacion", fechaCreacion);

            long fechaUltimaEdicion = 0;
            if (item.getFechaUltimaEdicion() != null) {
                fechaUltimaEdicion = item.getFechaUltimaEdicion()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
            }
            datos.put("fechaUltimaEdicion", fechaUltimaEdicion);

            mainListProcesada.add(datos);

            System.out.println("ID: " + item.getId() + 
            " | Título: " + item.getTitulo() + 
            " | Fecha Creación: " + item.getFechaCreacion() + 
            " | Tiempo: " + item.getTiempo() + 
            " | Puede Editar: " + puedeEditar);
        }
        

        model.addAttribute("mainList", mainListProcesada);
        model.addAttribute("seccionUrl", seccion);
        model.addAttribute("temaUrl", tema);
        model.addAttribute("tituloPagina", getNombreTema(tema));
        model.addAttribute("esAdmin", esAdmin()); // Pasamos variable a la vista para ocultar botones
        model.addAttribute("fechaActual", LocalDateTime.now()); // Para mostrar la fecha actual en la vista
        return "main/lista";
    }

    private boolean calcularPuedeEditar(Main item) {
        if (item.getFechaCreacion() == null || item.getTiempo() == null) {
            return false;
        }

        LocalDateTime ahora = LocalDateTime.now();
        
        // ✅ Verificar mes permitido
        if (!esMesPermitido(ahora)) {
            return false;
        }
        
        // ✅ Verificar día laboral
        if (!esDiaLaboral(ahora)) {
            return false;
        }

        // Usar fecha de última actualización si está disponible, sino fecha de creación
        LocalDateTime fechaBase = item.getFechaUltimaEdicion() != null 
            ? item.getFechaUltimaEdicion() 
            : item.getFechaCreacion();

        // Calcular diferencia en días
        long diffEnDias = ChronoUnit.DAYS.between(fechaBase, ahora);

        switch (item.getTiempo()) {
            case "Minuto": 
                return Duration.between(fechaBase, ahora).toMinutes() >= 1;
            case "Hora": 
                return Duration.between(fechaBase, ahora).toHours() >= 1;
            case "Diario": 
                return diffEnDias >= 1;
            case "Semanal": 
                return diffEnDias >= 7;
            case "Quincenal": 
                return diffEnDias >= 15;
            case "Mensual": 
                return diffEnDias >= 30;
            case "Bimestral": 
                return diffEnDias >= 60;
            case "Trimestral": 
                return diffEnDias >= 90;
            case "Cuatrimestral": 
                return diffEnDias >= 120;
            case "Semestral": 
                return diffEnDias >= 180;
            case "Anual": 
                return diffEnDias >= 365;
            default: 
                return false;
        }
    }

    // ✅ Verificar si el mes actual es Febrero(2), Mayo(5), Agosto(8) o Noviembre(11)
    private boolean esMesPermitido(LocalDateTime fecha) {
        int mes = fecha.getMonthValue(); // 1=Enero, 2=Febrero...
        
        // Meses permitidos: Febrero(2), Mayo(5), Agosto(8), Noviembre(11)
        return mes == 2 || mes == 5 || mes == 8 || mes == 11;
    }

    // ✅ Verificar si es día laboral (lunes a viernes)
    private boolean esDiaLaboral(LocalDateTime fecha) {
        int diaSemana = fecha.getDayOfWeek().getValue(); // 1=Lunes, 7=Domingo
        return diaSemana >= 1 && diaSemana <= 5; // Lunes a Viernes
    }

    // Ver detalles (Todos autenticados)
    @GetMapping("/{seccion}/{tema}/ver/{id}")
    public String verTema(@PathVariable String seccion, @PathVariable String tema,
                          @PathVariable String id, Model model, RedirectAttributes ra, Authentication auth) {
        // Verificar Acceso
        if (!tieneAccesoASeccion(seccion, auth)) {
            return "redirect:/error?mensaje=Acceso denegado";
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

    // Formulario NUEVO elemento (🔒 SOLO ADMIN)
    @GetMapping("/{seccion}/{tema}/add")
    public String addTema(@PathVariable String seccion, @PathVariable String tema, Model model, RedirectAttributes ra, Authentication auth) {
        // Verificar Acceso
        if (!tieneAccesoASeccion(seccion, auth)) {
            return "inicio";
        }
        // 🔒 SOLO ADMIN
        if(!esAdmin()) {
            ra.addFlashAttribute("error", "❌ Acceso denegado: Solo administradores");
            return "redirect:/" + seccion + "/" + tema;
        }

        try {
            Main main = new Main();
            main.setSeccion(seccion + "_" + tema);
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

    // Guardar elemento (🔒 SOLO ADMIN)
    @PostMapping("/{seccion}/{tema}/guardar")
    public String guardarTema(@PathVariable String seccion, @PathVariable String tema,
                              @ModelAttribute Main main, RedirectAttributes ra, Authentication auth) {
        // Verificar Acceso
        if (!tieneAccesoASeccion(seccion, auth)) {
            return "redirect:/error?mensaje=Acceso denegado";
        }
        // 🔒 SOLO ADMIN
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
                main.setFechaCreacion(LocalDateTime.now());
            }

            if (main.getTitulo() == null || main.getTitulo().trim().isEmpty()) {
                ra.addFlashAttribute("error", "⚠️ El título es obligatorio");
                return "redirect:/" + seccion + "/" + tema + "/add";
            }
            if (main.getPorcentaje() == null) {
                main.setPorcentaje(0.0);
            }

            mainRepository.save(main);
            ra.addFlashAttribute("mensaje", "✅ Guardado exitoso");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error al guardar: " + e.getMessage());
        }
        return "redirect:/" + seccion + "/" + tema;
    }

    // Editar elemento (🔒 SOLO ADMIN)
    @GetMapping("/{seccion}/{tema}/editar/{id}")
    public String editarTema(@PathVariable String seccion, @PathVariable String tema,
                             @PathVariable String id, Model model, RedirectAttributes ra, Authentication auth) {
        // Verificar Acceso
        if (!tieneAccesoASeccion(seccion, auth)) {
            return "redirect:/error?mensaje=Acceso denegado";
        }
        // 🔒 SOLO ADMIN
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

    // Actualizar elemento (🔒 SOLO ADMIN)
    @PostMapping("/{seccion}/{tema}/actualizar/{id}")
    public String actualizarTema(@PathVariable String seccion, @PathVariable String tema,
                                 @PathVariable String id, @ModelAttribute Main mainActualizado, RedirectAttributes ra, Authentication auth) {
        // Verificar Acceso
        if (!tieneAccesoASeccion(seccion, auth)) {
            return "redirect:/error?mensaje=Acceso denegado";
        }
        // 🔒 SOLO ADMIN
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
                main.setPorcentaje(mainActualizado.getPorcentaje() != null ? mainActualizado.getPorcentaje() : 0.0);
                main.setFechaUltimaEdicion(LocalDateTime.now());
                main.setNotificacionEnviada(false); // Reiniciar el estado de notificación al actualizar

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

    // Eliminar elemento (🔒 SOLO ADMIN)
    @PostMapping("/{seccion}/{tema}/eliminar/{id}")
    public String eliminarTema(@PathVariable String seccion, @PathVariable String tema,
                               @PathVariable String id, RedirectAttributes ra, Authentication auth) {
        // Verificar Acceso
        if (!tieneAccesoASeccion(seccion, auth)) {
            return "redirect:/error?mensaje=Acceso denegado";
        }
        // 🔒 SOLO ADMIN
        if(!esAdmin()) {
            ra.addFlashAttribute("error", "❌ Acceso denegado");
            return "redirect:/" + seccion + "/" + tema;
        }

        try {
            Optional<Main> mainOpt = mainRepository.findById(id);
            if (mainOpt.isPresent()) {
                String coleccionEsperada = seccion + "_" + tema;
                if (coleccionEsperada.equals(mainOpt.get().getSeccion())) {
                    mainRepository.deleteById(id);
                    ra.addFlashAttribute("mensaje", "✅ Eliminado correctamente");
                } else {
                    ra.addFlashAttribute("error", "❌ No se puede eliminar este registro");
                }
            } else {
                ra.addFlashAttribute("error", "❌ Registro no encontrado");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error al eliminar: " + e.getMessage());
        }
        return "redirect:/" + seccion + "/" + tema;
    }

    // ==================== GRÁFICAS (Todos autenticados) ====================
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

    // ==================== MANEJO DE ERRORES ====================
    @GetMapping("/error")
    public String manejarError() {
        return "redirect:/inicio";
    }

    // ==================== VERIFICAR EDICIÓN DISPONIBLE ====================

@GetMapping("/{seccion}/{tema}/verificar-edicion/{id}")
public String verificarEdicion(@PathVariable String seccion, @PathVariable String tema,
                               @PathVariable String id, Model model, RedirectAttributes ra,
                               Authentication auth) {
    // Verificar Acceso
    if (!tieneAccesoASeccion(seccion, auth)) {
        return "redirect:/error?mensaje=Acceso denegado";
    }
    try {
        Optional<Main> mainOpt = mainRepository.findById(id);
        if (mainOpt.isPresent()) {
            Main main = mainOpt.get();
            boolean puedeEditar = calcularPuedeEditar(main);
            
            if (puedeEditar) {
                // ✅ Verificar si ya se notificó
                List<com.example.maquinawebmongo.model.Notificacion> notificacionesExistentes = 
                    notificacionRepository.findByRegistroIdAndUsuarioId(id, auth.getName());
                
                if (notificacionesExistentes.isEmpty()) {
                    // Enviar notificación
                    String url = "/" + seccion + "/" + tema + "/editar/" + id;
                    String titulo = main.getTitulo();
                    notificacionService.notificarEdicionDisponible(id, titulo, url);
                    ra.addFlashAttribute("mensaje", "✅ Notificación enviada por correo y en el sistema.");
                } else {
                    ra.addFlashAttribute("mensaje", "ℹ️ Ya se envió una notificación para este indicador.");
                }
            } else {
                ra.addFlashAttribute("error", "⛔ El indicador aún no está disponible para edición.");
            }
        } else {
            ra.addFlashAttribute("error", "❌ Registro no encontrado.");
        }
    } catch (Exception e) {
        ra.addFlashAttribute("error", "❌ Error: " + e.getMessage());
    }
    return "redirect:/" + seccion + "/" + tema;
}

    // ==================== VERIFICAR ACCESO A SECCIÓN ====================

    private boolean tieneAccesoASeccion(String seccion, Authentication auth) {
        if (auth == null) return false;

        // 🔑 PRIMERO: Verificar si es ADMIN (directamente de Spring, NO de la BD)
        boolean esAdmin = auth.getAuthorities().stream()
            .anyMatch(g -> g.getAuthority().endsWith("ADMIN"));

        // ✅ Si es ADMIN: ACCESO TOTAL, NO revisamos nada más
        if (esAdmin) {
            System.out.println("✅ ADMIN detectado → Acceso concedido a: " + seccion);
            return true;
        }

        // 👤 SOLO si NO es ADMIN: revisamos sus secciones asignadas
        System.out.println("ℹ️ Usuario normal, revisando acceso a sección: " + seccion);
        return usuarioService.buscarPorUsername(auth.getName())
            .map(usuario -> {
                List<String> seccionesPermitidas = usuario.getSeccionesAcceso();
                return seccionesPermitidas != null && seccionesPermitidas.contains(seccion);
            })
            .orElse(false);
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    private String getNombreSeccion(String seccion) {
        return switch (seccion) {
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
            default -> {
                if (seccion.length() > 1) {
                    yield seccion.substring(0, 1).toUpperCase() + seccion.substring(1);
                } else {
                    yield seccion.toUpperCase();
                }
            }
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
            case "administracion-eficiente" -> "Administración Eficiente y Transparente de los Recursos Públicos";
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