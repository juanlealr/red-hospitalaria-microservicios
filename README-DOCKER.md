# Levantar la red hospitalaria en Docker

Stack completo (7 microservicios Spring Boot + frontend) con sus bases de datos
y datos de prueba. Pensado para pruebas locales de punta a punta.

## Requisitos

- Docker Desktop corriendo (con BuildKit, que es el default).
- ~8 GB de RAM libres y ~6 GB de disco para las imágenes.
- La primera construcción descarga Maven Central y las imágenes base (tarda).

## 1. Levantar todo

```bash
docker compose up -d --build
```

Esto construye los 7 servicios con un único Dockerfile multi-stage
(`docker/java-service.Dockerfile`), que compila los 7 módulos **una sola vez** y
reutiliza la capa en cada imagen.

Las bases se crean solas y **Flyway** aplica el esquema al arrancar cada servicio.
El usuario administrador `admin@hospital.com` / `123456789` se siembra
automáticamente en `auth-service`.

## 2. Sembrar datos de prueba

Con el stack arriba:

```powershell
powershell -ExecutionPolicy Bypass -File .\semilla-datos.ps1
```

Siembra, **usando la API real** (vía gateway, igual que el frontend):

| Dato | Detalle |
|---|---|
| Cuentas de personal | `admisiones`, `medico`, `laboratorio`, `farmaceutico` (`Clave12345`) |
| Inventario de camas | 3 sedes × 3 servicios con capacidades |
| Admisiones | 4 pacientes con ingreso, cobertura (HU-07) y traslados (HU-03) |
| Alta médica | 1 paciente dado de alta (HU-09) |
| Laboratorio | 2 órdenes, una con resultado y cerrada (HU-04/HU-05) |
| Farmacia | 2 solicitudes, una verificada y despachada (HU-06) |
| CQRS | Espera los eventos Kafka y verifica las proyecciones en Mongo (HU-08) |

El script es idempotente: si lo corres de nuevo no duplica pacientes activos ni
falla por correos repetidos.

## 3. URLs

| Servicio | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Gateway (API) | http://localhost:8000 |
| Eureka | http://localhost:8761 |
| Auth | http://localhost:8085 |
| Admisiones | http://localhost:8080 |
| Farmacia | http://localhost:8081 |
| Laboratorio | http://localhost:8082 (gRPC 9091) |
| Consultas (CQRS) | http://localhost:8083 |

Puertos de bases: Postgres admisiones `5432`, farmacia `5433`, laboratorio
`5434`, auth `5436`; Mongo `27017`; Kafka `9092`.

## 4. Verificación rápida

```bash
curl http://localhost:8000/api/status            # gateway
curl http://localhost:8080/api/status            # admisiones
curl http://localhost:8761/eureka/apps | findstr /i "AUTH-SERVICE"
```

Las pruebas unitarias de cada servicio corren sin infraestructura externa:

```bash
cd farmacia-service && ./mvnw test               # idem en cada módulo
```

> **Nota sobre roles e inventario:** `POST /api/admisiones/inventario-camas`
> exige `ROLE_ADMINISTRADOR` tanto en el servicio como en el gateway. Por eso
> `semilla-datos.ps1` carga el inventario como **admin**. Si un script de
> pruebas antiguo asumía que `ADMISIONES` podía hacerlo, ese paso devolverá 403.

## 5. Operación

```bash
docker compose ps                  # estado
docker compose logs -f gateway     # logs de un servicio
docker compose restart auth-service
docker compose down                # apagar (datos persisten)
docker compose down -v             # apagar y BORRAR datos
```

## Notas de arquitectura

- **Aseguradora externa (9095):** ambos servicios usan el *stub* del 80% por
  defecto (perfil `!aseguradora-rest`), así que **no** se necesita un servicio
  externo para probar.
- **Red interna:** todas las URLs `localhost` de los `application.yaml` se
  sobrescriben por variables de entorno en `docker-compose.yml` (Spring las
  enlaza de forma relajada, p. ej. `SPRING_DATASOURCE_URL`). Ojo: Spring Boot 4
  usa `spring.mongodb.uri` (no `spring.data.mongodb.uri`), por eso la variable
  es `SPRING_MONGODB_URI`.
- **gRPC (HU-09):** `admisiones-service` llama a `laboratorio-service:9091`. Spring
  Boot 4 auto-protege el servidor gRPC y el cliente no envía credenciales, lo que
  hacía fallar el alta con `UNAUTHENTICATED`. En `docker-compose.yml` se excluye
  esa autoconfiguración **solo en laboratorio**
  (`SPRING_AUTOCONFIGURE_EXCLUDE=...GrpcServerSecurityAutoConfiguration`); los
  endpoints REST siguen protegidos por JWT. Por eso laboratorio se declara como
  dependencia de admisiones.
