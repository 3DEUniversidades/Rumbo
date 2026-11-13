package com.rumbo.servicio;

public class ExcepcionAutenticacion extends RuntimeException {

    public ExcepcionAutenticacion(String mensaje) {
        super(mensaje);
    }
}
