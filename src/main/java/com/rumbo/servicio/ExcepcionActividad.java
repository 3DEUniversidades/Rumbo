package com.rumbo.servicio;

public class ExcepcionActividad extends RuntimeException {

    public ExcepcionActividad(String mensaje) {
        super(mensaje);
    }
}
