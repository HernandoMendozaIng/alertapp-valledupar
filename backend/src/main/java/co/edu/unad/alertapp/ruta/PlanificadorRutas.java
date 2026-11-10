package co.edu.unad.alertapp.ruta;

import co.edu.unad.alertapp.comun.ErrorNegocio;
import co.edu.unad.alertapp.comun.Geo;
import co.edu.unad.alertapp.seguimiento.ModoTransporte;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * HU 04 - Generación de rutas seguras optimizadas.
 *
 * Usa el algoritmo A* sobre la red vial. En la ruta corta el costo de cada tramo es su largo; en la ruta
 * segura el largo se multiplica por (1 + peso del modo x riesgo del tramo), de modo que una calle con
 * reportes recientes "se alarga" y el algoritmo prefiere rodearla si el desvío no es exagerado. Así la
 * seguridad pesa más que la distancia, como pide el criterio de aceptación, sin mandar a la persona a dar
 * una vuelta enorme. La heurística es la distancia en línea recta, que nunca sobreestima el costo.
 */
@Service
public class PlanificadorRutas {

    /** Distancia máxima entre el punto marcado y la calle más cercana. */
    static final double MAX_ENGANCHE_M = 300;
    /** Tope del riesgo de un tramo para que una sola zona no bloquee la ciudad entera. */
    static final double TOPE_RIESGO = 4.0;
    /** A partir de este riesgo se cuenta el tramo como "zona de riesgo" en el resumen. */
    static final double UMBRAL_ZONA = 0.8;
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final RedVial red;
    private final MapaRiesgo mapaRiesgo;

    public PlanificadorRutas(RedVial red, MapaRiesgo mapaRiesgo) {
        this.red = red;
        this.mapaRiesgo = mapaRiesgo;
    }

    public record Ruta(List<double[]> puntos, int metros, int minutos, int metrosEnRiesgo, double exposicion,
                       int reportesCercanos, List<String> calles, String nivel) {}

    public record Resultado(ModoTransporte modo, Ruta segura, Ruta corta, boolean mismaRuta, int metrosExtra,
                            int minutosExtra, int reduccionRiesgo, String explicacion, int reportesConsiderados,
                            String motor, OffsetDateTime calculadoEn) {}

    public Resultado planear(double oLat, double oLng, double dLat, double dLng, ModoTransporte modo) {
        if (!red.dentro(oLat, oLng) || !red.dentro(dLat, dLng)) {
            throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El prototipo calcula rutas dentro de las Comunas 3 y 4 y sus alrededores. Marca puntos dentro de esa zona.");
        }
        int origen = red.nodoCercano(oLat, oLng, modo, MAX_ENGANCHE_M, false);
        int destino = red.nodoCercano(dLat, dLng, modo, MAX_ENGANCHE_M, true);
        if (origen < 0 || destino < 0) {
            throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No hay una calle apta para " + nombre(modo) + " cerca del punto marcado. Muévelo un poco.");
        }
        if (origen == destino) {
            throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "El origen y el destino están demasiado cerca.");
        }
        int hora = OffsetDateTime.now(ZONA).getHour();
        MapaRiesgo.Instantanea riesgo = mapaRiesgo.para(hora);

        int[] caminoCorto = aEstrella(origen, destino, modo, riesgo.riesgoArista(), 0);
        int[] caminoSeguro = aEstrella(origen, destino, modo, riesgo.riesgoArista(), modo.pesoRiesgo());
        if (caminoCorto == null || caminoSeguro == null) {
            throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No encontré un camino entre esos puntos para " + nombre(modo) + ". Prueba con otro modo.");
        }
        Ruta corta = armar(caminoCorto, origen, modo, riesgo);
        Ruta segura = armar(caminoSeguro, origen, modo, riesgo);
        boolean misma = Arrays.equals(caminoCorto, caminoSeguro);
        int extra = Math.max(0, segura.metros() - corta.metros());
        int minutosExtra = Math.max(0, segura.minutos() - corta.minutos());
        int reduccion = corta.exposicion() <= 0 ? 0
                : (int) Math.round(100 * (1 - segura.exposicion() / corta.exposicion()));
        return new Resultado(modo, segura, corta, misma, extra, minutosExtra, Math.max(0, reduccion),
                explicar(segura, corta, misma, extra, minutosExtra, modo), riesgo.puntos().size(),
                "A* sobre la red vial de OpenStreetMap con riesgo por reportes", OffsetDateTime.now(ZONA));
    }

    /**
     * A* clásico. Devuelve la lista de aristas codificadas (arista * 2 + sentido) desde el origen o null si no hay camino.
     */
    int[] aEstrella(int origen, int destino, ModoTransporte modo, double[] riesgo, double peso) {
        int n = red.nodos();
        double[] g = new double[n];
        Arrays.fill(g, Double.POSITIVE_INFINITY);
        int[] llegada = new int[n];
        Arrays.fill(llegada, -1);
        boolean[] cerrado = new boolean[n];
        double dLat = red.latNodo[destino];
        double dLng = red.lngNodo[destino];
        PriorityQueue<double[]> abiertos = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
        g[origen] = 0;
        abiertos.add(new double[]{Geo.distanciaMetros(red.latNodo[origen], red.lngNodo[origen], dLat, dLng), origen});
        while (!abiertos.isEmpty()) {
            int u = (int) abiertos.poll()[1];
            if (cerrado[u]) continue;
            if (u == destino) break;
            cerrado[u] = true;
            for (int s : red.salidas[u]) {
                int e = s / 2;
                boolean alReves = s % 2 == 1;
                if (!red.transitable(e, alReves, modo)) continue;
                int v = alReves ? red.desde[e] : red.hasta[e];
                if (cerrado[v]) continue;
                double costo = red.largo[e] * (1 + peso * Math.min(riesgo[e], TOPE_RIESGO));
                double nuevo = g[u] + costo;
                if (nuevo < g[v]) {
                    g[v] = nuevo;
                    llegada[v] = s;
                    abiertos.add(new double[]{nuevo + Geo.distanciaMetros(red.latNodo[v], red.lngNodo[v], dLat, dLng), v});
                }
            }
        }
        if (llegada[destino] < 0) return null;
        List<Integer> camino = new ArrayList<>();
        int actual = destino;
        while (actual != origen) {
            int s = llegada[actual];
            camino.add(s);
            int e = s / 2;
            actual = s % 2 == 1 ? red.hasta[e] : red.desde[e];
        }
        int[] r = new int[camino.size()];
        for (int i = 0; i < r.length; i++) r[i] = camino.get(r.length - 1 - i);
        return r;
    }

    private Ruta armar(int[] camino, int origen, ModoTransporte modo, MapaRiesgo.Instantanea riesgo) {
        List<double[]> puntos = new ArrayList<>();
        puntos.add(new double[]{red.latNodo[origen], red.lngNodo[origen]});
        double metros = 0;
        double enRiesgo = 0;
        double exposicion = 0;
        Set<String> calles = new LinkedHashSet<>();
        for (int s : camino) {
            int e = s / 2;
            double[] geo = red.geometria[e];
            int total = geo.length / 2;
            for (int k = 1; k < total; k++) {
                int idx = s % 2 == 1 ? total - 1 - k : k;
                puntos.add(new double[]{geo[idx * 2], geo[idx * 2 + 1]});
            }
            metros += red.largo[e];
            double r = riesgo.riesgoArista()[e];
            exposicion += red.largo[e] * Math.min(r, TOPE_RIESGO);
            if (r >= UMBRAL_ZONA) enRiesgo += red.largo[e];
            if (!red.nombre[e].isBlank() && red.largo[e] > 60) calles.add(red.nombre[e]);
        }
        int cercanos = contarReportes(puntos, riesgo.puntos());
        double proporcion = metros == 0 ? 0 : enRiesgo / metros;
        String nivel = proporcion >= 0.3 ? "ALTO" : proporcion >= 0.1 ? "MEDIO" : "BAJO";
        return new Ruta(puntos, (int) Math.round(metros), (int) Math.max(1, Math.round(metros / modo.velocidad() / 60)),
                (int) Math.round(enRiesgo), Math.round(exposicion * 10) / 10d, cercanos,
                calles.stream().limit(5).toList(), nivel);
    }

    /** Reportes a menos de 100 m de algún punto de la ruta. */
    private static int contarReportes(List<double[]> puntos, List<MapaRiesgo.Punto> reportes) {
        Set<Long> cerca = new HashSet<>();
        for (MapaRiesgo.Punto r : reportes) {
            for (double[] p : puntos) {
                if (Math.abs(p[0] - r.lat()) > 0.001 || Math.abs(p[1] - r.lng()) > 0.001) continue;
                if (Geo.distanciaMetros(p[0], p[1], r.lat(), r.lng()) <= 100) {
                    cerca.add(r.id());
                    break;
                }
            }
        }
        return cerca.size();
    }

    private static String explicar(Ruta segura, Ruta corta, boolean misma, int extra, int minutosExtra, ModoTransporte modo) {
        if (misma) {
            return corta.metrosEnRiesgo() == 0
                    ? "La ruta más corta ya es la más segura: no pasa por tramos con reportes recientes."
                    : "No hay un desvío razonable: la ruta más corta también es la más segura. Ten cuidado en los tramos marcados.";
        }
        String verbo = switch (modo) {
            case A_PIE -> "caminar";
            case BICICLETA -> "pedalear";
            case VEHICULO -> "recorrer";
        };
        int metrosEvitados = corta.metrosEnRiesgo() - segura.metrosEnRiesgo();
        int reportesEvitados = corta.reportesCercanos() - segura.reportesCercanos();
        String gana;
        if (metrosEvitados > 0) {
            gana = "evita " + metrosEvitados + " m de tramos con reportes recientes";
        } else if (reportesEvitados > 0) {
            gana = "pasa lejos de " + reportesEvitados + (reportesEvitados == 1 ? " reporte" : " reportes")
                    + " que sí toca la más corta";
        } else {
            int menos = corta.exposicion() <= 0 ? 0
                    : (int) Math.round(100 * (1 - segura.exposicion() / corta.exposicion()));
            gana = "se aleja de las calles con más reportes (" + menos + " % menos exposición)";
        }
        String costo = extra == 0 ? " y no es más larga."
                : " a cambio de " + verbo + " " + extra + " m más" + (minutosExtra > 0 ? " (unos " + minutosExtra + " min)." : ".");
        return "La ruta segura " + gana + costo;
    }

    private static String nombre(ModoTransporte modo) {
        return switch (modo) {
            case A_PIE -> "ir a pie";
            case BICICLETA -> "ir en bicicleta";
            case VEHICULO -> "ir en carro o moto";
        };
    }
}
