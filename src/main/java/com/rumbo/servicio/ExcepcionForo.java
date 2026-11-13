package com.rumbo.servicio;

public class ExcepcionForo extends RuntimeException {

    private final int codigo;

    public ExcepcionForo(int codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }
}