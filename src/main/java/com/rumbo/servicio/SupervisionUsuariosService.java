package com.rumbo.servicio;

import com.rumbo.dto.DecisionSupervisionRequest;
import com.rumbo.dto.SolicitudSupervisionResponse;
import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupervisionUsuariosService {

    private static final String APROBAR = "APROBAR";
    private static final String RECHAZAR = "RECHAZAR";
    private static final String VERIFICACION = "VERIFICACION";
    private static final String PERFIL = "PERFIL";

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final SolicitudVerificacionRepository verificacionRepository;
    private final SolicitudActualizacionPerfilRepository perfilSolicitudRepository;
    private final RegistroSupervisionUsuariosRepository registroRepository;
    private final NotificacionService notificacionService;

    public SupervisionUsuariosService(UsuarioRepository usuarioRepository,
                                      PerfilRepository perfilRepository,
                                      SolicitudVerificacionRepository verificacionRepository,
                                      SolicitudActualizacionPerfilRepository perfilSolicitudRepository,
                                      RegistroSupervisionUsuariosRepository registroRepository,
                                      NotificacionService notificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.verificacionRepository = verificacionRepository;
        this.perfilSolicitudRepository = perfilSolicitudRepository;
        this.registroRepository = registroRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional(readOnly = true)
    public List<SolicitudSupervisionResponse> listarVerificaciones() {
        return verificacionRepository.findByEstadoOrderByFechaSolicitudAsc(
                        SolicitudVerificacion.ESTADO_PENDIENTE).stream()
                .map(s -> mapear(s.getIdSolicitud(), s.getIdUsuario(), null, null,
                        s.getEstado(), s.getFechaSolicitud())).toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitudSupervisionResponse> listarPerfiles() {
        return perfilSolicitudRepository.findByEstadoOrderByFechaSolicitudAsc(
                        SolicitudActualizacionPerfil.ESTADO_PENDIENTE).stream()
                .map(s -> mapear(s.getIdSolicitud(), s.getIdUsuario(),
                        s.getNombreUsuarioSolicitado(), s.getNombreCompletoSolicitado(),
                        s.getEstado(), s.getFechaSolicitud())).toList();
    }

    @Transactional
    public void decidirVerificacion(String idSolicitud, String idSupervisor,
                                    DecisionSupervisionRequest request) {
        validarDecision(request);
        SolicitudVerificacion solicitud = verificacionRepository.findById(idSolicitud)
                .orElseThrow(() -> new ExcepcionValidacion(Map.of("solicitud", "No existe")));
        exigirPendiente(solicitud.getEstado());
        Usuario usuario = obtenerUsuario(solicitud.getIdUsuario());
        boolean aprobada = APROBAR.equals(request.decision());
        usuario.setEstadoVerificacion(aprobada
                ? EstadoVerificacion.VERIFICADO : EstadoVerificacion.NO_VERIFICADO);
        solicitud.decidir(aprobada ? SolicitudVerificacion.ESTADO_APROBADA
                        : SolicitudVerificacion.ESTADO_RECHAZADA,
                idSupervisor, java.time.Instant.now().toString(), request.motivo());
        usuarioRepository.save(usuario);
        verificacionRepository.save(solicitud);
        registrarYNotificar(idSupervisor, idSolicitud, solicitud.getIdUsuario(),
                VERIFICACION, request, aprobada ? "Verificación aprobada"
                        : "Verificación rechazada");
    }

    @Transactional
    public void decidirPerfil(String idSolicitud, String idSupervisor,
                               DecisionSupervisionRequest request) {
        validarDecision(request);
        SolicitudActualizacionPerfil solicitud = perfilSolicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new ExcepcionValidacion(Map.of("solicitud", "No existe")));
        exigirPendiente(solicitud.getEstado());
        Usuario usuario = obtenerUsuario(solicitud.getIdUsuario());
        boolean aprobada = APROBAR.equals(request.decision());
        if (aprobada) {
            usuarioRepository.findByNombreUsuarioIgnoreCase(solicitud.getNombreUsuarioSolicitado())
                    .filter(otro -> !otro.getIdUsuario().equals(usuario.getIdUsuario()))
                    .ifPresent(otro -> { throw new ExcepcionConflicto(
                            "El nombre de usuario solicitado ya esta en uso"); });
            usuario.actualizarDatosCuenta(solicitud.getNombreUsuarioSolicitado(),
                    solicitud.getNombreCompletoSolicitado());
            usuarioRepository.save(usuario);
        }
        solicitud.decidir(aprobada ? SolicitudActualizacionPerfil.ESTADO_APROBADA
                        : SolicitudActualizacionPerfil.ESTADO_RECHAZADA,
                idSupervisor, java.time.Instant.now().toString(), request.motivo());
        perfilSolicitudRepository.save(solicitud);
        registrarYNotificar(idSupervisor, idSolicitud, solicitud.getIdUsuario(),
                PERFIL, request, aprobada ? "Cambio de perfil aprobado"
                        : "Cambio de perfil rechazado");
    }

    private void registrarYNotificar(String supervisor, String solicitud,
                                     String usuario, String tipo,
                                     DecisionSupervisionRequest request, String titulo) {
        registroRepository.save(new RegistroSupervisionUsuarios(supervisor, solicitud,
                tipo, request.decision(), request.motivo()));
        String detalle = request.motivo() == null || request.motivo().isBlank()
                ? titulo + "." : titulo + ". Motivo: " + request.motivo().strip();
        notificacionService.crear(usuario, titulo, detalle);
    }

    private SolicitudSupervisionResponse mapear(String idSolicitud, String idUsuario,
                                                String nombreUsuarioSolicitado,
                                                String nombreCompletoSolicitado,
                                                String estado, String fecha) {
        Usuario usuario = obtenerUsuario(idUsuario);
        Perfil perfil = perfilRepository.findByUsuarioIdUsuario(idUsuario).orElse(null);
        return new SolicitudSupervisionResponse(idSolicitud, idUsuario,
                usuario.getNombreUsuario(), usuario.getNombreCompleto(), usuario.getCorreo(),
                usuario.getRol().name(), perfil == null ? null : perfil.getNivelEstudios(),
                perfil == null || perfil.getCarrera() == null ? null : perfil.getCarrera().getId(),
                perfil == null || perfil.getCarrera() == null ? null : perfil.getCarrera().getNombre(),
                perfil == null ? null : perfil.getAreaLaboral(),
                perfil == null ? null : perfil.getDescripcionPersonal(),
                nombreUsuarioSolicitado, nombreCompletoSolicitado, estado, fecha);
    }

    private Usuario obtenerUsuario(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ExcepcionAutenticacion("Usuario no encontrado"));
    }

    private void validarDecision(DecisionSupervisionRequest request) {
        if (request == null || (!APROBAR.equals(request.decision())
                && !RECHAZAR.equals(request.decision()))) {
            throw new ExcepcionValidacion(Map.of("decision", "La decision debe ser APROBAR o RECHAZAR"));
        }
        if (RECHAZAR.equals(request.decision())
                && (request.motivo() == null || request.motivo().isBlank())) {
            throw new ExcepcionValidacion(Map.of("motivo", "El motivo de rechazo es obligatorio"));
        }
    }

    private void exigirPendiente(String estado) {
        if (!SolicitudVerificacion.ESTADO_PENDIENTE.equals(estado)
                && !SolicitudActualizacionPerfil.ESTADO_PENDIENTE.equals(estado)) {
            throw new ExcepcionConflicto("La solicitud ya fue decidida");
        }
    }
}
