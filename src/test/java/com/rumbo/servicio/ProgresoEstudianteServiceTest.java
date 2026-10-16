package com.rumbo.servicio;

import com.rumbo.modelo.Actividad;
import com.rumbo.modelo.EstadoActividad;
import com.rumbo.modelo.RegistroActividadEstudiante;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.RegistroActividadEstudianteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.sql.init.mode=never")
@ActiveProfiles("test")
@Import(ProgresoEstudianteService.class)
class ProgresoEstudianteServiceTest {

    @Autowired
    private ProgresoEstudianteService servicio;

    @Autowired
    private RegistroActividadEstudianteRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void prepararDatos() {
        String hashPrueba =
                "$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji";

        Usuario estudiante = new Usuario(
                "est-01",
                "kevin",
                "Estudiante Kevin",
                "kevin@example.com",
                hashPrueba,
                "ESTUDIANTE",
                "NO_VERIFICADO"
        );

        Actividad actividad = new Actividad(
                "act-01",
                "2026-10-16",
                "Explorar carreras",
                "Conocer opciones universitarias",
                "Investiga carreras de tu interés",
                "Consultar tres carreras",
                "Investigación documental",
                "Catálogo de carreras",
                "Anotar tus conclusiones",
                "Exploración",
                false
        );

        entityManager.persist(estudiante);
        entityManager.persist(actividad);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void guardaDecisionYRecortaEspaciosDeLaRazon() {
        servicio.registrarDecision(
                "est-01",
                "act-01",
                true,
                "  Quiero conocer mis opciones  "
        );

        RegistroActividadEstudiante registro = consultarRegistro();

        assertEquals(Boolean.TRUE, registro.getDecidioRealizar());
        assertEquals(EstadoActividad.PENDIENTE, registro.getEstado());
        assertEquals(
                "Quiero conocer mis opciones",
                registro.getRazonDecision()
        );
    }

    @Test
    void cambiarDecisionActualizaElMismoRegistroYConservaResultados() {
        RegistroActividadEstudiante inicial = servicio.registrarDecision(
                "est-01",
                "act-01",
                true,
                "Quiero explorar"
        );

        String idOriginal = inicial.getIdRegistro();

        inicial.setResultadosRegistrados("Me interesa Computación");
        repository.save(inicial);

        servicio.registrarDecision(
                "est-01",
                "act-01",
                false,
                "La realizaré en otra ocasión"
        );

        RegistroActividadEstudiante actualizado = consultarRegistro();

        assertEquals(idOriginal, actualizado.getIdRegistro());
        assertEquals(Boolean.FALSE, actualizado.getDecidioRealizar());
        assertEquals(EstadoActividad.RECHAZADA, actualizado.getEstado());
        assertEquals(
                "La realizaré en otra ocasión",
                actualizado.getRazonDecision()
        );
        assertEquals(
                "Me interesa Computación",
                actualizado.getResultadosRegistrados()
        );
        assertEquals(
                1,
                repository.findByUsuario_IdUsuario("est-01").size()
        );
    }

    @Test
    void rechazaRazonVaciaSinCrearRegistro() {
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.registrarDecision(
                        "est-01",
                        "act-01",
                        true,
                        "   "
                )
        );

        assertEquals(0, repository.count());
    }

    @Test
    void confirmarDecisionNoDeshaceUnaActividadCompletada() {
        RegistroActividadEstudiante inicial = servicio.registrarDecision(
                "est-01",
                "act-01",
                true,
                "Quiero explorar"
        );

        inicial.setEstado(EstadoActividad.COMPLETADA);
        repository.saveAndFlush(inicial);
        entityManager.clear();

        servicio.registrarDecision(
                "est-01",
                "act-01",
                true,
                "Confirmo mi decisión"
        );

        RegistroActividadEstudiante actualizado = consultarRegistro();

        assertEquals(EstadoActividad.COMPLETADA, actualizado.getEstado());
        assertEquals(Boolean.TRUE, actualizado.getDecidioRealizar());
    }

    @Test
    void completarDosVecesMantieneUnSoloRegistroCompletado() {
        servicio.marcarCompletada("est-01", "act-01");
        servicio.marcarCompletada("est-01", "act-01");

        RegistroActividadEstudiante registro = consultarRegistro();

        assertEquals(EstadoActividad.COMPLETADA, registro.getEstado());
        assertEquals(Boolean.TRUE, registro.getDecidioRealizar());
        assertEquals(
                1,
                repository.findByUsuario_IdUsuario("est-01").size()
        );
    }

    @Test
    void guardarResultadosConservaLaDecisionYElEstado() {
        servicio.registrarDecision(
                "est-01",
                "act-01",
                true,
                "Quiero explorar"
        );

        servicio.guardarResultados(
                "est-01",
                "act-01",
                "  Me interesan Computación y Arquitectura  "
        );

        RegistroActividadEstudiante registro = consultarRegistro();

        assertEquals(
                "Me interesan Computación y Arquitectura",
                registro.getResultadosRegistrados()
        );
        assertEquals(EstadoActividad.PENDIENTE, registro.getEstado());
        assertEquals(Boolean.TRUE, registro.getDecidioRealizar());
        assertEquals("Quiero explorar", registro.getRazonDecision());
    }

    @Test
    void consultarSinRegistroMuestraPendienteSinGuardarDatos() {
        var actividades = servicio.consultarLineaDeTiempo("est-01");

        assertEquals(1, actividades.size());

        var actividad = actividades.get(0);

        assertEquals("act-01", actividad.idActividad());
        assertEquals(EstadoActividad.PENDIENTE, actividad.estado());
        assertNull(actividad.decidioRealizar());
        assertEquals(0, repository.count());
    }

    @Test
    void progresoSeActualizaCuandoSeCompletaUnaActividad() {
        var progresoInicial = servicio.consultarProgresoGeneral("est-01");

        assertEquals(1, progresoInicial.totalActividades());
        assertEquals(0, progresoInicial.actividadesCompletadas());
        assertEquals(0.0, progresoInicial.porcentaje(), 0.001);

        servicio.marcarCompletada("est-01", "act-01");

        entityManager.flush();
        entityManager.clear();

        var progresoFinal = servicio.consultarProgresoGeneral("est-01");

        assertEquals(1, progresoFinal.totalActividades());
        assertEquals(1, progresoFinal.actividadesCompletadas());
        assertEquals(100.0, progresoFinal.porcentaje(), 0.001);
    }

    @Test
    void registraActividadNoRealizadaConSuRazon() {
        servicio.marcarNoRealizada(
                "est-01",
                "act-01",
                "  No tuve tiempo para investigar  "
        );

        RegistroActividadEstudiante registro = consultarRegistro();

        assertEquals(EstadoActividad.NO_REALIZADA, registro.getEstado());
        assertEquals(Boolean.TRUE, registro.getDecidioRealizar());
        assertEquals(
                "No tuve tiempo para investigar",
                registro.getRazonDecision()
        );

        var progreso = servicio.consultarProgresoGeneral("est-01");

        assertEquals(0, progreso.actividadesCompletadas());
        assertEquals(0.0, progreso.porcentaje(), 0.001);
    }

    @Test
    void sinActividadesElProgresoEsCero() {
        Actividad actividad = entityManager.find(
                Actividad.class,
                "act-01"
        );

        entityManager.remove(actividad);
        entityManager.flush();
        entityManager.clear();

        var progreso = servicio.consultarProgresoGeneral("est-01");

        assertEquals(0, progreso.totalActividades());
        assertEquals(0, progreso.actividadesCompletadas());
        assertEquals(0.0, progreso.porcentaje(), 0.001);
    }

    @Test
    void unaCompletadaYUnaRechazadaProducenCincuentaPorCiento() {
        Actividad segundaActividad = new Actividad(
                "act-02",
                "2026-10-17",
                "Consultar oportunidades laborales",
                "Conocer el campo laboral",
                "Investiga opciones de empleo",
                "Consultar tres oportunidades",
                "Investigación documental",
                "Recursos universitarios",
                "Anotar conclusiones",
                "Exploración",
                false
        );

        entityManager.persist(segundaActividad);
        entityManager.flush();

        servicio.marcarCompletada("est-01", "act-01");

        servicio.registrarDecision(
                "est-01",
                "act-02",
                false,
                "Prefiero realizar otra actividad"
        );

        entityManager.flush();
        entityManager.clear();

        var progreso = servicio.consultarProgresoGeneral("est-01");

        assertEquals(2, progreso.totalActividades());
        assertEquals(1, progreso.actividadesCompletadas());
        assertEquals(50.0, progreso.porcentaje(), 0.001);
    }

    @Test
    void consultarDetalleDevuelveInstruccionesSinCrearRegistro() {
        var detalle = servicio.consultarDetalleActividad(
                "est-01",
                "act-01"
        );

        assertEquals("act-01", detalle.idActividad());
        assertEquals(
                "Investiga carreras de tu interés",
                detalle.instrucciones()
        );
        assertEquals(EstadoActividad.PENDIENTE, detalle.estado());
        assertNull(detalle.decidioRealizar());
        assertEquals(0, repository.count());
    }

    @Test
    void consultarDetalleDeActividadInexistenteProduceError() {
        assertThrows(
                ExcepcionActividad.class,
                () -> servicio.consultarDetalleActividad(
                        "est-01",
                        "actividad-inexistente"
                )
        );
    }

    private RegistroActividadEstudiante consultarRegistro() {
        entityManager.flush();
        entityManager.clear();

        return repository
                .findByUsuario_IdUsuarioAndActividad_IdActividad(
                        "est-01",
                        "act-01"
                )
                .orElseThrow();
    }
}