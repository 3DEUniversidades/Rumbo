package com.rumbo.persistencia;

import com.rumbo.modelo.SolicitudActualizacionPerfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SolicitudActualizacionPerfilRepository
        extends JpaRepository<SolicitudActualizacionPerfil, String> {

    boolean existsByIdUsuarioAndEstado(String idUsuario, String estado);

    Optional<SolicitudActualizacionPerfil> findFirstByIdUsuarioAndEstadoOrderByFechaSolicitudDesc(
            String idUsuario, String estado);
}
