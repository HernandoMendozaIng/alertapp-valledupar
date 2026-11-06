package co.edu.unad.alertapp.reporte;

import co.edu.unad.alertapp.reporte.Catalogos.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface ReporteRepositorio extends JpaRepository<Reporte, Long>, JpaSpecificationExecutor<Reporte> {

    List<Reporte> findByAutorIdOrderByCreadoEnDesc(Long autorId);

    @Query("""
            select r from Reporte r
            where r.estado in :estados and r.fechaHecho >= :desde
            order by r.fechaHecho desc
            """)
    List<Reporte> activosDesde(@Param("estados") Collection<Estado> estados, @Param("desde") OffsetDateTime desde);

    long countByEstado(Estado estado);
}
