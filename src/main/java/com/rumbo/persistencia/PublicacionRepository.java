package com.rumbo.persistencia;

import com.rumbo.modelo.Publicacion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {

    @EntityGraph(attributePaths = {"autor", "carreras"})
    List<Publicacion> findByEstadoInOrderByFechaDescIdDesc(
            Collection<Publicacion.Estado> estados);

    @EntityGraph(attributePaths = {"autor", "carreras"})
    Optional<Publicacion> findByIdAndEstadoIn(
            Long id,
            Collection<Publicacion.Estado> estados);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Publicacion p where p.id = :id")
    Optional<Publicacion> buscarParaRevision(@Param("id") Long id);
}