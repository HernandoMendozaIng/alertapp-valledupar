# Planificación de sprints (Fase 4: 25 de octubre a 22 de noviembre de 2026)

Por el tiempo de la fase se trabajan tres sprints cortos en lugar de los 9 sprints de dos semanas del
cronograma completo. Cada sprint termina con una revisión (demostración del incremento) y una retrospectiva.

## Sprint 1. Construcción del prototipo funcional

- **Fechas:** 6 al 9 de noviembre de 2026
- **Objetivo:** tener funcionando de extremo a extremo el reporte, la validación, el mapa con IA, el botón de pánico y el panel.

| Tarea | Historia | Responsable | Estimado | Estado |
|---|---|---|---|---|
| Modelo entidad-relación y migración Flyway | HU 0.3 | Hernando Mendoza | 3 h | Hecho |
| API de registro, ingreso y JWT | HU 01 | Hernando Mendoza | 3 h | Hecho |
| API de reportes con foto y clasificador de severidad | HU 05 | Hernando Mendoza | 4 h | Hecho |
| Votos, anulación automática y comentarios | HU 07 | Hernando Mendoza | 3 h | Hecho |
| Microservicio de IA (DBSCAN e índice de riesgo) | HU 03, HU 08 | Hernando Mendoza | 4 h | Hecho |
| Alertas de pánico y panel de autoridad | HU 11, HU 13 | Hernando Mendoza | 3 h | Hecho |
| Interfaz web móvil (mapa, reporte, comunidad, métricas, perfil) | HU 03, 05, 07, 08 | Hernando Mendoza | 8 h | Hecho |
| Límites reales de las Comunas 3 y 4 desde OpenStreetMap | HU 03 | Hernando Mendoza | 1 h | Hecho |
| Revisión de las pantallas frente al diseño de Justinmind | HU 0.2 | Armel Peña Rangel | 2 h | Por hacer |

**Ampliación del 9 de noviembre.** Con el núcleo terminado antes de lo previsto, se agregaron al sprint las
historias que estaban como trabajo futuro, para que el prototipo cubra todo el backlog del documento:

| Tarea | Historia | Responsable | Estimado | Estado |
|---|---|---|---|---|
| Red vial de las Comunas 3 y 4 desde OpenStreetMap y algoritmo A* con riesgo | HU 04 | Hernando Mendoza | 4 h | Hecho |
| Canal en tiempo real (SSE) y avisos por cercanía | HU 02 | Hernando Mendoza | 3 h | Hecho |
| Guardianes, rutas frecuentes y ubicación compartida | HU 06, HU 12 | Hernando Mendoza | 4 h | Hecho |
| Verificación en dos pasos (TOTP), cambio de clave y correo, recuperación | HU 10, HU 01 | Hernando Mendoza | 3 h | Hecho |
| Pantallas nuevas: Rutas, Red, Avisos, Seguridad, Recuperar | HU 02, 04, 06, 10, 12 | Hernando Mendoza | 5 h | Hecho |
| Pruebas automáticas de interfaz con Playwright e integración continua | Pruebas | Hernando Mendoza | 3 h | Hecho |

## Sprint 2. Pruebas y documento maestro

- **Fechas:** 10 al 16 de noviembre de 2026
- **Objetivo:** dejar evidencias de las pruebas funcionales y el documento maestro ajustado con las 11 recomendaciones del tutor.

| Tarea | Responsable | Estimado | Estado |
|---|---|---|---|
| Ejecutar el protocolo de pruebas funcionales en el equipo de cada integrante | Armel y Hernando | 2 h | En curso |
| Ajustes de título, delimitación, antecedentes y marco conceptual | Armel Peña Rangel | 6 h | Por hacer |
| Prueba de carga del RNF-01 (10 transacciones por segundo) | Hernando Mendoza | 1 h | Hecho |
| Capítulos de resultados por objetivo específico (requisitos, diseño, implementación, evaluación) | Hernando Mendoza | 6 h | En curso |
| Diagramas UML y modelo entidad-relación en el documento | Hernando Mendoza | 2 h | Hecho |
| Ajuste de muestra, recursos, resultados esperados y conclusiones | Armel Peña Rangel | 4 h | Por hacer |
| Publicar avance en el foro con la descripción de lo hecho por cada uno | Armel y Hernando | 1 h | Por hacer |

## Sprint 3. Video y entrega

- **Fechas:** 17 al 22 de noviembre de 2026
- **Objetivo:** grabar el video, revisar en Turnitin y entregar.

| Tarea | Responsable | Estimado | Estado |
|---|---|---|---|
| Guion del video (máximo 10 minutos) | Hernando Mendoza | 1 h | Por hacer |
| Grabación con los dos integrantes en cámara | Armel y Hernando | 2 h | Por hacer |
| Anexos con enlaces de GitHub y del video | Hernando Mendoza | 0,5 h | Por hacer |
| Revisión de similitud en Turnitin y normas APA 7 | Armel Peña Rangel | 1 h | Por hacer |
| Aprobación del documento final en el foro y entrega | Armel Peña Rangel | 0,5 h | Por hacer |
