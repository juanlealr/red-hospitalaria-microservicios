CREATE UNIQUE INDEX ux_inventario_sede_servicio_ci
    ON inventario_camas (lower(sede_id), lower(servicio));