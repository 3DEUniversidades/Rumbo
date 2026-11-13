package com.rumbo.servicio;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrdenFechasActividadesTest {
    @Test
    void ordenaDeMasAntiguaAMasReciente() {
        assertEquals(
                List.of("2026-01-15", "2026-10-22", "2026-11-13", "2026-11-16"),
                ordenar("2026-01-15", "2026-11-16", "2026-10-22", "2026-11-13")
        );
    }

    @Test
    void mantieneElOrdenEntreDiasConsecutivos() {
        assertEquals(
                List.of("2026-11-11", "2026-11-12", "2026-11-13", "2026-11-25"),
                ordenar("2026-11-25", "2026-11-11", "2026-11-13", "2026-11-12")
        );
    }

    @Test
    void conservaLaCronologiaDeLasActividadesAnteriores() {
        assertEquals(
                List.of("2026-01-15", "2026-02-10", "2026-10-22", "2026-11-11"),
                ordenar("2026-02-10", "2026-01-15", "2026-11-11", "2026-10-22")
        );
    }

    @Test
    void conservaElOrdenAlCambiarDeMesYAnio() {
        assertEquals(
                List.of("2026-12-30", "2026-12-31", "2027-01-01", "2027-02-01"),
                ordenar("2027-02-01", "2026-12-30", "2027-01-01", "2026-12-31")
        );
    }

    @Test
    void comparaElMesAntesQueElDia() {
        assertEquals(
                List.of("2026-01-31", "2026-02-01", "2026-10-02", "2026-11-01"),
                ordenar("2026-11-01", "2026-02-01", "2026-01-31", "2026-10-02")
        );
    }

    @Test
    void fechasIgualesConservanTodasLasActividades() {
        assertEquals(
                List.of("2026-11-12", "2026-11-12", "2026-11-13"),
                ordenar("2026-11-13", "2026-11-12", "2026-11-12")
        );
    }

    private List<String> ordenar(String... fechas) {
        return List.of(fechas).stream()
                .sorted(OrdenFechasActividades.cronologico())
                .toList();
    }
}
