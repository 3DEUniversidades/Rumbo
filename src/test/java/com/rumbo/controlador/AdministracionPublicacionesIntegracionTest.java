package com.rumbo.controlador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumbo.dto.EliminarPublicacionAdministracionSolicitud;
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
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.sql.init.mode=never")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdministracionPublicacionesIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired PublicacionRepository publicaciones;
    @Autowired RespuestaPublicacionRepository respuestas;
    @Autowired RevisionMensajeRepository revisiones;
    @Autowired RegistroAdministracionRepository registros;
    @Autowired EntityManager em;
    private Long activa, validada;
    private String id(Rol rol) { return "b3-" + rol.name(); }
    private MockHttpSession sesion(Rol rol) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(GestorPermisos.ATRIBUTO_SESION,new SesionUsuario(id(rol), "Prueba", rol,"2026-11-02T00:00:00Z"));
        return s;
    }
    private String cuerpo(Long id, String motivo) throws Exception {
        return json.writeValueAsString(new EliminarPublicacionAdministracionSolicitud(id,motivo));
    }
    @BeforeEach
    void preparar() {
        String hash="$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji";
        for(Rol rol:Rol.values()) usuarios.save(new Usuario(id(rol), "b3_"+rol.name().toLowerCase(java.util.Locale.ROOT),
                "Prueba "+rol, rol.name().toLowerCase(java.util.Locale.ROOT)+"-b3@example.com",hash,rol,EstadoVerificacion.VERIFICADO));
        activa=publicaciones.saveAndFlush(new Publicacion(usuarios.findById(id(Rol.ESTUDIANTE)).orElseThrow(),
                "Duda de prueba", "Contenido A",Publicacion.Tipo.DUDA,List.of())).getId();
        Publicacion p=publicaciones.save(new Publicacion(usuarios.findById(id(Rol.COLABORADOR)).orElseThrow(),
                "Publicacion validada", "Contenido B",Publicacion.Tipo.EXPERIENCIA,List.of()));
        p.setEstado(Publicacion.Estado.VALIDADA); publicaciones.flush(); validada=p.getId();
        revisiones.save(new RevisionMensaje(p,usuarios.findById(id(Rol.SUPERVISOR_DE_MENSAJES)).orElseThrow(),RevisionMensaje.Decision.VALIDAR));
        respuestas.save(new RespuestaPublicacion(p,usuarios.findById(id(Rol.ESTUDIANTE)).orElseThrow(),"Respuesta que se conserva"));
        em.flush();em.clear();
    }
    @Test
    void adminConsultaActivasYValidadasConPaginacion() throws Exception {
        mvc.perform(get("/api/admin/publicaciones").session(sesion(Rol.ADMIN))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPublicaciones").value(2)).andExpect(jsonPath("$.publicaciones.length()").value(2))
                .andExpect(jsonPath("$.publicaciones[0].contrasenia").doesNotExist())
                .andExpect(jsonPath("$.publicaciones[0].correo").doesNotExist());
        mvc.perform(get("/api/admin/publicaciones?pagina=1&tamano=1").session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.publicaciones.length()").value(1));
    }
    @Test
    void eliminaActivaYLaOcultaDelForoYDeAdministracion() throws Exception {
        mvc.perform(delete("/api/admin/publicaciones/"+activa).session(sesion(Rol.ADMIN))
                .contentType(APPLICATION_JSON).content(cuerpo(activa,"  Motivo de prueba  ")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accion").value("ELIMINAR_PUBLICACION"))
                .andExpect(jsonPath("$.idPublicacion").value(activa.intValue()))
                .andExpect(jsonPath("$.tituloPublicacion").value("Duda de prueba"))
                .andExpect(jsonPath("$.motivo").value("Motivo de prueba"));
        em.flush();em.clear();
        assertEquals(Publicacion.Estado.ELIMINADA,publicaciones.findById(activa).orElseThrow().getEstado());
        mvc.perform(get("/api/foro/publicaciones/"+activa).session(sesion(Rol.ESTUDIANTE))).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/publicaciones").session(sesion(Rol.ADMIN))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPublicaciones").value(1));
        mvc.perform(get("/api/admin/registros").session(sesion(Rol.ADMIN))).andExpect(status().isOk())
                .andExpect(jsonPath("$.registros[0].idPublicacion").value(activa.intValue()))
                .andExpect(jsonPath("$.registros[0].nombreAdministrador").value("b3_admin"));
    }
    @Test
    void eliminaValidadaConservandoRevisionPreviaYRespuestas() throws Exception {
        mvc.perform(delete("/api/admin/publicaciones/"+validada).session(sesion(Rol.ADMIN))
                .contentType(APPLICATION_JSON).content(cuerpo(validada,"Retiro administrativo"))).andExpect(status().isOk());
        em.flush();em.clear();
        assertEquals(1,respuestas.countByPublicacionId(validada)); assertEquals(1,revisiones.count());
        assertEquals(RevisionMensaje.Decision.VALIDAR,revisiones.findAll().get(0).getDecision());
        assertEquals(1,registros.count());
        mvc.perform(get("/api/foro/publicaciones/"+validada).session(sesion(Rol.COLABORADOR))).andExpect(status().isNotFound());
        mvc.perform(get("/api/foro/publicaciones?autor="+id(Rol.COLABORADOR)).session(sesion(Rol.ESTUDIANTE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/moderacion/foro/mis-revisiones").session(sesion(Rol.SUPERVISOR_DE_MENSAJES)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].decision").value("VALIDAR"));
    }
    @Test
    void repeticionNoDuplicaAuditoria() throws Exception {
        String dato=cuerpo(activa,"Prueba de repeticion");
        mvc.perform(delete("/api/admin/publicaciones/"+activa).session(sesion(Rol.ADMIN))
                .contentType(APPLICATION_JSON).content(dato)).andExpect(status().isOk());
        mvc.perform(delete("/api/admin/publicaciones/"+activa).session(sesion(Rol.ADMIN))
                .contentType(APPLICATION_JSON).content(dato)).andExpect(status().isConflict());
        assertEquals(1,registros.count());
    }
    @ParameterizedTest
    @EnumSource(value=Rol.class,names="ADMIN",mode=EnumSource.Mode.EXCLUDE)
    void otrosRolesNoUsanHerramientasAdministrativas(Rol rol) throws Exception {
        mvc.perform(get("/api/admin/publicaciones").session(sesion(rol))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/publicaciones/"+activa).session(sesion(rol))
                .contentType(APPLICATION_JSON).content(cuerpo(activa,"No permitido"))).andExpect(status().isForbidden());
        mvc.perform(get("/administracion-publicaciones.html").session(sesion(rol))).andExpect(status().isForbidden());
        assertEquals(Publicacion.Estado.ACTIVA,publicaciones.findById(activa).orElseThrow().getEstado()); assertEquals(0,registros.count());
    }
    @Test
    void sinSesionSeRechazanPeticionesYPagina() throws Exception {
        mvc.perform(get("/api/admin/publicaciones")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/admin/publicaciones/"+activa).contentType(APPLICATION_JSON).content(cuerpo(activa,"Motivo")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/administracion-publicaciones.html")).andExpect(status().is3xxRedirection());
    }
    @Test
    void paginaDisponibleParaAdmin() throws Exception {
        mvc.perform(get("/administracion-publicaciones.html").session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("lista-publicaciones-admin")));
    }
    @Test
    void rechazaConfirmacionYMotivoInvalidos() throws Exception {
        for(EliminarPublicacionAdministracionSolicitud dato:new EliminarPublicacionAdministracionSolicitud[]{
                new EliminarPublicacionAdministracionSolicitud(null,"Motivo"),new EliminarPublicacionAdministracionSolicitud(activa+1000,"Motivo"),
                new EliminarPublicacionAdministracionSolicitud(activa,"  "),new EliminarPublicacionAdministracionSolicitud(activa,"x".repeat(1001))}) {
            mvc.perform(delete("/api/admin/publicaciones/"+activa).session(sesion(Rol.ADMIN))
                    .contentType(APPLICATION_JSON).content(json.writeValueAsString(dato))).andExpect(status().isBadRequest());
        }
        assertEquals(Publicacion.Estado.ACTIVA,publicaciones.findById(activa).orElseThrow().getEstado());assertEquals(0,registros.count());
    }
    @Test
    void identificadorInexistenteYLimitesNoProducenCambios() throws Exception {
        mvc.perform(delete("/api/admin/publicaciones/999999999").session(sesion(Rol.ADMIN)).contentType(APPLICATION_JSON)
                .content(cuerpo(999999999L,"Motivo"))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/publicaciones/0").session(sesion(Rol.ADMIN)).contentType(APPLICATION_JSON)
                .content(cuerpo(0L,"Motivo"))).andExpect(status().isBadRequest());
        for(String consulta:new String[]{"pagina=-1","tamano=0","tamano=51"}) {
            mvc.perform(get("/api/admin/publicaciones?"+consulta).session(sesion(Rol.ADMIN))).andExpect(status().isBadRequest());
        }
        assertEquals(0,registros.count());
    }
    @Test
    void adminEliminadoNoConservaAcceso() throws Exception {
        Usuario admin=usuarios.findById(id(Rol.ADMIN)).orElseThrow();admin.eliminarCuenta();usuarios.saveAndFlush(admin);
        mvc.perform(delete("/api/admin/publicaciones/"+activa).session(sesion(Rol.ADMIN)).contentType(APPLICATION_JSON)
                .content(cuerpo(activa,"Motivo"))).andExpect(status().isUnauthorized());assertEquals(0,registros.count());
    }
}
