package com.rumbo.dto;

import java.time.Instant;

public record RevisionMensajeRespuesta(
        Long id,
        Long publicacionId,
        String tituloPublicacion,
        String decision,
        Instant fecha,
        AutorForoRespuesta supervisor
) {
}