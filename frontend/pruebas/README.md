# Pruebas de interfaz

Prueba automática que recorre el flujo clínico de laboratorio **sobre la interfaz real**,
haciendo clic como lo haría una persona.

```bash
cd frontend
npm install        # solo la primera vez
npm run test:flujo
```

Termina con `43 comprobaciones ok, 0 fallos` y devuelve código de salida 0, así que sirve en CI.
No necesita Docker, ni base de datos, ni los microservicios encendidos.

## Qué comprueba

| Bloque | Qué recorre |
|---|---|
| HU-04 | El médico elige un paciente admitido, escribe el examen y crea la orden |
| HU-09 | Intenta el alta y el backend la rechaza con 409; la admisión sigue activa |
| HU-05 | El laboratorio registra el resultado y cierra la orden |
| HU-09 | Con la orden cerrada, el alta se concede |
| Permisos | Cada rol solo entra donde el gateway lo permite |
| Consolidado | El estado del paciente muestra sus exámenes |
| Regresión | Farmacia y admisiones siguen funcionando |

Verifica además una regla fácil de romper: una orden `CON_RESULTADO` **sigue bloqueando el alta**.
Solo el cierre la libera.

## Cómo funciona

| Archivo | Para qué sirve |
|---|---|
| `flujo-laboratorio.mjs` | La prueba: los pasos y las comprobaciones |
| `gateway-falso.mjs` | Backend simulado con las reglas de roles y los códigos reales |
| `entorno.mjs` | Monta la app en un navegador de mentira (jsdom) y expone `click`, `escribir`… |
| `entrada.jsx` | Punto de entrada que esbuild empaqueta: importa `src/App.jsx` tal cual |

La aplicación que se prueba es la de `src/`, sin copias ni adaptaciones: si una pantalla se rompe,
la prueba falla.

## Límites

`gateway-falso.mjs` **imita** las reglas del backend, no las ejecuta. Si cambian los roles del
gateway, los códigos de error o el contrato de `/api/laboratorio/**`, hay que actualizarlo a mano;
si no, la prueba seguirá en verde mientras el sistema real falla.

Esta prueba no sustituye a las de los microservicios (`./mvnw test` en cada módulo), que sí
ejercitan la lógica del backend.
