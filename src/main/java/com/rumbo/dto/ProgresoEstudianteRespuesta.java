package com.rumbo.dto;

public record ProgresoEstudianteRespuesta(
        long totalActividades,
        long actividadesCompletadas,
        double porcentaje
) {
}