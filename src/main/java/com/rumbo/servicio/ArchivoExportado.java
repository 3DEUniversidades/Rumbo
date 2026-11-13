package com.rumbo.servicio;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public record ArchivoExportado(Path ruta, String nombre, String tipoMime) implements AutoCloseable {

    public long tamanoBytes() throws IOException {
        return Files.size(ruta);
    }

    public InputStream abrirContenido() throws IOException {
        return new FilterInputStream(Files.newInputStream(ruta)) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    ArchivoExportado.this.close();
                }
            }
        };
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(ruta);
    }
}
