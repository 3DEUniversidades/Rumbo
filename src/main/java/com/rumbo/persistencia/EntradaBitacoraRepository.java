package com.rumbo.persistencia;

import com.rumbo.modelo.EntradaBitacora;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntradaBitacoraRepository
        extends JpaRepository<EntradaBitacora, String> {

    @EntityGraph(attributePaths = "actividad")
    List<EntradaBitacora> findByUsuario_IdUsuarioOrderByFechaCreacionDesc(
            String idUsuario
    );

    @EntityGraph(attributePaths = "actividad")
    Optional<EntradaBitacora> findByIdEntradaAndUsuario_IdUsuario(
            String idEntrada,
            String idUsuario
    );
}
