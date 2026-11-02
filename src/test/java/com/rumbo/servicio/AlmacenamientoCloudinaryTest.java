package com.rumbo.servicio;

import com.cloudinary.Cloudinary;
import com.rumbo.PropiedadesCloudinary;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlmacenamientoCloudinaryTest {

    private final AlmacenamientoCloudinary almacenamiento = crearAlmacenamiento();

    @Test
    void autorizaPdfPorApiPrivadaConIdCompletoYExpiracionCorta() {
        long antes = Instant.now().getEpochSecond();
        String url = almacenamiento.urlFirmada("rumbo/bitacora/entrada/archivo.pdf", "raw");
        long despues = Instant.now().getEpochSecond();
        URI uri = URI.create(url);
        Map<String, String> parametros = Arrays.stream(uri.getRawQuery().split("&"))
                .map(parte -> parte.split("=", 2))
                .collect(Collectors.toMap(
                        parte -> URLDecoder.decode(parte[0], StandardCharsets.UTF_8),
                        parte -> URLDecoder.decode(parte[1], StandardCharsets.UTF_8)));

        assertEquals("https", uri.getScheme());
        assertEquals("api.cloudinary.com", uri.getHost());
        assertEquals("/v1_1/cloud-prueba/raw/download", uri.getPath());
        assertEquals("rumbo/bitacora/entrada/archivo.pdf", parametros.get("public_id"));
        assertEquals("authenticated", parametros.get("type"));
        assertEquals("clave-prueba", parametros.get("api_key"));
        assertTrue(parametros.get("signature").matches("[a-f0-9]{40}"));
        assertFalse(parametros.containsKey("format"));
        assertFalse(url.contains("secreto-prueba"));
        long expira = Long.parseLong(parametros.get("expires_at"));
        assertTrue(expira >= antes + 300 && expira <= despues + 300);
    }

    @Test
    void imagenPrivadaConservaEntregaFirmadaPorCdn() {
        URI uri = URI.create(almacenamiento.urlFirmada("rumbo/bitacora/foto", "image"));

        assertEquals("https", uri.getScheme());
        assertEquals("res.cloudinary.com", uri.getHost());
        assertTrue(uri.getPath().startsWith("/cloud-prueba/image/authenticated/s--"));
        assertTrue(uri.getPath().endsWith("/rumbo/bitacora/foto"));
    }

    private static AlmacenamientoCloudinary crearAlmacenamiento() {
        PropiedadesCloudinary propiedades = new PropiedadesCloudinary();
        propiedades.setCloudName("cloud-prueba");
        propiedades.setApiKey("clave-prueba");
        propiedades.setApiSecret("secreto-prueba");
        Cloudinary cloudinary = new Cloudinary(Map.of(
                "cloud_name", propiedades.getCloudName(),
                "api_key", propiedades.getApiKey(),
                "api_secret", propiedades.getApiSecret(),
                "secure", true));
        return new AlmacenamientoCloudinary(cloudinary, propiedades);
    }
}
