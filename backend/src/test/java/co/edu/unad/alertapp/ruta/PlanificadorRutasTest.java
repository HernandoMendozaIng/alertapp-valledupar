package co.edu.unad.alertapp.ruta;

import co.edu.unad.alertapp.comun.ErrorNegocio;
import co.edu.unad.alertapp.seguimiento.ModoTransporte;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Red de juguete con dos caminos entre A (nodo 0) y D (nodo 3). Los largos coinciden con la geometría,
 * igual que en la red real, para que la heurística del A* sea válida:
 *   A -0- B -1- D   camino corto, 222 m. El tramo B-D es peatonal (no pasan carros).
 *   A -2- C -3- D   rodeo, 415 m. El tramo C-D es de un solo sentido, de C hacia D.
 */
class PlanificadorRutasTest {

    private RedVial red;

    @BeforeEach
    void preparar() throws Exception {
        String datos = """
                {"caja":[10.0,-74.0,11.0,-73.0],
                 "nodos":[[10.4500,-73.2500],[10.4510,-73.2500],[10.4510,-73.2516],[10.4520,-73.2500]],
                 "aristas":[
                  {"a":0,"b":1,"m":111.2,"t":"residential","n":"Calle corta","u":0,"p":7,"g":[]},
                  {"a":1,"b":3,"m":111.2,"t":"footway","n":"Pasaje","u":0,"p":4,"g":[]},
                  {"a":0,"b":2,"m":207.3,"t":"residential","n":"Rodeo","u":0,"p":7,"g":[]},
                  {"a":2,"b":3,"m":207.3,"t":"residential","n":"Rodeo","u":1,"p":7,"g":[]}
                 ]}""";
        red = new RedVial(new ObjectMapper().readTree(datos));
    }

    private PlanificadorRutas planificador(List<MapaRiesgo.Punto> reportes) {
        MapaRiesgo mapa = new MapaRiesgo(red, null) {
            @Override
            public Instantanea para(int hora) {
                return calcular(red, reportes, hora);
            }
        };
        return new PlanificadorRutas(red, mapa);
    }

    @Test
    @DisplayName("Sin reportes la ruta segura y la corta coinciden")
    void sinRiesgo() {
        var r = planificador(List.of()).planear(10.4500, -73.2500, 10.4520, -73.2500, ModoTransporte.A_PIE);
        assertThat(r.mismaRuta()).isTrue();
        assertThat(r.segura().metros()).isEqualTo(222);
        assertThat(r.reduccionRiesgo()).isZero();
    }

    @Test
    @DisplayName("Con un atraco en el camino corto, la ruta segura toma el rodeo")
    void evitaZonaDeRiesgo() {
        var atraco = new MapaRiesgo.Punto(1, 10.4510, -73.2500, 1.0, 3);
        var r = planificador(List.of(atraco)).planear(10.4500, -73.2500, 10.4520, -73.2500, ModoTransporte.A_PIE);
        assertThat(r.mismaRuta()).isFalse();
        assertThat(r.corta().metros()).isEqualTo(222);
        assertThat(r.segura().metros()).isEqualTo(415);
        assertThat(r.segura().calles()).containsExactly("Rodeo");
        assertThat(r.segura().exposicion()).isLessThan(r.corta().exposicion());
        assertThat(r.corta().reportesCercanos()).isEqualTo(1);
        assertThat(r.reduccionRiesgo()).isPositive();
        assertThat(r.explicacion()).contains("evita", "caminar");
    }

    @Test
    @DisplayName("El carro no entra al pasaje peatonal y respeta el sentido vial")
    void permisosYSentido() {
        var p = planificador(List.of());
        assertThat(p.planear(10.4500, -73.2500, 10.4520, -73.2500, ModoTransporte.A_PIE).corta().metros()).isEqualTo(222);
        assertThat(p.planear(10.4500, -73.2500, 10.4520, -73.2500, ModoTransporte.VEHICULO).corta().metros()).isEqualTo(415);
        // De D hacia A el carro no puede salir de D (pasaje peatonal o rodeo en contravía): el origen se engancha
        // en la esquina B, la más cercana con salida legal, y nunca toma el rodeo al revés.
        var carro = p.planear(10.4520, -73.2500, 10.4500, -73.2500, ModoTransporte.VEHICULO);
        assertThat(carro.corta().metros()).isEqualTo(111);
        assertThat(carro.corta().calles()).doesNotContain("Rodeo");
        // A pie sí se puede, por el pasaje.
        assertThat(p.planear(10.4520, -73.2500, 10.4500, -73.2500, ModoTransporte.A_PIE).corta().metros()).isEqualTo(222);
    }

    @Test
    @DisplayName("Rechaza puntos fuera de la caja de la red")
    void fueraDelArea() {
        assertThatThrownBy(() -> planificador(List.of()).planear(4.6, -74.08, 10.452, -73.25, ModoTransporte.A_PIE))
                .isInstanceOf(ErrorNegocio.class)
                .hasMessageContaining("Comunas 3 y 4");
    }

    @Test
    @DisplayName("Los reportes de una hora parecida pesan más que los de otra franja")
    void franjaHoraria() {
        var punto = new MapaRiesgo.Punto(1, 10.4510, -73.2500, 1.0, 21);
        double mismaFranja = MapaRiesgo.calcular(red, List.of(punto), 22).riesgoArista()[0];
        double otraFranja = MapaRiesgo.calcular(red, List.of(punto), 9).riesgoArista()[0];
        assertThat(mismaFranja).isGreaterThan(otraFranja);
        assertThat(MapaRiesgo.distanciaHoras(23, 1)).isEqualTo(2);
    }
}
