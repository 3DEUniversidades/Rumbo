package com.rumbo.servicio;

import com.rumbo.dto.LoginResponseDTO;
import com.rumbo.dto.RegistroRequestDTO;
import com.rumbo.dto.RegistroResponseDTO;
import com.rumbo.modelo.EstadoVerificacion;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.SesionUsuario;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.time.Instant;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ValidadorDatos validadorDatos;
    private final PasswordEncoder codificadorContrasenia;

    public UsuarioService(UsuarioRepository usuarioRepository, ValidadorDatos validadorDatos,
                          PasswordEncoder codificadorContrasenia) {
        this.usuarioRepository = usuarioRepository;
        this.validadorDatos = validadorDatos;
        this.codificadorContrasenia = codificadorContrasenia;
    }

    public LoginResponseDTO autenticar(String correo, String contraseniaIngresada) {
        SesionUsuario sesion = crearSesion(correo, contraseniaIngresada);
        return new LoginResponseDTO(
                sesion.getIdUsuario(),
                sesion.getNombreCompleto(),
                sesion.getRol().name()
        );
    }

    public SesionUsuario crearSesion(String correo, String contraseniaIngresada) {
        Map<String, String> errores = validadorDatos.validarLogin(correo, contraseniaIngresada);
        if (!errores.isEmpty()) {
            throw new ExcepcionValidacion(errores);
        }

        String correoNormalizado = validadorDatos.normalizarCorreo(correo);
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correoNormalizado)
                .orElseThrow(() -> new ExcepcionAutenticacion("Credenciales incorrectas"));

        if (!codificadorContrasenia.matches(contraseniaIngresada, usuario.getContrasenia())) {
            throw new ExcepcionAutenticacion("Credenciales incorrectas");
        }

        return new SesionUsuario(
                usuario.getIdUsuario(),
                usuario.getNombreCompleto(),
                usuario.getRol(),
                Instant.now().toString()
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
        Usuario usuario = new Usuario(
                UUID.randomUUID().toString(),
                solicitud.getNombreUsuario(),
                solicitud.getNombreCompleto(),
                solicitud.getCorreo(),
                codificadorContrasenia.encode(solicitud.getContrasenia()),
                rol,
                EstadoVerificacion.NO_VERIFICADO
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
