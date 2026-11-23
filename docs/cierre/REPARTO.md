# Reparto del trabajo

Equipo del Grupo 4 de Ingeniería de Software II (UPTC). El reparto sigue los dominios del enunciado.

| Integrante | Dominio y entregables |
|---|---|
| Juan Camilo Leal (usuario de Git: juanlealr) | Admisiones (HU-01, HU-02, HU-03, HU-07, HU-09, HU-10 y su parte de HU-08), autenticación y gestión de usuarios, API Gateway, Eureka, servicio de consultas y el frontend web de autenticación y admisiones |
| Bryan Nicolas Sanchez Vargas | Farmacia: solicitudes y despacho de medicamentos (HU-06), con verificación de cobertura y eventos de farmacia |
| Julian Cabra Coy | Laboratorio (HU-04 y HU-05) |

## Estado de los entregables

- Admisiones, autenticación, gateway, eureka y consultas están integrados y en funcionamiento.
- Farmacia está implementado, valida JWT como el resto y publica eventos que consume el servicio de consultas. Tiene bandeja de farmacia en el frontend.
- Laboratorio está implementado (HU-04 y HU-05, REST en 8082 y gRPC en 9091, con sus pruebas) y ya lo consume el alta médica. Falta su pantalla y que sus eventos se proyecten en el consolidado.
