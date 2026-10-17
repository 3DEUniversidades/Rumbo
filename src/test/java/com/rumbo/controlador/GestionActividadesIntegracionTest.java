package com.rumbo.controlador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumbo.dto.ActividadSolicitud;
import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import com.rumbo.servicio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GestionActividadesIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ActividadService actividades;
    @Autowired ProgresoEstudianteService progreso;
    @Autowired UsuarioRepository usuarios;
    @Autowired ActividadRepository repositorio;
    @Autowired RegistroActividadEstudianteRepository registros;
    @Autowired EntityManager entityManager;

    @BeforeEach
    void preparar() {
        usuarios.saveAndFlush(new Usuario("est-t10", "est_t10", "Estudiante T10",
                "est-t10@example.com", "$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji",
                Rol.ESTUDIANTE, EstadoVerificacion.NO_VERIFICADO));
    }

    private ActividadSolicitud datos(String fecha, String titulo) {
        return new ActividadSolicitud(fecha, titulo, "Contexto", "Instrucciones",
                "Acciones", "Metodo", "Herramientas", "Conclusiones", "Tipo configurable");
    }

    private MockHttpSession sesion(Rol rol) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(GestorPermisos.ATRIBUTO_SESION,
                new SesionUsuario("est-t10", "Prueba T10", rol, "2026-10-17T00:00:00Z"));
        return sesion;
    }

    @Test
    void encargadoPuedeCrearActividadConIdGenerado() throws Exception {
        mvc.perform(post("/api/actividades").session(sesion(Rol.ENCARGADO_DE_CARRERAS))
                .contentType(APPLICATION_JSON).content(json.writeValueAsString(datos("2026-10-20", "Nueva cita"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.idActividad").isNotEmpty())
                .andExpect(jsonPath("$.descripcion").value("Nueva cita"));
    }

    @Test
    void editarConservaResultadosYEliminarOcultaSinBorrarHistorial() throws Exception {
        Actividad actividad = actividades.crearActividad(datos("2026-10-20", "Inicial"));
        String id = actividad.getIdActividad();
        progreso.marcarCompletada("est-t10", id);
        progreso.guardarResultados("est-t10", id, "Mis conclusiones");
        entityManager.flush();
        entityManager.clear();

        mvc.perform(put("/api/actividades/{id}", id).session(sesion(Rol.ENCARGADO_DE_CARRERAS))
                .contentType(APPLICATION_JSON).content(json.writeValueAsString(datos("2026-11-01", "Editada"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.descripcion").value("Editada"));
        entityManager.flush();
        entityManager.clear();
        assertEquals(EstadoActividad.COMPLETADA, progreso.consultarDetalleActividad("est-t10", id).estado());
        assertEquals("Mis conclusiones", progreso.consultarDetalleActividad("est-t10", id).resultadosRegistrados());

        mvc.perform(delete("/api/actividades/{id}", id).session(sesion(Rol.ENCARGADO_DE_CARRERAS)))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();

        assertTrue(repositorio.findById(id).orElseThrow().isEliminada());
        assertTrue(progreso.consultarLineaDeTiempo("est-t10").stream().noneMatch(a -> a.idActividad().equals(id)));
        assertEquals(0, progreso.consultarProgresoGeneral("est-t10").totalActividades());
        var historial = registros.findByUsuario_IdUsuarioAndActividad_IdActividad("est-t10", id).orElseThrow();
        assertEquals("Mis conclusiones", historial.getResultadosRegistrados());
        assertEquals(EstadoActividad.COMPLETADA, historial.getEstado());
        assertThrows(ExcepcionActividad.class, () -> progreso.marcarCompletada("est-t10", id));
        mvc.perform(get("/api/actividades/{id}", id).session(sesion(Rol.ENCARGADO_DE_CARRERAS)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = "ENCARGADO_DE_CARRERAS", mode = EnumSource.Mode.EXCLUDE)
    void otrosRolesNoPuedenCrearEditarNiEliminar(Rol rol) throws Exception {
        String cuerpo = json.writeValueAsString(datos("2026-10-20", "No autorizada"));
        mvc.perform(post("/api/actividades").session(sesion(rol)).contentType(APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/actividades/inexistente").session(sesion(rol)).contentType(APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/actividades/inexistente").session(sesion(rol)))
                .andExpect(status().isForbidden());
        assertEquals(0, repositorio.count());
    }

    @Test
    void sinSesionNoSePuedeCrearEditarNiEliminar() throws Exception {
        String cuerpo = json.writeValueAsString(datos("2026-10-20", "Sin sesion"));
        mvc.perform(post("/api/actividades").contentType(APPLICATION_JSON).content(cuerpo)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/actividades/id").contentType(APPLICATION_JSON).content(cuerpo)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/actividades/id")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-02-30", "20/10/2026", ""})
    void rechazaFechasInvalidas(String fecha) throws Exception {
        mvc.perform(post("/api/actividades").session(sesion(Rol.ENCARGADO_DE_CARRERAS))
                .contentType(APPLICATION_JSON).content(json.writeValueAsString(datos(fecha, "Prueba"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.fecha").exists());
        assertEquals(0, repositorio.count());
    }

    @Test
    void rechazaDescripcionVaciaYCamposDemasiadoLargos() throws Exception {
        for (String titulo : new String[]{"  ", "x".repeat(256)}) {
            mvc.perform(post("/api/actividades").session(sesion(Rol.ENCARGADO_DE_CARRERAS))
                    .contentType(APPLICATION_JSON).content(json.writeValueAsString(datos("2026-10-20", titulo))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.descripcion").exists());
        }
        assertEquals(0, repositorio.count());
    }
}
