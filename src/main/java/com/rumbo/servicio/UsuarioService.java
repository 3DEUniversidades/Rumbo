package com.rumbo.servicio;

import com.rumbo.dto.LoginResponseDTO;
import com.rumbo.dto.SesionUsuario;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public LoginResponseDTO autenticar(String correo, String contraseniaIngresada) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ExcepcionAutenticacion("Credenciales incorrectas"));

        if (!usuario.verificarContrasenia(contraseniaIngresada)) {
            throw new ExcepcionAutenticacion("Credenciales incorrectas");
        }

        SesionUsuario sesion = new SesionUsuario(
                usuario.getIdUsuario(),
                LocalDateTime.now().toString()
        );

        return new LoginResponseDTO(
                sesion.getIdUsuario(),
                usuario.getNombreCompleto(),
                usuario.getRol()
        );
    }
}
