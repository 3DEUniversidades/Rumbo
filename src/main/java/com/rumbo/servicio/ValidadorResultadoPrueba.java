package com.rumbo.servicio;

import com.rumbo.dto.ResultadoPruebaSolicitud;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ValidadorResultadoPrueba {
    public void exigirValido(ResultadoPruebaSolicitud solicitud) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (solicitud == null) {
            throw new ExcepcionValidacion(Map.of("general", "Faltan los datos del resultado"));
        }
        if (solicitud.fechaRealizacion() == null) {
            errores.put("fechaRealizacion", "Indica cuando realizaste la prueba externa");
        } else if (solicitud.fechaRealizacion().isAfter(LocalDate.now(ZoneId.of("America/Guatemala")))) {
            errores.put("fechaRealizacion", "La fecha no puede estar en el futuro");
        }
        if (solicitud.resultado() == null || solicitud.resultado().isBlank()) {
            errores.put("resultado", "Escribe el resultado que obtuviste fuera de Rumbo");
        } else if (solicitud.resultado().strip().length() > 6000) {
            errores.put("resultado", "El maximo es de 6000 caracteres");
        }
        if (solicitud.interpretacionPersonal() != null && solicitud.interpretacionPersonal().strip().length() > 4000) {
            errores.put("interpretacionPersonal", "El maximo es de 4000 caracteres");
        }
        if (!errores.isEmpty()) throw new ExcepcionValidacion(errores);
    }
}
