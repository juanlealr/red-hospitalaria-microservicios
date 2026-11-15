-- admisiones-service/src/main/resources/db/migration/V4__bloqueo_optimista_inventario.sql
ALTER TABLE inventario_camas ADD COLUMN version BIGINT NOT NULL DEFAULT 0;