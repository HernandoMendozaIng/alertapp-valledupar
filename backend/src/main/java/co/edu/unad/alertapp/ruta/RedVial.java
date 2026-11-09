package co.edu.unad.alertapp.ruta;

import co.edu.unad.alertapp.comun.Geo;
import co.edu.unad.alertapp.seguimiento.ModoTransporte;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HU 04 - Red vial de las Comunas 3 y 4 cargada en memoria como grafo.
 *
 * Los datos salen de OpenStreetMap (scripts/datos/construir_red_vial.py). Cada nodo es un cruce y cada
 * arista un tramo de calle entre dos cruces, con su largo, su tipo de vía, si es de un solo sentido y su
 * geometría. El grafo es pequeño (unos 5 000 nodos), así que cabe completo en memoria y se consulta en
 * milisegundos sin depender de un servicio externo de mapas.
 */
@Component
public class RedVial {

    private static final Logger LOG = LoggerFactory.getLogger(RedVial.class);
    /** Tamaño de celda de la grilla espacial, unos 220 m. */
    static final double CELDA = 0.002;

    final double[] latNodo;
    final double[] lngNodo;
    final int[] desde;
    final int[] hasta;
    final double[] largo;
    final boolean[] unSentido;
    final int[] permiso;
    final String[] nombre;
    /** Geometría de cada arista: lat, lng intercalados, incluidos los dos extremos. */
    final double[][] geometria;
    /** Aristas que salen de cada nodo, codificadas como arista * 2 + (0 = de "desde" a "hasta", 1 = al revés). */
    final int[][] salidas;
    final double sur, oeste, norte, este;

    private final Map<Long, List<Integer>> grillaNodos = new HashMap<>();
    /** Puntos de todas las geometrías por celda: arista * 65536 + índice del punto. */
    private final Map<Long, List<Integer>> grillaPuntos = new HashMap<>();

    @Autowired
    public RedVial(ObjectMapper mapper) {
        this(leer(mapper, "geo/red-vial.json"));
    }

    RedVial(JsonNode raiz) {
        JsonNode nodos = raiz.get("nodos");
        JsonNode aristas = raiz.get("aristas");
        JsonNode caja = raiz.get("caja");
        sur = caja.get(0).asDouble();
        oeste = caja.get(1).asDouble();
        norte = caja.get(2).asDouble();
        este = caja.get(3).asDouble();

        int n = nodos.size();
        latNodo = new double[n];
        lngNodo = new double[n];
        for (int i = 0; i < n; i++) {
            latNodo[i] = nodos.get(i).get(0).asDouble();
            lngNodo[i] = nodos.get(i).get(1).asDouble();
        }

        int m = aristas.size();
        desde = new int[m];
        hasta = new int[m];
        largo = new double[m];
        unSentido = new boolean[m];
        permiso = new int[m];
        nombre = new String[m];
        geometria = new double[m][];
        List<List<Integer>> sal = new ArrayList<>(n);
        for (int i = 0; i < n; i++) sal.add(new ArrayList<>(4));

        for (int e = 0; e < m; e++) {
            JsonNode a = aristas.get(e);
            desde[e] = a.get("a").asInt();
            hasta[e] = a.get("b").asInt();
            largo[e] = a.get("m").asDouble();
            unSentido[e] = a.get("u").asInt() == 1;
            permiso[e] = a.get("p").asInt();
            nombre[e] = a.get("n").asText("");
            JsonNode g = a.get("g");
            double[] geo = new double[(g.size() + 2) * 2];
            geo[0] = latNodo[desde[e]];
            geo[1] = lngNodo[desde[e]];
            for (int k = 0; k < g.size(); k++) {
                geo[2 + k * 2] = g.get(k).get(0).asDouble();
                geo[3 + k * 2] = g.get(k).get(1).asDouble();
            }
            geo[geo.length - 2] = latNodo[hasta[e]];
            geo[geo.length - 1] = lngNodo[hasta[e]];
            geometria[e] = geo;
            sal.get(desde[e]).add(e * 2);
            sal.get(hasta[e]).add(e * 2 + 1);
            for (int k = 0; k < geo.length / 2; k++) {
                grillaPuntos.computeIfAbsent(celda(geo[k * 2], geo[k * 2 + 1]), x -> new ArrayList<>()).add(e * 65536 + k);
            }
        }
        salidas = new int[n][];
        for (int i = 0; i < n; i++) {
            salidas[i] = sal.get(i).stream().mapToInt(Integer::intValue).toArray();
            grillaNodos.computeIfAbsent(celda(latNodo[i], lngNodo[i]), x -> new ArrayList<>()).add(i);
        }
        LOG.info("Red vial cargada: {} nodos y {} tramos", n, m);
    }

    public int nodos() { return latNodo.length; }
    public int aristas() { return largo.length; }

    public boolean dentro(double lat, double lng) {
        return lat >= sur && lat <= norte && lng >= oeste && lng <= este;
    }

    /** ¿Se puede recorrer la arista en ese sentido con ese modo? */
    boolean transitable(int e, boolean alReves, ModoTransporte modo) {
        if ((permiso[e] & modo.permiso()) == 0) return false;
        return !(alReves && unSentido[e] && modo.respetaSentido());
    }

    /**
     * Nodo más cercano a menos de maxMetros, o -1 si no hay. Para el origen debe tener una salida apta para el
     * modo y para el destino una entrada: en una calle de un solo sentido no son lo mismo.
     */
    public int nodoCercano(double lat, double lng, ModoTransporte modo, double maxMetros, boolean comoDestino) {
        int mejor = -1;
        double mejorD = maxMetros;
        int radio = (int) Math.ceil(Geo.gradosLatitud(maxMetros) / CELDA);
        long cLat = Math.round(Math.floor(lat / CELDA));
        long cLng = Math.round(Math.floor(lng / CELDA));
        for (long i = cLat - radio; i <= cLat + radio; i++) {
            for (long j = cLng - radio; j <= cLng + radio; j++) {
                for (int nodo : grillaNodos.getOrDefault(clave(i, j), List.of())) {
                    if (!conectado(nodo, modo, comoDestino)) continue;
                    double d = Geo.distanciaMetros(lat, lng, latNodo[nodo], lngNodo[nodo]);
                    if (d < mejorD) {
                        mejorD = d;
                        mejor = nodo;
                    }
                }
            }
        }
        return mejor;
    }

    /** Llama a la acción con cada punto de geometría a menos de radioMetros: (arista, índice de punto, distancia). */
    void puntosCerca(double lat, double lng, double radioMetros, VisitaPunto visita) {
        int radio = (int) Math.ceil(Geo.gradosLatitud(radioMetros) / CELDA) + 1;
        long cLat = Math.round(Math.floor(lat / CELDA));
        long cLng = Math.round(Math.floor(lng / CELDA));
        for (long i = cLat - radio; i <= cLat + radio; i++) {
            for (long j = cLng - radio; j <= cLng + radio; j++) {
                for (int codigo : grillaPuntos.getOrDefault(clave(i, j), List.of())) {
                    int e = codigo / 65536;
                    int k = codigo % 65536;
                    double d = Geo.distanciaMetros(lat, lng, geometria[e][k * 2], geometria[e][k * 2 + 1]);
                    if (d <= radioMetros) visita.visitar(e, k, d);
                }
            }
        }
    }

    @FunctionalInterface
    interface VisitaPunto {
        void visitar(int arista, int punto, double metros);
    }

    private boolean conectado(int nodo, ModoTransporte modo, boolean entrada) {
        for (int s : salidas[nodo]) {
            // Salir por s va al revés si s es impar; llegar por s es el recorrido contrario.
            boolean alReves = entrada ? s % 2 == 0 : s % 2 == 1;
            if (transitable(s / 2, alReves, modo)) return true;
        }
        return false;
    }

    private static long celda(double lat, double lng) {
        return clave(Math.round(Math.floor(lat / CELDA)), Math.round(Math.floor(lng / CELDA)));
    }

    private static long clave(long i, long j) {
        return i * 1_000_003L + j;
    }

    private static JsonNode leer(ObjectMapper mapper, String recurso) {
        try (InputStream in = new ClassPathResource(recurso).getInputStream()) {
            return mapper.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la red vial " + recurso, e);
        }
    }
}
