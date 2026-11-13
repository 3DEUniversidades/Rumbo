package com.rumbo.dto;

import com.rumbo.modelo.TipoPruebaVocacional;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ResultadoPruebaRespuesta(
        String idResultado, String idPrueba, String idActividad,
        String nombrePrueba, TipoPruebaVocacional tipo,
        LocalDate fechaRealizacion, String resultado, String interpretacionPersonal,
        LocalDateTime fechaActualizacion, boolean pruebaDisponible
) {}
