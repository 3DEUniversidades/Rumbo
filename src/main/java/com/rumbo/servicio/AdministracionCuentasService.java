package com.rumbo.servicio;

import com.rumbo.dto.*;
import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdministracionCuentasService {
    private final UsuarioRepository usuarios;
    private final RegistroAdministracionRepository registros;
    public AdministracionCuentasService(UsuarioRepository usuarios, RegistroAdministracionRepository registros) {
        this.usuarios = usuarios; this.registros = registros;
    }
    private Usuario exigirAdmin(String id) {
        Usuario u = usuarios.findById(id).filter(cuenta -> !cuenta.isEliminado())
                .orElseThrow(() -> new ExcepcionAutenticacion("La cuenta ya no esta disponible"));
        if (u.getRol() != Rol.ADMIN) throw new ExcepcionPermiso("Se requiere rol de administrador");
        return u;
    }
    @Transactional
    public RegistroAdministracionRespuesta eliminar(String idAdministrador, String idObjetivo, EliminarCuentaSolicitud solicitud) {
        exigirAdmin(idAdministrador);
        Map<String,String> errores = new LinkedHashMap<>();
        if (solicitud == null || solicitud.nombreUsuarioConfirmacion() == null || solicitud.nombreUsuarioConfirmacion().isBlank()) {
            errores.put("nombreUsuarioConfirmacion", "Escribe el nombre de usuario para confirmar");
        }
        if (solicitud == null || solicitud.motivo() == null || solicitud.motivo().isBlank()) {
            errores.put("motivo", "Indica el motivo de la eliminacion");
        } else if (solicitud.motivo().strip().length() > 1000) {
            errores.put("motivo", "El maximo es de 1000 caracteres");
        }
        if (!errores.isEmpty()) throw new ExcepcionValidacion(errores);
        if (idAdministrador.equals(idObjetivo)) throw new ExcepcionConflicto("No puedes eliminar tu propia cuenta administradora");
        // Serializa las eliminaciones administrativas y comprueba de nuevo al actor.
        Usuario administrador = usuarios.bloquearAdministradoresActivos().stream()
                .filter(u -> u.getIdUsuario().equals(idAdministrador)).findFirst()
                .orElseThrow(() -> new ExcepcionAutenticacion("La cuenta ya no esta disponible"));
        Usuario objetivo = usuarios.buscarParaEliminar(idObjetivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
        if (objetivo.isEliminado()) throw new ExcepcionConflicto("La cuenta ya fue eliminada");
        if (!objetivo.getNombreUsuario().equals(solicitud.nombreUsuarioConfirmacion().strip())) {
            throw new ExcepcionValidacion(Map.of("nombreUsuarioConfirmacion", "El nombre no coincide con la cuenta seleccionada"));
        }
        objetivo.eliminarCuenta();
        usuarios.saveAndFlush(objetivo);
        return respuesta(registros.saveAndFlush(new RegistroAdministracion(administrador, objetivo, solicitud.motivo().strip())));
    }
    @Transactional(readOnly = true)
    public PaginaRegistrosAdministracionRespuesta consultarRegistros(String idAdministrador, int pagina, int tamano) {
        exigirAdmin(idAdministrador);
        if (pagina < 0 || tamano < 1 || tamano > 50) throw new ExcepcionValidacion(Map.of("paginacion", "Pagina desde 0 y tamano entre 1 y 50"));
        var resultado = registros.findAll(PageRequest.of(pagina, tamano,
                Sort.by(Sort.Direction.DESC, "fecha", "idRegistro")));
        return new PaginaRegistrosAdministracionRespuesta(resultado.getContent().stream().map(this::respuesta).toList(),
                pagina, resultado.getTotalPages(), resultado.getTotalElements());
    }
    private RegistroAdministracionRespuesta respuesta(RegistroAdministracion r) {
        return new RegistroAdministracionRespuesta(r.getIdRegistro(), r.getIdAdministrador(), r.getNombreAdministrador(),
                r.getIdObjetivo(), r.getUsuarioObjetivo(), r.getRolObjetivo(), r.getAccion(), r.getMotivo(), r.getFecha(), r.getIdPublicacion(), r.getTituloPublicacion());
    }
}
