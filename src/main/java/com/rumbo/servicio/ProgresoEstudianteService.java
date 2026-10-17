package com.rumbo.servicio;

import com.rumbo.dto.ActividadEstudianteRespuesta;
import com.rumbo.dto.ProgresoEstudianteRespuesta;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.rumbo.modelo.Actividad;
import com.rumbo.modelo.EstadoActividad;
import com.rumbo.modelo.RegistroActividadEstudiante;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.ActividadRepository;
import com.rumbo.persistencia.RegistroActividadEstudianteRepository;
import com.rumbo.persistencia.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgresoEstudianteService {

    private final RegistroActividadEstudianteRepository registroRepository;
    private final ActividadRepository actividadRepository;
    private final UsuarioRepository usuarioRepository;

    public ProgresoEstudianteService(
            RegistroActividadEstudianteRepository registroRepository,
            ActividadRepository actividadRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.registroRepository = registroRepository;
        this.actividadRepository = actividadRepository;
        this.usuarioRepository = usuarioRepository;
    }

@Transactional
public RegistroActividadEstudiante registrarDecision(
        String idUsuario,
        String idActividad,
        boolean decidioRealizar,
        String razon
) {
    if (razon == null || razon.isBlank()) {
        throw new IllegalArgumentException(
                "Debes indicar la razón de tu decisión"
        );
    }

    RegistroActividadEstudiante registro =
            obtenerOCrearRegistro(idUsuario, idActividad);

    registro.setDecidioRealizar(decidioRealizar);
    registro.setRazonDecision(razon.strip());

    if (!decidioRealizar) {
        registro.setEstado(EstadoActividad.RECHAZADA);
    } else if (registro.getEstado() != EstadoActividad.COMPLETADA) {
        registro.setEstado(EstadoActividad.PENDIENTE);
    }

    return registroRepository.save(registro);
}

@Transactional
public RegistroActividadEstudiante marcarCompletada(
        String idUsuario,
        String idActividad
) {
    RegistroActividadEstudiante registro =
            obtenerOCrearRegistro(idUsuario, idActividad);

    registro.setDecidioRealizar(true);
    registro.setEstado(EstadoActividad.COMPLETADA);

    return registroRepository.save(registro);
}

@Transactional
public RegistroActividadEstudiante guardarResultados(
        String idUsuario,
        String idActividad,
        String resultados
) {
    if (resultados == null) {
        throw new IllegalArgumentException(
                "Debes enviar el contenido de los resultados"
        );
    }

    RegistroActividadEstudiante registro =
            obtenerOCrearRegistro(idUsuario, idActividad);

    registro.setResultadosRegistrados(resultados.strip());

    return registroRepository.save(registro);
}

@Transactional(readOnly = true)
public List<ActividadEstudianteRespuesta> consultarLineaDeTiempo(
        String idUsuario
) {
    if (idUsuario == null || idUsuario.isBlank()) {
        throw new IllegalArgumentException(
                "El identificador del usuario es obligatorio"
        );
    }

    if (!usuarioRepository.existsById(idUsuario)) {
        throw new IllegalArgumentException("Usuario no encontrado");
    }

    List<Actividad> actividades =
            actividadRepository.findByEliminadaFalseOrderByFechaAsc();

    Map<String, RegistroActividadEstudiante> registrosPorActividad =
            registroRepository.findByUsuario_IdUsuario(idUsuario)
                    .stream()
                    .collect(Collectors.toMap(
                            registro -> registro.getActividad().getIdActividad(),
                            registro -> registro
                    ));

    return actividades.stream()
            .map(actividad -> construirRespuesta(
                    actividad,
                    registrosPorActividad.get(actividad.getIdActividad())
            ))
            .toList();
}

@Transactional(readOnly = true)
public ActividadEstudianteRespuesta consultarDetalleActividad(
        String idUsuario,
        String idActividad
) {
    if (idUsuario == null || idUsuario.isBlank()) {
        throw new IllegalArgumentException(
                "El identificador del usuario es obligatorio"
        );
    }

    if (idActividad == null || idActividad.isBlank()) {
        throw new IllegalArgumentException(
                "El identificador de la actividad es obligatorio"
        );
    }

    if (!usuarioRepository.existsById(idUsuario)) {
        throw new IllegalArgumentException("Usuario no encontrado");
    }

    Actividad actividad = actividadRepository.findByIdActividadAndEliminadaFalse(idActividad)
            .orElseThrow(() -> new ExcepcionActividad(
                    "Actividad no encontrada: " + idActividad
            ));

    RegistroActividadEstudiante registro = registroRepository
            .findByUsuario_IdUsuarioAndActividad_IdActividad(
                    idUsuario, idActividad
            )
            .orElse(null);

    return construirRespuesta(actividad, registro);
}

@Transactional(readOnly = true)
public ProgresoEstudianteRespuesta consultarProgresoGeneral(
        String idUsuario
) {
    List<ActividadEstudianteRespuesta> actividades =
            consultarLineaDeTiempo(idUsuario);

    long total = actividades.size();

    long completadas = actividades.stream()
            .filter(actividad ->
                    actividad.estado() == EstadoActividad.COMPLETADA
            )
            .count();

    double porcentaje = total == 0
            ? 0.0
            : completadas * 100.0 / total;

    return new ProgresoEstudianteRespuesta(
            total,
            completadas,
            porcentaje
    );
}

private ActividadEstudianteRespuesta construirRespuesta(
        Actividad actividad,
        RegistroActividadEstudiante registro
) {
    return new ActividadEstudianteRespuesta(
            actividad.getIdActividad(),
            actividad.getFecha(),
            actividad.getDescripcion(),
            actividad.getContexto(),
            actividad.getInstrucciones(),
            actividad.getAccionesARealizar(),
            actividad.getMetodoRecomendado(),
            actividad.getHerramientasDisponibles(),
            actividad.getInformacionAGuardar(),
            actividad.getTipoActividad(),
            registro == null ? null : registro.getDecidioRealizar(),
            registro == null ? null : registro.getRazonDecision(),
            registro == null
                    ? EstadoActividad.PENDIENTE
                    : registro.getEstado(),
            registro == null ? null : registro.getResultadosRegistrados(),
            registro == null ? null : registro.getFechaActualizacion()
    );
}

@Transactional
public RegistroActividadEstudiante marcarNoRealizada(
        String idUsuario,
        String idActividad,
        String razon
) {
    if (razon == null || razon.isBlank()) {
        throw new IllegalArgumentException(
                "Debes indicar por qué no realizaste la actividad"
        );
    }

    RegistroActividadEstudiante registro =
            obtenerOCrearRegistro(idUsuario, idActividad);

    registro.setDecidioRealizar(true);
    registro.setRazonDecision(razon.strip());
    registro.setEstado(EstadoActividad.NO_REALIZADA);

    return registroRepository.save(registro);
}

private RegistroActividadEstudiante obtenerOCrearRegistro(
        String idUsuario,
        String idActividad
) {
    if (idUsuario == null || idUsuario.isBlank()) {
        throw new IllegalArgumentException(
                "El identificador del usuario es obligatorio"
        );
    }

    if (idActividad == null || idActividad.isBlank()) {
        throw new IllegalArgumentException(
                "El identificador de la actividad es obligatorio"
        );
    }

    Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new IllegalArgumentException(
                    "Usuario no encontrado"
            ));

    Actividad actividad = actividadRepository.findByIdActividadAndEliminadaFalse(idActividad)
            .orElseThrow(() -> new ExcepcionActividad(
                    "Actividad no encontrada: " + idActividad
            ));

    return registroRepository
            .findByUsuario_IdUsuarioAndActividad_IdActividad(
                    idUsuario, idActividad
            )
            .orElseGet(() ->
                    new RegistroActividadEstudiante(usuario, actividad)
            );
}

}
