package co.edu.unad.alertapp.analitica;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class AnaliticaDtos {

    private AnaliticaDtos() {
    }

    /** Punto que se envía al microservicio de IA. */
    public record PuntoIa(long id, double lat, double lng, OffsetDateTime fecha, int severidad) {}

    public record SolicitudHotspots(List<PuntoIa> puntos, double epsMetros, int minPuntos) {}

    /** Zona caliente encontrada por DBSCAN. Nivel: ALTO (rojo), MEDIO (naranja) o BAJO (verde). */
    public record Hotspot(int id, double lat, double lng, double radioMetros, int reportes,
                          double severidadMedia, String nivel, int puntaje, String franjaPico) {}

    public record RespuestaHotspots(String motor, List<Hotspot> hotspots, int ruido) {}

    public record Conteo(String clave, long valor) {}

    public record Metricas(
            long total,
            long ultimos7Dias,
            long semanaAnterior,
            long validados,
            long pendientes,
            long descartados,
            List<Conteo> porDiaSemana,
            List<Conteo> porHora,
            List<Conteo> porTipo,
            List<Conteo> porComuna,
            List<Conteo> porSeveridad,
            List<Conteo> porSemana,
            Map<String, Object> filtros
    ) {}

    /** Riesgo estimado para una franja horaria y un día, a partir de la frecuencia histórica. */
    public record Riesgo(String nivel, double indice, String franja, String explicacion, String motor) {}
}
