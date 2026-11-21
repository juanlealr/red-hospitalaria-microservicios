CREATE UNIQUE INDEX ux_admision_activa_por_paciente
    ON admisiones (paciente_id)
    WHERE estado = 'ACTIVA';