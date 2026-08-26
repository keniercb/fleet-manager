package com.fleet.management.service.impl;

import com.fleet.management.service.PdfGenerationService;
import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGenerationServiceImpl implements PdfGenerationService {

    private final TemplateEngine templateEngine;

    @Override
    public byte[] generatePdf(String templatePath, Map<String, Object> model) {
        try {
            // 1. Procesar la plantilla Thymeleaf con el modelo
            Context context = new Context();
            context.setVariables(model);
            String html = templateEngine.process(templatePath, context);

            // 2. Renderizar HTML a PDF con Flying Saucer
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();

            // 3. Cargar el HTML procesado en el renderer
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);

            return outputStream.toByteArray();
        } catch (DocumentException e) {
            log.error("Error generando PDF desde plantilla: {}", templatePath, e);
            throw new RuntimeException("Error generando el documento PDF", e);
        }
    }
}
