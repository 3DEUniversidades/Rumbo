package com.rumbo.dto;

import com.rumbo.modelo.EstadoActividad;
import java.time.LocalDateTime;

public record ActividadEstudianteRespuesta(
        String idActividad,
        String fecha,
        String descripcion,
        String contexto,
        String instrucciones,
        String accionesARealizar,
        String metodoRecomendado,
        String herramientasDisponibles,
        String informacionAGuardar,
        String tipoActividad,
        Boolean decidioRealizar,
        String razonDecision,
        EstadoActividad estado,
        String resultadosRegistrados,
        LocalDateTime fechaActualizacion
) {
}