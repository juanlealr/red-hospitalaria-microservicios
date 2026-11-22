-- Transactional Outbox: el evento se guarda en la misma transacción que el cambio
-- de la orden y OutboxEventPublisher lo entrega a Kafka con reintentos.
CREATE TABLE outbox_events (
    id          BIGSERIAL PRIMARY KEY,
    evento_id   VARCHAR(36)  NOT NULL UNIQUE,
    clave       VARCHAR(255) NOT NULL,
    tipo        VARCHAR(100) NOT NULL,
    payload     TEXT         NOT NULL,
    ocurrido_en TIMESTAMP    NOT NULL,
    publicado   BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_outbox_pendientes ON outbox_events (publicado, id);
