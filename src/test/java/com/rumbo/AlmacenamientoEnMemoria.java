package com.rumbo;

import com.rumbo.servicio.AlmacenamientoArchivos;
import com.rumbo.servicio.ArchivoAlmacenado;
import com.rumbo.servicio.ExcepcionAlmacenamiento;
import com.rumbo.servicio.RecursoDescargado;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AlmacenamientoEnMemoria implements AlmacenamientoArchivos {

    private final Map<String, ArchivoEnMemoria> archivos =
            new ConcurrentHashMap<>();

    @Override
    public ArchivoAlmacenado subir(
            MultipartFile archivo,
            String carpeta,
            String publicId,
            String tipoRecurso,
            String tipoEntrega
    ) {
        try {
            byte[] contenido = archivo.getBytes();
            String tipoMime = archivo.getContentType() == null
                    ? "application/octet-stream"
                    : archivo.getContentType();

            archivos.put(publicId, new ArchivoEnMemoria(contenido, tipoMime));

            return new ArchivoAlmacenado(
                    publicId,
                    tipoRecurso,
                    tipoEntrega,
                    extension(archivo.getOriginalFilename()),
                    archivo.getOriginalFilename(),
                    tipoMime,
                    contenido.length
            );
        } catch (IOException e) {
            throw new ExcepcionAlmacenamiento("No se pudo guardar el archivo", e);
        }
    }

    @Override
    public void eliminar(String publicId, String tipoRecurso, String tipoEntrega) {
        archivos.remove(publicId);
    }

    @Override
    public String urlPublica(String publicId) {
        return "https://almacenamiento.local/publico/" + publicId;
    }

    @Override
    public String urlFirmada(String publicId, String tipoRecurso) {
        return "https://almacenamiento.local/firmado/" + publicId;
    }

    @Override
    public RecursoDescargado descargar(String publicId, String tipoRecurso) {
        ArchivoEnMemoria archivo = archivos.get(publicId);
        if (archivo == null) {
            throw new ExcepcionAlmacenamiento("El archivo no existe");
        }

        return new RecursoDescargado(
                new ByteArrayInputStream(archivo.contenido()),
                archivo.contenido().length,
                archivo.tipoMime()
        );
    }

    public boolean contiene(String publicId) {
        return archivos.containsKey(publicId);
    }

    private String extension(String nombre) {
        if (nombre == null) {
            return "";
        }
        int punto = nombre.lastIndexOf('.');
        return punto >= 0 ? nombre.substring(punto + 1) : "";
    }

    private record ArchivoEnMemoria(byte[] contenido, String tipoMime) {
    }
}
