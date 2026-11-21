package co.edu.unad.alertapp.notificacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertasCercanasTest {

    @Test
    @DisplayName("La distancia se redondea para no delatar la ubicación exacta del reporte")
    void redondeo() {
        assertThat(AlertasCercanas.redondear(3)).isEqualTo("10 m");
        assertThat(AlertasCercanas.redondear(87)).isEqualTo("90 m");
        assertThat(AlertasCercanas.redondear(234)).isEqualTo("250 m");
        assertThat(AlertasCercanas.redondear(1234)).isEqualTo("1,2 km");
    }
}
