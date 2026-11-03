# Flujo: Mi perfil del donante (mínimo), en Raíces

> **Estado:** aprobado · **Fecha:** 2026-11-02 · **Aprobó:** usuario (2026-11-02), al ver las pantallas dibujadas
> **Ajustada:** 2026-11-02, con la review de Sofía Deane: con el nombre oculto, la entidad ve la
> categoría del ranking, y se puede ocultar también en una sola donación (bitácora, "La review de la
> diseñadora, decidida").
> Plantilla de la skill `ux-flujo`. Pantalla nueva de Raíces, en el sitio
> ([ADR propuesto](../../adr/cliente-liviano/20261103-una-sola-identidad-en-el-cliente-liviano.md)):
> `/donante-perfil`, que hasta hoy abría "todavía no está dibujada". Es un perfil
> **mínimo**: solo lo que pidieron las decisiones del 2026-11-02 (preguntas 15, 16 y 18). Editar los
> datos de la cuenta (RI-AUTH-02) queda para cuando se dibuje el perfil completo.

## 1. Para quién y para qué

| | |
|---|---|
| Quién | La persona de responsabilidad social de una empresa que dona (Distribuidora Ejemplo SA), o una persona que dona. Entra pocas veces, desde la computadora |
| Tono | Pantallas de trabajo: claro, útil, tranquilizador ([tono según la pantalla](../tono-y-comunicacion.md#tono-según-la-pantalla)) |
| Qué quiere lograr | "Decidir si las entidades ven mi nombre cuando reciben lo que doné" |
| Dispositivo principal | Computadora, con el celular como segundo |
| Requerimientos | RI-AUTH-02 (perfil). La preferencia y la constancia de los legales, del [ADR del nombre visible](../../adr/20261102-nombre-visible-del-donante-y-constancia-de-los-legales.md) (`proposed`) |
| Datos que usa | Ninguno todavía: HB-20 y [DTI-32](../../adr/DEUDA_TECNICA.md#dti-32) (`PATCH /api/donantes/{id}/preferencias`, `GET /api/personas/me` con la aceptación). En la maqueta, el formulario va a `donante-perfil-guardado` |
| Boceto de E1 | Página 14, menú de usuario: "Mi perfil" |

## 2. Dónde está en el sitio

Donante → cuenta (arriba, junto al nombre) → **Mi perfil**, nivel 2
([mapa del sitio](../mapa-del-sitio.md): Mi perfil). No está en el menú de secciones: el menú no
marca ninguna. La cuenta de la cabecera suma el enlace "Mi perfil", junto a "Salir"; en el
celular, la cuenta sigue en la cabecera, arriba, y el menú de secciones abajo.

## 3. Recorrido

```mermaid
flowchart TD
  cuenta([Cuenta: Mi perfil]) --> perfil[Mi perfil]
  perfil -->|elige y guarda| servidor{¿Se guardó?}
  servidor -->|sí| guardado[Aviso: listo, con cómo te ven ahora]
  servidor -->|error| error[No pudimos guardar: sigue como antes]
  servidor -->|sin conexión| sinConexion[No sabemos si se guardó]
  perfil -->|Términos y privacidad| legales([Legales])
```

Desvíos: no guardar (se va con el menú y no cambia nada), error del servidor y sin conexión.

## 4. Pantallas

```text
┌─────────────────────────────────────────────────┐
│ MI PERFIL (rótulo)                              │
│ Distribuidora Ejemplo SA                        │
│ Empresa donante · contacto@distribuidora-…      │
│                                                 │
│ ┌ Cómo te ven las entidades ──────────────────┐ │
│ │ Cuando una entidad recibe lo que donaste,   │ │
│ │ ve quién donó.                              │ │
│ │ (•) Mostrar mi nombre                       │ │
│ │     Donó: Distribuidora Ejemplo SA          │ │
│ │ ( ) No mostrarlo                            │ │
│ │     Donó: Empresa donante · Sostenedor      │ │
│ │ Sin tu nombre, la entidad ve tu categoría   │ │
│ │ como donante. Vale para todas tus           │ │
│ │ donaciones, también las que ya llegaron.    │ │
│ │ Los avisos que la entidad ya recibió no     │ │
│ │ cambian.                                    │ │
│ │ [Guardar mi elección]                       │ │
│ └─────────────────────────────────────────────┘ │
│                                                 │
│ Términos y privacidad                           │
│ Aceptaste la versión 1 el 1 de noviembre de 2026│
│ Leer los términos y la política de privacidad → │
├─────────────────────────────────────────────────┤
│ Términos y privacidad · Contacto                │
└─────────────────────────────────────────────────┘
```

Cada opción lleva debajo cómo la ve la entidad, con la etiqueta "Donó" igual a la de Confirmar
recepción. Funciona sin JavaScript: dos opciones y un botón Guardar (usuario, 2026-11-02, contra un
interruptor que guarda solo).

## 5. Estados

| Pantalla | Estado | Qué se ve | Texto |
|---|---|---|---|
| Mi perfil | Con datos | Lo elegido marcado | — |
| Mi perfil | Oculto (`?escenario=oculto`) | "No mostrarlo" marcado | — |
| Mi perfil | Guardando (`?escenario=lento`) | El botón dice "Guardando…" y queda desactivado | "Guardando…" |
| Guardado (`donante-perfil-guardado`) | Éxito, al ocultarlo | Aviso arriba, con "No mostrarlo" marcado | "Listo. Las entidades ya no ven tu nombre: ven «Empresa donante · Sostenedor»." (una persona: «Donante Sostenedor»; la categoría es la del ranking de Incentivos) |
| Guardado (`donante-perfil-guardado?mostrar=si`) | Éxito, al mostrarlo | Aviso arriba, con "Mostrar mi nombre" marcado | "Listo. Las entidades ven tu nombre: «Distribuidora Ejemplo SA»." |
| Mi perfil | Error del servidor (`?escenario=error`) | Aviso arriba | "No pudimos guardar tu elección. Es un problema nuestro: sigue como estaba. Probá de nuevo en unos minutos; si sigue, escribinos con este código." + código y enlace a Contacto |
| Mi perfil | Sin conexión (`?escenario=sin-conexion`) | Aviso arriba | "Se cortó la conexión mientras se guardaba. Recargá la página para ver qué quedó elegido." |
| Mi perfil | Cargando | No aplica en la maqueta: la página llega armada del servidor | — |

## 6. Textos

| Elemento | Texto | Nota |
|---|---|---|
| Rótulo y título | "Mi perfil" · el nombre de la cuenta | |
| Grupo | "Cómo te ven las entidades" | `fieldset` + `legend` |
| Bajada del grupo | "Cuando una entidad recibe lo que donaste, ve quién donó." | |
| Opciones | "Mostrar mi nombre" · "No mostrarlo" | Con su ejemplo debajo |
| Nota | "Sin tu nombre, la entidad ve tu categoría como donante. Vale para todas tus donaciones, también las que ya llegaron. Los avisos que la entidad ya recibió no cambian." | Sale del ADR: el nombre se calcula al leer |
| Acción principal | "Guardar mi elección" | |
| Legales | "Aceptaste la versión 1 el 1 de noviembre de 2026." · "Leer los términos y la política de privacidad" | La constancia, a la vista; la versión 1 rige desde ese día |

Persona en vez de empresa: "Donante Sostenedor" (con su categoría), el mismo texto que ve la
entidad en Confirmar recepción y en Necesidades.

## 7. Accesibilidad

Un solo `h1`. Las opciones son un `fieldset` con `legend`; el ejemplo de cada opción va en su
`label`, para que se lea con ella. El aviso de guardado usa `role="status"` y recibe el foco; el
de error, `role="alert"`.

## 8. Revisión de usabilidad

| # | Heurística | Cómo la cumple | Riesgo que queda |
|---|---|---|---|
| 1 | Visibilidad del estado | Lo elegido, marcado; aviso al guardar con cómo queda | |
| 2 | Lenguaje del usuario | "Cómo te ven las entidades", "No mostrarlo" | |
| 3 | Control y libertad | Se cambia cuantas veces se quiera | |
| 4 | Consistencia | "Donó: …" igual que en Confirmar recepción | |
| 5 | Prevención de errores | El ejemplo muestra el efecto antes de guardar | |
| 6 | Reconocer antes que recordar | El ejemplo, debajo de cada opción | |
| 7 | Flexibilidad | — | |
| 8 | Diseño minimalista | Una sola decisión y la constancia | |
| 9 | Recuperarse de errores | Error y sin conexión dicen qué quedó | |
| 10 | Ayuda | La nota dice a qué donaciones alcanza | |

Factores de Morville: útil (el control que pidió el grupo), usable (una decisión), deseable
(etiqueta de herbario como las de Necesidades), valioso (privacidad sin perder transparencia),
ubicable (cuenta de la cabecera), accesible (AA, sin JavaScript), creíble (muestra la constancia de
los legales). Probar con personas: si se entiende que alcanza a las donaciones pasadas.

## 9. Decisiones y preguntas

Decisiones tomadas:

- El nombre de quien donó se ve, sea empresa o persona, y cada donante puede ocultarlo desde su
  perfil (usuario, 2026-11-02, preguntas 15 y 16).
- Perfil mínimo con esta opción y la constancia de los legales; el resto del perfil, después
  (usuario, 2026-11-02).
- Dos opciones con su ejemplo y un botón Guardar (usuario, 2026-11-02).
- Vale también para las donaciones pasadas (agente, del ADR).
- Por defecto, el nombre se ve. Sofía votó ocultarlo por defecto; el usuario mantuvo visibles la
  empresa y la persona (review de Sofía, 2026-11-02).
- Con el nombre oculto, la entidad ve la categoría del donante en el ranking de Incentivos:
  "Donante Sostenedor" o "Empresa donante · Sostenedor" (usuario, 2026-11-02, a propuesta de
  Sofía; antes, "Una persona que prefiere no mostrar su nombre").
- **Esta vez, sin mi nombre:** al registrar una donación, una casilla la oculta aunque el perfil
  muestre el nombre. Solo oculta: no muestra lo que el perfil oculta. Se dibuja con la pantalla de
  registrar una donación, que Raíces todavía no tiene (usuario, 2026-11-02).
- Sofía pidió los legales también en los ajustes: Mi perfil ya muestra la constancia y el enlace,
  y queda así (usuario, 2026-11-02).
- Si la categoría no llega (Incentivos no responde o el donante no tiene una), la entidad ve «Un
  donante» o «Una empresa donante», y la pantalla no falla (usuario, 2026-11-02, por la revisión).

Preguntas abiertas, para el equipo (no cambian el prototipo):

- La entidad también tiene Mi perfil (mapa del sitio), pero su nombre es público por RF-06: no
  lleva esta opción. Se dibuja con el perfil completo.
