package com.rumbo.controlador;

import com.rumbo.modelo.Rol;
import com.rumbo.servicio.ArchivoExportado;
import com.rumbo.servicio.ExcepcionExportacionBitacora;
import com.rumbo.servicio.ExportacionBitacoraService;
import com.rumbo.servicio.GestorPermisos;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/bitacora/exportar")
public class ControladorExportacionBitacora {

    private final ExportacionBitacoraService exportacion;
    private final GestorPermisos permisos;

    public ControladorExportacionBitacora(ExportacionBitacoraService exportacion, GestorPermisos permisos) {
        this.exportacion = exportacion;
        this.permisos = permisos;
    }

    @GetMapping("/pdf")
    public ResponseEntity<InputStreamResource> descargarPdf(HttpSession sesion) {
        var estudiante = permisos.exigirRol(sesion, Rol.ESTUDIANTE);
        return descargar(exportacion.exportarPdf(estudiante.getIdUsuario()));
    }

    @GetMapping("/zip")
    public ResponseEntity<InputStreamResource> descargarZip(HttpSession sesion) {
        var estudiante = permisos.exigirRol(sesion, Rol.ESTUDIANTE);
        return descargar(exportacion.exportarZip(estudiante.getIdUsuario()));
    }

    private ResponseEntity<InputStreamResource> descargar(ArchivoExportado archivo) {
        try {
            long tamano = archivo.tamanoBytes();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(archivo.tipoMime()))
                    .contentLength(tamano)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                    .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(new InputStreamResource(archivo.abrirContenido()));
        } catch (IOException e) {
            try {
                archivo.close();
            } catch (IOException limpieza) {
                e.addSuppressed(limpieza);
            }
            throw new ExcepcionExportacionBitacora("No se pudo preparar la descarga de la bitácora", e);
        }
    }
}
