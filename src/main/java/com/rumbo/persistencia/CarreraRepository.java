package com.rumbo.persistencia;

import com.rumbo.modelo.Carrera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import java.util.List;

public interface CarreraRepository extends JpaRepository<Carrera, Long> {
    List<Carrera> findByUniversidadIgnoreCase(String universidad, Sort orden);
    boolean existsByNombreIgnoreCaseAndUniversidadIgnoreCase(String nombre, String universidad);
    boolean existsByNombreIgnoreCaseAndUniversidadIgnoreCaseAndIdNot(String nombre, String universidad, Long id);
}
