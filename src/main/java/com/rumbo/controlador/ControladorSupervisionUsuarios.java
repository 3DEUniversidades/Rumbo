package com.rumbo.controlador;

import com.rumbo.dto.EstadoVerificacionResponse;
import com.rumbo.dto.SolicitudVerificacionResponse;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.servicio.GestorPermisos;
import com.rumbo.servicio.SolicitudVerificacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verificacion")
public class ControladorSupervisionUsuarios {

    private final GestorPermisos gestorPermisos;
    private final SolicitudVerificacionService solicitudService;

    public ControladorSupervisionUsuarios(GestorPermisos gestorPermisos,
                                          SolicitudVerificacionService solicitudService) {
        this.gestorPermisos = gestorPermisos;
        this.solicitudService = solicitudService;
    }

    @PostMapping("/solicitudes")
    public ResponseEntity<SolicitudVerificacionResponse> solicitar(HttpSession sesion) {
        SesionUsuario sesionUsuario = gestorPermisos.exigirAccion(
                sesion, GestorPermisos.ACCION_SOLICITAR_VERIFICACION);
        return ResponseEntity.status(201)
                .body(solicitudService.solicitarVerificacion(sesionUsuario.getIdUsuario()));
    }

    @GetMapping("/estado")
    public ResponseEntity<EstadoVerificacionResponse> consultarEstado(HttpSession sesion) {
        SesionUsuario sesionUsuario = gestorPermisos.exigirSesion(sesion);
        return ResponseEntity.ok(
                solicitudService.consultarEstadoVerificacion(sesionUsuario.getIdUsuario()));
    }
}
