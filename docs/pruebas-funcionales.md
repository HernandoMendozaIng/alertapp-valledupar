# Pruebas funcionales del prototipo (TRL 5)

## Criterio

El tutor definió para esta fase que el nivel TRL 5 se verifica con **pruebas funcionales**: cada función debe
cumplir su objetivo de extremo a extremo (la acción en la interfaz se guarda en la base de datos y la aplicación
lo confirma) y el prototipo debe cumplir al menos el **80 %** de las funciones previstas.

Entorno: macOS, Java 21, PostgreSQL 17, Python 3.13, Node 26, navegador Chromium (escritorio 1440×900 y
celular 390×844). Datos: 154 reportes sintéticos de prueba dentro de las Comunas 3 y 4.

## 1. Pruebas automatizadas de la API

Se ejecutan con `mvn test` (JUnit 5 + MockMvc sobre una base H2 en modo PostgreSQL). Cada prueba hace las
peticiones HTTP reales a la API y luego consulta el dato para comprobar que quedó guardado.

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
| PU-01 a PU-04 | HU 05 | Puntajes del clasificador de severidad (alta, media, baja, tope de 100) | Aprobadas |
| PU-05 a PU-06 | HU 03 | Distancia haversine y agrupamiento DBSCAN con ruido | Aprobadas |

Resultado: **19 de 19 pruebas aprobadas** (6 de noviembre de 2026).

## 2. Pruebas del modelo de IA

Se ejecutan con `pytest` en `ia-service/`.

| ID | Qué se verifica | Resultado |
|---|---|---|
| IA-01 | El servicio responde | Aprobada |
| IA-02 | DBSCAN agrupa 10 reportes cercanos en una zona, separa 2 aislados como ruido y calcula la franja pico | Aprobada |
| IA-03 | Con menos puntos que el mínimo no se forma zona | Aprobada |
| IA-04 | Riesgo ALTO junto a 8 reportes recientes en la misma hora | Aprobada |
| IA-05 | Riesgo BAJO (0) sin reportes a 500 m | Aprobada |

Resultado: **5 de 5 pruebas aprobadas**.

## 3. Pruebas de extremo a extremo desde la interfaz

Realizadas en el navegador con la aplicación completa en marcha (web, API, IA y PostgreSQL), verificando el
registro final directamente en PostgreSQL con `psql`.

| ID | Historia | Pasos | Resultado esperado | Resultado obtenido |
|---|---|---|---|---|
| UI-01 | HU 01 | Ingresar con la cuenta de ciudadano | Entra al mapa con saludo y nivel de riesgo | Cumple |
| UI-02 | HU 03 | Ver el mapa con filtros de 7, 30 y 90 días, puntos calientes y radio de 200 m | Zonas rojo/naranja/verde y límites de las comunas | Cumple |
| UI-03 | HU 05 | Reportar un atraco con cuchillo, en moto, celular, y enviar | Detalle del reporte con severidad alta; fila `reporte` id 155, `ALTA`, 78 puntos, `PENDIENTE` en la base | Cumple |
| UI-04 | HU 07 | Con otra cuenta (vecino1), abrir el reporte 155 y tocar "Confirmo" | Conteo en 1; fila `voto` CONFIRMA de vecino1 en la base | Cumple |
| UI-05 | HU 08 | En Métricas, cambiar de "Comunas 3 y 4" a "Comuna 3" | Total pasa de 142 a 87 y las gráficas se actualizan sin recargar | Cumple |
| UI-06 | HU 11 | Tocar SOS, esperar los 5 segundos | Hoja de cuenta regresiva y "Alerta enviada"; fila `alerta_panico` en estado `ACTIVA` | Cumple |
| UI-07 | HU 11 | Como autoridad, marcar la alerta como atendida | Fila `alerta_panico` en `ATENDIDA` con la nota "Patrulla enviada al sitio" | Cumple |
| UI-08 | HU 13 | Como autoridad, cambiar el reporte 9 de "Validado" a "En revisión" desde la tabla del panel | Fila `reporte` 9 en `EN_REVISION` (la nota al ciudadano se agrega desde el detalle del reporte) | Cumple |
| UI-09 | HU 09 | Exportar la semana en CSV | Archivo `alertapp-reportes-AAAA-MM-DD.csv` separado por punto y coma, abre en Excel | Cumple |
| UI-10 | HU 09 | Como autoridad, abrir el informe semanal | Informe con indicadores, recomendación de patrullaje por zona y franja horaria, gráficas y botón "Guardar como PDF" (impresión del navegador) | Cumple |

## 4. Cumplimiento de funciones

| Función prevista para el prototipo | Estado |
|---|---|
| Registro, ingreso y perfil (HU 01) | Cumple |
| Mapa de riesgo con hotspots y filtro de 200 m (HU 03) | Cumple |
| Reporte guiado con foto y anonimato (HU 05) | Cumple |
| Edición y eliminación del reporte propio | Cumple |
| Validación comunitaria con anulación automática (HU 07) | Cumple |
| Comentarios y trazabilidad de estados (HU 13) | Cumple |
| Métricas con filtros (HU 08) | Cumple |
| Exportación e informe para autoridades (HU 09) | Cumple |
| Botón de pánico con cancelación de 5 s (HU 11) | Cumple |
| Panel de autoridad con atención de alertas | Cumple |
| Analítica con IA (DBSCAN e índice de riesgo) | Cumple |

**11 de 11 funciones previstas cumplen (100 %)**, por encima del 80 % exigido.

Quedan fuera del alcance de TRL 5, como trabajo futuro: rutas seguras (HU 04), seguimiento en tiempo real y rutas
frecuentes (HU 06), 2FA (HU 10), red de guardianes (HU 12), notificaciones push (HU 02) y el canal de chat
directo con el cuadrante (parte de HU 13; en el prototipo la autoridad responde con comentarios en el reporte).
