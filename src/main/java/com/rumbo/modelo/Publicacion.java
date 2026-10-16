package com.rumbo.modelo;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "publicacion")
public class Publicacion {

    public enum Tipo {
        DUDA,
        EXPERIENCIA,
        PROYECTO,
        ACTIVIDAD,
        OPORTUNIDAD
    }

    public enum Estado {
        ACTIVA,
        VALIDADA,
        ELIMINADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Tipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado;

    @Column(nullable = false)
    private Instant fecha;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "publicacion_carrera",
            joinColumns = @JoinColumn(name = "publicacion_id"),
            inverseJoinColumns = @JoinColumn(name = "carrera_id")
    )
    private Set<Carrera> carreras = new LinkedHashSet<>();

    protected Publicacion() {
    }

    public Publicacion(
            Usuario autor,
            String titulo,
            String contenido,
            Tipo tipo,
            Collection<Carrera> carreras) {

        this.autor = autor;
        this.titulo = titulo;
        this.contenido = contenido;
        this.tipo = tipo;
        this.estado = Estado.ACTIVA;
        this.fecha = Instant.now();
        this.carreras.addAll(carreras);
    }

    public Long getId() {
        return id;
    }

    public Usuario getAutor() {
        return autor;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getContenido() {
        return contenido;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public Estado getEstado() {
        return estado;
    }

    public Instant getFecha() {
        return fecha;
    }

    public Set<Carrera> getCarreras() {
        return new LinkedHashSet<>(carreras);
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }   

}