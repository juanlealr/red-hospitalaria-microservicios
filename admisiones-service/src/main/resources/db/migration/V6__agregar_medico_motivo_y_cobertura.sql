ALTER TABLE admisiones ADD COLUMN medico_tratante VARCHAR(150);
UPDATE admisiones SET medico_tratante = 'Sin especificar' WHERE medico_tratante IS NULL;
ALTER TABLE admisiones ALTER COLUMN medico_tratante SET NOT NULL;

ALTER TABLE admisiones ADD COLUMN motivo_ingreso TEXT;
UPDATE admisiones SET motivo_ingreso = 'Sin especificar' WHERE motivo_ingreso IS NULL;
ALTER TABLE admisiones ALTER COLUMN motivo_ingreso SET NOT NULL;

ALTER TABLE admisiones ADD COLUMN cobertura_porcentaje INT;
ALTER TABLE admisiones
    ADD CONSTRAINT chk_admisiones_cobertura
    CHECK (cobertura_porcentaje IS NULL OR cobertura_porcentaje BETWEEN 0 AND 100);