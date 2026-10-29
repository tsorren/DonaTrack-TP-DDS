# Base de datos por servicio en el mismo motor PostgreSQL

- Status: proposed
- Date: 2026-10-28
- Deciders: Pendiente de decisión grupal (propuesta del usuario en la SPEC-05)
- Tags: persistencia, postgresql, database-per-service, aislamiento, neon, preprod, entrega-5

> **Origen:** [SPEC-05](../specs/active/SPEC-05-preprod-fiel-rediseno-y-rm01-rm08.md), decisión D1 (`APPROVED_BY_USER`, 2026-10-28) y roadmap de [preprod fiel](../testing/preprod-fiel-roadmap.md), ítem RM-19. **Ninguna parte de esta decisión está implementada:** el código espera a que mergeen `E4_donaciones_minio` y `E4_persistencia_donaciones`, que tocan los mismos archivos.

## Contexto y Problema

`[DOCUMENTED]` El ADR [arquitectura de persistencia multi-schema](./20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql.md) (`proposed`) decidió **un motor PostgreSQL con una base `donatrack` y un schema por servicio**, cada uno con su rol y `REVOKE` cruzados.

`[OBSERVED]` Así está hoy: `persistencia/init-db/01-init-schemas-roles.sql` crea los schemas y los roles, y los servicios se conectan a `jdbc:postgresql://postgres:5432/donatrack?currentSchema=<servicio>` (`docker-compose.preprod.yml`).

El aislamiento depende de que ningún `GRANT` quede mal puesto: todos los dominios comparten una base, y un permiso de más habilita un `JOIN` entre contextos. El usuario pidió (2026-10-28) pasar a **una base de datos por servicio**, sin perder la huella chica en CI y en el plan gratuito de la nube.

Problema: ¿cómo se aísla la persistencia relacional de cada servicio al nivel de base de datos, dentro del runner de 7 GB y de Neon?

## Atributos de Calidad y Drivers de Decisión

* **Aislamiento entre contextos:** que un error de permisos no alcance para cruzar dominios.
* **Fidelidad con la nube:** que preprod y Neon usen el mismo modelo.
* **Huella en CI:** el runner de GitHub tiene 7 GB.
* **Costo de migración:** E5 vence el 30/10; dos ramas abiertas tocan la persistencia.
* **Reversibilidad** y **paso a un motor por servicio** si hiciera falta.

## Relaciones Arquitectónicas

1. **Propone reemplazar:** [20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql](./20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql.md) (`proposed`). La relación `superseded` la aprueba un humano (AGENTS.md §9.3); hasta entonces, los dos quedan `proposed` y el código sigue al de 2026-09-02.
2. **Conserva** de ese ADR: un rol por servicio con mínimo privilegio, el script de init idempotente y el schema `<servicio>` dentro de cada base.
3. **Se aplica igual a objetos:** el equivalente en MinIO (una instancia por servicio) queda como RM-20 del roadmap, después de E7. No pide ADR propio si este se aprueba: aplica el mismo criterio a otro almacén.

## Alternativas Consideradas

Notas de 1 a 5; el puntaje es la suma de nota × peso / 100 (SPEC-05, D1). Las notas son juicio `[INFERRED]` sobre la evidencia relevada.

| Criterio | Peso | A. Schema por servicio (hoy) | **B. Base por servicio en el mismo motor** | C. Un motor (contenedor) por servicio |
|---|:-:|:-:|:-:|:-:|
| Aislamiento entre contextos | 25 | 3 | 4 | 5 |
| Fidelidad con la nube | 20 | 3 | 4 | 3 |
| Huella en CI | 20 | 5 | 5 | 2 |
| Costo de migración | 15 | 5 | 4 | 2 |
| Reversibilidad | 10 | 5 | 4 | 3 |
| Paso a un motor por servicio | 10 | 2 | 4 | 5 |
| **Total** | 100 | 3,8 | **4,2** | 3,35 |

## Análisis por alternativa

**A. Schema por servicio (status quo).** *Pros:* ya funciona, sin migración. *Contras:* una sola base; un `GRANT` mal puesto cruza dominios, y el aislamiento depende de revisar permisos a mano.

**B. Base por servicio en el mismo motor.** *Pros:* PostgreSQL no permite `JOIN` entre bases, así que el cruce es imposible aunque falle un permiso; `REVOKE CONNECT ON DATABASE … FROM PUBLIC` cierra el acceso. Un solo proceso: la huella no cambia. Hibernate y Flyway no cambian, porque se conserva el schema `<servicio>` dentro de cada base. Pasar a C después es cambiar el host de la URL. *Contras:* cambian las URLs de los 3 composes, el script de init y los tests de Testcontainers. `[INFERRED]` Neon admite varias bases por proyecto; se verifica en RM-01.

**C. Un motor por servicio.** *Pros:* aislamiento de proceso. *Contras:* unos 200 MB por motor (ADR de 2026-09-02), 3 o 4 servicios nuevos en 3 composes con sus healthchecks, y en la nube varios proyectos o instancias. La nube no lo necesita hoy.

## Resultado de la Decisión

Se propone **B**:

1. El script de init crea las bases `notificaciones`, `incentivos`, `donaciones` y `logistica` (el perfil `postgres` heredado de Logística), cada una con dueño su rol y `REVOKE CONNECT ON DATABASE <base> FROM PUBLIC`.
2. Dentro de cada base, el schema `<servicio>` sigue igual: `currentSchema`, `hibernate.default_schema` y `spring.flyway.schemas` no cambian.
3. Las URLs pasan de `/donatrack?currentSchema=<servicio>` a `/<servicio>?currentSchema=<servicio>`.
4. Los tests de Testcontainers cambian `withDatabaseName("donatrack")` por el nombre del servicio.
5. Los componentes de E6 (auth y cliente liviano) reciben su propia base en lugar de un schema. RNF-AUTH-06 ya admite "schema o base propia"; RNF-CL-05 pide un schema `cliente` y habría que ajustarlo ([requerimientos de E6](../requerimientos/requerimientos-e6.md)).

**Archivos que toca la implementación** (RM-19, después de que mergeen `E4_donaciones_minio` y `E4_persistencia_donaciones`): `persistencia/init-db/*`, los 3 composes, los 3 `application-postgres.properties`, los 4 tests de Testcontainers con sus copias del script, y `persistencia/README.md`.

**Precondiciones, antes de aprobar:**

* Confirmar en RM-01 que el plan de Neon permite una base por servicio en el mismo proyecto.
* Confirmar que el enunciado de E5 no exige un schema por servicio. `[DOCUMENTED]` La fuente de RNF-AUTH-06 dice "Enunciado E5: un schema por servicio, motor compartido (por analogía)". `[OBSERVED]` El enunciado de E5 no está en `docs/entregas/`. Si lo exige, el enunciado manda (AGENTS.md §2) y esta propuesta se rechaza o se difiere.

Justificación: es el pedido del usuario con el menor costo, y deja C a un cambio de host. Lleva el patrón *Database per Service* al nivel que PostgreSQL hace cumplir: el límite de cada bounded context pasa a ser una base, no una convención de permisos.

## Consecuencias Positivas

* El cruce entre dominios deja de depender de los permisos: PostgreSQL no lo permite entre bases.
* La huella en CI y en la nube no cambia.
* El paso a un motor por servicio no toca el código de la aplicación.

## Consecuencias Negativas

* Cambian las URLs JDBC y el script de init: hay que migrar los volúmenes locales (`docker compose down -v`).
* Durante la transición conviven dos ADRs `proposed` sobre persistencia, hasta que un humano apruebe el reemplazo.
* Choca con dos ramas abiertas de E4; implementarlo antes obligaría a rehacer sus cambios.

## Validación

1. Desde el rol de un servicio, `\c <otra-base>` falla por falta de `CONNECT`.
2. Flyway migra cada servicio en su base y su schema, sin cambios en las migraciones.
3. Los tests de Testcontainers de los 4 servicios y el preprod (`run-preprod-tests.sh`) quedan en verde.

## Trabajo Futuro / Links

* Roadmap: [RM-19 y RM-20](../testing/preprod-fiel-roadmap.md).
* Precondición en la nube: RM-01 (Neon).
* Deuda: [DTI-26](./DEUDA_TECNICA.md#dti-26) (decisión documentada, implementación pendiente).
