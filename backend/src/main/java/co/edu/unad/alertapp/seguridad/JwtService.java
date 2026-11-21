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

    /** Tipos de token: la sesión normal y el desafío corto que se entrega antes del segundo factor (HU 10). */
    static final String SESION = "sesion";
    static final String DESAFIO_2FA = "2fa";
    private static final int MINUTOS_DESAFIO = 5;

    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("tipo", SESION)
                .claim("rol", usuario.getRol().name())
                .claim("nombre", usuario.getNombre())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(horasValidez, ChronoUnit.HOURS)))
                .signWith(llave)
                .compact();
    }

    /** Token de 5 minutos que solo sirve para enviar el código del segundo factor; no da acceso a la API. */
    public String generarDesafio(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("tipo", DESAFIO_2FA)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(MINUTOS_DESAFIO, ChronoUnit.MINUTES)))
                .signWith(llave)
                .compact();
    }

    /** Valida un token de sesión. Un desafío de 2FA no pasa por aquí aunque tenga firma válida. */
    public Optional<Claims> validar(String token) {
        return leer(token).filter(c -> SESION.equals(c.get("tipo", String.class)));
    }

    public Optional<Long> validarDesafio(String token) {
        return leer(token).filter(c -> DESAFIO_2FA.equals(c.get("tipo", String.class)))
                .map(c -> Long.valueOf(c.getSubject()));
    }

    private Optional<Claims> leer(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(llave).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
