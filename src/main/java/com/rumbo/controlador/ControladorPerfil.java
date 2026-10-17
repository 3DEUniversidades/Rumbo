package com.rumbo.controlador;

import com.rumbo.dto.PerfilRequestDTO;
import com.rumbo.dto.PerfilResponseDTO;
import com.rumbo.dto.PerfilPublicoResponse;
import com.rumbo.dto.SolicitudActualizacionPerfilRequest;
import com.rumbo.dto.SolicitudActualizacionPerfilResponse;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.servicio.GestorPermisos;
import com.rumbo.servicio.PerfilService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/perfil")
public class ControladorPerfil {

    private final GestorPermisos gestorPermisos;
    private final PerfilService perfilService;

    public ControladorPerfil(GestorPermisos gestorPermisos, PerfilService perfilService) {
        this.gestorPermisos = gestorPermisos;
        this.perfilService = perfilService;
    }

    @GetMapping
    public ResponseEntity<PerfilResponseDTO> consultar(HttpSession sesion) {
        SesionUsuario usuario = gestorPermisos.exigirSesion(sesion);
        return ResponseEntity.ok(perfilService.consultar(usuario.getIdUsuario()));
    }

    @PutMapping
    public ResponseEntity<PerfilResponseDTO> actualizar(
            @RequestBody PerfilRequestDTO solicitud, HttpSession sesion) {
        SesionUsuario usuario = gestorPermisos.exigirRol(
                sesion, Rol.ESTUDIANTE, Rol.COLABORADOR);
        return ResponseEntity.ok(perfilService.actualizar(usuario.getIdUsuario(), solicitud));
    }

    @PostMapping("/solicitudes")
    public ResponseEntity<SolicitudActualizacionPerfilResponse> solicitarActualizacion(
            @RequestBody SolicitudActualizacionPerfilRequest solicitud,
            HttpSession sesion) {
        SesionUsuario usuario = gestorPermisos.exigirRol(
                sesion, Rol.ESTUDIANTE, Rol.COLABORADOR);
        return ResponseEntity.status(201).body(
                perfilService.solicitarActualizacion(usuario.getIdUsuario(), solicitud));
    }

    @GetMapping("/solicitudes/pendiente")
    public ResponseEntity<SolicitudActualizacionPerfilResponse> consultarSolicitudPendiente(
            HttpSession sesion) {
        SesionUsuario usuario = gestorPermisos.exigirRol(
                sesion, Rol.ESTUDIANTE, Rol.COLABORADOR);
        return ResponseEntity.ok(
                perfilService.consultarSolicitudPendiente(usuario.getIdUsuario()));
    }

    @GetMapping("/publico/{idUsuario}")
    public ResponseEntity<PerfilPublicoResponse> consultarPublico(
            @PathVariable String idUsuario, HttpSession sesion) {
        gestorPermisos.exigirSesion(sesion);
        return ResponseEntity.ok(perfilService.consultarPublico(idUsuario));
    }
}
