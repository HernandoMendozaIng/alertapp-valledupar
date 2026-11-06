package co.edu.unad.alertapp.reporte;

import co.edu.unad.alertapp.comun.ErrorNegocio;
import co.edu.unad.alertapp.config.AlertAppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Guarda las fotos de evidencia en disco local. En la arquitectura objetivo este componente
 * se reemplaza por Amazon S3; en el prototipo se usa una carpeta para no depender de la nube.
 * Solo acepta JPEG, PNG o WEBP y verifica la firma del archivo, no solo la extensión.
 */
@Component
public class AlmacenEvidencias {

    private final Path carpeta;

    public AlmacenEvidencias(AlertAppProperties props) throws IOException {
        this.carpeta = Path.of(props.evidencias().carpeta()).toAbsolutePath().normalize();
        Files.createDirectories(carpeta);
    }

    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        byte[] cabecera = new byte[12];
        try (InputStream in = archivo.getInputStream()) {
            int leidos = in.readNBytes(cabecera, 0, cabecera.length);
            String extension = detectarExtension(cabecera, leidos);
            String nombre = UUID.randomUUID() + "." + extension;
            Files.copy(archivo.getInputStream(), carpeta.resolve(nombre));
            return nombre;
        } catch (IOException e) {
            throw new ErrorNegocio(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la foto");
        }
    }

    public Path ruta(String nombre) {
        Path ruta = carpeta.resolve(nombre).normalize();
        if (!ruta.startsWith(carpeta) || !Files.exists(ruta)) {
            throw new ErrorNegocio(HttpStatus.NOT_FOUND, "Evidencia no encontrada");
        }
        return ruta;
    }

    public MediaType tipo(String nombre) {
        if (nombre.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (nombre.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }

    public void borrar(String nombre) {
        if (nombre == null) return;
        try {
            Files.deleteIfExists(carpeta.resolve(nombre).normalize());
        } catch (IOException ignored) {
            // Si el archivo ya no existe no hay nada que limpiar.
        }
    }

    private String detectarExtension(byte[] b, int n) {
        if (n >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (n >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (n >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        throw new ErrorNegocio(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "La evidencia debe ser una foto JPG, PNG o WEBP");
    }
}
