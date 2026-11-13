package com.rumbo.dto;

import java.time.LocalDateTime;

public record RegistroActualizacionRespuesta(
        Long id,
        Long carreraId,
        String carreraNombre,
        String campo,
        String valorAnterior,
        String valorNuevo,
        LocalDateTime fecha,
        String responsable
) {
}