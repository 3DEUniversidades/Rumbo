package com.rumbo.servicio;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Component
public class ValidadorArchivos {

    private static final int BYTES_CABECERA = 12;

    public ArchivoValidado validarImagen(MultipartFile archivo, long maximoBytes) {
        return validar(archivo, maximoBytes, false);
    }

    public ArchivoValidado validarAdjunto(MultipartFile archivo, long maximoBytes) {
        return validar(archivo, maximoBytes, true);
    }

    private ArchivoValidado validar(
            MultipartFile archivo,
            long maximoBytes,
            boolean permitirPdf
    ) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ExcepcionValidacion(Map.of(
                    "archivo",
                    "El archivo es obligatorio"
            ));
        }

        if (archivo.getSize() > maximoBytes) {
            long megas = maximoBytes / (1024 * 1024);
            throw new ExcepcionValidacion(Map.of(
                    "archivo",
                    "El archivo no puede superar los " + megas + " MB"
            ));
        }

        TipoDetectado tipo = detectar(leerCabecera(archivo));

        if (tipo == null) {
            throw new ExcepcionValidacion(Map.of(
                    "archivo",
                    "Formato no permitido. Usa JPG, PNG, WebP"
                            + (permitirPdf ? " o PDF" : "")
            ));
        }

        if (!tipo.imagen() && !permitirPdf) {
            throw new ExcepcionValidacion(Map.of(
                    "archivo",
                    "Formato no permitido. Usa JPG, PNG o WebP"
            ));
        }

        return new ArchivoValidado(
                tipo.tipoMime(),
                tipo.extension(),
                tipo.tipoRecurso(),
                tipo.imagen(),
                nombreSeguro(archivo.getOriginalFilename(), tipo.extension())
        );
    }

    private byte[] leerCabecera(MultipartFile archivo) {
        try (InputStream entrada = archivo.getInputStream()) {
            return entrada.readNBytes(BYTES_CABECERA);
        } catch (IOException e) {
            throw new ExcepcionValidacion(Map.of(
                    "archivo",
                    "No se pudo leer el archivo"
            ));
        }
    }

    private TipoDetectado detectar(byte[] cabecera) {
        if (coincide(cabecera, 0, 0xFF, 0xD8, 0xFF)) {
            return new TipoDetectado("image/jpeg", "jpg", "image", true);
        }

        if (coincide(cabecera, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return new TipoDetectado("image/png", "png", "image", true);
        }

        if (coincide(cabecera, 0, 0x52, 0x49, 0x46, 0x46)
                && coincide(cabecera, 8, 0x57, 0x45, 0x42, 0x50)) {
            return new TipoDetectado("image/webp", "webp", "image", true);
        }

        if (coincide(cabecera, 0, 0x25, 0x50, 0x44, 0x46, 0x2D)) {
            return new TipoDetectado("application/pdf", "pdf", "raw", false);
        }

        return null;
    }

    private boolean coincide(byte[] datos, int desde, int... valores) {
        if (datos.length < desde + valores.length) {
            return false;
        }

        for (int i = 0; i < valores.length; i++) {
            if ((datos[desde + i] & 0xFF) != valores[i]) {
                return false;
            }
        }

        return true;
    }

    private String nombreSeguro(String nombreOriginal, String extension) {
        String nombre = nombreOriginal == null ? "" : nombreOriginal;
        nombre = nombre.replace('\\', '/');
        int ultimaBarra = nombre.lastIndexOf('/');
        if (ultimaBarra >= 0) {
            nombre = nombre.substring(ultimaBarra + 1);
        }
        nombre = nombre.replaceAll("\\p{Cntrl}", "").strip();

        if (nombre.isBlank()) {
            nombre = "archivo." + extension;
        }

        return nombre.length() > 255 ? nombre.substring(0, 255) : nombre;
    }

    public record ArchivoValidado(
            String tipoMime,
            String extension,
            String tipoRecurso,
            boolean imagen,
            String nombreOriginal
    ) {
    }

    private record TipoDetectado(
            String tipoMime,
            String extension,
            String tipoRecurso,
            boolean imagen
    ) {
    }
}
