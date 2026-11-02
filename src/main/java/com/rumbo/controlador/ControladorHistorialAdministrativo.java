package com.rumbo.controlador;
import com.rumbo.dto.*;
import com.rumbo.modelo.Rol;
import com.rumbo.servicio.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/historial")
public class ControladorHistorialAdministrativo {
    private final HistorialAdministrativoService servicio;
    private final GestorPermisos permisos;
    public ControladorHistorialAdministrativo(HistorialAdministrativoService servicio,GestorPermisos permisos) {
        this.servicio=servicio;this.permisos=permisos;
    }
    @GetMapping
    public PaginaHistorialAdministrativoRespuesta consultar(
            @RequestParam(defaultValue="ADMINISTRACION") OrigenHistorialAdministrativo origen,
            @RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamano,HttpSession sesion) {
        return servicio.consultar(permisos.exigirRol(sesion,Rol.ADMIN).getIdUsuario(),origen,pagina,tamano);
    }
}
