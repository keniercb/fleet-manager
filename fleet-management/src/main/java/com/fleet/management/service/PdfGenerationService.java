package com.fleet.management.service;

import java.util.Map;

/**
 * Servicio generico para generar documentos PDF a partir de plantillas Thymeleaf.
 */
public interface PdfGenerationService {

    /**
     * Genera un PDF a partir de una plantilla Thymeleaf.
     *
     * @param templatePath ruta de la plantilla relativa a templates/ (ej: "reports/vehiculos-listado")
     * @param model        variables que se inyectaran en la plantilla
     * @return byte[] con el contenido del PDF generado
     */
    byte[] generatePdf(String templatePath, Map<String, Object> model);
}
