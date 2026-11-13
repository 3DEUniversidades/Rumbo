package com.rumbo.servicio;

import com.rumbo.dto.AutorForoRespuesta;
import com.rumbo.dto.PublicacionForoRespuesta;
import com.rumbo.dto.RevisionMensajeRespuesta;
import com.rumbo.modelo.Carrera;
import com.rumbo.modelo.Publicacion;
import com.rumbo.modelo.RevisionMensaje;
import com.rumbo.modelo.RevisionMensaje.Decision;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.PublicacionRepository;
import com.rumbo.persistencia.RespuestaPublicacionRepository;
import com.rumbo.persistencia.RevisionMensajeRepository;
import com.rumbo.persistencia.UsuarioRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ModeracionForoService {

    private final PublicacionRepository publicacionRepository;
    private final RevisionMensajeRepository revisionRepository;
    private final UsuarioRepository usuarioRepository;
    private final RespuestaPublicacionRepository respuestaRepository;

    public ModeracionForoService(
            PublicacionRepository publicacionRepository,
            RevisionMensajeRepository revisionRepository,
            UsuarioRepository usuarioRepository,
            RespuestaPublicacionRepository respuestaRepository) {

        this.publicacionRepository = publicacionRepository;
        this.revisionRepository = revisionRepository;
        this.usuarioRepository = usuarioRepository;
        this.respuestaRepository = respuestaRepository;
    }

    public List<PublicacionForoRespuesta> listarPendientes(
            String idSupervisor) {

        exigirSupervisor(idSupervisor);

        return publicacionRepository
                .findByEstadoInOrderByFechaDescIdDesc(
                        List.of(Publicacion.Estado.ACTIVA))
                .stream()
                .map(p -> mapearPublicacion(p, idSupervisor))
                .toList();
    }

    public List<RevisionMensajeRespuesta> consultarMisRevisiones(
            String idSupervisor) {

        exigirSupervisor(idSupervisor);

        return revisionRepository
                .findBySupervisorIdUsuarioOrderByFechaDescIdDesc(idSupervisor)
                .stream()
                .map(this::mapearRevision)
                .toList();
    }

    @Transactional
    public RevisionMensajeRespuesta revisar(
            Long publicacionId,
            Decision decision,
            String idSupervisor) {

        Usuario supervisor = exigirSupervisor(idSupervisor);

        if (publicacionId == null || publicacionId < 1) {
            throw new ExcepcionForo(
                    400, "El identificador de publicación no es válido.");
        }

        if (decision == null) {
            throw new ExcepcionForo(400, "La decisión es obligatoria.");
        }

        Publicacion publicacion = publicacionRepository
                .buscarParaRevision(publicacionId)
                .orElseThrow(() -> new ExcepcionForo(
                        404, "La publicación no existe."));

        if (publicacion.getEstado() != Publicacion.Estado.ACTIVA) {
            throw new ExcepcionConflicto(
                    "La publicación ya fue revisada. Actualiza el tablero.");
        }

        Publicacion.Estado nuevoEstado = decision == Decision.VALIDAR
                ? Publicacion.Estado.VALIDADA
                : Publicacion.Estado.ELIMINADA;

        publicacion.setEstado(nuevoEstado);

        RevisionMensaje revision = new RevisionMensaje(
                publicacion, supervisor, decision);

        revisionRepository.save(revision);

        // Publicacion está administrada por JPA:
        // su estado se actualiza al confirmar esta misma transacción.
        return mapearRevision(revision);
    }

    private Usuario exigirSupervisor(String idUsuario) {
        if (idUsuario == null || idUsuario.isBlank()) {
            throw new ExcepcionAutenticacion("Debes iniciar sesión.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ExcepcionAutenticacion(
                        "El usuario de la sesión no existe."));

        if (usuario.getRol() != Rol.SUPERVISOR_DE_MENSAJES) {
            throw new ExcepcionPermiso(
                    "Solo el supervisor de mensajes puede realizar esta acción.");
        }

        return usuario;
    }

    private RevisionMensajeRespuesta mapearRevision(RevisionMensaje r) {
        return new RevisionMensajeRespuesta(
                r.getId(),
                r.getPublicacion().getId(),
                r.getPublicacion().getTitulo(),
                r.getDecision().name(),
                r.getFecha(),
                mapearAutor(r.getSupervisor())
        );
    }

    private PublicacionForoRespuesta mapearPublicacion(Publicacion p, String idUsuario) {
        List<PublicacionForoRespuesta.CarreraResumen> carreras =
                p.getCarreras().stream()
                        .sorted(Comparator.comparing(Carrera::getNombre))
                        .map(c -> new PublicacionForoRespuesta.CarreraResumen(
                                c.getId(), c.getNombre()))
                        .toList();

        return new PublicacionForoRespuesta(
                p.getId(),
                p.getTitulo(),
                p.getContenido(),
                p.getTipo().name(),
                p.getFecha(),
                p.getEstado().name(),
                mapearAutor(p.getAutor()),
                carreras,
                respuestaRepository.countByPublicacionId(p.getId()),
                p.getReacciones().size(),
                p.getReacciones().stream()
                        .anyMatch(u -> u.getIdUsuario().equals(idUsuario))
        );
    }

    private AutorForoRespuesta mapearAutor(Usuario usuario) {
        return new AutorForoRespuesta(
                usuario.getIdUsuario(),
                usuario.getNombreUsuario()
        );
    }
}
