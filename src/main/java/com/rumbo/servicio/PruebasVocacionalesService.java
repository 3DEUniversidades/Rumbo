package com.rumbo.servicio;

import com.rumbo.dto.*;
import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PruebasVocacionalesService {
    private final PruebaVocacionalRepository pruebas;
    private final ResultadoPruebaRepository resultados;
    private final UsuarioRepository usuarios;
    private final ActividadRepository actividades;
    private final ValidadorResultadoPrueba validador;

    public PruebasVocacionalesService(PruebaVocacionalRepository pruebas,
            ResultadoPruebaRepository resultados, UsuarioRepository usuarios,
            ActividadRepository actividades, ValidadorResultadoPrueba validador) {
        this.pruebas = pruebas; this.resultados = resultados; this.usuarios = usuarios;
        this.actividades = actividades; this.validador = validador;
    }

    private Usuario exigirEstudiante(String idUsuario) {
        Usuario usuario = usuarios.findById(idUsuario)
                .orElseThrow(() -> new ExcepcionAutenticacion("La cuenta ya no esta disponible"));
        if (usuario.getRol() != Rol.ESTUDIANTE) throw new ExcepcionPermiso("Se requiere rol de estudiante");
        return usuario;
    }

    private PruebaVocacional exigirPrueba(String idPrueba) {
        return pruebas.buscarDisponible(idPrueba)
                .orElseThrow(() -> new ExcepcionPruebaNoEncontrada("Prueba no disponible"));
    }

    @Transactional(readOnly = true)
    public List<PruebaVocacionalRespuesta> consultarPruebas(String idUsuario) {
        exigirEstudiante(idUsuario);
        Map<String, ResultadoPrueba> propios = resultados.findByUsuario_IdUsuarioOrderByFechaActualizacionDesc(idUsuario)
                .stream().collect(Collectors.toMap(r -> r.getPrueba().getIdPrueba(), r -> r));
        return pruebas.consultarDisponibles().stream()
                .map(p -> respuesta(p, propios.get(p.getIdPrueba()))).toList();
    }

    @Transactional(readOnly = true)
    public List<PruebaVocacionalRespuesta> consultarPorActividad(String idUsuario, String idActividad) {
        exigirEstudiante(idUsuario);
        if (actividades.findByIdActividadAndEliminadaFalse(idActividad).isEmpty()) {
            throw new ExcepcionPruebaNoEncontrada("Actividad no disponible");
        }
        return consultarPruebas(idUsuario).stream().filter(p -> p.idActividad().equals(idActividad)).toList();
    }

    @Transactional(readOnly = true)
    public PruebaVocacionalRespuesta consultarPrueba(String idUsuario, String idPrueba) {
        exigirEstudiante(idUsuario);
        PruebaVocacional prueba = exigirPrueba(idPrueba);
        return respuesta(prueba, resultados.findByUsuario_IdUsuarioAndPrueba_IdPrueba(idUsuario, idPrueba).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<ResultadoPruebaRespuesta> consultarMisResultados(String idUsuario) {
        exigirEstudiante(idUsuario);
        return resultados.findByUsuario_IdUsuarioOrderByFechaActualizacionDesc(idUsuario)
                .stream().map(this::respuestaResultado).toList();
    }

    @Transactional
    public ResultadoPruebaRespuesta guardarResultado(String idUsuario, String idPrueba,
            ResultadoPruebaSolicitud solicitud) {
        Usuario estudiante = exigirEstudiante(idUsuario);
        validador.exigirValido(solicitud);
        PruebaVocacional prueba = exigirPrueba(idPrueba);
        ResultadoPrueba resultado = resultados.findByUsuario_IdUsuarioAndPrueba_IdPrueba(idUsuario, idPrueba)
                .orElseGet(() -> new ResultadoPrueba(estudiante, prueba));
        resultado.actualizar(solicitud.fechaRealizacion(), solicitud.resultado().strip(),
                solicitud.interpretacionPersonal() == null ? "" : solicitud.interpretacionPersonal().strip());
        return respuestaResultado(resultados.saveAndFlush(resultado));
    }

    private PruebaVocacionalRespuesta respuesta(PruebaVocacional p, ResultadoPrueba r) {
        return new PruebaVocacionalRespuesta(p.getIdPrueba(), p.getActividad().getIdActividad(),
                p.getNombre(), p.getTipo(), p.getProveedor(), p.getProposito(), p.getContexto(),
                p.getInstrucciones(), p.getMetodo(), p.getEnlaceExterno(), p.getRecursoInterpretacionUrl(),
                p.getOrientacionInterpretacion(), p.getCondicionesAcceso(), r == null ? null : respuestaResultado(r));
    }

    private ResultadoPruebaRespuesta respuestaResultado(ResultadoPrueba r) {
        PruebaVocacional p = r.getPrueba();
        return new ResultadoPruebaRespuesta(r.getIdResultado(), p.getIdPrueba(), p.getActividad().getIdActividad(),
                p.getNombre(), p.getTipo(), r.getFechaRealizacion(), r.getResultado(), r.getInterpretacionPersonal(),
                r.getFechaActualizacion(), !p.getActividad().isEliminada());
    }
}
