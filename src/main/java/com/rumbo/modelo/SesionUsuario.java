package com.rumbo.modelo;

public class SesionUsuario {

    private final String idUsuario;
    private final String nombreCompleto;
    private final Rol rol;
    private final String fechaInicio;
    private boolean activa;

    public SesionUsuario(String idUsuario, String nombreCompleto, Rol rol, String fechaInicio) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.fechaInicio = fechaInicio;
        this.activa = true;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public Rol getRol() {
        return rol;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
