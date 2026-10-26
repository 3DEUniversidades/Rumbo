package com.rumbo.persistencia;

import com.rumbo.modelo.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActividadRepository extends JpaRepository<Actividad, String> {

    List<Actividad> findAllByOrderByFechaAsc();

    List<Actividad> findByEliminadaFalseOrderByFechaAsc();

    Optional<Actividad> findByIdActividadAndEliminadaFalse(String idActividad);
}
