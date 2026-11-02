package com.rumbo.modelo;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "registro_administracion")
public class RegistroAdministracion {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String idRegistro;
    @Column(nullable = false) private String idAdministrador;
    @Column(nullable = false) private String nombreAdministrador;
    @Column(nullable = false) private String idObjetivo;
    @Column(nullable = false) private String usuarioObjetivo;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Rol rolObjetivo;
    @Column(nullable = false) private String accion;
    @Column(nullable = false, length = 1000) private String motivo;
    @Column(nullable = false) private Instant fecha;
    private Long idPublicacion;
    @Column(length = 160) private String tituloPublicacion;
    protected RegistroAdministracion() {}
    public RegistroAdministracion(Usuario administrador, Usuario objetivo, String motivo) {
        this.idAdministrador = administrador.getIdUsuario();
        this.nombreAdministrador = administrador.getNombreUsuario();
        this.idObjetivo = objetivo.getIdUsuario();
        this.usuarioObjetivo = objetivo.getNombreUsuario();
        this.rolObjetivo = objetivo.getRol();
        this.accion = "ELIMINAR_CUENTA"; this.motivo = motivo; this.fecha = Instant.now();
    }
    public RegistroAdministracion(Usuario administrador, Publicacion publicacion, String motivo) {
        this(administrador, publicacion.getAutor(), motivo);
        this.accion = "ELIMINAR_PUBLICACION";
        this.idPublicacion = publicacion.getId();
        this.tituloPublicacion = publicacion.getTitulo();
    }
    public Long getIdPublicacion() { return idPublicacion; }
    public String getTituloPublicacion() { return tituloPublicacion; }
    public String getIdRegistro() { return idRegistro; }
    public String getIdAdministrador() { return idAdministrador; }
    public String getNombreAdministrador() { return nombreAdministrador; }
    public String getIdObjetivo() { return idObjetivo; }
    public String getUsuarioObjetivo() { return usuarioObjetivo; }
    public Rol getRolObjetivo() { return rolObjetivo; }
    public String getAccion() { return accion; }
    public String getMotivo() { return motivo; }
    public Instant getFecha() { return fecha; }
}
