package com.rumbo.dto;
import java.time.Instant;
public record PublicacionAdministracionRespuesta(Long id, String titulo, String contenido,
        String estado, String idAutor, String nombreUsuarioAutor, Instant fecha) {}
