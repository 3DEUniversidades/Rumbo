package com.rumbo.servicio;

import com.rumbo.dto.NotificacionResponse;
import com.rumbo.modelo.Notificacion;
import com.rumbo.persistencia.NotificacionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacionService {

    private final NotificacionRepository repository;

    public NotificacionService(NotificacionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void crear(String idUsuario, String titulo, String mensaje) {
        repository.save(new Notificacion(idUsuario, titulo, mensaje));
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(String idUsuario) {
        return repository.findByIdUsuarioOrderByFechaDesc(idUsuario).stream()
                .map(n -> new NotificacionResponse(n.getIdNotificacion(), n.getTitulo(),
                        n.getMensaje(), n.isLeida(), n.getFecha()))
                .toList();
    }

    @Transactional
    public void marcarLeida(String idUsuario, String idNotificacion) {
        Notificacion notificacion = repository
                .findByIdNotificacionAndIdUsuario(idNotificacion, idUsuario)
                .orElseThrow(() -> new ExcepcionValidacion(
                        java.util.Map.of("notificacion", "La notificacion no existe")));
        notificacion.marcarLeida();
    }
}
