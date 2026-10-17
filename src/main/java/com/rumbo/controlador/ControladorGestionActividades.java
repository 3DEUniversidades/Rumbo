package com.rumbo.controlador;

import com.rumbo.modelo.Actividad;
import com.rumbo.modelo.Rol;
import com.rumbo.servicio.ActividadService;
import com.rumbo.servicio.GestorPermisos;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/actividades")
public class ControladorGestionActividades {

    private final ActividadService actividadService;
    private final GestorPermisos gestorPermisos;

    public ControladorGestionActividades(
            ActividadService actividadService,
            GestorPermisos gestorPermisos
    ) {
        this.actividadService = actividadService;
        this.gestorPermisos = gestorPermisos;
    }

    @GetMapping
    public List<Actividad> obtenerLineaDeTiempo(HttpSession sesion) {
        gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);
        return actividadService.obtenerLineaDeTiempo();
    }
}