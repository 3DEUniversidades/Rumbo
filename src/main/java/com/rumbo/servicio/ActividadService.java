package com.rumbo.servicio;

import com.rumbo.modelo.Actividad;
import com.rumbo.persistencia.ActividadRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActividadService {

    private final ActividadRepository actividadRepository;

    public ActividadService(ActividadRepository actividadRepository) {
        this.actividadRepository = actividadRepository;
    }

    public List<Actividad> obtenerLineaDeTiempo() {
        return actividadRepository.findAllByOrderByFechaAsc();
    }

    public Actividad marcarCompletada(String idActividad) {
        Actividad actividad = actividadRepository.findById(idActividad)
                .orElseThrow(() -> new ExcepcionActividad("Actividad no encontrada: " + idActividad));

        actividad.setCompletada(!actividad.isCompletada());
        return actividadRepository.save(actividad);
    }
}
