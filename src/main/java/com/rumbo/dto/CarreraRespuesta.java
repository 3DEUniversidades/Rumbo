package com.rumbo.dto;

import java.math.BigDecimal;

public record CarreraRespuesta(
        Long id,
        String nombre,
        Integer duracion,
        BigDecimal costo,
        String modalidad,
        Integer cargaCreditos,
        String habilidadesRequeridas,
        String oportunidadesLaborales,
        BigDecimal salarioEstimado,
        BigDecimal empleabilidad,
        String pdfOficialUrl,
        String urlCarreraOficial
) {
}

