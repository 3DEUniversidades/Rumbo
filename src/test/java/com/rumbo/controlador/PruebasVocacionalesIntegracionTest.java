package com.rumbo.controlador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumbo.dto.ResultadoPruebaSolicitud;
import com.rumbo.dto.CrearEntradaBitacoraSolicitud;
import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import com.rumbo.servicio.*;
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
import java.time.LocalDate;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PruebasVocacionalesIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired ActividadRepository actividades;
    @Autowired PruebaVocacionalRepository pruebas;
    @Autowired ResultadoPruebaRepository resultados;
    @Autowired RegistroActividadEstudianteRepository avances;
    @Autowired PruebasVocacionalesService servicio;
    @Autowired EntityManager em;

    @BeforeEach
    void preparar() {
        String hash = "$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji";
        usuarios.save(new Usuario("est-t11-a", "est_t11_a", "Estudiante A", "a-t11@example.com", hash, Rol.ESTUDIANTE, EstadoVerificacion.NO_VERIFICADO));
        usuarios.save(new Usuario("est-t11-b", "est_t11_b", "Estudiante B", "b-t11@example.com", hash, Rol.ESTUDIANTE, EstadoVerificacion.NO_VERIFICADO));
        Actividad actividad = actividades.save(new Actividad("act-t11-test", "2026-10-20", "Prueba externa", "Contexto", "Instrucciones", "Acciones", "Metodo", "Sitio externo", "Resultado", "Intereses"));
        pruebas.save(new PruebaVocacional("prueba-t11-test", actividad, "Perfil externo", TipoPruebaVocacional.INTERESES,
                "Proveedor", "Proposito", "Contexto", "Instrucciones", "Metodo", "https://example.org/prueba",
                "https://example.org/recursos", "Orientacion", "Condiciones"));
        em.flush(); em.clear();
    }

    private MockHttpSession sesion(String id, Rol rol) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(GestorPermisos.ATRIBUTO_SESION, new SesionUsuario(id, "Estudiante", rol, "2026-10-26T00:00:00Z"));
        return sesion;
    }
    private ResultadoPruebaSolicitud datos(String resultado) {
        return new ResultadoPruebaSolicitud(LocalDate.now(ZoneId.of("America/Guatemala")), resultado, "Quiero explorar opciones");
    }

    @Test
    void estudianteNoVerificadoPuedeConsultarCatalogoYActividad() throws Exception {
        mvc.perform(get("/api/pruebas-vocacionales").session(sesion("est-t11-a", Rol.ESTUDIANTE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].idPrueba").value("prueba-t11-test"))
                .andExpect(jsonPath("$[0].miResultado").isEmpty());
        mvc.perform(get("/api/pruebas-vocacionales/por-actividad/act-t11-test").session(sesion("est-t11-a", Rol.ESTUDIANTE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void guardarCorregirYRecuperarNoDuplicaNiCompletaActividad() throws Exception {
        MockHttpSession sesion = sesion("est-t11-a", Rol.ESTUDIANTE);
        mvc.perform(put("/api/pruebas-vocacionales/prueba-t11-test/mi-resultado").session(sesion)
                .contentType(APPLICATION_JSON).content(json.writeValueAsString(datos("  Area social  "))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("Area social"));
        em.flush(); em.clear();
        String id = resultados.findByUsuario_IdUsuarioAndPrueba_IdPrueba("est-t11-a", "prueba-t11-test").orElseThrow().getIdResultado();
        mvc.perform(put("/api/pruebas-vocacionales/prueba-t11-test/mi-resultado").session(sesion)
                .contentType(APPLICATION_JSON).content(json.writeValueAsString(datos("Area investigativa"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.idResultado").value(id));
        em.flush(); em.clear();
        assertEquals(1, resultados.count());
        assertEquals(0, avances.count());
        mvc.perform(get("/api/pruebas-vocacionales/mis-resultados").session(sesion))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].resultado").value("Area investigativa"));
    }

    @Test
    void resultadosSonPrivadosInclusoSiSeEnviaOtroIdEnElJson() throws Exception {
        String cuerpo = json.writeValueAsString(datos("Solo estudiante A"));
        cuerpo = cuerpo.substring(0, cuerpo.length() - 1) + ",\"idUsuario\":\"est-t11-b\"}";
        mvc.perform(put("/api/pruebas-vocacionales/prueba-t11-test/mi-resultado").session(sesion("est-t11-a", Rol.ESTUDIANTE))
                .contentType(APPLICATION_JSON).content(cuerpo)).andExpect(status().isOk());
        em.flush(); em.clear();
        assertTrue(resultados.findByUsuario_IdUsuarioAndPrueba_IdPrueba("est-t11-a", "prueba-t11-test").isPresent());
        assertTrue(resultados.findByUsuario_IdUsuarioAndPrueba_IdPrueba("est-t11-b", "prueba-t11-test").isEmpty());
        mvc.perform(get("/api/pruebas-vocacionales/mis-resultados").session(sesion("est-t11-b", Rol.ESTUDIANTE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/pruebas-vocacionales/prueba-t11-test").session(sesion("est-t11-b", Rol.ESTUDIANTE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.miResultado").isEmpty());
    }

    @Test
    void copiarABitacoraRelacionaActividadYConservaPrivacidadSinCambiarNotas() throws Exception {
        Usuario usuario = usuarios.findById("est-t11-a").orElseThrow();
        Actividad actividad = actividades.findById("act-t11-test").orElseThrow();
        RegistroActividadEstudiante registro = new RegistroActividadEstudiante(usuario, actividad);
        registro.setResultadosRegistrados("Notas previas de T-09");
        avances.saveAndFlush(registro);
        var resultado = servicio.guardarResultado("est-t11-a", "prueba-t11-test", datos("Area social"));
        String contenido = resultado.nombrePrueba() + "\n\n" + resultado.resultado();
        String cuerpo = mvc.perform(post("/api/bitacora").session(sesion("est-t11-a", Rol.ESTUDIANTE))
                .contentType(APPLICATION_JSON).content(json.writeValueAsString(
                        new CrearEntradaBitacoraSolicitud(contenido, resultado.idActividad()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idActividad").value("act-t11-test"))
                .andExpect(jsonPath("$.contenido").value(contenido))
                .andReturn().getResponse().getContentAsString();
        String idEntrada = json.readTree(cuerpo).get("idEntrada").asText();
        em.flush(); em.clear();
        assertEquals("Notas previas de T-09", avances
                .findByUsuario_IdUsuarioAndActividad_IdActividad("est-t11-a", "act-t11-test")
                .orElseThrow().getResultadosRegistrados());
        servicio.guardarResultado("est-t11-a", "prueba-t11-test", datos("Area investigativa"));
        mvc.perform(get("/api/bitacora").session(sesion("est-t11-a", Rol.ESTUDIANTE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].contenido").value(contenido));
        mvc.perform(get("/api/bitacora").session(sesion("est-t11-b", Rol.ESTUDIANTE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/api/bitacora/" + idEntrada).session(sesion("est-t11-b", Rol.ESTUDIANTE))
                .contentType(APPLICATION_JSON).content("{\"contenido\":\"Intento ajeno\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rechazaResultadosVaciosLargosYFechaFutura() throws Exception {
        for (ResultadoPruebaSolicitud solicitud : new ResultadoPruebaSolicitud[]{datos("   "), datos("x".repeat(6001)),
                new ResultadoPruebaSolicitud(LocalDate.now(ZoneId.of("America/Guatemala")).plusDays(1), "Texto", ""),
                new ResultadoPruebaSolicitud(null, "Texto", ""),
                new ResultadoPruebaSolicitud(LocalDate.now(ZoneId.of("America/Guatemala")), "Texto", "x".repeat(4001))}) {
            mvc.perform(put("/api/pruebas-vocacionales/prueba-t11-test/mi-resultado").session(sesion("est-t11-a", Rol.ESTUDIANTE))
                    .contentType(APPLICATION_JSON).content(json.writeValueAsString(solicitud))).andExpect(status().isBadRequest());
        }
        assertEquals(0, resultados.count());
    }

    @Test
    void actividadRetiradaOcultaPruebaPeroConservaResultadoPrivado() throws Exception {
        servicio.guardarResultado("est-t11-a", "prueba-t11-test", datos("Mi historial"));
        Actividad actividad = actividades.findById("act-t11-test").orElseThrow();
        actividad.retirarDeLineaDeTiempo(); actividades.save(actividad); em.flush(); em.clear();
        mvc.perform(get("/api/pruebas-vocacionales").session(sesion("est-t11-a", Rol.ESTUDIANTE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/api/pruebas-vocacionales/prueba-t11-test/mi-resultado").session(sesion("est-t11-a", Rol.ESTUDIANTE))
                .contentType(APPLICATION_JSON).content(json.writeValueAsString(datos("Cambio")))).andExpect(status().isNotFound());
        mvc.perform(get("/api/pruebas-vocacionales/mis-resultados").session(sesion("est-t11-a", Rol.ESTUDIANTE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].resultado").value("Mi historial"))
                .andExpect(jsonPath("$[0].pruebaDisponible").value(false));
    }

    @Test
    void noExistePruebaDevuelve404() throws Exception {
        mvc.perform(get("/api/pruebas-vocacionales/no-existe").session(sesion("est-t11-a", Rol.ESTUDIANTE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void sinSesionNoPermiteLeerNiEscribir() throws Exception {
        mvc.perform(get("/api/pruebas-vocacionales")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/pruebas-vocacionales/mis-resultados")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/pruebas-vocacionales/prueba-t11-test/mi-resultado")
                .contentType(APPLICATION_JSON).content(json.writeValueAsString(datos("Texto")))).andExpect(status().isUnauthorized());
        mvc.perform(get("/test-vocacional.html")).andExpect(status().is3xxRedirection());
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = "ESTUDIANTE", mode = EnumSource.Mode.EXCLUDE)
    void otrosRolesNoPuedenConsultarNiGuardarResultados(Rol rol) throws Exception {
        MockHttpSession sesion = sesion("est-t11-a", rol);
        mvc.perform(get("/api/pruebas-vocacionales").session(sesion)).andExpect(status().isForbidden());
        mvc.perform(get("/api/pruebas-vocacionales/mis-resultados").session(sesion)).andExpect(status().isForbidden());
        mvc.perform(put("/api/pruebas-vocacionales/prueba-t11-test/mi-resultado").session(sesion)
                .contentType(APPLICATION_JSON).content(json.writeValueAsString(datos("Texto")))).andExpect(status().isForbidden());
        mvc.perform(get("/test-vocacional.html").session(sesion)).andExpect(status().isForbidden());
    }

    @Test
    void precargaNoDuplicaNiReactivaActividadesRetiradas() {
        InicializadorPruebasVocacionales inicializador = new InicializadorPruebasVocacionales(actividades, pruebas);
        inicializador.run(null); em.flush();
        Actividad actividad = actividades.findById("act-0001").orElseThrow();
        actividad.retirarDeLineaDeTiempo(); actividades.save(actividad); em.flush(); em.clear();
        inicializador.run(null); em.flush(); em.clear();
        assertEquals(3, pruebas.count());
        assertEquals(3, actividades.count());
        assertTrue(actividades.findById("act-0001").orElseThrow().isEliminada());
    }
}
