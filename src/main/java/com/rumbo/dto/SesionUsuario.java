package com.rumbo.dto;

public class SesionUsuario {

    private String idUsuario;
    private String fechaInicio;
    private boolean activa;

    public SesionUsuario(String idUsuario, String fechaInicio) {
        this.idUsuario = idUsuario;
        this.fechaInicio = fechaInicio;
        this.activa = true;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public boolean getActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
