-- auth-service/src/main/resources/db/migration/V1__crear_tabla_usuarios.sql
CREATE TABLE usuarios (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL
);