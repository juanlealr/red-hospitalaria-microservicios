# farmacia-service

Microservicio del subdominio **Farmacia y cobertura** (Dev 3) de la red
hospitalaria multi-sede. Cubre HU-06 (recibir y despachar solicitudes de
medicamentos) y HU-07 (verificar la cobertura con la aseguradora externa).

Es el hexágono de `farmacia` extraído a su propio proceso, con su propia base
de datos, su propio despliegue y el mismo modelo de dominio del taller de DDD.

## Arquitectura

```
                       ┌──────────────────────────────────────────┐
   cliente / gateway   │        farmacia-service (hexágono)       │
   ──────────────────► │                                          │
   POST /api/farmacia  │  infraestructura.entrada.rest            │
                       │      SolicitudMedicamentoController      │
                       │              │ (puerto primario)          │
                       │              ▼                           │
                       │  aplicacion.SolicitudMedicamentoService  │
                       │    ├── SolicitudMedicamentoFactory       │
                       │    ├── VerificacionCoberturaService      │
                       │    └── RegistroSolicitudTransaccional    │
                       │              │                           │
                       │      ┌───────┴────────┐                  │
                       │      ▼                ▼                  │
                       │  RepositorioSolicitudes  ConsultaCobertura│
                       │  (puerto secundario)     (puerto 2rio)   │
                       └──────┬──────────────┬──────────────┬─────┘
                              │              │              │
                    PostgreSQL 16     Kafka (outbox)   Aseguradora
                    farmacia_db       farmacia.        externa
                                      solicitudes       (REST)
```

Regla de dependencia: `dominio` no importa Spring, JPA, HTTP ni Jackson;
`aplicacion` depende solo de sus puertos; `infraestructura` implementa esos
puertos. El modelo de persistencia (`SolicitudMedicamentoJpaEntity`) y el DTO
REST viven únicamente en infraestructura y se traducen con mappers.

### Estructura

| Paquete | Contenido |
| --- | --- |
| `dominio` | `SolicitudMedicamento` (raíz del Agregado), `Cobertura` (Value Object), `EstadoSolicitudMedicamento` |
| `aplicacion` | puerto primario `SolicitudMedicamentoUseCase`, caso de uso, `SolicitudMedicamentoFactory`, `VerificacionCoberturaService`, puertos secundarios y `EventosFarmacia` |
| `infraestructura.entrada.rest` | controlador REST, DTOs, mapper y traducción de errores a HTTP |
| `infraestructura.salida.persistencia` | entidad JPA, repositorio Spring Data, adaptador y mapper de persistencia |
| `infraestructura.salida.mensajeria` | Transactional Outbox y publicador a Kafka |
| `infraestructura.salida.aseguradora` | adaptadores del puerto de cobertura: REST con Circuit Breaker y stub local |

## Puertos y dependencias

| Componente | Puerto | Notas |
| --- | --- | --- |
| `farmacia-service` | 8081 | HTTP + Eureka |
| PostgreSQL | 5433 | contenedor `farmacia-db` (Database per Service) |
| Kafka | 9092 | infraestructura compartida; topic `farmacia.solicitudes` |
| Eureka | 8761 | registro de servicios |
| Gateway | 8000 | enruta `/api/farmacia/**` → `lb://farmacia-service` |

## API

| Método | Ruta | Caso de uso |
| --- | --- | --- |
| POST | `/api/farmacia/solicitudes` | Recibir solicitud (HU-06) |
| POST | `/api/farmacia/solicitudes/{id}/verificar-cobertura` | Verificar cobertura (HU-07) |
| POST | `/api/farmacia/solicitudes/{id}/despachar` | Despachar medicamento (HU-06) |
| GET | `/api/farmacia/solicitudes/{id}` | Consultar solicitud |
| GET | `/api/farmacia/solicitudes` | Listar solicitudes |
| GET | `/api/status` | Estado del servicio |

Ejemplo:

```powershell
$solicitud = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/farmacia/solicitudes `
  -ContentType "application/json" `
  -Body '{"pacienteId":"pac-1","medicoId":"med-1","medicamento":"Ibuprofeno 400 mg"}'

Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/farmacia/solicitudes/$($solicitud.id)/verificar-cobertura"
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/farmacia/solicitudes/$($solicitud.id)/despachar"
```

Manejo de errores (ProblemDetail):

| Situación | HTTP |
| --- | --- |
| Solicitud inexistente | 404 |
| Cuerpo inválido o id vacío | 400 |
| Transición de estado inválida (despachar sin cobertura, doble despacho) | 409 |
| Aseguradora caída o Circuit Breaker abierto | 503 |

## Eventos de dominio (Transactional Outbox + Kafka)

Cada cambio de estado guarda el evento en la tabla `outbox_events` **en la
misma transacción** que el cambio del Agregado. `OutboxEventPublisher` toma
lotes con `FOR UPDATE SKIP LOCKED`, los envía a Kafka y solo los marca como
publicados cuando el bróker confirma. Si Kafka está caído, el evento queda
pendiente y el servicio sigue respondiendo.

| Evento | Cuándo |
| --- | --- |
| `MedicamentoSolicitado` | POST de una solicitud nueva |
| `CoberturaVerificada` | La aseguradora respondió y se registró la cobertura |
| `MedicamentoDespachado` | Se despachó el medicamento |

- Topic: `farmacia.solicitudes` (configurable con `farmacia.kafka.topic-solicitudes`).
- Clave de partición: `solicitudId` → preserva el orden por Agregado.
- Cada mensaje lleva `eventoId` (UUID) para que los consumidores puedan
  implementar Idempotent Consumer.

## Integración con la aseguradora

El puerto `ConsultaCoberturaExterna` tiene dos adaptadores intercambiables:

- **Stub (por defecto)**: devuelve 80% para cualquier paciente. Permite
  levantar y probar el servicio sin dependencias externas.
- **REST** (`--spring.profiles.active=aseguradora-rest`): llama a
  `ASEGURADORA_URL` (`/cobertura/{pacienteId}`), con timeouts de conexión y
  lectura y Circuit Breaker de Resilience4j.

La llamada externa se hace **fuera** de la transacción de base de datos y
antes de registrar la cobertura; si falla, la solicitud queda en `RECIBIDA`
y el resto de la operación clínica sigue disponible.

> Decisión REST vs. gRPC: la aseguradora es un sistema de terceros, no un
> servicio propio de alto volumen. La guía de la Lección 6 recomienda REST
> para integraciones externas y reserva gRPC para tráfico interno entre
> servicios propios, así que aquí no se justifica un contrato `.proto`.

## Cómo ejecutarlo

```powershell
# 1. Base de datos
cd farmacia-service
docker compose up -d

# 2. Kafka (infraestructura compartida con admisiones-service)
cd ..\admisiones-service
docker compose up -d kafka

# 3. Eureka y Gateway (en terminales aparte)
cd ..\eureka-server
.\mvnw.cmd spring-boot:run

cd ..\gateway
.\mvnw.cmd spring-boot:run

# 4. El servicio (Flyway crea las tablas al arrancar)
cd ..\farmacia-service
.\mvnw.cmd spring-boot:run
```

También se puede consumir a través del gateway:
`http://localhost:8000/api/farmacia/solicitudes`.

Variables de entorno soportadas: `FARMACIA_DB_URL`, `FARMACIA_DB_USER`,
`FARMACIA_DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `EUREKA_URL`,
`ASEGURADORA_URL`.

## Pruebas

```powershell
.\mvnw.cmd test
```

35 pruebas que corren en segundos, sin base de datos ni bróker: dominio
(`Cobertura`, `SolicitudMedicamento` y su reconstrucción desde persistencia),
factory, servicio de dominio, caso de uso con repositorio y publicador fake,
controlador con MockMvc standalone y adaptador stub de la aseguradora.

## Decisiones de descomposición

- **Decompose by Subdomain**: el límite es el Bounded Context "Farmacia y
  cobertura" del Event Storming, no una capa técnica.
- **Database per Service**: `farmacia_db` es privada; nadie más lee ni
  escribe estas tablas.
- **Self-contained Service**: despachar no llama a la aseguradora; la
  verificación es un paso previo explícito (HU-07), así que una caída del
  tercero no bloquea el despacho de solicitudes ya verificadas.
- **Síncrono vs. asíncrono**: la aseguradora es una consulta que necesita
  respuesta (REST + Circuit Breaker); los hechos del dominio se publican de
  forma asíncrona (Kafka + Outbox) para que otros servicios reaccionen sin
  acoplarse.

## Pendiente para fases posteriores

- Consumir estos eventos desde Admisiones para el estado consolidado (HU-08).
- Exponer `sedeId` en la solicitud para enrutar el despacho por sede.
- Sustituir el stub de la aseguradora por el contrato real cuando esté
  disponible.
