package com.rumbo.servicio;

import com.rumbo.dto.*;
import com.rumbo.modelo.*;
import com.rumbo.persistencia.UsuarioRepository;
import com.rumbo.persistencia.PerfilRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ConsultaUsuariosAdministracionService {
    private final UsuarioRepository usuarios;
    private final PerfilRepository perfiles;
    public ConsultaUsuariosAdministracionService(UsuarioRepository usuarios, PerfilRepository perfiles) {
        this.usuarios = usuarios; this.perfiles = perfiles;
    }
    private void exigirAdministradorVigente(String idAdministrador) {
        Usuario admin = usuarios.findById(idAdministrador)
                .orElseThrow(() -> new ExcepcionAutenticacion("La cuenta ya no esta disponible"));
        if (admin.isEliminado()) throw new ExcepcionAutenticacion("La cuenta ya no esta disponible");
        if (admin.getRol() != Rol.ADMIN) throw new ExcepcionPermiso("Se requiere rol de administrador");
    }
    public PaginaUsuariosAdministracionRespuesta listar(String idAdministrador, int pagina, int tamano) {
        exigirAdministradorVigente(idAdministrador);
        if (pagina < 0 || tamano < 1 || tamano > 50) {
            throw new ExcepcionValidacion(Map.of("paginacion", "Pagina desde 0 y tamano entre 1 y 50"));
        }
        var resultado = usuarios.findByEliminadoFalse(PageRequest.of(pagina, tamano,
                Sort.by("nombreUsuario").ascending().and(Sort.by("idUsuario"))));
        var resumen = resultado.getContent().stream().map(u -> new UsuarioAdministracionResumen(
                u.getIdUsuario(), u.getNombreUsuario(), u.getNombreCompleto(), u.getRol(), u.getEstadoVerificacion())).toList();
        return new PaginaUsuariosAdministracionRespuesta(resumen, pagina, tamano,
                resultado.getTotalElements(), resultado.getTotalPages());
    }
    public PerfilAdministracionRespuesta consultar(String idAdministrador, String idUsuario) {
        exigirAdministradorVigente(idAdministrador);
        Usuario u = usuarios.findById(idUsuario).filter(usuario -> !usuario.isEliminado()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Perfil p = perfiles.findByUsuarioIdUsuario(idUsuario).orElse(null);
        Carrera c = p == null ? null : p.getCarrera();
        return new PerfilAdministracionRespuesta(u.getIdUsuario(), u.getNombreUsuario(),
                u.getNombreCompleto(), u.getCorreo(), u.getRol(), u.getEstadoVerificacion(), p != null,
                p == null ? null : p.getNivelEstudios(), p == null ? null : p.getDescripcionPersonal(),
                p == null ? null : p.getAreaLaboral(), c == null ? null : c.getId(),
                c == null ? null : c.getNombre(), p == null ? null : p.getFotoUrl());
    }
}
