# Quién aprobó o descartó una propuesta: el `sub` del token, guardado en la propuesta y traducido a nombre al mostrarlo

- Status: proposed
- Date: 2026-11-12
- Deciders: el usuario (aprobación de la spec `R11-p1-admins`, 2026-11-12); los detalles que la spec dejaba abiertos (la traducción del autor en el historial y el `X-Actor` de marcar vencida), el constructor del lote `p1-admins` (agente, sin consulta). Pendiente de aprobación del equipo en el PR. Redactado por el agente en el cierre documental de la oleada P1
- Tags: donaciones, contrato, rest, seguridad, auditoria, cliente-liviano, r11, entrega-6

> **Escrito después del código.** Desvío acordado del ciclo ARCHITECTURAL (AGENTS.md §7.3 pide el
> ADR antes de implementar): la corrida R11 construyó el lote con su
> [spec aprobada](../../specs/completed/R11-p1-admins-spec.md) y dejó los ADRs para el cierre
> documental de la oleada. `[OBSERVED]` El código ya implementa esta decisión (commit `9a19085f8`,
> migración `V22__propuesta_decidida_por.sql`, OpenAPI de Donaciones 1.10.0). Código existente no
> es aprobación: si el revisor rechaza el ADR, puede hacer falta revertir.

## Contexto y Problema

`[OBSERVED]` Antes de R11-10, `PropuestaDeAsignacionService` aprobaba con
`propuesta.aceptar("SISTEMA")`: toda decisión quedaba a nombre de "SISTEMA", y la propuesta no
guardaba quién ni cuándo decidió. El cambio de estado de una donación (marcar vencida) tomaba el
autor de la cabecera `X-Actor`, que el cliente llenaba con el texto fijo "Administración". Con una
cuenta por admin ([ADR de las cuentas](../auth-service/20261112-cuentas-de-administracion-marcadas-en-el-realm-y-alta-desde-el-sitio.md)),
cada acción tiene que quedar a nombre de quien la hizo (R11-10), y el registro de actividad
(R11-26) y la búsqueda por quién aprobó (R11-35) van a leer ese dato.

Problema: ¿qué identifica al autor, de dónde lo toma Donaciones y cómo se ve su nombre sin copiar
datos personales al servicio?

**Gate A:** decisión nueva: hasta ahora Donaciones no guardaba el autor de una decisión. **Gate B:**
toca seguridad (el autor no puede salir de un dato que el cliente manipula), suma columnas a
`propuesta` y cambia el contrato de `PropuestaDTO` (aditivo).

## Alternativas Consideradas

1. **El `sub` del token de quien llama** (elegida), leído en el controller de
   `Authentication.getCredentials()` como `Jwt`, guardado en `propuesta.decidida_por` con
   `fecha_decision`, y traducido a nombre por el cliente al mostrarlo.
2. **Sumar el correo o el nombre al token** y guardarlo tal cual.
3. **Una cabecera del sitio** (`X-Actor` con el nombre de la sesión).
4. **Guardar el nombre** del admin en Donaciones al decidir (copiarlo del IdP).

## Resultado de la Decisión

Alternativa 1:

| Regla | Valor |
|---|---|
| Autor | `Jwt.getSubject()` de quien llama a `PUT /api/asignaciones/propuestas/{id}/estado`. El controller solo lo lee y lo pasa; `Propuesta.aceptar(actor)` y `rechazar(actor)` lo reciben como texto, sin conocer Spring ni Keycloak |
| Sin token | "SISTEMA", como antes (seguridad deshabilitada en tests) |
| Base | `V22`: `propuesta.decidida_por TEXT NULL` y `fecha_decision TIMESTAMP NULL`; las decididas antes quedan en `NULL`, sin dato inventado |
| Contrato | `PropuestaDTO` suma `decididaPor` y `fechaDecision`, opcionales y anulables. Ruta y cuerpo del `PUT /estado` iguales. Cambio aditivo (AGENTS.md §8.2, *feature*) |
| Marcar vencida | El cliente manda en `X-Actor` el `sub` de la sesión en lugar de "Administración"; sin sesión, "Administración" |
| Mostrar | El historial del detalle de una donación cambia un autor con forma de UUID por el nombre de la cuenta, o su correo si no tiene nombre; "una cuenta de administración" si la cuenta no existe o Keycloak no responde; "SISTEMA" o vacío no muestran autor; un texto viejo (un nombre guardado antes) se muestra tal cual. Cada cuenta se busca una vez por detalle (`AutoresDelHistorial`) |

**Por qué:**

- El token no lleva datos personales (RNF-AUTH-09) y `RealmTokenContractIT` lo prueba: sumar el
  correo o el nombre rompe esa regla.
- El `sub` no cambia si la persona corrige su nombre; el nombre copiado envejece y es un dato
  personal más guardado en un servicio de negocio.
- Una cabecera del sitio se puede falsificar: el autor saldría de un dato del cliente (invariante 2
  de la spec). El token lo firma el IdP.

## Consecuencias

**Positivas**

- Cada aprobación o descarte queda con quién y cuándo, listo para R11-26 y R11-35.
- El nombre se ve siempre al día, porque se lee del IdP al mostrar.

**Negativas**

- Mostrar el nombre pide una lectura a Keycloak por autor distinto en cada detalle. Con pocas
  cuentas de administración es acotado; un registro de actividad con muchas filas va a necesitar
  leerlas por lote.
- El evento `PropuestaAprobada` ya llevaba `actor`: cambia el valor (un UUID en lugar de
  "SISTEMA"), no la forma. `[OBSERVED]` Nadie fuera de Donaciones lo lee hoy.
- `X-Actor` sigue existiendo para marcar vencida: es una segunda fuente del autor, ya prevista en
  ADR-E6-05 (retiro de `X-Actor`), que no cambia acá.
- Las otras acciones del admin (aprobar una organización, registrar en el depósito, importar)
  todavía no guardan autor: van con `p2-actividad`.

## Análisis por alternativa

| Alternativa | A favor | En contra |
|---|---|---|
| **1. `sub` del token** | Firmado por el IdP; estable; sin datos personales en Donaciones | Hay que traducirlo a nombre al mostrar |
| 2. Correo o nombre en el token | Se muestra sin traducir | Rompe RNF-AUTH-09; el nombre cambia |
| 3. Cabecera del sitio | Sin cambios en Donaciones | Cualquiera que llame puede poner otro nombre |
| 4. Copiar el nombre | Una lectura menos al mostrar | Dato personal duplicado que envejece |

## Relaciones

- **Va con:** [cuentas de administración marcadas en el realm](../auth-service/20261112-cuentas-de-administracion-marcadas-en-el-realm-y-alta-desde-el-sitio.md).
- **Respeta:** la [política de evolución de contratos](../20261028-politica-de-evolucion-de-contratos-entre-servicios.md)
  (cambio aditivo) y el [ADR de la validación JWT en doble capa](../20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md).
- **Spec:** [R11-p1-admins](../../specs/completed/R11-p1-admins-spec.md).
