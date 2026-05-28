package com.example.maquinawebmongo.controller;

import com.example.maquinawebmongo.model.GraficaData;
import com.example.maquinawebmongo.model.Main;
import com.example.maquinawebmongo.repository.MainRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class MainController {

    private final MainRepository mainRepository;

    public MainController(MainRepository mainRepository) {
        this.mainRepository = mainRepository;
    }

    // ==================== PÁGINA PRINCIPAL ====================
    @GetMapping("/")
    public String inicio() {
        return "inicio";
    }

    // ==================== DASHBOARD DE CADA SECCIÓN ====================
    @GetMapping("/{seccion}/dashboard")
    public String dashboard(@PathVariable String seccion, Model model) {
        model.addAttribute("seccion", seccion);
        model.addAttribute("seccionNombre", getNombreSeccion(seccion));
        return seccion + "/dashboard";
    }

    // ==================== CRUD GENÉRICO PARA CADA TEMA ====================

    // DIAGNÓSTICO: Ver programas en BD
    @GetMapping("/debug/ver-programas")
    @ResponseBody
    public String verProgramas() {
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

    // DIAGNÓSTICO: Actualizar programas existentes
    @GetMapping("/debug/actualizar-programas")
    @ResponseBody
    public String actualizarProgramas() {
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

    // Listar elementos de un tema específico
    @GetMapping("/{seccion}/{tema}")
    public String listarTema(@PathVariable String seccion, @PathVariable String tema, Model model) {
        String coleccion = seccion + "_" + tema;
        model.addAttribute("mainList", mainRepository.findBySeccion(coleccion));
        model.addAttribute("seccionUrl", seccion);
        model.addAttribute("temaUrl", tema);
        model.addAttribute("tituloPagina", getNombreTema(tema));
        return "main/lista";
    }

    // Ver detalles de un elemento (solo lectura)
    @GetMapping("/{seccion}/{tema}/ver/{id}")
    public String verTema(@PathVariable String seccion, @PathVariable String tema,
                          @PathVariable String id, Model model, RedirectAttributes ra) {
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

    // Formulario para nuevo elemento
    @GetMapping("/{seccion}/{tema}/add")
    public String addTema(@PathVariable String seccion, @PathVariable String tema, Model model) {
        Main main = new Main();
        main.setSeccion(seccion + "_" + tema);
        model.addAttribute("main", main);
        model.addAttribute("accion", "Crear");
        model.addAttribute("seccionUrl", seccion);
        model.addAttribute("temaUrl", tema);
        model.addAttribute("tituloPagina", getNombreTema(tema));
        return "main/formulario";
    }

    // Guardar elemento
    @PostMapping("/{seccion}/{tema}/guardar")
    public String guardarTema(@PathVariable String seccion, @PathVariable String tema,
                              @ModelAttribute Main main, RedirectAttributes ra) {
        try {
            String coleccion = seccion + "_" + tema;
            main.setSeccion(coleccion);
            
            // ✅ ASIGNAR PROGRAMA AUTOMÁTICAMENTE
            main.setPrograma(getNombreTema(tema));

            // Forzar generación de ID
            if (main.getId() == null || main.getId().isEmpty()) {
                main.setId(java.util.UUID.randomUUID().toString());
            }

            if (main.getTitulo() == null || main.getTitulo().trim().isEmpty()) {
                ra.addFlashAttribute("error", "⚠️ El título es obligatorio");
                return "redirect:/" + seccion + "/" + tema + "/add";
            }
            if (main.getPorcentaje() == null) {
                main.setPorcentaje(0.0);
            }

            mainRepository.save(main);
            ra.addFlashAttribute("mensaje", "✅ Guardado exitoso - Programa: " + main.getPrograma());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error al guardar: " + e.getMessage());
        }
        return "redirect:/" + seccion + "/" + tema;
    }

    // Editar elemento
    @GetMapping("/{seccion}/{tema}/editar/{id}")
    public String editarTema(@PathVariable String seccion, @PathVariable String tema,
                             @PathVariable String id, Model model, RedirectAttributes ra) {
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

    // Actualizar elemento
    @PostMapping("/{seccion}/{tema}/actualizar/{id}")
    public String actualizarTema(@PathVariable String seccion, @PathVariable String tema,
                                 @PathVariable String id, @ModelAttribute Main mainActualizado, RedirectAttributes ra) {
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
                // ✅ Forzar que el programa sea el correcto también al actualizar
                main.setPrograma(getNombreTema(tema));
                main.setTiempo(mainActualizado.getTiempo() != null ? mainActualizado.getTiempo().trim() : "");
                main.setPorcentaje(mainActualizado.getPorcentaje() != null ? mainActualizado.getPorcentaje() : 0.0);

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

    // Eliminar elemento
    @PostMapping("/{seccion}/{tema}/eliminar/{id}")
    public String eliminarTema(@PathVariable String seccion, @PathVariable String tema,
                               @PathVariable String id, RedirectAttributes ra) {
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

        // ==================== GRÁFICAS ====================

    // Gráfica general de UNA SECCIÓN (todos los programas)
    @GetMapping("/{seccion}/dashboard/grafica")
    public String graficaSeccion(@PathVariable String seccion, Model model) {
        model.addAttribute("seccion", seccion);
        model.addAttribute("seccionNombre", getNombreSeccion(seccion));
        return "seccion/grafica-seccion";
    }

    // API para obtener datos de gráfica de UNA SECCIÓN
   // API para obtener datos de gráfica de UNA SECCIÓN (dinámico y ordenado)
@GetMapping("/api/{seccion}/dashboard/grafica")
@ResponseBody
public List<GraficaData> getGraficaSeccion(@PathVariable String seccion) {
    // Obtener todos los temas (programas) que existen en esta sección
    String prefijo = seccion + "_";
    List<Main> todosLosRegistros = mainRepository.findAll();
    
    // Usar un Set para obtener temas únicos
    java.util.Set<String> temasUnicos = new java.util.HashSet<>();
    
    for (Main item : todosLosRegistros) {
        String seccionCompleta = item.getSeccion();
        if (seccionCompleta != null && seccionCompleta.startsWith(prefijo)) {
            String tema = seccionCompleta.substring(prefijo.length());
            temasUnicos.add(tema);
        }
    }
    
    // Convertir a lista y ordenar
    List<String> temasLista = new java.util.ArrayList<>(temasUnicos);
    java.util.Collections.sort(temasLista);
    
    List<GraficaData> datos = new java.util.ArrayList<>();
    
    for (String tema : temasLista) {
        String coleccion = seccion + "_" + tema;
        List<Main> items = mainRepository.findBySeccion(coleccion);
        
        // Calcular promedio de avance del programa
        double suma = 0;
        for (Main item : items) {
            suma += item.getPorcentaje() != null ? item.getPorcentaje() : 0;
        }
        double promedio = items.isEmpty() ? 0 : suma / items.size();
        
        datos.add(new GraficaData(getNombreTema(tema), promedio));
    }
    
    return datos;
}

    // Gráfica de un PROGRAMA ESPECÍFICO (todos sus indicadores)
    @GetMapping("/{seccion}/{tema}/grafica")
    public String graficaPrograma(@PathVariable String seccion, @PathVariable String tema, Model model) {
        model.addAttribute("seccionUrl", seccion);
        model.addAttribute("temaUrl", tema);
        model.addAttribute("tituloPagina", getNombreTema(tema));
        return "main/grafica-programa";
    }

    // API para obtener datos de gráfica de un PROGRAMA (sus indicadores)
    @GetMapping("/api/{seccion}/{tema}/grafica")
    @ResponseBody
    public List<GraficaData> getGraficaPrograma(@PathVariable String seccion, @PathVariable String tema) {
        String coleccion = seccion + "_" + tema;
        List<Main> items = mainRepository.findBySeccion(coleccion);
        
        List<GraficaData> datos = new java.util.ArrayList<>();
        for (Main item : items) {
            datos.add(new GraficaData(item.getTitulo(), item.getPorcentaje() != null ? item.getPorcentaje() : 0));
        }
        return datos;
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