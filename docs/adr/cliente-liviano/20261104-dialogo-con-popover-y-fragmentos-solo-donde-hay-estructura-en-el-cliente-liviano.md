# Diálogo con `popover` y fragmentos solo donde hay estructura: dos correcciones al ADR de interacción y componentes

- Status: proposed
- Date: 2026-11-04
- Deciders: pendiente (lo propone el agente; lo decide el usuario). Propuesta que el usuario pidió en la
  tanda 4 de la fase 2 ("proponerle una nota o un ADR sucesor `proposed`"; sesión del 2026-11-04)
- Tags: cliente-liviano, interfaz, alpine, componentes, accesibilidad, entrega-6

> **Origen:** al implementar la fase 2 del [backlog de migración](../../interfaz/backlog-migracion-componentes.md)
> (tandas 2 a 4) el código siguió caminos distintos a dos frases del
> [ADR de interacción, movimiento y componentes](./20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
> (`proposed`, D31 y D33). Este documento no lo reemplaza: propone corregir esas dos frases. No se
> toca el ADR de origen hasta que el usuario decida.

## Contexto y Problema

- `[DOCUMENTED]` D31 dice que "el `<dialog>` nativo con `showModal()` ya atrapa el foco" y por eso
  no se carga el plugin Focus de Alpine.
- `[OBSERVED]` El diálogo del sitio (`.dialogo`, B2-05) es un `<dialog class="dialogo" popover>` que se abre con
  `popovertarget`. Un popover **no atrapa el foco ni vuelve inerte el fondo**: el navegador mueve el
  foco adentro al abrir (con `autofocus`), lo devuelve al botón al cerrar y cierra con Esc o al tocar
  afuera, pero con Tab se puede salir al resto de la página. Se eligió así para que una baja se
  confirme **sin JavaScript** (RNF-CL-02); `showModal()` exige JavaScript.
- `[DOCUMENTED]` D33 dice "un componente es un fragmento con parámetros con nombre y slots". Para la fase 2 el
  catálogo ([`componentes.md`](../../interfaz/componentes.md)) tiene fragmento en `sello`, `muestra`,
  `novedades` (de `piezas-entidad`) y la navegación pública; **no** lo tienen botón, campo, diálogo, navegación
  de cada rol, tarjeta, lista, aviso, banner, vacío, carga, subida de fotos, pestañas, cabecera de la
  entidad ni mapa: son elementos nativos o atributos sobre HTML de la página, y su contrato es el HTML
  del catálogo (cerco: la clase figura en el catálogo y en `/estilos`).
- Problema: ¿qué debe decir el ADR sobre el diálogo y sobre cuándo un componente lleva fragmento, para
  que los agentes sigan lo que el sitio hace y no lo que la frase literal sugiere?

## Alternativas

| Alternativa | Sin JavaScript | Foco y fondo | Consistencia con el sitio | Costo |
|---|:-:|:-:|:-:|:-:|
| **P1. `popover` por defecto para el diálogo esencial; `showModal()` solo para uno no esencial que necesite fondo inerte** | sí | el foco entra y vuelve; sin trampa | igual que el código y que `cliente-liviano/AGENTS.md` | ninguno |
| P2. `showModal()` para todo (como dice D31) | no: sin JavaScript la baja no se confirma | trampa y fondo inerte nativos | cambia 3 pantallas | medio; rompe RNF-CL-02 |
| P3. `popover` más el plugin Focus de Alpine para atrapar el foco | no del todo | trampa con JavaScript | suma una dependencia | medio |

| Alternativa de fragmentos | Consistencia | Costo |
|---|:-:|:-:|
| **F1. Fragmento solo si el componente tiene estructura interna repetida o datos con parámetros (`sello`, `novedades`, `navegacion-publica`); el resto, el HTML del catálogo es el contrato** | igual que el código | ninguno |
| F2. Un fragmento por componente (como dice D33) | alta | alto: 15 fragmentos que envuelven un solo elemento nativo; cada uno con su `th:replace` y su parámetro |

## Resultado de la Decisión (propuesto)

- **Diálogo:** `<dialog class="dialogo" popover>` con `popovertarget`, sin plugin Focus. `showModal()` solo para
  un diálogo que no sea esencial y necesite el fondo inerte. D31 queda corregido: el `<dialog>` con
  `showModal()` atrapa el foco, pero el del sitio es un popover y no lo atrapa; el compromiso de
  accesibilidad es que `autofocus` pone el foco en la opción segura y el navegador lo devuelve al cerrar.
- **Fragmentos:** el fragmento Thymeleaf es para piezas con estructura o parámetros; botón, campo, tarjeta,
  lista y el resto se escriben con el HTML del catálogo.

## Consecuencias

- Buenas: el ADR coincide con el código y con `cliente-liviano/AGENTS.md`; las pantallas siguen
  andando sin JavaScript.
- Malas: quien abre un diálogo con teclado puede salir de él con Tab antes de cerrarlo; se acepta porque
  Esc y tocar afuera lo cierran y el foco vuelve al botón.
- Pendiente: la revisión humana. Hasta entonces el ADR de origen conserva las dos frases.

## Relaciones

1. **Corrige en parte:** [ADR de interacción, movimiento y componentes](./20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
   (D31, el diálogo; D33, los fragmentos). Si se acepta, el de origen pasa a `superseded` solo en esas
   dos frases o se le agrega una nota; lo resuelve el revisor humano.
2. **Respeta:** [una sola identidad](./20261103-una-sola-identidad-en-el-cliente-liviano.md) (`accepted`).
