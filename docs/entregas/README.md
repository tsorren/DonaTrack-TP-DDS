# Índice de Entregas — DonaTrack

> **Matriz Curricular de la Cátedra**  
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5

---

## Mapa de Entregas

| Entrega | Fecha aprox. | Tema principal | Artefactos entregados | Enunciado |
|---------|-------------|----------------|-----------------------|-----------|
| **Entrega 1** | Mayo 2026 | Modelo de Dominio y DDD | Diagrama de clases, Carátula, Roles del equipo, Justificaciones de decisiones | [Enunciado-1.pdf](1/Enunciado-1.pdf) |
| **Entrega 2** | Julio 2026 | Comunicación entre servicios y Shared Kernel | Diagramas de integración, ADRs de arquitectura base, imprimibles | [Enunciado-2.pdf](2/Enunciado-2.pdf) |
| **Entrega 3** | Julio 2026 | Logística, Notificaciones y mensajería AMQP | Eventos RabbitMQ, ciclo de vida de entregas y rutas, integración Donaciones ↔ Notificaciones | [Enunciado-3.pdf](3/Enunciado-3.pdf) |
| **Entrega 4** | Semana del 14 de Septiembre 2026 | Persistencia con Spring Data JPA y PostgreSQL | [Principios arquitectónicos (Fase 0)](../entrega-4/arquitectura/principios.md), [Documento de arquitectura](../entrega-4/arquitectura/arquitectura-sistema.md), [Diagrama de componentes](../arquitectura/diseno/diagrama-de-componentes.png), Esquemas multi-tenant, Flyway migrations, Testcontainers, ADRs de persistencia | [Enunciado-4.pdf](4/Enunciado-4.pdf) |
| **Entrega 5** | 30 de Octubre 2026 | Persistencia políglota: relacional con ORM por servicio, documental con ODM para Logística, desnormalización | En curso en la rama `ENTREGA_5` | [Enunciado-TP-Anual-2026.pdf](Enunciado-TP-Anual-2026.pdf) (pág. 25) |
| **Entrega 6** | 13 de Noviembre 2026 | Interfaz de Usuario y Arquitectura MVC | — | [Enunciado-TP-Anual-2026.pdf](Enunciado-TP-Anual-2026.pdf) (pág. 26) |
| **Entrega 7** | 27 de Noviembre 2026 | Observabilidad y Seguridad | — | [Enunciado-TP-Anual-2026.pdf](Enunciado-TP-Anual-2026.pdf) (pág. 28) |

---

## Notas

- Los archivos PDF son los enunciados oficiales de la cátedra; no modificar.
- Los diagramas y artefactos de diseño de cada entrega se encuentran en sus subdirectorios (`1/diagramas/`, `1/interfaz/`).
- El enunciado completo y final del TP Anual 2026 (entregas 1 a 7) está en [Enunciado-TP-Anual-2026.pdf](Enunciado-TP-Anual-2026.pdf). La versión final pasa de 6 a 7 entregas: lo que antes era la Entrega 5 (Web MVC) ahora es la Entrega 6, y Observabilidad y Seguridad pasa a la Entrega 7.
  - **Entrega 5** — Persistencia: persistencia políglota (relacional con ORM por servicio; documental con ODM para Logística) y desnormalización.
  - **Entrega 6** — Interfaz de Usuario y Arquitectura MVC: cliente liviano desacoplado con template engine, servicio de autenticación con SSO y Social-Login — ver [`cliente-liviano/README.md`](../../cliente-liviano/README.md) y [`auth-service/README.md`](../../auth-service/README.md).
  - **Entrega 7** — Observabilidad y Seguridad: métricas/trazas/logs, monitoreo con autorestart, rate limiting, y gRPC o GraphQL.
