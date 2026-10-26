package com.rumbo.persistencia;

import com.rumbo.modelo.SolicitudVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SolicitudVerificacionRepository extends JpaRepository<SolicitudVerificacion, String> {

    @Query("select s from SolicitudVerificacion s where s.idUsuario = :idUsuario " +
            "order by s.fechaSolicitud desc")
    List<SolicitudVerificacion> findByIdUsuario(@Param("idUsuario") String idUsuario);

    boolean existsByIdUsuarioAndEstado(String idUsuario, String estado);

    List<SolicitudVerificacion> findByEstado(String estado);

    List<SolicitudVerificacion> findByEstadoOrderByFechaSolicitudAsc(String estado);
}
