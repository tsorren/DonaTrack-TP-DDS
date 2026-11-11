# Spec: SPEC-08 — Conexión de pantallas del cliente liviano a las APIs (E6)

> **Estado:** PROPOSED (2026-11-10, corrida desatendida `conexion-pantallas`; la aprobación es el merge de sus PRs, "rama = gate").
> **Nivel:** ARCHITECTURAL (AGENTS.md §7.0): impacto cross-service (huecos aditivos en Notificaciones y Logística) y contrato del cliente con los cuatro servicios.
> **Fecha:** 2026-11-10
> **Base:** `ENTREGA_6` (`af49d305`). Rama del esqueleto: `E6_conexion_esqueleto`; PRs en borrador hacia `ENTREGA_6`, uno por rol (`E6_conexion_<rol>`) y uno por hueco de backend.
> **Decisiones que implementa:** A1, M1, B1, E1 y F1 del [plan del cliente con datos reales y gateway](../../requerimientos/plan-e6-cliente-datos-reales-y-gateway.md) (fases F2 y F3) y [ADR-E6-02](../../adr/20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md) (`proposed`, el `X-Trace-Id`). Contacto implementa el [ADR `proposed` de HB-21](../../adr/notificaciones-service/20261110-recepcion-de-mensajes-de-contacto-en-notificaciones.md) sin promoverlo.

---

## 1. Objetivo Funcional (Goal)

Que cada pantalla del [mapa del sitio](../../interfaz/mapa-del-sitio.md) lea la API real y cada
formulario guarde de verdad fuera del perfil `maqueta`: el entregable 2 de la E6 ("cliente liviano
desacoplado, renderizando las vistas").

**Criterio de hecho:**

- Cada pantalla del mapa tiene controller en `web/` y puerto con adaptador REST y de maqueta (tests
  de `MockRestServiceServer`), o figura como pendiente con su motivo.
- Cada formulario que hoy atiende `maqueta/SitioController` llama a la API fuera de maqueta y termina
  con la pieza común de envío (POST, redirect y aviso del servidor en `#avisos`, con `hx-indicator`),
  o figura como pendiente con su motivo.
- En `maqueta`, cada pantalla da el mismo HTML que antes, salvo diferencias explicadas.
- Gates verdes (sección 6) y auditoría de cierre `PASS`.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope

- **Esqueleto compartido** (un commit, `E6: conexión de pantallas, esqueleto compartido`):
  - `rest/DonacionesApi`, `LogisticaApi`, `IncentivosApi` y la nueva `NotificacionesApi`: todos los
    endpoints que usan las pantallas, con records que copian los DTOs (con envoltorios: Jackson 3
    falla con primitivos nulos). URL nueva: `cliente.api.notificaciones.url`
    (`NOTIFICACIONES_URL`, `:8081`).
  - `rest/Comun`: lo común de los adaptadores que leen donaciones (formatos de fecha, estados,
    categoría, cantidad e ícono), sacado de `DonacionesRest` para que lo usen varios frentes.
  - Las claves `cliente.datos.<clave>` de cada frente (en `application.properties`, `rest`; en
    `application-maqueta.properties`, `maqueta`): `publico`, `contacto`, `donante`, `misiones`,
    `entidad`, `admin-personas`, `admin-operacion` y `avisos`, más las que ya estaban (`ranking`,
    `donaciones`, `recibidas`).
  - El conteo de sin leer de la cabecera, compartido por los tres roles: puerto `AvisosSinLeer`,
    adaptador `rest/AvisosRest` (Notificaciones) y el de maqueta en `MaquetaConfig`; `Pantallas` lo
    pone en el modelo (`sinLeer`) y `layout-privado` deja de tener los números escritos.
  - La pieza común de envío: `web/Envios.listo(destino, texto, atributos)` (redirect y aviso),
    `templates/envio.html` (`aviso` e `indicador`), la región `#avisos` de los dos layouts y el
    indicador de envío en el catálogo y en `/estilos` (`.htmx-indicator` y `.htmx-request` sobre
    `.cargando`, en `componentes/carga.css`).
  - `interaccion.html`: `responseHandling` de HTMX para que una respuesta 4xx o 5xx de un envío o un
    filtro con `hx-boost` muestre su página de error.
  - `web/PublicoPortadaController` (la portada `/`, que salía de `PantallasController`), para que el
    frente público le sume datos sin tocar lo compartido.
  - Tests compartidos que no dependen de los datos de una pantalla: `SitioControllerTest`,
    `MaquetaAbiertaTest` y `VariantesInterceptorTest` con el contexto entero del perfil `maqueta`;
    `SeguridadTest` y `CompletarPerfilTest` con pantallas sin datos; las pruebas de las necesidades y
    de las solicitudes de entidad, en su propio archivo (`NecesidadesConIngresoTest`,
    `SolicitudesConIngresoTest`). En `SeguridadTest`, `/` pasa de "200" a "no manda a ingresar":
    la portada ya no está en `PantallasController` y el corte no carga `PublicoPortadaController`,
    que el frente público cambia; la regla de la cadena que se prueba es la misma.
  - `ContratoDeLasApisTest`: cada forma nueva de las `*Api` una vez (filtros, cabecera con el total,
    multipart, `X-Actor`, `X-Forwarded-For`, respuestas sin cuerpo y el alta de una donación).
- **Frentes en paralelo** (sección 3.2), cada uno con su lista cerrada de archivos.
- **Huecos de backend** que cumplen el criterio de aditivo mínimo, más Contacto (sección 3.3).
- **Pendientes del cliente con el backend ya mergeado:** la constancia de legales en el registro, el
  nombre visible en Mi perfil y "Figura como", y `quienDono` en Donaciones recibidas.

### Out-of-Scope

- El token hacia Logística, Incentivos y Notificaciones, el JWT en el gateway y en los servicios
  (F5, HB-13) y el cliente en el compose (F6).
- Estilos, tokens, clases y hojas de pantalla nuevos: solo el indicador de envío, sobre `.cargando`.
- Lo que no cumple el criterio de aditivo mínimo (sección 3.4): la pantalla conecta perdiendo el dato.
- El documento de la E6 para la cátedra. Mergear.

## 3. Decisiones Acordadas y Trade-offs

### 3.1 Inventario

Re-verificado contra `af49d305` (controllers, `@PreAuthorize` y DTOs). Resumen:

- **Cambió desde el informe de brechas del 07/11:** Confirmar recepción ya puede subir fotos
  (`POST /api/entregas/{id}/fotos`, multipart); el ranking ya trae `calculadoEn`; el seguimiento,
  `posicionCamion`; las métricas, la misión activa con su progreso; y HB-20 está en `ENTREGA_6`.
- **Quién llama:** `GET /api/entidades/{id}` es solo de ADMIN o de la entidad dueña, y
  `GET /api/donaciones/{id}`, del donante o ADMIN: el donante resuelve el nombre de una entidad con
  `/api/entidades/publicas`, y la entidad, quién donó con `quienDono` del listado.
- **Ids:** la cuenta es la persona; en Donaciones, el id del donante y el de la entidad son el de su
  persona; Notificaciones e Incentivos usan la persona.
- **Pantallas del mapa (41):** 🟢/🟡 todas las de los cuatro roles, salvo las que resuelve Keycloak
  (recuperar, activar, ingresar) y Contacto, que espera HB-21.
- **Formularios:** los que atiende `SitioController` tienen endpoint, salvo los de Keycloak
  (recuperar, activar e ingresar) y dos sin operación en la API (corrección y rechazo de una entidad,
  preferencias de avisos).

### 3.2 Frentes

| Frente | Rol | Puerto de la maqueta (y +100) | Pantallas |
|---|---|---|---|
| `publico` | público | 8091 | portada, donaciones realizadas, mapa, entidades y necesidades, nosotros, para donantes, para entidades, legales, contacto, registrarse, registro de donante y de entidad |
| `donante` | donante | 8092 | inicio, mis donaciones y detalle, entidades, misiones, seguimiento, notificaciones, mi perfil |
| `entidad` | entidad | 8093 | inicio, necesidades, recibidas, seguir entregas y confirmar recepción, notificaciones, mi perfil |
| `admin-personas` | admin | 8094 | inicio, donantes (registrar e importar CSV), entidades (registrar y aprobar) |
| `admin-operacion` | admin | 8095 | donaciones (registrar, detalle y vencida), asignaciones, camiones, ranking |

Reglas para todos:

- Clases nuevas con el prefijo de su rol (`Publico*`, `Donante*`, `Entidad*`, `AdminInicio*`,
  `AdminDonantes*`, `AdminEntidades*`, `AdminPersonas*`, `AdminDonaciones*`, `AdminAsignaciones*`,
  `AdminCamiones*`, `AdminOperacion*`); la interfaz de cada puerto es del frente, con la clave de
  `cliente.datos` que fijó el esqueleto.
- Lo compartido no se edita: `*Api`, `ClientesHttp`, `Comun`, `AvisosRest`, `Envios`, `Pantallas`,
  `PantallasController`, `SitioController`, `MaquetaConfig`, `CuentaDeEjemplo`, las propiedades, los
  layouts, `envio`, `componentes/`, `estilos`, las plantillas sin rol, `EstadoEntrega`,
  `PasoRecorrido`, `PersonaNueva`, `DonacionesComoUsuario` y los tests compartidos. Las fichas que
  cruzan roles (`raices-entidad-navegable`, `raices-admin-navegable`, `raices-mi-perfil`,
  `raices-seguir-entregas`) tampoco: sus textos los pasa el integrador.
- `AccesoController`, `MisDonacionesController` y `RecibidasController` mantienen sus dependencias
  (las nombran `SeguridadTest` y `CuentaDelTokenTest`). Un componente compartido (`huerta`, `buzon`)
  recibe las mismas variables que hoy.
- La receta: los datos de ejemplo pasan a records del adaptador de maqueta (M1); el adaptador REST
  con `MockRestServiceServer`; el controller en `web/` con `Pantallas.mostrar`; la plantilla solo
  cambia de dónde lee. Cada envío real va en un controller `@Profile("!maqueta")` con `th:action`
  (token CSRF), `hx-boost`, `hx-indicator` y `hx-select-oob="#avisos"`, y termina con
  `Envios.listo`; en maqueta lo sigue atendiendo `SitioController`. Fuera de maqueta el destino es la
  pantalla base con el aviso; las variantes `-guardado`, `-agregado` y parecidas son de la maqueta.

### 3.3 Huecos de backend que entran

| Hueco | Servicio | Contrato | Pantallas |
|---|---|---|---|
| HB-21 Contacto | Notificaciones | `POST /api/contacto` con `X-Forwarded-For`; cuerpo `{tema: ENTREGA\|DONACION\|NECESIDAD\|CUENTA\|SITIO\|OTRO, texto (1..2000), codigoError?, correo, claveEnvio (uuid)}`; `201 {numero}`, la misma clave `200 {numero}` del primero, `400` y `429`. Del ADR, solo ADMIN y sin pantalla: `GET /api/contacto?estado=&pagina=`, `GET /api/contacto/{numero}`, `POST /api/contacto/{numero}/respuesta {texto}` | publico-contacto |
| HB-23 Camiones deshabilitados (id nuevo: el hueco lo registra en la tabla de huecos de `requerimientos-e6.md`) | Logística | `GET /api/camiones?incluirDeshabilitados=true` → todos los camiones, también los `DESHABILITADO`; sin el parámetro o `false`, como hoy | admin-camiones |

### 3.4 Lo que queda fuera (la pantalla conecta perdiendo el dato)

- Tipo de evento e id de la donación en las notificaciones (donante y entidad): cambia los eventos de
  dominio de Notificaciones.
- HB-22, la entrega que llegó a la puerta (recibidas, inicio y seguimiento de la entidad): dos servicios.
- Barrio, "qué es" y lo recibido en lo público, y categoría, entidad y foto en el mapa: los DTOs
  públicos son mínimos a propósito (HB-08, D24).
- Lo recibido de una necesidad extraordinaria; motivo y fecha de la decisión de una propuesta y de un
  cambio de camión; corrección y rechazo de una entidad; búsqueda y fecha de alta de donantes;
  preferencias de avisos y "qué es" de la entidad; unidad del progreso y número de misión.
- La variante de la persona fuera de maqueta (Misiones en el menú y `templates/persona/`), la bandeja
  de la Administración (fuera del mapa) y las pantallas que resuelve Keycloak.
- `Pantallas` sigue poniendo en el modelo los datos de ejemplo de `pasos` (y `estados`), que leen
  `viaje-datos` y `persona/datos-persona`: quedan congelados, sin dueño, también fuera de maqueta.

### 3.5 Alternativas (Trade-off)

| Criterio (peso) | **A. Esqueleto + frentes por rol con lista cerrada** | B. Un agente por pantalla, en serie | C. Frentes sin esqueleto |
|---|:-:|:-:|:-:|
| Entrega antes del 13/11 (30) | 5 | 1 | 5 |
| Sin choques al integrar (30) | 4 | 5 | 1 |
| Un PR por rol revisable (20) | 5 | 3 | 4 |
| Costo del esqueleto (20) | 3 | 5 | 5 |
| **Total** | **4,3** | 3,3 | 3,6 |

- **A** fija una vez todo lo que dos frentes tocarían (APIs, claves, piezas, tests compartidos).
- B no entra en el plazo. C deja a cada frente editando `*Api` y las propiedades: choques seguros.
- Pieza de envío: **aviso por flash y `hx-select-oob`** frente a un fragmento de aviso devuelto por
  HTMX (`HX-Trigger`): el primero funciona igual sin JavaScript (la página trae el aviso) y no suma
  endpoints de fragmentos.
- Tests compartidos con el **contexto entero de maqueta** frente a listar controllers: la lista
  obligaría a cada frente a tocar el mismo archivo.

## 4. Invariantes

- En `maqueta`, el HTML de cada pantalla queda igual (comparación con `curl` antes y después).
- Ningún frente toca un archivo de otro ni uno compartido; dos frentes nunca comparten un archivo.
- Sin marcado, clases ni estilos nuevos en las plantillas, salvo el indicador de envío del catálogo
  y el campo trampa de Contacto (`<p hidden>` con el campo `sitio`), que pide el ADR de HB-21: no
  se ve ni se lee, así que la pantalla queda igual.
- Todo envío real funciona sin JavaScript (formulario con `action` real) y lleva el token CSRF.
- Contratos de backend aditivos: nada que ya existe cambia (AGENTS.md §8.2).
- Sin secretos ni datos reales; los ejemplos son sintéticos. Ningún ADR se promueve.

## 5. Criterios de Aceptación

```gherkin
Escenario: una pantalla conectada en maqueta
  Dado el perfil maqueta
  Cuando se pide una pantalla conectada en cualquiera de sus escenarios
  Entonces el HTML es el mismo que antes de conectarla

Escenario: un envío real
  Dado el sitio fuera de maqueta y una cuenta con sesión
  Cuando la persona envía un formulario conectado
  Entonces el cliente llama a la API, redirige a la pantalla y el aviso del servidor aparece en #avisos
  Y mientras dura el pedido se ve el indicador de envío

Escenario: la API falla durante un envío con HTMX
  Dado un formulario con hx-boost
  Cuando la API responde con un error
  Entonces se muestra la página de error en el contenido, como sin JavaScript

Escenario: la cabecera de las pantallas privadas
  Dado una cuenta de donante o de entidad fuera de maqueta
  Cuando abre una pantalla privada
  Entonces el número de sin leer sale de Notificaciones, y sin respuesta la cabecera queda sin número
```

## 6. Validación (Validation)

- `mvn spotless:check`; `mvn clean test -pl cliente-liviano -am` (incluye `InterfazFitnessTest`).
- Verificación visual de las pantallas tocadas (`-Pvisual`, `reporte.json` en `[]`).
- HTML de la maqueta antes y después, por frente y para el esqueleto.
- Gate 2 de cada servicio con hueco (`mvn clean test -pl <servicio> -am`); Testcontainers sin
  Docker → `[DEFERRED_NO_DOCKER]` (AGENTS.md §11.3). Gates 3 y 4: diferidos (sin cliente en el compose).
- `node scripts/agent-check.js` con 0 FAIL.

## 7. Especificación Técnica de Diseño (Technical Design)

- **Two-Gate (AGENTS.md §9.1):** Gate A falso. La corrida implementa decisiones ya tomadas (plan del
  cliente: A1, M1, B1, E1, F1; ADR-E6-02; ADR `proposed` de HB-21). El esqueleto suma piezas de
  implementación dentro del stack decidido (D31–D33: HTMX y la cola de avisos de Alpine) y HB-23 sigue
  un patrón existente de Logística. Sin ADR nuevo.
- **Plan TDD por frente:** primero el test del adaptador REST con el JSON del contrato
  (`MockRestServiceServer`), después el adaptador de maqueta con los datos de la plantilla y el test
  del controller (MockMvc, maqueta y `!maqueta`), y al final la plantilla. Para los huecos, el test
  del controller y del servicio con el contrato de la sección 3.3.
- **Pruebas del esqueleto:** `EnviosTest` (el POST deja el aviso y la pantalla lo muestra en `#avisos`
  de los dos layouts), `AvisosRestTest` (conteo, admin sin persona y Notificaciones caído) y los
  tests compartidos con el contexto entero.

## 8. Riesgos

- Los PRs de rol parten del mismo commit del esqueleto: se mergean en orden (backend → público →
  donante → entidad → admin). Si `ENTREGA_6` se mueve, `git merge origin/ENTREGA_6` en cada rama.
- El cliente se prueba contra el contrato con mocks: la prueba de punta a punta es F6.
- Si un hueco de backend no termina, sus pantallas quedan programadas contra un contrato que no está
  en `ENTREGA_6`; el reporte lo dice pantalla por pantalla.
- Textos nuevos sin aprobar: se marcan "Pendiente de OK del usuario" en su ficha y se aprueban antes
  del merge.
- Una sesión vencida durante un envío con HTMX redirige a Keycloak, que el navegador no sigue en un
  pedido de fondo: la persona recarga y vuelve a ingresar.
