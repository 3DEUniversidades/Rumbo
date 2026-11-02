package com.rumbo.controlador;

import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import com.rumbo.servicio.GestorPermisos;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.sql.init.mode=never")
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class HistorialSupervisorIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired SolicitudVerificacionRepository verificaciones;
    @Autowired SolicitudActualizacionPerfilRepository perfiles;
    @Autowired RegistroSupervisionUsuariosRepository registros;
    @Autowired EntityManager em;
    private static final String HASH="$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji";
    private Usuario cuenta(String id,Rol rol){return new Usuario(id,id,"Prueba "+id,id+"@example.com",HASH,rol,EstadoVerificacion.NO_VERIFICADO);}
    private MockHttpSession sesion(String id,Rol rol){MockHttpSession s=new MockHttpSession();s.setAttribute(GestorPermisos.ATRIBUTO_SESION,
            new SesionUsuario(id,"Prueba",rol,Instant.now().toString()));return s;}
    private MockHttpSession supervisor(String id){return sesion(id,Rol.SUPERVISOR_DE_USUARIOS);}
    @BeforeEach void preparar()throws Exception{
        usuarios.save(cuenta("t10-supervisor-a",Rol.SUPERVISOR_DE_USUARIOS));
        usuarios.save(cuenta("t10-supervisor-b",Rol.SUPERVISOR_DE_USUARIOS));
        usuarios.save(cuenta("t10-supervisor-vacio",Rol.SUPERVISOR_DE_USUARIOS));
        for(Rol rol:Rol.values())if(rol!=Rol.SUPERVISOR_DE_USUARIOS)usuarios.save(cuenta("t10-actor-"+rol.name(),rol));
        for(int i=1;i<=5;i++)usuarios.save(cuenta("t10-alumno-"+i,Rol.ESTUDIANTE));
        for(int i:new int[]{1,2,5})verificaciones.save(new SolicitudVerificacion("t10-verif-"+i,"t10-alumno-"+i,"pendiente",Instant.now().toString()));
        for(int i:new int[]{3,4})perfiles.save(new SolicitudActualizacionPerfil("t10-perfil-"+i,"t10-alumno-"+i,
                "t10_nuevo_"+i,"Nombre actualizado "+i,"pendiente",Instant.now().toString()));
        em.flush();
        decidir("verificaciones","t10-verif-1","APROBAR","t10-supervisor-a");
        decidir("verificaciones","t10-verif-2","RECHAZAR","t10-supervisor-a");
        decidir("perfiles","t10-perfil-3","APROBAR","t10-supervisor-a");
        decidir("perfiles","t10-perfil-4","RECHAZAR","t10-supervisor-a");
        decidir("verificaciones","t10-verif-5","APROBAR","t10-supervisor-b");
        em.flush();em.clear();
    }
    private void decidir(String ruta,String id,String decision,String supervisor)throws Exception{
        mvc.perform(put("/api/supervision/usuarios/"+ruta+"/"+id).session(supervisor(supervisor))
                .contentType(APPLICATION_JSON).content("{\"decision\":\""+decision+"\",\"motivo\":\"Motivo de prueba\"}"))
                .andExpect(status().isNoContent());
    }
    @Test void incluyeLasVerificacionesYActualizacionesAprobadasYRechazadas()throws Exception{
        mvc.perform(get("/api/perfil/mis-decisiones").session(supervisor("t10-supervisor-a")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalDecisiones").value(4))
                .andExpect(jsonPath("$.decisiones[*].tipo",containsInAnyOrder("VERIFICACION","VERIFICACION","PERFIL","PERFIL")))
                .andExpect(jsonPath("$.decisiones[*].decision",containsInAnyOrder("APROBAR","APROBAR","RECHAZAR","RECHAZAR")))
                .andExpect(jsonPath("$.decisiones[0].fecha").isNotEmpty()).andExpect(jsonPath("$.decisiones[0].motivo").value("Motivo de prueba"));
    }
    @Test void cadaSupervisorSoloConsultaSusDecisionesInclusoConIdAjenoEnLaPeticion()throws Exception{
        mvc.perform(get("/api/perfil/mis-decisiones?idSupervisor=t10-supervisor-b").session(supervisor("t10-supervisor-a")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalDecisiones").value(4))
                .andExpect(content().string(not(containsString("t10-verif-5"))));
        mvc.perform(get("/api/perfil/mis-decisiones?idSupervisor=t10-supervisor-a").session(supervisor("t10-supervisor-b")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalDecisiones").value(1))
                .andExpect(jsonPath("$.decisiones[0].idSolicitud").value("t10-verif-5"));
    }
    @Test void filtrosYPaginacionFuncionanSinDuplicarRegistros()throws Exception{
        mvc.perform(get("/api/perfil/mis-decisiones?tipo=PERFIL&decision=APROBAR").session(supervisor("t10-supervisor-a")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalDecisiones").value(1))
                .andExpect(jsonPath("$.decisiones[0].idSolicitud").value("t10-perfil-3"));
        mvc.perform(get("/api/perfil/mis-decisiones?tipo=VERIFICACION&decision=RECHAZAR").session(supervisor("t10-supervisor-a")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalDecisiones").value(1))
                .andExpect(jsonPath("$.decisiones[0].idSolicitud").value("t10-verif-2"));
        mvc.perform(get("/api/perfil/mis-decisiones?pagina=1&tamano=1").session(supervisor("t10-supervisor-a")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(4)).andExpect(jsonPath("$.decisiones.length()").value(1));
        assertEquals(5,registros.count());
    }
    @Test void noExponeCredencialesNiDatosPersonalesExtra()throws Exception{
        mvc.perform(get("/api/perfil/mis-decisiones").session(supervisor("t10-supervisor-a")))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString(HASH))))
                .andExpect(content().string(not(containsString("@example.com"))))
                .andExpect(jsonPath("$.decisiones[0].correo").doesNotExist())
                .andExpect(jsonPath("$.decisiones[0].contrasenia").doesNotExist());
    }
    @Test void supervisorSinDecisionesYReferenciasAntiguasNoFallan()throws Exception{
        mvc.perform(get("/api/perfil/mis-decisiones").session(supervisor("t10-supervisor-vacio")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalDecisiones").value(0));
        registros.saveAndFlush(new RegistroSupervisionUsuarios("t10-supervisor-a","t10-solicitud-antigua","VERIFICACION","RECHAZAR",null));
        mvc.perform(get("/api/perfil/mis-decisiones").session(supervisor("t10-supervisor-a")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalDecisiones").value(5));
    }
    @ParameterizedTest @EnumSource(value=Rol.class,names="SUPERVISOR_DE_USUARIOS",mode=EnumSource.Mode.EXCLUDE)
    void otrosRolesNoUsanLaConsultaPersonalDelSupervisor(Rol rol)throws Exception{
        mvc.perform(get("/api/perfil/mis-decisiones").session(sesion("t10-actor-"+rol.name(),rol)))
                .andExpect(status().isForbidden());
    }
    @Test void noPermiteConsultarSinSesionNiConParametrosInvalidos()throws Exception{
        mvc.perform(get("/api/perfil/mis-decisiones")).andExpect(status().isUnauthorized());
        for(String consulta:new String[]{"tipo=FORO","decision=PENDIENTE","pagina=-1","tamano=0","tamano=51"}){
            mvc.perform(get("/api/perfil/mis-decisiones?"+consulta).session(supervisor("t10-supervisor-a")))
                    .andExpect(status().isBadRequest());
        }
    }
    @Test void supervisorEliminadoPierdeAcceso()throws Exception{
        Usuario u=usuarios.findById("t10-supervisor-a").orElseThrow();u.eliminarCuenta();usuarios.saveAndFlush(u);
        mvc.perform(get("/api/perfil/mis-decisiones").session(supervisor("t10-supervisor-a"))).andExpect(status().isUnauthorized());
    }
    @Test void elRolVigenteDeLaCuentaTambienSeComprueba()throws Exception{
        usuarios.saveAndFlush(cuenta("t10-supervisor-a",Rol.COLABORADOR));
        mvc.perform(get("/api/perfil/mis-decisiones").session(supervisor("t10-supervisor-a"))).andExpect(status().isForbidden());
    }
    @Test void perfilIncluyeLaSeccionSinRomperDatosDeLaCuenta()throws Exception{
        mvc.perform(get("/perfil.html").session(supervisor("t10-supervisor-a")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("seccion-historial-supervisor")));
        mvc.perform(get("/api/perfil").session(supervisor("t10-supervisor-a")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("SUPERVISOR_DE_USUARIOS"));
    }
}
