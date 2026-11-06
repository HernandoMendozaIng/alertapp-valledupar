"""Pruebas del microservicio de IA (pytest)."""

from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

from fastapi.testclient import TestClient

from app import app

cliente = TestClient(app)
ZONA = ZoneInfo("America/Bogota")
AHORA = datetime.now(ZONA)


def punto(i, lat, lng, dias=3, hora=19, sev=70):
    fecha = (AHORA - timedelta(days=dias)).replace(hour=hora, minute=0, second=0, microsecond=0)
    return {"id": i, "lat": lat, "lng": lng, "fecha": fecha.isoformat(), "severidad": sev}


def test_salud():
    assert cliente.get("/salud").json()["estado"] == "ok"


def test_dbscan_encuentra_un_grupo_y_separa_ruido():
    # Diez reportes a menos de 100 m entre sí y dos aislados a más de 1 km.
    puntos = [punto(i, 10.4540 + i * 0.0001, -73.2529 + i * 0.0001) for i in range(10)]
    puntos += [punto(100, 10.4700, -73.2700), punto(101, 10.4300, -73.2400)]
    r = cliente.post("/hotspots", json={"puntos": puntos, "epsMetros": 200, "minPuntos": 4}).json()
    assert r["motor"] == "python-sklearn-dbscan"
    assert len(r["hotspots"]) == 1
    assert r["hotspots"][0]["reportes"] == 10
    assert r["ruido"] == 2
    assert r["hotspots"][0]["franjaPico"] == "16:00-20:00"


def test_pocos_puntos_no_forman_hotspot():
    r = cliente.post("/hotspots", json={"puntos": [punto(1, 10.45, -73.25)], "minPuntos": 4}).json()
    assert r["hotspots"] == []


def test_riesgo_alto_cerca_de_reportes_recientes():
    puntos = [punto(i, 10.4540, -73.2529, dias=1, hora=AHORA.hour) for i in range(8)]
    r = cliente.post("/riesgo", json={"puntos": puntos, "lat": 10.4541, "lng": -73.2530,
                                      "momento": AHORA.isoformat()}).json()
    assert r["nivel"] == "ALTO"
    assert r["indice"] >= 60


def test_riesgo_bajo_sin_reportes_cercanos():
    puntos = [punto(1, 10.49, -73.27)]
    r = cliente.post("/riesgo", json={"puntos": puntos, "lat": 10.44, "lng": -73.25,
                                      "momento": AHORA.isoformat()}).json()
    assert r["nivel"] == "BAJO"
    assert r["indice"] == 0
