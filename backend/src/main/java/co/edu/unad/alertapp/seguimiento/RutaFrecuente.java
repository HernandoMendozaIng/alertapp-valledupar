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

import java.time.OffsetDateTime;

/** HU 06 - Trayecto que el usuario repite (casa, trabajo, colegio) para calcularlo y compartirlo con un toque. */
@Entity
@Table(name = "ruta_frecuente")
public class RutaFrecuente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(name = "origen_latitud", nullable = false)
    private double origenLatitud;

    @Column(name = "origen_longitud", nullable = false)
    private double origenLongitud;

    @Column(name = "destino_latitud", nullable = false)
    private double destinoLatitud;

    @Column(name = "destino_longitud", nullable = false)
    private double destinoLongitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ModoTransporte modo;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    protected RutaFrecuente() {
    }

    public RutaFrecuente(Usuario usuario) {
        this.usuario = usuario;
    }

    public void actualizar(String nombre, double oLat, double oLng, double dLat, double dLng, ModoTransporte modo) {
        this.nombre = nombre;
        this.origenLatitud = oLat;
        this.origenLongitud = oLng;
        this.destinoLatitud = dLat;
        this.destinoLongitud = dLng;
        this.modo = modo;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getNombre() { return nombre; }
    public double getOrigenLatitud() { return origenLatitud; }
    public double getOrigenLongitud() { return origenLongitud; }
    public double getDestinoLatitud() { return destinoLatitud; }
    public double getDestinoLongitud() { return destinoLongitud; }
    public ModoTransporte getModo() { return modo; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}
