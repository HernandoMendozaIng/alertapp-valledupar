"""Construye la red vial de las Comunas 3 y 4 de Valledupar para el cálculo de rutas seguras (HU 04).

Descarga las vías de OpenStreetMap con la API Overpass, las convierte en un grafo de nodos y aristas
y guarda el resultado en backend/src/main/resources/geo/red-vial.json. Solo se conservan las
intersecciones y extremos de cada vía, de modo que cada arista es un tramo entre dos cruces con su
geometría completa.

Uso:  python3 scripts/datos/construir_red_vial.py
      (en macOS con Python de python.org: SSL_CERT_FILE=/etc/ssl/cert.pem python3 ...)
Datos: © colaboradores de OpenStreetMap, licencia ODbL.
"""
import json
import math
import pathlib
import urllib.parse
import urllib.request

RAIZ = pathlib.Path(__file__).resolve().parents[2]
SALIDA = RAIZ / "backend/src/main/resources/geo/red-vial.json"

# Caja de las Comunas 3 y 4 con un margen de unos 400 m para que las rutas puedan salir del límite.
SUR, OESTE, NORTE, ESTE = 10.4185, -73.2930, 10.4730, -73.2375

TIPOS = {
    # tipo OSM: (apta para carro, apta para bici, apta a pie)
    "trunk": (True, True, True), "trunk_link": (True, True, True),
    "primary": (True, True, True), "primary_link": (True, True, True),
    "secondary": (True, True, True), "secondary_link": (True, True, True),
    "tertiary": (True, True, True), "tertiary_link": (True, True, True),
    "residential": (True, True, True), "unclassified": (True, True, True),
    "living_street": (True, True, True), "service": (True, True, True),
    "cycleway": (False, True, True), "pedestrian": (False, True, True),
    "footway": (False, False, True), "path": (False, True, True),
    "steps": (False, False, True), "track": (False, True, True),
}

CONSULTA = f"""
[out:json][timeout:90];
way["highway"~"^({'|'.join(TIPOS)})$"]({SUR},{OESTE},{NORTE},{ESTE});
(._;>;);
out body;
"""


def distancia(a, b):
    r = 6371000.0
    la1, lo1, la2, lo2 = map(math.radians, (a[0], a[1], b[0], b[1]))
    h = math.sin((la2 - la1) / 2) ** 2 + math.cos(la1) * math.cos(la2) * math.sin((lo2 - lo1) / 2) ** 2
    return 2 * r * math.asin(math.sqrt(h))


def main():
    datos = urllib.parse.urlencode({"data": CONSULTA}).encode()
    peticion = urllib.request.Request("https://overpass-api.de/api/interpreter", data=datos,
                                      headers={"User-Agent": "alertapp-valledupar/1.0 (proyecto academico UNAD)"})
    with urllib.request.urlopen(peticion, timeout=120) as r:
        osm = json.load(r)

    coords = {e["id"]: (e["lat"], e["lon"]) for e in osm["elements"] if e["type"] == "node"}
    vias = [e for e in osm["elements"] if e["type"] == "way"]

    # Un nodo es vértice del grafo si es extremo de una vía o lo comparten dos o más vías.
    usos = {}
    for v in vias:
        for i, n in enumerate(v["nodes"]):
            usos[n] = usos.get(n, 0) + (2 if i in (0, len(v["nodes"]) - 1) else 1)
    vertices = {n for n, u in usos.items() if u >= 2}

    indice = {}
    nodos = []
    aristas = []

    def id_nodo(osm_id):
        if osm_id not in indice:
            indice[osm_id] = len(nodos)
            lat, lon = coords[osm_id]
            nodos.append([round(lat, 7), round(lon, 7)])
        return indice[osm_id]

    for v in vias:
        etiquetas = v.get("tags", {})
        tipo = etiquetas["highway"]
        carro, bici, pie = TIPOS[tipo]
        sentido = etiquetas.get("oneway") in ("yes", "1", "true") or etiquetas.get("junction") == "roundabout"
        nombre = etiquetas.get("name", "")
        tramo = [v["nodes"][0]]
        for n in v["nodes"][1:]:
            tramo.append(n)
            if n in vertices:
                if len(tramo) >= 2 and all(t in coords for t in tramo):
                    puntos = [coords[t] for t in tramo]
                    largo = sum(distancia(puntos[i], puntos[i + 1]) for i in range(len(puntos) - 1))
                    if largo > 0:
                        aristas.append({
                            "a": id_nodo(tramo[0]), "b": id_nodo(tramo[-1]),
                            "m": round(largo, 1), "t": tipo, "n": nombre,
                            "u": 1 if sentido else 0,
                            "p": (1 if carro else 0) | (2 if bici else 0) | (4 if pie else 0),
                            "g": [[round(p[0], 6), round(p[1], 6)] for p in puntos[1:-1]],
                        })
                tramo = [n]

    SALIDA.parent.mkdir(parents=True, exist_ok=True)
    SALIDA.write_text(json.dumps({
        "fuente": "OpenStreetMap (ODbL), API Overpass",
        "caja": [SUR, OESTE, NORTE, ESTE],
        "nodos": nodos,
        "aristas": aristas,
    }, ensure_ascii=False, separators=(",", ":")))
    print(f"{len(nodos)} nodos, {len(aristas)} aristas -> {SALIDA.relative_to(RAIZ)} "
          f"({SALIDA.stat().st_size / 1024:.0f} KB)")


if __name__ == "__main__":
    main()
