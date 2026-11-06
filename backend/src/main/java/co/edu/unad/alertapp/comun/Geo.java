package co.edu.unad.alertapp.comun;

/** Utilidades geográficas simples sobre coordenadas WGS84. */
public final class Geo {

    private static final double RADIO_TIERRA_M = 6_371_000d;

    private Geo() {
    }

    /** Distancia en metros entre dos puntos usando la fórmula del haversine. */
    public static double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RADIO_TIERRA_M * Math.asin(Math.sqrt(a));
    }

    /** Grados de latitud equivalentes a una distancia en metros (aproximación local). */
    public static double gradosLatitud(double metros) {
        return metros / 111_320d;
    }

    /** Grados de longitud equivalentes a una distancia en metros a una latitud dada. */
    public static double gradosLongitud(double metros, double latitud) {
        return metros / (111_320d * Math.cos(Math.toRadians(latitud)));
    }
}
