package co.edu.unad.alertapp.analitica;

import co.edu.unad.alertapp.analitica.AnaliticaDtos.Metricas;
import co.edu.unad.alertapp.analitica.AnaliticaDtos.RespuestaHotspots;
import co.edu.unad.alertapp.analitica.AnaliticaDtos.Riesgo;
import co.edu.unad.alertapp.reporte.Reporte;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/** HU 03 (hotspots), HU 08 (métricas y filtros) y HU 09 (exportación para autoridades). */
@RestController
@RequestMapping("/api/analitica")
public class AnaliticaControlador {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AnaliticaServicio servicio;

    public AnaliticaControlador(AnaliticaServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/hotspots")
    public RespuestaHotspots hotspots(@RequestParam(required = false) Short comuna,
                                      @RequestParam(defaultValue = "90") int dias) {
        return servicio.hotspots(comuna, limitar(dias));
    }

    @GetMapping("/metricas")
    public Metricas metricas(@RequestParam(required = false) Short comuna,
                             @RequestParam(defaultValue = "90") int dias) {
        return servicio.metricas(comuna, limitar(dias));
    }

    @GetMapping("/riesgo")
    public Riesgo riesgo(@RequestParam double lat, @RequestParam double lng) {
        return servicio.riesgo(lat, lng);
    }

    /** CSV separado por punto y coma con BOM UTF-8, para que Excel en español lo abra con tildes. */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) Short comuna,
                                           @RequestParam(defaultValue = "7") int dias) {
        List<Reporte> lista = servicio.base(comuna, limitar(dias)).stream()
                .sorted(Comparator.comparing(Reporte::getFechaHecho).reversed())
                .toList();
        StringBuilder csv = new StringBuilder("﻿");
        csv.append("id;fecha_hecho;comuna;barrio;tipo;arma;medio;elemento;severidad;puntaje;estado;confirmaciones;falsos;latitud;longitud\n");
        for (Reporte r : lista) {
            csv.append(r.getId()).append(';')
                    .append(r.getFechaHecho().atZoneSameInstant(AnaliticaServicio.ZONA).format(FECHA)).append(';')
                    .append(r.getComuna()).append(';')
                    .append(celda(r.getBarrio())).append(';')
                    .append(r.getTipoDelito()).append(';')
                    .append(r.getArma()).append(';')
                    .append(r.getMedio()).append(';')
                    .append(r.getElementoHurtado()).append(';')
                    .append(r.getSeveridad()).append(';')
                    .append(r.getPuntajeSeveridad()).append(';')
                    .append(r.getEstado()).append(';')
                    .append(r.getConfirmaciones()).append(';')
                    .append(r.getFalsos()).append(';')
                    .append(String.format(java.util.Locale.ROOT, "%.6f;%.6f", r.getLatitud(), r.getLongitud()))
                    .append('\n');
        }
        String nombre = "alertapp-reportes-" + LocalDate.now(AnaliticaServicio.ZONA) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static int limitar(int dias) {
        return Math.max(1, Math.min(dias, 365));
    }

    /** Evita inyección de fórmulas en Excel y rompe separadores dentro del texto. */
    private static String celda(String valor) {
        if (valor == null) return "";
        String limpio = valor.replace(';', ',').replace('\n', ' ');
        return limpio.matches("^[=+\\-@].*") ? "'" + limpio : limpio;
    }
}
