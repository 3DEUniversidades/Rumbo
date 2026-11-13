package com.rumbo.servicio;

import com.rumbo.dto.AdjuntoBitacoraRespuesta;
import com.rumbo.dto.EntradaBitacoraRespuesta;
import com.rumbo.modelo.Actividad;
import com.rumbo.modelo.AdjuntoBitacora;
import com.rumbo.modelo.EntradaBitacora;
import com.rumbo.modelo.RegistroActividadEstudiante;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.ActividadRepository;
import com.rumbo.persistencia.AdjuntoBitacoraRepository;
import com.rumbo.persistencia.EntradaBitacoraRepository;
import com.rumbo.persistencia.RegistroActividadEstudianteRepository;
import com.rumbo.persistencia.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BitacoraService {

    private static final Logger LOG =
            LoggerFactory.getLogger(BitacoraService.class);

    private static final int MAX_ADJUNTOS_POR_ENTRADA = 5;
    private static final long MAX_ADJUNTO_BYTES = 10L * 1024 * 1024;
    private static final String CARPETA_BITACORA = "bitacora";
    private static final String ENTREGA_AUTENTICADA = "authenticated";

    private final EntradaBitacoraRepository entradaRepository;
    private final AdjuntoBitacoraRepository adjuntoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ActividadRepository actividadRepository;
    private final RegistroActividadEstudianteRepository registroRepository;
    private final AlmacenamientoArchivos almacenamiento;
    private final ValidadorArchivos validadorArchivos;

    public BitacoraService(
            EntradaBitacoraRepository entradaRepository,
            AdjuntoBitacoraRepository adjuntoRepository,
            UsuarioRepository usuarioRepository,
            ActividadRepository actividadRepository,
            RegistroActividadEstudianteRepository registroRepository,
            AlmacenamientoArchivos almacenamiento,
            ValidadorArchivos validadorArchivos
    ) {
        this.entradaRepository = entradaRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.usuarioRepository = usuarioRepository;
        this.actividadRepository = actividadRepository;
        this.registroRepository = registroRepository;
        this.almacenamiento = almacenamiento;
        this.validadorArchivos = validadorArchivos;
    }

    @Transactional
    public EntradaBitacoraRespuesta crear(
            String idUsuario,
            String contenido,
            String idActividad
    ) {
        Usuario usuario = obtenerUsuario(idUsuario);
        String texto = exigirContenido(contenido);
        Actividad actividad = obtenerActividadOpcional(idActividad);

        EntradaBitacora entrada = new EntradaBitacora(usuario, texto, actividad);

        return construirRespuesta(entradaRepository.save(entrada));
    }

    @Transactional(readOnly = true)
    public List<EntradaBitacoraRespuesta> listar(String idUsuario) {
        obtenerUsuario(idUsuario);

        return entradaRepository
                .findByUsuario_IdUsuarioOrderByFechaCreacionDesc(idUsuario)
                .stream()
                .map(this::construirRespuesta)
                .toList();
    }

    @Transactional
    public EntradaBitacoraRespuesta editar(
            String idUsuario,
            String idEntrada,
            String contenido
    ) {
        EntradaBitacora entrada = obtenerEntradaPropia(idUsuario, idEntrada);
        String texto = exigirContenido(contenido);

        entrada.actualizarContenido(texto);

        return construirRespuesta(entradaRepository.save(entrada));
    }

    @Transactional
    public void eliminar(String idUsuario, String idEntrada) {
        EntradaBitacora entrada = obtenerEntradaPropia(idUsuario, idEntrada);

        for (AdjuntoBitacora adjunto : entrada.getAdjuntos()) {
            almacenamiento.eliminar(
                    adjunto.getPublicId(),
                    adjunto.getTipoRecurso(),
                    adjunto.getTipoEntrega()
            );
        }

        entradaRepository.delete(entrada);
    }

    @Transactional
    public AdjuntoBitacoraRespuesta agregarAdjunto(
            String idUsuario,
            String idEntrada,
            MultipartFile archivo
    ) {
        EntradaBitacora entrada = obtenerEntradaPropia(idUsuario, idEntrada);

        if (adjuntoRepository.countByEntrada_IdEntrada(idEntrada)
                >= MAX_ADJUNTOS_POR_ENTRADA) {
            throw new ExcepcionValidacion(Map.of(
                    "archivo",
                    "Puedes adjuntar hasta " + MAX_ADJUNTOS_POR_ENTRADA
                            + " archivos por entrada"
            ));
        }

        ValidadorArchivos.ArchivoValidado validado =
                validadorArchivos.validarAdjunto(archivo, MAX_ADJUNTO_BYTES);

        boolean archivoCrudo = "raw".equals(validado.tipoRecurso());
        String publicId = UUID.randomUUID().toString();
        if (archivoCrudo) {
            publicId = publicId + "." + validado.extension();
        }

        ArchivoAlmacenado subido = almacenamiento.subir(
                archivo,
                CARPETA_BITACORA + "/" + idUsuario + "/" + idEntrada,
                publicId,
                validado.tipoRecurso(),
                ENTREGA_AUTENTICADA
        );

        AdjuntoBitacora adjunto = new AdjuntoBitacora(
                entrada,
                subido.publicId(),
                subido.tipoRecurso(),
                subido.tipoEntrega(),
                subido.formato() == null || subido.formato().isBlank()
                        ? validado.extension()
                        : subido.formato(),
                validado.nombreOriginal(),
                validado.tipoMime(),
                subido.tamanoBytes()
        );

        try {
            AdjuntoBitacora guardado = adjuntoRepository.save(adjunto);
            entrada.agregarAdjunto(guardado);
            return mapearAdjunto(guardado);
        } catch (RuntimeException e) {
            eliminarSilencioso(
                    subido.publicId(),
                    subido.tipoRecurso(),
                    subido.tipoEntrega()
            );
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<AdjuntoBitacoraRespuesta> listarAdjuntos(
            String idUsuario,
            String idEntrada
    ) {
        EntradaBitacora entrada = obtenerEntradaPropia(idUsuario, idEntrada);

        return entrada.getAdjuntos().stream()
                .map(this::mapearAdjunto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DescargaAdjunto obtenerContenidoAdjunto(
            String idUsuario,
            String idEntrada,
            String idAdjunto
    ) {
        EntradaBitacora entrada = obtenerEntradaPropia(idUsuario, idEntrada);
        AdjuntoBitacora adjunto = obtenerAdjuntoPropio(entrada, idAdjunto);
        RecursoDescargado recurso = almacenamiento.descargar(
                adjunto.getPublicId(),
                adjunto.getTipoRecurso()
        );

        return new DescargaAdjunto(
                adjunto.getNombreOriginal(),
                adjunto.getTipoMime(),
                adjunto.getTamanoBytes(),
                recurso
        );
    }

    @Transactional
    public void eliminarAdjunto(
            String idUsuario,
            String idEntrada,
            String idAdjunto
    ) {
        EntradaBitacora entrada = obtenerEntradaPropia(idUsuario, idEntrada);
        AdjuntoBitacora adjunto = obtenerAdjuntoPropio(entrada, idAdjunto);

        almacenamiento.eliminar(
                adjunto.getPublicId(),
                adjunto.getTipoRecurso(),
                adjunto.getTipoEntrega()
        );

        entrada.quitarAdjunto(adjunto);
        entradaRepository.save(entrada);
    }

    @Transactional
    public EntradaBitacoraRespuesta relacionarActividad(
            String idUsuario,
            String idEntrada,
            String idActividad
    ) {
        EntradaBitacora entrada = obtenerEntradaPropia(idUsuario, idEntrada);
        Actividad actividad = obtenerActividadOpcional(idActividad);

        entrada.relacionarActividad(actividad);

        return construirRespuesta(entradaRepository.save(entrada));
    }

    @Transactional
    public EntradaBitacoraRespuesta crearDesdeResultados(
            String idUsuario,
            String idActividad
    ) {
        Usuario usuario = obtenerUsuario(idUsuario);
        Actividad actividad = obtenerActividad(idActividad);

        RegistroActividadEstudiante registro = registroRepository
                .findByUsuario_IdUsuarioAndActividad_IdActividad(
                        idUsuario,
                        idActividad
                )
                .orElse(null);

        if (registro == null
                || registro.getResultadosRegistrados() == null
                || registro.getResultadosRegistrados().isBlank()) {
            throw new ExcepcionValidacion(Map.of(
                    "resultados",
                    "La actividad no tiene resultados registrados"
            ));
        }

        EntradaBitacora entrada = new EntradaBitacora(
                usuario,
                registro.getResultadosRegistrados().strip(),
                actividad
        );

        return construirRespuesta(entradaRepository.save(entrada));
    }

    private EntradaBitacora obtenerEntradaPropia(
            String idUsuario,
            String idEntrada
    ) {
        if (idEntrada == null || idEntrada.isBlank()) {
            throw new ExcepcionBitacora("Entrada de bitácora no encontrada");
        }

        return entradaRepository
                .findByIdEntradaAndUsuario_IdUsuario(idEntrada, idUsuario)
                .orElseThrow(() -> new ExcepcionBitacora(
                        "Entrada de bitácora no encontrada"
                ));
    }

    private AdjuntoBitacora obtenerAdjuntoPropio(
            EntradaBitacora entrada,
            String idAdjunto
    ) {
        if (idAdjunto == null || idAdjunto.isBlank()) {
            throw new ExcepcionBitacora("Adjunto no encontrado");
        }

        return entrada.getAdjuntos().stream()
                .filter(adjunto -> idAdjunto.equals(adjunto.getIdAdjunto()))
                .findFirst()
                .orElseThrow(() -> new ExcepcionBitacora("Adjunto no encontrado"));
    }

    private void eliminarSilencioso(
            String publicId,
            String tipoRecurso,
            String tipoEntrega
    ) {
        try {
            almacenamiento.eliminar(publicId, tipoRecurso, tipoEntrega);
        } catch (ExcepcionAlmacenamiento e) {
            LOG.warn("No se pudo limpiar el adjunto {}", publicId, e);
        }
    }

    private Usuario obtenerUsuario(String idUsuario) {
        if (idUsuario == null || idUsuario.isBlank()) {
            throw new ExcepcionValidacion(Map.of(
                    "usuario",
                    "El usuario de la sesión no es válido"
            ));
        }

        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ExcepcionValidacion(Map.of(
                        "usuario",
                        "Usuario no encontrado"
                )));
    }

    private Actividad obtenerActividadOpcional(String idActividad) {
        if (idActividad == null || idActividad.isBlank()) {
            return null;
        }

        return obtenerActividad(idActividad);
    }

    private Actividad obtenerActividad(String idActividad) {
        if (idActividad == null || idActividad.isBlank()) {
            throw new ExcepcionValidacion(Map.of(
                    "idActividad",
                    "El identificador de la actividad es obligatorio"
            ));
        }

        return actividadRepository
                .findByIdActividadAndEliminadaFalse(idActividad)
                .orElseThrow(() -> new ExcepcionValidacion(Map.of(
                        "idActividad",
                        "La actividad no existe"
                )));
    }

    private String exigirContenido(String contenido) {
        if (contenido == null || contenido.isBlank()) {
            throw new ExcepcionValidacion(Map.of(
                    "contenido",
                    "El contenido es obligatorio"
            ));
        }

        return contenido.strip();
    }

    private EntradaBitacoraRespuesta construirRespuesta(
            EntradaBitacora entrada
    ) {
        Actividad actividad = entrada.getActividad();

        List<AdjuntoBitacoraRespuesta> adjuntos = entrada.getAdjuntos()
                .stream()
                .map(this::mapearAdjunto)
                .toList();

        return new EntradaBitacoraRespuesta(
                entrada.getIdEntrada(),
                entrada.getContenido(),
                entrada.getFechaCreacion(),
                entrada.getFechaEdicion(),
                actividad == null ? null : actividad.getIdActividad(),
                actividad == null ? null : actividad.getDescripcion(),
                adjuntos
        );
    }

    private AdjuntoBitacoraRespuesta mapearAdjunto(
            AdjuntoBitacora adjunto
    ) {
        String url = "/api/bitacora/"
                + adjunto.getEntrada().getIdEntrada()
                + "/adjuntos/"
                + adjunto.getIdAdjunto()
                + "/contenido";

        return new AdjuntoBitacoraRespuesta(
                adjunto.getIdAdjunto(),
                adjunto.getNombreOriginal(),
                adjunto.getTipoMime(),
                adjunto.getTamanoBytes(),
                "image".equals(adjunto.getTipoRecurso()),
                url
        );
    }

    public record DescargaAdjunto(
            String nombreOriginal,
            String tipoMime,
            long tamanoBytes,
            RecursoDescargado recurso
    ) {
    }
}
