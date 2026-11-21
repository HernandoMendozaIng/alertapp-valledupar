package co.edu.unad.alertapp.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementación del prototipo: el mensaje se escribe en el registro del servidor en lugar de enviarse.
 * El prototipo no tiene servidor de correo ni pasarela de SMS; en TRL 6 esta clase se reemplaza por
 * Amazon SES (correo) o Amazon SNS (SMS) sin tocar el resto del código.
 */
@Component
public class MensajeroRegistro implements Mensajero {

    private static final Logger LOG = LoggerFactory.getLogger(MensajeroRegistro.class);

    @Override
    public void enviar(String destino, String asunto, String texto) {
        LOG.info("[MENSAJE SIMULADO] Para: {} | {} | {}", destino, asunto, texto);
    }
}
