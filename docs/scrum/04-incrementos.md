# Incrementos y revisiones de sprint

## Incremento del Sprint 1

**Funciona de extremo a extremo:**

- Registro e ingreso con contraseña cifrada (BCrypt) y token JWT con vigencia de 12 horas.
- Formulario guiado de reporte con detección automática de la comuna sobre el mapa, foto opcional verificada
  por su firma de archivo y anonimato por defecto. La API asigna un puntaje de severidad de 0 a 100.
- Mapa con los límites de las Comunas 3 y 4, reportes por severidad, puntos calientes calculados con DBSCAN
  en el microservicio de IA y nivel de riesgo de la ubicación actual.
- Validación comunitaria: un voto por vecino, cambio de voto, validación con 3 confirmaciones de diferencia y
  anulación con 3 votos de falso. Comentarios con solo el primer nombre.
- Botón de pánico con cuenta regresiva de 5 segundos, envío de la ubicación y atención desde el panel.
- Panel de autoridad con alertas activas, cambio de estado de reportes, exportación CSV e informe semanal.
- Métricas por hora, día, tipo y comuna con filtros.
- Límite de 5 intentos de ingreso fallidos cada 15 minutos contra fuerza bruta.
- 24 pruebas automáticas aprobadas (19 de la API y 5 de la IA).

**Pendiente para el siguiente sprint:** revisión de pantallas por el Product Owner y pruebas en el equipo de
cada integrante.

## Retrospectiva del Sprint 1

| Qué funcionó | Qué mejorar | Acción |
|---|---|---|
| Recortar el alcance a lo que exige TRL 5 permitió terminar las funciones completas | El documento de la Fase 3 prometía AWS, Android nativo y ArcGIS, que no caben en el tiempo | Explicar en el documento la diferencia entre arquitectura objetivo y arquitectura del prototipo |
| El respaldo en Java mantiene el mapa funcionando si la IA se cae | Los datos de prueba son sintéticos | Aclararlo en el documento y en el video; la validación con usuarios reales corresponde a TRL 6 |
