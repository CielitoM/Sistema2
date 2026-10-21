CREATE TABLE IF NOT EXISTS universidad (
    id SERIAL PRIMARY KEY, nombre VARCHAR(160) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS carrera (
    id SERIAL PRIMARY KEY, nombre VARCHAR(160) NOT NULL UNIQUE,
    universidad_id INTEGER REFERENCES universidad(id)
);
CREATE TABLE IF NOT EXISTS estudiante_reportado (
    ci VARCHAR(30) PRIMARY KEY, nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL, carrera VARCHAR(160) NOT NULL,
    universidad VARCHAR(160), estado VARCHAR(30) NOT NULL DEFAULT 'activo',
    promedio NUMERIC(5,2), actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS calificacion_reportada (
    id SERIAL PRIMARY KEY, estudiante_ci VARCHAR(30) NOT NULL REFERENCES estudiante_reportado(ci),
    materia VARCHAR(160) NOT NULL, nota NUMERIC(5,2) NOT NULL CHECK (nota BETWEEN 0 AND 100),
    semestre INTEGER, anio INTEGER, UNIQUE(estudiante_ci, materia, semestre, anio)
);
CREATE TABLE IF NOT EXISTS titulo (
    id SERIAL PRIMARY KEY, ci VARCHAR(30) NOT NULL, carrera VARCHAR(160) NOT NULL,
    universidad VARCHAR(160), numero VARCHAR(80) UNIQUE NOT NULL, valido BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS certificado_nacional (
    numero VARCHAR(80) PRIMARY KEY, ci VARCHAR(30) NOT NULL, carrera VARCHAR(160) NOT NULL,
    emitido_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS auditoria (
    id BIGSERIAL PRIMARY KEY, fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    origen VARCHAR(80), tipo_operacion VARCHAR(80), id_solicitud VARCHAR(120),
    universidad VARCHAR(160), resultado VARCHAR(30), detalle TEXT
);
CREATE TABLE IF NOT EXISTS udp_idempotencia (
    id_solicitud VARCHAR(120) PRIMARY KEY, solicitud TEXT NOT NULL, respuesta TEXT NOT NULL,
    procesado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS materia_malla (
    id SERIAL PRIMARY KEY, carrera_id INTEGER NOT NULL REFERENCES carrera(id) ON DELETE CASCADE,
    nombre VARCHAR(160) NOT NULL, semestre INTEGER NOT NULL CHECK (semestre > 0),
    creditos NUMERIC(6,2), carga_horaria INTEGER,
    UNIQUE(carrera_id, nombre, semestre)
);
CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON auditoria(fecha);
CREATE INDEX IF NOT EXISTS idx_auditoria_tipo ON auditoria(tipo_operacion);
CREATE INDEX IF NOT EXISTS idx_materia_malla_carrera ON materia_malla(carrera_id, semestre);
CREATE UNIQUE INDEX IF NOT EXISTS uq_certificado_ci_carrera ON certificado_nacional(ci, carrera);
