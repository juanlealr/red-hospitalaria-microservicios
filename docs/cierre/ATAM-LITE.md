# ATAM-LITE — Evaluación de riesgos de la arquitectura

Evaluación breve de los atributos de calidad que pide el enunciado, contrastados con lo que está construido.

## Escenarios de calidad

| # | Atributo | Escenario | Respuesta esperada |
|---|---|---|---|
| E1 | Disponibilidad | El servicio de laboratorio no responde durante un alta | El alta se rechaza con 503; ingresos, traslados y consultas siguen operando |
| E2 | Consistencia | Dos médicos trasladan a la última cama de un servicio al mismo tiempo | Uno tiene éxito y el otro recibe 409; el inventario nunca queda negativo |
| E3 | Seguridad | Un rol sin permiso intenta trasladar o dar de alta | 403 en JSON, tanto en gateway como en el servicio |
| E4 | Trazabilidad | Se consulta el recorrido de un paciente | El historial conserva cada traslado con origen, destino y fecha |
| E5 | Modificabilidad | Se cambia la aseguradora o se agrega laboratorio al consolidado | Cambia un adaptador o un proyector, sin tocar el dominio |

## Decisiones arquitectónicas frente a los escenarios

| Decisión | Escenarios que apoya |
|---|---|
| Circuit Breaker en gRPC y REST | E1 |
| Bloqueo optimista y restricción única en base de datos | E2 |
| JWT validado en gateway y servicios | E3 |
| Historial de traslados y eventos en Outbox | E4 |
| Arquitectura hexagonal y puertos | E5 |

## Riesgos

- **Riesgo 1 — Laboratorio no entregado.** HU-04 y HU-05 no existen y el alta depende de un servicio que aún no corre. Mientras ese servicio no exista, toda alta falla con 503. Mitigación: entregar `laboratorio-service` con el contrato gRPC ya definido en el adaptador.
- **Riesgo 2 — Seguridad incompleta en farmacia.** `farmacia-service` no valida JWT, así que quien lo llame directo, sin gateway, evita la autorización. Mitigación: replicar el resource server de los otros servicios.
- **Riesgo 3 — Secreto JWT y credenciales de desarrollo en archivos versionados.** Un secreto compartido filtrado permite falsificar tokens. Mitigación: rotarlo, no subirlo al repositorio y exigir la variable de entorno en producción.
- **Riesgo 4 — Eventos sin cola de mensajes fallidos.** Un mensaje que no se puede procesar solo se registra en el log y se pierde para el modelo de lectura. Mitigación: DLQ y reproceso.
- **Riesgo 5 — Disponibilidad limitada.** Cada servicio, Kafka y las bases corren en una sola instancia, lo que no cumple una alta disponibilidad real. Mitigación: réplicas, balanceo y bases con réplica.

## Puntos de sensibilidad

- El umbral del Circuit Breaker (50 % en ventana de 10) cambia qué tan pronto se rechazan las altas.
- El intervalo del Outbox (2 s) fija la latencia con que el consolidado ve los cambios.

## Compromisos (trade-offs)

- **Consistencia eventual en lectura** a cambio de independencia entre servicios (el consolidado puede ir unos segundos atrás).
- **Fallar cerrado en el alta** a cambio de seguridad clínica (se prefiere no dar el alta a darla sin verificar).

## No-riesgos

- La reserva de camas es segura ante concurrencia gracias a la versión optimista.
- El núcleo de negocio es sustituible en sus integraciones por el uso de puertos.
