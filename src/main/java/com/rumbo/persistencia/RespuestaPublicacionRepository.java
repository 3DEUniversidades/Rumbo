package com.rumbo.persistencia;

import com.rumbo.modelo.RespuestaPublicacion;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespuestaPublicacionRepository
        extends JpaRepository<RespuestaPublicacion, Long> {

    long countByPublicacionId(Long publicacionId);

    @EntityGraph(attributePaths = "autor")
    List<RespuestaPublicacion> findByPublicacionIdOrderByFechaAscIdAsc(
            Long publicacionId);
}
