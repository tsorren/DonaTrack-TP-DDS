# Cuentas de administración marcadas con un atributo del realm y alta de un admin por otro desde el sitio

- Status: proposed
- Date: 2026-11-12
- Deciders: el usuario (aprobación de la spec `R11-p1-admins` y de su ficha, 2026-11-12, con las opciones recomendadas en las cuatro dudas); los detalles que la spec dejaba abiertos, el constructor del lote `p1-admins` (agente, sin consulta). Pendiente de aprobación del equipo en el PR. Redactado por el agente en el cierre documental de la oleada P1
- Tags: auth, keycloak, seguridad, cliente-liviano, administracion, r11, entrega-6

> **Escrito después del código.** Desvío acordado del ciclo ARCHITECTURAL (AGENTS.md §7.3 pide el
> ADR antes de implementar): la corrida R11 construyó el lote con su
> [spec aprobada](../../specs/completed/R11-p1-admins-spec.md) y dejó los ADRs para el cierre
> documental de la oleada. `[OBSERVED]` El código ya implementa esta decisión (commits `9a19085f8` y
> `72aac06c7`, rama `E6_backlog_r11_p1`). Código existente no es aprobación: si el revisor rechaza
> el ADR, puede hacer falta revertir.

## Contexto y Problema

`[OBSERVED]` Antes de R11-10, la administración era una sola cuenta fija del realm `donatrack`
(`auth-service/realm/donatrack-realm.json`), sin perfil, y RF-AUTH-07 ("solo un admin crea otro
admin; el primero sale de un *seed*") estaba sin hacer
([requerimientos E6](../../requerimientos/requerimientos-e6.md)). El backlog de la revisión del
2026-11-11 (R11-10) pide cuenta y perfil por administrador y que un admin dé de alta a otro.

Problema: ¿cómo sabe el sitio cuáles son las cuentas de administración para listarlas y contarlas,
y cómo da de alta una cuenta nueva sin servidor de correo y sin dejar datos sensibles a la vista?

**Gate A:** decisión nueva. Hasta ahora el realm solo tenía atributos de vínculo (`personaId`,
`solicitudEntidad`, [ADR del vínculo](./20261110-vinculo-cuenta-persona-personaid-en-keycloak.md));
esto suma una marca de rol en el perfil de usuario y una vía de alta de cuentas privilegiadas desde
el cliente liviano. **Gate B:** toca seguridad (quién puede crear una cuenta con `ADMIN`), el contrato
entre el IdP y el sitio (perfil declarativo del realm) y tiene costo de reversión (cada stack con el
realm ya importado necesita reimportarlo).

## Alternativas Consideradas

1. **Atributo `administracion=si` en el perfil declarativo del realm** (elegida), visible y editable
   solo por la administración del realm, más `altaPor` con el id de quien dio el alta.
2. **Listar por el rol:** `GET /roles/ADMIN/users`.
3. **Un grupo de Keycloak** "Administración" con sus miembros.
4. **Una tabla de administradores en Donaciones.**

Para la contraseña de la cuenta nueva:

- a. **Contraseña provisoria generada por el sitio** (elegida), temporal en Keycloak.
- b. **Acción por correo de Keycloak** (`execute-actions-email`).
- c. **Que el admin nuevo la elija** en el formulario de alta que completa otro.

## Resultado de la Decisión

Alternativa 1 con la variante a:

| Regla | Valor |
|---|---|
| Marca | `administracion: ["si"]` en el perfil de usuario declarativo del realm, con `view` y `edit` solo para `admin` (como `personaId`). El admin del *seed* la trae |
| Quién la dio de alta | `altaPor`: el `sub` de la cuenta que hizo el alta, con los mismos permisos |
| Lista y total | `GET /users?q=administracion:si&first=…&max=10` y `GET /users/count?q=administracion:si`, de a 10 con el paginador común; nunca la lista completa |
| Token | No cambia: ni la marca, ni el nombre, ni el correo viajan en el token (RNF-AUTH-09). `RealmTokenContractIT` sigue probándolo |
| Alta | Crea la cuenta, le asigna `ADMIN`, le quita `default-roles-donatrack` (que trae `DONANTE`) y le pone la marca. Si un paso falla después de crearla, la borra: la pantalla dice "No se creó nada" (invariante 3 de la spec). Un correo repetido (409) es un error del campo |
| Contraseña | 16 caracteres al azar de `SecureRandom`, sin caracteres que se confunden, en 4 grupos separados por guiones; marcada `temporary: true`, así Keycloak pide otra en el primer ingreso. Viaja a la página del resultado solo como *flash attribute*, con `Cache-Control: no-store`; no se guarda ni se escribe en ningún registro |
| Quién puede | Solo una cuenta con `ADMIN`, en rutas `/admin-*` con `hasRole('ADMIN')`. La cuenta de servicio del sitio solo asigna `ADMIN` desde el alta |
| Perfil | Cada admin corrige su nombre y apellido en `/admin-perfil` (puerto `DirectorioDeAdministradores`, adaptador `AdministradoresKeycloak`) |

**Por qué:**

- La búsqueda por atributo ya la usa el adaptador para las solicitudes de organización
  (`q=solicitudEntidad:pendiente`): no hay un segundo mecanismo. `/roles/ADMIN/users` no devuelve
  el total, que la regla general 1 del backlog pide como número, y exige `view-realm` para la cuenta
  de servicio, un permiso más amplio que el que tiene.
- El stack no tiene servidor de correo: la acción por correo no llega. Que el admin nuevo elija su
  contraseña en un formulario que llena otra persona rompe el sentido de una contraseña propia;
  la temporal obliga a cambiarla en el primer ingreso.
- Borrar lo creado si el alta falla a medias evita una cuenta con el rol por defecto (`DONANTE`) y
  sin `ADMIN`, que vería pantallas de donante sin que nadie lo haya pedido.

## Consecuencias

**Positivas**

- RF-AUTH-07 cumplido: un admin da de alta a otro desde el sitio y el primero sigue saliendo del
  *seed*.
- Cada acción del admin puede quedar a nombre de su cuenta (ver el
  [ADR del autor](../donaciones-service/20261112-autor-de-la-decision-de-una-propuesta-desde-el-token.md)).
- El token no cambia: los servicios no se enteran de la marca.

**Negativas**

- Keycloak importa el realm una sola vez. Un stack que ya tiene el realm `donatrack` no toma los
  dos atributos nuevos: Keycloak los descarta y la lista de administradores queda vacía. Hay que
  reimportar el realm (recrear solo el contenedor de Keycloak o importar el realm), nunca con
  `down -v` sobre un stack con datos.
- `[INFERRED]` No está comprobado contra Keycloak 26.8 que `/users/count` acepte `q`; la
  verificación con el IT del realm (`RealmTokenContractIT`, con Docker) quedó
  `[DEFERRED_NO_DOCKER]` en el lote. Si no lo acepta, el adaptador cuenta de a páginas de 100 y se
  anota como deuda ([DTI-42](../DEUDA_TECNICA.md#dti-42)).
- `[OBSERVED]` Caso límite: si falla también el `DELETE` que deshace un alta a medias, la cuenta
  queda creada y la pantalla igual dice "No se creó nada" (`AdministradoresKeycloak`); y sin
  `Location` en la respuesta del alta no se busca la cuenta para borrarla. Anotado en
  [DTI-42](../DEUDA_TECNICA.md#dti-42).
- No se puede quitar ni desactivar a un admin desde el sitio, ni recuperar la contraseña por correo
  (RF-AUTH-13): se hace desde la consola de Keycloak (dudas 1 y 2 de la spec, resueltas así).

## Análisis por alternativa

| Alternativa | A favor | En contra |
|---|---|---|
| **1. Atributo del realm** | Mismo mecanismo que las solicitudes; total con `/users/count`; no toca el token | El realm ya importado no lo toma sin reimportar |
| 2. Usuarios del rol `ADMIN` | La marca es el rol mismo, sin dato duplicado | Sin total; pide `view-realm` a la cuenta de servicio |
| 3. Grupo de Keycloak | Se ve en la consola | Otro concepto que mantener alineado con el rol; mismo problema de reimportar |
| 4. Tabla en Donaciones | Se pagina con el patrón de R11-01 | Datos de identidad en un servicio de negocio; dos fuentes de verdad sobre quién es admin |
| **a. Provisoria del sitio** | Anda sin correo; se cambia en el primer ingreso | Alguien la ve una vez y la tiene que pasar a mano |
| b. Correo de Keycloak | El sitio nunca ve una contraseña | No hay SMTP en el stack |
| c. La elige el admin nuevo | Nadie más la conoce… | …salvo quien llena el formulario, que es otra persona |

## Relaciones

- **Extiende:** el [vínculo cuenta-persona en Keycloak](./20261110-vinculo-cuenta-persona-personaid-en-keycloak.md)
  (`proposed`), con otro atributo del perfil declarativo y los mismos permisos.
- **Va con:** el [autor de la decisión de una propuesta desde el token](../donaciones-service/20261112-autor-de-la-decision-de-una-propuesta-desde-el-token.md).
- **Respeta:** el [proveedor de identidad Keycloak](./20261104-proveedor-de-identidad-keycloak.md)
  y RNF-AUTH-09 (sin datos personales en el token).
- **Spec y ficha:** [R11-p1-admins](../../specs/completed/R11-p1-admins-spec.md) ·
  [ficha de flujo](../../interfaz/flujos/r11-p1-admins.md).
- **Deuda:** [DTI-42](../DEUDA_TECNICA.md#dti-42).
