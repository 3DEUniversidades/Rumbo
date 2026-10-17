package com.rumbo.controlador;

import com.rumbo.dto.LoginRequestDTO;
import com.rumbo.dto.LoginResponseDTO;
import com.rumbo.dto.RegistroRequestDTO;
import com.rumbo.dto.RegistroResponseDTO;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.servicio.GestorPermisos;
import jakarta.servlet.http.HttpSession;
import com.rumbo.servicio.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class ControladorCuentas {

    private final UsuarioService usuarioService;

    private final GestorPermisos gestorPermisos;

    public ControladorCuentas(UsuarioService usuarioService, GestorPermisos gestorPermisos) {
        this.usuarioService = usuarioService;
        this.gestorPermisos = gestorPermisos;
    }

    // Reemplaza a iniciarSesion() del diagrama de Fase 2 — ahora usa DTOs tipados en vez de Map<String,String>
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> iniciarSesion(
            @RequestBody LoginRequestDTO credenciales, HttpSession sesion) {
        SesionUsuario sesionUsuario = usuarioService.crearSesion(
                credenciales.getCorreo(),
                credenciales.getContrasenia()
        );
        sesion.setAttribute(GestorPermisos.ATRIBUTO_SESION, sesionUsuario);
        return ResponseEntity.ok(new LoginResponseDTO(
                sesionUsuario.getIdUsuario(),
                sesionUsuario.getNombreCompleto(),
                sesionUsuario.getRol().name()
        ));
    }

    @GetMapping("/sesion")
    public ResponseEntity<SesionUsuario> consultarSesion(HttpSession sesion) {
        return ResponseEntity.ok(gestorPermisos.exigirSesion(sesion));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion(HttpSession sesion) {
        sesion.invalidate();
        return ResponseEntity.noContent().build();
    }

    // RF-01, RF-02, RF-03, RF-04, RF-09, RF-11, RF-16 — registro publico de estudiante o colaborador
    @PostMapping("/registro")
    public ResponseEntity<RegistroResponseDTO> registrar(@RequestBody RegistroRequestDTO solicitud) {
        RegistroResponseDTO respuesta = usuarioService.registrar(solicitud);
        return ResponseEntity.status(201).body(respuesta);
    }
}
