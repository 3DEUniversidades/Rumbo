package com.rumbo.servicio;

import com.rumbo.dto.RegistroRequestDTO;
import com.rumbo.modelo.Rol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ValidadorDatos {

    public static final int LONGITUD_MINIMA_CONTRASENIA = 8;

    private static final Pattern FORMATO_CORREO =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final String dominioInstitucional;

    public ValidadorDatos(
            @Value("${rumbo.correo.dominio-institucional:uvg.edu.gt}") String dominioInstitucional) {
        this.dominioInstitucional = dominioInstitucional;
    }

    public String getDominioInstitucional() {
        return dominioInstitucional;
    }

    public boolean validarCorreo(String correo) {
        return correo != null && FORMATO_CORREO.matcher(correo.trim()).matches();
    }

    public boolean validarCorreoInstitucional(String correo) {
        if (!validarCorreo(correo)) {
            return false;
        }
        String correoNormalizado = correo.trim();
        String dominio = correoNormalizado.substring(correoNormalizado.lastIndexOf('@') + 1);
        return dominio.equalsIgnoreCase(dominioInstitucional);
    }

    public boolean validarCampoObligatorio(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    public String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }

    public RegistroRequestDTO normalizarRegistro(RegistroRequestDTO solicitud) {
        if (solicitud == null) {
            return null;
        }
        solicitud.setNombreUsuario(normalizar(solicitud.getNombreUsuario()));
        solicitud.setNombreCompleto(normalizar(solicitud.getNombreCompleto()));
        solicitud.setCorreo(normalizar(solicitud.getCorreo()));
        solicitud.setRol(normalizar(solicitud.getRol()));
        return solicitud;
    }

    public Map<String, String> validarRegistro(RegistroRequestDTO solicitud) {
        Map<String, String> errores = new LinkedHashMap<>();

        if (solicitud == null) {
            errores.put("general", "No se recibieron datos para el registro");
            return errores;
        }

        if (!validarCampoObligatorio(solicitud.getNombreUsuario())) {
            errores.put("nombreUsuario", "El nombre de usuario es obligatorio");
        }

        if (!validarCampoObligatorio(solicitud.getNombreCompleto())) {
            errores.put("nombreCompleto", "El nombre completo es obligatorio");
        }

        if (!validarCampoObligatorio(solicitud.getCorreo())) {
            errores.put("correo", "El correo electronico es obligatorio");
        } else if (!validarCorreo(solicitud.getCorreo())) {
            errores.put("correo", "El correo electronico no tiene un formato valido");
        }

        if (!validarCampoObligatorio(solicitud.getContrasenia())) {
            errores.put("contrasenia", "La contrasenia es obligatoria");
        } else if (solicitud.getContrasenia().length() < LONGITUD_MINIMA_CONTRASENIA) {
            errores.put("contrasenia",
                    "La contrasenia debe tener al menos " + LONGITUD_MINIMA_CONTRASENIA + " caracteres");
        }

        if (!validarCampoObligatorio(solicitud.getRol())) {
            errores.put("rol", "El rol es obligatorio");
        } else {
            Rol rol = Rol.desdeTexto(solicitud.getRol());
            if (rol == null || !rol.esPublico()) {
                errores.put("rol", "El rol " + solicitud.getRol() + " no puede registrarse publicamente");
            }
        }

        if (Rol.COLABORADOR == Rol.desdeTexto(solicitud.getRol())
                && validarCampoObligatorio(solicitud.getCorreo())
                && validarCorreo(solicitud.getCorreo())
                && !validarCorreoInstitucional(solicitud.getCorreo())) {
            errores.put("correo",
                    "El correo de un colaborador debe ser institucional de la UVG (@" + dominioInstitucional + ")");
        }

        return errores;
    }
}
