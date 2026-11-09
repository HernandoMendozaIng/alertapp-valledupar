# Pruebas funcionales del prototipo (TRL 5)

## Criterio

El tutor definió para esta fase que el nivel TRL 5 se verifica con **pruebas funcionales**: cada función debe
cumplir su objetivo de extremo a extremo (la acción en la interfaz se guarda en la base de datos y la aplicación
lo confirma) y el prototipo debe cumplir al menos el **80 %** de las funciones previstas.

Entorno: macOS, Java 21, PostgreSQL 17, Python 3.13, Node 26 y Google Chrome. Las pruebas de interfaz simulan un
celular Pixel 7 (412 × 915) con la ubicación en el barrio Primero de Mayo. Datos: 154 reportes sintéticos de
prueba dentro de las Comunas 3 y 4.

Resumen al 9 de noviembre de 2026:

| Nivel | Herramienta | Pruebas | Aprobadas |
|---|---|---|---|
| API (unitarias y de integración) | JUnit 5, MockMvc, H2 | 44 | 44 |
| Modelo de IA | pytest | 5 | 5 |
| Interfaz de extremo a extremo | Playwright | 22 | 22 |
| **Total** | | **71** | **71** |

Cómo repetirlas:

```bash
cd backend && mvn test                         # API
cd ia-service && .venv/bin/python -m pytest    # IA
cd frontend && npm run test:e2e                # interfaz (con los tres servicios corriendo)
```

En GitHub, cada push a `main` y cada pull request corre las pruebas de la API, de la IA, el lint y la compilación
de la web (`.github/workflows/pruebas.yml`).

## 1. Pruebas de la API

Cada prueba hace las peticiones HTTP reales a la API y después consulta el dato para comprobar que quedó guardado.

### 1.1 Núcleo (Sprint 1)

| ID | Historia | Qué se verifica | Resultado |
|---|---|---|---|
| PF-01 | HU 01 | Registro, ingreso con JWT y consulta del perfil | Aprobada |
| PF-02 | HU 01 | Rechazo de correo repetido, clave débil, política no aceptada y clave incorrecta | Aprobada |
| PF-03 | HU 05, HU 03 | Crear reporte, severidad ALTA calculada, anonimato, búsqueda en radio de 200 m | Aprobada |
| PF-04 | HU 05 | Foto JPG aceptada y descargable; archivo que no es imagen rechazado (415) | Aprobada |
| PF-05 | HU 05 | Editar (recalcula severidad) y eliminar solo por el autor | Aprobada |
| PF-06 | HU 07 | El autor no puede votar; 3 confirmaciones validan; reporte validado no se edita | Aprobada |
| PF-07 | HU 07 | Cambio de voto recalcula; 3 votos de falso anulan; reporte anulado no recibe votos | Aprobada |
| PF-08 | HU 07 | Comentarios guardados en orden y con solo el primer nombre | Aprobada |
| PF-09 | HU 13, HU 09 | Ciudadano sin acceso al panel; autoridad cambia estado con nota; transición inválida rechazada; CSV solo para autoridad | Aprobada |
| PF-10 | HU 11 | Alerta de pánico única por usuario, visible y atendida por la autoridad | Aprobada |
| PF-11 | HU 08, HU 03 | Métricas por hora y totales; hotspots y riesgo con el respaldo en Java (IA apagada) | Aprobada |
| PF-12 | Seguridad | Sin token responde 401; token con el rol alterado se rechaza | Aprobada |
| PF-13 | Seguridad | Tras 5 contraseñas erradas el ingreso se bloquea 15 minutos (429) | Aprobada |

### 1.2 Ampliación de seguridad personal

| ID | Historia | Qué se verifica | Resultado |
|---|---|---|---|
| PF-14 | HU 02 | Un reporte avisa al vecino a 150 m; no al que está lejos, ni al que apagó las alertas, ni al autor | Aprobada |
| PF-15 | HU 02 | El radio de alerta solo acepta entre 200 m y 2 km | Aprobada |
| PF-16 | HU 04 | Ruta segura y corta en los tres modos; la segura nunca tiene más exposición que la corta; puntos fuera del área rechazados (422) | Aprobada |
| PF-17 | HU 06 | Guardar, editar, listar y borrar rutas frecuentes; otro usuario no puede borrarlas | Aprobada |
| PF-18 | HU 06, HU 12 | Seguimiento rechaza más de 60 min; dura exactamente 60; el guardián ve la posición; al detenerlo deja de verla | Aprobada |
| PF-19 | HU 12 | Máximo 3 guardianes; no uno mismo; solo cuentas existentes; al quitar uno se puede agregar otro | Aprobada |
| PF-20 | HU 12, HU 11 | El pánico llega al guardián con ubicación; la posición se actualiza; un extraño no ve nada; al cancelar vuelve a "sin alertas" | Aprobada |
| PF-21 | HU 10 | 2FA: código erróneo rechazado; activación; ingreso en dos pasos; el desafío no sirve como sesión; desactivación | Aprobada |
| PF-22 | HU 10 | Cambiar clave y correo exige la contraseña actual | Aprobada |
| PF-23 | HU 01 | Recuperación: responde igual si el correo no existe; el código sirve una sola vez | Aprobada |
| PF-24 | HU 13 | El autor recibe un aviso cuando la autoridad cambia el estado de su reporte | Aprobada |
| PF-25 | HU 02 | **Latencia**: con el servidor en un puerto real, la alerta llega por el canal en tiempo real en menos de 5 s | Aprobada (0,007 a 0,019 s) |

### 1.3 Pruebas unitarias

| ID | Componente | Qué se verifica | Resultado |
|---|---|---|---|
| PU-01 a PU-04 | Clasificador de severidad | Puntajes alta, media, baja y tope de 100 | Aprobadas |
| PU-05 a PU-06 | DBSCAN | Distancia haversine y agrupamiento con ruido | Aprobadas |
| PU-07 a PU-10 | TOTP | Vectores oficiales del RFC 6238, Base32, tolerancia de un paso, URI para la app | Aprobadas |
| PU-11 a PU-13 | Códigos de recuperación | Un solo uso, vencen a los 15 minutos, se anulan tras 5 intentos | Aprobadas |
| PU-14 | Alertas cercanas | La distancia se redondea para no delatar la ubicación exacta | Aprobada |
| PU-15 a PU-19 | Planificador de rutas | Sin riesgo coinciden; con un atraco toma el rodeo; el carro no usa pasajes peatonales ni va en contravía; fuera del área; peso por franja horaria | Aprobadas |

## 2. Pruebas del modelo de IA

| ID | Qué se verifica | Resultado |
|---|---|---|
| IA-01 | El servicio responde | Aprobada |
| IA-02 | DBSCAN agrupa 10 reportes cercanos en una zona, separa 2 aislados como ruido y calcula la franja pico | Aprobada |
| IA-03 | Con menos puntos que el mínimo no se forma zona | Aprobada |
| IA-04 | Riesgo ALTO junto a 8 reportes recientes en la misma hora | Aprobada |
| IA-05 | Riesgo BAJO (0) sin reportes a 500 m | Aprobada |

## 3. Pruebas de extremo a extremo desde la interfaz

Automatizadas con Playwright en `frontend/e2e/`. Cada prueba usa la aplicación completa (web, API, IA y
PostgreSQL) como lo haría una persona: toca los botones, escribe en los campos y comprueba lo que aparece.

| ID | Historia | Qué hace la prueba | Resultado |
|---|---|---|---|
| UI-01 | HU 01 | Ingresa con la cuenta de ciudadano, ve el saludo y sale | Aprobada |
| UI-02 | HU 01 | Contraseña equivocada muestra "Correo o contraseña incorrectos" | Aprobada |
| UI-03 | HU 01 | Recuperar contraseña pide el código después del correo | Aprobada |
| UI-04 | HU 01 | El registro valida los datos antes de crear la cuenta | Aprobada |
| UI-05 | HU 03 | El mapa carga reportes, puntos calientes y nivel de riesgo; filtro de 200 m | Aprobada |
| UI-06 | HU 05 | Reporte guiado completo hasta ver el detalle en estado Pendiente | Aprobada |
| UI-07 | HU 04 | Ruta frecuente: muestra la ruta segura, la corta y la explicación | Aprobada |
| UI-08 | HU 04 | Cambiar a "Carro o moto" recalcula la ruta | Aprobada |
| UI-09 | HU 04 | Marcar el destino tocando el mapa | Aprobada |
| UI-10 | HU 06 | Compartir el trayecto 15 minutos y detenerlo | Aprobada |
| UI-11 | HU 06 | Guardar, renombrar y borrar una ruta frecuente | Aprobada |
| UI-12 | HU 12 | Agregar un tercer guardián (el campo se bloquea) y quitarlo | Aprobada |
| UI-13 | HU 12, HU 11 | El guardián ve el pánico de Luis en vivo, con aviso y mapa, y lo deja de ver al cancelarse | Aprobada |
| UI-14 | HU 02 | Un reporte a pocos metros llega como aviso en menos de 5 s y se marca como leído | Aprobada (0,03 a 0,04 s en tres corridas) |
| UI-15 | HU 11 | SOS abre la cuenta regresiva y se puede cancelar | Aprobada |
| UI-16 | HU 10 | Activar 2FA con el QR, salir, ingresar con el código y desactivarla | Aprobada |
| UI-17 | HU 02, HU 10 | Desactivar y activar las alertas cercanas | Aprobada |
| UI-18 | HU 10 | Cambiar la contraseña con la actual y restaurarla | Aprobada |
| UI-19 | HU 07 | Votar "Confirmo" en el reporte de un vecino | Aprobada |
| UI-20 | HU 08 | Métricas con filtro de comuna sin recargar | Aprobada |
| UI-21 | HU 13 | La autoridad pasa un reporte a "En revisión" con nota y el autor recibe el aviso | Aprobada |
| UI-22 | HU 09 | Exportar el CSV de la semana | Aprobada |

La latencia de UI-14 incluye el envío del reporte por la API, el aviso por el canal en tiempo real y el dibujo en
la pantalla; el criterio de la HU 02 (5 s o menos al 95 %) se cumple con amplio margen.

### 3.1 Verificación en la base de datos (Sprint 1)

Además de las pruebas automáticas, el 6 de noviembre se verificó a mano en PostgreSQL con `psql` que cada acción
de la interfaz quedara guardada: reporte 155 con severidad `ALTA` y 78 puntos en `PENDIENTE`, voto `CONFIRMA` de
vecino1, alerta de pánico de `ACTIVA` a `ATENDIDA` con la nota "Patrulla enviada al sitio", reporte 9 de
`VALIDADO` a `EN_REVISION`, el filtro de métricas de 142 a 87 reportes al elegir la Comuna 3 y el CSV separado por
punto y coma que abre en Excel.

## 4. Cumplimiento de funciones

| Función prevista para el prototipo | Historia | Estado |
|---|---|---|
| Registro, ingreso, perfil y recuperación de contraseña | HU 01 | Cumple |
| Alertas geolocalizadas en tiempo real | HU 02 | Cumple (web; push con la app cerrada en TRL 6) |
| Mapa de riesgo con hotspots y filtro de 200 m | HU 03 | Cumple |
| Rutas seguras a pie, en bicicleta y en vehículo | HU 04 | Cumple |
| Reporte guiado con foto y anonimato | HU 05 | Cumple (video y audio en TRL 6) |
| Rutas frecuentes y seguimiento de hasta 1 hora | HU 06 | Cumple |
| Validación comunitaria con anulación automática | HU 07 | Cumple |
| Métricas con filtros | HU 08 | Cumple |
| Exportación e informe para autoridades | HU 09 | Cumple |
| Verificación en dos pasos y cambios de cuenta | HU 10 | Cumple |
| Botón de pánico con cancelación de 5 s | HU 11 | Cumple |
| Red de hasta 3 guardianes con ubicación en vivo | HU 12 | Cumple |
| Estados del caso, notas y aviso al ciudadano | HU 13 | Cumple (sin chat directo) |
| Panel de autoridad con atención de alertas | HU 09, HU 11 | Cumple |
| Analítica con IA (DBSCAN e índice de riesgo) | HU 03, HU 08 | Cumple |

**15 de 15 funciones previstas cumplen (100 %)**, por encima del 80 % exigido.

Lo que queda para TRL 6 no es una función nueva sino su versión en producción: notificaciones push con la app
cerrada (Firebase o Amazon SNS), SMS a guardianes sin cuenta, envío real de correos (Amazon SES), evidencia en
video y audio, despliegue en AWS y la validación con usuarios reales de las Comunas 3 y 4.
