package co.edu.unad.alertapp.comunidad;

import co.edu.unad.alertapp.reporte.Reporte;
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
@Table(name = "voto")
public class Voto {

    public enum Tipo { CONFIRMA, FALSO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporte_id")
    private Reporte reporte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Tipo tipo;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    protected Voto() {
    }

    public Voto(Reporte reporte, Usuario usuario, Tipo tipo) {
        this.reporte = reporte;
        this.usuario = usuario;
        this.tipo = tipo;
    }

    public Long getId() { return id; }
    public Reporte getReporte() { return reporte; }
    public Usuario getUsuario() { return usuario; }
    public Tipo getTipo() { return tipo; }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
        this.creadoEn = OffsetDateTime.now();
    }
}
