package com.rumbo.modelo;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "revision_mensaje")
public class RevisionMensaje {

    public enum Decision {
        VALIDAR,
        ELIMINAR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "publicacion_id",
            nullable = false,
            unique = true,
            updatable = false
    )
    private Publicacion publicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "supervisor_id",
            nullable = false,
            updatable = false
    )
    private Usuario supervisor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private Decision decision;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    protected RevisionMensaje() {
    }

    public RevisionMensaje(
            Publicacion publicacion,
            Usuario supervisor,
            Decision decision) {

        this.publicacion = publicacion;
        this.supervisor = supervisor;
        this.decision = decision;
        this.fecha = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public Usuario getSupervisor() {
        return supervisor;
    }

    public Decision getDecision() {
        return decision;
    }

    public Instant getFecha() {
        return fecha;
    }
}