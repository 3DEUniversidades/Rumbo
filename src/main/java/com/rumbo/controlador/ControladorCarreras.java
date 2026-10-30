package com.rumbo.controlador;

import com.fasterxml.jackson.databind.JsonNode;
import com.rumbo.dto.CarreraRespuesta;
import com.rumbo.modelo.Carrera;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.servicio.CarreraService;
import com.rumbo.servicio.ExcepcionCarrera;
import com.rumbo.servicio.ExcepcionCarreraNoEncontrada;
import com.rumbo.servicio.GestorPermisos;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carreras")
public class ControladorCarreras {

    private final CarreraService carreraService;
    private final GestorPermisos gestorPermisos;

    public ControladorCarreras(
            CarreraService carreraService,
            GestorPermisos gestorPermisos) {

        this.carreraService = carreraService;
        this.gestorPermisos = gestorPermisos;
    }

    // Consulta disponible para usuarios con sesión.
    @GetMapping
    public List<CarreraRespuesta> consultarCarreras(HttpSession sesion) {
        gestorPermisos.exigirSesion(sesion);

        return carreraService.consultarCarreras()
                .stream()
                .map(this::mapearRespuesta)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarreraRespuesta> buscarPorId(
            @PathVariable Long id,
            HttpSession sesion) {

        gestorPermisos.exigirSesion(sesion);

        return carreraService.buscarPorId(id)
                .map(this::mapearRespuesta)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCarrera(
            @PathVariable Long id,
            @RequestBody JsonNode solicitud,
            HttpSession sesion) {

        SesionUsuario encargado = gestorPermisos.exigirRol(
                sesion, Rol.ENCARGADO_DE_CARRERAS, Rol.ADMIN);

        try {
            Carrera actualizada = carreraService.actualizarCarrera(
                    id,
                    solicitud,
                    encargado.getIdUsuario()
            );

            return ResponseEntity.ok(mapearRespuesta(actualizada));

        } catch (ExcepcionCarreraNoEncontrada e) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", e.getMessage()));

        } catch (ExcepcionCarrera e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/historial")
    public ResponseEntity<?> consultarHistorial(HttpSession sesion) {
        gestorPermisos.exigirRol(
                sesion, Rol.ENCARGADO_DE_CARRERAS, Rol.ADMIN);

        return ResponseEntity.ok(
                carreraService.consultarHistorial(null));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<?> consultarHistorialDeCarrera(
            @PathVariable Long id,
            HttpSession sesion) {

        gestorPermisos.exigirRol(
                sesion, Rol.ENCARGADO_DE_CARRERAS, Rol.ADMIN);

        if (carreraService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Carrera no encontrada"));
        }

        return ResponseEntity.ok(
                carreraService.consultarHistorial(id));
    }

    private CarreraRespuesta mapearRespuesta(Carrera carrera) {
        return new CarreraRespuesta(
                carrera.getId(),
                carrera.getNombre(),
                carrera.getFacultad(),
                carrera.getDescripcion(),
                carrera.getDuracion(),
                carrera.getCosto(),
                carrera.getCostoPeriodo(),
                carrera.getCostoAnio(),
                carrera.getModalidad(),
                carrera.getCargaCreditos(),
                carrera.getAreasFormacion(),
                carrera.getCompetenciasDesarrolladas(),
                carrera.getOportunidadesLaborales(),
                carrera.getSalarioEstimado(),
                carrera.getEmpleabilidad(),
                carrera.getContextoLaboral(),
                carrera.getPdfOficialUrl(),
                carrera.getUrlCarreraOficial()
        );
    }
}