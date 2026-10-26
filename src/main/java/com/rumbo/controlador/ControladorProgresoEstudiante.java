package com.rumbo.controlador;

import com.rumbo.dto.DecisionActividadSolicitud;
import com.rumbo.dto.ResultadosActividadSolicitud;
import com.rumbo.dto.RazonActividadSolicitud;
import com.rumbo.dto.ActividadEstudianteRespuesta;
import com.rumbo.dto.ProgresoEstudianteRespuesta;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.servicio.ExcepcionActividad;
import com.rumbo.servicio.GestorPermisos;
import com.rumbo.servicio.ProgresoEstudianteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mi-progreso")
public class ControladorProgresoEstudiante {

    private final ProgresoEstudianteService progresoService;
    private final GestorPermisos gestorPermisos;

    public ControladorProgresoEstudiante(
            ProgresoEstudianteService progresoService,
            GestorPermisos gestorPermisos
    ) {
        this.progresoService = progresoService;
        this.gestorPermisos = gestorPermisos;
    }

    @GetMapping("/actividades")
    public List<ActividadEstudianteRespuesta> consultarActividades(
            HttpSession sesion
    ) {
        SesionUsuario estudiante =
                gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

        return progresoService.consultarLineaDeTiempo(
                estudiante.getIdUsuario()
        );
    }

    @GetMapping("/actividades/{idActividad}")
    public ActividadEstudianteRespuesta consultarDetalle(
            @PathVariable String idActividad,
            HttpSession sesion
    ) {
        SesionUsuario estudiante =
                gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

        return progresoService.consultarDetalleActividad(
                estudiante.getIdUsuario(),
                idActividad
        );
    }

    @GetMapping("/resumen")
    public ProgresoEstudianteRespuesta consultarResumen(
            HttpSession sesion
    ) {
        SesionUsuario estudiante =
                gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

        return progresoService.consultarProgresoGeneral(
                estudiante.getIdUsuario()
        );
    }

@PutMapping("/actividades/{idActividad}/decision")
public ResponseEntity<Void> registrarDecision(
        @PathVariable String idActividad,
        @RequestBody DecisionActividadSolicitud solicitud,
        HttpSession sesion
) {
    SesionUsuario estudiante =
            gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

    if (solicitud.decidioRealizar() == null) {
        throw new IllegalArgumentException(
                "Debes indicar si realizarás la actividad"
        );
    }

    if (solicitud.razon() == null || solicitud.razon().isBlank()) {
        throw new IllegalArgumentException(
                "Debes indicar la razón de tu decisión"
        );
    }

    progresoService.registrarDecision(
            estudiante.getIdUsuario(),
            idActividad,
            solicitud.decidioRealizar(),
            solicitud.razon()
    );

    return ResponseEntity.noContent().build();
}

@PutMapping("/actividades/{idActividad}/completada")
public ResponseEntity<Void> marcarCompletada(
        @PathVariable String idActividad,
        HttpSession sesion
) {
    SesionUsuario estudiante =
            gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

    progresoService.marcarCompletada(
            estudiante.getIdUsuario(),
            idActividad
    );

    return ResponseEntity.noContent().build();
}

@PutMapping("/actividades/{idActividad}/resultados")
public ResponseEntity<Void> guardarResultados(
        @PathVariable String idActividad,
        @RequestBody ResultadosActividadSolicitud solicitud,
        HttpSession sesion
) {
    SesionUsuario estudiante =
            gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

    if (solicitud.resultados() == null) {
        throw new IllegalArgumentException(
                "Debes enviar el contenido de los resultados"
        );
    }

    progresoService.guardarResultados(
            estudiante.getIdUsuario(),
            idActividad,
            solicitud.resultados()
    );

    return ResponseEntity.noContent().build();
}

@PutMapping("/actividades/{idActividad}/no-realizada")
public ResponseEntity<Void> marcarNoRealizada(
        @PathVariable String idActividad,
        @RequestBody RazonActividadSolicitud solicitud,
        HttpSession sesion
) {
    SesionUsuario estudiante =
            gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

    if (solicitud.razon() == null || solicitud.razon().isBlank()) {
        throw new IllegalArgumentException(
                "Debes indicar por qué no realizaste la actividad"
        );
    }

    progresoService.marcarNoRealizada(
            estudiante.getIdUsuario(),
            idActividad,
            solicitud.razon()
    );

    return ResponseEntity.noContent().build();
}

    @ExceptionHandler(ExcepcionActividad.class)
    public ResponseEntity<Map<String, String>> manejarActividadNoEncontrada(
            ExcepcionActividad excepcion
    ) {
        return ResponseEntity.status(404)
                .body(Map.of("error", excepcion.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarDatosInvalidos(
            IllegalArgumentException excepcion
    ) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", excepcion.getMessage()));
    }
}