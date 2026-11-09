package co.edu.unad.alertapp.guardian;

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

/** HU 12 - Relación entre un usuario protegido y una persona de confianza que recibe sus alertas. */
@Entity
@Table(name = "guardian")
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protegido_id")
    private Usuario protegido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id")
    private Usuario guardian;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    protected Guardian() {
    }

    public Guardian(Usuario protegido, Usuario guardian) {
        this.protegido = protegido;
        this.guardian = guardian;
    }

    public Long getId() { return id; }
    public Usuario getProtegido() { return protegido; }
    public Usuario getGuardian() { return guardian; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}
