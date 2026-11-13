package com.rumbo.servicio;

import com.rumbo.dto.*;
import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class HistorialAdministrativoService {
    private final UsuarioRepository usuarios;
    private final RegistroAdministracionRepository administracion;
    private final RegistroSupervisionUsuariosRepository supervision;
    private final RegistroActualizacionCarreraRepository carreras;
    private final RevisionMensajeRepository moderacion;
    private final SolicitudVerificacionRepository verificaciones;
    private final SolicitudActualizacionPerfilRepository cambiosPerfil;
    public HistorialAdministrativoService(UsuarioRepository usuarios, RegistroAdministracionRepository administracion,
            RegistroSupervisionUsuariosRepository supervision, RegistroActualizacionCarreraRepository carreras,
            RevisionMensajeRepository moderacion, SolicitudVerificacionRepository verificaciones,
            SolicitudActualizacionPerfilRepository cambiosPerfil) {
        this.usuarios=usuarios;this.administracion=administracion;this.supervision=supervision;
        this.carreras=carreras;this.moderacion=moderacion;this.verificaciones=verificaciones;this.cambiosPerfil=cambiosPerfil;
    }
    private PageRequest pagina(int pagina,int tamano,String id) {
        return PageRequest.of(pagina,tamano,Sort.by(Sort.Direction.DESC,"fecha",id));
    }
    public PaginaHistorialAdministrativoRespuesta consultar(String idAdministrador,OrigenHistorialAdministrativo origen,int pagina,int tamano) {
        Usuario admin=usuarios.findById(idAdministrador).filter(u -> !u.isEliminado())
                .orElseThrow(() -> new ExcepcionAutenticacion("La cuenta ya no esta disponible"));
        if(admin.getRol()!=Rol.ADMIN) throw new ExcepcionPermiso("Se requiere rol de administrador");
        if(origen==null || pagina<0 || tamano<1 || tamano>50) throw new ExcepcionValidacion(Map.of("consulta","Selecciona un origen, pagina desde 0 y tamano de 1 a 50"));
        Map<String,String> nombres=new HashMap<>();
        Page<RegistroHistorialAdministrativoRespuesta> resultado=switch(origen) {
            case ADMINISTRACION -> administracion.findAll(pagina(pagina,tamano,"idRegistro")).map(r ->
                new RegistroHistorialAdministrativoRespuesta(r.getIdRegistro(),origen,r.getAccion(),texto(r.getFecha()),
                        r.getIdAdministrador(),r.getNombreAdministrador(),
                        r.getIdPublicacion()==null ? "Cuenta: "+r.getUsuarioObjetivo() : "Publicacion #"+r.getIdPublicacion()+": "+r.getTituloPublicacion(),
                        r.getIdPublicacion()==null ? "Rol: "+r.getRolObjetivo() : "Autor: "+r.getUsuarioObjetivo(),
                        r.getMotivo(),null,null));
            case SUPERVISION -> supervision.findAll(pagina(pagina,tamano,"idRegistro")).map(r ->
                new RegistroHistorialAdministrativoRespuesta(r.getIdRegistro(),origen,r.getDecision(),r.getFecha(),
                        r.getIdSupervisor(),nombre(r.getIdSupervisor(),nombres),objetivoSupervision(r,nombres),
                        r.getTipo()+" - Solicitud: "+r.getIdSolicitud(),r.getMotivo(),null,null));
            case CARRERAS -> carreras.findAll(pagina(pagina,tamano,"id")).map(r ->
                new RegistroHistorialAdministrativoRespuesta(String.valueOf(r.getId()),origen,"ACTUALIZAR_CARRERA",texto(r.getFecha()),
                        r.getResponsable(),nombre(r.getResponsable(),nombres),"Carrera #"+r.getCarrera().getId()+": "+r.getCarrera().getNombre(),
                        "Campo: "+r.getCampo(),null,r.getValorAnterior(),r.getValorNuevo()));
            case MODERACION -> moderacion.findAll(pagina(pagina,tamano,"id")).map(r ->
                new RegistroHistorialAdministrativoRespuesta(String.valueOf(r.getId()),origen,r.getDecision().name(),texto(r.getFecha()),
                        r.getSupervisor().getIdUsuario(),r.getSupervisor().getNombreUsuario(),
                        "Publicacion #"+r.getPublicacion().getId()+": "+r.getPublicacion().getTitulo(),
                        "Revision del supervisor de mensajes",null,null,null));
        };
        return new PaginaHistorialAdministrativoRespuesta(resultado.getContent(),origen,pagina,resultado.getTotalPages(),resultado.getTotalElements());
    }
    private String nombre(String id,Map<String,String> nombres) {
        if(id==null || id.isBlank()) return "Responsable no registrado";
        return nombres.computeIfAbsent(id,valor -> usuarios.findById(valor).map(Usuario::getNombreUsuario).orElse(valor));
    }
    private String objetivoSupervision(RegistroSupervisionUsuarios r,Map<String,String> nombres) {
        if(r.getIdSolicitud()==null || r.getIdSolicitud().isBlank()) return "Solicitud sin identificador registrado";
        String id=null;
        if("VERIFICACION".equalsIgnoreCase(r.getTipo())) {
            id=verificaciones.findById(r.getIdSolicitud()).map(SolicitudVerificacion::getIdUsuario).orElse(null);
        } else if("PERFIL".equalsIgnoreCase(r.getTipo())) {
            id=cambiosPerfil.findById(r.getIdSolicitud()).map(SolicitudActualizacionPerfil::getIdUsuario).orElse(null);
        }
        return id==null ? "Solicitud: "+r.getIdSolicitud() : "Cuenta: "+nombre(id,nombres);
    }
    private String texto(Object valor) { return valor==null ? null : valor.toString(); }
}
