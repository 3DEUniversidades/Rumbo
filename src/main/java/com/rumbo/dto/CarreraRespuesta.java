package com.rumbo.dto;

import java.math.BigDecimal;
import java.util.List;

public record CarreraRespuesta(
        Long id,
        String nombre,
        String facultad,
        String descripcion,
        Integer duracion,
        BigDecimal costo,
        String costoPeriodo,
        Integer costoAnio,
        String modalidad,
        Integer cargaCreditos,
        List<String> areasFormacion,
        List<String> competenciasDesarrolladas,
        List<String> oportunidadesLaborales,
        BigDecimal salarioEstimado,
        BigDecimal empleabilidad,
        String contextoLaboral,
        String pdfOficialUrl,
        String urlCarreraOficial
) {
}
