package com.rumbo.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carreras")
public class Carrera {

    // atributos

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 120)
    private String facultad;

    @Column(columnDefinition = "text")
    private String descripcion;

    // Anios del plan base.
    @Column(nullable = false)
    private Integer duracion;

    // Total calculado del primer semestre de primer ingreso, incluida la inscripcion.
    // No es costo anual ni de toda la carrera: ver costoPeriodo y costoAnio.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costo;

    @Column(length = 200)
    private String costoPeriodo;

    private Integer costoAnio;

    @Column(length = 80)
    private String modalidad;

    private Integer cargaCreditos;

    @Convert(converter = ConversorListaTexto.class)
    @Column(columnDefinition = "text")
    private List<String> areasFormacion = new ArrayList<>();

    // Lo que el estudiante aprendera a hacer; no son requisitos de ingreso.
    @Convert(converter = ConversorListaTexto.class)
    @Column(columnDefinition = "text")
    private List<String> competenciasDesarrolladas = new ArrayList<>();

    @Convert(converter = ConversorListaTexto.class)
    @Column(columnDefinition = "text")
    private List<String> oportunidadesLaborales = new ArrayList<>();

    @Column(precision = 12, scale = 2)
    private BigDecimal salarioEstimado;

    @Column(precision = 5, scale = 2)
    private BigDecimal empleabilidad;

    // Periodo y alcance del salario e informacion laboral cuando no hay porcentaje verificado.
    @Column(columnDefinition = "text")
    private String contextoLaboral;

    @Column(length = 500)
    private String pdfOficialUrl;

    @Column(length = 500)
    private String urlCarreraOficial;


    // metodos

    protected Carrera() {
        // Lo necesita JPA para recuperar carreras de la base de datos.
    }

    public Carrera(String nombre, Integer duracion, BigDecimal costo) {
        this.nombre = nombre;
        this.duracion = duracion;
        this.costo = costo;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getFacultad() {
        return facultad;
    }

    public void setFacultad(String facultad) {
        this.facultad = facultad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getDuracion() {
        return duracion;
    }

    public void setDuracion(Integer duracion) {
        this.duracion = duracion;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    public String getCostoPeriodo() {
        return costoPeriodo;
    }

    public void setCostoPeriodo(String costoPeriodo) {
        this.costoPeriodo = costoPeriodo;
    }

    public Integer getCostoAnio() {
        return costoAnio;
    }

    public void setCostoAnio(Integer costoAnio) {
        this.costoAnio = costoAnio;
    }

    public String getModalidad() {
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public Integer getCargaCreditos() {
        return cargaCreditos;
    }

    public void setCargaCreditos(Integer cargaCreditos) {
        this.cargaCreditos = cargaCreditos;
    }

    public List<String> getAreasFormacion() {
        return new ArrayList<>(areasFormacion);
    }

    public void setAreasFormacion(List<String> areasFormacion) {
        this.areasFormacion = limpiar(areasFormacion);
    }

    public List<String> getCompetenciasDesarrolladas() {
        return new ArrayList<>(competenciasDesarrolladas);
    }

    public void setCompetenciasDesarrolladas(List<String> competenciasDesarrolladas) {
        this.competenciasDesarrolladas = limpiar(competenciasDesarrolladas);
    }

    public List<String> getOportunidadesLaborales() {
        return new ArrayList<>(oportunidadesLaborales);
    }

    public void setOportunidadesLaborales(List<String> oportunidadesLaborales) {
        this.oportunidadesLaborales = limpiar(oportunidadesLaborales);
    }

    public BigDecimal getSalarioEstimado() {
        return salarioEstimado;
    }

    public void setSalarioEstimado(BigDecimal salarioEstimado) {
        this.salarioEstimado = salarioEstimado;
    }

    public BigDecimal getEmpleabilidad() {
        return empleabilidad;
    }

    public void setEmpleabilidad(BigDecimal empleabilidad) {
        this.empleabilidad = empleabilidad;
    }

    public String getContextoLaboral() {
        return contextoLaboral;
    }

    public void setContextoLaboral(String contextoLaboral) {
        this.contextoLaboral = contextoLaboral;
    }

    public String getPdfOficialUrl() {
        return pdfOficialUrl;
    }

    public void setPdfOficialUrl(String pdfOficialUrl) {
        this.pdfOficialUrl = pdfOficialUrl;
    }

    public String getUrlCarreraOficial() {
        return urlCarreraOficial;
    }

    public void setUrlCarreraOficial(String urlCarreraOficial) {
        this.urlCarreraOficial = urlCarreraOficial;
    }

    // Copia la lista sin elementos vacios; una lista nula equivale a una lista vacia.
    private static List<String> limpiar(List<String> lista) {
        List<String> limpia = new ArrayList<>();
        if (lista == null) {
            return limpia;
        }
        for (String elemento : lista) {
            if (elemento != null && !elemento.isBlank()) {
                limpia.add(elemento.trim());
            }
        }
        return limpia;
    }
}
