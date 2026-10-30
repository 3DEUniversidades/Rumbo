package com.rumbo.controlador;

import com.rumbo.dto.ActividadSolicitud;
import com.rumbo.modelo.Actividad;
import com.rumbo.modelo.Rol;
import com.rumbo.servicio.ActividadService;
import com.rumbo.servicio.ExcepcionActividad;
import com.rumbo.servicio.GestorPermisos;
import com.rumbo.servicio.ValidadorDatos;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/actividades")
public class ControladorGestionActividades {
    private final ActividadService actividadService;
    private final GestorPermisos gestorPermisos;
    private final ValidadorDatos validadorDatos;

    public ControladorGestionActividades(ActividadService actividadService,
            GestorPermisos gestorPermisos, ValidadorDatos validadorDatos) {
        this.actividadService = actividadService;
        this.gestorPermisos = gestorPermisos;
        this.validadorDatos = validadorDatos;
    }

    @GetMapping
    public List<Actividad> obtenerLineaDeTiempo(HttpSession sesion) {
        gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE, Rol.ENCARGADO_DE_CARRERAS, Rol.ADMIN);
        return actividadService.obtenerLineaDeTiempo();
    }

    @GetMapping("/{id}")
    public Actividad consultar(@PathVariable String id, HttpSession sesion) {
        gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE, Rol.ENCARGADO_DE_CARRERAS, Rol.ADMIN);
        return actividadService.consultarActividad(id);
    }

    @PostMapping
    public ResponseEntity<Actividad> crear(@RequestBody ActividadSolicitud solicitud, HttpSession sesion) {
        gestorPermisos.exigirRol(sesion, Rol.ENCARGADO_DE_CARRERAS, Rol.ADMIN);
        validadorDatos.exigirActividadValida(solicitud);
        Actividad actividad = actividadService.crearActividad(solicitud);
        return ResponseEntity.created(URI.create("/api/actividades/" + actividad.getIdActividad()))
                .body(actividad);
    }

    @PutMapping("/{id}")
    public Actividad actualizar(@PathVariable String id,
            @RequestBody ActividadSolicitud solicitud, HttpSession sesion) {
        gestorPermisos.exigirRol(sesion, Rol.ENCARGADO_DE_CARRERAS, Rol.ADMIN);
        validadorDatos.exigirActividadValida(solicitud);
        return actividadService.actualizarActividad(id, solicitud);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id, HttpSession sesion) {
        gestorPermisos.exigirRol(sesion, Rol.ENCARGADO_DE_CARRERAS, Rol.ADMIN);
        actividadService.eliminarActividad(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ExcepcionActividad.class)
    public ResponseEntity<Map<String, String>> manejarNoEncontrada(ExcepcionActividad e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }
}
