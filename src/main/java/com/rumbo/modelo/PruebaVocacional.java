package com.rumbo.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "prueba_vocacional")
public class PruebaVocacional {
    @Id
    @Column(name = "id_prueba")
    private String idPrueba;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_actividad", nullable = false)
    private Actividad actividad;
    @Column(nullable = false, length = 160)
    private String nombre;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPruebaVocacional tipo;
    @Column(nullable = false, length = 160)
    private String proveedor;
    @Column(nullable = false, columnDefinition = "text")
    private String proposito;
    @Column(nullable = false, columnDefinition = "text")
    private String contexto;
    @Column(nullable = false, columnDefinition = "text")
    private String instrucciones;
    @Column(nullable = false, columnDefinition = "text")
    private String metodo;
    @Column(nullable = false, length = 1000)
    private String enlaceExterno;
    @Column(nullable = false, length = 1000)
    private String recursoInterpretacionUrl;
    @Column(nullable = false, columnDefinition = "text")
    private String orientacionInterpretacion;
    @Column(nullable = false, columnDefinition = "text")
    private String condicionesAcceso;

    protected PruebaVocacional() {}

    public PruebaVocacional(String idPrueba, Actividad actividad, String nombre,
            TipoPruebaVocacional tipo, String proveedor, String proposito,
            String contexto, String instrucciones, String metodo, String enlaceExterno,
            String recursoInterpretacionUrl, String orientacionInterpretacion, String condicionesAcceso) {
        this.idPrueba = idPrueba; this.actividad = actividad; this.nombre = nombre;
        this.tipo = tipo; this.proveedor = proveedor; this.proposito = proposito;
        this.contexto = contexto; this.instrucciones = instrucciones; this.metodo = metodo;
        this.enlaceExterno = enlaceExterno; this.recursoInterpretacionUrl = recursoInterpretacionUrl;
        this.orientacionInterpretacion = orientacionInterpretacion; this.condicionesAcceso = condicionesAcceso;
    }
    public String getIdPrueba() { return idPrueba; }
    public Actividad getActividad() { return actividad; }
    public String getNombre() { return nombre; }
    public TipoPruebaVocacional getTipo() { return tipo; }
    public String getProveedor() { return proveedor; }
    public String getProposito() { return proposito; }
    public String getContexto() { return contexto; }
    public String getInstrucciones() { return instrucciones; }
    public String getMetodo() { return metodo; }
    public String getEnlaceExterno() { return enlaceExterno; }
    public String getRecursoInterpretacionUrl() { return recursoInterpretacionUrl; }
    public String getOrientacionInterpretacion() { return orientacionInterpretacion; }
    public String getCondicionesAcceso() { return condicionesAcceso; }
}
