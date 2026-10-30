package com.rumbo.controlador;

import com.rumbo.dto.*;
import com.rumbo.modelo.Rol;
import com.rumbo.servicio.ExcepcionForo;
import com.rumbo.servicio.ForoService;
import com.rumbo.servicio.GestorPermisos;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/foro")
public class ControladorForo {

    private final ForoService foroService;
    private final GestorPermisos gestorPermisos;

    public ControladorForo(
            ForoService foroService,
            GestorPermisos gestorPermisos) {

        this.foroService = foroService;
        this.gestorPermisos = gestorPermisos;
    }

    @GetMapping("/acceso")
    public Map<String, String> consultarAcceso(HttpSession sesion) {
        return foroService.consultarAcceso(idUsuario(sesion));
    }

    @GetMapping("/publicaciones")
    public List<PublicacionForoRespuesta> listarPublicaciones(
            HttpSession sesion,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String tipo,
            @RequestParam(required = false) Long carreraId,
            @RequestParam(required = false) String autor) {

        return foroService.listarPublicaciones(
                idUsuario(sesion), q, tipo, carreraId, autor);
    }

    @GetMapping("/publicaciones/{id}")
    public PublicacionForoRespuesta consultarPublicacion(
            @PathVariable Long id,
            HttpSession sesion) {

        return foroService.consultarPublicacion(id, idUsuario(sesion));
    }

    @PostMapping("/publicaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacionForoRespuesta crearPublicacion(
            @RequestBody CrearPublicacionSolicitud solicitud,
            HttpSession sesion) {

        return foroService.crearPublicacion(solicitud, idUsuario(sesion));
    }

    @GetMapping("/publicaciones/{id}/respuestas")
    public List<RespuestaForoRespuesta> listarRespuestas(
            @PathVariable Long id,
            HttpSession sesion) {

        return foroService.listarRespuestas(id, idUsuario(sesion));
    }

    @PostMapping("/publicaciones/{id}/respuestas")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaForoRespuesta crearRespuesta(
            @PathVariable Long id,
            @RequestBody CrearRespuestaSolicitud solicitud,
            HttpSession sesion) {

        return foroService.crearRespuesta(
                id, solicitud, idUsuario(sesion));
    }

    @PutMapping("/publicaciones/{id}/me-gusta")
    public PublicacionForoRespuesta marcarMeGusta(
            @PathVariable Long id, HttpSession sesion) {
        return reaccionar(id, sesion, true);
    }

    @DeleteMapping("/publicaciones/{id}/me-gusta")
    public PublicacionForoRespuesta quitarMeGusta(
            @PathVariable Long id, HttpSession sesion) {
        return reaccionar(id, sesion, false);
    }

    private PublicacionForoRespuesta reaccionar(
            Long id, HttpSession sesion, boolean meGusta) {
        String usuario = gestorPermisos.exigirRol(
                sesion, Rol.ESTUDIANTE, Rol.COLABORADOR, Rol.ADMIN).getIdUsuario();
        if (id == null || id < 1) {
            throw new ExcepcionForo(400,
                    "El identificador de publicación no es válido.");
        }
        return foroService.reaccionar(id, usuario, meGusta);
    }

    private String idUsuario(HttpSession sesion) {
        return gestorPermisos.exigirSesion(sesion).getIdUsuario();
    }

    @ExceptionHandler(ExcepcionForo.class)
    public ResponseEntity<Map<String, String>> manejarError(
            ExcepcionForo excepcion) {

        return ResponseEntity.status(excepcion.getCodigo())
                .body(Map.of("error", excepcion.getMessage()));
    }
}
