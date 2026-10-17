package com.rumbo.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "registro_actividad_estudiante",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_registro_usuario_actividad",
        columnNames = {"id_usuario", "id_actividad"}
    )
)
public class RegistroActividadEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_registro")
    private String idRegistro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_actividad", nullable = false)
    private Actividad actividad;

    @Column(name = "decidio_realizar")
    private Boolean decidioRealizar;

    @Column(name = "razon_decision", columnDefinition = "text")
    private String razonDecision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoActividad estado = EstadoActividad.PENDIENTE;

    @Column(name = "resultados_registrados", columnDefinition = "text")
    private String resultadosRegistrados;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    protected RegistroActividadEstudiante() {
        // Constructor requerido por JPA
    }

    public RegistroActividadEstudiante(Usuario usuario, Actividad actividad) {
        this.usuario = usuario;
        this.actividad = actividad;
    }

    @PrePersist
    @PreUpdate
    private void actualizarFecha() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    public String getIdRegistro() {
        return idRegistro;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Actividad getActividad() {
        return actividad;
    }

    public Boolean getDecidioRealizar() {
        return decidioRealizar;
    }

    public void setDecidioRealizar(Boolean decidioRealizar) {
        this.decidioRealizar = decidioRealizar;
    }

    public String getRazonDecision() {
        return razonDecision;
    }

    public void setRazonDecision(String razonDecision) {
        this.razonDecision = razonDecision;
    }

    public EstadoActividad getEstado() {
        return estado;
    }

    public void setEstado(EstadoActividad estado) {
        this.estado = estado;
    }

    public String getResultadosRegistrados() {
        return resultadosRegistrados;
    }

    public void setResultadosRegistrados(String resultadosRegistrados) {
        this.resultadosRegistrados = resultadosRegistrados;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}