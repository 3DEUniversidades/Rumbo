package com.rumbo.controlador;

import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import com.rumbo.servicio.GestorPermisos;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ControladorModeracionForoTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PublicacionRepository publicaciones;
    @Autowired RevisionMensajeRepository revisiones;
    @Autowired PasswordEncoder codificador;

    private Usuario autor;
    private Long publicacionId;

    @BeforeEach
    void preparar() {
        String hash = codificador.encode("clave-de-prueba");

        autor = usuarios.save(new Usuario(
                "autor-mod",
                "autor_mod",
                "Autor de prueba",
                "autor.mod@rumbo.test",
                hash,
                Rol.ESTUDIANTE,
                EstadoVerificacion.VERIFICADO
        ));

        usuarios.save(new Usuario(
                "supervisor-mod",
                "supervisor_mod",
                "Supervisor de prueba",
                "supervisor.mod@rumbo.test",
                hash,
                Rol.SUPERVISOR_DE_MENSAJES,
                EstadoVerificacion.VERIFICADO
        ));

        usuarios.save(new Usuario(
                "otro-supervisor-mod",
                "otro_supervisor_mod",
                "Segundo supervisor",
                "otro.supervisor.mod@rumbo.test",
                hash,
                Rol.SUPERVISOR_DE_MENSAJES,
                EstadoVerificacion.VERIFICADO
        ));

        publicacionId = guardarPublicacion("Primera publicación").getId();
    }

    @Test
    void validarMantieneLaPublicacionEnElForoYRegistraSupervisor() throws Exception {
        MockHttpSession supervisor = login("supervisor.mod@rumbo.test");

        mvc.perform(post(
                        "/api/moderacion/foro/publicaciones/{id}/validar",
                        publicacionId)
                        .session(supervisor)
                        .header("X-Usuario-Id", "otro-supervisor-mod"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision").value("VALIDAR"))
                .andExpect(jsonPath("$.supervisor.idUsuario")
                        .value("supervisor-mod"));

        assertEquals(
                Publicacion.Estado.VALIDADA,
                publicaciones.findById(publicacionId).orElseThrow().getEstado()
        );

        mvc.perform(get("/api/moderacion/foro/pendientes")
                        .session(supervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(get("/api/foro/publicaciones/{id}", publicacionId)
                        .session(login("autor.mod@rumbo.test")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/moderacion/foro/mis-revisiones")
                        .session(login("otro.supervisor.mod@rumbo.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void eliminarOcultaLaPublicacionSinBorrarlaFisicamente() throws Exception {
        mvc.perform(post(
                        "/api/moderacion/foro/publicaciones/{id}/eliminar",
                        publicacionId)
                        .session(login("supervisor.mod@rumbo.test")))
                .andExpect(status().isCreated());

        assertEquals(
                Publicacion.Estado.ELIMINADA,
                publicaciones.findById(publicacionId).orElseThrow().getEstado()
        );

        mvc.perform(get("/api/foro/publicaciones/{id}", publicacionId)
                        .session(login("autor.mod@rumbo.test")))
                .andExpect(status().isNotFound());

        assertEquals(1, revisiones.count());
    }

    @Test
    void noPermiteRegistrarUnaSegundaDecision() throws Exception {
        MockHttpSession supervisor = login("supervisor.mod@rumbo.test");

        mvc.perform(post(
                        "/api/moderacion/foro/publicaciones/{id}/validar",
                        publicacionId)
                        .session(supervisor))
                .andExpect(status().isCreated());

        mvc.perform(post(
                        "/api/moderacion/foro/publicaciones/{id}/eliminar",
                        publicacionId)
                        .session(login("otro.supervisor.mod@rumbo.test")))
                .andExpect(status().isConflict());

        assertEquals(1, revisiones.count());
    }

    @Test
    void pendientesSeOrdenanPorPublicacionMasReciente() throws Exception {
        Publicacion segunda = guardarPublicacion("Segunda publicación");

        mvc.perform(get("/api/moderacion/foro/pendientes")
                        .session(login("supervisor.mod@rumbo.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(segunda.getId()));
    }

    @Test
    void rechazaPeticionesSinSesion() throws Exception {
        mvc.perform(get("/api/moderacion/foro/pendientes")
                        .header("X-Usuario-Id", "supervisor-mod"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post(
                        "/api/moderacion/foro/publicaciones/{id}/validar",
                        publicacionId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaLosDemasRoles() throws Exception {
        for (Rol rol : Rol.values()) {
            if (rol == Rol.SUPERVISOR_DE_MENSAJES) continue;

            MockHttpSession sesion = new MockHttpSession();

            sesion.setAttribute(
                    GestorPermisos.ATRIBUTO_SESION,
                    new SesionUsuario(
                            "autor-mod",
                            "Otro rol",
                            rol,
                            Instant.now().toString()
                    )
            );

            mvc.perform(get("/api/moderacion/foro/pendientes")
                            .session(sesion))
                    .andExpect(status().isForbidden());

            mvc.perform(post(
                            "/api/moderacion/foro/publicaciones/{id}/eliminar",
                            publicacionId)
                            .session(sesion))
                    .andExpect(status().isForbidden());
        }

        assertEquals(0, revisiones.count());
    }

    private Publicacion guardarPublicacion(String titulo) {
        return publicaciones.saveAndFlush(new Publicacion(
                autor,
                titulo,
                "Contenido de prueba",
                Publicacion.Tipo.DUDA,
                List.of()
        ));
    }

    private MockHttpSession login(String correo) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo":"%s",
                                  "contrasenia":"clave-de-prueba"
                                }
                                """.formatted(correo)))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();
    }
}