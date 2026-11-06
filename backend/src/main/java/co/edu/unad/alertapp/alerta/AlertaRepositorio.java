package co.edu.unad.alertapp.alerta;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertaRepositorio extends JpaRepository<AlertaPanico, Long> {

    Optional<AlertaPanico> findFirstByUsuarioIdAndEstado(Long usuarioId, AlertaPanico.Estado estado);

    List<AlertaPanico> findTop20ByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);

    @EntityGraph(attributePaths = "usuario")
    List<AlertaPanico> findTop50ByOrderByCreadoEnDesc();
}
