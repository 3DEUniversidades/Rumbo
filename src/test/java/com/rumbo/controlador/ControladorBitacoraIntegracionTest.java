package com.rumbo.controlador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumbo.dto.CrearEntradaBitacoraSolicitud;
import com.rumbo.modelo.Actividad;
import com.rumbo.modelo.EstadoVerificacion;
import com.rumbo.modelo.RegistroActividadEstudiante;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.ActividadRepository;
import com.rumbo.persistencia.RegistroActividadEstudianteRepository;
import com.rumbo.persistencia.UsuarioRepository;
import com.rumbo.servicio.GestorPermisos;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ControladorBitacoraIntegracionTest {

    private static final String HASH_PRUEBA =
            "$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji";

    private static final String FECHA_BITACORA =
            "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ActividadRepository actividades;

    @Autowired
    private RegistroActividadEstudianteRepository registros;

    private Usuario estudianteUno;
    private Usuario estudianteDos;
    private Actividad actividadConResultados;
    private Actividad actividadSinResultados;

    @BeforeEach
    void prepararDatos() {
        estudianteUno = usuarios.save(new Usuario(
                "est-bit-01",
                "bit_uno",
                "Estudiante Uno",
                "bit-uno@example.com",
                HASH_PRUEBA,
                Rol.ESTUDIANTE,
                EstadoVerificacion.NO_VERIFICADO
        ));

        estudianteDos = usuarios.save(new Usuario(
                "est-bit-02",
                "bit_dos",
                "Estudiante Dos",
                "bit-dos@example.com",
                HASH_PRUEBA,
                Rol.ESTUDIANTE,
                EstadoVerificacion.NO_VERIFICADO
        ));

        actividadConResultados = actividades.save(new Actividad(
                "act-bit-01",
                "2026-10-20",
                "Actividad con resultados",
                "Contexto",
                "Instrucciones",
                "Acciones",
                "Método",
                "Herramientas",
                "Conclusiones",
                "Exploración"
        ));

        actividadSinResultados = actividades.save(new Actividad(
                "act-bit-02",
                "2026-10-21",
                "Actividad sin resultados",
                "Contexto",
                "Instrucciones",
                "Acciones",
                "Método",
                "Herramientas",
                "Conclusiones",
                "Exploración"
        ));

        RegistroActividadEstudiante registro =
                new RegistroActividadEstudiante(
                        estudianteUno,
                        actividadConResultados
                );
        registro.setResultadosRegistrados(
                "Me interesa estudiar Ingeniería en Computación"
        );
        registros.save(registro);
    }

    @Test
    void estudianteCreaListaEditaYEliminaSuPropiaEntrada() throws Exception {
        String idEntrada = crearEntrada("Reflexión inicial");

        mockMvc.perform(get("/api/bitacora").session(sesion(estudianteUno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idEntrada").value(idEntrada))
                .andExpect(jsonPath("$[0].contenido")
                        .value("Reflexión inicial"))
                .andExpect(jsonPath("$[0].fechaCreacion")
                        .value(matchesPattern(FECHA_BITACORA)))
                .andExpect(jsonPath("$[0].fechaEdicion").value(nullValue()));

        mockMvc.perform(put("/api/bitacora/{id}", idEntrada)
                        .session(sesion(estudianteUno))
                        .contentType(APPLICATION_JSON)
                        .content("{\"contenido\":\"Reflexión editada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido")
                        .value("Reflexión editada"))
                .andExpect(jsonPath("$.fechaEdicion")
                        .value(matchesPattern(FECHA_BITACORA)));

        mockMvc.perform(delete("/api/bitacora/{id}", idEntrada)
                        .session(sesion(estudianteUno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje")
                        .value("Entrada eliminada"));

        mockMvc.perform(get("/api/bitacora").session(sesion(estudianteUno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void unSegundoEstudianteNoPuedeVerNiModificarEntradasAjenas()
            throws Exception {
        String idEntrada = crearEntrada("Entrada privada");

        mockMvc.perform(get("/api/bitacora").session(sesion(estudianteDos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(put("/api/bitacora/{id}", idEntrada)
                        .session(sesion(estudianteDos))
                        .contentType(APPLICATION_JSON)
                        .content("{\"contenido\":\"Intento ajeno\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Entrada de bitácora no encontrada"));

        mockMvc.perform(delete("/api/bitacora/{id}", idEntrada)
                        .session(sesion(estudianteDos)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Entrada de bitácora no encontrada"));

        mockMvc.perform(get("/api/bitacora").session(sesion(estudianteUno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].contenido").value("Entrada privada"));
    }

    @ParameterizedTest
    @EnumSource(
            value = Rol.class,
            names = {
                    "COLABORADOR",
                    "SUPERVISOR_DE_USUARIOS",
                    "SUPERVISOR_DE_MENSAJES",
                    "ENCARGADO_DE_CARRERAS",
                    "ADMIN"
            }
    )
    void rolesDistintosDeEstudianteReciben403(Rol rol) throws Exception {
        MockHttpSession sesion = sesion(estudianteUno, rol);

        mockMvc.perform(get("/api/bitacora").session(sesion))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/bitacora")
                        .session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content("{\"contenido\":\"No permitido\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinSesionDevuelve401() throws Exception {
        mockMvc.perform(get("/api/bitacora"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void contenidoVacioDevuelve400SinCrearEntrada() throws Exception {
        mockMvc.perform(post("/api/bitacora")
                        .session(sesion(estudianteUno))
                        .contentType(APPLICATION_JSON)
                        .content("{\"contenido\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.contenido")
                        .value("El contenido es obligatorio"));

        mockMvc.perform(get("/api/bitacora").session(sesion(estudianteUno)))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void creaEntradaDesdeResultadosRegistradosYRechazaSinResultados()
            throws Exception {
        mockMvc.perform(post(
                        "/api/bitacora/actividades/{id}/resultados",
                        actividadConResultados.getIdActividad()
                ).session(sesion(estudianteUno)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contenido").value(
                        "Me interesa estudiar Ingeniería en Computación"
                ))
                .andExpect(jsonPath("$.idActividad").value("act-bit-01"))
                .andExpect(jsonPath("$.descripcionActividad")
                        .value("Actividad con resultados"));

        mockMvc.perform(post(
                        "/api/bitacora/actividades/{id}/resultados",
                        actividadSinResultados.getIdActividad()
                ).session(sesion(estudianteUno)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.resultados").value(
                        "La actividad no tiene resultados registrados"
                ));
    }

    @Test
    void relacionaYDesrelacionaUnaActividadExistente() throws Exception {
        String idEntrada = crearEntrada("Entrada relacionable");

        mockMvc.perform(put("/api/bitacora/{id}/actividad", idEntrada)
                        .session(sesion(estudianteUno))
                        .contentType(APPLICATION_JSON)
                        .content("{\"idActividad\":\"act-bit-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idActividad").value("act-bit-01"))
                .andExpect(jsonPath("$.descripcionActividad")
                        .value("Actividad con resultados"));

        mockMvc.perform(put("/api/bitacora/{id}/actividad", idEntrada)
                        .session(sesion(estudianteUno))
                        .contentType(APPLICATION_JSON)
                        .content("{\"idActividad\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idActividad").value(nullValue()))
                .andExpect(jsonPath("$.descripcionActividad")
                        .value(nullValue()));
    }

    @Test
    void relacionarUnaActividadInexistenteDevuelve400() throws Exception {
        String idEntrada = crearEntrada("Entrada sin actividad válida");

        mockMvc.perform(put("/api/bitacora/{id}/actividad", idEntrada)
                        .session(sesion(estudianteUno))
                        .contentType(APPLICATION_JSON)
                        .content("{\"idActividad\":\"no-existe\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.idActividad")
                        .value("La actividad no existe"));
    }

    @Test
    void listaDeLaMasRecienteALaMasAntigua() throws Exception {
        crearEntrada("Primera entrada");
        Thread.sleep(25);
        crearEntrada("Segunda entrada");

        mockMvc.perform(get("/api/bitacora").session(sesion(estudianteUno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].contenido")
                        .value("Segunda entrada"))
                .andExpect(jsonPath("$[1].contenido")
                        .value("Primera entrada"));
    }

    private String crearEntrada(String contenido) throws Exception {
        String cuerpo = json.writeValueAsString(
                new CrearEntradaBitacoraSolicitud(contenido, null)
        );

        String respuesta = mockMvc.perform(post("/api/bitacora")
                        .session(sesion(estudianteUno))
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idEntrada").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return json.readTree(respuesta).get("idEntrada").asText();
    }

    private MockHttpSession sesion(Usuario usuario) {
        return sesion(usuario, usuario.getRol());
    }

    private MockHttpSession sesion(Usuario usuario, Rol rol) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(
                GestorPermisos.ATRIBUTO_SESION,
                new SesionUsuario(
                        usuario.getIdUsuario(),
                        usuario.getNombreCompleto(),
                        rol,
                        "2026-10-28T00:00:00Z"
                )
        );
        return sesion;
    }
}
