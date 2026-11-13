package com.rumbo.servicio;

import java.util.Comparator;

final class OrdenFechasActividades {
    private OrdenFechasActividades() {
    }

    static Comparator<String> cronologico() {
        // Las fechas se validan y almacenan con el formato AAAA-MM-DD.
        return Comparator.naturalOrder();
    }
}
