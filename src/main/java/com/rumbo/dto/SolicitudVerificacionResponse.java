package com.rumbo.dto;

public class SolicitudVerificacionResponse {

    private final String idSolicitud;
    private final String estado;
    private final String fechaSolicitud;

    public SolicitudVerificacionResponse(String idSolicitud, String estado, String fechaSolicitud) {
        this.idSolicitud = idSolicitud;
        this.estado = estado;
        this.fechaSolicitud = fechaSolicitud;
    }

    public String getIdSolicitud() {
        return idSolicitud;
    }

    public String getEstado() {
        return estado;
    }

    public String getFechaSolicitud() {
        return fechaSolicitud;
    }
}
