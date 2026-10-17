package com.rumbo.dto;

public class PerfilPublicoResponse {

    private final String idUsuario;
    private final String nombreUsuario;
    private final String nombreCompleto;
    private final String descripcionPersonal;

    public PerfilPublicoResponse(String idUsuario, String nombreUsuario,
                                 String nombreCompleto, String descripcionPersonal) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.descripcionPersonal = descripcionPersonal;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getDescripcionPersonal() {
        return descripcionPersonal;
    }
}
