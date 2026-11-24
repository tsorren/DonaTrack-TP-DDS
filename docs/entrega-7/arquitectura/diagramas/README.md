# Diagramas de arquitectura de la Entrega 7 (borrador)

Borrador temprano de los diagramas de E7 (tarea A1, decisión Q14 de
[requerimientos-e7.md](../../../requerimientos/requerimientos-e7.md)). Sirve de acuerdo del grupo y
de referencia para los ADR; la versión final se hace al cierre de E7. Muestra el **estado objetivo**:
lo marcado «planificado» está decidido en los ADR de E7 y todavía no existe en el código.

Rehecho el 2026-11-18 por la revisión de simplificación (E7-S05b A): Docker Swarm de un nodo en
lugar de Kubernetes (E7-S01), la imagen todo en uno `grafana/otel-lgtm` en lugar de cuatro
contenedores (E7-S03) y sin proxy delante (E7-S02b A).

| Figura | Archivo | Qué responde |
|---|---|---|
| 1 | [`01-componentes.puml`](01-componentes.puml) · [PNG](01-componentes.png) | Qué piezas tiene DonaTrack y cómo se hablan: gateway, Keycloak, MinIO, cliente liviano, Redis y la observabilidad en `otel-lgtm`, con las señales por OTLP |
| 2 | [`02-despliegue.puml`](02-despliegue.puml) · [PNG](02-despliegue.png) | Dónde corre cada pieza: un nodo de Swarm, los seis módulos propios con réplicas por variable, la infraestructura, Redis y `otel-lgtm` con una réplica, qué se publica y qué vigila la salud. Solo el stack de Swarm: Compose (desarrollo, CI, demo pública y túnel) no se dibuja |

## ADR que reflejan

- [Despliegue en Docker Swarm de un nodo, con réplicas](../../../adr/20261118-despliegue-en-docker-swarm-de-un-nodo-con-replicas.md): réplicas, publicación de puertos, *healthcheck* y reemplazo de tareas.
- [Observabilidad con OpenTelemetry y `otel-lgtm`](../../../adr/20261118-observabilidad-con-opentelemetry-y-grafana-otel-lgtm.md): OTLP desde los módulos, la imagen todo en uno y la alerta de reinicios.
- [Rate limiting en el gateway](../../../adr/20261115-rate-limiting-en-el-api-gateway-por-cuenta-o-por-ip.md): Redis compartido por las réplicas, con *fail-open*.
- [GraphQL de solo lectura en Donaciones](../../../adr/donaciones-service/20261117-graphql-de-solo-lectura-en-donaciones.md): `/api/donaciones/graphql` detrás del gateway.

Los recuadros de la Figura 2 agrupan por función y son solo visuales: Swarm no tiene namespaces. Lo
que el ADR de Swarm deja abierto no se dibuja como decidido: si el gateway se publica va marcado
«¿publicado? (abierto)», y no se dibujan el nombre del stack, las redes ni el puerto de Grafana.

## Regenerar

Las figuras usan el estilo de E4 (`../../../entrega-4/arquitectura/diagramas/estilo-e4.iuml`) y el
layout `smetana`, así que no necesitan Graphviz:

```bash
java -jar plantuml.jar -failfast2 -tpng docs/entrega-7/arquitectura/diagramas/*.puml
```

Los PNG de este borrador se generaron con PlantUML 1.2024.3. El CI usa la 1.2024.7.

Q14a pedía usar el job `generate-diagrams`, pero ese job está apagado (`enable-diagrams`) y solo
dibuja el modelo de clases que genera Maven por servicio. En su lugar, el job hermano
`render-docs-diagrams` del CI renderiza los `.puml` de `docs/entrega-*` cuando un PR los cambia,
falla si alguno no compila y deja los PNG como artefacto. El PNG del repo se genera a mano y se
commitea: la tipografía del runner no es la misma, así que no se compara byte a byte.

Ancho del borrador: 998 px (Figura 1) y 1105 px (Figura 2), pensado para leer en pantalla. Impreso a
17 cm de ancho, la letra de 12 px queda en unos 5,8 pt (Figura 1) y 5,2 pt (Figura 2). La versión
final tiene que quedar en 7 pt o más, que pide unos 800 px o menos (las figuras del entregable de E4
usaron 730 px): partir cada figura o sacar detalle a una tabla.
