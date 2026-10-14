package com.rumbo.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "carreras")
public class Carrera {

    // atributos

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false)
    private Integer duracion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costo;

    @Column(length = 80)
    private String modalidad;

    private Integer cargaCreditos;

    @Column(columnDefinition = "text")
    private String habilidadesRequeridas;

    @Column(columnDefinition = "text")
    private String oportunidadesLaborales;

    @Column(precision = 12, scale = 2)
    private BigDecimal salarioEstimado;

    @Column(precision = 5, scale = 2)
    private BigDecimal empleabilidad;

    @Column(length = 500)
    private String pdfOficialUrl;

    @Column(length = 500)
    private String urlCarreraOficial;


    // métodos

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

    public String getHabilidadesRequeridas() {
        return habilidadesRequeridas;
    }

    public void setHabilidadesRequeridas(String habilidadesRequeridas) {
        this.habilidadesRequeridas = habilidadesRequeridas;
    }

    public String getOportunidadesLaborales() {
        return oportunidadesLaborales;
    }

    public void setOportunidadesLaborales(String oportunidadesLaborales) {
        this.oportunidadesLaborales = oportunidadesLaborales;
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
    
}
