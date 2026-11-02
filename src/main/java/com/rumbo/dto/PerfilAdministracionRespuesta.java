package com.rumbo.dto;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.EstadoVerificacion;
public record PerfilAdministracionRespuesta(String idUsuario, String nombreUsuario,
        String nombreCompleto, String correo, Rol rol, EstadoVerificacion estadoVerificacion,
        boolean perfilDisponible, String nivelEstudios, String descripcionPersonal,
        String areaLaboral, Long idCarrera, String nombreCarrera, String fotoUrl) {}
