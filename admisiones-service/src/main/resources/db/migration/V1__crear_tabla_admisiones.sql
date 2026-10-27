CREATE TABLE admisiones (
    id VARCHAR(255) PRIMARY KEY,
    paciente_id VARCHAR(255) NOT NULL,
    sede_actual_id VARCHAR(255) NOT NULL,
    estado VARCHAR(50) NOT NULL,
    fecha_ingreso TIMESTAMP NOT NULL,
    fecha_alta TIMESTAMP
);

CREATE TABLE admision_traslados (
    admision_id VARCHAR(255) NOT NULL,
    sede_origen_id VARCHAR(255) NOT NULL,
    sede_destino_id VARCHAR(255) NOT NULL,
    fecha TIMESTAMP NOT NULL,
    CONSTRAINT fk_admision_traslados_admision FOREIGN KEY (admision_id) REFERENCES admisiones(id) ON DELETE CASCADE
);