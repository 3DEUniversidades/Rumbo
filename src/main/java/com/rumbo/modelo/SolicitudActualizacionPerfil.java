package com.rumbo.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "solicitud_actualizacion_perfil")
public class SolicitudActualizacionPerfil {

    public static final String ESTADO_PENDIENTE = "pendiente";

    @Id
    private String idSolicitud;
    private String idUsuario;
    private String nombreUsuarioSolicitado;
    private String nombreCompletoSolicitado;
    private String estado;
    private String fechaSolicitud;

    protected SolicitudActualizacionPerfil() {
    }

    public SolicitudActualizacionPerfil(String idSolicitud, String idUsuario,
                                        String nombreUsuarioSolicitado,
                                        String nombreCompletoSolicitado,
                                        String estado, String fechaSolicitud) {
        this.idSolicitud = idSolicitud;
        this.idUsuario = idUsuario;
        this.nombreUsuarioSolicitado = nombreUsuarioSolicitado;
        this.nombreCompletoSolicitado = nombreCompletoSolicitado;
        this.estado = estado;
        this.fechaSolicitud = fechaSolicitud;
    }

    public String getIdSolicitud() {
        return idSolicitud;
    }

    public String getIdUsuario() {
        return idUsuario;
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
