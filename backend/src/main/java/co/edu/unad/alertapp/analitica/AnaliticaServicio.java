package co.edu.unad.alertapp.analitica;

import co.edu.unad.alertapp.analitica.AnaliticaDtos.Conteo;
import co.edu.unad.alertapp.analitica.AnaliticaDtos.Hotspot;
import co.edu.unad.alertapp.analitica.AnaliticaDtos.Metricas;
import co.edu.unad.alertapp.analitica.AnaliticaDtos.PuntoIa;
import co.edu.unad.alertapp.analitica.AnaliticaDtos.RespuestaHotspots;
import co.edu.unad.alertapp.analitica.AnaliticaDtos.Riesgo;
import co.edu.unad.alertapp.analitica.AnaliticaDtos.SolicitudHotspots;
import co.edu.unad.alertapp.comun.Geo;
import co.edu.unad.alertapp.reporte.Catalogos.Estado;
import co.edu.unad.alertapp.reporte.FiltroReportes;
import co.edu.unad.alertapp.reporte.Reporte;
import co.edu.unad.alertapp.reporte.ReporteRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HU 03, HU 08 y HU 09. Calcula métricas, puntos calientes y el índice de riesgo.
 * El cálculo pesado (DBSCAN y riesgo) lo hace el microservicio de IA; aquí está el respaldo en Java
 * con la misma fórmula para que el prototipo funcione aunque ese servicio esté apagado.
 */
@Service
public class AnaliticaServicio {

    static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final Locale ES = Locale.forLanguageTag("es-CO");
    /** Para el análisis solo cuentan los reportes que no fueron descartados. */
    private static final EnumSet<Estado> ANALIZABLES = EnumSet.of(Estado.PENDIENTE, Estado.VALIDADO, Estado.EN_REVISION, Estado.CERRADO);
    private static final double EPS_METROS = 200;
    private static final int MIN_PUNTOS = 4;

    private final ReporteRepositorio reportes;
    private final ClienteIa ia;

    public AnaliticaServicio(ReporteRepositorio reportes, ClienteIa ia) {
        this.reportes = reportes;
        this.ia = ia;
    }

    @Transactional(readOnly = true)
    public List<Reporte> base(Short comuna, int dias) {
        OffsetDateTime desde = OffsetDateTime.now(ZONA).minusDays(dias);
        var filtro = new FiltroReportes(List.copyOf(ANALIZABLES), comuna, null, null, desde, null, null, null, null);
        return reportes.findAll(filtro.especificacion());
    }

    @Transactional(readOnly = true)
    public RespuestaHotspots hotspots(Short comuna, int dias) {
        List<PuntoIa> puntos = base(comuna, dias).stream().map(AnaliticaServicio::punto).toList();
        var solicitud = new SolicitudHotspots(puntos, EPS_METROS, MIN_PUNTOS);
        return ia.hotspots(solicitud).orElseGet(() -> hotspotsRespaldo(puntos));
    }

    @Transactional(readOnly = true)
    public Riesgo riesgo(double lat, double lng) {
        List<PuntoIa> puntos = base(null, 90).stream().map(AnaliticaServicio::punto).toList();
        OffsetDateTime ahora = OffsetDateTime.now(ZONA);
        return ia.riesgo(new ClienteIa.SolicitudRiesgo(puntos, lat, lng, ahora))
                .orElseGet(() -> riesgoRespaldo(puntos, lat, lng, ahora));
    }

    @Transactional(readOnly = true)
    public Metricas metricas(Short comuna, int dias) {
        List<Reporte> todos = base(comuna, dias);
        OffsetDateTime ahora = OffsetDateTime.now(ZONA);
        // La comparación semanal siempre usa las dos últimas semanas, sin importar el periodo elegido.
        List<Reporte> quincena = dias >= 14 ? todos : base(comuna, 14);
        long ult7 = quincena.stream().filter(r -> r.getFechaHecho().isAfter(ahora.minusDays(7))).count();
        long ant7 = quincena.stream().filter(r -> !r.getFechaHecho().isAfter(ahora.minusDays(7))
                && r.getFechaHecho().isAfter(ahora.minusDays(14))).count();

        Map<String, Long> dia = new LinkedHashMap<>();
        for (DayOfWeek d : DayOfWeek.values()) dia.put(d.getDisplayName(TextStyle.SHORT, ES), 0L);
        todos.forEach(r -> dia.merge(local(r).getDayOfWeek().getDisplayName(TextStyle.SHORT, ES), 1L, Long::sum));

        Map<String, Long> hora = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) hora.put(String.format("%02d", h), 0L);
        todos.forEach(r -> hora.merge(String.format("%02d", local(r).getHour()), 1L, Long::sum));

        Map<String, Long> semana = new TreeMap<>();
        todos.forEach(r -> {
            var z = local(r);
            semana.merge(z.get(IsoFields.WEEK_BASED_YEAR) + "-S" + String.format("%02d", z.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)), 1L, Long::sum);
        });

        Map<String, Object> filtros = new LinkedHashMap<>();
        filtros.put("comuna", comuna);
        filtros.put("dias", dias);

        return new Metricas(
                todos.size(), ult7, ant7,
                todos.stream().filter(r -> r.getEstado() == Estado.VALIDADO).count(),
                todos.stream().filter(r -> r.getEstado() == Estado.PENDIENTE).count(),
                reportes.countByEstado(Estado.DESCARTADO),
                lista(dia), lista(hora),
                agrupar(todos, r -> r.getTipoDelito().name()),
                agrupar(todos, r -> "Comuna " + r.getComuna()),
                agrupar(todos, r -> r.getSeveridad().name()),
                lista(semana),
                filtros);
    }

    static PuntoIa punto(Reporte r) {
        return new PuntoIa(r.getId(), r.getLatitud(), r.getLongitud(), r.getFechaHecho(), r.getPuntajeSeveridad());
    }

    private static ZonedDateTime local(Reporte r) {
        return r.getFechaHecho().atZoneSameInstant(ZONA);
    }

    private static List<Conteo> lista(Map<String, Long> mapa) {
        return mapa.entrySet().stream().map(e -> new Conteo(e.getKey(), e.getValue())).toList();
    }

    private static List<Conteo> agrupar(List<Reporte> todos, Function<Reporte, String> clave) {
        return todos.stream().collect(Collectors.groupingBy(clave, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new Conteo(e.getKey(), e.getValue()))
                .toList();
    }

    // ---- Respaldo en Java (misma lógica del microservicio de IA) ----

    RespuestaHotspots hotspotsRespaldo(List<PuntoIa> puntos) {
        var etiquetas = Dbscan.agrupar(puntos.stream().map(p -> new Dbscan.Punto(p.lat(), p.lng())).toList(), EPS_METROS, MIN_PUNTOS);
        Map<Integer, List<PuntoIa>> grupos = new TreeMap<>();
        int ruido = 0;
        for (int i = 0; i < puntos.size(); i++) {
            if (etiquetas[i] < 0) ruido++;
            else grupos.computeIfAbsent(etiquetas[i], k -> new ArrayList<>()).add(puntos.get(i));
        }
        OffsetDateTime ahora = OffsetDateTime.now(ZONA);
        List<Hotspot> zonas = new ArrayList<>();
        for (var e : grupos.entrySet()) {
            zonas.add(resumir(e.getKey(), e.getValue(), ahora));
        }
        zonas.sort(Comparator.comparingInt(Hotspot::puntaje).reversed());
        return new RespuestaHotspots("java-respaldo", zonas, ruido);
    }

    static Hotspot resumir(int id, List<PuntoIa> g, OffsetDateTime ahora) {
        double lat = g.stream().mapToDouble(PuntoIa::lat).average().orElse(0);
        double lng = g.stream().mapToDouble(PuntoIa::lng).average().orElse(0);
        double radio = g.stream().mapToDouble(p -> Geo.distanciaMetros(lat, lng, p.lat(), p.lng())).max().orElse(0);
        double sev = g.stream().mapToInt(PuntoIa::severidad).average().orElse(0);
        double recientes = g.stream().filter(p -> p.fecha().isAfter(ahora.minusDays(30))).count();
        // Puntaje 0-100: volumen (hasta 50), severidad media (hasta 30) y actividad del último mes (hasta 20).
        int puntaje = (int) Math.round(Math.min(g.size() / 30.0, 1) * 50 + sev / 100.0 * 30 + Math.min(recientes / 10.0, 1) * 20);
        String nivel = puntaje >= 60 ? "ALTO" : puntaje >= 35 ? "MEDIO" : "BAJO";
        int[] porFranja = new int[6];
        g.forEach(p -> porFranja[p.fecha().atZoneSameInstant(ZONA).getHour() / 4]++);
        int pico = 0;
        for (int i = 1; i < 6; i++) if (porFranja[i] > porFranja[pico]) pico = i;
        String franja = String.format("%02d:00-%02d:00", pico * 4, pico * 4 + 4);
        return new Hotspot(id, lat, lng, Math.max(radio, 60), g.size(), Math.round(sev * 10) / 10.0, nivel, puntaje, franja);
    }

    static Riesgo riesgoRespaldo(List<PuntoIa> puntos, double lat, double lng, OffsetDateTime ahora) {
        int horaActual = ahora.atZoneSameInstant(ZONA).getHour();
        double suma = 0;
        int cercanos = 0;
        for (PuntoIa p : puntos) {
            double d = Geo.distanciaMetros(lat, lng, p.lat(), p.lng());
            if (d > 500) continue;
            cercanos++;
            double dias = Math.max(0, ChronoUnit.HOURS.between(p.fecha(), ahora) / 24.0);
            int hp = p.fecha().atZoneSameInstant(ZONA).getHour();
            int dh = Math.min(Math.abs(hp - horaActual), 24 - Math.abs(hp - horaActual));
            double peso = Math.exp(-d / 250.0)                      // cercanía
                    * Math.pow(0.5, dias / 30.0)                    // vida media de 30 días
                    * Math.exp(-(dh * dh) / (2 * 2.0 * 2.0))        // similitud de hora (sigma 2 h)
                    * (0.5 + p.severidad() / 100.0);                // severidad
            suma += peso;
        }
        double indice = Math.round(100 * (1 - Math.exp(-suma / 1.5)) * 10) / 10.0;
        String nivel = indice >= 60 ? "ALTO" : indice >= 30 ? "MEDIO" : "BAJO";
        String franja = String.format("%02d:00-%02d:00", horaActual, (horaActual + 1) % 24);
        String explicacion = cercanos == 0
                ? "No hay reportes en 500 m a la redonda en los últimos 90 días."
                : cercanos + (cercanos == 1 ? " reporte" : " reportes") + " en 500 m a la redonda; pesan más los recientes, cercanos y de esta franja horaria.";
        return new Riesgo(nivel, indice, franja, explicacion, "java-respaldo");
    }
}
