package co.edu.unad.alertapp;

import co.edu.unad.alertapp.analitica.Dbscan;
import co.edu.unad.alertapp.analitica.Dbscan.Punto;
import co.edu.unad.alertapp.comun.Geo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DbscanTest {

    @Test
    void distanciaHaversineEntreDosPuntosConocidos() {
        // Aproximadamente 111 m por cada milésima de grado de latitud.
        assertThat(Geo.distanciaMetros(10.4540, -73.2529, 10.4550, -73.2529)).isCloseTo(111.2, within(0.5));
    }

    @Test
    void agrupaPuntosCercanosYMarcaRuido() {
        List<Punto> puntos = new ArrayList<>();
        for (int i = 0; i < 6; i++) puntos.add(new Punto(10.4540 + i * 0.0002, -73.2529));
        for (int i = 0; i < 5; i++) puntos.add(new Punto(10.4400 + i * 0.0002, -73.2700));
        puntos.add(new Punto(10.4900, -73.2300));

        int[] etiquetas = Dbscan.agrupar(puntos, 200, 4);

        assertThat(etiquetas[0]).isEqualTo(etiquetas[5]).isGreaterThanOrEqualTo(0);
        assertThat(etiquetas[6]).isEqualTo(etiquetas[10]).isNotEqualTo(etiquetas[0]);
        assertThat(etiquetas[11]).isEqualTo(-1);
    }
}
