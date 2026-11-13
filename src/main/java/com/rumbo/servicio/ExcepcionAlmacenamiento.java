package com.rumbo.servicio;

public class ExcepcionAlmacenamiento extends RuntimeException {

    public ExcepcionAlmacenamiento(String mensaje) {
        super(mensaje);
    }

    public ExcepcionAlmacenamiento(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
