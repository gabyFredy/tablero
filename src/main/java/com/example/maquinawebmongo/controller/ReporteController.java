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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
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

    // ==================== VISTA DE REPORTES ====================
    @GetMapping
public String reportesGeneral(Model model) {
    long totalRegistros = mainRepository.count();
    model.addAttribute("totalRegistros", totalRegistros);
    
    // Datos para la gráfica de vista previa
    List<Main> todosLosRegistros = mainRepository.findAll();
    long verdes = contarPorEstado(todosLosRegistros, "verde");
    long amarillos = contarPorEstado(todosLosRegistros, "amarillo");
    long rojos = contarPorEstado(todosLosRegistros, "rojo");
    long grises = contarPorEstado(todosLosRegistros, "gris");
    long total = verdes + amarillos + rojos + grises;
    
    // Crear lista de estados para la vista previa
    List<Map<String, Object>> estados = new ArrayList<>();
    if (total > 0) {
        // Calcular porcentajes como double con 1 decimal
        double pVerde = Math.round((verdes * 100.0 / total) * 10.0) / 10.0;
        double pAmarillo = Math.round((amarillos * 100.0 / total) * 10.0) / 10.0;
        double pRojo = Math.round((rojos * 100.0 / total) * 10.0) / 10.0;
        double pGris = Math.round((grises * 100.0 / total) * 10.0) / 10.0;
        
        estados.add(java.util.Map.of("nombre", "Aceptable", "cantidad", verdes, "porcentaje", pVerde));
        estados.add(java.util.Map.of("nombre", "En riesgo", "cantidad", amarillos, "porcentaje", pAmarillo));
        estados.add(java.util.Map.of("nombre", "Crítico", "cantidad", rojos, "porcentaje", pRojo));
        estados.add(java.util.Map.of("nombre", "Fallo en planeación", "cantidad", grises, "porcentaje", pGris));
    }
    model.addAttribute("estados", estados);
    
    return "reportes-general";
}
    // ==================== EXPORTAR A EXCEL ====================
    @GetMapping("/exportar/excel")
    public void exportarExcel(@RequestParam(required = false) String seccion,
                              HttpServletResponse response) throws IOException {

        List<Main> registros = obtenerRegistros(seccion);

        Workbook workbook = new XSSFWorkbook();
        
        // Hoja 1: Datos del reporte
        Sheet sheet = workbook.createSheet("Reporte_OmniView");

        CellStyle headerStyle = crearEstiloHeader(workbook);
        CellStyle verdeStyle = crearEstiloColor(workbook, IndexedColors.GREEN);
        CellStyle amarilloStyle = crearEstiloColor(workbook, IndexedColors.YELLOW);
        CellStyle rojoStyle = crearEstiloColor(workbook, IndexedColors.RED);
        CellStyle grisStyle = crearEstiloColor(workbook, IndexedColors.GREY_25_PERCENT);

        String[] columnas = {"#", "Título", "Programa/Área", "Periodicidad", "Avance (%)", "Estado", "Fecha Creación", "Sección"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columnas.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columnas[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4500);
        }

        int rowNum = 1;
        int contador = 1;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        for (Main m : registros) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(contador++);
            row.createCell(1).setCellValue(m.getTitulo() != null ? m.getTitulo() : "");
            row.createCell(2).setCellValue(m.getPrograma() != null ? m.getPrograma() : "");
            row.createCell(3).setCellValue(m.getTiempo() != null ? m.getTiempo() : "");
            row.createCell(4).setCellValue(m.getPorcentaje() != null ? m.getPorcentaje() : 0);

            String estado = getEstadoSemaforo(m.getPorcentaje());
            Cell estadoCell = row.createCell(5);
            estadoCell.setCellValue(estado);
            estadoCell.setCellStyle(getEstiloPorEstado(workbook, m.getPorcentaje(), verdeStyle, amarilloStyle, rojoStyle, grisStyle));

            row.createCell(6).setCellValue(m.getFechaCreacion() != null ? sdf.format(m.getFechaCreacion()) : "");
            row.createCell(7).setCellValue(m.getSeccion() != null ? m.getSeccion() : "");
        }

        // Hoja 2: Gráfica y resumen
        agregarGraficaExcel(workbook, registros);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_omniview_" + System.currentTimeMillis() + ".xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    // ==================== EXPORTAR A PDF ====================
    @SuppressWarnings("unused")
    @GetMapping("/exportar/pdf")
    public void exportarPDF(@RequestParam(required = false) String seccion,
                            HttpServletResponse response) throws IOException {

        List<Main> registros = obtenerRegistros(seccion);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_omniview_" + System.currentTimeMillis() + ".pdf");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);
        document.setMargins(30, 30, 30, 30);

        // TÍTULO
        PdfFont titleFont = PdfFontFactory.createFont();
        Paragraph title = new Paragraph("📊 Reporte OmniView - " + (seccion != null ? seccion.toUpperCase() : "General"))
                .setFontSize(20)
                .setFont(titleFont)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        // RESUMEN DE ESTADOS
        long total = registros.size();
        long verdes = contarPorEstado(registros, "verde");
        long amarillos = contarPorEstado(registros, "amarillo");
        long rojos = contarPorEstado(registros, "rojo");
        long grises = contarPorEstado(registros, "gris");

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
                cell.setBackgroundColor(ColorConstants.BLACK);
                summaryTable.addCell(cell);
            }
        }
        document.add(summaryTable);

        // ==================== GRÁFICA EN PDF ====================
        agregarGraficaPDF(document, registros);

        document.add(new Paragraph("\n"));

        // TABLA DE DATOS
        Table table = new Table(UnitValue.createPercentArray(new float[]{0.5f, 1.5f, 1f, 0.8f, 0.8f, 1f, 1f}));
        table.setWidth(UnitValue.createPercentValue(100));

        String[] headers = {"#", "Título", "Secretaria", "Periodicidad", "Avance", "Estado", "Sección"};
        for (String h : headers) {
            com.itextpdf.layout.element.Cell headerCell = new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph(h).setBold());
            headerCell.setBackgroundColor(ColorConstants.DARK_GRAY);
            headerCell.setFontColor(ColorConstants.WHITE);
            table.addHeaderCell(headerCell);
        }

        int contador = 1;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        for (Main m : registros) {
            Double porcentaje = m.getPorcentaje() != null ? m.getPorcentaje() : 0;
            String estado = getEstadoSemaforo(porcentaje);
            com.itextpdf.kernel.colors.Color estadoColor = getColorSemaforo(porcentaje);

            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.valueOf(contador++))));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(m.getTitulo() != null ? m.getTitulo() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(m.getPrograma() != null ? m.getSeccion() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(m.getTiempo() != null ? m.getTiempo() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.valueOf(porcentaje) + "%")));

            com.itextpdf.layout.element.Cell estadoCell = new com.itextpdf.layout.element.Cell().add(new Paragraph(estado));
            estadoCell.setBackgroundColor(estadoColor);
            table.addCell(estadoCell);

            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(m.getSeccion() != null ? m.getSeccion() : "")));
        }

        document.add(table);
        document.close();

        response.getOutputStream().write(baos.toByteArray());
    }

    // ==================== GRÁFICA EN EXCEL ====================
    private void agregarGraficaExcel(Workbook workbook, List<Main> registros) {
        Sheet chartSheet = workbook.createSheet("Gráfica de Avance");

        long verdes = contarPorEstado(registros, "verde");
        long amarillos = contarPorEstado(registros, "amarillo");
        long rojos = contarPorEstado(registros, "rojo");
        long grises = contarPorEstado(registros, "gris");

        // Encabezados
        Row headerRow = chartSheet.createRow(0);
        headerRow.createCell(0).setCellValue("Estado");
        headerRow.createCell(1).setCellValue("Cantidad");

        // Datos
        Row row1 = chartSheet.createRow(1);
        row1.createCell(0).setCellValue("🟢 Aceptable");
        row1.createCell(1).setCellValue(verdes);

        Row row2 = chartSheet.createRow(2);
        row2.createCell(0).setCellValue("🟡 En riesgo");
        row2.createCell(1).setCellValue(amarillos);

        Row row3 = chartSheet.createRow(3);
        row3.createCell(0).setCellValue("🔴 Crítico");
        row3.createCell(1).setCellValue(rojos);

        Row row4 = chartSheet.createRow(4);
        row4.createCell(0).setCellValue("⚪ Fallo en planeación");
        row4.createCell(1).setCellValue(grises);

        // Total
        Row totalRow = chartSheet.createRow(5);
        totalRow.createCell(0).setCellValue("TOTAL");
        totalRow.createCell(1).setCellValue(verdes + amarillos + rojos + grises);

        // Auto ajustar columnas
        chartSheet.autoSizeColumn(0);
        chartSheet.autoSizeColumn(1);

        // Estilos
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);
    }

    // ==================== GRÁFICA EN PDF ====================
    private void agregarGraficaPDF(Document document, List<Main> registros) throws IOException {
        long verdes = contarPorEstado(registros, "verde");
        long amarillos = contarPorEstado(registros, "amarillo");
        long rojos = contarPorEstado(registros, "rojo");
        long grises = contarPorEstado(registros, "gris");

        document.add(new Paragraph("📊 Resumen de Estados").setFontSize(16).setBold().setMarginBottom(10));

        // Tabla de barras
        Table chartTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}));
        chartTable.setWidth(UnitValue.createPercentValue(100));
        chartTable.setMarginBottom(15);

        String[] estados = {"Aceptable", "En riesgo", "Crítico", "Fallo en planeación"};
        long[] valores = {verdes, amarillos, rojos, grises};
        com.itextpdf.kernel.colors.Color[] colores = {
                ColorConstants.GREEN,
                ColorConstants.ORANGE,
                ColorConstants.RED,
                ColorConstants.GRAY
        };

        long maxValor = 1;
        for (long v : valores) {
            if (v > maxValor) maxValor = v;
        }

        for (int i = 0; i < estados.length; i++) {
            com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph(estados[i] + "\n" + valores[i] + " registros")
                    .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(colores[i])
                    .setBorder(null);

            // Altura proporcional al valor
            if (maxValor > 0) {
                float altura = (float) (valores[i] * 30.0 / maxValor + 20);
                cell.setHeight(Math.max(altura, 30));
            }
            chartTable.addCell(cell);
        }
        document.add(chartTable);

        // Distribución porcentual
        long total = verdes + amarillos + rojos + grises;
        if (total > 0) {
            document.add(new Paragraph("📈 Distribución porcentual:").setFontSize(12).setBold().setMarginBottom(5));

            Table progressTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
            progressTable.setWidth(UnitValue.createPercentValue(100));

            String[] nombres = {"🟢 Aceptable", "🟡 En riesgo", "🔴 Crítico", "⚪ Fallo en planeación"};
            double[] porcentajes = {
                    (double) verdes / total * 100,
                    (double) amarillos / total * 100,
                    (double) rojos / total * 100,
                    (double) grises / total * 100
            };

            for (int i = 0; i < nombres.length; i++) {
                String barra = "█".repeat(Math.max(1, (int) (porcentajes[i] / 3)));
                progressTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new Paragraph(nombres[i])));
                progressTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new Paragraph(String.format("%.1f%% %s", porcentajes[i], barra))));
            }
            document.add(progressTable);
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private List<Main> obtenerRegistros(String seccion) {
        if (seccion != null && !seccion.isEmpty()) {
            String coleccionCompleta = seccion + "_";
            return mainRepository.findAll().stream()
                    .filter(m -> m.getSeccion() != null && m.getSeccion().startsWith(coleccionCompleta))
                    .collect(Collectors.toList());
        }
        return mainRepository.findAll();
    }

    private String getEstadoSemaforo(Double porcentaje) {
        if (porcentaje == null) return "Sin estado";
        if (porcentaje >= 85) return "Aceptable";
        if (porcentaje >= 60) return "En riesgo";
        if (porcentaje >= 30) return "Crítico";
        return "Fallo en planeación";
    }

    private com.itextpdf.kernel.colors.Color getColorSemaforo(Double porcentaje) {
        if (porcentaje == null) return ColorConstants.GRAY;
        if (porcentaje >= 85) return ColorConstants.GREEN;
        if (porcentaje >= 60) return ColorConstants.ORANGE;
        if (porcentaje >= 30) return ColorConstants.RED;
        return ColorConstants.GRAY;
    }

    private long contarPorEstado(List<Main> registros, String estado) {
        if (estado.equals("verde")) {
            return registros.stream().filter(m -> m.getPorcentaje() != null && m.getPorcentaje() >= 85).count();
        } else if (estado.equals("amarillo")) {
            return registros.stream().filter(m -> m.getPorcentaje() != null && m.getPorcentaje() >= 60 && m.getPorcentaje() < 85).count();
        } else if (estado.equals("rojo")) {
            return registros.stream().filter(m -> m.getPorcentaje() != null && m.getPorcentaje() >= 30 && m.getPorcentaje() < 60).count();
        } else if (estado.equals("gris")) {
            return registros.stream().filter(m -> m.getPorcentaje() == null || m.getPorcentaje() < 30).count();
        }
        return 0;
    }

    // ==================== ESTILOS PARA EXCEL ====================

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

    private CellStyle getEstiloPorEstado(Workbook workbook, Double porcentaje,
                                         CellStyle verde, CellStyle amarillo,
                                         CellStyle rojo, CellStyle gris) {
        if (porcentaje == null) return gris;
        if (porcentaje >= 85) return verde;
        if (porcentaje >= 60) return amarillo;
        if (porcentaje >= 30) return rojo;
        return gris;
    }
}