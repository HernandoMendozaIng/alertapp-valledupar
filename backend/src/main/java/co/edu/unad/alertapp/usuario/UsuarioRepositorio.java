package co.edu.unad.alertapp.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    /** Candidatos a recibir una alerta: activaron notificaciones y su última ubicación cae en la caja dada. */
    @Query("""
            select u from Usuario u
            where u.notificaciones = true and u.id <> :excluir and u.ubicacionEn >= :desde
              and u.ultimaLatitud between :latMin and :latMax
              and u.ultimaLongitud between :lngMin and :lngMax""")
    List<Usuario> candidatosAlerta(Long excluir, OffsetDateTime desde, double latMin, double latMax,
                                   double lngMin, double lngMax);
}
