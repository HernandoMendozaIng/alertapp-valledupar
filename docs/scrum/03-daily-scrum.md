# Bitácora de daily scrum

Cada registro responde tres preguntas: qué hice desde el último daily, qué haré ahora y qué me bloquea.
Se registra el día en que ocurre; no se completan días hacia atrás.

## 6 de noviembre de 2026 (Sprint 1)

**Hernando Mendoza**
- Hice: revisé la guía de la Fase 4, la grabación de la webconferencia y los archivos del prototipo de Justinmind.
  Definimos con Armel construir una versión reducida con Java, React y PostgreSQL.
- Haré: API, base de datos, microservicio de IA e interfaz; pruebas automáticas y de extremo a extremo.
- Bloqueos: la Comuna 4 no aparece como límite propio en OpenStreetMap; se resolvió calculándola a partir del
  perímetro urbano menos las otras comunas.

**Armel Peña Rangel**
- Hice:
- Haré:
- Bloqueos:

## 9 de noviembre de 2026 (Sprint 1)

**Hernando Mendoza**
- Hice: dejé el prototipo corriendo en local y lo revisé completo. Como el núcleo quedó probado, agregué las
  historias que faltaban del backlog: rutas seguras (HU 04), alertas en tiempo real (HU 02), rutas frecuentes y
  seguimiento (HU 06), verificación en dos pasos (HU 10) y red de guardianes (HU 12). Sumé 22 pruebas de interfaz
  con Playwright y la integración continua en GitHub Actions.
- Haré: actualizar la documentación del repositorio y pasar al documento maestro los diagramas y las capturas.
- Bloqueos: la descarga de Chromium para Playwright se quedó colgada; las pruebas usan el Google Chrome instalado.
  En la prueba de rutas apareció un error real (el destino en una calle de un solo sentido se enganchaba a otra
  esquina) y quedó corregido.

**Armel Peña Rangel**
- Hice:
- Haré:
- Bloqueos:

## 10 de noviembre de 2026 (Sprint 2)

**Hernando Mendoza**
- Hice: preparé el mensaje de avance para el foro. Escribí la prueba de carga del RNF-01
  (`scripts/carga/prueba_carga.py`) y la corrí: 10 transacciones por segundo durante 60 s sin errores (p95 de
  22 ms) y 50 por segundo durante 30 s sin errores (p95 de 14 ms). Rehíce los diagramas de casos de uso y de
  componentes para el documento y volví a tomar las capturas con la base limpia.
- Haré: terminar los capítulos por objetivo del documento maestro y revisarlos con Armel.
- Bloqueos: el servidor de teselas de OpenStreetMap no respondió en la tarde, así que las capturas con mapa se
  dejaron en la versión del 9 de noviembre.

**Armel Peña Rangel**
- Hice:
- Haré:
- Bloqueos:

## Plantilla

```
## DD de noviembre de 2026 (Sprint N)

**Nombre**
- Hice:
- Haré:
- Bloqueos:
```
