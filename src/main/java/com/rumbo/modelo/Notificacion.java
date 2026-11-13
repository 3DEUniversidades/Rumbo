package com.rumbo.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idNotificacion;
    private String idUsuario;
    private String titulo;
    private String mensaje;
    private boolean leida;
    private String fecha;

    protected Notificacion() {
    }

    public Notificacion(String idUsuario, String titulo, String mensaje) {
        this.idUsuario = idUsuario;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.fecha = Instant.now().toString();
    }

    public String getIdNotificacion() { return idNotificacion; }
    public String getIdUsuario() { return idUsuario; }
    public String getTitulo() { return titulo; }
    public String getMensaje() { return mensaje; }
    public boolean isLeida() { return leida; }
    public String getFecha() { return fecha; }
    public void marcarLeida() { this.leida = true; }
}
