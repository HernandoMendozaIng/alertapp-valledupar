package co.edu.unad.alertapp.ruta;

import co.edu.unad.alertapp.reporte.Catalogos.Estado;
import co.edu.unad.alertapp.reporte.Reporte;
import co.edu.unad.alertapp.reporte.ReporteCreado;
import co.edu.unad.alertapp.reporte.ReporteRepositorio;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;

/**
 * HU 04 - Riesgo de cada tramo de calle a partir de los reportes ciudadanos.
 *
 * Cada reporte de los últimos 90 días aporta riesgo a los tramos que tiene a menos de 150 m, con más peso
 * mientras más cerca esté, más grave sea, más reciente sea, si la comunidad o la autoridad ya lo validaron
 * y si ocurrió a una hora parecida a la del viaje. Es el mismo criterio del índice de riesgo del mapa, pero
 * repartido sobre la red vial. Se recalcula cuando entra un reporte nuevo y, por si acaso, cada 2 minutos.
 */
@Component
public class MapaRiesgo {

    static final double RADIO_M = 150;
    static final int DIAS = 90;
    static final double VIDA_MEDIA_DIAS = 45;
    private static final Duration VIGENCIA = Duration.ofMinutes(2);
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final EnumSet<Estado> CUENTAN = EnumSet.of(Estado.PENDIENTE, Estado.VALIDADO, Estado.EN_REVISION, Estado.CERRADO);

    /** Reporte reducido a lo que necesita el cálculo. */
    record Punto(long id, double lat, double lng, double peso, int hora) {}

    record Instantanea(double[] riesgoArista, List<Punto> puntos, int hora, Instant calculado) {}

    private final RedVial red;
    private final ReporteRepositorio reportes;
    private volatile Instantanea cache;

    public MapaRiesgo(RedVial red, ReporteRepositorio reportes) {
        this.red = red;
        this.reportes = reportes;
    }

    /** Riesgo vigente para la hora dada (0-23). */
    @Transactional(readOnly = true)
    public Instantanea para(int hora) {
        Instantanea c = cache;
        if (c != null && c.hora() == hora && c.calculado().plus(VIGENCIA).isAfter(Instant.now())) {
            return c;
        }
        List<Punto> puntos = reportes.activosDesde(CUENTAN, OffsetDateTime.now().minusDays(DIAS)).stream()
                .map(MapaRiesgo::punto).toList();
        Instantanea nueva = calcular(red, puntos, hora);
        cache = nueva;
        return nueva;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void alCrearReporte(ReporteCreado evento) {
        cache = null;
    }

    public void invalidar() {
        cache = null;
    }

    static Instantanea calcular(RedVial red, List<Punto> puntos, int hora) {
        double[][] porPunto = new double[red.aristas()][];
        for (Punto p : puntos) {
            double factorHora = distanciaHoras(p.hora(), hora) <= 2 ? 1.4 : 1.0;
            red.puntosCerca(p.lat(), p.lng(), RADIO_M, (e, k, d) -> {
                if (porPunto[e] == null) porPunto[e] = new double[red.geometria[e].length / 2];
                porPunto[e][k] += p.peso() * factorHora * (1 - d / RADIO_M);
            });
        }
        // El riesgo del tramo es el del punto más peligroso que lo toca: basta pasar por una esquina caliente.
        double[] riesgo = new double[red.aristas()];
        for (int e = 0; e < riesgo.length; e++) {
            if (porPunto[e] == null) continue;
            double max = 0;
            for (double v : porPunto[e]) max = Math.max(max, v);
            riesgo[e] = max;
        }
        return new Instantanea(riesgo, puntos, hora, Instant.now());
    }

    static Punto punto(Reporte r) {
        double severidad = switch (r.getSeveridad()) {
            case ALTA -> 1.0;
            case MEDIA -> 0.65;
            case BAJA -> 0.35;
        };
        double validado = r.getEstado() == Estado.PENDIENTE ? 1.0 : 1.25;
        double dias = Math.max(0, Duration.between(r.getFechaHecho(), OffsetDateTime.now()).toHours() / 24d);
        double recencia = Math.pow(0.5, dias / VIDA_MEDIA_DIAS);
        int hora = r.getFechaHecho().atZoneSameInstant(ZONA).getHour();
        return new Punto(r.getId(), r.getLatitud(), r.getLongitud(), severidad * validado * recencia, hora);
    }

    static int distanciaHoras(int a, int b) {
        int d = Math.abs(a - b) % 24;
        return Math.min(d, 24 - d);
    }
}
