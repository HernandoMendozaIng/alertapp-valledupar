package co.edu.unad.alertapp.ruta;

import co.edu.unad.alertapp.ruta.PlanificadorRutas.Resultado;
import co.edu.unad.alertapp.seguimiento.ModoTransporte;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HU 04 - Generación de rutas seguras optimizadas. */
@RestController
@RequestMapping("/api/rutas")
public class RutaControlador {

    private final PlanificadorRutas planificador;

    public RutaControlador(PlanificadorRutas planificador) {
        this.planificador = planificador;
    }

    /** Devuelve la ruta segura y la más corta para compararlas en el mapa. */
    @GetMapping("/segura")
    public Resultado segura(@RequestParam double origenLat, @RequestParam double origenLng,
                            @RequestParam double destinoLat, @RequestParam double destinoLng,
                            @RequestParam(defaultValue = "A_PIE") ModoTransporte modo) {
        return planificador.planear(origenLat, origenLng, destinoLat, destinoLng, modo);
    }
}
