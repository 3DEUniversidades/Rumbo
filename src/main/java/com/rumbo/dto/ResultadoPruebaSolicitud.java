package com.rumbo.dto;

import java.time.LocalDate;

public record ResultadoPruebaSolicitud(
        LocalDate fechaRealizacion,
        String resultado,
        String interpretacionPersonal
) {}
