# auth-service — Bounded Context Reservado

> **Estado:** 🔒 `DEFERRED` — Reservado para entrega futura  
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5

---

## Estado actual

`auth-service` **no está implementado**. Existe únicamente como directorio placeholder con `.gitkeep`.  
El microservicio está formalmente registrado en [`.log4brains.yml`](../.log4brains.yml) y su carpeta de ADRs (`docs/adr/auth-service/`) está reservada.

**Hito canónico de construcción:** Entrega 6 — Despliegue, Observabilidad y Seguridad (Semana del 23 de Noviembre 2026).

---

## Responsabilidades planificadas

Este bounded context será responsable de:

- **Autenticación:** Emisión y validación de tokens de acceso (JWT u equivalente)
- **Autorización:** Control de acceso basado en roles/permisos para los microservicios del sistema
- **Gestión de usuarios:** Ciclo de vida de cuentas, credenciales y perfiles
- **Key Broker centralizado:** Custodia del ciclo de vida de claves simétricas (DEK — *Data Encryption Key*) para el esquema de Crypto-Shredding de datos personales

---

## ADRs relevantes

| ADR | Descripción | Estado |
|-----|-------------|--------|
| [DTI-07 — Dependencia diferida de auth-service para Key Broker](../docs/adr/notificaciones-service/20260902-dti-07-dependencia-diferida-de-auth-service-para-key-broker.md) | Formaliza la deuda técnica de la ausencia de auth-service y la solución interina `LocalKeyBrokerAdapter` en `notificaciones-service` | `proposed` |
| [Protección de PII: Crypto-Shredding y desacoplamiento de mensajes](../docs/adr/notificaciones-service/20260902-proteccion-de-pii-crypto-shredding-y-desacoplamiento-de-mensajes.md) | Arquitectura de referencia que establece a auth-service como Centralized Key Broker para Derecho al Olvido en O(1) | `proposed` |

> Ver también: [`docs/adr/DEUDA_TECNICA.md — DTI-07`](../docs/adr/DEUDA_TECNICA.md) para el tracking formal de esta deuda.

---

## Solución interina (mientras auth-service no exista)

Mientras este microservicio no esté disponible, `notificaciones-service` utiliza `LocalKeyBrokerAdapter`, que implementa el puerto `KeyBrokerClient` definido en `common-lib`. La migración a `RemoteAuthKeyBrokerClient` se realizará mediante `@Profile` o `@ConditionalOnProperty` sin cambios en el dominio ni en los mappers.

---

## No agregar código a este directorio

Este directorio no debe contener código fuente hasta que el equipo decida iniciar la Entrega 6. Cualquier decisión de diseño previa debe documentarse como ADR en `docs/adr/auth-service/`.
