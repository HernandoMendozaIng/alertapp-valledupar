package co.edu.unad.alertapp.usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CodigosRecuperacionTest {

    static class RelojMovil extends Clock {
        Instant ahora = Instant.parse("2026-11-09T15:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    @Test
    @DisplayName("El código sirve una sola vez")
    void unSoloUso() {
        var codigos = new CodigosRecuperacion();
        String c = codigos.generar("Ana@Prueba.co");
        assertThat(c).matches("\\d{6}");
        assertThat(codigos.canjear("ana@prueba.co", c)).isTrue();
        assertThat(codigos.canjear("ana@prueba.co", c)).isFalse();
    }

    @Test
    @DisplayName("Vence a los 15 minutos")
    void vence() {
        var reloj = new RelojMovil();
        var codigos = new CodigosRecuperacion(reloj);
        String c = codigos.generar("ana@prueba.co");
        reloj.ahora = reloj.ahora.plus(Duration.ofMinutes(16));
        assertThat(codigos.canjear("ana@prueba.co", c)).isFalse();
    }

    @Test
    @DisplayName("Tras 5 intentos fallidos se anula aunque luego llegue el código correcto")
    void intentos() {
        var codigos = new CodigosRecuperacion();
        String c = codigos.generar("ana@prueba.co");
        String malo = c.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < CodigosRecuperacion.INTENTOS; i++) {
            assertThat(codigos.canjear("ana@prueba.co", malo)).isFalse();
        }
        assertThat(codigos.canjear("ana@prueba.co", c)).isFalse();
    }
}
