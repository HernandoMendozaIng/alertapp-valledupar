package co.edu.unad.alertapp.comun;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
public class SaludControlador {

    @GetMapping("/api/salud")
    public Map<String, Object> salud() {
        return Map.of("estado", "ok", "servicio", "alertapp-backend", "hora", OffsetDateTime.now().toString());
    }
}
