package com.rumbo.controlador;
import com.rumbo.dto.*;
import com.rumbo.modelo.Rol;
import com.rumbo.servicio.ConsultaUsuariosAdministracionService;
import com.rumbo.servicio.GestorPermisos;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/usuarios")
public class ControladorConsultaUsuariosAdministracion {
    private final ConsultaUsuariosAdministracionService servicio;
    private final GestorPermisos permisos;
    public ControladorConsultaUsuariosAdministracion(ConsultaUsuariosAdministracionService servicio,
            GestorPermisos permisos) { this.servicio = servicio; this.permisos = permisos; }
    @GetMapping
    public PaginaUsuariosAdministracionRespuesta listar(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano, HttpSession sesion) {
        return servicio.listar(permisos.exigirRol(sesion, Rol.ADMIN).getIdUsuario(), pagina, tamano);
    }
    @GetMapping("/{idUsuario}")
    public PerfilAdministracionRespuesta consultar(@PathVariable String idUsuario, HttpSession sesion) {
        return servicio.consultar(permisos.exigirRol(sesion, Rol.ADMIN).getIdUsuario(), idUsuario);
    }
}
