package com.rumbo.controlador;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConsultaUsuariosAdministracionIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PerfilRepository perfiles;
    @Autowired EntityManager em;
    private static final String HASH = "$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji";
    private String id(Rol rol) { return "f5admin-" + rol.name(); }
    private MockHttpSession sesion(Rol rol) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(GestorPermisos.ATRIBUTO_SESION, new SesionUsuario(id(rol), "Cuenta de prueba", rol, "2026-11-01T00:00:00Z"));
        return s;
    }
    @BeforeEach
    void preparar() {
        for (Rol rol : Rol.values()) {
            usuarios.save(new Usuario(id(rol), "f5_" + rol.name().toLowerCase(java.util.Locale.ROOT),
                    "Prueba " + rol.name(), rol.name().toLowerCase(java.util.Locale.ROOT) + "@example.com",
                    HASH, rol, EstadoVerificacion.NO_VERIFICADO));
        }
        Perfil p = new Perfil(usuarios.findById(id(Rol.ESTUDIANTE)).orElseThrow());
        p.setNivelEstudios("Bachillerato"); p.setDescripcionPersonal("Descripcion de prueba");
        p.setFotoPublicId("identificador-interno-no-publicar");
        perfiles.save(p); em.flush(); em.clear();
    }

    @Test
    void administradorListaTodasLasCuentasSinExponerDatosSensibles() throws Exception {
        mvc.perform(get("/api/admin/usuarios").session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalUsuarios").value(Rol.values().length))
                .andExpect(jsonPath("$.usuarios.length()").value(Rol.values().length))
                .andExpect(jsonPath("$.usuarios[0].contrasenia").doesNotExist())
                .andExpect(jsonPath("$.usuarios[0].correo").doesNotExist())
                .andExpect(content().string(not(containsString(HASH))))
                .andExpect(content().string(not(containsString("identificador-interno-no-publicar"))));
    }
    @ParameterizedTest
    @EnumSource(Rol.class)
    void administradorConsultaCuentasDeTodosLosRoles(Rol objetivo) throws Exception {
        mvc.perform(get("/api/admin/usuarios/" + id(objetivo)).session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value(objetivo.name()))
                .andExpect(jsonPath("$.idUsuario").value(id(objetivo)))
                .andExpect(jsonPath("$.contrasenia").doesNotExist())
                .andExpect(jsonPath("$.fotoPublicId").doesNotExist())
                .andExpect(jsonPath("$.bitacora").doesNotExist())
                .andExpect(jsonPath("$.resultados").doesNotExist());
    }
    @Test
    void consultaPerfilCompletoYCuentaSinPerfilAdicional() throws Exception {
        mvc.perform(get("/api/admin/usuarios/" + id(Rol.ESTUDIANTE)).session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfilDisponible").value(true))
                .andExpect(jsonPath("$.nivelEstudios").value("Bachillerato"))
                .andExpect(jsonPath("$.descripcionPersonal").value("Descripcion de prueba"));
        mvc.perform(get("/api/admin/usuarios/" + id(Rol.SUPERVISOR_DE_USUARIOS)).session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfilDisponible").value(false));
    }
    @ParameterizedTest
    @EnumSource(value = Rol.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void otroRolNoConsultaNiListaNiAbrePantalla(Rol rol) throws Exception {
        mvc.perform(get("/api/admin/usuarios").session(sesion(rol))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/usuarios/" + id(Rol.ESTUDIANTE)).session(sesion(rol)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/administracion-usuarios.html").session(sesion(rol)))
                .andExpect(status().isForbidden());
    }
    @Test
    void sinSesionNoPuedeLeerDatosNiAbrirPantalla() throws Exception {
        mvc.perform(get("/api/admin/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/usuarios/" + id(Rol.ESTUDIANTE))).andExpect(status().isUnauthorized());
        mvc.perform(get("/administracion-usuarios.html")).andExpect(status().is3xxRedirection());
    }
    @Test
    void pantallaDisponibleParaAdministrador() throws Exception {
        mvc.perform(get("/administracion-usuarios.html").session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("lista-admin-usuarios")));
    }
    @Test
    void usuarioInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/admin/usuarios/no-existe").session(sesion(Rol.ADMIN))).andExpect(status().isNotFound());
    }
    @Test
    void paginacionEsEstableYValidaLosLimites() throws Exception {
        mvc.perform(get("/api/admin/usuarios?pagina=1&tamano=1").session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuarios.length()").value(1))
                .andExpect(jsonPath("$.pagina").value(1)).andExpect(jsonPath("$.usuarios[0].rol").value("COLABORADOR"));
        for (String consulta : new String[]{"pagina=-1", "tamano=0", "tamano=51"}) {
            mvc.perform(get("/api/admin/usuarios?" + consulta).session(sesion(Rol.ADMIN)))
                    .andExpect(status().isBadRequest());
        }
    }
    @Test
    void sesionQueDiceAdminNoBastaSiLaCuentaTieneOtroRol() throws Exception {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(GestorPermisos.ATRIBUTO_SESION,
                new SesionUsuario(id(Rol.ESTUDIANTE), "Cuenta", Rol.ADMIN, "2026-11-01T00:00:00Z"));
        mvc.perform(get("/api/admin/usuarios").session(s)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/usuarios/" + id(Rol.COLABORADOR)).session(s)).andExpect(status().isForbidden());
    }
    @Test
    void cuentaAdministradoraInexistenteNoConservaAcceso() throws Exception {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(GestorPermisos.ATRIBUTO_SESION,
                new SesionUsuario("admin-que-no-existe", "Cuenta", Rol.ADMIN, "2026-11-01T00:00:00Z"));
        mvc.perform(get("/api/admin/usuarios").session(s)).andExpect(status().isUnauthorized());
    }
}
