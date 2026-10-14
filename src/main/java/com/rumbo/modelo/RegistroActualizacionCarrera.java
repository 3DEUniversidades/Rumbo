package com.rumbo.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "registro_actualizacion_carrera")
public class RegistroActualizacionCarrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrera_id", nullable = false)
    private Carrera carrera;

    @Column(nullable = false, length = 60)
    private String campo;

    @Column(name = "valor_anterior", columnDefinition = "text")
    private String valorAnterior;

    @Column(name = "valor_nuevo", columnDefinition = "text")
    private String valorNuevo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    // idUsuario de quien hizo el cambio (Usuario.idUsuario)
    @Column(nullable = false, length = 60)
    private String responsable;

    protected RegistroActualizacionCarrera() {
        // Lo necesita JPA.
    }

    public RegistroActualizacionCarrera(Carrera carrera, String campo, String valorAnterior,
                                        String valorNuevo, LocalDateTime fecha, String responsable) {
        this.carrera = carrera;
        this.campo = campo;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        this.fecha = fecha;
        this.responsable = responsable;
    }

    public Long getId() {
        return id;
    }

    public Carrera getCarrera() {
        return carrera;
    }

    public String getCampo() {
        return campo;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getResponsable() {
        return responsable;
    }
}