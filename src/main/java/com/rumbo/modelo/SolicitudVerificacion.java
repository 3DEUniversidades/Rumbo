package com.rumbo.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "solicitud_verificacion")
public class SolicitudVerificacion {

    public static final String ESTADO_PENDIENTE = "pendiente";
    public static final String ESTADO_APROBADA = "aprobada";
    public static final String ESTADO_RECHAZADA = "rechazada";

    @Id
    private String idSolicitud;
    private String idUsuario;
    private String estado;
    private String fechaSolicitud;
    private String idSupervisor;
    private String fechaDecision;
    private String motivoDecision;

    protected SolicitudVerificacion() {
    }

    public SolicitudVerificacion(String idSolicitud, String idUsuario, String estado,
                                 String fechaSolicitud) {
        this.idSolicitud = idSolicitud;
        this.idUsuario = idUsuario;
        this.estado = estado;
        this.fechaSolicitud = fechaSolicitud;
    }

    public String getIdSolicitud() {
        return idSolicitud;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public String getEstado() {
        return estado;
    }

    public String getFechaSolicitud() {
        return fechaSolicitud;
    }

    public String getIdSupervisor() {
        return idSupervisor;
    }

    public String getFechaDecision() {
        return fechaDecision;
    }

    public String getMotivoDecision() {
        return motivoDecision;
    }

    public void decidir(String estado, String idSupervisor, String fechaDecision,
                       String motivoDecision) {
        this.estado = estado;
        this.idSupervisor = idSupervisor;
        this.fechaDecision = fechaDecision;
        this.motivoDecision = motivoDecision;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "SolicitudVerificacion{" +
                "idSolicitud='" + idSolicitud + '\'' +
                ", idUsuario='" + idUsuario + '\'' +
                ", estado='" + estado + '\'' +
                ", fechaSolicitud='" + fechaSolicitud + '\'' +
                '}';
    }
}
