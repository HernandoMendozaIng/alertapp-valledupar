package co.edu.unad.alertapp.reporte;

import co.edu.unad.alertapp.reporte.Catalogos.Arma;
import co.edu.unad.alertapp.reporte.Catalogos.Elemento;
import co.edu.unad.alertapp.reporte.Catalogos.Estado;
import co.edu.unad.alertapp.reporte.Catalogos.Medio;
import co.edu.unad.alertapp.reporte.Catalogos.Severidad;
import co.edu.unad.alertapp.reporte.Catalogos.TipoDelito;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reporte")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_delito", nullable = false, length = 30)
    private TipoDelito tipoDelito;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Arma arma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Medio medio;

    @Enumerated(EnumType.STRING)
    @Column(name = "elemento_hurtado", nullable = false, length = 30)
    private Elemento elementoHurtado;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "fecha_hecho", nullable = false)
    private OffsetDateTime fechaHecho;

    @Column(nullable = false)
    private double latitud;

    @Column(nullable = false)
    private double longitud;

    @Column(nullable = false)
    private short comuna;

    @Column(length = 60)
    private String barrio;

    @Column(nullable = false)
    private boolean anonimo = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severidad severidad;

    @Column(name = "puntaje_severidad", nullable = false)
    private short puntajeSeveridad;

    @Column(nullable = false)
    private int confirmaciones;

    @Column(nullable = false)
    private int falsos;

    @Column(name = "evidencia_archivo", length = 120)
    private String evidenciaArchivo;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    protected Reporte() {
    }

    public Reporte(Usuario autor) {
        this.autor = autor;
    }

    @PreUpdate
    void alActualizar() {
        this.actualizadoEn = OffsetDateTime.now();
    }

    public boolean esDe(Long usuarioId) {
        return autor.getId().equals(usuarioId);
    }

    public Long getId() { return id; }
    public Usuario getAutor() { return autor; }
    public TipoDelito getTipoDelito() { return tipoDelito; }
    public Arma getArma() { return arma; }
    public Medio getMedio() { return medio; }
    public Elemento getElementoHurtado() { return elementoHurtado; }
    public String getDescripcion() { return descripcion; }
    public OffsetDateTime getFechaHecho() { return fechaHecho; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
    public short getComuna() { return comuna; }
    public String getBarrio() { return barrio; }
    public boolean isAnonimo() { return anonimo; }
    public Estado getEstado() { return estado; }
    public Severidad getSeveridad() { return severidad; }
    public short getPuntajeSeveridad() { return puntajeSeveridad; }
    public int getConfirmaciones() { return confirmaciones; }
    public int getFalsos() { return falsos; }
    public String getEvidenciaArchivo() { return evidenciaArchivo; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public OffsetDateTime getActualizadoEn() { return actualizadoEn; }

    public void setTipoDelito(TipoDelito tipoDelito) { this.tipoDelito = tipoDelito; }
    public void setArma(Arma arma) { this.arma = arma; }
    public void setMedio(Medio medio) { this.medio = medio; }
    public void setElementoHurtado(Elemento elementoHurtado) { this.elementoHurtado = elementoHurtado; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setFechaHecho(OffsetDateTime fechaHecho) { this.fechaHecho = fechaHecho; }
    public void setLatitud(double latitud) { this.latitud = latitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }
    public void setComuna(short comuna) { this.comuna = comuna; }
    public void setBarrio(String barrio) { this.barrio = barrio; }
    public void setAnonimo(boolean anonimo) { this.anonimo = anonimo; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public void setEvidenciaArchivo(String evidenciaArchivo) { this.evidenciaArchivo = evidenciaArchivo; }
    public void setCreadoEn(OffsetDateTime creadoEn) { this.creadoEn = creadoEn; }

    public void setSeveridad(Severidad severidad, int puntaje) {
        this.severidad = severidad;
        this.puntajeSeveridad = (short) puntaje;
    }

    public void setVotos(int confirmaciones, int falsos) {
        this.confirmaciones = confirmaciones;
        this.falsos = falsos;
    }
}
