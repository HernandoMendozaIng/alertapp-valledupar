package co.edu.unad.alertapp.reporte;

import co.edu.unad.alertapp.reporte.Catalogos.Arma;
import co.edu.unad.alertapp.reporte.Catalogos.Elemento;
import co.edu.unad.alertapp.reporte.Catalogos.Medio;
import co.edu.unad.alertapp.reporte.Catalogos.Severidad;
import co.edu.unad.alertapp.reporte.Catalogos.TipoDelito;
import org.springframework.stereotype.Component;

/**
 * Asigna un puntaje de severidad de 0 a 100 a cada reporte a partir de sus variables.
 * Los pesos priorizan la violencia del hecho (arma y tipo), seguida del medio de escape
 * y del valor del elemento hurtado. Un puntaje de 60 o más es ALTA y de 30 a 59 es MEDIA.
 */
@Component
public class ClasificadorSeveridad {

    public record Resultado(int puntaje, Severidad severidad) {}

    public Resultado clasificar(TipoDelito tipo, Arma arma, Medio medio, Elemento elemento) {
        int puntaje = switch (arma) {
            case ARMA_FUEGO -> 50;
            case CORTOPUNZANTE -> 35;
            case NO_IDENTIFICADA -> 15;
            case SIN_ARMA -> 5;
        };
        puntaje += switch (tipo) {
            case ATRACO -> 25;
            case HURTO -> 10;
            case OTRO -> 5;
            case SOSPECHOSO -> 0;
        };
        puntaje += switch (medio) {
            case MOTOCICLETA, VEHICULO -> 10;
            case BICICLETA, NO_IDENTIFICADO -> 5;
            case A_PIE -> 0;
        };
        puntaje += switch (elemento) {
            case MOTOCICLETA, VEHICULO -> 15;
            case CELULAR, JOYAS, DINERO -> 8;
            case BICICLETA, PARTE_VEHICULO -> 6;
            case PRENDA, OTRO -> 3;
            case NINGUNO -> 0;
        };
        puntaje = Math.min(puntaje, 100);
        Severidad severidad = puntaje >= 60 ? Severidad.ALTA : puntaje >= 30 ? Severidad.MEDIA : Severidad.BAJA;
        return new Resultado(puntaje, severidad);
    }
}
