package com.rumbo.controlador;

import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import com.rumbo.servicio.GestorPermisos;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HistorialEncargadoIntegracionTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired CarreraRepository carreras;
    @Autowired RegistroActualizacionCarreraRepository registros;
    @Autowired PasswordEncoder codificador;
    @Autowired EntityManager entidades;

    private Long carreraId;
    private String hash;

    @BeforeEach
    void preparar() throws Exception {
        hash = codificador.encode("clave-de-prueba");
        usuarios.save(cuenta("t9-encargado-a", Rol.ENCARGADO_DE_CARRERAS));
        usuarios.save(cuenta("t9-encargado-b", Rol.ENCARGADO_DE_CARRERAS));
        usuarios.save(cuenta("t9-encargado-vacio", Rol.ENCARGADO_DE_CARRERAS));
        for (Rol rol : Rol.values()) {
            if (rol != Rol.ENCARGADO_DE_CARRERAS) {
                usuarios.save(cuenta("t9-actor-" + rol.name(), rol));
            }
        }
        carreraId = carreras.saveAndFlush(new Carrera(
                "Carrera de prueba", 5, new BigDecimal("1000"))).getId();
        actualizar("t9-encargado-a", Rol.ENCARGADO_DE_CARRERAS,
                "{\"descripcion\":\"Descripción del encargado A\",\"modalidad\":\"Presencial\"}");
        actualizar("t9-encargado-b", Rol.ENCARGADO_DE_CARRERAS,
                "{\"descripcion\":\"Descripción del encargado B\"}");
        actualizar("t9-actor-ADMIN", Rol.ADMIN, "{\"costo\":1500}");
        entidades.flush();
        entidades.clear();
    }

    @Test
    void incluyeLosCamposModificadosConSusValoresYFecha() throws Exception {
        mvc.perform(get("/api/carreras/mis-actualizaciones")
                        .session(encargado("t9-encargado-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].carreraId").value(carreraId))
                .andExpect(jsonPath("$[0].carreraNombre").value("Carrera de prueba"))
                .andExpect(jsonPath("$[0].campo").value("modalidad"))
                .andExpect(jsonPath("$[0].valorAnterior").isEmpty())
                .andExpect(jsonPath("$[0].valorNuevo").value("Presencial"))
                .andExpect(jsonPath("$[0].fecha").isNotEmpty())
                .andExpect(jsonPath("$[1].campo").value("descripcion"))
                .andExpect(jsonPath("$[1].valorNuevo").value("Descripción del encargado A"));
        assertEquals(4, registros.count());
    }

    @Test
    void cadaEncargadoConsultaSoloSusCambiosAunqueEnvieOtroResponsable() throws Exception {
        mvc.perform(get("/api/carreras/mis-actualizaciones")
                        .param("responsable", "t9-encargado-b")
                        .header("X-Usuario-Id", "t9-encargado-b")
                        .session(encargado("t9-encargado-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].responsable").value("t9-encargado-a"))
                .andExpect(jsonPath("$[1].responsable").value("t9-encargado-a"));
        mvc.perform(get("/api/carreras/mis-actualizaciones")
                        .session(encargado("t9-encargado-b")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].valorAnterior").value("Descripción del encargado A"))
                .andExpect(jsonPath("$[0].valorNuevo").value("Descripción del encargado B"));
    }

    @Test
    void ordenaPorFechaYDesempataPorIdentificadorMasReciente() throws Exception {
        Carrera carrera = carreras.findById(carreraId).orElseThrow();
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 1, 10, 0);
        RegistroActualizacionCarrera reciente = registros.save(new RegistroActualizacionCarrera(
                carrera, "descripcion", "A", "B", fecha.plusDays(1), "t9-encargado-vacio"));
        registros.save(new RegistroActualizacionCarrera(
                carrera, "costo", "100", "200", fecha, "t9-encargado-vacio"));
        RegistroActualizacionCarrera desempate = registros.saveAndFlush(new RegistroActualizacionCarrera(
                carrera, "costo", "200", "300", fecha, "t9-encargado-vacio"));
        mvc.perform(get("/api/carreras/mis-actualizaciones")
                        .session(encargado("t9-encargado-vacio")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reciente.getId()))
                .andExpect(jsonPath("$[1].id").value(desempate.getId()));
    }

    @Test
    void encargadoSinCambiosRecibeUnHistorialVacio() throws Exception {
        mvc.perform(get("/api/carreras/mis-actualizaciones")
                        .session(encargado("t9-encargado-vacio")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = "ENCARGADO_DE_CARRERAS", mode = EnumSource.Mode.EXCLUDE)
    void otrosRolesNoConsultanElHistorialPersonalDelEncargado(Rol rol) throws Exception {
        mvc.perform(get("/api/carreras/mis-actualizaciones")
                        .session(sesion("t9-actor-" + rol.name(), rol)))
                .andExpect(status().isForbidden());
    }

    @Test
    void exigeSesionYRechazaCuentasEliminadasOInexistentes() throws Exception {
        mvc.perform(get("/api/carreras/mis-actualizaciones"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/carreras/mis-actualizaciones")
                        .session(encargado("t9-inexistente")))
                .andExpect(status().isUnauthorized());
        Usuario usuario = usuarios.findById("t9-encargado-a").orElseThrow();
        usuario.eliminarCuenta();
        usuarios.saveAndFlush(usuario);
        mvc.perform(get("/api/carreras/mis-actualizaciones")
                        .session(encargado("t9-encargado-a")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void compruebaElRolActualAunqueLaSesionConserveElRolAnterior() throws Exception {
        usuarios.saveAndFlush(cuenta("t9-encargado-a", Rol.COLABORADOR));
        mvc.perform(get("/api/carreras/mis-actualizaciones")
                        .session(encargado("t9-encargado-a")))
                .andExpect(status().isForbidden());
    }

    @Test
    void perfilIncluyeElHistorialYLaConsultaNoExponeCredenciales() throws Exception {
        mvc.perform(get("/perfil.html").session(encargado("t9-encargado-a")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("seccion-historial-encargado")))
                .andExpect(content().string(containsString("/js/vistaHistorialEncargado.js")));
        mvc.perform(get("/api/carreras/mis-actualizaciones")
                        .session(encargado("t9-encargado-a")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(hash))))
                .andExpect(jsonPath("$[0].correo").doesNotExist())
                .andExpect(jsonPath("$[0].contrasenia").doesNotExist());
    }

    private void actualizar(String id, Rol rol, String cambios) throws Exception {
        mvc.perform(put("/api/carreras/{id}", carreraId).session(sesion(id, rol))
                        .contentType(APPLICATION_JSON).content(cambios))
                .andExpect(status().isOk());
    }

    private Usuario cuenta(String id, Rol rol) {
        return new Usuario(id, id, "Prueba " + id, id + "@rumbo.test",
                hash, rol, EstadoVerificacion.VERIFICADO);
    }

    private MockHttpSession encargado(String id) {
        return sesion(id, Rol.ENCARGADO_DE_CARRERAS);
    }

    private MockHttpSession sesion(String id, Rol rol) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(GestorPermisos.ATRIBUTO_SESION,
                new SesionUsuario(id, "Prueba", rol, "2026-11-02T00:00:00Z"));
        return sesion;
    }
}
