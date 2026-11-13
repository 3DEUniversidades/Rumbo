package com.rumbo.dto;

public class SolicitudActualizacionPerfilResponse {

    private final String idSolicitud;
    private final String nombreUsuarioSolicitado;
    private final String nombreCompletoSolicitado;
    private final String estado;
    private final String fechaSolicitud;

    public SolicitudActualizacionPerfilResponse(String idSolicitud,
                                                String nombreUsuarioSolicitado,
                                                String nombreCompletoSolicitado,
                                                String estado,
                                                String fechaSolicitud) {
        this.idSolicitud = idSolicitud;
        this.nombreUsuarioSolicitado = nombreUsuarioSolicitado;
        this.nombreCompletoSolicitado = nombreCompletoSolicitado;
        this.estado = estado;
        this.fechaSolicitud = fechaSolicitud;
    }

    public String getIdSolicitud() {
        return idSolicitud;
    }

    public String getNombreUsuarioSolicitado() {
        return nombreUsuarioSolicitado;
    }

    public String getNombreCompletoSolicitado() {
        return nombreCompletoSolicitado;
    }

    public String getEstado() {
        return estado;
    }

    public String getFechaSolicitud() {
        return fechaSolicitud;
    }
}
