# Gobernanza técnica

Este documento describe cómo se toman y se mantienen las decisiones técnicas del proyecto, según lo que existe en el repositorio.

**Propiedad por servicio.** Cada servicio tiene un responsable y su propio módulo Maven, con su base de datos. Nadie lee la base de otro servicio: la información se comparte solo por la API del servicio o por eventos.

**Contratos entre servicios.** Los eventos de Kafka son mensajes JSON con campos nombrados y estables (por ejemplo `admisionId`, `pacienteId`, `sedeDestinoId`). Los consumidores ignoran campos desconocidos y son idempotentes, de modo que un cambio compatible no obliga a desplegar todo a la vez. Un cambio incompatible se acuerda entre los dueños del productor y del consumidor.

**Decisiones de arquitectura.** Las decisiones importantes se registran como ADR en `docs/cierre/ARQUITECTURA.md`, con alternativas consideradas y consecuencias. Un cambio que contradiga un ADR exige actualizarlo.

**Reglas de dependencia.** El dominio y la aplicación no importan clases de infraestructura; las integraciones externas entran por puertos. Esto se revisa en cada cambio al núcleo.

**Seguridad.** Los permisos por rol se definen en el gateway y se repiten en cada servicio. El secreto JWT se lee de la variable `JWT_SECRET` y no debe subirse a producción con el valor de desarrollo.

**Calidad.** Cada servicio incluye pruebas automáticas que se ejecutan con `mvnw test` sin necesidad de infraestructura externa.
