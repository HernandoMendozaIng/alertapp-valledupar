package co.edu.unad.alertapp.seguimiento;

/**
 * HU 04 - Modo de desplazamiento. Define por qué vías se puede ir, a qué velocidad y cuánto pesa el riesgo:
 * quien camina está más expuesto a un atraco que quien va en carro, así que su ruta se aleja más de los
 * puntos calientes aunque tenga que caminar un poco más.
 */
public enum ModoTransporte {
    //          permiso  m/s   peso del riesgo  respeta sentido vial
    A_PIE(4, 1.3, 2.2, false),
    BICICLETA(2, 4.2, 1.6, true),
    VEHICULO(1, 7.0, 0.9, true);

    private final int permiso;
    private final double velocidad;
    private final double pesoRiesgo;
    private final boolean respetaSentido;

    ModoTransporte(int permiso, double velocidad, double pesoRiesgo, boolean respetaSentido) {
        this.permiso = permiso;
        this.velocidad = velocidad;
        this.pesoRiesgo = pesoRiesgo;
        this.respetaSentido = respetaSentido;
    }

    /** Bit que debe tener la vía en la red vial (1 carro, 2 bici, 4 a pie). */
    public int permiso() { return permiso; }
    /** Velocidad promedio en metros por segundo. */
    public double velocidad() { return velocidad; }
    public double pesoRiesgo() { return pesoRiesgo; }
    public boolean respetaSentido() { return respetaSentido; }
}
