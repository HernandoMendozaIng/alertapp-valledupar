package co.edu.unad.alertapp.analitica;

import co.edu.unad.alertapp.comun.Geo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Implementación de respaldo del algoritmo DBSCAN (Ester et al., 1996) con distancia haversine.
 * Se usa solo cuando el microservicio de IA en Python no responde, para que el mapa siga
 * mostrando puntos calientes. Devuelve la etiqueta de grupo de cada punto (-1 = ruido).
 */
public final class Dbscan {

    private Dbscan() {
    }

    public record Punto(double lat, double lng) {}

    public static int[] agrupar(List<Punto> puntos, double epsMetros, int minPuntos) {
        int n = puntos.size();
        int[] etiqueta = new int[n];
        Arrays.fill(etiqueta, Integer.MIN_VALUE); // sin visitar
        int grupo = -1;
        for (int i = 0; i < n; i++) {
            if (etiqueta[i] != Integer.MIN_VALUE) continue;
            List<Integer> vecinos = vecinos(puntos, i, epsMetros);
            if (vecinos.size() < minPuntos) {
                etiqueta[i] = -1;
                continue;
            }
            grupo++;
            etiqueta[i] = grupo;
            Deque<Integer> cola = new ArrayDeque<>(vecinos);
            while (!cola.isEmpty()) {
                int j = cola.poll();
                if (etiqueta[j] == -1) etiqueta[j] = grupo;
                if (etiqueta[j] != Integer.MIN_VALUE) continue;
                etiqueta[j] = grupo;
                List<Integer> vecinosJ = vecinos(puntos, j, epsMetros);
                if (vecinosJ.size() >= minPuntos) cola.addAll(vecinosJ);
            }
        }
        return etiqueta;
    }

    private static List<Integer> vecinos(List<Punto> puntos, int i, double eps) {
        Punto p = puntos.get(i);
        List<Integer> r = new ArrayList<>();
        for (int k = 0; k < puntos.size(); k++) {
            Punto q = puntos.get(k);
            if (Geo.distanciaMetros(p.lat(), p.lng(), q.lat(), q.lng()) <= eps) r.add(k);
        }
        return r;
    }
}
