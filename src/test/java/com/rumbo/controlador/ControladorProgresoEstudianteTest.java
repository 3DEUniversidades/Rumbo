package com.rumbo.controlador;

import com.rumbo.dto.ProgresoEstudianteRespuesta;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.servicio.ExcepcionActividad;
import com.rumbo.servicio.GestorPermisos;
import com.rumbo.servicio.ProgresoEstudianteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ControladorProgresoEstudianteTest {

    private MockMvc mockMvc;
    private ProgresoEstudianteService servicio;

    @BeforeEach
    void prepararControlador() {
        servicio = mock(ProgresoEstudianteService.class);

        ControladorProgresoEstudiante controlador =
                new ControladorProgresoEstudiante(
                        servicio,
                        new GestorPermisos()
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controlador)
                .setControllerAdvice(new ManejadorExcepciones())
                .build();
    }

    @Test
    void todasLasRutasRechazanPeticionesSinSesion() throws Exception {
        verificarRechazoDeRutas(null, 401);

        verifyNoInteractions(servicio);
    }

    @Test
    void todasLasRutasRechazanOtroRol() throws Exception {
        MockHttpSession sesion = crearSesion(Rol.COLABORADOR);

        verificarRechazoDeRutas(sesion, 403);

        verifyNoInteractions(servicio);
    }

    @Test
    void resumenUtilizaLaIdentidadDeLaSesion() throws Exception {
        when(servicio.consultarProgresoGeneral("est-01"))
                .thenReturn(new ProgresoEstudianteRespuesta(
                        4,
                        1,
                        25.0
                ));

        mockMvc.perform(
                        get("/api/mi-progreso/resumen")
                                .session(crearSesion(Rol.ESTUDIANTE))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActividades").value(4))
                .andExpect(jsonPath("$.actividadesCompletadas").value(1))
                .andExpect(jsonPath("$.porcentaje").value(25.0));

        verify(servicio).consultarProgresoGeneral("est-01");
    }

    @Test
    void guardaDecisionDelEstudianteAutenticado() throws Exception {
        mockMvc.perform(
                        put("/api/mi-progreso/actividades/act-01/decision")
                                .session(crearSesion(Rol.ESTUDIANTE))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "decidioRealizar": true,
                                          "razon": "Quiero explorar"
                                        }
                                        """)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(servicio).registrarDecision(
                "est-01",
                "act-01",
                true,
                "Quiero explorar"
        );
    }

    @Test
    void rechazaDecisionSinRespuesta() throws Exception {
        mockMvc.perform(
                        put("/api/mi-progreso/actividades/act-01/decision")
                                .session(crearSesion(Rol.ESTUDIANTE))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "razon": "Quiero explorar"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Debes indicar si realizarás la actividad"
                ));

        verifyNoInteractions(servicio);
    }

    @Test
    void actividadInexistenteDevuelve404() throws Exception {
        when(servicio.consultarDetalleActividad(
                "est-01",
                "inexistente"
        )).thenThrow(new ExcepcionActividad(
                "Actividad no encontrada: inexistente"
        ));

        mockMvc.perform(
                        get("/api/mi-progreso/actividades/inexistente")
                                .session(crearSesion(Rol.ESTUDIANTE))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(
                        "Actividad no encontrada: inexistente"
                ));
    }

    private MockHttpSession crearSesion(Rol rol) {
        MockHttpSession sesion = new MockHttpSession();

        SesionUsuario usuario = new SesionUsuario(
                "est-01",
                "Estudiante de prueba",
                rol,
                "2026-10-16T12:00:00Z"
        );

        sesion.setAttribute(
                GestorPermisos.ATRIBUTO_SESION,
                usuario
        );

        return sesion;
    }

    private void verificarRechazoDeRutas(
            MockHttpSession sesion,
            int codigoEsperado
    ) throws Exception {
        String[][] rutas = {
                {"GET", "/api/mi-progreso/actividades"},
                {"GET", "/api/mi-progreso/actividades/act-01"},
                {"GET", "/api/mi-progreso/resumen"},
                {"PUT", "/api/mi-progreso/actividades/act-01/decision"},
                {"PUT", "/api/mi-progreso/actividades/act-01/completada"},
                {"PUT", "/api/mi-progreso/actividades/act-01/resultados"},
                {"PUT", "/api/mi-progreso/actividades/act-01/no-realizada"}
        };

        for (String[] ruta : rutas) {
            MockHttpServletRequestBuilder peticion = request(
                    HttpMethod.valueOf(ruta[0]),
                    ruta[1]
            );

            if ("PUT".equals(ruta[0])) {
                peticion.contentType(APPLICATION_JSON)
                        .content("{}");
            }

            if (sesion != null) {
                peticion.session(sesion);
            }

            mockMvc.perform(peticion)
                    .andExpect(status().is(codigoEsperado));
        }
    }
}