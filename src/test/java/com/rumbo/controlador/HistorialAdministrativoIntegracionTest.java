package com.rumbo.controlador;

import com.rumbo.dto.OrigenHistorialAdministrativo;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.sql.init.mode=never")
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class HistorialAdministrativoIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired CarreraRepository carreras;
    @Autowired RegistroAdministracionRepository administracion;
    @Autowired RegistroSupervisionUsuariosRepository supervision;
    @Autowired RegistroActualizacionCarreraRepository cambios;
    @Autowired RevisionMensajeRepository revisiones;
    @Autowired PublicacionRepository publicaciones;
    @Autowired SolicitudVerificacionRepository verificaciones;
    @Autowired SolicitudActualizacionPerfilRepository perfiles;
    @Autowired EntityManager em;
    private String id(Rol rol){return "hist-"+rol.name();}
    private MockHttpSession sesion(Rol rol){
        MockHttpSession s=new MockHttpSession();s.setAttribute(GestorPermisos.ATRIBUTO_SESION,
                new SesionUsuario(id(rol),"Prueba",rol,"2026-11-02T00:00:00Z"));return s;
    }
    @BeforeEach void preparar(){
        String hash="$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji";
        for(Rol rol:Rol.values())usuarios.save(new Usuario(id(rol),"hist_"+rol.name().toLowerCase(java.util.Locale.ROOT),
                "Prueba "+rol,rol.name().toLowerCase(java.util.Locale.ROOT)+"-hist@example.com",hash,rol,EstadoVerificacion.VERIFICADO));
        administracion.save(new RegistroAdministracion(usuarios.findById(id(Rol.ADMIN)).orElseThrow(),usuarios.findById(id(Rol.ESTUDIANTE)).orElseThrow(),"Motivo administrativo"));
        verificaciones.save(new SolicitudVerificacion("hist-verificacion",id(Rol.ESTUDIANTE),"aprobada",Instant.now().toString()));
        perfiles.save(new SolicitudActualizacionPerfil("hist-perfil",id(Rol.COLABORADOR),"hist_colaborador","Prueba", "rechazada",Instant.now().toString()));
        supervision.save(new RegistroSupervisionUsuarios(id(Rol.SUPERVISOR_DE_USUARIOS),"hist-verificacion","VERIFICACION","APROBAR","Datos comprobados"));
        supervision.save(new RegistroSupervisionUsuarios(id(Rol.SUPERVISOR_DE_USUARIOS),"hist-perfil","PERFIL","RECHAZAR","Datos inconsistentes"));
        Carrera c=carreras.save(new Carrera("Carrera historial",5,new BigDecimal("1000.00")));
        cambios.save(new RegistroActualizacionCarrera(c,"descripcion","Antes","Despues",LocalDateTime.now(),id(Rol.ENCARGADO_DE_CARRERAS)));
        Publicacion p=publicaciones.save(new Publicacion(usuarios.findById(id(Rol.COLABORADOR)).orElseThrow(),"Publicacion historica","Texto publico",Publicacion.Tipo.EXPERIENCIA,List.of()));
        p.setEstado(Publicacion.Estado.VALIDADA);
        revisiones.save(new RevisionMensaje(p,usuarios.findById(id(Rol.SUPERVISOR_DE_MENSAJES)).orElseThrow(),RevisionMensaje.Decision.VALIDAR));
        em.flush();em.clear();
    }
    @ParameterizedTest @EnumSource(OrigenHistorialAdministrativo.class)
    void adminConsultaCadaOrigenSinExponerCredenciales(OrigenHistorialAdministrativo origen)throws Exception{
        mvc.perform(get("/api/admin/historial?origen="+origen.name()).session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.origen").value(origen.name()))
                .andExpect(jsonPath("$.registros[0].responsable").isNotEmpty())
                .andExpect(content().string(not(containsString("$2a$10$"))))
                .andExpect(content().string(not(containsString("@example.com"))))
                .andExpect(jsonPath("$.registros[0].contrasenia").doesNotExist());
    }
    @Test void supervisionIncluyeVerificacionYCambioDePerfil()throws Exception{
        mvc.perform(get("/api/admin/historial?origen=SUPERVISION").session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(2))
                .andExpect(jsonPath("$.registros[*].accion",containsInAnyOrder("APROBAR","RECHAZAR")))
                .andExpect(jsonPath("$.registros[*].objetivo",containsInAnyOrder("Cuenta: hist_estudiante","Cuenta: hist_colaborador")));
    }
    @Test void carrerasIncluyeValoresYResponsableSinDuplicarHistorial()throws Exception{
        mvc.perform(get("/api/admin/historial?origen=CARRERAS").session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.registros[0].valorAnterior").value("Antes"))
                .andExpect(jsonPath("$.registros[0].valorNuevo").value("Despues"))
                .andExpect(jsonPath("$.registros[0].responsable").value("hist_encargado_de_carreras"));
        assertEquals(1,cambios.count());assertEquals(1,administracion.count());assertEquals(2,supervision.count());assertEquals(1,revisiones.count());
    }
    @Test void historialSinFilasYReferenciasAusentesNoFalla()throws Exception{
        cambios.deleteAll();cambios.flush();
        mvc.perform(get("/api/admin/historial?origen=CARRERAS").session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(0));
        supervision.saveAndFlush(new RegistroSupervisionUsuarios("responsable-antiguo","solicitud-antigua","VERIFICACION","APROBAR","Registro previo"));
        mvc.perform(get("/api/admin/historial?origen=SUPERVISION").session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(3));
    }
    @Test void paginaSoloRegistrosDelOrigenSeleccionado()throws Exception{
        mvc.perform(get("/api/admin/historial?origen=SUPERVISION&pagina=1&tamano=1").session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.registros.length()").value(1)).andExpect(jsonPath("$.totalPaginas").value(2));
        for(String consulta:new String[]{"origen=NO_EXISTE","pagina=-1","tamano=0","tamano=51"}){
            mvc.perform(get("/api/admin/historial?"+consulta).session(sesion(Rol.ADMIN))).andExpect(status().isBadRequest());
        }
    }
    @ParameterizedTest @EnumSource(value=Rol.class,names="ADMIN",mode=EnumSource.Mode.EXCLUDE)
    void otrosRolesNoAccedenAlHistorialGeneral(Rol rol)throws Exception{
        mvc.perform(get("/api/admin/historial?origen=SUPERVISION").session(sesion(rol))).andExpect(status().isForbidden());
        mvc.perform(get("/registros-administrativos.html").session(sesion(rol))).andExpect(status().isForbidden());
    }
    @Test void paginaSoloAdminYSinSesionNoAccede()throws Exception{
        mvc.perform(get("/registros-administrativos.html").session(sesion(Rol.ADMIN))).andExpect(status().isOk());
        mvc.perform(get("/api/admin/historial")).andExpect(status().isUnauthorized());
        mvc.perform(get("/registros-administrativos.html")).andExpect(status().is3xxRedirection());
    }
    @Test void administradorEliminadoPierdeAcceso()throws Exception{
        Usuario u=usuarios.findById(id(Rol.ADMIN)).orElseThrow();u.eliminarCuenta();usuarios.saveAndFlush(u);
        mvc.perform(get("/api/admin/historial").session(sesion(Rol.ADMIN))).andExpect(status().isUnauthorized());
    }
}
