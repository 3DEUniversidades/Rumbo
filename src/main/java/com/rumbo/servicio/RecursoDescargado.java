package com.rumbo.servicio;

import java.io.InputStream;

public record RecursoDescargado(
        InputStream contenido,
        long tamanoBytes,
        String tipoMime
) {
}
