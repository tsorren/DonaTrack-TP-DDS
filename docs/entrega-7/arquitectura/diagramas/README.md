# Diagramas de arquitectura de la Entrega 7 (borrador)

Borrador temprano de los diagramas de E7 (tarea A1, decisión Q14 de
[requerimientos-e7.md](../../../requerimientos/requerimientos-e7.md)). Sirve de acuerdo del grupo y
de referencia para los ADR; la versión final se hace al cierre de E7. Muestra el **estado objetivo**:
lo marcado «planificado» está decidido en los ADR de E7 y todavía no existe en el código.

| Figura | Archivo | Qué responde |
|---|---|---|
| 1 | [`01-componentes.puml`](01-componentes.puml) · [PNG](01-componentes.png) | Qué piezas tiene DonaTrack y cómo se hablan: gateway, Keycloak, MinIO, cliente liviano, Redis y el stack de observabilidad |
| 2 | [`02-despliegue.puml`](02-despliegue.puml) · [PNG](02-despliegue.png) | Dónde corre cada pieza: el cluster kind, sus tres namespaces y lo que vigila la salud. Solo Kubernetes (Q14b): Compose, la demo pública y el túnel no se dibujan |

## ADR que reflejan

- [Kubernetes local con kind y Kustomize](../../../adr/20261117-despliegue-en-kubernetes-local-con-kind-y-kustomize.md): Ingress, namespaces, probes, reinicio y recursos.
- [Stack de observabilidad](../../../adr/20261117-stack-de-observabilidad-con-prometheus-tempo-loki-y-grafana.md): Prometheus, Tempo, Loki con Alloy y Grafana.
- [Rate limiting en el gateway](../../../adr/20261115-rate-limiting-en-el-api-gateway-por-cuenta-o-por-ip.md): dos réplicas y Redis con *fail-open*.
- [GraphQL de solo lectura en Donaciones](../../../adr/donaciones-service/20261117-graphql-de-solo-lectura-en-donaciones.md): `/api/donaciones/graphql` detrás del gateway.

Los nombres de los namespaces (`donatrack`, `donatrack-infra` y `donatrack-observabilidad`) son los
que crea el esqueleto del cluster (tarea K1).

## Regenerar

Las figuras usan el estilo de E4 (`../../../entrega-4/arquitectura/diagramas/estilo-e4.iuml`) y el
layout `smetana`, así que no necesitan Graphviz. Con PlantUML 1.2024.7, la versión del CI:

```bash
java -jar plantuml.jar -failfast2 -tpng docs/entrega-7/arquitectura/diagramas/*.puml
```

Q14a pedía usar el job `generate-diagrams`, pero ese job está apagado (`enable-diagrams`) y solo
dibuja el modelo de clases que genera Maven por servicio. En su lugar, el job hermano
`render-docs-diagrams` del CI renderiza los `.puml` de `docs/entrega-*` cuando un PR los cambia,
falla si alguno no compila y deja los PNG como artefacto. El PNG del repo se genera a mano y se
commitea: la tipografía del runner no es la misma, así que no se compara byte a byte.

Ancho del borrador: 1218 px (Figura 1) y 1174 px (Figura 2), pensado para leer en pantalla. Impreso a
17 cm de ancho, la letra de 12 px queda en unos 5 pt. La versión final tiene que quedar en 7 pt o más,
que pide unos 800 px o menos (las figuras del entregable de E4 usaron 730 px): partir cada figura o
sacar detalle a una tabla.
