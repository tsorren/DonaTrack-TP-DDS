# Spec: R11 p1-admins — Cada administrador con su cuenta

> **Estado:** PROPUESTA (2026-11-12), para la parada única de aprobación de la oleada P1.
> **Lote:** `p1-admins` (R11-10) del [backlog de la revisión del 2026-11-11](../../interfaz/backlog-revision-2026-11-11.md),
> ruta CONTRATO. Ficha de flujo: [`r11-p1-admins.md`](../../interfaz/flujos/r11-p1-admins.md).
> **Nivel:** ARCHITECTURAL por AGENTS.md §7.0 (cambia el realm de Keycloak, el contrato REST de
> Donaciones y suma una migración), escrita con el formato corto de la ruta STANDARD de
> `define-spec`, como pide el lote. El ADR va al cierre de la oleada (desvío acordado en la
> [prompt de orquestación](../../IA/prompts/orquestacion-backlog-r11.md)); ninguno se promueve.
> **Base:** `ebe332642`.
> **Módulos:** `auth-service` (realm y su test de contrato), `donaciones-service` (quién decide una
> propuesta), `cliente-liviano` (pantallas `admin-perfil` y `admin-administradores`, su controller y
> su adaptador de Keycloak), `docs/arquitectura/contratos/openapi-donaciones.yaml`.
> **Requerimientos:** RF-AUTH-07, RF-AUTH-12, RNF-AUTH-09 ([requerimientos E6](../../requerimientos/requerimientos-e6.md)).

---

## 1. Objetivo

Hoy la administración es una sola cuenta del realm, sin perfil, y lo que aprueba queda como
"SISTEMA" (`PropuestaDeAsignacionService.java:118`, `propuesta.aceptar("SISTEMA")`). Después de este
lote:

1. **Cuentas:** cada persona de la administración tiene su cuenta; un admin da de alta a otro desde
   el sitio (RF-AUTH-07). El primero sigue saliendo del *seed* del realm.
2. **Autor real:** aprobar o descartar una propuesta queda a nombre de la cuenta que lo hizo, y el
   historial de la donación lo muestra con su nombre.
3. **Perfil:** cada admin ve y corrige su nombre y apellido.

Si no entra entero, se construye en ese orden.

## 2. Alcance

### Entra

- **Realm (`auth-service/realm/donatrack-realm.json`):** dos atributos nuevos en el perfil de usuario
  declarativo, solo visibles y editables por la administración del realm (como `personaId`):
  `administracion` (marca de las cuentas de administración, valor `si`) y `altaPor` (id de la cuenta
  que la dio de alta). El admin del *seed* lleva `administracion: ["si"]`. **El token no cambia**
  (RNF-AUTH-09: ni nombre ni correo en el token).
- **Cliente liviano:**
  - Puerto nuevo `DirectorioDeAdministradores` (en `puertos/`) con su adaptador de Keycloak (en
    `cuentas/`, junto a `DirectorioKeycloak`, con la misma cuenta de servicio) y uno de maqueta.
  - Controllers de `admin-perfil` y `admin-administradores` (con sus envíos) y sus plantillas, según
    la ficha.
  - Generador de la contraseña provisoria (`SecureRandom`, 16 caracteres sin ambiguos, en grupos de
    4). Cumple la política del realm (`length(10) and notEmail`).
  - Traducción del autor de un cambio de estado (id de cuenta → nombre) en el historial del detalle
    de una donación del admin.
- **Donaciones:**
  - `actualizarEstado(id, estado)` pasa a recibir el autor: el `sub` del token de quien llama. El
    controller lo toma de la credencial (`Authentication.getCredentials()`, un `Jwt`); sin token
    (seguridad deshabilitada en tests), "SISTEMA" como hoy.
  - `Propuesta.aceptar(actor)` y `rechazar(actor)` guardan quién y cuándo decidió.
  - Migración `V22__propuesta_decidida_por.sql` (la siguiente a `V21`): `decidida_por TEXT NULL` y
    `fecha_decision TIMESTAMP NULL` en `propuesta`. Las ya decididas quedan en `NULL`.
  - `PropuestaDTO` suma `decididaPor` y `fechaDecision`, opcionales.
  - OpenAPI de Donaciones al día.

### No entra

- Quitar o desactivar a un administrador, cambiar el correo de una cuenta, recuperar la contraseña
  por correo (RF-AUTH-13, D8): ver las dudas de la ficha.
- El registro de actividad (R11-26, `p2-actividad`) y la búsqueda de asignaciones por quién aprobó
  (R11-35, `p2-admin-bandeja`): este lote les deja el dato (`decidida_por`, `fecha_decision`,
  `altaPor`), no la pantalla.
- El autor de otras acciones del admin que hoy no guardan ninguno: aprobar o rechazar una
  organización (`cuentas/SolicitudesDeEntidad`), registrar en el depósito, importar un CSV. Van con
  `p2-actividad`.
- El nombre en el saludo y en el menú de la cuenta (R11-44, `p1-nombres`): ese lote toma el nombre
  del admin de `DirectorioDeAdministradores.perfil(cuentaId)`.
- Separar las notificaciones del admin por cuenta: siguen llegando a la persona fija de
  Administración (`AdminConstantes.ID_ADMIN`).
- Renombrar rutas, clases o campos de la API.

## 3. Decisiones

1. **El autor se guarda como el id de la cuenta (`sub`), no como el correo ni el nombre.** El token
   no lleva datos personales (RNF-AUTH-09, y `RealmTokenContractIT` lo prueba: no se toca); el id no
   cambia si la persona corrige su nombre. El cliente lo traduce a nombre al mostrarlo.
   Descartado: sumar `email` o `name` al token (rompe RNF-AUTH-09) y guardar el nombre en Donaciones
   (copia un dato personal y envejece).
2. **Donaciones toma el autor del token, no de una cabecera.** Así nadie aprueba a nombre de otro.
   El `X-Actor` de marcar vencida (`DonacionesIndependientesController`) queda como está: el cliente
   pasa a mandar el `sub` de la sesión en lugar de "Administración".
3. **Las cuentas de administración se listan por el atributo `administracion`**, con la búsqueda por
   atributo que el adaptador ya usa (`/users?q=solicitudEntidad:pendiente`). Descartado: listar los
   usuarios del rol `ADMIN` (`/roles/ADMIN/users`), que pide `view-realm` para la cuenta de servicio
   y no da el total.
4. **El alta no deja cuentas a medias.** Si falla un paso después de crear la cuenta (asignar el rol,
   quitar el rol por defecto), el adaptador la borra y la pantalla dice que no se creó nada.
5. **La cuenta nueva queda solo con `ADMIN`.** La Admin API le da el rol por defecto
   (`default-roles-donatrack`, que trae `DONANTE`); el alta se lo quita.
6. **La contraseña provisoria la genera el sitio y no se guarda.** Keycloak la marca como temporal
   (`temporary: true`) y pide elegir otra en el primer ingreso. Se lleva a la pantalla del resultado
   en un *flash attribute* (una sola lectura), con `Cache-Control: no-store`, y nunca se escribe en
   un log.

## 4. Contrato antes y después

### 4.1 Donaciones

| | Antes | Después |
|---|---|---|
| `PUT /api/asignaciones/propuestas/{id}/estado` | Cuerpo `{ "estado": "APROBADA" \| "DESCARTADA" }`, `200` sin cuerpo; autor fijo "SISTEMA" | **Igual** (ruta, cuerpo, respuestas y errores). Autor: el `sub` del token. Se guarda en `propuesta.decidida_por` con `fecha_decision`, y al aprobar llega como hoy al `actor` del cambio de estado de cada donación asignada (`cambio_estado_donacion_independiente.actor`) |
| `GET /api/asignaciones/propuestas` | `PropuestaDTO { id, estado, fechaCreacion, necesidad, fragmentaciones }` | Suma `decididaPor` (string o `null`: id de la cuenta, "SISTEMA" o `null` en las decididas antes del cambio) y `fechaDecision` (fecha y hora o `null`). Aditivo |
| `GET /donaciones-independientes/{id}` (historial) | `actor`: "SISTEMA", un nombre o un correo | Igual de forma; en lo nuevo, `actor` es el id de la cuenta |
| Base | `propuesta` sin autor | `V22`: `decidida_por TEXT NULL`, `fecha_decision TIMESTAMP NULL` |

### 4.2 Keycloak (Admin API, por la cuenta de servicio `cliente-liviano`)

Permisos que ya tiene: `manage-users`, `view-users`, `query-users`. No se suman.

| Uso | Llamada |
|---|---|
| Lista de administradores, de a 10 | `GET /users?q=administracion:si&first={10·(página−1)}&max=10&briefRepresentation=false` |
| Total | `GET /users/count?q=administracion:si` |
| Estado "Todavía no entró" | `requiredActions` contiene `UPDATE_PASSWORD` (viene en la lista) |
| Perfil propio | `GET /users/{sub}` y `PUT /users/{sub}` (solo `firstName` y `lastName`, conservando lo demás, como `cambiarAtributos`) |
| Alta | `POST /users` con `username` y `email` = correo, `firstName`, `lastName`, `enabled: true`, `emailVerified: true`, `attributes: { administracion: [si], altaPor: [<sub>] }`, `credentials: [{ type: password, value, temporary: true }]` → id en `Location`; `POST /users/{id}/role-mappings/realm` con `ADMIN` (de `/available`); `DELETE /users/{id}/role-mappings/realm` con `default-roles-donatrack` |
| Deshacer un alta a medias | `DELETE /users/{id}` |
| Otra contraseña provisoria | `PUT /users/{id}/reset-password` con `{ type: password, value, temporary: true }`, solo si la cuenta tiene `UPDATE_PASSWORD` pendiente y `administracion: si` |
| Nombre de un autor | `GET /users/{id}`, una vez por autor distinto en la misma página |

### 4.3 Cliente liviano (pantallas)

Todas bajo `/admin-*`: las protege la regla existente (`hasRole('ADMIN')`, `SeguridadConfig`).

| Ruta | Parámetros | Respuesta |
|---|---|---|
| `GET /admin-perfil` | — | Página. Con `?escenario=` en `maqueta` |
| `POST /admin-perfil` | `nombre`, `apellido` | `303` a `/admin-perfil` con el aviso, o la página con los errores de campo |
| `GET /admin-administradores` | `pagina` (desde 1; más allá de la última, la última) | Página; con `HX-Request`, el `main` |
| `GET /admin-administradores-nuevo` | — | El formulario de alta como página (sin JavaScript) |
| `POST /admin-administradores` | `nombre`, `apellido`, `correo` | `303` a `/admin-administradores-alta` con la contraseña en *flash*; con errores, la ventana (o la página aparte) con lo escrito |
| `GET /admin-administradores-alta` | — | La contraseña provisoria si viene en *flash*; si no, `303` a `/admin-administradores` con el aviso "se mostró una sola vez" |
| `POST /admin-administradores-contrasena` | `cuenta` (id) | `303` a `/admin-administradores-alta`, o a la lista con el aviso si la cuenta ya entró |

Fuera de los archivos del lote, imprescindibles (van en `fueraDeZona` del constructor):

- `templates/layout-privado.html`: "Mi perfil" y "Administradores" en el menú de la cuenta del
  admin, y la condición de la campana del admin, que hoy se apoya en `perfil == null`.
- `web/Pantallas.java`: la dirección de Mi perfil del área admin.
- `web/AdminDonacionesEnvios.java`: `X-Actor` con el `sub` de la sesión.
- `rest/AdminOperacionDonaciones.java` (`historial`): el autor como nombre (§5, C8).

## 5. Criterios de aceptación

```gherkin
Escenario C1: un admin da de alta a otro
  Dado que entré como administradora
  Cuando doy de alta a "Bruno Díaz" con un correo que no usa ninguna cuenta
  Entonces veo una contraseña provisoria de 16 caracteres, una sola vez
  Y en Keycloak la cuenta tiene administracion=si, altaPor=<mi sub>, UPDATE_PASSWORD pendiente
  Y sus roles del realm son solo ADMIN (sin DONANTE ni default-roles-donatrack)
  Y "Administradores" la muestra como "Todavía no entró"

Escenario C2: el primer ingreso de la cuenta nueva
  Dado una cuenta de administración recién creada
  Cuando entra con su correo y la contraseña provisoria
  Entonces Keycloak le pide elegir su contraseña y después llega a admin-inicio
  Y el claim roles de su token es ["ADMIN"]

Escenario C3: correo repetido o mal escrito
  Cuando doy de alta con un correo que ya tiene una cuenta (o sin @)
  Entonces la ventana sigue abierta con el error junto al correo y lo escrito conservado
  Y no se creó ninguna cuenta

Escenario C4: alta a medias
  Dado que Keycloak falla al asignar el rol ADMIN
  Entonces la cuenta creada se borra y la pantalla dice "No se creó nada"

Escenario C5: lista paginada en el servidor
  Dado 12 cuentas de administración
  Cuando abro Administradores y después ?pagina=2
  Entonces cada página pide a Keycloak first/max de 10 y el total con /users/count
  Y la página 2 muestra 2 cuentas; ?pagina=9 muestra la página 2
  Y nunca se pide la lista completa

Escenario C6: aprobar a nombre propio
  Dado una propuesta PENDIENTE
  Cuando la apruebo con mi sesión
  Entonces propuesta.decidida_por = mi sub y fecha_decision tiene la hora
  Y el cambio de estado a Asignada de cada donación tiene actor = mi sub
  Y GET /api/asignaciones/propuestas devuelve decididaPor = mi sub

Escenario C7: descartar a nombre propio
  Cuando descarto una propuesta PENDIENTE
  Entonces decidida_por = mi sub y fecha_decision tiene la hora

Escenario C8: el historial muestra el nombre
  Dado una donación asignada por una propuesta que aprobé, y marcada vencida por otra admin
  Cuando abro su detalle como admin
  Entonces dice "La aprobó Ana Pérez." y "la marcó Carla Gómez"
  Y una aprobada antes del cambio ("SISTEMA") no dice quién la aprobó
  Y un autor sin nombre cargado se muestra con su correo

Escenario C9: perfil
  Cuando cambio mi nombre a "Ana María" y guardo
  Entonces Keycloak tiene firstName "Ana María", el resto de la cuenta sin cambios
  Y veo "Listo. Guardamos tus datos."
  Y el correo no es editable

Escenario C10: sin permiso
  Dado una cuenta con rol DONANTE
  Cuando pide /admin-administradores o hace POST /admin-administradores
  Entonces recibe la página 403 y no se crea nada

Escenario C11: otra contraseña provisoria
  Dado una cuenta de administración que todavía no entró
  Cuando genero otra contraseña provisoria
  Entonces la anterior deja de servir y veo la nueva una sola vez
  Y si la cuenta ya entró, no se cambia nada y veo el aviso

Escenario C12: el token sigue sin datos personales
  Entonces RealmTokenContractIT pasa sin cambios en sus aserciones
  Y suma que el admin del seed tiene administracion=si
```

Verificación: tests unitarios del servicio y del dominio de propuestas (C6, C7), un test de la
migración con la base del módulo si el módulo lo tiene, `MockRestServiceServer` para el adaptador de
Keycloak (C1, C3, C4, C5, C9, C11), tests de controller del cliente (C10 y los estados de la ficha),
`RealmTokenContractIT` y `scripts/tests/test_auth_realm_contract.py` (C12, con Docker), y C2 y C8 con
login real en el stack propio de la oleada.

## 6. Compatibilidad con los clientes actuales

- **Cliente liviano:** `decidirPropuesta` no cambia. Los dos campos nuevos de `PropuestaDTO` los
  ignora hasta que los use (Jackson tolera campos de más en los records del cliente; comprobarlo en
  su configuración).
- **Seeder de demo:** si aprueba propuestas con una sesión de admin, quedan a nombre del admin del
  *seed*; no necesita cambios. Si aprueba con el token técnico, el endpoint ya lo rechaza hoy
  (`ADMIN`), así que no lo hace.
- **Otros servicios:** el evento `PropuestaAprobada` ya llevaba `actor`; cambia el valor, no la
  forma. Nadie fuera de Donaciones lee ese campo (comprobarlo con un `grep` al construir).
- **Datos viejos:** las propuestas decididas antes quedan con `decidida_por` en `NULL`, y los cambios
  de estado viejos con "SISTEMA" o "Administración". El cliente los muestra como dice C8.
- **oasdiff:** el cambio es aditivo; no tiene que marcar incompatibilidades.

## 7. Riesgos

1. **El realm se importa una sola vez.** Un Keycloak que ya tiene el realm `donatrack` no toma los
   atributos nuevos: sin el perfil declarado, Keycloak descarta `administracion` y `altaPor`, y la
   lista queda vacía. El stack propio de la oleada (`donatrack-r11`) y el seeder de demo arrancan de
   cero, así que ahí anda. El stack del usuario necesita reimportar el realm (README de
   `auth-service`, "Keycloak importa el realm solo la primera vez"); va en el informe de la oleada.
   Nunca `down -v` sobre el stack del usuario.
2. **`/users/count` con `q`.** Se espera que Keycloak 26.8 acepte `q` en el conteo
   (`[INFERRED]`). Si no, el adaptador cuenta de a páginas de 100 hasta agotar (pocas cuentas) y se
   anota como deuda. Se comprueba primero, en el stack propio.
3. **Poder de la cuenta de servicio.** El sitio ya puede crear cuentas y asignar roles; ahora asigna
   `ADMIN`. Toda ruta que lo hace está bajo `/admin-*` con `hasRole('ADMIN')`, y el adaptador solo
   asigna `ADMIN` desde el alta. C10 lo prueba.
4. **Choque con `p1-nombres`.** Ese lote cambia de dónde sale el nombre de la cuenta y toca
   `layout-privado.html`, que este lote también toca (menú de la cuenta). Va en una tanda posterior:
   el integrador resuelve el choque conservando las dos entradas nuevas del menú.
5. **Choque con `p2-actividad` y `p2-admin-bandeja`.** Los dos van a leer `decidida_por`; si suman
   otra migración en `propuesta`, va después de `V22`.
6. **Datos personales.** El nombre del autor se lee de Keycloak al mostrar, no se copia a Donaciones.
   La contraseña provisoria no se guarda ni se loguea; los tests usan correos de `example.com`.
