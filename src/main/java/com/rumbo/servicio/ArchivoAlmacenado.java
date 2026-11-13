package com.rumbo.servicio;

public record ArchivoAlmacenado(
        String publicId,
        String tipoRecurso,
        String tipoEntrega,
        String formato,
        String nombreOriginal,
        String tipoMime,
        long tamanoBytes
) {
}
