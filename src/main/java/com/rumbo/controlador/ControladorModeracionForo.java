package com.rumbo.controlador;

import com.rumbo.dto.PublicacionForoRespuesta;
import com.rumbo.dto.RevisionMensajeRespuesta;
import com.rumbo.modelo.RevisionMensaje.Decision;
import com.rumbo.modelo.Rol;
import com.rumbo.servicio.ExcepcionForo;
import com.rumbo.servicio.GestorPermisos;
import com.rumbo.servicio.ModeracionForoService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/moderacion/foro")
public class ControladorModeracionForo {

    private final ModeracionForoService moderacionService;
    private final GestorPermisos gestorPermisos;

    public ControladorModeracionForo(
            ModeracionForoService moderacionService,
            GestorPermisos gestorPermisos) {

        this.moderacionService = moderacionService;
        this.gestorPermisos = gestorPermisos;
    }

    @GetMapping("/pendientes")
    public List<PublicacionForoRespuesta> listarPendientes(
            HttpSession sesion) {

        return moderacionService.listarPendientes(
                idSupervisor(sesion));
    }

    @GetMapping("/mis-revisiones")
    public List<RevisionMensajeRespuesta> consultarMisRevisiones(
            HttpSession sesion) {

        return moderacionService.consultarMisRevisiones(
                idSupervisor(sesion));
    }

    @PostMapping("/publicaciones/{id}/validar")
    @ResponseStatus(HttpStatus.CREATED)
    public RevisionMensajeRespuesta validar(
            @PathVariable Long id,
            HttpSession sesion) {

        return moderacionService.revisar(
                id, Decision.VALIDAR, idSupervisor(sesion));
    }

    @PostMapping("/publicaciones/{id}/eliminar")
    @ResponseStatus(HttpStatus.CREATED)
    public RevisionMensajeRespuesta eliminar(
            @PathVariable Long id,
            HttpSession sesion) {

        return moderacionService.revisar(
                id, Decision.ELIMINAR, idSupervisor(sesion));
    }

    private String idSupervisor(HttpSession sesion) {
        return gestorPermisos.exigirRol(
                sesion,
                Rol.SUPERVISOR_DE_MENSAJES
        ).getIdUsuario();
    }

    @ExceptionHandler(ExcepcionForo.class)
    public ResponseEntity<Map<String, String>> manejarError(
            ExcepcionForo excepcion) {

        return ResponseEntity.status(excepcion.getCodigo())
                .body(Map.of("error", excepcion.getMessage()));
    }
}