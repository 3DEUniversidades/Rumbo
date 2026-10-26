package com.rumbo.servicio;

import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
public class GestorPermisos {

    public static final String ATRIBUTO_SESION = "sesionUsuario";
    public static final String ACCION_SOLICITAR_VERIFICACION = "solicitarVerificacion";
    public static final String ACCION_TABLERO_SUPERVISION = "tableroSupervision";
    private static final Map<String, Rol> ROLES_POR_ACCION = Map.of(
            ACCION_SOLICITAR_VERIFICACION, Rol.ESTUDIANTE
    );

    public boolean puedeSupervisarUsuarios(Rol rol) {
        return rol == Rol.SUPERVISOR_DE_USUARIOS || rol == Rol.ADMIN;
    }

    public SesionUsuario exigirSesion(HttpSession sesion) {
        SesionUsuario sesionUsuario = (SesionUsuario) sesion.getAttribute(ATRIBUTO_SESION);
        if (sesionUsuario == null || !sesionUsuario.isActiva()) {
            throw new ExcepcionAutenticacion("Debe iniciar sesion");
        }
        return sesionUsuario;
    }

    public SesionUsuario exigirRol(HttpSession sesion, Rol... rolesPermitidos) {
        SesionUsuario sesionUsuario = exigirSesion(sesion);
        boolean tienePermiso = Arrays.stream(rolesPermitidos)
                .anyMatch(rolPermitido -> rolPermitido == sesionUsuario.getRol());

        if (!tienePermiso) {
            throw new ExcepcionPermiso("No tiene permisos para realizar esta accion");
        }

        return sesionUsuario;
    }

    public SesionUsuario exigirAccion(HttpSession sesion, String accion) {
        Rol rolPermitido = ROLES_POR_ACCION.get(accion);
        if (rolPermitido == null) {
            throw new ExcepcionPermiso("La accion no esta configurada");
        }
        return exigirRol(sesion, rolPermitido);
    }

    public boolean puedeAccederAlForo(String rolUsuario, String estadoVerificacion) {
        if ("COLABORADOR".equalsIgnoreCase(rolUsuario)
                || "ADMIN".equalsIgnoreCase(rolUsuario)) {
            return true;
        }
        return "ESTUDIANTE".equalsIgnoreCase(rolUsuario)
                && "VERIFICADO".equalsIgnoreCase(estadoVerificacion);
    }

}
