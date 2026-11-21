package co.edu.unad.alertapp.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TotpTest {

    /** Secreto de los vectores de prueba del RFC 6238, anexo B (SHA-1). */
    private static final byte[] SECRETO_RFC = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    @Test
    @DisplayName("Coincide con los vectores oficiales del RFC 6238 (últimos 6 dígitos)")
    void vectoresRfc() {
        assertThat(Totp.codigo(SECRETO_RFC, 59L / 30)).isEqualTo("287082");
        assertThat(Totp.codigo(SECRETO_RFC, 1111111109L / 30)).isEqualTo("081804");
        assertThat(Totp.codigo(SECRETO_RFC, 1234567890L / 30)).isEqualTo("005924");
        assertThat(Totp.codigo(SECRETO_RFC, 2000000000L / 30)).isEqualTo("279037");
    }

    @Test
    @DisplayName("Base32 ida y vuelta conserva los bytes")
    void base32() {
        assertThat(Totp.base32(SECRETO_RFC)).isEqualTo("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
        assertThat(Totp.desdeBase32("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")).isEqualTo(SECRETO_RFC);
    }

    @Test
    @DisplayName("Acepta un paso de desfase de reloj y rechaza códigos viejos o mal formados")
    void tolerancia() {
        String secreto = Totp.base32(SECRETO_RFC);
        Clock reloj = Clock.fixed(Instant.ofEpochSecond(1234567890L), ZoneOffset.UTC);
        assertThat(Totp.verificar(secreto, "005924", reloj)).isTrue();
        String anterior = Totp.codigo(SECRETO_RFC, 1234567890L / 30 - 1);
        String muyViejo = Totp.codigo(SECRETO_RFC, 1234567890L / 30 - 3);
        assertThat(Totp.verificar(secreto, anterior, reloj)).isTrue();
        assertThat(Totp.verificar(secreto, muyViejo, reloj)).isFalse();
        assertThat(Totp.verificar(secreto, "12345", reloj)).isFalse();
        assertThat(Totp.verificar(secreto, "abcdef", reloj)).isFalse();
    }

    @Test
    @DisplayName("Genera secretos distintos de 160 bits y una URI que entienden las apps autenticadoras")
    void secretoYUri() {
        String a = Totp.nuevoSecreto();
        assertThat(a).hasSize(32).isNotEqualTo(Totp.nuevoSecreto());
        assertThat(Totp.uri("AlertApp Valledupar", "ana@prueba.co", a))
                .startsWith("otpauth://totp/AlertApp%20Valledupar%3Aana%40prueba.co?secret=" + a)
                .contains("issuer=AlertApp%20Valledupar", "digits=6", "period=30");
    }
}
