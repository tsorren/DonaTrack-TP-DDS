# Huecos de backend de la corrida A/B de Raíces

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **2026-11-02 · lista de la [corrida A/B](../encargos/raices-ab-navegable.md)**, la que piden el
> encargo (sección 2) y las fichas [público](raices-publico-navegable.md),
> [donante](raices-donante-navegable.md) y [entidad](raices-entidad-navegable.md). Cada ficha copia
> los de sus pantallas; esta lista los junta y suma los que solo se ven en un sitio.

Qué muestra la maqueta con datos sintéticos y todavía no tiene de dónde salir. Cada línea: rol · pantalla · qué falta · por qué. Sin DTI ni ADR nuevos. Orden: Público, Donante, Entidad, Admin; dentro de cada rol, por pantalla. Las marcas "sitio A" y "sitio B" dicen en cuál se ve el dato; sin marca, en los dos. El sitio B se borró al elegir A (2026-11-03): sus líneas quedan como historia.

## Público

- Público · publico-donaciones, publico-mapa · fecha de la última actualización de los datos públicos · toda cifra dice cuándo se actualizó (tono, RF-CL-07); ningún endpoint la da; la maqueta dice "15 de octubre de 2026, 14:30".
- Público · publico-donaciones · entregas confirmadas con foto de la última semana o el último mes, con DTO público sin el donante (HB-01) y foto legible en público (hoy las fotos son privadas con URL firmada, RNF-PRIV-03) · el endpoint es `GET /api/donaciones/destacadas` (rama `E6_HB01_destacadas`); la maqueta usa 10 entregas sintéticas.
- Público · publico-entidades · entidades activas con pedidos vigentes, barrio aproximado y "qué es", sin contacto, con búsqueda por nombre y filtro por categoría (HB-08, HB-15, HB-18) · RF-22.
- Público · publico-entidades · total de entidades activas ("de las 46 que están en DonaTrack") (HB-02) · la misma cifra de la portada.
- Público · publico-entidades · si cada entidad confirmó entregas en el período, para enlazar "Ver sus entregas en el mapa" (el club no) · el listado de entidades no lo trae; obligaría a cruzarlo con las entregas del mapa (HB-07) · sitio B.
- Público · publico-ingresar · inicio de sesión con correo y contraseña y con Google (IdP, RF-04, D1 pendiente), bloqueo por intentos (RNF-AUTH-07) y el estado "entidad pendiente de aprobación" (HB-12) · la maqueta entra con tres cuentas de ejemplo.
- Público · publico-mapa · entregas confirmadas del período con barrio o coordenadas aproximadas, categoría, fecha y foto, filtrables por categoría (HB-07) · RF-02 y D24; la maqueta dibuja un mapa fijo con su lista.
- Público · publico-recuperar · pedido de enlace de un solo uso con vencimiento y respuesta neutra (RF-AUTH-13, RNF-AUTH-10) · no hay mecanismo de envío (D8 pendiente) ni IdP (D1); la maqueta va al aviso sin mandar nada.
- Público · publico-registro-donante · alta en el IdP y completar perfil (RI-AUTH-02, HB-11), constancia de los legales (HB-20) y quién ingresa por una empresa (D30 pendiente) · la maqueta pide el nombre de quien usa la cuenta y lleva al inicio del donante.
- Público · publico-registro-entidad · alta de la jurídica con dirección y un representante, en estado pendiente, y su aprobación (HB-12, RF-21) · RF-03; la maqueta lleva a la solicitud enviada.
- Público · publico-registro-entidad, publico-entidades · "Qué es" (comedor, merendero, escuela, hogar, club, biblioteca, otra) como lista fija, para el alta, el filtro y el ícono · JuridicaInputDTO trae rubro como texto libre; "Tipo de organización" sí existe (TipoJuridico: ONG, INSTITUCION, GUBERNAMENTAL; EMPRESA no se ofrece) · la maqueta ofrece la lista de la ficha (sitio A).

## Donante

- Donante · layout (acceso a Notificaciones en sus pantallas; en A, salvo las aprobadas) · cantidad de notificaciones sin leer · el conteo existe (GET /api/notificaciones/persona/{personaId}/no-leidas/conteo, NotificacionController); falta que el cliente liviano lo pida en cada pantalla sin traer la lista. No es un hueco de backend: queda como nota para la integración.
- Donante · donante-inicio · resumen de la empresa (entregas confirmadas, entidades que confirmaron, donaciones registradas, desde cuándo) · no hay endpoint agregado; contarlo de RF-05 obliga a cruzar cada donación con su entrega (HB-16).
- Donante · donante-inicio, donante-donaciones-detalle · nueva fecha de salida de una entrega con problema ("Sale de nuevo el 17 de octubre") · el historial de Logística registra el intento fallido; no se encontró dónde queda la reprogramación (sin verificar) · sitio B.
- Donante · donante-donaciones · filtro por categoría · HB-03.
- Donante · donante-donaciones, donante-donaciones-detalle · fecha de cada etapa y nota del estado · el listado de Donaciones no trae etapas; el historial está en Logística por entrega y desde la donación solo se llega por el seguimiento (HB-16).
- Donante · donante-donaciones-detalle · foto y fecha de recepción · SeguimientoEntregaResponseDTO no trae fotos.
- Donante · donante-donaciones-detalle · "Figura como" (nombre visible) · HB-20.
- Donante · donante-entidades · búsqueda por nombre, paginado, solo activas, tipo y zona · HB-08 y HB-15.
- Donante · donante-entidades · lo que pide cada entidad con cuánto recibió y cuánto falta · HB-18.
- Donante · donante-entidades · lo que la empresa ya donó a cada entidad · no hay filtro de donaciones de un donante por entidad asignada (HB-04 es del lado de la entidad).
- Donante · donante-entidades · dirección y horario del depósito · no hay endpoint de depósitos.
- Donante · donante-notificaciones · tipo de evento e id de la donación en NotificacionDTO · sin ellos no hay sello ni enlace al detalle (hoy solo mensaje).
- Donante · donante-perfil · datos de la cuenta (razón social, tipo, rubro, CUIT y dirección) y medios de contacto con el predeterminado · están en donaciones-service (`JuridicaInputDTO`, `MedioDeContactoInputDTO`), pero no hay `GET /api/personas/me` ni edición del perfil propio (RI-AUTH-02, HB-11) · agregado el 2026-11-04 (nota de Nico); la maqueta no guarda.
- Donante · donante-perfil · "Qué te avisamos" (todo o solo cuando la entidad confirma) · notificaciones-service no tiene preferencia de qué avisar · agregado el 2026-11-04; la maqueta no guarda.
- Donante · donante-notificaciones · avisos "recibida en el depósito" y "lista para entregar" · no están en EventoNotificableDTO; RF-08 pide asignación y recepción (HB-09).

- Donante · donante-misiones · desde cuándo está en su categoría · `.../ascensos` lo da, `DonantePerfilDTO` no. Sumado en F5.
- Donante · donante-misiones · cuál es la misión en curso y qué donación fue la más grande en Gran Aporte · `MisionDTO` no trae el número de misión ni el detalle del progreso. Sumado en F5.
- Donante · donante-misiones · la fecha del ascenso puede no coincidir con la de la misión que lo produjo (`CambioCategoria` guarda la fecha de proceso). Sumado en F5.

## Entidad

- Entidad · entidad-seguimiento · todas las entregas activas de la entidad en una llamada, con las pendientes de salir y las que esperan confirmación · GET /api/entregas/seguimiento/beneficiarios/{id} existe, sin verificar qué trae (HB-16). Sumado en F5.
- Entidad · entidad-seguimiento · la posición aproximada del camión · HB-05 y HB-06. Sumado en F5.
- Entidad · entidad-perfil · editar los datos y los medios de contacto de la entidad · HB-11, como en el donante. Sumado en F5.

- Entidad · entidad-inicio, entidad-recibidas · estado de la entrega (en la puerta, en camino, con problema) de todas las de la entidad en una llamada · Logística lo da por donación (HB-16).
- Entidad · entidad-recibidas · donaciones asignadas a la entidad, con filtro por estado y rango de fechas · GET /donaciones-independientes no filtra por entidad asignada (HB-04).
- Entidad · entidad-recibidas · pedido que cubre cada donación · falta necesidadId en la donación (HB-17).
- Entidad · entidad-recibidas · nombre visible de quien donó o su categoría · HB-20.
- Entidad · entidad-notificaciones · que asignación, en camino, entrega fallida y llegada generen notificación a la entidad · sin verificar (HB-09).
- Entidad · entidad-notificaciones · id de la donación o entrega en cada notificación, para enlazarla · los eventos traen solo descripcion.

## Admin

- Admin · admin-inicio · entidades que piden sumarse · falta el estado pendiente de aprobación (HB-12).
- Admin · admin-inicio · donaciones que vencen en los próximos días · GET /donaciones-independientes no filtra por vencimiento.
- Admin · admin-donantes · buscar por nombre o documento y paginar · GET /api/donantes lista todos y solo filtra por canal (como HB-15 en entidades). Sumado en F2 del roadmap de vistas.
- Admin · admin-donantes-registrar · no duplicar a una persona ya registrada · POST /api/personas no busca por documento (solo la importación lo hace). Sumado en F2.
- Admin · admin-donantes-importando e -importado · el avance y el resultado por fila · el backend guarda solo el estado del archivo (HB-14).
- Admin · admin-entidades · pedir corrección y rechazar con motivo, y el estado pendiente de aprobación · no existen (HB-12, RI-AUTH-05).
- Admin · admin-donaciones · una sola lista con estado, donante, depósito y vencimiento · el estado viene de GET /donaciones-independientes (solo trae donacionOriginalId) y el resto de GET /api/donaciones, con otro estado (EstadoDonacion); hoy hay que unirlas por donacionOriginalId. Sumado en F3.
- Admin · admin-donaciones?vence=pronto · filtrar por fecha de vencimiento · no existe el filtro. Sumado en F3.
- Admin · admin-donaciones · paginar (HB-04).
- Admin · admin-donaciones-registrar · la lista de depósitos para elegir · el DTO recibe nombreDeposito y una dirección, sin catálogo. Sumado en F3.
- Admin · admin-donaciones-registrar · divergencia contrato-código: openapi-donaciones.yaml (línea 1027) pide donanteId y DonacionesService lee dto.idDonante(). Reportada en F3, sin corregir (fuera del alcance del cliente liviano).
- Admin · admin-asignaciones · la entidad del pedido sin buscarla aparte (la propuesta trae la necesidad sin la entidad); el nombre del algoritmo y su puntaje no se exponen. Sumado en F3.
- Admin · admin-asignaciones · guardar y devolver el motivo y la fecha de la decisión: el controlador recibe la justificación pero no la pasa, y la propuesta no la guarda. Sumado en F3.
- Admin · admin-camiones · listar también los deshabilitados (GET /api/camiones usa solo los activos), para poder volver a habilitarlos, y devolver el motivo y la fecha del último cambio de estado (el PATCH recibe el motivo, pero CamionResponseDTO no lo trae). Sumado en F4.
- Admin · admin-inicio · las cifras de "Cómo está todo" (donaciones en depósito, asignadas y en camino, entregas confirmadas del mes, donantes, camiones en ruta y libres) · no hay un resumen agregado; contarlas obliga a traer las listas enteras de Donaciones, Logística y Usuarios en cada carga.
