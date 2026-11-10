package co.edu.unad.alertapp.seguimiento;

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

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * HU 06 / HU 12 - Ubicación compartida por un tiempo limitado (máximo 1 hora). La ven solo los guardianes
 * del usuario y se apaga sola al vencer o cuando el usuario la detiene.
 */
@Entity
@Table(name = "seguimiento")
public class Seguimiento {

    public enum Motivo { TRAYECTO, PANICO }

    public static final Duration MAXIMO = Duration.ofHours(1);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Motivo motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id")
    private RutaFrecuente ruta;

    @Column(nullable = false)
    private double latitud;

    @Column(nullable = false)
    private double longitud;

    @Column(name = "iniciado_en", nullable = false)
    private OffsetDateTime iniciadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    @Column(name = "detenido_en")
    private OffsetDateTime detenidoEn;

    protected Seguimiento() {
    }

    public Seguimiento(Usuario usuario, Motivo motivo, RutaFrecuente ruta, double lat, double lng, Duration duracion) {
        OffsetDateTime ahora = OffsetDateTime.now();
        this.usuario = usuario;
        this.motivo = motivo;
        this.ruta = ruta;
        this.latitud = lat;
        this.longitud = lng;
        this.iniciadoEn = ahora;
        this.actualizadoEn = ahora;
        this.expiraEn = ahora.plus(duracion.compareTo(MAXIMO) > 0 ? MAXIMO : duracion);
    }

    public boolean activo() {
        return detenidoEn == null && OffsetDateTime.now().isBefore(expiraEn);
    }

    public void mover(double lat, double lng) {
        this.latitud = lat;
        this.longitud = lng;
        this.actualizadoEn = OffsetDateTime.now();
    }

    public void detener() {
        if (detenidoEn == null) detenidoEn = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Motivo getMotivo() { return motivo; }
    public RutaFrecuente getRuta() { return ruta; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
    public OffsetDateTime getIniciadoEn() { return iniciadoEn; }
    public OffsetDateTime getActualizadoEn() { return actualizadoEn; }
    public OffsetDateTime getExpiraEn() { return expiraEn; }
    public OffsetDateTime getDetenidoEn() { return detenidoEn; }
}
