package com.rumbo.controlador;

import com.rumbo.modelo.Actividad;
import com.rumbo.servicio.ActividadService;
import com.rumbo.servicio.ExcepcionActividad;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/actividades")
public class ControladorGestionActividades {

    private final ActividadService actividadService;

    public ControladorGestionActividades(ActividadService actividadService) {
        this.actividadService = actividadService;
    }

    @GetMapping
    public List<Actividad> obtenerLineaDeTiempo() {
        return actividadService.obtenerLineaDeTiempo();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleCompletada(@PathVariable String id) {
        try {
            Actividad actividad = actividadService.marcarCompletada(id);
            return ResponseEntity.ok(actividad);
        } catch (ExcepcionActividad e) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
