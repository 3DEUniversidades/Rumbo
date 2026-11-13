package com.rumbo.persistencia;

import com.rumbo.modelo.ResultadoPrueba;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;

public interface ResultadoPruebaRepository extends JpaRepository<ResultadoPrueba, String> {
    @EntityGraph(attributePaths = {"prueba", "prueba.actividad"})
    List<ResultadoPrueba> findByUsuario_IdUsuarioOrderByFechaActualizacionDesc(String idUsuario);

    Optional<ResultadoPrueba> findByUsuario_IdUsuarioAndPrueba_IdPrueba(String idUsuario, String idPrueba);
}
