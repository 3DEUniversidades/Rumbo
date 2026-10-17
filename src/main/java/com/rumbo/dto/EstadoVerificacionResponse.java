package com.rumbo.dto;

public class EstadoVerificacionResponse {

    private final String estadoVerificacion;
    private final boolean solicitudPendiente;
    private final String estadoUltimaSolicitud;
    private final String mensaje;

    public EstadoVerificacionResponse(String estadoVerificacion, boolean solicitudPendiente,
                                      String estadoUltimaSolicitud, String mensaje) {
        this.estadoVerificacion = estadoVerificacion;
        this.solicitudPendiente = solicitudPendiente;
        this.estadoUltimaSolicitud = estadoUltimaSolicitud;
        this.mensaje = mensaje;
    }

    public String getEstadoVerificacion() {
        return estadoVerificacion;
    }

    public boolean isSolicitudPendiente() {
        return solicitudPendiente;
    }

    public String getEstadoUltimaSolicitud() {
        return estadoUltimaSolicitud;
    }

    public String getMensaje() {
        return mensaje;
    }
}
