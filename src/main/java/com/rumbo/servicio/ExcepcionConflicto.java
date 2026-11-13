package com.rumbo.servicio;

public class ExcepcionConflicto extends RuntimeException {

    public ExcepcionConflicto(String mensaje) {
        super(mensaje);
    }
}
