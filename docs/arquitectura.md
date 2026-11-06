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
| Web adaptable al celular | App Android híbrida + panel React |

## 2. Diagrama de componentes

```mermaid
flowchart LR
    subgraph Cliente
        W["App web móvil<br/>React + Leaflet"]
        P["Panel de autoridad<br/>React"]
    end
    subgraph Servidor
        API["API REST<br/>Spring Boot 3 / Java 21"]
        SEG["Seguridad<br/>Spring Security + JWT"]
        IA["Microservicio IA<br/>FastAPI + scikit-learn"]
    end
    BD[("PostgreSQL 17")]
    EV[/"Evidencias<br/>(fotos)"/]
    OSM["Teselas OpenStreetMap"]

    W -- "HTTPS / JSON" --> API
    P -- "HTTPS / JSON" --> API
    API --- SEG
    API -- "JPA / Flyway" --> BD
    API -- "archivos" --> EV
    API -- "POST /hotspots, /riesgo" --> IA
    W -- "teselas" --> OSM
```

## 3. Casos de uso

```mermaid
flowchart LR
    C(("Ciudadano"))
    A(("Autoridad"))
    IA(("Servicio IA"))

    subgraph AlertApp
        UC1["Registrarse e ingresar"]
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
    end

    C --- UC1 & UC2 & UC3 & UC4 & UC5 & UC6 & UC7 & UC8
    A --- UC1 & UC3 & UC5 & UC8 & UC9 & UC10 & UC11
    UC3 -. incluye .-> UC12
    UC8 -. incluye .-> UC12
    UC12 --- IA
```

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

### 4.4 Botón de pánico (HU 11)

```mermaid
sequenceDiagram
    actor C as Ciudadano
    participant W as App web
    participant API as API REST
    actor A as Autoridad

    C->>W: Toca SOS
    W->>W: Cuenta regresiva de 5 s (puede cancelar)
    W->>API: POST /api/alertas {lat, lng}
    API-->>W: Alerta ACTIVA
    loop cada 15 s
        A->>API: GET /api/panel/alertas
    end
    A->>API: POST /api/panel/alertas/{id}/atender
    API-->>A: Alerta ATENDIDA
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
```

Restricciones principales (ver `backend/src/main/resources/db/migration/V1__esquema_inicial.sql`):
voto único por usuario y reporte (`uq_voto_reporte_usuario`), dominios de estado, severidad y rol con `CHECK`,
aceptación obligatoria de la política de datos y borrado en cascada de votos y comentarios.

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
