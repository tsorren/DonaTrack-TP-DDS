# Kubernetes local de DonaTrack (E7)

Esqueleto del cluster (tarea K1) según el
[ADR de Kubernetes con kind y Kustomize](../docs/adr/20261117-despliegue-en-kubernetes-local-con-kind-y-kustomize.md).
Compose sigue siendo el entorno de desarrollo, del CI y de la demo pública mientras se migra.

## Estructura

| Ruta | Qué tiene | Quién la completa |
|---|---|---|
| `kind/cluster.yaml` | Cluster `donatrack` de un nodo, imagen fijada por digest, puertos 80 y 443 publicados en `127.0.0.1` y la etiqueta `ingress-ready` | K1 (hecho) |
| `base/` | Los manifiestos comunes a todos los entornos. Hoy, los tres namespaces | K2, K4, O1 a O4 |
| `overlays/local/` | Lo que cambia en el cluster kind; hoy solo toma la base | K2, K4, K5 |

Namespaces, uno por función:

| Namespace | Qué corre |
|---|---|
| `donatrack` | Los cuatro servicios, el api-gateway (dos réplicas) y el cliente liviano (K4) |
| `donatrack-infra` | Postgres, Mongo, RabbitMQ, Keycloak, MinIO, n8n y Redis (K2) |
| `donatrack-observabilidad` | Prometheus, Tempo, Loki, Alloy y Grafana (O1 a O4) |

Cada manifiesto nuevo va en `base/` (o en una subcarpeta por pieza) y se suma a `resources` de su
`kustomization.yaml`, con su namespace en `metadata.namespace`.

## Requisitos

- Docker en marcha. Para el sistema completo con observabilidad hacen falta unos 16 GB de memoria
  (ADR); el esqueleto solo necesita el nodo.
- [kind](https://kind.sigs.k8s.io/) v0.33.0 o posterior y `kubectl` (trae Kustomize; Docker Desktop
  lo instala).
- Los puertos 80 y 443 de la máquina libres.

## Uso

Crear el cluster y aplicar el entorno local:

```bash
kind create cluster --config k8s/kind/cluster.yaml
kubectl apply -k k8s/overlays/local
kubectl get namespaces -l app.kubernetes.io/part-of=donatrack
```

Ver lo que se aplicaría, sin cluster:

```bash
kubectl kustomize k8s/overlays/local
```

Borrar el cluster:

```bash
kind delete cluster --name donatrack
```
