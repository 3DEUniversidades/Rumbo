package com.rumbo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record EntradaBitacoraRespuesta(
        String idEntrada,
        String contenido,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        LocalDateTime fechaCreacion,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        LocalDateTime fechaEdicion,
        String idActividad,
        String descripcionActividad
) {
}
