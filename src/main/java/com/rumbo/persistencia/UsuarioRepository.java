package com.rumbo.persistencia;

import com.rumbo.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByCorreo(String correo);
}
