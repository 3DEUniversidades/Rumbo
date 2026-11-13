package com.rumbo.controlador;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortadaPublicaTest {

    @Autowired
    MockMvc mvc;

    @Test
    void laRaizMuestraLaPortadaSinSesion() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl("index.html"));
    }

    @Test
    void elIndiceMuestraLaPortadaSinSesion() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Empezar mi camino")));
    }

    @Test
    void losEstilosDeLaPortadaSonPublicos() throws Exception {
        mvc.perform(get("/css/landing.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"));
    }

    @Test
    void elLoginSigueSiendoPublico() throws Exception {
        mvc.perform(get("/login.html"))
                .andExpect(status().isOk());
    }
}
