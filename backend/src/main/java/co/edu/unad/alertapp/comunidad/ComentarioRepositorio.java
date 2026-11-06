package co.edu.unad.alertapp.comunidad;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioRepositorio extends JpaRepository<Comentario, Long> {

    @EntityGraph(attributePaths = "usuario")
    List<Comentario> findByReporteIdOrderByCreadoEnAsc(Long reporteId);

    long countByReporteId(Long reporteId);
}
