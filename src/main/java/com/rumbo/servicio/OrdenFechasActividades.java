package com.rumbo.servicio;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;

final class OrdenFechasActividades {
    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Guatemala");

    private OrdenFechasActividades() {
    }

    static Comparator<String> porProximidad() {
        return porProximidad(LocalDate.now(ZONA_HORARIA));
    }

    static Comparator<String> porProximidad(LocalDate hoy) {
        // Las fechas se validan y almacenan con el formato AAAA-MM-DD.
        String fechaHoy = hoy.toString();

        return (primera, segunda) -> {
            boolean primeraPasada = primera.compareTo(fechaHoy) < 0;
            boolean segundaPasada = segunda.compareTo(fechaHoy) < 0;

            if (primeraPasada != segundaPasada) {
                return primeraPasada ? 1 : -1;
            }

            // Hoy y futuras: primero la más cercana. Pasadas: primero la más reciente.
            return primeraPasada
                    ? segunda.compareTo(primera)
                    : primera.compareTo(segunda);
        };
    }
}
