package com.rumbo.dto;
import com.rumbo.modelo.Rol;
import java.time.Instant;
public record RegistroAdministracionRespuesta(String idRegistro, String idAdministrador,
        String nombreAdministrador, String idObjetivo, String usuarioObjetivo,
        Rol rolObjetivo, String accion, String motivo, Instant fecha, Long idPublicacion, String tituloPublicacion) {}
