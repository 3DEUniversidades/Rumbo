package com.rumbo.persistencia;

import com.rumbo.modelo.PruebaVocacional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PruebaVocacionalRepository extends JpaRepository<PruebaVocacional, String> {
    @EntityGraph(attributePaths = "actividad")
    @Query("select p from PruebaVocacional p where p.actividad.eliminada = false order by p.nombre")
    List<PruebaVocacional> consultarDisponibles();

    @EntityGraph(attributePaths = "actividad")
    @Query("select p from PruebaVocacional p where p.idPrueba = :id and p.actividad.eliminada = false")
    Optional<PruebaVocacional> buscarDisponible(@Param("id") String id);
}
