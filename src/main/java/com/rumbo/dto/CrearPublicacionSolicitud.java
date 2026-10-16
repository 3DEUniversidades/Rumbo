package com.rumbo.dto;

import java.util.List;

public record CrearPublicacionSolicitud(
        String titulo,
        String contenido,
        String tipo,
        List<Long> carrerasIds
) {
}
