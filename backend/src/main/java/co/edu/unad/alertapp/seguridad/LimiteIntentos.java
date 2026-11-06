package co.edu.unad.alertapp.seguridad;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Frena ataques de fuerza bruta al ingreso: tras 5 intentos fallidos con el mismo correo
 * en 15 minutos, ese correo queda bloqueado hasta que pase la ventana. En memoria, suficiente
 * para el prototipo; en la arquitectura objetivo lo resuelve AWS Cognito.
 */
@Component
public class LimiteIntentos {

    static final int MAXIMO = 5;
    static final Duration VENTANA = Duration.ofMinutes(15);

    private record Registro(int fallos, Instant desde) {}

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final Clock reloj;

    public LimiteIntentos() {
        this(Clock.systemUTC());
    }

    LimiteIntentos(Clock reloj) {
        this.reloj = reloj;
    }

    public boolean bloqueado(String correo) {
        Registro r = registros.get(clave(correo));
        if (r == null) return false;
        if (Instant.now(reloj).isAfter(r.desde().plus(VENTANA))) {
            registros.remove(clave(correo));
            return false;
        }
        return r.fallos() >= MAXIMO;
    }

    public void fallo(String correo) {
        Instant ahora = Instant.now(reloj);
        registros.merge(clave(correo), new Registro(1, ahora), (viejo, nuevo) ->
                ahora.isAfter(viejo.desde().plus(VENTANA)) ? nuevo : new Registro(viejo.fallos() + 1, viejo.desde()));
    }

    public void exito(String correo) {
        registros.remove(clave(correo));
    }

    private static String clave(String correo) {
        return correo.trim().toLowerCase();
    }
}
