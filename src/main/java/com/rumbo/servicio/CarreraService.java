package com.rumbo.servicio;

import com.rumbo.dto.ActualizarCarreraSolicitud;
import com.rumbo.dto.RegistroActualizacionRespuesta;
import com.rumbo.modelo.Carrera;
import com.rumbo.modelo.RegistroActualizacionCarrera;
import com.rumbo.persistencia.CarreraRepository;
import com.rumbo.persistencia.RegistroActualizacionCarreraRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarreraService {

    private final CarreraRepository carreraRepository;
    private final RegistroActualizacionCarreraRepository registroRepository;

    public CarreraService(CarreraRepository carreraRepository,
                          RegistroActualizacionCarreraRepository registroRepository) {
        this.carreraRepository = carreraRepository;
        this.registroRepository = registroRepository;
    }

    public List<Carrera> consultarCarreras() {
        return carreraRepository.findAll();
    }

    public Optional<Carrera> buscarPorId(Long id) {
        return carreraRepository.findById(id);
    }

    @Transactional
    public Carrera actualizarCarrera(Long id, ActualizarCarreraSolicitud s, String responsable) {
        Carrera carrera = carreraRepository.findById(id)
                .orElseThrow(ExcepcionCarreraNoEncontrada::new);

        validar(s);

        LocalDateTime ahora = LocalDateTime.now();
        List<RegistroActualizacionCarrera> registros = new ArrayList<>();

        if (s.nombre() != null && !s.nombre().equals(carrera.getNombre())) {
            registros.add(registro(carrera, "nombre", carrera.getNombre(), s.nombre(), ahora, responsable));
            carrera.setNombre(s.nombre().trim());
        }
        if (s.duracion() != null && !s.duracion().equals(carrera.getDuracion())) {
            registros.add(registro(carrera, "duracion", carrera.getDuracion(), s.duracion(), ahora, responsable));
            carrera.setDuracion(s.duracion());
        }
        if (s.costo() != null && cambio(carrera.getCosto(), s.costo())) {
            registros.add(registro(carrera, "costo", carrera.getCosto(), s.costo(), ahora, responsable));
            carrera.setCosto(s.costo());
        }
        if (s.modalidad() != null && !s.modalidad().equals(carrera.getModalidad())) {
            registros.add(registro(carrera, "modalidad", carrera.getModalidad(), s.modalidad(), ahora, responsable));
            carrera.setModalidad(s.modalidad());
        }
        if (s.cargaCreditos() != null && !s.cargaCreditos().equals(carrera.getCargaCreditos())) {
            registros.add(registro(carrera, "cargaCreditos", carrera.getCargaCreditos(), s.cargaCreditos(), ahora, responsable));
            carrera.setCargaCreditos(s.cargaCreditos());
        }
        if (s.habilidadesRequeridas() != null && !s.habilidadesRequeridas().equals(carrera.getHabilidadesRequeridas())) {
            registros.add(registro(carrera, "habilidadesRequeridas", carrera.getHabilidadesRequeridas(), s.habilidadesRequeridas(), ahora, responsable));
            carrera.setHabilidadesRequeridas(s.habilidadesRequeridas());
        }
        if (s.oportunidadesLaborales() != null && !s.oportunidadesLaborales().equals(carrera.getOportunidadesLaborales())) {
            registros.add(registro(carrera, "oportunidadesLaborales", carrera.getOportunidadesLaborales(), s.oportunidadesLaborales(), ahora, responsable));
            carrera.setOportunidadesLaborales(s.oportunidadesLaborales());
        }
        if (s.salarioEstimado() != null && cambio(carrera.getSalarioEstimado(), s.salarioEstimado())) {
            registros.add(registro(carrera, "salarioEstimado", carrera.getSalarioEstimado(), s.salarioEstimado(), ahora, responsable));
            carrera.setSalarioEstimado(s.salarioEstimado());
        }
        if (s.empleabilidad() != null && cambio(carrera.getEmpleabilidad(), s.empleabilidad())) {
            registros.add(registro(carrera, "empleabilidad", carrera.getEmpleabilidad(), s.empleabilidad(), ahora, responsable));
            carrera.setEmpleabilidad(s.empleabilidad());
        }
        if (s.pdfOficialUrl() != null && !s.pdfOficialUrl().equals(carrera.getPdfOficialUrl())) {
            registros.add(registro(carrera, "pdfOficialUrl", carrera.getPdfOficialUrl(), s.pdfOficialUrl(), ahora, responsable));
            carrera.setPdfOficialUrl(s.pdfOficialUrl());
        }
        if (s.urlCarreraOficial() != null && !s.urlCarreraOficial().equals(carrera.getUrlCarreraOficial())) {
            registros.add(registro(carrera, "urlCarreraOficial", carrera.getUrlCarreraOficial(), s.urlCarreraOficial(), ahora, responsable));
            carrera.setUrlCarreraOficial(s.urlCarreraOficial());
        }

        carreraRepository.save(carrera);
        registroRepository.saveAll(registros);
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

    private RegistroActualizacionCarrera registro(Carrera carrera, String campo, Object anterior,
                                                  Object nuevo, LocalDateTime fecha, String responsable) {
        return new RegistroActualizacionCarrera(
                carrera, campo,
                anterior == null ? null : anterior.toString(),
                nuevo.toString(), fecha, responsable);
    }

    // ignora diferencias en la escala
    private boolean cambio(BigDecimal actual, BigDecimal nuevo) {
        return actual == null || actual.compareTo(nuevo) != 0;
    }

    private void validar(ActualizarCarreraSolicitud s) {
        if (s.nombre() != null && s.nombre().isBlank()) {
            throw new ExcepcionCarrera("El nombre no puede estar vacío");
        }
        if (s.duracion() != null && s.duracion() <= 0) {
            throw new ExcepcionCarrera("La duración debe ser mayor a 0");
        }
        if (s.costo() != null && s.costo().signum() < 0) {
            throw new ExcepcionCarrera("El costo no puede ser negativo");
        }
        if (s.cargaCreditos() != null && s.cargaCreditos() <= 0) {
            throw new ExcepcionCarrera("La carga de créditos debe ser mayor a 0");
        }
        if (s.salarioEstimado() != null && s.salarioEstimado().signum() < 0) {
            throw new ExcepcionCarrera("El salario estimado no puede ser negativo");
        }
        if (s.empleabilidad() != null
                && (s.empleabilidad().signum() < 0 || s.empleabilidad().compareTo(new BigDecimal("100")) > 0)) {
            throw new ExcepcionCarrera("La empleabilidad debe estar entre 0 y 100");
        }
        validarUrl(s.pdfOficialUrl(), "El PDF oficial");
        validarUrl(s.urlCarreraOficial(), "El enlace oficial");
    }

    private void validarUrl(String url, String etiqueta) {
        if (url != null && !url.isBlank()
                && !(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new ExcepcionCarrera(etiqueta + " debe ser una URL válida (http/https)");
        }
    }
}