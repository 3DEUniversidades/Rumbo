package com.rumbo.servicio;

import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class GestorPermisos {

    public static final String ATRIBUTO_SESION = "sesionUsuario";

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
}
