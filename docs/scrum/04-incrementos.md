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

## Incremento de la ampliación (9 de noviembre)

**Funciona de extremo a extremo:**

- **Rutas seguras (HU 04):** origen y destino en el mapa, modo a pie, en bicicleta o en carro, y comparación con la
  ruta más corta. En la ruta de prueba "Casa al trabajo" a pie, la ruta segura pasa cerca de 1 reporte en lugar
  de 11, a cambio de caminar 523 m más. Se recalcula sola si entra un reporte cerca.
- **Alertas en tiempo real (HU 02):** el vecino que está a menos de 500 m (radio configurable de 200 m a 2 km)
  recibe el aviso en pantalla. La latencia medida en las pruebas fue de 0,04 s o menos (el criterio es 5 s).
- **Rutas frecuentes y seguimiento (HU 06):** guardar, editar y borrar rutas; compartir la ubicación de 5 a 60
  minutos y detenerlo en cualquier momento.
- **Verificación en dos pasos (HU 10):** activación con código QR, ingreso con código de la app autenticadora,
  cambio de contraseña y de correo. Recuperación de contraseña con código (HU 01).
- **Red de guardianes (HU 12):** hasta 3 guardianes; reciben el pánico al instante y ven la ubicación en vivo.
- 71 pruebas automáticas aprobadas (44 de la API, 5 de la IA y 22 de interfaz) e integración continua en GitHub.

**Pendiente para el siguiente sprint:** revisión de pantallas por el Product Owner y pruebas en el equipo de
cada integrante.

## Retrospectiva del Sprint 1

| Qué funcionó | Qué mejorar | Acción |
|---|---|---|
| Recortar el alcance a lo que exige TRL 5 permitió terminar las funciones completas | El documento de la Fase 3 prometía AWS, Android nativo y ArcGIS, que no caben en el tiempo | Explicar en el documento la diferencia entre arquitectura objetivo y arquitectura del prototipo |
| El respaldo en Java mantiene el mapa funcionando si la IA se cae | Los datos de prueba son sintéticos | Aclararlo en el documento y en el video; la validación con usuarios reales corresponde a TRL 6 |

## Retrospectiva de la ampliación

| Qué funcionó | Qué mejorar | Acción |
|---|---|---|
| Las pruebas de interfaz encontraron un error real en las calles de un solo sentido antes de la demostración | Las pruebas e2e dejan reportes de prueba en la base de demostración | `scripts/reiniciar-datos.sh` deja la base limpia antes de grabar el video |
| Medir la latencia con una prueba automática deja evidencia objetiva del criterio de 5 s | El push con la app cerrada y el SMS no se pueden probar sin servicios pagos | Documentarlos como parte de TRL 6 con su reemplazo en AWS |
