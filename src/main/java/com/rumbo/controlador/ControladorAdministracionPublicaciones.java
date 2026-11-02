package com.rumbo.controlador;
import com.rumbo.dto.*;
import com.rumbo.modelo.Rol;
import com.rumbo.servicio.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/publicaciones")
public class ControladorAdministracionPublicaciones {
    private final AdministracionPublicacionesService servicio;
    private final GestorPermisos permisos;
    public ControladorAdministracionPublicaciones(AdministracionPublicacionesService servicio, GestorPermisos permisos) {
        this.servicio = servicio; this.permisos = permisos;
    }
    @GetMapping
    public PaginaPublicacionesAdministracionRespuesta listar(@RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="20") int tamano, HttpSession sesion) {
        return servicio.listar(permisos.exigirRol(sesion, Rol.ADMIN).getIdUsuario(), pagina, tamano);
    }
    @DeleteMapping("/{id}")
    public RegistroAdministracionRespuesta eliminar(@PathVariable Long id,
            @RequestBody EliminarPublicacionAdministracionSolicitud solicitud, HttpSession sesion) {
        return servicio.eliminar(permisos.exigirRol(sesion, Rol.ADMIN).getIdUsuario(), id, solicitud);
    }
}
