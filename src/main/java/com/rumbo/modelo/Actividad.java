package com.rumbo.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "actividad")
public class Actividad {

    @Id
    @Column(name = "id_actividad")
    private String idActividad;
    private String fecha;
    private String descripcion;
    private String contexto;
    private String instrucciones;
    @Column(name = "acciones_a_realizar")
    private String accionesARealizar;
    @Column(name = "metodo_recomendado")
    private String metodoRecomendado;
    @Column(name = "herramientas_disponibles")
    private String herramientasDisponibles;
    @Column(name = "informacion_a_guardar")
    private String informacionAGuardar;
    @Column(name = "tipo_actividad")
    private String tipoActividad;

    // Eliminacion logica: conserva el historial personal de los estudiantes.
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean eliminada = false;

    public boolean isEliminada() {
        return eliminada;
    }

    public void retirarDeLineaDeTiempo() {
        this.eliminada = true;
    }

    protected Actividad() {
    }

    public Actividad(String idActividad, String fecha, String descripcion, String contexto,
                     String instrucciones, String accionesARealizar, String metodoRecomendado,
                     String herramientasDisponibles, String informacionAGuardar,
                     String tipoActividad) {
        this.idActividad = idActividad;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.contexto = contexto;
        this.instrucciones = instrucciones;
        this.accionesARealizar = accionesARealizar;
        this.metodoRecomendado = metodoRecomendado;
        this.herramientasDisponibles = herramientasDisponibles;
        this.informacionAGuardar = informacionAGuardar;
        this.tipoActividad = tipoActividad;
    }

    public String getIdActividad() {
        return idActividad;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getContexto() {
        return contexto;
    }

    public void setContexto(String contexto) {
        this.contexto = contexto;
    }

    public String getInstrucciones() {
        return instrucciones;
    }

    public void setInstrucciones(String instrucciones) {
        this.instrucciones = instrucciones;
    }

    public String getAccionesARealizar() {
        return accionesARealizar;
    }

    public void setAccionesARealizar(String accionesARealizar) {
        this.accionesARealizar = accionesARealizar;
    }

    public String getMetodoRecomendado() {
        return metodoRecomendado;
    }

    public void setMetodoRecomendado(String metodoRecomendado) {
        this.metodoRecomendado = metodoRecomendado;
    }

    public String getHerramientasDisponibles() {
        return herramientasDisponibles;
    }

    public void setHerramientasDisponibles(String herramientasDisponibles) {
        this.herramientasDisponibles = herramientasDisponibles;
    }

    public String getInformacionAGuardar() {
        return informacionAGuardar;
    }

    public void setInformacionAGuardar(String informacionAGuardar) {
        this.informacionAGuardar = informacionAGuardar;
    }

    public String getTipoActividad() {
        return tipoActividad;
    }

    public void setTipoActividad(String tipoActividad) {
        this.tipoActividad = tipoActividad;
    }
}
