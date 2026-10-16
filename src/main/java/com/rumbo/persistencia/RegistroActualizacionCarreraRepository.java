package com.rumbo.persistencia;

import com.rumbo.modelo.RegistroActualizacionCarrera;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroActualizacionCarreraRepository extends JpaRepository<RegistroActualizacionCarrera, Long> {

    List<RegistroActualizacionCarrera> findAllByOrderByFechaDesc();

    List<RegistroActualizacionCarrera> findByCarreraIdOrderByFechaDesc(Long carreraId);
}