# Product backlog priorizado

Roles Scrum del equipo:

| Rol | Responsable |
|---|---|
| Product Owner | Armel Peña Rangel (formulador del proyecto) |
| Scrum Master | Hernando Mendoza |
| Equipo de desarrollo | Armel Peña Rangel y Hernando Mendoza |

Prioridad con MoSCoW frente al alcance TRL 5: **Debe** (necesaria para la prueba funcional), **Debería**
(aporta a la validación), **Podría** (queda para TRL 6 o más).

El 9 de noviembre, con el núcleo terminado y probado, se decidió incluir también las cinco historias que
estaban como trabajo futuro (HU 02, 04, 06, 10 y 12), en una versión que cabe en un prototipo local. La columna
"Cómo se resolvió en el prototipo" aclara qué parte queda para la arquitectura objetivo.

| Prioridad | ID | Historia de usuario | Puntos | Sprint | Estado | Cómo se resolvió en el prototipo |
|---|---|---|---|---|---|---|
| Debe | HU 0.3 | Arquitectura de componentes, modelo de datos y entorno local | 8 | 1 | Hecho | Spring Boot, PostgreSQL con Flyway, microservicio de IA en Python |
| Debe | HU 01 | Registro e ingreso seguro con aceptación de la política de datos | 8 | 1 | Hecho | JWT, BCrypt, límite de intentos; recuperación de clave con código de 6 dígitos |
| Debe | HU 05 | Reporte guiado (tipo, arma, medio, elemento, lugar, foto, anonimato) | 8 | 1 | Hecho | Foto verificada por su firma; video y audio quedan para TRL 6 |
| Debe | HU 03 | Mapa con puntos calientes por color y filtro a 200 m | 8 | 1 | Hecho | DBSCAN en el microservicio de IA con respaldo en Java |
| Debe | HU 07 | Validación comunitaria con anulación automática y comentarios | 8 | 1 | Hecho | Diferencia de 3 votos valida o anula |
| Debe | HU 11 | Botón de pánico con ventana de cancelación de 5 s | 8 | 1 | Hecho | Llega al panel y a los guardianes con la ubicación |
| Debería | HU 08 | Métricas por hora, día, tipo y comuna con filtros | 8 | 1 | Hecho | Filtros sin recargar la página |
| Debería | HU 09 | Exportación CSV e informe semanal para autoridades | 8 | 1 | Hecho | CSV para Excel e informe imprimible en PDF |
| Debería | HU 13 | Estados del caso y notas de la autoridad | 5 | 1 | Hecho | El autor recibe un aviso con cada cambio; sin chat directo |
| Debería | HU 02 | Alertas geolocalizadas en tiempo real | 8 | 1 | Hecho | Server-Sent Events en la web (latencia medida < 1 s); push nativo con la app cerrada en TRL 6 |
| Debería | HU 04 | Rutas seguras que eviten zonas de riesgo | 8 | 1 | Hecho | A* sobre la red vial de OpenStreetMap con riesgo por tramo; ArcGIS en la arquitectura objetivo |
| Debería | HU 06 | Rutas frecuentes y seguimiento por una hora | 8 | 1 | Hecho | Guardar, editar, borrar; ubicación compartida de 5 a 60 minutos |
| Debería | HU 10 | Configuración de seguridad y 2FA | 8 | 1 | Hecho | TOTP (RFC 6238) con app autenticadora; Cognito en la arquitectura objetivo |
| Debería | HU 12 | Red de hasta 3 guardianes | 8 | 1 | Hecho | Aviso instantáneo y mapa en vivo; SMS a guardianes sin cuenta en TRL 6 |
| Debe | — | Pruebas funcionales TRL 5 y evidencias | 5 | 1 y 2 | En curso | 44 pruebas de API, 5 de IA y 22 de interfaz automatizadas |
| Debe | — | Ajustes del documento maestro y capítulos por objetivo | 8 | 2 | Por hacer | |
| Debe | — | Video de máximo 10 minutos con los dos integrantes en cámara | 3 | 3 | Por hacer | |

Las historias HU 0.1 (configurar Azure DevOps) y HU 0.2 (prototipo de pantallas en Justinmind) se cumplieron en
la fase de diseño: el tablero del prototipo se lleva en GitHub y las pantallas de Justinmind fueron la base de la
interfaz construida.
