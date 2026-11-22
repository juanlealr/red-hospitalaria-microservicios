# Matriz de trazabilidad: requisitos contra implementación

| Requisito | Servicio / pantalla | Mecanismo | Estado al corte |
|---|---|---|---|
| HU-01 Registrar ingreso | admisiones-service; Nueva admisión | Caso de uso `registrarIngreso`, ID de paciente asignado por secuencia, evento por Outbox | Implementado |
| HU-02 Disponibilidad de camas | admisiones-service; Disponibilidad | Inventario por sede y servicio, polling en el frontend | Implementado |
| HU-03 Traslado entre sedes | admisiones-service; detalle de admisión (médico) | Rechazo sin cama destino, historial de traslados, evento `PacienteTrasladado` | Implementado |
| HU-04 Solicitar examen de laboratorio | laboratorio-service | No existe | Pendiente |
| HU-05 Registrar resultado | laboratorio-service | No existe | Pendiente |
| HU-06 Despachar medicamentos | farmacia-service | Solicitud médica obligatoria, verificación de cobertura, evento por Outbox | Implementado (sin pantalla ni JWT) |
| HU-07 Verificar cobertura | admisiones-service; Nueva admisión | Puerto `ConsultaCoberturaExterna`, adaptador REST con Circuit Breaker y stub | Implementado |
| HU-08 Estado consolidado | consultas_admisiones_service; Paciente consolidado | Modelo de lectura CQRS en MongoDB | Parcial (admisión y medicación; falta laboratorio) |
| HU-09 Alta médica | admisiones-service; detalle de admisión (médico) | Verificación gRPC de órdenes pendientes con Circuit Breaker | Implementado (depende de laboratorio) |
| HU-10 Reporte de ocupación | admisiones-service; Reporte (administrador) | Cálculo sobre el inventario real | Implementado |
| RNF Integraciones no bloquean la operación | admisiones-service | Circuit Breaker y error 503 acotado al alta | Cumplido |
| RNF Lógica independiente de la integración | admisiones-service | Puertos y adaptadores (arquitectura hexagonal) | Cumplido |
| RNF Camas consistentes | admisiones-service | `@Version` e índice único parcial | Cumplido |
| RNF Trazabilidad entre sedes | admisiones, consultas | Historial de traslados y eventos | Cumplido |
| RNF Alta disponibilidad | Todos | Una instancia por servicio | No cumplido |
