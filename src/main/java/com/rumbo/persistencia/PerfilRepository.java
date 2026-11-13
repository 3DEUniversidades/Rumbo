package com.rumbo.persistencia;

import com.rumbo.modelo.Perfil;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepository extends JpaRepository<Perfil, String> {

    Optional<Perfil> findByUsuarioIdUsuario(String idUsuario);
}
