# Política de evolución de contratos entre servicios

- Status: proposed
- Date: 2026-10-28
- Deciders: Pendiente de decisión grupal (política propuesta por el usuario en la SPEC-05; responsable de integración: tsorren)
- Tags: contratos, openapi, json-schema, amqp, cicd, quality-gate, equipos, entrega-5

> **Origen:** [SPEC-05](../specs/active/SPEC-05-preprod-fiel-rediseno-y-rm01-rm08.md), decisión D5 (`APPROVED_BY_USER`, 2026-10-28), e ítem RM-08 del [roadmap de preprod fiel](../testing/preprod-fiel-roadmap.md). `[OBSERVED]` Implementada en la rama `E5_preprod_fiel_rm08`.

## Contexto y Problema

El equipo trabaja por servicio, con integrantes de distinta experiencia. Un cambio de contrato de un equipo no debería frenar a otro, pero el día de entrega, cuando se presenta `ENTREGA_x` a la cátedra, todo tiene que estar resuelto. Que `ENTREGA_x` quede rota entre medio no es un problema (usuario, 2026-10-28).

Fricción observada `[OBSERVED]`:

* 15 de los 19 schemas `evento-*` tenían `"additionalProperties": false`, pero los consumidores leen los eventos con `@JsonIgnoreProperties(ignoreUnknown = true)`. Un campo nuevo no rompe a nadie al ejecutar y sí haría fallar la validación de RM-08: un bloqueo falso.
* Un diff de OpenAPI que falle en cada PR frenaría también los cambios incompatibles coordinados.
* AGENTS.md §8.2 distingue entre cambio aditivo e incompatible, pero el CI no aplicaba esa distinción.

Problema: ¿dónde se bloquea un cambio de contrato incompatible para que no frene a otros equipos y quede resuelto el día de entrega?

## Atributos de Calidad y Drivers de Decisión

Pesos de la SPEC-05: fricción en cambios aditivos 25 · protección ante roturas, medida en el corte de entrega, 30 · no bloquear a otro equipo 20 · costo y dependencias 10 · alineación con AGENTS.md (§2, §4.3, §8.2) 10 · auditabilidad de la excepción 5.

## Relaciones Arquitectónicas

1. **Extiende:** [20260426-pipeline-unificado-de-cicd-y-automatizacion-de-calidad](./20260426-pipeline-unificado-de-cicd-y-automatizacion-de-calidad.md) (`accepted`): suma un estrato de bloqueo (post-merge y corte) sin cambiar los gates existentes del PR.
2. **Depende de:** [20261020-promocion-de-artefactos-por-digest-y-despliegue-reproducible-de-logistica](./20261020-promocion-de-artefactos-por-digest-y-despliegue-reproducible-de-logistica.md) (`proposed`): el bloqueo vive en su preprod post-merge (`merge.yml`, `preprod-post-merge`).
3. **Respeta:** AGENTS.md §4.3. La validación REST que `ContractIT` ya hacía en cada PR no cambia: bajarla a aviso debilitaría un gate existente.

## Alternativas Consideradas

| Alternativa | Fricción | Protección | No bloquear | Costo | Alineación | Auditoría | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **H. Aviso en el PR, bloqueo en el post-merge y en el corte de `ENTREGA_x`** | 5 | 5 | 4 | 4 | 4 | 3 | **4,5** |
| B. Bloqueo de lo incompatible en el PR, salvo excepción declarada | 4 | 5 | 3 | 4 | 5 | 5 | 4,25 |
| D. Expandir y contraer (v1 y v2 conviven) | 3 | 5 | 5 | 3 | 5 | 4 | 4,25 |
| C. Schemas de eventos abiertos a campos extra | 5 | 3 | 4 | 5 | 4 | 3 | 4,0 |
| E. Contratos dirigidos por el consumidor (Pact) | 4 | 5 | 4 | 1 | 3 | 4 | 3,9 |
| F. Solo aviso, nunca bloquea | 5 | 1 | 5 | 5 | 1 | 2 | 3,25 |
| A. Bloqueo duro en todo PR | 1 | 5 | 1 | 4 | 4 | 3 | 2,9 |
| G. Spec generada desde el código | 4 | 2 | 3 | 3 | 2 | 3 | 2,85 |

## Análisis por alternativa

**H.** *Pros:* nadie queda frenado por un cambio coordinado; la rotura se detecta en el post-merge y, como tarde, en el corte. *Contras:* `ENTREGA_x` puede quedar rota hasta el corte; el aviso del PR se puede ignorar.

**B.** *Pros:* la excepción queda escrita. *Contras:* si `ENTREGA_x` puede romperse, declarar una excepción en cada PR es burocracia. Queda absorbida por H.

**D.** *Pros:* nunca rompe al consumidor. *Contras:* duplica eventos y endpoints; se reserva para el caso que cruza el corte.

**C.** *Pros:* el contrato escrito queda tan permisivo como los consumidores; elimina el bloqueo falso. *Contras:* sola, no protege ante un campo requerido que desaparece. Se combina con H.

**E.** *Pros:* el consumidor declara lo que usa. *Contras:* dependencia y broker de contratos con poco beneficio en un monorepo.

**F.** *Contras:* el gate da verde ante una rotura real (P11 del análisis de preprod).

**A.** *Contras:* frena a todos por cambios coordinados.

**G.** *Contras:* la spec pasa a seguir al código, y AGENTS.md §2 le da la autoridad al contrato documentado.

## Resultado de la Decisión

Se propone **H + C + contrato primero**, con **D** solo para un cambio que cruza el corte:

1. **Contrato primero.** Un cambio de contrato entra como un PR chico con el schema o la spec; cada equipo implementa contra él, con fixtures validadas por el schema. `.github/CODEOWNERS` asigna `docs/arquitectura/contratos/` al responsable de integración (`@tsorren`), para que GitHub le pida revisión. Que esa revisión sea obligatoria depende de la protección de rama, configuración de GitHub fuera del repo.
2. **Schemas de eventos abiertos (C).** Los 15 schemas cerrados aceptan campos extra; siguen exigiendo los `required`, los tipos y los formatos. Es un cambio compatible.
3. **H.** Los chequeos **nuevos** de RM-08 avisan en el PR (`::warning::`, sin fallar el job) y fallan en el post-merge y en el corte:
   * mensajes AMQP capturados en el E2E contra su schema (`CONTRATOS_ESTRICTO`);
   * diff de OpenAPI contra la rama base con `oasdiff` (cambios incompatibles).
4. **Corte de entrega.** El responsable de integración lanza el post-merge a mano (`workflow_dispatch` de `merge.yml`) sobre `ENTREGA_x` 24 h antes del vencimiento.
5. **D. Expandir y contraer** solo para un cambio incompatible que el consumidor no llega a migrar antes del corte.
6. **Fallas heredadas.** `report_test_failures.py` marca como "heredadas de la base" las fallas de servicios que el PR no modificó, para que cada integrante sepa si un rojo es suyo. La marca no cambia el resultado.

Justificación: es la única combinación que no frena a otro equipo y deja la entrega protegida, sin debilitar un gate existente. Aplica el principio de *Tolerant Reader* (el consumidor ignora lo que no conoce, y el contrato escrito lo acompaña) y respeta los límites de cada bounded context: cada equipo cambia su contrato publicado, no el código del otro.

**Seguridad del manifiesto:** `main.yml` y `deploy.yml` solo aceptan manifiestos de corridas del propio repositorio (`head_repository_id == repository_id`), y el resolvedor rechaza imágenes fuera de `ghcr.io/<repo>/<servicio>@sha256:`. Un PR desde un fork no puede inyectar una imagen.

## Consecuencias Positivas

* Un campo nuevo en un evento no bloquea a nadie.
* Un cambio incompatible coordinado no frena a otros equipos en el PR.
* El corte de entrega tiene un gate explícito, a cargo de una persona.
* Cada integrante ve si un rojo viene de la base.

## Consecuencias Negativas

* `ENTREGA_x` puede quedar con un contrato roto hasta el corte; el post-merge falla y no promueve (RM-06) hasta que se arregle hacia adelante.
* El aviso del PR depende de que alguien lo lea.
* Los schemas abiertos ya no detectan un campo de más mal escrito; sí detectan el requerido que falta. Tampoco detectan datos de un hecho metidos en el comando `entrega.solicitada.v1`: esa separación pasa a depender de la revisión del contrato.
* El corte compara las specs contra `main` (la última entrega presentada): todo cambio incompatible de la entrega lo hace fallar, también uno coordinado y ya migrado. La única salida es expandir y contraer (D).
* El espía liga `donaciones.exchange` solo con los prefijos conocidos (`donacion.`, `donante.`, `persona.` y el comando de la instancia del preprod), para no ocultar los rechazos `mandatory` del broker: un evento nuevo de Donaciones con otro prefijo no se captura hasta sumar su clave.
* El corte depende de una corrida manual: si nadie la lanza, el último post-merge es el gate.

## Validación

1. En el PR, un mensaje AMQP que no cumple su schema deja un `::warning::` y el job sigue; en el post-merge (`CONTRATOS_ESTRICTO=true`) el E2E falla.
2. Un mensaje de un tipo sin schema se reporta igual que una violación.
3. En un PR de prueba que elimina un campo de una spec, `oasdiff` marca el cambio incompatible: aviso en el PR, error en el post-merge.
4. `node scripts/validate-contracts.js` sigue en verde con los schemas abiertos.

## Trabajo Futuro / Links

* Roadmap: [RM-08](../testing/preprod-fiel-roadmap.md).
* Protección de rama con revisión obligatoria de code owners: pide OK aparte (configuración de GitHub).
