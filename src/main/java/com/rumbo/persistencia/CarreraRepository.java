package com.rumbo.persistencia;

import com.rumbo.modelo.Carrera;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarreraRepository extends JpaRepository<Carrera, Long> {
}
