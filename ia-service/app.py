"""
AlertApp Valledupar - microservicio de analítica e IA.

Expone dos operaciones que consume la API en Java:

* POST /hotspots: agrupa los reportes con DBSCAN (scikit-learn, métrica haversine)
  y devuelve las zonas calientes con su nivel de riesgo y la franja horaria pico.
* POST /riesgo: calcula un índice de riesgo de 0 a 100 para una ubicación y un momento,
  ponderando cercanía, antigüedad, similitud de hora y severidad de los reportes vecinos.

DBSCAN (Ester et al., 1996) es un algoritmo de aprendizaje no supervisado: no necesita
saber cuántas zonas existen y separa como ruido los reportes aislados.
"""

from __future__ import annotations

import math
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

import numpy as np
from fastapi import FastAPI
from pydantic import BaseModel, Field
from sklearn.cluster import DBSCAN

ZONA = ZoneInfo("America/Bogota")
RADIO_TIERRA_M = 6_371_000.0

app = FastAPI(title="AlertApp IA", version="0.1.0")


class Punto(BaseModel):
    id: int
    lat: float
    lng: float
    fecha: datetime
    severidad: int = Field(ge=0, le=100)


class SolicitudHotspots(BaseModel):
    puntos: list[Punto]
    epsMetros: float = Field(default=200, gt=0, le=2000)
    minPuntos: int = Field(default=4, ge=2, le=50)


class Hotspot(BaseModel):
    id: int
    lat: float
    lng: float
    radioMetros: float
    reportes: int
    severidadMedia: float
    nivel: str
    puntaje: int
    franjaPico: str


class RespuestaHotspots(BaseModel):
    motor: str
    hotspots: list[Hotspot]
    ruido: int


class SolicitudRiesgo(BaseModel):
    puntos: list[Punto]
    lat: float
    lng: float
    momento: datetime


class Riesgo(BaseModel):
    nivel: str
    indice: float
    franja: str
    explicacion: str
    motor: str


def distancia_m(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    d_lat = math.radians(lat2 - lat1)
    d_lon = math.radians(lon2 - lon1)
    a = math.sin(d_lat / 2) ** 2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(d_lon / 2) ** 2
    return 2 * RADIO_TIERRA_M * math.asin(math.sqrt(a))


def resumir(grupo_id: int, grupo: list[Punto], ahora: datetime) -> Hotspot:
    lat = float(np.mean([p.lat for p in grupo]))
    lng = float(np.mean([p.lng for p in grupo]))
    radio = max(distancia_m(lat, lng, p.lat, p.lng) for p in grupo)
    sev = float(np.mean([p.severidad for p in grupo]))
    recientes = sum(1 for p in grupo if p.fecha > ahora - timedelta(days=30))
    # Puntaje 0-100: volumen (hasta 50), severidad media (hasta 30), actividad del último mes (hasta 20).
    puntaje = round(min(len(grupo) / 30, 1) * 50 + sev / 100 * 30 + min(recientes / 10, 1) * 20)
    nivel = "ALTO" if puntaje >= 60 else "MEDIO" if puntaje >= 35 else "BAJO"
    por_franja = [0] * 6
    for p in grupo:
        por_franja[p.fecha.astimezone(ZONA).hour // 4] += 1
    pico = int(np.argmax(por_franja))
    return Hotspot(
        id=grupo_id,
        lat=lat,
        lng=lng,
        radioMetros=max(radio, 60.0),
        reportes=len(grupo),
        severidadMedia=round(sev, 1),
        nivel=nivel,
        puntaje=puntaje,
        franjaPico=f"{pico * 4:02d}:00-{pico * 4 + 4:02d}:00",
    )


@app.get("/salud")
def salud() -> dict:
    return {"estado": "ok", "servicio": "alertapp-ia"}


@app.post("/hotspots", response_model=RespuestaHotspots)
def hotspots(sol: SolicitudHotspots) -> RespuestaHotspots:
    if len(sol.puntos) < sol.minPuntos:
        return RespuestaHotspots(motor="python-sklearn-dbscan", hotspots=[], ruido=len(sol.puntos))
    coords = np.radians([[p.lat, p.lng] for p in sol.puntos])
    modelo = DBSCAN(eps=sol.epsMetros / RADIO_TIERRA_M, min_samples=sol.minPuntos,
                    metric="haversine", algorithm="ball_tree")
    etiquetas = modelo.fit_predict(coords)
    ahora = datetime.now(ZONA)
    zonas = []
    for etiqueta in sorted(set(etiquetas) - {-1}):
        grupo = [p for p, e in zip(sol.puntos, etiquetas) if e == etiqueta]
        zonas.append(resumir(int(etiqueta), grupo, ahora))
    zonas.sort(key=lambda z: z.puntaje, reverse=True)
    return RespuestaHotspots(motor="python-sklearn-dbscan", hotspots=zonas, ruido=int(np.sum(etiquetas == -1)))


@app.post("/riesgo", response_model=Riesgo)
def riesgo(sol: SolicitudRiesgo) -> Riesgo:
    hora_actual = sol.momento.astimezone(ZONA).hour
    suma = 0.0
    cercanos = 0
    for p in sol.puntos:
        d = distancia_m(sol.lat, sol.lng, p.lat, p.lng)
        if d > 500:
            continue
        cercanos += 1
        dias = max(0.0, (sol.momento - p.fecha).total_seconds() / 86_400)
        hp = p.fecha.astimezone(ZONA).hour
        dh = min(abs(hp - hora_actual), 24 - abs(hp - hora_actual))
        peso = (
            math.exp(-d / 250)                  # cercanía
            * 0.5 ** (dias / 30)                # vida media de 30 días
            * math.exp(-(dh ** 2) / (2 * 2.0 ** 2))  # similitud de hora (sigma 2 h)
            * (0.5 + p.severidad / 100)         # severidad
        )
        suma += peso
    indice = round(100 * (1 - math.exp(-suma / 1.5)), 1)
    nivel = "ALTO" if indice >= 60 else "MEDIO" if indice >= 30 else "BAJO"
    if cercanos == 0:
        explicacion = "No hay reportes en 500 m a la redonda en los últimos 90 días."
    else:
        explicacion = (f"{cercanos} {'reporte' if cercanos == 1 else 'reportes'} en 500 m a la redonda; pesan más los recientes, "
                       "cercanos y de esta franja horaria.")
    return Riesgo(nivel=nivel, indice=indice, franja=f"{hora_actual:02d}:00-{(hora_actual + 1) % 24:02d}:00",
                  explicacion=explicacion, motor="python-modelo-ponderado")
