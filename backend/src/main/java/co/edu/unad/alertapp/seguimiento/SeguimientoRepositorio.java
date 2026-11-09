package co.edu.unad.alertapp.seguimiento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface SeguimientoRepositorio extends JpaRepository<Seguimiento, Long> {

    Optional<Seguimiento> findFirstByUsuarioIdAndDetenidoEnIsNullAndExpiraEnAfterOrderByIniciadoEnDesc(
            Long usuarioId, OffsetDateTime ahora);

    default Optional<Seguimiento> activo(Long usuarioId) {
        return findFirstByUsuarioIdAndDetenidoEnIsNullAndExpiraEnAfterOrderByIniciadoEnDesc(usuarioId, OffsetDateTime.now());
    }
}
