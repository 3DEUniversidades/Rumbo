package com.rumbo.servicio;

public class ExcepcionCarrera extends RuntimeException {

    public ExcepcionCarrera(String mensaje) {
        super(mensaje);
    }
}