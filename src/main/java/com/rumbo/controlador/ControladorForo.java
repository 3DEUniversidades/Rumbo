package com.rumbo.controlador;

import com.rumbo.dto.*;
import com.rumbo.servicio.ExcepcionForo;
import com.rumbo.servicio.ForoService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/foro")
public class ControladorForo {

    private final ForoService foroService;

    public ControladorForo(ForoService foroService) {
        this.foroService = foroService;
    }

    @GetMapping("/acceso")
    public Map<String, String> consultarAcceso(
            @RequestHeader(value = "X-Usuario-Id", required = false)
            String usuario) {

        return foroService.consultarAcceso(usuario);
    }

    @GetMapping("/publicaciones")
    public List<PublicacionForoRespuesta> listarPublicaciones(
            @RequestHeader(value = "X-Usuario-Id", required = false)
            String usuario,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String tipo,
            @RequestParam(required = false) Long carreraId,
            @RequestParam(required = false) String autor) {

        return foroService.listarPublicaciones(
                usuario, q, tipo, carreraId, autor);
    }

    @GetMapping("/publicaciones/{id}")
    public PublicacionForoRespuesta consultarPublicacion(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false)
            String usuario) {

        return foroService.consultarPublicacion(id, usuario);
    }

    @PostMapping("/publicaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacionForoRespuesta crearPublicacion(
            @RequestBody CrearPublicacionSolicitud solicitud,
            @RequestHeader(value = "X-Usuario-Id", required = false)
            String usuario) {

        return foroService.crearPublicacion(solicitud, usuario);
    }

    @GetMapping("/publicaciones/{id}/respuestas")
    public List<RespuestaForoRespuesta> listarRespuestas(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false)
            String usuario) {

        return foroService.listarRespuestas(id, usuario);
    }

    @PostMapping("/publicaciones/{id}/respuestas")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaForoRespuesta crearRespuesta(
            @PathVariable Long id,
            @RequestBody CrearRespuestaSolicitud solicitud,
            @RequestHeader(value = "X-Usuario-Id", required = false)
            String usuario) {

        return foroService.crearRespuesta(id, solicitud, usuario);
    }

    @ExceptionHandler(ExcepcionForo.class)
    public ResponseEntity<Map<String, String>> manejarError(
            ExcepcionForo excepcion) {

        return ResponseEntity.status(excepcion.getCodigo())
                .body(Map.of("error", excepcion.getMessage()));
    }
}