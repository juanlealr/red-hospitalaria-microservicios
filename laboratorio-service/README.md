# laboratorio-service

Microservicio del Bounded Context **Laboratorio** (HU-04, HU-05) de la Red hospitalaria
multi-sede. Está alineado con `admisiones-service`: misma estructura hexagonal, mismo
formato de errores, mismo esquema de seguridad JWT y Flyway para la base de datos.

| | |
|---|---|
| HTTP | `8081` (cámbialo con la variable `SERVER_PORT`) |
| gRPC | `9091` (el puerto al que apunta el cliente de admisiones-service) |
| Base de datos | PostgreSQL propia `laboratorio_db`, puerto **5433** del host |
| Eureka | se registra como `laboratorio-service` en `http://localhost:8761/eureka` |

## Cómo pegarlo en el proyecto

Copia la carpeta `laboratorio-service/` junto a `admisiones-service/` (mismo nivel).

```bash
cd laboratorio-service
docker-compose up -d            # PostgreSQL de laboratorio
./mvnw spring-boot:run          # en Windows: .\mvnw.cmd spring-boot:run
curl http://localhost:8081/api/status
```

Orden de arranque sugerido: `eureka-server` → bases de datos → `laboratorio-service` →
`admisiones-service` → `gateway`. Al iniciar debe aparecer en el log la línea de gRPC
escuchando en el puerto **9091**; si aparece otro puerto, el contrato con Admisiones no conecta.

## Estructura

```
co.edu.uptc.laboratorio_service
├── dominio/            OrdenLaboratorio (raíz), ResultadoExamen (VO), EstadoOrdenLaboratorio, OrdenLaboratorioFactory
├── aplicacion/         OrdenLaboratorioUseCase (puerto de entrada), OrdenLaboratorioService,
│                       RepositorioOrdenesLaboratorio (puerto de salida)
├── compartido/         GlobalExceptionHandler, RecursoNoEncontradoException
└── infraestructura/
    ├── entrada/controlador/   OrdenLaboratorioController, StatusController, dto/
    ├── entrada/grpc/          LaboratorioGrpcService  (lo llama admisiones-service)
    ├── salida/persistencia/   OrdenLaboratorioEntity, JpaRepository y adaptador
    ├── configuracion/         DominioConfig (registra la factory del dominio como bean)
    └── seguridad/             SecurityConfig, ManejadorErroresSeguridad
```

El dominio **no tiene anotaciones de Spring ni de JPA**; la entidad JPA es un modelo aparte
y el adaptador traduce entre ambos.

## API REST (`/api/laboratorio/ordenes`)

| Método y ruta | Qué hace | Roles (asumidos) |
|---|---|---|
| `POST /` `{pacienteId, tipoExamen}` | HU-04 solicitar examen → 201 | MEDICO, ADMINISTRADOR |
| `GET /` `?pacienteId=` | listar órdenes (todas o por paciente) | LABORATORIO, MEDICO, ADMINISTRADOR |
| `GET /{id}` | consultar una orden | LABORATORIO, MEDICO, ADMINISTRADOR |
| `GET /pendientes/{pacienteId}` | `{pacienteId, tienePendientes}` (misma consulta que el gRPC) | LABORATORIO, MEDICO, ADMINISTRADOR |
| `POST /{id}/resultado` `{valor, unidad}` | HU-05 registrar resultado → 200 | LABORATORIO, ADMINISTRADOR |
| `PUT /{id}/cierre` | cerrar orden → 204 | LABORATORIO, ADMINISTRADOR |
| `GET /api/status` | estado del servicio (público) | — |

Errores con el mismo formato que admisiones: `{timestamp, status, error, mensaje}`
(400 datos inválidos, 404 no existe, 409 estado inválido o conflicto de concurrencia).

## Contrato gRPC con Admisiones (HU-09)

`src/main/proto/laboratorio.proto` es idéntico al de `admisiones-service` (verificado comparando
los descriptores; solo cambia `java_package`). **No agregues `package` ni cambies números de campo**
en uno sin cambiarlo en el otro. Una orden está *pendiente* mientras no esté `CERRADA`
(`SOLICITADA` o `CON_RESULTADO`); la regla vive en `EstadoOrdenLaboratorio`.

## Pruebas manuales

```bash
TOKEN=<jwt de auth_service con rol MEDICO>
curl -X POST localhost:8081/api/laboratorio/ordenes -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" -d '{"pacienteId":"PAC-1001","tipoExamen":"Hemograma"}'
curl localhost:8081/api/laboratorio/ordenes/pendientes/PAC-1001 -H "Authorization: Bearer $TOKEN"
```

Con una orden abierta, `PUT /api/admisiones/{id}/alta` en admisiones debe responder 409;
al cerrar la orden, 204.

## Supuestos que debes confirmar (dependen de otros servicios)

1. **Roles**: `ROLE_LABORATORIO`, `ROLE_MEDICO`, `ROLE_ADMINISTRADOR` en `SecurityConfig`. `ROLE_LABORATORIO` puede no existir en `auth_service`.
2. **Puerto 8081 y 5433** pueden chocar con otros servicios del proyecto.
3. **Gateway**: debe enrutar `/api/laboratorio/**` hacia `lb://laboratorio-service`.
4. **Secreto JWT**: `${JWT_SECRET}` con el mismo valor por defecto de admisiones; debe coincidir con `auth_service`.
5. **Eventos Kafka**: no se publican (admisiones usa Outbox). Falta saber si `farmacia` o `consultas_admisiones` esperan eventos de laboratorio.
6. **Frontend**: si ya espera otras rutas o formatos de JSON para laboratorio, hay que ajustar los DTO.

## Tests

```bash
./mvnw test    # no requiere Docker: dominio, casos de uso, mapeo y adaptador gRPC
```
