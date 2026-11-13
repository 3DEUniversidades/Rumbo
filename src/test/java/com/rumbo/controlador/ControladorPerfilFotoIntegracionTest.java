package com.rumbo.controlador;

import com.rumbo.ConfiguracionAlmacenamientoPrueba;
import com.rumbo.modelo.EstadoVerificacion;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.UsuarioRepository;
import com.rumbo.servicio.GestorPermisos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ConfiguracionAlmacenamientoPrueba.class)
class ControladorPerfilFotoIntegracionTest {

    private static final String HASH_PRUEBA =
            "$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji";

    private static final byte[] JPEG = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarios;

    private Usuario estudianteUno;
    private Usuario estudianteDos;
    private Usuario administrador;

    @BeforeEach
    void prepararDatos() {
        estudianteUno = usuarios.save(new Usuario(
                "perf-foto-01",
                "perfil_foto_uno",
                "Perfil Foto Uno",
                "perfil-foto-uno@example.com",
                HASH_PRUEBA,
                Rol.ESTUDIANTE,
                EstadoVerificacion.NO_VERIFICADO
        ));

        estudianteDos = usuarios.save(new Usuario(
                "perf-foto-02",
                "perfil_foto_dos",
                "Perfil Foto Dos",
                "perfil-foto-dos@example.com",
                HASH_PRUEBA,
                Rol.ESTUDIANTE,
                EstadoVerificacion.NO_VERIFICADO
        ));

        administrador = usuarios.save(new Usuario(
                "perf-foto-admin",
                "perfil_foto_admin",
                "Perfil Foto Admin",
                "perfil-foto-admin@example.com",
                HASH_PRUEBA,
                Rol.ADMIN,
                EstadoVerificacion.NO_VERIFICADO
        ));
    }

    @Test
    void estudianteSubeFotoYSeMuestraEnSuPerfilPublico() throws Exception {
        MockMultipartFile foto = new MockMultipartFile(
                "archivo",
                "foto.jpg",
                "image/jpeg",
                JPEG
        );

        mockMvc.perform(multipart("/api/perfil/foto")
                        .file(foto)
                        .session(sesion(estudianteUno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl").isNotEmpty());

        mockMvc.perform(get("/api/perfil/publico/{id}", estudianteUno.getIdUsuario())
                        .session(sesion(estudianteDos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl").isNotEmpty());
    }

    @Test
    void estudiantePuedeEliminarSuFoto() throws Exception {
        subirFotoValida();

        mockMvc.perform(delete("/api/perfil/foto")
                        .session(sesion(estudianteUno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl").value(nullValue()));

        mockMvc.perform(get("/api/perfil/publico/{id}", estudianteUno.getIdUsuario())
                        .session(sesion(estudianteDos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl").value(nullValue()));
    }

    @Test
    void archivoQueNoEsImagenDevuelve400() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "documento.txt",
                "text/plain",
                "no es una imagen".getBytes()
        );

        mockMvc.perform(multipart("/api/perfil/foto")
                        .file(archivo)
                        .session(sesion(estudianteUno)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.archivo").isNotEmpty());
    }

    @Test
    void administradorNoPuedeSubirFotoDePerfil() throws Exception {
        MockMultipartFile foto = new MockMultipartFile(
                "archivo",
                "foto.jpg",
                "image/jpeg",
                JPEG
        );

        mockMvc.perform(multipart("/api/perfil/foto")
                        .file(foto)
                        .session(sesion(administrador)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinSesionDevuelve401() throws Exception {
        MockMultipartFile foto = new MockMultipartFile(
                "archivo",
                "foto.jpg",
                "image/jpeg",
                JPEG
        );

        mockMvc.perform(multipart("/api/perfil/foto").file(foto))
                .andExpect(status().isUnauthorized());
    }

    private void subirFotoValida() throws Exception {
        MockMultipartFile foto = new MockMultipartFile(
                "archivo",
                "foto.jpg",
                "image/jpeg",
                JPEG
        );

        mockMvc.perform(multipart("/api/perfil/foto")
                        .file(foto)
                        .session(sesion(estudianteUno)))
                .andExpect(status().isOk());
    }

    private MockHttpSession sesion(Usuario usuario) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(
                GestorPermisos.ATRIBUTO_SESION,
                new SesionUsuario(
                        usuario.getIdUsuario(),
                        usuario.getNombreCompleto(),
                        usuario.getRol(),
                        "2026-10-31T00:00:00Z"
                )
        );
        return sesion;
    }
}
