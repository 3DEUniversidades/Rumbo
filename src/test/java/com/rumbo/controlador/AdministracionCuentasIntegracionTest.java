package com.rumbo.controlador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumbo.dto.EliminarCuentaSolicitud;
import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import com.rumbo.servicio.GestorPermisos;
import jakarta.persistence.EntityManager;
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
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdministracionCuentasIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired PerfilRepository perfiles;
    @Autowired RegistroAdministracionRepository registros;
    @Autowired PublicacionRepository publicaciones;
    @Autowired RespuestaPublicacionRepository respuestas;
    @Autowired EntradaBitacoraRepository entradas;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager em;
    private static final String CLAVE = "ClavePrueba123";
    private String id(Rol rol) { return "b2-" + rol.name(); }
    private String nombre(Rol rol) { return "b2_" + rol.name().toLowerCase(java.util.Locale.ROOT); }
    private MockHttpSession sesion(String id, Rol rol) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(GestorPermisos.ATRIBUTO_SESION, new SesionUsuario(id, "Prueba", rol, "2026-11-02T00:00:00Z"));
        return s;
    }
    private MockHttpSession admin() { return sesion(id(Rol.ADMIN), Rol.ADMIN); }
    private String cuerpo(String nombre, String motivo) throws Exception {
        return json.writeValueAsString(new EliminarCuentaSolicitud(nombre, motivo));
    }
    @BeforeEach
    void preparar() {
        String hash = encoder.encode(CLAVE);
        for (Rol rol : Rol.values()) {
            usuarios.save(new Usuario(id(rol), nombre(rol), "Prueba " + rol, nombre(rol) + "@example.com",
                    hash, rol, EstadoVerificacion.VERIFICADO));
        }
        perfiles.save(new Perfil(usuarios.findById(id(Rol.ESTUDIANTE)).orElseThrow()));
        em.flush(); em.clear();
    }
    @Test
    void eliminaCuentaYRegistraActorObjetivoMotivoYFecha() throws Exception {
        mvc.perform(delete("/api/admin/usuarios/" + id(Rol.ESTUDIANTE)).session(admin())
                .contentType(APPLICATION_JSON).content(cuerpo(nombre(Rol.ESTUDIANTE), "  Cuenta de demostracion  ")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accion").value("ELIMINAR_CUENTA"))
                .andExpect(jsonPath("$.idAdministrador").value(id(Rol.ADMIN)))
                .andExpect(jsonPath("$.idObjetivo").value(id(Rol.ESTUDIANTE)))
                .andExpect(jsonPath("$.motivo").value("Cuenta de demostracion"))
                .andExpect(jsonPath("$.fecha").isNotEmpty());
        em.flush(); em.clear();
        Usuario borrado = usuarios.findById(id(Rol.ESTUDIANTE)).orElseThrow();
        assertTrue(borrado.isEliminado()); assertNotNull(borrado.getFechaEliminacion());
        assertTrue(perfiles.findByUsuarioIdUsuario(id(Rol.ESTUDIANTE)).isPresent());
        assertEquals(1, registros.count());
        mvc.perform(get("/api/admin/usuarios").session(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalUsuarios").value(Rol.values().length - 1));
        mvc.perform(get("/api/admin/usuarios/" + id(Rol.ESTUDIANTE)).session(admin())).andExpect(status().isNotFound());
        mvc.perform(get("/api/perfil/publico/" + id(Rol.ESTUDIANTE)).session(admin())).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/registros").session(admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRegistros").value(1)).andExpect(jsonPath("$.registros[0].usuarioObjetivo").value(nombre(Rol.ESTUDIANTE)));
    }
    @Test
    void cuentaEliminadaNoIniciaSesionYSeRevocaSuSesionAnterior() throws Exception {
        var respuesta = mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("correo", nombre(Rol.ESTUDIANTE) + "@example.com", "contrasenia", CLAVE))))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession anterior = (MockHttpSession) respuesta.getRequest().getSession(false);
        mvc.perform(delete("/api/admin/usuarios/" + id(Rol.ESTUDIANTE)).session(admin())
                .contentType(APPLICATION_JSON).content(cuerpo(nombre(Rol.ESTUDIANTE), "Prueba de revocacion")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/sesion").session(anterior)).andExpect(status().isUnauthorized());
        assertTrue(anterior.isInvalid());
        mvc.perform(get("/api/bitacora").session(sesion(id(Rol.ESTUDIANTE), Rol.ESTUDIANTE)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("correo", nombre(Rol.ESTUDIANTE) + "@example.com", "contrasenia", CLAVE))))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void conservaBitacoraPublicacionYRespuestasDeOtrasCuentas() throws Exception {
        Usuario estudiante = usuarios.findById(id(Rol.ESTUDIANTE)).orElseThrow();
        Usuario colaborador = usuarios.findById(id(Rol.COLABORADOR)).orElseThrow();
        Publicacion p = publicaciones.saveAndFlush(new Publicacion(estudiante, "Duda de prueba",
                "Contenido de demostracion", Publicacion.Tipo.DUDA, java.util.List.of()));
        Long idPublicacion = p.getId();
        Long idRespuesta = respuestas.saveAndFlush(new RespuestaPublicacion(p, colaborador, "Respuesta de otra cuenta")).getId();
        String idEntrada = entradas.saveAndFlush(new EntradaBitacora(estudiante, "Entrada privada", null)).getIdEntrada();
        mvc.perform(delete("/api/admin/usuarios/" + id(Rol.ESTUDIANTE)).session(admin())
                .contentType(APPLICATION_JSON).content(cuerpo(nombre(Rol.ESTUDIANTE), "Cuenta de demostracion")))
                .andExpect(status().isOk());
        em.flush(); em.clear();
        assertEquals("Entrada privada", entradas.findById(idEntrada).orElseThrow().getContenido());
        assertEquals(id(Rol.ESTUDIANTE), publicaciones.findById(idPublicacion).orElseThrow().getAutor().getIdUsuario());
        assertTrue(respuestas.findById(idRespuesta).isPresent());
        assertFalse(usuarios.findById(id(Rol.COLABORADOR)).orElseThrow().isEliminado());
    }
    @ParameterizedTest
    @EnumSource(value = Rol.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void eliminaCuentasDeOtrosRolesConservandoRelaciones(Rol objetivo) throws Exception {
        mvc.perform(delete("/api/admin/usuarios/" + id(objetivo)).session(admin())
                .contentType(APPLICATION_JSON).content(cuerpo(nombre(objetivo), "Prueba por rol")))
                .andExpect(status().isOk());
        em.flush(); em.clear();
        assertTrue(usuarios.findById(id(objetivo)).orElseThrow().isEliminado());
        assertFalse(usuarios.findById(id(Rol.ADMIN)).orElseThrow().isEliminado());
    }
    @ParameterizedTest
    @EnumSource(value = Rol.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void otrosRolesNoEliminanNiConsultanRegistros(Rol actor) throws Exception {
        mvc.perform(delete("/api/admin/usuarios/" + id(Rol.COLABORADOR)).session(sesion(id(actor), actor))
                .contentType(APPLICATION_JSON).content(cuerpo(nombre(Rol.COLABORADOR), "No permitido")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/registros").session(sesion(id(actor), actor))).andExpect(status().isForbidden());
        assertEquals(0, registros.count());
        assertFalse(usuarios.findById(id(Rol.COLABORADOR)).orElseThrow().isEliminado());
    }
    @Test
    void noPermiteEliminarLaCuentaAdministradoraActual() throws Exception {
        mvc.perform(delete("/api/admin/usuarios/" + id(Rol.ADMIN)).session(admin())
                .contentType(APPLICATION_JSON).content(cuerpo(nombre(Rol.ADMIN), "Intento propio")))
                .andExpect(status().isConflict());
        assertFalse(usuarios.findById(id(Rol.ADMIN)).orElseThrow().isEliminado());
        assertEquals(0, registros.count());
    }
    @Test
    void puedeEliminarOtroAdministradorYQuedaElActorActivo() throws Exception {
        usuarios.saveAndFlush(new Usuario("b2-admin-otro", "b2_otro_admin", "Otro admin", "otro-admin@example.com",
                encoder.encode(CLAVE), Rol.ADMIN, EstadoVerificacion.VERIFICADO));
        mvc.perform(delete("/api/admin/usuarios/b2-admin-otro").session(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo("b2_otro_admin", "Cuenta de prueba adicional"))).andExpect(status().isOk());
        assertTrue(usuarios.findById("b2-admin-otro").orElseThrow().isEliminado());
        assertFalse(usuarios.findById(id(Rol.ADMIN)).orElseThrow().isEliminado());
    }
    @Test
    void repetirEliminacionNoDuplicaRegistro() throws Exception {
        String cuerpo = cuerpo(nombre(Rol.COLABORADOR), "Cuenta duplicada");
        mvc.perform(delete("/api/admin/usuarios/" + id(Rol.COLABORADOR)).session(admin())
                .contentType(APPLICATION_JSON).content(cuerpo)).andExpect(status().isOk());
        mvc.perform(delete("/api/admin/usuarios/" + id(Rol.COLABORADOR)).session(admin())
                .contentType(APPLICATION_JSON).content(cuerpo)).andExpect(status().isConflict());
        assertEquals(1, registros.count());
    }
    @Test
    void rechazaConfirmacionIncorrectaYMotivosInvalidos() throws Exception {
        for (EliminarCuentaSolicitud dato : new EliminarCuentaSolicitud[]{new EliminarCuentaSolicitud("incorrecto", "Motivo"),
                new EliminarCuentaSolicitud(nombre(Rol.ESTUDIANTE), "   "),
                new EliminarCuentaSolicitud(nombre(Rol.ESTUDIANTE), "x".repeat(1001)), new EliminarCuentaSolicitud(null, "Motivo")}) {
            mvc.perform(delete("/api/admin/usuarios/" + id(Rol.ESTUDIANTE)).session(admin())
                    .contentType(APPLICATION_JSON).content(json.writeValueAsString(dato))).andExpect(status().isBadRequest());
        }
        assertFalse(usuarios.findById(id(Rol.ESTUDIANTE)).orElseThrow().isEliminado()); assertEquals(0, registros.count());
    }
    @Test
    void sinSesionNoAccedeYCuentaInexistenteNoProduceRegistro() throws Exception {
        mvc.perform(delete("/api/admin/usuarios/" + id(Rol.ESTUDIANTE)).contentType(APPLICATION_JSON)
                .content(cuerpo(nombre(Rol.ESTUDIANTE), "Motivo"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/registros")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/admin/usuarios/inexistente").session(admin()).contentType(APPLICATION_JSON)
                .content(cuerpo("inexistente", "Motivo"))).andExpect(status().isNotFound());
        assertEquals(0, registros.count());
    }
    @Test
    void rechazaPaginacionInvalidaDelHistorial() throws Exception {
        for (String consulta : new String[]{"pagina=-1", "tamano=0", "tamano=51"}) {
            mvc.perform(get("/api/admin/registros?" + consulta).session(admin())).andExpect(status().isBadRequest());
        }
    }
}
