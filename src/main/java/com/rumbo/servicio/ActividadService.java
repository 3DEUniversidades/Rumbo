package com.rumbo.servicio;

import com.rumbo.dto.ActividadSolicitud;
import com.rumbo.modelo.Actividad;
import com.rumbo.persistencia.ActividadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class ActividadService {
    private final ActividadRepository actividadRepository;
    private final ValidadorDatos validadorDatos;

    public ActividadService(ActividadRepository actividadRepository, ValidadorDatos validadorDatos) {
        this.actividadRepository = actividadRepository;
        this.validadorDatos = validadorDatos;
    }

    @Transactional(readOnly = true)
    public List<Actividad> obtenerLineaDeTiempo() {
        return actividadRepository.findByEliminadaFalseOrderByFechaAsc();
    }

    @Transactional(readOnly = true)
    public Actividad consultarActividad(String id) {
        return actividadRepository.findByIdActividadAndEliminadaFalse(id)
                .orElseThrow(() -> new ExcepcionActividad("Actividad no encontrada: " + id));
    }

    @Transactional
    public Actividad crearActividad(ActividadSolicitud solicitud) {
        validadorDatos.exigirActividadValida(solicitud);
        Actividad actividad = new Actividad(
                "act-" + UUID.randomUUID(),
                solicitud.fecha().strip(), solicitud.descripcion().strip(),
                solicitud.contexto().strip(), solicitud.instrucciones().strip(),
                solicitud.accionesARealizar().strip(), solicitud.metodoRecomendado().strip(),
                solicitud.herramientasDisponibles().strip(), solicitud.informacionAGuardar().strip(),
                solicitud.tipoActividad().strip()
        );
        return actividadRepository.save(actividad);
    }

    @Transactional
    public Actividad actualizarActividad(String id, ActividadSolicitud solicitud) {
        validadorDatos.exigirActividadValida(solicitud);
        Actividad actividad = consultarActividad(id);
        actividad.setFecha(solicitud.fecha().strip());
        actividad.setDescripcion(solicitud.descripcion().strip());
        actividad.setContexto(solicitud.contexto().strip());
        actividad.setInstrucciones(solicitud.instrucciones().strip());
        actividad.setAccionesARealizar(solicitud.accionesARealizar().strip());
        actividad.setMetodoRecomendado(solicitud.metodoRecomendado().strip());
        actividad.setHerramientasDisponibles(solicitud.herramientasDisponibles().strip());
        actividad.setInformacionAGuardar(solicitud.informacionAGuardar().strip());
        actividad.setTipoActividad(solicitud.tipoActividad().strip());
        return actividadRepository.save(actividad);
    }

    @Transactional
    public void eliminarActividad(String id) {
        Actividad actividad = consultarActividad(id);
        actividad.retirarDeLineaDeTiempo();
        actividadRepository.save(actividad);
    }
}
