# Flujo: Público "entrar", en Raíces (sitio navegable A/B)

> **Estado:** aprobada: el usuario eligió el sitio A de la corrida A/B (2026-11-03), como
> preveía el plan · **Fecha:** 2026-11-02 · **Aprobó:** el usuario, 2026-11-03
> Plantilla de la skill `ux-flujo`. Ficha escrita para los dos sitios de la corrida A/B de Raíces:
> quedó A, en `/<pantalla>` (B se borró al elegir A, 2026-11-03)
> ([ADR aceptado](../../adr/cliente-liviano/20261103-una-sola-identidad-en-el-cliente-liviano.md)).
> Fija para quién es, el recorrido, los estados, los textos, los datos y los enlaces de salida.
> **No fija la composición ni la navegación visual:** eso lo decide cada sitio.
> Pantallas nuevas: `publico-donaciones`, `publico-mapa`, `publico-entidades`, `publico-ingresar`,
> `publico-registro`, `publico-registro-donante`, `publico-registro-entidad` y `publico-recuperar`
> (con `publico-recuperar-enviado`). Ya aprobadas y
> relacionadas: `publico-portada`, `publico-contacto` (con `-enviado` y `-error`) y
> `publico-legales` ([contacto](raices-contacto.md), [legales](raices-legales.md)).

## 1. Para quién y para qué

| | |
|---|---|
| Quién | Quien llega por primera vez: la persona de responsabilidad social de una empresa que evalúa donar (el foco de Raíces), una persona que quiere donar, alguien de una entidad que quiere sumarse. También quien ya tiene cuenta y viene a ingresar. Desde la computadora, en la oficina; la entidad, muchas veces desde el celular |
| Tono | Portada y páginas públicas: profesional, seguro, directo. Formularios: claro, útil, tranquilizador. Errores: honesto, responsable, sereno ([tono según la pantalla](../tono-y-comunicacion.md#tono-según-la-pantalla)) |
| Qué quiere lograr | "Ver qué llegó y adónde, saber quiénes piden y qué, y entrar o crear mi cuenta para donar o para sumar mi entidad" |
| Dispositivo principal | Ambos; la computadora primero para la empresa, el celular para la entidad |
| Requerimientos | RF-01 (destacadas del último mes con su foto, registro y login, legales). RF-02 (mapa de entregadas, detalle por marcador con fecha, categoría y foto; ubicación aproximada, D24). RF-03 (registro: el donante queda operativo al completar el perfil; la entidad, pendiente de aprobación). RF-04 (login de los tres roles; credenciales inválidas, error genérico). RF-22 (entidades activas con necesidades vigentes, sin datos de contacto). RF-AUTH-02 (Continuar con Google), RF-AUTH-05/06, RF-AUTH-13 (recuperar). RNF-AUTH-07 (bloqueo tras varios intentos) y RNF-AUTH-10 (respuestas que no revelan cuentas). RF-CL-06 (lista junto al mapa), RF-CL-07 (cuándo se actualizó). RNF-PRIV-01/03 |
| Datos que usa | Contadores: `GET /api/donaciones/contadores` (HB-02, rama `E6_HB02_contadores_publicos`). Lo demás no existe para lo público todavía: HB-01 (destacadas), HB-07 (mapa), HB-08, HB-15 y HB-18 (entidades y necesidades), HB-11 y HB-12 (perfil y aprobación), HB-20 (constancia de los legales), IdP (RF-04, D1 pendiente). En la maqueta: los datos de ejemplo de la sección 6.3, escritos en la plantilla como en la portada; de `SitioController`, los `estados` (`EstadoEntrega.ENTREGADA` para el sello) y la foto de ejemplo; de `Escenario`, los nombres `vacio`, `lento` y `error`. Los huecos, en [Huecos de backend](#huecos-de-backend) y en la lista de la corrida ([raices-ab-huecos-backend.md](raices-ab-huecos-backend.md)) |
| Boceto de E1 | Pág. 2 (Donaciones realizadas: mapa coloreado, galería en mosaico, "Unirse a la comunidad"), pág. 23 (mapas por región, detalle de una donación), pág. 4 (Iniciar sesión), pág. 5 (Registrarse: dos tarjetas) y pág. 11 (Registro de entidad). Entidades y necesidades no tiene boceto (D26) |

## 2. Dónde está en el sitio

Todo es público (sin sesión). Nivel 1: la portada. Nivel 2: Entregas confirmadas, Mapa,
Entidades, Ingresar y Crear una cuenta. Nivel 3: Registro de donante, Registro de entidad y
Recuperar la contraseña (desde Ingresar) ([mapa del sitio](../mapa-del-sitio.md), filas de Público). Nada queda a más de 3 niveles.

**Destinos de la navegación pública** (los mismos en todas las públicas, también en la portada;
cómo se ven lo decide cada sitio). Contacto y Términos y privacidad la llevan sin sección marcada
desde el pulido del 2026-11-03 (usuario): antes no la tenían y, en el celular, la marca era la
única salida.

| Destino | Pantalla | Nota |
|---|---|---|
| La marca | `publico-portada` | "DonaTrack, ir a la portada" |
| Entregas confirmadas | `publico-donaciones` | En el mapa del sitio, "Donaciones realizadas" (ver Decisiones) |
| Mapa | `publico-mapa` | |
| Entidades | `publico-entidades` | |
| Ingresar | `publico-ingresar` | |
| Crear una cuenta | `publico-registro` | |

La sección actual lleva `aria-current="page"`. El pie es el de las otras públicas ("Términos y
privacidad · Contacto"). Para que el recorrido salga de la portada, la portada suma esta
navegación; además, conviene que "Lo que viajó este mes" enlace a Entregas confirmadas ("Ver todas
las entregas confirmadas") y que el rótulo de la tierra enlace a Entidades ("Ver todas las
entidades y lo que piden"). Lo demás de la portada no cambia.

## 3. Recorrido

```mermaid
flowchart TD
  portada([Portada]) --> donaciones[Entregas confirmadas]
  portada --> mapa[Mapa]
  portada --> entidades[Entidades]
  portada -->|Quiero donar| regDonante[Registro de donante]
  portada -->|Sumar mi entidad| regEntidad[Registro de entidad]
  portada --> ingresar[Ingresar]
  donaciones <--> mapa
  mapa <--> entidades
  donaciones --> regDonante
  entidades -->|Quiero donar| regDonante
  entidades -->|Sumar mi entidad| regEntidad
  ingresar -->|Crear una cuenta| registro[Crear una cuenta: elegir]
  registro --> regDonante
  registro --> regEntidad
  ingresar -->|datos correctos| inicioRol([Inicio del rol: donante, entidad o admin])
  ingresar -->|no coinciden| ingresarError[Ingresar, con el error]
  ingresarError --> ingresar
  ingresar -->|¿Olvidaste tu contraseña?| recuperar[Recuperar contraseña]
  ingresarError -->|Pedir una contraseña nueva| recuperar
  recuperar -->|correo escrito| recuperarEnviado[Aviso: si hay cuenta, te mandamos un enlace]
  recuperarEnviado -->|Volver a Ingresar| ingresar
  regDonante -->|datos completos| inicioDonante([Inicio del donante])
  regDonante -->|falta algo| regDonanteError[Registro de donante, con lo que falta]
  regEntidad -->|datos completos| enviada[Solicitud enviada]
  regEntidad -->|falta algo| regEntidadError[Registro de entidad, con lo que falta]
  enviada --> entidades
  enviada --> portada
```

Desvíos considerados:

- **Volver o cancelar:** la navegación pública y Atrás del navegador, en todas. Los formularios no
  guardan nada hasta enviarse; irse no deja nada a medias.
- **Filtro sin resultados:** en Mapa (una categoría sin entregas), en Entidades (una categoría que
  nadie pide o una búsqueda sin coincidencias). Cada uno dice qué no se encontró y ofrece "Ver
  todas".
- **Sin datos todavía:** Entregas confirmadas, Mapa y Entidades tienen su estado vacío
  (`?escenario=vacio`).
- **Error del servidor o sin conexión:** al cargar (Entregas confirmadas, Mapa, Entidades) y al
  enviar (Ingresar, los registros y Recuperar la contraseña). Lo escrito sigue en el formulario, salvo la contraseña.
- **Error de datos:** Ingresar dice "no coinciden" sin decir cuál de los dos datos falló (RF-04,
  RNF-AUTH-10); los registros, un resumen arriba con un enlace a cada campo.
- **Demasiados intentos** (RNF-AUTH-07): Ingresar dice que se pausaron los intentos por unos
  minutos.
- **Entidad en revisión** (RF-AUTH-06): si una entidad que todavía no se aprobó intenta ingresar,
  Ingresar le dice que está en revisión.
- **Sesión vencida** (RF-CL-02): Ingresar dice que la sesión se cerró; en el sistema, al ingresar
  vuelve a la pantalla donde estaba.
- **Sin permiso:** no aplica; todo es público.

**En la maqueta:** "Ingresar" lleva al inicio de cada rol con su cuenta de ejemplo (sección 6.3).
"¿Olvidaste tu contraseña?" lleva a Recuperar la contraseña de cada sitio
(`publico-recuperar`, con su aviso en `publico-recuperar-enviado`): lo público sin dibujar no tiene
página de "sin dibujar", así que el paso se dibuja (ver Decisiones).

## 4. Pantallas

Cada esquema dice **qué hay y en qué orden de importancia**, no dónde va. La composición, la
imagen de raíz del título (como mucho una por pantalla, [brief](../identidad/brief.md#voz))
y cómo se dibujan el mapa y los filtros los decide cada sitio.

### 4.1 Entregas confirmadas (`publico-donaciones`)

```text
Rótulo · Título · Bajada (qué se ve y qué no)
Período: [Última semana] [Último mes ●]          ← formulario get, ?periodo=semana
Cifra del período, con fechas · Actualizado …
┌ Entrega ───────────────────────────────┐ × 10
│ Foto (miniatura) · sello "Entrega       │
│ confirmada" · qué y cuánto · categoría  │
│ con su ícono · entidad y barrio · fecha │
│ [Ver la foto de la entrega]             │
└─────────────────────────────────────────┘
Salidas: Ver dónde llegaron, en el mapa · Quiero donar
```

La galería del boceto (pág. 2), sin el mapa: el mapa tiene su pantalla. La más reciente primero.
La foto grande se abre en la misma página (con JavaScript, un diálogo; sin él, un ancla a la foto
o la imagen sola), con el mismo texto que en la portada.

### 4.2 Mapa (`publico-mapa`)

```text
Rótulo · Título · Bajada (ubicación aproximada)
Qué se donó: [Todas ●][Alimentos][Ropa][Muebles][Útiles escolares]   ← ?categoria=
Cifra: entregas y entidades del período · Actualizado …
El mapa (en el sistema, MapLibre; en la maqueta, un dibujo fijo):
  un punto por entidad, en su barrio; al tocarlo, el detalle
  de sus entregas (fecha, categoría, qué, foto)
La lista, con los mismos datos (RF-CL-06): entidad por entidad,
  con su barrio y sus entregas; cada una, "Ver la foto de la entrega"
Atribución de los datos del mapa (D21), solo en el sistema
Salidas: Ver las fotos de cada entrega · Ver las entidades y lo que piden · Quiero donar
```

El boceto (págs. 2 y 23) colorea un mapa mundial por cantidad y baja por regiones. Acá, un solo
mapa con un punto por entidad: DonaTrack trabaja en una ciudad y el acercamiento lo da MapLibre.
El detalle de cada marcador cumple RF-02 (fecha, categoría y foto). Cómo se dibuja el estilo del
mapa (por ejemplo, con la red de la portada) lo decide cada sitio; el dibujo de la maqueta es
`aria-hidden` y la lista dice todo.

### 4.3 Entidades (`publico-entidades`)

```text
Rótulo · Título · Bajada (qué es un pedido y qué pasa con lo que donás)
Buscar por nombre [          ] [Buscar]          ← ?q=
Qué piden: [Todo ●][Alimentos][Ropa][Muebles][Útiles escolares]   ← ?categoria=
Cifra: entidades con pedidos vigentes, de cuántas · Al …
┌ Entidad ───────────────────────────────┐ × 6
│ Ícono de lo que pide · qué es (Comedor) │
│ · nombre · barrio                       │
│ Pide: pedido por pedido (qué, cuánto    │
│ falta, hasta cuándo o si se repite, por │
│ qué). Más de 3: los demás plegados,     │
│ "Ver los 5 pedidos"                     │
│ Ver sus entregas en el mapa (si tiene)  │
└─────────────────────────────────────────┘
Salidas: Quiero donar · ¿Ya tenés cuenta? Ingresar · ¿Representás a una entidad? Sumar mi entidad
```

Sin boceto (D26). Sin datos de contacto ni dirección (RF-22, D24). No hay un botón para donar a
una entidad: la asignación la decide DonaTrack (RF-17), así que el llamado es uno solo, "Quiero
donar", y la bajada dice cómo se asigna.

### 4.4 Ingresar (`publico-ingresar`)

```text
Rótulo · Título · Bajada
Aviso del estado, si hay (error, en revisión, bloqueado, sesión vencida)
Correo electrónico [        ]
Contraseña         [        ] [Mostrar]
¿Olvidaste tu contraseña?
[Ingresar]
o
[Continuar con Google]
¿Primera vez? Crear una cuenta
── Solo en la maqueta ──
Cuentas de ejemplo: Distribuidora Ejemplo SA · Martina Ruiz · Comedor Barrio Ejemplo · Administración
```

Respecto del boceto (pág. 4): "Correo electrónico" en lugar de "Usuario" (el IdP usa el correo) y
cada campo con su etiqueta visible (el boceto usaba la ayuda como etiqueta y se perdía al
escribir). Se suma "Continuar con Google" (RF-AUTH-02).

### 4.5 Crear una cuenta (`publico-registro`)

```text
Rótulo · Título · Bajada
┌ Quiero donar, yo o mi empresa ─────┐  ┌ Represento a una entidad ──────────┐
│ Qué podés hacer (2 líneas)          │  │ Qué podés hacer (2 líneas)          │
│ Qué te pedimos                      │  │ Qué te pedimos · se revisa antes    │
│ [Crear mi cuenta de donante]        │  │ [Sumar mi entidad]                  │
└─────────────────────────────────────┘  └─────────────────────────────────────┘
¿Ya tenés cuenta? Ingresar
```

Las dos tarjetas del boceto (pág. 5), con "entidad" en lugar de "beneficiario" (tono: se nombra
por su rol).

### 4.6 Registro de donante (`publico-registro-donante`)

```text
Rótulo · Título · Bajada
Quién dona: [Una empresa u organización ●] [Una persona]     ← ?tipo=persona
Empresa: Nombre de la empresa · Tu nombre y apellido
Persona: Nombre · Apellido
Correo electrónico · Contraseña [Mostrar]
Aviso del nombre visible
[ ] Leí y acepto los términos y la política de privacidad (versión 1)
[Crear mi cuenta]
¿Ya tenés cuenta? Ingresar · ¿Representás a una entidad? Sumar mi entidad
```

Sin boceto propio. La empresa va primero: es el foco de Raíces. La elección cambia los campos sin
JavaScript (dos direcciones, `publico-registro-donante` y `?tipo=persona`); con JavaScript, en el
lugar. Al crear la cuenta, en la maqueta, lleva al inicio del donante (`donante-inicio`, con la
cuenta de Distribuidora Ejemplo SA): el donante queda operativo al completar el perfil (RF-03).

### 4.7 Registro de entidad (`publico-registro-entidad`)

```text
Rótulo · Título · Bajada
Cómo sigue: 1 Completás la solicitud (estás acá) · 2 El equipo la revisa · 3 Publicás lo que necesitás
La entidad: Nombre · Qué es · Tipo de organización
Dirección de entrega: Calle · Altura · Localidad · Provincia
Quién la representa: Nombre y apellido · Correo electrónico · Contraseña [Mostrar]
[ ] Leí y acepto los términos y la política de privacidad (versión 1)
[Enviar la solicitud]
¿Querés donar? Crear una cuenta de donante
```

**Solicitud enviada** (`publico-registro-entidad-enviada`): el sello "Solicitud recibida", qué pasa
ahora y el mismo "Cómo sigue" con el paso 2 como actual. Salidas: "Ver las entidades y lo que
piden" e "Ir a la portada".

Respecto del boceto (pág. 11): en tres grupos con título, como pedía la nota del boceto ("conviene
partirlo en secciones claras"). Un solo representante, el que crea la cuenta; el rubro pasa a "Qué
es", una lista corta. El país no se pregunta: es Argentina.

### 4.8 Recuperar la contraseña (`publico-recuperar`)

```text
Rótulo · Título · Bajada
Correo electrónico [        ]   (ayuda del navegador si falta o no es un correo)
[Mandame el enlace]
¿Te acordaste? Volver a Ingresar
── después de enviar (publico-recuperar-enviado) ──
Aviso: Si hay una cuenta con ese correo, te mandamos un enlace. · Revisá tu bandeja… · [Volver a Ingresar]
```

Se llega desde Ingresar ("¿Olvidaste tu contraseña?" y "Pedir una contraseña nueva", en el error
de datos y en el de demasiados intentos). Los mismos datos que la del primer prototipo: un correo y el enlace
(RF-AUTH-13: enlace de un solo uso con vencimiento; el mecanismo de envío y el plazo dependen de
D8, pendiente). El aviso de después es neutro: dice lo mismo haya o no una cuenta con ese correo
(RNF-AUTH-10). Es un formulario real (POST); el aviso es su propia pantalla, como en Contacto. Sin
boceto.

## 5. Estados

Los estados de la maqueta van en la misma pantalla con `?escenario=` (como la portada); los
resultados de un envío, en su propia pantalla (como Contacto).

| Pantalla | Estado | Qué se ve | Texto |
|---|---|---|---|
| Entregas confirmadas | Con datos | La cifra y las 10 entregas del último mes | Ver Textos |
| Entregas confirmadas | Última semana (`?periodo=semana`) | Las 5 entregas del 8 al 15 de octubre | "5 entregas confirmadas en la última semana, del 8 al 15 de octubre de 2026." |
| Entregas confirmadas | Vacío (`?escenario=vacio`) | El título y el aviso en lugar de la galería; sin cifra | "Todavía no hay entregas confirmadas en el último mes." · "Se cuentan cuando cada entidad confirma lo que recibe, con su foto." · "Ver las entidades y lo que piden" |
| Entregas confirmadas | Cargando (`?escenario=lento`) | El lugar de cada entrega, sin datos inventados | "Cargando las entregas confirmadas…" |
| Entregas confirmadas | Error del servidor (`?escenario=error`) | El aviso en lugar de la galería | "No pudimos cargar las entregas confirmadas." · "Es un problema nuestro. Probá de nuevo en unos minutos; si sigue, escribinos con este código." · "Código: a1b2c3d4e5f6" · "Volver a cargar" · "escribinos" lleva a Contacto con el código y el regreso (`?codigo=a1b2c3d4e5f6&volver=…`) |
| Entregas confirmadas | Foto que no carga | El texto alternativo en su lugar y los datos de la entrega | El `alt` de la foto |
| Mapa | Con datos | El mapa y la lista, 10 entregas en 5 entidades | Ver Textos |
| Mapa | Filtro (`?categoria=alimentos`, `ropa`, `muebles`) | El mapa y la lista, solo con esa categoría; la cifra cambia | "5 entregas de alimentos en 1 entidad …" (ver 6.3) |
| Mapa | Filtro sin resultados (`?categoria=escolares`) | El mapa sin puntos y el aviso en lugar de la lista | "Todavía no hay entregas de útiles escolares en el último mes." · "Cuando una entidad confirme una entrega de esta categoría, aparece en el mapa." · "Ver todas las categorías" |
| Mapa | Vacío (`?escenario=vacio`) | El mapa sin puntos y el aviso; sin filtro ni cifra | "Todavía no hay entregas confirmadas en el último mes." · "Se cuentan cuando cada entidad confirma lo que recibe." · "Ver las entidades y lo que piden" |
| Mapa | Cargando (`?escenario=lento`) | El lugar del mapa y de la lista | "Cargando el mapa de entregas…" |
| Mapa | Error del servidor (`?escenario=error`) | El aviso en lugar del mapa y la lista | "No pudimos cargar el mapa de entregas." · el mismo texto, código y botón de Entregas confirmadas |
| Mapa | Detalle de un punto | La entidad, su barrio y sus entregas, cada una con fecha, categoría y foto | "Comedor Barrio Ejemplo · Barrio Ejemplo · 6 entregas confirmadas" · "Lentejas secas, 10 kg · Alimentos · Entrega confirmada el 10 de octubre de 2026" · "Ver la foto de la entrega" · "Cerrar" |
| Entidades | Con datos | La cifra y las 6 entidades | Ver Textos |
| Entidades | Filtro (`?categoria=…`) | Solo las que piden algo de esa categoría, y de cada una solo esos pedidos | "2 entidades piden alimentos." |
| Entidades | Filtro sin resultados (`?q=biblioteca&categoria=alimentos`: en la maqueta cada categoría tiene al menos una entidad, así que se llega combinando búsqueda y filtro) | El aviso en lugar de la lista; el texto sigue en el buscador y la categoría, elegida | "Ninguna entidad con «biblioteca» en el nombre pide alimentos ahora." · "Probá con otra categoría o con otra parte del nombre." · "Ver todas". Con una categoría sola y sin búsqueda: "Ninguna entidad pide <categoría> ahora." · "Cuando una lo publique, aparece acá." |
| Entidades | Búsqueda (`?q=comedor`) | Las que tienen ese texto en el nombre | "1 entidad con «comedor» en el nombre." |
| Entidades | Búsqueda sin resultados (`?q=…` sin coincidencias) | El aviso en lugar de la lista; el texto sigue en el buscador | "No encontramos entidades con «<texto>» en el nombre." · "Probá con otra parte del nombre o sacá el filtro." · "Ver todas" |
| Entidades | Vacío (`?escenario=vacio`) | El aviso; sin buscador, filtro ni cifra | "Ninguna entidad tiene pedidos vigentes ahora." · "Cuando una publique lo que le hace falta, aparece acá." · "Sumar mi entidad" |
| Entidades | Cargando (`?escenario=lento`) | El lugar de cada entidad | "Cargando las entidades…" |
| Entidades | Error del servidor (`?escenario=error`) | El aviso en lugar de la lista | "No pudimos cargar las entidades." · el mismo texto, código y botón |
| Ingresar | Inicial | El formulario vacío y las cuentas de ejemplo | — |
| Ingresar | Ingresando (`?escenario=lento`) | El botón dice "Ingresando…" y queda desactivado | "Ingresando…" |
| Ingresar | Error de datos (`publico-ingresar-error`) | Aviso arriba, con el foco; los dos campos marcados, sin decir cuál falló | "No pudimos ingresar." · "El correo o la contraseña no coinciden. Revisalos y probá de nuevo, o pedí una contraseña nueva." · "Pedir una contraseña nueva" |
| Ingresar | Error del servidor (`?escenario=error`) | Aviso arriba; el correo sigue escrito | "No pudimos ingresar en este momento." · "Es un problema nuestro. Probá de nuevo en unos minutos; si sigue, escribinos con este código." · el código |
| Ingresar | Sin conexión (`?escenario=sin-conexion`) | Con JavaScript, aviso arriba; sin él, la página del navegador | "Se cortó la conexión." · "Revisá la señal y probá de nuevo." |
| Ingresar | Demasiados intentos (`?escenario=bloqueado`) | Aviso arriba; el botón sigue, el ingreso se rechaza hasta que pase la pausa | "Pausamos los intentos de ingreso." · "Hubo varios intentos con datos que no coinciden. Por seguridad, probá de nuevo en unos minutos o pedí una contraseña nueva." |
| Ingresar | Entidad en revisión (`?escenario=pendiente`) | Aviso arriba, sin formulario de por medio | "Tu entidad todavía está en revisión." · "El equipo de DonaTrack revisa cada entidad antes de sumarla. Te escribimos al correo de contacto cuando esté aprobada; desde ese día podés ingresar." · "Escribinos" (Contacto) · "Ir a la portada" |
| Ingresar | Sesión vencida (`?escenario=sesion-vencida`) | Aviso arriba, tranquilo | "Tu sesión se cerró." · "Pasó un tiempo sin actividad. Ingresá de nuevo y seguís donde estabas." |
| Crear una cuenta | Con datos | Las dos tarjetas | Ver Textos (no tiene datos que carguen) |
| Registro de donante | Empresa (por defecto) / Persona (`?tipo=persona`) | Los campos de cada uno | — |
| Registro de donante | Creando (`?escenario=lento`) | El botón dice "Creando tu cuenta…" y queda desactivado | "Creando tu cuenta…" |
| Registro de donante | Éxito | En la maqueta, el inicio del donante | En el sistema, el inicio da la bienvenida (lo define la ficha del donante) |
| Registro de donante | Error de datos (`publico-registro-donante-error`) | Resumen arriba con el foco y un enlace a cada campo; cada campo con su error; lo escrito sigue, salvo la contraseña | "Revisá tres cosas antes de crear tu cuenta" · "Falta el nombre de la empresa" · "El correo no parece completo: falta la @" · "Para crear tu cuenta, aceptá los términos y la política de privacidad" |
| Registro de donante | Error del servidor (`?escenario=error`) | Aviso arriba; lo escrito sigue, salvo la contraseña | "No pudimos crear tu cuenta." · "Es un problema nuestro: lo que escribiste sigue acá, salvo la contraseña. Probá de nuevo en unos minutos; si sigue, escribinos con este código." · el código |
| Registro de entidad | Enviando (`?escenario=lento`) | El botón dice "Enviando la solicitud…" y queda desactivado | "Enviando la solicitud…" |
| Registro de entidad | Error de datos (`publico-registro-entidad-error`) | Resumen arriba con el foco y enlaces; cada campo con su error | "Revisá dos cosas antes de enviar" · "Elegí qué es tu entidad" · "Falta la calle de la dirección de entrega" |
| Registro de entidad | Error del servidor (`?escenario=error`) | Aviso arriba; lo escrito sigue, salvo la contraseña | "No pudimos enviar la solicitud." · "Es un problema nuestro: lo que escribiste sigue acá, salvo la contraseña. Probá de nuevo en unos minutos; si sigue, escribinos con este código." · el código |
| Solicitud enviada | Éxito | El sello, qué pasa ahora y "Cómo sigue" con el paso 2 actual | Ver Textos |
| Recuperar la contraseña | Inicial | El campo de correo vacío y el botón | Ver Textos |
| Recuperar la contraseña | Enviando | En A, con JavaScript, el botón dice "Enviando…" y queda desactivado; B no carga JavaScript en lo público y no lo muestra | "Enviando…" |
| Recuperar la contraseña | Aviso enviado (`publico-recuperar-enviado`) | El aviso en lugar del formulario, con el foco; el mismo haya o no cuenta (RNF-AUTH-10) | "Si hay una cuenta con ese correo, te mandamos un enlace." · "Revisá tu bandeja de entrada: el enlace sirve una sola vez y por tiempo limitado." · "Volver a Ingresar" |
| Recuperar la contraseña | Error de datos | El aviso del navegador en el campo (`required` y `type="email"`), antes de enviar; no hay otro error de datos, porque el aviso no dice si el correo tiene cuenta | El del navegador |
| Recuperar la contraseña | Error del servidor | Aviso arriba; el correo sigue escrito. Se describe, no se dibuja en la maqueta | "No pudimos mandar el enlace en este momento." · "Es un problema nuestro. Probá de nuevo en unos minutos; si sigue, escribinos con este código." · el código |

Errores de un campo que el navegador ya evita: `required`, `type="email"` y `minlength` muestran
su aviso propio antes de enviar; las pantallas `-error` muestran el error que vuelve del servidor.

## 6. Textos

Los títulos llevan una parte en cursiva (la marca la decide cada sitio). Estados, cifras, acciones
y errores dicen lo mismo en A y en B, con estas palabras ([tono](../tono-y-comunicacion.md#la-voz-propia-de-la-identidad)).

### 6.1 Por pantalla

| Pantalla | Elemento | Texto | Nota |
|---|---|---|---|
| Entregas confirmadas | Rótulo y título | "Entregas confirmadas" · "Lo que ya llegó, con su foto" | "Llegó" lo respalda la bajada: lo confirmó la entidad |
| | Bajada | "Cada entrega la confirmó la entidad que la recibió, con la fecha y una foto de lo entregado. Mostramos qué se donó, a qué entidad llegó y cuándo; no mostramos quién donó." | Dice qué respalda la cifra y qué se oculta |
| | Período | "Período" · "Última semana" · "Último mes" · "Ver" | El botón, solo sin JavaScript |
| | Cifra | "10 entregas confirmadas en el último mes, del 16 de septiembre al 15 de octubre de 2026." · "Actualizado el 15 de octubre de 2026, 14:30." | Período, fuente y fecha |
| | Cada entrega | "Sillas, 6 unidades" · "Muebles" · "Escuela Ejemplo N.º 12 · Centro Ejemplo" · "Entrega confirmada el 14 de octubre de 2026" · "Ver la foto de la entrega" | El sello dice "Entrega confirmada", con su forma (tilde) |
| | Foto grande | "Escuela Ejemplo N.º 12 confirmó la entrega el 14 de octubre de 2026 y subió esta foto." · "Cerrar" | El mismo de la portada |
| | Salidas | "Ver dónde llegaron, en el mapa" · "Quiero donar" | |
| Mapa | Rótulo y título | "Mapa" · "Adónde llegaron las donaciones" | |
| | Bajada | "Cada punto es una entidad que confirmó entregas en el último mes. La ubicación es aproximada: mostramos el barrio, nunca la dirección." | RNF-PRIV-01, D24 |
| | Filtro | "Qué se donó" · "Todas" · "Alimentos" · "Ropa" · "Muebles" · "Útiles escolares" · "Filtrar" | "Filtrar", solo sin JavaScript |
| | Cifra | "10 entregas confirmadas en 5 entidades, del 16 de septiembre al 15 de octubre de 2026." · "Actualizado el 15 de octubre de 2026, 14:30." | |
| | Lista | "Entidad por entidad" · "Comedor Barrio Ejemplo · Barrio Ejemplo · 6 entregas confirmadas" · cada entrega: "Lentejas secas, 10 kg · Alimentos · 10 de octubre" · "Ver la foto de la entrega" | La lista es la alternativa completa al mapa |
| | Atribución (sistema) | "Mapa: © OpenStreetMap" | D21; en la maqueta no hay mapa de terceros |
| | Salidas | "Ver las fotos de cada entrega" · "Ver las entidades y lo que piden" · "Quiero donar" | |
| Entidades | Rótulo y título | "Entidades" · "Lo que piden las entidades" | La entidad actúa: pide |
| | Bajada | "Comedores, escuelas, hogares y otras entidades de la comunidad publican lo que les hace falta. Lo que donás se asigna a una entidad que lo pidió." | No promete elegir la entidad (RF-17) |
| | Buscar | "Buscar por nombre" · "Buscar" | Ayuda: "Por ejemplo: comedor." |
| | Filtro | "Qué piden" · "Todo" · "Alimentos" · "Ropa" · "Muebles" · "Útiles escolares" · "Filtrar" | |
| | Cifra | "6 entidades con pedidos vigentes, de las 46 que están en DonaTrack. Al 15 de octubre de 2026." | Las 46 son las de la portada |
| | Cada pedido | "Lentejas secas · Faltan 30 de 40 kg este mes · Se repite cada mes" · "Por qué: para el almuerzo de lunes a viernes." · en uno de una sola vez: "Aceite de girasol · Faltan 20 litros · Hasta el 20 de octubre" | Lo pendiente, pendiente: "faltan", no "recibió" |
| | Plegado | "Ver los 5 pedidos" · "Ver menos" | Solo si tiene más de 3 |
| | Enlace al mapa | "Ver sus entregas en el mapa" | Solo si confirmó entregas en el período (el club no) |
| | Salidas | "Quiero donar" · "¿Ya tenés cuenta? Ingresar" · "¿Representás a una entidad? Sumar mi entidad" | |
| Ingresar | Rótulo y título | "Ingresar" · "Entrá a tu cuenta" | |
| | Bajada | "Para seguir tus donaciones o las entregas de tu entidad." | |
| | Campos | "Correo electrónico" · "Contraseña" · "Mostrar" / "Ocultar" | "Mostrar", con JavaScript |
| | Enlaces | "¿Olvidaste tu contraseña?" · "¿Primera vez? Crear una cuenta" | |
| | Acciones | "Ingresar" · "o" · "Continuar con Google" | |
| | Cuentas de ejemplo | "Cuentas de ejemplo" · "Es un prototipo: no se guarda nada. Entrá con una cuenta de ejemplo." · "Distribuidora Ejemplo SA, empresa donante" · "Martina Ruiz, persona donante" · "Comedor Barrio Ejemplo, entidad" · "Administración" | Solo en la maqueta |
| Recuperar la contraseña | Rótulo y título | "Ingresar" · "Pedí una contraseña nueva" | El rótulo es la sección: se llega desde Ingresar |
| | Bajada | "Escribí el correo de tu cuenta y te mandamos un enlace para crear una contraseña nueva." | |
| | Campo y acción | "Correo electrónico" · "Mandame el enlace" | Verbo y resultado |
| | Salida | "¿Te acordaste? Volver a Ingresar" | |
| | Aviso enviado | "Si hay una cuenta con ese correo, te mandamos un enlace." · "Revisá tu bandeja de entrada: el enlace sirve una sola vez y por tiempo limitado." · "Volver a Ingresar" | No revela cuentas (RNF-AUTH-10) y no da por hecho un envío que puede no haber pasado |
| Crear una cuenta | Rótulo y título | "Crear una cuenta" · "¿Cómo querés sumarte?" | |
| | Bajada | "Según lo que elijas, te pedimos datos distintos." | |
| | Donante | "Quiero donar, yo o mi empresa" · "Registrás lo que donás y lo seguís hasta que la entidad confirma la entrega." · "Ves a qué entidad se asignó, en qué estado está y la foto de la entrega." · "Te pedimos tu correo, una contraseña y tu nombre o el de tu empresa." · "Crear mi cuenta de donante" | Los pasos de la lámina de la portada |
| | Entidad | "Represento a una entidad" · "Publicás lo que le hace falta a tu entidad y confirmás cada entrega con una foto." · "Te pedimos los datos de la entidad, una dirección de entrega y quién la representa. El equipo de DonaTrack la revisa antes de sumarla." · "Sumar mi entidad" | "Sumar mi entidad", como en la portada |
| | Salida | "¿Ya tenés cuenta? Ingresar" | |
| Registro de donante | Rótulo y título | "Crear una cuenta" · "Tu cuenta de donante" | |
| | Bajada | "Con tu cuenta seguís cada donación, desde que la registrás hasta que la entidad confirma la entrega." | |
| | Quién dona | "Quién dona" · "Una empresa u organización" · "Una persona" | |
| | Empresa | "Nombre de la empresa" (ayuda: "Lo ven las entidades que reciben lo que donás.") · "Tu nombre y apellido" (ayuda: "Quien usa la cuenta en nombre de la empresa.") | |
| | Persona | "Nombre" · "Apellido" | |
| | Correo | "Correo electrónico" · ayuda: "Para ingresar y para escribirte sobre tus donaciones. No lo mostramos a nadie." | Para qué se pide (RNF-UI-04) |
| | Contraseña | "Contraseña" · ayuda: "Al menos 8 caracteres." | Regla propuesta (ver Preguntas) |
| | Aviso del nombre | Empresa: "Las entidades que reciben lo que donás ven el nombre de tu empresa. Podés ocultarlo cuando quieras en Mi perfil." · Persona: "Las entidades que reciben lo que donás ven tu nombre. Podés ocultarlo cuando quieras en Mi perfil." | De la [ficha de legales](raices-legales.md) |
| | Casilla | "Leí y acepto los términos y la política de privacidad (versión 1)" | Con enlace a `publico-legales` |
| | Obligatorios y acción | "Todos los datos son obligatorios." · "Crear mi cuenta" | |
| | Salidas | "¿Ya tenés cuenta? Ingresar" · "¿Representás a una entidad? Sumar mi entidad" | |
| Registro de entidad | Rótulo y título | "Crear una cuenta" · "Sumá tu entidad" | |
| | Bajada | "Para comedores, merenderos, escuelas, hogares y otras organizaciones de la comunidad. El equipo de DonaTrack revisa cada solicitud antes de sumarla." | Sin plazo: no hay uno acordado |
| | Cómo sigue | "1. Completás la solicitud." ("Estás acá") · "2. El equipo la revisa." · "3. Publicás lo que le hace falta a tu entidad y confirmás lo que recibís." | |
| | La entidad | "Nombre de la entidad" (ayuda: "Como la conoce el barrio. Es público.") · "Qué es" ("Comedor", "Merendero", "Escuela", "Hogar", "Club", "Biblioteca", "Otra"; ayuda: "Lo ven los donantes junto al nombre.") · "Tipo de organización" ("Asociación civil u ONG", "Institución: escuela, club o biblioteca", "Organismo público"; ayuda: "Si dudás, elegí la más parecida: el equipo lo revisa.") | Empresa no se ofrece: una empresa no puede ser entidad (contrato de `/api/entidades`) |
| | Dirección de entrega | "Calle" · "Altura" · "Localidad" · "Provincia" · ayuda del grupo: "Acá llegan las entregas. En público mostramos solo el barrio, nunca la dirección." | Para qué se pide |
| | Quién la representa | "Nombre y apellido" · "Correo electrónico" (ayuda: "Para avisarte cuando la solicitud esté aprobada y para ingresar.") · "Contraseña" (ayuda: "Al menos 8 caracteres.") | |
| | Casilla y acción | La misma casilla · "Todos los datos son obligatorios." · "Enviar la solicitud" | |
| | Salida | "¿Querés donar? Crear una cuenta de donante" | |
| Solicitud enviada | Sello y título | "Solicitud recibida" · "Recibimos la solicitud de tu entidad" | |
| | Qué pasa ahora | "El equipo de DonaTrack la revisa antes de sumarla. Te escribimos al correo que dejaste cuando esté aprobada; desde ese día podés ingresar y publicar lo que le hace falta a tu entidad." | Sin plazo |
| | Salidas | "Ver las entidades y lo que piden" · "Ir a la portada" | |

### 6.2 Ayudas por flujo crítico (RNF-UI-04)

Para Ingresar y los dos registros:

- [x] Cada campo tiene etiqueta visible.
- [x] El ejemplo de dato válido está donde se duda: la contraseña dice su largo; el buscador, un
  ejemplo.
- [x] Hay una pista de primer uso donde el flujo no es obvio: "Cómo sigue" en el registro de
  entidad; "Qué te pedimos" en Crear una cuenta.
- [x] Se explica para qué se piden los datos sensibles: el correo, la dirección (y qué se publica
  de ella) y el nombre visible.
- [x] Cada acción tiene su texto de éxito y su texto de error (sección 5).

### 6.3 Datos de maqueta

Sintéticos, escritos en la plantilla. Hoy es el jueves 15 de octubre de 2026 (el mismo día de la
portada). Período "último mes": del 16 de septiembre al 15 de octubre. "Última semana": del 8 al
15 de octubre.

**Entregas confirmadas** (Entregas confirmadas y Mapa), la más reciente primero:

| Qué y cuánto | Categoría | Entidad · barrio | Confirmada | Foto · texto alternativo |
|---|---|---|---|---|
| Sillas, 6 unidades | Muebles | Escuela Ejemplo N.º 12 · Centro Ejemplo | 14 de octubre | `img/raices/ejemplo-entrega-sillas.svg` · "Sillas apiladas en un aula" |
| Lentejas secas, 10 kg | Alimentos | Comedor Barrio Ejemplo · Barrio Ejemplo | 10 de octubre | `img/ejemplo-entrega.svg` · "Cajas con la donación junto a la puerta del comedor" |
| Arroz, 5 kg | Alimentos | Comedor Barrio Ejemplo · Barrio Ejemplo | 9 de octubre | ídem |
| Ropa de abrigo, 12 prendas | Ropa | Merendero Ejemplo Sur · Ejemplo Sur | 9 de octubre | `img/raices/ejemplo-entrega-ropa.svg` · "Bolsas con ropa de abrigo junto a la puerta del merendero" |
| Ropa de abrigo, 5 unidades | Ropa | Comedor Barrio Ejemplo · Barrio Ejemplo | 8 de octubre | ropa · "Bolsas con ropa de abrigo junto a la puerta del comedor" |
| Frazadas, 10 unidades | Ropa | Hogar Casa Ejemplo · Parque Ejemplo | 7 de octubre | ropa · "Bolsas con frazadas junto a la puerta del hogar" |
| Leche en polvo, 12 unidades | Alimentos | Comedor Barrio Ejemplo · Barrio Ejemplo | 6 de octubre | alimentos · ídem del comedor |
| Fideos secos, 8 kg | Alimentos | Comedor Barrio Ejemplo · Barrio Ejemplo | 5 de octubre | alimentos · ídem del comedor |
| Arroz, 10 kg | Alimentos | Comedor Barrio Ejemplo · Barrio Ejemplo | 2 de octubre | alimentos · ídem del comedor |
| Sillas, 4 unidades | Muebles | Biblioteca Popular Ejemplo · Villa Ejemplo | 22 de septiembre | `img/raices/ejemplo-entrega-sillas.svg` · "Sillas apiladas en la sala de lectura" |

Cuentas: 10 en el último mes, en 5 entidades (el Club no tiene); 5 en la última semana (las
primeras cinco). Por categoría: Alimentos 5 (todas del comedor: "5 entregas de alimentos en 1
entidad"), Ropa 3 (en 3 entidades), Muebles 2 (en 2 entidades), Útiles escolares 0. Coinciden con
el cuaderno de la portada (fideos, ropa del merendero y sillas) y con lo recibido en la [ficha de
Necesidades](raices-necesidades.md) (lentejas, arroz, leche y ropa de abrigo del comedor).

**Entidades con pedidos vigentes** (Entidades), en el orden de la portada:

| Entidad · qué es · barrio | Ícono | Pedidos (qué · cuánto falta · hasta cuándo · por qué) |
|---|---|---|
| Comedor Barrio Ejemplo · Comedor · Barrio Ejemplo | Alimentos | Lentejas secas · faltan 30 de 40 kg este mes · cada mes · para el almuerzo de lunes a viernes. Arroz · faltan 25 de 40 kg este mes · cada mes · para el almuerzo de lunes a viernes. Aceite de girasol · faltan 20 litros · hasta el 20 de octubre · para cocinar el almuerzo. Leche en polvo · faltan 12 de 24 unidades · hasta el 25 de octubre · para la merienda de los más chicos. Ropa de abrigo · faltan 25 de 30 unidades · hasta el 15 de noviembre · para las familias del barrio que perdieron ropa en el temporal del 3 de octubre |
| Escuela Ejemplo N.º 12 · Escuela · Centro Ejemplo | Muebles | Sillas · faltan 14 de 20 unidades · hasta el 30 de noviembre · para el aula nueva de primer grado. Útiles escolares · faltan 4 cajas · hasta el 30 de noviembre · para los chicos de primer grado |
| Merendero Ejemplo Sur · Merendero · Ejemplo Sur | Alimentos | Galletitas · faltan 10 de 20 kg este mes · cada mes · para la merienda de la tarde. Leche en polvo · faltan 20 unidades · hasta el 31 de octubre · para la merienda de la tarde |
| Hogar Casa Ejemplo · Hogar · Parque Ejemplo | Ropa | Frazadas · faltan 10 de 20 unidades · hasta el 15 de noviembre · para las camas del hogar |
| Club Ejemplo Norte · Club · Ejemplo Norte | Ropa | Camisetas deportivas · faltan 15 unidades · hasta el 30 de noviembre · para el equipo infantil de fútbol |
| Biblioteca Popular Ejemplo · Biblioteca · Villa Ejemplo | Muebles | Sillas · faltan 4 de 8 unidades · hasta el 31 de diciembre · para la sala de lectura infantil |

Por categoría: Alimentos 2 entidades (comedor y merendero), Ropa 3 (comedor, hogar y club), Muebles
2 (escuela y biblioteca), Útiles escolares 1 (escuela). Los pedidos del comedor son los de la [ficha de
Necesidades](raices-necesidades.md), sin el de fideos (lo publica la maqueta de esa ficha). El de
útiles de la escuela es la necesidad a la que se asignaron los útiles de Distribuidora Ejemplo SA
(`viaje-datos`, con problema, salen de nuevo el 17 de octubre): mientras no lleguen, siguen faltando.
**Es la única tabla de pedidos vigentes de la maqueta:** Entidades del donante
([ficha del donante](raices-donante-navegable.md)) muestra los mismos pedidos, con las mismas
cantidades y unidades.

**Cuentas de ejemplo** (Ingresar, de `SitioController`). Desde el 2026-11-07 (F5) son cuatro: la
segunda, "Martina Ruiz, persona donante", va a `donante-misiones?cuenta=persona`, y la de la empresa,
a `donante-inicio?cuenta=empresa` ([ficha de Misiones](raices-misiones.md#2-dónde-está-en-el-sitio)):

| Enlace | Lleva a | Cuenta |
|---|---|---|
| "Distribuidora Ejemplo SA, empresa donante" | `donante-inicio?cuenta=empresa` | Distribuidora Ejemplo SA · Empresa donante |
| "Martina Ruiz, persona donante" (desde 2026-11-07) | `donante-misiones?cuenta=persona` | Martina Ruiz · Persona donante |
| "Comedor Barrio Ejemplo, entidad" | `entidad-inicio` | Marta · Comedor Barrio Ejemplo |
| "Administración" | `admin-inicio` | Leo · Administración |

El botón "Ingresar" y "Continuar con Google" envían el formulario (`post`) a `donante-inicio`; el
registro de donante, también; el de entidad, a `publico-registro-entidad-enviada`. Las direcciones
llevan el prefijo del sitio (`/`).

### 6.4 Filtro de tono

- Lo confirmado dice qué lo respalda: "la confirmó la entidad … con una foto".
- Las cifras llevan período, alcance y fecha: "10 entregas confirmadas en el último mes, del 16 de
  septiembre al 15 de octubre de 2026. Actualizado …".
- Actividad no es impacto: la pantalla se llama "Entregas confirmadas", no "Donaciones realizadas"
  ni "impacto".
- Lo pendiente, pendiente: los pedidos dicen "faltan", y la entidad en revisión, "todavía está en
  revisión".
- Sin urgencia ni culpa: "Quiero donar", sin "ahora"; los pedidos no llevan cuenta regresiva (la
  fecha alcanza).
- Sin promesas que nadie fijó: ni plazo de revisión, ni "gratis", ni elegir la entidad.
- Las entidades actúan: piden, confirman, publican. Se nombran por su tipo y su nombre.
- La raíz, como mucho en un título por pantalla; nunca en botones, datos ni errores. Palabras que
  no van: "eco", "huella verde", "florecer", "regá", "cosechá", "sostenible" de adorno.

## 7. Accesibilidad

- Zonas: saltar al contenido, la cabecera con la navegación pública (`nav` con nombre, por
  ejemplo "Secciones"), el contenido (`main`) con un solo `h1`, y el pie con su `nav`.
- La sección actual de la navegación lleva `aria-current="page"`; también el período, el tipo de
  donante y la categoría elegidos, si son enlaces.
- Filtros y buscador: formularios `get` con `label` (o `fieldset` y `legend` en los grupos de
  opciones) y un botón que envía; funcionan sin JavaScript. Con JavaScript, la cifra que cambia se
  anuncia en una región `aria-live="polite"`.
- El mapa: el dibujo de la maqueta es `aria-hidden` y no recibe foco; la lista es la alternativa
  completa (RF-CL-06), en el mismo orden que el mapa. En el sistema, los marcadores de MapLibre se
  alcanzan con el teclado y su detalle es el mismo de la lista.
- Fotos: cada una con su `alt` (sección 6.3). La foto grande, en un `dialog` que recibe el foco,
  se cierra con Escape y devuelve el foco al enlace; sin JavaScript, el enlace lleva a la imagen.
- Formularios: cada campo con `label` y su ayuda en `aria-describedby`; `autocomplete` (`email`,
  `current-password`, `new-password`, `organization`, `given-name`, `family-name`, `name`,
  `address-line1`, `address-level2`, `address-level1`); el resumen de errores recibe el foco y
  enlaza a cada campo, que lleva `aria-invalid`. En Ingresar, el aviso describe a los dos campos
  sin decir cuál falló.
- "Mostrar" la contraseña: un botón con `aria-pressed`, solo con JavaScript; sin él, no aparece.
- Avisos de error, `role="alert"`; la solicitud enviada y "Ingresando…", `role="status"`.
- Los sellos ("Entrega confirmada", "Solicitud recibida") llevan texto; la forma y el color solo
  acompañan.
- Toque de 48 px, contraste AA y sin desborde entre 360 y 1920 px
  ([piso](../direccion-de-arte.md#el-piso-no-se-negocia)); con "reducir movimiento", sin animación.

## 8. Revisión de usabilidad

| # | Heurística | Cómo la cumple | Riesgo que queda |
|---|---|---|---|
| 1 | Visibilidad del estado | Cada lista dice su cifra con período y fecha de actualización; cargando, vacío y error con texto; "Ingresando…", "Creando tu cuenta…"; la entidad sabe en qué paso está ("Cómo sigue", "Tu entidad todavía está en revisión") | Sin plazo de revisión, la entidad no sabe cuánto esperar (pregunta abierta) |
| 2 | Lenguaje del usuario | "Entregas confirmadas", "Lo que piden", "Quiero donar, yo o mi empresa", "Represento a una entidad"; "Qué es" en lugar de "rubro" y "Tipo de organización" con ejemplos en lugar de "tipo jurídico" | "Tipo de organización" puede seguir siendo dudoso para alguien de un comedor |
| 3 | Control y libertad | Navegación pública y Atrás siempre; los filtros se sacan con "Ver todas"; ante un error, lo escrito sigue (salvo la contraseña) | — |
| 4 | Consistencia | Las mismas categorías, íconos y "Ver la foto de la entrega" que la portada; los avisos de error con código como en Contacto y Necesidades; el resumen de errores de Contacto | Los dos sitios dibujan distinto; los textos y estados, iguales |
| 5 | Prevención de errores | `type="email"`, `required`, `minlength`; la contraseña se puede mostrar; "Empresa" no se ofrece a una entidad; el país no se pregunta | La regla de la contraseña es propuesta (D1) |
| 6 | Reconocer antes que recordar | Cada tarjeta de Crear una cuenta dice qué se pide; el aviso del nombre visible está junto a la casilla; la lista del mapa repite todo lo del dibujo | — |
| 7 | Flexibilidad | Quien ya tiene cuenta ingresa desde cualquier pública; "Continuar con Google"; filtros y búsqueda para quien busca algo puntual | — |
| 8 | Diseño minimalista | Una pregunta por pantalla (qué llegó, adónde, quién pide, entrar, sumarse); el registro de donante pide cuatro datos y la casilla | El registro de entidad sigue siendo largo (diez datos), aunque en tres grupos |
| 9 | Recuperarse de errores | Cada error dice qué pasó y qué hacer; el de ingresar ofrece pedir una contraseña nueva; el del servidor, un código para escribir | — |
| 10 | Ayuda | Ayudas cortas en cada campo dudoso; "Cómo sigue" en el registro de entidad | — |

Los 7 factores de la experiencia ([criterios de la cátedra](../criterios-catedra-ux-ui.md),
sección 2):

| Factor | Cómo lo cumple |
|---|---|
| Útil | Responde lo que pregunta quien evalúa: qué llegó, adónde y quiénes piden; y deja entrar o sumarse |
| Usable | Formularios cortos, en grupos, con etiquetas visibles; listas con filtros que andan sin JavaScript |
| Deseable | La identidad de Raíces (papel, tierra, sellos, la raíz en un título) con la libertad de cada sitio |
| Valioso | La empresa ve pruebas (fotos y fechas) antes de registrarse; la entidad ve que su pedido cuenta |
| Ubicable | Los mismos seis destinos en todas las públicas, también en la portada (en A, salvo Contacto y Términos, aprobadas: la marca lleva a la portada); nada a más de 3 niveles |
| Accesible | Lista junto al mapa, textos alternativos, errores enlazados, AA, Ley 26.653 |
| Creíble | Cada cifra con período y fuente; lo confirmado, con la foto de la entidad; la ubicación, aproximada y dicho; nada de quién donó en público |

Qué conviene probar con personas reales (la revisión heurística no reemplaza al test con
usuarios): si alguien de una empresa encuentra la prueba de que lo donado llegó sin registrarse; si
alguien de un comedor entiende "Tipo de organización" y "Qué es" sin ayuda; si se entiende que el
donante no elige la entidad.

## 9. Decisiones y preguntas

Decisiones tomadas (agente, propuestas; aprobadas con la ficha al elegir el sitio A, 2026-11-03):

- **Entregas confirmadas y Mapa, separadas** (la observación del mapa del sitio lo dejaba para este
  flujo): la galería responde "qué llegó" con la foto; el mapa, "adónde". Se enlazan entre sí.
- **"Entregas confirmadas" en lugar de "Donaciones realizadas"** en la navegación y el rótulo: la
  pantalla muestra entregas que la entidad confirmó (tono, "Actividad no es impacto"). La dirección
  sigue siendo `publico-donaciones`.
- **El período de la galería: última semana o último mes** (RF-01 pide el último mes; HB-01, el
  parámetro). "Último año", del boceto, queda afuera: no entra en una galería sin paginar.
- **El mapa muestra el último mes, filtrado por categoría,** con un punto por entidad en su barrio.
- **Entidades sin un botón por entidad:** la asignación la decide DonaTrack (RF-17); un solo
  "Quiero donar".
- **El registro de donante empieza por la empresa** y pide a la persona que usa la cuenta, que es
  la alternativa recomendada de D30 (pendiente).
- **El registro de entidad, en tres grupos y con un solo representante;** el resto se suma en Mi
  perfil.
- **En la maqueta, Ingresar lleva al inicio de cada rol** con su cuenta de ejemplo; el formulario y
  Google, al del donante.
- **"¿Olvidaste tu contraseña?" lleva a Recuperar la contraseña de cada sitio:** el controlador no
  muestra "sin dibujar" para lo público y el recorrido no sale del sitio, así que A y B dibujan el
  paso (un correo y el aviso que no dice si hay cuenta). Así no hay enlaces muertos ni salidas fuera
  del sitio (arreglo de la crítica en B y de la pre-auditoría en A, 2026-11-02).
- **Los estados de carga van con `?escenario=`; los resultados de un envío, en su pantalla** (como
  la portada y Contacto).

Desvíos del boceto, para la [bitácora](../bitacora.md) al cerrar la corrida: la galería sin el mapa
(pág. 2); un solo mapa con un punto por entidad en lugar del mapa mundial por regiones (págs. 2 y
23); correo en lugar de usuario y etiquetas visibles (pág. 4); "entidad" en lugar de
"beneficiario" (pág. 5); el registro de entidad en tres grupos, con un representante y "Qué es" en
lugar de rubro (pág. 11).

Preguntas abiertas (no frenan la maqueta; la ficha usa la opción indicada):

1. **Regla de la contraseña:** la fija el IdP (D1, pendiente). La ficha usa "Al menos 8
   caracteres".
2. **¿Cuánto tarda la revisión de una entidad?** No hay plazo acordado; los textos no prometen uno.
3. **Correo ya registrado al crear una cuenta:** decir que existe ayuda, pero revela cuentas
   (RNF-AUTH-10 lo pide para login y recuperación). La maqueta no lo dibuja.
4. **¿Cuánto dura el enlace para una contraseña nueva?** Depende de D8 (pendiente); RF-AUTH-13
   sugiere 30 minutos. Los textos no dan un plazo: dicen "por tiempo limitado".

Hallazgos, sin corregir en esta ficha:

- La portada dice "Octubre de 2026 · 3 entregas confirmadas y 1 en camino" en la planta, pero la
  [ficha de Necesidades](raices-necesidades.md) ya registra más entregas recibidas en octubre (y
  esta ficha las muestra). La planta habla de "este mes" sin decir que es una muestra.
- En `viaje-datos` y en Seguimiento del donante (aprobada), la escuela
  se llama "Escuela Ejemplo 12"; en la portada de Raíces, "Escuela Ejemplo N.º 12". Las tres fichas
  de la corrida usan "Escuela Ejemplo N.º 12"; lo demás se iguala al integrar.

## Huecos de backend

Datos que la maqueta inventa porque el backend no los da hoy. Son los de la lista de la corrida
([raices-ab-huecos-backend.md](raices-ab-huecos-backend.md)), copiados acá. Sin DTI ni ADR nuevos.

- Público · publico-donaciones · entregas confirmadas con foto de la última semana o el último mes, con DTO público sin el donante (HB-01) y foto legible en público (hoy las fotos son privadas con URL firmada, RNF-PRIV-03) · RF-01 no tiene endpoint; la maqueta usa 10 entregas sintéticas.
- Público · publico-donaciones, publico-mapa · fecha de la última actualización de los datos públicos · toda cifra dice cuándo se actualizó (tono, RF-CL-07); ningún endpoint la da; la maqueta dice "15 de octubre de 2026, 14:30".
- Público · publico-mapa · entregas confirmadas del período con barrio o coordenadas aproximadas, categoría, fecha y foto, filtrables por categoría (HB-07) · RF-02 y D24; la maqueta dibuja un mapa fijo con su lista.
- Público · publico-entidades · entidades activas con pedidos vigentes, barrio aproximado y "qué es", sin contacto, con búsqueda por nombre y filtro por categoría (HB-08, HB-15, HB-18) · RF-22; el "rubro" de JuridicaInputDTO es texto libre y el filtro y el ícono necesitan una lista fija (comedor, merendero, escuela, hogar, club, biblioteca, otra).
- Público · publico-entidades · total de entidades activas ("de las 46 que están en DonaTrack") (HB-02) · la misma cifra de la portada.
- Público · publico-entidades · si cada entidad confirmó entregas en el período, para enlazar "Ver sus entregas en el mapa" (el club no) · el listado de entidades no lo trae; obligaría a cruzarlo con las entregas del mapa (HB-07) · sitio B.
- Público · publico-ingresar · inicio de sesión con correo y contraseña y con Google (IdP, RF-04, D1 pendiente), bloqueo por intentos (RNF-AUTH-07) y el estado "entidad pendiente de aprobación" (HB-12) · la maqueta entra con tres cuentas de ejemplo.
- Público · publico-recuperar · pedido de enlace de un solo uso con vencimiento y respuesta neutra (RF-AUTH-13, RNF-AUTH-10) · no hay mecanismo de envío (D8 pendiente) ni IdP (D1); la maqueta va al aviso sin mandar nada.
- Público · publico-registro-donante · alta en el IdP y completar perfil (RI-AUTH-02, HB-11), constancia de los legales (HB-20) y quién ingresa por una empresa (D30 pendiente) · la maqueta pide el nombre de quien usa la cuenta y lleva al inicio del donante.
- Público · publico-registro-entidad · alta de la jurídica con dirección y un representante, en estado pendiente, y su aprobación (HB-12, RF-21) · RF-03; la maqueta lleva a la solicitud enviada.
