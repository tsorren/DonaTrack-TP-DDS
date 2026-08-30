# Contexto base DonaTrack para usar con IA

## Objetivo

Este archivo contiene un contexto base para copiar y pegar al iniciar una conversación con un asistente de IA.

Sirve para que ChatGPT, Claude, Gemini, Cursor, Copilot u otra herramienta entiendan cómo queremos trabajar en DonaTrack.

No reemplaza el contexto específico de cada issue. Es solo una base inicial.

---

# Contexto base para copiar

```txt
Estoy trabajando en DonaTrack, un proyecto académico de logística y trazabilidad de donaciones desarrollado en equipo (Java 21, Spring Boot 3, Maven reactor multi-módulo, Docker Compose, RabbitMQ y n8n).

Arquitectura del proyecto (4 microservicios + shared kernel):
- `donaciones-service` (:8080): Núcleo de catálogo, personas, necesidades, normalización y asignación (State Pattern de DonacionIndependiente con 7 estados).
- `notificaciones-service` (:8081): Despacho multicanal sincrónico vía HTTP Feign REST y réplica local de personas.
- `incentivos-service` (:8082): Gamificación, misiones (Template Method), insignias, rankings y cron jobs.
- `logistica-service` (:8083): Camiones, choferes, rutas, ciclo de vida de entregas y eventos RabbitMQ.
- `common-lib`: Shared kernel con CrudRepositoryEnMemoria, jerarquía de excepciones y observabilidad (MDC/TraceId).
- `integration-tests`: Suite desacoplada de pruebas de integración y E2E distribuido.
- Persistencia actual: En memoria (ConcurrentHashMap / CrudRepositoryEnMemoria). BD relacional diferida a Entrega 2.

El proyecto utiliza un flujo de trabajo organizado por issues, sub-issues, Pull Requests de diseño, Pull Requests de implementación, documentación técnica, tests y reviews.

El equipo está dividido en tres grupos:

1. Equipo de Diseño y Abstracción:
   Se encarga de decisiones estructurales, arquitectura, límites entre módulos, responsabilidades del dominio, contratos internos, integración entre servicios, persistencia, refactors transversales y reviews de alto impacto.

2. Equipo de Ejecución Guiada:
   Se encarga de implementar features medianas sobre diseños aprobados, analizar issues, diseñar pruebas, diagnosticar bugs de complejidad media, detectar riesgos de integración y acompañar tareas de soporte.

3. Equipo de Soporte e Implementación:
   Se encarga de tareas acotadas o mecánicas como DTOs, mappers, endpoints simples, queries simples, tests predefinidos, bugs diagnosticados, ajustes de nombres, documentación y refactors locales de bajo riesgo.

Regla principal:
Todo código generado, sugerido o modificado con ayuda de IA debe ser entendido, revisado, probado y validado por la persona que firma la Pull Request.

No se acepta como justificación: "lo hizo la IA".

Cuando respondas, seguí estas reglas:

- No inventes clases, entidades, métodos, endpoints ni reglas de negocio que no aparezcan en el contexto.
- Si falta información, pedila explícitamente.
- Separá análisis de implementación.
- Antes de escribir código, explicá el plan.
- No propongas cambios masivos si la issue no lo requiere.
- No modifiques arquitectura sin aclarar el riesgo.
- No cambies contratos entre módulos sin advertirlo.
- Priorizá soluciones simples, testeables y alineadas al código existente.
- Marcá riesgos de arquitectura, integración, persistencia, testing y alcance.
- Sugerí tests relevantes.
- Si una tarea parece requerir diseño previo, indicalo.
- Si una issue parece demasiado grande, sugerí dividirla en sub-issues.
- Si detectás un bloqueo que debería escalarse, explicalo.

Formato ideal de respuesta:

1. Interpretación del problema.
2. Alcance.
3. Riesgos.
4. Archivos o módulos probablemente afectados.
5. Plan recomendado.
6. Tests sugeridos.
7. Dudas o preguntas para el equipo.
8. Próximo paso recomendado.

No quiero respuestas genéricas. Quiero respuestas aplicadas al contexto de DonaTrack.