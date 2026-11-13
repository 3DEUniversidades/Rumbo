package com.rumbo.persistencia;

import com.rumbo.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    org.springframework.data.domain.Page<Usuario> findByEliminadoFalse(org.springframework.data.domain.Pageable pagina);

    boolean existsByIdUsuarioAndEliminadoTrue(String idUsuario);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.rol = com.rumbo.modelo.Rol.ADMIN and u.eliminado = false order by u.idUsuario")
    java.util.List<Usuario> bloquearAdministradoresActivos();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.idUsuario = :id")
    Optional<Usuario> buscarParaEliminar(@org.springframework.data.repository.query.Param("id") String id);

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario);
}
