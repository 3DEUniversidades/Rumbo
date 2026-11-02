package com.rumbo.controlador;

import com.rumbo.dto.AdjuntoBitacoraRespuesta;
import com.rumbo.dto.CrearEntradaBitacoraSolicitud;
import com.rumbo.dto.EditarEntradaBitacoraSolicitud;
import com.rumbo.dto.EntradaBitacoraRespuesta;
import com.rumbo.dto.RelacionarActividadSolicitud;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.servicio.BitacoraService;
import com.rumbo.servicio.GestorPermisos;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bitacora")
public class ControladorBitacora {

    private final BitacoraService bitacoraService;
    private final GestorPermisos gestorPermisos;

    public ControladorBitacora(
            BitacoraService bitacoraService,
            GestorPermisos gestorPermisos
    ) {
        this.bitacoraService = bitacoraService;
        this.gestorPermisos = gestorPermisos;
    }

    @GetMapping
    public List<EntradaBitacoraRespuesta> listar(HttpSession sesion) {
        SesionUsuario estudiante =
                gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

        return bitacoraService.listar(estudiante.getIdUsuario());
    }

    @PostMapping
    public ResponseEntity<EntradaBitacoraRespuesta> crear(
            @RequestBody CrearEntradaBitacoraSolicitud solicitud,
            HttpSession sesion
    ) {
        SesionUsuario estudiante =
                gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

        EntradaBitacoraRespuesta entrada = bitacoraService.crear(
                estudiante.getIdUsuario(),
                solicitud.contenido(),
                solicitud.idActividad()
        );

        return ResponseEntity
                .created(URI.create("/api/bitacora/" + entrada.idEntrada()))
                .body(entrada);
    }

    @PutMapping("/{idEntrada}")
    public EntradaBitacoraRespuesta editar(
            @PathVariable String idEntrada,
            @RequestBody EditarEntradaBitacoraSolicitud solicitud,
            HttpSession sesion
    ) {
        SesionUsuario estudiante =
                gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

        return bitacoraService.editar(
                estudiante.getIdUsuario(),
                idEntrada,
                solicitud.contenido()
        );
    }

    @DeleteMapping("/{idEntrada}")
    public ResponseEntity<Map<String, String>> eliminar(
            @PathVariable String idEntrada,
            HttpSession sesion
    ) {
        SesionUsuario estudiante =
                gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

        bitacoraService.eliminar(estudiante.getIdUsuario(), idEntrada);

        return ResponseEntity.ok(Map.of("mensaje", "Entrada eliminada"));
    }

    @PostMapping(
            value = "/{idEntrada}/adjuntos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<AdjuntoBitacoraRespuesta> agregarAdjunto(
            @PathVariable String idEntrada,
            @RequestParam("archivo") MultipartFile archivo,
            HttpSession sesion
    ) {
        SesionUsuario estudiante =
                gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

        AdjuntoBitacoraRespuesta adjunto = bitacoraService.agregarAdjunto(
                estudiante.getIdUsuario(),
                idEntrada,
                archivo
        );

        return ResponseEntity
                .created(URI.create(
                        "/api/bitacora/" + idEntrada
                                + "/adjuntos/" + adjunto.idAdjunto()
                ))
                .body(adjunto);
    }

    @GetMapping("/{idEntrada}/adjuntos")
    public List<AdjuntoBitacoraRespuesta> listarAdjuntos(
            @PathVariable String idEntrada,
            HttpSession sesion
    ) {
        SesionUsuario estudiante =
                gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

        return bitacoraService.listarAdjuntos(
                estudiante.getIdUsuario(),
                idEntrada
        );
    }

    @GetMapping("/{idEntrada}/adjuntos/{idAdjunto}/contenido")
    public ResponseEntity<InputStreamResource> descargarAdjunto(
            @PathVariable String idEntrada,
            @PathVariable String idAdjunto,
            HttpSession sesion
    ) {
        SesionUsuario estudiante =
                gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

        BitacoraService.DescargaAdjunto descarga =
                bitacoraService.obtenerContenidoAdjunto(
                        estudiante.getIdUsuario(),
                        idEntrada,
                        idAdjunto
                );

        InputStreamResource recurso = new InputStreamResource(
                descarga.recurso().contenido()
        );

        ContentDisposition disposicion = ContentDisposition.inline()
                .filename(descarga.nombreOriginal(), StandardCharsets.UTF_8)
                .build();

        ResponseEntity.BodyBuilder respuesta = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(descarga.tipoMime()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposicion.toString()
                )
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff");

        if (descarga.tamanoBytes() >= 0) {
            respuesta.contentLength(descarga.tamanoBytes());
        }

        return respuesta.body(recurso);
    }

    @DeleteMapping("/{idEntrada}/adjuntos/{idAdjunto}")
    public ResponseEntity<Map<String, String>> eliminarAdjunto(
            @PathVariable String idEntrada,
            @PathVariable String idAdjunto,
            HttpSession sesion
    ) {
        SesionUsuario estudiante =
                gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

        bitacoraService.eliminarAdjunto(
                estudiante.getIdUsuario(),
                idEntrada,
                idAdjunto
        );

        return ResponseEntity.ok(Map.of("mensaje", "Adjunto eliminado"));
    }

    @PutMapping("/{idEntrada}/actividad")
    public EntradaBitacoraRespuesta relacionarActividad(
            @PathVariable String idEntrada,
            @RequestBody RelacionarActividadSolicitud solicitud,
            HttpSession sesion
    ) {
        SesionUsuario estudiante =
                gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

        return bitacoraService.relacionarActividad(
                estudiante.getIdUsuario(),
                idEntrada,
                solicitud.idActividad()
        );
    }

    @PostMapping("/actividades/{idActividad}/resultados")
    public ResponseEntity<EntradaBitacoraRespuesta> crearDesdeResultados(
            @PathVariable String idActividad,
            HttpSession sesion
    ) {
        SesionUsuario estudiante =
                gestorPermisos.exigirRol(sesion, Rol.ESTUDIANTE);

        EntradaBitacoraRespuesta entrada =
                bitacoraService.crearDesdeResultados(
                        estudiante.getIdUsuario(),
                        idActividad
                );

        return ResponseEntity
                .created(URI.create("/api/bitacora/" + entrada.idEntrada()))
                .body(entrada);
    }
}
