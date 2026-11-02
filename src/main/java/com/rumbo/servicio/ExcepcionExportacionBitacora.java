package com.rumbo.servicio;

public class ExcepcionExportacionBitacora extends RuntimeException {

    public ExcepcionExportacionBitacora(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
