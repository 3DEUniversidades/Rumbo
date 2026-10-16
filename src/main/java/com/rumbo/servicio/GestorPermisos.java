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
    private static final Map<Rol, String> RUTAS_POR_ROL = Map.of(
            Rol.ESTUDIANTE, "/roles/estudiante.html",
            Rol.COLABORADOR, "/roles/colaborador.html",
            Rol.SUPERVISOR_DE_USUARIOS, "/roles/supervisor-usuarios.html",
            Rol.SUPERVISOR_DE_MENSAJES, "/roles/supervisor-mensajes.html",
            Rol.ENCARGADO_DE_CARRERAS, "/roles/encargado-carreras.html",
            Rol.ADMIN, "/roles/administrador.html"
    );

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

    public String rutaParaRol(Rol rol) {
        String ruta = RUTAS_POR_ROL.get(rol);
        if (ruta == null) {
            throw new ExcepcionPermiso("El rol no tiene una pantalla asignada");
        }
        return ruta;
    }

}
