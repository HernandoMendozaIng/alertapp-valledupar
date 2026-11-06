package co.edu.unad.alertapp.comunidad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VotoRepositorio extends JpaRepository<Voto, Long> {

    Optional<Voto> findByReporteIdAndUsuarioId(Long reporteId, Long usuarioId);

    long countByReporteIdAndTipo(Long reporteId, Voto.Tipo tipo);

    @Query("select v from Voto v where v.usuario.id = :usuarioId and v.reporte.id in :reportes")
    List<Voto> deUsuarioEnReportes(@Param("usuarioId") Long usuarioId, @Param("reportes") Collection<Long> reportes);
}
