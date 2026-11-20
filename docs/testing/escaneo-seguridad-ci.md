# Escaneo de dependencias e imágenes con Trivy en el CI

> E7, tarea S2 (E7-S05d). Decisión D4 del plan de W1, aprobada el 2026-11-18: Trivy.
> Sin ADR propio, por decisión del plan (D4): los pasos se suman al pipeline del
> [ADR del pipeline unificado](../adr/20260426-pipeline-unificado-de-cicd-y-automatizacion-de-calidad.md),
> que no nombra Trivy ni el escaneo de vulnerabilidades. La base de la herramienta nueva es la aprobación de D4.
> Archivo: [`.github/workflows/main.yml`](../../.github/workflows/main.yml). Excepciones: [`.trivyignore`](../../.trivyignore).

## 1. Qué se escanea

| Escaneo | Job | Qué mira | Cuándo corre |
|---|---|---|---|
| Dependencias | `dependency-scan` | `trivy fs` sobre el repo: los `pom.xml` y cualquier otro manifiesto de dependencias | En todo PR |
| Imágenes Java | `docker-build` (un paso por servicio de la matriz) | `trivy image` sobre la imagen `pr-<n>` recién publicada en GHCR | Cuando la matriz Docker tiene servicios |
| Imagen de Keycloak | `auth-service` | `trivy image` sobre `auth-service:pr-<n>` | Cuando cambió `auth-service/` o hubo un cambio global |

Los tres usan solo el escáner de vulnerabilidades (`scanners: vuln`), no el de secretos.

## 2. Corte y reporte

Cada escaneo corre dos veces Trivy:

1. **Reporte.** Todas las severidades, en SARIF, sin cortar (`exit-code: 0`). El SARIF sube a la pestaña
   de seguridad del repo (*Security → Code scanning*) con una categoría propia: `trivy-dependencias`,
   `trivy-imagen-<servicio>` y `trivy-imagen-auth-service`. El repo es público, así que la pestaña acepta SARIF.
2. **Corte.** Solo `CRITICAL` que tenga arreglo publicado (`ignore-unfixed: true`), menos lo aceptado en
   `.trivyignore`. Si encuentra uno, el paso falla (`exit-code: 1`).

El reporte va antes del corte: el SARIF se publica aunque el corte falle. En `docker-build` y `auth-service`
el escaneo corre después del push, así que la imagen `pr-<n>` queda publicada en GHCR aunque el corte falle;
lo que se frena es el resto del pipeline (ver abajo).

Dónde se ve `[INFERRED]`, a confirmar en el primer CI: `main.yml` corre solo en PRs, así que los
resultados quedan asociados a la referencia del PR. Se ven en la pestaña de seguridad filtrando por
`pr:<n>` y en los checks del PR, no en la vista por defecto de la rama principal. GitHub puede agregar al
PR su propio check de *code scanning*, con un umbral que se configura en el repo y no es el corte de
Trivy. Ese check no entra al `quality-gate`.

Qué pasa si corta:

- `dependency-scan` falla y el `quality-gate` lo marca: es obligatorio en todo PR (`always`).
- Un corte de imagen hace fallar `docker-build` o `auth-service`; con eso `preprod-validation` no corre y el
  `quality-gate` falla.

Solo los tres jobs que suben SARIF tienen `security-events: write`.

## 3. Qué dispara el escaneo de imágenes

El filtro global de `detect-changes` incluye `.github/workflows/main.yml`. Un cambio del pipeline
reconstruye y escanea todas las imágenes, y además enciende `any-service` y `auth-changed`: corren
build, tests, Sonar (si corresponde) y el preprod completos. Antes, un PR que tocaba solo `.github/**`
dejaba la matriz Docker vacía. Los otros workflows (`merge.yml` y el resto) no entran al filtro.

**Cambio de comportamiento del CI (desde S2).** Un PR que toca `main.yml` corre el pipeline completo, más
largo que antes. Por los pasos de Trivy, el tope de `docker-build` sube de 15 a 25 minutos y el de
`auth-service`, de 25 a 35.

## 4. Excepciones en `.trivyignore`

Un crítico que ya estaba, o que no se puede arreglar en el PR, entra a `.trivyignore`: una línea con el
identificador y un comentario arriba con fecha, motivo y la deuda en
[`DEUDA_TECNICA.md`](../adr/DEUDA_TECNICA.md). No se apaga el paso. El corte lo pide explícito
(`trivyignores`). `[INFERRED]`, por la documentación de Trivy y a confirmar en el primer CI: el reporte
también lo aplica, porque Trivy lee `.trivyignore` de la raíz por defecto, así que lo aceptado no aparece
en la pestaña de seguridad.

Al 2026-11-19 no hay entradas: el primer escaneo real es el CI del PR de S2.

## 5. Acciones fijadas

| Acción | Versión | Commit |
|---|---|---|
| `aquasecurity/trivy-action` | v0.36.0 (último release al 2026-11-19) | `ed142fd0673e97e23eac54620cfb913e5ce36c25` |
| `github/codeql-action/upload-sarif` | v4.39.3 (último tag `v4.*` al 2026-11-19) | `33d7b85da1a04171e211befe2c739303b9a4c066` |

Desvío del pedido, que decía "último release": en `github/codeql-action`, `releases/latest` devuelve
`codeql-bundle-v2.27.5`, que es el paquete de CodeQL y no una versión de la acción. Por eso se fijó el
último tag `v4.*`.

Los dos commits salen del tag (desreferenciado si es anotado) y pertenecen a la rama por defecto de la
acción (`gh api repos/<acción>/compare/<rama>...<sha>` da `behind`). Leído en el `action.yaml`
de trivy-action en el commit fijado (se confirma en el primer CI): los inputs usados existen
(`trivyignores`, `skip-setup-trivy`, `ignore-unfixed`, `timeout`, entre otros) y la acción instala
Trivy v0.70.0 (valor por defecto de `version`) con `aquasecurity/setup-trivy` fijado por SHA. El segundo
uso de cada job no la reinstala (`skip-setup-trivy: true`). `[INFERRED]`: reusa la base de
vulnerabilidades del primero, porque los dos usan el mismo `cache-dir` por defecto.

## 6. Cómo correrlo local

Con Trivy instalado y Docker:

```bash
trivy fs --scanners vuln --severity CRITICAL --ignore-unfixed .
trivy image --scanners vuln --severity CRITICAL --ignore-unfixed ghcr.io/<repo>/<servicio>:pr-<n>
```

La primera corrida descarga la base de vulnerabilidades. La validación que cuenta es la del CI.
