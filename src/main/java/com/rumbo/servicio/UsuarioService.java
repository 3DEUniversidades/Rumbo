package com.rumbo.servicio;

import com.rumbo.dto.LoginResponseDTO;
import com.rumbo.dto.RegistroRequestDTO;
import com.rumbo.dto.RegistroResponseDTO;
import com.rumbo.dto.SesionUsuario;
import com.rumbo.modelo.EstadoVerificacion;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ValidadorDatos validadorDatos;
    private final BCryptPasswordEncoder codificadorContrasenia = new BCryptPasswordEncoder();

    public UsuarioService(UsuarioRepository usuarioRepository, ValidadorDatos validadorDatos) {
        this.usuarioRepository = usuarioRepository;
        this.validadorDatos = validadorDatos;
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
                usuario.getRol().name()
        );
    }

    public RegistroResponseDTO registrar(RegistroRequestDTO solicitud) {
        validadorDatos.normalizarRegistro(solicitud);

        Map<String, String> errores = validadorDatos.validarRegistro(solicitud);
        if (!errores.isEmpty()) {
            throw new ExcepcionValidacion(errores);
        }

        if (usuarioRepository.existsByNombreUsuarioIgnoreCase(solicitud.getNombreUsuario())) {
            throw new ExcepcionConflicto("El nombre de usuario ya esta en uso");
        }

        if (usuarioRepository.existsByCorreoIgnoreCase(solicitud.getCorreo())) {
            throw new ExcepcionConflicto("El correo electronico ya esta en uso");
        }

        Rol rol = Rol.desdeTexto(solicitud.getRol());
        EstadoVerificacion estado = rol == Rol.COLABORADOR
                ? EstadoVerificacion.VERIFICADO
                : EstadoVerificacion.NO_VERIFICADO;

        Usuario usuario = new Usuario(
                UUID.randomUUID().toString(),
                solicitud.getNombreUsuario(),
                solicitud.getNombreCompleto(),
                solicitud.getCorreo(),
                codificadorContrasenia.encode(solicitud.getContrasenia()),
                rol,
                estado
        );

        try {
            usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            throw new ExcepcionConflicto("El nombre de usuario o el correo ya estan en uso");
        }

        return new RegistroResponseDTO(
                usuario.getIdUsuario(),
                usuario.getNombreUsuario(),
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getRol().name(),
                usuario.getEstadoVerificacion().name()
        );
    }
}
