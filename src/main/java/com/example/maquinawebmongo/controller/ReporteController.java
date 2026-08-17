package com.example.maquinawebmongo.controller;

import com.example.maquinawebmongo.model.Main;
import com.example.maquinawebmongo.repository.MainRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Map;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final MainRepository mainRepository;

    public ReporteController(MainRepository mainRepository) {
        this.mainRepository = mainRepository;
    }

    private boolean esAdmin() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth == null) {
                System.out.println("❌ Auth es null");
                return false;
            }
            
            System.out.println("🔍 Verificando admin para: " + auth.getName());
            System.out.println("🔍 Authorities: " + auth.getAuthorities());
            
            boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(g -> {
                    String authority = g.getAuthority();
                    return authority.equals("ROLE_ADMIN") || 
                           authority.equals("ADMIN") ||
                           authority.endsWith("ADMIN");
                });
            
            System.out.println("✅ ¿Es admin? " + esAdmin);
            return esAdmin;
            
        } catch (Exception e) {
            System.err.println("❌ Error al verificar admin: " + e.getMessage());
            return false;
        }
    }

    // ==================== VISTA DE REPORTES ====================
    @GetMapping
    public String reportesGeneral(Model model) {
        if (!esAdmin()) {
            model.addAttribute("error", "Acceso denegado - Solo administradores");
            return "redirect:/inicio";
        }
        
        long totalRegistros = mainRepository.count();
        model.addAttribute("totalRegistros", totalRegistros);
        
        List<Main> todosLosRegistros = mainRepository.findAll();
        
        long verdes = contarPorColor(todosLosRegistros, "verde");
        long amarillos = contarPorColor(todosLosRegistros, "amarillo");
        long rojos = contarPorColor(todosLosRegistros, "rojo");
        long grises = contarPorColor(todosLosRegistros, "gris");
        long total = verdes + amarillos + rojos + grises;
        
        List<Map<String, Object>> estados = new ArrayList<>();
        if (total > 0) {
            double pVerde = Math.round((verdes * 100.0 / total) * 10.0) / 10.0;
            double pAmarillo = Math.round((amarillos * 100.0 / total) * 10.0) / 10.0;
            double pRojo = Math.round((rojos * 100.0 / total) * 10.0) / 10.0;
            double pGris = Math.round((grises * 100.0 / total) * 10.0) / 10.0;
            
            estados.add(Map.of("nombre", "Aceptable", "cantidad", verdes, "porcentaje", pVerde));
            estados.add(Map.of("nombre", "En riesgo", "cantidad", amarillos, "porcentaje", pAmarillo));
            estados.add(Map.of("nombre", "Crítico", "cantidad", rojos, "porcentaje", pRojo));
            estados.add(Map.of("nombre", "Fallo en planeación", "cantidad", grises, "porcentaje", pGris));
        }
        model.addAttribute("estados", estados);
        
        return "reportes-general";
    }

    // ==================== EXPORTAR A EXCEL ====================
    @GetMapping("/exportar/excel")
    public void exportarExcel(@RequestParam(required = false) String seccion,
                              @RequestParam(required = false) String color,
                              HttpServletResponse response) throws IOException {

        if (!esAdmin()) {
            System.err.println("❌ ACCESO DENEGADO: Usuario no es ADMIN");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado - Solo administradores");
            return;
        }

        System.out.println("✅ Exportando Excel - Usuario ADMIN autenticado");

        List<Main> registros = obtenerRegistros(seccion, color);

        Workbook workbook = new XSSFWorkbook();
        
        // Hoja 1: Datos del reporte
        Sheet sheet = workbook.createSheet("Reporte_OmniView");

        CellStyle headerStyle = crearEstiloHeader(workbook);
        CellStyle verdeStyle = crearEstiloColor(workbook, IndexedColors.GREEN);
        CellStyle amarilloStyle = crearEstiloColor(workbook, IndexedColors.YELLOW);
        CellStyle rojoStyle = crearEstiloColor(workbook, IndexedColors.RED);
        CellStyle grisStyle = crearEstiloColor(workbook, IndexedColors.GREY_25_PERCENT);

        String[] columnas = {"#", "Título", "Programa/Área", "Periodicidad", "Avance (%)", "Estado", "Color", "Fecha Creación", "Sección"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columnas.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columnas[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4500);
        }

        int rowNum = 1;
        int contador = 1;
        
        // ✅ SimpleDateFormat declarado AQUÍ (dentro del método)
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        for (Main m : registros) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(contador++);
            row.createCell(1).setCellValue(m.getTitulo() != null ? m.getTitulo() : "");
            row.createCell(2).setCellValue(m.getPrograma() != null ? m.getPrograma() : "");
            row.createCell(3).setCellValue(m.getTiempo() != null ? m.getTiempo() : "");
            row.createCell(4).setCellValue(m.getPorcentaje() != null ? m.getPorcentaje() : 0);

            String estado = getEstadoPorColor(m.getColorSeleccionado());
            String colorTexto = getColorTexto(m.getColorSeleccionado());
            
            Cell estadoCell = row.createCell(5);
            estadoCell.setCellValue(estado);
            estadoCell.setCellStyle(getEstiloPorColor(workbook, m.getColorSeleccionado(), verdeStyle, amarilloStyle, rojoStyle, grisStyle));

            Cell colorCell = row.createCell(6);
            colorCell.setCellValue(colorTexto);
            colorCell.setCellStyle(getEstiloPorColor(workbook, m.getColorSeleccionado(), verdeStyle, amarilloStyle, rojoStyle, grisStyle));

            // ✅ Usar sdf correctamente
            String fechaStr = "";
            if (m.getFechaCreacion() != null) {
                try {
                    fechaStr = sdf.format(m.getFechaCreacion());
                } catch (Exception e) {
                    // Si falla, usar formato alternativo
                    fechaStr = m.getFechaCreacion().toString();
                }
            }
            row.createCell(7).setCellValue(fechaStr);
            row.createCell(8).setCellValue(m.getSeccion() != null ? m.getSeccion() : "");
        }

        // Hoja 2: Gráfica y resumen
        agregarGraficaExcel(workbook, registros);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_omniview_" + System.currentTimeMillis() + ".xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
        
        System.out.println("✅ Excel exportado correctamente");
    }

    // ==================== EXPORTAR A PDF ====================
    @GetMapping("/exportar/pdf")
    public void exportarPDF(@RequestParam(required = false) String seccion,
                            @RequestParam(required = false) String color,
                            HttpServletResponse response) throws IOException {

        if (!esAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado - Solo administradores");
            return;
        }

        List<Main> registros = obtenerRegistros(seccion, color);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_omniview_" + System.currentTimeMillis() + ".pdf");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);
        document.setMargins(30, 30, 30, 30);

        // ============ TÍTULO ============
        PdfFont titleFont = PdfFontFactory.createFont();
        String tituloReporte = "📊 Reporte OmniView";
        if (seccion != null && !seccion.isEmpty() && !seccion.equals("todos")) {
            tituloReporte += " - " + seccion.toUpperCase();
        }
        if (color != null && !color.isEmpty() && !color.equals("todos")) {
            tituloReporte += " (Filtro: " + getColorTexto(color) + ")";
        }
        
        Paragraph title = new Paragraph(tituloReporte)
                .setFontSize(20)
                .setFont(titleFont)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        // ============ FECHA DE GENERACIÓN ============
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Paragraph fechaGeneracion = new Paragraph("📅 Fecha de generación: " + sdf.format(new java.util.Date()))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        document.add(fechaGeneracion);

        // ============ RESUMEN DE ESTADOS POR COLOR ============
        long total = registros.size();
        long verdes = contarPorColor(registros, "verde");
        long amarillos = contarPorColor(registros, "amarillo");
        long rojos = contarPorColor(registros, "rojo");
        long grises = contarPorColor(registros, "gris");

        // 🔍 DEBUG
        System.out.println("📊 Resumen de colores:");
        System.out.println("🟢 Verdes: " + verdes);
        System.out.println("🟡 Amarillos: " + amarillos);
        System.out.println("🔴 Rojos: " + rojos);
        System.out.println("⚪ Grises: " + grises);
        System.out.println("📝 Total: " + total);

        // ✅ TABLA DE RESUMEN DE COLORES
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1}));
        summaryTable.setWidth(UnitValue.createPercentValue(100));
        summaryTable.setMarginBottom(15);

        String[][] resumen = {
                {"Total", "🟢 Aceptable", "🟡 En riesgo", "🔴 Crítico", "⚪ Fallo en planeación"},
                {String.valueOf(total), String.valueOf(verdes), String.valueOf(amarillos), String.valueOf(rojos), String.valueOf(grises)}
        };

        for (String[] row : resumen) {
            for (String cellText : row) {
                com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                        .add(new Paragraph(cellText));
                if (row == resumen[0]) {
                    cell.setBackgroundColor(ColorConstants.DARK_GRAY);
                    cell.setFontColor(ColorConstants.WHITE);
                }
                summaryTable.addCell(cell);
            }
        }
        document.add(summaryTable);

        // ============ GRÁFICA DE BARRAS EN PDF ============
        agregarGraficaPDF(document, registros, seccion);

        document.add(new Paragraph("\n"));

        // ============ TABLA DE DATOS ============
        Table table = new Table(UnitValue.createPercentArray(new float[]{0.5f, 1.5f, 1f, 0.8f, 0.8f, 1f, 1f, 1f}));
        table.setWidth(UnitValue.createPercentValue(100));

        String[] headers = {"#", "Título", "Programa", "Periodicidad", "Avance", "Estado", "Color", "Sección"};
        for (String h : headers) {
            com.itextpdf.layout.element.Cell headerCell = new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph(h).setBold());
            headerCell.setBackgroundColor(ColorConstants.DARK_GRAY);
            headerCell.setFontColor(ColorConstants.WHITE);
            table.addHeaderCell(headerCell);
        }

        int contador = 1;

        for (Main m : registros) {
            Double porcentaje = m.getPorcentaje() != null ? m.getPorcentaje() : 0;
            String estado = getEstadoPorColor(m.getColorSeleccionado());
            String colorTexto = getColorTexto(m.getColorSeleccionado());
            com.itextpdf.kernel.colors.Color estadoColor = getColorSemaforo(m.getColorSeleccionado());

            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.valueOf(contador++))));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(m.getTitulo() != null ? m.getTitulo() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(m.getPrograma() != null ? m.getPrograma() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(m.getTiempo() != null ? m.getTiempo() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.format("%.2f", porcentaje) + "%")));

            com.itextpdf.layout.element.Cell estadoCell = new com.itextpdf.layout.element.Cell().add(new Paragraph(estado));
            estadoCell.setBackgroundColor(estadoColor);
            table.addCell(estadoCell);

            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(colorTexto)));

            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(m.getSeccion() != null ? m.getSeccion() : "")));
        }

        document.add(table);
        document.close();

        response.getOutputStream().write(baos.toByteArray());
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private List<Main> obtenerRegistros(String seccion, String color) {
        List<Main> todos = mainRepository.findAll();
        
        if (seccion != null && !seccion.isEmpty() && !seccion.equals("todos")) {
            String coleccionCompleta = seccion + "_";
            todos = todos.stream()
                    .filter(m -> m.getSeccion() != null && m.getSeccion().startsWith(coleccionCompleta))
                    .collect(Collectors.toList());
        }
        
        if (color != null && !color.isEmpty() && !color.equals("todos")) {
            todos = todos.stream()
                    .filter(m -> color.equals(m.getColorSeleccionado()))
                    .collect(Collectors.toList());
        }
        
        return todos;
    }

    private long contarPorColor(List<Main> registros, String color) {
        return registros.stream()
                .filter(m -> color.equals(m.getColorSeleccionado()))
                .count();
    }

    private String getColorTexto(String color) {
        if (color == null || color.isEmpty()) return "Sin color";
        return switch (color) {
            case "verde" -> "🟢 Aceptable";
            case "amarillo" -> "🟡 En riesgo";
            case "rojo" -> "🔴 Crítico";
            case "gris" -> "⚪ Fallo en planeación";
            default -> "Sin color";
        };
    }

    private String getEstadoPorColor(String color) {
        if (color == null || color.isEmpty()) return "Sin estado";
        return switch (color) {
            case "verde" -> "Aceptable";
            case "amarillo" -> "En riesgo";
            case "rojo" -> "Crítico";
            case "gris" -> "Fallo en planeación";
            default -> "Sin estado";
        };
    }

    private com.itextpdf.kernel.colors.Color getColorSemaforo(String color) {
        if (color == null || color.isEmpty()) return ColorConstants.GRAY;
        return switch (color) {
            case "verde" -> ColorConstants.GREEN;
            case "amarillo" -> ColorConstants.ORANGE;
            case "rojo" -> ColorConstants.RED;
            case "gris" -> ColorConstants.GRAY;
            default -> ColorConstants.GRAY;
        };
    }

    private CellStyle getEstiloPorColor(Workbook workbook, String color,
                                         CellStyle verde, CellStyle amarillo,
                                         CellStyle rojo, CellStyle gris) {
        if (color == null || color.isEmpty()) return gris;
        return switch (color) {
            case "verde" -> verde;
            case "amarillo" -> amarillo;
            case "rojo" -> rojo;
            case "gris" -> gris;
            default -> gris;
        };
    }

    private void agregarGraficaExcel(Workbook workbook, List<Main> registros) {
    Sheet chartSheet = workbook.createSheet("Gráfica y Resumen");

    long verdes = contarPorColor(registros, "verde");
    long amarillos = contarPorColor(registros, "amarillo");
    long rojos = contarPorColor(registros, "rojo");
    long grises = contarPorColor(registros, "gris");
    long total = verdes + amarillos + rojos + grises;

    // TÍTULO
    Row tituloRow = chartSheet.createRow(0);
    tituloRow.createCell(0).setCellValue("📊 RESUMEN DE ESTADOS POR COLOR");
    
    CellStyle tituloStyle = workbook.createCellStyle();
    Font tituloFont = workbook.createFont();
    tituloFont.setBold(true);
    tituloFont.setColor(IndexedColors.WHITE.getIndex());
    tituloStyle.setFont(tituloFont);
    tituloStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
    tituloStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    tituloStyle.setAlignment(HorizontalAlignment.CENTER);
    tituloRow.getCell(0).setCellStyle(tituloStyle);
    chartSheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

    // ENCABEZADOS
    Row headerRow = chartSheet.createRow(2);
    headerRow.createCell(0).setCellValue("Estado");
    headerRow.createCell(1).setCellValue("Cantidad");
    headerRow.createCell(2).setCellValue("Porcentaje");
    headerRow.createCell(3).setCellValue("Gráfica");

    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    headerStyle.setAlignment(HorizontalAlignment.CENTER);
    headerStyle.setBorderBottom(BorderStyle.THIN);
    headerStyle.setBorderTop(BorderStyle.THIN);
    headerStyle.setBorderLeft(BorderStyle.THIN);
    headerStyle.setBorderRight(BorderStyle.THIN);

    for (int i = 0; i < 4; i++) {
        headerRow.getCell(i).setCellStyle(headerStyle);
    }

    // DATOS
    String[][] datos = {
        {"🟢 Aceptable", String.valueOf(verdes), String.valueOf(total > 0 ? Math.round((verdes * 100.0 / total) * 10.0) / 10.0 : 0)},
        {"🟡 En riesgo", String.valueOf(amarillos), String.valueOf(total > 0 ? Math.round((amarillos * 100.0 / total) * 10.0) / 10.0 : 0)},
        {"🔴 Crítico", String.valueOf(rojos), String.valueOf(total > 0 ? Math.round((rojos * 100.0 / total) * 10.0) / 10.0 : 0)},
        {"⚪ Fallo en planeación", String.valueOf(grises), String.valueOf(total > 0 ? Math.round((grises * 100.0 / total) * 10.0) / 10.0 : 0)}
    };

    CellStyle dataStyle = workbook.createCellStyle();
    dataStyle.setBorderBottom(BorderStyle.THIN);
    dataStyle.setBorderLeft(BorderStyle.THIN);
    dataStyle.setBorderRight(BorderStyle.THIN);
    dataStyle.setAlignment(HorizontalAlignment.CENTER);

    int rowNum = 3;
    for (String[] fila : datos) {
        Row row = chartSheet.createRow(rowNum++);
        for (int i = 0; i < fila.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(fila[i]);
            cell.setCellStyle(dataStyle);
            
            // Colorear según estado
            if (i == 0) {
                if (fila[0].contains("Aceptable")) {
                    cell.setCellStyle(crearEstiloColor(workbook, IndexedColors.GREEN));
                } else if (fila[0].contains("En riesgo")) {
                    cell.setCellStyle(crearEstiloColor(workbook, IndexedColors.YELLOW));
                } else if (fila[0].contains("Crítico")) {
                    cell.setCellStyle(crearEstiloColor(workbook, IndexedColors.RED));
                } else if (fila[0].contains("Fallo")) {
                    cell.setCellStyle(crearEstiloColor(workbook, IndexedColors.GREY_25_PERCENT));
                }
            }
        }
        
        // GRÁFICA DE BARRAS
        double porcentaje = Double.parseDouble(fila[2]);
        String barra = "";
        int barras = (int) (porcentaje / 2);
        for (int i = 0; i < Math.min(barras, 30); i++) {
            barra += "█";
        }
        if (barra.isEmpty() && porcentaje > 0) {
            barra = "▌";
        }
        Row barRow = chartSheet.getRow(rowNum - 1);
        Cell barCell = barRow.createCell(3);
        barCell.setCellValue(barra);
        barCell.setCellStyle(dataStyle);
        chartSheet.setColumnWidth(3, 6000);
    }

    // TOTAL
    Row totalRow = chartSheet.createRow(rowNum++);
    totalRow.createCell(0).setCellValue("TOTAL");
    totalRow.createCell(1).setCellValue(total);
    totalRow.createCell(2).setCellValue("100%");
    
    CellStyle totalStyle = workbook.createCellStyle();
    Font totalFont = workbook.createFont();
    totalFont.setBold(true);
    totalStyle.setFont(totalFont);
    totalStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
    totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    totalStyle.setAlignment(HorizontalAlignment.CENTER);
    totalStyle.setBorderBottom(BorderStyle.THIN);
    totalStyle.setBorderTop(BorderStyle.THIN);
    totalStyle.setBorderLeft(BorderStyle.THIN);
    totalStyle.setBorderRight(BorderStyle.THIN);
    
    for (int i = 0; i < 3; i++) {
        totalRow.getCell(i).setCellStyle(totalStyle);
    }

    // Ajustar columnas
    for (int i = 0; i < 3; i++) {
        chartSheet.autoSizeColumn(i);
    }
    chartSheet.setColumnWidth(3, 6000);
}

    private void agregarGraficaPDF(Document document, List<Main> registros, String seccion) throws IOException {
    long verdes = contarPorColor(registros, "verde");
    long amarillos = contarPorColor(registros, "amarillo");
    long rojos = contarPorColor(registros, "rojo");
    long grises = contarPorColor(registros, "gris");
    long total = verdes + amarillos + rojos + grises;

    // Título de la gráfica
    String tituloGrafica = "📊 Resumen de Estados";
    if (seccion != null && !seccion.isEmpty() && !seccion.equals("todos")) {
        tituloGrafica += " - " + seccion.toUpperCase();
    }
    document.add(new Paragraph(tituloGrafica).setFontSize(16).setBold().setMarginBottom(10));

    if (total == 0) {
        document.add(new Paragraph("No hay datos para mostrar").setFontSize(12).setTextAlignment(TextAlignment.CENTER));
        return;
    }

    // ============ GRÁFICA DE PASTEL (simulada con tabla) ============
    Table pieTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}));
    pieTable.setWidth(UnitValue.createPercentValue(100));
    pieTable.setMarginBottom(15);

    String[] estados = {"Aceptable", "En riesgo", "Crítico", "Fallo en planeación"};
    long[] valores = {verdes, amarillos, rojos, grises};
    com.itextpdf.kernel.colors.Color[] colores = {
            ColorConstants.GREEN,
            ColorConstants.ORANGE,
            ColorConstants.RED,
            ColorConstants.GRAY
    };
    String[] iconos = {"🟢", "🟡", "🔴", "⚪"};

    // Calcular ángulos para la "gráfica de pastel" (simulada con barras circulares)
    for (int i = 0; i < estados.length; i++) {
        double porcentaje = (valores[i] * 100.0) / total;
        int barras = (int) Math.round(porcentaje / 5); // Cada barra = 5%
        
        // Crear barra circular simulada
        String barraCircular = "";
        for (int j = 0; j < Math.min(barras, 20); j++) {
            barraCircular += "●";
        }
        if (barras == 0 && valores[i] > 0) {
            barraCircular = "○";
        }
        
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(iconos[i] + " " + estados[i] + "\n" + 
                                   valores[i] + " (" + String.format("%.1f", porcentaje) + "%)")
                .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(colores[i])
                .setBorder(null);
        pieTable.addCell(cell);
    }
    document.add(pieTable);

    // ============ GRÁFICA DE BARRAS HORIZONTALES ============
    document.add(new Paragraph("📈 Distribución detallada:").setFontSize(12).setBold().setMarginBottom(5));

    Table barTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
    barTable.setWidth(UnitValue.createPercentValue(100));

    String[] nombres = {"🟢 Aceptable", "🟡 En riesgo", "🔴 Crítico", "⚪ Fallo en planeación"};
    double[] porcentajes = {
            (double) verdes / total * 100,
            (double) amarillos / total * 100,
            (double) rojos / total * 100,
            (double) grises / total * 100
    };

    for (int i = 0; i < nombres.length; i++) {
        String barra = "█".repeat(Math.max(1, (int) (porcentajes[i] / 2.5)));
        barTable.addCell(new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(nombres[i])));
        barTable.addCell(new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(String.format("%.1f%% %s", porcentajes[i], barra))));
    }
    document.add(barTable);
}

    private CellStyle crearEstiloHeader(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearEstiloColor(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}