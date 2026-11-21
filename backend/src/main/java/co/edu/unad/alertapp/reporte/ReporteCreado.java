package co.edu.unad.alertapp.reporte;

import co.edu.unad.alertapp.reporte.Catalogos.Arma;
import co.edu.unad.alertapp.reporte.Catalogos.Severidad;
import co.edu.unad.alertapp.reporte.Catalogos.TipoDelito;

/** Evento que se publica cuando un ciudadano registra un reporte. Lo escuchan las alertas (HU 02) y las rutas (HU 04). */
public record ReporteCreado(Long id, Long autorId, TipoDelito tipo, Arma arma, Severidad severidad,
                            double latitud, double longitud, short comuna, String barrio) {

    static ReporteCreado de(Reporte r) {
        return new ReporteCreado(r.getId(), r.getAutor().getId(), r.getTipoDelito(), r.getArma(), r.getSeveridad(),
                r.getLatitud(), r.getLongitud(), r.getComuna(), r.getBarrio());
    }

    /** Nombre del hecho para mostrar al ciudadano, igual al del formulario. */
    public String tipoLegible() {
        return switch (tipo) {
            case ATRACO -> "Atraco";
            case HURTO -> "Hurto sin violencia";
            case SOSPECHOSO -> "Actividad sospechosa";
            case OTRO -> "Hecho";
        };
    }

    public String armaLegible() {
        return switch (arma) {
            case ARMA_FUEGO -> "con arma de fuego";
            case CORTOPUNZANTE -> "con cuchillo o navaja";
            case SIN_ARMA -> "sin arma";
            case NO_IDENTIFICADA -> "arma no identificada";
        };
    }
}
