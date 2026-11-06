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
}
