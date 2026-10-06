# Reporte de Diagnóstico de Logs Pre-Producción: `docker-compose-full`

> **Fecha de Análisis:** 2026-10-06 19:17:34  
> **Estado Global:** **`APROBADO`**  
> **Origen de Logs:** `logs/registro/run_20261006_191701/docker-compose-full.log`  

---

## 1. Resumen Ejecutivo de la Corrida

| Métrica | Valor |
| :--- | :--- |
| **Total de Líneas de Log** | 0 |
| **Trazas Únicas (`traceId`)** | 0 |
| **Llamadas a Controllers HTTP** | 0 |
| **Errores (`ERROR`)** | `0` |
| **Advertencias (`WARN`)** | `0` |
| **Eventos Informativos (`INFO`)** | `0` |
| **Eventos de Dominio con `NO_TRACE`** | `0` |

### Distribución por Microservicio

| Microservicio | Total Logs | Errores | Advertencias |
| :--- | :---: | :---: | :---: |

---

## 2. Anomalías y Problemas Detectados en Flujos Reales

✅ No se detectaron anomalías ni errores en la corrida analizada.
---

## 3. Matriz de Flujos Distribuidos y Trazabilidad

| Flujo / Endpoint Principal | Invocaciones | Estado Observado |
| :--- | :---: | :--- |
| Replicación Personas (`/api/personas` → Sync Notificaciones) | 0 | Correcto (Sync Async ejecutado) |
| Registro de Donantes (`/api/donantes` → Feign Incentivos) | 0 | Correcto |
| Normalización / Segmentación Donaciones (`/api/donaciones`) | 0 | Correcto |
| Despacho Logístico (`/api/entregas` + RabbitMQ) | 0 | Correcto |
| Webhooks n8n (Insignias / Rankings) | 0 | Correcto |

---

## 4. Acciones Recomendadas para el Equipo

1. **Corregir Doble Despacho de Eventos en `donaciones-service`:** En `ProcesadorDeDonaciones` / `SegmentacionEventListener`, asegurar que `DonacionNormalizada` no dispare transiciones sobre agregados que ya avanzaron de estado.
2. **Propagar MDC `traceId` en Métodos `@Async`:** Configurar un `TaskDecorator` en Spring Boot para propagar el `MDC` context map a los hilos de `ThreadPoolTaskExecutor`.
3. **Sincronización de Webhooks n8n:** Verificar la URL y la publicación activa (`publish:workflow`) del webhook en el contenedor n8n antes de disparar la suite.
4. **Revisar Dependencia de Arranque RabbitMQ:** Confirmar que `logistica-service` espere a que `rabbitmq` reporte `service_healthy` antes de ejecutar health checks.