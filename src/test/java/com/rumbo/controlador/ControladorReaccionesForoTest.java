package com.rumbo.controlador;

import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import com.rumbo.servicio.GestorPermisos;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ControladorReaccionesForoTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PublicacionRepository publicaciones;
    @Autowired PasswordEncoder codificador;
    @Autowired EntityManager entidades;

    private Long publicacionId;

    @BeforeEach
    void preparar() {
        String hash = codificador.encode("clave-de-prueba");
        Usuario autor = guardarUsuario("autor", Rol.ESTUDIANTE,
                EstadoVerificacion.VERIFICADO, hash);
        guardarUsuario("colaborador", Rol.COLABORADOR,
                EstadoVerificacion.VERIFICADO, hash);
        guardarUsuario("pendiente", Rol.ESTUDIANTE,
                EstadoVerificacion.NO_VERIFICADO, hash);
        guardarUsuario("supervisor", Rol.SUPERVISOR_DE_MENSAJES,
                EstadoVerificacion.VERIFICADO, hash);
        guardarUsuario("admin", Rol.ADMIN, EstadoVerificacion.VERIFICADO, hash);
        publicacionId = publicaciones.saveAndFlush(new Publicacion(
                autor, "Una duda", "Contenido de prueba",
                Publicacion.Tipo.DUDA, List.of())).getId();
    }

    @Test
    void meGustaNoSeDuplicaYSePuedeQuitar() throws Exception {
        MockHttpSession autor = sesion("autor", Rol.ESTUDIANTE);
        for (int i = 0; i < 2; i++) {
            mvc.perform(put(ruta()).session(autor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cantidadReacciones").value(1))
                    .andExpect(jsonPath("$.meGusta").value(true));
            entidades.flush();
            entidades.clear();
        }
        mvc.perform(put(ruta()).session(sesion("colaborador", Rol.COLABORADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadReacciones").value(2));
        for (int i = 0; i < 2; i++) {
            mvc.perform(delete(ruta()).session(autor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cantidadReacciones").value(1))
                    .andExpect(jsonPath("$.meGusta").value(false));
            entidades.flush();
            entidades.clear();
        }
    }

    @Test
    void contadoresSeConsultanEnDetalleYListado() throws Exception {
        MockHttpSession autor = sesion("autor", Rol.ESTUDIANTE);
        mvc.perform(put(ruta()).session(autor)).andExpect(status().isOk());
        mvc.perform(post("/api/foro/publicaciones/{id}/respuestas", publicacionId)
                        .session(autor).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\":\"Una respuesta\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/foro/publicaciones/{id}", publicacionId).session(autor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadRespuestas").value(1))
                .andExpect(jsonPath("$.cantidadReacciones").value(1));
        mvc.perform(get("/api/foro/publicaciones").param("autor", "autor")
                        .session(sesion("colaborador", Rol.COLABORADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cantidadRespuestas").value(1))
                .andExpect(jsonPath("$[0].cantidadReacciones").value(1))
                .andExpect(jsonPath("$[0].meGusta").value(false));
    }

    @Test
    void rechazaReaccionesSinSesionYUsuariosSinAcceso() throws Exception {
        mvc.perform(put(ruta())).andExpect(status().isUnauthorized());
        mvc.perform(put(ruta()).session(sesion("pendiente", Rol.ESTUDIANTE)))
                .andExpect(status().isForbidden());
        mvc.perform(delete(ruta()).session(sesion("supervisor", Rol.SUPERVISOR_DE_MENSAJES)))
                .andExpect(status().isForbidden());
        mvc.perform(put(ruta()).session(sesion("admin", Rol.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaPublicacionesEliminadasEIdentificadoresInvalidos() throws Exception {
        MockHttpSession autor = sesion("autor", Rol.ESTUDIANTE);
        Publicacion publicacion = publicaciones.findById(publicacionId).orElseThrow();
        publicacion.setEstado(Publicacion.Estado.ELIMINADA);
        publicaciones.flush();
        mvc.perform(put(ruta()).session(autor)).andExpect(status().isNotFound());
        mvc.perform(delete(ruta()).session(autor)).andExpect(status().isNotFound());
        mvc.perform(put("/api/foro/publicaciones/0/me-gusta").session(autor))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/foro/publicaciones/{id}/me-gusta", Long.MAX_VALUE)
                        .session(autor))
                .andExpect(status().isNotFound());
    }

    @Test
    void perfilListaSoloLasPublicacionesVisiblesDelAutor() throws Exception {
        Usuario autor = usuarios.findById("autor").orElseThrow();
        Publicacion oculta = new Publicacion(autor, "Oculta", "Contenido",
                Publicacion.Tipo.DUDA, List.of());
        oculta.setEstado(Publicacion.Estado.ELIMINADA);
        publicaciones.saveAndFlush(oculta);
        publicaciones.saveAndFlush(new Publicacion(
                usuarios.findById("colaborador").orElseThrow(), "De otro autor",
                "Contenido", Publicacion.Tipo.EXPERIENCIA, List.of()));
        MockHttpSession colaborador = sesion("colaborador", Rol.COLABORADOR);
        mvc.perform(get("/api/perfil/publico/autor").session(colaborador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value("autor"))
                .andExpect(jsonPath("$.correo").doesNotExist())
                .andExpect(jsonPath("$.contrasenia").doesNotExist());
        mvc.perform(get("/api/foro/publicaciones").param("autor", "autor")
                        .session(colaborador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(publicacionId));
    }

    private String ruta() {
        return "/api/foro/publicaciones/" + publicacionId + "/me-gusta";
    }

    private Usuario guardarUsuario(String id, Rol rol, EstadoVerificacion estado, String hash) {
        return usuarios.save(new Usuario(id, id, "Nombre " + id,
                id + "@rumbo.test", hash, rol, estado));
    }

    private MockHttpSession sesion(String id, Rol rol) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(GestorPermisos.ATRIBUTO_SESION,
                new SesionUsuario(id, "Nombre " + id, rol, Instant.now().toString()));
        return sesion;
    }
}
