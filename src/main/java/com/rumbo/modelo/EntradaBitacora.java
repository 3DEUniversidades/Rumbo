package com.rumbo.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "entrada_bitacora")
public class EntradaBitacora {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_entrada")
    private String idEntrada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_actividad")
    private Actividad actividad;

    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_edicion")
    private LocalDateTime fechaEdicion;

    @OneToMany(
            mappedBy = "entrada",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("fechaSubida ASC")
    private List<AdjuntoBitacora> adjuntos = new ArrayList<>();

    protected EntradaBitacora() {
        // Constructor requerido por JPA
    }

    public EntradaBitacora(Usuario usuario, String contenido, Actividad actividad) {
        this.usuario = usuario;
        this.contenido = contenido;
        this.actividad = actividad;
    }

    @PrePersist
    private void prepararCreacion() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
    }

    public void actualizarContenido(String contenido) {
        this.contenido = contenido;
        this.fechaEdicion = LocalDateTime.now();
    }

    public void relacionarActividad(Actividad actividad) {
        this.actividad = actividad;
    }

    public String getIdEntrada() {
        return idEntrada;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Actividad getActividad() {
        return actividad;
    }

    public String getContenido() {
        return contenido;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaEdicion() {
        return fechaEdicion;
    }

    public List<AdjuntoBitacora> getAdjuntos() {
        return adjuntos;
    }

    public void agregarAdjunto(AdjuntoBitacora adjunto) {
        adjuntos.add(adjunto);
    }

    public void quitarAdjunto(AdjuntoBitacora adjunto) {
        adjuntos.remove(adjunto);
    }
}
