# Promoción de artefactos por digest y despliegue reproducible de Logística

- Status: proposed
- Date: 2026-10-20
- Deciders: Pendiente de decisión grupal
- Tags: cicd, despliegue, docker, ghcr, render, preprod, promocion, rollback, entrega-6

> **Origen:** análisis "preprod fiel" en tres fases: [criterio y brechas](../testing/preprod-fiel.md) (B2, B3; P1, P12), [arquitectura objetivo](../testing/preprod-fiel-arquitectura-objetivo.md) (§3 B2 y B3, §6) y [roadmap](../testing/preprod-fiel-roadmap.md) (RM-06 y RM-07). Base relevada: `origin/main` en `a808942c`.
>
> **Revisión 2026-10-28** ([SPEC-05](../specs/active/SPEC-05-preprod-fiel-rediseno-y-rm01-rm08.md), D3, aprobada por el usuario): el despliegue deja de depender de Render. Se suma la alternativa **2D** (ensayo del manifiesto en un entorno efímero del runner, con smoke y rollback) como elegida, y Render queda como adaptador opcional. La parte 1 no cambia. `[OBSERVED]` Implementada en la rama `E5_preprod_fiel_rm08` (`merge.yml`: `preprod-post-merge` y `promote`; `deploy.yml`); se valida con la primera corrida post-merge.

## Contexto y Problema

`[OBSERVED]` Hoy el pipeline publica una imagen que ningún preprod validó, y el despliegue no se reproduce desde el repositorio:

* **PR** (`.github/workflows/main.yml`): construye la imagen `pr-<N>` del servicio modificado y corre el preprod con ella. Si no la encuentra, usa la de la rama base con solo un aviso (`main.yml:699-704`). Los servicios no modificados toman el tag mutable `:<base>`. `main.yml` solo corre en `pull_request` (`main.yml:44-46`).
* **Merge** (`.github/workflows/merge.yml`): vuelve a construir desde el commit integrado y publica `:<rama>`, `:sha-<7>` y, en `main`, `:latest` (`merge.yml:259-269`), sin correr el preprod. Sus 5 jobs son `detect-services`, `build-and-verify`, `docker-publish`, `cleanup-pr-tags` y `merge-summary`. Ninguno tiene `timeout-minutes`.
* **Despliegue:** no hay job de despliegue. Logística corre en Render, con PostgreSQL en Neon y RabbitMQ en CloudAMQP, configurados a mano fuera del repo (ADR [despliegue de Logística en PaaS](./logistica-service/20261009-despliegue-de-logistica-en-paas-con-contenedores.md), `proposed`, Consecuencias Negativas). Ese ADR dice que Logística corre "a partir de su imagen Docker (target `ci` o `local`)". El target `local` compila desde el código fuente. No hay `render.yaml`, así que no se sabe si Render construye desde el repo o baja la imagen de GHCR.

Consecuencias:

1. **La imagen publicada no es la validada.** `:sha-<7>`, `:<rama>` y `:latest` salen de un build que no pasó por preprod.
2. **El estado post-merge nunca se prueba.** `[INFERRED]` Dos PR en verde pueden romper la base al combinarse, y el primero en notarlo es el PR siguiente.
3. **Lo desplegado no se puede rastrear hasta un commit validado**, y no hay smoke ni rollback ensayado.

Problema: ¿cómo se garantiza que lo que llega a los tags de producción y a Render sea exactamente lo que validó el preprod, dentro de los runners y los planes gratuitos?

## Atributos de Calidad y Drivers de Decisión

* **Integridad del artefacto:** construir una vez y promover el mismo digest ("Build Once, Deploy Many"). Una imagen por digest es inmutable; un tag no.
* **Rastreabilidad:** de cada despliegue se puede llegar al commit, a la corrida de preprod y al digest de cada servicio.
* **Recuperabilidad:** el rollback es el mismo procedimiento que el despliegue, con la versión anterior.
* **Costo en la plataforma gratuita:** runners de 7 GB, plan gratuito de Render (512 MB, suspensión a los 15 minutos sin tráfico, 750 horas por mes; [Render: free](https://render.com/docs/free)) y la pausa entre defensas que permite el enunciado.
* **Feedback del PR:** no alargar el pipeline de PR.
* **Seguridad:** el menor número de secretos nuevos, en un ámbito restringido.

## Relaciones Arquitectónicas

1. **Extiende:** [20260426-pipeline-unificado-de-cicd-y-automatizacion-de-calidad](./20260426-pipeline-unificado-de-cicd-y-automatizacion-de-calidad.md) (`accepted`). Ese ADR decide la validación por etapas, la protección de ramas y GitHub Pages. No decide cómo se publica ni cómo se despliega un artefacto. Esta decisión no cambia nada de lo que ese ADR fija.
2. **Complementa y precisa:** [logistica-service/20261009-despliegue-de-logistica-en-paas-con-contenedores](./logistica-service/20261009-despliegue-de-logistica-en-paas-con-contenedores.md) (`proposed`). Conserva la plataforma (Render, Neon y CloudAMQP) y la pausa entre defensas (punto 6). Precisa el punto 1: Render corre la imagen de GHCR por digest, no un build propio. Modifica el punto 5: el deploy hook de Render pasa a ser un secreto de GitHub, en un Environment protegido. Si el equipo prefiere registrar esto como reemplazo parcial, la relación `superseded` la aprueba un humano (AGENTS.md §9.3).
3. **Depende de:** [20260903-protocolo-salida-semantico-y-quality-gate-estricto](./20260903-protocolo-salida-semantico-y-quality-gate-estricto.md) (`proposed`). Promover lo que pasó el preprod solo sirve si el preprod no da verdes falsos (DTI-25, RM-02 del roadmap).
4. **Respeta:** [20261009-tolerancia-al-desorden-de-eventos-en-incentivos-y-consistencia-eventual-en-tests](./20261009-tolerancia-al-desorden-de-eventos-en-incentivos-y-consistencia-eventual-en-tests.md) (`proposed`), que descarta correr la suite híbrida en CI. El smoke post-despliegue de esta decisión no es esa suite: se dispara a mano, después de desplegar, y solo comprueba salud, contrato y un ciclo AMQP corto.

## Alternativas Consideradas

La decisión tiene dos partes: cómo se promueve un artefacto (parte 1) y cómo llega a Render (parte 2). Las matrices usan los criterios de la [fase 2](../testing/preprod-fiel-arquitectura-objetivo.md#2-método-de-evaluación): notas de 1 a 5, total = Σ peso × nota / 5, sobre 100. Una alternativa con "Cierra la brecha" ≤ 2 se elimina. Las notas son juicio `[INFERRED]` sobre la evidencia relevada.

### Parte 1 — Promoción

| Alternativa | Veredicto |
|---|---|
| **1A. Preprod post-merge sobre el digest candidato y promoción por re-etiquetado** | ✅ Elegida |
| 1B. Merge queue de GitHub | ❌ Más fuerte, pero más cara de adoptar. Queda como evolución |
| 1C. Re-etiquetar la imagen `pr-<N>` al mergear, con la rama obligada a estar al día | ❌ Solo es exacta si nadie mergeó antes, y no prueba el post-merge |
| Status quo: build del merge publicado sin preprod | ❌ Eliminada: es la brecha |

| Criterio (peso) | 1A | 1B | 1C | Status quo |
|---|:-:|:-:|:-:|:-:|
| Cierra la brecha (35) | 5 | 5 | 3 | 1 |
| Costo en la plataforma gratuita (20) | 3 | 3 | 5 | 5 |
| Superficie de cambio (20) | 4 | 3 | 4 | 5 |
| Feedback del PR (10) | 5 | 3 | 2 | 5 |
| Paridad y compatibilidad (15) | 5 | 5 | 5 | 5 |
| **Total** | **88** | 80 | 76 | eliminada |

### Parte 2 — Despliegue

| Alternativa | Veredicto |
|---|---|
| **2D. Ensayo del manifiesto en un entorno efímero del runner, con smoke y rollback; Render como adaptador opcional** (revisión 2026-10-28) | ✅ Elegida |
| 2A. Workflow manual con un manifiesto de versión, deploy hook por digest, smoke y rollback | Elegida en la versión del 2026-10-20; queda como adaptador opcional de 2D: bloqueada por las precondiciones de Render (RM-01) |
| 2B. Despliegue automático después de cada promoción a `main` | ❌ Despierta y redespliega la instancia en cada merge, contra la pausa entre defensas y las 750 horas del plan |
| 2C. Staging permanente en la nube con la suite híbrida post-merge | ❌ Triplica secretos y cuotas, y el ADR de tolerancia al desorden la descarta |
| Status quo: despliegue manual desde el panel de Render | ❌ Eliminada: no se reproduce desde el repo ni deja rastro |

| Criterio (peso) | 2A | 2B | 2C | Status quo |
|---|:-:|:-:|:-:|:-:|
| Cierra la brecha (35) | 5 | 5 | 5 | 1 |
| Costo en la plataforma gratuita (20) | 5 | 2 | 2 | 5 |
| Superficie de cambio (20) | 4 | 4 | 2 | 5 |
| Feedback del PR (10) | 5 | 5 | 4 | 5 |
| Paridad y compatibilidad (15) | 4 | 4 | 3 | 5 |
| **Total** | **93** | 81 | 68 | eliminada |

## Análisis por alternativa

**1A.** *Pros:* el digest que llega a `:<rama>` y `:latest` es el que pasó el preprod sobre el commit integrado. Cierra el caso de dos PR verdes que rompen la base. No alarga el PR. *Contras:* el preprod corre dos veces por cambio. Si el post-merge falla, la rama queda con una cabeza no promovida hasta que un arreglo pase.

**1B.** *Pros:* valida el resultado combinado antes de integrar. *Contras:* exige el evento `merge_group` en `main.yml` y reglas de rama nuevas, serializa los merges cerca de cada entrega, y su disponibilidad según el tipo de cuenta no se verificó `[INFERRED]`.

**1C.** *Pros:* no agrega corridas. *Contras:* `cleanup-pr-tags` borra los tags `pr-<N>` después del merge (`merge.yml:323`), obliga a rebasear antes de cada merge y no prueba el estado post-merge.

**2A.** *Pros:* el deploy hook de Render acepta el parámetro `imgURL` con tag o digest ([Render: deploying an image](https://render.com/docs/deploying-an-image)) `[DOCUMENTED]`. La imagen de GHCR es pública: se baja sin credenciales (`[VERIFIED]` 2026-10-20, token anónimo y manifiesto con HTTP 200). El despliegue y el rollback son el mismo workflow. *Contras:* un secreto nuevo en GitHub. Depende de que el plan gratuito permita un servicio configurado desde una imagen, algo que la documentación de Render no aclara.

**2B.** *Pros:* nadie se olvida de desplegar. *Contras:* contradice la pausa entre defensas del ADR de PaaS (punto 6) y consume horas del plan sin necesidad.

**2D (revisión 2026-10-28).** *Pros:* no depende de terceros ni de las precondiciones de Render; el mismo manifiesto levanta el mismo set de digests; el rollback se ensaya en cada corrida y solo usa `docker compose`. *Contras:* es el mismo artefacto en otra plataforma, no el despliegue real en la nube. Puntaje de la SPEC-05 (D3): 4,6 contra 3,2 de 2A, 3,0 de otro PaaS.

**2C.** *Pros:* es la más fiel a la nube. *Contras:* una segunda instancia en Render, una rama de Neon y un vhost en CloudAMQP, con sus secretos en CI. Adoptarla pediría un ADR sucesor del de tolerancia al desorden.

## Resultado de la Decisión

Se adoptan **1A** y **2D** (revisión 2026-10-28; la versión del 2026-10-20 adoptaba 2A, que queda como adaptador opcional):

1. **Build único.** `merge.yml` construye una vez desde el commit integrado y publica solo `:sha-<7>`, como **candidato**.
2. **Preprod post-merge.** Corre el preprod con todas las imágenes fijadas por digest, también las de los servicios no modificados, con los estratos del post-merge (S3 a S6, definidos en la [arquitectura objetivo, §5.3](../testing/preprod-fiel-arquitectura-objetivo.md#53-estratos-de-la-suite)).
3. **Promoción.** Si el post-merge da verde, re-etiqueta el mismo digest a `:<rama>` y, en `main`, a `:latest`, con `docker buildx imagetools create`, sin reconstruir. Si falla, no promueve: `:<rama>` sigue apuntando a la última versión validada.
4. **Manifiesto de versión.** Cada promoción publica un JSON con la rama, el commit, la corrida y el digest de cada servicio. Es la unidad de despliegue y de rollback. Por defecto se guarda como artefacto del workflow `[PROPOSED]`: la retención por defecto de 90 días alcanza para el curso `[INFERRED]`.
5. **Preprod de PR con digests.** Los servicios no modificados se resuelven por digest desde el último manifiesto promovido de la rama base, en lugar del tag mutable `:<base>`. Un servicio modificado sin imagen `pr-<N>` hace fallar el job.
6. **Despliegue reproducible (2D).** Un workflow nuevo, `deploy.yml`, corre después de cada `merge.yml` exitoso y a mano (`workflow_dispatch`, con la corrida cuyo manifiesto se quiere desplegar). Levanta en el runner el stack de preprod con los digests del manifiesto M (`scripts/ensayo-despliegue.sh`).
7. **Smoke.** `/actuator/health` en `UP` con una espera que tolera el arranque en frío, `/v3/api-docs` y el grupo `smoke` de `integration-tests`, para los 4 servicios.
8. **Rollback ensayado.** Sobre el mismo stack despliega el manifiesto anterior, M-1, y repite el smoke. Se ensaya en cada corrida.
9. **Render (2A como adaptador opcional).** Solo en la corrida manual y solo si existe el secreto `RENDER_DEPLOY_HOOK_LOGISTICA`: llama al deploy hook con `imgURL=<imagen de Logística>@sha256:<digest>`. Si el secreto no existe, avisa y no falla. Hoy se lee como secreto del repositorio; si se guarda en un GitHub Environment `produccion` con revisores requeridos (configuración fuera del repo), el job `render` tiene que declarar `environment: produccion`.
10. **Pausa.** Se mantiene la pausa entre defensas: Render solo recibe un despliegue a pedido.

**Precondiciones del adaptador de Render** (RM-01; ya no bloquean la parte 2):

* Que el servicio de Logística se pueda configurar con la imagen de GHCR como origen en el plan gratuito.
* Cómo construye hoy: si construye desde el repo, hay que cambiar el origen del servicio.

Si la primera no se cumple, el adaptador queda sin usar; P12 llega igual a 2 por el despliegue reproducible con smoke y rollback (SPEC-05, D3).

Justificación: es la única combinación que lleva P1 y P12 a 2 sin alargar el PR ni salir de las cuotas gratuitas. Aplica "Build Once, Deploy Many": un artefacto inmutable atraviesa todas las etapas. También separa responsabilidades: validar es del post-merge, promover del job de promoción y desplegar del workflow de despliegue, cada uno con un único motivo de cambio.

## Consecuencias Positivas

* Lo que llega a `:<rama>`, a `:latest` y a Render es exactamente lo que validó el preprod.
* El estado post-merge se prueba en cada integración.
* Cada despliegue se rastrea hasta un commit, una corrida y un digest por servicio.
* El rollback es un procedimiento conocido y ensayado.
* El manifiesto es la entrada de la prueba de actualización (RM-15) y del escaneo de imágenes (RM-14).

## Consecuencias Negativas

* El preprod corre dos veces por cambio. Hoy son unos 3 minutos por corrida (roadmap, §1), más el build.
* Entre el merge y la promoción hay una ventana en la que `:<rama>` todavía apunta a la versión anterior.
* Si el post-merge falla, la rama queda con una cabeza no promovida, y el equipo tiene que arreglarla hacia adelante.
* Si se usa el adaptador de Render, hay un secreto nuevo en GitHub: quien tenga la URL del deploy hook puede desplegar. Se mitiga con el Environment `produccion`, con revisores requeridos.
* Un artefacto vencido deja de servir para un rollback. Si hace falta más de 90 días, el manifiesto pasa a guardarse en otro lado (por ejemplo, un asset de release).
* El ensayo (2D) corre en el runner, no en la nube: valida el artefacto y el procedimiento, no la plataforma de Render. El despliegue real solo cubre Logística.
* Cada merge con imágenes suma el ensayo de despliegue (dos arranques del stack) a los minutos de CI.
* `deploy.yml` solo corre cuando está en la rama por defecto (`main`): en `ENTREGA_x` no se dispara hasta que esa entrega llegue a `main`.

## Validación

1. Después de un merge, los digests de `:<rama>` y `:latest` coinciden con el de `:sha-<7>` validado (`docker buildx imagetools inspect`).
2. En un ensayo con un IT roto, el post-merge falla y `:<rama>` no cambia.
3. Cada promoción publica su manifiesto.
4. `deploy.yml` con el manifiesto M levanta en el runner los digests de M y el smoke da verde.
5. Sobre el mismo stack, el manifiesto M-1 se despliega y su smoke da verde (rollback ensayado).
6. Solo con el adaptador de Render: el digest del evento de despliegue de Render es el de Logística en M.

## Trabajo Futuro / Links

* Roadmap: [RM-06 y RM-07](../testing/preprod-fiel-roadmap.md#32-e6--lo-validado-es-lo-que-se-publica-y-auth-en-preprod-hasta-el-1311).
* Deuda registrada: [DTI-24](./DEUDA_TECNICA.md) (esta decisión) y DTI-25 (gate con verdes falsos).
* Evolución condicionada: merge queue (1B) si el post-merge muestra roturas frecuentes de la base; staging permanente (2C) si más componentes pasan a la nube, con un ADR sucesor del de tolerancia al desorden.
