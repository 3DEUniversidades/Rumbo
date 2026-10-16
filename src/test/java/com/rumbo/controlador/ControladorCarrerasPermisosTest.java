package com.rumbo.controlador;

import com.rumbo.modelo.Carrera;
import com.rumbo.modelo.EstadoVerificacion;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.CarreraRepository;
import com.rumbo.persistencia.RegistroActualizacionCarreraRepository;
import com.rumbo.persistencia.UsuarioRepository;
import com.rumbo.servicio.GestorPermisos;
import java.math.BigDecimal;
import java.time.Instant;
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
class ControladorCarrerasPermisosTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired CarreraRepository carreras;
    @Autowired RegistroActualizacionCarreraRepository registros;
    @Autowired PasswordEncoder codificador;

    private Long carreraId;

    @BeforeEach
    void preparar() {
        usuarios.save(new Usuario(
                "encargado-test",
                "encargado_test",
                "Encargado de prueba",
                "encargado.test@rumbo.test",
                codificador.encode("clave-de-prueba"),
                Rol.ENCARGADO_DE_CARRERAS,
                EstadoVerificacion.VERIFICADO
        ));

        Carrera carrera = new Carrera(
                "Civil de prueba", 5, new BigDecimal("32107.00"));

        carreraId = carreras.saveAndFlush(carrera).getId();
    }

    @Test
    void encargadoPuedeEditarYElResponsableSaleDeLaSesion() throws Exception {
        MockHttpSession sesion = iniciarSesionEncargado();

        mvc.perform(put("/api/carreras/{id}", carreraId)
                        .session(sesion)
                        .header("X-Usuario-Id", "identidad-falsificada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Civil actualizada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Civil actualizada"));

        var cambios = registros.findByCarreraIdOrderByFechaDesc(carreraId);

        assertEquals(1, cambios.size());
        assertEquals("encargado-test", cambios.get(0).getResponsable());

        mvc.perform(get("/api/carreras/{id}/historial", carreraId)
                        .session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].responsable")
                        .value("encargado-test"));

        mvc.perform(get("/api/carreras/historial").session(sesion))
                .andExpect(status().isOk());
    }

    @Test
    void headerSinSesionNoPermiteGestionar() throws Exception {
        mvc.perform(put("/api/carreras/{id}", carreraId)
                        .header("X-Usuario-Id", "encargado-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/carreras/historial")
                        .header("X-Usuario-Id", "encargado-test"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/carreras/{id}/historial", carreraId)
                        .header("X-Usuario-Id", "encargado-test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ningunOtroRolPuedeEditarNiConsultarHistorial() throws Exception {
        for (Rol rol : Rol.values()) {
            if (rol == Rol.ENCARGADO_DE_CARRERAS) continue;

            MockHttpSession sesion = sesionServidor(rol);

            mvc.perform(put("/api/carreras/{id}", carreraId)
                            .session(sesion)
                            .header("X-Usuario-Id", "encargado-test")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());

            mvc.perform(get("/api/carreras/historial").session(sesion))
                    .andExpect(status().isForbidden());

            mvc.perform(get("/api/carreras/{id}/historial", carreraId)
                            .session(sesion))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void catalogoExigeSesionPeroNoRolDeEncargado() throws Exception {
        mvc.perform(get("/api/carreras"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/carreras")
                        .session(sesionServidor(Rol.ESTUDIANTE)))
                .andExpect(status().isOk());
    }

    @Test
    void pantallaGestionSoloSeSirveAlEncargado() throws Exception {
        mvc.perform(get("/gestion-carreras.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html"));

        mvc.perform(get("/gestion-carreras.html")
                        .session(sesionServidor(Rol.ESTUDIANTE)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/gestion-carreras.html")
                        .session(iniciarSesionEncargado()))
                .andExpect(status().isOk());
    }

    private MockHttpSession iniciarSesionEncargado() throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo":"encargado.test@rumbo.test",
                                  "contrasenia":"clave-de-prueba"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rutaPantalla")
                        .value("/roles/encargado-carreras.html"))
                .andReturn()
                .getRequest()
                .getSession();
    }

    private MockHttpSession sesionServidor(Rol rol) {
        MockHttpSession sesion = new MockHttpSession();

        sesion.setAttribute(
                GestorPermisos.ATRIBUTO_SESION,
                new SesionUsuario(
                        "usuario-test",
                        "Usuario de prueba",
                        rol,
                        Instant.now().toString()
                )
        );

        return sesion;
    }
}