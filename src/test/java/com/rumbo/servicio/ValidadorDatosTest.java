package com.rumbo.servicio;

import com.rumbo.dto.RegistroRequestDTO;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorDatosTest {

    private final ValidadorDatos validador = new ValidadorDatos("uvg.edu.gt");

    @Test
    void validarCorreoAceptaFormatoValido() {
        assertTrue(validador.validarCorreo("usuario@uvg.edu.gt"));
        assertTrue(validador.validarCorreo("nombre.apellido@dominio.com"));
    }

    @Test
    void validarCorreoRechazaFormatoInvalido() {
        assertFalse(validador.validarCorreo(null));
        assertFalse(validador.validarCorreo(""));
        assertFalse(validador.validarCorreo("usuario"));
        assertFalse(validador.validarCorreo("usuario@"));
        assertFalse(validador.validarCorreo("@uvg.edu.gt"));
        assertFalse(validador.validarCorreo("usuario@dominio"));
    }

    @Test
    void validarCorreoInstitucionalAceptaDominioExacto() {
        assertTrue(validador.validarCorreoInstitucional("usuario@uvg.edu.gt"));
        assertTrue(validador.validarCorreoInstitucional("Usuario@UVG.edu.gt"));
    }

    @Test
    void validarCorreoInstitucionalRechazaOtrosDominios() {
        assertFalse(validador.validarCorreoInstitucional("usuario@otro.com"));
        assertFalse(validador.validarCorreoInstitucional("usuario@uvg.edu.gt.falso.com"));
        assertFalse(validador.validarCorreoInstitucional("usuario@uvg.edu.gt.co"));
        assertFalse(validador.validarCorreoInstitucional("usuario@gmail.com"));
    }

    @Test
    void validarCampoObligatorioRechazaVacioYEspacios() {
        assertFalse(validador.validarCampoObligatorio(null));
        assertFalse(validador.validarCampoObligatorio(""));
        assertFalse(validador.validarCampoObligatorio("   "));
        assertTrue(validador.validarCampoObligatorio("valor"));
        assertTrue(validador.validarCampoObligatorio("  valor  "));
    }

    @Test
    void validarRegistroRechazaContraseniaCorta() {
        RegistroRequestDTO solicitud = solicitudValida();
        solicitud.setContrasenia("corta12");

        Map<String, String> errores = validador.validarRegistro(solicitud);

        assertTrue(errores.containsKey("contrasenia"));
    }

    @Test
    void validarRegistroRechazaCamposObligatoriosVacios() {
        RegistroRequestDTO solicitud = solicitudValida();
        solicitud.setNombreUsuario("   ");
        solicitud.setNombreCompleto("");
        solicitud.setCorreo(null);
        solicitud.setRol(null);

        Map<String, String> errores = validador.validarRegistro(solicitud);

        assertTrue(errores.containsKey("nombreUsuario"));
        assertTrue(errores.containsKey("nombreCompleto"));
        assertTrue(errores.containsKey("correo"));
        assertTrue(errores.containsKey("rol"));
    }

    private RegistroRequestDTO solicitudValida() {
        RegistroRequestDTO solicitud = new RegistroRequestDTO();
        solicitud.setNombreUsuario("juan");
        solicitud.setNombreCompleto("Juan Perez");
        solicitud.setCorreo("juan@uvg.edu.gt");
        solicitud.setContrasenia("contrasenia-valida");
        solicitud.setRol("ESTUDIANTE");
        return solicitud;
    }
}
