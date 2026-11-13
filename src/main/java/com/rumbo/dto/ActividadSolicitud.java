package com.rumbo.dto;

public record ActividadSolicitud(
        String fecha,
        String descripcion,
        String contexto,
        String instrucciones,
        String accionesARealizar,
        String metodoRecomendado,
        String herramientasDisponibles,
        String informacionAGuardar,
        String tipoActividad
) {
}
