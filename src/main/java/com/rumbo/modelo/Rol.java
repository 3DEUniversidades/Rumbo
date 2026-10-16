package com.rumbo.modelo;

import java.util.Locale;

public enum Rol {

    ESTUDIANTE,
    COLABORADOR,
    SUPERVISOR_DE_USUARIOS,
    SUPERVISOR_DE_MENSAJES,
    ENCARGADO_DE_CARRERAS,
    ADMIN;

    public boolean esPublico() {
        return this == ESTUDIANTE || this == COLABORADOR;
    }

    public static Rol desdeTexto(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return Rol.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
