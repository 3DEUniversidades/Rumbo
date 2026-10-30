package com.rumbo.controlador;

import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.servicio.GestorPermisos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GestionActividadesPantallaTest {
    @Autowired MockMvc mvc;

    private MockHttpSession sesion(Rol rol) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(GestorPermisos.ATRIBUTO_SESION,
                new SesionUsuario("pantalla-t10", "Prueba", rol, "2026-10-17T00:00:00Z"));
        return sesion;
    }

    @Test
    void sinSesionRedirigeAlLogin() throws Exception {
        mvc.perform(get("/gestion-actividades.html"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login.html"));
    }

    @Test
    void encargadoPuedeAbrirLaPantalla() throws Exception {
        mvc.perform(get("/gestion-actividades.html").session(sesion(Rol.ENCARGADO_DE_CARRERAS)))
                .andExpect(status().isOk());
    }

    @Test
    void adminPuedeAbrirLaPantalla() throws Exception {
        mvc.perform(get("/gestion-actividades.html").session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class,
            names = {"ENCARGADO_DE_CARRERAS", "ADMIN"},
            mode = EnumSource.Mode.EXCLUDE)
    void otrosRolesNoPuedenAbrirLaPantalla(Rol rol) throws Exception {
        mvc.perform(get("/gestion-actividades.html").session(sesion(rol)))
                .andExpect(status().isForbidden());
    }
}
