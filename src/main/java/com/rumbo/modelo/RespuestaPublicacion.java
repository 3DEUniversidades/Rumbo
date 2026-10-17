package com.rumbo.modelo;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "respuesta_publicacion")
public class RespuestaPublicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(nullable = false)
    private Instant fecha;

    protected RespuestaPublicacion() {
    }

    public RespuestaPublicacion(
            Publicacion publicacion,
            Usuario autor,
            String contenido) {

        this.publicacion = publicacion;
        this.autor = autor;
        this.contenido = contenido;
        this.fecha = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public Usuario getAutor() {
        return autor;
    }

    public String getContenido() {
        return contenido;
    }

    public Instant getFecha() {
        return fecha;
    }
}