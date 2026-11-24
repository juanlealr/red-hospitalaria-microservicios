# laboratorio-service

Microservicio del Bounded Context **Laboratorio** (HU-04, HU-05, HU-09) de la Red hospitalaria
multi-sede. Misma estructura hexagonal, mismo formato de errores, mismo esquema de seguridad JWT
y Flyway que `admisiones-service`; mismo patrón de eventos (Transactional Outbox + Kafka) que
`farmacia-service`.

## Puertos (ya no chocan con el resto del proyecto)

| Qué | Puerto | Nota |
|---|---|---|
| **HTTP** laboratorio | **8082** | `SERVER_PORT` lo cambia. 8080 admisiones · 8081 farmacia · 8083 consultas · 8085 auth · 8000 gateway · 8761 eureka |
| **gRPC** laboratorio | **9091** | `GRPC_PORT`. Es el `static://localhost:9091` que ya usa admisiones-service |
| **PostgreSQL** `laboratorio-db` | **5434** (host) | 5432 admisiones-db · 5433 farmacia-db · 5436 auth-db |
| Kafka | 9092 | compartido; lo levanta `admisiones-service/docker-compose.yml` |
| Eureka | `laboratorio-service` | `EUREKA_URL` (por defecto `http://localhost:8761/eureka`) |

Variables de entorno soportadas: `SERVER_PORT`, `GRPC_PORT`, `LABORATORIO_DB_URL`,
`LABORATORIO_DB_USER`, `LABORATORIO_DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `EUREKA_URL`, `JWT_SECRET`.

## Cómo arrancarlo

Orden sugerido: `eureka-server` → bases de datos y Kafka → `laboratorio-service` →
`admisiones-service` → `gateway`.

```bash
cd laboratorio-service
docker compose up -d             # PostgreSQL de laboratorio (puerto 5434)
./mvnw spring-boot:run           # en Windows: .\mvnw.cmd spring-boot:run
curl http://localhost:8082/api/status
```

Al iniciar debe aparecer en el log gRPC escuchando en el puerto **9091**; si aparece otro,
Admisiones no conecta. Kafka no es obligatorio para arrancar: si está caído, los eventos quedan
en la tabla `outbox_events` y se publican cuando vuelva.

## Estructura

```
co.edu.uptc.laboratorio_service
├── dominio/            OrdenLaboratorio (raíz), ResultadoExamen (VO), EstadoOrdenLaboratorio, OrdenLaboratorioFactory
├── aplicacion/         OrdenLaboratorioUseCase (puerto de entrada), OrdenLaboratorioService,
│                       RepositorioOrdenesLaboratorio y PublicadorEventosOrden (puertos de salida),
│                       EventosLaboratorio (nombres de eventos)
├── compartido/         GlobalExceptionHandler, RecursoNoEncontradoException
└── infraestructura/
    ├── entrada/controlador/   OrdenLaboratorioController, StatusController, dto/
    ├── entrada/grpc/          LaboratorioGrpcService  (lo llama admisiones-service)
    ├── salida/persistencia/   OrdenLaboratorioEntity, JpaRepository y adaptador, OutboxEventEntity/Repository
    ├── salida/mensajeria/     PublicadorEventosOutboxAdapter, OutboxEventPublisher, EventoOrdenPayload
    ├── configuracion/         DominioConfig (registra la factory del dominio como bean)
    └── seguridad/             SecurityConfig, ManejadorErroresSeguridad
```

El dominio **no tiene anotaciones de Spring ni de JPA**; la entidad JPA es un modelo aparte
y el adaptador traduce entre ambos.

## API REST (`/api/laboratorio/ordenes`) y roles

Los roles son los de `auth_service` (`ADMISIONES, MEDICO, LABORATORIO, FARMACEUTICO, ADMINISTRADOR`).
El gateway deja pasar `/api/laboratorio/**` solo a **MEDICO** y **LABORATORIO**; este servicio
repite esa regla y la afina por acción (defensa en profundidad: también protege el acceso directo al 8082).

| Método y ruta | Qué hace | Rol |
|---|---|---|
| `POST /` `{pacienteId, tipoExamen}` | HU-04 solicitar examen → 201 | MEDICO |
| `GET /` `?pacienteId=` | listar órdenes (todas o por paciente) | MEDICO, LABORATORIO |
| `GET /{id}` | consultar una orden | MEDICO, LABORATORIO |
| `GET /pendientes/{pacienteId}` | `{pacienteId, tienePendientes}` (misma consulta que el gRPC) | MEDICO, LABORATORIO |
| `POST /{id}/resultado` `{valor, unidad}` | HU-05 registrar resultado → 200 | LABORATORIO |
| `PUT /{id}/cierre` | cerrar orden → 204 | LABORATORIO |
| `GET /api/status` | estado del servicio (público) | — |

`ADMINISTRADOR` y `ADMISIONES` **no** tienen acceso HTTP (Admisiones consulta por gRPC, que no usa JWT).

Errores con el mismo formato que admisiones: `{timestamp, status, error, mensaje}`
(400 datos inválidos, 401 sin token, 403 rol sin permiso, 404 no existe, 409 estado inválido o conflicto de concurrencia).

Respuesta de una orden:

```json
{
  "id": "uuid", "pacienteId": "PAC-1001", "tipoExamen": "Hemograma",
  "estado": "SOLICITADA | CON_RESULTADO | CERRADA", "pendiente": true,
  "fechaSolicitud": "2026-11-22T10:15:30.123",
  "resultado": { "valor": "120", "unidad": "g/L" }
}
```
`resultado` es `null` hasta que el laboratorio lo registra.

## Contrato gRPC con Admisiones (HU-09)

`src/main/proto/laboratorio.proto` es equivalente al de `admisiones-service` (verificado: solo cambia
`java_package`). **No agregues `package` ni cambies números de campo** en uno sin cambiarlo en el otro.
Una orden está *pendiente* mientras no esté `CERRADA` (`SOLICITADA` o `CON_RESULTADO`); la regla vive
en `EstadoOrdenLaboratorio`.

## Eventos (Transactional Outbox + Kafka)

Cada cambio de la orden guarda su evento en `outbox_events` **en la misma transacción**;
`OutboxEventPublisher` los envía a Kafka y solo los marca publicados cuando el bróker confirma.

| Evento (`tipo`) | Cuándo |
|---|---|
| `ExamenSolicitado` | `POST /` (HU-04) |
| `ResultadoRegistrado` | `POST /{id}/resultado` (HU-05) |
| `OrdenCerrada` | `PUT /{id}/cierre` — la orden deja de bloquear el alta |

- Topic: **`laboratorio.ordenes`** (`laboratorio.kafka.topic-ordenes`). Un solo topic, el evento va en el campo `tipo`
  (igual que `farmacia.solicitudes`). Clave de partición: `ordenId` → orden garantizado por orden.
- Cada mensaje trae `eventoId` (UUID) para consumidores idempotentes.
- Cada evento lleva el **estado completo** de la orden, así el consumidor puede hacer *upsert* por `ordenId`.

```json
{
  "eventoId": "uuid", "tipo": "ResultadoRegistrado", "ocurridoEn": "2026-11-22T10:20:00.000",
  "ordenId": "uuid", "pacienteId": "PAC-1001", "tipoExamen": "Hemograma",
  "estado": "CON_RESULTADO", "resultadoValor": "120", "resultadoUnidad": "g/L",
  "fechaSolicitud": "2026-11-22T10:15:30.123"
}
```
(`resultadoValor`/`resultadoUnidad` son `null` en `ExamenSolicitado`; las fechas son texto ISO-8601.)

## Interfaz (frontend)

Las pantallas están en `frontend/src/features/laboratorio/` y siguen el mismo estilo que admisiones:

| Ruta | Pantalla | Rol |
|---|---|---|
| `/laboratorio` | Listado de órdenes, con contadores y filtros | MEDICO, LABORATORIO |
| `/laboratorio/nueva` | Solicitar examen (HU-04) | MEDICO |
| `/laboratorio/:id` | Detalle; registrar resultado y cerrar (HU-05) | MEDICO, LABORATORIO |

Además, el detalle de la admisión lista los exámenes del paciente con un enlace para solicitar otro, y
«Paciente consolidado» ya los muestra en lugar del texto de pendiente.

Los tres estados se distinguen por color: `SOLICITADA` ámbar, `CON_RESULTADO` tinta y `CERRADA` verde.
`CON_RESULTADO` **sigue bloqueando el alta**, y por eso no se pinta de verde.

El flujo completo está cubierto por una prueba automática de interfaz:

```bash
cd frontend && npm run test:flujo
```

Hace clic sobre la aplicación real (solicitar → 409 en el alta → resultado → cierre → alta concedida)
contra un gateway simulado que aplica las mismas reglas de roles del backend. Ver
`frontend/pruebas/README.md`.

## Qué falta fuera de este servicio

**`consultas_admisiones_service` (HU-08)** no consume todavía el topic `laboratorio.ordenes`. Mientras
tanto, «Paciente consolidado» pide los exámenes directamente a este servicio: se ven al instante, pero
sin pasar por la proyección por eventos. Para cerrarlo como en farmacia hacen falta un `NewTopic` en
`TopicosConfig`, un `@KafkaListener`, un `ProyectorFichasOrdenLaboratorio` (upsert por `ordenId`), una
`FichaOrdenLaboratorio` y el campo `ordenesLaboratorio` en `EstadoConsolidadoPaciente`.

## Pruebas manuales (a través del gateway, puerto 8000)

```bash
GW=http://localhost:8000
login() { curl -s -X POST $GW/api/auth/login -H 'Content-Type: application/json' \
          -d "{\"email\":\"$1\",\"password\":\"$2\"}" | sed 's/.*"token":"\([^"]*\)".*/\1/'; }
# Usuarios de ejemplo: los crea el ADMINISTRADOR con POST /api/auth/usuarios {email, password, rol}
TOK_MED=$(login medico@hospital.test 12345678)
TOK_LAB=$(login lab@hospital.test 12345678)

# MEDICO solicita (201); con TOK_LAB el mismo POST debe dar 403
curl -i -X POST $GW/api/laboratorio/ordenes -H "Authorization: Bearer $TOK_MED" \
     -H 'Content-Type: application/json' -d '{"pacienteId":"PAC-1001","tipoExamen":"Hemograma"}'
curl -s $GW/api/laboratorio/ordenes/pendientes/PAC-1001 -H "Authorization: Bearer $TOK_LAB"   # tienePendientes: true

# LABORATORIO registra resultado y cierra (con TOK_MED ambos deben dar 403)
curl -X POST $GW/api/laboratorio/ordenes/<ID>/resultado -H "Authorization: Bearer $TOK_LAB" \
     -H 'Content-Type: application/json' -d '{"valor":"120","unidad":"g/L"}'
curl -X PUT  $GW/api/laboratorio/ordenes/<ID>/cierre -H "Authorization: Bearer $TOK_LAB"
```

Con una orden abierta, `PUT /api/admisiones/{id}/alta` (rol MEDICO) debe responder **409**; al cerrar la orden, **204**.
Para ver los eventos: `kafka-console-consumer --bootstrap-server localhost:9092 --topic laboratorio.ordenes --from-beginning`.

## Decisión abierta

**¿Quién cierra la orden?** Hoy solo `LABORATORIO` (`PUT /{id}/cierre`) y una orden `CON_RESULTADO` cuenta como
pendiente, es decir, **bloquea el alta médica hasta que el laboratorio la cierre**. Si el equipo prefiere que el médico
confirme/cierre, o que solo `SOLICITADA` bloquee el alta, son cambios de una línea en `SecurityConfig` /
`EstadoOrdenLaboratorio`.

## Tests

```bash
./mvnw test    # no requiere Docker ni Kafka ni base de datos
```

| Clase de test | Qué cubre |
|---|---|
| `dominio/OrdenLaboratorioTest` | invariantes del agregado (ciclo SOLICITADA → CON_RESULTADO → CERRADA, reconstitución) |
| `aplicacion/OrdenLaboratorioServiceTest` | casos de uso con repositorio y publicador falsos; **qué evento se emite en cada paso** y que una operación rechazada no emite nada |
| `infraestructura/entrada/controlador/OrdenLaboratorioControllerTest` | REST con MockMvc standalone: 201/200/204, validaciones 400, 404, 409 (estado y concurrencia), 500 sin filtrar detalle, formato de error `{timestamp,status,error,mensaje}` |
| `infraestructura/entrada/controlador/dto/OrdenLaboratorioResponseTest` | forma JSON pública de la orden (`resultado` nulo/presente, `pendiente`, fecha ISO) |
| `infraestructura/seguridad/SeguridadRolesTest` | **matriz rol × endpoint** contra la `SecurityConfig` real, con JWT HS256 firmados como `auth_service`; 401 (sin token, basura, otro secreto, expirado, `alg=none`) y 403 (rol sin permiso, token sin claim `rol`). Debe seguir alineada con el gateway |
| `infraestructura/salida/mensajeria/PublicadorEventosOutboxAdapterTest` | contrato del payload de `laboratorio.ordenes` campo por campo (si cambia, hay que avisar a los consumidores) |
| `infraestructura/salida/mensajeria/OutboxEventPublisherTest` | solo marca publicado si Kafka confirma, corta el lote ante un fallo y reintenta en el ciclo siguiente |
| `infraestructura/entrada/grpc/LaboratorioGrpcServiceTest` | adaptador gRPC que consume Admisiones |
| `infraestructura/salida/persistencia/OrdenLaboratorioEntityTest` | mapeo dominio ↔ entidad JPA |

No hay test del adaptador JPA contra PostgreSQL real (haría falta Testcontainers o `@DataJpaTest` con una base); la
migración Flyway y `ddl-auto: validate` se comprueban al arrancar el servicio.
