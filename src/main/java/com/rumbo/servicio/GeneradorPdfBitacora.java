package com.rumbo.servicio;

import com.rumbo.dto.EntradaBitacoraRespuesta;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class GeneradorPdfBitacora {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color VERDE = new Color(11, 107, 58);
    private static final Color TEXTO = new Color(27, 36, 48);
    private static final Color SECUNDARIO = new Color(91, 103, 118);

    public void escribir(OutputStream salida, String estudiante,
                         List<EntradaBitacoraRespuesta> entradas) throws IOException {
        try (PDDocument documento = new PDDocument();
             InputStream fuente = PDType0Font.class.getResourceAsStream(
                     "/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")) {
            if (fuente == null) {
                throw new IOException("No se encontró la fuente del PDF");
            }
            PDType0Font tipografia = PDType0Font.load(documento, fuente, true);
            documento.getDocumentInformation().setTitle("Bitácora vocacional - Rumbo");
            documento.getDocumentInformation().setAuthor(estudiante);
            try (Maquetador pagina = new Maquetador(documento, tipografia)) {
                pagina.texto("RUMBO / MI PROCESO VOCACIONAL", 10, VERDE);
                pagina.texto("Bitácora personal", 23, VERDE);
                pagina.texto(estudiante, 13, TEXTO);
                pagina.texto("Exportada el " + FECHA.format(LocalDateTime.now())
                        + " · " + entradas.size() + (entradas.size() == 1 ? " entrada" : " entradas"),
                        10, SECUNDARIO);
                pagina.espacio(16);

                int numero = 1;
                for (EntradaBitacoraRespuesta entrada : entradas) {
                    pagina.reservar(100);
                    pagina.texto("Entrada " + numero++, 15, VERDE);
                    pagina.texto("Creada: " + FECHA.format(entrada.fechaCreacion()), 10, SECUNDARIO);
                    if (entrada.fechaEdicion() != null) {
                        pagina.texto("Última edición: " + FECHA.format(entrada.fechaEdicion()), 10, SECUNDARIO);
                    }
                    if (entrada.descripcionActividad() != null) {
                        pagina.texto("Actividad: " + entrada.descripcionActividad(), 10, SECUNDARIO);
                    }
                    pagina.espacio(6);
                    pagina.texto(entrada.contenido(), 11, TEXTO);
                    pagina.espacio(18);
                }
            }

            int numero = 1;
            for (PDPage pagina : documento.getPages()) {
                try (PDPageContentStream pie = new PDPageContentStream(documento, pagina,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    pie.beginText();
                    pie.setFont(tipografia, 9);
                    pie.setNonStrokingColor(SECUNDARIO);
                    pie.newLineAtOffset(48, 28);
                    pie.showText("Rumbo · Bitácora personal | Página " + numero++ + " de " + documento.getNumberOfPages());
                    pie.endText();
                }
            }
            documento.save(salida);
        }
    }

    private static final class Maquetador implements AutoCloseable {
        private static final float MARGEN = 48;
        private static final float ANCHO = PDRectangle.A4.getWidth() - 2 * MARGEN;
        private final PDDocument documento;
        private final PDType0Font fuente;
        private PDPageContentStream contenido;
        private float y;

        private Maquetador(PDDocument documento, PDType0Font fuente) throws IOException {
            this.documento = documento;
            this.fuente = fuente;
            nuevaPagina();
        }

        private void nuevaPagina() throws IOException {
            if (contenido != null) contenido.close();
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            contenido = new PDPageContentStream(documento, pagina);
            y = PDRectangle.A4.getHeight() - MARGEN;
        }

        private void reservar(float alto) throws IOException {
            if (y - alto < MARGEN) nuevaPagina();
        }

        private void espacio(float alto) {
            y -= alto;
        }

        private void texto(String texto, float tamano, Color color) throws IOException {
            String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFC)
                    .replace("\t", "    ");
            for (String parrafo : normalizado.split("\\R", -1)) {
                StringBuilder linea = new StringBuilder();
                float ancho = 0;
                int ultimoEspacio = -1;
                for (int punto : parrafo.codePoints().toArray()) {
                    if (Character.isISOControl(punto)) continue;
                    // Los símbolos sin glifo se conservan como código Unicode legible.
                    String caracter = fuente.hasGlyph(punto) ? new String(Character.toChars(punto))
                            : String.format(Locale.ROOT, "[U+%04X]", punto);
                    for (int glifo : caracter.codePoints().toArray()) {
                        String letra = new String(Character.toChars(glifo));
                        float anchoLetra = fuente.getStringWidth(letra) * tamano / 1000;
                        if (ancho + anchoLetra > ANCHO && !linea.isEmpty()) {
                            if (ultimoEspacio > 0) {
                                linea(linea.substring(0, ultimoEspacio).stripTrailing(), tamano, color);
                                String resto = linea.substring(ultimoEspacio + 1);
                                linea.setLength(0);
                                linea.append(resto);
                                ancho = fuente.getStringWidth(resto) * tamano / 1000;
                            } else {
                                linea(linea.toString(), tamano, color);
                                linea.setLength(0);
                                ancho = 0;
                            }
                            ultimoEspacio = -1;
                        }
                        if (" ".equals(letra)) ultimoEspacio = linea.length();
                        linea.append(letra);
                        ancho += anchoLetra;
                    }
                }
                linea(linea.toString(), tamano, color);
            }
        }

        private void linea(String texto, float tamano, Color color) throws IOException {
            float alto = tamano * 1.45f;
            reservar(alto);
            contenido.beginText();
            contenido.setFont(fuente, tamano);
            contenido.setNonStrokingColor(color);
            contenido.newLineAtOffset(MARGEN, y);
            contenido.showText(texto);
            contenido.endText();
            y -= alto;
        }

        @Override
        public void close() throws IOException {
            contenido.close();
        }
    }
}
