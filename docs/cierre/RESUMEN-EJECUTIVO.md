# Resumen ejecutivo

Se construyó una plataforma de microservicios para coordinar admisiones, camas, medicación y consulta consolidada de pacientes en una red hospitalaria multi-sede. Un API Gateway recibe todas las peticiones, valida el token JWT y aplica permisos por rol (admisiones, médico, laboratorio, farmacéutico y administrador). Los servicios de admisiones, farmacia, autenticación y consultas tienen su propia base de datos y se comunican por eventos en Kafka, con el patrón Outbox para no perder mensajes.

Entregado: ingreso de pacientes con ID asignado por el sistema, disponibilidad de camas, traslados con historial, alta médica, cobertura con aseguradora, reporte de ocupación, gestión de usuarios, despacho de medicamentos y un estado consolidado del paciente, además de un frontend web por rol. Las integraciones externas están protegidas con Circuit Breaker para que su caída no detenga la operación interna.

Pendiente al corte: el servicio de laboratorio (exámenes y resultados), la validación JWT en farmacia, la cola de mensajes fallidos y la alta disponibilidad con varias instancias. Esos puntos están documentados como riesgos en el ATAM-LITE y como estado en la matriz de trazabilidad.
