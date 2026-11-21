package co.edu.unad.alertapp.notificacion;

import co.edu.unad.alertapp.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notificacion")
public class Notificacion {

    public enum Tipo { REPORTE_CERCANO, PANICO_PROTEGIDO, SEGUIMIENTO_PROTEGIDO, GUARDIAN_AGREGADO, ESTADO_REPORTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Tipo tipo;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 300)
    private String cuerpo;

    @Column(length = 120)
    private String enlace;

    private Double latitud;

    private Double longitud;

    @Column(nullable = false)
    private boolean leida;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    protected Notificacion() {
    }

    public Notificacion(Usuario usuario, Tipo tipo, String titulo, String cuerpo, String enlace,
                        Double latitud, Double longitud) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.titulo = titulo;
        this.cuerpo = cuerpo;
        this.enlace = enlace;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public void marcarLeida() { this.leida = true; }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Tipo getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getCuerpo() { return cuerpo; }
    public String getEnlace() { return enlace; }
    public Double getLatitud() { return latitud; }
    public Double getLongitud() { return longitud; }
    public boolean isLeida() { return leida; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}
