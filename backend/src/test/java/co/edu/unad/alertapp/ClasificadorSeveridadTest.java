package co.edu.unad.alertapp;

import co.edu.unad.alertapp.reporte.Catalogos.Arma;
import co.edu.unad.alertapp.reporte.Catalogos.Elemento;
import co.edu.unad.alertapp.reporte.Catalogos.Medio;
import co.edu.unad.alertapp.reporte.Catalogos.Severidad;
import co.edu.unad.alertapp.reporte.Catalogos.TipoDelito;
import co.edu.unad.alertapp.reporte.ClasificadorSeveridad;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClasificadorSeveridadTest {

    private final ClasificadorSeveridad clasificador = new ClasificadorSeveridad();

    @Test
    void atracoConArmaDeFuegoEnMotoEsAlta() {
        var r = clasificador.clasificar(TipoDelito.ATRACO, Arma.ARMA_FUEGO, Medio.MOTOCICLETA, Elemento.CELULAR);
        assertThat(r.puntaje()).isEqualTo(93);
        assertThat(r.severidad()).isEqualTo(Severidad.ALTA);
    }

    @Test
    void hurtoSinArmaAPieEsBaja() {
        var r = clasificador.clasificar(TipoDelito.HURTO, Arma.SIN_ARMA, Medio.A_PIE, Elemento.PRENDA);
        assertThat(r.puntaje()).isEqualTo(18);
        assertThat(r.severidad()).isEqualTo(Severidad.BAJA);
    }

    @Test
    void hurtoConCortopunzanteEsMedia() {
        var r = clasificador.clasificar(TipoDelito.HURTO, Arma.CORTOPUNZANTE, Medio.A_PIE, Elemento.DINERO);
        assertThat(r.severidad()).isEqualTo(Severidad.MEDIA);
    }

    @Test
    void elPuntajeNuncaSuperaCien() {
        var r = clasificador.clasificar(TipoDelito.ATRACO, Arma.ARMA_FUEGO, Medio.VEHICULO, Elemento.VEHICULO);
        assertThat(r.puntaje()).isEqualTo(100);
    }
}
