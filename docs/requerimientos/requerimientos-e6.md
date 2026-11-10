# Requerimientos — Entrega 6: Interfaz de Usuario, Cliente Liviano y Autenticación

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Versión 1 · 2026-10-27 · insumo para los ADRs de E6 · para revisar con el equipo**

## 0. Cómo leer este documento

- **Para qué sirve:** el enunciado de E6 es breve. Este documento lo baja a requerimientos verificables y registra cada decisión de diseño con sus alternativas, para que de acá salgan los ADRs (§11).
- **Fuentes:**
  - Enunciado completo y final: [`Enunciado-TP-Anual-2026.pdf`](../entregas/Enunciado-TP-Anual-2026.pdf), en `docs/entregas/` desde el 2026-10-29 (§13). E6 está en las págs. 26–27; los requerimientos mínimos de UI/UX, en las págs. 9–10; el alta y la importación de donantes, en la sección de E1.
  - Bocetos y mapa del sitio de E1: [`docs/entregas/1/interfaz/`](../entregas/1/interfaz/).
  - Contratos vigentes: [`docs/arquitectura/contratos-rest.md`](../arquitectura/contratos-rest.md) y el código de `ENTREGA_5`.
- **Etiquetas** (AGENTS.md §3): `[DOCUMENTED]` lo dice la cátedra o un ADR · `[OBSERVED]` está en el código · `[INFERRED]` es una deducción · `[PROPOSED]` es una propuesta que todavía no existe.
- **Estado de cada decisión:** ✅ tomada por el equipo · ⏸️ pendiente · 🎓 pendiente de consulta a la cátedra.
- **Cobertura de cada requerimiento:** ✅ resuelto · 🟡 parcial o a verificar · ❌ falta.
- Las decisiones se numeran **D1–D33**. El detalle de cada una (alternativas, comparación y buenas prácticas) está en §9 y §10. Los requerimientos citan la decisión que los origina.
- Cuando dice **IdP** (*Identity Provider*, proveedor de identidad) se refiere al componente que hace el login y emite los tokens: Keycloak configurado, según la D1 (cerrada el 2026-11-04, ver el [ADR](../adr/auth-service/20261104-proveedor-de-identidad-keycloak.md)).

---

## 1. Objetivo y alcance

### 1.1 Lo que pide la cátedra `[DOCUMENTED]`

| ID | Requerimiento | Fuente | Cómo se verifica `[PROPOSED]` |
|---|---|---|---|
| RQ-E6-01 | Proyecto nuevo e independiente para la interfaz gráfica, desacoplado de la lógica de negocio | Enunciado E6, Requerimientos | Módulo Maven `cliente-liviano` propio, sin dependencias de los módulos de negocio (RNF-CL-01) |
| RQ-E6-02 | El cliente consume las APIs REST de las entregas anteriores | Ídem | Todas las pantallas de §6.1 obtienen sus datos por REST a través del gateway |
| RQ-E6-03 | Vistas generadas con un **motor de plantillas**, que renderizan dinámicamente los datos de las APIs | Ídem | Cada vista es una plantilla Thymeleaf renderizada en el servidor con datos de la API (D17) |
| RQ-E6-04 | **Servicio de Autenticación** que gestiona los tokens de autorización desde el servidor frontend hacia los servicios de aplicación | Ídem | §4 y §5: IdP emite los tokens y el cliente los usa hacia los servicios |
| RQ-E6-05 | Implementación de un **SSO con soporte de Social-Login** | Enunciado E6, entregable 3 | Login con usuario/contraseña y con Google sobre el mismo IdP; una sola sesión para todo el sitio (RF-AUTH-01/02) |
| RQ-E6-06 | Implementar los bocetos de interfaz de la Entrega 1 | Enunciado E6, entregable 1 | Cada pantalla del mapa del sitio de E1 tiene su vista implementada (§6.2) |
| RQ-E6-07 | Maquetado en HTML con navegabilidad entre interfaces y estilos aplicados | Enunciado E6, entregable 4 | Navegación entre todas las pantallas sin URLs a mano; estilos desde `tokens.css` |
| RQ-E6-08 | Cumplir las consideraciones de maquetado (usabilidad, navegación ≤ 3 niveles, patrones conocidos, WCAG AA, responsive, tokens de diseño, etc.) | Enunciado E6, Consideraciones para el Maquetado | §6.4, RNF-UI-01..11 |
| RQ-E6-09 | Los 20 requerimientos mínimos de UI/UX (acceso, donante, entidad, administrador) | Enunciado, págs. 9–10 | §6.1, RF-01..20 |

### 1.2 Alcance de este documento

- Servicio de autenticación (IdP), API gateway y cliente liviano.
- Integración con los cuatro servicios existentes: validación del token, permisos y llamadas entre servicios.
- Huecos de backend que las pantallas necesitan (§8).

### 1.3 Fuera de alcance

| Tema | Va a | Motivo |
|---|---|---|
| Key Broker de Notificaciones (DTI-07) | E7 | D29 |
| Rate limiting | E7 | Requerimiento 3 de E7 `[DOCUMENTED]` |
| gRPC o GraphQL | E7 | Requerimiento 4 de E7 `[DOCUMENTED]` |
| Observabilidad y monitoreo nuevos (métricas, trazas, autorestart) | E7 | Requerimientos 1 y 2 de E7 `[DOCUMENTED]` |
| Retiro definitivo del header `X-Actor` | E7 | D25: E6 lo acepta y lo ignora (`X-API-Key` se retira ya en E6) |
| Redacción de los ADRs | Tarea siguiente | Este documento es el insumo (§11) |

---

## 2. Punto de partida `[OBSERVED]`

| Pieza | Estado en `ENTREGA_5` |
|---|---|
| `auth-service/`, `cliente-liviano/` | Solo un README de placeholder |
| Entrada a los servicios | Sin gateway: cada servicio expone su puerto en `docker-compose.yml` |
| Seguridad de los servicios | Sin Spring Security. Donaciones tiene un `ApiKeyFilter` (`X-API-Key`) para la administración del broker de logística y lee `X-Actor` en `PATCH /donaciones-independientes/{id}/estado` |
| Identidad del negocio | `Persona` (humana o jurídica) con los roles `Donante` y `EntidadBeneficiaria`, que comparten el id de la persona (ADR [`20261007-identidad-compartida...`](../adr/donaciones-service/20261007-identidad-compartida-de-roles-donante-y-entidad-beneficiaria.md), `proposed`) |
| Seguimiento de entregas | `GET /api/entregas/seguimiento/donaciones/{id}` y `.../beneficiarios/{id}`: avance por paradas **sin GPS**; `ParadaSeguimientoDTO` no expone direcciones de otras entregas a propósito |
| Mapa público | `GET /api/entregas/mapa` devuelve latitud y longitud **exactas** (`EntregaMapaPublicoResponseDTO`) |
| Camión | `Camion` no tiene posición |
| Almacenamiento de objetos | MinIO en compose, hoy usado solo por Incentivos |
| Importación CSV | `POST /api/donantes/archivos` recibe una **ruta del filesystem del servidor** (`ArchivoInputDTO.path`) y la lee con `Files.newBufferedReader`; no acepta la subida del archivo; `ArchivoOutputDTO` informa solo id, ruta y estado |
| Infra de datos | Un PostgreSQL (un schema por servicio) y un MongoDB con replica set (Logística) |

---

## 3. Arquitectura objetivo `[PROPOSED]`

```text
                          ┌──────────────┐
                          │    Google    │  (Social-Login, D3)
                          └──────▲───────┘
                                 │ OIDC
 Navegador ──cookie de sesión──▶ ┌───────────────────┐   OIDC (login, refresh, logout)   ┌───────────────┐
 (HTML + HTMX)                   │  cliente-liviano  │ ◀───────────────────────────────▶ │      IdP      │
                                 │ Spring MVC +      │                                   │ (D1 ✅:       │
                                 │ Thymeleaf         │                                   │  Keycloak)    │
                                 │ sesión JDBC (D5)  │                                   └──────┬────────┘
                                 └─────────┬─────────┘                                          │ JWKS
                                           │ Authorization: Bearer <JWT>                        │
                                           ▼                                                    │
                                 ┌───────────────────┐ ◀──────── valida firma (JWKS) ───────────┤
                                 │  api-gateway      │  allowlist de rutas públicas (D16, D26)  │
                                 │ Spring Cloud GW   │                                          │
                                 └─────────┬─────────┘                                          │
                 ┌─────────────────┬───────┴─────────┬──────────────────┐                       │
                 ▼                 ▼                 ▼                  ▼                       │
            donaciones        logística         incentivos       notificaciones ◀───────────────┘
            (cada servicio valida otra vez el JWT con JWKS y aplica rol + pertenencia, D2 y D13)

 Procesos sin usuario (n8n, jobs, proveedor externo) → token propio por client credentials (D15)
```

En lenguaje llano:
- El navegador **nunca** ve un token: solo tiene una cookie de sesión.
- El cliente liviano guarda los tokens del usuario y los agrega a cada llamada.
- El gateway es el guardia de la entrada. Cada servicio vuelve a revisar la credencial, por si alguien llega por una puerta lateral.

---

## 4. Servicio de autenticación (IdP)

Los requerimientos están escritos sin depender del producto, porque se escribieron antes de cerrar la D1 (Keycloak). Donde un producto cambia la implementación, se aclara.

Salvo que la fila diga otra cosa, los criterios de §4 a §8 son `[PROPOSED]`: así los propone este documento, la cátedra no los fija.

### 4.1 Funcionales

| ID | Requerimiento | Criterio de aceptación `[PROPOSED]` | Origen |
|---|---|---|---|
| RF-AUTH-01 | Inicio de sesión con email y contraseña | Login correcto → sesión iniciada; credenciales inválidas → mensaje genérico (no revela si el email existe) | RQ-E6-04, UI/UX 4 |
| RF-AUTH-02 | Inicio de sesión con Google | Botón "Continuar con Google" en el login; flujo OIDC completo | RQ-E6-05, D3 |
| RF-AUTH-03 | Vinculación de una cuenta Google a una cuenta existente con el mismo email | Si el email ya existe, se pide la contraseña de esa cuenta antes de vincular | D4 |
| RF-AUTH-04 | Excepción de vinculación para cuentas sin contraseña (importadas por CSV o creadas en el depósito) | La vinculación se habilita cuando Google informa `email_verified=true`; el mecanismo final depende de la D12 | D4, D12 🎓 |
| RF-AUTH-05 | Autorregistro de donantes | Desde "Registrarse" → elegir "Soy Donante"; al primer login se completa el perfil (§4.3) | UI/UX 3, D10, D11 |
| RF-AUTH-06 | Autorregistro de entidades beneficiarias, con aprobación | La cuenta queda **pendiente**; el rol `ENTIDAD` se asigna cuando un admin aprueba | UI/UX 3, D10 |
| RF-AUTH-07 | Alta de administradores | Solo un admin crea otro admin; el primero sale de un *seed* configurado por variables de entorno | D10 |
| RF-AUTH-08 | Emisión de token de acceso (JWT) | Claims mínimos: `sub`, `roles` (claim normalizado, ver RNF-AUTH-08), `exp`, `iat`, `iss`, `aud` | RQ-E6-04 |
| RF-AUTH-09 | Token de refresco con rotación y detección de reuso | Cada refresh entrega uno nuevo; reusar uno viejo corta la sesión entera | D7 |
| RF-AUTH-10 | Publicar las claves públicas (JWKS) | Endpoint JWKS estándar, consumido por el gateway y los servicios | D2 |
| RF-AUTH-11 | Cerrar sesión | Revoca el refresh en el IdP, invalida la sesión del cliente y cierra la sesión SSO (OIDC *RP-initiated logout*) | — |
| RF-AUTH-12 | Cambiar la contraseña con la sesión iniciada | Pide la contraseña actual; invalida las otras sesiones del usuario | — |
| RF-AUTH-13 | Recuperar la contraseña por email | Link de un solo uso con vencimiento (sugerido: 30 min); la respuesta es la misma exista o no el email. El mecanismo de envío está pendiente (D8) | D8 ⏸️ |
| RF-AUTH-14 | Alta de usuarios para donantes importados por CSV y registrados en el depósito | La consigna pide *enviarles sus credenciales de acceso* y un *correo de bienvenida*; el mecanismo está pendiente de consulta (D12) | UI/UX 20, D12 🎓 |
| RF-AUTH-15 | Alta en lote eficiente | Más de 10.000 usuarios por archivo, en lotes y sin bloquear la importación del negocio | UI/UX 20 |
| RF-AUTH-16 | Verificación del email en el autorregistro `[PROPOSED]` | La cuenta no opera hasta confirmar el email; usa el mismo mecanismo que RF-AUTH-13 | D8 ⏸️ |
| RF-AUTH-17 | Client credentials para procesos sin usuario | Un cliente OAuth2 por proceso (n8n, proveedor externo, jobs) con los permisos mínimos | D15, D25 |

### 4.2 No funcionales

| ID | Requerimiento | Criterio de aceptación `[PROPOSED]` | Origen |
|---|---|---|---|
| RNF-AUTH-01 | Contraseñas con hash adaptativo y sal | Argon2id, bcrypt o PBKDF2 con los parámetros de OWASP; nunca reversibles ni logueadas | Buena práctica |
| RNF-AUTH-02 | Token de acceso de vida corta | 15 minutos | D6 |
| RNF-AUTH-03 | Sesión acotada | Refresh con vencimiento por inactividad de 8 h | D7 |
| RNF-AUTH-04 | Firma asimétrica | RS256 o ES256; claves rotables sin cortar el servicio (JWKS con más de una clave) | D2 |
| RNF-AUTH-05 | Secretos fuera del repo | Credenciales de administración del IdP, secretos de clientes OAuth2 y de Google por variables de entorno (patrón `.env.example` existente); la configuración exportada al repo, sin secretos | AGENTS.md §4.3 |
| RNF-AUTH-06 | Base de datos propia del IdP | Schema o base propia en el PostgreSQL existente; el enunciado permite compartir el motor | Enunciado E5: un schema por servicio, motor compartido (por analogía) |
| RNF-AUTH-07 | Protección de fuerza bruta en el login | Bloqueo temporal tras N intentos fallidos (sugerido: 5 en 15 min); el rate limiting general queda para E7 | Buena práctica |
| RNF-AUTH-08 | Claims independientes del producto | Los roles viajan en un claim `roles` plano (en Keycloak, con un *protocol mapper*), para que los servicios no dependan de `realm_access.roles` | D1 |
| RNF-AUTH-09 | Sin datos personales en el token | Ni nombre, ni documento, ni datos de contacto: solo `sub` y roles (minimización de PII, ADR [crypto-shredding](../adr/notificaciones-service/20260902-proteccion-de-pii-crypto-shredding-y-desacoplamiento-de-mensajes.md)) | D9 |
| RNF-AUTH-10 | Respuestas que no revelan cuentas | Login, recuperación y activación responden igual exista o no el email | OWASP ASVS |

### 4.3 Integración IdP ↔ negocio

| ID | Requerimiento | Criterio de aceptación `[PROPOSED]` | Origen |
|---|---|---|---|
| RI-AUTH-01 | El negocio guarda el `sub` del IdP en la `Persona` | Columna `usuarioId` (única, nullable) en `Persona`; el IdP no guarda ids del negocio | D9 |
| RI-AUTH-02 | "Completar perfil" al primer login | Si el `sub` no tiene `Persona` asociada, el cliente lleva al formulario de perfil; al guardarlo se crea la `Persona` y su rol | D11 |
| RI-AUTH-03 | `GET /api/personas/me` en Donaciones | Devuelve la persona y sus roles de negocio a partir del `sub` del token; el cliente lo cachea en la sesión | D9 |
| RI-AUTH-04 | Una persona puede tener más de un rol | Una jurídica puede ser `DONANTE` y `ENTIDAD` a la vez (identidad compartida); la UI ofrece cambiar de rol | ADR identidad compartida |
| RI-AUTH-05 | Aprobación de entidades | Pantalla de admin con las entidades pendientes; aprobar asigna el rol `ENTIDAD` en el IdP y activa la entidad en el negocio | D10 |
| RI-AUTH-06 | Quién inicia sesión por una jurídica | Pendiente (D30) | D30 ⏸️ |

---

## 5. API gateway e integración con los servicios

| ID | Requerimiento | Criterio de aceptación `[PROPOSED]` | Origen |
|---|---|---|---|
| RI-GW-01 | Gateway único de entrada para el cliente liviano | Spring Cloud Gateway con rutas a los 4 servicios; el cliente no conoce sus puertos | D16 |
| RI-GW-02 | El gateway valida el JWT | Firma (JWKS), `exp`, `iss`, `aud`; token inválido → `401` con el formato de error unificado | D2 |
| RI-GW-03 | Denegar por defecto | Allowlist explícita de rutas públicas (§6.2); todo lo demás exige token | D16, D26 |
| RI-GW-04 | Propagación del `traceId` | El gateway genera o propaga el `traceId` y escribe logs NDJSON con el formato de `common-lib` | AGENTS.md §4.2, [ADR de observabilidad](../adr/20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md) |
| RI-SVC-01 | Cada servicio valida el JWT por su cuenta | `spring-boot-starter-oauth2-resource-server` en los 4 servicios; una llamada directa al puerto sin token → `401` | D2 |
| RI-SVC-02 | Autorización por rol | `@PreAuthorize` por endpoint, con la matriz de §6 | D13, D14 |
| RI-SVC-03 | Autorización por pertenencia | El Application Service verifica que el recurso pertenezca al usuario (por ejemplo, "la donación es de este donante"); los ids de "mis datos" salen del token, nunca de un parámetro | D13 (OWASP API1 BOLA) |
| RI-SVC-04 | Actor de auditoría desde el token | Las transiciones auditadas registran el actor a partir del `sub`, no de `X-Actor` | D25 |
| RI-SVC-05 | Migración de `X-Actor` | En E6 el header se acepta y se ignora (con un warning en el log); se retira en E7 | D25 |
| RI-SVC-06 | Retiro de `X-API-Key` en E6 | La administración del broker de logística exige rol `ADMIN` en el token desde E6; el `ApiKeyFilter` se elimina; el proveedor externo usa client credentials | D25 |
| RI-SVC-07 | Llamadas originadas por un usuario | Se reenvía el token del usuario (*token relay*) | D15 |
| RI-SVC-08 | Llamadas sin usuario (n8n, jobs, callbacks) | Token propio por client credentials, con un rol técnico (por ejemplo, `SERVICIO`) | D15 |
| RI-SVC-09 | Mensajes AMQP sin JWT | Los eventos llevan `traceId` y el id del actor en el payload; el consumidor no revalida un token que pudo vencer | D15 |
| RI-SVC-10 | Errores de seguridad en el catálogo | `401` y `403` con códigos del [catálogo de errores](../arquitectura/catalogo-errores.md) | Estándar de errores |

---

## 6. Cliente liviano

### 6.1 Requerimientos funcionales de pantalla `[DOCUMENTED]`

Los RF se numeran como los requerimientos mínimos de UI/UX del enunciado (págs. 9–10). Las pantallas salen del mapa del sitio de E1. La cobertura del backend es `[OBSERVED]` en `ENTREGA_5`.

| ID | Actor | Requerimiento | Criterio de aceptación `[PROPOSED]` | Endpoints | Cob. | Hueco (§8) |
|---|---|---|---|---|:-:|---|
| RF-01 | Público | Landing: propósito, destacadas del último mes, links a fotos, registro/login, legales | La landing carga sin login y muestra propósito, hasta N destacadas del último mes y los links de RF-01 | `GET /donaciones-independientes`; destacadas en `GET /api/donaciones/destacadas` (PR #954) | 🟡 | HB-01, HB-02 |
| RF-02 | Público | Mapa de donaciones entregadas con detalle por marcador | Sin login; cada marcador abre el detalle (fecha, categoría, foto); coordenadas exactas, sin el texto de la dirección (D24) | `GET /api/entregas/mapa` | 🟡 | HB-07 |
| RF-03 | Público | Registro de donantes y entidades | Un donante queda operativo al completar el perfil; una entidad queda pendiente de aprobación | IdP + `POST /api/personas`, `/api/donantes`, `/api/entidades` | 🟡 | RI-AUTH-02 |
| RF-04 | Todos | Login de donantes, entidades y admins | Los 3 roles inician sesión y llegan a su inicio; credenciales inválidas dan error genérico | IdP | ❌ | §4 |
| RF-05 | Donante | Filtrar sus donaciones por estado y categoría/subcategoría | Combinar estado y categoría o subcategoría devuelve solo donaciones propias que cumplen ambos | `GET /donaciones-independientes?estado&subcategoriaId&donanteId` | 🟡 | HB-03 |
| RF-06 | Donante | Navegar entidades beneficiarias | Listado paginado con búsqueda por nombre; solo entidades activas | `GET /api/entidades` | 🟡 | HB-08, HB-15 |
| RF-07 | Donante humano | Misiones e insignias | Visible solo para donante humano; muestra misiones con progreso e insignias obtenidas | `GET /api/incentivos/donantes/{id}/misiones`, `.../insignias` | ✅ | Ocultar si es jurídica |
| RF-08 | Donante | Notificaciones: asignación, misión cumplida, ascenso, recepción | Cada uno de los 4 eventos genera una notificación visible en la bandeja | `GET /api/notificaciones/persona/{personaId}` | 🟡 | HB-09 |
| RF-09 | Donante | Mapa de sus entregas activas con camión, ubicación y última actualización | Solo sus entregas activas; cada camión con posición y "última actualización" | `GET /api/entregas/seguimiento/donaciones/{id}` | 🟡 | HB-05, HB-06, HB-16, D22 |
| RF-10 | Entidad | Registrar necesidades | Alta, edición y baja de necesidades propias; no ve ni edita las de otras entidades | `/api/necesidades` (CRUD) | ✅ | |
| RF-11 | Entidad | Estado de las donaciones asignadas | Listado de donaciones asignadas a la entidad, filtrable por estado | `GET /donaciones-independientes` | 🟡 | HB-04 |
| RF-12 | Entidad | Confirmar recepción cargando fotos | Confirmar recepción exige al menos una foto válida (D27); la entrega pasa a `ENTREGADA` | `PATCH /api/entregas/{id}/estado`, `PATCH .../fotos` (solo URL) | 🟡 | HB-10 |
| RF-13 | Entidad | Notificaciones de asignación y recepción | Asignación y recepción generan notificación visible en la bandeja | `GET /api/notificaciones/persona/{personaId}` | 🟡 | HB-09 |
| RF-14 | Entidad | Mapa de entregas activas a recibir | Solo entregas que la entidad debe recibir; dirección exacta de su propia entrega y camión con posición exacta (D24) | `GET /api/entregas/seguimiento/beneficiarios/{id}` | 🟡 | HB-05, HB-06, HB-16, D22 |
| RF-15 | Admin | Registrar donantes y donaciones recibidas en el depósito | El admin registra un donante y su donación en un mismo flujo; el donante recibe acceso (D12) | `POST /api/personas`, `/api/donantes`, `/api/donaciones` | 🟡 | RF-AUTH-14 (alta de usuario) |
| RF-16 | Admin | Marcar donaciones vencidas | Solo el admin; la transición queda auditada con el actor del token | `PATCH /donaciones-independientes/{id}/estado` | ✅ | Actor desde el token (RI-SVC-04) |
| RF-17 | Admin | Elegir la entidad final según los algoritmos | El admin ve el resultado de los algoritmos y aprueba una propuesta | `/api/asignaciones/ejecuciones`, `/propuestas`, `/propuestas/{id}/estado` | ✅ | |
| RF-18 | Admin | Administrar camiones | Alta, cambio de estado y baja de camiones | `/api/camiones` | ✅ | |
| RF-19 | Admin | Ranking mensual e historial | Ranking del último mes y selector de meses anteriores | `GET /api/incentivos/ranking/ultimo`, `.../historial` | ✅ | |
| RF-20 | Admin | Importar donantes por CSV (más de 10.000 filas) y darles usuario | Archivo de 20.000 filas procesado sin bloquear la UI, con progreso y resultado por fila | `POST /api/donantes/archivos` | 🟡 | RF-AUTH-14/15, HB-14, D12 🎓 |
| RF-21 | Admin | Aprobar entidades pendientes `[PROPOSED]` | Listado de entidades pendientes; aprobar asigna el rol `ENTIDAD` y activa la entidad en el negocio | — | ❌ | RI-AUTH-05 |
| RF-22 | Público | Catálogo público de entidades y sus necesidades ("emergencias" de la landing) `[PROPOSED]` | Sin login; entidades activas con necesidades vigentes, sin datos de contacto | `GET /api/entidades`, `GET /api/necesidades` | 🟡 | HB-08 |

Resumen: 6 ✅ · 14 🟡 · 2 ❌.

### 6.2 Pantallas públicas y privadas

| Acceso | Pantallas (mapa del sitio de E1 + decisiones) |
|---|---|
| Público | Landing, Donaciones realizadas (galería), Mapa de entregadas, Nosotros, Landing donante, Landing beneficiario, Legales y privacidad, Iniciar sesión, Registrarse (elegir tipo), Activar cuenta, Recuperar contraseña, **Entidades y necesidades** (D26) |
| Donante | Mis donaciones, Beneficiarios, Misiones e insignias (solo humana), Seguir entregas, Notificaciones, Mi perfil |
| Entidad | Necesidades, Donaciones recibidas, Seguir entregas, Notificaciones, Mi perfil |
| Admin | Registrar donantes, Registrar entidades, Aprobar entidades, Registrar donaciones, Donaciones, Asignaciones, Camiones, Ranking, Importar CSV |

Reglas para lo público (D26):
- Cada ruta pública usa un **DTO público propio**, nunca el interno con campos ocultos.
- Las coordenadas se muestran exactas, pero nunca el texto de la dirección (D24).
- Las rutas públicas son las únicas en la allowlist del gateway (RI-GW-03).

### 6.3 Requerimientos funcionales propios del cliente `[PROPOSED]`

| ID | Requerimiento | Criterio de aceptación | Origen |
|---|---|---|---|
| RF-CL-01 | Sesión del lado servidor | El navegador solo recibe una cookie `HttpOnly`, `Secure`, `SameSite=Lax`; los tokens viven en la sesión JDBC | D5 |
| RF-CL-02 | Refresco transparente | El cliente renueva el access token antes de que venza, sin que el usuario lo note; si el refresh falla, lleva al login conservando la URL de destino | D5, D7 |
| RF-CL-03 | Menú según rol | Cada rol ve solo sus secciones; con dos roles, se ofrece cambiar de rol | D14, RI-AUTH-04 |
| RF-CL-04 | Protección CSRF | Token CSRF en todo formulario que modifica datos (Spring Security + HTMX) | Buena práctica |
| RF-CL-05 | Catálogo de componentes | Página interna `/estilos` con todos los componentes y sus estados | D19 |
| RF-CL-06 | Alternativa accesible a los mapas | Cada mapa tiene al lado una lista o tabla con los mismos datos | D21, WCAG |
| RF-CL-07 | "Última actualización" visible | Toda posición de camión muestra cuándo se actualizó | UI/UX 9 y 14 |
| RF-CL-08 | Páginas de error propias | `401` → login; `403` → "sin permiso"; `404` y `5xx` con mensaje amable y `traceId` visible para soporte | RNF-UI-05 |

### 6.4 No funcionales de maquetado `[DOCUMENTED]` (Consideraciones para el Maquetado)

| ID | Requerimiento | Cómo verificarlo `[PROPOSED]` |
|---|---|---|
| RNF-UI-01 | Usabilidad heurística, curva de aprendizaje mínima | Revisión con las 10 heurísticas de Nielsen, una ficha por flujo crítico |
| RNF-UI-02 | Navegación de 3 niveles como máximo hasta cualquier función clave | Mapa del sitio con la profundidad de cada pantalla |
| RNF-UI-03 | Patrones conocidos (Material Design / HIG) | Cada componente del catálogo cita el patrón que sigue; se piden **patrones**, no una librería (D19) |
| RNF-UI-04 | Microcopy y ayudas: etiquetas, tooltips, placeholders, onboarding hints | Checklist por flujo crítico (registro, donación, confirmar recepción, importar CSV) |
| RNF-UI-05 | Feedback no intrusivo (toasts, modals, banners) en toda acción | Cada acción tiene mensaje de éxito y de error |
| RNF-UI-06 | Loaders, skeletons o spinners si la latencia pasa de 300 ms | `hx-indicator` en cada request HTMX; prueba con la red throttled |
| RNF-UI-07 | Responsive (media queries, flexbox, grillas): móvil, tablet, desktop | Capturas en los anchos de referencia 360, 1280 y 1920 px; la tablet queda cubierta por un barrido automático de anchos intermedios ([decisión del 2026-10-30](../interfaz/bitacora.md)) |
| RNF-UI-08 | Sin distorsión ni superposición en ningún tamaño | Mismas capturas + comprobación automática de desbordes entre 360 y 1920 px |
| RNF-UI-09 | WCAG nivel AA: ARIA, foco visible, HTML semántico | axe o Lighthouse sobre `/estilos` y las pantallas principales, sin violaciones AA |
| RNF-UI-10 | Áreas de toque de al menos 48x48 px | Auditoría de Lighthouse y token de tamaño mínimo |
| RNF-UI-11 | Tokens de diseño unificados (colores, tipografías, espaciados, bordes) | Un solo `tokens.css`; las pantallas usan solo tokens semánticos (D20) |

### 6.5 No funcionales del cliente `[PROPOSED]`

| ID | Requerimiento | Criterio de aceptación | Origen |
|---|---|---|---|
| RNF-CL-01 | Desacople total del negocio | El cliente no tiene entidades del dominio ni acceso a bases de negocio; solo DTOs de las APIs | RQ-E6-01 |
| RNF-CL-02 | Mejora progresiva | Las pantallas funcionan sin JS en lo esencial (navegar, ver, enviar formularios); HTMX, Alpine y el JS propio solo mejoran la experiencia, y lo que suman degrada a una versión más simple, nunca a una pantalla vacía (ajuste del 2026-11-04, D31) | D18, D31 |
| RNF-CL-03 | Peso de la página | Sin build de frontend; HTMX + Alpine (build CSP) + Pico.css + MapLibre solo en las pantallas con mapa | D18, D19, D21, D31 |
| RNF-CL-04 | Sesión escalable | Con 2 instancias del cliente, la sesión sobrevive a un cambio de instancia (Spring Session JDBC) | D5 |
| RNF-CL-05 | Schema propio | Las tablas de sesión van en un schema `cliente` del PostgreSQL existente | D5 |
| RNF-CL-06 | Guardarraíles de desarrollo con agentes | `cliente-liviano/AGENTS.md` con las reglas: solo tokens semánticos, un componente es un fragmento Thymeleaf, estados obligatorios, breakpoints fijos; desde D33, capas CUBE con cerco mecánico (catálogo, tope de clases de un solo uso) | D19, D33 |

---

## 7. Datos y privacidad `[PROPOSED]`

| ID | Requerimiento | Criterio de aceptación | Origen |
|---|---|---|---|
| RNF-PRIV-01 | Coordenadas exactas en lo público y en el camión | Mapa público y posición del camión con las coordenadas exactas, para todos los roles y sin login; el DTO público no lleva calle, altura, piso, departamento ni código postal. Revisado el 2026-11-06: antes pedía ~1 km de precisión ([ADR-E6-07](../adr/20261106-posicion-del-camion-fuente-privacidad-y-actualizacion.md)) | D24 |
| RNF-PRIV-02 | Dirección textual solo por pertenencia | La entidad ve la dirección (calle, altura, piso) de su propia entrega; el donante, la de las entregas de sus donaciones ya completadas; el admin ve todo. Las paradas ajenas del seguimiento no muestran dirección | D24 |
| RNF-PRIV-03 | Fotos privadas | Bucket privado; lectura por URL firmada de vida corta (5 min, detrás de una URL estable de Logística que redirige) | D27, [ADR-E6-08](../adr/logistica-service/20261106-fotos-de-recepcion-en-almacenamiento-de-objetos.md) |
| RNF-PRIV-04 | Fotos sin metadatos | Se quitan los EXIF (pueden traer coordenadas GPS) antes de guardar, re-codificando la imagen | D27, [ADR-E6-08](../adr/logistica-service/20261106-fotos-de-recepcion-en-almacenamiento-de-objetos.md) |
| RNF-PRIV-05 | Sin PII en tokens ni en logs | Ver RNF-AUTH-09; los logs registran `sub`, no emails | ADR crypto-shredding |

---

## 8. Huecos de backend que la UI necesita

Son cambios aditivos en servicios existentes (AGENTS.md §8.2: *Feature*). No hay que renombrar ni eliminar campos.

| ID | Servicio | Cambio `[PROPOSED]` | Para | Origen |
|---|---|---|---|---|
| HB-01 | Logística + Donaciones | `GET /donaciones/destacadas`: entregadas en los últimos 30 días con foto, con DTO público. El boceto ofrece además "última semana / último año": parámetro `periodo`. Las fotos llegan a Donaciones por `entrega.exitosa` (ver la nota). **Avance:** `GET /api/donaciones/destacadas` implementado en la el PR #954, con la réplica de las fotos ([DTI-36](../adr/DEUDA_TECNICA.md#dti-36)) y la exclusión de las entregas con reporte | RF-01 | D28, [réplicas por evento](../adr/20261106-replicas-por-evento-fotos-de-recepcion-y-donante-de-la-entrega.md) |
| HB-02 | Donaciones / Incentivos | Contadores públicos de la landing (donantes activos, donaciones realizadas, beneficiarios activos) en un endpoint agregado. **Avance:** `GET /api/donaciones/contadores` implementado en el PR #953, solo en Donaciones (Incentivos no aporta datos), y sumado a la allowlist del gateway | RF-01 (boceto) | Boceto E1 |
| HB-03 | Donaciones | Filtro por `categoriaId` en `GET /donaciones-independientes` | RF-05 | D28 |
| HB-04 | Donaciones | Filtro por entidad asignada y rango de fechas | RF-11, RF-01 | D28 |
| HB-05 | Logística | VO `PosicionCamion` (coordenadas + timestamp de origen) y puerto `FuentePosicionCamion` con un adaptador simulado que avanza por las paradas de la ruta. **Hecho (2026-11-08):** `FuentePosicionCamionSimulada` y `ActualizadorPosicionCamiones` cada 30 s | RF-09, RF-14 | D23 |
| HB-06 | Logística | Exponer la posición del camión en el seguimiento: campo aditivo `posicionCamion` en `SeguimientoEntregaResponseDTO`, exacta para todos (RNF-PRIV-01). **Hecho (2026-11-08):** OpenAPI de Logística 1.3.0 | RF-09, RF-14 | D23, D24 |
| HB-07 | Logística | ~~`GET /api/entregas/mapa` con coordenadas aproximadas~~ Con la D24 revisada las coordenadas quedan exactas: solo se corrige el Javadoc de `EntregaMapaPublicoResponseDTO` (tarea 22 del [plan sin auth](plan-e6-trabajo-sin-auth.md)) | RF-02 | D24 |
| HB-08 | Donaciones | DTOs públicos de entidades y necesidades | RF-06, RF-22 | D26 |
| HB-09 | Notificaciones | Verificar que los 4 eventos del donante y los 2 de la entidad generan notificación | RF-08, RF-13 | — |
| HB-10 | Logística | `POST /api/entregas/{id}/fotos` multipart → MinIO y `GET /api/entregas/{id}/fotos/{fotoId}` (redirige a la URL firmada); el `PATCH .../fotos` actual se conserva | RF-12 | D27, [ADR-E6-08](../adr/logistica-service/20261106-fotos-de-recepcion-en-almacenamiento-de-objetos.md) |
| HB-11 | Donaciones | `usuarioId` en `Persona` y `GET /api/personas/me` | RI-AUTH-01/03 | D9 |
| HB-12 | Donaciones | Estado "pendiente de aprobación" para entidades autorregistradas y endpoint de aprobación | RF-21 | D10 |
| HB-13 | Los 4 servicios | Resource server JWT, `@PreAuthorize` y chequeos de pertenencia | RI-SVC-01..03 | D2, D13 |
| HB-14 | Donaciones | Subida del CSV desde el navegador (multipart → MinIO, como D27) en lugar de una ruta del servidor, y progreso con resultado por fila (procesadas, creadas, actualizadas, con error). **Avance:** la subida multipart → MinIO está implementada en la rama `E4_donaciones_minio`; falta el progreso con resultado por fila | RF-20 | Auditoría `[OBSERVED]` |
| HB-15 | Donaciones | Paginado y búsqueda por nombre en `GET /api/entidades` (hoy devuelve todo, sin parámetros) | RF-06, RF-22 | Auditoría `[OBSERVED]` |
| HB-16 | Logística | Listado de las entregas activas de un donante o una entidad en una sola llamada (hoy el seguimiento es por donación). Para el donante, `donanteId` opcional en `entrega.solicitada.v1` y en `Entrega`. **Hecho (2026-11-08):** `GET /api/entregas/seguimiento/donantes/{donanteId}`, solo pendientes y en traslado; la entidad ya tenía `/seguimiento/beneficiarios/{id}` | RF-09, RF-14 | Auditoría `[OBSERVED]`, [réplicas por evento](../adr/20261106-replicas-por-evento-fotos-de-recepcion-y-donante-de-la-entrega.md) |
| HB-17 | Logística + Donaciones | Remito y reporte de problemas al confirmar; `necesidadId` en la donación independiente. **Contrato hecho (2026-11-08); DTI-29 sigue `partial` hasta HB-01 y HB-13:** remito y reporte en `PATCH /api/entregas/{id}/estado` y en `EntregaResponseDTO`, `reporte` en `entrega.exitosa`, `necesidadId` en la donación, y la necesidad cuenta lo recibido ([ajuste del ADR](../adr/20261108-entregas-con-problema-fotos-privadas-y-cantidad-recibida.md)) | RF-12 | [DTI-29](../adr/DEUDA_TECNICA.md#dti-29) |
| HB-18 | Donaciones | Necesidad con estado y filtro, versión, unidad, fecha límite, períodos de semana, quincena o mes, y su historia en otra llamada. **Dos cambios incompatibles** | RF-10, RF-22 | [DTI-30](../adr/DEUDA_TECNICA.md#dti-30) |
| HB-19 | Donaciones + Logística | Cancelar las entregas que no salieron cuando se da de baja su necesidad. **Valor nuevo del enum de entregas** | RF-10 | [DTI-31](../adr/DEUDA_TECNICA.md#dti-31) |
| HB-20 | Donaciones | Nombre visible del donante con preferencia de ocultarlo, y constancia de los legales en el alta. **Campo obligatorio nuevo** | RF-12, RF-03, RF-01 | [DTI-32](../adr/DEUDA_TECNICA.md#dti-32) |
| HB-21 | Por decidir | Recibir los mensajes del formulario de contacto | RF-01 | [DTI-33](../adr/DEUDA_TECNICA.md#dti-33) |
| HB-22 | Donaciones + Logística | Distinguir la entrega que llegó a la puerta y espera la confirmación de la entidad de la que todavía viene en camino: hoy las dos están en traslado | RF-11, RF-12, RF-14 | Conexión de Donaciones recibidas, 2026-11-08 `[OBSERVED]` en `TipoEstadoDonacion` y `EstadoEntrega` de Logística |

HB-22 salió al conectar Donaciones recibidas (2026-11-08) y todavía no tiene DTI ni dueño. HB-17 a HB-21 salen de las preguntas abiertas de Raíces, que el usuario decidió el 2026-11-02. Los
marcados en negrita no son aditivos y se registran según la
[política de evolución de contratos](../adr/20261028-politica-de-evolucion-de-contratos-entre-servicios.md).

**Nota sobre HB-01**: la foto de recepción vive en Logística y la categoría de la donación en Donaciones. **Decidido el 2026-11-06** ([ADR de réplicas por evento](../adr/20261106-replicas-por-evento-fotos-de-recepcion-y-donante-de-la-entrega.md), `proposed`): `entrega.exitosa` suma el campo opcional `fotosRecepcion` (URLs estables que redirigen a una URL firmada) y Donaciones lo replica, así sirve las destacadas sin llamar a Logística. Descartadas: que el cliente componga las dos respuestas y que Logística replique la categoría.

---

## 9. Decisiones tomadas

Cada decisión resume las alternativas que se compararon, la elegida y sus consecuencias. La columna **ADR** remite a §11.

### D2 — Validación del token: gateway + JWKS en cada servicio ✅ · ADR-E6-02

| Criterio | JWKS solo en los servicios | Gateway que valida y servicios que confían | **Gateway + JWKS en los servicios** |
|---|---|---|---|
| Componentes nuevos | Ninguno | Gateway | Gateway |
| Puertas laterales (puerto directo, llamadas internas) | Cubiertas | **Abiertas** | Cubiertas |
| Punto único de entrada para el cliente | No | Sí | Sí |
| Base para rate limiting en E7 | No | Sí | Sí |
| Trabajo en E6 | Medio | Medio | Alto |

- **Elegida:** doble capa. Introspección (preguntar al IdP en cada request) descartada `[REJECTED]`: suma una llamada por request y deja al IdP como punto único de falla.
- **Consecuencia:** cada servicio cachea las claves JWKS; el IdP caído no corta las requests mientras haya tokens vigentes.

### D3 — Social-Login: solo Google ✅ · ADR-E6-01
Cumple RQ-E6-05 con un solo registro de aplicación. GitHub y Facebook descartados: duplican configuración y Facebook exige revisión de la app.

### D4 — Vinculación social: pedir la contraseña ✅ · ADR-E6-01
- Si alguien entra con Google y su email ya existe, debe probar que es dueño de la cuenta.
- Es lo más seguro frente a un email mal verificado. Cuesta un paso más.
- Excepción: cuentas sin contraseña (RF-AUTH-04).

| Criterio | Vincular si Google verificó el email | **Pedir la contraseña para vincular** | Social solo para cuentas nuevas |
|---|---|---|---|
| Seguridad ante un email mal verificado por el proveedor | Media: confía en `email_verified` | **Alta**: prueba de posesión de la cuenta | Alta |
| Fricción para el usuario | Ninguna | **Un paso más** | Alta: rechaza a quien ya tiene cuenta |
| Cuentas sin contraseña (CSV, depósito) | Funciona | **Necesita excepción** (RF-AUTH-04) | Las deja sin login social |

### D5 — Sesión mixta con Spring Session JDBC ✅ · ADR-E6-03

| Criterio | Stateless (JWT en cookie) | **Mixta (sesión en el cliente, JWT hacia el backend)** | Stateless con cookie cifrada |
|---|---|---|---|
| Token en el navegador | Sí (HttpOnly) | **No**: solo un id de sesión | Cifrado |
| Logout | El access sigue vivo ≤ 15 min | **Inmediato** | Igual que stateless |
| Rotación de refresh con varias pestañas | Carreras: una pestaña usa el refresh viejo y el IdP corta la sesión | **Sin carreras**: el servidor serializa el refresco | Carreras |
| Escalar el cliente | Trivial | Store compartido (JDBC) | Trivial |
| Código | Manual | Viene en Spring (`oauth2Login`, `OAuth2AuthorizedClientManager`) | El más alto |

- Los servicios de negocio siguen **stateless** en cualquier caso.
- **En lenguaje llano:** el cliente es un mostrador que guarda tus credenciales y habla con los servicios por vos.

### D6 — Access token de 15 minutos ✅ · ADR-E6-03
Equilibrio estándar. Con 5 min habría más refrescos y más carga sobre el IdP; con 30 min, una ventana de robo más larga.

### D7 — Refresh con rotación y detección de reuso ✅ · ADR-E6-03
Cada uso da uno nuevo. Reusar uno viejo es señal de robo y corta la sesión. Inactividad máxima: 8 h.

| Criterio | **Rotación con detección de reuso** | Sin rotación, vida fija | Sin refresh (re-login al vencer) |
|---|---|---|---|
| Si roban el refresh | **Se detecta al primer reuso y se corta la sesión** | Sirve hasta que vence | No hay refresh que robar |
| Experiencia | **Transparente** | Transparente | Login cada 15 min |
| Riesgo de carreras con varias pestañas | Existe, y lo resuelve la sesión del servidor (D5) | No | No |

### D9 — Vínculo IdP ↔ negocio por `sub` ✅ · ADR-E6-04

| Criterio | **`sub` guardado en el negocio** | Claim `donanteId` en el token | Mismo id en ambos |
|---|---|---|---|
| Acoplamiento | **Bajo**: el IdP no sabe qué es un donante | Alto | Total |
| Si se cambia de IdP (D1 cerrada con Keycloak) | **Se migra una columna** | Se rehacen mappers y atributos | Se migran todas las PK y FK |
| Costo por request | Lookup por índice, cacheado en la sesión | Cero | Cero |
| Datos viejos | No hay | Hasta el próximo refresh | No hay |

- **Afinado con el modelo actual:** Donante y EntidadBeneficiaria comparten el id de la `Persona` `[OBSERVED]`; por eso el `sub` se guarda en `Persona` (HB-11) `[PROPOSED]`.

### D10 — Donante libre, entidad con aprobación, admin por admin ✅ · ADR-E6-04
- Buena práctica (OWASP ASVS): los roles con privilegio no se autoasignan.
- Un rol que **recibe valor** (bienes donados) pasa por verificación.
- Coincide con el mapa del sitio de E1, donde el admin "Registra Entidades".

| Criterio | **Donante libre, entidad con aprobación** | Donante y entidad libres | Solo el admin da de alta |
|---|---|---|---|
| Cumple el registro público (UI/UX 3) | **Sí** | Sí | No |
| Riesgo de entidades falsas que reciben donaciones | **Bajo** | Alto | Nulo |
| Trabajo del admin | **Una cola de pendientes** | Ninguno | Todas las altas |

### D11 — Alta: login en el IdP + "completar perfil" ✅ · ADR-E6-04

| Criterio | Formulario propio + Admin API del IdP | **IdP + completar perfil (lazy provisioning)** | Evento del IdP por RabbitMQ |
|---|---|---|---|
| Sirve igual para Google y contraseña | No: igual hace falta completar perfil | **Sí** | Sí |
| Cuentas huérfanas si algo falla | Hay que compensar | **No hay**: el perfil se crea cuando hace falta | Consistencia eventual |
| Credenciales con permiso de alta en el negocio | Sí | **No** | No |
| UI con estilo propio | Total | Requiere un tema del IdP | Tema del IdP |

### D13 — Autorización por rol + pertenencia ✅ · ADR-E6-04
El riesgo nº 1 del OWASP API Top 10 (*Broken Object Level Authorization*) es ver objetos ajenos cambiando un id. El rol solo no alcanza. La pertenencia es regla de negocio y va en el Application Service. Authorization Services de Keycloak descartado `[REJECTED]`: ata las reglas del negocio a un producto de IdP concreto.

### D14 — Roles `DONANTE`, `ENTIDAD`, `ADMIN` ✅ · ADR-E6-04
Si es humana o jurídica sale del dominio (`Persona`). Separar `DONANTE_HUMANO` y `DONANTE_JURIDICO` duplicaría un dato del negocio en el IdP. Para procesos sin usuario se suma un rol técnico (RI-SVC-08).

### D15 — Inter-servicios: token relay + client credentials ✅ · ADR-E6-05
Zero trust: ningún llamado interno pasa sin credencial.
- Si la acción la inició una persona, viaja su token (auditoría de quién hizo qué).
- Si es un proceso (n8n, jobs, callbacks), usa un token propio.
- AMQP lleva actor y `traceId`, no JWT: el token vence y el mensaje se puede procesar después.

| Criterio | Reenviar siempre el token del usuario | Solo client credentials | **Híbrido** | Confiar en la red interna | mTLS entre servicios |
|---|---|---|---|---|---|
| Auditoría de quién originó la acción | Sí | No: solo qué servicio | **Sí cuando hay usuario** | No | No |
| Jobs, n8n, callbacks | No sirve: no hay usuario | Sí | **Sí** | Sí | Sí |
| Puertas laterales | Cerradas | Cerradas | **Cerradas** | Abiertas | Cerradas |
| Infraestructura nueva | Ninguna | Clientes OAuth2 | **Clientes OAuth2** | Ninguna | PKI y certificados por servicio |

*Token exchange* (RFC 8693) queda como evolución si hace falta reducir el alcance del token reenviado.

### D16 — Gateway con Spring Cloud Gateway ✅ · ADR-E6-02
Mismo stack, validación JWT nativa y reuso del `traceId` y los logs NDJSON de `common-lib`. Traefik, Nginx y Kong descartados: validan JWT por plugin o módulo y escriben logs en otro formato.

### D17 — Spring MVC + Thymeleaf ✅ · ADR-E6-06
Integración nativa con Spring Security (`sec:authorize`). Las *natural templates* se abren como HTML y sirven de maqueta (RQ-E6-07). Node + Express descartado: toolchain nueva para el grupo.

### D18 — HTMX + JS puntual ✅ · ADR-E6-06
El servidor devuelve fragmentos HTML. Los spinners salen declarativos con `hx-indicator` (RNF-UI-06). Sigue siendo MVC liviano. SPA (React/Vue) descartada `[REJECTED]`: contradice RQ-E6-03.

> **Ajuste del 2026-11-04 (D31):** se suma Alpine.js con el build CSP para el estado que no sale del navegador; HTMX sigue siendo el único que habla con el servidor.

### D19 — Pico.css + CSS propio ✅ · ADR-E6-06

| Criterio | CSS propio con tokens | **Pico.css + CSS propio** | Bootstrap 5 tematizado | Material (MDC Web) |
|---|---|---|---|---|
| Fidelidad al diseño de la diseñadora | Total | **Alta** | Media | Baja |
| CSS a generar y revisar (desarrollo con Claude Code) | Mucho | **Poco** | Poco | Medio |
| Riesgo de deriva con IA | Alto sin reglas | **Bajo** | Bajo | Medio |
| Accesibilidad de base | A mano | **En HTML semántico** | Buena | Buena |

- Lo que más pesó: con IA, menos CSS propio es menos deriva y menos revisión.
- Guardarraíles: RNF-CL-06 y RF-CL-05.
- RNF-UI-03 se cumple siguiendo patrones de Material/HIG, no usando su librería.

### D20 — `tokens.css` en el repo como fuente de verdad ✅ · ADR-E6-06
- Dos niveles: **primitivos** (la paleta: `--verde-500`) y **semánticos** (el uso: `--color-accion-primaria`). Las pantallas usan solo los semánticos.
- La diseñadora puede iterar con Claude Code. Git guarda la historia y Figma queda para explorar.
- Descartados: exportar Figma Variables (cada cambio pasa por Figma) y Style Dictionary (suma Node al build).

### D21 — MapLibre GL + OpenFreeMap ✅ · ADR-E6-06

| Criterio | Leaflet + OSM | **MapLibre + OpenFreeMap** | Mapbox GL JS |
|---|---|---|---|
| Licencia y costo | Libre, gratis | **Libre, gratis, sin clave** | Propietaria, token, capa gratuita |
| Mapa con la identidad visual | Limitado | **Total** (JSON de estilo; editor Maputnik) | Total (Mapbox Studio) |
| Peso | ~40 KB | ~200 KB | ~200 KB+ |
| Requisito | Ninguno | WebGL | WebGL |
| SLA de las teselas | Política de uso de OSM | Comunitario, sin SLA | Comercial |

Obligatorio con cualquier opción: atribución de los datos y alternativa accesible (RF-CL-06).

### D22 — Actualización de la posición del camión: polling con HTMX ✅ · [ADR-E6-07](../adr/20261106-posicion-del-camion-fuente-privacidad-y-actualizacion.md)
Decidida por tsorren el 2026-11-06 (antes ⏸️). El navegador pide cada 30 s, solo con la pestaña visible, un fragmento HTML con el marcador del camión (`hx-trigger="every 30s [document.visibilityState=='visible']"`). 30 s es la frecuencia de la fuente (D23): consultar más seguido no trae datos más frescos. El fragmento es una plantilla aislada para que un SSE futuro empuje el mismo HTML.

| Criterio (peso) | **A. Polling** | B. SSE |
|---|:-:|:-:|
| Simplicidad y costo (20%) | **5** | 2 |
| Escalabilidad (15%) | **5** | 3 |
| Frescura (15%) | 3 | 5 |
| Seguridad (15%) | **4** | 3 |
| Testabilidad (10%) | **5** | 2 |
| Resiliencia (10%) | **4** | 3 |
| Consistencia arquitectónica (10%) | **5** | 3 |
| Observabilidad (5%) | 3 | 3 |
| **Total** `[INFERRED]` | **4,35** | **3,00** |

- WebSocket queda descartado `[REJECTED]`: el navegador no le manda nada al servidor.
- SSE pasa a convenir si el negocio pide frescura de pocos segundos, si el rate limiting de E7 vuelve caro el polling o si E7 adopta gRPC en streaming entre Logística y el cliente.
- El análisis por atributo de calidad (frescura, rendimiento, escalabilidad, seguridad, observabilidad, E7) está en el ADR.

### D23 — Posición del camión simulada, detrás de un puerto ✅ · [ADR-E6-07](../adr/20261106-posicion-del-camion-fuente-privacidad-y-actualizacion.md)

| Criterio | Derivada de la última parada | **Simulada detrás de un puerto** | Callback del proveedor externo | GPS del chofer en el navegador |
|---|---|---|---|---|
| Realismo en la demo | Bajo: salta de parada en parada | **Alto** | Alto | Real |
| Cambio en el sistema | Chico | **Medio**: VO + campo en el documento + `@Scheduled` + DTO + tests con reloj fijo | Grande: contrato de entrada, seguridad, idempotencia | Muy grande: rol nuevo `CHOFER` |
| Camino a un GPS real | Ninguno | **Cambiar el adaptador** | Ya es el real | Ya es real |

- Sigue el patrón de puertos y adaptadores que ya usa `ProveedorExternoPlanificacionSimulado` `[OBSERVED]`.
- Cada posición lleva su timestamp de origen: se descarta la que llega más vieja que la guardada (idempotencia).
- **Frecuencia:** el simulador avanza cada 30 s (decidido por tsorren el 2026-11-06).
- **Evolución prevista:** el callback del proveedor externo es otro adaptador del mismo puerto; se suma sin rediseño.

### D24 — Coordenadas exactas para todos; la dirección textual, por pertenencia ✅ (revisada el 2026-11-06) · [ADR-E6-07](../adr/20261106-posicion-del-camion-fuente-privacidad-y-actualizacion.md)
- **Cambio:** la versión anterior pedía coordenadas aproximadas (~1 km) en el mapa público y en la posición del camión, salvo para el admin. Revisada por tsorren el 2026-11-06: las coordenadas del mapa público (`GET /api/entregas/mapa`) y la posición del camión quedan **exactas para todos**.
- **Lo que no cambia:** el texto de la dirección (calle, altura, piso, departamento, código postal) no sale en lo público ni en las paradas ajenas del seguimiento (`ParadaSeguimientoDTO` `[OBSERVED]`); solo lo ve quien tiene la entrega (RNF-PRIV-02).
- **Motivo:** las direcciones de destino son de entidades beneficiarias, que son organizaciones. Desplazar el punto ~1 km da una privacidad engañosa: el camión igual se detiene en el punto exacto y la lista de entidades es conocida. Además, simplifica el código y hace más fiel la demo.
- **Costo aceptado:** con el tiempo, el recorrido exacto del camión revela dónde reciben **otras** entidades. Si se suman destinatarios que sean personas, la decisión se revisa.

| Criterio | **Exacta para todos** | Aproximada salvo admin (anterior) | Exacta solo para quien tiene una parada en la ruta | Sin camión salvo admin |
|---|---|---|---|---|
| Utilidad para RF-09 y RF-14 | **Alta** | Media: la entidad ve su camión a ~1 km | Alta | Nula: incumple RF-09 |
| Privacidad real | **Baja**, aceptada | Aparente: se anula cruzando con la lista de entidades | Media | Alta |
| Costo | **Ninguno** | Redondeo en dos DTOs y sus tests | Pertenencia (HB-13) y dos versiones del DTO | Ninguno |

### D25 — `X-Actor` y `X-API-Key` reemplazados por el token ✅ · ADR-E6-05
- Hoy `X-Actor` lo manda quien llama y nadie lo verifica: se puede falsificar.
- Quitar un header es un *breaking change* (AGENTS.md §8.2). Se trata distinto en cada caso:
  - `X-Actor`: en E6 se acepta y se ignora (el actor sale del token); en E7 se retira.
  - `X-API-Key`: **corte directo en E6**. Solo la usan la administración del broker de logística y sus tests; mantenerla dejaría una segunda vía de acceso de admin sin identidad.
- El proveedor externo pasa a client credentials.

| Criterio | Migración E6 → E7 (**elegida para `X-Actor`**) | Corte directo en E6 (**elegida para `X-API-Key`**) | Mantener los headers y validarlos contra el token |
|---|---|---|---|
| Rompe clientes actuales (tests de integración, Postman, n8n) | No, durante E6 | Sí: para `X-API-Key` solo afecta la administración del broker y sus tests | No |
| Seguridad durante E6 | Media: el header queda como dato no confiable, ignorado | Alta: una sola vía, la del token | Media: dato duplicado |
| Costo | Dos pasos | Un paso, con los clientes migrados de una vez | Mantener dos fuentes para siempre |

### D26 — Pantallas públicas: mínimo del enunciado + entidades y necesidades ✅ · ADR-E6-02
- La landing del boceto ya muestra "emergencias" con un botón "AYUDAR" `[OBSERVED]`.
- Exponer entidades y necesidades atrae donantes antes de que se registren.
- Cuesta sanitizar esos datos (HB-08).

| Criterio | Mínimo del enunciado | **Mínimo + entidades y necesidades** | Todo detrás de login |
|---|---|---|---|
| Cumple UI/UX 1 y 2 (landing y mapa públicos) | Sí | **Sí** | No |
| Fiel al boceto ("emergencias" en la landing) | No | **Sí** | No |
| Datos a sanitizar | Pocos | **Entidades y necesidades** | Mínimos |

### D27 — Fotos: multipart a Logística → MinIO ✅ · [ADR-E6-08](../adr/logistica-service/20261106-fotos-de-recepcion-en-almacenamiento-de-objetos.md)

| Criterio | **Multipart a Logística → MinIO** | URL prefirmada directa a MinIO | El cliente sube a MinIO |
|---|---|---|---|
| Validación (tipo real, tamaño, EXIF) | **En el servidor, antes de guardar** | Después de subir | En la capa de UI |
| Exposición | MinIO interno | MinIO expuesto al navegador + CORS | Credenciales de storage en la UI `[REJECTED]` |
| Escala con archivos grandes | Tres saltos | La mejor | Media |

Reglas:
- Tope de 5 MB.
- Se valida el tipo real por *magic bytes*.
- Se quitan los EXIF.
- Bucket privado y lectura con URL firmada.

### D28 — Filtros con parámetros aditivos + endpoint de destacadas ✅ · sin ADR
Endpoints dentro de un contrato y un patrón ya decididos (AGENTS.md §9.2). "Destacada" = entregada en los últimos 30 días y con foto: un criterio objetivo, sin votos ni curaduría. Filtrar en el cliente queda descartado: rendimiento y sobreexposición de datos.

### D29 — Key Broker (DTI-07) pasa entero a E7 ✅ · sin ADR nuevo
E6 ya suma IdP, gateway y cliente. El objetivo de DTI-07 y el del crypto-shredding definitivo de DTI-01 se actualizan a E7 en [`DEUDA_TECNICA.md`](../adr/DEUDA_TECNICA.md). Consecuencia: el servicio aparte que acompaña a Keycloak (D1) recién se construye en E7.

| Criterio | Implementar el Key Broker en E6 | Contrato en E6, implementación en E7 | **Todo a E7** |
|---|---|---|---|
| Tamaño de E6 | Crece: un servicio más, con base de claves | Acotado | **Mínimo** |
| DTI-07 | Se salda | Queda lista para saldar | **Sigue diferida** |
| Riesgo | Alto: E6 ya suma IdP, gateway y cliente | Bajo | **E7 arranca sin el contrato diseñado** |
| Encaje con la consigna | E7 es la entrega de Seguridad | Ídem | **Ídem** |

### D31 — Alpine.js (build CSP) para el estado de interfaz en el navegador · decidida por tsorren (2026-11-04), a validar con el equipo; implementación pendiente (DTI-34) · [ADR](../adr/cliente-liviano/20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
HTMX habla con el servidor; Alpine maneja solo estado visual que no sale del navegador (diálogos, pestañas, pasos, vista previa de fotos). Prohibido en Alpine: `fetch`, armar listas con datos del negocio y guardar tokens, para que las vistas sigan renderizadas por el motor de plantillas (RQ-E6-03). Build CSP porque el estándar necesita `unsafe-eval`. Descartados: Alpine estándar, Petite-Vue y seguir con JS propio por pantalla. RNF-CL-02 queda en "lo esencial".

### D32 — CSS nativo moderno y sistema de movimiento · decidida por tsorren (2026-11-04), a validar con el equipo; implementación pendiente (DTI-34) · [ADR](../adr/cliente-liviano/20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
`@layer` con Pico abajo, View Transitions, `@starting-style`, `@container` y `popover` como mejora progresiva; tokens de movimiento en `tokens.css` y primitivas con nombre en `movimiento.css`, con "reducir movimiento" resuelto adentro. Descartados: GSAP o Motion (por ahora) y `@keyframes` sueltos por pantalla.

### D33 — Capa CUBE propia con cerco y componentes con migración incremental · decidida por tsorren (2026-11-04), a validar con el equipo; implementación pendiente (DTI-34) · [ADR](../adr/cliente-liviano/20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
Composición, utilidades, bloques (un componente por hoja y fragmento, con slots) y excepciones; las hojas de pantalla, solo disposición. Cerco en `InterfazFitnessTest` y en las skills; vuelve el catálogo `/estilos` (RF-CL-05) con [`componentes.md`](../interfaz/componentes.md). Migración por el [backlog obligatorio](../interfaz/backlog-migracion-componentes.md). Tailwind v4 diferido después de E6, con condiciones (quitar Pico, componentes migrados y cerco equivalente).

---

## 10. Decisiones pendientes

### D1 — Proveedor de identidad ✅ · ADR-E6-01
**Estado:** **decidida el 2026-11-04: Keycloak configurado**, en un ADR `proposed` que falta aprobar en el PR. Según el equipo, la cátedra confirmó que cuenta como servicio de autenticación (P1). El Key Broker de E7 es un servicio aparte. Ver el [ADR](../adr/auth-service/20261104-proveedor-de-identidad-keycloak.md) (`proposed`), que actualiza la matriz de abajo, y el backlog del MVP en la [SPEC-06](../specs/active/SPEC-06-auth-mvp-keycloak.md). La matriz y los pasos que siguen quedan como registro del análisis previo.

| Criterio (peso) — nota 1 a 5 | SAS (servicio propio) | Keycloak |
|---|---|---|
| Diseño visible para la cátedra (30%) | 5 · clases, capas y tests propios | 2 · configuración (realm exportado) |
| Esfuerzo de construcción (25%, 5 = menos esfuerzo) | 2 · todo a construir | 5 · login, registro, Google y recuperación vienen hechos |
| Encaje con el Key Broker de DTI-07 (15%) | 5 · el mismo servicio | 1 · hace falta otro servicio |
| Alta en lote desde el CSV (10%) | 4 · endpoint propio | 3 · Admin API / *partial import* |
| Estándar OAuth2/OIDC y JWKS (10%) | 4 · sí | 5 · sí, más maduro |
| Operación (10%) | 4 · un servicio Spring más | 3 · imagen pesada, arranque lento, tema propio para el login |
| **Total ponderado** `[INFERRED]` | **3,95** | **3,10** |

- **En lenguaje llano:** Keycloak es comprar la cerradura (anda el primer día); SAS es fabricarla con piezas estándar (más trabajo, diseño propio).
- **Para cerrarla hace falta:**
  1. Consultar a la cátedra si un Keycloak configurado cuenta como *"implementación de un SSO"* y como *"servicio de Autenticación"* (RQ-E6-04/05).
  2. Ver con cuánto tiempo cuenta el equipo hasta la fecha de entrega de E6.
- **Lo que no cambia con ninguna de las dos:** D9 (`sub` en el negocio) y RNF-AUTH-08 (claim `roles` plano) hacen que cambiar de IdP cueste poco.

### D8 — Envío del email de recuperación (y de verificación) ⏸️ · ADR-E6-01
Ya está decidido que el email sale por **notificaciones-service**. Falta el cómo, que depende de la D1:

| Alternativa | Cómo funciona | A favor | En contra |
|---|---|---|---|
| Event listener del IdP → RabbitMQ → notificaciones | Extensión Java (SPI) de Keycloak que publica el evento con el link | Reusa la mensajería y el canal de email existentes; queda en el historial de notificaciones | Hay que escribir y empaquetar un SPI; el link viaja en un mensaje (vida corta + TLS) |
| SMTP propio del IdP | Keycloak manda el mail solo | Cero código | El mail no pasa por notificaciones: incumple lo decidido |
| Flujo propio en un servicio | El servicio genera el token, manda el evento y cambia la clave por Admin API | Control total | Duplica lo que el IdP ya trae |

Con SAS, la primera opción es natural: el propio servicio publica el evento.

- **Recomendación:** event listener → RabbitMQ → notificaciones. Es la única que cumple lo decidido sin duplicar lo que trae el IdP.
- **Para cerrarla hace falta:**
  1. ~~Cerrar la D1.~~ Cerrada el 2026-11-04 (Keycloak): la opción es el SPI.
  2. Si queda Keycloak, probar que el SPI se empaqueta en la imagen del IdP y que el evento llega a notificaciones con su `traceId`.
  3. Definir el evento nuevo (`EnlaceDeCuentaSolicitado` o similar) en el [catálogo de eventos](../arquitectura/eventos-amqp.md).

### D12 — Acceso de los donantes importados por CSV y del alta en depósito 🎓 · ADR-E6-01
**Choque detectado** `[DOCUMENTED]`:
- Importación: *"se lo deberá crear el usuario y enviarle sus credenciales de acceso"*.
- Alta en el depósito: *"se les enviará un correo electrónico de bienvenida para que accedan por primera vez"*.

La preferencia del equipo (activación a demanda) no manda nada al importar. **Se consulta a la cátedra.**

| Alternativa | Qué recibe el donante | Cumple la consigna | Riesgo |
|---|---|---|---|
| Link de activación por email, encolado | Un link de un solo uso para fijar su clave, enviado en lotes con límite de tasa | Sí, si "credenciales" incluye un link | Ráfaga de 10.000+ mails (filtros de spam, límites del proveedor) |
| Activación a demanda | Nada al importar; pide el link en "Activar mi cuenta" o entra con Google | Dudoso | Que la cátedra lo marque como incumplido |
| Contraseña temporal por email | Una clave que debe cambiar al primer login | Literal | La clave viaja por un canal no confiable (NIST SP 800-63B lo desaconseja) |

**Pregunta para la cátedra:** ¿un link de un solo uso para que la persona fije su contraseña cuenta como "enviarle sus credenciales de acceso"? ¿Es aceptable enviarlo bajo demanda?

### D30 — ¿Quién inicia sesión por una persona jurídica? ⏸️ · ADR-E6-04
Surge del modelo de identidad `[OBSERVED]`: una jurídica tiene representantes con su propio email. El enunciado dice que la jurídica dona *"o en caso de las jurídicas, un representante"*.

| Alternativa | A favor | En contra |
|---|---|---|
| Una cuenta por jurídica (email de la jurídica) | Simple; coincide con el CSV, que trae un solo email por jurídica | Credencial compartida entre personas: no se sabe quién hizo qué (mala práctica) |
| Una cuenta por representante, vinculada a la jurídica | Auditoría por persona; un representante que se va pierde el acceso sin afectar al resto | Más modelo (usuario ↔ representante ↔ jurídica); el CSV no trae representantes |
| Por representante, con un representante principal en E6 | Camino intermedio | Deja la multiplicidad para después |

**Recomendación:** una cuenta por representante. Para el CSV, el email de la jurídica se toma como su representante principal.

---

## 11. ADRs candidatos

Two-Gate Rule de [`docs/adr/README.md`](../adr/README.md): solo cuando hay decisión nueva **y** significancia arquitectónica. Todos nacen `proposed`.

| ADR | Decisiones | Gate A (nueva) | Gate B (significativa) | Estado para redactarlo |
|---|---|---|---|---|
| ADR-E6-01 Proveedor de identidad, Social-Login y emails de cuenta | D1, D3, D4, D8, D12 | Sí | Tecnología nueva y seguridad | D1 redactada en su propio [ADR](../adr/auth-service/20261104-proveedor-de-identidad-keycloak.md); el resto espera D8 y D12 |
| ADR-E6-02 API gateway, validación JWT en doble capa y rutas públicas | D2, D16, D26 | Sí | Integración entre servicios y seguridad | Redactado (`proposed`): [ADR](../adr/20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md) |
| ADR-E6-03 Sesión del cliente liviano y ciclo de vida de los tokens | D5, D6, D7 | Sí | Estado y seguridad | Listo |
| ADR-E6-04 Identidad y autorización: `sub`, roles, pertenencia y alta de cuentas | D9, D10, D11, D13, D14, D30 | Sí | Contratos entre IdP y negocio, seguridad | Esperar D30 (se puede redactar marcándola) |
| ADR-E6-05 Autenticación entre servicios y retiro de `X-Actor` / `X-API-Key` | D15, D25 | Sí | *Breaking change* de contrato | Listo |
| ADR-E6-06 Stack del cliente liviano: Thymeleaf, HTMX, Pico.css, tokens y mapas | D17–D21 | Sí | Tecnología nueva | Redactado (`proposed`): [ADR](../adr/cliente-liviano/20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md) |
| ADR-E6-07 Posición del camión: fuente, privacidad y actualización | D22, D23, D24 | Sí | Modelo de datos, privacidad y posible canal nuevo | Redactado (`proposed`): [ADR](../adr/20261106-posicion-del-camion-fuente-privacidad-y-actualizacion.md) |
| ADR-E6-08 Fotos de recepción en almacenamiento de objetos | D27 | Sí | Persistencia y privacidad | Redactado (`proposed`): [ADR](../adr/logistica-service/20261106-fotos-de-recepcion-en-almacenamiento-de-objetos.md) |
| Contrato de entregas para la entidad: remito, reporte y datos de Donaciones | Preguntas 5 y 14 de Raíces (HB-17) | Sí | Integración entre servicios y contrato | Redactado (`proposed`): [ADR](../adr/20261102-contrato-de-entregas-para-la-entidad-remito-reporte-y-datos-de-donaciones.md) |
| Ciclo de vida de la necesidad en el contrato | Preguntas 6 a 12 de Raíces (HB-18) | Sí | *Breaking change* de contrato | Redactado (`proposed`): [ADR](../adr/donaciones-service/20261102-ciclo-de-vida-de-la-necesidad-en-el-contrato.md) |
| Baja de una necesidad con entregas en curso | Pregunta 13 de Raíces (HB-19) | Sí | Integración entre servicios (comando y evento nuevos) | Redactado (`proposed`): [ADR](../adr/20261102-baja-de-una-necesidad-con-entregas-en-curso.md) |
| Nombre visible del donante y constancia de los legales | Preguntas 15, 16 y 18 de Raíces (HB-20) | Sí | Privacidad y datos personales | Redactado (`proposed`): [ADR](../adr/20261102-nombre-visible-del-donante-y-constancia-de-los-legales.md) |
| Dos alternativas de identidad visual con hojas propias | Refina D20 | Sí | Cambia D20 y la verificación de RNF-UI-11; costo de mantener dos hojas | Redactado (`proposed`): [ADR](../adr/cliente-liviano/20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md) |
| Tercera alternativa (Raíces) como prototipo propio | Refina el ADR de dos alternativas | Sí | Tercera identidad sin triplicar cada pantalla; dirección propia en la maqueta | Redactado (`proposed`): [ADR](../adr/cliente-liviano/20261101-tercera-alternativa-raices-como-prototipo-propio.md) |
| Interacción, movimiento y componentes: Alpine CSP, CSS nativo y capa CUBE con cerco | D31, D32, D33; modifica en parte D18 | Sí | Dependencia nueva, organización del CSS y reglas para agentes | Redactado (`proposed`): [ADR](../adr/cliente-liviano/20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md) |
| Réplicas por evento: fotos de recepción en Donaciones y donante en la entrega | HB-01, HB-16 (plan sin auth, 2026-11-06) | Sí | Integración entre servicios y contrato de dos mensajes | Redactado (`proposed`): [ADR](../adr/20261106-replicas-por-evento-fotos-de-recepcion-y-donante-de-la-entrega.md) |
| Una sola identidad visual (Raíces) y retiro de las alternativas | Reemplaza los dos ADR anteriores | Sí | Una pantalla se escribe una vez; el sitio en `/`; propuestas nuevas en su rama | Aceptado (`accepted`, 2026-11-04): [ADR](../adr/cliente-liviano/20261103-una-sola-identidad-en-el-cliente-liviano.md) |

Sin ADR (AGENTS.md §9.2):
- D28: endpoints aditivos sobre un patrón decidido.
- D29: decisión de calendario, no arquitectónica (el diseño del Key Broker ya está en el ADR de DTI-07). El ADR fija la cancelación en "Entrega 6"; la divergencia queda en §13 y en `DEUDA_TECNICA.md`, sin editar el ADR.

---

## 12. Riesgos y preguntas abiertas

| # | Riesgo o pregunta | Impacto | Mitigación o próximo paso |
|---|---|---|---|
| R1 | E6 suma tres componentes nuevos (IdP, gateway, cliente) para la fecha de entrega de E6 | Alto | Construir primero el camino crítico: IdP + gateway + login + una pantalla por rol |
| R2 | D1 cerrada con Keycloak: D8 necesita una extensión (SPI) que publique en RabbitMQ | Medio | D9 y RNF-AUTH-08 reducen el costo de cambiar de IdP |
| R3 | D12 puede incumplir la consigna | Alto | Consulta a la cátedra (§10, D12) |
| R4 | Los 4 servicios pasan a exigir token: se rompen los tests de integración, Postman y el preprod | Alto | Token de prueba por client credentials en los tests; colección de Postman con autenticación |
| R5 | La migración de `X-Actor` deja dos fuentes del actor durante E6 | Bajo | El token manda; el header solo se loguea |
| R6 | OpenFreeMap no tiene SLA | Bajo | Para la demo alcanza; el estilo del mapa permite cambiar de proveedor de teselas |
| P1 | ¿Keycloak configurado cuenta como "implementación de un SSO" y "servicio de Autenticación"? | Sí, según el equipo (2026-11-04) | Cátedra (D1) |
| P2 | ¿Un link de activación cuenta como "enviarle sus credenciales"? | — | Cátedra (D12) |

---

## 13. Deuda documental detectada (sin corregir en esta tarea)

| Documento | Divergencia |
|---|---|
| [`docs/arquitectura/contratos-rest.md`](../arquitectura/contratos-rest.md) | No lista `GET /api/entregas/mapa` ni los dos `GET /api/entregas/seguimiento/...` que existen en `EntregasController` `[OBSERVED]` · **Corregida** el 2026-11-06 (tarea 22 del [plan sin auth](plan-e6-trabajo-sin-auth.md)) |
| [`docs/ESTADO_DOCUMENTACION.md`](../ESTADO_DOCUMENTACION.md) §3.6 | Describe `cliente-liviano` como reservado para la "Entrega 5"; la consigna lo ubica en E6 · **Corregida** el 2026-10-29 (barrido de la renumeración de entregas) |
| [`docs/adr/DEUDA_TECNICA.md`](../adr/DEUDA_TECNICA.md) DTI-09 | Llama "Entrega 6: Despliegue, Observabilidad y Seguridad" a la entrega de la integración con `auth-service`; según el enunciado, E6 es "Interfaz de Usuario y Arquitectura MVC" y E7 es "Observabilidad y Seguridad" · **Marcada para revisar** en `DEUDA_TECNICA.md` el 2026-10-29 · **Corregida** el 2026-11-06: DTI-09 usa "Entrega 6: Interfaz de Usuario y Arquitectura MVC", que incluye el servicio de autenticación |
| [ADR DTI-07](../adr/notificaciones-service/20260902-dti-07-dependencia-diferida-de-auth-service-para-key-broker.md) (`proposed`) | Fija el "Hito Canónico de Cancelación" en "Entrega 6"; la D29 lo pasa a E7. No se edita el ADR: corresponde un ADR sucesor o una actualización por el revisor humano |
| [`docs/entregas/README.md`](../entregas/README.md) | Llama "Despliegue, Observabilidad y Seguridad" a E6; el enunciado completo lo corrige · **Corregida** el 2026-10-29 (barrido de la renumeración de entregas) |
| `logistica-service/.../dto/entregas/EntregaMapaPublicoResponseDTO.java` | El Javadoc dice que no incluye la dirección exacta, pero expone `latitud` y `longitud` exactas (HB-07) · **Corregida** el 2026-11-06: el Javadoc dice que las coordenadas son exactas y que no sale el texto de la dirección, como la D24 revisada (ADR-E6-07) |
| `docs/adr/DEUDA_TECNICA.md` DTI-09 y DTI-14 | Fechan E6 en la "semana del 23 de noviembre"; ni el enunciado ni el repo tienen el calendario que lo respalde · **Corregida** el 2026-11-06: usan el 13/11 del [mapa de entregas](../entregas/README.md), y DTI-09 el nombre de E6 del enunciado |
| `docs/entregas/` | El enunciado completo (`Enunciado-TP-Anual-2026.pdf`) estaba en la rama `E5_enunciado_tp_anual`, sin integrar · **Corregida** el 2026-10-29: el PDF está en `docs/entregas/` |
