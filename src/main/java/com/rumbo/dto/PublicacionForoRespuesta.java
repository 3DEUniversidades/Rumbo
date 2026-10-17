package com.rumbo.dto;

import java.time.Instant;
import java.util.List;

public record PublicacionForoRespuesta(
        Long id,
        String titulo,
        String contenido,
        String tipo,
        Instant fecha,
        String estado,
        AutorForoRespuesta autor,
        List<CarreraResumen> carreras
) {
    public record CarreraResumen(Long id, String nombre) {
    }
}