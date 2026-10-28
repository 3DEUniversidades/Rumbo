package com.rumbo.servicio;

import com.rumbo.dto.EntradaBitacoraRespuesta;
import com.rumbo.modelo.Actividad;
import com.rumbo.modelo.EntradaBitacora;
import com.rumbo.modelo.RegistroActividadEstudiante;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.ActividadRepository;
import com.rumbo.persistencia.EntradaBitacoraRepository;
import com.rumbo.persistencia.RegistroActividadEstudianteRepository;
import com.rumbo.persistencia.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class BitacoraService {

    private final EntradaBitacoraRepository entradaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ActividadRepository actividadRepository;
    private final RegistroActividadEstudianteRepository registroRepository;

    public BitacoraService(
            EntradaBitacoraRepository entradaRepository,
            UsuarioRepository usuarioRepository,
            ActividadRepository actividadRepository,
            RegistroActividadEstudianteRepository registroRepository
    ) {
        this.entradaRepository = entradaRepository;
        this.usuarioRepository = usuarioRepository;
        this.actividadRepository = actividadRepository;
        this.registroRepository = registroRepository;
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

        entradaRepository.delete(entrada);
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

        return new EntradaBitacoraRespuesta(
                entrada.getIdEntrada(),
                entrada.getContenido(),
                entrada.getFechaCreacion(),
                entrada.getFechaEdicion(),
                actividad == null ? null : actividad.getIdActividad(),
                actividad == null ? null : actividad.getDescripcion()
        );
    }
}
