package co.edu.unad.alertapp.seguimiento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RutaFrecuenteRepositorio extends JpaRepository<RutaFrecuente, Long> {

    List<RutaFrecuente> findByUsuarioIdOrderByNombre(Long usuarioId);

    long countByUsuarioId(Long usuarioId);
}
