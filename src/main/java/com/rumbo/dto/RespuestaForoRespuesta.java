package com.rumbo.dto;

import java.time.Instant;

public record RespuestaForoRespuesta(
        Long id,
        String contenido,
        Instant fecha,
        AutorForoRespuesta autor
) {
}