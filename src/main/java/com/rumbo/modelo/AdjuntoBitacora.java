package com.rumbo.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "adjunto_bitacora")
public class AdjuntoBitacora {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_adjunto")
    private String idAdjunto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_entrada", nullable = false)
    private EntradaBitacora entrada;

    @Column(name = "public_id", nullable = false, length = 500)
    private String publicId;

    @Column(name = "tipo_recurso", nullable = false, length = 20)
    private String tipoRecurso;

    @Column(name = "tipo_entrega", nullable = false, length = 20)
    private String tipoEntrega;

    @Column(length = 20)
    private String formato;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "tipo_mime", nullable = false, length = 100)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    protected AdjuntoBitacora() {
        // Constructor requerido por JPA
    }

    public AdjuntoBitacora(
            EntradaBitacora entrada,
            String publicId,
            String tipoRecurso,
            String tipoEntrega,
            String formato,
            String nombreOriginal,
            String tipoMime,
            long tamanoBytes
    ) {
        this.entrada = entrada;
        this.publicId = publicId;
        this.tipoRecurso = tipoRecurso;
        this.tipoEntrega = tipoEntrega;
        this.formato = formato;
        this.nombreOriginal = nombreOriginal;
        this.tipoMime = tipoMime;
        this.tamanoBytes = tamanoBytes;
    }

    @PrePersist
    private void prepararCreacion() {
        if (fechaSubida == null) {
            fechaSubida = LocalDateTime.now();
        }
    }

    public String getIdAdjunto() {
        return idAdjunto;
    }

    public EntradaBitacora getEntrada() {
        return entrada;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getTipoRecurso() {
        return tipoRecurso;
    }

    public String getTipoEntrega() {
        return tipoEntrega;
    }

    public String getFormato() {
        return formato;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }
}
