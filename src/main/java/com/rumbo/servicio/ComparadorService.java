package com.rumbo.servicio;

import com.rumbo.dto.CarreraRespuesta;
import com.rumbo.modelo.Carrera;
import com.rumbo.modelo.ComparacionCarreras;
import com.rumbo.modelo.ComparacionCarreras.Fila;
import com.rumbo.persistencia.CarreraRepository;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComparadorService {

    private static final String NO_DISPONIBLE = "No disponible";

    private final CarreraRepository carreraRepository;

    public ComparadorService(CarreraRepository carreraRepository) {
        this.carreraRepository = carreraRepository;
    }

    @Transactional(readOnly = true)
    public ComparacionCarreras comparar(List<Long> ids) {
        Map<Long, Carrera> encontradas = carreraRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Carrera::getId, Function.identity()));

        if (encontradas.size() != ids.size()) {
            throw new ExcepcionCarreraNoEncontrada();
        }

        // Conserva el orden elegido por el estudiante.
        List<CarreraRespuesta> carreras = ids.stream()
                .map(encontradas::get)
                .map(this::mapearCarrera)
                .toList();

        List<Fila> filas = new ArrayList<>();

        filas.add(texto("facultad", "Facultad", carreras,
                CarreraRespuesta::facultad));
        filas.add(texto("descripcion", "Descripción", carreras,
                CarreraRespuesta::descripcion));

        filas.add(numerica("duracion", "Duración", carreras,
                CarreraRespuesta::duracion, "años", true, ""));

        filas.add(numerica("costo", "Costo de referencia", carreras,
                CarreraRespuesta::costo, "GTQ",
                costosComparables(carreras),
                "No comparable: el periodo o el año del costo falta o es diferente."));

        filas.add(texto("costoPeriodo", "Periodo del costo", carreras,
                CarreraRespuesta::costoPeriodo));
        filas.add(texto("costoAnio", "Año del tarifario", carreras,
                c -> c.costoAnio() == null ? null : c.costoAnio().toString()));

        filas.add(texto("modalidad", "Modalidad", carreras,
                CarreraRespuesta::modalidad));

        filas.add(numerica("cargaCreditos", "Carga de créditos", carreras,
                CarreraRespuesta::cargaCreditos, "créditos", true, ""));

        filas.add(lista("areasFormacion", "Áreas de formación", carreras,
                CarreraRespuesta::areasFormacion));
        filas.add(lista("competenciasDesarrolladas", "Competencias", carreras,
                CarreraRespuesta::competenciasDesarrolladas));
        filas.add(lista("oportunidadesLaborales", "Oportunidades laborales", carreras,
                CarreraRespuesta::oportunidadesLaborales));

        // El modelo actual no tiene un campo estructurado que indique
        // si el salario es mensual o anual. No se supone esa equivalencia.
        filas.add(numerica("salarioEstimado", "Salario estimado", carreras,
                CarreraRespuesta::salarioEstimado, "GTQ", false,
                "No comparable automáticamente: revisa el periodo y alcance "
                        + "en el contexto laboral."));

        filas.add(numerica("empleabilidad", "Empleabilidad", carreras,
                CarreraRespuesta::empleabilidad, "%", true, ""));

        filas.add(texto("contextoLaboral", "Contexto laboral", carreras,
                CarreraRespuesta::contextoLaboral));
        filas.add(texto("pdfOficialUrl", "Documento oficial PDF", carreras,
                CarreraRespuesta::pdfOficialUrl));
        filas.add(texto("urlCarreraOficial", "Página oficial", carreras,
                CarreraRespuesta::urlCarreraOficial));

        return new ComparacionCarreras(carreras, filas);
    }

    private Fila numerica(
            String campo,
            String criterio,
            List<CarreraRespuesta> carreras,
            Function<CarreraRespuesta, ? extends Number> obtener,
            String unidad,
            boolean comparable,
            String motivo) {

        List<BigDecimal> numeros = carreras.stream()
                .map(c -> {
                    Number valor = obtener.apply(c);
                    return valor == null
                            ? null
                            : new BigDecimal(valor.toString());
                })
                .toList();

        List<String> valores = numeros.stream()
                .map(n -> n == null ? NO_DISPONIBLE : mostrarNumero(n, unidad))
                .toList();

        if (numeros.stream().anyMatch(Objects::isNull)) {
            return fila(campo, criterio, valores,
                    "Sin datos suficientes para comparar.");
        }

        if (!comparable) {
            return fila(campo, criterio, valores, motivo);
        }

        BigDecimal menor = Collections.min(numeros);
        BigDecimal mayor = Collections.max(numeros);

        List<String> indicadores = numeros.stream()
                .map(n -> {
                    if (menor.compareTo(mayor) == 0) {
                        return "= Igual";
                    }
                    if (n.compareTo(mayor) == 0) {
                        return "↑ Mayor";
                    }
                    if (n.compareTo(menor) == 0) {
                        return "↓ Menor";
                    }
                    return "";
                })
                .toList();

        String diferencia;

        if (menor.compareTo(mayor) == 0) {
            diferencia = "Mismo valor.";
        } else if (numeros.size() == 2) {
            boolean primeraMayor = numeros.get(0).compareTo(numeros.get(1)) > 0;

            String unidadDiferencia = "%".equals(unidad)
                    ? "puntos porcentuales"
                    : unidad;

            diferencia = (primeraMayor ? "A" : "B")
                    + " tiene "
                    + mostrarNumero(mayor.subtract(menor), unidadDiferencia)
                    + " más que "
                    + (primeraMayor ? "B." : "A.");
        } else {
            diferencia = "Se señalan el mayor y el menor valor.";
        }

        return new Fila(campo, criterio, valores, indicadores, diferencia);
    }

    private Fila texto(
            String campo,
            String criterio,
            List<CarreraRespuesta> carreras,
            Function<CarreraRespuesta, String> obtener) {

        List<String> originales = carreras.stream().map(obtener).toList();
        List<String> valores = originales.stream().map(this::dato).toList();

        String diferencia;

        if (originales.stream().anyMatch(v -> v == null || v.isBlank())) {
            diferencia = "Sin datos suficientes para comparar.";
        } else {
            boolean iguales = originales.stream()
                    .map(this::normalizar)
                    .distinct()
                    .count() == 1;

            diferencia = iguales ? "Misma información." : "Información diferente.";
        }

        return fila(campo, criterio, valores, diferencia);
    }

    private Fila lista(
            String campo,
            String criterio,
            List<CarreraRespuesta> carreras,
            Function<CarreraRespuesta, List<String>> obtener) {

        List<Map<String, String>> listas = carreras.stream()
                .map(c -> normalizarLista(obtener.apply(c)))
                .toList();

        List<String> valores = listas.stream()
                .map(m -> m.isEmpty()
                        ? NO_DISPONIBLE
                        : String.join("\n", m.values()))
                .toList();

        if (listas.stream().anyMatch(Map::isEmpty)) {
            return fila(campo, criterio, valores,
                    "Sin datos suficientes para comparar.");
        }

        if (listas.size() != 2) {
            return fila(campo, criterio, valores,
                    "Consulta las listas de cada carrera.");
        }

        Map<String, String> a = listas.get(0);
        Map<String, String> b = listas.get(1);

        List<String> comunes = a.keySet().stream()
                .filter(b::containsKey)
                .map(a::get)
                .toList();

        List<String> soloA = a.keySet().stream()
                .filter(k -> !b.containsKey(k))
                .map(a::get)
                .toList();

        List<String> soloB = b.keySet().stream()
                .filter(k -> !a.containsKey(k))
                .map(b::get)
                .toList();

        String diferencia = "En común: " + enumerar(comunes)
                + "\n\nSolo A: " + enumerar(soloA)
                + "\n\nSolo B: " + enumerar(soloB);

        return fila(campo, criterio, valores, diferencia);
    }

    private boolean costosComparables(List<CarreraRespuesta> carreras) {
        CarreraRespuesta primera = carreras.get(0);

        if (primera.costoAnio() == null
                || primera.costoPeriodo() == null
                || primera.costoPeriodo().isBlank()) {
            return false;
        }

        return carreras.stream().allMatch(c ->
                Objects.equals(primera.costoAnio(), c.costoAnio())
                && normalizar(primera.costoPeriodo())
                        .equals(normalizar(c.costoPeriodo())));
    }

    private Map<String, String> normalizarLista(List<String> lista) {
        Map<String, String> resultado = new LinkedHashMap<>();

        if (lista != null) {
            for (String elemento : lista) {
                if (elemento != null && !elemento.isBlank()) {
                    resultado.putIfAbsent(
                            normalizar(elemento), elemento.trim());
                }
            }
        }

        return resultado;
    }

    private Fila fila(
            String campo, String criterio,
            List<String> valores, String diferencia) {

        return new Fila(campo, criterio, valores,
                Collections.nCopies(valores.size(), ""), diferencia);
    }

    private String dato(String valor) {
        return valor == null || valor.isBlank()
                ? NO_DISPONIBLE
                : valor.trim();
    }

    private String enumerar(List<String> elementos) {
        return elementos.isEmpty() ? "Ninguno." : String.join("; ", elementos);
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String mostrarNumero(BigDecimal numero, String unidad) {
        Locale locale = Locale.forLanguageTag("es-GT");

        if ("GTQ".equals(unidad)) {
            return NumberFormat.getCurrencyInstance(locale).format(numero);
        }

        NumberFormat formato = NumberFormat.getNumberInstance(locale);
        formato.setMaximumFractionDigits(2);

        return formato.format(numero) + " " + unidad;
    }

    private CarreraRespuesta mapearCarrera(Carrera c) {
        return new CarreraRespuesta(
                c.getId(),
                c.getNombre(),
                c.getFacultad(),
                c.getDescripcion(),
                c.getDuracion(),
                c.getCosto(),
                c.getCostoPeriodo(),
                c.getCostoAnio(),
                c.getModalidad(),
                c.getCargaCreditos(),
                c.getAreasFormacion(),
                c.getCompetenciasDesarrolladas(),
                c.getOportunidadesLaborales(),
                c.getSalarioEstimado(),
                c.getEmpleabilidad(),
                c.getContextoLaboral(),
                c.getPdfOficialUrl(),
                c.getUrlCarreraOficial()
        );
    }
}