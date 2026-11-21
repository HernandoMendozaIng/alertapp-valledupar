# Arquitectura y diseño

## 1. Arquitectura objetivo y arquitectura del prototipo

El documento maestro define una arquitectura objetivo en AWS (EKS, Aurora PostgreSQL, Cognito, S3).
Para validar el nivel TRL 5 se construyó el mismo diseño de componentes en un entorno local, con los
mismos lenguajes (Java para la API, Python para la IA, React para la interfaz y PostgreSQL para los datos).
Cada componente local tiene su reemplazo directo en la nube:

| Componente del prototipo | Equivalente en la arquitectura objetivo |
|---|---|
| API REST Spring Boot en un proceso | Microservicios Java en contenedores sobre Amazon EKS |
| PostgreSQL 17 local con Flyway | Amazon Aurora PostgreSQL |
| JWT firmado por la API (HMAC) | AWS Cognito |
| Carpeta local de evidencias | Amazon S3 |
| Microservicio FastAPI local | Servicio de IA en contenedor (o SageMaker) |
| Mapa OpenStreetMap + Leaflet | APIs de ArcGIS (ESRI) |
| Rutas seguras con A* sobre la red vial de OpenStreetMap | Servicio de rutas de ArcGIS con capa de riesgo |
| Canal en tiempo real con Server-Sent Events | Notificaciones push con Firebase Cloud Messaging o Amazon SNS |
| TOTP (RFC 6238) implementado en la API | Autenticación multifactor de AWS Cognito |
| Mensajes de recuperación escritos en el registro del servidor | Amazon SES (correo) y Amazon SNS (SMS) |
| Web adaptable al celular | App Android híbrida + panel React |

## 2. Diagrama de componentes

```mermaid
flowchart LR
    subgraph Cliente
        W["App web móvil<br/>React + Leaflet"]
        P["Panel de autoridad<br/>React"]
    end
    subgraph Servidor["API REST · Spring Boot 3 / Java 21"]
        SEG["Seguridad<br/>JWT, BCrypt, TOTP"]
        REP["Reportes y comunidad"]
        RUT["Rutas seguras<br/>red vial + A*"]
        RT["Canal en tiempo real<br/>Server-Sent Events"]
        RED["Guardianes y seguimiento"]
    end
    IA["Microservicio IA<br/>FastAPI + scikit-learn"]
    BD[("PostgreSQL 17")]
    EV[/"Evidencias<br/>(fotos)"/]
    OSM["OpenStreetMap<br/>teselas y red vial"]

    W -- "HTTPS / JSON" --> Servidor
    P -- "HTTPS / JSON" --> Servidor
    RT -. "eventos" .-> W
    RT -. "eventos" .-> P
    Servidor -- "JPA / Flyway" --> BD
    REP -- "archivos" --> EV
    REP -- "POST /hotspots, /riesgo" --> IA
    REP -- "reporte creado" --> RT
    REP -- "reporte creado" --> RUT
    RED --> RT
    W -- "teselas" --> OSM
    OSM -. "red vial (script)" .-> RUT
```

La red vial se descarga una sola vez con `scripts/datos/construir_red_vial.py` y queda como archivo dentro de la
API (`backend/src/main/resources/geo/red-vial.json`), así que las rutas no dependen de ningún servicio externo.

## 3. Casos de uso

```mermaid
flowchart LR
    C(("Ciudadano"))
    G(("Guardián"))
    A(("Autoridad"))
    IA(("Servicio IA"))

    subgraph AlertApp
        UC1["Registrarse, ingresar y recuperar la clave"]
        UC2["Reportar hecho con foto"]
        UC3["Consultar mapa de riesgo"]
        UC4["Validar reporte (votar)"]
        UC5["Comentar reporte"]
        UC6["Editar o eliminar reporte propio"]
        UC7["Activar botón de pánico"]
        UC8["Consultar métricas"]
        UC9["Cambiar estado de un reporte"]
        UC10["Atender alerta de pánico"]
        UC11["Exportar CSV e informe semanal"]
        UC12["Calcular puntos calientes y riesgo"]
        UC13["Calcular ruta segura"]
        UC14["Guardar rutas frecuentes"]
        UC15["Compartir trayecto (máx. 1 hora)"]
        UC16["Administrar guardianes (máx. 3)"]
        UC17["Recibir alertas cercanas"]
        UC18["Activar verificación en dos pasos"]
        UC19["Ver ubicación del protegido"]
    end

    C --- UC1 & UC2 & UC3 & UC4 & UC5 & UC6 & UC7 & UC8 & UC13 & UC14 & UC15 & UC16 & UC17 & UC18
    G --- UC19 & UC17
    A --- UC1 & UC3 & UC5 & UC8 & UC9 & UC10 & UC11
    UC3 -. incluye .-> UC12
    UC8 -. incluye .-> UC12
    UC7 -. notifica .-> UC19
    UC15 -. notifica .-> UC19
    UC12 --- IA
```

El guardián es un ciudadano registrado que otro usuario eligió como persona de confianza: la misma cuenta puede
ser protegido y guardián a la vez.

## 4. Diagramas de secuencia

### 4.1 Reporte guiado (HU 05)

```mermaid
sequenceDiagram
    actor C as Ciudadano
    participant W as App web
    participant API as API REST
    participant CL as ClasificadorSeveridad
    participant BD as PostgreSQL

    C->>W: Completa tipo, arma, medio, elemento, lugar y foto
    W->>W: Detecta la comuna (punto en polígono)
    W->>API: POST /api/reportes (multipart, JWT)
    API->>API: Valida campos y firma de la imagen
    API->>CL: clasificar(tipo, arma, medio, elemento)
    CL-->>API: puntaje 0-100 y severidad
    API->>BD: INSERT reporte (estado PENDIENTE)
    BD-->>API: id
    API-->>W: 201 Created + reporte
    W-->>C: Muestra el detalle y el mensaje de confirmación
```

### 4.2 Validación comunitaria (HU 07)

```mermaid
sequenceDiagram
    actor V as Vecino
    participant W as App web
    participant API as API REST
    participant BD as PostgreSQL

    V->>W: Toca "Confirmo"
    W->>API: PUT /api/reportes/{id}/voto {tipo: CONFIRMA}
    API->>API: Rechaza si es el autor o si el reporte está cerrado
    API->>BD: INSERT o UPDATE voto (único por usuario)
    API->>BD: Recuenta confirmaciones y falsos
    alt confirmaciones - falsos >= 3
        API->>BD: estado = VALIDADO
    else falsos - confirmaciones >= 3
        API->>BD: estado = DESCARTADO
    end
    API-->>W: Reporte con conteos y estado
```

### 4.3 Mapa de riesgo con IA (HU 03)

```mermaid
sequenceDiagram
    actor C as Ciudadano
    participant W as App web
    participant API as API REST
    participant IA as Servicio IA
    participant BD as PostgreSQL

    C->>W: Abre el mapa
    W->>API: GET /api/analitica/hotspots?dias=30
    API->>BD: Reportes no descartados del periodo
    API->>IA: POST /hotspots (puntos, eps 200 m, mín. 4)
    alt IA disponible
        IA-->>API: Zonas DBSCAN con nivel y franja pico
    else IA no responde (2,5 s)
        API->>API: DBSCAN de respaldo en Java
    end
    API-->>W: Zonas calientes
    W-->>C: Círculos rojo, naranja o verde sobre el mapa
```

### 4.4 Botón de pánico con guardianes (HU 11 y HU 12)

```mermaid
sequenceDiagram
    actor C as Ciudadano
    participant W as App web
    participant API as API REST
    participant RT as Canal SSE
    actor G as Guardián
    actor A as Autoridad

    C->>W: Toca SOS
    W->>W: Cuenta regresiva de 5 s (puede cancelar)
    W->>API: POST /api/alertas {lat, lng}
    API->>API: Guarda la alerta ACTIVA y abre un seguimiento de 1 hora
    API->>RT: Notificación PANICO_PROTEGIDO a cada guardián
    RT-->>G: Aviso al instante con la ubicación
    API-->>W: Alerta ACTIVA
    loop cada 10 s mientras dure
        W->>API: PUT /api/seguimiento/posicion
        API->>RT: evento "ubicacion"
        RT-->>G: El punto se mueve en su mapa
    end
    A->>API: POST /api/panel/alertas/{id}/atender
    API->>RT: evento "protegido" (cambió el estado)
    RT-->>G: Deja de ver la ubicación
```

### 4.5 Alerta geolocalizada (HU 02)

```mermaid
sequenceDiagram
    actor V as Vecino
    participant W2 as App del vecino
    actor C as Ciudadano que reporta
    participant API as API REST
    participant BD as PostgreSQL
    participant RT as Canal SSE

    W2->>API: GET /api/notificaciones/flujo (conexión abierta)
    W2->>API: PUT /api/auth/yo/ubicacion (al abrir el mapa)
    C->>API: POST /api/reportes
    API->>BD: INSERT reporte
    Note over API: Después del commit se publica el evento ReporteCreado
    API->>BD: Vecinos con alertas activas en la caja de 2 km
    API->>API: Filtra por el radio de cada uno (500 m por defecto)
    API->>BD: INSERT notificacion
    API->>RT: evento "notificacion"
    RT-->>W2: "Atraco a 150 m de ti" en menos de 5 s
```

### 4.6 Ruta segura (HU 04)

```mermaid
sequenceDiagram
    actor C as Ciudadano
    participant W as App web
    participant API as PlanificadorRutas
    participant MR as MapaRiesgo
    participant RV as RedVial (memoria)
    participant BD as PostgreSQL

    C->>W: Marca origen y destino y elige el modo
    W->>API: GET /api/rutas/segura?origen&destino&modo
    API->>RV: Cruce más cercano con salida (origen) y entrada (destino)
    API->>MR: Riesgo por tramo para la hora actual
    alt caché vencida o reporte nuevo
        MR->>BD: Reportes de los últimos 90 días
        MR->>RV: Reparte el riesgo en los tramos a menos de 150 m
    end
    API->>API: A* con costo = largo (ruta corta)
    API->>API: A* con costo = largo × (1 + peso × riesgo) (ruta segura)
    API-->>W: Las dos rutas, distancias, tiempos y explicación
    Note over W: Si llega un evento "reporte" a menos de 250 m de la ruta, la recalcula
```

### 4.7 Ingreso con verificación en dos pasos (HU 10)

```mermaid
sequenceDiagram
    actor C as Ciudadano
    participant W as App web
    participant API as API REST
    participant APP as App autenticadora

    C->>W: Correo y contraseña
    W->>API: POST /api/auth/ingreso
    API->>API: Contraseña correcta y 2FA activa
    API-->>W: requiereSegundoFactor + desafío JWT de 5 min
    C->>APP: Lee el código de 6 dígitos
    C->>W: Escribe el código
    W->>API: POST /api/auth/ingreso/2fa {desafío, código}
    API->>API: TOTP válido (±30 s) y desafío de tipo "2fa"
    API-->>W: Token de sesión de 12 horas
```

## 5. Modelo entidad-relación

```mermaid
erDiagram
    USUARIO ||--o{ REPORTE : crea
    USUARIO ||--o{ VOTO : emite
    USUARIO ||--o{ COMENTARIO : escribe
    USUARIO ||--o{ ALERTA_PANICO : activa
    REPORTE ||--o{ VOTO : recibe
    REPORTE ||--o{ COMENTARIO : tiene
    USUARIO ||--o{ GUARDIAN : "protege a (máx. 3)"
    USUARIO ||--o{ GUARDIAN : "es guardián en"
    USUARIO ||--o{ NOTIFICACION : recibe
    USUARIO ||--o{ RUTA_FRECUENTE : guarda
    USUARIO ||--o{ SEGUIMIENTO : comparte
    RUTA_FRECUENTE |o--o{ SEGUIMIENTO : "se usa en"

    USUARIO {
        bigint id PK
        varchar nombre
        varchar correo UK
        varchar clave_hash
        varchar telefono
        varchar rol "CIUDADANO | AUTORIDAD"
        smallint comuna
        boolean acepto_politica
        timestamptz creado_en
        boolean notificaciones
        int radio_alertas "200 a 2000 m"
        float ultima_latitud
        float ultima_longitud
        timestamptz ubicacion_en
        varchar totp_secreto
        boolean dos_factores
    }
    REPORTE {
        bigint id PK
        bigint autor_id FK
        varchar tipo_delito
        varchar arma
        varchar medio
        varchar elemento_hurtado
        varchar descripcion
        timestamptz fecha_hecho
        float latitud
        float longitud
        smallint comuna
        varchar barrio
        boolean anonimo
        varchar estado
        varchar severidad
        smallint puntaje_severidad
        int confirmaciones
        int falsos
        varchar evidencia_archivo
        timestamptz creado_en
        timestamptz actualizado_en
    }
    VOTO {
        bigint id PK
        bigint reporte_id FK
        bigint usuario_id FK
        varchar tipo "CONFIRMA | FALSO"
        timestamptz creado_en
    }
    COMENTARIO {
        bigint id PK
        bigint reporte_id FK
        bigint usuario_id FK
        varchar texto
        boolean es_autoridad
        timestamptz creado_en
    }
    ALERTA_PANICO {
        bigint id PK
        bigint usuario_id FK
        float latitud
        float longitud
        varchar estado "ACTIVA | ATENDIDA | CANCELADA"
        varchar nota
        timestamptz creado_en
        timestamptz cerrada_en
    }
    GUARDIAN {
        bigint id PK
        bigint protegido_id FK
        bigint guardian_id FK
        timestamptz creado_en
    }
    NOTIFICACION {
        bigint id PK
        bigint usuario_id FK
        varchar tipo
        varchar titulo
        varchar cuerpo
        varchar enlace
        float latitud
        float longitud
        boolean leida
        timestamptz creado_en
    }
    RUTA_FRECUENTE {
        bigint id PK
        bigint usuario_id FK
        varchar nombre
        float origen_latitud
        float origen_longitud
        float destino_latitud
        float destino_longitud
        varchar modo "A_PIE | BICICLETA | VEHICULO"
        timestamptz creado_en
    }
    SEGUIMIENTO {
        bigint id PK
        bigint usuario_id FK
        varchar motivo "TRAYECTO | PANICO"
        bigint ruta_id FK
        float latitud
        float longitud
        timestamptz iniciado_en
        timestamptz actualizado_en
        timestamptz expira_en
        timestamptz detenido_en
    }
```

Restricciones principales (ver `V1__esquema_inicial.sql` y `V2__seguridad_personal.sql` en
`backend/src/main/resources/db/migration/`): voto único por usuario y reporte, dominios de estado, severidad,
rol, tipo de notificación y modo con `CHECK`, aceptación obligatoria de la política de datos, un guardián no
puede ser la misma persona (`ck_guardian_distinto`) ni repetirse (`uq_guardian`), el seguimiento nunca dura más
de una hora (`ck_seguimiento_duracion`) y el radio de alertas va de 200 m a 2 km. El límite de 3 guardianes se
valida en el servicio porque depende de cuántas filas ya existen.

## 6. Modelo de severidad y de riesgo

**Severidad del reporte (0 a 100).** Suma de pesos: arma (fuego 50, cortopunzante 35, no identificada 15,
sin arma 5), tipo (atraco 25, hurto 10, otro 5, sospechoso 0), medio (moto o carro 10, bicicleta o no
identificado 5) y elemento (moto o carro 15, celular, joyas o dinero 8, bicicleta o partes 6, otros 3).
Alta desde 60, media desde 30.

**Puntos calientes.** DBSCAN con distancia haversine, radio de 200 m y mínimo 4 reportes. Cada zona recibe un
puntaje de hasta 50 por volumen, 30 por severidad media y 20 por actividad del último mes; alto desde 60,
medio desde 35. La franja pico es el bloque de 4 horas con más reportes.

**Índice de riesgo de una ubicación.** Suma ponderada de los reportes a menos de 500 m: cercanía
`e^(-d/250)`, vida media de 30 días, similitud de hora gaussiana con desviación de 2 h y severidad. El índice es
`100·(1 − e^(−suma/1,5))`; alto desde 60, medio desde 30.

**Riesgo de cada tramo de calle (rutas seguras).** Cada reporte no descartado de los últimos 90 días aporta a los
puntos de la red vial que tiene a menos de 150 m un peso de `severidad × validación × recencia × franja × (1 − d/150)`,
donde la severidad vale 1 (alta), 0,65 (media) o 0,35 (baja); la validación 1,25 si la comunidad o la autoridad ya
lo confirmaron; la recencia tiene una vida media de 45 días, y la franja vale 1,4 si el hecho ocurrió a dos horas
o menos de la hora actual. El riesgo del tramo es el de su punto más peligroso, con un tope de 4.

**Costo de la ruta segura.** `largo × (1 + peso del modo × riesgo del tramo)`. El peso es 2,2 a pie, 1,6 en
bicicleta y 0,9 en vehículo, porque quien camina está más expuesto. A pie se ignora el sentido de las calles; en
bicicleta y en vehículo se respeta, y el carro no entra a senderos ni pasajes peatonales. La ruta más corta usa el
mismo algoritmo con peso 0, para poder comparar las dos.

**Alertas por cercanía.** Se avisa a quien activó las alertas, tiene una ubicación de las últimas 12 horas y está
dentro de su radio. La distancia se muestra redondeada (10 m, 50 m o 0,1 km) para no revelar el punto exacto
del reporte.
