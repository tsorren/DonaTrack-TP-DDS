# Bitácoras de Diseño y Planes de Refactorización — DonaTrack

> **Memoria Técnica de Oleadas de Refactorización, Diagramas y Anexos**  
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> Alineación con [`AGENTS.md §8`](../../../AGENTS.md) y [`docs/arquitectura/README.md`](../README.md)

---

## 1. Propósito

Este directorio resguarda el historial evolutivo, planes de refactorización quirúrgica por oleadas y diagramas técnicos del monorepo DonaTrack. Registra el paso a paso de cómo los servicios evolucionaron desde implementaciones monolíticas iniciales hacia una arquitectura orientada al dominio (DDD), desacoplada y tipada.

```text
docs/arquitectura/diseno/
├── README.md                              # Este índice de navegación
├── auditoria-final-proyecto.md            # 🏁 Auditoría integral de cierre tras oleadas de refactor
├── plan-generico-refactor-servicios.md    # 📋 Protocolo genérico de refactor por oleadas (v1)
├── plan-refactor-oleadas-generico-v2.md   # 📋 Protocolo genérico mejorado de refactor por oleadas (v2)
│
├── donaciones/                            # 📦 Refactorización de donaciones-service
├── incentivos/                            # 🏆 Refactorización de incentivos-service
├── logistica/                             # 🚚 Refactorización de logistica-service (Oleadas 1 a 7)
├── notificaciones/                        # 🔔 Refactorización de notificaciones-service
│
├── common/                                # 🎨 donatrack-style.puml (estilos visuales compartidos)
└── anexos-tecnicos/                       # ⚙️ Modelos técnicos y diagramas autogenerados
```

---

## 2. Metodologías y Planes Maestros

* [**Auditoría Final del Proyecto (`auditoria-final-proyecto.md`)**](auditoria-final-proyecto.md):  
  Balance global de cumplimiento, matrices de endpoints, cobertura de tests y verificación de deuda técnica tras la finalización de las oleadas de refactorización en todos los microservicios.
* [**Protocolo Genérico de Refactorización (v2) (`plan-refactor-oleadas-generico-v2.md`)**](plan-refactor-oleadas-generico-v2.md):  
  Metodología estándar para acometer refactorizaciones complejas preservando invariantes: baseline previo obligatorio, preservación estricta de contratos, ciclos TDD y suite de regresión.
* [**Protocolo Genérico de Refactorización (v1) (`plan-generico-refactor-servicios.md`)**](plan-generico-refactor-servicios.md):  
  Especificación fundacional del ciclo de refactorización por oleadas.

---

## 3. Bitácoras y Planes por Microservicio

| Servicio | Bitácoras y Planes | Hitos Principales |
|---|---|---|
| **Donaciones** | [`donaciones/plan-implementacion-refactor-donatrack-donaciones.md`](donaciones/plan-implementacion-refactor-donatrack-donaciones.md)<br>[`donaciones/oleadas-refactor.md`](donaciones/oleadas-refactor.md)<br>[`donaciones/decisiones_futuras_en_oleada_10.md`](donaciones/decisiones_futuras_en_oleada_10.md) | Máquina de 7 estados en `DonacionIndependiente`, extracción de `GestorPropuestasDeAsignacion`, normalización semántica con alias. |
| **Logística** | [`logistica/plan-refactor-logistica-service.md`](logistica/plan-refactor-logistica-service.md)<br>[`logistica/auditoria-final.md`](logistica/auditoria-final.md)<br>Bitácoras: [`Oleada 1`](logistica/bitacora-oleada-1.md) · [`2`](logistica/bitacora-oleada-2.md) · [`3`](logistica/bitacora-oleada-3.md) · [`4`](logistica/bitacora-oleada-4.md) · [`5`](logistica/bitacora-oleada-5.md) · [`6`](logistica/bitacora-oleada-6.md) · [`7`](logistica/bitacora-oleada-7.md)<br>[`logistica/lucid/analisis.md`](logistica/lucid/analisis.md) | Ciclo de vida de entregas (`PENDIENTE`, `EN_CURSO`, `COMPLETADA`, `FALLIDA`), desacoplamiento de rutas con RabbitMQ, eliminación de acoplamiento estático. |
| **Incentivos** | [`incentivos/plan-refactor-incentivos.md`](incentivos/plan-refactor-incentivos.md)<br>[`incentivos/oleadas-refactor.md`](incentivos/oleadas-refactor.md)<br>[`incentivos/decisiones_futuras_en_oleada_10.md`](incentivos/decisiones_futuras_en_oleada_10.md) | Template Method en jerarquía de Misiones, extracción de `MisionMapper`, cálculo de rankings con SQL y persistencia. |
| **Notificaciones** | [`notificaciones/plan-oleadas-notificaciones.md`](notificaciones/plan-oleadas-notificaciones.md)<br>[`notificaciones/fase-0-auditoria.md`](notificaciones/fase-0-auditoria.md)<br>[`notificaciones/auditoria-final.md`](notificaciones/auditoria-final.md)<br>[`notificaciones/decisiones_futuras_en_oleada_10.md`](notificaciones/decisiones_futuras_en_oleada_10.md) | Despacho asíncrono no bloqueante, adaptadores Twilio/SendGrid, transactional inbox para idempotencia y soporte inicial JPA con Flyway. |

---

## 4. Recursos Compartidos y Anexos Técnicos

* [**Estilos Compartidos (`common/donatrack-style.puml`)**](common/donatrack-style.puml):  
  Paleta de colores, estereotipos y convenciones visuales para diagramas de arquitectura PlantUML.
* [**Anexos Técnicos (`anexos-tecnicos/README.md`)**](anexos-tecnicos/README.md):  
  Modelos de clases y paquetes de build autogenerados mecánicamente por el plugin de Maven (`plantuml-generator`).

---

## 5. Navegación Rápida

* [Volver a Arquitectura (`docs/arquitectura/README.md`)](../README.md)
* [Volver al Índice General (`docs/README.md`)](../../README.md)
