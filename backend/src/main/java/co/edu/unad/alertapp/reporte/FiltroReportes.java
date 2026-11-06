package co.edu.unad.alertapp.reporte;

import co.edu.unad.alertapp.comun.Geo;
import co.edu.unad.alertapp.reporte.Catalogos.Estado;
import co.edu.unad.alertapp.reporte.Catalogos.Severidad;
import co.edu.unad.alertapp.reporte.Catalogos.TipoDelito;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinámicos del mapa y del panel (HU 08). Todos son opcionales.
 * El radio se aplica primero como un cuadro delimitador en SQL y luego con distancia exacta.
 */
public record FiltroReportes(
        List<Estado> estados,
        Short comuna,
        TipoDelito tipo,
        Severidad severidad,
        OffsetDateTime desde,
        OffsetDateTime hasta,
        Double lat,
        Double lng,
        Integer radioMetros
) {

    public boolean tieneRadio() {
        return lat != null && lng != null && radioMetros != null && radioMetros > 0;
    }

    public Specification<Reporte> especificacion() {
        return (raiz, consulta, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (estados != null && !estados.isEmpty()) p.add(raiz.get("estado").in(estados));
            if (comuna != null) p.add(cb.equal(raiz.get("comuna"), comuna));
            if (tipo != null) p.add(cb.equal(raiz.get("tipoDelito"), tipo));
            if (severidad != null) p.add(cb.equal(raiz.get("severidad"), severidad));
            if (desde != null) p.add(cb.greaterThanOrEqualTo(raiz.get("fechaHecho"), desde));
            if (hasta != null) p.add(cb.lessThanOrEqualTo(raiz.get("fechaHecho"), hasta));
            if (tieneRadio()) {
                double dLat = Geo.gradosLatitud(radioMetros);
                double dLng = Geo.gradosLongitud(radioMetros, lat);
                p.add(cb.between(raiz.get("latitud"), lat - dLat, lat + dLat));
                p.add(cb.between(raiz.get("longitud"), lng - dLng, lng + dLng));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    public boolean dentroDelRadio(Reporte r) {
        return !tieneRadio() || Geo.distanciaMetros(lat, lng, r.getLatitud(), r.getLongitud()) <= radioMetros;
    }
}
