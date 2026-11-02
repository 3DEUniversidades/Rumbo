package com.rumbo.controlador;

import com.rumbo.modelo.Carrera;
import com.rumbo.modelo.EstadoVerificacion;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.CarreraRepository;
import com.rumbo.persistencia.UsuarioRepository;
import com.rumbo.servicio.GestorPermisos;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
class ComparadorCarrerasIntegracionTest {

    @Autowired MockMvc mvc;
    @Autowired CarreraRepository carreras;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder codificador;
    @Autowired EntityManager entidades;

    private Long primera;
    private Long segunda;
    private Long tercera;

    @BeforeEach
    void preparar() {
        usuarios.save(new Usuario("comparador-e5", "comparador_e5", "Prueba comparador",
                "comparador@rumbo.test", codificador.encode("clave-de-prueba"),
                Rol.ENCARGADO_DE_CARRERAS, EstadoVerificacion.VERIFICADO));
        Carrera a = new Carrera("Carrera A", 5, new BigDecimal("38826"));
        a.setCargaCreditos(294);
        a.setCostoPeriodo("Primer semestre");
        a.setCostoAnio(2026);
        Carrera b = new Carrera("Carrera B", 2, new BigDecimal("20000"));
        b.setDuracion(new BigDecimal("2.5"));
        b.setCostoPeriodo("Primer semestre");
        b.setCostoAnio(2026);
        Carrera c = new Carrera("Carrera C", null, null);
        List<Carrera> guardadas = carreras.saveAllAndFlush(List.of(a, b, c));
        primera = guardadas.get(0).getId();
        segunda = guardadas.get(1).getId();
        tercera = guardadas.get(2).getId();
        entidades.clear();
    }

    @Test
    void conservaDecimalesYDatosDesconocidosEnElCatalogo() throws Exception {
        mvc.perform(get("/api/carreras/{id}", segunda).session(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracion").value(2.5));
        mvc.perform(get("/api/carreras/{id}", tercera).session(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracion").isEmpty())
                .andExpect(jsonPath("$.costo").isEmpty());
    }

    @Test
    void comparaDosConCreditosYDatosFaltantesSinInventarValores() throws Exception {
        mvc.perform(get("/api/comparador").session(sesion())
                        .param("ids", primera + "," + segunda))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filas[?(@.campo == 'cargaCreditos')].valores[0]")
                        .value("294 créditos"))
                .andExpect(jsonPath("$.filas[?(@.campo == 'cargaCreditos')].valores[1]")
                        .value("No disponible"))
                .andExpect(jsonPath("$.filas[?(@.campo == 'cargaCreditos')].diferencia")
                        .value("Sin datos suficientes para comparar."))
                .andExpect(jsonPath("$.filas[?(@.campo == 'duracion')].valores[1]")
                        .value("2.5 años"));
    }

    @Test
    void comparaMasDeDosConservandoElOrdenYLosNulos() throws Exception {
        mvc.perform(get("/api/comparador").session(sesion())
                        .param("ids", tercera + "," + segunda + "," + primera))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carreras.length()").value(3))
                .andExpect(jsonPath("$.carreras[0].id").value(tercera))
                .andExpect(jsonPath("$.carreras[2].id").value(primera))
                .andExpect(jsonPath("$.filas[?(@.campo == 'duracion')].valores[0]")
                        .value("No disponible"))
                .andExpect(jsonPath("$.filas[?(@.campo == 'costo')].valores[0]")
                        .value("No disponible"));
    }

    @Test
    void permiteEditarDuracionDecimalYDejarDatosDesconocidos() throws Exception {
        mvc.perform(put("/api/carreras/{id}", primera).session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracion\":1.5,\"costo\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracion").value(1.5))
                .andExpect(jsonPath("$.costo").isEmpty());
        entidades.flush();
        entidades.clear();
        assertEquals(0, new BigDecimal("1.5").compareTo(
                carreras.findById(primera).orElseThrow().getDuracion()));
        mvc.perform(put("/api/carreras/{id}", primera).session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracion\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracion").isEmpty());
    }

    @Test
    void rechazaDuracionesFueraDeRangoOQueRequieranRedondeo() throws Exception {
        for (String valor : List.of("0", "16", "2.55", "\"2.5\"")) {
            mvc.perform(put("/api/carreras/{id}", primera).session(sesion())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"duracion\":" + valor + "}"))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, new BigDecimal("5").compareTo(
                carreras.findById(primera).orElseThrow().getDuracion()));
    }

    @Test
    void exigeSesionYNoPermiteRepetirCarreras() throws Exception {
        mvc.perform(get("/api/comparador").param("ids", primera + "," + segunda))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/comparador").session(sesion())
                        .param("ids", primera + "," + primera))
                .andExpect(status().isBadRequest());
    }

    private MockHttpSession sesion() {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(GestorPermisos.ATRIBUTO_SESION,
                new SesionUsuario("comparador-e5", "Prueba comparador",
                        Rol.ENCARGADO_DE_CARRERAS, "2026-11-02T00:00:00Z"));
        return sesion;
    }
}
