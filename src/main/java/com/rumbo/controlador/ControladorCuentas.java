package com.rumbo.controlador;

import com.rumbo.dto.LoginRequestDTO;
import com.rumbo.dto.LoginResponseDTO;
import com.rumbo.servicio.ExcepcionAutenticacion;
import com.rumbo.servicio.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ControladorCuentas {

    private final UsuarioService usuarioService;

    public ControladorCuentas(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Reemplaza a iniciarSesion() del diagrama de Fase 2 — ahora usa DTOs tipados en vez de Map<String,String>
    @PostMapping("/login")
    public ResponseEntity<?> iniciarSesion(@RequestBody LoginRequestDTO credenciales) {
        try {
            LoginResponseDTO respuesta = usuarioService.autenticar(
                    credenciales.getCorreo(),
                    credenciales.getContrasenia()
            );
            return ResponseEntity.ok(respuesta);
        } catch (ExcepcionAutenticacion e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Credenciales incorrectas"));
        }
    }
}
