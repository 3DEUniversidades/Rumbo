package com.rumbo.dto;

import java.math.BigDecimal;

public record CarreraRespuesta(
        Long id,
        String nombre,
        Integer duracion,
        String pensum,
        BigDecimal costo
) {
}
