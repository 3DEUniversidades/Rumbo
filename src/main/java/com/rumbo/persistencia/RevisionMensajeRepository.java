package com.rumbo.persistencia;

import com.rumbo.modelo.RevisionMensaje;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevisionMensajeRepository
        extends JpaRepository<RevisionMensaje, Long> {

    @EntityGraph(attributePaths = {"publicacion", "supervisor"})
    List<RevisionMensaje> findBySupervisorIdUsuarioOrderByFechaDescIdDesc(
            String idUsuario);
}