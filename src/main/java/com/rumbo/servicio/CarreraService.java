package com.rumbo.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.rumbo.dto.RegistroActualizacionRespuesta;
import com.rumbo.modelo.Carrera;
import com.rumbo.modelo.RegistroActualizacionCarrera;
import com.rumbo.persistencia.CarreraRepository;
import com.rumbo.persistencia.RegistroActualizacionCarreraRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarreraService {

    private static final Set<String> CAMPOS_EDITABLES = Set.of(
            "nombre", "facultad", "descripcion", "duracion", "costo", "costoPeriodo", "costoAnio",
            "modalidad", "cargaCreditos", "areasFormacion", "competenciasDesarrolladas",
            "oportunidadesLaborales", "salarioEstimado", "empleabilidad", "contextoLaboral",
            "pdfOficialUrl", "urlCarreraOficial");

    private static final int MAX_TEXTO_LARGO = 4000;
    private static final int MAX_ELEMENTOS_LISTA = 30;
    private static final int MAX_LARGO_ELEMENTO = 300;
    private static final double MAX_VALOR_NUMERICO = 1_000_000_000d;

    private final CarreraRepository carreraRepository;
    private final RegistroActualizacionCarreraRepository registroRepository;

    public CarreraService(CarreraRepository carreraRepository,
                          RegistroActualizacionCarreraRepository registroRepository) {
        this.carreraRepository = carreraRepository;
        this.registroRepository = registroRepository;
    }

    public List<Carrera> consultarCarreras() {
        // Orden fijo: sin el, una fila actualizada puede cambiar de posicion en PostgreSQL.
        return carreraRepository.findAll(Sort.by("id"));
    }

    public Optional<Carrera> buscarPorId(Long id) {
        return carreraRepository.findById(id);
    }

    /**
     * Aplica solo los campos presentes en el JSON. Un campo ausente no se toca; un campo con
     * valor null (o texto vacio) lo deja como "no disponible". Cada campo que cambia genera un
     * registro con valor anterior, valor nuevo, fecha y responsable.
     */
    @Transactional
    public Carrera actualizarCarrera(Long id, JsonNode cambios, String responsable) {
        Carrera carrera = carreraRepository.findById(id)
                .orElseThrow(ExcepcionCarreraNoEncontrada::new);

        if (cambios == null || !cambios.isObject()) {
            throw new ExcepcionCarrera("La solicitud debe ser un objeto JSON");
        }
        cambios.fieldNames().forEachRemaining(campo -> {
            if (!CAMPOS_EDITABLES.contains(campo)) {
                throw new ExcepcionCarrera("Campo no editable: " + campo);
            }
        });

        Registrador r = new Registrador(carrera, LocalDateTime.now(), responsable);

        if (cambios.has("nombre")) {
            r.aplicar("nombre", carrera.getNombre(),
                    textoRequerido(cambios.get("nombre"), "nombre", 120), carrera::setNombre);
        }
        if (cambios.has("facultad")) {
            r.aplicar("facultad", carrera.getFacultad(),
                    textoOpcional(cambios.get("facultad"), "facultad", 120), carrera::setFacultad);
        }
        if (cambios.has("descripcion")) {
            r.aplicar("descripcion", carrera.getDescripcion(),
                    textoOpcional(cambios.get("descripcion"), "descripción", MAX_TEXTO_LARGO),
                    carrera::setDescripcion);
        }
        if (cambios.has("duracion")) {
            Integer duracion = enteroRequerido(cambios.get("duracion"), "duración");
            if (duracion < 1 || duracion > 15) {
                throw new ExcepcionCarrera("La duración debe estar entre 1 y 15 años");
            }
            r.aplicar("duracion", carrera.getDuracion(), duracion, carrera::setDuracion);
        }
        if (cambios.has("costo")) {
            BigDecimal costo = decimalOpcional(cambios.get("costo"), "costo");
            if (costo == null) {
                throw new ExcepcionCarrera("El campo costo es obligatorio");
            }
            if (costo.signum() < 0) {
                throw new ExcepcionCarrera("El costo no puede ser negativo");
            }
            r.aplicar("costo", carrera.getCosto(), costo, carrera::setCosto);
        }
        if (cambios.has("costoPeriodo")) {
            r.aplicar("costoPeriodo", carrera.getCostoPeriodo(),
                    textoOpcional(cambios.get("costoPeriodo"), "periodo del costo", 200),
                    carrera::setCostoPeriodo);
        }
        if (cambios.has("costoAnio")) {
            Integer anio = enteroOpcional(cambios.get("costoAnio"), "año del costo");
            if (anio != null && (anio < 2000 || anio > 2100)) {
                throw new ExcepcionCarrera("El año del costo debe estar entre 2000 y 2100");
            }
            r.aplicar("costoAnio", carrera.getCostoAnio(), anio, carrera::setCostoAnio);
        }
        if (cambios.has("modalidad")) {
            r.aplicar("modalidad", carrera.getModalidad(),
                    textoOpcional(cambios.get("modalidad"), "modalidad", 80), carrera::setModalidad);
        }
        if (cambios.has("cargaCreditos")) {
            Integer creditos = enteroOpcional(cambios.get("cargaCreditos"), "carga de créditos");
            if (creditos != null && (creditos < 1 || creditos > 1000)) {
                throw new ExcepcionCarrera("La carga de créditos debe estar entre 1 y 1000");
            }
            r.aplicar("cargaCreditos", carrera.getCargaCreditos(), creditos,
                    carrera::setCargaCreditos);
        }
        if (cambios.has("areasFormacion")) {
            r.aplicar("areasFormacion", carrera.getAreasFormacion(),
                    listaTexto(cambios.get("areasFormacion"), "áreas de formación"),
                    carrera::setAreasFormacion);
        }
        if (cambios.has("competenciasDesarrolladas")) {
            r.aplicar("competenciasDesarrolladas", carrera.getCompetenciasDesarrolladas(),
                    listaTexto(cambios.get("competenciasDesarrolladas"), "competencias desarrolladas"),
                    carrera::setCompetenciasDesarrolladas);
        }
        if (cambios.has("oportunidadesLaborales")) {
            r.aplicar("oportunidadesLaborales", carrera.getOportunidadesLaborales(),
                    listaTexto(cambios.get("oportunidadesLaborales"), "oportunidades laborales"),
                    carrera::setOportunidadesLaborales);
        }
        if (cambios.has("salarioEstimado")) {
            BigDecimal salario = decimalOpcional(cambios.get("salarioEstimado"), "salario estimado");
            if (salario != null && salario.signum() < 0) {
                throw new ExcepcionCarrera("El salario estimado no puede ser negativo");
            }
            r.aplicar("salarioEstimado", carrera.getSalarioEstimado(), salario,
                    carrera::setSalarioEstimado);
        }
        if (cambios.has("empleabilidad")) {
            BigDecimal empleabilidad = decimalOpcional(cambios.get("empleabilidad"), "empleabilidad");
            if (empleabilidad != null
                    && (empleabilidad.signum() < 0 || empleabilidad.compareTo(new BigDecimal("100")) > 0)) {
                throw new ExcepcionCarrera("La empleabilidad debe estar entre 0 y 100");
            }
            r.aplicar("empleabilidad", carrera.getEmpleabilidad(), empleabilidad,
                    carrera::setEmpleabilidad);
        }
        if (cambios.has("contextoLaboral")) {
            r.aplicar("contextoLaboral", carrera.getContextoLaboral(),
                    textoOpcional(cambios.get("contextoLaboral"), "contexto laboral", MAX_TEXTO_LARGO),
                    carrera::setContextoLaboral);
        }
        if (cambios.has("pdfOficialUrl")) {
            r.aplicar("pdfOficialUrl", carrera.getPdfOficialUrl(),
                    urlOpcional(cambios.get("pdfOficialUrl"), "PDF oficial"),
                    carrera::setPdfOficialUrl);
        }
        if (cambios.has("urlCarreraOficial")) {
            r.aplicar("urlCarreraOficial", carrera.getUrlCarreraOficial(),
                    urlOpcional(cambios.get("urlCarreraOficial"), "enlace oficial"),
                    carrera::setUrlCarreraOficial);
        }

        carreraRepository.save(carrera);
        registroRepository.saveAll(r.registros);
        return carrera;
    }

    @Transactional(readOnly = true)
    public List<RegistroActualizacionRespuesta> consultarHistorial(Long carreraId) {
        List<RegistroActualizacionCarrera> registros = (carreraId == null)
                ? registroRepository.findAllByOrderByFechaDesc()
                : registroRepository.findByCarreraIdOrderByFechaDesc(carreraId);

        return registros.stream()
                .map(r -> new RegistroActualizacionRespuesta(
                        r.getId(),
                        r.getCarrera().getId(),
                        r.getCarrera().getNombre(),
                        r.getCampo(),
                        r.getValorAnterior(),
                        r.getValorNuevo(),
                        r.getFecha(),
                        r.getResponsable()))
                .toList();
    }

    // ---------- Registro de cambios ----------

    // Acumula un registro por cada campo que realmente cambia y aplica el valor nuevo.
    private static final class Registrador {
        private final Carrera carrera;
        private final LocalDateTime fecha;
        private final String responsable;
        private final List<RegistroActualizacionCarrera> registros = new ArrayList<>();

        private Registrador(Carrera carrera, LocalDateTime fecha, String responsable) {
            this.carrera = carrera;
            this.fecha = fecha;
            this.responsable = responsable;
        }

        private <T> void aplicar(String campo, T actual, T nuevo, Consumer<T> asignar) {
            if (iguales(actual, nuevo)) {
                return;
            }
            registros.add(new RegistroActualizacionCarrera(
                    carrera, campo, comoTexto(actual), comoTexto(nuevo), fecha, responsable));
            asignar.accept(nuevo);
        }
    }

    // compareTo ignora diferencias de escala (8500.0 frente a 8500.00).
    private static boolean iguales(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) {
            return x.compareTo(y) == 0;
        }
        return Objects.equals(a, b);
    }

    private static String comoTexto(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (valor instanceof List<?> lista) {
            return lista.isEmpty()
                    ? null
                    : lista.stream().map(String::valueOf).collect(Collectors.joining(" | "));
        }
        return valor.toString();
    }

    // ---------- Lectura y validacion de cada tipo de campo ----------

    private static String textoOpcional(JsonNode nodo, String campo, int maximo) {
        if (nodo.isNull()) {
            return null;
        }
        if (!nodo.isTextual()) {
            throw new ExcepcionCarrera("El campo " + campo + " debe ser texto");
        }
        String texto = nodo.asText().trim();
        if (texto.isEmpty()) {
            return null;
        }
        if (texto.length() > maximo) {
            throw new ExcepcionCarrera(
                    "El campo " + campo + " no puede superar " + maximo + " caracteres");
        }
        return texto;
    }

    private static String textoRequerido(JsonNode nodo, String campo, int maximo) {
        String texto = textoOpcional(nodo, campo, maximo);
        if (texto == null) {
            throw new ExcepcionCarrera("El campo " + campo + " es obligatorio");
        }
        return texto;
    }

    private static Integer enteroOpcional(JsonNode nodo, String campo) {
        if (nodo.isNull()) {
            return null;
        }
        if (!nodo.isIntegralNumber() || !nodo.canConvertToInt()) {
            throw new ExcepcionCarrera("El campo " + campo + " debe ser un número entero");
        }
        return nodo.intValue();
    }

    private static Integer enteroRequerido(JsonNode nodo, String campo) {
        Integer valor = enteroOpcional(nodo, campo);
        if (valor == null) {
            throw new ExcepcionCarrera("El campo " + campo + " es obligatorio");
        }
        return valor;
    }

    private static BigDecimal decimalOpcional(JsonNode nodo, String campo) {
        if (nodo.isNull()) {
            return null;
        }
        if (!nodo.isNumber()) {
            throw new ExcepcionCarrera("El campo " + campo + " debe ser un número");
        }
        double valor = nodo.doubleValue();
        if (!Double.isFinite(valor) || Math.abs(valor) > MAX_VALOR_NUMERICO) {
            throw new ExcepcionCarrera("El campo " + campo + " está fuera del rango permitido");
        }
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private static List<String> listaTexto(JsonNode nodo, String campo) {
        List<String> lista = new ArrayList<>();
        if (nodo.isNull()) {
            return lista;
        }
        if (!nodo.isArray()) {
            throw new ExcepcionCarrera("El campo " + campo + " debe ser una lista de textos");
        }
        if (nodo.size() > MAX_ELEMENTOS_LISTA) {
            throw new ExcepcionCarrera(
                    "El campo " + campo + " admite como máximo " + MAX_ELEMENTOS_LISTA + " elementos");
        }
        for (JsonNode elemento : nodo) {
            if (!elemento.isTextual()) {
                throw new ExcepcionCarrera("El campo " + campo + " debe contener solo textos");
            }
            String texto = elemento.asText().trim();
            if (texto.isEmpty() || lista.contains(texto)) {
                continue;
            }
            if (texto.length() > MAX_LARGO_ELEMENTO) {
                throw new ExcepcionCarrera("Cada elemento de " + campo + " puede tener como máximo "
                        + MAX_LARGO_ELEMENTO + " caracteres");
            }
            lista.add(texto);
        }
        return lista;
    }

    // Solo https: es lo unico que la vista publica muestra como enlace.
    private static String urlOpcional(JsonNode nodo, String campo) {
        String url = textoOpcional(nodo, campo, 500);
        if (url == null) {
            return null;
        }
        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new ExcepcionCarrera("El campo " + campo + " debe ser una URL https válida");
            }
        } catch (URISyntaxException e) {
            throw new ExcepcionCarrera("El campo " + campo + " debe ser una URL https válida");
        }
        return url;
    }
}
