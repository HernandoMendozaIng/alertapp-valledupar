package co.edu.unad.alertapp.reporte;

import co.edu.unad.alertapp.reporte.Catalogos.Arma;
import co.edu.unad.alertapp.reporte.Catalogos.Elemento;
import co.edu.unad.alertapp.reporte.Catalogos.Estado;
import co.edu.unad.alertapp.reporte.Catalogos.Medio;
import co.edu.unad.alertapp.reporte.Catalogos.Severidad;
import co.edu.unad.alertapp.reporte.Catalogos.TipoDelito;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public final class ReporteDtos {

    private ReporteDtos() {
    }

    /** Datos del formulario guiado. Las coordenadas se limitan al perímetro urbano de Valledupar. */
    public record ReportePeticion(
            @NotNull(message = "Elige qué pasó") TipoDelito tipoDelito,
            @NotNull(message = "Indica el arma") Arma arma,
            @NotNull(message = "Indica cómo se movía el agresor") Medio medio,
            @NotNull(message = "Indica qué se llevaron") Elemento elementoHurtado,
            @Size(max = 500, message = "La descripción admite hasta 500 caracteres") String descripcion,
            @NotNull(message = "Indica la fecha y hora") @PastOrPresent(message = "La fecha no puede ser futura") OffsetDateTime fechaHecho,
            @NotNull @DecimalMin(value = "10.40", message = "La ubicación está fuera de Valledupar")
            @DecimalMax(value = "10.52", message = "La ubicación está fuera de Valledupar") Double latitud,
            @NotNull @DecimalMin(value = "-73.32", message = "La ubicación está fuera de Valledupar")
            @DecimalMax(value = "-73.20", message = "La ubicación está fuera de Valledupar") Double longitud,
            @NotNull(message = "Elige la comuna") @Min(value = 1, message = "Comuna no válida") @Max(value = 6, message = "Comuna no válida") Short comuna,
            @Size(max = 60) String barrio,
            boolean anonimo
    ) {}

    /** Vista pública: si el reporte es anónimo no se expone ningún dato del autor. */
    public record ReporteRespuesta(
            Long id,
            TipoDelito tipoDelito,
            Arma arma,
            Medio medio,
            Elemento elementoHurtado,
            String descripcion,
            OffsetDateTime fechaHecho,
            double latitud,
            double longitud,
            short comuna,
            String barrio,
            boolean anonimo,
            String autor,
            boolean esMio,
            Estado estado,
            Severidad severidad,
            int puntajeSeveridad,
            int confirmaciones,
            int falsos,
            String miVoto,
            boolean tieneEvidencia,
            OffsetDateTime creadoEn
    ) {
        public static ReporteRespuesta de(Reporte r, Long usuarioActual, String miVoto) {
            boolean esMio = r.esDe(usuarioActual);
            String autor = r.isAnonimo() ? null : r.getAutor().getNombre();
            return new ReporteRespuesta(r.getId(), r.getTipoDelito(), r.getArma(), r.getMedio(), r.getElementoHurtado(),
                    r.getDescripcion(), r.getFechaHecho(), r.getLatitud(), r.getLongitud(), r.getComuna(), r.getBarrio(),
                    r.isAnonimo(), autor, esMio, r.getEstado(), r.getSeveridad(), r.getPuntajeSeveridad(),
                    r.getConfirmaciones(), r.getFalsos(), miVoto, r.getEvidenciaArchivo() != null, r.getCreadoEn());
        }
    }
}
