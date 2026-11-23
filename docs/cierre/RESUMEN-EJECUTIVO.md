# Resumen ejecutivo

Se construyó una plataforma de microservicios para coordinar admisiones, camas, medicación y consulta consolidada de pacientes en una red hospitalaria multi-sede. Un API Gateway recibe todas las peticiones, valida el token JWT y aplica permisos por rol (admisiones, médico, laboratorio, farmacéutico y administrador). Cada servicio de negocio vuelve a validar ese token por su cuenta, de modo que no basta con saltarse el gateway para entrar. Los servicios de admisiones, farmacia, autenticación y consultas tienen su propia base de datos y se comunican por eventos en Kafka, con el patrón Outbox para no perder mensajes.

Entregado: ingreso de pacientes con ID asignado por el sistema, disponibilidad de camas, traslados con historial, alta médica, cobertura con aseguradora, reporte de ocupación, gestión de usuarios, solicitudes de laboratorio con resultados, despacho de medicamentos y un estado consolidado del paciente, además de un frontend web por rol. Las integraciones externas están protegidas con Circuit Breaker para que su caída no detenga la operación interna.

Pendiente al corte: proyectar los exámenes de laboratorio en el estado consolidado, una pantalla para laboratorio, la cola de mensajes fallidos y la alta disponibilidad con varias instancias. Esos puntos están documentados como riesgos en el ATAM-LITE y como estado en la matriz de trazabilidad.
