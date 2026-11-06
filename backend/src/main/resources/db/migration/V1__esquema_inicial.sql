-- AlertApp Valledupar - esquema inicial (Sprint 1)
-- Modelo relacional: usuario, reporte, voto, comentario, alerta_panico

CREATE TABLE usuario (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(80)  NOT NULL,
    correo          VARCHAR(120) NOT NULL UNIQUE,
    clave_hash      VARCHAR(100) NOT NULL,
    telefono        VARCHAR(20),
    rol             VARCHAR(20)  NOT NULL DEFAULT 'CIUDADANO',
    comuna          SMALLINT,
    acepto_politica BOOLEAN      NOT NULL,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('CIUDADANO', 'AUTORIDAD')),
    CONSTRAINT ck_usuario_politica CHECK (acepto_politica = TRUE)
);

CREATE TABLE reporte (
    id                 BIGSERIAL PRIMARY KEY,
    autor_id           BIGINT       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    tipo_delito        VARCHAR(30)  NOT NULL,
    arma               VARCHAR(30)  NOT NULL,
    medio              VARCHAR(30)  NOT NULL,
    elemento_hurtado   VARCHAR(30)  NOT NULL,
    descripcion        VARCHAR(500),
    fecha_hecho        TIMESTAMPTZ  NOT NULL,
    latitud            DOUBLE PRECISION NOT NULL,
    longitud           DOUBLE PRECISION NOT NULL,
    comuna             SMALLINT     NOT NULL,
    barrio             VARCHAR(60),
    anonimo            BOOLEAN      NOT NULL DEFAULT TRUE,
    estado             VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    severidad          VARCHAR(10)  NOT NULL,
    puntaje_severidad  SMALLINT     NOT NULL,
    confirmaciones     INTEGER      NOT NULL DEFAULT 0,
    falsos             INTEGER      NOT NULL DEFAULT 0,
    evidencia_archivo  VARCHAR(120),
    creado_en          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_reporte_estado CHECK (estado IN ('PENDIENTE', 'VALIDADO', 'EN_REVISION', 'CERRADO', 'DESCARTADO')),
    CONSTRAINT ck_reporte_severidad CHECK (severidad IN ('BAJA', 'MEDIA', 'ALTA')),
    CONSTRAINT ck_reporte_lat CHECK (latitud BETWEEN -90 AND 90),
    CONSTRAINT ck_reporte_lng CHECK (longitud BETWEEN -180 AND 180)
);

CREATE INDEX ix_reporte_estado      ON reporte (estado);
CREATE INDEX ix_reporte_fecha_hecho ON reporte (fecha_hecho);
CREATE INDEX ix_reporte_comuna      ON reporte (comuna);
CREATE INDEX ix_reporte_autor       ON reporte (autor_id);

CREATE TABLE voto (
    id          BIGSERIAL PRIMARY KEY,
    reporte_id  BIGINT      NOT NULL REFERENCES reporte (id) ON DELETE CASCADE,
    usuario_id  BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    tipo        VARCHAR(10) NOT NULL,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_voto_reporte_usuario UNIQUE (reporte_id, usuario_id),
    CONSTRAINT ck_voto_tipo CHECK (tipo IN ('CONFIRMA', 'FALSO'))
);

CREATE TABLE comentario (
    id            BIGSERIAL PRIMARY KEY,
    reporte_id    BIGINT       NOT NULL REFERENCES reporte (id) ON DELETE CASCADE,
    usuario_id    BIGINT       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    texto         VARCHAR(400) NOT NULL,
    es_autoridad  BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_comentario_reporte ON comentario (reporte_id);

CREATE TABLE alerta_panico (
    id           BIGSERIAL PRIMARY KEY,
    usuario_id   BIGINT       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    latitud      DOUBLE PRECISION NOT NULL,
    longitud     DOUBLE PRECISION NOT NULL,
    estado       VARCHAR(15)  NOT NULL DEFAULT 'ACTIVA',
    nota         VARCHAR(200),
    creado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    cerrada_en   TIMESTAMPTZ,
    CONSTRAINT ck_alerta_estado CHECK (estado IN ('ACTIVA', 'ATENDIDA', 'CANCELADA'))
);

CREATE INDEX ix_alerta_estado ON alerta_panico (estado);
