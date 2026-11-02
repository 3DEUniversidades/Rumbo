package com.rumbo.servicio;

import org.springframework.web.multipart.MultipartFile;

public interface AlmacenamientoArchivos {

    ArchivoAlmacenado subir(
            MultipartFile archivo,
            String carpeta,
            String publicId,
            String tipoRecurso,
            String tipoEntrega
    );

    void eliminar(String publicId, String tipoRecurso, String tipoEntrega);

    String urlPublica(String publicId);

    String urlFirmada(String publicId, String tipoRecurso);

    RecursoDescargado descargar(String publicId, String tipoRecurso);
}
