package co.edu.unad.alertapp.notificacion;

import co.edu.unad.alertapp.comun.Geo;
import co.edu.unad.alertapp.reporte.ReporteCreado;
import co.edu.unad.alertapp.usuario.Rol;
import co.edu.unad.alertapp.usuario.Usuario;
import co.edu.unad.alertapp.usuario.UsuarioRepositorio;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * HU 02 - Cuando entra un reporte, avisa a los vecinos que activaron las notificaciones y cuya última
 * ubicación conocida está dentro de su radio de alerta (500 m por defecto). Además difunde el reporte a
 * todos los conectados para que el mapa y las rutas seguras se actualicen sin recargar (HU 04).
 */
@Component
public class AlertasCercanas {

    /** Radio máximo configurable por el usuario; se usa para acotar la consulta. */
    static final int RADIO_MAXIMO_M = 2000;
    /** Una ubicación de hace más de 12 horas ya no dice dónde está la persona. */
    static final int HORAS_VIGENCIA_UBICACION = 12;

    private final UsuarioRepositorio usuarios;
    private final NotificacionServicio notificaciones;

    public AlertasCercanas(UsuarioRepositorio usuarios, NotificacionServicio notificaciones) {
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCrearReporte(ReporteCreado r) {
        notificaciones.difundir("reporte", Map.of("id", r.id(), "latitud", r.latitud(), "longitud", r.longitud(),
                "severidad", r.severidad().name()));

        double dLat = Geo.gradosLatitud(RADIO_MAXIMO_M);
        double dLng = Geo.gradosLongitud(RADIO_MAXIMO_M, r.latitud());
        var candidatos = usuarios.candidatosAlerta(r.autorId(), OffsetDateTime.now().minusHours(HORAS_VIGENCIA_UBICACION),
                r.latitud() - dLat, r.latitud() + dLat, r.longitud() - dLng, r.longitud() + dLng);
        for (Usuario u : candidatos) {
            if (u.getRol() != Rol.CIUDADANO) continue;
            double metros = Geo.distanciaMetros(u.getUltimaLatitud(), u.getUltimaLongitud(), r.latitud(), r.longitud());
            if (metros > u.getRadioAlertas()) continue;
            String lugar = r.barrio() == null || r.barrio().isBlank() ? "Comuna " + r.comuna() : r.barrio();
            notificaciones.notificar(u, Notificacion.Tipo.REPORTE_CERCANO,
                    r.tipoLegible() + " a " + redondear(metros) + " de ti",
                    lugar + ", " + r.armaLegible() + ". Si vas por esa zona, toma otra ruta.",
                    "/reportes/" + r.id(), r.latitud(), r.longitud());
        }
    }

    /** "80 m", "250 m", "1,2 km": la distancia exacta al metro no aporta y delata la ubicación. */
    static String redondear(double metros) {
        if (metros < 100) return Math.max(10, Math.round(metros / 10) * 10) + " m";
        if (metros < 1000) return Math.round(metros / 50) * 50 + " m";
        return String.format(java.util.Locale.ROOT, "%.1f km", metros / 1000).replace('.', ',');
    }
}
