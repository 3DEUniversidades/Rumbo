package com.rumbo.servicio;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrdenFechasActividadesTest {
    @Test
    void colocaLasProximasAntesDeLasPasadas() {
        assertEquals(
                List.of("2026-11-13", "2026-11-16", "2026-10-22", "2026-01-15"),
                ordenar(LocalDate.of(2026, 11, 12),
                        "2026-01-15", "2026-11-16", "2026-10-22", "2026-11-13")
        );
    }

    @Test
    void colocaHoyAntesDeLasFechasFuturas() {
        assertEquals(
                List.of("2026-11-12", "2026-11-13", "2026-11-25", "2026-11-11"),
                ordenar(LocalDate.of(2026, 11, 12),
                        "2026-11-25", "2026-11-11", "2026-11-13", "2026-11-12")
        );
    }

    @Test
    void ordenaLasPasadasDeMasRecienteAMasAntigua() {
        assertEquals(
                List.of("2026-11-11", "2026-10-22", "2026-02-10", "2026-01-15"),
                ordenar(LocalDate.of(2026, 11, 12),
                        "2026-02-10", "2026-01-15", "2026-11-11", "2026-10-22")
        );
    }

    @Test
    void conservaElOrdenAlCambiarDeMesYAnio() {
        assertEquals(
                List.of("2026-12-31", "2027-01-01", "2027-02-01", "2026-12-30"),
                ordenar(LocalDate.of(2026, 12, 31),
                        "2027-02-01", "2026-12-30", "2027-01-01", "2026-12-31")
        );
    }

    @Test
    void unaFechaQueYaPasoSeMueveDetrasDeLasProximas() {
        List<String> fechas = List.of("2026-11-13", "2026-11-16");

        assertEquals(fechas, fechas.stream()
                .sorted(OrdenFechasActividades.porProximidad(LocalDate.of(2026, 11, 12)))
                .toList());
        assertEquals(List.of("2026-11-16", "2026-11-13"), fechas.stream()
                .sorted(OrdenFechasActividades.porProximidad(LocalDate.of(2026, 11, 14)))
                .toList());
    }

    @Test
    void fechasIgualesConservanTodasLasActividades() {
        assertEquals(
                List.of("2026-11-12", "2026-11-12", "2026-11-13"),
                ordenar(LocalDate.of(2026, 11, 12),
                        "2026-11-13", "2026-11-12", "2026-11-12")
        );
    }

    private List<String> ordenar(LocalDate hoy, String... fechas) {
        return List.of(fechas).stream()
                .sorted(OrdenFechasActividades.porProximidad(hoy))
                .toList();
    }
}
