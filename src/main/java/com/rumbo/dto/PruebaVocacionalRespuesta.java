package com.rumbo.dto;

import com.rumbo.modelo.TipoPruebaVocacional;

public record PruebaVocacionalRespuesta(
        String idPrueba, String idActividad, String nombre, TipoPruebaVocacional tipo,
        String proveedor, String proposito, String contexto, String instrucciones,
        String metodo, String enlaceExterno, String recursoInterpretacionUrl,
        String orientacionInterpretacion, String condicionesAcceso,
        ResultadoPruebaRespuesta miResultado
) {}
