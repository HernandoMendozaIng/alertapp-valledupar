package co.edu.unad.alertapp.usuario;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HU 01 - Códigos de recuperación de contraseña: 6 dígitos, válidos 15 minutos y con máximo 5 intentos.
 * Solo se guarda el hash del código, nunca el código.
 */
@Component
public class CodigosRecuperacion {

    static final Duration VIGENCIA = Duration.ofMinutes(15);
    static final int INTENTOS = 5;
    private static final SecureRandom AZAR = new SecureRandom();

    private record Pendiente(String hash, Instant expira, int intentos) {}

    private final Map<String, Pendiente> pendientes = new ConcurrentHashMap<>();
    private final Clock reloj;

    public CodigosRecuperacion() {
        this(Clock.systemUTC());
    }

    CodigosRecuperacion(Clock reloj) {
        this.reloj = reloj;
    }

    public String generar(String correo) {
        String codigo = String.format("%06d", AZAR.nextInt(1_000_000));
        pendientes.put(clave(correo), new Pendiente(hash(codigo), Instant.now(reloj).plus(VIGENCIA), 0));
        return codigo;
    }

    /** Devuelve true y borra el código si es correcto. Cada fallo gasta un intento. */
    public boolean canjear(String correo, String codigo) {
        String k = clave(correo);
        Pendiente p = pendientes.get(k);
        if (p == null) return false;
        if (Instant.now(reloj).isAfter(p.expira()) || p.intentos() >= INTENTOS) {
            pendientes.remove(k);
            return false;
        }
        if (MessageDigest.isEqual(p.hash().getBytes(StandardCharsets.UTF_8), hash(codigo).getBytes(StandardCharsets.UTF_8))) {
            pendientes.remove(k);
            return true;
        }
        pendientes.put(k, new Pendiente(p.hash(), p.expira(), p.intentos() + 1));
        return false;
    }

    private static String clave(String correo) {
        return correo.trim().toLowerCase();
    }

    private static String hash(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
