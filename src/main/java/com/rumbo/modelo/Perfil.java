package com.rumbo.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "perfil")
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idPerfil;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    private String nivelEstudios;
    private String descripcionPersonal;
    private String areaLaboral;

    @Column(name = "foto_public_id", length = 255)
    private String fotoPublicId;

    @Column(name = "foto_url", columnDefinition = "text")
    private String fotoUrl;

    @ManyToOne
    @JoinColumn(name = "id_carrera")
    private Carrera carrera;

    protected Perfil() {
    }

    public Perfil(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getIdPerfil() {
        return idPerfil;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getNivelEstudios() {
        return nivelEstudios;
    }

    public void setNivelEstudios(String nivelEstudios) {
        this.nivelEstudios = nivelEstudios;
    }

    public String getDescripcionPersonal() {
        return descripcionPersonal;
    }

    public void setDescripcionPersonal(String descripcionPersonal) {
        this.descripcionPersonal = descripcionPersonal;
    }

    public String getAreaLaboral() {
        return areaLaboral;
    }

    public void setAreaLaboral(String areaLaboral) {
        this.areaLaboral = areaLaboral;
    }

    public String getFotoPublicId() {
        return fotoPublicId;
    }

    public void setFotoPublicId(String fotoPublicId) {
        this.fotoPublicId = fotoPublicId;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public Carrera getCarrera() {
        return carrera;
    }

    public void setCarrera(Carrera carrera) {
        this.carrera = carrera;
    }
}
