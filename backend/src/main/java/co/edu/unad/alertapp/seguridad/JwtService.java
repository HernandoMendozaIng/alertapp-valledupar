package co.edu.unad.alertapp.seguridad;

import co.edu.unad.alertapp.config.AlertAppProperties;
import co.edu.unad.alertapp.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey llave;
    private final int horasValidez;

    public JwtService(AlertAppProperties props) {
        byte[] bytes = props.jwt().secreto().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("El secreto JWT debe tener al menos 32 caracteres");
        }
        this.llave = Keys.hmacShaKeyFor(bytes);
        this.horasValidez = props.jwt().horasValidez();
    }

    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("rol", usuario.getRol().name())
                .claim("nombre", usuario.getNombre())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(horasValidez, ChronoUnit.HOURS)))
                .signWith(llave)
                .compact();
    }

    public Optional<Claims> validar(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(llave).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
