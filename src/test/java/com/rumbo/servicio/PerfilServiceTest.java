package com.rumbo.servicio;

import com.rumbo.dto.PerfilRequestDTO;
import com.rumbo.dto.PerfilResponseDTO;
import com.rumbo.dto.RegistroRequestDTO;
import com.rumbo.modelo.Carrera;
import com.rumbo.modelo.Perfil;
import com.rumbo.persistencia.CarreraRepository;
import com.rumbo.persistencia.PerfilRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PerfilServiceTest {

    @Autowired
    private PerfilService perfilService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CarreraRepository carreraRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Test
    void creaPerfilVacioAlRegistrarEstudianteYPermiteActualizarlo() {
        String idUsuario = usuarioService.registrar(registro("estudiante", "ESTUDIANTE"))
                .getIdUsuario();
        Carrera carrera = carreraRepository.save(new Carrera(
                "Ingenieria", 5, BigDecimal.ONE));

        PerfilResponseDTO vacio = perfilService.consultar(idUsuario);
        assertEquals("ESTUDIANTE", vacio.getRol());
        assertEquals("estudiante", vacio.getNombreUsuario());
        assertEquals("estudiante@correo.com", vacio.getCorreo());
        assertEquals("Persona de prueba", vacio.getNombreCompleto());
        assertNotNull(perfilRepository.findByUsuarioIdUsuario(idUsuario).orElse(null));

        PerfilRequestDTO solicitud = solicitud(carrera.getId());
        PerfilResponseDTO respuesta = perfilService.actualizar(idUsuario, solicitud);

        assertEquals("Bachillerato", respuesta.getNivelEstudios());
        assertEquals(carrera.getId(), respuesta.getCarreraId());
        assertEquals("Descripcion personal", respuesta.getDescripcionPersonal());
    }

    @Test
    void exigeAreaLaboralParaColaborador() {
        String idUsuario = usuarioService.registrar(registro("colaborador", "COLABORADOR"))
                .getIdUsuario();
        Carrera carrera = carreraRepository.save(new Carrera(
                "Administracion", 4, BigDecimal.ONE));
        PerfilRequestDTO solicitud = solicitud(carrera.getId());

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> perfilService.actualizar(idUsuario, solicitud));

        assertEquals("El area laboral es obligatoria",
                excepcion.getErrores().get("areaLaboral"));
    }

    private PerfilRequestDTO solicitud(Long carreraId) {
        PerfilRequestDTO solicitud = new PerfilRequestDTO();
        solicitud.setNivelEstudios("Bachillerato");
        solicitud.setCarreraId(carreraId);
        solicitud.setDescripcionPersonal("Descripcion personal");
        return solicitud;
    }

    private RegistroRequestDTO registro(String nombreUsuario, String rol) {
        RegistroRequestDTO registro = new RegistroRequestDTO();
        registro.setNombreUsuario(nombreUsuario);
        registro.setNombreCompleto("Persona de prueba");
        String dominio = "COLABORADOR".equals(rol) ? "uvg.edu.gt" : "correo.com";
        registro.setCorreo(nombreUsuario + "@" + dominio);
        registro.setContrasenia("contrasenia123");
        registro.setRol(rol);
        return registro;
    }
}
