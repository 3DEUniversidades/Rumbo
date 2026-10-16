package com.rumbo.dto;

public class LoginResponseDTO {

    private String idUsuario;
    private String nombreCompleto;
    private String rol;
    private String rutaPantalla;

    public LoginResponseDTO(String idUsuario, String nombreCompleto, String rol, String rutaPantalla) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.rutaPantalla = rutaPantalla;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getRol() {
        return rol;
    }

    public String getRutaPantalla() {
        return rutaPantalla;
    }
}
