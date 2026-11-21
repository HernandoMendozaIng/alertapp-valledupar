"""Prueba de carga del RNF-01: la API debe atender 10 transacciones por segundo sin degradarse.

Envía peticiones a ritmo constante durante un tiempo fijo, mezclando las consultas que más usa la app
(mapa, riesgo, puntos calientes, métricas y ruta segura), y reporta la latencia (mediana, p95 y máxima)
y los errores. Solo hace lecturas, así que no deja datos en la base.

Uso (con la API corriendo en :8080):
    python3 scripts/carga/prueba_carga.py                # 10 TPS durante 60 s
    python3 scripts/carga/prueba_carga.py --tps 20 --segundos 30
"""

import argparse
import json
import statistics
import threading
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor

API = "http://localhost:8080/api"
CORREO = "ciudadano@alertapp.co"
CLAVE = "Valledupar2026"

# Punto de referencia: barrio Primero de Mayo (Comuna 3).
LAT, LNG = 10.4569, -73.2562

CONSULTAS = [
    ("mapa", f"/reportes?lat={LAT}&lng={LNG}&radio=1000"),
    ("riesgo", f"/analitica/riesgo?lat={LAT}&lng={LNG}"),
    ("hotspots", "/analitica/hotspots?dias=30"),
    ("metricas", "/analitica/metricas"),
    ("ruta", f"/rutas/segura?origenLat={LAT}&origenLng={LNG}&destinoLat=10.4486&destinoLng=-73.2464&modo=A_PIE"),
]


def ingresar():
    cuerpo = json.dumps({"correo": CORREO, "clave": CLAVE}).encode()
    pet = urllib.request.Request(API + "/auth/ingreso", data=cuerpo, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(pet, timeout=10) as r:
        return json.load(r)["token"]


def consultar(token, nombre, ruta):
    pet = urllib.request.Request(API + ruta, headers={"Authorization": "Bearer " + token})
    inicio = time.perf_counter()
    try:
        with urllib.request.urlopen(pet, timeout=10) as r:
            r.read()
            ok = 200 <= r.status < 300
    except Exception:
        ok = False
    return nombre, time.perf_counter() - inicio, ok


def percentil(valores, p):
    orden = sorted(valores)
    return orden[min(len(orden) - 1, int(round(p / 100 * (len(orden) - 1))))]


def main():
    args = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    args.add_argument("--tps", type=float, default=10)
    args.add_argument("--segundos", type=int, default=60)
    a = args.parse_args()

    token = ingresar()
    total = int(a.tps * a.segundos)
    resultados, candado = [], threading.Lock()

    def guardar(futuro):
        with candado:
            resultados.append(futuro.result())

    print(f"Enviando {total} peticiones a {a.tps:g} por segundo durante {a.segundos} s...")
    inicio = time.perf_counter()
    with ThreadPoolExecutor(max_workers=50) as hilos:
        for i in range(total):
            # Ritmo constante: la petición i sale en el instante i / tps.
            espera = inicio + i / a.tps - time.perf_counter()
            if espera > 0:
                time.sleep(espera)
            nombre, ruta = CONSULTAS[i % len(CONSULTAS)]
            hilos.submit(consultar, token, nombre, ruta).add_done_callback(guardar)
    duracion = time.perf_counter() - inicio

    tiempos = [t for _, t, _ in resultados]
    errores = sum(1 for _, _, ok in resultados if not ok)
    print(f"\nPeticiones: {len(resultados)} en {duracion:.1f} s ({len(resultados) / duracion:.1f} por segundo)")
    print(f"Errores: {errores} ({100 * errores / len(resultados):.1f} %)")
    print(f"Latencia: mediana {statistics.median(tiempos) * 1000:.0f} ms, "
          f"p95 {percentil(tiempos, 95) * 1000:.0f} ms, máxima {max(tiempos) * 1000:.0f} ms\n")
    print(f"{'Consulta':<10} {'n':>4} {'mediana':>9} {'p95':>9}")
    for nombre, _ in CONSULTAS:
        t = [x for n, x, _ in resultados if n == nombre]
        print(f"{nombre:<10} {len(t):>4} {statistics.median(t) * 1000:>7.0f} ms {percentil(t, 95) * 1000:>6.0f} ms")


if __name__ == "__main__":
    main()
