package com.rumbo.servicio;

public class ExcepcionPermiso extends RuntimeException {

    public ExcepcionPermiso(String mensaje) {
        super(mensaje);
    }
}
