# Matriz de permisos — DonaTrack (E6)

> **Estado:** borrador para aprobar (2026-11-07). **Actualización 2026-11-10 (leído en el código, sin correr tests):** Los cuatro servicios aplican la matriz (`@PreAuthorize`, `PermisoDonaciones`, `PermisoLogistica`, `PermisoIncentivos` y `PermisoNotificaciones`, pertenencia por el claim `personaId`) y el realm y el cliente liviano llevan los permisos y las plantillas. En Logística, Incentivos y Notificaciones sí comparé las reglas (§6, §7 y §8) contra los controllers (`scripts/tests/test_logistica_seguridad.py`, `test_incentivos_seguridad.py` y `test_notificaciones_seguridad.py`); en Donaciones no. La aprobación del equipo sigue pendiente.
> **Origen:** RX-09 del plan de autenticación del sitio; cubre la "matriz de §6" que cita RI-SVC-02 de los [requerimientos de E6](../../requerimientos/requerimientos-e6.md) y que no existía.
> **Base:** endpoints de [`contratos-rest.md`](../contratos-rest.md), contrastados con los `@*Mapping` de los controllers en `ENTREGA_6` (`e43e50a9`).

## 1. Cómo se lee

Cada endpoint pasa hasta tres chequeos. Los tres salen del token, nunca de un parámetro que manda quien llama (RI-SVC-03).

| Chequeo | Pregunta | Sale de | Cómo se expresa en el servicio |
|---|---|---|---|
| **Rol** | ¿A qué área entra? | Claim `roles`: `DONANTE`, `ENTIDAD`, `ADMIN`, y el técnico `SERVICIO` | `hasRole(...)` |
| **Permiso** | ¿Puede hacer esta acción? | Claim `permisos` (roles del cliente `donatrack-api` en Keycloak) | `hasAuthority(...)` |
| **Pertenencia** | ¿El recurso es de su organización? | Claim `personaId` contra el dueño del recurso | `actor.puedeOperarSobre(dueño)` |

**Reglas generales** `[PROPOSED]`:

1. **`ADMIN` pasa todo**: en un endpoint que también usan donantes o entidades, el `ADMIN` no necesita el permiso y pasa la pertenencia (`Administrador.puedeOperarSobre` siempre da verdadero). En la tabla, "ADMIN" en la columna de rol quiere decir solo admin.
2. **`SERVICIO`** es el rol del token técnico (client credentials): el cliente liviano en el registro, n8n y las llamadas entre servicios. No lleva `personaId` ni permisos; la columna lo nombra solo donde hace falta.
3. **Denegar por defecto:** un endpoint que no está en esta matriz exige `ADMIN`.
4. **Sin token → `401`**; token válido sin rol, permiso o pertenencia → `403`. Las dos con el `ErrorResponse` común.
5. **Filtros de "mis datos":** cuando el id del dueño viaja como parámetro (`?donanteId=`, `?entidadId=`), el chequeo es sobre ese parámetro. Si un donante o una entidad lo omite, `403`: listar todo es solo de `ADMIN`.

**Tipos de pertenencia** (columna "Pertenencia"):

| Código | Regla | Ejemplo |
|---|---|---|
| — | No aplica | Catálogo de categorías |
| **id** | El id de la ruta es el dueño | `GET /api/donantes/{id}`: `id == personaId` (identidad compartida: `Donante.id = personaId`, `EntidadBeneficiaria.id = juridicaId`) |
| **param:X** | El parámetro `X` es el dueño, y es obligatorio para quien no es admin | `?donanteId=` |
| **cuerpo:X** | El campo `X` del cuerpo es el dueño | `POST /api/donaciones` con `donanteId` |
| **dueño** | Se busca el recurso y se compara su dueño; lo hace el bean de pertenencia del servicio con los servicios de lectura existentes (si el recurso no existe, ver §10) | `PUT /api/necesidades/{id}`: `necesidad.entidadId` |

## 2. Catálogo de permisos

Todos son roles del cliente `donatrack-api`, así que **los nombres son únicos en todo el sistema**: cada servicio declara los suyos en su `enum Permiso*` y un test de contrato compara los enums con el realm.

| Permiso | Servicio | Qué habilita |
|---|---|---|
| `VER_DONACIONES` | Donaciones | Ver las donaciones propias (donante) o las asignadas a la entidad |
| `REGISTRAR_DONACIONES` | Donaciones | Cargar una donación y subir las fotos de los bienes |
| `VER_NECESIDADES` | Donaciones | Ver las necesidades propias, incluidas las no públicas |
| `GESTIONAR_NECESIDADES` | Donaciones | Publicar, cambiar y dar de baja necesidades |
| `EDITAR_DATOS_ORGANIZACION` | Donaciones | Editar los datos de la persona en cuyo nombre opera (la jurídica o la humana) |
| `VER_ENTREGAS` | Logística | Seguir las entregas propias |
| `VER_RUTAS` | Logística | Ver la ruta y el camión de una entrega propia |
| `CONFIRMAR_RECEPCION` | Logística | Confirmar o rechazar la recepción y adjuntar las fotos |
| `VER_INCENTIVOS` | Incentivos | Ver nivel, métricas, misiones, insignias y posición en el ranking propios |
| `CONFIGURAR_INSIGNIAS` | Incentivos | Elegir qué insignias se muestran en público |
| `VER_NOTIFICACIONES` | Notificaciones | Ver las notificaciones propias y marcarlas como leídas |
| `GESTIONAR_MIEMBROS` | Cliente liviano (Keycloak) | Invitar gente a la organización, cambiarle los permisos y quitarla (RX-07). Ningún servicio lo revisa: lo usa el cliente liviano antes de llamar a la Admin API |

## 3. Plantillas

Son roles compuestos de `donatrack-api`. A cada persona se le asigna una plantilla y, si hace falta, permisos sueltos de más o de menos (RX-06). El rol de plataforma (`DONANTE` o `ENTIDAD`) va aparte.

| Permiso | `RESPONSABLE_ENTIDAD` | `OPERADOR_ENTIDAD` | `RESPONSABLE_DONANTE` | `DONANTE_PERSONA` |
|---|:-:|:-:|:-:|:-:|
| `VER_DONACIONES` | ✔ | ✔ | ✔ | ✔ |
| `REGISTRAR_DONACIONES` | | | ✔ | ✔ |
| `VER_NECESIDADES` | ✔ | ✔ | | |
| `GESTIONAR_NECESIDADES` | ✔ | ✔ | | |
| `EDITAR_DATOS_ORGANIZACION` | ✔ | | ✔ | ✔ |
| `VER_ENTREGAS` | ✔ | ✔ | ✔ | ✔ |
| `VER_RUTAS` | ✔ | | ✔ | ✔ |
| `CONFIRMAR_RECEPCION` | ✔ | ✔ | | |
| `VER_INCENTIVOS` | | | ✔ | ✔ |
| `CONFIGURAR_INSIGNIAS` | | | ✔ | ✔ |
| `VER_NOTIFICACIONES` | ✔ | ✔ | ✔ | ✔ |
| `GESTIONAR_MIEMBROS` | ✔ | | ✔ | |

- Quien registra una entidad queda con `RESPONSABLE_ENTIDAD` cuando el admin la aprueba (RX-10). Quien registra una empresa donante queda con `RESPONSABLE_DONANTE`, y una persona que dona, con `DONANTE_PERSONA`.
- `DONANTE_PERSONA` no tiene `GESTIONAR_MIEMBROS`: una persona humana no tiene organización que repartir.
- El ejemplo de diseño: Juana, `OPERADOR_ENTIDAD`. Héctor, `ENTIDAD` con `VER_RUTAS`, `CONFIRMAR_RECEPCION` y `VER_ENTREGAS` sueltos.

## 4. Rutas públicas

Sin token, en cada servicio (las declara su `ReglasDeAcceso`):

| Servicio | Ruta | Para |
|---|---|---|
| Donaciones | `GET /api/entidades/publicas` | Listado público de entidades (HB-08, D26) |
| Donaciones | `GET /api/necesidades/publicas` | Necesidades públicas (HB-08, D26) |
| Logística | `GET /api/entregas/mapa` | Mapa de entregas de la portada (RF-02) |
| Notificaciones | `POST /api/contacto` | Formulario de contacto (HB-21); la regla es por ruta, así que leer los mensajes queda en el ADMIN por `@PreAuthorize` |
| Donaciones | `GET /api/donaciones/destacadas` | Destacadas de la landing (HB-01, D26) |
| Donaciones | `GET /api/donaciones/contadores` | Contadores de la landing (HB-02, D26) |
| Todos | `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**` | Healthcheck del compose y documentación en desarrollo |

`[OBSERVED]` (2026-11-11) La allowlist del gateway (`donatrack.gateway.rutas-publicas`) es exactamente esta tabla sin la última fila: las seis rutas de las cuatro primeras filas. Un test estático (`scripts/tests/test_gateway_seguridad.py`) lo exige. El gateway abre la ruta sin distinguir método, así que `GET /api/contacto` pasa el gateway sin token y lo rechaza Notificaciones.

En el cliente liviano son públicas la portada y las pantallas `publico-*` (donaciones, mapa, entidades, contacto, legales y registro). Ingresar y recuperar la contraseña son pantallas de Keycloak.

## 5. Donaciones (completo)

| Método | Endpoint | Rol | Permiso | Pertenencia | Lo usa la UI |
|---|---|---|---|---|---|
| `POST` | `/api/donaciones` | `DONANTE` | `REGISTRAR_DONACIONES` | cuerpo:`donanteId` | Donante (cargar donación) |
| `GET` | `/api/donaciones` | ADMIN | — | — | |
| `GET` | `/api/donaciones/{id}` | `DONANTE` | `VER_DONACIONES` | dueño (`donanteId`) | Donante (detalle) |
| `POST` | `/api/donaciones/fotos` | `DONANTE` | `REGISTRAR_DONACIONES` | — (URL prefirmada, sin recurso) | Donante |
| `GET` | `/donaciones-independientes` | `DONANTE`, `ENTIDAD` | `VER_DONACIONES` | param:`donanteId` (donante) o param:`entidadId` (entidad) | Donante (mis donaciones), entidad (recibidas) |
| `GET` | `/donaciones-independientes/{id}` | `DONANTE` (ver §10) | `VER_DONACIONES` | dueño (donante de la donación original) | Donante (detalle) |
| `PATCH` | `/donaciones-independientes/{id}/estado` | ADMIN | — | — | |
| `POST` | `/api/necesidades` | `ENTIDAD` | `GESTIONAR_NECESIDADES` | cuerpo:`idEntidad` | Entidad (publicar) |
| `GET` | `/api/necesidades` | `ENTIDAD` | `VER_NECESIDADES` | param:`entidadId` | Entidad (mis necesidades) |
| `GET` | `/api/necesidades/publicas` | público | — | — | Público |
| `GET` | `/api/necesidades/{id}` | `ENTIDAD` | `VER_NECESIDADES` | dueño (entidad de la necesidad) | Entidad |
| `GET` | `/api/necesidades/{id}/periodos` | `ENTIDAD` | `VER_NECESIDADES` | dueño (entidad de la necesidad) | Entidad (historia de la recurrente, HB-18) |
| `PUT` | `/api/necesidades/{id}` | `ENTIDAD` | `GESTIONAR_NECESIDADES` | dueño (entidad de la necesidad) | Entidad (cambiar) |
| `DELETE` | `/api/necesidades/{id}` | `ENTIDAD` | `GESTIONAR_NECESIDADES` | dueño (entidad de la necesidad) | Entidad (baja) |
| `POST` | `/api/asignaciones/ejecuciones` | ADMIN, `SERVICIO` | — | — | |
| `GET` | `/api/asignaciones/ejecuciones` | ADMIN | — | — | |
| `GET` | `/api/asignaciones/propuestas` | ADMIN | — | — | |
| `PUT` | `/api/asignaciones/propuestas/{id}/estado` | ADMIN | — | — | |
| `POST` | `/api/entidades` | ADMIN | — | — | Admin (aprobar entidad, RX-10) |
| `GET` | `/api/entidades` | ADMIN | — | — | |
| `GET` | `/api/entidades/publicas` | público | — | — | Público |
| `GET` | `/api/entidades/{id}` | `ENTIDAD` | — | id | Entidad (su perfil) |
| `PUT` | `/api/entidades/{id}` | `ENTIDAD` | `EDITAR_DATOS_ORGANIZACION` | id | Entidad |
| `DELETE` | `/api/entidades/{id}` | ADMIN | — | — | |
| `POST` | `/api/donantes` | `SERVICIO` | — | — | Registro (Fase 7, token técnico) |
| `GET` | `/api/donantes` | ADMIN | — | — | |
| `GET` | `/api/donantes/{id}` | `DONANTE` | — | id | Donante (perfil) |
| `DELETE` | `/api/donantes/{id}` | ADMIN | — | — | |
| `PATCH` | `/api/donantes/{id}/preferencias` | `DONANTE` | — | id | Donante (Mi perfil, HB-20) |
| `POST` | `/api/donantes/archivos` | ADMIN | — | — | Admin (CSV) |
| `GET` | `/api/donantes/archivos/{id}` | ADMIN | — | — | Admin (CSV) |
| `POST` | `/api/personas` | `SERVICIO` | — | — | Registro (Fase 7, token técnico) |
| `GET` | `/api/personas` | ADMIN | — | — | |
| `PUT` | `/api/personas/{id}` | `DONANTE`, `ENTIDAD` | `EDITAR_DATOS_ORGANIZACION` | id | Donante (perfil) |
| `DELETE` | `/api/personas/{id}` | ADMIN, `SERVICIO` | — | — | `SERVICIO`: compensación del registro si falla (Fase 7) |
| `GET` | `/api/personas/{id}/legales` | `DONANTE`, `ENTIDAD`, `SERVICIO` | — | id (`SERVICIO` sin pertenencia) | Registro y entrada al sitio (HB-20) |
| `PUT` | `/api/personas/{id}/legales` | `DONANTE`, `ENTIDAD`, `SERVICIO` | — | id (`SERVICIO` sin pertenencia) | `SERVICIO`: al completar el perfil, con la casilla marcada; la persona: al cambiar la versión vigente (HB-20) |
| `GET` | `/api/categorias`, `/api/categorias/{id}` | cualquier rol | — | — | Formularios de donación y necesidad |
| `POST`, `PUT`, `DELETE` | `/api/categorias/**` | ADMIN | — | — | |
| `GET` | `/api/subcategorias`, `/api/subcategorias/{id}` | cualquier rol | — | — | Formularios de donación y necesidad |
| `POST`, `PUT`, `DELETE` | `/api/subcategorias/**` (incluye `aliases`) | ADMIN | — | — | |
| `GET`, `PATCH` | `/api/items-normalizados/**` | ADMIN | — | — | |
| `GET`, `PUT` | `/api/logistica/proveedores`, `/api/logistica/proveedor-preferido` | ADMIN | — | — | Hoy con `X-API-Key`; queda así hasta E2-07 (ver §10) |

## 6. Logística (lo que usa la UI y lo técnico)

| Método | Endpoint | Rol | Permiso | Pertenencia | Lo usa la UI |
|---|---|---|---|---|---|
| `GET` | `/api/entregas/mapa` | público | — | — | Público (mapa) |
| `GET` | `/api/entregas/seguimiento/donaciones/{idDonacion}` | `DONANTE` | `VER_ENTREGAS` | dueño (donante de la donación, se consulta a Donaciones o por `donanteId` de HB-16) | Donante (seguimiento) |
| `GET` | `/api/entregas/seguimiento/beneficiarios/{idBeneficiaria}` | `ENTIDAD` | `VER_ENTREGAS` | id | Entidad (seguimiento, recibidas) |
| `GET` | `/api/entregas/{id}` | `ENTIDAD` | `VER_ENTREGAS` | dueño (`idBeneficiaria`) | Entidad (confirmar) |
| `PATCH` | `/api/entregas/{id}/estado` | `ENTIDAD` | `CONFIRMAR_RECEPCION` | dueño (`idBeneficiaria`) | Entidad (confirmar recepción) |
| `PATCH` | `/api/entregas/{id}/fotos` | `ENTIDAD` | `CONFIRMAR_RECEPCION` | dueño (`idBeneficiaria`) | Entidad (fotos) |
| `GET` | `/api/entregas/{id}/historial` | `ENTIDAD` | `VER_ENTREGAS` | dueño (`idBeneficiaria`) | Entidad |
| `GET` | `/api/entregas/seguimiento/donantes/{donanteId}` | `DONANTE` | `VER_ENTREGAS` | id | Donante (seguimiento, HB-16) `[PROPOSED]` |
| `POST` | `/api/entregas/{id}/fotos` | `ENTIDAD` | `CONFIRMAR_RECEPCION` | dueño (`idBeneficiaria`) | Entidad (subir las fotos de recepción, HB-10) `[PROPOSED]` |
| `GET` | `/api/entregas/{id}/fotos/{fotoId}` | `ENTIDAD` | `VER_ENTREGAS` | dueño (`idBeneficiaria`) | Entidad (ver la foto, HB-10) `[PROPOSED]` |
| `GET` | `/api/rutas/{id}` | `DONANTE`, `ENTIDAD` | `VER_RUTAS` | dueño (la ruta lleva una entrega de su organización) | Seguimiento con mapa |
| `POST` | `/api/entregas` | ADMIN, `SERVICIO` | — | — | |
| `GET` | `/api/entregas` | ADMIN | — | — | |
| `POST` | `/api/logistica/resultados`, `/api/logistica/callback/rutas` | `SERVICIO` | — | — | Callback de n8n |
| `POST` | `/api/logistica/planificaciones/ejecuciones` | ADMIN | — | — | |
| `GET` | `/api/logistica/planificaciones/{id}` | ADMIN | — | — | |
| todos | `/api/rutas` (resto), `/api/camiones/**`, `/api/choferes/**` | ADMIN | — | — | |

`[PROPOSED]` (2026-11-10, al implementar Logística) Las tres filas marcadas no estaban cuando se escribió la matriz: HB-16 y HB-10 son posteriores. Se les dio la regla de su par (`seguimiento/donaciones` y `PATCH /fotos`) para que no queden en el ADMIN por defecto. Falta decidir si el donante puede ver la foto de recepción de su donación.

`[OBSERVED]` El dueño sale de la propia entrega (`beneficiariaDe`, `donanteDeDonacion`) y de las entregas de una ruta (`organizacionesDe`); un recurso inexistente no tiene dueño, así que quien no es admin recibe `403` y no puede averiguar qué ids existen. El donante de una entrega solo se conoce si ella llegó por mensajería: una entrega creada por `POST /api/entregas` no lo tiene y solo la ve el ADMIN. `GET /api/entregas/{id}/fotos/{fotoId}` responde `302` a una URL firmada: un `<img>` del navegador no manda el token, así que el cliente liviano tiene que pedirla por el servidor. Con `LOGISTICA_SEGURIDAD_HABILITADA=true`, los flujos 6, 8 y 9 de Postman y `LogisticaApiClient` de `integration-tests` llamaban sin token y recibirían `401`: desde la iteración 5 piden el token de `donatrack-pruebas` (M-07). `ProveedorLogisticaHttp` de Donaciones **no** entra en la lista (la versión anterior de esta nota lo incluía por error): habla con el proveedor externo, no con este servicio, porque el proveedor interno va por AMQP.

## 7. Incentivos (lo que usa la UI y lo técnico)

`donanteId` es el `personaId` del donante (identidad compartida), así que la pertenencia es **id** en todo el servicio.

| Método | Endpoint | Rol | Permiso | Pertenencia | Lo usa la UI |
|---|---|---|---|---|---|
| `GET` | `/api/incentivos/donantes/{donanteId}` y `/ascensos`, `/metricas`, `/misiones`, `/insignias` | `DONANTE` | `VER_INCENTIVOS` | id | Donante (inicio, perfil) |
| `PUT` | `/api/incentivos/donantes/{donanteId}/insignias/{nombreInsignia}/visibilidad` | `DONANTE` | `CONFIGURAR_INSIGNIAS` | id | Donante (perfil) |
| `GET` | `/api/incentivos/ranking/posicion/{donanteId}` | `DONANTE` | `VER_INCENTIVOS` | id | Donante (inicio) |
| `GET` | `/api/incentivos/ranking/ultimo`, `/historial`, `/{periodo}` | `DONANTE` | `VER_INCENTIVOS` | — (ranking general) | Donante |
| `POST`, `PATCH`, `DELETE` | `/api/incentivos/donantes/{donanteId}` | `SERVICIO`, ADMIN | — | — | |
| `POST` | `/api/incentivos/donaciones`, `/donaciones/exitosa` | `SERVICIO` | — | — | |
| `POST` | `/api/incentivos/ranking/calcular`, `/ranking/notificaciones`, `/evaluaciones-inactividad`, `/verificaciones-racha` | `SERVICIO`, ADMIN | — | — | Jobs y n8n (DTI-09) |
| `GET` | `/api/incentivos/admin/resumen` | ADMIN | — | — | Admin (inicio) |

`[OBSERVED]` (2026-11-10, al implementar Incentivos) `POST /api/incentivos/ranking/notificaciones` figura en la fila de jobs pero **no existe** en el código: no tiene regla. Con `INCENTIVOS_SEGURIDAD_HABILITADA=true`, los flujos 5 y 8 de Postman y `IncentivosApiClient` de `integration-tests` llamaban sin token y recibirían `401`: desde la iteración 5 piden el token de `donatrack-pruebas` (M-07).

## 8. Notificaciones (lo que usa la UI y lo técnico)

`[INFERRED]` Las notificaciones se dirigen a la persona en cuyo nombre se opera (la del `personaId`). Si se dirigen a la humana que representa a una jurídica, la pertenencia cambia y hay que revisar esta sección.

| Método | Endpoint | Rol | Permiso | Pertenencia | Lo usa la UI |
|---|---|---|---|---|---|
| `GET` | `/api/notificaciones/persona/{personaId}` | `DONANTE`, `ENTIDAD` | `VER_NOTIFICACIONES` | id | Donante y entidad (notificaciones) |
| `GET` | `/api/notificaciones/persona/{personaId}/no-leidas/conteo` | `DONANTE`, `ENTIDAD` | `VER_NOTIFICACIONES` | id | Menú (contador) |
| `PATCH` | `/api/notificaciones/{id}/leida` | `DONANTE`, `ENTIDAD` | `VER_NOTIFICACIONES` | dueño (`personaId`) | Marcar leída |
| `PATCH` | `/api/notificaciones/persona/{personaId}/marcar-todas-leidas` | `DONANTE`, `ENTIDAD` | `VER_NOTIFICACIONES` | id | Marcar todas |
| `GET` | `/api/notificaciones`, `/api/notificaciones/{id}` | ADMIN | — | — | |
| `POST` | `/api/notificaciones/eventos`, `/notificaciones` (legado, DTI-13) | `SERVICIO` | — | — | Incentivos por Feign |
| `GET` | `/notificaciones/persona/{personaId}` (alias QA) | ADMIN | — | — | |
| `PUT`, `GET`, `DELETE` | `/api/notificaciones/personas/**` | `SERVICIO`, ADMIN | — | — | Réplica de personas |
| `POST` | `/api/contacto` | público | — | — | Contacto (HB-21; límite por correo e IP) |
| `GET` | `/api/contacto`, `/api/contacto/{numero}` | ADMIN | — | — | Sin pantalla todavía (HB-21) |
| `POST` | `/api/contacto/{numero}/respuesta` | ADMIN | — | — | Sin pantalla todavía (HB-21) |

`[OBSERVED]` (2026-11-10, al implementar Notificaciones) El alias `GET /notificaciones/persona/{personaId}` compartía método con la ruta `/api/…`, y la matriz les da reglas distintas (ADMIN y dueño): pasó a ser un método propio, que solo entra el ADMIN. La pertenencia de `PATCH /{id}/leida` se resuelve con `NotificacionService.personaDe` (la notificación guarda su `personaId`, el DTO no): una notificación inexistente no tiene dueño, así que quien no es admin recibe `403` y no puede averiguar qué ids existen. Con `NOTIFICACIONES_SEGURIDAD_HABILITADA=true`, los flujos 7 y 8 de Postman y los clientes de `integration-tests` llamaban sin token y recibirían `401`: desde la iteración 5 piden el token de `donatrack-pruebas` (M-07). `NotificacionesFeignClient` de Incentivos **no** manda token: es la vía legada (`incentivos.rabbitmq.enabled=false`); por defecto Incentivos usa RabbitMQ, que esta seguridad no toca. Con esa vía legada y la seguridad de Notificaciones encendida, la notificación falla con `401` y el adapter lo registra sin cortar la operación del donante.

`[OBSERVED]` (2026-11-11, al integrar ENTREGA_6) El contacto (HB-21) llegó después de esta sección: `POST /api/contacto` es la ruta pública de Notificaciones (`ReglasDeAccesoNotificaciones` y `@PreAuthorize(PUBLICO)`), y leer y responder mensajes heredan el ADMIN de `ContactoController`. Como la regla pública es por ruta y no por método, `GET /api/contacto` sin token responde `403` en vez de `401`.

## 9. Decisiones para aprobar

| # | Decisión propuesta | Alternativa |
|---|---|---|
| M-01 | `ADMIN` pasa permiso y pertenencia en todos los endpoints compartidos | Darle al admin también la plantilla de permisos: duplica datos y se desincroniza |
| M-02 | El permiso controla la acción y la pertenencia controla el recurso. Las reglas de estado (por ejemplo, que la entidad solo pueda llevar una entrega a `ENTREGADA` o `NO_RECIBIDA`) siguen en el dominio | Un permiso por transición: multiplica el catálogo |
| M-03 | El registro (Fase 7) usa el token técnico `SERVICIO` para `POST /api/personas`, `POST /api/donantes` y la compensación `DELETE /api/personas/{id}`. La aprobación de entidad usa el token del admin | Dar `POST /api/personas` a cualquier usuario: deja crear personas a nombre de otros |
| M-04 | Categorías y subcategorías son legibles por cualquier usuario con sesión | Hacerlas públicas: hoy no las usa ninguna pantalla pública |
| M-05 | Los listados con filtro de dueño exigen el filtro a quien no es admin (regla 5 de §1) | Que el servicio complete el filtro con el `personaId` del token: es más cómodo, pero cambia la semántica del endpoint según quién llame |
| M-06 | Los endpoints `X-API-Key` del broker se marcan `ADMIN` en la matriz, pero se cambian recién en E2-07 | Cortarlos en E6, como dice D25: queda fuera del alcance de este plan |
| M-07 | En el compose, la validación se activa con `DONACIONES_SEGURIDAD_HABILITADA`, `INCENTIVOS_SEGURIDAD_HABILITADA`, `NOTIFICACIONES_SEGURIDAD_HABILITADA` y `LOGISTICA_SEGURIDAD_HABILITADA` (por defecto `false`). Con `--auth` (o `PREPROD_AUTH=true` en CI) el preprod las enciende, junto con `GATEWAY_SEGURIDAD_HABILITADA`, vía `PREPROD_SEGURIDAD`. Postman, `integration-tests` y k6 piden un token a la cuenta `donatrack-pruebas` si tienen `PRUEBAS_CLIENT_SECRET`; sin él siguen sin token, como con la seguridad apagada | Activarla siempre: el preprod y el CI cambian de golpe, y no se pudo probar fuera de la copia de trabajo |
| M-08 | Una sola cuenta de servicio nueva, `donatrack-pruebas` con rol `ADMIN`, para `integration-tests`, k6 y Postman con la seguridad encendida. No hacen falta tokens entre servicios: no hay llamadas HTTP internas en el despliegue estándar (Incentivos usa RabbitMQ, el proveedor interno de logística va por AMQP y n8n solo llama a mocks externos). Es la credencial más poderosa del realm: solo client credentials, solo `ADMIN` en su token y sin poder sobre Keycloak, y su secreto tiene que ser aleatorio en un entorno real | Una cuenta `SERVICIO` por servicio (`incentivos-service`, `donaciones-service`): código y credenciales sin ningún llamador. Usuarios de prueba por rol: más fieles, pero el `ADMIN` pasa todas las reglas y los `401` y `403` ya los cubre el test de seguridad de cada servicio |
| M-09 | `X-Actor` deja de ser obligatorio: con token el actor de la auditoría es el `sub` (RI-SVC-04); sin token (seguridad apagada) sigue valiendo el encabezado y su falta responde `400` como hoy | Quitar el encabezado ya: rompe a los llamadores mientras la seguridad sea opcional. Dejarlo obligatorio: cualquiera con un token podría firmar una acción como otro |
| M-10 | `X-API-Key` se mantiene en `ApiKeyFilter` y en el flujo 9 de Postman: lo usa el proveedor externo, que no es un cliente nuestro. Retirarlo exige una cuenta propia para ese proveedor (D25) | Retirarlo en esta etapa: el proveedor externo quedaría sin forma de autenticarse |

## 10. Pendiente de revisión

Provisorio: ya está implementado en Donaciones y falta la revisión de Tadeo. No son reglas confirmadas.

1. `GET /donaciones-independientes/{id}`: solo `DONANTE` (pertenencia por la donación original) y `ADMIN`. El DTO no expone la entidad asignada; la entidad las ve con el listado filtrado por `entidadId`.
2. Un recurso inexistente responde `404` antes de evaluar la pertenencia, así que quien no es admin puede saber si un id existe.
3. `/api/logistica/proveedores` y `/api/logistica/proveedor-preferido`: `ADMIN` con la seguridad activa, además de `X-API-Key` (M-06).
4. Orden de las respuestas: `@PreAuthorize` se evalúa al invocar el método, después de validar el cuerpo. Un pedido con cuerpo inválido responde `400` aunque quien llama no tenga permiso; el `403` solo aparece con un cuerpo válido.
