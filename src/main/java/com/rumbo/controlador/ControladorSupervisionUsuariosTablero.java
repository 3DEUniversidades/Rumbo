package com.rumbo.controlador;

import com.rumbo.dto.DecisionSupervisionRequest;
import com.rumbo.dto.SolicitudSupervisionResponse;
import com.rumbo.dto.NotificacionResponse;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.servicio.GestorPermisos;
import com.rumbo.servicio.NotificacionService;
import com.rumbo.servicio.SupervisionUsuariosService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/supervision/usuarios")
public class ControladorSupervisionUsuariosTablero {

    private final GestorPermisos gestorPermisos;
    private final SupervisionUsuariosService supervisionService;
    private final NotificacionService notificacionService;

    public ControladorSupervisionUsuariosTablero(
            GestorPermisos gestorPermisos,
            SupervisionUsuariosService supervisionService,
            NotificacionService notificacionService) {
        this.gestorPermisos = gestorPermisos;
        this.supervisionService = supervisionService;
        this.notificacionService = notificacionService;
    }

    @GetMapping("/verificaciones")
    public List<SolicitudSupervisionResponse> verificaciones(HttpSession sesion) {
        exigirSupervisor(sesion);
        return supervisionService.listarVerificaciones();
    }

    @PutMapping("/verificaciones/{idSolicitud}")
    public ResponseEntity<Void> decidirVerificacion(
            @PathVariable String idSolicitud,
            @RequestBody DecisionSupervisionRequest request,
            HttpSession sesion) {
        SesionUsuario supervisor = exigirSupervisor(sesion);
        supervisionService.decidirVerificacion(idSolicitud, supervisor.getIdUsuario(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/perfiles")
    public List<SolicitudSupervisionResponse> perfiles(HttpSession sesion) {
        exigirSupervisor(sesion);
        return supervisionService.listarPerfiles();
    }

    @PutMapping("/perfiles/{idSolicitud}")
    public ResponseEntity<Void> decidirPerfil(
            @PathVariable String idSolicitud,
            @RequestBody DecisionSupervisionRequest request,
            HttpSession sesion) {
        SesionUsuario supervisor = exigirSupervisor(sesion);
        supervisionService.decidirPerfil(idSolicitud, supervisor.getIdUsuario(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notificaciones")
    public List<NotificacionResponse> notificaciones(HttpSession sesion) {
        SesionUsuario usuario = gestorPermisos.exigirSesion(sesion);
        return notificacionService.listar(usuario.getIdUsuario());
    }

    @PutMapping("/notificaciones/{idNotificacion}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable String idNotificacion,
                                            HttpSession sesion) {
        SesionUsuario usuario = gestorPermisos.exigirSesion(sesion);
        notificacionService.marcarLeida(usuario.getIdUsuario(), idNotificacion);
        return ResponseEntity.noContent().build();
    }

    private SesionUsuario exigirSupervisor(HttpSession sesion) {
        SesionUsuario usuario = gestorPermisos.exigirSesion(sesion);
        if (!gestorPermisos.puedeSupervisarUsuarios(usuario.getRol())) {
            throw new com.rumbo.servicio.ExcepcionPermiso(
                    "No tiene permisos para supervisar usuarios");
        }
        return usuario;
    }
}
