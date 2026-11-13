package com.rumbo.dto;
public record RegistroHistorialAdministrativoRespuesta(String idRegistro, OrigenHistorialAdministrativo origen,
        String accion, String fecha, String idResponsable, String responsable, String objetivo,
        String detalle, String motivo, String valorAnterior, String valorNuevo) {}
