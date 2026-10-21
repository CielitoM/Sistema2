-- Migración no destructiva para instalaciones existentes de mec_db.
ALTER TABLE auditoria ADD COLUMN IF NOT EXISTS id_solicitud VARCHAR(120);
ALTER TABLE auditoria ADD COLUMN IF NOT EXISTS universidad VARCHAR(160);
CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON auditoria(fecha);
CREATE INDEX IF NOT EXISTS idx_auditoria_tipo ON auditoria(tipo_operacion);
CREATE TABLE IF NOT EXISTS udp_idempotencia (
    id_solicitud VARCHAR(120) PRIMARY KEY,
    solicitud TEXT NOT NULL,
    respuesta TEXT NOT NULL,
    procesado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS materia_malla (
    id SERIAL PRIMARY KEY,
    carrera_id INTEGER NOT NULL REFERENCES carrera(id) ON DELETE CASCADE,
    nombre VARCHAR(160) NOT NULL,
    semestre INTEGER NOT NULL CHECK (semestre > 0),
    creditos NUMERIC(6,2),
    carga_horaria INTEGER,
    UNIQUE(carrera_id, nombre, semestre)
);
CREATE INDEX IF NOT EXISTS idx_materia_malla_carrera ON materia_malla(carrera_id, semestre);
CREATE UNIQUE INDEX IF NOT EXISTS uq_certificado_ci_carrera
    ON certificado_nacional(ci, carrera);
