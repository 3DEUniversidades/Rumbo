package com.rumbo.controlador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumbo.ConfiguracionAlmacenamientoPrueba;
import com.rumbo.modelo.EntradaBitacora;
import com.rumbo.modelo.EstadoVerificacion;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.AdjuntoBitacoraRepository;
import com.rumbo.persistencia.EntradaBitacoraRepository;
import com.rumbo.persistencia.UsuarioRepository;
import com.rumbo.servicio.AlmacenamientoArchivos;
import com.rumbo.servicio.ExportacionBitacoraService;
import com.rumbo.servicio.GestorPermisos;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ConfiguracionAlmacenamientoPrueba.class)
class ControladorExportacionBitacoraIntegracionTest {

    private static final String HASH = "$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji";
    private static final byte[] PDF_ADJUNTO = "%PDF-1.4 documento original".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PNG = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 1, 2, 3};

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private EntradaBitacoraRepository entradas;
    @Autowired private AdjuntoBitacoraRepository adjuntos;
    @Autowired private AlmacenamientoArchivos almacenamiento;
    @Autowired private ExportacionBitacoraService exportacion;

    private Usuario dueno;
    private Usuario otro;

    @BeforeEach
    void preparar() {
        dueno = usuario("exp-dueno", "María Muñoz", Rol.ESTUDIANTE);
        otro = usuario("exp-otro", "Estudiante Ajeno", Rol.ESTUDIANTE);
    }

    @Test
    void pdfExportaSoloLasEntradasDeLaSesionEnOrdenCronologico() throws Exception {
        entrada(dueno, "Mi reflexión más reciente.", 6);
        entrada(dueno, "Mi primera reflexión: ¡qué aprendí!", 5);
        entrada(otro, "Contenido privado ajeno.", 4);

        var resultado = mvc.perform(get("/api/bitacora/exportar/pdf")
                        .param("idUsuario", otro.getIdUsuario()).session(sesion(dueno)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment;")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("bitacora.pdf")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn();
        assertTrue(resultado.getResponse().getHeaders(HttpHeaders.CACHE_CONTROL).contains("private, no-store"));
        String texto = textoPdf(resultado.getResponse().getContentAsByteArray());
        assertTrue(texto.contains("María Muñoz"));
        assertTrue(texto.contains("Mi primera reflexión: ¡qué aprendí!"));
        assertTrue(texto.indexOf("Mi primera reflexión") < texto.indexOf("Mi reflexión más reciente"));
        assertFalse(texto.contains("Contenido privado ajeno"));
        assertFalse(texto.contains("Estudiante Ajeno"));
    }

    @Test
    void zipIncluyePdfOriginalesYFotografiasSinPerderNombresRepetidos() throws Exception {
        var primera = entrada(dueno, "Texto incluido en el PDF del ZIP.", 5);
        var segunda = entrada(dueno, "Segunda entrada propia.", 6);
        var ajena = entrada(otro, "Texto ajeno que no se debe exportar.", 4);
        subir(dueno, primera, "documento.pdf", "application/pdf", PDF_ADJUNTO);
        subir(dueno, primera, "documento.pdf", "application/pdf", PDF_ADJUNTO);
        subir(dueno, segunda, "../../foto.png", "image/png", PNG);
        subir(otro, ajena, "archivo-ajeno.pdf", "application/pdf", PDF_ADJUNTO);

        byte[] bytes = mvc.perform(get("/api/bitacora/exportar/zip").session(sesion(dueno)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("bitacora.zip")))
                .andReturn().getResponse().getContentAsByteArray();
        Map<String, byte[]> archivos = leerZip(bytes);
        assertEquals(4, archivos.size());
        assertTrue(archivos.containsKey("bitacora.pdf"));
        assertEquals(2, archivos.keySet().stream().filter(n -> n.endsWith("documento.pdf")).count());
        for (var archivo : archivos.entrySet()) {
            assertFalse(archivo.getKey().contains(".."));
            assertFalse(archivo.getKey().startsWith("/"));
            assertFalse(archivo.getKey().contains("archivo-ajeno"));
            if (archivo.getKey().endsWith("documento.pdf")) assertArrayEquals(PDF_ADJUNTO, archivo.getValue());
            if (archivo.getKey().endsWith("foto.png")) assertArrayEquals(PNG, archivo.getValue());
        }
        assertTrue(archivos.keySet().stream().anyMatch(n -> n.startsWith("adjuntos/entrada-001/")));
        assertTrue(archivos.keySet().stream().anyMatch(n -> n.startsWith("adjuntos/entrada-002/")));
        String texto = textoPdf(archivos.get("bitacora.pdf"));
        assertTrue(texto.contains("Texto incluido en el PDF del ZIP."));
        assertTrue(texto.contains("Segunda entrada propia."));
        assertFalse(texto.contains("Texto ajeno"));
        assertFalse(texto.contains("cloudinary.com"));
    }

    @Test
    void zipSinAdjuntosContieneElPdf() throws Exception {
        entrada(dueno, "Entrada sin archivos adjuntos.", 5);
        byte[] bytes = mvc.perform(get("/api/bitacora/exportar/zip").session(sesion(dueno)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertEquals(1, leerZip(bytes).size());
        assertTrue(textoPdf(leerZip(bytes).get("bitacora.pdf")).contains("Entrada sin archivos adjuntos."));
    }

    @Test
    void bitacoraVaciaDevuelveUnErrorUtilEnAmbosFormatos() throws Exception {
        for (String formato : new String[]{"pdf", "zip"}) {
            mvc.perform(get("/api/bitacora/exportar/" + formato).session(sesion(dueno)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.bitacora", containsString("primera entrada")));
        }
    }

    @Test
    void exigeSesionYRolEstudianteParaAmbasDescargas() throws Exception {
        var admin = usuario("exp-admin", "Administrador", Rol.ADMIN);
        var colaborador = usuario("exp-colaborador", "Colaborador", Rol.COLABORADOR);
        for (String formato : new String[]{"pdf", "zip"}) {
            mvc.perform(get("/api/bitacora/exportar/" + formato)).andExpect(status().isUnauthorized());
            for (Usuario usuario : Arrays.asList(admin, colaborador)) {
                mvc.perform(get("/api/bitacora/exportar/" + formato).session(sesion(usuario)))
                        .andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void cuentaEliminadaNoPuedeExportarAunqueConserveUnaSesion() throws Exception {
        entrada(dueno, "Contenido anterior a eliminar la cuenta.", 5);
        dueno.eliminarCuenta();
        usuarios.saveAndFlush(dueno);
        for (String formato : new String[]{"pdf", "zip"}) {
            mvc.perform(get("/api/bitacora/exportar/" + formato).session(sesion(dueno)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void adjuntoInaccesibleDevuelveJsonDeErrorEnVezDeUnZipIncompleto() throws Exception {
        var entrada = entrada(dueno, "Este texto sí puede exportarse sin los adjuntos.", 5);
        String id = subir(dueno, entrada, "documento.pdf", "application/pdf", PDF_ADJUNTO);
        var adjunto = adjuntos.findById(id).orElseThrow();
        almacenamiento.eliminar(adjunto.getPublicId(), adjunto.getTipoRecurso(), adjunto.getTipoEntrega());

        mvc.perform(get("/api/bitacora/exportar/zip").session(sesion(dueno)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(jsonPath("$.error").isNotEmpty());
        mvc.perform(get("/api/bitacora/exportar/pdf").session(sesion(dueno)))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
    }

    @Test
    void archivoTemporalSeEliminaAlTerminarLaLectura() throws Exception {
        entrada(dueno, "Contenido temporal.", 5);
        try (var archivo = exportacion.exportarPdf(dueno.getIdUsuario())) {
            assertTrue(Files.exists(archivo.ruta()));
            try (var contenido = archivo.abrirContenido()) {
                assertEquals("%PDF-", new String(contenido.readNBytes(5), StandardCharsets.US_ASCII));
            }
            assertFalse(Files.exists(archivo.ruta()));
        }
    }

    private Usuario usuario(String id, String nombre, Rol rol) {
        return usuarios.save(new Usuario(id, id, nombre, id + "@example.com", HASH, rol,
                EstadoVerificacion.NO_VERIFICADO));
    }

    private EntradaBitacora entrada(Usuario usuario, String texto, int dia) {
        var entrada = new EntradaBitacora(usuario, texto, null);
        ReflectionTestUtils.setField(entrada, "fechaCreacion", LocalDateTime.of(2026, 11, dia, 10, 0));
        return entradas.saveAndFlush(entrada);
    }

    private String subir(Usuario usuario, EntradaBitacora entrada, String nombre, String tipo, byte[] bytes) throws Exception {
        String respuesta = mvc.perform(multipart("/api/bitacora/{id}/adjuntos", entrada.getIdEntrada())
                        .file(new MockMultipartFile("archivo", nombre, tipo, bytes)).session(sesion(usuario)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("idAdjunto").asText();
    }

    private MockHttpSession sesion(Usuario usuario) {
        var sesion = new MockHttpSession();
        sesion.setAttribute(GestorPermisos.ATRIBUTO_SESION, new SesionUsuario(usuario.getIdUsuario(),
                usuario.getNombreCompleto(), usuario.getRol(), "2026-11-02T00:00:00Z"));
        return sesion;
    }

    private String textoPdf(byte[] contenido) throws Exception {
        try (var documento = Loader.loadPDF(contenido)) {
            return new PDFTextStripper().getText(documento);
        }
    }

    private Map<String, byte[]> leerZip(byte[] contenido) throws Exception {
        Map<String, byte[]> archivos = new LinkedHashMap<>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(contenido), StandardCharsets.UTF_8)) {
            for (var entrada = zip.getNextEntry(); entrada != null; entrada = zip.getNextEntry()) {
                assertNull(archivos.put(entrada.getName(), zip.readAllBytes()), "Archivo duplicado en ZIP");
            }
        }
        return archivos;
    }
}
