package com.fleet.management.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reportes")
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {
    // Los endpoints de reportes de transporte se encuentran en ReporteTransporteController
    // bajo /api/reportes-transporte/*
}