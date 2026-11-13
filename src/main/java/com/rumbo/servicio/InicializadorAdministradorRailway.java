package com.rumbo.servicio;

import com.rumbo.modelo.EstadoVerificacion;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("railway")
public class InicializadorAdministradorRailway implements ApplicationRunner {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final ValidadorDatos validador;
    private final String correo;
    private final String contrasenia;
    private final String nombreUsuario;

    public InicializadorAdministradorRailway(
            UsuarioRepository usuarios, PasswordEncoder codificador, ValidadorDatos validador,
            @Value("${rumbo.admin-inicial.correo:}") String correo,
            @Value("${rumbo.admin-inicial.contrasenia:}") String contrasenia,
            @Value("${rumbo.admin-inicial.usuario:administrador}") String nombreUsuario) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.validador = validador;
        this.correo = correo;
        this.contrasenia = contrasenia;
        this.nombreUsuario = nombreUsuario;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        // Los reinicios no cambian contrasenas ni crean mas administradores.
        if (usuarios.existsByRolAndEliminadoFalse(Rol.ADMIN)) {
            return;
        }

        String correoNormalizado = validador.normalizarCorreo(correo);
        String usuarioNormalizado = validador.normalizar(nombreUsuario);
        if (!validador.validarCorreo(correoNormalizado) || correoNormalizado.length() > 255
                || !validador.validarCampoObligatorio(usuarioNormalizado) || usuarioNormalizado.length() > 255) {
            throw new IllegalStateException("Configura RUMBO_ADMIN_CORREO y RUMBO_ADMIN_USUARIO validos antes del primer despliegue.");
        }
        if (contrasenia == null || contrasenia.isBlank() || contrasenia.length() < 12
                || contrasenia.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Configura RUMBO_ADMIN_CONTRASENIA con al menos 12 caracteres y hasta 72 bytes UTF-8.");
        }
        if (usuarios.existsByCorreoIgnoreCase(correoNormalizado)
                || usuarios.existsByNombreUsuarioIgnoreCase(usuarioNormalizado)) {
            throw new IllegalStateException("El administrador inicial coincide con otra cuenta. Configura un correo y un usuario distintos.");
        }

        usuarios.save(new Usuario(UUID.randomUUID().toString(), usuarioNormalizado,
                "Administrador de Rumbo", correoNormalizado, codificador.encode(contrasenia),
                Rol.ADMIN, EstadoVerificacion.VERIFICADO));
    }
}
