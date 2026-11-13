package com.rumbo.dto;

public record AdjuntoBitacoraRespuesta(
        String idAdjunto,
        String nombreOriginal,
        String tipoMime,
        long tamanoBytes,
        boolean esImagen,
        String urlContenido
) {
}
