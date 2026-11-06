package co.edu.unad.alertapp.alerta;

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
@Table(name = "alerta_panico")
public class AlertaPanico {

    public enum Estado { ACTIVA, ATENDIDA, CANCELADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private double latitud;

    @Column(nullable = false)
    private double longitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Estado estado = Estado.ACTIVA;

    @Column(length = 200)
    private String nota;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "cerrada_en")
    private OffsetDateTime cerradaEn;

    protected AlertaPanico() {
    }

    public AlertaPanico(Usuario usuario, double latitud, double longitud) {
        this.usuario = usuario;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public void cerrar(Estado nuevo, String nota) {
        this.estado = nuevo;
        this.nota = nota;
        this.cerradaEn = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
    public Estado getEstado() { return estado; }
    public String getNota() { return nota; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public OffsetDateTime getCerradaEn() { return cerradaEn; }
}
