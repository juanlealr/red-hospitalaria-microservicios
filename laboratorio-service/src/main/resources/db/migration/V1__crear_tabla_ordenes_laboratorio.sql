CREATE TABLE ordenes_laboratorio (
    id               UUID PRIMARY KEY,
    paciente_id      VARCHAR(255) NOT NULL,
    tipo_examen      VARCHAR(150) NOT NULL,
    estado           VARCHAR(50)  NOT NULL,
    fecha_solicitud  TIMESTAMP    NOT NULL,
    resultado_valor  VARCHAR(255),
    resultado_unidad VARCHAR(50),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_orden_resultado_coherente CHECK (
        (estado = 'SOLICITADA'
            AND resultado_valor IS NULL AND resultado_unidad IS NULL)
        OR
        (estado IN ('CON_RESULTADO', 'CERRADA')
            AND resultado_valor IS NOT NULL AND resultado_unidad IS NOT NULL)
    )
);

-- Consulta de HU-09: ¿tiene el paciente órdenes sin cerrar?
CREATE INDEX idx_ordenes_laboratorio_paciente_estado
    ON ordenes_laboratorio (paciente_id, estado);
