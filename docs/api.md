# Referencia de la API

Todas las rutas empiezan por `/api`, reciben y devuelven JSON y, salvo las de acceso, necesitan la cabecera
`Authorization: Bearer <token>`. La documentación interactiva, generada por springdoc a partir del código, queda en
http://localhost:8080/docs con la API en marcha; esta página es el resumen para leer sin correrla.

Los errores siguen siempre la misma forma:

```json
{ "mensaje": "El correo no es válido", "campos": { "correo": "El correo no es válido" } }
```

| Código | Cuándo |
|---|---|
| 400 | Datos inválidos (el detalle por campo viene en `campos`) |
| 401 | Sin sesión, sesión vencida o credenciales incorrectas |
| 403 | La sesión es válida pero el rol no alcanza (panel de autoridad) |
| 404 | El recurso no existe o no es del usuario |
| 409 | Conflicto de estado: correo repetido, cuarto guardián, transición no permitida |
| 415 | La foto no es JPG, PNG ni WebP |
| 422 | La ruta no se puede calcular (fuera del área, sin calle cercana) |
| 429 | Demasiados intentos fallidos de ingreso |

## Acceso y cuenta (HU 01, HU 10)

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/auth/registro` | Crea la cuenta de ciudadano y devuelve la sesión |
| POST | `/auth/ingreso` | Devuelve la sesión, o `requiereSegundoFactor` y un `desafio` si la cuenta tiene 2FA |
| POST | `/auth/ingreso/2fa` | Canjea el desafío y el código de 6 dígitos por la sesión |
| POST | `/auth/recuperar` | Envía un código de recuperación (responde 202 exista o no la cuenta) |
| POST | `/auth/recuperar/confirmar` | Cambia la contraseña con el código |
| GET / PUT | `/auth/yo` | Consulta o edita nombre, celular y comuna |
| PUT | `/auth/yo/preferencias` | Activa o desactiva las alertas cercanas y su radio (200 a 2000 m) |
| PUT | `/auth/yo/ubicacion` | Guarda la última ubicación (para saber a quién alertar) |
| PUT | `/auth/yo/clave` | Cambia la contraseña con la actual |
| PUT | `/auth/yo/correo` | Cambia el correo con la contraseña |
| POST | `/auth/2fa/preparar` | Genera el secreto y la URI `otpauth://` para el código QR |
| POST | `/auth/2fa/activar` | Activa la 2FA si el código de la app es válido |
| POST | `/auth/2fa/desactivar` | Desactiva la 2FA con contraseña y código |

## Reportes y comunidad (HU 03, HU 05, HU 07)

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/reportes` | Lista con filtros `estado`, `comuna`, `tipo`, `severidad`, `desde`, `hasta`, `lat`, `lng`, `radio` |
| GET | `/reportes/mios` | Reportes del usuario |
| GET | `/reportes/{id}` | Detalle |
| POST | `/reportes` | Crea el reporte (JSON, o multipart con `datos` y `foto`) |
| PUT / DELETE | `/reportes/{id}` | Edita o borra; solo el autor y mientras esté pendiente |
| GET | `/reportes/{id}/evidencia` | Descarga la foto |
| PUT / DELETE | `/reportes/{id}/voto` | Vota `CONFIRMA` o `FALSO`, o retira el voto |
| GET / POST | `/reportes/{id}/comentarios` | Lista o agrega comentarios |
| DELETE | `/comentarios/{id}` | Borra un comentario propio |

## Analítica e IA (HU 03, HU 08, HU 09)

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/analitica/hotspots` | Puntos calientes con DBSCAN (`dias`, `comuna`) |
| GET | `/analitica/riesgo` | Índice de riesgo de una ubicación (`lat`, `lng`) |
| GET | `/analitica/metricas` | Conteos por hora, día, tipo, comuna, severidad y semana |
| GET | `/analitica/exportar` | CSV para Excel (solo autoridad) |

## Rutas, seguimiento y guardianes (HU 04, HU 06, HU 12)

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/rutas/segura` | Ruta segura y ruta corta (`origenLat`, `origenLng`, `destinoLat`, `destinoLng`, `modo`) |
| GET / POST | `/rutas-frecuentes` | Lista o guarda una ruta frecuente (máximo 10) |
| PUT / DELETE | `/rutas-frecuentes/{id}` | Edita o borra |
| GET | `/seguimiento` | Seguimiento activo (204 si no hay) |
| POST | `/seguimiento` | Empieza a compartir la ubicación (`minutos` de 5 a 60, `rutaId` opcional) |
| PUT | `/seguimiento/posicion` | Actualiza la posición; la reciben los guardianes al instante |
| POST | `/seguimiento/detener` | Deja de compartir |
| GET / POST | `/guardianes` | Lista o agrega un guardián por correo (máximo 3) |
| DELETE | `/guardianes/{id}` | Quita un guardián |
| GET | `/guardianes/protegidos` | Personas que me eligieron, con su ubicación solo si están en alerta o en trayecto |

## Pánico y panel de autoridad (HU 11, HU 13)

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/alertas` | Activa la alerta de pánico (avisa a guardianes y abre un seguimiento de 1 hora) |
| POST | `/alertas/{id}/cancelar` | La cancela el mismo usuario |
| GET | `/alertas/mias` | Historial del usuario |
| GET | `/panel/alertas` | Alertas recientes (autoridad) |
| POST | `/panel/alertas/{id}/atender` | Marca la alerta como atendida (autoridad) |
| PATCH | `/panel/reportes/{id}/estado` | Cambia el estado con nota opcional y avisa al autor (autoridad) |

## Notificaciones en tiempo real (HU 02)

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/notificaciones/flujo` | Conexión Server-Sent Events que queda abierta |
| GET | `/notificaciones` | Bandeja: `sinLeer` y las últimas 30 |
| POST | `/notificaciones/{id}/leida` | Marca una como leída |
| POST | `/notificaciones/leidas` | Marca todas |

Eventos del flujo:

| Evento | Para quién | Datos |
|---|---|---|
| `conectado` | Quien abre la conexión | `{ "ok": true }` |
| `notificacion` | El destinatario | La notificación completa (reporte cercano, pánico, trayecto, guardián nuevo, cambio de estado) |
| `reporte` | Todos los conectados | `id`, `latitud`, `longitud`, `severidad` del reporte nuevo |
| `ubicacion` | Los guardianes | Nueva posición del protegido |
| `protegido` | Los guardianes | El estado del protegido cambió (alerta cerrada o trayecto detenido) |

`GET /api/salud` responde sin sesión y sirve para comprobar que la API está arriba.
