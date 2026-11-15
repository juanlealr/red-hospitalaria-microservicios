ALTER TABLE admisiones ADD COLUMN servicio_actual VARCHAR(100);
UPDATE admisiones SET servicio_actual = 'sin-especificar' WHERE servicio_actual IS NULL;
ALTER TABLE admisiones ALTER COLUMN servicio_actual SET NOT NULL;

ALTER TABLE admision_traslados ADD COLUMN servicio_origen VARCHAR(100);
ALTER TABLE admision_traslados ADD COLUMN servicio_destino VARCHAR(100);

CREATE TABLE inventario_camas (
    id             VARCHAR(255) PRIMARY KEY,
    sede_id        VARCHAR(255) NOT NULL,
    servicio       VARCHAR(100) NOT NULL,
    camas_totales  INT NOT NULL,
    camas_ocupadas INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventario_sede_servicio UNIQUE (sede_id, servicio)
);