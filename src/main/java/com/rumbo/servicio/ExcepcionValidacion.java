package com.rumbo.servicio;

import java.util.Map;

public class ExcepcionValidacion extends RuntimeException {

    private final Map<String, String> errores;

    public ExcepcionValidacion(Map<String, String> errores) {
        super("Datos invalidos");
        this.errores = errores;
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
