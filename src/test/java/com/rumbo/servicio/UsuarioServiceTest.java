package com.rumbo.servicio;

import com.rumbo.dto.RegistroRequestDTO;
import com.rumbo.dto.RegistroResponseDTO;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registraEstudianteComoNoVerificado() {
        RegistroResponseDTO respuesta = usuarioService.registrar(
                solicitud("juan", "Juan Perez", "juan@correo.com", "contrasenia123", "ESTUDIANTE"));

        assertEquals("ESTUDIANTE", respuesta.getRol());
        assertEquals("NO_VERIFICADO", respuesta.getEstadoVerificacion());

        Usuario guardado = usuarioRepository.findById(respuesta.getIdUsuario()).orElseThrow();
        assertTrue(passwordEncoder.matches("contrasenia123", guardado.getContrasenia()));
    }

    @Test
    void registraColaboradorConCorreoInstitucionalComoNoVerificado() {
        RegistroResponseDTO respuesta = usuarioService.registrar(
                solicitud("colab", "Colaborador UVG", "colab@uvg.edu.gt", "contrasenia123", "COLABORADOR"));

        assertEquals("COLABORADOR", respuesta.getRol());
        assertEquals("NO_VERIFICADO", respuesta.getEstadoVerificacion());
    }

    @Test
    void rechazaColaboradorConCorreoNoInstitucional() {
        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class, () ->
                usuarioService.registrar(
                        solicitud("colab", "Colaborador", "colab@otro.com", "contrasenia123", "COLABORADOR")));

        assertTrue(excepcion.getErrores().containsKey("correo"));
    }

    @Test
    void rechazaRolAdministrativo() {
        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class, () ->
                usuarioService.registrar(
                        solicitud("admin2", "Admin", "admin2@uvg.edu.gt", "contrasenia123", "ADMINISTRADOR")));

        assertTrue(excepcion.getErrores().containsKey("rol"));
    }

    @Test
    void rechazaRolInexistente() {
        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class, () ->
                usuarioService.registrar(
                        solicitud("gerente", "Gerente", "gerente@uvg.edu.gt", "contrasenia123", "GERENTE")));

        assertTrue(excepcion.getErrores().containsKey("rol"));
    }

    @Test
    void rechazaNombreDeUsuarioDuplicado() {
        usuarioService.registrar(solicitud("juan", "Juan Perez", "juan@correo.com", "contrasenia123", "ESTUDIANTE"));

        ExcepcionConflicto excepcion = assertThrows(ExcepcionConflicto.class, () ->
                usuarioService.registrar(
                        solicitud("JUAN", "Juan Otro", "juan2@correo.com", "contrasenia123", "ESTUDIANTE")));

        assertTrue(excepcion.getMessage().toLowerCase().contains("usuario"));
    }

    @Test
    void rechazaCorreoDuplicadoSinDistinguirMayusculas() {
        usuarioService.registrar(solicitud("juan", "Juan Perez", "juan@correo.com", "contrasenia123", "ESTUDIANTE"));

        ExcepcionConflicto excepcion = assertThrows(ExcepcionConflicto.class, () ->
                usuarioService.registrar(
                        solicitud("juan2", "Juan Otro", "JUAN@Correo.com", "contrasenia123", "ESTUDIANTE")));

        assertTrue(excepcion.getMessage().toLowerCase().contains("correo"));
    }

    @Test
    void respuestaDeRegistroNoExponeLaContrasenia() {
        RegistroResponseDTO respuesta = usuarioService.registrar(
                solicitud("juan", "Juan Perez", "juan@correo.com", "contrasenia123", "ESTUDIANTE"));

        assertFalse(respuesta.toString().contains("contrasenia123"));
        Usuario guardado = usuarioRepository.findById(respuesta.getIdUsuario()).orElseThrow();
        assertTrue(passwordEncoder.matches("contrasenia123", guardado.getContrasenia()));
    }

    private RegistroRequestDTO solicitud(String nombreUsuario, String nombreCompleto,
                                         String correo, String contrasenia, String rol) {
        RegistroRequestDTO solicitud = new RegistroRequestDTO();
        solicitud.setNombreUsuario(nombreUsuario);
        solicitud.setNombreCompleto(nombreCompleto);
        solicitud.setCorreo(correo);
        solicitud.setContrasenia(contrasenia);
        solicitud.setRol(rol);
        return solicitud;
    }
}
