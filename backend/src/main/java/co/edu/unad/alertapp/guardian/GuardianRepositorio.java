package co.edu.unad.alertapp.guardian;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuardianRepositorio extends JpaRepository<Guardian, Long> {

    List<Guardian> findByProtegidoIdOrderByCreadoEn(Long protegidoId);

    List<Guardian> findByGuardianIdOrderByCreadoEn(Long guardianId);

    long countByProtegidoId(Long protegidoId);

    boolean existsByProtegidoIdAndGuardianId(Long protegidoId, Long guardianId);
}
