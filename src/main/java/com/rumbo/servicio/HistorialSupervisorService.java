package com.rumbo.servicio;
import com.rumbo.dto.*;
import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly=true)
public class HistorialSupervisorService {
    private final UsuarioRepository usuarios;
    private final ConsultaHistorialSupervisorRepository historial;
    private final SolicitudVerificacionRepository verificaciones;
    private final SolicitudActualizacionPerfilRepository perfiles;
    public HistorialSupervisorService(UsuarioRepository usuarios,ConsultaHistorialSupervisorRepository historial,
            SolicitudVerificacionRepository verificaciones,SolicitudActualizacionPerfilRepository perfiles) {
        this.usuarios=usuarios;this.historial=historial;this.verificaciones=verificaciones;this.perfiles=perfiles;
    }
    public PaginaDecisionesSupervisorRespuesta consultar(String idSupervisor,String tipo,String decision,int pagina,int tamano) {
        Usuario supervisor=usuarios.findById(idSupervisor).filter(u -> !u.isEliminado())
                .orElseThrow(() -> new ExcepcionAutenticacion("La cuenta ya no esta disponible"));
        if(supervisor.getRol()!=Rol.SUPERVISOR_DE_USUARIOS)throw new ExcepcionPermiso("Se requiere rol de supervisor de usuarios");
        String tipoNormalizado=normalizar(tipo),decisionNormalizada=normalizar(decision);
        if(!Set.of("TODAS","VERIFICACION","PERFIL").contains(tipoNormalizado))throw new ExcepcionValidacion(Map.of("tipo","Selecciona todas, verificaciones o cambios de perfil"));
        if(!Set.of("TODAS","APROBAR","RECHAZAR").contains(decisionNormalizada))throw new ExcepcionValidacion(Map.of("decision","Selecciona todas, aprobadas o rechazadas"));
        if(pagina<0||tamano<1||tamano>50)throw new ExcepcionValidacion(Map.of("paginacion","Pagina desde 0 y tamano de 1 a 50"));
        var resultado=historial.consultarPropias(idSupervisor,tipoNormalizado,decisionNormalizada,
                PageRequest.of(pagina,tamano,Sort.by(Sort.Direction.DESC,"fecha","idRegistro")));
        return new PaginaDecisionesSupervisorRespuesta(resultado.getContent().stream().map(this::respuesta).toList(),
                pagina,resultado.getTotalPages(),resultado.getTotalElements());
    }
    private DecisionHistorialSupervisorRespuesta respuesta(RegistroSupervisionUsuarios r) {
        String idUsuario=null;
        if(r.getIdSolicitud()!=null&&!r.getIdSolicitud().isBlank()) {
            if("VERIFICACION".equals(r.getTipo()))idUsuario=verificaciones.findById(r.getIdSolicitud()).map(SolicitudVerificacion::getIdUsuario).orElse(null);
            else if("PERFIL".equals(r.getTipo()))idUsuario=perfiles.findById(r.getIdSolicitud()).map(SolicitudActualizacionPerfil::getIdUsuario).orElse(null);
        }
        String nombre=idUsuario==null?null:usuarios.findById(idUsuario).map(Usuario::getNombreUsuario).orElse(null);
        return new DecisionHistorialSupervisorRespuesta(r.getIdRegistro(),r.getIdSolicitud(),r.getTipo(),r.getDecision(),r.getFecha(),r.getMotivo(),idUsuario,nombre);
    }
    private String normalizar(String valor){return valor==null||valor.isBlank()?"TODAS":valor.strip().toUpperCase(Locale.ROOT);}
}
