# Red hospitalaria multi-sede

Plataforma de microservicios para coordinar **admisiones, disponibilidad de camas,
órdenes de laboratorio y despacho de farmacia** entre varias sedes de una red
hospitalaria, con integraciones a sistemas externos (equipo de laboratorio y
aseguradora).

Proyecto del Grupo 4 · Ingeniería de Software II · UPTC.

## Arquitectura

Siete servicios Spring Boot (Java 25) detrás de un API Gateway, con una base de
datos por servicio y comunicación por eventos en Kafka:

| Servicio | Puerto | Almacén | Responsabilidad |
|---|---|---|---|
| `gateway` | 8000 | — | Punto único de entrada, enrutamiento y JWT |
| `eureka-server` | 8761 | — | Descubrimiento de servicios |
| `auth_service` | 8085 | PostgreSQL | Usuarios, roles y emisión de JWT |
| `admisiones-service` | 8080 | PostgreSQL | Admisiones, traslados, alta, camas, cobertura |
| `farmacia-service` | 8081 | PostgreSQL | Solicitudes y despacho de medicamentos |
| `laboratorio-service` | 8082 · gRPC 9091 | PostgreSQL | Órdenes de laboratorio y resultados |
| `consultas_admisiones_service` | 8083 | MongoDB | Modelo de lectura CQRS del paciente |
| `frontend` | 5173 | — | Interfaz web por rol |

Cada servicio de negocio aplica arquitectura hexagonal (dominio, aplicación,
infraestructura), valida el JWT por su cuenta y publica eventos con el patrón
Transactional Outbox. El detalle de las decisiones está en
[`docs/cierre/ARQUITECTURA.md`](docs/cierre/ARQUITECTURA.md).

## Arranque rápido

Requisitos: Docker Desktop con BuildKit, ~8 GB de RAM libres y ~6 GB de disco.

```bash
docker compose up -d --build
```

La primera construcción descarga Maven Central y compila los siete módulos una
sola vez. Las bases se crean solas y Flyway aplica el esquema al arrancar cada
servicio. El administrador inicial (`admin@hospital.com` / `123456789`) se siembra
automáticamente en `auth-service`.

Para cargar datos de prueba (cuentas de cada rol, inventario de camas, admisiones,
medicación y órdenes de laboratorio):

```powershell
powershell -ExecutionPolicy Bypass -File .\semilla-datos.ps1
```

Instrucciones ampliadas, URLs, puertos y operación: [`README-DOCKER.md`](README-DOCKER.md).

## Verificar que funciona

Con el stack arriba y los datos de prueba sembrados, este recorrido cruza **dos
servicios** (admisiones escribe en PostgreSQL, consultas lee de MongoDB lo que
llegó por Kafka).

Los pasos usan `curl` y piden el token por separado, sin depender de `jq`.

```bash
# 1. Login como administrador (para consultar el inventario)
curl -s -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@hospital.com","password":"123456789"}'
# Copie el valor de "token" de la respuesta en TOKEN
TOKEN=<pegar-token>

# 2. Inventario de camas (admisiones-service)
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8000/api/admisiones/inventario-camas/todas

# 3. Registrar un ingreso. Requiere rol ADMISIONES: repita el login del paso 1 con
#    admisiones.demo@hospital.test / Clave12345 y use ese token.
TOKEN_ADM=<pegar-token-de-admisiones>
curl -s -X POST -H "Authorization: Bearer $TOKEN_ADM" \
  "http://localhost:8000/api/admisiones/ingreso?pacienteId=PAC-001&sedeId=Sede-Central&servicio=UCI&medicoTratante=Dr.Demo&motivoIngreso=Demo"

# 4. Estado consolidado (consultas_admisiones_service, vía Kafka).
#    Puede tardar unos segundos: la proyección es asíncrona.
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8000/api/consultas/admisiones/paciente/PAC-001/consolidado
```

Respuestas esperadas: el inventario devuelve un arreglo con `sedeId`, `servicio` y
`camasDisponibles`; el ingreso responde **201** con `"estado":"ACTIVA"`; el
consolidado responde **200** con esa misma admisión dentro de `admisiones`.

## Frontend

Disponible en <http://localhost:5173>. Las cuentas de cada rol se crean desde la
pantalla de **Usuarios** con un usuario administrador. La interfaz adapta menú y
acciones al rol: admisiones, médico, laboratorio, farmacéutico y administrador.

## Pruebas

Cada servicio se prueba sin infraestructura externa:

```bash
cd farmacia-service && ./mvnw test      # idem en cada módulo
```

Incluyen pruebas de dominio, casos de uso con dobles hechos a mano y, en
`farmacia-service` y `laboratorio-service`, una matriz rol × endpoint que ejercita
la configuración de seguridad real con tokens JWT firmados.

## Documentación del proyecto

| Documento | Contenido |
|---|---|
| [`docs/cierre/ARQUITECTURA.md`](docs/cierre/ARQUITECTURA.md) | Componentes, diagrama C4 y ADR |
| [`docs/cierre/ATAM-LITE.md`](docs/cierre/ATAM-LITE.md) | Escenarios de calidad y riesgos |
| [`docs/cierre/MATRIZ.md`](docs/cierre/MATRIZ.md) | Trazabilidad requisito → implementación |
| [`docs/cierre/GOBERNANZA.md`](docs/cierre/GOBERNANZA.md) | Cómo se toman y se mantienen las decisiones |
| [`docs/cierre/REPARTO.md`](docs/cierre/REPARTO.md) | Reparto del trabajo por integrante |
