package com.rumbo.controlador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumbo.ConfiguracionAlmacenamientoPrueba;
import com.rumbo.modelo.EntradaBitacora;
import com.rumbo.modelo.EstadoVerificacion;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.EntradaBitacoraRepository;
import com.rumbo.persistencia.UsuarioRepository;
import com.rumbo.servicio.GestorPermisos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ConfiguracionAlmacenamientoPrueba.class)
class ControladorBitacoraAdjuntosIntegracionTest {

    private static final String HASH_PRUEBA =
            "$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji";

    private static final byte[] JPEG = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
    };

    private static final byte[] PDF = "%PDF-1.4 contenido de prueba".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private EntradaBitacoraRepository entradas;

    private Usuario estudianteUno;
    private Usuario estudianteDos;
    private EntradaBitacora entrada;

    @BeforeEach
    void prepararDatos() {
        estudianteUno = usuarios.save(new Usuario(
                "bit-adj-01",
                "bit_adj_uno",
                "Bitácora Adjuntos Uno",
                "bit-adq-uno@example.com",
                HASH_PRUEBA,
                Rol.ESTUDIANTE,
                EstadoVerificacion.NO_VERIFICADO
        ));

        estudianteDos = usuarios.save(new Usuario(
                "bit-adj-02",
                "bit_adj_dos",
                "Bitácora Adjuntos Dos",
                "bit-adq-dos@example.com",
                HASH_PRUEBA,
                Rol.ESTUDIANTE,
                EstadoVerificacion.NO_VERIFICADO
        ));

        entrada = entradas.save(new EntradaBitacora(
                estudianteUno,
                "Entrada con adjuntos",
                null
        ));
    }

    @Test
    void subeListaYDescargaUnAdjuntoPropio() throws Exception {
        String idAdjunto = subirAdjunto(estudianteUno, "foto.jpg", "image/jpeg", JPEG);

        mockMvc.perform(get("/api/bitacora")
                        .session(sesion(estudianteUno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].adjuntos", hasSize(1)))
                .andExpect(jsonPath("$[0].adjuntos[0].idAdjunto").value(idAdjunto))
                .andExpect(jsonPath("$[0].adjuntos[0].esImagen").value(true));

        mockMvc.perform(get("/api/bitacora/{id}/adjuntos", entrada.getIdEntrada())
                        .session(sesion(estudianteUno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombreOriginal").value("foto.jpg"));

        mockMvc.perform(get(
                        "/api/bitacora/{id}/adjuntos/{adjunto}/contenido",
                        entrada.getIdEntrada(),
                        idAdjunto
                ).session(sesion(estudianteUno)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(JPEG))
                .andExpect(header().stringValues(
                        HttpHeaders.CACHE_CONTROL,
                        "no-store",
                        "private, no-store"
                ));

        mockMvc.perform(delete(
                        "/api/bitacora/{id}/adjuntos/{adjunto}",
                        entrada.getIdEntrada(),
                        idAdjunto
                ).session(sesion(estudianteUno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Adjunto eliminado"));

        mockMvc.perform(get("/api/bitacora/{id}/adjuntos", entrada.getIdEntrada())
                        .session(sesion(estudianteUno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void subeYAbrePdfPrivadoSinExponerLaUrlDeCloudinary() throws Exception {
        String idAdjunto = subirAdjunto(
                estudianteUno, "documento.pdf", "application/pdf", PDF);

        mockMvc.perform(get("/api/bitacora/{id}/adjuntos", entrada.getIdEntrada())
                        .session(sesion(estudianteUno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].esImagen").value(false))
                .andExpect(jsonPath("$[0].tipoMime").value("application/pdf"))
                .andExpect(jsonPath("$[0].publicId").doesNotExist())
                .andExpect(jsonPath("$[0].urlContenido").value(
                        "/api/bitacora/" + entrada.getIdEntrada()
                                + "/adjuntos/" + idAdjunto + "/contenido"));

        mockMvc.perform(get(
                        "/api/bitacora/{id}/adjuntos/{adjunto}/contenido",
                        entrada.getIdEntrada(), idAdjunto
                ).session(sesion(estudianteUno)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(PDF))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.startsWith("inline;")));
    }

    @Test
    void otroEstudianteNoPuedeListarNiDescargarAdjuntosAjenos() throws Exception {
        String idAdjunto = subirAdjunto(estudianteUno, "documento.pdf", "application/pdf", PDF);

        mockMvc.perform(get("/api/bitacora/{id}/adjuntos", entrada.getIdEntrada())
                        .session(sesion(estudianteDos)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(
                        "/api/bitacora/{id}/adjuntos/{adjunto}/contenido",
                        entrada.getIdEntrada(),
                        idAdjunto
                ).session(sesion(estudianteDos)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(
                        "/api/bitacora/{id}/adjuntos/{adjunto}",
                        entrada.getIdEntrada(),
                        idAdjunto
                ).session(sesion(estudianteDos)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/bitacora/{id}/adjuntos", entrada.getIdEntrada())
                        .session(sesion(estudianteUno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void rechazaFormatoNoPermitidoYMasDeCincoAdjuntos() throws Exception {
        mockMvc.perform(multipart(
                        "/api/bitacora/{id}/adjuntos",
                        entrada.getIdEntrada()
                )
                        .file(new MockMultipartFile(
                                "archivo",
                                "notas.txt",
                                "text/plain",
                                "texto".getBytes()
                        ))
                        .session(sesion(estudianteUno)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.archivo").isNotEmpty());

        for (int i = 0; i < 5; i++) {
            subirAdjunto(estudianteUno, "foto-" + i + ".jpg", "image/jpeg", JPEG);
        }

        mockMvc.perform(multipart(
                        "/api/bitacora/{id}/adjuntos",
                        entrada.getIdEntrada()
                )
                        .file(new MockMultipartFile(
                                "archivo",
                                "foto-extra.jpg",
                                "image/jpeg",
                                JPEG
                        ))
                        .session(sesion(estudianteUno)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.archivo")
                        .value("Puedes adjuntar hasta 5 archivos por entrada"));
    }

    private String subirAdjunto(
            Usuario usuario,
            String nombre,
            String tipoMime,
            byte[] contenido
    ) throws Exception {
        String respuesta = mockMvc.perform(multipart(
                        "/api/bitacora/{id}/adjuntos",
                        entrada.getIdEntrada()
                )
                        .file(new MockMultipartFile(
                                "archivo",
                                nombre,
                                tipoMime,
                                contenido
                        ))
                        .session(sesion(usuario)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idAdjunto").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return json.readTree(respuesta).get("idAdjunto").asText();
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
