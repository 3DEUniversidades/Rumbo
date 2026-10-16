package com.rumbo.dto;

public class RegistroResponseDTO {

    private String idUsuario;
    private String nombreUsuario;
    private String nombreCompleto;
    private String correo;
    private String rol;
    private String estadoVerificacion;

    public RegistroResponseDTO(String idUsuario, String nombreUsuario, String nombreCompleto,
                               String correo, String rol, String estadoVerificacion) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.rol = rol;
        this.estadoVerificacion = estadoVerificacion;
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

    public String getCorreo() {
        return correo;
    }

    public String getRol() {
        return rol;
    }

    public String getEstadoVerificacion() {
        return estadoVerificacion;
    }
}
