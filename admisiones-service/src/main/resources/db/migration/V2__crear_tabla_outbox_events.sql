CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    clave VARCHAR(255),
    tipo VARCHAR(255),
    payload TEXT,
    publicado BOOLEAN NOT NULL DEFAULT FALSE
);