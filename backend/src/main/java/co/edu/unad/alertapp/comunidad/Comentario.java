package co.edu.unad.alertapp.comunidad;

import co.edu.unad.alertapp.reporte.Reporte;
import co.edu.unad.alertapp.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "comentario")
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporte_id")
    private Reporte reporte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 400)
    private String texto;

    @Column(name = "es_autoridad", nullable = false)
    private boolean esAutoridad;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    protected Comentario() {
    }

    public Comentario(Reporte reporte, Usuario usuario, String texto, boolean esAutoridad) {
        this.reporte = reporte;
        this.usuario = usuario;
        this.texto = texto;
        this.esAutoridad = esAutoridad;
    }

    public Long getId() { return id; }
    public Reporte getReporte() { return reporte; }
    public Usuario getUsuario() { return usuario; }
    public String getTexto() { return texto; }
    public boolean isEsAutoridad() { return esAutoridad; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}
