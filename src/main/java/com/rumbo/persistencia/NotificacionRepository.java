package com.rumbo.persistencia;

import com.rumbo.modelo.Notificacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, String> {
    List<Notificacion> findByIdUsuarioOrderByFechaDesc(String idUsuario);
    Optional<Notificacion> findByIdNotificacionAndIdUsuario(String idNotificacion,
                                                              String idUsuario);
}
