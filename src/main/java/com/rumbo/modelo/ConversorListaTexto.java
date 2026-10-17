package com.rumbo.modelo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarda una lista de textos como un arreglo JSON dentro de una sola columna de texto.
 * Asi la lista se lee, se edita y se registra en el historial como un unico campo.
 */
@Converter
public class ConversorListaTexto implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final TypeReference<List<String>> TIPO_LISTA = new TypeReference<>() { };

    @Override
    public String convertToDatabaseColumn(List<String> lista) {
        if (lista == null) {
            return null;
        }
        try {
            return MAPEADOR.writeValueAsString(lista);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo convertir la lista a texto", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String texto) {
        if (texto == null || texto.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(MAPEADOR.readValue(texto, TIPO_LISTA));
        } catch (JsonProcessingException e) {
            // Dato antiguo guardado como texto simple: se conserva como un unico elemento.
            List<String> unico = new ArrayList<>();
            unico.add(texto.trim());
            return unico;
        }
    }
}
