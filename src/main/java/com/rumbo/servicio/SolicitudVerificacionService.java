package com.rumbo.servicio;

import com.rumbo.dto.EstadoVerificacionResponse;
import com.rumbo.dto.SolicitudVerificacionResponse;
import com.rumbo.modelo.EstadoVerificacion;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SolicitudVerificacion;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.SolicitudVerificacionRepository;
import com.rumbo.persistencia.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class SolicitudVerificacionService {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UsuarioRepository usuarioRepository;
    private final SolicitudVerificacionRepository solicitudRepository;
    private final PerfilService perfilService;

    public SolicitudVerificacionService(UsuarioRepository usuarioRepository,
                                        SolicitudVerificacionRepository solicitudRepository,
                                        PerfilService perfilService) {
        this.usuarioRepository = usuarioRepository;
        this.solicitudRepository = solicitudRepository;
        this.perfilService = perfilService;
    }

    public SolicitudVerificacionResponse solicitarVerificacion(String idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ExcepcionAutenticacion("Usuario no encontrado"));

        if (usuario.getRol() != Rol.ESTUDIANTE) {
            throw new ExcepcionPermiso("Solo los estudiantes pueden solicitar verificacion");
        }

        if (usuario.getEstadoVerificacion() == EstadoVerificacion.VERIFICADO) {
            throw new ExcepcionConflicto("La cuenta ya esta verificada");
        }

        if (solicitudRepository.existsByIdUsuarioAndEstado(
                idUsuario, SolicitudVerificacion.ESTADO_PENDIENTE)) {
            throw new ExcepcionConflicto("Ya existe una solicitud pendiente");
        }

        if (!perfilService.estaCompleto(idUsuario)) {
            throw new ExcepcionValidacion(java.util.Map.of(
                    "perfil", "Debe completar nivel de estudios, carrera de interes y descripcion personal"));
        }

        SolicitudVerificacion solicitud = new SolicitudVerificacion(
                UUID.randomUUID().toString(),
                idUsuario,
                SolicitudVerificacion.ESTADO_PENDIENTE,
                LocalDateTime.now().format(FORMATO_FECHA)
        );
        solicitudRepository.save(solicitud);

        return mapearSolicitud(solicitud);
    }

    public EstadoVerificacionResponse consultarEstadoVerificacion(String idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ExcepcionAutenticacion("Usuario no encontrado"));

        List<SolicitudVerificacion> solicitudes =
                solicitudRepository.findByIdUsuario(idUsuario);
        boolean solicitudPendiente = solicitudes.stream()
                .anyMatch(solicitud -> SolicitudVerificacion.ESTADO_PENDIENTE
                        .equals(solicitud.getEstado()));
        String estadoUltimaSolicitud = solicitudes.isEmpty()
                ? null
                : solicitudes.get(0).getEstado();
        String estadoVerificacion = usuario.getEstadoVerificacion() == EstadoVerificacion.VERIFICADO
                ? "verificado"
                : "no verificado";
        String mensaje = construirMensaje(estadoVerificacion, solicitudPendiente,
                estadoUltimaSolicitud);

        return new EstadoVerificacionResponse(
                estadoVerificacion,
                solicitudPendiente,
                estadoUltimaSolicitud,
                mensaje
        );
    }

    private String construirMensaje(String estadoVerificacion, boolean solicitudPendiente,
                                     String estadoUltimaSolicitud) {
        if ("verificado".equals(estadoVerificacion)) {
            return "Tu cuenta está verificada.";
        }
        if (solicitudPendiente) {
            return "Tu solicitud de verificación está pendiente de revisión.";
        }
        if (SolicitudVerificacion.ESTADO_RECHAZADA.equals(estadoUltimaSolicitud)) {
            return "Tu solicitud fue rechazada. Puedes enviar una nueva solicitud.";
        }
        return "Para acceder al foro público debes completar o actualizar tus datos y solicitar la verificación de tu cuenta";
    }

    private SolicitudVerificacionResponse mapearSolicitud(SolicitudVerificacion solicitud) {
        return new SolicitudVerificacionResponse(
                solicitud.getIdSolicitud(),
                solicitud.getEstado(),
                solicitud.getFechaSolicitud()
        );
    }
}
