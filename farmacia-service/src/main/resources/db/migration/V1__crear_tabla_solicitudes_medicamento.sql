CREATE TABLE solicitudes_medicamento (
    id VARCHAR(36) PRIMARY KEY,
    paciente_id VARCHAR(255) NOT NULL,
    medico_id VARCHAR(255) NOT NULL,
    medicamento VARCHAR(255) NOT NULL,
    fecha_solicitud TIMESTAMP NOT NULL,
    estado VARCHAR(50) NOT NULL,
    cobertura_porcentaje INTEGER,
    fecha_despacho TIMESTAMP,
    CONSTRAINT chk_solicitudes_cobertura CHECK (cobertura_porcentaje IS NULL OR cobertura_porcentaje BETWEEN 0 AND 100)
);

CREATE INDEX idx_solicitudes_paciente ON solicitudes_medicamento (paciente_id);
