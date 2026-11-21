package co.edu.unad.alertapp.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @Column(name = "clave_hash", nullable = false, length = 100)
    private String claveHash;

    @Column(length = 20)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol = Rol.CIUDADANO;

    private Short comuna;

    @Column(name = "acepto_politica", nullable = false)
    private boolean aceptoPolitica;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    /** HU 02: recibir alertas de reportes cercanos y radio en metros. */
    @Column(nullable = false)
    private boolean notificaciones = true;

    @Column(name = "radio_alertas", nullable = false)
    private int radioAlertas = 500;

    /** Última ubicación conocida, la envía la app al abrir el mapa. Sirve para saber a quién alertar. */
    @Column(name = "ultima_latitud")
    private Double ultimaLatitud;

    @Column(name = "ultima_longitud")
    private Double ultimaLongitud;

    @Column(name = "ubicacion_en")
    private OffsetDateTime ubicacionEn;

    /** HU 10: secreto TOTP en Base32. Se guarda al preparar la activación y queda en uso al confirmar. */
    @Column(name = "totp_secreto", length = 64)
    private String totpSecreto;

    @Column(name = "dos_factores", nullable = false)
    private boolean dosFactores;

    protected Usuario() {
    }

    public Usuario(String nombre, String correo, String claveHash, String telefono, Rol rol, Short comuna) {
        this.nombre = nombre;
        this.correo = correo;
        this.claveHash = claveHash;
        this.telefono = telefono;
        this.rol = rol;
        this.comuna = comuna;
        this.aceptoPolitica = true;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getClaveHash() { return claveHash; }
    public String getTelefono() { return telefono; }
    public Rol getRol() { return rol; }
    public Short getComuna() { return comuna; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setComuna(Short comuna) { this.comuna = comuna; }
    public void setClaveHash(String claveHash) { this.claveHash = claveHash; }
    public void setCorreo(String correo) { this.correo = correo; }

    public boolean isNotificaciones() { return notificaciones; }
    public int getRadioAlertas() { return radioAlertas; }
    public Double getUltimaLatitud() { return ultimaLatitud; }
    public Double getUltimaLongitud() { return ultimaLongitud; }
    public OffsetDateTime getUbicacionEn() { return ubicacionEn; }
    public String getTotpSecreto() { return totpSecreto; }
    public boolean isDosFactores() { return dosFactores; }

    public void setNotificaciones(boolean notificaciones) { this.notificaciones = notificaciones; }
    public void setRadioAlertas(int radioAlertas) { this.radioAlertas = radioAlertas; }
    public void setTotpSecreto(String totpSecreto) { this.totpSecreto = totpSecreto; }
    public void setDosFactores(boolean dosFactores) { this.dosFactores = dosFactores; }

    public void ubicar(double latitud, double longitud) {
        this.ultimaLatitud = latitud;
        this.ultimaLongitud = longitud;
        this.ubicacionEn = OffsetDateTime.now();
    }
}
