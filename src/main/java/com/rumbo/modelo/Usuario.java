package com.rumbo.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private String idUsuario;
    private String nombreUsuario;
    private String nombreCompleto;
    private String correo;
    private String contrasenia;
    private String rol;
    private String estadoVerificacion;

    protected Usuario() {
    }

    public Usuario(String idUsuario, String nombreUsuario, String nombreCompleto,
                   String correo, String contrasenia, String rol, String estadoVerificacion) {
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

    public String getCorreo() {
        return correo;
    }

    public String getRol() {
        return rol;
    }

    public String getEstadoVerificacion() {
        return estadoVerificacion;
    }

    public void setEstadoVerificacion(String estadoVerificacion) {
        this.estadoVerificacion = estadoVerificacion;
    }

    public boolean verificarContrasenia(String contraseniaIngresada) {
        return new BCryptPasswordEncoder().matches(contraseniaIngresada, this.contrasenia);
    }
}
