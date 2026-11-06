# AlertApp Valledupar

Prototipo funcional (TRL 5) de una plataforma móvil y web basada en geolocalización e inteligencia artificial
para fortalecer la prevención situacional del delito en las Comunas 3 y 4 de Valledupar.

Proyecto de grado, Ingeniería de Sistemas, Universidad Nacional Abierta y a Distancia (UNAD), curso 202016907.
Autores: Armel Peña Rangel y Hernando Mendoza. Tutor: Orlando Gómez Barboza.

## Qué hace

| Historia de usuario | Función en el prototipo |
|---|---|
| HU 01 Acceso y autenticación | Registro con aceptación de la Ley 1581, ingreso con JWT, edición de perfil |
| HU 03 Zonas de riesgo (hotspots) | Mapa con los límites de las Comunas 3 y 4, puntos calientes por color y filtro a 200 m |
| HU 05 Reporte guiado | Formulario de tipo, arma, medio, elemento, fecha, ubicación, foto opcional y anonimato por defecto |
| HU 07 Validación comunitaria | Votos de confirmación o falso, validación y anulación automática con 3 votos de diferencia, comentarios |
| HU 08 Métricas y filtros | Reportes por hora, día, tipo y comuna con filtros sin recargar |
| HU 09 Reportes para autoridades | Exportación CSV para Excel e informe semanal imprimible en PDF |
| HU 11 Botón de pánico | Cuenta de 5 segundos para cancelar, envío de la ubicación y atención desde el panel |
| HU 13 Trazabilidad | Estados Pendiente, Validado, En revisión, Cerrado y Descartado con notas de la autoridad |

La IA está en un microservicio de Python que agrupa los reportes con DBSCAN (scikit-learn, distancia haversine)
y calcula un índice de riesgo por ubicación y hora. Si ese servicio no responde, la API usa un respaldo en Java
con la misma lógica.

Fuera del alcance de TRL 5, documentadas como trabajo futuro: rutas seguras (HU 04), seguimiento en tiempo real
(HU 06), 2FA (HU 10), red de guardianes (HU 12), notificaciones push (HU 02) y despliegue en AWS.

## Arquitectura

```
React + Leaflet (web móvil)  --->  API REST Spring Boot (Java 21)  --->  PostgreSQL 17
        :5173                          :8080   |                         (Flyway)
                                               +--->  Microservicio IA (Python, FastAPI, scikit-learn) :8000
```

Diagramas de componentes, casos de uso, secuencia y el modelo entidad-relación en [docs/arquitectura.md](docs/arquitectura.md).

## Cómo ejecutarlo

Requisitos: Java 21, Maven 3.9+, Python 3.11+, Node 20+ y PostgreSQL 15+.

```bash
# 1. Base de datos
createuser alertapp --pwprompt        # contraseña: alertapp
createdb alertapp -O alertapp

# 2. Los tres servicios
./scripts/iniciar.sh
```

Abre http://localhost:5173. La primera vez se cargan datos de prueba sintéticos (154 reportes dentro de las
Comunas 3 y 4; no son denuncias reales) y estas cuentas:

| Rol | Correo | Contraseña |
|---|---|---|
| Ciudadano | ciudadano@alertapp.co | Valledupar2026 |
| Autoridad | autoridad@alertapp.co | Valledupar2026 |

La documentación interactiva de la API queda en http://localhost:8080/docs.

## Pruebas

```bash
cd backend && mvn test                         # 19 pruebas: unitarias y flujo funcional de extremo a extremo
cd ia-service && .venv/bin/python -m pytest    # 5 pruebas del modelo de IA
```

El protocolo de pruebas funcionales y sus resultados están en [docs/pruebas-funcionales.md](docs/pruebas-funcionales.md).

## Metodología

Scrum con sprints cortos. Backlog, planificación de sprints, bitácora de daily scrum e incrementos en
[docs/scrum/](docs/scrum/).

## Estructura

```
backend/       API REST (Spring Boot, JPA, Spring Security, Flyway)
ia-service/    Microservicio de analítica e IA (FastAPI, scikit-learn)
frontend/      Aplicación web móvil y panel de autoridad (React, TypeScript, Leaflet)
docs/          Arquitectura, pruebas funcionales y artefactos Scrum
scripts/       Arranque local
```

## Datos geográficos

Los límites de las comunas provienen de OpenStreetMap (© colaboradores de OpenStreetMap, licencia ODbL).
OpenStreetMap no tiene la Comuna 4 como relación propia; se obtuvo restando las Comunas 1, 2, 3, 5 y 6 del
perímetro urbano de Valledupar.
