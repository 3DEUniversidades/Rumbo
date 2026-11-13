package com.rumbo.persistencia;

import com.rumbo.modelo.RegistroActividadEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistroActividadEstudianteRepository
        extends JpaRepository<RegistroActividadEstudiante, String> {

    List<RegistroActividadEstudiante> findByUsuario_IdUsuario(
            String idUsuario
    );

    Optional<RegistroActividadEstudiante>
            findByUsuario_IdUsuarioAndActividad_IdActividad(
                    String idUsuario,
                    String idActividad
            );
}