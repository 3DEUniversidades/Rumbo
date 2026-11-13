package com.rumbo.controlador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumbo.modelo.Carrera;
import com.rumbo.modelo.EstadoVerificacion;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.CarreraRepository;
import com.rumbo.persistencia.RegistroActualizacionCarreraRepository;
import com.rumbo.persistencia.UsuarioRepository;
import com.rumbo.servicio.GestorPermisos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CarrerasMultiuniversidadIntegracionTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CarreraRepository carreras;
    @Autowired UsuarioRepository usuarios;
    @Autowired RegistroActualizacionCarreraRepository registros;
    private Carrera uvg;
    private Set<Long> idsAnteriores;

    @BeforeEach
    void preparar() {
        idsAnteriores = carreras.findAll().stream().map(Carrera::getId).collect(Collectors.toSet());
        for (Rol rol : Rol.values()) {
            usuarios.save(new Usuario("multi-" + rol, "multi_" + rol, "Prueba " + rol,
                    "multi-" + rol + "@example.com", "hash-ficticio", rol, EstadoVerificacion.VERIFICADO));
        }
        uvg = carreras.saveAndFlush(new Carrera("Ingeniería Industrial", 5, new BigDecimal("10000")));
    }

    @AfterEach
    void limpiar() {
        for (var carrera : carreras.findAll()) {
            if (idsAnteriores.contains(carrera.getId())) continue;
            registros.deleteAll(registros.findByCarreraIdOrderByFechaDesc(carrera.getId()));
            carreras.deleteById(carrera.getId());
        }
        for (Rol rol : Rol.values()) usuarios.deleteById("multi-" + rol);
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"ENCARGADO_DE_CARRERAS", "ADMIN"})
    void creaCarreraDeOtraUniversidadConAuditoriaYValoresDesconocidos(Rol rol) throws Exception {
        String cuerpo = mvc.perform(post("/api/carreras").session(sesion(rol))
                        .header("X-Usuario-Id", "identidad-ajena")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ingeniería Industrial\",\"universidad\":\" usac \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/carreras/")))
                .andExpect(jsonPath("$.universidad").value("USAC"))
                .andExpect(jsonPath("$.costo").isEmpty())
                .andExpect(jsonPath("$.duracion").isEmpty())
                .andExpect(jsonPath("$.salarioEstimado").isEmpty())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(cuerpo).get("id").asLong();
        var cambios = registros.findByCarreraIdOrderByFechaDesc(id);
        assertEquals(2, cambios.size());
        var universidad = cambios.stream().filter(r -> r.getCampo().equals("universidad")).findFirst().orElseThrow();
        assertNull(universidad.getValorAnterior());
        assertEquals("USAC", universidad.getValorNuevo());
        assertEquals("multi-" + rol, universidad.getResponsable());
        assertNotNull(universidad.getFecha());
        assertEquals("UVG", carreras.findById(uvg.getId()).orElseThrow().getUniversidad());
    }

    @Test
    void universidadCambiaConHistorialYNoSeRegistraUnCambioIdentico() throws Exception {
        mvc.perform(put("/api/carreras/{id}", uvg.getId()).session(sesion(Rol.ENCARGADO_DE_CARRERAS))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"universidad\":\"USAC\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.universidad").value("USAC"));
        var cambios = registros.findByCarreraIdOrderByFechaDesc(uvg.getId());
        assertEquals(1, cambios.size());
        assertEquals("UVG", cambios.get(0).getValorAnterior());
        assertEquals("USAC", cambios.get(0).getValorNuevo());
        mvc.perform(put("/api/carreras/{id}", uvg.getId()).session(sesion(Rol.ENCARGADO_DE_CARRERAS))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"universidad\":\"usac\"}"))
                .andExpect(status().isOk());
        assertEquals(1, registros.findByCarreraIdOrderByFechaDesc(uvg.getId()).size());
        mvc.perform(get("/api/carreras/mis-actualizaciones").session(sesion(Rol.ENCARGADO_DE_CARRERAS)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].campo").value("universidad"));
    }

    @Test
    void validaUniversidadObligatoriaSinGuardarCambiosParciales() throws Exception {
        for (String dato : List.of("null", "\" \"", "123", "\"" + "x".repeat(161) + "\"")) {
            mvc.perform(put("/api/carreras/{id}", uvg.getId()).session(sesion(Rol.ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Nombre que no debe guardarse\",\"universidad\":" + dato + "}"))
                    .andExpect(status().isBadRequest());
            assertEquals("Ingeniería Industrial", carreras.findById(uvg.getId()).orElseThrow().getNombre());
        }
        long cantidad = carreras.count();
        for (String dato : List.of("{}", "{\"nombre\":\"Carrera sin universidad\"}",
                "{\"nombre\":\"Carrera inválida\",\"universidad\":\"USAC\",\"costo\":-1}")) {
            mvc.perform(post("/api/carreras").session(sesion(Rol.ADMIN))
                            .contentType(MediaType.APPLICATION_JSON).content(dato))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(cantidad, carreras.count());
        assertEquals(0, registros.findByCarreraIdOrderByFechaDesc(uvg.getId()).size());
    }

    @Test
    void evitaDuplicadosEnUnaUniversidadSinImpedirCarrerasHomonimasEntreInstituciones() throws Exception {
        mvc.perform(post("/api/carreras").session(sesion(Rol.ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"ingeniería industrial\",\"universidad\":\"UVG\"}"))
                .andExpect(status().isConflict());
        var usac = new Carrera("Ingeniería Industrial", null, null);
        usac.setUniversidad("USAC");
        carreras.saveAndFlush(usac);
        mvc.perform(put("/api/carreras/{id}", uvg.getId()).session(sesion(Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"universidad\":\"USAC\"}"))
                .andExpect(status().isConflict());
        assertEquals("UVG", carreras.findById(uvg.getId()).orElseThrow().getUniversidad());
    }

    @Test
    void filtraCatalogoPorUniversidadYComparaUnaCarreraDeCadaInstitucion() throws Exception {
        var usac = new Carrera("Ingeniería Industrial", null, null);
        usac.setUniversidad("USAC");
        carreras.saveAndFlush(usac);
        mvc.perform(get("/api/carreras").param("universidad", "usac").session(sesion(Rol.ESTUDIANTE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].universidad").value("USAC"));
        mvc.perform(get("/api/carreras").param("universidad", "No existe").session(sesion(Rol.ESTUDIANTE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/comparador").param("ids", uvg.getId() + "," + usac.getId())
                        .session(sesion(Rol.ESTUDIANTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carreras[0].universidad").value("UVG"))
                .andExpect(jsonPath("$.carreras[1].universidad").value("USAC"))
                .andExpect(jsonPath("$.filas[?(@.campo == 'universidad')].valores[0]").value("UVG"))
                .andExpect(jsonPath("$.filas[?(@.campo == 'universidad')].valores[1]").value("USAC"))
                .andExpect(jsonPath("$.filas[?(@.campo == 'costo')].valores[1]").value("No disponible"));
    }

    @Test
    void soloEncargadoYAdminPuedenCrearYSiempreSeExigeSesion() throws Exception {
        mvc.perform(post("/api/carreras").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        for (Rol rol : Rol.values()) {
            if (rol == Rol.ADMIN || rol == Rol.ENCARGADO_DE_CARRERAS) continue;
            mvc.perform(post("/api/carreras").session(sesion(rol)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"No permitido\",\"universidad\":\"USAC\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    private MockHttpSession sesion(Rol rol) {
        var sesion = new MockHttpSession();
        sesion.setAttribute(GestorPermisos.ATRIBUTO_SESION,
                new SesionUsuario("multi-" + rol, "Prueba", rol, "2026-11-02T00:00:00Z"));
        return sesion;
    }
}
