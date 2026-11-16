# Registro de actividad de administración en Donaciones, con el autor del token y la anotación del sitio para lo que pasa fuera

- Status: proposed
- Date: 2026-11-12
- Deciders: el usuario (aprobación de la spec `R11-p2-actividad` y de su ficha, 2026-11-12, con las opciones recomendadas de sus cinco dudas); los detalles que la spec dejaba abiertos, el constructor del lote `p2-actividad` (agente, sin consulta). Pendiente de aprobación del equipo en el PR
- Tags: donaciones, contrato, rest, persistencia, seguridad, auditoria, cliente-liviano, r11, entrega-6

> **Escrito con el lote** (duda 4 de la spec, resuelta con la opción recomendada: el constructor
> escribe el ADR como `proposed`, no el cierre de la oleada). `[OBSERVED]` El código del lote ya
> implementa esta decisión (migración `V25__actividad_de_administracion.sql`, OpenAPI de Donaciones
> 1.11.0, pantalla `admin-actividad`). Código existente no es aprobación: si el revisor rechaza el
> ADR, puede hacer falta revertir.
>
> **Nota del cierre documental de la oleada P2 (2026-11-12).** El constructor lo escribió en el
> mismo lote, pero después de construir: como los demás ADR de las rutas CONTRATO de la oleada, es
> un desvío acordado del ciclo ARCHITECTURAL (la decisión quedó registrada después del código, no
> antes). El cierre no lo duplica; solo agrega dos hechos posteriores: `[OBSERVED]` en los arreglos
> de la ronda 1 (`18195a3b1`), `ejecutarAsignacion` devuelve `EjecucionRealizada(ejecucionId,
> propuestas)` y `ASIGNACION_CORRIDA` guarda el id de la ejecución en `objeto_id` (antes, nulo); el
> cuerpo de la respuesta HTTP no cambió. `[VERIFIED]` `run-preprod-tests.sh` en el cierre (contrato, integración y E2E 26/26 en el stack propio) y
> el recorrido con login real de la administración pasaron con el registro. El desvío
> transaccional de abajo sigue abierto y quedó como deuda en
> [DTI-43](../DEUDA_TECNICA.md#dti-43). La spec pasó a `completed/`.

## Contexto y Problema

`[OBSERVED]` Antes de R11-26 no había ningún registro de lo que hace la administración. Solo
aprobar o descartar una propuesta guardaba quién y cuándo (`propuesta.decidida_por` y
`fecha_decision`, V22, del
[ADR del autor de una propuesta](./20261112-autor-de-la-decision-de-una-propuesta-desde-el-token.md)).
Desde R11-10 cada persona de la administración tiene su cuenta, y el `sub` de su token la
identifica. Parte de lo que hace el admin pasa por Donaciones (propuestas, asignación, donaciones,
donantes, importaciones y organizaciones) y parte no: el alta de un administrador y otra contraseña
provisoria pasan por Keycloak, y los camiones, por Logística.

Problema: ¿dónde vive el registro, quién fija su autor y cómo entran las acciones que no pasan por
Donaciones?

**Gate A:** decisión nueva (una tabla y un recurso nuevos, y un canal nuevo sitio → Donaciones para
anotar acciones de otros sistemas). **Gate B:** cambia el modelo de datos y el contrato de
Donaciones, y toca la seguridad (de dónde sale el autor de una auditoría).

## Alternativas Consideradas

1. **Una tabla en Donaciones** (`actividad_administracion`), con el autor tomado del token de cada
   llamada, `GET /api/actividad` paginado y `POST /api/actividad` para que el sitio anote, con un
   catálogo cerrado, lo que hace en Keycloak y Logística (elegida).
2. **Cada servicio guarda lo suyo** y el sitio junta tres listas (Donaciones, Logística y los
   eventos de administración de Keycloak).
3. **Un servicio o un tópico de auditoría nuevo**, alimentado por eventos AMQP de cada servicio.
4. **El registro lo guarda el sitio** (cliente liviano) para todas las acciones.
5. **Guardar también el nombre del admin en cada fila.**

## Decisión

Elegida la alternativa 1.

- `V25` crea `donaciones.actividad_administracion` (`id`, `ocurrio_en`, `autor`, `accion`,
  `objeto_id`, `detalle`), con un `CHECK` sobre las 14 acciones y tres índices (por fecha, por
  autor y por acción). Copia las propuestas ya decididas con autor (`decidida_por` no nulo ni
  `SISTEMA`, con `fecha_decision`): es un dato real guardado por V22.
- El autor es siempre el `sub` del token de quien llama, leído en Donaciones (un puerto
  `AdministradorQueActua` sobre el `SecurityContext`); ningún cuerpo ni cabecera lo fija, y
  `X-Actor` no cuenta (RI-SVC-04). Solo un token de ADMIN genera filas.
- El `detalle` es un rótulo sin datos de personas humanas: la razón social de una organización o
  de un donante persona jurídica, el nombre de un archivo, una patente o una cantidad. El nombre de
  una cuenta se lee de Keycloak al mostrar.
- `GET /api/actividad` sigue el [ADR de listados paginados](./20261111-listados-paginados-de-donaciones-sin-lista-completa.md)
  y la [consulta en SQL nativo](./20261111-consultas-de-listado-en-sql-nativo-y-lectura-por-lote.md).
- `POST /api/actividad` acepta solo las cinco acciones del sitio (`ADMINISTRADOR_DADO_DE_ALTA`,
  `CONTRASENA_PROVISORIA_NUEVA`, `CAMION_AGREGADO`, `CAMION_DESHABILITADO`, `CAMION_HABILITADO`).
- `POST /api/entidades` suma `origen` (opcional, `SOLICITUD` o `ADMIN`), para distinguir aprobar
  una solicitud de registrar una organización.

### Desvío de la spec: el registro va después de la acción, no en su transacción

`[OBSERVED]` La spec (§2, ítem 2, y §4) pide que la acción y su fila se guarden en la misma
transacción. El constructor registra en el controller, después de que la acción sale bien, porque
los servicios de Donaciones no son `@Transactional` y envolverlos cambiaría su comportamiento (el
importador asíncrono leería un archivo todavía sin confirmar). Consecuencia: nunca queda una fila
de una acción que falló, pero si el `INSERT` de la actividad falla después de la acción, la acción
queda sin fila y la respuesta es un error aunque la acción se hizo. El revisor decide si se acepta
o si se pide la transacción común (un cambio en cada servicio).

## Consecuencias

- Bueno: una sola consulta paginada responde "quién hizo qué y cuándo", con filtros en la base y
  el total real; el autor no se puede falsificar desde el sitio; no hay canal nuevo entre servicios
  (el sitio ya llama a Donaciones con el token del admin).
- Bueno: el registro no guarda nombres, correos ni documentos de personas humanas (RNF-AUTH-09).
- Malo: lo que se hace en Keycloak y Logística depende de que el sitio lo anote. Si la anotación
  falla, la acción sigue hecha y no queda en el registro (WARN con el `traceId`, aceptado en la
  duda 2). Lo que se hace directo en la consola de Keycloak no aparece.
- Malo: Donaciones guarda datos de acciones de otros sistemas (una patente, un `sub`), sin poder
  comprobarlos: el catálogo cerrado y los largos acotan lo que entra.
- Neutro: las filas no se editan ni se borran; si el volumen crece, hará falta una política de
  retención (sin lote).

## Alternativas descartadas

- **2. Cada servicio lo suyo:** el sitio tendría que mezclar y paginar tres fuentes, sin un total
  común (contra la regla 1 del backlog).
- **3. Servicio o tópico de auditoría:** suma infraestructura y un canal asíncrono nuevo para una
  consulta ocasional; demasiado costo para la entrega.
- **4. El sitio guarda todo:** el cliente liviano no tiene base propia, y el autor dependería del
  sitio y no del token que recibe el servicio.
- **5. El nombre en cada fila:** el nombre envejece y es un dato personal copiado (ya descartado en
  el ADR del autor de una propuesta).

## Enlaces

- Spec: [R11-p2-actividad](../../specs/completed/R11-p2-actividad-spec.md)
- Ficha: [r11-p2-actividad](../../interfaz/flujos/r11-p2-actividad.md)
- Contrato: [openapi-donaciones.yaml](../../arquitectura/contratos/openapi-donaciones.yaml) (1.11.0)
- Deuda: [DTI-42](../DEUDA_TECNICA.md#dti-42), ítem 2, y [DTI-43](../DEUDA_TECNICA.md#dti-43)
