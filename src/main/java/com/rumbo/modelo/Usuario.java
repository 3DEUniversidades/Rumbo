package com.rumbo.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private String idUsuario;

    @Column(nullable = false, unique = true)
    private String nombreUsuario;

    private String nombreCompleto;

    @Column(nullable = false, unique = true)
    private String correo;

    private String contrasenia;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    private EstadoVerificacion estadoVerificacion;

    protected Usuario() {
    }

    public Usuario(String idUsuario, String nombreUsuario, String nombreCompleto,
                   String correo, String contrasenia, Rol rol, EstadoVerificacion estadoVerificacion) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.contrasenia = contrasenia;
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

    public void actualizarDatosCuenta(String nombreUsuario, String nombreCompleto) {
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public Rol getRol() {
        return rol;
    }

    public EstadoVerificacion getEstadoVerificacion() {
        return estadoVerificacion;
    }

    public void setEstadoVerificacion(EstadoVerificacion estadoVerificacion) {
        this.estadoVerificacion = estadoVerificacion;
    }

    @JsonIgnore
    public String getContrasenia() {
        return contrasenia;
    }
}
