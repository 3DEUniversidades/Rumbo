package com.rumbo.dto;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.EstadoVerificacion;
public record UsuarioAdministracionResumen(String idUsuario, String nombreUsuario,
        String nombreCompleto, Rol rol, EstadoVerificacion estadoVerificacion) {}
