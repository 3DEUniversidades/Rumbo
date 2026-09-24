package com.rumbo.dto;

public class LoginResponseDTO {

    private String idUsuario;
    private String nombreCompleto;
    private String rol;

    public LoginResponseDTO(String idUsuario, String nombreCompleto, String rol) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
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
}
