package com.rumbo.servicio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestorPermisosTest {

    private final GestorPermisos gestorPermisos = new GestorPermisos();

    @Test
    void estudianteNoVerificadoNoPuedeAccederAlForo() {
        assertFalse(gestorPermisos.puedeAccederAlForo("ESTUDIANTE", "NO_VERIFICADO"));
    }

    @Test
    void estudianteVerificadoPuedeAccederAlForo() {
        assertTrue(gestorPermisos.puedeAccederAlForo("ESTUDIANTE", "VERIFICADO"));
    }

    @Test
    void colaboradorPuedeAccederAlForo() {
        assertTrue(gestorPermisos.puedeAccederAlForo("COLABORADOR", "NO_VERIFICADO"));
    }
}
