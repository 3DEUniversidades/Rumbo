package com.rumbo.controlador;

import com.rumbo.dto.ActualizarCarreraSolicitud;
import com.rumbo.dto.CarreraRespuesta;
import com.rumbo.modelo.Carrera;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.UsuarioRepository;
import com.rumbo.servicio.CarreraService;
import com.rumbo.servicio.ExcepcionCarrera;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carreras")
public class ControladorCarreras {

    // TODO: mover a la definición central de roles cuando exista el mapa de permisos
    private static final String ROL_ENCARGADO_CARRERAS = "ENCARGADO_CARRERAS";

    private final CarreraService carreraService;
    private final UsuarioRepository usuarioRepository;

    public ControladorCarreras(CarreraService carreraService, UsuarioRepository usuarioRepository) {
        this.carreraService = carreraService;
        this.usuarioRepository = usuarioRepository;
    }

    // ---------- Consulta pública (estudiantes / comparador) ----------

    @GetMapping
    public List<CarreraRespuesta> consultarCarreras() {
        return carreraService.consultarCarreras()
                .stream()
                .map(this::mapearRespuesta)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarreraRespuesta> buscarPorId(@PathVariable Long id) {
        return carreraService.buscarPorId(id)
                .map(this::mapearRespuesta)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------- Gestión (solo encargado de carreras) ----------

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCarrera(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false) String idUsuario,
            @RequestBody ActualizarCarreraSolicitud solicitud) {

        ResponseEntity<?> rechazo = validarEncargado(idUsuario);
        if (rechazo != null) {
            return rechazo;
        }
        if (carreraService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Carrera no encontrada"));
        }

        try {
            Carrera actualizada = carreraService.actualizarCarrera(id, solicitud, idUsuario);
            return ResponseEntity.ok(mapearRespuesta(actualizada));
        } catch (ExcepcionCarrera e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/historial")
    public ResponseEntity<?> consultarHistorial(
            @RequestHeader(value = "X-Usuario-Id", required = false) String idUsuario) {

        ResponseEntity<?> rechazo = validarEncargado(idUsuario);
        if (rechazo != null) {
            return rechazo;
        }
        return ResponseEntity.ok(carreraService.consultarHistorial(null));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<?> consultarHistorialDeCarrera(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false) String idUsuario) {

        ResponseEntity<?> rechazo = validarEncargado(idUsuario);
        if (rechazo != null) {
            return rechazo;
        }
        return ResponseEntity.ok(carreraService.consultarHistorial(id));
    }

    // ---------- Temporal ----------

    // REMPLAZAR por GestorPermisos cuando el módulo de sesión/permisos esté listo.
    // Devuelve null si el usuario es válido; si no, la respuesta de rechazo (401 / 403).
    private ResponseEntity<?> validarEncargado(String idUsuario) {
        if (idUsuario == null || idUsuario.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error", "Sesión requerida"));
        }
        Optional<Usuario> usuario = usuarioRepository.findById(idUsuario);
        if (usuario.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Usuario no válido"));
        }
        if (!ROL_ENCARGADO_CARRERAS.equals(usuario.get().getRol())) {
            return ResponseEntity.status(403).body(Map.of("error", "No tienes permiso para esta acción"));
        }
        return null;
    }

    private CarreraRespuesta mapearRespuesta(Carrera carrera) {
        return new CarreraRespuesta(
                carrera.getId(),
                carrera.getNombre(),
                carrera.getDuracion(),
                carrera.getCosto(),
                carrera.getModalidad(),
                carrera.getCargaCreditos(),
                carrera.getHabilidadesRequeridas(),
                carrera.getOportunidadesLaborales(),
                carrera.getSalarioEstimado(),
                carrera.getEmpleabilidad(),
                carrera.getPdfOficialUrl(),
                carrera.getUrlCarreraOficial()
        );
    }
}