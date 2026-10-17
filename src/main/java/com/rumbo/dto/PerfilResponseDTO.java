package com.rumbo.dto;

public class PerfilResponseDTO {

    private final String idUsuario;
    private final String nombreUsuario;
    private final String nombreCompleto;
    private final String correo;
    private final String rol;
    private final String nivelEstudios;
    private final Long carreraId;
    private final String carreraNombre;
    private final String areaLaboral;
    private final String descripcionPersonal;

    public PerfilResponseDTO(String idUsuario, String nombreUsuario, String nombreCompleto,
                             String correo, String rol,
                             String nivelEstudios, Long carreraId, String carreraNombre,
                             String areaLaboral, String descripcionPersonal) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.rol = rol;
        this.nivelEstudios = nivelEstudios;
        this.carreraId = carreraId;
        this.carreraNombre = carreraNombre;
        this.areaLaboral = areaLaboral;
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

    public String getCorreo() {
        return correo;
    }

    public String getRol() {
        return rol;
    }

    public String getNivelEstudios() {
        return nivelEstudios;
    }

    public Long getCarreraId() {
        return carreraId;
    }

    public String getCarreraNombre() {
        return carreraNombre;
    }

    public String getAreaLaboral() {
        return areaLaboral;
    }

    public String getDescripcionPersonal() {
        return descripcionPersonal;
    }
}
