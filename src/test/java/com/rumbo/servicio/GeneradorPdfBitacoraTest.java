package com.rumbo.servicio;

import com.rumbo.dto.EntradaBitacoraRespuesta;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorPdfBitacoraTest {

    private final GeneradorPdfBitacora generador = new GeneradorPdfBitacora();

    @Test
    void conservaAcentosFechasActividadYTextoConFuenteIncrustada() throws Exception {
        byte[] pdf = generar("¡Qué aprendí! Mi orientacio\u0301n vocacional.\nSegunda reflexión: ¿qué sigue?");
        try (var documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("María Muñoz"));
            assertTrue(texto.contains("¡Qué aprendí! Mi orientación vocacional."));
            assertTrue(texto.contains("Segunda reflexión: ¿qué sigue?"));
            assertTrue(texto.contains("05/11/2026 10:30"));
            assertTrue(texto.contains("06/11/2026 11:00"));
            assertTrue(texto.contains("Actividad: Explorar carreras"));
            for (var nombre : documento.getPage(0).getResources().getFontNames()) {
                assertTrue(documento.getPage(0).getResources().getFont(nombre).isEmbedded());
            }
        }
    }

    @Test
    void paginaTextosLargosYPalabrasSinEspaciosSinPerderElFinal() throws Exception {
        byte[] pdf = generar("Reflexión sobre mis intereses y mi futuro.\n".repeat(220)
                + "palabramuylarga".repeat(150) + "\nFinal de todas mis reflexiones.");
        try (var documento = Loader.loadPDF(pdf)) {
            assertTrue(documento.getNumberOfPages() > 3);
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("Final de todas mis reflexiones."));
            assertTrue(texto.contains("Página " + documento.getNumberOfPages() + " de " + documento.getNumberOfPages()));
        }
    }

    @Test
    void simbolosSinGlifoNoImpidenDescargarElContenido() throws Exception {
        try (var documento = Loader.loadPDF(generar("Me entusiasma mi futuro 😊"))) {
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("Me entusiasma mi futuro"));
            assertTrue(texto.contains("[U+1F60A]"));
        }
    }

    private byte[] generar(String contenido) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        generador.escribir(salida, "María Muñoz", List.of(new EntradaBitacoraRespuesta(
                "entrada-pdf", contenido, LocalDateTime.of(2026, 11, 5, 10, 30),
                LocalDateTime.of(2026, 11, 6, 11, 0), "actividad-1", "Explorar carreras", List.of())));
        return salida.toByteArray();
    }
}
