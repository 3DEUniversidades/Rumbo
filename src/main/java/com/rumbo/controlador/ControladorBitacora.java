package com.rumbo.controlador;

import com.rumbo.dto.CrearEntradaBitacoraSolicitud;
import com.rumbo.dto.EditarEntradaBitacoraSolicitud;
import com.rumbo.dto.EntradaBitacoraRespuesta;
import com.rumbo.dto.RelacionarActividadSolicitud;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.servicio.BitacoraService;
import com.rumbo.servicio.GestorPermisos;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
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
