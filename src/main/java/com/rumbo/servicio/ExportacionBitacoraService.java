package com.rumbo.servicio;

import com.rumbo.dto.EntradaBitacoraRespuesta;
import com.rumbo.modelo.Rol;
import com.rumbo.persistencia.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportacionBitacoraService {

    private static final Logger LOG = LoggerFactory.getLogger(ExportacionBitacoraService.class);
    private final BitacoraService bitacora;
    private final UsuarioRepository usuarios;
    private final GeneradorPdfBitacora pdf;

    public ExportacionBitacoraService(BitacoraService bitacora, UsuarioRepository usuarios,
                                     GeneradorPdfBitacora pdf) {
        this.bitacora = bitacora;
        this.usuarios = usuarios;
        this.pdf = pdf;
    }

    public ArchivoExportado exportarPdf(String idUsuario) {
        DatosExportacion datos = consultarDatos(idUsuario);
        return escribirTemporal("pdf", salida -> pdf.escribir(salida, datos.estudiante(), datos.entradas()));
    }

    public ArchivoExportado exportarZip(String idUsuario) {
        DatosExportacion datos = consultarDatos(idUsuario);
        return escribirTemporal("zip", salida -> {
            try (ZipOutputStream zip = new ZipOutputStream(salida, StandardCharsets.UTF_8)) {
                zip.putNextEntry(new ZipEntry("bitacora.pdf"));
                pdf.escribir(zip, datos.estudiante(), datos.entradas());
                zip.closeEntry();

                int numeroEntrada = 1;
                for (EntradaBitacoraRespuesta entrada : datos.entradas()) {
                    int numeroAdjunto = 1;
                    for (var adjunto : entrada.adjuntos()) {
                        String nombre = String.format(Locale.ROOT, "adjuntos/entrada-%03d/%03d-%s",
                                numeroEntrada, numeroAdjunto++, nombreSeguro(adjunto.nombreOriginal()));
                        var descarga = bitacora.obtenerContenidoAdjunto(
                                idUsuario, entrada.idEntrada(), adjunto.idAdjunto());
                        try (InputStream contenido = descarga.recurso().contenido()) {
                            zip.putNextEntry(new ZipEntry(nombre));
                            long copiados = contenido.transferTo(zip);
                            if (descarga.tamanoBytes() >= 0 && copiados != descarga.tamanoBytes()) {
                                throw new ExcepcionAlmacenamiento(
                                        "Un adjunto se descargó incompleto. Intenta exportar nuevamente.");
                            }
                            zip.closeEntry();
                        } catch (IOException e) {
                            throw new ExcepcionAlmacenamiento(
                                    "No se pudieron incluir todos los adjuntos. Intenta exportar nuevamente.", e);
                        }
                    }
                    numeroEntrada++;
                }
            }
        });
    }

    private String nombreSeguro(String original) {
        String nombre = original == null ? "" : Normalizer.normalize(original, Normalizer.Form.NFC);
        nombre = nombre.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}:*?\"<>|]", "_")
                .replaceAll("[. ]+$", "").strip();
        if (nombre.isBlank()) return "archivo";
        if (nombre.codePointCount(0, nombre.length()) > 140) {
            int punto = nombre.lastIndexOf('.');
            String extension = punto > 0 && nombre.length() - punto <= 10 ? nombre.substring(punto) : "";
            int fin = nombre.offsetByCodePoints(0,
                    Math.min(140 - extension.length(), nombre.codePointCount(0, nombre.length())));
            nombre = nombre.substring(0, fin) + extension;
        }
        return nombre;
    }

    private DatosExportacion consultarDatos(String idUsuario) {
        var usuario = usuarios.findById(idUsuario)
                .filter(u -> !u.isEliminado())
                .orElseThrow(() -> new ExcepcionAutenticacion("La cuenta no está disponible"));
        if (usuario.getRol() != Rol.ESTUDIANTE) {
            throw new ExcepcionPermiso("Solo estudiantes pueden exportar su bitácora");
        }
        List<EntradaBitacoraRespuesta> entradas = bitacora.listar(idUsuario).stream()
                .sorted(Comparator.comparing(EntradaBitacoraRespuesta::fechaCreacion)
                        .thenComparing(EntradaBitacoraRespuesta::idEntrada))
                .toList();
        if (entradas.isEmpty()) {
            throw new ExcepcionValidacion(Map.of("bitacora",
                    "Guarda tu primera entrada antes de exportar la bitácora."));
        }
        return new DatosExportacion(usuario.getNombreCompleto(), entradas);
    }

    private ArchivoExportado escribirTemporal(String extension, EscritorArchivo escritor) {
        Path temporal = null;
        try {
            temporal = Files.createTempFile("rumbo-bitacora-", "." + extension);
            try (OutputStream salida = new BufferedOutputStream(Files.newOutputStream(temporal))) {
                escritor.escribir(salida);
            }
            return new ArchivoExportado(temporal, "bitacora." + extension,
                    "pdf".equals(extension) ? "application/pdf" : "application/zip");
        } catch (IOException e) {
            limpiarTemporal(temporal);
            throw new ExcepcionExportacionBitacora("No se pudo generar la exportación de la bitácora", e);
        } catch (RuntimeException e) {
            limpiarTemporal(temporal);
            throw e;
        }
    }

    private void limpiarTemporal(Path ruta) {
        if (ruta == null) return;
        try {
            Files.deleteIfExists(ruta);
        } catch (IOException e) {
            LOG.warn("No se pudo eliminar un archivo temporal de exportación", e);
        }
    }

    private record DatosExportacion(String estudiante, List<EntradaBitacoraRespuesta> entradas) {}

    @FunctionalInterface
    private interface EscritorArchivo {
        void escribir(OutputStream salida) throws IOException;
    }
}
