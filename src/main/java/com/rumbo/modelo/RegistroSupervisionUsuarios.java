package com.rumbo.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "registro_supervision_usuarios")
public class RegistroSupervisionUsuarios {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idRegistro;
    private String idSupervisor;
    private String idSolicitud;
    private String tipo;
    private String decision;
    private String motivo;
    private String fecha;

    protected RegistroSupervisionUsuarios() {
    }

    public RegistroSupervisionUsuarios(String idSupervisor, String idSolicitud,
                                       String tipo, String decision, String motivo) {
        this.idSupervisor = idSupervisor;
        this.idSolicitud = idSolicitud;
        this.tipo = tipo;
        this.decision = decision;
        this.motivo = motivo;
        this.fecha = Instant.now().toString();
    }

    public String getIdRegistro() { return idRegistro; }
    public String getIdSupervisor() { return idSupervisor; }
    public String getIdSolicitud() { return idSolicitud; }
    public String getTipo() { return tipo; }
    public String getDecision() { return decision; }
    public String getMotivo() { return motivo; }
    public String getFecha() { return fecha; }
}
