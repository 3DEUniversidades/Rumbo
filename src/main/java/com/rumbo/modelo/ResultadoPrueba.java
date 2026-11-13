package com.rumbo.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "resultado_prueba", uniqueConstraints = @UniqueConstraint(
        name = "uk_resultado_prueba_usuario", columnNames = {"id_usuario", "id_prueba"}))
public class ResultadoPrueba {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_resultado")
    private String idResultado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_prueba", nullable = false)
    private PruebaVocacional prueba;
    @Column(nullable = false)
    private LocalDate fechaRealizacion;
    @Column(nullable = false, columnDefinition = "text")
    private String resultado;
    @Column(columnDefinition = "text")
    private String interpretacionPersonal;
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    protected ResultadoPrueba() {}
    public ResultadoPrueba(Usuario usuario, PruebaVocacional prueba) {
        this.usuario = usuario; this.prueba = prueba;
    }
    public void actualizar(LocalDate fecha, String resultado, String interpretacion) {
        this.fechaRealizacion = fecha; this.resultado = resultado;
        this.interpretacionPersonal = interpretacion;
        this.fechaActualizacion = LocalDateTime.now();
    }
    @PrePersist
    @PreUpdate
    private void registrarActualizacion() { fechaActualizacion = LocalDateTime.now(); }
    public String getIdResultado() { return idResultado; }
    public Usuario getUsuario() { return usuario; }
    public PruebaVocacional getPrueba() { return prueba; }
    public LocalDate getFechaRealizacion() { return fechaRealizacion; }
    public String getResultado() { return resultado; }
    public String getInterpretacionPersonal() { return interpretacionPersonal; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
