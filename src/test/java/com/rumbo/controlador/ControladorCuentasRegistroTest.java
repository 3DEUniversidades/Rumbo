package com.rumbo.controlador;

import com.rumbo.modelo.EstadoVerificacion;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ControladorCuentasRegistroTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void registraEstudianteYNoExponeContrasenia() throws Exception {
        String json = """
                {"nombreUsuario":"juan","nombreCompleto":"Juan Perez","correo":"juan@correo.com",
                 "contrasenia":"contrasenia123","rol":"ESTUDIANTE"}
                """;

        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").exists())
                .andExpect(jsonPath("$.nombreUsuario").value("juan"))
                .andExpect(jsonPath("$.correo").value("juan@correo.com"))
                .andExpect(jsonPath("$.rol").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.estadoVerificacion").value("NO_VERIFICADO"))
                .andExpect(content().string(not(containsString("contrasenia"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void registraColaboradorConCorreoInstitucional() throws Exception {
        String json = """
                {"nombreUsuario":"colab","nombreCompleto":"Colaborador UVG","correo":"colab@uvg.edu.gt",
                 "contrasenia":"contrasenia123","rol":"COLABORADOR"}
                """;

        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("COLABORADOR"))
                .andExpect(jsonPath("$.estadoVerificacion").value("VERIFICADO"));
    }

    @Test
    void rechazaColaboradorConCorreoNoInstitucional() throws Exception {
        String json = """
                {"nombreUsuario":"colab","nombreCompleto":"Colaborador","correo":"colab@otro.com",
                 "contrasenia":"contrasenia123","rol":"COLABORADOR"}
                """;

        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.correo").exists());
    }

    @Test
    void rechazaCorreoInstitucionalEnganoso() throws Exception {
        String json = """
                {"nombreUsuario":"colab","nombreCompleto":"Colaborador","correo":"colab@uvg.edu.gt.falso.com",
                 "contrasenia":"contrasenia123","rol":"COLABORADOR"}
                """;

        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.correo").exists());
    }

    @Test
    void rechazaRolAdministrativo() throws Exception {
        String json = """
                {"nombreUsuario":"admin2","nombreCompleto":"Admin","correo":"admin2@uvg.edu.gt",
                 "contrasenia":"contrasenia123","rol":"ADMINISTRADOR"}
                """;

        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.rol").exists());
    }

    @Test
    void rechazaRolInexistente() throws Exception {
        String json = """
                {"nombreUsuario":"gerente","nombreCompleto":"Gerente","correo":"gerente@uvg.edu.gt",
                 "contrasenia":"contrasenia123","rol":"GERENTE"}
                """;

        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.rol").exists());
    }

    @Test
    void rechazaContraseniaCorta() throws Exception {
        String json = """
                {"nombreUsuario":"juan","nombreCompleto":"Juan Perez","correo":"juan@correo.com",
                 "contrasenia":"corta12","rol":"ESTUDIANTE"}
                """;

        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.contrasenia").exists());
    }

    @Test
    void rechazaNombreDeUsuarioDuplicado() throws Exception {
        registrar("juan", "juan@correo.com");

        String json = """
                {"nombreUsuario":"JUAN","nombreCompleto":"Juan Otro","correo":"juan2@correo.com",
                 "contrasenia":"contrasenia123","rol":"ESTUDIANTE"}
                """;

        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("usuario")));
    }

    @Test
    void rechazaCorreoDuplicadoSinDistinguirMayusculas() throws Exception {
        registrar("juan", "juan@correo.com");

        String json = """
                {"nombreUsuario":"juan2","nombreCompleto":"Juan Otro","correo":"JUAN@Correo.com",
                 "contrasenia":"contrasenia123","rol":"ESTUDIANTE"}
                """;

        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("correo")));
    }

    @Test
    void loginSigueFuncionandoConCuentaExistente() throws Exception {
        Usuario usuario = new Usuario(
                "usr-test", "tester", "Tester Uno", "tester@correo.com",
                new BCryptPasswordEncoder().encode("clave-de-prueba"), Rol.ESTUDIANTE, EstadoVerificacion.NO_VERIFICADO);
        usuarioRepository.save(usuario);

        String json = """
                {"correo":"tester@correo.com","contrasenia":"clave-de-prueba"}
                """;

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value("usr-test"))
                .andExpect(jsonPath("$.rol").value("ESTUDIANTE"));
    }

    @Test
    void loginCreaSesionConRolYDatosDelUsuario() throws Exception {
        Usuario usuario = new Usuario(
                "usr-sesion", "sesion", "Usuario Sesion", "sesion@correo.com",
                new BCryptPasswordEncoder().encode("clave-de-prueba"), Rol.ESTUDIANTE, EstadoVerificacion.NO_VERIFICADO);
        usuarioRepository.save(usuario);

        MockHttpSession sesion = iniciarSesion("sesion@correo.com");

        mockMvc.perform(get("/api/auth/sesion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value("usr-sesion"))
                .andExpect(jsonPath("$.nombreCompleto").value("Usuario Sesion"))
                .andExpect(jsonPath("$.rol").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    void consultaSesionSinLoginRechazaLaSolicitud() throws Exception {
        mockMvc.perform(get("/api/auth/sesion"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidaLaSesion() throws Exception {
        Usuario usuario = new Usuario(
                "usr-logout", "logout", "Usuario Logout", "logout@correo.com",
                new BCryptPasswordEncoder().encode("clave-de-prueba"), Rol.ESTUDIANTE, EstadoVerificacion.NO_VERIFICADO);
        usuarioRepository.save(usuario);

        MockHttpSession sesion = iniciarSesion("logout@correo.com");

        mockMvc.perform(post("/api/auth/logout").session(sesion))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/sesion").session(sesion))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRechazaCredencialesIncorrectas() throws Exception {
        Usuario usuario = new Usuario(
                "usr-test", "tester", "Tester Uno", "tester@correo.com",
                new BCryptPasswordEncoder().encode("clave-de-prueba"), Rol.ESTUDIANTE, EstadoVerificacion.NO_VERIFICADO);
        usuarioRepository.save(usuario);

        String json = """
                {"correo":"tester@correo.com","contrasenia":"incorrecta"}
                """;

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginAceptaCorreoSinDistinguirMayusculasNiEspacios() throws Exception {
        Usuario usuario = new Usuario(
                "usr-test", "tester", "Tester Uno", "tester@correo.com",
                new BCryptPasswordEncoder().encode("clave-de-prueba"), Rol.ESTUDIANTE, EstadoVerificacion.NO_VERIFICADO);
        usuarioRepository.save(usuario);

        String json = """
                {"correo":"  TESTER@CORREO.COM  ","contrasenia":"clave-de-prueba"}
                """;

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value("usr-test"));
    }

    @Test
    void loginRechazaDatosIncompletos() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.correo").exists())
                .andExpect(jsonPath("$.campos.contrasenia").exists());
    }

    @Test
    void rutaDeCarrerasRechazaSolicitudSinSesion() throws Exception {
        mockMvc.perform(get("/api/carreras"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutaDeActividadesRechazaRolQueNoEsEstudiante() throws Exception {
        Usuario usuario = new Usuario(
                "usr-colab", "colab", "Colaborador", "colab@uvg.edu.gt",
                new BCryptPasswordEncoder().encode("clave-de-prueba"), Rol.COLABORADOR, EstadoVerificacion.NO_VERIFICADO);
        usuarioRepository.save(usuario);

        MockHttpSession sesion = iniciarSesion("colab@uvg.edu.gt");

        mockMvc.perform(get("/api/actividades").session(sesion))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginDevuelveLaPantallaDelRolActivo() throws Exception {
        guardarUsuario("usr-pantalla", "pantalla", "Usuario Pantalla",
                "pantalla@correo.com", Rol.ESTUDIANTE);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"pantalla@correo.com","contrasenia":"clave-de-prueba"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.rutaPantalla").value("/roles/estudiante.html"));
    }

    @Test
    void paginaDeOtroRolNoSeSirveAlUsuarioActivo() throws Exception {
        guardarUsuario("usr-pantalla", "pantalla", "Usuario Pantalla",
                "pantalla@correo.com", Rol.ESTUDIANTE);
        MockHttpSession sesion = iniciarSesion("pantalla@correo.com");

        mockMvc.perform(get("/roles/administrador.html").session(sesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/roles/estudiante.html"));
    }

    @Test
    void paginaDeRolNoSeSirveSinSesion() throws Exception {
        mockMvc.perform(get("/roles/estudiante.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html"));
    }

    private void guardarUsuario(String id, String nombreUsuario, String nombreCompleto,
                                String correo, Rol rol) {
        usuarioRepository.save(new Usuario(
                id, nombreUsuario, nombreCompleto, correo,
                new BCryptPasswordEncoder().encode("clave-de-prueba"),
                rol, EstadoVerificacion.VERIFICADO));
    }

    private MockHttpSession iniciarSesion(String correo) throws Exception {
        String json = """
                {"correo":"%s","contrasenia":"clave-de-prueba"}
                """.formatted(correo);

        return (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn()
                .getRequest()
                .getSession();
    }

    private void registrar(String nombreUsuario, String correo) throws Exception {
        String json = String.format("""
                {"nombreUsuario":"%s","nombreCompleto":"Juan Perez","correo":"%s",
                 "contrasenia":"contrasenia123","rol":"ESTUDIANTE"}
                """, nombreUsuario, correo);

        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }
}
