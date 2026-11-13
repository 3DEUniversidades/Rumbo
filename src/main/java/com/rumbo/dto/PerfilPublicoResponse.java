package com.rumbo.dto;

public class PerfilPublicoResponse {

    private final String idUsuario;
    private final String nombreUsuario;
    private final String nombreCompleto;
    private final String descripcionPersonal;
    private final String fotoUrl;

    public PerfilPublicoResponse(String idUsuario, String nombreUsuario,
                                 String nombreCompleto, String descripcionPersonal,
                                 String fotoUrl) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.descripcionPersonal = descripcionPersonal;
        this.fotoUrl = fotoUrl;
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

    public String getFotoUrl() {
        return fotoUrl;
    }
}
