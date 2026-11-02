package com.rumbo.servicio;
import com.rumbo.dto.*;
import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@Service
public class AdministracionPublicacionesService {
    private final UsuarioRepository usuarios;
    private final PublicacionRepository publicaciones;
    private final RegistroAdministracionRepository registros;
    public AdministracionPublicacionesService(UsuarioRepository usuarios, PublicacionRepository publicaciones,
            RegistroAdministracionRepository registros) {
        this.usuarios = usuarios; this.publicaciones = publicaciones; this.registros = registros;
    }
    private Usuario exigirAdmin(String id) {
        Usuario u = usuarios.findById(id).filter(cuenta -> !cuenta.isEliminado())
                .orElseThrow(() -> new ExcepcionAutenticacion("La cuenta ya no esta disponible"));
        if (u.getRol() != Rol.ADMIN) throw new ExcepcionPermiso("Se requiere rol de administrador");
        return u;
    }
    @Transactional(readOnly = true)
    public PaginaPublicacionesAdministracionRespuesta listar(String idAdministrador, int pagina, int tamano) {
        exigirAdmin(idAdministrador);
        if (pagina < 0 || tamano < 1 || tamano > 50) throw new ExcepcionValidacion(Map.of("paginacion", "Pagina desde 0 y tamano entre 1 y 50"));
        var resultado = publicaciones.findByEstadoIn(List.of(Publicacion.Estado.ACTIVA, Publicacion.Estado.VALIDADA),
                PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fecha", "id")));
        return new PaginaPublicacionesAdministracionRespuesta(resultado.getContent().stream()
                .map(p -> new PublicacionAdministracionRespuesta(p.getId(), p.getTitulo(), p.getContenido(),
                        p.getEstado().name(), p.getAutor().getIdUsuario(), p.getAutor().getNombreUsuario(), p.getFecha()))
                .toList(), pagina, resultado.getTotalPages(), resultado.getTotalElements());
    }
    @Transactional
    public RegistroAdministracionRespuesta eliminar(String idAdministrador, Long idPublicacion,
            EliminarPublicacionAdministracionSolicitud solicitud) {
        exigirAdmin(idAdministrador);
        if (idPublicacion == null || idPublicacion < 1) throw new ExcepcionValidacion(Map.of("id", "Identificador no valido"));
        if (solicitud == null || !idPublicacion.equals(solicitud.idConfirmacion())) {
            throw new ExcepcionValidacion(Map.of("idConfirmacion", "Confirma el identificador exacto de la publicacion"));
        }
        if (solicitud.motivo() == null || solicitud.motivo().isBlank() || solicitud.motivo().strip().length() > 1000) {
            throw new ExcepcionValidacion(Map.of("motivo", "Escribe un motivo de 1 a 1000 caracteres"));
        }
        Usuario admin = usuarios.bloquearAdministradoresActivos().stream()
                .filter(cuenta -> cuenta.getIdUsuario().equals(idAdministrador)).findFirst()
                .orElseThrow(() -> new ExcepcionAutenticacion("La cuenta ya no esta disponible"));
        if (admin.getRol() != Rol.ADMIN) throw new ExcepcionPermiso("Se requiere rol de administrador");
        Publicacion p = publicaciones.buscarParaRevision(idPublicacion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicacion no encontrada"));
        if (p.getEstado() == Publicacion.Estado.ELIMINADA) throw new ExcepcionConflicto("La publicacion ya fue eliminada");
        p.setEstado(Publicacion.Estado.ELIMINADA);
        publicaciones.saveAndFlush(p);
        RegistroAdministracion r = registros.saveAndFlush(new RegistroAdministracion(admin, p, solicitud.motivo().strip()));
        return new RegistroAdministracionRespuesta(r.getIdRegistro(), r.getIdAdministrador(), r.getNombreAdministrador(),
                r.getIdObjetivo(), r.getUsuarioObjetivo(), r.getRolObjetivo(), r.getAccion(), r.getMotivo(),
                r.getFecha(), r.getIdPublicacion(), r.getTituloPublicacion());
    }
}
