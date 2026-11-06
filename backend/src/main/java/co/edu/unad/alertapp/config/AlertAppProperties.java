package co.edu.unad.alertapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "alertapp")
public record AlertAppProperties(
        Jwt jwt,
        Cors cors,
        Ia ia,
        Evidencias evidencias,
        Validacion validacion,
        Seed seed
) {
    public record Jwt(String secreto, int horasValidez) {}

    public record Cors(List<String> origenes) {}

    public record Ia(String url, int timeoutMs) {}

    public record Evidencias(String carpeta) {}

    public record Validacion(int umbral) {}

    public record Seed(boolean habilitado) {}
}
