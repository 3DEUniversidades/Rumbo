package com.rumbo.controlador;
import com.rumbo.dto.*;
import com.rumbo.modelo.Rol;
import com.rumbo.servicio.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class ControladorAdministracionCuentas {
    private final AdministracionCuentasService servicio;
    private final GestorPermisos permisos;
    public ControladorAdministracionCuentas(AdministracionCuentasService servicio, GestorPermisos permisos) {
        this.servicio = servicio; this.permisos = permisos;
    }
    @DeleteMapping("/usuarios/{idUsuario}")
    public RegistroAdministracionRespuesta eliminar(@PathVariable String idUsuario,
            @RequestBody EliminarCuentaSolicitud solicitud, HttpSession sesion) {
        return servicio.eliminar(permisos.exigirRol(sesion, Rol.ADMIN).getIdUsuario(), idUsuario, solicitud);
    }
    @GetMapping("/registros")
    public PaginaRegistrosAdministracionRespuesta registros(@RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="20") int tamano, HttpSession sesion) {
        return servicio.consultarRegistros(permisos.exigirRol(sesion, Rol.ADMIN).getIdUsuario(), pagina, tamano);
    }
}
