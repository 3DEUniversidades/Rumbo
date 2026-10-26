package com.rumbo.modelo;

import com.rumbo.dto.CarreraRespuesta;
import java.util.List;

public record ComparacionCarreras(
        List<CarreraRespuesta> carreras,
        List<Fila> filas
) {
    public ComparacionCarreras {
        carreras = List.copyOf(carreras);
        filas = List.copyOf(filas);
    }

    public record Fila(
            String campo,
            String criterio,
            List<String> valores,
            List<String> indicadores,
            String diferencia
    ) {
        public Fila {
            valores = List.copyOf(valores);
            indicadores = List.copyOf(indicadores);
        }
    }
}