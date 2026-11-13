package com.rumbo.servicio;

import com.rumbo.dto.AdjuntoBitacoraRespuesta;
import com.rumbo.dto.EntradaBitacoraRespuesta;
import com.rumbo.modelo.EstadoVerificacion;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExportacionBitacoraServiceTest {

    @Test
    void descargaInterrumpidaCierraElAdjuntoYRechazaElZip() throws Exception {
        AtomicBoolean cerrado = new AtomicBoolean();
        InputStream contenido = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("Descarga interrumpida"); }
            @Override public void close() { cerrado.set(true); }
        };
        var servicio = servicioConAdjunto(contenido, 50);

        assertThrows(ExcepcionAlmacenamiento.class, () -> servicio.exportarZip("dueno"));
        assertTrue(cerrado.get());
    }

    @Test
    void contenidoTruncadoNoSeEntregaComoUnaExportacionCorrecta() throws Exception {
        AtomicBoolean cerrado = new AtomicBoolean();
        InputStream contenido = new ByteArrayInputStream(new byte[]{1, 2, 3}) {
            @Override public void close() { cerrado.set(true); }
        };
        var servicio = servicioConAdjunto(contenido, 20);

        assertThrows(ExcepcionAlmacenamiento.class, () -> servicio.exportarZip("dueno"));
        assertTrue(cerrado.get());
    }

    private ExportacionBitacoraService servicioConAdjunto(InputStream contenido, long tamano) throws Exception {
        var bitacora = mock(BitacoraService.class);
        var usuarios = mock(UsuarioRepository.class);
        var pdf = mock(GeneradorPdfBitacora.class);
        var usuario = new Usuario("dueno", "estudiante", "Estudiante", "estudiante@example.com",
                "hash-ficticio", Rol.ESTUDIANTE, EstadoVerificacion.NO_VERIFICADO);
        when(usuarios.findById("dueno")).thenReturn(Optional.of(usuario));
        when(bitacora.listar("dueno")).thenReturn(List.of(new EntradaBitacoraRespuesta(
                "entrada", "Reflexión", LocalDateTime.now(), null, null, null,
                List.of(new AdjuntoBitacoraRespuesta("adjunto", "documento.pdf", "application/pdf",
                        tamano, false, "/contenido")))));
        when(bitacora.obtenerContenidoAdjunto("dueno", "entrada", "adjunto"))
                .thenReturn(new BitacoraService.DescargaAdjunto("documento.pdf", "application/pdf", tamano,
                        new RecursoDescargado(contenido, tamano, "application/pdf")));
        doAnswer(invocacion -> {
            ((java.io.OutputStream) invocacion.getArgument(0)).write(new byte[]{1});
            return null;
        }).when(pdf).escribir(any(), anyString(), anyList());
        return new ExportacionBitacoraService(bitacora, usuarios, pdf);
    }
}
