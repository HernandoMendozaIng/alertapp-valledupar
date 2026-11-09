package co.edu.unad.alertapp.seguridad;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;

/**
 * HU 10 - Códigos de un solo uso basados en tiempo (TOTP, RFC 6238) para la autenticación de dos factores.
 *
 * Funciona con cualquier app autenticadora (Google Authenticator, Microsoft Authenticator, Authy):
 * el servidor y el celular comparten un secreto y cada 30 segundos ambos calculan el mismo código de
 * 6 dígitos con HMAC-SHA1. Se acepta un paso de tolerancia hacia atrás y hacia adelante por desfase de
 * reloj. En la arquitectura objetivo este paso lo hace AWS Cognito.
 */
public final class Totp {

    static final int DIGITOS = 6;
    static final int PASO_SEGUNDOS = 30;
    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom AZAR = new SecureRandom();

    private Totp() {
    }

    /** Secreto nuevo de 160 bits (lo recomendado por el RFC 4226) en Base32. */
    public static String nuevoSecreto() {
        byte[] bytes = new byte[20];
        AZAR.nextBytes(bytes);
        return base32(bytes);
    }

    /** URI que leen las apps autenticadoras desde el código QR. */
    public static String uri(String emisor, String cuenta, String secreto) {
        String etiqueta = URLEncoder.encode(emisor + ":" + cuenta, StandardCharsets.UTF_8).replace("+", "%20");
        return "otpauth://totp/" + etiqueta + "?secret=" + secreto + "&issuer="
                + URLEncoder.encode(emisor, StandardCharsets.UTF_8).replace("+", "%20")
                + "&algorithm=SHA1&digits=" + DIGITOS + "&period=" + PASO_SEGUNDOS;
    }

    /** Código vigente para un secreto. Lo usan las pruebas automáticas para simular la app autenticadora. */
    public static String codigoActual(String secreto) {
        return codigo(desdeBase32(secreto), Instant.now().getEpochSecond() / PASO_SEGUNDOS);
    }

    public static boolean verificar(String secreto, String codigo) {
        return verificar(secreto, codigo, Clock.systemUTC());
    }

    static boolean verificar(String secreto, String codigo, Clock reloj) {
        if (secreto == null || codigo == null || !codigo.matches("\\d{" + DIGITOS + "}")) return false;
        long paso = Instant.now(reloj).getEpochSecond() / PASO_SEGUNDOS;
        byte[] llave = desdeBase32(secreto);
        boolean valido = false;
        for (long d = -1; d <= 1; d++) {
            // Se comparan los tres pasos sin cortar antes para no filtrar información por tiempo de respuesta.
            valido |= MessageDigest.isEqual(codigo(llave, paso + d).getBytes(StandardCharsets.US_ASCII),
                    codigo.getBytes(StandardCharsets.US_ASCII));
        }
        return valido;
    }

    static String codigo(byte[] llave, long paso) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(llave, "HmacSHA1"));
            byte[] h = mac.doFinal(ByteBuffer.allocate(8).putLong(paso).array());
            int corte = h[h.length - 1] & 0x0f;
            int binario = ((h[corte] & 0x7f) << 24) | ((h[corte + 1] & 0xff) << 16)
                    | ((h[corte + 2] & 0xff) << 8) | (h[corte + 3] & 0xff);
            return String.format("%0" + DIGITOS + "d", binario % (int) Math.pow(10, DIGITOS));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 no disponible", e);
        }
    }

    static String base32(byte[] datos) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : datos) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                sb.append(ALFABETO.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) sb.append(ALFABETO.charAt((buffer << (5 - bits)) & 31));
        return sb.toString();
    }

    static byte[] desdeBase32(String texto) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char c : texto.replace("=", "").replace(" ", "").toUpperCase().toCharArray()) {
            int v = ALFABETO.indexOf(c);
            if (v < 0) throw new IllegalArgumentException("Secreto Base32 no válido");
            buffer = (buffer << 5) | v;
            bits += 5;
            if (bits >= 8) {
                salida.write((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return salida.toByteArray();
    }
}
