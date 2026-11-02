package com.rumbo.controlador;
import com.rumbo.dto.PaginaDecisionesSupervisorRespuesta;
import com.rumbo.modelo.Rol;
import com.rumbo.servicio.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil/mis-decisiones")
public class ControladorHistorialSupervisor {
    private final HistorialSupervisorService servicio;
    private final GestorPermisos permisos;
    public ControladorHistorialSupervisor(HistorialSupervisorService servicio,GestorPermisos permisos){this.servicio=servicio;this.permisos=permisos;}
    @GetMapping
    public PaginaDecisionesSupervisorRespuesta consultar(@RequestParam(defaultValue="TODAS") String tipo,
            @RequestParam(defaultValue="TODAS") String decision,@RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="20") int tamano,HttpSession sesion) {
        return servicio.consultar(permisos.exigirRol(sesion,Rol.SUPERVISOR_DE_USUARIOS).getIdUsuario(),tipo,decision,pagina,tamano);
    }
}
