package com.rumbo.servicio;

import com.rumbo.modelo.EstadoVerificacion;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InicializadorAdministradorRailwayTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder(4);
    private final ValidadorDatos validador = new ValidadorDatos("uvg.edu.gt");

    private InicializadorAdministradorRailway inicializador(String correo, String clave, String usuario) {
        return new InicializadorAdministradorRailway(usuarios, codificador, validador, correo, clave, usuario);
    }

    @Test
    void creaElPrimerAdministradorConBcryptYDatosNormalizados() {
        String clave = "ClaveFicticiaSoloParaTest!";
        inicializador("  Admin@ejemplo.test  ", clave, "  administrador  ").run(null);

        var captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        Usuario creado = captor.getValue();
        assertEquals("admin@ejemplo.test", creado.getCorreo());
        assertEquals("administrador", creado.getNombreUsuario());
        assertEquals(Rol.ADMIN, creado.getRol());
        assertEquals(EstadoVerificacion.VERIFICADO, creado.getEstadoVerificacion());
        assertNotEquals("usr-0001", creado.getIdUsuario());
        assertNotEquals(clave, creado.getContrasenia());
        assertTrue(codificador.matches(clave, creado.getContrasenia()));
    }

    @Test
    void unReinicioConAdministradorExistenteNoModificaCuentasNiExigeCredenciales() {
        when(usuarios.existsByRolAndEliminadoFalse(Rol.ADMIN)).thenReturn(true);

        inicializador("", "", "").run(null);

        verify(usuarios).existsByRolAndEliminadoFalse(Rol.ADMIN);
        verifyNoMoreInteractions(usuarios);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"corta", "            "})
    void rechazaUnaClaveAusenteOCortaSinPersistirla(String clave) {
        assertThrows(IllegalStateException.class,
                () -> inicializador("admin@ejemplo.test", clave, "administrador").run(null));
        verify(usuarios, never()).save(any());
    }

    @Test
    void validaElLimiteDeBcryptEnBytesSinExponerLaClaveEnElError() {
        String clave = "á".repeat(37);
        var error = assertThrows(IllegalStateException.class,
                () -> inicializador("admin@ejemplo.test", clave, "administrador").run(null));

        assertFalse(error.getMessage().contains(clave));
        verify(usuarios, never()).save(any());
    }

    @Test
    void rechazaUnCorreoInvalido() {
        assertThrows(IllegalStateException.class,
                () -> inicializador("sin-arroba", "ClaveFicticiaSoloParaTest!", "administrador").run(null));
        verify(usuarios, never()).save(any());
    }

    @Test
    void noConvierteUnaCuentaExistenteEnAdministradorPorSuCorreo() {
        when(usuarios.existsByCorreoIgnoreCase("estudiante@ejemplo.test")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> inicializador("estudiante@ejemplo.test", "ClaveFicticiaSoloParaTest!", "administrador").run(null));
        verify(usuarios, never()).save(any());
    }

    @Test
    void noReutilizaElNombreDeOtraCuenta() {
        when(usuarios.existsByNombreUsuarioIgnoreCase("estudiante")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> inicializador("admin@ejemplo.test", "ClaveFicticiaSoloParaTest!", "estudiante").run(null));
        verify(usuarios, never()).save(any());
    }
}
