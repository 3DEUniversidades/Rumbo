package com.rumbo.dto;

public record SolicitudSupervisionResponse(
        String idSolicitud,
        String idUsuario,
        String nombreUsuario,
        String nombreCompleto,
        String correo,
        String rol,
        String nivelEstudios,
        Long carreraId,
        String carreraNombre,
        String areaLaboral,
        String descripcionPersonal,
        String nombreUsuarioSolicitado,
        String nombreCompletoSolicitado,
        String estado,
        String fechaSolicitud) {
}
