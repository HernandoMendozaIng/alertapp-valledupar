-- AlertApp Valledupar - Sprint 2: seguridad personal
-- HU 02 alertas geolocalizadas, HU 06 rutas frecuentes y seguimiento, HU 10 2FA, HU 12 red de guardianes

ALTER TABLE usuario
    ADD COLUMN notificaciones     BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN radio_alertas      INTEGER NOT NULL DEFAULT 500,
    ADD COLUMN ultima_latitud     DOUBLE PRECISION,
    ADD COLUMN ultima_longitud    DOUBLE PRECISION,
    ADD COLUMN ubicacion_en       TIMESTAMPTZ,
    ADD COLUMN totp_secreto       VARCHAR(64),
    ADD COLUMN dos_factores       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT ck_usuario_radio CHECK (radio_alertas BETWEEN 200 AND 2000);

-- HU 12: cada usuario designa hasta 3 guardianes (el límite se valida en el servicio)
CREATE TABLE guardian (
    id           BIGSERIAL PRIMARY KEY,
    protegido_id BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    guardian_id  BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    creado_en    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_guardian UNIQUE (protegido_id, guardian_id),
    CONSTRAINT ck_guardian_distinto CHECK (protegido_id <> guardian_id)
);

CREATE INDEX ix_guardian_guardian ON guardian (guardian_id);

-- HU 02 y HU 12: bandeja de notificaciones que también se empuja en tiempo real
CREATE TABLE notificacion (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    tipo        VARCHAR(30)  NOT NULL,
    titulo      VARCHAR(120) NOT NULL,
    cuerpo      VARCHAR(300) NOT NULL,
    enlace      VARCHAR(120),
    latitud     DOUBLE PRECISION,
    longitud    DOUBLE PRECISION,
    leida       BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_notificacion_tipo CHECK (tipo IN ('REPORTE_CERCANO', 'PANICO_PROTEGIDO', 'SEGUIMIENTO_PROTEGIDO',
                                                    'GUARDIAN_AGREGADO', 'ESTADO_REPORTE'))
);

CREATE INDEX ix_notificacion_usuario ON notificacion (usuario_id, creado_en DESC);

-- HU 06: rutas frecuentes del usuario
CREATE TABLE ruta_frecuente (
    id               BIGSERIAL PRIMARY KEY,
    usuario_id       BIGINT       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    nombre           VARCHAR(60)  NOT NULL,
    origen_latitud   DOUBLE PRECISION NOT NULL,
    origen_longitud  DOUBLE PRECISION NOT NULL,
    destino_latitud  DOUBLE PRECISION NOT NULL,
    destino_longitud DOUBLE PRECISION NOT NULL,
    modo             VARCHAR(15)  NOT NULL,
    creado_en        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_ruta_modo CHECK (modo IN ('A_PIE', 'BICICLETA', 'VEHICULO'))
);

CREATE INDEX ix_ruta_usuario ON ruta_frecuente (usuario_id);

-- HU 06 y HU 12: ubicación compartida temporal (máximo 1 hora) que ven los guardianes
CREATE TABLE seguimiento (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    motivo          VARCHAR(15)  NOT NULL,
    ruta_id         BIGINT       REFERENCES ruta_frecuente (id) ON DELETE SET NULL,
    latitud         DOUBLE PRECISION NOT NULL,
    longitud        DOUBLE PRECISION NOT NULL,
    iniciado_en     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expira_en       TIMESTAMPTZ  NOT NULL,
    detenido_en     TIMESTAMPTZ,
    CONSTRAINT ck_seguimiento_motivo CHECK (motivo IN ('TRAYECTO', 'PANICO')),
    CONSTRAINT ck_seguimiento_duracion CHECK (expira_en <= iniciado_en + INTERVAL '1 hour')
);

CREATE INDEX ix_seguimiento_usuario ON seguimiento (usuario_id, iniciado_en DESC);
