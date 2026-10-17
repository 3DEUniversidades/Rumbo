package com.rumbo.servicio;

import com.rumbo.dto.*;
import com.rumbo.modelo.Carrera;
import com.rumbo.modelo.Publicacion;
import com.rumbo.modelo.Publicacion.Estado;
import com.rumbo.modelo.Publicacion.Tipo;
import com.rumbo.modelo.RespuestaPublicacion;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.*;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ForoService {

    private static final List<Estado> ESTADOS_VISIBLES =
            List.of(Estado.ACTIVA, Estado.VALIDADA);

    private final PublicacionRepository publicacionRepository;
    private final RespuestaPublicacionRepository respuestaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarreraRepository carreraRepository;

    public ForoService(
            PublicacionRepository publicacionRepository,
            RespuestaPublicacionRepository respuestaRepository,
            UsuarioRepository usuarioRepository,
            CarreraRepository carreraRepository) {

        this.publicacionRepository = publicacionRepository;
        this.respuestaRepository = respuestaRepository;
        this.usuarioRepository = usuarioRepository;
        this.carreraRepository = carreraRepository;
    }

    public Map<String, String> consultarAcceso(String idUsuario) {
        Usuario usuario = exigirAcceso(idUsuario);
        return Map.of("rol", codigo(usuario.getRol()));
    }

    public List<PublicacionForoRespuesta> listarPublicaciones(
            String idUsuario,
            String consulta,
            String tipoFiltro,
            Long carreraId,
            String autorId) {

        exigirAcceso(idUsuario);

        String busqueda = normalizar(consulta);

        if (busqueda.length() > 200) {
            throw new ExcepcionForo(400,
                    "La búsqueda admite hasta 200 caracteres.");
        }

        if (carreraId != null && carreraId < 1) {
            throw new ExcepcionForo(400, "La carrera no es válida.");
        }

        Tipo tipo = tipoFiltro == null || tipoFiltro.isBlank()
                ? null
                : leerTipo(tipoFiltro);

        return publicacionRepository
                .findByEstadoInOrderByFechaDescIdDesc(ESTADOS_VISIBLES)
                .stream()
                .filter(p -> tipo == null || p.getTipo() == tipo)
                .filter(p -> carreraId == null
                        || p.getCarreras().stream()
                                .anyMatch(c -> c.getId().equals(carreraId)))
                .filter(p -> autorId == null || autorId.isBlank()
                        || p.getAutor().getIdUsuario().equals(autorId.trim()))
                .filter(p -> busqueda.isEmpty()
                        || normalizar(p.getTitulo() + " " + p.getContenido())
                                .contains(busqueda))
                .map(this::mapearPublicacion)
                .toList();
    }

    public PublicacionForoRespuesta consultarPublicacion(
            Long id, String idUsuario) {

        exigirAcceso(idUsuario);
        return mapearPublicacion(buscarVisible(id));
    }

    @Transactional
    public PublicacionForoRespuesta crearPublicacion(
            CrearPublicacionSolicitud solicitud,
            String idUsuario) {

        Usuario autor = exigirAcceso(idUsuario);

        if (solicitud == null) {
            throw new ExcepcionForo(400, "La solicitud es obligatoria.");
        }

        String titulo = texto(solicitud.titulo(), "El título", 160);
        String contenido = texto(solicitud.contenido(), "El contenido", 6000);
        Tipo tipo = leerTipo(solicitud.tipo());

        if ("ESTUDIANTE".equals(codigo(autor.getRol()))
                && tipo != Tipo.DUDA) {
            throw new ExcepcionForo(403,
                    "Los estudiantes pueden crear publicaciones de tipo duda.");
        }

        List<Long> ids = solicitud.carrerasIds() == null
                ? List.of()
                : solicitud.carrerasIds();

        if (ids.size() > 3) {
            throw new ExcepcionForo(400,
                    "Puedes relacionar como máximo tres carreras.");
        }

        if (ids.stream().anyMatch(id -> id == null || id < 1)) {
            throw new ExcepcionForo(400,
                    "Los identificadores de carreras no son válidos.");
        }

        if (ids.stream().distinct().count() != ids.size()) {
            throw new ExcepcionForo(400,
                    "No puedes repetir una carrera.");
        }

        List<Carrera> carreras = carreraRepository.findAllById(ids);

        if (carreras.size() != ids.size()) {
            throw new ExcepcionForo(400,
                    "Una de las carreras seleccionadas no existe.");
        }

        Publicacion publicacion = new Publicacion(
                autor, titulo, contenido, tipo, carreras);

        return mapearPublicacion(publicacionRepository.save(publicacion));
    }

    public List<RespuestaForoRespuesta> listarRespuestas(
            Long publicacionId, String idUsuario) {

        exigirAcceso(idUsuario);

        // También comprueba que la publicación no esté eliminada.
        buscarVisible(publicacionId);

        return respuestaRepository
                .findByPublicacionIdOrderByFechaAscIdAsc(publicacionId)
                .stream()
                .map(this::mapearRespuesta)
                .toList();
    }

    @Transactional
    public RespuestaForoRespuesta crearRespuesta(
            Long publicacionId,
            CrearRespuestaSolicitud solicitud,
            String idUsuario) {

        Usuario autor = exigirAcceso(idUsuario);
        Publicacion publicacion = buscarVisible(publicacionId);

        if (solicitud == null) {
            throw new ExcepcionForo(400, "La solicitud es obligatoria.");
        }

        String contenido = texto(solicitud.contenido(), "La respuesta", 4000);

        RespuestaPublicacion respuesta = new RespuestaPublicacion(
                publicacion, autor, contenido);

        return mapearRespuesta(respuestaRepository.save(respuesta));
    }

    /* conexción provicional */

    private Usuario exigirAcceso(String idUsuario) {
        if (idUsuario == null || idUsuario.isBlank()) {
            throw new ExcepcionForo(401, "Debes iniciar sesión.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario.trim())
                .orElseThrow(() -> new ExcepcionForo(
                        401, "El usuario de la sesión no existe."));

        String rol = codigo(usuario.getRol());

        if ("COLABORADOR".equals(rol) || "ADMIN".equals(rol)) {
            return usuario;
        }

        if ("ESTUDIANTE".equals(rol)
                && "VERIFICADO".equals(codigo(
                        usuario.getEstadoVerificacion()))) {
            return usuario;
        }

        throw new ExcepcionForo(403,
                "El foro está disponible para estudiantes verificados "
                        + "y colaboradores.");
    }

    private Publicacion buscarVisible(Long id) {
        if (id == null || id < 1) {
            throw new ExcepcionForo(400,
                    "El identificador de publicación no es válido.");
        }

        return publicacionRepository
                .findByIdAndEstadoIn(id, ESTADOS_VISIBLES)
                .orElseThrow(() -> new ExcepcionForo(
                        404, "La publicación no está disponible."));
    }

    private Tipo leerTipo(String valor) {
        try {
            return Tipo.valueOf(codigo(valor));
        } catch (IllegalArgumentException e) {
            throw new ExcepcionForo(400,
                    "Selecciona un tipo de publicación válido.");
        }
    }

    private String texto(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new ExcepcionForo(400, campo + " es obligatorio.");
        }

        String limpio = valor.trim();

        if (limpio.length() > maximo) {
            throw new ExcepcionForo(400,
                    campo + " admite hasta " + maximo + " caracteres.");
        }

        return limpio;
    }

    private String codigo(String valor) {
        return valor == null
                ? ""
                : valor.trim().toUpperCase(Locale.ROOT);
    }

    private String codigo(Enum<?> valor) {
        return valor == null ? "" : valor.name();
    }

    private String normalizar(String valor) {
        return Normalizer.normalize(
                        valor == null ? "" : valor,
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private AutorForoRespuesta mapearAutor(Usuario usuario) {
        return new AutorForoRespuesta(
                usuario.getIdUsuario(),
                usuario.getNombreUsuario()
        );
    }

    private PublicacionForoRespuesta mapearPublicacion(Publicacion p) {
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
                carreras
        );
    }

    private RespuestaForoRespuesta mapearRespuesta(RespuestaPublicacion r) {
        return new RespuestaForoRespuesta(
                r.getId(),
                r.getContenido(),
                r.getFecha(),
                mapearAutor(r.getAutor())
        );
    }
}