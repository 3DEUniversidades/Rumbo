package com.rumbo.servicio;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.rumbo.PropiedadesCloudinary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class AlmacenamientoCloudinary implements AlmacenamientoArchivos {

    private static final Logger LOG =
            LoggerFactory.getLogger(AlmacenamientoCloudinary.class);

    private static final String ENTREGA_PUBLICA = "upload";
    private static final String ENTREGA_AUTENTICADA = "authenticated";

    private final Cloudinary cloudinary;
    private final PropiedadesCloudinary propiedades;
    private final HttpClient httpClient;

    public AlmacenamientoCloudinary(
            Cloudinary cloudinary,
            PropiedadesCloudinary propiedades
    ) {
        this.cloudinary = cloudinary;
        this.propiedades = propiedades;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public ArchivoAlmacenado subir(
            MultipartFile archivo,
            String carpeta,
            String publicId,
            String tipoRecurso,
            String tipoEntrega
    ) {
        validarConfiguracion();

        try {
            Map<String, Object> opciones = ObjectUtils.asMap(
                    "folder", construirCarpeta(carpeta),
                    "public_id", publicId,
                    "resource_type", tipoRecurso,
                    "type", tipoEntrega,
                    "overwrite", true,
                    "invalidate", true,
                    "use_filename", false,
                    "unique_filename", false
            );

            Map<?, ?> resultado = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    opciones
            );

            String idFinal = valor(resultado, "public_id", publicId);
            String formato = valor(resultado, "format", "");
            long tamano = numero(resultado, "bytes", archivo.getSize());

            return new ArchivoAlmacenado(
                    idFinal,
                    tipoRecurso,
                    tipoEntrega,
                    formato,
                    archivo.getOriginalFilename(),
                    archivo.getContentType(),
                    tamano
            );
        } catch (IOException | RuntimeException e) {
            LOG.warn("No se pudo subir el archivo a Cloudinary", e);
            throw new ExcepcionAlmacenamiento(
                    "No se pudo subir el archivo a Cloudinary",
                    e
            );
        }
    }

    @Override
    public void eliminar(String publicId, String tipoRecurso, String tipoEntrega) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        validarConfiguracion();

        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", tipoRecurso,
                            "type", tipoEntrega,
                            "invalidate", true
                    )
            );
        } catch (IOException | RuntimeException e) {
            LOG.warn("No se pudo eliminar el archivo {} de Cloudinary", publicId, e);
            throw new ExcepcionAlmacenamiento(
                    "No se pudo eliminar el archivo de Cloudinary",
                    e
            );
        }
    }

    @Override
    public String urlPublica(String publicId) {
        validarConfiguracion();

        if (publicId == null || publicId.isBlank()) {
            throw new ExcepcionAlmacenamiento(
                    "El archivo no tiene identificador de Cloudinary"
            );
        }

        Transformation<?> transformacionCloudinary = new Transformation<>()
                .width(320)
                .height(320)
                .crop("fill")
                .gravity("auto")
                .quality("auto")
                .fetchFormat("auto");

        return cloudinary.url()
                .secure(true)
                .resourceType("image")
                .type(ENTREGA_PUBLICA)
                .publicId(publicId)
                .transformation(transformacionCloudinary)
                .generate();
    }

    @Override
    public String urlFirmada(String publicId, String tipoRecurso) {
        validarConfiguracion();

        if (publicId == null || publicId.isBlank()) {
            throw new ExcepcionAlmacenamiento(
                    "El archivo no tiene identificador de Cloudinary"
            );
        }

        if ("raw".equals(tipoRecurso)) {
            try {
                // Los PDF pueden subirse aunque su entrega por CDN esté bloqueada.
                // El public_id raw ya incluye la extensión: no se añade un formato.
                return cloudinary.privateDownload(publicId, null, Map.of(
                        "resource_type", tipoRecurso,
                        "type", ENTREGA_AUTENTICADA,
                        "expires_at", Instant.now().plusSeconds(300).getEpochSecond()
                ));
            } catch (Exception e) {
                throw new ExcepcionAlmacenamiento(
                        "No se pudo autorizar la descarga privada de Cloudinary",
                        e
                );
            }
        }

        return cloudinary.url()
                .secure(true)
                .resourceType(tipoRecurso)
                .type(ENTREGA_AUTENTICADA)
                .publicId(publicId)
                .signed(true)
                .generate();
    }

    @Override
    public RecursoDescargado descargar(String publicId, String tipoRecurso) {
        String url = urlFirmada(publicId, tipoRecurso);

        try {
            HttpRequest solicitud = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<InputStream> respuesta = httpClient.send(
                    solicitud,
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
                respuesta.body().close();
                throw new ExcepcionAlmacenamiento(
                        "Cloudinary respondió con estado " + respuesta.statusCode()
                );
            }

            long tamano = respuesta.headers()
                    .firstValueAsLong("content-length")
                    .orElse(-1L);
            String tipoMime = respuesta.headers()
                    .firstValue("content-type")
                    .orElse("application/octet-stream");

            return new RecursoDescargado(respuesta.body(), tamano, tipoMime);
        } catch (IOException e) {
            throw new ExcepcionAlmacenamiento(
                    "No se pudo descargar el archivo desde Cloudinary",
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExcepcionAlmacenamiento(
                    "La descarga del archivo fue interrumpida",
                    e
            );
        }
    }

    private String construirCarpeta(String carpeta) {
        String base = limpiarSegmento(propiedades.getCarpetaBase());
        String relativa = limpiarSegmento(carpeta);

        if (base.isBlank()) {
            return relativa;
        }
        if (relativa.isBlank()) {
            return base;
        }
        return base + "/" + relativa;
    }

    private String limpiarSegmento(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replaceAll("^/+|/+$", "");
    }

    private void validarConfiguracion() {
        if (!propiedades.estaConfigurado()) {
            throw new ExcepcionAlmacenamiento(
                    "El almacenamiento de archivos no está configurado"
            );
        }
    }

    private String valor(Map<?, ?> resultado, String clave, String porDefecto) {
        Object valor = resultado.get(clave);
        return valor == null ? porDefecto : valor.toString();
    }

    private long numero(Map<?, ?> resultado, String clave, long porDefecto) {
        Object valor = resultado.get(clave);
        return valor instanceof Number numero ? numero.longValue() : porDefecto;
    }
}
