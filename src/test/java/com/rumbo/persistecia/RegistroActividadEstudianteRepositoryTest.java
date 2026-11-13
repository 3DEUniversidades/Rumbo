package com.rumbo.persistencia;

import com.rumbo.modelo.Rol;
import com.rumbo.modelo.EstadoVerificacion;
import com.rumbo.modelo.Actividad;
import com.rumbo.modelo.EstadoActividad;
import com.rumbo.modelo.RegistroActividadEstudiante;
import com.rumbo.modelo.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.sql.init.mode=never")
@ActiveProfiles("test")
class RegistroActividadEstudianteRepositoryTest {

    @Autowired
    private RegistroActividadEstudianteRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void conservaAvancesIndependientesParaLaMismaActividad() {
        Usuario estudianteUno = crearEstudiante("est-01", "kevin");
        Usuario estudianteDos = crearEstudiante("est-02", "otro");

        entityManager.persist(estudianteUno);
        entityManager.persist(estudianteDos);

        Actividad actividad = new Actividad(
                "act-test-01",
                "2026-10-16",
                "Explorar carreras",
                "Conocer opciones universitarias",
                "Investiga carreras de tu interés",
                "Consultar tres carreras",
                "Investigación documental",
                "Catálogo de carreras",
                "Anotar tus conclusiones",
                "Exploración"
        );

        entityManager.persist(actividad);

        RegistroActividadEstudiante registroUno =
                new RegistroActividadEstudiante(estudianteUno, actividad);

        registroUno.setDecidioRealizar(true);
        registroUno.setRazonDecision("Quiero conocer mis opciones");
        registroUno.setEstado(EstadoActividad.COMPLETADA);
        registroUno.setResultadosRegistrados("Me interesa Computación");

        RegistroActividadEstudiante registroDos =
                new RegistroActividadEstudiante(estudianteDos, actividad);

        repository.save(registroUno);
        repository.save(registroDos);

        entityManager.flush();
        entityManager.clear();

        RegistroActividadEstudiante recuperadoUno = repository
                .findByUsuario_IdUsuarioAndActividad_IdActividad(
                        "est-01", "act-test-01"
                )
                .orElseThrow();

        RegistroActividadEstudiante recuperadoDos = repository
                .findByUsuario_IdUsuarioAndActividad_IdActividad(
                        "est-02", "act-test-01"
                )
                .orElseThrow();

        assertEquals(EstadoActividad.COMPLETADA, recuperadoUno.getEstado());
        assertEquals(Boolean.TRUE, recuperadoUno.getDecidioRealizar());
        assertEquals(
                "Quiero conocer mis opciones",
                recuperadoUno.getRazonDecision()
        );
        assertEquals(
                "Me interesa Computación",
                recuperadoUno.getResultadosRegistrados()
        );

        assertEquals(EstadoActividad.PENDIENTE, recuperadoDos.getEstado());
        assertNull(recuperadoDos.getDecidioRealizar());
        assertNull(recuperadoDos.getResultadosRegistrados());

        assertNotNull(recuperadoUno.getFechaActualizacion());
        assertNotEquals(
                recuperadoUno.getIdRegistro(),
                recuperadoDos.getIdRegistro()
        );

        assertEquals(
                1,
                repository.findByUsuario_IdUsuario("est-01").size()
        );
    }

    private Usuario crearEstudiante(String id, String nombreUsuario) {
        // Hash BCrypt de prueba; aquí no estamos probando el login.
        String hashPrueba =
                "$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji";

        return new Usuario(
                id,
                nombreUsuario,
                "Estudiante " + nombreUsuario,
                nombreUsuario + "@example.com",
                hashPrueba,
                Rol.ESTUDIANTE,
                EstadoVerificacion.NO_VERIFICADO
        );
    }
}